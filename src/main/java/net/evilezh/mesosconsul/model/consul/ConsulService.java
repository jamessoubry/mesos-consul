package net.evilezh.mesosconsul.model.consul;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
@JsonIgnoreProperties(value = {"CreateIndex", "ModifyIndex", "EnableTagOverride"})
public class ConsulService {
    @JsonProperty("ID")
    public String id;

    @JsonProperty("Service")
    public String name;

    @JsonProperty("Tags")
    public List<String> tags;

    @JsonProperty("Address")
    public String address;

    @JsonProperty("Port")
    public int port;

/*
    @JsonProperty("EnableTagOverride")
    public boolean enableTagOverride;
*/

    @JsonIgnore
    public boolean synced = false;

    @JsonIgnore
    public boolean isManaged() {
        return id.startsWith("mconsul:");
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ConsulService))
            return false;
        ConsulService s = (ConsulService) obj;
        return this.id.equals(s.id) && this.address.equals(s.address) && this.port == s.port && this.tags.containsAll(s.tags) && s.tags.containsAll(this.tags); /*&&
                this.name.equals(s.name) &&
                this.tags.equals(s.tags) &&
                this.address.equals(s.address) &&
                this.port == s.port &&
                this.enableTagOverride == s.enableTagOverride;*/
    }
}
