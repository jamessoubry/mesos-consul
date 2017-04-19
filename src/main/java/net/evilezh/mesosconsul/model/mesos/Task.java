package net.evilezh.mesosconsul.model.mesos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonIgnoreProperties(value = {"status_update_state", "status_update_uuid"})


public class Task {
    @JsonProperty("agent_id")
    public String agentId;
    @JsonProperty("framework_id")
    public String frameworkId;
    @JsonProperty("name")
    public String name;
    @JsonProperty("state")
    public String state;
    @JsonProperty("task_id")
    public String taskId;
    @JsonProperty("discovery")
    public Discovery discovery;
    @JsonProperty("labels")
    public Map<String, String> labels;
    @JsonProperty("statuses")
    public List<Status> statuses = new ArrayList<>();
/*
    @JsonProperty("status")
    public Status status;
*/
    @JsonProperty("executor_id")
    public String executorId;
    public Object container;
    public List<Resource> resources;

    public void setLabels(Map<String, List<Map<String, String>>> data) {
        labels = data.get("labels").stream().collect(Collectors.toMap(it -> it.get("key"), it -> it.get("value")));
    }

    public void setAgentId(Map<String, String> data) {
        agentId = data.get("value");
    }

    public void setFrameworkId(Map<String, String> data) {
        frameworkId = data.get("value");
    }

    public void setTaskId(Map<String, String> data) {
        taskId = data.get("value");
    }

    public void setExecutorId(Map<String, String> data) {
        executorId = data.get("value");
    }
}
