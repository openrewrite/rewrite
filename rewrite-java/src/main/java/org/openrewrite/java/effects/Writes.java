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

import java.util.Arrays;

public class Writes implements Dispatch1<Boolean, JavaType.Variable> {

    public static final WriteSided WRITE_SIDED = new WriteSided();

    /** @return True if this expression, when evaluated, may write variable v. */
    public boolean writes(J e, JavaType.Variable v) {
        return dispatch(e, v);
    }

    @Override
    public Boolean defaultDispatch(J ignoredC, JavaType.Variable ignoredP1) {
        throw new Error();
    }

    @Override
    public Boolean visitArrayAccess(J.ArrayAccess pp, JavaType.Variable v) {
        return WRITE_SIDED.writes(pp, v, Side.RVALUE);
    }

    @Override
    public Boolean visitAssert(J.Assert pp, JavaType.Variable v) {
        return writes(pp.getCondition(), v);
    }

    @Override
    public Boolean visitAssignment(J.Assignment pp, JavaType.Variable v) {
        return WRITE_SIDED.writes(pp.getVariable(), v, Side.LVALUE) || WRITE_SIDED.writes(pp.getAssignment(), v, Side.RVALUE);
    }

    @Override
    public Boolean visitAssignmentOperation(J.AssignmentOperation pp, JavaType.Variable v) {
        return WRITE_SIDED.writes(pp.getVariable(), v, Side.LVALUE) || writes(pp.getAssignment(), v);
    }

    @Override
    public Boolean visitBinary(J.Binary pp, JavaType.Variable v) {
        return writes(pp.getLeft(), v) || writes(pp.getRight(), v);
    }

    @Override
    public Boolean visitBlock(J.Block pp, JavaType.Variable v) {
        return pp.getStatements().stream().map(s -> writes(s, v)).reduce(false, (a,b) -> a|b);
    }

    @Override
    public Boolean visitBreak(J.Break pp, JavaType.Variable variable) {
        return false;
    }

    @Override
    public Boolean visitCase(J.Case pp, JavaType.Variable v) {
        return pp.getStatements().stream().map(s -> writes(s, v)).reduce(false, (a,b) -> a|b);
    }

    @Override
    public Boolean visitContinue(J.Continue pp, JavaType.Variable variable) {
        return false;
    }

    @Override
    public Boolean visitDoWhileLoop(J.DoWhileLoop pp, JavaType.Variable v) {
        return writes(pp.getWhileCondition(), v) || writes(pp.getBody(), v);
    }

    @Override
    public Boolean visitEmpty(J.Empty pp, JavaType.Variable variable) {
        return false;
    }

    @Override
    public Boolean visitEnumValueSet(J.EnumValueSet pp, JavaType.Variable v) {
        return pp.getEnums().stream().map(n -> n.getInitializer() != null && writes(n.getInitializer(), v)).reduce(false, (a, b) -> a|b);
    }

    @Override
    public Boolean visitFieldAccess(J.FieldAccess pp, JavaType.Variable v) {
        return WRITE_SIDED.writes(pp, v, Side.RVALUE);
    }

    @Override
    public Boolean visitForeachLoop(J.ForEachLoop pp, JavaType.Variable v) {
        return writes(pp.getControl(), v) || writes(pp.getBody(), v);
    }

    @Override
    public Boolean visitForeachLoopControl(J.ForEachLoop.Control pp, JavaType.Variable v) {
        return writes(pp.getVariable(), v) || writes(pp.getIterable(), v);
    }

    @Override
    public Boolean visitForLoop(J.ForLoop pp, JavaType.Variable v) {
        return writes(pp.getControl(), v) || writes(pp.getBody(), v);
    }

    @Override
    public Boolean visitForLoopControl(J.ForLoop.Control pp, JavaType.Variable v) {
        return pp.getInit().stream().map(s -> writes(s, v)).reduce(false, (a,b) -> a|b) ||
                pp.getUpdate().stream().map(s -> writes(s, v)).reduce(false, (a,b) -> a|b) ||
                writes(pp.getCondition(), v);
    }

    @Override
    public Boolean visitIdentifier(J.Identifier pp, JavaType.Variable v) {
        return WRITE_SIDED.writes(pp, v, Side.RVALUE);
    }

    @Override
    public Boolean visitIf(J.If pp, JavaType.Variable v) {
        return writes(pp.getIfCondition(), v) || writes(pp.getThenPart(), v) ||
                (pp.getElsePart() != null && writes(pp.getElsePart().getBody(), v));
    }

    @Override
    public Boolean visitIfElse(J.If.Else pp, JavaType.Variable v) {
        return writes(pp.getBody(), v);
    }

    @Override
    public Boolean visitInstanceOf(J.InstanceOf pp, JavaType.Variable v) {
        return writes(pp.getExpression(), v);
    }

    @Override
    public Boolean visitLabel(J.Label pp, JavaType.Variable variable) {
        return false;
    }

    @Override
    public Boolean visitLambda(J.Lambda pp, JavaType.Variable v) {
        return pp.getBody() instanceof Expression && writes((Expression)pp.getBody(), v);
    }

    @Override
    public Boolean visitLiteral(J.Literal pp, JavaType.Variable variable) {
        return false;
    }

    @Override
    public Boolean visitMemberReference(J.MemberReference pp, JavaType.Variable v) {
        // Here we assume that v is a local variable, so it cannot be referenced by a member reference.
        // However there might be references to v in the expression.
        return writes(pp.getContaining(), v);
    }

    @Override
    public Boolean visitMethodInvocation(J.MethodInvocation pp, JavaType.Variable v) {
        // This does not take into account the effects inside the method body.
        // As long as v is a local variable, we are guaranteed that it cannot be affected
        // as a side-effect of the method invocation.
        return (pp.getSelect() != null && writes(pp.getSelect(), v))
                || pp.getArguments().stream().map(e -> writes(e, v)).reduce(false, (a,b) -> a|b);
    }

    @Override
    public Boolean visitNewArray(J.NewArray pp, JavaType.Variable v) {
        return (pp.getInitializer() != null && pp.getInitializer().stream().map(e -> writes(e, v)).reduce(false, (a,b) -> a|b))
                || pp.getDimensions().stream().map(e -> writes(e.getIndex(), v)).reduce(false, (a,b) -> a|b);
    }

    @Override
    public Boolean visitNewClass(J.NewClass pp, JavaType.Variable v) {
        return (pp.getEnclosing() != null && writes(pp.getEnclosing(), v))
                || (pp.getArguments() != null && pp.getArguments().stream().map(e -> writes(e, v)).reduce(false, (a,b) -> a|b))
                || (pp.getBody() != null && writes(pp.getBody(), v));
    }

    @Override
    public Boolean visitParentheses(J.Parentheses<?> pp, JavaType.Variable v) {
        return writes((Expression)pp.getTree(), v);
    }

    @Override
    public Boolean visitControlParentheses(J.ControlParentheses<?> pp, JavaType.Variable v) {
        return writes((Expression)pp.getTree(), v);
    }

    @Override
    public Boolean visitReturn(J.Return pp, JavaType.Variable v) {
        return pp.getExpression() != null && writes(pp.getExpression(), v);
    }

    @Override
    public Boolean visitSwitch(J.Switch pp, JavaType.Variable v) {
        return writes(pp.getSelector(), v) || writes(pp.getCases(), v);
    }

    @Override
    public Boolean visitSynchronized(J.Synchronized pp, JavaType.Variable v) {
        return writes(pp.getLock(), v) || writes(pp.getBody(), v);
    }

    @Override
    public Boolean visitTernary(J.Ternary pp, JavaType.Variable v) {
        return writes(pp.getCondition(), v) || writes(pp.getTruePart(), v) || writes(pp.getFalsePart(), v);
    }

    @Override
    public Boolean visitThrow(J.Throw pp, JavaType.Variable v) {
        return writes(pp.getException(), v);
    }

    @Override
    public Boolean visitTry(J.Try pp, JavaType.Variable v) {
        return (pp.getResources() != null && pp.getResources().stream().map(c -> writes(c, v)).reduce(false, (a,b) -> a|b))
                || writes(pp.getBody(), v)
                || pp.getCatches().stream().map(c -> writes(c.getBody(), v)).reduce(false, (a,b) -> a|b)
                || (pp.getFinally() != null && writes(pp.getFinally(), v));
    }

    @Override
    public Boolean visitTryResource(J.Try.Resource pp, JavaType.Variable v) {
        return pp.getVariableDeclarations() instanceof J.VariableDeclarations
                && writes((J.VariableDeclarations)pp.getVariableDeclarations(), v);
    }

    @Override
    public Boolean visitTypeCast(J.TypeCast pp, JavaType.Variable v) {
        return writes(pp.getExpression(), v);
    }

    @Override
    public Boolean visitUnary(J.Unary pp, JavaType.Variable v) {
        if(Arrays.asList(new J.Unary.Type[]{ J.Unary.Type.PreIncrement, J.Unary.Type.PreDecrement, J.Unary.Type. PostIncrement, J.Unary.Type.PostDecrement })
                .contains(pp.getOperator())) {
            // expr = expr + 1, expr = 1 + expr, ...: expr appears on both sides
            return WRITE_SIDED.writes(pp.getExpression(), v, Side.LVALUE) || WRITE_SIDED.writes(pp.getExpression(), v, Side.RVALUE);
        }
        return writes(pp.getExpression(), v);
    }

    @Override
    public Boolean visitVariableDeclarations(J.VariableDeclarations pp, JavaType.Variable v) {
        return pp.getVariables().stream().map(n -> n.getInitializer() != null && writes(n.getInitializer(), v)).reduce(false, (a,b) -> a|b);
    }

    @Override
    public Boolean visitWhileLoop(J.WhileLoop pp, JavaType.Variable v) {
        return writes(pp.getCondition(), v) || writes(pp.getBody(), v);
    }
}
