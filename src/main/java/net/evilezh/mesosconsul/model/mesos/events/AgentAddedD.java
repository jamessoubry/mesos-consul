package net.evilezh.mesosconsul.model.mesos.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class AgentAddedD extends Event {
    @JsonProperty("agent_added")
    public AgentAdded agentAdded;
}
