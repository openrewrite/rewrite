package org.openrewrite.java.tree;

public abstract class Dispatch2<T,P1,P2> {

    private String className(J pp) {
        return pp.getClass().getName().replaceAll("^org.openrewrite.java.tree.", "");
    }

    public final T dispatch(J pp, P1 p1, P2 p2) {
        switch (className(pp)) {
            case "J$ArrayAccess":
                return visitArrayAccess((J.ArrayAccess)pp, p1, p2);
            case "J$Assert":
                return visitAssert((J.Assert)pp, p1, p2);
            case "J$Assignment":
                return visitAssignment((J.Assignment)pp, p1, p2);
            case "J$AssignmentOperation":
                return visitAssignmentOperation((J.AssignmentOperation)pp, p1, p2);
            case "J$Binary":
                return visitBinary((J.Binary)pp, p1, p2);
            case "J$Block":
                return visitBlock((J.Block)pp, p1, p2);
            case "J$Break":
                return visitBreak((J.Break)pp, p1, p2);
            case "J$Case":
                return visitCase((J.Case)pp, p1, p2);
            case "J$ClassDeclaration":
                return visitClassDeclaration((J.ClassDeclaration)pp, p1, p2);
            case "J$CompilationUnit":
                return visitCompilationUnit((J.CompilationUnit)pp, p1, p2);
            case "J$Continue":
                return visitContinue((J.Continue)pp, p1, p2);
            case "J$ControlParentheses":
                return visitControlParentheses((J.ControlParentheses)pp, p1, p2);
            case "J$DoWhileLoop":
                return visitDoWhileLoop((J.DoWhileLoop)pp, p1, p2);
            case "J$Empty":
                return visitEmpty((J.Empty)pp, p1, p2);
            case "J$EnumValue":
                return visitEnumValue((J.EnumValue)pp, p1, p2);
            case "J$EnumValueSet":
                return visitEnumValueSet((J.EnumValueSet)pp, p1, p2);
            case "J$FieldAccess":
                return visitFieldAccess((J.FieldAccess)pp, p1, p2);
            case "J$ForeachLoop":
                return visitForeachLoop((J.ForEachLoop)pp, p1, p2);
            case "J$ForeachLoop$Control":
                return visitForeachLoopControl((J.ForEachLoop.Control)pp, p1, p2);
            case "J$ForLoop":
                return visitForLoop((J.ForLoop)pp, p1, p2);
            case "J$ForLoop$Control":
                return visitForLoopControl((J.ForLoop.Control)pp, p1, p2);
            case "J$Identifier":
                return visitIdentifier((J.Identifier)pp, p1, p2);
            case "J$InstanceOf":
                return visitInstanceOf((J.InstanceOf)pp, p1, p2);
            case "J$If":
                return visitIf((J.If)pp, p1, p2);
            case "J$If$Else":
                return visitIfElse((J.If.Else)pp, p1, p2);
            case "J$Label":
                return visitLabel((J.Label)pp, p1, p2);
            case "J$Lambda":
                return visitLambda((J.Lambda)pp, p1, p2);
            case "J$Literal":
                return visitLiteral((J.Literal)pp, p1, p2);
            case "J$MemberReference":
                return visitMemberReference((J.MemberReference)pp, p1, p2);
            case "J$MethodDeclaration":
                return visitMethodDeclaration((J.MethodDeclaration)pp, p1, p2);
            case "J$MethodInvocation":
                return visitMethodInvocation((J.MethodInvocation)pp, p1, p2);
            case "J$MultiCatch":
                return visitMultiCatch((J.MultiCatch)pp, p1, p2);
            case "J$NewArray":
                return visitNewArray((J.NewArray)pp, p1, p2);
            case "J$NewClass":
                return visitNewClass((J.NewClass)pp, p1, p2);
            case "J$Parentheses":
                return visitParentheses((J.Parentheses)pp, p1, p2);
            case "J$Return":
                return visitReturn((J.Return)pp, p1, p2);
            case "J$Switch":
                return visitSwitch((J.Switch)pp, p1, p2);
            case "J$Synchronized":
                return visitSynchronized((J.Synchronized)pp, p1, p2);
            case "J$Ternary":
                return visitTernary((J.Ternary)pp, p1, p2);
            case "J$Throw":
                return visitThrow((J.Throw)pp, p1, p2);
            case "J$Try":
                return visitTry((J.Try)pp, p1, p2);
            case "J$Try$Resource":
                return visitTryResource((J.Try.Resource)pp, p1, p2);
            case "J$TypeCast":
                return visitTypeCast((J.TypeCast)pp, p1, p2);
            case "J$Unary":
                return visitUnary((J.Unary)pp, p1, p2);
            case "J$VariableDeclarations":
                return visitVariableDeclarations((J.VariableDeclarations)pp, p1, p2);
            case "J$VariableDeclarations$NamedVariable":
                return visitVariableDeclarationsNamedVariable((J.VariableDeclarations.NamedVariable)pp, p1, p2);
            case "J$WhileLoop":
                return visitWhileLoop((J.WhileLoop)pp, p1, p2);

            default:
                throw new Error("Unexpected node type: " + pp.getClass().getName());
        }
    }


    public T defaultDispatch(J ignoredC, P1 ignoredP1, P2 ignoredP2) {
        throw new UnsupportedOperationException();
    }

    public T visitArrayAccess(J.ArrayAccess pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitAssert(J.Assert pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitAssignment(J.Assignment pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitAssignmentOperation(J.AssignmentOperation pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitBinary(J.Binary pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitBlock(J.Block pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitBreak(J.Break pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitCase(J.Case pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitClassDeclaration(J.ClassDeclaration pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitCompilationUnit(J.CompilationUnit pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitContinue(J.Continue pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitControlParentheses(J.ControlParentheses pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitDoWhileLoop(J.DoWhileLoop pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitEmpty(J.Empty pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitEnumValue(J.EnumValue pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitEnumValueSet(J.EnumValueSet pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitFieldAccess(J.FieldAccess pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitForeachLoop(J.ForEachLoop pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitForeachLoopControl(J.ForEachLoop.Control pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitForLoop(J.ForLoop pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitForLoopControl(J.ForLoop.Control pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitIdentifier(J.Identifier pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitInstanceOf(J.InstanceOf pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitIf(J.If pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitIfElse(J.If.Else pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitLabel(J.Label pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitLambda(J.Lambda pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitLiteral(J.Literal pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitMemberReference(J.MemberReference pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitMethodDeclaration(J.MethodDeclaration pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitMethodInvocation(J.MethodInvocation pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitMultiCatch(J.MultiCatch pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitNewArray(J.NewArray pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitNewClass(J.NewClass pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitParentheses(J.Parentheses pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitReturn(J.Return pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitSwitch(J.Switch pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitSynchronized(J.Synchronized pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitTernary(J.Ternary pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitThrow(J.Throw pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitTry(J.Try pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitTryResource(J.Try.Resource pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitTypeCast(J.TypeCast pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitUnary(J.Unary pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitVariableDeclarations(J.VariableDeclarations pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitVariableDeclarationsNamedVariable(J.VariableDeclarations.NamedVariable pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }

    public T visitWhileLoop(J.WhileLoop pp, P1 p1, P2 p2) {
        return defaultDispatch(pp, p1, p2);
    }
}
