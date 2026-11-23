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
import {isIdentifier, J} from '../../java';
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
import {
    Focus,
    MarkFocus,
    MatcherCallbacks,
    MatcherState,
    PatternMatchingComparator,
    PendingExplanation
} from './comparator';
import {
    CaptureMarker,
    CaptureStorageValue,
    generateCacheKey,
    globalAstCache,
    PATTERN_PREFIXES_SYMBOL,
    PlaceholderUtils,
    WRAPPERS_MAP_SYMBOL
} from './utils';
import {walk} from '../scope';
import {opensWithBrace, TemplateEngine} from './engine';
import {TreePrinters} from '../../print';
import {JS} from '../index';
import {
    AnsiAwarePrintOutputCapture,
    callSite,
    CaptureHighlightMarker,
    DebugPatternMatchingComparator,
    aroundMarks,
    describe,
    describeKind,
    ElementMarkerVisitor,
    mismatchDelimiters,
    narrowMark,
    PATTERN_DEBUG_MARKER_PRINTER,
    shouldUseEmoji,
    sourceLocation
} from './debug';
import {dedent} from '../../util';
import {JavaScriptSemanticComparatorVisitor, JTree} from '../comparator';


/**
 * Printed code without its leading blank lines and trailing whitespace, and without the indentation
 * all its lines share. Its first line counts as starting at `column`, where the code starts in its
 * file, so that the lines after it keep their indentation relative to it.
 */
function layOut(printed: string, column?: number): string {
    const text = printed.replace(/^\s*\n/, '');
    const indent = text.length - text.trimStart().length;
    const padded = column && column - 1 > indent ? ' '.repeat(column - 1 - indent) + text : text;
    return dedent(padded).trimEnd().replace(/^[ \t]+/, '');
}

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
 * Represents a pattern that can be matched against AST nodes. Where it does not match code you
 * expected it to, {@link Pattern.match} with `{debug: true}` prints why, as {@link PatternOptions.debug} describes.
 */
export class Pattern {
    private _options: PatternOptions = {};
    /** The pattern tree, keyed by whether its code was parsed as an expression. */
    private _cachedAstPatterns = new Map<boolean, J>();
    private static nextPatternId = 1;
    private readonly patternId: number;
    private readonly printedAttempts = new Set<string>();
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
                if (name && name.startsWith('unnamed_') && !this.unnamedCaptureMapping.has(name)) {
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
     *
     * @example
     * // Print why each match attempt of this pattern matched or not
     * pat.configure({debug: true})
     */
    configure(options: PatternOptions): Pattern {
        this._options = {...this._options, ...options};
        // Invalidate cache when configuration changes
        this._cachedAstPatterns.clear();
        return this;
    }

    /**
     * Gets the AST pattern for this pattern, using three-level caching:
     * 1. Instance-level cache (fastest - this pattern instance)
     * 2. Global LRU cache (fast - shared across pattern instances with same code)
     * 3. Compute via TemplateProcessor (slow - parse and process)
     *
     * @param expression Whether to parse the code as an expression, with statements as the fallback
     * @returns The cached or newly computed pattern AST
     * @internal
     */
    async getAstPattern(expression: boolean = false): Promise<J> {
        // Level 1: Instance cache (fastest path)
        const instanceCached = this._cachedAstPatterns.get(expression);
        if (instanceCached) {
            return instanceCached;
        }

        // Generate cache key for global lookup
        // Include raw code values in the key since they affect the generated AST
        const contextStatements = this._options.context || this._options.imports || [];
        const capturesKey = this.captures.map(c => {
            if (c instanceof RawCode || (c && typeof c === 'object' && (c as any)[RAW_CODE_SYMBOL])) {
                return `raw:${(c as RawCode).code}`;
            }
            return c.getName();
        }).join(',') + (expression ? '::expression' : '');
        const cacheKey = generateCacheKey(
            this.templateParts,
            capturesKey,
            // A capture's type reaches the parse as a declaration, so it shapes the tree the same
            // way an explicit context statement does and belongs in the key alongside one.
            [...contextStatements, ...TemplateEngine.capturePreamble(this.captures)],
            this._options.dependencies || {},
            this._options.types
        );

        // Level 2: Global cache (fast path - shared with Template)
        let tree = globalAstCache.get(cacheKey);
        if (!tree) {
            // Level 3: Compute via TemplateEngine (slow path)
            try {
                tree = await TemplateEngine.getPatternTree(
                    this.templateParts,
                    this.captures,
                    contextStatements,
                    this._options.dependencies || {},
                    this._options.types,
                    expression
                );
            } catch (e) {
                // A block matches only a block, which the statement parse is for
                if (!expression) {
                    throw e;
                }
                const statementPattern = await this.getAstPattern(false);
                this._cachedAstPatterns.set(expression, statementPattern);
                return statementPattern;
            }
            globalAstCache.set(cacheKey, tree);
        }

        // The key names captures but says nothing about their constraints, so two patterns of the
        // same shape share an entry; markers are attached per instance to keep them apart.
        const result = await TemplateEngine.attachCaptureMarkers(tree, this.captures);
        this._cachedAstPatterns.set(expression, result);

        return result;
    }

    /**
     * Matches this pattern against `tree`, returning what its captures hold, or `undefined` where it
     * does not match. To see why it does not match, pass `{debug: true}`, or a predicate that picks
     * the attempts to print, as {@link PatternOptions.debug} describes.
     *
     * @param tree The AST node to match against
     * @param cursor Cursor at the node's position in a larger tree. Used for context-aware
     *               capture constraints to navigate to parent nodes.
     * @param options Optional match options, such as `debug`
     * @returns A MatchResult if the pattern matches, undefined otherwise
     *
     * @example
     * ```typescript
     * // Normal match
     * const match = await pattern.match(node, cursor);
     *
     * // Print why this attempt matched or not
     * const match = await pattern.match(node, cursor, {debug: true});
     * ```
     */
    async match(tree: J, cursor: Cursor, options?: MatchOptions): Promise<MatchResult | undefined> {
        // Three-level precedence: call > pattern > global
        const debug = options?.debug ?? this._options.debug ?? process.env.PATTERN_DEBUG === 'true';
        if (debug) {
            // Taken before the first await, while the caller is still on the stack
            const caller = callSite(new Error().stack);
            const {attempt, patternAst, focus} = await this.attempt(tree, cursor);
            if (debug === true || debug(tree, cursor, attempt)) {
                await this.logMatchResult(tree, attempt, patternAst, focus, caller);
            }
            return attempt.matched ? attempt.result as MatchResult : undefined;
        }

        // Fast path - no debug
        const matcher = new Matcher(this, tree, cursor);
        const success = await matcher.matches();
        if (!success) {
            return undefined;
        }
        // Create MatchResult with unified storage
        const storage = (matcher as any).storage;
        return new MatchResult(new Map(storage), () => matcher.patternPrefixes());
    }

    /**
     * Prints a match attempt to stderr, followed by a blank line. An attempt that prints the same as
     * an earlier one prints only its heading.
     */
    private async logMatchResult(tree: J, attempt: MatchAttemptResult, patternAst: J, focus: Focus | undefined,
                                 caller: string | undefined): Promise<void> {
        const [source, heading, ...block] = await this.render(tree, attempt, patternAst, focus, caller, true);
        const key = [heading, ...block.filter(line => !line.includes('called from'))].join('\n');
        if (this.printedAttempts.has(key)) {
            console.error(`${heading}, as before\n`);
            return;
        }
        this.printedAttempts.add(key);
        console.error(source);
        console.error([heading, ...block].join('\n') + '\n');
    }

    /**
     * The lines debug output prints for a match attempt. They start with the pattern and a heading that
     * says where the code is. A success follows with the code and each capture lettered. A failure follows
     * with the pattern and the code around the mismatch marked, and how they differ.
     */
    private async render(tree: J, attempt: MatchAttemptResult, patternAst: J, focus: Focus | undefined,
                         caller: string | undefined, colours: boolean): Promise<string[]> {
        const prefix = `[Pattern #${this.patternId}]`;
        const location = attempt.location;
        const where = location ? ` at ${location.sourcePath}` +
            (location.line ? `:${location.line}:${location.column}` : '') : '';
        const lines = [`${prefix} ${this.getPatternSource()}`,
            `${prefix} ${attempt.matched ? '✅ SUCCESS' : '❌ FAILED'} matching against ${describeKind(tree)}${where}`];
        if (caller) {
            lines.push(`${prefix}    called from ${caller}`);
        }

        if (attempt.matched) {
            const result = attempt.result as any;
            const captures = new Map<number, JTree | JTree[]>();
            const legend: string[] = [];
            for (const [name, value] of result.storage as Map<string, CaptureStorageValue>) {
                const number = captures.size + 1;
                const code = result.extractElements(value);
                captures.set(number, code);
                const displayName = this.unnamedCaptureMapping.get(name) ?? name;
                const symbol = new CaptureHighlightMarker(number).symbol();
                legend.push(`${prefix}     ${symbol} 🪝${displayName} = ${await describe(code)}`);
            }
            const printed = await this.printCaptures(tree, captures, location?.column);
            lines.push(...printed.map(line => `${prefix}   ${line}`));
            if (legend.length > 0) {
                lines.push(prefix, `${prefix}   Captures:`, ...legend);
            }
        } else {
            const explanation = attempt.explanation;
            const emoji = !colours || shouldUseEmoji();
            const [green, red, reset] = emoji ? ['', '', ''] : ['\x1b[32m', '\x1b[31m', '\x1b[0m'];
            if (emoji) {
                lines.push(`${prefix}    (Mismatched elements are marked with 👉...👈)`);
            }
            const colored = (printed: string[], color: string) =>
                printed.map(line => `${color}${prefix}       ${line}${reset}`);
            const patternLines = await this.printMismatch(patternAst, explanation?.patternElement, focus?.pattern,
                emoji);
            const sourceLines = await this.printMismatch(tree, explanation?.targetElement, focus?.target, emoji,
                location?.column);
            lines.push(prefix, `${green}${prefix}    Pattern tree:${reset}`, ...colored(patternLines, green),
                prefix, `${red}${prefix}    Source tree:${reset}`, ...colored(sourceLines, red));
            if (explanation) {
                lines.push(prefix, `${prefix}    Details:`,
                    `${prefix}      Pattern:  ${explanation.expected}`,
                    `${prefix}      Source:   ${explanation.actual}`,
                    ...(explanation.details ? [`${prefix}      Note:     ${explanation.details}`] : []));
            }
        }
        return lines;
    }

    /** The lines of `tree` around the mismatch, with it marked on `element` and narrowed to `focus` there. */
    private async printMismatch(tree: J, element: JTree | undefined, focus: MarkFocus | undefined,
                                emoji: boolean, column?: number): Promise<string[]> {
        try {
            const delimiters = mismatchDelimiters(emoji);
            const marked = element ? await new ElementMarkerVisitor(element, emoji).visit(tree, undefined) as J : tree;
            let printed = await TreePrinters.printer(JS.Kind.CompilationUnit)
                .print(marked, new AnsiAwarePrintOutputCapture(PATTERN_DEBUG_MARKER_PRINTER));
            if (element && focus) {
                printed = await narrowMark(printed, element as J, focus, delimiters);
            }
            return aroundMarks(layOut(this.replacePlaceholderNames(printed), column).split('\n'), delimiters);
        } catch (e) {
            return [`(tree printing unavailable: ${e})`];
        }
    }

    /** The lines of `tree` with each capture's code between its letter, as in `Ⓐuser?Ⓐ.name`. */
    private async printCaptures(tree: J, captures: Map<number, JTree | JTree[]>, column?: number): Promise<string[]> {
        try {
            const marked = await new ElementMarkerVisitor(captures).visit(tree, undefined) as J;
            let printed = await TreePrinters.printer(JS.Kind.CompilationUnit)
                .print(marked, new AnsiAwarePrintOutputCapture(PATTERN_DEBUG_MARKER_PRINTER));
            // An optional receiver's `?` and a non-null `!` belong to the captured code, which a template
            // writes with them, but the printer writes them after the capture's closing letter
            for (const [number, code] of captures) {
                const last: any = [code].flat().pop();
                const postfix = last?.markers?.markers.some((marker: any) =>
                    marker.kind === JS.Markers.Optional || marker.kind === JS.Markers.NonNullAssertion);
                if (postfix) {
                    const symbol = new CaptureHighlightMarker(number).symbol();
                    printed = printed.split(symbol + '?').join('?' + symbol).split(symbol + '!').join('!' + symbol);
                }
            }
            return layOut(this.replacePlaceholderNames(printed), column).split('\n');
        } catch (e) {
            return [`(tree printing unavailable: ${e})`];
        }
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
     * Matches like {@link match}, and returns why the pattern did not match rather than printing it.
     * A failed attempt's `explanation` holds the reason, the expected and actual code, and the pattern
     * and source nodes where they differ. `debugLog` holds an entry for each capture constraint evaluated.
     *
     * @param tree The AST node to match against
     * @param cursor Cursor at the node's position in a larger tree
     * @param debugOptions Which entries `debugLog` collects, by default all of them
     * @returns Whether the pattern matched, with the captures where it did and the explanation where it did not
     *
     * @example
     * const x = capture('x');
     * const pat = pattern`console.log(${x})`;
     * const attempt = await pat.matchWithExplanation(node, cursor);
     * if (attempt.matched) {
     *     console.log('Captured x:', attempt.result.get('x'));
     * } else {
     *     const {reason, expected, actual} = attempt.explanation!;
     *     console.log(`${reason}: the pattern has ${expected} where the source has ${actual}`);
     * }
     */
    async matchWithExplanation(
        tree: J,
        cursor: Cursor,
        debugOptions?: DebugOptions
    ): Promise<MatchAttemptResult> {
        return (await this.attempt(tree, cursor, debugOptions, true)).attempt;
    }

    /**
     * The attempt, the pattern tree it compared, which holds the explanation's pattern element, and
     * where on each side the mismatch is narrower than its marked node.
     */
    private async attempt(tree: J, cursor: Cursor, debugOptions?: DebugOptions, report: boolean = false):
        Promise<{ attempt: MatchAttemptResult, patternAst: J, focus?: Focus }> {
        const options: DebugOptions = {enabled: true, logComparison: true, logConstraints: true, ...debugOptions};
        const matcher = new Matcher(this, tree, cursor, options);
        const matched = await matcher.matches();
        const attempt: MatchAttemptResult = matched ? {
            matched,
            result: new MatchResult(new Map((matcher as any).storage), () => matcher.patternPrefixes()),
            debugLog: matcher.getDebugLog()
        } : {
            matched,
            explanation: await matcher.getExplanation(text => this.replacePlaceholderNames(text)),
            debugLog: matcher.getDebugLog()
        };
        attempt.location = await sourceLocation(tree, cursor);
        const patternAst = matcher.getPatternAst()!;
        const focus = matcher.getFocus();
        if (report) {
            // Without the pattern's number, which depends on how many patterns were created before it
            const lines = await this.render(tree, attempt, patternAst, focus, undefined, false);
            attempt.report = lines.map(line => line.replace(/^\[Pattern #\d+] ?/, '')).join('\n') + '\n';
        }
        return {attempt, patternAst, focus};
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
 * const match = await pat.match(someNode, cursor);
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
 * const match = await pat.match(methodInvocation, cursor);
 * if (match) {
 *     const capturedArgs = match.get(args);  // Returns J[] for variadic captures
 * }
 */
export class MatchResult implements IMatchResult {
    constructor(
        private readonly storage: Map<string, CaptureStorageValue> = new Map(),
        private readonly computePatternPrefixes: () => Map<string, J.Space> = () => new Map()
    ) {
    }

    private patternPrefixes?: Map<string, J.Space>;

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

    /** @internal */
    [PATTERN_PREFIXES_SYMBOL](): Map<string, J.Space> {
        return this.patternPrefixes ??= this.computePatternPrefixes();
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

    // Debug tracking
    private readonly debugOptions: DebugOptions;
    private readonly debugLog: DebugLogEntry[] = [];
    private explanation?: PendingExplanation;

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
        this.lenientTypeMatching = pattern.options.lenientTypeMatching ?? true;
        this.sameCodeComparator = new JavaScriptSemanticComparatorVisitor(this.lenientTypeMatching);
    }

    private readonly cursor: Cursor;
    private readonly lenientTypeMatching: boolean;
    private readonly sameCodeComparator: JavaScriptSemanticComparatorVisitor;

    /**
     * Checks if the pattern matches the AST node.
     *
     * @returns true if the pattern matches, false otherwise
     */
    async matches(): Promise<boolean> {
        if (!this.patternAst) {
            this.patternAst = await this.pattern.getAstPattern(
                opensWithBrace(this.pattern.templateParts) && this.ast.kind !== J.Kind.Block);
        }

        // A visit reaches what parentheses hold right after them, so seeing through them here would match it twice
        if ((this.ast.kind === J.Kind.Parentheses || this.ast.kind === J.Kind.ControlParentheses) &&
            this.patternAst.kind !== this.ast.kind) {
            if (this.debugOptions.enabled) {
                this.setExplanation('kind-mismatch', 'an expression without parentheses',
                    `parentheses (${this.ast.kind.split('.').pop()})`,
                    'A visitor reaches the expression inside the parentheses next, where this pattern can match it. ' +
                    'To match the parentheses too, write them in the pattern.', this.patternAst, this.ast);
            }
            return false;
        }
        return this.matchNode(this.patternAst, this.ast);
    }

    /**
     * The prefix the pattern writes before each capture, by capture name. A capture of the whole
     * match has none, since the prefix of what it holds lies outside the match.
     */
    patternPrefixes(): Map<string, J.Space> {
        const prefixes = new Map<string, J.Space>();
        walk(this.patternAst, node => {
            const capture = isIdentifier(node) ? PlaceholderUtils.parseCapture(node.simpleName) : null;
            if (capture && !prefixes.has(capture.name) && !this.capturesWholeMatch(capture.name)) {
                prefixes.set(capture.name, (node as J.Identifier).prefix);
            }
            return true;
        });
        return prefixes;
    }

    private capturesWholeMatch(name: string): boolean {
        const bound = this.storage.get(name);
        return bound !== undefined && !Array.isArray(bound) && this.extractElements(bound) === this.ast;
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
        expected: string | Promise<string>,
        actual: string | Promise<string>,
        details?: string,
        patternElement?: any,
        targetElement?: any,
        focus?: Focus
    ): void {
        // Only set the first failure (most relevant)
        if (this.explanation) return;

        this.explanation = {
            reason,
            expected,
            actual,
            details,
            patternElement,
            targetElement,
            focus
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
        // Factory pattern: instantiate debug or production comparator
        // Zero cost in production - DebugPatternMatchingComparator is never instantiated
        const matcherCallbacks: MatcherCallbacks = {
            handleCapture: (capture: CaptureMarker, t: J, w?: J.RightPadded<J>) => this.handleCapture(capture, t, w),
            handleVariadicCapture: (capture: CaptureMarker, ts: J[], ws?: J.RightPadded<J>[]) => this.handleVariadicCapture(capture, ts, ws),
            getCapture: (name: string) => this.storage.get(name),
            saveState: () => this.saveState(),
            restoreState: (state) => this.restoreState(state),
            // Debug callbacks, all present or all absent
            debug: this.debugOptions.enabled ? {
                log: (level: DebugLogEntry['level'], scope: DebugLogEntry['scope'], message: string, data?: any) => this.log(level, scope, message, data),
                setExplanation: (reason: MatchExplanation['reason'], expected: string | Promise<string>,
                                 actual: string | Promise<string>, details?: string, patternElement?: any,
                                 targetElement?: any, focus?: Focus) =>
                    this.setExplanation(reason, expected, actual, details, patternElement, targetElement, focus),
                getExplanation: () => this.explanation,
                restoreExplanation: (explanation: PendingExplanation) => { this.explanation = explanation; },
                clearExplanation: () => { this.explanation = undefined; }
            } : undefined
        };

        const comparator = this.debugOptions.enabled
            ? new DebugPatternMatchingComparator(matcherCallbacks, this.lenientTypeMatching)
            : new PatternMatchingComparator(matcherCallbacks, this.lenientTypeMatching);
        // Pass cursors to allow constraints to navigate to root
        // Pattern cursor is undefined (pattern is the root), target cursor is provided by user
        const result = await comparator.compare(pattern, target, undefined, this.cursor);

        // If match failed and no explanation was set, provide a generic one
        if (!result && this.debugOptions.enabled && !this.explanation) {
            const patternKind = (pattern as any).kind?.split('.').pop() || 'unknown';
            const targetKind = (target as any).kind?.split('.').pop() || 'unknown';
            this.setExplanation(
                'structural-mismatch',
                `Pattern node of type ${patternKind}`,
                `Target node of type ${targetKind}`,
                'Nodes did not match structurally',
                pattern,
                target
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
    private async handleCapture(capture: CaptureMarker, target: J, wrapper?: J.RightPadded<J>): Promise<boolean> {
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
            const bound = this.storage.get(captureName);
            if (bound !== undefined) {
                return this.bindsSameCode(bound, [target]);
            }
            // Store wrapper if available (preserves markers), otherwise store element
            this.storage.set(captureName, wrapper ?? target);
        }

        return true;
    }

    /** A capture named twice in a pattern binds the same code at both places. */
    private async bindsSameCode(bound: CaptureStorageValue, targets: J[]): Promise<boolean> {
        const boundElements = (Array.isArray(bound) ? bound : [bound]).map(b =>
            b.kind === J.Kind.RightPadded ? (b as J.RightPadded<J>).element : b as J);
        if (boundElements.length !== targets.length) {
            return false;
        }
        for (let i = 0; i < targets.length; i++) {
            if (!await this.sameCodeComparator.compare(boundElements[i], targets[i])) {
                return false;
            }
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
    private async handleVariadicCapture(capture: CaptureMarker, targets: J[], wrappers?: J.RightPadded<J>[]): Promise<boolean> {
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
            const bound = this.storage.get(captureName);
            if (bound !== undefined) {
                return this.bindsSameCode(bound, targets);
            }
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
     *
     * @returns The debug log entries, or undefined if debug wasn't enabled
     */
    getDebugLog(): DebugLogEntry[] | undefined {
        return this.debugOptions.enabled ? [...this.debugLog] : undefined;
    }

    /**
     * Why the match failed, once its expected and actual code have printed and `display` has
     * rewritten the capture placeholders in them.
     */
    async getExplanation(display: (text: string) => string): Promise<MatchExplanation | undefined> {
        if (!this.explanation) {
            return undefined;
        }
        const [expected, actual] = await Promise.all([this.explanation.expected, this.explanation.actual]);
        const {focus, ...explanation} = this.explanation;
        return {...explanation, expected: display(expected), actual: display(actual)};
    }

    /** Where the mismatch is narrower than the nodes the explanation marks. */
    getFocus(): Focus | undefined {
        return this.explanation?.focus;
    }

    /** The pattern tree the last attempt compared. */
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
 * To see why it does or does not match some code, pass `{debug: true}` to `match()`
 * or create it with `pattern({debug: true})`, as {@link PatternOptions.debug} describes.
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
 * // Prints why each match attempt matched or not, as PatternOptions.debug describes
 * const pat = pattern({debug: true})`console.log(${x})`;
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
