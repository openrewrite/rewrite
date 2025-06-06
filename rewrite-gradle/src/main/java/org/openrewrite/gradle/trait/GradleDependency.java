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

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.gradle.internal.ChangeStringLiteral;
import org.openrewrite.gradle.internal.DependencyStringNotationConverter;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.trait.Trait;

import java.util.List;
import java.util.stream.Collectors;

import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
public class GradleDependency implements Trait<J.MethodInvocation> {
    Cursor cursor;

    @Getter
    ResolvedDependency resolvedDependency;

    @Getter
    org.openrewrite.gradle.internal.Dependency requestedDependency;

    public J.MethodInvocation withVersion(String version) {
        J.MethodInvocation m = cursor.getValue();
        Expression argument = m.getArguments().get(0);
        if (argument instanceof J.Literal || argument instanceof G.GString || argument instanceof G.MapEntry || argument instanceof G.MapLiteral || argument instanceof J.Assignment || argument instanceof K.StringTemplate) {
            return withVersion(m, version);
        } else if (argument instanceof J.MethodInvocation) {
            if (((J.MethodInvocation) argument).getSimpleName().equals("platform") ||
                    ((J.MethodInvocation) argument).getSimpleName().equals("enforcedPlatform")) {
                return m.withArguments(ListUtils.mapFirst(m.getArguments(), method -> withVersion((J.MethodInvocation) method, version)));
            }
        }
        return m;
    }

    public static class Matcher extends GradleTraitMatcher<GradleDependency> {
        private static final MethodMatcher DEPENDENCY_DSL_MATCHER = new MethodMatcher("DependencyHandlerSpec *(..)");

        @Nullable
        protected String configuration;

        @Nullable
        protected String groupId;

        @Nullable
        protected String artifactId;

        public Matcher configuration(@Nullable String configuration) {
            this.configuration = configuration;
            return this;
        }

        public Matcher groupId(@Nullable String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Matcher artifactId(@Nullable String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        @Override
        protected @Nullable GradleDependency test(Cursor cursor) {
            Object object = cursor.getValue();
            if (object instanceof J.MethodInvocation) {
                J.MethodInvocation methodInvocation = (J.MethodInvocation) object;

                if (!withinDependenciesBlock(cursor)) {
                    return null;
                }

                if (withinDependencyConstraintsBlock(cursor)) {
                    // A dependency constraint is different from an actual dependency
                    return null;
                }

                GradleProject gradleProject = getGradleProject(cursor);
                GradleDependencyConfiguration gdc = getConfiguration(gradleProject, methodInvocation);
                if (gdc == null && !(DEPENDENCY_DSL_MATCHER.matches(methodInvocation) && !"project".equals(methodInvocation.getSimpleName()))) {
                    return null;
                }

                if (!StringUtils.isBlank(configuration) && !methodInvocation.getSimpleName().equals(configuration)) {
                    return null;
                }

                org.openrewrite.gradle.internal.Dependency dependency = null;
                Expression argument = methodInvocation.getArguments().get(0);
                if (argument instanceof J.Literal || argument instanceof G.GString || argument instanceof G.MapEntry || argument instanceof G.MapLiteral || argument instanceof J.Assignment || argument instanceof K.StringTemplate) {
                    dependency = parseDependency(methodInvocation.getArguments());
                } else if (argument instanceof J.MethodInvocation) {
                    if (((J.MethodInvocation) argument).getSimpleName().equals("platform") ||
                            ((J.MethodInvocation) argument).getSimpleName().equals("enforcedPlatform")) {
                        dependency = parseDependency(((J.MethodInvocation) argument).getArguments());
                    } else if (((J.MethodInvocation) argument).getSimpleName().equals("project")) {
                        // project dependencies are not yet supported
                        return null;
                    }
                }

                if (dependency == null) {
                    return null;
                }

                if (gdc != null) {
                    if (gdc.isCanBeResolved()) {
                        for (ResolvedDependency resolvedDependency : gdc.getResolved()) {
                            if ((groupId == null || matchesGlob(resolvedDependency.getGroupId(), groupId)) &&
                                    (artifactId == null || matchesGlob(resolvedDependency.getArtifactId(), artifactId))) {
                                Dependency req = resolvedDependency.getRequested();
                                if ((req.getGroupId() == null || req.getGroupId().equals(dependency.getGroupId())) &&
                                        req.getArtifactId().equals(dependency.getArtifactId())) {
                                    return new GradleDependency(cursor, resolvedDependency, dependency);
                                }
                            }
                        }
                    } else {
                        for (GradleDependencyConfiguration transitiveConfiguration : gradleProject.configurationsExtendingFrom(gdc, true)) {
                            if (transitiveConfiguration.isCanBeResolved()) {
                                for (ResolvedDependency resolvedDependency : transitiveConfiguration.getResolved()) {
                                    if ((groupId == null || matchesGlob(resolvedDependency.getGroupId(), groupId)) &&
                                            (artifactId == null || matchesGlob(resolvedDependency.getArtifactId(), artifactId))) {
                                        Dependency req = resolvedDependency.getRequested();
                                        if ((req.getGroupId() == null || req.getGroupId().equals(dependency.getGroupId())) &&
                                                req.getArtifactId().equals(dependency.getArtifactId())) {
                                            return new GradleDependency(cursor, resolvedDependency, dependency);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if ((groupId == null || matchesGlob(dependency.getGroupId(), groupId)) &&
                        (artifactId == null || matchesGlob(dependency.getArtifactId(), artifactId))) {
                    // Couldn't find the actual resolved dependency, return a virtualized one instead
                    ResolvedDependency resolvedDependency = ResolvedDependency.builder()
                            .depth(-1)
                            .gav(new ResolvedGroupArtifactVersion(null, dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion() != null ? dependency.getVersion() : "", null))
                            .classifier(dependency.getClassifier())
                            .type(dependency.getExt())
                            .requested(Dependency.builder()
                                    .scope(methodInvocation.getSimpleName())
                                    .type(dependency.getExt())
                                    .gav(new GroupArtifactVersion(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()))
                                    .classifier(dependency.getClassifier())
                                    .build())
                            .build();
                    return new GradleDependency(cursor, resolvedDependency, dependency);
                }
            }

            return null;
        }

        private static @Nullable GradleDependencyConfiguration getConfiguration(@Nullable GradleProject gradleProject, J.MethodInvocation methodInvocation) {
            if (gradleProject == null) {
                return null;
            }

            String methodName = methodInvocation.getSimpleName();
            if (methodName.equals("classpath")) {
                return gradleProject.getBuildscript().getConfiguration(methodName);
            } else {
                return gradleProject.getConfiguration(methodName);
            }
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

        private boolean withinDependenciesBlock(Cursor cursor) {
            return withinBlock(cursor, "dependencies");
        }

        private boolean withinDependencyConstraintsBlock(Cursor cursor) {
            return withinBlock(cursor, "constraints") && withinDependenciesBlock(cursor);
        }

        private org.openrewrite.gradle.internal.@Nullable Dependency parseDependency(List<Expression> arguments) {
            Expression argument = arguments.get(0);
            if (argument instanceof J.Literal) {
                return DependencyStringNotationConverter.parse((String) ((J.Literal) argument).getValue());
            } else if (argument instanceof G.GString) {
                G.GString gstring = (G.GString) argument;
                List<J> strings = gstring.getStrings();
                if (strings.size() >= 2 && strings.get(0) instanceof J.Literal && ((J.Literal) strings.get(0)).getValue() != null) {
                    return DependencyStringNotationConverter.parse(parseArgumentString(strings));
                }
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
                String classifier = null;
                String ext = null;

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
                        case "classifier":
                            classifier = (String) value.getValue();
                            break;
                        case "ext":
                            ext = (String) value.getValue();
                            break;
                    }
                }

                if (group == null || artifact == null) {
                    return null;
                }

                return new org.openrewrite.gradle.internal.Dependency(group, artifact, version, classifier, ext);
            } else if (argument instanceof K.StringTemplate) {
                K.StringTemplate template = (K.StringTemplate) argument;
                List<J> strings = template.getStrings();
                if (strings.size() >= 2 && strings.get(0) instanceof J.Literal && ((J.Literal) strings.get(0)).getValue() != null) {
                    return DependencyStringNotationConverter.parse(parseArgumentString(strings));
                }
            }

            return null;
        }

        private static org.openrewrite.gradle.internal.@Nullable Dependency getMapEntriesDependency(List<Expression> arguments) {
            String group = null;
            String artifact = null;
            String version = null;
            String classifier = null;
            String ext = null;

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
                String keyValue = ((String) key.getValue()).toLowerCase();
                switch (keyValue) {
                    case "group":
                        group = (String) value.getValue();
                        break;
                    case "name":
                        artifact = (String) value.getValue();
                        break;
                    case "version":
                        version = (String) value.getValue();
                        break;
                    case "classifier":
                        classifier = (String) value.getValue();
                        break;
                    case "ext":
                        ext = (String) value.getValue();
                        break;
                }
            }

            if (group == null || artifact == null) {
                return null;
            }

            return new org.openrewrite.gradle.internal.Dependency(group, artifact, version, classifier, ext);
        }

        private static String parseArgumentString(List<J> arguments) {
            StringBuilder builder = new StringBuilder();
            arguments.forEach(argument -> builder.append(parseTemplateArgument(argument)));
            return builder.toString();
        }

        private static String parseTemplateArgument(J argument) {
            if (argument instanceof J.Literal) {
                return ((J.Literal) argument).getValue() != null ? ((J.Literal) argument).getValue().toString() : "";
            } else if (argument instanceof J.Identifier) {
                return ((J.Identifier) argument).getSimpleName();
            } else if (argument instanceof G.GString.Value) {
                return parseTemplateArgument(((G.GString.Value) argument).getTree());
            } else if (argument instanceof K.StringTemplate.Expression) {
                return parseTemplateArgument(((K.StringTemplate.Expression) argument).getTree());
            }
            return "";
        }
    }

    private J.MethodInvocation withVersion(J.MethodInvocation m, String version) {
        if (requestedDependency.getVersion() == null) {
            return m;
        }
        return m.withArguments(ListUtils.map(m.getArguments(), (ix, argument) -> {
            if (ix == 0 && argument instanceof J.Literal) {
                return ChangeStringLiteral.withStringValue((J.Literal) argument, requestedDependency.withVersion(version).toStringNotation());
            } else if (ix == 0 && argument instanceof G.GString) {
                G.GString gstring = (G.GString) argument;
                List<J> strings = gstring.getStrings();
                // only support parameterized version for now -> no changes need to be made to the template as the version is a variable
                return argument;
            } else if (ix == 0 && argument instanceof G.MapLiteral) {
                return ((G.MapLiteral) argument).withElements(ListUtils.map(((G.MapLiteral) argument).getElements(), mapEntry -> replaceMapArgument(mapEntry, "version", version)));
            } else if (argument instanceof G.MapEntry) {
                return replaceMapArgument((G.MapEntry) argument, "version", version);
            } else if (argument instanceof J.Assignment) {
                return replaceAssignment((J.Assignment) argument, "version", version);
            } else if (argument instanceof K.StringTemplate) {
                K.StringTemplate template = (K.StringTemplate) argument;
                List<J> strings = template.getStrings();
                // only support parameterized version for now -> no changes need to be made to the template as the version is a variable
                return argument;
            }

            return argument;
        }));
    }

    private static G.MapEntry replaceMapArgument(G.MapEntry entry, String key, String newValue) {
        if (!(entry.getKey() instanceof J.Literal) || !(entry.getValue() instanceof J.Literal)) {
            return entry;
        }
        J.Literal entryKey = (J.Literal) entry.getKey();
        J.Literal value = (J.Literal) entry.getValue();
        if (!(entryKey.getValue() instanceof String) || !(value.getValue() instanceof String)) {
            return entry;
        }
        if (key.equalsIgnoreCase((String) entryKey.getValue())) {
            return entry.withValue(ChangeStringLiteral.withStringValue(value, newValue));
        }
        return entry;
    }

    private static J.Assignment replaceAssignment(J.Assignment arg, String name, String newValue) {
        if (!(arg.getVariable() instanceof J.Identifier) || !(arg.getAssignment() instanceof J.Literal)) {
            return arg;
        }
        J.Identifier identifier = (J.Identifier) arg.getVariable();
        J.Literal value = (J.Literal) arg.getAssignment();
        if (!(value.getValue() instanceof String)) {
            return arg;
        }
        if (name.equals(identifier.getSimpleName())) {
            arg.withAssignment(ChangeStringLiteral.withStringValue(value, newValue));
        }
        return arg;
    }
}
