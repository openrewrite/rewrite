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
import {Cursor} from '../..';
import {J, Type} from '../../java';
import {JS} from '../index';
import {JavaScriptVisitor} from '../visitor';
import {produceAsync} from '../../visitor';
import {updateIfChanged} from '../../util';
import {Any, Capture, PatternOptions} from './types';
import {CAPTURE_CAPTURING_SYMBOL, CAPTURE_NAME_SYMBOL, CAPTURE_TYPE_SYMBOL, CaptureImpl} from './capture';
import {PatternMatchingComparator} from './comparator';
import {CaptureMarker, CaptureStorageValue, PlaceholderUtils, templateCache, WRAPPERS_MAP_SYMBOL} from './utils';
import {isTree, Tree} from "../../tree";

/**
 * Builder for creating patterns programmatically.
 * Use when pattern structure is not known at compile time.
 *
 * @example
 * // Loop-based pattern generation
 * const builder = Pattern.builder().code('myFunction(');
 * for (let i = 0; i < argCount; i++) {
 *     if (i > 0) builder.code(', ');
 *     builder.capture(capture(`arg${i}`));
 * }
 * builder.code(')');
 * const pat = builder.build();
 *
 * @example
 * // Conditional pattern construction
 * const builder = Pattern.builder().code('foo(');
 * builder.capture(capture('first'));
 * if (needsSecondArg) {
 *     builder.code(', ').capture(capture('second'));
 * }
 * builder.code(')');
 * const pat = builder.build();
 */
export class PatternBuilder {
    private parts: string[] = [];
    private captures: (Capture | Any<any>)[] = [];

    /**
     * Adds a static string part to the pattern.
     *
     * @param str The string to add
     * @returns This builder for chaining
     */
    code(str: string): this {
        // If there are already captures, we need to add an empty string before this
        if (this.captures.length > this.parts.length) {
            this.parts.push('');
        }
        // Append to the last part or start a new one
        if (this.parts.length === 0) {
            this.parts.push(str);
        } else {
            this.parts[this.parts.length - 1] += str;
        }
        return this;
    }

    /**
     * Adds a capture to the pattern.
     *
     * @param value The capture object (Capture or Any) or string name
     * @returns This builder for chaining
     */
    capture(value: Capture | Any<any> | string): this {
        // Ensure we have a part for after this capture
        if (this.parts.length === 0) {
            this.parts.push('');
        }
        // Convert string to Capture if needed
        const captureObj = typeof value === 'string' ? new CaptureImpl(value) : value;
        this.captures.push(captureObj as any);
        // Add an empty string for the next part
        this.parts.push('');
        return this;
    }

    /**
     * Builds the pattern from accumulated parts and captures.
     *
     * @returns A Pattern instance
     */
    build(): Pattern {
        // Ensure parts array is one longer than captures array
        while (this.parts.length <= this.captures.length) {
            this.parts.push('');
        }

        // Create a synthetic TemplateStringsArray
        const templateStrings = this.parts.slice() as any;
        templateStrings.raw = this.parts.slice();
        Object.defineProperty(templateStrings, 'raw', {
            value: this.parts.slice(),
            writable: false
        });

        // Delegate to the pattern() function
        return pattern(templateStrings, ...this.captures);
    }
}

/**
 * Represents a pattern that can be matched against AST nodes.
 */
export class Pattern {
    private _options: PatternOptions = {};

    /**
     * Gets the configuration options for this pattern.
     * @readonly
     */
    get options(): Readonly<PatternOptions> {
        return this._options;
    }

    /**
     * Creates a new builder for constructing patterns programmatically.
     *
     * @returns A new PatternBuilder instance
     *
     * @example
     * const pat = Pattern.builder()
     *     .code('function ')
     *     .capture(capture('name'))
     *     .code('() { return ')
     *     .capture(capture('value'))
     *     .code('; }')
     *     .build();
     */
    static builder(): PatternBuilder {
        return new PatternBuilder();
    }

    /**
     * Creates a new pattern from template parts and captures.
     *
     * @param templateParts The string parts of the template
     * @param captures The captures between the string parts (can be Capture or Any)
     */
    constructor(
        public readonly templateParts: TemplateStringsArray,
        public readonly captures: (Capture | Any<any>)[]
    ) {
    }

    /**
     * Configures this pattern with additional options.
     *
     * @param options Configuration options
     * @returns This pattern for method chaining
     *
     * @example
     * pattern`forwardRef((${props}, ${ref}) => ${body})`
     *     .configure({
     *         context: ['import { forwardRef } from "react"'],
     *         dependencies: {'@types/react': '^18.0.0'}
     *     })
     */
    configure(options: PatternOptions): Pattern {
        this._options = {...this._options, ...options};
        return this;
    }

    /**
     * Creates a matcher for this pattern against a specific AST node.
     *
     * @param ast The AST node to match against
     * @param cursor Optional cursor at the node's position in a larger tree. Used for context-aware
     *               capture constraints to navigate to parent nodes. If omitted, a cursor will be
     *               created at the ast root, allowing constraints to navigate within the matched subtree.
     * @returns A MatchResult if the pattern matches, undefined otherwise
     */
    async match(ast: J, cursor?: Cursor): Promise<MatchResult | undefined> {
        const matcher = new Matcher(this, ast, cursor);
        const success = await matcher.matches();
        if (!success) {
            return undefined;
        }
        // Create MatchResult with unified storage
        const storage = (matcher as any).storage;
        return new MatchResult(new Map(storage));
    }
}

/**
 * Result of a successful pattern match containing captured values.
 *
 * Provides access to captured AST nodes from pattern matching operations.
 * Use the `get()` method to retrieve captured values by name or by Capture object.
 *
 * @example
 * const x = capture('x');
 * const pat = pattern`foo(${x})`;
 * const match = await pat.match(someNode);
 * if (match) {
 *     const captured = match.get('x');  // Get by name
 *     // or
 *     const captured = match.get(x);    // Get by Capture object
 * }
 *
 * @example
 * // Variadic captures return arrays
 * const args = capture({ variadic: true });
 * const pat = pattern`foo(${args})`;
 * const match = await pat.match(methodInvocation);
 * if (match) {
 *     const capturedArgs = match.get(args);  // Returns J[] for variadic captures
 * }
 */
export class MatchResult {
    constructor(
        private readonly storage: Map<string, CaptureStorageValue> = new Map()
    ) {
    }

    // Overload: get with variadic Capture (array type) returns array
    get<T>(capture: Capture<T[]>): T[] | undefined;
    // Overload: get with regular Capture returns single value
    get<T>(capture: Capture<T>): T | undefined;
    // Overload: get with string returns J
    get(capture: string): J | undefined;
    // Implementation
    get(capture: Capture<any> | string): J | J[] | undefined {
        // Use symbol to get internal name without triggering Proxy
        const name = typeof capture === "string" ? capture : ((capture as any)[CAPTURE_NAME_SYMBOL] || capture.getName());
        const value = this.storage.get(name);
        if (value === undefined) {
            return undefined;
        }
        return this.extractElements(value);
    }

    /**
     * Extracts semantic elements from storage value.
     * For wrappers, extracts the .element; for arrays, returns array of elements.
     *
     * @param value The storage value
     * @returns The semantic element(s)
     */
    private extractElements(value: CaptureStorageValue): J {
        if (Array.isArray(value)) {
            // Check if it's an array of wrappers
            if (value.length > 0 && (value[0] as any).element !== undefined) {
                // Array of J.RightPadded - extract elements
                return (value as J.RightPadded<J>[]).map(w => w.element) as any;
            }
            // Already an array of elements
            return value as any;
        }
        // Check if it's a scalar wrapper
        if ((value as any).element !== undefined) {
            return (value as J.RightPadded<J>).element;
        }
        // Scalar element
        return value as J;
    }

    /**
     * Internal method to get wrappers (used by template expansion).
     * Returns both scalar and variadic wrappers.
     * @internal
     */
    [WRAPPERS_MAP_SYMBOL](): Map<string, J.RightPadded<J> | J.RightPadded<J>[]> {
        const result = new Map<string, J.RightPadded<J> | J.RightPadded<J>[]>();
        for (const [name, value] of this.storage) {
            if (Array.isArray(value) && value.length > 0 && (value[0] as any).element !== undefined) {
                // This is an array of wrappers (variadic)
                result.set(name, value as J.RightPadded<J>[]);
            } else if (!Array.isArray(value) && (value as any).element !== undefined) {
                // This is a scalar wrapper
                result.set(name, value as J.RightPadded<J>);
            }
        }
        return result;
    }
}

/**
 * Matcher for checking if a pattern matches an AST node and extracting captured nodes.
 */
class Matcher {
    // Unified storage: holds J for scalar captures, J.RightPadded<J>[] or J[] for variadic captures
    private readonly storage = new Map<string, CaptureStorageValue>();
    private patternAst?: J;

    /**
     * Creates a new matcher for a pattern against an AST node.
     *
     * @param pattern The pattern to match
     * @param ast The AST node to match against
     * @param cursor Optional cursor at the AST node's position
     */
    constructor(
        private readonly pattern: Pattern,
        private readonly ast: J,
        cursor?: Cursor
    ) {
        // If no cursor provided, create one at the ast root so constraints can navigate up
        this.cursor = cursor ?? new Cursor(ast, undefined);
    }

    private readonly cursor: Cursor;

    /**
     * Checks if the pattern matches the AST node.
     *
     * @returns true if the pattern matches, false otherwise
     */
    async matches(): Promise<boolean> {
        if (!this.patternAst) {
            // Prefer 'context' over deprecated 'imports'
            const contextStatements = this.pattern.options.context || this.pattern.options.imports || [];
            const templateProcessor = new TemplateProcessor(
                this.pattern.templateParts,
                this.pattern.captures,
                contextStatements,
                this.pattern.options.dependencies || {}
            );
            this.patternAst = await templateProcessor.toAstPattern();
        }

        return this.matchNode(this.patternAst, this.ast);
    }

    /**
     * Gets all captured nodes (projected view: extracts elements from wrappers).
     *
     * @returns A map of capture names to captured nodes
     */
    getAll(): Map<string, J> {
        const result = new Map<string, J>();
        for (const [name, value] of this.storage) {
            result.set(name, this.extractElements(value));
        }
        return result;
    }

    /**
     * Extracts semantic elements from storage value.
     * For wrappers, extracts the .element; for arrays, returns array of elements.
     *
     * @param value The storage value
     * @returns The semantic element(s)
     */
    private extractElements(value: CaptureStorageValue): J {
        if (Array.isArray(value)) {
            // Check if it's an array of wrappers
            if (value.length > 0 && (value[0] as any).element !== undefined) {
                // Array of J.RightPadded - extract elements
                return (value as J.RightPadded<J>[]).map(w => w.element) as any;
            }
            // Already an array of elements
            return value as any;
        }
        // Check if it's a scalar wrapper
        if ((value as any).element !== undefined) {
            return (value as J.RightPadded<J>).element;
        }
        // Scalar element
        return value as J;
    }

    /**
     * Matches a pattern node against a target node.
     *
     * @param pattern The pattern node
     * @param target The target node
     * @returns true if the pattern matches the target, false otherwise
     */
    private async matchNode(pattern: J, target: J): Promise<boolean> {
        // Always delegate to the comparator visitor, which handles:
        // - Capture detection and constraint evaluation
        // - Kind checking
        // - Deep structural comparison
        // This centralizes all matching logic in one place
        const lenientTypeMatching = this.pattern.options.lenientTypeMatching ?? true;
        const comparator = new PatternMatchingComparator({
            handleCapture: (capture, t, w) => this.handleCapture(capture, t, w),
            handleVariadicCapture: (capture, ts, ws) => this.handleVariadicCapture(capture, ts, ws),
            saveState: () => this.saveState(),
            restoreState: (state) => this.restoreState(state)
        }, lenientTypeMatching);
        // Pass cursors to allow constraints to navigate to root
        // Pattern cursor is undefined (pattern is the root), target cursor is provided by user
        return await comparator.compare(pattern, target, undefined, this.cursor);
    }

    /**
     * Saves the current state of storage for backtracking.
     *
     * @returns A snapshot of the current state
     */
    private saveState(): Map<string, CaptureStorageValue> {
        return new Map(this.storage);
    }

    /**
     * Restores a previously saved state for backtracking.
     *
     * @param state The state to restore
     */
    private restoreState(state: Map<string, CaptureStorageValue>): void {
        this.storage.clear();
        state.forEach((value, key) => this.storage.set(key, value));
    }

    /**
     * Handles a capture placeholder.
     *
     * @param capture The pattern node capture
     * @param target The target node
     * @param wrapper Optional wrapper containing the target (for preserving markers)
     * @returns true if the capture is successful, false otherwise
     */
    private handleCapture(capture: CaptureMarker, target: J, wrapper?: J.RightPadded<J>): boolean {
        const captureName = capture.captureName;

        if (!captureName) {
            return false;
        }

        // Find the original capture object to get capturing flag
        // Note: Constraints are now evaluated in PatternMatchingComparator where cursor is correctly positioned
        const captureObj = this.pattern.captures.find(c => c.getName() === captureName);

        // Only store the binding if this is a capturing placeholder
        const capturing = (captureObj as any)?.[CAPTURE_CAPTURING_SYMBOL] ?? true;
        if (capturing) {
            // Store wrapper if available (preserves markers), otherwise store element
            this.storage.set(captureName, wrapper ?? target);
        }

        return true;
    }

    /**
     * Handles a variadic capture placeholder.
     *
     * @param capture The pattern node capture (the variadic capture)
     * @param targets The target nodes that were matched
     * @param wrappers Optional wrappers to preserve markers
     * @returns true if the capture is successful, false otherwise
     */
    private handleVariadicCapture(capture: CaptureMarker, targets: J[], wrappers?: J.RightPadded<J>[]): boolean {
        const captureName = capture.captureName;

        if (!captureName) {
            return false;
        }

        // Find the original capture object to get capturing flag
        // Note: Constraints are now evaluated in PatternMatchingComparator where cursor is correctly positioned
        const captureObj = this.pattern.captures.find(c => c.getName() === captureName);

        // Only store the binding if this is a capturing placeholder
        const capturing = (captureObj as any)?.[CAPTURE_CAPTURING_SYMBOL] ?? true;
        if (capturing) {
            // Store the richest representation: wrappers if available, otherwise elements
            if (wrappers && wrappers.length > 0) {
                this.storage.set(captureName, wrappers);
            } else {
                this.storage.set(captureName, targets);
            }
        }

        return true;
    }
}

/**
 * Visitor that attaches CaptureMarkers to capture identifiers in the AST.
 * Markers are attached to Identifiers, then moved up to wrappers (RightPadded, ExpressionStatement).
 * Uses JavaScriptVisitor to properly handle AST traversal and avoid cycles in Type objects.
 */
class MarkerAttachmentVisitor extends JavaScriptVisitor<undefined> {
    constructor(private readonly captures: (Capture | Any<any>)[]) {
        super();
    }

    /**
     * Attaches CaptureMarker to capture identifiers.
     */
    protected override async visitIdentifier(ident: J.Identifier, p: undefined): Promise<J | undefined> {
        // First call parent to handle standard visitation
        const visited = await super.visitIdentifier(ident, p);
        if (!visited || visited.kind !== J.Kind.Identifier) {
            return visited;
        }
        ident = visited as J.Identifier;

        // Check if this is a capture placeholder
        if (ident.simpleName?.startsWith(PlaceholderUtils.CAPTURE_PREFIX)) {
            const captureInfo = PlaceholderUtils.parseCapture(ident.simpleName);
            if (captureInfo) {
                // Find the original capture object to get variadic options and constraint
                const captureObj = this.captures.find(c => c.getName() === captureInfo.name);
                const variadicOptions = captureObj?.getVariadicOptions();
                const constraint = captureObj?.getConstraint?.();

                // Add CaptureMarker to the Identifier with constraint
                const marker = new CaptureMarker(captureInfo.name, variadicOptions, constraint);
                return updateIfChanged(ident, {
                    markers: {
                        ...ident.markers,
                        markers: [...ident.markers.markers, marker]
                    }
                });
            }
        }

        return ident;
    }

    /**
     * Propagates markers from element to RightPadded wrapper.
     */
    public override async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, p: undefined): Promise<J.RightPadded<T>> {
        if (!isTree(right.element)) {
            return right;
        }

        const visitedElement = await this.visit(right.element as J, p);
        if (visitedElement && visitedElement !== right.element as Tree) {
            return produceAsync<J.RightPadded<T>>(right, async (draft: any) => {
                // Visit element first
                if (right.element && (right.element as any).kind) {
                    // Check if element has a CaptureMarker
                    const elementMarker = PlaceholderUtils.getCaptureMarker(visitedElement);
                    if (elementMarker) {
                        draft.markers.markers.push(elementMarker);
                    } else {
                        draft.element = visitedElement;
                    }
                }
            });
        }

        return right;
    }

    /**
     * Propagates markers from expression to ExpressionStatement.
     */
    protected override async visitExpressionStatement(expressionStatement: JS.ExpressionStatement, p: undefined): Promise<J | undefined> {
        // Visit the expression
        const visitedExpression = await this.visit(expressionStatement.expression, p);

        // Check if expression has a CaptureMarker
        const expressionMarker = PlaceholderUtils.getCaptureMarker(visitedExpression as any);
        if (expressionMarker) {
            return updateIfChanged(expressionStatement, {
                markers: {
                    ...expressionStatement.markers,
                    markers: [...expressionStatement.markers.markers, expressionMarker]
                },
            });
        }

        // No marker to move, just update with visited expression
        return updateIfChanged(expressionStatement, {
            expression: visitedExpression
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
     * @param captures The captures between the string parts (can be Capture or Any)
     * @param contextStatements Context declarations (imports, types, etc.) to prepend for type attribution
     * @param dependencies NPM dependencies for type attribution
     */
    constructor(
        private readonly templateParts: TemplateStringsArray,
        private readonly captures: (Capture | Any<any>)[],
        private readonly contextStatements: string[] = [],
        private readonly dependencies: Record<string, string> = {}
    ) {
    }

    /**
     * Converts the template to an AST pattern.
     *
     * @returns A Promise resolving to the AST pattern
     */
    async toAstPattern(): Promise<J> {
        // Generate type preamble for captures with types
        const preamble = this.generateTypePreamble();

        // Combine template parts and placeholders
        const templateString = this.buildTemplateString();

        // Add preamble to context statements (so they're skipped during extraction)
        const contextWithPreamble = preamble.length > 0
            ? [...this.contextStatements, ...preamble]
            : this.contextStatements;

        // Use cache to get or parse the compilation unit
        const cu = await templateCache.getOrParse(
            templateString,
            this.captures,
            contextWithPreamble,
            this.dependencies
        );

        // Extract the relevant part of the AST
        // The pattern code is always the last statement (after context + preamble)
        return await this.extractPatternFromAst(cu);
    }

    /**
     * Generates type preamble declarations for captures with type annotations.
     *
     * @returns Array of preamble statements
     */
    private generateTypePreamble(): string[] {
        const preamble: string[] = [];
        for (const capture of this.captures) {
            const captureName = (capture as any)[CAPTURE_NAME_SYMBOL] || capture.getName();
            const captureType = (capture as any)[CAPTURE_TYPE_SYMBOL];
            if (captureType) {
                // Convert Type to string if needed
                const typeString = typeof captureType === 'string'
                    ? captureType
                    : this.typeToString(captureType);
                const placeholder = PlaceholderUtils.createCapture(captureName, undefined);
                preamble.push(`let ${placeholder}: ${typeString};`);
            } else {
                const placeholder = PlaceholderUtils.createCapture(captureName, undefined);
                preamble.push(`let ${placeholder};`);
            }
        }
        return preamble;
    }

    /**
     * Builds a template string with placeholders for captures.
     * If the template looks like a block pattern, wraps it in a function.
     *
     * @returns The template string
     */
    private buildTemplateString(): string {
        let result = '';
        for (let i = 0; i < this.templateParts.length; i++) {
            result += this.templateParts[i];
            if (i < this.captures.length) {
                const capture = this.captures[i];
                // Use symbol to access capture name without triggering Proxy
                const captureName = (capture as any)[CAPTURE_NAME_SYMBOL] || capture.getName();
                result += PlaceholderUtils.createCapture(captureName, undefined);
            }
        }

        // Always wrap in function body - let the parser decide what it is,
        // then we'll extract intelligently based on what was parsed
        return `function __PATTERN__() { ${result} }`;
    }

    /**
     * Converts a Type instance to a TypeScript type string.
     *
     * @param type The Type instance
     * @returns A TypeScript type string
     */
    private typeToString(type: Type): string {
        // Handle Type.Class and Type.ShallowClass - return their fully qualified names
        if (type.kind === Type.Kind.Class || type.kind === Type.Kind.ShallowClass) {
            const classType = type as Type.Class;
            return classType.fullyQualifiedName;
        }

        // Handle Type.Primitive - map to TypeScript primitive types
        if (type.kind === Type.Kind.Primitive) {
            const primitiveType = type as Type.Primitive;
            switch (primitiveType.keyword) {
                case 'String':
                    return 'string';
                case 'boolean':
                    return 'boolean';
                case 'double':
                case 'float':
                case 'int':
                case 'long':
                case 'short':
                case 'byte':
                    return 'number';
                case 'void':
                    return 'void';
                default:
                    return 'any';
            }
        }

        // Handle Type.Array - render component type plus []
        if (type.kind === Type.Kind.Array) {
            const arrayType = type as Type.Array;
            const componentTypeString = this.typeToString(arrayType.elemType);
            return `${componentTypeString}[]`;
        }

        // For other types, return 'any' as a fallback
        // TODO: Implement proper Type to string conversion for other Type.Kind values
        return 'any';
    }

    /**
     * Extracts the pattern from the parsed AST.
     * The pattern code is always the last statement in the compilation unit
     * (after all context statements and type preamble declarations).
     *
     * @param cu The compilation unit
     * @returns The extracted pattern
     */
    private async extractPatternFromAst(cu: JS.CompilationUnit): Promise<J> {
        // Check if we have any statements
        if (!cu.statements || cu.statements.length === 0) {
            throw new Error(`No statements found in compilation unit`);
        }

        // The pattern code is always the last statement
        const lastStatement = cu.statements[cu.statements.length - 1].element;

        // Extract from wrapper using shared utility
        const extracted = PlaceholderUtils.extractFromWrapper(lastStatement, '__PATTERN__', 'Pattern');

        // Attach CaptureMarkers to capture identifiers
        return await this.attachCaptureMarkers(extracted);
    }

    /**
     * Attaches CaptureMarkers to capture identifiers in the AST.
     * This allows efficient capture detection without string parsing.
     * Uses JavaScriptVisitor to properly handle AST traversal and avoid cycles in Type objects.
     *
     * @param ast The AST to process
     * @returns The AST with CaptureMarkers attached
     */
    private async attachCaptureMarkers(ast: J): Promise<J> {
        const visitor = new MarkerAttachmentVisitor(this.captures);
        return (await visitor.visit(ast, undefined))!;
    }
}

/**
 * Tagged template function for creating patterns.
 *
 * @param strings The string parts of the template
 * @param captures The captures between the string parts (Capture, Any, or string names)
 * @returns A Pattern object
 *
 * @example
 * // Using the same capture multiple times for repeated patterns
 * const expr = capture('expr');
 * const redundantOr = pattern`${expr} || ${expr}`;
 *
 * @example
 * // Using any() for non-capturing matches
 * const pat = pattern`foo(${any()})`;
 */
export function pattern(strings: TemplateStringsArray, ...captures: (Capture | Any<any> | string)[]): Pattern {
    const capturesByName = captures.reduce((map, c) => {
        const capture = typeof c === "string" ? new CaptureImpl(c) : c;
        // Use symbol to get internal name without triggering Proxy
        const name = (capture as any)[CAPTURE_NAME_SYMBOL] || capture.getName();
        return map.set(name, capture);
    }, new Map<string, Capture | Any<any>>());
    return new Pattern(strings, captures.map(c => {
        // Use symbol to get internal name without triggering Proxy
        const name = typeof c === "string" ? c : ((c as any)[CAPTURE_NAME_SYMBOL] || c.getName());
        return capturesByName.get(name)!;
    }));
}
