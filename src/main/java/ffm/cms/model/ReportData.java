package ffm.cms.model;

import lombok.Data;

@Data
public class ReportData {
    
    public String lastRunDate;
    public String reportUrl;
    public String zipUrl;
    public String logUrl;
    public String env;

}
