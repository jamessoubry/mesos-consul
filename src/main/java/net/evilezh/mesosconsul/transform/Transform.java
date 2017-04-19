package net.evilezh.mesosconsul.transform;

import net.evilezh.mesosconsul.model.mesos.Agent;
import net.evilezh.mesosconsul.model.mesos.Task;

import java.util.List;

public interface Transform {
    List<TransformResult> transform(Task task, Agent node, String servicePrefix);
}
