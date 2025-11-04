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
import {JavaScriptParser} from '../parser';
import {DependencyWorkspace} from '../dependency-workspace';
import {Marker} from '../../markers';
import {randomId} from '../../uuid';
import {VariadicOptions, Capture, Any} from './types';

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
 * Cache for compiled templates and patterns.
 * Stores parsed ASTs to avoid expensive re-parsing and dependency resolution.
 */
export class TemplateCache {
    private cache = new Map<string, JS.CompilationUnit>();

    /**
     * Generates a cache key from template string, captures, and options.
     */
    private generateKey(
        templateString: string,
        captures: (Capture | Any<any>)[],
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
        captures: (Capture | Any<any>)[],
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
export const templateCache = new TemplateCache();

/**
 * Marker that stores capture metadata on pattern AST nodes.
 * This avoids the need to parse capture names from identifiers during matching.
 */
export class CaptureMarker implements Marker {
    readonly kind = 'org.openrewrite.javascript.CaptureMarker';
    readonly id = randomId();

    constructor(
        public readonly captureName: string,
        public readonly variadicOptions?: VariadicOptions
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
     * Gets the CaptureMarker from a node, if present.
     *
     * @param node The node to check
     * @returns The CaptureMarker or undefined
     */
    static getCaptureMarker(node: J): CaptureMarker | undefined {
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

    /**
     * Checks if a statement is an ExpressionStatement wrapping a capture identifier.
     * When a capture placeholder appears in statement position, the parser wraps it as
     * an ExpressionStatement. This method unwraps it to get the identifier.
     *
     * @param stmt The statement to check
     * @returns The unwrapped capture identifier, or the original statement if not wrapped
     */
    static unwrapStatementCapture(stmt: J): J {
        // Check if it's an ExpressionStatement containing a capture identifier
        if (stmt.kind === JS.Kind.ExpressionStatement) {
            const exprStmt = stmt as JS.ExpressionStatement;
            if (exprStmt.expression?.kind === J.Kind.Identifier) {
                const identifier = exprStmt.expression as J.Identifier;
                // Check if this is a capture placeholder
                if (identifier.simpleName?.startsWith(this.CAPTURE_PREFIX)) {
                    return identifier;
                }
            }
        }
        return stmt;
    }
}
