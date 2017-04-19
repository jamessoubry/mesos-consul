package net.evilezh.mesosconsul.model.mesos.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.evilezh.mesosconsul.model.mesos.Status;

import java.util.Map;

public class TaskUpdated {
    @JsonProperty("framework_id")
    public String frameworkId;
    public String state;
    public Status status;

    public void setFrameworkId(Map<String, String> data) {
        frameworkId = data.get("value");
    }
}
