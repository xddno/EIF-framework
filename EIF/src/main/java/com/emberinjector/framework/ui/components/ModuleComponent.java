package com.emberinjector.framework.ui.components;

import com.emberinjector.framework.module.Module;
import com.emberinjector.framework.property.Property;
import com.emberinjector.framework.property.PropertyManager;
import com.emberinjector.framework.property.properties.*;
import com.emberinjector.framework.ui.Component;
import com.emberinjector.framework.ui.dataset.impl.FloatSlider;
import com.emberinjector.framework.ui.dataset.impl.IntSlider;
import com.emberinjector.framework.ui.dataset.impl.PercentageSlider;
import com.emberinjector.framework.util.ColorUtil;
import net.minecraft.client.Minecraft;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ModuleComponent implements Component {
    public Module mod;
    public CategoryComponent category;
    public int offsetY;
    private final ArrayList<Component> settings;
    public boolean panelExpand;

    public ModuleComponent(Module mod, CategoryComponent category, int offsetY) {
        this.mod = mod;
        this.category = category;
        this.offsetY = offsetY;
        this.settings = new ArrayList<>();
        this.panelExpand = false;
    }

    public void initSettings(PropertyManager propertyManager) {
        int y = offsetY + 12;
        if (propertyManager.properties.containsKey(mod.getClass())) {
            for (Property<?> baseProperty : propertyManager.properties.get(mod.getClass())) {
                if (baseProperty instanceof BooleanProperty) {
                    CheckBoxComponent c = new CheckBoxComponent((BooleanProperty) baseProperty, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof FloatProperty) {
                    SliderComponent c = new SliderComponent(new FloatSlider((FloatProperty) baseProperty), this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof IntProperty) {
                    SliderComponent c = new SliderComponent(new IntSlider((IntProperty) baseProperty), this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof PercentProperty) {
                    SliderComponent c = new SliderComponent(new PercentageSlider((PercentProperty) baseProperty), this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof ModeProperty) {
                    ModeComponent c = new ModeComponent((ModeProperty) baseProperty, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof ColorProperty) {
                    ColorSliderComponent c = new ColorSliderComponent((ColorProperty) baseProperty, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof TextProperty) {
                    TextComponent c = new TextComponent((TextProperty) baseProperty, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                }
            }
        }
        this.settings.add(new BindComponent(this, y));
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
        int y = this.offsetY + 16;
        for (Component c : this.settings) {
            c.setComponentStartAt(y);
            if (c.isVisible()) {
                y += c.getHeight();
            }
        }
    }

    @Override
    public void draw(AtomicInteger offset) {
        int textColor;
        if (this.mod.isEnabled()) {
            textColor = ColorUtil.rainbow(System.currentTimeMillis(), offset.get()).getRGB();
        } else {
            textColor = new Color(102, 102, 102).getRGB();
        }
        int centerX = this.category.getX() + this.category.getWidth() / 2;
        int textWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(this.mod.getName());
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
                this.mod.getName(),
                centerX - textWidth / 2f,
                this.category.getY() + this.offsetY + 4,
                textColor
        );
        if (this.panelExpand && !this.settings.isEmpty()) {
            for (Component c : this.settings) {
                if (c.isVisible()) {
                    c.draw(offset);
                    offset.incrementAndGet();
                }
            }
        }
    }

    @Override
    public int getHeight() {
        if (!this.panelExpand) {
            return 16;
        }
        int h = 16;
        for (Component c : this.settings) {
            if (c.isVisible()) {
                h += c.getHeight();
            }
        }
        return h;
    }

    @Override
    public void update(int mousePosX, int mousePosY) {
        if (!panelExpand) return;
        for (Component c : this.settings) {
            if (c.isVisible()) {
                c.update(mousePosX, mousePosY);
            }
        }
    }

    @Override
    public void mouseDown(int x, int y, int button) {
        if (this.isHovered(x, y) && button == 0) {
            this.mod.toggle();
        }
        if (this.isHovered(x, y) && button == 1) {
            this.panelExpand = !this.panelExpand;
        }
        if (!panelExpand) return;
        for (Component c : this.settings) {
            if (c.isVisible()) {
                c.mouseDown(x, y, button);
            }
        }
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
        if (!panelExpand) return;
        for (Component c : this.settings) {
            if (c.isVisible()) {
                c.mouseReleased(x, y, button);
            }
        }
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
        if (!panelExpand) return;
        for (Component c : this.settings) {
            if (c.isVisible()) {
                c.keyTyped(chatTyped, keyCode);
            }
        }
    }

    public boolean isHovered(int x, int y) {
        return x > this.category.getX()
                && x < this.category.getX() + this.category.getWidth()
                && y > this.category.getY() + this.offsetY
                && y < this.category.getY() + 16 + this.offsetY;
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}
