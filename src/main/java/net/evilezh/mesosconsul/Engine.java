package net.evilezh.mesosconsul;

import com.codahale.metrics.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.evilezh.mesosconsul.consul.Consul;
import net.evilezh.mesosconsul.exceptions.*;
import net.evilezh.mesosconsul.mesos.Mesos;
import net.evilezh.mesosconsul.mesos.MesosEvent;
import net.evilezh.mesosconsul.model.config.Config;
import net.evilezh.mesosconsul.model.config.Transform;
import net.evilezh.mesosconsul.model.config.Transforms;
import net.evilezh.mesosconsul.model.mesos.Agent;
import net.evilezh.mesosconsul.model.mesos.Status;
import net.evilezh.mesosconsul.model.mesos.Task;
import net.evilezh.mesosconsul.transform.AbstractTranform;
import net.evilezh.mesosconsul.transform.TransformResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;

import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Engine implements MesosEvent {
    private static final Logger logger = LogManager.getLogger(Engine.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static Engine instance;

    //consul initialized at boot time and not configurable later.
    private Consul consul;

    //config and mesos can be reloaded/updated
    private Config config;
    private net.evilezh.mesosconsul.model.config.Mesos mesosConfig;
    private Mesos mesos;

    //list of configured transforms
    private final Map<String, AbstractTranform> transforms = new HashMap<>();
    private final Map<String, Class<? extends AbstractTranform>> transformEngines = new HashMap<>();

    //consul tasks will be executed via this thread pool
//    private final ExecutorService executorService; //= Executors.newFixedThreadPool(config.consulThreads);

    java.util.Timer timer;
    private final static MetricRegistry metrics = SharedMetricRegistries.getOrCreate("mesos-consul");
    private Meter serviceCreateFails;
    private Meter serviceCreateCalls;
    private Meter serviceDeregisterCalls;
    private Meter serviceDeregisterFails;
    private Meter consulNodeFailures;

    private FailRatio serviceRegisterFailRatio;
    private FailRatio serviceDeRegisterFailRatio;


    //service registration/deregistration queues and executor
//    private final BlockingQueue<Runnable> serviceTasks = new ArrayBlockingQueue<>(100);
    private ForkJoinPool serviceJobExecutor;


    private void initMeters() {
        serviceCreateFails = new Meter();
        serviceCreateCalls = new Meter();
        serviceDeregisterCalls = new Meter();
        serviceDeregisterFails = new Meter();
        consulNodeFailures = new Meter();
        serviceRegisterFailRatio = new FailRatio(serviceCreateFails, serviceCreateCalls);
        serviceDeRegisterFailRatio = new FailRatio(serviceDeregisterFails, serviceDeregisterCalls);

        metrics.register("service-create-fails", serviceCreateFails);
        metrics.register("service-create-calls", serviceCreateCalls);
        metrics.register("service-deregister-calls", serviceDeregisterCalls);
        metrics.register("service-deregister-fails", serviceDeregisterFails);
        metrics.register("service-register-fail-ratio", serviceRegisterFailRatio);
        metrics.register("service-deregister-fail-ratio", serviceDeRegisterFailRatio);
        metrics.register("consul-node-fail", consulNodeFailures);

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        timer = new java.util.Timer("health-check", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (consulNodeFailures.getOneMinuteRate() > 0.1 || serviceRegisterFailRatio.getRatio().getValue() > 0.1 || serviceDeRegisterFailRatio.getRatio().getValue() > 0.1) {
                    load();
                }
            }
        }, 1000, 1000);
    }

    //singleton
    private Engine(Consul consul) {
        this.consul = consul;
        //register internal transform classes
        transformEngines.put("regex", net.evilezh.mesosconsul.transform.Regex.class);
        load();
    }

    public synchronized static Engine getEngine(Consul consul) {
        if (instance == null)
            instance = new Engine(consul);
        return instance;
    }

    public synchronized static Engine getEngine() {
        if (instance == null)
            throw new RuntimeException("Invalid call, Engine should be initialized before.");
        return instance;
    }

    //force synchronization for load/reload
    private synchronized void load() {
        try {
            if (serviceJobExecutor != null) {
                serviceJobExecutor.shutdownNow();
            }

            //basic config
            String configJson = consul.getKey("config");
            this.config = Config.makeConfig(configJson.getBytes());

            loadTransforms();

            //serviceJobExecutor = new ThreadPoolExecutor(config.consulThreads, config.consulThreads, 1, TimeUnit.HOURS, serviceTasks);
            serviceJobExecutor = new ForkJoinPool(config.consulThreads);

            //mesos config
            String mesosConfigJson = consul.getKey("mesos");
            this.mesosConfig = net.evilezh.mesosconsul.model.config.Mesos.makeConfig(mesosConfigJson.getBytes());

            if (mesos != null) {
                mesos.stopProcessing();
                mesos = null;
            }

            mesos = new Mesos(mesosConfig, this);

            //force consul reset on new mesos
            consul.reset();

            initMeters();

            mesos.startRequest();
        } catch (KeyNotFoundException e) {
            logger.warn("Config item does not exist: " + e.getMessage());
        } catch (ConfigParseException e) {
            logger.error("Failed to load configuration. Parse error.");
        }
    }

    public Config getConfig() {
        return this.config;
    }

    public synchronized void setConfig(Config config) {
        if (mesos != null)
            mesos.destroy();
        if (config != null) {
            try {
                consul.setKey("config", mapper.writeValueAsString(config));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            load();
        }
    }

    public net.evilezh.mesosconsul.model.config.Mesos getMesosConfig() {
        return this.mesosConfig;
    }

    public synchronized void setMesosConfig(net.evilezh.mesosconsul.model.config.Mesos config) {
        if (mesos != null)
            mesos.destroy();
        if (config != null) {
            try {
                consul.setKey("mesos", mapper.writeValueAsString(config));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            load();
        }
    }


    //TRANSFORMS ------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    public Map<String, AbstractTranform> getTransforms() {
        return this.transforms;
    }

    public Map<String, Class<? extends AbstractTranform>> getTransformEngines() {
        return this.transformEngines;
    }


    private AbstractTranform createTransform(String name, Transform transConfig) throws Throwable {
        if (!transformEngines.containsKey(transConfig.type))
            throw new TransformTypeException("Transform with type: " + transConfig.type + " does not exist.");
        MethodHandle constructor = MethodHandles.lookup().findConstructor(transformEngines.get(transConfig.type), MethodType.methodType(void.class, String.class, Transform.class));
        return (AbstractTranform) constructor.invoke(name, transConfig);
    }

    public synchronized void addTransform(String name, Transform transConfig) throws Throwable {
        if (transforms.containsKey(name))
            throw new TransformExistException("Transform with name: " + name + " already exist. Please delete first.");
        AbstractTranform transform = createTransform(name, transConfig);
        //finally add into local register
        transforms.put(name, transform);

        writeTransforms();
    }


    public synchronized void enableTransform(String name) {
        transforms.get(name).config.enabled = true;
        //once transform is enabled, we need to re-generate all existing tasks
        Map<Task, List<TransformResult>> serviceMap = processNewTransform(transforms.get(name));
        for (List<TransformResult> results : serviceMap.values()) {
            for (TransformResult tr : results) {
                serviceJobExecutor.invoke(new RegisterService(tr, config.dryRun));
            }
        }
        consul.setKey("transforms/" + name + "/enabled", "true");
    }


    //used when enable transform to get list of tasks or for dry-run.
    private synchronized Map<Task, List<TransformResult>> processNewTransform(AbstractTranform at) {
        Map<Task, List<TransformResult>> data = mesos.getTasks().values().stream().collect(Collectors.toMap(it -> it, it -> at.transform(it, mesos.getAgent(it.agentId), config.servicePrefix)))
                .entrySet().stream().filter(it -> it.getValue().size() > 0).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        data.values().forEach(this::fillTransoformLocalData);
        return data;
    }


    private void fillTransoformLocalData(List<TransformResult> transformResults) {
        for (TransformResult transformResult : transformResults) {
            Agent agent = mesos.getAgent(transformResult.agentId);
            Task task = mesos.getTask(transformResult.taskId);
            Optional<Status> runningTask = task.statuses.stream().sorted(Comparator.comparingDouble(o -> o.timestamp)).filter(it -> it.state.equals("TASK_RUNNING")).findFirst();
            //just extra consistency check.
            if (runningTask.isPresent() && task.state.equals(Mesos.TASK_RUNNING)) {
                if (runningTask.get().containerStatus.networkInfos.size() > 0 && runningTask.get().containerStatus.networkInfos.get(0).ipAddresses.size() > 0) {
                    transformResult.serviceIp = runningTask.get().containerStatus.networkInfos.get(0).ipAddresses.get(0).ipAddress;
                    transformResult.hostIp = (String) agent.agentInfo.attributes.get("ip");
                } else {
                    throw new RuntimeException("Networkinfos or ip's missing.");
                }
            } else {
                throw new RuntimeException("Consistency failure.");
            }
        }
    }

    public synchronized void disableTransform(String name) {
        transforms.get(name).config.enabled = false;
    }

    public synchronized void removeTransform(String name) {
        if (transforms.containsKey(name))
            transforms.remove(name);
    }

    @Override
    public synchronized void mesosEvent(EVENT_TYPE eventType, Task task) {
//        logger.info("MESOS_EVENT: " + System.nanoTime());
        logger.debug(() -> {
            try {
                return "Event received: " + eventType + (task == null ? "" : ": " + mapper.writeValueAsString(task));
            } catch (JsonProcessingException e) {
                logger.error(e.getMessage(), e);
                return e.getMessage();
            }
        });

        switch (eventType) {
            case SUBSCRIBED_START:
                //reset and re-syunc consul agents and services
                consul.reset();
                consul.syncCatalog();
            case SUBSCRIBED_END:
                consul.removeNonSynced();
                break;
            case TASK_ADDED:
                mesos.addTask(task);
            case TASK_UPDATED:
                switch (task.state) {
                    case Mesos.TASK_RUNNING:
                        try {
                            Optional<Status> first = task.statuses.stream().sorted((o1, o2) -> Double.compare(o2.timestamp, o1.timestamp)).findFirst();
                            if (!first.isPresent())
                                throw new Exception("Integrity fail.");
                            if (first.get().healthy) {
//                            NodeEntry consulNode = findConsulNode(task, consul);
                                List<TransformResult> collect = transforms.entrySet().stream()
                                        .filter(t -> t.getValue().config.enabled)
                                        .flatMap(t -> t.getValue().transform(task, mesos.getAgent(task.agentId), config.servicePrefix).stream()).collect(Collectors.toList());
                                fillTransoformLocalData(collect);
                                for (TransformResult tr : collect) {
                                    serviceCreateCalls.mark();
                                    serviceJobExecutor.invoke(new RegisterService(tr, config.dryRun));
                                }
                            } else {
                                logger.info("DEREGISTER UNHEALTHY.");
                            }
                        } catch (Exception e) {
                            logger.error("Epic fail, full reload. " + e.getMessage());
                            load();
                        }
                        break;
                    case Mesos.TASK_STAGING:
                    case Mesos.TASK_UNREACHABLE:
                    case Mesos.TASK_GONE_BY_OPERATOR:
                        break;
                    default:
                        logger.debug("DEREGISTER: " + task.state);
                        serviceDeregisterCalls.mark();
                        DeRegisterService ds = new DeRegisterService(task, config.dryRun);
                        serviceJobExecutor.invoke(ds);
                        //consul.deRegister(task, config.dryRun);
                        break;
                }
                break;
        }
    }

    private synchronized void repairConsul() {
        logger.info("Entering repair mode. Consul failure.");

    }

    public Mesos getMesos() {
        return this.mesos;
    }

    public Optional<Map<String, List<TransformResult>>> dryrun(String type, Map<String, Object> config) throws Throwable {
        Transform transConfig = new Transform();
        transConfig.config = config;
        transConfig.type = type;

        AbstractTranform transform = createTransform("dryrun", transConfig);
        Map<Task, List<TransformResult>> serviceMap = processNewTransform(transform);

        //Map<Task, List<ConsulService>> serviceMap = consul.mapToServices(processNewTransform(transformOpt.get()), this.config.servicePrefix, "dryrun");
        return Optional.ofNullable(serviceMap.entrySet().stream().collect(Collectors.toMap(it -> it.getValue().get(0).taskId, Map.Entry::getValue)));
    }

    private class RegisterService extends RecursiveAction {
        boolean dryRun;
        TransformResult tr;

        RegisterService(TransformResult tr, boolean dryRun) {
            this.tr = tr;
            this.dryRun = dryRun;

        }

        @Override
        protected void compute() {
            try {
                consul.register(tr, dryRun);
            } catch (ConsulNodeNotFoundException ce) {
                logger.error(ce.getMessage());
                consulNodeFailures.mark();
            } catch (Exception e) {
                serviceCreateFails.mark();
                RegisterService rs = new RegisterService(tr, dryRun);
                rs.fork();
            }
        }
    }

    private class DeRegisterService extends RecursiveAction {
        boolean dryRun;
        Task task;

        DeRegisterService(Task task, boolean dryRun) {
            this.task = task;
            this.dryRun = dryRun;

        }

        @Override
        protected void compute() {
            try {
                consul.deRegister(config.servicePrefix, task, config.dryRun);
            } catch (ConsulNodeNotFoundException ce) {
                logger.error(ce.getMessage());
                consulNodeFailures.mark();
            } catch (Exception e) {
                serviceCreateFails.mark();
                DeRegisterService rs = new DeRegisterService(task, dryRun);
                rs.fork();
            }
        }
    }

    public void writeTransforms() throws IOException {
        Transforms tfs = new Transforms();
        tfs.transforms = transforms.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, it -> it.getValue().config));
        consul.writeTransforms(tfs);
    }

    //load only once on startup
    private synchronized void loadTransforms() {
        Map<String, Object> t = new HashMap<>();

        Map<String, Object> cfgTransforms = consul.getTree("transforms");
        t.put("transforms", cfgTransforms);
        try {
            Transforms tfs = mapper.readValue(mapper.writeValueAsBytes(t), Transforms.class);
            if (tfs.transforms != null) {
                for (Map.Entry<String, Transform> entry : tfs.transforms.entrySet()) {
                    try {
                        AbstractTranform at = createTransform(entry.getKey(), entry.getValue());
                        transforms.put(entry.getKey(), at);
                    } catch (Throwable throwable) {
                        logger.error("Failed to load transform: " + entry.getKey());
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to read transforms configuration from consul.", e);
        }
    }
}
