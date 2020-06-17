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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public interface JavaParser {
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
    
    List<J.CompilationUnit> parse(List<Path> sourceFiles, @Nullable Path relativeTo);

    default J.CompilationUnit parse(String source, String whichDependsOn) {
        return parse(source, singletonList(whichDependsOn));
    }

    default J.CompilationUnit parse(String source, List<String> whichDependOn) {
        return parse(source, whichDependOn.toArray(new String[0]));
    }

    default List<J.CompilationUnit> parse(List<Path> sourceFiles) {
        return parse(sourceFiles, null);
    }

    default J.CompilationUnit parse(String source, String... whichDependOn) {
        try {
            Path temp = Files.createTempDirectory("sources");

            Pattern classPattern = Pattern.compile("(class|interface|enum)\\s*(<[^>]*>)?\\s+(\\w+)");

            Function<String, String> simpleName = sourceStr -> {
                Matcher classMatcher = classPattern.matcher(sourceStr);
                return classMatcher.find() ? classMatcher.group(3) : null;
            };

            Function<String, Path> sourceFile = sourceText -> {
                Path file = temp.resolve(simpleName.apply(sourceText) + ".java");
                try {
                    Files.write(file, sourceText.getBytes(Charset.defaultCharset()));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return file;
            };

            try {
                List<J.CompilationUnit> cus = parse(Stream.concat(
                        Arrays.stream(whichDependOn).map(sourceFile),
                        Stream.of(sourceFile.apply(source))
                ).collect(toList()));

                return cus.get(cus.size() - 1);
            } finally {
                // delete temp recursively
                //noinspection ResultOfMethodCallIgnored
                Files.walk(temp)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Clear any in-memory parser caches that may prevent reparsing of classes with the same fully qualified name in
     * different rounds
     */
    JavaParser reset();

    @SuppressWarnings("unchecked")
    abstract class Builder<P extends JavaParser, B extends Builder<P, B>> {
        @Nullable
        protected List<Path> classpath;

        protected Charset charset = Charset.defaultCharset();
        protected boolean relaxedClassTypeMatching = false;
        protected MeterRegistry meterRegistry = Metrics.globalRegistry;
        protected boolean logCompilationWarningsAndErrors = true;

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

        public B classpath(List<Path> classpath) {
            this.classpath = classpath;
            return (B) this;
        }

        public abstract P build();
    }
}
