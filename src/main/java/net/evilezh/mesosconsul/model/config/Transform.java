package net.evilezh.mesosconsul.model.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RegexTransform.class, name = "regex")
})
public abstract class Transform {
    public abstract Class<? extends net.evilezh.mesosconsul.transform.Transform> getImplementation();
}

