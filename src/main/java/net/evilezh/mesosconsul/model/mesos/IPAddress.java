package net.evilezh.mesosconsul.model.mesos;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IPAddress {
    @JsonProperty("ip_address")
    public String ipAddress;
}
