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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.GroovyTemplate;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/** Uses the merging API; assigning a replacement map is intentionally not equivalent. */
public class UseManifestAttributes extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use the Gradle Manifest attributes merging API";
    }

    @Override
    public String getDescription() {
        return "Replace Groovy Gradle Manifest `attributes += map` with `attributes(map)`. " +
               "Gradle 9 makes the attributes property read-only. Only discarded assignments with Manifest " +
               "attribution or an unshadowed canonical jar/manifest configuration are migrated.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile && !(tree instanceof G.CompilationUnit)) {
                    return (J) tree;
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J visitAssignmentOperation(J.AssignmentOperation operation, ExecutionContext ctx) {
                J.AssignmentOperation a = (J.AssignmentOperation) super.visitAssignmentOperation(operation, ctx);
                if (a.getOperator() != J.AssignmentOperation.Type.Addition ||
                    !(a.getAssignment() instanceof G.MapLiteral ||
                      TypeUtils.isAssignableTo("java.util.Map", a.getAssignment().getType()))) {
                    return a;
                }
                Expression variable = a.getVariable();
                J.Identifier name = variable instanceof J.Identifier ? (J.Identifier) variable :
                        variable instanceof J.FieldAccess ? ((J.FieldAccess) variable).getName() : null;
                if (name == null || !"attributes".equals(name.getSimpleName())) {
                    return a;
                }
                JavaType.Variable field = name.getFieldType();
                boolean attributed = variable instanceof J.FieldAccess &&
                        TypeUtils.isAssignableTo("org.gradle.api.java.archives.Manifest", ((J.FieldAccess) variable).getTarget().getType()) ||
                        field != null && TypeUtils.isAssignableTo("org.gradle.api.java.archives.Manifest", field.getOwner());
                boolean canonical = canonicalContext() && !hasShadows();
                // Known non-Manifest attribution is never overridden by the sparse-model fallback.
                if (field != null && !attributed ||
                    variable instanceof J.Identifier && !canonical ||
                    !attributed && !(variable instanceof J.Identifier && canonical)) {
                    return a;
                }
                Cursor parent = getCursor().getParentTreeCursor();
                if (parent.getValue() instanceof J.Return) {
                    J.Return ret = parent.getValue();
                    if (!canonical || !(ret.getMarkers().findFirst(org.openrewrite.java.marker.ImplicitReturn.class).isPresent() ||
                            ret.getMarkers().findFirst(org.openrewrite.groovy.marker.ImplicitReturn.class).isPresent())) {
                        return a;
                    }
                    parent = parent.getParentTreeCursor();
                }
                if (!(parent.getValue() instanceof J.Block || parent.getValue() instanceof G.CompilationUnit)) {
                    return a;
                }
                J.MethodInvocation call = GroovyTemplate.builder("attributes([:])").build()
                        .apply(getCursor(), a.getCoordinates().replace());
                if (variable instanceof J.FieldAccess) {
                    J.FieldAccess access = (J.FieldAccess) variable;
                    call = call.getPadding().withSelect(JRightPadded.build(access.getTarget())
                            .withAfter(access.getPadding().getName().getBefore()))
                            .withName(access.getName().withFieldType(null));
                }
                // Keep operator-side comments in evaluation order, inside the argument list.
                Space before = a.getPadding().getOperator().getBefore();
                Space valuePrefix = a.getAssignment().getPrefix();
                Space prefix = before.getComments().isEmpty() ? valuePrefix :
                        before.withComments(org.openrewrite.internal.ListUtils.concatAll(before.getComments(), valuePrefix.getComments()));
                if (prefix.getComments().isEmpty()) {
                    prefix = Space.EMPTY;
                }
                return call.withArguments(Collections.singletonList(a.getAssignment().withPrefix(prefix)))
                        .withPrefix(a.getPrefix());
            }

            private boolean canonicalContext() {
                Cursor c = getCursor().getParentTreeCursor();
                String[] expected = {"manifest", "jar"};
                int depth = 0;
                while (c.getValue() instanceof J) {
                    Object v = c.getValue();
                    if (v instanceof J.Lambda) {
                        J.Lambda lambda = (J.Lambda) v;
                        if (!lambda.getParameters().getParameters().isEmpty() &&
                            !(lambda.getParameters().getParameters().size() == 1 &&
                              lambda.getParameters().getParameters().get(0) instanceof J.Empty)) {
                            return false;
                        }
                        Cursor owner = c.getParentTreeCursor();
                        if (!(owner.getValue() instanceof J.MethodInvocation)) {
                            return false;
                        }
                        J.MethodInvocation m = owner.getValue();
                        String wanted = depth < 2 ? expected[depth] : null;
                        // Unlike jar/manifest, project wrappers must have real Gradle attribution.
                        boolean projectConfiguration = depth >= 2 && "project".equals(m.getSimpleName()) &&
                                m.getArguments().size() == 2 && m.getArguments().get(0) instanceof J.Literal &&
                                ((J.Literal) m.getArguments().get(0)).getValue() instanceof String &&
                                m.getMethodType() != null &&
                                TypeUtils.isAssignableTo("org.gradle.api.Project", m.getMethodType().getDeclaringType());
                        if (m.getSelect() != null ||
                            (!projectConfiguration && (m.getArguments().size() != 1 ||
                                (wanted == null ? !("allprojects".equals(m.getSimpleName()) || "subprojects".equals(m.getSimpleName())) :
                                        !wanted.equals(m.getSimpleName()))))) {
                            return false;
                        }
                        if (m.getMethodType() != null &&
                            !m.getMethodType().getDeclaringType().getFullyQualifiedName().startsWith("org.gradle.") &&
                            !"RewriteGradleProject".equals(m.getMethodType().getDeclaringType().getFullyQualifiedName())) {
                            return false;
                        }
                        // Configuration closure results are ignored only when the call itself is a statement.
                        Cursor statement = owner.getParentTreeCursor();
                        if (statement.getValue() instanceof J.Return) {
                            J.Return ret = statement.getValue();
                            if (!(ret.getMarkers().findFirst(org.openrewrite.java.marker.ImplicitReturn.class).isPresent() ||
                                  ret.getMarkers().findFirst(org.openrewrite.groovy.marker.ImplicitReturn.class).isPresent())) {
                                return false;
                            }
                            statement = statement.getParentTreeCursor();
                        }
                        if (!(statement.getValue() instanceof J.Block || statement.getValue() instanceof G.CompilationUnit)) {
                            return false;
                        }
                        depth++;
                        c = owner;
                    } else if (v instanceof J.MethodDeclaration || v instanceof J.ClassDeclaration) {
                        return false;
                    }
                    if (c.getParent() == null) {
                        break;
                    }
                    c = c.getParentTreeCursor();
                }
                return depth >= 2;
            }

            // Deliberately conservative for sparse models: a declaration of a DSL name anywhere in
            // the script disables the unqualified fallback rather than guessing its dynamic resolution.
            private boolean hasShadows() {
                AtomicBoolean shadow = new AtomicBoolean();
                new JavaIsoVisitor<AtomicBoolean>() {
                    private void check(String name, AtomicBoolean found) {
                        if ("attributes".equals(name) || "manifest".equals(name) || "jar".equals(name) ||
                            "allprojects".equals(name) || "subprojects".equals(name)) {
                            found.set(true);
                        }
                    }
                    @Override
                    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable v, AtomicBoolean found) {
                        check(v.getSimpleName(), found);
                        return super.visitVariable(v, found);
                    }
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration m, AtomicBoolean found) {
                        check(m.getSimpleName(), found);
                        if ("project".equals(m.getSimpleName())) {
                            found.set(true);
                        }
                        return super.visitMethodDeclaration(m, found);
                    }
                }.visit(getCursor().firstEnclosingOrThrow(G.CompilationUnit.class), shadow);
                return shadow.get();
            }
        });
    }
}
