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
import {JavaScriptVisitor} from './visitor';
import {J} from '../java';
import {JS, JSX} from './tree';
import {Cursor, Tree} from "../tree";

/**
 * A visitor that compares two AST trees in lock step.
 * It takes another `J` instance as context and visits both trees simultaneously.
 * The visit operation is aborted when the nodes don't match.
 */
export class JavaScriptComparatorVisitor extends JavaScriptVisitor<J> {
    /**
     * Flag indicating whether the trees match so far
     */
    protected match: boolean = true;

    /**
     * Creates a new comparator visitor.
     */
    constructor() {
        super();
    }

    /**
     * Compares two AST trees.
     * 
     * @param tree1 The first tree to compare
     * @param tree2 The second tree to compare
     * @returns true if the trees match, false otherwise
     */
    async compare(tree1: J, tree2: J): Promise<boolean> {
        this.match = true;
        await this.visit(tree1, tree2);
        return this.match;
    }

    /**
     * Checks if two nodes have the same kind.
     * 
     * @param j The node being visited
     * @param other The other node to compare with
     * @returns true if the nodes have the same kind, false otherwise
     */
    protected hasSameKind(j: J, other: J): boolean {
        return j.kind === other.kind;
    }

    /**
     * Aborts the visit operation by setting the match flag to false.
     */
    protected abort(): void {
        this.match = false;
    }

    override async visit<R extends J>(j: Tree, p: J, parent?: Cursor): Promise<R | undefined> {
        // If we've already found a mismatch, abort further processing
        if (!this.match) {
            return j as R;
        }

        // Check if the nodes have the same kind
        if (!this.hasSameKind(j as J, p)) {
            this.abort();
            return j as R;
        }

        // Continue with normal visitation, passing the other node as context
        return super.visit(j, p);
    }

    /**
     * Overrides the visitBinary method to compare binary expressions.
     * 
     * @param binary The binary expression to visit
     * @param other The other binary expression to compare with
     * @returns The visited binary expression, or undefined if the visit was aborted
     */
    override async visitBinary(binary: J.Binary, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Binary) {
            this.abort();
            return binary;
        }

        const otherBinary = other as J.Binary;
        if (binary.operator.element !== otherBinary.operator.element) {
            this.abort();
            return binary;
        }

        // Visit left and right operands in lock step
        await this.visit(binary.left, otherBinary.left);
        if (!this.match) return binary;

        await this.visit(binary.right, otherBinary.right);
        return binary;
    }

    /**
     * Overrides the visitIdentifier method to compare identifiers.
     * 
     * @param identifier The identifier to visit
     * @param other The other identifier to compare with
     * @returns The visited identifier, or undefined if the visit was aborted
     */
    override async visitIdentifier(identifier: J.Identifier, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Identifier) {
            this.abort();
            return identifier;
        }

        const otherIdentifier = other as J.Identifier;
        if (identifier.simpleName !== otherIdentifier.simpleName) {
            this.abort();
        }

        return identifier;
    }

    /**
     * Overrides the visitLiteral method to compare literals.
     * 
     * @param literal The literal to visit
     * @param other The other literal to compare with
     * @returns The visited literal, or undefined if the visit was aborted
     */
    override async visitLiteral(literal: J.Literal, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Literal) {
            this.abort();
            return literal;
        }

        const otherLiteral = other as J.Literal;
        if (literal.valueSource !== otherLiteral.valueSource) {
            this.abort();
        }

        return literal;
    }

    /**
     * Overrides the visitBlock method to compare blocks.
     * 
     * @param block The block to visit
     * @param other The other block to compare with
     * @returns The visited block, or undefined if the visit was aborted
     */
    override async visitBlock(block: J.Block, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Block) {
            this.abort();
            return block;
        }

        const otherBlock = other as J.Block;
        if (block.statements.length !== otherBlock.statements.length) {
            this.abort();
            return block;
        }

        // Visit each statement in lock step
        for (let i = 0; i < block.statements.length; i++) {
            await this.visit(block.statements[i].element, otherBlock.statements[i].element);
            if (!this.match) break;
        }

        return block;
    }

    /**
     * Overrides the visitJsCompilationUnit method to compare compilation units.
     * 
     * @param compilationUnit The compilation unit to visit
     * @param other The other compilation unit to compare with
     * @returns The visited compilation unit, or undefined if the visit was aborted
     */
    override async visitJsCompilationUnit(compilationUnit: JS.CompilationUnit, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.CompilationUnit) {
            this.abort();
            return compilationUnit;
        }

        const otherCompilationUnit = other as JS.CompilationUnit;
        if (compilationUnit.statements.length !== otherCompilationUnit.statements.length) {
            this.abort();
            return compilationUnit;
        }

        // Visit each statement in lock step
        for (let i = 0; i < compilationUnit.statements.length; i++) {
            await this.visit(compilationUnit.statements[i].element, otherCompilationUnit.statements[i].element);
            if (!this.match) break;
        }

        return compilationUnit;
    }

    /**
     * Overrides the visitAlias method to compare aliases.
     * 
     * @param alias The alias to visit
     * @param other The other alias to compare with
     * @returns The visited alias, or undefined if the visit was aborted
     */
    override async visitAlias(alias: JS.Alias, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.Alias) {
            this.abort();
            return alias;
        }

        const otherAlias = other as JS.Alias;

        // Visit property name and alias in lock step
        await this.visit(alias.propertyName.element, otherAlias.propertyName.element);
        if (!this.match) return alias;

        await this.visit(alias.alias, otherAlias.alias);
        return alias;
    }

    /**
     * Overrides the visitArrowFunction method to compare arrow functions.
     * 
     * @param arrowFunction The arrow function to visit
     * @param other The other arrow function to compare with
     * @returns The visited arrow function, or undefined if the visit was aborted
     */
    override async visitArrowFunction(arrowFunction: JS.ArrowFunction, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ArrowFunction) {
            this.abort();
            return arrowFunction;
        }

        const otherArrowFunction = other as JS.ArrowFunction;

        // Compare modifiers
        if (arrowFunction.modifiers.length !== otherArrowFunction.modifiers.length) {
            this.abort();
            return arrowFunction;
        }

        // Visit modifiers in lock step
        for (let i = 0; i < arrowFunction.modifiers.length; i++) {
            await this.visit(arrowFunction.modifiers[i], otherArrowFunction.modifiers[i]);
            if (!this.match) return arrowFunction;
        }

        // Visit type parameters if present
        if (!!arrowFunction.typeParameters !== !!otherArrowFunction.typeParameters) {
            this.abort();
            return arrowFunction;
        }

        if (arrowFunction.typeParameters) {
            await this.visit(arrowFunction.typeParameters, otherArrowFunction.typeParameters!);
            if (!this.match) return arrowFunction;
        }

        // Visit lambda
        await this.visit(arrowFunction.lambda, otherArrowFunction.lambda);
        if (!this.match) return arrowFunction;

        // Visit return type expression if present
        if (!!arrowFunction.returnTypeExpression !== !!otherArrowFunction.returnTypeExpression) {
            this.abort();
            return arrowFunction;
        }

        if (arrowFunction.returnTypeExpression) {
            await this.visit(arrowFunction.returnTypeExpression, otherArrowFunction.returnTypeExpression!);
        }

        return arrowFunction;
    }

    /**
     * Overrides the visitAwait method to compare await expressions.
     * 
     * @param await_ The await expression to visit
     * @param other The other await expression to compare with
     * @returns The visited await expression, or undefined if the visit was aborted
     */
    override async visitAwait(await_: JS.Await, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.Await) {
            this.abort();
            return await_;
        }

        const otherAwait = other as JS.Await;

        // Visit expression
        await this.visit(await_.expression, otherAwait.expression);

        return await_;
    }

    /**
     * Overrides the visitJsxTag method to compare JSX tags.
     * 
     * @param element The JSX tag to visit
     * @param other The other JSX tag to compare with
     * @returns The visited JSX tag, or undefined if the visit was aborted
     */
    override async visitJsxTag(element: JSX.Tag, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.JsxTag) {
            this.abort();
            return element;
        }

        const otherElement = other as JSX.Tag;

        // Visit open name
        await this.visit(element.openName.element, otherElement.openName.element);
        if (!this.match) return element;

        // Compare attributes
        if (element.attributes.length !== otherElement.attributes.length) {
            this.abort();
            return element;
        }

        // Visit attributes in lock step
        for (let i = 0; i < element.attributes.length; i++) {
            await this.visit(element.attributes[i].element, otherElement.attributes[i].element);
            if (!this.match) return element;
        }

        // Compare self-closing
        if (!!element.selfClosing !== !!otherElement.selfClosing) {
            this.abort();
            return element;
        }

        // Compare children
        if (!!element.children !== !!otherElement.children) {
            this.abort();
            return element;
        }

        if (element.children) {
            if (element.children.length !== otherElement.children!.length) {
                this.abort();
                return element;
            }

            // Visit children in lock step
            for (let i = 0; i < element.children.length; i++) {
                await this.visit(element.children[i], otherElement.children![i]);
                if (!this.match) return element;
            }
        }

        // Compare closing name
        if (!!element.closingName !== !!otherElement.closingName) {
            this.abort();
            return element;
        }

        if (element.closingName) {
            await this.visit(element.closingName.element, otherElement.closingName!.element);
        }

        return element;
    }

    /**
     * Overrides the visitJsxAttribute method to compare JSX attributes.
     * 
     * @param attribute The JSX attribute to visit
     * @param other The other JSX attribute to compare with
     * @returns The visited JSX attribute, or undefined if the visit was aborted
     */
    override async visitJsxAttribute(attribute: JSX.Attribute, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.JsxAttribute) {
            this.abort();
            return attribute;
        }

        const otherAttribute = other as JSX.Attribute;

        // Visit key
        await this.visit(attribute.key, otherAttribute.key);
        if (!this.match) return attribute;

        // Compare value
        if (!!attribute.value !== !!otherAttribute.value) {
            this.abort();
            return attribute;
        }

        if (attribute.value) {
            await this.visit(attribute.value.element, otherAttribute.value!.element);
        }

        return attribute;
    }

    /**
     * Overrides the visitJsxSpreadAttribute method to compare JSX spread attributes.
     * 
     * @param spread The JSX spread attribute to visit
     * @param other The other JSX spread attribute to compare with
     * @returns The visited JSX spread attribute, or undefined if the visit was aborted
     */
    override async visitJsxSpreadAttribute(spread: JSX.SpreadAttribute, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.JsxSpreadAttribute) {
            this.abort();
            return spread;
        }

        const otherSpread = other as JSX.SpreadAttribute;

        // Visit expression
        await this.visit(spread.expression.element, otherSpread.expression.element);

        return spread;
    }

    /**
     * Overrides the visitJsxExpression method to compare JSX expressions.
     * 
     * @param expr The JSX expression to visit
     * @param other The other JSX expression to compare with
     * @returns The visited JSX expression, or undefined if the visit was aborted
     */
    override async visitJsxEmbeddedExpression(expr: JSX.EmbeddedExpression, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.JsxEmbeddedExpression) {
            this.abort();
            return expr;
        }

        const otherExpr = other as JSX.EmbeddedExpression;

        // Visit expression
        await this.visit(expr.expression.element, otherExpr.expression.element);

        return expr;
    }

    /**
     * Overrides the visitJsxNamespacedName method to compare JSX namespaced names.
     * 
     * @param ns The JSX namespaced name to visit
     * @param other The other JSX namespaced name to compare with
     * @returns The visited JSX namespaced name, or undefined if the visit was aborted
     */
    override async visitJsxNamespacedName(ns: JSX.NamespacedName, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.JsxNamespacedName) {
            this.abort();
            return ns;
        }

        const otherNs = other as JSX.NamespacedName;

        // Visit namespace
        await this.visit(ns.namespace, otherNs.namespace);
        if (!this.match) return ns;

        // Visit name
        await this.visit(ns.name.element, otherNs.name.element);

        return ns;
    }

    /**
     * Overrides the visitConditionalType method to compare conditional types.
     * 
     * @param conditionalType The conditional type to visit
     * @param other The other conditional type to compare with
     * @returns The visited conditional type, or undefined if the visit was aborted
     */
    override async visitConditionalType(conditionalType: JS.ConditionalType, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ConditionalType) {
            this.abort();
            return conditionalType;
        }

        const otherConditionalType = other as JS.ConditionalType;

        // Visit check type
        await this.visit(conditionalType.checkType, otherConditionalType.checkType);
        if (!this.match) return conditionalType;

        // Visit condition
        await this.visit(conditionalType.condition.element, otherConditionalType.condition.element);

        return conditionalType;
    }

    /**
     * Overrides the visitDelete method to compare delete expressions.
     * 
     * @param delete_ The delete expression to visit
     * @param other The other delete expression to compare with
     * @returns The visited delete expression, or undefined if the visit was aborted
     */
    override async visitDelete(delete_: JS.Delete, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.Delete) {
            this.abort();
            return delete_;
        }

        const otherDelete = other as JS.Delete;

        // Visit expression
        await this.visit(delete_.expression, otherDelete.expression);

        return delete_;
    }

    /**
     * Overrides the visitExpressionStatement method to compare expression statements.
     * 
     * @param expressionStatement The expression statement to visit
     * @param other The other expression statement to compare with
     * @returns The visited expression statement, or undefined if the visit was aborted
     */
    override async visitExpressionStatement(expressionStatement: JS.ExpressionStatement, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ExpressionStatement) {
            this.abort();
            return expressionStatement;
        }

        const otherExpressionStatement = other as JS.ExpressionStatement;

        // Visit expression
        await this.visit(expressionStatement.expression, otherExpressionStatement.expression);

        return expressionStatement;
    }

    /**
     * Overrides the visitExpressionWithTypeArguments method to compare expressions with type arguments.
     * 
     * @param expressionWithTypeArguments The expression with type arguments to visit
     * @param other The other expression with type arguments to compare with
     * @returns The visited expression with type arguments, or undefined if the visit was aborted
     */
    override async visitExpressionWithTypeArguments(expressionWithTypeArguments: JS.ExpressionWithTypeArguments, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ExpressionWithTypeArguments) {
            this.abort();
            return expressionWithTypeArguments;
        }

        const otherExpressionWithTypeArguments = other as JS.ExpressionWithTypeArguments;

        // Visit class
        await this.visit(expressionWithTypeArguments.clazz, otherExpressionWithTypeArguments.clazz);
        if (!this.match) return expressionWithTypeArguments;

        // Compare type arguments
        if (!!expressionWithTypeArguments.typeArguments !== !!otherExpressionWithTypeArguments.typeArguments) {
            this.abort();
            return expressionWithTypeArguments;
        }

        if (expressionWithTypeArguments.typeArguments) {
            if (expressionWithTypeArguments.typeArguments.elements.length !== otherExpressionWithTypeArguments.typeArguments!.elements.length) {
                this.abort();
                return expressionWithTypeArguments;
            }

            // Visit type arguments in lock step
            for (let i = 0; i < expressionWithTypeArguments.typeArguments.elements.length; i++) {
                await this.visit(expressionWithTypeArguments.typeArguments.elements[i].element, 
                                otherExpressionWithTypeArguments.typeArguments!.elements[i].element);
                if (!this.match) return expressionWithTypeArguments;
            }
        }

        return expressionWithTypeArguments;
    }

    /**
     * Overrides the visitFunctionCall method to compare method invocations.
     *
     * @param functionCall The function call to visit
     * @param other The other function call to compare with
     * @returns The visited function call, or undefined if the visit was aborted
     */
    override async visitFunctionCall(functionCall: JS.FunctionCall, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.FunctionCall) {
            this.abort();
            return functionCall;
        }

        const otherFunctionCall = other as JS.FunctionCall;

        // Compare function
        if ((functionCall.function === undefined) !== (otherFunctionCall.function === undefined)) {
            this.abort();
            return functionCall;
        }

        // Visit function if present
        if (functionCall.function && otherFunctionCall.function) {
            await this.visit(functionCall.function.element, otherFunctionCall.function.element);
            if (!this.match) return functionCall;
        }

        // Compare typeParameters
        if ((functionCall.typeParameters === undefined) !== (otherFunctionCall.typeParameters === undefined)) {
            this.abort();
            return functionCall;
        }

        // Visit typeParameters if present
        if (functionCall.typeParameters && otherFunctionCall.typeParameters) {
            if (functionCall.typeParameters.elements.length !== otherFunctionCall.typeParameters.elements.length) {
                this.abort();
                return functionCall;
            }

            // Visit each type parameter in lock step
            for (let i = 0; i < functionCall.typeParameters.elements.length; i++) {
                await this.visit(functionCall.typeParameters.elements[i].element, otherFunctionCall.typeParameters.elements[i].element);
                if (!this.match) return functionCall;
            }
        }

        // Compare arguments
        if (functionCall.arguments.elements.length !== otherFunctionCall.arguments.elements.length) {
            this.abort();
            return functionCall;
        }

        // Visit each argument in lock step
        for (let i = 0; i < functionCall.arguments.elements.length; i++) {
            await this.visit(functionCall.arguments.elements[i].element, otherFunctionCall.arguments.elements[i].element);
            if (!this.match) return functionCall;
        }

        return functionCall;
    }

    /**
     * Overrides the visitFunctionType method to compare function types.
     * 
     * @param functionType The function type to visit
     * @param other The other function type to compare with
     * @returns The visited function type, or undefined if the visit was aborted
     */
    override async visitFunctionType(functionType: JS.FunctionType, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.FunctionType) {
            this.abort();
            return functionType;
        }

        const otherFunctionType = other as JS.FunctionType;

        // Compare constructor type
        if (!!functionType.constructorType.element !== !!otherFunctionType.constructorType.element) {
            this.abort();
            return functionType;
        }

        if (functionType.constructorType.element) {
            if (functionType.constructorType.element !== otherFunctionType.constructorType.element) {
                this.abort();
                return functionType;
            }
        }

        // Compare type parameters
        if (!!functionType.typeParameters !== !!otherFunctionType.typeParameters) {
            this.abort();
            return functionType;
        }

        if (functionType.typeParameters) {
            await this.visit(functionType.typeParameters, otherFunctionType.typeParameters!);
            if (!this.match) return functionType;
        }

        // Compare parameters
        if (functionType.parameters.elements.length !== otherFunctionType.parameters.elements.length) {
            this.abort();
            return functionType;
        }

        // Visit parameters in lock step
        for (let i = 0; i < functionType.parameters.elements.length; i++) {
            await this.visit(functionType.parameters.elements[i].element, otherFunctionType.parameters.elements[i].element);
            if (!this.match) return functionType;
        }

        // Compare return type
        await this.visit(functionType.returnType.element, otherFunctionType.returnType.element);

        return functionType;
    }

    /**
     * Overrides the visitInferType method to compare infer types.
     * 
     * @param inferType The infer type to visit
     * @param other The other infer type to compare with
     * @returns The visited infer type, or undefined if the visit was aborted
     */
    override async visitInferType(inferType: JS.InferType, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.InferType) {
            this.abort();
            return inferType;
        }

        const otherInferType = other as JS.InferType;

        // Visit type parameter
        await this.visit(inferType.typeParameter.element, otherInferType.typeParameter.element);

        return inferType;
    }

    /**
     * Overrides the visitImportType method to compare import types.
     * 
     * @param importType The import type to visit
     * @param other The other import type to compare with
     * @returns The visited import type, or undefined if the visit was aborted
     */
    override async visitImportType(importType: JS.ImportType, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ImportType) {
            this.abort();
            return importType;
        }

        const otherImportType = other as JS.ImportType;

        // Compare has typeof
        if (importType.hasTypeof.element !== otherImportType.hasTypeof.element) {
            this.abort();
            return importType;
        }

        // Compare argument and attributes
        if (importType.argumentAndAttributes.elements.length !== otherImportType.argumentAndAttributes.elements.length) {
            this.abort();
            return importType;
        }

        // Visit argument and attributes in lock step
        for (let i = 0; i < importType.argumentAndAttributes.elements.length; i++) {
            await this.visit(importType.argumentAndAttributes.elements[i].element, 
                           otherImportType.argumentAndAttributes.elements[i].element);
            if (!this.match) return importType;
        }

        // Compare qualifier
        if (!!importType.qualifier !== !!otherImportType.qualifier) {
            this.abort();
            return importType;
        }

        if (importType.qualifier) {
            await this.visit(importType.qualifier.element, otherImportType.qualifier!.element);
            if (!this.match) return importType;
        }

        // Compare type arguments
        if (!!importType.typeArguments !== !!otherImportType.typeArguments) {
            this.abort();
            return importType;
        }

        if (importType.typeArguments) {
            if (importType.typeArguments.elements.length !== otherImportType.typeArguments!.elements.length) {
                this.abort();
                return importType;
            }

            // Visit type arguments in lock step
            for (let i = 0; i < importType.typeArguments.elements.length; i++) {
                await this.visit(importType.typeArguments.elements[i].element, 
                               otherImportType.typeArguments!.elements[i].element);
                if (!this.match) return importType;
            }
        }

        return importType;
    }

    /**
     * Overrides the visitImportDeclaration method to compare import declarations.
     * 
     * @param jsImport The import declaration to visit
     * @param other The other import declaration to compare with
     * @returns The visited import declaration, or undefined if the visit was aborted
     */
    override async visitImportDeclaration(jsImport: JS.Import, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.Import) {
            this.abort();
            return jsImport;
        }

        const otherImport = other as JS.Import;

        // Compare modifiers
        if (jsImport.modifiers.length !== otherImport.modifiers.length) {
            this.abort();
            return jsImport;
        }

        // Visit each modifier in lock step
        for (let i = 0; i < jsImport.modifiers.length; i++) {
            await this.visit(jsImport.modifiers[i], otherImport.modifiers[i]);
            if (!this.match) return jsImport;
        }

        // Compare import clause
        if (!!jsImport.importClause !== !!otherImport.importClause) {
            this.abort();
            return jsImport;
        }

        if (jsImport.importClause) {
            await this.visit(jsImport.importClause, otherImport.importClause!);
            if (!this.match) return jsImport;
        }

        // Visit module specifier
        if (jsImport.moduleSpecifier) {
            await this.visit(jsImport.moduleSpecifier.element, otherImport.moduleSpecifier!.element);
            if (!this.match) return jsImport;
        }

        // Compare attributes
        if (!!jsImport.attributes !== !!otherImport.attributes) {
            this.abort();
            return jsImport;
        }

        if (jsImport.attributes) {
            await this.visit(jsImport.attributes, otherImport.attributes!);
        }

        // Compare initializer
        if (jsImport.initializer) {
            await this.visit(jsImport.initializer.element, otherImport.initializer!.element);
            if (!this.match) return jsImport;
        }

        return jsImport;
    }

    /**
     * Overrides the visitImportClause method to compare import clauses.
     * 
     * @param importClause The import clause to visit
     * @param other The other import clause to compare with
     * @returns The visited import clause, or undefined if the visit was aborted
     */
    override async visitImportClause(importClause: JS.ImportClause, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ImportClause) {
            this.abort();
            return importClause;
        }

        const otherImportClause = other as JS.ImportClause;

        // Compare name
        if (!!importClause.name !== !!otherImportClause.name) {
            this.abort();
            return importClause;
        }

        if (importClause.name) {
            await this.visit(importClause.name.element, otherImportClause.name!.element);
            if (!this.match) return importClause;
        }

        // Compare named bindings
        if (!!importClause.namedBindings !== !!otherImportClause.namedBindings) {
            this.abort();
            return importClause;
        }

        if (importClause.namedBindings) {
            await this.visit(importClause.namedBindings, otherImportClause.namedBindings!);
        }

        return importClause;
    }

    /**
     * Overrides the visitNamedImports method to compare named imports.
     * 
     * @param namedImports The named imports to visit
     * @param other The other named imports to compare with
     * @returns The visited named imports, or undefined if the visit was aborted
     */
    override async visitNamedImports(namedImports: JS.NamedImports, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.NamedImports) {
            this.abort();
            return namedImports;
        }

        const otherNamedImports = other as JS.NamedImports;

        // Compare elements
        if (namedImports.elements.elements.length !== otherNamedImports.elements.elements.length) {
            this.abort();
            return namedImports;
        }

        // Visit elements in lock step
        for (let i = 0; i < namedImports.elements.elements.length; i++) {
            await this.visit(namedImports.elements.elements[i].element, otherNamedImports.elements.elements[i].element);
            if (!this.match) return namedImports;
        }

        return namedImports;
    }

    /**
     * Overrides the visitImportSpecifier method to compare import specifiers.
     * 
     * @param importSpecifier The import specifier to visit
     * @param other The other import specifier to compare with
     * @returns The visited import specifier, or undefined if the visit was aborted
     */
    override async visitImportSpecifier(importSpecifier: JS.ImportSpecifier, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ImportSpecifier) {
            this.abort();
            return importSpecifier;
        }

        const otherImportSpecifier = other as JS.ImportSpecifier;

        // Compare import type
        if (!!importSpecifier.importType.element !== !!otherImportSpecifier.importType.element) {
            this.abort();
            return importSpecifier;
        }

        if (importSpecifier.importType.element) {
            if (importSpecifier.importType.element !== otherImportSpecifier.importType.element) {
                this.abort();
                return importSpecifier;
            }
        }

        // Visit specifier
        await this.visit(importSpecifier.specifier, otherImportSpecifier.specifier);

        return importSpecifier;
    }

    /**
     * Overrides the visitImportAttributes method to compare import attributes.
     * 
     * @param importAttributes The import attributes to visit
     * @param other The other import attributes to compare with
     * @returns The visited import attributes, or undefined if the visit was aborted
     */
    override async visitImportAttributes(importAttributes: JS.ImportAttributes, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ImportAttributes) {
            this.abort();
            return importAttributes;
        }

        const otherImportAttributes = other as JS.ImportAttributes;

        // Compare elements
        if (importAttributes.elements.elements.length !== otherImportAttributes.elements.elements.length) {
            this.abort();
            return importAttributes;
        }

        // Visit elements in lock step
        for (let i = 0; i < importAttributes.elements.elements.length; i++) {
            await this.visit(importAttributes.elements.elements[i].element, otherImportAttributes.elements.elements[i].element);
            if (!this.match) return importAttributes;
        }

        return importAttributes;
    }

    /**
     * Overrides the visitImportTypeAttributes method to compare import type attributes.
     * 
     * @param importTypeAttributes The import type attributes to visit
     * @param other The other import type attributes to compare with
     * @returns The visited import type attributes, or undefined if the visit was aborted
     */
    override async visitImportTypeAttributes(importTypeAttributes: JS.ImportTypeAttributes, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ImportTypeAttributes) {
            this.abort();
            return importTypeAttributes;
        }

        const otherImportTypeAttributes = other as JS.ImportTypeAttributes;

        // Compare token
        await this.visit(importTypeAttributes.token.element, otherImportTypeAttributes.token.element);
        if (!this.match) return importTypeAttributes;

        // Compare elements
        if (importTypeAttributes.elements.elements.length !== otherImportTypeAttributes.elements.elements.length) {
            this.abort();
            return importTypeAttributes;
        }

        // Visit elements in lock step
        for (let i = 0; i < importTypeAttributes.elements.elements.length; i++) {
            await this.visit(importTypeAttributes.elements.elements[i].element, otherImportTypeAttributes.elements.elements[i].element);
            if (!this.match) return importTypeAttributes;
        }

        return importTypeAttributes;
    }

    /**
     * Overrides the visitImportAttribute method to compare import attributes.
     * 
     * @param importAttribute The import attribute to visit
     * @param other The other import attribute to compare with
     * @returns The visited import attribute, or undefined if the visit was aborted
     */
    override async visitImportAttribute(importAttribute: JS.ImportAttribute, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ImportAttribute) {
            this.abort();
            return importAttribute;
        }

        const otherImportAttribute = other as JS.ImportAttribute;

        // Visit name
        await this.visit(importAttribute.name, otherImportAttribute.name);
        if (!this.match) return importAttribute;

        // Visit value
        await this.visit(importAttribute.value.element, otherImportAttribute.value.element);

        return importAttribute;
    }

    /**
     * Overrides the visitBinaryExtensions method to compare binary expressions.
     * 
     * @param jsBinary The binary expression to visit
     * @param other The other binary expression to compare with
     * @returns The visited binary expression, or undefined if the visit was aborted
     */
    override async visitBinaryExtensions(jsBinary: JS.Binary, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.Binary) {
            this.abort();
            return jsBinary;
        }

        const otherBinary = other as JS.Binary;

        // Visit left operand
        await this.visit(jsBinary.left, otherBinary.left);
        if (!this.match) return jsBinary;

        // Compare operator
        if (jsBinary.operator.element !== otherBinary.operator.element) {
            this.abort();
            return jsBinary;
        }

        // Visit right operand
        await this.visit(jsBinary.right, otherBinary.right);

        return jsBinary;
    }

    /**
     * Overrides the visitLiteralType method to compare literal types.
     * 
     * @param literalType The literal type to visit
     * @param other The other literal type to compare with
     * @returns The visited literal type, or undefined if the visit was aborted
     */
    override async visitLiteralType(literalType: JS.LiteralType, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.LiteralType) {
            this.abort();
            return literalType;
        }

        const otherLiteralType = other as JS.LiteralType;

        // Visit literal
        await this.visit(literalType.literal, otherLiteralType.literal);

        return literalType;
    }

    /**
     * Overrides the visitMappedType method to compare mapped types.
     * 
     * @param mappedType The mapped type to visit
     * @param other The other mapped type to compare with
     * @returns The visited mapped type, or undefined if the visit was aborted
     */
    override async visitMappedType(mappedType: JS.MappedType, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.MappedType) {
            this.abort();
            return mappedType;
        }

        const otherMappedType = other as JS.MappedType;

        // Compare prefix token
        if (!!mappedType.prefixToken !== !!otherMappedType.prefixToken) {
            this.abort();
            return mappedType;
        }

        if (mappedType.prefixToken) {
            await this.visit(mappedType.prefixToken.element, otherMappedType.prefixToken!.element);
            if (!this.match) return mappedType;
        }

        // Compare has readonly
        if (mappedType.hasReadonly.element !== otherMappedType.hasReadonly.element) {
            this.abort();
            return mappedType;
        }

        // Visit keys remapping
        await this.visit(mappedType.keysRemapping, otherMappedType.keysRemapping);
        if (!this.match) return mappedType;

        // Compare suffix token
        if (!!mappedType.suffixToken !== !!otherMappedType.suffixToken) {
            this.abort();
            return mappedType;
        }

        if (mappedType.suffixToken) {
            await this.visit(mappedType.suffixToken.element, otherMappedType.suffixToken!.element);
            if (!this.match) return mappedType;
        }

        // Compare has question token
        if (mappedType.hasQuestionToken.element !== otherMappedType.hasQuestionToken.element) {
            this.abort();
            return mappedType;
        }

        // Compare value type
        if (mappedType.valueType.elements.length !== otherMappedType.valueType.elements.length) {
            this.abort();
            return mappedType;
        }

        // Visit value type elements in lock step
        for (let i = 0; i < mappedType.valueType.elements.length; i++) {
            await this.visit(mappedType.valueType.elements[i].element, otherMappedType.valueType.elements[i].element);
            if (!this.match) return mappedType;
        }

        return mappedType;
    }

    /**
     * Overrides the visitKeysRemapping method to compare keys remapping.
     * 
     * @param keysRemapping The keys remapping to visit
     * @param other The other keys remapping to compare with
     * @returns The visited keys remapping, or undefined if the visit was aborted
     */
    override async visitMappedTypeKeysRemapping(keysRemapping: JS.MappedType.KeysRemapping, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.MappedTypeKeysRemapping) {
            this.abort();
            return keysRemapping;
        }

        const otherKeysRemapping = other as JS.MappedType.KeysRemapping;

        // Visit type parameter
        await this.visit(keysRemapping.typeParameter.element, otherKeysRemapping.typeParameter.element);
        if (!this.match) return keysRemapping;

        // Compare name type
        if (!!keysRemapping.nameType !== !!otherKeysRemapping.nameType) {
            this.abort();
            return keysRemapping;
        }

        if (keysRemapping.nameType) {
            await this.visit(keysRemapping.nameType.element, otherKeysRemapping.nameType!.element);
        }

        return keysRemapping;
    }

    /**
     * Overrides the visitMappedTypeParameter method to compare mapped type parameters.
     * 
     * @param mappedTypeParameter The mapped type parameter to visit
     * @param other The other mapped type parameter to compare with
     * @returns The visited mapped type parameter, or undefined if the visit was aborted
     */
    override async visitMappedTypeParameter(mappedTypeParameter: JS.MappedType.Parameter, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.MappedTypeParameter) {
            this.abort();
            return mappedTypeParameter;
        }

        const otherMappedTypeParameter = other as JS.MappedType.Parameter;

        // Visit name
        await this.visit(mappedTypeParameter.name, otherMappedTypeParameter.name);
        if (!this.match) return mappedTypeParameter;

        // Visit iterate type
        await this.visit(mappedTypeParameter.iterateType.element, otherMappedTypeParameter.iterateType.element);

        return mappedTypeParameter;
    }

    /**
     * Overrides the visitObjectBindingPattern method to compare object binding declarations.
     * 
     * @param objectBindingPattern The object binding declarations to visit
     * @param other The other object binding declarations to compare with
     * @returns The visited object binding declarations, or undefined if the visit was aborted
     */
    override async visitObjectBindingPattern(objectBindingPattern: JS.ObjectBindingPattern, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ObjectBindingPattern) {
            this.abort();
            return objectBindingPattern;
        }

        const otherObjectBindingPattern = other as JS.ObjectBindingPattern;

        // Compare leading annotations
        if (objectBindingPattern.leadingAnnotations.length !== otherObjectBindingPattern.leadingAnnotations.length) {
            this.abort();
            return objectBindingPattern;
        }

        // Visit leading annotations in lock step
        for (let i = 0; i < objectBindingPattern.leadingAnnotations.length; i++) {
            await this.visit(objectBindingPattern.leadingAnnotations[i], otherObjectBindingPattern.leadingAnnotations[i]);
            if (!this.match) return objectBindingPattern;
        }

        // Compare modifiers
        if (objectBindingPattern.modifiers.length !== otherObjectBindingPattern.modifiers.length) {
            this.abort();
            return objectBindingPattern;
        }

        // Visit modifiers in lock step
        for (let i = 0; i < objectBindingPattern.modifiers.length; i++) {
            await this.visit(objectBindingPattern.modifiers[i], otherObjectBindingPattern.modifiers[i]);
            if (!this.match) return objectBindingPattern;
        }

        // Compare type expression
        if (!!objectBindingPattern.typeExpression !== !!otherObjectBindingPattern.typeExpression) {
            this.abort();
            return objectBindingPattern;
        }

        if (objectBindingPattern.typeExpression) {
            await this.visit(objectBindingPattern.typeExpression, otherObjectBindingPattern.typeExpression!);
            if (!this.match) return objectBindingPattern;
        }

        // Compare bindings
        if (objectBindingPattern.bindings.elements.length !== otherObjectBindingPattern.bindings.elements.length) {
            this.abort();
            return objectBindingPattern;
        }

        // Visit bindings in lock step
        for (let i = 0; i < objectBindingPattern.bindings.elements.length; i++) {
            await this.visit(objectBindingPattern.bindings.elements[i].element,
                           otherObjectBindingPattern.bindings.elements[i].element);
            if (!this.match) return objectBindingPattern;
        }

        // Compare initializer
        if (!!objectBindingPattern.initializer !== !!otherObjectBindingPattern.initializer) {
            this.abort();
            return objectBindingPattern;
        }

        if (objectBindingPattern.initializer) {
            await this.visit(objectBindingPattern.initializer.element, otherObjectBindingPattern.initializer!.element);
        }

        return objectBindingPattern;
    }

    /**
     * Overrides the visitPropertyAssignment method to compare property assignments.
     * 
     * @param propertyAssignment The property assignment to visit
     * @param other The other property assignment to compare with
     * @returns The visited property assignment, or undefined if the visit was aborted
     */
    override async visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.PropertyAssignment) {
            this.abort();
            return propertyAssignment;
        }

        const otherPropertyAssignment = other as JS.PropertyAssignment;

        // Visit name
        await this.visit(propertyAssignment.name.element, otherPropertyAssignment.name.element);
        if (!this.match) return propertyAssignment;

        // Compare initializer
        if (!!propertyAssignment.initializer !== !!otherPropertyAssignment.initializer) {
            this.abort();
            return propertyAssignment;
        }

        if (propertyAssignment.initializer) {
            await this.visit(propertyAssignment.initializer, otherPropertyAssignment.initializer!);
        }

        return propertyAssignment;
    }

    /**
     * Overrides the visitSatisfiesExpression method to compare satisfies expressions.
     * 
     * @param satisfiesExpression The satisfies expression to visit
     * @param other The other satisfies expression to compare with
     * @returns The visited satisfies expression, or undefined if the visit was aborted
     */
    override async visitSatisfiesExpression(satisfiesExpression: JS.SatisfiesExpression, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.SatisfiesExpression) {
            this.abort();
            return satisfiesExpression;
        }

        const otherSatisfiesExpression = other as JS.SatisfiesExpression;

        // Visit expression
        await this.visit(satisfiesExpression.expression, otherSatisfiesExpression.expression);
        if (!this.match) return satisfiesExpression;

        // Visit satisfies type
        await this.visit(satisfiesExpression.satisfiesType.element, otherSatisfiesExpression.satisfiesType.element);

        return satisfiesExpression;
    }

    /**
     * Overrides the visitScopedVariableDeclarations method to compare scoped variable declarations.
     * 
     * @param scopedVariableDeclarations The scoped variable declarations to visit
     * @param other The other scoped variable declarations to compare with
     * @returns The visited scoped variable declarations, or undefined if the visit was aborted
     */
    override async visitScopedVariableDeclarations(scopedVariableDeclarations: JS.ScopedVariableDeclarations, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ScopedVariableDeclarations) {
            this.abort();
            return scopedVariableDeclarations;
        }

        const otherScopedVariableDeclarations = other as JS.ScopedVariableDeclarations;

        // Compare modifiers
        if (scopedVariableDeclarations.modifiers.length !== otherScopedVariableDeclarations.modifiers.length) {
            this.abort();
            return scopedVariableDeclarations;
        }

        // Visit modifiers in lock step
        for (let i = 0; i < scopedVariableDeclarations.modifiers.length; i++) {
            await this.visit(scopedVariableDeclarations.modifiers[i], otherScopedVariableDeclarations.modifiers[i]);
            if (!this.match) return scopedVariableDeclarations;
        }

        // Compare variables
        if (scopedVariableDeclarations.variables.length !== otherScopedVariableDeclarations.variables.length) {
            this.abort();
            return scopedVariableDeclarations;
        }

        // Visit variables in lock step
        for (let i = 0; i < scopedVariableDeclarations.variables.length; i++) {
            await this.visit(scopedVariableDeclarations.variables[i].element, otherScopedVariableDeclarations.variables[i].element);
            if (!this.match) return scopedVariableDeclarations;
        }

        return scopedVariableDeclarations;
    }

    /**
     * Overrides the visitStatementExpression method to compare statement expressions.
     * 
     * @param statementExpression The statement expression to visit
     * @param other The other statement expression to compare with
     * @returns The visited statement expression, or undefined if the visit was aborted
     */
    override async visitStatementExpression(statementExpression: JS.StatementExpression, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.StatementExpression) {
            this.abort();
            return statementExpression;
        }

        const otherStatementExpression = other as JS.StatementExpression;

        // Visit statement
        await this.visit(statementExpression.statement, otherStatementExpression.statement);

        return statementExpression;
    }

    /**
     * Overrides the visitTaggedTemplateExpression method to compare tagged template expressions.
     * 
     * @param taggedTemplateExpression The tagged template expression to visit
     * @param other The other tagged template expression to compare with
     * @returns The visited tagged template expression, or undefined if the visit was aborted
     */
    override async visitTaggedTemplateExpression(taggedTemplateExpression: JS.TaggedTemplateExpression, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.TaggedTemplateExpression) {
            this.abort();
            return taggedTemplateExpression;
        }

        const otherTaggedTemplateExpression = other as JS.TaggedTemplateExpression;

        // Compare tag
        if (!!taggedTemplateExpression.tag !== !!otherTaggedTemplateExpression.tag) {
            this.abort();
            return taggedTemplateExpression;
        }

        if (taggedTemplateExpression.tag) {
            await this.visit(taggedTemplateExpression.tag.element, otherTaggedTemplateExpression.tag!.element);
            if (!this.match) return taggedTemplateExpression;
        }

        // Compare type arguments
        if (!!taggedTemplateExpression.typeArguments !== !!otherTaggedTemplateExpression.typeArguments) {
            this.abort();
            return taggedTemplateExpression;
        }

        if (taggedTemplateExpression.typeArguments) {
            if (taggedTemplateExpression.typeArguments.elements.length !== otherTaggedTemplateExpression.typeArguments!.elements.length) {
                this.abort();
                return taggedTemplateExpression;
            }

            // Visit type arguments in lock step
            for (let i = 0; i < taggedTemplateExpression.typeArguments.elements.length; i++) {
                await this.visit(taggedTemplateExpression.typeArguments.elements[i].element, 
                               otherTaggedTemplateExpression.typeArguments!.elements[i].element);
                if (!this.match) return taggedTemplateExpression;
            }
        }

        // Visit template expression
        await this.visit(taggedTemplateExpression.templateExpression, otherTaggedTemplateExpression.templateExpression);

        return taggedTemplateExpression;
    }

    /**
     * Overrides the visitTemplateExpression method to compare template expressions.
     * 
     * @param templateExpression The template expression to visit
     * @param other The other template expression to compare with
     * @returns The visited template expression, or undefined if the visit was aborted
     */
    override async visitTemplateExpression(templateExpression: JS.TemplateExpression, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.TemplateExpression) {
            this.abort();
            return templateExpression;
        }

        const otherTemplateExpression = other as JS.TemplateExpression;

        // Visit head
        await this.visit(templateExpression.head, otherTemplateExpression.head);
        if (!this.match) return templateExpression;

        // Compare spans
        if (templateExpression.spans.length !== otherTemplateExpression.spans.length) {
            this.abort();
            return templateExpression;
        }

        // Visit spans in lock step
        for (let i = 0; i < templateExpression.spans.length; i++) {
            await this.visit(templateExpression.spans[i].element, otherTemplateExpression.spans[i].element);
            if (!this.match) return templateExpression;
        }

        return templateExpression;
    }

    /**
     * Overrides the visitTemplateExpressionSpan method to compare template expression spans.
     * 
     * @param span The template expression span to visit
     * @param other The other template expression span to compare with
     * @returns The visited template expression span, or undefined if the visit was aborted
     */
    override async visitTemplateExpressionSpan(span: JS.TemplateExpression.Span, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.TemplateExpressionSpan) {
            this.abort();
            return span;
        }

        const otherSpan = other as JS.TemplateExpression.Span;

        // Visit expression
        await this.visit(span.expression, otherSpan.expression);
        if (!this.match) return span;

        // Visit tail
        await this.visit(span.tail, otherSpan.tail);

        return span;
    }

    /**
     * Overrides the visitTuple method to compare tuples.
     * 
     * @param tuple The tuple to visit
     * @param other The other tuple to compare with
     * @returns The visited tuple, or undefined if the visit was aborted
     */
    override async visitTuple(tuple: JS.Tuple, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.Tuple) {
            this.abort();
            return tuple;
        }

        const otherTuple = other as JS.Tuple;

        // Compare elements
        if (tuple.elements.elements.length !== otherTuple.elements.elements.length) {
            this.abort();
            return tuple;
        }

        // Visit elements in lock step
        for (let i = 0; i < tuple.elements.elements.length; i++) {
            await this.visit(tuple.elements.elements[i].element, otherTuple.elements.elements[i].element);
            if (!this.match) return tuple;
        }

        return tuple;
    }

    /**
     * Overrides the visitTypeDeclaration method to compare type declarations.
     * 
     * @param typeDeclaration The type declaration to visit
     * @param other The other type declaration to compare with
     * @returns The visited type declaration, or undefined if the visit was aborted
     */
    override async visitTypeDeclaration(typeDeclaration: JS.TypeDeclaration, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.TypeDeclaration) {
            this.abort();
            return typeDeclaration;
        }

        const otherTypeDeclaration = other as JS.TypeDeclaration;

        // Compare modifiers
        if (typeDeclaration.modifiers.length !== otherTypeDeclaration.modifiers.length) {
            this.abort();
            return typeDeclaration;
        }

        // Visit modifiers in lock step
        for (let i = 0; i < typeDeclaration.modifiers.length; i++) {
            await this.visit(typeDeclaration.modifiers[i], otherTypeDeclaration.modifiers[i]);
            if (!this.match) return typeDeclaration;
        }

        // Visit name
        await this.visit(typeDeclaration.name.element, otherTypeDeclaration.name.element);
        if (!this.match) return typeDeclaration;

        // Compare type parameters
        if (!!typeDeclaration.typeParameters !== !!otherTypeDeclaration.typeParameters) {
            this.abort();
            return typeDeclaration;
        }

        if (typeDeclaration.typeParameters) {
            await this.visit(typeDeclaration.typeParameters, otherTypeDeclaration.typeParameters!);
            if (!this.match) return typeDeclaration;
        }

        // Visit initializer
        await this.visit(typeDeclaration.initializer.element, otherTypeDeclaration.initializer.element);

        return typeDeclaration;
    }

    /**
     * Overrides the visitTypeOf method to compare typeof expressions.
     * 
     * @param typeOf The typeof expression to visit
     * @param other The other typeof expression to compare with
     * @returns The visited typeof expression, or undefined if the visit was aborted
     */
    override async visitTypeOf(typeOf: JS.TypeOf, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.TypeOf) {
            this.abort();
            return typeOf;
        }

        const otherTypeOf = other as JS.TypeOf;

        // Visit expression
        await this.visit(typeOf.expression, otherTypeOf.expression);

        return typeOf;
    }

    /**
     * Overrides the visitTypeTreeExpression method to compare type tree expressions.
     * 
     * @param typeTreeExpression The type tree expression to visit
     * @param other The other type tree expression to compare with
     * @returns The visited type tree expression, or undefined if the visit was aborted
     */
    override async visitTypeTreeExpression(typeTreeExpression: JS.TypeTreeExpression, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.TypeTreeExpression) {
            this.abort();
            return typeTreeExpression;
        }

        const otherTypeTreeExpression = other as JS.TypeTreeExpression;

        // Visit expression
        await this.visit(typeTreeExpression.expression, otherTypeTreeExpression.expression);

        return typeTreeExpression;
    }

    /**
     * Overrides the visitAs method to compare as expressions.
     *
     * @param as_ The as expression to visit
     * @param other The other as expression to compare with
     * @returns The visited as expression, or undefined if the visit was aborted
     */
    override async visitAs(as_: JS.As, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.As) {
            this.abort();
            return as_;
        }

        const otherAs = other as JS.As;

        // Visit left and right operands in lock step
        await this.visit(as_.left.element, otherAs.left.element);
        if (!this.match) return as_;

        await this.visit(as_.right, otherAs.right);
        return as_;
    }

    /**
     * Overrides the visitAssignmentOperationExtensions method to compare assignment operations.
     * 
     * @param assignmentOperation The assignment operation to visit
     * @param other The other assignment operation to compare with
     * @returns The visited assignment operation, or undefined if the visit was aborted
     */
    override async visitAssignmentOperationExtensions(assignmentOperation: JS.AssignmentOperation, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.AssignmentOperation) {
            this.abort();
            return assignmentOperation;
        }

        const otherAssignmentOperation = other as JS.AssignmentOperation;

        // Visit variable
        await this.visit(assignmentOperation.variable, otherAssignmentOperation.variable);
        if (!this.match) return assignmentOperation;

        // Compare operator
        if (assignmentOperation.operator.element !== otherAssignmentOperation.operator.element) {
            this.abort();
            return assignmentOperation;
        }

        // Visit assignment
        await this.visit(assignmentOperation.assignment, otherAssignmentOperation.assignment);

        return assignmentOperation;
    }

    /**
     * Overrides the visitIndexedAccessType method to compare indexed access types.
     * 
     * @param indexedAccessType The indexed access type to visit
     * @param other The other indexed access type to compare with
     * @returns The visited indexed access type, or undefined if the visit was aborted
     */
    override async visitIndexedAccessType(indexedAccessType: JS.IndexedAccessType, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.IndexedAccessType) {
            this.abort();
            return indexedAccessType;
        }

        const otherIndexedAccessType = other as JS.IndexedAccessType;

        // Visit object type
        await this.visit(indexedAccessType.objectType, otherIndexedAccessType.objectType);
        if (!this.match) return indexedAccessType;

        // Visit index type
        await this.visit(indexedAccessType.indexType, otherIndexedAccessType.indexType);

        return indexedAccessType;
    }

    /**
     * Overrides the visitIndexType method to compare index types.
     * 
     * @param indexType The index type to visit
     * @param other The other index type to compare with
     * @returns The visited index type, or undefined if the visit was aborted
     */
    override async visitIndexedAccessTypeIndexType(indexType: JS.IndexedAccessType.IndexType, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.IndexType) {
            this.abort();
            return indexType;
        }

        const otherIndexType = other as JS.IndexedAccessType.IndexType;

        // Visit element
        await this.visit(indexType.element.element, otherIndexType.element.element);

        return indexType;
    }

    /**
     * Overrides the visitTypeQuery method to compare type queries.
     * 
     * @param typeQuery The type query to visit
     * @param other The other type query to compare with
     * @returns The visited type query, or undefined if the visit was aborted
     */
    override async visitTypeQuery(typeQuery: JS.TypeQuery, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.TypeQuery) {
            this.abort();
            return typeQuery;
        }

        const otherTypeQuery = other as JS.TypeQuery;

        // Visit type expression
        await this.visit(typeQuery.typeExpression, otherTypeQuery.typeExpression);
        if (!this.match) return typeQuery;

        // Compare type arguments
        if (!!typeQuery.typeArguments !== !!otherTypeQuery.typeArguments) {
            this.abort();
            return typeQuery;
        }

        if (typeQuery.typeArguments) {
            if (typeQuery.typeArguments.elements.length !== otherTypeQuery.typeArguments!.elements.length) {
                this.abort();
                return typeQuery;
            }

            // Visit type arguments in lock step
            for (let i = 0; i < typeQuery.typeArguments.elements.length; i++) {
                await this.visit(typeQuery.typeArguments.elements[i].element, 
                               otherTypeQuery.typeArguments!.elements[i].element);
                if (!this.match) return typeQuery;
            }
        }

        return typeQuery;
    }

    /**
     * Overrides the visitTypeInfo method to compare type info.
     * 
     * @param typeInfo The type info to visit
     * @param other The other type info to compare with
     * @returns The visited type info, or undefined if the visit was aborted
     */
    override async visitTypeInfo(typeInfo: JS.TypeInfo, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.TypeInfo) {
            this.abort();
            return typeInfo;
        }

        const otherTypeInfo = other as JS.TypeInfo;

        // Visit type identifier
        await this.visit(typeInfo.typeIdentifier, otherTypeInfo.typeIdentifier);

        return typeInfo;
    }

    /**
     * Overrides the visitComputedPropertyName method to compare computed property names.
     * 
     * @param computedPropertyName The computed property name to visit
     * @param other The other computed property name to compare with
     * @returns The visited computed property name, or undefined if the visit was aborted
     */
    override async visitComputedPropertyName(computedPropertyName: JS.ComputedPropertyName, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ComputedPropertyName) {
            this.abort();
            return computedPropertyName;
        }

        const otherComputedPropertyName = other as JS.ComputedPropertyName;

        // Visit expression
        await this.visit(computedPropertyName.expression.element, otherComputedPropertyName.expression.element);

        return computedPropertyName;
    }

    /**
     * Overrides the visitTypeOperator method to compare type operators.
     * 
     * @param typeOperator The type operator to visit
     * @param other The other type operator to compare with
     * @returns The visited type operator, or undefined if the visit was aborted
     */
    override async visitTypeOperator(typeOperator: JS.TypeOperator, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.TypeOperator) {
            this.abort();
            return typeOperator;
        }

        const otherTypeOperator = other as JS.TypeOperator;

        // Visit expression
        await this.visit(typeOperator.expression.element, otherTypeOperator.expression.element);

        return typeOperator;
    }

    /**
     * Overrides the visitTypePredicate method to compare type predicates.
     * 
     * @param typePredicate The type predicate to visit
     * @param other The other type predicate to compare with
     * @returns The visited type predicate, or undefined if the visit was aborted
     */
    override async visitTypePredicate(typePredicate: JS.TypePredicate, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.TypePredicate) {
            this.abort();
            return typePredicate;
        }

        const otherTypePredicate = other as JS.TypePredicate;

        // Compare asserts
        if (typePredicate.asserts.element !== otherTypePredicate.asserts.element) {
            this.abort();
            return typePredicate;
        }

        // Visit parameter name
        await this.visit(typePredicate.parameterName, otherTypePredicate.parameterName);
        if (!this.match) return typePredicate;

        // Compare expression
        if (!!typePredicate.expression !== !!otherTypePredicate.expression) {
            this.abort();
            return typePredicate;
        }

        if (typePredicate.expression) {
            await this.visit(typePredicate.expression.element, otherTypePredicate.expression!.element);
        }

        return typePredicate;
    }

    /**
     * Overrides the visitUnion method to compare unions.
     * 
     * @param union The union to visit
     * @param other The other union to compare with
     * @returns The visited union, or undefined if the visit was aborted
     */
    override async visitUnion(union: JS.Union, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.Union) {
            this.abort();
            return union;
        }

        const otherUnion = other as JS.Union;

        // Compare types
        if (union.types.length !== otherUnion.types.length) {
            this.abort();
            return union;
        }

        // Visit types in lock step
        for (let i = 0; i < union.types.length; i++) {
            await this.visit(union.types[i].element, otherUnion.types[i].element);
            if (!this.match) return union;
        }

        return union;
    }

    /**
     * Overrides the visitIntersection method to compare intersections.
     * 
     * @param intersection The intersection to visit
     * @param other The other intersection to compare with
     * @returns The visited intersection, or undefined if the visit was aborted
     */
    override async visitIntersection(intersection: JS.Intersection, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.Intersection) {
            this.abort();
            return intersection;
        }

        const otherIntersection = other as JS.Intersection;

        // Compare types
        if (intersection.types.length !== otherIntersection.types.length) {
            this.abort();
            return intersection;
        }

        // Visit types in lock step
        for (let i = 0; i < intersection.types.length; i++) {
            await this.visit(intersection.types[i].element, otherIntersection.types[i].element);
            if (!this.match) return intersection;
        }

        return intersection;
    }

    /**
     * Overrides the visitAnnotatedType method to compare annotated types.
     * 
     * @param annotatedType The annotated type to visit
     * @param other The other annotated type to compare with
     * @returns The visited annotated type, or undefined if the visit was aborted
     */
    override async visitAnnotatedType(annotatedType: J.AnnotatedType, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.AnnotatedType) {
            this.abort();
            return annotatedType;
        }

        const otherAnnotatedType = other as J.AnnotatedType;

        // Compare annotations
        if (annotatedType.annotations.length !== otherAnnotatedType.annotations.length) {
            this.abort();
            return annotatedType;
        }

        // Visit each annotation in lock step
        for (let i = 0; i < annotatedType.annotations.length; i++) {
            await this.visit(annotatedType.annotations[i], otherAnnotatedType.annotations[i]);
            if (!this.match) return annotatedType;
        }

        // Visit type expression
        await this.visit(annotatedType.typeExpression, otherAnnotatedType.typeExpression);

        return annotatedType;
    }

    /**
     * Overrides the visitAnnotation method to compare annotations.
     * 
     * @param annotation The annotation to visit
     * @param other The other annotation to compare with
     * @returns The visited annotation, or undefined if the visit was aborted
     */
    override async visitAnnotation(annotation: J.Annotation, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Annotation) {
            this.abort();
            return annotation;
        }

        const otherAnnotation = other as J.Annotation;

        // Visit annotation type
        await this.visit(annotation.annotationType, otherAnnotation.annotationType);
        if (!this.match) return annotation;

        // Compare arguments
        if ((annotation.arguments === undefined) !== (otherAnnotation.arguments === undefined)) {
            this.abort();
            return annotation;
        }

        // If both have arguments, visit them
        if (annotation.arguments && otherAnnotation.arguments) {
            if (annotation.arguments.elements.length !== otherAnnotation.arguments.elements.length) {
                this.abort();
                return annotation;
            }

            // Visit each argument in lock step
            for (let i = 0; i < annotation.arguments.elements.length; i++) {
                await this.visit(annotation.arguments.elements[i].element, otherAnnotation.arguments.elements[i].element);
                if (!this.match) return annotation;
            }
        }

        return annotation;
    }

    /**
     * Overrides the visitArrayAccess method to compare array access expressions.
     * 
     * @param arrayAccess The array access expression to visit
     * @param other The other array access expression to compare with
     * @returns The visited array access expression, or undefined if the visit was aborted
     */
    override async visitArrayAccess(arrayAccess: J.ArrayAccess, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.ArrayAccess) {
            this.abort();
            return arrayAccess;
        }

        const otherArrayAccess = other as J.ArrayAccess;

        // Visit indexed expression
        await this.visit(arrayAccess.indexed, otherArrayAccess.indexed);
        if (!this.match) return arrayAccess;

        // Visit dimension
        await this.visit(arrayAccess.dimension, otherArrayAccess.dimension);

        return arrayAccess;
    }

    /**
     * Overrides the visitArrayDimension method to compare array dimensions.
     * 
     * @param arrayDimension The array dimension to visit
     * @param other The other array dimension to compare with
     * @returns The visited array dimension, or undefined if the visit was aborted
     */
    override async visitArrayDimension(arrayDimension: J.ArrayDimension, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.ArrayDimension) {
            this.abort();
            return arrayDimension;
        }

        const otherArrayDimension = other as J.ArrayDimension;

        // Visit index
        if (arrayDimension.index && otherArrayDimension.index) {
            await this.visit(arrayDimension.index.element, otherArrayDimension.index.element);
        } else if (arrayDimension.index !== otherArrayDimension.index) {
            // One has an index and the other doesn't
            this.abort();
        }

        return arrayDimension;
    }

    /**
     * Overrides the visitArrayType method to compare array types.
     * 
     * @param arrayType The array type to visit
     * @param other The other array type to compare with
     * @returns The visited array type, or undefined if the visit was aborted
     */
    override async visitArrayType(arrayType: J.ArrayType, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.ArrayType) {
            this.abort();
            return arrayType;
        }

        const otherArrayType = other as J.ArrayType;

        // Visit element type
        await this.visit(arrayType.elementType, otherArrayType.elementType);
        if (!this.match) return arrayType;

        // Compare annotations
        if ((arrayType.annotations?.length || 0) !== (otherArrayType.annotations?.length || 0)) {
            this.abort();
            return arrayType;
        }

        // Visit annotations if they exist
        if (arrayType.annotations && otherArrayType.annotations) {
            for (let i = 0; i < arrayType.annotations.length; i++) {
                await this.visit(arrayType.annotations[i], otherArrayType.annotations[i]);
                if (!this.match) return arrayType;
            }
        }

        return arrayType;
    }

    /**
     * Overrides the visitAssert method to compare assert statements.
     * 
     * @param anAssert The assert statement to visit
     * @param other The other assert statement to compare with
     * @returns The visited assert statement, or undefined if the visit was aborted
     */
    override async visitAssert(anAssert: J.Assert, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Assert) {
            this.abort();
            return anAssert;
        }

        const otherAssert = other as J.Assert;

        // Visit condition
        await this.visit(anAssert.condition, otherAssert.condition);
        if (!this.match) return anAssert;

        // Compare detail
        if ((anAssert.detail !== undefined) !== (otherAssert.detail !== undefined)) {
            this.abort();
            return anAssert;
        }

        // Visit detail if it exists
        if (anAssert.detail && otherAssert.detail) {
            await this.visit(anAssert.detail.element, otherAssert.detail.element);
        }

        return anAssert;
    }

    /**
     * Overrides the visitAssignment method to compare assignment expressions.
     * 
     * @param assignment The assignment expression to visit
     * @param other The other assignment expression to compare with
     * @returns The visited assignment expression, or undefined if the visit was aborted
     */
    override async visitAssignment(assignment: J.Assignment, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Assignment) {
            this.abort();
            return assignment;
        }

        const otherAssignment = other as J.Assignment;

        // Visit variable
        await this.visit(assignment.variable, otherAssignment.variable);
        if (!this.match) return assignment;

        // Visit assignment
        if (assignment.assignment && otherAssignment.assignment) {
            await this.visit(assignment.assignment.element, otherAssignment.assignment.element);
        } else if (assignment.assignment !== otherAssignment.assignment) {
            // One has an assignment and the other doesn't
            this.abort();
        }

        return assignment;
    }

    /**
     * Overrides the visitAssignmentOperation method to compare assignment operation expressions.
     * 
     * @param assignOp The assignment operation expression to visit
     * @param other The other assignment operation expression to compare with
     * @returns The visited assignment operation expression, or undefined if the visit was aborted
     */
    override async visitAssignmentOperation(assignOp: J.AssignmentOperation, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.AssignmentOperation) {
            this.abort();
            return assignOp;
        }

        const otherAssignOp = other as J.AssignmentOperation;

        // Visit variable
        await this.visit(assignOp.variable, otherAssignOp.variable);
        if (!this.match) return assignOp;

        // Compare operator
        if (assignOp.operator.element !== otherAssignOp.operator.element) {
            this.abort();
            return assignOp;
        }

        // Visit assignment
        await this.visit(assignOp.assignment, otherAssignOp.assignment);

        return assignOp;
    }

    /**
     * Overrides the visitBreak method to compare break statements.
     * 
     * @param breakStatement The break statement to visit
     * @param other The other break statement to compare with
     * @returns The visited break statement, or undefined if the visit was aborted
     */
    override async visitBreak(breakStatement: J.Break, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Break) {
            this.abort();
            return breakStatement;
        }

        const otherBreak = other as J.Break;

        // Compare label presence
        if ((breakStatement.label !== undefined) !== (otherBreak.label !== undefined)) {
            this.abort();
            return breakStatement;
        }

        // Visit label if it exists
        if (breakStatement.label && otherBreak.label) {
            await this.visit(breakStatement.label, otherBreak.label);
        }

        return breakStatement;
    }

    /**
     * Overrides the visitCase method to compare case statements.
     * 
     * @param aCase The case statement to visit
     * @param other The other case statement to compare with
     * @returns The visited case statement, or undefined if the visit was aborted
     */
    override async visitCase(aCase: J.Case, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Case) {
            this.abort();
            return aCase;
        }

        const otherCase = other as J.Case;

        // Compare case labels
        if (aCase.caseLabels.elements.length !== otherCase.caseLabels.elements.length) {
            this.abort();
            return aCase;
        }

        // Visit each case label in lock step
        for (let i = 0; i < aCase.caseLabels.elements.length; i++) {
            await this.visit(aCase.caseLabels.elements[i].element, otherCase.caseLabels.elements[i].element);
            if (!this.match) return aCase;
        }

        // Compare statements
        if (aCase.statements.elements.length !== otherCase.statements.elements.length) {
            this.abort();
            return aCase;
        }

        // Visit each statement in lock step
        for (let i = 0; i < aCase.statements.elements.length; i++) {
            await this.visit(aCase.statements.elements[i].element, otherCase.statements.elements[i].element);
            if (!this.match) return aCase;
        }

        // Compare body presence
        if ((aCase.body !== undefined) !== (otherCase.body !== undefined)) {
            this.abort();
            return aCase;
        }

        // Visit body if it exists
        if (aCase.body && otherCase.body) {
            await this.visit(aCase.body.element, otherCase.body.element);
            if (!this.match) return aCase;
        }

        // Compare guard presence
        if ((aCase.guard !== undefined) !== (otherCase.guard !== undefined)) {
            this.abort();
            return aCase;
        }

        // Visit guard if it exists
        if (aCase.guard && otherCase.guard) {
            await this.visit(aCase.guard, otherCase.guard);
        }

        return aCase;
    }

    /**
     * Overrides the visitClassDeclaration method to compare class declarations.
     * 
     * @param classDecl The class declaration to visit
     * @param other The other class declaration to compare with
     * @returns The visited class declaration, or undefined if the visit was aborted
     */
    override async visitClassDeclaration(classDecl: J.ClassDeclaration, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.ClassDeclaration) {
            this.abort();
            return classDecl;
        }

        const otherClassDecl = other as J.ClassDeclaration;

        // Compare leading annotations
        if (classDecl.leadingAnnotations.length !== otherClassDecl.leadingAnnotations.length) {
            this.abort();
            return classDecl;
        }

        // Visit each leading annotation in lock step
        for (let i = 0; i < classDecl.leadingAnnotations.length; i++) {
            await this.visit(classDecl.leadingAnnotations[i], otherClassDecl.leadingAnnotations[i]);
            if (!this.match) return classDecl;
        }

        // Compare modifiers
        if (classDecl.modifiers.length !== otherClassDecl.modifiers.length) {
            this.abort();
            return classDecl;
        }

        // Visit each modifier in lock step
        for (let i = 0; i < classDecl.modifiers.length; i++) {
            await this.visit(classDecl.modifiers[i], otherClassDecl.modifiers[i]);
            if (!this.match) return classDecl;
        }

        // Visit class kind
        await this.visit(classDecl.classKind, otherClassDecl.classKind);
        if (!this.match) return classDecl;

        // Visit name
        await this.visit(classDecl.name, otherClassDecl.name);
        if (!this.match) return classDecl;

        // Compare type parameters presence
        if ((classDecl.typeParameters !== undefined) !== (otherClassDecl.typeParameters !== undefined)) {
            this.abort();
            return classDecl;
        }

        // Visit type parameters if they exist
        if (classDecl.typeParameters && otherClassDecl.typeParameters) {
            if (classDecl.typeParameters.elements.length !== otherClassDecl.typeParameters.elements.length) {
                this.abort();
                return classDecl;
            }

            // Visit each type parameter in lock step
            for (let i = 0; i < classDecl.typeParameters.elements.length; i++) {
                await this.visit(classDecl.typeParameters.elements[i].element, otherClassDecl.typeParameters.elements[i].element);
                if (!this.match) return classDecl;
            }
        }

        // Compare primary constructor presence
        if ((classDecl.primaryConstructor !== undefined) !== (otherClassDecl.primaryConstructor !== undefined)) {
            this.abort();
            return classDecl;
        }

        // Visit primary constructor if it exists
        if (classDecl.primaryConstructor && otherClassDecl.primaryConstructor) {
            if (classDecl.primaryConstructor.elements.length !== otherClassDecl.primaryConstructor.elements.length) {
                this.abort();
                return classDecl;
            }

            // Visit each primary constructor element in lock step
            for (let i = 0; i < classDecl.primaryConstructor.elements.length; i++) {
                await this.visit(classDecl.primaryConstructor.elements[i].element, otherClassDecl.primaryConstructor.elements[i].element);
                if (!this.match) return classDecl;
            }
        }

        // Compare extends presence
        if ((classDecl.extends !== undefined) !== (otherClassDecl.extends !== undefined)) {
            this.abort();
            return classDecl;
        }

        // Visit extends if it exists
        if (classDecl.extends && otherClassDecl.extends) {
            await this.visit(classDecl.extends.element, otherClassDecl.extends.element);
            if (!this.match) return classDecl;
        }

        // Compare implements presence
        if ((classDecl.implements !== undefined) !== (otherClassDecl.implements !== undefined)) {
            this.abort();
            return classDecl;
        }

        // Visit implements if it exists
        if (classDecl.implements && otherClassDecl.implements) {
            if (classDecl.implements.elements.length !== otherClassDecl.implements.elements.length) {
                this.abort();
                return classDecl;
            }

            // Visit each implements element in lock step
            for (let i = 0; i < classDecl.implements.elements.length; i++) {
                await this.visit(classDecl.implements.elements[i].element, otherClassDecl.implements.elements[i].element);
                if (!this.match) return classDecl;
            }
        }

        // Compare permitting presence
        if ((classDecl.permitting !== undefined) !== (otherClassDecl.permitting !== undefined)) {
            this.abort();
            return classDecl;
        }

        // Visit permitting if it exists
        if (classDecl.permitting && otherClassDecl.permitting) {
            if (classDecl.permitting.elements.length !== otherClassDecl.permitting.elements.length) {
                this.abort();
                return classDecl;
            }

            // Visit each permitting element in lock step
            for (let i = 0; i < classDecl.permitting.elements.length; i++) {
                await this.visit(classDecl.permitting.elements[i].element, otherClassDecl.permitting.elements[i].element);
                if (!this.match) return classDecl;
            }
        }

        // Visit body
        await this.visit(classDecl.body, otherClassDecl.body);

        return classDecl;
    }

    /**
     * Overrides the visitClassDeclarationKind method to compare class declaration kinds.
     * 
     * @param kind The class declaration kind to visit
     * @param other The other class declaration kind to compare with
     * @returns The visited class declaration kind, or undefined if the visit was aborted
     */
    override async visitClassDeclarationKind(kind: J.ClassDeclaration.Kind, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.ClassDeclarationKind) {
            this.abort();
            return kind;
        }

        const otherKind = other as J.ClassDeclaration.Kind;

        // Compare annotations
        if (kind.annotations.length !== otherKind.annotations.length) {
            this.abort();
            return kind;
        }

        // Visit each annotation in lock step
        for (let i = 0; i < kind.annotations.length; i++) {
            await this.visit(kind.annotations[i], otherKind.annotations[i]);
            if (!this.match) return kind;
        }

        return kind;
    }

    /**
     * Overrides the visitCompilationUnit method to compare compilation units.
     * 
     * @param compilationUnit The compilation unit to visit
     * @param other The other compilation unit to compare with
     * @returns The visited compilation unit, or undefined if the visit was aborted
     */
    override async visitCompilationUnit(compilationUnit: J.CompilationUnit, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.CompilationUnit) {
            this.abort();
            return compilationUnit;
        }

        const otherCompilationUnit = other as J.CompilationUnit;

        // Compare package declaration presence
        if ((compilationUnit.packageDeclaration !== undefined) !== (otherCompilationUnit.packageDeclaration !== undefined)) {
            this.abort();
            return compilationUnit;
        }

        // Visit package declaration if it exists
        if (compilationUnit.packageDeclaration && otherCompilationUnit.packageDeclaration) {
            await this.visit(compilationUnit.packageDeclaration.element, otherCompilationUnit.packageDeclaration.element);
            if (!this.match) return compilationUnit;
        }

        // Compare imports
        if (compilationUnit.imports.length !== otherCompilationUnit.imports.length) {
            this.abort();
            return compilationUnit;
        }

        // Visit each import in lock step
        for (let i = 0; i < compilationUnit.imports.length; i++) {
            await this.visit(compilationUnit.imports[i].element, otherCompilationUnit.imports[i].element);
            if (!this.match) return compilationUnit;
        }

        // Compare classes
        if (compilationUnit.classes.length !== otherCompilationUnit.classes.length) {
            this.abort();
            return compilationUnit;
        }

        // Visit each class in lock step
        for (let i = 0; i < compilationUnit.classes.length; i++) {
            await this.visit(compilationUnit.classes[i], otherCompilationUnit.classes[i]);
            if (!this.match) return compilationUnit;
        }

        return compilationUnit;
    }

    /**
     * Overrides the visitContinue method to compare continue statements.
     * 
     * @param continueStatement The continue statement to visit
     * @param other The other continue statement to compare with
     * @returns The visited continue statement, or undefined if the visit was aborted
     */
    override async visitContinue(continueStatement: J.Continue, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Continue) {
            this.abort();
            return continueStatement;
        }

        const otherContinue = other as J.Continue;

        // Compare label presence
        if ((continueStatement.label !== undefined) !== (otherContinue.label !== undefined)) {
            this.abort();
            return continueStatement;
        }

        // Visit label if it exists
        if (continueStatement.label && otherContinue.label) {
            await this.visit(continueStatement.label, otherContinue.label);
        }

        return continueStatement;
    }

    /**
     * Overrides the visitControlParentheses method to compare control parentheses.
     * 
     * @param controlParens The control parentheses to visit
     * @param other The other control parentheses to compare with
     * @returns The visited control parentheses, or undefined if the visit was aborted
     */
    override async visitControlParentheses<T extends J>(controlParens: J.ControlParentheses<T>, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.ControlParentheses) {
            this.abort();
            return controlParens;
        }

        const otherControlParens = other as J.ControlParentheses<J>;

        // Visit tree
        await this.visit(controlParens.tree.element, otherControlParens.tree.element);

        return controlParens;
    }

    /**
     * Overrides the visitDeconstructionPattern method to compare deconstruction patterns.
     * 
     * @param pattern The deconstruction pattern to visit
     * @param other The other deconstruction pattern to compare with
     * @returns The visited deconstruction pattern, or undefined if the visit was aborted
     */
    override async visitDeconstructionPattern(pattern: J.DeconstructionPattern, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.DeconstructionPattern) {
            this.abort();
            return pattern;
        }

        const otherPattern = other as J.DeconstructionPattern;

        // Visit deconstructor
        await this.visit(pattern.deconstructor, otherPattern.deconstructor);
        if (!this.match) return pattern;

        // Compare nested elements
        if (pattern.nested.elements.length !== otherPattern.nested.elements.length) {
            this.abort();
            return pattern;
        }

        // Visit each nested element in lock step
        for (let i = 0; i < pattern.nested.elements.length; i++) {
            await this.visit(pattern.nested.elements[i].element, otherPattern.nested.elements[i].element);
            if (!this.match) return pattern;
        }

        return pattern;
    }

    /**
     * Overrides the visitDoWhileLoop method to compare do-while loops.
     * 
     * @param doWhileLoop The do-while loop to visit
     * @param other The other do-while loop to compare with
     * @returns The visited do-while loop, or undefined if the visit was aborted
     */
    override async visitDoWhileLoop(doWhileLoop: J.DoWhileLoop, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.DoWhileLoop) {
            this.abort();
            return doWhileLoop;
        }

        const otherDoWhileLoop = other as J.DoWhileLoop;

        // Visit body
        await this.visit(doWhileLoop.body.element, otherDoWhileLoop.body.element);
        if (!this.match) return doWhileLoop;

        // Visit while condition
        await this.visit(doWhileLoop.whileCondition.element, otherDoWhileLoop.whileCondition.element);

        return doWhileLoop;
    }

    /**
     * Overrides the visitEmpty method to compare empty statements.
     * 
     * @param empty The empty statement to visit
     * @param other The other empty statement to compare with
     * @returns The visited empty statement, or undefined if the visit was aborted
     */
    override async visitEmpty(empty: J.Empty, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Empty) {
            this.abort();
            return empty;
        }

        // Empty statements have no properties to compare, so we just check the kind

        return empty;
    }

    /**
     * Overrides the visitEnumValue method to compare enum values.
     * 
     * @param enumValue The enum value to visit
     * @param other The other enum value to compare with
     * @returns The visited enum value, or undefined if the visit was aborted
     */
    override async visitEnumValue(enumValue: J.EnumValue, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.EnumValue) {
            this.abort();
            return enumValue;
        }

        const otherEnumValue = other as J.EnumValue;

        // Compare annotations
        if (enumValue.annotations.length !== otherEnumValue.annotations.length) {
            this.abort();
            return enumValue;
        }

        // Visit each annotation in lock step
        for (let i = 0; i < enumValue.annotations.length; i++) {
            await this.visit(enumValue.annotations[i], otherEnumValue.annotations[i]);
            if (!this.match) return enumValue;
        }

        // Visit name
        await this.visit(enumValue.name, otherEnumValue.name);
        if (!this.match) return enumValue;

        // Compare initializer presence
        if ((enumValue.initializer !== undefined) !== (otherEnumValue.initializer !== undefined)) {
            this.abort();
            return enumValue;
        }

        // Visit initializer if it exists
        if (enumValue.initializer && otherEnumValue.initializer) {
            await this.visit(enumValue.initializer, otherEnumValue.initializer);
        }

        return enumValue;
    }

    /**
     * Overrides the visitEnumValueSet method to compare enum value sets.
     * 
     * @param enumValueSet The enum value set to visit
     * @param other The other enum value set to compare with
     * @returns The visited enum value set, or undefined if the visit was aborted
     */
    override async visitEnumValueSet(enumValueSet: J.EnumValueSet, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.EnumValueSet) {
            this.abort();
            return enumValueSet;
        }

        const otherEnumValueSet = other as J.EnumValueSet;

        // Compare enums
        if (enumValueSet.enums.length !== otherEnumValueSet.enums.length) {
            this.abort();
            return enumValueSet;
        }

        // Visit each enum in lock step
        for (let i = 0; i < enumValueSet.enums.length; i++) {
            await this.visit(enumValueSet.enums[i].element, otherEnumValueSet.enums[i].element);
            if (!this.match) return enumValueSet;
        }

        return enumValueSet;
    }

    /**
     * Overrides the visitErroneous method to compare erroneous nodes.
     * 
     * @param erroneous The erroneous node to visit
     * @param other The other erroneous node to compare with
     * @returns The visited erroneous node, or undefined if the visit was aborted
     */
    override async visitErroneous(erroneous: J.Erroneous, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Erroneous) {
            this.abort();
            return erroneous;
        }

        const otherErroneous = other as J.Erroneous;

        // Compare text
        if (erroneous.text !== otherErroneous.text) {
            this.abort();
            return erroneous;
        }

        return erroneous;
    }

    /**
     * Overrides the visitFieldAccess method to compare field access expressions.
     * 
     * @param fieldAccess The field access expression to visit
     * @param other The other field access expression to compare with
     * @returns The visited field access expression, or undefined if the visit was aborted
     */
    override async visitFieldAccess(fieldAccess: J.FieldAccess, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.FieldAccess) {
            this.abort();
            return fieldAccess;
        }

        const otherFieldAccess = other as J.FieldAccess;

        // Visit target
        await this.visit(fieldAccess.target, otherFieldAccess.target);
        if (!this.match) return fieldAccess;

        // Visit name
        await this.visit(fieldAccess.name.element, otherFieldAccess.name.element);
        if (!this.match) return fieldAccess;

        return fieldAccess;
    }

    /**
     * Overrides the visitForEachLoop method to compare for-each loops.
     * 
     * @param forEachLoop The for-each loop to visit
     * @param other The other for-each loop to compare with
     * @returns The visited for-each loop, or undefined if the visit was aborted
     */
    override async visitForEachLoop(forEachLoop: J.ForEachLoop, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.ForEachLoop) {
            this.abort();
            return forEachLoop;
        }

        const otherForEachLoop = other as J.ForEachLoop;

        // Visit control
        await this.visit(forEachLoop.control, otherForEachLoop.control);
        if (!this.match) return forEachLoop;

        // Visit body
        await this.visit(forEachLoop.body.element, otherForEachLoop.body.element);
        if (!this.match) return forEachLoop;

        return forEachLoop;
    }

    /**
     * Overrides the visitForEachLoopControl method to compare for-each loop controls.
     * 
     * @param control The for-each loop control to visit
     * @param other The other for-each loop control to compare with
     * @returns The visited for-each loop control, or undefined if the visit was aborted
     */
    override async visitForEachLoopControl(control: J.ForEachLoop.Control, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.ForEachLoopControl) {
            this.abort();
            return control;
        }

        const otherControl = other as J.ForEachLoop.Control;

        // Visit variable
        await this.visit(control.variable.element, otherControl.variable.element);
        if (!this.match) return control;

        // Visit iterable
        await this.visit(control.iterable.element, otherControl.iterable.element);
        if (!this.match) return control;

        return control;
    }

    /**
     * Overrides the visitForLoop method to compare for loops.
     * 
     * @param forLoop The for loop to visit
     * @param other The other for loop to compare with
     * @returns The visited for loop, or undefined if the visit was aborted
     */
    override async visitForLoop(forLoop: J.ForLoop, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.ForLoop) {
            this.abort();
            return forLoop;
        }

        const otherForLoop = other as J.ForLoop;

        // Visit control
        await this.visit(forLoop.control, otherForLoop.control);
        if (!this.match) return forLoop;

        // Visit body
        await this.visit(forLoop.body.element, otherForLoop.body.element);
        if (!this.match) return forLoop;

        return forLoop;
    }

    /**
     * Overrides the visitForLoopControl method to compare for loop controls.
     * 
     * @param control The for loop control to visit
     * @param other The other for loop control to compare with
     * @returns The visited for loop control, or undefined if the visit was aborted
     */
    override async visitForLoopControl(control: J.ForLoop.Control, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.ForLoopControl) {
            this.abort();
            return control;
        }

        const otherControl = other as J.ForLoop.Control;

        // Compare init statements
        if (control.init.length !== otherControl.init.length) {
            this.abort();
            return control;
        }

        // Visit each init statement in lock step
        for (let i = 0; i < control.init.length; i++) {
            await this.visit(control.init[i].element, otherControl.init[i].element);
            if (!this.match) return control;
        }

        // Compare condition
        if ((control.condition === undefined) !== (otherControl.condition === undefined)) {
            this.abort();
            return control;
        }

        // Visit condition if present
        if (control.condition && otherControl.condition) {
            await this.visit(control.condition.element, otherControl.condition.element);
            if (!this.match) return control;
        }

        // Compare update statements
        if (control.update.length !== otherControl.update.length) {
            this.abort();
            return control;
        }

        // Visit each update statement in lock step
        for (let i = 0; i < control.update.length; i++) {
            await this.visit(control.update[i].element, otherControl.update[i].element);
            if (!this.match) return control;
        }

        return control;
    }

    /**
     * Overrides the visitIf method to compare if statements.
     * 
     * @param ifStatement The if statement to visit
     * @param other The other if statement to compare with
     * @returns The visited if statement, or undefined if the visit was aborted
     */
    override async visitIf(ifStatement: J.If, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.If) {
            this.abort();
            return ifStatement;
        }

        const otherIfStatement = other as J.If;

        // Visit condition
        await this.visit(ifStatement.ifCondition, otherIfStatement.ifCondition);
        if (!this.match) return ifStatement;

        // Visit then part
        await this.visit(ifStatement.thenPart.element, otherIfStatement.thenPart.element);
        if (!this.match) return ifStatement;

        // Compare else part
        if ((ifStatement.elsePart === undefined) !== (otherIfStatement.elsePart === undefined)) {
            this.abort();
            return ifStatement;
        }

        // Visit else part if present
        if (ifStatement.elsePart && otherIfStatement.elsePart) {
            await this.visit(ifStatement.elsePart, otherIfStatement.elsePart);
            if (!this.match) return ifStatement;
        }

        return ifStatement;
    }

    /**
     * Overrides the visitElse method to compare else statements.
     * 
     * @param elseStatement The else statement to visit
     * @param other The other else statement to compare with
     * @returns The visited else statement, or undefined if the visit was aborted
     */
    override async visitElse(elseStatement: J.If.Else, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.IfElse) {
            this.abort();
            return elseStatement;
        }

        const otherElseStatement = other as J.If.Else;

        // Visit body
        await this.visit(elseStatement.body.element, otherElseStatement.body.element);
        if (!this.match) return elseStatement;

        return elseStatement;
    }

    /**
     * Overrides the visitImport method to compare import statements.
     * 
     * @param importStatement The import statement to visit
     * @param other The other import statement to compare with
     * @returns The visited import statement, or undefined if the visit was aborted
     */
    override async visitImport(importStatement: J.Import, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Import) {
            this.abort();
            return importStatement;
        }

        const otherImportStatement = other as J.Import;

        // Compare static
        if (importStatement.static.element !== otherImportStatement.static.element) {
            this.abort();
            return importStatement;
        }

        // Visit qualid
        await this.visit(importStatement.qualid, otherImportStatement.qualid);
        if (!this.match) return importStatement;

        // Compare alias
        if ((importStatement.alias === undefined) !== (otherImportStatement.alias === undefined)) {
            this.abort();
            return importStatement;
        }

        // Visit alias if present
        if (importStatement.alias && otherImportStatement.alias) {
            await this.visit(importStatement.alias.element, otherImportStatement.alias.element);
            if (!this.match) return importStatement;
        }

        return importStatement;
    }

    /**
     * Overrides the visitInstanceOf method to compare instanceof expressions.
     * 
     * @param instanceOf The instanceof expression to visit
     * @param other The other instanceof expression to compare with
     * @returns The visited instanceof expression, or undefined if the visit was aborted
     */
    override async visitInstanceOf(instanceOf: J.InstanceOf, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.InstanceOf) {
            this.abort();
            return instanceOf;
        }

        const otherInstanceOf = other as J.InstanceOf;

        // Visit expression
        await this.visit(instanceOf.expression.element, otherInstanceOf.expression.element);
        if (!this.match) return instanceOf;

        // Visit class
        await this.visit(instanceOf.class, otherInstanceOf.class);
        if (!this.match) return instanceOf;

        // Compare pattern
        if ((instanceOf.pattern === undefined) !== (otherInstanceOf.pattern === undefined)) {
            this.abort();
            return instanceOf;
        }

        // Visit pattern if present
        if (instanceOf.pattern && otherInstanceOf.pattern) {
            await this.visit(instanceOf.pattern, otherInstanceOf.pattern);
            if (!this.match) return instanceOf;
        }

        // Compare modifier
        if ((instanceOf.modifier === undefined) !== (otherInstanceOf.modifier === undefined)) {
            this.abort();
            return instanceOf;
        }

        // Visit modifier if present
        if (instanceOf.modifier && otherInstanceOf.modifier) {
            await this.visit(instanceOf.modifier, otherInstanceOf.modifier);
            if (!this.match) return instanceOf;
        }

        return instanceOf;
    }

    /**
     * Overrides the visitIntersectionType method to compare intersection types.
     * 
     * @param intersectionType The intersection type to visit
     * @param other The other intersection type to compare with
     * @returns The visited intersection type, or undefined if the visit was aborted
     */
    override async visitIntersectionType(intersectionType: J.IntersectionType, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.IntersectionType) {
            this.abort();
            return intersectionType;
        }

        const otherIntersectionType = other as J.IntersectionType;

        // Compare bounds
        if (intersectionType.bounds.elements.length !== otherIntersectionType.bounds.elements.length) {
            this.abort();
            return intersectionType;
        }

        // Visit each bound in lock step
        for (let i = 0; i < intersectionType.bounds.elements.length; i++) {
            await this.visit(intersectionType.bounds.elements[i].element, otherIntersectionType.bounds.elements[i].element);
            if (!this.match) return intersectionType;
        }

        return intersectionType;
    }

    /**
     * Overrides the visitLabel method to compare label statements.
     * 
     * @param label The label statement to visit
     * @param other The other label statement to compare with
     * @returns The visited label statement, or undefined if the visit was aborted
     */
    override async visitLabel(label: J.Label, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Label) {
            this.abort();
            return label;
        }

        const otherLabel = other as J.Label;

        // Visit label identifier
        await this.visit(label.label.element, otherLabel.label.element);
        if (!this.match) return label;

        // Visit statement
        await this.visit(label.statement, otherLabel.statement);
        if (!this.match) return label;

        return label;
    }

    /**
     * Overrides the visitLambda method to compare lambda expressions.
     * 
     * @param lambda The lambda expression to visit
     * @param other The other lambda expression to compare with
     * @returns The visited lambda expression, or undefined if the visit was aborted
     */
    override async visitLambda(lambda: J.Lambda, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Lambda) {
            this.abort();
            return lambda;
        }

        const otherLambda = other as J.Lambda;

        // Visit parameters
        await this.visit(lambda.parameters, otherLambda.parameters);
        if (!this.match) return lambda;

        // Visit body
        await this.visit(lambda.body, otherLambda.body);
        if (!this.match) return lambda;

        return lambda;
    }

    /**
     * Overrides the visitLambdaParameters method to compare lambda parameters.
     * 
     * @param parameters The lambda parameters to visit
     * @param other The other lambda parameters to compare with
     * @returns The visited lambda parameters, or undefined if the visit was aborted
     */
    override async visitLambdaParameters(parameters: J.Lambda.Parameters, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.LambdaParameters) {
            this.abort();
            return parameters;
        }

        const otherParameters = other as J.Lambda.Parameters;

        // Compare parenthesized
        if (parameters.parenthesized !== otherParameters.parenthesized) {
            this.abort();
            return parameters;
        }

        // Compare parameters
        if (parameters.parameters.length !== otherParameters.parameters.length) {
            this.abort();
            return parameters;
        }

        // Visit each parameter in lock step
        for (let i = 0; i < parameters.parameters.length; i++) {
            await this.visit(parameters.parameters[i].element, otherParameters.parameters[i].element);
            if (!this.match) return parameters;
        }

        return parameters;
    }

    /**
     * Overrides the visitMemberReference method to compare member references.
     * 
     * @param memberReference The member reference to visit
     * @param other The other member reference to compare with
     * @returns The visited member reference, or undefined if the visit was aborted
     */
    override async visitMemberReference(memberReference: J.MemberReference, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.MemberReference) {
            this.abort();
            return memberReference;
        }

        const otherMemberReference = other as J.MemberReference;

        // Visit containing
        await this.visit(memberReference.containing.element, otherMemberReference.containing.element);
        if (!this.match) return memberReference;

        // Compare typeParameters
        if ((memberReference.typeParameters === undefined) !== (otherMemberReference.typeParameters === undefined)) {
            this.abort();
            return memberReference;
        }

        // Visit typeParameters if present
        if (memberReference.typeParameters && otherMemberReference.typeParameters) {
            if (memberReference.typeParameters.elements.length !== otherMemberReference.typeParameters.elements.length) {
                this.abort();
                return memberReference;
            }

            // Visit each type parameter in lock step
            for (let i = 0; i < memberReference.typeParameters.elements.length; i++) {
                await this.visit(memberReference.typeParameters.elements[i].element, otherMemberReference.typeParameters.elements[i].element);
                if (!this.match) return memberReference;
            }
        }

        // Visit reference
        await this.visit(memberReference.reference.element, otherMemberReference.reference.element);
        if (!this.match) return memberReference;

        return memberReference;
    }

    /**
     * Overrides the visitMethodDeclaration method to compare method declarations.
     * 
     * @param methodDeclaration The method declaration to visit
     * @param other The other method declaration to compare with
     * @returns The visited method declaration, or undefined if the visit was aborted
     */
    override async visitMethodDeclaration(methodDeclaration: J.MethodDeclaration, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.MethodDeclaration) {
            this.abort();
            return methodDeclaration;
        }

        const otherMethodDeclaration = other as J.MethodDeclaration;

        // Compare leadingAnnotations
        if (methodDeclaration.leadingAnnotations.length !== otherMethodDeclaration.leadingAnnotations.length) {
            this.abort();
            return methodDeclaration;
        }

        // Visit each leading annotation in lock step
        for (let i = 0; i < methodDeclaration.leadingAnnotations.length; i++) {
            await this.visit(methodDeclaration.leadingAnnotations[i], otherMethodDeclaration.leadingAnnotations[i]);
            if (!this.match) return methodDeclaration;
        }

        // Compare modifiers
        if (methodDeclaration.modifiers.length !== otherMethodDeclaration.modifiers.length) {
            this.abort();
            return methodDeclaration;
        }

        // Visit each modifier in lock step
        for (let i = 0; i < methodDeclaration.modifiers.length; i++) {
            await this.visit(methodDeclaration.modifiers[i], otherMethodDeclaration.modifiers[i]);
            if (!this.match) return methodDeclaration;
        }

        // Compare typeParameters
        if ((methodDeclaration.typeParameters === undefined) !== (otherMethodDeclaration.typeParameters === undefined)) {
            this.abort();
            return methodDeclaration;
        }

        // Visit typeParameters if present
        if (methodDeclaration.typeParameters && otherMethodDeclaration.typeParameters) {
            await this.visit(methodDeclaration.typeParameters, otherMethodDeclaration.typeParameters);
            if (!this.match) return methodDeclaration;
        }

        // Compare returnTypeExpression
        if ((methodDeclaration.returnTypeExpression === undefined) !== (otherMethodDeclaration.returnTypeExpression === undefined)) {
            this.abort();
            return methodDeclaration;
        }

        // Visit returnTypeExpression if present
        if (methodDeclaration.returnTypeExpression && otherMethodDeclaration.returnTypeExpression) {
            await this.visit(methodDeclaration.returnTypeExpression, otherMethodDeclaration.returnTypeExpression);
            if (!this.match) return methodDeclaration;
        }

        // Compare nameAnnotations
        if (methodDeclaration.nameAnnotations.length !== otherMethodDeclaration.nameAnnotations.length) {
            this.abort();
            return methodDeclaration;
        }

        // Visit each name annotation in lock step
        for (let i = 0; i < methodDeclaration.nameAnnotations.length; i++) {
            await this.visit(methodDeclaration.nameAnnotations[i], otherMethodDeclaration.nameAnnotations[i]);
            if (!this.match) return methodDeclaration;
        }

        // Visit name
        await this.visit(methodDeclaration.name, otherMethodDeclaration.name);
        if (!this.match) return methodDeclaration;

        // Compare parameters
        if (methodDeclaration.parameters.elements.length !== otherMethodDeclaration.parameters.elements.length) {
            this.abort();
            return methodDeclaration;
        }

        // Visit each parameter in lock step
        for (let i = 0; i < methodDeclaration.parameters.elements.length; i++) {
            await this.visit(methodDeclaration.parameters.elements[i].element, otherMethodDeclaration.parameters.elements[i].element);
            if (!this.match) return methodDeclaration;
        }

        // Compare throws
        if ((methodDeclaration.throws === undefined) !== (otherMethodDeclaration.throws === undefined)) {
            this.abort();
            return methodDeclaration;
        }

        // Visit throws if present
        if (methodDeclaration.throws && otherMethodDeclaration.throws) {
            if (methodDeclaration.throws.elements.length !== otherMethodDeclaration.throws.elements.length) {
                this.abort();
                return methodDeclaration;
            }

            // Visit each throw in lock step
            for (let i = 0; i < methodDeclaration.throws.elements.length; i++) {
                await this.visit(methodDeclaration.throws.elements[i].element, otherMethodDeclaration.throws.elements[i].element);
                if (!this.match) return methodDeclaration;
            }
        }

        // Compare body
        if ((methodDeclaration.body === undefined) !== (otherMethodDeclaration.body === undefined)) {
            this.abort();
            return methodDeclaration;
        }

        // Visit body if present
        if (methodDeclaration.body && otherMethodDeclaration.body) {
            await this.visit(methodDeclaration.body, otherMethodDeclaration.body);
            if (!this.match) return methodDeclaration;
        }

        // Compare defaultValue
        if ((methodDeclaration.defaultValue === undefined) !== (otherMethodDeclaration.defaultValue === undefined)) {
            this.abort();
            return methodDeclaration;
        }

        // Visit defaultValue if present
        if (methodDeclaration.defaultValue && otherMethodDeclaration.defaultValue) {
            await this.visit(methodDeclaration.defaultValue.element, otherMethodDeclaration.defaultValue.element);
            if (!this.match) return methodDeclaration;
        }

        return methodDeclaration;
    }

    /**
     * Overrides the visitMethodInvocation method to compare method invocations.
     * 
     * @param methodInvocation The method invocation to visit
     * @param other The other method invocation to compare with
     * @returns The visited method invocation, or undefined if the visit was aborted
     */
    override async visitMethodInvocation(methodInvocation: J.MethodInvocation, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.MethodInvocation) {
            this.abort();
            return methodInvocation;
        }

        const otherMethodInvocation = other as J.MethodInvocation;

        // Compare select
        if ((methodInvocation.select === undefined) !== (otherMethodInvocation.select === undefined)) {
            this.abort();
            return methodInvocation;
        }

        // Visit select if present
        if (methodInvocation.select && otherMethodInvocation.select) {
            await this.visit(methodInvocation.select.element, otherMethodInvocation.select.element);
            if (!this.match) return methodInvocation;
        }

        // Compare typeParameters
        if ((methodInvocation.typeParameters === undefined) !== (otherMethodInvocation.typeParameters === undefined)) {
            this.abort();
            return methodInvocation;
        }

        // Visit typeParameters if present
        if (methodInvocation.typeParameters && otherMethodInvocation.typeParameters) {
            if (methodInvocation.typeParameters.elements.length !== otherMethodInvocation.typeParameters.elements.length) {
                this.abort();
                return methodInvocation;
            }

            // Visit each type parameter in lock step
            for (let i = 0; i < methodInvocation.typeParameters.elements.length; i++) {
                await this.visit(methodInvocation.typeParameters.elements[i].element, otherMethodInvocation.typeParameters.elements[i].element);
                if (!this.match) return methodInvocation;
            }
        }

        // Visit name
        await this.visit(methodInvocation.name, otherMethodInvocation.name);
        if (!this.match) return methodInvocation;

        // Compare arguments
        if (methodInvocation.arguments.elements.length !== otherMethodInvocation.arguments.elements.length) {
            this.abort();
            return methodInvocation;
        }

        // Visit each argument in lock step
        for (let i = 0; i < methodInvocation.arguments.elements.length; i++) {
            await this.visit(methodInvocation.arguments.elements[i].element, otherMethodInvocation.arguments.elements[i].element);
            if (!this.match) return methodInvocation;
        }

        return methodInvocation;
    }

    /**
     * Overrides the visitModifier method to compare modifiers.
     * 
     * @param modifier The modifier to visit
     * @param other The other modifier to compare with
     * @returns The visited modifier, or undefined if the visit was aborted
     */
    override async visitModifier(modifier: J.Modifier, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Modifier) {
            this.abort();
            return modifier;
        }

        const otherModifier = other as J.Modifier;

        // Compare keyword
        if (modifier.keyword !== otherModifier.keyword) {
            this.abort();
            return modifier;
        }

        // Compare type
        if (modifier.type !== otherModifier.type) {
            this.abort();
            return modifier;
        }

        // Compare annotations
        if (modifier.annotations.length !== otherModifier.annotations.length) {
            this.abort();
            return modifier;
        }

        // Visit each annotation in lock step
        for (let i = 0; i < modifier.annotations.length; i++) {
            await this.visit(modifier.annotations[i], otherModifier.annotations[i]);
            if (!this.match) return modifier;
        }

        return modifier;
    }

    /**
     * Overrides the visitMultiCatch method to compare multi-catch expressions.
     * 
     * @param multiCatch The multi-catch expression to visit
     * @param other The other multi-catch expression to compare with
     * @returns The visited multi-catch expression, or undefined if the visit was aborted
     */
    override async visitMultiCatch(multiCatch: J.MultiCatch, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.MultiCatch) {
            this.abort();
            return multiCatch;
        }

        const otherMultiCatch = other as J.MultiCatch;

        // Compare alternatives
        if (multiCatch.alternatives.length !== otherMultiCatch.alternatives.length) {
            this.abort();
            return multiCatch;
        }

        // Visit each alternative in lock step
        for (let i = 0; i < multiCatch.alternatives.length; i++) {
            await this.visit(multiCatch.alternatives[i].element, otherMultiCatch.alternatives[i].element);
            if (!this.match) return multiCatch;
        }

        return multiCatch;
    }

    /**
     * Overrides the visitNewArray method to compare new array expressions.
     * 
     * @param newArray The new array expression to visit
     * @param other The other new array expression to compare with
     * @returns The visited new array expression, or undefined if the visit was aborted
     */
    override async visitNewArray(newArray: J.NewArray, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.NewArray) {
            this.abort();
            return newArray;
        }

        const otherNewArray = other as J.NewArray;

        // Compare typeExpression
        if ((newArray.typeExpression === undefined) !== (otherNewArray.typeExpression === undefined)) {
            this.abort();
            return newArray;
        }

        // Visit typeExpression if present
        if (newArray.typeExpression && otherNewArray.typeExpression) {
            await this.visit(newArray.typeExpression, otherNewArray.typeExpression);
            if (!this.match) return newArray;
        }

        // Compare dimensions
        if (newArray.dimensions.length !== otherNewArray.dimensions.length) {
            this.abort();
            return newArray;
        }

        // Visit each dimension in lock step
        for (let i = 0; i < newArray.dimensions.length; i++) {
            await this.visit(newArray.dimensions[i], otherNewArray.dimensions[i]);
            if (!this.match) return newArray;
        }

        // Compare initializer
        if ((newArray.initializer === undefined) !== (otherNewArray.initializer === undefined)) {
            this.abort();
            return newArray;
        }

        // Visit initializer if present
        if (newArray.initializer && otherNewArray.initializer) {
            if (newArray.initializer.elements.length !== otherNewArray.initializer.elements.length) {
                this.abort();
                return newArray;
            }

            // Visit each initializer element in lock step
            for (let i = 0; i < newArray.initializer.elements.length; i++) {
                await this.visit(newArray.initializer.elements[i].element, otherNewArray.initializer.elements[i].element);
                if (!this.match) return newArray;
            }
        }

        return newArray;
    }

    /**
     * Overrides the visitNewClass method to compare new class expressions.
     * 
     * @param newClass The new class expression to visit
     * @param other The other new class expression to compare with
     * @returns The visited new class expression, or undefined if the visit was aborted
     */
    override async visitNewClass(newClass: J.NewClass, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.NewClass) {
            this.abort();
            return newClass;
        }

        const otherNewClass = other as J.NewClass;

        // Compare enclosing
        if ((newClass.enclosing === undefined) !== (otherNewClass.enclosing === undefined)) {
            this.abort();
            return newClass;
        }

        // Visit enclosing if present
        if (newClass.enclosing && otherNewClass.enclosing) {
            await this.visit(newClass.enclosing.element, otherNewClass.enclosing.element);
            if (!this.match) return newClass;
        }

        // Compare class
        if ((newClass.class === undefined) !== (otherNewClass.class === undefined)) {
            this.abort();
            return newClass;
        }

        // Visit class if present
        if (newClass.class && otherNewClass.class) {
            await this.visit(newClass.class, otherNewClass.class);
            if (!this.match) return newClass;
        }

        // Compare arguments
        if (newClass.arguments.elements.length !== otherNewClass.arguments.elements.length) {
            this.abort();
            return newClass;
        }

        // Visit each argument in lock step
        for (let i = 0; i < newClass.arguments.elements.length; i++) {
            await this.visit(newClass.arguments.elements[i].element, otherNewClass.arguments.elements[i].element);
            if (!this.match) return newClass;
        }

        // Compare body
        if ((newClass.body === undefined) !== (otherNewClass.body === undefined)) {
            this.abort();
            return newClass;
        }

        // Visit body if present
        if (newClass.body && otherNewClass.body) {
            await this.visit(newClass.body, otherNewClass.body);
            if (!this.match) return newClass;
        }

        return newClass;
    }

    /**
     * Overrides the visitNullableType method to compare nullable types.
     * 
     * @param nullableType The nullable type to visit
     * @param other The other nullable type to compare with
     * @returns The visited nullable type, or undefined if the visit was aborted
     */
    override async visitNullableType(nullableType: J.NullableType, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.NullableType) {
            this.abort();
            return nullableType;
        }

        const otherNullableType = other as J.NullableType;

        // Compare annotations
        if (nullableType.annotations.length !== otherNullableType.annotations.length) {
            this.abort();
            return nullableType;
        }

        // Visit each annotation in lock step
        for (let i = 0; i < nullableType.annotations.length; i++) {
            await this.visit(nullableType.annotations[i], otherNullableType.annotations[i]);
            if (!this.match) return nullableType;
        }

        // Visit typeTree
        await this.visit(nullableType.typeTree.element, otherNullableType.typeTree.element);
        if (!this.match) return nullableType;

        return nullableType;
    }

    /**
     * Overrides the visitPackage method to compare package declarations.
     * 
     * @param packageDeclaration The package declaration to visit
     * @param other The other package declaration to compare with
     * @returns The visited package declaration, or undefined if the visit was aborted
     */
    override async visitPackage(packageDeclaration: J.Package, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Package) {
            this.abort();
            return packageDeclaration;
        }

        const otherPackageDeclaration = other as J.Package;

        // Visit expression
        await this.visit(packageDeclaration.expression, otherPackageDeclaration.expression);
        if (!this.match) return packageDeclaration;

        // Compare annotations
        if ((packageDeclaration.annotations === undefined) !== (otherPackageDeclaration.annotations === undefined)) {
            this.abort();
            return packageDeclaration;
        }

        // Visit annotations if present
        if (packageDeclaration.annotations && otherPackageDeclaration.annotations) {
            if (packageDeclaration.annotations.length !== otherPackageDeclaration.annotations.length) {
                this.abort();
                return packageDeclaration;
            }

            // Visit each annotation in lock step
            for (let i = 0; i < packageDeclaration.annotations.length; i++) {
                await this.visit(packageDeclaration.annotations[i], otherPackageDeclaration.annotations[i]);
                if (!this.match) return packageDeclaration;
            }
        }

        return packageDeclaration;
    }

    /**
     * Overrides the visitParameterizedType method to compare parameterized types.
     * 
     * @param parameterizedType The parameterized type to visit
     * @param other The other parameterized type to compare with
     * @returns The visited parameterized type, or undefined if the visit was aborted
     */
    override async visitParameterizedType(parameterizedType: J.ParameterizedType, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.ParameterizedType) {
            this.abort();
            return parameterizedType;
        }

        const otherParameterizedType = other as J.ParameterizedType;

        // Visit class
        await this.visit(parameterizedType.class, otherParameterizedType.class);
        if (!this.match) return parameterizedType;

        // Compare typeParameters
        if ((parameterizedType.typeParameters === undefined) !== (otherParameterizedType.typeParameters === undefined)) {
            this.abort();
            return parameterizedType;
        }

        // Visit typeParameters if present
        if (parameterizedType.typeParameters && otherParameterizedType.typeParameters) {
            if (parameterizedType.typeParameters.elements.length !== otherParameterizedType.typeParameters.elements.length) {
                this.abort();
                return parameterizedType;
            }

            // Visit each type parameter in lock step
            for (let i = 0; i < parameterizedType.typeParameters.elements.length; i++) {
                await this.visit(parameterizedType.typeParameters.elements[i].element, otherParameterizedType.typeParameters.elements[i].element);
                if (!this.match) return parameterizedType;
            }
        }

        return parameterizedType;
    }

    /**
     * Overrides the visitParentheses method to compare parentheses expressions.
     * 
     * @param parentheses The parentheses expression to visit
     * @param other The other parentheses expression to compare with
     * @returns The visited parentheses expression, or undefined if the visit was aborted
     */
    override async visitParentheses(parentheses: J.Parentheses<J>, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Parentheses) {
            this.abort();
            return parentheses;
        }

        const otherParentheses = other as J.Parentheses<J>;

        // Visit tree
        await this.visit(parentheses.tree.element, otherParentheses.tree.element);
        if (!this.match) return parentheses;

        return parentheses;
    }

    /**
     * Overrides the visitParenthesizedTypeTree method to compare parenthesized type trees.
     * 
     * @param parenthesizedTypeTree The parenthesized type tree to visit
     * @param other The other parenthesized type tree to compare with
     * @returns The visited parenthesized type tree, or undefined if the visit was aborted
     */
    override async visitParenthesizedTypeTree(parenthesizedTypeTree: J.ParenthesizedTypeTree, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.ParenthesizedTypeTree) {
            this.abort();
            return parenthesizedTypeTree;
        }

        const otherParenthesizedTypeTree = other as J.ParenthesizedTypeTree;

        // Compare annotations
        if (parenthesizedTypeTree.annotations.length !== otherParenthesizedTypeTree.annotations.length) {
            this.abort();
            return parenthesizedTypeTree;
        }

        // Visit each annotation in lock step
        for (let i = 0; i < parenthesizedTypeTree.annotations.length; i++) {
            await this.visit(parenthesizedTypeTree.annotations[i], otherParenthesizedTypeTree.annotations[i]);
            if (!this.match) return parenthesizedTypeTree;
        }

        // Visit parenthesizedType
        await this.visit(parenthesizedTypeTree.parenthesizedType, otherParenthesizedTypeTree.parenthesizedType);
        if (!this.match) return parenthesizedTypeTree;

        return parenthesizedTypeTree;
    }

    /**
     * Overrides the visitPrimitive method to compare primitive types.
     * 
     * @param primitive The primitive type to visit
     * @param other The other primitive type to compare with
     * @returns The visited primitive type, or undefined if the visit was aborted
     */
    override async visitPrimitive(primitive: J.Primitive, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Primitive) {
            this.abort();
            return primitive;
        }

        const otherPrimitive = other as J.Primitive;

        // Compare type
        if (primitive.type.kind !== otherPrimitive.type.kind) {
            this.abort();
            return primitive;
        }

        return primitive;
    }

    /**
     * Overrides the visitReturn method to compare return statements.
     * 
     * @param returnStatement The return statement to visit
     * @param other The other return statement to compare with
     * @returns The visited return statement, or undefined if the visit was aborted
     */
    override async visitReturn(returnStatement: J.Return, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Return) {
            this.abort();
            return returnStatement;
        }

        const otherReturnStatement = other as J.Return;

        // Compare expression
        if ((returnStatement.expression === undefined) !== (otherReturnStatement.expression === undefined)) {
            this.abort();
            return returnStatement;
        }

        // Visit expression if present
        if (returnStatement.expression && otherReturnStatement.expression) {
            await this.visit(returnStatement.expression, otherReturnStatement.expression);
            if (!this.match) return returnStatement;
        }

        return returnStatement;
    }

    /**
     * Overrides the visitSwitch method to compare switch statements.
     * 
     * @param switchStatement The switch statement to visit
     * @param other The other switch statement to compare with
     * @returns The visited switch statement, or undefined if the visit was aborted
     */
    override async visitSwitch(switchStatement: J.Switch, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Switch) {
            this.abort();
            return switchStatement;
        }

        const otherSwitchStatement = other as J.Switch;

        // Visit selector
        await this.visit(switchStatement.selector, otherSwitchStatement.selector);
        if (!this.match) return switchStatement;

        // Visit cases
        await this.visit(switchStatement.cases, otherSwitchStatement.cases);
        if (!this.match) return switchStatement;

        return switchStatement;
    }

    /**
     * Overrides the visitSwitchExpression method to compare switch expressions.
     * 
     * @param switchExpression The switch expression to visit
     * @param other The other switch expression to compare with
     * @returns The visited switch expression, or undefined if the visit was aborted
     */
    override async visitSwitchExpression(switchExpression: J.SwitchExpression, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.SwitchExpression) {
            this.abort();
            return switchExpression;
        }

        const otherSwitchExpression = other as J.SwitchExpression;

        // Visit selector
        await this.visit(switchExpression.selector, otherSwitchExpression.selector);
        if (!this.match) return switchExpression;

        // Visit cases
        await this.visit(switchExpression.cases, otherSwitchExpression.cases);
        if (!this.match) return switchExpression;

        return switchExpression;
    }

    /**
     * Overrides the visitSynchronized method to compare synchronized statements.
     * 
     * @param synchronizedStatement The synchronized statement to visit
     * @param other The other synchronized statement to compare with
     * @returns The visited synchronized statement, or undefined if the visit was aborted
     */
    override async visitSynchronized(synchronizedStatement: J.Synchronized, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Synchronized) {
            this.abort();
            return synchronizedStatement;
        }

        const otherSynchronizedStatement = other as J.Synchronized;

        // Visit lock
        await this.visit(synchronizedStatement.lock, otherSynchronizedStatement.lock);
        if (!this.match) return synchronizedStatement;

        // Visit body
        await this.visit(synchronizedStatement.body, otherSynchronizedStatement.body);
        if (!this.match) return synchronizedStatement;

        return synchronizedStatement;
    }

    /**
     * Overrides the visitTernary method to compare ternary expressions.
     * 
     * @param ternary The ternary expression to visit
     * @param other The other ternary expression to compare with
     * @returns The visited ternary expression, or undefined if the visit was aborted
     */
    override async visitTernary(ternary: J.Ternary, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Ternary) {
            this.abort();
            return ternary;
        }

        const otherTernary = other as J.Ternary;

        // Visit condition
        await this.visit(ternary.condition, otherTernary.condition);
        if (!this.match) return ternary;

        // Visit truePart
        await this.visit(ternary.truePart.element, otherTernary.truePart.element);
        if (!this.match) return ternary;

        // Visit falsePart
        await this.visit(ternary.falsePart.element, otherTernary.falsePart.element);
        if (!this.match) return ternary;

        return ternary;
    }

    /**
     * Overrides the visitThrow method to compare throw statements.
     * 
     * @param throwStatement The throw statement to visit
     * @param other The other throw statement to compare with
     * @returns The visited throw statement, or undefined if the visit was aborted
     */
    override async visitThrow(throwStatement: J.Throw, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Throw) {
            this.abort();
            return throwStatement;
        }

        const otherThrowStatement = other as J.Throw;

        // Visit exception
        await this.visit(throwStatement.exception, otherThrowStatement.exception);
        if (!this.match) return throwStatement;

        return throwStatement;
    }

    /**
     * Overrides the visitTry method to compare try statements.
     * 
     * @param tryStatement The try statement to visit
     * @param other The other try statement to compare with
     * @returns The visited try statement, or undefined if the visit was aborted
     */
    override async visitTry(tryStatement: J.Try, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Try) {
            this.abort();
            return tryStatement;
        }

        const otherTryStatement = other as J.Try;

        // Compare resources
        if ((tryStatement.resources === undefined) !== (otherTryStatement.resources === undefined)) {
            this.abort();
            return tryStatement;
        }

        // Visit resources if present
        if (tryStatement.resources && otherTryStatement.resources) {
            if (tryStatement.resources.elements.length !== otherTryStatement.resources.elements.length) {
                this.abort();
                return tryStatement;
            }

            // Visit each resource in lock step
            for (let i = 0; i < tryStatement.resources.elements.length; i++) {
                await this.visit(tryStatement.resources.elements[i].element, otherTryStatement.resources.elements[i].element);
                if (!this.match) return tryStatement;
            }
        }

        // Visit body
        await this.visit(tryStatement.body, otherTryStatement.body);
        if (!this.match) return tryStatement;

        // Compare catches
        if (tryStatement.catches.length !== otherTryStatement.catches.length) {
            this.abort();
            return tryStatement;
        }

        // Visit each catch in lock step
        for (let i = 0; i < tryStatement.catches.length; i++) {
            await this.visit(tryStatement.catches[i], otherTryStatement.catches[i]);
            if (!this.match) return tryStatement;
        }

        // Compare finally
        if ((tryStatement.finally === undefined) !== (otherTryStatement.finally === undefined)) {
            this.abort();
            return tryStatement;
        }

        // Visit finally if present
        if (tryStatement.finally && otherTryStatement.finally) {
            await this.visit(tryStatement.finally.element, otherTryStatement.finally.element);
            if (!this.match) return tryStatement;
        }

        return tryStatement;
    }

    /**
     * Overrides the visitTryResource method to compare try resources.
     * 
     * @param resource The try resource to visit
     * @param other The other try resource to compare with
     * @returns The visited try resource, or undefined if the visit was aborted
     */
    override async visitTryResource(resource: J.Try.Resource, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.TryResource) {
            this.abort();
            return resource;
        }

        const otherResource = other as J.Try.Resource;

        // Visit variableDeclarations
        await this.visit(resource.variableDeclarations, otherResource.variableDeclarations);
        if (!this.match) return resource;

        // Compare terminatedWithSemicolon
        if (resource.terminatedWithSemicolon !== otherResource.terminatedWithSemicolon) {
            this.abort();
            return resource;
        }

        return resource;
    }

    /**
     * Overrides the visitTryCatch method to compare try catch blocks.
     * 
     * @param tryCatch The try catch block to visit
     * @param other The other try catch block to compare with
     * @returns The visited try catch block, or undefined if the visit was aborted
     */
    override async visitTryCatch(tryCatch: J.Try.Catch, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.TryCatch) {
            this.abort();
            return tryCatch;
        }

        const otherTryCatch = other as J.Try.Catch;

        // Visit parameter
        await this.visit(tryCatch.parameter, otherTryCatch.parameter);
        if (!this.match) return tryCatch;

        // Visit body
        await this.visit(tryCatch.body, otherTryCatch.body);
        if (!this.match) return tryCatch;

        return tryCatch;
    }

    /**
     * Overrides the visitTypeCast method to compare type cast expressions.
     * 
     * @param typeCast The type cast expression to visit
     * @param other The other type cast expression to compare with
     * @returns The visited type cast expression, or undefined if the visit was aborted
     */
    override async visitTypeCast(typeCast: J.TypeCast, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.TypeCast) {
            this.abort();
            return typeCast;
        }

        const otherTypeCast = other as J.TypeCast;

        // Visit class
        await this.visit(typeCast.class, otherTypeCast.class);
        if (!this.match) return typeCast;

        // Visit expression
        await this.visit(typeCast.expression, otherTypeCast.expression);
        if (!this.match) return typeCast;

        return typeCast;
    }

    /**
     * Overrides the visitTypeParameter method to compare type parameters.
     * 
     * @param typeParameter The type parameter to visit
     * @param other The other type parameter to compare with
     * @returns The visited type parameter, or undefined if the visit was aborted
     */
    override async visitTypeParameter(typeParameter: J.TypeParameter, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.TypeParameter) {
            this.abort();
            return typeParameter;
        }

        const otherTypeParameter = other as J.TypeParameter;

        // Compare annotations
        if (typeParameter.annotations.length !== otherTypeParameter.annotations.length) {
            this.abort();
            return typeParameter;
        }

        // Visit each annotation in lock step
        for (let i = 0; i < typeParameter.annotations.length; i++) {
            await this.visit(typeParameter.annotations[i], otherTypeParameter.annotations[i]);
            if (!this.match) return typeParameter;
        }

        // Compare modifiers
        if (typeParameter.modifiers.length !== otherTypeParameter.modifiers.length) {
            this.abort();
            return typeParameter;
        }

        // Visit each modifier in lock step
        for (let i = 0; i < typeParameter.modifiers.length; i++) {
            await this.visit(typeParameter.modifiers[i], otherTypeParameter.modifiers[i]);
            if (!this.match) return typeParameter;
        }

        // Visit name
        await this.visit(typeParameter.name, otherTypeParameter.name);
        if (!this.match) return typeParameter;

        // Compare bounds
        if ((typeParameter.bounds === undefined) !== (otherTypeParameter.bounds === undefined)) {
            this.abort();
            return typeParameter;
        }

        // Visit bounds if present
        if (typeParameter.bounds && otherTypeParameter.bounds) {
            if (typeParameter.bounds.elements.length !== otherTypeParameter.bounds.elements.length) {
                this.abort();
                return typeParameter;
            }

            // Visit each bound in lock step
            for (let i = 0; i < typeParameter.bounds.elements.length; i++) {
                await this.visit(typeParameter.bounds.elements[i].element, otherTypeParameter.bounds.elements[i].element);
                if (!this.match) return typeParameter;
            }
        }

        return typeParameter;
    }

    /**
     * Overrides the visitTypeParameters method to compare type parameters.
     * 
     * @param typeParameters The type parameters to visit
     * @param other The other type parameters to compare with
     * @returns The visited type parameters, or undefined if the visit was aborted
     */
    override async visitTypeParameters(typeParameters: J.TypeParameters, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.TypeParameters) {
            this.abort();
            return typeParameters;
        }

        const otherTypeParameters = other as J.TypeParameters;

        // Compare annotations
        if (typeParameters.annotations.length !== otherTypeParameters.annotations.length) {
            this.abort();
            return typeParameters;
        }

        // Visit each annotation in lock step
        for (let i = 0; i < typeParameters.annotations.length; i++) {
            await this.visit(typeParameters.annotations[i], otherTypeParameters.annotations[i]);
            if (!this.match) return typeParameters;
        }

        // Compare typeParameters
        if (typeParameters.typeParameters.length !== otherTypeParameters.typeParameters.length) {
            this.abort();
            return typeParameters;
        }

        // Visit each type parameter in lock step
        for (let i = 0; i < typeParameters.typeParameters.length; i++) {
            await this.visit(typeParameters.typeParameters[i].element, otherTypeParameters.typeParameters[i].element);
            if (!this.match) return typeParameters;
        }

        return typeParameters;
    }

    /**
     * Overrides the visitUnary method to compare unary expressions.
     * 
     * @param unary The unary expression to visit
     * @param other The other unary expression to compare with
     * @returns The visited unary expression, or undefined if the visit was aborted
     */
    override async visitUnary(unary: J.Unary, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Unary) {
            this.abort();
            return unary;
        }

        const otherUnary = other as J.Unary;

        // Compare operator
        if (unary.operator.element !== otherUnary.operator.element) {
            this.abort();
            return unary;
        }

        // Visit expression
        await this.visit(unary.expression, otherUnary.expression);
        if (!this.match) return unary;

        return unary;
    }

    /**
     * Overrides the visitUnknown method to compare unknown nodes.
     * 
     * @param unknown The unknown node to visit
     * @param other The other unknown node to compare with
     * @returns The visited unknown node, or undefined if the visit was aborted
     */
    override async visitUnknown(unknown: J.Unknown, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Unknown) {
            this.abort();
            return unknown;
        }

        const otherUnknown = other as J.Unknown;

        // Visit source
        await this.visit(unknown.source, otherUnknown.source);
        if (!this.match) return unknown;

        return unknown;
    }

    /**
     * Overrides the visitUnknownSource method to compare unknown sources.
     * 
     * @param unknownSource The unknown source to visit
     * @param other The other unknown source to compare with
     * @returns The visited unknown source, or undefined if the visit was aborted
     */
    override async visitUnknownSource(unknownSource: J.UnknownSource, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.UnknownSource) {
            this.abort();
            return unknownSource;
        }

        const otherUnknownSource = other as J.UnknownSource;

        // Compare text
        if (unknownSource.text !== otherUnknownSource.text) {
            this.abort();
            return unknownSource;
        }

        return unknownSource;
    }

    /**
     * Overrides the visitVariableDeclarations method to compare variable declarations.
     * 
     * @param variableDeclarations The variable declarations to visit
     * @param other The other variable declarations to compare with
     * @returns The visited variable declarations, or undefined if the visit was aborted
     */
    override async visitVariableDeclarations(variableDeclarations: J.VariableDeclarations, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.VariableDeclarations) {
            this.abort();
            return variableDeclarations;
        }

        const otherVariableDeclarations = other as J.VariableDeclarations;

        // Compare leadingAnnotations
        if (variableDeclarations.leadingAnnotations.length !== otherVariableDeclarations.leadingAnnotations.length) {
            this.abort();
            return variableDeclarations;
        }

        // Visit each leading annotation in lock step
        for (let i = 0; i < variableDeclarations.leadingAnnotations.length; i++) {
            await this.visit(variableDeclarations.leadingAnnotations[i], otherVariableDeclarations.leadingAnnotations[i]);
            if (!this.match) return variableDeclarations;
        }

        // Compare modifiers
        if (variableDeclarations.modifiers.length !== otherVariableDeclarations.modifiers.length) {
            this.abort();
            return variableDeclarations;
        }

        // Visit each modifier in lock step
        for (let i = 0; i < variableDeclarations.modifiers.length; i++) {
            await this.visit(variableDeclarations.modifiers[i], otherVariableDeclarations.modifiers[i]);
            if (!this.match) return variableDeclarations;
        }

        // Compare typeExpression
        if ((variableDeclarations.typeExpression === undefined) !== (otherVariableDeclarations.typeExpression === undefined)) {
            this.abort();
            return variableDeclarations;
        }

        // Visit typeExpression if present
        if (variableDeclarations.typeExpression && otherVariableDeclarations.typeExpression) {
            await this.visit(variableDeclarations.typeExpression, otherVariableDeclarations.typeExpression);
            if (!this.match) return variableDeclarations;
        }

        // Compare varargs
        if ((variableDeclarations.varargs === undefined) !== (otherVariableDeclarations.varargs === undefined)) {
            this.abort();
            return variableDeclarations;
        }

        // Compare variables
        if (variableDeclarations.variables.length !== otherVariableDeclarations.variables.length) {
            this.abort();
            return variableDeclarations;
        }

        // Visit each variable in lock step
        for (let i = 0; i < variableDeclarations.variables.length; i++) {
            await this.visit(variableDeclarations.variables[i].element, otherVariableDeclarations.variables[i].element);
            if (!this.match) return variableDeclarations;
        }

        return variableDeclarations;
    }

    /**
     * Overrides the visitVariable method to compare variable declarations.
     * 
     * @param variable The variable declaration to visit
     * @param other The other variable declaration to compare with
     * @returns The visited variable declaration, or undefined if the visit was aborted
     */
    override async visitVariable(variable: J.VariableDeclarations.NamedVariable, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.NamedVariable) {
            this.abort();
            return variable;
        }

        const otherVariable = other as J.VariableDeclarations.NamedVariable;

        // Visit name
        await this.visit(variable.name, otherVariable.name);
        if (!this.match) return variable;

        // Compare dimensionsAfterName
        if (variable.dimensionsAfterName.length !== otherVariable.dimensionsAfterName.length) {
            this.abort();
            return variable;
        }

        // Compare initializer
        if ((variable.initializer === undefined) !== (otherVariable.initializer === undefined)) {
            this.abort();
            return variable;
        }

        // Visit initializer if present
        if (variable.initializer && otherVariable.initializer) {
            await this.visit(variable.initializer.element, otherVariable.initializer.element);
            if (!this.match) return variable;
        }

        return variable;
    }

    /**
     * Overrides the visitWhileLoop method to compare while loops.
     * 
     * @param whileLoop The while loop to visit
     * @param other The other while loop to compare with
     * @returns The visited while loop, or undefined if the visit was aborted
     */
    override async visitWhileLoop(whileLoop: J.WhileLoop, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.WhileLoop) {
            this.abort();
            return whileLoop;
        }

        const otherWhileLoop = other as J.WhileLoop;

        // Visit condition
        await this.visit(whileLoop.condition, otherWhileLoop.condition);
        if (!this.match) return whileLoop;

        // Visit body
        await this.visit(whileLoop.body.element, otherWhileLoop.body.element);
        if (!this.match) return whileLoop;

        return whileLoop;
    }

    /**
     * Overrides the visitWildcard method to compare wildcards.
     * 
     * @param wildcard The wildcard to visit
     * @param other The other wildcard to compare with
     * @returns The visited wildcard, or undefined if the visit was aborted
     */
    override async visitWildcard(wildcard: J.Wildcard, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Wildcard) {
            this.abort();
            return wildcard;
        }

        const otherWildcard = other as J.Wildcard;

        // Compare bound
        if ((wildcard.bound === undefined) !== (otherWildcard.bound === undefined)) {
            this.abort();
            return wildcard;
        }

        // Compare bound if present
        if (wildcard.bound && otherWildcard.bound) {
            if (wildcard.bound.element !== otherWildcard.bound.element) {
                this.abort();
                return wildcard;
            }
        }

        // Compare boundedType
        if ((wildcard.boundedType === undefined) !== (otherWildcard.boundedType === undefined)) {
            this.abort();
            return wildcard;
        }

        // Visit boundedType if present
        if (wildcard.boundedType && otherWildcard.boundedType) {
            await this.visit(wildcard.boundedType, otherWildcard.boundedType);
            if (!this.match) return wildcard;
        }

        return wildcard;
    }

    /**
     * Overrides the visitYield method to compare yield statements.
     * 
     * @param yieldStatement The yield statement to visit
     * @param other The other yield statement to compare with
     * @returns The visited yield statement, or undefined if the visit was aborted
     */
    override async visitYield(yieldStatement: J.Yield, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Yield) {
            this.abort();
            return yieldStatement;
        }

        const otherYieldStatement = other as J.Yield;

        // Compare implicit
        if (yieldStatement.implicit !== otherYieldStatement.implicit) {
            this.abort();
            return yieldStatement;
        }

        // Visit value
        await this.visit(yieldStatement.value, otherYieldStatement.value);
        if (!this.match) return yieldStatement;

        return yieldStatement;
    }

    /**
     * Overrides the visitVoid method to compare void expressions.
     * 
     * @param void_ The void expression to visit
     * @param other The other void expression to compare with
     * @returns The visited void expression, or undefined if the visit was aborted
     */
    override async visitVoid(void_: JS.Void, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.Void) {
            this.abort();
            return void_;
        }

        const otherVoid = other as JS.Void;

        // Visit expression
        await this.visit(void_.expression, otherVoid.expression);

        return void_;
    }

    /**
     * Overrides the visitWithStatement method to compare with statements.
     * 
     * @param withStatement The with statement to visit
     * @param other The other with statement to compare with
     * @returns The visited with statement, or undefined if the visit was aborted
     */
    override async visitWithStatement(withStatement: JS.WithStatement, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.WithStatement) {
            this.abort();
            return withStatement;
        }

        const otherWithStatement = other as JS.WithStatement;

        // Visit expression
        await this.visit(withStatement.expression, otherWithStatement.expression);
        if (!this.match) return withStatement;

        // Visit body
        await this.visit(withStatement.body.element, otherWithStatement.body.element);

        return withStatement;
    }

    /**
     * Overrides the visitIndexSignatureDeclaration method to compare index signature declarations.
     * 
     * @param indexSignatureDeclaration The index signature declaration to visit
     * @param other The other index signature declaration to compare with
     * @returns The visited index signature declaration, or undefined if the visit was aborted
     */
    override async visitIndexSignatureDeclaration(indexSignatureDeclaration: JS.IndexSignatureDeclaration, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.IndexSignatureDeclaration) {
            this.abort();
            return indexSignatureDeclaration;
        }

        const otherIndexSignatureDeclaration = other as JS.IndexSignatureDeclaration;

        // Compare modifiers
        if (indexSignatureDeclaration.modifiers.length !== otherIndexSignatureDeclaration.modifiers.length) {
            this.abort();
            return indexSignatureDeclaration;
        }

        // Visit modifiers in lock step
        for (let i = 0; i < indexSignatureDeclaration.modifiers.length; i++) {
            await this.visit(indexSignatureDeclaration.modifiers[i], otherIndexSignatureDeclaration.modifiers[i]);
            if (!this.match) return indexSignatureDeclaration;
        }

        // Compare parameters
        if (indexSignatureDeclaration.parameters.elements.length !== otherIndexSignatureDeclaration.parameters.elements.length) {
            this.abort();
            return indexSignatureDeclaration;
        }

        // Visit parameters in lock step
        for (let i = 0; i < indexSignatureDeclaration.parameters.elements.length; i++) {
            await this.visit(indexSignatureDeclaration.parameters.elements[i].element, 
                           otherIndexSignatureDeclaration.parameters.elements[i].element);
            if (!this.match) return indexSignatureDeclaration;
        }

        // Visit type expression
        await this.visit(indexSignatureDeclaration.typeExpression.element, otherIndexSignatureDeclaration.typeExpression.element);

        return indexSignatureDeclaration;
    }

    /**
     * Overrides the visitForOfLoop method to compare for-of loops.
     * 
     * @param forOfLoop The for-of loop to visit
     * @param other The other for-of loop to compare with
     * @returns The visited for-of loop, or undefined if the visit was aborted
     */
    override async visitForOfLoop(forOfLoop: JS.ForOfLoop, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ForOfLoop) {
            this.abort();
            return forOfLoop;
        }

        const otherForOfLoop = other as JS.ForOfLoop;

        // Compare await
        if ((forOfLoop.await === undefined) !== (otherForOfLoop.await === undefined)) {
            this.abort();
            return forOfLoop;
        }

        // Visit loop
        await this.visit(forOfLoop.loop, otherForOfLoop.loop);
        if (!this.match) return forOfLoop;

        return forOfLoop;
    }

    /**
     * Overrides the visitForInLoop method to compare for-in loops.
     * 
     * @param forInLoop The for-in loop to visit
     * @param other The other for-in loop to compare with
     * @returns The visited for-in loop, or undefined if the visit was aborted
     */
    override async visitForInLoop(forInLoop: JS.ForInLoop, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ForInLoop) {
            this.abort();
            return forInLoop;
        }

        const otherForInLoop = other as JS.ForInLoop;

        // Visit control
        await this.visit(forInLoop.control, otherForInLoop.control);
        if (!this.match) return forInLoop;

        // Visit body
        await this.visit(forInLoop.body.element, otherForInLoop.body.element);
        if (!this.match) return forInLoop;

        return forInLoop;
    }

    /**
     * Overrides the visitNamespaceDeclaration method to compare namespace declarations.
     * 
     * @param namespaceDeclaration The namespace declaration to visit
     * @param other The other namespace declaration to compare with
     * @returns The visited namespace declaration, or undefined if the visit was aborted
     */
    override async visitNamespaceDeclaration(namespaceDeclaration: JS.NamespaceDeclaration, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.NamespaceDeclaration) {
            this.abort();
            return namespaceDeclaration;
        }

        const otherNamespaceDeclaration = other as JS.NamespaceDeclaration;

        // Compare modifiers
        if (namespaceDeclaration.modifiers.length !== otherNamespaceDeclaration.modifiers.length) {
            this.abort();
            return namespaceDeclaration;
        }

        // Visit each modifier in lock step
        for (let i = 0; i < namespaceDeclaration.modifiers.length; i++) {
            await this.visit(namespaceDeclaration.modifiers[i], otherNamespaceDeclaration.modifiers[i]);
            if (!this.match) return namespaceDeclaration;
        }

        // Compare keywordType
        if (namespaceDeclaration.keywordType.element !== otherNamespaceDeclaration.keywordType.element) {
            this.abort();
            return namespaceDeclaration;
        }

        // Visit name
        await this.visit(namespaceDeclaration.name.element, otherNamespaceDeclaration.name.element);
        if (!this.match) return namespaceDeclaration;

        // Compare body
        if ((namespaceDeclaration.body === undefined) !== (otherNamespaceDeclaration.body === undefined)) {
            this.abort();
            return namespaceDeclaration;
        }

        // Visit body if present
        if (namespaceDeclaration.body && otherNamespaceDeclaration.body) {
            await this.visit(namespaceDeclaration.body, otherNamespaceDeclaration.body);
            if (!this.match) return namespaceDeclaration;
        }

        return namespaceDeclaration;
    }

    /**
     * Overrides the visitTypeLiteral method to compare type literals.
     * 
     * @param typeLiteral The type literal to visit
     * @param other The other type literal to compare with
     * @returns The visited type literal, or undefined if the visit was aborted
     */
    override async visitTypeLiteral(typeLiteral: JS.TypeLiteral, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.TypeLiteral) {
            this.abort();
            return typeLiteral;
        }

        const otherTypeLiteral = other as JS.TypeLiteral;

        // Visit members
        await this.visit(typeLiteral.members, otherTypeLiteral.members);
        if (!this.match) return typeLiteral;

        return typeLiteral;
    }

    /**
     * Overrides the visitBindingElement method to compare binding elements.
     * 
     * @param bindingElement The binding element to visit
     * @param other The other binding element to compare with
     * @returns The visited binding element, or undefined if the visit was aborted
     */
    override async visitBindingElement(bindingElement: JS.BindingElement, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.BindingElement) {
            this.abort();
            return bindingElement;
        }

        const otherBindingElement = other as JS.BindingElement;

        // Compare propertyName
        if ((bindingElement.propertyName === undefined) !== (otherBindingElement.propertyName === undefined)) {
            this.abort();
            return bindingElement;
        }

        // Visit propertyName if present
        if (bindingElement.propertyName && otherBindingElement.propertyName) {
            await this.visit(bindingElement.propertyName.element, otherBindingElement.propertyName.element);
            if (!this.match) return bindingElement;
        }

        // Visit name
        await this.visit(bindingElement.name, otherBindingElement.name);
        if (!this.match) return bindingElement;

        // Compare initializer
        if ((bindingElement.initializer === undefined) !== (otherBindingElement.initializer === undefined)) {
            this.abort();
            return bindingElement;
        }

        // Visit initializer if present
        if (bindingElement.initializer && otherBindingElement.initializer) {
            await this.visit(bindingElement.initializer.element, otherBindingElement.initializer.element);
            if (!this.match) return bindingElement;
        }

        return bindingElement;
    }

    /**
     * Overrides the visitArrayBindingPattern method to compare array binding patterns.
     * 
     * @param arrayBindingPattern The array binding pattern to visit
     * @param other The other array binding pattern to compare with
     * @returns The visited array binding pattern, or undefined if the visit was aborted
     */
    override async visitArrayBindingPattern(arrayBindingPattern: JS.ArrayBindingPattern, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ArrayBindingPattern) {
            this.abort();
            return arrayBindingPattern;
        }

        const otherArrayBindingPattern = other as JS.ArrayBindingPattern;

        // Compare elements
        if (arrayBindingPattern.elements.elements.length !== otherArrayBindingPattern.elements.elements.length) {
            this.abort();
            return arrayBindingPattern;
        }

        // Visit each element in lock step
        for (let i = 0; i < arrayBindingPattern.elements.elements.length; i++) {
            await this.visit(arrayBindingPattern.elements.elements[i].element, otherArrayBindingPattern.elements.elements[i].element);
            if (!this.match) return arrayBindingPattern;
        }

        return arrayBindingPattern;
    }

    /**
     * Overrides the visitExportDeclaration method to compare export declarations.
     * 
     * @param exportDeclaration The export declaration to visit
     * @param other The other export declaration to compare with
     * @returns The visited export declaration, or undefined if the visit was aborted
     */
    override async visitExportDeclaration(exportDeclaration: JS.ExportDeclaration, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ExportDeclaration) {
            this.abort();
            return exportDeclaration;
        }

        const otherExportDeclaration = other as JS.ExportDeclaration;

        // Compare modifiers
        if (exportDeclaration.modifiers.length !== otherExportDeclaration.modifiers.length) {
            this.abort();
            return exportDeclaration;
        }

        // Visit each modifier in lock step
        for (let i = 0; i < exportDeclaration.modifiers.length; i++) {
            await this.visit(exportDeclaration.modifiers[i], otherExportDeclaration.modifiers[i]);
            if (!this.match) return exportDeclaration;
        }

        // Compare typeOnly
        if (exportDeclaration.typeOnly.element !== otherExportDeclaration.typeOnly.element) {
            this.abort();
            return exportDeclaration;
        }

        // Compare exportClause
        if ((exportDeclaration.exportClause === undefined) !== (otherExportDeclaration.exportClause === undefined)) {
            this.abort();
            return exportDeclaration;
        }

        // Visit exportClause if present
        if (exportDeclaration.exportClause && otherExportDeclaration.exportClause) {
            await this.visit(exportDeclaration.exportClause, otherExportDeclaration.exportClause);
            if (!this.match) return exportDeclaration;
        }

        // Compare moduleSpecifier
        if ((exportDeclaration.moduleSpecifier === undefined) !== (otherExportDeclaration.moduleSpecifier === undefined)) {
            this.abort();
            return exportDeclaration;
        }

        // Visit moduleSpecifier if present
        if (exportDeclaration.moduleSpecifier && otherExportDeclaration.moduleSpecifier) {
            await this.visit(exportDeclaration.moduleSpecifier.element, otherExportDeclaration.moduleSpecifier.element);
            if (!this.match) return exportDeclaration;
        }

        // Compare attributes
        if ((exportDeclaration.attributes === undefined) !== (otherExportDeclaration.attributes === undefined)) {
            this.abort();
            return exportDeclaration;
        }

        // Visit attributes if present
        if (exportDeclaration.attributes && otherExportDeclaration.attributes) {
            await this.visit(exportDeclaration.attributes, otherExportDeclaration.attributes);
            if (!this.match) return exportDeclaration;
        }

        return exportDeclaration;
    }

    /**
     * Overrides the visitExportAssignment method to compare export assignments.
     * 
     * @param exportAssignment The export assignment to visit
     * @param other The other export assignment to compare with
     * @returns The visited export assignment, or undefined if the visit was aborted
     */
    override async visitExportAssignment(exportAssignment: JS.ExportAssignment, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ExportAssignment) {
            this.abort();
            return exportAssignment;
        }

        const otherExportAssignment = other as JS.ExportAssignment;

        // Compare exportEquals
        if (exportAssignment.exportEquals !== otherExportAssignment.exportEquals) {
            this.abort();
            return exportAssignment;
        }

        // Visit expression
        await this.visit(exportAssignment.expression.element, otherExportAssignment.expression.element);
        if (!this.match) return exportAssignment;

        return exportAssignment;
    }

    /**
     * Overrides the visitNamedExports method to compare named exports.
     * 
     * @param namedExports The named exports to visit
     * @param other The other named exports to compare with
     * @returns The visited named exports, or undefined if the visit was aborted
     */
    override async visitNamedExports(namedExports: JS.NamedExports, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.NamedExports) {
            this.abort();
            return namedExports;
        }

        const otherNamedExports = other as JS.NamedExports;

        // Compare elements
        if (namedExports.elements.elements.length !== otherNamedExports.elements.elements.length) {
            this.abort();
            return namedExports;
        }

        // Visit each element in lock step
        for (let i = 0; i < namedExports.elements.elements.length; i++) {
            await this.visit(namedExports.elements.elements[i].element, otherNamedExports.elements.elements[i].element);
            if (!this.match) return namedExports;
        }

        return namedExports;
    }

    /**
     * Overrides the visitExportSpecifier method to compare export specifiers.
     * 
     * @param exportSpecifier The export specifier to visit
     * @param other The other export specifier to compare with
     * @returns The visited export specifier, or undefined if the visit was aborted
     */
    override async visitExportSpecifier(exportSpecifier: JS.ExportSpecifier, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ExportSpecifier) {
            this.abort();
            return exportSpecifier;
        }

        const otherExportSpecifier = other as JS.ExportSpecifier;

        // Compare typeOnly
        if (exportSpecifier.typeOnly.element !== otherExportSpecifier.typeOnly.element) {
            this.abort();
            return exportSpecifier;
        }

        // Visit specifier
        await this.visit(exportSpecifier.specifier, otherExportSpecifier.specifier);
        if (!this.match) return exportSpecifier;

        return exportSpecifier;
    }

    /**
     * Overrides the visitComputedPropertyMethodDeclaration method to compare computed property method declarations.
     * 
     * @param computedPropMethod The computed property method declaration to visit
     * @param other The other computed property method declaration to compare with
     * @returns The visited computed property method declaration, or undefined if the visit was aborted
     */
    override async visitComputedPropertyMethodDeclaration(computedPropMethod: JS.ComputedPropertyMethodDeclaration, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== JS.Kind.ComputedPropertyMethodDeclaration) {
            this.abort();
            return computedPropMethod;
        }

        const otherComputedPropMethod = other as JS.ComputedPropertyMethodDeclaration;

        // Compare leading annotations
        if (computedPropMethod.leadingAnnotations.length !== otherComputedPropMethod.leadingAnnotations.length) {
            this.abort();
            return computedPropMethod;
        }

        // Visit leading annotations in lock step
        for (let i = 0; i < computedPropMethod.leadingAnnotations.length; i++) {
            await this.visit(computedPropMethod.leadingAnnotations[i], otherComputedPropMethod.leadingAnnotations[i]);
            if (!this.match) return computedPropMethod;
        }

        // Compare modifiers
        if (computedPropMethod.modifiers.length !== otherComputedPropMethod.modifiers.length) {
            this.abort();
            return computedPropMethod;
        }

        // Visit modifiers in lock step
        for (let i = 0; i < computedPropMethod.modifiers.length; i++) {
            await this.visit(computedPropMethod.modifiers[i], otherComputedPropMethod.modifiers[i]);
            if (!this.match) return computedPropMethod;
        }

        // Compare type parameters
        if (!!computedPropMethod.typeParameters !== !!otherComputedPropMethod.typeParameters) {
            this.abort();
            return computedPropMethod;
        }

        if (computedPropMethod.typeParameters) {
            await this.visit(computedPropMethod.typeParameters, otherComputedPropMethod.typeParameters!);
            if (!this.match) return computedPropMethod;
        }

        // Compare return type expression
        if (!!computedPropMethod.returnTypeExpression !== !!otherComputedPropMethod.returnTypeExpression) {
            this.abort();
            return computedPropMethod;
        }

        if (computedPropMethod.returnTypeExpression) {
            await this.visit(computedPropMethod.returnTypeExpression, otherComputedPropMethod.returnTypeExpression!);
            if (!this.match) return computedPropMethod;
        }

        // Visit name
        await this.visit(computedPropMethod.name, otherComputedPropMethod.name);
        if (!this.match) return computedPropMethod;

        // Compare parameters
        if (computedPropMethod.parameters.elements.length !== otherComputedPropMethod.parameters.elements.length) {
            this.abort();
            return computedPropMethod;
        }

        // Visit parameters in lock step
        for (let i = 0; i < computedPropMethod.parameters.elements.length; i++) {
            await this.visit(computedPropMethod.parameters.elements[i].element, 
                           otherComputedPropMethod.parameters.elements[i].element);
            if (!this.match) return computedPropMethod;
        }

        // Compare body
        if (!!computedPropMethod.body !== !!otherComputedPropMethod.body) {
            this.abort();
            return computedPropMethod;
        }

        if (computedPropMethod.body) {
            await this.visit(computedPropMethod.body, otherComputedPropMethod.body!);
        }

        return computedPropMethod;
    }
}
