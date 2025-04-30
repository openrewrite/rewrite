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
import {
    isJavaScript,
    JavaScriptKind,
    JS,
} from "./tree";
import {
    isJava,
    isSpace,
    J,
    JavaType
} from "../java";
import {produceAsync} from "../visitor";
import {createDraft, Draft, finishDraft} from "immer";
import {JavaReceiver, JavaSender} from "../java/rpc";
import {Cursor, Tree} from "../tree";

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

    protected async preVisit(j: JS, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(j, j2 => j2.id);
        await q.getAndSend(j, j2 => asRef(j2.prefix), space => this.visitSpace(space, q));
        await q.sendMarkers(j, j2 => j2.markers);
        return j;
    }

    protected async visitAlias(alias: JS.Alias, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(alias, a => a.propertyName, propName => this.visitRightPadded(propName, q));
        await q.getAndSend(alias, a => a.alias, alias => this.visit(alias, q));
        return alias;
    }

    protected async visitArrowFunction(arrowFunction: JS.ArrowFunction, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(arrowFunction, a => a.leadingAnnotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(arrowFunction, a => a.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(arrowFunction, a => a.typeParameters, params => this.visit(params, q));
        await q.getAndSend(arrowFunction, a => a.parameters, params => this.visit(params, q));
        await q.getAndSend(arrowFunction, a => a.returnTypeExpression, type => this.visit(type, q));
        await q.getAndSend(arrowFunction, a => a.body, body => this.visitLeftPadded(body, q));
        await q.getAndSend(arrowFunction, a => asRef(a.type), type => this.visitType(type, q));
        return arrowFunction;
    }

    protected async visitAwait(anAwait: JS.Await, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(anAwait, a => a.expression, expr => this.visit(expr, q));
        await q.getAndSend(anAwait, a => asRef(a.type), type => this.visitType(type, q));
        return anAwait;
    }

    protected async visitJSCompilationUnit(cu: JS.CompilationUnit, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(cu, c => c.sourcePath);
        await q.getAndSend(cu, c => c.charsetName);
        await q.getAndSend(cu, c => c.charsetBomMarked);
        await q.getAndSend(cu, c => c.checksum);
        await q.getAndSend(cu, c => c.fileAttributes);
        await q.getAndSendList(cu, c => c.imports, imp => imp.element.id, imp => this.visitRightPadded(imp, q));
        await q.getAndSendList(cu, c => c.statements, stmt => stmt.element.id, stmt => this.visitRightPadded(stmt, q));
        await q.getAndSend(cu, c => asRef(c.eof), space => this.visitSpace(space, q));
        return cu;
    }

    protected async visitConditionalType(conditionalType: JS.ConditionalType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(conditionalType, c => c.checkType, type => this.visit(type, q));
        await q.getAndSend(conditionalType, c => c.condition, cond => this.visitContainer(cond, q));
        await q.getAndSend(conditionalType, c => asRef(c.type), type => this.visitType(type, q));
        return conditionalType;
    }

    protected async visitDefaultType(defaultType: JS.DefaultType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(defaultType, d => d.left, left => this.visit(left, q));
        await q.getAndSend(defaultType, d => asRef(d.beforeEquals), space => this.visitSpace(space, q));
        await q.getAndSend(defaultType, d => d.right, right => this.visit(right, q));
        await q.getAndSend(defaultType, d => asRef(d.type), type => this.visitType(type, q));
        return defaultType;
    }

    protected async visitDelete(deleteExpr: JS.Delete, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(deleteExpr, d => d.expression, expr => this.visit(expr, q));
        await q.getAndSend(deleteExpr, d => asRef(d.type), type => this.visitType(type, q));
        return deleteExpr;
    }

    protected async visitExport(anExport: JS.Export, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(anExport, e => e.exports, exports => this.visitContainer(exports, q));
        await q.getAndSend(anExport, e => asRef(e.from), space => this.visitSpace(space, q));
        await q.getAndSend(anExport, e => e.target, target => this.visit(target, q));
        await q.getAndSend(anExport, e => e.initializer, init => this.visitLeftPadded(init, q));
        return anExport;
    }

    protected async visitExpressionStatement(exprStmt: JS.ExpressionStatement, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(exprStmt, e => e.expression, expr => this.visit(expr, q));
        return exprStmt;
    }

    protected async visitTrailingTokenStatement(trailStmt: JS.TrailingTokenStatement, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(trailStmt, t => t.expression, expr => this.visitRightPadded(expr, q));
        await q.getAndSend(trailStmt, t => asRef(t.type), type => this.visitType(type, q));
        return trailStmt;
    }

    protected async visitExpressionWithTypeArguments(exprType: JS.ExpressionWithTypeArguments, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(exprType, e => e.clazz, clazz => this.visit(clazz, q));
        await q.getAndSend(exprType, e => e.typeArguments, args => this.visitContainer(args, q));
        await q.getAndSend(exprType, e => asRef(e.type), type => this.visitType(type, q));
        return exprType;
    }

    protected async visitFunctionType(functionType: JS.FunctionType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(functionType, f => f.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(functionType, f => f.constructorType, ct => this.visitLeftPadded(ct, q));
        await q.getAndSend(functionType, f => f.typeParameters, params => this.visit(params, q));
        await q.getAndSend(functionType, f => f.parameters, params => this.visitContainer(params, q));
        await q.getAndSend(functionType, f => f.returnType, returnType => this.visitLeftPadded(returnType, q));
        await q.getAndSend(functionType, f => asRef(f.type), type => this.visitType(type, q));
        return functionType;
    }

    protected async visitImportAttribute(importAttr: JS.ImportAttribute, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(importAttr, i => i.name, name => this.visit(name, q));
        await q.getAndSend(importAttr, i => i.value, value => this.visitLeftPadded(value, q));
        return importAttr;
    }

    protected async visitImportAttributes(importAttrs: JS.ImportAttributes, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(importAttrs, i => i.token);
        await q.getAndSend(importAttrs, i => i.elements, elements => this.visitContainer(elements, q));
        return importAttrs;
    }

    protected async visitImportType(importType: JS.ImportType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(importType, i => i.hasTypeof, hasTypeof => this.visitRightPadded(hasTypeof, q));
        await q.getAndSend(importType, i => i.argumentAndAttributes, args => this.visitContainer(args, q));
        await q.getAndSend(importType, i => i.qualifier, qualifier => this.visitLeftPadded(qualifier, q));
        await q.getAndSend(importType, i => i.typeArguments, args => this.visitContainer(args, q));
        await q.getAndSend(importType, i => asRef(i.type), type => this.visitType(type, q));
        return importType;
    }

    protected async visitImportTypeAttributes(importTypeAttrs: JS.ImportTypeAttributes, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(importTypeAttrs, i => i.token, token => this.visitRightPadded(token, q));
        await q.getAndSend(importTypeAttrs, i => i.elements, elements => this.visitContainer(elements, q));
        await q.getAndSend(importTypeAttrs, i => asRef(i.end), space => this.visitSpace(space, q));
        return importTypeAttrs;
    }

    protected async visitInferType(inferType: JS.InferType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(inferType, i => i.typeParameter, param => this.visitLeftPadded(param, q));
        await q.getAndSend(inferType, i => asRef(i.type), type => this.visitType(type, q));
        return inferType;
    }

    protected async visitJsImport(jsImport: JS.JsImport, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(jsImport, i => i.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(jsImport, i => i.importClause, clause => this.visit(clause, q));
        await q.getAndSend(jsImport, i => i.moduleSpecifier, spec => this.visitLeftPadded(spec, q));
        await q.getAndSend(jsImport, i => i.attributes, attrs => this.visit(attrs, q));
        return jsImport;
    }

    protected async visitJsImportClause(jsImportClause: JS.JsImportClause, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(jsImportClause, c => c.typeOnly);
        await q.getAndSend(jsImportClause, c => c.name, name => this.visitRightPadded(name, q));
        await q.getAndSend(jsImportClause, c => c.namedBindings, bindings => this.visit(bindings, q));
        return jsImportClause;
    }

    protected async visitNamedImports(namedImports: JS.NamedImports, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(namedImports, n => n.elements, elements => this.visitContainer(elements, q));
        await q.getAndSend(namedImports, n => asRef(n.type), type => this.visitType(type, q));
        return namedImports;
    }

    protected async visitJsImportSpecifier(jsImportSpecifier: JS.JsImportSpecifier, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(jsImportSpecifier, i => i.importType, importType => this.visitLeftPadded(importType, q));
        await q.getAndSend(jsImportSpecifier, i => i.specifier, specifier => this.visit(specifier, q));
        await q.getAndSend(jsImportSpecifier, i => asRef(i.type), type => this.visitType(type, q));
        return jsImportSpecifier;
    }

    protected async visitJSVariableDeclarations(varDecls: JS.JSVariableDeclarations, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(varDecls, v => v.leadingAnnotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(varDecls, v => v.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(varDecls, v => v.typeExpression, type => this.visit(type, q));
        await q.getAndSend(varDecls, v => asRef(v.varargs), space => this.visitSpace(space, q));
        await q.getAndSendList(varDecls, v => v.variables, variable => variable.element.id, variable => this.visitRightPadded(variable, q));
        return varDecls;
    }

    protected async visitJSNamedVariable(variable: JS.JSVariableDeclarations.JSNamedVariable, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(variable, v => v.name, name => this.visit(name, q));
        await q.getAndSendList(variable, v => v.dimensionsAfterName, dim => dim.element, dim => this.visitLeftPadded(dim, q));
        await q.getAndSend(variable, v => v.initializer, init => this.visitLeftPadded(init, q));
        await q.getAndSend(variable, v => asRef(v.variableType), type => this.visitType(type, q));
        return variable;
    }

    protected async visitJSMethodDeclaration(methodDecl: JS.JSMethodDeclaration, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(methodDecl, m => m.leadingAnnotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(methodDecl, m => m.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(methodDecl, m => m.typeParameters, param => this.visit(param, q));
        await q.getAndSend(methodDecl, m => m.returnTypeExpression, type => this.visit(type, q));
        await q.getAndSend(methodDecl, m => m.name, name => this.visit(name, q));
        await q.getAndSend(methodDecl, m => m.parameters, params => this.visitContainer(params, q));
        await q.getAndSend(methodDecl, m => m.body, body => this.visit(body, q));
        return methodDecl;
    }

    protected async visitJsBinary(jsBinary: JS.JsBinary, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(jsBinary, b => b.left, left => this.visit(left, q));
        await q.getAndSend(jsBinary, b => b.operator, op => this.visitLeftPadded(op, q));
        await q.getAndSend(jsBinary, b => b.right, right => this.visit(right, q));
        await q.getAndSend(jsBinary, b => asRef(b.type), type => this.visitType(type, q));
        return jsBinary;
    }

    protected async visitLiteralType(literalType: JS.LiteralType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(literalType, l => l.literal, literal => this.visit(literal, q));
        await q.getAndSend(literalType, l => asRef(l.type), type => this.visitType(type, q));
        return literalType;
    }

    protected async visitMappedType(mappedType: JS.MappedType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(mappedType, m => m.prefixToken, token => this.visitLeftPadded(token, q));
        await q.getAndSend(mappedType, m => m.hasReadonly, readonly => this.visitLeftPadded(readonly, q));
        await q.getAndSend(mappedType, m => m.keysRemapping, keys => this.visit(keys, q));
        await q.getAndSend(mappedType, m => m.suffixToken, token => this.visitLeftPadded(token, q));
        await q.getAndSend(mappedType, m => m.hasQuestionToken, token => this.visitLeftPadded(token, q));
        await q.getAndSend(mappedType, m => m.valueType, type => this.visitContainer(type, q));
        await q.getAndSend(mappedType, m => asRef(m.type), type => this.visitType(type, q));
        return mappedType;
    }

    protected async visitMappedTypeKeysRemapping(keysRemapping: JS.MappedType.KeysRemapping, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(keysRemapping, k => k.typeParameter, param => this.visitRightPadded(param, q));
        await q.getAndSend(keysRemapping, k => k.nameType, nameType => this.visitRightPadded(nameType, q));
        return keysRemapping;
    }

    protected async visitMappedTypeMappedTypeParameter(param: JS.MappedType.MappedTypeParameter, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(param, p => p.name, name => this.visit(name, q));
        await q.getAndSend(param, p => p.iterateType, type => this.visitLeftPadded(type, q));
        return param;
    }

    protected async visitObjectBindingDeclarations(objectBindings: JS.ObjectBindingDeclarations, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(objectBindings, o => o.leadingAnnotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(objectBindings, o => o.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(objectBindings, o => o.typeExpression, expr => this.visit(expr, q));
        await q.getAndSend(objectBindings, o => o.bindings, bindings => this.visitContainer(bindings, q));
        await q.getAndSend(objectBindings, o => o.initializer, init => this.visitLeftPadded(init, q));
        return objectBindings;
    }

    protected async visitPropertyAssignment(propAssign: JS.PropertyAssignment, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(propAssign, p => p.name, name => this.visitRightPadded(name, q));
        await q.getAndSend(propAssign, p => p.assigmentToken);
        await q.getAndSend(propAssign, p => p.initializer, init => this.visit(init, q));
        return propAssign;
    }

    protected async visitSatisfiesExpression(satisfiesExpr: JS.SatisfiesExpression, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(satisfiesExpr, s => s.expression, expr => this.visit(expr, q));
        await q.getAndSend(satisfiesExpr, s => s.satisfiesType, type => this.visitLeftPadded(type, q));
        await q.getAndSend(satisfiesExpr, s => asRef(s.type), type => this.visitType(type, q));
        return satisfiesExpr;
    }

    protected async visitScopedVariableDeclarations(scopedVarDecls: JS.ScopedVariableDeclarations, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(scopedVarDecls, s => s.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(scopedVarDecls, s => s.scope, scope => this.visitLeftPadded(scope, q));
        await q.getAndSendList(scopedVarDecls, s => s.variables, v => v.element.id, v => this.visitRightPadded(v, q));

        return scopedVarDecls;
    }

    protected async visitStatementExpression(stmtExpr: JS.StatementExpression, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(stmtExpr, s => s.statement, stmt => this.visit(stmt, q));
        return stmtExpr;
    }

    protected async visitTaggedTemplateExpression(taggedTemplate: JS.TaggedTemplateExpression, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(taggedTemplate, t => t.tag, tag => this.visitRightPadded(tag, q));
        await q.getAndSend(taggedTemplate, t => t.typeArguments, args => this.visitContainer(args, q));
        await q.getAndSend(taggedTemplate, t => t.templateExpression, expr => this.visit(expr, q));
        await q.getAndSend(taggedTemplate, t => asRef(t.type), type => this.visitType(type, q));
        return taggedTemplate;
    }

    protected async visitTemplateExpression(template: JS.TemplateExpression, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(template, t => t.head, head => this.visit(head, q));
        await q.getAndSendList(template, t => t.templateSpans, span => span.element.id, span => this.visitRightPadded(span, q));
        await q.getAndSend(template, t => asRef(t.type), type => this.visitType(type, q));
        return template;
    }

    protected async visitTemplateExpressionTemplateSpan(span: JS.TemplateExpression.TemplateSpan, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(span, s => s.expression, expr => this.visit(expr, q));
        await q.getAndSend(span, s => s.tail, tail => this.visit(tail, q));
        return span;
    }

    protected async visitTuple(tuple: JS.Tuple, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(tuple, t => t.elements, elements => this.visitContainer(elements, q));
        await q.getAndSend(tuple, t => asRef(t.type), type => this.visitType(type, q));
        return tuple;
    }

    protected async visitTypeDeclaration(typeDecl: JS.TypeDeclaration, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(typeDecl, t => t.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(typeDecl, t => t.name, name => this.visitLeftPadded(name, q));
        await q.getAndSend(typeDecl, t => t.typeParameters, params => this.visit(params, q));
        await q.getAndSend(typeDecl, t => t.initializer, init => this.visitLeftPadded(init, q));
        await q.getAndSend(typeDecl, t => asRef(t.type), type => this.visitType(type, q));
        return typeDecl;
    }

    protected async visitTypeOf(typeOf: JS.TypeOf, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typeOf, t => t.expression, expr => this.visit(expr, q));
        await q.getAndSend(typeOf, t => asRef(t.type), type => this.visitType(type, q));
        return typeOf;
    }

    protected async visitTypeTreeExpression(typeTreeExpression: JS.TypeTreeExpression, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typeTreeExpression, t => t.expression, expr => this.visit(expr, q));
        return typeTreeExpression;
    }

    protected async visitJsUnary(unary: JS.Unary, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(unary, u => u.operator, op => this.visitLeftPadded(op, q));
        await q.getAndSend(unary, u => u.expression, expr => this.visit(expr, q));
        await q.getAndSend(unary, u => asRef(u.type), type => this.visitType(type, q));

        return unary;
    }

    protected async visitJsAssignmentOperation(assignOp: JS.JsAssignmentOperation, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(assignOp, a => a.variable, variable => this.visit(variable, q));
        await q.getAndSend(assignOp, a => a.operator, op => this.visitLeftPadded(op, q));
        await q.getAndSend(assignOp, a => a.assignment, assignment => this.visit(assignment, q));
        await q.getAndSend(assignOp, a => asRef(a.type), type => this.visitType(type, q));

        return assignOp;
    }

    protected async visitIndexedAccessType(indexedAccessType: JS.IndexedAccessType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(indexedAccessType, i => i.objectType, type => this.visit(type, q));
        await q.getAndSend(indexedAccessType, i => i.indexType, indexType => this.visit(indexType, q));
        await q.getAndSend(indexedAccessType, i => asRef(i.type), type => this.visitType(type, q));

        return indexedAccessType;
    }

    protected async visitIndexedAccessTypeIndexType(indexType: JS.IndexedAccessType.IndexType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(indexType, i => i.element, element => this.visitRightPadded(element, q));
        await q.getAndSend(indexType, i => asRef(i.type), type => this.visitType(type, q));

        return indexType;
    }

    protected async visitIndexSignatureDeclaration(indexSignature: JS.IndexSignatureDeclaration, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(indexSignature, i => i.modifiers, m => this.visit(m, q));
        await q.getAndSend(indexSignature, i => i.parameters, p => this.visitContainer(p, q));
        await q.getAndSend(indexSignature, i => i.typeExpression, expr => this.visitLeftPadded(expr, q));
        await q.getAndSend(indexSignature, i => asRef(i.type), type => this.visitType(type, q));

        return indexSignature;
    }

    protected async visitTypeQuery(typeQuery: JS.TypeQuery, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typeQuery, t => t.typeExpression, expr => this.visit(expr, q));
        await q.getAndSend(typeQuery, t => t.typeArguments, args => this.visitContainer(args, q));
        await q.getAndSend(typeQuery, t => asRef(t.type), type => this.visitType(type, q));

        return typeQuery;
    }

    protected async visitTypeInfo(typeInfo: JS.TypeInfo, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typeInfo, t => t.typeIdentifier, identifier => this.visit(identifier, q));

        return typeInfo;
    }

    protected async visitTypeOperator(typeOperator: JS.TypeOperator, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typeOperator, t => t.operator);
        await q.getAndSend(typeOperator, t => t.expression, expr => this.visitLeftPadded(expr, q));

        return typeOperator;
    }

    protected async visitTypePredicate(typePredicate: JS.TypePredicate, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typePredicate, t => t.asserts, asserts => this.visitLeftPadded(asserts, q));
        await q.getAndSend(typePredicate, t => t.parameterName, name => this.visit(name, q));
        await q.getAndSend(typePredicate, t => t.expression, expr => this.visitLeftPadded(expr, q));
        await q.getAndSend(typePredicate, t => asRef(t.type), type => this.visitType(type, q));

        return typePredicate;
    }

    protected async visitUnion(union: JS.Union, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(union, u => u.types, type => type.element.id, type => this.visitRightPadded(type, q));
        await q.getAndSend(union, u => asRef(u.type), type => this.visitType(type, q));

        return union;
    }

    protected async visitIntersection(intersection: JS.Intersection, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(intersection, i => i.types, type => type.element.id, type => this.visitRightPadded(type, q));
        await q.getAndSend(intersection, i => asRef(i.type), type => this.visitType(type, q));

        return intersection;
    }

    protected async visitVoid(void_: JS.Void, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(void_, v => v.expression, expr => this.visit(expr, q));

        return void_;
    }

    protected async visitJSYield(yield_: JS.Yield, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(yield_, y => y.delegated, delegated => this.visitLeftPadded(delegated, q));
        await q.getAndSend(yield_, y => y.expression, expr => this.visit(expr, q));

        return yield_;
    }

    protected async visitWithStatement(withStmt: JS.WithStatement, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(withStmt, w => w.expression, expr => this.visit(expr, q));
        await q.getAndSend(withStmt, w => w.body, body => this.visitRightPadded(body, q));

        return withStmt;
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

    protected async visitAlias(alias: JS.Alias, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(alias);

        draft.propertyName = await q.receive(alias.propertyName, propName => this.visitRightPadded(propName, q));
        draft.alias = await q.receive(alias.alias, alias => this.visit(alias, q));

        return finishDraft(draft);
    }

    protected async visitArrowFunction(arrowFunction: JS.ArrowFunction, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(arrowFunction);

        draft.leadingAnnotations = await q.receiveListDefined(arrowFunction.leadingAnnotations, annot => this.visit(annot, q));
        draft.modifiers = await q.receiveListDefined(arrowFunction.modifiers, mod => this.visit(mod, q));
        draft.typeParameters = await q.receive(arrowFunction.typeParameters, params => this.visit(params, q));
        draft.parameters = await q.receive(arrowFunction.parameters, params => this.visit(params, q));
        draft.returnTypeExpression = await q.receive(arrowFunction.returnTypeExpression, type => this.visit(type, q));
        draft.body = await q.receive(arrowFunction.body, body => this.visitLeftPadded(body, q));
        draft.type = await q.receive(arrowFunction.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitAwait(await_: JS.Await, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(await_);

        draft.expression = await q.receive(await_.expression, expr => this.visit(expr, q));
        draft.type = await q.receive(await_.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitJSCompilationUnit(cu: JS.CompilationUnit, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(cu);

        draft.sourcePath = await q.receive(cu.sourcePath);
        draft.charsetName = await q.receive(cu.charsetName);
        draft.charsetBomMarked = await q.receive(cu.charsetBomMarked);
        draft.checksum = await q.receive(cu.checksum);
        draft.fileAttributes = await q.receive(cu.fileAttributes);
        draft.imports = await q.receiveListDefined(cu.imports, imp => this.visitRightPadded(imp, q));
        draft.statements = await q.receiveListDefined(cu.statements, stmt => this.visitRightPadded(stmt, q));
        draft.eof = await q.receive(cu.eof, space => this.visitSpace(space, q));

        return finishDraft(draft);
    }

    protected async visitConditionalType(conditionalType: JS.ConditionalType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(conditionalType);

        draft.checkType = await q.receive(conditionalType.checkType, type => this.visit(type, q));
        draft.condition = await q.receive(conditionalType.condition, cond => this.visitContainer(cond, q));
        draft.type = await q.receive(conditionalType.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitDefaultType(defaultType: JS.DefaultType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(defaultType);

        draft.left = await q.receive(defaultType.left, left => this.visit(left, q));
        draft.beforeEquals = await q.receive(defaultType.beforeEquals, space => this.visitSpace(space, q));
        draft.right = await q.receive(defaultType.right, right => this.visit(right, q));
        draft.type = await q.receive(defaultType.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitDelete(deleteExpr: JS.Delete, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(deleteExpr);

        draft.expression = await q.receive(deleteExpr.expression, expr => this.visit(expr, q));
        draft.type = await q.receive(deleteExpr.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitExport(export_: JS.Export, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(export_);

        draft.exports = await q.receive(export_.exports, exports => this.visitContainer(exports, q));
        draft.from = await q.receive(export_.from, space => this.visitSpace(space, q));
        draft.target = await q.receive(export_.target, target => this.visit(target, q));
        draft.initializer = await q.receive(export_.initializer, init => this.visitLeftPadded(init, q));

        return finishDraft(draft);
    }

    protected async visitExpressionStatement(exprStmt: JS.ExpressionStatement, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(exprStmt);

        draft.expression = await q.receive(exprStmt.expression, expr => this.visit(expr, q));

        return finishDraft(draft);
    }

    protected async visitTrailingTokenStatement(trailStmt: JS.TrailingTokenStatement, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(trailStmt);

        draft.expression = await q.receive(trailStmt.expression, expr => this.visitRightPadded(expr, q));
        draft.type = await q.receive(trailStmt.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitExpressionWithTypeArguments(exprType: JS.ExpressionWithTypeArguments, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(exprType);

        draft.clazz = await q.receive(exprType.clazz, clazz => this.visit(clazz, q));
        draft.typeArguments = await q.receive(exprType.typeArguments, args => this.visitContainer(args, q));
        draft.type = await q.receive(exprType.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitFunctionType(functionType: JS.FunctionType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(functionType);

        draft.modifiers = await q.receiveListDefined(functionType.modifiers, mod => this.visit(mod, q));
        draft.constructorType = await q.receive(functionType.constructorType, ct => this.visitLeftPadded(ct, q));
        draft.typeParameters = await q.receive(functionType.typeParameters, params => this.visit(params, q));
        draft.parameters = await q.receive(functionType.parameters, params => this.visitContainer(params, q));
        draft.returnType = await q.receive(functionType.returnType, rt => this.visitLeftPadded(rt, q));
        draft.type = await q.receive(functionType.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitImportAttribute(importAttr: JS.ImportAttribute, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(importAttr);

        draft.name = await q.receive(importAttr.name, name => this.visit(name, q));
        draft.value = await q.receive(importAttr.value, value => this.visitLeftPadded(value, q));

        return finishDraft(draft);
    }

    protected async visitImportAttributes(importAttrs: JS.ImportAttributes, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(importAttrs);

        draft.token = await q.receive(importAttrs.token);
        draft.elements = await q.receive(importAttrs.elements, elements => this.visitContainer(elements, q));

        return finishDraft(draft);
    }

    protected async visitImportType(importType: JS.ImportType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(importType);

        draft.hasTypeof = await q.receive(importType.hasTypeof, hasTypeof => this.visitRightPadded(hasTypeof, q));
        draft.argumentAndAttributes = await q.receive(importType.argumentAndAttributes, args => this.visitContainer(args, q));
        draft.qualifier = await q.receive(importType.qualifier, qualifier => this.visitLeftPadded(qualifier, q));
        draft.typeArguments = await q.receive(importType.typeArguments, args => this.visitContainer(args, q));
        draft.type = await q.receive(importType.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitImportTypeAttributes(importTypeAttrs: JS.ImportTypeAttributes, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(importTypeAttrs);

        draft.token = await q.receive(importTypeAttrs.token, token => this.visitRightPadded(token, q));
        draft.elements = await q.receive(importTypeAttrs.elements, elements => this.visitContainer(elements, q));
        draft.end = await q.receive(importTypeAttrs.end, space => this.visitSpace(space, q));

        return finishDraft(draft);
    }

    protected async visitIndexSignatureDeclaration(indexSignature: JS.IndexSignatureDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(indexSignature);

        draft.parameters = await q.receive(indexSignature.parameters, params => this.visitContainer(params, q));
        draft.typeExpression = await q.receive(indexSignature.typeExpression, expr => this.visitLeftPadded(expr, q));
        draft.type = await q.receive(indexSignature.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitInferType(inferType: JS.InferType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(inferType);

        draft.typeParameter = await q.receive(inferType.typeParameter, param => this.visitLeftPadded(param, q));
        draft.type = await q.receive(inferType.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitJsImport(jsImport: JS.JsImport, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(jsImport);

        draft.modifiers = await q.receiveListDefined(jsImport.modifiers, mod => this.visit(mod, q));
        draft.importClause = await q.receive(jsImport.importClause, clause => this.visit(clause, q));
        draft.moduleSpecifier = await q.receive(jsImport.moduleSpecifier, spec => this.visitLeftPadded(spec, q));
        draft.attributes = await q.receive(jsImport.attributes, attrs => this.visit(attrs, q));

        return finishDraft(draft);
    }

    protected async visitJsImportClause(jsImportClause: JS.JsImportClause, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(jsImportClause);

        draft.typeOnly = await q.receive(jsImportClause.typeOnly);
        draft.name = await q.receive(jsImportClause.name, name => this.visitRightPadded(name, q));
        draft.namedBindings = await q.receive(jsImportClause.namedBindings, bindings => this.visit(bindings, q));

        return finishDraft(draft);
    }

    protected async visitNamedImports(namedImports: JS.NamedImports, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(namedImports);

        draft.elements = await q.receive(namedImports.elements, elements => this.visitContainer(elements, q));
        draft.type = await q.receive(namedImports.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitJsImportSpecifier(jsImportSpecifier: JS.JsImportSpecifier, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(jsImportSpecifier);

        draft.importType = await q.receive(jsImportSpecifier.importType, importType => this.visitLeftPadded(importType, q));
        draft.specifier = await q.receive(jsImportSpecifier.specifier, specifier => this.visit(specifier, q));
        draft.type = await q.receive(jsImportSpecifier.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitJsBinary(jsBinary: JS.JsBinary, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(jsBinary);

        draft.left = await q.receive(jsBinary.left, left => this.visit(left, q));
        draft.operator = await q.receive(jsBinary.operator, op => this.visitLeftPadded(op, q));
        draft.right = await q.receive(jsBinary.right, right => this.visit(right, q));
        draft.type = await q.receive(jsBinary.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitLiteralType(literalType: JS.LiteralType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(literalType);

        draft.literal = await q.receive(literalType.literal, literal => this.visit(literal, q));
        draft.type = await q.receive(literalType.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitMappedType(mappedType: JS.MappedType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(mappedType);

        draft.prefixToken = await q.receive(mappedType.prefixToken, token => this.visitLeftPadded(token, q));
        draft.hasReadonly = await q.receive(mappedType.hasReadonly, readonly => this.visitLeftPadded(readonly, q));
        draft.keysRemapping = await q.receive(mappedType.keysRemapping, keys => this.visit(keys, q));
        draft.suffixToken = await q.receive(mappedType.suffixToken, token => this.visitLeftPadded(token, q));
        draft.hasQuestionToken = await q.receive(mappedType.hasQuestionToken, token => this.visitLeftPadded(token, q));
        draft.valueType = await q.receive(mappedType.valueType, type => this.visitContainer(type, q));
        draft.type = await q.receive(mappedType.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitMappedTypeKeysRemapping(keysRemapping: JS.MappedType.KeysRemapping, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(keysRemapping);

        draft.typeParameter = await q.receive(keysRemapping.typeParameter, param => this.visitRightPadded(param, q));
        draft.nameType = await q.receive(keysRemapping.nameType, nameType => this.visitRightPadded(nameType, q));

        return finishDraft(draft);
    }

    protected async visitMappedTypeMappedTypeParameter(param: JS.MappedType.MappedTypeParameter, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(param);

        draft.name = await q.receive(param.name, name => this.visit(name, q));
        draft.iterateType = await q.receive(param.iterateType, type => this.visitLeftPadded(type, q));

        return finishDraft(draft);
    }

    protected async visitObjectBindingDeclarations(objectBindings: JS.ObjectBindingDeclarations, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(objectBindings);

        draft.leadingAnnotations = await q.receiveListDefined(objectBindings.leadingAnnotations, annot => this.visitDefined<J.Annotation>(annot, q));
        draft.modifiers = await q.receiveListDefined(objectBindings.modifiers, mod => this.visit(mod, q));
        draft.typeExpression = await q.receive(objectBindings.typeExpression, expr => this.visit(expr, q));
        draft.bindings = await q.receive(objectBindings.bindings, bindings => this.visitContainer(bindings, q));
        draft.initializer = await q.receive(objectBindings.initializer, init => this.visitLeftPadded(init, q));

        return finishDraft(draft);
    }

    protected async visitPropertyAssignment(propAssign: JS.PropertyAssignment, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(propAssign);

        draft.name = await q.receive(propAssign.name, name => this.visitRightPadded(name, q));
        draft.assigmentToken = await q.receive(propAssign.assigmentToken);
        draft.initializer = await q.receive(propAssign.initializer, init => this.visit(init, q));

        return finishDraft(draft);
    }

    protected async visitSatisfiesExpression(satisfiesExpr: JS.SatisfiesExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(satisfiesExpr);

        draft.expression = await q.receive(satisfiesExpr.expression, expr => this.visit(expr, q));
        draft.satisfiesType = await q.receive(satisfiesExpr.satisfiesType, type => this.visitLeftPadded(type, q));
        draft.type = await q.receive(satisfiesExpr.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitJSVariableDeclarations(varDecls: JS.JSVariableDeclarations, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(varDecls);

        draft.leadingAnnotations = await q.receiveListDefined(varDecls.leadingAnnotations, annot => this.visit(annot, q));
        draft.modifiers = await q.receiveListDefined(varDecls.modifiers, mod => this.visit(mod, q));
        draft.typeExpression = await q.receive(varDecls.typeExpression, type => this.visit(type, q));
        draft.varargs = await q.receive(varDecls.varargs, space => this.visitSpace(space, q));
        draft.variables = await q.receiveListDefined(varDecls.variables, variable => this.visitRightPadded(variable, q));

        return finishDraft(draft);
    }

    protected async visitJSNamedVariable(variable: JS.JSVariableDeclarations.JSNamedVariable, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(variable);

        draft.name = await q.receive(variable.name, name => this.visit(name, q));
        draft.dimensionsAfterName = await q.receiveListDefined(variable.dimensionsAfterName, dim => this.visitLeftPadded(dim, q));
        draft.initializer = await q.receive(variable.initializer, init => this.visitLeftPadded(init, q));
        draft.variableType = await q.receive(variable.variableType, type => this.visitType(type, q) as any as JavaType.Variable);

        return finishDraft(draft);
    }

    protected async visitJSMethodDeclaration(methodDecl: JS.JSMethodDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(methodDecl);

        draft.leadingAnnotations = await q.receiveListDefined(methodDecl.leadingAnnotations, annot => this.visit(annot, q));
        draft.modifiers = await q.receiveListDefined(methodDecl.modifiers, mod => this.visit(mod, q));
        draft.typeParameters = await q.receive(methodDecl.typeParameters, p => this.visit(p, q));
        draft.returnTypeExpression = await q.receive(methodDecl.returnTypeExpression, type => this.visit(type, q));
        draft.name = await q.receive(methodDecl.name, name => this.visit(name, q));
        draft.parameters = await q.receive(methodDecl.parameters, params => this.visitContainer(params, q));
        draft.body = await q.receive(methodDecl.body, body => this.visitDefined<J.Block>(body, q));

        return finishDraft(draft);
    }

    protected async visitScopedVariableDeclarations(scopedVarDecls: JS.ScopedVariableDeclarations, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(scopedVarDecls);

        draft.modifiers = await q.receiveListDefined(scopedVarDecls.modifiers, mod => this.visit(mod, q));
        draft.scope = await q.receive(scopedVarDecls.scope, scope => this.visitLeftPadded(scope, q));
        draft.variables = await q.receiveListDefined(scopedVarDecls.variables, v => this.visitRightPadded(v, q));

        return finishDraft(draft);
    }

    protected async visitStatementExpression(stmtExpr: JS.StatementExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(stmtExpr);

        draft.statement = await q.receive(stmtExpr.statement, stmt => this.visit(stmt, q));

        return finishDraft(draft);
    }

    protected async visitTaggedTemplateExpression(taggedTemplate: JS.TaggedTemplateExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(taggedTemplate);

        draft.tag = await q.receive(taggedTemplate.tag, tag => this.visitRightPadded(tag, q));
        draft.typeArguments = await q.receive(taggedTemplate.typeArguments, args => this.visitContainer(args, q));
        draft.templateExpression = await q.receive(taggedTemplate.templateExpression, expr => this.visit(expr, q));
        draft.type = await q.receive(taggedTemplate.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitTemplateExpression(template: JS.TemplateExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(template);

        draft.head = await q.receive(template.head, head => this.visit(head, q));
        draft.templateSpans = await q.receiveListDefined(template.templateSpans, span => this.visitRightPadded(span, q));
        draft.type = await q.receive(template.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitTemplateExpressionTemplateSpan(span: JS.TemplateExpression.TemplateSpan, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(span);

        draft.expression = await q.receive(span.expression, expr => this.visit(expr, q));
        draft.tail = await q.receive(span.tail, tail => this.visitDefined<J.Literal>(tail, q));

        return finishDraft(draft);
    }

    protected async visitTuple(tuple: JS.Tuple, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(tuple);

        draft.elements = await q.receive(tuple.elements, elements => this.visitContainer(elements, q));
        draft.type = await q.receive(tuple.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitTypeDeclaration(typeDecl: JS.TypeDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(typeDecl);

        draft.modifiers = await q.receiveListDefined(typeDecl.modifiers, mod => this.visit(mod, q));
        draft.name = await q.receive(typeDecl.name, name => this.visitLeftPadded(name, q));
        draft.typeParameters = await q.receive(typeDecl.typeParameters, params => this.visit(params, q));
        draft.initializer = await q.receive(typeDecl.initializer, init => this.visitLeftPadded(init, q));
        draft.type = await q.receive(typeDecl.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitTypeOf(typeOf: JS.TypeOf, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(typeOf);

        draft.expression = await q.receive(typeOf.expression, expr => this.visit(expr, q));
        draft.type = await q.receive(typeOf.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitTypePredicate(typePredicate: JS.TypePredicate, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(typePredicate);

        draft.asserts = await q.receive(typePredicate.asserts, expr => this.visitLeftPadded(expr, q));
        draft.parameterName = await q.receive(typePredicate.parameterName, expr => this.visit(expr, q));
        draft.expression = await q.receive(typePredicate.expression, expr => this.visitLeftPadded(expr, q));
        draft.type = await q.receive(typePredicate.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitTypeTreeExpression(typeTreeExpression: JS.TypeTreeExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(typeTreeExpression);

        draft.expression = await q.receive(typeTreeExpression.expression, expr => this.visit(expr, q));

        return finishDraft(draft);
    }

    protected async visitJsUnary(unary: JS.Unary, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(unary);

        draft.operator = await q.receive(unary.operator, op => this.visitLeftPadded(op, q));
        draft.expression = await q.receive(unary.expression, expr => this.visit(expr, q));
        draft.type = await q.receive(unary.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitUnion(union: JS.Union, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(union);

        draft.types = await q.receiveListDefined(union.types, t => this.visitRightPadded(t, q));
        draft.type = await q.receive(union.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitWithStatement(withStmt: JS.WithStatement, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(withStmt);

        draft.expression = await q.receive(withStmt.expression, expr => this.visit(expr, q));
        draft.body = await q.receive(withStmt.body, body => this.visitRightPadded(body, q));

        return finishDraft(draft);
    }

    protected async visitLeftPadded<T extends J | J.Space | number | boolean>(left: J.LeftPadded<T>, q: RpcReceiveQueue): Promise<J.LeftPadded<T>> {
        if (!left) {
            throw new Error("TreeDataReceiveQueue should have instantiated an empty left padding");
        }

        return produceAsync<J.LeftPadded<T>>(left, async draft => {
            draft.before = await q.receive(left.before, space => this.visitSpace(space, q));
            draft.element = await q.receive(left.element, elem => {
                if (isJava(elem) || isJavaScript(elem)) {
                    return this.visit(elem as J, q) as any as T;
                } else if (isSpace(elem)) {
                    return this.visitSpace(elem as J.Space, q) as any as T;
                }
                return elem;
            }) as Draft<T>;
            draft.markers = await q.receiveMarkers(left.markers);
        });
    }

    protected async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, q: RpcReceiveQueue): Promise<J.RightPadded<T>> {
        if (!right) {
            throw new Error("TreeDataReceiveQueue should have instantiated an empty right padding");
        }

        return produceAsync<J.RightPadded<T>>(right, async draft => {
            draft.element = await q.receive(right.element, elem => {
                if (isJava(elem) || isJavaScript(elem)) {
                    return this.visit(elem as J, q) as any as T;
                } else if (isSpace(elem)) {
                    return this.visitSpace(elem as J.Space, q) as any as T;
                }
                return elem as any as T;
            }) as Draft<T>;
            draft.after = await q.receive(right.after, space => this.visitSpace(space, q));
            draft.markers = await q.receiveMarkers(right.markers);
        });
    }

    protected async visitContainer<T extends J>(container: J.Container<T>, q: RpcReceiveQueue): Promise<J.Container<T>> {
        return produceAsync<J.Container<T>>(container, async draft => {
            draft.before = await q.receive(container.before, space => this.visitSpace(space, q));
            draft.elements = await q.receiveListDefined(container.elements, elem => this.visitRightPadded(elem, q)) as Draft<J.RightPadded<T>[]>;
            draft.markers = await q.receiveMarkers(container.markers);
        });
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
Object.values(JavaScriptKind).forEach(kind => {
    RpcCodecs.registerCodec(kind, javaScriptCodec);
});
