/*
 * Copyright 2021 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.DeleteStatement;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.References;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.time.Duration;
import java.util.*;

@Value
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("ConstantConditions")
public class RemoveUnusedLocalVariables extends Recipe {
    @Incubating(since = "7.17.2")
    @Option(displayName = "Ignore matching variable names",
            description = "An array of variable identifier names for local variables to ignore, even if the local variable is unused.",
            required = false,
            example = "[unused, notUsed, IGNORE_ME]")
    @Nullable
    String[] ignoreVariablesNamed;

    @Override
    public String getDisplayName() {
        return "Remove unused local variables";
    }

    @Override
    public String getDescription() {
        return "If a local variable is declared but not used, it is dead code and should be removed.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1481");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveUnusedLocalVariablesVisitor(ignoreVariablesNamed);
    }

    private static class RemoveUnusedLocalVariablesVisitor extends JavaIsoVisitor<ExecutionContext> {
        private Set<String> ignoreVariableNames;

        RemoveUnusedLocalVariablesVisitor(String[] ignoreVariablesNamed) {
            if (ignoreVariablesNamed != null) {
                ignoreVariableNames = new HashSet<>(ignoreVariablesNamed.length);
                ignoreVariableNames.addAll(Arrays.asList(ignoreVariablesNamed));
            }
        }

        private Cursor getCursorToParentScope(Cursor cursor) {
            return cursor.dropParentUntil(is ->
                    is instanceof J.Block ||
                            is instanceof J.MethodDeclaration ||
                            is instanceof J.ForLoop ||
                            is instanceof J.ForEachLoop ||
                            is instanceof J.ForLoop.Control ||
                            is instanceof J.ForEachLoop.Control ||
                            is instanceof J.Case ||
                            is instanceof J.Try ||
                            is instanceof J.Try.Resource ||
                            is instanceof J.Try.Catch ||
                            is instanceof J.MultiCatch ||
                            is instanceof J.Lambda
            );
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
            // skip matching ignored variable names right away
            if (ignoreVariableNames != null && ignoreVariableNames.contains(variable.getSimpleName())) {
                return variable;
            }

            Cursor parentScope = getCursorToParentScope(getCursor());
            J parent = parentScope.getValue();
            if (parentScope.getParent() == null ||
                    // skip class instance variables
                    parentScope.getParent().getValue() instanceof J.ClassDeclaration ||
                    // skip anonymous class instance variables
                    parentScope.getParent().getValue() instanceof J.NewClass ||
                    // skip if method declaration parameter
                    parent instanceof J.MethodDeclaration ||
                    // skip if defined in an enhanced or standard for loop, since there isn't much we can do about the semantics at that point
                    parent instanceof J.ForLoop.Control || parent instanceof J.ForEachLoop.Control ||
                    // skip if defined in a try's catch clause as an Exception variable declaration
                    parent instanceof J.Try.Resource || parent instanceof J.Try.Catch || parent instanceof J.MultiCatch ||
                    // skip if defined as a parameter to a lambda expression
                    parent instanceof J.Lambda
            ) {
                return variable;
            }

            List<J> readReferences = References.findRhsReferences(parentScope.getValue(), variable.getName());
            if (readReferences.isEmpty()) {
                List<Statement> assignmentReferences = References.findLhsReferences(parentScope.getValue(), variable.getName());
                for (Statement ref : assignmentReferences) {
                    doAfterVisit(new DeleteStatement<>(ref));
                }
                return null;
            }

            return super.visitVariable(variable, ctx);
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            if (!multiVariable.getAllAnnotations().isEmpty()) {
                return multiVariable;
            }

            J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);
            if (mv.getVariables().isEmpty()) {
                doAfterVisit(new DeleteStatement<>(mv));
            }
            return mv;
        }

    }

}
