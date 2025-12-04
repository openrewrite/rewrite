/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.javascript;

import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.javascript.tree.JSX;

public class JavaScriptIsoVisitor<P> extends JavaScriptVisitor<P> {

    @Override
    public JS.CompilationUnit visitJsCompilationUnit(JS.CompilationUnit cu, P p) {
        return (JS.CompilationUnit) super.visitJsCompilationUnit(cu, p);
    }

    @Override
    public JS.Alias visitAlias(JS.Alias alias, P p) {
        return (JS.Alias) super.visitAlias(alias, p);
    }

    @Override
    public JS.ArrayBindingPattern visitArrayBindingPattern(JS.ArrayBindingPattern arrayBindingPattern, P p) {
        return (JS.ArrayBindingPattern) super.visitArrayBindingPattern(arrayBindingPattern, p);
    }

    @Override
    public JS.ArrowFunction visitArrowFunction(JS.ArrowFunction arrowFunction, P p) {
        return (JS.ArrowFunction) super.visitArrowFunction(arrowFunction, p);
    }

    @Override
    public JS.As visitAs(JS.As as_, P p) {
        return (JS.As) super.visitAs(as_, p);
    }

    @Override
    public JS.AssignmentOperation visitAssignmentOperationExtensions(JS.AssignmentOperation assignOp, P p) {
        return (JS.AssignmentOperation) super.visitAssignmentOperationExtensions(assignOp, p);
    }

    @Override
    public JS.Await visitAwait(JS.Await await, P p) {
        return (JS.Await) super.visitAwait(await, p);
    }

    @Override
    public JS.BindingElement visitBindingElement(JS.BindingElement binding, P p) {
        return (JS.BindingElement) super.visitBindingElement(binding, p);
    }

    @Override
    public JS.ComputedPropertyMethodDeclaration visitComputedPropertyMethodDeclaration(JS.ComputedPropertyMethodDeclaration method, P p) {
        return (JS.ComputedPropertyMethodDeclaration) super.visitComputedPropertyMethodDeclaration(method, p);
    }

    @Override
    public JS.ConditionalType visitConditionalType(JS.ConditionalType conditionalType, P p) {
        return (JS.ConditionalType) super.visitConditionalType(conditionalType, p);
    }

    @Override
    public JS.Delete visitDelete(JS.Delete delete, P p) {
        return (JS.Delete) super.visitDelete(delete, p);
    }

    @Override
    public JS.ExportAssignment visitExportAssignment(JS.ExportAssignment exportAssignment, P p) {
        return (JS.ExportAssignment) super.visitExportAssignment(exportAssignment, p);
    }

    @Override
    public JS.ExportSpecifier visitExportSpecifier(JS.ExportSpecifier exportSpecifier, P p) {
        return (JS.ExportSpecifier) super.visitExportSpecifier(exportSpecifier, p);
    }

    @Override
    public JS.ExpressionStatement visitExpressionStatement(JS.ExpressionStatement statement, P p) {
        return (JS.ExpressionStatement) super.visitExpressionStatement(statement, p);
    }

    @Override
    public JS.ExpressionWithTypeArguments visitExpressionWithTypeArguments(JS.ExpressionWithTypeArguments expressionWithTypeArguments, P p) {
        return (JS.ExpressionWithTypeArguments) super.visitExpressionWithTypeArguments(expressionWithTypeArguments, p);
    }

    @Override
    public JS.ForInLoop visitForInLoop(JS.ForInLoop forInLoop, P p) {
        return (JS.ForInLoop) super.visitForInLoop(forInLoop, p);
    }

    @Override
    public JS.ForOfLoop visitForOfLoop(JS.ForOfLoop forOfLoop, P p) {
        return (JS.ForOfLoop) super.visitForOfLoop(forOfLoop, p);
    }

    @Override
    public JS.FunctionCall visitFunctionCall(JS.FunctionCall functionCall, P p) {
        return (JS.FunctionCall) super.visitFunctionCall(functionCall, p);
    }

    @Override
    public JS.FunctionType visitFunctionType(JS.FunctionType functionType, P p) {
        return (JS.FunctionType) super.visitFunctionType(functionType, p);
    }

    @Override
    public JS.Binary visitBinaryExtensions(JS.Binary binary, P p) {
        return (JS.Binary) super.visitBinaryExtensions(binary, p);
    }

    @Override
    public JS.Import visitImportDeclaration(JS.Import anImport, P p) {
        return (JS.Import) super.visitImportDeclaration(anImport, p);
    }

    @Override
    public JS.ImportAttributes visitImportAttributes(JS.ImportAttributes importAttributes, P p) {
        return (JS.ImportAttributes) super.visitImportAttributes(importAttributes, p);
    }

    @Override
    public JS.ImportAttribute visitImportAttribute(JS.ImportAttribute importAttribute, P p) {
        return (JS.ImportAttribute) super.visitImportAttribute(importAttribute, p);
    }

    @Override
    public JS.ExportDeclaration visitExportDeclaration(JS.ExportDeclaration exportDeclaration, P p) {
        return (JS.ExportDeclaration) super.visitExportDeclaration(exportDeclaration, p);
    }

    @Override
    public JS.ImportClause visitImportClause(JS.ImportClause importClause, P p) {
        return (JS.ImportClause) super.visitImportClause(importClause, p);
    }

    @Override
    public JS.ImportSpecifier visitImportSpecifier(JS.ImportSpecifier importSpecifier, P p) {
        return (JS.ImportSpecifier) super.visitImportSpecifier(importSpecifier, p);
    }

    @Override
    public JS.ImportType visitImportType(JS.ImportType importType, P p) {
        return (JS.ImportType) super.visitImportType(importType, p);
    }

    @Override
    public JS.ImportTypeAttributes visitImportTypeAttributes(JS.ImportTypeAttributes importTypeAttributes, P p) {
        return (JS.ImportTypeAttributes) super.visitImportTypeAttributes(importTypeAttributes, p);
    }

    @Override
    public JS.IndexedAccessType visitIndexedAccessType(JS.IndexedAccessType indexedAccessType, P p) {
        return (JS.IndexedAccessType) super.visitIndexedAccessType(indexedAccessType, p);
    }

    @Override
    public JS.IndexSignatureDeclaration visitIndexSignatureDeclaration(JS.IndexSignatureDeclaration indexSignatureDeclaration, P p) {
        return (JS.IndexSignatureDeclaration) super.visitIndexSignatureDeclaration(indexSignatureDeclaration, p);
    }

    @Override
    public JS.IndexedAccessType.IndexType visitIndexedAccessTypeIndexType(JS.IndexedAccessType.IndexType indexedAccessTypeIndexType, P p) {
        return (JS.IndexedAccessType.IndexType) super.visitIndexedAccessTypeIndexType(indexedAccessTypeIndexType, p);
    }

    @Override
    public JS.InferType visitInferType(JS.InferType inferType, P p) {
        return (JS.InferType) super.visitInferType(inferType, p);
    }

    @Override
    public JS.LiteralType visitLiteralType(JS.LiteralType literalType, P p) {
        return (JS.LiteralType) super.visitLiteralType(literalType, p);
    }

    @Override
    public JS.MappedType visitMappedType(JS.MappedType mappedType, P p) {
        return (JS.MappedType) super.visitMappedType(mappedType, p);
    }

    @Override
    public JS.MappedType.KeysRemapping visitMappedTypeKeysRemapping(JS.MappedType.KeysRemapping mappedTypeKeys, P p) {
        return (JS.MappedType.KeysRemapping) super.visitMappedTypeKeysRemapping(mappedTypeKeys, p);
    }

    @Override
    public JS.MappedType.Parameter visitMappedTypeParameter(JS.MappedType.Parameter parameter, P p) {
        return (JS.MappedType.Parameter) super.visitMappedTypeParameter(parameter, p);
    }

    @Override
    public JS.NamedExports visitNamedExports(JS.NamedExports namedExports, P p) {
        return (JS.NamedExports) super.visitNamedExports(namedExports, p);
    }

    @Override
    public JS.NamedImports visitNamedImports(JS.NamedImports namedImports, P p) {
        return (JS.NamedImports) super.visitNamedImports(namedImports, p);
    }

    @Override
    public JS.NamespaceDeclaration visitNamespaceDeclaration(JS.NamespaceDeclaration namespaceDeclaration, P p) {
        return (JS.NamespaceDeclaration) super.visitNamespaceDeclaration(namespaceDeclaration, p);
    }

    @Override
    public JS.Intersection visitIntersection(JS.Intersection intersection, P p) {
        return (JS.Intersection) super.visitIntersection(intersection, p);
    }

    @Override
    public J.IntersectionType visitIntersectionType(J.IntersectionType intersectionType, P p) {
        return (J.IntersectionType) super.visitIntersectionType(intersectionType, p);
    }

    @Override
    public JS.ObjectBindingPattern visitObjectBindingPattern(JS.ObjectBindingPattern objectBindingPattern, P p) {
        return (JS.ObjectBindingPattern) super.visitObjectBindingPattern(objectBindingPattern, p);
    }

    @Override
    public JS.PropertyAssignment visitPropertyAssignment(JS.PropertyAssignment propertyAssignment, P p) {
        return (JS.PropertyAssignment) super.visitPropertyAssignment(propertyAssignment, p);
    }

    @Override
    public JS.SatisfiesExpression visitSatisfiesExpression(JS.SatisfiesExpression satisfiesExpression, P p) {
        return (JS.SatisfiesExpression) super.visitSatisfiesExpression(satisfiesExpression, p);
    }

    @Override
    public JS.ScopedVariableDeclarations visitScopedVariableDeclarations(JS.ScopedVariableDeclarations scopedVariableDeclarations, P p) {
        return (JS.ScopedVariableDeclarations) super.visitScopedVariableDeclarations(scopedVariableDeclarations, p);
    }

    @Override
    public JS.Shebang visitShebang(JS.Shebang shebang, P p) {
        return (JS.Shebang) super.visitShebang(shebang, p);
    }

    @Override
    public JS.StatementExpression visitStatementExpression(JS.StatementExpression expression, P p) {
        return (JS.StatementExpression) super.visitStatementExpression(expression, p);
    }

    @Override
    public JS.TaggedTemplateExpression visitTaggedTemplateExpression(JS.TaggedTemplateExpression taggedTemplateExpression, P p) {
        return (JS.TaggedTemplateExpression) super.visitTaggedTemplateExpression(taggedTemplateExpression, p);
    }

    @Override
    public JS.TemplateExpression visitTemplateExpression(JS.TemplateExpression templateExpression, P p) {
        return (JS.TemplateExpression) super.visitTemplateExpression(templateExpression, p);
    }

    @Override
    public JS.TemplateExpression.Span visitTemplateExpressionSpan(JS.TemplateExpression.Span value, P p) {
        return (JS.TemplateExpression.Span) super.visitTemplateExpressionSpan(value, p);
    }

    @Override
    public JS.Tuple visitTuple(JS.Tuple tuple, P p) {
        return (JS.Tuple) super.visitTuple(tuple, p);
    }

    @Override
    public JS.TypeDeclaration visitTypeDeclaration(JS.TypeDeclaration typeDeclaration, P p) {
        return (JS.TypeDeclaration) super.visitTypeDeclaration(typeDeclaration, p);
    }

    @Override
    public JS.TypeInfo visitTypeInfo(JS.TypeInfo typeInfo, P p) {
        return (JS.TypeInfo) super.visitTypeInfo(typeInfo, p);
    }

    @Override
    public JS.TypeLiteral visitTypeLiteral(JS.TypeLiteral typeLiteral, P p) {
        return (JS.TypeLiteral) super.visitTypeLiteral(typeLiteral, p);
    }

    @Override
    public JS.TypeOf visitTypeOf(JS.TypeOf typeOf, P p) {
        return (JS.TypeOf) super.visitTypeOf(typeOf, p);
    }

    @Override
    public JS.TypePredicate visitTypePredicate(JS.TypePredicate typePredicate, P p) {
        return (JS.TypePredicate) super.visitTypePredicate(typePredicate, p);
    }

    @Override
    public JS.TypeQuery visitTypeQuery(JS.TypeQuery typeQuery, P p) {
        return (JS.TypeQuery) super.visitTypeQuery(typeQuery, p);
    }

    @Override
    public JS.TypeTreeExpression visitTypeTreeExpression(JS.TypeTreeExpression typeTreeExpression, P p) {
        return (JS.TypeTreeExpression) super.visitTypeTreeExpression(typeTreeExpression, p);
    }

    @Override
    public JS.WithStatement visitWithStatement(JS.WithStatement withStatement, P p) {
        return (JS.WithStatement) super.visitWithStatement(withStatement, p);
    }

    @Override
    public JS.ComputedPropertyName visitComputedPropertyName(JS.ComputedPropertyName computedPropertyName, P p) {
        return (JS.ComputedPropertyName) super.visitComputedPropertyName(computedPropertyName, p);
    }

    @Override
    public JS.TypeOperator visitTypeOperator(JS.TypeOperator typeOperator, P p) {
        return (JS.TypeOperator) super.visitTypeOperator(typeOperator, p);
    }

    @Override
    public JS.Union visitUnion(JS.Union union, P p) {
        return (JS.Union) super.visitUnion(union, p);
    }

    @Override
    public JS.Void visitVoid(JS.Void aVoid, P p) {
        return (JS.Void) super.visitVoid(aVoid, p);
    }

    @Override
    public JS.Yield visitYield(J.Yield yield, P p) {
        return (J.Yield) super.visitYield(yield, p);
    }

    // J overrides.
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
    public J.DeconstructionPattern visitDeconstructionPattern(J.DeconstructionPattern deconstructionPattern, P p) {
        return (J.DeconstructionPattern) super.visitDeconstructionPattern(deconstructionPattern, p);
    }

    @Override
    public J.Erroneous visitErroneous(J.Erroneous erroneous, P p) {
        return (J.Erroneous) super.visitErroneous(erroneous, p);
    }

    @Override
    public J.Lambda.Parameters visitLambdaParameters(J.Lambda.Parameters parameters, P p) {
        return (J.Lambda.Parameters) super.visitLambdaParameters(parameters, p);
    }

    @Override
    public J.Modifier visitModifier(J.Modifier modifer, P p) {
        return (J.Modifier) super.visitModifier(modifer, p);
    }

    @Override
    public J.NullableType visitNullableType(J.NullableType nullableType, P p) {
        return (J.NullableType) super.visitNullableType(nullableType, p);
    }

    @Override
    public J.ParenthesizedTypeTree visitParenthesizedTypeTree(J.ParenthesizedTypeTree parTree, P p) {
        return (J.ParenthesizedTypeTree) super.visitParenthesizedTypeTree(parTree, p);
    }

    @Override
    public J.TypeParameters visitTypeParameters(J.TypeParameters typeParameters, P p) {
        return (J.TypeParameters) super.visitTypeParameters(typeParameters, p);
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
    public JSX.Tag visitJsxTag(JSX.Tag tag, P p) {
        return (JSX.Tag) super.visitJsxTag(tag, p);
    }

    @Override
    public JSX.Attribute visitJsxAttribute(JSX.Attribute attribute, P p) {
        return (JSX.Attribute) super.visitJsxAttribute(attribute, p);
    }

    @Override
    public JSX.SpreadAttribute visitJsxSpreadAttribute(JSX.SpreadAttribute spreadAttribute, P p) {
        return (JSX.SpreadAttribute) super.visitJsxSpreadAttribute(spreadAttribute, p);
    }

    @Override
    public JSX.EmbeddedExpression visitJsxEmbeddedExpression(JSX.EmbeddedExpression embeddedExpression, P p) {
        return (JSX.EmbeddedExpression) super.visitJsxEmbeddedExpression(embeddedExpression, p);
    }

    @Override
    public JSX.NamespacedName visitJsxNamespacedName(JSX.NamespacedName namespacedName, P p) {
        return (JSX.NamespacedName) super.visitJsxNamespacedName(namespacedName, p);
    }
}
