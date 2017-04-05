package net.evilezh.mesosconsul.model.mesos.events;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TaskAddedD.class, name = "TASK_ADDED"),
        @JsonSubTypes.Type(value = SubscribedD.class, name = "SUBSCRIBED"),
        @JsonSubTypes.Type(value = TaskUpdatedD.class, name = "TASK_UPDATED"),
        @JsonSubTypes.Type(value = AgentAddedD.class, name = "AGENT_ADDED"),
        @JsonSubTypes.Type(value = AgentRemovedD.class, name = "AGENT_REMOVED"),
})

public abstract class Event {
    public String type;
}
