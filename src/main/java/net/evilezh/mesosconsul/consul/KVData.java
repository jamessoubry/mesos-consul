package net.evilezh.mesosconsul.consul;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Base64;

@JsonIgnoreProperties(value = {"LockIndex", "Flags", "CreateIndex", "ModifyIndex"})
public class KVData {
    @JsonProperty("Key")
    public String key;
    @JsonProperty("Value")
    public String value;

    public void setValue(String value) {
        this.value = new String(Base64.getDecoder().decode(value.getBytes()));
    }
}