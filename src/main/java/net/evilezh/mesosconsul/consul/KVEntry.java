package net.evilezh.mesosconsul.consul;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KVEntry {
    @JsonProperty("KV")
    public KVData kv;
}