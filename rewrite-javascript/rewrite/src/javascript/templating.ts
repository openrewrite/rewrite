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
import {JS} from '.';
import {JavaScriptParser} from './parser';
import {JavaScriptVisitor} from './visitor';
import {Cursor, isTree, Tree} from '..';
import {J} from '../java';
import {JavaScriptSemanticComparatorVisitor} from "./comparator";
import {DependencyWorkspace} from './dependency-workspace';
import {Marker} from '../markers';
import {randomId} from '../uuid';
import {produce} from "immer";

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
export function and<T>(...constraints: ((node: T) => boolean)[]): (node: T) => boolean {
    return (node: T) => constraints.every(c => c(node));
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
export function or<T>(...constraints: ((node: T) => boolean)[]): (node: T) => boolean {
    return (node: T) => constraints.some(c => c(node));
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
export function not<T>(constraint: (node: T) => boolean): (node: T) => boolean {
    return (node: T) => !constraint(node);
}

/**
 * Cache for compiled templates and patterns.
 * Stores parsed ASTs to avoid expensive re-parsing and dependency resolution.
 */
class TemplateCache {
    private cache = new Map<string, JS.CompilationUnit>();

    /**
     * Generates a cache key from template string, captures, and options.
     */
    private generateKey(
        templateString: string,
        captures: Capture[],
        contextStatements: string[],
        dependencies: Record<string, string>
    ): string {
        // Use the actual template string (with placeholders) as the primary key
        const templateKey = templateString;

        // Capture names
        const capturesKey = captures.map(c => c.getName()).join(',');

        // Context statements
        const contextKey = contextStatements.join(';');

        // Dependencies
        const depsKey = JSON.stringify(dependencies || {});

        return `${templateKey}::${capturesKey}::${contextKey}::${depsKey}`;
    }

    /**
     * Gets a cached compilation unit or creates and caches a new one.
     */
    async getOrParse(
        templateString: string,
        captures: Capture[],
        contextStatements: string[],
        dependencies: Record<string, string>
    ): Promise<JS.CompilationUnit> {
        const key = this.generateKey(templateString, captures, contextStatements, dependencies);

        let cu = this.cache.get(key);
        if (cu) {
            return cu;
        }

        // Create workspace if dependencies are provided
        // DependencyWorkspace has its own cache, so multiple templates with
        // the same dependencies will automatically share the same workspace
        let workspaceDir: string | undefined;
        if (dependencies && Object.keys(dependencies).length > 0) {
            workspaceDir = await DependencyWorkspace.getOrCreateWorkspace(dependencies);
        }

        // Prepend context statements for type attribution context
        const fullTemplateString = contextStatements.length > 0
            ? contextStatements.join('\n') + '\n' + templateString
            : templateString;

        // Parse and cache (workspace only needed during parsing)
        const parser = new JavaScriptParser({relativeTo: workspaceDir});
        const parseGenerator = parser.parse({text: fullTemplateString, sourcePath: 'template.ts'});
        cu = (await parseGenerator.next()).value as JS.CompilationUnit;

        this.cache.set(key, cu);
        return cu;
    }

    /**
     * Clears the cache.
     */
    clear(): void {
        this.cache.clear();
    }
}

// Global cache instance
const templateCache = new TemplateCache();

/**
 * Marker that stores capture metadata on pattern AST nodes.
 * This avoids the need to parse capture names from identifiers during matching.
 */
class CaptureMarker implements Marker {
    readonly kind = 'org.openrewrite.javascript.CaptureMarker';
    readonly id = randomId();

    constructor(
        public readonly captureName: string,
        public readonly variadicOptions?: VariadicOptions
    ) {
    }
}

/**
 * A comparator for pattern matching that is lenient about optional properties.
 * Allows patterns without type annotations to match actual code with type annotations.
 * Uses semantic comparison to match semantically equivalent code (e.g., isDate() and util.isDate()).
 */
class PatternMatchingComparator extends JavaScriptSemanticComparatorVisitor {
    constructor(
        private readonly matcher: {
            handleCapture: (pattern: J, target: J) => boolean;
            handleVariadicCapture: (pattern: J, targets: J[]) => boolean;
        },
        lenientTypeMatching: boolean = true
    ) {
        // Enable lenient type matching based on pattern configuration (default: true for backward compatibility)
        super(lenientTypeMatching);
    }

    override async visit<R extends J>(j: Tree, p: J, parent?: Cursor): Promise<R | undefined> {
        // Check if the pattern node is a capture - this handles captures anywhere in the tree
        if (PlaceholderUtils.isCapture(j as J)) {
            const success = this.matcher.handleCapture(j as J, p);
            if (!success) {
                return this.abort(j) as R;
            }
            return j as R;
        }

        if (!this.match) {
            return j as R;
        }

        return super.visit(j, p, parent);
    }

    protected hasSameKind(j: J, other: J): boolean {
        return super.hasSameKind(j, other) ||
               (j.kind == J.Kind.Identifier && PlaceholderUtils.isCapture(j as J.Identifier));
    }

    override async visitIdentifier(identifier: J.Identifier, other: J): Promise<J | undefined> {
        if (PlaceholderUtils.isCapture(identifier)) {
            const success = this.matcher.handleCapture(identifier, other);
            return success ? identifier : this.abort(identifier);
        }
        return super.visitIdentifier(identifier, other);
    }

    override async visitMethodInvocation(methodInvocation: J.MethodInvocation, other: J): Promise<J | undefined> {
        // Check if any arguments are variadic captures
        const hasVariadicCapture = methodInvocation.arguments.elements.some(arg =>
            PlaceholderUtils.isVariadicCapture(arg.element)
        );

        // If no variadic captures, use parent implementation (which includes semantic/type-aware matching)
        if (!hasVariadicCapture) {
            return super.visitMethodInvocation(methodInvocation, other);
        }

        // Otherwise, handle variadic captures ourselves
        if (!this.match || other.kind !== J.Kind.MethodInvocation) {
            return this.abort(methodInvocation);
        }

        const otherMethodInvocation = other as J.MethodInvocation;

        // Compare select
        if ((methodInvocation.select === undefined) !== (otherMethodInvocation.select === undefined)) {
            return this.abort(methodInvocation);
        }

        // Visit select if present
        if (methodInvocation.select && otherMethodInvocation.select) {
            await this.visit(methodInvocation.select.element, otherMethodInvocation.select.element);
            if (!this.match) return methodInvocation;
        }

        // Compare typeParameters
        if ((methodInvocation.typeParameters === undefined) !== (otherMethodInvocation.typeParameters === undefined)) {
            return this.abort(methodInvocation);
        }

        // Visit typeParameters if present
        if (methodInvocation.typeParameters && otherMethodInvocation.typeParameters) {
            if (methodInvocation.typeParameters.elements.length !== otherMethodInvocation.typeParameters.elements.length) {
                return this.abort(methodInvocation);
            }

            // Visit each type parameter in lock step
            for (let i = 0; i < methodInvocation.typeParameters.elements.length; i++) {
                await this.visit(methodInvocation.typeParameters.elements[i].element, otherMethodInvocation.typeParameters.elements[i].element);
                if (!this.match) return methodInvocation;
            }
        }

        // Visit name
        await this.visit(methodInvocation.name, otherMethodInvocation.name);
        if (!this.match) return methodInvocation;

        // Special handling for variadic captures in arguments
        if (!await this.matchArguments(methodInvocation.arguments.elements, otherMethodInvocation.arguments.elements)) {
            return this.abort(methodInvocation);
        }

        return methodInvocation;
    }

    /**
     * Matches argument lists, with special handling for variadic captures.
     * A variadic capture can match zero or more consecutive arguments.
     */
    private async matchArguments(patternArgs: any[], targetArgs: any[]): Promise<boolean> {
        let patternIdx = 0;
        let targetIdx = 0;

        while (patternIdx < patternArgs.length && targetIdx <= targetArgs.length) {
            const patternArg = patternArgs[patternIdx].element;

            // Check if this pattern argument is a variadic capture
            if (PlaceholderUtils.isVariadicCapture(patternArg)) {
                const variadicOptions = PlaceholderUtils.getVariadicOptions(patternArg);

                // Calculate how many target arguments this variadic should consume
                const remainingPatternArgs = patternArgs.length - patternIdx - 1;
                const remainingTargetArgs = targetArgs.length - targetIdx;
                const maxConsume = remainingTargetArgs - remainingPatternArgs;

                // Check min/max constraints
                const min = variadicOptions?.min ?? 0;
                const max = variadicOptions?.max ?? Infinity;

                if (maxConsume < min || maxConsume > max) {
                    return false;
                }

                // Capture all arguments for this variadic
                // Filter out J.Empty elements as they represent zero arguments
                const capturedArgs: J[] = [];
                for (let i = 0; i < maxConsume; i++) {
                    const arg = targetArgs[targetIdx + i].element;
                    // Skip J.Empty as it represents an empty argument list, not an actual argument
                    if (arg.kind !== J.Kind.Empty) {
                        capturedArgs.push(arg);
                    }
                }

                // Re-check min/max constraints against actual captured arguments (after filtering J.Empty)
                if (capturedArgs.length < min || capturedArgs.length > max) {
                    return false;
                }

                // Handle the variadic capture
                const success = this.matcher.handleVariadicCapture(patternArg, capturedArgs);
                if (!success) {
                    return false;
                }

                targetIdx += maxConsume;
                patternIdx++;
            } else {
                // Regular non-variadic argument - must match exactly one target argument
                if (targetIdx >= targetArgs.length) {
                    return false; // Pattern has more args than target
                }

                const targetArg = targetArgs[targetIdx].element;

                // J.Empty represents no argument, so regular captures should not match it
                if (targetArg.kind === J.Kind.Empty) {
                    return false;
                }

                await this.visit(patternArg, targetArg);
                if (!this.match) {
                    return false;
                }

                patternIdx++;
                targetIdx++;
            }
        }

        // Ensure we've consumed all arguments from both pattern and target
        return patternIdx === patternArgs.length && targetIdx === targetArgs.length;
    }
}

/**
 * Options for variadic captures that match zero or more nodes in a sequence.
 */
export interface VariadicOptions {
    /**
     * Separator used when expanding the capture in templates (default: ', ').
     * Examples: ', ' for arguments, '; ' for statements.
     */
    separator?: string;

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
 * Configuration options for captures.
 */

/**
 * Options for the capture function.
 *
 * The constraint function receives different parameter types depending on whether
 * the capture is variadic:
 * - For regular captures: constraint receives a single node of type T
 * - For variadic captures: constraint receives an array of nodes of type T[]
 *
 * Note: The constraint parameter is typed as 'any' to avoid TypeScript union type
 * issues when accessing properties. Type safety is enforced through function overloads.
 * Users should ensure their constraint functions match the variadic setting at runtime.
 */
export interface CaptureOptions<T = any> {
    variadic?: boolean | VariadicOptions;
    constraint?: (node: any) => boolean;
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

// Symbol to access the internal capture name without triggering Proxy
const CAPTURE_NAME_SYMBOL = Symbol('captureName');
// Symbol to access variadic options without triggering Proxy
const CAPTURE_VARIADIC_SYMBOL = Symbol('captureVariadic');
// Symbol to access constraint function without triggering Proxy
const CAPTURE_CONSTRAINT_SYMBOL = Symbol('captureConstraint');

class CaptureImpl<T = any> implements Capture<T> {
    public readonly name: string;
    [CAPTURE_NAME_SYMBOL]: string;
    [CAPTURE_VARIADIC_SYMBOL]: VariadicOptions | undefined;
    [CAPTURE_CONSTRAINT_SYMBOL]: ((node: T) => boolean) | undefined;

    constructor(name: string, options?: CaptureOptions<T>) {
        this.name = name;
        this[CAPTURE_NAME_SYMBOL] = name;

        // Normalize variadic options
        if (options?.variadic) {
            if (typeof options.variadic === 'boolean') {
                this[CAPTURE_VARIADIC_SYMBOL] = { separator: ', ' };
            } else {
                this[CAPTURE_VARIADIC_SYMBOL] = {
                    separator: options.variadic.separator || ', ',
                    min: options.variadic.min,
                    max: options.variadic.max
                };
            }
        }

        // Store constraint if provided
        if (options?.constraint) {
            this[CAPTURE_CONSTRAINT_SYMBOL] = options.constraint;
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

class TemplateParamImpl<T = any> implements TemplateParam<T> {
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
class CaptureValue {
    constructor(
        public readonly rootCapture: Capture,
        public readonly propertyPath: string[],
        public readonly arrayOperation?: { type: 'index' | 'slice' | 'length'; args?: any[] }
    ) {}

    /**
     * Resolves this capture value by looking up the root capture in the values map
     * and navigating through the property path.
     */
    resolve(values: Pick<Map<string, J | J[]>, 'get'>): any {
        const rootName = (this.rootCapture as any)[CAPTURE_NAME_SYMBOL] || this.rootCapture.getName();
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
            if (current === null || current === undefined) {
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
 * Creates a capture specification for use in template patterns.
 *
 * @template T The expected type of the captured AST node (for TypeScript autocomplete only)
 * @param name Optional name for the capture. If not provided, an auto-generated name is used.
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
// Overload 1: Regular capture with constraint (most specific - no variadic property)
export function capture<T = any>(
    name: string,
    options: { constraint: (node: T) => boolean } & { variadic?: never }
): Capture<T> & T;

// Overload 2: Variadic capture with name (explicitly requires variadic property)
export function capture<T = any>(
    name: string,
    options: { variadic: true | VariadicOptions; constraint?: (nodes: T[]) => boolean; separator?: string; min?: number; max?: number }
): Capture<T[]> & T[];

// Overload 3: Variadic capture without name (explicitly requires variadic property)
export function capture<T = any>(
    name: undefined,
    options: { variadic: true | VariadicOptions; constraint?: (nodes: T[]) => boolean; separator?: string; min?: number; max?: number }
): Capture<T[]> & T[];

// Overload 4: Catch-all for simple captures without special options
export function capture<T = any>(
    name?: string,
    options?: CaptureOptions<T>
): Capture<T> & T;

// Implementation
export function capture<T = any>(name?: string, options?: CaptureOptions<T>): Capture<T> & T {
    const captureName = name || `unnamed_${capture.nextUnnamedId++}`;
    const impl = new CaptureImpl<T>(captureName, options);

    // Return a Proxy that intercepts property accesses and creates CaptureValues
    return new Proxy(impl as any, {
        get(target: any, prop: string | symbol): any {
            // Allow access to internal symbols
            if (prop === CAPTURE_NAME_SYMBOL) {
                return target[CAPTURE_NAME_SYMBOL];
            }
            if (prop === CAPTURE_VARIADIC_SYMBOL) {
                return target[CAPTURE_VARIADIC_SYMBOL];
            }

            // Allow access to internal constraint symbol
            if (prop === CAPTURE_CONSTRAINT_SYMBOL) {
                return target[CAPTURE_CONSTRAINT_SYMBOL];
            }

            // Allow methods to be called directly on the target
            if (prop === 'getName' || prop === 'isVariadic' || prop === 'getVariadicOptions' || prop === 'getConstraint') {
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

/**
 * Creates a Proxy around a CaptureValue that allows further property access.
 * This enables chaining like method.name.simpleName
 */
function createCaptureValueProxy(
    rootCapture: Capture,
    propertyPath: string[],
    arrayOperation?: { type: 'index' | 'slice' | 'length'; args?: any[] }
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

// Static counter for generating unique IDs for unnamed captures
capture.nextUnnamedId = 1;

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
    private captures: Capture[] = [];

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
     * @param value The capture object or string name
     * @returns This builder for chaining
     */
    capture(value: Capture | string): this {
        // Ensure we have a part for after this capture
        if (this.parts.length === 0) {
            this.parts.push('');
        }
        // Convert string to Capture if needed
        const captureObj = typeof value === 'string' ? new CaptureImpl(value) : value;
        this.captures.push(captureObj);
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
     * @param captures The captures between the string parts
     */
    constructor(
        public readonly templateParts: TemplateStringsArray,
        public readonly captures: Capture[]
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
     *         imports: ['import { isDate } from "util"'],
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
        return success ? new MatchResult(matcher.getAll()) : undefined;
    }
}

export class MatchResult implements Pick<Map<string, J>, "get"> {
    constructor(
        private readonly bindings: Map<string, J> = new Map()
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
        return this.bindings.get(name);
    }
}

/**
 * Matcher for checking if a pattern matches an AST node and extracting captured nodes.
 */
class Matcher {
    private readonly bindings = new Map<string, J>();
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
     * Gets all captured nodes.
     *
     * @returns A map of capture names to captured nodes
     */
    getAll(): Map<string, J> {
        return new Map(this.bindings);
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
            handleVariadicCapture: (p, ts) => this.handleVariadicCapture(p, ts)
        }, lenientTypeMatching);
        return await comparator.compare(pattern, target);
    }

    /**
     * Handles a capture placeholder.
     *
     * @param pattern The pattern node
     * @param target The target node
     * @returns true if the capture is successful, false otherwise
     */
    private handleCapture(pattern: J, target: J): boolean {
        const captureName = PlaceholderUtils.getCaptureName(pattern);

        if (!captureName) {
            return false;
        }

        // Find the original capture object to get constraint
        const captureObj = this.pattern.captures.find(c => c.getName() === captureName);
        const constraint = captureObj?.getConstraint?.();

        // Apply constraint if present
        if (constraint && !constraint(target as any)) {
            return false;
        }

        // Store the binding
        this.bindings.set(captureName, target);
        return true;
    }

    /**
     * Handles a variadic capture placeholder.
     *
     * @param pattern The pattern node (the variadic capture)
     * @param targets The target nodes that were matched
     * @returns true if the capture is successful, false otherwise
     */
    private handleVariadicCapture(pattern: J, targets: J[]): boolean {
        const captureName = PlaceholderUtils.getCaptureName(pattern);

        if (!captureName) {
            return false;
        }

        // Find the original capture object to get constraint
        const captureObj = this.pattern.captures.find(c => c.getName() === captureName);
        const constraint = captureObj?.getConstraint?.();

        // Apply constraint if present - for variadic captures, constraint receives the array
        if (constraint && !constraint(targets as any)) {
            return false;
        }

        // For variadic captures, store the array of matched nodes
        this.bindings.set(captureName, targets as any);
        return true;
    }
}

/**
 * Tagged template function for creating patterns.
 *
 * @param strings The string parts of the template
 * @param captures The captures between the string parts
 * @returns A Pattern object
 *
 * @example
 * // Using the same capture multiple times for repeated patterns
 * const expr = capture('expr');
 * const redundantOr = pattern`${expr} || ${expr}`;
 */
export function pattern(strings: TemplateStringsArray, ...captures: (Capture | string)[]): Pattern {
    const capturesByName = captures.reduce((map, c) => {
        const capture = typeof c === "string" ? new CaptureImpl(c) : c;
        // Use symbol to get internal name without triggering Proxy
        const name = (capture as any)[CAPTURE_NAME_SYMBOL] || capture.getName();
        return map.set(name, capture);
    }, new Map<string, Capture>());
    return new Pattern(strings, captures.map(c => {
        // Use symbol to get internal name without triggering Proxy
        const name = typeof c === "string" ? c : ((c as any)[CAPTURE_NAME_SYMBOL] || c.getName());
        return capturesByName.get(name)!;
    }));
}

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
 * Valid parameter types for template literals.
 * - Capture: For pattern matching and reuse
 * - CaptureValue: Result of property access or array operations on captures (e.g., capture.prop, capture[0], capture.slice(1))
 * - Tree: AST nodes to be inserted directly
 * - Tree[]: Arrays of AST nodes (from variadic capture operations like slice)
 * - Primitives: Values to be converted to literals
 */
export type TemplateParameter = Capture | CaptureValue | TemplateParam | Tree | Tree[] | string | number | boolean;

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
     *         imports: ['import { isDate } from "util"'],
     *         dependencies: { 'util': '^1.0.0' }
     *     })
     */
    configure(options: TemplateOptions): Template {
        this.options = { ...this.options, ...options };
        return this;
    }

    /**
     * Applies this template and returns the resulting tree.
     *
     * @param cursor The cursor pointing to the current location in the AST
     * @param tree Input tree
     * @param values values for parameters in template
     * @returns A Promise resolving to the generated AST node
     */
    async apply(cursor: Cursor, tree: J, values?: Map<Capture | string, J> | Pick<Map<string, J>, 'get'>): Promise<J | undefined> {
        // Normalize the values map: convert any Capture keys to string keys
        let normalizedValues: Pick<Map<string, J>, 'get'> | undefined;
        if (values instanceof Map) {
            const normalized = new Map<string, J>();
            for (const [key, value] of values.entries()) {
                const stringKey = typeof key === 'string'
                    ? key
                    : ((key as any)[CAPTURE_NAME_SYMBOL] || key.getName());
                normalized.set(stringKey, value);
            }
            normalizedValues = normalized;
        } else {
            // If it's Pick<Map> (like MatchResult), it already uses string keys
            normalizedValues = values;
        }

        // Prefer 'context' over deprecated 'imports'
        const contextStatements = this.options.context || this.options.imports || [];
        return TemplateEngine.applyTemplate(
            this.templateParts,
            this.parameters,
            cursor,
            {
                tree,
                mode: JavaCoordinates.Mode.Replace
            },
            normalizedValues,
            contextStatements,
            this.options.dependencies || {}
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
 * @param parameters The parameters between the string parts (Capture, Tree, or primitives)
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

/**
 * Parameter specification for template generation.
 * Represents a placeholder in a template that will be replaced with a parameter value.
 */
interface Parameter {
    /**
     * The value to substitute into the template.
     */
    value: any;
}

/**
 * Internal template engine - handles the core templating logic.
 * Not exported, so only visible within this module.
 */
class TemplateEngine {
    /**
     * Applies a template with optional match results from pattern matching.
     *
     * @param templateParts The string parts of the template
     * @param parameters The parameters between the string parts
     * @param cursor The cursor pointing to the current location in the AST
     * @param coordinates The coordinates specifying where and how to insert the generated AST
     * @param values Map of capture names to values to replace the parameters with
     * @param contextStatements Context declarations (imports, types, etc.) to prepend for type attribution
     * @param dependencies NPM dependencies for type attribution
     * @returns A Promise resolving to the generated AST node
     */
    static async applyTemplate(
        templateParts: TemplateStringsArray,
        parameters: Parameter[],
        cursor: Cursor,
        coordinates: JavaCoordinates,
        values: Pick<Map<string, J>, 'get'> = new Map(),
        contextStatements: string[] = [],
        dependencies: Record<string, string> = {}
    ): Promise<J | undefined> {
        // Build the template string with parameter placeholders
        const templateString = TemplateEngine.buildTemplateString(templateParts, parameters);

        // If the template string is empty, return undefined
        if (!templateString.trim()) {
            return undefined;
        }

        // Use cache to get or parse the compilation unit
        // For templates, we don't have captures, so use empty array
        const cu = await templateCache.getOrParse(
            templateString,
            [], // templates don't have captures in the cache key
            contextStatements,
            dependencies
        );

        // Check if there are any statements
        if (!cu.statements || cu.statements.length === 0) {
            return undefined;
        }

        // Skip context statements to get to the actual template code
        const templateStatementIndex = contextStatements.length;
        if (templateStatementIndex >= cu.statements.length) {
            return undefined;
        }

        // Extract the relevant part of the AST
        const firstStatement = cu.statements[templateStatementIndex].element;
        let extracted = firstStatement.kind === JS.Kind.ExpressionStatement ?
            (firstStatement as JS.ExpressionStatement).expression :
            firstStatement;

        // Create a copy to avoid sharing cached AST instances
        const ast = produce(extracted, draft => {});

        // Create substitutions map for placeholders
        const substitutions = new Map<string, Parameter>();
        for (let i = 0; i < parameters.length; i++) {
            const placeholder = `${PlaceholderUtils.PLACEHOLDER_PREFIX}${i}__`;
            substitutions.set(placeholder, parameters[i]);
        }

        // Unsubstitute placeholders with actual parameter values and match results
        const visitor = new PlaceholderReplacementVisitor(substitutions, values);
        const unsubstitutedAst = (await visitor.visit(ast, null))!;

        // Apply the template to the current AST
        return new TemplateApplier(cursor, coordinates, unsubstitutedAst, parameters).apply();
    }

    /**
     * Builds a template string with parameter placeholders.
     *
     * @param templateParts The string parts of the template
     * @param parameters The parameters between the string parts
     * @returns The template string
     */
    private static buildTemplateString(
        templateParts: TemplateStringsArray,
        parameters: Parameter[]
    ): string {
        let result = '';
        for (let i = 0; i < templateParts.length; i++) {
            result += templateParts[i];
            if (i < parameters.length) {
                const param = parameters[i].value;
                // Use a placeholder for Captures, TemplateParams, CaptureValues, Tree nodes, and Tree arrays
                // Inline everything else (strings, numbers, booleans) directly
                // Check for Capture (could be a Proxy, so check for symbol property)
                const isCapture = param instanceof CaptureImpl ||
                                (param && typeof param === 'object' && param[CAPTURE_NAME_SYMBOL]);
                const isTemplateParam = param instanceof TemplateParamImpl;
                const isCaptureValue = param instanceof CaptureValue;
                const isTreeArray = Array.isArray(param) && param.length > 0 && isTree(param[0]);
                if (isCapture || isTemplateParam || isCaptureValue || isTree(param) || isTreeArray) {
                    const placeholder = `${PlaceholderUtils.PLACEHOLDER_PREFIX}${i}__`;
                    result += placeholder;
                } else {
                    result += param;
                }
            }
        }
        return result;
    }
}

/**
 * Utility class for managing placeholder naming and parsing.
 * Centralizes all logic related to capture placeholders.
 */
class PlaceholderUtils {
    static readonly CAPTURE_PREFIX = '__capture_';
    static readonly PLACEHOLDER_PREFIX = '__PLACEHOLDER_';

    /**
     * Checks if a node is a capture placeholder.
     *
     * @param node The node to check
     * @returns true if the node is a capture placeholder, false otherwise
     */
    static isCapture(node: J): boolean {
        // Check for CaptureMarker first (efficient)
        for (const marker of node.markers.markers) {
            if (marker instanceof CaptureMarker) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the capture name from a node with a CaptureMarker.
     *
     * @param node The node to extract capture name from
     * @returns The capture name, or null if not a capture
     */
    static getCaptureName(node: J): string | undefined {
        // Check for CaptureMarker
        for (const marker of node.markers.markers) {
            if (marker instanceof CaptureMarker) {
                return marker.captureName;
            }
        }

        return undefined;
    }

    /**
     * Parses a capture placeholder to extract name and type constraint.
     *
     * @param identifier The identifier string to parse
     * @returns Object with name and optional type constraint, or null if not a valid capture
     */
    static parseCapture(identifier: string): { name: string; typeConstraint?: string } | null {
        if (!identifier.startsWith(this.CAPTURE_PREFIX)) {
            return null;
        }

        // Handle unnamed captures: "__capture_unnamed_N__"
        if (identifier.startsWith(`${this.CAPTURE_PREFIX}unnamed_`)) {
            const match = identifier.match(/__capture_(unnamed_\d+)__/);
            return match ? {name: match[1]} : null;
        }

        // Handle named captures: "__capture_name__" or "__capture_name_type__"
        const match = identifier.match(/__capture_([^_]+)(?:_([^_]+))?__/);
        if (!match) {
            return null;
        }

        return {
            name: match[1],
            typeConstraint: match[2]
        };
    }

    /**
     * Creates a capture placeholder string.
     *
     * @param name The capture name
     * @param typeConstraint Optional type constraint
     * @returns The formatted placeholder string
     */
    static createCapture(name: string, typeConstraint?: string): string {
        return typeConstraint
            ? `${this.CAPTURE_PREFIX}${name}_${typeConstraint}__`
            : `${this.CAPTURE_PREFIX}${name}__`;
    }

    /**
     * Checks if a capture marker indicates a variadic capture.
     *
     * @param node The node to check
     * @returns true if the node has a variadic CaptureMarker, false otherwise
     */
    static isVariadicCapture(node: J): boolean {
        for (const marker of node.markers.markers) {
            if (marker instanceof CaptureMarker && marker.variadicOptions) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the variadic options from a capture marker.
     *
     * @param node The node to extract variadic options from
     * @returns The VariadicOptions, or undefined if not a variadic capture
     */
    static getVariadicOptions(node: J): VariadicOptions | undefined {
        for (const marker of node.markers.markers) {
            if (marker instanceof CaptureMarker) {
                return marker.variadicOptions;
            }
        }
        return undefined;
    }
}

/**
 * Visitor that replaces placeholder nodes with actual parameter values.
 */
class PlaceholderReplacementVisitor extends JavaScriptVisitor<any> {
    constructor(
        private readonly substitutions: Map<string, Parameter>,
        private readonly values: Pick<Map<string, J | J[]>, 'get'> = new Map()
    ) {
        super();
    }

    /**
     * Merges prefixes by preserving comments from the source element
     * while using whitespace from the template placeholder.
     *
     * @param sourcePrefix The prefix from the captured element (may contain comments)
     * @param templatePrefix The prefix from the template placeholder (defines whitespace)
     * @returns A merged prefix with source comments and template whitespace
     */
    private mergePrefix(sourcePrefix: J.Space, templatePrefix: J.Space): J.Space {
        // If source has no comments, just use template prefix
        if (sourcePrefix.comments.length === 0) {
            return templatePrefix;
        }

        // Preserve comments from source, use whitespace from template
        return {
            kind: J.Kind.Space,
            comments: sourcePrefix.comments,
            whitespace: templatePrefix.whitespace
        };
    }

    async visit<R extends J>(tree: J, p: any, parent?: Cursor): Promise<R | undefined> {
        // Check if this node is a placeholder
        if (this.isPlaceholder(tree)) {
            const replacement = this.replacePlaceholder(tree);
            if (replacement !== tree) {
                return replacement as R;
            }
        }

        // Continue with normal traversal
        return super.visit(tree, p, parent);
    }

    override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
        // First, check if any argument is a variadic placeholder or a CaptureValue that resolves to an array
        const hasVariadicPlaceholder = method.arguments.elements.some(arg => {
            const argElement = arg.element;
            if (!this.isPlaceholder(argElement)) {
                return false;
            }
            const placeholderText = this.getPlaceholderText(argElement);
            if (!placeholderText) {
                return false;
            }
            const param = this.substitutions.get(placeholderText);
            if (!param) {
                return false;
            }

            // Check if it's a direct Tree[] array
            if (Array.isArray(param.value)) {
                return true;
            }

            // Check if it's a CaptureValue that will expand to an array
            // Note: CaptureValue might be wrapped in a Proxy, so check for method existence
            if (param.value instanceof CaptureValue &&
                typeof param.value.isArrayExpansion === 'function' &&
                param.value.isArrayExpansion()) {
                return true;
            }

            // Check if this is a variadic capture
            const isCapture = param.value instanceof CaptureImpl ||
                             (param.value && typeof param.value === 'object' && param.value[CAPTURE_NAME_SYMBOL]);
            if (isCapture) {
                const name = param.value[CAPTURE_NAME_SYMBOL] || param.value.name;
                const capture = Array.from(this.substitutions.values())
                    .map(p => p.value)
                    .find(v => v instanceof CaptureImpl && v.getName() === name) as CaptureImpl | undefined;
                return capture?.isVariadic() || false;
            }
            return false;
        });

        if (!hasVariadicPlaceholder) {
            // No variadic placeholders, use standard traversal
            return super.visitMethodInvocation(method, p);
        }

        // We have variadic placeholders - need to expand them
        const newArguments: any[] = [];

        for (const arg of method.arguments.elements) {
            const argElement = arg.element;

            // Check if this argument is a variadic placeholder
            if (this.isPlaceholder(argElement)) {
                const placeholderText = this.getPlaceholderText(argElement);
                if (placeholderText) {
                    const param = this.substitutions.get(placeholderText);
                    if (param) {
                        let arrayToExpand: J[] | undefined = undefined;

                        // Check if it's a direct Tree[] array
                        if (Array.isArray(param.value)) {
                            arrayToExpand = param.value as J[];
                        }
                        // Check if it's a CaptureValue (e.g., from .slice() or array index)
                        else if (param.value instanceof CaptureValue) {
                            const resolved = param.value.resolve(this.values);
                            // If resolved value is an array, expand it
                            if (Array.isArray(resolved)) {
                                arrayToExpand = resolved;
                            }
                        }
                        // Check if it's a direct variadic capture
                        else {
                            const isCapture = param.value instanceof CaptureImpl ||
                                             (param.value && typeof param.value === 'object' && param.value[CAPTURE_NAME_SYMBOL]);
                            if (isCapture) {
                                const name = param.value[CAPTURE_NAME_SYMBOL] || param.value.name;
                                const capture = Array.from(this.substitutions.values())
                                    .map(p => p.value)
                                    .find(v => v instanceof CaptureImpl && v.getName() === name) as CaptureImpl | undefined;

                                if (capture?.isVariadic()) {
                                    // Get the matched array
                                    const matchedArray = this.values.get(name);
                                    if (Array.isArray(matchedArray)) {
                                        arrayToExpand = matchedArray;
                                    }
                                }
                            }
                        }

                        // Expand the array if we found one
                        if (arrayToExpand) {
                            if (arrayToExpand.length > 0) {
                                // Expand the array into multiple arguments
                                for (let i = 0; i < arrayToExpand.length; i++) {
                                    const item = arrayToExpand[i];
                                    newArguments.push(produce(arg, (draft: any) => {
                                        draft.element = produce(item, (itemDraft: any) => {
                                            // First item gets merged prefix (placeholder whitespace + item comments)
                                            // Subsequent items keep their original prefix
                                            if (i === 0) {
                                                itemDraft.prefix = this.mergePrefix(item.prefix, argElement.prefix);
                                            } else {
                                                // Subsequent items preserve their prefix (including comments)
                                                itemDraft.prefix = item.prefix;
                                            }
                                            itemDraft.markers = argElement.markers;
                                        });
                                        // All but the last argument need comma-space in 'after'
                                        // We keep the 'after' from the original arg structure for proper comma handling
                                    }));
                                }
                                continue; // Skip adding the placeholder itself
                            } else {
                                // Empty array - don't add any arguments
                                continue;
                            }
                        }
                    }
                }
            }

            // Not a variadic placeholder (or expansion failed) - process normally
            const replacedElement = await this.visit(argElement, p);
            if (replacedElement) {
                newArguments.push(produce(arg, (draft: any) => {
                    draft.element = replacedElement;
                }));
            }
        }

        // Create new method invocation with expanded arguments
        return produce(method, (draft: any) => {
            draft.arguments.elements = newArguments;
        });
    }

    /**
     * Checks if a node is a placeholder.
     *
     * @param node The node to check
     * @returns True if the node is a placeholder
     */
    private isPlaceholder(node: J): boolean {
        if (node.kind === J.Kind.Identifier) {
            const identifier = node as J.Identifier;
            return identifier.simpleName.startsWith(PlaceholderUtils.PLACEHOLDER_PREFIX);
        } else if (node.kind === J.Kind.Literal) {
            const literal = node as J.Literal;
            return literal.valueSource?.startsWith(PlaceholderUtils.PLACEHOLDER_PREFIX) || false;
        }
        return false;
    }

    /**
     * Replaces a placeholder node with the actual parameter value.
     *
     * @param placeholder The placeholder node
     * @returns The replacement node or the original if not a placeholder
     */
    private replacePlaceholder(placeholder: J): J {
        const placeholderText = this.getPlaceholderText(placeholder);

        if (!placeholderText || !placeholderText.startsWith(PlaceholderUtils.PLACEHOLDER_PREFIX)) {
            return placeholder;
        }

        // Find the corresponding parameter
        const param = this.substitutions.get(placeholderText);
        if (!param || param.value === undefined) {
            return placeholder;
        }

        // Check if the parameter value is a CaptureValue
        const isCaptureValue = param.value instanceof CaptureValue;

        if (isCaptureValue) {
            // Resolve the capture value to get the actual property value
            const propertyValue = param.value.resolve(this.values);

            if (propertyValue !== undefined) {
                // If the property value is already a J node, use it
                if (isTree(propertyValue)) {
                    const propValueAsJ = propertyValue as J;
                    return produce(propValueAsJ, draft => {
                        draft.markers = placeholder.markers;
                        draft.prefix = this.mergePrefix(propValueAsJ.prefix, placeholder.prefix);
                    });
                }
                // If it's a primitive value and placeholder is an identifier, update the simpleName
                if (typeof propertyValue === 'string' && placeholder.kind === J.Kind.Identifier) {
                    return produce(placeholder as J.Identifier, draft => {
                        draft.simpleName = propertyValue;
                    });
                }
                // If it's a primitive value and placeholder is a literal, update the value
                if (typeof propertyValue === 'string' && placeholder.kind === J.Kind.Literal) {
                    return produce(placeholder as J.Literal, draft => {
                        draft.value = propertyValue;
                        draft.valueSource = `"${propertyValue}"`;
                    });
                }
            }

            // If no match found or unhandled type, return placeholder unchanged
            return placeholder;
        }

        // Check if the parameter value is a Capture (could be a Proxy) or TemplateParam
        const isCapture = param.value instanceof CaptureImpl ||
                         (param.value && typeof param.value === 'object' && param.value[CAPTURE_NAME_SYMBOL]);
        const isTemplateParam = param.value instanceof TemplateParamImpl;

        if (isCapture || isTemplateParam) {
            // Simple capture/template param (no property path for template params)
            const name = isTemplateParam ? param.value.name :
                        (param.value[CAPTURE_NAME_SYMBOL] || param.value.name);
            const matchedNode = this.values.get(name);
            if (matchedNode && !Array.isArray(matchedNode)) {
                return produce(matchedNode, draft => {
                    draft.markers = placeholder.markers;
                    draft.prefix = this.mergePrefix(matchedNode.prefix, placeholder.prefix);
                });
            }

            // If no match found, return placeholder unchanged
            return placeholder;
        }

        // If the parameter value is an AST node, use it directly
        if (isTree(param.value)) {
            // Return the AST node, preserving comments from the source
            return produce(param.value as J, draft => {
                draft.markers = placeholder.markers;
                draft.prefix = this.mergePrefix(param.value.prefix, placeholder.prefix);
            });
        }

        return placeholder;
    }

    /**
     * Gets the placeholder text from a node.
     *
     * @param node The node to get placeholder text from
     * @returns The placeholder text or null
     */
    private getPlaceholderText(node: J): string | null {
        if (node.kind === J.Kind.Identifier) {
            return (node as J.Identifier).simpleName;
        } else if (node.kind === J.Kind.Literal) {
            return (node as J.Literal).valueSource || null;
        }
        return null;
    }

}

/**
 * Helper class for applying a template to an AST.
 */
class TemplateApplier {
    constructor(
        private readonly cursor: Cursor,
        private readonly coordinates: JavaCoordinates,
        private readonly ast: J,
        private readonly parameters: Parameter[] = []
    ) {
    }

    /**
     * Applies the template to the current AST.
     *
     * @returns A Promise resolving to the modified AST
     */
    async apply(): Promise<J | undefined> {
        const {loc} = this.coordinates;

        // Apply the template based on the location and mode
        switch (loc || 'EXPRESSION_PREFIX') {
            case 'EXPRESSION_PREFIX':
                return this.applyToExpression();
            case 'STATEMENT_PREFIX':
                return this.applyToStatement();
            case 'BLOCK_END':
                return this.applyToBlock();
            default:
                throw new Error(`Unsupported location: ${loc}`);
        }
    }

    /**
     * Applies the template to an expression.
     *
     * @returns A Promise resolving to the modified AST
     */
    private async applyToExpression(): Promise<J | undefined> {
        const {tree} = this.coordinates;

        // Create a copy of the AST with the prefix from the target
        return tree ? produce(this.ast, draft => {
            draft.prefix = (tree as J).prefix;
        }) : this.ast;
    }

    /**
     * Applies the template to a statement.
     *
     * @returns A Promise resolving to the modified AST
     */
    private async applyToStatement(): Promise<J | undefined> {
        const {tree} = this.coordinates;

        // Create a copy of the AST with the prefix from the target
        return produce(this.ast, draft => {
            draft.prefix = (tree as J).prefix;
        });
    }

    /**
     * Applies the template to a block.
     *
     * @returns A Promise resolving to the modified AST
     */
    private async applyToBlock(): Promise<J | undefined> {
        const {tree} = this.coordinates;

        // Create a copy of the AST with the prefix from the target
        return produce(this.ast, draft => {
            draft.prefix = (tree as J).prefix;
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
     * @param captures The captures between the string parts
     * @param contextStatements Context declarations (imports, types, etc.) to prepend for type attribution
     * @param dependencies NPM dependencies for type attribution
     */
    constructor(
        private readonly templateParts: TemplateStringsArray,
        private readonly captures: Capture[],
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
        // Combine template parts and placeholders
        const templateString = this.buildTemplateString();

        // Use cache to get or parse the compilation unit
        const cu = await templateCache.getOrParse(
            templateString,
            this.captures,
            this.contextStatements,
            this.dependencies
        );

        // Extract the relevant part of the AST
        return this.extractPatternFromAst(cu);
    }

    /**
     * Builds a template string with placeholders for captures.
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
                result += PlaceholderUtils.createCapture(captureName);
            }
        }
        return result;
    }

    /**
     * Extracts the pattern from the parsed AST.
     *
     * @param cu The compilation unit
     * @returns The extracted pattern
     */
    private extractPatternFromAst(cu: JS.CompilationUnit): J {
        // Skip context statements to get to the actual pattern code
        const patternStatementIndex = this.contextStatements.length;

        // Extract the relevant part of the AST based on the template content
        const firstStatement = cu.statements[patternStatementIndex].element;

        let extracted: J;
        // If the first statement is an expression statement, extract the expression
        if (firstStatement.kind === JS.Kind.ExpressionStatement) {
            extracted = (firstStatement as JS.ExpressionStatement).expression;
        } else {
            // Otherwise, return the statement itself
            extracted = firstStatement;
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
        const visited = new Set<any>();
        return produce(ast, draft => {
            this.visitAndAttachMarkers(draft, visited);
        });
    }

    /**
     * Recursively visits AST nodes and attaches CaptureMarkers to capture identifiers.
     *
     * @param node The node to visit
     * @param visited Set of already visited nodes to avoid cycles
     */
    private visitAndAttachMarkers(node: any, visited: Set<any>): void {
        if (!node || typeof node !== 'object' || visited.has(node)) {
            return;
        }

        // Mark as visited to avoid cycles
        visited.add(node);

        // If this is an identifier that looks like a capture, attach a marker
        if (node.kind === J.Kind.Identifier && node.simpleName?.startsWith(PlaceholderUtils.CAPTURE_PREFIX)) {
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
    before: Pattern | Pattern[];
    after: Template;
}

/**
 * Implementation of a replacement rule.
 */
class RewriteRuleImpl implements RewriteRule {
    constructor(
        private readonly before: Pattern[],
        private readonly after: Template
    ) {
    }

    async tryOn(cursor: Cursor, node: J): Promise<J | undefined> {
        for (const pattern of this.before) {
            const match = await pattern.match(node);
            if (match) {
                const result = await this.after.apply(cursor, node, match);
                if (result) {
                    return result;
                }
            }
        }

        // Return undefined if no patterns match
        return undefined;
    }
}

/**
 * Creates a replacement rule using a capture context and configuration.
 *
 * @param builderFn Function that takes a capture context and returns before/after configuration
 * @returns A replacement rule that can be applied to AST nodes
 *
 * @example
 * // Single pattern
 * const swapOperands = rewrite(() => ({
 *     before: pattern`${"left"} + ${"right"}`,
 *     after: template`${"right"} + ${"left"}`
 * }));
 *
 * @example
 * // Multiple patterns
 * const normalizeComparisons = rewrite(() => ({
 *     before: [
 *         pattern`${"left"} == ${"right"}`,
 *         pattern`${"left"} === ${"right"}`
 *     ],
 *     after: template`${"left"} === ${"right"}`
 * }));
 *
 * @example
 * // Using in a visitor - IMPORTANT: use `|| node` to handle undefined when no match
 * class MyVisitor extends JavaScriptVisitor<any> {
 *     override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
 *         const rule = rewrite(() => ({
 *             before: pattern`${capture('a')} + ${capture('b')}`,
 *             after: template`${capture('b')} + ${capture('a')}`
 *         }));
 *         // tryOn() returns undefined if no pattern matches, so always use || node
 *         return await rule.tryOn(this.cursor, binary) || binary;
 *     }
 * }
 */
export function rewrite(
    builderFn: () => RewriteConfig
): RewriteRule {
    const config = builderFn();

    // Ensure we have valid before and after properties
    if (!config.before || !config.after) {
        throw new Error('Builder function must return an object with before and after properties');
    }

    return new RewriteRuleImpl(Array.isArray(config.before) ? config.before : [config.before], config.after);
}
