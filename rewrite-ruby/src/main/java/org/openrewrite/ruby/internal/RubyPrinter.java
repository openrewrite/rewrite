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
package org.openrewrite.ruby.internal;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.ruby.RubyVisitor;
import org.openrewrite.ruby.marker.*;
import org.openrewrite.ruby.tree.Ruby;
import org.openrewrite.ruby.tree.RubySpace;

import java.util.List;
import java.util.function.UnaryOperator;

public class RubyPrinter<P> extends RubyVisitor<PrintOutputCapture<P>> {
    private static final UnaryOperator<String> MARKER_WRAPPER =
            out -> "~~" + out + (out.isEmpty() ? "" : "~~") + ">";

    private final RubyJavaPrinter delegate = new RubyJavaPrinter();

    @Override
    public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
        if (!(tree instanceof Ruby)) {
            // re-route printing to the java printer
            return delegate.visit(tree, p);
        } else {
            return super.visit(tree, p);
        }
    }

    public Ruby visitCompilationUnit(Ruby.CompilationUnit compilationUnit, PrintOutputCapture<P> p) {
        visitSpace(compilationUnit.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p);
        visitMarkers(compilationUnit.getMarkers(), p);
        visit(compilationUnit.getBodyNode(), p);
        visitSpace(compilationUnit.getEof(), Space.Location.COMPILATION_UNIT_EOF, p);
        return compilationUnit;
    }

    @Override
    public J visitBinary(J.Binary binary, PrintOutputCapture<P> p) {
        String keyword = "";
        switch (binary.getOperator()) {
            case Addition:
                keyword = "+";
                break;
            case Subtraction:
                keyword = "-";
                break;
            case Multiplication:
                keyword = "*";
                break;
            case Division:
                keyword = "/";
                break;
            case Modulo:
                keyword = "%";
                break;
            case LessThan:
                keyword = "<";
                break;
            case GreaterThan:
                keyword = ">";
                break;
            case LessThanOrEqual:
                keyword = "<=";
                break;
            case GreaterThanOrEqual:
                keyword = ">=";
                break;
            case Equal:
                keyword = "==";
                break;
            case NotEqual:
                keyword = "!=";
                break;
            case BitAnd:
                keyword = "&";
                break;
            case BitOr:
                keyword = "|";
                break;
            case BitXor:
                keyword = "^";
                break;
            case LeftShift:
                keyword = "<<";
                break;
            case RightShift:
                keyword = ">>";
                break;
            case UnsignedRightShift:
                keyword = ">>>";
                break;
            case Or:
                keyword = "||";
                if (binary.getMarkers().findFirst(EnglishOperator.class).isPresent()) {
                    keyword = "or";
                }
                break;
            case And:
                keyword = "&&";
                if (binary.getMarkers().findFirst(EnglishOperator.class).isPresent()) {
                    keyword = "and";
                }
                break;
        }
        beforeSyntax(binary, Space.Location.BINARY_PREFIX, p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), Space.Location.BINARY_OPERATOR, p);
        p.append(keyword);
        visit(binary.getRight(), p);
        afterSyntax(binary, p);
        return binary;
    }

    @Override
    public J visitBinary(Ruby.Binary binary, PrintOutputCapture<P> p) {
        String keyword = "";
        switch (binary.getOperator()) {
            case Comparison:
                keyword = "<=>";
                break;
            case Exponentiation:
                keyword = "**";
                break;
            case OnesComplement:
                keyword = "~";
                break;
            case RangeInclusive:
                keyword = "..";
                break;
            case RangeExclusive:
                keyword = "...";
                break;
            case Within:
                keyword = "===";
                break;
        }
        beforeSyntax(binary, RubySpace.Location.BINARY_PREFIX, p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), RubySpace.Location.BINARY_OPERATOR, p);
        p.append(keyword);
        visit(binary.getRight(), p);
        afterSyntax(binary, p);
        return binary;
    }

    protected void beforeSyntax(J j, @SuppressWarnings("unused") RubySpace.Location loc,
                                PrintOutputCapture<P> p) {
        beforeSyntax(j.getPrefix(), j.getMarkers(), Space.Location.LANGUAGE_EXTENSION, p);
    }

    protected void beforeSyntax(J j, Space.Location loc, PrintOutputCapture<P> p) {
        beforeSyntax(j.getPrefix(), j.getMarkers(), loc, p);
    }

    protected void beforeSyntax(Space prefix, Markers markers, Space.Location loc, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), MARKER_WRAPPER));
        }
        visitSpace(prefix, loc, p);
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), MARKER_WRAPPER));
        }
    }

    protected void afterSyntax(J t, PrintOutputCapture<P> p) {
        afterSyntax(t.getMarkers(), p);
    }

    protected void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), MARKER_WRAPPER));
        }
    }

    public Space visitSpace(Space space, RubySpace.Location loc, PrintOutputCapture<P> p) {
        return delegate.visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
        return delegate.visitSpace(space, loc, p);
    }

    private class RubyJavaPrinter extends JavaPrinter<P> {
        @Override
        public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
            if (tree instanceof Ruby) {
                // re-route printing back up to groovy
                return RubyPrinter.this.visit(tree, p);
            } else {
                return super.visit(tree, p);
            }
        }

        @Override
        protected void visitStatement(@Nullable JRightPadded<Statement> paddedStat, JRightPadded.Location location, PrintOutputCapture<P> p) {
            if (paddedStat != null) {
                visit(paddedStat.getElement(), p);
                visitSpace(paddedStat.getAfter(), location.getAfterLocation(), p);
                visitMarkers(paddedStat.getMarkers(), p);
            }
        }

        @Override
        public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
            if (marker instanceof org.openrewrite.java.marker.Semicolon) {
                p.append(';');
            }
            return super.visitMarker(marker, p);
        }

        @Override
        public J visitAssignmentOperation(J.AssignmentOperation assignOp, PrintOutputCapture<P> p) {
            String keyword = "";
            switch (assignOp.getOperator()) {
                case Addition:
                    keyword = "+=";
                    break;
                case Subtraction:
                    keyword = "-=";
                    break;
                case Multiplication:
                    keyword = "*=";
                    break;
                case Division:
                    keyword = "/=";
                    break;
                case Modulo:
                    keyword = "%=";
                    break;
                case Exponentiation:
                    keyword = "**=";
                    break;
            }
            beforeSyntax(assignOp, Space.Location.ASSIGNMENT_OPERATION_PREFIX, p);
            visit(assignOp.getVariable(), p);
            visitSpace(assignOp.getPadding().getOperator().getBefore(), Space.Location.ASSIGNMENT_OPERATION_OPERATOR, p);
            p.append(keyword);
            visit(assignOp.getAssignment(), p);
            afterSyntax(assignOp, p);
            return assignOp;
        }

        @Override
        public <T extends J> J visitControlParentheses(J.ControlParentheses<T> controlParens, PrintOutputCapture<P> p) {
            visitSpace(controlParens.getPrefix(), Space.Location.CONTROL_PARENTHESES_PREFIX, p);
            visitRightPadded(controlParens.getPadding().getTree(), JRightPadded.Location.PARENTHESES, p);
            if (controlParens.getMarkers().findFirst(ExplicitThen.class).isPresent()) {
                p.append("then");
            } else if (controlParens.getMarkers().findFirst(ExplicitDo.class).isPresent()) {
                p.append("do");
            }
            afterSyntax(controlParens, p);
            return controlParens;
        }

        @Override
        public J visitElse(J.If.Else anElse, PrintOutputCapture<P> p) {
            beforeSyntax(anElse, Space.Location.ELSE_PREFIX, p);
            if (anElse.getBody() instanceof J.If) {
                p.append("els"); // the nested `J.If` will print the remaining `if`
            } else {
                p.append("else");
            }
            visitStatement(anElse.getPadding().getBody(), JRightPadded.Location.IF_ELSE, p);
            if (!(anElse.getBody() instanceof J.If)) {
                p.append("end");
            }
            afterSyntax(anElse, p);
            return anElse;
        }

        @Override
        public J visitIf(J.If iff, PrintOutputCapture<P> p) {
            beforeSyntax(iff, Space.Location.IF_PREFIX, p);
            p.append("if");
            visit(iff.getIfCondition(), p);
            visitStatement(iff.getPadding().getThenPart(), JRightPadded.Location.IF_THEN, p);
            if (iff.getElsePart() == null) {
                p.append("end");
            } else {
                visit(iff.getElsePart(), p);
            }
            afterSyntax(iff, p);
            return iff;
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<P> p) {
            beforeSyntax(method, Space.Location.METHOD_INVOCATION_PREFIX, p);

            visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, p);
            visit(method.getName(), p);
            JContainer<Expression> argContainer = method.getPadding().getArguments();

            visitSpace(argContainer.getBefore(), Space.Location.METHOD_INVOCATION_ARGUMENTS, p);
            List<JRightPadded<Expression>> args = argContainer.getPadding().getElements();
            for (int i = 0; i < args.size(); i++) {
                JRightPadded<Expression> arg = args.get(i);
                boolean omitParens = arg.getElement().getMarkers()
                                             .findFirst(OmitParentheses.class)
                                             .isPresent() ||
                                     arg.getElement().getMarkers()
                                             .findFirst(org.openrewrite.java.marker.OmitParentheses.class)
                                             .isPresent();

                if (i == 0 && !omitParens) {
                    p.append('(');
                } else if (i > 0 && omitParens && (
                        !args.get(0).getElement().getMarkers().findFirst(OmitParentheses.class).isPresent() &&
                        !args.get(0).getElement().getMarkers().findFirst(org.openrewrite.java.marker.OmitParentheses.class).isPresent()
                )) {
                    p.append(')');
                } else if (i > 0) {
                    p.append(',');
                }

                visitRightPadded(arg, JRightPadded.Location.METHOD_INVOCATION_ARGUMENT, p);

                if (i == args.size() - 1 && !omitParens) {
                    p.append(')');
                }
            }

            afterSyntax(method, p);
            return method;
        }

        @Override
        public J visitUnary(J.Unary unary, PrintOutputCapture<P> p) {
            beforeSyntax(unary, Space.Location.UNARY_PREFIX, p);
            switch (unary.getOperator()) {
                case PreIncrement:
                    p.append("++");
                    visit(unary.getExpression(), p);
                    break;
                case PreDecrement:
                    p.append("--");
                    visit(unary.getExpression(), p);
                    break;
                case PostIncrement:
                    visit(unary.getExpression(), p);
                    visitSpace(unary.getPadding().getOperator().getBefore(), Space.Location.UNARY_OPERATOR, p);
                    p.append("++");
                    break;
                case PostDecrement:
                    visit(unary.getExpression(), p);
                    visitSpace(unary.getPadding().getOperator().getBefore(), Space.Location.UNARY_OPERATOR, p);
                    p.append("--");
                    break;
                case Positive:
                    p.append('+');
                    visit(unary.getExpression(), p);
                    break;
                case Negative:
                    p.append('-');
                    visit(unary.getExpression(), p);
                    break;
                case Complement:
                    p.append('~');
                    visit(unary.getExpression(), p);
                    break;
                case Not:
                default:
                    if (unary.getMarkers().findFirst(EnglishOperator.class).isPresent()) {
                        p.append("not");
                    } else {
                        p.append('!');
                    }
                    visit(unary.getExpression(), p);
            }
            afterSyntax(unary, p);
            return unary;
        }

        @Override
        public J visitWhileLoop(J.WhileLoop whileLoop, PrintOutputCapture<P> p) {
            beforeSyntax(whileLoop, Space.Location.WHILE_PREFIX, p);

            boolean until = whileLoop.getMarkers().findFirst(Until.class).isPresent();
            if (whileLoop.getMarkers().findFirst(WhileModifier.class).isPresent()) {
                visitStatement(whileLoop.getPadding().getBody(), JRightPadded.Location.WHILE_BODY, p);
                p.append(until ? "until" : "while");
                visit(whileLoop.getCondition(), p);
            } else {
                p.append(until ? "until" : "while");
                visit(whileLoop.getCondition(), p);
                visitStatement(whileLoop.getPadding().getBody(), JRightPadded.Location.WHILE_BODY, p);
                p.append("end");
            }

            afterSyntax(whileLoop, p);
            return whileLoop;
        }
    }
}
