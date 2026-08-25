package com.emberinjector.framework.ui.dataset.impl;

import com.emberinjector.framework.property.properties.PercentProperty;
import com.emberinjector.framework.ui.dataset.Slider;

public class PercentageSlider implements Slider {
    private final PercentProperty property;

    public PercentageSlider(PercentProperty property) {
        this.property = property;
    }

    @Override
    public void setValue(float value) {
        property.setValue(value);
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
