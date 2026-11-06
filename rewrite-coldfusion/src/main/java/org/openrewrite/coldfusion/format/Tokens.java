/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.coldfusion.format;

import org.openrewrite.Cursor;
import org.openrewrite.coldfusion.marker.FunctionKeyword;
import org.openrewrite.coldfusion.marker.KeywordSpelling;
import org.openrewrite.coldfusion.marker.OperatorSpelling;
import org.openrewrite.coldfusion.marker.VarKeyword;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.*;

/**
 * The characters an element's syntax starts and ends with, so a space between two elements is only removed where
 * they would not run together into one token, as {@code a - -1} would into {@code a--1}.
 */
final class Tokens {
    private static final char UNKNOWN = '\0';

    private Tokens() {
    }

    static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    /**
     * Whether two tokens can be written with nothing between them and still lex as two.
     */
    static boolean canJoin(char left, char right) {
        if (left == UNKNOWN || right == UNKNOWN) {
            return false;
        }
        if (isWordChar(left) && isWordChar(right)) {
            return false;
        }
        switch ("" + left + right) {
            case "++":
            case "--":
            case "&&":
            case "||":
            case "==":
            case "**":
            case "//":
            case "/*":
            case "*/":
            case "::":
            case "??":
            case "..":
            case "!=":
            case "<=":
            case ">=":
            case "<>":
            case "=>":
            case "->":
            case "?:":
            case "?.":
            case "&=":
            case "+=":
            case "-=":
            case "*=":
            case "/=":
            case "%=":
            case "^=":
            case "<!":
                return false;
            default:
                // a number's decimal point
                return !(left == '.' && Character.isDigit(right) || Character.isDigit(left) && right == '.');
        }
    }

    static boolean canJoin(String left, char right) {
        return !left.isEmpty() && canJoin(left.charAt(left.length() - 1), right);
    }

    static char first(J j, Cursor cursor) {
        char c = firstOf(j);
        return c == UNKNOWN ? printed(j, cursor, true) : c;
    }

    static char last(J j, Cursor cursor) {
        char c = lastOf(j);
        return c == UNKNOWN ? printed(j, cursor, false) : c;
    }

    private static char firstOf(J j) {
        if (j instanceof J.Identifier) {
            return firstChar(((J.Identifier) j).getSimpleName());
        } else if (j instanceof J.Literal) {
            String source = ((J.Literal) j).getValueSource();
            return source == null ? UNKNOWN : firstChar(source);
        } else if (j instanceof J.FieldAccess) {
            return firstOf(((J.FieldAccess) j).getTarget());
        } else if (j instanceof J.MethodInvocation) {
            J.MethodInvocation m = (J.MethodInvocation) j;
            return m.getSelect() != null ? firstOf(m.getSelect()) : firstOf(m.getName());
        } else if (j instanceof J.ArrayAccess) {
            return firstOf(((J.ArrayAccess) j).getIndexed());
        } else if (j instanceof J.Binary) {
            return firstOf(((J.Binary) j).getLeft());
        } else if (j instanceof Cf.Binary) {
            return firstOf(((Cf.Binary) j).getLeft());
        } else if (j instanceof J.Unary) {
            J.Unary u = (J.Unary) j;
            if (isPostfix(u.getOperator())) {
                return firstOf(u.getExpression());
            }
            OperatorSpelling spelling = u.getMarkers().findFirst(OperatorSpelling.class).orElse(null);
            return spelling != null ? firstChar(spelling.getText()) : firstChar(Operators.unary(u.getOperator()));
        } else if (j instanceof J.Assignment) {
            return j.getMarkers().findFirst(VarKeyword.class).isPresent() ? 'v' : firstOf(((J.Assignment) j).getVariable());
        } else if (j instanceof J.AssignmentOperation) {
            return j.getMarkers().findFirst(VarKeyword.class).isPresent() ? 'v' : firstOf(((J.AssignmentOperation) j).getVariable());
        } else if (j instanceof Cf.AssignmentOperation) {
            return j.getMarkers().findFirst(VarKeyword.class).isPresent() ? 'v' : firstOf(((Cf.AssignmentOperation) j).getVariable());
        } else if (j instanceof J.Ternary) {
            return firstOf(((J.Ternary) j).getCondition());
        } else if (j instanceof J.Parentheses) {
            return '(';
        } else if (j instanceof J.NewArray) {
            return ((J.NewArray) j).getTypeExpression() == null ? '[' : 'n';
        } else if (j instanceof Cf.StructLiteral) {
            return ((Cf.StructLiteral) j).isOrdered() ? '[' : '{';
        } else if (j instanceof J.NewClass) {
            return keyword(j, "new");
        } else if (j instanceof J.Lambda) {
            J.Lambda lambda = (J.Lambda) j;
            if (lambda.getMarkers().findFirst(FunctionKeyword.class).isPresent()) {
                return firstChar(lambda.getMarkers().findFirst(FunctionKeyword.class).get().getKeyword());
            }
            return lambda.getParameters().isParenthesized() ? '(' :
                    lambda.getParameters().getParameters().isEmpty() ? UNKNOWN : firstOf(lambda.getParameters().getParameters().get(0));
        } else if (j instanceof Cf.Closure) {
            return firstOf(((Cf.Closure) j).getLambda());
        } else if (j instanceof Cf.StringTemplate) {
            Cf.StringTemplate template = (Cf.StringTemplate) j;
            if (!template.getDelimiter().isEmpty()) {
                return template.getDelimiter().charAt(0);
            }
            return template.getStrings().isEmpty() ? UNKNOWN : firstOf(template.getStrings().get(0));
        } else if (j instanceof Cf.Interpolation) {
            return '#';
        } else if (j instanceof Cf.Invocation) {
            return firstOf(((Cf.Invocation) j).getTarget());
        } else if (j instanceof Cf.KeyValue) {
            return firstOf(((Cf.KeyValue) j).getKey());
        } else if (j instanceof Cf.TypeLiteral) {
            return firstOf(((Cf.TypeLiteral) j).getName());
        } else if (j instanceof Cf.ExpressionStatement) {
            return firstOf(((Cf.ExpressionStatement) j).getExpression());
        } else if (j instanceof J.VariableDeclarations.NamedVariable) {
            J.VariableDeclarations.NamedVariable v = (J.VariableDeclarations.NamedVariable) j;
            return v.getMarkers().findFirst(VarKeyword.class).isPresent() ? 'v' : firstOf(v.getName());
        } else if (j instanceof J.If) {
            String keyword = j.getMarkers().findFirst(KeywordSpelling.class).map(KeywordSpelling::getText).orElse("if");
            // an if spelled as nothing, inside elseif, starts at its condition
            return keyword.isEmpty() ? '(' : keyword.charAt(0);
        } else if (j instanceof J.Block) {
            return ((J.Block) j).isStatic() ? 's' : '{';
        }
        return UNKNOWN;
    }

    private static char lastOf(J j) {
        if (j instanceof J.Identifier) {
            return lastChar(((J.Identifier) j).getSimpleName());
        } else if (j instanceof J.Literal) {
            String source = ((J.Literal) j).getValueSource();
            return source == null ? UNKNOWN : lastChar(source);
        } else if (j instanceof J.FieldAccess) {
            return lastOf(((J.FieldAccess) j).getName());
        } else if (j instanceof J.MethodInvocation) {
            J.MethodInvocation m = (J.MethodInvocation) j;
            return m.getPadding().getArguments().getMarkers().findFirst(OmitParentheses.class).isPresent() ?
                    lastOf(m.getName()) : ')';
        } else if (j instanceof J.ArrayAccess) {
            return ']';
        } else if (j instanceof J.Binary) {
            return lastOf(((J.Binary) j).getRight());
        } else if (j instanceof Cf.Binary) {
            return lastOf(((Cf.Binary) j).getRight());
        } else if (j instanceof J.Unary) {
            J.Unary u = (J.Unary) j;
            if (isPostfix(u.getOperator())) {
                return '+' == Operators.unary(u.getOperator()).charAt(0) ? '+' : '-';
            }
            return lastOf(u.getExpression());
        } else if (j instanceof J.Assignment) {
            return lastOf(((J.Assignment) j).getAssignment());
        } else if (j instanceof J.AssignmentOperation) {
            return lastOf(((J.AssignmentOperation) j).getAssignment());
        } else if (j instanceof Cf.AssignmentOperation) {
            return lastOf(((Cf.AssignmentOperation) j).getAssignment());
        } else if (j instanceof J.Ternary) {
            return lastOf(((J.Ternary) j).getFalsePart());
        } else if (j instanceof J.Parentheses) {
            return ')';
        } else if (j instanceof J.NewArray) {
            return ']';
        } else if (j instanceof Cf.StructLiteral) {
            return ((Cf.StructLiteral) j).isOrdered() ? ']' : '}';
        } else if (j instanceof J.NewClass) {
            J.NewClass n = (J.NewClass) j;
            return n.getPadding().getArguments().getMarkers().findFirst(OmitParentheses.class).isPresent() && n.getClazz() != null ?
                    lastOf(n.getClazz()) : ')';
        } else if (j instanceof J.Lambda) {
            J body = ((J.Lambda) j).getBody();
            return body instanceof J.Block ? '}' : lastOf(body);
        } else if (j instanceof Cf.Closure) {
            return '}';
        } else if (j instanceof Cf.StringTemplate) {
            Cf.StringTemplate template = (Cf.StringTemplate) j;
            if (!template.getDelimiter().isEmpty()) {
                return template.getDelimiter().charAt(0);
            }
            return template.getStrings().isEmpty() ? UNKNOWN : lastOf(template.getStrings().get(template.getStrings().size() - 1));
        } else if (j instanceof Cf.Interpolation) {
            return '#';
        } else if (j instanceof Cf.Invocation) {
            return ')';
        } else if (j instanceof Cf.KeyValue) {
            return lastOf(((Cf.KeyValue) j).getValue());
        } else if (j instanceof Cf.TypeLiteral) {
            return lastOf(((Cf.TypeLiteral) j).getName());
        } else if (j instanceof Cf.ExpressionStatement) {
            return lastOf(((Cf.ExpressionStatement) j).getExpression());
        }
        return UNKNOWN;
    }

    private static boolean isPostfix(J.Unary.Type type) {
        return type == J.Unary.Type.PostIncrement || type == J.Unary.Type.PostDecrement;
    }

    private static char keyword(J j, String canonical) {
        return firstChar(j.getMarkers().findFirst(KeywordSpelling.class).map(KeywordSpelling::getText).orElse(canonical));
    }

    private static char printed(J j, Cursor cursor, boolean first) {
        String printed;
        try {
            printed = j.withPrefix(Space.EMPTY).printTrimmed(cursor);
        } catch (RuntimeException e) {
            return UNKNOWN;
        }
        if (printed.isEmpty()) {
            return UNKNOWN;
        }
        return first ? printed.charAt(0) : printed.charAt(printed.length() - 1);
    }

    private static char firstChar(String s) {
        return s.isEmpty() ? UNKNOWN : s.charAt(0);
    }

    private static char lastChar(String s) {
        return s.isEmpty() ? UNKNOWN : s.charAt(s.length() - 1);
    }
}
