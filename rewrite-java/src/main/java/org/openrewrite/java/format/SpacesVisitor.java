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
        return container.withElement(spaceBefore(container.getElement(), spaceBefore));
    }

    <T extends J> JRightPadded<T> spaceBeforeRightPaddedElement(JRightPadded<T> container, boolean spaceBefore) {
        return container.withElement(spaceBefore(container.getElement(), spaceBefore));
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
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
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
        if (c.getPadding().getTypeParameters() != null) {
            boolean spaceWithinAngleBrackets = style.getWithin().isAngleBrackets();
            int typeParametersSize = c.getPadding().getTypeParameters().getElements().size();
            c = c.getPadding().withTypeParameters(
                    c.getPadding().getTypeParameters().getPadding().withElements(
                            ListUtils.map(c.getPadding().getTypeParameters().getPadding().getElements(),
                                    (index, elemContainer) -> {
                                        if (index == 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(), spaceWithinAngleBrackets)
                                            );
                                        }
                                        if (index == typeParametersSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, spaceWithinAngleBrackets);
                                        }
                                        return elemContainer;
                                    }
                            )
                    )
            );
        }
        return c;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);
        m = m.getPadding().withParameters(
                spaceBefore(m.getPadding().getParameters(), style.getBeforeParentheses().isMethodDeclaration()));
        if (m.getBody() != null) {
            m = m.withBody(spaceBefore(m.getBody(), style.getBeforeLeftBrace().isMethodLeftBrace()));
        }
        Statement firstParam = m.getParameters().iterator().next();
        if (firstParam instanceof J.Empty) {
            boolean useSpace = style.getWithin().isEmptyMethodDeclarationParentheses();
            m = m.getPadding().withParameters(
                    m.getPadding().getParameters().getPadding().withElements(
                            ListUtils.map(m.getPadding().getParameters().getPadding().getElements(),
                                    param -> param.withElement(spaceBefore(param.getElement(), useSpace))
                            )
                    )
            );
        } else {
            final int paramsSize = m.getParameters().size();
            boolean useSpace = style.getWithin().isMethodDeclarationParentheses();
            m = m.getPadding().withParameters(
                    m.getPadding().getParameters().getPadding().withElements(
                            ListUtils.map(m.getPadding().getParameters().getPadding().getElements(),
                                    (index, param) -> {
                                        if (index == 0) {
                                            param = param.withElement(spaceBefore(param.getElement(), useSpace));
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
        if (m.getPadding().getTypeParameters() != null) {
            boolean spaceWithinAngleBrackets = style.getWithin().isAngleBrackets();
            int typeParametersSize = m.getPadding().getTypeParameters().getElements().size();
            m = m.getPadding().withTypeParameters(
                    m.getPadding().getTypeParameters().getPadding().withElements(
                            ListUtils.map(m.getPadding().getTypeParameters().getPadding().getElements(),
                                    (index, elemContainer) -> {
                                        if (index == 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(), spaceWithinAngleBrackets)
                                            );
                                        }
                                        if (index == typeParametersSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, spaceWithinAngleBrackets);
                                        }
                                        return elemContainer;
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
        m = m.getPadding().withArguments(spaceBefore(m.getPadding().getArguments(), style.getBeforeParentheses().isMethodCall()));
        Expression firstArg = m.getArguments().iterator().next();
        if (firstArg instanceof J.Empty) {
            boolean useSpace = style.getWithin().isEmptyMethodCallParentheses();
            m = m.getPadding().withArguments(
                    m.getPadding().getArguments().getPadding().withElements(
                            ListUtils.map(m.getPadding().getArguments().getPadding().getElements(),
                                    arg -> arg.withElement(spaceBefore(arg.getElement(), useSpace))
                            )
                    )
            );
        } else {
            final int argsSize = m.getArguments().size();
            boolean useSpace = style.getWithin().isMethodCallParentheses();
            m = m.getPadding().withArguments(
                    m.getPadding().getArguments().getPadding().withElements(
                            ListUtils.map(m.getPadding().getArguments().getPadding().getElements(),
                                    (index, arg) -> {
                                        if (index == 0) {
                                            arg = arg.withElement(spaceBefore(arg.getElement(), useSpace));
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
                                i.getIfCondition().getPadding().getTree().withElement(
                                        spaceBefore(i.getIfCondition().getPadding().getTree().getElement(), useSpaceWithinIfParentheses
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
                    t.getPadding().getResources().getPadding().withElements(
                            ListUtils.mapLast(t.getPadding().getResources().getPadding().getElements(),
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
        c = c.withParameter(spaceBefore(c.getParameter(), style.getBeforeParentheses().isCatchParentheses()));
        c = c.withBody(spaceBefore(c.getBody(), style.getBeforeLeftBrace().isCatchLeftBrace()));
        boolean spaceWithinCatchParens = style.getWithin().isCatchParentheses();
        c = c.withParameter(
                c.getParameter().withTree(
                        spaceBefore(c.getParameter().getTree(), spaceWithinCatchParens)
                )
        );
        c = c.withParameter(
                c.getParameter().getPadding().withTree(
                        spaceAfter(c.getParameter().getPadding().getTree(), spaceWithinCatchParens)
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
        if (a.getPadding().getArguments() != null) {
            a = a.getPadding().withArguments(spaceBefore(a.getPadding().getArguments(),
                    style.getBeforeParentheses().isAnnotationParameters()));
        }
        boolean spaceWithinAnnotationParentheses = style.getWithin().isAnnotationParentheses();
        if (a.getPadding().getArguments() != null) {
            int argsSize = a.getPadding().getArguments().getElements().size();
            a = a.getPadding().withArguments(
                    a.getPadding().getArguments().getPadding().withElements(
                            ListUtils.map(a.getPadding().getArguments().getPadding().getElements(),
                                    (index, arg) -> {
                                        if (index == 0) {
                                            // don't overwrite changes made by before left brace annotation array
                                            // initializer setting when space within annotation parens is false
                                            if (spaceWithinAnnotationParentheses ||
                                                    !style.getBeforeLeftBrace().isAnnotationArrayInitializerLeftBrace()) {
                                                arg = arg.withElement(
                                                        spaceBefore(arg.getElement(), spaceWithinAnnotationParentheses)
                                                );
                                            }
                                        }
                                        if (index == argsSize - 1) {
                                            arg = spaceAfter(arg, spaceWithinAnnotationParentheses);
                                        }
                                        return arg;
                                    }
                            )
                    )
            );
        }
        return a;
    }

    @Override
    public J.Assignment visitAssignment(J.Assignment assignment, P p) {
        J.Assignment a = super.visitAssignment(assignment, p);
        a = a.getPadding().withAssignment(spaceBefore(a.getPadding().getAssignment(), style.getAroundOperators().isAssignment()));
        a = a.getPadding().withAssignment(
                a.getPadding().getAssignment().withElement(
                        spaceBefore(a.getPadding().getAssignment().getElement(), style.getAroundOperators().isAssignment())
                )
        );
        return a;
    }

    @Override
    public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, P p) {
        J.AssignmentOperation a = super.visitAssignmentOperation(assignOp, p);
        J.AssignmentOperation.Padding padding = a.getPadding();
        JLeftPadded<J.AssignmentOperation.Type> operator = padding.getOperator();
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
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, p);
        if (v.getPadding().getInitializer() != null) {
            v = v.getPadding().withInitializer(spaceBefore(v.getPadding().getInitializer(), style.getAroundOperators().isAssignment()));
        }
        if (v.getPadding().getInitializer() != null) {
            if (v.getPadding().getInitializer().getElement() != null) {
                v = v.getPadding().withInitializer(
                        v.getPadding().getInitializer().withElement(
                                spaceBefore(v.getPadding().getInitializer().getElement(), style.getAroundOperators().isAssignment())
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
        if (useAroundUnaryOperatorSpace && StringUtils.isNullOrEmpty(unary.getExpression().getPrefix().getWhitespace())) {
            unary = unary.withExpression(
                    unary.getExpression().withPrefix(
                            unary.getExpression().getPrefix().withWhitespace(" ")
                    )
            );
        } else if (!useAroundUnaryOperatorSpace && unary.getExpression().getPrefix().getWhitespace().equals(" ")) {
            unary = unary.withExpression(
                    unary.getExpression().withPrefix(
                            unary.getExpression().getPrefix().withWhitespace("")
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
                m.getPadding().getReference().withElement(
                        spaceBefore(m.getPadding().getReference().getElement(), style.getAroundOperators().isMethodReferenceDoubleColon())
                )
        );
        return m;
    }

    @Override
    public J.NewArray visitNewArray(J.NewArray newArray, P p) {
        J.NewArray n = super.visitNewArray(newArray, p);
        if (getCursor().getParent() != null && getCursor().getParent().firstEnclosing(J.class) instanceof J.Annotation) {
            /*
             * IntelliJ setting
             * Spaces -> Within -> Annotation parentheses
             * when enabled supercedes
             * Spaces -> Before left brace -> Annotation array initializer left brace
             */
            if (!style.getWithin().isAnnotationParentheses()) {
                n = spaceBefore(n, style.getBeforeLeftBrace().isAnnotationArrayInitializerLeftBrace());
            }
        } else {
            if (n.getPadding().getInitializer() != null) {
                JContainer<Expression> initializer = spaceBefore(n.getPadding().getInitializer(), style.getBeforeLeftBrace().isArrayInitializerLeftBrace());
                n = n.getPadding().withInitializer(initializer);
            }
        }
        if (n.getPadding().getInitializer() != null) {
            JContainer<Expression> i = n.getPadding().getInitializer();
            if (!i.getElements().isEmpty()) {
                if (style.getOther().isAfterComma()) {
                    i = JContainer.withElements(i, ListUtils.map(i.getElements(), (idx, elem) -> idx == 0 ? elem : spaceBefore(elem, true)));
                }

                boolean useSpaceWithinArrayInitializerBraces = style.getWithin().isArrayInitializerBraces();
                if (useSpaceWithinArrayInitializerBraces) {
                    if (!(i.getElements().iterator().next() instanceof J.Empty)) {
                        if (StringUtils.isNullOrEmpty(i.getElements().iterator().next().getPrefix().getWhitespace())) {
                            i = i.getPadding().withElements(ListUtils.mapFirst(i.getPadding().getElements(),
                                    e -> e.withElement(e.getElement().withPrefix(e.getElement().getPrefix().withWhitespace(" ")))));
                        }
                        if (StringUtils.isNullOrEmpty(i.getPadding().getElements().get(i.getElements().size() - 1).getAfter().getWhitespace())) {
                            i = i.getPadding().withElements(ListUtils.mapLast(i.getPadding().getElements(),
                                    e -> e.withAfter(e.getAfter().withWhitespace(" "))));
                        }
                    }
                } else {
                    if (!(i.getElements().iterator().next() instanceof J.Empty)) {
                        if (i.getElements().iterator().next().getPrefix().getWhitespace().equals(" ")) {
                            i = i.getPadding().withElements(ListUtils.mapFirst(i.getPadding().getElements(),
                                    e -> e.withElement(e.getElement().withPrefix(e.getElement().getPrefix().withWhitespace("")))));
                        }
                        if (i.getPadding().getElements().get(i.getElements().size() - 1).getAfter().getWhitespace().equals(" ")) {
                            i = i.getPadding().withElements(ListUtils.mapLast(i.getPadding().getElements(),
                                    e -> e.withAfter(e.getAfter().withWhitespace(""))));
                        }
                    }
                }
                boolean useSpaceWithinEmptyArrayInitializerBraces = style.getWithin().isEmptyArrayInitializerBraces();
                if (useSpaceWithinEmptyArrayInitializerBraces) {
                    if ((i.getElements().iterator().next() instanceof J.Empty)) {
                        if (StringUtils.isNullOrEmpty(i.getElements().iterator().next().getPrefix().getWhitespace())) {
                            i = i.getPadding().withElements(ListUtils.mapFirst(i.getPadding().getElements(),
                                    e -> e.withElement(e.getElement().withPrefix(e.getElement().getPrefix().withWhitespace(" ")))));
                        }
                    }
                } else {
                    if ((i.getElements().iterator().next() instanceof J.Empty)) {
                        if (i.getElements().iterator().next().getPrefix().getWhitespace().equals(" ")) {
                            i = i.getPadding().withElements(ListUtils.mapFirst(i.getPadding().getElements(),
                                    e -> e.withElement(e.getElement().withPrefix(e.getElement().getPrefix().withWhitespace("")))));
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
            if (StringUtils.isNullOrEmpty(a.getDimension().getPadding().getIndex().getElement().getPrefix().getWhitespace())) {
                a = a.withDimension(
                        a.getDimension().getPadding().withIndex(
                                a.getDimension().getPadding().getIndex().withElement(
                                        a.getDimension().getPadding().getIndex().getElement().withPrefix(
                                                a.getDimension().getPadding().getIndex().getElement().getPrefix().withWhitespace(" ")
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
            if (a.getDimension().getPadding().getIndex().getElement().getPrefix().getWhitespace().equals(" ")) {
                a = a.withDimension(
                        a.getDimension().getPadding().withIndex(
                                a.getDimension().getPadding().getIndex().withElement(
                                        a.getDimension().getPadding().getIndex().getElement().withPrefix(
                                                a.getDimension().getPadding().getIndex().getElement().getPrefix().withWhitespace("")
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
            if (StringUtils.isNullOrEmpty(p2.getPadding().getTree().getElement().getPrefix().getWhitespace())) {
                p2 = p2.getPadding().withTree(
                        p2.getPadding().getTree().withElement(
                                p2.getPadding().getTree().getElement().withPrefix(
                                        p2.getPadding().getTree().getElement().getPrefix().withWhitespace(" ")
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
            if (p2.getPadding().getTree().getElement().getPrefix().getWhitespace().equals(" ")) {
                p2 = p2.getPadding().withTree(
                        p2.getPadding().getTree().withElement(
                                p2.getPadding().getTree().getElement().withPrefix(
                                        p2.getPadding().getTree().getElement().getPrefix().withWhitespace("")
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

    @Override
    public J.ParameterizedType visitParameterizedType(J.ParameterizedType type, P p) {
        J.ParameterizedType pt = super.visitParameterizedType(type, p);
        boolean spaceWithinAngleBrackets = style.getWithin().isAngleBrackets();
        if (pt.getPadding().getTypeParameters() != null &&
                !(pt.getPadding().getTypeParameters().getElements().iterator().next() instanceof J.Empty)) {
            int typeParametersSize = pt.getPadding().getTypeParameters().getElements().size();
            pt = pt.getPadding().withTypeParameters(
                    pt.getPadding().getTypeParameters().getPadding().withElements(
                            ListUtils.map(pt.getPadding().getTypeParameters().getPadding().getElements(),
                                    (index, elemContainer) -> {
                                        if (index == 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(), spaceWithinAngleBrackets)
                                            );
                                        }
                                        if (index == typeParametersSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, spaceWithinAngleBrackets);
                                        }
                                        return elemContainer;
                                    }
                            )
                    )
            );
        }
        return pt;
    }
}
