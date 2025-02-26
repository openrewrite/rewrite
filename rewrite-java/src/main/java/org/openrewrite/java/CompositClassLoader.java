package org.openrewrite.java;

import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * A composite class loader that delegates to a list of child class loaders.
 * This Classloader comes in handy when recipes from different artifacts are used together which are loaded dependently.
 * The implementations will return the first found result.
 */
public class CompositClassLoader extends ClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final ClassLoader[] childLoaders;

    public CompositClassLoader(List<ClassLoader> childLoaders) {
        this.childLoaders = childLoaders.toArray(new ClassLoader[0]);
    }

    public CompositClassLoader(ClassLoader... childLoaders) {
        this.childLoaders = Arrays.copyOf(childLoaders, childLoaders.length);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for (ClassLoader childLoader : childLoaders) {
            try {
                return childLoader.loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public @Nullable URL getResource(String name) {
        for (ClassLoader childLoader : childLoaders) {
            URL resource = childLoader.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        return null; // null means found nothing
    }
}
