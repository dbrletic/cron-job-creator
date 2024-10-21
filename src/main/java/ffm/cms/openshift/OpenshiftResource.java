package ffm.cms.openshift;

import ffm.cms.model.CronJobDashboardData;
import ffm.cms.model.CronJobData;
import ffm.cms.model.CronJobReports;
import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.redhogs.cronparser.CronExpressionDescriptor;
import io.fabric8.tekton.client.*;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunList;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunList;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.triggers.v1beta1.Param;
import io.fabric8.tekton.triggers.v1beta1.TriggerBinding;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * Handles getting information from OpenShift like Cronjob Schedules, logs, etc
 * @author dbrletic
 */
@ApplicationScoped
@RegisterRestClient
@Path("/openshift")
public class OpenshiftResource {

    @Inject //Generic OpenShift client
    private OpenShiftClient openshiftClient; //Make sure to add a ServiceAccount to the deployment that has access to the namespace that has the pipeline runs.  This will automatticaly add in the kubeconfig file that gives the client the needed permissions. 
    
    //All the private Static String
    final private static String CRIO_MSG = "Unable to get logs. Check email for status of run.";
    final private static String CRITICAL_FAILURE = "Critical Failure. Selenium test did not run or had exception.";
    final private static String BUILD_FAILURE = "Compliation error. Check logs for errors.";
    final private static String STEP_CONTAINER = "step-build-and-run-selenium-tests";
    final private static String STEP_ZIP_FILES = "step-reduce-image-sizes-from-selenium-tests";
    final private static String RAN_BUT_FAILED = "Tests run: 0, Failures: 0, Errors: 0, Skipped: 0";
    final private static String RUN_BUT_FAILED_MSG = "Test run but had exception - Run: %d, Passed: %d, Failures: %d";
    final private static String TEST_RUN = "Test run - Run: %d, Passed: %d, Failures: %d";
    final private static String CRI_O_ERROR = "unable to retrieve container logs for cri-o:";
    final private static String NO_TEST_RUN = "Tests run: 0, Failures: 0, Errors: 0, Skipped: 0";
    final private static String BUILD_SUCCESS = "BUILD SUCCESS";
    final private static String NO_TEST_RUN_MSG = "Test run - Run: 0, Passed: 0, Failures: 0";
    final private static String PASSED = "Passed";
    final private static String FAILED = "Failed";
    final private static String RUNNING = "Running";
    final private static String CANCELLED = "Cancelled";
    final private static String TEST_FAILURE_START = "[ERROR] Failures:";
    final private static String TEST_FAILURE_END = "[ERROR] Tests run:";
    final private static String INFO = "[INFO]";
  
    final private static String GREEN = "#69ff33"; //Green
    final private static String YELLOW = "#EBF58A"; //Yellow
    final private static String RED = "#ff4763"; //Red
    final private static String GRAY = "#f6f6f6"; //Gray
    final private static String ORANGE = "#F0C476"; //Orange

    final private static String JS_START = "$(document).ready( function () {";
    final private static String JS_END = " });";
    final private static String JS_REPEAT_AND_REPLACE ="var REAPLCE = new DataTable('#REPLACE', {paging: false } );";


    //All the Pattern Matching 
    private Pattern patternTestRun = Pattern.compile("Tests run: \\d+, Failures: \\d+, Errors: \\d+, Skipped: \\d+");
    private Pattern patternBuildFailed = Pattern.compile("COMPILATION ERROR");
    private Pattern patternTimeStart = Pattern.compile("Total time:");
    private Pattern patternEnv = Pattern.compile("test\\d+");
    private Pattern patternCRIOError = Pattern.compile(CRI_O_ERROR);
    private Pattern patternNoTestRun = Pattern.compile(NO_TEST_RUN);

    //Sorts CronJobDashboardData by their names
    Comparator<CronJobDashboardData> nameSorter = (a, b) -> a.name.compareToIgnoreCase(b.name);
    //Sorts CronJobDashboardData by their release brance
    Comparator<CronJobDashboardData> releaseBranchSorter = (a, b) -> a.name.compareToIgnoreCase(b.releaseBranch);

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance cronJobData(List<CronJobData> cronJobs);
        public static native TemplateInstance gatlingCronJobData(List<CronJobData> cronJobs);
        public static native TemplateInstance cronJobDashboard(List<CronJobDashboardData> cronJobs);
        public static native TemplateInstance cronJobReportHistory(List<CronJobReports> cronJobsReports);
    }

    @Inject
    @ConfigProperty(name = "quarkus.openshift.mounts.pipeline-storage.path")
    private String pipelinePVCMountPath;

    @GET()
    @Path("/{namespace}/cronjobs")
    @Produces(MediaType.TEXT_HTML)
    @Blocking //Due to the OpenShiftClient need to make this blocking
    public TemplateInstance getCurrentCronJobs(@RestPath String namespace) throws ParseException, Exception{
        Instant start = Instant.now();
        // Helpful openShiftClient / kubernetes cheatsheet
        //https://github.com/fabric8io/kubernetes-client/blob/main/doc/CHEATSHEET.md#cronjob
        
        //Have to use TektonClient for anything related to pipelines
        TektonClient tknClient = new KubernetesClientBuilder().build().adapt(TektonClient.class);
       
        //Getting all the CronJobs
        List<CronJob> cronJobList = openshiftClient.batch().v1().cronjobs().inNamespace(namespace).list().getItems();

        //Getting all the TriggerBindings
        List<TriggerBinding> tbList =  tknClient.v1beta1().triggerBindings().inNamespace(namespace).list().getItems();
        
        Map<String, String> bindingParamsToBranch = new HashMap<String, String>();
        List<CronJobData> cronJobs = new ArrayList<>();

        //Listing the current OpenShift user

        //So there is no duplicate code between Selenium and Gatling
        bindingParamsToBranch = bindParamsToBranch(tbList);
        
        //Filling out the values for the linked template
        for(CronJob job : cronJobList){
            CronJobData currentJob = new CronJobData();
            currentJob.name = job.getMetadata().getName();
            currentJob.schedule = job.getSpec().getSchedule();
            currentJob.humanReadableMsg = CronExpressionDescriptor.getDescription(currentJob.schedule);
            String bindingName = currentJob.name + "-binding";
            currentJob.branch = bindingParamsToBranch.get(bindingName);
            //Had to change newer cronjobs to end in cj instead of cronjob. Should clean up 
            if(currentJob.branch == "" || currentJob.branch == null || currentJob.branch.isBlank() || currentJob.branch.isEmpty()){
                //Changing the name back to the old style
                bindingName = currentJob.name.replaceAll("cj", "cronjob") + "-binding";
                //System.out.println("Looking for: " + bindingName);
                currentJob.branch = bindingParamsToBranch.get(bindingName); 
            }
            //Setting the type
            int typeIndexNameEnd = currentJob.name.indexOf("-");
            currentJob.type=currentJob.name.substring(0, typeIndexNameEnd);
           // System.out.println("Job name: " + currentJob.name + " Job Type: " + currentJob.type);

           //Setting the env
           Matcher matcherEnv = patternEnv.matcher(currentJob.name);
           if(matcherEnv.find()){
                currentJob.env = currentJob.name.substring(matcherEnv.start(), matcherEnv.end());
           }
            cronJobs.add(currentJob);
        }
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        System.out.println("getCurrentCronJobs took " + elapsedMs + " milliseconds to complete");
        return Templates.cronJobData(cronJobs);
    }

    @GET()
    @Path("/{namespace}/gatling")
    @Produces(MediaType.TEXT_HTML)
    @Blocking //Due to the OpenShiftClient need to make this blocking
    public TemplateInstance getCurrentGatlingCronJobs(@RestPath String namespace) throws ParseException, Exception{

        // Helpful openShiftClient / kubernetes cheatsheet
        //https://github.com/fabric8io/kubernetes-client/blob/main/doc/CHEATSHEET.md#cronjob
        
        //Have to use TektonClient for anything related to pipelines
        TektonClient tknClient = new KubernetesClientBuilder().build().adapt(TektonClient.class);
       
        //Getting all the CronJobs
        List<CronJob> cronJobList = openshiftClient.batch().v1().cronjobs().inNamespace(namespace).list().getItems();

        //Getting all the TriggerBindings
        List<TriggerBinding> tbList =  tknClient.v1beta1().triggerBindings().inNamespace(namespace).list().getItems();
            
        Map<String, String> bindingParamsToBranch = new HashMap<String, String>();
        List<CronJobData> cronJobs = new ArrayList<>();

        //So there is no duplicate code
        bindingParamsToBranch = bindParamsToBranch(tbList);

        //Listing the current OpenShift user
        //System.out.println("Current User: " + openshiftClient.currentUser());

        //Filling out the values for the linked template.
        for(CronJob job : cronJobList){
            CronJobData currentJob = new CronJobData();
            currentJob.name = job.getMetadata().getName();
            currentJob.schedule = job.getSpec().getSchedule();
            currentJob.humanReadableMsg = CronExpressionDescriptor.getDescription(currentJob.schedule);
            String bindingName = currentJob.name + "-binding";
            currentJob.branch = bindingParamsToBranch.get(bindingName);

            //Had to change newer cronjobs to end in cj instead of cronjob. Should clean up 
            if(currentJob.branch == "" || currentJob.branch == null || currentJob.branch.isBlank() || currentJob.branch.isEmpty()){
                //Changing the name back to the old style
                bindingName = currentJob.name.replaceAll("cj", "cronjob") + "-binding";
                currentJob.branch = bindingParamsToBranch.get(bindingName); 
            }
            //Only using Cronjob that start with Gatling
            if(currentJob.name.startsWith("gatling"))
                cronJobs.add(currentJob);
        }
        return Templates.gatlingCronJobData(cronJobs); //Add in the data 
    }

    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/{namespace}/verify/{cronJobName}")
    public String getCronJobSchedule(@RestPath String namespace, @RestPath String cronJobName){
        
        System.out.println("Verify Schedule for Namepace: " + namespace  +" Cronjob: " + cronJobName);
                
        //Checking to see if the given CronJob is in the system. Catching it nicely since the client will just throw a NullPointerException
        try{
            openshiftClient.batch().v1().cronjobs().inNamespace(namespace).withName(cronJobName);
        }
        catch (NullPointerException e){
            return cronJobName + " not found";
        }
       
        return openshiftClient.batch().v1().cronjobs().inNamespace(namespace).withName(cronJobName)
                              .get().getSpec().getSchedule();
       
    
    }

    @GET
    @Path("/{namespace}/dashboard")
    @Produces(MediaType.TEXT_HTML)
    @Blocking //Due to the OpenShiftClient need to make this blocking
    public TemplateInstance getCronJobDashBoard(@RestPath String namespace){
        Instant start = Instant.now(); //Curious to see how long this takes, will take some time
        Set <String> uniqueEnvs = new HashSet<>();
        List<CronJobDashboardData> dashboardData = new ArrayList<>();
        int cronjobCounter = 0; //Count how many CronJob pipelines were displayeds
        HashMap<String, String> pipelineRunToPod;

        //Have to use TektonClient for anything related to pipelines
        TektonClient tknClient = new KubernetesClientBuilder().build().adapt(TektonClient.class);

        //Getting all the pipeline and task runs
        PipelineRunList list = tknClient.v1beta1().pipelineRuns().inNamespace(namespace).list();
        TaskRunList taskRunList = tknClient.v1beta1().taskRuns().inNamespace(namespace).list();

        //Getting all the TaskRuns from the TakeRunList
        List <TaskRun> taskRuns = taskRunList.getItems();
        
        //Getting all the pipelineRuns from the PipeLineRunList
        List <PipelineRun> pipleRunList = list.getItems();

        System.out.println("Getting " + pipleRunList.size() + " pipeline runs and data on OpenShift for namespace: " + namespace);
        //Mapping the pod a given TaskRun was exeucted on 
        pipelineRunToPod = mapPodToRun(taskRuns);
    
        for(PipelineRun pipleLineRun: pipleRunList){
            String runPod = "";
            int removeStart = pipleLineRun.getMetadata().getName().indexOf("-tt-");
            if(removeStart == -1) //Manually run pipelines will have not have -tt-**** on the end so we can skip them. 
                continue;

               
            runPod = pipelineRunToPod.get(pipleLineRun.getMetadata().getName());
            if (runPod == null){
                System.out.println(pipleLineRun.getMetadata().getName() + " was not found."); 
                continue; 
            }
                
            CronJobDashboardData data = new CronJobDashboardData(); 
            data.name = pipleLineRun.getMetadata().getName().substring(0, removeStart);
            
            //Grabbing the logs from the pod
            String runLogs = "";
            try{ //If a test gets cancelled the pod instantly goes away and no logs causing a null pointer error. 
                runLogs = openshiftClient.pods().inNamespace(namespace).withName(runPod).inContainer(STEP_CONTAINER).getLog(true);
            } catch (KubernetesClientException e){
                System.out.println("Could not get logs for pod: " + runPod + " for cronjob: "+ pipleLineRun.getMetadata().getName());
                //Catching the Exception but still want to display it
                runLogs="";
            }
             
            //Pulling the Selenium Test run data out of the logs. 
            Matcher matcherTimeStart = patternTimeStart.matcher(runLogs);
            Matcher matcherEnv = patternEnv.matcher(data.name);
                
           //Getting the time it took to run the selenium test
            if(matcherTimeStart.find()){
                data.runTime = runLogs.substring(matcherTimeStart.end(), matcherTimeStart.end() + 11).replace("[", "");
            }
            else
                data.runTime=""; //Generally this is the case if the test is running
                
            //Getting the Enviroment the code was run on
            if(matcherEnv.find()){
                data.env = data.name.substring(matcherEnv.start(), matcherEnv.end());
                uniqueEnvs.add(data.env); //Getting only the Enviroment Names that I need once
            }
       
             //Setting the type of Pipeline that was run. It always the text before the first -
            int typeIndexNameEnd = pipleLineRun.getMetadata().getName().indexOf("-");
            data.type = pipleLineRun.getMetadata().getName().substring(0, typeIndexNameEnd);

            //Removing tailing cronjob from name to make it cleaner    
            data.name = data.name.replaceAll("-confjob",""); //Had a typo in a eailer build of the file generator. Files could still be around
            data.name = data.name.replaceAll("-cronjob","");

            //Getting the releaseBranch from cronjob name
            data.releaseBranch = getReleaseBranchFromName(data.name);
            
            //Creating link to piplerun logs and hosting the files
            //The -- is how I seperate the dash in the cronjob name and the pod name. Will be used later to get run logs from a pod for a specific run
            data.runLink = "/openshift/" + namespace + "/download/"+ data.name + "--" + runPod; 

            List<Condition> pipelineConditions =  pipleLineRun.getStatus().getConditions();
            //There should only be one pipeline conditions. No idea why it was made as a list
            Condition pipelineCondition = pipelineConditions.get(0);
            data.result = pipelineCondition.getReason();
            data.lastTransitionTime = createReadableDate(pipelineCondition.getLastTransitionTime());
           
            //Setting the color and message
            data = getColorStatusAndMsg(data, runLogs, namespace, runPod); 
            cronjobCounter++;
            dashboardData.add(data);

            //TODO Create a ArrayList of all the unique env, pass that to the Template instance, and use that to only have to create one
            //Like it will be a for loop of unique array list, but then under it will if env == headername write out
            //Would take out extra HTML loops. Need to Dynamically write the DataTable.js javascript
        }
        List<String> uniqueEnvNameList = new ArrayList<>(uniqueEnvs);
        String dataTableJS = createDataTableLoadingJS(uniqueEnvNameList);
        Collections.sort(dashboardData, nameSorter); //Sorting everything by name 
        Collections.sort(dashboardData, releaseBranchSorter); //Sorting everything by  name of the release branch
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        System.out.printf("getCronJobDashBoard took %d milliseconds to complete", elapsedMs);
        System.out.println(" Rendering Dashboard with " + cronjobCounter + " Selenium Test Run Results.");
        
        return  Templates.cronJobDashboard(dashboardData).data("dataTableJS", dataTableJS);
    }

    @GET
    @Path("/{namespace}/download/{testNameWithPodName}")
    public Response downloadLogsFromPod(@RestPath String namespace, @RestPath String testNameWithPodName){
        String[] testNamePodName = testNameWithPodName.split("--");
        String testName = testNamePodName[0];
        String podName = testNamePodName[1];
        String projectDir = System.getProperty("user.dir");

        File fileLocationAndFolder = new File(projectDir + File.separator + testName + "-logs.txt");
        //Getting the runLogs for that Seleniun Build Step of of that test
        String runLogs = openshiftClient.pods().inNamespace(namespace).withName(podName).inContainer(STEP_CONTAINER).getLog(true);
        java.nio.file.Path fileLocation = Paths.get(projectDir, testName + "-logs.txt");
        
        try {
            Files.write(fileLocation, runLogs.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return Response.ok(fileLocationAndFolder, MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"+ " + testName +"-logs.txt\"")
                        .build();       
    }  


    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/{namespace}/listSeleniumReports/{type}")
    public TemplateInstance  listSeleniumReports(@RestPath String namespace, @RestPath String type){

        //type shold be cronjob, users,all
        List<CronJobReports> reportList = new ArrayList<>();
        //List<String> pipleRunNames = listSubFolders(pipelinePVCMountPath);
        //Goes pipelinePVCMountPath/<cj or users>indivialRunsName/date/*.tar.gz and *.html
        if(type.equals("cj") || type.equals("users")){
            reportList = createCronJobReportFromFolder(type);
        }else{
            //Basically just listed both cj and users reports
            reportList = createCronJobReportFromFolder("cj");
            reportList.addAll(createCronJobReportFromFolder("type"));
        }

        /*
        for(String pipleRunName: pipleRunNames){
            System.out.println("Searching for subfolders of: " + pipelinePVCMountPath + "/" + type + "/" + pipleRunName);
            List<String> indivialRuns = listSubFolders(pipelinePVCMountPath + "/" + pipleRunName);
            for(String indivialRun:indivialRuns ){
                CronJobReports cronJobReport = new CronJobReports();
                String fullPath = pipelinePVCMountPath + "/" + pipleRunName + "/" + indivialRun; //Creating the URL to use later
                String urlPath = "/reports" + "/" + pipleRunName + "/" + indivialRun;
                System.out.println("Finding files in: " + pipelinePVCMountPath + "/" + pipleRunName + "/" + indivialRun);
                HashMap<String, String> zipHtmlLog = findFiles(fullPath);
            
                cronJobReport.name=pipleRunName;
                cronJobReport.lastRunDate=indivialRun;
                cronJobReport.reportUrl=urlPath + "/" + "html/" + zipHtmlLog.get("html");
                cronJobReport.zipUrl=urlPath + "/" + "zip/" + zipHtmlLog.get("zip");
                cronJobReport.logUrl=urlPath + "/" + "log/" + zipHtmlLog.get("log");
                reportList.add(cronJobReport);
            }
        } */
        //String dataTableJS = createDataTableLoadingJS(pipleRunNames);
        return Templates.cronJobReportHistory(reportList);//.data("dataTableJS", dataTableJS);
    }
    //TODO Move the following methods to their own helper class to clean up code

    private List<CronJobReports> createCronJobReportFromFolder(String type){
        List<String> pipleRunNames = listSubFolders(pipelinePVCMountPath + "/" + type);
        List<CronJobReports> reportList = new ArrayList<>();
        for(String pipleRunName: pipleRunNames){
            System.out.println("Searching for subfolders of: " + pipelinePVCMountPath + "/" + type + "/" + pipleRunName);
            List<String> indivialRuns = listSubFolders(pipelinePVCMountPath + "/" + pipleRunName);
            for(String indivialRun:indivialRuns ){
                CronJobReports cronJobReport = new CronJobReports();
                String fullPath = pipelinePVCMountPath + "/" + type + "/" + pipleRunName + "/" + indivialRun; //Creating the URL to use later
                String urlPath = "/reports" + "/"  + type + "/" + pipleRunName + "/" + indivialRun;
                System.out.println("Finding files in: " + pipelinePVCMountPath + "/"  + type + "/" + pipleRunName + "/" + indivialRun);
                HashMap<String, String> zipHtmlLog = findFiles(fullPath);
            
                cronJobReport.name=pipleRunName;
                cronJobReport.lastRunDate=indivialRun;
                cronJobReport.reportUrl=urlPath + "/" + "html/" + zipHtmlLog.get("html");
                cronJobReport.zipUrl=urlPath + "/" + "zip/" + zipHtmlLog.get("zip");
                cronJobReport.logUrl=urlPath + "/" + "log/" + zipHtmlLog.get("log");
                reportList.add(cronJobReport);
            }
        }
        return reportList;

    }
    /**
     * Searches through the TriggerBindings to find the release Branch associated with the given CronJob
     * @param tbList
     * @return A Map that binds a cronjob name to its Trigger Binding releaseBranch value
     */
    private Map<String,String> bindParamsToBranch(List<TriggerBinding> tbList){
        Map<String, String> bindingParamsToBranch = new HashMap<String, String>();

        //Easier to just grab the releaseBranch all once and just map them to the name of the TriggerBinding
        for(TriggerBinding tb : tbList){
            List<Param> params =  tb.getSpec().getParams();
            for(Param param: params){
                 if(param.getName().equals("releaseBranch")){
                     bindingParamsToBranch.put(tb.getMetadata().getName(), param.getValue());
                 }
            }
         }
        return bindingParamsToBranch;
    }
   
    /**
     * Figures out what to color the Pipeline Run and what message to display
     * @param data 
     * @param runLogs 
     * @param namespace
     * @param runPod
     * @return
     */
    private CronJobDashboardData getColorStatusAndMsg(CronJobDashboardData data, String runLogs, String namespace, String runPod){

        /**
         * Red - job failed, no results
           Orange - job did not complete due to exception, partial results
           Yellow - job completed, some tests failed
           Green - all tests passed
           Gray - Running
         * 
         */
        String doubleCheck;
        Matcher matcherTestRun = patternTestRun.matcher(runLogs);
        Matcher matcherBuildFailed = patternBuildFailed.matcher(runLogs);
        Matcher matcherCRIOError = patternCRIOError.matcher(runLogs); //This is a extreme edge case that can happen to all jobs on a node. 
        Matcher matcherNoTestRun = patternNoTestRun.matcher(runLogs);
        if(matcherTestRun.find() && !data.result.equals("Failed")){
            doubleCheck = runLogs.substring(matcherTestRun.start(), matcherTestRun.end());     
            if(doubleCheck.equalsIgnoreCase(RAN_BUT_FAILED) && !data.result.equals("Failed")){
                data.msg = findPassedFailedFromZipLogs(namespace, runPod,true);
                data.color = ORANGE; //Orange Didn't pass but didn't totally fail
                data.failedTests = getFailedTests(runLogs);
            }
            else if(doubleCheck.contains("Failures: 0")){
                data.msg =  findPassedFailedFromZipLogs(namespace, runPod,false);
                data.color = GREEN;
            }
            else{
                data.msg =  findPassedFailedFromZipLogs(namespace, runPod,false);
                data.color = YELLOW;
                data.failedTests = getFailedTests(runLogs);
            }        
        }
        else if(matcherBuildFailed.find()){
            data.msg = BUILD_FAILURE;
            data.color = RED; 
        }
        else if(data.result.equals(RUNNING)){
            data.msg = "";
            data.color = GRAY;
        }
        else if(data.result.equals(CANCELLED)){
            data.msg = "";
            data.color = GRAY;
            data.runLink = ""; //Can't get the logs for cancelled pod
        }
        else if(matcherNoTestRun.find() && runLogs.contains(BUILD_SUCCESS)){// Ran but no test were actually run (and did not get a exception), so nothing to zip up and send. 
            data.color = ORANGE;
            data.msg = NO_TEST_RUN_MSG;
        }
        else{
            data.msg = CRITICAL_FAILURE; //Didn't even run any Selenium Tests  
            data.color = RED; 
        }

        //Doing a double check for issue with the CRI-O system on a pod/node. This is a edge case that comes up red but could have worked. 
        //Need to have the testers check the email on information about the run. 
        if(matcherCRIOError.find()){
            data.msg = CRIO_MSG;
            data.color = GRAY;
        }

        return data;
    }

    /**
     * Creates a easer to read date from the last Transaction time 
     * @param date The date to be converted
     * @return A easer to read date
     */
    private String createReadableDate(String date){

        //Just in case the cronjob has not run yet and the last Transaction time is null or empty/blank. 
        if(date == null || date.isBlank())
            return "";

        // Parse the string to an Instant object
        Instant instant = Instant.parse(date);

        // Create a DateTimeFormatter with the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm MM-dd-yyyy z").withZone(ZoneId.of("America/New_York"));

        // Format the Instant to a readable string
        return formatter.format(instant);

    }

    /**
     * Maps the Pod that ran the pipeline run to the Name of the Pipeline Run in order to get the logs of the pod
     * @param taskRuns A array of all the TaskRuns 
     * @return A Hashmap that is maps the pod that a given PipelineRun was executed on 
     */
    private HashMap<String, String> mapPodToRun(List <TaskRun> taskRuns){
        HashMap<String, String> podToRunTask = new HashMap<String, String>();
        System.out.print("Starting map to TaskRun");
        for(TaskRun taskRun : taskRuns){
            String key = taskRun.getMetadata().getLabels().get("tekton.dev/pipelineRun");
            String value = taskRun.getStatus().getPodName();    
            System.out.println("key: " + key + " value: " + value);
            podToRunTask.put(key, value);
        }
        return podToRunTask;
    }


    /**
     * What is basically happening here is that the mvn test ran but hit a exception at some point after running a bunch of test.  Instead of show any test that actually ran before the exception mvn just bails out and shows 0 across the board
     * So grabbing the logs of the next step from the pod and finding how many actually ran, passed, and failed using the zip logs
     * Turns out this result string is more useful then the straight Selenium Test Result because it does not show how many test passed only how many Ran, Failed, or Skipped
     * @param namespace
     * @param runPod
     * @param exceptionFound
     * @return
     */
    private String findPassedFailedFromZipLogs(String namespace, String runPod, boolean exceptionFound){
        //Abusing the fact that zip displays the files its zipping to find the number of Pass/Failed images generated
        String zipLogs = openshiftClient.pods().inNamespace(namespace).withName(runPod).inContainer(STEP_ZIP_FILES).getLog(true);
        int passedCount = StringUtils.countMatches(zipLogs, PASSED);
        int failedCount = StringUtils.countMatches(zipLogs, FAILED);
        if(exceptionFound)
            return String.format(RUN_BUT_FAILED_MSG, passedCount + failedCount, passedCount, failedCount);
        else
            return String.format(TEST_RUN, passedCount + failedCount, passedCount, failedCount);
    }

    /**
     * Finds the selenium test that failed during a mvn test. NOTE: If there is a exception mvn test will not list out the test that failed. 
     * Will have to use the zip output to find which pass/failed. 
     * @param runLogs
     * @return
     */
    private String getFailedTests(String runLogs){

        int failureStart = runLogs.indexOf(TEST_FAILURE_START);
        if(failureStart > 0){
            int failureEnd = runLogs.indexOf(TEST_FAILURE_END, failureStart);
            //Basically removing the [ERROR] Failures: from the string
            String failuresMsg = runLogs.substring(failureStart + TEST_FAILURE_START.length(), failureEnd);
            int infoStart= failuresMsg.indexOf(INFO);
            //Remove INFO itself from the failure message
            String finalFailureMsg = failuresMsg.substring(0, infoStart);
            //This remove the extra little bit of endline and space above and below the failure msgs. 
            finalFailureMsg = finalFailureMsg.substring(2, finalFailureMsg.length()-1);
            //System.out.println(finalFailureMsg);
            return finalFailureMsg;
        }
        return "";

    }

    /**
     * Gets the release branch (to be used for sorting) from the name of the cronjob
     * @param cronjobName
     * @return
     */
    private String getReleaseBranchFromName(String cronjobName){

        //Since all cronjob names follow the format of CLEAN_GROUPS-URL-CLEAN_RELEASE_BRANCH we know everything after test<number>- is the release branch name
        int startPosition = cronjobName.indexOf("test") + 6;
        return cronjobName.substring(startPosition, cronjobName.length());
    }

   
    private List<String>  listSubFolders(String parentFolderLocation){
        List<String> folderNames = new ArrayList<>();
        File parentFolder = new File(parentFolderLocation);
        if (parentFolder.exists() && parentFolder.isDirectory()) {
            File[] files = parentFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        folderNames.add(file.getName());
                    }
                }
            }
        }
        return folderNames;
    }

    private HashMap<String, String> findFiles(String folderPath) {
        HashMap<String, String> result = new HashMap<>();
        File folder = new File(folderPath);
        
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("Invalid folder path");
        }

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName().toLowerCase();
                    if (fileName.endsWith(".tar.gz") && !result.containsKey("zip")) {
                        result.put("zip", fileName);
                    } else if (fileName.endsWith(".html") && !result.containsKey("html")) {
                        result.put("html", fileName);
                    }
                    if(fileName.endsWith(".log") && !result.containsKey("log")){
                        result.put("log", fileName);
                    }
                    if (result.size() == 3) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    private String createDataTableLoadingJS(List<String> headerNames){
        
        String loadDataTables = "";
        for(String name: headerNames){
            loadDataTables =  loadDataTables + JS_REPEAT_AND_REPLACE.replace("REPLACE", name);
            loadDataTables = loadDataTables + "\n";
        }
        return JS_START + loadDataTables + JS_END;
    }

    private boolean skipOrNot(String type, String pipelineRunName){
        switch (type) {
            case "cronjob":
                if(pipelineRunName.endsWith("-cj"))
                    return false;
                else 
                    return true;
            case "users":
                if(!pipelineRunName.endsWith("-cj"))
                    return false;
                else 
                    return true;
            case "all":
                return false;
            default:
                return false;
        }

    }


}

