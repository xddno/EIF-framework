package com.emberinjector.framework.property.properties;

import com.google.gson.JsonObject;
import com.emberinjector.framework.property.Property;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class FloatProperty extends Property<Float> {
    private final float min;
    private final float max;
    private final int decimalPlaces;

    public FloatProperty(String name, float value, float min, float max) {
        this(name, value, min, max, 1);
    }

    public FloatProperty(String name, float value, float min, float max, int decimalPlaces) {
        super(name, value, (Predicate<Float>) val -> val >= min && val <= max, (BooleanSupplier) null);
        this.min = min;
        this.max = max;
        this.decimalPlaces = decimalPlaces;
    }

    public FloatProperty(String name, float value, float min, float max, BooleanSupplier visibleChecker) {
        this(name, value, min, max, 1, visibleChecker);
    }

    public FloatProperty(String name, float value, float min, float max, int decimalPlaces, BooleanSupplier visibleChecker) {
        super(name, value, (Predicate<Float>) val -> val >= min && val <= max, visibleChecker);
        this.min = min;
        this.max = max;
        this.decimalPlaces = decimalPlaces;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    @Override
    public String formatValue() {
        return String.format("%." + decimalPlaces + "f", getValue());
    }

    @Override
    public String getValuePrompt() {
        return "0.0";
    }

    @Override
    public boolean parseString(String string) {
        try {
            setValue(Float.parseFloat(string));
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
