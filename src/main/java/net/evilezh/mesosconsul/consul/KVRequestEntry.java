package net.evilezh.mesosconsul.consul;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KVRequestEntry {
    @JsonProperty("KV")
    public KVRequest kv;
}
