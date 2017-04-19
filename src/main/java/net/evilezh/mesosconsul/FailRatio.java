package net.evilezh.mesosconsul;

import com.codahale.metrics.Meter;
import com.codahale.metrics.RatioGauge;
import com.codahale.metrics.Timer;

public class FailRatio extends RatioGauge {
    private final Meter fails;
    private final Meter calls;

    public FailRatio(Meter fails, Meter calls) {
        this.fails = fails;
        this.calls = calls;

    }

    @Override
    protected Ratio getRatio() {
        return Ratio.of(fails.getOneMinuteRate(), calls.getOneMinuteRate());
    }
}
