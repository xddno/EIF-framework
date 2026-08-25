package com.emberinjector.framework.ui.dataset.impl;

import com.emberinjector.framework.property.properties.FloatProperty;
import com.emberinjector.framework.ui.dataset.Slider;

public class FloatSlider implements Slider {
    private final FloatProperty property;

    public FloatSlider(FloatProperty property) {
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
        return 0.1f;
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
