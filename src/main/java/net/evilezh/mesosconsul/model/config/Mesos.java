package net.evilezh.mesosconsul.model.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.evilezh.mesosconsul.exceptions.ConfigParseException;

public class Mesos {
    public String uri;
    public String principal;
    public String secret;

    public static Mesos makeConfig(byte[] bytes) throws ConfigParseException {
        ObjectMapper om = new ObjectMapper();
        try {
            return om.readValue(bytes, Mesos.class);
        } catch (Exception e) {
            throw new ConfigParseException(e);
        }
    }

}
