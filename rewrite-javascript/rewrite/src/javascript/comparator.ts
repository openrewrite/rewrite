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
import {J, Type} from '../java';
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
     * Cursor tracking the current position in the target tree.
     * Maintained in parallel with the pattern tree cursor (this.cursor).
     */
    protected targetCursor?: Cursor;

    /**
     * Compares two AST trees.
     *
     * @param tree1 The first tree to compare (pattern tree)
     * @param tree2 The second tree to compare (target tree)
     * @param parentCursor1 Optional parent cursor for the pattern tree (for navigating to root)
     * @param parentCursor2 Optional parent cursor for the target tree (for navigating to root)
     * @returns true if the trees match, false otherwise
     */
    async compare(tree1: J, tree2: J, parentCursor1?: Cursor, parentCursor2?: Cursor): Promise<boolean> {
        this.match = true;
        // Initialize targetCursor with parent if provided, otherwise undefined (will be set by visit())
        this.targetCursor = parentCursor2;
        // Initialize this.cursor (pattern cursor) with parent if provided
        this.cursor = parentCursor1 || new Cursor(undefined, undefined);
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
    protected abort<T>(t: T): T {
        this.match = false;
        return t;
    }


    /**
     * Generic method to visit a property value using the appropriate visitor method.
     * This ensures wrappers (RightPadded, LeftPadded, Container) are properly tracked on the cursor.
     *
     * @param j The property value from the first tree
     * @param other The corresponding property value from the second tree
     * @returns The visited property value from the first tree
     */
    protected async visitProperty(j: any, other: any): Promise<any> {
        // Handle null/undefined (but not other falsy values like 0, false, '')
        if (j == null || other == null) {
            if (j !== other) {
                this.abort(j);
            }
            return j;
        }

        const kind = (j as any).kind;

        // Check wrappers by kind
        if (kind === J.Kind.RightPadded) {
            return await this.visitRightPadded(j, other);
        }

        if (kind === J.Kind.LeftPadded) {
            return await this.visitLeftPadded(j, other);
        }

        if (kind === J.Kind.Container) {
            return await this.visitContainer(j, other);
        }

        // Check if it's a Space (skip comparison)
        if (kind === J.Kind.Space) {
            return j;
        }

        // Check if it's a Tree node (has a kind property with a string value)
        if (kind !== undefined && typeof kind === 'string') {
            // Check if it's a Type node (starts with "org.openrewrite.java.tree.JavaType$")
            if (kind.startsWith('org.openrewrite.java.tree.JavaType$')) {
                return await this.visitType(j, other);
            }
            return await this.visit(j, other);
        }

        // For primitive values, compare directly
        if (j !== other) {
            this.abort(j);
        }
        return j;
    }

    /**
     * Generic method to visit all properties of an element, calling visitProperty for each.
     * This automatically handles wrappers and ensures proper cursor tracking.
     * Also checks that both elements have the same kind.
     *
     * @param j The element from the first tree
     * @param other The corresponding element from the second tree
     * @returns The visited element from the first tree
     */
    protected async visitElement<T extends J>(j: T, other: T): Promise<T> {
        if (!this.match) {
            return j;
        }

        // Check if kinds match
        if (j.kind !== other.kind) {
            return this.abort(j);
        }

        // Iterate over all properties
        for (const key of Object.keys(j)) {
            // Skip internal/private properties, id property, and markers property
            if (key.startsWith('_') || key === 'id' || key === 'markers') {
                continue;
            }

            const jValue = (j as any)[key];
            const otherValue = (other as any)[key];

            // Handle arrays - compare element by element
            if (Array.isArray(jValue)) {
                if (!Array.isArray(otherValue) || jValue.length !== otherValue.length) {
                    return this.abort(j);
                }

                for (let i = 0; i < jValue.length; i++) {
                    await this.visitProperty(jValue[i], otherValue[i]);
                    if (!this.match) {
                        return j;
                    }
                }
            } else {
                // Visit the property (which will handle wrappers, trees, primitives, etc.)
                await this.visitProperty(jValue, otherValue);

                if (!this.match) {
                    return j;
                }
            }
        }

        return j;
    }

    override async visit<R extends J>(j: Tree, p: J, parent?: Cursor): Promise<R | undefined> {
        // If we've already found a mismatch, abort further processing
        if (!this.match) {
            return j as R;
        }

        // Check if the nodes have the same kind
        if (!this.hasSameKind(j as J, p)) {
            return this.abort(j) as R;
        }

        // Update targetCursor to track the target node in parallel with the pattern cursor
        // (Can be overridden by subclasses if they need cursor access before calling super)
        const savedTargetCursor = this.targetCursor;
        this.targetCursor = new Cursor(p, this.targetCursor);
        try {
            // Continue with normal visitation, passing the other node as context
            return await super.visit(j, p);
        } finally {
            this.targetCursor = savedTargetCursor;
        }
    }

    /**
     * Override visitRightPadded to compare only the elements, not markers or spacing.
     * The context parameter p contains the corresponding element from the other tree.
     * Pushes the wrapper onto the cursor stack so captures can access it.
     * Also updates targetCursor in parallel.
     */
    public async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, p: J): Promise<J.RightPadded<T>> {
        if (!this.match) {
            return right;
        }

        // Extract the other element if it's also a RightPadded
        const isRightPadded = (p as any).kind === J.Kind.RightPadded;
        const otherWrapper = isRightPadded ? (p as unknown) as J.RightPadded<T> : undefined;
        const otherElement = isRightPadded ? otherWrapper!.element : p;

        // Push wrappers onto both cursors, then compare only the elements, not markers or spacing
        const savedCursor = this.cursor;
        const savedTargetCursor = this.targetCursor;
        this.cursor = new Cursor(right, this.cursor);
        this.targetCursor = otherWrapper ? new Cursor(otherWrapper, this.targetCursor) : this.targetCursor;
        try {
            await this.visitProperty(right.element, otherElement);
        } finally {
            this.cursor = savedCursor;
            this.targetCursor = savedTargetCursor;
        }

        return right;
    }

    /**
     * Override visitLeftPadded to compare only the elements, not markers or spacing.
     * The context parameter p contains the corresponding element from the other tree.
     * Pushes the wrapper onto the cursor stack so captures can access it.
     * Also updates targetCursor in parallel.
     */
    public async visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, p: J): Promise<J.LeftPadded<T>> {
        if (!this.match) {
            return left;
        }

        // Extract the other element if it's also a LeftPadded
        const isLeftPadded = (p as any).kind === J.Kind.LeftPadded;
        const otherWrapper = isLeftPadded ? (p as unknown) as J.LeftPadded<T> : undefined;
        const otherElement = isLeftPadded ? otherWrapper!.element : p;

        // Push wrappers onto both cursors, then compare only the elements, not markers or spacing
        const savedCursor = this.cursor;
        const savedTargetCursor = this.targetCursor;
        this.cursor = new Cursor(left, this.cursor);
        this.targetCursor = otherWrapper ? new Cursor(otherWrapper, this.targetCursor) : this.targetCursor;
        try {
            await this.visitProperty(left.element, otherElement);
        } finally {
            this.cursor = savedCursor;
            this.targetCursor = savedTargetCursor;
        }

        return left;
    }

    /**
     * Override visitContainer to compare only the elements, not markers or spacing.
     * The context parameter p contains the corresponding element from the other tree.
     * Pushes the wrapper onto the cursor stack so captures can access it.
     * Also updates targetCursor in parallel.
     */
    public async visitContainer<T extends J>(container: J.Container<T>, p: J): Promise<J.Container<T>> {
        if (!this.match) {
            return container;
        }

        // Extract the other elements if it's also a Container
        const isContainer = (p as any).kind === J.Kind.Container;
        const otherContainer = isContainer ? (p as unknown) as J.Container<T> : undefined;
        const otherElements: J.RightPadded<T>[] = isContainer ? otherContainer!.elements : (p as any);

        // Compare elements array length
        if (container.elements.length !== otherElements.length) {
            return this.abort(container);
        }

        // Push wrappers onto both cursors, then compare each element
        const savedCursor = this.cursor;
        const savedTargetCursor = this.targetCursor;
        this.cursor = new Cursor(container, this.cursor);
        this.targetCursor = otherContainer ? new Cursor(otherContainer, this.targetCursor) : this.targetCursor;
        try {
            for (let i = 0; i < container.elements.length; i++) {
                await this.visitProperty(container.elements[i], otherElements[i]);
                if (!this.match) {
                    return this.abort(container);
                }
            }
        } finally {
            this.cursor = savedCursor;
            this.targetCursor = savedTargetCursor;
        }

        return container;
    }

    /**
     * Overrides the visitBinary method to compare binary expressions.
     * 
     * @param binary The binary expression to visit
     * @param other The other binary expression to compare with
     * @returns The visited binary expression, or undefined if the visit was aborted
     */
    override async visitBinary(binary: J.Binary, other: J): Promise<J | undefined> {
        return this.visitElement(binary, other as J.Binary);
    }

    /**
     * Overrides the visitIdentifier method to compare identifiers.
     * 
     * @param identifier The identifier to visit
     * @param other The other identifier to compare with
     * @returns The visited identifier, or undefined if the visit was aborted
     */
    override async visitIdentifier(identifier: J.Identifier, other: J): Promise<J | undefined> {
        return this.visitElement(identifier, other as J.Identifier);
    }

    /**
     * Overrides the visitLiteral method to compare literals.
     * 
     * @param literal The literal to visit
     * @param other The other literal to compare with
     * @returns The visited literal, or undefined if the visit was aborted
     */
    override async visitLiteral(literal: J.Literal, other: J): Promise<J | undefined> {
        return this.visitElement(literal, other as J.Literal);
    }

    /**
     * Overrides the visitBlock method to compare blocks.
     * 
     * @param block The block to visit
     * @param other The other block to compare with
     * @returns The visited block, or undefined if the visit was aborted
     */
    override async visitBlock(block: J.Block, other: J): Promise<J | undefined> {
        return this.visitElement(block, other as J.Block);
    }

    /**
     * Overrides the visitJsCompilationUnit method to compare compilation units.
     * 
     * @param compilationUnit The compilation unit to visit
     * @param other The other compilation unit to compare with
     * @returns The visited compilation unit, or undefined if the visit was aborted
     */
    override async visitJsCompilationUnit(compilationUnit: JS.CompilationUnit, other: J): Promise<J | undefined> {
        return this.visitElement(compilationUnit, other as JS.CompilationUnit);
    }

    /**
     * Overrides the visitAlias method to compare aliases.
     * 
     * @param alias The alias to visit
     * @param other The other alias to compare with
     * @returns The visited alias, or undefined if the visit was aborted
     */
    override async visitAlias(alias: JS.Alias, other: J): Promise<J | undefined> {
        return this.visitElement(alias, other as JS.Alias);
    }

    /**
     * Overrides the visitArrowFunction method to compare arrow functions.
     * 
     * @param arrowFunction The arrow function to visit
     * @param other The other arrow function to compare with
     * @returns The visited arrow function, or undefined if the visit was aborted
     */
    override async visitArrowFunction(arrowFunction: JS.ArrowFunction, other: J): Promise<J | undefined> {
        return this.visitElement(arrowFunction, other as JS.ArrowFunction);
    }

    /**
     * Overrides the visitAwait method to compare await expressions.
     * 
     * @param await_ The await expression to visit
     * @param other The other await expression to compare with
     * @returns The visited await expression, or undefined if the visit was aborted
     */
    override async visitAwait(await_: JS.Await, other: J): Promise<J | undefined> {
        return this.visitElement(await_, other as JS.Await);
    }

    /**
     * Overrides the visitJsxTag method to compare JSX tags.
     * 
     * @param element The JSX tag to visit
     * @param other The other JSX tag to compare with
     * @returns The visited JSX tag, or undefined if the visit was aborted
     */
    override async visitJsxTag(element: JSX.Tag, other: J): Promise<J | undefined> {
        return this.visitElement(element, other as JSX.Tag);
    }

    /**
     * Overrides the visitJsxAttribute method to compare JSX attributes.
     * 
     * @param attribute The JSX attribute to visit
     * @param other The other JSX attribute to compare with
     * @returns The visited JSX attribute, or undefined if the visit was aborted
     */
    override async visitJsxAttribute(attribute: JSX.Attribute, other: J): Promise<J | undefined> {
        return this.visitElement(attribute, other as JSX.Attribute);
    }

    /**
     * Overrides the visitJsxSpreadAttribute method to compare JSX spread attributes.
     * 
     * @param spread The JSX spread attribute to visit
     * @param other The other JSX spread attribute to compare with
     * @returns The visited JSX spread attribute, or undefined if the visit was aborted
     */
    override async visitJsxSpreadAttribute(spread: JSX.SpreadAttribute, other: J): Promise<J | undefined> {
        return this.visitElement(spread, other as JSX.SpreadAttribute);
    }

    /**
     * Overrides the visitJsxExpression method to compare JSX expressions.
     * 
     * @param expr The JSX expression to visit
     * @param other The other JSX expression to compare with
     * @returns The visited JSX expression, or undefined if the visit was aborted
     */
    override async visitJsxEmbeddedExpression(expr: JSX.EmbeddedExpression, other: J): Promise<J | undefined> {
        return this.visitElement(expr, other as JSX.EmbeddedExpression);
    }

    /**
     * Overrides the visitJsxNamespacedName method to compare JSX namespaced names.
     * 
     * @param ns The JSX namespaced name to visit
     * @param other The other JSX namespaced name to compare with
     * @returns The visited JSX namespaced name, or undefined if the visit was aborted
     */
    override async visitJsxNamespacedName(ns: JSX.NamespacedName, other: J): Promise<J | undefined> {
        return this.visitElement(ns, other as JSX.NamespacedName);
    }

    /**
     * Overrides the visitConditionalType method to compare conditional types.
     * 
     * @param conditionalType The conditional type to visit
     * @param other The other conditional type to compare with
     * @returns The visited conditional type, or undefined if the visit was aborted
     */
    override async visitConditionalType(conditionalType: JS.ConditionalType, other: J): Promise<J | undefined> {
        return this.visitElement(conditionalType, other as JS.ConditionalType);
    }

    /**
     * Overrides the visitDelete method to compare delete expressions.
     * 
     * @param delete_ The delete expression to visit
     * @param other The other delete expression to compare with
     * @returns The visited delete expression, or undefined if the visit was aborted
     */
    override async visitDelete(delete_: JS.Delete, other: J): Promise<J | undefined> {
        return this.visitElement(delete_, other as JS.Delete);
    }

    /**
     * Overrides the visitExpressionStatement method to compare expression statements.
     * 
     * @param expressionStatement The expression statement to visit
     * @param other The other expression statement to compare with
     * @returns The visited expression statement, or undefined if the visit was aborted
     */
    override async visitExpressionStatement(expressionStatement: JS.ExpressionStatement, other: J): Promise<J | undefined> {
        return this.visitElement(expressionStatement, other as JS.ExpressionStatement);
    }

    /**
     * Overrides the visitExpressionWithTypeArguments method to compare expressions with type arguments.
     * 
     * @param expressionWithTypeArguments The expression with type arguments to visit
     * @param other The other expression with type arguments to compare with
     * @returns The visited expression with type arguments, or undefined if the visit was aborted
     */
    override async visitExpressionWithTypeArguments(expressionWithTypeArguments: JS.ExpressionWithTypeArguments, other: J): Promise<J | undefined> {
        return this.visitElement(expressionWithTypeArguments, other as JS.ExpressionWithTypeArguments);
    }

    /**
     * Overrides the visitFunctionCall method to compare method invocations.
     *
     * @param functionCall The function call to visit
     * @param other The other function call to compare with
     * @returns The visited function call, or undefined if the visit was aborted
     */
    override async visitFunctionCall(functionCall: JS.FunctionCall, other: J): Promise<J | undefined> {
        return this.visitElement(functionCall, other as JS.FunctionCall);
    }

    /**
     * Overrides the visitFunctionType method to compare function types.
     * 
     * @param functionType The function type to visit
     * @param other The other function type to compare with
     * @returns The visited function type, or undefined if the visit was aborted
     */
    override async visitFunctionType(functionType: JS.FunctionType, other: J): Promise<J | undefined> {
        return this.visitElement(functionType, other as JS.FunctionType);
    }

    /**
     * Overrides the visitInferType method to compare infer types.
     * 
     * @param inferType The infer type to visit
     * @param other The other infer type to compare with
     * @returns The visited infer type, or undefined if the visit was aborted
     */
    override async visitInferType(inferType: JS.InferType, other: J): Promise<J | undefined> {
        return this.visitElement(inferType, other as JS.InferType);
    }

    /**
     * Overrides the visitImportType method to compare import types.
     * 
     * @param importType The import type to visit
     * @param other The other import type to compare with
     * @returns The visited import type, or undefined if the visit was aborted
     */
    override async visitImportType(importType: JS.ImportType, other: J): Promise<J | undefined> {
        return this.visitElement(importType, other as JS.ImportType);
    }

    /**
     * Overrides the visitImportDeclaration method to compare import declarations.
     * 
     * @param jsImport The import declaration to visit
     * @param other The other import declaration to compare with
     * @returns The visited import declaration, or undefined if the visit was aborted
     */
    override async visitImportDeclaration(jsImport: JS.Import, other: J): Promise<J | undefined> {
        return this.visitElement(jsImport, other as JS.Import);
    }

    /**
     * Overrides the visitImportClause method to compare import clauses.
     * 
     * @param importClause The import clause to visit
     * @param other The other import clause to compare with
     * @returns The visited import clause, or undefined if the visit was aborted
     */
    override async visitImportClause(importClause: JS.ImportClause, other: J): Promise<J | undefined> {
        return this.visitElement(importClause, other as JS.ImportClause);
    }

    /**
     * Overrides the visitNamedImports method to compare named imports.
     * 
     * @param namedImports The named imports to visit
     * @param other The other named imports to compare with
     * @returns The visited named imports, or undefined if the visit was aborted
     */
    override async visitNamedImports(namedImports: JS.NamedImports, other: J): Promise<J | undefined> {
        return this.visitElement(namedImports, other as JS.NamedImports);
    }

    /**
     * Overrides the visitImportSpecifier method to compare import specifiers.
     * 
     * @param importSpecifier The import specifier to visit
     * @param other The other import specifier to compare with
     * @returns The visited import specifier, or undefined if the visit was aborted
     */
    override async visitImportSpecifier(importSpecifier: JS.ImportSpecifier, other: J): Promise<J | undefined> {
        return this.visitElement(importSpecifier, other as JS.ImportSpecifier);
    }

    /**
     * Overrides the visitImportAttributes method to compare import attributes.
     * 
     * @param importAttributes The import attributes to visit
     * @param other The other import attributes to compare with
     * @returns The visited import attributes, or undefined if the visit was aborted
     */
    override async visitImportAttributes(importAttributes: JS.ImportAttributes, other: J): Promise<J | undefined> {
        return this.visitElement(importAttributes, other as JS.ImportAttributes);
    }

    /**
     * Overrides the visitImportTypeAttributes method to compare import type attributes.
     * 
     * @param importTypeAttributes The import type attributes to visit
     * @param other The other import type attributes to compare with
     * @returns The visited import type attributes, or undefined if the visit was aborted
     */
    override async visitImportTypeAttributes(importTypeAttributes: JS.ImportTypeAttributes, other: J): Promise<J | undefined> {
        return this.visitElement(importTypeAttributes, other as JS.ImportTypeAttributes);
    }

    /**
     * Overrides the visitImportAttribute method to compare import attributes.
     * 
     * @param importAttribute The import attribute to visit
     * @param other The other import attribute to compare with
     * @returns The visited import attribute, or undefined if the visit was aborted
     */
    override async visitImportAttribute(importAttribute: JS.ImportAttribute, other: J): Promise<J | undefined> {
        return this.visitElement(importAttribute, other as JS.ImportAttribute);
    }

    /**
     * Overrides the visitBinaryExtensions method to compare binary expressions.
     * 
     * @param jsBinary The binary expression to visit
     * @param other The other binary expression to compare with
     * @returns The visited binary expression, or undefined if the visit was aborted
     */
    override async visitBinaryExtensions(jsBinary: JS.Binary, other: J): Promise<J | undefined> {
        return this.visitElement(jsBinary, other as JS.Binary);
    }

    /**
     * Overrides the visitLiteralType method to compare literal types.
     * 
     * @param literalType The literal type to visit
     * @param other The other literal type to compare with
     * @returns The visited literal type, or undefined if the visit was aborted
     */
    override async visitLiteralType(literalType: JS.LiteralType, other: J): Promise<J | undefined> {
        return this.visitElement(literalType, other as JS.LiteralType);
    }

    /**
     * Overrides the visitMappedType method to compare mapped types.
     * 
     * @param mappedType The mapped type to visit
     * @param other The other mapped type to compare with
     * @returns The visited mapped type, or undefined if the visit was aborted
     */
    override async visitMappedType(mappedType: JS.MappedType, other: J): Promise<J | undefined> {
        return this.visitElement(mappedType, other as JS.MappedType);
    }

    /**
     * Overrides the visitKeysRemapping method to compare keys remapping.
     * 
     * @param keysRemapping The keys remapping to visit
     * @param other The other keys remapping to compare with
     * @returns The visited keys remapping, or undefined if the visit was aborted
     */
    override async visitMappedTypeKeysRemapping(keysRemapping: JS.MappedType.KeysRemapping, other: J): Promise<J | undefined> {
        return this.visitElement(keysRemapping, other as JS.MappedType.KeysRemapping);
    }

    /**
     * Overrides the visitMappedTypeParameter method to compare mapped type parameters.
     * 
     * @param mappedTypeParameter The mapped type parameter to visit
     * @param other The other mapped type parameter to compare with
     * @returns The visited mapped type parameter, or undefined if the visit was aborted
     */
    override async visitMappedTypeParameter(mappedTypeParameter: JS.MappedType.Parameter, other: J): Promise<J | undefined> {
        return this.visitElement(mappedTypeParameter, other as JS.MappedType.Parameter);
    }

    /**
     * Overrides the visitObjectBindingPattern method to compare object binding declarations.
     * 
     * @param objectBindingPattern The object binding declarations to visit
     * @param other The other object binding declarations to compare with
     * @returns The visited object binding declarations, or undefined if the visit was aborted
     */
    override async visitObjectBindingPattern(objectBindingPattern: JS.ObjectBindingPattern, other: J): Promise<J | undefined> {
        return this.visitElement(objectBindingPattern, other as JS.ObjectBindingPattern);
    }

    /**
     * Overrides the visitPropertyAssignment method to compare property assignments.
     * 
     * @param propertyAssignment The property assignment to visit
     * @param other The other property assignment to compare with
     * @returns The visited property assignment, or undefined if the visit was aborted
     */
    override async visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, other: J): Promise<J | undefined> {
        return this.visitElement(propertyAssignment, other as JS.PropertyAssignment);
    }

    /**
     * Overrides the visitSatisfiesExpression method to compare satisfies expressions.
     * 
     * @param satisfiesExpression The satisfies expression to visit
     * @param other The other satisfies expression to compare with
     * @returns The visited satisfies expression, or undefined if the visit was aborted
     */
    override async visitSatisfiesExpression(satisfiesExpression: JS.SatisfiesExpression, other: J): Promise<J | undefined> {
        return this.visitElement(satisfiesExpression, other as JS.SatisfiesExpression);
    }

    /**
     * Overrides the visitScopedVariableDeclarations method to compare scoped variable declarations.
     * 
     * @param scopedVariableDeclarations The scoped variable declarations to visit
     * @param other The other scoped variable declarations to compare with
     * @returns The visited scoped variable declarations, or undefined if the visit was aborted
     */
    override async visitScopedVariableDeclarations(scopedVariableDeclarations: JS.ScopedVariableDeclarations, other: J): Promise<J | undefined> {
        return this.visitElement(scopedVariableDeclarations, other as JS.ScopedVariableDeclarations);
    }

    /**
     * Overrides the visitStatementExpression method to compare statement expressions.
     * 
     * @param statementExpression The statement expression to visit
     * @param other The other statement expression to compare with
     * @returns The visited statement expression, or undefined if the visit was aborted
     */
    override async visitStatementExpression(statementExpression: JS.StatementExpression, other: J): Promise<J | undefined> {
        return this.visitElement(statementExpression, other as JS.StatementExpression);
    }

    /**
     * Overrides the visitTaggedTemplateExpression method to compare tagged template expressions.
     * 
     * @param taggedTemplateExpression The tagged template expression to visit
     * @param other The other tagged template expression to compare with
     * @returns The visited tagged template expression, or undefined if the visit was aborted
     */
    override async visitTaggedTemplateExpression(taggedTemplateExpression: JS.TaggedTemplateExpression, other: J): Promise<J | undefined> {
        return this.visitElement(taggedTemplateExpression, other as JS.TaggedTemplateExpression);
    }

    /**
     * Overrides the visitTemplateExpression method to compare template expressions.
     * 
     * @param templateExpression The template expression to visit
     * @param other The other template expression to compare with
     * @returns The visited template expression, or undefined if the visit was aborted
     */
    override async visitTemplateExpression(templateExpression: JS.TemplateExpression, other: J): Promise<J | undefined> {
        return this.visitElement(templateExpression, other as JS.TemplateExpression);
    }

    /**
     * Overrides the visitTemplateExpressionSpan method to compare template expression spans.
     * 
     * @param span The template expression span to visit
     * @param other The other template expression span to compare with
     * @returns The visited template expression span, or undefined if the visit was aborted
     */
    override async visitTemplateExpressionSpan(span: JS.TemplateExpression.Span, other: J): Promise<J | undefined> {
        return this.visitElement(span, other as JS.TemplateExpression.Span);
    }

    /**
     * Overrides the visitTuple method to compare tuples.
     * 
     * @param tuple The tuple to visit
     * @param other The other tuple to compare with
     * @returns The visited tuple, or undefined if the visit was aborted
     */
    override async visitTuple(tuple: JS.Tuple, other: J): Promise<J | undefined> {
        return this.visitElement(tuple, other as JS.Tuple);
    }

    /**
     * Overrides the visitTypeDeclaration method to compare type declarations.
     * 
     * @param typeDeclaration The type declaration to visit
     * @param other The other type declaration to compare with
     * @returns The visited type declaration, or undefined if the visit was aborted
     */
    override async visitTypeDeclaration(typeDeclaration: JS.TypeDeclaration, other: J): Promise<J | undefined> {
        return this.visitElement(typeDeclaration, other as JS.TypeDeclaration);
    }

    /**
     * Overrides the visitTypeOf method to compare typeof expressions.
     * 
     * @param typeOf The typeof expression to visit
     * @param other The other typeof expression to compare with
     * @returns The visited typeof expression, or undefined if the visit was aborted
     */
    override async visitTypeOf(typeOf: JS.TypeOf, other: J): Promise<J | undefined> {
        return this.visitElement(typeOf, other as JS.TypeOf);
    }

    /**
     * Overrides the visitTypeTreeExpression method to compare type tree expressions.
     * 
     * @param typeTreeExpression The type tree expression to visit
     * @param other The other type tree expression to compare with
     * @returns The visited type tree expression, or undefined if the visit was aborted
     */
    override async visitTypeTreeExpression(typeTreeExpression: JS.TypeTreeExpression, other: J): Promise<J | undefined> {
        return this.visitElement(typeTreeExpression, other as JS.TypeTreeExpression);
    }

    /**
     * Overrides the visitAs method to compare as expressions.
     *
     * @param as_ The as expression to visit
     * @param other The other as expression to compare with
     * @returns The visited as expression, or undefined if the visit was aborted
     */
    override async visitAs(as_: JS.As, other: J): Promise<J | undefined> {
        return this.visitElement(as_, other as JS.As);
    }

    /**
     * Overrides the visitAssignmentOperationExtensions method to compare assignment operations.
     * 
     * @param assignmentOperation The assignment operation to visit
     * @param other The other assignment operation to compare with
     * @returns The visited assignment operation, or undefined if the visit was aborted
     */
    override async visitAssignmentOperationExtensions(assignmentOperation: JS.AssignmentOperation, other: J): Promise<J | undefined> {
        return this.visitElement(assignmentOperation, other as JS.AssignmentOperation);
    }

    /**
     * Overrides the visitIndexedAccessType method to compare indexed access types.
     * 
     * @param indexedAccessType The indexed access type to visit
     * @param other The other indexed access type to compare with
     * @returns The visited indexed access type, or undefined if the visit was aborted
     */
    override async visitIndexedAccessType(indexedAccessType: JS.IndexedAccessType, other: J): Promise<J | undefined> {
        return this.visitElement(indexedAccessType, other as JS.IndexedAccessType);
    }

    /**
     * Overrides the visitIndexType method to compare index types.
     * 
     * @param indexType The index type to visit
     * @param other The other index type to compare with
     * @returns The visited index type, or undefined if the visit was aborted
     */
    override async visitIndexedAccessTypeIndexType(indexType: JS.IndexedAccessType.IndexType, other: J): Promise<J | undefined> {
        return this.visitElement(indexType, other as JS.IndexedAccessType.IndexType);
    }

    /**
     * Overrides the visitTypeQuery method to compare type queries.
     * 
     * @param typeQuery The type query to visit
     * @param other The other type query to compare with
     * @returns The visited type query, or undefined if the visit was aborted
     */
    override async visitTypeQuery(typeQuery: JS.TypeQuery, other: J): Promise<J | undefined> {
        return this.visitElement(typeQuery, other as JS.TypeQuery);
    }

    /**
     * Overrides the visitTypeInfo method to compare type info.
     * 
     * @param typeInfo The type info to visit
     * @param other The other type info to compare with
     * @returns The visited type info, or undefined if the visit was aborted
     */
    override async visitTypeInfo(typeInfo: JS.TypeInfo, other: J): Promise<J | undefined> {
        return this.visitElement(typeInfo, other as JS.TypeInfo);
    }

    /**
     * Overrides the visitComputedPropertyName method to compare computed property names.
     * 
     * @param computedPropertyName The computed property name to visit
     * @param other The other computed property name to compare with
     * @returns The visited computed property name, or undefined if the visit was aborted
     */
    override async visitComputedPropertyName(computedPropertyName: JS.ComputedPropertyName, other: J): Promise<J | undefined> {
        return this.visitElement(computedPropertyName, other as JS.ComputedPropertyName);
    }

    /**
     * Overrides the visitTypeOperator method to compare type operators.
     * 
     * @param typeOperator The type operator to visit
     * @param other The other type operator to compare with
     * @returns The visited type operator, or undefined if the visit was aborted
     */
    override async visitTypeOperator(typeOperator: JS.TypeOperator, other: J): Promise<J | undefined> {
        return this.visitElement(typeOperator, other as JS.TypeOperator);
    }

    /**
     * Overrides the visitTypePredicate method to compare type predicates.
     * 
     * @param typePredicate The type predicate to visit
     * @param other The other type predicate to compare with
     * @returns The visited type predicate, or undefined if the visit was aborted
     */
    override async visitTypePredicate(typePredicate: JS.TypePredicate, other: J): Promise<J | undefined> {
        return this.visitElement(typePredicate, other as JS.TypePredicate);
    }

    /**
     * Overrides the visitUnion method to compare unions.
     * 
     * @param union The union to visit
     * @param other The other union to compare with
     * @returns The visited union, or undefined if the visit was aborted
     */
    override async visitUnion(union: JS.Union, other: J): Promise<J | undefined> {
        return this.visitElement(union, other as JS.Union);
    }

    /**
     * Overrides the visitIntersection method to compare intersections.
     * 
     * @param intersection The intersection to visit
     * @param other The other intersection to compare with
     * @returns The visited intersection, or undefined if the visit was aborted
     */
    override async visitIntersection(intersection: JS.Intersection, other: J): Promise<J | undefined> {
        return this.visitElement(intersection, other as JS.Intersection);
    }

    /**
     * Overrides the visitAnnotatedType method to compare annotated types.
     * 
     * @param annotatedType The annotated type to visit
     * @param other The other annotated type to compare with
     * @returns The visited annotated type, or undefined if the visit was aborted
     */
    override async visitAnnotatedType(annotatedType: J.AnnotatedType, other: J): Promise<J | undefined> {
        return this.visitElement(annotatedType, other as J.AnnotatedType);
    }

    /**
     * Overrides the visitAnnotation method to compare annotations.
     * 
     * @param annotation The annotation to visit
     * @param other The other annotation to compare with
     * @returns The visited annotation, or undefined if the visit was aborted
     */
    override async visitAnnotation(annotation: J.Annotation, other: J): Promise<J | undefined> {
        return this.visitElement(annotation, other as J.Annotation);
    }

    /**
     * Overrides the visitArrayAccess method to compare array access expressions.
     * 
     * @param arrayAccess The array access expression to visit
     * @param other The other array access expression to compare with
     * @returns The visited array access expression, or undefined if the visit was aborted
     */
    override async visitArrayAccess(arrayAccess: J.ArrayAccess, other: J): Promise<J | undefined> {
        return this.visitElement(arrayAccess, other as J.ArrayAccess);
    }

    /**
     * Overrides the visitArrayDimension method to compare array dimensions.
     * 
     * @param arrayDimension The array dimension to visit
     * @param other The other array dimension to compare with
     * @returns The visited array dimension, or undefined if the visit was aborted
     */
    override async visitArrayDimension(arrayDimension: J.ArrayDimension, other: J): Promise<J | undefined> {
        return this.visitElement(arrayDimension, other as J.ArrayDimension);
    }

    /**
     * Overrides the visitArrayType method to compare array types.
     * 
     * @param arrayType The array type to visit
     * @param other The other array type to compare with
     * @returns The visited array type, or undefined if the visit was aborted
     */
    override async visitArrayType(arrayType: J.ArrayType, other: J): Promise<J | undefined> {
        return this.visitElement(arrayType, other as J.ArrayType);
    }

    /**
     * Overrides the visitAssert method to compare assert statements.
     * 
     * @param anAssert The assert statement to visit
     * @param other The other assert statement to compare with
     * @returns The visited assert statement, or undefined if the visit was aborted
     */
    override async visitAssert(anAssert: J.Assert, other: J): Promise<J | undefined> {
        return this.visitElement(anAssert, other as J.Assert);
    }

    /**
     * Overrides the visitAssignment method to compare assignment expressions.
     * 
     * @param assignment The assignment expression to visit
     * @param other The other assignment expression to compare with
     * @returns The visited assignment expression, or undefined if the visit was aborted
     */
    override async visitAssignment(assignment: J.Assignment, other: J): Promise<J | undefined> {
        return this.visitElement(assignment, other as J.Assignment);
    }

    /**
     * Overrides the visitAssignmentOperation method to compare assignment operation expressions.
     * 
     * @param assignOp The assignment operation expression to visit
     * @param other The other assignment operation expression to compare with
     * @returns The visited assignment operation expression, or undefined if the visit was aborted
     */
    override async visitAssignmentOperation(assignOp: J.AssignmentOperation, other: J): Promise<J | undefined> {
        return this.visitElement(assignOp, other as J.AssignmentOperation);
    }

    /**
     * Overrides the visitBreak method to compare break statements.
     * 
     * @param breakStatement The break statement to visit
     * @param other The other break statement to compare with
     * @returns The visited break statement, or undefined if the visit was aborted
     */
    override async visitBreak(breakStatement: J.Break, other: J): Promise<J | undefined> {
        return this.visitElement(breakStatement, other as J.Break);
    }

    /**
     * Overrides the visitCase method to compare case statements.
     * 
     * @param aCase The case statement to visit
     * @param other The other case statement to compare with
     * @returns The visited case statement, or undefined if the visit was aborted
     */
    override async visitCase(aCase: J.Case, other: J): Promise<J | undefined> {
        return this.visitElement(aCase, other as J.Case);
    }

    /**
     * Overrides the visitClassDeclaration method to compare class declarations.
     * 
     * @param classDecl The class declaration to visit
     * @param other The other class declaration to compare with
     * @returns The visited class declaration, or undefined if the visit was aborted
     */
    override async visitClassDeclaration(classDecl: J.ClassDeclaration, other: J): Promise<J | undefined> {
        return this.visitElement(classDecl, other as J.ClassDeclaration);
    }

    /**
     * Overrides the visitClassDeclarationKind method to compare class declaration kinds.
     * 
     * @param kind The class declaration kind to visit
     * @param other The other class declaration kind to compare with
     * @returns The visited class declaration kind, or undefined if the visit was aborted
     */
    override async visitClassDeclarationKind(kind: J.ClassDeclaration.Kind, other: J): Promise<J | undefined> {
        return this.visitElement(kind, other as J.ClassDeclaration.Kind);
    }

    /**
     * Overrides the visitCompilationUnit method to compare compilation units.
     * 
     * @param compilationUnit The compilation unit to visit
     * @param other The other compilation unit to compare with
     * @returns The visited compilation unit, or undefined if the visit was aborted
     */
    override async visitCompilationUnit(compilationUnit: J.CompilationUnit, other: J): Promise<J | undefined> {
        return this.visitElement(compilationUnit, other as J.CompilationUnit);
    }

    /**
     * Overrides the visitContinue method to compare continue statements.
     * 
     * @param continueStatement The continue statement to visit
     * @param other The other continue statement to compare with
     * @returns The visited continue statement, or undefined if the visit was aborted
     */
    override async visitContinue(continueStatement: J.Continue, other: J): Promise<J | undefined> {
        return this.visitElement(continueStatement, other as J.Continue);
    }

    /**
     * Overrides the visitControlParentheses method to compare control parentheses.
     * 
     * @param controlParens The control parentheses to visit
     * @param other The other control parentheses to compare with
     * @returns The visited control parentheses, or undefined if the visit was aborted
     */
    override async visitControlParentheses<T extends J>(controlParens: J.ControlParentheses<T>, other: J): Promise<J | undefined> {
        return this.visitElement(controlParens, other as J.ControlParentheses<T>);
    }

    /**
     * Overrides the visitDeconstructionPattern method to compare deconstruction patterns.
     * 
     * @param pattern The deconstruction pattern to visit
     * @param other The other deconstruction pattern to compare with
     * @returns The visited deconstruction pattern, or undefined if the visit was aborted
     */
    override async visitDeconstructionPattern(pattern: J.DeconstructionPattern, other: J): Promise<J | undefined> {
        return this.visitElement(pattern, other as J.DeconstructionPattern);
    }

    /**
     * Overrides the visitDoWhileLoop method to compare do-while loops.
     * 
     * @param doWhileLoop The do-while loop to visit
     * @param other The other do-while loop to compare with
     * @returns The visited do-while loop, or undefined if the visit was aborted
     */
    override async visitDoWhileLoop(doWhileLoop: J.DoWhileLoop, other: J): Promise<J | undefined> {
        return this.visitElement(doWhileLoop, other as J.DoWhileLoop);
    }

    /**
     * Overrides the visitEmpty method to compare empty statements.
     * 
     * @param empty The empty statement to visit
     * @param other The other empty statement to compare with
     * @returns The visited empty statement, or undefined if the visit was aborted
     */
    override async visitEmpty(empty: J.Empty, other: J): Promise<J | undefined> {
        return this.visitElement(empty, other as J.Empty);
    }

    /**
     * Overrides the visitEnumValue method to compare enum values.
     * 
     * @param enumValue The enum value to visit
     * @param other The other enum value to compare with
     * @returns The visited enum value, or undefined if the visit was aborted
     */
    override async visitEnumValue(enumValue: J.EnumValue, other: J): Promise<J | undefined> {
        return this.visitElement(enumValue, other as J.EnumValue);
    }

    /**
     * Overrides the visitEnumValueSet method to compare enum value sets.
     * 
     * @param enumValueSet The enum value set to visit
     * @param other The other enum value set to compare with
     * @returns The visited enum value set, or undefined if the visit was aborted
     */
    override async visitEnumValueSet(enumValueSet: J.EnumValueSet, other: J): Promise<J | undefined> {
        return this.visitElement(enumValueSet, other as J.EnumValueSet);
    }

    /**
     * Overrides the visitErroneous method to compare erroneous nodes.
     * 
     * @param erroneous The erroneous node to visit
     * @param other The other erroneous node to compare with
     * @returns The visited erroneous node, or undefined if the visit was aborted
     */
    override async visitErroneous(erroneous: J.Erroneous, other: J): Promise<J | undefined> {
        return this.visitElement(erroneous, other as J.Erroneous);
    }

    /**
     * Overrides the visitFieldAccess method to compare field access expressions.
     * 
     * @param fieldAccess The field access expression to visit
     * @param other The other field access expression to compare with
     * @returns The visited field access expression, or undefined if the visit was aborted
     */
    override async visitFieldAccess(fieldAccess: J.FieldAccess, other: J): Promise<J | undefined> {
        return this.visitElement(fieldAccess, other as J.FieldAccess);
    }

    /**
     * Overrides the visitForEachLoop method to compare for-each loops.
     * 
     * @param forEachLoop The for-each loop to visit
     * @param other The other for-each loop to compare with
     * @returns The visited for-each loop, or undefined if the visit was aborted
     */
    override async visitForEachLoop(forEachLoop: J.ForEachLoop, other: J): Promise<J | undefined> {
        return this.visitElement(forEachLoop, other as J.ForEachLoop);
    }

    /**
     * Overrides the visitForEachLoopControl method to compare for-each loop controls.
     * 
     * @param control The for-each loop control to visit
     * @param other The other for-each loop control to compare with
     * @returns The visited for-each loop control, or undefined if the visit was aborted
     */
    override async visitForEachLoopControl(control: J.ForEachLoop.Control, other: J): Promise<J | undefined> {
        return this.visitElement(control, other as J.ForEachLoop.Control);
    }

    /**
     * Overrides the visitForLoop method to compare for loops.
     * 
     * @param forLoop The for loop to visit
     * @param other The other for loop to compare with
     * @returns The visited for loop, or undefined if the visit was aborted
     */
    override async visitForLoop(forLoop: J.ForLoop, other: J): Promise<J | undefined> {
        return this.visitElement(forLoop, other as J.ForLoop);
    }

    /**
     * Overrides the visitForLoopControl method to compare for loop controls.
     * 
     * @param control The for loop control to visit
     * @param other The other for loop control to compare with
     * @returns The visited for loop control, or undefined if the visit was aborted
     */
    override async visitForLoopControl(control: J.ForLoop.Control, other: J): Promise<J | undefined> {
        return this.visitElement(control, other as J.ForLoop.Control);
    }

    /**
     * Overrides the visitIf method to compare if statements.
     * 
     * @param ifStatement The if statement to visit
     * @param other The other if statement to compare with
     * @returns The visited if statement, or undefined if the visit was aborted
     */
    override async visitIf(ifStatement: J.If, other: J): Promise<J | undefined> {
        return this.visitElement(ifStatement, other as J.If);
    }

    /**
     * Overrides the visitElse method to compare else statements.
     * 
     * @param elseStatement The else statement to visit
     * @param other The other else statement to compare with
     * @returns The visited else statement, or undefined if the visit was aborted
     */
    override async visitElse(elseStatement: J.If.Else, other: J): Promise<J | undefined> {
        return this.visitElement(elseStatement, other as J.If.Else);
    }

    /**
     * Overrides the visitImport method to compare import statements.
     * 
     * @param importStatement The import statement to visit
     * @param other The other import statement to compare with
     * @returns The visited import statement, or undefined if the visit was aborted
     */
    override async visitImport(importStatement: J.Import, other: J): Promise<J | undefined> {
        return this.visitElement(importStatement, other as J.Import);
    }

    /**
     * Overrides the visitInstanceOf method to compare instanceof expressions.
     * 
     * @param instanceOf The instanceof expression to visit
     * @param other The other instanceof expression to compare with
     * @returns The visited instanceof expression, or undefined if the visit was aborted
     */
    override async visitInstanceOf(instanceOf: J.InstanceOf, other: J): Promise<J | undefined> {
        return this.visitElement(instanceOf, other as J.InstanceOf);
    }

    /**
     * Overrides the visitIntersectionType method to compare intersection types.
     * 
     * @param intersectionType The intersection type to visit
     * @param other The other intersection type to compare with
     * @returns The visited intersection type, or undefined if the visit was aborted
     */
    override async visitIntersectionType(intersectionType: J.IntersectionType, other: J): Promise<J | undefined> {
        return this.visitElement(intersectionType, other as J.IntersectionType);
    }

    /**
     * Overrides the visitLabel method to compare label statements.
     * 
     * @param label The label statement to visit
     * @param other The other label statement to compare with
     * @returns The visited label statement, or undefined if the visit was aborted
     */
    override async visitLabel(label: J.Label, other: J): Promise<J | undefined> {
        return this.visitElement(label, other as J.Label);
    }

    /**
     * Overrides the visitLambda method to compare lambda expressions.
     * 
     * @param lambda The lambda expression to visit
     * @param other The other lambda expression to compare with
     * @returns The visited lambda expression, or undefined if the visit was aborted
     */
    override async visitLambda(lambda: J.Lambda, other: J): Promise<J | undefined> {
        return this.visitElement(lambda, other as J.Lambda);
    }

    /**
     * Overrides the visitLambdaParameters method to compare lambda parameters.
     * 
     * @param parameters The lambda parameters to visit
     * @param other The other lambda parameters to compare with
     * @returns The visited lambda parameters, or undefined if the visit was aborted
     */
    override async visitLambdaParameters(parameters: J.Lambda.Parameters, other: J): Promise<J | undefined> {
        return this.visitElement(parameters, other as J.Lambda.Parameters);
    }

    /**
     * Overrides the visitMemberReference method to compare member references.
     * 
     * @param memberReference The member reference to visit
     * @param other The other member reference to compare with
     * @returns The visited member reference, or undefined if the visit was aborted
     */
    override async visitMemberReference(memberReference: J.MemberReference, other: J): Promise<J | undefined> {
        return this.visitElement(memberReference, other as J.MemberReference);
    }

    /**
     * Overrides the visitMethodDeclaration method to compare method declarations.
     * 
     * @param methodDeclaration The method declaration to visit
     * @param other The other method declaration to compare with
     * @returns The visited method declaration, or undefined if the visit was aborted
     */
    override async visitMethodDeclaration(methodDeclaration: J.MethodDeclaration, other: J): Promise<J | undefined> {
        return this.visitElement(methodDeclaration, other as J.MethodDeclaration);
    }

    /**
     * Overrides the visitMethodInvocation method to compare method invocations.
     * 
     * @param methodInvocation The method invocation to visit
     * @param other The other method invocation to compare with
     * @returns The visited method invocation, or undefined if the visit was aborted
     */
    override async visitMethodInvocation(methodInvocation: J.MethodInvocation, other: J): Promise<J | undefined> {
        return this.visitElement(methodInvocation, other as J.MethodInvocation);
    }

    /**
     * Overrides the visitModifier method to compare modifiers.
     * 
     * @param modifier The modifier to visit
     * @param other The other modifier to compare with
     * @returns The visited modifier, or undefined if the visit was aborted
     */
    override async visitModifier(modifier: J.Modifier, other: J): Promise<J | undefined> {
        return this.visitElement(modifier, other as J.Modifier);
    }

    /**
     * Overrides the visitMultiCatch method to compare multi-catch expressions.
     * 
     * @param multiCatch The multi-catch expression to visit
     * @param other The other multi-catch expression to compare with
     * @returns The visited multi-catch expression, or undefined if the visit was aborted
     */
    override async visitMultiCatch(multiCatch: J.MultiCatch, other: J): Promise<J | undefined> {
        return this.visitElement(multiCatch, other as J.MultiCatch);
    }

    /**
     * Overrides the visitNewArray method to compare new array expressions.
     * 
     * @param newArray The new array expression to visit
     * @param other The other new array expression to compare with
     * @returns The visited new array expression, or undefined if the visit was aborted
     */
    override async visitNewArray(newArray: J.NewArray, other: J): Promise<J | undefined> {
        return this.visitElement(newArray, other as J.NewArray);
    }

    /**
     * Overrides the visitNewClass method to compare new class expressions.
     * 
     * @param newClass The new class expression to visit
     * @param other The other new class expression to compare with
     * @returns The visited new class expression, or undefined if the visit was aborted
     */
    override async visitNewClass(newClass: J.NewClass, other: J): Promise<J | undefined> {
        return this.visitElement(newClass, other as J.NewClass);
    }

    /**
     * Overrides the visitNullableType method to compare nullable types.
     * 
     * @param nullableType The nullable type to visit
     * @param other The other nullable type to compare with
     * @returns The visited nullable type, or undefined if the visit was aborted
     */
    override async visitNullableType(nullableType: J.NullableType, other: J): Promise<J | undefined> {
        return this.visitElement(nullableType, other as J.NullableType);
    }

    /**
     * Overrides the visitPackage method to compare package declarations.
     * 
     * @param packageDeclaration The package declaration to visit
     * @param other The other package declaration to compare with
     * @returns The visited package declaration, or undefined if the visit was aborted
     */
    override async visitPackage(packageDeclaration: J.Package, other: J): Promise<J | undefined> {
        return this.visitElement(packageDeclaration, other as J.Package);
    }

    /**
     * Overrides the visitParameterizedType method to compare parameterized types.
     * 
     * @param parameterizedType The parameterized type to visit
     * @param other The other parameterized type to compare with
     * @returns The visited parameterized type, or undefined if the visit was aborted
     */
    override async visitParameterizedType(parameterizedType: J.ParameterizedType, other: J): Promise<J | undefined> {
        return this.visitElement(parameterizedType, other as J.ParameterizedType);
    }

    /**
     * Overrides the visitParentheses method to compare parentheses expressions.
     * 
     * @param parentheses The parentheses expression to visit
     * @param other The other parentheses expression to compare with
     * @returns The visited parentheses expression, or undefined if the visit was aborted
     */
    override async visitParentheses(parentheses: J.Parentheses<J>, other: J): Promise<J | undefined> {
        return this.visitElement(parentheses, other as J.Parentheses<J>);
    }

    /**
     * Overrides the visitParenthesizedTypeTree method to compare parenthesized type trees.
     * 
     * @param parenthesizedTypeTree The parenthesized type tree to visit
     * @param other The other parenthesized type tree to compare with
     * @returns The visited parenthesized type tree, or undefined if the visit was aborted
     */
    override async visitParenthesizedTypeTree(parenthesizedTypeTree: J.ParenthesizedTypeTree, other: J): Promise<J | undefined> {
        return this.visitElement(parenthesizedTypeTree, other as J.ParenthesizedTypeTree);
    }

    /**
     * Overrides the visitPrimitive method to compare primitive types.
     * 
     * @param primitive The primitive type to visit
     * @param other The other primitive type to compare with
     * @returns The visited primitive type, or undefined if the visit was aborted
     */
    override async visitPrimitive(primitive: J.Primitive, other: J): Promise<J | undefined> {
        return this.visitElement(primitive, other as J.Primitive);
    }

    /**
     * Overrides the visitReturn method to compare return statements.
     * 
     * @param returnStatement The return statement to visit
     * @param other The other return statement to compare with
     * @returns The visited return statement, or undefined if the visit was aborted
     */
    override async visitReturn(returnStatement: J.Return, other: J): Promise<J | undefined> {
        return this.visitElement(returnStatement, other as J.Return);
    }

    /**
     * Overrides the visitSwitch method to compare switch statements.
     * 
     * @param switchStatement The switch statement to visit
     * @param other The other switch statement to compare with
     * @returns The visited switch statement, or undefined if the visit was aborted
     */
    override async visitSwitch(switchStatement: J.Switch, other: J): Promise<J | undefined> {
        return this.visitElement(switchStatement, other as J.Switch);
    }

    /**
     * Overrides the visitSwitchExpression method to compare switch expressions.
     * 
     * @param switchExpression The switch expression to visit
     * @param other The other switch expression to compare with
     * @returns The visited switch expression, or undefined if the visit was aborted
     */
    override async visitSwitchExpression(switchExpression: J.SwitchExpression, other: J): Promise<J | undefined> {
        return this.visitElement(switchExpression, other as J.SwitchExpression);
    }

    /**
     * Overrides the visitSynchronized method to compare synchronized statements.
     * 
     * @param synchronizedStatement The synchronized statement to visit
     * @param other The other synchronized statement to compare with
     * @returns The visited synchronized statement, or undefined if the visit was aborted
     */
    override async visitSynchronized(synchronizedStatement: J.Synchronized, other: J): Promise<J | undefined> {
        return this.visitElement(synchronizedStatement, other as J.Synchronized);
    }

    /**
     * Overrides the visitTernary method to compare ternary expressions.
     * 
     * @param ternary The ternary expression to visit
     * @param other The other ternary expression to compare with
     * @returns The visited ternary expression, or undefined if the visit was aborted
     */
    override async visitTernary(ternary: J.Ternary, other: J): Promise<J | undefined> {
        return this.visitElement(ternary, other as J.Ternary);
    }

    /**
     * Overrides the visitThrow method to compare throw statements.
     * 
     * @param throwStatement The throw statement to visit
     * @param other The other throw statement to compare with
     * @returns The visited throw statement, or undefined if the visit was aborted
     */
    override async visitThrow(throwStatement: J.Throw, other: J): Promise<J | undefined> {
        return this.visitElement(throwStatement, other as J.Throw);
    }

    /**
     * Overrides the visitTry method to compare try statements.
     * 
     * @param tryStatement The try statement to visit
     * @param other The other try statement to compare with
     * @returns The visited try statement, or undefined if the visit was aborted
     */
    override async visitTry(tryStatement: J.Try, other: J): Promise<J | undefined> {
        return this.visitElement(tryStatement, other as J.Try);
    }

    /**
     * Overrides the visitTryResource method to compare try resources.
     * 
     * @param resource The try resource to visit
     * @param other The other try resource to compare with
     * @returns The visited try resource, or undefined if the visit was aborted
     */
    override async visitTryResource(resource: J.Try.Resource, other: J): Promise<J | undefined> {
        return this.visitElement(resource, other as J.Try.Resource);
    }

    /**
     * Overrides the visitTryCatch method to compare try catch blocks.
     * 
     * @param tryCatch The try catch block to visit
     * @param other The other try catch block to compare with
     * @returns The visited try catch block, or undefined if the visit was aborted
     */
    override async visitTryCatch(tryCatch: J.Try.Catch, other: J): Promise<J | undefined> {
        return this.visitElement(tryCatch, other as J.Try.Catch);
    }

    /**
     * Overrides the visitTypeCast method to compare type cast expressions.
     * 
     * @param typeCast The type cast expression to visit
     * @param other The other type cast expression to compare with
     * @returns The visited type cast expression, or undefined if the visit was aborted
     */
    override async visitTypeCast(typeCast: J.TypeCast, other: J): Promise<J | undefined> {
        return this.visitElement(typeCast, other as J.TypeCast);
    }

    /**
     * Overrides the visitTypeParameter method to compare type parameters.
     * 
     * @param typeParameter The type parameter to visit
     * @param other The other type parameter to compare with
     * @returns The visited type parameter, or undefined if the visit was aborted
     */
    override async visitTypeParameter(typeParameter: J.TypeParameter, other: J): Promise<J | undefined> {
        return this.visitElement(typeParameter, other as J.TypeParameter);
    }

    /**
     * Overrides the visitTypeParameters method to compare type parameters.
     * 
     * @param typeParameters The type parameters to visit
     * @param other The other type parameters to compare with
     * @returns The visited type parameters, or undefined if the visit was aborted
     */
    override async visitTypeParameters(typeParameters: J.TypeParameters, other: J): Promise<J | undefined> {
        return this.visitElement(typeParameters, other as J.TypeParameters);
    }

    /**
     * Overrides the visitUnary method to compare unary expressions.
     * 
     * @param unary The unary expression to visit
     * @param other The other unary expression to compare with
     * @returns The visited unary expression, or undefined if the visit was aborted
     */
    override async visitUnary(unary: J.Unary, other: J): Promise<J | undefined> {
        return this.visitElement(unary, other as J.Unary);
    }

    /**
     * Overrides the visitUnknown method to compare unknown nodes.
     * 
     * @param unknown The unknown node to visit
     * @param other The other unknown node to compare with
     * @returns The visited unknown node, or undefined if the visit was aborted
     */
    override async visitUnknown(unknown: J.Unknown, other: J): Promise<J | undefined> {
        return this.visitElement(unknown, other as J.Unknown);
    }

    /**
     * Overrides the visitUnknownSource method to compare unknown sources.
     * 
     * @param unknownSource The unknown source to visit
     * @param other The other unknown source to compare with
     * @returns The visited unknown source, or undefined if the visit was aborted
     */
    override async visitUnknownSource(unknownSource: J.UnknownSource, other: J): Promise<J | undefined> {
        return this.visitElement(unknownSource, other as J.UnknownSource);
    }

    /**
     * Overrides the visitVariableDeclarations method to compare variable declarations.
     * 
     * @param variableDeclarations The variable declarations to visit
     * @param other The other variable declarations to compare with
     * @returns The visited variable declarations, or undefined if the visit was aborted
     */
    override async visitVariableDeclarations(variableDeclarations: J.VariableDeclarations, other: J): Promise<J | undefined> {
        return this.visitElement(variableDeclarations, other as J.VariableDeclarations);
    }

    /**
     * Overrides the visitVariable method to compare variable declarations.
     * 
     * @param variable The variable declaration to visit
     * @param other The other variable declaration to compare with
     * @returns The visited variable declaration, or undefined if the visit was aborted
     */
    override async visitVariable(variable: J.VariableDeclarations.NamedVariable, other: J): Promise<J | undefined> {
        return this.visitElement(variable, other as J.VariableDeclarations.NamedVariable);
    }

    /**
     * Overrides the visitWhileLoop method to compare while loops.
     * 
     * @param whileLoop The while loop to visit
     * @param other The other while loop to compare with
     * @returns The visited while loop, or undefined if the visit was aborted
     */
    override async visitWhileLoop(whileLoop: J.WhileLoop, other: J): Promise<J | undefined> {
        return this.visitElement(whileLoop, other as J.WhileLoop);
    }

    /**
     * Overrides the visitWildcard method to compare wildcards.
     * 
     * @param wildcard The wildcard to visit
     * @param other The other wildcard to compare with
     * @returns The visited wildcard, or undefined if the visit was aborted
     */
    override async visitWildcard(wildcard: J.Wildcard, other: J): Promise<J | undefined> {
        return this.visitElement(wildcard, other as J.Wildcard);
    }

    /**
     * Overrides the visitYield method to compare yield statements.
     * 
     * @param yieldStatement The yield statement to visit
     * @param other The other yield statement to compare with
     * @returns The visited yield statement, or undefined if the visit was aborted
     */
    override async visitYield(yieldStatement: J.Yield, other: J): Promise<J | undefined> {
        return this.visitElement(yieldStatement, other as J.Yield);
    }

    /**
     * Overrides the visitVoid method to compare void expressions.
     * 
     * @param void_ The void expression to visit
     * @param other The other void expression to compare with
     * @returns The visited void expression, or undefined if the visit was aborted
     */
    override async visitVoid(void_: JS.Void, other: J): Promise<J | undefined> {
        return this.visitElement(void_, other as JS.Void);
    }

    /**
     * Overrides the visitWithStatement method to compare with statements.
     * 
     * @param withStatement The with statement to visit
     * @param other The other with statement to compare with
     * @returns The visited with statement, or undefined if the visit was aborted
     */
    override async visitWithStatement(withStatement: JS.WithStatement, other: J): Promise<J | undefined> {
        return this.visitElement(withStatement, other as JS.WithStatement);
    }

    /**
     * Overrides the visitIndexSignatureDeclaration method to compare index signature declarations.
     * 
     * @param indexSignatureDeclaration The index signature declaration to visit
     * @param other The other index signature declaration to compare with
     * @returns The visited index signature declaration, or undefined if the visit was aborted
     */
    override async visitIndexSignatureDeclaration(indexSignatureDeclaration: JS.IndexSignatureDeclaration, other: J): Promise<J | undefined> {
        return this.visitElement(indexSignatureDeclaration, other as JS.IndexSignatureDeclaration);
    }

    /**
     * Overrides the visitForOfLoop method to compare for-of loops.
     * 
     * @param forOfLoop The for-of loop to visit
     * @param other The other for-of loop to compare with
     * @returns The visited for-of loop, or undefined if the visit was aborted
     */
    override async visitForOfLoop(forOfLoop: JS.ForOfLoop, other: J): Promise<J | undefined> {
        return this.visitElement(forOfLoop, other as JS.ForOfLoop);
    }

    /**
     * Overrides the visitForInLoop method to compare for-in loops.
     * 
     * @param forInLoop The for-in loop to visit
     * @param other The other for-in loop to compare with
     * @returns The visited for-in loop, or undefined if the visit was aborted
     */
    override async visitForInLoop(forInLoop: JS.ForInLoop, other: J): Promise<J | undefined> {
        return this.visitElement(forInLoop, other as JS.ForInLoop);
    }

    /**
     * Overrides the visitNamespaceDeclaration method to compare namespace declarations.
     * 
     * @param namespaceDeclaration The namespace declaration to visit
     * @param other The other namespace declaration to compare with
     * @returns The visited namespace declaration, or undefined if the visit was aborted
     */
    override async visitNamespaceDeclaration(namespaceDeclaration: JS.NamespaceDeclaration, other: J): Promise<J | undefined> {
        return this.visitElement(namespaceDeclaration, other as JS.NamespaceDeclaration);
    }

    /**
     * Overrides the visitTypeLiteral method to compare type literals.
     * 
     * @param typeLiteral The type literal to visit
     * @param other The other type literal to compare with
     * @returns The visited type literal, or undefined if the visit was aborted
     */
    override async visitTypeLiteral(typeLiteral: JS.TypeLiteral, other: J): Promise<J | undefined> {
        return this.visitElement(typeLiteral, other as JS.TypeLiteral);
    }

    /**
     * Overrides the visitBindingElement method to compare binding elements.
     * 
     * @param bindingElement The binding element to visit
     * @param other The other binding element to compare with
     * @returns The visited binding element, or undefined if the visit was aborted
     */
    override async visitBindingElement(bindingElement: JS.BindingElement, other: J): Promise<J | undefined> {
        return this.visitElement(bindingElement, other as JS.BindingElement);
    }

    /**
     * Overrides the visitArrayBindingPattern method to compare array binding patterns.
     * 
     * @param arrayBindingPattern The array binding pattern to visit
     * @param other The other array binding pattern to compare with
     * @returns The visited array binding pattern, or undefined if the visit was aborted
     */
    override async visitArrayBindingPattern(arrayBindingPattern: JS.ArrayBindingPattern, other: J): Promise<J | undefined> {
        return this.visitElement(arrayBindingPattern, other as JS.ArrayBindingPattern);
    }

    /**
     * Overrides the visitExportDeclaration method to compare export declarations.
     * 
     * @param exportDeclaration The export declaration to visit
     * @param other The other export declaration to compare with
     * @returns The visited export declaration, or undefined if the visit was aborted
     */
    override async visitExportDeclaration(exportDeclaration: JS.ExportDeclaration, other: J): Promise<J | undefined> {
        return this.visitElement(exportDeclaration, other as JS.ExportDeclaration);
    }

    /**
     * Overrides the visitExportAssignment method to compare export assignments.
     * 
     * @param exportAssignment The export assignment to visit
     * @param other The other export assignment to compare with
     * @returns The visited export assignment, or undefined if the visit was aborted
     */
    override async visitExportAssignment(exportAssignment: JS.ExportAssignment, other: J): Promise<J | undefined> {
        return this.visitElement(exportAssignment, other as JS.ExportAssignment);
    }

    /**
     * Overrides the visitNamedExports method to compare named exports.
     * 
     * @param namedExports The named exports to visit
     * @param other The other named exports to compare with
     * @returns The visited named exports, or undefined if the visit was aborted
     */
    override async visitNamedExports(namedExports: JS.NamedExports, other: J): Promise<J | undefined> {
        return this.visitElement(namedExports, other as JS.NamedExports);
    }

    /**
     * Overrides the visitExportSpecifier method to compare export specifiers.
     * 
     * @param exportSpecifier The export specifier to visit
     * @param other The other export specifier to compare with
     * @returns The visited export specifier, or undefined if the visit was aborted
     */
    override async visitExportSpecifier(exportSpecifier: JS.ExportSpecifier, other: J): Promise<J | undefined> {
        return this.visitElement(exportSpecifier, other as JS.ExportSpecifier);
    }

    /**
     * Overrides the visitComputedPropertyMethodDeclaration method to compare computed property method declarations.
     * 
     * @param computedPropMethod The computed property method declaration to visit
     * @param other The other computed property method declaration to compare with
     * @returns The visited computed property method declaration, or undefined if the visit was aborted
     */
    override async visitComputedPropertyMethodDeclaration(computedPropMethod: JS.ComputedPropertyMethodDeclaration, other: J): Promise<J | undefined> {
        return this.visitElement(computedPropMethod, other as JS.ComputedPropertyMethodDeclaration);
    }
}

/**
 * A comparator visitor that checks semantic equality including type attribution.
 * This ensures comparisons account for type information, allowing semantically
 * equivalent code to match even when structurally different (e.g., `foo()` vs `module.foo()`
 * when both refer to the same method).
 */
export class JavaScriptSemanticComparatorVisitor extends JavaScriptComparatorVisitor {
    /**
     * When true, allows patterns without type annotations to match code with type annotations.
     * This enables lenient matching where undefined types on either side are considered compatible.
     */
    protected readonly lenientTypeMatching: boolean;

    /**
     * Creates a new semantic comparator visitor.
     *
     * @param lenientTypeMatching If true, allows matching between nodes with and without type annotations
     */
    constructor(lenientTypeMatching: boolean = false) {
        super();
        this.lenientTypeMatching = lenientTypeMatching;
    }

    /**
     * Override visitProperty to allow lenient type matching.
     * When lenientTypeMatching is enabled, null vs Type comparisons are allowed
     * (where one value is null/undefined and the other is a Type object).
     */
    protected override async visitProperty(j: any, other: any): Promise<any> {
        // Handle null/undefined with lenient type matching
        if (this.lenientTypeMatching && (j == null || other == null)) {
            if (j !== other) {
                // Don't abort if one is null and the other is a Type
                const jKind = (j as any)?.kind;
                const otherKind = (other as any)?.kind;
                const isTypeComparison =
                    (jKind && typeof jKind === 'string' && jKind.startsWith('org.openrewrite.java.tree.JavaType$')) ||
                    (otherKind && typeof otherKind === 'string' && otherKind.startsWith('org.openrewrite.java.tree.JavaType$'));

                if (!isTypeComparison) {
                    this.abort(j);
                }
            }
            return j;
        }

        // Otherwise, use base class behavior
        return super.visitProperty(j, other);
    }

    /**
     * Checks if two types are semantically equal.
     * For method types, this checks that the declaring type and method name match.
     * With lenient type matching, undefined types on either side are considered compatible.
     */
    private isOfType(target?: Type, source?: Type): boolean {
        if (!target || !source) {
            // Lenient mode: if either type is undefined, allow the match
            return this.lenientTypeMatching ? true : target === source;
        }

        if (target.kind !== source.kind) {
            return false;
        }

        // For method types, check declaring type
        // Note: We don't check the name field because it might not be fully resolved in patterns
        // The method invocation visitor already checks that simple names match
        if (target.kind === Type.Kind.Method && source.kind === Type.Kind.Method) {
            const targetMethod = target as Type.Method;
            const sourceMethod = source as Type.Method;

            // Check if declaring types match
            const declaringTypesMatch = this.isOfType(targetMethod.declaringType, sourceMethod.declaringType);
            if (declaringTypesMatch) {
                return true;
            }

            // If declaring types don't match exactly, check if they might be semantically equivalent
            // (e.g., 'react' module vs 'React' namespace importing from 'react')
            // In this case, we check if the method signatures are otherwise identical
            if (targetMethod.declaringType && sourceMethod.declaringType &&
                Type.isFullyQualified(targetMethod.declaringType) && Type.isFullyQualified(sourceMethod.declaringType)) {

                const targetDeclType = targetMethod.declaringType as Type.FullyQualified;
                const sourceDeclType = sourceMethod.declaringType as Type.FullyQualified;

                // Check if the declaring type names could represent the same module
                // (e.g., 'react' and 'React', where React is a namespace alias)
                const targetFQN = Type.FullyQualified.getFullyQualifiedName(targetDeclType);
                const sourceFQN = Type.FullyQualified.getFullyQualifiedName(sourceDeclType);

                // If the names differ only in case and one appears to be a module name
                // (all lowercase) while the other is capitalized (namespace alias),
                // check if the method signatures match
                if (targetFQN.toLowerCase() === sourceFQN.toLowerCase()) {
                    // Method signatures should match: return type and parameters
                    if (!this.isOfType(targetMethod.returnType, sourceMethod.returnType)) {
                        return false;
                    }

                    if (targetMethod.parameterTypes.length !== sourceMethod.parameterTypes.length) {
                        return false;
                    }

                    for (let i = 0; i < targetMethod.parameterTypes.length; i++) {
                        if (!this.isOfType(targetMethod.parameterTypes[i], sourceMethod.parameterTypes[i])) {
                            return false;
                        }
                    }

                    return true;
                }
            }

            return false;
        }

        // For fully qualified types, check the fully qualified name
        if (Type.isFullyQualified(target) && Type.isFullyQualified(source)) {
            return Type.FullyQualified.getFullyQualifiedName(target) ===
                   Type.FullyQualified.getFullyQualifiedName(source);
        }

        // Default: types are equal if they're the same kind
        return true;
    }

    /**
     * Override method invocation comparison to include type attribution checking.
     * When types match semantically, we allow matching even if one has a receiver
     * and the other doesn't (e.g., `isDate(x)` vs `util.isDate(x)`).
     */
    override async visitMethodInvocation(method: J.MethodInvocation, other: J): Promise<J | undefined> {
        if (other.kind !== J.Kind.MethodInvocation) {
            return this.abort(method);
        }

        const otherMethod = other as J.MethodInvocation;

        // Check basic structural equality first
        if (method.name.simpleName !== otherMethod.name.simpleName ||
            method.arguments.elements.length !== otherMethod.arguments.elements.length) {
            return this.abort(method);
        }

        // Check type attribution
        // Both must have method types for semantic equality
        if (!method.methodType || !otherMethod.methodType) {
            // Lenient mode: if either has no type, allow structural matching
            if (this.lenientTypeMatching) {
                return super.visitMethodInvocation(method, other);
            }
            // Strict mode: if one has type but the other doesn't, they don't match
            if (method.methodType || otherMethod.methodType) {
                return this.abort(method);
            }
            // If neither has type, fall through to structural comparison
            return super.visitMethodInvocation(method, other);
        }

        // Both have types - check they match semantically
        const typesMatch = this.isOfType(method.methodType, otherMethod.methodType);
        if (!typesMatch) {
            // Types don't match - abort comparison
            return this.abort(method);
        }

        // Types match! Now check if we can ignore receiver differences.
        // We can only ignore receiver differences when one or both receivers are identifiers
        // that represent module/namespace imports (e.g., `util` in `util.isDate()`).
        // For other receivers (e.g., variables, expressions), we must compare them.

        const canIgnoreReceiverDifference =
            // Case 1: One has no select (direct call like `forwardRef()`), other has select (namespace like `React.forwardRef()`)
            (!method.select && otherMethod.select) ||
            (method.select && !otherMethod.select);

        if (!canIgnoreReceiverDifference) {
            // Both have selects or both don't - must compare them structurally
            if ((method.select === undefined) !== (otherMethod.select === undefined)) {
                return this.abort(method);
            }

            if (method.select && otherMethod.select) {
                await this.visitRightPadded(method.select, otherMethod.select as any);
            }
        }

        // Compare type parameters
        if ((method.typeParameters === undefined) !== (otherMethod.typeParameters === undefined)) {
            return this.abort(method);
        }

        if (method.typeParameters && otherMethod.typeParameters) {
            if (method.typeParameters.elements.length !== otherMethod.typeParameters.elements.length) {
                return this.abort(method);
            }
            for (let i = 0; i < method.typeParameters.elements.length; i++) {
                await this.visitRightPadded(method.typeParameters.elements[i], otherMethod.typeParameters.elements[i] as any);
                if (!this.match) {
                    return this.abort(method);
                }
            }
        }

        // Compare name (already checked simpleName above, but visit for markers/prefix)
        await this.visit(method.name, otherMethod.name);
        if (!this.match) {
            return this.abort(method);
        }

        // Compare arguments (visit RightPadded to check for markers)
        for (let i = 0; i < method.arguments.elements.length; i++) {
            await this.visitRightPadded(method.arguments.elements[i], otherMethod.arguments.elements[i] as any);
            if (!this.match) {
                return this.abort(method);
            }
        }

        return method;
    }

    /**
     * Override identifier comparison to include type checking for field access.
     */
    override async visitIdentifier(identifier: J.Identifier, other: J): Promise<J | undefined> {
        if (other.kind !== J.Kind.Identifier) {
            return this.abort(identifier);
        }

        const otherIdentifier = other as J.Identifier;

        // Check name matches
        if (identifier.simpleName !== otherIdentifier.simpleName) {
            return this.abort(identifier);
        }

        // For identifiers with field types, check type attribution
        if (identifier.fieldType && otherIdentifier.fieldType) {
            if (!this.isOfType(identifier.fieldType, otherIdentifier.fieldType)) {
                return this.abort(identifier);
            }
        } else if (identifier.fieldType || otherIdentifier.fieldType) {
            // Lenient mode: if either has no type, allow structural matching
            if (!this.lenientTypeMatching) {
                // Strict mode: if only one has a type, they don't match
                return this.abort(identifier);
            }
        }

        return super.visitIdentifier(identifier, other);
    }

    /**
     * Override variable declarations comparison to handle lenient type matching.
     * When lenientTypeMatching is true, patterns without typeExpression can match
     * code with typeExpression.
     */
    override async visitVariableDeclarations(variableDeclarations: J.VariableDeclarations, other: J): Promise<J | undefined> {
        const otherVariableDeclarations = other as J.VariableDeclarations;

        // Visit leading annotations
        if (variableDeclarations.leadingAnnotations.length !== otherVariableDeclarations.leadingAnnotations.length) {
            return this.abort(variableDeclarations);
        }

        for (let i = 0; i < variableDeclarations.leadingAnnotations.length; i++) {
            await this.visit(variableDeclarations.leadingAnnotations[i], otherVariableDeclarations.leadingAnnotations[i]);
            if (!this.match) return variableDeclarations;
        }

        // Visit modifiers
        if (variableDeclarations.modifiers.length !== otherVariableDeclarations.modifiers.length) {
            return this.abort(variableDeclarations);
        }

        for (let i = 0; i < variableDeclarations.modifiers.length; i++) {
            await this.visit(variableDeclarations.modifiers[i], otherVariableDeclarations.modifiers[i]);
            if (!this.match) return variableDeclarations;
        }

        // Compare typeExpression - lenient matching allows one to be undefined
        if ((variableDeclarations.typeExpression === undefined) !== (otherVariableDeclarations.typeExpression === undefined)) {
            if (!this.lenientTypeMatching) {
                return this.abort(variableDeclarations);
            }
            // In lenient mode, skip type comparison and continue
        } else if (variableDeclarations.typeExpression && otherVariableDeclarations.typeExpression) {
            // Both have typeExpression, visit them
            await this.visit(variableDeclarations.typeExpression, otherVariableDeclarations.typeExpression);
            if (!this.match) return variableDeclarations;
        }

        // Compare varargs
        if ((variableDeclarations.varargs === undefined) !== (otherVariableDeclarations.varargs === undefined)) {
            return this.abort(variableDeclarations);
        }

        // Compare variables
        if (variableDeclarations.variables.length !== otherVariableDeclarations.variables.length) {
            return this.abort(variableDeclarations);
        }

        // Visit each variable in lock step
        for (let i = 0; i < variableDeclarations.variables.length; i++) {
            await this.visitRightPadded(variableDeclarations.variables[i], otherVariableDeclarations.variables[i] as any);
            if (!this.match) return variableDeclarations;
        }

        return variableDeclarations;
    }

    /**
     * Override method declaration comparison to handle lenient type matching.
     * When lenientTypeMatching is true, patterns without returnTypeExpression can match
     * code with returnTypeExpression.
     */
    override async visitMethodDeclaration(methodDeclaration: J.MethodDeclaration, other: J): Promise<J | undefined> {
        const otherMethodDeclaration = other as J.MethodDeclaration;

        // Visit leading annotations
        if (methodDeclaration.leadingAnnotations.length !== otherMethodDeclaration.leadingAnnotations.length) {
            return this.abort(methodDeclaration);
        }

        for (let i = 0; i < methodDeclaration.leadingAnnotations.length; i++) {
            await this.visit(methodDeclaration.leadingAnnotations[i], otherMethodDeclaration.leadingAnnotations[i]);
            if (!this.match) return methodDeclaration;
        }

        // Visit modifiers
        if (methodDeclaration.modifiers.length !== otherMethodDeclaration.modifiers.length) {
            return this.abort(methodDeclaration);
        }

        for (let i = 0; i < methodDeclaration.modifiers.length; i++) {
            await this.visit(methodDeclaration.modifiers[i], otherMethodDeclaration.modifiers[i]);
            if (!this.match) return methodDeclaration;
        }

        // Visit type parameters if present
        if (!!methodDeclaration.typeParameters !== !!otherMethodDeclaration.typeParameters) {
            return this.abort(methodDeclaration);
        }

        if (methodDeclaration.typeParameters && otherMethodDeclaration.typeParameters) {
            await this.visit(methodDeclaration.typeParameters, otherMethodDeclaration.typeParameters);
            if (!this.match) return methodDeclaration;
        }

        // Compare returnTypeExpression - lenient matching allows one to be undefined
        if ((methodDeclaration.returnTypeExpression === undefined) !== (otherMethodDeclaration.returnTypeExpression === undefined)) {
            if (!this.lenientTypeMatching) {
                return this.abort(methodDeclaration);
            }
            // In lenient mode, skip type comparison and continue
        } else if (methodDeclaration.returnTypeExpression && otherMethodDeclaration.returnTypeExpression) {
            // Both have returnTypeExpression, visit them
            await this.visit(methodDeclaration.returnTypeExpression, otherMethodDeclaration.returnTypeExpression);
            if (!this.match) return methodDeclaration;
        }

        // Visit name
        await this.visit(methodDeclaration.name, otherMethodDeclaration.name);
        if (!this.match) return methodDeclaration;

        // Compare parameters
        if (methodDeclaration.parameters.elements.length !== otherMethodDeclaration.parameters.elements.length) {
            return this.abort(methodDeclaration);
        }

        // Visit each parameter in lock step
        for (let i = 0; i < methodDeclaration.parameters.elements.length; i++) {
            await this.visitRightPadded(methodDeclaration.parameters.elements[i], otherMethodDeclaration.parameters.elements[i] as any);
            if (!this.match) return methodDeclaration;
        }

        // Visit throws if present
        if (!!methodDeclaration.throws !== !!otherMethodDeclaration.throws) {
            return this.abort(methodDeclaration);
        }

        if (methodDeclaration.throws && otherMethodDeclaration.throws) {
            // Visit each throws expression in lock step
            if (methodDeclaration.throws.elements.length !== otherMethodDeclaration.throws.elements.length) {
                return this.abort(methodDeclaration);
            }

            for (let i = 0; i < methodDeclaration.throws.elements.length; i++) {
                await this.visitRightPadded(methodDeclaration.throws.elements[i], otherMethodDeclaration.throws.elements[i] as any);
                if (!this.match) return methodDeclaration;
            }
        }

        // Visit body if present
        if (!!methodDeclaration.body !== !!otherMethodDeclaration.body) {
            return this.abort(methodDeclaration);
        }

        if (methodDeclaration.body && otherMethodDeclaration.body) {
            await this.visit(methodDeclaration.body, otherMethodDeclaration.body);
            if (!this.match) return methodDeclaration;
        }

        return methodDeclaration;
    }
}
