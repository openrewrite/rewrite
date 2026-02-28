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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    private final Set<String> exportedPackages = Collections.synchronizedSet(new HashSet<>());

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
                            try {
                                return defineClass(name, bytes, 0, bytes.length);
                            } catch (LinkageError e) {
                                // On JDK 25+, cross-module type references in DocCommentTable can cause the
                                // app classloader to load jdk.compiler classes before this classloader does.
                                // Fall back to parent delegation for the already-loaded class.
                                try {
                                    Class<?> fallback = super.loadClass(name);
                                    // The fallback class is in a named module (e.g. jdk.compiler) while
                                    // classes we already defined are in our unnamed module. Add an export
                                    // from the named module to our unnamed module so they can interoperate.
                                    addExportIfNeeded(fallback);
                                    return fallback;
                                } catch (ClassNotFoundException cnfe) {
                                    throw e;
                                }
                            }
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

    /**
     * When a class falls back to parent delegation (due to LinkageError), it may end up in a
     * named module (e.g. jdk.compiler) while classes we already defined are in our unnamed module.
     * This method adds an export from the named module to our unnamed module so classes across the
     * module boundary can access each other.
     * <p>
     * All Module API access is done via reflection because this class compiles with --release 8.
     */
    private void addExportIfNeeded(Class<?> clazz) {
        try {
            Class<?> moduleClass = Class.forName("java.lang.Module");

            Method getModule = Class.class.getMethod("getModule");
            Object classModule = getModule.invoke(clazz);

            Method isNamed = moduleClass.getMethod("isNamed");
            if (!(boolean) isNamed.invoke(classModule)) {
                return;
            }

            String className = clazz.getName();
            int lastDot = className.lastIndexOf('.');
            if (lastDot < 0) {
                return;
            }
            String packageName = className.substring(0, lastDot);

            Method getUnnamedModule = ClassLoader.class.getMethod("getUnnamedModule");
            Object unnamedModule = getUnnamedModule.invoke(this);

            Method isExported = moduleClass.getMethod("isExported", String.class, moduleClass);
            if ((boolean) isExported.invoke(classModule, packageName, unnamedModule)) {
                return;
            }
            if (!exportedPackages.add(packageName)) {
                return;
            }

            forceAddExport(moduleClass, classModule, packageName, unnamedModule);
        } catch (Throwable t) {
            // Module API not available (Java 8) or unable to add export;
            // downstream code will likely fail with IllegalAccessError
        }
    }

    /**
     * Uses sun.misc.Unsafe to obtain a trusted MethodHandles.Lookup, then invokes
     * Module.implAddExports to add an export from the source module to the target module.
     * This bypasses the module system's caller check that would otherwise prevent code in the
     * unnamed module from adding exports to a named module.
     */
    private static void forceAddExport(Class<?> moduleClass, Object source, String packageName, Object target) throws Throwable {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Object unsafe = theUnsafe.get(null);

        Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        Method staticFieldOffset = unsafeClass.getMethod("staticFieldOffset", Field.class);
        long offset = (long) staticFieldOffset.invoke(unsafe, implLookupField);
        Method getObject = unsafeClass.getMethod("getObject", Object.class, long.class);
        MethodHandles.Lookup trustedLookup =
                (MethodHandles.Lookup) getObject.invoke(unsafe, MethodHandles.Lookup.class, offset);

        MethodHandle implAddExports = trustedLookup.findVirtual(
                moduleClass, "implAddExports",
                MethodType.methodType(void.class, String.class, moduleClass));
        implAddExports.invoke(source, packageName, target);
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
