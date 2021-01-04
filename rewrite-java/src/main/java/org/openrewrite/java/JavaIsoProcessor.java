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

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

/**
 * This iso(morphic) refactoring visitor is the appropriate base class for most Java refactoring visitors.
 * It comes with an additional constraint compared to the non-isomorphic JavaRefactorVisitor:
 * Each visit method must return an AST element of the same type as the one being visited.
 *
 * For visitors that do not need the extra flexibility of JavaRefactorVisitor, this constraint
 * makes for a more pleasant visitor authoring experience as less casting will be required.
 */
public class JavaIsoProcessor extends JavaProcessor {
    @Override
    public Expression visitExpression(Expression expression, ExecutionContext ctx) {
        return (Expression) super.visitExpression(expression, ctx);
    }

    @Override
    public Statement visitStatement(Statement statement, ExecutionContext ctx) {
        return (Statement) super.visitStatement(statement, ctx);
    }

    @Override
    public J.AnnotatedType visitAnnotatedType(J.AnnotatedType annotatedType, ExecutionContext ctx) {
        return (J.AnnotatedType) super.visitAnnotatedType(annotatedType, ctx);
    }

    @Override
    public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
        return (J.Annotation) super.visitAnnotation(annotation, ctx);
    }

    @Override
    public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, ExecutionContext ctx) {
        return (J.ArrayAccess) super.visitArrayAccess(arrayAccess, ctx);
    }

    @Override
    public J.ArrayDimension visitArrayDimension(J.ArrayDimension arrayDimension, ExecutionContext ctx) {
        return (J.ArrayDimension) super.visitArrayDimension(arrayDimension, ctx);
    }

    @Override
    public J.ArrayType visitArrayType(J.ArrayType arrayType, ExecutionContext ctx) {
        return (J.ArrayType) super.visitArrayType(arrayType, ctx);
    }

    @Override
    public J.Assert visitAssert(J.Assert _assert, ExecutionContext ctx) {
        return (J.Assert) super.visitAssert(_assert, ctx);
    }

    @Override
    public J.Assign visitAssign(J.Assign assign, ExecutionContext ctx) {
        return (J.Assign) super.visitAssign(assign, ctx);
    }

    @Override
    public J.AssignOp visitAssignOp(J.AssignOp assignOp, ExecutionContext ctx) {
        return (J.AssignOp) super.visitAssignOp(assignOp, ctx);
    }

    @Override
    public J.Binary visitBinary(J.Binary binary, ExecutionContext ctx) {
        return (J.Binary) super.visitBinary(binary, ctx);
    }

    @Override
    public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
        return (J.Block) super.visitBlock(block, ctx);
    }

    @Override
    public J.Break visitBreak(J.Break breakStatement, ExecutionContext ctx) {
        return (J.Break) super.visitBreak(breakStatement, ctx);
    }

    @Override
    public J.Case visitCase(J.Case _case, ExecutionContext ctx) {
        return (J.Case) super.visitCase(_case, ctx);
    }

    @Override
    public J.Try.Catch visitCatch(J.Try.Catch _catch, ExecutionContext ctx) {
        return (J.Try.Catch) super.visitCatch(_catch, ctx);
    }

    @Override
    public J.ClassDecl visitClassDecl(J.ClassDecl classDecl, ExecutionContext ctx) {
        return (J.ClassDecl) super.visitClassDecl(classDecl, ctx);
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
        return (J.CompilationUnit) super.visitCompilationUnit(cu, ctx);
    }

    @Override
    public J.Continue visitContinue(J.Continue continueStatement, ExecutionContext ctx) {
        return (J.Continue) super.visitContinue(continueStatement, ctx);
    }

    @Override
    public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, ExecutionContext ctx) {
        return (J.DoWhileLoop) super.visitDoWhileLoop(doWhileLoop, ctx);
    }

    @Override
    public J.If.Else visitElse(J.If.Else elze, ExecutionContext ctx) {
        return (J.If.Else) super.visitElse(elze, ctx);
    }

    @Override
    public J.Empty visitEmpty(J.Empty empty, ExecutionContext ctx) {
        return (J.Empty) super.visitEmpty(empty, ctx);
    }

    @Override
    public J.EnumValue visitEnumValue(J.EnumValue _enum, ExecutionContext ctx) {
        return (J.EnumValue) super.visitEnumValue(_enum, ctx);
    }

    @Override
    public J.EnumValueSet visitEnumValueSet(J.EnumValueSet enums, ExecutionContext ctx) {
        return (J.EnumValueSet) super.visitEnumValueSet(enums, ctx);
    }

    @Override
    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
        return (J.FieldAccess) super.visitFieldAccess(fieldAccess, ctx);
    }

    @Override
    public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, ExecutionContext ctx) {
        return (J.ForEachLoop) super.visitForEachLoop(forLoop, ctx);
    }

    @Override
    public J.ForLoop visitForLoop(J.ForLoop forLoop, ExecutionContext ctx) {
        return (J.ForLoop) super.visitForLoop(forLoop, ctx);
    }

    @Override
    public J.Ident visitIdentifier(J.Ident identifier, ExecutionContext ctx) {
        return (J.Ident) super.visitIdentifier(identifier, ctx);
    }

    @Override
    public J.If visitIf(J.If iff, ExecutionContext ctx) {
        return (J.If) super.visitIf(iff, ctx);
    }

    @Override
    public J.Import visitImport(J.Import _import, ExecutionContext ctx) {
        return (J.Import) super.visitImport(_import, ctx);
    }

    @Override
    public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, ExecutionContext ctx) {
        return (J.InstanceOf) super.visitInstanceOf(instanceOf, ctx);
    }

    @Override
    public J.Label visitLabel(J.Label label, ExecutionContext ctx) {
        return (J.Label) super.visitLabel(label, ctx);
    }

    @Override
    public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext ctx) {
        return (J.Lambda) super.visitLambda(lambda, ctx);
    }

    @Override
    public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
        return (J.Literal) super.visitLiteral(literal, ctx);
    }

    @Override
    public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
        return (J.MemberReference) super.visitMemberReference(memberRef, ctx);
    }

    @Override
    public J.MethodDecl visitMethod(J.MethodDecl method, ExecutionContext ctx) {
        return (J.MethodDecl) super.visitMethod(method, ctx);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
        return (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
    }

    @Override
    public J.MultiCatch visitMultiCatch(J.MultiCatch multiCatch, ExecutionContext ctx) {
        return (J.MultiCatch) super.visitMultiCatch(multiCatch, ctx);
    }

    @Override
    public J.VariableDecls visitMultiVariable(J.VariableDecls multiVariable, ExecutionContext ctx) {
        return (J.VariableDecls) super.visitMultiVariable(multiVariable, ctx);
    }

    @Override
    public J.NewArray visitNewArray(J.NewArray newArray, ExecutionContext ctx) {
        return (J.NewArray) super.visitNewArray(newArray, ctx);
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
        return (J.NewClass) super.visitNewClass(newClass, ctx);
    }

    @Override
    public J.Package visitPackage(J.Package pkg, ExecutionContext ctx) {
        return (J.Package) super.visitPackage(pkg, ctx);
    }

    @Override
    public J.ParameterizedType visitParameterizedType(J.ParameterizedType type, ExecutionContext ctx) {
        return (J.ParameterizedType) super.visitParameterizedType(type, ctx);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens, ExecutionContext ctx) {
        return (J.Parentheses<T>) super.visitParentheses(parens, ctx);
    }

    @Override
    public J.Primitive visitPrimitive(J.Primitive primitive, ExecutionContext ctx) {
        return (J.Primitive) super.visitPrimitive(primitive, ctx);
    }

    @Override
    public J.Return visitReturn(J.Return _return, ExecutionContext ctx) {
        return (J.Return) super.visitReturn(_return, ctx);
    }

    @Override
    public J.Switch visitSwitch(J.Switch _switch, ExecutionContext ctx) {
        return (J.Switch) super.visitSwitch(_switch, ctx);
    }

    @Override
    public J.Synchronized visitSynchronized(J.Synchronized _sync, ExecutionContext ctx) {
        return (J.Synchronized) super.visitSynchronized(_sync, ctx);
    }

    @Override
    public J.Ternary visitTernary(J.Ternary ternary, ExecutionContext ctx) {
        return (J.Ternary) super.visitTernary(ternary, ctx);
    }

    @Override
    public J.Throw visitThrow(J.Throw thrown, ExecutionContext ctx) {
        return (J.Throw) super.visitThrow(thrown, ctx);
    }

    @Override
    public J.Try visitTry(J.Try _try, ExecutionContext ctx) {
        return (J.Try) super.visitTry(_try, ctx);
    }

    @Override
    public J.TypeCast visitTypeCast(J.TypeCast typeCast, ExecutionContext ctx) {
        return (J.TypeCast) super.visitTypeCast(typeCast, ctx);
    }

    @Override
    public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam, ExecutionContext ctx) {
        return (J.TypeParameter) super.visitTypeParameter(typeParam, ctx);
    }

    @Override
    public J.Unary visitUnary(J.Unary unary, ExecutionContext ctx) {
        return (J.Unary) super.visitUnary(unary, ctx);
    }

    @Override
    public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable, ExecutionContext ctx) {
        return (J.VariableDecls.NamedVar) super.visitVariable(variable, ctx);
    }

    @Override
    public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, ExecutionContext ctx) {
        return (J.WhileLoop) super.visitWhileLoop(whileLoop, ctx);
    }

    @Override
    public J.Wildcard visitWildcard(J.Wildcard wildcard, ExecutionContext ctx) {
        return (J.Wildcard) super.visitWildcard(wildcard, ctx);
    }
}
