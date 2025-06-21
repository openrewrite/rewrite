/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.trait;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A trait that provides scope-aware variable analysis capabilities, including
 * variable usage tracking, shadowing detection, and scope depth management.
 * This trait is particularly useful for transformations that need to understand
 * variable visibility and usage across different lexical scopes.
 */
@Incubating(since = "8.57.0")
@Value
public class VariableScope implements Trait<J.Block> {
    Cursor cursor;

    /**
     * Check if a variable is used after a given statement in the containing block,
     * accounting for variable shadowing in nested scopes (lambdas, anonymous classes, etc.).
     *
     * @param varName   The name of the variable to check
     * @param statement The statement after which to check for variable usage
     * @return true if the variable is used after the statement, false otherwise
     */
    public boolean isVariableUsedAfter(String varName, Statement statement) {
        J.Block containingBlock = getTree();
        List<Statement> statements = containingBlock.getStatements();
        int statementIndex = statements.indexOf(statement);

        if (statementIndex == -1 || statementIndex == statements.size() - 1) {
            return false;
        }

        AtomicBoolean found = new AtomicBoolean(false);
        for (int i = statementIndex + 1; i < statements.size(); i++) {
            new ScopeAwareVariableUsageVisitor(varName, found).visit(statements.get(i), 0);
            if (found.get()) {
                break;
            }
        }
        return found.get();
    }

    /**
     * Get all variable declarations that shadow the given variable name at different scope depths.
     *
     * @param varName The variable name to check for shadowing
     * @return A map of scope depth to the set of variable declarations that shadow the variable
     */
    public Map<Integer, Set<J.Identifier>> getShadowingDeclarations(String varName) {
        Map<Integer, Set<J.Identifier>> shadowMap = new HashMap<>();
        new ShadowingDetectionVisitor(varName, shadowMap).visit(getTree(), 0);
        return shadowMap;
    }

    /**
     * Check if a variable name is shadowed at any scope depth within this block.
     *
     * @param varName The variable name to check
     * @return true if the variable is shadowed anywhere in this block
     */
    public boolean isVariableShadowed(String varName) {
        return !getShadowingDeclarations(varName).isEmpty();
    }

    /**
     * Get the maximum scope depth reached within this block.
     * Scope depth increases for lambdas and anonymous classes only.
     *
     * @return The maximum scope depth
     */
    public int getMaxScopeDepth() {
        AtomicInteger maxDepth = new AtomicInteger(0);

        new JavaIsoVisitor<Integer>() {
            private int currentDepth = 0;

            @Override
            public J.Lambda visitLambda(J.Lambda lambda, Integer p) {
                currentDepth++;
                maxDepth.set(Math.max(maxDepth.get(), currentDepth));
                J.Lambda result = super.visitLambda(lambda, p);
                currentDepth--;
                return result;
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, Integer p) {
                if (newClass.getBody() != null) {
                    currentDepth++;
                    maxDepth.set(Math.max(maxDepth.get(), currentDepth));
                }
                J.NewClass result = super.visitNewClass(newClass, p);
                if (newClass.getBody() != null) {
                    currentDepth--;
                }
                return result;
            }
        }.visit(getTree(), 0);

        return maxDepth.get();
    }

    /**
     * A visitor that tracks variable usage while being aware of variable shadowing
     * across different lexical scope depths.
     */
    @RequiredArgsConstructor
    private static class ScopeAwareVariableUsageVisitor extends JavaIsoVisitor<Integer> {
        private final String targetVarName;
        private final AtomicBoolean found;

        // Track lexical scopes where the variable is shadowed
        private int lexicalScopeDepth = 0;
        private final Set<Integer> shadowedAtDepth = new HashSet<>();

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
            // Only count it if it's not shadowed in current lexical scope
            if (identifier.getSimpleName().equals(targetVarName) && !shadowedAtDepth.contains(lexicalScopeDepth)) {
                found.set(true);
            }
            return super.visitIdentifier(identifier, p);
        }

        @Override
        public J.Lambda visitLambda(J.Lambda lambda, Integer p) {
            // Lambda creates a new lexical scope
            lexicalScopeDepth++;

            // Check lambda parameters for shadowing
            for (J parameter : lambda.getParameters().getParameters()) {
                if (parameter instanceof J.VariableDeclarations) {
                    J.VariableDeclarations varDecls = (J.VariableDeclarations) parameter;
                    for (J.VariableDeclarations.NamedVariable var : varDecls.getVariables()) {
                        if (var.getSimpleName().equals(targetVarName)) {
                            shadowedAtDepth.add(lexicalScopeDepth);
                        }
                    }
                } else if (parameter instanceof J.Identifier) {
                    // Single parameter lambda like x -> { ... }
                    J.Identifier paramId = (J.Identifier) parameter;
                    if (paramId.getSimpleName().equals(targetVarName)) {
                        shadowedAtDepth.add(lexicalScopeDepth);
                    }
                }
            }

            J.Lambda result = super.visitLambda(lambda, p);

            // Exit lexical scope
            shadowedAtDepth.remove(lexicalScopeDepth);
            lexicalScopeDepth--;
            return result;
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, Integer p) {
            // Anonymous class creates a new lexical scope
            if (newClass.getBody() != null) {
                lexicalScopeDepth++;
            }
            J.NewClass result = super.visitNewClass(newClass, p);
            if (newClass.getBody() != null) {
                shadowedAtDepth.remove(lexicalScopeDepth);
                lexicalScopeDepth--;
            }
            return result;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
            // Variable declaration in current lexical scope shadows outer variables
            if (variable.getSimpleName().equals(targetVarName)) {
                shadowedAtDepth.add(lexicalScopeDepth);
            }
            return super.visitVariable(variable, p);
        }
    }

    /**
     * A visitor that detects variable shadowing at different lexical scope depths.
     */
    @RequiredArgsConstructor
    private static class ShadowingDetectionVisitor extends JavaIsoVisitor<Integer> {
        private final String targetVarName;
        private final Map<Integer, Set<J.Identifier>> shadowMap;
        private int lexicalScopeDepth = 0;

        @Override
        public J.Lambda visitLambda(J.Lambda lambda, Integer p) {
            lexicalScopeDepth++;

            // Check lambda parameters for shadowing - parameters are at the new lexical scope depth
            for (J parameter : lambda.getParameters().getParameters()) {
                if (parameter instanceof J.VariableDeclarations) {
                    J.VariableDeclarations varDecls = (J.VariableDeclarations) parameter;
                    for (J.VariableDeclarations.NamedVariable var : varDecls.getVariables()) {
                        if (var.getSimpleName().equals(targetVarName)) {
                            shadowMap.computeIfAbsent(lexicalScopeDepth, k -> new HashSet<>()).add(var.getName());
                        }
                    }
                } else if (parameter instanceof J.Identifier) {
                    // Single parameter lambda like x -> { ... }
                    J.Identifier paramId = (J.Identifier) parameter;
                    if (paramId.getSimpleName().equals(targetVarName)) {
                        // Create a synthetic NamedVariable for single parameter lambdas
                        shadowMap.computeIfAbsent(lexicalScopeDepth, k -> new HashSet<>()).add(paramId);
                    }
                }
            }

            J.Lambda result = super.visitLambda(lambda, p);
            lexicalScopeDepth--;
            return result;
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, Integer p) {
            if (newClass.getBody() != null) {
                lexicalScopeDepth++;
            }
            J.NewClass result = super.visitNewClass(newClass, p);
            if (newClass.getBody() != null) {
                lexicalScopeDepth--;
            }
            return result;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
            if (variable.getSimpleName().equals(targetVarName)) {
                // Check for variables in special visibility scopes (try-with-resources, catch, for loops)
                // that shadow variables in the same lexical scope
                if (isInSpecialVisibilityScope()) {
                    shadowMap.computeIfAbsent(1, k -> new HashSet<>()).add(variable.getName());
                }
                // Any variable with the same name in a nested lexical scope shadows the outer variable
                else if (lexicalScopeDepth > 0) {
                    shadowMap.computeIfAbsent(lexicalScopeDepth, k -> new HashSet<>()).add(variable.getName());
                }
            }
            return super.visitVariable(variable, p);
        }

        @Override
        public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, Integer p) {
            // Handle instanceof pattern variables (Java 14+)
            if (instanceOf.getPattern() instanceof J.Identifier) {
                J.Identifier patternVar = (J.Identifier) instanceOf.getPattern();
                if (patternVar.getSimpleName().equals(targetVarName) && lexicalScopeDepth > 0) {
                    shadowMap.computeIfAbsent(lexicalScopeDepth, k -> new HashSet<>()).add(patternVar);
                }
            }
            // Also check if the pattern is wrapped in a VariableDeclarations
            else if (instanceOf.getPattern() instanceof J.VariableDeclarations) {
                J.VariableDeclarations varDecls = (J.VariableDeclarations) instanceOf.getPattern();
                for (J.VariableDeclarations.NamedVariable var : varDecls.getVariables()) {
                    if (var.getSimpleName().equals(targetVarName) && lexicalScopeDepth > 0) {
                        shadowMap.computeIfAbsent(lexicalScopeDepth, k -> new HashSet<>()).add(var.getName());
                    }
                }
            }
            return super.visitInstanceOf(instanceOf, p);
        }

        private boolean isInSpecialVisibilityScope() {
            // Check if this variable is declared in a try-with-resources, catch block, or for loop
            Cursor cursor = getCursor();
            while (cursor != null) {
                Object value = cursor.getValue();
                if (value instanceof J.Try.Resource ||
                        value instanceof J.Try.Catch ||
                        value instanceof J.ForLoop.Control ||
                        value instanceof J.ForEachLoop.Control) {
                    return true;
                }
                // Stop at block boundaries to avoid going too far up
                if (value instanceof J.Block) {
                    break;
                }
                cursor = cursor.getParent();
            }
            return false;
        }


    }

    public static class Matcher extends SimpleTraitMatcher<VariableScope> {

        @Override
        protected @Nullable VariableScope test(Cursor cursor) {
            // Match J.Block elements that represent scope boundaries
            if (cursor.getValue() instanceof J.Block) {
                return new VariableScope(cursor);
            }
            return null;
        }
    }
}