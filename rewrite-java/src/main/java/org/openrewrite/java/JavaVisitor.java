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
package org.openrewrite.java;

import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;

public interface JavaVisitor<R, P> extends TreeVisitor<R, P> {
    R visitAnnotatedType(J.AnnotatedType annotatedType, P p);
    R visitAnnotation(J.Annotation annotation, P p);
    R visitArrayAccess(J.ArrayAccess arrayAccess, P p);
    R visitArrayDimension(J.ArrayDimension arrayDimension, P p);
    R visitArrayType(J.ArrayType arrayType, P p);
    R visitAssert(J.Assert azzert, P p);
    R visitAssign(J.Assign assign, P p);
    R visitAssignOp(J.AssignOp assignOp, P p);
    R visitBinary(J.Binary binary, P p);
    R visitBlock(J.Block block, P p);
    R visitBreak(J.Break breakStatement, P p);
    R visitCase(J.Case caze, P p);
    R visitCatch(J.Try.Catch catzh, P p);
    R visitClassDecl(J.ClassDecl classDecl, P p);
    R visitCompilationUnit(J.CompilationUnit cu, P p);
    R visitContinue(J.Continue continueStatement, P p);
    R visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p);
    R visitElse(J.If.Else elze, P p);
    R visitEmpty(J.Empty empty, P p);
    R visitEnumValue(J.EnumValue enoom, P p);
    R visitEnumValueSet(J.EnumValueSet enums, P p);
    R visitFieldAccess(J.FieldAccess fieldAccess, P p);
    R visitForEachLoop(J.ForEachLoop forEachLoop, P p);
    R visitForLoop(J.ForLoop forLoop, P p);
    R visitIdentifier(J.Ident ident, P p);
    R visitIf(J.If iff, P p);
    R visitImport(J.Import impoort, P p);
    R visitInstanceOf(J.InstanceOf instanceOf, P p);
    R visitLabel(J.Label label, P p);
    R visitLambda(J.Lambda lambda, P p);
    R visitLiteral(J.Literal literal, P p);
    R visitMemberReference(J.MemberReference memberRef, P p);
    R visitMethod(J.MethodDecl method, P p);
    R visitMethodInvocation(J.MethodInvocation method, P p);
    R visitMultiCatch(J.MultiCatch multiCatch, P p);
    R visitMultiVariable(J.VariableDecls multiVariable, P p);
    R visitNewArray(J.NewArray newArray, P p);
    R visitNewClass(J.NewClass newClass, P p);
    R visitPackage(J.Package pkg, P p);
    R visitParameterizedType(J.ParameterizedType type, P p);
    <T extends J> R visitParentheses(J.Parentheses<T> parens, P p);
    R visitPrimitive(J.Primitive primitive, P p);
    R visitReturn(J.Return retrn, P p);
    R visitSwitch(J.Switch switzh, P p);
    R visitSynchronized(J.Synchronized synch, P p);
    R visitTernary(J.Ternary ternary, P p);
    R visitThrow(J.Throw thrown, P p);
    R visitTry(J.Try tryable, P p);
    R visitTypeCast(J.TypeCast typeCast, P p);
    R visitTypeParameter(J.TypeParameter typeParam, P p);
    R visitUnary(J.Unary unary, P p);
    R visitVariable(J.VariableDecls.NamedVar variable, P p);
    R visitWhileLoop(J.WhileLoop whileLoop, P p);
    R visitWildcard(J.Wildcard wildcard, P p);
}
