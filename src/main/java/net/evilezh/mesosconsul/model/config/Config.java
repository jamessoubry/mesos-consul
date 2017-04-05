package net.evilezh.mesosconsul.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

public class Config {
    private static final Logger logger = LogManager.getLogger(Config.class);

    public Mesos mesos;
    public Consul consul;
    public List<Transform> transform;
    @JsonProperty("sleep-on-error-ms")
    public long sleep;
    @JsonProperty("dry-run")
    public boolean dryDrun;

    public static Optional<Config> makeConfig(byte[] bytes) {
        ObjectMapper om = new ObjectMapper();
        try {
            return Optional.ofNullable(om.readValue(bytes, Config.class));
        } catch (Exception e) {
            logger.error("Failed to read configuration ... ", e);
            return Optional.empty();
        }
    }

    public static boolean isValid(byte[] bytes) {
        return makeConfig(bytes).isPresent();
    }
}
