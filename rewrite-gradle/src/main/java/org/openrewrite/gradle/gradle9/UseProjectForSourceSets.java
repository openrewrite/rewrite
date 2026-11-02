/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.gradle.gradle9;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** ProjectDependency no longer delegates Project properties in Gradle 9. */
public class UseProjectForSourceSets extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use an explicit Project for sourceSets access";
    }

    @Override
    public String getDescription() {
        return "Replace Groovy dependency-handler `project(':path').sourceSets` with " +
               "`rootProject.project(':path').sourceSets`. Gradle 9 removed ProjectDependency's delegation to Project. " +
               "Only literal absolute paths in an unshadowed canonical dependencies " +
               "configuration are migrated; ordinary project dependencies are retained.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess field, ExecutionContext ctx) {
                J.FieldAccess f = super.visitFieldAccess(field, ctx);
                if (!"sourceSets".equals(f.getSimpleName()) || !(f.getTarget() instanceof J.MethodInvocation)) {
                    return f;
                }
                Cursor parent = getCursor().getParentTreeCursor();
                if (parent.getValue() instanceof J.Assignment &&
                    ((J.Assignment) parent.getValue()).getVariable() == field ||
                    parent.getValue() instanceof J.AssignmentOperation &&
                    ((J.AssignmentOperation) parent.getValue()).getVariable() == field) {
                    return f;
                }
                J.MethodInvocation project = (J.MethodInvocation) f.getTarget();
                if (!"project".equals(project.getSimpleName()) || project.getArguments().size() != 1 ||
                    !(project.getArguments().get(0) instanceof J.Literal)) {
                    return f;
                }
                Object path = ((J.Literal) project.getArguments().get(0)).getValue();
                if (!(path instanceof String) || !((String) path).startsWith(":")) {
                    return f;
                }
                JavaType.Method method = project.getMethodType();
                // Attribution is incomplete for dynamic DSLs and may leak from an outer
                // dependencies block into a custom closure. Require the canonical context even
                // for attributed unqualified calls, and never rewrite an explicit receiver.
                if (project.getSelect() != null || method != null && !gradleMethod(project) ||
                    !canonicalContext() || hasShadows()) {
                    return f;
                }
                JavaType.FullyQualified projectType = JavaType.ShallowClass.build("org.gradle.api.Project");
                J.Identifier root = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                        java.util.Collections.emptyList(), "rootProject", projectType, null);
                return f.withTarget(project.withSelect(root).withMethodType(method == null ? null :
                        method.withDeclaringType(projectType).withReturnType(projectType)));
            }

            private boolean gradleMethod(J.MethodInvocation method) {
                JavaType.Method type = method.getMethodType();
                return type == null || type.getDeclaringType().getFullyQualifiedName().startsWith("org.gradle.") ||
                       "RewriteGradleProject".equals(type.getDeclaringType().getFullyQualifiedName()) ||
                       "DependencyHandlerSpec".equals(type.getDeclaringType().getFullyQualifiedName());
            }

            private boolean canonicalContext() {
                int closures = 0;
                Cursor c = getCursor();
                while (c != null) {
                    Object value = c.getValue();
                    if (value instanceof J.Lambda) {
                        J.Lambda lambda = (J.Lambda) value;
                        if (!lambda.getParameters().getParameters().isEmpty() &&
                            !(lambda.getParameters().getParameters().size() == 1 &&
                              lambda.getParameters().getParameters().get(0) instanceof J.Empty)) {
                            return false;
                        }
                        Cursor owner = c.getParentTreeCursor();
                        if (!(owner.getValue() instanceof J.MethodInvocation)) {
                            return false;
                        }
                        J.MethodInvocation call = owner.getValue();
                        String name = call.getSimpleName();
                        if (call.getSelect() != null || !gradleMethod(call) ||
                            (closures == 0 ? !"dependencies".equals(name) :
                                    !("subprojects".equals(name) || "allprojects".equals(name) || filteredSubprojects(call))) ||
                            (filteredSubprojects(call) ? call.getArguments().size() != 2 : call.getArguments().size() != 1)) {
                            return false;
                        }
                        closures++;
                    } else if (value instanceof J.MethodDeclaration || value instanceof J.ClassDeclaration) {
                        return false;
                    }
                    c = c.getParent();
                }
                return closures > 0;
            }

            private boolean filteredSubprojects(J.MethodInvocation call) {
                if (!"configure".equals(call.getSimpleName()) || call.getArguments().size() != 2 ||
                    !(call.getArguments().get(0) instanceof J.MethodInvocation)) {
                    return false;
                }
                J.MethodInvocation filter = (J.MethodInvocation) call.getArguments().get(0);
                return "findAll".equals(filter.getSimpleName()) && filter.getArguments().size() == 1 &&
                       filter.getArguments().get(0) instanceof J.Lambda &&
                       filter.getSelect() instanceof J.Identifier &&
                       "subprojects".equals(((J.Identifier) filter.getSelect()).getSimpleName());
            }

            private boolean hasShadows() {
                Set<String> names = new HashSet<>(Arrays.asList("project", "rootProject", "dependencies",
                        "subprojects", "allprojects", "configure", "getProject", "getRootProject",
                        "getDependencies", "getSubprojects", "getAllprojects"));
                Cursor c = getCursor();
                while (c != null) {
                    Object scope = c.getValue();
                    if (scope instanceof J.Block || scope instanceof G.CompilationUnit || scope instanceof J.Lambda) {
                        AtomicBoolean found = new AtomicBoolean();
                        // Inspect declarations in this lexical scope only. A sibling closure's `project`
                        // parameter (common in subprojects.each) cannot shadow this dependencies block.
                        new JavaIsoVisitor<AtomicBoolean>() {
                            @Override
                            public J preVisit(J tree, AtomicBoolean shadow) {
                                if (tree != scope && (tree instanceof J.Block || tree instanceof J.Lambda ||
                                        tree instanceof J.ClassDeclaration)) {
                                    stopAfterPreVisit();
                                }
                                return tree;
                            }

                            @Override
                            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean shadow) {
                                if ("ext".equals(method.getSimpleName()) && method.getSelect() == null) {
                                    // Extra properties declared by an ext block outlive that closure.
                                    new JavaIsoVisitor<AtomicBoolean>() {
                                        @Override
                                        public J.Assignment visitAssignment(J.Assignment assignment, AtomicBoolean found) {
                                            if (assignment.getVariable() instanceof J.Identifier &&
                                                names.contains(((J.Identifier) assignment.getVariable()).getSimpleName())) {
                                                found.set(true);
                                            }
                                            return super.visitAssignment(assignment, found);
                                        }
                                    }.visit(method, shadow);
                                }
                                return super.visitMethodInvocation(method, shadow);
                            }

                            @Override
                            public J.VariableDeclarations.NamedVariable visitVariable(
                                    J.VariableDeclarations.NamedVariable variable, AtomicBoolean shadow) {
                                if (names.contains(variable.getSimpleName())) {
                                    shadow.set(true);
                                }
                                return super.visitVariable(variable, shadow);
                            }

                            @Override
                            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, AtomicBoolean shadow) {
                                if (names.contains(method.getSimpleName())) {
                                    shadow.set(true);
                                }
                                // A different method's parameters and locals are a separate scope.
                                return method;
                            }

                            @Override
                            public J.Assignment visitAssignment(J.Assignment assignment, AtomicBoolean shadow) {
                                if (assignment.getVariable() instanceof J.Identifier &&
                                    names.contains(((J.Identifier) assignment.getVariable()).getSimpleName())) {
                                    shadow.set(true);
                                } else if (assignment.getVariable() instanceof J.FieldAccess) {
                                    J.FieldAccess access = (J.FieldAccess) assignment.getVariable();
                                    if (names.contains(access.getSimpleName()) && access.getTarget() instanceof J.Identifier &&
                                        Arrays.asList("ext", "this").contains(((J.Identifier) access.getTarget()).getSimpleName())) {
                                        shadow.set(true);
                                    }
                                }
                                return super.visitAssignment(assignment, shadow);
                            }
                        }.visit((Tree) scope, found);
                        if (found.get()) {
                            return true;
                        }
                    }
                    c = c.getParent();
                }
                return false;
            }
        });
    }
}
