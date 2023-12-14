package ffm.cms;

import ffm.cms.model.CronJobData;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import net.redhogs.cronparser.CronExpressionDescriptor;
import io.fabric8.tekton.client.*;

import org.jboss.resteasy.reactive.RestPath;

import java.text.ParseException;
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
    @Blocking
    public TemplateInstance getCurrentCronJobs(@RestPath String namespace) throws ParseException, Exception{

        // Helpful openShiftClient / kubernetes cheatsheet
        //https://github.com/fabric8io/kubernetes-client/blob/main/doc/CHEATSHEET.md#cronjob

        //https://www.npmjs.com/package/cronstrue
        // To try to get the TiggerBinding https://github.com/fabric8io/kubernetes-client/blob/main/kubernetes-examples/src/main/java/io/fabric8/kubernetes/examples/GenericKubernetesResourceExample.java
        //https://stackoverflow.com/questions/40858456/how-to-display-a-javascript-var-in-html-body


        List<CronJobData> cronJobs = new ArrayList<>();
        List<CronJob> cronJobList = openshiftClient.batch().v1().cronjobs().inNamespace(namespace).list().getItems();
        try (final TektonClient client = new KubernetesClientBuilder().build().adapt(TektonClient.class)) {
            // Do stuff with client
        } 
        System.out.print("Listing Cronjob in Namespace: " + namespace);
        for(CronJob job : cronJobList){
            CronJobData currentJob = new CronJobData();
            currentJob.name = job.getMetadata().getName();
            currentJob.schedule = job.getSpec().getSchedule();
            currentJob.humanReadableMsg = CronExpressionDescriptor.getDescription(currentJob.schedule);
            System.out.println(currentJob.name + ":" + currentJob.schedule );
            cronJobs.add(currentJob);
        }
        return Templates.cronJobData(cronJobs);
    }
}
