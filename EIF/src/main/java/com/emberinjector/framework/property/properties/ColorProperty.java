package com.emberinjector.framework.property.properties;

import com.google.gson.JsonObject;
import com.emberinjector.framework.property.Property;

import java.awt.*;
import java.util.function.BooleanSupplier;

public class ColorProperty extends Property<Color> {
    private final float[] hsv = new float[3];

    public ColorProperty(String name, Color value) {
        this(name, value, null);
    }

    public ColorProperty(String name, Color value, BooleanSupplier visibleChecker) {
        super(name, value, visibleChecker);
        Color.RGBtoHSB(value.getRed(), value.getGreen(), value.getBlue(), hsv);
    }

    public float getHue() {
        return hsv[0];
    }

    public float getSaturation() {
        return hsv[1];
    }

    public float getBrightness() {
        return hsv[2];
    }

    public void setHue(float hue) {
        hsv[0] = hue;
        setValue(new Color(Color.HSBtoRGB(hue, hsv[1], hsv[2])));
    }

    public void setSaturation(float saturation) {
        hsv[1] = saturation;
        setValue(new Color(Color.HSBtoRGB(hsv[0], saturation, hsv[2])));
    }

    public void setBrightness(float brightness) {
        hsv[2] = brightness;
        setValue(new Color(Color.HSBtoRGB(hsv[0], hsv[1], brightness)));
    }

    @Override
    public String formatValue() {
        Color c = getValue();
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    @Override
    public String getValuePrompt() {
        return "#FFFFFF";
    }

    @Override
    public boolean parseString(String string) {
        try {
            setValue(Color.decode(string));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        try {
            int rgb = jsonObject.get("value").getAsInt();
            setValue(new Color(rgb));
            Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsv);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty("value", getValue().getRGB());
    }
}
