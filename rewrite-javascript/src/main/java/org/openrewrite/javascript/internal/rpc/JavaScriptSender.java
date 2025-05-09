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
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.internal.rpc.JavaSender;
import org.openrewrite.java.tree.*;
import org.openrewrite.javascript.JavaScriptVisitor;
import org.openrewrite.javascript.tree.JS;
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
    public J visitAlias(JS.Alias alias, RpcSendQueue q) {
        q.getAndSend(alias, el -> el.getPadding().getPropertyName(), el -> visitRightPadded(el, q));
        q.getAndSend(alias, JS.Alias::getAlias, el -> visit(el, q));
        return alias;
    }

    @Override
    public J visitArrowFunction(JS.ArrowFunction arrowFunction, RpcSendQueue q) {
        q.getAndSendList(arrowFunction, JS.ArrowFunction::getLeadingAnnotations, J.Annotation::getId, el -> visit(el, q));
        q.getAndSendList(arrowFunction, JS.ArrowFunction::getModifiers, J.Modifier::getId, el -> visit(el, q));
        if (arrowFunction.getTypeParameters() != null) {
            q.getAndSend(arrowFunction, JS.ArrowFunction::getTypeParameters, el -> visit(el, q));
        }
        q.getAndSend(arrowFunction, JS.ArrowFunction::getParameters, el -> visit(el, q));
        if (arrowFunction.getReturnTypeExpression() != null) {
            q.getAndSend(arrowFunction, JS.ArrowFunction::getReturnTypeExpression, el -> visit(el, q));
        }
        q.getAndSend(arrowFunction, el -> el.getPadding().getBody(), el -> visitLeftPadded(el, q));
        if (arrowFunction.getType() != null) {
            q.getAndSend(arrowFunction, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return arrowFunction;
    }

    @Override
    public J visitAwait(JS.Await await, RpcSendQueue q) {
        q.getAndSend(await, JS.Await::getExpression, el -> visit(el, q));
        if (await.getType() != null) {
            q.getAndSend(await, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return await;
    }

    @Override
    public J visitConditionalType(JS.ConditionalType conditionalType, RpcSendQueue q) {
        q.getAndSend(conditionalType, JS.ConditionalType::getCheckType, el -> visit(el, q));
        q.getAndSend(conditionalType, el -> el.getPadding().getCondition(), el -> visitContainer(el, q));
        if (conditionalType.getType() != null) {
            q.getAndSend(conditionalType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return conditionalType;
    }

    @Override
    public J visitDefaultType(JS.DefaultType defaultType, RpcSendQueue q) {
        q.getAndSend(defaultType, JS.DefaultType::getLeft, el -> visit(el, q));
        q.getAndSend(defaultType, el -> asRef(el.getBeforeEquals()), el -> visitSpace(getValueNonNull(el), q));
        q.getAndSend(defaultType, JS.DefaultType::getRight, el -> visit(el, q));
        if (defaultType.getType() != null) {
            q.getAndSend(defaultType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return defaultType;
    }

    @Override
    public J visitDelete(JS.Delete delete, RpcSendQueue q) {
        q.getAndSend(delete, JS.Delete::getExpression, el -> visit(el, q));
        if (delete.getType() != null) {
            q.getAndSend(delete, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return delete;
    }

    @Override
    public J visitExport(JS.Export export, RpcSendQueue q) {
        if (export.getPadding().getExports() != null) {
            q.getAndSend(export, el -> el.getPadding().getExports(), el -> visitContainer(el, q));
        }
        if (export.getFrom() != null) {
            q.getAndSend(export, el -> asRef(el.getFrom()), el -> visitSpace(getValueNonNull(el), q));
        }
        if (export.getTarget() != null) {
            q.getAndSend(export, JS.Export::getTarget, el -> visit(el, q));
        }
        if (export.getPadding().getInitializer() != null) {
            q.getAndSend(export, el -> el.getPadding().getInitializer(), el -> visitLeftPadded(el, q));
        }
        return export;
    }

    @Override
    public J visitExpressionStatement(JS.ExpressionStatement expressionStatement, RpcSendQueue q) {
        q.getAndSend(expressionStatement, JS.ExpressionStatement::getExpression, el -> visit(el, q));
        return expressionStatement;
    }

    @Override
    public J visitExpressionWithTypeArguments(JS.ExpressionWithTypeArguments expressionWithTypeArguments, RpcSendQueue q) {
        q.getAndSend(expressionWithTypeArguments, JS.ExpressionWithTypeArguments::getClazz, el -> visit(el, q));
        if (expressionWithTypeArguments.getPadding().getTypeArguments() != null) {
            q.getAndSend(expressionWithTypeArguments, el -> el.getPadding().getTypeArguments(), el -> visitContainer(el, q));
        }
        if (expressionWithTypeArguments.getType() != null) {
            q.getAndSend(expressionWithTypeArguments, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return expressionWithTypeArguments;
    }

    @Override
    public J visitFunctionType(JS.FunctionType functionType, RpcSendQueue q) {
        q.getAndSendList(functionType, JS.FunctionType::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSend(functionType, el -> el.getPadding().getConstructorType(), el -> visitLeftPadded(el, q));
        if (functionType.getTypeParameters() != null) {
            q.getAndSend(functionType, JS.FunctionType::getTypeParameters, el -> visit(el, q));
        }
        q.getAndSend(functionType, el -> el.getPadding().getParameters(), el -> visitContainer(el, q));
        q.getAndSend(functionType, el -> el.getPadding().getReturnType(), el -> visitLeftPadded(el, q));
        if (functionType.getType() != null) {
            q.getAndSend(functionType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return functionType;
    }

    @Override
    public J visitInferType(JS.InferType inferType, RpcSendQueue q) {
        q.getAndSend(inferType, el -> el.getPadding().getTypeParameter(), el -> visitLeftPadded(el, q));
        if (inferType.getType() != null) {
            q.getAndSend(inferType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return inferType;
    }

    @Override
    public J visitImportType(JS.ImportType importType, RpcSendQueue q) {
        q.getAndSend(importType, el -> el.getPadding().getHasTypeof(), el -> visitRightPadded(el, q));
        q.getAndSend(importType, el -> el.getPadding().getArgumentAndAttributes(), el -> visitContainer(el, q));
        if (importType.getPadding().getQualifier() != null) {
            q.getAndSend(importType, el -> el.getPadding().getQualifier(), el -> visitLeftPadded(el, q));
        }
        if (importType.getPadding().getTypeArguments() != null) {
            q.getAndSend(importType, el -> el.getPadding().getTypeArguments(), el -> visitContainer(el, q));
        }
        if (importType.getType() != null) {
            q.getAndSend(importType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return importType;
    }

    @Override
    public J visitJsImport(JS.JsImport jsImport, RpcSendQueue q) {
        q.getAndSendList(jsImport, JS.JsImport::getModifiers, J.Modifier::getId, el -> visit(el, q));
        if (jsImport.getImportClause() != null) {
            q.getAndSend(jsImport, JS.JsImport::getImportClause, el -> visit(el, q));
        }
        q.getAndSend(jsImport, el -> el.getPadding().getModuleSpecifier(), el -> visitLeftPadded(el, q));
        if (jsImport.getAttributes() != null) {
            q.getAndSend(jsImport, JS.JsImport::getAttributes, el -> visit(el, q));
        }
        return jsImport;
    }

    @Override
    public J visitJsImportClause(JS.JsImportClause jsImportClause, RpcSendQueue q) {
        q.getAndSend(jsImportClause, JS.JsImportClause::isTypeOnly);
        if (jsImportClause.getPadding().getName() != null) {
            q.getAndSend(jsImportClause, el -> el.getPadding().getName(), el -> visitRightPadded(el, q));
        }
        if (jsImportClause.getNamedBindings() != null) {
            q.getAndSend(jsImportClause, JS.JsImportClause::getNamedBindings, el -> visit(el, q));
        }
        return jsImportClause;
    }

    @Override
    public J visitNamedImports(JS.NamedImports namedImports, RpcSendQueue q) {
        q.getAndSend(namedImports, el -> el.getPadding().getElements(), el -> visitContainer(el, q));
        if (namedImports.getType() != null) {
            q.getAndSend(namedImports, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return namedImports;
    }

    @Override
    public J visitJsImportSpecifier(JS.JsImportSpecifier jsImportSpecifier, RpcSendQueue q) {
        q.getAndSend(jsImportSpecifier, el -> el.getPadding().getImportType(), el -> visitLeftPadded(el, q));
        q.getAndSend(jsImportSpecifier, JS.JsImportSpecifier::getSpecifier, el -> visit(el, q));
        if (jsImportSpecifier.getType() != null) {
            q.getAndSend(jsImportSpecifier, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return jsImportSpecifier;
    }

    @Override
    public J visitJSVariableDeclarations(JS.JSVariableDeclarations jSVariableDeclarations, RpcSendQueue q) {
        q.getAndSendList(jSVariableDeclarations, JS.JSVariableDeclarations::getLeadingAnnotations, J.Annotation::getId, el -> visit(el, q));
        q.getAndSendList(jSVariableDeclarations, JS.JSVariableDeclarations::getModifiers, J.Modifier::getId, el -> visit(el, q));
        if (jSVariableDeclarations.getTypeExpression() != null) {
            q.getAndSend(jSVariableDeclarations, JS.JSVariableDeclarations::getTypeExpression, el -> visit(el, q));
        }
        if (jSVariableDeclarations.getVarargs() != null) {
            q.getAndSend(jSVariableDeclarations, el -> asRef(el.getVarargs()), el -> visitSpace(getValueNonNull(el), q));
        }
        q.getAndSendList(jSVariableDeclarations, el -> el.getPadding().getVariables(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        return jSVariableDeclarations;
    }

    @Override
    public J visitJSVariableDeclarationsJSNamedVariable(JS.JSVariableDeclarations.JSNamedVariable jSNamedVariable, RpcSendQueue q) {
        q.getAndSend(jSNamedVariable, JS.JSVariableDeclarations.JSNamedVariable::getName, el -> visit(el, q));
        q.getAndSendList(jSNamedVariable, JS.JSVariableDeclarations.JSNamedVariable::getDimensionsAfterName, el -> el.getElement().getWhitespace() + el.getElement().getComments(), el -> visitLeftPadded(el, q));
        if (jSNamedVariable.getPadding().getInitializer() != null) {
            q.getAndSend(jSNamedVariable, el -> el.getPadding().getInitializer(), el -> visitLeftPadded(el, q));
        }
        if (jSNamedVariable.getVariableType() != null) {
            q.getAndSend(jSNamedVariable, el -> asRef(el.getVariableType()), el -> visitType(getValueNonNull(el), q));
        }
        return jSNamedVariable;
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
        q.getAndSend(importTypeAttributes, el -> asRef(el.getEnd()), el -> visitSpace(getValueNonNull(el), q));
        return importTypeAttributes;
    }

    @Override
    public J visitImportAttribute(JS.ImportAttribute importAttribute, RpcSendQueue q) {
        q.getAndSend(importAttribute, JS.ImportAttribute::getName, el -> visit(el, q));
        q.getAndSend(importAttribute, el -> el.getPadding().getValue(), el -> visitLeftPadded(el, q));
        return importAttribute;
    }

    @Override
    public J visitJsBinary(JS.JsBinary jsBinary, RpcSendQueue q) {
        q.getAndSend(jsBinary, JS.JsBinary::getLeft, el -> visit(el, q));
        q.getAndSend(jsBinary, el -> el.getPadding().getOperator(), el -> visitLeftPadded(el, q));
        q.getAndSend(jsBinary, JS.JsBinary::getRight, el -> visit(el, q));
        if (jsBinary.getType() != null) {
            q.getAndSend(jsBinary, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return jsBinary;
    }

    @Override
    public J visitLiteralType(JS.LiteralType literalType, RpcSendQueue q) {
        q.getAndSend(literalType, JS.LiteralType::getLiteral, el -> visit(el, q));
        q.getAndSend(literalType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return literalType;
    }

    @Override
    public J visitMappedType(JS.MappedType mappedType, RpcSendQueue q) {
        if (mappedType.getPadding().getPrefixToken() != null) {
            q.getAndSend(mappedType, el -> el.getPadding().getPrefixToken(), el -> visitLeftPadded(el, q));
        }
        q.getAndSend(mappedType, el -> el.getPadding().getHasReadonly(), el -> visitLeftPadded(el, q));
        q.getAndSend(mappedType, JS.MappedType::getKeysRemapping, el -> visit(el, q));
        if (mappedType.getPadding().getSuffixToken() != null) {
            q.getAndSend(mappedType, el -> el.getPadding().getSuffixToken(), el -> visitLeftPadded(el, q));
        }
        q.getAndSend(mappedType, el -> el.getPadding().getHasQuestionToken(), el -> visitLeftPadded(el, q));
        q.getAndSend(mappedType, el -> el.getPadding().getValueType(), el -> visitContainer(el, q));
        if (mappedType.getType() != null) {
            q.getAndSend(mappedType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return mappedType;
    }

    @Override
    public J visitMappedTypeKeysRemapping(JS.MappedType.KeysRemapping keysRemapping, RpcSendQueue q) {
        q.getAndSend(keysRemapping, el -> el.getPadding().getTypeParameter(), el -> visitRightPadded(el, q));
        if (keysRemapping.getPadding().getNameType() != null) {
            q.getAndSend(keysRemapping, el -> el.getPadding().getNameType(), el -> visitRightPadded(el, q));
        }
        return keysRemapping;
    }

    @Override
    public J visitMappedTypeMappedTypeParameter(JS.MappedType.MappedTypeParameter mappedTypeParameter, RpcSendQueue q) {
        q.getAndSend(mappedTypeParameter, JS.MappedType.MappedTypeParameter::getName, el -> visit(el, q));
        q.getAndSend(mappedTypeParameter, el -> el.getPadding().getIterateType(), el -> visitLeftPadded(el, q));
        return mappedTypeParameter;
    }

    @Override
    public J visitObjectBindingDeclarations(JS.ObjectBindingDeclarations objectBindingDeclarations, RpcSendQueue q) {
        q.getAndSendList(objectBindingDeclarations, JS.ObjectBindingDeclarations::getLeadingAnnotations, J.Annotation::getId, el -> visit(el, q));
        q.getAndSendList(objectBindingDeclarations, JS.ObjectBindingDeclarations::getModifiers, J.Modifier::getId, el -> visit(el, q));
        if (objectBindingDeclarations.getTypeExpression() != null) {
            q.getAndSend(objectBindingDeclarations, JS.ObjectBindingDeclarations::getTypeExpression, el -> visit(el, q));
        }
        q.getAndSend(objectBindingDeclarations, el -> el.getPadding().getBindings(), el -> visitContainer(el, q));
        if (objectBindingDeclarations.getPadding().getInitializer() != null) {
            q.getAndSend(objectBindingDeclarations, el -> el.getPadding().getInitializer(), el -> visitLeftPadded(el, q));
        }
        return objectBindingDeclarations;
    }

    @Override
    public J visitPropertyAssignment(JS.PropertyAssignment propertyAssignment, RpcSendQueue q) {
        q.getAndSend(propertyAssignment, el -> el.getPadding().getName(), el -> visitRightPadded(el, q));
        q.getAndSend(propertyAssignment, JS.PropertyAssignment::getAssigmentToken);
        if (propertyAssignment.getInitializer() != null) {
            q.getAndSend(propertyAssignment, JS.PropertyAssignment::getInitializer, el -> visit(el, q));
        }
        return propertyAssignment;
    }

    @Override
    public J visitSatisfiesExpression(JS.SatisfiesExpression satisfiesExpression, RpcSendQueue q) {
        q.getAndSend(satisfiesExpression, JS.SatisfiesExpression::getExpression, el -> visit(el, q));
        q.getAndSend(satisfiesExpression, el -> el.getPadding().getSatisfiesType(), el -> visitLeftPadded(el, q));
        if (satisfiesExpression.getType() != null) {
            q.getAndSend(satisfiesExpression, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return satisfiesExpression;
    }

    @Override
    public J visitScopedVariableDeclarations(JS.ScopedVariableDeclarations scopedVariableDeclarations, RpcSendQueue q) {
        q.getAndSendList(scopedVariableDeclarations, JS.ScopedVariableDeclarations::getModifiers, J.Modifier::getId, el -> visit(el, q));
        if (scopedVariableDeclarations.getPadding().getScope() != null) {
            q.getAndSend(scopedVariableDeclarations, el -> el.getPadding().getScope(), el -> visitLeftPadded(el, q));
        }
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
        if (taggedTemplateExpression.getPadding().getTag() != null) {
            q.getAndSend(taggedTemplateExpression, el -> el.getPadding().getTag(), el -> visitRightPadded(el, q));
        }
        if (taggedTemplateExpression.getPadding().getTypeArguments() != null) {
            q.getAndSend(taggedTemplateExpression, el -> el.getPadding().getTypeArguments(), el -> visitContainer(el, q));
        }
        q.getAndSend(taggedTemplateExpression, JS.TaggedTemplateExpression::getTemplateExpression, el -> visit(el, q));
        if (taggedTemplateExpression.getType() != null) {
            q.getAndSend(taggedTemplateExpression, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return taggedTemplateExpression;
    }

    @Override
    public J visitTemplateExpression(JS.TemplateExpression templateExpression, RpcSendQueue q) {
        q.getAndSend(templateExpression, JS.TemplateExpression::getHead, el -> visit(el, q));
        q.getAndSendList(templateExpression, el -> el.getPadding().getTemplateSpans(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        if (templateExpression.getType() != null) {
            q.getAndSend(templateExpression, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return templateExpression;
    }

    @Override
    public J visitTemplateExpressionTemplateSpan(JS.TemplateExpression.TemplateSpan templateSpan, RpcSendQueue q) {
        q.getAndSend(templateSpan, JS.TemplateExpression.TemplateSpan::getExpression, el -> visit(el, q));
        q.getAndSend(templateSpan, JS.TemplateExpression.TemplateSpan::getTail, el -> visit(el, q));
        return templateSpan;
    }

    @Override
    public J visitTrailingTokenStatement(JS.TrailingTokenStatement trailingTokenStatement, RpcSendQueue q) {
        q.getAndSend(trailingTokenStatement, el -> el.getPadding().getExpression(), el -> visitRightPadded(el, q));
        if (trailingTokenStatement.getType() != null) {
            q.getAndSend(trailingTokenStatement, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return trailingTokenStatement;
    }

    @Override
    public J visitTuple(JS.Tuple tuple, RpcSendQueue q) {
        q.getAndSend(tuple, el -> el.getPadding().getElements(), el -> visitContainer(el, q));
        if (tuple.getType() != null) {
            q.getAndSend(tuple, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return tuple;
    }

    @Override
    public J visitTypeDeclaration(JS.TypeDeclaration typeDeclaration, RpcSendQueue q) {
        q.getAndSendList(typeDeclaration, JS.TypeDeclaration::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSend(typeDeclaration, el -> el.getPadding().getName(), el -> visitLeftPadded(el, q));
        if (typeDeclaration.getTypeParameters() != null) {
            q.getAndSend(typeDeclaration, JS.TypeDeclaration::getTypeParameters, el -> visit(el, q));
        }
        q.getAndSend(typeDeclaration, el -> el.getPadding().getInitializer(), el -> visitLeftPadded(el, q));
        if (typeDeclaration.getType() != null) {
            q.getAndSend(typeDeclaration, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return typeDeclaration;
    }

    @Override
    public J visitTypeOf(JS.TypeOf typeOf, RpcSendQueue q) {
        q.getAndSend(typeOf, JS.TypeOf::getExpression, el -> visit(el, q));
        if (typeOf.getType() != null) {
            q.getAndSend(typeOf, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return typeOf;
    }

    @Override
    public J visitTypeTreeExpression(JS.TypeTreeExpression typeTreeExpression, RpcSendQueue q) {
        q.getAndSend(typeTreeExpression, JS.TypeTreeExpression::getExpression, el -> visit(el, q));
        return typeTreeExpression;
    }

    @Override
    public J visitJsAssignmentOperation(JS.JsAssignmentOperation jsAssignmentOperation, RpcSendQueue q) {
        q.getAndSend(jsAssignmentOperation, JS.JsAssignmentOperation::getVariable, el -> visit(el, q));
        q.getAndSend(jsAssignmentOperation, el -> el.getPadding().getOperator(), el -> visitLeftPadded(el, q));
        q.getAndSend(jsAssignmentOperation, JS.JsAssignmentOperation::getAssignment, el -> visit(el, q));
        if (jsAssignmentOperation.getType() != null) {
            q.getAndSend(jsAssignmentOperation, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return jsAssignmentOperation;
    }

    @Override
    public J visitIndexedAccessType(JS.IndexedAccessType indexedAccessType, RpcSendQueue q) {
        q.getAndSend(indexedAccessType, JS.IndexedAccessType::getObjectType, el -> visit(el, q));
        q.getAndSend(indexedAccessType, JS.IndexedAccessType::getIndexType, el -> visit(el, q));
        if (indexedAccessType.getType() != null) {
            q.getAndSend(indexedAccessType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return indexedAccessType;
    }

    @Override
    public J visitIndexedAccessTypeIndexType(JS.IndexedAccessType.IndexType indexType, RpcSendQueue q) {
        q.getAndSend(indexType, el -> el.getPadding().getElement(), el -> visitRightPadded(el, q));
        if (indexType.getType() != null) {
            q.getAndSend(indexType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return indexType;
    }

    @Override
    public J visitTypeQuery(JS.TypeQuery typeQuery, RpcSendQueue q) {
        q.getAndSend(typeQuery, JS.TypeQuery::getTypeExpression, el -> visit(el, q));
        if (typeQuery.getPadding().getTypeArguments() != null) {
            q.getAndSend(typeQuery, el -> el.getPadding().getTypeArguments(), el -> visitContainer(el, q));
        }
        if (typeQuery.getType() != null) {
            q.getAndSend(typeQuery, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return typeQuery;
    }

    @Override
    public J visitTypeInfo(JS.TypeInfo typeInfo, RpcSendQueue q) {
        q.getAndSend(typeInfo, JS.TypeInfo::getTypeIdentifier, el -> visit(el, q));
        return typeInfo;
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
        if (typePredicate.getPadding().getExpression() != null) {
            q.getAndSend(typePredicate, el -> el.getPadding().getExpression(), el -> visitLeftPadded(el, q));
        }
        if (typePredicate.getType() != null) {
            q.getAndSend(typePredicate, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return typePredicate;
    }

    @Override
    public J visitUnion(JS.Union union, RpcSendQueue q) {
        q.getAndSendList(union, el -> el.getPadding().getTypes(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        if (union.getType() != null) {
            q.getAndSend(union, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return union;
    }

    @Override
    public J visitIntersection(JS.Intersection intersection, RpcSendQueue q) {
        q.getAndSendList(intersection, el -> el.getPadding().getTypes(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        if (intersection.getType() != null) {
            q.getAndSend(intersection, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return intersection;
    }

    @Override
    public J visitVoid(JS.Void void_, RpcSendQueue q) {
        q.getAndSend(void_, JS.Void::getExpression, el -> visit(el, q));
        return void_;
    }

    @Override
    public J visitUnary(JS.Unary unary, RpcSendQueue q) {
        q.getAndSend(unary, el -> el.getPadding().getOperator(), el -> visitLeftPadded(el, q));
        q.getAndSend(unary, JS.Unary::getExpression, el -> visit(el, q));
        if (unary.getType() != null) {
            q.getAndSend(unary, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return unary;
    }

    @Override
    public J visitYield(JS.Yield yield, RpcSendQueue q) {
        q.getAndSend(yield, el -> el.getPadding().getDelegated(), el -> visitLeftPadded(el, q));
        if (yield.getExpression() != null) {
            q.getAndSend(yield, JS.Yield::getExpression, el -> visit(el, q));
        }
        if (yield.getType() != null) {
            q.getAndSend(yield, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return yield;
    }

    @Override
    public J visitWithStatement(JS.WithStatement withStatement, RpcSendQueue q) {
        q.getAndSend(withStatement, JS.WithStatement::getExpression, el -> visit(el, q));
        q.getAndSend(withStatement, el -> el.getPadding().getBody(), el -> visitRightPadded(el, q));
        return withStatement;
    }

    @Override
    public J visitIndexSignatureDeclaration(JS.IndexSignatureDeclaration indexSignatureDeclaration, RpcSendQueue q) {
        q.getAndSendList(indexSignatureDeclaration, JS.IndexSignatureDeclaration::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSend(indexSignatureDeclaration, el -> el.getPadding().getParameters(), el -> visitContainer(el, q));
        q.getAndSend(indexSignatureDeclaration, el -> el.getPadding().getTypeExpression(), el -> visitLeftPadded(el, q));
        if (indexSignatureDeclaration.getType() != null) {
            q.getAndSend(indexSignatureDeclaration, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return indexSignatureDeclaration;
    }

    @Override
    public J visitJSMethodDeclaration(JS.JSMethodDeclaration jSMethodDeclaration, RpcSendQueue q) {
        q.getAndSendList(jSMethodDeclaration, JS.JSMethodDeclaration::getLeadingAnnotations, J.Annotation::getId, el -> visit(el, q));
        q.getAndSendList(jSMethodDeclaration, JS.JSMethodDeclaration::getModifiers, J.Modifier::getId, el -> visit(el, q));
        if (jSMethodDeclaration.getTypeParameters() != null) {
            q.getAndSend(jSMethodDeclaration, JS.JSMethodDeclaration::getTypeParameters, el -> visit(el, q));
        }
        if (jSMethodDeclaration.getReturnTypeExpression() != null) {
            q.getAndSend(jSMethodDeclaration, JS.JSMethodDeclaration::getReturnTypeExpression, el -> visit(el, q));
        }
        q.getAndSend(jSMethodDeclaration, JS.JSMethodDeclaration::getName, el -> visit(el, q));
        q.getAndSend(jSMethodDeclaration, el -> el.getPadding().getParameters(), el -> visitContainer(el, q));
        if (jSMethodDeclaration.getBody() != null) {
            q.getAndSend(jSMethodDeclaration, JS.JSMethodDeclaration::getBody, el -> visit(el, q));
        }
        if (jSMethodDeclaration.getPadding().getDefaultValue() != null) {
            q.getAndSend(jSMethodDeclaration, el -> el.getPadding().getDefaultValue(), el -> visitLeftPadded(el, q));
        }
        if (jSMethodDeclaration.getMethodType() != null) {
            q.getAndSend(jSMethodDeclaration, el -> asRef(el.getMethodType()), el -> visitType(getValueNonNull(el), q));
        }
        return jSMethodDeclaration;
    }

    @Override
    public J visitJSForOfLoop(JS.JSForOfLoop jSForOfLoop, RpcSendQueue q) {
        q.getAndSend(jSForOfLoop, el -> el.getPadding().getAwait(), el -> visitLeftPadded(el, q));
        q.getAndSend(jSForOfLoop, JS.JSForOfLoop::getControl, el -> visit(el, q));
        q.getAndSend(jSForOfLoop, el -> el.getPadding().getBody(), el -> visitRightPadded(el, q));
        return jSForOfLoop;
    }

    @Override
    public J visitJSForInLoop(JS.JSForInLoop jSForInLoop, RpcSendQueue q) {
        q.getAndSend(jSForInLoop, JS.JSForInLoop::getControl, el -> visit(el, q));
        q.getAndSend(jSForInLoop, el -> el.getPadding().getBody(), el -> visitRightPadded(el, q));
        return jSForInLoop;
    }

    @Override
    public J visitJSForInOfLoopControl(JS.JSForInOfLoopControl jSForInOfLoopControl, RpcSendQueue q) {
        q.getAndSend(jSForInOfLoopControl, el -> el.getPadding().getVariable(), el -> visitRightPadded(el, q));
        q.getAndSend(jSForInOfLoopControl, el -> el.getPadding().getIterable(), el -> visitRightPadded(el, q));
        return jSForInOfLoopControl;
    }

    @Override
    public J visitJSTry(JS.JSTry jSTry, RpcSendQueue q) {
        q.getAndSend(jSTry, JS.JSTry::getBody, el -> visit(el, q));
        q.getAndSend(jSTry, JS.JSTry::getCatches, el -> visit(el, q));
        if (jSTry.getPadding().getFinallie() != null) {
            q.getAndSend(jSTry, el -> el.getPadding().getFinallie(), el -> visitLeftPadded(el, q));
        }
        return jSTry;
    }

    @Override
    public J visitJSTryJSCatch(JS.JSTry.JSCatch jSCatch, RpcSendQueue q) {
        q.getAndSend(jSCatch, JS.JSTry.JSCatch::getParameter, el -> visit(el, q));
        q.getAndSend(jSCatch, JS.JSTry.JSCatch::getBody, el -> visit(el, q));
        return jSCatch;
    }

    @Override
    public J visitNamespaceDeclaration(JS.NamespaceDeclaration namespaceDeclaration, RpcSendQueue q) {
        q.getAndSendList(namespaceDeclaration, JS.NamespaceDeclaration::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSend(namespaceDeclaration, el -> el.getPadding().getKeywordType(), el -> visitLeftPadded(el, q));
        q.getAndSend(namespaceDeclaration, el -> el.getPadding().getName(), el -> visitRightPadded(el, q));
        if (namespaceDeclaration.getBody() != null) {
            q.getAndSend(namespaceDeclaration, JS.NamespaceDeclaration::getBody, el -> visit(el, q));
        }
        return namespaceDeclaration;
    }

    @Override
    public J visitFunctionDeclaration(JS.FunctionDeclaration functionDeclaration, RpcSendQueue q) {
        q.getAndSendList(functionDeclaration, JS.FunctionDeclaration::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSend(functionDeclaration, el -> el.getPadding().getAsteriskToken(), el -> visitLeftPadded(el, q));
        q.getAndSend(functionDeclaration, el -> el.getPadding().getName(), el -> visitLeftPadded(el, q));
        if (functionDeclaration.getTypeParameters() != null) {
            q.getAndSend(functionDeclaration, JS.FunctionDeclaration::getTypeParameters, el -> visit(el, q));
        }
        q.getAndSend(functionDeclaration, el -> el.getPadding().getParameters(), el -> visitContainer(el, q));
        if (functionDeclaration.getReturnTypeExpression() != null) {
            q.getAndSend(functionDeclaration, JS.FunctionDeclaration::getReturnTypeExpression, el -> visit(el, q));
        }
        if (functionDeclaration.getBody() != null) {
            q.getAndSend(functionDeclaration, JS.FunctionDeclaration::getBody, el -> visit(el, q));
        }
        if (functionDeclaration.getType() != null) {
            q.getAndSend(functionDeclaration, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return functionDeclaration;
    }

    @Override
    public J visitTypeLiteral(JS.TypeLiteral typeLiteral, RpcSendQueue q) {
        q.getAndSend(typeLiteral, JS.TypeLiteral::getMembers, el -> visit(el, q));
        if (typeLiteral.getType() != null) {
            q.getAndSend(typeLiteral, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return typeLiteral;
    }

    @Override
    public J visitArrayBindingPattern(JS.ArrayBindingPattern arrayBindingPattern, RpcSendQueue q) {
        q.getAndSend(arrayBindingPattern, el -> el.getPadding().getElements(), el -> visitContainer(el, q));
        if (arrayBindingPattern.getType() != null) {
            q.getAndSend(arrayBindingPattern, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return arrayBindingPattern;
    }

    @Override
    public J visitBindingElement(JS.BindingElement bindingElement, RpcSendQueue q) {
        if (bindingElement.getPadding().getPropertyName() != null) {
            q.getAndSend(bindingElement, el -> el.getPadding().getPropertyName(), el -> visitRightPadded(el, q));
        }
        q.getAndSend(bindingElement, JS.BindingElement::getName, el -> visit(el, q));
        if (bindingElement.getPadding().getInitializer() != null) {
            q.getAndSend(bindingElement, el -> el.getPadding().getInitializer(), el -> visitLeftPadded(el, q));
        }
        if (bindingElement.getVariableType() != null) {
            q.getAndSend(bindingElement, el -> asRef(el.getVariableType()), el -> visitType(getValueNonNull(el), q));
        }
        return bindingElement;
    }

    @Override
    public J visitExportDeclaration(JS.ExportDeclaration exportDeclaration, RpcSendQueue q) {
        q.getAndSendList(exportDeclaration, JS.ExportDeclaration::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSend(exportDeclaration, el -> el.getPadding().getTypeOnly(), el -> visitLeftPadded(el, q));
        if (exportDeclaration.getExportClause() != null) {
            q.getAndSend(exportDeclaration, JS.ExportDeclaration::getExportClause, el -> visit(el, q));
        }
        if (exportDeclaration.getPadding().getModuleSpecifier() != null) {
            q.getAndSend(exportDeclaration, el -> el.getPadding().getModuleSpecifier(), el -> visitLeftPadded(el, q));
        }
        if (exportDeclaration.getAttributes() != null) {
            q.getAndSend(exportDeclaration, JS.ExportDeclaration::getAttributes, el -> visit(el, q));
        }
        return exportDeclaration;
    }

    @Override
    public J visitExportAssignment(JS.ExportAssignment exportAssignment, RpcSendQueue q) {
        q.getAndSendList(exportAssignment, JS.ExportAssignment::getModifiers, J.Modifier::getId, el -> visit(el, q));
        q.getAndSend(exportAssignment, el -> el.getPadding().getExportEquals(), el -> visitLeftPadded(el, q));
        if (exportAssignment.getExpression() != null) {
            q.getAndSend(exportAssignment, JS.ExportAssignment::getExpression, el -> visit(el, q));
        }
        return exportAssignment;
    }

    @Override
    public J visitNamedExports(JS.NamedExports namedExports, RpcSendQueue q) {
        q.getAndSend(namedExports, el -> el.getPadding().getElements(), el -> visitContainer(el, q));
        if (namedExports.getType() != null) {
            q.getAndSend(namedExports, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return namedExports;
    }

    @Override
    public J visitExportSpecifier(JS.ExportSpecifier exportSpecifier, RpcSendQueue q) {
        q.getAndSend(exportSpecifier, el -> el.getPadding().getTypeOnly(), el -> visitLeftPadded(el, q));
        q.getAndSend(exportSpecifier, JS.ExportSpecifier::getSpecifier, el -> visit(el, q));
        if (exportSpecifier.getType() != null) {
            q.getAndSend(exportSpecifier, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        }
        return exportSpecifier;
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
