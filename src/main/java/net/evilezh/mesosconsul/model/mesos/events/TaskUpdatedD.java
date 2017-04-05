package net.evilezh.mesosconsul.model.mesos.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.evilezh.mesosconsul.model.mesos.Task;

public class TaskUpdatedD extends Event {
    @JsonProperty("task_updated")
    public Task taskUpdated;
}
