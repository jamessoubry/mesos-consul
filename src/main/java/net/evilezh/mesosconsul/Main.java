package net.evilezh.mesosconsul;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import net.evilezh.mesosconsul.model.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    public static final HttpClient client;
    static Main instance;

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

        try {
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
            config.sleep = 5000;
            consulConfig.address = properties.getProperty("consul");
            consulConfig.token = Optional.ofNullable(properties.getProperty("token"));
            config.consul = consulConfig;
            Consul consul = new Consul(config);
            ObjectMapper om = new ObjectMapper();
            Config cfg = om.readValue(consul.getKey(properties.getProperty("path")), Config.class);
            Engine engine = new Engine(cfg);
            engine.startRequest();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            System.exit(1);
        }


    }
}
