package ffm.cms;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import ffm.cms.helper.SimpleProgressMonitor;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class JGitBean {

    private static final String REMOTE_URL = "https://github.com/dbrletic/cron-job-creator.git";
    private static final String SELENIUM_RELEASE_BRANCH="origin/release/selenium-test";
    private Git git;

    
    //https://github.com/centic9/jgit-cookbook/

    public void updateAndPushCronjobs(Map<String, String> cronjobsToUpdate, String msg, String userName) throws GitAPIException, IOException{
        File currentRepo;
        String newBranch = "feature/cronjob-update-" + userName + "-" + generateFiveCharUUID();
        Ref newBranchRef;


        currentRepo = cloneRepository();
        createNewBranch(currentRepo, newBranch);
        commitAndPush(currentRepo,msg);
    }
 
    private File cloneRepository() throws GitAPIException {
        File localRepo = new File("C:" + File.separator + "tmp" + File.separator + "cron-job-creator");
        Git.cloneRepository()
           .setURI(REMOTE_URL)
           .setDirectory(localRepo)
           .setProgressMonitor(new SimpleProgressMonitor())
           //.setCredentialsProvider(new UsernamePasswordCredentialsProvider(USERNAME, PASSWORD))
           .call();
        return localRepo;
    }

    private void  createNewBranch(File localRepo, String branchName) throws GitAPIException, IOException {
        git.checkout()
        .setCreateBranch(true)
        .setName(branchName)
        .setStartPoint(SELENIUM_RELEASE_BRANCH)
        .call();
    }
    private void updateCronJobFiles(Map<String, String> cronjobsToUpdate){

    }

    private void commitAndPush(File localRepo, String msg) throws GitAPIException, IOException {
        //Git git = Git.open(localRepo);
        git.add().addFilepattern(".yaml").call();
        git.commit().setMessage(msg).call();
        git.push().setRemote("origin").call();
    }

     private static String generateFiveCharUUID() {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString().replace("-", "");
        return uuidString.substring(0, 5);
    }

}
