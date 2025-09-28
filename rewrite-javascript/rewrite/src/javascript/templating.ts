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
import {Cursor, isTree, Tree} from '..';
import {J} from '../java';
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
}

class CaptureImpl implements Capture {
    constructor(
        public readonly name: string
    ) {
    }
}

/**
 * Creates a capture specification for use in template patterns.
 *
 * @returns A Capture object
 *
 * @example
 * // Multiple captures
 * const {left, right} = {left: capture(), right: capture()};
 * const pattern = pattern`${left} + ${right}`;
 *
 * // Repeated patterns using the same capture
 * const expr = capture();
 * const redundantOr = pattern`${expr} || ${expr}`;
 */
export function capture(): Capture {
    return new CaptureImpl(`unnamed_${capture.nextUnnamedId++}`);
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

    /**
     * Creates a matcher for this pattern against a specific AST node.
     *
     * @param ast The AST node to match against
     * @returns A Matcher object
     */
    async match(ast: J): Promise<MatchResult | undefined> {
        const matcher = new Matcher(this, ast);
        const success = await matcher.matches();
        return success ? new MatchResult(matcher.getAll()) : undefined;
    }
}

export class MatchResult implements Pick<Map<string, J>, "get"> {
    constructor(
        private readonly bindings: Map<string, J> = new Map()
    ) {
    }

    get(capture: Capture | string): J | undefined {
        const name = typeof capture === "string" ? capture : capture.name;
        return this.bindings.get(name);
    }
}

/**
 * Matcher for checking if a pattern matches an AST node and extracting captured nodes.
 */
class Matcher {
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
export function pattern(strings: TemplateStringsArray, ...captures: (Capture | string)[]): Pattern {
    const capturesByName = captures.reduce((map, c) => {
        const capture = typeof c === "string" ? new CaptureImpl(c) : c;
        return map.set(capture.name, capture);
    }, new Map<string, Capture>());
    return new Pattern(strings, captures.map(c => capturesByName.get(typeof c === "string" ? c : c.name)!));
}

type JavaCoordinates = {
    tree?: Tree;
    loc?: JavaCoordinates.Location;
    mode?: JavaCoordinates.Mode;
};

namespace JavaCoordinates {
    // FIXME need to come up with the equivalent of `Space.Location` support
    export type Location = 'EXPRESSION_PREFIX' | 'STATEMENT_PREFIX' | 'BLOCK_END';

    export enum Mode {
        Before,
        After,
        Replace,
    }
}

/**
 * Valid parameter types for template literals.
 * - Capture: For pattern matching and reuse
 * - Tree: AST nodes to be inserted directly
 * - Primitives: Values to be converted to literals
 */
export type TemplateParameter = Capture | Tree | string | number | boolean;

/**
 * Template for creating AST nodes.
 *
 * This class provides the public API for template generation.
 * The actual templating logic is handled by the internal TemplateEngine.
 *
 * @example
 * // Generate a literal AST node
 * const result = template`2`.apply(cursor, coordinates);
 *
 * @example
 * // Generate an AST node with a parameter
 * const result = template`${capture()}`.apply(cursor, coordinates);
 */
export class Template {
    /**
     * Creates a new template.
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
     * Applies this template and returns the resulting tree.
     *
     * @param cursor The cursor pointing to the current location in the AST
     * @param tree Input tree
     * @param values values for parameters in template
     * @returns A Promise resolving to the generated AST node
     */
    async apply(cursor: Cursor, tree: J, values?: Pick<Map<string, J>, 'get'>): Promise<J | undefined> {
        return TemplateEngine.applyTemplate(this.templateParts, this.parameters, cursor, {
            tree,
            mode: JavaCoordinates.Mode.Replace
        }, values);
    }
}

export function template(strings: TemplateStringsArray, ...parameters: TemplateParameter[]): Template {
    // Convert parameters to Parameter objects (no longer need to check for mutable tree property)
    const processedParameters = parameters.map(param => {
        // Just wrap each parameter value in a Parameter object
        return {value: param};
    });

    return new Template(strings, processedParameters);
}

/**
 * Parameter specification for template generation.
 * Represents a placeholder in a template that will be replaced with a parameter value.
 */
interface Parameter {
    /**
     * The value to substitute into the template.
     */
    value: any;
}

/**
 * Internal template engine - handles the core templating logic.
 * Not exported, so only visible within this module.
 */
class TemplateEngine {
    /**
     * Applies a template with optional match results from pattern matching.
     *
     * @param templateParts The string parts of the template
     * @param parameters The parameters between the string parts
     * @param cursor The cursor pointing to the current location in the AST
     * @param coordinates The coordinates specifying where and how to insert the generated AST
     * @param values Map of capture names to values to replace the parameters with
     * @returns A Promise resolving to the generated AST node
     */
    static async applyTemplate(
        templateParts: TemplateStringsArray,
        parameters: Parameter[],
        cursor: Cursor,
        coordinates: JavaCoordinates,
        values: Pick<Map<string, J>, 'get'> = new Map()
    ): Promise<J | undefined> {
        // Build the template string with parameter placeholders
        const templateString = TemplateEngine.buildTemplateString(templateParts, parameters);

        // If the template string is empty, return undefined
        if (!templateString.trim()) {
            return undefined;
        }

        // Parse the template string into an AST
        const parser = new JavaScriptParser();
        const parseGenerator = parser.parse({text: templateString, sourcePath: 'template.ts'});
        const cu: JS.CompilationUnit = (await parseGenerator.next()).value as JS.CompilationUnit;

        // Check if there are any statements
        if (!cu.statements || cu.statements.length === 0) {
            return undefined;
        }

        // Extract the relevant part of the AST
        const firstStatement = cu.statements[0].element;
        const ast = firstStatement.kind === JS.Kind.ExpressionStatement ?
            (firstStatement as JS.ExpressionStatement).expression :
            firstStatement;

        // Create substitutions map for placeholders
        const substitutions = new Map<string, Parameter>();
        for (let i = 0; i < parameters.length; i++) {
            const placeholder = `${PlaceholderUtils.PLACEHOLDER_PREFIX}${i}__`;
            substitutions.set(placeholder, typeof parameters[i].value === 'string' ? {value: values.get(parameters[i].value) || parameters[i].value} : parameters[i]);
        }

        // Unsubstitute placeholders with actual parameter values and match results
        const visitor = new PlaceholderReplacementVisitor(substitutions, values);
        const unsubstitutedAst = (await visitor.visit(ast, null))!;

        // Apply the template to the current AST
        return new TemplateApplier(cursor, coordinates, unsubstitutedAst, parameters).apply();
    }

    /**
     * Builds a template string with parameter placeholders.
     *
     * @param templateParts The string parts of the template
     * @param parameters The parameters between the string parts
     * @returns The template string
     */
    private static buildTemplateString(templateParts: TemplateStringsArray, parameters: Parameter[]): string {
        let result = '';
        for (let i = 0; i < templateParts.length; i++) {
            result += templateParts[i];
            if (i < parameters.length) {
                if (parameters[i].value instanceof CaptureImpl || typeof parameters[i].value === 'string' || isTree(parameters[i].value)) {
                    const placeholder = `${PlaceholderUtils.PLACEHOLDER_PREFIX}${i}__`;
                    result += placeholder;
                } else {
                    result += parameters[i].value;
                }
            }
        }
        return result;
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
            return match ? {name: match[1]} : null;
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
 * Visitor that replaces placeholder nodes with actual parameter values.
 */
class PlaceholderReplacementVisitor extends JavaScriptVisitor<any> {
    constructor(
        private readonly substitutions: Map<string, Parameter>,
        private readonly values: Pick<Map<string, J>, 'get'> = new Map()
    ) {
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

        // If the parameter value is a Capture, look up the matched result
        if (param.value instanceof CaptureImpl) {
            const matchedNode = this.values.get(param.value.name);
            if (matchedNode) {
                return produce(matchedNode, draft => {
                    draft.markers = placeholder.markers;
                    draft.prefix = placeholder.prefix;
                });
            }
            // If no match found, return placeholder unchanged
            return placeholder;
        }

        // If the parameter value is an AST node, use it directly
        if (isTree(param.value)) {
            // Return the AST node, preserving the original prefix
            return produce(param.value as J, draft => {
                draft.markers = placeholder.markers;
                draft.prefix = placeholder.prefix;
            });
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
        const {loc} = this.coordinates;

        // Apply the template based on the location and mode
        switch (loc || 'EXPRESSION_PREFIX') {
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
        return tree ? produce(this.ast, draft => {
            draft.prefix = (tree as J).prefix;
        }) : this.ast;
    }

    /**
     * Applies the template to a statement.
     *
     * @returns A Promise resolving to the modified AST
     */
    private async applyToStatement(): Promise<J | undefined> {
        const {tree} = this.coordinates;

        // Create a copy of the AST with the prefix from the target
        return produce(this.ast, draft => {
            draft.prefix = (tree as J).prefix;
        });
    }

    /**
     * Applies the template to a block.
     *
     * @returns A Promise resolving to the modified AST
     */
    private async applyToBlock(): Promise<J | undefined> {
        const {tree} = this.coordinates;

        // Create a copy of the AST with the prefix from the target
        return produce(this.ast, draft => {
            draft.prefix = (tree as J).prefix;
        });
    }
}

/**
 * Processor for template strings.
 * Converts a template string with captures into an AST pattern.
 */
class TemplateProcessor {
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
        const parseGenerator = parser.parse({text: templateString, sourcePath: 'template.ts'});
        const cu: JS.CompilationUnit = (await parseGenerator.next()).value as JS.CompilationUnit;
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
export interface RewriteRule {
    tryOn(cursor: Cursor, node: J): Promise<J | undefined>;
}

/**
 * Configuration for a replacement rule.
 */
export interface RewriteConfig {
    before: Pattern | Pattern[];
    after: Template;
}

/**
 * Implementation of a replacement rule.
 */
class RewriteRuleImpl implements RewriteRule {
    constructor(
        private readonly before: Pattern[],
        private readonly after: Template
    ) {
    }

    async tryOn(cursor: Cursor, node: J): Promise<J | undefined> {
        for (const pattern of this.before) {
            const match = await pattern.match(node);

            if (match) {
                const result = await this.after.apply(cursor, node, match);
                if (result) {
                    return result;
                }
            }
        }

        // Return undefined if no patterns match
        return undefined;
    }
}

/**
 * Creates a replacement rule using a capture context and configuration.
 *
 * @param builderFn Function that takes a capture context and returns before/after configuration
 * @returns A replacement rule that can be applied to AST nodes
 *
 * @example
 * // Single pattern
 * const swapOperands = replace<J.Binary>(() => ({
 *     before: pattern`${"left"} + ${"right"}`,
 *     after: template`${"right"} + ${"left"}`
 * }));
 *
 * @example
 * // Multiple patterns
 * const normalizeComparisons = replace<J.Binary>(() => ({
 *     before: [
 *         pattern`${"left"} == ${"right"}`,
 *         pattern`${"left"} === ${"right"}`
 *     ],
 *     after: template`${"left"} === ${"right"}`
 * }));
 */
export function rewrite(
    builderFn: () => RewriteConfig
): RewriteRule {
    const config = builderFn();

    // Ensure we have valid before and after properties
    if (!config.before || !config.after) {
        throw new Error('Builder function must return an object with before and after properties');
    }

    return new RewriteRuleImpl(Array.isArray(config.before) ? config.before : [config.before], config.after);
}
