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
import org.openrewrite.gradle.DependencyVersionSelector;
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
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.semver.Semver;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangePluginVersion extends Recipe {
    private static final String GRADLE_PROPERTIES_FILE_NAME = "gradle.properties";

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
        return "Change a Gradle plugin version by id";
    }

    @Override
    public String getDescription() {
        return "Change a Gradle plugin by id to a later version.";
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
        GroovyVisitor<ExecutionContext> groovyVisitor = new GroovyVisitor<ExecutionContext>() {
            @Nullable
            private GradleProject gradleProject;

            @Nullable
            private GradleSettings gradleSettings;

            @Override
            public J visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                gradleProject = cu.getMarkers().findFirst(GradleProject.class).orElse(null);
                gradleSettings = cu.getMarkers().findFirst(GradleSettings.class).orElse(null);
                if (gradleProject == null && gradleSettings == null) {
                    return cu;
                }
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
                if (pluginId == null || !StringUtils.matchesGlob(pluginId, pluginIdPattern)) {
                    return m;
                }

                List<Expression> versionArgs = m.getArguments();
                String currentVersion = "0";
                if (versionArgs.get(0) instanceof J.Literal) {
                    currentVersion = (String) ((J.Literal) versionArgs.get(0)).getValue();
                    if (currentVersion == null) {
                        currentVersion = "0";
                    }
                }

                String selectedNewVersion = newVersion;
                if (currentVersion.equals("0") && "latest.patch".equals(newVersion)) {
                    selectedNewVersion = "latest.release";
                }

                try {
                    String resolvedVersion = new DependencyVersionSelector(null, gradleProject, gradleSettings)
                            .select(new GroupArtifactVersion(pluginId, pluginId + ".gradle.plugin", currentVersion), "classpath", selectedNewVersion, versionPattern, ctx);
                    if (resolvedVersion == null) {
                        return m;
                    }
                    return m.withArguments(ListUtils.map(versionArgs, v -> new J.Literal(
                            Tree.randomId(),
                            v.getPrefix(),
                            v.getMarkers(),
                            resolvedVersion,
                            "'" + resolvedVersion + "'",
                            null,
                            JavaType.Primitive.String
                    )));
                } catch (MavenDownloadingException e) {
                    return Markup.warn(m, e);
                }
            }
        };
        return Preconditions.or(Preconditions.check(Preconditions.or(new IsBuildGradle<>(), new IsSettingsGradle<>()), groovyVisitor));
    }
}
