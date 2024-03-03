/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.gradle;

import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.internal.DefaultImportsCustomizer;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
public class GradleParser implements Parser {
    private final GradleParser.Builder base;

    private Collection<Path> defaultClasspath;
    private GroovyParser buildParser;
    private GroovyParser settingsParser;

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        if (buildParser == null) {
            Collection<Path> buildscriptClasspath = base.buildscriptClasspath;
            if (buildscriptClasspath == null) {
                if (defaultClasspath == null) {
                    defaultClasspath = loadDefaultClasspath();
                }
                buildscriptClasspath = defaultClasspath;
            }
            buildParser = GroovyParser.builder(base.groovyParser)
                    .classpath(buildscriptClasspath)
                    .compilerCustomizers(
                            new DefaultImportsCustomizer(),
                            config -> config.setScriptBaseClass("RewriteGradleProject")
                    )
                    .build();
        }
        if (settingsParser == null) {
            Collection<Path> settingsClasspath = base.settingsClasspath;
            if (settingsClasspath == null) {
                if (defaultClasspath == null) {
                    defaultClasspath = loadDefaultClasspath();
                }
                settingsClasspath = defaultClasspath;
            }
            settingsParser = GroovyParser.builder(base.groovyParser)
                    .classpath(settingsClasspath)
                    .compilerCustomizers(
                            new DefaultImportsCustomizer(),
                            config -> config.setScriptBaseClass("RewriteSettings")
                    )
                    .build();
        }

        return StreamSupport.stream(sources.spliterator(), false)
                .flatMap(source -> {
                    if (source.getPath().endsWith("settings.gradle")) {
                        return settingsParser.parseInputs(Collections.singletonList(source), relativeTo, ctx);
                    }
                    return buildParser.parseInputs(Collections.singletonList(source), relativeTo, ctx);
                });
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().endsWith(".gradle");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("build.gradle");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {
        protected GroovyParser.Builder groovyParser = GroovyParser.builder();

        @Nullable
        private Collection<Path> buildscriptClasspath;

        @Nullable
        private Collection<Path> settingsClasspath;

        public Builder() {
            super(G.CompilationUnit.class);
        }

        public Builder groovyParser(GroovyParser.Builder groovyParser) {
            this.groovyParser = groovyParser;
            return this;
        }

        public Builder buildscriptClasspath(Collection<Path> classpath) {
            this.buildscriptClasspath = classpath;
            return this;
        }

        public Builder buildscriptClasspath(String... classpath) {
            this.buildscriptClasspath = JavaParser.dependenciesFromClasspath(classpath);
            return this;
        }

        public Builder buildscriptClasspathFromResources(ExecutionContext ctx, String... artifactNamesWithVersions) {
            this.buildscriptClasspath = JavaParser.dependenciesFromResources(ctx, artifactNamesWithVersions);
            return this;
        }

        public Builder settingsClasspath(Collection<Path> classpath) {
            this.settingsClasspath = classpath;
            return this;
        }

        public Builder settingsClasspath(String... classpath) {
            this.settingsClasspath = JavaParser.dependenciesFromClasspath(classpath);
            return this;
        }

        public Builder settingsClasspathFromResources(ExecutionContext ctx, String... artifactNamesWithVersions) {
            this.settingsClasspath = JavaParser.dependenciesFromResources(ctx, artifactNamesWithVersions);
            return this;
        }

        public GradleParser build() {
            return new GradleParser(this);
        }

        @Override
        public String getDslName() {
            return "gradle";
        }
    }

    private static List<Path> loadDefaultClasspath() {
        try {
            Class.forName("org.gradle.api.Project");
            return JavaParser.runtimeClasspath();
        } catch (ClassNotFoundException e) {
            return JavaParser.dependenciesFromResources(new InMemoryExecutionContext(),
                    "gradle-base-services",
                    "gradle-core-api",
                    "gradle-language-groovy",
                    "gradle-language-java",
                    "gradle-logging",
                    "gradle-messaging",
                    "gradle-native",
                    "gradle-process-services",
                    "gradle-resources",
                    "gradle-testing-base",
                    "gradle-testing-jvm",
                    "gradle-enterprise-gradle-plugin");
        }
    }
}
