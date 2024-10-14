package ffm.cms.openshift;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@RegisterRestClient
@Path("/reports")
public class HtmlReportRender {

    @Inject
    @ConfigProperty(name = "quarkus.openshift.mounts.pipeline-storage.path")
    private String pipelinePVCMountPath;
    
    @GET
    @Path("/{pipeLineRunName}/{indivialRun}/html/{html}")
    @Produces(MediaType.TEXT_HTML)
    public Response getHtmlPage(@RestPath String pipeLineRunName, @RestPath String indivialRun, @RestPath String html) {
        
        String htmlLookup = pipelinePVCMountPath + "/" + pipeLineRunName + "/" + indivialRun +"/" + html;
        System.out.println("Looking up html file: " + htmlLookup);
        java.nio.file.Path path = Paths.get(htmlLookup);
        try {
            String htmlContent = Files.readString(path);
            return Response.ok(htmlContent).type("text/html").build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error reading the HTML file").build();
        }
    }

    @GET
    @Path("/{pipeLineRunName}/{indivialRun}/zip/{filename}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getFile(@RestPath String pipeLineRunName, @RestPath String indivialRun, @RestPath String filename) {
        
        String FILE_BASE_PATH = pipelinePVCMountPath + "/" + pipeLineRunName + "/" + indivialRun;
        java.nio.file.Path filePath = Paths.get(FILE_BASE_PATH, filename);

        System.out.println("Looking for zip at " + filePath);
        if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
            try {
                return Response.ok(Files.readAllBytes(filePath), MediaType.APPLICATION_OCTET_STREAM)
                               .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                               .build();
            } catch (IOException e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                               .entity("File read error").build();
            }
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("File not found").build();
        }
    }

    @GET
    @Path("/{pipeLineRunName}/{indivialRun}/zip/{filename}")
    @Produces("image/jpeg")
    pubic Response getImage(){
        //testing/selenium/pipeline-reports/sreregression-01-test2-py25-version-10-2-test2/16-27-11-10-2024/SRF_Med_007-1011241636-Failed.jpeg
        

    }
    
}
