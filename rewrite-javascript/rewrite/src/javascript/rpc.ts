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
import {JavaScriptVisitor} from "./visitor";
import {asRef, RpcCodec, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {isJavaScript, JS, JSX} from "./tree";
import {Expression, J, JavaType, Statement, TypedTree, TypeTree} from "../java";
import {createDraft, finishDraft} from "immer";
import {JavaReceiver, JavaSender} from "../java/rpc";
import {Cursor, Tree, TreeKind} from "../tree";
import ComputedPropertyName = JS.ComputedPropertyName;

class JavaScriptSender extends JavaScriptVisitor<RpcSendQueue> {
    private javaSender: JavaScriptSenderDelegate;

    constructor() {
        super();
        this.javaSender = new JavaScriptSenderDelegate(this);
    }

    override async visit<R extends J>(tree: Tree, p: RpcSendQueue, parent?: Cursor): Promise<R | undefined> {
        if (isJavaScript(tree)) {
            return super.visit(tree, p, parent);
        }

        return this.javaSender.visit(tree, p, parent);
    }

    override async preVisit(j: JS, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(j, j2 => j2.id);
        await q.getAndSend(j, j2 => j2.prefix, space => this.visitSpace(space, q));
        await q.sendMarkers(j, j2 => j2.markers);
        return j;
    }

    override async visitJsCompilationUnit(cu: JS.CompilationUnit, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(cu, c => c.sourcePath);
        await q.getAndSend(cu, c => c.charsetName);
        await q.getAndSend(cu, c => c.charsetBomMarked);
        await q.getAndSend(cu, c => c.checksum);
        await q.getAndSend(cu, c => c.fileAttributes);
        await q.getAndSendList(cu, c => c.statements, stmt => stmt.element.id, stmt => this.visitRightPadded(stmt, q));
        await q.getAndSend(cu, c => c.eof, space => this.visitSpace(space, q));
        return cu;
    }

    override async visitAlias(alias: JS.Alias, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(alias, el => el.propertyName, el => this.visitRightPadded(el, q));
        await q.getAndSend(alias, el => el.alias, el => this.visit(el, q));
        return alias;
    }

    override async visitArrowFunction(arrowFunction: JS.ArrowFunction, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(arrowFunction, el => el.leadingAnnotations, el => el.id, el => this.visit(el, q));
        await q.getAndSendList(arrowFunction, el => el.modifiers, el => el.id, el => this.visit(el, q));
        await q.getAndSend(arrowFunction, el => el.typeParameters, el => this.visit(el, q));
        await q.getAndSend(arrowFunction, el => el.lambda, el => this.visit(el, q));
        await q.getAndSend(arrowFunction, el => el.returnTypeExpression, el => this.visit(el, q));
        return arrowFunction;
    }

    override async visitAwait(await_: JS.Await, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(await_, el => el.expression, el => this.visit(el, q));
        await q.getAndSend(await_, el => asRef(el.type), el => this.visitType(el, q));
        return await_;
    }

    override async visitConditionalType(conditionalType: JS.ConditionalType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(conditionalType, el => el.checkType, el => this.visit(el, q));
        await q.getAndSend(conditionalType, el => el.condition, el => this.visitLeftPadded(el, q));
        await q.getAndSend(conditionalType, el => asRef(el.type), el => this.visitType(el, q));
        return conditionalType;
    }

    override async visitDelete(delete_: JS.Delete, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(delete_, el => el.expression, el => this.visit(el, q));
        return delete_;
    }

    override async visitExpressionStatement(expressionStatement: JS.ExpressionStatement, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(expressionStatement, el => el.expression, el => this.visit(el, q));
        return expressionStatement;
    }

    override async visitExpressionWithTypeArguments(expressionWithTypeArguments: JS.ExpressionWithTypeArguments, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(expressionWithTypeArguments, el => el.clazz, el => this.visit(el, q));
        await q.getAndSend(expressionWithTypeArguments, el => el.typeArguments, el => this.visitContainer(el, q));
        await q.getAndSend(expressionWithTypeArguments, el => asRef(el.type), el => this.visitType(el, q));
        return expressionWithTypeArguments;
    }

    override async visitFunctionCall(functionCall: JS.FunctionCall, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(functionCall, m => m.function, f => this.visitRightPadded(f, q));
        await q.getAndSend(functionCall, m => m.typeParameters, params => this.visitContainer(params, q));
        await q.getAndSend(functionCall, m => m.arguments, args => this.visitContainer(args, q));
        await q.getAndSend(functionCall, m => asRef(m.functionType), type => this.visitType(type, q));
        return functionCall;
    }

    override async visitFunctionType(functionType: JS.FunctionType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(functionType, el => el.modifiers, el => el.id, el => this.visit(el, q));
        await q.getAndSend(functionType, el => el.constructorType, el => this.visitLeftPadded(el, q));
        await q.getAndSend(functionType, el => el.typeParameters, el => this.visit(el, q));
        await q.getAndSend(functionType, el => el.parameters, el => this.visitContainer(el, q));
        await q.getAndSend(functionType, el => el.returnType, el => this.visitLeftPadded(el, q));
        return functionType;
    }

    override async visitInferType(inferType: JS.InferType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(inferType, el => el.typeParameter, el => this.visitLeftPadded(el, q));
        await q.getAndSend(inferType, el => asRef(el.type), el => this.visitType(el, q));
        return inferType;
    }

    override async visitImportType(importType: JS.ImportType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(importType, el => el.hasTypeof, el => this.visitRightPadded(el, q));
        await q.getAndSend(importType, el => el.argumentAndAttributes, el => this.visitContainer(el, q));
        await q.getAndSend(importType, el => el.qualifier, el => this.visitLeftPadded(el, q));
        await q.getAndSend(importType, el => el.typeArguments, el => this.visitContainer(el, q));
        await q.getAndSend(importType, el => asRef(el.type), el => this.visitType(el, q));
        return importType;
    }

    override async visitImportDeclaration(jsImport: JS.Import, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(jsImport, el => el.modifiers, el => el.id, el => this.visit(el, q));
        await q.getAndSend(jsImport, el => el.importClause, el => this.visit(el, q));
        await q.getAndSend(jsImport, el => el.moduleSpecifier, el => this.visitLeftPadded(el, q));
        await q.getAndSend(jsImport, el => el.attributes, el => this.visit(el, q));
        await q.getAndSend(jsImport, el => el.initializer, el => this.visitLeftPadded(el, q));
        return jsImport;
    }

    override async visitImportClause(jsImportClause: JS.ImportClause, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(jsImportClause, el => el.typeOnly);
        await q.getAndSend(jsImportClause, el => el.name, el => this.visitRightPadded(el, q));
        await q.getAndSend(jsImportClause, el => el.namedBindings, el => this.visit(el, q));
        return jsImportClause;
    }

    override async visitNamedImports(namedImports: JS.NamedImports, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(namedImports, el => el.elements, el => this.visitContainer(el, q));
        await q.getAndSend(namedImports, el => asRef(el.type), el => this.visitType(el, q));
        return namedImports;
    }

    override async visitImportSpecifier(jsImportSpecifier: JS.ImportSpecifier, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(jsImportSpecifier, el => el.importType, el => this.visitLeftPadded(el, q));
        await q.getAndSend(jsImportSpecifier, el => el.specifier, el => this.visit(el, q));
        await q.getAndSend(jsImportSpecifier, el => asRef(el.type), el => this.visitType(el, q));
        return jsImportSpecifier;
    }

    override async visitImportAttributes(importAttributes: JS.ImportAttributes, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(importAttributes, el => el.token);
        await q.getAndSend(importAttributes, el => el.elements, el => this.visitContainer(el, q));
        return importAttributes;
    }

    override async visitImportTypeAttributes(importTypeAttributes: JS.ImportTypeAttributes, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(importTypeAttributes, el => el.token, el => this.visitRightPadded(el, q));
        await q.getAndSend(importTypeAttributes, el => el.elements, el => this.visitContainer(el, q));
        await q.getAndSend(importTypeAttributes, el => el.end, el => this.visitSpace(el, q));
        return importTypeAttributes;
    }

    override async visitImportAttribute(importAttribute: JS.ImportAttribute, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(importAttribute, el => el.name, el => this.visit(el, q));
        await q.getAndSend(importAttribute, el => el.value, el => this.visitLeftPadded(el, q));
        return importAttribute;
    }

    override async visitBinaryExtensions(binary: JS.Binary, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(binary, el => el.left, el => this.visit(el, q));
        await q.getAndSend(binary, el => el.operator, el => this.visitLeftPadded(el, q));
        await q.getAndSend(binary, el => el.right, el => this.visit(el, q));
        await q.getAndSend(binary, el => asRef(el.type), el => this.visitType(el, q));
        return binary;
    }

    override async visitLiteralType(literalType: JS.LiteralType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(literalType, el => el.literal, el => this.visit(el, q));
        await q.getAndSend(literalType, el => asRef(el.type), el => this.visitType(el, q));
        return literalType;
    }

    override async visitMappedType(mappedType: JS.MappedType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(mappedType, el => el.prefixToken, el => this.visitLeftPadded(el, q));
        await q.getAndSend(mappedType, el => el.hasReadonly, el => this.visitLeftPadded(el, q));
        await q.getAndSend(mappedType, el => el.keysRemapping, el => this.visit(el, q));
        await q.getAndSend(mappedType, el => el.suffixToken, el => this.visitLeftPadded(el, q));
        await q.getAndSend(mappedType, el => el.hasQuestionToken, el => this.visitLeftPadded(el, q));
        await q.getAndSend(mappedType, el => el.valueType, el => this.visitContainer(el, q));
        await q.getAndSend(mappedType, el => asRef(el.type), el => this.visitType(el, q));
        return mappedType;
    }

    override async visitMappedTypeKeysRemapping(keysRemapping: JS.MappedType.KeysRemapping, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(keysRemapping, el => el.typeParameter, el => this.visitRightPadded(el, q));
        await q.getAndSend(keysRemapping, el => el.nameType, el => this.visitRightPadded(el, q));
        return keysRemapping;
    }

    override async visitMappedTypeParameter(mappedTypeParameter: JS.MappedType.Parameter, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(mappedTypeParameter, el => el.name, el => this.visit(el, q));
        await q.getAndSend(mappedTypeParameter, el => el.iterateType, el => this.visitLeftPadded(el, q));
        return mappedTypeParameter;
    }

    override async visitObjectBindingPattern(objectBindingPattern: JS.ObjectBindingPattern, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(objectBindingPattern, el => el.leadingAnnotations, el => el.id, el => this.visit(el, q));
        await q.getAndSendList(objectBindingPattern, el => el.modifiers, el => el.id, el => this.visit(el, q));
        await q.getAndSend(objectBindingPattern, el => el.typeExpression, el => this.visit(el, q));
        await q.getAndSend(objectBindingPattern, el => el.bindings, el => this.visitContainer(el, q));
        await q.getAndSend(objectBindingPattern, el => el.initializer, el => this.visitLeftPadded(el, q));
        return objectBindingPattern;
    }

    override async visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(propertyAssignment, el => el.name, el => this.visitRightPadded(el, q));
        await q.getAndSend(propertyAssignment, el => el.assigmentToken);
        await q.getAndSend(propertyAssignment, el => el.initializer, el => this.visit(el, q));
        return propertyAssignment;
    }

    override async visitSatisfiesExpression(satisfiesExpression: JS.SatisfiesExpression, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(satisfiesExpression, el => el.expression, el => this.visit(el, q));
        await q.getAndSend(satisfiesExpression, el => el.satisfiesType, el => this.visitLeftPadded(el, q));
        await q.getAndSend(satisfiesExpression, el => asRef(el.type), el => this.visitType(el, q));
        return satisfiesExpression;
    }

    override async visitScopedVariableDeclarations(scopedVariableDeclarations: JS.ScopedVariableDeclarations, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(scopedVariableDeclarations, el => el.modifiers, el => el.id, el => this.visit(el, q));
        await q.getAndSendList(scopedVariableDeclarations, el => el.variables, el => el.element.id, el => this.visitRightPadded(el, q));
        return scopedVariableDeclarations;
    }

    override async visitStatementExpression(statementExpression: JS.StatementExpression, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(statementExpression, el => el.statement, el => this.visit(el, q));
        return statementExpression;
    }

    override async visitTaggedTemplateExpression(taggedTemplateExpression: JS.TaggedTemplateExpression, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(taggedTemplateExpression, el => el.tag, el => this.visitRightPadded(el, q));
        await q.getAndSend(taggedTemplateExpression, el => el.typeArguments, el => this.visitContainer(el, q));
        await q.getAndSend(taggedTemplateExpression, el => el.templateExpression, el => this.visit(el, q));
        await q.getAndSend(taggedTemplateExpression, el => asRef(el.type), el => this.visitType(el, q));
        return taggedTemplateExpression;
    }

    override async visitTemplateExpression(templateExpression: JS.TemplateExpression, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(templateExpression, el => el.head, el => this.visit(el, q));
        await q.getAndSendList(templateExpression, el => el.spans, el => el.element.id, el => this.visitRightPadded(el, q));
        await q.getAndSend(templateExpression, el => asRef(el.type), el => this.visitType(el, q));
        return templateExpression;
    }

    override async visitTemplateExpressionSpan(span: JS.TemplateExpression.Span, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(span, el => el.expression, el => this.visit(el, q));
        await q.getAndSend(span, el => el.tail, el => this.visit(el, q));
        return span;
    }

    override async visitTuple(tuple: JS.Tuple, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(tuple, el => el.elements, el => this.visitContainer(el, q));
        await q.getAndSend(tuple, el => asRef(el.type), el => this.visitType(el, q));
        return tuple;
    }

    override async visitTypeDeclaration(typeDeclaration: JS.TypeDeclaration, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(typeDeclaration, el => el.modifiers, el => el.id, el => this.visit(el, q));
        await q.getAndSend(typeDeclaration, el => el.name, el => this.visitLeftPadded(el, q));
        await q.getAndSend(typeDeclaration, el => el.typeParameters, el => this.visit(el, q));
        await q.getAndSend(typeDeclaration, el => el.initializer, el => this.visitLeftPadded(el, q));
        await q.getAndSend(typeDeclaration, el => asRef(el.type), el => this.visitType(el, q));
        return typeDeclaration;
    }

    override async visitTypeOf(typeOf: JS.TypeOf, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typeOf, el => el.expression, el => this.visit(el, q));
        await q.getAndSend(typeOf, el => asRef(el.type), el => this.visitType(el, q));
        return typeOf;
    }

    override async visitTypeTreeExpression(typeTreeExpression: JS.TypeTreeExpression, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typeTreeExpression, el => el.expression, el => this.visit(el, q));
        return typeTreeExpression;
    }

    override async visitAs(as_: JS.As, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(as_, el => el.left, el => this.visitRightPadded(el, q));
        await q.getAndSend(as_, el => el.right, el => this.visit(el, q));
        await q.getAndSend(as_, el => asRef(el.type), el => this.visitType(el, q));
        return as_;
    }

    override async visitAssignmentOperationExtensions(assignmentOperation: JS.AssignmentOperation, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(assignmentOperation, el => el.variable, el => this.visit(el, q));
        await q.getAndSend(assignmentOperation, el => el.operator, el => this.visitLeftPadded(el, q));
        await q.getAndSend(assignmentOperation, el => el.assignment, el => this.visit(el, q));
        await q.getAndSend(assignmentOperation, el => asRef(el.type), el => this.visitType(el, q));
        return assignmentOperation;
    }

    override async visitIndexedAccessType(indexedAccessType: JS.IndexedAccessType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(indexedAccessType, el => el.objectType, el => this.visit(el, q));
        await q.getAndSend(indexedAccessType, el => el.indexType, el => this.visit(el, q));
        await q.getAndSend(indexedAccessType, el => asRef(el.type), el => this.visitType(el, q));
        return indexedAccessType;
    }

    override async visitIndexedAccessTypeIndexType(indexType: JS.IndexedAccessType.IndexType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(indexType, el => el.element, el => this.visitRightPadded(el, q));
        await q.getAndSend(indexType, el => asRef(el.type), el => this.visitType(el, q));
        return indexType;
    }

    override async visitTypeQuery(typeQuery: JS.TypeQuery, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typeQuery, el => el.typeExpression, el => this.visit(el, q));
        await q.getAndSend(typeQuery, el => el.typeArguments, el => this.visitContainer(el, q));
        await q.getAndSend(typeQuery, el => asRef(el.type), el => this.visitType(el, q));
        return typeQuery;
    }

    override async visitTypeInfo(typeInfo: JS.TypeInfo, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typeInfo, el => el.typeIdentifier, el => this.visit(el, q));
        return typeInfo;
    }

    override async visitComputedPropertyName(computedPropertyName: JS.ComputedPropertyName, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(computedPropertyName, el => el.expression, el => this.visitRightPadded(el, q));
        return computedPropertyName;
    }

    override async visitTypeOperator(typeOperator: JS.TypeOperator, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typeOperator, el => el.operator);
        await q.getAndSend(typeOperator, el => el.expression, el => this.visitLeftPadded(el, q));
        return typeOperator;
    }

    override async visitTypePredicate(typePredicate: JS.TypePredicate, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typePredicate, el => el.asserts, el => this.visitLeftPadded(el, q));
        await q.getAndSend(typePredicate, el => el.parameterName, el => this.visit(el, q));
        await q.getAndSend(typePredicate, el => el.expression, el => this.visitLeftPadded(el, q));
        await q.getAndSend(typePredicate, el => asRef(el.type), el => this.visitType(el, q));
        return typePredicate;
    }

    override async visitUnion(union: JS.Union, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(union, el => el.types, el => el.element.id, el => this.visitRightPadded(el, q));
        await q.getAndSend(union, el => asRef(el.type), el => this.visitType(el, q));
        return union;
    }

    override async visitIntersection(intersection: JS.Intersection, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(intersection, el => el.types, el => el.element.id, el => this.visitRightPadded(el, q));
        await q.getAndSend(intersection, el => asRef(el.type), el => this.visitType(el, q));
        return intersection;
    }

    override async visitVoid(void_: JS.Void, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(void_, el => el.expression, el => this.visit(el, q));
        return void_;
    }

    override async visitWithStatement(withStatement: JS.WithStatement, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(withStatement, el => el.expression, el => this.visit(el, q));
        await q.getAndSend(withStatement, el => el.body, el => this.visitRightPadded(el, q));
        return withStatement;
    }

    override async visitJsxTag(tag: JSX.Tag, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(tag, el => el.openName, el => this.visitLeftPadded(el, q));
        await q.getAndSend(tag, el => el.typeArguments, el => this.visitContainer(el, q));
        await q.getAndSend(tag, el => el.afterName, space => this.visitSpace(space, q));
        await q.getAndSendList(tag, el => el.attributes, attr => attr.element.id, attr => this.visitRightPadded(attr, q));

        await q.getAndSend(tag, el => el.selfClosing, space => this.visitSpace(space, q));
        await q.getAndSendList(tag, el => el.children!, child => child.id, child => this.visit(child, q));
        await q.getAndSend(tag, el => el.closingName, el => this.visitLeftPadded(el, q));
        await q.getAndSend(tag, el => el.afterClosingName, el => this.visitSpace(el, q));

        return tag;
    }

    override async visitJsxAttribute(attribute: JSX.Attribute, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(attribute, el => el.key, el => this.visit(el, q));
        await q.getAndSend(attribute, el => el.value, el => this.visitLeftPadded(el, q));
        return attribute;
    }

    override async visitJsxSpreadAttribute(spreadAttribute: JSX.SpreadAttribute, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(spreadAttribute, el => el.dots, space => this.visitSpace(space, q));
        await q.getAndSend(spreadAttribute, el => el.expression, el => this.visitRightPadded(el, q));
        return spreadAttribute;
    }

    override async visitJsxEmbeddedExpression(embeddedExpression: JSX.EmbeddedExpression, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(embeddedExpression, el => el.expression, el => this.visitRightPadded(el, q));
        return embeddedExpression;
    }

    override async visitJsxNamespacedName(namespacedName: JSX.NamespacedName, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(namespacedName, el => el.namespace, el => this.visit(el, q));
        await q.getAndSend(namespacedName, el => el.name, el => this.visitLeftPadded(el, q));
        return namespacedName;
    }

    override async visitIndexSignatureDeclaration(indexSignatureDeclaration: JS.IndexSignatureDeclaration, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(indexSignatureDeclaration, el => el.modifiers, el => el.id, el => this.visit(el, q));
        await q.getAndSend(indexSignatureDeclaration, el => el.parameters, el => this.visitContainer(el, q));
        await q.getAndSend(indexSignatureDeclaration, el => el.typeExpression, el => this.visitLeftPadded(el, q));
        await q.getAndSend(indexSignatureDeclaration, el => asRef(el.type), el => this.visitType(el, q));
        return indexSignatureDeclaration;
    }

    override async visitComputedPropertyMethodDeclaration(computedPropMethod: JS.ComputedPropertyMethodDeclaration, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(computedPropMethod, el => el.leadingAnnotations, el => el.id, el => this.visit(el, q));
        await q.getAndSendList(computedPropMethod, el => el.modifiers, el => el.id, el => this.visit(el, q));
        await q.getAndSend(computedPropMethod, el => el.typeParameters, el => this.visit(el, q));
        await q.getAndSend(computedPropMethod, el => el.returnTypeExpression, el => this.visit(el, q));
        await q.getAndSend(computedPropMethod, el => el.name, el => this.visit(el, q));
        await q.getAndSend(computedPropMethod, el => el.parameters, el => this.visitContainer(el, q));
        await q.getAndSend(computedPropMethod, el => el.body, el => this.visit(el, q));
        await q.getAndSend(computedPropMethod, el => el.methodType, el => this.visitType(el, q));
        return computedPropMethod;
    }

    override async visitForOfLoop(forOfLoop: JS.ForOfLoop, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(forOfLoop, el => el.await, space => this.visitSpace(space, q));
        await q.getAndSend(forOfLoop, el => el.loop, el => this.visit(el, q));
        return forOfLoop;
    }

    override async visitForInLoop(forInLoop: JS.ForInLoop, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(forInLoop, el => el.control, el => this.visit(el, q));
        await q.getAndSend(forInLoop, el => el.body, el => this.visitRightPadded(el, q));
        return forInLoop;
    }

    override async visitNamespaceDeclaration(namespaceDeclaration: JS.NamespaceDeclaration, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(namespaceDeclaration, el => el.modifiers, el => el.id, el => this.visit(el, q));
        await q.getAndSend(namespaceDeclaration, el => el.keywordType, el => this.visitLeftPadded(el, q));
        await q.getAndSend(namespaceDeclaration, el => el.name, el => this.visitRightPadded(el, q));
        await q.getAndSend(namespaceDeclaration, el => el.body, el => this.visit(el, q));
        return namespaceDeclaration;
    }

    override async visitTypeLiteral(typeLiteral: JS.TypeLiteral, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typeLiteral, el => el.members, el => this.visit(el, q));
        await q.getAndSend(typeLiteral, el => asRef(el.type), el => this.visitType(el, q));
        return typeLiteral;
    }

    override async visitArrayBindingPattern(arrayBindingPattern: JS.ArrayBindingPattern, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(arrayBindingPattern, el => el.elements, el => this.visitContainer(el, q));
        await q.getAndSend(arrayBindingPattern, el => asRef(el.type), el => this.visitType(el, q));
        return arrayBindingPattern;
    }

    override async visitBindingElement(bindingElement: JS.BindingElement, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(bindingElement, el => el.propertyName, el => this.visitRightPadded(el, q));
        await q.getAndSend(bindingElement, el => el.name, el => this.visit(el, q));
        await q.getAndSend(bindingElement, el => el.initializer, el => this.visitLeftPadded(el, q));
        await q.getAndSend(bindingElement, el => el.variableType, el => this.visitType(el, q));
        return bindingElement;
    }

    override async visitExportDeclaration(exportDeclaration: JS.ExportDeclaration, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(exportDeclaration, el => el.modifiers, el => el.id, el => this.visit(el, q));
        await q.getAndSend(exportDeclaration, el => el.typeOnly, el => this.visitLeftPadded(el, q));
        await q.getAndSend(exportDeclaration, el => el.exportClause, el => this.visit(el, q));
        await q.getAndSend(exportDeclaration, el => el.moduleSpecifier, el => this.visitLeftPadded(el, q));
        await q.getAndSend(exportDeclaration, el => el.attributes, el => this.visit(el, q));
        return exportDeclaration;
    }

    override async visitExportAssignment(exportAssignment: JS.ExportAssignment, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(exportAssignment, el => el.exportEquals);
        await q.getAndSend(exportAssignment, el => el.expression, el => this.visitLeftPadded(el, q));
        return exportAssignment;
    }

    override async visitNamedExports(namedExports: JS.NamedExports, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(namedExports, el => el.elements, el => this.visitContainer(el, q));
        await q.getAndSend(namedExports, el => asRef(el.type), el => this.visitType(el, q));
        return namedExports;
    }

    override async visitExportSpecifier(exportSpecifier: JS.ExportSpecifier, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(exportSpecifier, el => el.typeOnly, el => this.visitLeftPadded(el, q));
        await q.getAndSend(exportSpecifier, el => el.specifier, el => this.visit(el, q));
        await q.getAndSend(exportSpecifier, el => asRef(el.type), el => this.visitType(el, q));
        return exportSpecifier;
    }

    override async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, q: RpcSendQueue): Promise<J.RightPadded<T>> {
        return this.javaSender.visitRightPadded(right, q);
    }

    override async visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, q: RpcSendQueue): Promise<J.LeftPadded<T>> {
        return this.javaSender.visitLeftPadded(left, q);
    }

    override async visitContainer<T extends J>(container: J.Container<T>, q: RpcSendQueue): Promise<J.Container<T>> {
        return this.javaSender.visitContainer(container, q);
    }

    override async visitSpace(space: J.Space, q: RpcSendQueue): Promise<J.Space> {
        return this.javaSender.visitSpace(space, q);
    }

    override async visitType(javaType: JavaType | undefined, q: RpcSendQueue): Promise<JavaType | undefined> {
        return this.javaSender.visitType(javaType, q);
    }
}

class JavaScriptSenderDelegate extends JavaSender {
    private javascriptSender: JavaScriptSender;

    constructor(javascriptSender: JavaScriptSender) {
        super();
        this.javascriptSender = javascriptSender;
    }

    async visit<R extends J>(tree: Tree, p: RpcSendQueue, parent?: Cursor): Promise<R | undefined> {
        // Delegate to JavaScript sender if this is a JavaScript element
        if (isJavaScript(tree)) {
            return this.javascriptSender.visit(tree, p, parent);
        }

        // Otherwise handle as a Java element
        return super.visit(tree, p, parent);
    }
}

class JavaScriptReceiver extends JavaScriptVisitor<RpcReceiveQueue> {
    private javaReceiverDelegate: JavaReceiver;

    constructor() {
        super();
        this.javaReceiverDelegate = new JavaScriptReceiverDelegate(this);
    }

    async visit<R extends J>(tree: Tree, p: RpcReceiveQueue, parent?: Cursor): Promise<R | undefined> {
        if (isJavaScript(tree)) {
            return super.visit(tree, p, parent);
        }
        return this.javaReceiverDelegate.visit(tree, p, parent);
    }

    override async preVisit(j: JS, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(j);

        draft.id = await q.receive(j.id);
        draft.prefix = await q.receive(j.prefix, space => this.visitSpace(space, q));
        draft.markers = await q.receiveMarkers(j.markers);

        return finishDraft(draft);
    }

    override async visitJsCompilationUnit(cu: JS.CompilationUnit, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(cu);

        draft.sourcePath = await q.receive(cu.sourcePath);
        draft.charsetName = await q.receive(cu.charsetName);
        draft.charsetBomMarked = await q.receive(cu.charsetBomMarked);
        draft.checksum = await q.receive(cu.checksum);
        draft.fileAttributes = await q.receive(cu.fileAttributes);
        draft.statements = await q.receiveListDefined(cu.statements, stmt => this.visitRightPadded(stmt, q));
        draft.eof = await q.receive(cu.eof, space => this.visitSpace(space, q));

        return finishDraft(draft);
    }

    override async visitAlias(alias: JS.Alias, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(alias);
        draft.propertyName = await q.receive(draft.propertyName, el => this.visitRightPadded(el, q));
        draft.alias = await q.receive(draft.alias, el => this.visitDefined<Expression>(el, q));
        return finishDraft(draft);
    }

    override async visitArrowFunction(arrowFunction: JS.ArrowFunction, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(arrowFunction);
        draft.leadingAnnotations = await q.receiveListDefined(draft.leadingAnnotations, el => this.visitDefined<J.Annotation>(el, q));
        draft.modifiers = await q.receiveListDefined(draft.modifiers, el => this.visitDefined<J.Modifier>(el, q));
        draft.typeParameters = await q.receive(draft.typeParameters, el => this.visitDefined<J.TypeParameters>(el, q));
        draft.lambda = await q.receive(draft.lambda, el => this.visitDefined<J.Lambda>(el, q));
        draft.returnTypeExpression = await q.receive(draft.returnTypeExpression, el => this.visitDefined<TypeTree>(el, q));
        return finishDraft(draft);
    }

    override async visitAwait(await_: JS.Await, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(await_);
        draft.expression = await q.receive(draft.expression, el => this.visitDefined<Expression>(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitConditionalType(conditionalType: JS.ConditionalType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(conditionalType);
        draft.checkType = await q.receive(draft.checkType, el => this.visitDefined<Expression>(el, q));
        draft.condition = await q.receive(draft.condition, el => this.visitLeftPadded(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitDelete(delete_: JS.Delete, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(delete_);
        draft.expression = await q.receive(draft.expression, el => this.visitDefined<Expression>(el, q));
        return finishDraft(draft);
    }

    override async visitExpressionStatement(expressionStatement: JS.ExpressionStatement, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(expressionStatement);
        draft.expression = await q.receive(draft.expression, el => this.visitDefined<Expression>(el, q));
        return finishDraft(draft);
    }

    override async visitExpressionWithTypeArguments(expressionWithTypeArguments: JS.ExpressionWithTypeArguments, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(expressionWithTypeArguments);
        draft.clazz = await q.receive(draft.clazz, el => this.visitDefined<J>(el, q));
        draft.typeArguments = await q.receive(draft.typeArguments, el => this.visitContainer(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitFunctionCall(functionCall: JS.FunctionCall, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(functionCall);

        draft.function = await q.receive(functionCall.function, select => this.visitRightPadded(select, q));
        draft.typeParameters = await q.receive(functionCall.typeParameters, typeParams => this.visitContainer(typeParams, q));
        draft.arguments = await q.receive(functionCall.arguments, args => this.visitContainer(args, q));
        draft.functionType = await q.receive(functionCall.functionType, type => this.visitType(type, q) as unknown as JavaType.Method);

        return finishDraft(draft);
    }

    override async visitFunctionType(functionType: JS.FunctionType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(functionType);
        draft.modifiers = await q.receiveListDefined(draft.modifiers, el => this.visitDefined<J.Modifier>(el, q));
        draft.constructorType = await q.receive(draft.constructorType, el => this.visitLeftPadded(el, q));
        draft.typeParameters = await q.receive(draft.typeParameters, el => this.visitDefined<J.TypeParameters>(el, q));
        draft.parameters = await q.receive(draft.parameters, el => this.visitContainer(el, q));
        draft.returnType = await q.receive(draft.returnType, el => this.visitLeftPadded(el, q));
        return finishDraft(draft);
    }

    override async visitInferType(inferType: JS.InferType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(inferType);
        draft.typeParameter = await q.receive(draft.typeParameter, el => this.visitLeftPadded(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitImportType(importType: JS.ImportType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(importType);
        draft.hasTypeof = await q.receive(draft.hasTypeof, el => this.visitRightPadded(el, q));
        draft.argumentAndAttributes = await q.receive(draft.argumentAndAttributes, el => this.visitContainer(el, q));
        draft.qualifier = await q.receive(draft.qualifier, el => this.visitLeftPadded(el, q));
        draft.typeArguments = await q.receive(draft.typeArguments, el => this.visitContainer(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitImportDeclaration(jsImport: JS.Import, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(jsImport);
        draft.modifiers = await q.receiveListDefined(draft.modifiers, el => this.visitDefined<J.Modifier>(el, q));
        draft.importClause = await q.receive(draft.importClause, el => this.visitDefined<JS.ImportClause>(el, q));
        draft.moduleSpecifier = await q.receive(draft.moduleSpecifier, el => this.visitLeftPadded(el, q));
        draft.attributes = await q.receive(draft.attributes, el => this.visitDefined<JS.ImportAttributes>(el, q));
        draft.initializer = await q.receive(draft.initializer, el => this.visitLeftPadded(el, q));
        return finishDraft(draft);
    }

    override async visitImportClause(jsImportClause: JS.ImportClause, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(jsImportClause);
        draft.typeOnly = await q.receive(draft.typeOnly);
        draft.name = await q.receive(draft.name, el => this.visitRightPadded(el, q));
        draft.namedBindings = await q.receive(draft.namedBindings, el => this.visitDefined<Expression>(el, q));
        return finishDraft(draft);
    }

    override async visitNamedImports(namedImports: JS.NamedImports, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(namedImports);
        draft.elements = await q.receive(draft.elements, el => this.visitContainer(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitImportSpecifier(jsImportSpecifier: JS.ImportSpecifier, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(jsImportSpecifier);
        draft.importType = await q.receive(draft.importType, el => this.visitLeftPadded(el, q));
        draft.specifier = await q.receive(draft.specifier, el => this.visitDefined<JS.Alias | J.Identifier>(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitImportAttributes(importAttributes: JS.ImportAttributes, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(importAttributes);
        draft.token = await q.receive(draft.token);
        draft.elements = await q.receive(draft.elements, el => this.visitContainer(el, q));
        return finishDraft(draft);
    }

    override async visitImportTypeAttributes(importTypeAttributes: JS.ImportTypeAttributes, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(importTypeAttributes);
        draft.token = await q.receive(draft.token, el => this.visitRightPadded(el, q));
        draft.elements = await q.receive(draft.elements, el => this.visitContainer(el, q));
        draft.end = await q.receive(draft.end, el => this.visitSpace(el, q));
        return finishDraft(draft);
    }

    override async visitImportAttribute(importAttribute: JS.ImportAttribute, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(importAttribute);
        draft.name = await q.receive(draft.name, el => this.visitDefined<Expression>(el, q));
        draft.value = await q.receive(draft.value, el => this.visitLeftPadded(el, q));
        return finishDraft(draft);
    }

    override async visitBinaryExtensions(binary: JS.Binary, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(binary);
        draft.left = await q.receive(draft.left, el => this.visitDefined<Expression>(el, q));
        draft.operator = await q.receive(draft.operator, el => this.visitLeftPadded(el, q));
        draft.right = await q.receive(draft.right, el => this.visitDefined<Expression>(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitLiteralType(literalType: JS.LiteralType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(literalType);
        draft.literal = await q.receive(draft.literal, el => this.visitDefined<Expression>(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitMappedType(mappedType: JS.MappedType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(mappedType);
        draft.prefixToken = await q.receive(draft.prefixToken, el => this.visitLeftPadded(el, q));
        draft.hasReadonly = await q.receive(draft.hasReadonly, el => this.visitLeftPadded(el, q));
        draft.keysRemapping = await q.receive(draft.keysRemapping, el => this.visitDefined<JS.MappedType.KeysRemapping>(el, q));
        draft.suffixToken = await q.receive(draft.suffixToken, el => this.visitLeftPadded(el, q));
        draft.hasQuestionToken = await q.receive(draft.hasQuestionToken, el => this.visitLeftPadded(el, q));
        draft.valueType = await q.receive(draft.valueType, el => this.visitContainer(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitMappedTypeKeysRemapping(keysRemapping: JS.MappedType.KeysRemapping, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(keysRemapping);
        draft.typeParameter = await q.receive(draft.typeParameter, el => this.visitRightPadded(el, q));
        draft.nameType = await q.receive(draft.nameType, el => this.visitRightPadded(el, q));
        return finishDraft(draft);
    }

    override async visitMappedTypeParameter(mappedTypeParameter: JS.MappedType.Parameter, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(mappedTypeParameter);
        draft.name = await q.receive(draft.name, el => this.visitDefined<Expression>(el, q));
        draft.iterateType = await q.receive(draft.iterateType, el => this.visitLeftPadded(el, q));
        return finishDraft(draft);
    }

    override async visitObjectBindingPattern(objectBindingPattern: JS.ObjectBindingPattern, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(objectBindingPattern);
        draft.leadingAnnotations = await q.receiveListDefined(draft.leadingAnnotations, el => this.visitDefined<J.Annotation>(el, q));
        draft.modifiers = await q.receiveListDefined(draft.modifiers, el => this.visitDefined<J.Modifier>(el, q));
        draft.typeExpression = await q.receive(draft.typeExpression, el => this.visitDefined<TypeTree>(el, q));
        draft.bindings = await q.receive(draft.bindings, el => this.visitContainer(el, q));
        draft.initializer = await q.receive(draft.initializer, el => this.visitLeftPadded(el, q));
        return finishDraft(draft);
    }

    override async visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(propertyAssignment);
        draft.name = await q.receive(draft.name, el => this.visitRightPadded(el, q));
        draft.assigmentToken = await q.receive(draft.assigmentToken);
        draft.initializer = await q.receive(draft.initializer, el => this.visitDefined<Expression>(el, q));
        return finishDraft(draft);
    }

    override async visitSatisfiesExpression(satisfiesExpression: JS.SatisfiesExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(satisfiesExpression);
        draft.expression = await q.receive(draft.expression, el => this.visitDefined<J>(el, q));
        draft.satisfiesType = await q.receive(draft.satisfiesType, el => this.visitLeftPadded(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitScopedVariableDeclarations(scopedVariableDeclarations: JS.ScopedVariableDeclarations, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(scopedVariableDeclarations);
        draft.modifiers = await q.receiveListDefined(draft.modifiers, el => this.visitDefined<J.Modifier>(el, q));
        draft.variables = await q.receiveListDefined(draft.variables, el => this.visitRightPadded(el, q));
        return finishDraft(draft);
    }

    override async visitStatementExpression(statementExpression: JS.StatementExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(statementExpression);
        draft.statement = await q.receive(draft.statement, el => this.visitDefined<Statement>(el, q));
        return finishDraft(draft);
    }

    override async visitTaggedTemplateExpression(taggedTemplateExpression: JS.TaggedTemplateExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(taggedTemplateExpression);
        draft.tag = await q.receive(draft.tag, el => this.visitRightPadded(el, q));
        draft.typeArguments = await q.receive(draft.typeArguments, el => this.visitContainer(el, q));
        draft.templateExpression = await q.receive(draft.templateExpression, el => this.visitDefined<Expression>(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitTemplateExpression(templateExpression: JS.TemplateExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(templateExpression);
        draft.head = await q.receive(draft.head, el => this.visitDefined<J.Literal>(el, q));
        draft.spans = await q.receiveListDefined(draft.spans, el => this.visitRightPadded(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitTemplateExpressionSpan(span: JS.TemplateExpression.Span, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(span);
        draft.expression = await q.receive(draft.expression, el => this.visitDefined<J>(el, q));
        draft.tail = await q.receive(draft.tail, el => this.visitDefined<J.Literal>(el, q));
        return finishDraft(draft);
    }

    override async visitTuple(tuple: JS.Tuple, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(tuple);
        draft.elements = await q.receive(draft.elements, el => this.visitContainer(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitTypeDeclaration(typeDeclaration: JS.TypeDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(typeDeclaration);
        draft.modifiers = await q.receiveListDefined(draft.modifiers, el => this.visitDefined<J.Modifier>(el, q));
        draft.name = await q.receive(draft.name, el => this.visitLeftPadded(el, q));
        draft.typeParameters = await q.receive(draft.typeParameters, el => this.visitDefined<J.TypeParameters>(el, q));
        draft.initializer = await q.receive(draft.initializer, el => this.visitLeftPadded(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitTypeOf(typeOf: JS.TypeOf, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(typeOf);
        draft.expression = await q.receive(draft.expression, el => this.visitDefined<Expression>(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitTypeTreeExpression(typeTreeExpression: JS.TypeTreeExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(typeTreeExpression);
        draft.expression = await q.receive(draft.expression, el => this.visitDefined<Expression>(el, q));
        return finishDraft(draft);
    }

    override async visitAs(as_: JS.As, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(as_);
        draft.left = await q.receive(draft.left, el => this.visitRightPadded<Expression>(el, q));
        draft.right = await q.receive(draft.right, el => this.visitDefined<Expression>(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitAssignmentOperationExtensions(assignmentOperation: JS.AssignmentOperation, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(assignmentOperation);
        draft.variable = await q.receive(draft.variable, el => this.visitDefined<Expression>(el, q));
        draft.operator = await q.receive(draft.operator, el => this.visitLeftPadded(el, q));
        draft.assignment = await q.receive(draft.assignment, el => this.visitDefined<Expression>(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitIndexedAccessType(indexedAccessType: JS.IndexedAccessType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(indexedAccessType);
        draft.objectType = await q.receive(draft.objectType, el => this.visitDefined<TypeTree>(el, q));
        draft.indexType = await q.receive(draft.indexType, el => this.visitDefined<TypeTree>(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitIndexedAccessTypeIndexType(indexType: JS.IndexedAccessType.IndexType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(indexType);
        draft.element = await q.receive(draft.element, el => this.visitRightPadded(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitTypeQuery(typeQuery: JS.TypeQuery, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(typeQuery);
        draft.typeExpression = await q.receive(draft.typeExpression, el => this.visitDefined<TypeTree>(el, q));
        draft.typeArguments = await q.receive(draft.typeArguments, el => this.visitContainer(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitTypeInfo(typeInfo: JS.TypeInfo, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(typeInfo);
        draft.typeIdentifier = await q.receive(draft.typeIdentifier, el => this.visitDefined<TypeTree>(el, q));
        return finishDraft(draft);
    }

    override async visitComputedPropertyName(computedPropertyName: JS.ComputedPropertyName, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(computedPropertyName);
        draft.expression = await q.receive(draft.expression, el => this.visitRightPadded(el, q));
        return finishDraft(draft);
    }

    override async visitTypeOperator(typeOperator: JS.TypeOperator, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(typeOperator);
        draft.operator = await q.receive(draft.operator);
        draft.expression = await q.receive(draft.expression, el => this.visitLeftPadded(el, q));
        return finishDraft(draft);
    }

    override async visitTypePredicate(typePredicate: JS.TypePredicate, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(typePredicate);
        draft.asserts = await q.receive(draft.asserts, el => this.visitLeftPadded(el, q));
        draft.parameterName = await q.receive(draft.parameterName, el => this.visitDefined<J.Identifier>(el, q));
        draft.expression = await q.receive(draft.expression, el => this.visitLeftPadded(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitUnion(union: JS.Union, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(union);
        draft.types = await q.receiveListDefined(draft.types, el => this.visitRightPadded(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitIntersection(intersection: JS.Intersection, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(intersection);
        draft.types = await q.receiveListDefined(draft.types, el => this.visitRightPadded(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitVoid(void_: JS.Void, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(void_);
        draft.expression = await q.receive(draft.expression, el => this.visitDefined<Expression>(el, q));
        return finishDraft(draft);
    }

    override async visitWithStatement(withStatement: JS.WithStatement, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(withStatement);
        draft.expression = await q.receive(draft.expression, el => this.visitDefined<J.ControlParentheses<Expression>>(el, q));
        draft.body = await q.receive(draft.body, el => this.visitRightPadded(el, q));
        return finishDraft(draft);
    }

    override async visitJsxTag(tag: JSX.Tag, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(tag);
        draft.openName = await q.receive(draft.openName, el => this.visitLeftPadded(el, q));
        draft.typeArguments = await q.receive(draft.typeArguments, el => this.visitContainer(el, q));
        draft.afterName = await q.receive(draft.afterName, space => this.visitSpace(space, q));
        draft.attributes = await q.receiveListDefined(draft.attributes, attr => this.visitRightPadded(attr, q));

        draft.selfClosing = await q.receive(draft.selfClosing, space => this.visitSpace(space, q));
        draft.children = await q.receiveListDefined(draft.children, child => this.visit(child, q));
        draft.closingName = await q.receive(draft.closingName, el => this.visitLeftPadded(el, q));
        draft.afterClosingName = await q.receive(draft.afterClosingName, el => this.visitSpace(el, q));

        return finishDraft(draft);
    }

    override async visitJsxAttribute(attribute: JSX.Attribute, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(attribute);
        draft.key = await q.receive(draft.key, el => this.visitDefined<J.Identifier | JSX.NamespacedName>(el, q));
        draft.value = await q.receive(draft.value, el => this.visitLeftPadded(el, q));
        return finishDraft(draft);
    }

    override async visitJsxSpreadAttribute(spreadAttribute: JSX.SpreadAttribute, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(spreadAttribute);
        draft.dots = await q.receive(draft.dots, space => this.visitSpace(space, q));
        draft.expression = await q.receive(draft.expression, el => this.visitRightPadded(el, q));
        return finishDraft(draft);
    }

    override async visitJsxEmbeddedExpression(embeddedExpression: JSX.EmbeddedExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(embeddedExpression);
        draft.expression = await q.receive(draft.expression, el => this.visitRightPadded(el, q));
        return finishDraft(draft);
    }

    override async visitJsxNamespacedName(namespacedName: JSX.NamespacedName, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(namespacedName);
        draft.namespace = await q.receive(draft.namespace, el => this.visitDefined<J.Identifier>(el, q));
        draft.name = await q.receive(draft.name, el => this.visitLeftPadded(el, q));
        return finishDraft(draft);
    }

    override async visitIndexSignatureDeclaration(indexSignatureDeclaration: JS.IndexSignatureDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(indexSignatureDeclaration);
        draft.modifiers = await q.receiveListDefined(draft.modifiers, el => this.visitDefined<J.Modifier>(el, q));
        draft.parameters = await q.receive(draft.parameters, el => this.visitContainer(el, q));
        draft.typeExpression = await q.receive(draft.typeExpression, el => this.visitLeftPadded(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitComputedPropertyMethodDeclaration(computedPropMethod: JS.ComputedPropertyMethodDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(computedPropMethod);
        draft.leadingAnnotations = await q.receiveListDefined(draft.leadingAnnotations, el => this.visitDefined<J.Annotation>(el, q));
        draft.modifiers = await q.receiveListDefined(draft.modifiers, el => this.visitDefined<J.Modifier>(el, q));
        draft.typeParameters = await q.receive(draft.typeParameters, el => this.visitDefined<J.TypeParameters>(el, q));
        draft.returnTypeExpression = await q.receive(draft.returnTypeExpression, el => this.visitDefined<TypeTree>(el, q));
        draft.name = await q.receive(draft.name, el => this.visitDefined<ComputedPropertyName>(el, q));
        draft.parameters = await q.receive(draft.parameters, el => this.visitContainer(el, q));
        draft.body = await q.receive(draft.body, el => this.visitDefined<J.Block>(el, q));
        draft.methodType = await q.receive(draft.methodType, el => this.visitType(el, q) as any as JavaType.Method);
        return finishDraft(draft);
    }

    override async visitForOfLoop(forOfLoop: JS.ForOfLoop, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(forOfLoop);
        draft.await = await q.receive(draft.await, space => this.visitSpace(space, q));
        draft.loop = await q.receive(draft.loop, el => this.visitDefined<J.ForEachLoop>(el, q));
        return finishDraft(draft);
    }

    override async visitForInLoop(forInLoop: JS.ForInLoop, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(forInLoop);
        draft.control = await q.receive(draft.control, el => this.visitDefined<JS.ForInLoop.Control>(el, q));
        draft.body = await q.receive(draft.body, el => this.visitRightPadded(el, q));
        return finishDraft(draft);
    }

    override async visitNamespaceDeclaration(namespaceDeclaration: JS.NamespaceDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(namespaceDeclaration);
        draft.modifiers = await q.receiveListDefined(draft.modifiers, el => this.visitDefined<J.Modifier>(el, q));
        draft.keywordType = await q.receive(draft.keywordType, el => this.visitLeftPadded(el, q));
        draft.name = await q.receive(draft.name, el => this.visitRightPadded(el, q));
        draft.body = await q.receive(draft.body, el => this.visitDefined<J.Block>(el, q));
        return finishDraft(draft);
    }

    override async visitTypeLiteral(typeLiteral: JS.TypeLiteral, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(typeLiteral);
        draft.members = await q.receive(draft.members, el => this.visitDefined<J.Block>(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitArrayBindingPattern(arrayBindingPattern: JS.ArrayBindingPattern, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(arrayBindingPattern);
        draft.elements = await q.receive(draft.elements, el => this.visitContainer(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitBindingElement(bindingElement: JS.BindingElement, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(bindingElement);
        draft.propertyName = await q.receive(draft.propertyName, el => this.visitRightPadded(el, q));
        draft.name = await q.receive(draft.name, el => this.visitDefined<TypedTree>(el, q));
        draft.initializer = await q.receive(draft.initializer, el => this.visitLeftPadded(el, q));
        draft.variableType = await q.receive(draft.variableType, el => this.visitType(el, q) as any as JavaType.Variable);
        return finishDraft(draft);
    }

    override async visitExportDeclaration(exportDeclaration: JS.ExportDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(exportDeclaration);
        draft.modifiers = await q.receiveListDefined(draft.modifiers, el => this.visitDefined<J.Modifier>(el, q));
        draft.typeOnly = await q.receive(draft.typeOnly, el => this.visitLeftPadded(el, q));
        draft.exportClause = await q.receive(draft.exportClause, el => this.visitDefined<Expression>(el, q));
        draft.moduleSpecifier = await q.receive(draft.moduleSpecifier, el => this.visitLeftPadded(el, q));
        draft.attributes = await q.receive(draft.attributes, el => this.visitDefined<JS.ImportAttributes>(el, q));
        return finishDraft(draft);
    }

    override async visitExportAssignment(exportAssignment: JS.ExportAssignment, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(exportAssignment);
        draft.exportEquals = await q.receive(draft.exportEquals);
        draft.expression = await q.receive(draft.expression, el => this.visitLeftPadded(el, q));
        return finishDraft(draft);
    }

    override async visitNamedExports(namedExports: JS.NamedExports, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(namedExports);
        draft.elements = await q.receive(draft.elements, el => this.visitContainer(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitExportSpecifier(exportSpecifier: JS.ExportSpecifier, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(exportSpecifier);
        draft.typeOnly = await q.receive(draft.typeOnly, el => this.visitLeftPadded(el, q));
        draft.specifier = await q.receive(draft.specifier, el => this.visitDefined<Expression>(el, q));
        draft.type = await q.receive(draft.type, el => this.visitType(el, q));
        return finishDraft(draft);
    }

    override async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, q: RpcReceiveQueue): Promise<J.RightPadded<T>> {
        return this.javaReceiverDelegate.visitRightPadded(right, q)
    }

    protected async visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, q: RpcReceiveQueue): Promise<J.LeftPadded<T>> {
        return this.javaReceiverDelegate.visitLeftPadded(left, q);
    }

    protected async visitContainer<T extends J>(container: J.Container<T>, q: RpcReceiveQueue): Promise<J.Container<T>> {
        return this.javaReceiverDelegate.visitContainer(container, q);
    }

    override async visitSpace(space: J.Space, q: RpcReceiveQueue): Promise<J.Space> {
        return this.javaReceiverDelegate.visitSpace(space, q);
    }

    override async visitType(javaType: JavaType | undefined, q: RpcReceiveQueue): Promise<JavaType | undefined> {
        return this.javaReceiverDelegate.visitType(javaType, q);
    }
}

class JavaScriptReceiverDelegate extends JavaReceiver {
    private javascriptReceiver: JavaScriptReceiver;

    constructor(javascriptReceiver: JavaScriptReceiver) {
        super();
        this.javascriptReceiver = javascriptReceiver;
    }

    async visit<R extends J>(tree: Tree, p: RpcReceiveQueue, parent?: Cursor): Promise<R | undefined> {
        if (isJavaScript(tree)) {
            return this.javascriptReceiver.visit(tree, p, parent);
        }
        return super.visit(tree, p, parent);
    }
}

const javaScriptCodec: RpcCodec<JS> = {
    async rpcReceive(before: JS, q: RpcReceiveQueue): Promise<JS> {
        return (await new JavaScriptReceiver().visit(before, q))! as JS;
    },

    async rpcSend(after: JS, q: RpcSendQueue): Promise<void> {
        await new JavaScriptSender().visit(after, q);
    }
}

// Register codec for all JavaScript AST node types
Object.values(JS.Kind).forEach(kind => {
    if (!Object.values(TreeKind).includes(kind as any)) {
        RpcCodecs.registerCodec(kind, javaScriptCodec);
    }
});
