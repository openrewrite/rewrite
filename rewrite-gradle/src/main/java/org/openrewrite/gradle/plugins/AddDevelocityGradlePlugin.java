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
package org.openrewrite.gradle.plugins;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.DependencyVersionSelector;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.groovy.GroovyTemplate;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinTemplate;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.search.FindProperties;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.gradle.util.GradleWrapper.WRAPPER_PROPERTIES_LOCATION_RELATIVE_PATH;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddDevelocityGradlePlugin extends ScanningRecipe<AddDevelocityGradlePlugin.WrapperVersions> {
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Plugin version",
            description = "An exact version number or node-style semver selector used to select the version number. " +
                          "You can also use `latest.release` for the latest available version and `latest.patch` if " +
                          "the current version is a valid semantic version. For more details, you can look at the documentation " +
                          "page of [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors). " +
                          "Defaults to `latest.release`.",
            example = "3.x",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Server URL",
            description = "The URL of the Develocity server. If omitted the recipe will set no URL and Gradle will direct scans to https://scans.gradle.com/",
            required = false,
            example = "https://scans.gradle.com/")
    @Nullable
    String server;

    @Option(displayName = "Allow untrusted server",
            description = "When set to `true` the plugin will be configured to allow unencrypted http connections with the server. " +
                          "If set to `false` or omitted, the plugin will refuse to communicate without transport layer security enabled.",
            required = false,
            example = "true")
    @Nullable
    Boolean allowUntrustedServer;

    @Option(displayName = "Capture task input files",
            description = "When set to `true` the plugin will capture additional information about the inputs to Gradle tasks. " +
                          "This increases the size of build scans, but is useful for diagnosing issues with task caching. ",
            required = false,
            example = "true")
    @Nullable
    Boolean captureTaskInputFiles;

    @Option(displayName = "Upload in background",
            description = "When set to `true` the plugin will capture additional information about the outputs of Gradle tasks. " +
                          "This increases the size of build scans, but is useful for diagnosing issues with task caching. ",
            required = false,
            example = "true")
    @Nullable
    Boolean uploadInBackground;

    @Option(displayName = "Publish criteria",
            description = "When set to `Always` the plugin will publish build scans of every single build. " +
                          "When set to `Failure` the plugin will only publish build scans when the build fails. " +
                          "When omitted scans will be published only when the `--scan` option is passed to the build.",
            required = false,
            valid = {"Always", "Failure"},
            example = "Always")
    @Nullable
    PublishCriteria publishCriteria;

    public enum PublishCriteria {
        Always,
        Failure
    }

    String displayName = "Add the Develocity Gradle plugin";

    String description = "Add the Develocity Gradle plugin to settings.gradle files.";

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (version != null) {
            validated = validated.and(Semver.validate(version, null));
        }
        return validated;
    }

    /**
     * The Gradle version declared by each wrapper in the repository, keyed by the directory the wrapper belongs to
     * (the repository root is {@link #ROOT}). Scripts resolve against the nearest enclosing wrapper, so an independent
     * build nested in a subdirectory is never judged by the root wrapper's version.
     */
    public static class WrapperVersions {
        static final Path ROOT = Paths.get("");

        final Map<Path, String> versionsByDirectory = new HashMap<>();

        void put(Path wrapperPropertiesPath, String version) {
            // The path is known to end in the three segments of `gradle/wrapper/gradle-wrapper.properties`
            int nameCount = wrapperPropertiesPath.getNameCount();
            versionsByDirectory.put(nameCount > 3 ? wrapperPropertiesPath.subpath(0, nameCount - 3) : ROOT, version);
        }

        @Nullable String nearest(Path sourcePath) {
            if (versionsByDirectory.isEmpty()) {
                return null;
            }
            for (Path dir = sourcePath.getParent(); ; dir = dir.getParent()) {
                String version = versionsByDirectory.get(dir == null ? ROOT : dir);
                if (version != null) {
                    return version;
                }
                if (dir == null) {
                    return null;
                }
            }
        }
    }

    @Override
    public WrapperVersions getInitialValue(ExecutionContext ctx) {
        return new WrapperVersions();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(WrapperVersions acc) {
        return new PropertiesVisitor<ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return super.isAcceptable(sourceFile, ctx) &&
                       PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/" + WRAPPER_PROPERTIES_LOCATION_RELATIVE_PATH);
            }

            @Override
            public Properties visitFile(Properties.File file, ExecutionContext ctx) {
                for (Properties.Entry entry : FindProperties.find(file, "distributionUrl", false)) {
                    String version = GradleWrapper.versionFromDistributionUrl(entry.getValue().getText());
                    if (version != null) {
                        acc.put(file.getSourcePath(), version);
                        break;
                    }
                }
                return file;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(WrapperVersions acc) {
        return Preconditions.check(Preconditions.or(new IsBuildGradle<>(), new IsSettingsGradle<>()), new JavaIsoVisitor<ExecutionContext>() {

            /**
             * Prefer the {@link BuildTool} marker the OpenRewrite Gradle plugin attaches, and otherwise fall back to
             * the version the nearest wrapper declares, so the recipe works on LSTs produced by any other parser.
             */
            private @Nullable String gradleVersion(JavaSourceFile cu) {
                return cu.getMarkers().findFirst(BuildTool.class)
                        .filter(buildTool -> buildTool.getType() == BuildTool.Type.Gradle)
                        .map(BuildTool::getVersion)
                        .orElseGet(() -> acc.nearest(cu.getSourcePath()));
            }

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    if (tree instanceof G.CompilationUnit) {
                        return visitCompilationUnit((G.CompilationUnit) tree, ctx);
                    }
                    if (tree instanceof K.CompilationUnit) {
                        return visitCompilationUnit((K.CompilationUnit) tree, ctx);
                    }
                }
                return super.visit(tree, ctx);
            }

            public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                String gradleVersion = gradleVersion(cu);
                if (gradleVersion == null) {
                    return cu;
                }
                VersionComparator versionComparator = Semver.validate("(,6)", null).getValue();
                if (versionComparator == null) {
                    return cu;
                }
                // Don't modify an existing gradle enterprise DSL, only add one which is not already present
                if (containsGradleDevelocityDsl(cu)) {
                    return cu;
                }

                boolean gradleSixOrLater = versionComparator.compare(null, gradleVersion, "6.0") >= 0;
                if (gradleSixOrLater && cu.getSourcePath().endsWith("settings.gradle")) {
                    // Newer than 6.0 goes in settings
                    Optional<GradleSettings> maybeGradleSettings = cu.getMarkers().findFirst(GradleSettings.class);
                    if (!maybeGradleSettings.isPresent()) {
                        return cu;
                    }
                    GradleSettings gradleSettings = maybeGradleSettings.get();

                    try {
                        String newVersion = findNewerVersion(new DependencyVersionSelector(metadataFailures, null, gradleSettings), ctx);
                        if (newVersion == null) {
                            return cu;
                        }

                        String pluginId;
                        if (versionComparator.compare(null, newVersion, "3.17") >= 0) {
                            pluginId = "com.gradle.develocity";
                        } else {
                            pluginId = "com.gradle.enterprise";
                        }

                        cu = withPlugin(cu, pluginId, newVersion, versionComparator, ctx);
                    } catch (MavenDownloadingException e) {
                        return e.warn(cu);
                    }
                } else if (!gradleSixOrLater && "build.gradle".equals(cu.getSourcePath().toString())) {
                    // Older than 6.0 goes in root build.gradle only, not in build.gradle of subprojects
                    Optional<GradleProject> maybeGradleProject = cu.getMarkers().findFirst(GradleProject.class);
                    if (!maybeGradleProject.isPresent()) {
                        return cu;
                    }
                    GradleProject gradleProject = maybeGradleProject.get();

                    try {
                        String newVersion = findNewerVersion(new DependencyVersionSelector(metadataFailures, gradleProject, null), ctx);
                        if (newVersion == null) {
                            return cu;
                        }

                        cu = withPlugin(cu, "com.gradle.build-scan", newVersion, versionComparator, ctx);
                    } catch (MavenDownloadingException e) {
                        return e.warn(cu);
                    }
                }

                return cu;
            }

            public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ExecutionContext ctx) {
                String gradleVersion = gradleVersion(cu);
                if (gradleVersion == null) {
                    return cu;
                }
                VersionComparator versionComparator = Semver.validate("(,6)", null).getValue();
                if (versionComparator == null) {
                    return cu;
                }
                // Don't modify an existing gradle enterprise DSL, only add one which is not already present
                if (containsGradleDevelocityDsl(cu)) {
                    return cu;
                }

                boolean gradleSixOrLater = versionComparator.compare(null, gradleVersion, "6.0") >= 0;
                if (gradleSixOrLater && cu.getSourcePath().endsWith("settings.gradle.kts")) {
                    // Newer than 6.0 goes in settings
                    Optional<GradleSettings> maybeGradleSettings = cu.getMarkers().findFirst(GradleSettings.class);
                    if (!maybeGradleSettings.isPresent()) {
                        return cu;
                    }
                    GradleSettings gradleSettings = maybeGradleSettings.get();

                    try {
                        String newVersion = findNewerVersion(new DependencyVersionSelector(metadataFailures, null, gradleSettings), ctx);
                        if (newVersion == null) {
                            return cu;
                        }

                        String pluginId;
                        if (versionComparator.compare(null, newVersion, "3.17") >= 0) {
                            pluginId = "com.gradle.develocity";
                        } else {
                            pluginId = "com.gradle.enterprise";
                        }

                        cu = withPlugin(cu, pluginId, newVersion, versionComparator, ctx);
                    } catch (MavenDownloadingException e) {
                        return e.warn(cu);
                    }
                } else if (!gradleSixOrLater && "build.gradle.kts".equals(cu.getSourcePath().toString())) {
                    // Older than 6.0 goes in root build.gradle only, not in build.gradle of subprojects
                    Optional<GradleProject> maybeGradleProject = cu.getMarkers().findFirst(GradleProject.class);
                    if (!maybeGradleProject.isPresent()) {
                        return cu;
                    }
                    GradleProject gradleProject = maybeGradleProject.get();

                    try {
                        String newVersion = findNewerVersion(new DependencyVersionSelector(metadataFailures, gradleProject, null), ctx);
                        if (newVersion == null) {
                            return cu;
                        }

                        cu = withPlugin(cu, "com.gradle.build-scan", newVersion, versionComparator, ctx);
                    } catch (MavenDownloadingException e) {
                        return e.warn(cu);
                    }
                }

                return cu;
            }

            private @Nullable String findNewerVersion(DependencyVersionSelector versionSelector, ExecutionContext ctx) throws MavenDownloadingException {
                String newVersion = versionSelector
                        .select(new GroupArtifact("com.gradle.develocity", "com.gradle.develocity.gradle.plugin"), "classpath", version, null, ctx);
                if (newVersion == null) {
                    newVersion = versionSelector
                            .select(new GroupArtifact("com.gradle.enterprise", "com.gradle.enterprise.gradle.plugin"), "classpath", version, null, ctx);
                }
                return newVersion;
            }

            private G.CompilationUnit withPlugin(G.CompilationUnit cu, String pluginId, String newVersion, VersionComparator versionComparator, ExecutionContext ctx) {
                cu = (G.CompilationUnit) new AddPluginVisitor(pluginId, newVersion, null, null, false)
                        .visitNonNull(cu, ctx, getCursor());
                cu = (G.CompilationUnit) new UpgradePluginVersion(pluginId, newVersion, null).getVisitor()
                        .visitNonNull(cu, ctx, getCursor());
                return (G.CompilationUnit) appendDevelocityDsl(cu, getCursor(), newVersion, versionComparator, false);
            }

            private K.CompilationUnit withPlugin(K.CompilationUnit cu, String pluginId, String newVersion, VersionComparator versionComparator, ExecutionContext ctx) {
                cu = (K.CompilationUnit) new AddPluginVisitor(pluginId, newVersion, null, null, false)
                        .visitNonNull(cu, ctx, getCursor());
                cu = (K.CompilationUnit) new UpgradePluginVersion(pluginId, newVersion, null).getVisitor()
                        .visitNonNull(cu, ctx, getCursor());
                return (K.CompilationUnit) appendDevelocityDsl(cu, getCursor(), newVersion, versionComparator, true);
            }
        });
    }

    private static boolean containsGradleDevelocityDsl(JavaSourceFile cu) {
        AtomicBoolean found = new AtomicBoolean(false);
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, AtomicBoolean atomicBoolean) {
                if (atomicBoolean.get()) {
                    return (J) tree;
                }
                return super.visit(tree, atomicBoolean);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean atomicBoolean) {
                if ("gradleEnterprise".equals(method.getSimpleName()) || "develocity".equals(method.getSimpleName())) {
                    atomicBoolean.set(true);
                }
                return super.visitMethodInvocation(method, atomicBoolean);
            }
        }.visit(cu, found);

        return found.get();
    }

    /**
     * Every option value travels as a parameter rather than as text, so nothing a user supplies can be mistaken
     * for the template's own placeholder syntax.
     */
    private JavaSourceFile appendDevelocityDsl(JavaSourceFile cu, Cursor scope, String newVersion,
                                               VersionComparator versionComparator, boolean kotlinDsl) {
        if (server == null && allowUntrustedServer == null && captureTaskInputFiles == null && uploadInBackground == null && publishCriteria == null) {
            return cu;
        }
        boolean versionIsAtLeast3_2 = versionComparator.compare(null, newVersion, "3.2") >= 0;
        boolean versionIsAtLeast3_7 = versionComparator.compare(null, newVersion, "3.7") >= 0;
        boolean versionIsAtLeast3_17 = versionComparator.compare(null, newVersion, "3.17") >= 0;

        List<Expression> values = new ArrayList<>();
        StringBuilder ge = new StringBuilder(versionIsAtLeast3_17 ? "develocity {\n" : "gradleEnterprise {\n");
        if (server != null && !server.isEmpty()) {
            ge.append(kotlinDsl ? "    server.set(#{any()})\n" : "    server = #{any()}\n");
            values.add(literal(server, kotlinDsl));
        }
        if (allowUntrustedServer != null && versionIsAtLeast3_2) {
            ge.append(kotlinDsl ? "    allowUntrustedServer.set(#{any()})\n" : "    allowUntrustedServer = #{any()}\n");
            values.add(literal(allowUntrustedServer));
        }
        if (captureTaskInputFiles != null || uploadInBackground != null || (allowUntrustedServer != null && !versionIsAtLeast3_2) || publishCriteria != null) {
            ge.append("    buildScan {\n");
            if (publishCriteria != null) {
                if (publishCriteria == PublishCriteria.Always) {
                    ge.append(versionIsAtLeast3_17 ? "        publishing.onlyIf { true }\n" : "        publishAlways()\n");
                } else if (versionIsAtLeast3_17) {
                    ge.append(kotlinDsl ?
                            "        publishing.onlyIf { it.buildResult.failures.isNotEmpty() }\n" :
                            "        publishing.onlyIf { !it.buildResult.failures.empty }\n");
                } else {
                    ge.append("        publishOnFailure()\n");
                }
            }
            if (allowUntrustedServer != null && !versionIsAtLeast3_2) {
                ge.append(kotlinDsl ? "        allowUntrustedServer.set(#{any()})\n" : "        allowUntrustedServer = #{any()}\n");
                values.add(literal(allowUntrustedServer));
            }
            if (uploadInBackground != null) {
                ge.append(kotlinDsl ? "        uploadInBackground.set(#{any()})\n" : "        uploadInBackground = #{any()}\n");
                values.add(literal(uploadInBackground));
            }
            if (captureTaskInputFiles != null) {
                if (versionIsAtLeast3_7) {
                    ge.append("        capture {\n");
                    String property = versionIsAtLeast3_17 ? "fileFingerprints" : "taskInputFiles";
                    ge.append(kotlinDsl ? "            " + property + ".set(#{any()})\n" : "            " + property + " = #{any()}\n");
                    values.add(literal(captureTaskInputFiles));
                    ge.append("        }\n");
                } else {
                    ge.append(kotlinDsl ? "        captureTaskInputFiles.set(#{any()})\n" : "        captureTaskInputFiles = #{any()}\n");
                    values.add(literal(captureTaskInputFiles));
                }
            }
            ge.append("    }\n");
        }
        ge.append("}");

        List<Statement> statements = kotlinDsl ?
                ((J.Block) ((K.CompilationUnit) cu).getStatements().get(0)).getStatements() :
                ((G.CompilationUnit) cu).getStatements();
        if (statements.isEmpty()) {
            return cu;
        }
        Statement last = statements.get(statements.size() - 1);
        JavaTemplate template = kotlinDsl ?
                KotlinTemplate.builder(ge.toString()).build() :
                GroovyTemplate.builder(ge.toString()).build();
        return template.apply(new Cursor(scope, cu), last.getCoordinates().after(), values.toArray());
    }

    // A value, not a construct: the quoting is the recipe's own, so there is nothing for a parser to tell us
    private static J.Literal literal(String value, boolean kotlinDsl) {
        String quote = kotlinDsl ? "\"" : "'";
        return new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, value, quote + value + quote, null, JavaType.Primitive.String);
    }

    private static J.Literal literal(Boolean value) {
        return new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, value, String.valueOf(value), null, JavaType.Primitive.Boolean);
    }
}
