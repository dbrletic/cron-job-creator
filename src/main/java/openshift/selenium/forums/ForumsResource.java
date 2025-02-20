package openshift.selenium.forums;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.logging.Logger;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Handles getting all the forums that will submit information to OpenShift
 * @author dbrletic
 */
@ApplicationScoped
@RegisterRestClient
@Path("/forums")
public class ForumsResource {


    @ConfigProperty(name = "quarkus.openshift.mounts.pipeline-storage.path")
    private String pipelinePVCMountPath;

    @ConfigProperty(name = "selenium.tags.file.name")
    private String seleniumTagsFileName;

    private static final Logger LOGGER = Logger.getLogger(ForumsResource.class);

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance newSeleniumJob();
        public static native TemplateInstance updateCronJobSchedule();
        public static native TemplateInstance massUpdate();
        public static native TemplateInstance startPipeline();
        public static native TemplateInstance seleniumTags(TreeMap<String, String> seleniumTags);
        public static native TemplateInstance massCreate();
    }
    
    @GET
    @Path("/create-job")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance newSeleniumCronJobForum(){
        return Templates.newSeleniumJob();
    }

    @GET
    @Path("/update-job")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance updateSeleniumConjobSchedule(){
        return Templates.updateCronJobSchedule();
    }

    @GET
    @Path("/mass-update")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance massUpdate(){
        return Templates.massUpdate();
    }

    @GET
    @Path("/mass-create")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance masseCreate(){
        return Templates.massCreate();
    }

    @GET
    @Path("/start-pipeline")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance startPipeline(){
        return Templates.startPipeline();
    }

    @GET
    @Path("/selenium-cj-tags")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance seleniumTags(){
        TreeMap<String, String> seleniumTagPairs = readSeleniumTagFile();
        return Templates.seleniumTags(seleniumTagPairs);
    }

    @POST
    @Path("/updateSeleniumTags")
    @Blocking
    @Consumes(MediaType.APPLICATION_JSON) // Expecting JSON data
    public Response submitForm(Map<String, String> formData) {
        //String projectDir = System.getProperty("user.dir");
        //File seleniumTagFilePath = new File(pipelinePVCMountPath + File.separator + seleniumTagsFileName);
        String seleniumTagPath = pipelinePVCMountPath + File.separator + seleniumTagsFileName;
        //String seleniumTagPath = projectDir + File.separator + seleniumTagsFileName;
        LOGGER.info("Writing to file: " + seleniumTagPath);
        
        for(Map.Entry<String, String> entry : formData.entrySet()) {
            LOGGER.info("Key: " + entry.getKey() + " - Value: " + entry.getValue());
        }    
        try (FileWriter writer = new FileWriter(seleniumTagPath, false)) {
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue() + "\n");
            }
            writer.close();
            return Response.ok("Data saved successfully!").build();
        } catch (IOException e) {
            LOGGER.error(e);
            return Response.status(500).entity("Error saving data").build();
        }
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
}
