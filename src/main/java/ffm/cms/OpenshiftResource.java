package ffm.cms;

import ffm.cms.model.CronJobData;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestPath;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@Path("/openshift")
public class OpenshiftResource {

    @Inject
    private OpenShiftClient openshiftClient;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance cronJobData(List<CronJobData> cronJobs);
    }


    @GET()
    @Path("/{namespace}/cronjobs")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getCurrentCronJobs(@RestPath String namespace){

        List<CronJobData> cronJobs = new ArrayList<>();
        List<CronJob> cronJobList = openshiftClient.batch().v1().cronjobs().inNamespace(namespace).list().getItems();
        for(CronJob job : cronJobList){
            CronJobData currentJob = new CronJobData();
            currentJob.name = job.getMetadata().getName();
            currentJob.schdule = job.getSpec().getSchedule();
            cronJobs.add(currentJob);
        }
       // String schedule = cronJobList.get(0).getSpec().getSchedule();
        //String name = cronJobList.get(0).getMetadata().getName();
        return Templates.cronJobData(cronJobs);
    }
}
