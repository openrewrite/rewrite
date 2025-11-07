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
import {J} from '../../java';
import {Capture, Parameter, TemplateOptions, TemplateParameter} from './types';
import {MatchResult} from './pattern';
import {generateCacheKey, globalAstCache, WRAPPERS_MAP_SYMBOL} from './utils';
import {CAPTURE_NAME_SYMBOL} from './capture';
import {TemplateEngine} from './engine';
import {JS} from '..';

/**
 * Coordinates for template application.
 */
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
 * Builder for creating templates programmatically.
 * Use when template structure is not known at compile time.
 *
 * @example
 * // Conditional construction
 * const builder = Template.builder().code('function foo(x) {');
 * if (needsValidation) {
 *     builder.code('if (typeof x !== "number") throw new Error("Invalid");');
 * }
 * builder.code('return x * 2; }');
 * const tmpl = builder.build();
 *
 * @example
 * // Composition from fragments
 * function createWrapper(innerBody: Capture): Template {
 *     return Template.builder()
 *         .code('function wrapper() { try { ')
 *         .param(innerBody)
 *         .code(' } catch(e) { console.error(e); } }')
 *         .build();
 * }
 */
export class TemplateBuilder {
    private parts: string[] = [];
    private params: TemplateParameter[] = [];

    /**
     * Adds a static string part to the template.
     *
     * @param str The string to add
     * @returns This builder for chaining
     */
    code(str: string): this {
        // If there are already params, we need to add an empty string before this
        if (this.params.length > this.parts.length) {
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
     * Adds a parameter to the template.
     *
     * @param value The parameter value (Capture, Tree, or primitive)
     * @returns This builder for chaining
     */
    param(value: TemplateParameter): this {
        // Ensure we have a part for after this parameter
        if (this.parts.length === 0) {
            this.parts.push('');
        }
        this.params.push(value);
        // Add an empty string for the next part
        this.parts.push('');
        return this;
    }

    /**
     * Builds the template from accumulated parts and parameters.
     *
     * @returns A Template instance
     */
    build(): Template {
        // Ensure parts array is one longer than params array
        while (this.parts.length <= this.params.length) {
            this.parts.push('');
        }

        // Create a synthetic TemplateStringsArray
        const templateStrings = this.parts.slice() as any;
        templateStrings.raw = this.parts.slice();
        Object.defineProperty(templateStrings, 'raw', {
            value: this.parts.slice(),
            writable: false
        });

        // Delegate to the template() function
        return template(templateStrings, ...this.params);
    }
}

/**
 * Template for creating AST nodes.
 *
 * This class provides the public API for template generation.
 * The actual templating logic is handled by the internal TemplateEngine.
 *
 * Templates can reference captures from patterns, and you can access properties
 * of captured nodes using dot notation. This allows you to extract and insert
 * specific subtrees from matched AST nodes.
 *
 * @example
 * // Generate a literal AST node
 * const result = template`2`.apply(cursor, coordinates);
 *
 * @example
 * // Generate an AST node with a parameter
 * const result = template`${capture()}`.apply(cursor, coordinates);
 *
 * @example
 * // Access properties of captured nodes in templates
 * const method = capture<J.MethodInvocation>('method');
 * const pat = pattern`foo(${method})`;
 * const tmpl = template`bar(${method.name})`; // Access the 'name' property
 *
 * const match = await pat.match(someNode);
 * if (match) {
 *     // The template will insert just the 'name' subtree from the captured method
 *     const result = await tmpl.apply(cursor, someNode, match);
 * }
 *
 * @example
 * // Deep property access chains
 * const method = capture<J.MethodInvocation>('method');
 * template`console.log(${method.name.simpleName})` // Navigate multiple properties
 *
 * @example
 * // Array element access
 * const invocation = capture<J.MethodInvocation>('invocation');
 * template`bar(${invocation.arguments.elements[0].element})` // Access array elements
 */
export class Template {
    private options: TemplateOptions = {};
    private _cachedTemplate?: J;

    /**
     * Creates a new builder for constructing templates programmatically.
     *
     * @returns A new TemplateBuilder instance
     *
     * @example
     * const tmpl = Template.builder()
     *     .code('function foo() {')
     *     .code('return ')
     *     .param(capture('value'))
     *     .code('; }')
     *     .build();
     */
    static builder(): TemplateBuilder {
        return new TemplateBuilder();
    }

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
     * Configures this template with additional options.
     *
     * @param options Configuration options
     * @returns This template for method chaining
     *
     * @example
     * template`isDate(${capture('date')})`
     *     .configure({
     *         context: ['import { isDate } from "util"'],
     *         dependencies: { 'util': '^1.0.0' }
     *     })
     */
    configure(options: TemplateOptions): Template {
        this.options = { ...this.options, ...options };
        // Invalidate cache when configuration changes
        this._cachedTemplate = undefined;
        return this;
    }

    /**
     * Gets the template tree for this template, using two-level caching:
     * - Level 1: Instance cache (this._cachedTemplate) - fastest, no lookup needed
     * - Level 2: Global cache (globalAstCache) - fast, shared across all templates
     * - Level 3: TemplateEngine - slow, parses and processes the template
     *
     * Since all parameters are now placeholders (no primitives), templates with the same
     * structure always parse to the same AST regardless of parameter values.
     *
     * @returns The cached or newly computed template tree
     * @internal
     */
    async getTemplateTree(): Promise<JS.CompilationUnit> {
        // Level 1: Instance cache (fastest path)
        if (this._cachedTemplate) {
            return this._cachedTemplate as JS.CompilationUnit;
        }

        // Generate cache key for global lookup
        // Since all parameters use placeholders, we only need the template structure
        const contextStatements = this.options.context || this.options.imports || [];
        const parametersKey = this.parameters.length.toString(); // Just the count
        const cacheKey = generateCacheKey(
            this.templateParts,
            parametersKey,
            contextStatements,
            this.options.dependencies || {}
        );

        // Level 2: Global cache (fast path - shared with Pattern)
        const cached = globalAstCache.get(cacheKey);
        if (cached) {
            this._cachedTemplate = cached as JS.CompilationUnit;
            return cached as JS.CompilationUnit;
        }

        // Level 3: Compute via TemplateEngine (slow path)
        const result = await TemplateEngine.getTemplateTree(
            this.templateParts,
            this.parameters,
            contextStatements,
            this.options.dependencies || {}
        ) as JS.CompilationUnit;

        // Cache in both levels
        globalAstCache.set(cacheKey, result);
        this._cachedTemplate = result;

        return result;
    }

    /**
     * Applies this template and returns the resulting tree.
     *
     * @param cursor The cursor pointing to the current location in the AST
     * @param tree Input tree
     * @param values values for parameters in template
     * @returns A Promise resolving to the generated AST node
     */
    async apply(cursor: Cursor, tree: J, values?: Map<Capture | string, J> | Pick<Map<string, J>, 'get'> | Record<string, J>): Promise<J | undefined> {
        // Normalize the values map: convert any Capture keys to string keys
        let normalizedValues: Pick<Map<string, J>, 'get'> | undefined;
        let wrappersMap: Map<string, J.RightPadded<J> | J.RightPadded<J>[]> = new Map();

        if (values instanceof MatchResult) {
            // MatchResult - extract both bindings and wrappersMap
            normalizedValues = values;
            wrappersMap = (values as any)[WRAPPERS_MAP_SYMBOL]();
        } else if (values instanceof Map) {
            const normalized = new Map<string, J>();
            for (const [key, value] of values.entries()) {
                const stringKey = typeof key === 'string'
                    ? key
                    : ((key as any)[CAPTURE_NAME_SYMBOL] || key.getName());
                normalized.set(stringKey, value);
            }
            normalizedValues = normalized;
        } else if (values && typeof values === 'object') {
            // Check if it's a Map-like object with 'get' method, or a plain object literal
            if ('get' in values && typeof values.get === 'function') {
                // Map-like object with get method
                normalizedValues = values as Pick<Map<string, J>, 'get'>;
            } else {
                // Plain object literal - convert to Map
                // Keys may be strings or Capture objects (via computed properties {[x]: value})
                const normalized = new Map<string, J>();
                for (const [key, value] of Object.entries(values)) {
                    // If the key happens to be a stringified Capture (from computed properties),
                    // it's already been converted to a string by JavaScript
                    normalized.set(key, value);
                }
                normalizedValues = normalized;
            }
        }

        // Use instance-level cache to get the template tree
        const ast = await this.getTemplateTree();

        // Delegate to TemplateEngine for placeholder substitution and application
        return TemplateEngine.applyTemplateFromAst(
            ast,
            this.parameters,
            cursor,
            {
                tree,
                mode: JavaCoordinates.Mode.Replace
            },
            normalizedValues,
            wrappersMap
        );
    }
}

/**
 * Tagged template function for creating templates that generate AST nodes.
 *
 * Templates support property access on captures from patterns, allowing you to
 * extract and insert specific subtrees from matched AST nodes. Use dot notation
 * to navigate properties (e.g., `method.name`) or array bracket notation to
 * access array elements (e.g., `args.elements[0].element`).
 *
 * @param strings The string parts of the template
 * @param parameters The parameters between the string parts (Capture, CaptureValue, TemplateParam, Tree, or Tree[])
 * @returns A Template object that can be applied to generate AST nodes
 *
 * @example
 * // Simple template with literal
 * const tmpl = template`console.log("hello")`;
 * const result = await tmpl.apply(cursor, node);
 *
 * @example
 * // Template with capture - matches captured value from pattern
 * const expr = capture('expr');
 * const pat = pattern`foo(${expr})`;
 * const tmpl = template`bar(${expr})`;
 *
 * const match = await pat.match(node);
 * if (match) {
 *     const result = await tmpl.apply(cursor, node, match);
 * }
 *
 * @example
 * // Property access on captures - extract subtrees
 * const method = capture<J.MethodInvocation>('method');
 * const pat = pattern`foo(${method})`;
 * // Access the 'name' property of the captured method invocation
 * const tmpl = template`bar(${method.name})`;
 *
 * @example
 * // Deep property chains
 * const method = capture<J.MethodInvocation>('method');
 * template`console.log(${method.name.simpleName})`
 *
 * @example
 * // Array element access
 * const invocation = capture<J.MethodInvocation>('invocation');
 * template`bar(${invocation.arguments.elements[0].element})`
 */
export function template(strings: TemplateStringsArray, ...parameters: TemplateParameter[]): Template {
    // Convert parameters to Parameter objects (no longer need to check for mutable tree property)
    const processedParameters = parameters.map(param => {
        // Just wrap each parameter value in a Parameter object
        return {value: param};
    });

    return new Template(strings, processedParameters);
}

export type {JavaCoordinates};
