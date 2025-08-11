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
package org.openrewrite.java;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * Rewrite's JavaParser is reliant on java's compiler internal classes that are now encapsulated within Java's
 * module system. Starting in Java 17, the JVM now enforces strong encapsulation of these internal classes and
 * default behavior is to throw a security exception when attempting to use these internal classes. This classloader
 * circumvents these security restrictions by isolating Rewrite's Java 17 parser implementation classes and then
 * loading any of the internal classes directly from the .jmod files.
 * <p>
 * NOTE: Any classes in the package "org.openrewrite.java.isolated" will be loaded into this isolated classloader.
 */
public class JavaUnrestrictedClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    final List<Path> modules;

    public JavaUnrestrictedClassLoader(ClassLoader parentClassloader) {
        this(parentClassloader, getLombok());
    }

    private static List<URL> getLombok() {
        try {
            return JavaParser.dependenciesFromClasspath("lombok").stream().map(it -> {
                try {
                    return it.toUri().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).collect(toList());
        } catch (Exception e) {
            return emptyList();
        }
    }

    public JavaUnrestrictedClassLoader(ClassLoader parentClassloader, List<URL> jars) {
        super(jars.toArray(new URL[0]), parentClassloader);

        //A list of modules to load internal classes from
        final FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        modules = Arrays.asList(
                fs.getPath("modules", "jdk.compiler"),
                fs.getPath("modules", "java.compiler"),
                fs.getPath("modules", "java.base")
        );
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {

        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }

            String internalName = name.replace('.', '/') + ".class";

            //If the class is in the package "org.openrewrite.java.isolated", load it from this class loader.
            Class<?> _class = loadIsolatedClass(name);
            if (_class != null) {
                return _class;
            }

            //Otherwise look for internal classes in the list of modules.
            if (name.startsWith("com.sun") || name.startsWith("sun")) {
                try {
                    for (Path path : modules) {
                        Path classFile = path.resolve(internalName);
                        if (Files.exists(classFile)) {
                            byte[] bytes = Files.readAllBytes(classFile);
                            return defineClass(name, bytes, 0, bytes.length);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (name.startsWith("lombok")) {
                return findClass(name);
            }
            return super.loadClass(name);
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        // Give precedence to our own Lombok handlers
        if (name.startsWith("META-INF/services/lombok.")) {
            Objects.requireNonNull(name);
            @SuppressWarnings("unchecked")
            Enumeration<URL>[] tmp = (Enumeration<URL>[]) new Enumeration<?>[2];
            tmp[0] = findResources(name);
            tmp[1] = getParent().getResources(name);

            return new CompoundEnumeration<>(tmp);
        }
        return super.getResources(name);
    }

    private static final class CompoundEnumeration<E> implements Enumeration<E> {
        private final Enumeration<E>[] enums;
        private int index;

        public CompoundEnumeration(Enumeration<E>[] enums) {
            this.enums = enums;
        }

        private boolean next() {
            while (index < enums.length) {
                if (enums[index].hasMoreElements()) {
                    return true;
                }
                index++;
            }
            return false;
        }

        @Override
        public boolean hasMoreElements() {
            return next();
        }

        @Override
        public E nextElement() {
            if (!next()) {
                throw new NoSuchElementException();
            }
            return enums[index].nextElement();
        }
    }

    @Override
    public @Nullable URL getResource(String name) {
        try {
            for (Path path : modules) {
                Path classFile = path.resolve(name);
                if (Files.exists(classFile)) {
                    return classFile.toUri().toURL();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return super.getResource(name);
    }

    private @Nullable Class<?> loadIsolatedClass(String className) {
        if (!className.startsWith("org.openrewrite.java.isolated")) {
            return null;
        }
        String internalName = className.replace('.', '/') + ".class";
        URL url = JavaParser.class.getClassLoader().getResource(internalName);
        if (url == null) {
            return null;
        }

        try (InputStream stream = url.openStream()) {
            ByteArrayOutputStream classBytes = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096];
            int bytesRead;
            while ((bytesRead = stream.read(bytes)) > 0) {
                classBytes.write(bytes, 0, bytesRead);
            }
            return defineClass(className, classBytes.toByteArray(), 0, classBytes.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
