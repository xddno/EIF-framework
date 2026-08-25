package com.emberinjector.framework.module;

import com.emberinjector.framework.Main;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class GuiModule extends Module {
    public GuiModule() {
        super("ClickGui", false, Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnabled() {
        setEnabled(false);
        if (Main.clickGui != null) {
            Minecraft.getMinecraft().displayGuiScreen(Main.clickGui);
        }
    }
}
