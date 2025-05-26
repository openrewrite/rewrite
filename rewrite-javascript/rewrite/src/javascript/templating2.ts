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
import {JS} from '.';
import {JavaScriptParser} from './parser';
import {JavaScriptVisitor} from './visitor';
import {Cursor} from '..';
import {J} from '../java';
import {JavaCoordinates} from './templating';
import {produce} from "immer";
import {JavaScriptComparatorVisitor} from "./comparator";

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
     * The captured tree node.
     * This is set when the pattern matches and can be used directly in templates.
     */
    tree?: J;
}

class CaptureImpl implements Capture {
    constructor(
        public readonly name: string,
        public tree?: J  // This needs to be mutable for binding updates
    ) {
    }
}

/**
 * Creates a capture specification for use in template patterns.
 *
 * @param name The name of the capture, or undefined to generate a unique name
 * @returns A Capture object
 *
 * @example
 * const pattern = pattern`${capture('x')} + ${capture('y')}`;
 * 
 * // Using unnamed captures
 * const {left, right} = {left: capture(), right: capture()};
 * const pattern = pattern`${left} + ${right}`;
 * 
 * // Repeated patterns using the same capture
 * const expr = capture('expr');
 * const redundantOr = pattern`${expr} || ${expr}`;
 */
export function capture(name?: string): Capture {
    // Generate a unique name if none is provided
    if (name === undefined) {
        name = `unnamed_${capture.nextUnnamedId++}`;
    }
    return new CaptureImpl(name);
}

// Static counter for generating unique IDs for unnamed captures
capture.nextUnnamedId = 1;

/**
 * Represents a pattern that can be matched against AST nodes.
 */
export class Pattern {
    /**
     * Creates a new pattern from template parts and captures.
     *
     * @param templateParts The string parts of the template
     * @param captures The captures between the string parts
     */
    constructor(
        public readonly templateParts: TemplateStringsArray,
        public readonly captures: Capture[]
    ) {
    }

    configure(configuration: {}) {
        return this;
    }

    /**
     * Creates a matcher for this pattern against a specific AST node.
     *
     * @param ast The AST node to match against
     * @returns A Matcher object
     */
    against(ast: J): Matcher {
        return new Matcher(this, ast);
    }
}

/**
 * Utility class for managing placeholder naming and parsing.
 * Centralizes all logic related to capture placeholders.
 */
class PlaceholderUtils {
    static readonly CAPTURE_PREFIX = '__capture_';
    static readonly PLACEHOLDER_PREFIX = '__PLACEHOLDER_';
    
    /**
     * Checks if a node is a capture placeholder.
     *
     * @param node The node to check
     * @returns true if the node is a capture placeholder, false otherwise
     */
    static isCapture(node: J): boolean {
        if (node.kind === J.Kind.Identifier) {
            const id = node as J.Identifier;
            return id.simpleName.startsWith(this.CAPTURE_PREFIX);
        }
        return false;
    }
    
    /**
     * Parses a capture placeholder to extract name and type constraint.
     *
     * @param identifier The identifier string to parse
     * @returns Object with name and optional type constraint, or null if not a valid capture
     */
    static parseCapture(identifier: string): { name: string; typeConstraint?: string } | null {
        if (!identifier.startsWith(this.CAPTURE_PREFIX)) {
            return null;
        }
        
        // Handle unnamed captures: "__capture_unnamed_N__"
        if (identifier.startsWith(`${this.CAPTURE_PREFIX}unnamed_`)) {
            const match = identifier.match(/__capture_(unnamed_\d+)__/);
            return match ? { name: match[1] } : null;
        }
        
        // Handle named captures: "__capture_name__" or "__capture_name_type__"
        const match = identifier.match(/__capture_([^_]+)(?:_([^_]+))?__/);
        if (!match) {
            return null;
        }
        
        return {
            name: match[1],
            typeConstraint: match[2]
        };
    }
    
    /**
     * Creates a capture placeholder string.
     *
     * @param name The capture name
     * @param typeConstraint Optional type constraint
     * @returns The formatted placeholder string
     */
    static createCapture(name: string, typeConstraint?: string): string {
        return typeConstraint 
            ? `${this.CAPTURE_PREFIX}${name}_${typeConstraint}__`
            : `${this.CAPTURE_PREFIX}${name}__`;
    }
}

/**
 * Matcher for checking if a pattern matches an AST node and extracting captured nodes.
 */
export class Matcher {
    private readonly bindings = new Map<string, J>();
    private patternAst?: J;
    private templateProcessor?: TemplateProcessor;

    /**
     * Creates a new matcher for a pattern against an AST node.
     *
     * @param pattern The pattern to match
     * @param ast The AST node to match against
     */
    constructor(
        private readonly pattern: Pattern,
        private readonly ast: J
    ) {
    }

    /**
     * Checks if the pattern matches the AST node.
     *
     * @returns true if the pattern matches, false otherwise
     */
    async matches(): Promise<boolean> {
        if (!this.patternAst) {
            this.templateProcessor = new TemplateProcessor(this.pattern.templateParts, this.pattern.captures);
            this.patternAst = await this.templateProcessor.toAstPattern();
        }

        return this.matchNode(this.patternAst, this.ast);
    }

    /**
     * Gets a captured node by name or capture object.
     *
     * @param nameOrCapture The name of the capture or the capture object
     * @returns The captured node, or undefined if not found
     */
    get(nameOrCapture: string | Capture): J | undefined {
        const name = typeof nameOrCapture === 'string' ? nameOrCapture : nameOrCapture.name;
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
    private async matchNode(pattern: J, target: J): Promise<boolean> {
        // Check if pattern is a capture placeholder
        if (PlaceholderUtils.isCapture(pattern)) {
            return this.handleCapture(pattern, target);
        }

        // Check if nodes have the same kind
        if (pattern.kind !== target.kind) {
            return false;
        }

        const matcher = this;
        return await ((new class extends JavaScriptComparatorVisitor {
            protected hasSameKind(j: J, other: J): boolean {
                return super.hasSameKind(j, other) || j.kind == J.Kind.Identifier && this.matchesParameter(j as J.Identifier, other);
            }

            override async visitIdentifier(identifier: J.Identifier, other: J): Promise<J | undefined> {
                return this.matchesParameter(identifier, other) ? identifier : await super.visitIdentifier(identifier, other);
            }

            private matchesParameter(identifier: J.Identifier, other: J): boolean {
                return PlaceholderUtils.isCapture(identifier) && 
                       matcher.handleCapture(identifier, other);
            }
        }).compare(pattern, target));
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
        const captureInfo = PlaceholderUtils.parseCapture(id.simpleName);
        
        if (!captureInfo) {
            return false;
        }

        // Store the binding
        this.bindings.set(captureInfo.name, target);

        // Find the corresponding capture object and update its tree property
        if (this.pattern.captures) {
            const capture = this.pattern.captures.find(c => c.name === captureInfo.name);
            if (capture) {
                capture.tree = target;
            }
        }

        return true;
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
 * // Using the same capture multiple times for repeated patterns
 * const expr = capture('expr');
 * const redundantOr = pattern`${expr} || ${expr}`;
 */
export function pattern(strings: TemplateStringsArray, ...captures: Capture[]): Pattern {
    // Check for undefined captures (indicates missing default parameters)
    for (let i = 0; i < captures.length; i++) {
        if (captures[i] === undefined || captures[i] === null) {
            throw new Error(
                `Capture parameter ${i} is undefined. ` +
                `Make sure to provide default values for all parameters in your replace() function. ` +
                `Example: (left = capture(), right = capture()) => ({...})`
            );
        }
        if (typeof captures[i] !== 'object' || !('name' in captures[i])) {
            throw new Error(
                `Capture parameter ${i} is not a valid Capture object. ` +
                `Expected a Capture created with capture(), got: ${typeof captures[i]}`
            );
        }
    }

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
    private readonly substitutions = new Map<string, Parameter>();

    /**
     * Creates a new template generator.
     *
     * @param templateParts The string parts of the template
     * @param parameters The parameters between the string parts
     */
    constructor(
        private readonly templateParts: TemplateStringsArray,
        private readonly parameters: Parameter[]
    ) {
    }

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

        // If the template string is empty, return undefined
        if (!templateString.trim()) {
            return undefined;
        }

        // Parse the template string into an AST
        const parser = new JavaScriptParser();
        const parseResults = await parser.parse({text: templateString, sourcePath: 'template.ts'});
        const cu: JS.CompilationUnit = parseResults[0] as JS.CompilationUnit;

        // Check if there are any statements
        if (!cu.statements || cu.statements.length === 0) {
            return undefined;
        }

        // Extract the relevant part of the AST
        const firstStatement = cu.statements[0].element;
        const ast = firstStatement.kind === JS.Kind.ExpressionStatement
            ? (firstStatement as JS.ExpressionStatement).expression
            : firstStatement;

        // Unsubstitute placeholders with actual parameter values
        const unsubstitutedAst = await this.unsubstitute(ast);

        // Apply the template to the current AST
        return new TemplateApplier(
            cursor,
            coordinates,
            unsubstitutedAst,
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
                const placeholder = `${PlaceholderUtils.PLACEHOLDER_PREFIX}${i}__`;
                this.substitutions.set(placeholder, this.parameters[i]);
                result += placeholder;
            }
        }
        return result;
    }

    /**
     * Replaces placeholders in the AST with actual parameter values.
     *
     * @param ast The AST to unsubstitute
     * @returns The AST with placeholders replaced
     */
    private async unsubstitute(ast: J): Promise<J> {
        const visitor = new PlaceholderReplacementVisitor(this.substitutions);
        return (await visitor.visit(ast, null))!;
    }
}

/**
 * Visitor that replaces placeholder nodes with actual parameter values.
 */
class PlaceholderReplacementVisitor extends JavaScriptVisitor<any> {
    constructor(private readonly substitutions: Map<string, Parameter>) {
        super();
    }

    async visit<R extends J>(tree: J, p: any, parent?: Cursor): Promise<R | undefined> {
        // Check if this node is a placeholder
        if (this.isPlaceholder(tree)) {
            const replacement = this.replacePlaceholder(tree);
            if (replacement !== tree) {
                return replacement as R;
            }
        }

        // Continue with normal traversal
        return super.visit(tree, p, parent);
    }

    /**
     * Checks if a node is a placeholder.
     *
     * @param node The node to check
     * @returns True if the node is a placeholder
     */
    private isPlaceholder(node: J): boolean {
        if (node.kind === J.Kind.Identifier) {
            const identifier = node as J.Identifier;
            return identifier.simpleName.startsWith(PlaceholderUtils.PLACEHOLDER_PREFIX);
        } else if (node.kind === J.Kind.Literal) {
            const literal = node as J.Literal;
            return literal.valueSource?.startsWith(PlaceholderUtils.PLACEHOLDER_PREFIX) || false;
        }
        return false;
    }

    /**
     * Replaces a placeholder node with the actual parameter value.
     *
     * @param placeholder The placeholder node
     * @returns The replacement node or the original if not a placeholder
     */
    private replacePlaceholder(placeholder: J): J {
        const placeholderText = this.getPlaceholderText(placeholder);

        if (!placeholderText || !placeholderText.startsWith(PlaceholderUtils.PLACEHOLDER_PREFIX)) {
            return placeholder;
        }

        // Find the corresponding parameter
        const param = this.substitutions.get(placeholderText);
        if (!param || param.value === undefined) {
            return placeholder;
        }

        // If the parameter value is an AST node, use it directly
        if (typeof param.value === 'object' && param.value !== null && 'kind' in param.value) {
            // Return the AST node, preserving the original prefix
            return produce(param.value as J, draft => {
                draft.markers = placeholder.markers;
                draft.prefix = placeholder.prefix;
            });
        } else if (param.value instanceof CaptureImpl) {
            return produce(param.value.tree as J, draft => {
                draft.markers = placeholder.markers;
                draft.prefix = placeholder.prefix;
            });
        }

        // For primitive values, create a new literal node
        if (placeholder.kind === J.Kind.Literal) {
            // Create a new literal with the primitive value
            const literal = placeholder as J.Literal;
            return produce({} as J.Literal, draft => {
                // Copy all properties from the original literal
                Object.assign(draft, literal);
                // Override the value and valueSource
                draft.value = param.value;
                draft.valueSource = String(param.value);
            }) as J;
        } else if (placeholder.kind === J.Kind.Identifier) {
            // Create a new identifier with the primitive value
            const identifier = placeholder as J.Identifier;
            return produce({} as J.Identifier, draft => {
                // Copy all properties from the original identifier
                Object.assign(draft, identifier);
                // Override the simpleName
                draft.simpleName = String(param.value);
            }) as J;
        }

        return placeholder;
    }

    /**
     * Gets the placeholder text from a node.
     *
     * @param node The node to get placeholder text from
     * @returns The placeholder text or null
     */
    private getPlaceholderText(node: J): string | null {
        if (node.kind === J.Kind.Identifier) {
            return (node as J.Identifier).simpleName;
        } else if (node.kind === J.Kind.Literal) {
            return (node as J.Literal).valueSource || null;
        }
        return null;
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
    ) {
    }

    /**
     * Applies the template to the current AST.
     *
     * @returns A Promise resolving to the modified AST
     */
    async apply(): Promise<J | undefined> {
        const {tree, loc, mode} = this.coordinates;

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
        const {tree} = this.coordinates;

        // Create a copy of the AST with the prefix from the target
        return produce(this.ast, draft => {
            draft.prefix = (tree as J).prefix;
        });
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

export function template(strings: TemplateStringsArray, ...parameters: any[]): TemplateGenerator {
    // Convert Capture objects to Parameter objects
    const processedParameters = parameters.map(param => {
        // If param is a Capture object with a tree, convert it to a Parameter
        if (param instanceof CaptureImpl && param.tree) {
            return {value: param.tree};
        }
        // If param is already a Parameter, return it as is
        if (param && typeof param === 'object' && 'value' in param) {
            return param;
        }
        // Otherwise, wrap it in a Parameter
        return {value: param};
    });

    return new TemplateGenerator(strings, processedParameters);
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
    ) {
    }

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
                result += PlaceholderUtils.createCapture(capture.name);
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
        const firstStatement = cu.statements[0].element;

        // If the first statement is an expression statement, extract the expression
        if (firstStatement.kind === JS.Kind.ExpressionStatement) {
            return (firstStatement as JS.ExpressionStatement).expression;
        }

        // Otherwise, return the statement itself
        return firstStatement;
    }
}

/**
 * Represents a replacement rule that can match a pattern and apply a template.
 */
export interface ReplaceRule {
    tryOn(node: J, cursor: Cursor, coordinates?: JavaCoordinates): Promise<J | undefined>;
}

/**
 * Configuration for a replacement rule.
 */
export interface ReplaceConfig {
    matching: Pattern;
    as: TemplateGenerator;
}

/**
 * Implementation of a replacement rule.
 */
class ReplaceRuleImpl implements ReplaceRule {
    constructor(
        private readonly matchPattern: Pattern,
        private readonly templateGenerator: TemplateGenerator
    ) {
    }

    async tryOn(node: J, cursor: Cursor, coordinates: JavaCoordinates): Promise<J | undefined> {
        const matcher = this.matchPattern.against(node);

        if (await matcher.matches()) {
            const result = await this.templateGenerator.apply(cursor,
                coordinates || {tree: node, loc: "EXPRESSION_PREFIX", mode: JavaCoordinates.Mode.Replace});
            return result || node; // Fallback to original if template fails
        }

        // Return the original node if no match
        return undefined;
    }
}

/**
 * Creates a replacement rule using a builder function with default parameters.
 *
 * @param builderFn Function that takes capture objects (with defaults) and returns match/template config
 * @returns A replacement rule that can be applied to AST nodes
 *
 * @example
 * const swapOperands = rewrite((left = capture(), right = capture()) => ({
 *     matching: pattern`${left} + ${right}`,
 *     as: template`${right} + ${left}`
 * }));
 *
 * @example
 * // Matching repeated expressions
 * const redundantOrRule = rewrite(() => {
 *     const expr = capture();
 *     return {
 *         matching: pattern`${expr} || ${expr}`,
 *         as: template`${expr}`
 *     };
 * });
 */
export function rewrite(
    builderFn: () => ReplaceConfig
): ReplaceRule {
    // Call with no arguments to trigger default parameters
    const config = builderFn();

    // Ensure we have valid match and template properties
    if (!config.matching || !config.as) {
        throw new Error('Builder function must return an object with match and template properties');
    }

    return new ReplaceRuleImpl(config.matching, config.as);
}
