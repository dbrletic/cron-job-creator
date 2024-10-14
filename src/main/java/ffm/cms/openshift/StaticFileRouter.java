package ffm.cms.openshift;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class StaticFileRouter {
    
    @Inject
    @ConfigProperty(name = "quarkus.openshift.mounts.pipeline-storage.path")
    String pipelineMountPath;
    
    void configureRouter(@Observes Router router) {
        System.out.println("Configuring to look at static reports starting at: " + pipelineMountPath);
        router.route("/static/pipeline/reports/*")
              .handler(StaticHandler.create(pipelineMountPath + "/"));
    }
}
