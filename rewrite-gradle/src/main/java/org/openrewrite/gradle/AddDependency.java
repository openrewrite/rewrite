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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindJVMTestSuites;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.gradle.trait.GradleMultiDependency;
import org.openrewrite.gradle.trait.JvmTestSuite;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.HasSourceSet;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.semver.Semver;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddDependency extends ScanningRecipe<AddDependency.Scanned> {

    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'com.google.guava:guava:VERSION'",
            example = "guava")
    String artifactId;

    @Option(displayName = "Version",
            description = "An exact version number or node-style semver selector used to select the version number. " +
                    "You can also use `latest.release` for the latest available version and `latest.patch` if " +
                    "the current version is a valid semantic version. For more details, you can look at the documentation " +
                    "page of [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors).",
            example = "29.X",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example, " +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Configuration",
            description = "A configuration to use when it is not what can be inferred from usage. Most of the time this will be left empty, but " +
                    "is used when adding a new as of yet unused dependency.",
            example = "implementation",
            required = false)
    @Nullable
    String configuration;

    @Option(displayName = "Only if using",
            description = "Used to determine if the dependency will be added and in which scope it should be placed.",
            example = "org.junit.jupiter.api.*",
            required = false)
    @Nullable
    String onlyIfUsing;

    @Option(displayName = "Classifier",
            description = "A classifier to add. Commonly used to select variants of a library.",
            example = "test",
            required = false)
    @Nullable
    String classifier;

    @Option(displayName = "Extension",
            description = "The extension of the dependency to add. If omitted Gradle defaults to assuming the type is \"jar\".",
            example = "jar",
            required = false)
    @Nullable
    String extension;

    @Option(displayName = "Family pattern",
            description = "A pattern, applied to groupIds, used to determine which other dependencies should have aligned version numbers. " +
                    "Accepts '*' as a wildcard character.",
            example = "com.fasterxml.jackson*",
            required = false)
    @Nullable
    String familyPattern;

    @Option(displayName = "Accept transitive",
            description = "Default false. If enabled, the dependency will not be added if it is already on the classpath as a transitive dependency.",
            example = "true",
            required = false)
    @Nullable
    Boolean acceptTransitive;

    String displayName = "Add Gradle dependency";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", groupId, artifactId, version);
    }

    String description = "Add a gradle dependency to a `build.gradle` file in the correct configuration based on where it is used. " +
            "If the dependency is already declared at a lower version, that declaration is upgraded instead.";

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (version != null) {
            validated = validated.and(Semver.validate(version, versionPattern));
        }
        return validated;
    }

    public static class Scanned {
        Map<JavaProject, Boolean> usingType = new HashMap<>();
        Map<JavaProject, Set<String>> configurationsByProject = new HashMap<>();
        Map<Path, @Nullable JavaProject> declaringProjectDirs = new HashMap<>();
        Map<String, BuildProject> buildProjects = new HashMap<>();
        List<Declaration> declarations = new ArrayList<>();

        // Null leaves existing declarations alone instead of upgrading them
        UpgradeDependencyVersion.@Nullable DependencyVersionState upgradeState;
    }

    // The classpaths each configuration feeds, rather than the GradleProject, which is too heavy to keep
    @Value
    static class BuildProject {
        @Nullable
        JavaProject java;

        Map<String, Set<String>> classpathsByConfiguration;

        // Configurations requesting the dependency, including through a catalog reference no Declaration captures
        Set<String> requestingConfigurations;
    }

    @Value
    static class Declaration {
        @Nullable
        String projectPath;

        String configuration;

        @Nullable
        String versionVariable;

        boolean versionless;
        boolean inProjectsBlock;
    }

    @Override
    public Scanned getInitialValue(ExecutionContext ctx) {
        Scanned acc = new Scanned();
        if (version != null) {
            acc.upgradeState = upgradeDependencyVersion().getInitialValue(ctx);
        }
        return acc;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Scanned acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {

            @Nullable
            UsesType<ExecutionContext> usesType = null;
            final TreeVisitor<?, ExecutionContext> hasTestSourceSet = new HasSourceSet("test").getVisitor();
            final @Nullable TreeVisitor<?, ExecutionContext> upgradeScanner = acc.upgradeState == null ?
                    null : upgradeDependencyVersion().getScanner(acc.upgradeState);

            private boolean usesType(SourceFile sourceFile, ExecutionContext ctx) {
                if (onlyIfUsing == null) {
                    return true;
                }
                if (usesType == null) {
                    usesType = new UsesType<>(onlyIfUsing, true);
                }

                return usesType.isAcceptable(sourceFile, ctx) && usesType.visit(sourceFile, ctx) != sourceFile;
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                if (upgradeScanner != null) {
                    if (upgradeScanner.isAcceptable(sourceFile, ctx)) {
                        upgradeScanner.visit(sourceFile, ctx);
                    }
                    recordDeclaringProject(sourceFile, acc);
                    recordDeclarations(sourceFile, acc, ctx);
                }
                if (sourceFile instanceof JavaSourceFile &&
                        configuration != null && onlyIfUsing != null &&
                        configuration.startsWith("test") && configuration.length() > 4 &&
                        Character.isUpperCase(configuration.charAt(4)) &&
                        sourceFile == hasTestSourceSet.visit(sourceFile, ctx)) {
                    return tree;
                }
                JavaProject javaProject = sourceFile.getMarkers().findFirst(JavaProject.class).orElse(null);
                if (javaProject == null) {
                    return tree;
                }
                JavaSourceSet javaSourceSet = sourceFile.getMarkers().findFirst(JavaSourceSet.class).orElse(null);
                String configForThisFile = javaSourceSet == null ? null :
                        "main".equals(javaSourceSet.getName()) ? "implementation" : javaSourceSet.getName() + "Implementation";
                // Skip the expensive UsesType visit if a previous file in this project's source set already
                // contributed the same configuration; subsequent files cannot change the answer.
                Set<String> knownConfigurations = acc.configurationsByProject.get(javaProject);
                if (configForThisFile != null && knownConfigurations != null && knownConfigurations.contains(configForThisFile)) {
                    return tree;
                }
                boolean uses = usesType(sourceFile, ctx);
                acc.usingType.compute(javaProject, (jp, usingType) -> Boolean.TRUE.equals(usingType) || uses);

                if (uses && configForThisFile != null) {
                    acc.configurationsByProject.computeIfAbsent(javaProject, ignored -> new HashSet<>()).add(configForThisFile);
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Scanned acc) {
        // Allow when configuration is explicitly provided, when onlyIfUsing is not set (default to "implementation"),
        // or when source files were scanned
        boolean hasExplicitConfiguration = !StringUtils.isBlank(configuration);
        TreeVisitor<?, ExecutionContext> addDependency = Preconditions.check(hasExplicitConfiguration || onlyIfUsing == null || !acc.configurationsByProject.isEmpty(),
                Preconditions.check(new IsBuildGradle<>(true), new JavaIsoVisitor<ExecutionContext>() {

                    @Override
                    public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                        if (!(tree instanceof JavaSourceFile)) {
                            return (J) tree;
                        }
                        JavaSourceFile s = (JavaSourceFile) tree;
                        Optional<JavaProject> maybeJp = s.getMarkers().findFirst(JavaProject.class);
                        Optional<GradleProject> maybeGp = s.getMarkers().findFirst(GradleProject.class);

                        if (!maybeGp.isPresent()) {
                            return s;
                        }
                        if (!isUsing(maybeJp.orElse(null), acc)) {
                            return s;
                        }

                        JavaProject jp = maybeJp.orElse(null);
                        GradleProject gp = maybeGp.get();

                        Set<String> resolvedConfigurations = targetConfigurations(jp, acc);

                        // Only apply filtering when the configuration is known to the GradleProject
                        // (which requires Java sources to have been scanned with the tooling API)
                        boolean configurationKnown = resolvedConfigurations.stream()
                                .anyMatch(c -> gp.getConfiguration(c) != null);
                        if (configurationKnown) {
                            GradleConfigurationFilter gradleConfigurationFilter = new GradleConfigurationFilter(gp, resolvedConfigurations);
                            gradleConfigurationFilter.removeTransitiveConfigurations();
                            gradleConfigurationFilter.removeConfigurationsContainingDependency(new GroupArtifact(groupId, artifactId));
                            if (Boolean.TRUE.equals(acceptTransitive)) {
                                gradleConfigurationFilter.removeConfigurationsContainingTransitiveDependency(new GroupArtifact(groupId, artifactId));
                            }
                            resolvedConfigurations = new HashSet<>(gradleConfigurationFilter.getFilteredConfigurations());
                            // Skip when a sibling feeding the same classpath already declares it
                            resolvedConfigurations.removeIf(c -> {
                                GradleDependencyConfiguration gdc = gp.getConfiguration(c);
                                return gdc != null && declaredOnSibling(gp, gdc);
                            });
                        }

                        if (resolvedConfigurations.isEmpty()) {
                            return s;
                        }

                        Set<JvmTestSuite> jvmTestSuites = FindJVMTestSuites.jvmTestSuites(s);
                        for (String resolvedConfiguration : resolvedConfigurations) {
                            JvmTestSuite jvmTestSuite = maybeJvmTestSuite(resolvedConfiguration, jvmTestSuites);
                            if (jvmTestSuite != null) {
                                s = (JavaSourceFile) jvmTestSuite.addDependency(resolvedConfiguration, groupId, artifactId, version, versionPattern, classifier, extension, metadataFailures, null, ctx)
                                        .visitNonNull(s, ctx);
                            } else {
                                s = (JavaSourceFile) new AddDependencyVisitor(groupId, artifactId, version, versionPattern, resolvedConfiguration,
                                        classifier, extension, metadataFailures, this::isTopLevel, null).visitNonNull(s, ctx);
                            }
                        }

                        return s;
                    }

                    private boolean isTopLevel(Cursor cursor) {
                        if (cursor.getValue() instanceof J.Block) {
                            return cursor.getParentOrThrow().getValue() instanceof JavaSourceFile;
                        }
                        return cursor.getParentOrThrow().firstEnclosing(J.MethodInvocation.class) == null;
                    }

                    private @Nullable JvmTestSuite maybeJvmTestSuite(String configuration, Set<JvmTestSuite> jvmTestSuites) {
                        for (JvmTestSuite jvmTestSuite : jvmTestSuites) {
                            if (jvmTestSuite.isAcceptable(configuration)) {
                                return jvmTestSuite;
                            }
                        }
                        return null;
                    }
                })
        );
        if (acc.upgradeState == null) {
            return addDependency;
        }

        TreeVisitor<?, ExecutionContext> upgrade = upgradeDependencyVersion().getVisitor(acc.upgradeState, upgradeScope(acc));
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return upgrade.isAcceptable(sourceFile, ctx) || addDependency.isAcceptable(sourceFile, ctx);
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile s = (SourceFile) tree;
                if (upgrade.isAcceptable(s, ctx) && isInUpgradeScope(s, acc)) {
                    s = (SourceFile) upgrade.visitNonNull(s, ctx);
                }
                if (addDependency.isAcceptable(s, ctx)) {
                    s = (SourceFile) addDependency.visitNonNull(s, ctx);
                }
                return s;
            }
        };
    }

    private Set<String> targetConfigurations(@Nullable JavaProject javaProject, Scanned acc) {
        Set<String> targets = StringUtils.isBlank(configuration) ?
                new HashSet<>(acc.configurationsByProject.getOrDefault(javaProject, emptySet())) :
                new HashSet<>(singletonList(configuration));
        if (targets.isEmpty()) {
            targets.add("implementation");
        }
        return targets;
    }

    /**
     * Only declarations on a configuration the dependency is added to, or sharing a resolvable classpath with one, are
     * upgraded. A version variable or catalog entry follows when it backs at least one of those.
     */
    private UpgradeDependencyVersion.UpgradeScope upgradeScope(Scanned acc) {
        Map<String, Boolean> cache = new HashMap<>();
        Set<String> variables = new HashSet<>();
        boolean catalog = false;
        for (Declaration declaration : acc.declarations) {
            if (inScope(declaration.getProjectPath(), declaration.getConfiguration(), declaration.isInProjectsBlock(), acc, cache)) {
                if (declaration.getVersionVariable() != null) {
                    variables.add(declaration.getVersionVariable());
                }
                catalog |= declaration.isVersionless();
            }
        }
        // An in-scope request with no declaration stating its version is assumed to come from the catalog
        for (Map.Entry<String, BuildProject> project : acc.buildProjects.entrySet()) {
            for (String requesting : project.getValue().getRequestingConfigurations()) {
                if (!catalog && inScope(project.getKey(), requesting, false, acc, cache) &&
                        acc.declarations.stream().noneMatch(d -> !d.isVersionless() && requesting.equals(d.getConfiguration()) &&
                                (d.isInProjectsBlock() || project.getKey().equals(d.getProjectPath())))) {
                    catalog = true;
                }
            }
        }
        boolean catalogInScope = catalog;
        return new UpgradeDependencyVersion.UpgradeScope() {
            @Override
            public boolean includes(GradleDependency declaration) {
                SourceFile buildFile = declaration.getCursor().firstEnclosingOrThrow(SourceFile.class);
                return inScope(projectPath(buildFile), declaration.getConfigurationName(),
                        inProjectsBlock(declaration.getCursor()), acc, cache);
            }

            @Override
            public boolean includesConfiguration(SourceFile buildFile, GradleDependencyConfiguration configuration) {
                String projectPath = projectPath(buildFile);
                if (inScope(projectPath, configuration.getName(), false, acc, cache)) {
                    return true;
                }
                // A classpath also moves when an in-scope configuration it extends from declares the dependency
                for (GradleDependencyConfiguration parent : configuration.allExtendsFrom()) {
                    if (parent.findRequestedDependency(groupId, artifactId) != null &&
                            inScope(projectPath, parent.getName(), false, acc, cache)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean includesVersionVariable(String name) {
                return variables.contains(name);
            }

            @Override
            public boolean includesCatalogLibrary(GroupArtifact ga) {
                return catalogInScope;
            }
        };
    }

    // A script outside any scanned project is left to the path-based gate of isInUpgradeScope
    private boolean inScope(@Nullable String projectPath, String declaredConfiguration, boolean inProjectsBlock,
                            Scanned acc, Map<String, Boolean> cache) {
        if (inProjectsBlock) {
            for (String p : acc.buildProjects.keySet()) {
                if (inScope(p, declaredConfiguration, false, acc, cache)) {
                    return true;
                }
            }
            return false;
        }
        BuildProject project = projectPath == null ? null : acc.buildProjects.get(projectPath);
        if (project == null) {
            return true;
        }
        return cache.computeIfAbsent(projectPath + "#" + declaredConfiguration,
                k -> configurationInScope(project, declaredConfiguration, acc));
    }

    private boolean configurationInScope(BuildProject project, String declaredConfiguration, Scanned acc) {
        if (!isUsing(project.getJava(), acc)) {
            return false;
        }
        Set<String> targets = targetConfigurations(project.getJava(), acc);
        if (targets.contains(declaredConfiguration)) {
            return true;
        }
        Set<String> declaredClasspaths = project.getClasspathsByConfiguration().getOrDefault(declaredConfiguration, emptySet());
        for (String target : targets) {
            if (!Collections.disjoint(declaredClasspaths, project.getClasspathsByConfiguration().getOrDefault(target, emptySet()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean inProjectsBlock(Cursor cursor) {
        for (Cursor c = cursor.getParent(); c != null; c = c.getParent()) {
            if (c.getValue() instanceof J.MethodInvocation) {
                String name = ((J.MethodInvocation) c.getValue()).getSimpleName();
                if ("subprojects".equals(name) || "allprojects".equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static @Nullable String projectPath(SourceFile sourceFile) {
        return sourceFile.getMarkers().findFirst(GradleProject.class).map(GradleProject::getPath).orElse(null);
    }

    private void recordDeclarations(SourceFile sourceFile, Scanned acc, ExecutionContext ctx) {
        String path = sourceFile.getSourcePath().toString();
        if (!(sourceFile instanceof JavaSourceFile) || (!path.endsWith(".gradle") && !path.endsWith(".gradle.kts"))) {
            return;
        }
        GradleProject gp = sourceFile.getMarkers().findFirst(GradleProject.class).orElse(null);
        if (gp != null && (!acc.buildProjects.containsKey(gp.getPath()) ||
                path.endsWith("build.gradle") || path.endsWith("build.gradle.kts"))) {
            Map<String, Set<String>> classpathsByConfiguration = new HashMap<>();
            Set<String> requestingConfigurations = new HashSet<>();
            for (GradleDependencyConfiguration c : gp.getConfigurations()) {
                if (!c.isCanBeResolved() && c.findRequestedDependency(groupId, artifactId) != null) {
                    requestingConfigurations.add(c.getName());
                }
                Set<String> classpaths = new HashSet<>();
                if (c.isCanBeResolved()) {
                    classpaths.add(c.getName());
                }
                for (GradleDependencyConfiguration extending : gp.configurationsExtendingFrom(c, true)) {
                    if (extending.isCanBeResolved()) {
                        classpaths.add(extending.getName());
                    }
                }
                if (!classpaths.isEmpty()) {
                    classpathsByConfiguration.put(c.getName(), classpaths);
                }
            }
            acc.buildProjects.put(gp.getPath(), new BuildProject(
                    sourceFile.getMarkers().findFirst(JavaProject.class).orElse(null), classpathsByConfiguration, requestingConfigurations));
        }
        String projectPath = gp == null ? null : gp.getPath();
        // Matched on resolved coordinates, since a catalog reference like `libs.guava` declares none
        GradleMultiDependency.matcher()
                .asVisitor((multiDependency, c) -> {
                    multiDependency.forEach(dep -> {
                        if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                            acc.declarations.add(new Declaration(projectPath, dep.getConfigurationName(),
                                    dep.getVersionVariable(), dep.getDeclaredVersion() == null, inProjectsBlock(dep.getCursor())));
                        }
                    });
                    return multiDependency.getTree();
                })
                .visit(sourceFile, ctx);
    }

    private boolean declaredOnSibling(GradleProject gp, GradleDependencyConfiguration gdc) {
        for (GradleDependencyConfiguration resolvable : gp.configurationsExtendingFrom(gdc, true)) {
            if (!resolvable.isCanBeResolved()) {
                continue;
            }
            for (GradleDependencyConfiguration sibling : gp.getNameToConfiguration().values()) {
                if (sibling != gdc && !sibling.isCanBeResolved() &&
                        sibling.findRequestedDependency(groupId, artifactId) != null &&
                        gp.configurationsExtendingFrom(sibling, true).contains(resolvable)) {
                    return true;
                }
            }
        }
        return false;
    }

    private UpgradeDependencyVersion upgradeDependencyVersion() {
        return new UpgradeDependencyVersion(groupId, artifactId, version, versionPattern);
    }

    private boolean isUsing(@Nullable JavaProject javaProject, Scanned acc) {
        if (onlyIfUsing == null) {
            return true;
        }
        // When configuration needs to be inferred, also require source set info
        return javaProject != null && acc.usingType.getOrDefault(javaProject, false) &&
                (!StringUtils.isBlank(configuration) || acc.configurationsByProject.containsKey(javaProject));
    }

    private void recordDeclaringProject(SourceFile sourceFile, Scanned acc) {
        String path = sourceFile.getSourcePath().toString();
        if (!path.endsWith("build.gradle") && !path.endsWith("build.gradle.kts")) {
            return;
        }
        GradleProject gradleProject = sourceFile.getMarkers().findFirst(GradleProject.class).orElse(null);
        if (gradleProject == null) {
            return;
        }
        for (GradleDependencyConfiguration c : gradleProject.getConfigurations()) {
            if (c.findRequestedDependency(groupId, artifactId) != null) {
                Path dir = sourceFile.getSourcePath().getParent();
                acc.declaringProjectDirs.put(dir == null ? Paths.get("") : dir,
                        sourceFile.getMarkers().findFirst(JavaProject.class).orElse(null));
                return;
            }
        }
    }

    // A file at or above a declaring project that passes onlyIfUsing; gradle/*.toml belongs to the build one level up
    private boolean isInUpgradeScope(SourceFile sourceFile, Scanned acc) {
        Path path = sourceFile.getSourcePath();
        Path scope = path.getParent();
        if (scope != null && path.toString().endsWith(".toml") && "gradle".equals(String.valueOf(scope.getFileName()))) {
            scope = scope.getParent();
        }
        for (Map.Entry<Path, @Nullable JavaProject> declaring : acc.declaringProjectDirs.entrySet()) {
            if (isUsing(declaring.getValue(), acc) &&
                    (scope == null || scope.toString().isEmpty() || declaring.getKey().startsWith(scope))) {
                return true;
            }
        }
        return false;
    }
}
