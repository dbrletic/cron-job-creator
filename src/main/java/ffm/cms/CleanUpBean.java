package ffm.cms;

import java.io.File;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
/**
 * Cleans up the left over zip and txt files so there are not any name conflicts. 
 */
public class CleanUpBean {
    
    @Scheduled(every="30s")     
    void cleanUpZipsAndTxts() {
        String projectDir = System.getProperty("user.dir");
        File folder = new File(projectDir);
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".zip") || file.getName().endsWith(".txt")) {
                    if (file.delete()) {
                        System.out.println(file.getName() + " is deleted!");
                    } else {
                        System.out.println("Failed to delete " + file.getName());
                    }
                }
            }
        }
    }
}
