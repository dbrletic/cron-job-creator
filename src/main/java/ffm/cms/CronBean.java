package ffm.cms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CronBean {

    @ConfigProperty(name = "usePropertiesFile")
    Boolean usePropertiesFile;

    @ConfigProperty(name ="releaseBranch")
    String releaseBranch;

    @ConfigProperty(name = "userNameFFM")
    String userName;

    @ConfigProperty(name = "userPassword")
    String userPassword;

    @ConfigProperty(name = "groups")
    String groups;

    @ConfigProperty(name = "url")
    String url;

    @ConfigProperty(name = "browser")
    String browser;

    @ConfigProperty(name = "seleniumTestEmailList")
    String seleniumTestEmailList;

    @Startup
    public void createFiles() {
        System.out.println("Starting up process");
        try{
            if(usePropertiesFile){
                processCronjob();
                processEventListener();
                processTriggerBinding();
                processTriggerTemplate();
            }
        } catch (IOException e){
            System.out.println(e.getMessage());
            System.out.println(e.getStackTrace());
        }
       
    }

    public void processCronjob() throws IOException {

        InputStream inputStream = getClass().getResourceAsStream("/cronjob-selenium-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        // Replace every instance of "group" with groups variable
        String outputContent = inputContent.replaceAll("GROUPS", groups);

        //Replace every instance of "{url}" with url variable 
        outputContent = outputContent.replaceAll("URL", url);

        // Write out the output file
        Path outputFile = Paths.get("cronjob-selenium-" + groups + "-" + url + ".yaml");
        Files.write(outputFile, outputContent.getBytes());
    }

    public void processEventListener() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/eventlistener-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        // Replace every instance of "group" with groups variable
        String outputContent = inputContent.replaceAll("GROUPS", groups);

        //Replace every instance of "{url}" with url variable 
        outputContent = outputContent.replaceAll("URL", url);

        // Write out the output file
        Path outputFile = Paths.get("eventlistener-" + groups + "-" + url + ".yaml");
        Files.write(outputFile, outputContent.getBytes());

    }

    public void processTriggerBinding() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/trigger-binding-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        // Replace every instance of "GROUPS" with groups variable
        String outputContent = inputContent.replaceAll("GROUPS", groups);

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

        // Write out the output file
        Path outputFile = Paths.get("trigger-binding-" + groups + "-" + url + ".yaml");
        Files.write(outputFile, outputContent.getBytes());

    }

     public void processTriggerTemplate() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/trigger-template-master.yml");
      
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String inputContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        // Replace every instance of "group" with groups variable
        String outputContent = inputContent.replaceAll("GROUPS", groups);

        //Replace every instance of "{url}" with url variable 
        outputContent = outputContent.replaceAll("URL", url);

        // Write out the output file
        Path outputFile = Paths.get("trigger-template-" + groups + "-" + url + ".yaml");
        Files.write(outputFile, outputContent.getBytes());

    }


   
}