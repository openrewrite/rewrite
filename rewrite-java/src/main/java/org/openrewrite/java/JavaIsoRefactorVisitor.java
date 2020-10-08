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

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.Statement;


/**
 * This iso(morphic) refactoring visitor is the appropriate base class for most Java refactoring visitors.
 * It comes with an additional constraint compared to the non-isomorphic JavaRefactorVisitor:
 * Each visit method must return an AST element of the same type as the one being visited.
 *
 * For visitors that do not need the extra flexibility of JavaRefactorVisitor, this constraint
 * makes for a more pleasant visitor authoring experience as less casting will be required.
 */
public class JavaIsoRefactorVisitor extends JavaRefactorVisitor {

    @Override
    public Statement visitStatement(Statement statement) {
        return (Statement) super.visitStatement(statement);
    }

    @Override
    public J.AnnotatedType visitAnnotatedType(J.AnnotatedType annotatedType) {
        return (J.AnnotatedType) super.visitAnnotatedType(annotatedType);
    }

    @Override
    public J.Annotation visitAnnotation(J.Annotation annotation) {
        return (J.Annotation) super.visitAnnotation(annotation);
    }

    @Override
    public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess) {
        return (J.ArrayAccess) super.visitArrayAccess(arrayAccess);
    }

    @Override
    public J.ArrayType visitArrayType(J.ArrayType arrayType) {
        return (J.ArrayType) super.visitArrayType(arrayType);
    }

    @Override
    public J.Assert visitAssert(J.Assert azzert) {
        return (J.Assert) super.visitAssert(azzert);
    }

    @Override
    public J.Assign visitAssign(J.Assign assign) {
        return (J.Assign) super.visitAssign(assign);
    }

    @Override
    public J.AssignOp visitAssignOp(J.AssignOp assignOp) {
        return (J.AssignOp) super.visitAssignOp(assignOp);
    }

    @Override
    public J.Binary visitBinary(J.Binary binary) {
        return (J.Binary) super.visitBinary(binary);
    }

    @SuppressWarnings("unchecked")
    @Override
    public J.Block<J> visitBlock(J.Block<J> block) {
        return (J.Block<J>) super.visitBlock(block);
    }

    @Override
    public J.Break visitBreak(J.Break breakStatement) {
        return (J.Break) super.visitBreak(breakStatement);
    }

    @Override
    public J.Case visitCase(J.Case caze) {
        return (J.Case) super.visitCase(caze);
    }

    @Override
    public J.Try.Catch visitCatch(J.Try.Catch catzh) {
        return (J.Try.Catch) super.visitCatch(catzh);
    }

    @Override
    public J.ClassDecl visitClassDecl(J.ClassDecl classDecl) {
        return (J.ClassDecl) super.visitClassDecl(classDecl);
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu) {
        return (J.CompilationUnit) super.visitCompilationUnit(cu);
    }

    @Override
    public J.Continue visitContinue(J.Continue continueStatement) {
        return (J.Continue) super.visitContinue(continueStatement);
    }

    @Override
    public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop) {
        return (J.DoWhileLoop) super.visitDoWhileLoop(doWhileLoop);
    }

    @Override
    public J.If.Else visitElse(J.If.Else elze) {
        return (J.If.Else) super.visitElse(elze);
    }

    @Override
    public J.Empty visitEmpty(J.Empty empty) {
        return (J.Empty) super.visitEmpty(empty);
    }

    @Override
    public J.EnumValue visitEnumValue(J.EnumValue enoom) {
        return (J.EnumValue) super.visitEnumValue(enoom);
    }

    @Override
    public J.EnumValueSet visitEnumValueSet(J.EnumValueSet enums) {
        return (J.EnumValueSet) super.visitEnumValueSet(enums);
    }

    @Override
    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess) {
        return (J.FieldAccess) super.visitFieldAccess(fieldAccess);
    }

    @Override
    public J.Try.Finally visitFinally(J.Try.Finally finallie) {
        return (J.Try.Finally) super.visitFinally(finallie);
    }

    @Override
    public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop) {
        return (J.ForEachLoop) super.visitForEachLoop(forLoop);
    }

    @Override
    public J.ForLoop visitForLoop(J.ForLoop forLoop) {
        return (J.ForLoop) super.visitForLoop(forLoop);
    }

    @Override
    public J.Ident visitIdentifier(J.Ident ident) {
        return (J.Ident) super.visitIdentifier(ident);
    }

    @Override
    public J.If visitIf(J.If iff) {
        return (J.If) super.visitIf(iff);
    }

    @Override
    public J.Import visitImport(J.Import impoort) {
        return (J.Import) super.visitImport(impoort);
    }

    @Override
    public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf) {
        return (J.InstanceOf) super.visitInstanceOf(instanceOf);
    }

    @Override
    public J.Label visitLabel(J.Label label) {
        return (J.Label) super.visitLabel(label);
    }

    @Override
    public J.Lambda visitLambda(J.Lambda lambda) {
        return (J.Lambda) super.visitLambda(lambda);
    }

    @Override
    public J.Literal visitLiteral(J.Literal literal) {
        return (J.Literal) super.visitLiteral(literal);
    }

    @Override
    public J.MemberReference visitMemberReference(J.MemberReference memberRef) {
        return (J.MemberReference) super.visitMemberReference(memberRef);
    }

    @Override
    public J.MethodDecl visitMethod(J.MethodDecl method) {
        return (J.MethodDecl) super.visitMethod(method);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method) {
        return (J.MethodInvocation) super.visitMethodInvocation(method);
    }

    @Override
    public J.MultiCatch visitMultiCatch(J.MultiCatch multiCatch) {
        return (J.MultiCatch) super.visitMultiCatch(multiCatch);
    }

    @Override
    public J.VariableDecls visitMultiVariable(J.VariableDecls multiVariable) {
        return (J.VariableDecls) super.visitMultiVariable(multiVariable);
    }

    @Override
    public J.NewArray visitNewArray(J.NewArray newArray) {
        return (J.NewArray) super.visitNewArray(newArray);
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass) {
        return (J.NewClass) super.visitNewClass(newClass);
    }

    @Override
    public J.Package visitPackage(J.Package pkg) {
        return (J.Package) super.visitPackage(pkg);
    }

    @Override
    public J.ParameterizedType visitParameterizedType(J.ParameterizedType type) {
        return (J.ParameterizedType) super.visitParameterizedType(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens) {
        return (J.Parentheses<T>) super.visitParentheses(parens);
    }

    @Override
    public J.Primitive visitPrimitive(J.Primitive primitive) {
        return (J.Primitive) super.visitPrimitive(primitive);
    }

    @Override
    public J.Return visitReturn(J.Return retrn) {
        return (J.Return) super.visitReturn(retrn);
    }

    @Override
    public J.Switch visitSwitch(J.Switch switzh) {
        return (J.Switch) super.visitSwitch(switzh);
    }

    @Override
    public J.Synchronized visitSynchronized(J.Synchronized synch) {
        return (J.Synchronized) super.visitSynchronized(synch);
    }

    @Override
    public J.Ternary visitTernary(J.Ternary ternary) {
        return (J.Ternary) super.visitTernary(ternary);
    }

    @Override
    public J.Throw visitThrow(J.Throw thrown) {
        return (J.Throw) super.visitThrow(thrown);
    }

    @Override
    public J.Try visitTry(J.Try tryable) {
        return (J.Try) super.visitTry(tryable);
    }

    @Override
    public J.TypeCast visitTypeCast(J.TypeCast typeCast) {
        return (J.TypeCast) super.visitTypeCast(typeCast);
    }

    @Override
    public NameTree visitTypeName(NameTree name) {
        return (NameTree) super.visitTypeName(name);
    }

    @Override
    public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam) {
        return (J.TypeParameter) super.visitTypeParameter(typeParam);
    }

    @Override
    public J.TypeParameters visitTypeParameters(J.TypeParameters typeParams) {
        return (J.TypeParameters) super.visitTypeParameters(typeParams);
    }

    @Override
    public J.Unary visitUnary(J.Unary unary) {
        return (J.Unary) super.visitUnary(unary);
    }

    @Override
    public J.UnparsedSource visitUnparsedSource(J.UnparsedSource unparsed) {
        return (J.UnparsedSource) super.visitUnparsedSource(unparsed);
    }

    @Override
    public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable) {
        return (J.VariableDecls.NamedVar) super.visitVariable(variable);
    }

    @Override
    public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop) {
        return (J.WhileLoop) super.visitWhileLoop(whileLoop);
    }

    @Override
    public J.Wildcard visitWildcard(J.Wildcard wildcard) {
        return (J.Wildcard) super.visitWildcard(wildcard);
    }
}
