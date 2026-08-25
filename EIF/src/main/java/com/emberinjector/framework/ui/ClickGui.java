package com.emberinjector.framework.ui;

import com.emberinjector.framework.module.Module;
import com.emberinjector.framework.module.ModuleManager;
import com.emberinjector.framework.property.PropertyManager;
import com.emberinjector.framework.ui.components.CategoryComponent;
import com.emberinjector.framework.ui.components.ModuleComponent;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

public class ClickGui extends GuiScreen {
    private static ClickGui instance;
    private final ArrayList<CategoryComponent> categoryList;
    private final ModuleManager moduleManager;
    private final PropertyManager propertyManager;

    public ClickGui(ModuleManager moduleManager, PropertyManager propertyManager) {
        instance = this;
        this.moduleManager = moduleManager;
        this.propertyManager = propertyManager;

        categoryList = new ArrayList<>();
        int topOffset = 5;

        CategoryComponent combat = new CategoryComponent("Combat", new ArrayList<>());
        combat.setY(topOffset);
        categoryList.add(combat);
        topOffset += 20;

        CategoryComponent movement = new CategoryComponent("Movement", new ArrayList<>());
        movement.setY(topOffset);
        categoryList.add(movement);
        topOffset += 20;

        CategoryComponent render = new CategoryComponent("Render", new ArrayList<>());
        render.setY(topOffset);
        categoryList.add(render);
        topOffset += 20;

        CategoryComponent player = new CategoryComponent("Player", new ArrayList<>());
        player.setY(topOffset);
        categoryList.add(player);
        topOffset += 20;

        CategoryComponent misc = new CategoryComponent("Misc", new ArrayList<>());
        misc.setY(topOffset);
        categoryList.add(misc);
    }

    public void populateModules() {
        for (java.util.Map.Entry<Class<?>, Module> entry : moduleManager.modules.entrySet()) {
            Module module = entry.getValue();
            String catName = guessCategory(module);
            for (CategoryComponent cat : categoryList) {
                if (cat.getName().equalsIgnoreCase(catName)) {
                    ModuleComponent mc = new ModuleComponent(module, cat, 16);
                    mc.initSettings(propertyManager);
                    cat.modulesInCategory.add(mc);
                    break;
                }
            }
        }
        for (CategoryComponent cat : categoryList) {
            cat.modulesInCategory.sort(Comparator.comparing(m ->
                    ((ModuleComponent) m).mod.getName().toLowerCase()));
        }
    }

    private String guessCategory(Module module) {
        String name = module.getName().toLowerCase();
        if (name.contains("click")) return "Misc";
        if (name.contains("knockback")) return "Combat";
        return "Misc";
    }

    public static ClickGui getInstance() {
        return instance;
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 100).getRGB());

        if (mc.fontRendererObj != null) {
            mc.fontRendererObj.drawStringWithShadow("Ember Framework", 4,
                    this.height - 3 - mc.fontRendererObj.FONT_HEIGHT * 2,
                    new Color(60, 162, 253).getRGB());
            mc.fontRendererObj.drawStringWithShadow("v1.0", 4,
                    this.height - 3 - mc.fontRendererObj.FONT_HEIGHT,
                    new Color(60, 162, 253).getRGB());
        }

        for (CategoryComponent category : categoryList) {
            category.render(this.fontRendererObj);
            category.handleDrag(x, y);

            for (Component module : category.getModules()) {
                module.update(x, y);
            }
        }

        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            int scrollDir = wheel > 0 ? 1 : -1;
            for (CategoryComponent category : categoryList) {
                category.onScroll(x, y, scrollDir);
            }
        }
    }

    @Override
    public void mouseClicked(int x, int y, int mouseButton) {
        for (CategoryComponent category : categoryList) {
            if (category.insideArea(x, y) && !category.isHovered(x, y)
                    && !category.mousePressed(x, y) && mouseButton == 0) {
                category.mousePressed(true);
                category.xx = x - category.getX();
                category.yy = y - category.getY();
            }

            if (category.mousePressed(x, y) && mouseButton == 0) {
                category.setOpened(!category.isOpened());
            }

            if (category.isHovered(x, y) && mouseButton == 0) {
                category.setPin(!category.isPin());
            }

            if (category.isOpened()) {
                for (Component c : category.getModules()) {
                    c.mouseDown(x, y, mouseButton);
                }
            }
        }
    }

    @Override
    public void mouseReleased(int x, int y, int mouseButton) {
        for (CategoryComponent categoryComponent : categoryList) {
            if (mouseButton == 0) {
                categoryComponent.mousePressed(false);
            }
            if (categoryComponent.isOpened()) {
                for (Component component : categoryComponent.getModules()) {
                    component.mouseReleased(x, y, mouseButton);
                }
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int key) {
        if (key == 1) {
            this.mc.displayGuiScreen(null);
        } else {
            for (CategoryComponent cat : categoryList) {
                if (cat.isOpened()) {
                    for (Component component : cat.getModules()) {
                        component.keyTyped(typedChar, key);
                    }
                }
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
