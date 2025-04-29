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
import {mapAsync, SourceFile, ValidImmerRecipeReturnType} from "../";
import {
    Annotation,
    ControlParentheses,
    J,
    JavaType,
    LambdaParameters,
    Literal,
    Modifier,
    NameTree,
    Space,
    Statement,
    TypeParameters,
    TypeTree,
    JavaVisitor
} from "../java";
import {createDraft, Draft, finishDraft} from "immer";
import {
    Alias,
    ArrowFunction,
    Await,
    CompilationUnit,
    ConditionalType,
    DefaultType,
    Delete,
    Export,
    Expression,
    ExpressionStatement,
    ExpressionWithTypeArguments,
    FunctionType,
    ImportAttribute,
    ImportAttributes,
    ImportType,
    ImportTypeAttributes,
    IndexedAccessType,
    IndexedAccessTypeIndexType,
    InferType,
    Intersection,
    isJavaScript,
    JS,
    JavaScriptKind,
    JsAssignmentOperation,
    JsBinary,
    JsImport,
    JsImportClause,
    JsImportSpecifier,
    LiteralType,
    MappedType,
    MappedTypeKeysRemapping,
    MappedTypeMappedTypeParameter,
    NamedImports,
    ObjectBindingDeclarations,
    PropertyAssignment,
    SatisfiesExpression,
    ScopedVariableDeclarations,
    StatementExpression,
    TaggedTemplateExpression,
    TemplateExpression,
    TemplateExpressionTemplateSpan,
    TrailingTokenStatement,
    Tuple,
    TypeDeclaration,
    TypeInfo,
    TypeOf,
    TypeOperator,
    TypePredicate,
    TypeQuery,
    TypeTreeExpression,
    Union,
    Void,
    WithStatement,
    Yield
} from "./tree";

export class JavaScriptVisitor<P> extends JavaVisitor<P> {

    override isAcceptable(sourceFile: SourceFile): boolean {
        return isJavaScript(sourceFile);
    }

    // noinspection JSUnusedGlobalSymbols,JSUnusedLocalSymbols
    override async visitExpression(expression: Expression, _p: P): Promise<J | undefined> {
        return expression;
    }

    // noinspection JSUnusedGlobalSymbols,JSUnusedLocalSymbols
    override async visitStatement(statement: Statement, _p: P): Promise<J | undefined> {
        return statement;
    }

    // noinspection JSUnusedLocalSymbols
    override async visitSpace(space: Space, _p: P): Promise<Space> {
        return space;
    }

    // noinspection JSUnusedLocalSymbols
    override async visitType(javaType: JavaType | undefined, _p: P): Promise<JavaType | undefined> {
        return javaType;
    }

    // noinspection JSUnusedLocalSymbols
    override async visitTypeName<N extends NameTree>(nameTree: N, _p: P): Promise<N> {
        return nameTree;
    }

    protected async visitAlias(alias: Alias, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Alias>(alias, p, async draft => {
            draft.propertyName = await this.visitRightPadded(alias.propertyName, p);
            draft.alias = await this.visitDefined(alias.alias, p) as Expression;
            draft.type = await this.visitType(alias.type, p);
        });
    }

    protected async visitArrowFunction(arrowFunction: ArrowFunction, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ArrowFunction>(arrowFunction, p, async draft => {
            draft.leadingAnnotations = await mapAsync(arrowFunction.leadingAnnotations, a => this.visitDefined<Annotation>(a, p));
            draft.modifiers = await mapAsync(arrowFunction.modifiers, m => this.visitDefined<Modifier>(m, p));

            if (arrowFunction.typeParameters) {
                draft.typeParameters = await this.visitDefined(arrowFunction.typeParameters, p) as TypeParameters;
            }

            draft.parameters = await this.visitDefined(arrowFunction.parameters, p) as LambdaParameters;

            if (arrowFunction.returnTypeExpression) {
                draft.returnTypeExpression = await this.visitDefined(arrowFunction.returnTypeExpression, p) as TypeTree;
            }

            draft.body = await this.visitLeftPadded(arrowFunction.body, p);
            draft.type = await this.visitType(arrowFunction.type, p);
        });
    }

    protected async visitAwait(await_: Await, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Await>(await_, p, async draft => {
            draft.expression = await this.visitDefined(await_.expression, p) as Expression;
            draft.type = await this.visitType(await_.type, p);
        });
    }

    protected async visitJSCompilationUnit(cu: CompilationUnit, p: P): Promise<J | undefined> {
        return this.produceJavaScript<CompilationUnit>(cu, p, async draft => {
            draft.imports = await mapAsync(cu.imports, imp => this.visitRightPadded(imp, p));
            draft.statements = await mapAsync(cu.statements, stmt => this.visitRightPadded(stmt, p));
            draft.eof = await this.visitSpace(cu.eof, p);
        });
    }

    protected async visitConditionalType(conditionalType: ConditionalType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ConditionalType>(conditionalType, p, async draft => {
            draft.checkType = await this.visitDefined(conditionalType.checkType, p) as Expression;
            draft.condition = await this.visitContainer(conditionalType.condition, p);
            draft.type = await this.visitType(conditionalType.type, p);
        });
    }

    protected async visitDefaultType(defaultType: DefaultType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<DefaultType>(defaultType, p, async draft => {
            draft.left = await this.visitDefined(defaultType.left, p) as Expression;
            draft.beforeEquals = await this.visitSpace(defaultType.beforeEquals, p);
            draft.right = await this.visitDefined(defaultType.right, p) as Expression;
            draft.type = await this.visitType(defaultType.type, p);
        });
    }

    protected async visitDelete(deleteExpr: Delete, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Delete>(deleteExpr, p, async draft => {
            draft.expression = await this.visitDefined(deleteExpr.expression, p) as Expression;
            draft.type = await this.visitType(deleteExpr.type, p);
        });
    }

    protected async visitExport(export_: Export, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Export>(export_, p, async draft => {
            draft.exports = await this.visitOptionalContainer(export_.exports, p);

            if (export_.from) {
                draft.from = await this.visitSpace(export_.from, p);
            }

            if (export_.target) {
                draft.target = await this.visitDefined(export_.target, p) as Literal;
            }

            draft.initializer = await this.visitOptionalLeftPadded(export_.initializer, p);
        });
    }

    protected async visitExpressionStatement(exprStmt: ExpressionStatement, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ExpressionStatement>(exprStmt, p, async draft => {
            draft.expression = await this.visitDefined(exprStmt.expression, p) as Expression;
        });
    }

    protected async visitTrailingTokenStatement(trailStmt: TrailingTokenStatement, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TrailingTokenStatement>(trailStmt, p, async draft => {
            draft.expression = await this.visitRightPadded(trailStmt.expression, p);
            draft.type = await this.visitType(trailStmt.type, p);
        });
    }

    protected async visitExpressionWithTypeArguments(exprType: ExpressionWithTypeArguments, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ExpressionWithTypeArguments>(exprType, p, async draft => {
            draft.clazz = await this.visitDefined(exprType.clazz, p) as J;
            draft.typeArguments = await this.visitOptionalContainer(exprType.typeArguments, p);
            draft.type = await this.visitType(exprType.type, p);
        });
    }

    protected async visitFunctionType(functionType: FunctionType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<FunctionType>(functionType, p, async draft => {
            draft.modifiers = await mapAsync(functionType.modifiers, m => this.visitDefined<Modifier>(m, p));
            draft.constructorType = await this.visitLeftPadded(functionType.constructorType, p);

            if (functionType.typeParameters) {
                draft.typeParameters = await this.visitDefined(functionType.typeParameters, p) as TypeParameters;
            }

            draft.parameters = await this.visitContainer(functionType.parameters, p);
            draft.returnType = await this.visitLeftPadded(functionType.returnType, p);
            draft.type = await this.visitType(functionType.type, p);
        });
    }

    protected async visitImportAttribute(importAttr: ImportAttribute, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ImportAttribute>(importAttr, p, async draft => {
            draft.name = await this.visitDefined(importAttr.name, p) as Expression;
            draft.value = await this.visitLeftPadded(importAttr.value, p);
        });
    }

    protected async visitImportAttributes(importAttrs: ImportAttributes, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ImportAttributes>(importAttrs, p, async draft => {
            draft.elements = await this.visitContainer(importAttrs.elements, p);
        });
    }

    protected async visitImportType(importType: ImportType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ImportType>(importType, p, async draft => {
            draft.hasTypeof = await this.visitRightPadded(importType.hasTypeof, p);
            draft.argumentAndAttributes = await this.visitContainer(importType.argumentAndAttributes, p);
            draft.qualifier = await this.visitOptionalLeftPadded(importType.qualifier, p);
            draft.typeArguments = await this.visitOptionalContainer(importType.typeArguments, p);
            draft.type = await this.visitType(importType.type, p);
        });
    }

    protected async visitImportTypeAttributes(importTypeAttrs: ImportTypeAttributes, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ImportTypeAttributes>(importTypeAttrs, p, async draft => {
            draft.token = await this.visitRightPadded(importTypeAttrs.token, p);
            draft.elements = await this.visitContainer(importTypeAttrs.elements, p);
            draft.end = await this.visitSpace(importTypeAttrs.end, p);
        });
    }

    protected async visitInferType(inferType: InferType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<InferType>(inferType, p, async draft => {
            draft.typeParameter = await this.visitLeftPadded(inferType.typeParameter, p);
            draft.type = await this.visitType(inferType.type, p);
        });
    }

    protected async visitJsImport(jsImport: JsImport, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JsImport>(jsImport, p, async draft => {
            draft.modifiers = await mapAsync(jsImport.modifiers, m => this.visitDefined<Modifier>(m, p));

            if (jsImport.importClause) {
                draft.importClause = await this.visitDefined(jsImport.importClause, p) as JsImportClause;
            }

            draft.moduleSpecifier = await this.visitLeftPadded(jsImport.moduleSpecifier, p);

            if (jsImport.attributes) {
                draft.attributes = await this.visitDefined(jsImport.attributes, p) as ImportAttributes;
            }
        });
    }

    protected async visitJsImportClause(jsImportClause: JsImportClause, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JsImportClause>(jsImportClause, p, async draft => {
            if (jsImportClause.name) {
                draft.name = await this.visitRightPadded(jsImportClause.name, p);
            }

            if (jsImportClause.namedBindings) {
                draft.namedBindings = await this.visitDefined(jsImportClause.namedBindings, p) as Expression;
            }
        });
    }

    protected async visitNamedImports(namedImports: NamedImports, p: P): Promise<J | undefined> {
        return this.produceJavaScript<NamedImports>(namedImports, p, async draft => {
            draft.elements = await this.visitContainer(namedImports.elements, p);
            draft.type = await this.visitType(namedImports.type, p);
        });
    }

    protected async visitJsImportSpecifier(jsImportSpecifier: JsImportSpecifier, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JsImportSpecifier>(jsImportSpecifier, p, async draft => {
            draft.importType = await this.visitLeftPadded(jsImportSpecifier.importType, p);
            draft.specifier = await this.visitDefined(jsImportSpecifier.specifier, p) as Expression;
            draft.type = await this.visitType(jsImportSpecifier.type, p);
        });
    }

    protected async visitJsBinary(jsBinary: JsBinary, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JsBinary>(jsBinary, p, async draft => {
            draft.left = await this.visitDefined(jsBinary.left, p) as Expression;
            draft.operator = await this.visitLeftPadded(jsBinary.operator, p);
            draft.right = await this.visitDefined(jsBinary.right, p) as Expression;
            draft.type = await this.visitType(jsBinary.type, p);
        });
    }

    protected async visitLiteralType(literalType: LiteralType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<LiteralType>(literalType, p, async draft => {
            draft.literal = await this.visitDefined(literalType.literal, p) as Expression;
            draft.type = await this.visitType(literalType.type, p) as JavaType;
        });
    }

    protected async visitMappedType(mappedType: MappedType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<MappedType>(mappedType, p, async draft => {
            draft.prefixToken = await this.visitOptionalLeftPadded(mappedType.prefixToken, p);
            draft.hasReadonly = await this.visitLeftPadded(mappedType.hasReadonly, p);
            draft.keysRemapping = await this.visitDefined(mappedType.keysRemapping, p) as MappedTypeKeysRemapping;
            draft.suffixToken = await this.visitOptionalLeftPadded(mappedType.suffixToken, p);
            draft.hasQuestionToken = await this.visitLeftPadded(mappedType.hasQuestionToken, p);
            draft.valueType = await this.visitContainer(mappedType.valueType, p);
            draft.type = await this.visitType(mappedType.type, p);
        });
    }

    protected async visitMappedTypeKeysRemapping(keysRemapping: MappedTypeKeysRemapping, p: P): Promise<J | undefined> {
        return this.produceJavaScript<MappedTypeKeysRemapping>(keysRemapping, p, async draft => {
            draft.typeParameter = await this.visitRightPadded(keysRemapping.typeParameter, p);
            draft.nameType = await this.visitOptionalRightPadded(keysRemapping.nameType, p);
        });
    }

    protected async visitMappedTypeMappedTypeParameter(param: MappedTypeMappedTypeParameter, p: P): Promise<J | undefined> {
        return this.produceJavaScript<MappedTypeMappedTypeParameter>(param, p, async draft => {
            draft.name = await this.visitDefined(param.name, p) as Expression;
            draft.iterateType = await this.visitLeftPadded(param.iterateType, p);
        });
    }

    protected async visitObjectBindingDeclarations(objectBindings: ObjectBindingDeclarations, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ObjectBindingDeclarations>(objectBindings, p, async draft => {
            draft.leadingAnnotations = await mapAsync(objectBindings.leadingAnnotations, a => this.visitDefined<Annotation>(a, p));
            draft.modifiers = await mapAsync(objectBindings.modifiers, m => this.visitDefined<Modifier>(m, p));

            if (objectBindings.typeExpression) {
                draft.typeExpression = await this.visitDefined(objectBindings.typeExpression, p) as TypeTree;
            }

            draft.bindings = await this.visitContainer(objectBindings.bindings, p);
            draft.initializer = await this.visitOptionalLeftPadded(objectBindings.initializer, p);
        });
    }

    protected async visitPropertyAssignment(propAssign: PropertyAssignment, p: P): Promise<J | undefined> {
        return this.produceJavaScript<PropertyAssignment>(propAssign, p, async draft => {
            draft.name = await this.visitRightPadded(propAssign.name, p);

            if (propAssign.initializer) {
                draft.initializer = await this.visitDefined(propAssign.initializer, p) as Expression;
            }
        });
    }

    protected async visitSatisfiesExpression(satisfiesExpr: SatisfiesExpression, p: P): Promise<J | undefined> {
        return this.produceJavaScript<SatisfiesExpression>(satisfiesExpr, p, async draft => {
            draft.expression = await this.visitDefined(satisfiesExpr.expression, p) as J;
            draft.satisfiesType = await this.visitLeftPadded(satisfiesExpr.satisfiesType, p);
            draft.type = await this.visitType(satisfiesExpr.type, p);
        });
    }

    protected async visitScopedVariableDeclarations(scopedVarDecls: ScopedVariableDeclarations, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ScopedVariableDeclarations>(scopedVarDecls, p, async draft => {
            draft.modifiers = await mapAsync(scopedVarDecls.modifiers, m => this.visitDefined<Modifier>(m, p));
            draft.scope = await this.visitOptionalLeftPadded(scopedVarDecls.scope, p);
            draft.variables = await mapAsync(scopedVarDecls.variables, v => this.visitRightPadded(v, p));
        });
    }

    protected async visitStatementExpression(stmtExpr: StatementExpression, p: P): Promise<J | undefined> {
        return this.produceJavaScript<StatementExpression>(stmtExpr, p, async draft => {
            draft.statement = await this.visitDefined(stmtExpr.statement, p) as Statement;
        });
    }

    protected async visitTaggedTemplateExpression(taggedTemplate: TaggedTemplateExpression, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TaggedTemplateExpression>(taggedTemplate, p, async draft => {
            draft.tag = await this.visitOptionalRightPadded(taggedTemplate.tag, p);
            draft.typeArguments = await this.visitOptionalContainer(taggedTemplate.typeArguments, p);
            draft.templateExpression = await this.visitDefined(taggedTemplate.templateExpression, p) as Expression;
            draft.type = await this.visitType(taggedTemplate.type, p);
        });
    }

    protected async visitTemplateExpression(template: TemplateExpression, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TemplateExpression>(template, p, async draft => {
            draft.head = await this.visitDefined(template.head, p) as Literal;
            draft.templateSpans = await mapAsync(template.templateSpans, span => this.visitRightPadded(span, p));
            draft.type = await this.visitType(template.type, p);
        });
    }

    protected async visitTemplateExpressionTemplateSpan(span: TemplateExpressionTemplateSpan, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TemplateExpressionTemplateSpan>(span, p, async draft => {
            draft.expression = await this.visitDefined(span.expression, p) as J;
            draft.tail = await this.visitDefined(span.tail, p) as Literal;
        });
    }

    protected async visitTuple(tuple: Tuple, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Tuple>(tuple, p, async draft => {
            draft.elements = await this.visitContainer(tuple.elements, p);
            draft.type = await this.visitType(tuple.type, p);
        });
    }

    protected async visitTypeDeclaration(typeDecl: TypeDeclaration, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TypeDeclaration>(typeDecl, p, async draft => {
            draft.modifiers = await mapAsync(typeDecl.modifiers, m => this.visitDefined<Modifier>(m, p));
            draft.name = await this.visitLeftPadded(typeDecl.name, p);

            if (typeDecl.typeParameters) {
                draft.typeParameters = await this.visitDefined(typeDecl.typeParameters, p) as TypeParameters;
            }

            draft.initializer = await this.visitLeftPadded(typeDecl.initializer, p);
            draft.type = await this.visitType(typeDecl.type, p);
        });
    }

    protected async visitTypeOf(typeOf: TypeOf, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TypeOf>(typeOf, p, async draft => {
            draft.expression = await this.visitDefined(typeOf.expression, p) as Expression;
            draft.type = await this.visitType(typeOf.type, p);
        });
    }
    
    protected async visitTypeTreeExpression(typeTreeExpression: TypeTreeExpression, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TypeTreeExpression>(typeTreeExpression, p, async draft => {
            draft.expression = await this.visitDefined(typeTreeExpression.expression, p) as Expression;
            draft.type = await this.visitType(typeTreeExpression.type, p);
        });
    }
    
    protected async visitJsAssignmentOperation(assignOp: JsAssignmentOperation, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JsAssignmentOperation>(assignOp, p, async draft => {
            draft.variable = await this.visitDefined(assignOp.variable, p) as Expression;
            draft.operator = await this.visitLeftPadded(assignOp.operator, p);
            draft.assignment = await this.visitDefined(assignOp.assignment, p) as Expression;
            draft.type = await this.visitType(assignOp.type, p);
        });
    }
    
    protected async visitIndexedAccessType(indexedAccessType: IndexedAccessType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<IndexedAccessType>(indexedAccessType, p, async draft => {
            draft.objectType = await this.visitDefined(indexedAccessType.objectType, p) as Expression;
            draft.indexType = await this.visitDefined(indexedAccessType.indexType, p) as IndexedAccessTypeIndexType;
            draft.type = await this.visitType(indexedAccessType.type, p);
        });
    }
    
    protected async visitIndexedAccessTypeIndexType(indexType: IndexedAccessTypeIndexType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<IndexedAccessTypeIndexType>(indexType, p, async draft => {
            draft.element = await this.visitRightPadded(indexType.element, p);
            draft.type = await this.visitType(indexType.type, p);
        });
    }
    
    protected async visitTypeQuery(typeQuery: TypeQuery, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TypeQuery>(typeQuery, p, async draft => {
            draft.typeExpression = await this.visitDefined(typeQuery.typeExpression, p) as Expression;
            draft.typeArguments = await this.visitOptionalContainer(typeQuery.typeArguments, p);
            draft.type = await this.visitType(typeQuery.type, p);
        });
    }
    
    protected async visitTypeInfo(typeInfo: TypeInfo, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TypeInfo>(typeInfo, p, async draft => {
            draft.typeIdentifier = await this.visitDefined(typeInfo.typeIdentifier, p) as Expression;
        });
    }
    
    protected async visitTypeOperator(typeOperator: TypeOperator, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TypeOperator>(typeOperator, p, async draft => {
            draft.expression = await this.visitLeftPadded(typeOperator.expression, p);
        });
    }
    
    protected async visitTypePredicate(typePredicate: TypePredicate, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TypePredicate>(typePredicate, p, async draft => {
            draft.asserts = await this.visitLeftPadded(typePredicate.asserts, p);
            draft.parameterName = await this.visitDefined(typePredicate.parameterName, p) as Expression;
            draft.expression = await this.visitOptionalLeftPadded(typePredicate.expression, p);
        });
    }
    
    protected async visitUnion(union: Union, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Union>(union, p, async draft => {
            draft.types = await mapAsync(union.types, t => this.visitRightPadded(t, p));
            draft.type = await this.visitType(union.type, p);
        });
    }
    
    protected async visitIntersection(intersection: Intersection, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Intersection>(intersection, p, async draft => {
            draft.types = await mapAsync(intersection.types, t => this.visitRightPadded(t, p));
            draft.type = await this.visitType(intersection.type, p);
        });
    }
    
    protected async visitVoid(void_: Void, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Void>(void_, p, async draft => {
            draft.expression = await this.visitDefined(void_.expression, p) as Expression;
        });
    }
    
    protected async visitJSYield(yield_: Yield, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Yield>(yield_, p, async draft => {
            draft.delegated = await this.visitLeftPadded(yield_.delegated, p);
            if (yield_.expression) {
                draft.expression = await this.visitDefined(yield_.expression, p) as Expression;
            }
        });
    }

    protected async visitWithStatement(withStmt: WithStatement, p: P): Promise<J | undefined> {
        return this.produceJavaScript<WithStatement>(withStmt, p, async draft => {
            draft.expression = await this.visitDefined(withStmt.expression, p) as ControlParentheses<Expression>;
            draft.body = await this.visitRightPadded(withStmt.body, p);
        });
    }

    protected async produceJavaScript<J2 extends JS>(
        before: JS | undefined,
        p: P,
        recipe?: (draft: Draft<J2>) =>
            ValidImmerRecipeReturnType<Draft<J2>> |
            PromiseLike<ValidImmerRecipeReturnType<Draft<J2>>>
    ): Promise<J2> {
        const draft: Draft<J2> = createDraft(before as J2);
        (draft as Draft<J>).prefix = await this.visitSpace(before!.prefix, p);
        (draft as Draft<J>).markers = await this.visitMarkers(before!.markers, p);
        if (recipe) {
            await recipe(draft);
        }
        return finishDraft(draft) as J2;
    }

    override accept(t: J, p: P): Promise<J | undefined> {
        switch (t.kind) {
            case JavaScriptKind.Alias:
                return this.visitAlias(t as Alias, p);
            case JavaScriptKind.ArrowFunction:
                return this.visitArrowFunction(t as ArrowFunction, p);
            case JavaScriptKind.Await:
                return this.visitAwait(t as Await, p);
            case JavaScriptKind.CompilationUnit:
                return this.visitJSCompilationUnit(t as CompilationUnit, p);
            case JavaScriptKind.ConditionalType:
                return this.visitConditionalType(t as ConditionalType, p);
            case JavaScriptKind.DefaultType:
                return this.visitDefaultType(t as DefaultType, p);
            case JavaScriptKind.Delete:
                return this.visitDelete(t as Delete, p);
            case JavaScriptKind.Export:
                return this.visitExport(t as Export, p);
            case JavaScriptKind.ExpressionStatement:
                return this.visitExpressionStatement(t as ExpressionStatement, p);
            case JavaScriptKind.TrailingTokenStatement:
                return this.visitTrailingTokenStatement(t as TrailingTokenStatement, p);
            case JavaScriptKind.ExpressionWithTypeArguments:
                return this.visitExpressionWithTypeArguments(t as ExpressionWithTypeArguments, p);
            case JavaScriptKind.FunctionType:
                return this.visitFunctionType(t as FunctionType, p);
            case JavaScriptKind.ImportAttribute:
                return this.visitImportAttribute(t as ImportAttribute, p);
            case JavaScriptKind.ImportAttributes:
                return this.visitImportAttributes(t as ImportAttributes, p);
            case JavaScriptKind.ImportType:
                return this.visitImportType(t as ImportType, p);
            case JavaScriptKind.ImportTypeAttributes:
                return this.visitImportTypeAttributes(t as ImportTypeAttributes, p);
            case JavaScriptKind.IndexedAccessType:
                return this.visitIndexedAccessType(t as IndexedAccessType, p);
            case JavaScriptKind.IndexedAccessTypeIndexType:
                return this.visitIndexedAccessTypeIndexType(t as IndexedAccessTypeIndexType, p);
            case JavaScriptKind.InferType:
                return this.visitInferType(t as InferType, p);
            case JavaScriptKind.JsAssignmentOperation:
                return this.visitJsAssignmentOperation(t as JsAssignmentOperation, p);
            case JavaScriptKind.JsImport:
                return this.visitJsImport(t as JsImport, p);
            case JavaScriptKind.JsImportClause:
                return this.visitJsImportClause(t as JsImportClause, p);
            case JavaScriptKind.NamedImports:
                return this.visitNamedImports(t as NamedImports, p);
            case JavaScriptKind.JsImportSpecifier:
                return this.visitJsImportSpecifier(t as JsImportSpecifier, p);
            case JavaScriptKind.JsBinary:
                return this.visitJsBinary(t as JsBinary, p);
            case JavaScriptKind.LiteralType:
                return this.visitLiteralType(t as LiteralType, p);
            case JavaScriptKind.MappedType:
                return this.visitMappedType(t as MappedType, p);
            case JavaScriptKind.MappedTypeKeysRemapping:
                return this.visitMappedTypeKeysRemapping(t as MappedTypeKeysRemapping, p);
            case JavaScriptKind.MappedTypeMappedTypeParameter:
                return this.visitMappedTypeMappedTypeParameter(t as MappedTypeMappedTypeParameter, p);
            case JavaScriptKind.ObjectBindingDeclarations:
                return this.visitObjectBindingDeclarations(t as ObjectBindingDeclarations, p);
            case JavaScriptKind.PropertyAssignment:
                return this.visitPropertyAssignment(t as PropertyAssignment, p);
            case JavaScriptKind.SatisfiesExpression:
                return this.visitSatisfiesExpression(t as SatisfiesExpression, p);
            case JavaScriptKind.ScopedVariableDeclarations:
                return this.visitScopedVariableDeclarations(t as ScopedVariableDeclarations, p);
            case JavaScriptKind.StatementExpression:
                return this.visitStatementExpression(t as StatementExpression, p);
            case JavaScriptKind.TaggedTemplateExpression:
                return this.visitTaggedTemplateExpression(t as TaggedTemplateExpression, p);
            case JavaScriptKind.TemplateExpression:
                return this.visitTemplateExpression(t as TemplateExpression, p);
            case JavaScriptKind.TemplateExpressionTemplateSpan:
                return this.visitTemplateExpressionTemplateSpan(t as TemplateExpressionTemplateSpan, p);
            case JavaScriptKind.Tuple:
                return this.visitTuple(t as Tuple, p);
            case JavaScriptKind.TypeDeclaration:
                return this.visitTypeDeclaration(t as TypeDeclaration, p);
            case JavaScriptKind.TypeOf:
                return this.visitTypeOf(t as TypeOf, p);
            case JavaScriptKind.TypeQuery:
                return this.visitTypeQuery(t as TypeQuery, p);
            case JavaScriptKind.TypeTreeExpression:
                return this.visitTypeTreeExpression(t as TypeTreeExpression, p);
            case JavaScriptKind.TypeInfo:
                return this.visitTypeInfo(t as TypeInfo, p);
            case JavaScriptKind.TypeOperator:
                return this.visitTypeOperator(t as TypeOperator, p);
            case JavaScriptKind.TypePredicate:
                return this.visitTypePredicate(t as TypePredicate, p);
            case JavaScriptKind.Union:
                return this.visitUnion(t as Union, p);
            case JavaScriptKind.Intersection:
                return this.visitIntersection(t as Intersection, p);
            case JavaScriptKind.Void:
                return this.visitVoid(t as Void, p);
            case JavaScriptKind.Yield:
                return this.visitJSYield(t as Yield, p);
            case JavaScriptKind.WithStatement:
                return this.visitWithStatement(t as WithStatement, p);
            default:
                return super.accept(t, p);
        }
    }
}
