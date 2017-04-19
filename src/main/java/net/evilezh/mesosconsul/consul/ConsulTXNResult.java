package net.evilezh.mesosconsul.consul;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsulTXNResult {
    @JsonProperty("Results")
    public List<KVEntry> results;
}