package com.emberinjector.framework.property.properties;

import com.google.gson.JsonObject;
import com.emberinjector.framework.property.Property;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

public class ModeProperty extends Property<String> {
    private final List<String> modes;
    private int index;

    public ModeProperty(String name, String value, String... modes) {
        this(name, value, null, modes);
    }

    public ModeProperty(String name, String value, BooleanSupplier visibleChecker, String... modes) {
        super(name, value, visibleChecker);
        this.modes = Arrays.asList(modes);
        this.index = this.modes.indexOf(value);
        if (this.index == -1) {
            this.index = 0;
        }
    }

    public List<String> getModes() {
        return modes;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String formatValue() {
        return getValue();
    }

    @Override
    public String getValuePrompt() {
        return "mode";
    }

    @Override
    public boolean parseString(String string) {
        for (int i = 0; i < modes.size(); i++) {
            if (modes.get(i).equalsIgnoreCase(string)) {
                index = i;
                setValue(modes.get(i));
                return true;
            }
        }
        return false;
    }

    public void cycle(boolean forward) {
        if (forward) {
            index = (index + 1) % modes.size();
        } else {
            index = (index - 1 + modes.size()) % modes.size();
        }
        setValue(modes.get(index));
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        try {
            String val = jsonObject.get("value").getAsString();
            for (int i = 0; i < modes.size(); i++) {
                if (modes.get(i).equals(val)) {
                    index = i;
                    setValue(val);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty("value", getValue());
    }
}
