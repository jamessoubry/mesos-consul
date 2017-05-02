package net.evilezh.mesosconsul.consul;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.Json;
import net.evilezh.mesosconsul.Engine;
import net.evilezh.mesosconsul.exceptions.ConsulNodeNotFoundException;
import net.evilezh.mesosconsul.exceptions.KeyNotFoundException;
import net.evilezh.mesosconsul.model.config.Transform;
import net.evilezh.mesosconsul.model.config.Transforms;
import net.evilezh.mesosconsul.model.consul.ConsulService;
import net.evilezh.mesosconsul.model.consul.Node;
import net.evilezh.mesosconsul.model.consul.NodeEntry;
import net.evilezh.mesosconsul.model.mesos.Task;
import net.evilezh.mesosconsul.transform.TransformResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.evilezh.mesosconsul.Main.client;
import static net.evilezh.mesosconsul.Engine.getEngine;

public class Consul {
    public static final String VERB_SET = "set";
    public static final String VERB_DELETE = "delete";
    public static final String VERB_DELETE_TREE = "delete-tree";

    private static final Logger logger = LogManager.getLogger(Consul.class);
    //    private Optional<String> token;
//    private String address;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final net.evilezh.mesosconsul.model.config.Consul config;

    private List<NodeEntry> nodes = new ArrayList<>();


    public Consul(net.evilezh.mesosconsul.model.config.Consul config) {
        this.config = config;
        //remove tailing slash
        if (config.path.endsWith("/"))
            config.path = config.path.substring(0, config.path.length() - 1);
    }

    public String getKey(String path) throws KeyNotFoundException {
        String requestKey = config.address + "/v1/kv/" + config.path + "/" + (path.startsWith("/") ? path.substring(1) : path) + "?raw";
        try {
            Request request = client.newRequest(requestKey).method(HttpMethod.GET);
            if (config.token.isPresent())
                request = request.header("X-Consul-token", config.token.get());
            ContentResponse response = request.send();

            if (response.getStatus() == 404)
                throw new KeyNotFoundException("Not found: " + requestKey);

            if (response.getStatus() > 299)
                throw new RuntimeException("Response status: " + response.getStatus() + ", response message: " + response.getContentAsString());

            return response.getContentAsString();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void setKey(String path, String data) {
        String requestKey = config.address + "/v1/kv/" + config.path + "/" + (path.startsWith("/") ? path.substring(1) : path);
        Request request = client.newRequest(requestKey)
                .method(HttpMethod.PUT)
                .content(new StringContentProvider(data));
        if (config.token.isPresent())
            request = request.header("X-Consul-token", config.token.get());
        try {
            ContentResponse response = request.send();
            if (response.getStatus() > 299)
                throw new RuntimeException("Response status: " + response.getStatus() + ", response message: " + response.getContentAsString());
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    public void setKeys(Map<String, String> keys) {
        List<Map<String, Object>> collect = keys.entrySet().stream().map(entry -> {
            Map<String, Object> ret = new HashMap<>();
            Map<String, String> data = new HashMap<>();
            data.put("Verb", "set");
            data.put("Key", config.path + "/" + entry.getKey());
            data.put("Value", Base64.getEncoder().encodeToString(entry.getValue().getBytes()));
            ret.put("KV", data);
            return ret;
        }).collect(Collectors.toList());
        String requestKey = config.address + "/v1/txn";
        try {
            Request request = client.newRequest(requestKey)
                    .method(HttpMethod.PUT)
                    .content(new StringContentProvider(mapper.writeValueAsString(collect)));
            if (config.token.isPresent())
                request = request.header("X-Consul-token", config.token.get());

            ContentResponse response = request.send();
            if (response.getStatus() > 299)
                throw new RuntimeException("Response status: " + response.getStatus() + ", response message: " + response.getContentAsString());
        } catch (InterruptedException | TimeoutException | ExecutionException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> getFlatTree(String path) {
        String req = "[{\"KV\": {\"Verb\": \"get-tree\",\"Key\": \"" + config.path + "/" + path + "\"}}]";
        String requestKey = config.address + "/v1/txn";
        try {
            Request request = client.newRequest(requestKey)
                    .method(HttpMethod.PUT)
                    .content(new StringContentProvider(req));
            if (config.token.isPresent())
                request = request.header("X-Consul-token", config.token.get());

            ContentResponse response = request.send();
            if (response.getStatus() > 299)
                throw new RuntimeException("Response status: " + response.getStatus() + ", response message: " + response.getContentAsString());
            ConsulTXNResult result = mapper.readValue(response.getContent(), ConsulTXNResult.class);
            return result.results.stream().map(it -> it.kv).collect(Collectors.toMap(it2 -> it2.key.replace(this.config.path + "/", "").replace(path.length() > 0 ? path + "/" : path, ""), it2 -> it2.value));
        } catch (InterruptedException | TimeoutException | ExecutionException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> getTree(String path) {
        Map<String, String> flatTree = getFlatTree(path);
        Map<String, Object> ret = new HashMap<>();
        flatTree.forEach((key, value) -> mergePath(key.split("\\/"), value, ret, 0));
        return ret;
    }

    public void removeNonSynced() {

    }

    public void reset() {
        nodes.clear();
    }

    public synchronized List<NodeEntry> getNodes() {
        return this.nodes;
    }

    public synchronized void syncCatalog() {
        long z = System.currentTimeMillis();
        try {
            //final List<Node> nodes = new ArrayList<>();
            while (true) {
                try {
                    ContentResponse get = client.GET(config.address + "/v1/catalog/nodes" + (config.token.isPresent() ? "?token=" + config.token.get() : ""));
                    if (get.getStatus() == 200) {
                        Node[] nodeList = mapper.readValue(get.getContent(), Node[].class);
                        Semaphore s = new Semaphore(nodeList.length);
                        s.acquire(nodeList.length);
                        for (Node node : nodeList) {
                            client.newRequest(config.address + "/v1/catalog/node/" + node.id + (config.token.isPresent() ? "?token=" + config.token.get() : "")).method(HttpMethod.GET)
                                    .send(new BufferingResponseListener(10 * 1024 * 1024) {
                                        @Override
                                        public void onComplete(Result result) {
                                            try {
                                                NodeEntry nodeEntry = mapper.readValue(getContent(), NodeEntry.class);
                                                logger.trace("Decoded entry: " + getContentAsString());
                                                nodes.add(nodeEntry);
                                                s.release();
                                            } catch (IOException e) {
                                                logger.error(e.getMessage());
                                                logger.error("Failed to decode in NodeEntry: " + getContentAsString());
                                            }
                                        }
                                    });
                        }
                        if (s.tryAcquire(nodeList.length, 30, TimeUnit.SECONDS))
                            break;
                    } else {
                        Thread.sleep(config.retryInterval);
                    }
                } catch (ExecutionException | TimeoutException | IOException e) {
                    logger.error(e.getMessage(), e);
                    Thread.sleep(config.retryInterval);
                }
            }
            logger.info("Synced consul agents and services. Time took: " + (System.currentTimeMillis() - z) + "ms.");
        } catch (InterruptedException e) {
            logger.info("Interrupt while syncing consul catalog ...");
            Thread.currentThread().interrupt();
        }
    }

    private NodeEntry findConsulNode(String agentIp) throws ConsulNodeNotFoundException {
        return nodes.stream().filter(it ->
                it.node.address.equals(agentIp) ||
                        (it.node.taggedAddresses != null && it.node.taggedAddresses.lan != null && it.node.taggedAddresses.lan.equals(agentIp)) ||
                        (it.node.taggedAddresses != null && it.node.taggedAddresses.wan != null && it.node.taggedAddresses.wan.equals(agentIp)))
                .findFirst()
                .orElseThrow(() -> new ConsulNodeNotFoundException("Can find consul node for ip: " + agentIp));
    }

    public void register(TransformResult tr, boolean dryRun) throws ConsulNodeNotFoundException {
        ConsulService service = mapToService(tr);
        NodeEntry entry = findConsulNode(tr.hostIp);

        //check for existence
        if (entry.services.entrySet().stream().noneMatch(it -> it.getKey().equals(service.id))) {
            entry.services.put(service.id, service);
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("Node", entry.node.id);
                data.put("Address", entry.node.address);
                data.put("Service", service);
                String s = mapper.writeValueAsString(data);
                if (!dryRun) {
/*
                    Request request = client.newRequest(config.address + "/v1/catalog/register")
                            .method(HttpMethod.PUT)
                            .content(new StringContentProvider(s))
                            .header("content-type", "application/json");
                    if (config.token.isPresent())
                        request = request.header("X-Consul-token", config.token.get());
                    try {
                        ContentResponse response = request.send();
                        if (response.getStatus() > 299)
                            throw new RuntimeException("Response status: " + response.getStatus() + ", response message: " + response.getContentAsString());
                    } catch (InterruptedException | TimeoutException | ExecutionException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
*/
                } else {
                    logger.info("DRYRUN: Register service: " + s);
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

        }
    }

    private ConsulService mapToService(TransformResult tr) {
        ConsulService service = new ConsulService();
        service.port = tr.port;
        service.id = tr.servicePrefix + ":" + tr.taskId + ":" + tr.transformName + ":" + service.port;
        service.name = tr.taskName;
        service.address = tr.serviceIp;
        service.synced = true;
        service.tags = new ArrayList<>();
        service.tags.add(tr.portName);
        return service;
    }

    //public call
    public Map<Task, List<ConsulService>> mapToServices(Map<Task, List<TransformResult>> serviceMap, String servicePrefix, String transformName) {
        return serviceMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
            List<TransformResult> transformResults = entry.getValue();
            Task task = entry.getKey();
            String ip = task.statuses.stream().filter(it -> it.state.equals("TASK_RUNNING")).findFirst().get().containerStatus.networkInfos.get(0).ipAddresses.get(0).ipAddress;
            return transformResults.stream().map(it -> {
                ConsulService service = new ConsulService();
                service.port = it.port;
                service.id = servicePrefix + ":" + task.taskId + ":" + transformName + ":" + service.port;
                service.name = it.taskName;
                service.address = ip;
                service.synced = true;
                service.tags = new ArrayList<>();
                service.tags.add(it.portName);
                return service;
            }).collect(Collectors.toList());
        }));
    }

    public List<ConsulService> mapToServices(Task task, List<TransformResult> transformResults, String servicePrefix, String transformName) {
        String ip = task.statuses.stream().filter(it -> it.state.equals("TASK_RUNNING")).findFirst().get().containerStatus.networkInfos.get(0).ipAddresses.get(0).ipAddress;
        return transformResults.stream().map(it -> {
            ConsulService service = new ConsulService();
            service.port = it.port;
            service.id = servicePrefix + ":" + task.taskId + ":" + transformName + ":" + service.port;
            service.name = it.taskName;
            service.address = ip;
            service.synced = true;
            service.tags = new ArrayList<>();
            service.tags.add(it.portName);
            return service;
        }).collect(Collectors.toList());
    }

    public void deRegister(String prefix, Task task, boolean dryRun) throws ConsulNodeNotFoundException {
        //transformResult.hostIp = (String) agent.agentInfo.attributes.get("ip");
        //TODO long scan in case if agent is gone and de-registered already
        NodeEntry consulNode = findConsulNode((String) Engine.getEngine().getMesos().getAgent(task.agentId).agentInfo.attributes.get("ip"));
        consulNode.services.values().removeIf(it -> {
            boolean b = it.id.startsWith(prefix + ":" + task.taskId + ":");
            if (b) {
                if (!dryRun) {
                    //remove service from consul
                } else {
                    logger.info("DRYRUN: Deregister task: " + task.taskId + ", service: " + Json.encode(it));

                }
            }
            return b;

        });
    }


/*    public void deRegisterService(ConsulService service, NodeEntry entry, boolean dryRun) throws InterruptedException {
        Map<String, String> data = new HashMap<>();
        entry.services.remove(service.id);
        if (!dryRun) {
            Pair<ConsulService, NodeEntry> t = new Pair<>(service, entry);
            //deRegisterQueue.remove(t);
        }
        data.put("Node", entry.node.id);
        data.put("ServiceID", service.id);
        try {
            logger.info("REMOVE SERVICE: " + mapper.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        if (!dryRun) {
            //TODO write to consul
        }
    }*/

    public static void main(String[] args) {
        net.evilezh.mesosconsul.model.config.Consul consulConfig = new net.evilezh.mesosconsul.model.config.Consul();
        consulConfig.address = "http://consul.service.consul:8500";
        consulConfig.token = Optional.of("f8e64451-18b3-47e5-af52-89ef29df827a");
        consulConfig.path = "configuration/mesos-consul";
        Consul consul = new Consul(consulConfig);
        Map<String, Object> tree = consul.getTree("");
        logger.info("done");
//        consul.syncCatalog();
    }

    private class Pair<A, B> {

        final A a;
        final B b;

        Pair(A a, B b) {
            this.a = a;
            this.b = b;
        }

    }

    @SuppressWarnings("unchecked")
    private void mergePath(String[] path, Object value, Map<String, Object> data, int idx) {
        String key = path[idx];
        if (idx == path.length - 1) {
            data.put(key, value);
        } else {
            if (!data.containsKey(path[idx]))
                data.put(key, new HashMap<>());
            mergePath(path, value, (Map<String, Object>) data.get(key), ++idx);
        }
    }

    //atomic write. First delete all tree, then dump what we have
    public void writeTransforms(Transforms tfs) {
        List<KVRequestEntry> txnRequest = new ArrayList<>();
        KVRequestEntry requestEntry = new KVRequestEntry();
        KVRequest delete = new KVRequest();
        delete.verb = VERB_DELETE_TREE;
        delete.key = config.path + "/ " + "transforms";
        requestEntry.kv = delete;
        txnRequest.add(requestEntry);
        try {
            for (Map.Entry<String, Transform> entry : tfs.transforms.entrySet()) {
//        tfs.transforms.entrySet().stream().map(entry -> {
                //enabled
                KVRequestEntry re = new KVRequestEntry();
                KVRequest req = new KVRequest();
                req.verb = VERB_SET;
                req.key = config.path + "/transforms/" + entry.getKey() + "/enabled";
                req.setValue(Boolean.toString(entry.getValue().enabled));
                re.kv = req;
                txnRequest.add(re);
                //type
                re = new KVRequestEntry();
                req = new KVRequest();
                req.verb = VERB_SET;
                req.key = config.path + "/transforms/" + entry.getKey() + "/type";
                req.setValue(entry.getValue().type);
                re.kv = req;
                txnRequest.add(re);
                //config
                re = new KVRequestEntry();
                req = new KVRequest();
                req.verb = VERB_SET;
                req.key = config.path + "/transforms/" + entry.getKey() + "/config";
                req.setValue(mapper.writeValueAsString(entry.getValue().config));
                re.kv = req;
                txnRequest.add(re);
            }
            String requestKey = config.address + "/v1/txn";
            Request request = client.newRequest(requestKey)
                    .method(HttpMethod.PUT)
                    .content(new StringContentProvider(mapper.writeValueAsString(txnRequest)));
            if (config.token.isPresent())
                request = request.header("X-Consul-token", config.token.get());
            ContentResponse response = request.send();
            if (response.getStatus() > 299)
                throw new RuntimeException("Response status: " + response.getStatus() + ", response message: " + response.getContentAsString());
        } catch (InterruptedException | TimeoutException | ExecutionException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    static final Pattern mesosTaskPattern = Pattern.compile("(?<prefix>.*?):(?<taskId>.*?):(?<transform>.*?):(?<port>.*)");

    public List<ConsulService> getServices(String variable, String data) {
        String prefix = getEngine().getConfig().servicePrefix;
        return nodes.stream().flatMap(entry -> entry.services.values().stream()).filter(service -> {
            Matcher matcher = mesosTaskPattern.matcher(service.id);
            return matcher.matches() && matcher.group("prefix").equals(prefix) && matcher.group(variable).equals(data);
        }).collect(Collectors.toList());
    }
}
