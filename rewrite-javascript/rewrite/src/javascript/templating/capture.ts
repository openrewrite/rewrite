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
import {Any, Capture, CaptureOptions, TemplateParam, VariadicOptions} from './types';

/**
 * Combines multiple constraints with AND logic.
 * All constraints must return true for the combined constraint to pass.
 *
 * @example
 * const largeEvenNumber = capture('n', {
 *     constraint: and(
 *         (node) => typeof node.value === 'number',
 *         (node) => node.value > 100,
 *         (node) => node.value % 2 === 0
 *     )
 * });
 */
export function and<T>(...constraints: ((node: T, cursor?: Cursor) => boolean)[]): (node: T, cursor?: Cursor) => boolean {
    return (node: T, cursor?: Cursor) => constraints.every(c => c(node, cursor));
}

/**
 * Combines multiple constraints with OR logic.
 * At least one constraint must return true for the combined constraint to pass.
 *
 * @example
 * const stringOrNumber = capture('value', {
 *     constraint: or(
 *         (node) => node.kind === J.Kind.Literal && typeof node.value === 'string',
 *         (node) => node.kind === J.Kind.Literal && typeof node.value === 'number'
 *     )
 * });
 */
export function or<T>(...constraints: ((node: T, cursor?: Cursor) => boolean)[]): (node: T, cursor?: Cursor) => boolean {
    return (node: T, cursor?: Cursor) => constraints.some(c => c(node, cursor));
}

/**
 * Negates a constraint.
 * Returns true when the constraint returns false, and vice versa.
 *
 * @example
 * const notString = capture('value', {
 *     constraint: not((node) => typeof node.value === 'string')
 * });
 */
export function not<T>(constraint: (node: T, cursor?: Cursor) => boolean): (node: T, cursor?: Cursor) => boolean {
    return (node: T, cursor?: Cursor) => !constraint(node, cursor);
}

// Symbol to access the internal capture name without triggering Proxy
export const CAPTURE_NAME_SYMBOL = Symbol('captureName');
// Symbol to access variadic options without triggering Proxy
export const CAPTURE_VARIADIC_SYMBOL = Symbol('captureVariadic');
// Symbol to access constraint function without triggering Proxy
export const CAPTURE_CONSTRAINT_SYMBOL = Symbol('captureConstraint');
// Symbol to access capturing flag without triggering Proxy
export const CAPTURE_CAPTURING_SYMBOL = Symbol('captureCapturing');
// Symbol to access type information without triggering Proxy
export const CAPTURE_TYPE_SYMBOL = Symbol('captureType');

export class CaptureImpl<T = any> implements Capture<T> {
    public readonly name: string;
    [CAPTURE_NAME_SYMBOL]: string;
    [CAPTURE_VARIADIC_SYMBOL]: VariadicOptions | undefined;
    [CAPTURE_CONSTRAINT_SYMBOL]: ((node: T) => boolean) | undefined;
    [CAPTURE_CAPTURING_SYMBOL]: boolean;
    [CAPTURE_TYPE_SYMBOL]: string | Type | undefined;

    constructor(name: string, options?: CaptureOptions<T>, capturing: boolean = true) {
        this.name = name;
        this[CAPTURE_NAME_SYMBOL] = name;
        this[CAPTURE_CAPTURING_SYMBOL] = capturing;

        // Normalize variadic options
        if (options?.variadic) {
            if (typeof options.variadic === 'boolean') {
                this[CAPTURE_VARIADIC_SYMBOL] = {};
            } else {
                this[CAPTURE_VARIADIC_SYMBOL] = {
                    min: options.variadic.min,
                    max: options.variadic.max
                };
            }
        }

        // Store constraint if provided
        if (options?.constraint) {
            this[CAPTURE_CONSTRAINT_SYMBOL] = options.constraint;
        }

        // Store type if provided
        if (options?.type) {
            this[CAPTURE_TYPE_SYMBOL] = options.type;
        }
    }

    getName(): string {
        return this[CAPTURE_NAME_SYMBOL];
    }

    isVariadic(): boolean {
        return this[CAPTURE_VARIADIC_SYMBOL] !== undefined;
    }

    getVariadicOptions(): VariadicOptions | undefined {
        return this[CAPTURE_VARIADIC_SYMBOL];
    }

    getConstraint(): ((node: T) => boolean) | undefined {
        return this[CAPTURE_CONSTRAINT_SYMBOL];
    }

    isCapturing(): boolean {
        return this[CAPTURE_CAPTURING_SYMBOL];
    }

    getType(): string | Type | undefined {
        return this[CAPTURE_TYPE_SYMBOL];
    }
}

export class TemplateParamImpl<T = any> implements TemplateParam<T> {
    public readonly name: string;

    constructor(name: string) {
        this.name = name;
    }

    getName(): string {
        return this.name;
    }
}

/**
 * Represents a property access on a captured value.
 * When you access a property on a Capture (e.g., method.name), you get a CaptureValue
 * that knows how to resolve that property from the matched values.
 */
export class CaptureValue {
    constructor(
        public readonly rootCapture: Capture,
        public readonly propertyPath: string[],
        public readonly arrayOperation?: { type: 'index' | 'slice' | 'length'; args?: number[] }
    ) {}

    /**
     * Resolves this capture value by looking up the root capture in the values map
     * and navigating through the property path.
     */
    resolve(values: Pick<Map<string, J | J[]>, 'get'>): any {
        const rootName = this.rootCapture.getName();
        let current: any = values.get(rootName);

        // Handle array operations on variadic captures
        if (this.arrayOperation && Array.isArray(current)) {
            switch (this.arrayOperation.type) {
                case 'index':
                    current = current[this.arrayOperation.args![0]];
                    break;
                case 'slice':
                    current = current.slice(...this.arrayOperation.args!);
                    break;
                case 'length':
                    current = current.length;
                    break;
            }
        }

        // Navigate through property path
        for (const prop of this.propertyPath) {
            if (current === null || current === undefined || typeof current === 'number') {
                return undefined;
            }
            current = current[prop];
        }

        return current;
    }

    /**
     * Checks if this CaptureValue will resolve to an array that should be expanded.
     */
    isArrayExpansion(): boolean {
        // Only slice operations and the root variadic capture return arrays
        return this.arrayOperation?.type === 'slice' ||
               (this.arrayOperation === undefined && this.propertyPath.length === 0 &&
                (this.rootCapture as any).isVariadic?.());
    }
}

/**
 * Creates a Proxy around a CaptureValue that allows further property access.
 * This enables chaining like method.name.simpleName
 */
function createCaptureValueProxy(
    rootCapture: Capture,
    propertyPath: string[],
    arrayOperation?: { type: 'index' | 'slice' | 'length'; args?: number[] }
): any {
    const captureValue = new CaptureValue(rootCapture, propertyPath, arrayOperation);

    return new Proxy(captureValue, {
        get(target: CaptureValue, prop: string | symbol): any {
            // Allow access to the CaptureValue instance itself and its methods
            if (prop === 'resolve' || prop === 'rootCapture' || prop === 'propertyPath' ||
                prop === 'arrayOperation' || prop === 'isArrayExpansion') {
                const value = target[prop as keyof CaptureValue];
                return typeof value === 'function' ? value.bind(target) : value;
            }

            // For string property access, extend the property path
            if (typeof prop === 'string') {
                return createCaptureValueProxy(target.rootCapture, [...target.propertyPath, prop], target.arrayOperation);
            }

            return undefined;
        }
    });
}

/**
 * Creates a capture specification for use in template patterns.
 *
 * @template T The expected type of the captured AST node (for TypeScript autocomplete only)
 * @returns A Capture object that supports property access for use in templates
 *
 * @remarks
 * **Pattern Matching is Structural:**
 *
 * What a capture matches is determined by the pattern structure, not the type parameter:
 * - `pattern`${capture('x')}`` matches ANY single expression (identifier, literal, call, binary, etc.)
 * - `pattern`foo(${capture('x')})`` matches only expressions inside `foo()` calls
 * - `pattern`${capture('x')} + ${capture('y')}`` matches only binary `+` expressions
 *
 * The TypeScript type parameter `<T>` provides IDE autocomplete but doesn't enforce runtime types.
 *
 * @example
 * // Named inline captures
 * const pat = pattern`${capture('left')} + ${capture('right')}`;
 * // Matches: <any expression> + <any expression>
 *
 * // Unnamed captures
 * const {left, right} = {left: capture(), right: capture()};
 * const pattern = pattern`${left} + ${right}`;
 *
 * @example
 * // Type parameter is for IDE autocomplete only
 * const method = capture<J.MethodInvocation>('method');
 * const pat = pattern`foo(${method})`;
 * // Matches: foo(<any expression>) - not restricted to method invocations!
 * // Type <J.MethodInvocation> only helps with autocomplete in your code
 *
 * @example
 * // Structural pattern determines what matches
 * const arg = capture('arg');
 * const pat = pattern`process(${arg})`;
 * // Matches: process(42), process("text"), process(x + y), etc.
 * // The 'arg' capture will bind to whatever expression is passed to process()
 *
 * @example
 * // Repeated patterns using the same capture
 * const expr = capture('expr');
 * const redundantOr = pattern`${expr} || ${expr}`;
 * // Matches expressions where both sides are identical: x || x, foo() || foo()
 *
 * @example
 * // Property access in templates
 * const method = capture<J.MethodInvocation>('method');
 * template`console.log(${method.name.simpleName})`  // Accesses properties of captured node
 */
/**
 * Creates a Proxy wrapper for CaptureImpl that intercepts property accesses.
 * Shared logic between capture() and any() to avoid duplication.
 */
function createCaptureProxy<T>(impl: CaptureImpl<T>): any {
    return new Proxy(impl as any, {
        get(target: any, prop: string | symbol): any {
            // Allow access to internal symbols
            if (prop === CAPTURE_NAME_SYMBOL) {
                return target[CAPTURE_NAME_SYMBOL];
            }
            if (prop === CAPTURE_VARIADIC_SYMBOL) {
                return target[CAPTURE_VARIADIC_SYMBOL];
            }
            if (prop === CAPTURE_CONSTRAINT_SYMBOL) {
                return target[CAPTURE_CONSTRAINT_SYMBOL];
            }
            if (prop === CAPTURE_CAPTURING_SYMBOL) {
                return target[CAPTURE_CAPTURING_SYMBOL];
            }
            if (prop === CAPTURE_TYPE_SYMBOL) {
                return target[CAPTURE_TYPE_SYMBOL];
            }

            // Support using Capture as object key via computed properties {[x]: value}
            if (prop === Symbol.toPrimitive || prop === 'toString' || prop === 'valueOf') {
                return () => target[CAPTURE_NAME_SYMBOL];
            }

            // Allow methods to be called directly on the target
            if (prop === 'getName' || prop === 'isVariadic' || prop === 'getVariadicOptions' || prop === 'getConstraint' || prop === 'isCapturing' || prop === 'getType') {
                return target[prop].bind(target);
            }

            // For variadic captures, support array-like operations
            if (target.isVariadic() && typeof prop === 'string') {
                // Numeric index access: args[0], args[1], etc.
                const indexNum = Number(prop);
                if (!isNaN(indexNum) && indexNum >= 0 && Number.isInteger(indexNum)) {
                    return createCaptureValueProxy(target, [], { type: 'index', args: [indexNum] });
                }

                // Array method: slice
                if (prop === 'slice') {
                    return (...args: number[]) => {
                        return createCaptureValueProxy(target, [], { type: 'slice', args });
                    };
                }

                // Array property: length
                if (prop === 'length') {
                    return createCaptureValueProxy(target, [], { type: 'length' });
                }
            }

            // For string property access, create a CaptureValue with a property path
            if (typeof prop === 'string') {
                return createCaptureValueProxy(target, [prop]);
            }

            return undefined;
        }
    });
}

// Overload 1: Options object with constraint (no variadic)
export function capture<T = any>(
    options: CaptureOptions<T> & { variadic?: never }
): Capture<T> & T;

// Overload 2: Options object with variadic
export function capture<T = any>(
    options: { name?: string; variadic: true | VariadicOptions; constraint?: (nodes: T[]) => boolean; min?: number; max?: number }
): Capture<T[]> & T[];

// Overload 3: Just a string name (simple named capture)
export function capture<T = any>(name?: string): Capture<T> & T;

// Implementation
export function capture<T = any>(nameOrOptions?: string | CaptureOptions<T>): Capture<T> & T {
    let name: string | undefined;
    let options: CaptureOptions<T> | undefined;

    if (typeof nameOrOptions === 'string') {
        // Simple named capture: capture('name')
        name = nameOrOptions;
        options = undefined;
    } else {
        // Options-based API: capture({ name: 'name', ...options }) or capture()
        options = nameOrOptions;
        name = options?.name;
    }

    const captureName = name || `unnamed_${capture.nextUnnamedId++}`;
    const impl = new CaptureImpl<T>(captureName, options);

    return createCaptureProxy(impl);
}

// Static counter for generating unique IDs for unnamed captures
capture.nextUnnamedId = 1;

/**
 * Creates a non-capturing pattern match for use in patterns.
 *
 * Use `any()` when you need to match AST structure without binding the matched value to a name.
 * This is useful for validation patterns where you care about structure but not the specific values.
 *
 * **Key Differences from `capture()`:**
 * - `any()` returns `Any<T>` type (not `Capture<T>`)
 * - Cannot be used in templates (TypeScript compiler prevents this)
 * - Does not bind matched values (more memory efficient for patterns)
 * - Supports same features: constraints, variadic matching
 *
 * @template T The expected type of the matched AST node (for TypeScript autocomplete and constraints)
 * @param options Optional configuration (variadic, constraint)
 * @returns An Any object that matches patterns without capturing
 *
 * @example
 * // Match any single argument without capturing
 * const pat = pattern`foo(${any()})`;
 *
 * @example
 * // Match with constraint validation
 * const numericArg = any<J.Literal>({
 *     constraint: (node) => typeof node.value === 'number'
 * });
 * const pat = pattern`process(${numericArg})`;
 *
 * @example
 * // Variadic any - match zero or more without capturing
 * const rest = any({ variadic: true });
 * const first = capture('first');
 * const pat = pattern`foo(${first}, ${rest})`;
 *
 * @example
 * // Mixed with captures - capture some, ignore others
 * const important = capture('important');
 * const pat = pattern`
 *     if (${any()}) {
 *         ${important}
 *     }
 * `;
 *
 * @example
 * // Variadic with constraints
 * const numericArgs = any<J.Literal>({
 *     variadic: true,
 *     constraint: (nodes) => nodes.every(n => typeof n.value === 'number')
 * });
 * const pat = pattern`sum(${numericArgs})`;
 */
// Overload 1: Regular any with constraint (most specific - no variadic property)
export function any<T = any>(
    options: { constraint: (node: T) => boolean } & { variadic?: never }
): Any<T> & T;

// Overload 2: Variadic any with constraint
export function any<T = any>(
    options: { variadic: true | VariadicOptions; constraint?: (nodes: T[]) => boolean; min?: number; max?: number }
): Any<T[]> & T[];

// Overload 3: Catch-all for simple any without special options
export function any<T = any>(
    options?: CaptureOptions<T>
): Any<T> & T;

// Implementation
export function any<T = any>(options?: CaptureOptions<T>): Any<T> & T {
    const anonName = `anon_${any.nextAnonId++}`;
    const impl = new CaptureImpl<T>(anonName, options, false); // capturing = false

    return createCaptureProxy(impl);
}

// Static counter for generating unique IDs for anonymous (non-capturing) patterns
any.nextAnonId = 1;

/**
 * Creates a parameter specification for use in standalone templates (not used with patterns).
 *
 * Use `param()` when creating templates that are not used with pattern matching.
 * Use `capture()` when the template works with a pattern.
 *
 * @template T The expected type of the parameter value (for TypeScript autocomplete only)
 * @param name Optional name for the parameter. If not provided, an auto-generated name is used.
 * @returns A TemplateParam object (simpler than Capture, no property access support)
 *
 * @remarks
 * **When to use `param()` vs `capture()`:**
 *
 * - Use `param()` in **standalone templates** (no pattern matching involved)
 * - Use `capture()` in **patterns** and templates used with patterns
 *
 * **Key Differences:**
 * - `TemplateParam` is simpler - no property access proxy overhead
 * - `Capture` supports property access (e.g., `capture('x').name.simpleName`)
 * - Both work in templates, but `param()` makes intent clearer for standalone use
 *
 * @example
 * // ✅ GOOD: Use param() for standalone templates
 * const value = param<J.Literal>('value');
 * const tmpl = template`return ${value} * 2;`;
 * await tmpl.apply(cursor, node, new Map([['value', someLiteral]]));
 *
 * @example
 * // ✅ GOOD: Use capture() with patterns
 * const value = capture('value');
 * const pat = pattern`foo(${value})`;
 * const tmpl = template`bar(${value})`;  // capture() makes sense here
 *
 * @example
 * // ⚠️ CONFUSING: Using capture() in standalone template
 * const value = capture('value');
 * template`return ${value} * 2;`;  // "Capturing" what? There's no pattern!
 *
 * @example
 * // ❌ WRONG: param() doesn't support property access
 * const node = param<J.MethodInvocation>('invocation');
 * template`console.log(${node.name})`  // Error! Use capture() for property access
 */
export function param<T = any>(name?: string): TemplateParam<T> {
    const paramName = name || `unnamed_${capture.nextUnnamedId++}`;
    return new TemplateParamImpl<T>(paramName);
}

/**
 * Concise alias for `capture`. Works well for inline captures in patterns and templates.
 *
 * @param name Optional name for the capture. If not provided, an auto-generated name is used.
 * @returns A Capture object
 *
 * @example
 * // Inline captures with _ alias
 * pattern`isDate(${_('dateArg')})`
 * template`${_('dateArg')} instanceof Date`
 */
export const _ = capture;
