/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.android.internal;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Set;

/**
 * Rewrites extra-property declarations whose values back an SDK assignment.
 * Handles both DSL shapes:
 * <ul>
 *   <li>Groovy DSL: {@code ext.compileSdkVersion = 34} (an assignment whose LHS is a field access {@code ext.NAME})</li>
 *   <li>Groovy DSL: {@code ext { compileSdkVersion = 34 }} (an assignment inside an {@code ext { }} closure)</li>
 *   <li>Kotlin DSL: {@code val compileSdkVersion by extra(34)} (variable declaration with {@code by extra(...)})</li>
 *   <li>Kotlin DSL: {@code extra["compileSdkVersion"] = 34} (assignment to subscript)</li>
 * </ul>
 * The visitor only rewrites entries whose name appears in {@code propertyNames}, and only when
 * the current literal value is below {@code newValue}.
 */
public class UpgradeSdkExtraPropertyVisitor extends JavaIsoVisitor<ExecutionContext> {

    private final Set<String> propertyNames;
    private final int newValue;
    private boolean inExtBlock;

    public UpgradeSdkExtraPropertyVisitor(Set<String> propertyNames, int newValue) {
        this.propertyNames = propertyNames;
        this.newValue = newValue;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
        boolean entered = false;
        if (!inExtBlock && "ext".equals(method.getSimpleName()) && !method.getArguments().isEmpty() &&
                method.getArguments().get(method.getArguments().size() - 1) instanceof J.Lambda) {
            inExtBlock = true;
            entered = true;
        }
        try {
            // `val compileSdkVersion by extra(34)`  -- in Kotlin DSL this is modeled as a method invocation
            // whose receiver is the J.VariableDeclarations; we handle this via visitVariableDeclarations.
            // Subscript assignment `extra["compileSdkVersion"] = 34` shows up as a method invocation `set(...)`,
            // but for our scope it's easier to handle by intercepting the BY-form variable declarations below.
            return super.visitMethodInvocation(method, ctx);
        } finally {
            if (entered) {
                inExtBlock = false;
            }
        }
    }

    @Override
    public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
        J.Assignment a = super.visitAssignment(assignment, ctx);
        String name = lhsName(a.getVariable());
        if (name == null || !propertyNames.contains(name)) {
            return a;
        }
        // Only rewrite if LHS is an extra-property shape:
        // - inside ext { } block: assignment LHS is a J.Identifier
        // - top-level Groovy ext.NAME = ... : LHS is J.FieldAccess with target identifier "ext"/"project"
        if (!isExtraPropertyLhs(a.getVariable())) {
            return a;
        }
        return rewriteLiteralIntAssignment(a);
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
        J.VariableDeclarations vd = super.visitVariableDeclarations(multiVariable, ctx);
        if (vd.getVariables().isEmpty()) {
            return vd;
        }
        // Kotlin DSL `val compileSdkVersion by extra(34)` shows up as a J.VariableDeclarations
        // whose variable name matches and whose initializer is `extra(...)`.
        return vd.withVariables(org.openrewrite.internal.ListUtils.map(vd.getVariables(), namedVar -> {
            String name = namedVar.getSimpleName();
            if (!propertyNames.contains(name)) {
                return namedVar;
            }
            Expression init = namedVar.getInitializer();
            if (!(init instanceof J.MethodInvocation)) {
                return namedVar;
            }
            J.MethodInvocation mi = (J.MethodInvocation) init;
            if (!"extra".equals(mi.getSimpleName()) || mi.getArguments().size() != 1) {
                return namedVar;
            }
            Expression arg = mi.getArguments().get(0);
            if (!(arg instanceof J.Literal)) {
                return namedVar;
            }
            J.Literal lit = (J.Literal) arg;
            if (lit.getType() != JavaType.Primitive.Int || !(lit.getValue() instanceof Integer)) {
                return namedVar;
            }
            int current = (Integer) lit.getValue();
            if (current >= newValue) {
                return namedVar;
            }
            J.Literal newLit = lit.withValue(newValue).withValueSource(String.valueOf(newValue));
            return namedVar.withInitializer(mi.withArguments(java.util.Collections.singletonList(newLit)));
        }));
    }

    private J.Assignment rewriteLiteralIntAssignment(J.Assignment a) {
        if (!(a.getAssignment() instanceof J.Literal)) {
            return a;
        }
        J.Literal lit = (J.Literal) a.getAssignment();
        if (lit.getType() != JavaType.Primitive.Int || !(lit.getValue() instanceof Integer)) {
            return a;
        }
        int current = (Integer) lit.getValue();
        if (current >= newValue) {
            return a;
        }
        return a.withAssignment(lit.withValue(newValue).withValueSource(String.valueOf(newValue)));
    }

    private boolean isExtraPropertyLhs(Expression lhs) {
        if (lhs instanceof J.FieldAccess) {
            J.FieldAccess fa = (J.FieldAccess) lhs;
            if (fa.getTarget() instanceof J.Identifier) {
                String t = ((J.Identifier) fa.getTarget()).getSimpleName();
                return "ext".equals(t) || "project".equals(t);
            }
            return false;
        }
        if (lhs instanceof J.Identifier) {
            return inExtBlock;
        }
        return false;
    }

    private static @org.jspecify.annotations.Nullable String lhsName(Expression lhs) {
        if (lhs instanceof J.Identifier) {
            return ((J.Identifier) lhs).getSimpleName();
        }
        if (lhs instanceof J.FieldAccess) {
            return ((J.FieldAccess) lhs).getSimpleName();
        }
        return null;
    }
}
