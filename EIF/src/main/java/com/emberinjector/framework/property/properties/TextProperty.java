package com.emberinjector.framework.property.properties;

import com.google.gson.JsonObject;
import com.emberinjector.framework.property.Property;

import java.util.function.BooleanSupplier;

public class TextProperty extends Property<String> {

    public TextProperty(String name, String value) {
        super(name, value, (BooleanSupplier) null);
    }

    public TextProperty(String name, String value, BooleanSupplier visibleChecker) {
        super(name, value, visibleChecker);
    }

    @Override
    public String formatValue() {
        return getValue();
    }

    @Override
    public String getValuePrompt() {
        return "text";
    }

    @Override
    public boolean parseString(String string) {
        setValue(string);
        return true;
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        try {
            setValue(jsonObject.get("value").getAsString());
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
