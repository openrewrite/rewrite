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
import {asRef, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {isJavaScript, JS, JSX} from "./tree";
import {Expression, J, Statement, Type, TypedTree, TypeTree} from "../java";
import {JavaReceiver, JavaSender, registerJLanguageCodecs} from "../java/rpc";
import {Cursor, Tree} from "../tree";
import {updateIfChanged} from "../util";
import ComputedPropertyName = JS.ComputedPropertyName;

class JavaScriptSender extends JavaScriptVisitor<RpcSendQueue> {
    private delegate: JavaScriptDelegateSender;

    constructor() {
        super();
        this.delegate = new JavaScriptDelegateSender(this);
    }

    override async visit<R extends J>(tree: Tree, p: RpcSendQueue, parent?: Cursor): Promise<R | undefined> {
        if (isJavaScript(tree)) {
            return super.visit(tree, p, parent);
        }

        return this.delegate.visit(tree, p, parent);
    }

    override async preVisit(j: JS, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(j, j2 => j2.id);
        await q.getAndSend(j, j2 => j2.prefix, space => this.visitSpace(space, q));
        await q.getAndSend(j, j2 => j2.markers);
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
        await q.getAndSend(functionCall, m => asRef(m.methodType), type => this.visitType(type, q));
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

    override async visitShebang(shebang: JS.Shebang, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(shebang, el => el.text);
        return shebang;
    }

    override async visitSpread(spread: JS.Spread, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(spread, el => el.expression, el => this.visit(el, q));
        await q.getAndSend(spread, el => asRef(el.type), el => this.visitType(el, q));
        return spread;
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
        return this.delegate.visitRightPadded(right, q);
    }

    override async visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, q: RpcSendQueue): Promise<J.LeftPadded<T>> {
        return this.delegate.visitLeftPadded(left, q);
    }

    override async visitContainer<T extends J>(container: J.Container<T>, q: RpcSendQueue): Promise<J.Container<T>> {
        return this.delegate.visitContainer(container, q);
    }

    override async visitSpace(space: J.Space, q: RpcSendQueue): Promise<J.Space> {
        return this.delegate.visitSpace(space, q);
    }

    override async visitType(javaType: Type | undefined, q: RpcSendQueue): Promise<Type | undefined> {
        return this.delegate.visitType(javaType, q);
    }
}

class JavaScriptDelegateSender extends JavaSender {
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

class JavaScriptReceiver extends JavaReceiver {
    public override visit<T extends J>(j: T | undefined, q: RpcReceiveQueue): T | undefined {
        if (!j) return undefined;

        // If it's a JavaScript node, dispatch to JS-specific handlers
        if (isJavaScript(j)) {
            let result = this.preVisit(j as JS, q);
            if (result === undefined) return undefined;

            result = this.visitJavaScriptNode(result as JS, q);
            return result as T | undefined;
        }

        // Otherwise delegate to Java visitor
        return super.visit(j, q);
    }

    // Helper to visit JS nodes - calls specific visitor method based on kind
    protected visitJavaScriptNode(js: JS, q: RpcReceiveQueue): J | undefined {
        switch (js.kind) {
            case JS.Kind.CompilationUnit:
                return this.visitJsCompilationUnit(js as JS.CompilationUnit, q);
            case JS.Kind.Alias:
                return this.visitAlias(js as JS.Alias, q);
            case JS.Kind.ArrowFunction:
                return this.visitArrowFunction(js as JS.ArrowFunction, q);
            case JS.Kind.As:
                return this.visitAs(js as JS.As, q);
            case JS.Kind.AssignmentOperation:
                return this.visitAssignmentOperationExtensions(js as JS.AssignmentOperation, q);
            case JS.Kind.Await:
                return this.visitAwait(js as JS.Await, q);
            case JS.Kind.Binary:
                return this.visitBinaryExtensions(js as JS.Binary, q);
            case JS.Kind.BindingElement:
                return this.visitBindingElement(js as JS.BindingElement, q);
            case JS.Kind.ComputedPropertyMethodDeclaration:
                return this.visitComputedPropertyMethodDeclaration(js as JS.ComputedPropertyMethodDeclaration, q);
            case JS.Kind.ComputedPropertyName:
                return this.visitComputedPropertyName(js as JS.ComputedPropertyName, q);
            case JS.Kind.ConditionalType:
                return this.visitConditionalType(js as JS.ConditionalType, q);
            case JS.Kind.Delete:
                return this.visitDelete(js as JS.Delete, q);
            case JS.Kind.Export:
            case JS.Kind.ExportDeclaration:
                return this.visitExportDeclaration(js as JS.ExportDeclaration, q);
            case JS.Kind.ExportAssignment:
                return this.visitExportAssignment(js as JS.ExportAssignment, q);
            case JS.Kind.ExportSpecifier:
                return this.visitExportSpecifier(js as JS.ExportSpecifier, q);
            case JS.Kind.ExpressionStatement:
                return this.visitExpressionStatement(js as JS.ExpressionStatement, q);
            case JS.Kind.ExpressionWithTypeArguments:
                return this.visitExpressionWithTypeArguments(js as JS.ExpressionWithTypeArguments, q);
            case JS.Kind.ForInLoop:
                return this.visitForInLoop(js as JS.ForInLoop, q);
            case JS.Kind.ForOfLoop:
                return this.visitForOfLoop(js as JS.ForOfLoop, q);
            case JS.Kind.FunctionCall:
                return this.visitFunctionCall(js as JS.FunctionCall, q);
            case JS.Kind.FunctionType:
                return this.visitFunctionType(js as JS.FunctionType, q);
            case JS.Kind.Import:
                return this.visitImportDeclaration(js as JS.Import, q);
            case JS.Kind.ImportAttribute:
                return this.visitImportAttribute(js as JS.ImportAttribute, q);
            case JS.Kind.ImportAttributes:
                return this.visitImportAttributes(js as JS.ImportAttributes, q);
            case JS.Kind.ImportClause:
                return this.visitImportClause(js as JS.ImportClause, q);
            case JS.Kind.ImportSpecifier:
                return this.visitImportSpecifier(js as JS.ImportSpecifier, q);
            case JS.Kind.ImportType:
                return this.visitImportType(js as JS.ImportType, q);
            case JS.Kind.ImportTypeAttributes:
                return this.visitImportTypeAttributes(js as JS.ImportTypeAttributes, q);
            case JS.Kind.IndexedAccessType:
                return this.visitIndexedAccessType(js as JS.IndexedAccessType, q);
            case JS.Kind.IndexedAccessTypeIndexType:
                return this.visitIndexedAccessTypeIndexType(js as JS.IndexedAccessType.IndexType, q);
            case JS.Kind.IndexSignatureDeclaration:
                return this.visitIndexSignatureDeclaration(js as JS.IndexSignatureDeclaration, q);
            case JS.Kind.InferType:
                return this.visitInferType(js as JS.InferType, q);
            case JS.Kind.Intersection:
                return this.visitIntersection(js as JS.Intersection, q);
            case JS.Kind.JsxAttribute:
                return this.visitJsxAttribute(js as JSX.Attribute, q);
            case JS.Kind.JsxEmbeddedExpression:
                return this.visitJsxEmbeddedExpression(js as JSX.EmbeddedExpression, q);
            case JS.Kind.JsxNamespacedName:
                return this.visitJsxNamespacedName(js as JSX.NamespacedName, q);
            case JS.Kind.JsxSpreadAttribute:
                return this.visitJsxSpreadAttribute(js as JSX.SpreadAttribute, q);
            case JS.Kind.JsxTag:
                return this.visitJsxTag(js as JSX.Tag, q);
            case JS.Kind.LiteralType:
                return this.visitLiteralType(js as JS.LiteralType, q);
            case JS.Kind.MappedType:
                return this.visitMappedType(js as JS.MappedType, q);
            case JS.Kind.MappedTypeKeysRemapping:
                return this.visitMappedTypeKeysRemapping(js as JS.MappedType.KeysRemapping, q);
            case JS.Kind.MappedTypeParameter:
                return this.visitMappedTypeParameter(js as JS.MappedType.Parameter, q);
            case JS.Kind.NamedExports:
                return this.visitNamedExports(js as JS.NamedExports, q);
            case JS.Kind.NamedImports:
                return this.visitNamedImports(js as JS.NamedImports, q);
            case JS.Kind.NamespaceDeclaration:
                return this.visitNamespaceDeclaration(js as JS.NamespaceDeclaration, q);
            case JS.Kind.ObjectBindingPattern:
                return this.visitObjectBindingPattern(js as JS.ObjectBindingPattern, q);
            case JS.Kind.PropertyAssignment:
                return this.visitPropertyAssignment(js as JS.PropertyAssignment, q);
            case JS.Kind.SatisfiesExpression:
                return this.visitSatisfiesExpression(js as JS.SatisfiesExpression, q);
            case JS.Kind.ScopedVariableDeclarations:
                return this.visitScopedVariableDeclarations(js as JS.ScopedVariableDeclarations, q);
            case JS.Kind.Shebang:
                return this.visitShebang(js as JS.Shebang, q);
            case JS.Kind.Spread:
                return this.visitSpread(js as JS.Spread, q);
            case JS.Kind.StatementExpression:
                return this.visitStatementExpression(js as JS.StatementExpression, q);
            case JS.Kind.TaggedTemplateExpression:
                return this.visitTaggedTemplateExpression(js as JS.TaggedTemplateExpression, q);
            case JS.Kind.TemplateExpression:
                return this.visitTemplateExpression(js as JS.TemplateExpression, q);
            case JS.Kind.TemplateExpressionSpan:
                return this.visitTemplateExpressionSpan(js as JS.TemplateExpression.Span, q);
            case JS.Kind.Tuple:
                return this.visitTuple(js as JS.Tuple, q);
            case JS.Kind.TypeDeclaration:
                return this.visitTypeDeclaration(js as JS.TypeDeclaration, q);
            case JS.Kind.TypeInfo:
                return this.visitTypeInfo(js as JS.TypeInfo, q);
            case JS.Kind.TypeLiteral:
                return this.visitTypeLiteral(js as JS.TypeLiteral, q);
            case JS.Kind.TypeOf:
                return this.visitTypeOf(js as JS.TypeOf, q);
            case JS.Kind.TypeOperator:
                return this.visitTypeOperator(js as JS.TypeOperator, q);
            case JS.Kind.TypePredicate:
                return this.visitTypePredicate(js as JS.TypePredicate, q);
            case JS.Kind.TypeQuery:
                return this.visitTypeQuery(js as JS.TypeQuery, q);
            case JS.Kind.TypeTreeExpression:
                return this.visitTypeTreeExpression(js as JS.TypeTreeExpression, q);
            case JS.Kind.Union:
                return this.visitUnion(js as JS.Union, q);
            case JS.Kind.Void:
                return this.visitVoid(js as JS.Void, q);
            case JS.Kind.WithStatement:
                return this.visitWithStatement(js as JS.WithStatement, q);
            case JS.Kind.ArrayBindingPattern:
                return this.visitArrayBindingPattern(js as JS.ArrayBindingPattern, q);
            default:
                // Unknown JS kind - return as-is
                return js;
        }
    }

    // Helper method - same as visit but asserts the result is defined
    protected visitDefined<T extends J>(j: T, q: RpcReceiveQueue): T {
        return this.visit(j, q)!;
    }

    protected preVisit(j: JS, q: RpcReceiveQueue): J | undefined {
        // inlined `updateIfChanged()` for performance
        const id = q.receive(j.id)!;
        const prefix = q.receive(j.prefix, space => this.visitSpace(space, q))!;
        const markers = q.receive(j.markers)!;
        return id === j.id && prefix === j.prefix && markers === j.markers
            ? j
            : { ...j, id, prefix, markers };
    }

    visitJsCompilationUnit(cu: JS.CompilationUnit, q: RpcReceiveQueue): J | undefined {
        const updates = {
            sourcePath: q.receive(cu.sourcePath),
            charsetName: q.receive(cu.charsetName),
            charsetBomMarked: q.receive(cu.charsetBomMarked),
            checksum: q.receive(cu.checksum),
            fileAttributes: q.receive(cu.fileAttributes),
            statements: q.receiveListDefined(cu.statements, stmt => this.visitRightPadded(stmt, q)),
            eof: q.receive(cu.eof, space => this.visitSpace(space, q))
        };
        return updateIfChanged(cu, updates);
    }

    visitAlias(alias: JS.Alias, q: RpcReceiveQueue): J | undefined {
        const updates = {
            propertyName: q.receive(alias.propertyName, el => this.visitRightPadded(el, q)),
            alias: q.receive(alias.alias, el => this.visitDefined<Expression>(el, q))
        };
        return updateIfChanged(alias, updates);
    }

    visitArrowFunction(arrowFunction: JS.ArrowFunction, q: RpcReceiveQueue): J | undefined {
        const updates = {
            leadingAnnotations: q.receiveListDefined(arrowFunction.leadingAnnotations, el => this.visitDefined<J.Annotation>(el, q)),
            modifiers: q.receiveListDefined(arrowFunction.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            typeParameters: q.receive(arrowFunction.typeParameters, el => this.visitDefined<J.TypeParameters>(el, q)),
            lambda: q.receive(arrowFunction.lambda, el => this.visitDefined<J.Lambda>(el, q)),
            returnTypeExpression: q.receive(arrowFunction.returnTypeExpression, el => this.visitDefined<TypeTree>(el, q))
        };
        return updateIfChanged(arrowFunction, updates);
    }

    visitAwait(anAwait: JS.Await, q: RpcReceiveQueue): J | undefined {
        const updates = {
            expression: q.receive(anAwait.expression, el => this.visitDefined<Expression>(el, q)),
            type: q.receive(anAwait.type, el => this.visitType(el, q))
        };
        return updateIfChanged(anAwait, updates);
    }

    visitConditionalType(conditionalType: JS.ConditionalType, q: RpcReceiveQueue): J | undefined {
        const updates = {
            checkType: q.receive(conditionalType.checkType, el => this.visitDefined<Expression>(el, q)),
            condition: q.receive(conditionalType.condition, el => this.visitLeftPadded(el, q)),
            type: q.receive(conditionalType.type, el => this.visitType(el, q))
        };
        return updateIfChanged(conditionalType, updates);
    }

    visitDelete(delete_: JS.Delete, q: RpcReceiveQueue): J | undefined {
        const updates = {
            expression: q.receive(delete_.expression, el => this.visitDefined<Expression>(el, q))
        };
        return updateIfChanged(delete_, updates);
    }

    visitExpressionStatement(expressionStatement: JS.ExpressionStatement, q: RpcReceiveQueue): J | undefined {
        const updates = {
            expression: q.receive(expressionStatement.expression, el => this.visitDefined<Expression>(el, q))
        };
        return updateIfChanged(expressionStatement, updates);
    }

    visitExpressionWithTypeArguments(expressionWithTypeArguments: JS.ExpressionWithTypeArguments, q: RpcReceiveQueue): J | undefined {
        const updates = {
            clazz: q.receive(expressionWithTypeArguments.clazz, el => this.visitDefined<J>(el, q)),
            typeArguments: q.receive(expressionWithTypeArguments.typeArguments, el => this.visitContainer(el, q)),
            type: q.receive(expressionWithTypeArguments.type, el => this.visitType(el, q))
        };
        return updateIfChanged(expressionWithTypeArguments, updates);
    }

    visitFunctionCall(functionCall: JS.FunctionCall, q: RpcReceiveQueue): J | undefined {
        const updates = {
            function: q.receive(functionCall.function, select => this.visitRightPadded(select, q)),
            typeParameters: q.receive(functionCall.typeParameters, typeParams => this.visitContainer(typeParams, q)),
            arguments: q.receive(functionCall.arguments, args => this.visitContainer(args, q)),
            methodType: q.receive(functionCall.methodType, type => this.visitType(type, q) as unknown as Type.Method)
        };
        return updateIfChanged(functionCall, updates);
    }

    visitFunctionType(functionType: JS.FunctionType, q: RpcReceiveQueue): J | undefined {
        const updates = {
            modifiers: q.receiveListDefined(functionType.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            constructorType: q.receive(functionType.constructorType, el => this.visitLeftPadded(el, q)),
            typeParameters: q.receive(functionType.typeParameters, el => this.visitDefined<J.TypeParameters>(el, q)),
            parameters: q.receive(functionType.parameters, el => this.visitContainer(el, q)),
            returnType: q.receive(functionType.returnType, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(functionType, updates);
    }

    visitInferType(inferType: JS.InferType, q: RpcReceiveQueue): J | undefined {
        const updates = {
            typeParameter: q.receive(inferType.typeParameter, el => this.visitLeftPadded(el, q)),
            type: q.receive(inferType.type, el => this.visitType(el, q))
        };
        return updateIfChanged(inferType, updates);
    }

    visitImportType(importType: JS.ImportType, q: RpcReceiveQueue): J | undefined {
        const updates = {
            hasTypeof: q.receive(importType.hasTypeof, el => this.visitRightPadded(el, q)),
            argumentAndAttributes: q.receive(importType.argumentAndAttributes, el => this.visitContainer(el, q)),
            qualifier: q.receive(importType.qualifier, el => this.visitLeftPadded(el, q)),
            typeArguments: q.receive(importType.typeArguments, el => this.visitContainer(el, q)),
            type: q.receive(importType.type, el => this.visitType(el, q))
        };
        return updateIfChanged(importType, updates);
    }

    visitImportDeclaration(jsImport: JS.Import, q: RpcReceiveQueue): J | undefined {
        const updates = {
            modifiers: q.receiveListDefined(jsImport.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            importClause: q.receive(jsImport.importClause, el => this.visitDefined<JS.ImportClause>(el, q)),
            moduleSpecifier: q.receive(jsImport.moduleSpecifier, el => this.visitLeftPadded(el, q)),
            attributes: q.receive(jsImport.attributes, el => this.visitDefined<JS.ImportAttributes>(el, q)),
            initializer: q.receive(jsImport.initializer, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(jsImport, updates);
    }

    visitImportClause(jsImportClause: JS.ImportClause, q: RpcReceiveQueue): J | undefined {
        const updates = {
            typeOnly: q.receive(jsImportClause.typeOnly),
            name: q.receive(jsImportClause.name, el => this.visitRightPadded(el, q)),
            namedBindings: q.receive(jsImportClause.namedBindings, el => this.visitDefined<Expression>(el, q))
        };
        return updateIfChanged(jsImportClause, updates);
    }

    visitNamedImports(namedImports: JS.NamedImports, q: RpcReceiveQueue): J | undefined {
        const updates = {
            elements: q.receive(namedImports.elements, el => this.visitContainer(el, q)),
            type: q.receive(namedImports.type, el => this.visitType(el, q))
        };
        return updateIfChanged(namedImports, updates);
    }

    visitImportSpecifier(jsImportSpecifier: JS.ImportSpecifier, q: RpcReceiveQueue): J | undefined {
        const updates = {
            importType: q.receive(jsImportSpecifier.importType, el => this.visitLeftPadded(el, q)),
            specifier: q.receive(jsImportSpecifier.specifier, el => this.visitDefined<JS.Alias | J.Identifier>(el, q)),
            type: q.receive(jsImportSpecifier.type, el => this.visitType(el, q))
        };
        return updateIfChanged(jsImportSpecifier, updates);
    }

    visitImportAttributes(importAttributes: JS.ImportAttributes, q: RpcReceiveQueue): J | undefined {
        const updates = {
            token: q.receive(importAttributes.token),
            elements: q.receive(importAttributes.elements, el => this.visitContainer(el, q))
        };
        return updateIfChanged(importAttributes, updates);
    }

    visitImportTypeAttributes(importTypeAttributes: JS.ImportTypeAttributes, q: RpcReceiveQueue): J | undefined {
        const updates = {
            token: q.receive(importTypeAttributes.token, el => this.visitRightPadded(el, q)),
            elements: q.receive(importTypeAttributes.elements, el => this.visitContainer(el, q)),
            end: q.receive(importTypeAttributes.end, el => this.visitSpace(el, q))
        };
        return updateIfChanged(importTypeAttributes, updates);
    }

    visitImportAttribute(importAttribute: JS.ImportAttribute, q: RpcReceiveQueue): J | undefined {
        const updates = {
            name: q.receive(importAttribute.name, el => this.visitDefined<Expression>(el, q)),
            value: q.receive(importAttribute.value, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(importAttribute, updates);
    }

    visitBinaryExtensions(binary: JS.Binary, q: RpcReceiveQueue): J | undefined {
        const updates = {
            left: q.receive(binary.left, el => this.visitDefined<Expression>(el, q)),
            operator: q.receive(binary.operator, el => this.visitLeftPadded(el, q)),
            right: q.receive(binary.right, el => this.visitDefined<Expression>(el, q)),
            type: q.receive(binary.type, el => this.visitType(el, q))
        };
        return updateIfChanged(binary, updates);
    }

    visitLiteralType(literalType: JS.LiteralType, q: RpcReceiveQueue): J | undefined {
        const updates = {
            literal: q.receive(literalType.literal, el => this.visitDefined<Expression>(el, q)),
            type: q.receive(literalType.type, el => this.visitType(el, q))
        };
        return updateIfChanged(literalType, updates);
    }

    visitMappedType(mappedType: JS.MappedType, q: RpcReceiveQueue): J | undefined {
        const updates = {
            prefixToken: q.receive(mappedType.prefixToken, el => this.visitLeftPadded(el, q)),
            hasReadonly: q.receive(mappedType.hasReadonly, el => this.visitLeftPadded(el, q)),
            keysRemapping: q.receive(mappedType.keysRemapping, el => this.visitDefined<JS.MappedType.KeysRemapping>(el, q)),
            suffixToken: q.receive(mappedType.suffixToken, el => this.visitLeftPadded(el, q)),
            hasQuestionToken: q.receive(mappedType.hasQuestionToken, el => this.visitLeftPadded(el, q)),
            valueType: q.receive(mappedType.valueType, el => this.visitContainer(el, q)),
            type: q.receive(mappedType.type, el => this.visitType(el, q))
        };
        return updateIfChanged(mappedType, updates);
    }

    visitMappedTypeKeysRemapping(keysRemapping: JS.MappedType.KeysRemapping, q: RpcReceiveQueue): J | undefined {
        const updates = {
            typeParameter: q.receive(keysRemapping.typeParameter, el => this.visitRightPadded(el, q)),
            nameType: q.receive(keysRemapping.nameType, el => this.visitRightPadded(el, q))
        };
        return updateIfChanged(keysRemapping, updates);
    }

    visitMappedTypeParameter(mappedTypeParameter: JS.MappedType.Parameter, q: RpcReceiveQueue): J | undefined {
        const updates = {
            name: q.receive(mappedTypeParameter.name, el => this.visitDefined<Expression>(el, q)),
            iterateType: q.receive(mappedTypeParameter.iterateType, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(mappedTypeParameter, updates);
    }

    visitObjectBindingPattern(objectBindingPattern: JS.ObjectBindingPattern, q: RpcReceiveQueue): J | undefined {
        const updates = {
            leadingAnnotations: q.receiveListDefined(objectBindingPattern.leadingAnnotations, el => this.visitDefined<J.Annotation>(el, q)),
            modifiers: q.receiveListDefined(objectBindingPattern.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            typeExpression: q.receive(objectBindingPattern.typeExpression, el => this.visitDefined<TypeTree>(el, q)),
            bindings: q.receive(objectBindingPattern.bindings, el => this.visitContainer(el, q)),
            initializer: q.receive(objectBindingPattern.initializer, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(objectBindingPattern, updates);
    }

    visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, q: RpcReceiveQueue): J | undefined {
        const updates = {
            name: q.receive(propertyAssignment.name, el => this.visitRightPadded(el, q)),
            assigmentToken: q.receive(propertyAssignment.assigmentToken),
            initializer: q.receive(propertyAssignment.initializer, el => this.visitDefined<Expression>(el, q))
        };
        return updateIfChanged(propertyAssignment, updates);
    }

    visitSatisfiesExpression(satisfiesExpression: JS.SatisfiesExpression, q: RpcReceiveQueue): J | undefined {
        const updates = {
            expression: q.receive(satisfiesExpression.expression, el => this.visitDefined<J>(el, q)),
            satisfiesType: q.receive(satisfiesExpression.satisfiesType, el => this.visitLeftPadded(el, q)),
            type: q.receive(satisfiesExpression.type, el => this.visitType(el, q))
        };
        return updateIfChanged(satisfiesExpression, updates);
    }

    visitScopedVariableDeclarations(scopedVariableDeclarations: JS.ScopedVariableDeclarations, q: RpcReceiveQueue): J | undefined {
        const updates = {
            modifiers: q.receiveListDefined(scopedVariableDeclarations.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            variables: q.receiveListDefined(scopedVariableDeclarations.variables, el => this.visitRightPadded(el, q))
        };
        return updateIfChanged(scopedVariableDeclarations, updates);
    }

    visitShebang(shebang: JS.Shebang, q: RpcReceiveQueue): J | undefined {
        const updates = {
            text: q.receive(shebang.text)
        };
        return updateIfChanged(shebang, updates);
    }

    visitSpread(spread: JS.Spread, q: RpcReceiveQueue): J | undefined {
        const updates = {
            expression: q.receive(spread.expression, el => this.visitDefined<Expression>(el, q)),
            type: q.receive(spread.type, el => this.visitType(el, q))
        };
        return updateIfChanged(spread, updates);
    }

    visitStatementExpression(statementExpression: JS.StatementExpression, q: RpcReceiveQueue): J | undefined {
        const updates = {
            statement: q.receive(statementExpression.statement, el => this.visitDefined<Statement>(el, q))
        };
        return updateIfChanged(statementExpression, updates);
    }

    visitTaggedTemplateExpression(taggedTemplateExpression: JS.TaggedTemplateExpression, q: RpcReceiveQueue): J | undefined {
        const updates = {
            tag: q.receive(taggedTemplateExpression.tag, el => this.visitRightPadded(el, q)),
            typeArguments: q.receive(taggedTemplateExpression.typeArguments, el => this.visitContainer(el, q)),
            templateExpression: q.receive(taggedTemplateExpression.templateExpression, el => this.visitDefined<Expression>(el, q)),
            type: q.receive(taggedTemplateExpression.type, el => this.visitType(el, q))
        };
        return updateIfChanged(taggedTemplateExpression, updates);
    }

    visitTemplateExpression(templateExpression: JS.TemplateExpression, q: RpcReceiveQueue): J | undefined {
        const updates = {
            head: q.receive(templateExpression.head, el => this.visitDefined<J.Literal>(el, q)),
            spans: q.receiveListDefined(templateExpression.spans, el => this.visitRightPadded(el, q)),
            type: q.receive(templateExpression.type, el => this.visitType(el, q))
        };
        return updateIfChanged(templateExpression, updates);
    }

    visitTemplateExpressionSpan(span: JS.TemplateExpression.Span, q: RpcReceiveQueue): J | undefined {
        const updates = {
            expression: q.receive(span.expression, el => this.visitDefined<J>(el, q)),
            tail: q.receive(span.tail, el => this.visitDefined<J.Literal>(el, q))
        };
        return updateIfChanged(span, updates);
    }

    visitTuple(tuple: JS.Tuple, q: RpcReceiveQueue): J | undefined {
        const updates = {
            elements: q.receive(tuple.elements, el => this.visitContainer(el, q)),
            type: q.receive(tuple.type, el => this.visitType(el, q))
        };
        return updateIfChanged(tuple, updates);
    }

    visitTypeDeclaration(typeDeclaration: JS.TypeDeclaration, q: RpcReceiveQueue): J | undefined {
        const updates = {
            modifiers: q.receiveListDefined(typeDeclaration.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            name: q.receive(typeDeclaration.name, el => this.visitLeftPadded(el, q)),
            typeParameters: q.receive(typeDeclaration.typeParameters, el => this.visitDefined<J.TypeParameters>(el, q)),
            initializer: q.receive(typeDeclaration.initializer, el => this.visitLeftPadded(el, q)),
            type: q.receive(typeDeclaration.type, el => this.visitType(el, q))
        };
        return updateIfChanged(typeDeclaration, updates);
    }

    visitTypeOf(typeOf: JS.TypeOf, q: RpcReceiveQueue): J | undefined {
        const updates = {
            expression: q.receive(typeOf.expression, el => this.visitDefined<Expression>(el, q)),
            type: q.receive(typeOf.type, el => this.visitType(el, q))
        };
        return updateIfChanged(typeOf, updates);
    }

    visitTypeTreeExpression(typeTreeExpression: JS.TypeTreeExpression, q: RpcReceiveQueue): J | undefined {
        const updates = {
            expression: q.receive(typeTreeExpression.expression, el => this.visitDefined<Expression>(el, q))
        };
        return updateIfChanged(typeTreeExpression, updates);
    }

    visitAs(as_: JS.As, q: RpcReceiveQueue): J | undefined {
        const updates = {
            left: q.receive(as_.left, el => this.visitRightPadded<Expression>(el, q)),
            right: q.receive(as_.right, el => this.visitDefined<Expression>(el, q)),
            type: q.receive(as_.type, el => this.visitType(el, q))
        };
        return updateIfChanged(as_, updates);
    }

    visitAssignmentOperationExtensions(assignmentOperation: JS.AssignmentOperation, q: RpcReceiveQueue): J | undefined {
        const updates = {
            variable: q.receive(assignmentOperation.variable, el => this.visitDefined<Expression>(el, q)),
            operator: q.receive(assignmentOperation.operator, el => this.visitLeftPadded(el, q)),
            assignment: q.receive(assignmentOperation.assignment, el => this.visitDefined<Expression>(el, q)),
            type: q.receive(assignmentOperation.type, el => this.visitType(el, q))
        };
        return updateIfChanged(assignmentOperation, updates);
    }

    visitIndexedAccessType(indexedAccessType: JS.IndexedAccessType, q: RpcReceiveQueue): J | undefined {
        const updates = {
            objectType: q.receive(indexedAccessType.objectType, el => this.visitDefined<TypeTree>(el, q)),
            indexType: q.receive(indexedAccessType.indexType, el => this.visitDefined<TypeTree>(el, q)),
            type: q.receive(indexedAccessType.type, el => this.visitType(el, q))
        };
        return updateIfChanged(indexedAccessType, updates);
    }

    visitIndexedAccessTypeIndexType(indexType: JS.IndexedAccessType.IndexType, q: RpcReceiveQueue): J | undefined {
        const updates = {
            element: q.receive(indexType.element, el => this.visitRightPadded(el, q)),
            type: q.receive(indexType.type, el => this.visitType(el, q))
        };
        return updateIfChanged(indexType, updates);
    }

    visitTypeQuery(typeQuery: JS.TypeQuery, q: RpcReceiveQueue): J | undefined {
        const updates = {
            typeExpression: q.receive(typeQuery.typeExpression, el => this.visitDefined<TypeTree>(el, q)),
            typeArguments: q.receive(typeQuery.typeArguments, el => this.visitContainer(el, q)),
            type: q.receive(typeQuery.type, el => this.visitType(el, q))
        };
        return updateIfChanged(typeQuery, updates);
    }

    visitTypeInfo(typeInfo: JS.TypeInfo, q: RpcReceiveQueue): J | undefined {
        const updates = {
            typeIdentifier: q.receive(typeInfo.typeIdentifier, el => this.visitDefined<TypeTree>(el, q))
        };
        return updateIfChanged(typeInfo, updates);
    }

    visitComputedPropertyName(computedPropertyName: JS.ComputedPropertyName, q: RpcReceiveQueue): J | undefined {
        const updates = {
            expression: q.receive(computedPropertyName.expression, el => this.visitRightPadded(el, q))
        };
        return updateIfChanged(computedPropertyName, updates);
    }

    visitTypeOperator(typeOperator: JS.TypeOperator, q: RpcReceiveQueue): J | undefined {
        const updates = {
            operator: q.receive(typeOperator.operator),
            expression: q.receive(typeOperator.expression, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(typeOperator, updates);
    }

    visitTypePredicate(typePredicate: JS.TypePredicate, q: RpcReceiveQueue): J | undefined {
        const updates = {
            asserts: q.receive(typePredicate.asserts, el => this.visitLeftPadded(el, q)),
            parameterName: q.receive(typePredicate.parameterName, el => this.visitDefined<J.Identifier>(el, q)),
            expression: q.receive(typePredicate.expression, el => this.visitLeftPadded(el, q)),
            type: q.receive(typePredicate.type, el => this.visitType(el, q))
        };
        return updateIfChanged(typePredicate, updates);
    }

    visitUnion(union: JS.Union, q: RpcReceiveQueue): J | undefined {
        const updates = {
            types: q.receiveListDefined(union.types, el => this.visitRightPadded(el, q)),
            type: q.receive(union.type, el => this.visitType(el, q))
        };
        return updateIfChanged(union, updates);
    }

    visitIntersection(intersection: JS.Intersection, q: RpcReceiveQueue): J | undefined {
        const updates = {
            types: q.receiveListDefined(intersection.types, el => this.visitRightPadded(el, q)),
            type: q.receive(intersection.type, el => this.visitType(el, q))
        };
        return updateIfChanged(intersection, updates);
    }

    visitVoid(void_: JS.Void, q: RpcReceiveQueue): J | undefined {
        const updates = {
            expression: q.receive(void_.expression, el => this.visitDefined<Expression>(el, q))
        };
        return updateIfChanged(void_, updates);
    }

    visitWithStatement(withStatement: JS.WithStatement, q: RpcReceiveQueue): J | undefined {
        const updates = {
            expression: q.receive(withStatement.expression, el => this.visitDefined<J.ControlParentheses<Expression>>(el, q)),
            body: q.receive(withStatement.body, el => this.visitRightPadded(el, q))
        };
        return updateIfChanged(withStatement, updates);
    }

    visitJsxTag(tag: JSX.Tag, q: RpcReceiveQueue): J | undefined {
            const updates = {
                openName: q.receive(tag.openName, el => this.visitLeftPadded(el, q)),
                typeArguments: q.receive(tag.typeArguments, el => this.visitContainer(el, q)),
                afterName: q.receive(tag.afterName, space => this.visitSpace(space, q)),
                attributes: q.receiveListDefined(tag.attributes, attr => this.visitRightPadded(attr, q)),
                selfClosing: q.receive(tag.selfClosing, space => this.visitSpace(space, q)),
                children: q.receiveList(tag.children, child => this.visit(child, q)),
                closingName: q.receive(tag.closingName, el => this.visitLeftPadded(el, q)),
                afterClosingName: q.receive(tag.afterClosingName, el => this.visitSpace(el, q))
            };
            // Type assertion is needed due to `JSX.Tag` being a union type
            return updateIfChanged(tag, updates as any);
    }

    visitJsxAttribute(attribute: JSX.Attribute, q: RpcReceiveQueue): J | undefined {
        const updates = {
            key: q.receive(attribute.key, el => this.visitDefined<J.Identifier | JSX.NamespacedName>(el, q)),
            value: q.receive(attribute.value, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(attribute, updates);
    }

    visitJsxSpreadAttribute(spreadAttribute: JSX.SpreadAttribute, q: RpcReceiveQueue): J | undefined {
        const updates = {
            dots: q.receive(spreadAttribute.dots, space => this.visitSpace(space, q)),
            expression: q.receive(spreadAttribute.expression, el => this.visitRightPadded(el, q))
        };
        return updateIfChanged(spreadAttribute, updates);
    }

    visitJsxEmbeddedExpression(embeddedExpression: JSX.EmbeddedExpression, q: RpcReceiveQueue): J | undefined {
        const updates = {
            expression: q.receive(embeddedExpression.expression, el => this.visitRightPadded(el, q))
        };
        return updateIfChanged(embeddedExpression, updates);
    }

    visitJsxNamespacedName(namespacedName: JSX.NamespacedName, q: RpcReceiveQueue): J | undefined {
        const updates = {
            namespace: q.receive(namespacedName.namespace, el => this.visitDefined<J.Identifier>(el, q)),
            name: q.receive(namespacedName.name, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(namespacedName, updates);
    }

    visitIndexSignatureDeclaration(indexSignatureDeclaration: JS.IndexSignatureDeclaration, q: RpcReceiveQueue): J | undefined {
        const updates = {
            modifiers: q.receiveListDefined(indexSignatureDeclaration.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            parameters: q.receive(indexSignatureDeclaration.parameters, el => this.visitContainer(el, q)),
            typeExpression: q.receive(indexSignatureDeclaration.typeExpression, el => this.visitLeftPadded(el, q)),
            type: q.receive(indexSignatureDeclaration.type, el => this.visitType(el, q))
        };
        return updateIfChanged(indexSignatureDeclaration, updates);
    }

    visitComputedPropertyMethodDeclaration(computedPropMethod: JS.ComputedPropertyMethodDeclaration, q: RpcReceiveQueue): J | undefined {
        const updates = {
            leadingAnnotations: q.receiveListDefined(computedPropMethod.leadingAnnotations, el => this.visitDefined<J.Annotation>(el, q)),
            modifiers: q.receiveListDefined(computedPropMethod.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            typeParameters: q.receive(computedPropMethod.typeParameters, el => this.visitDefined<J.TypeParameters>(el, q)),
            returnTypeExpression: q.receive(computedPropMethod.returnTypeExpression, el => this.visitDefined<TypeTree>(el, q)),
            name: q.receive(computedPropMethod.name, el => this.visitDefined<ComputedPropertyName>(el, q)),
            parameters: q.receive(computedPropMethod.parameters, el => this.visitContainer(el, q)),
            body: q.receive(computedPropMethod.body, el => this.visitDefined<J.Block>(el, q)),
            methodType: q.receive(computedPropMethod.methodType, el => this.visitType(el, q) as any as Type.Method)
        };
        return updateIfChanged(computedPropMethod, updates);
    }

    visitForOfLoop(forOfLoop: JS.ForOfLoop, q: RpcReceiveQueue): J | undefined {
        const updates = {
            await: q.receive(forOfLoop.await, space => this.visitSpace(space, q)),
            loop: q.receive(forOfLoop.loop, el => this.visitDefined<J.ForEachLoop>(el, q))
        };
        return updateIfChanged(forOfLoop, updates);
    }

    visitForInLoop(forInLoop: JS.ForInLoop, q: RpcReceiveQueue): J | undefined {
        const updates = {
            control: q.receive(forInLoop.control, el => this.visitDefined<JS.ForInLoop.Control>(el, q)),
            body: q.receive(forInLoop.body, el => this.visitRightPadded(el, q))
        };
        return updateIfChanged(forInLoop, updates);
    }

    visitNamespaceDeclaration(namespaceDeclaration: JS.NamespaceDeclaration, q: RpcReceiveQueue): J | undefined {
        const updates = {
            modifiers: q.receiveListDefined(namespaceDeclaration.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            keywordType: q.receive(namespaceDeclaration.keywordType, el => this.visitLeftPadded(el, q)),
            name: q.receive(namespaceDeclaration.name, el => this.visitRightPadded(el, q)),
            body: q.receive(namespaceDeclaration.body, el => this.visitDefined<J.Block>(el, q))
        };
        return updateIfChanged(namespaceDeclaration, updates);
    }

    visitTypeLiteral(typeLiteral: JS.TypeLiteral, q: RpcReceiveQueue): J | undefined {
        const updates = {
            members: q.receive(typeLiteral.members, el => this.visitDefined<J.Block>(el, q)),
            type: q.receive(typeLiteral.type, el => this.visitType(el, q))
        };
        return updateIfChanged(typeLiteral, updates);
    }

    visitArrayBindingPattern(arrayBindingPattern: JS.ArrayBindingPattern, q: RpcReceiveQueue): J | undefined {
        const updates = {
            elements: q.receive(arrayBindingPattern.elements, el => this.visitContainer(el, q)),
            type: q.receive(arrayBindingPattern.type, el => this.visitType(el, q))
        };
        return updateIfChanged(arrayBindingPattern, updates);
    }

    visitBindingElement(bindingElement: JS.BindingElement, q: RpcReceiveQueue): J | undefined {
        const updates = {
            propertyName: q.receive(bindingElement.propertyName, el => this.visitRightPadded(el, q)),
            name: q.receive(bindingElement.name, el => this.visitDefined<TypedTree>(el, q)),
            initializer: q.receive(bindingElement.initializer, el => this.visitLeftPadded(el, q)),
            variableType: q.receive(bindingElement.variableType, el => this.visitType(el, q) as any as Type.Variable)
        };
        return updateIfChanged(bindingElement, updates);
    }

    visitExportDeclaration(exportDeclaration: JS.ExportDeclaration, q: RpcReceiveQueue): J | undefined {
        const updates = {
            modifiers: q.receiveListDefined(exportDeclaration.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            typeOnly: q.receive(exportDeclaration.typeOnly, el => this.visitLeftPadded(el, q)),
            exportClause: q.receive(exportDeclaration.exportClause, el => this.visitDefined<Expression>(el, q)),
            moduleSpecifier: q.receive(exportDeclaration.moduleSpecifier, el => this.visitLeftPadded(el, q)),
            attributes: q.receive(exportDeclaration.attributes, el => this.visitDefined<JS.ImportAttributes>(el, q))
        };
        return updateIfChanged(exportDeclaration, updates);
    }

    visitExportAssignment(exportAssignment: JS.ExportAssignment, q: RpcReceiveQueue): J | undefined {
        const updates = {
            exportEquals: q.receive(exportAssignment.exportEquals),
            expression: q.receive(exportAssignment.expression, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(exportAssignment, updates);
    }

    visitNamedExports(namedExports: JS.NamedExports, q: RpcReceiveQueue): J | undefined {
        const updates = {
            elements: q.receive(namedExports.elements, el => this.visitContainer(el, q)),
            type: q.receive(namedExports.type, el => this.visitType(el, q))
        };
        return updateIfChanged(namedExports, updates);
    }

    visitExportSpecifier(exportSpecifier: JS.ExportSpecifier, q: RpcReceiveQueue): J | undefined {
        const updates = {
            typeOnly: q.receive(exportSpecifier.typeOnly, el => this.visitLeftPadded(el, q)),
            specifier: q.receive(exportSpecifier.specifier, el => this.visitDefined<Expression>(el, q)),
            type: q.receive(exportSpecifier.type, el => this.visitType(el, q))
        };
        return updateIfChanged(exportSpecifier, updates);
    }

    // Inherited from JavaReceiver: visitRightPadded, visitLeftPadded, visitContainer, visitSpace, visitType
}

// Register codecs for JavaScript
registerJLanguageCodecs(JS.Kind.CompilationUnit, new JavaScriptReceiver(), new JavaScriptSender(), JS.Kind);
