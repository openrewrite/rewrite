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
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.util.ChangeStringLiteral;
import org.openrewrite.gradle.util.Dependency;
import org.openrewrite.gradle.util.DependencyStringNotationConverter;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeDependencyVersion extends ScanningRecipe<UpgradeDependencyVersion.DependencyVersionState> {
    private static final String VERSION_VARIABLE_KEY = "VERSION_VARIABLE";
    private static final String NEW_VERSION_KEY = "NEW_VERSION";
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
               "* `String` notation: `\"group:artifact:version\"` \n" +
               "* `Map` notation: `group: 'group', name: 'artifact', version: 'version'`\n" +
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
    }

    @Override
    public DependencyVersionState getInitialValue(ExecutionContext ctx) {
        return new DependencyVersionState();
    }

    private static final MethodMatcher DEPENDENCY_DSL_MATCHER = new MethodMatcher("DependencyHandlerSpec *(..)");

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(DependencyVersionState acc) {

        return new GroovyVisitor<ExecutionContext>() {
            GradleProject gradleProject;
            final DependencyMatcher dependencyMatcher = new DependencyMatcher(groupId, artifactId, null);

            @Override
            public J visitCompilationUnit(G.CompilationUnit cu, ExecutionContext executionContext) {
                gradleProject = cu.getMarkers().findFirst(GradleProject.class).orElse(null);
                if (gradleProject == null) {
                    return cu;
                }
                return super.visitCompilationUnit(cu, executionContext);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (DEPENDENCY_DSL_MATCHER.matches(m)) {
                    if (m.getArguments().get(0) instanceof G.MapEntry) {
                        String groupId = null;
                        String artifactId = null;
                        String version = null;

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
                                    groupId = valueValue;
                                    break;
                                case "name":
                                    artifactId = valueValue;
                                    break;
                                case "version":
                                    version = valueValue;
                                    break;
                            }
                        }
                        if (groupId == null || artifactId == null) {
                            return m;
                        }

                        if (!dependencyMatcher.matches(groupId, artifactId)) {
                            return m;
                        }
                        String versionVariableName = version;
                        GroupArtifact ga = new GroupArtifact(groupId, artifactId);
                        if (acc.gaToNewVersion.containsKey(ga)) {
                            return m;
                        }
                        try {
                            String resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                    .select(new GroupArtifact(groupId, artifactId), m.getSimpleName(), newVersion, versionPattern, ctx);
                            acc.versionPropNameToGA.put(versionVariableName, ga);
                            acc.gaToNewVersion.put(ga, resolvedVersion);
                        } catch (MavenDownloadingException e) {
                            acc.gaToNewVersion.put(ga, e);
                            return m;
                        }
                    } else {
                        for (Expression depArg : m.getArguments()) {
                            if (depArg instanceof G.GString) {
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
                                Dependency dep = DependencyStringNotationConverter.parse((String) groupArtifact.getValue());
                                if (!dependencyMatcher.matches(dep.getGroupId(), dep.getArtifactId())) {
                                    continue;
                                }
                                String versionVariableName = ((J.Identifier) versionValue.getTree()).getSimpleName();
                                GroupArtifact ga = new GroupArtifact(dep.getGroupId(), dep.getArtifactId());
                                if (acc.gaToNewVersion.containsKey(ga)) {
                                    continue;
                                }
                                try {
                                    String resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                            .select(new GroupArtifact(dep.getGroupId(), dep.getArtifactId()), m.getSimpleName(), newVersion, versionPattern, ctx);
                                    acc.versionPropNameToGA.put(versionVariableName, ga);
                                    acc.gaToNewVersion.put(ga, resolvedVersion);
                                } catch (MavenDownloadingException e) {
                                    acc.gaToNewVersion.put(ga, e);
                                }
                            }
                        }
                    }
                }
                return m;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(DependencyVersionState acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree t, ExecutionContext ctx) {
                if (t instanceof Properties) {
                    t = new UpdateProperties(acc).visitNonNull(t, ctx);
                } else if (t instanceof G.CompilationUnit) {
                    t = new UpdateGroovy(acc).visitNonNull(t, ctx);
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
        public Properties visitFile(Properties.File file, ExecutionContext executionContext) {
            if(!file.getSourcePath().endsWith(GRADLE_PROPERTIES_FILE_NAME)) {
                return file;
            }
            return super.visitFile(file, executionContext);
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
    private class UpdateGroovy extends GroovyVisitor<ExecutionContext> {
        final DependencyVersionState acc;
        GradleProject gradleProject;
        final DependencyMatcher dependencyMatcher = new DependencyMatcher(groupId, artifactId, null);

        @Override
        public J visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
            gradleProject = cu.getMarkers().findFirst(GradleProject.class)
                    .orElse(null);
            if(gradleProject == null) {
                return cu;
            }
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public J postVisit(J tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile cu = (JavaSourceFile) tree;
                Map<String, Map<GroupArtifact, Set<String>>> variableNames = getCursor().getMessage(VERSION_VARIABLE_KEY);
                if (variableNames != null && gradleProject != null) {
                    cu = (JavaSourceFile) new UpdateVariable(variableNames, gradleProject).visitNonNull(cu, ctx);
                }
                Map<GroupArtifactVersion, Set<String>> versionUpdates = getCursor().getMessage(NEW_VERSION_KEY);
                if (versionUpdates != null && gradleProject != null) {
                    GradleProject newGp = gradleProject;
                    for (Map.Entry<GroupArtifactVersion, Set<String>> gavToConfigurations : versionUpdates.entrySet()) {
                        newGp = replaceVersion(newGp, ctx, gavToConfigurations.getKey(), gavToConfigurations.getValue());
                    }
                    cu = cu.withMarkers(cu.getMarkers().removeByType(GradleProject.class).add(newGp));
                }
                return cu;
            }
            return tree;
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
            if (DEPENDENCY_DSL_MATCHER.matches(m)) {
                List<Expression> depArgs = m.getArguments();
                if (depArgs.get(0) instanceof J.Literal || depArgs.get(0) instanceof G.GString || depArgs.get(0) instanceof G.MapEntry) {
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
                    if (dependencyMatcher.matches(dep.getGroupId(), dep.getArtifactId())) {
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
                    String gav = (String) literal.getValue();
                    if (gav == null) {
                        getCursor().putMessage(UPDATE_VERSION_ERROR_KEY, new IllegalStateException("Unable to update version"));
                        return arg;
                    }
                    Dependency dep = DependencyStringNotationConverter.parse(gav);
                    if (dependencyMatcher.matches(dep.getGroupId(), dep.getArtifactId())
                        && dep.getVersion() != null
                        && !dep.getVersion().startsWith("$")) {
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
                            getCursor().dropParentUntil(p -> p instanceof SourceFile)
                                    .computeMessageIfAbsent(NEW_VERSION_KEY, it -> new HashMap<GroupArtifactVersion, Set<String>>())
                                    .computeIfAbsent(new GroupArtifactVersion(dep.getGroupId(), dep.getArtifactId(), selectedVersion), it -> new HashSet<>())
                                    .add(method.getSimpleName());

                            String newGav = dep
                                    .withVersion(selectedVersion)
                                    .toStringNotation();
                            return literal
                                    .withValue(newGav)
                                    .withValueSource(requireNonNull(literal.getValueSource()).replace(gav, newGav));
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
            if (depArgs.size() >= 3 && depArgs.get(0) instanceof G.MapEntry
                && depArgs.get(1) instanceof G.MapEntry
                && depArgs.get(2) instanceof G.MapEntry) {
                Expression groupValue = ((G.MapEntry) depArgs.get(0)).getValue();
                Expression artifactValue = ((G.MapEntry) depArgs.get(1)).getValue();
                if (!(groupValue instanceof J.Literal) || !(artifactValue instanceof J.Literal)) {
                    return m;
                }
                J.Literal groupLiteral = (J.Literal) groupValue;
                J.Literal artifactLiteral = (J.Literal) artifactValue;
                //noinspection DataFlowIssue
                if (!dependencyMatcher.matches((String) groupLiteral.getValue(), (String) artifactLiteral.getValue())) {
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
                                    .withValueSource(requireNonNull(versionLiteral.getValueSource()).replace(version, selectedVersion))
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

            return m;
        }
    }

    @RequiredArgsConstructor
    private class UpdateVariable extends GroovyIsoVisitor<ExecutionContext> {
        private final Map<String, Map<GroupArtifact, Set<String>>> versionVariableNames;
        private final GradleProject gradleProject;

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
            J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);
            boolean noneMatch = true;
            Map<GroupArtifact, Set<String>> gaToConfigurations = null;
            for (Map.Entry<String, Map<GroupArtifact, Set<String>>> versionVariableNameEntry : versionVariableNames.entrySet()) {
                if (versionVariableNameEntry.getKey().equals((v.getSimpleName()))) {
                    noneMatch = false;
                    gaToConfigurations = versionVariableNameEntry.getValue();
                    break;
                }
            }
            if (noneMatch) {
                return v;
            }
            if (!(v.getInitializer() instanceof J.Literal)) {
                return v;
            }
            J.Literal initializer = (J.Literal) v.getInitializer();
            if (initializer.getType() != JavaType.Primitive.String) {
                return v;
            }
            String version = (String) initializer.getValue();
            if (version == null) {
                return v;
            }

            try {
                for (Map.Entry<GroupArtifact, Set<String>> gaEntry : gaToConfigurations.entrySet()) {
                    GroupArtifact ga = gaEntry.getKey();
                    GroupArtifactVersion gav = new GroupArtifactVersion(ga.getGroupId(), ga.getArtifactId(), version);
                    DependencyVersionSelector selector = new DependencyVersionSelector(metadataFailures, gradleProject, null);
                    String selectedVersion = Optional.ofNullable(selector.select(gav, null, newVersion, versionPattern, ctx))
                            .orElse(selector.select(gav, "classpath", newVersion, versionPattern, ctx));
                    if (selectedVersion == null) {
                        return v;
                    }
                    getCursor().dropParentUntil(p -> p instanceof SourceFile)
                            .computeMessageIfAbsent(NEW_VERSION_KEY, m -> new HashMap<GroupArtifactVersion, Set<String>>())
                            .computeIfAbsent(new GroupArtifactVersion(ga.getGroupId(), ga.getArtifactId(), selectedVersion), it -> new HashSet<>())
                            .addAll(gaEntry.getValue());

                    J.Literal newVersionLiteral = ChangeStringLiteral.withStringValue(initializer, selectedVersion);
                    v = v.withInitializer(newVersionLiteral);
                }
            } catch (MavenDownloadingException e) {
                return e.warn(v);
            }
            return v;
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
            J.Assignment a = super.visitAssignment(assignment, ctx);
            if (!(a.getVariable() instanceof J.Identifier)) {
                return a;
            }
            J.Identifier identifier = (J.Identifier) a.getVariable();
            Map<GroupArtifact, Set<String>> gaToConfigurations = null;
            boolean noneMatch = true;
            for (Map.Entry<String, Map<GroupArtifact, Set<String>>> versionVariableNameEntry : versionVariableNames.entrySet()) {
                if (versionVariableNameEntry.getKey().equals(identifier.getSimpleName())) {
                    noneMatch = false;
                    gaToConfigurations = versionVariableNameEntry.getValue();
                    break;
                }
            }
            if (noneMatch) {
                return a;
            }
            if (!(a.getAssignment() instanceof J.Literal)) {
                return a;
            }
            J.Literal literal = (J.Literal) a.getAssignment();
            if (literal.getType() != JavaType.Primitive.String) {
                return a;
            }
            String version = (String) literal.getValue();
            if (version == null) {
                return a;
            }

            try {
                for (Map.Entry<GroupArtifact, Set<String>> gaEntry : gaToConfigurations.entrySet()) {
                    GroupArtifact ga = gaEntry.getKey();
                    GroupArtifactVersion gav = new GroupArtifactVersion(ga.getGroupId(), ga.getArtifactId(), version);
                    DependencyVersionSelector selector = new DependencyVersionSelector(metadataFailures, gradleProject, null);
                    String selectedVersion = Optional.ofNullable(selector.select(gav, null, newVersion, versionPattern, ctx))
                            .orElse(selector.select(gav, "classpath", newVersion, versionPattern, ctx));
                    if (selectedVersion == null) {
                        return a;
                    }
                    getCursor().dropParentUntil(p -> p instanceof SourceFile)
                            .computeMessageIfAbsent(NEW_VERSION_KEY, m -> new HashMap<GroupArtifactVersion, Set<String>>())
                            .computeIfAbsent(new GroupArtifactVersion(ga.getGroupId(), ga.getArtifactId(), selectedVersion), it -> new HashSet<>())
                            .addAll(gaEntry.getValue());

                    J.Literal newVersionLiteral = ChangeStringLiteral.withStringValue(literal, selectedVersion);
                    a = a.withAssignment(newVersionLiteral);
                }
            } catch (MavenDownloadingException e) {
                return e.warn(a);
            }
            return a;
        }
    }

    static GradleProject replaceVersion(GradleProject gp, ExecutionContext ctx, GroupArtifactVersion gav, Set<String> configurations) {
        try {
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
            ResolvedGroupArtifactVersion resolvedGav = resolvedPom.getGav();
            List<ResolvedDependency> transitiveDependencies = resolvedPom.resolveDependencies(Scope.Runtime, mpd, ctx);
            Map<String, GradleDependencyConfiguration> nameToConfiguration = gp.getNameToConfiguration();
            Map<String, GradleDependencyConfiguration> newNameToConfiguration = new HashMap<>(nameToConfiguration.size());
            boolean anyChanged = false;
            for (GradleDependencyConfiguration gdc : nameToConfiguration.values()) {
                GradleDependencyConfiguration newGdc = gdc;
                newGdc = newGdc.withRequested(ListUtils.map(gdc.getRequested(), requested -> {
                    if (!Objects.equals(requested.getGroupId(), gav.getGroupId()) || !Objects.equals(requested.getArtifactId(), gav.getArtifactId())) {
                        return requested;
                    }
                    return requested.withGav(gav);
                }));
                newGdc = newGdc.withDirectResolved(ListUtils.map(gdc.getDirectResolved(), resolved -> {
                    if (!Objects.equals(resolved.getGroupId(), resolvedGav.getGroupId()) || !Objects.equals(resolved.getArtifactId(), resolvedGav.getArtifactId())) {
                        return resolved;
                    }
                    return resolved.withGav(resolvedGav)
                            .withDependencies(transitiveDependencies);
                }));
                anyChanged |= newGdc != gdc;
                newNameToConfiguration.put(newGdc.getName(), newGdc);
            }
            if (anyChanged) {
                gp = gp.withNameToConfiguration(newNameToConfiguration);
            }
        } catch (MavenDownloadingException | MavenDownloadingExceptions e) {
            return gp;
        }
        return gp;
    }
}
