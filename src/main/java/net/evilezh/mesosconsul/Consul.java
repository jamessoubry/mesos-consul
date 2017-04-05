package net.evilezh.mesosconsul;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.evilezh.mesosconsul.model.config.Config;
import net.evilezh.mesosconsul.model.consul.Node;
import net.evilezh.mesosconsul.model.consul.NodeEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.http.HttpMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.evilezh.mesosconsul.Main.client;

public class Consul {
    private static final Logger logger = LogManager.getLogger(Consul.class);
    //    private Optional<String> token;
//    private String address;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Config config;

    public final ArrayList<NodeEntry> nodes = new ArrayList<>();

/*
    public Consul(String address, Optional<String> token) {
        mapper = new ObjectMapper();
        this.address = address;
        this.token = token;
        config = Optional.empty();
    }
*/

    public Consul(Config config) {
        this.config = config;
    }

    public String getKey(String path) throws Exception {
        ContentResponse response = client.GET(config.consul.address + "/v1/kv/" + (path.startsWith("/") ? path.substring(1) : path) + "?raw" + (config.consul.token.isPresent() ? "&token=" + config.consul.token.get() : ""));
        if (response.getStatus() > 299)
            throw new Exception("Response status: " + response.getStatus() + ", response message: " + response.getContentAsString());
        return response.getContentAsString();
    }

    public void updateTask() {

    }

    public void resetTask() {

    }

    public void reset() {
        nodes.clear();
        syncCatalog();
    }

    private void syncCatalog() {
        try {
            //final List<Node> nodes = new ArrayList<>();
            while (true) {
                try {
                    ContentResponse get = client.GET(config.consul.address + "/v1/catalog/nodes" + (config.consul.token.isPresent() ? "?token=" + config.consul.token.get() : ""));
                    if (get.getStatus() == 200) {
                        Node[] nodeList = mapper.readValue(get.getContent(), Node[].class);
                        Semaphore s = new Semaphore(nodeList.length);
                        s.acquire(nodeList.length);
                        for (Node node : nodeList) {
                            client.newRequest(config.consul.address + "/v1/catalog/node/" + node.id + (config.consul.token.isPresent() ? "?token=" + config.consul.token.get() : "")).method(HttpMethod.GET)
                                    .send(new BufferingResponseListener(10 * 1024 * 1024) {
                                        @Override
                                        public void onComplete(Result result) {
                                            try {
                                                NodeEntry nodeEntry = mapper.readValue(getContent(), NodeEntry.class);
                                                logger.info("Decoded entry: " + getContentAsString());
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
                        Thread.sleep(config.sleep);
                    }
                } catch (ExecutionException | TimeoutException | IOException e) {
                    logger.error(e.getMessage(), e);
                    Thread.sleep(config.sleep);
                }
            }
            logger.info("Job done");
        } catch (InterruptedException e) {
            logger.info("Interrupt while syncing consul catalog ...");
            Thread.currentThread().interrupt();

        }
    }

    public static void main(String[] args) {
        Config config = new Config();
        config.sleep = 5000;
        net.evilezh.mesosconsul.model.config.Consul consulConfig = new net.evilezh.mesosconsul.model.config.Consul();
        consulConfig.address = "http://consul.service.consul:8500";
        consulConfig.token = Optional.of("f8e64451-18b3-47e5-af52-89ef29df827a");
        config.consul = consulConfig;
        Consul consul = new Consul(config);
        consul.syncCatalog();
    }
}
