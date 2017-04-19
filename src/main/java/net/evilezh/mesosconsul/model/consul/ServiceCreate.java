package net.evilezh.mesosconsul.model.consul;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class ServiceCreate {
    @JsonProperty("ID")
    public String id = UUID.randomUUID().toString();

    @JsonProperty("Node")
    public String node;

    @JsonProperty("Address")
    public String address;

    @JsonProperty("Service")
    public RService service;

    public ServiceCreate(Node node, ConsulService service) {
        this.node = node.id;
        this.address = node.address;
        this.service = new RService(service);
    }

    private class RService {
        @JsonProperty("ID")
        public String id;

        @JsonProperty("Service")
        public String name;

        @JsonProperty("Tags")
        List<String> tags;

        @JsonProperty("Address")
        public String address;

        @JsonProperty("Port")
        public int port;

        RService(ConsulService service) {
            this.id = service.id;
            this.name = service.name;
            this.tags = service.tags;
            this.address = service.address;
            this.port = service.port;
        }
    }
}
