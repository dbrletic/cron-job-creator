package ffm.cms.openshift;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import ffm.cms.model.CronJobDashboardData;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.triggers.v1beta1.Param;
import io.fabric8.tekton.triggers.v1beta1.TriggerBinding;
import jakarta.inject.Inject;


public class OpenshiftUtility {

    @Inject //Generic OpenShift client
    private OpenShiftClient openshiftClient;

    final private static String CRIO_MSG = "Unable to get logs. Check email for status of run.";
    final private static String CRITICAL_FAILURE = "Critical Failure. Selenium test did not run or had exception.";
    final private static String BUILD_FAILURE = "Compliation error. Check logs for errors.";
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

    private static Pattern patternTestRun = Pattern.compile("Tests run: \\d+, Failures: \\d+, Errors: \\d+, Skipped: \\d+");
    private static Pattern patternBuildFailed = Pattern.compile("COMPILATION ERROR");
    private static Pattern patternCRIOError = Pattern.compile(CRI_O_ERROR);
    private static Pattern patternNoTestRun = Pattern.compile(NO_TEST_RUN);

    final private static String JS_START = "$(document).ready( function () {";
    final private static String JS_END = " });";
    final private static String JS_REPEAT_AND_REPLACE ="var REAPLCE = new DataTable('#REPLACE', {paging: false } );";


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

    /**
     * Creates a javascript method that loads all the tables on a page to use with DataTable.js
     * @param headerNames The names of all the tables 
     * @return 
     */
    private String createDataTableLoadingJS(List<String> headerNames){
        
        String loadDataTables = "";
        for(String name: headerNames){
            loadDataTables =  loadDataTables + JS_REPEAT_AND_REPLACE.replace("REPLACE", name);
            loadDataTables = loadDataTables + "\n";
        }
        return JS_START + loadDataTables + JS_END;
    }
    
}
