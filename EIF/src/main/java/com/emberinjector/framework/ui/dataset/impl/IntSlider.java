package com.emberinjector.framework.ui.dataset.impl;

import com.emberinjector.framework.property.properties.IntProperty;
import com.emberinjector.framework.ui.dataset.Slider;

public class IntSlider implements Slider {
    private final IntProperty property;

    public IntSlider(IntProperty property) {
        this.property = property;
    }

    @Override
    public void setValue(float value) {
        property.setValue(Math.round(value));
    }

    @Override
    public float getValue() {
        return property.getValue();
    }

    @Override
    public float getMin() {
        return property.getMin();
    }

    @Override
    public float getMax() {
        return property.getMax();
    }

    @Override
    public float getIncrement() {
        return 1;
    }

    @Override
    public String getName() {
        return property.getName();
    }

    @Override
    public String getDisplayValue() {
        return property.formatValue();
    }
}
