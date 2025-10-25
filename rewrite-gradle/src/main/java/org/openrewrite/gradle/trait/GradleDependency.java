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
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.internal.ChangeStringLiteral;
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
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.DependencyNotation;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.toList;

@Value
public class GradleDependency implements Trait<J.MethodInvocation> {

    private static final MethodMatcher DEPENDENCY_DSL_MATCHER = new MethodMatcher("DependencyHandlerSpec *(..)");

    private static @Nullable GradleProject getGradleProject(Cursor cursor) {
        SourceFile sourceFile = cursor.firstEnclosing(SourceFile.class);
        if (sourceFile == null) {
            return null;
        }

        Optional<GradleProject> maybeGp = sourceFile.getMarkers().findFirst(GradleProject.class);
        return maybeGp.orElse(null);
    }

    private static boolean withinBlock(Cursor cursor, String name) {
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

    private static boolean withinDependenciesBlock(Cursor cursor) {
        return withinBlock(cursor, "dependencies");
    }

    private static boolean withinDependencyConstraintsBlock(Cursor cursor) {
        return withinBlock(cursor, "constraints") && withinDependenciesBlock(cursor);
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

    public static boolean isDependencyDeclaration(Cursor cursor) {
        Object object = cursor.getValue();
        if (!(object instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation methodInvocation = (J.MethodInvocation) object;

        if (!withinDependenciesBlock(cursor)) {
            return false;
        }

        if (withinDependencyConstraintsBlock(cursor)) {
            // A dependency constraint is different from an actual dependency
            return false;
        }

        GradleProject gradleProject = getGradleProject(cursor);
        GradleDependencyConfiguration gdc = getConfiguration(gradleProject, methodInvocation);
        if (gdc == null && !(DEPENDENCY_DSL_MATCHER.matches(methodInvocation) && !"project".equals(methodInvocation.getSimpleName()))) {
            return false;
        }

        // If the DSL matcher matches, this is definitely a dependency (Groovy with type attribution)
        if (DEPENDENCY_DSL_MATCHER.matches(methodInvocation)) {
            return true;
        }

        // If we have a valid configuration, this is a dependency even without type attribution (Kotlin DSL)
        // This handles cases where methodType is null but Gradle resolved the configuration
        return gdc != null;
    }

    @With
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
     * Gets the resolved version of the dependency
     *
     * @return the resolved version of the dependency, after any variable substitutions have taken place
     */
    public String getVersion() {
        return resolvedDependency.getVersion();
    }

    public GroupArtifactVersion getGav() {
        return resolvedDependency.getGav().asGroupArtifactVersion();
    }

    @SuppressWarnings("unused")
    public ResolvedGroupArtifactVersion getResolvedGav() {
        return resolvedDependency.getGav();
    }

    /**
     * Gets the configuration name for this dependency.
     * For example, "implementation", "testImplementation", "api", etc.
     * For platform dependencies wrapped in platform() or enforcedPlatform(),
     * returns the configuration name of the outer method invocation.
     *
     * @return The configuration name
     */
    public String getConfigurationName() {
        return getTree().getSimpleName();
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

        // Binary concatenation: "group:artifact:" + version
        if (arg instanceof J.Binary) {
            J.Binary binary = (J.Binary) arg;
            if (binary.getLeft() instanceof J.Literal) {
                J.Literal left = (J.Literal) binary.getLeft();
                if (left.getValue() instanceof String) {
                    String leftStr = (String) left.getValue();
                    // Parse the group:artifact part, adding dummy version if needed
                    if (!leftStr.contains(":")) {
                        return null;
                    }
                    String toParse = leftStr.endsWith(":") ? leftStr + "1.0" : leftStr;
                    Dependency dep = DependencyNotation.parse(toParse);
                    return dep != null ? dep.getGroupId() : null;
                }
            }
        }

        // String literal notation: "group:artifact:version"
        if (arg instanceof J.Literal && ((J.Literal) arg).getValue() instanceof String) {
            Dependency dep =
                    DependencyNotation.parse((String) ((J.Literal) arg).getValue());
            return dep != null ? dep.getGroupId() : null;
        }

        // GString notation: "group:artifact:$version"
        if (arg instanceof G.GString) {
            List<J> strings = ((G.GString) arg).getStrings();
            if (!strings.isEmpty() && strings.get(0) instanceof J.Literal) {
                Dependency dep =
                        DependencyNotation.parse((String) ((J.Literal) strings.get(0)).getValue());
                return dep != null ? dep.getGroupId() : null;
            }
        }

        // Kotlin string template: "group:artifact:$version"
        if (arg instanceof K.StringTemplate) {
            List<J> strings = ((K.StringTemplate) arg).getStrings();
            if (!strings.isEmpty() && strings.get(0) instanceof J.Literal) {
                Dependency dep =
                        DependencyNotation.parse((String) ((J.Literal) strings.get(0)).getValue());
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

        // Binary concatenation: "group:artifact:" + version
        if (arg instanceof J.Binary) {
            J.Binary binary = (J.Binary) arg;
            if (binary.getLeft() instanceof J.Literal) {
                J.Literal left = (J.Literal) binary.getLeft();
                if (left.getValue() instanceof String) {
                    String leftStr = (String) left.getValue();
                    // Parse the group:artifact part, adding dummy version if needed
                    if (!leftStr.contains(":")) {
                        return null;
                    }
                    String toParse = leftStr.endsWith(":") ? leftStr + "1.0" : leftStr;
                    Dependency dep = DependencyNotation.parse(toParse);
                    return dep != null ? dep.getArtifactId() : null;
                }
            }
        }

        // String literal notation: "group:artifact:version"
        if (arg instanceof J.Literal && ((J.Literal) arg).getValue() instanceof String) {
            Dependency dep = DependencyNotation.parse((String) ((J.Literal) arg).getValue());
            return dep != null ? dep.getArtifactId() : null;
        }

        // GString notation: "group:artifact:$version"
        if (arg instanceof G.GString) {
            List<J> strings = ((G.GString) arg).getStrings();
            if (!strings.isEmpty() && strings.get(0) instanceof J.Literal) {
                Dependency dep = DependencyNotation.parse((String) ((J.Literal) strings.get(0)).getValue());
                return dep != null ? dep.getArtifactId() : null;
            }
        }

        // Kotlin string template: "group:artifact:$version"
        if (arg instanceof K.StringTemplate) {
            List<J> strings = ((K.StringTemplate) arg).getStrings();
            if (!strings.isEmpty() && strings.get(0) instanceof J.Literal) {
                Dependency dep = DependencyNotation.parse((String) ((J.Literal) strings.get(0)).getValue());
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
                    DependencyNotation.parse((String) ((J.Literal) arg).getValue());
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
     * Helper method to extract a string v from various expression types.
     * Handles literals, identifiers, field access, method invocations, and GStrings.
     */
    private @Nullable String extractValueAsString(Expression value) {
        Expression v = value.withMarkers(Markers.EMPTY);
        if (v instanceof J.Literal) {
            Object literalValue = ((J.Literal) v).getValue();
            return literalValue instanceof String ? (String) literalValue : null;
        } else if (v instanceof J.Identifier) {
            return ((J.Identifier) v).getSimpleName();
        } else if (v instanceof J.FieldAccess) {
            return v.printTrimmed(cursor);
        } else if (v instanceof J.MethodInvocation) {
            // Handle property('name') or findProperty('name') patterns
            J.MethodInvocation mi = (J.MethodInvocation) v;
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
            return v.printTrimmed(cursor);
        } else if (v instanceof G.Binary) {
            G.Binary binary = (G.Binary) v;
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
            return v.printTrimmed(cursor);
        } else if (v instanceof G.GString) {
            G.GString gString = (G.GString) v;
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
        } else if (v instanceof K.StringTemplate) {
            K.StringTemplate template = (K.StringTemplate) v;
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

        // Create a GradleDependency for the inner platform() method invocation
        // We can reuse the same ResolvedDependency since it represents the same actual dependency
        // The cursor should point to the platform() method itself
        Cursor platformCursor = new Cursor(cursor, platformMethod);
        return new GradleDependency(platformCursor, resolvedDependency);
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

        // Handle binary concatenation: "group:artifact:" + version
        if (arg instanceof J.Binary) {
            J.Binary binary = (J.Binary) arg;
            if (binary.getLeft() instanceof J.Literal && binary.getRight() instanceof J.Identifier) {
                // Check if the left side is a dependency coordinate without version
                J.Literal left = (J.Literal) binary.getLeft();
                if (left.getValue() instanceof String) {
                    String leftStr = (String) left.getValue();
                    // Check if it looks like a dependency coordinate (has at least group:artifact:)
                    String[] parts = leftStr.split(":");
                    if (parts.length >= 2) {
                        return ((J.Identifier) binary.getRight()).getSimpleName();
                    }
                }
            }
        }

        // Handle "group:artifact:$version" patterns with GString
        if (arg instanceof G.GString) {
            List<J> strings = ((G.GString) arg).getStrings();
            if (strings.size() == 2 && strings.get(0) instanceof J.Literal && strings.get(1) instanceof G.GString.Value) {
                Object versionTree = ((G.GString.Value) strings.get(1)).getTree();
                if (versionTree instanceof J.Identifier) {
                    return ((J.Identifier) versionTree).getSimpleName();
                } else if (versionTree instanceof J.FieldAccess) {
                    return ((J.FieldAccess) versionTree).printTrimmed(cursor);
                } else if (versionTree instanceof J.MethodInvocation) {
                    // Handle property('version') or findProperty('version')
                    String propName = extractPropertyNameFromMethodInvocation((J.MethodInvocation) versionTree);
                    if (propName != null) {
                        return propName;
                    }
                } else if (versionTree instanceof G.Binary) {
                    // Handle properties['version']
                    String propName = extractPropertyNameFromGBinary((G.Binary) versionTree);
                    if (propName != null) {
                        return propName;
                    }
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
                return versionExp.printTrimmed(cursor);
            } else if (versionExp instanceof J.MethodInvocation) {
                // Handle property('version') or findProperty('version')
                return extractPropertyNameFromMethodInvocation((J.MethodInvocation) versionExp);
            } else if (versionExp instanceof G.Binary) {
                // Handle properties['version']
                return extractPropertyNameFromGBinary((G.Binary) versionExp);
            } else if (versionExp instanceof G.GString) {
                // Handle GString in map notation
                G.GString gString = (G.GString) versionExp;
                List<J> strings = gString.getStrings();
                if (!strings.isEmpty() && strings.get(0) instanceof G.GString.Value) {
                    G.GString.Value versionGStringValue = (G.GString.Value) strings.get(0);
                    Object tree = versionGStringValue.getTree();
                    if (tree instanceof J.Identifier) {
                        return ((J.Identifier) tree).getSimpleName();
                    } else if (tree instanceof J.FieldAccess) {
                        return ((J.FieldAccess) tree).printTrimmed(cursor);
                    } else if (tree instanceof J.MethodInvocation) {
                        String propName = extractPropertyNameFromMethodInvocation((J.MethodInvocation) tree);
                        if (propName != null) {
                            return propName;
                        }
                    } else if (tree instanceof G.Binary) {
                        String propName = extractPropertyNameFromGBinary((G.Binary) tree);
                        if (propName != null) {
                            return propName;
                        }
                    }
                }
            } else if (versionExp instanceof K.StringTemplate) {
                // Handle Kotlin StringTemplate in map notation
                K.StringTemplate template = (K.StringTemplate) versionExp;
                List<J> strings = template.getStrings();
                if (!strings.isEmpty() && strings.get(0) instanceof K.StringTemplate.Expression) {
                    K.StringTemplate.Expression versionTemplateExpr = (K.StringTemplate.Expression) strings.get(0);
                    Object tree = versionTemplateExpr.getTree();
                    if (tree instanceof J.Identifier) {
                        return ((J.Identifier) tree).getSimpleName();
                    } else if (tree instanceof J.FieldAccess) {
                        return ((J.FieldAccess) tree).printTrimmed(cursor);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Checks if this dependency matches the given DependencyMatcher.
     * This is a convenience method that extracts the declared group and artifact IDs
     * and passes them to the matcher.
     *
     * @param matcher The DependencyMatcher to check against
     * @return true if this dependency matches the matcher's patterns
     */
    public boolean matches(DependencyMatcher matcher) {
        String groupId = getDeclaredGroupId();
        String artifactId = getDeclaredArtifactId();

        if (groupId == null || artifactId == null) {
            return false;
        }

        return matcher.matches(getDeclaredGroupId(), getDeclaredArtifactId());
    }

    /**
     * Extract property name from method invocation patterns like property('guavaVersion')
     * or findProperty('guavaVersion').
     */
    private static @Nullable String extractPropertyNameFromMethodInvocation(J.MethodInvocation mi) {
        if ("property".equals(mi.getSimpleName()) || "findProperty".equals(mi.getSimpleName())) {
            if (!mi.getArguments().isEmpty() && mi.getArguments().get(0) instanceof J.Literal) {
                J.Literal literal = (J.Literal) mi.getArguments().get(0);
                if (literal.getValue() instanceof String) {
                    return (String) literal.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Handle Groovy binary access like project.properties['guavaVersion']
     * where the binary operator is Access (bracket notation).
     */
    private static @Nullable String extractPropertyNameFromGBinary(G.Binary binary) {
        if (binary.getOperator() == G.Binary.Type.Access && binary.getRight() instanceof J.Literal) {
            J.Literal right = (J.Literal) binary.getRight();
            if (right.getValue() instanceof String) {
                return (String) right.getValue();
            }
        }
        return null;
    }

    /**
     * Removes the version from a dependency declaration.
     * This method handles various dependency notation formats including string literals,
     * map literals, and map entries.
     * If this dependency uses platform() or enforcedPlatform(), this method automatically
     * operates on the inner dependency declaration.
     *
     * @return A new GradleDependency with the version removed from the cursor's method invocation,
     * or the original GradleDependency if the version cannot be removed
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
                Dependency dep = DependencyNotation.parse((String) l.getValue());
                if (dep == null || dep.getClassifier() != null || (dep.getType() != null && !"jar".equals(dep.getType()))) {
                    return this;
                }
                updated = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg ->
                        ChangeStringLiteral.withStringValue(l, DependencyNotation.toStringNotation(dep.withGav(dep.getGav().withVersion(null))))
                ));
            }
        } else if (m.getArguments().get(0) instanceof G.MapLiteral) {
            updated = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                G.MapLiteral mapLiteral = (G.MapLiteral) arg;
                return mapLiteral.withElements(ListUtils.map(mapLiteral.getElements(), entry -> {
                    if (entry.getKey() instanceof J.Literal && "version".equals(((J.Literal) entry.getKey()).getValue())) {
                        //noinspection DataFlowIssue
                        return null;
                    }
                    return entry;
                }));
            }));
        } else if (m.getArguments().get(0) instanceof G.MapEntry) {
            updated = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                G.MapEntry entry = (G.MapEntry) arg;
                if (entry.getKey() instanceof J.Literal && "version".equals(((J.Literal) entry.getKey()).getValue())) {
                    //noinspection DataFlowIssue
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

    /**
     * Updates the declared group ID of a dependency.
     * This method handles various dependency notation formats including string literals,
     * GStrings, map entries, map literals, assignments, and Kotlin string templates.
     * If this dependency uses platform() or enforcedPlatform(), this method automatically
     * operates on the inner dependency declaration.
     *
     * @param newGroupId The new group ID to set
     * @return A new GradleDependency with the updated group ID, or the original if no change was made
     */
    public GradleDependency withDeclaredGroupId(String newGroupId) {
        if (StringUtils.isBlank(newGroupId)) {
            return this;
        }

        J.MethodInvocation m = getTree();

        // Handle platform dependencies
        if (isPlatform() && m.getArguments().get(0) instanceof J.MethodInvocation) {
            GradleDependency platformDep = getPlatformDependency();
            if (platformDep != null) {
                GradleDependency updated = platformDep.withDeclaredGroupId(newGroupId);
                if (updated != platformDep) {
                    return new GradleDependency(new Cursor(cursor.getParent(),
                            m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> updated.getTree()))),
                            resolvedDependency);
                }
            }
            return this;
        }

        J.MethodInvocation updated = m;
        Expression firstArg = m.getArguments().get(0);

        if (firstArg instanceof J.Literal) {
            String gav = (String) ((J.Literal) firstArg).getValue();
            if (gav != null) {
                Dependency dep = DependencyNotation.parse(gav);
                if (dep != null && !newGroupId.equals(dep.getGroupId())) {
                    Dependency updatedDep = dep.withGav(dep.getGav().withGroupId(newGroupId));
                    updated = m.withArguments(ListUtils.mapFirst(m.getArguments(),
                            arg -> ChangeStringLiteral.withStringValue((J.Literal) arg, DependencyNotation.toStringNotation(updatedDep))));
                }
            }
        } else if (firstArg instanceof G.GString) {
            G.GString gstring = (G.GString) firstArg;
            List<J> strings = gstring.getStrings();
            if (strings.size() >= 2 && strings.get(0) instanceof J.Literal) {
                J.Literal literal = (J.Literal) strings.get(0);
                String originalValue = (String) literal.getValue();
                Dependency dep = DependencyNotation.parse(originalValue);
                if (dep != null && !newGroupId.equals(dep.getGroupId())) {
                    Dependency updatedDep = dep.withGav(dep.getGav().withGroupId(newGroupId));
                    String replacement = DependencyNotation.toStringNotation(updatedDep);
                    // Preserve trailing colon if present (for version interpolation)
                    if (originalValue.endsWith(":")) {
                        replacement = replacement + ":";
                    }
                    // Update only the literal part, preserve the GString structure with the rest of the elements
                    J.Literal newLiteral = literal.withValue(replacement)
                            .withValueSource(replacement);
                    G.GString updatedGString = gstring.withStrings(ListUtils.mapFirst(strings, s -> newLiteral));
                    updated = m.withArguments(singletonList(updatedGString));
                }
            }
        } else if (firstArg instanceof G.MapEntry || firstArg instanceof G.MapLiteral) {
            List<G.MapEntry> entries = firstArg instanceof G.MapLiteral ?
                    ((G.MapLiteral) firstArg).getElements() : m.getArguments().stream()
                    .filter(G.MapEntry.class::isInstance)
                    .map(G.MapEntry.class::cast)
                    .collect(toList());

            for (G.MapEntry entry : entries) {
                if (entry.getKey() instanceof J.Literal &&
                        "group".equals(((J.Literal) entry.getKey()).getValue()) &&
                        entry.getValue() instanceof J.Literal) {
                    String currentGroup = (String) ((J.Literal) entry.getValue()).getValue();
                    if (!newGroupId.equals(currentGroup)) {
                        G.MapEntry updatedEntry = entry.withValue(
                                ChangeStringLiteral.withStringValue((J.Literal) entry.getValue(), newGroupId));

                        if (firstArg instanceof G.MapLiteral) {
                            G.MapLiteral mapLiteral = (G.MapLiteral) firstArg;
                            updated = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg ->
                                    mapLiteral.withElements(ListUtils.map(mapLiteral.getElements(),
                                            e -> e == entry ? updatedEntry : e))));
                        } else {
                            updated = m.withArguments(ListUtils.map(m.getArguments(),
                                    arg -> arg == entry ? updatedEntry : arg));
                        }
                    }
                    break;
                }
            }
        } else if (firstArg instanceof J.Assignment) {
            List<Expression> updatedArgs = m.getArguments();
            for (Expression updatedArg : updatedArgs) {
                if (updatedArg instanceof J.Assignment) {
                    J.Assignment assignment = (J.Assignment) updatedArg;
                    if (assignment.getVariable() instanceof J.Identifier &&
                            "group".equals(((J.Identifier) assignment.getVariable()).getSimpleName()) &&
                            assignment.getAssignment() instanceof J.Literal) {
                        String currentGroup = (String) ((J.Literal) assignment.getAssignment()).getValue();
                        if (!newGroupId.equals(currentGroup)) {
                            J.Assignment updatedAssignment = assignment.withAssignment(
                                    ChangeStringLiteral.withStringValue((J.Literal) assignment.getAssignment(), newGroupId));
                            updated = m.withArguments(ListUtils.map(m.getArguments(),
                                    arg -> arg == assignment ? updatedAssignment : arg));
                        }
                        break;
                    }
                }
            }
        } else if (firstArg instanceof K.StringTemplate) {
            K.StringTemplate template = (K.StringTemplate) firstArg;
            List<J> strings = template.getStrings();
            if (!strings.isEmpty() && strings.get(0) instanceof J.Literal) {
                J.Literal literal = (J.Literal) strings.get(0);
                Dependency dep = DependencyNotation.parse((String) literal.getValue());
                if (dep != null && !newGroupId.equals(dep.getGroupId())) {
                    Dependency updatedDep = dep.withGav(dep.getGav().withGroupId(newGroupId));
                    String replacement = DependencyNotation.toStringNotation(updatedDep);
                    J.Literal newLiteral = literal.withValue(replacement)
                            .withValueSource(template.getDelimiter() + replacement + template.getDelimiter());
                    updated = m.withArguments(singletonList(newLiteral));
                }
            }
        }

        return updated == m ? this :
                new GradleDependency(new Cursor(cursor.getParent(), updated), resolvedDependency);
    }

    /**
     * Updates the declared artifact ID of a dependency.
     * This method handles various dependency notation formats including string literals,
     * GStrings, map entries, map literals, assignments, and Kotlin string templates.
     * If this dependency uses platform() or enforcedPlatform(), this method automatically
     * operates on the inner dependency declaration.
     *
     * @param newArtifactId The new artifact ID to set
     * @return A new GradleDependency with the updated artifact ID, or the original if no change was made
     */
    public GradleDependency withDeclaredArtifactId(String newArtifactId) {
        if (StringUtils.isBlank(newArtifactId)) {
            return this;
        }

        J.MethodInvocation m = getTree();

        // Handle platform dependencies
        if (isPlatform() && m.getArguments().get(0) instanceof J.MethodInvocation) {
            GradleDependency platformDep = getPlatformDependency();
            if (platformDep != null) {
                GradleDependency updated = platformDep.withDeclaredArtifactId(newArtifactId);
                if (updated != platformDep) {
                    return new GradleDependency(new Cursor(cursor.getParent(),
                            m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> updated.getTree()))),
                            resolvedDependency);
                }
            }
            return this;
        }

        J.MethodInvocation updated = m;
        Expression firstArg = m.getArguments().get(0);

        if (firstArg instanceof J.Literal) {
            String gav = (String) ((J.Literal) firstArg).getValue();
            if (gav != null) {
                Dependency dep = DependencyNotation.parse(gav);
                if (dep != null && !newArtifactId.equals(dep.getArtifactId())) {
                    Dependency updatedDep = dep.withGav(dep.getGav().withArtifactId(newArtifactId));
                    updated = m.withArguments(ListUtils.mapFirst(m.getArguments(),
                            arg -> ChangeStringLiteral.withStringValue((J.Literal) arg, DependencyNotation.toStringNotation(updatedDep))));
                }
            }
        } else if (firstArg instanceof G.GString) {
            G.GString gstring = (G.GString) firstArg;
            List<J> strings = gstring.getStrings();
            if (strings.size() >= 2 && strings.get(0) instanceof J.Literal) {
                J.Literal literal = (J.Literal) strings.get(0);
                String originalValue = (String) literal.getValue();
                Dependency dep = DependencyNotation.parse(originalValue);
                if (dep != null && !newArtifactId.equals(dep.getArtifactId())) {
                    Dependency updatedDep = dep.withGav(dep.getGav().withArtifactId(newArtifactId));
                    String replacement = DependencyNotation.toStringNotation(updatedDep);
                    // Preserve trailing colon if present (for version interpolation)
                    if (originalValue.endsWith(":")) {
                        replacement = replacement + ":";
                    }
                    // Update only the literal part, preserve the GString structure with the rest of the elements
                    J.Literal newLiteral = literal.withValue(replacement)
                            .withValueSource(replacement);
                    G.GString updatedGString = gstring.withStrings(ListUtils.mapFirst(strings, s -> newLiteral));
                    updated = m.withArguments(singletonList(updatedGString));
                }
            }
        } else if (firstArg instanceof G.MapEntry || firstArg instanceof G.MapLiteral) {
            List<G.MapEntry> entries = firstArg instanceof G.MapLiteral ?
                    ((G.MapLiteral) firstArg).getElements() : m.getArguments().stream()
                    .filter(G.MapEntry.class::isInstance)
                    .map(G.MapEntry.class::cast)
                    .collect(toList());

            for (G.MapEntry entry : entries) {
                if (entry.getKey() instanceof J.Literal &&
                        "name".equals(((J.Literal) entry.getKey()).getValue()) &&
                        entry.getValue() instanceof J.Literal) {
                    String currentArtifact = (String) ((J.Literal) entry.getValue()).getValue();
                    if (!newArtifactId.equals(currentArtifact)) {
                        G.MapEntry updatedEntry = entry.withValue(
                                ChangeStringLiteral.withStringValue((J.Literal) entry.getValue(), newArtifactId));

                        if (firstArg instanceof G.MapLiteral) {
                            G.MapLiteral mapLiteral = (G.MapLiteral) firstArg;
                            updated = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg ->
                                    mapLiteral.withElements(ListUtils.map(mapLiteral.getElements(),
                                            e -> e == entry ? updatedEntry : e))));
                        } else {
                            updated = m.withArguments(ListUtils.map(m.getArguments(),
                                    arg -> arg == entry ? updatedEntry : arg));
                        }
                    }
                    break;
                }
            }
        } else if (firstArg instanceof J.Assignment) {
            List<Expression> updatedArgs = m.getArguments();
            for (Expression updatedArg : updatedArgs) {
                if (updatedArg instanceof J.Assignment) {
                    J.Assignment assignment = (J.Assignment) updatedArg;
                    if (assignment.getVariable() instanceof J.Identifier &&
                            "name".equals(((J.Identifier) assignment.getVariable()).getSimpleName()) &&
                            assignment.getAssignment() instanceof J.Literal) {
                        String currentArtifact = (String) ((J.Literal) assignment.getAssignment()).getValue();
                        if (!newArtifactId.equals(currentArtifact)) {
                            J.Assignment updatedAssignment = assignment.withAssignment(
                                    ChangeStringLiteral.withStringValue((J.Literal) assignment.getAssignment(), newArtifactId));
                            updated = m.withArguments(ListUtils.map(m.getArguments(),
                                    arg -> arg == assignment ? updatedAssignment : arg));
                        }
                        break;
                    }
                }
            }
        } else if (firstArg instanceof K.StringTemplate) {
            K.StringTemplate template = (K.StringTemplate) firstArg;
            List<J> strings = template.getStrings();
            if (!strings.isEmpty() && strings.get(0) instanceof J.Literal) {
                J.Literal literal = (J.Literal) strings.get(0);
                Dependency dep = DependencyNotation.parse((String) literal.getValue());
                if (dep != null && !newArtifactId.equals(dep.getArtifactId())) {
                    Dependency updatedDep = dep.withGav(dep.getGav().withArtifactId(newArtifactId));
                    String replacement = DependencyNotation.toStringNotation(updatedDep);
                    J.Literal newLiteral = literal.withValue(replacement)
                            .withValueSource(template.getDelimiter() + replacement + template.getDelimiter());
                    updated = m.withArguments(singletonList(newLiteral));
                }
            }
        }

        return updated == m ? this :
                new GradleDependency(new Cursor(cursor.getParent(), updated), resolvedDependency);
    }

    /**
     * Updates the declared version of a dependency.
     * This method handles various dependency notation formats including string literals,
     * GStrings, map entries, map literals, assignments, and Kotlin string templates.
     * If this dependency uses platform() or enforcedPlatform(), this method automatically
     * operates on the inner dependency declaration.
     *
     * @param newVersion The new version to set (can be null to remove version)
     * @return A new GradleDependency with the updated version, or the original if no change was made
     */
    public GradleDependency withDeclaredVersion(@Nullable String newVersion) {
        if (newVersion == null || newVersion.isEmpty()) {
            return removeVersion();
        }

        J.MethodInvocation m = getTree();

        // Handle platform dependencies
        if (isPlatform() && m.getArguments().get(0) instanceof J.MethodInvocation) {
            GradleDependency platformDep = getPlatformDependency();
            if (platformDep != null) {
                GradleDependency updated = platformDep.withDeclaredVersion(newVersion);
                if (updated != platformDep) {
                    return new GradleDependency(new Cursor(cursor.getParent(),
                            m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> updated.getTree()))),
                            resolvedDependency);
                }
            }
            return this;
        }

        J.MethodInvocation updated = m;
        Expression firstArg = m.getArguments().get(0);

        if (firstArg instanceof J.Literal) {
            String gav = (String) ((J.Literal) firstArg).getValue();
            if (gav != null) {
                Dependency dep = DependencyNotation.parse(gav);
                if (dep != null && !newVersion.equals(dep.getVersion())) {
                    Dependency updatedDep = dep.withGav(dep.getGav().withVersion(newVersion));
                    updated = m.withArguments(ListUtils.mapFirst(m.getArguments(),
                            arg -> ChangeStringLiteral.withStringValue((J.Literal) arg, DependencyNotation.toStringNotation(updatedDep))));
                }
            }
        } else if (firstArg instanceof G.GString) {
            // For GString, we convert to a simple string literal with the new version
            G.GString gstring = (G.GString) firstArg;
            List<J> strings = gstring.getStrings();
            if (strings.size() >= 2 && strings.get(0) instanceof J.Literal) {
                J.Literal literal = (J.Literal) strings.get(0);
                Dependency dep = DependencyNotation.parse((String) literal.getValue());
                if (dep != null) {
                    Dependency updatedDep = dep.withGav(dep.getGav().withVersion(newVersion));
                    String replacement = DependencyNotation.toStringNotation(updatedDep);
                    J.Literal newLiteral = literal.withValue(replacement)
                            .withValueSource(gstring.getDelimiter() + replacement + gstring.getDelimiter());
                    updated = m.withArguments(singletonList(newLiteral));
                }
            }
        } else if (firstArg instanceof G.MapEntry || firstArg instanceof G.MapLiteral) {
            List<G.MapEntry> entries = firstArg instanceof G.MapLiteral ?
                    new ArrayList<>(((G.MapLiteral) firstArg).getElements()) :
                    m.getArguments().stream()
                            .filter(G.MapEntry.class::isInstance)
                            .map(G.MapEntry.class::cast)
                            .collect(toList());

            boolean versionFound = false;
            for (G.MapEntry entry : entries) {
                if (entry.getKey() instanceof J.Literal &&
                        "version".equals(((J.Literal) entry.getKey()).getValue()) &&
                        entry.getValue() instanceof J.Literal) {
                    String currentVersion = (String) ((J.Literal) entry.getValue()).getValue();
                    if (!newVersion.equals(currentVersion)) {
                        G.MapEntry updatedEntry = entry.withValue(
                                ChangeStringLiteral.withStringValue((J.Literal) entry.getValue(), newVersion));

                        if (firstArg instanceof G.MapLiteral) {
                            G.MapLiteral mapLiteral = (G.MapLiteral) firstArg;
                            updated = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg ->
                                    mapLiteral.withElements(ListUtils.map(mapLiteral.getElements(),
                                            e -> e == entry ? updatedEntry : e))));
                        } else {
                            updated = m.withArguments(ListUtils.map(m.getArguments(),
                                    arg -> arg == entry ? updatedEntry : arg));
                        }
                    }
                    versionFound = true;
                    break;
                }
            }

            if (!versionFound && firstArg instanceof G.MapLiteral) {
                // TODO Add version entry to map literal
            }
        } else if (firstArg instanceof J.Assignment) {
            List<Expression> updatedArgs = m.getArguments();
            for (Expression updatedArg : updatedArgs) {
                if (updatedArg instanceof J.Assignment) {
                    J.Assignment assignment = (J.Assignment) updatedArg;
                    if (assignment.getVariable() instanceof J.Identifier &&
                            "version".equals(((J.Identifier) assignment.getVariable()).getSimpleName()) &&
                            assignment.getAssignment() instanceof J.Literal) {
                        String currentVersion = (String) ((J.Literal) assignment.getAssignment()).getValue();
                        if (!newVersion.equals(currentVersion)) {
                            J.Assignment updatedAssignment = assignment.withAssignment(
                                    ChangeStringLiteral.withStringValue((J.Literal) assignment.getAssignment(), newVersion));
                            updated = m.withArguments(ListUtils.map(m.getArguments(),
                                    arg -> arg == assignment ? updatedAssignment : arg));
                        }
                        break;
                    }
                }
            }
            // TODO handle adding version when none exsits
        } else if (firstArg instanceof K.StringTemplate) {
            // For StringTemplate, we convert to a simple string literal with the new version
            K.StringTemplate template = (K.StringTemplate) firstArg;
            List<J> strings = template.getStrings();
            if (!strings.isEmpty() && strings.get(0) instanceof J.Literal) {
                J.Literal literal = (J.Literal) strings.get(0);
                Dependency dep = DependencyNotation.parse((String) literal.getValue());
                if (dep != null) {
                    Dependency updatedDep = dep.withGav(dep.getGav().withVersion(newVersion));
                    String replacement = DependencyNotation.toStringNotation(updatedDep);
                    J.Literal newLiteral = literal.withValue(replacement)
                            .withValueSource(template.getDelimiter() + replacement + template.getDelimiter());
                    updated = m.withArguments(singletonList(newLiteral));
                }
            }
        }

        return updated == m ? this : new GradleDependency(new Cursor(cursor.getParent(), updated), resolvedDependency);
    }

    public static class Matcher extends GradleTraitMatcher<GradleDependency> {

        @Nullable
        protected String configuration;

        @Nullable
        protected DependencyMatcher matcher;

        public Matcher() {}

        public Matcher configuration(@Nullable String configuration) {
            this.configuration = configuration;
            return this;
        }

        public Matcher groupId(@Nullable String groupPattern) {
            if (matcher == null) {
                matcher = new DependencyMatcher(groupPattern, null, null);
            } else {
                matcher = matcher.withGroupPattern(groupPattern);
            }
            return this;
        }

        public Matcher artifactId(@Nullable String artifactPattern) {
            if (matcher == null) {
                matcher = new DependencyMatcher(null, artifactPattern, null);
            } else {
                matcher = matcher.withArtifactPattern(artifactPattern);
            }
            return this;
        }

        public Matcher matcher(@Nullable DependencyMatcher matcher) {
            this.matcher = matcher;
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
            if(!isDependencyDeclaration(cursor)) {
                return null;
            }
            J.MethodInvocation methodInvocation = cursor.getValue();
            GradleProject gradleProject = getGradleProject(cursor);
            GradleDependencyConfiguration gdc = getConfiguration(gradleProject, methodInvocation);

            if (!StringUtils.isBlank(configuration) && !methodInvocation.getSimpleName().equals(configuration)) {
                return null;
            }

            Dependency dependency = null;
            Expression argument = methodInvocation.getArguments().get(0);
            if (argument instanceof J.Literal || argument instanceof G.GString || argument instanceof G.MapEntry || argument instanceof G.MapLiteral || argument instanceof J.Assignment || argument instanceof K.StringTemplate) {
                dependency = parseDependency(methodInvocation.getArguments());
            } else if (argument instanceof J.Binary && ((J.Binary) argument).getLeft() instanceof J.Literal) {
                dependency = parseDependency(singletonList(((J.Binary) argument).getLeft()));
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
                        if (matcher == null || matcher.matches(resolvedDependency.getGroupId(), resolvedDependency.getArtifactId())) {
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
                                if (matcher == null || matcher.matches(resolvedDependency.getGroupId(), resolvedDependency.getArtifactId())) {
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

            if (matcher == null || matcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
                // Couldn't find the actual resolved dependency, return a synthetic one instead
                ResolvedDependency resolvedDependency = ResolvedDependency.builder()
                        .depth(-1)
                        .gav(new ResolvedGroupArtifactVersion(null, dependency.getGroupId() == null ? "" : dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion() != null ? dependency.getVersion() : "", null))
                        .classifier(dependency.getClassifier())
                        .type(dependency.getType())
                        .requested(Dependency.builder()
                                .scope(methodInvocation.getSimpleName())
                                .type(dependency.getType())
                                .gav(dependency.getGav())
                                .classifier(dependency.getClassifier())
                                .build())
                        .build();
                return new GradleDependency(cursor, withRequested(resolvedDependency, dependency));
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

        private @Nullable Dependency parseDependency(List<Expression> arguments) {
            Expression argument = arguments.get(0);
            if (argument instanceof J.Literal) {
                return DependencyNotation.parse((String) ((J.Literal) argument).getValue());
            } else if (argument instanceof G.GString) {
                G.GString gstring = (G.GString) argument;
                List<J> strings = gstring.getStrings();
                if (strings.size() >= 2 && strings.get(0) instanceof J.Literal && ((J.Literal) strings.get(0)).getValue() != null) {
                    return DependencyNotation.parse((String) ((J.Literal) strings.get(0)).getValue());
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

                return Dependency.builder()
                        .gav(new GroupArtifactVersion(group, artifact, version))
                        .build();
            } else if (argument instanceof K.StringTemplate) {
                K.StringTemplate template = (K.StringTemplate) argument;
                List<J> strings = template.getStrings();
                if (!strings.isEmpty() && strings.get(0) instanceof J.Literal && ((J.Literal) strings.get(0)).getValue() != null) {
                    return DependencyNotation.parse((String) ((J.Literal) strings.get(0)).getValue());
                }
            }

            return null;
        }

        private static @Nullable Dependency getMapEntriesDependency(List<Expression> arguments) {
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

            return org.openrewrite.maven.tree.Dependency.builder()
                    .gav(new GroupArtifactVersion(group, artifact, version))
                    .build();
        }
    }
}
