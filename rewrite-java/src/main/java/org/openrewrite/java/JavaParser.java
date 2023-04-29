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
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;
import io.github.classgraph.ScanResult;
import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.style.NamedStyles;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public interface JavaParser extends Parser<J.CompilationUnit> {

    /**
     * Set to <code>true</code> on an {@link ExecutionContext} supplied to parsing to skip generation of
     * type attribution from the class in {@link JavaSourceSet} marker.
     */
    String SKIP_SOURCE_SET_TYPE_GENERATION = "org.openrewrite.java.skipSourceSetTypeGeneration";

    static List<Path> runtimeClasspath() {
        return new ClassGraph()
                .disableNestedJarScanning()
                .getClasspathURIs().stream()
                .map(Paths::get).collect(toList());
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
        List<URI> runtimeClasspath = new ClassGraph().getClasspathURIs();
        List<Path> artifacts = new ArrayList<>(artifactNames.length);
        List<String> missingArtifactNames = new ArrayList<>(artifactNames.length);
        for (String artifactName : artifactNames) {
            Pattern jarPattern = Pattern.compile(artifactName + "-.*?\\.jar$");
            // In a multi-project IDE classpath, some classpath entries aren't jars
            Pattern explodedPattern = Pattern.compile("/" + artifactName + "/");
            boolean lacking = true;
            for (URI cpEntry : runtimeClasspath) {
                String cpEntryString = cpEntry.toString();
                if (jarPattern.matcher(cpEntryString).find()
                    || (explodedPattern.matcher(cpEntryString).find()
                        && Paths.get(cpEntry).toFile().isDirectory())) {
                    artifacts.add(Paths.get(cpEntry));
                    lacking = false;
                    // Do not break because jarPattern matches "foo-bar-1.0.jar" and "foo-1.0.jar" to "foo"
                }
            }
            if (lacking) {
                missingArtifactNames.add(artifactName);
            }
        }

        if (!missingArtifactNames.isEmpty()) {
            throw new IllegalArgumentException("Unable to find runtime dependencies beginning with: " +
                                               missingArtifactNames.stream().map(a -> "'" + a + "'").sorted().collect(joining(", ")));
        }

        return artifacts;
    }

    static List<Path> dependenciesFromResources(ExecutionContext ctx, String... artifactNamesWithVersions) {
        List<Path> artifacts = new ArrayList<>(artifactNamesWithVersions.length);
        Set<String> missingArtifactNames = new LinkedHashSet<>(artifactNamesWithVersions.length);
        File resourceTarget = JavaParserExecutionContextView.view(ctx)
                .getParserClasspathDownloadTarget();

        nextArtifact:
        for (String artifactName : artifactNamesWithVersions) {
            Pattern jarPattern = Pattern.compile(artifactName + "-?.*\\.jar$");
            File[] extracted = resourceTarget.listFiles();
            if (extracted != null) {
                for (File file : extracted) {
                    if (jarPattern.matcher(file.getName()).find()) {
                        artifacts.add(file.toPath());
                        continue nextArtifact;
                    }
                }
            }
            missingArtifactNames.add(artifactName);
        }

        if (missingArtifactNames.isEmpty()) {
            return artifacts;
        }

        Class<?> caller;
        try {
            // StackWalker is only available in Java 15+, but right now we only use classloader isolated
            // recipe instances in Java 17 environments, so we can safely use StackWalker there.
            Class<?> options = Class.forName("java.lang.StackWalker$Option");
            Object retainOption = options.getDeclaredField("RETAIN_CLASS_REFERENCE").get(null);

            Class<?> walkerClass = Class.forName("java.lang.StackWalker");
            Method getInstance = walkerClass.getDeclaredMethod("getInstance", options);
            Object walker = getInstance.invoke(null, retainOption);
            Method getDeclaringClass = Class.forName("java.lang.StackWalker$StackFrame").getDeclaredMethod("getDeclaringClass");
            caller = (Class<?>) walkerClass.getMethod("walk", Function.class).invoke(walker, (Function<Stream<Object>, Object>) s -> s
                    .map(f -> {
                        try {
                            return (Class<?>) getDeclaringClass.invoke(f);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(c -> !c.getName().equals(JavaParser.class.getName()) &&
                                 !c.getName().equals(JavaParser.Builder.class.getName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unable to find caller of JavaParser.dependenciesFromResources(..)")));
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException |
                 NoSuchMethodException | InvocationTargetException e) {
            caller = JavaParser.class;
        }

        try (ScanResult result = new ClassGraph().acceptPaths("META-INF/rewrite/classpath")
                .addClassLoader(caller.getClassLoader())
                .scan()) {
            ResourceList resources = result.getResourcesWithExtension(".jar");

            for (String artifactName : new ArrayList<>(missingArtifactNames)) {
                Pattern jarPattern = Pattern.compile(artifactName + "-?.*\\.jar$");
                for (Resource resource : resources) {
                    if (jarPattern.matcher(resource.getPath()).find()) {
                        try {
                            Path artifact = resourceTarget.toPath().resolve(Paths.get(resource.getPath()).getFileName());

                            Files.copy(
                                    requireNonNull(caller.getResourceAsStream("/" + resource.getPath())),
                                    artifact
                            );
                            missingArtifactNames.remove(artifactName);
                            artifacts.add(artifact);
                        } catch (FileAlreadyExistsException ignore) {
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        break;
                    }
                }
            }

            if (!missingArtifactNames.isEmpty()) {
                throw new IllegalArgumentException("Unable to find classpath resource dependencies beginning with: " +
                                                   missingArtifactNames.stream().map(a -> "'" + a + "'").sorted().collect(joining(", ", "", ".\n")) +
                                                   "The caller is of type " + (caller == null ? "NO CALLER IDENTIFIED" : caller.getName()) + ".\n" +
                                                   "The resources resolvable from the caller's classpath are: " +
                                                   resources.stream().map(Resource::getPath).sorted().collect(joining(", "))
                );
            }
        }

        return artifacts;
    }

    /**
     * Builds a Java parser with a language level equal to that of the JDK running this JVM process.
     */
    static JavaParser.Builder<? extends JavaParser, ?> fromJavaVersion() {
        JavaParser.Builder<? extends JavaParser, ?> javaParser;
        String[] versionParts = System.getProperty("java.version").split("\\.");
        int version = Integer.parseInt(versionParts[0]);
        if (version == 1) {
            version = 8;
        }

        if (version >= 17) {
            try {
                javaParser = (JavaParser.Builder<? extends JavaParser, ?>) Class
                        .forName("org.openrewrite.java.Java17Parser")
                        .getDeclaredMethod("builder")
                        .invoke(null);
                return javaParser;
            } catch (Exception e) {
                //Fall through, look for a parser on an older version.
            }
        }

        if (version >= 11) {
            try {
                javaParser = (JavaParser.Builder<? extends JavaParser, ?>) Class
                        .forName("org.openrewrite.java.Java11Parser")
                        .getDeclaredMethod("builder")
                        .invoke(null);
                return javaParser;
            } catch (Exception e) {
                //Fall through, look for a parser on an older version.
            }
        }

        try {
            javaParser = (JavaParser.Builder<? extends JavaParser, ?>) Class
                    .forName("org.openrewrite.java.Java8Parser")
                    .getDeclaredMethod("builder")
                    .invoke(null);
            return javaParser;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create a Java parser instance. " +
                                            "`rewrite-java-8`, `rewrite-java-11`, or `rewrite-java-17` must be on the classpath.", e);
        }
    }

    @Override
    default Stream<J.CompilationUnit> parse(ExecutionContext ctx, @Language("java") String... sources) {
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
    default Stream<J.CompilationUnit> parse(@Language("java") String... sources) {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        return parse(ctx, sources);
    }

    @Override
    default boolean accept(Path path) {
        return path.toString().endsWith(".java");
    }

    /**
     * Clear any in-memory parser caches that may prevent re-parsing of classes with the same fully qualified name in
     * different rounds
     */
    JavaParser reset();

    JavaParser reset(Collection<URI> uris);

    /**
     * Changes the classpath on the parser. Intended for use in multiple pass parsing, where we want to keep the
     * compiler symbol table intact for type attribution on later parses, i.e. for maven multi-module projects.
     *
     * @param classpath new classpath to use
     */
    void setClasspath(Collection<Path> classpath);

    /**
     * Changes the source set on the parser. Intended for use in multiple pass parsing, where we want to keep the
     * compiler symbol table intact for type attribution on later parses, i.e. for maven multi-module projects.
     *
     * @deprecated
     * @param sourceSet source set used to set {@link org.openrewrite.java.marker.JavaSourceSet} markers on
     *                  subsequently parsed {@link J.CompilationUnit}
     */
    @Deprecated//(since = "7.40.0", forRemoval = true)
    void setSourceSet(String sourceSet);

    @Deprecated//(since = "7.40.0", forRemoval = true)
    JavaSourceSet getSourceSet(ExecutionContext ctx);

    @SuppressWarnings("unchecked")
    abstract class Builder<P extends JavaParser, B extends Builder<P, B>> extends Parser.Builder {
        protected Collection<Path> classpath = Collections.emptyList();
        protected Collection<byte[]> classBytesClasspath = Collections.emptyList();
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

        public B dependsOn(Collection<Input> inputs) {
            this.dependsOn = inputs;
            return (B) this;
        }

        public B dependsOn(@Language("java") String... inputsAsStrings) {
            this.dependsOn = Arrays.stream(inputsAsStrings)
                    .map(Input::fromString)
                    .collect(toList());
            return (B) this;
        }

        public B classpath(Collection<Path> classpath) {
            this.classpath = classpath;
            return (B) this;
        }

        public B classpath(String... classpath) {
            this.classpath = dependenciesFromClasspath(classpath);
            return (B) this;
        }

        public B classpathFromResources(ExecutionContext ctx, String... classpath) {
            this.classpath = dependenciesFromResources(ctx, classpath);
            return (B) this;
        }

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
        Pattern packagePattern = Pattern.compile("^package\\s+([^;]+);");
        Pattern classPattern = Pattern.compile("(class|interface|enum|record)\\s*(<[^>]*>)?\\s+(\\w+)");

        Function<String, String> simpleName = sourceStr -> {
            Matcher classMatcher = classPattern.matcher(sourceStr);
            return classMatcher.find() ? classMatcher.group(3) : null;
        };

        Matcher packageMatcher = packagePattern.matcher(sourceCode);
        String pkg = packageMatcher.find() ? packageMatcher.group(1).replace('.', '/') + "/" : "";

        String className = Optional.ofNullable(simpleName.apply(sourceCode))
                                   .orElse(Long.toString(System.nanoTime())) + ".java";

        return prefix.resolve(Paths.get(pkg + className));
    }
}
