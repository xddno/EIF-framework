// Copyright (c) Lefraudeur. All rights reserved.
// Original work: https://github.com/Lefraudeur/MujinaBaseV2

package com.emberinjector.framework.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class MemoryJarClassLoader extends ClassLoader
{
    private final byte[] jarBytes;
    private final Map<String, byte[]> classCache = new ConcurrentHashMap<>();
    private static final Set<String> DELEGATE_PREFIXES = new HashSet<>();
    static {
        DELEGATE_PREFIXES.add("org.objectweb.asm");
    }

    public MemoryJarClassLoader(byte[] jarBytes)
    {
        this.jarBytes = jarBytes;
        preloadJar();
    }

    public MemoryJarClassLoader(byte[] jarBytes, ClassLoader parent)
    {
        super(parent);
        this.jarBytes = jarBytes;
        preloadJar();
    }

    private void preloadJar()
    {
        try (JarInputStream inputStream = new JarInputStream(new ByteArrayInputStream(jarBytes)))
        {
            for (JarEntry entry = inputStream.getNextJarEntry(); entry != null; entry = inputStream.getNextJarEntry())
            {
                if (entry.isDirectory())
                {
                    inputStream.closeEntry();
                    continue;
                }
                ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                byte[] buff = new byte[4096];
                int read = 0;
                while ((read = inputStream.read(buff)) != -1)
                    arrayOutputStream.write(buff, 0, read);
                classCache.put(entry.getName(), arrayOutputStream.toByteArray());
                inputStream.closeEntry();
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to preload JAR", e);
        }
    }

    public Class<?> loadClassNoDelegation(String name, boolean resolve) throws ClassNotFoundException
    {
        Class<?> found = findLoadedClass(name);
        if (found == null)
            found = findClass(name);
        if (resolve)
            resolveClass(found);
        return found;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        for (String prefix : DELEGATE_PREFIXES)
        {
            if (name.startsWith(prefix))
            {
                Class<?> found = findLoadedClass(name);
                if (found == null)
                    found = findClass(name);
                if (resolve)
                    resolveClass(found);
                return found;
            }
        }
        return super.loadClass(name, resolve);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException
    {
        String path = name.replace('.', '/');
        path += ".class";
        byte[] classBytes = classCache.get(path);
        if (classBytes == null)
            throw new ClassNotFoundException(name);
        return defineClass(name, classBytes, 0, classBytes.length);
    }

    @Override
    public InputStream getResourceAsStream(String name)
    {
        InputStream stream = super.getResourceAsStream(name);
        if (stream != null)
            return stream;
        byte[] extracted = classCache.get(name);
        if (extracted == null)
            return null;
        return new ByteArrayInputStream(extracted);
    }
}
