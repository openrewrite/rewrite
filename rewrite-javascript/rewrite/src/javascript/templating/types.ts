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
import type {MatchResult, Pattern} from "./pattern";
import type {Template} from "./template";
import type {CaptureValue} from "./capture";

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
 * Options for the capture function.
 *
 * The constraint function receives different parameter types depending on whether
 * the capture is variadic:
 * - For regular captures: constraint receives a single node of type T
 * - For variadic captures: constraint receives an array of nodes of type T[]
 *
 * The constraint function can optionally receive a cursor parameter to perform
 * context-aware validation during pattern matching.
 */
export interface CaptureOptions<T = any> {
    name?: string;
    variadic?: boolean | VariadicOptions;
    /**
     * Optional constraint function that validates whether a captured node should be accepted.
     * The function receives:
     * - node: The captured node (or array of nodes for variadic captures)
     * - cursor: Optional cursor providing access to the node's context in the AST
     *
     * @param node The captured node to validate
     * @param cursor Optional cursor at the captured node's position
     * @returns true if the capture should be accepted, false otherwise
     *
     * @example
     * ```typescript
     * // Simple node validation
     * capture<J.Literal>('size', {
     *     constraint: (node) => typeof node.value === 'number' && node.value > 100
     * })
     *
     * // Context-aware validation
     * capture<J.MethodInvocation>('method', {
     *     constraint: (node, cursor) => {
     *         if (!node.name.simpleName.startsWith('get')) return false;
     *         const cls = cursor?.firstEnclosing(isClassDeclaration);
     *         return cls?.name.simpleName === 'ApiController';
     *     }
     * })
     * ```
     */
    constraint?: (node: T, cursor?: Cursor) => boolean;
    /**
     * Type annotation for this capture. When provided, the template engine will generate
     * a preamble declaring the capture identifier with this type annotation, allowing
     * the TypeScript parser/compiler to produce a properly type-attributed AST.
     *
     * Can be specified as:
     * - A string type annotation (e.g., "boolean", "string", "number")
     * - A Type instance from the AST (the type will be inferred from the Type)
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
    getConstraint?(): ((node: T, cursor?: Cursor) => boolean) | undefined;
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
    getConstraint?(): ((node: T) => boolean) | undefined;
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
}

/**
 * Valid parameter types for template literals.
 * - Capture: For pattern matching and reuse
 * - CaptureValue: Result of property access or array operations on captures (e.g., capture.prop, capture[0], capture.slice(1))
 * - TemplateParam: For standalone template parameters
 * - Tree: AST nodes to be inserted directly
 * - Tree[]: Arrays of AST nodes (from variadic capture operations like slice)
 *
 * Note: Primitive values (string, number, boolean) are NOT supported in template literals.
 * Use Template.builder() API if you need to insert literal values.
 */
export type TemplateParameter = Capture | CaptureValue | TemplateParam | Tree | Tree[];

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
 * Configuration for a replacement rule.
 */
export interface RewriteConfig {
    before: Pattern | Pattern[];
    after: Template | ((match: MatchResult) => Template);

    /**
     * Optional context predicate that must evaluate to true for the transformation to be applied.
     * Evaluated after the pattern matches structurally but before applying the template.
     * Provides access to both the matched node and the cursor for context inspection.
     *
     * @param node The matched AST node
     * @param cursor The cursor at the matched node, providing access to ancestors and context
     * @returns true if the transformation should be applied, false otherwise
     *
     * @example
     * ```typescript
     * rewrite(() => ({
     *     before: pattern`await ${_('promise')}`,
     *     after: template`await ${_('promise')}.catch(handleError)`,
     *     where: (node, cursor) => {
     *         // Only apply inside async functions
     *         const method = cursor.firstEnclosing((n: any): n is J.MethodDeclaration =>
     *             n.kind === J.Kind.MethodDeclaration
     *         );
     *         return method?.modifiers.some(m => m.type === 'async') || false;
     *     }
     * }));
     * ```
     */
    where?: (node: J, cursor: Cursor) => boolean | Promise<boolean>;

    /**
     * Optional context predicate that must evaluate to false for the transformation to be applied.
     * Evaluated after the pattern matches structurally but before applying the template.
     * Provides access to both the matched node and the cursor for context inspection.
     *
     * @param node The matched AST node
     * @param cursor The cursor at the matched node, providing access to ancestors and context
     * @returns true if the transformation should NOT be applied, false if it should proceed
     *
     * @example
     * ```typescript
     * rewrite(() => ({
     *     before: pattern`await ${_('promise')}`,
     *     after: template`await ${_('promise')}.catch(handleError)`,
     *     whereNot: (node, cursor) => {
     *         // Don't apply inside try-catch blocks
     *         return cursor.firstEnclosing((n: any): n is J.Try =>
     *             n.kind === J.Kind.Try
     *         ) !== undefined;
     *     }
     * }));
     * ```
     */
    whereNot?: (node: J, cursor: Cursor) => boolean | Promise<boolean>;
}
