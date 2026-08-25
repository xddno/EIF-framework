// MIT License. Copyright (c) EIF-framework author. See NOTICE.md.
// SPDX-License-Identifier: MIT

package net.minecraft.launchwrapper;

import java.util.HashMap;
import java.util.Map;

public class Launch {
    public static Map<String, Object> blackboard = new HashMap<>();
    public static ClassLoader classLoader = Launch.class.getClassLoader();
}
