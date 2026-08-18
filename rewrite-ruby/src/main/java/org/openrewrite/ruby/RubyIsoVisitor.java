/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.ruby;

import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.ruby.tree.Rb;

public class RubyIsoVisitor<P> extends RubyVisitor<P> {

    @Override
    public Rb.Array visitArray(Rb.Array array, P p) {
        return (Rb.Array) super.visitArray(array, p);
    }

    @Override
    public Rb.Binary visitBinary(Rb.Binary binary, P p) {
        return (Rb.Binary) super.visitBinary(binary, p);
    }

    @Override
    public Rb.CompilationUnit visitCompilationUnit(Rb.CompilationUnit compilationUnit, P p) {
        return (Rb.CompilationUnit) super.visitCompilationUnit(compilationUnit, p);
    }

    @Override
    public Rb.ComplexString visitComplexString(Rb.ComplexString complexString, P p) {
        return (Rb.ComplexString) super.visitComplexString(complexString, p);
    }

    @Override
    public Rb.ComplexString.Value visitComplexStringValue(Rb.ComplexString.Value value, P p) {
        return (Rb.ComplexString.Value) super.visitComplexStringValue(value, p);
    }

    @Override
    public Rb.DataSection visitDataSection(Rb.DataSection dataSection, P p) {
        return (Rb.DataSection) super.visitDataSection(dataSection, p);
    }

    @Override
    public Rb.Hash visitHash(Rb.Hash hash, P p) {
        return (Rb.Hash) super.visitHash(hash, p);
    }

    @Override
    public Rb.Heredoc visitHeredoc(Rb.Heredoc heredoc, P p) {
        return (Rb.Heredoc) super.visitHeredoc(heredoc, p);
    }

    @Override
    public Rb.Hash.KeyValue visitKeyValue(Rb.Hash.KeyValue keyValue, P p) {
        return (Rb.Hash.KeyValue) super.visitKeyValue(keyValue, p);
    }

    @Override
    public Rb.MultipleAssignment visitMultipleAssignment(Rb.MultipleAssignment multipleAssignment, P p) {
        return (Rb.MultipleAssignment) super.visitMultipleAssignment(multipleAssignment, p);
    }

    @Override
    public Rb.Redo visitRedo(Rb.Redo redo, P p) {
        return (Rb.Redo) super.visitRedo(redo, p);
    }

    @Override
    public Rb.Yield visitYield(Rb.Yield yield, P p) {
        return (Rb.Yield) super.visitYield(yield, p);
    }

    @Override
    public Expression visitExpression(Expression expression, P p) {
        return (Expression) super.visitExpression(expression, p);
    }

    @Override
    public Statement visitStatement(Statement statement, P p) {
        return (Statement) super.visitStatement(statement, p);
    }

    @Override
    public J.AnnotatedType visitAnnotatedType(J.AnnotatedType annotatedType, P p) {
        return (J.AnnotatedType) super.visitAnnotatedType(annotatedType, p);
    }

    @Override
    public J.Annotation visitAnnotation(J.Annotation annotation, P p) {
        return (J.Annotation) super.visitAnnotation(annotation, p);
    }

    @Override
    public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, P p) {
        return (J.ArrayAccess) super.visitArrayAccess(arrayAccess, p);
    }

    @Override
    public J.ArrayDimension visitArrayDimension(J.ArrayDimension arrayDimension, P p) {
        return (J.ArrayDimension) super.visitArrayDimension(arrayDimension, p);
    }

    @Override
    public J.ArrayType visitArrayType(J.ArrayType arrayType, P p) {
        return (J.ArrayType) super.visitArrayType(arrayType, p);
    }

    @Override
    public J.Assert visitAssert(J.Assert _assert, P p) {
        return (J.Assert) super.visitAssert(_assert, p);
    }

    @Override
    public J.Assignment visitAssignment(J.Assignment assignment, P p) {
        return (J.Assignment) super.visitAssignment(assignment, p);
    }

    @Override
    public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, P p) {
        return (J.AssignmentOperation) super.visitAssignmentOperation(assignOp, p);
    }

    @Override
    public J.Binary visitBinary(J.Binary binary, P p) {
        return (J.Binary) super.visitBinary(binary, p);
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        return (J.Block) super.visitBlock(block, p);
    }

    @Override
    public J.Break visitBreak(J.Break breakStatement, P p) {
        return (J.Break) super.visitBreak(breakStatement, p);
    }

    @Override
    public J.Case visitCase(J.Case _case, P p) {
        return (J.Case) super.visitCase(_case, p);
    }

    @Override
    public J.Try.Catch visitCatch(J.Try.Catch _catch, P p) {
        return (J.Try.Catch) super.visitCatch(_catch, p);
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        return (J.ClassDeclaration) super.visitClassDeclaration(classDecl, p);
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, P p) {
        return (J.CompilationUnit) super.visitCompilationUnit(cu, p);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends J> J.ControlParentheses<T> visitControlParentheses(J.ControlParentheses<T> controlParens, P p) {
        return (J.ControlParentheses<T>) super.visitControlParentheses(controlParens, p);
    }

    @Override
    public J.Continue visitContinue(J.Continue continueStatement, P p) {
        return (J.Continue) super.visitContinue(continueStatement, p);
    }

    @Override
    public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        return (J.DoWhileLoop) super.visitDoWhileLoop(doWhileLoop, p);
    }

    @Override
    public J.If.Else visitElse(J.If.Else else_, P p) {
        return (J.If.Else) super.visitElse(else_, p);
    }

    @Override
    public J.Empty visitEmpty(J.Empty empty, P p) {
        return (J.Empty) super.visitEmpty(empty, p);
    }

    @Override
    public J.EnumValue visitEnumValue(J.EnumValue _enum, P p) {
        return (J.EnumValue) super.visitEnumValue(_enum, p);
    }

    @Override
    public J.EnumValueSet visitEnumValueSet(J.EnumValueSet enums, P p) {
        return (J.EnumValueSet) super.visitEnumValueSet(enums, p);
    }

    @Override
    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, P p) {
        return (J.FieldAccess) super.visitFieldAccess(fieldAccess, p);
    }

    @Override
    public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, P p) {
        return (J.ForEachLoop) super.visitForEachLoop(forLoop, p);
    }

    @Override
    public J.ForEachLoop.Control visitForEachControl(J.ForEachLoop.Control control, P p) {
        return (J.ForEachLoop.Control) super.visitForEachControl(control, p);
    }

    @Override
    public J.ForLoop visitForLoop(J.ForLoop forLoop, P p) {
        return (J.ForLoop) super.visitForLoop(forLoop, p);
    }

    @Override
    public J.ForLoop.Control visitForControl(J.ForLoop.Control control, P p) {
        return (J.ForLoop.Control) super.visitForControl(control, p);
    }

    @Override
    public J.Identifier visitIdentifier(J.Identifier identifier, P p) {
        return (J.Identifier) super.visitIdentifier(identifier, p);
    }

    @Override
    public J.If visitIf(J.If iff, P p) {
        return (J.If) super.visitIf(iff, p);
    }

    @Override
    public J.Import visitImport(J.Import _import, P p) {
        return (J.Import) super.visitImport(_import, p);
    }

    @Override
    public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, P p) {
        return (J.InstanceOf) super.visitInstanceOf(instanceOf, p);
    }

    @Override
    public J.IntersectionType visitIntersectionType(J.IntersectionType intersectionType, P p) {
        return (J.IntersectionType) super.visitIntersectionType(intersectionType, p);
    }

    @Override
    public J.Label visitLabel(J.Label label, P p) {
        return (J.Label) super.visitLabel(label, p);
    }

    @Override
    public J.Lambda visitLambda(J.Lambda lambda, P p) {
        return (J.Lambda) super.visitLambda(lambda, p);
    }

    @Override
    public J.Literal visitLiteral(J.Literal literal, P p) {
        return (J.Literal) super.visitLiteral(literal, p);
    }

    @Override
    public J.MemberReference visitMemberReference(J.MemberReference memberRef, P p) {
        return (J.MemberReference) super.visitMemberReference(memberRef, p);
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        return (J.MethodDeclaration) super.visitMethodDeclaration(method, p);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        return (J.MethodInvocation) super.visitMethodInvocation(method, p);
    }

    @Override
    public J.MultiCatch visitMultiCatch(J.MultiCatch multiCatch, P p) {
        return (J.MultiCatch) super.visitMultiCatch(multiCatch, p);
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        return (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, p);
    }

    @Override
    public J.NewArray visitNewArray(J.NewArray newArray, P p) {
        return (J.NewArray) super.visitNewArray(newArray, p);
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, P p) {
        return (J.NewClass) super.visitNewClass(newClass, p);
    }

    @Override
    public J.NullableType visitNullableType(J.NullableType nullableType, P p) {
        return (J.NullableType) super.visitNullableType(nullableType, p);
    }

    @Override
    public J.Package visitPackage(J.Package pkg, P p) {
        return (J.Package) super.visitPackage(pkg, p);
    }

    @Override
    public J.ParameterizedType visitParameterizedType(J.ParameterizedType type, P p) {
        return (J.ParameterizedType) super.visitParameterizedType(type, p);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens, P p) {
        return (J.Parentheses<T>) super.visitParentheses(parens, p);
    }

    @Override
    public J.Primitive visitPrimitive(J.Primitive primitive, P p) {
        return (J.Primitive) super.visitPrimitive(primitive, p);
    }

    @Override
    public J.Return visitReturn(J.Return _return, P p) {
        return (J.Return) super.visitReturn(_return, p);
    }

    @Override
    public J.Switch visitSwitch(J.Switch _switch, P p) {
        return (J.Switch) super.visitSwitch(_switch, p);
    }

    @Override
    public J.SwitchExpression visitSwitchExpression(J.SwitchExpression _switch, P p) {
        return (J.SwitchExpression) super.visitSwitchExpression(_switch, p);
    }

    @Override
    public J.Synchronized visitSynchronized(J.Synchronized _sync, P p) {
        return (J.Synchronized) super.visitSynchronized(_sync, p);
    }

    @Override
    public J.Ternary visitTernary(J.Ternary ternary, P p) {
        return (J.Ternary) super.visitTernary(ternary, p);
    }

    @Override
    public J.Throw visitThrow(J.Throw thrown, P p) {
        return (J.Throw) super.visitThrow(thrown, p);
    }

    @Override
    public J.Try visitTry(J.Try _try, P p) {
        return (J.Try) super.visitTry(_try, p);
    }

    @Override
    public J.Try.Resource visitTryResource(J.Try.Resource tryResource, P p) {
        return (J.Try.Resource) super.visitTryResource(tryResource, p);
    }

    @Override
    public J.TypeCast visitTypeCast(J.TypeCast typeCast, P p) {
        return (J.TypeCast) super.visitTypeCast(typeCast, p);
    }

    @Override
    public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam, P p) {
        return (J.TypeParameter) super.visitTypeParameter(typeParam, p);
    }

    @Override
    public J.Unary visitUnary(J.Unary unary, P p) {
        return (J.Unary) super.visitUnary(unary, p);
    }

    @Override
    public J.Unknown visitUnknown(J.Unknown unknown, P p) {
        return (J.Unknown) super.visitUnknown(unknown, p);
    }

    @Override
    public J.Unknown.Source visitUnknownSource(J.Unknown.Source source, P p) {
        return (J.Unknown.Source) super.visitUnknownSource(source, p);
    }

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        return (J.VariableDeclarations.NamedVariable) super.visitVariable(variable, p);
    }

    @Override
    public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, P p) {
        return (J.WhileLoop) super.visitWhileLoop(whileLoop, p);
    }

    @Override
    public J.Wildcard visitWildcard(J.Wildcard wildcard, P p) {
        return (J.Wildcard) super.visitWildcard(wildcard, p);
    }

    @Override
    public J.Yield visitYield(J.Yield yield, P p) {
        return (J.Yield) super.visitYield(yield, p);
    }
}
