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
        // With intersection types, stmt IS the statement with padding mixed in
        await q.getAndSendList(cu, c => c.statements, stmt => stmt.id, stmt => this.visitRightPadded(stmt, q), J.Kind.RightPadded);
        await q.getAndSend(cu, c => c.eof, space => this.visitSpace(space, q));
        return cu;
    }

    override async visitAlias(alias: JS.Alias, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(alias, el => el.propertyName, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
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
        await q.getAndSend(conditionalType, el => el.condition, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
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
        await q.getAndSend(functionCall, m => m.function, f => this.visitRightPadded(f, q), J.Kind.RightPadded);
        await q.getAndSend(functionCall, m => m.typeParameters, params => this.visitContainer(params, q));
        await q.getAndSend(functionCall, m => m.arguments, args => this.visitContainer(args, q));
        await q.getAndSend(functionCall, m => asRef(m.methodType), type => this.visitType(type, q));
        return functionCall;
    }

    override async visitFunctionType(functionType: JS.FunctionType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(functionType, el => el.modifiers, el => el.id, el => this.visit(el, q));
        await q.getAndSend(functionType, el => el.constructorType, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(functionType, el => el.typeParameters, el => this.visit(el, q));
        await q.getAndSend(functionType, el => el.parameters, el => this.visitContainer(el, q));
        await q.getAndSend(functionType, el => el.returnType, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        return functionType;
    }

    override async visitInferType(inferType: JS.InferType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(inferType, el => el.typeParameter, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(inferType, el => asRef(el.type), el => this.visitType(el, q));
        return inferType;
    }

    override async visitImportType(importType: JS.ImportType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(importType, el => el.hasTypeof, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
        await q.getAndSend(importType, el => el.argumentAndAttributes, el => this.visitContainer(el, q));
        await q.getAndSend(importType, el => el.qualifier, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(importType, el => el.typeArguments, el => this.visitContainer(el, q));
        await q.getAndSend(importType, el => asRef(el.type), el => this.visitType(el, q));
        return importType;
    }

    override async visitImportDeclaration(jsImport: JS.Import, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(jsImport, el => el.modifiers, el => el.id, el => this.visit(el, q));
        await q.getAndSend(jsImport, el => el.importClause, el => this.visit(el, q));
        await q.getAndSend(jsImport, el => el.moduleSpecifier, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(jsImport, el => el.attributes, el => this.visit(el, q));
        await q.getAndSend(jsImport, el => el.initializer, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        return jsImport;
    }

    override async visitImportClause(jsImportClause: JS.ImportClause, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(jsImportClause, el => el.typeOnly);
        await q.getAndSend(jsImportClause, el => el.name, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
        await q.getAndSend(jsImportClause, el => el.namedBindings, el => this.visit(el, q));
        return jsImportClause;
    }

    override async visitNamedImports(namedImports: JS.NamedImports, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(namedImports, el => el.elements, el => this.visitContainer(el, q));
        await q.getAndSend(namedImports, el => asRef(el.type), el => this.visitType(el, q));
        return namedImports;
    }

    override async visitImportSpecifier(jsImportSpecifier: JS.ImportSpecifier, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(jsImportSpecifier, el => el.importType, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
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
        await q.getAndSend(importTypeAttributes, el => el.token, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
        await q.getAndSend(importTypeAttributes, el => el.elements, el => this.visitContainer(el, q));
        await q.getAndSend(importTypeAttributes, el => el.end, el => this.visitSpace(el, q));
        return importTypeAttributes;
    }

    override async visitImportAttribute(importAttribute: JS.ImportAttribute, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(importAttribute, el => el.name, el => this.visit(el, q));
        await q.getAndSend(importAttribute, el => el.value, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        return importAttribute;
    }

    override async visitBinaryExtensions(binary: JS.Binary, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(binary, el => el.left, el => this.visit(el, q));
        await q.getAndSend(binary, el => el.operator, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
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
        await q.getAndSend(mappedType, el => el.prefixToken, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(mappedType, el => el.hasReadonly, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(mappedType, el => el.keysRemapping, el => this.visit(el, q));
        await q.getAndSend(mappedType, el => el.suffixToken, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(mappedType, el => el.hasQuestionToken, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(mappedType, el => el.valueType, el => this.visitContainer(el, q));
        await q.getAndSend(mappedType, el => asRef(el.type), el => this.visitType(el, q));
        return mappedType;
    }

    override async visitMappedTypeKeysRemapping(keysRemapping: JS.MappedType.KeysRemapping, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(keysRemapping, el => el.typeParameter, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
        await q.getAndSend(keysRemapping, el => el.nameType, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
        return keysRemapping;
    }

    override async visitMappedTypeParameter(mappedTypeParameter: JS.MappedType.Parameter, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(mappedTypeParameter, el => el.name, el => this.visit(el, q));
        await q.getAndSend(mappedTypeParameter, el => el.iterateType, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        return mappedTypeParameter;
    }

    override async visitObjectBindingPattern(objectBindingPattern: JS.ObjectBindingPattern, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(objectBindingPattern, el => el.leadingAnnotations, el => el.id, el => this.visit(el, q));
        await q.getAndSendList(objectBindingPattern, el => el.modifiers, el => el.id, el => this.visit(el, q));
        await q.getAndSend(objectBindingPattern, el => el.typeExpression, el => this.visit(el, q));
        await q.getAndSend(objectBindingPattern, el => el.bindings, el => this.visitContainer(el, q));
        await q.getAndSend(objectBindingPattern, el => el.initializer, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        return objectBindingPattern;
    }

    override async visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(propertyAssignment, el => el.name, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
        await q.getAndSend(propertyAssignment, el => el.assigmentToken);
        await q.getAndSend(propertyAssignment, el => el.initializer, el => this.visit(el, q));
        return propertyAssignment;
    }

    override async visitSatisfiesExpression(satisfiesExpression: JS.SatisfiesExpression, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(satisfiesExpression, el => el.expression, el => this.visit(el, q));
        await q.getAndSend(satisfiesExpression, el => el.satisfiesType, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(satisfiesExpression, el => asRef(el.type), el => this.visitType(el, q));
        return satisfiesExpression;
    }

    override async visitScopedVariableDeclarations(scopedVariableDeclarations: JS.ScopedVariableDeclarations, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(scopedVariableDeclarations, el => el.modifiers, el => el.id, el => this.visit(el, q));
        // With intersection types, el IS the variable with padding mixed in
        await q.getAndSendList(scopedVariableDeclarations, el => el.variables, el => el.id, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
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
        await q.getAndSend(taggedTemplateExpression, el => el.tag, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
        await q.getAndSend(taggedTemplateExpression, el => el.typeArguments, el => this.visitContainer(el, q));
        await q.getAndSend(taggedTemplateExpression, el => el.templateExpression, el => this.visit(el, q));
        await q.getAndSend(taggedTemplateExpression, el => asRef(el.type), el => this.visitType(el, q));
        return taggedTemplateExpression;
    }

    override async visitTemplateExpression(templateExpression: JS.TemplateExpression, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(templateExpression, el => el.head, el => this.visit(el, q));
        // With intersection types, el IS the span with padding mixed in
        await q.getAndSendList(templateExpression, el => el.spans, el => el.id, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
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
        await q.getAndSend(typeDeclaration, el => el.name, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(typeDeclaration, el => el.typeParameters, el => this.visit(el, q));
        await q.getAndSend(typeDeclaration, el => el.initializer, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
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
        await q.getAndSend(as_, el => el.left, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
        await q.getAndSend(as_, el => el.right, el => this.visit(el, q));
        await q.getAndSend(as_, el => asRef(el.type), el => this.visitType(el, q));
        return as_;
    }

    override async visitAssignmentOperationExtensions(assignmentOperation: JS.AssignmentOperation, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(assignmentOperation, el => el.variable, el => this.visit(el, q));
        await q.getAndSend(assignmentOperation, el => el.operator, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
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
        await q.getAndSend(indexType, el => el.element, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
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
        await q.getAndSend(computedPropertyName, el => el.expression, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
        return computedPropertyName;
    }

    override async visitTypeOperator(typeOperator: JS.TypeOperator, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typeOperator, el => el.operator);
        await q.getAndSend(typeOperator, el => el.expression, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        return typeOperator;
    }

    override async visitTypePredicate(typePredicate: JS.TypePredicate, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typePredicate, el => el.asserts, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(typePredicate, el => el.parameterName, el => this.visit(el, q));
        await q.getAndSend(typePredicate, el => el.expression, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(typePredicate, el => asRef(el.type), el => this.visitType(el, q));
        return typePredicate;
    }

    override async visitUnion(union: JS.Union, q: RpcSendQueue): Promise<J | undefined> {
        // With intersection types, el IS the type with padding mixed in
        await q.getAndSendList(union, el => el.types, el => el.id, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
        await q.getAndSend(union, el => asRef(el.type), el => this.visitType(el, q));
        return union;
    }

    override async visitIntersection(intersection: JS.Intersection, q: RpcSendQueue): Promise<J | undefined> {
        // With intersection types, el IS the type with padding mixed in
        await q.getAndSendList(intersection, el => el.types, el => el.id, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
        await q.getAndSend(intersection, el => asRef(el.type), el => this.visitType(el, q));
        return intersection;
    }

    override async visitVoid(void_: JS.Void, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(void_, el => el.expression, el => this.visit(el, q));
        return void_;
    }

    override async visitWithStatement(withStatement: JS.WithStatement, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(withStatement, el => el.expression, el => this.visit(el, q));
        await q.getAndSend(withStatement, el => el.body, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
        return withStatement;
    }

    override async visitJsxTag(tag: JSX.Tag, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(tag, el => el.openName, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(tag, el => el.typeArguments, el => this.visitContainer(el, q));
        await q.getAndSend(tag, el => el.afterName, space => this.visitSpace(space, q));
        // With intersection types, attr IS the attribute with padding mixed in
        await q.getAndSendList(tag, el => el.attributes, attr => attr.id, attr => this.visitRightPadded(attr, q), J.Kind.RightPadded);

        await q.getAndSend(tag, el => el.selfClosing, space => this.visitSpace(space, q));
        await q.getAndSendList(tag, el => el.children!, child => child.id, child => this.visit(child, q));
        await q.getAndSend(tag, el => el.closingName, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(tag, el => el.afterClosingName, el => this.visitSpace(el, q));

        return tag;
    }

    override async visitJsxAttribute(attribute: JSX.Attribute, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(attribute, el => el.key, el => this.visit(el, q));
        await q.getAndSend(attribute, el => el.value, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        return attribute;
    }

    override async visitJsxSpreadAttribute(spreadAttribute: JSX.SpreadAttribute, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(spreadAttribute, el => el.dots, space => this.visitSpace(space, q));
        await q.getAndSend(spreadAttribute, el => el.expression, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
        return spreadAttribute;
    }

    override async visitJsxEmbeddedExpression(embeddedExpression: JSX.EmbeddedExpression, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(embeddedExpression, el => el.expression, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
        return embeddedExpression;
    }

    override async visitJsxNamespacedName(namespacedName: JSX.NamespacedName, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(namespacedName, el => el.namespace, el => this.visit(el, q));
        await q.getAndSend(namespacedName, el => el.name, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        return namespacedName;
    }

    override async visitIndexSignatureDeclaration(indexSignatureDeclaration: JS.IndexSignatureDeclaration, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(indexSignatureDeclaration, el => el.modifiers, el => el.id, el => this.visit(el, q));
        await q.getAndSend(indexSignatureDeclaration, el => el.parameters, el => this.visitContainer(el, q));
        await q.getAndSend(indexSignatureDeclaration, el => el.typeExpression, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
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
        await q.getAndSend(forInLoop, el => el.body, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
        return forInLoop;
    }

    override async visitNamespaceDeclaration(namespaceDeclaration: JS.NamespaceDeclaration, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(namespaceDeclaration, el => el.modifiers, el => el.id, el => this.visit(el, q));
        await q.getAndSend(namespaceDeclaration, el => el.keywordType, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(namespaceDeclaration, el => el.name, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
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
        await q.getAndSend(bindingElement, el => el.propertyName, el => this.visitRightPadded(el, q), J.Kind.RightPadded);
        await q.getAndSend(bindingElement, el => el.name, el => this.visit(el, q));
        await q.getAndSend(bindingElement, el => el.initializer, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(bindingElement, el => el.variableType, el => this.visitType(el, q));
        return bindingElement;
    }

    override async visitExportDeclaration(exportDeclaration: JS.ExportDeclaration, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(exportDeclaration, el => el.modifiers, el => el.id, el => this.visit(el, q));
        await q.getAndSend(exportDeclaration, el => el.typeOnly, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(exportDeclaration, el => el.exportClause, el => this.visit(el, q));
        await q.getAndSend(exportDeclaration, el => el.moduleSpecifier, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        await q.getAndSend(exportDeclaration, el => el.attributes, el => this.visit(el, q));
        return exportDeclaration;
    }

    override async visitExportAssignment(exportAssignment: JS.ExportAssignment, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(exportAssignment, el => el.exportEquals);
        await q.getAndSend(exportAssignment, el => el.expression, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
        return exportAssignment;
    }

    override async visitNamedExports(namedExports: JS.NamedExports, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(namedExports, el => el.elements, el => this.visitContainer(el, q));
        await q.getAndSend(namedExports, el => asRef(el.type), el => this.visitType(el, q));
        return namedExports;
    }

    override async visitExportSpecifier(exportSpecifier: JS.ExportSpecifier, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(exportSpecifier, el => el.typeOnly, el => this.visitLeftPadded(el, q), J.Kind.LeftPadded);
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

class JavaScriptReceiver extends JavaScriptVisitor<RpcReceiveQueue> {
    private delegate: JavaReceiver;

    constructor() {
        super();
        this.delegate = new JavaScriptDelegateReceiver(this);
    }

    async visit<R extends J>(tree: Tree, p: RpcReceiveQueue, parent?: Cursor): Promise<R | undefined> {
        if (isJavaScript(tree)) {
            return super.visit(tree, p, parent);
        }
        return this.delegate.visit(tree, p, parent);
    }

    override async preVisit(j: JS, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            id: await q.receive(j.id),
            prefix: await q.receive(j.prefix, space => this.visitSpace(space, q)),
            markers: await q.receive(j.markers)
        };
        return updateIfChanged(j, updates);
    }

    override async visitJsCompilationUnit(cu: JS.CompilationUnit, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            sourcePath: await q.receive(cu.sourcePath),
            charsetName: await q.receive(cu.charsetName),
            charsetBomMarked: await q.receive(cu.charsetBomMarked),
            checksum: await q.receive(cu.checksum),
            fileAttributes: await q.receive(cu.fileAttributes),
            statements: await q.receiveListDefined(cu.statements, stmt => this.visitRightPadded(stmt, q)),
            eof: await q.receive(cu.eof, space => this.visitSpace(space, q))
        };
        return updateIfChanged(cu, updates);
    }

    override async visitAlias(alias: JS.Alias, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            propertyName: await q.receive(alias.propertyName, el => this.visitRightPadded(el, q)),
            alias: await q.receive(alias.alias, el => this.visitDefined<Expression>(el, q))
        };
        return updateIfChanged(alias, updates);
    }

    override async visitArrowFunction(arrowFunction: JS.ArrowFunction, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            leadingAnnotations: await q.receiveListDefined(arrowFunction.leadingAnnotations, el => this.visitDefined<J.Annotation>(el, q)),
            modifiers: await q.receiveListDefined(arrowFunction.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            typeParameters: await q.receive(arrowFunction.typeParameters, el => this.visitDefined<J.TypeParameters>(el, q)),
            lambda: await q.receive(arrowFunction.lambda, el => this.visitDefined<J.Lambda>(el, q)),
            returnTypeExpression: await q.receive(arrowFunction.returnTypeExpression, el => this.visitDefined<TypeTree>(el, q))
        };
        return updateIfChanged(arrowFunction, updates);
    }

    override async visitAwait(anAwait: JS.Await, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            expression: await q.receive(anAwait.expression, el => this.visitDefined<Expression>(el, q)),
            type: await q.receive(anAwait.type, el => this.visitType(el, q))
        };
        return updateIfChanged(anAwait, updates);
    }

    override async visitConditionalType(conditionalType: JS.ConditionalType, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            checkType: await q.receive(conditionalType.checkType, el => this.visitDefined<Expression>(el, q)),
            condition: await q.receive(conditionalType.condition, el => this.visitLeftPadded(el, q)),
            type: await q.receive(conditionalType.type, el => this.visitType(el, q))
        };
        return updateIfChanged(conditionalType, updates);
    }

    override async visitDelete(delete_: JS.Delete, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            expression: await q.receive(delete_.expression, el => this.visitDefined<Expression>(el, q))
        };
        return updateIfChanged(delete_, updates);
    }

    override async visitExpressionStatement(expressionStatement: JS.ExpressionStatement, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            expression: await q.receive(expressionStatement.expression, el => this.visitDefined<Expression>(el, q))
        };
        return updateIfChanged(expressionStatement, updates);
    }

    override async visitExpressionWithTypeArguments(expressionWithTypeArguments: JS.ExpressionWithTypeArguments, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            clazz: await q.receive(expressionWithTypeArguments.clazz, el => this.visitDefined<J>(el, q)),
            typeArguments: await q.receive(expressionWithTypeArguments.typeArguments, el => this.visitContainer(el, q)),
            type: await q.receive(expressionWithTypeArguments.type, el => this.visitType(el, q))
        };
        return updateIfChanged(expressionWithTypeArguments, updates);
    }

    override async visitFunctionCall(functionCall: JS.FunctionCall, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            function: await q.receive(functionCall.function, select => this.visitRightPadded(select, q)),
            typeParameters: await q.receive(functionCall.typeParameters, typeParams => this.visitContainer(typeParams, q)),
            arguments: await q.receive(functionCall.arguments, args => this.visitContainer(args, q)),
            methodType: await q.receive(functionCall.methodType, type => this.visitType(type, q) as unknown as Type.Method)
        };
        return updateIfChanged(functionCall, updates);
    }

    override async visitFunctionType(functionType: JS.FunctionType, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            modifiers: await q.receiveListDefined(functionType.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            constructorType: await q.receive(functionType.constructorType, async el => this.visitLeftPadded(el, q) as any),
            typeParameters: await q.receive(functionType.typeParameters, el => this.visitDefined<J.TypeParameters>(el, q)),
            parameters: await q.receive(functionType.parameters, el => this.visitContainer(el, q)),
            returnType: await q.receive(functionType.returnType, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(functionType, updates);
    }

    override async visitInferType(inferType: JS.InferType, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            typeParameter: await q.receive(inferType.typeParameter, el => this.visitLeftPadded(el, q)),
            type: await q.receive(inferType.type, el => this.visitType(el, q))
        };
        return updateIfChanged(inferType, updates);
    }

    override async visitImportType(importType: JS.ImportType, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            hasTypeof: await q.receive(importType.hasTypeof, async el => this.visitRightPadded(el, q) as any),
            argumentAndAttributes: await q.receive(importType.argumentAndAttributes, el => this.visitContainer(el, q)),
            qualifier: await q.receive(importType.qualifier, el => this.visitLeftPadded(el, q)),
            typeArguments: await q.receive(importType.typeArguments, el => this.visitContainer(el, q)),
            type: await q.receive(importType.type, el => this.visitType(el, q))
        };
        return updateIfChanged(importType, updates);
    }

    override async visitImportDeclaration(jsImport: JS.Import, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            modifiers: await q.receiveListDefined(jsImport.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            importClause: await q.receive(jsImport.importClause, el => this.visitDefined<JS.ImportClause>(el, q)),
            moduleSpecifier: await q.receive(jsImport.moduleSpecifier, el => this.visitLeftPadded(el, q)),
            attributes: await q.receive(jsImport.attributes, el => this.visitDefined<JS.ImportAttributes>(el, q)),
            initializer: await q.receive(jsImport.initializer, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(jsImport, updates);
    }

    override async visitImportClause(jsImportClause: JS.ImportClause, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            typeOnly: await q.receive(jsImportClause.typeOnly),
            name: await q.receive(jsImportClause.name, el => this.visitRightPadded(el, q)),
            namedBindings: await q.receive(jsImportClause.namedBindings, el => this.visitDefined<Expression>(el, q))
        };
        return updateIfChanged(jsImportClause, updates);
    }

    override async visitNamedImports(namedImports: JS.NamedImports, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            elements: await q.receive(namedImports.elements, el => this.visitContainer(el, q)),
            type: await q.receive(namedImports.type, el => this.visitType(el, q))
        };
        return updateIfChanged(namedImports, updates);
    }

    override async visitImportSpecifier(jsImportSpecifier: JS.ImportSpecifier, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            importType: await q.receive(jsImportSpecifier.importType, async el => this.visitLeftPadded(el, q) as any),
            specifier: await q.receive(jsImportSpecifier.specifier, el => this.visitDefined<JS.Alias | J.Identifier>(el, q)),
            type: await q.receive(jsImportSpecifier.type, el => this.visitType(el, q))
        };
        return updateIfChanged(jsImportSpecifier, updates);
    }

    override async visitImportAttributes(importAttributes: JS.ImportAttributes, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            token: await q.receive(importAttributes.token),
            elements: await q.receive(importAttributes.elements, el => this.visitContainer(el, q))
        };
        return updateIfChanged(importAttributes, updates);
    }

    override async visitImportTypeAttributes(importTypeAttributes: JS.ImportTypeAttributes, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            token: await q.receive(importTypeAttributes.token, el => this.visitRightPadded(el, q)),
            elements: await q.receive(importTypeAttributes.elements, el => this.visitContainer(el, q)),
            end: await q.receive(importTypeAttributes.end, el => this.visitSpace(el, q))
        };
        return updateIfChanged(importTypeAttributes, updates);
    }

    override async visitImportAttribute(importAttribute: JS.ImportAttribute, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            name: await q.receive(importAttribute.name, el => this.visitDefined<Expression>(el, q)),
            value: await q.receive(importAttribute.value, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(importAttribute, updates);
    }

    override async visitBinaryExtensions(binary: JS.Binary, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            left: await q.receive(binary.left, el => this.visitDefined<Expression>(el, q)),
            operator: await q.receive(binary.operator, async el => this.visitLeftPadded(el, q) as any),
            right: await q.receive(binary.right, el => this.visitDefined<Expression>(el, q)),
            type: await q.receive(binary.type, el => this.visitType(el, q))
        };
        return updateIfChanged(binary, updates);
    }

    override async visitLiteralType(literalType: JS.LiteralType, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            literal: await q.receive(literalType.literal, el => this.visitDefined<Expression>(el, q)),
            type: await q.receive(literalType.type, el => this.visitType(el, q))
        };
        return updateIfChanged(literalType, updates);
    }

    override async visitMappedType(mappedType: JS.MappedType, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            prefixToken: await q.receive(mappedType.prefixToken, el => this.visitLeftPadded(el, q)),
            hasReadonly: await q.receive(mappedType.hasReadonly, async el => this.visitLeftPadded(el, q) as any),
            keysRemapping: await q.receive(mappedType.keysRemapping, el => this.visitDefined<JS.MappedType.KeysRemapping>(el, q)),
            suffixToken: await q.receive(mappedType.suffixToken, el => this.visitLeftPadded(el, q)),
            hasQuestionToken: await q.receive(mappedType.hasQuestionToken, async el => this.visitLeftPadded(el, q) as any),
            valueType: await q.receive(mappedType.valueType, el => this.visitContainer(el, q)),
            type: await q.receive(mappedType.type, el => this.visitType(el, q))
        };
        return updateIfChanged(mappedType, updates);
    }

    override async visitMappedTypeKeysRemapping(keysRemapping: JS.MappedType.KeysRemapping, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            typeParameter: await q.receive(keysRemapping.typeParameter, el => this.visitRightPadded(el, q)),
            nameType: await q.receive(keysRemapping.nameType, el => this.visitRightPadded(el, q))
        };
        return updateIfChanged(keysRemapping, updates);
    }

    override async visitMappedTypeParameter(mappedTypeParameter: JS.MappedType.Parameter, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            name: await q.receive(mappedTypeParameter.name, el => this.visitDefined<Expression>(el, q)),
            iterateType: await q.receive(mappedTypeParameter.iterateType, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(mappedTypeParameter, updates);
    }

    override async visitObjectBindingPattern(objectBindingPattern: JS.ObjectBindingPattern, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            leadingAnnotations: await q.receiveListDefined(objectBindingPattern.leadingAnnotations, el => this.visitDefined<J.Annotation>(el, q)),
            modifiers: await q.receiveListDefined(objectBindingPattern.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            typeExpression: await q.receive(objectBindingPattern.typeExpression, el => this.visitDefined<TypeTree>(el, q)),
            bindings: await q.receive(objectBindingPattern.bindings, el => this.visitContainer(el, q)),
            initializer: await q.receive(objectBindingPattern.initializer, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(objectBindingPattern, updates);
    }

    override async visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            name: await q.receive(propertyAssignment.name, el => this.visitRightPadded(el, q)),
            assigmentToken: await q.receive(propertyAssignment.assigmentToken),
            initializer: await q.receive(propertyAssignment.initializer, el => this.visitDefined<Expression>(el, q))
        };
        return updateIfChanged(propertyAssignment, updates);
    }

    override async visitSatisfiesExpression(satisfiesExpression: JS.SatisfiesExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            expression: await q.receive(satisfiesExpression.expression, el => this.visitDefined<J>(el, q)),
            satisfiesType: await q.receive(satisfiesExpression.satisfiesType, el => this.visitLeftPadded(el, q)),
            type: await q.receive(satisfiesExpression.type, el => this.visitType(el, q))
        };
        return updateIfChanged(satisfiesExpression, updates);
    }

    override async visitScopedVariableDeclarations(scopedVariableDeclarations: JS.ScopedVariableDeclarations, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            modifiers: await q.receiveListDefined(scopedVariableDeclarations.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            variables: await q.receiveListDefined(scopedVariableDeclarations.variables, el => this.visitRightPadded(el, q))
        };
        return updateIfChanged(scopedVariableDeclarations, updates);
    }

    override async visitShebang(shebang: JS.Shebang, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            text: await q.receive(shebang.text)
        };
        return updateIfChanged(shebang, updates);
    }

    override async visitSpread(spread: JS.Spread, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            expression: await q.receive(spread.expression, el => this.visitDefined<Expression>(el, q)),
            type: await q.receive(spread.type, el => this.visitType(el, q))
        };
        return updateIfChanged(spread, updates);
    }

    override async visitStatementExpression(statementExpression: JS.StatementExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            statement: await q.receive(statementExpression.statement, el => this.visitDefined<Statement>(el, q))
        };
        return updateIfChanged(statementExpression, updates);
    }

    override async visitTaggedTemplateExpression(taggedTemplateExpression: JS.TaggedTemplateExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            tag: await q.receive(taggedTemplateExpression.tag, el => this.visitRightPadded(el, q)),
            typeArguments: await q.receive(taggedTemplateExpression.typeArguments, el => this.visitContainer(el, q)),
            templateExpression: await q.receive(taggedTemplateExpression.templateExpression, el => this.visitDefined<Expression>(el, q)),
            type: await q.receive(taggedTemplateExpression.type, el => this.visitType(el, q))
        };
        return updateIfChanged(taggedTemplateExpression, updates);
    }

    override async visitTemplateExpression(templateExpression: JS.TemplateExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            head: await q.receive(templateExpression.head, el => this.visitDefined<J.Literal>(el, q)),
            spans: await q.receiveListDefined(templateExpression.spans, el => this.visitRightPadded(el, q)),
            type: await q.receive(templateExpression.type, el => this.visitType(el, q))
        };
        return updateIfChanged(templateExpression, updates);
    }

    override async visitTemplateExpressionSpan(span: JS.TemplateExpression.Span, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            expression: await q.receive(span.expression, el => this.visitDefined<J>(el, q)),
            tail: await q.receive(span.tail, el => this.visitDefined<J.Literal>(el, q))
        };
        return updateIfChanged(span, updates);
    }

    override async visitTuple(tuple: JS.Tuple, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            elements: await q.receive(tuple.elements, el => this.visitContainer(el, q)),
            type: await q.receive(tuple.type, el => this.visitType(el, q))
        };
        return updateIfChanged(tuple, updates);
    }

    override async visitTypeDeclaration(typeDeclaration: JS.TypeDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            modifiers: await q.receiveListDefined(typeDeclaration.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            name: await q.receive(typeDeclaration.name, el => this.visitLeftPadded(el, q)),
            typeParameters: await q.receive(typeDeclaration.typeParameters, el => this.visitDefined<J.TypeParameters>(el, q)),
            initializer: await q.receive(typeDeclaration.initializer, el => this.visitLeftPadded(el, q)),
            type: await q.receive(typeDeclaration.type, el => this.visitType(el, q))
        };
        return updateIfChanged(typeDeclaration, updates);
    }

    override async visitTypeOf(typeOf: JS.TypeOf, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            expression: await q.receive(typeOf.expression, el => this.visitDefined<Expression>(el, q)),
            type: await q.receive(typeOf.type, el => this.visitType(el, q))
        };
        return updateIfChanged(typeOf, updates);
    }

    override async visitTypeTreeExpression(typeTreeExpression: JS.TypeTreeExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            expression: await q.receive(typeTreeExpression.expression, el => this.visitDefined<Expression>(el, q))
        };
        return updateIfChanged(typeTreeExpression, updates);
    }

    override async visitAs(as_: JS.As, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            left: await q.receive(as_.left, el => this.visitRightPadded<Expression>(el, q)),
            right: await q.receive(as_.right, el => this.visitDefined<Expression>(el, q)),
            type: await q.receive(as_.type, el => this.visitType(el, q))
        };
        return updateIfChanged(as_, updates);
    }

    override async visitAssignmentOperationExtensions(assignmentOperation: JS.AssignmentOperation, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            variable: await q.receive(assignmentOperation.variable, el => this.visitDefined<Expression>(el, q)),
            operator: await q.receive(assignmentOperation.operator, async el => this.visitLeftPadded(el, q) as any),
            assignment: await q.receive(assignmentOperation.assignment, el => this.visitDefined<Expression>(el, q)),
            type: await q.receive(assignmentOperation.type, el => this.visitType(el, q))
        };
        return updateIfChanged(assignmentOperation, updates);
    }

    override async visitIndexedAccessType(indexedAccessType: JS.IndexedAccessType, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            objectType: await q.receive(indexedAccessType.objectType, el => this.visitDefined<TypeTree>(el, q)),
            indexType: await q.receive(indexedAccessType.indexType, el => this.visitDefined<TypeTree>(el, q)),
            type: await q.receive(indexedAccessType.type, el => this.visitType(el, q))
        };
        return updateIfChanged(indexedAccessType, updates);
    }

    override async visitIndexedAccessTypeIndexType(indexType: JS.IndexedAccessType.IndexType, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            element: await q.receive(indexType.element, el => this.visitRightPadded(el, q)),
            type: await q.receive(indexType.type, el => this.visitType(el, q))
        };
        return updateIfChanged(indexType, updates);
    }

    override async visitTypeQuery(typeQuery: JS.TypeQuery, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            typeExpression: await q.receive(typeQuery.typeExpression, el => this.visitDefined<TypeTree>(el, q)),
            typeArguments: await q.receive(typeQuery.typeArguments, el => this.visitContainer(el, q)),
            type: await q.receive(typeQuery.type, el => this.visitType(el, q))
        };
        return updateIfChanged(typeQuery, updates);
    }

    override async visitTypeInfo(typeInfo: JS.TypeInfo, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            typeIdentifier: await q.receive(typeInfo.typeIdentifier, el => this.visitDefined<TypeTree>(el, q))
        };
        return updateIfChanged(typeInfo, updates);
    }

    override async visitComputedPropertyName(computedPropertyName: JS.ComputedPropertyName, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            expression: await q.receive(computedPropertyName.expression, el => this.visitRightPadded(el, q))
        };
        return updateIfChanged(computedPropertyName, updates);
    }

    override async visitTypeOperator(typeOperator: JS.TypeOperator, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            operator: await q.receive(typeOperator.operator),
            expression: await q.receive(typeOperator.expression, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(typeOperator, updates);
    }

    override async visitTypePredicate(typePredicate: JS.TypePredicate, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            asserts: await q.receive(typePredicate.asserts, async el => this.visitLeftPadded(el, q) as any),
            parameterName: await q.receive(typePredicate.parameterName, el => this.visitDefined<J.Identifier>(el, q)),
            expression: await q.receive(typePredicate.expression, el => this.visitLeftPadded(el, q)),
            type: await q.receive(typePredicate.type, el => this.visitType(el, q))
        };
        return updateIfChanged(typePredicate, updates);
    }

    override async visitUnion(union: JS.Union, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            types: await q.receiveListDefined(union.types, el => this.visitRightPadded(el, q)),
            type: await q.receive(union.type, el => this.visitType(el, q))
        };
        return updateIfChanged(union, updates);
    }

    override async visitIntersection(intersection: JS.Intersection, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            types: await q.receiveListDefined(intersection.types, el => this.visitRightPadded(el, q)),
            type: await q.receive(intersection.type, el => this.visitType(el, q))
        };
        return updateIfChanged(intersection, updates);
    }

    override async visitVoid(void_: JS.Void, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            expression: await q.receive(void_.expression, el => this.visitDefined<Expression>(el, q))
        };
        return updateIfChanged(void_, updates);
    }

    override async visitWithStatement(withStatement: JS.WithStatement, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            expression: await q.receive(withStatement.expression, el => this.visitDefined<J.ControlParentheses<Expression>>(el, q)),
            body: await q.receive(withStatement.body, el => this.visitRightPadded(el, q))
        };
        return updateIfChanged(withStatement, updates);
    }

    override async visitJsxTag(tag: JSX.Tag, q: RpcReceiveQueue): Promise<J | undefined> {
            const updates = {
                openName: await q.receive(tag.openName, el => this.visitLeftPadded(el, q)),
                typeArguments: await q.receive(tag.typeArguments, el => this.visitContainer(el, q)),
                afterName: await q.receive(tag.afterName, space => this.visitSpace(space, q)),
                attributes: await q.receiveListDefined(tag.attributes, attr => this.visitRightPadded(attr, q)),
                selfClosing: await q.receive(tag.selfClosing, space => this.visitSpace(space, q)),
                children: await q.receiveList(tag.children, child => this.visit(child, q)),
                closingName: await q.receive(tag.closingName, el => this.visitLeftPadded(el, q)),
                afterClosingName: await q.receive(tag.afterClosingName, el => this.visitSpace(el, q))
            };
            // Type assertion is needed due to `JSX.Tag` being a union type
            return updateIfChanged(tag, updates as any);
    }

    override async visitJsxAttribute(attribute: JSX.Attribute, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            key: await q.receive(attribute.key, el => this.visitDefined<J.Identifier | JSX.NamespacedName>(el, q)),
            value: await q.receive(attribute.value, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(attribute, updates);
    }

    override async visitJsxSpreadAttribute(spreadAttribute: JSX.SpreadAttribute, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            dots: await q.receive(spreadAttribute.dots, space => this.visitSpace(space, q)),
            expression: await q.receive(spreadAttribute.expression, el => this.visitRightPadded(el, q))
        };
        return updateIfChanged(spreadAttribute, updates);
    }

    override async visitJsxEmbeddedExpression(embeddedExpression: JSX.EmbeddedExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            expression: await q.receive(embeddedExpression.expression, el => this.visitRightPadded(el, q))
        };
        return updateIfChanged(embeddedExpression, updates);
    }

    override async visitJsxNamespacedName(namespacedName: JSX.NamespacedName, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            namespace: await q.receive(namespacedName.namespace, el => this.visitDefined<J.Identifier>(el, q)),
            name: await q.receive(namespacedName.name, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(namespacedName, updates);
    }

    override async visitIndexSignatureDeclaration(indexSignatureDeclaration: JS.IndexSignatureDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            modifiers: await q.receiveListDefined(indexSignatureDeclaration.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            parameters: await q.receive(indexSignatureDeclaration.parameters, el => this.visitContainer(el, q)),
            typeExpression: await q.receive(indexSignatureDeclaration.typeExpression, el => this.visitLeftPadded(el, q)),
            type: await q.receive(indexSignatureDeclaration.type, el => this.visitType(el, q))
        };
        return updateIfChanged(indexSignatureDeclaration, updates);
    }

    override async visitComputedPropertyMethodDeclaration(computedPropMethod: JS.ComputedPropertyMethodDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            leadingAnnotations: await q.receiveListDefined(computedPropMethod.leadingAnnotations, el => this.visitDefined<J.Annotation>(el, q)),
            modifiers: await q.receiveListDefined(computedPropMethod.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            typeParameters: await q.receive(computedPropMethod.typeParameters, el => this.visitDefined<J.TypeParameters>(el, q)),
            returnTypeExpression: await q.receive(computedPropMethod.returnTypeExpression, el => this.visitDefined<TypeTree>(el, q)),
            name: await q.receive(computedPropMethod.name, el => this.visitDefined<ComputedPropertyName>(el, q)),
            parameters: await q.receive(computedPropMethod.parameters, el => this.visitContainer(el, q)),
            body: await q.receive(computedPropMethod.body, el => this.visitDefined<J.Block>(el, q)),
            methodType: await q.receive(computedPropMethod.methodType, el => this.visitType(el, q) as any as Type.Method)
        };
        return updateIfChanged(computedPropMethod, updates);
    }

    override async visitForOfLoop(forOfLoop: JS.ForOfLoop, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            await: await q.receive(forOfLoop.await, space => this.visitSpace(space, q)),
            loop: await q.receive(forOfLoop.loop, el => this.visitDefined<J.ForEachLoop>(el, q))
        };
        return updateIfChanged(forOfLoop, updates);
    }

    override async visitForInLoop(forInLoop: JS.ForInLoop, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            control: await q.receive(forInLoop.control, el => this.visitDefined<JS.ForInLoop.Control>(el, q)),
            body: await q.receive(forInLoop.body, el => this.visitRightPadded(el, q))
        };
        return updateIfChanged(forInLoop, updates);
    }

    override async visitNamespaceDeclaration(namespaceDeclaration: JS.NamespaceDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            modifiers: await q.receiveListDefined(namespaceDeclaration.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            keywordType: await q.receive(namespaceDeclaration.keywordType, async el => this.visitLeftPadded(el, q) as any),
            name: await q.receive(namespaceDeclaration.name, el => this.visitRightPadded(el, q)),
            body: await q.receive(namespaceDeclaration.body, el => this.visitDefined<J.Block>(el, q))
        };
        return updateIfChanged(namespaceDeclaration, updates);
    }

    override async visitTypeLiteral(typeLiteral: JS.TypeLiteral, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            members: await q.receive(typeLiteral.members, el => this.visitDefined<J.Block>(el, q)),
            type: await q.receive(typeLiteral.type, el => this.visitType(el, q))
        };
        return updateIfChanged(typeLiteral, updates);
    }

    override async visitArrayBindingPattern(arrayBindingPattern: JS.ArrayBindingPattern, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            elements: await q.receive(arrayBindingPattern.elements, el => this.visitContainer(el, q)),
            type: await q.receive(arrayBindingPattern.type, el => this.visitType(el, q))
        };
        return updateIfChanged(arrayBindingPattern, updates);
    }

    override async visitBindingElement(bindingElement: JS.BindingElement, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            propertyName: await q.receive(bindingElement.propertyName, el => this.visitRightPadded(el, q)),
            name: await q.receive(bindingElement.name, el => this.visitDefined<TypedTree>(el, q)),
            initializer: await q.receive(bindingElement.initializer, el => this.visitLeftPadded(el, q)),
            variableType: await q.receive(bindingElement.variableType, el => this.visitType(el, q) as any as Type.Variable)
        };
        return updateIfChanged(bindingElement, updates);
    }

    override async visitExportDeclaration(exportDeclaration: JS.ExportDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            modifiers: await q.receiveListDefined(exportDeclaration.modifiers, el => this.visitDefined<J.Modifier>(el, q)),
            typeOnly: await q.receive(exportDeclaration.typeOnly, async el => this.visitLeftPadded(el, q) as any),
            exportClause: await q.receive(exportDeclaration.exportClause, el => this.visitDefined<Expression>(el, q)),
            moduleSpecifier: await q.receive(exportDeclaration.moduleSpecifier, el => this.visitLeftPadded(el, q)),
            attributes: await q.receive(exportDeclaration.attributes, el => this.visitDefined<JS.ImportAttributes>(el, q))
        };
        return updateIfChanged(exportDeclaration, updates);
    }

    override async visitExportAssignment(exportAssignment: JS.ExportAssignment, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            exportEquals: await q.receive(exportAssignment.exportEquals),
            expression: await q.receive(exportAssignment.expression, el => this.visitLeftPadded(el, q))
        };
        return updateIfChanged(exportAssignment, updates);
    }

    override async visitNamedExports(namedExports: JS.NamedExports, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            elements: await q.receive(namedExports.elements, el => this.visitContainer(el, q)),
            type: await q.receive(namedExports.type, el => this.visitType(el, q))
        };
        return updateIfChanged(namedExports, updates);
    }

    override async visitExportSpecifier(exportSpecifier: JS.ExportSpecifier, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            typeOnly: await q.receive(exportSpecifier.typeOnly, async el => this.visitLeftPadded(el, q) as any),
            specifier: await q.receive(exportSpecifier.specifier, el => this.visitDefined<Expression>(el, q)),
            type: await q.receive(exportSpecifier.type, el => this.visitType(el, q))
        };
        return updateIfChanged(exportSpecifier, updates);
    }

    override async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, q: RpcReceiveQueue): Promise<J.RightPadded<T>> {
        return this.delegate.visitRightPadded(right, q)
    }

    async visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, q: RpcReceiveQueue): Promise<J.LeftPadded<T>> {
        return this.delegate.visitLeftPadded(left, q);
    }

    async visitContainer<T extends J>(container: J.Container<T>, q: RpcReceiveQueue): Promise<J.Container<T>> {
        return this.delegate.visitContainer(container, q);
    }

    override async visitSpace(space: J.Space, q: RpcReceiveQueue): Promise<J.Space> {
        return this.delegate.visitSpace(space, q);
    }

    override async visitType(javaType: Type | undefined, q: RpcReceiveQueue): Promise<Type | undefined> {
        return this.delegate.visitType(javaType, q);
    }
}

class JavaScriptDelegateReceiver extends JavaReceiver {
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

registerJLanguageCodecs(JS.Kind.CompilationUnit, new JavaScriptReceiver(), new JavaScriptSender(), JS.Kind);
