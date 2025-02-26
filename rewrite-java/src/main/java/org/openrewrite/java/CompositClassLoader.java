/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
