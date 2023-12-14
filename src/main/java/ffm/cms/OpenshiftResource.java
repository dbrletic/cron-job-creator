package ffm.cms;

import ffm.cms.model.CronJobData;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
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
import io.fabric8.tekton.triggers.v1alpha1.Param;
import io.fabric8.tekton.triggers.v1alpha1.TriggerBinding;

import org.jboss.resteasy.reactive.RestPath;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        
        //Having to use TektonClient for anything related to pipelines
        TektonClient tknClient = new KubernetesClientBuilder().build().adapt(TektonClient.class);
        Map<String, String> bindingParamsToBranch = new HashMap<String, String>();
        List<CronJobData> cronJobs = new ArrayList<>();
        List<CronJob> cronJobList = openshiftClient.batch().v1().cronjobs().inNamespace(namespace).list().getItems();
       
        List<TriggerBinding> tbList =  tknClient.v1alpha1().triggerBindings().inNamespace(namespace).list().getItems();   
        for(TriggerBinding tb : tbList){
           List<Param> params =  tb.getSpec().getParams();
           for(Param param: params){
                if(param.getName().equals("releaseBranch")){
                    bindingParamsToBranch.put(tb.getMetadata().getName(), param.getValue());
                }
           }
        }
        for(CronJob job : cronJobList){
            CronJobData currentJob = new CronJobData();
            currentJob.name = job.getMetadata().getName();
            currentJob.schedule = job.getSpec().getSchedule();
            currentJob.humanReadableMsg = CronExpressionDescriptor.getDescription(currentJob.schedule);
            String bindingName = currentJob.name + "-binding";
            currentJob.branch = bindingParamsToBranch.get(bindingName);
            cronJobs.add(currentJob);
        }
        return Templates.cronJobData(cronJobs);
    }
}
