package net.evilezh.mesosconsul.transform;

public abstract class AbstractTranform implements Transform {
    protected final net.evilezh.mesosconsul.model.config.Transform transform;

    public AbstractTranform(net.evilezh.mesosconsul.model.config.Transform transform) {
        this.transform = transform;
    }
}
