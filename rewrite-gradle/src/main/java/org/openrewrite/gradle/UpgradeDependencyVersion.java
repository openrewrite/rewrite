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

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.internal.ChangeStringLiteral;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.gradle.trait.GradleMultiDependency;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeDependencyVersion extends ScanningRecipe<UpgradeDependencyVersion.DependencyVersionState> {
    private static final String GRADLE_PROPERTIES_FILE_NAME = "gradle.properties";

    @EqualsAndHashCode.Exclude
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
    String artifactId;

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
                    "Setting 'newVersion' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Override
    public String getDisplayName() {
        return "Upgrade Gradle dependency versions";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", groupId, artifactId);
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Upgrade the version of a dependency in a build.gradle file. " +
                "Supports updating dependency declarations of various forms:\n" +
                " * `String` notation: `\"group:artifact:version\"` \n" +
                " * `Map` notation: `group: 'group', name: 'artifact', version: 'version'`\n" +
                "Can update version numbers which are defined earlier in the same file in variable declarations.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        return validated;
    }

    private static final String UPDATE_VERSION_ERROR_KEY = "UPDATE_VERSION_ERROR_KEY";
    private static final MethodMatcher PROPERTY_METHOD = new MethodMatcher("* property(String)");
    private static final MethodMatcher FIND_PROPERTY_METHOD = new MethodMatcher("* findProperty(String)");

    @Value
    public static class DependencyVersionState {
        Map<String, Map<GroupArtifact, Set<String>>> variableNames = new HashMap<>();
        Map<String, Map<GroupArtifact, Set<String>>> versionPropNameToGA = new HashMap<>();

        /**
         * The value is either a String representing the resolved version
         * or a MavenDownloadingException representing an error during resolution.
         */
        Map<GroupArtifact, @Nullable Object> gaToNewVersion = new HashMap<>();

        Map<String, Map<GroupArtifact, Set<String>>> configurationPerGAPerModule = new HashMap<>();
    }

    @Override
    public DependencyVersionState getInitialValue(ExecutionContext ctx) {
        return new DependencyVersionState();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(DependencyVersionState acc) {

        //noinspection BooleanMethodIsAlwaysInverted
        return new JavaVisitor<ExecutionContext>() {
            @Nullable
            GradleProject gradleProject;

            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return (sourceFile instanceof G.CompilationUnit && sourceFile.getSourcePath().toString().endsWith(".gradle")) ||
                        (sourceFile instanceof K.CompilationUnit && sourceFile.getSourcePath().toString().endsWith(".gradle.kts"));
            }

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    gradleProject = tree.getMarkers().findFirst(GradleProject.class).orElse(null);
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                // Check for any dependency (single or varargs with literal strings)
                GradleMultiDependency.matcher()
                        .matcher(new DependencyMatcher(groupId, artifactId, getVersionComparator()))
                        .get(getCursor())
                        .ifPresent(multiDependency ->
                                multiDependency.forEach(dep -> scanDependency(dep, ctx)));
                return m;
            }
            /**
             * Scans a single dependency and records its information for later processing.
             */
            private void scanDependency(GradleDependency gradleDependency, ExecutionContext ctx) {
                gatherVariables(gradleDependency);
                String groupId = gradleDependency.getGroupId();
                String artifactId = gradleDependency.getArtifactId();

                // Record the dependency and resolve its version if needed
                GroupArtifact ga = new GroupArtifact(groupId, artifactId);
                String configName = gradleDependency.getConfigurationName();
                if (gradleProject != null) {
                    acc.getConfigurationPerGAPerModule()
                        .computeIfAbsent(getGradleProjectKey(gradleProject), k -> new HashMap<>())
                        .computeIfAbsent(ga, k -> new HashSet<>())
                        .add(configName);
                }

                if (!acc.gaToNewVersion.containsKey(ga) && shouldResolveVersion(groupId, artifactId)) {
                    try {
                        String newVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                .select(ga, configName, UpgradeDependencyVersion.this.newVersion, versionPattern, ctx);

                        String versionVar = gradleDependency.getVersionVariable();
                        if (versionVar != null) {
                            acc.versionPropNameToGA
                                .computeIfAbsent(versionVar, k -> new HashMap<>())
                                .computeIfAbsent(ga, k -> new HashSet<>())
                                .add(configName);
                        }

                        acc.gaToNewVersion.put(ga, newVersion);
                    } catch (MavenDownloadingException e) {
                        acc.gaToNewVersion.put(ga, e);
                    }
                }
            }

            // Some recipes make use of UpgradeDependencyVersion as an implementation detail.
            // Those other recipes might not know up-front which dependency needs upgrading
            // So they use the UpgradeDependencyVersion recipe with null groupId and artifactId to pre-populate all data they could possibly need
            // This works around the lack of proper recipe pipelining which might allow us to have multiple scanning phases as necessary
            private boolean shouldResolveVersion(String declaredGroupId, String declaredArtifactId) {
                //noinspection ConstantValue
                return (groupId == null || artifactId == null) ||
                        new DependencyMatcher(groupId, artifactId, null).matches(declaredGroupId, declaredArtifactId);
            }

            /**
             * Gathers version variable names for dependencies
             */
            private void gatherVariables(GradleDependency gradleDependency) {
                String versionVariableName = gradleDependency.getVersionVariable();
                if (versionVariableName == null) {
                    return;
                }

                String groupId = gradleDependency.getGroupId();
                String artifactId = gradleDependency.getArtifactId();

                if (shouldResolveVersion(groupId, artifactId)) {
                    J.MethodInvocation method = gradleDependency.getTree();
                    acc.variableNames.computeIfAbsent(versionVariableName, it -> new HashMap<>())
                            .computeIfAbsent(new GroupArtifact(groupId, artifactId), it -> new HashSet<>())
                            .add(method.getSimpleName());
                }
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(DependencyVersionState acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            private final UpdateGradle updateGradle = new UpdateGradle(acc);
            private final UpdateProperties updateProperties = new UpdateProperties(acc);

            @Override
            public boolean isAcceptable(SourceFile sf, ExecutionContext ctx) {
                return updateProperties.isAcceptable(sf, ctx) || updateGradle.isAcceptable(sf, ctx);
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                Tree t = tree;
                if (t instanceof SourceFile) {
                    SourceFile sf = (SourceFile) t;
                    if (updateProperties.isAcceptable(sf, ctx)) {
                        t = updateProperties.visitNonNull(t, ctx);
                    } else if (updateGradle.isAcceptable(sf, ctx)) {
                        t = updateGradle.visitNonNull(t, ctx);
                    }
                    Optional<GradleProject> projectMarker = t.getMarkers().findFirst(GradleProject.class);
                    if (tree != t && projectMarker.isPresent()) {
                        try {
                            GradleProject gradleProject = projectMarker.get();
                            Map<GroupArtifact, Set<String>> configurationsPerGa = acc.getConfigurationPerGAPerModule().getOrDefault(getGradleProjectKey(gradleProject), emptyMap());
                            List<GroupArtifactVersion> upgrades = new ArrayList<>();
                            if (acc.gaToNewVersion.isEmpty()) {
                                DependencyMatcher matcher = new DependencyMatcher(groupId, artifactId, null);
                                DependencyVersionSelector versionSelector = new DependencyVersionSelector(metadataFailures, gradleProject, null);
                                for (GroupArtifact groupArtifact : configurationsPerGa.keySet()) {
                                    if (!matcher.matches(groupArtifact.getGroupId(), groupArtifact.getArtifactId())) {
                                        continue;
                                    }
                                    String selectedVersion = versionSelector.select(groupArtifact, null, newVersion, versionPattern, ctx);
                                    GroupArtifactVersion gav = new GroupArtifactVersion(groupArtifact.getGroupId(), groupArtifact.getArtifactId(), selectedVersion);
                                    upgrades.add(gav);
                                }

                            } else {
                                for (Map.Entry<GroupArtifact, @Nullable Object> newVersion : acc.gaToNewVersion.entrySet()) {
                                    if (newVersion.getValue() instanceof String) {
                                        GroupArtifactVersion gav = new GroupArtifactVersion(newVersion.getKey().getGroupId(), newVersion.getKey().getArtifactId(), (String) newVersion.getValue());
                                        upgrades.add(gav);
                                    }
                                }
                            }

                            gradleProject = gradleProject.upgradeDirectDependencyVersions(upgrades, ctx)
                                    .upgradeBuildscriptDirectDependencyVersions(upgrades, ctx);
                            if (projectMarker.get() != gradleProject) {
                                t = t.withMarkers(t.getMarkers().setByType(gradleProject));
                            }
                        } catch (MavenDownloadingException e) {
                            t = Markup.warn(t, e);
                        }
                    }
                }
                return t;
            }
        };
    }

    @RequiredArgsConstructor
    private class UpdateProperties extends PropertiesVisitor<ExecutionContext> {
        final DependencyVersionState acc;
        final DependencyMatcher dependencyMatcher = new DependencyMatcher(groupId, artifactId, null);

        @Override
        public Properties visitFile(Properties.File file, ExecutionContext ctx) {
            if (!file.getSourcePath().endsWith(GRADLE_PROPERTIES_FILE_NAME)) {
                return file;
            }
            return super.visitFile(file, ctx);
        }

        @Override
        public org.openrewrite.properties.tree.Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
            if (acc.versionPropNameToGA.containsKey(entry.getKey())) {
                GroupArtifact ga = acc.versionPropNameToGA.get(entry.getKey()).keySet().stream().findFirst().orElse(null);
                if (ga == null || !dependencyMatcher.matches(ga.getGroupId(), ga.getArtifactId())) {
                    return entry;
                }
                Object result = acc.gaToNewVersion.get(ga);
                if (result == null || result instanceof Exception) {
                    return entry;
                }
                VersionComparator versionComparator = getVersionComparator();
                if (versionComparator == null) {
                    return entry;
                }
                Optional<String> finalVersion = versionComparator.upgrade(entry.getValue().getText(), singletonList((String) result));
                return finalVersion.map(v -> entry.withValue(entry.getValue().withText(v))).orElse(entry);
            }
            return entry;
        }
    }

    private @Nullable VersionComparator getVersionComparator() {
        return Semver.validate(StringUtils.isBlank(newVersion) ? "latest.release" : newVersion, versionPattern).getValue();
    }

    @RequiredArgsConstructor
    private class UpdateGradle extends JavaVisitor<ExecutionContext> {
        final DependencyVersionState acc;

        @Nullable
        GradleProject gradleProject;

        final DependencyMatcher dependencyMatcher = new DependencyMatcher(groupId, artifactId, null);

        @Override
        public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
            return (sourceFile instanceof G.CompilationUnit && sourceFile.getSourcePath().toString().endsWith(".gradle")) ||
                    (sourceFile instanceof K.CompilationUnit && sourceFile.getSourcePath().toString().endsWith(".gradle.kts"));
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile sourceFile = (JavaSourceFile) tree;
                gradleProject = sourceFile.getMarkers().findFirst(GradleProject.class)
                        .orElse(null);
                return super.visit(sourceFile, ctx);
            }
            return super.visit(tree, ctx);
        }

        @Override
        public J postVisit(J tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile cu = (JavaSourceFile) tree;
                if (!acc.variableNames.isEmpty()) {
                    cu = (JavaSourceFile) new UpdateVariable(acc.variableNames, gradleProject).visitNonNull(cu, ctx);
                }
                return cu;
            }
            return tree;
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

            GradleMultiDependency multiDependency = GradleMultiDependency.matcher()
                    .matcher(dependencyMatcher)
                    .get(getCursor())
                    .orElse(null);
            if (multiDependency != null) {
                m = multiDependency.map(dep -> {
                    Object scanResult = acc.gaToNewVersion.get(new GroupArtifact(dep.getGroupId(), dep.getArtifactId()));
                    if (scanResult instanceof Exception) {
                        return Markup.warn(dep.getTree(), (Exception) scanResult);
                    }
                    return updateDependency(dep, ctx);
                });
            }

            if ("ext".equals(method.getSimpleName()) && getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath().endsWith("settings.gradle")) {
                // rare case that gradle versions are set via settings.gradle ext block (only possible for Groovy DSL)
                m = (J.MethodInvocation) new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext executionContext) {
                        J.Assignment a = super.visitAssignment(assignment, executionContext);
                        if (!(a.getVariable() instanceof J.Identifier)) {
                            return a;
                        }
                        Map<GroupArtifact, Set<String>> groupArtifactSetMap = acc.versionPropNameToGA.get("gradle." + a.getVariable());
                        // Guard to ensure that an unsupported notation doesn't throw an exception
                        if (groupArtifactSetMap == null) {
                            return a;
                        }
                        GroupArtifact ga = groupArtifactSetMap.entrySet().stream().findFirst().map(Map.Entry::getKey).orElse(null);
                        if (ga == null) {
                            return a;
                        }
                        String newVersion = (String) acc.gaToNewVersion.get(ga);
                        if (newVersion == null) {
                            return a;
                        }
                        if (!(a.getAssignment() instanceof J.Literal)) {
                            return a;
                        }
                        J.Literal l = (J.Literal) a.getAssignment();
                        if (J.Literal.isLiteralValue(l, newVersion)) {
                            return a;
                        }

                        VersionComparator versionComparator = getVersionComparator();
                        if (versionComparator != null) {
                            String currentVersion = (String) l.getValue();
                            //noinspection DataFlowIssue
                            Optional<String> finalVersion = versionComparator.upgrade(currentVersion, singletonList(newVersion));
                            if (!finalVersion.isPresent()) {
                                // Would be a downgrade, don't change
                                return a;
                            }
                        }

                        String quote = l.getValueSource() == null ? "\"" : l.getValueSource().substring(0, 1);
                        return a.withAssignment(l.withValue(newVersion).withValueSource(quote + newVersion + quote));
                    }
                }.visitNonNull(m, ctx, getCursor().getParentTreeCursor());
            } else if ("ext".equals(m.getSimpleName())) {
                return m.withArguments(ListUtils.map(m.getArguments(), arg -> (Expression) new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext executionContext) {
                        if (assignment.getVariable() instanceof J.Identifier && acc.versionPropNameToGA.containsKey(((J.Identifier) assignment.getVariable()).getSimpleName())) {
                            acc.variableNames
                                    .computeIfAbsent(((J.Identifier) assignment.getVariable()).getSimpleName(), it -> acc.versionPropNameToGA.get(((J.Identifier) assignment.getVariable()).getSimpleName()));
                        }
                        return super.visitAssignment(assignment, executionContext);
                    }
                }.visitNonNull(arg, ctx)));
            }
            return m;
        }

        /**
         * Updates a dependency and returns the transformed GradleDependency.
         */
        private J.MethodInvocation updateDependency(GradleDependency dependency, ExecutionContext ctx) {
            // Get the current version and coordinates
            String currentVersion = dependency.getDeclaredVersion();
            String groupId = dependency.getDeclaredGroupId();
            String artifactId = dependency.getDeclaredArtifactId();

            if (groupId == null || artifactId == null) {
                return dependency.getTree();
            }

            // Check if we have a new version for this dependency
            Object scanResult = acc.gaToNewVersion.get(new GroupArtifact(groupId, artifactId));
            if (scanResult instanceof Exception) {
                // Can't mark warning on a synthetic tree, so just return unchanged
                return dependency.getTree();
            }

            // Handle variable references
            String versionVariable = dependency.getVersionVariable();
            if (versionVariable != null) {
                // Variable updates are handled separately
                return dependency.getTree();
            }

            // If version starts with $, it's a variable reference we can't handle directly
            if (currentVersion != null && currentVersion.startsWith("$")) {
                return dependency.getTree();
            }

            // Select the new version
            String selectedVersion;
            try {
                String configName = dependency.getConfigurationName();

                if (currentVersion == null) {
                    // Only handle dependencies without versions if they are platform dependencies
                    // Regular dependencies without versions are governed by constraints and should not be upgraded
                    if (!dependency.isPlatform()) {
                        return dependency.getTree();
                    }
                    GroupArtifact ga = new GroupArtifact(groupId, artifactId);
                    selectedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                            .select(ga, configName, newVersion, versionPattern, ctx);
                } else {
                    GroupArtifactVersion gav = new GroupArtifactVersion(groupId, artifactId, currentVersion);
                    selectedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                            .select(gav, configName, newVersion, versionPattern, ctx);
                }
            } catch (MavenDownloadingException e) {
                return dependency.getTree();
            }

            if (selectedVersion == null || (currentVersion != null && currentVersion.equals(selectedVersion))) {
                return dependency.getTree();
            }

            return dependency.withDeclaredVersion(selectedVersion).getTree();
        }
    }

    @AllArgsConstructor
    private class UpdateVariable extends JavaIsoVisitor<ExecutionContext> {
        private final Map<String, Map<GroupArtifact, Set<String>>> versionVariableNames;

        @Nullable
        private final GradleProject gradleProject;

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
            J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);
            if (!(v.getInitializer() instanceof J.Literal) ||
                    ((J.Literal) v.getInitializer()).getValue() == null ||
                    ((J.Literal) v.getInitializer()).getType() != JavaType.Primitive.String) {
                return v;
            }
            Map.Entry<GroupArtifact, Set<String>> gaWithConfigs = getGroupArtifactWithConfigs((v.getSimpleName()));
            if (gaWithConfigs == null) {
                return v;
            }

            try {
                J.Literal newVersion = getNewVersion((J.Literal) v.getInitializer(), gaWithConfigs, ctx);
                return newVersion == null ? v : v.withInitializer(newVersion);
            } catch (MavenDownloadingException e) {
                return e.warn(v);
            }
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
            J.Assignment a = super.visitAssignment(assignment, ctx);
            if (!(a.getVariable() instanceof J.Identifier) ||
                    !(a.getAssignment() instanceof J.Literal) ||
                    ((J.Literal) a.getAssignment()).getValue() == null ||
                    ((J.Literal) a.getAssignment()).getType() != JavaType.Primitive.String) {
                return a;
            }
            Map.Entry<GroupArtifact, Set<String>> gaWithConfigs = getGroupArtifactWithConfigs(((J.Identifier) a.getVariable()).getSimpleName());
            if (gaWithConfigs == null) {
                return a;
            }

            try {
                J.Literal newVersion = getNewVersion((J.Literal) a.getAssignment(), gaWithConfigs, ctx);
                return newVersion == null ? a : a.withAssignment(newVersion);
            } catch (MavenDownloadingException e) {
                return e.warn(a);
            }
        }

        private Map.@Nullable Entry<GroupArtifact, Set<String>> getGroupArtifactWithConfigs(String identifier) {
            for (Map.Entry<String, Map<GroupArtifact, Set<String>>> versionVariableNameEntry : versionVariableNames.entrySet()) {
                if (versionVariableNameEntry.getKey().equals(identifier)) {
                    // take first matching group artifact with its configurations
                    return versionVariableNameEntry.getValue().entrySet().iterator().next();
                }
            }
            return null;
        }

        private J.@Nullable Literal getNewVersion(J.Literal literal, Map.Entry<GroupArtifact, Set<String>> gaWithConfigurations, ExecutionContext ctx) throws MavenDownloadingException {
            GroupArtifact ga = gaWithConfigurations.getKey();
            DependencyVersionSelector dependencyVersionSelector = new DependencyVersionSelector(metadataFailures, gradleProject, null);
            GroupArtifactVersion gav = new GroupArtifactVersion(ga.getGroupId(), ga.getArtifactId(), (String) literal.getValue());

            String selectedVersion;
            try {
                selectedVersion = dependencyVersionSelector.select(gav, null, newVersion, versionPattern, ctx);
            } catch (MavenDownloadingException e) {
                if (!gaWithConfigurations.getValue().contains("classpath")) {
                    throw e;
                }
                // try again with "classpath" configuration; if this one fails as well, the MavenDownloadingException is bubbled up so it can be handled
                selectedVersion = dependencyVersionSelector.select(gav, "classpath", newVersion, versionPattern, ctx);
            }
            if (selectedVersion == null) {
                return null;
            }

            return ChangeStringLiteral.withStringValue(literal, selectedVersion);
        }
    }

    static String getGradleProjectKey(GradleProject project) {
        if (StringUtils.isBlank(project.getGroup())) {
            return project.getName();
        }
        if (":".equals(project.getPath())) {
            return project.getGroup();
        }
        return project.getGroup() + project.getPath().replace(":", ".");
    }
}
