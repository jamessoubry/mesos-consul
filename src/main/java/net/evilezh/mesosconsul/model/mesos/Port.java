package net.evilezh.mesosconsul.model.mesos;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Port {
    Map<String, String> labels;
    public int number;
    public String protocol;
    public String name;

    public Port(Map<String, Object> data) {
        this.number = (int) data.get("number");
        this.protocol = (String) data.get("protocol");
        this.name = data.containsKey("name") ? (String) data.get("name") : null;

        setLabels((Map<String, List<Map<String, String>>>) data.get("labels"));
    }

    public Port() {
    }


    public void setLabels(Map<String, List<Map<String, String>>> data) {
        if (data != null && data.size() > 0)
            labels = data.get("labels").stream().collect(Collectors.toMap(it -> it.get("key"), it -> it.get("value")));
    }
}
