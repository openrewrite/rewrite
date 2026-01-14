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
import {JTree} from '../comparator';
import {
    Any,
    Capture,
    DebugLogEntry,
    DebugOptions,
    MatchAttemptResult,
    MatchExplanation,
    MatchOptions,
    MatchResult as IMatchResult,
    PatternOptions
} from './types';
import {CAPTURE_CAPTURING_SYMBOL, CAPTURE_NAME_SYMBOL, CaptureImpl, RAW_CODE_SYMBOL, RawCode} from './capture';
import {DebugPatternMatchingComparator, MatcherCallbacks, MatcherState, PatternMatchingComparator} from './comparator';
import {CaptureMarker, CaptureStorageValue, generateCacheKey, globalAstCache, WRAPPERS_MAP_SYMBOL} from './utils';
import {TemplateEngine} from './engine';
import {TreePrinters} from '../../print';
import {JS} from '../index';
import {AnsiAwarePrintOutputCapture, ElementMarkerVisitor, PATTERN_DEBUG_MARKER_PRINTER, shouldUseEmoji} from './debug';
import {dedent, prefixLines} from '../../util';


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
    private static nextPatternId = 1;
    private readonly patternId: number;
    private readonly unnamedCaptureMapping = new Map<string, string>();

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
        this.patternId = Pattern.nextPatternId++;

        // Build mapping for unnamed captures (unnamed_N -> X, which will be displayed as 🪝X)
        let unnamedIndex = 1;
        for (const cap of captures) {
            if (cap && typeof cap === 'object' && 'getName' in cap) {
                const name = (cap as Capture<any> | Any<any>).getName();
                if (name && name.startsWith('unnamed_')) {
                    this.unnamedCaptureMapping.set(name, `${unnamedIndex}`);
                    unnamedIndex++;
                }
            }
        }
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
     * @param tree The AST node to match against
     * @param cursor Cursor at the node's position in a larger tree. Used for context-aware
     *               capture constraints to navigate to parent nodes.
     * @param options Optional match options (e.g., debug flag)
     * @returns A MatchResult if the pattern matches, undefined otherwise
     *
     * @example
     * ```typescript
     * // Normal match
     * const match = await pattern.match(node, cursor);
     *
     * // Debug this specific call
     * const match = await pattern.match(node, cursor, { debug: true });
     * ```
     */
    async match(tree: J, cursor: Cursor, options?: MatchOptions): Promise<MatchResult | undefined> {
        // Three-level precedence: call > pattern > global
        const debugEnabled =
            options?.debug !== undefined
                ? options.debug  // 1. Explicit call-level (true OR false)
                : (this._options.debug !== undefined
                    ? this._options.debug  // 2. Explicit pattern-level
                    : process.env.PATTERN_DEBUG === 'true');  // 3. Global

        if (debugEnabled) {
            // Use matchWithExplanation and log the result
            const result = await this.matchWithExplanation(tree, cursor);
            await this.logMatchResult(tree, cursor, result);

            if (result.matched) {
                // result.result is the MatchResult class instance
                return result.result as MatchResult | undefined;
            } else {
                return undefined;
            }
        }

        // Fast path - no debug
        const matcher = new Matcher(this, tree, cursor);
        const success = await matcher.matches();
        if (!success) {
            return undefined;
        }
        // Create MatchResult with unified storage
        const storage = (matcher as any).storage;
        return new MatchResult(new Map(storage));
    }

    /**
     * Formats and logs the match result to stderr.
     * @private
     */
    private async logMatchResult(tree: J, cursor: Cursor | undefined, result: MatchAttemptResult): Promise<void> {
        const patternSource = this.getPatternSource();
        const patternId = `Pattern #${this.patternId}`;
        const nodeKind = (tree as any).kind || 'unknown';
        // Format kind: extract short name (e.g., "org.openrewrite.java.tree.J$Binary" -> "J$Binary")
        const shortKind = nodeKind.split('.').pop() || nodeKind;

        // First, log the pattern source
        console.error(`[${patternId}] ${patternSource}`);

        // Build the complete match result message
        const lines: string[] = [];

        if (result.matched) {
            // Success case - result first, then tree with highlighted captures, then capture legend
            lines.push(`[${patternId}] ✅ SUCCESS matching against ${shortKind}:`);

            // Mark captured elements with numbered markers
            let markedAst = tree;
            const captureNumbers = new Map<number, JTree | JTree[]>();

            if (result.result) {
                const storage = (result.result as any).storage as Map<string, CaptureStorageValue>;
                if (storage && storage.size > 0) {
                    // Build map of capture number -> captured element(s)
                    // Limit to 11 captures (0-10) for emoji display
                    let captureIndex = 0;
                    for (const [_, value] of storage) {
                        if (captureIndex <= 10) {
                            const extractedValue = (result.result as any).extractElements(value);
                            captureNumbers.set(captureIndex + 1, extractedValue); // Start from 1
                            captureIndex++;
                        }
                    }

                    // Mark the tree with capture numbers
                    if (captureNumbers.size > 0) {
                        try {
                            const visitor = new ElementMarkerVisitor(captureNumbers);
                            markedAst = await visitor.visit(tree, undefined) as J;
                        } catch (markError) {
                            console.warn(`Failed to mark captures: ${markError}`);
                        }
                    }
                }
            }

            // Print the marked tree
            let treeStr: string;
            try {
                const printer = TreePrinters.printer(JS.Kind.CompilationUnit);
                const output = new AnsiAwarePrintOutputCapture(PATTERN_DEBUG_MARKER_PRINTER);
                treeStr = await printer.print(markedAst, output);
                // Replace placeholder names with clean display names
                treeStr = this.replacePlaceholderNames(treeStr);
            } catch (e) {
                treeStr = '(tree printing unavailable)';
            }
            treeStr.split('\n').forEach(line => lines.push(`[${patternId}]   ${line}`));

            // Print capture legend
            if (result.result) {
                const storage = (result.result as any).storage as Map<string, CaptureStorageValue>;
                if (storage && storage.size > 0) {
                    lines.push(`[${patternId}]`);
                    lines.push(`[${patternId}]   Captures:`);

                    let captureIndex = 0;
                    for (const [name, _value] of storage) {
                        if (captureIndex <= 10) {
                            const displayName = this.unnamedCaptureMapping.get(name) || name;
                            const emoji = ['0️⃣', '1️⃣', '2️⃣', '3️⃣', '4️⃣', '5️⃣', '6️⃣', '7️⃣', '8️⃣', '9️⃣', '🔟'][captureIndex + 1];
                            lines.push(`[${patternId}]     ${emoji} = 🪝${displayName}`);
                            captureIndex++;
                        }
                    }
                }
            }
        } else {
            // Failure case - show both pattern tree and matched tree with markers
            lines.push(`[${patternId}] ❌ FAILED matching against ${shortKind}:`);

            // Add explanation of marker format (only for emoji - ANSI inverse video is self-explanatory)
            if (shouldUseEmoji()) {
                lines.push(`[${patternId}]    (Mismatched elements are marked with 👉...👈)`);
            }
            lines.push(`[${patternId}]`);

            // Get both cursors and explanation from the result
            const patternCursor = result.patternCursor;
            const targetCursor = result.targetCursor || cursor;
            const explanation = result.explanation;

            // Print pattern tree with marker - use the actual pattern AST from matching
            const patternAst = result.patternAst || await this.getAstPattern();
            let patternTreeStr: string;
            try {
                let markedPattern: J | undefined;
                // Use explanation's patternElement if available, otherwise fall back to cursor
                if (explanation?.patternElement || (patternCursor && (patternCursor.value as any)?.id)) {
                    try {
                        markedPattern = await this.markElementWithSearchResult(patternAst, patternCursor, explanation, true);
                    } catch (markError) {
                        // If marking fails, continue without the marker
                        console.warn(`Failed to mark pattern tree: ${markError}`);
                    }
                }
                const patternToPrint = markedPattern || patternAst;
                const printer = TreePrinters.printer(JS.Kind.CompilationUnit);
                const output = new AnsiAwarePrintOutputCapture(PATTERN_DEBUG_MARKER_PRINTER);
                patternTreeStr = await printer.print(patternToPrint, output);
                // Replace placeholder names with clean display names
                patternTreeStr = this.replacePlaceholderNames(patternTreeStr);
            } catch (e) {
                patternTreeStr = `(tree printing unavailable: ${e})`;
            }

            const useAnsi = !shouldUseEmoji();
            const greenColor = useAnsi ? '\x1b[32m' : '';
            const redColor = useAnsi ? '\x1b[31m' : '';
            const resetColor = useAnsi ? '\x1b[0m' : '';

            lines.push(`${greenColor}[${patternId}]    Pattern tree:${resetColor}`);
            patternTreeStr.split('\n').forEach(line => lines.push(`${greenColor}[${patternId}]       ${line}${resetColor}`));

            // Print matched tree with marker
            let matchedTreeStr: string;
            try {
                let markedTree: J | undefined;
                // Use explanation's targetElement if available, otherwise fall back to cursor
                if (explanation?.targetElement || (targetCursor && (targetCursor.value as any)?.id)) {
                    try {
                        markedTree = await this.markElementWithSearchResult(tree, targetCursor, explanation, false);
                    } catch (markError) {
                        // If marking fails, continue without the marker
                        console.warn(`Failed to mark matched tree: ${markError}`);
                    }
                }
                const treeToPrint = markedTree || tree;
                const printer = TreePrinters.printer(JS.Kind.CompilationUnit);
                const output = new AnsiAwarePrintOutputCapture(PATTERN_DEBUG_MARKER_PRINTER);
                matchedTreeStr = await printer.print(treeToPrint, output);
                // Replace placeholder names with clean display names
                matchedTreeStr = this.replacePlaceholderNames(matchedTreeStr);
            } catch (e) {
                matchedTreeStr = `(tree printing unavailable: ${e})`;
            }

            const sourcePath = this.getSourcePathFromCursor(targetCursor, tree);
            const sourcePathStr = sourcePath ? ` (${sourcePath})` : '';
            lines.push(`[${patternId}]`);
            lines.push(`${redColor}[${patternId}]    Matched tree${sourcePathStr}:${resetColor}`);
            matchedTreeStr.split('\n').forEach(line => lines.push(`${redColor}[${patternId}]       ${line}${resetColor}`));

            // Print detailed mismatch information
            if (explanation) {
                lines.push(`[${patternId}]`);
                lines.push(`[${patternId}]    Details:`);
                lines.push(`[${patternId}]      Reason:   ${explanation.reason}`);
                lines.push(`[${patternId}]      Expected: ${explanation.expected}`);
                lines.push(`[${patternId}]      Actual:   ${explanation.actual}`);

                // Print mismatched subtrees if available
                if (explanation.patternElement || explanation.targetElement) {
                    // Helper to print an element (handles both regular elements and Containers)
                    const printElement = async (element: any, label: string): Promise<void> => {
                        try {
                            const printer = TreePrinters.printer(JS.Kind.CompilationUnit);

                            // Check if this is a Container - if so, print its elements
                            const kind = (element as any)?.kind;
                            if (kind === 'org.openrewrite.java.tree.JContainer') {
                                const container = element as J.Container<J>;
                                if (container.elements && container.elements.length > 0) {
                                    const elementStrs = await Promise.all(
                                        container.elements.map(async (padded) => {
                                            // Create fresh output capture for each element
                                            const output = new AnsiAwarePrintOutputCapture(PATTERN_DEBUG_MARKER_PRINTER);
                                            const subtree = await printer.print(padded.element, output);
                                            return subtree.trim();
                                        })
                                    );
                                    let subtreeStr = elementStrs.join(', ');
                                    subtreeStr = this.replacePlaceholderNames(subtreeStr);
                                    lines.push(`[${patternId}]      ${label}:  (${subtreeStr})`);
                                } else {
                                    lines.push(`[${patternId}]      ${label}:  ()`);
                                }
                            } else {
                                // Regular element
                                const output = new AnsiAwarePrintOutputCapture(PATTERN_DEBUG_MARKER_PRINTER);
                                let subtreeStr = await printer.print(element, output);
                                subtreeStr = this.replacePlaceholderNames(subtreeStr);

                                // Process multi-line output: trim, dedent, then re-indent with proper alignment
                                subtreeStr = dedent(subtreeStr.trim());

                                // Calculate proper indentation to align continuation lines
                                // Pattern is: "[Pattern #2]      Label:  content"
                                // So continuation lines should start at position of "content"
                                const patternPrefix = `[${patternId}]      `;
                                const labelPrefix = `${label}:  `;
                                const firstLinePrefix = patternPrefix + labelPrefix;
                                const continuationPrefix = patternPrefix + ' '.repeat(labelPrefix.length);

                                // Apply prefixes using prefixLines utility
                                const prefixedStr = prefixLines(subtreeStr, continuationPrefix, firstLinePrefix);
                                lines.push(prefixedStr);
                            }
                        } catch (e) {
                            lines.push(`[${patternId}]      ${label}:  (unable to print: ${e})`);
                        }
                    };

                    // Print pattern subtree
                    if (explanation.patternElement) {
                        await printElement(explanation.patternElement, 'Pattern');
                    }

                    // Print target subtree
                    if (explanation.targetElement) {
                        await printElement(explanation.targetElement, 'Matched');
                    }
                }
            }
        }

        // Single console.error call with all lines joined
        console.error(lines.join('\n'));
    }

    /**
     * Walks up the cursor to find the source file and extract its source path.
     * @private
     */
    private getSourcePathFromCursor(cursor: Cursor | undefined, ast?: J): string | undefined {
        // First check if the ast node itself is a SourceFile
        if (ast && typeof ast === 'object' && 'sourcePath' in ast) {
            return (ast as any).sourcePath;
        }
        // Then try walking up the cursor
        if (!cursor) return undefined;
        let current: Cursor | undefined = cursor;
        while (current) {
            const value = current.value;
            if (value && typeof value === 'object' && 'sourcePath' in value) {
                return (value as any).sourcePath;
            }
            current = current.parent;
        }
        return undefined;
    }

    /**
     * Replaces placeholder names in tree strings with clean display names.
     * Uses 🪝 prefix to make captures easily identifiable.
     * @private
     */
    private replacePlaceholderNames(treeStr: string): string {
        let result = treeStr;
        // Replace unnamed captures with their short forms (🪝1, 🪝2, etc.)
        for (const [originalName, displayName] of this.unnamedCaptureMapping.entries()) {
            const placeholder = `__capt_${originalName}__`;
            result = result.split(placeholder).join(`🪝${displayName}`);
        }
        // Replace all remaining __capt_XXX__ with 🪝XXX (for named captures)
        result = result.replace(/__capt_([^_]+(?:_[^_]+)*)__/g, '🪝$1');
        return result;
    }

    /**
     * Marks the element at the current cursor position with a SearchResult marker (🟡).
     * If an explanation with pattern/target elements is provided, marks those specific elements.
     * @param ast The AST tree to mark
     * @param cursor The cursor position (used as fallback if no explanation element)
     * @param explanation The match explanation containing the actual elements to mark
     * @param isPattern Whether this is the pattern tree (true) or target tree (false)
     * @private
     */
    private async markElementWithSearchResult(ast: J, cursor: Cursor | undefined, explanation?: MatchExplanation, isPattern: boolean = false): Promise<J> {
        // If we have an explanation, use the specific element from it
        if (explanation) {
            const element = isPattern ? explanation.patternElement : explanation.targetElement;
            if (element) {
                const visitor = new ElementMarkerVisitor(element);
                return await visitor.visit(ast, undefined) as J;
            }
        }

        // Fallback: mark the element at the cursor (if cursor exists)
        if (!cursor) return ast;
        const element = cursor.value;
        if (!element) return ast;

        const visitor = new ElementMarkerVisitor(element as any);
        return await visitor.visit(ast, undefined) as J;
    }

    /**
     * Gets the source code representation of this pattern for logging.
     * @private
     */
    private getPatternSource(): string {
        // Reconstruct pattern source from template parts
        let source = '';
        for (let i = 0; i < this.templateParts.length; i++) {
            source += this.templateParts[i];
            if (i < this.captures.length) {
                const cap = this.captures[i];
                // Skip raw code
                if (cap instanceof RawCode || (cap && typeof cap === 'object' && (cap as any)[RAW_CODE_SYMBOL])) {
                    source += '${raw(...)}';
                    continue;
                }
                // Show capture name or placeholder
                const name = (cap as any)[CAPTURE_NAME_SYMBOL];
                if (cap && typeof cap === 'object' && name) {
                    // Use mapped name for unnamed captures, or original name (with 🪝 prefix)
                    const displayName = this.unnamedCaptureMapping.get(name) || name;
                    source += `\${🪝${displayName}}`;
                } else {
                    source += '${...}';
                }
            }
        }

        return source;
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
     * @param tree The AST node to match against
     * @param cursor Cursor at the node's position in a larger tree
     * @param debugOptions Optional debug options (defaults to all logging enabled)
     * @returns Detailed result with debug information
     *
     * @example
     * const x = capture('x');
     * const pat = pattern`console.log(${x})`;
     * const attempt = await pat.matchWithExplanation(node, cursor);
     * if (attempt.matched) {
     *     console.log('Matched!');
     *     console.log('Captured x:', attempt.result.get('x'));
     * } else {
     *     console.log('Failed:', attempt.explanation);
     *     console.log('Debug log:', attempt.debugLog);
     * }
     */
    async matchWithExplanation(
        tree: J,
        cursor: Cursor,
        debugOptions?: DebugOptions
    ): Promise<MatchAttemptResult> {
        // Default to full debug logging if not specified
        const options: DebugOptions = {
            enabled: true,
            logComparison: true,
            logConstraints: true,
            ...debugOptions
        };

        const matcher = new Matcher(this, tree, cursor, options);
        const success = await matcher.matches();

        if (success) {
            // Match succeeded - return MatchResult with debug info
            const storage = (matcher as any).storage;
            const matchResult = new MatchResult(new Map(storage));
            return {
                matched: true,
                result: matchResult,
                debugLog: matcher.getDebugLog(),
                patternCursor: matcher.getPatternCursor(),
                targetCursor: matcher.getTargetCursor(),
                patternAst: matcher.getPatternAst()
            };
        } else {
            // Match failed - return explanation
            return {
                matched: false,
                explanation: matcher.getExplanation(),
                debugLog: matcher.getDebugLog(),
                patternCursor: matcher.getPatternCursor(),
                targetCursor: matcher.getTargetCursor(),
                patternAst: matcher.getPatternAst()
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
     * Checks if a capture has been matched.
     *
     * @param capture The capture name (string) or Capture object
     * @returns true if the capture exists in the match result
     */
    has(capture: Capture | string): boolean {
        const name = typeof capture === "string" ? capture : ((capture as any)[CAPTURE_NAME_SYMBOL] || capture.getName());
        return this.storage.has(name);
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
    private lastComparator?: PatternMatchingComparator | DebugPatternMatchingComparator;

    /**
     * Creates a new matcher for a pattern against an AST node.
     *
     * @param pattern The pattern to match
     * @param ast The AST node to match against
     * @param cursor Cursor at the AST node's position
     * @param debugOptions Optional debug options for instrumentation
     */
    constructor(
        private readonly pattern: Pattern,
        private readonly ast: J,
        cursor: Cursor,
        debugOptions?: DebugOptions
    ) {
        this.cursor = cursor;
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
     * @param patternElement The actual pattern element that failed to match
     * @param targetElement The actual target element that failed to match
     */
    private setExplanation(
        reason: MatchExplanation['reason'],
        expected: string,
        actual: string,
        details?: string,
        patternElement?: any,
        targetElement?: any
    ): void {
        // Only set the first failure (most relevant)
        if (this.explanation) return;

        this.explanation = {
            reason,
            expected,
            actual,
            details,
            patternElement,
            targetElement
        };
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
        const matcherCallbacks: MatcherCallbacks = {
            handleCapture: (capture: CaptureMarker, t: J, w?: J.RightPadded<J>) => this.handleCapture(capture, t, w),
            handleVariadicCapture: (capture: CaptureMarker, ts: J[], ws?: J.RightPadded<J>[]) => this.handleVariadicCapture(capture, ts, ws),
            saveState: () => this.saveState(),
            restoreState: (state) => this.restoreState(state),
            // Debug callbacks (Layer 1) - grouped together, always present or absent
            debug: this.debugOptions.enabled ? {
                log: (level: DebugLogEntry['level'], scope: DebugLogEntry['scope'], message: string, data?: any) => this.log(level, scope, message, data),
                setExplanation: (reason: MatchExplanation['reason'], expected: string, actual: string, details?: string, patternElement?: any, targetElement?: any) => this.setExplanation(reason, expected, actual, details, patternElement, targetElement),
                getExplanation: () => this.explanation,
                restoreExplanation: (explanation: MatchExplanation) => { this.explanation = explanation; },
                clearExplanation: () => { this.explanation = undefined; }
            } : undefined
        };

        this.lastComparator = this.debugOptions.enabled
            ? new DebugPatternMatchingComparator(matcherCallbacks, lenientTypeMatching)
            : new PatternMatchingComparator(matcherCallbacks, lenientTypeMatching);
        // Pass cursors to allow constraints to navigate to root
        // Pattern cursor is undefined (pattern is the root), target cursor is provided by user
        const result = await this.lastComparator!.compare(pattern, target, undefined, this.cursor);

        // If match failed and no explanation was set, provide a generic one
        if (!result && this.debugOptions.enabled && !this.explanation) {
            const patternKind = (pattern as any).kind?.split('.').pop() || 'unknown';
            const targetKind = (target as any).kind?.split('.').pop() || 'unknown';
            this.setExplanation(
                'structural-mismatch',
                `Pattern node of type ${patternKind}`,
                `Target node of type ${targetKind}`,
                'Nodes did not match structurally'
            );
        }

        return result;
    }

    /**
     * Saves the current state for backtracking.
     * Includes both capture storage AND debug state (explanation, log).
     *
     * @returns A snapshot of the current state
     */
    private saveState(): MatcherState {
        return {
            storage: new Map(this.storage),
            debugState: this.debugOptions.enabled ? {
                explanation: this.explanation,
                logLength: this.debugLog.length
            } : undefined
        };
    }

    /**
     * Restores a previously saved state for backtracking.
     * Restores both capture storage AND debug state.
     *
     * @param state The state to restore
     */
    private restoreState(state: MatcherState): void {
        // Restore capture storage
        this.storage.clear();
        state.storage.forEach((value, key) => this.storage.set(key, value));

        // Restore debug state if it was saved
        if (state.debugState) {
            // Restore explanation to the saved state
            // This clears any explanations set during failed exploratory attempts (like pivot detection)
            this.explanation = state.debugState.explanation;
            // Truncate debug log to saved length (remove entries added during failed attempt)
            this.debugLog.length = state.debugState.logLength;
        }
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

    /**
     * Gets the pattern cursor from the comparator (for debug visualization).
     * @internal
     */
    getPatternCursor(): Cursor | undefined {
        return (this.lastComparator as any)?.cursor;
    }

    /**
     * Gets the target cursor from the comparator (for debug visualization).
     * @internal
     */
    getTargetCursor(): Cursor | undefined {
        return (this.lastComparator as any)?.targetCursor;
    }

    /**
     * Gets the pattern AST that was used during matching (for debug visualization).
     * @internal
     */
    getPatternAst(): J | undefined {
        return this.patternAst;
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
/**
 * Creates a pattern from a template literal (direct usage).
 *
 * @example
 * ```typescript
 * const pat = pattern`console.log(${x})`;
 * ```
 */
export function pattern(strings: TemplateStringsArray, ...captures: (Capture | Any<any> | RawCode | string)[]): Pattern;

/**
 * Creates a pattern factory with options that returns a tagged template function.
 *
 * @example
 * ```typescript
 * const pat = pattern({ debug: true })`console.log(${x})`;
 * ```
 */
export function pattern(options: PatternOptions): (strings: TemplateStringsArray, ...captures: (Capture | Any<any> | RawCode | string)[]) => Pattern;

// Implementation
export function pattern(
    stringsOrOptions: TemplateStringsArray | PatternOptions,
    ...captures: (Capture | Any<any> | RawCode | string)[]
): Pattern | ((strings: TemplateStringsArray, ...captures: (Capture | Any<any> | RawCode | string)[]) => Pattern) {
    // Check if first arg is TemplateStringsArray (direct usage)
    if (Array.isArray(stringsOrOptions) && 'raw' in stringsOrOptions) {
        // Direct usage: pattern`...`
        return createPattern(stringsOrOptions as TemplateStringsArray, captures, {});
    }

    // Options usage: pattern({ ... })`...`
    const options = stringsOrOptions as PatternOptions;
    return (strings: TemplateStringsArray, ...caps: (Capture | Any<any> | RawCode | string)[]): Pattern => {
        return createPattern(strings, caps, options);
    };
}

/**
 * Internal helper to create a Pattern instance.
 * @private
 */
function createPattern(
    strings: TemplateStringsArray,
    captures: (Capture | Any<any> | RawCode | string)[],
    options: PatternOptions
): Pattern {
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

    const pat = new Pattern(strings, captures.map(c => {
        // Return raw code as-is
        if (c instanceof RawCode || (typeof c === 'object' && c && (c as any)[RAW_CODE_SYMBOL])) {
            return c as RawCode;
        }
        // Use symbol to get internal name without triggering Proxy
        const name = typeof c === "string" ? c : ((c as any)[CAPTURE_NAME_SYMBOL] || c.getName());
        return capturesByName.get(name)!;
    }));

    // Apply options if provided
    if (options && Object.keys(options).length > 0) {
        pat.configure(options);
    }

    return pat;
}
