/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.javascript.internal.rpc;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;
import org.openrewrite.javascript.JavaScriptIsoVisitor;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.javascript.tree.JSX;

import java.util.List;
import java.util.Objects;

/**
 * Validates that the contents of lists and containers like JLeftPadded respect their generic types.
 *
 * @param <P>
 */
@SuppressWarnings("UnusedReturnValue")
public class JavaScriptValidator<P> extends JavaScriptIsoVisitor<P> {

    private <T extends Tree> @Nullable T visitAndValidate(@Nullable T tree, Class<? extends Tree> expected, P p) {
        if (tree != null && !expected.isInstance(tree)) {
            throw new ClassCastException("Type " + tree.getClass() + " is not assignable to " + expected);
        }
        // noinspection unchecked
        return (T) visit(tree, p);
    }

    private <T extends Tree> T visitAndValidateNonNull(@Nullable T tree, Class<? extends Tree> expected, P p) {
        Objects.requireNonNull(tree);
        if (!expected.isInstance(tree)) {
            throw new ClassCastException("Type " + tree.getClass() + " is not assignable to " + expected);
        }
        // noinspection unchecked
        return (T) visitNonNull(tree, p);
    }

    private <T extends Tree> @Nullable List<T> visitAndValidate(@Nullable List<T> list, Class<? extends Tree> expected, P p) {
        return list == null ? null : ListUtils.map(list, e -> visitAndValidateNonNull(e, expected, p));
    }

    @Override
    public JS.CompilationUnit visitJsCompilationUnit(JS.CompilationUnit compilationUnit, P p) {
        ListUtils.map(compilationUnit.getImports(), el -> visitAndValidateNonNull(el, J.Import.class, p));
        ListUtils.map(compilationUnit.getStatements(), el -> visitAndValidateNonNull(el, Statement.class, p));
        return compilationUnit;
    }

    @Override
    public JS.Alias visitAlias(JS.Alias alias, P p) {
        visitAndValidateNonNull(alias.getPropertyName(), J.Identifier.class, p);
        visitAndValidateNonNull(alias.getAlias(), Expression.class, p);
        return alias;
    }

    @Override
    public JS.ArrowFunction visitArrowFunction(JS.ArrowFunction arrowFunction, P p) {
        ListUtils.map(arrowFunction.getLeadingAnnotations(), el -> visitAndValidateNonNull(el, J.Annotation.class, p));
        ListUtils.map(arrowFunction.getModifiers(), el -> visitAndValidateNonNull(el, J.Modifier.class, p));
        visitAndValidate(arrowFunction.getTypeParameters(), J.TypeParameters.class, p);
        visitAndValidateNonNull(arrowFunction.getLambda(), J.Lambda.class, p);
        visitAndValidate(arrowFunction.getReturnTypeExpression(), TypeTree.class, p);
        return arrowFunction;
    }

    @Override
    public JS.Await visitAwait(JS.Await await, P p) {
        visitAndValidateNonNull(await.getExpression(), Expression.class, p);
        return await;
    }

    @Override
    public JS.ConditionalType visitConditionalType(JS.ConditionalType conditionalType, P p) {
        visitAndValidateNonNull(conditionalType.getCheckType(), Expression.class, p);
        visitAndValidateNonNull(conditionalType.getCondition(), J.Ternary.class, p);
        return conditionalType;
    }

    @Override
    public JS.Delete visitDelete(JS.Delete delete, P p) {
        visitAndValidateNonNull(delete.getExpression(), Expression.class, p);
        return delete;
    }

    @Override
    public JS.ExpressionStatement visitExpressionStatement(JS.ExpressionStatement expressionStatement, P p) {
        visitAndValidateNonNull(expressionStatement.getExpression(), Expression.class, p);
        return expressionStatement;
    }

    @Override
    public JS.ExpressionWithTypeArguments visitExpressionWithTypeArguments(JS.ExpressionWithTypeArguments expressionWithTypeArguments, P p) {
        visitAndValidateNonNull(expressionWithTypeArguments.getClazz(), J.class, p);
        visitAndValidate(expressionWithTypeArguments.getTypeArguments(), Expression.class, p);
        return expressionWithTypeArguments;
    }

    @Override
    public JS.FunctionCall visitFunctionCall(JS.FunctionCall functionCall, P p) {
        visitAndValidate(functionCall.getFunction(), Expression.class, p);
        visitAndValidate(functionCall.getTypeParameters(), Expression.class, p);
        visitAndValidate(functionCall.getArguments(), Expression.class, p);
        return functionCall;
    }

    @Override
    public JS.FunctionType visitFunctionType(JS.FunctionType functionType, P p) {
        ListUtils.map(functionType.getModifiers(), el -> visitAndValidateNonNull(el, J.Modifier.class, p));
        visitAndValidate(functionType.getTypeParameters(), J.TypeParameters.class, p);
        visitAndValidate(functionType.getParameters(), Statement.class, p);
        visitAndValidateNonNull(functionType.getReturnType(), Expression.class, p);
        return functionType;
    }

    @Override
    public JS.InferType visitInferType(JS.InferType inferType, P p) {
        visitAndValidateNonNull(inferType.getTypeParameter(), J.class, p);
        return inferType;
    }

    @Override
    public JS.ImportType visitImportType(JS.ImportType importType, P p) {
        visitAndValidate(importType.getArgumentAndAttributes(), J.class, p);
        visitAndValidate(importType.getQualifier(), Expression.class, p);
        visitAndValidate(importType.getTypeArguments(), Expression.class, p);
        return importType;
    }

    @Override
    public JS.Import visitImportDeclaration(JS.Import import_, P p) {
        visitAndValidate(import_.getModifiers(), J.Modifier.class, p);
        visitAndValidate(import_.getImportClause(), JS.ImportClause.class, p);
        visitAndValidate(import_.getModuleSpecifier(), Expression.class, p);
        visitAndValidate(import_.getAttributes(), JS.ImportAttributes.class, p);
        visitAndValidate(import_.getInitializer(), Expression.class, p);
        return import_;
    }

    @Override
    public JS.ImportClause visitImportClause(JS.ImportClause importClause, P p) {
        visitAndValidate(importClause.getName(), J.Identifier.class, p);
        visitAndValidate(importClause.getNamedBindings(), Expression.class, p);
        return importClause;
    }

    @Override
    public JS.NamedImports visitNamedImports(JS.NamedImports namedImports, P p) {
        visitAndValidate(namedImports.getElements(), JS.ImportSpecifier.class, p);
        return namedImports;
    }

    @Override
    public JS.ImportSpecifier visitImportSpecifier(JS.ImportSpecifier importSpecifier, P p) {
        visitAndValidateNonNull(importSpecifier.getSpecifier(), Expression.class, p);
        return importSpecifier;
    }

    @Override
    public JS.ImportAttributes visitImportAttributes(JS.ImportAttributes importAttributes, P p) {
        visitAndValidate(importAttributes.getElements(), Statement.class, p);
        return importAttributes;
    }

    @Override
    public JS.ImportTypeAttributes visitImportTypeAttributes(JS.ImportTypeAttributes importTypeAttributes, P p) {
        visitAndValidateNonNull(importTypeAttributes.getToken(), Expression.class, p);
        visitAndValidate(importTypeAttributes.getElements(), JS.ImportAttribute.class, p);
        return importTypeAttributes;
    }

    @Override
    public JS.ImportAttribute visitImportAttribute(JS.ImportAttribute importAttribute, P p) {
        visitAndValidateNonNull(importAttribute.getName(), Expression.class, p);
        visitAndValidateNonNull(importAttribute.getValue(), Expression.class, p);
        return importAttribute;
    }

    @Override
    public JS.Binary visitBinaryExtensions(JS.Binary binary, P p) {
        visitAndValidateNonNull(binary.getLeft(), Expression.class, p);
        visitAndValidateNonNull(binary.getRight(), Expression.class, p);
        return binary;
    }

    @Override
    public JS.LiteralType visitLiteralType(JS.LiteralType literalType, P p) {
        visitAndValidateNonNull(literalType.getLiteral(), Expression.class, p);
        return literalType;
    }

    @Override
    public JS.MappedType visitMappedType(JS.MappedType mappedType, P p) {
        visitAndValidate(mappedType.getPrefixToken(), J.Literal.class, p);
        visitAndValidateNonNull(mappedType.getKeysRemapping(), JS.MappedType.KeysRemapping.class, p);
        visitAndValidate(mappedType.getSuffixToken(), J.Literal.class, p);
        visitAndValidate(mappedType.getValueType(), TypeTree.class, p);
        return mappedType;
    }

    @Override
    public JS.MappedType.KeysRemapping visitMappedTypeKeysRemapping(JS.MappedType.KeysRemapping keysRemapping, P p) {
        visitAndValidateNonNull(keysRemapping.getTypeParameter(), JS.MappedType.Parameter.class, p);
        visitAndValidate(keysRemapping.getNameType(), Expression.class, p);
        return keysRemapping;
    }

    @Override
    public JS.MappedType.Parameter visitMappedTypeParameter(JS.MappedType.Parameter parameter, P p) {
        visitAndValidateNonNull(parameter.getName(), Expression.class, p);
        visitAndValidateNonNull(parameter.getIterateType(), TypeTree.class, p);
        return parameter;
    }

    @Override
    public JS.ObjectBindingPattern visitObjectBindingPattern(JS.ObjectBindingPattern objectBindingPattern, P p) {
        ListUtils.map(objectBindingPattern.getLeadingAnnotations(), el -> visitAndValidateNonNull(el, J.Annotation.class, p));
        ListUtils.map(objectBindingPattern.getModifiers(), el -> visitAndValidateNonNull(el, J.Modifier.class, p));
        visitAndValidate(objectBindingPattern.getTypeExpression(), TypeTree.class, p);
        visitAndValidate(objectBindingPattern.getBindings(), J.class, p);
        visitAndValidate(objectBindingPattern.getInitializer(), Expression.class, p);
        return objectBindingPattern;
    }

    @Override
    public JS.PropertyAssignment visitPropertyAssignment(JS.PropertyAssignment propertyAssignment, P p) {
        visitAndValidateNonNull(propertyAssignment.getName(), Expression.class, p);
        visitAndValidate(propertyAssignment.getInitializer(), Expression.class, p);
        return propertyAssignment;
    }

    @Override
    public JS.SatisfiesExpression visitSatisfiesExpression(JS.SatisfiesExpression satisfiesExpression, P p) {
        visitAndValidateNonNull(satisfiesExpression.getExpression(), J.class, p);
        visitAndValidateNonNull(satisfiesExpression.getSatisfiesType(), Expression.class, p);
        return satisfiesExpression;
    }

    @Override
    public JS.ScopedVariableDeclarations visitScopedVariableDeclarations(JS.ScopedVariableDeclarations scopedVariableDeclarations, P p) {
        ListUtils.map(scopedVariableDeclarations.getModifiers(), el -> visitAndValidateNonNull(el, J.Modifier.class, p));
        ListUtils.map(scopedVariableDeclarations.getVariables(), el -> visitAndValidateNonNull(el, J.class, p));
        return scopedVariableDeclarations;
    }

    @Override
    public JS.StatementExpression visitStatementExpression(JS.StatementExpression statementExpression, P p) {
        visitAndValidateNonNull(statementExpression.getStatement(), Statement.class, p);
        return statementExpression;
    }

    @Override
    public JS.WithStatement visitWithStatement(JS.WithStatement withStatement, P p) {
        visitAndValidateNonNull(withStatement.getExpression(), J.ControlParentheses.class, p);
        visitAndValidateNonNull(withStatement.getBody(), Statement.class, p);
        return withStatement;
    }

    @Override
    public JS.TaggedTemplateExpression visitTaggedTemplateExpression(JS.TaggedTemplateExpression taggedTemplateExpression, P p) {
        visitAndValidate(taggedTemplateExpression.getTag(), Expression.class, p);
        visitAndValidate(taggedTemplateExpression.getTypeArguments(), Expression.class, p);
        visitAndValidateNonNull(taggedTemplateExpression.getTemplateExpression(), Expression.class, p);
        return taggedTemplateExpression;
    }

    @Override
    public JS.TemplateExpression visitTemplateExpression(JS.TemplateExpression templateExpression, P p) {
        visitAndValidateNonNull(templateExpression.getHead(), J.Literal.class, p);
        ListUtils.map(templateExpression.getSpans(), el -> visitAndValidateNonNull(el, JS.TemplateExpression.Span.class, p));
        return templateExpression;
    }

    @Override
    public JS.TemplateExpression.Span visitTemplateExpressionSpan(JS.TemplateExpression.Span span, P p) {
        visitAndValidateNonNull(span.getExpression(), J.class, p);
        visitAndValidateNonNull(span.getTail(), J.Literal.class, p);
        return span;
    }

    @Override
    public JS.Tuple visitTuple(JS.Tuple tuple, P p) {
        visitAndValidate(tuple.getElements(), J.class, p);
        return tuple;
    }

    @Override
    public JS.TypeDeclaration visitTypeDeclaration(JS.TypeDeclaration typeDeclaration, P p) {
        ListUtils.map(typeDeclaration.getModifiers(), el -> visitAndValidateNonNull(el, J.Modifier.class, p));
        visitAndValidateNonNull(typeDeclaration.getName(), J.Identifier.class, p);
        visitAndValidate(typeDeclaration.getTypeParameters(), J.TypeParameters.class, p);
        visitAndValidateNonNull(typeDeclaration.getInitializer(), Expression.class, p);
        return typeDeclaration;
    }

    @Override
    public JS.TypeOf visitTypeOf(JS.TypeOf typeOf, P p) {
        visitAndValidateNonNull(typeOf.getExpression(), Expression.class, p);
        return typeOf;
    }

    @Override
    public JS.TypeQuery visitTypeQuery(JS.TypeQuery typeQuery, P p) {
        visitAndValidateNonNull(typeQuery.getTypeExpression(), TypeTree.class, p);
        visitAndValidate(typeQuery.getTypeArguments(), Expression.class, p);
        return typeQuery;
    }

    @Override
    public JS.ComputedPropertyName visitComputedPropertyName(JS.ComputedPropertyName computedPropertyName, P p) {
        visitAndValidateNonNull(computedPropertyName.getExpression(), Expression.class, p);
        return computedPropertyName;
    }

    @Override
    public JS.TypeOperator visitTypeOperator(JS.TypeOperator typeOperator, P p) {
        visitAndValidateNonNull(typeOperator.getExpression(), Expression.class, p);
        return typeOperator;
    }

    @Override
    public JS.TypePredicate visitTypePredicate(JS.TypePredicate typePredicate, P p) {
        visitAndValidateNonNull(typePredicate.getParameterName(), J.Identifier.class, p);
        visitAndValidate(typePredicate.getExpression(), Expression.class, p);
        return typePredicate;
    }

    @Override
    public JS.Union visitUnion(JS.Union union, P p) {
        ListUtils.map(union.getTypes(), el -> visitAndValidateNonNull(el, Expression.class, p));
        return union;
    }

    @Override
    public JS.Intersection visitIntersection(JS.Intersection intersection, P p) {
        ListUtils.map(intersection.getTypes(), el -> visitAndValidateNonNull(el, Expression.class, p));
        return intersection;
    }

    @Override
    public JS.Void visitVoid(JS.Void void_, P p) {
        visitAndValidateNonNull(void_.getExpression(), Expression.class, p);
        return void_;
    }

    @Override
    public JS.TypeInfo visitTypeInfo(JS.TypeInfo typeInfo, P p) {
        visitAndValidateNonNull(typeInfo.getTypeIdentifier(), TypeTree.class, p);
        return typeInfo;
    }

    @Override
    public JS.ComputedPropertyMethodDeclaration visitComputedPropertyMethodDeclaration(JS.ComputedPropertyMethodDeclaration computedPropertyMethodDeclaration, P p) {
        ListUtils.map(computedPropertyMethodDeclaration.getLeadingAnnotations(), el -> visitAndValidateNonNull(el, J.Annotation.class, p));
        ListUtils.map(computedPropertyMethodDeclaration.getModifiers(), el -> visitAndValidateNonNull(el, J.Modifier.class, p));
        visitAndValidate(computedPropertyMethodDeclaration.getTypeParameters(), J.TypeParameters.class, p);
        visitAndValidate(computedPropertyMethodDeclaration.getReturnTypeExpression(), TypeTree.class, p);
        visitAndValidateNonNull(computedPropertyMethodDeclaration.getName(), JS.ComputedPropertyName.class, p);
        visitAndValidate(computedPropertyMethodDeclaration.getParameters(), Statement.class, p);
        visitAndValidate(computedPropertyMethodDeclaration.getBody(), J.Block.class, p);
        return computedPropertyMethodDeclaration;
    }

    @Override
    public JS.ForOfLoop visitForOfLoop(JS.ForOfLoop forOfLoop, P p) {
        visitAndValidateNonNull(forOfLoop.getLoop(), J.ForEachLoop.class, p);
        return forOfLoop;
    }

    @Override
    public JS.ForInLoop visitForInLoop(JS.ForInLoop forInLoop, P p) {
        visitAndValidateNonNull(forInLoop.getControl(), J.ForEachLoop.Control.class, p);
        visitAndValidateNonNull(forInLoop.getBody(), Statement.class, p);
        return forInLoop;
    }

    @Override
    public JS.NamespaceDeclaration visitNamespaceDeclaration(JS.NamespaceDeclaration namespaceDeclaration, P p) {
        ListUtils.map(namespaceDeclaration.getModifiers(), el -> visitAndValidateNonNull(el, J.Modifier.class, p));
        visitAndValidateNonNull(namespaceDeclaration.getName(), Expression.class, p);
        visitAndValidate(namespaceDeclaration.getBody(), J.Block.class, p);
        return namespaceDeclaration;
    }

    @Override
    public JS.TypeLiteral visitTypeLiteral(JS.TypeLiteral typeLiteral, P p) {
        visitAndValidateNonNull(typeLiteral.getMembers(), J.Block.class, p);
        return typeLiteral;
    }

    @Override
    public JS.IndexSignatureDeclaration visitIndexSignatureDeclaration(JS.IndexSignatureDeclaration indexSignatureDeclaration, P p) {
        ListUtils.map(indexSignatureDeclaration.getModifiers(), el -> visitAndValidateNonNull(el, J.Modifier.class, p));
        visitAndValidate(indexSignatureDeclaration.getParameters(), J.class, p);
        visitAndValidateNonNull(indexSignatureDeclaration.getTypeExpression(), Expression.class, p);
        return indexSignatureDeclaration;
    }

    @Override
    public JS.ArrayBindingPattern visitArrayBindingPattern(JS.ArrayBindingPattern arrayBindingPattern, P p) {
        visitAndValidate(arrayBindingPattern.getElements(), Expression.class, p);
        return arrayBindingPattern;
    }

    @Override
    public JS.BindingElement visitBindingElement(JS.BindingElement bindingElement, P p) {
        visitAndValidate(bindingElement.getPropertyName(), Expression.class, p);
        visitAndValidateNonNull(bindingElement.getName(), TypedTree.class, p);
        visitAndValidate(bindingElement.getInitializer(), Expression.class, p);
        return bindingElement;
    }

    @Override
    public JS.ExportDeclaration visitExportDeclaration(JS.ExportDeclaration exportDeclaration, P p) {
        ListUtils.map(exportDeclaration.getModifiers(), el -> visitAndValidateNonNull(el, J.Modifier.class, p));
        visitAndValidate(exportDeclaration.getExportClause(), Expression.class, p);
        visitAndValidate(exportDeclaration.getModuleSpecifier(), Expression.class, p);
        visitAndValidate(exportDeclaration.getAttributes(), JS.ImportAttributes.class, p);
        return exportDeclaration;
    }

    @Override
    public JS.ExportAssignment visitExportAssignment(JS.ExportAssignment exportAssignment, P p) {
        visitAndValidateNonNull(exportAssignment.getExpression(), Expression.class, p);
        return exportAssignment;
    }

    @Override
    public JS.NamedExports visitNamedExports(JS.NamedExports namedExports, P p) {
        visitAndValidate(namedExports.getElements(), Expression.class, p);
        return namedExports;
    }

    @Override
    public JS.ExportSpecifier visitExportSpecifier(JS.ExportSpecifier exportSpecifier, P p) {
        visitAndValidateNonNull(exportSpecifier.getSpecifier(), Expression.class, p);
        return exportSpecifier;
    }

    @Override
    public JS.IndexedAccessType visitIndexedAccessType(JS.IndexedAccessType indexedAccessType, P p) {
        visitAndValidateNonNull(indexedAccessType.getObjectType(), TypeTree.class, p);
        visitAndValidateNonNull(indexedAccessType.getIndexType(), TypeTree.class, p);
        return indexedAccessType;
    }

    @Override
    public JS.IndexedAccessType.IndexType visitIndexedAccessTypeIndexType(JS.IndexedAccessType.IndexType indexType, P p) {
        visitAndValidateNonNull(indexType.getElement(), TypeTree.class, p);
        return indexType;
    }

    @Override
    public JS.As visitAs(JS.As as_, P p) {
        visitAndValidateNonNull(as_.getLeft(), Expression.class, p);
        visitAndValidateNonNull(as_.getRight(), Expression.class, p);
        return as_;
    }

    @Override
    public JS.AssignmentOperation visitAssignmentOperationExtensions(JS.AssignmentOperation assignmentOperation, P p) {
        visitAndValidateNonNull(assignmentOperation.getVariable(), Expression.class, p);
        visitAndValidateNonNull(assignmentOperation.getAssignment(), Expression.class, p);
        return assignmentOperation;
    }

    @Override
    public JS.TypeTreeExpression visitTypeTreeExpression(JS.TypeTreeExpression typeTreeExpression, P p) {
        visitAndValidateNonNull(typeTreeExpression.getExpression(), Expression.class, p);
        return typeTreeExpression;
    }

    @Override
    public J.AnnotatedType visitAnnotatedType(J.AnnotatedType annotatedType, P p) {
        ListUtils.map(annotatedType.getAnnotations(), el -> visitAndValidateNonNull(el, J.Annotation.class, p));
        visitAndValidateNonNull(annotatedType.getTypeExpression(), TypeTree.class, p);
        return annotatedType;
    }

    @Override
    public J.Annotation visitAnnotation(J.Annotation annotation, P p) {
        visitAndValidateNonNull(annotation.getAnnotationType(), NameTree.class, p);
        visitAndValidate(annotation.getArguments(), Expression.class, p);
        return annotation;
    }

    @Override
    public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, P p) {
        visitAndValidateNonNull(arrayAccess.getIndexed(), Expression.class, p);
        visitAndValidateNonNull(arrayAccess.getDimension(), J.ArrayDimension.class, p);
        return arrayAccess;
    }

    @Override
    public J.ArrayType visitArrayType(J.ArrayType arrayType, P p) {
        visitAndValidateNonNull(arrayType.getElementType(), TypeTree.class, p);
        ListUtils.map(arrayType.getAnnotations(), el -> visitAndValidateNonNull(el, J.Annotation.class, p));
        return arrayType;
    }

    @Override
    public J.Assert visitAssert(J.Assert assert_, P p) {
        visitAndValidateNonNull(assert_.getCondition(), Expression.class, p);
        visitAndValidate(assert_.getDetail() != null ? assert_.getDetail().getElement() : null, Expression.class, p);
        return assert_;
    }

    @Override
    public J.Assignment visitAssignment(J.Assignment assignment, P p) {
        visitAndValidateNonNull(assignment.getVariable(), Expression.class, p);
        visitAndValidateNonNull(assignment.getAssignment(), Expression.class, p);
        return assignment;
    }

    @Override
    public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignmentOperation, P p) {
        visitAndValidateNonNull(assignmentOperation.getVariable(), Expression.class, p);
        visitAndValidateNonNull(assignmentOperation.getAssignment(), Expression.class, p);
        return assignmentOperation;
    }

    @Override
    public J.Binary visitBinary(J.Binary binary, P p) {
        visitAndValidateNonNull(binary.getLeft(), Expression.class, p);
        visitAndValidateNonNull(binary.getRight(), Expression.class, p);
        return binary;
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        ListUtils.map(block.getStatements(), el -> visitAndValidateNonNull(el, Statement.class, p));
        return block;
    }

    @Override
    public J.Break visitBreak(J.Break break_, P p) {
        visitAndValidate(break_.getLabel(), J.Identifier.class, p);
        return break_;
    }

    @Override
    public J.Case visitCase(J.Case case_, P p) {
        visitAndValidate(case_.getCaseLabels(), J.class, p);
        visitAndValidate(case_.getStatements(), Statement.class, p);
        visitAndValidate(case_.getBody(), J.class, p);
        visitAndValidate(case_.getGuard(), Expression.class, p);
        return case_;
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDeclaration, P p) {
        ListUtils.map(classDeclaration.getLeadingAnnotations(), el -> visitAndValidateNonNull(el, J.Annotation.class, p));
        ListUtils.map(classDeclaration.getModifiers(), el -> visitAndValidateNonNull(el, J.Modifier.class, p));
        visitAndValidate(classDeclaration.getPadding().getKind().getAnnotations(), J.Annotation.class, p);
        visitAndValidateNonNull(classDeclaration.getName(), J.Identifier.class, p);
        visitAndValidate(classDeclaration.getTypeParameters(), J.TypeParameter.class, p);
        visitAndValidate(classDeclaration.getPrimaryConstructor(), Statement.class, p);
        visitAndValidate(classDeclaration.getExtends(), TypeTree.class, p);
        visitAndValidate(classDeclaration.getImplements(), TypeTree.class, p);
        visitAndValidate(classDeclaration.getPermits(), TypeTree.class, p);
        visitAndValidateNonNull(classDeclaration.getBody(), J.Block.class, p);
        return classDeclaration;
    }

    @Override
    public J.Continue visitContinue(J.Continue continue_, P p) {
        visitAndValidate(continue_.getLabel(), J.Identifier.class, p);
        return continue_;
    }

    @Override
    public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        visitAndValidateNonNull(doWhileLoop.getBody(), Statement.class, p);
        visitAndValidateNonNull(doWhileLoop.getWhileCondition(), Expression.class, p);
        return doWhileLoop;
    }

    @Override
    public J.Empty visitEmpty(J.Empty empty, P p) {
        return empty;
    }

    @Override
    public J.EnumValue visitEnumValue(J.EnumValue enumValue, P p) {
        ListUtils.map(enumValue.getAnnotations(), el -> visitAndValidateNonNull(el, J.Annotation.class, p));
        visitAndValidateNonNull(enumValue.getName(), J.Identifier.class, p);
        visitAndValidate(enumValue.getInitializer(), J.NewClass.class, p);
        return enumValue;
    }

    @Override
    public J.EnumValueSet visitEnumValueSet(J.EnumValueSet enumValueSet, P p) {
        ListUtils.map(enumValueSet.getEnums(), el -> visitAndValidateNonNull(el, J.EnumValue.class, p));
        return enumValueSet;
    }

    @Override
    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, P p) {
        visitAndValidateNonNull(fieldAccess.getTarget(), Expression.class, p);
        visitAndValidateNonNull(fieldAccess.getName(), J.Identifier.class, p);
        return fieldAccess;
    }

    @Override
    public J.ForEachLoop visitForEachLoop(J.ForEachLoop forEachLoop, P p) {
        visitAndValidateNonNull(forEachLoop.getControl(), J.ForEachLoop.Control.class, p);
        visitAndValidateNonNull(forEachLoop.getBody(), Statement.class, p);
        return forEachLoop;
    }

    @Override
    public J.ForEachLoop.Control visitForEachControl(J.ForEachLoop.Control control, P p) {
        visitAndValidateNonNull(control.getVariable(), Statement.class, p);
        visitAndValidateNonNull(control.getIterable(), Expression.class, p);
        return control;
    }

    @Override
    public J.ForLoop visitForLoop(J.ForLoop forLoop, P p) {
        visitAndValidateNonNull(forLoop.getControl(), J.ForLoop.Control.class, p);
        visitAndValidateNonNull(forLoop.getBody(), Statement.class, p);
        return forLoop;
    }

    @Override
    public J.ForLoop.Control visitForControl(J.ForLoop.Control control, P p) {
        ListUtils.map(control.getInit(), el -> visitAndValidateNonNull(el, Statement.class, p));
        visitAndValidateNonNull(control.getCondition(), Expression.class, p);
        ListUtils.map(control.getUpdate(), el -> visitAndValidateNonNull(el, Statement.class, p));
        return control;
    }

    @Override
    public J.ParenthesizedTypeTree visitParenthesizedTypeTree(J.ParenthesizedTypeTree parenthesizedTypeTree, P p) {
        ListUtils.map(parenthesizedTypeTree.getAnnotations(), el -> visitAndValidateNonNull(el, J.Annotation.class, p));
        visitAndValidateNonNull(parenthesizedTypeTree.getParenthesizedType(), J.Parentheses.class, p);
        return parenthesizedTypeTree;
    }

    @Override
    public J.Identifier visitIdentifier(J.Identifier identifier, P p) {
        ListUtils.map(identifier.getAnnotations(), el -> visitAndValidateNonNull(el, J.Annotation.class, p));
        return identifier;
    }

    @Override
    public J.If visitIf(J.If if_, P p) {
        visitAndValidateNonNull(if_.getIfCondition(), J.ControlParentheses.class, p);
        visitAndValidateNonNull(if_.getThenPart(), Statement.class, p);
        visitAndValidate(if_.getElsePart(), J.If.Else.class, p);
        return if_;
    }

    @Override
    public J.If.Else visitElse(J.If.Else else_, P p) {
        visitAndValidateNonNull(else_.getBody(), Statement.class, p);
        return else_;
    }

    @Override
    public J.Import visitImport(J.Import import_, P p) {
        visitAndValidateNonNull(import_.getQualid(), J.FieldAccess.class, p);
        visitAndValidate(import_.getAlias(), J.Identifier.class, p);
        return import_;
    }

    @Override
    public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, P p) {
        visitAndValidateNonNull(instanceOf.getExpression(), Expression.class, p);
        visitAndValidateNonNull(instanceOf.getClazz(), J.class, p);
        visitAndValidate(instanceOf.getPattern(), J.class, p);
        visitAndValidate(instanceOf.getModifier(), J.Modifier.class, p);
        return instanceOf;
    }

    @Override
    public J.DeconstructionPattern visitDeconstructionPattern(J.DeconstructionPattern deconstructionPattern, P p) {
        visitAndValidateNonNull(deconstructionPattern.getDeconstructor(), Expression.class, p);
        visitAndValidate(deconstructionPattern.getNested(), J.class, p);
        return deconstructionPattern;
    }

    @Override
    public J.IntersectionType visitIntersectionType(J.IntersectionType intersectionType, P p) {
        visitAndValidate(intersectionType.getBounds(), TypeTree.class, p);
        return intersectionType;
    }

    @Override
    public J.Label visitLabel(J.Label label, P p) {
        visitAndValidateNonNull(label.getLabel(), J.Identifier.class, p);
        visitAndValidateNonNull(label.getStatement(), Statement.class, p);
        return label;
    }

    @Override
    public J.Lambda visitLambda(J.Lambda lambda, P p) {
        visitAndValidate(lambda.getParameters().getParameters(), J.class, p);
        visitAndValidateNonNull(lambda.getBody(), J.class, p);
        return lambda;
    }

    @Override
    public J.Literal visitLiteral(J.Literal literal, P p) {
        return literal;
    }

    @Override
    public J.MemberReference visitMemberReference(J.MemberReference memberReference, P p) {
        visitAndValidateNonNull(memberReference.getContaining(), Expression.class, p);
        visitAndValidate(memberReference.getTypeParameters(), Expression.class, p);
        visitAndValidateNonNull(memberReference.getReference(), J.Identifier.class, p);
        return memberReference;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration methodDeclaration, P p) {
        ListUtils.map(methodDeclaration.getLeadingAnnotations(), el -> visitAndValidateNonNull(el, J.Annotation.class, p));
        ListUtils.map(methodDeclaration.getModifiers(), el -> visitAndValidateNonNull(el, J.Modifier.class, p));
        visitAndValidate(methodDeclaration.getPadding().getTypeParameters(), J.TypeParameters.class, p);
        visitAndValidate(methodDeclaration.getReturnTypeExpression(), TypeTree.class, p);
        visitAndValidate(methodDeclaration.getParameters(), Statement.class, p);
        visitAndValidate(methodDeclaration.getThrows(), NameTree.class, p);
        visitAndValidate(methodDeclaration.getBody(), J.Block.class, p);
        visitAndValidate(methodDeclaration.getDefaultValue(), Expression.class, p);
        return methodDeclaration;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation methodInvocation, P p) {
        visitAndValidate(methodInvocation.getSelect(), Expression.class, p);
        visitAndValidate(methodInvocation.getTypeParameters(), Expression.class, p);
        visitAndValidateNonNull(methodInvocation.getName(), J.Identifier.class, p);
        visitAndValidate(methodInvocation.getArguments(), Expression.class, p);
        return methodInvocation;
    }

    @Override
    public J.Modifier visitModifier(J.Modifier modifier, P p) {
        ListUtils.map(modifier.getAnnotations(), el -> visitAndValidateNonNull(el, J.Annotation.class, p));
        return modifier;
    }

    @Override
    public J.MultiCatch visitMultiCatch(J.MultiCatch multiCatch, P p) {
        ListUtils.map(multiCatch.getAlternatives(), el -> visitAndValidateNonNull(el, NameTree.class, p));
        return multiCatch;
    }

    @Override
    public J.NewArray visitNewArray(J.NewArray newArray, P p) {
        visitAndValidate(newArray.getTypeExpression(), TypeTree.class, p);
        ListUtils.map(newArray.getDimensions(), el -> visitAndValidateNonNull(el, J.ArrayDimension.class, p));
        visitAndValidate(newArray.getInitializer(), Expression.class, p);
        return newArray;
    }

    @Override
    public J.ArrayDimension visitArrayDimension(J.ArrayDimension arrayDimension, P p) {
        visitAndValidateNonNull(arrayDimension.getIndex(), Expression.class, p);
        return arrayDimension;
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, P p) {
        visitAndValidate(newClass.getEnclosing(), Expression.class, p);
        visitAndValidate(newClass.getClazz(), TypeTree.class, p);
        visitAndValidate(newClass.getArguments(), Expression.class, p);
        visitAndValidate(newClass.getBody(), J.Block.class, p);
        return newClass;
    }

    @Override
    public J.NullableType visitNullableType(J.NullableType nullableType, P p) {
        ListUtils.map(nullableType.getAnnotations(), el -> visitAndValidateNonNull(el, J.Annotation.class, p));
        visitAndValidateNonNull(nullableType.getTypeTree(), TypeTree.class, p);
        return nullableType;
    }

    @Override
    public J.Package visitPackage(J.Package package_, P p) {
        visitAndValidateNonNull(package_.getExpression(), Expression.class, p);
        ListUtils.map(package_.getAnnotations(), el -> visitAndValidateNonNull(el, J.Annotation.class, p));
        return package_;
    }

    @Override
    public J.ParameterizedType visitParameterizedType(J.ParameterizedType parameterizedType, P p) {
        visitAndValidateNonNull(parameterizedType.getClazz(), NameTree.class, p);
        visitAndValidate(parameterizedType.getTypeParameters(), Expression.class, p);
        return parameterizedType;
    }

    @Override
    public <J2 extends J> J.Parentheses<J2> visitParentheses(J.Parentheses<J2> parentheses, P p) {
        visitAndValidateNonNull(parentheses.getTree(), J.class, p);
        return parentheses;
    }

    @Override
    public <J2 extends J> J.ControlParentheses<J2> visitControlParentheses(J.ControlParentheses<J2> controlParentheses, P p) {
        visitAndValidateNonNull(controlParentheses.getTree(), J.class, p);
        return controlParentheses;
    }

    @Override
    public J.Primitive visitPrimitive(J.Primitive primitive, P p) {
        return primitive;
    }

    @Override
    public J.Return visitReturn(J.Return return_, P p) {
        visitAndValidate(return_.getExpression(), Expression.class, p);
        return return_;
    }

    @Override
    public J.Switch visitSwitch(J.Switch switch_, P p) {
        visitAndValidateNonNull(switch_.getSelector(), J.ControlParentheses.class, p);
        visitAndValidateNonNull(switch_.getCases(), J.Block.class, p);
        return switch_;
    }

    @Override
    public J.SwitchExpression visitSwitchExpression(J.SwitchExpression switchExpression, P p) {
        visitAndValidateNonNull(switchExpression.getSelector(), J.ControlParentheses.class, p);
        visitAndValidateNonNull(switchExpression.getCases(), J.Block.class, p);
        return switchExpression;
    }

    @Override
    public J.Synchronized visitSynchronized(J.Synchronized synchronized_, P p) {
        visitAndValidateNonNull(synchronized_.getLock(), J.ControlParentheses.class, p);
        visitAndValidateNonNull(synchronized_.getBody(), J.Block.class, p);
        return synchronized_;
    }

    @Override
    public J.Ternary visitTernary(J.Ternary ternary, P p) {
        visitAndValidateNonNull(ternary.getCondition(), Expression.class, p);
        visitAndValidateNonNull(ternary.getTruePart(), Expression.class, p);
        visitAndValidateNonNull(ternary.getFalsePart(), Expression.class, p);
        return ternary;
    }

    @Override
    public J.Throw visitThrow(J.Throw throw_, P p) {
        visitAndValidateNonNull(throw_.getException(), Expression.class, p);
        return throw_;
    }

    @Override
    public J.Try visitTry(J.Try try_, P p) {
        visitAndValidate(try_.getResources(), J.Try.Resource.class, p);
        visitAndValidateNonNull(try_.getBody(), J.Block.class, p);
        ListUtils.map(try_.getCatches(), el -> visitAndValidateNonNull(el, J.Try.Catch.class, p));
        visitAndValidate(try_.getFinally(), J.Block.class, p);
        return try_;
    }

    @Override
    public J.Try.Resource visitTryResource(J.Try.Resource resource, P p) {
        visitAndValidateNonNull(resource.getVariableDeclarations(), TypedTree.class, p);
        return resource;
    }

    @Override
    public J.Try.Catch visitCatch(J.Try.Catch catch_, P p) {
        visitAndValidateNonNull(catch_.getParameter(), J.ControlParentheses.class, p);
        visitAndValidateNonNull(catch_.getBody(), J.Block.class, p);
        return catch_;
    }

    @Override
    public J.TypeCast visitTypeCast(J.TypeCast typeCast, P p) {
        visitAndValidateNonNull(typeCast.getClazz(), J.ControlParentheses.class, p);
        visitAndValidateNonNull(typeCast.getExpression(), Expression.class, p);
        return typeCast;
    }

    @Override
    public J.TypeParameter visitTypeParameter(J.TypeParameter typeParameter, P p) {
        ListUtils.map(typeParameter.getAnnotations(), el -> visitAndValidateNonNull(el, J.Annotation.class, p));
        ListUtils.map(typeParameter.getModifiers(), el -> visitAndValidateNonNull(el, J.Modifier.class, p));
        visitAndValidateNonNull(typeParameter.getName(), Expression.class, p);
        visitAndValidate(typeParameter.getBounds(), TypeTree.class, p);
        return typeParameter;
    }

    @Override
    public J.Unary visitUnary(J.Unary unary, P p) {
        visitAndValidateNonNull(unary.getExpression(), Expression.class, p);
        return unary;
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations variableDeclarations, P p) {
        ListUtils.map(variableDeclarations.getLeadingAnnotations(), el -> visitAndValidateNonNull(el, J.Annotation.class, p));
        ListUtils.map(variableDeclarations.getModifiers(), el -> visitAndValidateNonNull(el, J.Modifier.class, p));
        visitAndValidate(variableDeclarations.getTypeExpression(), TypeTree.class, p);
        ListUtils.map(variableDeclarations.getVariables(), el -> visitAndValidateNonNull(el, J.VariableDeclarations.NamedVariable.class, p));
        return variableDeclarations;
    }

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable namedVariable, P p) {
        visitAndValidateNonNull(namedVariable.getName(), VariableDeclarator.class, p);
        visitAndValidate(namedVariable.getInitializer(), Expression.class, p);
        return namedVariable;
    }

    @Override
    public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, P p) {
        visitAndValidateNonNull(whileLoop.getCondition(), J.ControlParentheses.class, p);
        visitAndValidateNonNull(whileLoop.getBody(), Statement.class, p);
        return whileLoop;
    }

    @Override
    public J.Wildcard visitWildcard(J.Wildcard wildcard, P p) {
        visitAndValidate(wildcard.getBoundedType(), NameTree.class, p);
        return wildcard;
    }

    @Override
    public J.Yield visitYield(J.Yield yield, P p) {
        visitAndValidateNonNull(yield.getValue(), Expression.class, p);
        return yield;
    }

    @Override
    public J.Unknown visitUnknown(J.Unknown unknown, P p) {
        visitAndValidateNonNull(unknown.getSource(), J.Unknown.Source.class, p);
        return unknown;
    }

    @Override
    public J.Unknown.Source visitUnknownSource(J.Unknown.Source source, P p) {
        return source;
    }

    @Override
    public J.Erroneous visitErroneous(J.Erroneous erroneous, P p) {
        return erroneous;
    }

    @Override
    public JSX.Tag visitJsxTag(JSX.Tag tag, P p) {
        visitAndValidateNonNull(tag.getOpenName(), NameTree.class, p);
        ListUtils.map(tag.getChildren(), el -> visitAndValidateNonNull(el, Expression.class, p));
        ListUtils.map(tag.getAttributes(), el -> visitAndValidateNonNull(el, JSX.class, p));
        return tag;
    }

    @Override
    public JSX.Attribute visitJsxAttribute(JSX.Attribute attribute, P p) {
        visitAndValidateNonNull(attribute.getKey(), NameTree.class, p);
        visitAndValidate(attribute.getValue(), Expression.class, p);
        return attribute;
    }

    @Override
    public JSX.SpreadAttribute visitJsxSpreadAttribute(JSX.SpreadAttribute spreadAttribute, P p) {
        visitAndValidateNonNull(spreadAttribute.getExpression(), Expression.class, p);
        return spreadAttribute;
    }

    @Override
    public JSX.EmbeddedExpression visitJsxEmbeddedExpression(JSX.EmbeddedExpression embeddedExpression, P p) {
        visitAndValidateNonNull(embeddedExpression.getExpression(), Expression.class, p);
        return embeddedExpression;
    }

    @Override
    public JSX.NamespacedName visitJsxNamespacedName(JSX.NamespacedName namespacedName, P p) {
        visitAndValidateNonNull(namespacedName.getNamespace(), J.Identifier.class, p);
        visitAndValidateNonNull(namespacedName.getName(), J.Identifier.class, p);
        return namespacedName;
    }
}
