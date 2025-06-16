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
import org.openrewrite.gradle.internal.Dependency;
import org.openrewrite.gradle.internal.DependencyStringNotationConverter;
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
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.trait.Trait;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
public class SpringDependencyManagementPluginEntry implements Trait<J.MethodInvocation> {
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
                    if (dependencies.stream().anyMatch(dependency -> (groupId != null && !matchesGlob(dependency.getGroupId(), groupId)) || (artifactId != null && !matchesGlob(dependency.getArtifactId(), artifactId)))) {
                        dependencies.clear();
                    }
                }

                GroupArtifactVersion importedBom = null;
                if (IMPORTS_MATCHER.matches(methodInvocation, true)) {
                    Expression argument = methodInvocation.getArguments().get(0);
                    if (argument instanceof J.Literal || argument instanceof G.GString || argument instanceof G.MapEntry || argument instanceof G.MapLiteral || argument instanceof J.Assignment || argument instanceof K.StringTemplate) {
                        importedBom = parseDependency(methodInvocation.getArguments());
                    }
                    if (importedBom != null && ((groupId != null && !matchesGlob(importedBom.getGroupId(), groupId)) || (artifactId != null && !matchesGlob(importedBom.getArtifactId(), artifactId)))) {
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
                Set<String> artifacts = dependencies.stream().map(GroupArtifactVersion::getArtifactId).collect(Collectors.toCollection(HashSet::new));
                if (importedBom != null) {
                    artifacts.add(importedBom.getArtifactId());
                }

                return new SpringDependencyManagementPluginEntry(cursor, group, artifacts, version);
            }

            return null;
        }

        private boolean withinBlock(Cursor cursor, String name) {
            Cursor parentCursor = cursor.getParent();
            while (parentCursor != null) {
                if (parentCursor.getValue() instanceof J.MethodInvocation) {
                    J.MethodInvocation m = parentCursor.getValue();
                    if (m.getSimpleName().equals(name)) {
                        return true;
                    }
                }
                parentCursor = parentCursor.getParent();
            }

            return false;
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
                    if (method.getSimpleName().equals("entry") && method.getArguments().size() == 1) {
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
                GroupArtifactVersion dependency = null;
                if (argument instanceof J.Literal) {
                    String notation = (String) ((J.Literal) argument).getValue();
                    int versionIdx = notation.lastIndexOf(':');
                    if (versionIdx != -1) {
                        dependency = new GroupArtifactVersion(notation.substring(0, versionIdx), entry, notation.substring(versionIdx + 1));
                    }
                } else if (argument instanceof G.MapLiteral) {
                    List<Expression> mapEntryExpressions = ((G.MapLiteral) argument).getElements()
                            .stream()
                            .map(e -> (Expression) e)
                            .collect(Collectors.toList());
                    dependency = getMapEntriesDependencySet(mapEntryExpressions, entry);
                } else if (argument instanceof G.MapEntry) {
                    dependency = getMapEntriesDependencySet(arguments, entry);
                } else if (argument instanceof J.Assignment) {
                    String group = null;
                    String version = null;

                    for (Expression e : arguments) {
                        if (!(e instanceof J.Assignment)) {
                            continue;
                        }
                        J.Assignment arg = (J.Assignment) e;
                        if (!(arg.getVariable() instanceof J.Identifier) || !(arg.getAssignment() instanceof J.Literal)) {
                            continue;
                        }
                        J.Identifier identifier = (J.Identifier) arg.getVariable();
                        J.Literal value = (J.Literal) arg.getAssignment();
                        if (!(value.getValue() instanceof String)) {
                            continue;
                        }
                        String name = identifier.getSimpleName();
                        if ("group".equals(name)) {
                            group = (String) value.getValue();
                        } else if ("version".equals(name)) {
                            version = (String) value.getValue();
                        }
                    }

                    if (group != null) {
                        dependency = new GroupArtifactVersion(group, entry, version);
                    }
                } else if (argument instanceof J.MethodInvocation && "mapOf".equals(((J.MethodInvocation) argument).getSimpleName())) {
                    String group = null;
                    String version = null;

                    for (Expression e : ((J.MethodInvocation) argument).getArguments()) {
                        if (!(e instanceof J.MethodInvocation)) {
                            continue;
                        }
                        J.MethodInvocation m = (J.MethodInvocation) e;
                        if (!("to".equals(m.getSimpleName()) || m.getSelect() instanceof J.Literal || m.getArguments().size() != 1 || !(m.getArguments().get(0) instanceof J.Literal))) {
                            continue;
                        }
                        J.Literal identifier = (J.Literal) m.getSelect();
                        J.Literal value = (J.Literal) m.getArguments().get(0);
                        if (!(identifier.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                            continue;
                        }
                        String name = (String) identifier.getValue();
                        switch (name) {
                            case "group":
                                group = (String) value.getValue();
                                break;
                            case "version":
                                version = (String) value.getValue();
                                break;
                        }
                    }

                    if (group != null) {
                        dependency = new GroupArtifactVersion(group, entry, version);
                    }
                }
                if (dependency != null) {
                    dependencies.add(dependency);
                }
            });
            return dependencies;

        }

        private @Nullable GroupArtifactVersion parseDependency(List<Expression> arguments) {
            Expression argument = arguments.get(0);
            if (argument instanceof J.Literal) {
                Dependency dependency = DependencyStringNotationConverter.parse((String) ((J.Literal) argument).getValue());
                return dependency == null ? null : dependency.getGav();
            } else if (argument instanceof G.MapLiteral) {
                List<Expression> mapEntryExpressions = ((G.MapLiteral) argument).getElements()
                        .stream()
                        .map(e -> (Expression) e)
                        .collect(Collectors.toList());
                return getMapEntriesDependency(mapEntryExpressions);
            } else if (argument instanceof G.MapEntry) {
                return getMapEntriesDependency(arguments);
            } else if (argument instanceof J.Assignment) {
                String group = null;
                String artifact = null;
                String version = null;

                for (Expression e : arguments) {
                    if (!(e instanceof J.Assignment)) {
                        continue;
                    }
                    J.Assignment arg = (J.Assignment) e;
                    if (!(arg.getVariable() instanceof J.Identifier) || !(arg.getAssignment() instanceof J.Literal)) {
                        continue;
                    }
                    J.Identifier identifier = (J.Identifier) arg.getVariable();
                    J.Literal value = (J.Literal) arg.getAssignment();
                    if (!(value.getValue() instanceof String)) {
                        continue;
                    }
                    String name = identifier.getSimpleName();
                    switch (name) {
                        case "group":
                            group = (String) value.getValue();
                            break;
                        case "name":
                            artifact = (String) value.getValue();
                            break;
                        case "version":
                            version = (String) value.getValue();
                            break;
                    }
                }

                if (group == null || artifact == null) {
                    return null;
                }

                return new GroupArtifactVersion(group, artifact, version);
            } else if (argument instanceof J.MethodInvocation && "mapOf".equals(((J.MethodInvocation) argument).getSimpleName())) {
                String group = null;
                String artifact = null;
                String version = null;

                for (Expression e : ((J.MethodInvocation) argument).getArguments()) {
                    if (!(e instanceof J.MethodInvocation)) {
                        continue;
                    }
                    J.MethodInvocation m = (J.MethodInvocation) e;
                    if (!("to".equals(m.getSimpleName()) || m.getSelect() instanceof J.Literal || m.getArguments().size() != 1 || !(m.getArguments().get(0) instanceof J.Literal))) {
                        continue;
                    }
                    J.Literal identifier = (J.Literal) m.getSelect();
                    J.Literal value = (J.Literal) m.getArguments().get(0);
                    if (!(identifier.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                        continue;
                    }
                    String name = (String) identifier.getValue();
                    switch (name) {
                        case "group":
                            group = (String) value.getValue();
                            break;
                        case "name":
                            artifact = (String) value.getValue();
                            break;
                        case "version":
                            version = (String) value.getValue();
                            break;
                    }
                }

                if (group != null && artifact != null) {
                    return new GroupArtifactVersion(group, artifact, version);
                }
            }

            return null;
        }

        private static @Nullable GroupArtifactVersion getMapEntriesDependency(List<Expression> arguments) {
            String group = null;
            String artifact = null;
            String version = null;

            for (Expression e : arguments) {
                if (!(e instanceof G.MapEntry)) {
                    continue;
                }
                G.MapEntry arg = (G.MapEntry) e;
                if (!(arg.getKey() instanceof J.Literal) || !(arg.getValue() instanceof J.Literal)) {
                    continue;
                }
                J.Literal key = (J.Literal) arg.getKey();
                J.Literal value = (J.Literal) arg.getValue();
                if (!(key.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                    continue;
                }
                String keyValue = (String) key.getValue();
                if ("group".equals(keyValue)) {
                    group = (String) value.getValue();
                } else if ("name".equals(keyValue)) {
                    artifact = (String) value.getValue();
                } else if ("version".equals(keyValue)) {
                    version = (String) value.getValue();
                }
            }

            if (group == null || artifact == null) {
                return null;
            }

            return new GroupArtifactVersion(group, artifact, version);
        }

        private static @Nullable GroupArtifactVersion getMapEntriesDependencySet(List<Expression> arguments, String artifactId) {
            String group = null;
            String version = null;

            for (Expression e : arguments) {
                if (!(e instanceof G.MapEntry)) {
                    continue;
                }
                G.MapEntry arg = (G.MapEntry) e;
                if (!(arg.getKey() instanceof J.Literal) || !(arg.getValue() instanceof J.Literal)) {
                    continue;
                }
                J.Literal key = (J.Literal) arg.getKey();
                J.Literal value = (J.Literal) arg.getValue();
                if (!(key.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                    continue;
                }
                String keyValue = (String) key.getValue();
                if ("group".equals(keyValue)) {
                    group = (String) value.getValue();
                } else if ("version".equals(keyValue)) {
                    version = (String) value.getValue();
                }
            }

            if (group == null) {
                return null;
            }

            return new GroupArtifactVersion(group, artifactId, version);
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
                return updateDependency(m, ctx, gradleProject, depMatcher);
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
                    if (!StringUtils.isBlank(newVersion)) {
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
                                return updateMapEntry((G.MapEntry) arg, newGav);
                            } else if (arg instanceof G.MapLiteral) {
                                G.MapLiteral mapArg = (G.MapLiteral) arg;
                                return mapArg.withElements(ListUtils.map(mapArg.getElements(), e -> updateMapEntry(e, newGav)));
                            } else if (arg instanceof J.Assignment) {
                                J.Assignment ass = (J.Assignment) arg;
                                if (ass.getVariable() instanceof J.Identifier && ass.getAssignment() instanceof J.Literal) {
                                    J.Identifier identifier = (J.Identifier) ass.getVariable();
                                    J.Literal assignment = (J.Literal) ass.getAssignment();
                                    if (assignment.getValue() instanceof String) {
                                        if ("group".equals(identifier.getSimpleName())) {
                                            return ass.withAssignment(ChangeStringLiteral.withStringValue(assignment, newGav.getGroupId()));
                                        } else if ("version".equals(identifier.getSimpleName())) {
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
                                    if (!(identifier.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                                        return e;
                                    }
                                    String name = (String) identifier.getValue();
                                    switch (name) {
                                        case "group":
                                            return methodInvocation.withArguments(ListUtils.map(methodInvocation.getArguments(), literal -> ChangeStringLiteral.withStringValue(literal, newGav.getGroupId())));
                                        case "name":
                                            return methodInvocation.withArguments(ListUtils.map(methodInvocation.getArguments(), literal -> ChangeStringLiteral.withStringValue(literal, newGav.getArtifactId())));
                                        case "version":
                                            return methodInvocation.withArguments(ListUtils.map(methodInvocation.getArguments(), literal -> ChangeStringLiteral.withStringValue(literal, newGav.getVersion())));
                                    }
                                    return e;
                                }));
                            }

                            return arg;
                        }));
                    }

                    return visited;
                }

                private G.@Nullable MapEntry updateMapEntry(G.@Nullable MapEntry entry, GroupArtifactVersion gav) {
                    if (entry != null) {
                        if ((entry.getKey() instanceof J.Literal) && (entry.getValue() instanceof J.Literal)) {
                            J.Literal key = (J.Literal) entry.getKey();
                            J.Literal value = (J.Literal) entry.getValue();
                            if ((key.getValue() instanceof String) && (value.getValue() instanceof String)) {
                                String keyValue = (String) key.getValue();
                                if ("group".equals(keyValue)) {
                                    return entry.withValue(ChangeStringLiteral.withStringValue(value, newGav.getGroupId()));
                                } else if ("version".equals(keyValue)) {
                                    return entry.withValue(ChangeStringLiteral.withStringValue(value, newGav.getVersion()));
                                }
                            }
                        }
                    }
                    return entry;
                }
            }.visit(m, ctx));
        }

        private J.MethodInvocation updateDependency(J.MethodInvocation m, ExecutionContext ctx, GradleProject gradleProject, DependencyMatcher depMatcher) {
            List<Expression> depArgs = m.getArguments();
            if (depArgs.get(0) instanceof J.Literal) {
                String gav = (String) ((J.Literal) depArgs.get(0)).getValue();
                if (gav != null) {
                    Dependency original = DependencyStringNotationConverter.parse(gav);
                    if (original != null) {
                        Dependency updated = original;
                        if (!StringUtils.isBlank(newGroup) && !updated.getGroupId().equals(newGroup)) {
                            updated = updated.withGroupId(newGroup);
                        }
                        if (!StringUtils.isBlank(newArtifact) && !updated.getArtifactId().equals(newArtifact)) {
                            updated = updated.withArtifactId(newArtifact);
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
                                updated = updated.withVersion(resolvedVersion);
                            }
                        }
                        if (original != updated) {
                            String replacement = updated.toStringNotation();
                            m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> ChangeStringLiteral.withStringValue((J.Literal) arg, replacement)));
                        }
                    }
                }
            } else if (m.getArguments().get(0) instanceof G.MapEntry) {
                G.MapEntry groupEntry = null;
                G.MapEntry artifactEntry = null;
                G.MapEntry versionEntry = null;
                String groupId = null;
                String artifactId = null;
                String version = null;

                for (Expression e : depArgs) {
                    if (!(e instanceof G.MapEntry)) {
                        continue;
                    }
                    G.MapEntry arg = (G.MapEntry) e;
                    if (!(arg.getKey() instanceof J.Literal) || !(arg.getValue() instanceof J.Literal)) {
                        continue;
                    }
                    J.Literal key = (J.Literal) arg.getKey();
                    J.Literal value = (J.Literal) arg.getValue();
                    if (!(key.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                        continue;
                    }
                    String keyValue = (String) key.getValue();
                    String valueValue = (String) value.getValue();
                    switch (keyValue) {
                        case "group":
                            groupEntry = arg;
                            groupId = valueValue;
                            break;
                        case "name":
                            artifactEntry = arg;
                            artifactId = valueValue;
                            break;
                        case "version":
                            versionEntry = arg;
                            version = valueValue;
                            break;
                    }
                }
                if (groupId == null || artifactId == null) {
                    return m;
                }
                if (!depMatcher.matches(groupId, artifactId)) {
                    return m;
                }
                String updatedGroupId = groupId;
                if (!StringUtils.isBlank(newGroup) && !updatedGroupId.equals(newGroup)) {
                    updatedGroupId = newGroup;
                }
                String updatedArtifactId = artifactId;
                if (!StringUtils.isBlank(newArtifact) && !updatedArtifactId.equals(newArtifact)) {
                    updatedArtifactId = newArtifact;
                }
                String updatedVersion = version;
                if (!StringUtils.isBlank(newVersion)) {
                    String resolvedVersion;
                    try {
                        resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                .select(new GroupArtifact(updatedGroupId, updatedArtifactId), m.getSimpleName(), newVersion, versionPattern, ctx);
                    } catch (MavenDownloadingException e) {
                        return e.warn(m);
                    }
                    if (resolvedVersion != null && !resolvedVersion.equals(updatedVersion)) {
                        updatedVersion = resolvedVersion;
                    }
                }

                if (!updatedGroupId.equals(groupId) || !updatedArtifactId.equals(artifactId) || updatedVersion != null && !updatedVersion.equals(version)) {
                    G.MapEntry finalGroup = groupEntry;
                    String finalGroupIdValue = updatedGroupId;
                    G.MapEntry finalArtifact = artifactEntry;
                    String finalArtifactIdValue = updatedArtifactId;
                    G.MapEntry finalVersion = versionEntry;
                    String finalVersionValue = updatedVersion;
                    m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                        if (arg == finalGroup) {
                            return finalGroup.withValue(ChangeStringLiteral.withStringValue((J.Literal) finalGroup.getValue(), finalGroupIdValue));
                        }
                        if (arg == finalArtifact) {
                            return finalArtifact.withValue(ChangeStringLiteral.withStringValue((J.Literal) finalArtifact.getValue(), finalArtifactIdValue));
                        }
                        if (arg == finalVersion) {
                            return finalVersion.withValue(ChangeStringLiteral.withStringValue((J.Literal) finalVersion.getValue(), finalVersionValue));
                        }
                        return arg;
                    }));
                }
            } else if (m.getArguments().get(0) instanceof G.MapLiteral) {
                G.MapLiteral map = (G.MapLiteral) depArgs.get(0);
                G.MapEntry groupEntry = null;
                G.MapEntry artifactEntry = null;
                G.MapEntry versionEntry = null;
                String groupId = null;
                String artifactId = null;
                String version = null;

                for (G.MapEntry arg : map.getElements()) {
                    if (!(arg.getKey() instanceof J.Literal) || !(arg.getValue() instanceof J.Literal)) {
                        continue;
                    }
                    J.Literal key = (J.Literal) arg.getKey();
                    J.Literal value = (J.Literal) arg.getValue();
                    if (!(key.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                        continue;
                    }
                    String keyValue = (String) key.getValue();
                    String valueValue = (String) value.getValue();
                    switch (keyValue) {
                        case "group":
                            groupEntry = arg;
                            groupId = valueValue;
                            break;
                        case "name":
                            artifactEntry = arg;
                            artifactId = valueValue;
                            break;
                        case "version":
                            versionEntry = arg;
                            version = valueValue;
                            break;
                    }
                }
                if (groupId == null || artifactId == null) {
                    return m;
                }
                if (!depMatcher.matches(groupId, artifactId)) {
                    return m;
                }
                String updatedGroupId = groupId;
                if (!StringUtils.isBlank(newGroup) && !updatedGroupId.equals(newGroup)) {
                    updatedGroupId = newGroup;
                }
                String updatedArtifactId = artifactId;
                if (!StringUtils.isBlank(newArtifact) && !updatedArtifactId.equals(newArtifact)) {
                    updatedArtifactId = newArtifact;
                }
                String updatedVersion = version;
                if (!StringUtils.isBlank(newVersion)) {
                    String resolvedVersion;
                    try {
                        resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                .select(new GroupArtifact(updatedGroupId, updatedArtifactId), m.getSimpleName(), newVersion, versionPattern, ctx);
                    } catch (MavenDownloadingException e) {
                        return e.warn(m);
                    }
                    if (resolvedVersion != null && !resolvedVersion.equals(updatedVersion)) {
                        updatedVersion = resolvedVersion;
                    }
                }

                if (!updatedGroupId.equals(groupId) || !updatedArtifactId.equals(artifactId) || updatedVersion != null && !updatedVersion.equals(version)) {
                    G.MapEntry finalGroup = groupEntry;
                    String finalGroupIdValue = updatedGroupId;
                    G.MapEntry finalArtifact = artifactEntry;
                    String finalArtifactIdValue = updatedArtifactId;
                    G.MapEntry finalVersion = versionEntry;
                    String finalVersionValue = updatedVersion;
                    m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                        G.MapLiteral mapLiteral = (G.MapLiteral) arg;
                        return mapLiteral.withElements(ListUtils.map(mapLiteral.getElements(), e -> {
                            if (e == finalGroup) {
                                return finalGroup.withValue(ChangeStringLiteral.withStringValue((J.Literal) finalGroup.getValue(), finalGroupIdValue));
                            }
                            if (e == finalArtifact) {
                                return finalArtifact.withValue(ChangeStringLiteral.withStringValue((J.Literal) finalArtifact.getValue(), finalArtifactIdValue));
                            }
                            if (e == finalVersion) {
                                return finalVersion.withValue(ChangeStringLiteral.withStringValue((J.Literal) finalVersion.getValue(), finalVersionValue));
                            }
                            return e;
                        }));
                    }));
                }
            } else if (m.getArguments().get(0) instanceof J.Assignment) {
                J.Assignment groupAssignment = null;
                J.Assignment artifactAssignment = null;
                J.Assignment versionAssignment = null;
                String groupId = null;
                String artifactId = null;
                String version = null;

                for (Expression e : depArgs) {
                    if (!(e instanceof J.Assignment)) {
                        continue;
                    }
                    J.Assignment arg = (J.Assignment) e;
                    if (!(arg.getVariable() instanceof J.Identifier) || !(arg.getAssignment() instanceof J.Literal)) {
                        continue;
                    }
                    J.Identifier identifier = (J.Identifier) arg.getVariable();
                    J.Literal assignment = (J.Literal) arg.getAssignment();
                    if (!(assignment.getValue() instanceof String)) {
                        continue;
                    }
                    String valueValue = (String) assignment.getValue();
                    switch (identifier.getSimpleName()) {
                        case "group":
                            groupAssignment = arg;
                            groupId = valueValue;
                            break;
                        case "name":
                            artifactAssignment = arg;
                            artifactId = valueValue;
                            break;
                        case "version":
                            versionAssignment = arg;
                            version = valueValue;
                            break;
                    }
                }
                if (groupId == null || artifactId == null) {
                    return m;
                }
                if (!depMatcher.matches(groupId, artifactId)) {
                    return m;
                }
                String updatedGroupId = groupId;
                if (!StringUtils.isBlank(newGroup) && !updatedGroupId.equals(newGroup)) {
                    updatedGroupId = newGroup;
                }
                String updatedArtifactId = artifactId;
                if (!StringUtils.isBlank(newArtifact) && !updatedArtifactId.equals(newArtifact)) {
                    updatedArtifactId = newArtifact;
                }
                String updatedVersion = version;
                if (!StringUtils.isBlank(newVersion)) {
                    String resolvedVersion;
                    try {
                        resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                .select(new GroupArtifact(updatedGroupId, updatedArtifactId), m.getSimpleName(), newVersion, versionPattern, ctx);
                    } catch (MavenDownloadingException e) {
                        return e.warn(m);
                    }
                    if (resolvedVersion != null && !resolvedVersion.equals(updatedVersion)) {
                        updatedVersion = resolvedVersion;
                    }
                }

                if (!updatedGroupId.equals(groupId) || !updatedArtifactId.equals(artifactId) || updatedVersion != null && !updatedVersion.equals(version)) {
                    J.Assignment finalGroup = groupAssignment;
                    String finalGroupIdValue = updatedGroupId;
                    J.Assignment finalArtifact = artifactAssignment;
                    String finalArtifactIdValue = updatedArtifactId;
                    J.Assignment finalVersion = versionAssignment;
                    String finalVersionValue = updatedVersion;
                    m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                        if (arg == finalGroup) {
                            return finalGroup.withAssignment(ChangeStringLiteral.withStringValue((J.Literal) finalGroup.getAssignment(), finalGroupIdValue));
                        }
                        if (arg == finalArtifact) {
                            return finalArtifact.withAssignment(ChangeStringLiteral.withStringValue((J.Literal) finalArtifact.getAssignment(), finalArtifactIdValue));
                        }
                        if (arg == finalVersion) {
                            return finalVersion.withAssignment(ChangeStringLiteral.withStringValue((J.Literal) finalVersion.getAssignment(), finalVersionValue));
                        }
                        return arg;
                    }));
                }
            } else if (m.getArguments().get(0) instanceof J.MethodInvocation && "mapOf".equals(((J.MethodInvocation) m.getArguments().get(0)).getSimpleName())) {
                J.MethodInvocation groupMethodInvocation = null;
                J.MethodInvocation artifactMethodInvocation = null;
                J.MethodInvocation versionMethodInvocation = null;
                String groupId = null;
                String artifactId = null;
                String version = null;

                J.MethodInvocation method = (J.MethodInvocation) m.getArguments().get(0);
                for (Expression e : method.getArguments()) {
                    if (!(e instanceof J.MethodInvocation)) {
                        continue;
                    }
                    J.MethodInvocation methodInvocation = (J.MethodInvocation) e;
                    if (!("to".equals(m.getSimpleName()) || methodInvocation.getSelect() instanceof J.Literal) || methodInvocation.getArguments().size() != 1 || !(methodInvocation.getArguments().get(0) instanceof J.Literal)) {
                        continue;
                    }
                    J.Literal identifier = (J.Literal) methodInvocation.getSelect();
                    J.Literal value = (J.Literal) methodInvocation.getArguments().get(0);
                    if (!(identifier.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                        continue;
                    }
                    String name = (String) identifier.getValue();
                    switch (name) {
                        case "group":
                            groupId = (String) value.getValue();
                            groupMethodInvocation = methodInvocation;
                            break;
                        case "name":
                            artifactId = (String) value.getValue();
                            artifactMethodInvocation = methodInvocation;
                            break;
                        case "version":
                            version = (String) value.getValue();
                            versionMethodInvocation = methodInvocation;
                            break;
                    }
                }
                if (groupId == null || artifactId == null) {
                    return m;
                }
                if (!depMatcher.matches(groupId, artifactId)) {
                    return m;
                }
                String updatedGroupId = groupId;
                if (!StringUtils.isBlank(newGroup) && !updatedGroupId.equals(newGroup)) {
                    updatedGroupId = newGroup;
                }
                String updatedArtifactId = artifactId;
                if (!StringUtils.isBlank(newArtifact) && !updatedArtifactId.equals(newArtifact)) {
                    updatedArtifactId = newArtifact;
                }
                String updatedVersion = version;
                if (!StringUtils.isBlank(newVersion)) {
                    String resolvedVersion;
                    try {
                        resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                .select(new GroupArtifact(updatedGroupId, updatedArtifactId), m.getSimpleName(), newVersion, versionPattern, ctx);
                    } catch (MavenDownloadingException e) {
                        return e.warn(m);
                    }
                    if (resolvedVersion != null && !resolvedVersion.equals(updatedVersion)) {
                        updatedVersion = resolvedVersion;
                    }
                }

                if (!updatedGroupId.equals(groupId) || !updatedArtifactId.equals(artifactId) || updatedVersion != null && !updatedVersion.equals(version)) {
                    J.MethodInvocation finalGroup = groupMethodInvocation;
                    String finalGroupIdValue = updatedGroupId;
                    J.MethodInvocation finalArtifact = artifactMethodInvocation;
                    String finalArtifactIdValue = updatedArtifactId;
                    J.MethodInvocation finalVersion = versionMethodInvocation;
                    String finalVersionValue = updatedVersion;
                    m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                        if (arg instanceof J.MethodInvocation && "mapOf".equals(((J.MethodInvocation) arg).getSimpleName())) {
                            return ((J.MethodInvocation) arg).withArguments(ListUtils.map(((J.MethodInvocation) arg).getArguments(), e -> {
                                if (e == finalGroup) {
                                    return finalGroup.withArguments(ListUtils.map(finalGroup.getArguments(), literal -> ChangeStringLiteral.withStringValue(literal, finalGroupIdValue)));
                                }
                                if (e == finalArtifact) {
                                    return finalArtifact.withArguments(ListUtils.map(finalArtifact.getArguments(), literal -> ChangeStringLiteral.withStringValue(literal, finalArtifactIdValue)));
                                }
                                if (e == finalVersion) {
                                    return finalVersion.withArguments(ListUtils.map(finalVersion.getArguments(), literal -> ChangeStringLiteral.withStringValue(literal, finalVersionValue)));
                                }
                                return e;
                            }));
                        }
                        return arg;
                    }));
                }
            }

            return m;
        }
    }
}
