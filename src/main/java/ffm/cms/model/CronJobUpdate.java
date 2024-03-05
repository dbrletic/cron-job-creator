package ffm.cms.model;


public class CronJobUpdate {

    private  String cronJobName;
    private  String cronJobSchedule;
 
    public void setCronJobName(String cronJobName){
        this.cronJobName = cronJobName;
    }

    public String getCronJobName(){
        return cronJobName;
    }

    public void setCronJobSchedule(String cronJobSchedule){
        this.cronJobSchedule = cronJobSchedule;
    }

    public String getCronJobSchedule(){
        return cronJobSchedule;
    }
}
