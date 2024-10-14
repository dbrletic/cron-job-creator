package ffm.cms.openshift;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Routes staic files (htmls, images, etc) from the PVC Mount path to use through the Dashboard
 * Based upon: https://quarkus.io/guides/http-reference
 * @author dbrletic
 */
@ApplicationScoped
public class StaticFileRouter {
    
    @Inject
    @ConfigProperty(name = "quarkus.openshift.mounts.pipeline-storage.path")
    String pipelineMountPath;
    
    void configureRouter(@Observes StartupEvent startupEvent, Router router) {
        router.route("/static/pipeline/reports/*")
              .handler(StaticHandler.create(pipelineMountPath + "/")); //Since there is not a / on the end on the PVC mount
    }
}
