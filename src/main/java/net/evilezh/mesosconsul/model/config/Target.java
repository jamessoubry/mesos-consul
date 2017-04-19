package net.evilezh.mesosconsul.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Target {
    //this will insert service without actual port
    @JsonProperty("eval-without-port")
    public boolean evalWithoutPorts;
    @JsonProperty("port")
    public String port;
    public String task;
}
