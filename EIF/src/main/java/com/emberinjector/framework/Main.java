// Copyright (c) Lefraudeur. All rights reserved.
// Copyright (c) achul123. All rights reserved.
// Original work: https://github.com/Lefraudeur/MujinaBaseV2
// Fork:         https://github.com/achul123/MujinaBaseV2

package com.emberinjector.framework;

import com.emberinjector.framework.client.EventDelegateImpl;
import com.emberinjector.framework.client.EventDispatcher;
import com.emberinjector.framework.module.Module;
import com.emberinjector.framework.module.ModuleManager;
import com.emberinjector.framework.module.GuiModule;
import com.emberinjector.framework.module.modules.KnockbackDelay;
import com.emberinjector.framework.property.PropertyManager;
import com.emberinjector.framework.ui.ClickGui;

public class Main {
    private static volatile boolean quitRequested = false;
    public static ModuleManager moduleManager;
    public static ClickGui clickGui;

    public static void onLoad() {
        moduleManager = new ModuleManager();
        moduleManager.modules.put(GuiModule.class, new GuiModule());
        moduleManager.modules.put(KnockbackDelay.class, new KnockbackDelay());

        PropertyManager propertyManager = new PropertyManager();
        for (Module module : moduleManager.modules.values()) {
            propertyManager.register(module);
        }

        clickGui = new ClickGui(moduleManager, propertyManager);
        clickGui.populateModules();

        EventDelegateImpl delegate = new EventDelegateImpl(moduleManager, propertyManager);
        EventDispatcher.setDelegate(delegate);

        System.out.println("[EIF] Injection successful!");
    }

    public static void onUnload() {
        System.out.println("[EIF] Unloading...");
    }

    public static void requestUnload() {
        quitRequested = true;
    }

    public static boolean isQuitRequested() {
        return quitRequested;
    }
}
