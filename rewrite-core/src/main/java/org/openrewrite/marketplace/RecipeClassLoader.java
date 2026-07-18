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

import java.io.IOException;
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

    // Classes whose FQN collides with a {@link #PARENT_DELEGATED_PREFIXES} entry by
    // accident — `org.openrewrite.RecipeBuilder` etc. all `startsWith("org.openrewrite.Recipe")`
    // — but which are Kotlin recipe-DSL internals that must resolve through the recipe-jar's
    // classloader. Without this exclusion the lambda receivers (child-loaded `EditScope`,
    // `ScanScope`, etc.) and the anonymous `Recipe` synthesized inside `RecipeBuilder.build()`
    // (parent-loaded) end up on different loaders, producing a `ClassCastException` on the
    // first `getVisitor()` call. See moderneinc/moderne-cli#3949.
    private static final Set<String> NON_DELEGATED_CLASSES = new HashSet<>(Arrays.asList(
            "org.openrewrite.RecipeBuilder",
            "org.openrewrite.RecipeDsl",
            "org.openrewrite.RecipeDslKt"
    ));

    // Core OpenRewrite types that must be loaded from parent (from Moderne's RecipeClassLoader)
    private static final List<String> PARENT_DELEGATED_PREFIXES = Arrays.asList(
            "org.openrewrite.AbstractRecipe",
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
            "org.openrewrite.Singleton",
            "org.openrewrite.Charset",
            "org.openrewrite.Checksum",
            "org.openrewrite.remote",
            "org.openrewrite.rpc.Reference",
            "org.openrewrite.rpc.RpcCodec",
            "org.openrewrite.rpc.RpcObjectData",
            "org.openrewrite.rpc.RpcReceiveQueue",
            "org.openrewrite.rpc.RpcRecipe",
            "org.openrewrite.rpc.RpcSendQueue",
            "org.openrewrite.Parser",
            "org.openrewrite.Tree",
            "org.openrewrite.Validated",
            "org.openrewrite.ValidationException",
            "org.openrewrite.config",
            "org.openrewrite.internal",
            "org.openrewrite.marker",
            "org.openrewrite.scheduling",
            "org.openrewrite.semver",
            "org.openrewrite.style",
            "org.openrewrite.template",
            "org.openrewrite.trait",
            "org.openrewrite.polyglot",
            "org.openrewrite.FileAttributes",
            "org.openrewrite.ParseErrorVisitor",
            "org.openrewrite.PrintOutputCapture",
            "org.openrewrite.ipc.http.HttpSender",
            "org.openrewrite.golang.GoModVisitor",
            "org.openrewrite.golang.GoSumVisitor",
            "org.openrewrite.gradle.attributes.Category",
            "org.openrewrite.gradle.attributes.ProjectAttribute",
            "org.openrewrite.java.JavadocVisitor",
            "org.openrewrite.java.JavaParser",
            "org.openrewrite.java.Java17Parser",
            "org.openrewrite.java.MethodMatcher",
            "org.openrewrite.java.TypeNameMatcher",
            "org.openrewrite.java.internal.TypesInUse",
            "org.openrewrite.java.TypeNameMatcher",
            // JavaSourceSet#getTypeFactory crosses the recipe/parent classloader
            // boundary when JavaTemplate reads it from the enclosing source file's
            // marker; the interface must be shared so the cast in JavaTemplateParser
            // succeeds.
            "org.openrewrite.java.internal.JavaTypeFactory",
            "org.openrewrite.maven.MavenDownloadingException",
            "org.openrewrite.maven.MavenDownloadingExceptions",
            "org.openrewrite.maven.MavenExecutionContextView",
            "org.openrewrite.maven.MavenSettings",
            "org.openrewrite.maven.internal",
            "org.openrewrite.maven.attributes.Attributed",
            "org.openrewrite.protobuf.ProtoVisitor",
            "org.openrewrite.text.PlainText",
            "org.openrewrite.quark.Quark"
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

        // Force child-load for Kotlin recipe-DSL types that would otherwise be caught by
        // a coarse `org.openrewrite.Recipe*` prefix match below. Recipe authors compile
        // their `recipe { … }` lambdas against the bundled rewrite-kotlin version; the
        // receiver-type linkage and the in-builder `EditScope()` construction must agree
        // on which `Class<?>` they resolve to. Covers nested anonymous classes
        // (`RecipeBuilder$buildSimpleRecipe$1`, etc.).
        if (NON_DELEGATED_CLASSES.contains(className)) {
            return false;
        }
        for (String cls : NON_DELEGATED_CLASSES) {
            if (className.startsWith(cls + "$")) {
                return false;
            }
        }

        // SLF4J, Jackson, and the Kotlin runtime should always come from the parent.
        // Why kotlin: if both the parent and a recipe jar ship kotlin-stdlib, types like
        // kotlin.jvm.functions.Function1 get defined by both loaders. When Jackson (loaded
        // from parent) interacts with jackson-module-kotlin (typically bundled in the recipe
        // jar), the JVM raises a LinkageError on loader-constraint violations.
        // See moderneinc/customer-requests#2372.
        if (className.startsWith("org.slf4j") ||
            className.startsWith("com.fasterxml.jackson") ||
            className.startsWith("kotlin.")) {
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
