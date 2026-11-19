/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.internal.parser;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaParserExecutionContextView;

import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaParserCallerTest {

    @Test
    void parsersAndBuildersReadTheTypeTablesOfTheirCaller(@TempDir Path tempDir) throws Exception {
        Path recipeJar = tempDir.resolve("recipe");
        Path typeTable = recipeJar.resolve("META-INF/rewrite/classpath.tsv.gz");
        Files.createDirectories(typeTable.getParent());
        try (TypeTable.Writer writer = TypeTable.newWriter(Files.newOutputStream(typeTable))) {
            writer.jar("com.example", "recipe-only", "1.0").write(codeSource(Nullable.class));
        }
        Path recipeSource = Files.writeString(tempDir.resolve("Recipe.java"),
          //language=java
          """
            package com.example;

            import org.openrewrite.ExecutionContext;
            import org.openrewrite.java.internal.parser.JavaParserCallerTest.HostParser;
            import org.openrewrite.java.internal.parser.JavaParserCallerTest.HostParserBuilder;

            import java.nio.file.Path;
            import java.util.List;
            import java.util.function.Function;

            public class Recipe implements Function<ExecutionContext, List<List<Path>>> {
                @Override
                public List<List<Path>> apply(ExecutionContext ctx) {
                    HostParser parser = new HostParser();
                    parser.parse(ctx, "");
                    return List.of(new HostParserBuilder().classpathFromResources(ctx, "recipe-only"), parser.classpath);
                }
            }
            """);
        String classpath = codeSource(Parser.class) + File.pathSeparator + codeSource(HostParser.class);
        assertThat(ToolProvider.getSystemJavaCompiler()
          .run(null, null, null, "-cp", classpath, "-d", recipeJar.toString(), recipeSource.toString()))
          .isZero();

        ExecutionContext ctx = new InMemoryExecutionContext();
        JavaParserExecutionContextView.view(ctx).setParserClasspathDownloadTarget(tempDir.resolve("cache").toFile());
        try (URLClassLoader recipeLoader = new URLClassLoader(new URL[]{recipeJar.toUri().toURL()}, getClass().getClassLoader())) {
            @SuppressWarnings("unchecked")
            Function<ExecutionContext, List<List<Path>>> recipe = (Function<ExecutionContext, List<List<Path>>>) recipeLoader
              .loadClass("com.example.Recipe").getConstructor().newInstance();
            List<List<Path>> classpaths = recipe.apply(ctx);

            assertThat(classpaths.get(0)).as("asked of the parser builder")
              .singleElement()
              .extracting(jar -> jar.getFileName().toString())
              .isEqualTo("recipe-only-1.0.jar");

            assertThat(classpaths.get(1)).as("read by the parser while parsing")
              .singleElement()
              .extracting(jar -> jar.getFileName().toString())
              .isEqualTo("recipe-only-1.0.jar");
        }
    }

    private static Path codeSource(Class<?> type) throws Exception {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    public static class HostParser implements Parser {
        public List<Path> classpath = emptyList();

        @Override
        public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
            classpath = JavaParser.dependenciesFromResources(ctx, "recipe-only");
            return Stream.empty();
        }

        @Override
        public boolean accept(Path path) {
            return true;
        }

        @Override
        public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
            return prefix;
        }
    }

    public static class HostParserBuilder extends Parser.Builder {
        public HostParserBuilder() {
            super(SourceFile.class);
        }

        public List<Path> classpathFromResources(ExecutionContext ctx, String... artifactNames) {
            return JavaParser.dependenciesFromResources(ctx, artifactNames);
        }

        @Override
        public Parser build() {
            return new HostParser();
        }

        @Override
        public String getDslName() {
            return "host";
        }
    }
}
