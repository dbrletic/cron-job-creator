package ffm.cms.forums;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.fabric8.openshift.api.model.TemplateInstance;
import io.quarkus.qute.CheckedTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Handles getting all the forums that will submit information to OpenShift
 * @author dbrletic
 */
@ApplicationScoped
@RegisterRestClient
@Path("/forums")
public class ForumsResource {


    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance newSeleniumJob();
        public static native TemplateInstance updateCronJobSchedule();
        public static native TemplateInstance massUpdate();
        public static native TemplateInstance startPipeline();
    }
    
    @GET
    @Path("/createSeleniumCronjob")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance newSeleniumCronJobForum(){
        return Templates.newSeleniumJob();
    }

    @GET
    @Path("/updateCronJobSchedule")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance updateSeleniumConjobSchedule(){
        return Templates.updateCronJobSchedule();
    }

    @GET
    @Path("/massUpdate")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance massUpdate(){
        return Templates.massUpdate();
    }

    @GET
    @Path("/startPipeline")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance startPipeline(){
        return Templates.startPipeline();
    }
}
