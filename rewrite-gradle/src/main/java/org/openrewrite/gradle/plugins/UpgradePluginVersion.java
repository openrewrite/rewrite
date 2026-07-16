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
import org.openrewrite.gradle.*;
import org.openrewrite.gradle.internal.ChangeStringLiteral;
import org.openrewrite.gradle.internal.VersionCatalogToml;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.gradle.trait.GradlePlugin;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.TomlTableValue;
import org.openrewrite.toml.tree.Toml;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@SuppressWarnings("DuplicatedCode")
@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradePluginVersion extends ScanningRecipe<UpgradePluginVersion.DependencyVersionState> {
    private static final String GRADLE_PROPERTIES_FILE_NAME = "gradle.properties";

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

    String displayName = "Update a Gradle plugin by id";

    String description = "Update a Gradle plugin by id to a later version defined by the plugins DSL. " +
                "To upgrade a plugin dependency defined by `buildscript.dependencies`, use the `UpgradeDependencyVersion` " +
                "recipe instead.";

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        return validated;
    }

    public static class DependencyVersionState {
        Map<String, String> versionPropNameToPluginId = new HashMap<>();
        Map<String, @Nullable String> pluginIdToNewVersion = new HashMap<>();
    }

    @Override
    public DependencyVersionState getInitialValue(ExecutionContext ctx) {
        return new DependencyVersionState();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(DependencyVersionState acc) {

        JavaVisitor<ExecutionContext> javaVisitor = new JavaVisitor<ExecutionContext>() {
            @Nullable
            private GradleProject gradleProject;

            @Nullable
            private GradleSettings gradleSettings;

            private final Map<String, String> localVariableValues = new HashMap<>();

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    gradleProject = tree.getMarkers().findFirst(GradleProject.class).orElse(null);
                    gradleSettings = tree.getMarkers().findFirst(GradleSettings.class).orElse(null);
                    localVariableValues.clear();
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                J.VariableDeclarations.NamedVariable v = (J.VariableDeclarations.NamedVariable) super.visitVariable(variable, ctx);
                if (v.getInitializer() instanceof J.Literal) {
                    String value = literalValue(v.getInitializer());
                    if (value != null) {
                        localVariableValues.put(v.getSimpleName(), value);
                    }
                }
                return v;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                GradlePlugin plugin = new GradlePlugin.Matcher().pluginIdPattern(pluginIdPattern).get(getCursor()).orElse(null);
                if (plugin == null || plugin.getPluginId() == null) {
                    return m;
                }

                String pluginId = plugin.getPluginId();
                List<Expression> versionArgs = m.getArguments();
                try {
                    if (plugin.getVersion() != null) {
                        String resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, gradleSettings)
                                .select(new GroupArtifactVersion(pluginId, pluginId + ".gradle.plugin", plugin.getVersion()), "classpath", newVersion, versionPattern, ctx);
                        acc.pluginIdToNewVersion.put(pluginId, resolvedVersion);
                    } else if (versionArgs.get(0) instanceof G.GString) {
                        G.GString gString = (G.GString) versionArgs.get(0);
                        if (gString.getStrings().isEmpty() || !(gString.getStrings().get(0) instanceof G.GString.Value)) {
                            return m;
                        }

                        G.GString.Value gStringValue = (G.GString.Value) gString.getStrings().get(0);
                        String versionVariableName = gStringValue.getTree().toString();
                        String resolvedPluginVersion = new DependencyVersionSelector(metadataFailures, gradleProject, gradleSettings)
                                .select(new GroupArtifact(pluginId, pluginId + ".gradle.plugin"), "classpath", newVersion, versionPattern, ctx);
                        if (resolvedPluginVersion == null) {
                            return m;
                        }

                        acc.versionPropNameToPluginId.put(versionVariableName, pluginId);
                        acc.pluginIdToNewVersion.put(pluginId, resolvedPluginVersion);
                    } else if (versionArgs.get(0) instanceof J.Identifier) {
                        J.Identifier identifier = (J.Identifier) versionArgs.get(0);
                        String versionVariableName = identifier.getSimpleName();
                        String localCurrentVersion = localVariableValues.get(versionVariableName);
                        String resolvedPluginVersion;
                        if (localCurrentVersion != null) {
                            resolvedPluginVersion = new DependencyVersionSelector(metadataFailures, gradleProject, gradleSettings)
                                    .select(new GroupArtifactVersion(pluginId, pluginId + ".gradle.plugin", localCurrentVersion), "classpath", newVersion, versionPattern, ctx);
                        } else {
                            resolvedPluginVersion = new DependencyVersionSelector(metadataFailures, gradleProject, gradleSettings)
                                    .select(new GroupArtifact(pluginId, pluginId + ".gradle.plugin"), "classpath", newVersion, versionPattern, ctx);
                        }
                        if (resolvedPluginVersion == null) {
                            return m;
                        }

                        acc.versionPropNameToPluginId.put(versionVariableName, pluginId);
                        acc.pluginIdToNewVersion.put(pluginId, resolvedPluginVersion);
                    }
                } catch (MavenDownloadingException e) {
                    m = Markup.warn(m, e);
                }
                return m;
            }
        };
        return Preconditions.check(Preconditions.or(new IsBuildGradle<>(), new IsSettingsGradle<>()), javaVisitor);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(DependencyVersionState acc) {
        TomlIsoVisitor<ExecutionContext> tomlVisitor = new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile instanceof Toml.Document &&
                        sourceFile.getSourcePath().endsWith(VersionCatalogToml.FILE_NAME);
            }

            @Override
            public Toml.Document visitDocument(Toml.Document document, ExecutionContext ctx) {
                Toml.Table plugins = VersionCatalogToml.findTable(document, "plugins");
                Toml.Table versions = VersionCatalogToml.findTable(document, "versions");
                if (plugins == null) {
                    return document;
                }
                Map<String, String> references = new HashMap<>();
                for (Toml value : plugins.getValues()) {
                    if (value instanceof Toml.KeyValue && ((Toml.KeyValue) value).getValue() instanceof Toml.Table) {
                        Toml.Table plugin = (Toml.Table) ((Toml.KeyValue) value).getValue();
                        String ref = TomlTableValue.getString(plugin, "version.ref");
                        String id = TomlTableValue.getString(plugin, "id");
                        if (ref != null && id != null && StringUtils.matchesGlob(id, pluginIdPattern)) {
                            String selected = select(VersionCatalogToml.getVersion(versions, ref), id, ctx);
                            if (selected != null) {
                                references.put(ref, selected);
                            }
                        }
                    }
                }
                Toml.Document updated = document.withValues(ListUtils.map(document.getValues(), value -> {
                    if (!(value instanceof Toml.Table)) {
                        return value;
                    }
                    Toml.Table table = (Toml.Table) value;
                    if (table.getName() != null && "plugins".equals(table.getName().getName())) {
                        return updatePlugins(table, ctx);
                    }
                    if (table.getName() != null && "versions".equals(table.getName().getName())) {
                        return updateVersions(table, references);
                    }
                    return table;
                }));
                return super.visitDocument(updated, ctx);
            }

            private Toml.Table updatePlugins(Toml.Table plugins, ExecutionContext ctx) {
                return plugins.withValues(ListUtils.map(plugins.getValues(), value -> {
                    if (!(value instanceof Toml.KeyValue)) {
                        return value;
                    }
                    Toml.KeyValue plugin = (Toml.KeyValue) value;
                    if (plugin.getValue() instanceof Toml.Literal && ((Toml.Literal) plugin.getValue()).getValue() instanceof String) {
                        Toml.Literal literal = (Toml.Literal) plugin.getValue();
                        String[] parts = ((String) literal.getValue()).split(":", 2);
                        if (parts.length == 2 && StringUtils.matchesGlob(parts[0], pluginIdPattern)) {
                            String selected = select(parts[1], parts[0], ctx);
                            if (selected != null) {
                                return plugin.withValue(literal.withSource(VersionCatalogToml.quoted(literal, parts[0] + ":" + selected))
                                        .withValue(parts[0] + ":" + selected));
                            }
                        }
                    } else if (plugin.getValue() instanceof Toml.Table) {
                        Toml.Table inline = (Toml.Table) plugin.getValue();
                        String id = TomlTableValue.getString(inline, "id");
                        if (id != null && StringUtils.matchesGlob(id, pluginIdPattern) && TomlTableValue.has(inline, "version")) {
                            String selected = select(TomlTableValue.getString(inline, "version"), id, ctx);
                            if (selected != null) {
                                return plugin.withValue(TomlTableValue.withString(inline, "version", selected));
                            }
                        }
                    }
                    return plugin;
                }));
            }

            private Toml.Table updateVersions(Toml.Table versions, Map<String, String> references) {
                return versions.withValues(ListUtils.map(versions.getValues(), value -> {
                    if (!(value instanceof Toml.KeyValue) || !(((Toml.KeyValue) value).getKey() instanceof Toml.Identifier) ||
                            !(((Toml.KeyValue) value).getValue() instanceof Toml.Literal)) {
                        return value;
                    }
                    String selected = references.get(((Toml.Identifier) ((Toml.KeyValue) value).getKey()).getName());
                    if (selected == null) {
                        return value;
                    }
                    Toml.Literal literal = (Toml.Literal) ((Toml.KeyValue) value).getValue();
                    return ((Toml.KeyValue) value).withValue(literal.withSource(VersionCatalogToml.quoted(literal, selected)).withValue(selected));
                }));
            }

            private @Nullable String select(@Nullable String current, String id, ExecutionContext ctx) {
                if (current == null) {
                    return null;
                }
                try {
                    return new DependencyVersionSelector(metadataFailures, null, null)
                            .select(new GroupArtifactVersion(id, id + ".gradle.plugin", current), "classpath", newVersion, versionPattern, ctx);
                } catch (MavenDownloadingException e) {
                    return null;
                }
            }

        };
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
                        if (resolvedVersion == null || StringUtils.isBlank(currentVersion)) {
                            return entry;
                        }
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
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    GradleProject gradleProject = tree.getMarkers().findFirst(GradleProject.class).orElse(null);
                    String sbVersion = acc.pluginIdToNewVersion.get("org.springframework.boot");
                    if (gradleProject != null && sbVersion != null) {
                        if (acc.pluginIdToNewVersion.containsKey("org.springframework.boot") &&
                                gradleProject.getPlugins().stream().anyMatch(plugin -> "io.spring.dependency-management".equals(plugin.getId())) &&
                                gradleProject.getPlugins().stream().anyMatch(plugin -> "org.springframework.boot".equals(plugin.getId()))) {
                            //noinspection NullableProblems
                            AtomicReference<@Nullable String> springBootPluginVersion = new GradlePlugin.Matcher()
                                    .pluginIdPattern("org.springframework.boot")
                                    .asVisitor((GradlePlugin plugin, AtomicReference<String> ref) -> {
                                        if (plugin.getVersion() != null) {
                                            ref.set(plugin.getVersion());
                                        }
                                        return plugin.getTree();
                                    }).reduce(tree, new AtomicReference<>());
                            if (springBootPluginVersion.get() != null) {
                                Set<GroupArtifact> requested = gradleProject.getConfigurations().stream()
                                        .flatMap(conf -> conf.getRequested().stream())
                                        .map(Dependency::getGav)
                                        .map(GroupArtifactVersion::asGroupArtifact)
                                        .collect(toSet());
                                try {
                                    MavenPomDownloader mpd = new MavenPomDownloader(ctx);
                                    List<GroupArtifact> oldPlatformManaged = mpd.download(new GroupArtifactVersion("org.springframework.boot", "spring-boot-dependencies", springBootPluginVersion.get()), null, null, gradleProject.getMavenRepositories()).getDependencyManagement().stream()
                                            .map(md -> new GroupArtifact(md.getGroupId(), md.getArtifactId()))
                                            .filter(requested::contains)
                                            .collect(toList());
                                    List<GroupArtifactVersion> newPlatformManaged = mpd.download(new GroupArtifactVersion("org.springframework.boot", "spring-boot-dependencies", sbVersion), null, null, gradleProject.getMavenRepositories()).getDependencyManagement().stream()
                                            .map(md -> new GroupArtifactVersion(md.getGroupId(), md.getArtifactId(), md.getVersion()))
                                            .filter(gav -> requested.stream().anyMatch(r -> r.equals(gav.asGroupArtifact())))
                                            .collect(toList());

                                    List<GroupArtifact> newlyManaged = newPlatformManaged.stream().map(GroupArtifactVersion::asGroupArtifact).collect(toList());
                                    List<GroupArtifact> noLongerManaged = new ArrayList<>(oldPlatformManaged);
                                    noLongerManaged.removeAll(newlyManaged);

                                    newlyManaged.removeAll(oldPlatformManaged);


                                    gradleProject = gradleProject.upgradeDirectDependencyVersions(newPlatformManaged, ctx);
                                    for (GroupArtifact ga : noLongerManaged) {
                                        doAfterVisit(new AddExplicitDependencyVersion(ga.getGroupId(), ga.getArtifactId()));
                                    }

                                    for (GroupArtifact ga : newlyManaged) {
                                        doAfterVisit(new RemoveRedundantDependencyVersions(ga.getGroupId(), ga.getArtifactId(), RemoveRedundantDependencyVersions.Comparator.GTE).getVisitor());
                                    }
                                } catch (MavenDownloadingException e) {
                                    tree = Markup.warn(tree, e);
                                }
                            }
                        }
                        gradleProject = gradleProject.upgradeBuildscriptDirectDependencyVersions(singletonList(new GroupArtifactVersion("org.springframework.boot", "org.springframework.boot.gradle.plugin", sbVersion)), ctx);
                        tree = tree.withMarkers(tree.getMarkers().setByType(gradleProject));
                    }
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                // Match the trait before super to ensure the cursor is unmodified
                GradlePlugin plugin = new GradlePlugin.Matcher().pluginIdPattern(pluginIdPattern).get(getCursor()).orElse(null);

                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                if (plugin == null || plugin.getPluginId() == null || plugin.getVersion() == null ||
                        !"version".equals(m.getSimpleName())) {
                    return m;
                }

                String resolvedVersion = acc.pluginIdToNewVersion.get(plugin.getPluginId());
                if (resolvedVersion == null) {
                    return m;
                }
                List<Expression> versionArgs = m.getArguments();
                return m.withArguments(ListUtils.map(versionArgs, v -> ChangeStringLiteral.withStringValue(v, resolvedVersion)));
            }

            @Override
            public J visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                J.VariableDeclarations.NamedVariable visited = (J.VariableDeclarations.NamedVariable) super.visitVariable(variable, ctx);
                if (acc.versionPropNameToPluginId.containsKey(visited.getSimpleName()) && visited.getInitializer() instanceof J.Literal) {
                    J.Literal initializer = (J.Literal) visited.getInitializer();
                    String oldVersion = literalValue(initializer);
                    String newVersion = acc.pluginIdToNewVersion.get(acc.versionPropNameToPluginId.get(visited.getSimpleName()));
                    if (newVersion != null && !newVersion.equals(oldVersion)) {
                        VersionComparator versionComparator = Semver.validate(newVersion, versionPattern).getValue();
                        if (versionComparator == null) {
                            return visited;
                        }
                        Optional<String> finalVersion = versionComparator.upgrade(oldVersion != null ? oldVersion : "", singletonList(newVersion));
                        if (finalVersion.isPresent()) {
                            String valueSource = initializer.getValueSource() == null || oldVersion == null ? initializer.getValueSource() : initializer.getValueSource().replace(oldVersion, newVersion);
                            return visited.withInitializer(initializer.withValueSource(valueSource).withValue(finalVersion.get()));
                        }
                    }
                }
                return visited;
            }
        };
        return Preconditions.or(
                propertiesVisitor,
                tomlVisitor,
                Preconditions.check(Preconditions.or(new IsBuildGradle<>(), new IsSettingsGradle<>()), javaVisitor)
        );
    }

    private @Nullable String literalValue(Expression expr) {
        return new JavaVisitor<AtomicReference<@Nullable String>>() {
            @Override
            public J visitLiteral(J.Literal literal, AtomicReference<@Nullable String> value) {
                if (literal.getType() == JavaType.Primitive.String) {
                    value.compareAndSet(null, (String) literal.getValue());
                }
                return literal;
            }
        }.reduce(expr, new AtomicReference<>(null)).get();
    }
}
