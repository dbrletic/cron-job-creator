package ffm.cms;


public class Data {

    private String releaseBranch;
    private String userNameFFM;
    private String userPassword;
    private String groups;
    private String browser;
    private String url;
    private String seleniumTestEmailList;
    private String cronJobSchedule;
    
    public Data(String releaseBranch, String userNameFFM, String userPassword, String groups, String browser,
            String url, String seleniumTestEmailList, String cronJobSchedule) {
        this.releaseBranch = releaseBranch;
        this.userNameFFM = userNameFFM;
        this.userPassword = userPassword;
        this.groups = groups;
        this.browser = browser;
        this.url = url;
        this.seleniumTestEmailList = seleniumTestEmailList;
        this.cronJobSchedule = cronJobSchedule;
    }
    public String getCronJobSchedule() {
        return cronJobSchedule;
    }
    public void setCronJobSchedule(String cronJobSchedule) {
        this.cronJobSchedule = cronJobSchedule;
    }
   
    public String getReleaseBranch() {
        return releaseBranch;
    }
    public String getUserNameFFM() {
        return userNameFFM;
    }
    public String getUserPassword() {
        return userPassword;
    }
    public String getGroups() {
        return groups;
    }
    public String getBrowser() {
        return browser;
    }
    public String getUrl() {
        return url;
    }
    public String getSeleniumTestEmailList() {
        return seleniumTestEmailList;
    }
    public void setReleaseBranch(String releaseBranch) {
        this.releaseBranch = releaseBranch;
    }
    public void setUserNameFFM(String userNameFFM) {
        this.userNameFFM = userNameFFM;
    }
    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }
    public void setGroups(String groups) {
        this.groups = groups;
    }
    public void setBrowser(String browser) {
        this.browser = browser;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public void setSeleniumTestEmailList(String seleniumTestEmailList) {
        this.seleniumTestEmailList = seleniumTestEmailList;
    }
    @Override
    public String toString() {
        return "Data [releaseBranch=" + releaseBranch + ", userNameFFM=" + userNameFFM + ", userPassword="
                + userPassword + ", groups=" + groups + ", browser=" + browser + ", url=" + url
                + ", seleniumTestEmailList=" + seleniumTestEmailList + ", cronJobSchedule=" + cronJobSchedule + "]";
    }
    

    
}
