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

import org.openrewrite.Incubating;
import org.openrewrite.java.tree.*;

@Incubating(since = "7.25.0")
public class Reads implements JavaDispatcher1<Boolean, JavaType.Variable> {

    private static final ReadSided READ_SIDED = new ReadSided();

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
        return READ_SIDED.reads(assignment.getVariable(), v, Side.LVALUE) || READ_SIDED.reads(assignment.getAssignment(), v, Side.RVALUE);
    }

    @Override
    public Boolean visitBinary(J.Binary pp, JavaType.Variable v) {
        return reads(pp.getLeft(), v) || reads(pp.getRight(), v);
    }

    @Override
    public Boolean visitBlock(J.Block pp, JavaType.Variable v) {
        for (Statement s : pp.getStatements()) {
            if (reads(s, v)) {
                return true;
            }
        }
        return false;
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
        for (Statement statement : control.getInit()) {
            if (reads(statement, v)) {
                return true;
            }
        }
        for (Statement s : control.getUpdate()) {
            if (reads(s, v)) {
                return true;
            }
        }
        return reads(control.getCondition(), v);
    }

    @Override
    public Boolean visitIdentifier(J.Identifier ident, JavaType.Variable v) {
        return READ_SIDED.reads(ident, v, Side.RVALUE);
    }

    @Override
    public Boolean visitIf(J.If iff, JavaType.Variable v) {
        return reads(iff.getIfCondition(), v) || reads(iff.getThenPart(), v) || iff.getElsePart() != null && reads(iff.getElsePart().getBody(), v);
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
        if (method.getSelect() != null && reads(method.getSelect(), v)) {
            return true;
        }
        for (Expression e : method.getArguments()) {
            if (reads(e, v)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitNewArray(J.NewArray newArray, JavaType.Variable v) {
        if (newArray.getInitializer() != null) {
            for (Expression e : newArray.getInitializer()) {
                if (reads(e, v)) {
                    return true;
                }
            }
        }
        for (J.ArrayDimension e : newArray.getDimensions()) {
            if (reads(e.getIndex(), v)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitNewClass(J.NewClass newClass, JavaType.Variable v) {
        if (newClass.getEnclosing() != null && reads(newClass.getEnclosing(), v)) {
            return true;
        }
        if (newClass.getArguments() != null) {
            for (Expression e : newClass.getArguments()) {
                if (reads(e, v)) {
                    return true;
                }
            }
        }
        return newClass.getBody() != null && reads(newClass.getBody(), v);
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
        if (tryable.getResources() != null) {
            for (J.Try.Resource c : tryable.getResources()) {
                if (reads(c, v)) {
                    return true;
                }
            }
        }
        if (reads(tryable.getBody(), v)) {
            return true;
        }
        for (J.Try.Catch c : tryable.getCatches()) {
            if (reads(c.getBody(), v)) {
                return true;
            }
        }
        return tryable.getFinally() != null && reads(tryable.getFinally(), v);
    }

    @Override
    public Boolean visitTryResource(J.Try.Resource tryResource, JavaType.Variable v) {
        return tryResource.getVariableDeclarations() instanceof J.VariableDeclarations && reads(tryResource.getVariableDeclarations(), v);
    }

    @Override
    public Boolean visitUnary(J.Unary pp, JavaType.Variable v) {
        return reads(pp.getExpression(), v);
    }

    @Override
    public Boolean visitVariableDeclarations(J.VariableDeclarations pp, JavaType.Variable v) {
        for (J.VariableDeclarations.NamedVariable n : pp.getVariables()) {
            if (n.getInitializer() != null && reads(n.getInitializer(), v)) {
                return true;
            }
        }
        return false;
    }
}

