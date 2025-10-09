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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.internal.ChangeStringLiteral;
import org.openrewrite.gradle.internal.DependencyStringNotationConverter;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
public class GradleDependency implements Trait<J.MethodInvocation> {
    Cursor cursor;

    @Getter
    ResolvedDependency resolvedDependency;

    /**
     * Gets the resolved group ID of the dependency after Gradle's dependency resolution.
     *
     * @return The resolved group ID (e.g., "com.google.guava" from "com.google.guava:guava:VERSION")
     */
    public String getGroupId() {
        return resolvedDependency.getGroupId();
    }

    /**
     * Gets the resolved artifact ID of the dependency after Gradle's dependency resolution.
     *
     * @return The resolved artifact ID (e.g., "guava" from "com.google.guava:guava:VERSION")
     */
    public String getArtifactId() {
        return resolvedDependency.getArtifactId();
    }

    /**
     * Gets the declared group ID from the dependency declaration.
     * This may be a literal value or a variable name/expression.
     * For platform dependencies, automatically looks at the inner dependency.
     *
     * @return The declared group ID string, or null if it cannot be determined
     */
    public @Nullable String getDeclaredGroupId() {
        // If this is a platform dependency, delegate to the inner dependency
        if (isPlatform()) {
            GradleDependency platformDep = getPlatformDependency();
            return platformDep != null ? platformDep.getDeclaredGroupId() : null;
        }

        J.MethodInvocation m = cursor.getValue();
        List<Expression> depArgs = m.getArguments();

        if (depArgs.isEmpty()) {
            return null;
        }

        Expression arg = depArgs.get(0);

        // String literal notation: "group:artifact:version"
        if (arg instanceof J.Literal && ((J.Literal) arg).getValue() instanceof String) {
            Dependency dep =
                DependencyStringNotationConverter.parse((String) ((J.Literal) arg).getValue());
            return dep != null ? dep.getGroupId() : null;
        }

        // GString notation: "group:artifact:$version"
        if (arg instanceof G.GString) {
            List<J> strings = ((G.GString) arg).getStrings();
            if (!strings.isEmpty() && strings.get(0) instanceof J.Literal) {
                Dependency dep =
                    DependencyStringNotationConverter.parse((String) ((J.Literal) strings.get(0)).getValue());
                return dep != null ? dep.getGroupId() : null;
            }
        }

        // Kotlin string template: "group:artifact:$version"
        if (arg instanceof K.StringTemplate) {
            List<J> strings = ((K.StringTemplate) arg).getStrings();
            if (!strings.isEmpty() && strings.get(0) instanceof J.Literal) {
                Dependency dep =
                    DependencyStringNotationConverter.parse((String) ((J.Literal) strings.get(0)).getValue());
                return dep != null ? dep.getGroupId() : null;
            }
        }

        // Map notation - look for "group" entry
        if (depArgs.size() >= 2) {
            for (Expression e : depArgs) {
                if (e instanceof G.MapEntry) {
                    G.MapEntry entry = (G.MapEntry) e;
                    if (entry.getKey() instanceof J.Literal &&
                        "group".equals(((J.Literal) entry.getKey()).getValue())) {
                        return extractValueAsString(entry.getValue());
                    }
                } else if (e instanceof J.Assignment) {
                    J.Assignment assignment = (J.Assignment) e;
                    if (assignment.getVariable() instanceof J.Identifier &&
                        "group".equals(((J.Identifier) assignment.getVariable()).getSimpleName())) {
                        return extractValueAsString(assignment.getAssignment());
                    }
                }
            }
        }

        // Map literal notation
        if (arg instanceof G.MapLiteral) {
            for (G.MapEntry entry : ((G.MapLiteral) arg).getElements()) {
                if (entry.getKey() instanceof J.Literal &&
                    "group".equals(((J.Literal) entry.getKey()).getValue())) {
                    return extractValueAsString(entry.getValue());
                }
            }
        }

        return null;
    }

    /**
     * Gets the declared artifact ID from the dependency declaration.
     * This may be a literal value or a variable name/expression.
     * For platform dependencies, automatically looks at the inner dependency.
     *
     * @return The declared artifact ID string, or null if it cannot be determined
     */
    public @Nullable String getDeclaredArtifactId() {
        // If this is a platform dependency, delegate to the inner dependency
        if (isPlatform()) {
            GradleDependency platformDep = getPlatformDependency();
            return platformDep != null ? platformDep.getDeclaredArtifactId() : null;
        }

        J.MethodInvocation m = cursor.getValue();
        List<Expression> depArgs = m.getArguments();

        if (depArgs.isEmpty()) {
            return null;
        }

        Expression arg = depArgs.get(0);

        // String literal notation: "group:artifact:version"
        if (arg instanceof J.Literal && ((J.Literal) arg).getValue() instanceof String) {
            Dependency dep = DependencyStringNotationConverter.parse((String) ((J.Literal) arg).getValue());
            return dep != null ? dep.getArtifactId() : null;
        }

        // GString notation: "group:artifact:$version"
        if (arg instanceof G.GString) {
            List<J> strings = ((G.GString) arg).getStrings();
            if (!strings.isEmpty() && strings.get(0) instanceof J.Literal) {
                Dependency dep = DependencyStringNotationConverter.parse((String) ((J.Literal) strings.get(0)).getValue());
                return dep != null ? dep.getArtifactId() : null;
            }
        }

        // Kotlin string template: "group:artifact:$version"
        if (arg instanceof K.StringTemplate) {
            List<J> strings = ((K.StringTemplate) arg).getStrings();
            if (!strings.isEmpty() && strings.get(0) instanceof J.Literal) {
                Dependency dep = DependencyStringNotationConverter.parse((String) ((J.Literal) strings.get(0)).getValue());
                return dep != null ? dep.getArtifactId() : null;
            }
        }

        // Map notation - look for "name" or "artifact" entry
        if (depArgs.size() >= 2) {
            for (Expression e : depArgs) {
                if (e instanceof G.MapEntry) {
                    G.MapEntry entry = (G.MapEntry) e;
                    if (entry.getKey() instanceof J.Literal) {
                        String key = (String) ((J.Literal) entry.getKey()).getValue();
                        if ("name".equals(key) || "artifact".equals(key)) {
                            return extractValueAsString(entry.getValue());
                        }
                    }
                } else if (e instanceof J.Assignment) {
                    J.Assignment assignment = (J.Assignment) e;
                    if (assignment.getVariable() instanceof J.Identifier) {
                        String key = ((J.Identifier) assignment.getVariable()).getSimpleName();
                        if ("name".equals(key) || "artifact".equals(key)) {
                            return extractValueAsString(assignment.getAssignment());
                        }
                    }
                }
            }
        }

        // Map literal notation
        if (arg instanceof G.MapLiteral) {
            for (G.MapEntry entry : ((G.MapLiteral) arg).getElements()) {
                if (entry.getKey() instanceof J.Literal) {
                    String key = (String) ((J.Literal) entry.getKey()).getValue();
                    if ("name".equals(key) || "artifact".equals(key)) {
                        return extractValueAsString(entry.getValue());
                    }
                }
            }
        }

        return null;
    }

    /**
     * Gets the declared version from the dependency declaration.
     * This may be a literal value or a variable name/expression.
     * For platform dependencies, automatically looks at the inner dependency.
     *
     * @return The declared version string (literal or variable name), or null if it cannot be determined
     */
    public @Nullable String getDeclaredVersion() {
        // For version variables, getVersionVariable() already handles this
        String versionVar = getVersionVariable();
        if (versionVar != null) {
            return versionVar;
        }

        // If this is a platform dependency, delegate to the inner dependency
        if (isPlatform()) {
            GradleDependency platformDep = getPlatformDependency();
            return platformDep != null ? platformDep.getDeclaredVersion() : null;
        }

        J.MethodInvocation m = cursor.getValue();
        List<Expression> depArgs = m.getArguments();

        if (depArgs.isEmpty()) {
            return null;
        }

        Expression arg = depArgs.get(0);

        // String literal notation: "group:artifact:version"
        if (arg instanceof J.Literal && ((J.Literal) arg).getValue() instanceof String) {
            Dependency dep =
                DependencyStringNotationConverter.parse((String) ((J.Literal) arg).getValue());
            return dep != null ? dep.getVersion() : null;
        }

        // Map notation - look for "version" entry
        if (depArgs.size() >= 3) {
            for (Expression e : depArgs) {
                if (e instanceof G.MapEntry) {
                    G.MapEntry entry = (G.MapEntry) e;
                    if (entry.getKey() instanceof J.Literal &&
                        "version".equals(((J.Literal) entry.getKey()).getValue())) {
                        return extractValueAsString(entry.getValue());
                    }
                } else if (e instanceof J.Assignment) {
                    J.Assignment assignment = (J.Assignment) e;
                    if (assignment.getVariable() instanceof J.Identifier &&
                        "version".equals(((J.Identifier) assignment.getVariable()).getSimpleName())) {
                        return extractValueAsString(assignment.getAssignment());
                    }
                }
            }
        }

        // Map literal notation
        if (arg instanceof G.MapLiteral) {
            for (G.MapEntry entry : ((G.MapLiteral) arg).getElements()) {
                if (entry.getKey() instanceof J.Literal &&
                    "version".equals(((J.Literal) entry.getKey()).getValue())) {
                    return extractValueAsString(entry.getValue());
                }
            }
        }

        return null;
    }

    /**
     * Helper method to extract a string value from various expression types.
     * Handles literals, identifiers, field access, method invocations, and GStrings.
     */
    private @Nullable String extractValueAsString(Expression value) {
        if (value instanceof J.Literal) {
            Object literalValue = ((J.Literal) value).getValue();
            return literalValue instanceof String ? (String) literalValue : null;
        } else if (value instanceof J.Identifier) {
            return ((J.Identifier) value).getSimpleName();
        } else if (value instanceof J.FieldAccess) {
            return value.printTrimmed(cursor);
        } else if (value instanceof J.MethodInvocation) {
            // Handle property('name') or findProperty('name') patterns
            J.MethodInvocation mi = (J.MethodInvocation) value;
            String methodName = mi.getSimpleName();
            if (("property".equals(methodName) || "findProperty".equals(methodName)) &&
                mi.getArguments().size() == 1 &&
                mi.getArguments().get(0) instanceof J.Literal) {
                Object arg = ((J.Literal) mi.getArguments().get(0)).getValue();
                if (arg instanceof String) {
                    return (String) arg;
                }
            }

            // Also handle project.property('name') and project.findProperty('name')
            if (mi.getSelect() instanceof J.Identifier &&
                "project".equals(((J.Identifier) mi.getSelect()).getSimpleName()) &&
                ("property".equals(methodName) || "findProperty".equals(methodName)) &&
                mi.getArguments().size() == 1 &&
                mi.getArguments().get(0) instanceof J.Literal) {
                Object arg = ((J.Literal) mi.getArguments().get(0)).getValue();
                if (arg instanceof String) {
                    return (String) arg;
                }
            }

            // For other method invocations, return the full expression
            return value.printTrimmed(cursor);
        } else if (value instanceof G.Binary) {
            G.Binary binary = (G.Binary) value;

            // Handle project.properties['name'] pattern (G.Binary with Access operator)
            if (binary.getOperator() == G.Binary.Type.Access && binary.getRight() instanceof J.Literal) {
                J.Literal right = (J.Literal) binary.getRight();
                if (right.getValue() instanceof String) {
                    return (String) right.getValue();
                }
            }

            // Handle binary expressions like "$guavaVersion" + "-jre"
            if (binary.getLeft() instanceof G.GString) {
                G.GString left = (G.GString) binary.getLeft();
                List<J> strings = left.getStrings();
                if (strings.size() == 1 && strings.get(0) instanceof G.GString.Value) {
                    G.GString.Value gStringValue = (G.GString.Value) strings.get(0);
                    Tree tree = gStringValue.getTree();
                    if (tree instanceof J.Identifier) {
                        return ((J.Identifier) tree).getSimpleName();
                    }
                }
            } else if (binary.getLeft() instanceof J.Identifier) {
                return ((J.Identifier) binary.getLeft()).getSimpleName();
            }

            // For other binary expressions, return the full expression
            return value.printTrimmed(cursor);
        } else if (value instanceof G.GString) {
            G.GString gString = (G.GString) value;
            List<J> strings = gString.getStrings();
            if (!strings.isEmpty() && strings.get(0) instanceof G.GString.Value) {
                G.GString.Value gStringValue = (G.GString.Value) strings.get(0);
                Tree tree = gStringValue.getTree();
                if (tree instanceof J.Identifier) {
                    return ((J.Identifier) tree).getSimpleName();
                } else if (tree instanceof J.FieldAccess) {
                    return tree.printTrimmed(cursor);
                } else if (tree instanceof J.MethodInvocation) {
                    // Recursively handle method invocations within GString
                    return extractValueAsString((J.MethodInvocation) tree);
                } else if (tree instanceof G.Binary) {
                    // Recursively handle binary expressions within GString
                    return extractValueAsString((G.Binary) tree);
                }
            }
        } else if (value instanceof K.StringTemplate) {
            K.StringTemplate template = (K.StringTemplate) value;
            List<J> strings = template.getStrings();
            if (!strings.isEmpty() && strings.get(0) instanceof K.StringTemplate.Expression) {
                K.StringTemplate.Expression templateExp = (K.StringTemplate.Expression) strings.get(0);
                Tree tree = templateExp.getTree();
                if (tree instanceof J.Identifier) {
                    return ((J.Identifier) tree).getSimpleName();
                }
            }
        }
        return null;
    }

    /**
     * Checks if this dependency declaration uses platform() or enforcedPlatform().
     *
     * @return true if the dependency is wrapped in platform() or enforcedPlatform()
     */
    public boolean isPlatform() {
        J.MethodInvocation m = cursor.getValue();
        if (m.getArguments().isEmpty()) {
            return false;
        }
        Expression firstArg = m.getArguments().get(0);
        if (!(firstArg instanceof J.MethodInvocation)) {
            return false;
        }
        String methodName = ((J.MethodInvocation) firstArg).getSimpleName();
        return "platform".equals(methodName) || "enforcedPlatform".equals(methodName);
    }

    /**
     * Gets the inner dependency if this dependency uses platform() or enforcedPlatform().
     * For example, for {@code implementation platform("com.example:bom:1.0")}, this returns
     * the GradleDependency representing {@code platform("com.example:bom:1.0")}.
     *
     * @return The inner GradleDependency if this is a platform dependency, or null otherwise
     */
    public @Nullable GradleDependency getPlatformDependency() {
        J.MethodInvocation m = cursor.getValue();
        if (m.getArguments().isEmpty()) {
            return null;
        }
        Expression firstArg = m.getArguments().get(0);
        if (!(firstArg instanceof J.MethodInvocation)) {
            return null;
        }
        J.MethodInvocation platformMethod = (J.MethodInvocation) firstArg;
        String methodName = platformMethod.getSimpleName();
        if (!"platform".equals(methodName) && !"enforcedPlatform".equals(methodName)) {
            return null;
        }

        // Try to get the dependency trait for the inner platform() method invocation
        return new Matcher().get(platformMethod, cursor).orElse(null);
    }

    /**
     * Gets the version variable name if the dependency's version is specified via a variable reference
     * rather than a literal value. Handles various dependency notation formats.
     * If this dependency uses platform() or enforcedPlatform(), this method automatically
     * looks at the inner dependency declaration.
     *
     * @return The variable name used for the version, or null if the version is a literal or cannot be determined
     */
    public @Nullable String getVersionVariable() {
        // If this is a platform dependency, delegate to the inner dependency
        if (isPlatform()) {
            GradleDependency platformDep = getPlatformDependency();
            return platformDep != null ? platformDep.getVersionVariable() : null;
        }

        J.MethodInvocation m = cursor.getValue();
        List<Expression> depArgs = m.getArguments();

        if (depArgs.isEmpty()) {
            return null;
        }

        Expression arg = depArgs.get(0);

        // Handle "group:artifact:$version" patterns with GString
        if (arg instanceof G.GString) {
            List<J> strings = ((G.GString) arg).getStrings();
            if (strings.size() == 2 && strings.get(0) instanceof J.Literal && strings.get(1) instanceof G.GString.Value) {
                Object versionTree = ((G.GString.Value) strings.get(1)).getTree();
                if (versionTree instanceof J.Identifier) {
                    return ((J.Identifier) versionTree).getSimpleName();
                } else if (versionTree instanceof J.FieldAccess) {
                    return ((J.FieldAccess) versionTree).printTrimmed(cursor);
                }
            }
        }

        // Handle "group:artifact:$version" patterns with Kotlin StringTemplate
        if (arg instanceof K.StringTemplate) {
            List<J> strings = ((K.StringTemplate) arg).getStrings();
            if (strings.size() == 2 && strings.get(0) instanceof J.Literal && strings.get(1) instanceof K.StringTemplate.Expression) {
                Object versionTree = ((K.StringTemplate.Expression) strings.get(1)).getTree();
                if (versionTree instanceof J.Identifier) {
                    return ((J.Identifier) versionTree).getSimpleName();
                }
            }
        }

        // Handle map notation: group: 'x', name: 'y', version: variableName
        if (depArgs.size() >= 3) {
            Expression versionExp = null;

            if (depArgs.get(0) instanceof G.MapEntry && depArgs.get(1) instanceof G.MapEntry && depArgs.get(2) instanceof G.MapEntry) {
                versionExp = ((G.MapEntry) depArgs.get(2)).getValue();
            } else if (depArgs.get(0) instanceof J.Assignment && depArgs.get(1) instanceof J.Assignment && depArgs.get(2) instanceof J.Assignment) {
                versionExp = ((J.Assignment) depArgs.get(2)).getAssignment();
            }

            if (versionExp instanceof J.Identifier) {
                return ((J.Identifier) versionExp).getSimpleName();
            } else if (versionExp instanceof J.FieldAccess) {
                return ((J.FieldAccess) versionExp).printTrimmed(cursor);
            }
        }

        return null;
    }

    /**
     * Gets the tree (J.MethodInvocation) that should be modified when updating this dependency.
     * For regular dependencies, returns the current tree.
     * For dependencies with platform wrappers, returns the inner platform() or enforcedPlatform() method invocation.
     *
     * @return The J.MethodInvocation to update
     */
    public J.MethodInvocation getTreeToUpdate() {
        if (isPlatform()) {
            // The first argument is a platform() or enforcedPlatform() call
            J.MethodInvocation m = getTree();
            if (!m.getArguments().isEmpty() && m.getArguments().get(0) instanceof J.MethodInvocation) {
                return (J.MethodInvocation) m.getArguments().get(0);
            }
        }
        return getTree();
    }

    /**
     * Wraps an updated dependency tree with the appropriate wrapper if needed.
     * For regular dependencies, returns the updated tree as-is.
     * For dependencies with platform wrappers, wraps the updated platform() call with the outer configuration method.
     *
     * @param updatedTree The updated dependency tree (potentially from getTreeToUpdate())
     * @return The properly wrapped J.MethodInvocation
     */
    public J.MethodInvocation wrapUpdatedTree(J.MethodInvocation updatedTree) {
        if (!isPlatform()) {
            return updatedTree;
        }

        J.MethodInvocation m = getTree();
        // Replace the platform method invocation argument with the updated one
        return m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> updatedTree));
    }

    /**
     * Removes the version from a dependency declaration.
     * This method handles various dependency notation formats including string literals,
     * map literals, and map entries.
     * If this dependency uses platform() or enforcedPlatform(), this method automatically
     * operates on the inner dependency declaration.
     *
     * @return A new GradleDependency with the version removed from the cursor's method invocation,
     *         or the original GradleDependency if the version cannot be removed
     */
    public GradleDependency removeVersion() {
        J.MethodInvocation m = cursor.getValue();
        J.MethodInvocation updated = m;

        // If this is a platform dependency, we need to update the inner dependency
        if (isPlatform() && m.getArguments().get(0) instanceof J.MethodInvocation) {
            J.MethodInvocation platformMethod = (J.MethodInvocation) m.getArguments().get(0);
            GradleDependency platformDep = getPlatformDependency();
            if (platformDep != null) {
                GradleDependency updatedPlatformDep = platformDep.removeVersion();
                if (updatedPlatformDep != platformDep) {
                    // The inner dependency was modified, so update the outer method invocation
                    updated = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg ->
                        platformMethod.withArguments(updatedPlatformDep.getTree().getArguments())
                    ));
                    return new GradleDependency(new Cursor(cursor.getParent(), updated), resolvedDependency);
                }
            }
            return this;
        }

        if (m.getArguments().get(0) instanceof J.Literal) {
            J.Literal l = (J.Literal) m.getArguments().get(0);
            if (l.getType() == JavaType.Primitive.String) {
                Dependency dep = DependencyStringNotationConverter.parse((String) l.getValue());
                if (dep == null || dep.getClassifier() != null || (dep.getType() != null && !"jar".equals(dep.getType()))) {
                    return this;
                }
                updated = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg ->
                        ChangeStringLiteral.withStringValue(l, dep.withGav(dep.getGav().withVersion(null)).toStringNotation()))
                );
            }
        } else if (m.getArguments().get(0) instanceof G.MapLiteral) {
            updated = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                G.MapLiteral mapLiteral = (G.MapLiteral) arg;
                return mapLiteral.withElements(ListUtils.map(mapLiteral.getElements(), entry -> {
                    if (entry.getKey() instanceof J.Literal && "version".equals(((J.Literal) entry.getKey()).getValue())) {
                        return null;
                    }
                    return entry;
                }));
            }));
        } else if (m.getArguments().get(0) instanceof G.MapEntry) {
            updated = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                G.MapEntry entry = (G.MapEntry) arg;
                if (entry.getKey() instanceof J.Literal && "version".equals(((J.Literal) entry.getKey()).getValue())) {
                    return null;
                }
                return entry;
            }));
        }

        if (updated == m) {
            return this;
        }

        return new GradleDependency(new Cursor(cursor.getParent(), updated), resolvedDependency);
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
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<GradleDependency, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, P p) {
                    GradleDependency dependency = test(getCursor());
                    return dependency != null ?
                            (J) visitor.visit(dependency, p) :
                            super.visitMethodInvocation(method, p);
                }
            };
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

                Dependency dependency = null;
                Expression argument = methodInvocation.getArguments().get(0);
                if (argument instanceof J.Literal || argument instanceof G.GString || argument instanceof G.MapEntry || argument instanceof G.MapLiteral || argument instanceof J.Assignment || argument instanceof K.StringTemplate) {
                    dependency = parseDependency(methodInvocation.getArguments());
                } else if (argument instanceof J.Binary && ((J.Binary) argument).getLeft() instanceof J.Literal) {
                    dependency = parseDependency(Arrays.asList(((J.Binary) argument).getLeft()));
                } else if (argument instanceof J.MethodInvocation) {
                    if ("platform".equals(((J.MethodInvocation) argument).getSimpleName()) ||
                            "enforcedPlatform".equals(((J.MethodInvocation) argument).getSimpleName())) {
                        dependency = parseDependency(((J.MethodInvocation) argument).getArguments());
                    } else if ("project".equals(((J.MethodInvocation) argument).getSimpleName())) {
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
                                    return new GradleDependency(cursor, withRequested(resolvedDependency, dependency));
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
                                            return new GradleDependency(cursor, withRequested(resolvedDependency, dependency));
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
                            .type(dependency.getType())
                            .requested(Dependency.builder()
                                    .scope(methodInvocation.getSimpleName())
                                    .type(dependency.getType())
                                    .gav(new GroupArtifactVersion(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()))
                                    .classifier(dependency.getClassifier())
                                    .build())
                            .build();
                    return new GradleDependency(cursor, withRequested(resolvedDependency, dependency));
                }
            }

            return null;
        }

        /**
         * Our Gradle model doesn't truly know the requested versions as it isn't able to get that from the Gradle API.
         * So if this Trait has figured out which declaration made the request resulting in a particular resolved dependency
         * use that more-accurate information instead.
         */
        private static ResolvedDependency withRequested(ResolvedDependency resolved, Dependency requested) {
            return resolved.withRequested(resolved.getRequested().withGav(requested.getGav()));
        }


        private static @Nullable GradleDependencyConfiguration getConfiguration(@Nullable GradleProject gradleProject, J.MethodInvocation methodInvocation) {
            if (gradleProject == null) {
                return null;
            }

            String methodName = methodInvocation.getSimpleName();
            if ("classpath".equals(methodName)) {
                return gradleProject.getBuildscript().getConfiguration(methodName);
            } else {
                return gradleProject.getConfiguration(methodName);
            }
        }

        private boolean withinDependenciesBlock(Cursor cursor) {
            return withinBlock(cursor, "dependencies");
        }

        private boolean withinDependencyConstraintsBlock(Cursor cursor) {
            return withinBlock(cursor, "constraints") && withinDependenciesBlock(cursor);
        }

        private @Nullable Dependency parseDependency(List<Expression> arguments) {
            Expression argument = arguments.get(0);
            if (argument instanceof J.Literal) {
                return DependencyStringNotationConverter.parse((String) ((J.Literal) argument).getValue());
            } else if (argument instanceof G.GString) {
                G.GString gstring = (G.GString) argument;
                List<J> strings = gstring.getStrings();
                if (strings.size() >= 2 && strings.get(0) instanceof J.Literal && ((J.Literal) strings.get(0)).getValue() != null) {
                    return DependencyStringNotationConverter.parse((String) ((J.Literal) strings.get(0)).getValue());
                }
            } else if (argument instanceof G.MapLiteral) {
                List<Expression> mapEntryExpressions = ((G.MapLiteral) argument).getElements()
                        .stream()
                        .map(e -> (Expression) e)
                        .collect(toList());
                return getMapEntriesDependency(mapEntryExpressions);
            } else if (argument instanceof G.MapEntry) {
                return getMapEntriesDependency(arguments);
            } else if (argument instanceof J.Assignment) {
                String group = null;
                String artifact = null;

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
                    }
                }

                if (group == null || artifact == null) {
                    return null;
                }

                return Dependency.builder()
                        .gav(new GroupArtifactVersion(group, artifact, null))
                        .build();
            } else if (argument instanceof K.StringTemplate) {
                K.StringTemplate template = (K.StringTemplate) argument;
                List<J> strings = template.getStrings();
                if (strings.size() >= 2 && strings.get(0) instanceof J.Literal && ((J.Literal) strings.get(0)).getValue() != null) {
                    return DependencyStringNotationConverter.parse((String) ((J.Literal) strings.get(0)).getValue());
                }
            }

            return null;
        }

        private static @Nullable Dependency getMapEntriesDependency(List<Expression> arguments) {
            String group = null;
            String artifact = null;

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
                }
            }

            if (group == null || artifact == null) {
                return null;
            }

            return org.openrewrite.maven.tree.Dependency.builder()
                    .gav(new GroupArtifactVersion(group, artifact, null))
                    .build();
        }
    }
}
