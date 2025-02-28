package openshift.selenium.processing;

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

/**
 * Handles creating new cronjob files (and associated pipeline files) to run kick off Selenium Test
 * @author dbrletic
 */
@ApplicationScoped
public class ProcessCronJob{

    private static String CLEAN_GROUPS_URL_CLEAN_RELEASE_BRANCH = "CLEAN_GROUPS-URL-CLEAN_RELEASE_BRANCH";

    /**
     * Creates a OpenShift Cronjob yaml from the supplied values for a Selenium run. 
     * @param schedule
     * @param groups
     * @param url
     * @param cleanReleaseBranch
     * @param cleanGroup
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public String processCronjob(String schedule, String groups, String url, String cleanReleaseBranch, String cleanGroup) throws IOException, ParseException {

        InputStream inputStream = getClass().getResourceAsStream("/cronjob-selenium-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        // Replace every instance of "CLEAN_GROUPS" with cleanGroup variable
        String outputContent = inputContent.replaceAll("CLEAN_GROUPS", cleanGroup);

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

    /**
     * Updates a cronjob with a new schedule for a Selenium run. 
     * @param allVars Basically the name of the cronjob that is in the format of CLEAN_GROUPS-URL-CLEAN_RELEASE_BRANCH-cj
     * @param schedule The new Schedule
     * @return
     * @throws ParseException
     * @throws IOException
     */
    public String updateCronJOb(String allVars, String schedule) throws ParseException, IOException{

        String cleanVars = allVars.replace("-cj","");

        InputStream inputStream = getClass().getResourceAsStream("/cronjob-selenium-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        // Replace every instance of "CLEAN_GROUPS-URL-CLEAN_RELEASE_BRANCH" with cleanGroup variable
        String outputContent = inputContent.replaceAll(CLEAN_GROUPS_URL_CLEAN_RELEASE_BRANCH, cleanVars);

        //Replace every instance of SCHEDULE with schedule variable
        outputContent = outputContent.replaceAll("SCHEDULE", schedule);

        //Replacing HUMAN_READABLE with a easy to understand description of the cronjob schedule 
        outputContent = outputContent.replaceAll("HUMAN_READALBE", CronExpressionDescriptor.getDescription(schedule));

        // Write out the output file for the new cronjob file
         Path outputFile = Paths.get("cj-" + cleanVars +  ".yaml");
         Files.write(outputFile, outputContent.getBytes());
 
         return outputFile.toFile().getPath();

    }

    /**
     * Creates the EventListern yaml from the supplied files for a Selenium run. 
     * @param groups
     * @param url
     * @param cleanReleaseBranch
     * @param cleanGroup
     * @return
     * @throws IOException
     */
    public String processEventListener(String groups, String url, String cleanReleaseBranch, String cleanGroup) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/eventlistener-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        // Replace every instance of "CLEAN_GROUPS" with cleanGroup variable
        String outputContent = inputContent.replaceAll("CLEAN_GROUPS", cleanGroup);

        //Replace every instance of "{url}" with url variable 
        outputContent = outputContent.replaceAll("URL", url);

        //Relace CLEAN_RELEASE_BRANCH with a releaseBranch with no slashes
        outputContent = outputContent.replaceAll("CLEAN_RELEASE_BRANCH", cleanReleaseBranch);
        

        // Write out the output file for the new eventlistener file
        Path outputFile = Paths.get("el-" + cleanGroup + "-" + url + "-" + cleanReleaseBranch + ".yaml");
        Files.write(outputFile, outputContent.getBytes());

        return outputFile.toFile().getPath();
    }

    /**
     * Creates the TriggerBinding from the supplied values for a Selenium run. 
     * @param groups
     * @param url
     * @param releaseBranch
     * @param userName
     * @param userPassword
     * @param browser
     * @param seleniumTestEmailList
     * @param cleanReleaseBranch
     * @param cleanGroup
     * @return
     * @throws IOException
     */
    public String processTriggerBinding(String groups, String url, String releaseBranch, String userName, String userPassword, String browser, String seleniumTestEmailList, String cleanReleaseBranch, String cleanGroup) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/trigger-binding-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        // Replace every instance of "CLEAN_GROUPS" with cleanGroup variable
        String outputContent = inputContent.replaceAll("CLEAN_GROUPS", cleanGroup);
        
        // Replace every instance of "GROUPS" with groups variable
        outputContent = outputContent.replaceAll("GROUPS", groups);

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

    /**
     * Cretes the TriggerTemplate from the supplied yaml file  for a Selenium run. 
     * @param groups
     * @param url
     * @param cleanReleaseBranch
     * @param cleanGroup
     * @return
     * @throws IOException
     */
     public String processTriggerTemplate(String groups, String url, String cleanReleaseBranch, String cleanGroup) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/trigger-template-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        // Replace every instance of "CLEAN_GROUPS" with cleanGroup variable
        String outputContent = inputContent.replaceAll("CLEAN_GROUPS", cleanGroup);
        
        //Replace every instance of "{url}" with url variable 
        outputContent = outputContent.replaceAll("URL", url);

        //Relace CLEAN_RELEASE_BRANCH with a releaseBranch with no slashes
        outputContent = outputContent.replaceAll("CLEAN_RELEASE_BRANCH", cleanReleaseBranch);
        

        // Write out the output file the new trigger template 
        Path outputFile = Paths.get("tt-" + cleanGroup + "-" + url + "-" + cleanReleaseBranch + ".yaml");
        Files.write(outputFile, outputContent.getBytes());

        return outputFile.toFile().getPath();
    }

    /**
     * Creates the yaml file for a gatling Cronjob
     * @param schedule
     * @param url
     * @param cleanReleaseBranch
     * @param type
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public String processGatlingCronjob(String schedule, String url, String cleanReleaseBranch, String type) throws IOException, ParseException {

        InputStream inputStream = getClass().getResourceAsStream("/cronjob-gatling-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        //Replace every instance of "{url}" with url variable 
        String outputContent = inputContent.replaceAll("URL", url);

        //Replace every instance of SCHEDULE with schedule variable
        outputContent = outputContent.replaceAll("SCHEDULE", schedule);

        //Replacing HUMAN_READABLE with a easy to understand description of the cronjob schedule 
        outputContent = outputContent.replaceAll("HUMAN_READALBE", CronExpressionDescriptor.getDescription(schedule));

        //Relace CLEAN_RELEASE_BRANCH with a releaseBranch with no slashes
        outputContent = outputContent.replaceAll("CLEAN_RELEASE_BRANCH", cleanReleaseBranch);

        // Write out the output file for the new cronjob file
        Path outputFile = Paths.get("cj-gatling-" +  type +"-" + url + "-" + cleanReleaseBranch + ".yaml");
        Files.write(outputFile, outputContent.getBytes());

        return outputFile.toFile().getPath();
    }

    /**
     * 
     * @param url
     * @param cleanReleaseBranch
     * @param type
     * @return
     * @throws IOException
     */
    public String processGatlingEventListener(String url, String cleanReleaseBranch, String type) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/eventlistener-gatling-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        //Replace every instance of "URL" with url variable 
        String outputContent = inputContent.replaceAll("URL", url);

        //Relace CLEAN_RELEASE_BRANCH with a releaseBranch with no slashes
        outputContent = outputContent.replaceAll("CLEAN_RELEASE_BRANCH", cleanReleaseBranch);
        

        // Write out the output file for the new eventlistener file
        Path outputFile = Paths.get("el-gatling-" + type +"-" + url + "-" + cleanReleaseBranch + ".yaml");
        Files.write(outputFile, outputContent.getBytes());

        return outputFile.toFile().getPath();
    }

    /**
     * 
     * @param url
     * @param cleanReleaseBranch
     * @param type
     * @return
     * @throws IOException
     */
    public String processGatlingTriggerTemplate(String url, String cleanReleaseBranch, String type) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/trigger-template-gatling-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));
 
        //Replace every instance of "URL" with url variable 
        String outputContent = inputContent.replaceAll("URL", url);

        //Relace CLEAN_RELEASE_BRANCH with a releaseBranch with no slashes
        outputContent = outputContent.replaceAll("CLEAN_RELEASE_BRANCH", cleanReleaseBranch);
        
        // Write out the output file the new trigger template 
        Path outputFile = Paths.get("tt-gatling-" + type +"-" + url + "-" + cleanReleaseBranch + ".yaml");
        Files.write(outputFile, outputContent.getBytes());

        return outputFile.toFile().getPath();
    }

    /**
     * 
     * @param url
     * @param releaseBranch
     * @param type
     * @param gatlingTestEmailList
     * @param cleanReleaseBranch
     * @return
     * @throws IOException
     */
    public String processGatlingTriggerBinding(String url, String releaseBranch, String type, String gatlingTestEmailList, String cleanReleaseBranch) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/trigger-binding-gatling-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));


        //Replace every instance of "URL" with url variable 
        String outputContent = inputContent.replaceAll("URL", url);

        //Replace every instance of "RELEASEBRANCH" with releaseBranch variable 
        outputContent = outputContent.replaceAll("RELEASEBRANCH", releaseBranch);

        //Replace every instance of "SELENIUMTESTEMAILLIST" with seleniumTestEmailList variable 
        outputContent = outputContent.replaceAll("GATLINGTESTEMAILLIST", gatlingTestEmailList);     
        
        //Relace CLEAN_RELEASE_BRANCH with a releaseBranch with no slashes
        outputContent = outputContent.replaceAll("CLEAN_RELEASE_BRANCH", cleanReleaseBranch);
        

        // Write out the output file for the new trigger binding
        Path outputFile = Paths.get("tb-gatling-" + type +"-" + url + "-" + cleanReleaseBranch + ".yaml");
        Files.write(outputFile, outputContent.getBytes());

        return outputFile.toFile().getPath();
    }

}