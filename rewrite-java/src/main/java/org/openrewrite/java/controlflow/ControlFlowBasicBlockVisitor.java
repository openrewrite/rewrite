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
package org.openrewrite.java.controlflow;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.SearchResult;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Incubating(since = "7.25.0")
@AllArgsConstructor
public class ControlFlowBasicBlockVisitor<P> extends JavaIsoVisitor<P> {

    @SuppressWarnings("unused")
    private String getPredecessors(
            Map<J, ControlFlowNode> leadersToNodes,
            Map<ControlFlowNode, Integer> blockNumbers,
            J leader
    ) {
        if (leader instanceof J.ControlParentheses) {
            J.ControlParentheses<?> leaderControlParentheses = (J.ControlParentheses<?>) leader;
            ControlFlowNode block = leadersToNodes.get(leaderControlParentheses.getTree());

            List<String> predecessors =
                    block
                            .getPredecessors()
                            .stream()
                            .map(blockNumbers::get)
                            .map(Object::toString)
                            .collect(Collectors.toList());
            return "Predecessors: " + String.join(", ", predecessors);
        }

        ControlFlowNode block = leadersToNodes.get(leader);
        List<String> predecessors =
                block
                        .getPredecessors()
                        .stream()
                        .map(blockNumbers::get)
                        .map(Object::toString)
                        .collect(Collectors.toList());
        return "Predecessors: " + String.join(", ", predecessors);

    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        boolean isTestMethod = false;
        J.MethodDeclaration methodDeclaration = getCursor().firstEnclosing(J.MethodDeclaration.class);
        if (methodDeclaration != null) {
            isTestMethod = methodDeclaration.getBody() == block;
        }

        boolean isStaticOrInitBlock = J.Block.isStaticOrInitBlock(getCursor());
        if (isTestMethod || isStaticOrInitBlock) {
            return ControlFlow.startingAt(getCursor()).findControlFlow().map(controlFlow -> {
                // maps basic block and condition nodes to the first statement in the node (the node leader)
                Map<J, ControlFlowNode.BasicBlock> leadersToBlocks =
                        controlFlow
                                .getBasicBlocks()
                                .stream()
                                .collect(Collectors.toMap(ControlFlowNode.BasicBlock::getLeader, Function.identity()));
                Map<J, ControlFlowNode.ConditionNode> conditionToConditionNodes =
                        controlFlow
                                .getConditionNodes()
                                .stream()
                                .collect(Collectors.toMap(ControlFlowNode.ConditionNode::getCondition, Function.identity()));
                doAfterVisit(new ControlFlowMarkingVisitor<>("L", leadersToBlocks));
                doAfterVisit(new ControlFlowMarkingVisitor<>("C", conditionToConditionNodes));

                final String searchResultText =
                        "BB: " + controlFlow.getBasicBlocks().size() +
                                " CN: " + controlFlow.getConditionNodeCount() +
                                " EX: " + controlFlow.getExitCount();
                return block.withMarkers(block.getMarkers().searchResult(searchResultText));
            }).orElseGet(() -> super.visitBlock(block, p));
        }
        return super.visitBlock(block, p);
    }

    @RequiredArgsConstructor
    private static class ControlFlowMarkingVisitor<P> extends JavaIsoVisitor<P> {
        private final String label;
        private final Map<J, ? extends ControlFlowNode> nodeToBlock;
        private int nodeNumber = 0;
        private final Map<ControlFlowNode, Integer> nodeNumbers = new HashMap<>();

        @Override
        public Statement visitStatement(Statement statement, P p) {
            if (nodeToBlock.containsKey(statement)) {
                Optional<SearchResult> maybeSearchResult =
                        statement
                                .getMarkers()
                                .getMarkers()
                                .stream()
                                .filter(SearchResult.class::isInstance)
                                .map(SearchResult.class::cast)
                                .findFirst();
                ControlFlowNode b = nodeToBlock.get(statement);
                assert b != null;
                int number = nodeNumbers.computeIfAbsent(b, __ -> ++nodeNumber);
                if (maybeSearchResult.isPresent()) {
                    SearchResult searchResult = maybeSearchResult.get();
                    String newDescription =
                            (searchResult.getDescription() == null ? "" : searchResult.getDescription()) +
                                    " | " + number + label;
                    return statement.withMarkers(
                            statement
                                    .getMarkers()
                                    .removeByType(SearchResult.class)
                                    .add(searchResult.withDescription(newDescription)));
                } else {
                    return statement.withMarkers(statement.getMarkers().searchResult("" + number + label));
                }
            } else return statement;
        }

        @Override
        public Expression visitExpression(Expression expression, P p) {
            if (nodeToBlock.containsKey(expression)) {
                ControlFlowNode b = nodeToBlock.get(expression);
                assert b != null;
                int number = nodeNumbers.computeIfAbsent(b, __ -> ++nodeNumber);
                return expression.withMarkers(expression.getMarkers().searchResult(number + leaderDescription(expression)));
            }  else {
                return expression;
            }
        }

        private String leaderDescription(Expression e) {
            String tag = leaderTag(e);
            if (tag == null) {
                return label;
            }
            return label + " (" + tag + ')';
        }

        @Nullable
        private static String leaderTag(Expression e) {
            if (e instanceof J.Binary) {
                J.Binary binary = (J.Binary) e;
                switch (binary.getOperator()) {
                    case And:
                        return "&&";
                    case Or:
                        return "||";
                    case Addition:
                        return "+";
                    case Subtraction:
                        return "-";
                    case Multiplication:
                        return "*";
                    case Division:
                        return "/";
                    case Modulo:
                        return "%";
                    case LessThan:
                        return "<";
                    case LessThanOrEqual:
                        return "<=";
                    case GreaterThan:
                        return ">";
                    case GreaterThanOrEqual:
                        return ">=";
                    case Equal:
                        return "==";
                    case NotEqual:
                        return "!=";
                    case BitAnd:
                        return "&";
                    case BitOr:
                        return "|";
                    case BitXor:
                        return "^";
                    case LeftShift:
                        return "<<";
                    case RightShift:
                        return ">>";
                    case UnsignedRightShift:
                        return ">>>";
                    default:
                        throw new IllegalStateException("Unexpected value: " + binary.getOperator());
                }
            }
            return null;
        }
    }
}
