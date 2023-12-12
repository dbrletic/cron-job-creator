package ffm.cms;

import io.fabric8.openshift.client.OpenShiftClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@ApplicationScoped
public class openshiftResource {

    @Inject
    private OpenShiftClient openshiftClient;

    @GET()
    @Path("/listcronjobs")
    public void getCurrentCronJobs(){
        String test = openshiftClient.resource("cronjobs").inNamespace("tester-pipelines").toString();
        System.out.println("Resource");
        System.out.println(test);
        String secondTest = openshiftClient.resourceList("cronjobs").inNamespace("tester-pipelines").toString();
        System.out.println("Resource List");
        System.out.println(secondTest);
    }
}
