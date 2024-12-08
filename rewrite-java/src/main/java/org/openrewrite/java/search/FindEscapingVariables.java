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
package org.openrewrite.java.search;

import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Identify local variables that escape there defining scope.
 *
 * @see FindVariableEscapeLocations
 */
public class FindEscapingVariables extends JavaIsoVisitor<ExecutionContext> {
    /**
     * Finds named local variables from the given subtree that will escape their defining scope
     */
    public static Set<J.VariableDeclarations.NamedVariable> find(J subtree) {
        return TreeVisitor.collect(new FindEscapingVariables(), subtree, new HashSet<>())
                .stream()
                .filter(J.VariableDeclarations.NamedVariable.class::isInstance)
                .map(J.VariableDeclarations.NamedVariable.class::cast)
                .collect(Collectors.toSet());
    }

    /**
     * Determine if a local named variable from the given subtree will escape its defining scope
     */
    public static boolean willLeakFrom(J.VariableDeclarations.NamedVariable variable, J subtree) {
        return find(subtree).contains(variable);
    }

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext executionContext) {
        variable = super.visitVariable(variable, executionContext);

        if (variable.isField(getCursor())) {
            return variable;
        }

        J.Block definingScope = getCursor().firstEnclosing(J.Block.class);
        if (definingScope != null && variable.getName().getFieldType() != null) {
            boolean willLeak = FindVariableEscapeLocations.findLeakingVars(definingScope).stream()
                    .map(J.Identifier::getFieldType)
                    .anyMatch(variable.getName().getFieldType()::equals);
            if (willLeak) {
                variable = SearchResult.found(variable);
            }
        }

        return variable;
    }
}
