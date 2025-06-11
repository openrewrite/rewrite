package org.openrewrite.gradle.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.gradle.internal.Dependency;
import org.openrewrite.gradle.internal.DependencyStringNotationConverter;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.maven.tree.*;
import org.openrewrite.trait.Trait;

import java.util.List;
import java.util.stream.Collectors;

import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
public class GradleDependencyConstraint implements Trait<J.MethodInvocation> {
    Cursor cursor;
    GroupArtifactVersion gav;

    public static class Matcher extends GradleTraitMatcher<GradleDependencyConstraint> {
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
        protected @Nullable GradleDependencyConstraint test(Cursor cursor) {
            Object object = cursor.getValue();
            if (object instanceof J.MethodInvocation) {
                J.MethodInvocation methodInvocation = (J.MethodInvocation) object;

                if (!(withinBlock(cursor, "dependencies") && withinBlock(cursor, "constraints"))) {
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

                GroupArtifactVersion gav = null;
                Expression argument = methodInvocation.getArguments().get(0);
                if (argument instanceof J.Literal || argument instanceof G.GString || argument instanceof G.MapEntry || argument instanceof G.MapLiteral || argument instanceof J.Assignment || argument instanceof K.StringTemplate || argument instanceof J.Identifier) {
                    gav = parseGroupArtifactVersion(methodInvocation.getArguments());
                } else if (argument instanceof J.MethodInvocation) {
                    if ("enforcedPlatform".equals(((J.MethodInvocation) argument).getSimpleName())) {
                        gav = parseGroupArtifactVersion(((J.MethodInvocation) argument).getArguments());
                    } else if ("project".equals(((J.MethodInvocation) argument).getSimpleName())) {
                        // project dependencies are not yet supported
                        return null;
                    }
                }

                if (gav == null) {
                    return null;
                }

                if ((groupId == null || matchesGlob(gav.getGroupId(), groupId)) &&
                        (artifactId == null || matchesGlob(gav.getArtifactId(), artifactId))) {
                    // Couldn't find the actual resolved dependency, return a virtualized one instead
                    return new GradleDependencyConstraint(cursor, gav);
                }
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

        private @Nullable GroupArtifactVersion parseGroupArtifactVersion(List<Expression> arguments) {
            Expression argument = arguments.get(0);
            if (argument instanceof J.Literal) {
                Dependency maybeDependency = DependencyStringNotationConverter
                        .parse((String) ((J.Literal) argument).getValue());
                return maybeDependency != null ? maybeDependency.getGav() : null;
            } else if (argument instanceof G.GString) {
                G.GString gstring = (G.GString) argument;
                List<J> strings = gstring.getStrings();
                if (strings.size() >= 2 && strings.get(0) instanceof J.Literal && ((J.Literal) strings.get(0)).getValue() != null) {
                    Dependency maybeDependency = DependencyStringNotationConverter
                            .parse((String) ((J.Literal) strings.get(0)).getValue());
                    return maybeDependency != null ? maybeDependency.getGav() : null;
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
                    } else if ("name".equals(name)) {
                        artifact = (String) value.getValue();
                    } else if ("version".equals(name)) {
                        version = (String) value.getValue();
                    }
                }

                if (group == null || artifact == null) {
                    return null;
                }

                return new GroupArtifactVersion(group, artifact, version);
            } else if (argument instanceof K.StringTemplate) {
                K.StringTemplate template = (K.StringTemplate) argument;
                List<J> strings = template.getStrings();
                if (strings.size() >= 2 && strings.get(0) instanceof J.Literal && ((J.Literal) strings.get(0)).getValue() != null) {
                    Dependency maybeDependency = DependencyStringNotationConverter
                            .parse((String) ((J.Literal) strings.get(0)).getValue());
                    return maybeDependency != null ? maybeDependency.getGav() : null;
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
    }
}
