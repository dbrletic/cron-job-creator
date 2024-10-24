package ffm.cms.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FFEStartPipeline {
    
    @NotBlank
    private String releaseBranch;
    @NotBlank
    private String userNameFFM;
    @NotBlank
    private String userPassword;
    @NotBlank
    private String groups;
    @NotBlank
    private String url;
    @NotBlank
    private String seleniumTestEmailList;
    @NotBlank
    private String mvnArgs;
    @NotBlank
    private String pipelineRunName;
    @NotBlank
    private String logs;

}
