package net.evilezh.mesosconsul.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public class Consul {
    public String address;
    public Optional<String> token = Optional.empty();
    @JsonProperty("service-prefix")
    public String servicePrefix;

    public void setToken(String data) {
        token = Optional.of(data);
    }
}
