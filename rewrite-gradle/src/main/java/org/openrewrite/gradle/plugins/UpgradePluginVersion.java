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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.DependencyVersionSelector;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.gradle.internal.ChangeStringLiteral;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.gradle.trait.Traits;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;

@SuppressWarnings("DuplicatedCode")
@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradePluginVersion extends ScanningRecipe<UpgradePluginVersion.DependencyVersionState> {
    private static final String GRADLE_PROPERTIES_FILE_NAME = "gradle.properties";
    public static final String PLUGIN_SUFFIX = ".gradle.plugin";

    @EqualsAndHashCode.Exclude
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

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

    public static class DependencyVersionState {
        Map<String, String> versionAssignmentToPluginId = new HashMap<>();
        Map<String, String> versionPropNameToPluginId = new HashMap<>();
        Map<String, @Nullable String> pluginIdToNewVersion = new HashMap<>();
    }

    @Override
    public DependencyVersionState getInitialValue(ExecutionContext ctx) {
        return new DependencyVersionState();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isPluginVersion(Cursor cursor) {
        if (!(cursor.getValue() instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation maybeVersion = cursor.getValue();
        if (!"version".equals(maybeVersion.getSimpleName())) {
            return false;
        }
        return parentMethodCursorOrNull(cursor, "plugins") != null;
    }

    private @Nullable Cursor parentMethodCursorOrNull(Cursor cursor, String name) {
        Cursor parent = cursor.dropParentUntil(it -> (it instanceof J.MethodInvocation) || it == Cursor.ROOT_VALUE);
        if (!(parent.getValue() instanceof J.MethodInvocation)) {
            return null;
        }
        return name.equals(((J.MethodInvocation) parent.getValue()).getSimpleName()) ? parent : null;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(DependencyVersionState acc) {

        JavaVisitor<ExecutionContext> javaVisitor = new JavaVisitor<ExecutionContext>() {
            @Nullable
            private GradleProject gradleProject;

            @Nullable
            private GradleSettings gradleSettings;

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    gradleProject = tree.getMarkers().findFirst(GradleProject.class).orElse(null);
                    gradleSettings = tree.getMarkers().findFirst(GradleSettings.class).orElse(null);
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                Optional<GradleDependency> gradleDependency = Traits.gradleDependency().get(getCursor());
                if (!(isPluginVersion(getCursor()) || gradleDependency.isPresent())) {
                    return m;
                }
                String pluginId = pluginIdOrNull(m, gradleDependency);
                if (pluginId == null) {
                    return m;
                }

                String currentVersion;
                //buildscript
                if (gradleDependency.isPresent()) {
                    GradleDependency dependency = gradleDependency.get();
                    currentVersion = dependency.getResolvedDependency().getVersion();
                    if (dependency.getRequestedDependency().getVersion() != null) {
                        acc.versionAssignmentToPluginId.put(dependency.getRequestedDependency().getVersion(), pluginId);
                    }
                } else {
                    //plugins block
                    List<Expression> versionArgs = m.getArguments();
                    currentVersion = literalValue(versionArgs.get(0));
                    if (currentVersion == null) {
                        if (versionArgs.get(0) instanceof G.GString) {
                            G.GString gString = (G.GString) versionArgs.get(0);
                            if (gString.getStrings().isEmpty() || !(gString.getStrings().get(0) instanceof G.GString.Value)) {
                                return m;
                            }

                            G.GString.Value gStringValue = (G.GString.Value) gString.getStrings().get(0);
                            acc.versionPropNameToPluginId.put(gStringValue.getTree().toString(), pluginId);
                        } else if (versionArgs.get(0) instanceof J.Identifier) {
                            acc.versionPropNameToPluginId.put(((J.Identifier) versionArgs.get(0)).getSimpleName(), pluginId);
                        }
                    }
                }

                try {
                    String resolvedVersion;
                    if (currentVersion == null) {
                        resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, gradleSettings)
                                .select(new GroupArtifact(pluginId, pluginId + PLUGIN_SUFFIX), "classpath", newVersion, versionPattern, ctx);
                    } else {
                        resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, gradleSettings)
                                .select(new GroupArtifactVersion(pluginId, pluginId + PLUGIN_SUFFIX, currentVersion), "classpath", newVersion, versionPattern, ctx);
                    }
                    acc.pluginIdToNewVersion.put(pluginId, resolvedVersion);
                } catch (MavenDownloadingException e) {
                    // continue
                }
                return m;
            }
        };
        return Preconditions.check(Preconditions.or(new IsBuildGradle<>(), new IsSettingsGradle<>()), javaVisitor);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(DependencyVersionState acc) {
        PropertiesVisitor<ExecutionContext> propertiesVisitor = new PropertiesVisitor<ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return super.isAcceptable(sourceFile, ctx) && sourceFile.getSourcePath().endsWith(GRADLE_PROPERTIES_FILE_NAME);
            }

            @Override
            public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                if (acc.versionPropNameToPluginId.containsKey(entry.getKey())) {
                    String currentVersion = entry.getValue().getText();
                    String pluginId = acc.versionPropNameToPluginId.get(entry.getKey());
                    if (!StringUtils.isBlank(newVersion)) {
                        String resolvedVersion = acc.pluginIdToNewVersion.get(pluginId);
                        VersionComparator versionComparator = Semver.validate(newVersion, versionPattern).getValue();
                        if (versionComparator == null) {
                            return entry;
                        }

                        Optional<String> finalVersion = versionComparator.upgrade(currentVersion, singletonList(resolvedVersion));
                        if (finalVersion.isPresent()) {
                            return entry.withValue(entry.getValue().withText(finalVersion.get()));
                        }
                    }
                }
                return entry;
            }
        };
        JavaVisitor<ExecutionContext> javaVisitor = new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                Optional<GradleDependency> gradleDependency = Traits.gradleDependency().get(getCursor());
                if (!(isPluginVersion(getCursor()) || gradleDependency.isPresent())) {
                    return m;
                }
                String pluginId = pluginIdOrNull(m, gradleDependency);
                if (pluginId == null) {
                    return m;
                }
                String resolvedVersion = acc.pluginIdToNewVersion.get(pluginId);
                if (resolvedVersion == null) {
                    return m;
                }

                //buildscript dependency
                if (gradleDependency.isPresent()) {
                    return gradleDependency.get().withVersion(resolvedVersion);
                }
                //plugins block
                List<Expression> versionArgs = m.getArguments();
                String currentVersion = literalValue(m.getArguments().get(0));
                if (currentVersion == null) {
                    return m;
                }
                return m.withArguments(ListUtils.map(versionArgs, v -> {
                    assert v != null;
                    return ChangeStringLiteral.withStringValue(v, resolvedVersion);
                }));
            }

            @Override
            public J visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                J.Assignment visited = (J.Assignment) super.visitAssignment(assignment, ctx);
                Cursor cursor = parentMethodCursorOrNull(getCursor(), "ext");
                if (!StringUtils.isBlank(newVersion) && cursor != null && parentMethodCursorOrNull(cursor, "buildscript") != null) {
                    Expression variable = assignment.getVariable();
                    if (variable instanceof J.Identifier && visited.getAssignment() instanceof J.Literal && ((J.Literal) visited.getAssignment()).getValue() instanceof String) {
                        String pluginId = acc.versionAssignmentToPluginId.get(((J.Identifier) variable).getSimpleName());
                        if (pluginId != null) {
                            String resolvedVersion = acc.pluginIdToNewVersion.get(pluginId);
                            String currentVersion = (String) ((J.Literal) visited.getAssignment()).getValue();
                            VersionComparator versionComparator = Semver.validate(newVersion, versionPattern).getValue();
                            if (versionComparator != null) {
                                Optional<String> finalVersion = versionComparator.upgrade(currentVersion, singletonList(resolvedVersion));
                                if (finalVersion.isPresent()) {
                                    return visited.withAssignment(ChangeStringLiteral.withStringValue(visited.getAssignment(), resolvedVersion));
                                }
                            }
                        }
                    }
                }
                return visited;
            }

            @Override
            public J visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                J.VariableDeclarations.NamedVariable visited = (J.VariableDeclarations.NamedVariable) super.visitVariable(variable, ctx);
                if (acc.versionPropNameToPluginId.containsKey(visited.getSimpleName()) && visited.getInitializer() instanceof J.Literal) {
                    J.Literal initializer = (J.Literal) visited.getInitializer();
                    String oldVersion = literalValue(initializer);
                    String newVersion = acc.pluginIdToNewVersion.get(acc.versionPropNameToPluginId.get(visited.getSimpleName()));
                    if (newVersion != null && !newVersion.equals(oldVersion)) {
                        String valueSource = initializer.getValueSource() == null || oldVersion == null ? initializer.getValueSource() : initializer.getValueSource().replace(oldVersion, newVersion);
                        return visited.withInitializer(initializer.withValueSource(valueSource).withValue(newVersion));
                    }
                }
                return visited;
            }
        };
        return Preconditions.or(propertiesVisitor, Preconditions.check(Preconditions.or(new IsBuildGradle<>(), new IsSettingsGradle<>()), javaVisitor));
    }

    private @Nullable String pluginIdOrNull(J.MethodInvocation m, Optional<GradleDependency> gradleDependency) {
        String pluginId = null;
        if (m.getSelect() != null) {
            List<Expression> pluginArgs = ((J.MethodInvocation) m.getSelect()).getArguments();
            if ((pluginArgs.get(0) instanceof J.Literal)) {
                pluginId = literalValue(pluginArgs.get(0));
            }
        } else if (gradleDependency.isPresent()) {
            pluginId = gradleDependency.get().getResolvedDependency().getGroupId();
        }
        if (pluginId == null || !StringUtils.matchesGlob(pluginId, pluginIdPattern)) {
            return null;
        }
        return pluginId;
    }

    @SuppressWarnings("DataFlowIssue")
    private @Nullable String literalValue(Expression expr) {
        AtomicReference<String> value = new AtomicReference<>(null);
        new JavaVisitor<Integer>() {
            @Override
            public J visitLiteral(J.Literal literal, Integer integer) {
                value.set((String) literal.getValue());
                return literal;
            }
        }.visit(expr, 0);
        return value.get();
    }
}
