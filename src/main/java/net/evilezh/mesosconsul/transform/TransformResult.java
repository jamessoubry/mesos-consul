package net.evilezh.mesosconsul.transform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.evilezh.mesosconsul.model.mesos.Agent;

import static net.evilezh.mesosconsul.Engine.getEngine;

public class TransformResult {
    public String portName, agentId, taskName, taskId, servicePrefix, transformName;
    public int port;

    //filled by Engine, extracted from mesos task.
    public String hostIp, serviceIp;

    @JsonIgnore
    public Agent getAgent() {
        return getEngine().getMesos().getAgent(agentId);
    }
}
