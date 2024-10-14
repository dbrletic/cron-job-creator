package ffm.cms.cleanup;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import java.time.Instant;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;



/**
 * Cleans up the left over zip and txt files so there are not any name conflicts. 
 * Also clean up the PVC mount of older zips and htmls using the giving cron schedule and 
 * @author dbrletic
 */
@ApplicationScoped
public class CleanUpBean {
    
    @Inject
    @ConfigProperty(name = "quarkus.openshift.mounts.pipeline-storage.path")
    String pipelineMountPath;

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

    @Scheduled(cron = "0 5 * * 1 ?") //Runs every Monday at 5 am. 
    void cleanUpOldPipelineRuns(){
        try {
            cleanOldFilesAndFolders(Paths.get(pipelineMountPath));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Takes a parentFolder and deletes all files and folders older then 7 days. 
     * TODO Move to Utility Class
     * @param parentFolder
     * @throws IOException
     */
    private void cleanOldFilesAndFolders(Path parentFolder) throws IOException {
        Instant cutoffTime = Instant.now().minus(7, ChronoUnit.DAYS);

        Files.walkFileTree(parentFolder, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (attrs.lastModifiedTime().toInstant().isBefore(cutoffTime)) {
                    System.out.println("Deleting file: " + file);
                    Files.delete(file); // Delete the file
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (isEmptyDirectory(dir)) {
                    System.out.println("Deleting directory: " + dir);
                    Files.delete(dir); // Delete the directory if empty
                }
                return FileVisitResult.CONTINUE;
            }

            private boolean isEmptyDirectory(Path dir) throws IOException {
                try (Stream<Path> entries = Files.list(dir)) {
                    return !entries.findAny().isPresent(); // Check if the directory is empty
                }
            }
        });
    }

}
