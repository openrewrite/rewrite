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
package org.openrewrite.java.internal.template;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Loop;
import org.openrewrite.java.tree.Statement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class PatternVariables {

    private static final String DEFAULT_LABEL = "!";

    private static class ResultCollector {
        final StringBuilder builder = new StringBuilder();
        boolean instanceOfFound;
    }

    static @Nullable String simplifiedPatternVariableCondition(Expression condition, @Nullable J toReplace) {
        ResultCollector resultCollector = new ResultCollector();
        simplifiedPatternVariableCondition0(condition, toReplace, resultCollector);
        return resultCollector.instanceOfFound ? resultCollector.builder.toString() : null;
    }

    private static boolean simplifiedPatternVariableCondition0(J expr, @Nullable J toReplace, ResultCollector collector) {
        if (expr == toReplace) {
            collector.builder.append('§');
            return true;
        }

        if (expr instanceof J.Parentheses) {
            J.Parentheses<?> parens = (J.Parentheses<?>) expr;
            collector.builder.append('(');
            try {
                return simplifiedPatternVariableCondition0(parens.getTree(), toReplace, collector);
            } finally {
                collector.builder.append(')');
            }
        } else if (expr instanceof J.Unary) {
            J.Unary unary = (J.Unary) expr;
            switch (unary.getOperator()) {
                case PostIncrement: {
                    boolean found = simplifiedPatternVariableCondition0(unary.getExpression(), toReplace, collector);
                    collector.builder.append("++");
                    return found;
                }
                case PostDecrement: {
                    boolean found = simplifiedPatternVariableCondition0(unary.getExpression(), toReplace, collector);
                    collector.builder.append("--");
                    return found;
                }
                case PreIncrement:
                    collector.builder.append("++");
                    break;
                case PreDecrement:
                    collector.builder.append("--");
                    break;
                case Positive:
                    collector.builder.append('+');
                    break;
                case Negative:
                    collector.builder.append('-');
                    break;
                case Complement:
                    collector.builder.append('~');
                    break;
                case Not:
                    collector.builder.append('!');
                    break;
                default:
                    throw new IllegalStateException("Unexpected unary operator: " + unary.getOperator());
            }
            return simplifiedPatternVariableCondition0(unary.getExpression(), toReplace, collector);
        } else if (expr instanceof J.Binary) {
            J.Binary binary = (J.Binary) expr;
            int length = collector.builder.length();
            boolean result = simplifiedPatternVariableCondition0(binary.getLeft(), toReplace, collector);
            switch (binary.getOperator()) {
                case Addition:
                    collector.builder.append('+');
                    break;
                case Subtraction:
                    collector.builder.append('-');
                    break;
                case Multiplication:
                    collector.builder.append('*');
                    break;
                case Division:
                    collector.builder.append('/');
                    break;
                case Modulo:
                    collector.builder.append('%');
                    break;
                case LessThan:
                    collector.builder.append('<');
                    break;
                case GreaterThan:
                    collector.builder.append('>');
                    break;
                case LessThanOrEqual:
                    collector.builder.append("<=");
                    break;
                case GreaterThanOrEqual:
                    collector.builder.append(">=");
                    break;
                case Equal:
                    collector.builder.append("==");
                    break;
                case NotEqual:
                    collector.builder.append("!=");
                    break;
                case BitAnd:
                    collector.builder.append('&');
                    break;
                case BitOr:
                    collector.builder.append('|');
                    break;
                case BitXor:
                    collector.builder.append('^');
                    break;
                case LeftShift:
                    collector.builder.append("<<");
                    break;
                case RightShift:
                    collector.builder.append(">>");
                    break;
                case UnsignedRightShift:
                    collector.builder.append(">>>");
                    break;
                case Or:
                    collector.builder.append("||");
                    break;
                case And:
                    collector.builder.append("&&");
                    break;
                default:
                    throw new IllegalStateException("Unexpected binary operator: " + binary.getOperator());
            }
            result |= simplifiedPatternVariableCondition0(binary.getRight(), toReplace, collector);
            if (!result) {
                switch (binary.getOperator()) {
                    case LessThan:
                    case GreaterThan:
                    case LessThanOrEqual:
                    case GreaterThanOrEqual:
                    case Equal:
                    case NotEqual:
                    case Or:
                    case And:
                        collector.builder.setLength(length);
                        collector.builder.append("true");
                        return false;
                }
            }
            return result;
        } else if (expr instanceof J.InstanceOf) {
            J.InstanceOf instanceOf = (J.InstanceOf) expr;
            if (instanceOf.getPattern() != null) {
                collector.builder.append("((Object)null) instanceof ").append(instanceOf.getClazz()).append(' ').append(instanceOf.getPattern());
                collector.instanceOfFound = true;
                return true;
            }
            collector.builder.append("true");
        } else if (expr instanceof J.Literal) {
            J.Literal literal = (J.Literal) expr;
            collector.builder.append(literal.getValue());
        } else if (expr instanceof Expression) {
            collector.builder.append("null");
        }
        return false;
    }

    static boolean neverCompletesNormally(Statement statement) {
        return neverCompletesNormally0(statement, new HashSet<>());
    }

    private static boolean neverCompletesNormally0(@Nullable Statement statement, Set<String> labelsToIgnore) {
        if (statement instanceof J.Return || statement instanceof J.Throw) {
            return true;
        } else if (statement instanceof J.Break) {
            J.Break breakStatement = (J.Break) statement;
            return breakStatement.getLabel() != null && !labelsToIgnore.contains(breakStatement.getLabel().getSimpleName()) ||
                    breakStatement.getLabel() == null && !labelsToIgnore.contains(DEFAULT_LABEL);
        } else if (statement instanceof J.Continue) {
            J.Continue continueStatement = (J.Continue) statement;
            return continueStatement.getLabel() != null && !labelsToIgnore.contains(continueStatement.getLabel().getSimpleName()) ||
                    continueStatement.getLabel() == null && !labelsToIgnore.contains(DEFAULT_LABEL);
        } else if (statement instanceof J.Block) {
            return neverCompletesNormally0(getLastStatement(statement), labelsToIgnore);
        } else if (statement instanceof Loop) {
            Loop loop = (Loop) statement;
            return neverCompletesNormallyIgnoringLabel(loop.getBody(), DEFAULT_LABEL, labelsToIgnore);
        } else if (statement instanceof J.If) {
            J.If if_ = (J.If) statement;
            return if_.getElsePart() != null &&
                    neverCompletesNormally0(if_.getThenPart(), labelsToIgnore) &&
                    neverCompletesNormally0(if_.getElsePart().getBody(), labelsToIgnore);
        } else if (statement instanceof J.Switch) {
            J.Switch switch_ = (J.Switch) statement;
            if (switch_.getCases().getStatements().isEmpty()) {
                return false;
            }
            Statement defaultCase = null;
            for (Statement case_ : switch_.getCases().getStatements()) {
                if (!neverCompletesNormallyIgnoringLabel(case_, DEFAULT_LABEL, labelsToIgnore)) {
                    return false;
                }
                if (case_ instanceof J.Case) {
                    Expression elem = ((J.Case) case_).getPattern();
                    if (elem instanceof J.Identifier && ((J.Identifier) elem).getSimpleName().equals("default")) {
                        defaultCase = case_;
                    }
                }
            }
            return neverCompletesNormallyIgnoringLabel(defaultCase, DEFAULT_LABEL, labelsToIgnore);
        } else if (statement instanceof J.Case) {
            J.Case case_ = (J.Case) statement;
            if (case_.getStatements().isEmpty()) {
                // fallthrough to next case
                return true;
            }
            return neverCompletesNormally0(getLastStatement(case_), labelsToIgnore);
        } else if (statement instanceof J.Try) {
            J.Try try_ = (J.Try) statement;
            if (try_.getFinally() != null && !try_.getFinally().getStatements().isEmpty() &&
                    neverCompletesNormally0(try_.getFinally(), labelsToIgnore)) {
                return true;
            }
            boolean bodyHasExit = false;
            if (!try_.getBody().getStatements().isEmpty() &&
                    !(bodyHasExit = neverCompletesNormally0(try_.getBody(), labelsToIgnore))) {
                return false;
            }
            for (J.Try.Catch catch_ : try_.getCatches()) {
                if (!neverCompletesNormally0(catch_.getBody(), labelsToIgnore)) {
                    return false;
                }
            }
            return bodyHasExit;
        } else if (statement instanceof J.Synchronized) {
            return neverCompletesNormally0(((J.Synchronized) statement).getBody(), labelsToIgnore);
        } else if (statement instanceof J.Label) {
            String label = ((J.Label) statement).getLabel().getSimpleName();
            Statement labeledStatement = ((J.Label) statement).getStatement();
            return neverCompletesNormallyIgnoringLabel(labeledStatement, label, labelsToIgnore);
        }
        return false;
    }

    private static boolean neverCompletesNormallyIgnoringLabel(@Nullable Statement statement, String label, Set<String> labelsToIgnore) {
        boolean added = labelsToIgnore.add(label);
        try {
            return neverCompletesNormally0(statement, labelsToIgnore);
        } finally {
            if (added) {
                labelsToIgnore.remove(label);
            }
        }
    }

    private static @Nullable Statement getLastStatement(Statement statement) {
        if (statement instanceof J.Block) {
            List<Statement> statements = ((J.Block) statement).getStatements();
            return statements.isEmpty() ? null : getLastStatement(statements.get(statements.size() - 1));
        } else if (statement instanceof J.Case) {
            List<Statement> statements = ((J.Case) statement).getStatements();
            return statements.isEmpty() ? null : getLastStatement(statements.get(statements.size() - 1));
        } else if (statement instanceof Loop) {
            return getLastStatement(((Loop) statement).getBody());
        }
        return statement;
    }
}
