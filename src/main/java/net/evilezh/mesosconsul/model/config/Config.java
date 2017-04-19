package net.evilezh.mesosconsul.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.evilezh.mesosconsul.exceptions.ConfigParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Config {
    private static final Logger logger = LogManager.getLogger(Config.class);

    //default to dry run mode
    @JsonProperty("dry-run")
    public boolean dryRun = true;

    @JsonProperty("service-prefix")
    public String servicePrefix = "mconsul";

    @JsonProperty("max-consul-call-threads")
    public int consulThreads = 10;

    public static Config makeConfig(byte[] bytes) throws ConfigParseException {
        ObjectMapper om = new ObjectMapper();
        try {
            return om.readValue(bytes, Config.class);
        } catch (Exception e) {
            throw new ConfigParseException(e);
        }
    }
}
