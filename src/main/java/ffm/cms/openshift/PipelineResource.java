package ffm.cms.openshift;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;

import ffm.cms.model.FFEStartPipeline;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBinding;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRefBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunBuilder;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import java.util.Collections;
@ApplicationScoped
@RegisterRestClient
@Path("/pipeline")
public class PipelineResource {
    
    @Inject
    @ConfigProperty(name = "ffe.selenium.pipeline.name")
    private String openshiftSeleniumPipelineName; //The name of the pipeline to kick off 

    private final static String SELENIUM_GRID_BROWSER = "box";

    /**
     * Takes in the POST data and starts a new pipeline in the given namespace 
     * @param namespace Namespace to run the pipeline in
     * @param data All the data need to kick off a selenium pipeline
     * @return The name of the pipeline to return back to the system. 
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Blocking
    @Path("{namespace}/startRun")
    public String startPipelineRun(@RestPath String namespace, @Valid FFEStartPipeline data) {
       
        TektonClient tknClient = new KubernetesClientBuilder().build().adapt(TektonClient.class);
        //PipelineRun createdPipelineRun = tknClient.v1beta1().pipelineRuns().inNamespace(namespace).create(createPipelineRun(data,namespace)); 
        PipelineRun createdPipelineRun = tknClient.v1beta1().pipelineRuns().inNamespace(namespace).resource(createPipelineRun(data,namespace)).create();
        System.out.println("Kicking off new pipeline " + createdPipelineRun.getMetadata().getName() + " in namespace " + namespace  + "based upon " + openshiftSeleniumPipelineName);
        return createdPipelineRun.getMetadata().getName();
    }
     

    /**
     * Creates a new pipeline from the supplied data
     * @param data The data to add to the pipeline
     * @param namespace Namespace to run the pipeline in
     * @return A completele Pipeline Item
     */
    private PipelineRun createPipelineRun(FFEStartPipeline data, String namespace){
      
       WorkspaceBinding configWorkspace = new WorkspaceBinding();
       configWorkspace.setName("config-source");
       configWorkspace.setEmptyDir(new EmptyDirVolumeSource());

        PipelineRun pipelineRun = new PipelineRunBuilder()
        .withNewMetadata()
            .withGenerateName(openshiftSeleniumPipelineName + "-")
            .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
            .withPipelineRef(new PipelineRefBuilder().withName(openshiftSeleniumPipelineName).build())
            .addNewParam()
                .withName("releaseBranch")
                .withNewValue(data.getReleaseBranch())  
            .endParam()
            .addNewParam()
                .withName("userName")
                .withNewValue(data.getUserNameFFM())  
            .endParam()
            .addNewParam()
                .withName("userPassword")
                .withNewValue(data.getUserPassword())  
            .endParam()
            .addNewParam()
                .withName("groups")
                .withNewValue(data.getGroups())  
            .endParam()
            .addNewParam()
                .withName("url")
                .withNewValue(data.getUrl())  
            .endParam()
            .addNewParam()
                .withName("browser")
                .withNewValue(SELENIUM_GRID_BROWSER)  // This is always going to be box to run on SeleniumGrid
            .endParam()
            .addNewParam()
                .withName("seleniumTestEmailList")
                .withNewValue(data.getSeleniumTestEmailList())  
            .endParam()
            .addNewParam()
                .withName("logs")
                .withNewValue(data.getLogs())  
            .endParam()
            .addNewParam()
                .withName("mvnArgs")
                .withNewValue(data.getMvnArgs())  
            .endParam()
            .addNewParam()
                .withName("pipelineRunName")
                .withNewValue(data.getPipelineRunName())  
        .endParam()
        .withWorkspaces(Collections.singletonList(configWorkspace))
        .endSpec()
        .build(); 
        
        return pipelineRun;
    }
    
}
