/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.internal;

import io.quarkus.gizmo.ClassOutput;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class TreeVisitorAdapterClassLoader extends ClassLoader implements ClassOutput {
    private final Map<String, Class<?>> adaptedClasses = new HashMap<>();

    public TreeVisitorAdapterClassLoader(ClassLoader parent) {
        super(parent);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasClass(String name) {
        return adaptedClasses.containsKey(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> ex = findLoadedClass(name);
        if (ex != null) {
            return ex;
        }
        if (adaptedClasses.containsKey(name)) {
            return findClass(name);
        }
        return super.loadClass(name, resolve);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> clazz = adaptedClasses.get(name);
        if (clazz == null) {
            throw new ClassNotFoundException();
        }
        return clazz;
    }

    @Override
    public void write(String name, byte[] data) {
        if (System.getProperty("org.openrewrite.adapt.dumpClass") != null) {
            try {
                File dir = new File("target/test-classes/", name.substring(0, name.lastIndexOf("/")));
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
                File output = new File("target/test-classes/", name + ".class");
                Files.write(output.toPath(), data);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot dump the class: " + name, e);
            }
        }
        String normalizedName = name.replace('/', '.');
        adaptedClasses.put(
                normalizedName,
                defineClass(normalizedName, data, 0, data.length)
        );
    }

    @Override
    public Writer getSourceWriter(String className) {
        return new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) {
                // no-op
            }

            @Override
            public void flush() {
                // no-op
            }

            @Override
            public void close() {
                // no-op
            }
        };
    }
}
