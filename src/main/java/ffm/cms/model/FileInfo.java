package ffm.cms.model;

import lombok.Data;

@Data
public class FileInfo {
    private String name;
    private boolean isDirectory;

    public FileInfo(String name, boolean isDirectory) {
        this.name = name;
        this.isDirectory = isDirectory;
    }
}
