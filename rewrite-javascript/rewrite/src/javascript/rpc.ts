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

    // Override visit() to skip cursor handling for performance
    override visit<R extends J>(tree: Tree | undefined, p: RpcSendQueue, _parent?: Cursor): R | undefined {
        if (!tree) return undefined;
        // Only call preVisit for JavaScript elements - Java elements are handled by the delegate
        if (isJavaScript(tree)) {
            let result = this.preVisit(tree as JS, p);
            if (!result) return undefined;
            return this.accept(result, p) as R | undefined;
        }
        return this.delegate.visit(tree, p) as R | undefined;
    }

    override preVisit(j: JS, q: RpcSendQueue): J | undefined {
        q.getAndSend(j, j2 => j2.id);
        q.getAndSend(j, j2 => j2.prefix, space => this.visitSpace(space, q));
        q.getAndSend(j, j2 => j2.markers);
        return j;
    }

    override visitJsCompilationUnit(cu: JS.CompilationUnit, q: RpcSendQueue): J | undefined {
        q.getAndSend(cu, c => c.sourcePath);
        q.getAndSend(cu, c => c.charsetName);
        q.getAndSend(cu, c => c.charsetBomMarked);
        q.getAndSend(cu, c => c.checksum);
        q.getAndSend(cu, c => c.fileAttributes);
        q.getAndSendList(cu, c => c.statements, stmt => stmt.element.id, stmt => this.visitRightPadded(stmt, q));
        q.getAndSend(cu, c => c.eof, space => this.visitSpace(space, q));
        return cu;
    }

    override visitAlias(alias: JS.Alias, q: RpcSendQueue): J | undefined {
        q.getAndSend(alias, el => el.propertyName, el => this.visitRightPadded(el, q));
        q.getAndSend(alias, el => el.alias, el => this.visit(el, q));
        return alias;
    }

    override visitArrowFunction(arrowFunction: JS.ArrowFunction, q: RpcSendQueue): J | undefined {
        q.getAndSendList(arrowFunction, el => el.leadingAnnotations, el => el.id, el => this.visit(el, q));
        q.getAndSendList(arrowFunction, el => el.modifiers, el => el.id, el => this.visit(el, q));
        q.getAndSend(arrowFunction, el => el.typeParameters, el => this.visit(el, q));
        q.getAndSend(arrowFunction, el => el.lambda, el => this.visit(el, q));
        q.getAndSend(arrowFunction, el => el.returnTypeExpression, el => this.visit(el, q));
        return arrowFunction;
    }

    override visitAwait(await_: JS.Await, q: RpcSendQueue): J | undefined {
        q.getAndSend(await_, el => el.expression, el => this.visit(el, q));
        q.getAndSend(await_, el => asRef(el.type), el => this.visitType(el, q));
        return await_;
    }

    override visitConditionalType(conditionalType: JS.ConditionalType, q: RpcSendQueue): J | undefined {
        q.getAndSend(conditionalType, el => el.checkType, el => this.visit(el, q));
        q.getAndSend(conditionalType, el => el.condition, el => this.visitLeftPadded(el, q));
        q.getAndSend(conditionalType, el => asRef(el.type), el => this.visitType(el, q));
        return conditionalType;
    }

    override visitDelete(delete_: JS.Delete, q: RpcSendQueue): J | undefined {
        q.getAndSend(delete_, el => el.expression, el => this.visit(el, q));
        return delete_;
    }

    override visitExpressionStatement(expressionStatement: JS.ExpressionStatement, q: RpcSendQueue): J | undefined {
        q.getAndSend(expressionStatement, el => el.expression, el => this.visit(el, q));
        return expressionStatement;
    }

    override visitExpressionWithTypeArguments(expressionWithTypeArguments: JS.ExpressionWithTypeArguments, q: RpcSendQueue): J | undefined {
        q.getAndSend(expressionWithTypeArguments, el => el.clazz, el => this.visit(el, q));
        q.getAndSend(expressionWithTypeArguments, el => el.typeArguments, el => this.visitContainer(el, q));
        q.getAndSend(expressionWithTypeArguments, el => asRef(el.type), el => this.visitType(el, q));
        return expressionWithTypeArguments;
    }

    override visitFunctionCall(functionCall: JS.FunctionCall, q: RpcSendQueue): J | undefined {
        q.getAndSend(functionCall, m => m.function, f => this.visitRightPadded(f, q));
        q.getAndSend(functionCall, m => m.typeParameters, params => this.visitContainer(params, q));
        q.getAndSend(functionCall, m => m.arguments, args => this.visitContainer(args, q));
        q.getAndSend(functionCall, m => asRef(m.methodType), type => this.visitType(type, q));
        return functionCall;
    }

    override visitFunctionType(functionType: JS.FunctionType, q: RpcSendQueue): J | undefined {
        q.getAndSendList(functionType, el => el.modifiers, el => el.id, el => this.visit(el, q));
        q.getAndSend(functionType, el => el.constructorType, el => this.visitLeftPadded(el, q));
        q.getAndSend(functionType, el => el.typeParameters, el => this.visit(el, q));
        q.getAndSend(functionType, el => el.parameters, el => this.visitContainer(el, q));
        q.getAndSend(functionType, el => el.returnType, el => this.visitLeftPadded(el, q));
        return functionType;
    }

    override visitInferType(inferType: JS.InferType, q: RpcSendQueue): J | undefined {
        q.getAndSend(inferType, el => el.typeParameter, el => this.visitLeftPadded(el, q));
        q.getAndSend(inferType, el => asRef(el.type), el => this.visitType(el, q));
        return inferType;
    }

    override visitImportType(importType: JS.ImportType, q: RpcSendQueue): J | undefined {
        q.getAndSend(importType, el => el.hasTypeof, el => this.visitRightPadded(el, q));
        q.getAndSend(importType, el => el.argumentAndAttributes, el => this.visitContainer(el, q));
        q.getAndSend(importType, el => el.qualifier, el => this.visitLeftPadded(el, q));
        q.getAndSend(importType, el => el.typeArguments, el => this.visitContainer(el, q));
        q.getAndSend(importType, el => asRef(el.type), el => this.visitType(el, q));
        return importType;
    }

    override visitImportDeclaration(jsImport: JS.Import, q: RpcSendQueue): J | undefined {
        q.getAndSendList(jsImport, el => el.modifiers, el => el.id, el => this.visit(el, q));
        q.getAndSend(jsImport, el => el.importClause, el => this.visit(el, q));
        q.getAndSend(jsImport, el => el.moduleSpecifier, el => this.visitLeftPadded(el, q));
        q.getAndSend(jsImport, el => el.attributes, el => this.visit(el, q));
        q.getAndSend(jsImport, el => el.initializer, el => this.visitLeftPadded(el, q));
        return jsImport;
    }

    override visitImportClause(jsImportClause: JS.ImportClause, q: RpcSendQueue): J | undefined {
        q.getAndSend(jsImportClause, el => el.typeOnly);
        q.getAndSend(jsImportClause, el => el.name, el => this.visitRightPadded(el, q));
        q.getAndSend(jsImportClause, el => el.namedBindings, el => this.visit(el, q));
        return jsImportClause;
    }

    override visitNamedImports(namedImports: JS.NamedImports, q: RpcSendQueue): J | undefined {
        q.getAndSend(namedImports, el => el.elements, el => this.visitContainer(el, q));
        q.getAndSend(namedImports, el => asRef(el.type), el => this.visitType(el, q));
        return namedImports;
    }

    override visitImportSpecifier(jsImportSpecifier: JS.ImportSpecifier, q: RpcSendQueue): J | undefined {
        q.getAndSend(jsImportSpecifier, el => el.importType, el => this.visitLeftPadded(el, q));
        q.getAndSend(jsImportSpecifier, el => el.specifier, el => this.visit(el, q));
        q.getAndSend(jsImportSpecifier, el => asRef(el.type), el => this.visitType(el, q));
        return jsImportSpecifier;
    }

    override visitImportAttributes(importAttributes: JS.ImportAttributes, q: RpcSendQueue): J | undefined {
        q.getAndSend(importAttributes, el => el.token);
        q.getAndSend(importAttributes, el => el.elements, el => this.visitContainer(el, q));
        return importAttributes;
    }

    override visitImportTypeAttributes(importTypeAttributes: JS.ImportTypeAttributes, q: RpcSendQueue): J | undefined {
        q.getAndSend(importTypeAttributes, el => el.token, el => this.visitRightPadded(el, q));
        q.getAndSend(importTypeAttributes, el => el.elements, el => this.visitContainer(el, q));
        q.getAndSend(importTypeAttributes, el => el.end, el => this.visitSpace(el, q));
        return importTypeAttributes;
    }

    override visitImportAttribute(importAttribute: JS.ImportAttribute, q: RpcSendQueue): J | undefined {
        q.getAndSend(importAttribute, el => el.name, el => this.visit(el, q));
        q.getAndSend(importAttribute, el => el.value, el => this.visitLeftPadded(el, q));
        return importAttribute;
    }

    override visitBinaryExtensions(binary: JS.Binary, q: RpcSendQueue): J | undefined {
        q.getAndSend(binary, el => el.left, el => this.visit(el, q));
        q.getAndSend(binary, el => el.operator, el => this.visitLeftPadded(el, q));
        q.getAndSend(binary, el => el.right, el => this.visit(el, q));
        q.getAndSend(binary, el => asRef(el.type), el => this.visitType(el, q));
        return binary;
    }

    override visitLiteralType(literalType: JS.LiteralType, q: RpcSendQueue): J | undefined {
        q.getAndSend(literalType, el => el.literal, el => this.visit(el, q));
        q.getAndSend(literalType, el => asRef(el.type), el => this.visitType(el, q));
        return literalType;
    }

    override visitMappedType(mappedType: JS.MappedType, q: RpcSendQueue): J | undefined {
        q.getAndSend(mappedType, el => el.prefixToken, el => this.visitLeftPadded(el, q));
        q.getAndSend(mappedType, el => el.hasReadonly, el => this.visitLeftPadded(el, q));
        q.getAndSend(mappedType, el => el.keysRemapping, el => this.visit(el, q));
        q.getAndSend(mappedType, el => el.suffixToken, el => this.visitLeftPadded(el, q));
        q.getAndSend(mappedType, el => el.hasQuestionToken, el => this.visitLeftPadded(el, q));
        q.getAndSend(mappedType, el => el.valueType, el => this.visitContainer(el, q));
        q.getAndSend(mappedType, el => asRef(el.type), el => this.visitType(el, q));
        return mappedType;
    }

    override visitMappedTypeKeysRemapping(keysRemapping: JS.MappedType.KeysRemapping, q: RpcSendQueue): J | undefined {
        q.getAndSend(keysRemapping, el => el.typeParameter, el => this.visitRightPadded(el, q));
        q.getAndSend(keysRemapping, el => el.nameType, el => this.visitRightPadded(el, q));
        return keysRemapping;
    }

    override visitMappedTypeParameter(mappedTypeParameter: JS.MappedType.Parameter, q: RpcSendQueue): J | undefined {
        q.getAndSend(mappedTypeParameter, el => el.name, el => this.visit(el, q));
        q.getAndSend(mappedTypeParameter, el => el.iterateType, el => this.visitLeftPadded(el, q));
        return mappedTypeParameter;
    }

    override visitObjectBindingPattern(objectBindingPattern: JS.ObjectBindingPattern, q: RpcSendQueue): J | undefined {
        q.getAndSendList(objectBindingPattern, el => el.leadingAnnotations, el => el.id, el => this.visit(el, q));
        q.getAndSendList(objectBindingPattern, el => el.modifiers, el => el.id, el => this.visit(el, q));
        q.getAndSend(objectBindingPattern, el => el.typeExpression, el => this.visit(el, q));
        q.getAndSend(objectBindingPattern, el => el.bindings, el => this.visitContainer(el, q));
        q.getAndSend(objectBindingPattern, el => el.initializer, el => this.visitLeftPadded(el, q));
        return objectBindingPattern;
    }

    override visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, q: RpcSendQueue): J | undefined {
        q.getAndSend(propertyAssignment, el => el.name, el => this.visitRightPadded(el, q));
        q.getAndSend(propertyAssignment, el => el.assigmentToken);
        q.getAndSend(propertyAssignment, el => el.initializer, el => this.visit(el, q));
        return propertyAssignment;
    }

    override visitSatisfiesExpression(satisfiesExpression: JS.SatisfiesExpression, q: RpcSendQueue): J | undefined {
        q.getAndSend(satisfiesExpression, el => el.expression, el => this.visit(el, q));
        q.getAndSend(satisfiesExpression, el => el.satisfiesType, el => this.visitLeftPadded(el, q));
        q.getAndSend(satisfiesExpression, el => asRef(el.type), el => this.visitType(el, q));
        return satisfiesExpression;
    }

    override visitScopedVariableDeclarations(scopedVariableDeclarations: JS.ScopedVariableDeclarations, q: RpcSendQueue): J | undefined {
        q.getAndSendList(scopedVariableDeclarations, el => el.modifiers, el => el.id, el => this.visit(el, q));
        q.getAndSendList(scopedVariableDeclarations, el => el.variables, el => el.element.id, el => this.visitRightPadded(el, q));
        return scopedVariableDeclarations;
    }

    override visitShebang(shebang: JS.Shebang, q: RpcSendQueue): J | undefined {
        q.getAndSend(shebang, el => el.text);
        return shebang;
    }

    override visitSpread(spread: JS.Spread, q: RpcSendQueue): J | undefined {
        q.getAndSend(spread, el => el.expression, el => this.visit(el, q));
        q.getAndSend(spread, el => asRef(el.type), el => this.visitType(el, q));
        return spread;
    }

    override visitStatementExpression(statementExpression: JS.StatementExpression, q: RpcSendQueue): J | undefined {
        q.getAndSend(statementExpression, el => el.statement, el => this.visit(el, q));
        return statementExpression;
    }

    override visitTaggedTemplateExpression(taggedTemplateExpression: JS.TaggedTemplateExpression, q: RpcSendQueue): J | undefined {
        q.getAndSend(taggedTemplateExpression, el => el.tag, el => this.visitRightPadded(el, q));
        q.getAndSend(taggedTemplateExpression, el => el.typeArguments, el => this.visitContainer(el, q));
        q.getAndSend(taggedTemplateExpression, el => el.templateExpression, el => this.visit(el, q));
        q.getAndSend(taggedTemplateExpression, el => asRef(el.type), el => this.visitType(el, q));
        return taggedTemplateExpression;
    }

    override visitTemplateExpression(templateExpression: JS.TemplateExpression, q: RpcSendQueue): J | undefined {
        q.getAndSend(templateExpression, el => el.head, el => this.visit(el, q));
        q.getAndSendList(templateExpression, el => el.spans, el => el.element.id, el => this.visitRightPadded(el, q));
        q.getAndSend(templateExpression, el => asRef(el.type), el => this.visitType(el, q));
        return templateExpression;
    }

    override visitTemplateExpressionSpan(span: JS.TemplateExpression.Span, q: RpcSendQueue): J | undefined {
        q.getAndSend(span, el => el.expression, el => this.visit(el, q));
        q.getAndSend(span, el => el.tail, el => this.visit(el, q));
        return span;
    }

    override visitTuple(tuple: JS.Tuple, q: RpcSendQueue): J | undefined {
        q.getAndSend(tuple, el => el.elements, el => this.visitContainer(el, q));
        q.getAndSend(tuple, el => asRef(el.type), el => this.visitType(el, q));
        return tuple;
    }

    override visitTypeDeclaration(typeDeclaration: JS.TypeDeclaration, q: RpcSendQueue): J | undefined {
        q.getAndSendList(typeDeclaration, el => el.modifiers, el => el.id, el => this.visit(el, q));
        q.getAndSend(typeDeclaration, el => el.name, el => this.visitLeftPadded(el, q));
        q.getAndSend(typeDeclaration, el => el.typeParameters, el => this.visit(el, q));
        q.getAndSend(typeDeclaration, el => el.initializer, el => this.visitLeftPadded(el, q));
        q.getAndSend(typeDeclaration, el => asRef(el.type), el => this.visitType(el, q));
        return typeDeclaration;
    }

    override visitTypeOf(typeOf: JS.TypeOf, q: RpcSendQueue): J | undefined {
        q.getAndSend(typeOf, el => el.expression, el => this.visit(el, q));
        q.getAndSend(typeOf, el => asRef(el.type), el => this.visitType(el, q));
        return typeOf;
    }

    override visitTypeTreeExpression(typeTreeExpression: JS.TypeTreeExpression, q: RpcSendQueue): J | undefined {
        q.getAndSend(typeTreeExpression, el => el.expression, el => this.visit(el, q));
        return typeTreeExpression;
    }

    override visitAs(as_: JS.As, q: RpcSendQueue): J | undefined {
        q.getAndSend(as_, el => el.left, el => this.visitRightPadded(el, q));
        q.getAndSend(as_, el => el.right, el => this.visit(el, q));
        q.getAndSend(as_, el => asRef(el.type), el => this.visitType(el, q));
        return as_;
    }

    override visitAssignmentOperationExtensions(assignmentOperation: JS.AssignmentOperation, q: RpcSendQueue): J | undefined {
        q.getAndSend(assignmentOperation, el => el.variable, el => this.visit(el, q));
        q.getAndSend(assignmentOperation, el => el.operator, el => this.visitLeftPadded(el, q));
        q.getAndSend(assignmentOperation, el => el.assignment, el => this.visit(el, q));
        q.getAndSend(assignmentOperation, el => asRef(el.type), el => this.visitType(el, q));
        return assignmentOperation;
    }

    override visitIndexedAccessType(indexedAccessType: JS.IndexedAccessType, q: RpcSendQueue): J | undefined {
        q.getAndSend(indexedAccessType, el => el.objectType, el => this.visit(el, q));
        q.getAndSend(indexedAccessType, el => el.indexType, el => this.visit(el, q));
        q.getAndSend(indexedAccessType, el => asRef(el.type), el => this.visitType(el, q));
        return indexedAccessType;
    }

    override visitIndexedAccessTypeIndexType(indexType: JS.IndexedAccessType.IndexType, q: RpcSendQueue): J | undefined {
        q.getAndSend(indexType, el => el.element, el => this.visitRightPadded(el, q));
        q.getAndSend(indexType, el => asRef(el.type), el => this.visitType(el, q));
        return indexType;
    }

    override visitTypeQuery(typeQuery: JS.TypeQuery, q: RpcSendQueue): J | undefined {
        q.getAndSend(typeQuery, el => el.typeExpression, el => this.visit(el, q));
        q.getAndSend(typeQuery, el => el.typeArguments, el => this.visitContainer(el, q));
        q.getAndSend(typeQuery, el => asRef(el.type), el => this.visitType(el, q));
        return typeQuery;
    }

    override visitTypeInfo(typeInfo: JS.TypeInfo, q: RpcSendQueue): J | undefined {
        q.getAndSend(typeInfo, el => el.typeIdentifier, el => this.visit(el, q));
        return typeInfo;
    }

    override visitComputedPropertyName(computedPropertyName: JS.ComputedPropertyName, q: RpcSendQueue): J | undefined {
        q.getAndSend(computedPropertyName, el => el.expression, el => this.visitRightPadded(el, q));
        return computedPropertyName;
    }

    override visitTypeOperator(typeOperator: JS.TypeOperator, q: RpcSendQueue): J | undefined {
        q.getAndSend(typeOperator, el => el.operator);
        q.getAndSend(typeOperator, el => el.expression, el => this.visitLeftPadded(el, q));
        return typeOperator;
    }

    override visitTypePredicate(typePredicate: JS.TypePredicate, q: RpcSendQueue): J | undefined {
        q.getAndSend(typePredicate, el => el.asserts, el => this.visitLeftPadded(el, q));
        q.getAndSend(typePredicate, el => el.parameterName, el => this.visit(el, q));
        q.getAndSend(typePredicate, el => el.expression, el => this.visitLeftPadded(el, q));
        q.getAndSend(typePredicate, el => asRef(el.type), el => this.visitType(el, q));
        return typePredicate;
    }

    override visitUnion(union: JS.Union, q: RpcSendQueue): J | undefined {
        q.getAndSendList(union, el => el.types, el => el.element.id, el => this.visitRightPadded(el, q));
        q.getAndSend(union, el => asRef(el.type), el => this.visitType(el, q));
        return union;
    }

    override visitIntersection(intersection: JS.Intersection, q: RpcSendQueue): J | undefined {
        q.getAndSendList(intersection, el => el.types, el => el.element.id, el => this.visitRightPadded(el, q));
        q.getAndSend(intersection, el => asRef(el.type), el => this.visitType(el, q));
        return intersection;
    }

    override visitVoid(void_: JS.Void, q: RpcSendQueue): J | undefined {
        q.getAndSend(void_, el => el.expression, el => this.visit(el, q));
        return void_;
    }

    override visitWithStatement(withStatement: JS.WithStatement, q: RpcSendQueue): J | undefined {
        q.getAndSend(withStatement, el => el.expression, el => this.visit(el, q));
        q.getAndSend(withStatement, el => el.body, el => this.visitRightPadded(el, q));
        return withStatement;
    }

    override visitJsxTag(tag: JSX.Tag, q: RpcSendQueue): J | undefined {
        q.getAndSend(tag, el => el.openName, el => this.visitLeftPadded(el, q));
        q.getAndSend(tag, el => el.typeArguments, el => this.visitContainer(el, q));
        q.getAndSend(tag, el => el.afterName, space => this.visitSpace(space, q));
        q.getAndSendList(tag, el => el.attributes, attr => attr.element.id, attr => this.visitRightPadded(attr, q));

        q.getAndSend(tag, el => el.selfClosing, space => this.visitSpace(space, q));
        q.getAndSendList(tag, el => el.children!, child => child.id, child => this.visit(child, q));
        q.getAndSend(tag, el => el.closingName, el => this.visitLeftPadded(el, q));
        q.getAndSend(tag, el => el.afterClosingName, el => this.visitSpace(el, q));

        return tag;
    }

    override visitJsxAttribute(attribute: JSX.Attribute, q: RpcSendQueue): J | undefined {
        q.getAndSend(attribute, el => el.key, el => this.visit(el, q));
        q.getAndSend(attribute, el => el.value, el => this.visitLeftPadded(el, q));
        return attribute;
    }

    override visitJsxSpreadAttribute(spreadAttribute: JSX.SpreadAttribute, q: RpcSendQueue): J | undefined {
        q.getAndSend(spreadAttribute, el => el.dots, space => this.visitSpace(space, q));
        q.getAndSend(spreadAttribute, el => el.expression, el => this.visitRightPadded(el, q));
        return spreadAttribute;
    }

    override visitJsxEmbeddedExpression(embeddedExpression: JSX.EmbeddedExpression, q: RpcSendQueue): J | undefined {
        q.getAndSend(embeddedExpression, el => el.expression, el => this.visitRightPadded(el, q));
        return embeddedExpression;
    }

    override visitJsxNamespacedName(namespacedName: JSX.NamespacedName, q: RpcSendQueue): J | undefined {
        q.getAndSend(namespacedName, el => el.namespace, el => this.visit(el, q));
        q.getAndSend(namespacedName, el => el.name, el => this.visitLeftPadded(el, q));
        return namespacedName;
    }

    override visitIndexSignatureDeclaration(indexSignatureDeclaration: JS.IndexSignatureDeclaration, q: RpcSendQueue): J | undefined {
        q.getAndSendList(indexSignatureDeclaration, el => el.modifiers, el => el.id, el => this.visit(el, q));
        q.getAndSend(indexSignatureDeclaration, el => el.parameters, el => this.visitContainer(el, q));
        q.getAndSend(indexSignatureDeclaration, el => el.typeExpression, el => this.visitLeftPadded(el, q));
        q.getAndSend(indexSignatureDeclaration, el => asRef(el.type), el => this.visitType(el, q));
        return indexSignatureDeclaration;
    }

    override visitComputedPropertyMethodDeclaration(computedPropMethod: JS.ComputedPropertyMethodDeclaration, q: RpcSendQueue): J | undefined {
        q.getAndSendList(computedPropMethod, el => el.leadingAnnotations, el => el.id, el => this.visit(el, q));
        q.getAndSendList(computedPropMethod, el => el.modifiers, el => el.id, el => this.visit(el, q));
        q.getAndSend(computedPropMethod, el => el.typeParameters, el => this.visit(el, q));
        q.getAndSend(computedPropMethod, el => el.returnTypeExpression, el => this.visit(el, q));
        q.getAndSend(computedPropMethod, el => el.name, el => this.visit(el, q));
        q.getAndSend(computedPropMethod, el => el.parameters, el => this.visitContainer(el, q));
        q.getAndSend(computedPropMethod, el => el.body, el => this.visit(el, q));
        q.getAndSend(computedPropMethod, el => el.methodType, el => this.visitType(el, q));
        return computedPropMethod;
    }

    override visitForOfLoop(forOfLoop: JS.ForOfLoop, q: RpcSendQueue): J | undefined {
        q.getAndSend(forOfLoop, el => el.await, space => this.visitSpace(space, q));
        q.getAndSend(forOfLoop, el => el.loop, el => this.visit(el, q));
        return forOfLoop;
    }

    override visitForInLoop(forInLoop: JS.ForInLoop, q: RpcSendQueue): J | undefined {
        q.getAndSend(forInLoop, el => el.control, el => this.visit(el, q));
        q.getAndSend(forInLoop, el => el.body, el => this.visitRightPadded(el, q));
        return forInLoop;
    }

    override visitNamespaceDeclaration(namespaceDeclaration: JS.NamespaceDeclaration, q: RpcSendQueue): J | undefined {
        q.getAndSendList(namespaceDeclaration, el => el.modifiers, el => el.id, el => this.visit(el, q));
        q.getAndSend(namespaceDeclaration, el => el.keywordType, el => this.visitLeftPadded(el, q));
        q.getAndSend(namespaceDeclaration, el => el.name, el => this.visitRightPadded(el, q));
        q.getAndSend(namespaceDeclaration, el => el.body, el => this.visit(el, q));
        return namespaceDeclaration;
    }

    override visitTypeLiteral(typeLiteral: JS.TypeLiteral, q: RpcSendQueue): J | undefined {
        q.getAndSend(typeLiteral, el => el.members, el => this.visit(el, q));
        q.getAndSend(typeLiteral, el => asRef(el.type), el => this.visitType(el, q));
        return typeLiteral;
    }

    override visitArrayBindingPattern(arrayBindingPattern: JS.ArrayBindingPattern, q: RpcSendQueue): J | undefined {
        q.getAndSend(arrayBindingPattern, el => el.elements, el => this.visitContainer(el, q));
        q.getAndSend(arrayBindingPattern, el => asRef(el.type), el => this.visitType(el, q));
        return arrayBindingPattern;
    }

    override visitBindingElement(bindingElement: JS.BindingElement, q: RpcSendQueue): J | undefined {
        q.getAndSend(bindingElement, el => el.propertyName, el => this.visitRightPadded(el, q));
        q.getAndSend(bindingElement, el => el.name, el => this.visit(el, q));
        q.getAndSend(bindingElement, el => el.initializer, el => this.visitLeftPadded(el, q));
        q.getAndSend(bindingElement, el => el.variableType, el => this.visitType(el, q));
        return bindingElement;
    }

    override visitExportDeclaration(exportDeclaration: JS.ExportDeclaration, q: RpcSendQueue): J | undefined {
        q.getAndSendList(exportDeclaration, el => el.modifiers, el => el.id, el => this.visit(el, q));
        q.getAndSend(exportDeclaration, el => el.typeOnly, el => this.visitLeftPadded(el, q));
        q.getAndSend(exportDeclaration, el => el.exportClause, el => this.visit(el, q));
        q.getAndSend(exportDeclaration, el => el.moduleSpecifier, el => this.visitLeftPadded(el, q));
        q.getAndSend(exportDeclaration, el => el.attributes, el => this.visit(el, q));
        return exportDeclaration;
    }

    override visitExportAssignment(exportAssignment: JS.ExportAssignment, q: RpcSendQueue): J | undefined {
        q.getAndSend(exportAssignment, el => el.exportEquals);
        q.getAndSend(exportAssignment, el => el.expression, el => this.visitLeftPadded(el, q));
        return exportAssignment;
    }

    override visitNamedExports(namedExports: JS.NamedExports, q: RpcSendQueue): J | undefined {
        q.getAndSend(namedExports, el => el.elements, el => this.visitContainer(el, q));
        q.getAndSend(namedExports, el => asRef(el.type), el => this.visitType(el, q));
        return namedExports;
    }

    override visitExportSpecifier(exportSpecifier: JS.ExportSpecifier, q: RpcSendQueue): J | undefined {
        q.getAndSend(exportSpecifier, el => el.typeOnly, el => this.visitLeftPadded(el, q));
        q.getAndSend(exportSpecifier, el => el.specifier, el => this.visit(el, q));
        q.getAndSend(exportSpecifier, el => asRef(el.type), el => this.visitType(el, q));
        return exportSpecifier;
    }

    override visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, q: RpcSendQueue): J.RightPadded<T> {
        return this.delegate.visitRightPadded(right, q);
    }

    override visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, q: RpcSendQueue): J.LeftPadded<T> {
        return this.delegate.visitLeftPadded(left, q);
    }

    override visitContainer<T extends J>(container: J.Container<T>, q: RpcSendQueue): J.Container<T> {
        return this.delegate.visitContainer(container, q);
    }

    override visitSpace(space: J.Space, q: RpcSendQueue): J.Space {
        return this.delegate.visitSpace(space, q);
    }

    override visitType(javaType: Type | undefined, q: RpcSendQueue): Type | undefined {
        return this.delegate.visitType(javaType, q);
    }
}

class JavaScriptDelegateSender extends JavaSender {
    private javascriptSender: JavaScriptSender;

    constructor(javascriptSender: JavaScriptSender) {
        super();
        this.javascriptSender = javascriptSender;
    }

    visit<R extends J>(tree: Tree, p: RpcSendQueue, parent?: Cursor): R | undefined {
        // Delegate to JavaScript sender if this is a JavaScript element
        if (isJavaScript(tree)) {
            return this.javascriptSender.visit(tree, p, parent);
        }

        // Otherwise handle as a Java element
        return super.visit(tree, p, parent);
    }
}

class JavaScriptDelegateReceiver extends JavaReceiver {
    private javascriptReceiver: JavaScriptReceiver;

    constructor(javascriptReceiver: JavaScriptReceiver) {
        super();
        this.javascriptReceiver = javascriptReceiver;
    }

    override visit<R extends J>(tree: Tree | undefined, q: RpcReceiveQueue, _parent?: Cursor): R | undefined {
        if (!tree) return undefined;

        // Delegate to JavaScript receiver if this is a JavaScript element
        if (isJavaScript(tree)) {
            return this.javascriptReceiver.visit(tree, q) as R | undefined;
        }

        // Otherwise handle as a Java element
        return super.visit(tree as J, q) as R | undefined;
    }
}

class JavaScriptReceiver extends JavaScriptVisitor<RpcReceiveQueue> {
    private readonly _delegate: JavaScriptDelegateReceiver;

    constructor() {
        super();
        this._delegate = new JavaScriptDelegateReceiver(this);
    }

    // Override visit() to skip cursor handling for performance
    public override visit<R extends J>(tree: Tree | undefined, q: RpcReceiveQueue, _parent?: Cursor): R | undefined {
        if (!tree) return undefined;

        // Only call preVisit for JavaScript elements - Java elements are handled by the delegate
        if (isJavaScript(tree)) {
            let result = this.preVisit(tree as JS, q);
            if (result === undefined) return undefined;
            return this.accept(result as JS, q) as R | undefined;
        }

        // Otherwise delegate to Java receiver (which handles its own preVisit)
        return this._delegate.visit(tree, q) as R | undefined;
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

    // Delegate helper methods to JavaReceiver
    public override visitSpace(space: J.Space, q: RpcReceiveQueue): J.Space {
        return this._delegate.visitSpace(space, q);
    }

    public visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, q: RpcReceiveQueue): J.LeftPadded<T> {
        return this._delegate.visitLeftPadded(left, q);
    }

    public visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, q: RpcReceiveQueue): J.RightPadded<T> {
        return this._delegate.visitRightPadded(right, q);
    }

    public visitContainer<T extends J>(container: J.Container<T>, q: RpcReceiveQueue): J.Container<T> {
        return this._delegate.visitContainer(container, q);
    }

    protected override visitType(javaType: Type | undefined, q: RpcReceiveQueue): Type | undefined {
        return this._delegate.visitType(javaType, q);
    }
}

// Register codecs for JavaScript
registerJLanguageCodecs(JS.Kind.CompilationUnit, new JavaScriptReceiver(), new JavaScriptSender(), JS.Kind);
