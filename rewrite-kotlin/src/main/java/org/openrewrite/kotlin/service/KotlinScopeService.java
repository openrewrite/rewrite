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
package org.openrewrite.kotlin.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.service.ScopeService;
import org.openrewrite.java.trait.Scope;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.marker.PrimaryConstructor;
import org.openrewrite.kotlin.tree.K;

import java.util.*;

/**
 * Kotlin's binding rules where they part from Java's. A {@code when} subject is visible only in its branches.
 * A lambda without parameters binds {@code it}. A {@code val} or {@code var} primary constructor parameter is a
 * property of its class.
 */
@Incubating(since = "8.113.0")
public class KotlinScopeService extends ScopeService {
    private static final Set<String> HARD_KEYWORDS = new HashSet<>(Arrays.asList(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is",
            "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof",
            "val", "var", "when", "while"));

    @Override
    public boolean isScope(Cursor cursor) {
        return cursor.getValue() instanceof K.When || super.isScope(cursor);
    }

    @Override
    public List<Scope.Binding> bindings(Cursor scope) {
        List<Scope.Binding> bindings = new ArrayList<>(super.bindings(scope));
        Object tree = scope.getValue();
        if (tree instanceof J.Lambda && declaresNoParameter((J.Lambda) tree)) {
            bindings.add(new Scope.Binding(scope, "it", Scope.Namespace.VARIABLE, scope, null));
        } else if (tree instanceof J.ClassDeclaration) {
            properties(scope, (J.ClassDeclaration) tree, bindings);
        } else if (tree instanceof J.MethodDeclaration &&
                   ((J.MethodDeclaration) tree).getMarkers().findFirst(PrimaryConstructor.class).isPresent()) {
            // The class binds a property parameter, where the whole body sees it
            bindings.removeIf(binding -> {
                Object declarations = binding.getCursor().getParentTreeCursor().getParentTreeCursor().getValue();
                return declarations instanceof J.VariableDeclarations && isProperty((J.VariableDeclarations) declarations) &&
                       ((J.MethodDeclaration) tree).getParameters().contains(declarations);
            });
        }
        return bindings;
    }

    @Override
    public boolean isVisibleAt(Scope.Binding binding, Cursor position) {
        J scopeTree = binding.getScope().getTree();
        if (scopeTree instanceof K.When) {
            J branches = ((K.When) scopeTree).getBranches();
            for (Cursor c = position; c != null; c = c.getParent()) {
                if (c.getValue() instanceof J && ((J) c.getValue()).getId().equals(branches.getId())) {
                    return true;
                }
            }
            return false;
        }
        return super.isVisibleAt(binding, position);
    }

    @Override
    public Scope.@Nullable Namespace namespaceOf(Cursor identifier) {
        Cursor parent = identifier.getParentTreeCursor();
        if (parent.getValue() instanceof J.Assignment &&
            ((J.Assignment) parent.getValue()).getVariable() == identifier.getValue()) {
            Object call = parent.getParentTreeCursor().getValue();
            if (call instanceof J.MethodInvocation || call instanceof J.NewClass) {
                return null;
            }
        }
        return super.namespaceOf(identifier);
    }

    @Override
    public boolean localMayShadowLocal() {
        return true;
    }

    @Override
    public boolean isReserved(String name) {
        return HARD_KEYWORDS.contains(name);
    }

    private static boolean declaresNoParameter(J.Lambda lambda) {
        for (J parameter : lambda.getParameters().getParameters()) {
            if (!(parameter instanceof J.Empty)) {
                return false;
            }
        }
        return true;
    }

    private static void properties(Cursor scope, J.ClassDeclaration classDeclaration, List<Scope.Binding> bindings) {
        Cursor body = new Cursor(scope, classDeclaration.getBody());
        for (Statement statement : classDeclaration.getBody().getStatements()) {
            if (!(statement instanceof J.MethodDeclaration) ||
                !statement.getMarkers().findFirst(PrimaryConstructor.class).isPresent()) {
                continue;
            }
            Cursor constructor = new Cursor(body, statement);
            for (Statement parameter : ((J.MethodDeclaration) statement).getParameters()) {
                if (!(parameter instanceof J.VariableDeclarations) || !isProperty((J.VariableDeclarations) parameter)) {
                    continue;
                }
                Cursor declarations = new Cursor(constructor, parameter);
                for (J.VariableDeclarations.NamedVariable variable : ((J.VariableDeclarations) parameter).getVariables()) {
                    Cursor name = new Cursor(new Cursor(declarations, variable), variable.getName());
                    bindings.add(new Scope.Binding(name, variable.getSimpleName(), Scope.Namespace.VARIABLE, scope,
                            variable.getName().getFieldType()));
                }
            }
        }
    }

    private static boolean isProperty(J.VariableDeclarations parameter) {
        for (J.Modifier modifier : parameter.getModifiers()) {
            if (modifier.getType() == J.Modifier.Type.Final || "var".equals(modifier.getKeyword())) {
                return true;
            }
        }
        return false;
    }
}
