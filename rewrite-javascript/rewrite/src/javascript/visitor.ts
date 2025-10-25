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
import {mapAsync, updateIfChanged} from "../util";
import {SourceFile} from "../tree";
import {ValidImmerRecipeReturnType} from "../visitor";
import {Expression, J, Type, JavaVisitor, NameTree, Statement, TypedTree} from "../java";
import {createDraft, Draft, finishDraft} from "immer";
import {isJavaScript, JS, JSX} from "./tree";
import ComputedPropertyName = JS.ComputedPropertyName;

export class JavaScriptVisitor<P> extends JavaVisitor<P> {

    override async isAcceptable(sourceFile: SourceFile): Promise<boolean> {
        return isJavaScript(sourceFile);
    }

    // noinspection JSUnusedGlobalSymbols,JSUnusedLocalSymbols
    protected override async visitExpression(expression: Expression, p: P): Promise<J | undefined> {
        return expression;
    }

    // noinspection JSUnusedGlobalSymbols,JSUnusedLocalSymbols
    protected override async visitStatement(statement: Statement, p: P): Promise<J | undefined> {
        return statement;
    }

    // noinspection JSUnusedLocalSymbols
    override async visitSpace(space: J.Space, p: P): Promise<J.Space> {
        return space;
    }

    // noinspection JSUnusedLocalSymbols
    protected override async visitType(javaType: Type | undefined, p: P): Promise<Type | undefined> {
        return javaType;
    }

    // noinspection JSUnusedLocalSymbols
    protected override async visitTypeName<N extends NameTree>(nameTree: N, p: P): Promise<N> {
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
        const expression = await this.visitExpression(alias, p);
        if (!expression?.kind || expression.kind !== JS.Kind.Alias) {
            return expression;
        }
        alias = expression as JS.Alias;

        const updates: any = {
            prefix: await this.visitSpace(alias.prefix, p),
            markers: await this.visitMarkers(alias.markers, p),
            propertyName: await this.visitRightPadded(alias.propertyName, p),
            alias: await this.visitDefined<Expression>(alias.alias, p)
        };
        return updateIfChanged(alias, updates);
    }

    protected async visitArrowFunction(arrowFunction: JS.ArrowFunction, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(arrowFunction, p);
        if (!expression?.kind || expression.kind !== JS.Kind.ArrowFunction) {
            return expression;
        }
        arrowFunction = expression as JS.ArrowFunction;

        const statement = await this.visitStatement(arrowFunction, p);
        if (!statement?.kind || statement.kind !== JS.Kind.ArrowFunction) {
            return statement;
        }
        arrowFunction = statement as JS.ArrowFunction;

        const updates: any = {
            prefix: await this.visitSpace(arrowFunction.prefix, p),
            markers: await this.visitMarkers(arrowFunction.markers, p),
            leadingAnnotations: await mapAsync(arrowFunction.leadingAnnotations, item => this.visitDefined<J.Annotation>(item, p)),
            modifiers: await mapAsync(arrowFunction.modifiers, item => this.visitDefined<J.Modifier>(item, p)),
            typeParameters: arrowFunction.typeParameters && await this.visitDefined<J.TypeParameters>(arrowFunction.typeParameters, p),
            lambda: await this.visitDefined<J.Lambda>(arrowFunction.lambda, p),
            returnTypeExpression: arrowFunction.returnTypeExpression && await this.visitDefined<TypedTree>(arrowFunction.returnTypeExpression, p)
        };
        return updateIfChanged(arrowFunction, updates);
    }

    protected async visitAs(as_: JS.As, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(as_.prefix, p),
            markers: await this.visitMarkers(as_.markers, p),
            left: await this.visitRightPadded<Expression>(as_.left, p),
            right: await this.visitDefined<Expression>(as_.right, p),
            type: as_.type && await this.visitType(as_.type, p)
        };
        return updateIfChanged(as_, updates);
    }

    protected async visitAwait(await_: JS.Await, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(await_, p);
        if (!expression?.kind || expression.kind !== JS.Kind.Await) {
            return expression;
        }
        await_ = expression as JS.Await;

        const updates: any = {
            prefix: await this.visitSpace(await_.prefix, p),
            markers: await this.visitMarkers(await_.markers, p),
            expression: await this.visitDefined<Expression>(await_.expression, p),
            type: await_.type && await this.visitType(await_.type, p)
        };
        return updateIfChanged(await_, updates);
    }

    protected async visitJsxTag(element: JSX.Tag, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(element.prefix, p),
            markers: await this.visitMarkers(element.markers, p),
            openName: await this.visitLeftPadded(element.openName, p),
            typeArguments: element.typeArguments && await this.visitContainer(element.typeArguments, p),
            afterName: await this.visitSpace(element.afterName, p),
            attributes: await mapAsync(element.attributes, attr => this.visitRightPadded(attr, p)),
            selfClosing: element.selfClosing && await this.visitSpace(element.selfClosing, p),
            children: element.children && await mapAsync(element.children, child => this.visit(child, p)),
            closingName: element.closingName && await this.visitLeftPadded(element.closingName, p),
            afterClosingName: element.afterClosingName && await this.visitSpace(element.afterClosingName, p)
        };
        return updateIfChanged(element, updates);
    }

    protected async visitJsxAttribute(attribute: JSX.Attribute, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(attribute.prefix, p),
            markers: await this.visitMarkers(attribute.markers, p),
            key: await this.visitDefined<J.Identifier | JSX.NamespacedName>(attribute.key, p),
            value: attribute.value && await this.visitLeftPadded(attribute.value, p)
        };
        return updateIfChanged(attribute, updates);
    }

    protected async visitJsxSpreadAttribute(spread: JSX.SpreadAttribute, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(spread.prefix, p),
            markers: await this.visitMarkers(spread.markers, p),
            dots: await this.visitSpace(spread.dots, p),
            expression: await this.visitRightPadded(spread.expression, p)
        };
        return updateIfChanged(spread, updates);
    }

    protected async visitJsxEmbeddedExpression(expr: JSX.EmbeddedExpression, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(expr.prefix, p),
            markers: await this.visitMarkers(expr.markers, p),
            expression: await this.visitRightPadded(expr.expression, p)
        };
        return updateIfChanged(expr, updates);
    }

    protected async visitJsxNamespacedName(ns: JSX.NamespacedName, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(ns.prefix, p),
            markers: await this.visitMarkers(ns.markers, p),
            namespace: await this.visitDefined<J.Identifier>(ns.namespace, p),
            name: await this.visitLeftPadded(ns.name, p)
        };
        return updateIfChanged(ns, updates);
    }

    protected async visitJsCompilationUnit(compilationUnit: JS.CompilationUnit, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(compilationUnit.prefix, p),
            markers: await this.visitMarkers(compilationUnit.markers, p),
            statements: await mapAsync(compilationUnit.statements, stmt => this.visitRightPadded(stmt, p)),
            eof: await this.visitSpace(compilationUnit.eof, p)
        };
        return updateIfChanged(compilationUnit, updates);
    }

    protected async visitConditionalType(conditionalType: JS.ConditionalType, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(conditionalType, p);
        if (!expression?.kind || expression.kind !== JS.Kind.ConditionalType) {
            return expression;
        }
        conditionalType = expression as JS.ConditionalType;

        const updates: any = {
            prefix: await this.visitSpace(conditionalType.prefix, p),
            markers: await this.visitMarkers(conditionalType.markers, p),
            checkType: await this.visitDefined<Expression>(conditionalType.checkType, p),
            condition: await this.visitLeftPadded(conditionalType.condition, p),
            type: conditionalType.type && await this.visitType(conditionalType.type, p)
        };
        return updateIfChanged(conditionalType, updates);
    }

    protected async visitDelete(delete_: JS.Delete, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(delete_, p);
        if (!expression?.kind || expression.kind !== JS.Kind.Delete) {
            return expression;
        }
        delete_ = expression as JS.Delete;

        const statement = await this.visitStatement(delete_, p);
        if (!statement?.kind || statement.kind !== JS.Kind.Delete) {
            return statement;
        }
        delete_ = statement as JS.Delete;

        const updates: any = {
            prefix: await this.visitSpace(delete_.prefix, p),
            markers: await this.visitMarkers(delete_.markers, p),
            expression: await this.visitDefined<Expression>(delete_.expression, p)
        };
        return updateIfChanged(delete_, updates);
    }

    protected async visitExpressionStatement(expressionStatement: JS.ExpressionStatement, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(expressionStatement, p);
        if (!expression?.kind || expression.kind !== JS.Kind.ExpressionStatement) {
            return expression;
        }
        expressionStatement = expression as JS.ExpressionStatement;

        const statement = await this.visitStatement(expressionStatement, p);
        if (!statement?.kind || statement.kind !== JS.Kind.ExpressionStatement) {
            return statement;
        }
        expressionStatement = statement as JS.ExpressionStatement;

        const updates: any = {
            prefix: await this.visitSpace(expressionStatement.prefix, p),
            markers: await this.visitMarkers(expressionStatement.markers, p),
            expression: await this.visitDefined<Expression>(expressionStatement.expression, p)
        };
        return updateIfChanged(expressionStatement, updates);
    }

    protected async visitExpressionWithTypeArguments(expressionWithTypeArguments: JS.ExpressionWithTypeArguments, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(expressionWithTypeArguments, p);
        if (!expression?.kind || expression.kind !== JS.Kind.ExpressionWithTypeArguments) {
            return expression;
        }
        expressionWithTypeArguments = expression as JS.ExpressionWithTypeArguments;

        const updates: any = {
            prefix: await this.visitSpace(expressionWithTypeArguments.prefix, p),
            markers: await this.visitMarkers(expressionWithTypeArguments.markers, p),
            clazz: await this.visitDefined<J>(expressionWithTypeArguments.clazz, p),
            typeArguments: expressionWithTypeArguments.typeArguments && await this.visitContainer(expressionWithTypeArguments.typeArguments, p),
            type: expressionWithTypeArguments.type && await this.visitType(expressionWithTypeArguments.type, p)
        };
        return updateIfChanged(expressionWithTypeArguments, updates);
    }

    protected async visitFunctionCall(functionCall: JS.FunctionCall, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(functionCall, p);
        if (!expression?.kind || expression.kind !== JS.Kind.FunctionCall) {
            return expression;
        }
        functionCall = expression as JS.FunctionCall;

        const statement = await this.visitStatement(functionCall, p);
        if (!statement?.kind || statement.kind !== JS.Kind.FunctionCall) {
            return statement;
        }
        functionCall = statement as JS.FunctionCall;

        return this.produceJava<JS.FunctionCall>(functionCall, p, async draft => {
            draft.function = await this.visitOptionalRightPadded(functionCall.function, p);
            draft.typeParameters = await this.visitOptionalContainer(functionCall.typeParameters, p);
            draft.arguments = await this.visitContainer(functionCall.arguments, p);
            draft.methodType = await this.visitType(functionCall.methodType, p) as Type.Method | undefined;
        });
    }

    protected async visitFunctionType(functionType: JS.FunctionType, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(functionType, p);
        if (!expression?.kind || expression.kind !== JS.Kind.FunctionType) {
            return expression;
        }
        functionType = expression as JS.FunctionType;

        const updates: any = {
            prefix: await this.visitSpace(functionType.prefix, p),
            markers: await this.visitMarkers(functionType.markers, p),
            constructorType: await this.visitLeftPadded(functionType.constructorType, p),
            typeParameters: functionType.typeParameters && await this.visitDefined<J.TypeParameters>(functionType.typeParameters, p),
            parameters: await this.visitContainer(functionType.parameters, p),
            returnType: await this.visitLeftPadded(functionType.returnType, p)
        };
        return updateIfChanged(functionType, updates);
    }

    protected async visitInferType(inferType: JS.InferType, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(inferType, p);
        if (!expression?.kind || expression.kind !== JS.Kind.InferType) {
            return expression;
        }
        inferType = expression as JS.InferType;

        const updates: any = {
            prefix: await this.visitSpace(inferType.prefix, p),
            markers: await this.visitMarkers(inferType.markers, p),
            typeParameter: await this.visitLeftPadded(inferType.typeParameter, p),
            type: inferType.type && await this.visitType(inferType.type, p)
        };
        return updateIfChanged(inferType, updates);
    }

    protected async visitImportType(importType: JS.ImportType, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(importType, p);
        if (!expression?.kind || expression.kind !== JS.Kind.ImportType) {
            return expression;
        }
        importType = expression as JS.ImportType;

        const updates: any = {
            prefix: await this.visitSpace(importType.prefix, p),
            markers: await this.visitMarkers(importType.markers, p),
            hasTypeof: await this.visitRightPadded(importType.hasTypeof, p),
            argumentAndAttributes: await this.visitContainer(importType.argumentAndAttributes, p),
            qualifier: importType.qualifier && await this.visitLeftPadded(importType.qualifier, p),
            typeArguments: importType.typeArguments && await this.visitContainer(importType.typeArguments, p),
            type: importType.type && await this.visitType(importType.type, p)
        };
        return updateIfChanged(importType, updates);
    }

    protected async visitImportDeclaration(jsImport: JS.Import, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(jsImport, p);
        if (!statement?.kind || statement.kind !== JS.Kind.Import) {
            return statement;
        }
        jsImport = statement as JS.Import;

        const updates: any = {
            prefix: await this.visitSpace(jsImport.prefix, p),
            markers: await this.visitMarkers(jsImport.markers, p),
            modifiers: await mapAsync(jsImport.modifiers, item => this.visitDefined<J.Modifier>(item, p)),
            importClause: jsImport.importClause && await this.visitDefined<JS.ImportClause>(jsImport.importClause, p),
            moduleSpecifier: jsImport.moduleSpecifier && await this.visitLeftPadded(jsImport.moduleSpecifier, p),
            attributes: jsImport.attributes && await this.visitDefined<JS.ImportAttributes>(jsImport.attributes, p),
            initializer: jsImport.initializer && await this.visitLeftPadded(jsImport.initializer, p)
        };
        return updateIfChanged(jsImport, updates);
    }

    protected async visitImportClause(importClause: JS.ImportClause, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(importClause.prefix, p),
            markers: await this.visitMarkers(importClause.markers, p),
            name: importClause.name && await this.visitRightPadded(importClause.name, p),
            namedBindings: importClause.namedBindings && await this.visitDefined<Expression>(importClause.namedBindings, p)
        };
        return updateIfChanged(importClause, updates);
    }

    protected async visitNamedImports(namedImports: JS.NamedImports, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(namedImports, p);
        if (!expression?.kind || expression.kind !== JS.Kind.NamedImports) {
            return expression;
        }
        namedImports = expression as JS.NamedImports;

        const updates: any = {
            prefix: await this.visitSpace(namedImports.prefix, p),
            markers: await this.visitMarkers(namedImports.markers, p),
            elements: await this.visitContainer(namedImports.elements, p),
            type: namedImports.type && await this.visitType(namedImports.type, p)
        };
        return updateIfChanged(namedImports, updates);
    }

    protected async visitImportSpecifier(importSpecifier: JS.ImportSpecifier, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(importSpecifier, p);
        if (!expression?.kind || expression.kind !== JS.Kind.ImportSpecifier) {
            return expression;
        }
        importSpecifier = expression as JS.ImportSpecifier;

        const updates: any = {
            prefix: await this.visitSpace(importSpecifier.prefix, p),
            markers: await this.visitMarkers(importSpecifier.markers, p),
            importType: await this.visitLeftPadded(importSpecifier.importType, p),
            specifier: await this.visitDefined<JS.Alias | J.Identifier>(importSpecifier.specifier, p),
            type: importSpecifier.type && await this.visitType(importSpecifier.type, p)
        };
        return updateIfChanged(importSpecifier, updates);
    }

    protected async visitImportAttributes(importAttributes: JS.ImportAttributes, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(importAttributes.prefix, p),
            markers: await this.visitMarkers(importAttributes.markers, p),
            elements: await this.visitContainer(importAttributes.elements, p)
        };
        return updateIfChanged(importAttributes, updates);
    }

    protected async visitImportTypeAttributes(importTypeAttributes: JS.ImportTypeAttributes, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(importTypeAttributes.prefix, p),
            markers: await this.visitMarkers(importTypeAttributes.markers, p),
            token: await this.visitRightPadded(importTypeAttributes.token, p),
            elements: await this.visitContainer(importTypeAttributes.elements, p),
            end: await this.visitSpace(importTypeAttributes.end, p)
        };
        return updateIfChanged(importTypeAttributes, updates);
    }

    protected async visitImportAttribute(importAttribute: JS.ImportAttribute, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(importAttribute, p);
        if (!statement?.kind || statement.kind !== JS.Kind.ImportAttribute) {
            return statement;
        }
        importAttribute = statement as JS.ImportAttribute;

        const updates: any = {
            prefix: await this.visitSpace(importAttribute.prefix, p),
            markers: await this.visitMarkers(importAttribute.markers, p),
            name: await this.visitDefined<Expression>(importAttribute.name, p),
            value: await this.visitLeftPadded(importAttribute.value, p)
        };
        return updateIfChanged(importAttribute, updates);
    }

    protected async visitBinaryExtensions(jsBinary: JS.Binary, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(jsBinary.prefix, p),
            markers: await this.visitMarkers(jsBinary.markers, p),
            left: await this.visitDefined<Expression>(jsBinary.left, p),
            operator: await this.visitLeftPadded(jsBinary.operator, p),
            right: await this.visitDefined<Expression>(jsBinary.right, p),
            type: jsBinary.type && await this.visitType(jsBinary.type, p)
        };
        return updateIfChanged(jsBinary, updates);
    }

    protected async visitLiteralType(literalType: JS.LiteralType, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(literalType, p);
        if (!expression?.kind || expression.kind !== JS.Kind.LiteralType) {
            return expression;
        }
        literalType = expression as JS.LiteralType;

        const updates: any = {
            prefix: await this.visitSpace(literalType.prefix, p),
            markers: await this.visitMarkers(literalType.markers, p),
            literal: await this.visitDefined<Expression>(literalType.literal, p),
            type: (await this.visitType(literalType.type, p))!
        };
        return updateIfChanged(literalType, updates);
    }

    protected async visitMappedType(mappedType: JS.MappedType, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(mappedType, p);
        if (!expression?.kind || expression.kind !== JS.Kind.MappedType) {
            return expression;
        }
        mappedType = expression as JS.MappedType;

        const updates: any = {
            prefix: await this.visitSpace(mappedType.prefix, p),
            markers: await this.visitMarkers(mappedType.markers, p),
            prefixToken: mappedType.prefixToken && await this.visitLeftPadded(mappedType.prefixToken, p),
            hasReadonly: await this.visitLeftPadded(mappedType.hasReadonly, p),
            keysRemapping: await this.visitDefined<JS.MappedType.KeysRemapping>(mappedType.keysRemapping, p),
            suffixToken: mappedType.suffixToken && await this.visitLeftPadded(mappedType.suffixToken, p),
            hasQuestionToken: await this.visitLeftPadded(mappedType.hasQuestionToken, p),
            valueType: await this.visitContainer(mappedType.valueType, p),
            type: mappedType.type && await this.visitType(mappedType.type, p)
        };
        return updateIfChanged(mappedType, updates);
    }

    protected async visitMappedTypeKeysRemapping(keysRemapping: JS.MappedType.KeysRemapping, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(keysRemapping.prefix, p),
            markers: await this.visitMarkers(keysRemapping.markers, p),
            typeParameter: await this.visitRightPadded(keysRemapping.typeParameter, p),
            nameType: keysRemapping.nameType && await this.visitRightPadded(keysRemapping.nameType, p)
        };
        return updateIfChanged(keysRemapping, updates);
    }

    protected async visitMappedTypeParameter(mappedTypeParameter: JS.MappedType.Parameter, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(mappedTypeParameter.prefix, p),
            markers: await this.visitMarkers(mappedTypeParameter.markers, p),
            name: await this.visitDefined<Expression>(mappedTypeParameter.name, p),
            iterateType: await this.visitLeftPadded(mappedTypeParameter.iterateType, p)
        };
        return updateIfChanged(mappedTypeParameter, updates);
    }

    protected async visitObjectBindingPattern(objectBindingPattern: JS.ObjectBindingPattern, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(objectBindingPattern, p);
        if (!expression?.kind || expression.kind !== JS.Kind.ObjectBindingPattern) {
            return expression;
        }
        objectBindingPattern = expression as JS.ObjectBindingPattern;

        const updates: any = {
            prefix: await this.visitSpace(objectBindingPattern.prefix, p),
            markers: await this.visitMarkers(objectBindingPattern.markers, p),
            leadingAnnotations: await mapAsync(objectBindingPattern.leadingAnnotations, item => this.visitDefined<J.Annotation>(item, p)),
            modifiers: await mapAsync(objectBindingPattern.modifiers, item => this.visitDefined<J.Modifier>(item, p)),
            typeExpression: objectBindingPattern.typeExpression && await this.visitDefined<TypedTree>(objectBindingPattern.typeExpression, p),
            bindings: await this.visitContainer(objectBindingPattern.bindings, p),
            initializer: objectBindingPattern.initializer && await this.visitLeftPadded(objectBindingPattern.initializer, p)
        };
        return updateIfChanged(objectBindingPattern, updates);
    }

    protected async visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(propertyAssignment, p);
        if (!statement?.kind || statement.kind !== JS.Kind.PropertyAssignment) {
            return statement;
        }
        propertyAssignment = statement as JS.PropertyAssignment;

        const updates: any = {
            prefix: await this.visitSpace(propertyAssignment.prefix, p),
            markers: await this.visitMarkers(propertyAssignment.markers, p),
            name: await this.visitRightPadded(propertyAssignment.name, p),
            initializer: propertyAssignment.initializer && await this.visitDefined<Expression>(propertyAssignment.initializer, p)
        };
        return updateIfChanged(propertyAssignment, updates);
    }

    protected async visitSatisfiesExpression(satisfiesExpression: JS.SatisfiesExpression, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(satisfiesExpression, p);
        if (!expression?.kind || expression.kind !== JS.Kind.SatisfiesExpression) {
            return expression;
        }
        satisfiesExpression = expression as JS.SatisfiesExpression;

        const updates: any = {
            prefix: await this.visitSpace(satisfiesExpression.prefix, p),
            markers: await this.visitMarkers(satisfiesExpression.markers, p),
            expression: await this.visitDefined<J>(satisfiesExpression.expression, p),
            satisfiesType: await this.visitLeftPadded(satisfiesExpression.satisfiesType, p),
            type: satisfiesExpression.type && await this.visitType(satisfiesExpression.type, p)
        };
        return updateIfChanged(satisfiesExpression, updates);
    }

    protected async visitScopedVariableDeclarations(scopedVariableDeclarations: JS.ScopedVariableDeclarations, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(scopedVariableDeclarations, p);
        if (!statement?.kind || statement.kind !== JS.Kind.ScopedVariableDeclarations) {
            return statement;
        }
        scopedVariableDeclarations = statement as JS.ScopedVariableDeclarations;

        const updates: any = {
            prefix: await this.visitSpace(scopedVariableDeclarations.prefix, p),
            markers: await this.visitMarkers(scopedVariableDeclarations.markers, p),
            modifiers: await mapAsync(scopedVariableDeclarations.modifiers, item => this.visitDefined<J.Modifier>(item, p)),
            variables: await mapAsync(scopedVariableDeclarations.variables, item => this.visitRightPadded(item, p))
        };
        return updateIfChanged(scopedVariableDeclarations, updates);
    }

    protected async visitStatementExpression(statementExpression: JS.StatementExpression, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(statementExpression, p);
        if (!expression?.kind || expression.kind !== JS.Kind.StatementExpression) {
            return expression;
        }
        statementExpression = expression as JS.StatementExpression;

        const statement = await this.visitStatement(statementExpression, p);
        if (!statement?.kind || statement.kind !== JS.Kind.StatementExpression) {
            return statement;
        }
        statementExpression = statement as JS.StatementExpression;

        const updates: any = {
            prefix: await this.visitSpace(statementExpression.prefix, p),
            markers: await this.visitMarkers(statementExpression.markers, p),
            statement: await this.visitDefined<Statement>(statementExpression.statement, p)
        };
        return updateIfChanged(statementExpression, updates);
    }

    protected async visitTaggedTemplateExpression(taggedTemplateExpression: JS.TaggedTemplateExpression, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(taggedTemplateExpression, p);
        if (!expression?.kind || expression.kind !== JS.Kind.TaggedTemplateExpression) {
            return expression;
        }
        taggedTemplateExpression = expression as JS.TaggedTemplateExpression;

        const statement = await this.visitStatement(taggedTemplateExpression, p);
        if (!statement?.kind || statement.kind !== JS.Kind.TaggedTemplateExpression) {
            return statement;
        }
        taggedTemplateExpression = statement as JS.TaggedTemplateExpression;

        const updates: any = {
            prefix: await this.visitSpace(taggedTemplateExpression.prefix, p),
            markers: await this.visitMarkers(taggedTemplateExpression.markers, p),
            tag: taggedTemplateExpression.tag && await this.visitRightPadded(taggedTemplateExpression.tag, p),
            typeArguments: taggedTemplateExpression.typeArguments && await this.visitContainer(taggedTemplateExpression.typeArguments, p),
            templateExpression: await this.visitDefined<Expression>(taggedTemplateExpression.templateExpression, p),
            type: taggedTemplateExpression.type && await this.visitType(taggedTemplateExpression.type, p)
        };
        return updateIfChanged(taggedTemplateExpression, updates);
    }

    protected async visitTemplateExpression(templateExpression: JS.TemplateExpression, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(templateExpression, p);
        if (!expression?.kind || expression.kind !== JS.Kind.TemplateExpression) {
            return expression;
        }
        templateExpression = expression as JS.TemplateExpression;

        const statement = await this.visitStatement(templateExpression, p);
        if (!statement?.kind || statement.kind !== JS.Kind.TemplateExpression) {
            return statement;
        }
        templateExpression = statement as JS.TemplateExpression;

        const updates: any = {
            prefix: await this.visitSpace(templateExpression.prefix, p),
            markers: await this.visitMarkers(templateExpression.markers, p),
            head: await this.visitDefined<J.Literal>(templateExpression.head, p),
            spans: await mapAsync(templateExpression.spans, item => this.visitRightPadded(item, p)),
            type: templateExpression.type && await this.visitType(templateExpression.type, p)
        };
        return updateIfChanged(templateExpression, updates);
    }

    protected async visitTemplateExpressionSpan(span: JS.TemplateExpression.Span, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(span.prefix, p),
            markers: await this.visitMarkers(span.markers, p),
            expression: await this.visitDefined<J>(span.expression, p),
            tail: await this.visitDefined<J.Literal>(span.tail, p)
        };
        return updateIfChanged(span, updates);
    }

    protected async visitTuple(tuple: JS.Tuple, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(tuple, p);
        if (!expression?.kind || expression.kind !== JS.Kind.Tuple) {
            return expression;
        }
        tuple = expression as JS.Tuple;

        const updates: any = {
            prefix: await this.visitSpace(tuple.prefix, p),
            markers: await this.visitMarkers(tuple.markers, p),
            elements: await this.visitContainer(tuple.elements, p),
            type: tuple.type && await this.visitType(tuple.type, p)
        };
        return updateIfChanged(tuple, updates);
    }

    protected async visitTypeDeclaration(typeDeclaration: JS.TypeDeclaration, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(typeDeclaration, p);
        if (!statement?.kind || statement.kind !== JS.Kind.TypeDeclaration) {
            return statement;
        }
        typeDeclaration = statement as JS.TypeDeclaration;

        const updates: any = {
            prefix: await this.visitSpace(typeDeclaration.prefix, p),
            markers: await this.visitMarkers(typeDeclaration.markers, p),
            modifiers: await mapAsync(typeDeclaration.modifiers, item => this.visitDefined<J.Modifier>(item, p)),
            name: await this.visitLeftPadded(typeDeclaration.name, p),
            typeParameters: typeDeclaration.typeParameters && await this.visitDefined<J.TypeParameters>(typeDeclaration.typeParameters, p),
            initializer: await this.visitLeftPadded(typeDeclaration.initializer, p),
            type: typeDeclaration.type && await this.visitType(typeDeclaration.type, p)
        };
        return updateIfChanged(typeDeclaration, updates);
    }

    protected async visitTypeOf(typeOf: JS.TypeOf, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(typeOf, p);
        if (!expression?.kind || expression.kind !== JS.Kind.TypeOf) {
            return expression;
        }
        typeOf = expression as JS.TypeOf;

        const updates: any = {
            prefix: await this.visitSpace(typeOf.prefix, p),
            markers: await this.visitMarkers(typeOf.markers, p),
            expression: await this.visitDefined<Expression>(typeOf.expression, p),
            type: typeOf.type && await this.visitType(typeOf.type, p)
        };
        return updateIfChanged(typeOf, updates);
    }

    protected async visitTypeTreeExpression(typeTreeExpression: JS.TypeTreeExpression, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(typeTreeExpression, p);
        if (!expression?.kind || expression.kind !== JS.Kind.TypeTreeExpression) {
            return expression;
        }
        typeTreeExpression = expression as JS.TypeTreeExpression;

        const updates: any = {
            prefix: await this.visitSpace(typeTreeExpression.prefix, p),
            markers: await this.visitMarkers(typeTreeExpression.markers, p),
            expression: await this.visitDefined<Expression>(typeTreeExpression.expression, p)
        };
        return updateIfChanged(typeTreeExpression, updates);
    }

    protected async visitAssignmentOperationExtensions(assignmentOperation: JS.AssignmentOperation, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(assignmentOperation.prefix, p),
            markers: await this.visitMarkers(assignmentOperation.markers, p),
            variable: await this.visitDefined<Expression>(assignmentOperation.variable, p),
            operator: await this.visitLeftPadded(assignmentOperation.operator, p),
            assignment: await this.visitDefined<Expression>(assignmentOperation.assignment, p),
            type: assignmentOperation.type && await this.visitType(assignmentOperation.type, p)
        };
        return updateIfChanged(assignmentOperation, updates);
    }

    protected async visitIndexedAccessType(indexedAccessType: JS.IndexedAccessType, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(indexedAccessType, p);
        if (!expression?.kind || expression.kind !== JS.Kind.IndexedAccessType) {
            return expression;
        }
        indexedAccessType = expression as JS.IndexedAccessType;

        const updates: any = {
            prefix: await this.visitSpace(indexedAccessType.prefix, p),
            markers: await this.visitMarkers(indexedAccessType.markers, p),
            objectType: await this.visitDefined<TypedTree>(indexedAccessType.objectType, p),
            indexType: await this.visitDefined<TypedTree>(indexedAccessType.indexType, p),
            type: indexedAccessType.type && await this.visitType(indexedAccessType.type, p)
        };
        return updateIfChanged(indexedAccessType, updates);
    }

    protected async visitIndexedAccessTypeIndexType(indexType: JS.IndexedAccessType.IndexType, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(indexType.prefix, p),
            markers: await this.visitMarkers(indexType.markers, p),
            element: await this.visitRightPadded(indexType.element, p),
            type: indexType.type && await this.visitType(indexType.type, p)
        };
        return updateIfChanged(indexType, updates);
    }

    protected async visitTypeQuery(typeQuery: JS.TypeQuery, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(typeQuery, p);
        if (!expression?.kind || expression.kind !== JS.Kind.TypeQuery) {
            return expression;
        }
        typeQuery = expression as JS.TypeQuery;

        const updates: any = {
            prefix: await this.visitSpace(typeQuery.prefix, p),
            markers: await this.visitMarkers(typeQuery.markers, p),
            typeExpression: await this.visitDefined<TypedTree>(typeQuery.typeExpression, p),
            typeArguments: typeQuery.typeArguments && await this.visitContainer(typeQuery.typeArguments, p),
            type: typeQuery.type && await this.visitType(typeQuery.type, p)
        };
        return updateIfChanged(typeQuery, updates);
    }

    protected async visitTypeInfo(typeInfo: JS.TypeInfo, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(typeInfo, p);
        if (!expression?.kind || expression.kind !== JS.Kind.TypeInfo) {
            return expression;
        }
        typeInfo = expression as JS.TypeInfo;

        const updates: any = {
            prefix: await this.visitSpace(typeInfo.prefix, p),
            markers: await this.visitMarkers(typeInfo.markers, p),
            typeIdentifier: await this.visitDefined<TypedTree>(typeInfo.typeIdentifier, p)
        };
        return updateIfChanged(typeInfo, updates);
    }

    protected async visitComputedPropertyName(computedPropertyName: JS.ComputedPropertyName, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(computedPropertyName, p);
        if (!expression?.kind || expression.kind !== JS.Kind.ComputedPropertyName) {
            return expression;
        }
        computedPropertyName = expression as JS.ComputedPropertyName;

        const updates: any = {
            prefix: await this.visitSpace(computedPropertyName.prefix, p),
            markers: await this.visitMarkers(computedPropertyName.markers, p),
            expression: await this.visitRightPadded(computedPropertyName.expression, p)
        };
        return updateIfChanged(computedPropertyName, updates);
    }

    protected async visitTypeOperator(typeOperator: JS.TypeOperator, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(typeOperator, p);
        if (!expression?.kind || expression.kind !== JS.Kind.TypeOperator) {
            return expression;
        }
        typeOperator = expression as JS.TypeOperator;

        const updates: any = {
            prefix: await this.visitSpace(typeOperator.prefix, p),
            markers: await this.visitMarkers(typeOperator.markers, p),
            expression: await this.visitLeftPadded(typeOperator.expression, p)
        };
        return updateIfChanged(typeOperator, updates);
    }

    protected async visitTypePredicate(typePredicate: JS.TypePredicate, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(typePredicate, p);
        if (!expression?.kind || expression.kind !== JS.Kind.TypePredicate) {
            return expression;
        }
        typePredicate = expression as JS.TypePredicate;

        const updates: any = {
            prefix: await this.visitSpace(typePredicate.prefix, p),
            markers: await this.visitMarkers(typePredicate.markers, p),
            asserts: await this.visitLeftPadded(typePredicate.asserts, p),
            parameterName: await this.visitDefined<J.Identifier>(typePredicate.parameterName, p),
            expression: typePredicate.expression && await this.visitLeftPadded(typePredicate.expression, p),
            type: typePredicate.type && await this.visitType(typePredicate.type, p)
        };
        return updateIfChanged(typePredicate, updates);
    }

    protected async visitUnion(union: JS.Union, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(union, p);
        if (!expression?.kind || expression.kind !== JS.Kind.Union) {
            return expression;
        }
        union = expression as JS.Union;

        const updates: any = {
            prefix: await this.visitSpace(union.prefix, p),
            markers: await this.visitMarkers(union.markers, p),
            types: await mapAsync(union.types, item => this.visitRightPadded(item, p)),
            type: union.type && await this.visitType(union.type, p)
        };
        return updateIfChanged(union, updates);
    }

    protected async visitIntersection(intersection: JS.Intersection, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(intersection, p);
        if (!expression?.kind || expression.kind !== JS.Kind.Intersection) {
            return expression;
        }
        intersection = expression as JS.Intersection;

        const updates: any = {
            prefix: await this.visitSpace(intersection.prefix, p),
            markers: await this.visitMarkers(intersection.markers, p),
            types: await mapAsync(intersection.types, item => this.visitRightPadded(item, p)),
            type: intersection.type && await this.visitType(intersection.type, p)
        };
        return updateIfChanged(intersection, updates);
    }

    protected async visitVoid(void_: JS.Void, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(void_, p);
        if (!expression?.kind || expression.kind !== JS.Kind.Void) {
            return expression;
        }
        void_ = expression as JS.Void;

        const updates: any = {
            prefix: await this.visitSpace(void_.prefix, p),
            markers: await this.visitMarkers(void_.markers, p),
            expression: await this.visitDefined<Expression>(void_.expression, p)
        };
        return updateIfChanged(void_, updates);
    }

    protected async visitWithStatement(withStatement: JS.WithStatement, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(withStatement, p);
        if (!statement?.kind || statement.kind !== JS.Kind.WithStatement) {
            return statement;
        }
        withStatement = statement as JS.WithStatement;

        const updates: any = {
            prefix: await this.visitSpace(withStatement.prefix, p),
            markers: await this.visitMarkers(withStatement.markers, p),
            expression: await this.visitDefined<J.ControlParentheses<Expression>>(withStatement.expression, p),
            body: await this.visitRightPadded(withStatement.body, p)
        };
        return updateIfChanged(withStatement, updates);
    }

    protected async visitIndexSignatureDeclaration(indexSignatureDeclaration: JS.IndexSignatureDeclaration, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(indexSignatureDeclaration, p);
        if (!statement?.kind || statement.kind !== JS.Kind.IndexSignatureDeclaration) {
            return statement;
        }
        indexSignatureDeclaration = statement as JS.IndexSignatureDeclaration;

        const updates: any = {
            prefix: await this.visitSpace(indexSignatureDeclaration.prefix, p),
            markers: await this.visitMarkers(indexSignatureDeclaration.markers, p),
            modifiers: await mapAsync(indexSignatureDeclaration.modifiers, item => this.visitDefined<J.Modifier>(item, p)),
            parameters: await this.visitContainer(indexSignatureDeclaration.parameters, p),
            typeExpression: await this.visitLeftPadded(indexSignatureDeclaration.typeExpression, p),
            type: indexSignatureDeclaration.type && await this.visitType(indexSignatureDeclaration.type, p)
        };
        return updateIfChanged(indexSignatureDeclaration, updates);
    }

    protected async visitComputedPropertyMethodDeclaration(computedPropMethod: JS.ComputedPropertyMethodDeclaration, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(computedPropMethod, p);
        if (!statement?.kind || statement.kind !== JS.Kind.ComputedPropertyMethodDeclaration) {
            return statement;
        }
        computedPropMethod = statement as JS.ComputedPropertyMethodDeclaration;

        const updates: any = {
            prefix: await this.visitSpace(computedPropMethod.prefix, p),
            markers: await this.visitMarkers(computedPropMethod.markers, p),
            leadingAnnotations: await mapAsync(computedPropMethod.leadingAnnotations, item => this.visitDefined<J.Annotation>(item, p)),
            modifiers: await mapAsync(computedPropMethod.modifiers, item => this.visitDefined<J.Modifier>(item, p)),
            typeParameters: computedPropMethod.typeParameters && await this.visitDefined<J.TypeParameters>(computedPropMethod.typeParameters, p),
            returnTypeExpression: computedPropMethod.returnTypeExpression && await this.visitDefined<TypedTree>(computedPropMethod.returnTypeExpression, p),
            name: await this.visitDefined<ComputedPropertyName>(computedPropMethod.name, p),
            parameters: await this.visitContainer(computedPropMethod.parameters, p),
            body: computedPropMethod.body && await this.visitDefined<J.Block>(computedPropMethod.body, p),
            methodType: computedPropMethod.methodType && (await this.visitType(computedPropMethod.methodType, p) as Type.Method)
        };
        return updateIfChanged(computedPropMethod, updates);
    }

    protected async visitForOfLoop(forOfLoop: JS.ForOfLoop, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(forOfLoop.prefix, p),
            markers: await this.visitMarkers(forOfLoop.markers, p),
            loop: await this.visitDefined<J.ForEachLoop>(forOfLoop.loop, p)
        };
        return updateIfChanged(forOfLoop, updates);
    }

    protected async visitForInLoop(forInLoop: JS.ForInLoop, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(forInLoop.prefix, p),
            markers: await this.visitMarkers(forInLoop.markers, p),
            control: await this.visitDefined<JS.ForInLoop.Control>(forInLoop.control, p),
            body: await this.visitRightPadded(forInLoop.body, p)
        };
        return updateIfChanged(forInLoop, updates);
    }

    protected async visitNamespaceDeclaration(namespaceDeclaration: JS.NamespaceDeclaration, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(namespaceDeclaration, p);
        if (!statement?.kind || statement.kind !== JS.Kind.NamespaceDeclaration) {
            return statement;
        }
        namespaceDeclaration = statement as JS.NamespaceDeclaration;

        const updates: any = {
            prefix: await this.visitSpace(namespaceDeclaration.prefix, p),
            markers: await this.visitMarkers(namespaceDeclaration.markers, p),
            modifiers: await mapAsync(namespaceDeclaration.modifiers, item => this.visitDefined<J.Modifier>(item, p)),
            keywordType: await this.visitLeftPadded(namespaceDeclaration.keywordType, p),
            name: await this.visitRightPadded(namespaceDeclaration.name, p),
            body: namespaceDeclaration.body && await this.visitDefined<J.Block>(namespaceDeclaration.body, p)
        };
        return updateIfChanged(namespaceDeclaration, updates);
    }

    protected async visitTypeLiteral(typeLiteral: JS.TypeLiteral, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(typeLiteral.prefix, p),
            markers: await this.visitMarkers(typeLiteral.markers, p),
            members: await this.visitDefined<J.Block>(typeLiteral.members, p),
            type: typeLiteral.type && await this.visitType(typeLiteral.type, p)
        };
        return updateIfChanged(typeLiteral, updates);
    }

    protected async visitArrayBindingPattern(arrayBindingPattern: JS.ArrayBindingPattern, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(arrayBindingPattern.prefix, p),
            markers: await this.visitMarkers(arrayBindingPattern.markers, p),
            elements: await this.visitContainer(arrayBindingPattern.elements, p),
            type: arrayBindingPattern.type && await this.visitType(arrayBindingPattern.type, p)
        };
        return updateIfChanged(arrayBindingPattern, updates);
    }

    protected async visitBindingElement(bindingElement: JS.BindingElement, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(bindingElement, p);
        if (!statement?.kind || statement.kind !== JS.Kind.BindingElement) {
            return statement;
        }
        bindingElement = statement as JS.BindingElement;

        const updates: any = {
            prefix: await this.visitSpace(bindingElement.prefix, p),
            markers: await this.visitMarkers(bindingElement.markers, p),
            propertyName: bindingElement.propertyName && await this.visitRightPadded(bindingElement.propertyName, p),
            name: await this.visitDefined<TypedTree>(bindingElement.name, p),
            initializer: bindingElement.initializer && await this.visitLeftPadded(bindingElement.initializer, p),
            variableType: bindingElement.variableType && (await this.visitType(bindingElement.variableType, p) as Type.Variable)
        };
        return updateIfChanged(bindingElement, updates);
    }

    protected async visitExportDeclaration(exportDeclaration: JS.ExportDeclaration, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(exportDeclaration, p);
        if (!statement?.kind || statement.kind !== JS.Kind.ExportDeclaration) {
            return statement;
        }
        exportDeclaration = statement as JS.ExportDeclaration;

        const updates: any = {
            prefix: await this.visitSpace(exportDeclaration.prefix, p),
            markers: await this.visitMarkers(exportDeclaration.markers, p),
            modifiers: await mapAsync(exportDeclaration.modifiers, item => this.visitDefined<J.Modifier>(item, p)),
            typeOnly: await this.visitLeftPadded(exportDeclaration.typeOnly, p),
            exportClause: exportDeclaration.exportClause && await this.visitDefined<Expression>(exportDeclaration.exportClause, p),
            moduleSpecifier: exportDeclaration.moduleSpecifier && await this.visitLeftPadded(exportDeclaration.moduleSpecifier, p),
            attributes: exportDeclaration.attributes && await this.visitDefined<JS.ImportAttributes>(exportDeclaration.attributes, p)
        };
        return updateIfChanged(exportDeclaration, updates);
    }

    protected async visitExportAssignment(exportAssignment: JS.ExportAssignment, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(exportAssignment, p);
        if (!statement?.kind || statement.kind !== JS.Kind.ExportAssignment) {
            return statement;
        }
        exportAssignment = statement as JS.ExportAssignment;

        const updates: any = {
            prefix: await this.visitSpace(exportAssignment.prefix, p),
            markers: await this.visitMarkers(exportAssignment.markers, p),
            expression: await this.visitLeftPadded<Expression>(exportAssignment.expression, p)
        };
        return updateIfChanged(exportAssignment, updates);
    }

    protected async visitNamedExports(namedExports: JS.NamedExports, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(namedExports.prefix, p),
            markers: await this.visitMarkers(namedExports.markers, p),
            elements: await this.visitContainer(namedExports.elements, p),
            type: namedExports.type && await this.visitType(namedExports.type, p)
        };
        return updateIfChanged(namedExports, updates);
    }

    protected async visitExportSpecifier(exportSpecifier: JS.ExportSpecifier, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(exportSpecifier.prefix, p),
            markers: await this.visitMarkers(exportSpecifier.markers, p),
            typeOnly: await this.visitLeftPadded(exportSpecifier.typeOnly, p),
            specifier: await this.visitDefined<Expression>(exportSpecifier.specifier, p),
            type: exportSpecifier.type && await this.visitType(exportSpecifier.type, p)
        };
        return updateIfChanged(exportSpecifier, updates);
    }

    override async accept<J2 extends J, P2 extends P>(j: J2, p: P2): Promise<J | undefined> {
        if (isJavaScript(j)) {
            const tree = j as JS;
            switch (tree.kind) {
                case JS.Kind.Alias:
                    return this.visitAlias(tree as unknown as JS.Alias, p);
                case JS.Kind.ArrowFunction:
                    return this.visitArrowFunction(tree as unknown as JS.ArrowFunction, p);
                case JS.Kind.As:
                    return this.visitAs(tree as unknown as JS.As, p);
                case JS.Kind.Await:
                    return this.visitAwait(tree as unknown as JS.Await, p);
                case JS.Kind.CompilationUnit:
                    return this.visitJsCompilationUnit(tree as unknown as JS.CompilationUnit, p);
                case JS.Kind.ComputedPropertyName:
                    return this.visitComputedPropertyName(tree as unknown as JS.ComputedPropertyName, p);
                case JS.Kind.ConditionalType:
                    return this.visitConditionalType(tree as unknown as JS.ConditionalType, p);
                case JS.Kind.Delete:
                    return this.visitDelete(tree as unknown as JS.Delete, p);
                case JS.Kind.ExpressionStatement:
                    return this.visitExpressionStatement(tree as unknown as JS.ExpressionStatement, p);
                case JS.Kind.ExpressionWithTypeArguments:
                    return this.visitExpressionWithTypeArguments(tree as unknown as JS.ExpressionWithTypeArguments, p);
                case JS.Kind.FunctionCall:
                    return this.visitFunctionCall(tree as unknown as JS.FunctionCall, p);
                case JS.Kind.FunctionType:
                    return this.visitFunctionType(tree as unknown as JS.FunctionType, p);
                case JS.Kind.InferType:
                    return this.visitInferType(tree as unknown as JS.InferType, p);
                case JS.Kind.ImportType:
                    return this.visitImportType(tree as unknown as JS.ImportType, p);
                case JS.Kind.Import:
                    return this.visitImportDeclaration(tree as unknown as JS.Import, p);
                case JS.Kind.ImportClause:
                    return this.visitImportClause(tree as unknown as JS.ImportClause, p);
                case JS.Kind.NamedImports:
                    return this.visitNamedImports(tree as unknown as JS.NamedImports, p);
                case JS.Kind.ImportSpecifier:
                    return this.visitImportSpecifier(tree as unknown as JS.ImportSpecifier, p);
                case JS.Kind.ImportAttributes:
                    return this.visitImportAttributes(tree as unknown as JS.ImportAttributes, p);
                case JS.Kind.ImportTypeAttributes:
                    return this.visitImportTypeAttributes(tree as unknown as JS.ImportTypeAttributes, p);
                case JS.Kind.ImportAttribute:
                    return this.visitImportAttribute(tree as unknown as JS.ImportAttribute, p);
                case JS.Kind.JsxAttribute:
                    return this.visitJsxAttribute(tree as unknown as JSX.Attribute, p);
                case JS.Kind.JsxSpreadAttribute:
                    return this.visitJsxSpreadAttribute(tree as unknown as JSX.SpreadAttribute, p);
                case JS.Kind.JsxEmbeddedExpression:
                    return this.visitJsxEmbeddedExpression(tree as unknown as JSX.EmbeddedExpression, p);
                case JS.Kind.JsxNamespacedName:
                    return this.visitJsxNamespacedName(tree as unknown as JSX.NamespacedName, p);
                case JS.Kind.JsxTag:
                    return this.visitJsxTag(tree as unknown as JSX.Tag, p);
                case JS.Kind.Binary:
                    return this.visitBinaryExtensions(tree as unknown as JS.Binary, p);
                case JS.Kind.LiteralType:
                    return this.visitLiteralType(tree as unknown as JS.LiteralType, p);
                case JS.Kind.MappedType:
                    return this.visitMappedType(tree as unknown as JS.MappedType, p);
                case JS.Kind.MappedTypeKeysRemapping:
                    return this.visitMappedTypeKeysRemapping(tree as unknown as JS.MappedType.KeysRemapping, p);
                case JS.Kind.MappedTypeParameter:
                    return this.visitMappedTypeParameter(tree as unknown as JS.MappedType.Parameter, p);
                case JS.Kind.ObjectBindingPattern:
                    return this.visitObjectBindingPattern(tree as unknown as JS.ObjectBindingPattern, p);
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
                case JS.Kind.Tuple:
                    return this.visitTuple(tree as unknown as JS.Tuple, p);
                case JS.Kind.TypeDeclaration:
                    return this.visitTypeDeclaration(tree as unknown as JS.TypeDeclaration, p);
                case JS.Kind.TypeOf:
                    return this.visitTypeOf(tree as unknown as JS.TypeOf, p);
                case JS.Kind.TypeTreeExpression:
                    return this.visitTypeTreeExpression(tree as unknown as JS.TypeTreeExpression, p);
                case JS.Kind.AssignmentOperation:
                    return this.visitAssignmentOperationExtensions(tree as unknown as JS.AssignmentOperation, p);
                case JS.Kind.IndexedAccessType:
                    return this.visitIndexedAccessType(tree as unknown as JS.IndexedAccessType, p);
                case JS.Kind.IndexType:
                    return this.visitIndexedAccessTypeIndexType(tree as unknown as JS.IndexedAccessType.IndexType, p);
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
                case JS.Kind.WithStatement:
                    return this.visitWithStatement(tree as unknown as JS.WithStatement, p);
                case JS.Kind.IndexSignatureDeclaration:
                    return this.visitIndexSignatureDeclaration(tree as unknown as JS.IndexSignatureDeclaration, p);
                case JS.Kind.ComputedPropertyMethodDeclaration:
                    return this.visitComputedPropertyMethodDeclaration(tree as unknown as JS.ComputedPropertyMethodDeclaration, p);
                case JS.Kind.ForOfLoop:
                    return this.visitForOfLoop(tree as unknown as JS.ForOfLoop, p);
                case JS.Kind.ForInLoop:
                    return this.visitForInLoop(tree as unknown as JS.ForInLoop, p);
                case JS.Kind.NamespaceDeclaration:
                    return this.visitNamespaceDeclaration(tree as unknown as JS.NamespaceDeclaration, p);
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
