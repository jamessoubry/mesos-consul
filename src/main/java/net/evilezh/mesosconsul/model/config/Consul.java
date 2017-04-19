package net.evilezh.mesosconsul.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public class Consul {
    public String address;
    public String path;
    public Optional<String> token = Optional.empty();
    public long retryInterval = 5000;
    public void setToken(String data) {
        token = Optional.of(data);
    }
}
