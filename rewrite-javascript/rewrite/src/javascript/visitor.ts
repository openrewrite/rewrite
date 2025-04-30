// noinspection JSUnusedGlobalSymbols

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
import {mapAsync, SourceFile, ValidImmerRecipeReturnType} from "../";
import {
    J,
    JavaType,
    JavaVisitor,
    NameTree,
    Statement,
    TypedTree,
    TypeTree
} from "../java";
import {createDraft, Draft, finishDraft} from "immer";
import {
    Alias,
    ArrayBindingPattern,
    ArrowFunction,
    Await,
    BindingElement,
    CompilationUnit,
    ConditionalType,
    DefaultType,
    Delete,
    Export,
    ExportAssignment,
    ExportDeclaration,
    ExportSpecifier,
    Expression,
    ExpressionStatement,
    ExpressionWithTypeArguments,
    FunctionDeclaration,
    FunctionType,
    ImportAttribute,
    ImportAttributes,
    ImportType,
    ImportTypeAttributes,
    IndexedAccessType,
    IndexSignatureDeclaration,
    InferType,
    Intersection,
    isJavaScript,
    JavaScriptKind,
    JS,
    JsAssignmentOperation,
    JsBinary,
    JSForInLoop,
    JSForInOfLoopControl,
    JSForOfLoop,
    JsImport,
    JsImportClause,
    JsImportSpecifier,
    JSMethodDeclaration,
    JSTry,
    JSVariableDeclarations,
    LiteralType,
    MappedType,
    NamedExports,
    NamedImports,
    NamespaceDeclaration,
    ObjectBindingDeclarations,
    PropertyAssignment,
    SatisfiesExpression,
    ScopedVariableDeclarations,
    StatementExpression,
    TaggedTemplateExpression,
    TemplateExpression,
    TrailingTokenStatement,
    Tuple,
    TypeDeclaration,
    TypeInfo,
    TypeLiteral,
    TypeOf,
    TypeOperator,
    TypePredicate,
    TypeQuery,
    TypeTreeExpression,
    Unary,
    Union,
    Void,
    WithStatement,
    Yield,
} from "./tree";
import JSNamedVariable = JSVariableDeclarations.JSNamedVariable;
import MappedTypeParameter = MappedType.MappedTypeParameter;
import TemplateSpan = TemplateExpression.TemplateSpan;
import IndexType = IndexedAccessType.IndexType;

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
    override async visitSpace(space: J.Space, _p: P): Promise<J.Space> {
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

    protected async visitAlias(alias: Alias, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Alias>(alias, p, async draft => {
            draft.propertyName = await this.visitRightPadded(alias.propertyName, p);
            draft.alias = await this.visitDefined<Expression>(alias.alias, p);
        });
    }

    protected async visitArrowFunction(arrowFunction: ArrowFunction, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ArrowFunction>(arrowFunction, p, async draft => {
            draft.leadingAnnotations = await mapAsync(arrowFunction.leadingAnnotations, item => this.visitDefined<J.Annotation>(item, p));
            draft.modifiers = await mapAsync(arrowFunction.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.typeParameters = arrowFunction.typeParameters && await this.visitDefined<J.TypeParameters>(arrowFunction.typeParameters, p);
            draft.parameters = await this.visitDefined<J.LambdaParameters>(arrowFunction.parameters, p);
            draft.returnTypeExpression = arrowFunction.returnTypeExpression && await this.visitDefined<TypeTree>(arrowFunction.returnTypeExpression, p);
            draft.body = await this.visitLeftPadded(arrowFunction.body, p);
            draft.type = arrowFunction.type && await this.visitType(arrowFunction.type, p);
        });
    }

    protected async visitAwait(await_: Await, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Await>(await_, p, async draft => {
            draft.expression = await this.visitDefined<Expression>(await_.expression, p);
            draft.type = await_.type && await this.visitType(await_.type, p);
        });
    }

    protected async visitJsCompilationUnit(compilationUnit: CompilationUnit, p: P): Promise<J | undefined> {
        return this.produceJavaScript<CompilationUnit>(compilationUnit, p, async draft => {
            draft.imports = await mapAsync(compilationUnit.imports, imp => this.visitRightPadded(imp, p));
            draft.statements = await mapAsync(compilationUnit.statements, stmt => this.visitRightPadded(stmt, p));
            draft.eof = await this.visitSpace(compilationUnit.eof, p);
        });
    }

    protected async visitConditionalType(conditionalType: ConditionalType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ConditionalType>(conditionalType, p, async draft => {
            draft.checkType = await this.visitDefined<Expression>(conditionalType.checkType, p);
            draft.condition = await this.visitContainer(conditionalType.condition, p);
            draft.type = conditionalType.type && await this.visitType(conditionalType.type, p);
        });
    }

    protected async visitDefaultType(defaultType: DefaultType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<DefaultType>(defaultType, p, async draft => {
            draft.left = await this.visitDefined<Expression>(defaultType.left, p);
            draft.beforeEquals = await this.visitSpace(defaultType.beforeEquals, p);
            draft.right = await this.visitDefined<Expression>(defaultType.right, p);
            draft.type = defaultType.type && await this.visitType(defaultType.type, p);
        });
    }

    protected async visitDelete(delete_: Delete, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Delete>(delete_, p, async draft => {
            draft.expression = await this.visitDefined<Expression>(delete_.expression, p);
            draft.type = delete_.type && await this.visitType(delete_.type, p);
        });
    }

    protected async visitExport(export_: Export, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Export>(export_, p, async draft => {
            draft.exports = export_.exports && await this.visitContainer(export_.exports, p);
            draft.from = export_["from"] && await this.visitSpace(export_["from"], p);
            draft.target = export_.target && await this.visitDefined<J.Literal>(export_.target, p);
            draft.initializer = export_.initializer && await this.visitLeftPadded(export_.initializer, p);
        });
    }

    protected async visitExpressionStatement(expressionStatement: ExpressionStatement, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ExpressionStatement>(expressionStatement, p, async draft => {
            draft.expression = await this.visitDefined<Expression>(expressionStatement.expression, p);
        });
    }

    protected async visitExpressionWithTypeArguments(expressionWithTypeArguments: ExpressionWithTypeArguments, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ExpressionWithTypeArguments>(expressionWithTypeArguments, p, async draft => {
            draft.clazz = await this.visitDefined<J>(expressionWithTypeArguments.clazz, p);
            draft.typeArguments = expressionWithTypeArguments.typeArguments && await this.visitContainer(expressionWithTypeArguments.typeArguments, p);
            draft.type = expressionWithTypeArguments.type && await this.visitType(expressionWithTypeArguments.type, p);
        });
    }

    protected async visitFunctionType(functionType: FunctionType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<FunctionType>(functionType, p, async draft => {
            draft.modifiers = await mapAsync(functionType.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.constructorType = await this.visitLeftPadded(functionType.constructorType, p);
            draft.typeParameters = functionType.typeParameters && await this.visitDefined<J.TypeParameters>(functionType.typeParameters, p);
            draft.parameters = await this.visitContainer(functionType.parameters, p);
            draft.returnType = await this.visitLeftPadded(functionType.returnType, p);
            draft.type = functionType.type && await this.visitType(functionType.type, p);
        });
    }

    protected async visitInferType(inferType: InferType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<InferType>(inferType, p, async draft => {
            draft.typeParameter = await this.visitLeftPadded(inferType.typeParameter, p);
            draft.type = inferType.type && await this.visitType(inferType.type, p);
        });
    }

    protected async visitImportType(importType: ImportType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ImportType>(importType, p, async draft => {
            draft.hasTypeof = await this.visitRightPadded(importType.hasTypeof, p);
            draft.argumentAndAttributes = await this.visitContainer(importType.argumentAndAttributes, p);
            draft.qualifier = importType.qualifier && await this.visitLeftPadded(importType.qualifier, p);
            draft.typeArguments = importType.typeArguments && await this.visitContainer(importType.typeArguments, p);
            draft.type = importType.type && await this.visitType(importType.type, p);
        });
    }

    protected async visitJsImport(jsImport: JsImport, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JsImport>(jsImport, p, async draft => {
            draft.modifiers = await mapAsync(jsImport.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.importClause = jsImport.importClause && await this.visitDefined<JsImportClause>(jsImport.importClause, p);
            draft.moduleSpecifier = await this.visitLeftPadded(jsImport.moduleSpecifier, p);
            draft.attributes = jsImport.attributes && await this.visitDefined<ImportAttributes>(jsImport.attributes, p);
        });
    }

    protected async visitJsImportClause(jsImportClause: JsImportClause, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JsImportClause>(jsImportClause, p, async draft => {
            draft.name = jsImportClause.name && await this.visitRightPadded(jsImportClause.name, p);
            draft.namedBindings = jsImportClause.namedBindings && await this.visitDefined<Expression>(jsImportClause.namedBindings, p);
        });
    }

    protected async visitNamedImports(namedImports: NamedImports, p: P): Promise<J | undefined> {
        return this.produceJavaScript<NamedImports>(namedImports, p, async draft => {
            draft.elements = await this.visitContainer(namedImports.elements, p);
            draft.type = namedImports.type && await this.visitType(namedImports.type, p);
        });
    }

    protected async visitJsImportSpecifier(jsImportSpecifier: JsImportSpecifier, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JsImportSpecifier>(jsImportSpecifier, p, async draft => {
            draft.importType = await this.visitLeftPadded(jsImportSpecifier.importType, p);
            draft.specifier = await this.visitDefined<Expression>(jsImportSpecifier.specifier, p);
            draft.type = jsImportSpecifier.type && await this.visitType(jsImportSpecifier.type, p);
        });
    }

    protected async visitJSVariableDeclarations(jSVariableDeclarations: JSVariableDeclarations, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JSVariableDeclarations>(jSVariableDeclarations, p, async draft => {
            draft.leadingAnnotations = await mapAsync(jSVariableDeclarations.leadingAnnotations, item => this.visitDefined<J.Annotation>(item, p));
            draft.modifiers = await mapAsync(jSVariableDeclarations.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.typeExpression = jSVariableDeclarations.typeExpression && await this.visitDefined<TypeTree>(jSVariableDeclarations.typeExpression, p);
            draft.varargs = jSVariableDeclarations.varargs && await this.visitSpace(jSVariableDeclarations.varargs, p);
            draft.variables = await mapAsync(jSVariableDeclarations.variables, item => this.visitRightPadded(item, p));
        });
    }

    protected async visitJSNamedVariable(jSNamedVariable: JSNamedVariable, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JSNamedVariable>(jSNamedVariable, p, async draft => {
            draft.name = await this.visitDefined<Expression>(jSNamedVariable.name, p);
            draft.dimensionsAfterName = await mapAsync(jSNamedVariable.dimensionsAfterName, item => this.visitLeftPadded(item, p));
            draft.initializer = jSNamedVariable.initializer && await this.visitLeftPadded(jSNamedVariable.initializer, p);
            draft.variableType = jSNamedVariable.variableType && (await this.visitType(jSNamedVariable.variableType, p) as JavaType.Variable);
        });
    }

    protected async visitImportAttributes(importAttributes: ImportAttributes, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ImportAttributes>(importAttributes, p, async draft => {
            draft.elements = await this.visitContainer(importAttributes.elements, p);
        });
    }

    protected async visitImportTypeAttributes(importTypeAttributes: ImportTypeAttributes, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ImportTypeAttributes>(importTypeAttributes, p, async draft => {
            draft.token = await this.visitRightPadded(importTypeAttributes.token, p);
            draft.elements = await this.visitContainer(importTypeAttributes.elements, p);
            draft.end = await this.visitSpace(importTypeAttributes.end, p);
        });
    }

    protected async visitImportAttribute(importAttribute: ImportAttribute, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ImportAttribute>(importAttribute, p, async draft => {
            draft.name = await this.visitDefined<Expression>(importAttribute.name, p);
            draft.value = await this.visitLeftPadded(importAttribute.value, p);
        });
    }

    protected async visitJsBinary(jsBinary: JsBinary, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JsBinary>(jsBinary, p, async draft => {
            draft.left = await this.visitDefined<Expression>(jsBinary.left, p);
            draft.operator = await this.visitLeftPadded(jsBinary.operator, p);
            draft.right = await this.visitDefined<Expression>(jsBinary.right, p);
            draft.type = jsBinary.type && await this.visitType(jsBinary.type, p);
        });
    }

    protected async visitLiteralType(literalType: LiteralType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<LiteralType>(literalType, p, async draft => {
            draft.literal = await this.visitDefined<Expression>(literalType.literal, p);
            draft.type = (await this.visitType(literalType.type, p))!;
        });
    }

    protected async visitMappedType(mappedType: MappedType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<MappedType>(mappedType, p, async draft => {
            draft.prefixToken = mappedType.prefixToken && await this.visitLeftPadded(mappedType.prefixToken, p);
            draft.hasReadonly = await this.visitLeftPadded(mappedType.hasReadonly, p);
            draft.keysRemapping = await this.visitDefined<MappedType.KeysRemapping>(mappedType.keysRemapping, p);
            draft.suffixToken = mappedType.suffixToken && await this.visitLeftPadded(mappedType.suffixToken, p);
            draft.hasQuestionToken = await this.visitLeftPadded(mappedType.hasQuestionToken, p);
            draft.valueType = await this.visitContainer(mappedType.valueType, p);
            draft.type = mappedType.type && await this.visitType(mappedType.type, p);
        });
    }

    protected async visitKeysRemapping(keysRemapping: MappedType.KeysRemapping, p: P): Promise<J | undefined> {
        return this.produceJavaScript<MappedType.KeysRemapping>(keysRemapping, p, async draft => {
            draft.typeParameter = await this.visitRightPadded(keysRemapping.typeParameter, p);
            draft.nameType = keysRemapping.nameType && await this.visitRightPadded(keysRemapping.nameType, p);
        });
    }

    protected async visitMappedTypeParameter(mappedTypeParameter: MappedTypeParameter, p: P): Promise<J | undefined> {
        return this.produceJavaScript<MappedTypeParameter>(mappedTypeParameter, p, async draft => {
            draft.name = await this.visitDefined<Expression>(mappedTypeParameter.name, p);
            draft.iterateType = await this.visitLeftPadded(mappedTypeParameter.iterateType, p);
        });
    }

    protected async visitObjectBindingDeclarations(objectBindingDeclarations: ObjectBindingDeclarations, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ObjectBindingDeclarations>(objectBindingDeclarations, p, async draft => {
            draft.leadingAnnotations = await mapAsync(objectBindingDeclarations.leadingAnnotations, item => this.visitDefined<J.Annotation>(item, p));
            draft.modifiers = await mapAsync(objectBindingDeclarations.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.typeExpression = objectBindingDeclarations.typeExpression && await this.visitDefined<TypeTree>(objectBindingDeclarations.typeExpression, p);
            draft.bindings = await this.visitContainer(objectBindingDeclarations.bindings, p);
            draft.initializer = objectBindingDeclarations.initializer && await this.visitLeftPadded(objectBindingDeclarations.initializer, p);
        });
    }

    protected async visitPropertyAssignment(propertyAssignment: PropertyAssignment, p: P): Promise<J | undefined> {
        return this.produceJavaScript<PropertyAssignment>(propertyAssignment, p, async draft => {
            draft.name = await this.visitRightPadded(propertyAssignment.name, p);
            draft.initializer = propertyAssignment.initializer && await this.visitDefined<Expression>(propertyAssignment.initializer, p);
        });
    }

    protected async visitSatisfiesExpression(satisfiesExpression: SatisfiesExpression, p: P): Promise<J | undefined> {
        return this.produceJavaScript<SatisfiesExpression>(satisfiesExpression, p, async draft => {
            draft.expression = await this.visitDefined<J>(satisfiesExpression.expression, p);
            draft.satisfiesType = await this.visitLeftPadded(satisfiesExpression.satisfiesType, p);
            draft.type = satisfiesExpression.type && await this.visitType(satisfiesExpression.type, p);
        });
    }

    protected async visitScopedVariableDeclarations(scopedVariableDeclarations: ScopedVariableDeclarations, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ScopedVariableDeclarations>(scopedVariableDeclarations, p, async draft => {
            draft.modifiers = await mapAsync(scopedVariableDeclarations.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.scope = scopedVariableDeclarations.scope && await this.visitLeftPadded(scopedVariableDeclarations.scope, p);
            draft.variables = await mapAsync(scopedVariableDeclarations.variables, item => this.visitRightPadded(item, p));
        });
    }

    protected async visitStatementExpression(statementExpression: StatementExpression, p: P): Promise<J | undefined> {
        return this.produceJavaScript<StatementExpression>(statementExpression, p, async draft => {
            draft.statement = await this.visitDefined<Statement>(statementExpression.statement, p);
        });
    }

    protected async visitTaggedTemplateExpression(taggedTemplateExpression: TaggedTemplateExpression, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TaggedTemplateExpression>(taggedTemplateExpression, p, async draft => {
            draft.tag = taggedTemplateExpression.tag && await this.visitRightPadded(taggedTemplateExpression.tag, p);
            draft.typeArguments = taggedTemplateExpression.typeArguments && await this.visitContainer(taggedTemplateExpression.typeArguments, p);
            draft.templateExpression = await this.visitDefined<Expression>(taggedTemplateExpression.templateExpression, p);
            draft.type = taggedTemplateExpression.type && await this.visitType(taggedTemplateExpression.type, p);
        });
    }

    protected async visitTemplateExpression(templateExpression: TemplateExpression, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TemplateExpression>(templateExpression, p, async draft => {
            draft.head = await this.visitDefined<J.Literal>(templateExpression.head, p);
            draft.templateSpans = await mapAsync(templateExpression.templateSpans, item => this.visitRightPadded(item, p));
            draft.type = templateExpression.type && await this.visitType(templateExpression.type, p);
        });
    }

    protected async visitTemplateSpan(templateSpan: TemplateSpan, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TemplateSpan>(templateSpan, p, async draft => {
            draft.expression = await this.visitDefined<J>(templateSpan.expression, p);
            draft.tail = await this.visitDefined<J.Literal>(templateSpan.tail, p);
        });
    }

    protected async visitTrailingTokenStatement(trailingTokenStatement: TrailingTokenStatement, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TrailingTokenStatement>(trailingTokenStatement, p, async draft => {
            draft.expression = await this.visitRightPadded(trailingTokenStatement.expression, p);
            draft.type = trailingTokenStatement.type && await this.visitType(trailingTokenStatement.type, p);
        });
    }

    protected async visitTuple(tuple: Tuple, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Tuple>(tuple, p, async draft => {
            draft.elements = await this.visitContainer(tuple.elements, p);
            draft.type = tuple.type && await this.visitType(tuple.type, p);
        });
    }

    protected async visitTypeDeclaration(typeDeclaration: TypeDeclaration, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TypeDeclaration>(typeDeclaration, p, async draft => {
            draft.modifiers = await mapAsync(typeDeclaration.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.name = await this.visitLeftPadded(typeDeclaration.name, p);
            draft.typeParameters = typeDeclaration.typeParameters && await this.visitDefined<J.TypeParameters>(typeDeclaration.typeParameters, p);
            draft.initializer = await this.visitLeftPadded(typeDeclaration.initializer, p);
            draft.type = typeDeclaration.type && await this.visitType(typeDeclaration.type, p);
        });
    }

    protected async visitTypeOf(typeOf: TypeOf, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TypeOf>(typeOf, p, async draft => {
            draft.expression = await this.visitDefined<Expression>(typeOf.expression, p);
            draft.type = typeOf.type && await this.visitType(typeOf.type, p);
        });
    }

    protected async visitTypeTreeExpression(typeTreeExpression: TypeTreeExpression, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TypeTreeExpression>(typeTreeExpression, p, async draft => {
            draft.expression = await this.visitDefined<Expression>(typeTreeExpression.expression, p);
        });
    }

    protected async visitJsAssignmentOperation(jsAssignmentOperation: JsAssignmentOperation, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JsAssignmentOperation>(jsAssignmentOperation, p, async draft => {
            draft.variable = await this.visitDefined<Expression>(jsAssignmentOperation.variable, p);
            draft.operator = await this.visitLeftPadded(jsAssignmentOperation.operator, p);
            draft.assignment = await this.visitDefined<Expression>(jsAssignmentOperation.assignment, p);
            draft.type = jsAssignmentOperation.type && await this.visitType(jsAssignmentOperation.type, p);
        });
    }

    protected async visitIndexedAccessType(indexedAccessType: IndexedAccessType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<IndexedAccessType>(indexedAccessType, p, async draft => {
            draft.objectType = await this.visitDefined<TypeTree>(indexedAccessType.objectType, p);
            draft.indexType = await this.visitDefined<TypeTree>(indexedAccessType.indexType, p);
            draft.type = indexedAccessType.type && await this.visitType(indexedAccessType.type, p);
        });
    }

    protected async visitIndexType(indexType: IndexType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<IndexType>(indexType, p, async draft => {
            draft.element = await this.visitRightPadded(indexType.element, p);
            draft.type = indexType.type && await this.visitType(indexType.type, p);
        });
    }

    protected async visitTypeQuery(typeQuery: TypeQuery, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TypeQuery>(typeQuery, p, async draft => {
            draft.typeExpression = await this.visitDefined<TypeTree>(typeQuery.typeExpression, p);
            draft.typeArguments = typeQuery.typeArguments && await this.visitContainer(typeQuery.typeArguments, p);
            draft.type = typeQuery.type && await this.visitType(typeQuery.type, p);
        });
    }

    protected async visitTypeInfo(typeInfo: TypeInfo, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TypeInfo>(typeInfo, p, async draft => {
            draft.typeIdentifier = await this.visitDefined<TypeTree>(typeInfo.typeIdentifier, p);
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
            draft.parameterName = await this.visitDefined<J.Identifier>(typePredicate.parameterName, p);
            draft.expression = typePredicate.expression && await this.visitLeftPadded(typePredicate.expression, p);
            draft.type = typePredicate.type && await this.visitType(typePredicate.type, p);
        });
    }

    protected async visitUnion(union: Union, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Union>(union, p, async draft => {
            draft.types = await mapAsync(union.types, item => this.visitRightPadded(item, p));
            draft.type = union.type && await this.visitType(union.type, p);
        });
    }

    protected async visitIntersection(intersection: Intersection, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Intersection>(intersection, p, async draft => {
            draft.types = await mapAsync(intersection.types, item => this.visitRightPadded(item, p));
            draft.type = intersection.type && await this.visitType(intersection.type, p);
        });
    }

    protected async visitVoid(void_: Void, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Void>(void_, p, async draft => {
            draft.expression = await this.visitDefined<Expression>(void_.expression, p);
        });
    }

    protected async visitJsUnary(unary: Unary, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Unary>(unary, p, async draft => {
            draft.operator = await this.visitLeftPadded(unary.operator, p);
            draft.expression = await this.visitDefined<Expression>(unary.expression, p);
            draft.type = unary.type && await this.visitType(unary.type, p);
        });
    }

    protected async visitJsYield(yield_: Yield, p: P): Promise<J | undefined> {
        return this.produceJavaScript<Yield>(yield_, p, async draft => {
            draft.delegated = await this.visitLeftPadded(yield_.delegated, p);
            draft.expression = yield_.expression && await this.visitDefined<Expression>(yield_.expression, p);
            draft.type = yield_.type && await this.visitType(yield_.type, p);
        });
    }

    protected async visitWithStatement(withStatement: WithStatement, p: P): Promise<J | undefined> {
        return this.produceJavaScript<WithStatement>(withStatement, p, async draft => {
            draft.expression = await this.visitDefined<J.ControlParentheses<Expression>>(withStatement.expression, p);
            draft.body = await this.visitRightPadded(withStatement.body, p);
        });
    }

    protected async visitIndexSignatureDeclaration(indexSignatureDeclaration: IndexSignatureDeclaration, p: P): Promise<J | undefined> {
        return this.produceJavaScript<IndexSignatureDeclaration>(indexSignatureDeclaration, p, async draft => {
            draft.modifiers = await mapAsync(indexSignatureDeclaration.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.parameters = await this.visitContainer(indexSignatureDeclaration.parameters, p);
            draft.typeExpression = await this.visitLeftPadded(indexSignatureDeclaration.typeExpression, p);
            draft.type = indexSignatureDeclaration.type && await this.visitType(indexSignatureDeclaration.type, p);
        });
    }

    protected async visitJSMethodDeclaration(jSMethodDeclaration: JSMethodDeclaration, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JSMethodDeclaration>(jSMethodDeclaration, p, async draft => {
            draft.leadingAnnotations = await mapAsync(jSMethodDeclaration.leadingAnnotations, item => this.visitDefined<J.Annotation>(item, p));
            draft.modifiers = await mapAsync(jSMethodDeclaration.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.typeParameters = jSMethodDeclaration.typeParameters && await this.visitDefined<J.TypeParameters>(jSMethodDeclaration.typeParameters, p);
            draft.returnTypeExpression = jSMethodDeclaration.returnTypeExpression && await this.visitDefined<TypeTree>(jSMethodDeclaration.returnTypeExpression, p);
            draft.name = await this.visitDefined<Expression>(jSMethodDeclaration.name, p);
            draft.parameters = await this.visitContainer(jSMethodDeclaration.parameters, p);
            draft.body = jSMethodDeclaration.body && await this.visitDefined<J.Block>(jSMethodDeclaration.body, p);
            draft.defaultValue = jSMethodDeclaration.defaultValue && await this.visitLeftPadded(jSMethodDeclaration.defaultValue, p);
            draft.methodType = jSMethodDeclaration.methodType && (await this.visitType(jSMethodDeclaration.methodType, p) as JavaType.Method);
        });
    }

    protected async visitJSForOfLoop(jSForOfLoop: JSForOfLoop, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JSForOfLoop>(jSForOfLoop, p, async draft => {
            draft.await = await this.visitLeftPadded(jSForOfLoop.await, p);
            draft.control = await this.visitDefined<JSForInOfLoopControl>(jSForOfLoop.control, p);
            draft.body = await this.visitRightPadded(jSForOfLoop.body, p);
        });
    }

    protected async visitJSForInLoop(jSForInLoop: JSForInLoop, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JSForInLoop>(jSForInLoop, p, async draft => {
            draft.control = await this.visitDefined<JSForInOfLoopControl>(jSForInLoop.control, p);
            draft.body = await this.visitRightPadded(jSForInLoop.body, p);
        });
    }

    protected async visitJSForInOfLoopControl(jSForInOfLoopControl: JSForInOfLoopControl, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JSForInOfLoopControl>(jSForInOfLoopControl, p, async draft => {
            draft.variable = await this.visitRightPadded(jSForInOfLoopControl.variable, p);
            draft.iterable = await this.visitRightPadded(jSForInOfLoopControl.iterable, p);
        });
    }

    protected async visitJSTry(jSTry: JSTry, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JSTry>(jSTry, p, async draft => {
            draft.body = await this.visitDefined<J.Block>(jSTry.body, p);
            draft.catches = await this.visitDefined<JSTry.JSCatch>(jSTry.catches, p);
            draft.finally = jSTry.finally && await this.visitLeftPadded(jSTry.finally, p);
        });
    }

    protected async visitJSCatch(jSCatch: JSTry.JSCatch, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JSTry.JSCatch>(jSCatch, p, async draft => {
            draft.parameter = await this.visitDefined<J.ControlParentheses<JSVariableDeclarations>>(jSCatch.parameter, p);
            draft.body = await this.visitDefined<J.Block>(jSCatch.body, p);
        });
    }

    protected async visitNamespaceDeclaration(namespaceDeclaration: NamespaceDeclaration, p: P): Promise<J | undefined> {
        return this.produceJavaScript<NamespaceDeclaration>(namespaceDeclaration, p, async draft => {
            draft.modifiers = await mapAsync(namespaceDeclaration.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.keywordType = await this.visitLeftPadded(namespaceDeclaration.keywordType, p);
            draft.name = await this.visitRightPadded(namespaceDeclaration.name, p);
            draft.body = namespaceDeclaration.body && await this.visitDefined<J.Block>(namespaceDeclaration.body, p);
        });
    }

    protected async visitFunctionDeclaration(functionDeclaration: FunctionDeclaration, p: P): Promise<J | undefined> {
        return this.produceJavaScript<FunctionDeclaration>(functionDeclaration, p, async draft => {
            draft.modifiers = await mapAsync(functionDeclaration.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.asteriskToken = await this.visitLeftPadded(functionDeclaration.asteriskToken, p);
            draft.name = await this.visitLeftPadded(functionDeclaration.name, p);
            draft.typeParameters = functionDeclaration.typeParameters && await this.visitDefined<J.TypeParameters>(functionDeclaration.typeParameters, p);
            draft.parameters = await this.visitContainer(functionDeclaration.parameters, p);
            draft.returnTypeExpression = functionDeclaration.returnTypeExpression && await this.visitDefined<TypeTree>(functionDeclaration.returnTypeExpression, p);
            draft.body = functionDeclaration.body && await this.visitDefined<J>(functionDeclaration.body, p);
            draft.type = functionDeclaration.type && await this.visitType(functionDeclaration.type, p);
        });
    }

    protected async visitTypeLiteral(typeLiteral: TypeLiteral, p: P): Promise<J | undefined> {
        return this.produceJavaScript<TypeLiteral>(typeLiteral, p, async draft => {
            draft.members = await this.visitDefined<J.Block>(typeLiteral.members, p);
            draft.type = typeLiteral.type && await this.visitType(typeLiteral.type, p);
        });
    }

    protected async visitArrayBindingPattern(arrayBindingPattern: ArrayBindingPattern, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ArrayBindingPattern>(arrayBindingPattern, p, async draft => {
            draft.elements = await this.visitContainer(arrayBindingPattern.elements, p);
            draft.type = arrayBindingPattern.type && await this.visitType(arrayBindingPattern.type, p);
        });
    }

    protected async visitBindingElement(bindingElement: BindingElement, p: P): Promise<J | undefined> {
        return this.produceJavaScript<BindingElement>(bindingElement, p, async draft => {
            draft.propertyName = bindingElement.propertyName && await this.visitRightPadded(bindingElement.propertyName, p);
            draft.name = await this.visitDefined<TypedTree>(bindingElement.name, p);
            draft.initializer = bindingElement.initializer && await this.visitLeftPadded(bindingElement.initializer, p);
            draft.variableType = bindingElement.variableType && (await this.visitType(bindingElement.variableType, p) as JavaType.Variable);
        });
    }

    protected async visitExportDeclaration(exportDeclaration: ExportDeclaration, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ExportDeclaration>(exportDeclaration, p, async draft => {
            draft.modifiers = await mapAsync(exportDeclaration.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.typeOnly = await this.visitLeftPadded(exportDeclaration.typeOnly, p);
            draft.exportClause = exportDeclaration.exportClause && await this.visitDefined<Expression>(exportDeclaration.exportClause, p);
            draft.moduleSpecifier = exportDeclaration.moduleSpecifier && await this.visitLeftPadded(exportDeclaration.moduleSpecifier, p);
            draft.attributes = exportDeclaration.attributes && await this.visitDefined<ImportAttributes>(exportDeclaration.attributes, p);
        });
    }

    protected async visitExportAssignment(exportAssignment: ExportAssignment, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ExportAssignment>(exportAssignment, p, async draft => {
            draft.modifiers = await mapAsync(exportAssignment.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.exportEquals = await this.visitLeftPadded(exportAssignment.exportEquals, p);
            draft.expression = exportAssignment.expression && await this.visitDefined<Expression>(exportAssignment.expression, p);
        });
    }

    protected async visitNamedExports(namedExports: NamedExports, p: P): Promise<J | undefined> {
        return this.produceJavaScript<NamedExports>(namedExports, p, async draft => {
            draft.elements = await this.visitContainer(namedExports.elements, p);
            draft.type = namedExports.type && await this.visitType(namedExports.type, p);
        });
    }

    protected async visitExportSpecifier(exportSpecifier: ExportSpecifier, p: P): Promise<J | undefined> {
        return this.produceJavaScript<ExportSpecifier>(exportSpecifier, p, async draft => {
            draft.typeOnly = await this.visitLeftPadded(exportSpecifier.typeOnly, p);
            draft.specifier = await this.visitDefined<Expression>(exportSpecifier.specifier, p);
            draft.type = exportSpecifier.type && await this.visitType(exportSpecifier.type, p);
        });
    }

    override async accept<J2 extends J, P2 extends P>(j: J2, p: P2): Promise<J | undefined> {
        if (isJavaScript(j)) {
            const tree = j as JS;
            switch (tree.kind) {
                case JavaScriptKind.Alias:
                    return this.visitAlias(tree as unknown as Alias, p);
                case JavaScriptKind.ArrowFunction:
                    return this.visitArrowFunction(tree as unknown as ArrowFunction, p);
                case JavaScriptKind.Await:
                    return this.visitAwait(tree as unknown as Await, p);
                case JavaScriptKind.CompilationUnit:
                    return this.visitJsCompilationUnit(tree as unknown as CompilationUnit, p);
                case JavaScriptKind.ConditionalType:
                    return this.visitConditionalType(tree as unknown as ConditionalType, p);
                case JavaScriptKind.DefaultType:
                    return this.visitDefaultType(tree as unknown as DefaultType, p);
                case JavaScriptKind.Delete:
                    return this.visitDelete(tree as unknown as Delete, p);
                case JavaScriptKind.Export:
                    return this.visitExport(tree as unknown as Export, p);
                case JavaScriptKind.ExpressionStatement:
                    return this.visitExpressionStatement(tree as unknown as ExpressionStatement, p);
                case JavaScriptKind.ExpressionWithTypeArguments:
                    return this.visitExpressionWithTypeArguments(tree as unknown as ExpressionWithTypeArguments, p);
                case JavaScriptKind.FunctionType:
                    return this.visitFunctionType(tree as unknown as FunctionType, p);
                case JavaScriptKind.InferType:
                    return this.visitInferType(tree as unknown as InferType, p);
                case JavaScriptKind.ImportType:
                    return this.visitImportType(tree as unknown as ImportType, p);
                case JavaScriptKind.JsImport:
                    return this.visitJsImport(tree as unknown as JsImport, p);
                case JavaScriptKind.JsImportClause:
                    return this.visitJsImportClause(tree as unknown as JsImportClause, p);
                case JavaScriptKind.NamedImports:
                    return this.visitNamedImports(tree as unknown as NamedImports, p);
                case JavaScriptKind.JsImportSpecifier:
                    return this.visitJsImportSpecifier(tree as unknown as JsImportSpecifier, p);
                case JavaScriptKind.JSVariableDeclarations:
                    return this.visitJSVariableDeclarations(tree as unknown as JSVariableDeclarations, p);
                case JavaScriptKind.JSNamedVariable:
                    return this.visitJSNamedVariable(tree as unknown as JSNamedVariable, p);
                case JavaScriptKind.ImportAttributes:
                    return this.visitImportAttributes(tree as unknown as ImportAttributes, p);
                case JavaScriptKind.ImportTypeAttributes:
                    return this.visitImportTypeAttributes(tree as unknown as ImportTypeAttributes, p);
                case JavaScriptKind.ImportAttribute:
                    return this.visitImportAttribute(tree as unknown as ImportAttribute, p);
                case JavaScriptKind.JsBinary:
                    return this.visitJsBinary(tree as unknown as JsBinary, p);
                case JavaScriptKind.LiteralType:
                    return this.visitLiteralType(tree as unknown as LiteralType, p);
                case JavaScriptKind.MappedType:
                    return this.visitMappedType(tree as unknown as MappedType, p);
                case JavaScriptKind.MappedTypeKeysRemapping:
                    return this.visitKeysRemapping(tree as unknown as MappedType.KeysRemapping, p);
                case JavaScriptKind.MappedTypeMappedTypeParameter:
                    return this.visitMappedTypeParameter(tree as unknown as MappedTypeParameter, p);
                case JavaScriptKind.ObjectBindingDeclarations:
                    return this.visitObjectBindingDeclarations(tree as unknown as ObjectBindingDeclarations, p);
                case JavaScriptKind.PropertyAssignment:
                    return this.visitPropertyAssignment(tree as unknown as PropertyAssignment, p);
                case JavaScriptKind.SatisfiesExpression:
                    return this.visitSatisfiesExpression(tree as unknown as SatisfiesExpression, p);
                case JavaScriptKind.ScopedVariableDeclarations:
                    return this.visitScopedVariableDeclarations(tree as unknown as ScopedVariableDeclarations, p);
                case JavaScriptKind.StatementExpression:
                    return this.visitStatementExpression(tree as unknown as StatementExpression, p);
                case JavaScriptKind.TaggedTemplateExpression:
                    return this.visitTaggedTemplateExpression(tree as unknown as TaggedTemplateExpression, p);
                case JavaScriptKind.TemplateExpression:
                    return this.visitTemplateExpression(tree as unknown as TemplateExpression, p);
                case JavaScriptKind.TemplateExpressionTemplateSpan:
                    return this.visitTemplateSpan(tree as unknown as TemplateSpan, p);
                case JavaScriptKind.TrailingTokenStatement:
                    return this.visitTrailingTokenStatement(tree as unknown as TrailingTokenStatement, p);
                case JavaScriptKind.Tuple:
                    return this.visitTuple(tree as unknown as Tuple, p);
                case JavaScriptKind.TypeDeclaration:
                    return this.visitTypeDeclaration(tree as unknown as TypeDeclaration, p);
                case JavaScriptKind.TypeOf:
                    return this.visitTypeOf(tree as unknown as TypeOf, p);
                case JavaScriptKind.TypeTreeExpression:
                    return this.visitTypeTreeExpression(tree as unknown as TypeTreeExpression, p);
                case JavaScriptKind.JsAssignmentOperation:
                    return this.visitJsAssignmentOperation(tree as unknown as JsAssignmentOperation, p);
                case JavaScriptKind.IndexedAccessType:
                    return this.visitIndexedAccessType(tree as unknown as IndexedAccessType, p);
                case JavaScriptKind.IndexType:
                    return this.visitIndexType(tree as unknown as IndexedAccessType.IndexType, p);
                case JavaScriptKind.TypeQuery:
                    return this.visitTypeQuery(tree as unknown as TypeQuery, p);
                case JavaScriptKind.TypeInfo:
                    return this.visitTypeInfo(tree as unknown as TypeInfo, p);
                case JavaScriptKind.TypeOperator:
                    return this.visitTypeOperator(tree as unknown as TypeOperator, p);
                case JavaScriptKind.TypePredicate:
                    return this.visitTypePredicate(tree as unknown as TypePredicate, p);
                case JavaScriptKind.Union:
                    return this.visitUnion(tree as unknown as Union, p);
                case JavaScriptKind.Intersection:
                    return this.visitIntersection(tree as unknown as Intersection, p);
                case JavaScriptKind.Void:
                    return this.visitVoid(tree as unknown as Void, p);
                case JavaScriptKind.Unary:
                    return this.visitJsUnary(tree as unknown as Unary, p);
                case JavaScriptKind.Yield:
                    return this.visitJsYield(tree as unknown as Yield, p);
                case JavaScriptKind.WithStatement:
                    return this.visitWithStatement(tree as unknown as WithStatement, p);
                case JavaScriptKind.IndexSignatureDeclaration:
                    return this.visitIndexSignatureDeclaration(tree as unknown as IndexSignatureDeclaration, p);
                case JavaScriptKind.JSMethodDeclaration:
                    return this.visitJSMethodDeclaration(tree as unknown as JSMethodDeclaration, p);
                case JavaScriptKind.JSForOfLoop:
                    return this.visitJSForOfLoop(tree as unknown as JSForOfLoop, p);
                case JavaScriptKind.JSForInLoop:
                    return this.visitJSForInLoop(tree as unknown as JSForInLoop, p);
                case JavaScriptKind.JSForInOfLoopControl:
                    return this.visitJSForInOfLoopControl(tree as unknown as JSForInOfLoopControl, p);
                case JavaScriptKind.JSTry:
                    return this.visitJSTry(tree as unknown as JSTry, p);
                case JavaScriptKind.JSCatch:
                    return this.visitJSCatch(tree as unknown as JSTry.JSCatch, p);
                case JavaScriptKind.NamespaceDeclaration:
                    return this.visitNamespaceDeclaration(tree as unknown as NamespaceDeclaration, p);
                case JavaScriptKind.FunctionDeclaration:
                    return this.visitFunctionDeclaration(tree as unknown as FunctionDeclaration, p);
                case JavaScriptKind.TypeLiteral:
                    return this.visitTypeLiteral(tree as unknown as TypeLiteral, p);
                case JavaScriptKind.ArrayBindingPattern:
                    return this.visitArrayBindingPattern(tree as unknown as ArrayBindingPattern, p);
                case JavaScriptKind.BindingElement:
                    return this.visitBindingElement(tree as unknown as BindingElement, p);
                case JavaScriptKind.ExportDeclaration:
                    return this.visitExportDeclaration(tree as unknown as ExportDeclaration, p);
                case JavaScriptKind.ExportAssignment:
                    return this.visitExportAssignment(tree as unknown as ExportAssignment, p);
                case JavaScriptKind.NamedExports:
                    return this.visitNamedExports(tree as unknown as NamedExports, p);
                case JavaScriptKind.ExportSpecifier:
                    return this.visitExportSpecifier(tree as unknown as ExportSpecifier, p);
            }
        }
        return super.accept(j, p);
    }
}
