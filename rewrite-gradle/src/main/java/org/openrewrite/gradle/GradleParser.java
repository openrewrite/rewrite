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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.internal.DefaultImportsCustomizer;
import org.openrewrite.gradle.internal.GradleDelegateTypeCheckingExtension;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.internal.JavaTypeFactory;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.tree.ParseError;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.singletonList;

@RequiredArgsConstructor
public class GradleParser implements Parser {
    /**
     * The Android Gradle Plugin is only on the classpath when the build supplies it, so its accessor can't be compiled
     * into the type table with the others.
     */
    @SuppressWarnings("LanguageMismatch")
    private static final String KTS_BUILD_STUBS =
            "package org.gradle.api\n" +
            "fun Project.android(block: com.android.build.api.dsl.ApplicationExtension.() -> Unit) {}\n";

    private static final String[] KTS_DEFAULT_IMPORTS = Stream.concat(
            Arrays.stream(DefaultImportsCustomizer.DEFAULT_IMPORTS),
            Stream.of("org.gradle.kotlin.dsl")
    ).toArray(String[]::new);

    /**
     * Gradle compiles its Kotlin scripts so that a lambda passed as an {@code Action} receives its argument as {@code this}.
     */
    private static final String HAS_IMPLICIT_RECEIVER = "org.gradle.api.HasImplicitReceiver";

    private final GradleParser.Builder base;

    private @Nullable List<Path> defaultClasspath;
    private @Nullable GroovyParser groovyBuildParser;
    private @Nullable GroovyParser groovySettingsParser;
    private @Nullable KotlinParser kotlinBuildParser;
    private @Nullable KotlinParser kotlinSettingsParser;

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        if (groovyBuildParser == null) {
            Collection<Path> buildscriptClasspath = base.buildscriptClasspath;
            if (buildscriptClasspath == null) {
                buildscriptClasspath = defaultClasspath(relativeTo, ctx);
            }
            groovyBuildParser = GroovyParser.builder(base.groovyParser)
                    .classpath(buildscriptClasspath)
                    .compilerCustomizers(
                            new DefaultImportsCustomizer(),
                            config -> config.setScriptBaseClass("RewriteGradleProject")
                    )
                    .typeCheckingExtensions(GradleDelegateTypeCheckingExtension::new)
                    .build();
        }
        if (kotlinBuildParser == null) {
            Collection<Path> buildscriptClasspath = base.buildscriptClasspath;
            if (buildscriptClasspath == null) {
                buildscriptClasspath = defaultClasspath(relativeTo, ctx);
            }
            kotlinBuildParser = KotlinParser.builder(base.kotlinParser)
                    .classpath(withKotlinDslAccessors(buildscriptClasspath, ctx))
                    .dependsOn(KTS_BUILD_STUBS)
                    .isKotlinScript(true)
                    .scriptImplicitReceivers("org.gradle.api.Project")
                    .scriptDefaultImports(KTS_DEFAULT_IMPORTS)
                    .samWithReceiverAnnotations(HAS_IMPLICIT_RECEIVER)
                    .build();
        }
        if (groovySettingsParser == null) {
            Collection<Path> settingsClasspath = base.settingsClasspath;
            if (settingsClasspath == null) {
                settingsClasspath = defaultClasspath(relativeTo, ctx);
            }
            groovySettingsParser = GroovyParser.builder(base.groovyParser)
                    .classpath(settingsClasspath)
                    .compilerCustomizers(
                            new DefaultImportsCustomizer(),
                            config -> config.setScriptBaseClass("RewriteSettings")
                    )
                    .typeCheckingExtensions(GradleDelegateTypeCheckingExtension::new)
                    .build();
        }
        if (kotlinSettingsParser == null) {
            Collection<Path> settingsClasspath = base.settingsClasspath;
            if (settingsClasspath == null) {
                settingsClasspath = defaultClasspath(relativeTo, ctx);
            }
            kotlinSettingsParser = KotlinParser.builder(base.kotlinParser)
                    .classpath(withKotlinDslAccessors(settingsClasspath, ctx))
                    .isKotlinScript(true)
                    .scriptImplicitReceivers("org.gradle.api.initialization.Settings")
                    .scriptDefaultImports(KTS_DEFAULT_IMPORTS)
                    .samWithReceiverAnnotations(HAS_IMPLICIT_RECEIVER)
                    .build();
        }

        return StreamSupport.stream(sources.spliterator(), false)
                .flatMap(source -> {
                    Path sourcePath = source.getPath();
                    if (sourcePath.endsWith("settings.gradle.kts")) {
                        return kotlinSettingsParser.parseInputs(singletonList(source), relativeTo, ctx);
                    } else if (sourcePath.endsWith("settings.gradle")) {
                        return groovySettingsParser.parseInputs(singletonList(source), relativeTo, ctx);
                    } else if (sourcePath.toString().endsWith(".gradle.kts")) {
                        return kotlinBuildParser.parseInputs(singletonList(source), relativeTo, ctx);
                    }
                    return groovyBuildParser.parseInputs(singletonList(source), relativeTo, ctx);
                });
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().endsWith(".gradle") || path.toString().endsWith(".gradle.kts");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("build.gradle");
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * A mapping {@link Function} for use in a {@code stream.map(...)} / {@code optional.map(...)} chain over the
     * result of parsing a generated Gradle code snippet, in place of {@code SomeType.class::cast}.
     * <p>
     * Recipes parse constant, valid Gradle snippets, so a {@link ParseError} here means the underlying Groovy or
     * Kotlin parser threw while parsing the snippet. Rethrow that real cause — its
     * {@link org.openrewrite.ParseExceptionResult} carries the full stack trace — instead of letting the cast mask
     * it with an opaque {@link ClassCastException} ("Cannot cast ParseError to ...").
     *
     * @param expected the {@link SourceFile} subtype the snippet is expected to parse into
     * @return a function that casts to {@code expected}, or throws with the underlying parse failure as its cause
     */
    public static <S extends SourceFile> Function<SourceFile, S> requireParsed(Class<S> expected) {
        return sourceFile -> {
            if (sourceFile instanceof ParseError) {
                throw new IllegalStateException(
                        "Failed to parse generated Gradle code as " + expected.getSimpleName(),
                        ((ParseError) sourceFile).toException());
            }
            return expected.cast(sourceFile);
        };
    }

    public static class Builder extends Parser.Builder {
        protected GroovyParser.Builder groovyParser = GroovyParser.builder();
        protected KotlinParser.Builder kotlinParser = KotlinParser.builder();

        @Nullable
        private Collection<Path> buildscriptClasspath;

        @Nullable
        private Collection<Path> settingsClasspath;

        @Nullable
        private String gradleVersion;

        public Builder() {
            super(G.CompilationUnit.class);
        }

        public Builder groovyParser(GroovyParser.Builder groovyParser) {
            this.groovyParser = groovyParser;
            return this;
        }

        public Builder kotlinParser(KotlinParser.Builder kotlinParser) {
            this.kotlinParser = kotlinParser;
            return this;
        }

        /**
         * Forward a {@link JavaTypeFactory} to both the Groovy and Kotlin sub-parsers
         * so either DSL variant produces types from the caller's factory.
         */
        public Builder typeFactory(JavaTypeFactory typeFactory) {
            this.groovyParser.typeFactory(typeFactory);
            this.kotlinParser.typeFactory(typeFactory);
            return this;
        }

        /**
         * The Gradle version whose API types the scripts are attributed with, when no classpath is given.
         * Only the major version matters. Without one, the version of the Gradle wrapper in the directory the sources
         * are parsed relative to is used, else the newest major version.
         */
        public Builder gradleVersion(@Nullable String gradleVersion) {
            this.gradleVersion = gradleVersion;
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

        @Override
        public GradleParser build() {
            return new GradleParser(this);
        }

        @Override
        public String getDslName() {
            return "gradle";
        }

        @Override
        public Builder clone() {
            Builder clone = (Builder) super.clone();
            clone.groovyParser = this.groovyParser.clone();
            clone.kotlinParser = this.kotlinParser.clone();
            return clone;
        }
    }

    private List<Path> defaultClasspath(@Nullable Path relativeTo, ExecutionContext ctx) {
        if (defaultClasspath == null) {
            String gradleVersion = base.gradleVersion != null ? base.gradleVersion : wrapperGradleVersion(relativeTo);
            defaultClasspath = OwnTypeTable.load(ctx,
                    "gradle-public-api-" + gradleApiMajor(gradleVersion) + ".",
                    "develocity-gradle-plugin");
        }
        return defaultClasspath;
    }

    /**
     * Adds the accessors Gradle generates for each build, which {@code addGradleApiToTypeTable} in build.gradle.kts
     * compiles into the type table.
     */
    private static List<Path> withKotlinDslAccessors(Collection<Path> classpath, ExecutionContext ctx) {
        List<Path> withAccessors = new ArrayList<>(classpath);
        withAccessors.addAll(OwnTypeTable.load(ctx, "gradle-kotlin-dsl-accessors"));
        return withAccessors;
    }

    /**
     * Calls {@link JavaParser#dependenciesFromResources}, which reads the type tables of its caller's class loader and
     * skips {@link GradleParser} when it looks for the caller. From here it reads this jar's table, which matches the
     * artifact names this parser asks for. Whatever calls the parser may come from another jar, as {@link Parser#parse}
     * comes from the host process under a recipe class loader.
     */
    private static final class OwnTypeTable {
        static List<Path> load(ExecutionContext ctx, String... artifactNames) {
            return JavaParser.dependenciesFromResources(ctx, artifactNames);
        }
    }

    static @Nullable String wrapperGradleVersion(@Nullable Path projectDir) {
        if (projectDir == null) {
            return null;
        }
        Path wrapper = projectDir.resolve(GradleWrapper.WRAPPER_PROPERTIES_LOCATION_RELATIVE_PATH);
        if (!Files.isRegularFile(wrapper)) {
            return null;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(wrapper)) {
            properties.load(in);
        } catch (IOException | IllegalArgumentException e) {
            // Properties.load throws IllegalArgumentException on a malformed unicode escape
            return null;
        }
        return GradleWrapper.versionFromDistributionUrl(properties.getProperty("distributionUrl", ""));
    }

    /**
     * The majors of {@code gradleApiMajors} in build.gradle.kts, whose API the type table holds.
     */
    static final int[] GRADLE_API_MAJORS = {6, 7, 8, 9};

    static int gradleApiMajor(@Nullable String gradleVersion) {
        int newest = GRADLE_API_MAJORS[GRADLE_API_MAJORS.length - 1];
        if (gradleVersion == null) {
            return newest;
        }
        int major;
        try {
            major = Integer.parseInt(gradleVersion.split("\\.", 2)[0]);
        } catch (NumberFormatException e) {
            return newest;
        }
        return Math.max(GRADLE_API_MAJORS[0], Math.min(newest, major));
    }
}
