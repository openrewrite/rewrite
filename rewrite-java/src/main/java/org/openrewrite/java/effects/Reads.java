/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.effects;

import org.openrewrite.java.tree.Dispatch1;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

public class Reads implements Dispatch1<Boolean, JavaType.Variable> {

    public static final ReadSided READ_SIDED = new ReadSided();

    /**
     * @return Whether this expression may read variable v.
     */
    public boolean reads(J e, JavaType.Variable v) {
        return dispatch(e, v);
    }

    @Override
    public Boolean defaultDispatch(J ignoredC, JavaType.Variable ignoredP1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitArrayAccess(J.ArrayAccess arrayAccess, JavaType.Variable v) {
        return READ_SIDED.visitArrayAccess(arrayAccess, v, Side.RVALUE);
    }

    @Override
    public Boolean visitAssignment(J.Assignment assignment, JavaType.Variable v) {
        return READ_SIDED.reads(assignment.getVariable(), v, Side.LVALUE)
                || READ_SIDED.reads(assignment.getAssignment(), v, Side.RVALUE);
    }

    @Override
    public Boolean visitBinary(J.Binary pp, JavaType.Variable v) {
        return reads(pp.getLeft(), v) || reads(pp.getRight(), v);
    }

    @Override
    public Boolean visitBlock(J.Block pp, JavaType.Variable v) {
        return pp.getStatements().stream().map(s -> reads(s, v)).reduce(false, (a, b) -> a | b);
    }

    @Override
    public Boolean visitControlParentheses(J.ControlParentheses<?> controlParens, JavaType.Variable v) {
        return controlParens.getTree() instanceof Expression && reads(controlParens.getTree(), v);
    }

    @Override
    public Boolean visitFieldAccess(J.FieldAccess fieldAccess, JavaType.Variable variable) {
        return READ_SIDED.reads(fieldAccess, variable, Side.RVALUE);
    }

    @Override
    public Boolean visitForeachLoop(J.ForEachLoop forLoop, JavaType.Variable v) {
        return reads(forLoop.getControl(), v) || reads(forLoop.getBody(), v);
    }

    @Override
    public Boolean visitForeachLoopControl(J.ForEachLoop.Control control, JavaType.Variable v) {
        return reads(control.getVariable(), v) || reads(control.getIterable(), v);
    }

    @Override
    public Boolean visitForLoop(J.ForLoop forLoop, JavaType.Variable v) {
        return reads(forLoop.getControl(), v) || reads(forLoop.getBody(), v);
    }

    @Override
    public Boolean visitForLoopControl(J.ForLoop.Control control, JavaType.Variable v) {
        return control.getInit().stream().map(s -> reads(s, v)).reduce(false, (a, b) -> a | b)
                || control.getUpdate().stream().map(s -> reads(s, v)).reduce(false, (a, b) -> a | b)
                || reads(control.getCondition(), v);
    }

    @Override
    public Boolean visitIdentifier(J.Identifier ident, JavaType.Variable v) {
        return READ_SIDED.reads(ident, v, Side.RVALUE);
    }

    @Override
    public Boolean visitIf(J.If iff, JavaType.Variable v) {
        return reads(iff.getIfCondition(), v)
                || reads(iff.getThenPart(), v)
                || (iff.getElsePart() != null && reads(iff.getElsePart().getBody(), v));
    }

    @Override
    public Boolean visitIfElse(J.If.Else pp, JavaType.Variable v) {
        return reads(pp.getBody(), v);
    }

    @Override
    public Boolean visitInstanceOf(J.InstanceOf instanceOf, JavaType.Variable v) {
        return reads(instanceOf.getExpression(), v);
    }

    @Override
    public Boolean visitLabel(J.Label label, JavaType.Variable variable) {
        return false;
    }

    @Override
    public Boolean visitLambda(J.Lambda lambda, JavaType.Variable v) {
        return lambda.getBody() instanceof Expression && reads(lambda.getBody(), v);
    }

    @Override
    public Boolean visitLiteral(J.Literal literal, JavaType.Variable variable) {
        return false;
    }

    @Override
    public Boolean visitMemberReference(J.MemberReference memberRef, JavaType.Variable v) {
        // Here we assume that v is a local variable, so it cannot be referenced by a member reference.
        // However, there might be references to v in the expression.
        return reads(memberRef.getContaining(), v);
    }

    @Override
    public Boolean visitMethodInvocation(J.MethodInvocation method, JavaType.Variable v) {
        // This does not take into account the effects inside the method body.
        // As long as v is a local variable, we are guaranteed that it cannot be affected
        // as a side effect of the method invocation.
        return (method.getSelect() != null && reads(method.getSelect(), v))
                || method.getArguments().stream().map(e -> reads(e, v)).reduce(false, (a, b) -> a | b);
    }

    @Override
    public Boolean visitNewArray(J.NewArray newArray, JavaType.Variable v) {
        return (newArray.getInitializer() != null && newArray.getInitializer().stream().map(e -> reads(e, v)).reduce(false, (a, b) -> a | b))
                || newArray.getDimensions().stream().map(e -> reads(e.getIndex(), v)).reduce(false, (a, b) -> a | b);
    }

    @Override
    public Boolean visitNewClass(J.NewClass newClass, JavaType.Variable v) {
        return (newClass.getEnclosing() != null && reads(newClass.getEnclosing(), v))
                || (newClass.getArguments() != null && newClass.getArguments().stream().map(e -> reads(e, v)).reduce(false, (a, b) -> a | b))
                || (newClass.getBody() != null && reads(newClass.getBody(), v));
    }

    @Override
    public Boolean visitParentheses(J.Parentheses<?> parens, JavaType.Variable v) {
        return reads(parens.getTree(), v);
    }

    @Override
    public Boolean visitReturn(J.Return retrn, JavaType.Variable v) {
        return retrn.getExpression() != null && reads(retrn.getExpression(), v);
    }

    @Override
    public Boolean visitSwitch(J.Switch switzh, JavaType.Variable v) {
        return reads(switzh.getSelector(), v) || reads(switzh.getCases(), v);
    }

    @Override
    public Boolean visitSynchronized(J.Synchronized synch, JavaType.Variable v) {
        return reads(synch.getLock(), v) || reads(synch.getBody(), v);
    }

    @Override
    public Boolean visitTernary(J.Ternary ternary, JavaType.Variable v) {
        return reads(ternary.getCondition(), v) || reads(ternary.getTruePart(), v) || reads(ternary.getFalsePart(), v);
    }

    @Override
    public Boolean visitThrow(J.Throw thrown, JavaType.Variable v) {
        return reads(thrown.getException(), v);
    }

    @Override
    public Boolean visitTry(J.Try tryable, JavaType.Variable v) {
        return (tryable.getResources() != null && tryable.getResources().stream().map(c -> reads(c, v)).reduce(false, (a, b) -> a | b))
                || reads(tryable.getBody(), v)
                || tryable.getCatches().stream().map(c -> reads(c.getBody(), v)).reduce(false, (a, b) -> a | b)
                || (tryable.getFinally() != null && reads(tryable.getFinally(), v));
    }

    @Override
    public Boolean visitTryResource(J.Try.Resource tryResource, JavaType.Variable v) {
        return tryResource.getVariableDeclarations() instanceof J.VariableDeclarations
                && reads(tryResource.getVariableDeclarations(), v);
    }

    @Override
    public Boolean visitUnary(J.Unary pp, JavaType.Variable v) {
        return reads(pp.getExpression(), v);
    }

    @Override
    public Boolean visitVariableDeclarations(J.VariableDeclarations pp, JavaType.Variable v) {
        return pp.getVariables().stream().map(n -> n.getInitializer() != null && reads(n.getInitializer(), v)).reduce(false, (a, b) -> a | b);
    }
}

