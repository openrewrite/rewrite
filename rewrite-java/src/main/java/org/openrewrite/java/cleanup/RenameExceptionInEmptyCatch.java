/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RenameVariable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Statement;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class RenameExceptionInEmptyCatch extends Recipe {

    @Override
    public String getDisplayName() {
        return "Rename caught exceptions in empty catch blocks to `ignored`";
    }

    @Override
    public String getDescription() {
        return "Renames caught exceptions in empty catch blocks to `ignored`. `ignored` will be incremented by 1 if a namespace conflict exists.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                    Map<Cursor, Set<String>> variableScopes = new LinkedHashMap<>();
                    getCursor().putMessage("VARIABLES_KEY", variableScopes);
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                Map<Cursor, Set<String>> variableScope = getCursor().getNearestMessage("VARIABLES_KEY");
                if (variableScope != null) {
                    // Collect class fields first since the name space is available anywhere in the class.
                    for (Statement o : classDecl.getBody().getStatements()) {
                        if (o instanceof J.VariableDeclarations) {
                            J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) o;
                            for (J.VariableDeclarations.NamedVariable v : variableDeclarations.getVariables()) {
                                variableScope.computeIfAbsent(getCursor(), k -> new HashSet<>()).add(v.getSimpleName());
                            }
                        }
                    }
                }
                return super.visitClassDeclaration(classDecl, ctx);
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                if (validIdentifier()) {
                    Cursor parentScope = getCursorToParentScope();
                    if (!(parentScope.getValue() instanceof J.ClassDeclaration)) {
                        Map<Cursor, Set<String>> variableScope = getCursor().getNearestMessage("VARIABLES_KEY");
                        if (variableScope != null) {
                            Set<String> namesInScope = variableScope.computeIfAbsent(parentScope, k -> new HashSet<>());
                            namesInScope.add(identifier.getSimpleName());
                        }
                    }
                }
                return super.visitIdentifier(identifier, ctx);
            }

            @Override
            public J.Try.Catch visitCatch(J.Try.Catch aCatch, ExecutionContext ctx) {
                // Only visit empty catch blocks.
                if (aCatch.getBody().getStatements().isEmpty() && aCatch.getBody().getEnd().getComments().isEmpty()) {
                    return super.visitCatch(aCatch, ctx);
                }
                return aCatch;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                Cursor parentScope = getCursorToParentScope();
                if (parentScope.getValue() instanceof J.Try.Catch) {
                    Map<Cursor, Set<String>> variableScopes = getCursor().getNearestMessage("VARIABLES_KEY");
                    if (variableScopes != null) {
                        Set<String> namesInScope = variableScopes.computeIfAbsent(parentScope, k -> new HashSet<>());
                        namesInScope.addAll(
                                multiVariable.getVariables().stream()
                                        .map(J.VariableDeclarations.NamedVariable::getSimpleName)
                                        .collect(Collectors.toList()));

                        // Update the names in each scope. There should be no name shadowing produced because the
                        // exception variable is scoped to the catch and the catch block is empty.
                        aggregateNameScopes(variableScopes);

                        String baseName = "ignored";
                        int count = 0;
                        for (J.VariableDeclarations.NamedVariable variable : multiVariable.getVariables()) {
                            if (variable.getSimpleName().contains(baseName)) {
                                continue;
                            }

                            String newName = baseName;
                            // Generate a new name to prevent namespace shadowing.
                            while (variableScopes.containsKey(parentScope) && variableScopes.get(parentScope).contains(newName)) {
                                newName = baseName + (count += 1);
                            }
                            // Rename the variable.
                            doAfterVisit(new RenameVariable<>(variable, newName));
                            namesInScope.add(newName);
                        }
                    }
                }
                return super.visitVariableDeclarations(multiVariable, ctx);
            }

            // Aggregates the names from parent scopes that are visible in child scopes.
            private void aggregateNameScopes(Map<Cursor, Set<String>> variableScopes) {
                List<Cursor> parentScopes = new ArrayList<>(variableScopes.keySet());
                for (Cursor parentScope : parentScopes) {
                    for (Cursor maybeInScope : parentScopes) {
                        if (parentScope.isScopeInPath(maybeInScope.getValue())) {
                            variableScopes.get(parentScope).addAll(variableScopes.get(maybeInScope));
                        }
                    }
                }
            }

            // Filter out class names and method names.
            private boolean validIdentifier() {
                Cursor parent = getCursor().getParent();
                return parent != null &&
                       !(parent.getValue() instanceof J.ClassDeclaration) &&
                       !(parent.getValue() instanceof J.MethodDeclaration) &&
                       !(parent.getValue() instanceof J.MethodInvocation);
            }

            // Sets structure for cursor positions to aggregate namespaces.
            private Cursor getCursorToParentScope() {
                return getCursor().dropParentUntil(is ->
                        is instanceof J.CompilationUnit ||
                        is instanceof J.ClassDeclaration ||
                        is instanceof J.Block ||
                        is instanceof J.MethodDeclaration ||
                        is instanceof J.ForLoop ||
                        is instanceof J.Case ||
                        is instanceof J.Try ||
                        is instanceof J.Try.Catch ||
                        is instanceof J.MultiCatch ||
                        is instanceof J.Lambda
                );
            }
        };
    }
}
