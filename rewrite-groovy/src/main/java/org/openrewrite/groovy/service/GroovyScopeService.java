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
package org.openrewrite.groovy.service;

import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.groovy.marker.LambdaStyle;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.service.ScopeService;
import org.openrewrite.java.trait.Scope;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Groovy's binding rules where they part from Java's. A closure that names no parameters and has no arrow binds {@code it}. A variable
 * a script declares is local to the script's body, so it is visible after its declaration and not inside the
 * script's methods and classes.
 */
@Incubating(since = "8.111.0")
public class GroovyScopeService extends ScopeService {
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList("as", "def", "in", "trait"));

    @Override
    public List<Scope.Binding> bindings(Cursor scope) {
        List<Scope.Binding> bindings = super.bindings(scope);
        Object tree = scope.getValue();
        if (tree instanceof J.Lambda && declaresNoParameter((J.Lambda) tree)) {
            bindings = new ArrayList<>(bindings);
            bindings.add(new Scope.Binding(scope, "it", Scope.Namespace.VARIABLE, scope, null));
        }
        return bindings;
    }

    @Override
    public boolean isVisibleAt(Scope.Binding binding, Cursor position) {
        J scopeTree = binding.getScope().getTree();
        if (scopeTree instanceof G.CompilationUnit && binding.getNamespace() == Scope.Namespace.VARIABLE &&
            binding.getTree() instanceof J.Identifier) {
            List<Statement> statements = ((G.CompilationUnit) scopeTree).getStatements();
            int declared = indexUnder(binding.getCursor(), statements);
            int asked = indexUnder(position, statements);
            return declared >= 0 && asked > declared &&
                   !(statements.get(asked) instanceof J.MethodDeclaration || statements.get(asked) instanceof J.ClassDeclaration);
        }
        return super.isVisibleAt(binding, position);
    }

    @Override
    public boolean isReserved(String name) {
        return KEYWORDS.contains(name) || super.isReserved(name);
    }

    private static boolean declaresNoParameter(J.Lambda lambda) {
        // An arrow without parameters, as in { -> ... }, declares that the closure takes none
        if (lambda.getMarkers().findFirst(LambdaStyle.class).filter(LambdaStyle::isArrow).isPresent()) {
            return false;
        }
        for (J parameter : lambda.getParameters().getParameters()) {
            if (!(parameter instanceof J.Empty)) {
                return false;
            }
        }
        return true;
    }

    private static int indexUnder(Cursor cursor, List<? extends J> trees) {
        for (Cursor c = cursor; c != null; c = c.getParent()) {
            if (c.getValue() instanceof J) {
                for (int i = 0; i < trees.size(); i++) {
                    if (trees.get(i).getId().equals(((J) c.getValue()).getId())) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }
}
