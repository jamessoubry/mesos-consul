package net.evilezh.mesosconsul.model.mesos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonIgnoreProperties(value = {"data", "timestamp", "uuid", "reason"})
public class Status {
    @JsonProperty("agent_id")
    public String agentId;
    @JsonProperty("executor_id")
    public String executorId;
    @JsonProperty("task_id")
    public String taskId;
    public String state;
    public String source;
    @JsonProperty("container_status")
    public ContainerStatus containerStatus;
    public String message;
    @JsonProperty("labels")
    Map<String, String> labels;
    public boolean healthy = true;

    public void setAgentId(Map<String, String> data) {
        agentId = data.get("value");
    }

    public void setTaskId(Map<String, String> data) {
        taskId = data.get("value");
    }


    public void setExecutorId(Map<String, String> data) {
        executorId = data.get("value");
    }

    public void setLabels(Map<String, List<Map<String, String>>> data) {
        labels = data.get("labels").stream().collect(Collectors.toMap(it -> it.get("key"), it -> it.get("value")));
    }


}
