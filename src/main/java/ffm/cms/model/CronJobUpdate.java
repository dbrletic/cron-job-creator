package ffm.cms.model;

import jakarta.validation.constraints.NotBlank;

public class CronJobUpdate {

    @NotBlank
    private  String cronJobName;
    @NotBlank
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
