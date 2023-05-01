/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.UnwrapParentheses;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.java.style.UnnecessaryParenthesesStyle;
import org.openrewrite.java.tree.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class UnnecessaryParentheses extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove unnecessary parentheses";
    }

    @Override
    public String getDescription() {
        return "Removes unnecessary parentheses from code where extra parentheses pairs are redundant.";
    }

    @Override
    public Set<String> getTags() {
        return new LinkedHashSet<>(Arrays.asList("RSPEC-1110", "RSPEC-1611"));
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        //noinspection NotNullFieldNotInitialized
        return new JavaVisitor<ExecutionContext>() {
            private static final String UNNECESSARY_PARENTHESES_MESSAGE = "unnecessaryParenthesesUnwrapTarget";

            UnnecessaryParenthesesStyle style;

            @Override
            public J preVisit(J tree, ExecutionContext executionContext) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                    //noinspection DataFlowIssue
                    style = ((SourceFile) cu).getStyle(UnnecessaryParenthesesStyle.class);
                    if (style == null) {
                        style = Checkstyle.unnecessaryParentheses();
                    }
                }
                return tree;
            }

            @Override
            public <T extends J> J visitParentheses(J.Parentheses<T> parens, ExecutionContext ctx) {
                J par = super.visitParentheses(parens, ctx);
                Cursor c = getCursor().pollNearestMessage(UNNECESSARY_PARENTHESES_MESSAGE);
                if (c != null && (c.getValue() instanceof J.Literal || c.getValue() instanceof J.Identifier)) {
                    par = new UnwrapParentheses<>((J.Parentheses<?>) par).visit(par, ctx, getCursor().getParentOrThrow());
                }

                assert par != null;
                if (par instanceof J.Parentheses) {
                    if (getCursor().getParentTreeCursor().getValue() instanceof J.Parentheses) {
                        return ((J.Parentheses<?>) par).getTree().withPrefix(Space.EMPTY);
                    }
                }
                return par;
            }

            @Override
            public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
                J.Identifier i = (J.Identifier) super.visitIdentifier(ident, ctx);
                if (style.getIdent() && getCursor().getParentTreeCursor().getValue() instanceof J.Parentheses) {
                    getCursor().putMessageOnFirstEnclosing(J.Parentheses.class, UNNECESSARY_PARENTHESES_MESSAGE, getCursor());
                }
                return i;
            }

            @Override
            public J visitLiteral(J.Literal literal, ExecutionContext ctx) {
                J.Literal l = (J.Literal) super.visitLiteral(literal, ctx);
                JavaType.Primitive type = l.getType();
                if ((style.getNumInt() && type == JavaType.Primitive.Int) ||
                    (style.getNumDouble() && type == JavaType.Primitive.Double) ||
                    (style.getNumLong() && type == JavaType.Primitive.Long) ||
                    (style.getNumFloat() && type == JavaType.Primitive.Float) ||
                    (style.getStringLiteral() && type == JavaType.Primitive.String) ||
                    (style.getLiteralNull() && type == JavaType.Primitive.Null) ||
                    (style.getLiteralFalse() && type == JavaType.Primitive.Boolean && l.getValue() == Boolean.valueOf(false)) ||
                    (style.getLiteralTrue() && type == JavaType.Primitive.Boolean && l.getValue() == Boolean.valueOf(true))) {
                    if (getCursor().getParentTreeCursor().getValue() instanceof J.Parentheses) {
                        getCursor().putMessageOnFirstEnclosing(J.Parentheses.class, UNNECESSARY_PARENTHESES_MESSAGE, getCursor());
                    }
                }
                return l;
            }

            @Override
            public J visitAssignmentOperation(J.AssignmentOperation assignOp, ExecutionContext ctx) {
                J.AssignmentOperation a = (J.AssignmentOperation) super.visitAssignmentOperation(assignOp, ctx);
                J.AssignmentOperation.Type op = a.getOperator();
                if (a.getAssignment() instanceof J.Parentheses && ((style.getBitAndAssign() && op == J.AssignmentOperation.Type.BitAnd) ||
                                                                   (style.getBitOrAssign() && op == J.AssignmentOperation.Type.BitOr) ||
                                                                   (style.getBitShiftRightAssign() && op == J.AssignmentOperation.Type.UnsignedRightShift) ||
                                                                   (style.getBitXorAssign() && op == J.AssignmentOperation.Type.BitXor) ||
                                                                   (style.getShiftRightAssign() && op == J.AssignmentOperation.Type.RightShift) ||
                                                                   (style.getShiftLeftAssign() && op == J.AssignmentOperation.Type.LeftShift) ||
                                                                   (style.getMinusAssign() && op == J.AssignmentOperation.Type.Subtraction) ||
                                                                   (style.getDivAssign() && op == J.AssignmentOperation.Type.Division) ||
                                                                   (style.getPlusAssign() && op == J.AssignmentOperation.Type.Addition) ||
                                                                   (style.getStarAssign() && op == J.AssignmentOperation.Type.Multiplication) ||
                                                                   (style.getModAssign() && op == J.AssignmentOperation.Type.Modulo))) {
                    a = (J.AssignmentOperation) new UnwrapParentheses<>((J.Parentheses<?>) a.getAssignment()).visitNonNull(a, ctx, getCursor().getParentOrThrow());
                }
                return a;
            }

            @Override
            public J visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                J.Assignment a = visitAndCast(assignment, ctx, super::visitAssignment);
                if (style.getAssign() && a.getAssignment() instanceof J.Parentheses) {
                    a = (J.Assignment) new UnwrapParentheses<>((J.Parentheses<?>) a.getAssignment()).visitNonNull(a, ctx, getCursor().getParentOrThrow());
                }
                return a;
            }

            @Override
            public J visitReturn(J.Return retrn, ExecutionContext ctx) {
                J.Return rtn = (J.Return) super.visitReturn(retrn, ctx);
                if (style.getExpr() && rtn.getExpression() instanceof J.Parentheses) {
                    rtn = (J.Return) new UnwrapParentheses<>((J.Parentheses<?>) rtn.getExpression()).visitNonNull(rtn, ctx, getCursor().getParentOrThrow());
                }
                return rtn;
            }

            @Override
            public J visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                J.VariableDeclarations.NamedVariable v = (J.VariableDeclarations.NamedVariable) super.visitVariable(variable, ctx);
                if (style.getAssign() && v.getInitializer() != null && v.getInitializer() instanceof J.Parentheses) {
                    v = (J.VariableDeclarations.NamedVariable) new UnwrapParentheses<>((J.Parentheses<?>) v.getInitializer()).visitNonNull(v, ctx, getCursor().getParentOrThrow());
                }
                return v;
            }

            @Override
            public J visitLambda(J.Lambda lambda, ExecutionContext ctx) {
                J.Lambda l = (J.Lambda) super.visitLambda(lambda, ctx);
                if (l.getParameters().getParameters().size() == 1 &&
                    l.getParameters().isParenthesized() &&
                    l.getParameters().getParameters().get(0) instanceof J.VariableDeclarations &&
                    ((J.VariableDeclarations) l.getParameters().getParameters().get(0)).getTypeExpression() == null) {
                    l = l.withParameters(l.getParameters().withParenthesized(false));
                }
                return l;
            }

            @Override
            public J visitIf(J.If iff, ExecutionContext ctx) {
                J.If i = (J.If) super.visitIf(iff, ctx);
                // Unwrap when if condition is a single parenthesized expression
                Expression expression = i.getIfCondition().getTree();
                if (expression instanceof J.Parentheses) {
                    i = (J.If) new UnwrapParentheses<>((J.Parentheses<?>) expression).visitNonNull(i, ctx, getCursor().getParentOrThrow());
                }
                return i;
            }

            @Override
            public J visitWhileLoop(J.WhileLoop whileLoop, ExecutionContext ctx) {
                J.WhileLoop w = (J.WhileLoop) super.visitWhileLoop(whileLoop, ctx);
                // Unwrap when while condition is a single parenthesized expression
                Expression expression = w.getCondition().getTree();
                if (expression instanceof J.Parentheses) {
                    w = (J.WhileLoop) new UnwrapParentheses<>((J.Parentheses<?>) expression).visitNonNull(w, ctx, getCursor().getParentOrThrow());
                }
                return w;
            }

            @Override
            public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, ExecutionContext ctx) {
                J.DoWhileLoop dw = (J.DoWhileLoop) super.visitDoWhileLoop(doWhileLoop, ctx);
                // Unwrap when while condition is a single parenthesized expression
                Expression expression = dw.getWhileCondition().getTree();
                if (expression instanceof J.Parentheses) {
                    dw = (J.DoWhileLoop) new UnwrapParentheses<>((J.Parentheses<?>) expression).visitNonNull(dw, ctx, getCursor().getParentOrThrow());
                }
                return dw;
            }

            @Override
            public J visitForControl(J.ForLoop.Control control, ExecutionContext ctx) {
                J.ForLoop.Control fc = (J.ForLoop.Control) super.visitForControl(control, ctx);
                Expression condition = fc.getCondition();
                if (condition instanceof J.Parentheses) {
                    fc = (J.ForLoop.Control) new UnwrapParentheses<>((J.Parentheses<?>) condition).visitNonNull(fc, ctx, getCursor().getParentOrThrow());
                }
                return fc;
            }
        };
    }

    @Override
    public boolean causesAnotherCycle() {
        return true;
    }
}
