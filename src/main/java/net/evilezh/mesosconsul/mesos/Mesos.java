package net.evilezh.mesosconsul.mesos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.evilezh.mesosconsul.exceptions.ForcedStopException;
import net.evilezh.mesosconsul.consul.Consul;
import net.evilezh.mesosconsul.model.consul.NodeEntry;
import net.evilezh.mesosconsul.model.mesos.Agent;
import net.evilezh.mesosconsul.model.mesos.Status;
import net.evilezh.mesosconsul.model.mesos.Task;
import net.evilezh.mesosconsul.model.mesos.events.*;
import net.evilezh.mesosconsul.transform.Transform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.ZooKeeper;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.util.*;

import static net.evilezh.mesosconsul.Main.client;

public class Mesos implements Response.CompleteListener, Response.ContentListener {

    private static final Logger logger = LogManager.getLogger(Mesos.class);

    //mesos task states
    public static final String TASK_STAGING = "TASK_STAGING";
    public static final String TASK_STARTING = "TASK_STARTING";
    public static final String TASK_RUNNING = "TASK_RUNNING";
    public static final String TASK_KILLING = "TASK_KILLING";
    public static final String TASK_FINISHED = "TASK_FINISHED";
    public static final String TASK_FAILED = "TASK_FAILED";
    public static final String TASK_KILLED = "TASK_KILLED";
    public static final String TASK_ERROR = "TASK_ERROR";
    public static final String TASK_LOST = "TASK_LOST";
    public static final String TASK_DROPPED = "TASK_DROPPED";
    public static final String TASK_GONE = "TASK_GONE";
    public static final String TASK_GONE_BY_OPERATOR = "TASK_GONE_BY_OPERATOR";
    public static final String TASK_UNKNOWN = "TASK_UNKNOWN";
    public static final String TASK_UNREACHABLE = "TASK_UNREACHABLE";

    public MesosEvent eventReceiver;


    //for event processing
    private boolean start = false;
    private int eventSize = 0;
    private StringBuilder sb = new StringBuilder();
    private static final ObjectMapper mapper = new ObjectMapper();


    //    private final Consul consul;
//    private final List<Transform> transform;
    private final Map<String, Task> tasks;
    private final Map<String, Agent> agents;
    private final net.evilezh.mesosconsul.model.config.Mesos config;

    public <E extends MesosEvent> Mesos(net.evilezh.mesosconsul.model.config.Mesos config, E evenReceiver) {
//        this.consul = consul;
//        this.transform = config.transform.stream().map(this::newTransform).collect(Collectors.toList());

        this.tasks = new HashMap<>();
        this.agents = new HashMap<>();
        this.config = config;
        this.eventReceiver = evenReceiver;
    }

    public synchronized void reset() {
        tasks.clear();
        agents.clear();
    }


    public synchronized void addTask(Task task) {
        tasks.put(task.taskId, task);
//        parseTask(task, consul);
    }


    private NodeEntry findConsulNode(Task task, Consul consul) {
        String taskId = task.taskId;
        Agent agent = agents.get(task.agentId);
        String agentIp = (String) agent.agentInfo.attributes.get("ip");
        return consul.getNodes().stream().filter(it -> it.node.address.equals(agentIp)).findFirst().get();
    }

    public synchronized void removeTask(Task task, Consul consul) {
        String taskId = task.taskId;
        Agent agent = agents.get(task.agentId);
        String agentIp = (String) agent.agentInfo.attributes.get("ip");
        Optional<NodeEntry> first = consul.getNodes().stream().filter(it -> it.node.address.equals(agentIp)).findFirst();


        if (tasks.containsKey(task.taskId))
            tasks.remove(task.taskId);
    }

    public synchronized Task updateTask(Status status) {
        Task task = tasks.get(status.taskId);
        task.statuses.add(status);
        task.state = status.state;
/*

        switch (status.state) {
            case TASK_STAGING:
            case TASK_STARTING:
                break;
            case TASK_RUNNING:
//                parseTask(task, consul);
                break;
            default: //everything else is considered terminal
                String taskId = task.taskId;
                Agent agent = agents.get(task.agentId);
                String agentIp = (String) agent.agentInfo.attributes.get("ip");
                Optional<NodeEntry> optionalNodeEntry = consul.nodes.stream().filter(it -> it.node.address.equals(agentIp)).findFirst();
                if (!optionalNodeEntry.isPresent())
                    throw new RuntimeException("Something failed in chain ...");
                NodeEntry nodeEntry = optionalNodeEntry.get();
                nodeEntry.services.entrySet().stream()
                        .filter(it -> it.getKey().startsWith("mconsul:" + taskId + ":"))
                        .forEach(it -> consul.deRegisterService(it.getValue(), nodeEntry));
                break;

        }

*/

//        ObjectMapper mapper = new ObjectMapper();

        try {
            logger.debug(mapper.writeValueAsString(status));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        logger.debug("TASK UPDATED");
        return task;
    }

    public synchronized void addAgent(Agent agent) {
        agents.put(agent.agentInfo.id, agent);
    }

    public synchronized void removeAgent(Agent agent) {
        agents.remove(agent.agentInfo.id);
    }

    public synchronized void destroy() {

    }

    public Agent getAgent(String id) {
        return agents.get(id);
    }


    public void stopProcessing() {
        if (request != null && !request.abort(new ForcedStopException("Interrupted by user"))) {
            throw new RuntimeException("Failed to stop mesos even stream;");
        }
    }

    private Request request;

    @SuppressWarnings("unchecked")
    public void startRequest() {
//        EventReader eventReader = new EventReader(new EventProcessor(mesos, consul));
        try {
            String connectUri = "";
            if (config.uri.startsWith("zk://")) {
                ZooKeeper zk = new ZooKeeper(config.uri.substring(5), 60000, event -> {
                });

                List<String> children = zk.getChildren("/", false);
                OptionalLong serverIdx = children.stream().filter(it -> it.startsWith("json.info")).map(it -> it.split("_")[1]).mapToLong(Long::parseLong).min();
                if (serverIdx.isPresent()) {
                    String s = children.stream().filter(it -> it.endsWith(Long.toString(serverIdx.getAsLong()))).findFirst().get();
                    byte[] data = zk.getData("/" + s, false, null);
                    Map<String, Object> o = mapper.readValue(data, new TypeReference<HashMap<String, Object>>() {
                    });
                    zk.close();
                    Map<String, Object> address = (Map<String, Object>) o.get("address");
                    connectUri = "http://" + address.get("ip") + ":" + address.get("port") + "/api/v1";
                } else {
                    logger.error("Can't get any mesos servers.");
                }
            } else {
                connectUri = config.uri + "/api/v1";
            }

            request = client.newRequest(connectUri)
                    .method(HttpMethod.POST)
                    .content(new StringContentProvider("{\"type\": \"SUBSCRIBE\"}"))
                    .header(HttpHeader.CONTENT_TYPE, "application/json")
                    .header(HttpHeader.KEEP_ALIVE, "true")
                    .onResponseContent(this)
                    .onResponseFailure((response, failure) -> {
                        logger.error("Connection failed ...");
                    });
            request.send(this);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void onComplete(Result result) {
        if (result.getFailure() instanceof InterruptedException) {
            logger.info("Got interrupt !!! Exit ...");
            System.exit(0);
        } else if (result.getFailure() instanceof ForcedStopException) {
            logger.info("Stopping event stream by user request.");
        } else { //if failed for any other reson, restart it;
            startRequest();
        }
    }

    @Override
    public void onContent(Response response, ByteBuffer content) {
        if (!start) {
            start = true;
            int spos = content.position();
            byte b = 0;
            int size = 0;
            while (content.remaining() > 0 && b != 10) {
                b = content.get();
                if (b == 10) {
                    int rlen = content.position() - spos - 1;
                    byte[] data = new byte[rlen];
                    content.position(spos);
                    content.get(data);
                    eventSize = Integer.parseInt(new String(data));
                    content.get();
                }
            }
        }
        eventSize -= content.remaining();
        byte[] data = new byte[content.remaining()];
        content.get(data);
        sb.append(new String(data));
        if (eventSize == 0) {
            logger.debug("New event:" + sb.toString().substring(0, 300));
            process(sb.toString().getBytes());
            //queue.add(sb.toString());
            start = false;
            sb = new StringBuilder();
        }
    }

    private void process(byte[] data) {
        try {
            //Object o = mapper.readValue(data, Map.class);
            //Files.write(Paths.get("C:\\Users\\evilezh\\Desktop\\out1.json"),mapper.writeValueAsBytes(o));
            Event event = mapper.readValue(data, Event.class);
            logger.debug("Got event: " + event.getClass().getName());
            if (event instanceof SubscribedD) {
                //reset/sync mesos/consul.
//                mesos.reset();
//                consul.reset();
//                consul.syncCatalog();
                //fill agents
                eventReceiver.mesosEvent(MesosEvent.EVENT_TYPE.SUBSCRIBED_START, null);
                ((SubscribedD) event).subscribed.getState.getAgents.agents.forEach(this::addAgent);
                //fire addTask events for each task
                ((SubscribedD) event).subscribed.getState.getTasks.tasks.forEach(it -> eventReceiver.mesosEvent(MesosEvent.EVENT_TYPE.TASK_ADDED, it));
                eventReceiver.mesosEvent(MesosEvent.EVENT_TYPE.SUBSCRIBED_END, null);
//                consul.removeNonSynced();
            } else if (event instanceof TaskAddedD) {
//                logger.info("DATA: " + new String(data));
                eventReceiver.mesosEvent(MesosEvent.EVENT_TYPE.TASK_ADDED, ((TaskAddedD) event).taskAdded.task);
//                mesos.addTask(((TaskAddedD) event).taskAdded.task, consul);
            } else if (event instanceof TaskUpdatedD) {
//                logger.info("DATA: " + new String(data));

                eventReceiver.mesosEvent(MesosEvent.EVENT_TYPE.TASK_UPDATED, updateTask(((TaskUpdatedD) event).taskUpdated.status));
            } else if (event instanceof AgentAddedD) {
                logger.info("DATA: " + mapper.writeValueAsString(((AgentAddedD) event).agentAdded));
            } else if (event instanceof AgentRemovedD) {
                logger.info("DATA: " + mapper.writeValueAsString(((AgentRemovedD) event).agentRemoved));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, Task> getTasks() {
        return this.tasks;
    }

    public Task getTask(String id) {
        return tasks.get(id);
    }
}
