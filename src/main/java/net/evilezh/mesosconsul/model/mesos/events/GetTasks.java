package net.evilezh.mesosconsul.model.mesos.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.evilezh.mesosconsul.model.mesos.Task;

import java.util.List;

@JsonIgnoreProperties(value = {"completed_tasks"})
public class GetTasks {
    @JsonProperty("orphan_tasks")
    public List<Task> orphanTasks;
    public List<Task> tasks;
}
