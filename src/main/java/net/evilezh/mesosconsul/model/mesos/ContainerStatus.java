package net.evilezh.mesosconsul.model.mesos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ContainerStatus {
    @JsonProperty("network_infos")
    public List<NetworkInfos> networkInfos;
    @JsonProperty("executor_pid")
    public int executorPid;
}
