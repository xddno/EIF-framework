package com.emberinjector.framework.ui.components;

import com.emberinjector.framework.property.properties.TextProperty;
import com.emberinjector.framework.ui.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.atomic.AtomicInteger;

public class TextComponent implements Component {
    private final TextProperty property;
    private final ModuleComponent module;
    private final GuiTextField textField;
    private int offsetY;
    private int x;
    private int y;

    public TextComponent(TextProperty property, ModuleComponent parentModule, int offsetY) {
        this.property = property;
        this.module = parentModule;
        this.offsetY = offsetY;
        this.textField = new GuiTextField(0, Minecraft.getMinecraft().fontRendererObj,
                parentModule.category.getX() + 4,
                parentModule.category.getY() + offsetY + 2,
                parentModule.category.getWidth() - 8,
                12);
        this.textField.setText(property.getValue());
        this.x = parentModule.category.getX() + parentModule.category.getWidth();
        this.y = parentModule.category.getY() + parentModule.offsetY;
    }

    @Override
    public void draw(AtomicInteger offset) {
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        textField.drawTextBox();
        GL11.glPopMatrix();
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
        textField.yPosition = this.module.category.getY() + this.offsetY + 2;
    }

    @Override
    public int getHeight() {
        return 16;
    }

    @Override
    public void update(int mousePosX, int mousePosY) {
        this.y = this.module.category.getY() + this.offsetY;
        this.x = this.module.category.getX();
        textField.xPosition = this.module.category.getX() + 4;
        textField.yPosition = this.module.category.getY() + this.offsetY + 2;
    }

    @Override
    public void mouseDown(int x, int y, int button) {
        textField.mouseClicked(x, y, button);
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
        if (textField.isFocused()) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
                textField.setFocused(false);
                property.setValue(textField.getText());
            } else {
                textField.textboxKeyTyped(chatTyped, keyCode);
                property.setValue(textField.getText());
            }
        }
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }
}
