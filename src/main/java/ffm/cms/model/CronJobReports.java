package ffm.cms.model;

import io.quarkus.qute.TemplateData;

@TemplateData
public class CronJobReports {
    public String name;
    public String lastRunDate;
    public String zipUrl;
    public String reportUrl;
    public String logUrl;
    public String env;
}
