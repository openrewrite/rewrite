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
package org.openrewrite.groovy.tree;

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaDispatcher1;

public interface GroovyDispatcher1<T, P1> extends JavaDispatcher1<T, P1> {

    default T dispatch(J pp, P1 p1) {
        if (pp instanceof J.ArrayAccess) {
            return visitArrayAccess((J.ArrayAccess) pp, p1);
        } else if (pp instanceof J.Assert) {
            return visitAssert((J.Assert) pp, p1);
        } else if (pp instanceof J.Assignment) {
            return visitAssignment((J.Assignment) pp, p1);
        } else if (pp instanceof J.AssignmentOperation) {
            return visitAssignmentOperation((J.AssignmentOperation) pp, p1);
        } else if (pp instanceof J.Binary) {
            return visitBinary((J.Binary) pp, p1);
        } else if (pp instanceof J.Block) {
            return visitBlock((J.Block) pp, p1);
        } else if (pp instanceof J.Break) {
            return visitBreak((J.Break) pp, p1);
        } else if (pp instanceof J.Case) {
            return visitCase((J.Case) pp, p1);
        } else if (pp instanceof J.ClassDeclaration) {
            return visitClassDeclaration((J.ClassDeclaration) pp, p1);
        } else if (pp instanceof J.CompilationUnit) {
            return visitCompilationUnit((J.CompilationUnit) pp, p1);
        } else if (pp instanceof J.Continue) {
            return visitContinue((J.Continue) pp, p1);
        } else if (pp instanceof J.ControlParentheses) {
            return visitControlParentheses((J.ControlParentheses<?>) pp, p1);
        } else if (pp instanceof J.DoWhileLoop) {
            return visitDoWhileLoop((J.DoWhileLoop) pp, p1);
        } else if (pp instanceof J.Empty) {
            return visitEmpty((J.Empty) pp, p1);
        } else if (pp instanceof J.EnumValue) {
            return visitEnumValue((J.EnumValue) pp, p1);
        } else if (pp instanceof J.EnumValueSet) {
            return visitEnumValueSet((J.EnumValueSet) pp, p1);
        } else if (pp instanceof J.FieldAccess) {
            return visitFieldAccess((J.FieldAccess) pp, p1);
        } else if (pp instanceof J.ForEachLoop) {
            return visitForeachLoop((J.ForEachLoop) pp, p1);
        } else if (pp instanceof J.ForEachLoop.Control) {
            return visitForeachLoopControl((J.ForEachLoop.Control) pp, p1);
        } else if (pp instanceof J.ForLoop) {
            return visitForLoop((J.ForLoop) pp, p1);
        } else if (pp instanceof J.ForLoop.Control) {
            return visitForLoopControl((J.ForLoop.Control) pp, p1);
        } else if (pp instanceof J.Identifier) {
            return visitIdentifier((J.Identifier) pp, p1);
        } else if (pp instanceof J.InstanceOf) {
            return visitInstanceOf((J.InstanceOf) pp, p1);
        } else if (pp instanceof J.If) {
            return visitIf((J.If) pp, p1);
        } else if (pp instanceof J.If.Else) {
            return visitIfElse((J.If.Else) pp, p1);
        } else if (pp instanceof J.Label) {
            return visitLabel((J.Label) pp, p1);
        } else if (pp instanceof J.Lambda) {
            return visitLambda((J.Lambda) pp, p1);
        } else if (pp instanceof J.Literal) {
            return visitLiteral((J.Literal) pp, p1);
        } else if (pp instanceof J.MemberReference) {
            return visitMemberReference((J.MemberReference) pp, p1);
        } else if (pp instanceof J.MethodDeclaration) {
            return visitMethodDeclaration((J.MethodDeclaration) pp, p1);
        } else if (pp instanceof J.MethodInvocation) {
            return visitMethodInvocation((J.MethodInvocation) pp, p1);
        } else if (pp instanceof J.MultiCatch) {
            return visitMultiCatch((J.MultiCatch) pp, p1);
        } else if (pp instanceof J.NewArray) {
            return visitNewArray((J.NewArray) pp, p1);
        } else if (pp instanceof J.NewClass) {
            return visitNewClass((J.NewClass) pp, p1);
        } else if (pp instanceof J.Parentheses) {
            return visitParentheses((J.Parentheses<?>) pp, p1);
        } else if (pp instanceof J.Return) {
            return visitReturn((J.Return) pp, p1);
        } else if (pp instanceof J.Switch) {
            return visitSwitch((J.Switch) pp, p1);
        } else if (pp instanceof J.Synchronized) {
            return visitSynchronized((J.Synchronized) pp, p1);
        } else if (pp instanceof J.Ternary) {
            return visitTernary((J.Ternary) pp, p1);
        } else if (pp instanceof J.Throw) {
            return visitThrow((J.Throw) pp, p1);
        } else if (pp instanceof J.Try) {
            return visitTry((J.Try) pp, p1);
        } else if (pp instanceof J.Try.Resource) {
            return visitTryResource((J.Try.Resource) pp, p1);
        } else if (pp instanceof J.TypeCast) {
            return visitTypeCast((J.TypeCast) pp, p1);
        } else if (pp instanceof J.Unary) {
            return visitUnary((J.Unary) pp, p1);
        } else if (pp instanceof J.VariableDeclarations) {
            return visitVariableDeclarations((J.VariableDeclarations) pp, p1);
        } else if (pp instanceof J.VariableDeclarations.NamedVariable) {
            return visitVariableDeclarationsNamedVariable((J.VariableDeclarations.NamedVariable) pp, p1);
        } else if (pp instanceof J.WhileLoop) {
            return visitWhileLoop((J.WhileLoop) pp, p1);
        } else if (pp instanceof G.MapEntry) {
            return visitMapEntry((G.MapEntry) pp, p1);
        } else if (pp instanceof G.MapLiteral) {
            return visitMapLiteral((G.MapLiteral) pp, p1);
        } else if (pp instanceof G.ListLiteral) {
            return visitListLiteral((G.ListLiteral) pp, p1);
        } else if (pp instanceof G.GString) {
            return visitGString((G.GString) pp, p1);
        } else if (pp instanceof G.Binary) {
            return visitGBinary((G.Binary) pp, p1);
        }
        throw new Error("Unexpected node type: " + pp.getClass().getName());
    }

    default T visitMapEntry(G.MapEntry pp, P1 p1) {
        return defaultDispatch(pp, p1);
    }

    default T visitMapLiteral(G.MapLiteral pp, P1 p1) {
        return defaultDispatch(pp, p1);
    }

    default T visitListLiteral(G.ListLiteral pp, P1 p1) {
        return defaultDispatch(pp, p1);
    }

    default T visitGString(G.GString pp, P1 p1) {
        return defaultDispatch(pp, p1);
    }

    default T visitGBinary(G.Binary pp, P1 p1) {
        return defaultDispatch(pp, p1);
    }
}
