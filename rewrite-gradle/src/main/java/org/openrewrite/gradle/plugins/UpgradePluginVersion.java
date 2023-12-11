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
package org.openrewrite.gradle.plugins;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.semver.Semver;

import java.util.List;
import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradePluginVersion extends Recipe {
    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Plugin id",
            description = "The `ID` part of `plugin { ID }`, as a glob expression.",
            example = "com.jfrog.bintray")
    String pluginIdPattern;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number. " +
                          "You can also use `latest.release` for the latest available version and `latest.patch` if " +
                          "the current version is a valid semantic version. For more details, you can look at the documentation " +
                          "page of [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors). " +
                          "Defaults to `latest.release`.",
            example = "29.X",
            required = false)
    @Nullable
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Override
    public String getDisplayName() {
        return "Update a Gradle plugin by id";
    }

    @Override
    public String getDescription() {
        return "Update a Gradle plugin by id to a later version.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher pluginMatcher = new MethodMatcher("PluginSpec id(..)", false);
        MethodMatcher versionMatcher = new MethodMatcher("Plugin version(..)", false);
        return Preconditions.check(Preconditions.or(new IsBuildGradle<>(), new IsSettingsGradle<>()), new GroovyVisitor<ExecutionContext>() {
            private GradleProject gradleProject;
            private GradleSettings gradleSettings;

            @Override
            public J visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                Optional<GradleProject> maybeGradleProject = cu.getMarkers().findFirst(GradleProject.class);
                Optional<GradleSettings> maybeGradleSettings = cu.getMarkers().findFirst(GradleSettings.class);
                if (!maybeGradleProject.isPresent() && !maybeGradleSettings.isPresent()) {
                    return cu;
                }

                gradleProject = maybeGradleProject.orElse(null);
                gradleSettings = maybeGradleSettings.orElse(null);
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (!(versionMatcher.matches(m) &&
                        m.getSelect() instanceof J.MethodInvocation &&
                        pluginMatcher.matches(m.getSelect()))) {
                    return m;
                }
                List<Expression> pluginArgs = ((J.MethodInvocation) m.getSelect()).getArguments();
                if (!(pluginArgs.get(0) instanceof J.Literal)) {
                    return m;
                }
                String pluginId = (String) ((J.Literal) pluginArgs.get(0)).getValue();
                if(pluginId == null || !StringUtils.matchesGlob(pluginId, pluginIdPattern)) {
                    return m;
                }

                List<Expression> versionArgs = m.getArguments();
                if (!(versionArgs.get(0) instanceof J.Literal)) {
                    return m;
                }
                String currentVersion = (String) ((J.Literal) versionArgs.get(0)).getValue();
                if(currentVersion == null) {
                    return m;
                }
                Optional<String> version;
                try {
                    version = AddPluginVisitor.resolvePluginVersion(pluginId, currentVersion, newVersion, versionPattern, getRepositories(), ctx);
                    J.MethodInvocation finalM = m;
                    m = version.map(upgradeVersion -> finalM.withArguments(ListUtils.map(versionArgs, v -> {
                                J.Literal versionLiteral = (J.Literal) v;
                                assert versionLiteral.getValueSource() != null;
                                return versionLiteral
                                        .withValue(upgradeVersion)
                                        .withValueSource(versionLiteral.getValueSource().replace(currentVersion, upgradeVersion));
                            })))
                            .orElse(m);

                    return m;
                } catch (MavenDownloadingException e) {
                    return e.warn(m);
                }
            }

            private List<MavenRepository> getRepositories() {
                if (gradleSettings != null) {
                    return gradleSettings.getPluginRepositories();
                }
                return gradleProject.getMavenPluginRepositories();
            }
        });
    }
}
