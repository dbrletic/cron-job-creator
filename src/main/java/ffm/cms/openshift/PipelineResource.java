package ffm.cms.openshift;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;

import ffm.cms.model.CronJobReports;
import ffm.cms.model.FFEStartPipeline;
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
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
@RegisterRestClient
@Path("/pipeline")
public class PipelineResource {
    
    @Inject
    @ConfigProperty(name = "ffe.selenium.pipeline.name")
    private String openshiftSeleniumPipelineName; //The name of the pipeline to kick off 

    @Inject
    @ConfigProperty(name = "quarkus.openshift.mounts.pipeline-storage.path")
    private String pipelinePVCMountPath;

    private final static String SELENIUM_GRID_BROWSER = "box";
    final private static String JS_START = "$(document).ready( function () {";
    final private static String JS_END = " });";
    final private static String JS_REPEAT_AND_REPLACE ="var REAPLCE = new DataTable('#REPLACE', {paging: false } );";

    private Pattern patternEnv = Pattern.compile("test\\d+");

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance cronJobReportHistory(List<CronJobReports> cronJobsReports, String dataTableJS, List<String> uniqueEnvs);
    }

    /**
     * Takes in the POST data and starts a new pipeline in the given namespace 
     * @param namespace Namespace to run the pipeline in
     * @param data All the data need to kick off a selenium pipeline
     * @return The name of the pipeline to return back to the system. 
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Blocking
    @Path("{namespace}/startRun")
    public String startPipelineRun(@RestPath String namespace, @Valid FFEStartPipeline data) {
       
        TektonClient tknClient = new KubernetesClientBuilder().build().adapt(TektonClient.class);
        //PipelineRun createdPipelineRun = tknClient.v1beta1().pipelineRuns().inNamespace(namespace).create(createPipelineRun(data,namespace)); 
        PipelineRun createdPipelineRun = tknClient.v1beta1().pipelineRuns().inNamespace(namespace).resource(createPipelineRun(data,namespace)).create();
        System.out.println("Kicking off new pipeline " + createdPipelineRun.getMetadata().getName() + " in namespace " + namespace  + "based upon " + openshiftSeleniumPipelineName);
        return createdPipelineRun.getMetadata().getName();
    }

    /**
     * Creates a report based upon the files on the PVC
     * @param namespace Current namespace of the project
     * @param type The type of reports to get, either cronjobs (cj), users, or all. If defaults to all if anything besides cj or users
     * @return
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/{namespace}/listSeleniumReports/{type}")
    public TemplateInstance  listSeleniumReports(@RestPath String namespace, @RestPath String type){

        //type shold be cj, users,all
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
        
        String dataTableJS = createDataTableLoadingJS(uniqueEnvs);
        Collections.sort(uniqueEnvs);
        System.out.println(uniqueEnvs);
        System.out.println(dataTableJS);
        return Templates.cronJobReportHistory(reportList, dataTableJS, uniqueEnvs);
    }
     
    /**
     * Creates a ArrayList of cronJobReports based upon the type (cj or users). Searchs through the PVCMountPath to figure out all the subfolders that contain reports. 
     * @param type Which folder type to go into, either cj or users
     * @return
     */
    private List<CronJobReports> createCronJobReportFromFolder(String type){
        List<String> pipelineRunNames = listSubFolders(pipelinePVCMountPath + "/" + type);
        List<CronJobReports> reportList = new ArrayList<>();
        for(String pipelineRunName: pipelineRunNames){
            System.out.println("Searching for subfolders of: " + pipelinePVCMountPath + "/" + type + "/" + pipelineRunName);
            List<String> indivialRuns = listSubFolders(pipelinePVCMountPath + "/" + type + "/" + pipelineRunName);//Each pipelineRunName is a folder with the date being the subfolder that contains all the information. 
            for(String indivialRun:indivialRuns ){
                CronJobReports cronJobReport = new CronJobReports();
                String fullPath = pipelinePVCMountPath + "/" + type + "/" + pipelineRunName + "/" + indivialRun; //Creating the URL to use later
                String urlPath = "/reports" + "/"  + type + "/" + pipelineRunName + "/" + indivialRun;
                System.out.println("Finding files in: " + pipelinePVCMountPath + "/"  + type + "/" + pipelineRunName + "/" + indivialRun);
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

    /**
     * List all the subfolders of a given parent
     * @param parentFolderLocation Parent folder to look through
     * @return
     */
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
        return formattedDate;
    }

    /**
     * Finds the first zip, html, and log file in a given folder path. There should only be one of each. 
     * @param folderPath The folder path on the OS to search for
     * @return
     */
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
            .withGenerateName(openshiftSeleniumPipelineName + "-")
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
                        "      value: FFE.MPMS.Leads@afs.com,FFE.MPMS.Test@afs.com";
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
}