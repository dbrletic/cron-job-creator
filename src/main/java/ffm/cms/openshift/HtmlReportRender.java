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

/**
 * Takes in a REST request and return either a HTML page, image, or zip file located on the supplied PVC mount to render a Selenium Report through the Dashboard
 * @author dbrletic
 */
@ApplicationScoped
@RegisterRestClient
@Path("/reports")
public class HtmlReportRender {

    @Inject
    @ConfigProperty(name = "quarkus.openshift.mounts.pipeline-storage.path")
    private String pipelinePVCMountPath;
    
     /**
     * Find the given HTML file or jpeg file on the PVC Mount
     * What happens is that the HTML file is loaded first and then reaches out for the associated jpegs to complete the report
     * @param pipeLineRunName The name of the pipeline run
     * @param indivialRun The individual run, in the format of its completed time stamp
     * @param html The name of the file to look for
     * @return
     */
    @GET
    @Path("/{type}/{pipeLineRunName}/{indivialRun}/html/{html}")
    @Produces(MediaType.TEXT_HTML)
    public Response getHtmlPage(@RestPath String type, @RestPath String pipeLineRunName, @RestPath String indivialRun, @RestPath String html) {
        
        String htmlLookup = pipelinePVCMountPath + "/" + type + "/" + pipeLineRunName + "/" + indivialRun +"/" + html;
        System.out.println("Looking up html file: " + htmlLookup);
        java.nio.file.Path path = Paths.get(htmlLookup);
        try {
            
            if(html.contains(".jpeg")){
                return Response.ok(path.toFile()).build();
            }
            else {
                String htmlContent = Files.readString(path);
                return Response.ok(htmlContent).type("text/html").build();
            }       
            
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error reading the HTML file").build();
        }
    }

    /**
     * Finds and returns a given zip file of a report on the PVC Mount
    * @param pipeLineRunName The name of the pipeline run
     * @param indivialRun The individual run, in the format of its completed time stamp
     * @param filename The zip file name to look for
     * @return
     */
    @GET
    @Path("/{type}/{pipeLineRunName}/{indivialRun}/zip/{filename}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getFile(@RestPath String type, @RestPath String pipeLineRunName, @RestPath String indivialRun, @RestPath String filename) {
        
        String FILE_BASE_PATH = pipelinePVCMountPath + "/" + type + "/" + pipeLineRunName + "/" + indivialRun;
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

     /**
     * Finds and returns a given log file of a report on the PVC Mount. Due to trying to render it with all the spacing in the browser had to slightly change rendering the file
    * @param pipeLineRunName The name of the pipeline run
     * @param indivialRun The individual run, in the format of its completed time stamp
     * @param filename The log file name to look for
     * @return
     */
    @GET
    @Path("/{type}/{pipeLineRunName}/{indivialRun}/log/{filename}")
    @Produces(MediaType.TEXT_HTML)
    public String  getLogFile(@RestPath String type, @RestPath String pipeLineRunName, @RestPath String indivialRun, @RestPath String filename) {
        
        String FILE_BASE_PATH = pipelinePVCMountPath + "/" + type + "/" + pipeLineRunName + "/" + indivialRun;
        java.nio.file.Path filePath = Paths.get(FILE_BASE_PATH, filename);

        System.out.println("Looking for log at " + filePath);
        if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
            try {
                String logContent = Files.readString(filePath);
                String escapedLogContent = escapeHtml(logContent);
                return "<html><head><style>pre { white-space: pre-wrap; }</style></head><body><pre>" + escapedLogContent + "</pre></body></html>";
            } catch (IOException e) {
                return "<html><body>Error reading log file</body></html>";
            }
        } else {
            return "<html><body>File not Found.</body></html>";
        }
    }

    private String escapeHtml(String str) {
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
