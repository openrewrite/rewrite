/*
 * Copyright 2022 the original author or authors.
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

import org.intellij.lang.annotations.Language;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.UncheckedConsumer;

import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

public class Assertions {

    private Assertions() {
    }

    private static final Parser.Builder gradleParser = GradleParser.builder()
            .groovyParser(GroovyParser.builder().logCompilationWarningsAndErrors(true));

    /**
     * @param version      The Gradle version to use.
     * @param distribution The Gradle distribution to use.
     * @return An exception now that the method has moved.
     * @deprecated This method has moved to org.openrewrite.gradle.toolingapi.Assertions. This
     * has allowed us to remove the compile time dependency on the tooling model and thus save a lot
     * of space in the distribution of rewrite-gradle recipes.
     */
    @Deprecated
    public static UncheckedConsumer<List<SourceFile>> withToolingApi(@Nullable String version, @Nullable String distribution) {
        throw new UnsupportedOperationException("This method has moved to org.openrewrite.gradle.toolingapi.Assertions. " +
                                                "Add a dependency on org.openrewrite.gradle.tooling:model to continue using it.");
    }

    /**
     * @param version The Gradle version to use.
     * @return An exception now that the method has moved.
     * @deprecated This method has moved to org.openrewrite.gradle.toolingapi.Assertions. This
     * has allowed us to remove the compile time dependency on the tooling model and thus save a lot
     * of space in the distribution of rewrite-gradle recipes.
     */
    @Deprecated
    public static UncheckedConsumer<List<SourceFile>> withToolingApi(String version) {
        throw new UnsupportedOperationException("This method has moved to org.openrewrite.gradle.toolingapi.Assertions. " +
                                                "Add a dependency on org.openrewrite.gradle.tooling:model to continue using it.");
    }

    /**
     * @return An exception now that the method has moved.
     * @deprecated This method has moved to org.openrewrite.gradle.toolingapi.Assertions. This
     * has allowed us to remove the compile time dependency on the tooling model and thus save a lot
     * of space in the distribution of rewrite-gradle recipes.
     */
    @Deprecated
    public static UncheckedConsumer<List<SourceFile>> withToolingApi() {
        throw new UnsupportedOperationException("This method has moved to org.openrewrite.gradle.toolingapi.Assertions. " +
                                                "Add a dependency on org.openrewrite.gradle.tooling:model to continue using it.");
    }

    public static SourceSpecs buildGradle(@Language("groovy") @Nullable String before) {
        return buildGradle(before, s -> {
        });
    }

    public static SourceSpecs buildGradle(@Language("groovy") @Nullable String before, Consumer<SourceSpec<G.CompilationUnit>> spec) {
        SourceSpec<G.CompilationUnit> gradle = new SourceSpec<>(G.CompilationUnit.class, "gradle", gradleParser, before, null);
        gradle.path(Paths.get("build.gradle"));
        spec.accept(gradle);
        return gradle;
    }

    public static SourceSpecs buildGradle(@Language("groovy") @Nullable String before, @Language("groovy") @Nullable String after) {
        return buildGradle(before, after, s -> {
        });
    }

    public static SourceSpecs buildGradle(@Language("groovy") @Nullable String before, @Language("groovy") @Nullable String after,
                                          Consumer<SourceSpec<G.CompilationUnit>> spec) {
        SourceSpec<G.CompilationUnit> gradle = new SourceSpec<>(G.CompilationUnit.class, "gradle", gradleParser, before, s -> after);
        gradle.path("build.gradle");
        spec.accept(gradle);
        return gradle;
    }

    public static SourceSpecs settingsGradle(@Language("groovy") @Nullable String before) {
        return settingsGradle(before, s -> {
        });
    }

    public static SourceSpecs settingsGradle(@Language("groovy") @Nullable String before, Consumer<SourceSpec<G.CompilationUnit>> spec) {
        SourceSpec<G.CompilationUnit> gradle = new SourceSpec<>(G.CompilationUnit.class, "gradle", gradleParser, before, null);
        gradle.path(Paths.get("settings.gradle"));
        spec.accept(gradle);
        return gradle;
    }

    public static SourceSpecs settingsGradle(@Language("groovy") @Nullable String before, @Language("groovy") @Nullable String after) {
        return settingsGradle(before, after, s -> {
        });
    }

    public static SourceSpecs settingsGradle(@Language("groovy") @Nullable String before, @Language("groovy") @Nullable String after,
                                             Consumer<SourceSpec<G.CompilationUnit>> spec) {
        SourceSpec<G.CompilationUnit> gradle = new SourceSpec<>(G.CompilationUnit.class, "gradle", gradleParser, before, s -> after);
        gradle.path("settings.gradle");
        spec.accept(gradle);
        return gradle;
    }
}
