package net.evilezh.mesosconsul.model.mesos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class NetworkInfos {
    @JsonProperty("ip_addresses")
    List<IPAddress> ipAddresses;
}
