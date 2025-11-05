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
import {produce} from 'immer';
import {J, Type} from '../../java';
import {JS} from '../index';
import {randomId} from '../../uuid';
import {Any, Capture, PatternOptions} from './types';
import {CAPTURE_CAPTURING_SYMBOL, CAPTURE_NAME_SYMBOL, CAPTURE_TYPE_SYMBOL, CaptureImpl} from './capture';
import {PatternMatchingComparator} from './comparator';
import {CaptureMarker, CaptureStorageValue, PlaceholderUtils, templateCache, WRAPPERS_MAP_SYMBOL} from './utils';

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
     * pattern`isDate(${capture('date')})`
     *     .configure({
     *         imports: ['import { isDate } from \"util\"'],
     *         dependencies: { 'util': '^1.0.0' }
     *     })
     */
    configure(options: PatternOptions): Pattern {
        this._options = { ...this._options, ...options };
        return this;
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
        // Check if pattern is a capture placeholder
        if (PlaceholderUtils.isCapture(pattern)) {
            return this.handleCapture(pattern, target);
        }

        // Check if nodes have the same kind
        if (pattern.kind !== target.kind) {
            return false;
        }

        // Use the pattern matching comparator with configured lenient type matching
        // Default to true for backward compatibility with existing patterns
        const lenientTypeMatching = this.pattern.options.lenientTypeMatching ?? true;
        const comparator = new PatternMatchingComparator({
            handleCapture: (p, t) => this.handleCapture(p, t),
            handleVariadicCapture: (p, ts, ws) => this.handleVariadicCapture(p, ts, ws),
            saveState: () => this.saveState(),
            restoreState: (state) => this.restoreState(state)
        }, lenientTypeMatching);
        return await comparator.compare(pattern, target);
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
     * @param pattern The pattern node
     * @param target The target node
     * @param wrapper Optional wrapper containing the target (for preserving markers)
     * @returns true if the capture is successful, false otherwise
     */
    private handleCapture(pattern: J, target: J, wrapper?: J.RightPadded<J>): boolean {
        const captureName = PlaceholderUtils.getCaptureName(pattern);

        if (!captureName) {
            return false;
        }

        // Find the original capture object to get constraint and capturing flag
        const captureObj = this.pattern.captures.find(c => c.getName() === captureName);
        const constraint = captureObj?.getConstraint?.();

        // Apply constraint if present
        if (constraint && !constraint(target as any)) {
            return false;
        }

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
     * @param pattern The pattern node (the variadic capture)
     * @param targets The target nodes that were matched
     * @param wrappers Optional wrappers to preserve markers
     * @returns true if the capture is successful, false otherwise
     */
    private handleVariadicCapture(pattern: J, targets: J[], wrappers?: J.RightPadded<J>[]): boolean {
        const captureName = PlaceholderUtils.getCaptureName(pattern);

        if (!captureName) {
            return false;
        }

        // Find the original capture object to get constraint and capturing flag
        const captureObj = this.pattern.captures.find(c => c.getName() === captureName);
        const constraint = captureObj?.getConstraint?.();

        // Apply constraint if present - for variadic captures, constraint receives the array of elements
        if (constraint && !constraint(targets as any)) {
            return false;
        }

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
        return this.extractPatternFromAst(cu);
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
                preamble.push(`const ${placeholder}: ${typeString};`);
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

        // Check if this looks like a block pattern (starts with { and contains statement keywords)
        const trimmed = result.trim();
        if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
            // Check for statement keywords that indicate this is a block, not an object literal
            const hasStatementKeywords = /\b(return|if|for|while|do|switch|try|throw|break|continue|const|let|var|function|class)\b/.test(result);
            if (hasStatementKeywords) {
                // Wrap in a function to ensure it parses as a block
                return `function __PATTERN__() ${result}`;
            }
        }

        return result;
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
    private extractPatternFromAst(cu: JS.CompilationUnit): J {
        // Check if we have any statements
        if (!cu.statements || cu.statements.length === 0) {
            throw new Error(`No statements found in compilation unit`);
        }

        // The pattern code is always the last statement
        const lastStatement = cu.statements[cu.statements.length - 1].element;

        let extracted: J;

        // Check if this is our wrapper function for block patterns
        if (lastStatement.kind === J.Kind.MethodDeclaration) {
            const method = lastStatement as J.MethodDeclaration;
            if (method.name?.simpleName === '__PATTERN__' && method.body) {
                // Extract the block from the wrapper function
                extracted = method.body;
            } else {
                extracted = lastStatement;
            }
        } else if (lastStatement.kind === JS.Kind.ExpressionStatement) {
            // If the statement is an expression statement, extract the expression
            extracted = (lastStatement as JS.ExpressionStatement).expression;
        } else {
            // Otherwise, return the statement itself
            extracted = lastStatement;
        }

        // Attach CaptureMarkers to capture identifiers
        return this.attachCaptureMarkers(extracted);
    }

    /**
     * Attaches CaptureMarkers to capture identifiers in the AST.
     * This allows efficient capture detection without string parsing.
     *
     * @param ast The AST to process
     * @returns The AST with CaptureMarkers attached
     */
    private attachCaptureMarkers(ast: J): J {
        const visited = new Set<J | object>();
        return produce(ast, draft => {
            this.visitAndAttachMarkers(draft, visited);
        });
    }

    /**
     * Recursively visits AST nodes and attaches CaptureMarkers to capture identifiers.
     * For statement-level captures (identifiers in ExpressionStatement), the marker
     * is attached to the ExpressionStatement itself rather than the nested identifier.
     *
     * @param node The node to visit
     * @param visited Set of already visited nodes to avoid cycles
     */
    private visitAndAttachMarkers(node: any, visited: Set<J | object>): void {
        if (!node || typeof node !== 'object' || visited.has(node)) {
            return;
        }

        // Mark as visited to avoid cycles
        visited.add(node);

        // Check if this is an ExpressionStatement containing a capture identifier
        // For statement-level captures, we attach the marker to the ExpressionStatement itself
        if (node.kind === JS.Kind.ExpressionStatement &&
            node.expression?.kind === J.Kind.Identifier &&
            node.expression.simpleName?.startsWith(PlaceholderUtils.CAPTURE_PREFIX)) {

            const captureInfo = PlaceholderUtils.parseCapture(node.expression.simpleName);
            if (captureInfo) {
                // Initialize markers on the ExpressionStatement
                if (!node.markers) {
                    node.markers = { kind: 'org.openrewrite.marker.Markers', id: randomId(), markers: [] };
                }
                if (!node.markers.markers) {
                    node.markers.markers = [];
                }

                // Find the original capture object to get variadic options
                const captureObj = this.captures.find(c => c.getName() === captureInfo.name);
                const variadicOptions = captureObj?.getVariadicOptions();

                // Add CaptureMarker to the ExpressionStatement
                node.markers.markers.push(new CaptureMarker(captureInfo.name, variadicOptions));
            }
        }
        // For non-statement captures (expressions), attach marker to the identifier
        else if (node.kind === J.Kind.Identifier && node.simpleName?.startsWith(PlaceholderUtils.CAPTURE_PREFIX)) {
            const captureInfo = PlaceholderUtils.parseCapture(node.simpleName);
            if (captureInfo) {
                // Initialize markers if needed
                if (!node.markers) {
                    node.markers = { kind: 'org.openrewrite.marker.Markers', id: randomId(), markers: [] };
                }
                if (!node.markers.markers) {
                    node.markers.markers = [];
                }

                // Find the original capture object to get variadic options
                const captureObj = this.captures.find(c => c.getName() === captureInfo.name);
                const variadicOptions = captureObj?.getVariadicOptions();

                // Add CaptureMarker with variadic options if available
                node.markers.markers.push(new CaptureMarker(captureInfo.name, variadicOptions));
            }
        }

        // Recursively visit all properties
        for (const key in node) {
            if (node.hasOwnProperty(key)) {
                const value = node[key];
                if (Array.isArray(value)) {
                    value.forEach(item => this.visitAndAttachMarkers(item, visited));
                } else if (typeof value === 'object' && value !== null) {
                    this.visitAndAttachMarkers(value, visited);
                }
            }
        }
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
