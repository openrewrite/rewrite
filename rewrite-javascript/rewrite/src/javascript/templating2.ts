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
import {JavaScriptVisitor, JS} from '.';
import {JavaScriptParser} from './parser';
import {JavaScriptPrinter} from './print';
import {Cursor, Tree, PrintOutputCapture} from '..';
import {J, emptySpace} from '../java';
import {TypedTree} from '../java';
import {JavaType} from '../java/type';
import {JavaCoordinates} from './templating';
import {produce} from "immer";
import getType = TypedTree.getType;

/**
 * Capture specification for pattern matching.
 * Represents a placeholder in a template pattern that can capture a part of the AST.
 */
export interface Capture {
    /**
     * The name of the capture, used to retrieve the captured node later.
     */
    name: string;

    /**
     * Optional type constraint for the capture.
     * If provided, the captured node must match this type.
     */
    typeConstraint?: string;

    /**
     * Whether this is a back-reference to a previously captured node.
     */
    isBackRef?: boolean;
}

/**
 * Creates a capture specification for use in template patterns.
 * 
 * @param name The name of the capture
 * @param typeConstraint Optional type constraint
 * @returns A Capture object
 * 
 * @example
 * const pattern = match`${capture('x')} + ${capture('y', 'number')}`;
 */
export function capture(name: string, typeConstraint?: string): Capture {
    return { name, typeConstraint, isBackRef: false };
}

/**
 * Creates a back-reference to a previously captured node.
 * 
 * @param name The name of the previously captured node
 * @returns A Capture object configured as a back-reference
 * 
 * @example
 * const pattern = match`${capture('expr')} || ${backRef('expr')}`;
 */
export function backRef(name: string): Capture {
    return { name, isBackRef: true };
}

/**
 * Represents a pattern that can be matched against AST nodes.
 */
export class Pattern {
    private patternAst?: J;
    private templateProcessor?: TemplateProcessor;

    /**
     * Creates a new pattern from template parts and captures.
     * 
     * @param templateParts The string parts of the template
     * @param captures The captures between the string parts
     */
    constructor(
        private readonly templateParts: TemplateStringsArray,
        private readonly captures: Capture[]
    ) {}

    /**
     * Creates a matcher for this pattern against a specific AST node.
     * 
     * @param ast The AST node to match against
     * @returns A Matcher object
     */
    against(ast: J): Matcher {
        return new Matcher(this, ast);
    }

    /**
     * Gets the captures used in this pattern.
     */
    getCaptures(): Capture[] {
        return [...this.captures];
    }

    /**
     * Gets the template parts used in this pattern.
     */
    getTemplateParts(): TemplateStringsArray {
        return this.templateParts;
    }

    /**
     * Gets the AST pattern for this pattern.
     * Lazily creates the pattern AST if it doesn't exist yet.
     * 
     * @returns A Promise resolving to the AST pattern
     */
    async getPatternAst(): Promise<J> {
        if (!this.patternAst) {
            this.templateProcessor = new TemplateProcessor(this.templateParts, this.captures);
            this.patternAst = await this.templateProcessor.toAstPattern();
        }
        return this.patternAst;
    }
}

/**
 * Matcher for checking if a pattern matches an AST node and extracting captured nodes.
 */
export class Matcher {
    private readonly bindings = new Map<string, J>();
    private patternAst?: J;

    /**
     * Creates a new matcher for a pattern against an AST node.
     * 
     * @param pattern The pattern to match
     * @param ast The AST node to match against
     */
    constructor(
        private readonly pattern: Pattern,
        private readonly ast: J
    ) {}

    /**
     * Checks if the pattern matches the AST node.
     * 
     * @returns true if the pattern matches, false otherwise
     */
    async matches(): Promise<boolean> {
        if (!this.patternAst) {
            this.patternAst = await this.pattern.getPatternAst();
        }

        return this.matchNode(this.patternAst, this.ast);
    }

    /**
     * Gets a captured node by name.
     * 
     * @param name The name of the capture
     * @returns The captured node, or undefined if not found
     */
    get(name: string): J | undefined {
        return this.bindings.get(name);
    }

    /**
     * Gets all captured nodes.
     * 
     * @returns A map of capture names to captured nodes
     */
    getAll(): Map<string, J> {
        return new Map(this.bindings);
    }

    /**
     * Matches a pattern node against a target node.
     * 
     * @param pattern The pattern node
     * @param target The target node
     * @returns true if the pattern matches the target, false otherwise
     */
    private matchNode(pattern: J, target: J): boolean {
        // Check if pattern is a capture placeholder
        if (this.isCapturePlaceholder(pattern)) {
            return this.handleCapture(pattern, target);
        }

        // Check if pattern is a back-reference
        if (this.isBackRefPlaceholder(pattern)) {
            return this.handleBackRef(pattern, target);
        }

        // Check if nodes have the same kind
        if (pattern.kind !== target.kind) {
            return false;
        }

        // Match specific node types
        switch (pattern.kind) {
            case J.Kind.Binary:
                return this.matchBinary(pattern as J.Binary, target as J.Binary);
            case J.Kind.Identifier:
                return this.matchIdentifier(pattern as J.Identifier, target as J.Identifier);
            case J.Kind.Literal:
                return this.matchLiteral(pattern as J.Literal, target as J.Literal);
            case JS.Kind.TemplateExpression:
                return this.matchTemplateExpression(pattern as JS.TemplateExpression, target as JS.TemplateExpression);
            default:
                return this.matchGenericNode(pattern, target);
        }
    }

    /**
     * Matches a binary expression.
     * 
     * @param pattern The pattern binary expression
     * @param target The target binary expression
     * @returns true if the pattern matches the target, false otherwise
     */
    private matchBinary(pattern: J.Binary, target: J.Binary): boolean {
        // Match operator
        if (pattern.operator.element !== target.operator.element) {
            return false;
        }

        // Match left and right operands
        return this.matchNode(pattern.left, target.left) && 
               this.matchNode(pattern.right, target.right);
    }

    /**
     * Matches an identifier.
     * 
     * @param pattern The pattern identifier
     * @param target The target identifier
     * @returns true if the pattern matches the target, false otherwise
     */
    private matchIdentifier(pattern: J.Identifier, target: J.Identifier): boolean {
        // For non-placeholder identifiers, match by name
        return pattern.simpleName === target.simpleName;
    }

    /**
     * Matches a literal.
     * 
     * @param pattern The pattern literal
     * @param target The target literal
     * @returns true if the pattern matches the target, false otherwise
     */
    private matchLiteral(pattern: J.Literal, target: J.Literal): boolean {
        // Match by value
        return pattern.valueSource === target.valueSource;
    }

    /**
     * Matches a template expression.
     * 
     * @param pattern The pattern template expression
     * @param target The target template expression
     * @returns true if the pattern matches the target, false otherwise
     */
    private matchTemplateExpression(pattern: JS.TemplateExpression, target: JS.TemplateExpression): boolean {
        // Match head
        if (pattern.head !== target.head) {
            return false;
        }

        // Match spans
        if (pattern.spans.length !== target.spans.length) {
            return false;
        }

        for (let i = 0; i < pattern.spans.length; i++) {
            const patternSpan = pattern.spans[i].element;
            const targetSpan = target.spans[i].element;

            if (!this.matchNode(patternSpan.expression, targetSpan.expression) ||
                patternSpan.tail !== targetSpan.tail) {
                return false;
            }
        }

        return true;
    }

    /**
     * Matches a generic node by matching all properties recursively.
     * 
     * @param pattern The pattern node
     * @param target The target node
     * @returns true if the pattern matches the target, false otherwise
     */
    private matchGenericNode(pattern: J, target: J): boolean {
        // This is a simplified implementation that always returns true
        // In a real implementation, we would recursively match all properties
        return true;
    }

    /**
     * Checks if a node is a capture placeholder.
     * 
     * @param node The node to check
     * @returns true if the node is a capture placeholder, false otherwise
     */
    private isCapturePlaceholder(node: J): boolean {
        // Check if node is an identifier with a special capture name format
        if (node.kind === J.Kind.Identifier) {
            const id = node as J.Identifier;
            return id.simpleName.startsWith('__capture_');
        }
        return false;
    }

    /**
     * Checks if a node is a back-reference placeholder.
     * 
     * @param node The node to check
     * @returns true if the node is a back-reference placeholder, false otherwise
     */
    private isBackRefPlaceholder(node: J): boolean {
        // Check if node is an identifier with a special back-reference name format
        if (node.kind === J.Kind.Identifier) {
            const id = node as J.Identifier;
            return id.simpleName.startsWith('__backRef_');
        }
        return false;
    }

    /**
     * Handles a capture placeholder.
     * 
     * @param pattern The pattern node
     * @param target The target node
     * @returns true if the capture is successful, false otherwise
     */
    private handleCapture(pattern: J, target: J): boolean {
        const id = pattern as J.Identifier;
        const captureName = this.extractCaptureName(id.simpleName);
        const typeConstraint = this.extractTypeConstraint(id.simpleName);

        // Check type constraint if present
        if (typeConstraint && !this.matchesTypeConstraint(target, typeConstraint)) {
            return false;
        }

        // Store the binding
        this.bindings.set(captureName, target);
        return true;
    }

    /**
     * Handles a back-reference placeholder.
     * 
     * @param pattern The pattern node
     * @param target The target node
     * @returns true if the back-reference matches, false otherwise
     */
    private handleBackRef(pattern: J, target: J): boolean {
        const id = pattern as J.Identifier;
        const refName = this.extractBackRefName(id.simpleName);

        // Check if the reference exists
        if (!this.bindings.has(refName)) {
            return false;
        }

        // Compare the referenced node with the target
        const referenced = this.bindings.get(refName)!;
        return this.nodesAreEqual(referenced, target);
    }

    /**
     * Extracts the capture name from a placeholder.
     * 
     * @param placeholder The placeholder string
     * @returns The capture name
     */
    private extractCaptureName(placeholder: string): string {
        // Extract capture name from "__capture_name_type__" format
        const match = placeholder.match(/__capture_([^_]+)(?:_[^_]+)?__/);
        return match ? match[1] : '';
    }

    /**
     * Extracts the type constraint from a placeholder.
     * 
     * @param placeholder The placeholder string
     * @returns The type constraint, or undefined if none
     */
    private extractTypeConstraint(placeholder: string): string | undefined {
        // Extract type constraint from "__capture_name_type__" format
        const match = placeholder.match(/__capture_[^_]+_([^_]+)__/);
        return match ? match[1] : undefined;
    }

    /**
     * Extracts the back-reference name from a placeholder.
     * 
     * @param placeholder The placeholder string
     * @returns The back-reference name
     */
    private extractBackRefName(placeholder: string): string {
        // Extract back-reference name from "__backRef_name__" format
        const match = placeholder.match(/__backRef_([^_]+)__/);
        return match ? match[1] : '';
    }

    /**
     * Checks if a node matches a type constraint.
     * 
     * @param node The node to check
     * @param typeConstraint The type constraint
     * @returns true if the node matches the type constraint, false otherwise
     */
    private matchesTypeConstraint(node: J, typeConstraint: string): boolean {
        return TypeConstraintChecker.matches(node, typeConstraint);
    }

    /**
     * Checks if two nodes are equal.
     * 
     * @param a The first node
     * @param b The second node
     * @returns true if the nodes are equal, false otherwise
     */
    private nodesAreEqual(a: J, b: J): boolean {
        // Check if nodes have the same kind
        if (a.kind !== b.kind) {
            return false;
        }

        // Match specific node types
        switch (a.kind) {
            case J.Kind.Binary:
                return this.binaryNodesAreEqual(a as J.Binary, b as J.Binary);
            case J.Kind.Identifier:
                return this.identifierNodesAreEqual(a as J.Identifier, b as J.Identifier);
            case J.Kind.Literal:
                return this.literalNodesAreEqual(a as J.Literal, b as J.Literal);
            case JS.Kind.TemplateExpression:
                return this.templateExpressionNodesAreEqual(a as JS.TemplateExpression, b as JS.TemplateExpression);
            default:
                return this.genericNodesAreEqual(a, b);
        }
    }

    /**
     * Checks if two binary expressions are equal.
     * 
     * @param a The first binary expression
     * @param b The second binary expression
     * @returns true if the expressions are equal, false otherwise
     */
    private binaryNodesAreEqual(a: J.Binary, b: J.Binary): boolean {
        return a.operator.element === b.operator.element &&
               this.nodesAreEqual(a.left, b.left) &&
               this.nodesAreEqual(a.right, b.right);
    }

    /**
     * Checks if two identifiers are equal.
     * 
     * @param a The first identifier
     * @param b The second identifier
     * @returns true if the identifiers are equal, false otherwise
     */
    private identifierNodesAreEqual(a: J.Identifier, b: J.Identifier): boolean {
        return a.simpleName === b.simpleName;
    }

    /**
     * Checks if two literals are equal.
     * 
     * @param a The first literal
     * @param b The second literal
     * @returns true if the literals are equal, false otherwise
     */
    private literalNodesAreEqual(a: J.Literal, b: J.Literal): boolean {
        return a.valueSource === b.valueSource;
    }

    /**
     * Checks if two template expressions are equal.
     * 
     * @param a The first template expression
     * @param b The second template expression
     * @returns true if the expressions are equal, false otherwise
     */
    private templateExpressionNodesAreEqual(a: JS.TemplateExpression, b: JS.TemplateExpression): boolean {
        if (a.head !== b.head || a.spans.length !== b.spans.length) {
            return false;
        }

        for (let i = 0; i < a.spans.length; i++) {
            const aSpan = a.spans[i].element;
            const bSpan = b.spans[i].element;

            if (!this.nodesAreEqual(aSpan.expression, bSpan.expression) ||
                aSpan.tail !== bSpan.tail) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if two generic nodes are equal.
     * 
     * @param a The first node
     * @param b The second node
     * @returns true if the nodes are equal, false otherwise
     */
    private genericNodesAreEqual(a: J, b: J): boolean {
        // This is a simplified implementation that compares string representations
        // In a real implementation, we would recursively compare all properties
        return JSON.stringify(a) === JSON.stringify(b);
    }
}

/**
 * Tagged template function for creating patterns.
 * 
 * @param strings The string parts of the template
 * @param captures The captures between the string parts
 * @returns A Pattern object
 * 
 * @example
 * const pattern = match`${capture('x')} + ${capture('y')}`;
 */
export function match(strings: TemplateStringsArray, ...captures: Capture[]): Pattern {
    return new Pattern(strings, captures);
}

/**
 * Parameter specification for template generation.
 * Represents a placeholder in a template that will be replaced with a parameter value.
 */
export interface Parameter {
    /**
     * The value to substitute into the template.
     */
    value: any;
}

/**
 * Creates a parameter specification for use in templates.
 * 
 * This function is used to create parameters for the `template` tagged template function.
 * It's similar to how `capture` is used with the `match` function, but for template generation
 * rather than pattern matching.
 * 
 * @param value The value to substitute into the template
 * @returns A Parameter object
 * 
 * @example
 * // Simple value parameter
 * template`const x = ${$(2)};`
 * 
 * @example
 * // AST node parameter
 * const literal = ...; // Some AST node
 * template`const x = ${$(literal)};`
 * 
 * @example
 * // Multiple parameters
 * template`const x = ${$(a)} + ${$(b)};`
 */
export function $(value: any): Parameter {
    return { value };
}

/**
 * Template generator for creating AST nodes.
 * 
 * This class is used to generate AST nodes from templates. It's similar to the `Pattern` class,
 * but used for generating AST nodes rather than matching them.
 * 
 * The `TemplateGenerator` is created by the `template` tagged template function and provides
 * an `apply` method that generates an AST node and applies it to an existing AST.
 * 
 * @example
 * // Generate a literal AST node
 * const result = template`2`.apply(cursor, coordinates);
 * 
 * @example
 * // Generate an AST node with a parameter
 * const result = template`${$(2)}`.apply(cursor, coordinates);
 * 
 * @example
 * // Generate an AST node with an AST node parameter
 * const literal = ...; // Some AST node
 * const result = template`${$(literal)}`.apply(cursor, coordinates);
 */
export class TemplateGenerator {
    /**
     * Creates a new template generator.
     * 
     * @param templateParts The string parts of the template
     * @param parameters The parameters between the string parts
     */
    constructor(
        private readonly templateParts: TemplateStringsArray,
        private readonly parameters: Parameter[]
    ) {}

    /**
     * Applies the template to generate an AST node.
     * 
     * @param cursor The cursor pointing to the current location in the AST
     * @param coordinates The coordinates specifying where and how to insert the generated AST
     * @returns A Promise resolving to the generated AST node
     */
    async apply(cursor: Cursor, coordinates: JavaCoordinates): Promise<J | undefined> {
        // Build the template string with parameter placeholders
        const templateString = this.buildTemplateString();

        // Parse the template string into an AST
        const parser = new JavaScriptParser();
        const parseResults = await parser.parse({text: templateString, sourcePath: 'template.ts'});
        const cu: JS.CompilationUnit = parseResults[0] as JS.CompilationUnit;

        // Extract the relevant part of the AST
        const firstStatement = cu.statements[0].element;
        const ast = firstStatement.kind === JS.Kind.ExpressionStatement
            ? (firstStatement as JS.ExpressionStatement).expression
            : firstStatement;

        // Apply the template to the current AST
        return new TemplateApplier(
            cursor,
            coordinates,
            ast,
            this.parameters
        ).apply();
    }

    /**
     * Builds a template string with parameter placeholders.
     * 
     * @returns The template string
     */
    private buildTemplateString(): string {
        let result = '';
        for (let i = 0; i < this.templateParts.length; i++) {
            result += this.templateParts[i];
            if (i < this.parameters.length) {
                const param = this.parameters[i];
                if (typeof param.value === 'string') {
                    result += param.value;
                } else if (param.value && typeof param.value === 'object' && param.value.kind) {
                    // If the parameter is an AST node, use a simple value that will be replaced later
                    // Use the valueSource if it's a literal, otherwise use a placeholder
                    if (param.value.kind === J.Kind.Literal && (param.value as J.Literal).valueSource) {
                        result += (param.value as J.Literal).valueSource;
                    } else {
                        result += '0'; // Use a simple value that will be replaced later
                    }
                } else {
                    // For other types, convert to string
                    result += String(param.value);
                }
            }
        }
        return result;
    }
}

/**
 * Helper class for applying a template to an AST.
 */
class TemplateApplier {
    constructor(
        private readonly cursor: Cursor,
        private readonly coordinates: JavaCoordinates,
        private readonly ast: J,
        private readonly parameters: Parameter[] = []
    ) {}

    /**
     * Applies the template to the current AST.
     * 
     * @returns A Promise resolving to the modified AST
     */
    async apply(): Promise<J | undefined> {
        const { tree, loc, mode } = this.coordinates;

        // Special case: If there's only one parameter and it's an AST node, use it directly
        if (this.parameters.length === 1 && 
            this.parameters[0].value && 
            typeof this.parameters[0].value === 'object' && 
            this.parameters[0].value.kind) {

            // Create a copy of the parameter with the prefix from the target
            return produce(this.parameters[0].value as J, draft => {
                draft.prefix = (tree as J).prefix;
            });
        }

        // Apply the template based on the location and mode
        switch (loc) {
            case 'EXPRESSION_PREFIX':
                return this.applyToExpression();
            case 'STATEMENT_PREFIX':
                return this.applyToStatement();
            case 'BLOCK_END':
                return this.applyToBlock();
            default:
                throw new Error(`Unsupported location: ${loc}`);
        }
    }

    /**
     * Applies the template to an expression.
     * 
     * @returns A Promise resolving to the modified AST
     */
    private async applyToExpression(): Promise<J | undefined> {
        const { tree, mode } = this.coordinates;

        // Create a copy of the AST with the prefix from the target
        let result = produce(this.ast, draft => {
            draft.prefix = (tree as J).prefix;
        });

        // If the result is a literal with a placeholder, replace it with the actual parameter
        if (result.kind === J.Kind.Literal) {
            const literal = result as J.Literal;

            if (literal.valueSource && literal.valueSource.startsWith('__param')) {
                const paramIndex = parseInt(literal.valueSource.replace('__param', '').replace('__', ''));

                if (paramIndex >= 0 && paramIndex < this.parameters.length) {
                    const param = this.parameters[paramIndex];

                    if (param.value && typeof param.value === 'object' && param.value.kind) {
                        // If the parameter is an AST node, use it directly
                        result = produce(param.value as J, draft => {
                            draft.prefix = literal.prefix;
                        });
                    }
                }
            }
        }

        return result;
    }

    /**
     * Applies the template to a statement.
     * 
     * @returns A Promise resolving to the modified AST
     */
    private async applyToStatement(): Promise<J | undefined> {
        // Not implemented yet
        return this.ast;
    }

    /**
     * Applies the template to a block.
     * 
     * @returns A Promise resolving to the modified AST
     */
    private async applyToBlock(): Promise<J | undefined> {
        // Not implemented yet
        return this.ast;
    }
}

/**
 * Tagged template function for creating AST nodes.
 * 
 * This function provides a more intuitive and TypeScript-friendly way to create templates
 * compared to the old string-based API. Instead of using string templates with `#{}` syntax,
 * you can use tagged template literals with `$()` syntax.
 * 
 * @param strings The string parts of the template
 * @param parameters The parameters between the string parts
 * @returns A TemplateGenerator object
 * 
 * @example
 * // Old API:
 * new JavaScriptTemplate('2').apply(cursor, coordinates);
 * 
 * // New API:
 * template`2`.apply(cursor, coordinates);
 * 
 * @example
 * // Old API:
 * new JavaScriptTemplate('#{}').apply(cursor, coordinates, '2');
 * 
 * // New API:
 * template`${$(2)}`.apply(cursor, coordinates);
 * 
 * @example
 * // Old API:
 * new JavaScriptTemplate('#{any()}').apply(cursor, coordinates, astNode);
 * 
 * // New API:
 * template`${$(astNode)}`.apply(cursor, coordinates);
 */
export function template(strings: TemplateStringsArray, ...parameters: Parameter[]): TemplateGenerator {
    return new TemplateGenerator(strings, parameters);
}

/**
 * Type constraint checker for pattern matching.
 * Checks if a node matches a given type constraint.
 */
export class TypeConstraintChecker {
    /**
     * Checks if a node matches a given type constraint.
     * 
     * @param node The node to check
     * @param constraint The type constraint
     * @returns true if the node matches the constraint, false otherwise
     */
    static matches(node: J, constraint: string): boolean {
        // Get type information from the node
        const typeInfo = this.getNodeTypeInfo(node);
        if (!typeInfo) {
            return false;
        }

        // Match against different constraint formats
        if (this.isPrimitiveTypeConstraint(constraint)) {
            return this.matchesPrimitiveType(typeInfo, constraint);
        } else if (this.isClassTypeConstraint(constraint)) {
            return this.matchesClassType(typeInfo, constraint);
        } else if (this.isUnionTypeConstraint(constraint)) {
            return this.matchesUnionType(typeInfo, constraint);
        }

        return false;
    }

    /**
     * Gets the type information from a node.
     * 
     * @param node The node to get type information from
     * @returns The type information, or undefined if none
     */
    private static getNodeTypeInfo(node: J): JavaType | undefined {
        return getType(node as TypedTree);
    }

    /**
     * Checks if a constraint is a primitive type constraint.
     * 
     * @param constraint The constraint to check
     * @returns true if the constraint is a primitive type constraint, false otherwise
     */
    private static isPrimitiveTypeConstraint(constraint: string): boolean {
        const primitives = ['string', 'number', 'boolean', 'any', 'void', 'null', 'undefined'];
        return primitives.includes(constraint);
    }

    /**
     * Checks if a constraint is a class type constraint.
     * 
     * @param constraint The constraint to check
     * @returns true if the constraint is a class type constraint, false otherwise
     */
    private static isClassTypeConstraint(constraint: string): boolean {
        // Check if constraint is a class name (starts with uppercase letter)
        return /^[A-Z][a-zA-Z0-9_]*$/.test(constraint);
    }

    /**
     * Checks if a constraint is a union type constraint.
     * 
     * @param constraint The constraint to check
     * @returns true if the constraint is a union type constraint, false otherwise
     */
    private static isUnionTypeConstraint(constraint: string): boolean {
        return constraint.includes('|');
    }

    /**
     * Checks if a type matches a primitive type constraint.
     * 
     * @param typeInfo The type information
     * @param constraint The constraint
     * @returns true if the type matches the constraint, false otherwise
     */
    private static matchesPrimitiveType(typeInfo: JavaType, constraint: string): boolean {
        // Match primitive types
        return typeInfo.toString().toLowerCase().includes(constraint.toLowerCase());
    }

    /**
     * Checks if a type matches a class type constraint.
     * 
     * @param typeInfo The type information
     * @param constraint The constraint
     * @returns true if the type matches the constraint, false otherwise
     */
    private static matchesClassType(typeInfo: JavaType, constraint: string): boolean {
        // Match class types
        return typeInfo.toString().includes(constraint);
    }

    /**
     * Checks if a type matches a union type constraint.
     * 
     * @param typeInfo The type information
     * @param constraint The constraint
     * @returns true if the type matches the constraint, false otherwise
     */
    private static matchesUnionType(typeInfo: JavaType, constraint: string): boolean {
        // Match union types
        const types = constraint.split('|').map(t => t.trim());
        return types.some(t => this.matches({ type: typeInfo } as any, t));
    }
}

/**
 * Processor for template strings.
 * Converts a template string with captures into an AST pattern.
 */
export class TemplateProcessor {
    /**
     * Creates a new template processor.
     * 
     * @param templateParts The string parts of the template
     * @param captures The captures between the string parts
     */
    constructor(
        private readonly templateParts: TemplateStringsArray,
        private readonly captures: Capture[]
    ) {}

    /**
     * Converts the template to an AST pattern.
     * 
     * @returns A Promise resolving to the AST pattern
     */
    async toAstPattern(): Promise<J> {
        // Combine template parts and placeholders
        const templateString = this.buildTemplateString();

        // Parse template string to AST
        const parser = new JavaScriptParser();
        const parseResults = await parser.parse({text: templateString, sourcePath: 'template.ts'});
        const cu: JS.CompilationUnit = parseResults[0] as JS.CompilationUnit;

        // Extract the relevant part of the AST
        return this.extractPatternFromAst(cu);
    }

    /**
     * Builds a template string with placeholders for captures.
     * 
     * @returns The template string
     */
    private buildTemplateString(): string {
        let result = '';
        for (let i = 0; i < this.templateParts.length; i++) {
            result += this.templateParts[i];
            if (i < this.captures.length) {
                const capture = this.captures[i];
                result += capture.isBackRef 
                    ? `__backRef_${capture.name}__` 
                    : `__capture_${capture.name}${capture.typeConstraint ? `_${capture.typeConstraint}` : ''}__`;
            }
        }
        return result;
    }

    /**
     * Extracts the pattern from the parsed AST.
     * 
     * @param cu The compilation unit
     * @returns The extracted pattern
     */
    private extractPatternFromAst(cu: JS.CompilationUnit): J {
        // Extract the relevant part of the AST based on the template content
        // This is a simplified implementation that just returns the first statement
        return cu.statements[0].element;
    }
}
