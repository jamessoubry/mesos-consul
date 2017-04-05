package net.evilezh.mesosconsul;


import net.evilezh.mesosconsul.model.config.*;
import org.eclipse.jetty.client.api.ContentResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static net.evilezh.mesosconsul.Main.client;

public class ConsulHelper {
    private final net.evilezh.mesosconsul.model.config.Consul config;

    public ConsulHelper(net.evilezh.mesosconsul.model.config.Consul config) {
        this.config = config;
    }

    public String getKey(String path) throws InterruptedException, ExecutionException, TimeoutException {
        return client.GET(config.address + "").getContentAsString();
    }
}
