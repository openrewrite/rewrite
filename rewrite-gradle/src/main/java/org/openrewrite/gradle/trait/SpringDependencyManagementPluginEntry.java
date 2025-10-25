/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.gradle.trait;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.DependencyVersionSelector;
import org.openrewrite.gradle.internal.ChangeStringLiteral;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.DependencyNotation;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.trait.Trait;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
public class SpringDependencyManagementPluginEntry implements Trait<J.MethodInvocation> {
    private static final String GROUP = "group";
    private static final String ARTIFACT = "name";
    private static final String VERSION = "version";

    Cursor cursor;

    String group;
    Set<String> artifacts; //As dependencySet can have multiple Entry invocations
    String version;

    public SpringDependencyManagementPluginEntry withGroupArtifactVersion(DependencyMatcher matcher, @Nullable String newGroup, @Nullable String newArtifact, @Nullable String newVersion, @Nullable String versionPattern, MavenMetadataFailures metadataFailures, ExecutionContext ctx) {
        GradleProject gradleProject = getGradleProject();
        if (gradleProject == null) {
            return this;
        }
        ChangeDependencyManagementVisitor changeDependencyManagementVisitor = new ChangeDependencyManagementVisitor(gradleProject, matcher, metadataFailures, this, newGroup, newArtifact, newVersion, versionPattern);
        Cursor newCursor = new Cursor(this.cursor.getParent(), requireNonNull(changeDependencyManagementVisitor.visit(getTree(), ctx)));
        return new SpringDependencyManagementPluginEntry(newCursor, newGroup == null ? group : newGroup, artifacts, version);
    }

    private @Nullable GradleProject getGradleProject() {
        SourceFile sourceFile = cursor.firstEnclosing(SourceFile.class);
        if (sourceFile == null) {
            return null;
        }

        Optional<GradleProject> maybeGp = sourceFile.getMarkers().findFirst(GradleProject.class);
        return maybeGp.orElse(null);
    }

    public static class Matcher extends GradleTraitMatcher<SpringDependencyManagementPluginEntry> {
        private static final MethodMatcher DEPENDENCY_DSL_MATCHER = new MethodMatcher("io.spring.gradle.dependencymanagement.dsl.DependenciesHandler dependency(..)");
        private static final MethodMatcher DEPENDENCY_SET_MATCHER = new MethodMatcher("io.spring.gradle.dependencymanagement.dsl.DependenciesHandler dependencySet(..)");
        private static final MethodMatcher IMPORTS_MATCHER = new MethodMatcher("io.spring.gradle.dependencymanagement.dsl.ImportsHandler mavenBom(..)");

        @Nullable
        protected String groupId;

        @Nullable
        protected String artifactId;

        public Matcher groupId(@Nullable String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Matcher artifactId(@Nullable String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        @Override
        protected @Nullable SpringDependencyManagementPluginEntry test(Cursor cursor) {
            Object object = cursor.getValue();
            if (object instanceof J.MethodInvocation) {
                J.MethodInvocation methodInvocation = (J.MethodInvocation) object;

                if (!withinDependenciesBlock(cursor) && !withinImportsBlock(cursor)) {
                    return null;
                }

                if (!withinDependencyManagementBlock(cursor)) {
                    return null;
                }

                Set<GroupArtifactVersion> dependencies = new HashSet<>();
                if (DEPENDENCY_DSL_MATCHER.matches(methodInvocation, true) || DEPENDENCY_SET_MATCHER.matches(methodInvocation, true)) {
                    Expression argument = methodInvocation.getArguments().get(0);
                    if (argument instanceof J.Literal || argument instanceof G.MapEntry || argument instanceof G.MapLiteral || argument instanceof J.Assignment || (argument instanceof J.MethodInvocation && "mapOf".equals(((J.MethodInvocation) argument).getSimpleName()))) {
                        if (DEPENDENCY_SET_MATCHER.matches(methodInvocation, true)) {
                            dependencies.addAll(parseDependencySet(methodInvocation));
                        } else if (DEPENDENCY_DSL_MATCHER.matches(methodInvocation, true)) {
                            GroupArtifactVersion gav = parseDependency(methodInvocation.getArguments());
                            if (gav != null) {
                                dependencies.add(gav);
                            }
                        }
                    }
                    if (dependencies.stream().anyMatch(dependency -> !matchesGlob(dependency.getGroupId(), groupId) || !matchesGlob(dependency.getArtifactId(), artifactId))) {
                        dependencies.clear();
                    }
                }

                GroupArtifactVersion importedBom = null;
                if (IMPORTS_MATCHER.matches(methodInvocation, true)) {
                    Expression argument = methodInvocation.getArguments().get(0);
                    if (argument instanceof J.Literal || argument instanceof G.GString || argument instanceof G.MapEntry || argument instanceof G.MapLiteral || argument instanceof J.Assignment || argument instanceof K.StringTemplate) {
                        importedBom = parseDependency(methodInvocation.getArguments());
                    }
                    if (importedBom != null && (!matchesGlob(importedBom.getGroupId(), groupId) || !matchesGlob(importedBom.getArtifactId(), artifactId))) {
                        importedBom = null;
                    }
                }

                if (dependencies.isEmpty() && importedBom == null) {
                    return null;
                }

                Optional<GroupArtifactVersion> dependencyGroupVersion = dependencies.stream().findFirst();
                Optional<GroupArtifactVersion> importedBomGroupVersion = Optional.ofNullable(importedBom);
                String group = dependencyGroupVersion.map(GroupArtifactVersion::getGroupId)
                        .orElseGet(() -> importedBomGroupVersion.map(GroupArtifactVersion::getGroupId).orElse(null));
                String version = dependencyGroupVersion.map(GroupArtifactVersion::getVersion)
                        .orElseGet(() -> importedBomGroupVersion.map(GroupArtifactVersion::getVersion).orElse(null));
                Set<String> artifacts = dependencies.stream().map(GroupArtifactVersion::getArtifactId).collect(toCollection(HashSet::new));
                if (importedBom != null) {
                    artifacts.add(importedBom.getArtifactId());
                }
                if (StringUtils.isBlank(group) || artifacts.isEmpty() || StringUtils.isBlank(version)) {
                    return null;
                }

                return new SpringDependencyManagementPluginEntry(cursor, group, artifacts, version);
            }

            return null;
        }

        private boolean withinDependencyManagementBlock(Cursor cursor) {
            return withinBlock(cursor, "dependencyManagement");
        }

        private boolean withinImportsBlock(Cursor cursor) {
            return withinBlock(cursor, "imports");
        }

        private boolean withinDependenciesBlock(Cursor cursor) {
            return withinBlock(cursor, "dependencies");
        }

        private List<GroupArtifactVersion> parseDependencySet(J.MethodInvocation methodInvocation) {
            List<String> entries = new ArrayList<>();

            new JavaIsoVisitor<List<String>>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, List<String> entries) {
                    if ("entry".equals(method.getSimpleName()) && method.getArguments().size() == 1) {
                        Expression argument = method.getArguments().get(0);
                        if (argument instanceof J.Literal) {
                            Object value = ((J.Literal) argument).getValue();
                            if (value instanceof String) {
                                entries.add((String) value);
                            }
                        }
                    }
                    return super.visitMethodInvocation(method, entries);
                }
            }.visit(methodInvocation, entries);

            List<GroupArtifactVersion> dependencies = new ArrayList<>();
            List<Expression> arguments = methodInvocation.getArguments();
            Expression argument = arguments.get(0);
            entries.forEach(entry -> {
                GavMap gavMap = null;
                if (argument instanceof J.Literal) {
                    String notation = (String) ((J.Literal) argument).getValue();
                    int versionIdx = notation == null ? -1 : notation.lastIndexOf(':');
                    if (versionIdx != -1) {
                        dependencies.add(new GroupArtifactVersion(notation.substring(0, versionIdx), entry, notation.substring(versionIdx + 1)));
                    }
                    return;
                } else if (argument instanceof G.MapLiteral) {
                    gavMap = getGAVMapEntriesForGMapEntries(((G.MapLiteral) argument).getElements());
                } else if (argument instanceof G.MapEntry) {
                    gavMap = getGAVMapEntriesForGMapEntries(arguments);
                } else if (argument instanceof J.Assignment) {
                    gavMap = getGAVMapEntriesForAssignments(arguments);
                } else if (argument instanceof J.MethodInvocation) {
                    gavMap = getGAVMapEntriesForMapOfInvocation((J.MethodInvocation) argument);
                }
                if (gavMap != null && gavMap.isValid()) {
                    GroupArtifactVersion gav = gavMap.asGav(entry);
                    if (gav != null) {
                        dependencies.add(gav);
                    }
                }
            });
            return dependencies;

        }

        private @Nullable GroupArtifactVersion parseDependency(List<Expression> arguments) {
            Expression argument = arguments.get(0);
            GavMap gavMap = null;
            if (argument instanceof J.Literal) {
                String stringNotation = (String) ((J.Literal) argument).getValue();
                Dependency dependency = stringNotation == null ? null : DependencyNotation.parse(stringNotation);
                return dependency == null ? null : dependency.getGav();
            } else if (argument instanceof G.MapLiteral) {
                gavMap = getGAVMapEntriesForGMapEntries(((G.MapLiteral) argument).getElements());
            } else if (argument instanceof G.MapEntry) {
                gavMap = getGAVMapEntriesForGMapEntries(arguments);
            } else if (argument instanceof J.Assignment) {
                gavMap = getGAVMapEntriesForAssignments(arguments);
            } else if (argument instanceof J.MethodInvocation) {
                gavMap = getGAVMapEntriesForMapOfInvocation((J.MethodInvocation) argument);
            }

            if (gavMap == null || !gavMap.isValid()) {
                return null;
            }
            return gavMap.asGav();
        }
    }

    @RequiredArgsConstructor
    private static class ChangeDependencyManagementVisitor extends JavaIsoVisitor<ExecutionContext> {

        private final GradleProject gradleProject;
        private final DependencyMatcher depMatcher;
        private final MavenMetadataFailures metadataFailures;
        private final SpringDependencyManagementPluginEntry dependencyManagement;

        @Nullable
        private final String newGroup;
        @Nullable
        private final String newArtifact;
        @Nullable
        private final String newVersion;

        @Nullable
        private final String versionPattern;

        @Override
        public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
            return sourceFile instanceof G.CompilationUnit || sourceFile instanceof K.CompilationUnit;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

            if ("dependency".equals(method.getSimpleName()) || "mavenBom".equals(method.getSimpleName())) {
                return updateDependency(m, ctx, gradleProject);
            } else if ("dependencySet".equals(method.getSimpleName())) {
                return updateDependencySet(m, ctx);
            }

            return m;
        }

        private J.MethodInvocation updateDependencySet(J.MethodInvocation m, ExecutionContext ctx) {
            if (dependencyManagement.getArtifacts().stream().allMatch(artifact ->
                    depMatcher.matches(dependencyManagement.getGroup(), artifact))) {
                for (String managedArtifact : dependencyManagement.getArtifacts()) {
                    GroupArtifactVersion original = new GroupArtifactVersion(dependencyManagement.getGroup(), managedArtifact, dependencyManagement.getVersion());
                    GroupArtifactVersion updated = original;
                    if (!StringUtils.isBlank(newGroup) && !newGroup.equals(updated.getGroupId())) {
                        updated = updated.withGroupId(newGroup);
                    }
                    if (!StringUtils.isBlank(newArtifact) && !updated.getArtifactId().equals(newArtifact)) {
                        updated = updated.withArtifactId(newArtifact);
                    }
                    if (!StringUtils.isBlank(newVersion) && updated.getGroupId() != null) {
                        String resolvedVersion;
                        try {
                            resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                    .select(new GroupArtifact(updated.getGroupId(), updated.getArtifactId()), m.getSimpleName(), newVersion, versionPattern, ctx);
                        } catch (MavenDownloadingException e) {
                            return e.warn(m);
                        }
                        if (resolvedVersion != null && !resolvedVersion.equals(updated.getVersion())) {
                            updated = updated.withVersion(resolvedVersion);
                        }
                    }
                    if (original != updated) {
                        m = updateDependencySet(m, ctx, original, updated);
                    }
                }
            }

            return m;
        }

        private J.MethodInvocation updateDependencySet(J.MethodInvocation m, ExecutionContext ctx, GroupArtifactVersion oldGav, GroupArtifactVersion newGav) {
            return (J.MethodInvocation) requireNonNull(new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    J.MethodInvocation visited = super.visitMethodInvocation(method, ctx);
                    if ("entry".equals(method.getSimpleName())) {
                        visited = visited.withArguments(ListUtils.map(visited.getArguments(), arg -> {
                            if (arg instanceof J.Literal && ((J.Literal) arg).getValue() instanceof String) {
                                if (oldGav.getArtifactId().equals(((J.Literal) arg).getValue())) {
                                    return ChangeStringLiteral.withStringValue((J.Literal) arg, newGav.getArtifactId());
                                }
                            }
                            return arg;
                        }));
                    } else if ("dependencySet".equals(method.getSimpleName())) {
                        visited = visited.withArguments(ListUtils.map(visited.getArguments(), arg -> {
                            if (arg instanceof J.Literal) {
                                return ChangeStringLiteral.withStringValue((J.Literal) arg, newGav.getGroupId() + ":" + newGav.getVersion());
                            } else if (arg instanceof G.MapEntry) {
                                return updateMapEntry((G.MapEntry) arg);
                            } else if (arg instanceof G.MapLiteral) {
                                G.MapLiteral mapArg = (G.MapLiteral) arg;
                                return mapArg.withElements(ListUtils.map(mapArg.getElements(), this::updateMapEntry));
                            } else if (arg instanceof J.Assignment) {
                                J.Assignment ass = (J.Assignment) arg;
                                if (ass.getVariable() instanceof J.Identifier && ass.getAssignment() instanceof J.Literal) {
                                    J.Identifier identifier = (J.Identifier) ass.getVariable();
                                    J.Literal assignment = (J.Literal) ass.getAssignment();
                                    if (assignment.getValue() instanceof String) {
                                        if (GROUP.equals(identifier.getSimpleName()) && newGav.getGroupId() != null) {
                                            return ass.withAssignment(ChangeStringLiteral.withStringValue(assignment, newGav.getGroupId()));
                                        } else if (VERSION.equals(identifier.getSimpleName()) && newGav.getVersion() != null) {
                                            return ass.withAssignment(ChangeStringLiteral.withStringValue(assignment, newGav.getVersion()));
                                        }
                                    }
                                }
                            } else if (arg instanceof J.MethodInvocation && "mapOf".equals(((J.MethodInvocation) arg).getSimpleName())) {
                                J.MethodInvocation m = (J.MethodInvocation) arg;
                                return m.withArguments(ListUtils.map(m.getArguments(), e -> {
                                    if (!(e instanceof J.MethodInvocation)) {
                                        return e;
                                    }
                                    J.MethodInvocation methodInvocation = (J.MethodInvocation) e;
                                    if (!("to".equals(m.getSimpleName()) || methodInvocation.getSelect() instanceof J.Literal || methodInvocation.getArguments().size() != 1 || !(methodInvocation.getArguments().get(0) instanceof J.Literal))) {
                                        return e;
                                    }
                                    J.Literal identifier = (J.Literal) methodInvocation.getSelect();
                                    J.Literal value = (J.Literal) methodInvocation.getArguments().get(0);
                                    if (identifier == null || !(identifier.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                                        return e;
                                    }
                                    String name = (String) identifier.getValue();
                                    switch (name) {
                                        case GROUP:
                                            return methodInvocation.withArguments(ListUtils.map(methodInvocation.getArguments(), literal -> literal == null || newGav.getGroupId() == null ? literal : ChangeStringLiteral.withStringValue(literal, newGav.getGroupId())));
                                        case ARTIFACT:
                                            return methodInvocation.withArguments(ListUtils.map(methodInvocation.getArguments(), literal -> literal == null ? null : ChangeStringLiteral.withStringValue(literal, newGav.getArtifactId())));
                                        case VERSION:
                                            return methodInvocation.withArguments(ListUtils.map(methodInvocation.getArguments(), literal -> literal == null || newGav.getVersion() == null ? literal : ChangeStringLiteral.withStringValue(literal, newGav.getVersion())));
                                    }
                                    return e;
                                }));
                            }

                            return arg;
                        }));
                    }

                    return visited;
                }

                private G.@Nullable MapEntry updateMapEntry(G.@Nullable MapEntry entry) {
                    if (entry != null) {
                        if ((entry.getKey() instanceof J.Literal) && (entry.getValue() instanceof J.Literal)) {
                            J.Literal key = (J.Literal) entry.getKey();
                            J.Literal value = (J.Literal) entry.getValue();
                            if ((key.getValue() instanceof String) && (value.getValue() instanceof String)) {
                                String keyValue = (String) key.getValue();
                                if (GROUP.equals(keyValue) && newGav.getGroupId() != null) {
                                    return entry.withValue(ChangeStringLiteral.withStringValue(value, newGav.getGroupId()));
                                } else if (VERSION.equals(keyValue) && newGav.getVersion() != null) {
                                    return entry.withValue(ChangeStringLiteral.withStringValue(value, newGav.getVersion()));
                                }
                            }
                        }
                    }
                    return entry;
                }
            }.visit(m, ctx));
        }

        private J.MethodInvocation updateDependency(J.MethodInvocation m, ExecutionContext ctx, GradleProject gradleProject) {
            List<Expression> depArgs = m.getArguments();
            if (depArgs.get(0) instanceof J.Literal) {
                String gav = (String) ((J.Literal) depArgs.get(0)).getValue();
                if (gav != null) {
                    Dependency original = DependencyNotation.parse(gav);
                    if (original != null) {
                        Dependency updated = original;
                        if (!StringUtils.isBlank(newGroup) && !updated.getGroupId().equals(newGroup)) {
                            updated = updated.withGav(updated.getGav().withGroupId(newGroup));
                        }
                        if (!StringUtils.isBlank(newArtifact) && !updated.getArtifactId().equals(newArtifact)) {
                            updated = updated.withGav(updated.getGav().withArtifactId(newArtifact));
                        }
                        if (!StringUtils.isBlank(newVersion)) {
                            String resolvedVersion;
                            try {
                                resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                        .select(new GroupArtifact(updated.getGroupId(), updated.getArtifactId()), m.getSimpleName(), newVersion, versionPattern, ctx);
                            } catch (MavenDownloadingException e) {
                                return e.warn(m);
                            }
                            if (resolvedVersion != null && !resolvedVersion.equals(updated.getVersion())) {
                                updated = updated.withGav(updated.getGav().withVersion(resolvedVersion));
                            }
                        }
                        if (original != updated) {
                            String replacement = DependencyNotation.toStringNotation(updated);
                            m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> arg == null ? null : ChangeStringLiteral.withStringValue((J.Literal) arg, replacement)));
                        }
                    }
                }
                return m;
            }

            GavMap gavMap = null;
            if (m.getArguments().get(0) instanceof G.MapEntry) {
                gavMap = getGAVMapEntriesForGMapEntries(depArgs);
            } else if (m.getArguments().get(0) instanceof G.MapLiteral) {
                gavMap = getGAVMapEntriesForGMapEntries(((G.MapLiteral) depArgs.get(0)).getElements());
            } else if (m.getArguments().get(0) instanceof J.Assignment) {
                gavMap = getGAVMapEntriesForAssignments(depArgs);
            } else if (m.getArguments().get(0) instanceof J.MethodInvocation) {
                gavMap = getGAVMapEntriesForMapOfInvocation((J.MethodInvocation) m.getArguments().get(0));
            }

            if (gavMap != null && gavMap.isValid()) {
                GavMapEntry<?> groupId = gavMap.get(GROUP);
                GavMapEntry<?> artifactId = gavMap.get(ARTIFACT);
                GavMapEntry<?> version = gavMap.get(VERSION);

                try {
                    GroupArtifactVersion targetGav = calculateTargetGav(groupId, artifactId, version, m.getSimpleName(), ctx);

                    if (targetGav == null) {
                        return m;
                    }

                    if (!Objects.equals(targetGav.getGroupId(), groupId.getValue()) ||
                            !Objects.equals(targetGav.getArtifactId(), artifactId.getValue()) ||
                            (targetGav.getVersion() != null && !Objects.equals(targetGav.getVersion(), version == null ? null : version.getValue()))) {
                        if (m.getArguments().get(0) instanceof G.MapEntry || m.getArguments().get(0) instanceof G.MapLiteral) {
                            m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                                if (arg == groupId.getElement() && targetGav.getGroupId() != null) {
                                    return ((G.MapEntry) groupId.getElement()).withValue(ChangeStringLiteral.withStringValue((J.Literal) ((G.MapEntry) groupId.getElement()).getValue(), targetGav.getGroupId()));
                                }
                                if (arg == artifactId.getElement()) {
                                    return ((G.MapEntry) artifactId.getElement()).withValue(ChangeStringLiteral.withStringValue((J.Literal) ((G.MapEntry) artifactId.getElement()).getValue(), targetGav.getArtifactId()));
                                }
                                if (version != null && arg == version.getElement() && targetGav.getVersion() != null) {
                                    return ((G.MapEntry) version.getElement()).withValue(ChangeStringLiteral.withStringValue((J.Literal) ((G.MapEntry) version.getElement()).getValue(), targetGav.getVersion()));
                                }
                                return arg;
                            }));
                        } else if (m.getArguments().get(0) instanceof J.Assignment) {
                            m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                                if (arg == groupId.getElement() && targetGav.getGroupId() != null) {
                                    return ((J.Assignment) groupId.getElement()).withAssignment(ChangeStringLiteral.withStringValue((J.Literal) ((J.Assignment) groupId.getElement()).getAssignment(), targetGav.getGroupId()));
                                }
                                if (arg == artifactId.getElement()) {
                                    return ((J.Assignment) artifactId.getElement()).withAssignment(ChangeStringLiteral.withStringValue((J.Literal) ((J.Assignment) artifactId.getElement()).getAssignment(), targetGav.getArtifactId()));
                                }
                                if (version != null && arg == version.getElement() && targetGav.getVersion() != null) {
                                    return ((J.Assignment) version.getElement()).withAssignment(ChangeStringLiteral.withStringValue((J.Literal) ((J.Assignment) version.getElement()).getAssignment(), targetGav.getVersion()));
                                }
                                return arg;
                            }));
                        } else if (m.getArguments().get(0) instanceof J.MethodInvocation) {
                            m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                                if (arg instanceof J.MethodInvocation && "mapOf".equals(((J.MethodInvocation) arg).getSimpleName())) {
                                    return ((J.MethodInvocation) arg).withArguments(ListUtils.map(((J.MethodInvocation) arg).getArguments(), e -> {
                                        if (e == groupId.getElement() && targetGav.getGroupId() != null) {
                                            return ((J.MethodInvocation) groupId.getElement()).withArguments(ListUtils.map(((J.MethodInvocation) groupId.getElement()).getArguments(), literal -> literal == null ? null : ChangeStringLiteral.withStringValue(literal, targetGav.getGroupId())));
                                        }
                                        if (e == artifactId.getElement()) {
                                            return ((J.MethodInvocation) artifactId.getElement()).withArguments(ListUtils.map(((J.MethodInvocation) artifactId.getElement()).getArguments(), literal -> literal == null ? null : ChangeStringLiteral.withStringValue(literal, targetGav.getArtifactId())));
                                        }
                                        if (version != null && e == version.getElement() && targetGav.getVersion() != null) {
                                            return ((J.MethodInvocation) version.getElement()).withArguments(ListUtils.map(((J.MethodInvocation) version.getElement()).getArguments(), literal -> literal == null ? null : ChangeStringLiteral.withStringValue(literal, targetGav.getVersion())));
                                        }
                                        return e;
                                    }));
                                }
                                return arg;
                            }));
                        }
                    }
                } catch (MavenDownloadingException e) {
                    return e.warn(m);
                }
            }

            return m;
        }

        private @Nullable GroupArtifactVersion calculateTargetGav(@Nullable GavMapEntry<?> groupId, @Nullable GavMapEntry<?> artifactId, @Nullable GavMapEntry<?> version, String configuration, ExecutionContext ctx) throws MavenDownloadingException {
            if (groupId == null || artifactId == null) {
                return null;
            }
            if (!depMatcher.matches(groupId.getValue(), artifactId.getValue())) {
                return null;
            }
            String updatedGroupId = groupId.getValue();
            if (!StringUtils.isBlank(newGroup) && !updatedGroupId.equals(newGroup)) {
                updatedGroupId = newGroup;
            }
            String updatedArtifactId = artifactId.getValue();
            if (!StringUtils.isBlank(newArtifact) && !updatedArtifactId.equals(newArtifact)) {
                updatedArtifactId = newArtifact;
            }
            String updatedVersion = version == null ? null : version.getValue();
            if (!StringUtils.isBlank(newVersion)) {
                String resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                        .select(new GroupArtifact(updatedGroupId, updatedArtifactId), configuration, newVersion, versionPattern, ctx);
                if (resolvedVersion != null && !resolvedVersion.equals(updatedVersion)) {
                    updatedVersion = resolvedVersion;
                }
            }

            return new GroupArtifactVersion(updatedGroupId, updatedArtifactId, updatedVersion);
        }
    }

    private static GavMap getGAVMapEntriesForMapOfInvocation(J.MethodInvocation mapOf) {
        if (!"mapOf".equals(mapOf.getSimpleName())) {
            return new GavMap();
        }
        return getGAVMapEntries(
                mapOf.getArguments(),
                J.MethodInvocation.class,
                J.MethodInvocation::getSelect,
                invocation -> invocation.getArguments().get(0),
                e -> !"to".equals(e.getSimpleName()),
                e -> !(e.getSelect() instanceof J.Literal),
                e -> e.getArguments().size() != 1,
                e -> !(e.getArguments().get(0) instanceof J.Literal)
        );
    }

    private static GavMap getGAVMapEntriesForGMapEntries(List<? extends Expression> mapEntries) {
        return getGAVMapEntries(
                mapEntries,
                G.MapEntry.class,
                G.MapEntry::getKey,
                G.MapEntry::getValue,
                e -> !(e.getKey() instanceof J.Literal),
                e -> !(e.getValue() instanceof J.Literal)
        );
    }

    private static GavMap getGAVMapEntriesForAssignments(List<? extends Expression> assignments) {
        return getGAVMapEntries(
                assignments,
                J.Assignment.class,
                J.Assignment::getVariable,
                J.Assignment::getAssignment,
                a -> !(a.getVariable() instanceof J.Identifier),
                a -> !(a.getAssignment() instanceof J.Literal)
        );
    }

    @SafeVarargs
    private static <T extends Expression> GavMap getGAVMapEntries(List<? extends Expression> elements, Class<T> type, Function<T, @Nullable Expression> keyExtractor, Function<T, @Nullable Expression> valueExtractor, Predicate<T>... ignore) {
        GavMap map = new GavMap();
        for (Expression e : elements) {
            if (!type.isInstance(e)) {
                continue;
            }
            T element = type.cast(e);
            if (Arrays.stream(ignore).anyMatch(predicate -> predicate.test(element))) {
                continue;
            }
            Expression keyExpression = keyExtractor.apply(element);
            Expression valueExpression = valueExtractor.apply(element);
            if (keyExpression == null || valueExpression == null) {
                continue;
            }
            String key = literalOrIdentifierToString(keyExpression);
            String value = literalOrIdentifierToString(valueExpression);
            map.put(key, new GavMapEntry<>(element, value));
        }

        return map;
    }

    private static String literalOrIdentifierToString(Expression expr) {
        if (expr instanceof J.Literal) {
            Object value = ((J.Literal) expr).getValue();
            return value == null ? "" : value.toString();
        } else if (expr instanceof J.Identifier) {
            return ((J.Identifier) expr).getSimpleName();
        }
        throw new IllegalArgumentException("Expression must be a J.Literal or J.Identifier");
    }

    private static class GavMap {
        Map<String, GavMapEntry<? extends Expression>> entries = new HashMap<>();

        void put(String key, GavMapEntry<? extends Expression> entry) {
            entries.put(key, entry);
        }

        @Nullable
        GavMapEntry<? extends Expression> get(String key) {
            return entries.get(key);
        }

        @Nullable GroupArtifactVersion asGav() {
            return asGav(null);
        }

        @Nullable GroupArtifactVersion asGav(@Nullable String artifactName) {
            GavMapEntry<? extends Expression> group = entries.get(GROUP);
            GavMapEntry<? extends Expression> artifact = entries.get(ARTIFACT);
            GavMapEntry<? extends Expression> version = entries.get(VERSION);

            if (group != null && (artifact != null || !StringUtils.isBlank(artifactName))) {
                return new GroupArtifactVersion(
                        group.getValue(),
                        !StringUtils.isBlank(artifactName) ? artifactName : requireNonNull(artifact).getValue(),
                        version == null ? null : version.getValue()
                );
            }
            return null;
        }

        private boolean isValid() {
            return entries.containsKey(GROUP);
        }
    }

    @Value
    private static class GavMapEntry<T> {
        T element;
        String value;
    }
}
