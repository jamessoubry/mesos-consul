package net.evilezh.mesosconsul.model.mesos.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Subscribed {
    @JsonProperty("get_state")
    public GetState getState;
}
