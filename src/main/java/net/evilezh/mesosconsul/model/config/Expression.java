package net.evilezh.mesosconsul.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Expression {
    public String operator;
/*
    @JsonProperty("ignore-missing")
    public boolean ignoreMissing = true;
*/
    public List<Match> match;

    public void setOperator(String data) {
        if (data.toLowerCase().equals("or") || data.toLowerCase().equals("and")) {
            this.operator = data.toLowerCase();
        } else {
            throw new ConfigTestFailedException("No such Expression operator: " + data + ". Choose one of following: and,or.");
        }
    }
}
