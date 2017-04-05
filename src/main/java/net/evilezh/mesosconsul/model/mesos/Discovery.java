package net.evilezh.mesosconsul.model.mesos;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Discovery {
    public String name;
    public String visibility;
    public List<Port> ports;

    public void setPorts(Map<String, List<Map<String, Object>>> data) {
        ports = data.get("ports").stream().map(Port::new).collect(Collectors.toList());
    }
}
