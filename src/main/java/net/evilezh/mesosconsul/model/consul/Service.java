package net.evilezh.mesosconsul.model.consul;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Service {
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
    @JsonProperty("EnableTagOverride")
    public boolean enableTagOverride;
    @JsonProperty("CreateIndex")
    public long createIndex;
    @JsonProperty("ModifyIndex")
    public long modifyIndex;


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Service))
            return false;
        Service s = (Service) obj;
        return this.id.equals(s.id); /*&&
                this.name.equals(s.name) &&
                this.tags.equals(s.tags) &&
                this.address.equals(s.address) &&
                this.port == s.port &&
                this.enableTagOverride == s.enableTagOverride;*/
    }
}
