package ffm.cms.openshift;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;

import ffm.cms.model.FFEStartPipeline;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.tekton.client.TektonClient;
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
import java.util.Random;

@ApplicationScoped
@RegisterRestClient
@Path("/pipeline")
public class PipelineResource {
    
    @Inject //Generic OpenShift client
    private OpenShiftClient openshiftClient; //Make sure to add a ServiceAccount to the deployment that has access to the namespace that has the pipeline runs.  This will automatticaly add in the kubeconfig file that gives the client the needed permissions. 
    
    @Inject
    @ConfigProperty(name = "quarkus.openshift.mounts.pipeline-storage.path")
    private String pipelinePVCMountPath;

    @Inject
    @ConfigProperty(name = "ffe.selenium.pipeline.name")
    private String openshiftSeleniumPipelineName;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Blocking
    @Path("{namespace}/startRun")
    public String startPipelineRun(@RestPath String namespace, @Valid FFEStartPipeline data) {
        TektonClient tknClient = new KubernetesClientBuilder().build().adapt(TektonClient.class);
        PipelineRun createdPipelineRun = tknClient.v1beta1().pipelineRuns().inNamespace(namespace).create(createPipelineRun(data,namespace));
        return createdPipelineRun.getMetadata().getName();
    }

    private static String generateRandomString(int length) {
        String lowercaseLettersAndNumbers = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(lowercaseLettersAndNumbers.length());
            char randomChar = lowercaseLettersAndNumbers.charAt(index);
            sb.append(randomChar);
        }

        return sb.toString();
    }
     
    private PipelineRun createPipelineRun(FFEStartPipeline data, String namespace){
       String pipelineName = openshiftSeleniumPipelineName + "-" + generateRandomString(5);
        
        PipelineRun pipelineRun = new PipelineRunBuilder()
        .withNewMetadata()
            .withGenerateName(pipelineName)
            .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
            .withPipelineRef(new PipelineRefBuilder().withName(openshiftSeleniumPipelineName).build())
            .addNewParam()
                .withName("releaseBranch")
                .withNewValue(data.getReleaseBranch())  // Replace with your branch name
            .endParam()
            .addNewParam()
                .withName("userName")
                .withNewValue(data.getUserNameFFM())  // Replace with your desired name
            .endParam()
            .addNewParam()
                .withName("userPassword")
                .withNewValue(data.getUserPassword())  // Replace with your URL
            .endParam()
            .addNewParam()
                .withName("groups")
                .withNewValue(data.getGroups())  // Replace with your branch name
            .endParam()
            .addNewParam()
                .withName("url")
                .withNewValue(data.getUrl())  // Replace with your desired name
            .endParam()
            .addNewParam()
                .withName("browser")
                .withNewValue(data.getBrowser())  // Replace with your URL
            .endParam()
            .addNewParam()
                .withName("seleniumTestEmailList")
                .withNewValue(data.getSeleniumTestEmailList())  // Replace with your URL
            .endParam()
            .addNewParam()
                .withName("logs")
                .withNewValue(data.getLogs())  // Replace with your URL
        .endParam()
            .addNewParam()
                .withName("mvnArgs")
                .withNewValue(data.getMvnArgs())  // Replace with your URL
            .endParam()
            .addNewParam()
                .withName("pipelineRunName")
                .withNewValue(data.getPipelineRunName())  // Replace with your URL
        .endParam()
        .endSpec()
        .build();
        
        return pipelineRun;
    }
    
}
