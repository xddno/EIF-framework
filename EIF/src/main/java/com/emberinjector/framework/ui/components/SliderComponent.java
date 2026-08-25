package com.emberinjector.framework.ui.components;

import com.emberinjector.framework.ui.Component;
import com.emberinjector.framework.ui.dataset.Slider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SliderComponent implements Component {
    private final Slider slider;
    private final ModuleComponent module;
    private int offsetY;
    private int x;
    private int y;
    private boolean dragging;
    private double percent;

    public SliderComponent(Slider slider, ModuleComponent parentModule, int offsetY) {
        this.slider = slider;
        this.module = parentModule;
        this.x = parentModule.category.getX() + parentModule.category.getWidth();
        this.y = parentModule.category.getY() + parentModule.offsetY;
        this.offsetY = offsetY;
        float range = slider.getMax() - slider.getMin();
        if (range > 0) {
            this.percent = (slider.getValue() - slider.getMin()) / range;
        }
    }

    @Override
    public void draw(AtomicInteger offset) {
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);

        String display = this.slider.getName().replace("-", " ") + ": " + this.slider.getDisplayValue();
        Minecraft.getMinecraft().fontRendererObj.drawString(
                display,
                (this.module.category.getX() + 4) * 2,
                (this.module.category.getY() + this.offsetY + 2) * 2,
                -1, false
        );

        sliderDraw((this.module.category.getX() + 4) * 2,
                (this.module.category.getY() + this.offsetY + 10) * 2,
                (this.module.category.getWidth() - 8) * 2,
                this.percent);

        GL11.glPopMatrix();
    }

    private void sliderDraw(int x, int y, int width, double percent) {
        int filledWidth = (int) (width * percent);
        Gui.drawRect(x, y, x + width, y + 4, new Color(0, 0, 0, 150).getRGB());
        Gui.drawRect(x, y, x + filledWidth, y + 4, new Color(60, 162, 253, 200).getRGB());
        Gui.drawRect(x + filledWidth - 1, y - 1, x + filledWidth + 1, y + 5, Color.WHITE.getRGB());
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return 18;
    }

    @Override
    public void update(int mousePosX, int mousePosY) {
        this.y = this.module.category.getY() + this.offsetY;
        this.x = this.module.category.getX();
        if (this.dragging) {
            double xPos = mousePosX;
            double sliderWidth = this.module.category.getWidth() - 8;
            double newPercent = (xPos - (this.x + 4)) / sliderWidth;
            newPercent = Math.max(0, Math.min(1, newPercent));
            this.percent = newPercent;
            float range = this.slider.getMax() - this.slider.getMin();
            float increment = this.slider.getIncrement();
            if (increment > 0) {
                float value = (float) (this.slider.getMin() + range * newPercent);
                value = Math.round(value / increment) * increment;
                value = Math.max(this.slider.getMin(), Math.min(this.slider.getMax(), value));
                this.slider.setValue(value);
                float actualPercent = (value - this.slider.getMin()) / range;
                this.percent = actualPercent;
            }
        }
    }

    @Override
    public void mouseDown(int x, int y, int button) {
        if (this.isHovered(x, y) && button == 0 && this.module.panelExpand) {
            this.dragging = true;
        }
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
        if (button == 0) {
            this.dragging = false;
        }
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
    }

    public boolean isHovered(int x, int y) {
        return x > this.x && x < this.x + this.module.category.getWidth()
                && y > this.y && y < this.y + this.getHeight();
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}
