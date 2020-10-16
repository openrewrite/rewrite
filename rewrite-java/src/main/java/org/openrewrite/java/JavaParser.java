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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import org.openrewrite.Parser;
import org.openrewrite.Style;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

public interface JavaParser extends Parser<J.CompilationUnit> {
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

        return Arrays.stream(System.getProperty("java.class.path").split("\\Q" + System.getProperty("path.separator") + "\\E"))
                .filter(cpEntry -> artifactNamePatterns.stream().anyMatch(namePattern -> namePattern.matcher(cpEntry).find()))
                .map(cpEntry -> new File(cpEntry).toPath())
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
    default List<J.CompilationUnit> parse(String... sources) {
        Pattern classPattern = Pattern.compile("(class|interface|enum)\\s*(<[^>]*>)?\\s+(\\w+)");

        Function<String, String> simpleName = sourceStr -> {
            Matcher classMatcher = classPattern.matcher(sourceStr);
            return classMatcher.find() ? classMatcher.group(3) : null;
        };

        return parseInputs(
                Arrays.stream(sources)
                        .map(sourceFile -> {
                            URI uri = URI.create(Optional.ofNullable(simpleName.apply(sourceFile))
                                .orElse(Long.toString(System.nanoTime())) + ".java");
                            return new Input(
                                    uri,
                                    () -> new ByteArrayInputStream(sourceFile.getBytes())
                            );
                        })
                        .collect(toList()),
                null
        );
    }

    @Override
    default boolean accept(URI path) {
        return path.toString().endsWith(".java");
    }

    /**
     * Clear any in-memory parser caches that may prevent reparsing of classes with the same fully qualified name in
     * different rounds
     */
    JavaParser reset();

    @SuppressWarnings("unchecked")
    abstract class Builder<P extends JavaParser, B extends Builder<P, B>> {
        @Nullable
        protected Collection<Path> classpath;

        protected Charset charset = Charset.defaultCharset();
        protected boolean relaxedClassTypeMatching = false;
        protected MeterRegistry meterRegistry = Metrics.globalRegistry;
        protected boolean logCompilationWarningsAndErrors = true;
        protected List<JavaStyle> styles = new ArrayList<>();

        public B logCompilationWarningsAndErrors(boolean logCompilationWarningsAndErrors) {
            this.logCompilationWarningsAndErrors = logCompilationWarningsAndErrors;
            return (B) this;
        }

        public B meterRegistry(MeterRegistry registry) {
            this.meterRegistry = registry;
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

        public B classpath(Collection<Path> classpath) {
            this.classpath = classpath;
            return (B) this;
        }

        public B classpath(String... classpath) {
            this.classpath = dependenciesFromClasspath(classpath);
            return (B) this;
        }

        public B styles(Iterable<? extends Style> styles) {
            stream(styles.spliterator(), false)
                    .filter(JavaStyle.class::isInstance)
                    .map(JavaStyle.class::cast)
                    .forEach(this.styles::add);
            return (B) this;
        }

        public abstract P build();
    }
}
