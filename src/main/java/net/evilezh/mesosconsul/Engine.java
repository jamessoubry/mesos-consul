package net.evilezh.mesosconsul;

import net.evilezh.mesosconsul.model.config.Config;
import net.evilezh.mesosconsul.transform.Regex;
import net.evilezh.mesosconsul.transform.Transform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;

import static net.evilezh.mesosconsul.Main.client;

public class Engine implements Response.CompleteListener {
    private static final Logger logger = LogManager.getLogger(Engine.class);
    private final Config config;

    public Engine(Config config) {
        this.config = config;

    }

    public void startRequest() {

        Consul consul = new Consul(config);
        //Transform transform = new Regex();

        Mesos mesos = new Mesos(config);
        EventReader eventReader = new EventReader(new EventProcessor(mesos, consul));

        Request request = client.newRequest("http://master.mesos.service.consul:5050/api/v1/executor")
                .method(HttpMethod.POST)
                .content(new StringContentProvider("{\"type\": \"SUBSCRIBE\"}"))
                .header(HttpHeader.CONTENT_TYPE, "application/json")
                .header(HttpHeader.KEEP_ALIVE, "true")
                .onResponseContent(eventReader)
                .onResponseFailure((response, failure) -> {
                    logger.error("Connection failed ...");
                });
        request.send(this);
    }

    @Override
    public void onComplete(Result result) {
        if (result.getFailure() instanceof InterruptedException) {
            logger.info("Got interrupt !!! Exit ...");
            System.exit(0);
        } else {
            startRequest();
        }

    }
}
