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
package org.openrewrite.maven.marketplace;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
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
            "org.openrewrite.PrintOutputCapture",
            "org.openrewrite.ipc.http.HttpSender",
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

    public RecipeClassLoader(Path recipeJar, List<Path> classpath) {
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

            // Determine delegation strategy
            try {
                if (shouldDelegateToParent(name)) {
                    foundClass = parent.loadClass(name);
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
     * Create a standard ClassLoader for the recipe JAR and its dependencies.
     * This is primarily used for scanning operations where ClassGraph needs standard parent delegation.
     */
    public static ClassLoader forScanning(Path recipeJar, List<Path> classpath) {
        return new URLClassLoader(
                getUrls(recipeJar, classpath),
                RecipeClassLoader.class.getClassLoader()
        );
    }

    /**
     * Convert paths to URL array for URLClassLoader.
     */
    public static URL[] getUrls(Path recipeJar, List<Path> classpath) {
        return Stream.concat(classpath.stream(), Stream.of(recipeJar))
                .map(Path::toUri)
                .map(uri -> {
                    try {
                        return uri.toURL();
                    } catch (MalformedURLException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .toArray(URL[]::new);
    }
}
