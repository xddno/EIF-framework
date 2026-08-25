package com.emberinjector.framework.property.properties;

import com.google.gson.JsonObject;
import com.emberinjector.framework.property.Property;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class PercentProperty extends Property<Float> {
    private final float min;
    private final float max;

    public PercentProperty(String name, float value) {
        this(name, value, 0f, 100f);
    }

    public PercentProperty(String name, float value, float min, float max) {
        super(name, value, (Predicate<Float>) val -> val >= min && val <= max, (BooleanSupplier) null);
        this.min = min;
        this.max = max;
    }

    public PercentProperty(String name, float value, float min, float max, BooleanSupplier visibleChecker) {
        super(name, value, (Predicate<Float>) val -> val >= min && val <= max, visibleChecker);
        this.min = min;
        this.max = max;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    @Override
    public String formatValue() {
        return String.format("%.0f%%", getValue());
    }

    @Override
    public String getValuePrompt() {
        return "0-100";
    }

    @Override
    public boolean parseString(String string) {
        try {
            float val = Float.parseFloat(string.replace("%", ""));
            setValue(val);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        try {
            setValue(jsonObject.get("value").getAsFloat());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty("value", getValue());
    }
}
