package com.emberinjector.framework.ui.components;

import com.emberinjector.framework.property.properties.ColorProperty;
import com.emberinjector.framework.ui.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ColorSliderComponent implements Component {
    private final ColorProperty property;
    private final ModuleComponent module;
    private int offsetY;
    private int x;
    private int y;
    private int dragging;

    public ColorSliderComponent(ColorProperty property, ModuleComponent parentModule, int offsetY) {
        this.property = property;
        this.module = parentModule;
        this.x = parentModule.category.getX() + parentModule.category.getWidth();
        this.y = parentModule.category.getY() + parentModule.offsetY;
        this.offsetY = offsetY;
    }

    @Override
    public void draw(AtomicInteger offset) {
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);

        int baseX = (this.module.category.getX() + 4) * 2;
        int baseY = (this.module.category.getY() + this.offsetY + 2) * 2;
        int sliderWidth = (this.module.category.getWidth() - 8) * 2;

        Minecraft.getMinecraft().fontRendererObj.drawString(
                this.property.getName().replace("-", " "),
                baseX, baseY, -1, false
        );

        int sliderY = baseY + 10;
        int sliderHeight = 6;

        drawHueSlider(baseX, sliderY, sliderWidth, sliderHeight);

        int hueX = baseX + (int) (this.property.getHue() * sliderWidth);
        Gui.drawRect(hueX - 1, sliderY - 1, hueX + 1, sliderY + sliderHeight + 1, Color.WHITE.getRGB());

        GL11.glPopMatrix();
    }

    private void drawHueSlider(int x, int y, int width, int height) {
        for (int i = 0; i < width; i++) {
            float hue = (float) i / width;
            int color = Color.HSBtoRGB(hue, 1.0f, 1.0f);
            Gui.drawRect(x + i, y, x + i + 1, y + height, color);
        }
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return 22;
    }

    @Override
    public void update(int mousePosX, int mousePosY) {
        this.y = this.module.category.getY() + this.offsetY;
        this.x = this.module.category.getX();
        if (dragging != 0 && this.module.panelExpand) {
            int baseX = (this.module.category.getX() + 4) * 2;
            int sliderWidth = (this.module.category.getWidth() - 8) * 2;
            double relX = (mousePosX * 2) - baseX;
            if (dragging == 1) {
                float hue = (float) (relX / sliderWidth);
                hue = Math.max(0, Math.min(1, hue));
                property.setHue(hue);
            }
        }
    }

    @Override
    public void mouseDown(int x, int y, int button) {
        if (this.isHovered(x, y) && this.module.panelExpand) {
            int sliderYStart = this.y + 12;
            if (y >= sliderYStart && y <= sliderYStart + 11) {
                this.dragging = 1;
            }
        }
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
        this.dragging = 0;
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
        return property.isVisible();
    }
}
