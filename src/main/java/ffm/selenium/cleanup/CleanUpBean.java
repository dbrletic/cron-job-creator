package ffm.selenium.cleanup;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

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

    @Inject
    @ConfigProperty(name = "selenium.age.threshold.days")
    long AGE_THRESHOLD_DAYS; 
    
    private static final Logger LOGGER = Logger.getLogger(CleanUpBean.class);

    @Scheduled(every="30s")     
    void cleanUpZipsAndTxts() {
        String projectDir = System.getProperty("user.dir");
        File folder = new File(projectDir);
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".zip") || file.getName().endsWith(".txt")) {
                    if (file.delete()) {
                        LOGGER.info(file.getName() + " is deleted!");
                    } else {
                        LOGGER.info("Failed to delete " + file.getName());
                    }
                }
            }
        }
    }

    @Scheduled(cron = "0 6 * * 1 ?", timeZone = "America/New_York") //Runs every monday  at 6 am EDT 
    void cleanUpOldPipelineRuns(){
        // Get current date and time
        LocalDateTime now = LocalDateTime.now();
        // Define the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("mm:HH:ss dd/MM/yyyy");
        // Format the current date and time
        String formattedDate = now.format(formatter);
        try {
            LOGGER.info("Starting Cleanup of files older then "+ AGE_THRESHOLD_DAYS + " days on " + formattedDate) ;
            cleanOldFilesAndFolders(Paths.get(pipelineMountPath));
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    /**
     * Takes a parentFolder and deletes all files and folders older then 7 days. 
     * TODO Move to Utility Class
     * @param parentFolder
     * @throws IOException
     */
    private void cleanOldFilesAndFolders(Path parentFolder) throws IOException {
        Instant cutoffTime = Instant.now().minus(AGE_THRESHOLD_DAYS, ChronoUnit.DAYS);

        Files.walkFileTree(parentFolder, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (attrs.lastModifiedTime().toInstant().isBefore(cutoffTime)) {
                   LOGGER.info("Deleting file: " +  file.toAbsolutePath());
                    Files.delete(file); // Delete the file
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (isEmptyDirectory(dir)) {
                    LOGGER.info("Deleting directory: " + dir);
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
