package ffm.cms;

import ffm.cms.model.CronJobDashboardData;
import ffm.cms.model.CronJobData;
import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.kubernetes.api.model.OwnerReference;
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
import jakarta.ws.rs.core.MediaType;
import net.redhogs.cronparser.CronExpressionDescriptor;
import io.fabric8.tekton.client.*;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunList;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunList;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.triggers.v1beta1.Param;
import io.fabric8.tekton.triggers.v1beta1.TriggerBinding;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestPath;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@Path("/openshift")
public class OpenshiftResource {

    @Inject //Generic OpenShift client
    private OpenShiftClient openshiftClient;
    //Creating a OpenShiftClient with logged in creditals. 
    //private OpenShiftClient loggedInOpenShiftClient;

    @ConfigProperty(name = "upload.directory")
    private String UPLOAD_DIR;

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
        System.out.println("Current User: " + openshiftClient.currentUser());

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
                System.out.println("Looking for: " + bindingName);
                currentJob.branch = bindingParamsToBranch.get(bindingName); 
            }
            //Setting the type
            int typeIndexNameEnd = currentJob.name.indexOf("-");
            currentJob.type=currentJob.name.substring(0, typeIndexNameEnd);
            System.out.println("Job name: " + currentJob.name + " Job Type: " + currentJob.type);

            cronJobs.add(currentJob);
        }
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
        System.out.println("Current User: " + openshiftClient.currentUser());

        //Filling out the values for the linked template.
        for(CronJob job : cronJobList){
            CronJobData currentJob = new CronJobData();
            currentJob.name = job.getMetadata().getName();
            currentJob.schedule = job.getSpec().getSchedule();
            currentJob.humanReadableMsg = CronExpressionDescriptor.getDescription(currentJob.schedule);
            String bindingName = currentJob.name + "-binding";
            currentJob.branch = bindingParamsToBranch.get(bindingName);
            //System.out.println("Verify - " + currentJob.name + ": " + currentJob.branch);
            //Had to change newer cronjobs to end in cj instead of cronjob. Should clean up 
            if(currentJob.branch == "" || currentJob.branch == null || currentJob.branch.isBlank() || currentJob.branch.isEmpty()){
                //Changing the name back to the old style
                bindingName = currentJob.name.replaceAll("cj", "cronjob") + "-binding";
                //System.out.println("Looking for: " + bindingName);
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
        System.out.println("Current User: " + openshiftClient.currentUser());
        
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
        List<CronJobDashboardData> dashboardData = new ArrayList<>();
        
        System.out.println("Getting all pipeline runs on OpenShift: ");

        //Have to use TektonClient for anything related to pipelines
        TektonClient tknClient = new KubernetesClientBuilder().build().adapt(TektonClient.class);
        PipelineRunList list = tknClient.v1beta1().pipelineRuns().inNamespace(namespace).list();
        TaskRunList taskRunList = tknClient.v1beta1().taskRuns().inNamespace(namespace).list();

        System.out.println("Listing TaskRuns");
        List <TaskRun> taskRuns = taskRunList.getItems();
        
        
        

        
       
        //Getting all the pipelineRuns
        List <PipelineRun> pipleRunList = list.getItems();
        System.out.println("Listing PipelineRuns");
        for(PipelineRun pipleLineRun: pipleRunList){
            String runPod = "";
            String runUUID = pipleLineRun.getMetadata().getUid();

            /**
            * Get all the TaskRuns
            * Get the UUID of the PipelineRun 
            * Check if the UUID Of the  PipelineRun is the same of the current taskRun, if so get the podName of that TaskRun
            * Get the logs using the podname while using the specific step you want as the container
            */ 
            for(TaskRun taskRun : taskRuns){
                if(taskRun.getOwnerReferenceFor(runUUID).isPresent()){
                    runPod = taskRun.getStatus().getPodName();                 
                }
            }

            System.out.println(pipleLineRun.getMetadata().getName() + "run on pod " + runPod);
            
          
            CronJobDashboardData data = new CronJobDashboardData(); 
            int removeStart = pipleLineRun.getMetadata().getName().indexOf("-tt-");
            if(removeStart != -1){ //Manually run pipelines will have not have -tt-**** on the end
                data.name = pipleLineRun.getMetadata().getName().substring(0, removeStart);
                String runLogs = openshiftClient.pods().inNamespace(namespace).withName(runPod).inContainer("step-build-and-run-selenium-tests").getLog(true);
                System.out.println("Got logs, size is: " + runLogs.length());
            }
            else
                data.name = pipleLineRun.getMetadata().getName();





            //Removing tailing cronjob from name to make it cleaner    
            data.name = data.name.replaceAll("-confjob",""); //Had a typo in the file generator 
            data.name = data.name.replaceAll("-cronjob","");
            List<Condition> pipelineConditions =  pipleLineRun.getStatus().getConditions();
        
            //There should only be one pipeline conditions. No idea why it was made as a list
            Condition pipelineCondition = pipelineConditions.get(0);

            //Creating link to piplerun logs
            data.runLink = openshiftClient.getOpenshiftUrl() + "k8s/ns/" + namespace + "/tekton.dev~v1beta1~PipelineRun/" + pipleLineRun.getMetadata().getName() + "/logs";

            int typeIndexNameEnd = data.name.indexOf("-");
            data.msg = pipelineCondition.getMessage();
            data.result = pipelineCondition.getReason();
            data.type = data.name.substring(0,typeIndexNameEnd);
            data.lastTransitionTime = createReadableData(pipelineCondition.getLastTransitionTime());
            data.color = getColorStatus(data.result);
            
            if(removeStart != -1) //Only adding in pipeline run data that were created from cronjobs. Manually runs dont have the -cronjob-tt-**** on the end
                dashboardData.add(data);
            System.out.println("-----------------");
            
        }
        return  Templates.cronJobDashboard(dashboardData);
    }


    /**
     * Searches through the TriggerBindings to find the release Branch associated with the given CronJob
     * @param tbList
     * @return
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
     * Sets the color to use as the background of the Results cell
     * @param result The result status of the pipeline run
     * @return The color to use 
     */
    private String getColorStatus(String result){

        switch (result) {
            case "Succeeded":
                return "#69ff33"; // Green
            case "Running":
                return "#f6ff7a"; // Yellow
            case "Failed":
                return "#ff4763"; // Red
            default:
                return "white";
        }
    }

    /**
     * Creates a easer to read date from the last Transaction time 
     * @param date The date to be converted
     * @return A easer to read date
     */
    private String createReadableData(String date){

        // Parse the string to an Instant object
        Instant instant = Instant.parse(date);

        // Create a DateTimeFormatter with the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm z MM-dd-yyyy").withZone(ZoneId.systemDefault());

        // Format the Instant to a readable string
        return formatter.format(instant);

    }

}
