package ffm.cms;

import ffm.cms.model.CronJobDashboardData;
import ffm.cms.model.CronJobData;
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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles getting information from OpenShift like Cronjob Schedules, logs, etc
 * @author dbrletic
 */
@ApplicationScoped
@Path("/openshift")
public class OpenshiftResource {

    @Inject //Generic OpenShift client
    private OpenShiftClient openshiftClient; //Make sure to add a ServiceAccount to the deployment that has access to the namespace that has the pipeline runs. 
    
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
    final private static String TEST_FAILURE_START = "[INFO] Results:\n" + //
                "[INFO] \n" + //
                "[ERROR] Failures:";
    final private static String TEST_FAILURE_END = "[INFO] \n" + //
                "[ERROR] Tests run:";
  
    final private static String GREEN = "#69ff33"; //Green
    final private static String YELLOW = "#EBF58A"; //Yellow
    final private static String RED = "#ff4763"; //Red
    final private static String GRAY = "#f6f6f6"; //Gray
    final private static String ORANGE = "#F0C476"; //Orange



    private Pattern patternTestRun = Pattern.compile("Tests run: \\d+, Failures: \\d+, Errors: \\d+, Skipped: \\d+");
    private Pattern patternBuildFailed = Pattern.compile("COMPILATION ERROR");
    private Pattern patternTimeStart = Pattern.compile("Total time:");
    private Pattern patternEnv = Pattern.compile("test\\d+");
    private Pattern patternCRIOError = Pattern.compile(CRI_O_ERROR);
    private Pattern patternStartOfFailedTests = Pattern.compile(TEST_FAILURE_START);
    private Pattern patternEndOfFailedTests = Pattern.compile(TEST_FAILURE_END);
    private Pattern patternNoTestRun = Pattern.compile(NO_TEST_RUN);

    //Sorts CronJobDashboardData by their names
    Comparator<CronJobDashboardData> nameSorter = (a, b) -> a.name.compareToIgnoreCase(b.name);

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance cronJobData(List<CronJobData> cronJobs);
        public static native TemplateInstance gatlingCronJobData(List<CronJobData> cronJobs);
        public static native TemplateInstance cronJobDashboard(List<CronJobDashboardData> cronJobs);
    }

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
        return Templates.gatlingCronJobData(cronJobs);
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
            }
       
             //Setting the type of Pipeline that was run. It always the text before the first -
            int typeIndexNameEnd = pipleLineRun.getMetadata().getName().indexOf("-");
            data.type = pipleLineRun.getMetadata().getName().substring(0, typeIndexNameEnd);

            //Removing tailing cronjob from name to make it cleaner    
            data.name = data.name.replaceAll("-confjob",""); //Had a typo in a eailer build of the file generator. Files could still be around
            data.name = data.name.replaceAll("-cronjob","");

            
            //Creating link to piplerun logs and hosting the files
            //The -- is how I seperate the dash in the cronjob name and the pod name. Will be used later to get run logs from a pod for a specific run
            data.runLink = "/openshift/" + namespace + "/download/"+ data.name + "--" + runPod; 

            List<Condition> pipelineConditions =  pipleLineRun.getStatus().getConditions();
            //There should only be one pipeline conditions. No idea why it was made as a list
            Condition pipelineCondition = pipelineConditions.get(0);
            data.result = pipelineCondition.getReason();
            data.lastTransitionTime = createReadableData(pipelineCondition.getLastTransitionTime());
           
            //Setting the color and message
            data = getColorStatusAndMsg(data, runLogs, namespace, runPod); 
            cronjobCounter++;
            dashboardData.add(data);
        }
       
        Collections.sort(dashboardData, nameSorter); //Sorting everything by  name of the cronjob
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        System.out.printf("getCronJobDashBoard took %d milliseconds to complete", elapsedMs);
        System.out.println(" Rendering Dashboard with " + cronjobCounter + " Selenium Test Run Results.");
        
        return  Templates.cronJobDashboard(dashboardData);
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

    //TODO Move the following methods to their own helper class to clean up code

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
        Matcher matcherTestFailureStart = patternStartOfFailedTests.matcher(runLogs);
        Matcher matcherTestFailureEnd = patternEndOfFailedTests.matcher(runLogs);
        Matcher matcherNoTestRun = patternNoTestRun.matcher(runLogs);
        System.out.println(data.name + " " + matcherTestFailureStart.matches() + " " + matcherTestFailureEnd.matches());
        if(matcherTestRun.find() && !data.result.equals("Failed")){
            doubleCheck = runLogs.substring(matcherTestRun.start(), matcherTestRun.end());     
            if(doubleCheck.equalsIgnoreCase(RAN_BUT_FAILED) && !data.result.equals("Failed")){
                data.msg = findPassedFailedFromZipLogs(namespace, runPod,true);
                data.color = ORANGE; //Orange Didn't pass but didn't totally fail
                if(matcherTestFailureStart.matches() &&  matcherTestFailureEnd.matches())
                    getFailedTests(runLogs, matcherTestFailureStart.end(), matcherTestFailureEnd.start());
            }
            else if(doubleCheck.contains("Failures: 0")){
                data.msg =  findPassedFailedFromZipLogs(namespace, runPod,false);
                data.color = GREEN;
            }
            else{
                data.msg =  findPassedFailedFromZipLogs(namespace, runPod,false);
                data.color = YELLOW;
                if(matcherTestFailureStart.matches() &&  matcherTestFailureEnd.matches())
                    getFailedTests(runLogs, matcherTestFailureStart.end(), matcherTestFailureEnd.start()); 
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
    private String createReadableData(String date){

        //Just in case the cronjob has not run yet and the last Transaction time is null or empty/blank. 
        if(date == null || date.isBlank())
            return "";

        // Parse the string to an Instant object
        Instant instant = Instant.parse(date);

        // Create a DateTimeFormatter with the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm z MM-dd-yyyy").withZone(ZoneId.systemDefault());

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
        for(TaskRun taskRun : taskRuns){
            String key = taskRun.getMetadata().getLabels().get("tekton.dev/pipelineRun");
            String value = taskRun.getStatus().getPodName();    
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
    
    private String getFailedTests(String runLogs, int startPosition, int finishPosition){

        System.out.println("Start: " + startPosition + " Finished: " + finishPosition);
        return "";

    }

}
