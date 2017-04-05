package net.evilezh.mesosconsul.model.mesos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(value = {"allocated_resources", "registered_time", "total_resources", "reregistered_time","offered_resources"})
public class Agent {
    public boolean active;
    public String version;
    public String pid;
    @JsonProperty("agent_info")
    public AgentInfo agentInfo;
}
