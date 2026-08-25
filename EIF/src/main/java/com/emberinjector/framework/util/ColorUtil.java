package com.emberinjector.framework.util;

import java.awt.*;

public class ColorUtil {
    public static final Color RED = new Color(255, 0, 0);
    public static final Color GOLD = new Color(255, 165, 0);
    public static final Color YELLOW = new Color(255, 255, 0);
    public static final Color GREEN = new Color(0, 255, 0);

    public static Color fromHSB(float hue, float saturation, float brightness) {
        return new Color(Color.HSBtoRGB(hue, saturation, brightness));
    }

    public static Color interpolate(float progress, Color startColor, Color endColor) {
        progress = Math.min(Math.max(progress, 0.0f), 1.0f);
        return new Color(
                (int) (startColor.getRed() + progress * (endColor.getRed() - startColor.getRed())),
                (int) (startColor.getGreen() + progress * (endColor.getGreen() - startColor.getGreen())),
                (int) (startColor.getBlue() + progress * (endColor.getBlue() - startColor.getBlue()))
        );
    }

    public static Color getHealthBlend(float percent) {
        if (percent >= 0.9f) return GREEN;
        if (percent >= 0.55f) return interpolate((percent - 0.55f) / 0.35f, YELLOW, GREEN);
        if (percent >= 0.45f) return YELLOW;
        if (percent >= 0.1f) return interpolate((percent - 0.1f) / 0.35f, RED, YELLOW);
        return RED;
    }

    public static Color darker(Color color, float factor) {
        return scale(color, factor, color.getAlpha());
    }

    public static Color scale(Color color, float scaleFactor, int alpha) {
        return new Color(
                Math.min(Math.max((int) (color.getRed() * scaleFactor), 0), 255),
                Math.min(Math.max((int) (color.getGreen() * scaleFactor), 0), 255),
                Math.min(Math.max((int) (color.getBlue() * scaleFactor), 0), 255),
                alpha
        );
    }

    public static Color rainbow(long time, int offset, float saturation, float brightness) {
        float hue = (float) ((time + offset * 200L) % 4000L) / 4000f;
        return new Color(Color.HSBtoRGB(hue, saturation, brightness));
    }

    public static Color rainbow(long time, int offset) {
        return rainbow(time, offset, 0.7f, 1.0f);
    }
}
