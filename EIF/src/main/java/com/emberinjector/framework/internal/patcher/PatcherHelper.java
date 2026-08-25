// Copyright (c) Lefraudeur. All rights reserved.
// Copyright (c) achul123. All rights reserved.
// Original work: https://github.com/Lefraudeur/MujinaBaseV2
// Fork:         https://github.com/achul123/MujinaBaseV2

package com.emberinjector.framework.internal.patcher;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

public class PatcherHelper {

    private static ClassModifier[] classModifiers;
    private static Map<String, ClassModifier> classModifierMap;

    private static byte[][] helperClassBytes;
    private static String[] helperClassNames;

    public static boolean init() {
        try {
            classModifiers = Patcher.classModifiers;
            classModifierMap = new HashMap<>();
            for (ClassModifier cm : classModifiers) {
                classModifierMap.put(cm.name, cm);
            }
            prepareHelperClassData();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void prepareHelperClassData() {
        String[] names = {
            "com.emberinjector.framework.internal.Canceler",
            "com.emberinjector.framework.internal.Thrower",
            "com.emberinjector.framework.internal.EventDelegate",
            "com.emberinjector.framework.client.EventDispatcher"
        };
        helperClassNames = names;
        helperClassBytes = new byte[names.length][];
        for (int i = 0; i < names.length; i++) {
            try {
                String path = names[i].replace('.', '/') + ".class";
                InputStream is = PatcherHelper.class.getClassLoader().getResourceAsStream(path);
                if (is != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int read;
                    while ((read = is.read(buf)) != -1) baos.write(buf, 0, read);
                    helperClassBytes[i] = baos.toByteArray();
                    is.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static byte[][] getHelperClassBytes() {
        return helperClassBytes.clone();
    }

    public static String[] getHelperClassNames() {
        return helperClassNames;
    }

    public static ClassLoader[] getTargetClassLoaders() {
        if (classModifiers == null) return new ClassLoader[0];
        Set<ClassLoader> loaders = new HashSet<>();
        for (ClassModifier cm : classModifiers) {
            try {
                Class<?> targetClass = Class.forName(cm.name.replace('/', '.'));
                ClassLoader loader = targetClass.getClassLoader();
                if (loader != null) {
                    loaders.add(loader);
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        return loaders.toArray(new ClassLoader[0]);
    }

    public static Class<?>[] getClassesToTransform() {
        if (classModifiers == null) return new Class<?>[0];
        List<Class<?>> classes = new ArrayList<>();
        for (ClassModifier cm : classModifiers) {
            try {
                classes.add(Class.forName(cm.name.replace('/', '.')));
            } catch (ClassNotFoundException ignored) {
            }
        }
        return classes.toArray(new Class<?>[0]);
    }

    public static ClassModifier getClassModifier(Class<?> classToModify) {
        if (classToModify == null || classModifierMap == null) return null;
        return classModifierMap.get(classToModify.getName().replace('.', '/'));
    }

    public static Set<String> getEventHandlerClassNames() {
        if (classModifiers == null) return Collections.emptySet();
        Set<String> names = new HashSet<>();
        for (ClassModifier cm : classModifiers) {
            names.addAll(cm.getEventHandlerClassesNames());
        }
        return names;
    }

    public static Set<String> getLunarClassLoaderExcludeSet() {
        if (classModifiers == null) return Collections.emptySet();
        Set<String> names = new HashSet<>();
        for (ClassModifier cm : classModifiers) {
            names.add(cm.name.replace('/', '.'));
        }
        return names;
    }
}
