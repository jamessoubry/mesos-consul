package net.evilezh.mesosconsul.model.mesos;

import java.util.Map;

public class ScalarResource extends Resource {
    public double scalar;

    public void setScalar(Map<String, Double> data) {
        scalar = data.get("value");
    }
}
