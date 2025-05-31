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
import org.openrewrite.gradle.internal.Dependency;
import org.openrewrite.gradle.internal.DependencyStringNotationConverter;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.gradle.trait.Traits;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenDownloadingExceptions;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.*;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.*;

import static java.util.Collections.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeDependencyVersion extends ScanningRecipe<UpgradeDependencyVersion.DependencyVersionState> {
    private static final String VERSION_VARIABLE_KEY = "VERSION_VARIABLE";
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

    @Value
    public static class DependencyVersionState {
        Map<String, GroupArtifact> versionPropNameToGA = new HashMap<>();

        /**
         * The value is either a String representing the resolved version
         * or a MavenDownloadingException representing an error during resolution.
         */
        Map<GroupArtifact, Object> gaToNewVersion = new HashMap<>();

        Map<String, Map<GroupArtifact, Set<String>>> configurationPerGAPerModule = new HashMap<>();
        Set<GradleProject> modules = new HashSet<>();
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
                    if (gradleProject != null) {
                        acc.modules.add(gradleProject);
                    }
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                GradleDependency.Matcher gradleDependencyMatcher = Traits.gradleDependency();

                if (gradleDependencyMatcher.get(getCursor()).isPresent()) {
                    if (m.getArguments().get(0) instanceof G.MapEntry) {
                        String declaredGroupId = null;
                        String declaredArtifactId = null;
                        String declaredVersion = null;

                        for (Expression e : m.getArguments()) {
                            if (!(e instanceof G.MapEntry)) {
                                continue;
                            }
                            G.MapEntry arg = (G.MapEntry) e;
                            if (!(arg.getKey() instanceof J.Literal)) {
                                continue;
                            }
                            J.Literal key = (J.Literal) arg.getKey();
                            String valueValue = null;
                            if (arg.getValue() instanceof J.Literal) {
                                J.Literal value = (J.Literal) arg.getValue();
                                if (value.getValue() instanceof String) {
                                    valueValue = (String) value.getValue();
                                }
                            } else if (arg.getValue() instanceof J.Identifier) {
                                J.Identifier value = (J.Identifier) arg.getValue();
                                valueValue = value.getSimpleName();
                            } else if (arg.getValue() instanceof G.GString) {
                                G.GString value = (G.GString) arg.getValue();
                                List<J> strings = value.getStrings();
                                if (!strings.isEmpty() && strings.get(0) instanceof G.GString.Value) {
                                    G.GString.Value versionGStringValue = (G.GString.Value) strings.get(0);
                                    if (versionGStringValue.getTree() instanceof J.Identifier) {
                                        valueValue = ((J.Identifier) versionGStringValue.getTree()).getSimpleName();
                                    }
                                }
                            }
                            if (!(key.getValue() instanceof String)) {
                                continue;
                            }
                            String keyValue = (String) key.getValue();
                            switch (keyValue) {
                                case "group":
                                    declaredGroupId = valueValue;
                                    break;
                                case "name":
                                    declaredArtifactId = valueValue;
                                    break;
                                case "version":
                                    declaredVersion = valueValue;
                                    break;
                            }
                        }
                        if (declaredGroupId == null || declaredArtifactId == null || declaredVersion == null) {
                            return m;
                        }

                        String versionVariableName = declaredVersion;
                        GroupArtifact ga = new GroupArtifact(declaredGroupId, declaredArtifactId);
                        if (gradleProject != null) {
                            acc.getConfigurationPerGAPerModule().computeIfAbsent(getGradleProjectKey(gradleProject), k -> new HashMap<>())
                                    .computeIfAbsent(ga, k -> new HashSet<>())
                                    .add(m.getSimpleName());
                        }
                        if (acc.gaToNewVersion.containsKey(ga) || !shouldResolveVersion(declaredGroupId, declaredArtifactId)) {
                            return m;
                        }
                        try {
                            String resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                    .select(new GroupArtifact(declaredGroupId, declaredArtifactId), m.getSimpleName(), newVersion, versionPattern, ctx);
                            acc.versionPropNameToGA.put(versionVariableName, ga);
                            // It is fine for this value to be null, record it in the map to avoid future lookups
                            //noinspection DataFlowIssue
                            acc.gaToNewVersion.put(ga, resolvedVersion);
                        } catch (MavenDownloadingException e) {
                            acc.gaToNewVersion.put(ga, e);
                            return m;
                        }
                    } else if (m.getArguments().get(0) instanceof J.Assignment) {
                        String declaredGroupId = null;
                        String declaredArtifactId = null;
                        String declaredVersion = null;

                        for (Expression e : m.getArguments()) {
                            if (!(e instanceof G.MapEntry)) {
                                continue;
                            }
                            J.Assignment assignment = (J.Assignment) e;
                            if (!(assignment.getVariable() instanceof J.Identifier)) {
                                continue;
                            }
                            J.Identifier variable = (J.Identifier) assignment.getVariable();
                            String valueValue = null;
                            if (assignment.getAssignment() instanceof J.Literal) {
                                J.Literal value = (J.Literal) assignment.getAssignment();
                                if (value.getValue() instanceof String) {
                                    valueValue = (String) value.getValue();
                                }
                            } else if (assignment.getAssignment() instanceof J.Identifier) {
                                J.Identifier value = (J.Identifier) assignment.getAssignment();
                                valueValue = value.getSimpleName();
                            } else if (assignment.getAssignment() instanceof K.StringTemplate) {
                                K.StringTemplate value = (K.StringTemplate) assignment.getAssignment();
                                List<J> strings = value.getStrings();
                                if (!strings.isEmpty() && strings.get(0) instanceof K.StringTemplate.Expression) {
                                    K.StringTemplate.Expression versionTemplateValue = (K.StringTemplate.Expression) strings.get(0);
                                    if (versionTemplateValue.getTree() instanceof J.Identifier) {
                                        valueValue = ((J.Identifier) versionTemplateValue.getTree()).getSimpleName();
                                    }
                                }
                            }
                            switch (variable.getSimpleName()) {
                                case "group":
                                    declaredGroupId = valueValue;
                                    break;
                                case "name":
                                    declaredArtifactId = valueValue;
                                    break;
                                case "version":
                                    declaredVersion = valueValue;
                                    break;
                            }
                        }
                        if (declaredGroupId == null || declaredArtifactId == null || declaredVersion == null) {
                            return m;
                        }

                        String versionVariableName = declaredVersion;
                        GroupArtifact ga = new GroupArtifact(declaredGroupId, declaredArtifactId);
                        if (gradleProject != null) {
                            acc.getConfigurationPerGAPerModule().computeIfAbsent(getGradleProjectKey(gradleProject), k -> new HashMap<>())
                                    .computeIfAbsent(ga, k -> new HashSet<>())
                                    .add(m.getSimpleName());
                        }
                        if (acc.gaToNewVersion.containsKey(ga) || !shouldResolveVersion(declaredGroupId, declaredArtifactId)) {
                            return m;
                        }
                        try {
                            String resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                    .select(new GroupArtifact(declaredGroupId, declaredArtifactId), m.getSimpleName(), newVersion, versionPattern, ctx);
                            acc.versionPropNameToGA.put(versionVariableName, ga);
                            // It is fine for this value to be null, record it in the map to avoid future lookups
                            //noinspection DataFlowIssue
                            acc.gaToNewVersion.put(ga, resolvedVersion);
                        } catch (MavenDownloadingException e) {
                            acc.gaToNewVersion.put(ga, e);
                            return m;
                        }
                    } else {
                        for (Expression depArg : m.getArguments()) {
                            Dependency dep = null;
                            String versionVariableName = null;
                            if (depArg instanceof J.Literal && ((J.Literal) depArg).getValue() instanceof String) {
                                String gav = (String) ((J.Literal) depArg).getValue();
                                dep = DependencyStringNotationConverter.parse(gav);
                                if (dep != null) {
                                    versionVariableName = dep.getVersion();
                                }
                            } else if (depArg instanceof G.GString) {
                                G.GString gString = (G.GString) depArg;
                                List<J> strings = gString.getStrings();
                                if (strings.size() != 2 || !(strings.get(0) instanceof J.Literal) || !(strings.get(1) instanceof G.GString.Value)) {
                                    continue;
                                }
                                J.Literal groupArtifact = (J.Literal) strings.get(0);
                                G.GString.Value versionValue = (G.GString.Value) strings.get(1);
                                if (!(versionValue.getTree() instanceof J.Identifier) || !(groupArtifact.getValue() instanceof String)) {
                                    continue;
                                }
                                dep = DependencyStringNotationConverter.parse((String) groupArtifact.getValue());
                                versionVariableName = ((J.Identifier) versionValue.getTree()).getSimpleName();
                            } else if (depArg instanceof K.StringTemplate) {
                                K.StringTemplate template = (K.StringTemplate) depArg;
                                List<J> strings = template.getStrings();
                                if (strings.size() != 2 || !(strings.get(0) instanceof J.Literal) || !(strings.get(1) instanceof K.StringTemplate.Expression)) {
                                    continue;
                                }
                                J.Literal groupArtifact = (J.Literal) strings.get(0);
                                K.StringTemplate.Expression versionValue = (K.StringTemplate.Expression) strings.get(1);
                                if (!(versionValue.getTree() instanceof J.Identifier) || !(groupArtifact.getValue() instanceof String)) {
                                    continue;
                                }
                                dep = DependencyStringNotationConverter.parse((String) groupArtifact.getValue());
                                versionVariableName = ((J.Identifier) versionValue.getTree()).getSimpleName();
                            }
                            if (dep == null || versionVariableName == null) {
                                continue;
                            }
                            GroupArtifact ga = new GroupArtifact(dep.getGroupId(), dep.getArtifactId());
                            if (gradleProject != null) {
                                acc.getConfigurationPerGAPerModule().computeIfAbsent(getGradleProjectKey(gradleProject), k -> new HashMap<>())
                                        .computeIfAbsent(ga, k -> new HashSet<>())
                                        .add(m.getSimpleName());
                            }
                            if (acc.gaToNewVersion.containsKey(ga) || !shouldResolveVersion(dep.getGroupId(), dep.getArtifactId())) {
                                continue;
                            }
                            try {
                                String resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                        .select(new GroupArtifact(dep.getGroupId(), dep.getArtifactId()), m.getSimpleName(), newVersion, versionPattern, ctx);
                                if (resolvedVersion != null) {
                                    acc.versionPropNameToGA.put(versionVariableName, ga);
                                    acc.gaToNewVersion.put(ga, resolvedVersion);
                                }
                            } catch (MavenDownloadingException e) {
                                acc.gaToNewVersion.put(ga, e);
                            }
                        }
                    }
                }
                return m;
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
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(DependencyVersionState acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            private final UpdateGradle updateGradle = new UpdateGradle(acc);
            private final UpdateProperties updateProperties = new UpdateProperties(acc);
            private final UpdateDependencyLock updateLockFile = new UpdateDependencyLock(acc.modules);

            @Override
            public boolean isAcceptable(SourceFile sf, ExecutionContext ctx) {
                return updateProperties.isAcceptable(sf, ctx) || updateGradle.isAcceptable(sf, ctx) || updateLockFile.isAcceptable(sf, ctx);
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
                    if ((tree != t || updateLockFile.isAcceptable(sf, ctx)) && projectMarker.isPresent()) {
                        GradleProject gradleProject = projectMarker.get();
                        Map<GroupArtifact, Set<String>> configurationsPerGa = acc.getConfigurationPerGAPerModule().getOrDefault(getGradleProjectKey(gradleProject), emptyMap());
                        if (acc.getGaToNewVersion().isEmpty()) {
                            DependencyMatcher matcher = new DependencyMatcher(groupId, artifactId, null);
                            DependencyVersionSelector versionSelector = new DependencyVersionSelector(metadataFailures, gradleProject, null);
                            for (GroupArtifact groupArtifact : configurationsPerGa.keySet()) {
                                if (!matcher.matches(groupArtifact.getGroupId(), groupArtifact.getArtifactId())) {
                                    continue;
                                }
                                try {
                                    String selectedVersion = versionSelector.select(groupArtifact, null, newVersion, versionPattern, ctx);
                                    GroupArtifactVersion gav = new GroupArtifactVersion(groupArtifact.getGroupId(), groupArtifact.getArtifactId(), selectedVersion);
                                    gradleProject = replaceVersion(gradleProject, ctx, gav, configurationsPerGa.getOrDefault(gav.asGroupArtifact(), emptySet()));
                                } catch (MavenDownloadingException ignore) {}
                            }

                        } else {
                            for (Map.Entry<GroupArtifact, Object> newVersion : acc.getGaToNewVersion().entrySet()) {
                                if (newVersion.getValue() instanceof String) {
                                    GroupArtifactVersion gav = new GroupArtifactVersion(newVersion.getKey().getGroupId(), newVersion.getKey().getArtifactId(), (String) newVersion.getValue());
                                    gradleProject = replaceVersion(gradleProject, ctx, gav, configurationsPerGa.getOrDefault(gav.asGroupArtifact(), emptySet()));
                                }
                            }
                        }
                        if (projectMarker.get() != gradleProject) {
                            t = t.withMarkers(t.getMarkers().setByType(gradleProject));
                        }
                    }
                    if (updateLockFile.isAcceptable(sf, ctx)) {
                        t = updateLockFile.visitNonNull(t, ctx);
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
                GroupArtifact ga = acc.versionPropNameToGA.get(entry.getKey());
                if (ga == null || !dependencyMatcher.matches(ga.getGroupId(), ga.getArtifactId())) {
                    return entry;
                }
                Object result = acc.gaToNewVersion.get(ga);
                if (result == null || result instanceof Exception) {
                    return entry;
                }
                VersionComparator versionComparator = Semver.validate(StringUtils.isBlank(newVersion) ? "latest.release" : newVersion, versionPattern).getValue();
                if (versionComparator == null) {
                    return entry;
                }
                Optional<String> finalVersion = versionComparator.upgrade(entry.getValue().getText(), singletonList((String) result));
                return finalVersion.map(v -> entry.withValue(entry.getValue().withText(v))).orElse(entry);
            }
            return entry;
        }
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
                Map<String, Map<GroupArtifact, Set<String>>> variableNames = getCursor().getMessage(VERSION_VARIABLE_KEY);
                if (variableNames != null) {
                    cu = (JavaSourceFile) new UpdateVariable(variableNames, gradleProject).visitNonNull(cu, ctx);
                }
                return cu;
            }
            return tree;
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
            GradleDependency.Matcher gradleDependencyMatcher = Traits.gradleDependency();

            if (gradleDependencyMatcher.get(getCursor()).isPresent()) {
                List<Expression> depArgs = m.getArguments();
                if (depArgs.get(0) instanceof J.Literal || depArgs.get(0) instanceof G.GString || depArgs.get(0) instanceof G.MapEntry || depArgs.get(0) instanceof J.Assignment || depArgs.get(0) instanceof K.StringTemplate) {
                    m = updateDependency(m, ctx);
                } else if (depArgs.get(0) instanceof J.MethodInvocation &&
                        (((J.MethodInvocation) depArgs.get(0)).getSimpleName().equals("platform") ||
                                ((J.MethodInvocation) depArgs.get(0)).getSimpleName().equals("enforcedPlatform"))) {
                    m = m.withArguments(ListUtils.mapFirst(depArgs, platform -> updateDependency((J.MethodInvocation) platform, ctx)));
                }
            }
            return m;
        }

        private J.MethodInvocation updateDependency(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = method;
            m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                if (arg instanceof G.GString) {
                    G.GString gString = (G.GString) arg;
                    List<J> strings = gString.getStrings();
                    if (strings.size() != 2 || !(strings.get(0) instanceof J.Literal) || !(strings.get(1) instanceof G.GString.Value)) {
                        return arg;
                    }
                    J.Literal groupArtifact = (J.Literal) strings.get(0);
                    G.GString.Value versionValue = (G.GString.Value) strings.get(1);
                    if (!(versionValue.getTree() instanceof J.Identifier) || !(groupArtifact.getValue() instanceof String)) {
                        return arg;
                    }
                    Dependency dep = DependencyStringNotationConverter.parse((String) groupArtifact.getValue());
                    if (dep != null && dependencyMatcher.matches(dep.getGroupId(), dep.getArtifactId())) {
                        Object scanResult = acc.gaToNewVersion.get(new GroupArtifact(dep.getGroupId(), dep.getArtifactId()));
                        if (scanResult instanceof Exception) {
                            getCursor().putMessage(UPDATE_VERSION_ERROR_KEY, scanResult);
                            return arg;
                        }

                        String versionVariableName = ((J.Identifier) versionValue.getTree()).getSimpleName();
                        getCursor().dropParentUntil(p -> p instanceof SourceFile)
                                .computeMessageIfAbsent(VERSION_VARIABLE_KEY, v -> new HashMap<String, Map<GroupArtifact, Set<String>>>())
                                .computeIfAbsent(versionVariableName, it -> new HashMap<>())
                                .computeIfAbsent(new GroupArtifact(dep.getGroupId(), dep.getArtifactId()), it -> new HashSet<>())
                                .add(method.getSimpleName());
                    }
                } else if (arg instanceof K.StringTemplate) {
                    K.StringTemplate template = (K.StringTemplate) arg;
                    List<J> strings = template.getStrings();
                    if (strings.size() != 2 || !(strings.get(0) instanceof J.Literal) || !(strings.get(1) instanceof K.StringTemplate.Expression)) {
                        return arg;
                    }
                    J.Literal groupArtifact = (J.Literal) strings.get(0);
                    K.StringTemplate.Expression versionValue = (K.StringTemplate.Expression) strings.get(1);
                    if (!(versionValue.getTree() instanceof J.Identifier) || !(groupArtifact.getValue() instanceof String)) {
                        return arg;
                    }
                    Dependency dep = DependencyStringNotationConverter.parse((String) groupArtifact.getValue());
                    if (dep != null && dependencyMatcher.matches(dep.getGroupId(), dep.getArtifactId())) {
                        Object scanResult = acc.gaToNewVersion.get(new GroupArtifact(dep.getGroupId(), dep.getArtifactId()));
                        if (scanResult instanceof Exception) {
                            getCursor().putMessage(UPDATE_VERSION_ERROR_KEY, scanResult);
                            return arg;
                        }

                        String versionVariableName = ((J.Identifier) versionValue.getTree()).getSimpleName();
                        getCursor().dropParentUntil(p -> p instanceof SourceFile)
                                .computeMessageIfAbsent(VERSION_VARIABLE_KEY, v -> new HashMap<String, Map<GroupArtifact, Set<String>>>())
                                .computeIfAbsent(versionVariableName, it -> new HashMap<>())
                                .computeIfAbsent(new GroupArtifact(dep.getGroupId(), dep.getArtifactId()), it -> new HashSet<>())
                                .add(method.getSimpleName());
                    }
                } else if (arg instanceof J.Literal) {
                    J.Literal literal = (J.Literal) arg;
                    if (literal.getType() != JavaType.Primitive.String) {
                        return arg;
                    }
                    String gav = (String) literal.getValue();
                    if (gav == null) {
                        getCursor().putMessage(UPDATE_VERSION_ERROR_KEY, new IllegalStateException("Unable to update version"));
                        return arg;
                    }
                    Dependency dep = DependencyStringNotationConverter.parse(gav);
                    if (dep != null && dependencyMatcher.matches(dep.getGroupId(), dep.getArtifactId()) &&
                            dep.getVersion() != null &&
                            !dep.getVersion().startsWith("$")) {
                        Object scanResult = acc.gaToNewVersion.get(new GroupArtifact(dep.getGroupId(), dep.getArtifactId()));
                        if (scanResult instanceof Exception) {
                            getCursor().putMessage(UPDATE_VERSION_ERROR_KEY, scanResult);
                            return arg;
                        }

                        try {
                            String selectedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                    .select(dep.getGav(), method.getSimpleName(), newVersion, versionPattern, ctx);
                            if (selectedVersion == null || dep.getVersion().equals(selectedVersion)) {
                                return arg;
                            }

                            String newGav = dep
                                    .withVersion(selectedVersion)
                                    .toStringNotation();
                            return literal
                                    .withValue(newGav)
                                    .withValueSource(literal.getValueSource() == null ? newGav : literal.getValueSource().replace(gav, newGav));
                        } catch (MavenDownloadingException e) {
                            getCursor().putMessage(UPDATE_VERSION_ERROR_KEY, e);
                        }
                    }
                }
                return arg;
            }));
            Exception err = getCursor().pollMessage(UPDATE_VERSION_ERROR_KEY);
            if (err != null) {
                m = Markup.warn(m, err);
            }
            List<Expression> depArgs = m.getArguments();
            if (depArgs.size() >= 3) {
                if (depArgs.get(0) instanceof G.MapEntry &&
                        depArgs.get(1) instanceof G.MapEntry &&
                        depArgs.get(2) instanceof G.MapEntry) {
                    Expression groupValue = ((G.MapEntry) depArgs.get(0)).getValue();
                    Expression artifactValue = ((G.MapEntry) depArgs.get(1)).getValue();
                    if (!(groupValue instanceof J.Literal) || !(artifactValue instanceof J.Literal)) {
                        return m;
                    }
                    J.Literal groupLiteral = (J.Literal) groupValue;
                    J.Literal artifactLiteral = (J.Literal) artifactValue;
                    if (groupLiteral.getValue() == null || artifactLiteral.getValue() == null || !dependencyMatcher.matches((String) groupLiteral.getValue(), (String) artifactLiteral.getValue())) {
                        return m;
                    }
                    Object scanResult = acc.gaToNewVersion.get(new GroupArtifact((String) groupLiteral.getValue(), (String) artifactLiteral.getValue()));
                    if (scanResult instanceof Exception) {
                        return Markup.warn(m, (Exception) scanResult);
                    }
                    G.MapEntry versionEntry = (G.MapEntry) depArgs.get(2);
                    Expression versionExp = versionEntry.getValue();
                    if (versionExp instanceof J.Literal && ((J.Literal) versionExp).getValue() instanceof String) {
                        J.Literal versionLiteral = (J.Literal) versionExp;
                        String version = (String) versionLiteral.getValue();
                        if (version.startsWith("$")) {
                            return m;
                        }
                        String selectedVersion;
                        try {
                            GroupArtifactVersion gav = new GroupArtifactVersion((String) groupLiteral.getValue(), (String) artifactLiteral.getValue(), version);
                            selectedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                    .select(gav, m.getSimpleName(), newVersion, versionPattern, ctx);
                        } catch (MavenDownloadingException e) {
                            return e.warn(m);
                        }
                        if (selectedVersion == null || version.equals(selectedVersion)) {
                            return m;
                        }
                        List<Expression> newArgs = new ArrayList<>(3);
                        newArgs.add(depArgs.get(0));
                        newArgs.add(depArgs.get(1));
                        newArgs.add(versionEntry.withValue(
                                versionLiteral
                                        .withValueSource(versionLiteral.getValueSource() == null ?
                                                selectedVersion :
                                                versionLiteral.getValueSource().replace(version, selectedVersion))
                                        .withValue(selectedVersion)));
                        newArgs.addAll(depArgs.subList(3, depArgs.size()));

                        return m.withArguments(newArgs);
                    } else if (versionExp instanceof J.Identifier) {
                        String versionVariableName = ((J.Identifier) versionExp).getSimpleName();
                        getCursor().dropParentUntil(p -> p instanceof SourceFile)
                                .computeMessageIfAbsent(VERSION_VARIABLE_KEY, v -> new HashMap<String, Map<GroupArtifact, Set<String>>>())
                                .computeIfAbsent(versionVariableName, it -> new HashMap<>())
                                .computeIfAbsent(new GroupArtifact((String) groupLiteral.getValue(), (String) artifactLiteral.getValue()), it -> new HashSet<>())
                                .add(m.getSimpleName());
                    }
                } else if (depArgs.get(0) instanceof J.Assignment &&
                        depArgs.get(1) instanceof J.Assignment &&
                        depArgs.get(2) instanceof J.Assignment) {
                    Expression groupValue = ((J.Assignment) depArgs.get(0)).getAssignment();
                    Expression artifactValue = ((J.Assignment) depArgs.get(1)).getAssignment();
                    if (!(groupValue instanceof J.Literal) || !(artifactValue instanceof J.Literal)) {
                        return m;
                    }
                    J.Literal groupLiteral = (J.Literal) groupValue;
                    J.Literal artifactLiteral = (J.Literal) artifactValue;
                    if (groupLiteral.getValue() == null || artifactLiteral.getValue() == null || !dependencyMatcher.matches((String) groupLiteral.getValue(), (String) artifactLiteral.getValue())) {
                        return m;
                    }
                    Object scanResult = acc.gaToNewVersion.get(new GroupArtifact((String) groupLiteral.getValue(), (String) artifactLiteral.getValue()));
                    if (scanResult instanceof Exception) {
                        return Markup.warn(m, (Exception) scanResult);
                    }
                    K.Assignment versionEntry = (J.Assignment) depArgs.get(2);
                    Expression versionExp = versionEntry.getAssignment();
                    if (versionExp instanceof J.Literal && ((J.Literal) versionExp).getValue() instanceof String) {
                        J.Literal versionLiteral = (J.Literal) versionExp;
                        String version = (String) versionLiteral.getValue();
                        if (version.startsWith("$")) {
                            return m;
                        }
                        String selectedVersion;
                        try {
                            GroupArtifactVersion gav = new GroupArtifactVersion((String) groupLiteral.getValue(), (String) artifactLiteral.getValue(), version);
                            selectedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                    .select(gav, m.getSimpleName(), newVersion, versionPattern, ctx);
                        } catch (MavenDownloadingException e) {
                            return e.warn(m);
                        }
                        if (selectedVersion == null || version.equals(selectedVersion)) {
                            return m;
                        }
                        List<Expression> newArgs = new ArrayList<>(3);
                        newArgs.add(depArgs.get(0));
                        newArgs.add(depArgs.get(1));
                        newArgs.add(versionEntry.withAssignment(
                                versionLiteral
                                        .withValueSource(versionLiteral.getValueSource() == null ?
                                                selectedVersion :
                                                versionLiteral.getValueSource().replace(version, selectedVersion))
                                        .withValue(selectedVersion)));
                        newArgs.addAll(depArgs.subList(3, depArgs.size()));

                        return m.withArguments(newArgs);
                    } else if (versionExp instanceof J.Identifier) {
                        String versionVariableName = ((J.Identifier) versionExp).getSimpleName();
                        getCursor().dropParentUntil(p -> p instanceof SourceFile)
                                .computeMessageIfAbsent(VERSION_VARIABLE_KEY, v -> new HashMap<String, Map<GroupArtifact, Set<String>>>())
                                .computeIfAbsent(versionVariableName, it -> new HashMap<>())
                                .computeIfAbsent(new GroupArtifact((String) groupLiteral.getValue(), (String) artifactLiteral.getValue()), it -> new HashSet<>())
                                .add(m.getSimpleName());
                    }
                }
            }

            return m;
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

    public static GradleProject replaceVersion(GradleProject gp, ExecutionContext ctx, GroupArtifactVersion gav, Set<String> configurations) {
        try {
            //noinspection ConstantValue
            if (gav.getGroupId() == null || gav.getArtifactId() == null) {
                return gp;
            }

            Set<String> remainingConfigurations = new HashSet<>(configurations);
            remainingConfigurations.remove("classpath");

            if (remainingConfigurations.isEmpty()) {
                return gp;
            }

            MavenPomDownloader mpd = new MavenPomDownloader(ctx);
            Pom pom = mpd.download(gav, null, null, gp.getMavenRepositories());
            ResolvedPom resolvedPom = pom.resolve(emptyList(), mpd, gp.getMavenRepositories(), ctx);
            List<ResolvedDependency> transitiveDependencies = resolvedPom.resolveDependencies(Scope.Runtime, mpd, ctx);
            org.openrewrite.maven.tree.Dependency newRequested = org.openrewrite.maven.tree.Dependency.builder()
                    .gav(gav)
                    .build();
            ResolvedDependency newDep = ResolvedDependency.builder()
                    .gav(resolvedPom.getGav())
                    .requested(newRequested)
                    .dependencies(transitiveDependencies)
                    .build();

            Map<String, GradleDependencyConfiguration> nameToConfiguration = gp.getNameToConfiguration();
            Map<String, GradleDependencyConfiguration> newNameToConfiguration = new HashMap<>(nameToConfiguration.size());
            boolean anyChanged = false;
            for (GradleDependencyConfiguration gdc : nameToConfiguration.values()) {
                GradleDependencyConfiguration newGdc = gdc
                        .withRequested(ListUtils.map(gdc.getRequested(), requested -> maybeUpdateDependency(requested, newRequested)))
                        .withDirectResolved(ListUtils.map(gdc.getDirectResolved(), resolved -> maybeUpdateResolvedDependency(gp, resolved, newDep, new HashSet<>())));
                if (hasBomWithoutDependencies(newDep)) {
                    for (ResolvedManagedDependency resolvedDependency : resolvedPom.getDependencyManagement()) {
                        ResolvedGroupArtifactVersion resolvedGav = new ResolvedGroupArtifactVersion(null, resolvedDependency.getGroupId(), resolvedDependency.getArtifactId(), resolvedDependency.getVersion(), null);
                        newGdc = newGdc.withDirectResolved(ListUtils.map(newGdc.getDirectResolved(), resolved -> maybeUpdateManagedResolvedDependency(gp, resolved, resolvedGav, new HashSet<>())));
                    }
                }
                anyChanged |= newGdc != gdc;
                newNameToConfiguration.put(newGdc.getName(), newGdc);
            }
            if (anyChanged) {
                return gp.withNameToConfiguration(newNameToConfiguration);
            }
        } catch (MavenDownloadingException | MavenDownloadingExceptions e) {
            return gp;
        }
        return gp;
    }

    private static org.openrewrite.maven.tree.Dependency maybeUpdateDependency(
            org.openrewrite.maven.tree.Dependency dep,
            org.openrewrite.maven.tree.Dependency newDep) {
        if (Objects.equals(dep.getGroupId(), newDep.getGroupId()) && Objects.equals(dep.getArtifactId(), newDep.getArtifactId()) && Objects.equals(dep.getVersion(), newDep.getVersion())) {
            return dep;
        }
        if (Objects.equals(dep.getGroupId(), newDep.getGroupId()) && Objects.equals(dep.getArtifactId(), newDep.getArtifactId())) {
            return newDep;
        }
        return dep;
    }

    private static ResolvedDependency maybeUpdateManagedResolvedDependency(
            GradleProject gp,
            ResolvedDependency dep,
            ResolvedGroupArtifactVersion gav,
            Set<ResolvedDependency> traversalHistory) {
        if (traversalHistory.contains(dep)) {
            return dep;
        }
        if (Objects.equals(dep.getGroupId(), gav.getGroupId()) && Objects.equals(dep.getArtifactId(), gav.getArtifactId()) && Objects.equals(dep.getVersion(), gav.getVersion()) ||
                wouldDowngrade(dep.getGav().asGroupArtifactVersion(), gav.asGroupArtifactVersion())) {
            return dep;
        }
        if (Objects.equals(dep.getGroupId(), gav.getGroupId()) && Objects.equals(dep.getArtifactId(), gav.getArtifactId())) {
            if (!isSpringPluginManaged(gp, dep)) {
                return dep.withGav(gav);
            }
        }
        boolean added = traversalHistory.add(dep);
        try {
            return dep.withDependencies(ListUtils.map(dep.getDependencies(), d -> maybeUpdateManagedResolvedDependency(gp, d, gav, traversalHistory)));
        } finally {
            if (added) {
                traversalHistory.remove(dep);
            }
        }
    }

    private static ResolvedDependency maybeUpdateResolvedDependency(
            GradleProject gp,
            ResolvedDependency dep,
            ResolvedDependency newDep,
            Set<ResolvedDependency> traversalHistory) {
        if (traversalHistory.contains(dep)) {
            return dep;
        }
        if (Objects.equals(dep.getGroupId(), newDep.getGroupId()) && Objects.equals(dep.getArtifactId(), newDep.getArtifactId()) && Objects.equals(dep.getVersion(), newDep.getVersion()) ||
                wouldDowngrade(dep.getGav().asGroupArtifactVersion(), newDep.getGav().asGroupArtifactVersion())) {
            return dep;
        }
        if (Objects.equals(dep.getGroupId(), newDep.getGroupId()) && Objects.equals(dep.getArtifactId(), newDep.getArtifactId())) {
            if (isSpringPluginManaged(gp, newDep)) {
                //Spring plugin overwrites versions that are not set in constraints or similar blocks so the transitive dependencies would remain the same except for the current lib.
                return dep.withGav(newDep.getGav());
            }
            return newDep;
        }
        boolean added = traversalHistory.add(dep);
        try {
            return dep.withDependencies(ListUtils.map(dep.getDependencies(), d -> maybeUpdateResolvedDependency(gp, d, newDep, traversalHistory)));
        } finally {
            if (added) {
                traversalHistory.remove(dep);
            }
        }
    }

    private static boolean wouldDowngrade(GroupArtifactVersion from, GroupArtifactVersion to) {
        if (from.getGroupId().equals(to.getGroupId()) && from.getArtifactId().equals(to.getArtifactId())) {
            Version fromVersion = new Version(from.getVersion());
            Version toVersion = new Version(to.getVersion());
            return fromVersion.compareTo(toVersion) > 0;
        }
        return false;
    }

    private static boolean isSpringPluginManaged(GradleProject gp, ResolvedDependency newDep) {
        if (gp.getPlugins().stream().anyMatch(plugin -> "io.spring.dependency-management".equals(plugin.getId()))) {
            //TODO build a more complete list of dependencies that are managed by the spring plugin so that the sub-dependencies of the managed ones are not updated as these are most likely also managed by the plugin.
            return newDep.getGroupId().startsWith("org.springframework");
        }
        return false;
    }

    // Some dependencies like jackson-bom do not publish a .module file and only contain dependencyManagement section.
    // In that case we want to update the versions of the managed dependencies.
    // If a module file is pushed, and it contains dependencies, we do not need to resolve using dependencyManagement as the bom will have dependencies on the overridden versions.
    static boolean hasBomWithoutDependencies(ResolvedDependency dep) {
        if ("bom".equals(dep.getType()) && dep.getDependencies().isEmpty()) {
            return true;
        }
        for (ResolvedDependency d : dep.getDependencies()) {
            if (hasBomWithoutDependencies(d)) {
                return true;
            }
        }
        return false;
    }

    static String getGradleProjectKey(GradleProject project) {
        if (StringUtils.isBlank(project.getGroup())) {
            return project.getName();
        }
        if (":".equals(project.getPath())) {
            return project.getGroup();
        }
        return project.getGroup() + project.getPath();
    }
}
