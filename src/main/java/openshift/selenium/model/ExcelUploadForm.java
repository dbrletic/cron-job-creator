package openshift.selenium.model;

import java.io.InputStream;
import jakarta.ws.rs.FormParam;

public class ExcelUploadForm {
    
    @FormParam("file")
    public InputStream file;

    @FormParam("fileName")
    public String fileName;
}
