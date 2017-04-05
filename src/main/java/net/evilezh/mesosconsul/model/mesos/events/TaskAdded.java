package net.evilezh.mesosconsul.model.mesos.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.evilezh.mesosconsul.model.mesos.Task;

public class TaskAdded {

    @JsonProperty("task")
    public Task task;
}
