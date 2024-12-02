package ffm.selenium.processing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import ffm.selenium.model.FFEData;
import ffm.selenium.model.FFEGatlingData;
import ffm.selenium.model.ScheduleJob;
import ffm.selenium.model.UpdateCronJobSchedule;

import com.cronutils.model.Cron;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Handles receiving request to create new cronjob files
 * @author dbrletic
 */
@Path("/ffe-cronjob")
@ApplicationScoped
public class CronResource {

    @Inject
    private ProcessCronJob cronjobHandler;

    private static final Logger LOGGER = Logger.getLogger(CronResource.class);

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

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/mass-update")
    public Response massUpdate(ScheduleJob[] jobs){
        List<String> newFilesLocation = new ArrayList<String>();
        String projectDir = System.getProperty("user.dir");
        String zipFileLocation = projectDir + File.separator + "update-" + generateFiveCharUUID() + ".zip";
        String updates="";
        for(ScheduleJob job: jobs){
            
            LOGGER.info("Updating: " + job.getJobName() + " with new schedule: " + job.getSchedule());
            updates = updates + "\n" + "Updating: " + job.getJobName() + " with new schedule: " + job.getSchedule();
            try{
                newFilesLocation.add(cronjobHandler.updateCronJOb(job.getJobName(),  job.getSchedule()));
            } catch (IOException | ParseException e){
                
                LOGGER.error(e.getMessage());
                LOGGER.error(e.getStackTrace());
                return Response.serverError().status(500).entity("Parsing Error of files").build();
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
            e.printStackTrace();
        }

        // Remove the files in the zip from local file system
        LOGGER.info("Removing files that have been zipped");
        for(String scrFile: newFilesLocation){
            File fileToDelete = FileUtils.getFile(scrFile);
            FileUtils.deleteQuietly(fileToDelete);
        }
        return new File(zipFileLocation);        
    }

     private static String generateFiveCharUUID() {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString().replace("-", "");
        return uuidString.substring(0, 5);
    }



}
