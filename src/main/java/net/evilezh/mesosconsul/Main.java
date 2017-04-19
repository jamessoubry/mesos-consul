package net.evilezh.mesosconsul;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.ext.dropwizard.MetricsService;
import net.evilezh.mesosconsul.consul.Consul;
import net.evilezh.mesosconsul.model.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    public static final HttpClient client;
    static Main instance;

    //initialize default http client
    static {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setTrustAll(true);
        client = new HttpClient(sslContextFactory);
        client.setTCPNoDelay(true);
        client.setIdleTimeout(0);
        try {
            client.start();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        //set JSONPath defaults to use Jackson
        Configuration.setDefaults(new Configuration.Defaults() {
            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }
        });

        //quit on any initialisation error
        try {
            //we need consul URL and PATH in consul where to read/write configurations. Token is optional.
            Properties properties = System.getProperties();
            if (!properties.containsKey("consul")) {
                logger.error("Consul address missing. Please specify -Dconsul=<consul URL>");
                System.exit(1);
            }
            if (!properties.containsKey("path")) {
                logger.error("Configuration path missing. Please specify -Dpath=<path to config>");
            }

            Config config = new Config();
            net.evilezh.mesosconsul.model.config.Consul consulConfig = new net.evilezh.mesosconsul.model.config.Consul();
            config.servicePrefix = "mconsul";
            consulConfig.address = properties.getProperty("consul");
            consulConfig.token = Optional.ofNullable(properties.getProperty("token"));
            consulConfig.path = properties.getProperty("path");

//            config.consul = consulConfig;
            Consul consul = new Consul(consulConfig);
            Engine cm = Engine.getEngine(consul);

            DropwizardMetricsOptions metricsOptions =
                    new DropwizardMetricsOptions()
                            .setBaseName("")
                            .setJmxEnabled(true)
                            .setJmxDomain("mesos-consul")
                            .setEnabled(true)
                            .setRegistryName("mesos-consul");


            Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(metricsOptions));
            MetricsService metricsService = MetricsService.create(vertx);
            vertx.deployVerticle(new RestAPI(cm));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            System.exit(1);
        }
    }
}
