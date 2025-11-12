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
import {J} from '../../java';
import {Any, Capture, DebugLogEntry, DebugOptions, MatchAttemptResult, MatchExplanation, PatternOptions, MatchResult as IMatchResult} from './types';
import {CAPTURE_CAPTURING_SYMBOL, CAPTURE_NAME_SYMBOL, CaptureImpl, RAW_CODE_SYMBOL, RawCode} from './capture';
import {DebugPatternMatchingComparator, PatternMatchingComparator} from './comparator';
import {CaptureMarker, CaptureStorageValue, generateCacheKey, globalAstCache, WRAPPERS_MAP_SYMBOL} from './utils';
import {TemplateEngine} from './engine';


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
    private captures: (Capture | Any<any> | RawCode)[] = [];

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
     * @param value The capture object (Capture, Any, or RawCode) or string name
     * @returns This builder for chaining
     */
    capture(value: Capture | Any<any> | RawCode | string): this {
        // Ensure we have a part for after this capture
        if (this.parts.length === 0) {
            this.parts.push('');
        }
        // Convert string to Capture if needed, or use value as-is for RawCode
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
    private _cachedAstPattern?: J;

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
     * @param captures The captures between the string parts (can be Capture, Any, or RawCode)
     */
    constructor(
        public readonly templateParts: TemplateStringsArray,
        public readonly captures: (Capture | Any<any> | RawCode)[]
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
        // Invalidate cache when configuration changes
        this._cachedAstPattern = undefined;
        return this;
    }

    /**
     * Gets the AST pattern for this pattern, using two-level caching:
     * 1. Instance-level cache (fastest - this pattern instance)
     * 2. Global LRU cache (fast - shared across pattern instances with same code)
     * 3. Compute via TemplateProcessor (slow - parse and process)
     *
     * @returns The cached or newly computed pattern AST
     * @internal
     */
    async getAstPattern(): Promise<J> {
        // Level 1: Instance cache (fastest path)
        if (this._cachedAstPattern) {
            return this._cachedAstPattern;
        }

        // Generate cache key for global lookup
        // Include raw code values in the key since they affect the generated AST
        const contextStatements = this._options.context || this._options.imports || [];
        const capturesKey = this.captures.map(c => {
            if (c instanceof RawCode || (c && typeof c === 'object' && (c as any)[RAW_CODE_SYMBOL])) {
                return `raw:${(c as RawCode).code}`;
            }
            return c.getName();
        }).join(',');
        const cacheKey = generateCacheKey(
            this.templateParts,
            capturesKey,
            contextStatements,
            this._options.dependencies || {}
        );

        // Level 2: Global cache (fast path - shared with Template)
        const cached = globalAstCache.get(cacheKey);
        if (cached) {
            this._cachedAstPattern = cached;
            return cached;
        }

        // Level 3: Compute via TemplateEngine (slow path)
        const result = await TemplateEngine.getPatternTree(
            this.templateParts,
            this.captures,
            contextStatements,
            this._options.dependencies || {}
        );

        // Cache in both levels
        globalAstCache.set(cacheKey, result);
        this._cachedAstPattern = result;

        return result;
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

    /**
     * Matches a pattern against an AST node with detailed debug information.
     * Part of Layer 2 (Public API).
     *
     * This method always enables debug logging and returns detailed information about
     * the match attempt, including:
     * - Whether the pattern matched
     * - Captured nodes (if matched)
     * - Explanation of failure (if not matched)
     * - Debug log entries showing the matching process
     *
     * @param ast The AST node to match against
     * @param cursor Optional cursor at the node's position in a larger tree
     * @param debugOptions Optional debug options (defaults to all logging enabled)
     * @returns Detailed result with debug information
     *
     * @example
     * const x = capture('x');
     * const pat = pattern`console.log(${x})`;
     * const attempt = await pat.matchWithExplanation(node);
     * if (attempt.matched) {
     *     console.log('Matched!');
     *     console.log('Captured x:', attempt.result.get('x'));
     * } else {
     *     console.log('Failed:', attempt.explanation);
     *     console.log('Debug log:', attempt.debugLog);
     * }
     */
    async matchWithExplanation(
        ast: J,
        cursor?: Cursor,
        debugOptions?: DebugOptions
    ): Promise<MatchAttemptResult> {
        // Default to full debug logging if not specified
        const options: DebugOptions = {
            enabled: true,
            logComparison: true,
            logConstraints: true,
            ...debugOptions
        };

        const matcher = new Matcher(this, ast, cursor, options);
        const success = await matcher.matches();

        if (success) {
            // Match succeeded - return MatchResult with debug info
            const storage = (matcher as any).storage;
            const matchResult = new MatchResult(new Map(storage));
            return {
                matched: true,
                result: matchResult,
                debugLog: matcher.getDebugLog()
            };
        } else {
            // Match failed - return explanation
            return {
                matched: false,
                explanation: matcher.getExplanation(),
                debugLog: matcher.getDebugLog()
            };
        }
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
export class MatchResult implements IMatchResult {
    constructor(
        private readonly storage: Map<string, CaptureStorageValue> = new Map()
    ) {
    }

    // Overload: get with Capture returns value
    get<T>(capture: Capture<T>): T | undefined;
    // Overload: get with string returns value
    get(capture: string): any;
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

    // Debug tracking (Layer 1: Core Instrumentation)
    private readonly debugOptions: DebugOptions;
    private readonly debugLog: DebugLogEntry[] = [];
    private explanation?: MatchExplanation;
    private readonly currentPath: string[] = [];

    /**
     * Creates a new matcher for a pattern against an AST node.
     *
     * @param pattern The pattern to match
     * @param ast The AST node to match against
     * @param cursor Optional cursor at the AST node's position
     * @param debugOptions Optional debug options for instrumentation
     */
    constructor(
        private readonly pattern: Pattern,
        private readonly ast: J,
        cursor?: Cursor,
        debugOptions?: DebugOptions
    ) {
        // If no cursor provided, create one at the ast root so constraints can navigate up
        this.cursor = cursor ?? new Cursor(ast, undefined);
        this.debugOptions = debugOptions ?? {};
    }

    private readonly cursor: Cursor;

    /**
     * Checks if the pattern matches the AST node.
     *
     * @returns true if the pattern matches, false otherwise
     */
    async matches(): Promise<boolean> {
        if (!this.patternAst) {
            this.patternAst = await this.pattern.getAstPattern();
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
     * Logs a debug message if debugging is enabled.
     * Part of Layer 1 (Core Instrumentation).
     *
     * @param level The severity level
     * @param scope The scope/category
     * @param message The message to log
     * @param data Optional data to include
     */
    private log(
        level: DebugLogEntry['level'],
        scope: DebugLogEntry['scope'],
        message: string,
        data?: any
    ): void {
        if (!this.debugOptions.enabled) return;

        // Filter by scope if specific logging is requested
        if (scope === 'comparison' && !this.debugOptions.logComparison) return;
        if (scope === 'constraint' && !this.debugOptions.logConstraints) return;

        this.debugLog.push({
            level,
            scope,
            path: [...this.currentPath],
            message,
            data
        });
    }

    /**
     * Sets the explanation for why the pattern match failed.
     * Only sets the first failure (most relevant).
     * Part of Layer 1 (Core Instrumentation).
     *
     * @param reason The reason for failure
     * @param expected Human-readable description of what was expected
     * @param actual Human-readable description of what was found
     * @param details Optional additional context
     */
    private setExplanation(
        reason: MatchExplanation['reason'],
        expected: string,
        actual: string,
        details?: string
    ): void {
        // Only set the first failure (most relevant)
        if (this.explanation) return;

        this.explanation = {
            reason,
            path: [...this.currentPath],
            expected,
            actual,
            details
        };
    }

    /**
     * Pushes a path component onto the current path.
     * Used to track where in the AST tree we are during matching.
     * Part of Layer 1 (Core Instrumentation).
     *
     * @param name The path component to push
     */
    private pushPath(name: string): void {
        this.currentPath.push(name);
    }

    /**
     * Pops the last path component from the current path.
     * Part of Layer 1 (Core Instrumentation).
     */
    private popPath(): void {
        this.currentPath.pop();
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

        // Factory pattern: instantiate debug or production comparator
        // Zero cost in production - DebugPatternMatchingComparator is never instantiated
        const matcherCallbacks = {
            handleCapture: (capture: CaptureMarker, t: J, w?: J.RightPadded<J>) => this.handleCapture(capture, t, w),
            handleVariadicCapture: (capture: CaptureMarker, ts: J[], ws?: J.RightPadded<J>[]) => this.handleVariadicCapture(capture, ts, ws),
            saveState: () => this.saveState(),
            restoreState: (state: Map<string, CaptureStorageValue>) => this.restoreState(state),
            // Debug callbacks (Layer 1) - grouped together, always present or absent
            debug: this.debugOptions.enabled ? {
                log: (level: DebugLogEntry['level'], scope: DebugLogEntry['scope'], message: string, data?: any) => this.log(level, scope, message, data),
                setExplanation: (reason: MatchExplanation['reason'], expected: string, actual: string, details?: string) => this.setExplanation(reason, expected, actual, details),
                pushPath: (name: string) => this.pushPath(name),
                popPath: () => this.popPath()
            } : undefined
        };

        const comparator = this.debugOptions.enabled
            ? new DebugPatternMatchingComparator(matcherCallbacks, lenientTypeMatching)
            : new PatternMatchingComparator(matcherCallbacks, lenientTypeMatching);
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
        // Filter out RawCode since it doesn't have getName()
        const captureObj = this.pattern.captures.find(c =>
            !(c instanceof RawCode || (c && typeof c === 'object' && (c as any)[RAW_CODE_SYMBOL])) &&
            c.getName() === captureName
        );

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
        // Filter out RawCode since it doesn't have getName()
        const captureObj = this.pattern.captures.find(c =>
            !(c instanceof RawCode || (c && typeof c === 'object' && (c as any)[RAW_CODE_SYMBOL])) &&
            c.getName() === captureName
        );

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

    /**
     * Gets the debug log entries collected during matching.
     * Part of Layer 2 (Public API).
     *
     * @returns The debug log entries, or undefined if debug wasn't enabled
     */
    getDebugLog(): DebugLogEntry[] | undefined {
        return this.debugOptions.enabled ? [...this.debugLog] : undefined;
    }

    /**
     * Gets the explanation for why the match failed.
     * Part of Layer 2 (Public API).
     *
     * @returns The match explanation, or undefined if match succeeded or no explanation available
     */
    getExplanation(): MatchExplanation | undefined {
        return this.explanation;
    }
}

/**
 * Tagged template function for creating patterns.
 *
 * @param strings The string parts of the template
 * @param captures The captures between the string parts (Capture, Any, RawCode, or string names)
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
 *
 * @example
 * // Using raw() for dynamic pattern construction
 * const operator = '===';
 * const pat = pattern`x ${raw(operator)} y`;
 */
export function pattern(strings: TemplateStringsArray, ...captures: (Capture | Any<any> | RawCode | string)[]): Pattern {
    const capturesByName = captures.reduce((map, c) => {
        // Skip raw code - it's not a capture
        if (c instanceof RawCode || (typeof c === 'object' && c && (c as any)[RAW_CODE_SYMBOL])) {
            return map;
        }
        const capture = typeof c === "string" ? new CaptureImpl(c) : c;
        // Use symbol to get internal name without triggering Proxy
        const name = (capture as any)[CAPTURE_NAME_SYMBOL] || capture.getName();
        return map.set(name, capture);
    }, new Map<string, Capture | Any<any>>());
    return new Pattern(strings, captures.map(c => {
        // Return raw code as-is
        if (c instanceof RawCode || (typeof c === 'object' && c && (c as any)[RAW_CODE_SYMBOL])) {
            return c as RawCode;
        }
        // Use symbol to get internal name without triggering Proxy
        const name = typeof c === "string" ? c : ((c as any)[CAPTURE_NAME_SYMBOL] || c.getName());
        return capturesByName.get(name)!;
    }));
}
