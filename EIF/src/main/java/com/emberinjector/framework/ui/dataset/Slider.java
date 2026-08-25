package com.emberinjector.framework.ui.dataset;

public interface Slider {
    void setValue(float value);
    float getValue();
    float getMin();
    float getMax();
    float getIncrement();
    String getName();
    String getDisplayValue();
}
