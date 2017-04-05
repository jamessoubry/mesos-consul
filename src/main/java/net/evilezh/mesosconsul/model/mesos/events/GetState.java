package net.evilezh.mesosconsul.model.mesos.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(value = {"get_frameworks", "get_executors"})
public class GetState {
    @JsonProperty("get_tasks")
    public GetTasks getTasks;
    @JsonProperty("get_agents")
    public GetAgents getAgents;
    //TODO get_agents
}
