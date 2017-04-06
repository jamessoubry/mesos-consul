package net.evilezh.mesosconsul;

import net.evilezh.mesosconsul.model.config.Config;
import net.evilezh.mesosconsul.model.mesos.Agent;
import net.evilezh.mesosconsul.model.mesos.Task;
import net.evilezh.mesosconsul.transform.Transform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Mesos {
    private static final Logger logger = LogManager.getLogger(Mesos.class);

    //    private final Consul consul;
    private final List<Transform> transform;
    private final Map<String, Task> tasks;
    private final Map<String, Agent> agents;
    private final Config config;

    public Mesos(Config config) {
//        this.consul = consul;
        this.transform = config.transform.stream().map(this::newTransform).collect(Collectors.toList());

        this.tasks = new HashMap<>();
        this.agents = new HashMap<>();
        this.config = config;
    }

    public synchronized void reset() {
        tasks.clear();
        agents.clear();
    }


    public synchronized void addTask(Task task) {
        tasks.put(task.taskId, task);
        if (task.state.equals("TASK_RUNNING")) {
            Map<String, Integer> taskNames = null;
            for (Transform tr : transform) {
                taskNames = tr.apply(task, agents.get(task.agentId));
                if (taskNames.size() != 0)
                    break;
            }
//            List<String> collect = transform.stream().map(it -> it.apply(task, agents.get(task.agentId))).flatMap(Collection::stream).collect(Collectors.toList());
            logger.info("-----------------");
            if (taskNames.size() > 0) {
                //TODO add extra checks of data existence
                String ip = task.statuses.stream().filter(it -> it.state.equals("TASK_RUNNING")).findFirst().get().containerStatus.networkInfos.get(0).ipAddresses.get(0).ipAddress;
                taskNames.entrySet().forEach(entry -> logger.info(() -> entry.getKey() + ":" + entry.getValue() + " -> " + ip));


            } else {
                logger.error("EMPTY task: " + task.name);
            }
        }


/*
        if (task.state.equals("TASK_RUNNING")) {
            String taskName = transform.apply(task, agents.get(task.agentId), config.transform.get(0));

        }
*/
    }

    public synchronized void updateTask(Task task) {

    }

    private Transform newTransform(net.evilezh.mesosconsul.model.config.Transform transform) {
        try {
            MethodHandle constructor = MethodHandles.lookup().findConstructor(transform.getImplementation(), MethodType.methodType(void.class, net.evilezh.mesosconsul.model.config.Transform.class));
            Transform tr = (Transform) constructor.invoke(transform);
            return tr;
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public synchronized void addAgent(Agent agent) {
        agents.put(agent.agentInfo.id, agent);
    }

    public synchronized void removeAgent(Agent agent) {
        agents.remove(agent.agentInfo.id);
    }
}
