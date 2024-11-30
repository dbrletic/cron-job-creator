package ffm.selenium.model;

import java.util.ArrayList;

import io.quarkus.qute.TemplateData;
import lombok.Data;

@Data
@TemplateData
public class ReportDataList {
    
    public ArrayList<ReportData> reportData;
    public String runName;
    public String displayName;
    public String env;

    public ReportDataList(){
        this.reportData = new ArrayList<ReportData>();
        this.runName="";
        this.env="";
        this.displayName="";
    }
}
