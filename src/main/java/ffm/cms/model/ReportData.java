package ffm.cms.model;

import io.quarkus.qute.TemplateData;
import lombok.Data;

@Data
@TemplateData
public class ReportData {
    
    public String lastRunDate;
    public String reportUrl;
    public String zipUrl;
    public String logUrl;
    public String env;

}
