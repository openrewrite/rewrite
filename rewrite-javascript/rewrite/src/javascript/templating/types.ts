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
import {Cursor, Tree} from '../..';
import {J, Type} from '../../java';
import type {Pattern} from "./pattern";
import type {Template} from "./template";
import type {CaptureValue, RawCode} from "./capture";

/**
 * Options for variadic captures that match zero or more nodes in a sequence.
 */
export interface VariadicOptions {
    /**
     * Minimum number of nodes that must be matched (default: 0).
     */
    min?: number;

    /**
     * Maximum number of nodes that can be matched (default: unlimited).
     */
    max?: number;
}

/**
 * Read-only access to captures matched so far during pattern matching.
 * Provides a consistent interface with MatchResult for looking up captured values.
 */
export interface CaptureMap {
    /**
     * Gets the value of a capture by Capture object or name.
     * Returns undefined if the capture hasn't been matched yet.
     */
    get<T>(capture: Capture<T>): T | undefined;
    get(capture: string): any;

    /**
     * Checks if a capture has been matched.
     */
    has(capture: Capture | string): boolean;
}

/**
 * Context passed to capture constraint functions.
 * Provides access to the cursor for AST navigation and previously matched captures.
 */
export interface CaptureConstraintContext {
    /**
     * The cursor pointing to the node being matched.
     * Allows navigating the AST (parent, root, etc.).
     */
    cursor: Cursor;

    /**
     * Read-only view of values captured so far in the matching process.
     * Allows constraints to depend on previous captures.
     * Returns undefined for captures that haven't been processed yet.
     */
    captures: CaptureMap;
}

/**
 * Constraint function for captures.
 *
 * Receives the node being validated and a context providing access to:
 * - cursor: For navigating the AST
 * - captures: For accessing previously matched captures
 *
 * For non-variadic captures: use ConstraintFunction<T> where T is the node type
 * For variadic captures: use ConstraintFunction<T[]> where T[] is the array type
 *
 * When used with variadic captures, the cursor points to the nearest common parent
 * of the captured elements.
 */
export type ConstraintFunction<T> = (node: T, context: CaptureConstraintContext) => boolean;

/**
 * Options for the capture function.
 *
 * The constraint function receives different parameter types depending on whether
 * the capture is variadic:
 * - For regular captures: constraint receives a single node of type T
 * - For variadic captures: constraint receives an array of nodes of type T[]
 *
 * The context parameter provides access to the cursor and previously matched captures.
 */
export interface CaptureOptions<T = any> {
    name?: string;
    variadic?: boolean | VariadicOptions;
    /**
     * Optional constraint function that validates whether a captured node should be accepted.
     * The function receives:
     * - node: The captured node (or array of nodes for variadic captures)
     * - context: Provides access to cursor and previously matched captures
     *
     * @param node The captured node to validate
     * @param context Provides cursor for AST navigation and previously matched captures
     * @returns true if the capture should be accepted, false otherwise
     *
     * @example
     * ```typescript
     * // Simple node validation
     * capture<J.Literal>('size', {
     *     constraint: (node) => typeof node.value === 'number' && node.value > 100
     * })
     *
     * // Context-aware validation using cursor
     * capture<J.MethodInvocation>('method', {
     *     constraint: (node, context) => {
     *         if (!node.name.simpleName.startsWith('get')) return false;
     *         const cls = context.cursor.firstEnclosing(isClassDeclaration);
     *         return cls?.name.simpleName === 'ApiController';
     *     }
     * })
     *
     * // Validation depending on previous captures
     * const min = capture('min');
     * const max = capture('max', {
     *     constraint: (node, context) => {
     *         const minVal = context.captures.get(min);
     *         return minVal && node.value > minVal.value;
     *     }
     * })
     * ```
     */
    constraint?: ConstraintFunction<T>;
    /**
     * Type annotation for this capture. When provided, the template engine will generate
     * a preamble declaring the capture identifier with this type annotation, allowing
     * the TypeScript parser/compiler to produce a properly type-attributed AST.
     *
     * **Why Use Type Attribution:**
     * When matching against TypeScript code with type information, providing a type ensures
     * the pattern's AST has matching type attribution, which can be important for:
     * - Semantic matching based on types
     * - Matching code that depends on type inference
     * - Ensuring pattern parses with correct type context
     *
     * Can be specified as:
     * - A string type annotation (e.g., "boolean", "string", "number", "Promise<any>", "User[]")
     * - A Type instance from the AST (the type will be inferred from the Type)
     *
     * @example
     * ```typescript
     * // Match promise chains with proper type attribution
     * const chain = capture({
     *   name: 'chain',
     *   type: 'Promise<any>',  // TypeScript will attribute this as Promise type
     *   constraint: (call: J.MethodInvocation) => {
     *     // Validate promise chain structure
     *     return call.name.simpleName === 'then';
     *   }
     * });
     * pattern`${chain}.catch(err => console.log(err))`
     *
     * // Match arrays with type annotation
     * const items = capture({
     *   name: 'items',
     *   type: 'number[]',  // Array of numbers
     * });
     * pattern`${items}.map(x => x * 2)`
     * ```
     */
    type?: string | Type;
}

/**
 * Capture specification for pattern matching.
 * Represents a placeholder in a template pattern that can capture a part of the AST.
 *
 * @template T The expected type of the captured AST node (for TypeScript autocomplete)
 *
 * @remarks
 * **Important: Type Parameter is for IDE Support Only**
 *
 * The generic type parameter `<T>` provides IDE autocomplete and type checking in your code,
 * but does NOT enforce any runtime constraints on what the capture will match.
 *
 * **Pattern Matching Behavior:**
 * - A bare `pattern`${capture()}`` will structurally match ANY expression
 * - Pattern structure determines matching: `pattern`foo(${capture()})`` only matches `foo()` calls with one arg
 * - Use structural patterns to narrow matching scope before applying semantic validation
 *
 * **Variadic Captures:**
 * Use `{ variadic: true }` to match zero or more nodes in a sequence:
 * ```typescript
 * const args = capture({ variadic: true });
 * pattern`foo(${args})`  // Matches: foo(), foo(a), foo(a, b, c)
 * ```
 */
export interface Capture<T = any> {
    /**
     * Gets the string name of this capture.
     */
    getName(): string;

    /**
     * Returns true if this is a variadic capture (matching zero or more nodes).
     */
    isVariadic(): boolean;

    /**
     * Returns the variadic options if this is a variadic capture, undefined otherwise.
     */
    getVariadicOptions(): VariadicOptions | undefined;

    /**
     * Gets the constraint function if this capture has one.
     * For regular captures (T = Expression), constraint receives a single node.
     * For variadic captures (T = Expression[]), constraint receives an array of nodes.
     * The constraint function can optionally receive a cursor for context-aware validation.
     */
    getConstraint?(): ConstraintFunction<T> | undefined;
}

/**
 * Non-capturing pattern match specification.
 * Represents a placeholder in a pattern that matches AST nodes without binding them to a name.
 *
 * Use `any()` when you need to match structure without caring about the specific values.
 * The key difference from `Capture` is that `Any` cannot be used in templates - the TypeScript
 * type system prevents this at compile time.
 *
 * @template T The expected type of the matched AST node (for TypeScript autocomplete and constraints)
 *
 * @remarks
 * **Why Any<T> is Separate from Capture<T>:**
 *
 * Using a separate type provides compile-time safety:
 * - `pattern`foo(${any()})`` - ✅ OK in patterns
 * - `template`bar(${any()})`` - ❌ TypeScript error (Any<T> not assignable to template parameters)
 *
 * This prevents logical errors where you try to use a non-capturing match in a template.
 *
 * **Semantic Parallel with TypeScript's `any`:**
 *
 * Just as TypeScript's `any` type means "be permissive about types here",
 * pattern matching's `any()` means "be permissive about values here":
 * - TypeScript `any`: Accept any type, don't check it
 * - Pattern `any()`: Match any value, don't bind it
 *
 * @example
 * // Match without capturing
 * const pat = pattern`foo(${any()})`
 *
 * @example
 * // Variadic any - match zero or more without capturing
 * const first = any();
 * const rest = any({ variadic: true });
 * const pat = pattern`bar(${first}, ${rest})`
 *
 * @example
 * // With constraints - validate but don't capture
 * const numericArg = any<J.Literal>({
 *     constraint: (node) => typeof node.value === 'number'
 * });
 * const pat = pattern`process(${numericArg})`
 */
export interface Any<T = any> {
    /**
     * Gets the internal identifier for this any pattern.
     */
    getName(): string;

    /**
     * Returns true if this is a variadic any (matching zero or more nodes).
     */
    isVariadic(): boolean;

    /**
     * Returns the variadic options if this is a variadic any, undefined otherwise.
     */
    getVariadicOptions(): VariadicOptions | undefined;

    /**
     * Gets the constraint function if this any pattern has one.
     * For regular any (T = Expression), constraint receives a single node.
     * For variadic any (T = Expression[]), constraint receives an array of nodes.
     */
    getConstraint?(): ConstraintFunction<T> | undefined;
}

/**
 * Template parameter specification for template-only parameter substitution.
 * Unlike Capture, TemplateParam does not support property access and is simpler.
 *
 * @template T The expected type of the parameter value (for TypeScript autocomplete only)
 */
export interface TemplateParam<T = any> {
    /**
     * The name of the parameter, used to look up the value in the values map.
     */
    readonly name: string;

    /**
     * Gets the string name of this parameter.
     */
    getName(): string;
}

/**
 * Configuration options for patterns.
 */
export interface PatternOptions {
    /**
     * Declarations to provide type attribution context for the pattern.
     * These can include import statements, type declarations, function declarations, or any
     * other declarations needed for proper type information. They are prepended to the pattern
     * when parsing to ensure proper type attribution.
     *
     * @example
     * ```typescript
     * pattern`forwardRef(${capture('comp')})`
     *     .configure({
     *         context: [
     *             `import { forwardRef } from 'react'`,
     *             `type MyType = { value: number }`
     *         ]
     *     })
     * ```
     */
    context?: string[];

    /**
     * @deprecated Use `context` instead. This alias will be removed in a future version.
     *
     * Import statements to provide type attribution context.
     * These are prepended to the pattern when parsing to ensure proper type information.
     */
    imports?: string[];

    /**
     * NPM dependencies required for import resolution and type attribution.
     * Maps package names to version specifiers (e.g., { 'util': '^1.0.0' }).
     * The template engine will create a package.json with these dependencies.
     */
    dependencies?: Record<string, string>;

    /**
     * When true, allows patterns without type annotations to match code with type annotations.
     * This enables more flexible pattern matching during development or when full type attribution
     * is not needed. When false, enforces strict type matching where both pattern and target must
     * have matching type annotations.
     *
     * @default true (lenient matching enabled for backward compatibility)
     */
    lenientTypeMatching?: boolean;

    /**
     * Enable debug logging for this pattern.
     * When enabled, all match attempts will log detailed information to stderr,
     * including the AST path traversed, mismatches encountered, and captured values.
     *
     * Can be overridden at the match() call level.
     * Global debug can be enabled via PATTERN_DEBUG=true environment variable.
     *
     * Precedence: match() call > pattern configure() > PATTERN_DEBUG env var
     *
     * @default undefined (inherits from environment or match() call)
     *
     * @example
     * ```typescript
     * // Pattern-level debug
     * const pat = pattern({ debug: true })`console.log(${value})`;
     *
     * // Disable debug for a noisy pattern when global debug is on
     * const noisyPat = pattern({ debug: false })`import ${x} from ${y}`;
     * ```
     */
    debug?: boolean;
}

/**
 * Options for individual match() calls.
 */
export interface MatchOptions {
    /**
     * Enable debug logging for this specific match() call.
     * Overrides pattern-level debug setting and global PATTERN_DEBUG env var.
     *
     * @example
     * ```typescript
     * // Debug just this call
     * const match = await pattern.match(node, cursor, { debug: true });
     *
     * // Disable debug for this call even if pattern or global debug is on
     * const match = await pattern.match(node, cursor, { debug: false });
     * ```
     */
    debug?: boolean;
}

/**
 * Valid parameter types for template literals.
 * - Capture: For pattern matching and reuse
 * - CaptureValue: Result of property access or array operations on captures (e.g., capture.prop, capture[0], capture.slice(1))
 * - TemplateParam: For standalone template parameters
 * - RawCode: For inserting literal code strings at construction time
 * - Tree: AST nodes to be inserted directly
 * - Tree[]: Arrays of AST nodes (from variadic capture operations like slice)
 * - J.RightPadded<any>: Wrapper containing an element with markers (element will be extracted)
 * - J.RightPadded<any>[]: Array of wrappers (elements will be expanded)
 * - J.Container<any>: Container with elements (elements will be expanded)
 *
 * Note: Primitive values (string, number, boolean) are NOT supported in template literals.
 * Use raw() for inserting code strings, or Template.builder() API for programmatic construction.
 */
export type TemplateParameter =
    Capture
    | CaptureValue
    | TemplateParam
    | RawCode
    | Tree
    | Tree[]
    | J.RightPadded<any>
    | J.RightPadded<any>[]
    | J.Container<any>;

/**
 * Parameter specification for template generation (internal).
 * Represents a placeholder in a template that will be replaced with a parameter value.
 * This is the internal wrapper used by the template engine.
 *
 * Note: The value is typed as `any` rather than `TemplateParameter` to allow flexible
 * internal handling without excessive type guards. The public API (template function)
 * constrains inputs to `TemplateParameter`, providing type safety at the API boundary.
 */
export interface Parameter {
    /**
     * The value to substitute into the template.
     */
    value: any;
}

/**
 * Configuration options for templates.
 */
export interface TemplateOptions {
    /**
     * Declarations to provide type attribution context for the template.
     * These can include import statements, type declarations, function declarations, or any
     * other declarations needed for proper type information. They are prepended to the template
     * when parsing to ensure proper type attribution.
     *
     * @example
     * ```typescript
     * template`console.log(${capture('value')})`
     *     .configure({
     *         context: [
     *             `type MyType = { value: number }`,
     *             `const console = { log: (x: any) => void 0 }`
     *         ]
     *     })
     * ```
     */
    context?: string[];

    /**
     * @deprecated Use `context` instead. This alias will be removed in a future version.
     *
     * Import statements to provide type attribution context.
     * These are prepended to the template when parsing to ensure proper type information.
     */
    imports?: string[];

    /**
     * NPM dependencies required for import resolution and type attribution.
     * Maps package names to version specifiers (e.g., { 'util': '^1.0.0' }).
     * The template engine will create a package.json with these dependencies.
     */
    dependencies?: Record<string, string>;
}

/**
 * Options for template application.
 */
export interface ApplyOptions {
    /**
     * Values for parameters in the template.
     * Can be a Map, MatchResult, or plain object with capture names as keys.
     *
     * @example
     * ```typescript
     * // Using MatchResult from pattern matching
     * const match = await pattern.match(node, cursor);
     * await template.apply(node, cursor, { values: match });
     *
     * // Using a Map
     * await template.apply(node, cursor, {
     *     values: new Map([['x', someNode]])
     * });
     *
     * // Using a plain object
     * await template.apply(node, cursor, {
     *     values: { x: someNode }
     * });
     * ```
     */
    values?: Map<Capture | string, J> | MatchResult | Record<string, J>;
}

/**
 * Represents a replacement rule that can match a pattern and apply a template.
 */
export interface RewriteRule {
    /**
     * Attempts to apply this rewrite rule to the given AST node.
     *
     * @param cursor The cursor context at the current position in the AST
     * @param node The AST node to try matching and transforming
     * @returns The transformed node if a pattern matched, or `undefined` if no pattern matched.
     *          When using in a visitor, always use the `|| node` pattern to return the original
     *          node when there's no match: `return await rule.tryOn(this.cursor, node) || node;`
     */
    tryOn(cursor: Cursor, node: J): Promise<J | undefined>;

    /**
     * Chains this rule with another rule, creating a composite rule that applies both transformations sequentially.
     *
     * The resulting rule:
     * 1. First applies this rule to the input node
     * 2. If this rule matches and transforms the node, applies the next rule to the result
     * 3. If the next rule returns undefined (no match), keeps the result from the first rule
     * 4. If this rule returns undefined (no match), returns undefined without trying the next rule
     *
     * @param next The rule to apply after this rule
     * @returns A new RewriteRule that applies both rules in sequence
     *
     * @example
     * ```typescript
     * const rule1 = rewrite(() => {
     *     const { a, b } = { a: capture(), b: capture() };
     *     return {
     *         before: pattern`${a} + ${b}`,
     *         after: template`${b} + ${a}`
     *     };
     * });
     *
     * const rule2 = rewrite(() => ({
     *     before: pattern`${capture('x')} + 1`,
     *     after: template`${capture('x')}++`
     * }));
     *
     * const combined = rule1.andThen(rule2);
     * // Will first swap operands, then if result matches "x + 1", change to "x++"
     * ```
     */
    andThen(next: RewriteRule): RewriteRule;

    /**
     * Creates a composite rule that tries this rule first, and if it doesn't match, tries an alternative rule.
     *
     * The resulting rule:
     * 1. First applies this rule to the input node
     * 2. If this rule matches and transforms the node, returns the result
     * 3. If this rule returns undefined (no match), tries the alternative rule on the original node
     *
     * @param alternative The rule to try if this rule doesn't match
     * @returns A new RewriteRule that tries both rules with fallback behavior
     *
     * @example
     * ```typescript
     * // Try specific pattern first, fall back to general pattern
     * const specific = rewrite(() => ({
     *     before: pattern`foo(${capture('x')}, 0)`,
     *     after: template`bar(${capture('x')})`
     * }));
     *
     * const general = rewrite(() => ({
     *     before: pattern`foo(${capture('x')}, ${capture('y')})`,
     *     after: template`baz(${capture('x')}, ${capture('y')})`
     * }));
     *
     * const combined = specific.orElse(general);
     * // Will try specific pattern first, if no match, try general pattern
     * ```
     */
    orElse(alternative: RewriteRule): RewriteRule;
}

/**
 * Context for preMatch predicate - only has cursor, no captures yet.
 */
export interface PreMatchContext {
    /**
     * The cursor pointing to the node being considered for matching.
     * Allows navigating the AST (parent, root, etc.).
     */
    cursor: Cursor;
}

/**
 * Context for postMatch predicate - has cursor and captured values.
 */
export interface PostMatchContext {
    /**
     * The cursor pointing to the matched node.
     * Allows navigating the AST (parent, root, etc.).
     */
    cursor: Cursor;

    /**
     * Values captured during pattern matching.
     */
    captures: CaptureMap;
}

/**
 * Configuration for a replacement rule.
 */
export interface RewriteConfig {
    before: Pattern | Pattern[];
    after: Template | ((match: MatchResult) => Template);

    /**
     * Optional predicate evaluated BEFORE pattern matching.
     * Use for efficient early filtering based on AST context when captures aren't needed.
     * If this returns false, pattern matching is skipped entirely.
     *
     * @param node The AST node being considered for matching
     * @param context Context providing cursor for AST navigation
     * @returns true to proceed with pattern matching, false to skip this node
     *
     * @example
     * ```typescript
     * rewrite(() => ({
     *     before: pattern`console.log(${_('msg')})`,
     *     after: template`logger.info(${_('msg')})`,
     *     preMatch: (node, {cursor}) => {
     *         // Only attempt matching inside functions named 'handleError'
     *         const method = cursor.firstEnclosing(isMethodDeclaration);
     *         return method?.name.simpleName === 'handleError';
     *     }
     * }));
     * ```
     */
    preMatch?: (node: J, context: PreMatchContext) => boolean | Promise<boolean>;

    /**
     * Optional predicate evaluated AFTER pattern matching succeeds.
     * Use when you need access to captured values to decide whether to apply the transformation.
     * If this returns false, the transformation is not applied.
     *
     * @param node The matched AST node
     * @param context Context providing cursor for AST navigation and captured values
     * @returns true to apply the transformation, false to skip
     *
     * @example
     * ```typescript
     * rewrite(() => ({
     *     before: pattern`${_('a')} + ${_('b')}`,
     *     after: template`${_('b')} + ${_('a')}`,
     *     postMatch: (node, {cursor, captures}) => {
     *         // Only swap if 'a' is a literal number
     *         const a = captures.get('a');
     *         return a?.kind === J.Kind.Literal && typeof a.value === 'number';
     *     }
     * }));
     * ```
     */
    postMatch?: (node: J, context: PostMatchContext) => boolean | Promise<boolean>;
}

/**
 * Options for debugging pattern matching.
 * Used in Layer 1 (Core Instrumentation) to control debug output.
 */
export interface DebugOptions {
    /**
     * Enable detailed logging during pattern matching.
     */
    enabled?: boolean;

    /**
     * Log structural comparison steps.
     */
    logComparison?: boolean;

    /**
     * Log constraint evaluation.
     */
    logConstraints?: boolean;
}

/**
 * A single debug log entry collected during pattern matching.
 * Part of Layer 1 (Core Instrumentation).
 */
export interface DebugLogEntry {
    /**
     * Severity level of the log entry.
     */
    level: 'trace' | 'debug' | 'info' | 'warn';

    /**
     * The scope/category of the log entry.
     */
    scope: 'matching' | 'comparison' | 'constraint';

    /**
     * Human-readable message.
     */
    message: string;

    /**
     * Optional data associated with this log entry.
     */
    data?: any;
}

/**
 * Detailed explanation of why a pattern failed to match.
 * Built by Layer 1 (Core Instrumentation) and exposed by Layer 2 (API).
 */
export interface MatchExplanation {
    /**
     * The reason for the match failure.
     */
    reason: 'structural-mismatch' | 'constraint-failed' | 'type-mismatch' | 'kind-mismatch' | 'value-mismatch' | 'array-length-mismatch';

    /**
     * Human-readable description of what was expected.
     */
    expected: string;

    /**
     * Human-readable description of what was actually found.
     */
    actual: string;

    /**
     * The actual pattern element that failed to match.
     * Used for debug visualization - attach markers to this element.
     */
    patternElement?: any;

    /**
     * The actual target element that failed to match.
     * Used for debug visualization - attach markers to this element.
     */
    targetElement?: any;

    /**
     * For constraint failures, details about which constraints failed.
     */
    constraintFailures?: Array<{
        captureName: string;
        actualValue: any;
        error?: string;
    }>;

    /**
     * Additional context about the failure.
     */
    details?: string;
}

/**
 * Interface for accessing captured nodes from a successful pattern match.
 * Part of the public API.
 */
export interface MatchResult {
    /**
     * Get a captured node by name or by Capture object.
     *
     * @param capture The capture name (string) or Capture object
     * @returns The captured node(s), or undefined if not found
     */
    get(capture: string): any;

    get<T>(capture: Capture<T>): T | undefined;

    /**
     * Checks if a capture has been matched.
     *
     * @param capture The capture name (string) or Capture object
     * @returns true if the capture exists in the match result
     */
    has(capture: Capture | string): boolean;
}

/**
 * Result of a pattern match attempt with debug information.
 * Part of Layer 2 (Public API).
 */
export interface MatchAttemptResult {
    /**
     * Whether the pattern matched successfully.
     */
    matched: boolean;

    /**
     * If matched, the match result with captured nodes. Undefined if not matched.
     * Use `result.get('captureName')` or `result.get(captureObject)` to access captures.
     */
    result?: MatchResult;

    /**
     * If not matched, explanation of why. Undefined if matched.
     */
    explanation?: MatchExplanation;

    /**
     * Debug log entries collected during matching (if debug was enabled).
     */
    debugLog?: DebugLogEntry[];

    /**
     * Cursors from the comparator (for debug visualization).
     * @internal
     */
    patternCursor?: Cursor;
    targetCursor?: Cursor;

    /**
     * The actual pattern AST that was used during matching (for debug visualization).
     * @internal
     */
    patternAst?: any;
}
