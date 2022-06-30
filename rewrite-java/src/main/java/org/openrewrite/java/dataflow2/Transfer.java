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
package org.openrewrite.java.dataflow2;

import org.openrewrite.Cursor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaDispatcher3;
import org.openrewrite.java.tree.JavaType;

public class Transfer<T> implements JavaDispatcher3<ProgramState<T>, Cursor, ProgramState<T>, TraversalControl<ProgramState<T>>> {

    private final DataFlowAnalysis<T> dfa;

    public Transfer(DataFlowAnalysis<T> dfa) {
        this.dfa = dfa;
    }

    @Override
    public ProgramState<T> visitArrayAccess(J.ArrayAccess pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        J.ArrayAccess ac = c.getValue();
        // TODO
        return inputState.push(dfa.joiner.lowerBound());
    }

    @Override
    public ProgramState<T> visitAssert(J.Assert pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        // TODO
        return inputState.push(dfa.joiner.lowerBound());
    }

    @Override
    public ProgramState<T> visitAssignment(J.Assignment pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        J.Assignment a = c.getValue();
        if (a.getVariable() instanceof J.Identifier) {
            J.Identifier ident = (J.Identifier) a.getVariable();
            return inputState.set(ident.getFieldType(), inputState.expr());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public ProgramState<T> visitBinary(J.Binary pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState.pop(2).push(dfa.joiner.lowerBound());
    }

    @Override
    public ProgramState<T> visitBlock(J.Block pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState;
    }

    @Override
    public ProgramState<T> visitControlParentheses(J.ControlParentheses<?> pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState;
    }

    @Override
    public ProgramState<T> visitEmpty(J.Empty pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState;
    }

    @Override
    public ProgramState<T> visitForLoop(J.ForLoop pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        // TODO
        return defaultDispatch(pp, c, inputState, t);
    }

    @Override
    public ProgramState<T> visitForLoopControl(J.ForLoop.Control pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        // TODO
        return defaultDispatch(pp, c, inputState, t);
    }

    @Override
    public ProgramState<T> visitFieldAccess(J.FieldAccess pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        // TODO
        return inputState.push(dfa.joiner.lowerBound());
    }

    @Override
    public ProgramState<T> visitIdentifier(J.Identifier pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        J.Identifier i = c.getValue();
        T v = inputState.get(i.getFieldType());
        return inputState.push(v);
    }

    @Override
    public ProgramState<T> visitIf(J.If pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState;
    }

    @Override
    public ProgramState<T> visitIfElse(J.If.Else pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState;
    }

    @Override
    public ProgramState<T> visitLiteral(J.Literal pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState.push(dfa.joiner.lowerBound());
    }

    @Override
    public ProgramState<T> visitMethodInvocation(J.MethodInvocation pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        J.MethodInvocation method = c.getValue();
        int d = dfa.stackSizeBefore(method);
        ProgramState<T> result = inputState.pop(d);
        return result.push(dfa.joiner.lowerBound());
    }

    @Override
    public ProgramState<T> visitVariableDeclarationsNamedVariable(J.VariableDeclarations.NamedVariable pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> tc) {
        J.VariableDeclarations.NamedVariable v = c.getValue();
        JavaType.Variable t = v.getVariableType();
        if (v.getInitializer() != null) {
            //ProgramState s = analysis(v.getInitializer());
            return inputState.set(t, inputState.expr()).pop();
        } else {
            //ProgramState s = inputState(c, tc);
            assert !inputState.getMap().containsKey(t);
            return inputState.set(t, dfa.joiner.defaultInitialization()).pop();
        }
    }

    @Override
    public ProgramState<T> visitNewClass(J.NewClass pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        J.NewClass newClass = c.getValue();
        int d = dfa.stackSizeBefore(newClass);
        ProgramState<T> result = inputState.pop(d);
        return result.push(dfa.joiner.lowerBound());
    }

    @Override
    public ProgramState<T> visitParentheses(J.Parentheses<?> pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState;
    }

    @Override
    public ProgramState<T> visitUnary(J.Unary pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState.pop().push(dfa.joiner.lowerBound());
    }

    @Override
    public ProgramState<T> visitVariableDeclarations(J.VariableDeclarations pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return defaultDispatch(pp, c, inputState, t);
    }

    @Override
    public ProgramState<T> visitWhileLoop(J.WhileLoop pp, Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return defaultDispatch(pp, c, inputState, t);
    }

//    // dispatch functions for virtual nodes
//
//    @Override
//    public ProgramState<T> visitToIfThenElseBranches(J.If ifThenElse, ProgramState<T> s, String ifThenElseBranch) {
//        return s;
//    }
}
