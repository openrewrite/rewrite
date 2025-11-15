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
import {J} from '../../java';
import {JS} from '../index';
import {Marker, Markers} from '../../markers';
import {randomId} from '../../uuid';
import {ConstraintFunction, VariadicOptions} from './types';

/**
 * Internal storage value type for pattern match captures.
 * - J: Scalar captures without wrapper (fallback)
 * - J.RightPadded<J>: Scalar captures with wrapper (preserves trailing markers like semicolons)
 * - J[]: Variadic captures without wrapper metadata
 * - J.RightPadded<J>[]: Variadic captures with wrapper metadata (preserves markers like commas)
 */
export type CaptureStorageValue = J | J.RightPadded<J> | J[] | J.RightPadded<J>[];

/**
 * Symbol to access wrappersMap without exposing it as public API
 */
export const WRAPPERS_MAP_SYMBOL = Symbol('wrappersMap');

/**
 * Shared wrapper function name used by both patterns and templates.
 * Using the same name allows cache sharing when pattern and template code is identical.
 */
export const WRAPPER_FUNCTION_NAME = '__WRAPPER__';

/**
 * Simple LRU (Least Recently Used) cache implementation using Map's insertion order.
 * JavaScript Map maintains insertion order, so the first entry is the oldest.
 *
 * Used by both Pattern and Template caching to provide bounded memory usage.
 */
export class LRUCache<K, V> {
    private cache = new Map<K, V>();

    constructor(private maxSize: number) {
    }

    get(key: K): V | undefined {
        const value = this.cache.get(key);
        if (value !== undefined) {
            // Move to end (most recently used)
            this.cache.delete(key);
            this.cache.set(key, value);
        }
        return value;
    }

    set(key: K, value: V): void {
        // Remove if exists (to update position)
        this.cache.delete(key);

        // Add to end
        this.cache.set(key, value);

        // Evict oldest if over capacity
        if (this.cache.size > this.maxSize) {
            const iterator = this.cache.keys();
            const firstEntry = iterator.next();
            if (!firstEntry.done) {
                this.cache.delete(firstEntry.value);
            }
        }
    }

    clear(): void {
        this.cache.clear();
    }
}

/**
 * Shared global LRU cache for both pattern and template ASTs.
 * When pattern and template code is identical, they share the same cached AST.
 * This mirrors JavaTemplate's unified approach in the Java implementation.
 * Bounded to 100 entries using LRU eviction.
 */
export const globalAstCache = new LRUCache<string, J>(100);

/**
 * Generates a cache key for template/pattern processing.
 * Used by both Pattern and Template for consistent cache key generation.
 *
 * @param templateParts The template string parts
 * @param itemsKey String representing the captures/parameters (comma-separated)
 * @param contextStatements Context declarations
 * @param dependencies NPM dependencies
 * @returns A cache key string
 */
export function generateCacheKey(
    templateParts: string[] | TemplateStringsArray,
    itemsKey: string,
    contextStatements: string[],
    dependencies: Record<string, string>
): string {
    return [
        Array.from(templateParts).join('|'),
        itemsKey,
        contextStatements.join(';'),
        JSON.stringify(dependencies)
    ].join('::');
}

/**
 * Marker that stores capture metadata on pattern AST nodes.
 * This avoids the need to parse capture names from identifiers during matching.
 */
export class CaptureMarker implements Marker {
    readonly kind = 'org.openrewrite.javascript.CaptureMarker';
    readonly id = randomId();

    constructor(
        public readonly captureName: string,
        public readonly variadicOptions?: VariadicOptions,
        public readonly constraint?: ConstraintFunction<any>
    ) {
    }
}

/**
 * Utility class for managing placeholder naming and parsing.
 * Centralizes all logic related to capture placeholders.
 */
export class PlaceholderUtils {
    static readonly CAPTURE_PREFIX = '__capt_';
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
     * Gets the CaptureMarker from a node, if present.
     *
     * @param node The node to check
     * @returns The CaptureMarker or undefined
     */
    static getCaptureMarker(node: { markers: Markers }): CaptureMarker | undefined {
        for (const marker of node.markers.markers) {
            if (marker instanceof CaptureMarker) {
                return marker;
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

        // Handle unnamed captures: "__capt_unnamed_N__"
        if (identifier.startsWith(`${this.CAPTURE_PREFIX}unnamed_`)) {
            const match = identifier.match(/__capt_(unnamed_\d+)__/);
            return match ? {name: match[1]} : null;
        }

        // Handle all other captures (including any()): "__capt_name__" or "__capt_name_type__"
        const match = identifier.match(/__capt_([^_]+(?:_\d+)?)(?:_([^_]+))?__/);
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
        // Always use CAPTURE_PREFIX - the capturing flag is used internally for binding behavior
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
    static isVariadicCapture(node: { markers: Markers }): boolean {
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
    static getVariadicOptions(node: { markers: Markers }): VariadicOptions | undefined {
        for (const marker of node.markers.markers) {
            if (marker instanceof CaptureMarker) {
                return marker.variadicOptions;
            }
        }
        return undefined;
    }

    /**
     * Extracts the relevant AST node from a wrapper function.
     * Used by both pattern and template processors to intelligently extract
     * code from `function __WRAPPER__() { code }` wrappers.
     *
     * @param lastStatement The last statement from the compilation unit
     * @param contextName Context name for error messages (e.g., 'Pattern', 'Template')
     * @returns The extracted AST node
     */
    static extractFromWrapper(lastStatement: J, contextName: string): J {
        let extracted: J;

        // Since we always wrap in function __WRAPPER__() { code }, look for it
        if (lastStatement.kind === J.Kind.MethodDeclaration) {
            const method = lastStatement as J.MethodDeclaration;
            if (method.name?.simpleName === WRAPPER_FUNCTION_NAME && method.body) {
                const body = method.body;

                // Intelligently extract based on what's in the function body
                if (body.statements.length === 0) {
                    throw new Error(`${contextName} function body is empty`);
                } else if (body.statements.length === 1) {
                    const stmt = body.statements[0].element;

                    // Single expression statement → extract the expression
                    if (stmt.kind === JS.Kind.ExpressionStatement) {
                        extracted = (stmt as JS.ExpressionStatement).expression;
                    }
                    // Single block statement → keep the block
                    else if (stmt.kind === J.Kind.Block) {
                        extracted = stmt;
                    }
                    // Other single statement → keep it
                    else {
                        extracted = stmt;
                    }
                } else {
                    // Multiple statements → keep the block
                    extracted = body;
                }
            } else {
                // Not our wrapper function
                extracted = lastStatement;
            }
        } else {
            // Shouldn't happen with our wrapping strategy, but handle it
            extracted = lastStatement;
        }

        return extracted;
    }
}
