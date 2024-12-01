package ffm.selenium.exception;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance generalException(Exception exception);
    }

    @Override
    public Response toResponse(Exception exception) {
        TemplateInstance instance = Templates.generalException(exception); 
        String renderedPage = instance.render(); // Render template into a string
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR) //Has to return a response so turning the qute template into a string for the response
            .entity(renderedPage)
            .type("text/html")
            .build();
    }
}
