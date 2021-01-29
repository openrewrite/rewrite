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

import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.UnwrapParentheses;
import org.openrewrite.java.style.UnnecessaryParenthesesStyle;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

public class UnnecessaryParenthesesVisitor<P> extends JavaVisitor<P> {

    private final UnnecessaryParenthesesStyle style;

    public UnnecessaryParenthesesVisitor(UnnecessaryParenthesesStyle style) {
        this.style = style;
        setCursoringOn();
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parens, P p) {
        J par = super.visitParentheses(parens, p);
        if (style.isIdent() && ((J.Parentheses<T>) par).getTree().getElem() instanceof J.Ident) {
            par = new UnwrapParentheses<>((J.Parentheses<?>) par).visit(par, p, getCursor());
        }
        return par;
    }

    @Override
    public J visitLiteral(J.Literal literal, P p) {
        J.Literal l = visitAndCast(literal, p, super::visitLiteral);
        JavaType.Primitive type = l.getType();
        if ((style.isNumInt() && type == JavaType.Primitive.Int) ||
                (style.isNumDouble() && type == JavaType.Primitive.Double) ||
                (style.isNumLong() && type == JavaType.Primitive.Long) ||
                (style.isNumFloat() && type == JavaType.Primitive.Float) ||
                (style.isStringLiteral() && type == JavaType.Primitive.String) ||
                (style.isLiteralFalse() && type == JavaType.Primitive.Boolean && l.getValue() == Boolean.valueOf(false)) ||
                (style.isLiteralTrue() && type == JavaType.Primitive.Boolean && l.getValue() == Boolean.valueOf(true))) {
//            l = (J.Literal) new UnwrapParentheses<>((J.Parentheses<?>) l).visit(l, p, getCursor()); // TODO
        }
        return l;
    }

    @Override
    public J visitAssignOp(J.AssignOp assignOp, P p) {
        J.AssignOp a = visitAndCast(assignOp, p, super::visitAssignOp);

        Expression assignment = a.getAssignment();
        J.AssignOp.Type op = a.getOperator().getElem();
        if (assignment instanceof J.Parentheses && ((style.isBandAssign() && op == J.AssignOp.Type.BitAnd) ||
                (style.isBorAssign() && op == J.AssignOp.Type.BitOr) ||
                (style.isBsrAssign() && op == J.AssignOp.Type.UnsignedRightShift) ||
                (style.isBxorAssign() && op == J.AssignOp.Type.BitXor) ||
                (style.isSrAssign() && op == J.AssignOp.Type.RightShift) ||
                (style.isSlAssign() && op == J.AssignOp.Type.LeftShift) ||
                (style.isMinusAssign() && op == J.AssignOp.Type.Subtraction) ||
                (style.isDivAssign() && op == J.AssignOp.Type.Division) ||
                (style.isPlusAssign() && op == J.AssignOp.Type.Addition) ||
                (style.isStarAssign() && op == J.AssignOp.Type.Multiplication) ||
                (style.isModAssign() && op == J.AssignOp.Type.Modulo))) {
            a = (J.AssignOp) new UnwrapParentheses<>((J.Parentheses<?>) a.getAssignment()).visit(a, p, getCursor());
        }
        return a;
    }

    @Override
    public J visitAssign(J.Assign assign, P p) {
        J.Assign a = visitAndCast(assign, p, super::visitAssign);
        if (style.isAssign()) {
            a = (J.Assign) new UnwrapParentheses<>((J.Parentheses<?>) a.getAssignment().getElem()).visit(a, p, getCursor());
        }
        return a;
    }

    @Override
    public J visitVariable(J.VariableDecls.NamedVar variable, P p) {
        J.VariableDecls.NamedVar v = visitAndCast(variable, p, super::visitVariable);
        if (style.isAssign() && v.getInitializer() != null && v.getInitializer().getElem() instanceof J.Parentheses) {
            v = (J.VariableDecls.NamedVar) new UnwrapParentheses<>((J.Parentheses<?>) v.getInitializer().getElem()).visit(v, p, getCursor());
        }
        return v;
    }

    @Override
    public J visitLambda(J.Lambda lambda, P p) {
        J.Lambda l = visitAndCast(lambda, p, super::visitLambda);
        if (l.getParameters().getParams().size() == 1 &&
                l.getParameters().isParenthesized() &&
                l.getParameters().getParams().get(0).getElem() instanceof J.VariableDecls &&
                ((J.VariableDecls) l.getParameters().getParams().get(0).getElem()).getTypeExpr() == null) {
            l = l.withParameters(l.getParameters().withParenthesized(false));
        }
        return l;
    }

}


