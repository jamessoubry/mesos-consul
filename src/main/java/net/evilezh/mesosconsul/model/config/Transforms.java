package net.evilezh.mesosconsul.model.config;

/*
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
*/

import java.util.Map;

/*
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RegexTransform.class, name = "regex")
})*/

public class Transforms {
    //name, configuration
    public Map<String, Transform> transforms;
}

