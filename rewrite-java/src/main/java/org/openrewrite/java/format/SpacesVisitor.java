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
package org.openrewrite.java.format;

import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.java.tree.*;

public class SpacesVisitor<P> extends JavaIsoVisitor<P> {

    /*
    TODO Finish support for SpacesStyle properties, from SpacesStyle.Within.groupingParentheses down
     */

    private final SpacesStyle style;

    public SpacesVisitor(SpacesStyle style) {
        this.style = style;
        setCursoringOn();
    }

    <T extends J> T spaceBefore(T j, boolean spaceBefore) {
        if (spaceBefore && StringUtils.isNullOrEmpty(j.getPrefix().getWhitespace())) {
            return j.withPrefix(j.getPrefix().withWhitespace(" "));
        } else if (!spaceBefore && j.getPrefix().getWhitespace().equals(" ")) {
            return j.withPrefix(j.getPrefix().withWhitespace(""));
        } else {
            return j;
        }
    }

    <T> JContainer<T> spaceBefore(JContainer<T> container, boolean spaceBefore) {
        if (spaceBefore && StringUtils.isNullOrEmpty(container.getBefore().getWhitespace())) {
            return container.withBefore(container.getBefore().withWhitespace(" "));
        } else if (!spaceBefore && container.getBefore().getWhitespace().equals(" ")) {
            return container.withBefore(container.getBefore().withWhitespace(""));
        } else {
            return container;
        }
    }

    <T extends J> JLeftPadded<T> spaceBefore(JLeftPadded<T> container, boolean spaceBefore) {
        if (spaceBefore && StringUtils.isNullOrEmpty(container.getBefore().getWhitespace())) {
            return container.withBefore(container.getBefore().withWhitespace(" "));
        } else if (!spaceBefore && container.getBefore().getWhitespace().equals(" ")) {
            return container.withBefore(container.getBefore().withWhitespace(""));
        } else {
            return container;
        }
    }

    <T extends J> JLeftPadded<T> spaceBeforeLeftPaddedElement(JLeftPadded<T> container, boolean spaceBefore) {
        return container.withElem(spaceBefore(container.getElem(), spaceBefore));
    }

    <T extends J> JRightPadded<T> spaceBeforeRightPaddedElement(JRightPadded<T> container, boolean spaceBefore) {
        return container.withElem(spaceBefore(container.getElem(), spaceBefore));
    }

    <T extends J> JRightPadded<T> spaceAfter(JRightPadded<T> container, boolean spaceAfter) {
        if (spaceAfter && StringUtils.isNullOrEmpty(container.getAfter().getWhitespace())) {
            return container.withAfter(container.getAfter().withWhitespace(" "));
        } else if (!spaceAfter && container.getAfter().getWhitespace().equals(" ")) {
            return container.withAfter(container.getAfter().withWhitespace(""));
        } else {
            return container;
        }
    }

    @Override
    public J.ClassDecl visitClassDecl(J.ClassDecl classDecl, P p) {
        J.ClassDecl c = super.visitClassDecl(classDecl, p);
        c = c.withBody(spaceBefore(c.getBody(), style.getBeforeLeftBrace().isClassLeftBrace()));
        boolean withinCodeBraces = style.getWithin().isCodeBraces();
        if (withinCodeBraces && StringUtils.isNullOrEmpty(c.getBody().getEnd().getWhitespace())) {
            c = c.withBody(
                    c.getBody().withEnd(
                            c.getBody().getEnd().withWhitespace(" ")
                    )
            );
        } else if (!withinCodeBraces && c.getBody().getEnd().getWhitespace().equals(" ")) {
            c = c.withBody(
                    c.getBody().withEnd(
                            c.getBody().getEnd().withWhitespace("")
                    )
            );
        }
        return c;
    }

    @Override
    public J.MethodDecl visitMethod(J.MethodDecl method, P p) {
        J.MethodDecl m = super.visitMethod(method, p);
        m = m.getPadding().withParams(
                spaceBefore(m.getPadding().getParams(), style.getBeforeParentheses().isMethodDeclaration()));
        if (m.getBody() != null) {
            m = m.withBody(spaceBefore(m.getBody(), style.getBeforeLeftBrace().isMethodLeftBrace()));
        }
        Statement firstParam = m.getParams().iterator().next();
        if (firstParam instanceof J.Empty) {
            boolean useSpace = style.getWithin().isEmptyMethodDeclarationParentheses();
            m = m.getPadding().withParams(
                    m.getPadding().getParams().getPadding().withElems(
                            ListUtils.map(m.getPadding().getParams().getPadding().getElems(),
                                    param -> param.withElem(spaceBefore(param.getElem(), useSpace))
                            )
                    )
            );
        } else {
            final int paramsSize = m.getParams().size();
            boolean useSpace = style.getWithin().isMethodDeclarationParentheses();
            m = m.getPadding().withParams(
                    m.getPadding().getParams().getPadding().withElems(
                            ListUtils.map(m.getPadding().getParams().getPadding().getElems(),
                                    (index, param) -> {
                                        if (index == 0) {
                                            param = param.withElem(spaceBefore(param.getElem(), useSpace));
                                        }
                                        if (index == paramsSize - 1) {
                                            param = spaceAfter(param, useSpace);
                                        }
                                        return param;
                                    }
                            )
                    )
            );
        }
        return m;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation m = super.visitMethodInvocation(method, p);
        m = m.getPadding().withArgs(spaceBefore(m.getPadding().getArgs(), style.getBeforeParentheses().isMethodCall()));
        Expression firstArg = m.getArgs().iterator().next();
        if (firstArg instanceof J.Empty) {
            boolean useSpace = style.getWithin().isEmptyMethodCallParentheses();
            m = m.getPadding().withArgs(
                    m.getPadding().getArgs().getPadding().withElems(
                            ListUtils.map(m.getPadding().getArgs().getPadding().getElems(),
                                    arg -> arg.withElem(spaceBefore(arg.getElem(), useSpace))
                            )
                    )
            );
        } else {
            final int argsSize = m.getArgs().size();
            boolean useSpace = style.getWithin().isMethodCallParentheses();
            m = m.getPadding().withArgs(
                    m.getPadding().getArgs().getPadding().withElems(
                            ListUtils.map(m.getPadding().getArgs().getPadding().getElems(),
                                    (index, arg) -> {
                                        if (index == 0) {
                                            arg = arg.withElem(spaceBefore(arg.getElem(), useSpace));
                                        }
                                        if (index == argsSize - 1) {
                                            arg = spaceAfter(arg, useSpace);
                                        }
                                        return arg;
                                    }
                            )
                    )
            );
        }
        return m;
    }

    @Override
    public J.If visitIf(J.If iff, P p) {
        J.If i = super.visitIf(iff, p);
        i = i.withIfCondition(spaceBefore(i.getIfCondition(), style.getBeforeParentheses().isIfParentheses()));
        i = i.getPadding().withThenPart(spaceBeforeRightPaddedElement(i.getPadding().getThenPart(), style.getBeforeLeftBrace().isIfLeftBrace()));
        boolean useSpaceWithinIfParentheses = style.getWithin().isIfParentheses();
        i = i.withIfCondition(
                i.getIfCondition().getPadding().withTree(
                        spaceAfter(
                                i.getIfCondition().getPadding().getTree().withElem(
                                        spaceBefore(i.getIfCondition().getPadding().getTree().getElem(), useSpaceWithinIfParentheses
                                        )
                                ),
                                useSpaceWithinIfParentheses
                        )
                )
        );
        return i;
    }

    @Override
    public J.If.Else visitElse(J.If.Else elze, P p) {
        J.If.Else e = super.visitElse(elze, p);
        e = e.getPadding().withBody(spaceBeforeRightPaddedElement(e.getPadding().getBody(), style.getBeforeLeftBrace().isElseLeftBrace()));
        e = spaceBefore(e, style.getBeforeKeywords().isElseKeyword());
        return e;
    }

    @Override
    public J.ForLoop visitForLoop(J.ForLoop forLoop, P p) {
        J.ForLoop f = super.visitForLoop(forLoop, p);
        f = f.withControl(spaceBefore(f.getControl(), style.getBeforeParentheses().isForParentheses()));
        f = f.getPadding().withBody(spaceBeforeRightPaddedElement(f.getPadding().getBody(), style.getBeforeLeftBrace().isForLeftBrace()));
        boolean spaceWithinForParens = style.getWithin().isForParentheses();
        f = f.withControl(
                f.getControl().withInit(
                        spaceBefore(f.getControl().getInit(), spaceWithinForParens)
                )
        );
        f = f.withControl(
                f.getControl().getPadding().withUpdate(
                        ListUtils.mapLast(f.getControl().getPadding().getUpdate(),
                                stmt -> spaceAfter(stmt, spaceWithinForParens)
                        )
                )
        );

        return f;
    }

    @Override
    public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, P p) {
        J.ForEachLoop f = super.visitForEachLoop(forLoop, p);
        f = f.withControl(spaceBefore(f.getControl(), style.getBeforeParentheses().isForParentheses()));
        f = f.getPadding().withBody(spaceBeforeRightPaddedElement(f.getPadding().getBody(), style.getBeforeLeftBrace().isForLeftBrace()));
        boolean spaceWithinForParens = style.getWithin().isForParentheses();
        f = f.withControl(
                f.getControl().withVariable(
                        spaceBefore(f.getControl().getVariable(), spaceWithinForParens)
                )
        );
        f = f.withControl(
                f.getControl().getPadding().withIterable(
                        spaceAfter(f.getControl().getPadding().getIterable(), spaceWithinForParens)
                )
        );
        return f;
    }

    @Override
    public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, P p) {
        J.WhileLoop w = super.visitWhileLoop(whileLoop, p);
        w = w.withCondition(spaceBefore(w.getCondition(), style.getBeforeParentheses().isWhileParentheses()));
        w = w.getPadding().withBody(spaceBeforeRightPaddedElement(w.getPadding().getBody(), style.getBeforeLeftBrace().isWhileLeftBrace()));
        boolean spaceWithinWhileParens = style.getWithin().isWhileParentheses();
        w = w.withCondition(
                w.getCondition().withTree(
                        spaceBefore(w.getCondition().getTree(), spaceWithinWhileParens)
                )
        );
        w = w.withCondition(
                w.getCondition().getPadding().withTree(
                        spaceAfter(w.getCondition().getPadding().getTree(), spaceWithinWhileParens)
                )
        );
        return w;
    }

    @Override
    public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        J.DoWhileLoop d = super.visitDoWhileLoop(doWhileLoop, p);
        d = d.getPadding().withWhileCondition(spaceBefore(d.getPadding().getWhileCondition(), style.getBeforeKeywords().isWhileKeyword()));
        d = d.getPadding().withWhileCondition(spaceBeforeLeftPaddedElement(d.getPadding().getWhileCondition(), style.getBeforeParentheses().isWhileParentheses()));
        d = d.getPadding().withBody(spaceBeforeRightPaddedElement(d.getPadding().getBody(), style.getBeforeLeftBrace().isDoLeftBrace()));
        boolean spaceWithinWhileParens = style.getWithin().isWhileParentheses();
        d = d.withWhileCondition(
                d.getWhileCondition().withTree(
                        spaceBefore(d.getWhileCondition().getTree(), spaceWithinWhileParens)
                )
        );
        d = d.withWhileCondition(
                d.getWhileCondition().getPadding().withTree(
                        spaceAfter(d.getWhileCondition().getPadding().getTree(), spaceWithinWhileParens)
                )
        );
        return d;
    }

    @Override
    public J.Switch visitSwitch(J.Switch _switch, P p) {
        J.Switch s = super.visitSwitch(_switch, p);
        s = s.withSelector(spaceBefore(s.getSelector(), style.getBeforeParentheses().isSwitchParentheses()));
        s = s.withCases(spaceBefore(s.getCases(), style.getBeforeLeftBrace().isSwitchLeftBrace()));
        boolean spaceWithinSwitchParens = style.getWithin().isSwitchParentheses();
        s = s.withSelector(
                s.getSelector().withTree(
                        spaceBefore(s.getSelector().getTree(), spaceWithinSwitchParens)
                )
        );
        s = s.withSelector(
                s.getSelector().getPadding().withTree(
                        spaceAfter(s.getSelector().getPadding().getTree(), spaceWithinSwitchParens)
                )
        );
        return s;
    }

    @Override
    public J.Try visitTry(J.Try _try, P p) {
        J.Try t = super.visitTry(_try, p);
        if (t.getPadding().getResources() != null) {
            t = t.getPadding().withResources(spaceBefore(t.getPadding().getResources(), style.getBeforeParentheses().isTryParentheses()));
        }
        t = t.withBody(spaceBefore(t.getBody(), style.getBeforeLeftBrace().isTryLeftBrace()));
        if (t.getPadding().getFinally() != null) {
            JLeftPadded<J.Block> f = spaceBefore(t.getPadding().getFinally(), style.getBeforeKeywords().isFinallyKeyword());
            f = spaceBeforeLeftPaddedElement(f, style.getBeforeLeftBrace().isFinallyLeftBrace());
            t = t.getPadding().withFinally(f);
        }
        boolean spaceWithinTryParentheses = style.getWithin().isTryParentheses();
        if (t.getResources() != null) {
            t = t.withResources(ListUtils.mapFirst(t.getResources(), res -> spaceBefore(res, spaceWithinTryParentheses)));
        }
        if (t.getPadding().getResources() != null) {
            t = t.getPadding().withResources(
                    t.getPadding().getResources().getPadding().withElems(
                            ListUtils.mapLast(t.getPadding().getResources().getPadding().getElems(),
                                    res -> spaceAfter(res, spaceWithinTryParentheses)
                            )
                    )
            );
        }
        return t;
    }

    @Override
    public J.Try.Catch visitCatch(J.Try.Catch _catch, P p) {
        J.Try.Catch c = super.visitCatch(_catch, p);
        c = spaceBefore(c, style.getBeforeKeywords().isCatchKeyword());
        c = c.withParam(spaceBefore(c.getParam(), style.getBeforeParentheses().isCatchParentheses()));
        c = c.withBody(spaceBefore(c.getBody(), style.getBeforeLeftBrace().isCatchLeftBrace()));
        boolean spaceWithinCatchParens = style.getWithin().isCatchParentheses();
        c = c.withParam(
                c.getParam().withTree(
                        spaceBefore(c.getParam().getTree(), spaceWithinCatchParens)
                )
        );
        c = c.withParam(
                c.getParam().getPadding().withTree(
                        spaceAfter(c.getParam().getPadding().getTree(), spaceWithinCatchParens)
                )
        );
        return c;
    }

    @Override
    public J.Synchronized visitSynchronized(J.Synchronized sync, P p) {
        J.Synchronized s = super.visitSynchronized(sync, p);
        s = s.withLock(spaceBefore(s.getLock(), style.getBeforeParentheses().isSynchronizedParentheses()));
        s = s.withBody(spaceBefore(s.getBody(), style.getBeforeLeftBrace().isSynchronizedLeftBrace()));
        boolean spaceWithinSynchronizedParens = style.getWithin().isSynchronizedParentheses();
        s = s.withLock(
                s.getLock().withTree(
                        spaceBefore(s.getLock().getTree(), spaceWithinSynchronizedParens)
                )
        );
        s = s.withLock(
                s.getLock().getPadding().withTree(
                        spaceAfter(s.getLock().getPadding().getTree(), spaceWithinSynchronizedParens)
                )
        );
        return s;
    }

    @Override
    public J.Annotation visitAnnotation(J.Annotation annotation, P p) {
        J.Annotation a = super.visitAnnotation(annotation, p);
        J.Annotation.Padding padding = a.getPadding();
        if (padding.getArgs() != null) {
            a = padding.withArgs(spaceBefore(padding.getArgs(),
                    style.getBeforeParentheses().isAnnotationParameters()));
            if (a.getArgs() != null) {
                a = a.withArgs(ListUtils.map(a.getArgs(), (i, arg) -> i == 0 ? arg : spaceBefore(arg, style.getWithin().isAnnotationParentheses())));
            }
        }
        return a;
    }

    @Override
    public J.Assign visitAssign(J.Assign assign, P p) {
        J.Assign a = super.visitAssign(assign, p);
        a = a.getPadding().withAssignment(spaceBefore(a.getPadding().getAssignment(), style.getAroundOperators().isAssignment()));
        a = a.getPadding().withAssignment(
                a.getPadding().getAssignment().withElem(
                        spaceBefore(a.getPadding().getAssignment().getElem(), style.getAroundOperators().isAssignment())
                )
        );
        return a;
    }

    @Override
    public J.AssignOp visitAssignOp(J.AssignOp assignOp, P p) {
        J.AssignOp a = super.visitAssignOp(assignOp, p);
        J.AssignOp.Padding padding = a.getPadding();
        JLeftPadded<J.AssignOp.Type> operator = padding.getOperator();
        String operatorBeforeWhitespace = operator.getBefore().getWhitespace();
        if (style.getAroundOperators().isAssignment() && StringUtils.isNullOrEmpty(operatorBeforeWhitespace)) {
            a = padding.withOperator(
                    operator.withBefore(
                            operator.getBefore().withWhitespace(" ")
                    )
            );
        } else if (!style.getAroundOperators().isAssignment() && operatorBeforeWhitespace.equals(" ")) {
            a = padding.withOperator(
                    operator.withBefore(
                            operator.getBefore().withWhitespace("")
                    )
            );
        }
        a = a.withAssignment(spaceBefore(a.getAssignment(), style.getAroundOperators().isAssignment()));
        return a;
    }

    @Override
    public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable, P p) {
        J.VariableDecls.NamedVar v = super.visitVariable(variable, p);
        if (v.getPadding().getInitializer() != null) {
            v = v.getPadding().withInitializer(spaceBefore(v.getPadding().getInitializer(), style.getAroundOperators().isAssignment()));
        }
        if (v.getPadding().getInitializer() != null) {
            if (v.getPadding().getInitializer().getElem() != null) {
                v = v.getPadding().withInitializer(
                        v.getPadding().getInitializer().withElem(
                                spaceBefore(v.getPadding().getInitializer().getElem(), style.getAroundOperators().isAssignment())
                        )
                );
            }
        }
        return v;
    }

    @Override
    public J.Binary visitBinary(J.Binary binary, P p) {
        J.Binary b = super.visitBinary(binary, p);
        J.Binary.Type operator = b.getOperator();
        switch (operator) {
            case And:
            case Or:
                b = applyBinarySpaceAround(b, style.getAroundOperators().isLogical());
                break;
            case Equal:
            case NotEqual:
                b = applyBinarySpaceAround(b, style.getAroundOperators().isEquality());
                break;
            case LessThan:
            case LessThanOrEqual:
            case GreaterThan:
            case GreaterThanOrEqual:
                b = applyBinarySpaceAround(b, style.getAroundOperators().isRelational());
                break;
            case BitAnd:
            case BitOr:
            case BitXor:
                b = applyBinarySpaceAround(b, style.getAroundOperators().isBitwise());
                break;
            case Addition:
            case Subtraction:
                b = applyBinarySpaceAround(b, style.getAroundOperators().isAdditive());
                break;
            case Multiplication:
            case Division:
            case Modulo:
                b = applyBinarySpaceAround(b, style.getAroundOperators().isMultiplicative());
                break;
            case LeftShift:
            case RightShift:
            case UnsignedRightShift:
                b = applyBinarySpaceAround(b, style.getAroundOperators().isShift());
                break;
        }
        return b;
    }

    private J.Binary applyBinarySpaceAround(J.Binary binary, boolean useSpaceAround) {
        J.Binary.Padding padding = binary.getPadding();
        JLeftPadded<J.Binary.Type> operator = padding.getOperator();
        if (useSpaceAround) {
            if (StringUtils.isNullOrEmpty(operator.getBefore().getWhitespace())) {
                binary = padding.withOperator(
                        operator.withBefore(
                                operator.getBefore().withWhitespace(" ")
                        )
                );
            }
            if (StringUtils.isNullOrEmpty(binary.getRight().getPrefix().getWhitespace())) {
                binary = binary.withRight(
                        binary.getRight().withPrefix(
                                binary.getRight().getPrefix().withWhitespace(" ")
                        )
                );
            }
        } else {
            if (operator.getBefore().getWhitespace().equals(" ")) {
                binary = padding.withOperator(
                        operator.withBefore(
                                operator.getBefore().withWhitespace("")
                        )
                );
            }
            if (binary.getRight().getPrefix().getWhitespace().equals(" ")) {
                binary = binary.withRight(
                        binary.getRight().withPrefix(
                                binary.getRight().getPrefix().withWhitespace("")
                        )
                );
            }
        }
        return binary;
    }

    @Override
    public J.Unary visitUnary(J.Unary unary, P p) {
        J.Unary u = super.visitUnary(unary, p);
        switch (u.getOperator()) {
            case PostIncrement:
            case PostDecrement:
                u = applyUnaryOperatorBeforeSpace(u, style.getAroundOperators().isUnary());
                break;
            case PreIncrement:
            case PreDecrement:
            case Negative:
            case Positive:
            case Not:
            case Complement:
                u = applyUnaryOperatorBeforeSpace(u, style.getAroundOperators().isUnary());
                u = applyUnaryOperatorExprSpace(u, style.getAroundOperators().isUnary());
                break;
        }
        return u;
    }

    private J.Unary applyUnaryOperatorExprSpace(J.Unary unary, boolean useAroundUnaryOperatorSpace) {
        if (useAroundUnaryOperatorSpace && StringUtils.isNullOrEmpty(unary.getExpr().getPrefix().getWhitespace())) {
            unary = unary.withExpr(
                    unary.getExpr().withPrefix(
                            unary.getExpr().getPrefix().withWhitespace(" ")
                    )
            );
        } else if (!useAroundUnaryOperatorSpace && unary.getExpr().getPrefix().getWhitespace().equals(" ")) {
            unary = unary.withExpr(
                    unary.getExpr().withPrefix(
                            unary.getExpr().getPrefix().withWhitespace("")
                    )
            );
        }
        return unary;
    }

    private J.Unary applyUnaryOperatorBeforeSpace(J.Unary u, boolean useAroundUnaryOperatorSpace) {
        J.Unary.Padding padding = u.getPadding();
        JLeftPadded<J.Unary.Type> operator = padding.getOperator();
        if (useAroundUnaryOperatorSpace && StringUtils.isNullOrEmpty(operator.getBefore().getWhitespace())) {
            u = padding.withOperator(
                    operator.withBefore(
                            operator.getBefore().withWhitespace(" ")
                    )
            );
        } else if (!useAroundUnaryOperatorSpace && operator.getBefore().getWhitespace().equals(" ")) {
            u = padding.withOperator(
                    operator.withBefore(
                            operator.getBefore().withWhitespace("")
                    )
            );
        }
        return u;
    }

    @Override
    public J.Lambda visitLambda(J.Lambda lambda, P p) {
        J.Lambda l = super.visitLambda(lambda, p);
        boolean useSpaceAroundLambdaArrow = style.getAroundOperators().isLambdaArrow();
        if (useSpaceAroundLambdaArrow && StringUtils.isNullOrEmpty(l.getArrow().getWhitespace())) {
            l = l.withArrow(
                    l.getArrow().withWhitespace(" ")
            );
        } else if (!useSpaceAroundLambdaArrow && l.getArrow().getWhitespace().equals(" ")) {
            l = l.withArrow(
                    l.getArrow().withWhitespace("")
            );
        }
        l = l.withBody(spaceBefore(l.getBody(), style.getAroundOperators().isLambdaArrow()));
        return l;
    }

    @Override
    public J.MemberReference visitMemberReference(J.MemberReference memberRef, P p) {
        J.MemberReference m = super.visitMemberReference(memberRef, p);
        m = m.getPadding().withReference(
                spaceBefore(m.getPadding().getReference(), style.getAroundOperators().isMethodReferenceDoubleColon())
        );
        m = m.getPadding().withReference(
                m.getPadding().getReference().withElem(
                        spaceBefore(m.getPadding().getReference().getElem(), style.getAroundOperators().isMethodReferenceDoubleColon())
                )
        );
        return m;
    }

    @Override
    public J.NewArray visitNewArray(J.NewArray newArray, P p) {
        J.NewArray n = super.visitNewArray(newArray, p);
        if (getCursor().getParent() != null && getCursor().getParent().firstEnclosing(J.class) instanceof J.Annotation) {
            n = spaceBefore(n, style.getBeforeLeftBrace().isAnnotationArrayInitializerLeftBrace());
        } else {
            if (n.getPadding().getInitializer() != null) {
                JContainer<Expression> initializer = spaceBefore(n.getPadding().getInitializer(), style.getBeforeLeftBrace().isArrayInitializerLeftBrace());
                n = n.getPadding().withInitializer(initializer);
            }
        }
        if (n.getPadding().getInitializer() != null) {
            JContainer<Expression> i = n.getPadding().getInitializer();
            if (!i.getElems().isEmpty()) {
                if (style.getOther().isAfterComma()) {
                    i = JContainer.withElems(i, ListUtils.map(i.getElems(), (idx, elem) -> idx == 0 ? elem : spaceBefore(elem, true)));
                }

                boolean useSpaceWithinArrayInitializerBraces = style.getWithin().isArrayInitializerBraces();
                if (useSpaceWithinArrayInitializerBraces) {
                    if (!(i.getElems().iterator().next() instanceof J.Empty)) {
                        if (StringUtils.isNullOrEmpty(i.getElems().iterator().next().getPrefix().getWhitespace())) {
                            i = i.getPadding().withElems(ListUtils.mapFirst(i.getPadding().getElems(),
                                    e -> e.withElem(e.getElem().withPrefix(e.getElem().getPrefix().withWhitespace(" ")))));
                        }
                        if (StringUtils.isNullOrEmpty(i.getPadding().getElems().get(i.getElems().size() - 1).getAfter().getWhitespace())) {
                            i = i.getPadding().withElems(ListUtils.mapLast(i.getPadding().getElems(),
                                    e -> e.withAfter(e.getAfter().withWhitespace(" "))));
                        }
                    }
                } else {
                    if (!(i.getElems().iterator().next() instanceof J.Empty)) {
                        if (i.getElems().iterator().next().getPrefix().getWhitespace().equals(" ")) {
                            i = i.getPadding().withElems(ListUtils.mapFirst(i.getPadding().getElems(),
                                    e -> e.withElem(e.getElem().withPrefix(e.getElem().getPrefix().withWhitespace("")))));
                        }
                        if (i.getPadding().getElems().get(i.getElems().size() - 1).getAfter().getWhitespace().equals(" ")) {
                            i = i.getPadding().withElems(ListUtils.mapLast(i.getPadding().getElems(),
                                    e -> e.withAfter(e.getAfter().withWhitespace(""))));
                        }
                    }
                }
                boolean useSpaceWithinEmptyArrayInitializerBraces = style.getWithin().isEmptyArrayInitializerBraces();
                if (useSpaceWithinEmptyArrayInitializerBraces) {
                    if ((i.getElems().iterator().next() instanceof J.Empty)) {
                        if (StringUtils.isNullOrEmpty(i.getElems().iterator().next().getPrefix().getWhitespace())) {
                            i = i.getPadding().withElems(ListUtils.mapFirst(i.getPadding().getElems(),
                                    e -> e.withElem(e.getElem().withPrefix(e.getElem().getPrefix().withWhitespace(" ")))));
                        }
                    }
                } else {
                    if ((i.getElems().iterator().next() instanceof J.Empty)) {
                        if (i.getElems().iterator().next().getPrefix().getWhitespace().equals(" ")) {
                            i = i.getPadding().withElems(ListUtils.mapFirst(i.getPadding().getElems(),
                                    e -> e.withElem(e.getElem().withPrefix(e.getElem().getPrefix().withWhitespace("")))));
                        }
                    }
                }
            }
            n = n.getPadding().withInitializer(i);
        }
        return n;
    }

    @Override
    public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, P p) {
        J.ArrayAccess a = super.visitArrayAccess(arrayAccess, p);
        boolean useSpaceWithinBrackets = style.getWithin().isBrackets();
        if (useSpaceWithinBrackets) {
            if (StringUtils.isNullOrEmpty(a.getDimension().getPadding().getIndex().getElem().getPrefix().getWhitespace())) {
                a = a.withDimension(
                        a.getDimension().getPadding().withIndex(
                                a.getDimension().getPadding().getIndex().withElem(
                                        a.getDimension().getPadding().getIndex().getElem().withPrefix(
                                                a.getDimension().getPadding().getIndex().getElem().getPrefix().withWhitespace(" ")
                                        )
                                )
                        )
                );
            }
            if (StringUtils.isNullOrEmpty(a.getDimension().getPadding().getIndex().getAfter().getWhitespace())) {
                a = a.withDimension(
                        a.getDimension().getPadding().withIndex(
                                a.getDimension().getPadding().getIndex().withAfter(
                                        a.getDimension().getPadding().getIndex().getAfter().withWhitespace(" ")
                                )
                        )
                );
            }
        } else {
            if (a.getDimension().getPadding().getIndex().getElem().getPrefix().getWhitespace().equals(" ")) {
                a = a.withDimension(
                        a.getDimension().getPadding().withIndex(
                                a.getDimension().getPadding().getIndex().withElem(
                                        a.getDimension().getPadding().getIndex().getElem().withPrefix(
                                                a.getDimension().getPadding().getIndex().getElem().getPrefix().withWhitespace("")
                                        )
                                )
                        )
                );
            }
            if (a.getDimension().getPadding().getIndex().getAfter().getWhitespace().equals(" ")) {
                a = a.withDimension(
                        a.getDimension().getPadding().withIndex(
                                a.getDimension().getPadding().getIndex().withAfter(
                                        a.getDimension().getPadding().getIndex().getAfter().withWhitespace("")
                                )
                        )
                );
            }
        }
        return a;
    }

    @Override
    public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens, P p) {
        J.Parentheses<T> p2 = super.visitParentheses(parens, p);
        if (style.getWithin().isGroupingParentheses()) {
            if (StringUtils.isNullOrEmpty(p2.getPadding().getTree().getElem().getPrefix().getWhitespace())) {
                p2 = p2.getPadding().withTree(
                        p2.getPadding().getTree().withElem(
                                p2.getPadding().getTree().getElem().withPrefix(
                                        p2.getPadding().getTree().getElem().getPrefix().withWhitespace(" ")
                                )
                        )
                );
            }
            if (StringUtils.isNullOrEmpty(p2.getPadding().getTree().getAfter().getWhitespace())) {
                p2 = p2.getPadding().withTree(
                        p2.getPadding().getTree().withAfter(
                                p2.getPadding().getTree().getAfter().withWhitespace(" ")
                        )
                );
            }
        } else {
            if (p2.getPadding().getTree().getElem().getPrefix().getWhitespace().equals(" ")) {
                p2 = p2.getPadding().withTree(
                        p2.getPadding().getTree().withElem(
                                p2.getPadding().getTree().getElem().withPrefix(
                                        p2.getPadding().getTree().getElem().getPrefix().withWhitespace("")
                                )
                        )
                );
            }
            if (p2.getPadding().getTree().getAfter().getWhitespace().equals(" ")) {
                p2 = p2.getPadding().withTree(
                        p2.getPadding().getTree().withAfter(
                                p2.getPadding().getTree().getAfter().withWhitespace("")
                        )
                );
            }
        }
        return p2;
    }

    @Override
    public J.TypeCast visitTypeCast(J.TypeCast typeCast, P p) {
        J.TypeCast tc = super.visitTypeCast(typeCast, p);
        boolean spaceWithinTypeCastParens = style.getWithin().isTypeCastParentheses();
        tc = tc.withClazz(
                tc.getClazz().withTree(
                        spaceBefore(tc.getClazz().getTree(), spaceWithinTypeCastParens)
                )
        );
        tc = tc.withClazz(
                tc.getClazz().getPadding().withTree(
                        spaceAfter(tc.getClazz().getPadding().getTree(), spaceWithinTypeCastParens)
                )
        );
        return tc;
    }
}
