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

import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.style.NamedStyles;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

public interface JavaParser extends Parser<J.CompilationUnit> {

    List<Path> runtimeClasspath = Collections.unmodifiableList(Arrays.stream(System.getProperty("java.class.path").split("\\Q" + System.getProperty("path.separator") + "\\E"))
            .map(cpEntry -> new File(cpEntry).toPath())
            .collect(toList()));

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
        List<Pattern> artifactNamePatterns = Arrays.stream(artifactNames)
                .map(name -> Pattern.compile(name + "-.*?\\.jar$"))
                .collect(toList());

        return runtimeClasspath.stream()
                .filter(cpEntry -> artifactNamePatterns.stream().anyMatch(namePattern -> namePattern.matcher(cpEntry.toString()).find()))
                .collect(toList());
    }

    /**
     * Builds a Java parser with a language level equal to that of the JDK running this JVM process.
     */
    static JavaParser.Builder<? extends JavaParser, ?> fromJavaVersion() {
        JavaParser.Builder<? extends JavaParser, ?> javaParser;
        try {
            if (System.getProperty("java.version").startsWith("1.8")) {
                javaParser = (JavaParser.Builder<? extends JavaParser, ?>) Class
                        .forName("org.openrewrite.java.Java8Parser")
                        .getDeclaredMethod("builder")
                        .invoke(null);
            } else {
                javaParser = (JavaParser.Builder<? extends JavaParser, ?>) Class
                        .forName("org.openrewrite.java.Java11Parser")
                        .getDeclaredMethod("builder")
                        .invoke(null);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create a Java parser instance. " +
                    "`rewrite-java-8` or `rewrite-java-11` must be on the classpath.");
        }

        return javaParser;
    }

    @Override
    default List<J.CompilationUnit> parse(ExecutionContext ctx, String... sources) {
        Pattern packagePattern = Pattern.compile("^package\\s+([^;]+);");
        Pattern classPattern = Pattern.compile("(class|interface|enum)\\s*(<[^>]*>)?\\s+(\\w+)");

        Function<String, String> simpleName = sourceStr -> {
            Matcher classMatcher = classPattern.matcher(sourceStr);
            return classMatcher.find() ? classMatcher.group(3) : null;
        };

        return parseInputs(
                Arrays.stream(sources)
                        .map(sourceFile -> {
                            Matcher packageMatcher = packagePattern.matcher(sourceFile);
                            String pkg = packageMatcher.find() ? packageMatcher.group(1).replace('.', '/') + "/" : "";

                            String className = Optional.ofNullable(simpleName.apply(sourceFile))
                                    .orElse(Long.toString(System.nanoTime())) + ".java";

                            Path path = Paths.get(pkg + className);
                            return new Input(
                                    path,
                                    () -> new ByteArrayInputStream(sourceFile.getBytes())
                            );
                        })
                        .collect(toList()),
                null,
                new InMemoryExecutionContext()
        );
    }

    @Override
    default List<J.CompilationUnit> parse(@Language("java") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
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

    /**
     * Changes the classpath on the parser. Intended for use in multiple pass parsing, where we want to keep the
     * compiler symbol table intact for type attribution on later parses, i.e. for maven multi-module projects.
     * @param classpath new classpath to use
     */
    void setClasspath(Collection<Path> classpath);

    @SuppressWarnings("unchecked")
    abstract class Builder<P extends JavaParser, B extends Builder<P, B>> {
        @Nullable
        protected Collection<Path> classpath = Collections.emptyList();

        protected Collection<byte[]> classBytesClasspath = Collections.emptyList();

        @Nullable
        protected Collection<Input> dependsOn;

        protected Charset charset = Charset.defaultCharset();
        protected boolean relaxedClassTypeMatching = false;
        protected boolean logCompilationWarningsAndErrors = false;
        protected final List<NamedStyles> styles = new ArrayList<>();

        public B logCompilationWarningsAndErrors(boolean logCompilationWarningsAndErrors) {
            this.logCompilationWarningsAndErrors = logCompilationWarningsAndErrors;
            return (B) this;
        }

        public B charset(Charset charset) {
            this.charset = charset;
            return (B) this;
        }

        public B relaxedClassTypeMatching(boolean relaxedClassTypeMatching) {
            this.relaxedClassTypeMatching = relaxedClassTypeMatching;
            return (B) this;
        }

        public B dependsOn(Collection<Input> inputs) {
            this.dependsOn = inputs;
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
    }
}
