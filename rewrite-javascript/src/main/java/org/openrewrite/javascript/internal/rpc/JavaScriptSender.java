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
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.internal.rpc.JavaSender;
import org.openrewrite.java.tree.*;
import org.openrewrite.javascript.JavaScriptVisitor;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.rpc.RpcSendQueue;

/**
 * A sender for JavaScript AST elements that uses the Java RPC framework.
 * This class implements a double delegation pattern with {@link JavaSender}
 * to handle both JavaScript and Java elements.
 */
public class JavaScriptSender extends JavaScriptVisitor<RpcSendQueue> {
    private final JavaScriptSenderDelegate delegate = new JavaScriptSenderDelegate(this);

    @Override
    public @Nullable J visit(@Nullable Tree tree, RpcSendQueue p, Cursor parent) {
        if (tree instanceof JS) {
            return super.visit(tree, p, parent);
        }
        return delegate.visit(tree, p, parent);
    }

    @Override
    public J preVisit(J j, RpcSendQueue q) {
        q.getAndSend(j, Tree::getId);
        q.getAndSend(j, J::getPrefix, space -> visitSpace(space, q));
        q.sendMarkers(j, Tree::getMarkers);

        return j;
    }

    @Override
    public J visitAlias(JS.Alias alias, RpcSendQueue q) {
        q.getAndSend(alias, a -> a.getPadding().getPropertyName(), propName -> visitRightPadded(propName, q));
        q.getAndSend(alias, JS.Alias::getAlias, aliasExpr -> visit(aliasExpr, q));
        q.getAndSend(alias, JS.Alias::getType, type -> visitType(type, q));

        return alias;
    }

    @Override
    public J visitArrowFunction(JS.ArrowFunction arrowFunction, RpcSendQueue q) {
        q.getAndSendList(arrowFunction, JS.ArrowFunction::getLeadingAnnotations, J.Annotation::getId, annot -> visit(annot, q));
        q.getAndSendList(arrowFunction, JS.ArrowFunction::getModifiers, J.Modifier::getId, mod -> visit(mod, q));
        q.getAndSend(arrowFunction, JS.ArrowFunction::getTypeParameters, params -> visit(params, q));
        q.getAndSend(arrowFunction, JS.ArrowFunction::getParameters, params -> visit(params, q));
        q.getAndSend(arrowFunction, JS.ArrowFunction::getReturnTypeExpression, type -> visit(type, q));
        q.getAndSend(arrowFunction, a -> a.getPadding().getBody(), body -> visitLeftPadded(body, q));
        q.getAndSend(arrowFunction, JS.ArrowFunction::getType, type -> visitType(type, q));

        return arrowFunction;
    }

    @Override
    public J visitAwait(JS.Await await, RpcSendQueue q) {
        q.getAndSend(await, JS.Await::getExpression, expr -> visit(expr, q));
        q.getAndSend(await, JS.Await::getType, type -> visitType(type, q));

        return await;
    }

    @Override
    public J visitCompilationUnit(JS.CompilationUnit cu, RpcSendQueue q) {
        q.getAndSend(cu, c -> c.getSourcePath().toString());
        q.getAndSend(cu, c -> c.getCharset().name());
        q.getAndSend(cu, JS.CompilationUnit::isCharsetBomMarked);
        q.getAndSend(cu, JS.CompilationUnit::getChecksum);
        q.getAndSend(cu, JS.CompilationUnit::getFileAttributes);
        q.getAndSendList(cu, c -> c.getPadding().getImports(), imp -> imp.getElement().getId(), imp -> visitRightPadded(imp, q));
        q.getAndSendList(cu, c -> c.getPadding().getStatements(), stmt -> stmt.getElement().getId(), stmt -> visitRightPadded(stmt, q));
        q.getAndSend(cu, JS.CompilationUnit::getEof, space -> visitSpace(space, q));

        return cu;
    }

    @Override
    public J visitConditionalType(JS.ConditionalType conditionalType, RpcSendQueue q) {
        q.getAndSend(conditionalType, JS.ConditionalType::getCheckType, type -> visit(type, q));
        q.getAndSend(conditionalType, c -> c.getPadding().getCondition(), cond -> visitContainer(cond, q));
        q.getAndSend(conditionalType, JS.ConditionalType::getType, type -> visitType(type, q));

        return conditionalType;
    }

    @Override
    public J visitDefaultType(JS.DefaultType defaultType, RpcSendQueue q) {
        q.getAndSend(defaultType, JS.DefaultType::getLeft, left -> visit(left, q));
        q.getAndSend(defaultType, JS.DefaultType::getBeforeEquals, space -> visitSpace(space, q));
        q.getAndSend(defaultType, JS.DefaultType::getRight, right -> visit(right, q));
        q.getAndSend(defaultType, JS.DefaultType::getType, type -> visitType(type, q));

        return defaultType;
    }

    @Override
    public J visitDelete(JS.Delete deleteExpr, RpcSendQueue q) {
        q.getAndSend(deleteExpr, JS.Delete::getExpression, expr -> visit(expr, q));
        q.getAndSend(deleteExpr, JS.Delete::getType, type -> visitType(type, q));

        return deleteExpr;
    }

    @Override
    public J visitExport(JS.Export export_, RpcSendQueue q) {
        q.getAndSend(export_, e -> e.getPadding().getExports(), exports -> visitContainer(exports, q));
        q.getAndSend(export_, JS.Export::getFrom, space -> visitSpace(space, q));
        q.getAndSend(export_, JS.Export::getTarget, target -> visit(target, q));
        q.getAndSend(export_, e -> e.getPadding().getInitializer(), init -> visitLeftPadded(init, q));

        return export_;
    }

    @Override
    public J visitExpressionStatement(JS.ExpressionStatement exprStmt, RpcSendQueue q) {
        q.getAndSend(exprStmt, JS.ExpressionStatement::getExpression, expr -> visit(expr, q));

        return exprStmt;
    }

    @Override
    public J visitTrailingTokenStatement(JS.TrailingTokenStatement trailStmt, RpcSendQueue q) {
        q.getAndSend(trailStmt, t -> t.getPadding().getExpression(), expr -> visitRightPadded(expr, q));
        q.getAndSend(trailStmt, JS.TrailingTokenStatement::getType, type -> visitType(type, q));

        return trailStmt;
    }

    @Override
    public J visitExpressionWithTypeArguments(JS.ExpressionWithTypeArguments exprType, RpcSendQueue q) {
        q.getAndSend(exprType, JS.ExpressionWithTypeArguments::getClazz, clazz -> visit(clazz, q));
        q.getAndSend(exprType, e -> e.getPadding().getTypeArguments(), args -> visitContainer(args, q));
        q.getAndSend(exprType, JS.ExpressionWithTypeArguments::getType, type -> visitType(type, q));

        return exprType;
    }

    @Override
    public J visitFunctionType(JS.FunctionType functionType, RpcSendQueue q) {
        q.getAndSendList(functionType, JS.FunctionType::getModifiers, J.Modifier::getId, mod -> visit(mod, q));
        q.getAndSend(functionType, f -> f.getPadding().getConstructorType(), ct -> visitLeftPadded(ct, q));
        q.getAndSend(functionType, JS.FunctionType::getTypeParameters, params -> visit(params, q));
        q.getAndSend(functionType, f -> f.getPadding().getParameters(), params -> visitContainer(params, q));
        q.getAndSend(functionType, f -> f.getPadding().getReturnType(), returnType -> visitLeftPadded(returnType, q));
        q.getAndSend(functionType, JS.FunctionType::getType, type -> visitType(type, q));

        return functionType;
    }

    @Override
    public J visitImportAttribute(JS.ImportAttribute importAttr, RpcSendQueue q) {
        q.getAndSend(importAttr, JS.ImportAttribute::getName, name -> visit(name, q));
        q.getAndSend(importAttr, i -> i.getPadding().getValue(), value -> visitLeftPadded(value, q));

        return importAttr;
    }

    @Override
    public J visitImportAttributes(JS.ImportAttributes importAttrs, RpcSendQueue q) {
        q.getAndSend(importAttrs, JS.ImportAttributes::getToken);
        q.getAndSend(importAttrs, i -> i.getPadding().getElements(), elements -> visitContainer(elements, q));

        return importAttrs;
    }

    @Override
    public J visitImportType(JS.ImportType importType, RpcSendQueue q) {
        q.getAndSend(importType, i -> i.getPadding().getHasTypeof(), hasTypeof -> visitRightPadded(hasTypeof, q));
        q.getAndSend(importType, i -> i.getPadding().getArgumentAndAttributes(), args -> visitContainer(args, q));
        q.getAndSend(importType, i -> i.getPadding().getQualifier(), qualifier -> visitLeftPadded(qualifier, q));
        q.getAndSend(importType, i -> i.getPadding().getTypeArguments(), args -> visitContainer(args, q));
        q.getAndSend(importType, JS.ImportType::getType, type -> visitType(type, q));

        return importType;
    }

    @Override
    public J visitImportTypeAttributes(JS.ImportTypeAttributes importTypeAttrs, RpcSendQueue q) {
        q.getAndSend(importTypeAttrs, i -> i.getPadding().getToken(), token -> visitRightPadded(token, q));
        q.getAndSend(importTypeAttrs, i -> i.getPadding().getElements(), elements -> visitContainer(elements, q));
        q.getAndSend(importTypeAttrs, JS.ImportTypeAttributes::getEnd, space -> visitSpace(space, q));

        return importTypeAttrs;
    }

    @Override
    public J visitIndexedAccessType(JS.IndexedAccessType indexedAccessType, RpcSendQueue q) {
        q.getAndSend(indexedAccessType, JS.IndexedAccessType::getObjectType, objectType -> visit(objectType, q));
        q.getAndSend(indexedAccessType, JS.IndexedAccessType::getIndexType, indexType -> visit(indexType, q));
        q.getAndSend(indexedAccessType, JS.IndexedAccessType::getType, type -> visitType(type, q));
        return indexedAccessType;
    }

    @Override
    public J visitIndexedAccessTypeIndexType(JS.IndexedAccessType.IndexType indexType, RpcSendQueue q) {
        q.getAndSend(indexType, i -> i.getPadding().getElement(), element -> visitRightPadded(element, q));
        q.getAndSend(indexType, JS.IndexedAccessType.IndexType::getType, type -> visitType(type, q));
        return indexType;
    }


    @Override
    public J visitInferType(JS.InferType inferType, RpcSendQueue q) {
        q.getAndSend(inferType, i -> i.getPadding().getTypeParameter(), param -> visitLeftPadded(param, q));
        q.getAndSend(inferType, JS.InferType::getType, type -> visitType(type, q));

        return inferType;
    }

    @Override
    public J visitIntersection(JS.Intersection intersection, RpcSendQueue q) {
        q.getAndSendList(intersection, i -> i.getPadding().getTypes(), type -> type.getElement().getId(),
                type -> visitRightPadded(type, q));
        q.getAndSend(intersection, JS.Intersection::getType, type -> visitType(type, q));
        return intersection;
    }

    @Override
    public J visitJsAssignmentOperation(JS.JsAssignmentOperation assignOp, RpcSendQueue q) {
        q.getAndSend(assignOp, JS.JsAssignmentOperation::getVariable, variable -> visit(variable, q));
        q.getAndSend(assignOp, a -> a.getPadding().getOperator(), operator -> visitLeftPadded(operator, q));
        q.getAndSend(assignOp, JS.JsAssignmentOperation::getAssignment, assignment -> visit(assignment, q));
        q.getAndSend(assignOp, JS.JsAssignmentOperation::getType, type -> visitType(type, q));
        return assignOp;
    }

    @Override
    public J visitJsImport(JS.JsImport jsImport, RpcSendQueue q) {
        q.getAndSendList(jsImport, JS.JsImport::getModifiers, J.Modifier::getId, mod -> visit(mod, q));
        q.getAndSend(jsImport, JS.JsImport::getImportClause, clause -> visit(clause, q));
        q.getAndSend(jsImport, i -> i.getPadding().getModuleSpecifier(), spec -> visitLeftPadded(spec, q));
        q.getAndSend(jsImport, JS.JsImport::getAttributes, attrs -> visit(attrs, q));

        return jsImport;
    }

    @Override
    public J visitJsImportClause(JS.JsImportClause jsImportClause, RpcSendQueue q) {
        q.getAndSend(jsImportClause, JS.JsImportClause::isTypeOnly);
        q.getAndSend(jsImportClause, c -> c.getPadding().getName(), name -> visitRightPadded(name, q));
        q.getAndSend(jsImportClause, JS.JsImportClause::getNamedBindings, bindings -> visit(bindings, q));

        return jsImportClause;
    }

    @Override
    public J visitNamedImports(JS.NamedImports namedImports, RpcSendQueue q) {
        q.getAndSend(namedImports, n -> n.getPadding().getElements(), elements -> visitContainer(elements, q));
        q.getAndSend(namedImports, JS.NamedImports::getType, type -> visitType(type, q));

        return namedImports;
    }

    @Override
    public J visitJsImportSpecifier(JS.JsImportSpecifier jsImportSpecifier, RpcSendQueue q) {
        q.getAndSend(jsImportSpecifier, i -> i.getPadding().getImportType(), importType -> visitLeftPadded(importType, q));
        q.getAndSend(jsImportSpecifier, JS.JsImportSpecifier::getSpecifier, specifier -> visit(specifier, q));
        q.getAndSend(jsImportSpecifier, JS.JsImportSpecifier::getType, type -> visitType(type, q));

        return jsImportSpecifier;
    }

    @Override
    public J visitJSVariableDeclarations(JS.JSVariableDeclarations varDecls, RpcSendQueue q) {
        q.getAndSendList(varDecls, JS.JSVariableDeclarations::getLeadingAnnotations, J::getId, annot -> visitAnnotation(annot, q));
        q.getAndSendList(varDecls, JS.JSVariableDeclarations::getModifiers, J::getId, mod -> visitModifier(mod, q));
        q.getAndSend(varDecls, JS.JSVariableDeclarations::getTypeExpression, type -> visit(type, q));
        q.getAndSend(varDecls, JS.JSVariableDeclarations::getVarargs, space -> visitSpace(space, q));
        q.getAndSendList(varDecls, v -> v.getPadding().getVariables(), variable -> variable.getElement().getId(),
                variable -> visitRightPadded(variable, q));

        return varDecls;
    }

    @Override
    public J visitJSVariableDeclarationsJSNamedVariable(JS.JSVariableDeclarations.JSNamedVariable variable, RpcSendQueue q) {
        q.getAndSend(variable, JS.JSVariableDeclarations.JSNamedVariable::getName, name -> visit(name, q));
        q.getAndSendList(variable, JS.JSVariableDeclarations.JSNamedVariable::getDimensionsAfterName,
                JLeftPadded::getElement, dim -> visitLeftPadded(dim, q));
        q.getAndSend(variable, v -> v.getPadding().getInitializer(), init -> visitLeftPadded(init, q));
        q.getAndSend(variable, JS.JSVariableDeclarations.JSNamedVariable::getVariableType, type -> visitType(type, q));

        return variable;
    }

    @Override
    public J visitJsBinary(JS.JsBinary jsBinary, RpcSendQueue q) {
        q.getAndSend(jsBinary, JS.JsBinary::getLeft, left -> visit(left, q));
        q.getAndSend(jsBinary, b -> b.getPadding().getOperator(), op -> visitLeftPadded(op, q));
        q.getAndSend(jsBinary, JS.JsBinary::getRight, right -> visit(right, q));
        q.getAndSend(jsBinary, JS.JsBinary::getType, type -> visitType(type, q));

        return jsBinary;
    }

    @Override
    public J visitLiteralType(JS.LiteralType literalType, RpcSendQueue q) {
        q.getAndSend(literalType, JS.LiteralType::getLiteral, literal -> visit(literal, q));
        q.getAndSend(literalType, JS.LiteralType::getType, type -> visitType(type, q));

        return literalType;
    }

    @Override
    public J visitMappedType(JS.MappedType mappedType, RpcSendQueue q) {
        q.getAndSend(mappedType, m -> m.getPadding().getPrefixToken(), token -> visitLeftPadded(token, q));
        q.getAndSend(mappedType, m -> m.getPadding().getHasReadonly(), readonly -> visitLeftPadded(readonly, q));
        q.getAndSend(mappedType, JS.MappedType::getKeysRemapping, keys -> visit(keys, q));
        q.getAndSend(mappedType, m -> m.getPadding().getSuffixToken(), token -> visitLeftPadded(token, q));
        q.getAndSend(mappedType, m -> m.getPadding().getHasQuestionToken(), token -> visitLeftPadded(token, q));
        q.getAndSend(mappedType, m -> m.getPadding().getValueType(), type -> visitContainer(type, q));
        q.getAndSend(mappedType, JS.MappedType::getType, type -> visitType(type, q));

        return mappedType;
    }

    @Override
    public J visitMappedTypeKeysRemapping(JS.MappedType.KeysRemapping keysRemapping, RpcSendQueue q) {
        q.getAndSend(keysRemapping, k -> k.getPadding().getTypeParameter(), param -> visitRightPadded(param, q));
        q.getAndSend(keysRemapping, k -> k.getPadding().getNameType(), nameType -> visitRightPadded(nameType, q));

        return keysRemapping;
    }

    @Override
    public J visitMappedTypeMappedTypeParameter(JS.MappedType.MappedTypeParameter param, RpcSendQueue q) {
        q.getAndSend(param, JS.MappedType.MappedTypeParameter::getName, name -> visit(name, q));
        q.getAndSend(param, p -> p.getPadding().getIterateType(), type -> visitLeftPadded(type, q));

        return param;
    }

    @Override
    public J visitObjectBindingDeclarations(JS.ObjectBindingDeclarations objectBindings, RpcSendQueue q) {
        q.getAndSendList(objectBindings, JS.ObjectBindingDeclarations::getLeadingAnnotations, J.Annotation::getId, annot -> visit(annot, q));
        q.getAndSendList(objectBindings, JS.ObjectBindingDeclarations::getModifiers, J.Modifier::getId, mod -> visit(mod, q));
        q.getAndSend(objectBindings, JS.ObjectBindingDeclarations::getTypeExpression, expr -> visit(expr, q));
        q.getAndSend(objectBindings, o -> o.getPadding().getBindings(), bindings -> visitContainer(bindings, q));
        q.getAndSend(objectBindings, o -> o.getPadding().getInitializer(), init -> visitLeftPadded(init, q));
        q.getAndSend(objectBindings, JS.ObjectBindingDeclarations::getType, type -> visitType(type, q));

        return objectBindings;
    }

    @Override
    public J visitPropertyAssignment(JS.PropertyAssignment propAssign, RpcSendQueue q) {
        q.getAndSend(propAssign, p -> p.getPadding().getName(), name -> visitRightPadded(name, q));
        q.getAndSend(propAssign, JS.PropertyAssignment::getAssigmentToken);
        q.getAndSend(propAssign, JS.PropertyAssignment::getInitializer, init -> visit(init, q));
        q.getAndSend(propAssign, JS.PropertyAssignment::getType, type -> visitType(type, q));

        return propAssign;
    }

    @Override
    public J visitSatisfiesExpression(JS.SatisfiesExpression satisfiesExpr, RpcSendQueue q) {
        q.getAndSend(satisfiesExpr, JS.SatisfiesExpression::getExpression, expr -> visit(expr, q));
        q.getAndSend(satisfiesExpr, s -> s.getPadding().getSatisfiesType(), type -> visitLeftPadded(type, q));
        q.getAndSend(satisfiesExpr, JS.SatisfiesExpression::getType, type -> visitType(type, q));

        return satisfiesExpr;
    }

    @Override
    public J visitScopedVariableDeclarations(JS.ScopedVariableDeclarations scopedVarDecls, RpcSendQueue q) {
        q.getAndSendList(scopedVarDecls, JS.ScopedVariableDeclarations::getModifiers, J.Modifier::getId, mod -> visit(mod, q));
        q.getAndSend(scopedVarDecls, s -> s.getPadding().getScope(), scope -> visitLeftPadded(scope, q));
        q.getAndSendList(scopedVarDecls, s -> s.getPadding().getVariables(), v -> v.getElement().getId(), v -> visitRightPadded(v, q));

        return scopedVarDecls;
    }

    @Override
    public J visitStatementExpression(JS.StatementExpression stmtExpr, RpcSendQueue q) {
        q.getAndSend(stmtExpr, JS.StatementExpression::getStatement, stmt -> visit(stmt, q));

        return stmtExpr;
    }

    @Override
    public J visitTaggedTemplateExpression(JS.TaggedTemplateExpression taggedTemplate, RpcSendQueue q) {
        q.getAndSend(taggedTemplate, t -> t.getPadding().getTag(), tag -> visitRightPadded(tag, q));
        q.getAndSend(taggedTemplate, t -> t.getPadding().getTypeArguments(), args -> visitContainer(args, q));
        q.getAndSend(taggedTemplate, JS.TaggedTemplateExpression::getTemplateExpression, expr -> visit(expr, q));
        q.getAndSend(taggedTemplate, JS.TaggedTemplateExpression::getType, type -> visitType(type, q));

        return taggedTemplate;
    }

    @Override
    public J visitTemplateExpression(JS.TemplateExpression template, RpcSendQueue q) {
        q.getAndSend(template, JS.TemplateExpression::getHead, head -> visit(head, q));
        q.getAndSendList(template, t -> t.getPadding().getTemplateSpans(), span -> span.getElement().getId(), span -> visitRightPadded(span, q));
        q.getAndSend(template, JS.TemplateExpression::getType, type -> visitType(type, q));

        return template;
    }

    @Override
    public J visitTemplateExpressionTemplateSpan(JS.TemplateExpression.TemplateSpan span, RpcSendQueue q) {
        q.getAndSend(span, JS.TemplateExpression.TemplateSpan::getExpression, expr -> visit(expr, q));
        q.getAndSend(span, JS.TemplateExpression.TemplateSpan::getTail, tail -> visit(tail, q));

        return span;
    }

    @Override
    public J visitTuple(JS.Tuple tuple, RpcSendQueue q) {
        q.getAndSend(tuple, t -> t.getPadding().getElements(), elements -> visitContainer(elements, q));
        q.getAndSend(tuple, JS.Tuple::getType, type -> visitType(type, q));

        return tuple;
    }

    @Override
    public J visitTypeDeclaration(JS.TypeDeclaration typeDecl, RpcSendQueue q) {
        q.getAndSendList(typeDecl, JS.TypeDeclaration::getModifiers, J.Modifier::getId, mod -> visit(mod, q));
        q.getAndSend(typeDecl, t -> t.getPadding().getName(), name -> visitLeftPadded(name, q));
        q.getAndSend(typeDecl, JS.TypeDeclaration::getTypeParameters, params -> visit(params, q));
        q.getAndSend(typeDecl, t -> t.getPadding().getInitializer(), init -> visitLeftPadded(init, q));
        q.getAndSend(typeDecl, JS.TypeDeclaration::getType, type -> visitType(type, q));

        return typeDecl;
    }

    @Override
    public J visitTypeInfo(JS.TypeInfo typeInfo, RpcSendQueue q) {
        q.getAndSend(typeInfo, JS.TypeInfo::getTypeIdentifier, id -> visit(id, q));
        return typeInfo;
    }

    @Override
    public J visitTypeOperator(JS.TypeOperator typeOperator, RpcSendQueue q) {
        q.getAndSend(typeOperator, t -> t.getPadding().getExpression(), expr -> visitLeftPadded(expr, q));
        return typeOperator;
    }

    @Override
    public J visitTypePredicate(JS.TypePredicate typePredicate, RpcSendQueue q) {
        q.getAndSend(typePredicate, t -> t.getPadding().getAsserts(), asserts -> visitLeftPadded(asserts, q));
        q.getAndSend(typePredicate, JS.TypePredicate::getParameterName, name -> visit(name, q));
        q.getAndSend(typePredicate, t -> t.getPadding().getExpression(), expr -> visitLeftPadded(expr, q));
        return typePredicate;
    }

    @Override
    public J visitTypeQuery(JS.TypeQuery typeQuery, RpcSendQueue q) {
        q.getAndSend(typeQuery, JS.TypeQuery::getTypeExpression, expr -> visit(expr, q));
        q.getAndSend(typeQuery, tq -> tq.getPadding().getTypeArguments(), args -> visitContainer(args, q));
        q.getAndSend(typeQuery, JS.TypeQuery::getType, type -> visitType(type, q));
        return typeQuery;
    }

    @Override
    public J visitTypeTreeExpression(JS.TypeTreeExpression typeTreeExpr, RpcSendQueue q) {
        q.getAndSend(typeTreeExpr, JS.TypeTreeExpression::getExpression, expr -> visit(expr, q));
        q.getAndSend(typeTreeExpr, JS.TypeTreeExpression::getType, type -> visitType(type, q));
        return typeTreeExpr;
    }

    @Override
    public J visitUnion(JS.Union union, RpcSendQueue q) {
        q.getAndSendList(union, u -> u.getPadding().getTypes(), type -> type.getElement().getId(),
                type -> visitRightPadded(type, q));
        q.getAndSend(union, JS.Union::getType, type -> visitType(type, q));
        return union;
    }

    @Override
    public J visitJSMethodDeclaration(JS.JSMethodDeclaration methodDecl, RpcSendQueue q) {
        q.getAndSendList(methodDecl, JS.JSMethodDeclaration::getLeadingAnnotations, J.Annotation::getId, annot -> visit(annot, q));
        q.getAndSendList(methodDecl, JS.JSMethodDeclaration::getModifiers, J.Modifier::getId, mod -> visit(mod, q));
        q.getAndSend(methodDecl, JS.JSMethodDeclaration::getReturnTypeExpression, type -> visit(type, q));
        q.getAndSend(methodDecl, JS.JSMethodDeclaration::getName, name -> visit(name, q));
        q.getAndSend(methodDecl, m -> m.getPadding().getParameters(), params -> visitContainer(params, q));
        q.getAndSend(methodDecl, JS.JSMethodDeclaration::getBody, body -> visit(body, q));
        q.getAndSend(methodDecl, jsMethodDeclaration -> jsMethodDeclaration.getPadding().getDefaultValue(), def -> visitLeftPadded(def, q));
        q.getAndSend(methodDecl, JS.JSMethodDeclaration::getMethodType, type -> visitType(type, q));

        return methodDecl;
    }

    @Override
    public J visitTypeOf(JS.TypeOf typeOf, RpcSendQueue q) {
        q.getAndSend(typeOf, JS.TypeOf::getExpression, expr -> visit(expr, q));
        q.getAndSend(typeOf, JS.TypeOf::getType, type -> visitType(type, q));

        return typeOf;
    }

    @Override
    public J visitVoid(JS.Void void_, RpcSendQueue q) {
        q.getAndSend(void_, JS.Void::getExpression, expr -> visit(expr, q));
        return void_;
    }

    @Override
    public J visitWithStatement(JS.WithStatement withStmt, RpcSendQueue q) {
        q.getAndSend(withStmt, JS.WithStatement::getExpression, expr -> visit(expr, q));
        q.getAndSend(withStmt, w -> w.getPadding().getBody(), body -> visitRightPadded(body, q));

        return withStmt;
    }

    @Override
    public J visitYield(JS.Yield yield_, RpcSendQueue q) {
        q.getAndSend(yield_, y -> y.getPadding().getDelegated(), delegated -> visitLeftPadded(delegated, q));
        q.getAndSend(yield_, JS.Yield::getExpression, expr -> visit(expr, q));
        return yield_;
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
