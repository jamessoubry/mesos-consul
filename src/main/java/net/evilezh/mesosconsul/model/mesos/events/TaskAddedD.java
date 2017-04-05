package net.evilezh.mesosconsul.model.mesos.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TaskAddedD extends Event {
    @JsonProperty("task_added")
    public TaskAdded taskAdded;
}
