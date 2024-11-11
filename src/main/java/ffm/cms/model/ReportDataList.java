package ffm.cms.model;

import java.util.ArrayList;

import io.quarkus.qute.TemplateData;
import lombok.Data;

@Data
@TemplateData
public class ReportDataList {
    
    public ArrayList<ReportData> reportData;
    public String runName;
    public String env;
}
