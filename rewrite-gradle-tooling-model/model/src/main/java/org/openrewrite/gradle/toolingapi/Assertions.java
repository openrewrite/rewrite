/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.gradle.toolingapi;

import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.UpdateGradleWrapper;
import org.openrewrite.gradle.marker.GradleBuildscript;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.OperatingSystemProvenance;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.test.UncheckedConsumer;
import org.openrewrite.text.PlainText;
import org.openrewrite.toml.tree.Toml;
import org.opentest4j.TestAbortedException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;

public class Assertions {

    public static UncheckedConsumer<List<SourceFile>> withToolingApi(URI distributionUrl) {
        return withToolingApi(GradleWrapper.create(distributionUrl, new InMemoryExecutionContext()));
    }

    @SuppressWarnings("unused")
    public static UncheckedConsumer<List<SourceFile>> withToolingApi(URI distributionUrl, String initScriptContents) {
        return withToolingApi(GradleWrapper.create(distributionUrl, new InMemoryExecutionContext()), initScriptContents);
    }

    public static UncheckedConsumer<List<SourceFile>> withToolingApi(@Nullable GradleWrapper gradleWrapper) {
        return withToolingApi(gradleWrapper, null);
    }

    public static UncheckedConsumer<List<SourceFile>> withToolingApi(@Nullable GradleWrapper gradleWrapper, @Nullable String initScriptContents) {
        return sourceFiles -> {
            try {
                Path tempDirectory = Files.createTempDirectory("project");
                // Usage of Assertions.mavenProject() might result in gradle files inside a subdirectory
                Path projectDir = tempDirectory;
                try {
                    for (SourceFile sourceFile : sourceFiles) {
                        if (sourceFile instanceof K.CompilationUnit) {
                            K.CompilationUnit k = (K.CompilationUnit) sourceFile;
                            if (k.getSourcePath().toString().endsWith(".gradle.kts")) {
                                Path kotlinGradle = tempDirectory.resolve(k.getSourcePath());
                                if (!tempDirectory.equals(kotlinGradle.getParent()) && tempDirectory.equals(kotlinGradle.getParent().getParent())) {
                                    projectDir = kotlinGradle.getParent();
                                }
                                Files.createDirectories(kotlinGradle.getParent());
                                Files.write(kotlinGradle, k.printAllAsBytes());
                            }
                        } else if (sourceFile instanceof G.CompilationUnit) {
                            G.CompilationUnit g = (G.CompilationUnit) sourceFile;
                            if (g.getSourcePath().toString().endsWith(".gradle")) {
                                Path groovyGradle = tempDirectory.resolve(g.getSourcePath());
                                if (!tempDirectory.equals(groovyGradle.getParent()) && tempDirectory.equals(groovyGradle.getParent().getParent())) {
                                    projectDir = groovyGradle.getParent();
                                }
                                Files.createDirectories(groovyGradle.getParent());
                                Files.write(groovyGradle, g.printAllAsBytes());
                            }
                        } else if (sourceFile instanceof Properties.File) {
                            Properties.File f = (Properties.File) sourceFile;
                            if (f.getSourcePath().endsWith("gradle.properties")) {
                                Path gradleProperties = tempDirectory.resolve(f.getSourcePath());
                                if (!tempDirectory.equals(gradleProperties.getParent()) && tempDirectory.equals(gradleProperties.getParent().getParent())) {
                                    projectDir = gradleProperties.getParent();
                                }
                                Files.createDirectories(gradleProperties.getParent());
                                Files.write(gradleProperties, f.printAllAsBytes());
                            }
                        } else if (sourceFile instanceof Toml.Document) {
                            Toml.Document d = (Toml.Document) sourceFile;
                            if (d.getSourcePath().startsWith("gradle/") && d.getSourcePath().toString().endsWith(".versions.toml")) {
                                Path versionCatalog = tempDirectory.resolve(d.getSourcePath());
                                if (!tempDirectory.equals(versionCatalog.getParent()) && tempDirectory.equals(versionCatalog.getParent().getParent())) {
                                    projectDir = versionCatalog.getParent();
                                }
                                Files.createDirectories(versionCatalog.getParent());
                                Files.write(versionCatalog, d.printAllAsBytes());
                            }
                        } else if (sourceFile instanceof PlainText) {
                            PlainText plainText = (PlainText) sourceFile;
                            if (plainText.getSourcePath().endsWith("gradle.lockfile") || plainText.getSourcePath().endsWith("buildscript-gradle.lockfile")) {
                                Path lockfile = tempDirectory.resolve(plainText.getSourcePath());
                                if (!tempDirectory.equals(lockfile.getParent()) && tempDirectory.equals(lockfile.getParent().getParent())) {
                                    projectDir = lockfile.getParent();
                                }
                                Files.createDirectories(lockfile.getParent());
                                Files.write(lockfile, plainText.printAllAsBytes());
                            }
                        }
                    }

                    if (gradleWrapper != null) {
                        Files.createDirectories(projectDir.resolve("gradle/wrapper/"));
                        Files.write(projectDir.resolve(GradleWrapper.WRAPPER_PROPERTIES_LOCATION),
                                ("distributionBase=GRADLE_USER_HOME\n" +
                                 "distributionPath=wrapper/dists\n" +
                                 "distributionUrl=" + gradleWrapper.getPropertiesFormattedUrl() + "\n" +
                                 ((gradleWrapper.getDistributionChecksum() == null) ? "" : "distributionSha256Sum=" + gradleWrapper.getDistributionChecksum().getHexValue() + "\n") +
                                 "zipStoreBase=GRADLE_USER_HOME\n" +
                                 "zipStorePath=wrapper/dists").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
                        Files.write(projectDir.resolve(GradleWrapper.WRAPPER_JAR_LOCATION), gradleWrapper.wrapperJar().printAllAsBytes(), StandardOpenOption.CREATE_NEW);
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

                    Set<org.openrewrite.maven.tree.MavenRepository> allRepositories = new LinkedHashSet<>();
                    Set<org.openrewrite.maven.tree.MavenRepository> allBuildscriptRepositories = new LinkedHashSet<>();
                    boolean freestandingScriptFound = false;
                    Map<String, GradleProject> gradleProjects = new HashMap<>();
                    for (int i = 0; i < sourceFiles.size(); i++) {
                        SourceFile sourceFile = sourceFiles.get(i);
                        if (sourceFile.getSourcePath().endsWith("settings.gradle") || sourceFile.getSourcePath().endsWith("settings.gradle.kts")) {
                            OpenRewriteModel model = OpenRewriteModelBuilder.forProjectDirectory(tempDirectory.resolve(sourceFile.getSourcePath()).getParent().toFile(), null, initScriptContents);
                            GradleSettings gradleSettings = model.getGradleSettings();
                            if(gradleSettings != null) {
                                sourceFiles.set(i, sourceFile.withMarkers(sourceFile.getMarkers().setByType(gradleSettings)));
                            }
                        } else if (sourceFile.getSourcePath().endsWith("build.gradle") || sourceFile.getSourcePath().endsWith("build.gradle.kts")) {
                            OpenRewriteModel model = OpenRewriteModelBuilder.forProjectDirectory(projectDir.toFile(), tempDirectory.resolve(sourceFile.getSourcePath()).toFile(), initScriptContents);
                            GradleProject gradleProject = model.getGradleProject();
                            allRepositories.addAll(gradleProject.getMavenRepositories());
                            allBuildscriptRepositories.addAll(gradleProject.getBuildscript().getMavenRepositories());
                            sourceFiles.set(i, sourceFile.withMarkers(sourceFile.getMarkers().setByType(gradleProject)));
                            gradleProjects.put(getDirectory(sourceFile), model.getGradleProject());
                        } else if (sourceFile.getSourcePath().toString().endsWith(".gradle") || sourceFile.getSourcePath().toString().endsWith(".gradle.kts")) {
                            freestandingScriptFound = true;
                        }
                    }
                    for (int i = 0; i < sourceFiles.size(); i++) {
                        SourceFile sourceFile = sourceFiles.get(i);
                        if (sourceFile.getSourcePath().endsWith("gradle.lockfile") || sourceFile.getSourcePath().endsWith("buildscript-gradle.lockfile")) {
                            GradleProject project = gradleProjects.get(getDirectory(sourceFile));
                            if (project != null) {
                                sourceFiles.set(i, sourceFile.withMarkers(sourceFile.getMarkers().setByType(project)));
                            }
                        }
                    }
                    if (freestandingScriptFound) {
                        // Mimic the behavior of the gradle plugin
                        // Construct a synthetic marker to apply to freestanding Gradle scripts to aid recipes in resolving dependencies
                        GradleProject freestandingScriptMarker = new GradleProject(
                                randomId(), "", "", "", "", emptyList(), new ArrayList<>(allRepositories),
                                emptyList(), emptyMap(), new GradleBuildscript(randomId(), new ArrayList<>(allBuildscriptRepositories), emptyMap()));
                        for (int i = 0; i < sourceFiles.size(); i++) {
                            SourceFile sourceFile = sourceFiles.get(i);
                            if ((sourceFile.getSourcePath().toString().endsWith(".gradle") || sourceFile.getSourcePath().toString().endsWith(".gradle.kts")) &&
                                    !sourceFile.getMarkers().findFirst(GradleProject.class).isPresent() && !sourceFile.getMarkers().findFirst(GradleSettings.class).isPresent()) {
                                sourceFiles.set(i, sourceFile.withMarkers(sourceFile.getMarkers().add(freestandingScriptMarker)));
                            }
                        }
                    }
                } finally {
                    deleteDirectory(tempDirectory.toFile());
                }
            } catch (IOException e) {
                throw new TestAbortedException("Failed to load Gradle tooling API", e);
            }
        };
    }

    public static UncheckedConsumer<List<SourceFile>> withToolingApi(@Nullable String version, @Nullable String distribution) {
        GradleWrapper gradleWrapper = null;
        if (version != null) {
            gradleWrapper = GradleWrapper.create(distribution, version, new InMemoryExecutionContext());
        }
        return withToolingApi(gradleWrapper);
    }

    @SuppressWarnings("unused")
    public static UncheckedConsumer<List<SourceFile>> withToolingApi(String version) {
        return withToolingApi(version, "bin");
    }

    public static UncheckedConsumer<List<SourceFile>> withToolingApi() {
        return withToolingApi((GradleWrapper) null, null);
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

    private static String getDirectory(SourceFile file) {
        Path sourcePath = file.getSourcePath();
        Path parent = sourcePath.getParent();
        if (parent != null) {
            return parent.toString();
        }
        return "";
    }
}
