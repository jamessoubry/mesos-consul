package net.evilezh.mesosconsul.transform;

import java.util.Map;

public abstract class AbstractTranform implements Transform {
    public final String name;
    public final net.evilezh.mesosconsul.model.config.Transform config;
    /*
        protected final Map<String, Object> config;
        public boolean active = false;

        public final String type;
    */


    public AbstractTranform(String name, net.evilezh.mesosconsul.model.config.Transform config) throws Throwable {
        this.name = name;
        this.config = config;
    }
}
