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
package org.openrewrite.marketplace;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

/**
 * A classloader that provides maximum isolation for recipe implementations
 * while delegating to the parent for OpenRewrite API types that must be shared.
 */
public class RecipeClassLoader extends URLClassLoader {
    private final ClassLoader parent;

    // Core OpenRewrite types that must be loaded from parent (from Moderne's RecipeClassLoader)
    private static final List<String> PARENT_DELEGATED_PREFIXES = Arrays.asList(
            "org.openrewrite.Column",
            "org.openrewrite.Cursor",
            "org.openrewrite.DelegatingExecutionContext",
            "org.openrewrite.ExecutionContext",
            "org.openrewrite.InMemoryExecutionContext",
            "org.openrewrite.Option",
            "org.openrewrite.DataTable",
            "org.openrewrite.ParseExceptionResult",
            "org.openrewrite.ParseWarning",
            "org.openrewrite.Recipe",
            "org.openrewrite.Result",
            "org.openrewrite.ScanningRecipe",
            "org.openrewrite.SourceFile",
            "org.openrewrite.Charset",
            "org.openrewrite.Checksum",
            "org.openrewrite.remote",
            "org.openrewrite.Parser",
            "org.openrewrite.Tree",
            "org.openrewrite.Validated",
            "org.openrewrite.ValidationException",
            "org.openrewrite.config",
            "org.openrewrite.internal",
            "org.openrewrite.marker",
            "org.openrewrite.scheduling",
            "org.openrewrite.style",
            "org.openrewrite.template",
            "org.openrewrite.trait",
            "org.openrewrite.FileAttributes",
            "org.openrewrite.ParseErrorVisitor",
            "org.openrewrite.PrintOutputCapture",
            "org.openrewrite.ipc.http.HttpSender",
            "org.openrewrite.java.JavadocVisitor",
            "org.openrewrite.java.internal.TypesInUse",
            "org.openrewrite.maven.MavenDownloadingException",
            "org.openrewrite.maven.MavenDownloadingExceptions",
            "org.openrewrite.maven.MavenExecutionContextView",
            "org.openrewrite.maven.MavenSettings",
            "org.openrewrite.maven.internal",
            "org.openrewrite.text.PlainText",
            "org.openrewrite.quark.Quark",
            "org.openrewrite.java.JavaParser",
            "org.openrewrite.java.MethodMatcher"
    );

    public RecipeClassLoader(@Nullable Path recipeJar, List<Path> classpath) {
        this(getUrls(recipeJar, classpath), RecipeClassLoader.class.getClassLoader());
    }

    public RecipeClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, null); // Pass null as parent to URLClassLoader to control delegation manually
        this.parent = parent;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // Check if already loaded
            Class<?> foundClass = findLoadedClass(name);
            if (foundClass != null) {
                if (resolve) {
                    resolveClass(foundClass);
                }
                return foundClass;
            }

            foundClass = loadNestMember(name);
            if (foundClass != null) {
                if (resolve) {
                    resolveClass(foundClass);
                }
                return foundClass;
            }

            // Determine delegation strategy
            try {
                if (shouldDelegateToParent(name)) {
                    try {
                        foundClass = parent.loadClass(name);
                    } catch (ClassNotFoundException e) {
                        // Fall back to child if parent doesn't have the class.
                        // This handles marker/tree/style types from language-specific modules
                        // (e.g., org.openrewrite.gradle.marker.GradlePluginDescriptor)
                        // that aren't on the parent classloader.
                        foundClass = findClass(name);
                    }
                } else {
                    // Try child-first for non-delegated classes
                    foundClass = findClass(name);
                }
            } catch (ClassNotFoundException e) {
                // Fall back to parent if not found in child
                foundClass = parent.loadClass(name);
            }

            if (resolve) {
                resolveClass(foundClass);
            }
            return foundClass;
        }
    }

    /**
     * Anchors nestmates to the defining loader of their NestHost. The JVM uses the NestHost
     * attribute to gate private member access between siblings; if members of the same nest
     * end up on different defining loaders, access checks fail at runtime with
     * {@link IllegalAccessError}. Routing every member through the host's loader keeps the
     * nest intact regardless of how delegation rules are configured.
     *
     * @return the resolved class when {@code name} declares a NestHost, or {@code null} when
     *         the class has no NestHost attribute (or is its own host) and normal delegation
     *         should apply.
     */
    private @Nullable Class<?> loadNestMember(String name) throws ClassNotFoundException {
        String nestHostInternal = peekNestHost(name);
        if (nestHostInternal == null || nestHostInternal.equals(name.replace('.', '/'))) {
            return null;
        }
        Class<?> hostClass = loadClass(nestHostInternal.replace('/', '.'));
        ClassLoader hostLoader = hostClass.getClassLoader();
        if (hostLoader == this) {
            // Host is defined here: load the member here too. No parent fallback —
            // surfacing ClassNotFoundException is preferable to silently splitting the nest.
            return findClass(name);
        }
        if (hostLoader != null) {
            return hostLoader.loadClass(name);
        }
        return Class.forName(name, false, null);
    }

    private @Nullable String peekNestHost(String name) {
        // getResource already checks this loader's URLs first and falls through to the parent,
        // so we can detect nest membership even for members the child doesn't ship.
        URL url = getResource(name.replace('.', '/') + ".class");
        if (url == null) {
            return null;
        }
        try (InputStream in = url.openStream()) {
            String[] holder = new String[1];
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visitNestHost(String nestHost) {
                    holder[0] = nestHost;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
            return holder[0];
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public @Nullable URL getResource(String name) {
        Objects.requireNonNull(name);
        URL url = findResource(name);
        if (url == null) {
            return parent.getResource(name);
        }
        return url;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Objects.requireNonNull(name);
        @SuppressWarnings("unchecked")
        Enumeration<URL>[] tmp = (Enumeration<URL>[]) new Enumeration<?>[2];
        tmp[0] = findResources(name);
        tmp[1] = parent.getResources(name);

        return new CompoundEnumeration<>(tmp);
    }

    private boolean shouldDelegateToParent(String className) {
        // Check additional delegations first (can be overridden)
        for (String prefix : getAdditionalParentDelegatedPackages()) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }

        // SLF4J and Jackson should always be from parent
        if (className.startsWith("org.slf4j") || className.startsWith("com.fasterxml.jackson")) {
            return true;
        }

        // Non-OpenRewrite classes are handled by child-first loading
        if (!className.startsWith("org.openrewrite")) {
            return false;
        }

        // OpenRewrite API types that must be shared
        for (String prefix : PARENT_DELEGATED_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }

        // Check for tree/marker/style types using package structure
        return isTreeMarkerStyleType(className);
    }

    private boolean isTreeMarkerStyleType(String className) {
        String[] parts = className.split("\\.");
        if (parts.length < 4) {
            return false;
        }

        // Check if any package part is tree, marker, or style
        for (int i = 2; i < parts.length - 1; i++) {
            String part = parts[i];
            if ("tree".equals(part) || "marker".equals(part) || "style".equals(part)) {
                return true;
            }
        }

        // Check for Visitor pattern classes
        if (!className.contains("$")) {
            String last = parts[parts.length - 1];
            String secondToLast = parts[parts.length - 2];
            return last.endsWith("Visitor") &&
                   last.toLowerCase().startsWith(secondToLast.toLowerCase()) &&
                   last.length() == secondToLast.length() + "Visitor".length();
        }

        return false;
    }

    /**
     * @return List of class/package prefixes to delegate to parent classloader
     */
    protected List<String> getAdditionalParentDelegatedPackages() {
        return emptyList();
    }

    /**
     * Convert paths to URL array for URLClassLoader.
     * Directories must have URLs ending with '/' for URLClassLoader to recognize them.
     */
    public static URL[] getUrls(@Nullable Path recipeJar, List<Path> classpath) {
        return Stream.concat(classpath.stream(), Stream.of(recipeJar))
                .filter(Objects::nonNull)
                .map(path -> {
                    try {
                        if (Files.isDirectory(path)) {
                            // For directories, ensure URL ends with '/'
                            String urlString = path.toUri().toURL().toString();
                            if (!urlString.endsWith("/")) {
                                urlString += "/";
                            }
                            return new URL(urlString);
                        } else {
                            return path.toUri().toURL();
                        }
                    } catch (MalformedURLException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .toArray(URL[]::new);
    }
}

/*
 * A utility class that will enumerate over an array of enumerations.
 * @see java.lang.CompoundEnumeration
 */
final class CompoundEnumeration<E> implements Enumeration<E> {
    private final Enumeration<E>[] enums;
    private int index;

    public CompoundEnumeration(Enumeration<E>[] enums) {
        this.enums = enums;
    }

    private boolean next() {
        while (index < enums.length) {
            if (enums[index] != null && enums[index].hasMoreElements()) {
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
