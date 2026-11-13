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

import org.jspecify.annotations.Nullable;
import org.openrewrite.coldfusion.marker.OperatorSpelling;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Markers;

/**
 * How operators are written, as the printer writes them.
 */
final class Operators {

    private Operators() {
    }

    static String binary(J.Binary binary) {
        String spelled = spelling(binary.getMarkers());
        if (spelled != null) {
            return spelled;
        }
        switch (binary.getOperator()) {
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
            case GreaterThan:
                return ">";
            case LessThanOrEqual:
                return "<=";
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
            case Or:
                return "||";
            case And:
            default:
                return "&&";
        }
    }

    static String binary(Cf.Binary binary) {
        String spelled = spelling(binary.getMarkers());
        if (spelled != null) {
            return spelled;
        }
        switch (binary.getOperator()) {
            case Concat:
                return "&";
            case Power:
                return "^";
            case IntegerDivision:
                return "\\";
            case Contains:
                return "CONTAINS";
            case DoesNotContain:
                return "DOES NOT CONTAIN";
            case Equivalence:
                return "EQV";
            case Implication:
                return "IMP";
            case Xor:
                return "XOR";
            case Elvis:
                return "?:";
            case Identical:
                return "===";
            case NotIdentical:
            default:
                return "!==";
        }
    }

    static String unary(J.Unary.Type type) {
        switch (type) {
            case PreIncrement:
            case PostIncrement:
                return "++";
            case PreDecrement:
            case PostDecrement:
                return "--";
            case Positive:
                return "+";
            case Negative:
                return "-";
            case Complement:
                return "~";
            case Not:
            default:
                return "!";
        }
    }

    static String unary(J.Unary unary) {
        String spelled = spelling(unary.getMarkers());
        return spelled != null ? spelled : unary(unary.getOperator());
    }

    static String assignment(J.AssignmentOperation.Type type) {
        switch (type) {
            case Addition:
                return "+=";
            case Subtraction:
                return "-=";
            case Multiplication:
                return "*=";
            case Division:
                return "/=";
            case Modulo:
                return "%=";
            case BitAnd:
                return "&=";
            case BitOr:
                return "|=";
            case BitXor:
                return "^=";
            case LeftShift:
                return "<<=";
            case RightShift:
                return ">>=";
            case UnsignedRightShift:
                return ">>>=";
            case Exponentiation:
                return "**=";
            case FloorDivision:
                return "//=";
            case MatrixMultiplication:
            default:
                return "@=";
        }
    }

    /**
     * Whether an operator is a word, as {@code EQ} or {@code CONTAINS}, which needs a space on each side.
     */
    static boolean isWord(String operator) {
        return !operator.isEmpty() && Tokens.isWordChar(operator.charAt(0));
    }

    private static @Nullable String spelling(Markers markers) {
        return markers.findFirst(OperatorSpelling.class).map(OperatorSpelling::getText).orElse(null);
    }
}
