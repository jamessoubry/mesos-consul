package net.evilezh.mesosconsul.transform;

import net.evilezh.mesosconsul.model.mesos.Agent;
import net.evilezh.mesosconsul.model.mesos.Task;

import java.util.Map;

public interface Transform {
    <T extends net.evilezh.mesosconsul.model.config.Transform> Map<String, Integer> apply(Task task, Agent agent);
}
