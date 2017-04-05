package net.evilezh.mesosconsul.model.mesos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ScalarResource.class, name = "SCALAR"),
        @JsonSubTypes.Type(value = RangeResource.class, name = "RANGES")
})

public class Resource {
    public String name;
    public String role;
    public String type;
}
