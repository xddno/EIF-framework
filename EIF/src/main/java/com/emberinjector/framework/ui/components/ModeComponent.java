package com.emberinjector.framework.ui.components;

import com.emberinjector.framework.property.properties.ModeProperty;
import com.emberinjector.framework.ui.Component;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.atomic.AtomicInteger;

public class ModeComponent implements Component {
    private final ModeProperty property;
    private final ModuleComponent module;
    private int offsetY;
    private int x;
    private int y;

    public ModeComponent(ModeProperty property, ModuleComponent parentModule, int offsetY) {
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
        String text = this.property.getName().replace("-", " ") + ": " + this.property.formatValue();
        Minecraft.getMinecraft().fontRendererObj.drawString(
                text,
                (this.module.category.getX() + 4) * 2,
                (this.module.category.getY() + this.offsetY + 5) * 2,
                -1, false
        );
        GL11.glPopMatrix();
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    @Override
    public void update(int mousePosX, int mousePosY) {
        this.y = this.module.category.getY() + this.offsetY;
        this.x = this.module.category.getX();
    }

    @Override
    public void mouseDown(int x, int y, int button) {
        if (this.isHovered(x, y) && this.module.panelExpand) {
            if (button == 0) {
                this.property.cycle(true);
            } else if (button == 1) {
                this.property.cycle(false);
            }
        }
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
    }

    public boolean isHovered(int x, int y) {
        return x > this.x && x < this.x + this.module.category.getWidth()
                && y > this.y && y < this.y + 12;
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }
}
