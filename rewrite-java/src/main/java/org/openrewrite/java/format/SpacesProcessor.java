package org.openrewrite.java.format;

import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoProcessor;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.java.tree.*;

public class SpacesProcessor<P> extends JavaIsoProcessor<P> {

    /*
    TODO Finish support for SpacesStyle properties, from SpacesStyle.Within.groupingParentheses down
     */

    private final SpacesStyle style;

    public SpacesProcessor(SpacesStyle style) {
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

    @Override
    public J.ClassDecl visitClassDecl(J.ClassDecl classDecl, P p) {
        J.ClassDecl classDecl1 = super.visitClassDecl(classDecl, p);
        classDecl1 = classDecl1.withBody(spaceBefore(classDecl1.getBody(), style.getBeforeLeftBrace().isClassLeftBrace()));
        boolean withinCodeBraces = style.getWithin().isCodeBraces();
        if (withinCodeBraces && StringUtils.isNullOrEmpty(classDecl1.getBody().getEnd().getWhitespace())) {
            classDecl1 = classDecl1.withBody(
                    classDecl1.getBody().withEnd(
                            classDecl1.getBody().getEnd().withWhitespace(" ")
                    )
            );
        } else if (!withinCodeBraces && classDecl1.getBody().getEnd().getWhitespace().equals(" ")) {
            classDecl1 = classDecl1.withBody(
                    classDecl1.getBody().withEnd(
                            classDecl1.getBody().getEnd().withWhitespace("")
                    )
            );
        }
        return classDecl1;
    }

    @Override
    public J.MethodDecl visitMethod(J.MethodDecl method, P p) {
        J.MethodDecl methodDecl = super.visitMethod(method, p);
        methodDecl = methodDecl.withParams(
                spaceBefore(methodDecl.getParams(), style.getBeforeParentheses().isMethodDeclaration()));
        if (methodDecl.getBody() != null) {
            methodDecl = methodDecl.withBody(spaceBefore(methodDecl.getBody(), style.getBeforeLeftBrace().isMethodLeftBrace()));
        }
        return methodDecl;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation methodInvocation = super.visitMethodInvocation(method, p);
        return methodInvocation.withArgs(spaceBefore(methodInvocation.getArgs(), style.getBeforeParentheses().isMethodCall()));
    }

    @Override
    public J.If visitIf(J.If iff, P p) {
        J.If anIf = super.visitIf(iff, p);
        anIf = anIf.withIfCondition(spaceBefore(anIf.getIfCondition(), style.getBeforeParentheses().isIfParentheses()));
        anIf = anIf.withThenPart(spaceBeforeRightPaddedElement(anIf.getThenPart(), style.getBeforeLeftBrace().isIfLeftBrace()));
        return anIf;
    }

    @Override
    public J.If.Else visitElse(J.If.Else elze, P p) {
        J.If.Else anElse = super.visitElse(elze, p);
        anElse = anElse.withBody(spaceBeforeRightPaddedElement(anElse.getBody(), style.getBeforeLeftBrace().isElseLeftBrace()));
        anElse = spaceBefore(anElse, style.getBeforeKeywords().isElseKeyword());
        return anElse;
    }

    @Override
    public J.ForLoop visitForLoop(J.ForLoop forLoop, P p) {
        J.ForLoop fl = super.visitForLoop(forLoop, p);
        fl = fl.withControl(spaceBefore(fl.getControl(), style.getBeforeParentheses().isForParentheses()));
        fl = fl.withBody(spaceBeforeRightPaddedElement(fl.getBody(), style.getBeforeLeftBrace().isForLeftBrace()));
        return fl;
    }

    @Override
    public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, P p) {
        J.ForEachLoop forEachLoop = super.visitForEachLoop(forLoop, p);
        forEachLoop = forEachLoop.withControl(spaceBefore(forEachLoop.getControl(), style.getBeforeParentheses().isForParentheses()));
        forEachLoop = forEachLoop.withBody(spaceBeforeRightPaddedElement(forEachLoop.getBody(), style.getBeforeLeftBrace().isForLeftBrace()));
        return forEachLoop;
    }

    @Override
    public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, P p) {
        J.WhileLoop wl = super.visitWhileLoop(whileLoop, p);
        wl = wl.withCondition(spaceBefore(wl.getCondition(), style.getBeforeParentheses().isWhileParentheses()));
        wl = wl.withBody(spaceBeforeRightPaddedElement(wl.getBody(), style.getBeforeLeftBrace().isWhileLeftBrace()));
        return wl;
    }

    @Override
    public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        J.DoWhileLoop dwl = super.visitDoWhileLoop(doWhileLoop, p);
        dwl = dwl.withWhileCondition(spaceBefore(dwl.getWhileCondition(), style.getBeforeKeywords().isWhileKeyword()));
        dwl = dwl.withWhileCondition(spaceBeforeLeftPaddedElement(dwl.getWhileCondition(), style.getBeforeParentheses().isWhileParentheses()));
        dwl = dwl.withBody(spaceBeforeRightPaddedElement(dwl.getBody(), style.getBeforeLeftBrace().isDoLeftBrace()));
        return dwl;
    }

    @Override
    public J.Switch visitSwitch(J.Switch _switch, P p) {
        J.Switch aSwitch = super.visitSwitch(_switch, p);
        aSwitch = aSwitch.withSelector(spaceBefore(aSwitch.getSelector(), style.getBeforeParentheses().isSwitchParentheses()));
        aSwitch = aSwitch.withCases(spaceBefore(aSwitch.getCases(), style.getBeforeLeftBrace().isSwitchLeftBrace()));
        return aSwitch;
    }

    @Override
    public J.Try visitTry(J.Try _try, P p) {
        J.Try aTry = super.visitTry(_try, p);
        if (aTry.getResources() != null) {
            aTry = aTry.withResources(spaceBefore(aTry.getResources(), style.getBeforeParentheses().isTryParentheses()));
        }
        aTry = aTry.withBody(spaceBefore(aTry.getBody(), style.getBeforeLeftBrace().isTryLeftBrace()));
        if (aTry.getFinally() != null) {
            JLeftPadded<J.Block> finally1 = spaceBefore(aTry.getFinally(), style.getBeforeKeywords().isFinallyKeyword());
            finally1 = spaceBeforeLeftPaddedElement(finally1, style.getBeforeLeftBrace().isFinallyLeftBrace());
            aTry = aTry.withFinally(finally1);
        }
        return aTry;
    }

    @Override
    public J.Try.Catch visitCatch(J.Try.Catch _catch, P p) {
        J.Try.Catch aCatch = super.visitCatch(_catch, p);
        aCatch = spaceBefore(aCatch, style.getBeforeKeywords().isCatchKeyword());
        aCatch = aCatch.withParam(spaceBefore(aCatch.getParam(), style.getBeforeParentheses().isCatchParentheses()));
        aCatch = aCatch.withBody(spaceBefore(aCatch.getBody(), style.getBeforeLeftBrace().isCatchLeftBrace()));
        return aCatch;
    }

    @Override
    public J.Synchronized visitSynchronized(J.Synchronized _sync, P p) {
        J.Synchronized aSynchronized = super.visitSynchronized(_sync, p);
        aSynchronized = aSynchronized.withLock(spaceBefore(aSynchronized.getLock(), style.getBeforeParentheses().isSynchronizedParentheses()));
        aSynchronized = aSynchronized.withBody(spaceBefore(aSynchronized.getBody(), style.getBeforeLeftBrace().isSynchronizedLeftBrace()));
        return aSynchronized;
    }

    @Override
    public J.Annotation visitAnnotation(J.Annotation annotation, P p) {
        J.Annotation anno = super.visitAnnotation(annotation, p);
        if (anno.getArgs() != null) {
            anno = anno.withArgs(spaceBefore(anno.getArgs(), style.getBeforeParentheses().isAnnotationParameters()));
        }
        return anno;
    }

    @Override
    public J.Assign visitAssign(J.Assign assign, P p) {
        J.Assign assign1 = super.visitAssign(assign, p);
        assign1 = assign1.withAssignment(spaceBefore(assign1.getAssignment(), style.getAroundOperators().isAssignment()));
        assign1 = assign1.withAssignment(
                assign1.getAssignment().withElem(
                        spaceBefore(assign1.getAssignment().getElem(), style.getAroundOperators().isAssignment())
                )
        );
        return assign1;
    }

    @Override
    public J.AssignOp visitAssignOp(J.AssignOp assignOp, P p) {
        J.AssignOp assignOp1 = super.visitAssignOp(assignOp, p);
        String operatorBeforeWhitespace = assignOp1.getOperator().getBefore().getWhitespace();
        if (style.getAroundOperators().isAssignment() && StringUtils.isNullOrEmpty(operatorBeforeWhitespace)) {
            assignOp1 = assignOp1.withOperator(
                    assignOp1.getOperator().withBefore(
                            assignOp1.getOperator().getBefore().withWhitespace(" ")
                    )
            );
        } else if (!style.getAroundOperators().isAssignment() && operatorBeforeWhitespace.equals(" ")) {
            assignOp1 = assignOp1.withOperator(
                    assignOp1.getOperator().withBefore(
                            assignOp1.getOperator().getBefore().withWhitespace("")
                    )
            );
        }
        assignOp1 = assignOp1.withAssignment(spaceBefore(assignOp1.getAssignment(), style.getAroundOperators().isAssignment()));
        return assignOp1;
    }

    @Override
    public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable, P p) {
        J.VariableDecls.NamedVar namedVar = super.visitVariable(variable, p);
        if (namedVar.getInitializer() != null) {
            namedVar = namedVar.withInitializer(spaceBefore(namedVar.getInitializer(), style.getAroundOperators().isAssignment()));
        }
        if (namedVar.getInitializer() != null) {
            if (namedVar.getInitializer().getElem() != null) {
                namedVar = namedVar.withInitializer(
                        namedVar.getInitializer().withElem(
                                spaceBefore(namedVar.getInitializer().getElem(), style.getAroundOperators().isAssignment())
                        )
                );
            }
        }
        return namedVar;
    }

    @Override
    public J.Binary visitBinary(J.Binary binary, P p) {
        J.Binary binary1 = super.visitBinary(binary, p);
        J.Binary.Type operator = binary1.getOperator().getElem();
        switch (operator) {
            case And:
            case Or:
                binary1 = applyBinarySpaceAround(binary1, style.getAroundOperators().isLogical());
                break;
            case Equal:
            case NotEqual:
                binary1 = applyBinarySpaceAround(binary1, style.getAroundOperators().isEquality());
                break;
            case LessThan:
            case LessThanOrEqual:
            case GreaterThan:
            case GreaterThanOrEqual:
                binary1 = applyBinarySpaceAround(binary1, style.getAroundOperators().isRelational());
            case BitAnd:
            case BitOr:
            case BitXor:
                binary1 = applyBinarySpaceAround(binary1, style.getAroundOperators().isBitwise());
            case Addition:
            case Subtraction:
                binary1 = applyBinarySpaceAround(binary1, style.getAroundOperators().isAdditive());
            case Multiplication:
            case Division:
            case Modulo:
                binary1 = applyBinarySpaceAround(binary1, style.getAroundOperators().isMultiplicative());
            case LeftShift:
            case RightShift:
            case UnsignedRightShift:
                binary1 = applyBinarySpaceAround(binary1, style.getAroundOperators().isShift());
        }
        return binary1;
    }

    private J.Binary applyBinarySpaceAround(J.Binary binary, boolean useSpaceAround) {
        if (useSpaceAround) {
            if (StringUtils.isNullOrEmpty(binary.getOperator().getBefore().getWhitespace())) {
                binary = binary.withOperator(
                        binary.getOperator().withBefore(
                                binary.getOperator().getBefore().withWhitespace(" ")
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
            if (binary.getOperator().getBefore().getWhitespace().equals(" ")) {
                binary = binary.withOperator(
                        binary.getOperator().withBefore(
                                binary.getOperator().getBefore().withWhitespace("")
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
        J.Unary unary1 = super.visitUnary(unary, p);
        switch (unary1.getOperator().getElem()) {
            case PostIncrement:
            case PostDecrement:
                unary1 = applyUnaryOperatorBeforeSpace(unary1, style.getAroundOperators().isUnary());
                break;
            case PreIncrement:
            case PreDecrement:
            case Negative:
            case Positive:
            case Not:
            case Complement:
                unary1 = applyUnaryOperatorBeforeSpace(unary1, style.getAroundOperators().isUnary());
                unary1 = applyUnaryOperatorExprSpace(unary1, style.getAroundOperators().isUnary());
                break;
        }
        return unary1;
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

    private J.Unary applyUnaryOperatorBeforeSpace(J.Unary unary, boolean useAroundUnaryOperatorSpace) {
        if (useAroundUnaryOperatorSpace && StringUtils.isNullOrEmpty(unary.getOperator().getBefore().getWhitespace())) {
            unary = unary.withOperator(
                    unary.getOperator().withBefore(
                            unary.getOperator().getBefore().withWhitespace(" ")
                    )
            );
        } else if (!useAroundUnaryOperatorSpace && unary.getOperator().getBefore().getWhitespace().equals(" ")) {
            unary = unary.withOperator(
                    unary.getOperator().withBefore(
                            unary.getOperator().getBefore().withWhitespace("")
                    )
            );
        }
        return unary;
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
        J.MemberReference memberReference = super.visitMemberReference(memberRef, p);
        memberReference = memberReference.withReference(
                spaceBefore(memberReference.getReference(), style.getAroundOperators().isMethodReferenceDoubleColon())
        );
        memberReference = memberReference.withReference(
                memberReference.getReference().withElem(
                        spaceBefore(memberReference.getReference().getElem(), style.getAroundOperators().isMethodReferenceDoubleColon())
                )
        );
        return memberReference;
    }

    @Override
    public J.NewArray visitNewArray(J.NewArray newArray, P p) {
        J.NewArray newArray1 = super.visitNewArray(newArray, p);
        if (getCursor().getParent() != null && getCursor().getParent().getTree() instanceof J.Annotation) {
            if (newArray1.getInitializer() != null) {
                newArray1 = newArray1.withInitializer(
                        spaceBefore(newArray1.getInitializer(), style.getBeforeLeftBrace().isAnnotationArrayInitializerLeftBrace())
                );
            }
        } else {
            if (newArray1.getInitializer() != null) {
                JContainer<Expression> initializer = spaceBefore(newArray1.getInitializer(), style.getBeforeLeftBrace().isArrayInitializerLeftBrace());
                newArray1 = newArray1.withInitializer(initializer);
            }
        }
        if (newArray1.getInitializer() != null) {
            JContainer<Expression> initializer = newArray1.getInitializer();
            if (!initializer.getElem().isEmpty()) {
                boolean useSpaceWithinArrayInitializerBraces = style.getWithin().isArrayInitializerBraces();
                if (useSpaceWithinArrayInitializerBraces) {
                    if (!(initializer.getElem().iterator().next().getElem() instanceof J.Empty)) {
                        if (StringUtils.isNullOrEmpty(initializer.getElem().iterator().next().getElem().getPrefix().getWhitespace())) {
                            initializer = initializer.withElem(ListUtils.mapFirst(initializer.getElem(), e -> e.withElem(e.getElem().withPrefix(e.getElem().getPrefix().withWhitespace(" ")))));
                        }
                        if (StringUtils.isNullOrEmpty(initializer.getElem().get(initializer.getElem().size() - 1).getAfter().getWhitespace())) {
                            initializer = initializer.withElem(ListUtils.mapLast(initializer.getElem(), e -> e.withAfter(e.getAfter().withWhitespace(" "))));
                        }
                    }
                } else {
                    if (!(initializer.getElem().iterator().next().getElem() instanceof J.Empty)) {
                        if (initializer.getElem().iterator().next().getElem().getPrefix().getWhitespace().equals(" ")) {
                            initializer = initializer.withElem(ListUtils.mapFirst(initializer.getElem(), e -> e.withElem(e.getElem().withPrefix(e.getElem().getPrefix().withWhitespace("")))));
                        }
                        if (initializer.getElem().get(initializer.getElem().size() - 1).getAfter().getWhitespace().equals(" ")) {
                            initializer = initializer.withElem(ListUtils.mapLast(initializer.getElem(), e -> e.withAfter(e.getAfter().withWhitespace(""))));
                        }
                    }
                }
                boolean useSpaceWithinEmptyArrayInitializerBraces = style.getWithin().isEmptyArrayInitializerBraces();
                if (useSpaceWithinEmptyArrayInitializerBraces) {
                    if ((initializer.getElem().iterator().next().getElem() instanceof J.Empty)) {
                        if (StringUtils.isNullOrEmpty(initializer.getElem().iterator().next().getElem().getPrefix().getWhitespace())) {
                            initializer = initializer.withElem(ListUtils.mapFirst(initializer.getElem(), e -> e.withElem(e.getElem().withPrefix(e.getElem().getPrefix().withWhitespace(" ")))));
                        }
                    }
                } else {
                    if ((initializer.getElem().iterator().next().getElem() instanceof J.Empty)) {
                        if (initializer.getElem().iterator().next().getElem().getPrefix().getWhitespace().equals(" ")) {
                            initializer = initializer.withElem(ListUtils.mapFirst(initializer.getElem(), e -> e.withElem(e.getElem().withPrefix(e.getElem().getPrefix().withWhitespace("")))));
                        }
                    }
                }
            }
            newArray1 = newArray1.withInitializer(initializer);
        }
        return newArray1;
    }

    @Override
    public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, P p) {
        J.ArrayAccess arrayAccess1 = super.visitArrayAccess(arrayAccess, p);
        boolean useSpaceWithinBrackets = style.getWithin().isBrackets();
        if (useSpaceWithinBrackets) {
            if (StringUtils.isNullOrEmpty(arrayAccess1.getDimension().getIndex().getElem().getPrefix().getWhitespace())) {
                arrayAccess1 = arrayAccess1.withDimension(
                        arrayAccess1.getDimension().withIndex(
                                arrayAccess1.getDimension().getIndex().withElem(
                                        arrayAccess1.getDimension().getIndex().getElem().withPrefix(
                                                arrayAccess1.getDimension().getIndex().getElem().getPrefix().withWhitespace(" ")
                                        )
                                )
                        )
                );
            }
            if (StringUtils.isNullOrEmpty(arrayAccess1.getDimension().getIndex().getAfter().getWhitespace())) {
                arrayAccess1 = arrayAccess1.withDimension(
                        arrayAccess1.getDimension().withIndex(
                                arrayAccess1.getDimension().getIndex().withAfter(
                                        arrayAccess1.getDimension().getIndex().getAfter().withWhitespace(" ")
                                )
                        )
                );
            }
        } else {
            if (arrayAccess1.getDimension().getIndex().getElem().getPrefix().getWhitespace().equals(" ")) {
                arrayAccess1 = arrayAccess1.withDimension(
                        arrayAccess1.getDimension().withIndex(
                                arrayAccess1.getDimension().getIndex().withElem(
                                        arrayAccess1.getDimension().getIndex().getElem().withPrefix(
                                                arrayAccess1.getDimension().getIndex().getElem().getPrefix().withWhitespace("")
                                        )
                                )
                        )
                );
            }
            if (arrayAccess1.getDimension().getIndex().getAfter().getWhitespace().equals(" ")) {
                arrayAccess1 = arrayAccess1.withDimension(
                        arrayAccess1.getDimension().withIndex(
                                arrayAccess1.getDimension().getIndex().withAfter(
                                        arrayAccess1.getDimension().getIndex().getAfter().withWhitespace("")
                                )
                        )
                );
            }
        }
        return arrayAccess1;
    }
}
