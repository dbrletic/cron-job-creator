package ffm.cms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

@Path("/ffe-cronjob")
@ApplicationScoped
public class CronResource {

    @Inject
    private ProcessCronJob cronjobHandler;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Blocking
    public Response createFiles(Data data) throws IOException{
        System.out.println("Starting up process");
        System.out.println(data.toString());
        List<String> newFilesLocation = new ArrayList<String>();
        String projectDir = System.getProperty("user.dir");
        String zipFileLocation = projectDir + File.separator + data.getGroups() + "-" + data.getUrl() + ".zip";
        FileOutputStream fos;
        ZipOutputStream zipOut;

        try{
            newFilesLocation.add(cronjobHandler.processCronjob(data.getGroups(), data.getUrl()));
            newFilesLocation.add(cronjobHandler.processEventListener(data.getGroups(), data.getUrl()));
            newFilesLocation.add(cronjobHandler.processTriggerBinding(data.getGroups(), data.getUrl(), data.getReleaseBranch(), data.getUserNameFFM(), data.getUserPassword(), data.getBrowser(), data.getSeleniumTestEmailList()));
            newFilesLocation.add(cronjobHandler.processTriggerTemplate(data.getGroups(), data.getUrl()));
        } catch (IOException e){
            System.out.println(e.getMessage());
            System.out.println(e.getStackTrace());
        }

        //Zipping up files
       
        try {
            System.out.println("Creating zip file: " + zipFileLocation);
            fos = new FileOutputStream(zipFileLocation);
            zipOut = new ZipOutputStream(fos);

            for (String srcFile : newFilesLocation) {
                System.out.println("Zipping file: " + srcFile);
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

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        // Remove the files in the zip from local file system
        System.out.println("Removing files that have been zipped");
        for(String scrFile: newFilesLocation){
            File fileToDelete = FileUtils.getFile(scrFile);
            FileUtils.deleteQuietly(fileToDelete);
        }

        File downloadZip = new File(zipFileLocation);
        

        System.out.println("Add to response: " + zipFileLocation);
       
        System.out.println("Attaching: " + downloadZip.getName());
        System.out.println("Zip is made: " + downloadZip.isFile());

    
        return Response
        .ok(FileUtils.readFileToByteArray(downloadZip))
        .type("application/zip")
        .header("Content-Disposition", "attachment; filename=\"filename.zip\"")
        .build();
    }
    
}
