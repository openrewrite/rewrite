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

final class PatternVariables {

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
}
