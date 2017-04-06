package net.evilezh.mesosconsul;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.evilezh.mesosconsul.model.mesos.events.*;
import net.evilezh.mesosconsul.transform.Transform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;


public class EventProcessor {
    private final static ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LogManager.getLogger(EventProcessor.class);
    private final Mesos mesos;
    private final Consul consul;

    public EventProcessor(Mesos mesos, Consul consul) {
        this.mesos = mesos;
        this.consul = consul;
    }

    public synchronized void process(byte[] data) {
        try {
            Object o = mapper.readValue(data,Map.class);
            Files.write(Paths.get("C:\\Users\\evilezh\\Desktop\\out1.json"),mapper.writeValueAsBytes(o));
            Event event = mapper.readValue(data, Event.class);
            logger.info("Got event: " + event.getClass().getName());
            if (event instanceof SubscribedD) {
                //reset/sync mesos/consul.
                mesos.reset();
                consul.reset();
                ((SubscribedD) event).subscribed.getState.getAgents.agents.forEach(mesos::addAgent);
                ((SubscribedD) event).subscribed.getState.getTasks.tasks.forEach(mesos::addTask);
            } else if (event instanceof TaskAddedD) {

            } else if (event instanceof TaskUpdatedD) {

            } else if (event instanceof AgentAddedD) {
                logger.info("DATA: " + mapper.writeValueAsString(((AgentAddedD) event).agentAdded));
            } else if (event instanceof AgentRemovedD) {
                logger.info("DATA: " + mapper.writeValueAsString(((AgentRemovedD) event).agentRemoved));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
