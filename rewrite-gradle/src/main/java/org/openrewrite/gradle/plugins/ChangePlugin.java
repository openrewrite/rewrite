/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.gradle.marker.GradlePluginDescriptor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.gradle.util.ChangeStringLiteral;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.semver.Semver;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * When changing a plugin id that uses the `apply` syntax or versionless plugins syntax, the version is will not be changed.
 * At the time of this writing, we do not have a relationship between the plugin id and the jar that contains it that is
 * required in order to update the version for the apply syntax. For the versionless plugins syntax, the version for a
 * third party plugin must be defined in another file that is presently outside the scope of change for this recipe.
 * If you are using either of these plugin styles, you should ensure that the plugin's version is appropriately updated.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class ChangePlugin extends Recipe {
    @Option(displayName = "Plugin ID",
            description = "The current Gradle plugin id.",
            example = "org.openrewrite.rewrite")
    String pluginId;

    @Option(displayName = "New Plugin ID",
            description = "The new Gradle plugin id.",
            example = "org.openrewrite.rewrite")
    String newPluginId;

    @Option(displayName = "New Plugin Version",
            description = "An exact version number or node-style semver selector used to select the version number. " +
                          "You can also use `latest.release` for the latest available version and `latest.patch` if " +
                          "the current version is a valid semantic version. For more details, you can look at the documentation " +
                          "page of [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors).",
            example = "7.x",
            required = false)
    @Nullable
    String newVersion;

    @Override
    public String getDisplayName() {
        return "Change a Gradle plugin";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", pluginId, newPluginId);
    }

    @Override
    public String getDescription() {
        return "Changes the selected Gradle plugin to the new plugin.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, null));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher pluginMatcher = new MethodMatcher("PluginSpec id(..)");
        MethodMatcher versionMatcher = new MethodMatcher("Plugin version(..)");
        MethodMatcher applyMatcher = new MethodMatcher("RewriteGradleProject apply(..)");
        return Preconditions.check(
                Preconditions.or(new IsBuildGradle<>(), new IsSettingsGradle<>()),
                new GroovyIsoVisitor<ExecutionContext>() {
                    GradleProject gradleProject;
                    GradleSettings gradleSettings;

                    @Override
                    public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                        Optional<GradleProject> maybeGp = cu.getMarkers().findFirst(GradleProject.class);
                        Optional<GradleSettings> maybeGs = cu.getMarkers().findFirst(GradleSettings.class);
                        if (!maybeGp.isPresent() && !maybeGs.isPresent()) {
                            return cu;
                        }

                        gradleProject = maybeGp.orElse(null);
                        gradleSettings = maybeGs.orElse(null);

                        G.CompilationUnit g = super.visitCompilationUnit(cu, ctx);
                        if (g != cu) {
                            if (gradleProject != null) {
                                GradleProject updatedGp = gradleProject.withPlugins(ListUtils.map(gradleProject.getPlugins(), plugin -> {
                                    if (pluginId.equals(plugin.getId())) {
                                        return new GradlePluginDescriptor("unknown", newPluginId);
                                    }
                                    return plugin;
                                }));
                                g = g.withMarkers(g.getMarkers().setByType(updatedGp));
                            } else {
                                GradleSettings updatedGs = gradleSettings.withPlugins(ListUtils.map(gradleSettings.getPlugins(), plugin -> {
                                    if (pluginId.equals(plugin.getId())) {
                                        return new GradlePluginDescriptor("unknown", newPluginId);
                                    }
                                    return plugin;
                                }));
                                g = g.withMarkers(g.getMarkers().setByType(updatedGs));
                            }
                        }
                        return g;
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = method;
                        if (versionMatcher.matches(m) &&
                            m.getSelect() instanceof J.MethodInvocation &&
                            pluginMatcher.matches(m.getSelect())) {
                            m = maybeUpdateVersion(m, ctx);
                        } else if (pluginMatcher.matches(m)) {
                            m = maybeUpdatePluginSyntax(m);
                        } else if (applyMatcher.matches(m)) {
                            m = maybeUpdateApplySyntax(m);
                        }
                        return super.visitMethodInvocation(m, ctx);
                    }

                    private J.MethodInvocation maybeUpdateVersion(J.MethodInvocation m, ExecutionContext ctx) {
                        J.MethodInvocation select = (J.MethodInvocation) m.getSelect();
                        if (!pluginId.equals(((J.Literal) select.getArguments().get(0)).getValue())) {
                            return m;
                        }

                        List<Expression> args = m.getArguments();
                        if (!(args.get(0) instanceof J.Literal)) {
                            return m;
                        }

                        J.Literal versionLiteral = (J.Literal) args.get(0);
                        if (versionLiteral.getType() != JavaType.Primitive.String) {
                            return m;
                        }

                        if (!StringUtils.isBlank(newVersion)) {
                            try {
                                String resolvedVersion = new DependencyVersionSelector(null, gradleProject, gradleSettings)
                                        .select(new GroupArtifact(newPluginId, newPluginId + ".gradle.plugin"), "classpath", newVersion, null, ctx);
                                if (resolvedVersion == null) {
                                    return m;
                                }

                                m = m.withSelect(select.withArguments(ListUtils.mapFirst(select.getArguments(), arg -> ChangeStringLiteral.withStringValue((J.Literal) arg, newPluginId))))
                                        .withArguments(Collections.singletonList(ChangeStringLiteral.withStringValue(versionLiteral, resolvedVersion)));
                            } catch (MavenDownloadingException e) {
                                return e.warn(m);
                            }
                        }

                        return m;
                    }

                    private J.MethodInvocation maybeUpdatePluginSyntax(J.MethodInvocation m) {
                        List<Expression> args = m.getArguments();
                        if (!(args.get(0) instanceof J.Literal)) {
                            return m;
                        }

                        J.Literal pluginIdLiteral = (J.Literal) args.get(0);
                        if (pluginIdLiteral.getType() != JavaType.Primitive.String) {
                            return m;
                        }

                        String pluginIdValue = (String) pluginIdLiteral.getValue();
                        if (!pluginId.equals(pluginIdValue)) {
                            return m;
                        }

                        return m.withArguments(ListUtils.concat(ChangeStringLiteral.withStringValue(pluginIdLiteral, newPluginId), args.subList(1, args.size())));
                    }

                    private J.MethodInvocation maybeUpdateApplySyntax(J.MethodInvocation m) {
                        List<Expression> args = m.getArguments();
                        if (!(args.get(0) instanceof G.MapEntry)) {
                            return m;
                        }

                        G.MapEntry entry = (G.MapEntry) args.get(0);
                        if (!(entry.getKey() instanceof J.Literal) && !(entry.getValue() instanceof J.Literal)) {
                            return m;
                        }

                        J.Literal keyLiteral = (J.Literal) entry.getKey();
                        if (keyLiteral.getType() != JavaType.Primitive.String) {
                            return m;
                        }

                        String keyValue = (String) keyLiteral.getValue();
                        if (!"plugin".equals(keyValue)) {
                            return m;
                        }

                        J.Literal valueLiteral = (J.Literal) entry.getValue();
                        if (valueLiteral.getType() != JavaType.Primitive.String) {
                            return m;
                        }

                        String valueValue = (String) valueLiteral.getValue();
                        if (!pluginId.equals(valueValue)) {
                            return m;
                        }

                        entry = entry.withValue(ChangeStringLiteral.withStringValue(valueLiteral, newPluginId));
                        return m.withArguments(ListUtils.concat(entry, args.subList(1, args.size())));
                    }

                    private List<MavenRepository> getPluginRepositories() {
                        if (gradleProject != null) {
                            return gradleProject.getMavenPluginRepositories();
                        }
                        return gradleSettings.getPluginRepositories();
                    }
                }
        );
    }
}
