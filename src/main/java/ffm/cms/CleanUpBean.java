package ffm.cms;

import java.io.File;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class CleanUpBean {
    
    @Scheduled(every="15s")     
    void cleanUpZips() {
        String projectDir = System.getProperty("user.dir");
        File folder = new File(projectDir);
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".zip")) {
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
