package com.netflix.java.refactor.ast.visitor

import com.netflix.java.refactor.ast.Cursor
import com.netflix.java.refactor.ast.Expression
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.Tree

class RetrieveCursorVisitor(val t: Tree): AstVisitor<Cursor?>(null) {

    override fun visitAnnotation(annotation: Tr.Annotation) = if(annotation === t) cursor() else super.visitAnnotation(annotation)

    override fun visitArrayAccess(arrayAccess: Tr.ArrayAccess) = if(arrayAccess === t) cursor() else super.visitArrayAccess(arrayAccess)

    override fun visitArrayType(arrayType: Tr.ArrayType) = if(arrayType === t) cursor() else super.visitArrayType(arrayType)

    override fun visitAssign(assign: Tr.Assign) = if(assign === t) cursor() else super.visitAssign(assign)

    override fun visitAssignOp(assign: Tr.AssignOp) = if(assign === t) cursor() else super.visitAssignOp(assign)

    override fun visitBinary(binary: Tr.Binary) = if(binary === t) cursor() else super.visitBinary(binary)

    override fun visitBlock(block: Tr.Block<Tree>) = if(block === t) cursor() else super.visitBlock(block)

    override fun visitBreak(breakStatement: Tr.Break) = if(breakStatement === t) cursor() else super.visitBreak(breakStatement)

    override fun visitCase(case: Tr.Case) = if(case === t) cursor() else super.visitCase(case)

    override fun visitCatch(catch: Tr.Catch) = if(catch === t) cursor() else super.visitCatch(catch)

    override fun visitClassDecl(classDecl: Tr.ClassDecl) = if(classDecl === t) cursor() else super.visitClassDecl(classDecl)

    override fun visitCompilationUnit(cu: Tr.CompilationUnit) = if(cu === t) cursor() else super.visitCompilationUnit(cu)

    override fun visitContinue(continueStatement: Tr.Continue) = if(continueStatement === t) cursor() else super.visitContinue(continueStatement)

    override fun visitDoWhileLoop(doWhileLoop: Tr.DoWhileLoop) = if(doWhileLoop === t) cursor() else super.visitDoWhileLoop(doWhileLoop)

    override fun visitEmpty(empty: Tr.Empty) = if(empty === t) cursor() else super.visitEmpty(empty)

    override fun visitEnumValue(enum: Tr.EnumValue) = if(enum === t) cursor() else super.visitEnumValue(enum)

    override fun visitEnumValueSet(enums: Tr.EnumValueSet) = if(enums === t) cursor() else super.visitEnumValueSet(enums)

    override fun visitExpression(expr: Expression) = if(expr === t) cursor() else super.visitExpression(expr)

    override fun visitFieldAccess(field: Tr.FieldAccess) = if(field === t) cursor() else super.visitFieldAccess(field)

    override fun visitForLoop(forLoop: Tr.ForLoop) = if(forLoop === t) cursor() else super.visitForLoop(forLoop)

    override fun visitForEachLoop(forEachLoop: Tr.ForEachLoop) = if(forEachLoop === t) cursor() else super.visitForEachLoop(forEachLoop)

    override fun visitIdentifier(ident: Tr.Ident) = if(ident === t) cursor() else super.visitIdentifier(ident)

    override fun visitIf(iff: Tr.If) = if(iff === t) cursor() else super.visitIf(iff)

    override fun visitImport(import: Tr.Import) = if(import === t) cursor() else super.visitImport(import)

    override fun visitInstanceOf(instanceOf: Tr.InstanceOf) = if(instanceOf === t) cursor() else super.visitInstanceOf(instanceOf)

    override fun visitLabel(label: Tr.Label) = if(label === t) cursor() else super.visitLabel(label)

    override fun visitLambda(lambda: Tr.Lambda) = if(lambda === t) cursor() else super.visitLambda(lambda)

    override fun visitLiteral(literal: Tr.Literal) = if(literal === t) cursor() else super.visitLiteral(literal)

    override fun visitMethod(method: Tr.MethodDecl) = if(method === t) cursor() else super.visitMethod(method)

    override fun visitMethodInvocation(meth: Tr.MethodInvocation) = if(meth === t) cursor() else super.visitMethodInvocation(meth)

    override fun visitMultiCatch(multiCatch: Tr.MultiCatch) = if(multiCatch === t) cursor() else super.visitMultiCatch(multiCatch)

    override fun visitMultiVariable(multiVariable: Tr.VariableDecls) = if(multiVariable === t) cursor() else super.visitMultiVariable(multiVariable)

    override fun visitNewArray(newArray: Tr.NewArray) = if(newArray === t) cursor() else super.visitNewArray(newArray)

    override fun visitNewClass(newClass: Tr.NewClass) = if(newClass === t) cursor() else super.visitNewClass(newClass)

    override fun visitPackage(pkg: Tr.Package) = if(pkg === t) cursor() else super.visitPackage(pkg)

    override fun visitParameterizedType(type: Tr.ParameterizedType) = if(type === t) cursor() else super.visitParameterizedType(type)

    override fun <T : Tree> visitParentheses(parens: Tr.Parentheses<T>) = if(parens === t) cursor() else super.visitParentheses(parens)

    override fun visitPrimitive(primitive: Tr.Primitive) = if(primitive === t) cursor() else super.visitPrimitive(primitive)

    override fun visitReturn(retrn: Tr.Return) = if(retrn === t) cursor() else super.visitReturn(retrn)

    override fun visitSwitch(switch: Tr.Switch) = if(switch === t) cursor() else super.visitSwitch(switch)

    override fun visitSynchronized(synch: Tr.Synchronized) = if(synch === t) cursor() else super.visitSynchronized(synch)

    override fun visitTernary(ternary: Tr.Ternary) = if(ternary === t) cursor() else super.visitTernary(ternary)

    override fun visitThrow(thrown: Tr.Throw) = if(thrown === t) cursor() else super.visitThrow(thrown)

    override fun visitTry(tryable: Tr.Try) = if(tryable === t) cursor() else super.visitTry(tryable)

    override fun visitTypeCast(typeCast: Tr.TypeCast) = if(typeCast === t) cursor() else super.visitTypeCast(typeCast)

    override fun visitTypeParameter(typeParameter: Tr.TypeParameter) = if(typeParameter === t) cursor() else super.visitTypeParameter(typeParameter)

    override fun visitTypeParameters(typeParameters: Tr.TypeParameters) = if(typeParameters === t) cursor() else super.visitTypeParameters(typeParameters)

    override fun visitUnary(unary: Tr.Unary) = if(unary === t) cursor() else super.visitUnary(unary)

    override fun visitUnparsedSource(unparsed: Tr.UnparsedSource) = if(unparsed === t) cursor() else super.visitUnparsedSource(unparsed)

    override fun visitVariable(variable: Tr.VariableDecls.NamedVar) = if(variable === t) cursor() else super.visitVariable(variable)

    override fun visitWhileLoop(whileLoop: Tr.WhileLoop) = if(whileLoop === t) cursor() else super.visitWhileLoop(whileLoop)

    override fun visitWildcard(wildcard: Tr.Wildcard) = if(wildcard === t) cursor() else super.visitWildcard(wildcard)
}