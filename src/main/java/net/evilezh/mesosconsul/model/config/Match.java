package net.evilezh.mesosconsul.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.regex.Pattern;


public class Match {
    public String name;
    @JsonProperty("path")
    public String jsonPath;
    public Pattern regex;

    public void setRegex(String data) {
        regex = Pattern.compile(data);
    }
}
