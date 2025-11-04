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
 */
export interface CaptureOptions<T = any> {
    name?: string;
    variadic?: boolean | VariadicOptions;
    constraint?: (node: T) => boolean;
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
 * - A bare `pattern`${capture('x')}`` will structurally match ANY expression
 * - Pattern structure determines matching: `pattern`foo(${capture('x')})`` only matches `foo()` calls
 * - Use structural patterns to narrow matching scope before applying semantic validation
 *
 * **Variadic Captures:**
 * Use `{ variadic: true }` to match zero or more nodes in a sequence:
 * ```typescript
 * const args = capture('args', { variadic: true });
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
     */
    getConstraint?(): ((node: T) => boolean) | undefined;
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
 * const rest = any({ variadic: true });
 * const pat = pattern`bar(${capture('first')}, ${rest})`
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
 * - Tree: AST nodes to be inserted directly
 * - Tree[]: Arrays of AST nodes (from variadic capture operations like slice)
 * - Primitives: Values to be converted to literals
 */
export type TemplateParameter = Capture | any | TemplateParam | Tree | Tree[] | string | number | boolean;

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
}

/**
 * Configuration for a replacement rule.
 */
export interface RewriteConfig {
    before: any; // Pattern | Pattern[], but we'll import Pattern in rewrite.ts
    after: any;  // Template, but we'll import Template in rewrite.ts
}
