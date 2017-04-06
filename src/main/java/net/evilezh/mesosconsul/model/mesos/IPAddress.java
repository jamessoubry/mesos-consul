package net.evilezh.mesosconsul.model.mesos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IPAddress {
    @JsonProperty("ip_address")
    public String ipAddress;
    public String protocol;
    @JsonProperty("labels")
    public Map<String, String> labels;

    public void setLabels(Map<String, List<Map<String, String>>> data) {
        labels = data.get("labels").stream().collect(Collectors.toMap(it -> it.get("key"), it -> it.get("value")));
    }
}
