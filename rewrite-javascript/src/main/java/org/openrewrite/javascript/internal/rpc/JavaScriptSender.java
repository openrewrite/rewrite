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
package org.openrewrite.javascript.internal.rpc;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.java.internal.rpc.JavaSender;
import org.openrewrite.java.tree.*;
import org.openrewrite.javascript.JavaScriptVisitor;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.javascript.tree.JSX;
import org.openrewrite.rpc.RpcSendQueue;

import static org.openrewrite.rpc.Reference.asRef;
import static org.openrewrite.rpc.Reference.getValueNonNull;

/**
 * A sender for JavaScript AST elements that uses the Java RPC framework.
 * This class implements a double delegation pattern with {@link JavaSender}
 * to handle both JavaScript and Java elements.
 */
public class JavaScriptSender extends JavaScriptVisitor<RpcSendQueue> {
    private final JavaScriptSenderDelegate delegate = new JavaScriptSenderDelegate(this);

    @Override
    public @Nullable J visit(@Nullable Tree tree, RpcSendQueue p) {
        if (tree instanceof JS) {
            return super.visit(tree, p);
        }
        return delegate.visit(tree, p);
    }

    @Override
    public J preVisit(J j, RpcSendQueue q) {
        q.getAndSend(j, Tree::getId);
        q.getAndSend(j, J::getPrefix, space -> visitSpace(space, q));
        q.getAndSend(j, Tree::getMarkers);

        return j;
    }

    @Override
    public J visitJsCompilationUnit(JS.CompilationUnit cu, RpcSendQueue q) {
        q.getAndSend(cu, c -> c.getSourcePath().toString());
        q.getAndSend(cu, c -> c.getCharset().name());
        q.getAndSend(cu, JS.CompilationUnit::isCharsetBomMarked);
        q.getAndSend(cu, JS.CompilationUnit::getChecksum);
        q.getAndSend(cu, JS.CompilationUnit::getFileAttributes);
        q.getAndSendList(cu, c -> c.getPadding().getStatements(), stmt -> stmt.getElement().getId(), stmt -> visitRightPadded(stmt, q));
        q.getAndSend(cu, JS.CompilationUnit::getEof, space -> visitSpace(space, q));

        return cu;
    }

    @Override
    public J visitAlias(JS.Alias alias, RpcSendQueue q) {
        q.getAndSend(alias, el -> el.getPadding().getPropertyName(), el -> visitRightPadded(el, q));
        q.getAndSend(alias, JS.Alias::getAlias, el -> visit(el, q));
        return alias;
    }

    @Override
    public J visitArrowFunction(JS.ArrowFunction arrowFunction, RpcSendQueue q) {
        q.getAndSendList(arrowFunction, JS.ArrowFunction::getLeadingAnnotations, J.Annotation::getId, el -> visit(el, q));
        q.getAndSendList(arrowFunction, JS.ArrowFunction::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSend(arrowFunction, JS.ArrowFunction::getTypeParameters, el -> visit(el, q));
        q.getAndSend(arrowFunction, JS.ArrowFunction::getLambda, el -> visit(el, q));
        q.getAndSend(arrowFunction, JS.ArrowFunction::getReturnTypeExpression, el -> visit(el, q));
        return arrowFunction;
    }

    @Override
    public J visitAwait(JS.Await await, RpcSendQueue q) {
        q.getAndSend(await, JS.Await::getExpression, el -> visit(el, q));
        q.getAndSend(await, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return await;
    }

    @Override
    public J visitConditionalType(JS.ConditionalType conditionalType, RpcSendQueue q) {
        q.getAndSend(conditionalType, JS.ConditionalType::getCheckType, el -> visit(el, q));
        q.getAndSend(conditionalType, el -> el.getPadding().getCondition(), el -> visitLeftPadded(el, q));
        q.getAndSend(conditionalType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return conditionalType;
    }

    @Override
    public J visitDelete(JS.Delete delete, RpcSendQueue q) {
        q.getAndSend(delete, JS.Delete::getExpression, el -> visit(el, q));
        return delete;
    }

    @Override
    public J visitExpressionStatement(JS.ExpressionStatement expressionStatement, RpcSendQueue q) {
        q.getAndSend(expressionStatement, JS.ExpressionStatement::getExpression, el -> visit(el, q));
        return expressionStatement;
    }

    @Override
    public J visitExpressionWithTypeArguments(JS.ExpressionWithTypeArguments expressionWithTypeArguments, RpcSendQueue q) {
        q.getAndSend(expressionWithTypeArguments, JS.ExpressionWithTypeArguments::getClazz, el -> visit(el, q));
        q.getAndSend(expressionWithTypeArguments, el -> el.getPadding().getTypeArguments(), el -> visitContainer(el, q));
        q.getAndSend(expressionWithTypeArguments, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return expressionWithTypeArguments;
    }


    @Override
    public J visitFunctionType(JS.FunctionType functionType, RpcSendQueue q) {
        q.getAndSendList(functionType, JS.FunctionType::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSend(functionType, el -> el.getPadding().getConstructorType(), el -> visitLeftPadded(el, q));
        q.getAndSend(functionType, JS.FunctionType::getTypeParameters, el -> visit(el, q));
        q.getAndSend(functionType, el -> el.getPadding().getParameters(), el -> visitContainer(el, q));
        q.getAndSend(functionType, el -> el.getPadding().getReturnType(), el -> visitLeftPadded(el, q));
        return functionType;
    }

    @Override
    public J visitInferType(JS.InferType inferType, RpcSendQueue q) {
        q.getAndSend(inferType, el -> el.getPadding().getTypeParameter(), el -> visitLeftPadded(el, q));
        q.getAndSend(inferType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return inferType;
    }

    @Override
    public J visitImportType(JS.ImportType importType, RpcSendQueue q) {
        q.getAndSend(importType, el -> el.getPadding().getHasTypeof(), el -> visitRightPadded(el, q));
        q.getAndSend(importType, el -> el.getPadding().getArgumentAndAttributes(), el -> visitContainer(el, q));
        q.getAndSend(importType, el -> el.getPadding().getQualifier(), el -> visitLeftPadded(el, q));
        q.getAndSend(importType, el -> el.getPadding().getTypeArguments(), el -> visitContainer(el, q));
        q.getAndSend(importType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return importType;
    }

    @Override
    public J visitImportDeclaration(JS.Import jsImport, RpcSendQueue q) {
        q.getAndSendList(jsImport, JS.Import::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSend(jsImport, JS.Import::getImportClause, el -> visit(el, q));
        q.getAndSend(jsImport, el -> el.getPadding().getModuleSpecifier(), el -> visitLeftPadded(el, q));
        q.getAndSend(jsImport, JS.Import::getAttributes, el -> visit(el, q));
        q.getAndSend(jsImport, el -> el.getPadding().getInitializer(), el -> visitLeftPadded(el, q));
        return jsImport;
    }

    @Override
    public J visitImportClause(JS.ImportClause importClause, RpcSendQueue q) {
        q.getAndSend(importClause, JS.ImportClause::isTypeOnly);
        q.getAndSend(importClause, el -> el.getPadding().getName(), el -> visitRightPadded(el, q));
        q.getAndSend(importClause, JS.ImportClause::getNamedBindings, el -> visit(el, q));
        return importClause;
    }

    @Override
    public J visitNamedImports(JS.NamedImports namedImports, RpcSendQueue q) {
        q.getAndSend(namedImports, el -> el.getPadding().getElements(), el -> visitContainer(el, q));
        q.getAndSend(namedImports, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return namedImports;
    }

    @Override
    public J visitImportSpecifier(JS.ImportSpecifier importSpecifier, RpcSendQueue q) {
        q.getAndSend(importSpecifier, el -> el.getPadding().getImportType(), el -> visitLeftPadded(el, q));
        q.getAndSend(importSpecifier, JS.ImportSpecifier::getSpecifier, el -> visit(el, q));
        q.getAndSend(importSpecifier, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return importSpecifier;
    }

    @Override
    public J visitImportAttributes(JS.ImportAttributes importAttributes, RpcSendQueue q) {
        q.getAndSend(importAttributes, JS.ImportAttributes::getToken);
        q.getAndSend(importAttributes, el -> el.getPadding().getElements(), el -> visitContainer(el, q));
        return importAttributes;
    }

    @Override
    public J visitImportTypeAttributes(JS.ImportTypeAttributes importTypeAttributes, RpcSendQueue q) {
        q.getAndSend(importTypeAttributes, el -> el.getPadding().getToken(), el -> visitRightPadded(el, q));
        q.getAndSend(importTypeAttributes, el -> el.getPadding().getElements(), el -> visitContainer(el, q));
        q.getAndSend(importTypeAttributes, JS.ImportTypeAttributes::getEnd, el -> visitSpace(getValueNonNull(el), q));
        return importTypeAttributes;
    }

    @Override
    public J visitImportAttribute(JS.ImportAttribute importAttribute, RpcSendQueue q) {
        q.getAndSend(importAttribute, JS.ImportAttribute::getName, el -> visit(el, q));
        q.getAndSend(importAttribute, el -> el.getPadding().getValue(), el -> visitLeftPadded(el, q));
        return importAttribute;
    }

    @Override
    public J visitBinaryExtensions(JS.Binary binary, RpcSendQueue q) {
        q.getAndSend(binary, JS.Binary::getLeft, el -> visit(el, q));
        q.getAndSend(binary, el -> el.getPadding().getOperator(), el -> visitLeftPadded(el, q));
        q.getAndSend(binary, JS.Binary::getRight, el -> visit(el, q));
        q.getAndSend(binary, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return binary;
    }

    @Override
    public J visitLiteralType(JS.LiteralType literalType, RpcSendQueue q) {
        q.getAndSend(literalType, JS.LiteralType::getLiteral, el -> visit(el, q));
        q.getAndSend(literalType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return literalType;
    }

    @Override
    public J visitMappedType(JS.MappedType mappedType, RpcSendQueue q) {
        q.getAndSend(mappedType, el -> el.getPadding().getPrefixToken(), el -> visitLeftPadded(el, q));
        q.getAndSend(mappedType, el -> el.getPadding().getHasReadonly(), el -> visitLeftPadded(el, q));
        q.getAndSend(mappedType, JS.MappedType::getKeysRemapping, el -> visit(el, q));
        q.getAndSend(mappedType, el -> el.getPadding().getSuffixToken(), el -> visitLeftPadded(el, q));
        q.getAndSend(mappedType, el -> el.getPadding().getHasQuestionToken(), el -> visitLeftPadded(el, q));
        q.getAndSend(mappedType, el -> el.getPadding().getValueType(), el -> visitContainer(el, q));
        q.getAndSend(mappedType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return mappedType;
    }

    @Override
    public J visitMappedTypeKeysRemapping(JS.MappedType.KeysRemapping keysRemapping, RpcSendQueue q) {
        q.getAndSend(keysRemapping, el -> el.getPadding().getTypeParameter(), el -> visitRightPadded(el, q));
        q.getAndSend(keysRemapping, el -> el.getPadding().getNameType(), el -> visitRightPadded(el, q));
        return keysRemapping;
    }

    @Override
    public J visitMappedTypeParameter(JS.MappedType.Parameter parameter, RpcSendQueue q) {
        q.getAndSend(parameter, JS.MappedType.Parameter::getName, el -> visit(el, q));
        q.getAndSend(parameter, el -> el.getPadding().getIterateType(), el -> visitLeftPadded(el, q));
        return parameter;
    }

    @Override
    public J visitObjectBindingPattern(JS.ObjectBindingPattern objectBindingPattern, RpcSendQueue q) {
        q.getAndSendList(objectBindingPattern, JS.ObjectBindingPattern::getLeadingAnnotations, J.Annotation::getId, el -> visit(el, q));
        q.getAndSendList(objectBindingPattern, JS.ObjectBindingPattern::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSend(objectBindingPattern, JS.ObjectBindingPattern::getTypeExpression, el -> visit(el, q));
        q.getAndSend(objectBindingPattern, el -> el.getPadding().getBindings(), el -> visitContainer(el, q));
        q.getAndSend(objectBindingPattern, el -> el.getPadding().getInitializer(), el -> visitLeftPadded(el, q));
        return objectBindingPattern;
    }

    @Override
    public J visitPropertyAssignment(JS.PropertyAssignment propertyAssignment, RpcSendQueue q) {
        q.getAndSend(propertyAssignment, el -> el.getPadding().getName(), el -> visitRightPadded(el, q));
        q.getAndSend(propertyAssignment, JS.PropertyAssignment::getAssigmentToken);
        q.getAndSend(propertyAssignment, JS.PropertyAssignment::getInitializer, el -> visit(el, q));
        return propertyAssignment;
    }

    @Override
    public J visitSatisfiesExpression(JS.SatisfiesExpression satisfiesExpression, RpcSendQueue q) {
        q.getAndSend(satisfiesExpression, JS.SatisfiesExpression::getExpression, el -> visit(el, q));
        q.getAndSend(satisfiesExpression, el -> el.getPadding().getSatisfiesType(), el -> visitLeftPadded(el, q));
        q.getAndSend(satisfiesExpression, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return satisfiesExpression;
    }

    @Override
    public J visitScopedVariableDeclarations(JS.ScopedVariableDeclarations scopedVariableDeclarations, RpcSendQueue q) {
        q.getAndSendList(scopedVariableDeclarations, JS.ScopedVariableDeclarations::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSendList(scopedVariableDeclarations, el -> el.getPadding().getVariables(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        return scopedVariableDeclarations;
    }

    @Override
    public J visitStatementExpression(JS.StatementExpression statementExpression, RpcSendQueue q) {
        q.getAndSend(statementExpression, JS.StatementExpression::getStatement, el -> visit(el, q));
        return statementExpression;
    }

    @Override
    public J visitTaggedTemplateExpression(JS.TaggedTemplateExpression taggedTemplateExpression, RpcSendQueue q) {
        q.getAndSend(taggedTemplateExpression, el -> el.getPadding().getTag(), el -> visitRightPadded(el, q));
        q.getAndSend(taggedTemplateExpression, el -> el.getPadding().getTypeArguments(), el -> visitContainer(el, q));
        q.getAndSend(taggedTemplateExpression, JS.TaggedTemplateExpression::getTemplateExpression, el -> visit(el, q));
        q.getAndSend(taggedTemplateExpression, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return taggedTemplateExpression;
    }

    @Override
    public J visitTemplateExpression(JS.TemplateExpression templateExpression, RpcSendQueue q) {
        q.getAndSend(templateExpression, JS.TemplateExpression::getHead, el -> visit(el, q));
        q.getAndSendList(templateExpression, el -> el.getPadding().getSpans(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        q.getAndSend(templateExpression, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return templateExpression;
    }

    @Override
    public J visitTemplateExpressionSpan(JS.TemplateExpression.Span span, RpcSendQueue q) {
        q.getAndSend(span, JS.TemplateExpression.Span::getExpression, el -> visit(el, q));
        q.getAndSend(span, JS.TemplateExpression.Span::getTail, el -> visit(el, q));
        return span;
    }

    @Override
    public J visitTuple(JS.Tuple tuple, RpcSendQueue q) {
        q.getAndSend(tuple, el -> el.getPadding().getElements(), el -> visitContainer(el, q));
        q.getAndSend(tuple, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return tuple;
    }

    @Override
    public J visitTypeDeclaration(JS.TypeDeclaration typeDeclaration, RpcSendQueue q) {
        q.getAndSendList(typeDeclaration, JS.TypeDeclaration::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSend(typeDeclaration, el -> el.getPadding().getName(), el -> visitLeftPadded(el, q));
        q.getAndSend(typeDeclaration, JS.TypeDeclaration::getTypeParameters, el -> visit(el, q));
        q.getAndSend(typeDeclaration, el -> el.getPadding().getInitializer(), el -> visitLeftPadded(el, q));
        q.getAndSend(typeDeclaration, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return typeDeclaration;
    }

    @Override
    public J visitTypeOf(JS.TypeOf typeOf, RpcSendQueue q) {
        q.getAndSend(typeOf, JS.TypeOf::getExpression, el -> visit(el, q));
        q.getAndSend(typeOf, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return typeOf;
    }

    @Override
    public J visitTypeTreeExpression(JS.TypeTreeExpression typeTreeExpression, RpcSendQueue q) {
        q.getAndSend(typeTreeExpression, JS.TypeTreeExpression::getExpression, el -> visit(el, q));
        return typeTreeExpression;
    }

    @Override
    public J visitAs(JS.As as_, RpcSendQueue q) {
        q.getAndSend(as_, el -> el.getPadding().getLeft(), el -> visitRightPadded(el, q));
        q.getAndSend(as_, JS.As::getRight, el -> visit(el, q));
        q.getAndSend(as_, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return as_;
    }

    @Override
    public J visitAssignmentOperationExtensions(JS.AssignmentOperation assignmentOperation, RpcSendQueue q) {
        q.getAndSend(assignmentOperation, JS.AssignmentOperation::getVariable, el -> visit(el, q));
        q.getAndSend(assignmentOperation, el -> el.getPadding().getOperator(), el -> visitLeftPadded(el, q));
        q.getAndSend(assignmentOperation, JS.AssignmentOperation::getAssignment, el -> visit(el, q));
        q.getAndSend(assignmentOperation, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return assignmentOperation;
    }

    @Override
    public J visitIndexedAccessType(JS.IndexedAccessType indexedAccessType, RpcSendQueue q) {
        q.getAndSend(indexedAccessType, JS.IndexedAccessType::getObjectType, el -> visit(el, q));
        q.getAndSend(indexedAccessType, JS.IndexedAccessType::getIndexType, el -> visit(el, q));
        q.getAndSend(indexedAccessType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return indexedAccessType;
    }

    @Override
    public J visitIndexedAccessTypeIndexType(JS.IndexedAccessType.IndexType indexType, RpcSendQueue q) {
        q.getAndSend(indexType, el -> el.getPadding().getElement(), el -> visitRightPadded(el, q));
        q.getAndSend(indexType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return indexType;
    }

    @Override
    public J visitTypeQuery(JS.TypeQuery typeQuery, RpcSendQueue q) {
        q.getAndSend(typeQuery, JS.TypeQuery::getTypeExpression, el -> visit(el, q));
        q.getAndSend(typeQuery, el -> el.getPadding().getTypeArguments(), el -> visitContainer(el, q));
        q.getAndSend(typeQuery, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return typeQuery;
    }

    @Override
    public J visitTypeInfo(JS.TypeInfo typeInfo, RpcSendQueue q) {
        q.getAndSend(typeInfo, JS.TypeInfo::getTypeIdentifier, el -> visit(el, q));
        return typeInfo;
    }

    @Override
    public J visitComputedPropertyName(JS.ComputedPropertyName computedPropertyName, RpcSendQueue q) {
        q.getAndSend(computedPropertyName, el -> el.getPadding().getExpression(), el -> visitRightPadded(el, q));
        return computedPropertyName;
    }

    @Override
    public J visitTypeOperator(JS.TypeOperator typeOperator, RpcSendQueue q) {
        q.getAndSend(typeOperator, JS.TypeOperator::getOperator);
        q.getAndSend(typeOperator, el -> el.getPadding().getExpression(), el -> visitLeftPadded(el, q));
        return typeOperator;
    }

    @Override
    public J visitTypePredicate(JS.TypePredicate typePredicate, RpcSendQueue q) {
        q.getAndSend(typePredicate, el -> el.getPadding().getAsserts(), el -> visitLeftPadded(el, q));
        q.getAndSend(typePredicate, JS.TypePredicate::getParameterName, el -> visit(el, q));
        q.getAndSend(typePredicate, el -> el.getPadding().getExpression(), el -> visitLeftPadded(el, q));
        q.getAndSend(typePredicate, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return typePredicate;
    }

    @Override
    public J visitUnion(JS.Union union, RpcSendQueue q) {
        q.getAndSendList(union, el -> el.getPadding().getTypes(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        q.getAndSend(union, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return union;
    }

    @Override
    public J visitIntersection(JS.Intersection intersection, RpcSendQueue q) {
        q.getAndSendList(intersection, el -> el.getPadding().getTypes(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        q.getAndSend(intersection, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return intersection;
    }

    @Override
    public J visitVoid(JS.Void void_, RpcSendQueue q) {
        q.getAndSend(void_, JS.Void::getExpression, el -> visit(el, q));
        return void_;
    }

    @Override
    public J visitWithStatement(JS.WithStatement withStatement, RpcSendQueue q) {
        q.getAndSend(withStatement, JS.WithStatement::getExpression, el -> visit(el, q));
        q.getAndSend(withStatement, el -> el.getPadding().getBody(), el -> visitRightPadded(el, q));
        return withStatement;
    }

    @Override
    public J visitJsxTag(JSX.Tag tag, RpcSendQueue q) {
        q.getAndSend(tag, el -> el.getPadding().getOpenName(), el -> visitLeftPadded(el, q));
        q.getAndSend(tag, JSX.Tag::getTypeArguments, el -> visitContainer(el, q));
        q.getAndSend(tag, JSX.Tag::getAfterName, space -> visitSpace(space, q));
        q.getAndSendList(tag, el -> el.getPadding().getAttributes(), attr -> attr.getElement().getId(), attr -> visitRightPadded(attr, q));

        q.getAndSend(tag, JSX.Tag::getSelfClosing, space -> visitSpace(space, q));
        q.getAndSendList(tag, JSX.Tag::getChildren, Tree::getId, child -> visit(child, q));
        q.getAndSend(tag, el -> el.getPadding().getClosingName(), el -> visitLeftPadded(el, q));
        q.getAndSend(tag, JSX.Tag::getAfterClosingName, space -> visitSpace(space, q));

        return tag;
    }

    @Override
    public J visitJsxAttribute(JSX.Attribute attribute, RpcSendQueue q) {
        q.getAndSend(attribute, JSX.Attribute::getKey, el -> visit(el, q));
        q.getAndSend(attribute, el -> el.getPadding().getValue(), el -> visitLeftPadded(el, q));
        return attribute;
    }

    @Override
    public J visitJsxSpreadAttribute(JSX.SpreadAttribute spreadAttribute, RpcSendQueue q) {
        q.getAndSend(spreadAttribute, JSX.SpreadAttribute::getDots, space -> visitSpace(space, q));
        q.getAndSend(spreadAttribute, el -> el.getPadding().getExpression(), el -> visitRightPadded(el, q));
        return spreadAttribute;
    }

    @Override
    public J visitJsxEmbeddedExpression(JSX.EmbeddedExpression embeddedExpression, RpcSendQueue q) {
        q.getAndSend(embeddedExpression, el -> el.getPadding().getExpression(), el -> visitRightPadded(el, q));
        return embeddedExpression;
    }

    @Override
    public J visitJsxNamespacedName(JSX.NamespacedName namespacedName, RpcSendQueue q) {
        q.getAndSend(namespacedName, JSX.NamespacedName::getNamespace, el -> visit(el, q));
        q.getAndSend(namespacedName, el -> el.getPadding().getName(), el -> visitLeftPadded(el, q));
        return namespacedName;
    }

    @Override
    public J visitIndexSignatureDeclaration(JS.IndexSignatureDeclaration indexSignatureDeclaration, RpcSendQueue q) {
        q.getAndSendList(indexSignatureDeclaration, JS.IndexSignatureDeclaration::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSend(indexSignatureDeclaration, el -> el.getPadding().getParameters(), el -> visitContainer(el, q));
        q.getAndSend(indexSignatureDeclaration, el -> el.getPadding().getTypeExpression(), el -> visitLeftPadded(el, q));
        q.getAndSend(indexSignatureDeclaration, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return indexSignatureDeclaration;
    }

    @Override
    public J visitComputedPropertyMethodDeclaration(JS.ComputedPropertyMethodDeclaration computedPropMethod, RpcSendQueue q) {
        q.getAndSendList(computedPropMethod, JS.ComputedPropertyMethodDeclaration::getLeadingAnnotations, J.Annotation::getId, el -> visit(el, q));
        q.getAndSendList(computedPropMethod, JS.ComputedPropertyMethodDeclaration::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSend(computedPropMethod, JS.ComputedPropertyMethodDeclaration::getTypeParameters, el -> visit(el, q));
        q.getAndSend(computedPropMethod, JS.ComputedPropertyMethodDeclaration::getReturnTypeExpression, el -> visit(el, q));
        q.getAndSend(computedPropMethod, JS.ComputedPropertyMethodDeclaration::getName, el -> visit(el, q));
        q.getAndSend(computedPropMethod, el -> el.getPadding().getParameters(), el -> visitContainer(el, q));
        q.getAndSend(computedPropMethod, JS.ComputedPropertyMethodDeclaration::getBody, el -> visit(el, q));
        q.getAndSend(computedPropMethod, el -> asRef(el.getMethodType()), el -> visitType(getValueNonNull(el), q));
        return computedPropMethod;
    }

    @Override
    public J visitForOfLoop(JS.ForOfLoop forOfLoop, RpcSendQueue q) {
        q.getAndSend(forOfLoop, JS.ForOfLoop::getAwait, space -> visitSpace(space, q));
        q.getAndSend(forOfLoop, JS.ForOfLoop::getLoop, el -> visit(el, q));
        return forOfLoop;
    }

    @Override
    public J visitForInLoop(JS.ForInLoop forInLoop, RpcSendQueue q) {
        q.getAndSend(forInLoop, JS.ForInLoop::getControl, el -> visit(el, q));
        q.getAndSend(forInLoop, el -> el.getPadding().getBody(), el -> visitRightPadded(el, q));
        return forInLoop;
    }

    @Override
    public J visitNamespaceDeclaration(JS.NamespaceDeclaration namespaceDeclaration, RpcSendQueue q) {
        q.getAndSendList(namespaceDeclaration, JS.NamespaceDeclaration::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSend(namespaceDeclaration, el -> el.getPadding().getKeywordType(), el -> visitLeftPadded(el, q));
        q.getAndSend(namespaceDeclaration, el -> el.getPadding().getName(), el -> visitRightPadded(el, q));
        q.getAndSend(namespaceDeclaration, JS.NamespaceDeclaration::getBody, el -> visit(el, q));
        return namespaceDeclaration;
    }

    @Override
    public J visitTypeLiteral(JS.TypeLiteral typeLiteral, RpcSendQueue q) {
        q.getAndSend(typeLiteral, JS.TypeLiteral::getMembers, el -> visit(el, q));
        q.getAndSend(typeLiteral, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return typeLiteral;
    }

    @Override
    public J visitArrayBindingPattern(JS.ArrayBindingPattern arrayBindingPattern, RpcSendQueue q) {
        q.getAndSend(arrayBindingPattern, el -> el.getPadding().getElements(), el -> visitContainer(el, q));
        q.getAndSend(arrayBindingPattern, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return arrayBindingPattern;
    }

    @Override
    public J visitBindingElement(JS.BindingElement bindingElement, RpcSendQueue q) {
        q.getAndSend(bindingElement, el -> el.getPadding().getPropertyName(), el -> visitRightPadded(el, q));
        q.getAndSend(bindingElement, JS.BindingElement::getName, el -> visit(el, q));
        q.getAndSend(bindingElement, el -> el.getPadding().getInitializer(), el -> visitLeftPadded(el, q));
        q.getAndSend(bindingElement, el -> asRef(el.getVariableType()), el -> visitType(getValueNonNull(el), q));
        return bindingElement;
    }

    @Override
    public J visitExportDeclaration(JS.ExportDeclaration exportDeclaration, RpcSendQueue q) {
        q.getAndSendList(exportDeclaration, JS.ExportDeclaration::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSend(exportDeclaration, el -> el.getPadding().getTypeOnly(), el -> visitLeftPadded(el, q));
        q.getAndSend(exportDeclaration, JS.ExportDeclaration::getExportClause, el -> visit(el, q));
        q.getAndSend(exportDeclaration, el -> el.getPadding().getModuleSpecifier(), el -> visitLeftPadded(el, q));
        q.getAndSend(exportDeclaration, JS.ExportDeclaration::getAttributes, el -> visit(el, q));
        return exportDeclaration;
    }

    @Override
    public J visitExportAssignment(JS.ExportAssignment exportAssignment, RpcSendQueue q) {
        q.getAndSend(exportAssignment, JS.ExportAssignment::isExportEquals);
        q.getAndSend(exportAssignment, el -> el.getPadding().getExpression(), el -> visitLeftPadded(el, q));
        return exportAssignment;
    }

    @Override
    public J visitNamedExports(JS.NamedExports namedExports, RpcSendQueue q) {
        q.getAndSend(namedExports, el -> el.getPadding().getElements(), el -> visitContainer(el, q));
        q.getAndSend(namedExports, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return namedExports;
    }

    @Override
    public J visitExportSpecifier(JS.ExportSpecifier exportSpecifier, RpcSendQueue q) {
        q.getAndSend(exportSpecifier, el -> el.getPadding().getTypeOnly(), el -> visitLeftPadded(el, q));
        q.getAndSend(exportSpecifier, JS.ExportSpecifier::getSpecifier, el -> visit(el, q));
        q.getAndSend(exportSpecifier, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return exportSpecifier;
    }

    @Override
    public J visitFunctionCall(JS.FunctionCall functionCall, RpcSendQueue q) {
        q.getAndSend(functionCall, m -> m.getPadding().getFunction(), select -> visitRightPadded(select, q));
        q.getAndSend(functionCall, m -> m.getPadding().getTypeParameters(), typeParams -> visitContainer(typeParams, q));
        q.getAndSend(functionCall, m -> m.getPadding().getArguments(), args -> visitContainer(args, q));
        q.getAndSend(functionCall, m -> asRef(m.getMethodType()), type -> visitType(getValueNonNull(type), q));
        return functionCall;
    }

    private <T> void visitLeftPadded(JLeftPadded<T> left, RpcSendQueue q) {
        delegate.visitLeftPadded(left, q);
    }

    private <T> void visitRightPadded(JRightPadded<T> right, RpcSendQueue q) {
        delegate.visitRightPadded(right, q);
    }

    private <J2 extends J> void visitContainer(JContainer<J2> container, RpcSendQueue q) {
        delegate.visitContainer(container, q);
    }

    private void visitSpace(Space space, RpcSendQueue q) {
        delegate.visitSpace(space, q);
    }

    @Override
    public @Nullable JavaType visitType(@Nullable JavaType javaType, RpcSendQueue q) {
        return delegate.visitType(javaType, q);
    }

    private static class JavaScriptSenderDelegate extends JavaSender {
        private final JavaScriptSender delegate;

        public JavaScriptSenderDelegate(JavaScriptSender delegate) {
            this.delegate = delegate;
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, RpcSendQueue p) {
            if (tree instanceof JS) {
                return delegate.visit(tree, p);
            }
            return super.visit(tree, p);
        }
    }
}
