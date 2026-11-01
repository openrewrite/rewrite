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
package org.openrewrite.gradle.gradle8;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateArchiveClassifier extends Recipe {
    @Override
    public String getDisplayName() {
        return "Migrate archive task classifier";
    }

    @Override
    public String getDescription() {
        return "Replace legacy classifier assignments and setter calls with archiveClassifier in proven Groovy archive task closures. " +
               "Recognize typed task declarations and same-file task configuration calls, including nested task calls in omitted-parentheses " +
               "artifact syntax. Preserve MavenArtifact classifier properties. Dynamic task types and cross-file task declarations are not inferred.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                Set<String> taskNames = new HashSet<>();
                Set<UUID> typedTasks = new HashSet<>();
                Set<String> lexicalNames = new HashSet<>();
                new GroovyIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations v, ExecutionContext c) {
                        v.getVariables().forEach(n -> lexicalNames.add(n.getSimpleName()));
                        return super.visitVariableDeclarations(v, c);
                    }

                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration m, ExecutionContext c) {
                        lexicalNames.add(m.getSimpleName());
                        return super.visitMethodDeclaration(m, c);
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation m, ExecutionContext c) {
                        // Only project-level declarations establish names for subsequent dynamic calls.
                        if (getCursor().firstEnclosing(J.Lambda.class) == null &&
                            getCursor().firstEnclosing(J.ClassDeclaration.class) == null &&
                            getCursor().firstEnclosing(J.MethodDeclaration.class) == null) {
                            if ("task".equals(m.getSimpleName()) && m.getSelect() == null &&
                                m.getArguments().size() == 1 && m.getArguments().get(0) instanceof J.MethodInvocation) {
                                J.MethodInvocation declaration = (J.MethodInvocation) m.getArguments().get(0);
                                if (declaration.getSelect() == null && hasArchiveType(declaration, cu)) {
                                    taskNames.add(declaration.getSimpleName());
                                    typedTasks.add(declaration.getId());
                                }
                            } else if (isTasksSelect(m) &&
                                       ("register".equals(m.getSimpleName()) || "create".equals(m.getSimpleName())) &&
                                       m.getArguments().size() >= 2 && m.getArguments().get(0) instanceof J.Literal &&
                                       ((J.Literal) m.getArguments().get(0)).getValue() instanceof String &&
                                       isArchiveType(m.getArguments().get(1), cu)) {
                                taskNames.add((String) ((J.Literal) m.getArguments().get(0)).getValue());
                                typedTasks.add(m.getId());
                            }
                        }
                        return super.visitMethodInvocation(m, c);
                    }
                }.visit(cu, ctx);
                taskNames.removeAll(lexicalNames);
                if (lexicalNames.contains("tasks") || lexicalNames.contains("task")) {
                    // A local task/container symbol makes the DSL declarations ambiguous.
                    taskNames.clear();
                    typedTasks.clear();
                }
                return (G.CompilationUnit) new JavaVisitor<ExecutionContext>() {
                    private boolean archiveDelegate() {
                        // Stop at the nearest closure: nested DSLs may have entirely different delegates.
                        J.Lambda lambda = getCursor().firstEnclosing(J.Lambda.class);
                        if (lexicalNames.contains("classifier") || lambda == null ||
                            getCursor().firstEnclosing(J.MethodDeclaration.class) != null ||
                            getCursor().firstEnclosing(J.ClassDeclaration.class) != null ||
                            lambda.getParameters().getParameters().stream().anyMatch(p -> !(p instanceof J.Empty))) {
                            return false;
                        }
                        Cursor lambdaCursor = getCursor().dropParentUntil(v -> v == lambda);
                        Object parent = lambdaCursor.getParentTreeCursor().getValue();
                        if (!(parent instanceof J.MethodInvocation)) {
                            return false;
                        }
                        J.MethodInvocation owner = (J.MethodInvocation) parent;
                        boolean projectLevel = lambdaCursor.getParentTreeCursor().firstEnclosing(J.Lambda.class) == null;
                        boolean proven = typedTasks.contains(owner.getId()) ||
                                projectLevel && !lexicalNames.contains("tasks") && isTasksSelect(owner) &&
                                "withType".equals(owner.getSimpleName()) && !owner.getArguments().isEmpty() &&
                                isArchiveType(owner.getArguments().get(0), cu);
                        if (!proven && projectLevel && !lexicalNames.contains("tasks") && isTasksSelect(owner) &&
                            "named".equals(owner.getSimpleName()) && owner.getArguments().size() == 2 &&
                            owner.getArguments().get(0) instanceof J.Literal) {
                            proven = taskNames.contains(((J.Literal) owner.getArguments().get(0)).getValue());
                        }
                        if (!proven && owner.getArguments().size() == 1 &&
                            owner.getArguments().get(0) instanceof J.Lambda &&
                            (owner.getSelect() == null || !lexicalNames.contains("tasks") && isTasksSelect(owner)) &&
                            taskNames.contains(owner.getSimpleName())) {
                            // An ordinary artifact(task) { ... } owns its closure itself; it never enters here.
                            // In artifact task { ... }, the AST instead nests task(closure) as artifact's argument.
                            Object enclosing = lambdaCursor.getParentTreeCursor().getParentTreeCursor().getValue();
                            proven = projectLevel ||
                                    enclosing instanceof J.MethodInvocation &&
                                    "artifact".equals(((J.MethodInvocation) enclosing).getSimpleName());
                        }
                        return proven;
                    }

                    @Override
                    public J visitAssignment(J.Assignment assignment, ExecutionContext c) {
                        J.Assignment a = (J.Assignment) super.visitAssignment(assignment, c);
                        if (a.getVariable() instanceof J.Identifier &&
                            "classifier".equals(((J.Identifier) a.getVariable()).getSimpleName()) &&
                            (((J.Identifier) a.getVariable()).getFieldType() == null ||
                             TypeUtils.isAssignableTo("org.gradle.api.tasks.bundling.AbstractArchiveTask",
                                     ((J.Identifier) a.getVariable()).getFieldType().getOwner())) &&
                            archiveDelegate()) {
                            return a.withVariable(((J.Identifier) a.getVariable()).withSimpleName("archiveClassifier"));
                        }
                        return a;
                    }

                    private boolean setterStatement() {
                        Object parent = getCursor().getParentTreeCursor().getValue();
                        if (parent instanceof J.Block) {
                            return true;
                        }
                        return parent instanceof J.Return &&
                               (((J.Return) parent).getMarkers().findFirst(org.openrewrite.java.marker.ImplicitReturn.class).isPresent() ||
                                ((J.Return) parent).getMarkers().findFirst(org.openrewrite.groovy.marker.ImplicitReturn.class).isPresent());
                    }

                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext c) {
                        J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, c);
                        if (m.getSelect() != null || !("classifier".equals(m.getSimpleName()) || "setClassifier".equals(m.getSimpleName())) ||
                            m.getArguments().size() != 1 || m.getArguments().get(0) instanceof J.Lambda ||
                            m.getArguments().get(0) instanceof J.Empty || lexicalNames.contains(m.getSimpleName()) ||
                            m.getMethodType() != null &&
                            !TypeUtils.isAssignableTo("org.gradle.api.tasks.bundling.AbstractArchiveTask",
                                    m.getMethodType().getDeclaringType()) ||
                            !setterStatement() || !archiveDelegate()) {
                            return m;
                        }
                        Expression argument = m.getArguments().get(0);
                        Expression rhs = argument.withPrefix(argument.getPrefix().withWhitespace(" "));
                        return new J.Assignment(Tree.randomId(), m.getPrefix(), Markers.EMPTY,
                                m.getName().withSimpleName("archiveClassifier").withPrefix(Space.EMPTY),
                                new JLeftPadded<>(Space.SINGLE_SPACE, rhs, Markers.EMPTY), rhs.getType());
                    }
                }.visit(cu, ctx);
            }
        });
    }

    private static boolean isTasksSelect(J.MethodInvocation m) {
        return m.getSelect() instanceof J.Identifier && "tasks".equals(((J.Identifier) m.getSelect()).getSimpleName());
    }

    private static boolean hasArchiveType(J.MethodInvocation declaration, G.CompilationUnit cu) {
        for (Expression argument : declaration.getArguments()) {
            if (argument instanceof G.MapEntry) {
                G.MapEntry entry = (G.MapEntry) argument;
                if (entry.getKey() instanceof J.Literal && "type".equals(((J.Literal) entry.getKey()).getValue())) {
                    return isArchiveType(entry.getValue(), cu);
                }
            }
        }
        return false;
    }

    private static String typeName(Expression expression) {
        if (expression instanceof J.Identifier) {
            return ((J.Identifier) expression).getSimpleName();
        }
        if (expression instanceof J.FieldAccess) {
            J.FieldAccess field = (J.FieldAccess) expression;
            String target = typeName(field.getTarget());
            return target.isEmpty() ? "" : target + "." + field.getSimpleName();
        }
        return "";
    }

    private static boolean isArchiveType(Expression expression, G.CompilationUnit cu) {
        if (TypeUtils.isAssignableTo("org.gradle.api.tasks.bundling.AbstractArchiveTask", expression.getType())) {
            return true;
        }
        String name = typeName(expression);
        String simple = name.substring(name.lastIndexOf('.') + 1);
        if (!("Jar".equals(simple) || "Zip".equals(simple) || "Tar".equals(simple) || "AbstractArchiveTask".equals(simple))) {
            return false;
        }
        String qualified = "org.gradle.api.tasks.bundling." + simple;
        if (qualified.equals(name)) {
            return true;
        }
        if (!simple.equals(name)) {
            return false;
        }
        for (J.Import anImport : cu.getImports()) {
            String imported = anImport.getQualid().printTrimmed();
            if (anImport.getAlias() != null && simple.equals(anImport.getAlias().getSimpleName()) ||
                (imported.endsWith("." + simple) || imported.endsWith(".*")) &&
                !qualified.equals(imported) && !"org.gradle.api.tasks.bundling.*".equals(imported) &&
                !"org.apache.tools.ant.filters.*".equals(imported)) {
                return false;
            }
        }
        boolean[] shadowed = {false};
        new GroovyIsoVisitor<boolean[]>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations v, boolean[] found) {
                if (v.getVariables().stream().anyMatch(n -> simple.equals(n.getSimpleName()))) {
                    found[0] = true;
                }
                return super.visitVariableDeclarations(v, found);
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration cd, boolean[] found) {
                if (simple.equals(cd.getSimpleName())) {
                    found[0] = true;
                }
                return super.visitClassDeclaration(cd, found);
            }
        }.visit(cu, shadowed);
        return !shadowed[0];
    }
}
