package com.emberinjector.framework.property.properties;

import com.google.gson.JsonObject;
import com.emberinjector.framework.property.Property;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class IntProperty extends Property<Integer> {
    private final int min;
    private final int max;

    public IntProperty(String name, int value, int min, int max) {
        super(name, value, (Predicate<Integer>) val -> val >= min && val <= max, (BooleanSupplier) null);
        this.min = min;
        this.max = max;
    }

    public IntProperty(String name, int value, int min, int max, BooleanSupplier visibleChecker) {
        super(name, value, (Predicate<Integer>) val -> val >= min && val <= max, visibleChecker);
        this.min = min;
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    @Override
    public String formatValue() {
        return String.valueOf(getValue());
    }

    @Override
    public String getValuePrompt() {
        return "0";
    }

    @Override
    public boolean parseString(String string) {
        try {
            setValue(Integer.parseInt(string));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        try {
            setValue(jsonObject.get("value").getAsInt());
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
