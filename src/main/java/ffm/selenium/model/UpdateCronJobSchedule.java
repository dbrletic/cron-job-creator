package ffm.selenium.model;


import java.util.Map;

public class UpdateCronJobSchedule {
    
    private Map<String, String> pairs;
    private String userName;
    private String description;

    public Map<String, String> getPairs() {
        return pairs;
    }

    public void setPairs(Map<String, String> pairs) {
        this.pairs = pairs;
    }

    public String getUserName(){
        return userName;
    }

    public void setUserName(String userName){
        this.userName = userName;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public String getDescription(){
        return description;
    }

}
