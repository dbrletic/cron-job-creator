package ffm.cms.openshift;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;

@ApplicationScoped
@RegisterRestClient
@Path("/report")
public class HtmlReportRender {
    
}
