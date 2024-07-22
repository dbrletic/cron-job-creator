package ffm.cms.model;

import io.quarkus.qute.TemplateData;

@TemplateData
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

    public String toString() {
        return "[" +
                "name='" + name + '\'' +
                ", releaseBranch='" + releaseBranch + '\'' +
                ", group='" + group + '\'' +
                ", type='" + type + '\'' +
                ", msg='" + msg + '\'' +
                ", result='" + result + '\'' +
                ", lastTransitionTime='" + lastTransitionTime + '\'' +
                ", color='" + color + '\'' +
                ", runLink='" + runLink + '\'' +
                ", runTime='" + runTime + '\'' +
                ", env='" + env + '\'' +
                ", failedTests='" + failedTests + '\'' +
                ']';
    }
}
