package net.evilezh.mesosconsul.model.consul;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Node {
    @JsonProperty("Node")
    public String id;
    @JsonProperty("Address")
    public String address;
    @JsonProperty("TaggedAddresses")
    public TaggedAddresses taggedAddresses;
    @JsonProperty("CreateIndex")
    public long createIndex;
    @JsonProperty("ModifyIndex")
    public long modifyIndex;
}
