package com.emberinjector.framework.ui.components;

import com.emberinjector.framework.module.Module;
import com.emberinjector.framework.ui.Component;
import com.emberinjector.framework.ui.dataset.BindStage;
import com.emberinjector.framework.util.KeyBindUtil;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.atomic.AtomicInteger;

public class BindComponent implements Component {
    private boolean isBinding;
    private final ModuleComponent parentModule;
    private int offsetY;
    private int x;
    private int y;

    public BindComponent(ModuleComponent b, int offsetY) {
        this.parentModule = b;
        this.x = b.category.getX() + b.category.getWidth();
        this.y = b.category.getY() + b.offsetY;
        this.offsetY = offsetY;
    }

    @Override
    public void draw(AtomicInteger offset) {
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        String displayText = this.isBinding
                ? BindStage.binding
                : BindStage.bind + ": " + KeyBindUtil.getKeyName(this.parentModule.mod.getKey());
        this.renderText(displayText, 0x3CA2FD);
        GL11.glPopMatrix();
    }

    @Override
    public void update(int mousePosX, int mousePosY) {
        this.y = this.parentModule.category.getY() + this.offsetY;
        this.x = this.parentModule.category.getX();
    }

    @Override
    public void mouseDown(int x, int y, int button) {
        if (this.isHovered(x, y) && button == 0 && this.parentModule.panelExpand) {
            this.isBinding = !this.isBinding;
        } else if (this.isBinding && this.parentModule.panelExpand) {
            int keyIndex = button - 100;
            if (button == 0) {
                this.isBinding = false;
                return;
            }
            this.parentModule.mod.setKey(keyIndex);
            this.isBinding = false;
        }
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
        if (this.isBinding) {
            if (keyCode == 1) {
                this.isBinding = false;
                return;
            }
            int newKey = (keyCode == 11) ? 54 : keyCode;
            this.parentModule.mod.setKey(newKey);
            this.isBinding = false;
        }
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    public boolean isHovered(int x, int y) {
        return x > this.x && x < this.x + this.parentModule.category.getWidth()
                && y > this.y - 1 && y < this.y + 12;
    }

    @Override
    public int getHeight() {
        return 12;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    private void renderText(String s, int color) {
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
                s,
                (this.parentModule.category.getX() + 4) * 2f,
                (this.parentModule.category.getY() + this.offsetY + 3) * 2f,
                color
        );
    }
}
