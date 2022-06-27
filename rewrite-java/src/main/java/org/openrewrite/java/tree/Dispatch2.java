package org.openrewrite.java.tree;

public abstract class Dispatch2<T,P1,P2> {

    private String className(J pp) {
        return pp.getClass().getName().replaceAll("^org.openrewrite.java.tree.", "");
    }

    public final T dispatch(J pp, P1 p1, P2 p2) {
        switch (className(pp)) {
            case "J$ArrayAccess":
                return visitArrayAccess(pp, p1, p2);
            case "J$Assert":
                return visitAssert(pp, p1, p2);
            case "J$Assignment":
                return visitAssignment(pp, p1, p2);
            case "J$AssignmentOperation":
                return visitAssignmentOperation(pp, p1, p2);
            case "J$Binary":
                return visitBinary(pp, p1, p2);
            case "J$Block":
                return visitBlock(pp, p1, p2);
            case "J$Break":
                return visitBreak(pp, p1, p2);
            case "J$Case":
                return visitCase(pp, p1, p2);
            case "J$ClassDeclaration":
                return visitClassDeclaration(pp, p1, p2);
            case "J$CompilationUnit":
                return visitCompilationUnit(pp, p1, p2);
            case "J$Continue":
                return visitContinue(pp, p1, p2);
            case "J$ControlParentheses":
                return visitControlParentheses(pp, p1, p2);
            case "J$DoWhileLoop":
                return visitDoWhileLoop(pp, p1, p2);
            case "J$Empty":
                return visitEmpty(pp, p1, p2);
            case "J$EnumValue":
                return visitEnumValue(pp, p1, p2);
            case "J$EnumValueSet":
                return visitEnumValueSet(pp, p1, p2);
            case "J$FieldAccess":
                return visitFieldAccess(pp, p1, p2);
            case "J$ForeachLoop":
                return visitForeachLoop(pp, p1, p2);
            case "J$ForLoop":
                return visitForLoop(pp, p1, p2);
            case "J$ForLoop$Control":
                return visitForLoopControl(pp, p1, p2);
            case "J$Identifier":
                return visitIdentifier(pp, p1, p2);
            case "J$InstanceOf":
                return visitInstanceOf(pp, p1, p2);
            case "J$If":
                return visitIf(pp, p1, p2);
            case "J$If$Else":
                return visitIfElse(pp, p1, p2);
            case "J$Label":
                return visitLabel(pp, p1, p2);
            case "J$Lambda":
                return visitLambda(pp, p1, p2);
            case "J$Literal":
                return visitLiteral(pp, p1, p2);
            case "J$MethodDeclaration":
                return visitMethodDeclaration(pp, p1, p2);
            case "J$MethodInvocation":
                return visitMethodInvocation(pp, p1, p2);
            case "J$MultiCatch":
                return visitMultiCatch(pp, p1, p2);
            case "J$NewArray":
                return visitNewArray(pp, p1, p2);
            case "J$NewClass":
                return visitNewClass(pp, p1, p2);
            case "J$Parentheses":
                return visitParentheses(pp, p1, p2);
            case "J$Return":
                return visitReturn(pp, p1, p2);
            case "J$Switch":
                return visitSwitch(pp, p1, p2);
            case "J$Ternary":
                return visitTernary(pp, p1, p2);
            case "J$Throw":
                return visitThrow(pp, p1, p2);
            case "J$Try":
                return visitTry(pp, p1, p2);
            case "J$TypeCast":
                return visitTypeCast(pp, p1, p2);
            case "J$Unary":
                return visitUnary(pp, p1, p2);
            case "J$VariableDeclarations":
                return visitVariableDeclarations(pp, p1, p2);
            case "J$VariableDeclarations$NamedVariable":
                return visitVariableDeclarationsNamedVariable(pp, p1, p2);
            case "J$WhileLoop":
                return visitWhileLoop(pp, p1, p2);

            default:
                throw new Error("Unexpected node type: " + pp.getClass().getName());
        }
    }


    public T defaultDispatch(J ignoredC, P1 ignoredP1, P2 ignoredP2) {
        throw new UnsupportedOperationException();
    }

    public T visitArrayAccess(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitAssert(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitAssignment(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitAssignmentOperation(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitBinary(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitBlock(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitBreak(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitCase(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitClassDeclaration(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitCompilationUnit(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitContinue(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitControlParentheses(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitDoWhileLoop(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitEmpty(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitEnumValue(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitEnumValueSet(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitFieldAccess(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitForeachLoop(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitForLoop(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitForLoopControl(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitIdentifier(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitInstanceOf(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitIf(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitIfElse(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitLabel(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitLambda(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitLiteral(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitMethodDeclaration(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitMethodInvocation(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitMultiCatch(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitNewArray(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitNewClass(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitParentheses(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitReturn(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitSwitch(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitTernary(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitThrow(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitTry(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitTypeCast(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitUnary(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitVariableDeclarations(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitVariableDeclarationsNamedVariable(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitWhileLoop(J pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }
}
