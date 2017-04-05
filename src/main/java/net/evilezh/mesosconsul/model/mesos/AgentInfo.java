package net.evilezh.mesosconsul.model.mesos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonIgnoreProperties(value = {"resources"})
public class AgentInfo {
    public Map<String, Object> attributes;
    public String hostname;
    public String id;
    public int port;

    public void setId(Map<String, String> data) {
        id = data.get("value");
    }

    public void setAttributes(List<Map<String, Object>> data) {
        attributes = data.stream().collect(Collectors.toMap(it -> (String) it.get("name"), it ->
                it.get(((String) it.get("type")).toLowerCase())
        ));
    }
}
