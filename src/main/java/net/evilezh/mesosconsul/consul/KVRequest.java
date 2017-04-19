package net.evilezh.mesosconsul.consul;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Base64;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class KVRequest {
    public String verb;
    public String key;
    public String value;

    public void setValue(String value) {
        this.value = new String(Base64.getEncoder().encode(value.getBytes()));
    }
}
