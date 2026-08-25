// Copyright (c) Lefraudeur. All rights reserved.
// Original work: https://github.com/Lefraudeur/MujinaBaseV2

package com.emberinjector.framework.internal;

public class EventClassLoader extends ClassLoader
{
    private final MemoryJarClassLoader newClassLoader;

    public EventClassLoader(ClassLoader parent, MemoryJarClassLoader newClassloader)
    {
        super(parent);
        this.newClassLoader = newClassloader;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        try
        {
            return super.loadClass(name, resolve);
        }
        catch (ClassNotFoundException ignored)
        {
        }

        return newClassLoader.loadClassNoDelegation(name, resolve);
    }
}
