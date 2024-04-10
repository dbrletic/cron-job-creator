package ffm.cms.model;


public class FFEGatlingData {

    private String releaseBranch;
    private String url;
    private String type;
    private String gatlingTestEmailList;
    private String cronJobSchedule;
    
    public FFEGatlingData(String releaseBranch, 
            String url, String gatlingTestEmailList, String cronJobSchedule) {
        this.releaseBranch = releaseBranch;
        this.url = url;
        this.gatlingTestEmailList = gatlingTestEmailList;
        this.cronJobSchedule = cronJobSchedule;
    }
    public String getCronJobSchedule() {
        return cronJobSchedule;
    }
    public String getReleaseBranch(){
        return releaseBranch;
    }
    public String getGatlingTestEmailList(){
        return gatlingTestEmailList;
    }
    public String getUrl(){
        return url;
    }
    public String getType(){
        return type;
    }
    public void setCronJobSchedule(String cronJobSchedule) {
        this.cronJobSchedule = cronJobSchedule;
    }
    public void setReleaseBranch(String releaseBranch) {
        this.releaseBranch = releaseBranch;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public void setGatlingTestEmailList(String gatlingTestEmailList) {
        this.gatlingTestEmailList = gatlingTestEmailList;
    }
    public void setType(String type){
        this.type = type;
    }

    @Override
    public String toString() {
        return "Data [releaseBranch=" + releaseBranch + ", userNameFFM=" + ", url=" + url + ", type=" + type + 
            ", gatlingTestEmailList=" + gatlingTestEmailList + ", cronJobSchedule=" + cronJobSchedule + "]";
    }

    
}
