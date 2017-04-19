package net.evilezh.mesosconsul.model.consul;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class NodeEntry {
    @JsonProperty("Node")
    public Node node;
    @JsonProperty("Services")
    public Map<String, ConsulService> services;
}
