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

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
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

public class GradleParser implements Parser<G.CompilationUnit> {
    private final GroovyParser buildParser;
    private final GroovyParser settingsParser;

    @Deprecated//(since = "7.37.0", forRemoval = true)
    public GradleParser(GroovyParser.Builder groovyParser) {
        GroovyParser.Builder base = groovyParser;
        this.buildParser = GroovyParser.builder(base)
                .compilerCustomizers(
                        new DefaultImportsCustomizer(),
                        config -> config.setScriptBaseClass("RewriteGradleProject")
                )
                .build();
        this.settingsParser = GroovyParser.builder(base)
                .compilerCustomizers(
                        new DefaultImportsCustomizer(),
                        config -> config.setScriptBaseClass("RewriteSettings")
                )
                .build();
    }

    private GradleParser(Builder builder) {
        GroovyParser.Builder base = builder.groovyParser;
        this.buildParser = GroovyParser.builder(base)
                .classpath(builder.buildscriptClasspath)
                .compilerCustomizers(
                        new DefaultImportsCustomizer(),
                        config -> config.setScriptBaseClass("RewriteGradleProject")
                )
                .build();
        this.settingsParser = GroovyParser.builder(base)
                .classpath(builder.settingsClasspath)
                .compilerCustomizers(
                        new DefaultImportsCustomizer(),
                        config -> config.setScriptBaseClass("RewriteSettings")
                )
                .build();
    }

    @Override
    public Stream<G.CompilationUnit> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
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
        private Collection<Path> buildscriptClasspath = loadDefaultClasspath();

        @Nullable
        private Collection<Path> settingsClasspath = loadDefaultClasspath();

        public Builder() {
            super(G.CompilationUnit.class);
        }

        public Builder groovyParser(GroovyParser.Builder groovyParser) {
            this.groovyParser = groovyParser;
            return this;
        }

        /**
         * @deprecated Use {@code groovyParser(GroovyParser.Builder)} instead.
         */
        @Deprecated//(since = "7.37.0", forRemoval = true)
        public Builder setGroovyParser(GroovyParser.Builder groovyParser) {
            return groovyParser(groovyParser);
        }

        @Deprecated//(since = "7.37.0", forRemoval = true)
        public GroovyParser.Builder getGroovyParser() {
            return groovyParser;
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
}
