package com.emberinjector.framework.module;

import java.util.LinkedHashMap;

public class ModuleManager {
    public final LinkedHashMap<Class<?>, Module> modules = new LinkedHashMap<>();

    public Module getModule(String name) {
        return modules.values().stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        return (T) modules.get(clazz);
    }

    public void onKeyPress(int keyCode) {
        for (Module module : modules.values()) {
            if (module.getKey() == keyCode) {
                module.toggle();
            }
        }
    }
}
