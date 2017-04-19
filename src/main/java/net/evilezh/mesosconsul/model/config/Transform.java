package net.evilezh.mesosconsul.model.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Transform {
    public String type;
    public Map<String, Object> config;
    public boolean enabled;

    public void setConfig(String data) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        config = mapper.readValue(data, new TypeReference<HashMap<String, Object>>() {
        });
    }
}
