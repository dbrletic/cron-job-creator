package openshift.selenium.model;

import io.quarkus.qute.TemplateData;
import lombok.Data;

@TemplateData
@Data
public class CronJobDashboardData {
    
    public String name;
    public String releaseBranch;
    public String group;
    public String type;
    public String msg;
    public String result;
    public String lastTransitionTime;
    public String color;
    public String runLink;
    public String runTime;
    public String env;
    public String failedTests;
    public String displayName;
}
