package ffm.cms.openshift;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@RegisterRestClient
@Path("/testing/selenium/pipeline-reports")
public class ImageWebRender {
    
    @Inject
    @ConfigProperty(name = "quarkus.openshift.mounts.pipeline-storage.path")
    private String pipelinePVCMountPath;

    @GET
    @Path("/{pipeLineRunName}/{indivialRun}/{filename}")
    @Produces("image/jpeg")
    public Response getImage(@RestPath String pipeLineRunName, @RestPath String indivialRun, @RestPath String filename) {
        //Example:
        // /sreregression-01-test2-py25-version-10-2-test2/16-27-11-10-2024/SRF_Med_007-1011241636-Failed.jpeg
        String imageLookUp = pipelinePVCMountPath + "/" + pipeLineRunName + "/" + indivialRun + "/"  + filename;
        java.nio.file.Path imagePath = Paths.get(imageLookUp);
        if (Files.exists(imagePath)) {
            return Response.ok(imagePath.toFile()).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
   

}
