package ffm.cms.model;

import lombok.Data;

@Data
public class ScheduleJob {

    public String jobName;
    public String additionalInfo;
    public String schedule;
    public String description;

    // Getters and Setters (optional)
}