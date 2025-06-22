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

import lombok.AccessLevel;
import lombok.Getter;
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
     * Represents variable usage information with lazy evaluation for performance.
     * Allows querying for reads, writes, or any usage without computing all data upfront.
     */
    @RequiredArgsConstructor
    public static class VariableUsage {
        @Getter
        private final String varName;

        @Getter
        private final List<Statement> statementsToSearch;
        
        @Getter
        private final VariableScope scope;
        
        // Lazy evaluation with caching
        private @Nullable Set<J.Identifier> cachedReads;
        private @Nullable Set<J.Identifier> cachedWrites;
        private @Nullable Boolean cachedHasReads;
        private @Nullable Boolean cachedHasWrites;
        private @Nullable Boolean cachedHasReferenceToShadowedVariable;
        private @Nullable Map<Integer, Set<J.Identifier>> cachedRelevantShadowing;
        
        /**
         * Check if the variable has any usage (read or write) - most efficient check.
         * Short-circuits on first reference found.
         */
        public boolean hasUses() {
            return hasReads() || hasWrites();
        }
        
        /**
         * Check if the variable is read anywhere in the search scope.
         * Uses lazy evaluation and caches result.
         */
        public boolean hasReads() {
            if (cachedHasReads != null) {
                return cachedHasReads;
            }
            
            // Try to find first read without computing all reads
            AtomicBoolean found = new AtomicBoolean(false);
            for (Statement stmt : statementsToSearch) {
                new ScopeAwareVariableUsageVisitor(varName, found, null, true, false).visit(stmt, 0);
                if (found.get()) {
                    cachedHasReads = true;
                    return true;
                }
            }
            cachedHasReads = false;
            return false;
        }
        
        /**
         * Check if the variable is written anywhere in the search scope.
         * Uses lazy evaluation and caches result.
         */
        public boolean hasWrites() {
            if (cachedHasWrites != null) {
                return cachedHasWrites;
            }
            
            // Try to find first write without computing all writes
            AtomicBoolean found = new AtomicBoolean(false);
            for (Statement stmt : statementsToSearch) {
                new ScopeAwareVariableUsageVisitor(varName, found, null, false, true).visit(stmt, 0);
                if (found.get()) {
                    cachedHasWrites = true;
                    return true;
                }
            }
            cachedHasWrites = false;
            return false;
        }
        
        /**
         * Get all read references to the variable.
         * Computes and caches all reads if not already done.
         */
        public Set<J.Identifier> getReads() {
            if (cachedReads == null) {
                Set<J.Identifier> reads = new HashSet<>();
                for (Statement stmt : statementsToSearch) {
                    new ScopeAwareVariableUsageVisitor(varName, null, reads, true, false).visit(stmt, 0);
                }
                cachedReads = reads;
                cachedHasReads = !reads.isEmpty();
            }
            return cachedReads;
        }
        
        /**
         * Get all write references to the variable.
         * Computes and caches all writes if not already done.
         */
        public Set<J.Identifier> getWrites() {
            if (cachedWrites == null) {
                Set<J.Identifier> writes = new HashSet<>();
                for (Statement stmt : statementsToSearch) {
                    new ScopeAwareVariableUsageVisitor(varName, null, writes, false, true).visit(stmt, 0);
                }
                cachedWrites = writes;
                cachedHasWrites = !writes.isEmpty();
            }
            return cachedWrites;
        }
        
        /**
         * Get all references (reads and writes) to the variable.
         */
        public Set<J.Identifier> getAllUses() {
            Set<J.Identifier> all = new HashSet<>();
            all.addAll(getReads());
            all.addAll(getWrites());
            return all;
        }
        
        /**
         * Check if any of the collected reads/writes refer to shadowed variables.
         * This helps determine if the usage refers to the original variable or a shadowed one.
         * 
         * @return true if any usage refers to a variable that shadows the target variable
         */
        public boolean hasReferenceToShadowedVariable() {
            if (cachedHasReferenceToShadowedVariable != null) {
                return cachedHasReferenceToShadowedVariable;
            }
            
            // Get all shadowing declarations in the containing block
            Map<Integer, Set<J.Identifier>> allShadowing = scope.getShadowingDeclarations(varName);
            if (allShadowing.isEmpty()) {
                cachedHasReferenceToShadowedVariable = false;
                return false;
            }
            
            // Check if any of our collected identifiers are within shadowed scopes
            Set<J.Identifier> allUses = getAllUses();
            boolean hasReference = !getRelevantShadowing().isEmpty() && !allUses.isEmpty();
            
            cachedHasReferenceToShadowedVariable = hasReference;
            return hasReference;
        }
        
        /**
         * Get shadowing information relevant to this usage analysis.
         * Returns only shadowing that affects the statements being analyzed in this VariableUsage.
         * 
         * @return map of scope depths where the target variable is shadowed within the analyzed statements
         */
        public Map<Integer, Set<J.Identifier>> getRelevantShadowing() {
            if (cachedRelevantShadowing != null) {
                return cachedRelevantShadowing;
            }
            
            Map<Integer, Set<J.Identifier>> relevantShadowing = new HashMap<>();
            
            // Check for shadowing within our statements to search
            for (Statement stmt : statementsToSearch) {
                // Check if this statement contains any shadowing declarations
                ShadowingWithinStatementVisitor visitor = new ShadowingWithinStatementVisitor(varName);
                visitor.visit(stmt, 0);
                
                if (!visitor.foundShadowing.isEmpty()) {
                    for (Map.Entry<Integer, Set<J.Identifier>> entry : visitor.foundShadowing.entrySet()) {
                        relevantShadowing.computeIfAbsent(entry.getKey(), k -> new HashSet<>())
                                        .addAll(entry.getValue());
                    }
                }
            }
            
            cachedRelevantShadowing = relevantShadowing;
            return relevantShadowing;
        }
        
    }

    /**
     * Fluent builder for configuring variable usage scope queries.
     * Allows precise control over which parts of the code to include in the analysis.
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ScopeQuery {
        @Getter(AccessLevel.PACKAGE)
        private final Cursor reference;

        private boolean includeCurrentStatement = false;
        private boolean includeEnclosingScopes = false;
        private boolean includeParameters = false;
        private boolean includeFields = false;
        private boolean includePrecedingStatements = false;
        private boolean includeFollowingStatements = false;
        private boolean includeNestedScopes = false;

        /**
         * Start building a scope query relative to the given statement.
         */
        public static ScopeQuery at(J reference, Cursor parent) {
            return new ScopeQuery(new Cursor(parent, reference));
        }

        /**
         * Start building a scope query relative to the given statement.
         */
        public static ScopeQuery at(Cursor reference) {
            return new ScopeQuery(reference);
        }

        /**
         * Include the reference statement itself in the analysis.
         */
        public ScopeQuery includeCurrentStatement() {
            this.includeCurrentStatement = true;
            return this;
        }

        /**
         * Include variables from enclosing scopes (method parameters, outer blocks, etc.).
         */
        public ScopeQuery includeEnclosingScopes() {
            this.includeEnclosingScopes = true;
            return this;
        }

        /**
         * Include method/lambda parameters in the analysis.
         */
        public ScopeQuery includeParameters() {
            this.includeParameters = true;
            return this;
        }

        /**
         * Include class fields in the analysis.
         */
        public ScopeQuery includeFields() {
            this.includeFields = true;
            return this;
        }

        /**
         * Include statements that come before the reference statement in the current block.
         */
        public ScopeQuery includePrecedingStatements() {
            this.includePrecedingStatements = true;
            return this;
        }

        /**
         * Include statements that come after the reference statement in the current block.
         */
        public ScopeQuery includeFollowingStatements() {
            this.includeFollowingStatements = true;
            return this;
        }

        /**
         * Include nested scopes (lambdas, anonymous classes, etc.) in the analysis.
         */
        public ScopeQuery includeNestedScopes() {
            this.includeNestedScopes = true;
            return this;
        }

        /**
         * Include everything in the current block (current + preceding + following statements).
         */
        public ScopeQuery includeCurrentBlock() {
            return includeCurrentStatement()
                    .includePrecedingStatements()
                    .includeFollowingStatements();
        }

        /**
         * Include everything accessible from this position (enclosing scopes + current block).
         */
        public ScopeQuery includeAll() {
            return includeCurrentBlock()
                    .includeEnclosingScopes()
                    .includeParameters()
                    .includeFields()
                    .includeNestedScopes();
        }

        boolean shouldIncludeCurrentStatement() { return includeCurrentStatement; }
        boolean shouldIncludeEnclosingScopes() { return includeEnclosingScopes; }
        boolean shouldIncludeParameters() { return includeParameters; }
        boolean shouldIncludeFields() { return includeFields; }
        boolean shouldIncludePrecedingStatements() { return includePrecedingStatements; }
        boolean shouldIncludeFollowingStatements() { return includeFollowingStatements; }
        boolean shouldIncludeNestedScopes() { return includeNestedScopes; }
    }

    /**
     * Get detailed variable usage information with flexible scope control using a fluent query builder.
     * Returns a VariableUsage object that allows efficient querying with lazy evaluation.
     *
     * @param varName The name of the variable to analyze
     * @param query   The scope query configuration
     * @return VariableUsage object with lazy evaluation capabilities
     */
    public VariableUsage getVariableUsage(String varName, ScopeQuery query) {
        List<Statement> statementsToSearch = new ArrayList<>();
        
        Cursor referenceCursor = query.getReference();
        J.Block containingBlock = referenceCursor.firstEnclosingOrThrow(J.Block.class);
        List<Statement> statements = containingBlock.getStatements();
        Statement containingStatement = referenceCursor.firstEnclosingOrThrow(Statement.class);
        int statementIndex = statements.indexOf(containingStatement);

        if (statementIndex == -1) {
            return new VariableUsage(varName, Collections.emptyList(), this);
        }

        // Add statements based on query configuration
        if (query.shouldIncludePrecedingStatements() && statementIndex > 0) {
            statementsToSearch.addAll(statements.subList(0, statementIndex));
        }

        if (query.shouldIncludeCurrentStatement()) {
            statementsToSearch.add(containingStatement);
        }

        if (query.shouldIncludeFollowingStatements() && statementIndex < statements.size() - 1) {
            statementsToSearch.addAll(statements.subList(statementIndex + 1, statements.size()));
        }

        // TODO: Implement other scope types (enclosing scopes, parameters, fields, nested scopes)
        // For now, we only handle statement-level analysis within the current block

        return new VariableUsage(varName, statementsToSearch, this);
    }


    /**
     * Get detailed variable usage information after a given statement in the containing block.
     * Returns a VariableUsage object that allows efficient querying with lazy evaluation.
     *
     * @param varName   The name of the variable to analyze
     * @param statement The statement after which to check for variable usage
     * @return VariableUsage object with lazy evaluation capabilities
     */
    public VariableUsage getVariableUsageAfter(String varName, Cursor statement) {
        return getVariableUsage(varName, ScopeQuery.at(statement).includeFollowingStatements());
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
     * across different lexical scope depths. Can distinguish between reads and writes.
     */
    @RequiredArgsConstructor
    private static class ScopeAwareVariableUsageVisitor extends JavaIsoVisitor<Integer> {
        private final String targetVarName;
        private final @Nullable AtomicBoolean found;
        private final @Nullable Set<J.Identifier> collectedIdentifiers;
        private final boolean collectReads;
        private final boolean collectWrites;

        // Track lexical scopes where the variable is shadowed
        private int lexicalScopeDepth = 0;
        private final Set<Integer> shadowedAtDepth = new HashSet<>();

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
            // Only count it if it's not shadowed in current lexical scope
            if (identifier.getSimpleName().equals(targetVarName) && !shadowedAtDepth.contains(lexicalScopeDepth)) {
                boolean isWrite = isVariableWrite(identifier);
                boolean isRead = !isWrite;
                
                // Check if this usage type matches what we're looking for
                if ((collectReads && isRead) || (collectWrites && isWrite)) {
                    if (found != null) {
                        found.set(true);
                    }
                    if (collectedIdentifiers != null) {
                        collectedIdentifiers.add(identifier);
                    }
                }
            }
            return super.visitIdentifier(identifier, p);
        }
        
        /**
         * Determines if an identifier represents a write operation to a variable.
         * This includes assignments, increment/decrement operations, and method calls that modify the variable.
         */
        private boolean isVariableWrite(J.Identifier identifier) {
            Cursor cursor = getCursor();
            
            // Walk up the cursor to find the context
            while (cursor != null) {
                Object parent = cursor.getValue();
                
                // Direct assignment: x = value
                if (parent instanceof J.Assignment) {
                    J.Assignment assignment = (J.Assignment) parent;
                    return assignment.getVariable() == identifier;
                }
                
                // Compound assignment: x += value, x *= value, etc.
                if (parent instanceof J.AssignmentOperation) {
                    J.AssignmentOperation assignOp = (J.AssignmentOperation) parent;
                    return assignOp.getVariable() == identifier;
                }
                
                // Unary operations: ++x, --x, x++, x--
                if (parent instanceof J.Unary) {
                    J.Unary unary = (J.Unary) parent;
                    if (unary.getOperator() == J.Unary.Type.PreIncrement || 
                        unary.getOperator() == J.Unary.Type.PreDecrement ||
                        unary.getOperator() == J.Unary.Type.PostIncrement ||
                        unary.getOperator() == J.Unary.Type.PostDecrement) {
                        return unary.getExpression() == identifier;
                    }
                }
                
                // Variable declaration with initialization: int x = value
                if (parent instanceof J.VariableDeclarations.NamedVariable) {
                    J.VariableDeclarations.NamedVariable namedVar = (J.VariableDeclarations.NamedVariable) parent;
                    return namedVar.getName() == identifier && namedVar.getInitializer() != null;
                }
                
                // Stop searching if we hit certain boundaries
                if (parent instanceof J.MethodInvocation || 
                    parent instanceof J.NewClass ||
                    parent instanceof J.Lambda ||
                    parent instanceof J.Block) {
                    break;
                }
                
                cursor = cursor.getParent();
            }
            
            // Default to read if we can't determine it's a write
            return false;
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

        @Override
        public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, Integer p) {
            // Handle instanceof pattern variables (Java 14+) - they shadow at the current lexical scope
            if (instanceOf.getPattern() instanceof J.Identifier) {
                J.Identifier patternVar = (J.Identifier) instanceOf.getPattern();
                if (patternVar.getSimpleName().equals(targetVarName)) {
                    shadowedAtDepth.add(lexicalScopeDepth);
                }
            }
            // Also check if the pattern is wrapped in a VariableDeclarations
            else if (instanceOf.getPattern() instanceof J.VariableDeclarations) {
                J.VariableDeclarations varDecls = (J.VariableDeclarations) instanceOf.getPattern();
                for (J.VariableDeclarations.NamedVariable var : varDecls.getVariables()) {
                    if (var.getSimpleName().equals(targetVarName)) {
                        shadowedAtDepth.add(lexicalScopeDepth);
                    }
                }
            }
            return super.visitInstanceOf(instanceOf, p);
        }
    }

    /**
     * A visitor that detects variable shadowing within a specific statement or set of statements.
     * Used by VariableUsage to find relevant shadowing information.
     */
    @RequiredArgsConstructor
    private static class ShadowingWithinStatementVisitor extends JavaIsoVisitor<Integer> {
        private final String targetVarName;
        final Map<Integer, Set<J.Identifier>> foundShadowing = new HashMap<>();
        private int lexicalScopeDepth = 0;

        @Override
        public J.Lambda visitLambda(J.Lambda lambda, Integer p) {
            lexicalScopeDepth++;

            // Check lambda parameters for shadowing
            for (J parameter : lambda.getParameters().getParameters()) {
                if (parameter instanceof J.VariableDeclarations) {
                    J.VariableDeclarations varDecls = (J.VariableDeclarations) parameter;
                    for (J.VariableDeclarations.NamedVariable var : varDecls.getVariables()) {
                        if (var.getSimpleName().equals(targetVarName)) {
                            foundShadowing.computeIfAbsent(lexicalScopeDepth, k -> new HashSet<>()).add(var.getName());
                        }
                    }
                } else if (parameter instanceof J.Identifier) {
                    J.Identifier paramId = (J.Identifier) parameter;
                    if (paramId.getSimpleName().equals(targetVarName)) {
                        foundShadowing.computeIfAbsent(lexicalScopeDepth, k -> new HashSet<>()).add(paramId);
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
                // Only consider it shadowing if we're actually in a deeper scope
                if (lexicalScopeDepth > 0) {
                    foundShadowing.computeIfAbsent(lexicalScopeDepth, k -> new HashSet<>()).add(variable.getName());
                }
            }
            return super.visitVariable(variable, p);
        }
        
        @Override
        public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, Integer p) {
            // Handle instanceof pattern variables (Java 14+)
            if (instanceOf.getPattern() instanceof J.Identifier) {
                J.Identifier patternVar = (J.Identifier) instanceOf.getPattern();
                if (patternVar.getSimpleName().equals(targetVarName)) {
                    // instanceof pattern variables always shadow at depth 1
                    foundShadowing.computeIfAbsent(1, k -> new HashSet<>()).add(patternVar);
                }
            }
            // Also check if the pattern is wrapped in a VariableDeclarations
            else if (instanceOf.getPattern() instanceof J.VariableDeclarations) {
                J.VariableDeclarations varDecls = (J.VariableDeclarations) instanceOf.getPattern();
                for (J.VariableDeclarations.NamedVariable var : varDecls.getVariables()) {
                    if (var.getSimpleName().equals(targetVarName)) {
                        foundShadowing.computeIfAbsent(1, k -> new HashSet<>()).add(var.getName());
                    }
                }
            }
            return super.visitInstanceOf(instanceOf, p);
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
            return new VariableScope(cursor);
        }
    }
}
