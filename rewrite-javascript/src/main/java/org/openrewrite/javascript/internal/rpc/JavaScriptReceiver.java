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
import org.openrewrite.java.internal.rpc.JavaReceiver;
import org.openrewrite.java.tree.*;
import org.openrewrite.javascript.JavaScriptVisitor;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.ValueCodec;

import static java.util.Objects.requireNonNull;

/**
 * A receiver for JavaScript AST elements that uses the Java RPC framework.
 * This class implements a double delegation pattern with {@link JavaReceiver}
 * to handle both JavaScript and Java elements.
 */
public class JavaScriptReceiver extends JavaScriptVisitor<RpcReceiveQueue> {
    private final JavaScriptReceiverDelegate delegate = new JavaScriptReceiverDelegate(this);

    @Override
    public @Nullable J visit(@Nullable Tree tree, RpcReceiveQueue p) {
        if (tree instanceof JS)
            return super.visit(tree, p);
        return delegate.visit(tree, p);
    }

    @Override
    public J preVisit(J j, RpcReceiveQueue q) {
        return ((J) j.withId(q.receiveAndGet(j.getId(), ValueCodec.UUID)))
                .withPrefix(q.receive(j.getPrefix(), space -> visitSpace(space, q)))
                .withMarkers(q.receiveMarkers(j.getMarkers()));
    }

    @Override
    public J visitCompilationUnit(JS.CompilationUnit cu, RpcReceiveQueue q) {
        return cu.withSourcePath(q.receiveAndGet(cu.getSourcePath(), ValueCodec.PATH))
                .withCharset(q.receiveAndGet(cu.getCharset(), ValueCodec.CHARSET))
                .withCharsetBomMarked(q.receive(cu.isCharsetBomMarked()))
                .withChecksum(q.receive(cu.getChecksum()))
                .<JS.CompilationUnit>withFileAttributes(q.receive(cu.getFileAttributes()))
                .getPadding().withImports(q.receiveList(cu.getPadding().getImports(), imp -> visitRightPadded(imp, q)))
                .getPadding().withStatements(q.receiveList(cu.getPadding().getStatements(), stmt -> visitRightPadded(stmt, q)))
                .withEof(q.receive(cu.getEof(), space -> visitSpace(space, q)));
    }

    @Override
    public J visitAlias(JS.Alias alias, RpcReceiveQueue q) {
        return alias
                .getPadding().withPropertyName(q.receive(alias.getPadding().getPropertyName(), el -> visitRightPadded(el, q)))
                .withAlias(q.receive(alias.getAlias(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitArrowFunction(JS.ArrowFunction arrowFunction, RpcReceiveQueue q) {
        return arrowFunction
                .withLeadingAnnotations(q.receiveList(arrowFunction.getLeadingAnnotations(), annot -> (J.Annotation) visitNonNull(annot, q)))
                .withModifiers(q.receiveList(arrowFunction.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                .withTypeParameters(q.receive(arrowFunction.getTypeParameters(), params -> (J.TypeParameters) visitNonNull(params, q)))
                .withParameters(q.receive(arrowFunction.getParameters(), params -> (J.Lambda.Parameters) visitNonNull(params, q)))
                .withReturnTypeExpression(q.receive(arrowFunction.getReturnTypeExpression(), tree -> (TypeTree) visitNonNull(tree, q)))
                .getPadding().withBody(q.receive(arrowFunction.getPadding().getBody(), el -> visitLeftPadded(el, q)))
                .withType(q.receive(arrowFunction.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitAwait(JS.Await await, RpcReceiveQueue q) {
        return await
                .withExpression(q.receive(await.getExpression(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(await.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitConditionalType(JS.ConditionalType conditionalType, RpcReceiveQueue q) {
        return conditionalType
                .withCheckType(q.receive(conditionalType.getCheckType(), expr -> (Expression) visitNonNull(expr, q)))
                .getPadding().withCondition(q.receive(conditionalType.getPadding().getCondition(), el -> visitContainer(el, q)))
                .withType(q.receive(conditionalType.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitDefaultType(JS.DefaultType defaultType, RpcReceiveQueue q) {
        return defaultType
                .withLeft(q.receive(defaultType.getLeft(), expr -> (Expression) visitNonNull(expr, q)))
                .withBeforeEquals(q.receive(defaultType.getBeforeEquals(), space -> visitSpace(space, q)))
                .withRight(q.receive(defaultType.getRight(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(defaultType.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitDelete(JS.Delete delete, RpcReceiveQueue q) {
        return delete
                .withExpression(q.receive(delete.getExpression(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(delete.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitExport(JS.Export export, RpcReceiveQueue q) {
        return export
                .getPadding()
                .withExports(q.receive(export.getPadding().getExports(), el -> visitContainer(el, q)))
                .withFrom(q.receive(export.getFrom(), space -> visitSpace(space, q)))
                .withTarget(q.receive(export.getTarget(), lit -> (J.Literal) visitNonNull(lit, q)))
                .getPadding().withInitializer(q.receive(export.getPadding().getInitializer(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitExpressionStatement(JS.ExpressionStatement expressionStatement, RpcReceiveQueue q) {
        return expressionStatement
                .withExpression(q.receive(expressionStatement.getExpression(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitExpressionWithTypeArguments(JS.ExpressionWithTypeArguments expressionWithTypeArguments, RpcReceiveQueue q) {
        return expressionWithTypeArguments
                .withClazz(q.receive(expressionWithTypeArguments.getClazz(), el -> visitNonNull(el, q)))
                .getPadding().withTypeArguments(q.receive(expressionWithTypeArguments.getPadding().getTypeArguments(), el -> visitContainer(el, q)))
                .withType(q.receive(expressionWithTypeArguments.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitFunctionType(JS.FunctionType functionType, RpcReceiveQueue q) {
        return functionType
                .withModifiers(q.receiveList(functionType.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                .getPadding().withConstructorType(q.receive(functionType.getPadding().getConstructorType(), el -> visitLeftPadded(el, q)))
                .withTypeParameters(q.receive(functionType.getTypeParameters(), params -> (J.TypeParameters) visitNonNull(params, q)))
                .getPadding().withParameters(q.receive(functionType.getPadding().getParameters(), el -> visitContainer(el, q)))
                .getPadding().withReturnType(q.receive(functionType.getPadding().getReturnType(), el -> visitLeftPadded(el, q)))
                .withType(q.receive(functionType.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitInferType(JS.InferType inferType, RpcReceiveQueue q) {
        return inferType
                .getPadding().withTypeParameter(q.receive(inferType.getPadding().getTypeParameter(), el -> visitLeftPadded(el, q)))
                .withType(q.receive(inferType.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitImportType(JS.ImportType importType, RpcReceiveQueue q) {
        return importType
                .getPadding().withHasTypeof(q.receive(importType.getPadding().getHasTypeof(), el -> visitRightPadded(el, q)))
                .getPadding().withArgumentAndAttributes(q.receive(importType.getPadding().getArgumentAndAttributes(), el -> visitContainer(el, q)))
                .getPadding().withQualifier(q.receive(importType.getPadding().getQualifier(), el -> visitLeftPadded(el, q)))
                .getPadding().withTypeArguments(q.receive(importType.getPadding().getTypeArguments(), el -> visitContainer(el, q)))
                .withType(q.receive(importType.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitJsImport(JS.JsImport jsImport, RpcReceiveQueue q) {
        return jsImport
                .withModifiers(q.receiveList(jsImport.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                .withImportClause(q.receive(jsImport.getImportClause(), el -> (JS.JsImportClause) visitNonNull(el, q)))
                .getPadding().withModuleSpecifier(q.receive(jsImport.getPadding().getModuleSpecifier(), el -> visitLeftPadded(el, q)))
                .withAttributes(q.receive(jsImport.getAttributes(), el -> (JS.ImportAttributes) visitNonNull(el, q)));
    }

    @Override
    public J visitJsImportClause(JS.JsImportClause jsImportClause, RpcReceiveQueue q) {
        return jsImportClause
                .withTypeOnly(q.receive(jsImportClause.isTypeOnly()))
                .getPadding().withName(q.receive(jsImportClause.getPadding().getName(), el -> visitRightPadded(el, q)))
                .withNamedBindings(q.receive(jsImportClause.getNamedBindings(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitNamedImports(JS.NamedImports namedImports, RpcReceiveQueue q) {
        return namedImports
                .getPadding().withElements(q.receive(namedImports.getPadding().getElements(), el -> visitContainer(el, q)))
                .withType(q.receive(namedImports.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitJsImportSpecifier(JS.JsImportSpecifier jsImportSpecifier, RpcReceiveQueue q) {
        return jsImportSpecifier
                .getPadding().withImportType(q.receive(jsImportSpecifier.getPadding().getImportType(), el -> visitLeftPadded(el, q)))
                .withSpecifier(q.receive(jsImportSpecifier.getSpecifier(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(jsImportSpecifier.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitJSVariableDeclarations(JS.JSVariableDeclarations jSVariableDeclarations, RpcReceiveQueue q) {
        return jSVariableDeclarations
                .withLeadingAnnotations(q.receiveList(jSVariableDeclarations.getLeadingAnnotations(), annot -> (J.Annotation) visitNonNull(annot, q)))
                .withModifiers(q.receiveList(jSVariableDeclarations.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                .withTypeExpression(q.receive(jSVariableDeclarations.getTypeExpression(), tree -> (TypeTree) visitNonNull(tree, q)))
                .withVarargs(q.receive(jSVariableDeclarations.getVarargs(), space -> visitSpace(space, q)))
                .getPadding().withVariables(q.receiveList(jSVariableDeclarations.getPadding().getVariables(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitJSVariableDeclarationsJSNamedVariable(JS.JSVariableDeclarations.JSNamedVariable jSNamedVariable, RpcReceiveQueue q) {
        return jSNamedVariable
                .withName(q.receive(jSNamedVariable.getName(), expr -> (Expression) visitNonNull(expr, q)))
                .withDimensionsAfterName(q.receiveList(jSNamedVariable.getDimensionsAfterName(), el -> visitLeftPadded(el, q)))
                .getPadding().withInitializer(q.receive(jSNamedVariable.getPadding().getInitializer(), el -> visitLeftPadded(el, q)))
                .withVariableType(q.receive(jSNamedVariable.getVariableType(), type -> (JavaType.Variable) visitType(type, q)));
    }

    @Override
    public J visitImportAttributes(JS.ImportAttributes importAttributes, RpcReceiveQueue q) {
        return importAttributes
                .withToken(q.receiveAndGet(importAttributes.getToken(), ValueCodec.forEnum(JS.ImportAttributes.Token.class)))
                .getPadding().withElements(q.receive(importAttributes.getPadding().getElements(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitImportTypeAttributes(JS.ImportTypeAttributes importTypeAttributes, RpcReceiveQueue q) {
        return importTypeAttributes
                .getPadding().withToken(q.receive(importTypeAttributes.getPadding().getToken(), el -> visitRightPadded(el, q)))
                .getPadding().withElements(q.receive(importTypeAttributes.getPadding().getElements(), el -> visitContainer(el, q)))
                .withEnd(q.receive(importTypeAttributes.getEnd(), space -> visitSpace(space, q)));
    }

    @Override
    public J visitImportAttribute(JS.ImportAttribute importAttribute, RpcReceiveQueue q) {
        return importAttribute
                .withName(q.receive(importAttribute.getName(), expr -> (Expression) visitNonNull(expr, q)))
                .getPadding().withValue(q.receive(importAttribute.getPadding().getValue(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitJsBinary(JS.JsBinary jsBinary, RpcReceiveQueue q) {
        return jsBinary
                .withLeft(q.receive(jsBinary.getLeft(), expr -> (Expression) visitNonNull(expr, q)))
                .getPadding().withOperator(q.receive(jsBinary.getPadding().getOperator(), el -> visitLeftPadded(el, q, ValueCodec.forEnum(JS.JsBinary.Type.class))))
                .withRight(q.receive(jsBinary.getRight(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(jsBinary.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitLiteralType(JS.LiteralType literalType, RpcReceiveQueue q) {
        return literalType
                .withLiteral(q.receive(literalType.getLiteral(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(literalType.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitMappedType(JS.MappedType mappedType, RpcReceiveQueue q) {
        return mappedType
                .getPadding().withPrefixToken(q.receive(mappedType.getPadding().getPrefixToken(), el -> visitLeftPadded(el, q)))
                .getPadding().withHasReadonly(q.receive(mappedType.getPadding().getHasReadonly(), el -> visitLeftPadded(el, q)))
                .withKeysRemapping(q.receive(mappedType.getKeysRemapping(), el -> (JS.MappedType.KeysRemapping) visitNonNull(el, q))).getPadding()
                .withSuffixToken(q.receive(mappedType.getPadding().getSuffixToken(), el -> visitLeftPadded(el, q)))
                .getPadding().withHasQuestionToken(q.receive(mappedType.getPadding().getHasQuestionToken(), el -> visitLeftPadded(el, q)))
                .getPadding().withValueType(q.receive(mappedType.getPadding().getValueType(), el -> visitContainer(el, q)))
                .withType(q.receive(mappedType.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitMappedTypeKeysRemapping(JS.MappedType.KeysRemapping keysRemapping, RpcReceiveQueue q) {
        return keysRemapping
                .getPadding().withTypeParameter(q.receive(keysRemapping.getPadding().getTypeParameter(), el -> visitRightPadded(el, q)))
                .getPadding().withNameType(q.receive(keysRemapping.getPadding().getNameType(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitMappedTypeMappedTypeParameter(JS.MappedType.MappedTypeParameter mappedTypeParameter, RpcReceiveQueue q) {
        return mappedTypeParameter
                .withName(q.receive(mappedTypeParameter.getName(), expr -> (Expression) visitNonNull(expr, q)))
                .getPadding().withIterateType(q.receive(mappedTypeParameter.getPadding().getIterateType(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitObjectBindingDeclarations(JS.ObjectBindingDeclarations objectBindingDeclarations, RpcReceiveQueue q) {
        return objectBindingDeclarations
                .withLeadingAnnotations(q.receiveList(objectBindingDeclarations.getLeadingAnnotations(), annot -> (J.Annotation) visitNonNull(annot, q)))
                .withModifiers(q.receiveList(objectBindingDeclarations.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                .withTypeExpression(q.receive(objectBindingDeclarations.getTypeExpression(), tree -> (TypeTree) visitNonNull(tree, q)))
                .getPadding().withBindings(q.receive(objectBindingDeclarations.getPadding().getBindings(), el -> visitContainer(el, q)))
                .getPadding().withInitializer(q.receive(objectBindingDeclarations.getPadding().getInitializer(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitPropertyAssignment(JS.PropertyAssignment propertyAssignment, RpcReceiveQueue q) {
        return propertyAssignment
                .getPadding().withName(q.receive(propertyAssignment.getPadding().getName(), el -> visitRightPadded(el, q)))
                .withAssigmentToken(q.receiveAndGet(propertyAssignment.getAssigmentToken(), ValueCodec.forEnum(JS.PropertyAssignment.AssigmentToken.class)))
                .withInitializer(q.receive(propertyAssignment.getInitializer(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitSatisfiesExpression(JS.SatisfiesExpression satisfiesExpression, RpcReceiveQueue q) {
        return satisfiesExpression
                .withExpression(q.receive(satisfiesExpression.getExpression(), el -> visitNonNull(el, q)))
                .getPadding().withSatisfiesType(q.receive(satisfiesExpression.getPadding().getSatisfiesType(), el -> visitLeftPadded(el, q)))
                .withType(q.receive(satisfiesExpression.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitScopedVariableDeclarations(JS.ScopedVariableDeclarations scopedVariableDeclarations, RpcReceiveQueue q) {
        return scopedVariableDeclarations
                .withModifiers(q.receiveList(scopedVariableDeclarations.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                .getPadding().withScope(q.receive(scopedVariableDeclarations.getPadding().getScope(), el -> visitLeftPadded(el, q, ValueCodec.forEnum(JS.ScopedVariableDeclarations.Scope.class))))
                .getPadding().withVariables(q.receiveList(scopedVariableDeclarations.getPadding().getVariables(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitStatementExpression(JS.StatementExpression statementExpression, RpcReceiveQueue q) {
        return statementExpression
                .withStatement(q.receive(statementExpression.getStatement(), stmt -> (Statement) visitNonNull(stmt, q)));
    }

    @Override
    public J visitTaggedTemplateExpression(JS.TaggedTemplateExpression taggedTemplateExpression, RpcReceiveQueue q) {
        return taggedTemplateExpression
                .getPadding().withTag(q.receive(taggedTemplateExpression.getPadding().getTag(), el -> visitRightPadded(el, q)))
                .getPadding().withTypeArguments(q.receive(taggedTemplateExpression.getPadding().getTypeArguments(), el -> visitContainer(el, q)))
                .withTemplateExpression(q.receive(taggedTemplateExpression.getTemplateExpression(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(taggedTemplateExpression.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitTemplateExpression(JS.TemplateExpression templateExpression, RpcReceiveQueue q) {
        return templateExpression
                .withHead(q.receive(templateExpression.getHead(), lit -> (J.Literal) visitNonNull(lit, q)))
                .getPadding().withTemplateSpans(q.receiveList(templateExpression.getPadding().getTemplateSpans(), el -> visitRightPadded(el, q)))
                .withType(q.receive(templateExpression.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitTemplateExpressionTemplateSpan(JS.TemplateExpression.TemplateSpan templateSpan, RpcReceiveQueue q) {
        return templateSpan
                .withExpression(q.receive(templateSpan.getExpression(), el -> visitNonNull(el, q)))
                .withTail(q.receive(templateSpan.getTail(), lit -> (J.Literal) visitNonNull(lit, q)));
    }

    @Override
    public J visitTrailingTokenStatement(JS.TrailingTokenStatement trailingTokenStatement, RpcReceiveQueue q) {
        return trailingTokenStatement
                .getPadding().withExpression(q.receive(trailingTokenStatement.getPadding().getExpression(), el -> visitRightPadded(el, q)))
                .withType(q.receive(trailingTokenStatement.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitTuple(JS.Tuple tuple, RpcReceiveQueue q) {
        return tuple
                .getPadding().withElements(q.receive(tuple.getPadding().getElements(), el -> visitContainer(el, q)))
                .withType(q.receive(tuple.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitTypeDeclaration(JS.TypeDeclaration typeDeclaration, RpcReceiveQueue q) {
        return typeDeclaration
                .withModifiers(q.receiveList(typeDeclaration.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                .getPadding().withName(q.receive(typeDeclaration.getPadding().getName(), el -> visitLeftPadded(el, q)))
                .withTypeParameters(q.receive(typeDeclaration.getTypeParameters(), params -> (J.TypeParameters) visitNonNull(params, q)))
                .getPadding().withInitializer(q.receive(typeDeclaration.getPadding().getInitializer(), el -> visitLeftPadded(el, q)))
                .withType(q.receive(typeDeclaration.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitTypeOf(JS.TypeOf typeOf, RpcReceiveQueue q) {
        return typeOf
                .withExpression(q.receive(typeOf.getExpression(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(typeOf.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitTypeTreeExpression(JS.TypeTreeExpression typeTreeExpression, RpcReceiveQueue q) {
        return typeTreeExpression
                .withExpression(q.receive(typeTreeExpression.getExpression(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitJsAssignmentOperation(JS.JsAssignmentOperation jsAssignmentOperation, RpcReceiveQueue q) {
        return jsAssignmentOperation
                .withVariable(q.receive(jsAssignmentOperation.getVariable(), expr -> (Expression) visitNonNull(expr, q)))
                .getPadding().withOperator(q.receive(jsAssignmentOperation.getPadding().getOperator(), el -> visitLeftPadded(el, q, ValueCodec.forEnum(JS.JsAssignmentOperation.Type.class))))
                .withAssignment(q.receive(jsAssignmentOperation.getAssignment(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(jsAssignmentOperation.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitIndexedAccessType(JS.IndexedAccessType indexedAccessType, RpcReceiveQueue q) {
        return indexedAccessType
                .withObjectType(q.receive(indexedAccessType.getObjectType(), tree -> (TypeTree) visitNonNull(tree, q)))
                .withIndexType(q.receive(indexedAccessType.getIndexType(), tree -> (TypeTree) visitNonNull(tree, q)))
                .withType(q.receive(indexedAccessType.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitIndexedAccessTypeIndexType(JS.IndexedAccessType.IndexType indexType, RpcReceiveQueue q) {
        return indexType
                .getPadding().withElement(q.receive(indexType.getPadding().getElement(), el -> visitRightPadded(el, q)))
                .withType(q.receive(indexType.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitTypeQuery(JS.TypeQuery typeQuery, RpcReceiveQueue q) {
        return typeQuery
                .withTypeExpression(q.receive(typeQuery.getTypeExpression(), tree -> (TypeTree) visitNonNull(tree, q)))
                .getPadding().withTypeArguments(q.receive(typeQuery.getPadding().getTypeArguments(), el -> visitContainer(el, q)))
                .withType(q.receive(typeQuery.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitTypeInfo(JS.TypeInfo typeInfo, RpcReceiveQueue q) {
        return typeInfo
                .withTypeIdentifier(q.receive(typeInfo.getTypeIdentifier(), tree -> (TypeTree) visitNonNull(tree, q)));
    }

    @Override
    public J visitTypeOperator(JS.TypeOperator typeOperator, RpcReceiveQueue q) {
        return typeOperator
                .withOperator(q.receiveAndGet(typeOperator.getOperator(), ValueCodec.forEnum(JS.TypeOperator.Type.class)))
                .getPadding().withExpression(q.receive(typeOperator.getPadding().getExpression(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitTypePredicate(JS.TypePredicate typePredicate, RpcReceiveQueue q) {
        return typePredicate
                .getPadding().withAsserts(q.receive(typePredicate.getPadding().getAsserts(), el -> visitLeftPadded(el, q)))
                .withParameterName(q.receive(typePredicate.getParameterName(), id -> (J.Identifier) visitNonNull(id, q)))
                .getPadding().withExpression(q.receive(typePredicate.getPadding().getExpression(), el -> visitLeftPadded(el, q)))
                .withType(q.receive(typePredicate.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitUnion(JS.Union union, RpcReceiveQueue q) {
        return union
                .getPadding().withTypes(q.receiveList(union.getPadding().getTypes(), el -> visitRightPadded(el, q)))
                .withType(q.receive(union.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitIntersection(JS.Intersection intersection, RpcReceiveQueue q) {
        return intersection
                .getPadding().withTypes(q.receiveList(intersection.getPadding().getTypes(), el -> visitRightPadded(el, q)))
                .withType(q.receive(intersection.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitVoid(JS.Void void_, RpcReceiveQueue q) {
        return void_
                .withExpression(q.receive(void_.getExpression(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitUnary(JS.Unary unary, RpcReceiveQueue q) {
        return unary
                .getPadding().withOperator(q.receive(unary.getPadding().getOperator(), el -> visitLeftPadded(el, q, ValueCodec.forEnum(JS.Unary.Type.class))))
                .withExpression(q.receive(unary.getExpression(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(unary.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitYield(JS.Yield yield, RpcReceiveQueue q) {
        return yield
                .getPadding().withDelegated(q.receive(yield.getPadding().getDelegated(), el -> visitLeftPadded(el, q)))
                .withExpression(q.receive(yield.getExpression(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(yield.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitWithStatement(JS.WithStatement withStatement, RpcReceiveQueue q) {
        return withStatement
                .withExpression(q.receive(withStatement.getExpression(), expr -> (J.ControlParentheses<Expression>) visitNonNull(expr, q)))
                .getPadding().withBody(q.receive(withStatement.getPadding().getBody(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitIndexSignatureDeclaration(JS.IndexSignatureDeclaration indexSignatureDeclaration, RpcReceiveQueue q) {
        return indexSignatureDeclaration
                .withModifiers(q.receiveList(indexSignatureDeclaration.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                .getPadding().withParameters(q.receive(indexSignatureDeclaration.getPadding().getParameters(), el -> visitContainer(el, q)))
                .getPadding().withTypeExpression(q.receive(indexSignatureDeclaration.getPadding().getTypeExpression(), el -> visitLeftPadded(el, q)))
                .withType(q.receive(indexSignatureDeclaration.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitJSMethodDeclaration(JS.JSMethodDeclaration jSMethodDeclaration, RpcReceiveQueue q) {
        return jSMethodDeclaration
                .withLeadingAnnotations(q.receiveList(jSMethodDeclaration.getLeadingAnnotations(), annot -> (J.Annotation) visitNonNull(annot, q)))
                .withModifiers(q.receiveList(jSMethodDeclaration.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                .withTypeParameters(q.receive(jSMethodDeclaration.getTypeParameters(), params -> (J.TypeParameters) visitNonNull(params, q)))
                .withReturnTypeExpression(q.receive(jSMethodDeclaration.getReturnTypeExpression(), tree -> (TypeTree) visitNonNull(tree, q)))
                .withName(q.receive(jSMethodDeclaration.getName(), expr -> (Expression) visitNonNull(expr, q)))
                .getPadding().withParameters(q.receive(jSMethodDeclaration.getPadding().getParameters(), el -> visitContainer(el, q)))
                .withBody(q.receive(jSMethodDeclaration.getBody(), block -> (J.Block) visitNonNull(block, q)))
                .getPadding().withDefaultValue(q.receive(jSMethodDeclaration.getPadding().getDefaultValue(), el -> visitLeftPadded(el, q)))
                .withMethodType(q.receive(jSMethodDeclaration.getMethodType(), type -> (JavaType.Method) visitType(type, q)));
    }

    @Override
    public J visitJSForOfLoop(JS.JSForOfLoop jSForOfLoop, RpcReceiveQueue q) {
        return jSForOfLoop
                .getPadding().withAwait(q.receive(jSForOfLoop.getPadding().getAwait(), el -> visitLeftPadded(el, q)))
                .withControl(q.receive(jSForOfLoop.getControl(), el -> (JS.JSForInOfLoopControl) visitNonNull(el, q)))
                .getPadding().withBody(q.receive(jSForOfLoop.getPadding().getBody(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitJSForInLoop(JS.JSForInLoop jSForInLoop, RpcReceiveQueue q) {
        return jSForInLoop
                .withControl(q.receive(jSForInLoop.getControl(), el -> (JS.JSForInOfLoopControl) visitNonNull(el, q)))
                .getPadding().withBody(q.receive(jSForInLoop.getPadding().getBody(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitJSForInOfLoopControl(JS.JSForInOfLoopControl jSForInOfLoopControl, RpcReceiveQueue q) {
        return jSForInOfLoopControl
                .getPadding().withVariable(q.receive(jSForInOfLoopControl.getPadding().getVariable(), el -> visitRightPadded(el, q)))
                .getPadding().withIterable(q.receive(jSForInOfLoopControl.getPadding().getIterable(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitJSTry(JS.JSTry jSTry, RpcReceiveQueue q) {
        return jSTry
                .withBody(q.receive(jSTry.getBody(), block -> (J.Block) visitNonNull(block, q)))
                .withCatches(q.receive(jSTry.getCatches(), el -> (JS.JSTry.JSCatch) visitNonNull(el, q)))
                .getPadding().withFinallie(q.receive(jSTry.getPadding().getFinallie(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitJSTryJSCatch(JS.JSTry.JSCatch jSCatch, RpcReceiveQueue q) {
        return jSCatch
                .withParameter(q.receive(jSCatch.getParameter(), expr -> (J.ControlParentheses<JS.JSVariableDeclarations>) visitNonNull(expr, q)))
                .withBody(q.receive(jSCatch.getBody(), block -> (J.Block) visitNonNull(block, q)));
    }

    @Override
    public J visitNamespaceDeclaration(JS.NamespaceDeclaration namespaceDeclaration, RpcReceiveQueue q) {
        return namespaceDeclaration
                .withModifiers(q.receiveList(namespaceDeclaration.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                .getPadding().withKeywordType(q.receive(namespaceDeclaration.getPadding().getKeywordType(), el -> visitLeftPadded(el, q, ValueCodec.forEnum(JS.NamespaceDeclaration.KeywordType.class))))
                .getPadding().withName(q.receive(namespaceDeclaration.getPadding().getName(), el -> visitRightPadded(el, q)))
                .withBody(q.receive(namespaceDeclaration.getBody(), block -> (J.Block) visitNonNull(block, q)));
    }

    @Override
    public J visitFunctionDeclaration(JS.FunctionDeclaration functionDeclaration, RpcReceiveQueue q) {
        return functionDeclaration
                .withModifiers(q.receiveList(functionDeclaration.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                .getPadding().withAsteriskToken(q.receive(functionDeclaration.getPadding().getAsteriskToken(), el -> visitLeftPadded(el, q)))
                .getPadding().withName(q.receive(functionDeclaration.getPadding().getName(), el -> visitLeftPadded(el, q)))
                .withTypeParameters(q.receive(functionDeclaration.getTypeParameters(), params -> (J.TypeParameters) visitNonNull(params, q)))
                .getPadding().withParameters(q.receive(functionDeclaration.getPadding().getParameters(), el -> visitContainer(el, q)))
                .withReturnTypeExpression(q.receive(functionDeclaration.getReturnTypeExpression(), tree -> (TypeTree) visitNonNull(tree, q)))
                .withBody(q.receive(functionDeclaration.getBody(), el -> visitNonNull(el, q)))
                .withType(q.receive(functionDeclaration.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitTypeLiteral(JS.TypeLiteral typeLiteral, RpcReceiveQueue q) {
        return typeLiteral
                .withMembers(q.receive(typeLiteral.getMembers(), block -> (J.Block) visitNonNull(block, q)))
                .withType(q.receive(typeLiteral.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitArrayBindingPattern(JS.ArrayBindingPattern arrayBindingPattern, RpcReceiveQueue q) {
        return arrayBindingPattern
                .getPadding().withElements(q.receive(arrayBindingPattern.getPadding().getElements(), el -> visitContainer(el, q)))
                .withType(q.receive(arrayBindingPattern.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitBindingElement(JS.BindingElement bindingElement, RpcReceiveQueue q) {
        return bindingElement
                .getPadding().withPropertyName(q.receive(bindingElement.getPadding().getPropertyName(), el -> visitRightPadded(el, q)))
                .withName(q.receive(bindingElement.getName(), el -> (TypedTree) visitNonNull(el, q)))
                .getPadding().withInitializer(q.receive(bindingElement.getPadding().getInitializer(), el -> visitLeftPadded(el, q)))
                .withVariableType(q.receive(bindingElement.getVariableType(), type -> (JavaType.Variable) visitType(type, q)));
    }

    @Override
    public J visitExportDeclaration(JS.ExportDeclaration exportDeclaration, RpcReceiveQueue q) {
        return exportDeclaration
                .withModifiers(q.receiveList(exportDeclaration.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                .getPadding().withTypeOnly(q.receive(exportDeclaration.getPadding().getTypeOnly(), el -> visitLeftPadded(el, q)))
                .withExportClause(q.receive(exportDeclaration.getExportClause(), expr -> (Expression) visitNonNull(expr, q)))
                .getPadding().withModuleSpecifier(q.receive(exportDeclaration.getPadding().getModuleSpecifier(), el -> visitLeftPadded(el, q)))
                .withAttributes(q.receive(exportDeclaration.getAttributes(), el -> (JS.ImportAttributes) visitNonNull(el, q)));
    }

    @Override
    public J visitExportAssignment(JS.ExportAssignment exportAssignment, RpcReceiveQueue q) {
        return exportAssignment
                .withModifiers(q.receiveList(exportAssignment.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                .getPadding().withExportEquals(q.receive(exportAssignment.getPadding().getExportEquals(), el -> visitLeftPadded(el, q)))
                .withExpression(q.receive(exportAssignment.getExpression(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitNamedExports(JS.NamedExports namedExports, RpcReceiveQueue q) {
        return namedExports
                .getPadding().withElements(q.receive(namedExports.getPadding().getElements(), el -> visitContainer(el, q)))
                .withType(q.receive(namedExports.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitExportSpecifier(JS.ExportSpecifier exportSpecifier, RpcReceiveQueue q) {
        return exportSpecifier
                .getPadding().withTypeOnly(q.receive(exportSpecifier.getPadding().getTypeOnly(), el -> visitLeftPadded(el, q)))
                .withSpecifier(q.receive(exportSpecifier.getSpecifier(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(exportSpecifier.getType(), type -> visitType(type, q)));
    }

    public Space visitSpace(Space space, RpcReceiveQueue q) {
        return delegate.visitSpace(space, q);
    }

    public <J2 extends J> JContainer<J2> visitContainer(JContainer<J2> container, RpcReceiveQueue q) {
        return delegate.visitContainer(container, q);
    }

    public <T> JLeftPadded<T> visitLeftPadded(JLeftPadded<T> left, RpcReceiveQueue q) {
        return delegate.visitLeftPadded(left, q);
    }

    public <T> JLeftPadded<T> visitLeftPadded(JLeftPadded<T> left, RpcReceiveQueue q, ValueCodec<T> codec) {
        return delegate.visitLeftPadded(left, q, codec);
    }

    public <T> JRightPadded<T> visitRightPadded(JRightPadded<T> right, RpcReceiveQueue q) {
        return delegate.visitRightPadded(right, q);
    }

    @Override
    public JavaType visitType(@SuppressWarnings("NullableProblems") JavaType javaType,
                              RpcReceiveQueue rpcReceiveQueue) {
        return requireNonNull(super.visitType(javaType, rpcReceiveQueue));
    }

    private static class JavaScriptReceiverDelegate extends JavaReceiver {
        private final JavaScriptReceiver delegate;

        public JavaScriptReceiverDelegate(JavaScriptReceiver delegate) {
            this.delegate = delegate;
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, RpcReceiveQueue p) {
            if (tree instanceof JS) {
                return delegate.visit(tree, p);
            }
            return super.visit(tree, p);
        }
    }
}
