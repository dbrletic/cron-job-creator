package ffm.cms.model;

import io.quarkus.qute.TemplateData;

@TemplateData
public class CronJobData {

    public String name;
    public String schedule;
    public String cluster;
    public String humanReadableMsg;
    public String branch;
    public String type;

    public String toString() {
        return "[" +
                "name='" + name + '\'' +
                ", schedule='" + schedule + '\'' +
                ", cluster='" + cluster + '\'' +
                ", humanReadableMsg='" + humanReadableMsg + '\'' +
                ", branch='" + branch + '\'' +
                ", type='" + type + '\'' +
                ']';
    }
}
