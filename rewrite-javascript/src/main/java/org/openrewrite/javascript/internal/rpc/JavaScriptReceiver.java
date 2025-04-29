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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.java.internal.rpc.JavaReceiver;
import org.openrewrite.java.tree.*;
import org.openrewrite.javascript.JavaScriptVisitor;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.rpc.RpcReceiveQueue;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        try {
            return ((J) j.withId(q.receive(j.getId())))
                    .withPrefix(q.receive(j.getPrefix(), space -> visitSpace(space, q)))
                    .withMarkers(q.receiveMarkers(j.getMarkers()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitAlias(JS.Alias alias, RpcReceiveQueue q) {
        try {
            return alias
                    .getPadding().withPropertyName(q.receive(alias.getPadding().getPropertyName(), propName -> visitRightPadded(propName, q)))
                    .withAlias(q.receive(alias.getAlias(), aliasExpr -> (Expression) visitNonNull(aliasExpr, q)))
                    .withType(q.receive(alias.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitArrowFunction(JS.ArrowFunction arrowFunction, RpcReceiveQueue q) {
        try {
            return arrowFunction
                    .withLeadingAnnotations(q.receiveList(arrowFunction.getLeadingAnnotations(), annot -> (J.Annotation) visitNonNull(annot, q)))
                    .withModifiers(q.receiveList(arrowFunction.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                    .withTypeParameters(q.receive(arrowFunction.getTypeParameters(), params -> (J.TypeParameters) visitNonNull(params, q)))
                    .withParameters(q.receive(arrowFunction.getParameters(), params -> (J.Lambda.Parameters) visitNonNull(params, q)))
                    .withReturnTypeExpression(q.receive(arrowFunction.getReturnTypeExpression(), type -> (TypeTree) visitNonNull(type, q)))
                    .getPadding().withBody(q.receive(arrowFunction.getPadding().getBody(), body -> visitLeftPadded(body, q)))
                    .withType(q.receive(arrowFunction.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitAwait(JS.Await await, RpcReceiveQueue q) {
        try {
            return await.withExpression(q.receive(await.getExpression(), expr -> (Expression) visitNonNull(expr, q)))
                    .withType(q.receive(await.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitCompilationUnit(JS.CompilationUnit cu, RpcReceiveQueue q) {
        try {
            return cu.withSourcePath(Paths.get(q.receiveAndGet(cu.getSourcePath(), Path::toString)))
                    .withCharset(Charset.forName(q.receiveAndGet(cu.getCharset(), Charset::name)))
                    .withCharsetBomMarked(q.receive(cu.isCharsetBomMarked()))
                    .withChecksum(q.receive(cu.getChecksum()))
                    .<JS.CompilationUnit>withFileAttributes(q.receive(cu.getFileAttributes()))
                    .getPadding().withImports(q.receiveList(cu.getPadding().getImports(), imp -> visitRightPadded(imp, q)))
                    .getPadding().withStatements(q.receiveList(cu.getPadding().getStatements(), stmt -> visitRightPadded(stmt, q)))
                    .withEof(q.receive(cu.getEof(), space -> visitSpace(space, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitConditionalType(JS.ConditionalType conditionalType, RpcReceiveQueue q) {
        try {
            return conditionalType.withCheckType(q.receive(conditionalType.getCheckType(), type -> (Expression) visitNonNull(type, q)))
                    .getPadding().withCondition(q.receive(conditionalType.getPadding().getCondition(), cond -> visitContainer(cond, q)))
                    .withType(q.receive(conditionalType.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitDefaultType(JS.DefaultType defaultType, RpcReceiveQueue q) {
        try {
            return defaultType.withLeft(q.receive(defaultType.getLeft(), left -> (Expression) visitNonNull(left, q)))
                    .withBeforeEquals(q.receive(defaultType.getBeforeEquals(), space -> visitSpace(space, q)))
                    .withRight(q.receive(defaultType.getRight(), right -> (Expression) visitNonNull(right, q)))
                    .withType(q.receive(defaultType.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitDelete(JS.Delete deleteExpr, RpcReceiveQueue q) {
        try {
            return deleteExpr.withExpression(q.receive(deleteExpr.getExpression(), expr -> (Expression) visitNonNull(expr, q)))
                    .withType(q.receive(deleteExpr.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitExport(JS.Export export_, RpcReceiveQueue q) {
        try {
            return export_.getPadding().withExports(q.receive(export_.getPadding().getExports(), exports -> visitContainer(exports, q)))
                    .withFrom(q.receive(export_.getFrom(), space -> visitSpace(space, q)))
                    .withTarget(q.receive(export_.getTarget(), target -> (J.Literal) visitNonNull(target, q)))
                    .getPadding().withInitializer(q.receive(export_.getPadding().getInitializer(), init -> visitLeftPadded(init, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitExpressionStatement(JS.ExpressionStatement exprStmt, RpcReceiveQueue q) {
        try {
            return exprStmt.withExpression(q.receive(exprStmt.getExpression(), expr -> (Expression) visitNonNull(expr, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitTrailingTokenStatement(JS.TrailingTokenStatement trailStmt, RpcReceiveQueue q) {
        try {
            return trailStmt.getPadding().withExpression(q.receive(trailStmt.getPadding().getExpression(), expr -> visitRightPadded(expr, q)))
                    .withType(q.receive(trailStmt.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitExpressionWithTypeArguments(JS.ExpressionWithTypeArguments exprType, RpcReceiveQueue q) {
        try {
            return exprType.withClazz(q.receive(exprType.getClazz(), clazz -> visitNonNull(clazz, q)))
                    .getPadding().withTypeArguments(q.receive(exprType.getPadding().getTypeArguments(), args -> visitContainer(args, q)))
                    .withType(q.receive(exprType.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitFunctionType(JS.FunctionType functionType, RpcReceiveQueue q) {
        try {
            return functionType.withModifiers(q.receiveList(functionType.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                    .getPadding().withConstructorType(q.receive(functionType.getPadding().getConstructorType(), ct -> visitLeftPadded(ct, q)))
                    .withTypeParameters(q.receive(functionType.getTypeParameters(), params -> (J.TypeParameters) visitNonNull(params, q)))
                    .getPadding().withParameters(q.receive(functionType.getPadding().getParameters(), params -> visitContainer(params, q)))
                    .getPadding().withReturnType(q.receive(functionType.getPadding().getReturnType(), returnType -> visitLeftPadded(returnType, q)))
                    .withType(q.receive(functionType.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitImportAttribute(JS.ImportAttribute importAttr, RpcReceiveQueue q) {
        try {
            return importAttr.withName(q.receive(importAttr.getName(), name -> (Expression) visitNonNull(name, q)))
                    .getPadding().withValue(q.receive(importAttr.getPadding().getValue(), value -> visitLeftPadded(value, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitImportAttributes(JS.ImportAttributes importAttrs, RpcReceiveQueue q) {
        try {
            return importAttrs.withToken(q.receive(importAttrs.getToken()))
                    .getPadding().withElements(q.receive(importAttrs.getPadding().getElements(), elements -> visitContainer(elements, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitImportType(JS.ImportType importType, RpcReceiveQueue q) {
        try {
            return importType.getPadding().withHasTypeof(q.receive(importType.getPadding().getHasTypeof(), hasTypeof -> visitRightPadded(hasTypeof, q)))
                    .getPadding().withArgumentAndAttributes(q.receive(importType.getPadding().getArgumentAndAttributes(), args -> visitContainer(args, q)))
                    .getPadding().withQualifier(q.receive(importType.getPadding().getQualifier(), qualifier -> visitLeftPadded(qualifier, q)))
                    .getPadding().withTypeArguments(q.receive(importType.getPadding().getTypeArguments(), args -> visitContainer(args, q)))
                    .withType(q.receive(importType.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitImportTypeAttributes(JS.ImportTypeAttributes importTypeAttrs, RpcReceiveQueue q) {
        try {
            return importTypeAttrs.getPadding().withToken(q.receive(importTypeAttrs.getPadding().getToken(), token -> visitRightPadded(token, q)))
                    .getPadding().withElements(q.receive(importTypeAttrs.getPadding().getElements(), elements -> visitContainer(elements, q)))
                    .withEnd(q.receive(importTypeAttrs.getEnd(), space -> visitSpace(space, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitIndexedAccessType(JS.IndexedAccessType indexedAccessType, RpcReceiveQueue q) {
        return indexedAccessType
                .withObjectType(q.receive(indexedAccessType.getObjectType(), expr -> (TypeTree) visitNonNull(expr, q)))
                .withIndexType(q.receive(indexedAccessType.getIndexType(), indexType -> (TypeTree) visitNonNull(indexType, q)))
                .withType(q.receive(indexedAccessType.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitIndexedAccessTypeIndexType(JS.IndexedAccessType.IndexType indexType, RpcReceiveQueue q) {
        return indexType
                .getPadding().withElement(q.receive(indexType.getPadding().getElement(), elem -> visitRightPadded(elem, q)))
                .withType(q.receive(indexType.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitIndexSignatureDeclaration(JS.IndexSignatureDeclaration signatureDeclaration, RpcReceiveQueue q) {
        return signatureDeclaration
                .withModifiers(q.receiveList(signatureDeclaration.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                .getPadding().withParameters(q.receive(signatureDeclaration.getPadding().getParameters(), param -> visitContainer(param, q)))
                .getPadding().withTypeExpression(q.receive(signatureDeclaration.getPadding().getTypeExpression(), expr -> visitLeftPadded(expr, q)))
                .withType(q.receive(signatureDeclaration.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitInferType(JS.InferType inferType, RpcReceiveQueue q) {
        try {
            return inferType.getPadding().withTypeParameter(q.receive(inferType.getPadding().getTypeParameter(), param -> visitLeftPadded(param, q)))
                    .withType(q.receive(inferType.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitIntersection(JS.Intersection intersection, RpcReceiveQueue q) {
        return intersection
                .getPadding().withTypes(q.receiveList(intersection.getPadding().getTypes(), t -> visitRightPadded(t, q)))
                .withType(q.receive(intersection.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitJsAssignmentOperation(JS.JsAssignmentOperation assignOp, RpcReceiveQueue q) {
        return assignOp
                .withVariable(q.receive(assignOp.getVariable(), var -> (Expression) visitNonNull(var, q)))
                .getPadding().withOperator(q.receive(assignOp.getPadding().getOperator(), op -> visitLeftPadded(op, q)))
                .withAssignment(q.receive(assignOp.getAssignment(), assign -> (Expression) visitNonNull(assign, q)))
                .withType(q.receive(assignOp.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitJsImport(JS.JsImport jsImport, RpcReceiveQueue q) {
        try {
            return jsImport.withModifiers(q.receiveList(jsImport.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                    .withImportClause(q.receive(jsImport.getImportClause(), clause -> (JS.JsImportClause) visitNonNull(clause, q)))
                    .getPadding().withModuleSpecifier(q.receive(jsImport.getPadding().getModuleSpecifier(), spec -> visitLeftPadded(spec, q)))
                    .withAttributes(q.receive(jsImport.getAttributes(), attrs -> (JS.ImportAttributes) visitNonNull(attrs, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitJsImportClause(JS.JsImportClause jsImportClause, RpcReceiveQueue q) {
        try {
            return jsImportClause.withTypeOnly(q.receive(jsImportClause.isTypeOnly()))
                    .getPadding().withName(q.receive(jsImportClause.getPadding().getName(), name -> visitRightPadded(name, q)))
                    .withNamedBindings(q.receive(jsImportClause.getNamedBindings(), bindings -> (Expression) visitNonNull(bindings, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitNamedImports(JS.NamedImports namedImports, RpcReceiveQueue q) {
        try {
            return namedImports.getPadding().withElements(q.receive(namedImports.getPadding().getElements(), elements -> visitContainer(elements, q)))
                    .withType(q.receive(namedImports.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitJsImportSpecifier(JS.JsImportSpecifier jsImportSpecifier, RpcReceiveQueue q) {
        try {
            return jsImportSpecifier.getPadding().withImportType(q.receive(jsImportSpecifier.getPadding().getImportType(), importType -> visitLeftPadded(importType, q)))
                    .withSpecifier(q.receive(jsImportSpecifier.getSpecifier(), specifier -> (Expression) visitNonNull(specifier, q)))
                    .withType(q.receive(jsImportSpecifier.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitJsBinary(JS.JsBinary jsBinary, RpcReceiveQueue q) {
        try {
            return jsBinary.withLeft(q.receive(jsBinary.getLeft(), left -> (Expression) visitNonNull(left, q)))
                    .getPadding().withOperator(q.receive(jsBinary.getPadding().getOperator(), op -> visitLeftPadded(op, q)))
                    .withRight(q.receive(jsBinary.getRight(), right -> (Expression) visitNonNull(right, q)))
                    .withType(q.receive(jsBinary.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitLiteralType(JS.LiteralType literalType, RpcReceiveQueue q) {
        try {
            return literalType.withLiteral(q.receive(literalType.getLiteral(), literal -> (Expression) visitNonNull(literal, q)))
                    .withType(q.receive(literalType.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitMappedType(JS.MappedType mappedType, RpcReceiveQueue q) {
        try {
            return mappedType.getPadding().withPrefixToken(q.receive(mappedType.getPadding().getPrefixToken(), token -> visitLeftPadded(token, q)))
                    .getPadding().withHasReadonly(q.receive(mappedType.getPadding().getHasReadonly(), readonly -> visitLeftPadded(readonly, q)))
                    .withKeysRemapping(q.receive(mappedType.getKeysRemapping(), keys -> (JS.MappedType.KeysRemapping) visitNonNull(keys, q)))
                    .getPadding().withSuffixToken(q.receive(mappedType.getPadding().getSuffixToken(), token -> visitLeftPadded(token, q)))
                    .getPadding().withHasQuestionToken(q.receive(mappedType.getPadding().getHasQuestionToken(), token -> visitLeftPadded(token, q)))
                    .getPadding().withValueType(q.receive(mappedType.getPadding().getValueType(), type -> visitContainer(type, q)))
                    .withType(q.receive(mappedType.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitMappedTypeKeysRemapping(JS.MappedType.KeysRemapping keysRemapping, RpcReceiveQueue q) {
        try {
            return keysRemapping.getPadding().withTypeParameter(q.receive(keysRemapping.getPadding().getTypeParameter(), param -> visitRightPadded(param, q)))
                    .getPadding().withNameType(q.receive(keysRemapping.getPadding().getNameType(), nameType -> visitRightPadded(nameType, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitMappedTypeMappedTypeParameter(JS.MappedType.MappedTypeParameter param, RpcReceiveQueue q) {
        try {
            return param.withName(q.receive(param.getName(), name -> (Expression) visitNonNull(name, q)))
                    .getPadding().withIterateType(q.receive(param.getPadding().getIterateType(), type -> visitLeftPadded(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitObjectBindingDeclarations(JS.ObjectBindingDeclarations objectBindings, RpcReceiveQueue q) {
        try {
            return objectBindings.withLeadingAnnotations(q.receiveList(objectBindings.getLeadingAnnotations(), annot -> (J.Annotation) visitNonNull(annot, q)))
                    .withModifiers(q.receiveList(objectBindings.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                    .withTypeExpression(q.receive(objectBindings.getTypeExpression(), expr -> (TypeTree) visitNonNull(expr, q)))
                    .getPadding().withBindings(q.receive(objectBindings.getPadding().getBindings(), bindings -> visitContainer(bindings, q)))
                    .getPadding().withInitializer(q.receive(objectBindings.getPadding().getInitializer(), init -> visitLeftPadded(init, q)))
                    .withType(q.receive(objectBindings.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitPropertyAssignment(JS.PropertyAssignment propAssign, RpcReceiveQueue q) {
        try {
            return propAssign.getPadding().withName(q.receive(propAssign.getPadding().getName(), name -> visitRightPadded(name, q)))
                    .withAssigmentToken(q.receive(propAssign.getAssigmentToken()))
                    .withInitializer(q.receive(propAssign.getInitializer(), init -> (Expression) visitNonNull(init, q)))
                    .withType(q.receive(propAssign.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitSatisfiesExpression(JS.SatisfiesExpression satisfiesExpr, RpcReceiveQueue q) {
        try {
            return satisfiesExpr.withExpression(q.receive(satisfiesExpr.getExpression(), expr -> visitNonNull(expr, q)))
                    .getPadding().withSatisfiesType(q.receive(satisfiesExpr.getPadding().getSatisfiesType(), type -> visitLeftPadded(type, q)))
                    .withType(q.receive(satisfiesExpr.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitScopedVariableDeclarations(JS.ScopedVariableDeclarations scopedVarDecls, RpcReceiveQueue q) {
        try {
            return scopedVarDecls.withModifiers(q.receiveList(scopedVarDecls.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                    .getPadding().withScope(q.receive(scopedVarDecls.getPadding().getScope(), scope -> visitLeftPadded(scope, q)))
                    .getPadding().withVariables(q.receiveList(scopedVarDecls.getPadding().getVariables(), v -> visitRightPadded(v, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitStatementExpression(JS.StatementExpression stmtExpr, RpcReceiveQueue q) {
        try {
            return stmtExpr.withStatement(q.receive(stmtExpr.getStatement(), stmt -> (Statement) visitNonNull(stmt, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitTaggedTemplateExpression(JS.TaggedTemplateExpression taggedTemplate, RpcReceiveQueue q) {
        try {
            return taggedTemplate.getPadding().withTag(q.receive(taggedTemplate.getPadding().getTag(), tag -> visitRightPadded(tag, q)))
                    .getPadding().withTypeArguments(q.receive(taggedTemplate.getPadding().getTypeArguments(), args -> visitContainer(args, q)))
                    .withTemplateExpression(q.receive(taggedTemplate.getTemplateExpression(), expr -> (Expression) visitNonNull(expr, q)))
                    .withType(q.receive(taggedTemplate.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitTemplateExpression(JS.TemplateExpression template, RpcReceiveQueue q) {
        try {
            return template.withHead(q.receive(template.getHead(), head -> (J.Literal) visitNonNull(head, q)))
                    .getPadding().withTemplateSpans(q.receiveList(template.getPadding().getTemplateSpans(), span -> visitRightPadded(span, q)))
                    .withType(q.receive(template.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitTemplateExpressionTemplateSpan(JS.TemplateExpression.TemplateSpan span, RpcReceiveQueue q) {
        try {
            return span.withExpression(q.receive(span.getExpression(), expr -> visitNonNull(expr, q)))
                    .withTail(q.receive(span.getTail(), tail -> (J.Literal) visitNonNull(tail, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitTuple(JS.Tuple tuple, RpcReceiveQueue q) {
        try {
            return tuple.getPadding().withElements(q.receive(tuple.getPadding().getElements(), elements -> visitContainer(elements, q)))
                    .withType(q.receive(tuple.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitTypeDeclaration(JS.TypeDeclaration typeDecl, RpcReceiveQueue q) {
        try {
            return typeDecl.withModifiers(q.receiveList(typeDecl.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                    .getPadding().withName(q.receive(typeDecl.getPadding().getName(), name -> visitLeftPadded(name, q)))
                    .withTypeParameters(q.receive(typeDecl.getTypeParameters(), params -> (J.TypeParameters) visitNonNull(params, q)))
                    .getPadding().withInitializer(q.receive(typeDecl.getPadding().getInitializer(), init -> visitLeftPadded(init, q)))
                    .withType(q.receive(typeDecl.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitTypeInfo(JS.TypeInfo typeInfo, RpcReceiveQueue q) {
        return typeInfo
                .withTypeIdentifier(q.receive(typeInfo.getTypeIdentifier(), id -> (TypeTree) visitNonNull(id, q)));
    }

    @Override
    public J visitJSMethodDeclaration(JS.JSMethodDeclaration methodDecl, RpcReceiveQueue q) {
        return methodDecl.withLeadingAnnotations(q.receiveList(methodDecl.getLeadingAnnotations(), annot -> (J.Annotation) visitNonNull(annot, q)))
                .withModifiers(q.receiveList(methodDecl.getModifiers(), mod -> (J.Modifier) visitNonNull(mod, q)))
                .withTypeParameters(q.receive(methodDecl.getTypeParameters(), type -> (J.TypeParameters) visitNonNull(type, q)))
                .withReturnTypeExpression(q.receive(methodDecl.getReturnTypeExpression(), type -> (TypeTree) visitNonNull(type, q)))
                .withName(q.receive(methodDecl.getName(), name -> (Expression) visitNonNull(name, q)))
                .getPadding().withParameters(q.receive(methodDecl.getPadding().getParameters(), params -> visitContainer(params, q)))
                .withBody(q.receive(methodDecl.getBody(), body -> (J.Block) visitNonNull(body, q)))
                .getPadding().withDefaultValue(q.receive(methodDecl.getPadding().getDefaultValue(), def -> visitLeftPadded(def, q)))
                .withType(q.receive(methodDecl.getMethodType(), type -> (JavaType.Method) visitType(type, q)));
    }

    @Override
    public J visitTypeOf(JS.TypeOf typeOf, RpcReceiveQueue q) {
        try {
            return typeOf.withExpression(q.receive(typeOf.getExpression(), expr -> (Expression) visitNonNull(expr, q)))
                    .withType(q.receive(typeOf.getType(), type -> visitType(type, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitTypeOperator(JS.TypeOperator typeOperator, RpcReceiveQueue q) {
        return typeOperator
                .getPadding().withExpression(q.receive(typeOperator.getPadding().getExpression(), expr -> visitLeftPadded(expr, q)));
    }

    @Override
    public J visitTypePredicate(JS.TypePredicate typePredicate, RpcReceiveQueue q) {
        return typePredicate
                .getPadding().withAsserts(q.receive(typePredicate.getPadding().getAsserts(), asserts -> visitLeftPadded(asserts, q)))
                .withParameterName(q.receive(typePredicate.getParameterName(), name -> (J.Identifier) visitNonNull(name, q)))
                .getPadding().withExpression(q.receive(typePredicate.getPadding().getExpression(), expr -> visitLeftPadded(expr, q)));
    }

    @Override
    public J visitTypeQuery(JS.TypeQuery typeQuery, RpcReceiveQueue q) {
        return typeQuery
                .withTypeExpression(q.receive(typeQuery.getTypeExpression(), expr -> (TypeTree) visitNonNull(expr, q)))
                .getPadding().withTypeArguments(q.receive(typeQuery.getPadding().getTypeArguments(), args -> visitContainer(args, q)))
                .withType(q.receive(typeQuery.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitTypeTreeExpression(JS.TypeTreeExpression typeTreeExpr, RpcReceiveQueue q) {
        return typeTreeExpr
                .withExpression(q.receive(typeTreeExpr.getExpression(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(typeTreeExpr.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitUnary(JS.Unary unary, RpcReceiveQueue q) {
        return unary
                .getPadding().withOperator(q.receive(unary.getPadding().getOperator(), op -> visitLeftPadded(op, q)))
                .withExpression(q.receive(unary.getExpression(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(unary.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitUnion(JS.Union union, RpcReceiveQueue q) {
        return union
                .getPadding().withTypes(q.receiveList(union.getPadding().getTypes(), t -> visitRightPadded(t, q)))
                .withType(q.receive(union.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitVoid(JS.Void void_, RpcReceiveQueue q) {
        return void_
                .withExpression(q.receive(void_.getExpression(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public J visitWithStatement(JS.WithStatement withStmt, RpcReceiveQueue q) {
        try {
            return withStmt.withExpression(q.receive(withStmt.getExpression(), expr -> (J.ControlParentheses<@NonNull Expression>) visitNonNull(expr, q)))
                    .getPadding().withBody(q.receive(withStmt.getPadding().getBody(), body -> visitRightPadded(body, q)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J visitYield(JS.Yield yield_, RpcReceiveQueue q) {
        return yield_
                .getPadding().withDelegated(q.receive(yield_.getPadding().getDelegated(), delegated -> visitLeftPadded(delegated, q)))
                .withExpression(q.receive(yield_.getExpression(), expr -> (Expression) visitNonNull(expr, q)));
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
