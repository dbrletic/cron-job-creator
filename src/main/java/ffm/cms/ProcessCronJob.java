package ffm.cms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import net.redhogs.cronparser.CronExpressionDescriptor;

@ApplicationScoped
public class ProcessCronJob{

    
    public String processCronjob(String schedule, String groups, String url, String cleanReleaseBranch, String cleanGroup) throws IOException, ParseException {

        InputStream inputStream = getClass().getResourceAsStream("/cronjob-selenium-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        // Replace every instance of "CLEAN_GROUPS" with cleanGroup variable
        String outputContent = inputContent.replaceAll("CLEAN_GROUPS", cleanGroup);

        // Replace every instance of "groups" with groups variable
        outputContent = inputContent.replaceAll("GROUPS", groups);

        //Replace every instance of "{url}" with url variable 
        outputContent = outputContent.replaceAll("URL", url);

        //Replace every instance of SCHEDULE with schedule variable
        outputContent = outputContent.replaceAll("SCHEDULE", schedule);

        //Replacing HUMAN_READABLE with a easy to understand description of the cronjob schedule 
        outputContent = outputContent.replaceAll("HUMAN_READALBE", CronExpressionDescriptor.getDescription(schedule));

        //Relace CLEAN_RELEASE_BRANCH with a releaseBranch with no slashes
        outputContent = outputContent.replaceAll("CLEAN_RELEASE_BRANCH", cleanReleaseBranch);

        // Write out the output file for the new cronjob file
        Path outputFile = Paths.get("cj-" + cleanGroup + "-" + url + "-" + cleanReleaseBranch + ".yaml");
        Files.write(outputFile, outputContent.getBytes());

        return outputFile.toFile().getPath();
    }

    public String processEventListener(String groups, String url, String cleanReleaseBranch, String cleanGroup) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/eventlistener-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        // Replace every instance of "CLEAN_GROUPS" with cleanGroup variable
        String outputContent = inputContent.replaceAll("CLEAN_GROUPS", cleanGroup);

        // Replace every instance of "group" with groups variable
        outputContent = inputContent.replaceAll("GROUPS", groups);

        //Replace every instance of "{url}" with url variable 
        outputContent = outputContent.replaceAll("URL", url);

        //Relace CLEAN_RELEASE_BRANCH with a releaseBranch with no slashes
        outputContent = outputContent.replaceAll("CLEAN_RELEASE_BRANCH", cleanReleaseBranch);
        

        // Write out the output file for the new eventlistener file
        Path outputFile = Paths.get("el-" + cleanGroup + "-" + url + "-" + cleanReleaseBranch + ".yaml");
        Files.write(outputFile, outputContent.getBytes());

        return outputFile.toFile().getPath();
    }

    public String processTriggerBinding(String groups, String url, String releaseBranch, String userName, String userPassword, String browser, String seleniumTestEmailList, String cleanReleaseBranch, String cleanGroup) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/trigger-binding-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        // Replace every instance of "CLEAN_GROUPS" with cleanGroup variable
        String outputContent = inputContent.replaceAll("CLEAN_GROUPS", cleanGroup);
        
        // Replace every instance of "GROUPS" with groups variable
        outputContent = inputContent.replaceAll("GROUPS", groups);

        //Replace every instance of "URL" with url variable 
        outputContent = outputContent.replaceAll("URL", url);

        //Replace every instance of "RELEASEBRANCH" with releaseBranch variable 
        outputContent = outputContent.replaceAll("RELEASEBRANCH", releaseBranch);

        //Replace every instance of "USERNAME" with userName variable 
        outputContent = outputContent.replaceAll("USERNAME", userName);


        //Replace every instance of "USERPASSWORD" with userPassword variable 
        outputContent = outputContent.replaceAll("USERPASSWORD", userPassword);

        //Replace every instance of "BROWSER" with browser variable 
        outputContent = outputContent.replaceAll("BROWSER", browser);

        //Replace every instance of "SELENIUMTESTEMAILLIST" with seleniumTestEmailList variable 
        outputContent = outputContent.replaceAll("SELENIUMTESTEMAILLIST", seleniumTestEmailList);     
        
        //Relace CLEAN_RELEASE_BRANCH with a releaseBranch with no slashes
        outputContent = outputContent.replaceAll("CLEAN_RELEASE_BRANCH", cleanReleaseBranch);
        

        // Write out the output file for the new trigger binding
        Path outputFile = Paths.get("tb-" + cleanGroup + "-" + url + "-" + cleanReleaseBranch + ".yaml");
        Files.write(outputFile, outputContent.getBytes());

        return outputFile.toFile().getPath();
    }

     public String processTriggerTemplate(String groups, String url, String cleanReleaseBranch, String cleanGroup) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/trigger-template-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        // Replace every instance of "CLEAN_GROUPS" with cleanGroup variable
        String outputContent = inputContent.replaceAll("CLEAN_GROUPS", cleanGroup);
        
        // Replace every instance of "group" with groups variable
        outputContent = inputContent.replaceAll("GROUPS", groups);

        //Replace every instance of "{url}" with url variable 
        outputContent = outputContent.replaceAll("URL", url);

        //Relace CLEAN_RELEASE_BRANCH with a releaseBranch with no slashes
        outputContent = outputContent.replaceAll("CLEAN_RELEASE_BRANCH", cleanReleaseBranch);
        

        // Write out the output file the new trigger template 
        Path outputFile = Paths.get("tt-" + cleanGroup + "-" + url + "-" + cleanReleaseBranch + ".yaml");
        Files.write(outputFile, outputContent.getBytes());

        return outputFile.toFile().getPath();
    }
}