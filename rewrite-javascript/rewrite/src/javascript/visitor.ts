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
import {Expression, J, JavaType, JavaVisitor, NameTree, Statement, TypedTree} from "../java";
import {createDraft, Draft, finishDraft} from "immer";
import {isJavaScript, JS} from "./tree";

export class JavaScriptVisitor<P> extends JavaVisitor<P> {

    override isAcceptable(sourceFile: SourceFile): boolean {
        return isJavaScript(sourceFile);
    }

    // noinspection JSUnusedGlobalSymbols,JSUnusedLocalSymbols
    override async visitExpression(expression: Expression, p: P): Promise<J | undefined> {
        return expression;
    }

    // noinspection JSUnusedGlobalSymbols,JSUnusedLocalSymbols
    override async visitStatement(statement: Statement, p: P): Promise<J | undefined> {
        return statement;
    }

    // noinspection JSUnusedLocalSymbols
    override async visitSpace(space: J.Space, p: P): Promise<J.Space> {
        return space;
    }

    // noinspection JSUnusedLocalSymbols
    override async visitType(javaType: JavaType | undefined, p: P): Promise<JavaType | undefined> {
        return javaType;
    }

    // noinspection JSUnusedLocalSymbols
    override async visitTypeName<N extends NameTree>(nameTree: N, p: P): Promise<N> {
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

    protected async visitAlias(alias: JS.Alias, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.Alias>(alias, p, async draft => {
            draft.propertyName = await this.visitRightPadded(alias.propertyName, p);
            draft.alias = await this.visitDefined<Expression>(alias.alias, p);
        });
    }

    protected async visitArrowFunction(arrowFunction: JS.ArrowFunction, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.ArrowFunction>(arrowFunction, p, async draft => {
            draft.leadingAnnotations = await mapAsync(arrowFunction.leadingAnnotations, item => this.visitDefined<J.Annotation>(item, p));
            draft.modifiers = await mapAsync(arrowFunction.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.typeParameters = arrowFunction.typeParameters && await this.visitDefined<J.TypeParameters>(arrowFunction.typeParameters, p);
            draft.parameters = await this.visitDefined<J.Lambda.Parameters>(arrowFunction.parameters, p);
            draft.returnTypeExpression = arrowFunction.returnTypeExpression && await this.visitDefined<TypedTree>(arrowFunction.returnTypeExpression, p);
            draft.body = await this.visitLeftPadded(arrowFunction.body, p);
            draft.type = arrowFunction.type && await this.visitType(arrowFunction.type, p);
        });
    }

    protected async visitAwait(await_: JS.Await, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.Await>(await_, p, async draft => {
            draft.expression = await this.visitDefined<Expression>(await_.expression, p);
            draft.type = await_.type && await this.visitType(await_.type, p);
        });
    }

    protected async visitJsCompilationUnit(compilationUnit: JS.CompilationUnit, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.CompilationUnit>(compilationUnit, p, async draft => {
            draft.imports = await mapAsync(compilationUnit.imports, imp => this.visitRightPadded(imp, p));
            draft.statements = await mapAsync(compilationUnit.statements, stmt => this.visitRightPadded(stmt, p));
            draft.eof = await this.visitSpace(compilationUnit.eof, p);
        });
    }

    protected async visitConditionalType(conditionalType: JS.ConditionalType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.ConditionalType>(conditionalType, p, async draft => {
            draft.checkType = await this.visitDefined<Expression>(conditionalType.checkType, p);
            draft.condition = await this.visitLeftPadded(conditionalType.condition, p);
            draft.type = conditionalType.type && await this.visitType(conditionalType.type, p);
        });
    }

    protected async visitDelete(delete_: JS.Delete, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.Delete>(delete_, p, async draft => {
            draft.expression = await this.visitDefined<Expression>(delete_.expression, p);
        });
    }

    protected async visitExpressionStatement(expressionStatement: JS.ExpressionStatement, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.ExpressionStatement>(expressionStatement, p, async draft => {
            draft.expression = await this.visitDefined<Expression>(expressionStatement.expression, p);
        });
    }

    protected async visitExpressionWithTypeArguments(expressionWithTypeArguments: JS.ExpressionWithTypeArguments, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.ExpressionWithTypeArguments>(expressionWithTypeArguments, p, async draft => {
            draft.clazz = await this.visitDefined<J>(expressionWithTypeArguments.clazz, p);
            draft.typeArguments = expressionWithTypeArguments.typeArguments && await this.visitContainer(expressionWithTypeArguments.typeArguments, p);
            draft.type = expressionWithTypeArguments.type && await this.visitType(expressionWithTypeArguments.type, p);
        });
    }

    protected async visitFunctionType(functionType: JS.FunctionType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.FunctionType>(functionType, p, async draft => {
            draft.constructorType = await this.visitLeftPadded(functionType.constructorType, p);
            draft.typeParameters = functionType.typeParameters && await this.visitDefined<J.TypeParameters>(functionType.typeParameters, p);
            draft.parameters = await this.visitContainer(functionType.parameters, p);
            draft.returnType = await this.visitLeftPadded(functionType.returnType, p);
        });
    }

    protected async visitInferType(inferType: JS.InferType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.InferType>(inferType, p, async draft => {
            draft.typeParameter = await this.visitLeftPadded(inferType.typeParameter, p);
            draft.type = inferType.type && await this.visitType(inferType.type, p);
        });
    }

    protected async visitImportType(importType: JS.ImportType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.ImportType>(importType, p, async draft => {
            draft.hasTypeof = await this.visitRightPadded(importType.hasTypeof, p);
            draft.argumentAndAttributes = await this.visitContainer(importType.argumentAndAttributes, p);
            draft.qualifier = importType.qualifier && await this.visitLeftPadded(importType.qualifier, p);
            draft.typeArguments = importType.typeArguments && await this.visitContainer(importType.typeArguments, p);
            draft.type = importType.type && await this.visitType(importType.type, p);
        });
    }

    protected async visitJsImport(jsImport: JS.JsImport, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.JsImport>(jsImport, p, async draft => {
            draft.modifiers = await mapAsync(jsImport.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.importClause = jsImport.importClause && await this.visitDefined<JS.JsImportClause>(jsImport.importClause, p);
            draft.moduleSpecifier = await this.visitLeftPadded(jsImport.moduleSpecifier, p);
            draft.attributes = jsImport.attributes && await this.visitDefined<JS.ImportAttributes>(jsImport.attributes, p);
        });
    }

    protected async visitJsImportClause(jsImportClause: JS.JsImportClause, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.JsImportClause>(jsImportClause, p, async draft => {
            draft.name = jsImportClause.name && await this.visitRightPadded(jsImportClause.name, p);
            draft.namedBindings = jsImportClause.namedBindings && await this.visitDefined<Expression>(jsImportClause.namedBindings, p);
        });
    }

    protected async visitNamedImports(namedImports: JS.NamedImports, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.NamedImports>(namedImports, p, async draft => {
            draft.elements = await this.visitContainer(namedImports.elements, p);
            draft.type = namedImports.type && await this.visitType(namedImports.type, p);
        });
    }

    protected async visitJsImportSpecifier(jsImportSpecifier: JS.JsImportSpecifier, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.JsImportSpecifier>(jsImportSpecifier, p, async draft => {
            draft.importType = await this.visitLeftPadded(jsImportSpecifier.importType, p);
            draft.specifier = await this.visitDefined<Expression>(jsImportSpecifier.specifier, p);
            draft.type = jsImportSpecifier.type && await this.visitType(jsImportSpecifier.type, p);
        });
    }

    protected async visitJSVariableDeclarations(jSVariableDeclarations: JS.JSVariableDeclarations, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.JSVariableDeclarations>(jSVariableDeclarations, p, async draft => {
            draft.leadingAnnotations = await mapAsync(jSVariableDeclarations.leadingAnnotations, item => this.visitDefined<J.Annotation>(item, p));
            draft.modifiers = await mapAsync(jSVariableDeclarations.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.typeExpression = jSVariableDeclarations.typeExpression && await this.visitDefined<TypedTree>(jSVariableDeclarations.typeExpression, p);
            draft.varargs = jSVariableDeclarations.varargs && await this.visitSpace(jSVariableDeclarations.varargs, p);
            draft.variables = await mapAsync(jSVariableDeclarations.variables, item => this.visitRightPadded(item, p));
        });
    }

    protected async visitJSNamedVariable(jSNamedVariable: JS.JSVariableDeclarations.JSNamedVariable, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.JSVariableDeclarations.JSNamedVariable>(jSNamedVariable, p, async draft => {
            draft.name = await this.visitDefined<Expression>(jSNamedVariable.name, p);
            draft.dimensionsAfterName = await mapAsync(jSNamedVariable.dimensionsAfterName, item => this.visitLeftPadded(item, p));
            draft.initializer = jSNamedVariable.initializer && await this.visitLeftPadded(jSNamedVariable.initializer, p);
            draft.variableType = jSNamedVariable.variableType && (await this.visitType(jSNamedVariable.variableType, p) as JavaType.Variable);
        });
    }

    protected async visitImportAttributes(importAttributes: JS.ImportAttributes, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.ImportAttributes>(importAttributes, p, async draft => {
            draft.elements = await this.visitContainer(importAttributes.elements, p);
        });
    }

    protected async visitImportTypeAttributes(importTypeAttributes: JS.ImportTypeAttributes, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.ImportTypeAttributes>(importTypeAttributes, p, async draft => {
            draft.token = await this.visitRightPadded(importTypeAttributes.token, p);
            draft.elements = await this.visitContainer(importTypeAttributes.elements, p);
            draft.end = await this.visitSpace(importTypeAttributes.end, p);
        });
    }

    protected async visitImportAttribute(importAttribute: JS.ImportAttribute, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.ImportAttribute>(importAttribute, p, async draft => {
            draft.name = await this.visitDefined<Expression>(importAttribute.name, p);
            draft.value = await this.visitLeftPadded(importAttribute.value, p);
        });
    }

    protected async visitJsBinary(jsBinary: JS.JsBinary, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.JsBinary>(jsBinary, p, async draft => {
            draft.left = await this.visitDefined<Expression>(jsBinary.left, p);
            draft.operator = await this.visitLeftPadded(jsBinary.operator, p);
            draft.right = await this.visitDefined<Expression>(jsBinary.right, p);
            draft.type = jsBinary.type && await this.visitType(jsBinary.type, p);
        });
    }

    protected async visitLiteralType(literalType: JS.LiteralType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.LiteralType>(literalType, p, async draft => {
            draft.literal = await this.visitDefined<Expression>(literalType.literal, p);
            draft.type = (await this.visitType(literalType.type, p))!;
        });
    }

    protected async visitMappedType(mappedType: JS.MappedType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.MappedType>(mappedType, p, async draft => {
            draft.prefixToken = mappedType.prefixToken && await this.visitLeftPadded(mappedType.prefixToken, p);
            draft.hasReadonly = await this.visitLeftPadded(mappedType.hasReadonly, p);
            draft.keysRemapping = await this.visitDefined<JS.MappedType.KeysRemapping>(mappedType.keysRemapping, p);
            draft.suffixToken = mappedType.suffixToken && await this.visitLeftPadded(mappedType.suffixToken, p);
            draft.hasQuestionToken = await this.visitLeftPadded(mappedType.hasQuestionToken, p);
            draft.valueType = await this.visitContainer(mappedType.valueType, p);
            draft.type = mappedType.type && await this.visitType(mappedType.type, p);
        });
    }

    protected async visitKeysRemapping(keysRemapping: JS.MappedType.KeysRemapping, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.MappedType.KeysRemapping>(keysRemapping, p, async draft => {
            draft.typeParameter = await this.visitRightPadded(keysRemapping.typeParameter, p);
            draft.nameType = keysRemapping.nameType && await this.visitRightPadded(keysRemapping.nameType, p);
        });
    }

    protected async visitMappedTypeParameter(mappedTypeParameter: JS.MappedType.MappedTypeParameter, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.MappedType.MappedTypeParameter>(mappedTypeParameter, p, async draft => {
            draft.name = await this.visitDefined<Expression>(mappedTypeParameter.name, p);
            draft.iterateType = await this.visitLeftPadded(mappedTypeParameter.iterateType, p);
        });
    }

    protected async visitObjectBindingDeclarations(objectBindingDeclarations: JS.ObjectBindingDeclarations, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.ObjectBindingDeclarations>(objectBindingDeclarations, p, async draft => {
            draft.leadingAnnotations = await mapAsync(objectBindingDeclarations.leadingAnnotations, item => this.visitDefined<J.Annotation>(item, p));
            draft.modifiers = await mapAsync(objectBindingDeclarations.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.typeExpression = objectBindingDeclarations.typeExpression && await this.visitDefined<TypedTree>(objectBindingDeclarations.typeExpression, p);
            draft.bindings = await this.visitContainer(objectBindingDeclarations.bindings, p);
            draft.initializer = objectBindingDeclarations.initializer && await this.visitLeftPadded(objectBindingDeclarations.initializer, p);
        });
    }

    protected async visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.PropertyAssignment>(propertyAssignment, p, async draft => {
            draft.name = await this.visitRightPadded(propertyAssignment.name, p);
            draft.initializer = propertyAssignment.initializer && await this.visitDefined<Expression>(propertyAssignment.initializer, p);
        });
    }

    protected async visitSatisfiesExpression(satisfiesExpression: JS.SatisfiesExpression, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.SatisfiesExpression>(satisfiesExpression, p, async draft => {
            draft.expression = await this.visitDefined<J>(satisfiesExpression.expression, p);
            draft.satisfiesType = await this.visitLeftPadded(satisfiesExpression.satisfiesType, p);
            draft.type = satisfiesExpression.type && await this.visitType(satisfiesExpression.type, p);
        });
    }

    protected async visitScopedVariableDeclarations(scopedVariableDeclarations: JS.ScopedVariableDeclarations, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.ScopedVariableDeclarations>(scopedVariableDeclarations, p, async draft => {
            draft.modifiers = await mapAsync(scopedVariableDeclarations.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.scope = scopedVariableDeclarations.scope && await this.visitLeftPadded(scopedVariableDeclarations.scope, p);
            draft.variables = await mapAsync(scopedVariableDeclarations.variables, item => this.visitRightPadded(item, p));
        });
    }

    protected async visitStatementExpression(statementExpression: JS.StatementExpression, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.StatementExpression>(statementExpression, p, async draft => {
            draft.statement = await this.visitDefined<Statement>(statementExpression.statement, p);
        });
    }

    protected async visitTaggedTemplateExpression(taggedTemplateExpression: JS.TaggedTemplateExpression, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.TaggedTemplateExpression>(taggedTemplateExpression, p, async draft => {
            draft.tag = taggedTemplateExpression.tag && await this.visitRightPadded(taggedTemplateExpression.tag, p);
            draft.typeArguments = taggedTemplateExpression.typeArguments && await this.visitContainer(taggedTemplateExpression.typeArguments, p);
            draft.templateExpression = await this.visitDefined<Expression>(taggedTemplateExpression.templateExpression, p);
            draft.type = taggedTemplateExpression.type && await this.visitType(taggedTemplateExpression.type, p);
        });
    }

    protected async visitTemplateExpression(templateExpression: JS.TemplateExpression, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.TemplateExpression>(templateExpression, p, async draft => {
            draft.head = await this.visitDefined<J.Literal>(templateExpression.head, p);
            draft.spans = await mapAsync(templateExpression.spans, item => this.visitRightPadded(item, p));
            draft.type = templateExpression.type && await this.visitType(templateExpression.type, p);
        });
    }

    protected async visitTemplateExpressionSpan(span: JS.TemplateExpression.Span, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.TemplateExpression.Span>(span, p, async draft => {
            draft.expression = await this.visitDefined<J>(span.expression, p);
            draft.tail = await this.visitDefined<J.Literal>(span.tail, p);
        });
    }

    protected async visitTrailingTokenStatement(trailingTokenStatement: JS.TrailingTokenStatement, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.TrailingTokenStatement>(trailingTokenStatement, p, async draft => {
            draft.expression = await this.visitRightPadded(trailingTokenStatement.expression, p);
            draft.type = trailingTokenStatement.type && await this.visitType(trailingTokenStatement.type, p);
        });
    }

    protected async visitTuple(tuple: JS.Tuple, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.Tuple>(tuple, p, async draft => {
            draft.elements = await this.visitContainer(tuple.elements, p);
            draft.type = tuple.type && await this.visitType(tuple.type, p);
        });
    }

    protected async visitTypeDeclaration(typeDeclaration: JS.TypeDeclaration, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.TypeDeclaration>(typeDeclaration, p, async draft => {
            draft.modifiers = await mapAsync(typeDeclaration.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.name = await this.visitLeftPadded(typeDeclaration.name, p);
            draft.typeParameters = typeDeclaration.typeParameters && await this.visitDefined<J.TypeParameters>(typeDeclaration.typeParameters, p);
            draft.initializer = await this.visitLeftPadded(typeDeclaration.initializer, p);
            draft.type = typeDeclaration.type && await this.visitType(typeDeclaration.type, p);
        });
    }

    protected async visitTypeOf(typeOf: JS.TypeOf, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.TypeOf>(typeOf, p, async draft => {
            draft.expression = await this.visitDefined<Expression>(typeOf.expression, p);
            draft.type = typeOf.type && await this.visitType(typeOf.type, p);
        });
    }

    protected async visitTypeTreeExpression(typeTreeExpression: JS.TypeTreeExpression, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.TypeTreeExpression>(typeTreeExpression, p, async draft => {
            draft.expression = await this.visitDefined<Expression>(typeTreeExpression.expression, p);
        });
    }

    protected async visitJsAssignmentOperation(jsAssignmentOperation: JS.JsAssignmentOperation, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.JsAssignmentOperation>(jsAssignmentOperation, p, async draft => {
            draft.variable = await this.visitDefined<Expression>(jsAssignmentOperation.variable, p);
            draft.operator = await this.visitLeftPadded(jsAssignmentOperation.operator, p);
            draft.assignment = await this.visitDefined<Expression>(jsAssignmentOperation.assignment, p);
            draft.type = jsAssignmentOperation.type && await this.visitType(jsAssignmentOperation.type, p);
        });
    }

    protected async visitIndexedAccessType(indexedAccessType: JS.IndexedAccessType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.IndexedAccessType>(indexedAccessType, p, async draft => {
            draft.objectType = await this.visitDefined<TypedTree>(indexedAccessType.objectType, p);
            draft.indexType = await this.visitDefined<TypedTree>(indexedAccessType.indexType, p);
            draft.type = indexedAccessType.type && await this.visitType(indexedAccessType.type, p);
        });
    }

    protected async visitIndexType(indexType: JS.IndexedAccessType.IndexType, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.IndexedAccessType.IndexType>(indexType, p, async draft => {
            draft.element = await this.visitRightPadded(indexType.element, p);
            draft.type = indexType.type && await this.visitType(indexType.type, p);
        });
    }

    protected async visitTypeQuery(typeQuery: JS.TypeQuery, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.TypeQuery>(typeQuery, p, async draft => {
            draft.typeExpression = await this.visitDefined<TypedTree>(typeQuery.typeExpression, p);
            draft.typeArguments = typeQuery.typeArguments && await this.visitContainer(typeQuery.typeArguments, p);
            draft.type = typeQuery.type && await this.visitType(typeQuery.type, p);
        });
    }

    protected async visitTypeInfo(typeInfo: JS.TypeInfo, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.TypeInfo>(typeInfo, p, async draft => {
            draft.typeIdentifier = await this.visitDefined<TypedTree>(typeInfo.typeIdentifier, p);
        });
    }

    protected async visitTypeOperator(typeOperator: JS.TypeOperator, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.TypeOperator>(typeOperator, p, async draft => {
            draft.expression = await this.visitLeftPadded(typeOperator.expression, p);
        });
    }

    protected async visitTypePredicate(typePredicate: JS.TypePredicate, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.TypePredicate>(typePredicate, p, async draft => {
            draft.asserts = await this.visitLeftPadded(typePredicate.asserts, p);
            draft.parameterName = await this.visitDefined<J.Identifier>(typePredicate.parameterName, p);
            draft.expression = typePredicate.expression && await this.visitLeftPadded(typePredicate.expression, p);
            draft.type = typePredicate.type && await this.visitType(typePredicate.type, p);
        });
    }

    protected async visitUnion(union: JS.Union, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.Union>(union, p, async draft => {
            draft.types = await mapAsync(union.types, item => this.visitRightPadded(item, p));
            draft.type = union.type && await this.visitType(union.type, p);
        });
    }

    protected async visitIntersection(intersection: JS.Intersection, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.Intersection>(intersection, p, async draft => {
            draft.types = await mapAsync(intersection.types, item => this.visitRightPadded(item, p));
            draft.type = intersection.type && await this.visitType(intersection.type, p);
        });
    }

    protected async visitVoid(void_: JS.Void, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.Void>(void_, p, async draft => {
            draft.expression = await this.visitDefined<Expression>(void_.expression, p);
        });
    }

    protected async visitJsUnary(unary: JS.Unary, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.Unary>(unary, p, async draft => {
            draft.operator = await this.visitLeftPadded(unary.operator, p);
            draft.expression = await this.visitDefined<Expression>(unary.expression, p);
            draft.type = unary.type && await this.visitType(unary.type, p);
        });
    }

    protected async visitJsYield(yield_: JS.Yield, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.Yield>(yield_, p, async draft => {
            draft.delegated = await this.visitLeftPadded(yield_.delegated, p);
            draft.expression = yield_.expression && await this.visitDefined<Expression>(yield_.expression, p);
            draft.type = yield_.type && await this.visitType(yield_.type, p);
        });
    }

    protected async visitWithStatement(withStatement: JS.WithStatement, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.WithStatement>(withStatement, p, async draft => {
            draft.expression = await this.visitDefined<J.ControlParentheses<Expression>>(withStatement.expression, p);
            draft.body = await this.visitRightPadded(withStatement.body, p);
        });
    }

    protected async visitIndexSignatureDeclaration(indexSignatureDeclaration: JS.IndexSignatureDeclaration, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.IndexSignatureDeclaration>(indexSignatureDeclaration, p, async draft => {
            draft.modifiers = await mapAsync(indexSignatureDeclaration.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.parameters = await this.visitContainer(indexSignatureDeclaration.parameters, p);
            draft.typeExpression = await this.visitLeftPadded(indexSignatureDeclaration.typeExpression, p);
            draft.type = indexSignatureDeclaration.type && await this.visitType(indexSignatureDeclaration.type, p);
        });
    }

    protected async visitJSMethodDeclaration(jSMethodDeclaration: JS.JSMethodDeclaration, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.JSMethodDeclaration>(jSMethodDeclaration, p, async draft => {
            draft.leadingAnnotations = await mapAsync(jSMethodDeclaration.leadingAnnotations, item => this.visitDefined<J.Annotation>(item, p));
            draft.modifiers = await mapAsync(jSMethodDeclaration.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.typeParameters = jSMethodDeclaration.typeParameters && await this.visitDefined<J.TypeParameters>(jSMethodDeclaration.typeParameters, p);
            draft.returnTypeExpression = jSMethodDeclaration.returnTypeExpression && await this.visitDefined<TypedTree>(jSMethodDeclaration.returnTypeExpression, p);
            draft.name = await this.visitDefined<Expression>(jSMethodDeclaration.name, p);
            draft.parameters = await this.visitContainer(jSMethodDeclaration.parameters, p);
            draft.body = jSMethodDeclaration.body && await this.visitDefined<J.Block>(jSMethodDeclaration.body, p);
            draft.defaultValue = jSMethodDeclaration.defaultValue && await this.visitLeftPadded(jSMethodDeclaration.defaultValue, p);
            draft.methodType = jSMethodDeclaration.methodType && (await this.visitType(jSMethodDeclaration.methodType, p) as JavaType.Method);
        });
    }

    protected async visitJSForOfLoop(jSForOfLoop: JS.JSForOfLoop, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.JSForOfLoop>(jSForOfLoop, p, async draft => {
            draft.await = await this.visitLeftPadded(jSForOfLoop.await, p);
            draft.control = await this.visitDefined<JS.JSForInOfLoopControl>(jSForOfLoop.control, p);
            draft.body = await this.visitRightPadded(jSForOfLoop.body, p);
        });
    }

    protected async visitJSForInLoop(jSForInLoop: JS.JSForInLoop, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.JSForInLoop>(jSForInLoop, p, async draft => {
            draft.control = await this.visitDefined<JS.JSForInOfLoopControl>(jSForInLoop.control, p);
            draft.body = await this.visitRightPadded(jSForInLoop.body, p);
        });
    }

    protected async visitJSForInOfLoopControl(jSForInOfLoopControl: JS.JSForInOfLoopControl, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.JSForInOfLoopControl>(jSForInOfLoopControl, p, async draft => {
            draft.variable = await this.visitRightPadded(jSForInOfLoopControl.variable, p);
            draft.iterable = await this.visitRightPadded(jSForInOfLoopControl.iterable, p);
        });
    }

    protected async visitJSTry(jSTry: JS.JSTry, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.JSTry>(jSTry, p, async draft => {
            draft.body = await this.visitDefined<J.Block>(jSTry.body, p);
            draft.catches = await this.visitDefined<JS.JSTry.JSCatch>(jSTry.catches, p);
            draft.finally = jSTry.finally && await this.visitLeftPadded(jSTry.finally, p);
        });
    }

    protected async visitJSCatch(jSCatch: JS.JSTry.JSCatch, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.JSTry.JSCatch>(jSCatch, p, async draft => {
            draft.parameter = await this.visitDefined<J.ControlParentheses<JS.JSVariableDeclarations>>(jSCatch.parameter, p);
            draft.body = await this.visitDefined<J.Block>(jSCatch.body, p);
        });
    }

    protected async visitNamespaceDeclaration(namespaceDeclaration: JS.NamespaceDeclaration, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.NamespaceDeclaration>(namespaceDeclaration, p, async draft => {
            draft.modifiers = await mapAsync(namespaceDeclaration.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.keywordType = await this.visitLeftPadded(namespaceDeclaration.keywordType, p);
            draft.name = await this.visitRightPadded(namespaceDeclaration.name, p);
            draft.body = namespaceDeclaration.body && await this.visitDefined<J.Block>(namespaceDeclaration.body, p);
        });
    }

    protected async visitFunctionDeclaration(functionDeclaration: JS.FunctionDeclaration, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.FunctionDeclaration>(functionDeclaration, p, async draft => {
            draft.modifiers = await mapAsync(functionDeclaration.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.asteriskToken = await this.visitLeftPadded(functionDeclaration.asteriskToken, p);
            draft.name = await this.visitLeftPadded(functionDeclaration.name, p);
            draft.typeParameters = functionDeclaration.typeParameters && await this.visitDefined<J.TypeParameters>(functionDeclaration.typeParameters, p);
            draft.parameters = await this.visitContainer(functionDeclaration.parameters, p);
            draft.returnTypeExpression = functionDeclaration.returnTypeExpression && await this.visitDefined<TypedTree>(functionDeclaration.returnTypeExpression, p);
            draft.body = functionDeclaration.body && await this.visitDefined<J>(functionDeclaration.body, p);
            draft.type = functionDeclaration.type && await this.visitType(functionDeclaration.type, p);
        });
    }

    protected async visitTypeLiteral(typeLiteral: JS.TypeLiteral, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.TypeLiteral>(typeLiteral, p, async draft => {
            draft.members = await this.visitDefined<J.Block>(typeLiteral.members, p);
            draft.type = typeLiteral.type && await this.visitType(typeLiteral.type, p);
        });
    }

    protected async visitArrayBindingPattern(arrayBindingPattern: JS.ArrayBindingPattern, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.ArrayBindingPattern>(arrayBindingPattern, p, async draft => {
            draft.elements = await this.visitContainer(arrayBindingPattern.elements, p);
            draft.type = arrayBindingPattern.type && await this.visitType(arrayBindingPattern.type, p);
        });
    }

    protected async visitBindingElement(bindingElement: JS.BindingElement, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.BindingElement>(bindingElement, p, async draft => {
            draft.propertyName = bindingElement.propertyName && await this.visitRightPadded(bindingElement.propertyName, p);
            draft.name = await this.visitDefined<TypedTree>(bindingElement.name, p);
            draft.initializer = bindingElement.initializer && await this.visitLeftPadded(bindingElement.initializer, p);
            draft.variableType = bindingElement.variableType && (await this.visitType(bindingElement.variableType, p) as JavaType.Variable);
        });
    }

    protected async visitExportDeclaration(exportDeclaration: JS.ExportDeclaration, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.ExportDeclaration>(exportDeclaration, p, async draft => {
            draft.modifiers = await mapAsync(exportDeclaration.modifiers, item => this.visitDefined<J.Modifier>(item, p));
            draft.typeOnly = await this.visitLeftPadded(exportDeclaration.typeOnly, p);
            draft.exportClause = exportDeclaration.exportClause && await this.visitDefined<Expression>(exportDeclaration.exportClause, p);
            draft.moduleSpecifier = exportDeclaration.moduleSpecifier && await this.visitLeftPadded(exportDeclaration.moduleSpecifier, p);
            draft.attributes = exportDeclaration.attributes && await this.visitDefined<JS.ImportAttributes>(exportDeclaration.attributes, p);
        });
    }

    protected async visitExportAssignment(exportAssignment: JS.ExportAssignment, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.ExportAssignment>(exportAssignment, p, async draft => {
            draft.expression = await this.visitLeftPadded<Expression>(exportAssignment.expression, p);
        });
    }

    protected async visitNamedExports(namedExports: JS.NamedExports, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.NamedExports>(namedExports, p, async draft => {
            draft.elements = await this.visitContainer(namedExports.elements, p);
            draft.type = namedExports.type && await this.visitType(namedExports.type, p);
        });
    }

    protected async visitExportSpecifier(exportSpecifier: JS.ExportSpecifier, p: P): Promise<J | undefined> {
        return this.produceJavaScript<JS.ExportSpecifier>(exportSpecifier, p, async draft => {
            draft.typeOnly = await this.visitLeftPadded(exportSpecifier.typeOnly, p);
            draft.specifier = await this.visitDefined<Expression>(exportSpecifier.specifier, p);
            draft.type = exportSpecifier.type && await this.visitType(exportSpecifier.type, p);
        });
    }

    override async accept<J2 extends J, P2 extends P>(j: J2, p: P2): Promise<J | undefined> {
        if (isJavaScript(j)) {
            const tree = j as JS;
            switch (tree.kind) {
                case JS.Kind.Alias:
                    return this.visitAlias(tree as unknown as JS.Alias, p);
                case JS.Kind.ArrowFunction:
                    return this.visitArrowFunction(tree as unknown as JS.ArrowFunction, p);
                case JS.Kind.Await:
                    return this.visitAwait(tree as unknown as JS.Await, p);
                case JS.Kind.CompilationUnit:
                    return this.visitJsCompilationUnit(tree as unknown as JS.CompilationUnit, p);
                case JS.Kind.ConditionalType:
                    return this.visitConditionalType(tree as unknown as JS.ConditionalType, p);
                case JS.Kind.Delete:
                    return this.visitDelete(tree as unknown as JS.Delete, p);
                case JS.Kind.ExpressionStatement:
                    return this.visitExpressionStatement(tree as unknown as JS.ExpressionStatement, p);
                case JS.Kind.ExpressionWithTypeArguments:
                    return this.visitExpressionWithTypeArguments(tree as unknown as JS.ExpressionWithTypeArguments, p);
                case JS.Kind.FunctionType:
                    return this.visitFunctionType(tree as unknown as JS.FunctionType, p);
                case JS.Kind.InferType:
                    return this.visitInferType(tree as unknown as JS.InferType, p);
                case JS.Kind.ImportType:
                    return this.visitImportType(tree as unknown as JS.ImportType, p);
                case JS.Kind.JsImport:
                    return this.visitJsImport(tree as unknown as JS.JsImport, p);
                case JS.Kind.JsImportClause:
                    return this.visitJsImportClause(tree as unknown as JS.JsImportClause, p);
                case JS.Kind.NamedImports:
                    return this.visitNamedImports(tree as unknown as JS.NamedImports, p);
                case JS.Kind.JsImportSpecifier:
                    return this.visitJsImportSpecifier(tree as unknown as JS.JsImportSpecifier, p);
                case JS.Kind.JSVariableDeclarations:
                    return this.visitJSVariableDeclarations(tree as unknown as JS.JSVariableDeclarations, p);
                case JS.Kind.JSNamedVariable:
                    return this.visitJSNamedVariable(tree as unknown as JS.JSVariableDeclarations.JSNamedVariable, p);
                case JS.Kind.ImportAttributes:
                    return this.visitImportAttributes(tree as unknown as JS.ImportAttributes, p);
                case JS.Kind.ImportTypeAttributes:
                    return this.visitImportTypeAttributes(tree as unknown as JS.ImportTypeAttributes, p);
                case JS.Kind.ImportAttribute:
                    return this.visitImportAttribute(tree as unknown as JS.ImportAttribute, p);
                case JS.Kind.JsBinary:
                    return this.visitJsBinary(tree as unknown as JS.JsBinary, p);
                case JS.Kind.LiteralType:
                    return this.visitLiteralType(tree as unknown as JS.LiteralType, p);
                case JS.Kind.MappedType:
                    return this.visitMappedType(tree as unknown as JS.MappedType, p);
                case JS.Kind.MappedTypeKeysRemapping:
                    return this.visitKeysRemapping(tree as unknown as JS.MappedType.KeysRemapping, p);
                case JS.Kind.MappedTypeMappedTypeParameter:
                    return this.visitMappedTypeParameter(tree as unknown as JS.MappedType.MappedTypeParameter, p);
                case JS.Kind.ObjectBindingDeclarations:
                    return this.visitObjectBindingDeclarations(tree as unknown as JS.ObjectBindingDeclarations, p);
                case JS.Kind.PropertyAssignment:
                    return this.visitPropertyAssignment(tree as unknown as JS.PropertyAssignment, p);
                case JS.Kind.SatisfiesExpression:
                    return this.visitSatisfiesExpression(tree as unknown as JS.SatisfiesExpression, p);
                case JS.Kind.ScopedVariableDeclarations:
                    return this.visitScopedVariableDeclarations(tree as unknown as JS.ScopedVariableDeclarations, p);
                case JS.Kind.StatementExpression:
                    return this.visitStatementExpression(tree as unknown as JS.StatementExpression, p);
                case JS.Kind.TaggedTemplateExpression:
                    return this.visitTaggedTemplateExpression(tree as unknown as JS.TaggedTemplateExpression, p);
                case JS.Kind.TemplateExpression:
                    return this.visitTemplateExpression(tree as unknown as JS.TemplateExpression, p);
                case JS.Kind.TemplateExpressionSpan:
                    return this.visitTemplateExpressionSpan(tree as unknown as JS.TemplateExpression.Span, p);
                case JS.Kind.TrailingTokenStatement:
                    return this.visitTrailingTokenStatement(tree as unknown as JS.TrailingTokenStatement, p);
                case JS.Kind.Tuple:
                    return this.visitTuple(tree as unknown as JS.Tuple, p);
                case JS.Kind.TypeDeclaration:
                    return this.visitTypeDeclaration(tree as unknown as JS.TypeDeclaration, p);
                case JS.Kind.TypeOf:
                    return this.visitTypeOf(tree as unknown as JS.TypeOf, p);
                case JS.Kind.TypeTreeExpression:
                    return this.visitTypeTreeExpression(tree as unknown as JS.TypeTreeExpression, p);
                case JS.Kind.JsAssignmentOperation:
                    return this.visitJsAssignmentOperation(tree as unknown as JS.JsAssignmentOperation, p);
                case JS.Kind.IndexedAccessType:
                    return this.visitIndexedAccessType(tree as unknown as JS.IndexedAccessType, p);
                case JS.Kind.IndexType:
                    return this.visitIndexType(tree as unknown as JS.IndexedAccessType.IndexType, p);
                case JS.Kind.TypeQuery:
                    return this.visitTypeQuery(tree as unknown as JS.TypeQuery, p);
                case JS.Kind.TypeInfo:
                    return this.visitTypeInfo(tree as unknown as JS.TypeInfo, p);
                case JS.Kind.TypeOperator:
                    return this.visitTypeOperator(tree as unknown as JS.TypeOperator, p);
                case JS.Kind.TypePredicate:
                    return this.visitTypePredicate(tree as unknown as JS.TypePredicate, p);
                case JS.Kind.Union:
                    return this.visitUnion(tree as unknown as JS.Union, p);
                case JS.Kind.Intersection:
                    return this.visitIntersection(tree as unknown as JS.Intersection, p);
                case JS.Kind.Void:
                    return this.visitVoid(tree as unknown as JS.Void, p);
                case JS.Kind.Unary:
                    return this.visitJsUnary(tree as unknown as JS.Unary, p);
                case JS.Kind.Yield:
                    return this.visitJsYield(tree as unknown as JS.Yield, p);
                case JS.Kind.WithStatement:
                    return this.visitWithStatement(tree as unknown as JS.WithStatement, p);
                case JS.Kind.IndexSignatureDeclaration:
                    return this.visitIndexSignatureDeclaration(tree as unknown as JS.IndexSignatureDeclaration, p);
                case JS.Kind.JSMethodDeclaration:
                    return this.visitJSMethodDeclaration(tree as unknown as JS.JSMethodDeclaration, p);
                case JS.Kind.JSForOfLoop:
                    return this.visitJSForOfLoop(tree as unknown as JS.JSForOfLoop, p);
                case JS.Kind.JSForInLoop:
                    return this.visitJSForInLoop(tree as unknown as JS.JSForInLoop, p);
                case JS.Kind.JSForInOfLoopControl:
                    return this.visitJSForInOfLoopControl(tree as unknown as JS.JSForInOfLoopControl, p);
                case JS.Kind.JSTry:
                    return this.visitJSTry(tree as unknown as JS.JSTry, p);
                case JS.Kind.JSCatch:
                    return this.visitJSCatch(tree as unknown as JS.JSTry.JSCatch, p);
                case JS.Kind.NamespaceDeclaration:
                    return this.visitNamespaceDeclaration(tree as unknown as JS.NamespaceDeclaration, p);
                case JS.Kind.FunctionDeclaration:
                    return this.visitFunctionDeclaration(tree as unknown as JS.FunctionDeclaration, p);
                case JS.Kind.TypeLiteral:
                    return this.visitTypeLiteral(tree as unknown as JS.TypeLiteral, p);
                case JS.Kind.ArrayBindingPattern:
                    return this.visitArrayBindingPattern(tree as unknown as JS.ArrayBindingPattern, p);
                case JS.Kind.BindingElement:
                    return this.visitBindingElement(tree as unknown as JS.BindingElement, p);
                case JS.Kind.ExportDeclaration:
                    return this.visitExportDeclaration(tree as unknown as JS.ExportDeclaration, p);
                case JS.Kind.ExportAssignment:
                    return this.visitExportAssignment(tree as unknown as JS.ExportAssignment, p);
                case JS.Kind.NamedExports:
                    return this.visitNamedExports(tree as unknown as JS.NamedExports, p);
                case JS.Kind.ExportSpecifier:
                    return this.visitExportSpecifier(tree as unknown as JS.ExportSpecifier, p);
            }
        }
        return super.accept(j, p);
    }
}
