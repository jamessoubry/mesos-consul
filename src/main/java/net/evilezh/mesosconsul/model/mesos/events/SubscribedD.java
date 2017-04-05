package net.evilezh.mesosconsul.model.mesos.events;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class SubscribedD extends Event {
    @JsonProperty("subscribed")
    public Subscribed subscribed;
}
