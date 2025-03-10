package openshift.selenium.openshift;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestPath;

import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBinding;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRefBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunBuilder;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import openshift.selenium.model.FFEStartPipeline;
import openshift.selenium.model.ReportData;
import openshift.selenium.model.ReportDataList;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
@RegisterRestClient
@Path("/pipeline")
public class PipelineResource {
    
    @ConfigProperty(name = "selenium.pipeline.name")
    private String openshiftSeleniumPipelineName; //The name of the pipeline to kick off 

    @ConfigProperty(name = "quarkus.openshift.mounts.pipeline-storage.path")
    private String pipelinePVCMountPath;

    @ConfigProperty(name = "selenium.tags.file.name")
    private String seleniumTagsFileName;

    private final static String SELENIUM_GRID_BROWSER = "box";
    private Pattern patternEnv = Pattern.compile("test\\d+");

    // Define the formatter for the UTC input format and ET ouput format
    private DateTimeFormatter inputUTCFormatter = DateTimeFormatter.ofPattern("mm-HH-dd-MM-yyyy");
    private DateTimeFormatter outputETFormatter = DateTimeFormatter.ofPattern("HH:mm MM-dd-yyyy z");

    private static final Logger LOGGER = Logger.getLogger(PipelineResource.class);

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance cronJobReportHistory(List<ReportDataList> cronJobReportsMasterList, List<String> uniqueEnvs);
    }

    /**
     * Takes in the POST data and starts a new pipeline in the given namespace 
     * @param namespace Namespace to run the pipeline in
     * @param data All the data need to kick off a selenium pipeline
     * @return The name of the pipeline to return back to the system. 
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN) 
    @Blocking
    @Path("{namespace}/startRun")
    public String startPipelineRun(@RestPath String namespace, @Valid FFEStartPipeline data) {
       
        TektonClient tknClient = new KubernetesClientBuilder().build().adapt(TektonClient.class); //Uses the Kubeconfig file to set this. Make sure to have the right ServiceAccount in the Deployment!
        PipelineRun createdPipelineRun = tknClient.v1beta1().pipelineRuns().inNamespace(namespace).resource(createPipelineRun(data,namespace)).create();
        LOGGER.info("Kicking off new pipeline " + createdPipelineRun.getMetadata().getName() + " in namespace " + namespace  + "based upon " + openshiftSeleniumPipelineName);
        return createdPipelineRun.getMetadata().getName();
    }

    /**
     * Creates a report dashboard of a given type from the saved run PVC
     * @param namespace Current namespace of the project
     * @param type The type of reports to get, either cronjobs (cj), users, or all. If defaults to all if anything besides cj or users
     * @return
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/{namespace}/listSeleniumReports/{type}")
    public TemplateInstance listSeleniumReports(@RestPath String namespace, @RestPath String type){
        //type shold be cj, users,all. Defaults to all if a unknown type is added
        Instant start = Instant.now(); //Curious to see how long this takes, will take some time
      
       
        List<ReportDataList> reportList = new ArrayList<>();
        List<String> runNames;
        Matcher matcherEnv;
        List <String> uniqueEnvs = new ArrayList<>();
        HashSet<String> hashUniqueEnvs = new HashSet<>();
        TreeMap<String,String> seleniumTags = readSeleniumTagFile();
        //Goes pipelinePVCMountPath/<cj or users>/indivialRunsName/date/*.tar.gz, *.html, and *.log
        if(type.equals("cj") || type.equals("users")){
            reportList = createCronJobReportFromFolder(type,seleniumTags);
            runNames = listSubFolders(pipelinePVCMountPath + File.separator+ type);

        }else{
            //Basically just listed both cj and users reports
            reportList = createCronJobReportFromFolder("cj", seleniumTags);
            reportList.addAll(createCronJobReportFromFolder("users", seleniumTags));
            runNames = listSubFolders(pipelinePVCMountPath + File.separator + "cj");
            runNames.addAll(listSubFolders(pipelinePVCMountPath + File.separator + "users"));
        }

        //Find all the unique env names so I can create tables afterwards. 
        for(String name: runNames){
            matcherEnv = patternEnv.matcher(name);
           //Getting the Enviroment the code was run on
           if(matcherEnv.find()){
                String env = name.substring(matcherEnv.start(), matcherEnv.end());
                hashUniqueEnvs.add(env); //Getting only the Enviroment Names that I need once
           }
        }
         
        //Converting the set to a ArrayList for dispalying with Quarkus QUTE
        uniqueEnvs = new ArrayList<>(hashUniqueEnvs);

        //Making sure the names are in order
        Collections.sort(uniqueEnvs);
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        LOGGER.info("listSeleniumReports took " + elapsedMs + "milliseconds to complete");
        
        return Templates.cronJobReportHistory(reportList, uniqueEnvs);
    }

    /**
     * List all the subfolders of a given parent
     * @param parentFolderLocation Parent folder to look through
     * @return
     */
    private List<String>  listSubFolders(String parentFolderLocation){
        List<String> folderNames = new ArrayList<>();
        java.nio.file.Path parentPath = Paths.get(parentFolderLocation);

        try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(parentPath)) {
            // Iterate over each entry in the directory
            for (java.nio.file.Path entry : stream) {
                // Check if the entry is a directory
                if (Files.isDirectory(entry)) {
                    folderNames.add(entry.getFileName().toString());
                }
            }
        }
        catch (IOException e){
            LOGGER.error(e);
        }
        return folderNames;
    }

     /**
     * Creates a readable string date from the folder Name. Converts military time to AM/PM and Month/Day/Year. 
     * @param folderName
     * @return
     */
    private String createDateFromFolderName(String folderName){
        //Format of folder name is %M-%H-%d-%m-%Y""
        //ex: 10-09-28-10-2024
        String[] splitFolderName = folderName.split("-");
        int minutes = Integer.parseInt(splitFolderName[2] );
        int hours = Integer.parseInt(splitFolderName[1]);
         // Convert to 12-hour format
         String period = (hours < 12) ? "AM" : "PM";
         int convertedHour = (hours % 12 == 0) ? 12 : hours % 12;

        String formattedDate = String.format("%d:%02d %s", convertedHour, minutes, period) + " " + splitFolderName[3] + "/" + splitFolderName[2] + "/" + splitFolderName[4];
        LOGGER.debug("Convert from: " + formattedDate + " to " + createDateFromFolderNameAndConvertToET(folderName));
        return formattedDate;
    }

    private String createDateFromFolderNameAndConvertToET(String folderName){
        String utcDateString="";
        //Format of folder name is %M-%H-%d-%m-%Y""
        //ex: 10-09-28-10-2024
        LOGGER.debug("Convert " + folderName + " to ET");
        String[] splitFolderName = folderName.split("-");
        for(String part: splitFolderName){
            utcDateString = utcDateString + part + "-";
        }
        utcDateString = utcDateString.substring(0, utcDateString.length()-1);
        // Parse the UTC date string to LocalDateTime
        LocalDateTime utcDateTime = LocalDateTime.parse(utcDateString, inputUTCFormatter);

        // Convert UTC LocalDateTime to ZonedDateTime in UTC
        ZonedDateTime utcZonedDateTime = utcDateTime.atZone(ZoneId.of("UTC"));
        
        // Convert UTC ZonedDateTime to Eastern Time (ET)
        ZonedDateTime etZonedDateTime = utcZonedDateTime.withZoneSameInstant(ZoneId.of("America/New_York"));

        // Format the ET ZonedDateTime
        String etDateString = etZonedDateTime.format(outputETFormatter);

        return etDateString;
    }

    /**
     * Finds the first zip, html, and log file in a given folder path. There should only be one of each. 
     * @param folderPath The folder path on the OS to search for
     * @return
     */
    private HashMap<String, String> findFiles(String folderPath) {
        HashMap<String, String> result = new HashMap<>();
        java.nio.file.Path path = Paths.get(folderPath);

        try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(path)) {
            for (java.nio.file.Path entry : stream) {
                String fileName = entry.getFileName().toString().toLowerCase();
                if (fileName.endsWith(".tar.gz") && !result.containsKey("zip")) {
                    result.put("zip", fileName);
                } 
                else if (fileName.endsWith(".html") && !result.containsKey("html")) {
                    result.put("html", fileName);
                }
                else if(fileName.endsWith(".log") && !result.containsKey("log")){
                    result.put("log", fileName);
                }
                else if (result.size() == 3) {
                    break; //I found all three of my files I need, break out of searching the rest. 
                }
            }
        }
        catch (IOException e){
            LOGGER.error(e);
        }
        return result;
    }
    
    /**
     * Creates a new pipeline from the supplied data
     * @param data The data to add to the pipeline
     * @param namespace Namespace to run the pipeline in
     * @return A completele Pipeline Item
     */
    private PipelineRun createPipelineRun(FFEStartPipeline data, String namespace){
      
        WorkspaceBinding configWorkspace = new WorkspaceBinding();
        configWorkspace.setName("config-source");
        configWorkspace.setEmptyDir(new EmptyDirVolumeSource());

        //Have to add the release branch to the name, making sure that there are not any slash that could mess up the folder name. 
        String cleanReleaseBranch = data.getReleaseBranch().replace("/", "-");
        cleanReleaseBranch = cleanReleaseBranch.replace("\\", "-");

         //Also have to remove any _ to keep names consistent 
        cleanReleaseBranch = cleanReleaseBranch.replace("_", "-");

        //Also have to remove any . since that is not allowed in the name of a folder
        cleanReleaseBranch = cleanReleaseBranch.replace(".", "-");

        //Same deal as above but with groups
        String cleanGroup = data.getGroups().replace("_", "-");
        //Format for pipelineRunName is CLEAN_GROUPS-URL-CLEAN_RELEASE_BRANCH. Any pipelineRunName kicked off manually will not have -cj on the end
        String pipelineRunName = cleanGroup + "-" + data.getUrl() + "-" + cleanReleaseBranch;

        PipelineRun pipelineRun = new PipelineRunBuilder()
        .withNewMetadata()
            .withGenerateName(openshiftSeleniumPipelineName + "-")//Will add in the random characters after the -
            .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
            .withPipelineRef(new PipelineRefBuilder().withName(openshiftSeleniumPipelineName).build())
            .addNewParam()
                .withName("releaseBranch")
                .withNewValue(data.getReleaseBranch())  
            .endParam()
            .addNewParam()
                .withName("userName")
                .withNewValue(data.getUserNameFFM())  
            .endParam()
            .addNewParam()
                .withName("userPassword")
                .withNewValue(data.getUserPassword())  
            .endParam()
            .addNewParam()
                .withName("groups")
                .withNewValue(data.getGroups())  
            .endParam()
            .addNewParam()
                .withName("url")
                .withNewValue(data.getUrl())  
            .endParam()
            .addNewParam()
                .withName("browser")
                .withNewValue(SELENIUM_GRID_BROWSER)  // This is always going to be box to run on SeleniumGrid
            .endParam()
            .addNewParam()
                .withName("seleniumTestEmailList")
                .withNewValue(data.getSeleniumTestEmailList())  
            .endParam()
            .addNewParam()
                .withName("logs")
                .withNewValue(data.getLogs())  
            .endParam()
            .addNewParam()
                .withName("mvnArgs")
                .withNewValue(data.getMvnArgs())  
            .endParam()
            .addNewParam()
                .withName("pipelineRunName")
                .withNewValue(pipelineRunName)  
            .endParam()
        .withWorkspaces(Collections.singletonList(configWorkspace))
        .endSpec()
        .build(); 
        
        return pipelineRun;
    }

    /**
     * Creates a ArrayList of cronJobReports based upon the type (cj or users). Searchs through the PVCMountPath to figure out all the subfolders that contain reports. 
     * @param type Which folder type to go into, either cj or users
     * @return
     */
    private List<ReportDataList> createCronJobReportFromFolder(String type, TreeMap<String, String> seleniumTags){
        List<String> pipelineRunNames = listSubFolders(pipelinePVCMountPath + "/" + type);
        List<ReportDataList> reportDataMasterList = new ArrayList<>();
        for(String pipelineRunName: pipelineRunNames){
            //Finding which env the test was run from
            Matcher matcherEnv = patternEnv.matcher(pipelineRunName);
            String currentEnv="";
            if(matcherEnv.find()){ //This should always ben found but just making sure. 
                currentEnv = pipelineRunName.substring(matcherEnv.start(), matcherEnv.end());
            }
            //System.out.println("Searching for subfolders of: " + pipelinePVCMountPath + "/" + type + "/" + pipelineRunName);
            List<String> indivialRuns = listSubFolders(pipelinePVCMountPath + "/" + type + "/" + pipelineRunName);//Each pipelineRunName is a folder with the date being the subfolder that contains all the information. 
            ReportDataList currentReportDataList = new ReportDataList();
            //Setting the Display name, if not found defaults to the pipeline run name
            if(seleniumTags.containsKey(pipelineRunName)){
                if(seleniumTags.get(pipelineRunName).isBlank())
                    currentReportDataList.displayName = pipelineRunName; //There is a scenario where the key  is in place but the value is blank
                else
                    currentReportDataList.displayName = seleniumTags.get(pipelineRunName);
            }else
                currentReportDataList.displayName = pipelineRunName;
            currentReportDataList.runName=pipelineRunName;
            for(String indivialRun:indivialRuns ){
                ReportData currentReport = new ReportData();
                String fullPath = pipelinePVCMountPath + "/" + type + "/" + pipelineRunName + "/" + indivialRun; //Creating the URL to use later
                String urlPath = "/reports" + "/"  + type + "/" + pipelineRunName + "/" + indivialRun;
                //System.out.println("Finding files in: " + pipelinePVCMountPath + "/"  + type + "/" + pipelineRunName + "/" + indivialRun);
                HashMap<String, String> zipHtmlLog = findFiles(fullPath);
                //Getting a decent format for the date to display
                currentReport.lastRunDate=createDateFromFolderName(indivialRun);
                currentReport.reportUrl=urlPath + "/" + "html/" + zipHtmlLog.get("html");
                currentReport.zipUrl=urlPath + "/" + "zip/" + zipHtmlLog.get("zip");
                currentReport.logUrl=urlPath + "/" + "log/" + zipHtmlLog.get("log");
                currentReport.env = currentEnv;
                //Getting the Enviroment the code was run on
                currentReportDataList.reportData.add(currentReport);
            }
            currentReportDataList.env = currentEnv;
            //currentReportDataList.reportData = sortReportsByDate(currentReportDataList.reportData);
            reportDataMasterList.add(currentReportDataList);
        }
        return reportDataMasterList;
    }

    private TreeMap<String, String> readSeleniumTagFile(){
        TreeMap<String, String> seleniumTagPairs =  new TreeMap<>();

        String seleniumTagPath = pipelinePVCMountPath + File.separator + seleniumTagsFileName;
        //String seleniumTagPath = projectDir + File.separator + seleniumTagsFileName;
        // Path to the file
        java.nio.file.Path filePath = java.nio.file.Path.of(seleniumTagPath);

         // Read all lines from the file
        try{
            List<String> lines = Files.readAllLines(filePath);
            // Process each line to extract key-value pairs
            for (String line : lines) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    String key = parts[0].trim();
                    String value = parts[1];
                    seleniumTagPairs.put(key, value);
                }
            }
            return seleniumTagPairs;
        } catch (IOException e) {
            LOGGER.error(e);
        }
        //Should only return a blank map if gotten here
        return seleniumTagPairs;
    }
    /**
     * Simple method that creates data to test the page without having to actually read a PVC
     * @return
    
     //Leaving this here to help create dummy data. 
    private static ArrayList<ReportDataList> getReportDataListDummyData() {
        // Create a list to hold multiple ReportDataList objects
        ArrayList<ReportDataList> reportDataLists = new ArrayList<>();

        // Create and populate ReportDataList entries
        for (int i = 0; i < 5; i++) {
            // Create a new ReportDataList
            ReportDataList reportDataList = new ReportDataList();

            // Set runName and env
            reportDataList.runName = "Run" + (i + 1);
            reportDataList.env = "test" + (i + 1);

            // Create and add ReportData entries to the reportDataList
            for (int j = 0; j < 3; j++) {
                // Create a new ReportData entry
                ReportData reportData = new ReportData();
                reportData.lastRunDate = "2024-11-0" + (j + 1);
                reportData.reportUrl = "http://example.com/report" + (j + 1);
                reportData.zipUrl = "http://example.com/report" + (j + 1) + ".zip";
                reportData.logUrl = "http://example.com/report" + (j + 1) + ".log";
                reportData.env = "test" + (i + 1);

                // Add the ReportData entry to the ReportDataList
                reportDataList.reportData.add(reportData);
            }

            // Add the populated ReportDataList to the result list
            reportDataLists.add(reportDataList);
        }

        return reportDataLists;
    }
        /**
     * Creates a report based upon the files on the PVC
     * @param namespace Current namespace of the project
     * @param type The type of reports to get, either cronjobs (cj), users, or all. If defaults to all if anything besides cj or users
     * @return 
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/{namespace}/listSeleniumReports/{type}")
    public TemplateInstance  listSeleniumReports(@RestPath String namespace, @RestPath String type){

        //type shold be cj, users,all. Defaults to all if a unknown type is added
        Instant start = Instant.now(); //Curious to see how long this takes, will take some time
        List<CronJobReports> reportList = new ArrayList<>();
        List<String> runNames;
        Matcher matcherEnv;
        List <String> uniqueEnvs = new ArrayList<>();
        //Goes pipelinePVCMountPath/<cj or users>indivialRunsName/date/*.tar.gz, *.html, and *.log
        if(type.equals("cj") || type.equals("users")){
            reportList = createCronJobReportFromFolder(type);
            runNames = listSubFolders(pipelinePVCMountPath + File.separator+ type);

        }else{
            //Basically just listed both cj and users reports
            reportList = createCronJobReportFromFolder("cj");
            reportList.addAll(createCronJobReportFromFolder("users"));
            runNames = listSubFolders(pipelinePVCMountPath + File.separator + "cj");
            runNames.addAll(listSubFolders(pipelinePVCMountPath + File.separator + "users"));
        }

        //Find all the unique env names so I can create tables afterwards. 
        for(String name: runNames){
            matcherEnv = patternEnv.matcher(name);
           //Getting the Enviroment the code was run on
           if(matcherEnv.find()){
                String env = name.substring(matcherEnv.start(), matcherEnv.end());
                uniqueEnvs.add(env); //Getting only the Enviroment Names that I need once
           }
        }
        
        // Remote the duplicate using a HashSet
        HashSet<String> hashUniqueEnvs = new HashSet<>(uniqueEnvs);
        
        //Converting the set Back to a ArrayList for dispalying with Quarkus QUTE
        uniqueEnvs = new ArrayList<>(hashUniqueEnvs);

        //Making sure the names are in order
        Collections.sort(uniqueEnvs);
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        System.out.printf("listSeleniumReports took %d milliseconds to complete", elapsedMs);
        return Templates.cronJobReportHistory(reportList, uniqueEnvs);
    }
    */
     //Saving this code in case I ever have to add another field to a bunch of files again. 
    /** 
    @GET
    @Path("/update-files")
    public String updateFiles() throws IOException{
        System.out.println("Scanning local files at " + directoryPath);

        List<java.nio.file.Path> files = Files.walk(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());

        for (java.nio.file.Path file : files) {
            String fileName = file.getFileName().toString();
            System.out.println("Workong on " + fileName);
            if (fileName.startsWith("tt-")) {
                Files.writeString(file, updateTT(file));
            } else if (fileName.startsWith("tb-")) {
                Files.writeString(file, updateTB(file));
            }
        }
        return "Edited all the files";

    }

    private String updateTT(java.nio.file.Path file  ){
        String currentFile;
        String updatedFile;
        String paramsStart = "- description: The Email list for results\r\n" + //
                            "      name: seleniumTestEmailList";
        String templateStart ="- name: seleniumTestEmailList\r\n" + //
                            "            value: $(tt.params.seleniumTestEmailList)";
        String addParam =   "    - description: The name of the pipelineRun to save to the PVC\r\n" + //
                            "      name: pipelineRunName";
        String addTemplate = "          - name: pipelineRunName\r\n" + //
                            "            value: $(tt.params.pipelineRunName)";
        try {
            currentFile = Files.readString(file);
        } catch (IOException e) {
            return "Got a error";
        }
        int startIndex = currentFile.indexOf(paramsStart);
        updatedFile = new StringBuilder(currentFile).insert(startIndex + paramsStart.length() + 1,  addParam).toString();
        startIndex = updatedFile.indexOf(templateStart);
        updatedFile = new StringBuilder(updatedFile).insert(startIndex + templateStart.length() +1, addTemplate).toString();

        return updatedFile;
    }

    private String updateTB(java.nio.file.Path file  ){
        String currentFile = "";
        String valueStart = "- name: seleniumTestEmailList\r\n" + //
                        "      value: MPMS.Leads@test.com,MPMS.Test@test.com";
        try {
            currentFile = Files.readString(file);
        } catch (IOException e) {
            return "Got a error";
        }
        int nameIndex = currentFile.indexOf("name");
        int endPosition = currentFile.indexOf('\n', nameIndex + 6);
        int valueStartIndex = currentFile.indexOf(valueStart);
        String cronjobName = currentFile.substring(nameIndex + 6, endPosition);
        cronjobName = cronjobName.replace("-binding", "");
        String valueToAdd = "\n    - name: pipelineRunName\r\n" + //
                            "      value: " + cronjobName;                   

        String updatedFile = new StringBuilder(currentFile).insert(valueStartIndex + valueStart.length(), valueToAdd).toString();                 

        return updatedFile;
    }  */

     /**
     * Creates a ArrayList of cronJobReports based upon the type (cj or users). Searchs through the PVCMountPath to figure out all the subfolders that contain reports. 
     * @param type Which folder type to go into, either cj or users
     * @return
     
    private List<CronJobReports> createCronJobReportFromFolder(String type){
        List<String> pipelineRunNames = listSubFolders(pipelinePVCMountPath + "/" + type);
        List<CronJobReports> reportList = new ArrayList<>();
        for(String pipelineRunName: pipelineRunNames){

            //System.out.println("Searching for subfolders of: " + pipelinePVCMountPath + "/" + type + "/" + pipelineRunName);
            List<String> indivialRuns = listSubFolders(pipelinePVCMountPath + "/" + type + "/" + pipelineRunName);//Each pipelineRunName is a folder with the date being the subfolder that contains all the information. 
            for(String indivialRun:indivialRuns ){
                CronJobReports cronJobReport = new CronJobReports();
                String fullPath = pipelinePVCMountPath + "/" + type + "/" + pipelineRunName + "/" + indivialRun; //Creating the URL to use later
                String urlPath = "/reports" + "/"  + type + "/" + pipelineRunName + "/" + indivialRun;
                //System.out.println("Finding files in: " + pipelinePVCMountPath + "/"  + type + "/" + pipelineRunName + "/" + indivialRun);
                HashMap<String, String> zipHtmlLog = findFiles(fullPath);
                
                cronJobReport.name=pipelineRunName;
                cronJobReport.lastRunDate=createDateFromFolderName(indivialRun);
                cronJobReport.reportUrl=urlPath + "/" + "html/" + zipHtmlLog.get("html");
                cronJobReport.zipUrl=urlPath + "/" + "zip/" + zipHtmlLog.get("zip");
                cronJobReport.logUrl=urlPath + "/" + "log/" + zipHtmlLog.get("log");

                //Finding which env the test was run from
                Matcher matcherEnv = patternEnv.matcher(pipelineRunName);
                //Getting the Enviroment the code was run on
                if(matcherEnv.find()){
                    cronJobReport.env = pipelineRunName.substring(matcherEnv.start(), matcherEnv.end());
                }    
                reportList.add(cronJobReport);
            }
        } 
        return reportList; 
    }
    */    
}