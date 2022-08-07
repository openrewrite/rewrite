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
package org.openrewrite.java.controlflow;

import lombok.AllArgsConstructor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
final class ControlFlowJavaPrinter<P> extends JavaPrinter<P> {
    final List<J> nodesToPrint;

    @Override
    public J visitAssert(J.Assert azzert, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(azzert, p);
        return super.visitAssert(azzert, p);
    }

    @Override
    public J visitAssignment(J.Assignment assignment, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(assignment, p);
        return super.visitAssignment(assignment, p);
    }

    @Override
    public J visitAssignmentOperation(J.AssignmentOperation assignOp, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(assignOp, p);
        return super.visitAssignmentOperation(assignOp, p);
    }

    @Override
    public J visitBinary(J.Binary binary, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(binary, p);
        return super.visitBinary(binary, p);
    }

    @Override
    public J visitBlock(J.Block block, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(block, p);
        // Customize the block to change the prefix that starts this block
        // This results in a block that is more readable when printed
        J.Block customized = block.withPrefix(block.getEnd());
        return super.visitBlock(customized, p);
    }

    @Override
    protected void visitStatement(@Nullable JRightPadded<Statement> paddedStat, JRightPadded.Location location, PrintOutputCapture<P> p) {
        if (paddedStat == null) {
            return;
        }
        maybeEnableOrDisable(paddedStat.getElement(), p);
        super.visitStatement(paddedStat, location, p);
    }

    @Override
    public J visitBreak(J.Break breakStatement, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(breakStatement, p);
        return super.visitBreak(breakStatement, p);
    }

    @Override
    public J visitCase(J.Case caze, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(caze, p);
        return super.visitCase(caze, p);
    }

    @Override
    public J visitCatch(J.Try.Catch catzh, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(catzh, p);
        return super.visitCatch(catzh, p);
    }

    @Override
    public J visitClassDeclaration(J.ClassDeclaration classDecl, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(classDecl, p);
        return super.visitClassDeclaration(classDecl, p);
    }

    @Override
    public J visitContinue(J.Continue continueStatement, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(continueStatement, p);
        return super.visitContinue(continueStatement, p);
    }

    @Override
    public <T extends J> J visitControlParentheses(J.ControlParentheses<T> controlParens, PrintOutputCapture<P> p) {
        J.If enclosing = getCursor().firstEnclosing(J.If.class);
        if (enclosing != null && enclosing.getIfCondition() == controlParens) {
            return super.visitControlParentheses(controlParens, p);
        }
        J.WhileLoop enclosingLoop = getCursor().firstEnclosing(J.WhileLoop.class);
        if (enclosingLoop != null && enclosingLoop.getCondition() == controlParens) {
            return super.visitControlParentheses(controlParens, p);
        }
        J.DoWhileLoop enclosingDoWhileLoop = getCursor().firstEnclosing(J.DoWhileLoop.class);
        if (enclosingDoWhileLoop != null && enclosingDoWhileLoop.getWhileCondition() == controlParens) {
            return super.visitControlParentheses(controlParens, p);
        }
        maybeEnableOrDisable(controlParens, p);
        return super.visitControlParentheses(controlParens, p);
    }

    @Override
    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(doWhileLoop, p);
        return super.visitDoWhileLoop(doWhileLoop, p);
    }

    @Override
    public J visitEnumValue(J.EnumValue enoom, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(enoom, p);
        return super.visitEnumValue(enoom, p);
    }

    @Override
    public J visitEnumValueSet(J.EnumValueSet enums, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(enums, p);
        return super.visitEnumValueSet(enums, p);
    }

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(fieldAccess, p);
        return super.visitFieldAccess(fieldAccess, p);
    }

    @Override
    public J visitForLoop(J.ForLoop forLoop, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(forLoop, p);
        return super.visitForLoop(forLoop, p);
    }

    @Override
    public J visitForEachLoop(J.ForEachLoop forEachLoop, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(forEachLoop, p);
        return super.visitForEachLoop(forEachLoop, p);
    }

    @Override
    public J visitIdentifier(J.Identifier ident, PrintOutputCapture<P> p) {
        // If the variable side of an assignment is not a flow step, so we don't need to do anything
        J.Assignment parentAssignment = getCursor().firstEnclosing(J.Assignment.class);
        if (parentAssignment != null && parentAssignment.getVariable().unwrap() == ident) {
            return super.visitIdentifier(ident, p);
        }
        J.VariableDeclarations.NamedVariable parentNamedVariable = getCursor().firstEnclosing(J.VariableDeclarations.NamedVariable.class);
        if (parentNamedVariable != null && parentNamedVariable.getName() == ident) {
            return super.visitIdentifier(ident, p);
        }
        // If the identifier is a field access, don't modify the printer state
        J.FieldAccess parentFieldAccess = getCursor().firstEnclosing(J.FieldAccess.class);
        if (parentFieldAccess != null && parentFieldAccess.getName() == ident) {
            return super.visitIdentifier(ident, p);
        }
        // If the identifier is a new class name, don't modify the printer state
        J.NewClass parentNewClass = getCursor().firstEnclosing(J.NewClass.class);
        if (parentNewClass != null && parentNewClass.getClazz() == ident) {
            return super.visitIdentifier(ident, p);
        }
        // If the identifier is a method name, don't modify the printer state
        J.MethodInvocation parentMethodInvocation = getCursor().firstEnclosing(J.MethodInvocation.class);
        if (parentMethodInvocation != null && parentMethodInvocation.getName() == ident) {
            return super.visitIdentifier(ident, p);
        }
        maybeEnableOrDisable(ident, p);
        return super.visitIdentifier(ident, p);
    }

    @Override
    public J visitIf(J.If iff, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(iff, p);
        return super.visitIf(iff, p);
    }

    @Override
    public J visitElse(J.If.Else elze, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(elze, p);
        return super.visitElse(elze, p);
    }

    @Override
    public J visitInstanceOf(J.InstanceOf instanceOf, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(instanceOf, p);
        return super.visitInstanceOf(instanceOf, p);
    }

    @Override
    public J visitLambda(J.Lambda lambda, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(lambda, p);
        return super.visitLambda(lambda, p);
    }

    @Override
    public J visitLiteral(J.Literal literal, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(literal, p);
        return super.visitLiteral(literal, p);
    }

    @Override
    public J visitMemberReference(J.MemberReference memberRef, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(memberRef, p);
        return super.visitMemberReference(memberRef, p);
    }

    @Override
    public J visitMethodDeclaration(J.MethodDeclaration method, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(method, p);
        return super.visitMethodDeclaration(method, p);
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(method, p);
        return super.visitMethodInvocation(method, p);
    }

    @Override
    public J visitMultiCatch(J.MultiCatch multiCatch, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(multiCatch, p);
        return super.visitMultiCatch(multiCatch, p);
    }

    @Override
    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(multiVariable, p);
        return super.visitVariableDeclarations(multiVariable, p);
    }

    @Override
    public J visitNewArray(J.NewArray newArray, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(newArray, p);
        return super.visitNewArray(newArray, p);
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parens, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(parens, p);
        return super.visitParentheses(parens, p);
    }

    @Override
    public J visitSwitch(J.Switch switzh, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(switzh, p);
        return super.visitSwitch(switzh, p);
    }

    @Override
    public J visitSynchronized(J.Synchronized synch, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(synch, p);
        return super.visitSynchronized(synch, p);
    }

    @Override
    public J visitTernary(J.Ternary ternary, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(ternary, p);
        return super.visitTernary(ternary, p);
    }

    @Override
    public J visitThrow(J.Throw thrown, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(thrown, p);
        return super.visitThrow(thrown, p);
    }

    @Override
    public J visitTry(J.Try tryable, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(tryable, p);
        return super.visitTry(tryable, p);
    }

    @Override
    public J visitUnary(J.Unary unary, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(unary, p);
        return super.visitUnary(unary, p);
    }

    @Override
    public J visitVariable(J.VariableDeclarations.NamedVariable variable, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(variable, p);
        return super.visitVariable(variable, p);
    }

    @Override
    public J visitWhileLoop(J.WhileLoop whileLoop, PrintOutputCapture<P> p) {
        maybeEnableOrDisable(whileLoop, p);
        return super.visitWhileLoop(whileLoop, p);
    }

    @Override
    public J visitEmpty(J.Empty empty, PrintOutputCapture<P> pPrintOutputCapture) {
        J.MethodInvocation maybeParent = getCursor().firstEnclosing(J.MethodInvocation.class);
        if (maybeParent != null && maybeParent.getArguments().contains(empty)) {
            return super.visitEmpty(empty, pPrintOutputCapture);
        }
        maybeEnableOrDisable(empty, pPrintOutputCapture);
        return super.visitEmpty(
                empty.withPrefix(
                        Space.build(
                                " ",
                                ListUtils.concat(empty.getComments(), new TextComment(true, "Empty", "", Markers.EMPTY))
                        )
                ),
                pPrintOutputCapture
        );
    }

    static class ControlFlowPrintOutputCapture<P> extends PrintOutputCapture<P> {
        boolean enabled = false;

        public ControlFlowPrintOutputCapture(P p) {
            super(p);
        }

        @Override
        public PrintOutputCapture<P> append(char c) {
            if (enabled) {
                return super.append(c);
            } else {
                switch (c) {
                    case '\t':
                    case '\n':
                    case '\r':
                        return super.append(c);
                    default:
                        return super.append(' ');
                }
            }
        }

        @Override
        public PrintOutputCapture<P> append(@Nullable String text) {
            if (enabled) {
                return super.append(text);
            } else if (text != null) {
                return super.append(text.replaceAll(
                        "[^ \\t\\n\\r]",
                        " "));
            } else {
                return this;
            }
        }

        public void enable() {
            enabled = true;
        }

        public void disable() {
            enabled = false;
        }
    }

    private void maybeEnableOrDisable(J j, PrintOutputCapture<P> p) {
        if (nodesToPrint.contains(j)) {
            castPrint(p).enable();
        } else {
            castPrint(p).disable();
        }
    }

    private static <P> ControlFlowPrintOutputCapture<P> castPrint(PrintOutputCapture<P> print) {
        return (ControlFlowPrintOutputCapture<P>) print;
    }

}
