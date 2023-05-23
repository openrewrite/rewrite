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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.toolingapi.OpenRewriteModel;
import org.openrewrite.gradle.toolingapi.OpenRewriteModelBuilder;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.OperatingSystemProvenance;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.UncheckedConsumer;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class Assertions {

    private Assertions() {
    }

    private static final Parser.Builder gradleParser = GradleParser.builder()
            .groovyParser(GroovyParser.builder().logCompilationWarningsAndErrors(true));

    public static UncheckedConsumer<List<SourceFile>> withToolingApi(@Nullable String version, @Nullable String distribution) {
        return sourceFiles -> {
            try {
                Path tempDirectory = Files.createTempDirectory("project");
                // Usage of Assertions.mavenProject() might result in gradle files inside a subdirectory
                Path projectDir = tempDirectory;
                try {
                    for (SourceFile sourceFile : sourceFiles) {
                        if (sourceFile instanceof G.CompilationUnit) {
                            G.CompilationUnit g = (G.CompilationUnit) sourceFile;
                            if (g.getSourcePath().toString().endsWith(".gradle")) {
                                Path groovyGradle = tempDirectory.resolve(g.getSourcePath());
                                projectDir = groovyGradle.getParent();
                                Files.createDirectories(groovyGradle.getParent());
                                Files.write(groovyGradle, g.printAllAsBytes());
                            }
                        } else if (sourceFile instanceof Properties.File) {
                            Properties.File f = (Properties.File) sourceFile;
                            if (f.getSourcePath().endsWith("gradle.properties")) {
                                Path gradleProperties = tempDirectory.resolve(f.getSourcePath());
                                projectDir = gradleProperties.getParent();
                                Files.createDirectories(gradleProperties.getParent());
                                Files.write(gradleProperties, f.printAllAsBytes());
                            }
                        }
                    }

                    if (version != null) {
                        GradleWrapper gradleWrapper = requireNonNull(GradleWrapper.validate(new InMemoryExecutionContext(), version, distribution, null, null).getValue());
                        Files.createDirectories(projectDir.resolve("gradle/wrapper/"));
                        Files.write(projectDir.resolve(GradleWrapper.WRAPPER_PROPERTIES_LOCATION), ("distributionBase=GRADLE_USER_HOME\n" +
                                "distributionPath=wrapper/dists\n" +
                                "distributionUrl=" + gradleWrapper.getPropertiesFormattedUrl() + "\n" +
                                "distributionSha256Sum=" + gradleWrapper.getDistributionChecksum().getHexValue() + "\n" +
                                "zipStoreBase=GRADLE_USER_HOME\n" +
                                "zipStorePath=wrapper/dists").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
                        Files.write(projectDir.resolve(GradleWrapper.WRAPPER_JAR_LOCATION), gradleWrapper.asRemote().printAllAsBytes(), StandardOpenOption.CREATE_NEW);
                        Path gradleSh = projectDir.resolve(GradleWrapper.WRAPPER_SCRIPT_LOCATION);
                        Files.copy(requireNonNull(UpdateGradleWrapper.class.getResourceAsStream("/gradlew")), gradleSh);
                        OperatingSystemProvenance current = OperatingSystemProvenance.current();
                        if (current.isLinux() || current.isMacOsX()) {
                            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(gradleSh);
                            permissions.add(PosixFilePermission.OWNER_EXECUTE);
                            Files.setPosixFilePermissions(gradleSh, permissions);
                        }
                        Files.copy(requireNonNull(UpdateGradleWrapper.class.getResourceAsStream("/gradlew.bat")), projectDir.resolve(GradleWrapper.WRAPPER_BATCH_LOCATION));
                    }

                    OpenRewriteModel model = OpenRewriteModelBuilder.forProjectDirectory(projectDir.toFile());
                    GradleProject gradleProject = GradleProject.fromToolingModel(model.gradleProject());
                    for (int i = 0; i < sourceFiles.size(); i++) {
                        SourceFile sourceFile = sourceFiles.get(i);
                        if (sourceFile.getSourcePath().toString().endsWith(".gradle")) {
                            sourceFiles.set(i, sourceFile.withMarkers(sourceFile.getMarkers().add(gradleProject)));
                            break;
                        }
                    }
                } finally {
                    deleteDirectory(tempDirectory.toFile());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    public static UncheckedConsumer<List<SourceFile>> withToolingApi(String version) {
        return withToolingApi(version, "bin");
    }

    public static UncheckedConsumer<List<SourceFile>> withToolingApi() {
        return withToolingApi(null, null);
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
