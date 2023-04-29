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

import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.*;

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
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesJavaVersion<>(14), new RemoveInstanceOfPatternMatchVisitor());
    }

    /**
     * The implementation of "instanceof" pattern match replacement.
     */
    private static class RemoveInstanceOfPatternMatchVisitor extends JavaVisitor<ExecutionContext> {

        private VariableUsage variableUsage;

        @Override
        public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            // Analyze variable usage in the whole compilation unit and
            // run the compilation unit transformation.
            // Maybe it's better to use messages instead of the private field
            variableUsage = VariableUsageAnalyzer.analyze(cu);
            J.CompilationUnit result = (J.CompilationUnit) super.visitCompilationUnit(cu, ctx);
            variableUsage = null;
            return result;
        }

        @Override
        public J visitInstanceOf(J.InstanceOf instanceOf, ExecutionContext ctx) {
            // Remove pattern variables from instanceof.
            // And continue analyzes, because expressions in LHS of instanceof
            // can contain variables, that should be replaced by their definitions
            // For example the following code:
            // if (obj instanceof String str)
            // is replaced by:
            // if (obj instanceof String)
            return super.visitInstanceOf(instanceOf.withPattern(null), ctx);
        }

        @Override
        public J visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
            // If the identifier is a usage of a variable declared in an instanceof
            // expression, then replace it by an LHS of the instanceof cast to
            // a specified type.
            // For example the following code
            // if (obj instanceof String str && str.isEmpty())
            // is replaced by:
            // if (obj instanceof String str && ((String) obj).isEmpty())
            J.InstanceOf instanceOf = variableUsage.conditions.get(identifier);
            if (instanceOf != null) {
                J result = autoFormat(
                        typeCast((TypeTree) instanceOf.getClazz(), instanceOf.getExpression()),
                        ctx);
                // If a parent expression is a method invocation, enclose type cast in parentheses
                Object parent = getCursor().getParentTreeCursor().getValue();
                if (parent instanceof J.MethodInvocation) {
                    result = parentheses(result);
                }
                return result.withPrefix(identifier.getPrefix());
            }
            return identifier;
        }

        @Override
        public J.If visitIf(J.If iff, ExecutionContext ctx) {
            J.If result = (J.If) super.visitIf(iff, ctx);

            // If the "then" part of the "if" statement uses variables declared in
            // an "instanceof" expression, then add a variable declaration at
            // the beginning of the block
            Set<J.InstanceOf> thenInstanceOfs = variableUsage.thenParts.get(iff);
            if (thenInstanceOfs != null) {
                // Replace a single statement by a block
                if (!(result.getThenPart() instanceof J.Block)) {
                    result = autoFormat(result.withThenPart(J.Block.createEmptyBlock()
                            .withStatements(Collections.singletonList(result.getThenPart()))), ctx);
                }
                // Add variable declarations in the order of "instanceof" expressions
                Iterator<J.InstanceOf> iter = variableUsage.declarations.get(iff).descendingIterator();
                while (iter.hasNext()) {
                    J.InstanceOf instanceOf = iter.next();
                    if (thenInstanceOfs.contains(instanceOf)) {
                        result = result.withThenPart(addVariableDeclaration(
                                (J.Block) result.getThenPart(), instanceOf, ctx));
                    }
                }
            }

            // If the "else" part of the "if" statement uses variables declared in
            // an "instanceof" expression, then add a variable declaration at
            // the beginning of the block
            Set<J.InstanceOf> elseInstanceOfs = variableUsage.elseParts.get(iff.getElsePart());
            J.If.Else elsePart = result.getElsePart();
            if (elsePart != null && elseInstanceOfs != null) {
                // Replace a single statement by a block
                if (!(elsePart.getBody() instanceof J.Block)) {
                    result = autoFormat(result.withElsePart(elsePart.withBody(
                                    J.Block.createEmptyBlock().withStatements(
                                            Collections.singletonList(elsePart.getBody())))),
                            ctx);
                    elsePart = result.getElsePart();
                }
                if (elsePart != null) {
                    // Add variable declarations in the order of "instanceof" expressions
                    Iterator<J.InstanceOf> iter = variableUsage.declarations.get(iff).descendingIterator();
                    while (iter.hasNext()) {
                        J.InstanceOf instanceOf = iter.next();
                        if (elseInstanceOfs.contains(instanceOf)) {
                            result = result.withElsePart(elsePart.withBody(
                                    addVariableDeclaration(
                                            (J.Block) elsePart.getBody(), instanceOf, ctx)));
                        }
                    }
                }
            }
            return result;
        }

        /**
         * Adds a variable declaration at the beginning of a statement block. The
         * declaration is based on a pattern variable declared in an instanceof
         * expression.
         *
         * @param block      the statement block
         * @param instanceOf the instanceof expression
         * @param ctx        the execution context
         * @return the updated block
         */
        private J.Block addVariableDeclaration(J.Block block, J.InstanceOf instanceOf, ExecutionContext ctx) {
            JavaTemplate template = JavaTemplate
                    .builder(() -> new Cursor(getCursor(), block), "#{} #{} = (#{}) #{any()};")
                    .build();
            return block.withTemplate(template,
                    block.getCoordinates().firstStatement(),
                    instanceOf.getClazz().toString(),
                    ((J.Identifier) Objects.requireNonNull(instanceOf.getPattern())).getSimpleName(),
                    instanceOf.getClazz().toString(),
                    visit(instanceOf.getExpression(), ctx));
        }

        /**
         * Creates a type cast expression.
         *
         * @param typeTree   the type tree
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
         * @param <T>  the expression type
         * @param tree the tree
         * @return the wrapped tree
         */
        private <T extends J> J.Parentheses<T> parentheses(T tree) {
            return new J.Parentheses<>(Tree.randomId(), Space.EMPTY, Markers.EMPTY, padRight(tree));
        }

        /**
         * Wraps a tree in a {@link JRightPadded<T>} object.
         *
         * @param <T>  the expression type
         * @param tree the tree
         * @return the wrapped tree
         */
        private <T> JRightPadded<T> padRight(T tree) {
            return new JRightPadded<>(tree, Space.EMPTY, Markers.EMPTY);
        }

    }

    /**
     * Variable usage context.
     */
    private enum UsageContext {
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
         * Variables declared in "if" statements using "instanceof" expressions.
         */
        public Map<J.If, Deque<J.InstanceOf>> declarations = new HashMap<>();

        /**
         * Variables used in conditions of "if" statements or in ternary operators.
         */
        public Map<J.Identifier, J.InstanceOf> conditions = new HashMap<>();

        /**
         * Variables used in "then" parts of "if" statements.
         */
        public Map<J.If, Set<J.InstanceOf>> thenParts = new HashMap<>();

        /**
         * Variables used in "else" parts of "if" statements.
         */
        public Map<J.If.Else, Set<J.InstanceOf>> elseParts = new HashMap<>();

    }

    /**
     * Analyzes variable usage. Only variables declared using instanceof
     * pattern matching are considered.
     */
    private static class VariableUsageAnalyzer extends JavaIsoVisitor<J> {

        /**
         * Names of variables in the current scope mapped to "instanceof" expressions
         * declaring them.
         */
        private final Map<String, J.InstanceOf> currentScope = new HashMap<>();

        /**
         * Mapping of "instanceof" expressions to their parent trees (either "if"
         * statement or ternary operators).
         */
        private final Map<J.InstanceOf, J> parentTrees = new HashMap<>();

        /**
         * Results of variable usage analyzes.
         */
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
            collector.visit(tree, tree);
            collector.currentScope.clear();
            return collector.variableUsage;
        }

        @Override
        public J.If visitIf(J.If iff, J contextTree) {
            // Set the context to a current "if" statement, so all
            // nested "instanceof" expressions are related to it
            return iff.withIfCondition(visitAndCast(iff.getIfCondition(), iff))
                    .withThenPart(Objects.requireNonNull(visitAndCast(iff.getThenPart(), iff)))
                    .withElsePart(visitAndCast(iff.getElsePart(), iff));
        }

        @Override
        public J.Ternary visitTernary(J.Ternary ternary, J contextTree) {
            // Set the context to a current ternary operator, so all
            // nested "instanceof" expressions are related to it
            return super.visitTernary(ternary, ternary);
        }

        @Override
        public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, J contextTree) {
            // If the "instanceof" has a pattern variable,
            // then add it to the current variable scope
            if (instanceOf.getPattern() instanceof J.Identifier) {
                String variableName = ((J.Identifier) instanceOf.getPattern()).getSimpleName();
                currentScope.put(variableName, instanceOf);
                // Associate "instanceof" with either its parent "if" statement
                // or its parent ternary operator
                parentTrees.put(instanceOf, contextTree);
                // If the "instanceof" is used in the condition of an "if" statement,
                // then add it to a list of variable declarations associated with
                // the "if" statement, so later they can be converted to a simple
                // variable declarations
                if (contextTree instanceof J.If) {
                    variableUsage.declarations
                            .computeIfAbsent((J.If) contextTree, k -> new LinkedList<>())
                            .add(instanceOf);
                }
            }
            // An expression in the LHS of the "instanceof" can contain variables, so analyze it
            visit(instanceOf.getExpression(), contextTree);
            return instanceOf;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(
                J.VariableDeclarations.NamedVariable variable, J contextTree) {
            // Only pattern variables from "instanceof" should be in the current scope.
            // If there is a same-named explicit variable declaration,
            // then remove it from the scope
            currentScope.remove(variable.getSimpleName());
            // Variable initialization expressions can contain variables, so analyze it
            visit(variable.getInitializer(), contextTree);
            return variable;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, J contextTree) {
            // If the identifier is a variable declared using an "instanceof" statement,
            // then add it to the variable usage according to the current context
            J.InstanceOf instanceOf = currentScope.get(identifier.getSimpleName());
            if (instanceOf != null) {
                J parentTree = parentTrees.get(instanceOf);
                switch (getUsageContext(parentTree)) {
                    case NONE:
                        break;
                    case CONDITION:
                        variableUsage.conditions.put(identifier, instanceOf);
                        break;
                    case THEN_PART:
                        variableUsage.thenParts
                                .computeIfAbsent(
                                        (J.If) parentTree,
                                        k -> new HashSet<>())
                                .add(instanceOf);
                        break;
                    case ELSE_PART:
                        currentScope.get(identifier.getSimpleName());
                        variableUsage.elseParts
                                .computeIfAbsent(
                                        ((J.If) parentTree).getElsePart(),
                                        k -> new HashSet<>())
                                .add(instanceOf);
                        break;
                }
            }
            return identifier;
        }

        /**
         * Determines a usage context of the variable. a condition, "then" part or
         * "else" part.
         *
         * @param parentTree either "if" statement or ternary operator
         * @return the usage context
         */
        private UsageContext getUsageContext(J parentTree) {
            if (parentTree instanceof J.If) {
                J.If iff = (J.If) parentTree;
                Iterator<Object> iter = getCursor().getPath();
                while (iter.hasNext()) {
                    Object tree = iter.next();
                    if (tree.equals(iff.getIfCondition())) {
                        return UsageContext.CONDITION;
                    } else if (tree.equals(iff.getThenPart())) {
                        return UsageContext.THEN_PART;
                    } else if (tree.equals(iff.getElsePart())) {
                        return UsageContext.ELSE_PART;
                    }
                }
            } else if (parentTree instanceof J.Ternary) {
                return UsageContext.CONDITION;
            }
            return UsageContext.NONE;
        }

    }

}
