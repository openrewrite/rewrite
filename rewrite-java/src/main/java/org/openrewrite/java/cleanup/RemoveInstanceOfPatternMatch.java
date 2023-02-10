/*
 * Copyright 2023 the original author or authors.
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

import java.time.Duration;
import java.util.*;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.Markers;

/**
 * The recipe that replaces `instanceof` pattern matching by a simple variable
 * declarations.
 */
public class RemoveInstanceOfPatternMatch extends Recipe {

    @Override
    public String getDisplayName() {
        return "Removes from code Java 14's `instanceof` pattern matching";
    }

    @Override
    public String getDescription() {
        return "Adds an explicit variable declaration at the beginning of `if` statement instead of `instanceof` pattern matching.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesJavaVersion<>(14);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveInstanceOfPatternMatchVisitor();
    }

    /**
     * The implementation `instanceof` pattern match replacement.
     */
    private static class RemoveInstanceOfPatternMatchVisitor extends JavaVisitor<ExecutionContext> {

        private VariableUsage variableUsage;

        @Override
        public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext p) {
            // Analyze variable usage in the whole compilation unit and
            // run the compilation unit transformation.
            // Maybe it's better to use messages instead of the private field
            variableUsage = VariableUsageAnalyzer.analyze(cu);
            J.CompilationUnit result = (J.CompilationUnit) super.visitCompilationUnit(cu, p);
            variableUsage = null;
            return result;
        }

        @Override
        public J visitInstanceOf(J.InstanceOf instanceOf, ExecutionContext p) {
            // Remove pattern variables from instanceof and continue analyzes,
            // because expressions in LHS of instanceof can contain variables,
            // that should be replaced by their definitions
            // For instance
            // if (obj instanceof String str)
            // is replaced by the following:
            // if (obj instanceof String)
            return super.visitInstanceOf(instanceOf.withPattern(null), p);
        }

        @Override
        public J visitIdentifier(J.Identifier identifier, ExecutionContext p) {
            // If the identifier is a usage of a variable declared in an instanceof
            // expression, then replace it by an LHS of the instanceof cast to
            // a specified type.
            // The resulting type cast expression is always enclosed in parentheses,
            // but it would be good to remove parentheses  when they aren't required
            // For instance
            // if (obj instanceof String str && str.isEmpty())
            // is replaced by the following:
            // if (obj instanceof String str && ((String) obj).isEmpty())
            J.InstanceOf instanceOf = variableUsage.conditions.get(identifier);
            if (instanceOf != null) {
                return autoFormat(
                        parentheses(typeCast(
                                (TypeTree) instanceOf.getClazz(),
                                instanceOf.getExpression())).withPrefix(identifier.getPrefix()),
                        p);
            }
            return identifier;
        }

        @Override
        public J.If visitIf(J.If iff, ExecutionContext p) {
            J.If result = (J.If) super.visitIf(iff, p);

            // If the "then" part of the "if" statement uses variables declared in
            // an "instanceof" expression, then add a variable declaration at
            // the beginning of the block
            Deque<J.InstanceOf> thenInstanceOfs = variableUsage.thenParts.get(iff);
            if (thenInstanceOfs != null) {
                // Replace a single statement by a block
                if (!(result.getThenPart() instanceof J.Block)) {
                    result = autoFormat(result.withThenPart(J.Block.createEmptyBlock()
                            .withStatements(Collections.singletonList(result.getThenPart()))), p);
                }
                // Add variable declarations
                while (!thenInstanceOfs.isEmpty()) {
                    result = result.withThenPart(addVariableDeclaration(
                            (J.Block) result.getThenPart(), thenInstanceOfs.removeLast(), p));
                }
            }

            // If the "else" part of the "if" statement uses variables declared in
            // an "instanceof" expression, then add a variable declaration at
            // the beginning of the block
            Deque<J.InstanceOf> elseInstanceOfs = variableUsage.elseParts.get(iff.getElsePart());
            J.If.Else elsePart = result.getElsePart();
            if (elsePart != null && elseInstanceOfs != null) {
                // Replace a single statement by a block
                if (!(elsePart.getBody() instanceof J.Block)) {
                    result = autoFormat(result.withElsePart(elsePart.withBody(
                            J.Block.createEmptyBlock().withStatements(
                                    Collections.singletonList(elsePart.getBody())))),
                            p);
                }
                // Add variable declarations
                while (!elseInstanceOfs.isEmpty()) {
                    result = result.withElsePart(elsePart.withBody(
                            addVariableDeclaration(
                                    (J.Block) elsePart.getBody(), elseInstanceOfs.removeLast(), p)));
                }
            }
            return result;
        }

        /**
         * Adds a variable declaration at the beginning of a statement block. The
         * declaration is based on a pattern variable declared in an instanceof
         * expression.
         *
         * @param block the statement block
         * @param instanceOf the instanceof expression
         * @param p the execution context
         * @return the updated block
         */
        private J.Block addVariableDeclaration(J.Block block, J.InstanceOf instanceOf, ExecutionContext p) {
            JavaTemplate template = JavaTemplate
                    .builder(this::getCursor, "#{} #{} = (#{}) #{any()}")
                    .build();
            return block.withTemplate(template,
                    block.getCoordinates().firstStatement(),
                    instanceOf.getClazz().toString(),
                    ((J.Identifier) Objects.requireNonNull(instanceOf.getPattern())).getSimpleName(),
                    instanceOf.getClazz().toString(),
                    visit(instanceOf.getExpression(), p));
        }

        /**
         * Creates a type cast expression.
         *
         * @param typeTree the type tree
         * @param expression the expression to cast
         * @return the type cast expression
         */
        private J.TypeCast typeCast(TypeTree typeTree, Expression expression) {
            return new J.TypeCast(
                    Tree.randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    new J.ControlParentheses<>(
                            Tree.randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            padRight(typeTree)),
                    expression);
        }

        /**
         * Wraps a tree in parentheses.
         *
         * @param <T> the expression type
         * @param tree the tree
         * @return the wrapped tree
         */
        private <T extends J> J.Parentheses<T> parentheses(T tree) {
            return new J.Parentheses<>(Tree.randomId(), Space.EMPTY, Markers.EMPTY, padRight(tree));
        }

        /**
         * Wraps a tree in a {@link JRightPadded<T>} object.
         *
         * @param <T> the expression type
         * @param tree the tree
         * @return the wrapped tree
         */
        private <T> JRightPadded<T> padRight(T tree) {
            return new JRightPadded<>(tree, Space.EMPTY, Markers.EMPTY);
        }

    }

    /**
     * Variable analyzes context.
     */
    private enum ContextKind {
        NONE,
        CONDITION,
        THEN_PART,
        ELSE_PART
    }

    /**
     * Variable usage information.
     */
    private static class VariableUsage {

        /**
         * Variables used in conditions of "if" statements or in ternary operators.
         */
        public Map<J.Identifier, J.InstanceOf> conditions = new HashMap<>();

        /**
         * Variables used in "then" parts of "if" statements.
         */
        public Map<J.If, Deque<J.InstanceOf>> thenParts = new HashMap<>();

        /**
         * Variables used in "else" parts of "if" statements.
         */
        public Map<J.If.Else, Deque<J.InstanceOf>> elseParts = new HashMap<>();

    }

    /**
     * Analyzes information on variable usage. Only variables declared using
     * instanceof pattern matching are considered.
     */
    private static class VariableUsageAnalyzer extends JavaIsoVisitor<ContextKind> {

        /**
         * Names of variables in the current scope mapped to instanceof expressions
         * declaring them
         */
        private final Map<String, J.InstanceOf> currentScope = new HashMap<>();

        private final VariableUsage variableUsage = new VariableUsage();

        private VariableUsageAnalyzer() {
        }

        /**
         * Analyzes variable usage.
         *
         * @param tree the tree to analyze
         * @return the variable usage
         */
        public static VariableUsage analyze(J tree) {
            VariableUsageAnalyzer collector = new VariableUsageAnalyzer();
            collector.visit(tree, ContextKind.NONE);
            collector.currentScope.clear();
            return collector.variableUsage;
        }

        @Override
        public J.If visitIf(J.If iff, ContextKind p) {
            return iff.withIfCondition(visitAndCast(iff.getIfCondition(), ContextKind.CONDITION))
                    .withThenPart(visitAndCast(iff.getThenPart(), ContextKind.THEN_PART))
                    .withElsePart(visitAndCast(iff.getElsePart(), ContextKind.ELSE_PART));
        }

        @Override
        public J.Ternary visitTernary(J.Ternary ternary, ContextKind p) {
            return super.visitTernary(ternary, ContextKind.CONDITION);
        }

        @Override
        public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, ContextKind p) {
            // If the instanceof has a pattern variable, then add it to the current variable scope
            if (instanceOf.getPattern() instanceof J.Identifier) {
                String variableName = ((J.Identifier) instanceOf.getPattern()).getSimpleName();
                currentScope.put(variableName, instanceOf);
            }
            // An expression in the LHS of the instanceof can contain variables, so analyze it
            visit(instanceOf.getExpression(), p);
            return instanceOf;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(
                J.VariableDeclarations.NamedVariable variable, ContextKind p) {
            // Only pattern variables from instanceof should be in the current scope.
            // If there is a same-named explicit variable declaration, then remove it from the scope
            currentScope.remove(variable.getSimpleName());
            // Variable initialization expressions can contain variables, so analyze it
            visit(variable.getInitializer(), p);
            return variable;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, ContextKind p) {
            // If the identifier is a variable declared using an instanceof statement,
            // then add it to the variable usage according to the current context
            J.InstanceOf instanceOf = currentScope.get(identifier.getSimpleName());
            if (instanceOf != null) {
                switch (p) {
                case NONE:
                    break;
                case CONDITION:
                    variableUsage.conditions.put(identifier, instanceOf);
                    break;
                case THEN_PART:
                    variableUsage.thenParts
                            .computeIfAbsent(
                                    getCursor().firstEnclosingOrThrow(J.If.class),
                                    k -> new ArrayDeque<>())
                            .add(instanceOf);
                    break;
                case ELSE_PART:
                    variableUsage.elseParts
                            .computeIfAbsent(
                                    getCursor().firstEnclosingOrThrow(J.If.Else.class),
                                    k -> new ArrayDeque<>())
                            .add(instanceOf);
                    break;
                }
            }
            return identifier;
        }

    }

}
