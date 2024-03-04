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
import org.openrewrite.*;
import org.openrewrite.gradle.DependencyVersionSelector;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddDevelocityGradlePlugin extends Recipe {
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

    @Option(displayName = "Publish Criteria",
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

    @Override
    public String getDisplayName() {
        return "Add the Develocity Gradle plugin";
    }

    @Override
    public String getDescription() {
        return "Add the Develocity Gradle plugin to settings.gradle files.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (version != null) {
            validated = validated.and(Semver.validate(version, null));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(new IsBuildGradle<>(), new IsSettingsGradle<>()), new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                Optional<BuildTool> maybeBuildTool = cu.getMarkers().findFirst(BuildTool.class);
                if (!maybeBuildTool.isPresent()) {
                    return cu;
                }
                BuildTool buildTool = maybeBuildTool.get();
                if (buildTool.getType() != BuildTool.Type.Gradle) {
                    return cu;
                }
                VersionComparator versionComparator = Semver.validate("(,6)", null).getValue();
                if (versionComparator == null) {
                    return cu;
                }
                // Don't modify an existing gradle enterprise DSL, only add one which is not already present
                if (containsGradleEnterpriseDsl(cu)) {
                    return cu;
                }

                boolean gradleSixOrLater = versionComparator.compare(null, buildTool.getVersion(), "6.0") >= 0;
                if (gradleSixOrLater && cu.getSourcePath().endsWith("settings.gradle")) {
                    // Newer than 6.0 goes in settings
                    Optional<GradleSettings> maybeGradleSettings = cu.getMarkers().findFirst(GradleSettings.class);
                    if (!maybeGradleSettings.isPresent()) {
                        return cu;
                    }
                    GradleSettings gradleSettings = maybeGradleSettings.get();
                    cu = withPlugin(cu, "com.gradle.enterprise", versionComparator, null, gradleSettings, ctx);
                } else if (!gradleSixOrLater && cu.getSourcePath().toString().equals("build.gradle")) {
                    // Older than 6.0 goes in root build.gradle only, not in build.gradle of subprojects
                    Optional<GradleProject> maybeGradleProject = cu.getMarkers().findFirst(GradleProject.class);
                    if (!maybeGradleProject.isPresent()) {
                        return cu;
                    }
                    GradleProject gradleProject = maybeGradleProject.get();

                    cu = withPlugin(cu, "com.gradle.build-scan", versionComparator, gradleProject, null, ctx);
                }

                return cu;
            }
        });
    }

    private G.CompilationUnit withPlugin(G.CompilationUnit cu, String pluginId, VersionComparator versionComparator, @Nullable GradleProject gradleProject, @Nullable GradleSettings gradleSettings, ExecutionContext ctx) {
        try {
            String newVersion = new DependencyVersionSelector(null, gradleProject, gradleSettings)
                    .select(new GroupArtifact("com.gradle.enterprise", "com.gradle.enterprise.gradle.plugin"), "classpath", version, null, ctx);
            if (newVersion == null) {
                return cu;
            }

            cu = (G.CompilationUnit) new AddPluginVisitor(pluginId, newVersion, null)
                    .visitNonNull(cu, ctx);
            cu = (G.CompilationUnit) new UpgradePluginVersion(pluginId, newVersion, null).getVisitor()
                    .visitNonNull(cu, ctx);
            J.MethodInvocation gradleEnterpriseInvocation = gradleEnterpriseDsl(
                    newVersion,
                    versionComparator,
                    getIndent(cu),
                    ctx);
            return cu.withStatements(ListUtils.concat(cu.getStatements(), gradleEnterpriseInvocation));
        } catch (MavenDownloadingException e) {
            return e.warn(cu);
        }
    }

    private static boolean containsGradleEnterpriseDsl(JavaSourceFile cu) {
        AtomicBoolean found = new AtomicBoolean(false);
        new GroovyIsoVisitor<AtomicBoolean>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, AtomicBoolean atomicBoolean) {
                if (atomicBoolean.get()) {
                    return (J) tree;
                }
                return super.visit(tree, atomicBoolean);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean atomicBoolean) {
                if (method.getSimpleName().equals("gradleEnterprise")) {
                    atomicBoolean.set(true);
                }
                return super.visitMethodInvocation(method, atomicBoolean);
            }
        }.visit(cu, found);

        return found.get();
    }

    @Nullable
    private J.MethodInvocation gradleEnterpriseDsl(String newVersion, VersionComparator versionComparator, String indent, ExecutionContext ctx) {
        if (server == null && allowUntrustedServer == null && captureTaskInputFiles == null && uploadInBackground == null && publishCriteria == null) {
            return null;
        }
        boolean versionIsAtLeast3_2 = versionComparator.compare(null, newVersion, "3.2") >= 0;
        boolean versionIsAtLeast3_7 = versionComparator.compare(null, newVersion, "3.7") >= 0;
        StringBuilder ge = new StringBuilder("\ngradleEnterprise {\n");
        if (server != null && !server.isEmpty()) {
            ge.append(indent).append("server = '").append(server).append("'\n");
        }
        if (allowUntrustedServer != null && versionIsAtLeast3_2) {
            ge.append(indent).append("allowUntrustedServer = ").append(allowUntrustedServer).append("\n");
        }
        if (captureTaskInputFiles != null || uploadInBackground != null || (allowUntrustedServer != null && !versionIsAtLeast3_2) || publishCriteria != null) {
            ge.append(indent).append("buildScan {\n");
            if (publishCriteria != null) {
                if (publishCriteria == PublishCriteria.Always) {
                    ge.append(indent).append(indent).append("publishAlways()\n");
                } else {
                    ge.append(indent).append(indent).append("publishOnFailure()\n");
                }
            }
            if (allowUntrustedServer != null && !versionIsAtLeast3_2) {
                ge.append(indent).append(indent).append("allowUntrustedServer = ").append(allowUntrustedServer).append("\n");
            }
            if (uploadInBackground != null) {
                ge.append(indent).append(indent).append("uploadInBackground = ").append(uploadInBackground).append("\n");
            }
            if (captureTaskInputFiles != null) {
                if (versionIsAtLeast3_7) {
                    ge.append(indent).append(indent).append("capture {\n");
                    ge.append(indent).append(indent).append(indent).append("taskInputFiles = ").append(captureTaskInputFiles).append("\n");
                    ge.append(indent).append(indent).append("}\n");
                } else {
                    ge.append(indent).append(indent).append("captureTaskInputFiles = ").append(captureTaskInputFiles).append("\n");
                }
            }
            ge.append(indent).append("}\n");
        }
        ge.append("}\n");
        G.CompilationUnit cu = GradleParser.builder().build()
                .parseInputs(singletonList(
                        Parser.Input.fromString(Paths.get("settings.gradle"), ge.toString())), null, ctx)
                .map(G.CompilationUnit.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle"));

        return (J.MethodInvocation) cu.getStatements().get(0);
    }

    private static String getIndent(G.CompilationUnit cu) {
        TabsAndIndentsStyle style = cu.getStyle(TabsAndIndentsStyle.class, IntelliJ.tabsAndIndents());
        if (style.getUseTabCharacter()) {
            return "\t";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < style.getIndentSize(); i++) {
                sb.append(" ");
            }
            return sb.toString();
        }
    }
}
