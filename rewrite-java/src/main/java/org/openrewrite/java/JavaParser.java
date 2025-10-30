/*
 * Copyright 2020 the original author or authors.
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

import io.github.classgraph.ClassGraph;
import lombok.experimental.UtilityClass;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ToBeRemoved;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.internal.parser.RewriteClasspathJarClasspathLoader;
import org.openrewrite.java.internal.parser.TypeTable;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.style.NamedStyles;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public interface JavaParser extends Parser {

    /**
     * Set to <code>true</code> on an {@link ExecutionContext} supplied to parsing to skip generation of
     * type attribution from the class in {@link JavaSourceSet} marker.
     */
    String SKIP_SOURCE_SET_TYPE_GENERATION = "org.openrewrite.java.skipSourceSetTypeGeneration";

    static List<Path> runtimeClasspath() {
        return RuntimeClasspathCache.getRuntimeClasspath();
    }

    /**
     * Convenience utility for constructing a parser with binary dependencies on the runtime classpath of the process
     * constructing the parser.
     *
     * @param artifactNames The "artifact name" of the dependency to look for. Artifact name is the artifact portion of
     *                      group:artifact:version coordinates. For example, for Google's Guava (com.google.guava:guava:VERSION),
     *                      the artifact name is "guava".
     * @return A set of paths of jars on the runtime classpath matching the provided artifact names, to the extent such
     * matching jars can be found.
     */
    static List<Path> dependenciesFromClasspath(String... artifactNames) {
        List<Path> runtimeClasspath = RuntimeClasspathCache.getRuntimeClasspath();
        List<Path> artifacts = new ArrayList<>(artifactNames.length);
        List<String> missingArtifactNames = new ArrayList<>(artifactNames.length);
        for (String artifactName : artifactNames) {
            List<Path> matchedArtifacts = filterArtifacts(artifactName, runtimeClasspath);
            if (matchedArtifacts.isEmpty()) {
                missingArtifactNames.add(artifactName);
            } else {
                artifacts.addAll(matchedArtifacts);
            }
        }
        if (!missingArtifactNames.isEmpty()) {
            String missing = missingArtifactNames.stream().sorted().collect(joining("', '", "'", "'"));
            throw new IllegalArgumentException(String.format("Unable to find runtime dependencies beginning with: %s, classpath: %s",
                    missing, runtimeClasspath));
        }
        return artifacts;
    }

    /**
     * Filters the classpath entries to find paths that match the given artifact name.
     *
     * @param artifactName     The artifact name to search for.
     * @param runtimeClasspath The list of classpath URIs to search within.
     * @return List of Paths that match the artifact name.
     */
    // VisibleForTesting
    static List<Path> filterArtifacts(String artifactName, List<Path> runtimeClasspath) {
        List<Path> artifacts = new ArrayList<>();
        // Bazel automatically replaces '-' with '_' when generating jar files.
        String normalizedArtifactName = artifactName.replace('-', '_');
        Pattern jarPattern = Pattern.compile(String.format("(%s|%s)(?:-.*?)?\\.jar$", artifactName, normalizedArtifactName));
        // In a multi-project IDE classpath, some classpath entries aren't jars
        Pattern explodedPattern = Pattern.compile("/" + artifactName + "/");
        for (Path cpEntry : runtimeClasspath) {
            String cpEntryString = cpEntry.toString();
            if (jarPattern.matcher(cpEntryString).find() ||
                    explodedPattern.matcher(cpEntryString).find() && cpEntry.toFile().isDirectory()) {
                artifacts.add(cpEntry);
                // Do not break because jarPattern matches "foo-bar-1.0.jar" and "foo-1.0.jar" to "foo"
            }
        }
        return artifacts;
    }

    /**
     * Load artifacts from packaged resources. This is useful for loading dependencies which are not on the recipe
     * execution classpath, or where you need to load multiple different versions of the same artifact.
     * Supports both {@link TypeTable} `classpath.tsv.gz` and packaged resource jars in `META-INF/rewrite/classpath/`.
     *
     * @param ctx                       The execution context to use for loading resources.
     * @param artifactNamesWithVersions artifact prefix to match, e.g. "guava" or "guava-31" for a specific version.
     * @return A list of paths to the located artifacts.
     */
    static List<Path> dependenciesFromResources(ExecutionContext ctx, String... artifactNamesWithVersions) {
        if (artifactNamesWithVersions.length == 0) {
            return emptyList();
        }
        List<Path> artifacts = new ArrayList<>(artifactNamesWithVersions.length);
        Set<String> missingArtifactNames = new LinkedHashSet<>(Arrays.asList(artifactNamesWithVersions));

        TypeTable typeTable = TypeTable.fromClasspath(ctx, missingArtifactNames);
        if (typeTable != null) {
            for (Iterator<String> it = missingArtifactNames.iterator(); it.hasNext(); ) {
                String missingArtifactName = it.next();
                Path located = typeTable.load(missingArtifactName);
                if (located != null) {
                    artifacts.add(located);
                    it.remove();
                }
            }
        }

        if (!missingArtifactNames.isEmpty()) {
            try (RewriteClasspathJarClasspathLoader classpathLoader = new RewriteClasspathJarClasspathLoader(ctx)) {
                for (String missingArtifactName : new ArrayList<>(missingArtifactNames)) {
                    Path located = classpathLoader.load(missingArtifactName);
                    if (located != null) {
                        artifacts.add(located);
                        missingArtifactNames.remove(missingArtifactName);
                    }
                }
            }
        }

        if (!missingArtifactNames.isEmpty()) {
            String missing = missingArtifactNames.stream().sorted().collect(joining("', '", "'", "'"));
            throw new IllegalArgumentException(String.format("Unable to find classpath resource dependencies beginning with: %s", missing));
        }

        return artifacts;
    }

    /**
     * Builds a Java parser with a language level equal to that of the JDK running this JVM process.
     */
    static JavaParser.Builder<? extends JavaParser, ?> fromJavaVersion() {
        return JdkParserBuilderCache.getBuilder();
    }

    @Override
    default Stream<SourceFile> parse(ExecutionContext ctx, @Language("java") String... sources) {
        return parseInputs(
                Arrays.stream(sources)
                        .map(sourceFile -> new Input(
                                sourcePathFromSourceText(Paths.get(""), sourceFile), null,
                                () -> new ByteArrayInputStream(sourceFile.getBytes(getCharset(ctx))), true
                        ))
                        .collect(toList()),
                null,
                ctx
        );
    }

    @Override
    default Stream<SourceFile> parse(@Language("java") String... sources) {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        return parse(ctx, sources);
    }

    @Override
    default boolean accept(Path path) {
        return path.toString().endsWith(".java") && !path.endsWith("module-info.java");
    }

    /**
     * Clear any in-memory parser caches that may prevent re-parsing of classes with the same fully qualified name in
     * different rounds
     */
    @Override
    JavaParser reset();

    JavaParser reset(Collection<URI> uris);

    /**
     * Changes the classpath on the parser. Intended for use in multiple pass parsing, where we want to keep the
     * compiler symbol table intact for type attribution on later parses, i.e. for maven multi-module projects.
     *
     * @param classpath new classpath to use
     */
    void setClasspath(Collection<Path> classpath);

    @SuppressWarnings("unchecked")
    abstract class Builder<P extends JavaParser, B extends Builder<P, B>> extends Parser.Builder {
        protected Collection<Path> classpath = emptyList();
        protected Collection<String> artifactNames = emptyList();
        protected Collection<byte[]> classBytesClasspath = emptyList();
        protected JavaTypeCache javaTypeCache = new JavaTypeCache();

        @Nullable
        protected Collection<Input> dependsOn;

        protected Charset charset = Charset.defaultCharset();
        protected boolean logCompilationWarningsAndErrors = false;
        protected final List<NamedStyles> styles = new ArrayList<>();

        public Builder() {
            super(J.CompilationUnit.class);
        }

        public B logCompilationWarningsAndErrors(boolean logCompilationWarningsAndErrors) {
            this.logCompilationWarningsAndErrors = logCompilationWarningsAndErrors;
            return (B) this;
        }

        public B typeCache(JavaTypeCache javaTypeCache) {
            this.javaTypeCache = javaTypeCache;
            return (B) this;
        }

        public B charset(Charset charset) {
            this.charset = charset;
            return (B) this;
        }

        @SuppressWarnings("unused")
        public B dependsOn(Collection<Input> inputs) {
            this.dependsOn = inputs;
            return (B) this;
        }

        public B dependsOn(@Language("java") String... inputsAsStrings) {
            this.dependsOn = Arrays.stream(inputsAsStrings)
                    .map(input -> Input.fromString(resolveSourcePathFromSourceText(Paths.get(""), input), input))
                    .collect(toList());
            return (B) this;
        }

        public B classpath(Collection<Path> classpath) {
            this.artifactNames = emptyList();
            this.classpath = classpath;
            return (B) this;
        }

        // internal method which doesn't overwrite the classpath but just amends it
        @Incubating(since = "8.18.3")
        public B addClasspathEntry(Path entry) {
            if (classpath.isEmpty()) {
                classpath = singletonList(entry);
            } else if (!classpath.contains(entry)) {
                classpath = new ArrayList<>(classpath);
                classpath.add(entry);
            }
            return (B) this;
        }

        /**
         * Sets the classpath from runtime classpath dependencies of the process constructing the parser. Predates
         * {@link #classpathFromResources(ExecutionContext, String...)}, with the latter preferred to limit dependencies
         * needed on the recipe runtime classpath, as the runtime classpath may differ in say the CLI or Platform.
         * <p>
         * This is suitable, for example, when writing tests that may require older versions of dependencies,
         * without needing to add them to the recipe runtime classpath through resources.
         *
         * @return A list of paths to jars on the runtime classpath of the process constructing the parser.
         */
        public B classpath(String... artifactNames) {
            this.artifactNames = Arrays.asList(artifactNames);
            this.classpath = emptyList();
            return (B) this;
        }

        /**
         * Load artifacts from packaged resources. This is useful for loading dependencies which are not on the recipe
         * execution classpath, or where you need to load multiple different versions of the same artifact.
         * Supports both {@link TypeTable} `classpath.tsv.gz` and packaged resource jars in `META-INF/rewrite/classpath/`.
         *
         * @param ctx       The execution context to use for loading resources.
         * @param classpath artifact prefix to match, e.g. "guava" or "guava-31" for a specific version.
         * @return A list of paths to the located artifacts.
         */
        @SuppressWarnings({"UnusedReturnValue", "unused"})
        public B classpathFromResources(ExecutionContext ctx, String... classpath) {
            this.artifactNames = emptyList();
            this.classpath = dependenciesFromResources(ctx, classpath);
            return (B) this;
        }

        /**
         * @deprecated prefer {@link #classpath} and {@link #classpathFromResources(ExecutionContext, String...)}.
         */
        @Deprecated
        @ToBeRemoved(after = "2025-12-31", reason = "Use classpath or classpathFromResources instead.")
        public B classpath(byte[]... classpath) {
            this.classBytesClasspath = Arrays.asList(classpath);
            return (B) this;
        }

        public B styles(Iterable<? extends NamedStyles> styles) {
            for (NamedStyles style : styles) {
                this.styles.add(style);
            }
            return (B) this;
        }

        protected Collection<Path> resolvedClasspath() {
            if (!artifactNames.isEmpty()) {
                classpath = new ArrayList<>(classpath);
                classpath.addAll(JavaParser.dependenciesFromClasspath(artifactNames.toArray(new String[0])));
                artifactNames = emptyList();
            }
            return classpath;
        }

        @Override
        public abstract P build();

        @Override
        public String getDslName() {
            return "java";
        }

        @Override
        public Builder<P, B> clone() {
            Builder<P, B> clone = (Builder<P, B>) super.clone();
            clone.javaTypeCache = this.javaTypeCache.clone();
            return clone;
        }
    }

    @Override
    default Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return resolveSourcePathFromSourceText(prefix, sourceCode);
    }

    static Path resolveSourcePathFromSourceText(Path prefix, String sourceCode) {
        Pattern packagePattern = Pattern.compile("^package\\s+([^;]+);");
        Pattern classPattern = Pattern.compile("(class|interface|enum|record)\\s*(<[^>]*>)?\\s+(\\w+)");
        Pattern publicClassPattern = Pattern.compile("public\\s+" + classPattern.pattern());

        Function<String, @Nullable String> simpleName = sourceStr -> {
            Matcher classMatcher = publicClassPattern.matcher(sourceStr);
            if (classMatcher.find()) {
                return classMatcher.group(3);
            }
            classMatcher = classPattern.matcher(sourceStr);
            return classMatcher.find() ? classMatcher.group(3) : null;
        };

        Matcher packageMatcher = packagePattern.matcher(sourceCode);
        String pkg = packageMatcher.find() ? packageMatcher.group(1).replace('.', '/') + "/" : "";

        String className = Optional.ofNullable(simpleName.apply(sourceCode))
                .orElse(Long.toString(System.nanoTime())) + ".java";

        return prefix.resolve(Paths.get(pkg + className));
    }
}

@UtilityClass
class RuntimeClasspathCache {
    @Nullable
    private static volatile List<Path> runtimeClasspath = null;

    static List<Path> getRuntimeClasspath() {
        List<Path> paths = runtimeClasspath;
        if (paths == null) {
            synchronized (RuntimeClasspathCache.class) {
                paths = runtimeClasspath;
                if (paths == null) {
                    runtimeClasspath = paths = new ClassGraph()
                            .disableNestedJarScanning()
                            .getClasspathURIs().stream()
                            .filter(uri -> "file".equals(uri.getScheme()))
                            .map(Paths::get)
                            .collect(toList());
                }
            }
        }
        return paths;
    }
}

@UtilityClass
class JdkParserBuilderCache {
    // Cached supplier for the parser builder - initialized on first access
    private static volatile @Nullable Supplier<JavaParser.Builder<? extends JavaParser, ?>> cachedBuilderSupplier = null;

    static JavaParser.Builder<? extends JavaParser, ?> getBuilder() {
        Supplier<JavaParser.Builder<? extends JavaParser, ?>> supplier = cachedBuilderSupplier;
        if (supplier != null) {
            return supplier.get();
        }

        synchronized (JdkParserBuilderCache.class) {
            // Double-check after acquiring lock
            supplier = cachedBuilderSupplier;
            if (supplier != null) {
                return supplier.get();
            }

            // Determine Java version once
            String[] versionParts = System.getProperty("java.version").split("[.-]");
            int version = Integer.parseInt(versionParts[0]);
            if (version == 1) {
                version = 8;
            }

            // Try to find and cache appropriate parser
            if (version > 21) {
                supplier = tryCreateBuilderSupplier("org.openrewrite.java.Java25Parser");
            }

            if (version > 17 && supplier == null) {
                supplier = tryCreateBuilderSupplier("org.openrewrite.java.Java21Parser");
            }

            if (version > 11 && supplier == null) {
                supplier = tryCreateBuilderSupplier("org.openrewrite.java.Java17Parser");
            }

            if (version > 8 && supplier == null) {
                supplier = tryCreateBuilderSupplier("org.openrewrite.java.Java11Parser");
            }

            if (supplier == null) {
                supplier = tryCreateBuilderSupplier("org.openrewrite.java.Java8Parser");
            }

            if (supplier != null) {
                cachedBuilderSupplier = supplier;
                return supplier.get();
            }

            throw new IllegalStateException("Unable to create a Java parser instance. " +
                    "`rewrite-java-8`, `rewrite-java-11`, `rewrite-java-17`, `rewrite-java-21`, or `rewrite-java-25` must be on the classpath.");
        }
    }

    private static @Nullable Supplier<JavaParser.Builder<? extends JavaParser, ?>> tryCreateBuilderSupplier(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Method builderMethod = clazz.getDeclaredMethod("builder");
            return () -> {
                try {
                    //noinspection rawtypes,unchecked
                    return (JavaParser.Builder) builderMethod.invoke(null);
                } catch (Throwable e) {
                    throw new RuntimeException("Failed to invoke builder() on " + className, e);
                }
            };
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return null; // This parser version isn't available
        }
    }
}
