package net.evilezh.mesosconsul.model.mesos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Discovery {
    public String name;
    public String visibility;
    public List<Port> ports;

    public void setPorts(Map<String, Object> data) {
        if (data.get("ports") instanceof List) {
            List<Map<String, Object>> datac = (List<Map<String, Object>>) data.get("ports");
            ports = datac.stream().map(Port::new).collect(Collectors.toList());
        } else {
            ports = new ArrayList<>();
        }
    }
}
