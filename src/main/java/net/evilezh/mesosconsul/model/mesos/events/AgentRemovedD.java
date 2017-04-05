package net.evilezh.mesosconsul.model.mesos.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class AgentRemovedD extends Event {
    @JsonProperty("agent_removed")
    public Map<String, Object> agentRemoved;
}
