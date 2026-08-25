package com.emberinjector.framework.property;

import com.emberinjector.framework.module.Module;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class PropertyManager {
    public final LinkedHashMap<Class<?>, ArrayList<Property<?>>> properties = new LinkedHashMap<>();

    public void register(Module module) {
        ArrayList<Property<?>> list = new ArrayList<>();
        for (Field field : module.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object obj = field.get(module);
                if (obj instanceof Property<?>) {
                    ((Property<?>) obj).setOwner(module);
                    list.add((Property<?>) obj);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        properties.put(module.getClass(), list);
    }
}
