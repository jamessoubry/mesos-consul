package net.evilezh.mesosconsul.model.config;


import java.util.List;

public class RegexTransform extends net.evilezh.mesosconsul.model.config.Transform {
    public Expression expression;
    public List<Target> target;

    @Override
    public Class<? extends net.evilezh.mesosconsul.transform.Transform> getImplementation() {
        return net.evilezh.mesosconsul.transform.Regex.class;
    }
}
