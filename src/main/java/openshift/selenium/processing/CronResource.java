package openshift.selenium.processing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import static com.cronutils.model.CronType.UNIX;

import com.cronutils.model.Cron;
import java.util.Base64;
import java.util.HashMap;

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
import jakarta.ws.rs.core.Response;
import openshift.selenium.model.ExcelUploadForm;
import openshift.selenium.model.FFEData;
import openshift.selenium.model.FFEGatlingData;
import openshift.selenium.model.ScheduleJob;
import openshift.selenium.model.UpdateCronJobSchedule;

/**
 * Handles receiving request to create new cronjob files
 * @author dbrletic
 */
@Path("/ffe-cronjob")
@ApplicationScoped
public class CronResource {

    @Inject
    private ProcessCronJob cronjobHandler;

    @Inject
    @ConfigProperty(name = "selenium.excel.example.file")
    private String EXCEL_EXAMPLE_FILE_NAME;

    private static final Logger LOGGER = Logger.getLogger(CronResource.class);

    /**
     * Returns the excel-example hosted on the system to make creating a mass create file easier. 
     * @return
     */
    @GET
    @Path("/excel-example")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") // MIME type for .xlsx
    public Response downloadExcelFile() {
        InputStream fileStream = Thread.currentThread()
        .getContextClassLoader()
        .getResourceAsStream(EXCEL_EXAMPLE_FILE_NAME);

        return Response.ok(fileStream)
                .header("Content-Disposition", "attachment; filename=\"massCreateFormatExample.xlsx\"")
                .build();

    }

    /**
     * Creates the yaml files required to add a Selenium Pipeline kicked off by a OpenShift Cronjob. 
     * @param data
     * @return
     * @throws IOException
     * @throws ParseException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Blocking
    public Response createFiles(@Valid FFEData data) throws IOException, ParseException{
        LOGGER.info("Starting up process for " + data.getGroups() + "-" + data.getUrl()) ;
        LOGGER.debug(data.toString());
        if(data.getReleaseBranch().length() + data.getGroups().length() + data.getUrl().length() > 52){
            //Since ReleaseBranch, Groups, and Url are used to create names of the jobs there is as long the combo can be. 
            return Response.status(400, "Release Branch, Groups, and Url are too long.").build();
        }
        List<String> newFilesLocation = new ArrayList<String>();
        String projectDir = System.getProperty("user.dir");
        String zipFileLocation = projectDir + File.separator + data.getGroups() + "-" + data.getUrl() + ".zip";

        //Have to add the release branch to the name, making sure that there are not any slash that could mess up the file name. 
        String cleanReleaseBranch = data.getReleaseBranch().replace("/", "-");
        cleanReleaseBranch = cleanReleaseBranch.replace("\\", "-");

         //Also have to remove any _ since that is not allowed in the name of a cronjob file
        cleanReleaseBranch = cleanReleaseBranch.replace("_", "-");

        //Also have to remove any . since that is not allowed in the meta name of a cronjob file
        cleanReleaseBranch = cleanReleaseBranch.replace(".", "-").toLowerCase();

        String cleanGroup = data.getGroups().replace("_", "-").toLowerCase();
        
        try{
            newFilesLocation.add(cronjobHandler.processCronjob(data.getCronJobSchedule(),data.getGroups(), data.getUrl(), cleanReleaseBranch, cleanGroup));
            newFilesLocation.add(cronjobHandler.processEventListener(data.getGroups(), data.getUrl(), cleanReleaseBranch,cleanGroup));
            newFilesLocation.add(cronjobHandler.processTriggerBinding(data.getGroups(), data.getUrl(), data.getReleaseBranch(), data.getUserNameFFM(), data.getUserPassword(), data.getBrowser(), data.getSeleniumTestEmailList(), cleanReleaseBranch,cleanGroup));
            newFilesLocation.add(cronjobHandler.processTriggerTemplate(data.getGroups(), data.getUrl(), cleanReleaseBranch,cleanGroup));
        } catch (IOException e){
            LOGGER.error(e.getMessage());
            LOGGER.error(e.getStackTrace());
        }

        //Zipping up files
        File downloadZip = zipUpFiles(newFilesLocation,zipFileLocation);
        
        LOGGER.info("Add to response: " + zipFileLocation);
        LOGGER.info("Zip is made: " + downloadZip.isFile());

        return Response
            .ok(FileUtils.readFileToByteArray(downloadZip))
            .type("application/zip")
            .header("Content-Disposition", "attachment; filename=\"filename.zip\"")
            .build();
    }

    /**
     * Creates the yaml files to kick off a Gatling Pipeline run from a OpenShift Cronjob.  
     * @param data
     * @return
     * @throws IOException
     * @throws ParseException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Blocking
    @Path("gatling")
    public Response createGatlingFiles(FFEGatlingData data) throws IOException, ParseException{
        LOGGER.info("Starting up Gatling process for " + data.getUrl() + "-" + data.getReleaseBranch()) ;
        LOGGER.debug(data.toString());
        List<String> newFilesLocation = new ArrayList<String>();
        String projectDir = System.getProperty("user.dir");
        String zipFileLocation = projectDir + File.separator + "gatling-" + data.getUrl() + ".zip";

        //Have to add the release branch to the name, making sure that there are not any slash that could mess up the file name. 
        String cleanReleaseBranch = data.getReleaseBranch().replace("/", "-");
        cleanReleaseBranch = cleanReleaseBranch.replace("\\", "-");

         //Also have to remove any _ since that is not allowed in the name of a cronjob file
        cleanReleaseBranch = cleanReleaseBranch.replace("_", "-");

        //Also have to remove any . since that is not allowed in the meta name of a cronjob file
        cleanReleaseBranch = cleanReleaseBranch.replace(".", "-");

        try{
            newFilesLocation.add(cronjobHandler.processGatlingCronjob(data.getCronJobSchedule(), data.getUrl(), cleanReleaseBranch, data.getType()));
            newFilesLocation.add(cronjobHandler.processGatlingEventListener(data.getUrl(), cleanReleaseBranch, data.getType()));
            newFilesLocation.add(cronjobHandler.processGatlingTriggerBinding(data.getUrl(), data.getReleaseBranch(), data.getType(), data.getGatlingTestEmailList(), cleanReleaseBranch));
            newFilesLocation.add(cronjobHandler.processGatlingTriggerTemplate(data.getUrl(), cleanReleaseBranch, data.getType()));
        } catch (IOException e){
            LOGGER.error(e.getMessage());
            LOGGER.error(e.getStackTrace());
        }

         //Zipping up files
        File downloadZip = zipUpFiles(newFilesLocation, zipFileLocation);
       
        LOGGER.info("Add to response: " + zipFileLocation);
        LOGGER.info("Zip is made: " + downloadZip.isFile());

        return Response
            .ok(FileUtils.readFileToByteArray(downloadZip))
            .type("application/zip")
            .header("Content-Disposition", "attachment; filename=\"filename.zip\"")
            .build();
    }

    /**
     * Creates the files to update a single CronJob Schedule
     * @param update
     * @return
     * @throws IOException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Blocking
    @Path("/update")
    public Response updateCronJobs(@Valid UpdateCronJobSchedule update) throws IOException{
        Map<String,String> cronJobsToUpdate = update.getPairs();
        List<String> newFilesLocation = new ArrayList<String>();
        String projectDir = System.getProperty("user.dir");
        String zipFileLocation = projectDir + File.separator + "update-" + generateFiveCharUUID() + ".zip";
        String invalidCronMsg = "";
        Cron currentCron; 
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
        CronParser cronParser = new CronParser(cronDefinition);
       
        //Validating all of the Cronjobs before writing files
         for(Map.Entry<String, String> entry : cronJobsToUpdate.entrySet()){
            try{
                currentCron = cronParser.parse(entry.getValue());
                currentCron.validate();//This is dumb, why does it not just say true or false? At least it gives a reason the cron expression is invalid
            } catch(java.lang.IllegalArgumentException  e){
                LOGGER.info(entry.getKey() +":" + e.getMessage());
                invalidCronMsg = invalidCronMsg + "\n" + entry.getKey() + ": " + e.getMessage();
            }
        }

        if(!invalidCronMsg.isBlank()){
            LOGGER.info(invalidCronMsg);
            return Response.ok().status(400, invalidCronMsg).entity(invalidCronMsg).header("errorMsg", invalidCronMsg).build();       
        } 
            

        for(Map.Entry<String, String> entry : cronJobsToUpdate.entrySet()){
            LOGGER.info("Cronjob: " + entry.getKey() + " New Schedule: " + entry.getValue()); 
            try{
                newFilesLocation.add(cronjobHandler.updateCronJOb(entry.getKey(),  entry.getValue()));
            } catch (IOException | ParseException e){
                LOGGER.error(e.getMessage());
                LOGGER.error(e.getStackTrace());
            }
            
            
        }
      
        File downloadZip = zipUpFiles(newFilesLocation,zipFileLocation);
        LOGGER.info("Add to response: " + zipFileLocation);
        LOGGER.info("Zip is made: " + downloadZip.isFile());

        try {
            return Response
                .ok(FileUtils.readFileToByteArray(downloadZip))
                .type("application/zip")
                .header("Content-Disposition", "attachment; filename=\"filename.zip\"")
                .build();
        } catch (IOException e) {
            return Response.serverError().status(500).build();
        }
        
    }

    /**
     * Creates all the yaml files to update the CronJob Schedule for a massive amount of jobs. Since the system is controlled by ArgoCD it is 
     * between to just recreate the Cronjob Schedule yaml then update it directly from the system. 
     * @param jobs All the jobs to update
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/json")
    @Path("/mass-update")
    public Response massUpdate(ScheduleJob[] jobs){
        List<String> newFilesLocation = new ArrayList<String>();
        List<String> failedCronExpressions = new ArrayList<String>();
        for(ScheduleJob job: jobs){
        
            LOGGER.info("Updating: " + job.getJobName() + " with new schedule: " + job.getSchedule());
            if(isValidCronExpression(job.getSchedule().trim())){        
                try{
                    newFilesLocation.add(cronjobHandler.updateCronJOb(job.getJobName(),  job.getSchedule()));
                } catch (IOException | ParseException e){
                    
                    LOGGER.error(e.getMessage());
                    LOGGER.error(e.getStackTrace());
                    return Response.serverError().status(500).entity("Parsing Error of files").build();
                }
            }
            else{
                LOGGER.info("Schedule: " + job.getJobName() + " cron-schedule: " + job.getSchedule() + " is invalid");
                failedCronExpressions.add(job.getJobName() + " schedule (" + job.getSchedule() + ") is invalid");
            }
        }
        String base64Zip="";
        try {
            base64Zip = zipInMemory(newFilesLocation);
        } catch (IOException e) {
            LOGGER.error(e.getStackTrace());
            return Response.serverError().status(500).build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("failedJobs", failedCronExpressions);
        response.put("zipFile", base64Zip);

        return Response.ok(response).build();
    }

    /**
     * Mass creates the EventLister, TriggerBinding, TriggerTemplate, and CronJob yamls for each row in the upload Excel File. 
     * @param form The file to create all the files from. 
     * @return
     * @throws IOException
     * @throws ParseException
     */
    @POST
    @Path("/mass-create")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    public Response uploadExcelFile(ExcelUploadForm form) throws IOException, ParseException{
        LOGGER.info("Creating new schedules from uploaded excel file.");
        List<String> newFilesLocation = new ArrayList<String>();
        List<FFEData> cronjobSchedules = new ArrayList<>();
        List<String> failedCronExpressions = new ArrayList<String>();
        try (InputStream inputStream = form.file) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0); // Read the first sheet
            Iterator<Row> rowIterator = sheet.iterator();
            
            rowIterator.next(); // Skip the header row

            //Reading all the data
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                cronjobSchedules.add(new FFEData(
                    getCellValue(row.getCell(0)),  // Release Branch
                    getCellValue(row.getCell(1)),  // User Name
                    getCellValue(row.getCell(2)),  // User Password
                    getCellValue(row.getCell(3)),  // Groups
                    getCellValue(row.getCell(4)),  // Browser
                    getCellValue(row.getCell(5)),  // Url
                    getCellValue(row.getCell(6)),  // Selenium Test EmailList
                    getCellValue(row.getCell(7))   // Cron Job Schedule
                ));
            }
            workbook.close();
        } catch (Exception e) {
            List<String> error = new ArrayList<>();
            error.add("Error: " + e.getMessage());
            return Response.serverError().build();
        }
        //Creating all the files for each from the upload Excel sheet. 
        for(FFEData data: cronjobSchedules){

            //Have to add the release branch to the name, making sure that there are not any slash that could mess up the file name. 
            String cleanReleaseBranch = data.getReleaseBranch().replace("/", "-");
            cleanReleaseBranch = cleanReleaseBranch.replace("\\", "-");

            //Also have to remove any _ since that is not allowed in the name of a cronjob file
            cleanReleaseBranch = cleanReleaseBranch.replace("_", "-");

            //Also have to remove any . since that is not allowed in the meta name of a cronjob file
            cleanReleaseBranch = cleanReleaseBranch.replace(".", "-");

            String cleanGroup = data.getGroups().replace("_", "-").toLowerCase();

            String openShiftJobName = cleanGroup + "-" + data.getUrl() + "-" + cleanReleaseBranch + "-cj";

            if(isValidCronExpression(data.getCronJobSchedule().trim())){        
                try{
                    newFilesLocation.add(cronjobHandler.processCronjob(data.getCronJobSchedule(),data.getGroups(), data.getUrl(), cleanReleaseBranch, cleanGroup));
                    newFilesLocation.add(cronjobHandler.processEventListener(data.getGroups(), data.getUrl(), cleanReleaseBranch,cleanGroup));
                    newFilesLocation.add(cronjobHandler.processTriggerBinding(data.getGroups(), data.getUrl(), data.getReleaseBranch(), data.getUserNameFFM(), data.getUserPassword(), data.getBrowser(), data.getSeleniumTestEmailList(), cleanReleaseBranch,cleanGroup));
                    newFilesLocation.add(cronjobHandler.processTriggerTemplate(data.getGroups(), data.getUrl(), cleanReleaseBranch,cleanGroup));
                } catch (IOException | ParseException e){
                    LOGGER.error(e.getMessage());
                    LOGGER.error(e.getStackTrace());
                }
            }
            else{
                LOGGER.info("Name: " + openShiftJobName + " cron-schedule: " +  data.getCronJobSchedule() + " is invalid");
                failedCronExpressions.add(openShiftJobName + " schedule (" + data.getCronJobSchedule()+ ") is invalid");
            }
        }

        LOGGER.info("Zipping up " + newFilesLocation.size() + " files.");
        String base64Zip="";
        try {
            base64Zip = zipInMemory(newFilesLocation);
        } catch (Exception e) {
            e.printStackTrace(); // Prints the full stack trace
            LOGGER.error(e.getStackTrace());
            return Response.serverError().status(500).build();
        }
        LOGGER.info("Created " + newFilesLocation.size() + " files. ");
        Map<String, Object> response = new HashMap<>();
        response.put("failedJobs", failedCronExpressions);
        response.put("zipFile", base64Zip);

        return Response.ok(response).build();
    }


    /**
     * Helper method that zips up a bunch of files, delete the zipped file, and returns the location of the newly created zip
     * @param newFilesLocation Array of location of the files to be zipped
     * @param zipFileLocation The final location of the zip file
     * @return Return a File object of the newly created zip file
     */
    private File zipUpFiles(List<String> newFilesLocation, String zipFileLocation){
        FileOutputStream fos;
        ZipOutputStream zipOut;
        try {
            LOGGER.info("Creating zip file: " + zipFileLocation);
            fos = new FileOutputStream(zipFileLocation);
            zipOut = new ZipOutputStream(fos);

            for (String srcFile : newFilesLocation) {
                LOGGER.info("Zipping file: " + srcFile);
                File fileToZip = new File(srcFile);
                FileInputStream fis = new FileInputStream(fileToZip);
                ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                zipOut.putNextEntry(zipEntry);

                byte[] bytes = new byte[1024];
                int length;
                while((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                zipOut.closeEntry();
                fis.close();
            }
            zipOut.flush();
            zipOut.close();
            fos.flush();
            fos.close();

        } catch (IOException e) {
            LOGGER.error(e.getStackTrace());
        }

        // Remove the files in the zip from local file system
        LOGGER.info("Removing files that have been zipped");
        for(String scrFile: newFilesLocation){
            File fileToDelete = FileUtils.getFile(scrFile);
            FileUtils.deleteQuietly(fileToDelete);
        }
        return new File(zipFileLocation);        
    }

    /**
     * Creates a zip file in Memory and returns it as a Base64 Encoded
     * @param newFilesLocation A list of all the file locations to be zip
     * @return A base64 Encoded version of the zip file
     * @throws IOException
     */
    private String zipInMemory(List<String> newFilesLocation) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream  = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream  = new ZipOutputStream(byteArrayOutputStream );
        newFilesLocation = new ArrayList<>(new LinkedHashSet<>(newFilesLocation)); //This remove duplicates which breaks the zipping. 

            for(String srcFile: newFilesLocation){
                
                LOGGER.debug("Zipping file: " + srcFile);
                java.nio.file.Path filePath = Paths.get(srcFile);
                String content = Files.readString(filePath);
                ZipEntry entry = new ZipEntry(srcFile);
                zipOutputStream.putNextEntry(entry);
                zipOutputStream.write(content.getBytes());
                zipOutputStream.closeEntry();
            }
            zipOutputStream.close();
            // Remove the files in the zip from local file system
            LOGGER.info("Removing " + newFilesLocation.size() +" files that have been zipped");
            for(String scrFile: newFilesLocation){
                File fileToDelete = FileUtils.getFile(scrFile);
                FileUtils.deleteQuietly(fileToDelete);
            }

        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    }

    /**
     * Generates a five characer UUID value
     * @return
     */
    private static String generateFiveCharUUID() {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString().replace("-", "");
        return uuidString.substring(0, 5);
    }

   /**
    * Returns the value inside of a Excel file
    * @param cell
    * @return
    */
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }

    /**
     * Checks if a Cron Expression is valid
     * @param cronExpression
     * @return
     */
    public static boolean isValidCronExpression(String cronExpression) {
        try {
            // Define the cron type (e.g., QUARTZ or UNIX)
            CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(UNIX);

            // Create a parser based on the definition
            CronParser parser = new CronParser(cronDefinition);

            // Parse and validate the cron expression
            Cron cron = parser.parse(cronExpression);
            cron.validate(); // Throws an exception if invalid

            return true; // Cron expression is valid
        } catch (Exception e) {
            // Cron expression is invalid
            return false;
        }
    }
}