package com.semivanilla.announcer.animation.impl;

import com.semivanilla.announcer.animation.Animation;
import com.semivanilla.announcer.manager.ConfigManager;

public class GradientAnimation implements Animation {
    private static final float MIN = -1f, MAX = 1f;
    private float phase = -1f;

    @Override
    public String getValue() {
        //clamp phase between MIN and MAX
        phase = Math.max(MIN, Math.min(MAX, phase));
        return String.valueOf(phase);
    }

    @Override
    public String nextValue() {
        if (phase >= MAX) {
            phase = MIN;
        }
        phase += (float) ConfigManager.getGradientSpeed();
        return getValue();
    }
}
