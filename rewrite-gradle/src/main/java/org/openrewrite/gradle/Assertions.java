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
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.toolingapi.OpenRewriteModel;
import org.openrewrite.gradle.toolingapi.OpenRewriteModelBuilder;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.internal.ThrowingUnaryOperator;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class Assertions {

    private Assertions() {
    }

    private static final Parser.Builder gradleParser = GradleParser.builder()
            .setGroovyParser(GroovyParser.builder().logCompilationWarningsAndErrors(true));

    public static SourceSpecs buildGradle(@Language("groovy") @Nullable String before) {
        return buildGradle(before, s -> {
        });
    }

    public static SourceSpecs buildGradle(@Language("groovy") @Nullable String before, Consumer<SourceSpec<G.CompilationUnit>> spec) {
        SourceSpec<G.CompilationUnit> gradle = new SourceSpec<>(G.CompilationUnit.class, "gradle", gradleParser, before, null);
        gradle.path(Paths.get("build.gradle"));
        acceptSpec(spec, gradle);
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
        acceptSpec(spec, gradle);
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

    private static void acceptSpec(Consumer<SourceSpec<G.CompilationUnit>> spec, SourceSpec<G.CompilationUnit> buildGradle) {
        ThrowingUnaryOperator<G.CompilationUnit> userSuppliedBeforeRecipe = buildGradle.getBeforeRecipe();
        buildGradle.mapBeforeRecipe(cu -> {
            G.CompilationUnit c = userSuppliedBeforeRecipe.apply(cu);
            try {
                Path projectDir = Files.createTempDirectory("project");
                Path buildGradleOnDisk = projectDir.resolve("build.gradle");
                try {
                    Files.write(buildGradleOnDisk, c.printAllAsBytes());
                    OpenRewriteModel model = OpenRewriteModelBuilder.forProjectDirectory(projectDir.toFile());
                    return c.withMarkers(c.getMarkers().add(GradleProject.fromToolingModel(model.gradleProject())));
                } finally {
                    deleteDirectory(projectDir.toFile());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        spec.accept(buildGradle);
    }

    private static void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        directoryToBeDeleted.delete();
    }
}
