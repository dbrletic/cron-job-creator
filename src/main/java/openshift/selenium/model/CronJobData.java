package openshift.selenium.model;

import io.quarkus.qute.TemplateData;
import lombok.Data;

@TemplateData
@Data
public class CronJobData {

    public String name;
    public String schedule;
    public String cluster;
    public String humanReadableMsg;
    public String branch;
    public String type;
    public String env;
    public String displayName;
}
