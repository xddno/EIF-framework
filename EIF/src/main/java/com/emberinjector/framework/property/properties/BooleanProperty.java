package com.emberinjector.framework.property.properties;

import com.google.gson.JsonObject;
import com.emberinjector.framework.property.Property;

import java.util.function.BooleanSupplier;

public class BooleanProperty extends Property<Boolean> {
    public BooleanProperty(String name, boolean value) {
        super(name, value, (BooleanSupplier) null);
    }

    public BooleanProperty(String name, boolean value, BooleanSupplier visibleChecker) {
        super(name, value, visibleChecker);
    }

    @Override
    public String formatValue() {
        return getValue() ? "ON" : "OFF";
    }

    @Override
    public String getValuePrompt() {
        return "true/false";
    }

    @Override
    public boolean parseString(String string) {
        if (string.equalsIgnoreCase("true")) {
            setValue(true);
            return true;
        }
        if (string.equalsIgnoreCase("false")) {
            setValue(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        try {
            setValue(jsonObject.get("value").getAsBoolean());
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
