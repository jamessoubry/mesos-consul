package net.evilezh.mesosconsul;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.MetricsService;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import net.evilezh.mesosconsul.exceptions.ConfigParseException;
import net.evilezh.mesosconsul.model.config.Config;

import net.evilezh.mesosconsul.model.config.Mesos;
import net.evilezh.mesosconsul.model.config.Transform;
import net.evilezh.mesosconsul.transform.TransformResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


public class RestAPI extends AbstractVerticle {
    private static final Logger logger = LogManager.getLogger(RestAPI.class);

    private static final ObjectMapper mapper = new ObjectMapper();
    private final Engine manager;

    public RestAPI(Engine manager) {
        this.manager = manager;
    }


    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Router router = Router.router(vertx);
        router.route("/v1/*").handler(rc -> {
            rc.response().headersEndHandler((v) -> {
                if (rc.response().getStatusCode() == 200 || rc.response().getStatusCode() == 201)
                    rc.response().headers().add(HttpHeaders.CONTENT_TYPE, "application/json");
            });
            rc.next();
        });
        router.route().handler(BodyHandler.create());

        router.get("/metrics").handler(this::getMetrics);

        router.get("/v1/config").handler(this::getConfig);
        router.put("/v1/config").handler(this::setConfig);

        router.get("/v1/mesos-config").handler(this::getMesosConfig);
        router.put("/v1/mesos-config").handler(this::setMesosConfig);

        router.get("/v1/state").handler(this::getState);

        router.get("/v1/transform").handler(this::listTransforms);
        router.get("/v1/transform/:name").handler(this::getTransform);
        router.put("/v1/transform/:name/type/:type").handler(this::addTransform);
        router.delete("/v1/transform/:name").handler(this::deleteTransform);
        router.put("/v1/transform/:name/enable").handler(this::enableTransform);
        router.put("/v1/transform/:name/disable").handler(this::disableTransform);
        router.get("/v1/transform/:name/status").handler(this::getTransformStatus);

        router.get("/v1/consul/services").handler(null);
        router.get("/v1/consul/services/:transform").handler(null);
        router.delete("/v1/consul/services/:transform").handler(null);

        router.post("/v1/dry-run/:type").handler(this::dryRun);


        vertx.createHttpServer().requestHandler(router::accept).listen(80);


    }

    private void getMetrics(RoutingContext rc) {
        MetricsService metricsService = MetricsService.create(vertx);
        JsonObject metrics = metricsService.getMetricsSnapshot(vertx);
        rc.response().end(Json.encode(metrics));
    }

    private void getConfig(RoutingContext rc) {
        rc.response().end(Json.encode(manager.getConfig() == null ? new HashMap<>() : manager.getConfig()));
    }

    private void setConfig(RoutingContext rc) {
        try {
            Config config = Config.makeConfig(rc.getBody().getBytes());
            manager.setConfig(config);
            rc.response().setStatusCode(201);
            rc.response().end(Json.encode(config));
        } catch (ConfigParseException e) {
            logger.error(e.getMessage(), e);
            rc.response().setStatusCode(400);
            rc.response().end();
        }
    }

    private void getMesosConfig(RoutingContext rc) {
        rc.response().end(Json.encode(manager.getMesosConfig() == null ? new HashMap<>() : manager.getMesosConfig()));
    }

    private void setMesosConfig(RoutingContext rc) {
        try {
            Mesos mesosConfig = Mesos.makeConfig(rc.getBody().getBytes());
            manager.setMesosConfig(mesosConfig);
            rc.response().setStatusCode(201);
            rc.response().end(Json.encode(mesosConfig));
        } catch (ConfigParseException e) {
            logger.error(e.getMessage(), e);
            rc.response().setStatusCode(400);
            rc.response().end();
        }
    }

    private void getState(RoutingContext rc) {
        Map<String, Object> state = new HashMap<>();
        state.put("config", manager.getConfig() == null ? "missing" : "ok");
        state.put("mesos-config", manager.getMesosConfig() == null ? "missing" : "ok");
        state.put("transforms", manager.getTransforms().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, it -> it.getValue().config.enabled ? "enabled" : "disabled")));
        state.put("transform-engines", manager.getTransformEngines().keySet());
        rc.response().setStatusCode(200);
        rc.response().end(Json.encode(state));
    }

    private void dryRun(RoutingContext rc) {
        try {
            String type = rc.request().getParam("type");
            Map<String, Object> transformConfig = mapper.readValue(rc.getBody().getBytes(), new TypeReference<HashMap<String, Object>>() {
            });
            Optional<Map<String, List<TransformResult>>> dryrun = manager.dryrun(type, transformConfig);
            if (dryrun.isPresent()) {
                rc.response().setStatusCode(200);
                rc.response().end(Json.encode(dryrun.get()));
            } else {
                rc.response().setStatusCode(400);
                rc.response().end();
            }
        } catch (Throwable throwable) {
            logger.error(throwable.getMessage(), throwable);
            rc.response().setStatusCode(400);
            rc.response().end();
        }
    }

    private void listTransforms(RoutingContext rc) {
        rc.response().end(Json.encode(manager.getTransforms()));
    }

    private void addTransform(RoutingContext rc) {
        try {
            String type = rc.request().getParam("type");
            String name = rc.request().getParam("name");

            Map<String, Object> transformConfig = mapper.readValue(rc.getBody().getBytes(), new TypeReference<HashMap<String, Object>>() {
            });
            Transform transform = new Transform();
            transform.enabled = false;
            transform.type = type;
            transform.config = transformConfig;

            manager.addTransform(name, transform);
            rc.response().setStatusCode(201);
            rc.response().end(Json.encode(transform));
        } catch (JsonParseException | JsonMappingException e) {
            rc.response().setStatusCode(400);
            rc.response().end("{}");
        } catch (Throwable throwable) {
            logger.error(throwable.getMessage(), throwable);
            rc.response().setStatusCode(400);
            rc.response().end();
        }
    }

    private void getTransform(RoutingContext rc) {
        String name = rc.request().getParam("name");
        if (manager.getTransforms().containsKey(name)) {
            rc.response().setStatusCode(200);
            rc.response().end(Json.encode(manager.getTransforms().get(name)));
        } else {
            rc.response().setStatusCode(404);
            rc.response().end();
        }
    }

    private void deleteTransform(RoutingContext rc) {

    }

    private void enableTransform(RoutingContext rc) {
        try {
            String name = rc.request().getParam("name");
            manager.enableTransform(name);
            rc.response().setStatusCode(200);
            rc.response().end(Json.encode(manager.getTransforms().get(name)));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            rc.response().setStatusCode(400);
            rc.response().end();
        }
    }

    private void disableTransform(RoutingContext rc) {

    }

    private void getTransformStatus(RoutingContext rc) {

    }


    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        super.stop(stopFuture);
    }

    public void addServices() {

/*
        //post test data for dry run
        http.post("/v1/dryrun/:type", (q, a) -> {
            try {
                String type = q.params(":type");
                Map<String, Object> config = mapper.readValue(q.bodyAsBytes(), new TypeReference<HashMap<String, Object>>() {
                });

                Optional<Map<String, List<ConsulService>>> dryrun = manager.dryrun(type, config);
                if (dryrun.isPresent()) {
                    a.status(200);
                    return dryrun.get();
                } else {
                    a.status(400);
                    return null;
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                a.status(400);
            }
            return null;
        }, new JsonTransformer());

        http.after((q, a) -> a.header("content-type", "application/json"));
*/
    }


    String swaggerYaml = "swagger: '2.0'\n" +
            "info:\n" +
            "  title: Mesos-consul api\n" +
            "  version: \"1.0.0\"\n" +
            "host: localhost\n" +
            "schemes:\n" +
            "  - http\n" +
            "basePath: /v1\n" +
            "produces:\n" +
            "  - application/json\n" +
            "paths:\n" +
            "  /config:\n" +
            "    get:\n" +
            "      summary: Product Types\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: Ok\n" +
            "        400: \n" +
            "          description: Config missing\n" +
            "        default:\n" +
            "          description: Unexpected error\n" +
            "    put:\n" +
            "      summary: Update config\n" +
            "      produces:\n" +
            "        - application/json\n" +
            "      parameters: \n" +
            "        - name: body\n" +
            "          in: body\n" +
            "          description: Config json\n" +
            "          required: true\n" +
            "          schema:\n" +
            "            \"$ref\": \"#/definitions/inBody\"\n" +
            "      responses:\n" +
            "        201:\n" +
            "          description: Config update/created\n" +
            "        default:\n" +
            "          description: Unexpected error\n" +
            "\n" +
            "definitions:\n" +
            "  inBody:\n" +
            "    type: object        \n";
}
