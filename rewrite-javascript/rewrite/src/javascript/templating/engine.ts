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
import {Cursor, isTree, produceAsync, Tree, updateIfChanged} from '../..';
import {emptySpace, getPaddedElement, J, Statement, Type} from '../../java';
import {Any, Capture, JavaScriptParser, JavaScriptVisitor, JS} from '..';
import {create as produce} from 'mutative';
import {CaptureMarker, PlaceholderUtils, WRAPPER_FUNCTION_NAME} from './utils';
import {CAPTURE_NAME_SYMBOL, CAPTURE_TYPE_SYMBOL, CaptureImpl, CaptureValue, RAW_CODE_SYMBOL, RawCode} from './capture';
import {PlaceholderReplacementVisitor} from './placeholder-replacement';
import {JavaCoordinates} from './template';
import {maybeAutoFormat} from '../format';
import {isExpression, isStatement} from '../parser-utils';
import {randomId} from '../../uuid';
import ts from "typescript";
import {DependencyWorkspace} from "../dependency-workspace";
import {Parameter} from "./types";

/**
 * Simple LRU (Least Recently Used) cache implementation.
 * Used for template/pattern compilation caching with bounded memory usage.
 */
class LRUCache<K, V> {
    private cache = new Map<K, V>();

    constructor(private maxSize: number) {}

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
 * Module-level TypeScript sourceFileCache for template parsing.
 */
let templateSourceFileCache: Map<string, ts.SourceFile> | undefined;

/**
 * Configure the sourceFileCache used for template parsing.
 *
 * @param cache The sourceFileCache to use, or undefined to disable caching
 */
export function setTemplateSourceFileCache(cache?: Map<string, ts.SourceFile>): void {
    templateSourceFileCache = cache;
}

/**
 * Cache for compiled templates and patterns.
 * Stores parsed ASTs to avoid expensive re-parsing and dependency resolution.
 * Bounded to 100 entries using LRU eviction to prevent unbounded memory growth.
 */
class TemplateCache {
    private cache = new LRUCache<string, JS.CompilationUnit>(100);

    /**
     * Generates a cache key from template string, captures, and options.
     */
    private generateKey(
        templateString: string,
        captures: (Capture | Any)[],
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
        captures: (Capture | Any)[],
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
            workspaceDir = await DependencyWorkspace.getOrCreateWorkspace({dependencies});
        }

        // Prepend context statements for type attribution context
        const fullTemplateString = contextStatements.length > 0
            ? contextStatements.join('\n') + '\n' + templateString
            : templateString;

        // Parse and cache (workspace only needed during parsing)
        // Use templateSourceFileCache if configured for ~3.2x speedup on dependency file parsing
        const parser = new JavaScriptParser({
            relativeTo: workspaceDir,
            sourceFileCache: templateSourceFileCache
        });
        const parseGenerator = parser.parse({text: fullTemplateString, sourcePath: 'template.tsx'});
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

/**
 * Cache for compiled templates and patterns.
 * Private to the engine module - encapsulates caching implementation.
 */
const templateCache = new TemplateCache();

/**
 * Clears the template cache. Only exported for testing and benchmarking purposes.
 * Normal application code should not need to call this.
 */
export function clearTemplateCache(): void {
    templateCache.clear();
}

/**
 * Internal template engine - handles the core templating logic.
 * Not exported from index, so only visible within the templating module.
 */
export class TemplateEngine {
    /**
     * Gets the parsed and extracted template tree (before value substitution).
     * This is the cacheable part of template processing.
     *
     * @param templateParts The string parts of the template
     * @param parameters The parameters between the string parts
     * @param contextStatements Context declarations (imports, types, etc.) to prepend for type attribution
     * @param dependencies NPM dependencies for type attribution
     * @returns A Promise resolving to the extracted template AST
     */
    static async getTemplateTree(
        templateParts: TemplateStringsArray,
        parameters: Parameter[],
        contextStatements: string[] = [],
        dependencies: Record<string, string> = {}
    ): Promise<J> {
        // Generate type preamble for captures/parameters with types
        const preamble = TemplateEngine.generateTypePreamble(parameters);

        // Build the template string with parameter placeholders
        const templateString = TemplateEngine.buildTemplateString(templateParts, parameters);

        // Add preamble to context statements (so they're skipped during extraction)
        const contextWithPreamble = preamble.length > 0
            ? [...contextStatements, ...preamble]
            : contextStatements;

        // Use cache to get or parse the compilation unit
        const cu = await templateCache.getOrParse(
            templateString,
            [],
            contextWithPreamble,
            dependencies
        );

        // Check if there are any statements
        if (!cu.statements || cu.statements.length === 0) {
            throw new Error(`Failed to parse template code (no statements):\n${templateString}`);
        }

        // The template code is always the last statement (after context + preamble)
        const lastStatement = cu.statements[cu.statements.length - 1];

        // Extract from wrapper using shared utility
        const extracted = PlaceholderUtils.extractFromWrapper(lastStatement, 'Template');

        return produce(extracted, _ => {});
    }

    /**
     * Applies a template from a pre-parsed AST and returns the resulting AST.
     * This method is used by Template.apply() after getting the cached template tree.
     *
     * @param ast The pre-parsed template AST
     * @param parameters The parameters between the string parts
     * @param cursor The cursor pointing to the current location in the AST
     * @param coordinates The coordinates specifying where and how to insert the generated AST
     * @param values Map of capture names to values to replace the parameters with
     * @param wrappersMap Map of capture names to J.RightPadded wrappers (for preserving markers)
     * @returns A Promise resolving to the generated AST node
     */
    static async applyTemplateFromAst(
        ast: JS.CompilationUnit,
        parameters: Parameter[],
        cursor: Cursor,
        coordinates: JavaCoordinates,
        values: Pick<Map<string, J>, 'get'> = new Map(),
        wrappersMap: Pick<Map<string, J.RightPadded<J> | J.RightPadded<J>[]>, 'get'> = new Map()
    ): Promise<J | undefined> {
        // Create substitutions map for placeholders
        const substitutions = new Map<string, Parameter>();
        for (let i = 0; i < parameters.length; i++) {
            const placeholder = `${PlaceholderUtils.PLACEHOLDER_PREFIX}${i}__`;
            substitutions.set(placeholder, parameters[i]);
        }

        // Unsubstitute placeholders with actual parameter values and match results
        const visitor = new PlaceholderReplacementVisitor(substitutions, values, wrappersMap);
        const unsubstitutedAst = (await visitor.visit(ast, null))!;

        // Apply the template to the current AST
        return new TemplateApplier(cursor, coordinates, unsubstitutedAst).apply();
    }

    /**
     * Generates type preamble declarations for captures/parameters with type annotations.
     *
     * @param parameters The parameters
     * @returns Array of preamble statements
     */
    private static generateTypePreamble(parameters: Parameter[]): string[] {
        const preamble: string[] = [];

        for (let i = 0; i < parameters.length; i++) {
            const param = parameters[i].value;
            const placeholder = `${PlaceholderUtils.PLACEHOLDER_PREFIX}${i}__`;

            // Check for Capture (could be a Proxy, so check for symbol property)
            const isCapture = param instanceof CaptureImpl ||
                (param && typeof param === 'object' && param[CAPTURE_NAME_SYMBOL]);
            const isCaptureValue = param instanceof CaptureValue;
            const isTreeArray = Array.isArray(param) && param.length > 0 && isTree(param[0]);

            if (isCapture) {
                const captureType = param[CAPTURE_TYPE_SYMBOL];
                if (captureType) {
                    const typeString = typeof captureType === 'string'
                        ? captureType
                        : this.typeToString(captureType);
                    // Only add preamble if we have a concrete type (not 'any')
                    if (typeString !== 'any') {
                        preamble.push(`let ${placeholder}: ${typeString};`);
                    }
                }
            } else if (isCaptureValue) {
                // For CaptureValue, check if the root capture has a type
                const rootCapture = param.rootCapture;
                if (rootCapture) {
                    const captureType = (rootCapture as any)[CAPTURE_TYPE_SYMBOL];
                    if (captureType) {
                        const typeString = typeof captureType === 'string'
                            ? captureType
                            : this.typeToString(captureType);
                        // Only add preamble if we have a concrete type (not 'any')
                        if (typeString !== 'any') {
                            preamble.push(`let ${placeholder}: ${typeString};`);
                        }
                    }
                }
            } else if (isTree(param) && !isTreeArray) {
                // For J elements, derive type from the element's type property if it exists
                const jElement = param as J;
                if ((jElement as any).type) {
                    const typeString = this.typeToString((jElement as any).type);
                    // Only add preamble if we have a concrete type (not 'any')
                    if (typeString !== 'any') {
                        preamble.push(`let ${placeholder}: ${typeString};`);
                    }
                }
            }
        }

        return preamble;
    }

    /**
     * Builds a template string with parameter placeholders.
     * RawCode parameters are spliced directly into the template at construction time.
     * Other parameters use placeholders that are replaced during application.
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

                // Check if this is a RawCode instance - splice directly
                if (param instanceof RawCode || (param && typeof param === 'object' && param[RAW_CODE_SYMBOL])) {
                    result += (param as RawCode).code;
                } else {
                    // All other parameters use placeholders
                    // This ensures templates with the same structure always produce the same AST
                    const placeholder = `${PlaceholderUtils.PLACEHOLDER_PREFIX}${i}__`;
                    result += placeholder;
                }
            }
        }

        // Always wrap in function body - let the parser decide what it is,
        // then we'll extract intelligently based on what was parsed
        return `function ${WRAPPER_FUNCTION_NAME}() { ${result} }`;
    }

    /**
     * Converts a Type instance to a TypeScript type string.
     *
     * @param type The Type instance
     * @returns A TypeScript type string
     */
    private static typeToString(type: Type): string {
        // Handle Type.Class and Type.ShallowClass - return their fully qualified names
        if (type.kind === Type.Kind.Class || type.kind === Type.Kind.ShallowClass) {
            const classType = type as Type.Class;
            return classType.fullyQualifiedName;
        }

        // Handle Type.Primitive - map to TypeScript primitive types
        if (type.kind === Type.Kind.Primitive) {
            const primitiveType = type as Type.Primitive;
            switch (primitiveType.keyword) {
                case 'String':
                    return 'string';
                case 'boolean':
                    return 'boolean';
                case 'double':
                case 'float':
                case 'int':
                case 'long':
                case 'short':
                case 'byte':
                    return 'number';
                case 'void':
                    return 'void';
                default:
                    return 'any';
            }
        }

        // Handle Type.Array - render component type plus []
        if (type.kind === Type.Kind.Array) {
            const arrayType = type as Type.Array;
            const componentTypeString = this.typeToString(arrayType.elemType);
            return `${componentTypeString}[]`;
        }

        // For other types, return 'any' as a fallback
        // TODO: Implement proper Type to string conversion for other Type.Kind values
        return 'any';
    }

    /**
     * Gets the parsed and extracted pattern tree with capture markers attached.
     * This is the entry point for pattern processing, providing pattern-specific
     * functionality on top of the shared template tree generation.
     *
     * @param templateParts The string parts of the template
     * @param captures The captures between the string parts (can include RawCode)
     * @param contextStatements Context declarations (imports, types, etc.) to prepend for type attribution
     * @param dependencies NPM dependencies for type attribution
     * @returns A Promise resolving to the extracted pattern AST with capture markers
     */
    static async getPatternTree(
        templateParts: TemplateStringsArray,
        captures: (Capture | Any | RawCode)[],
        contextStatements: string[] = [],
        dependencies: Record<string, string> = {}
    ): Promise<J> {
        // Generate type preamble for captures with types (skip RawCode)
        const preamble: string[] = [];
        for (const capture of captures) {
            // Skip raw code - it's not a capture
            if (capture instanceof RawCode || (capture && typeof capture === 'object' && (capture as any)[RAW_CODE_SYMBOL])) {
                continue;
            }

            const captureName = (capture as any)[CAPTURE_NAME_SYMBOL] || capture.getName();
            const captureType = (capture as any)[CAPTURE_TYPE_SYMBOL];
            if (captureType) {
                // Convert Type to string if needed
                const typeString = typeof captureType === 'string'
                    ? captureType
                    : this.typeToString(captureType);
                // Only add preamble if we have a concrete type (not 'any')
                if (typeString !== 'any') {
                    const placeholder = PlaceholderUtils.createCapture(captureName, undefined);
                    preamble.push(`let ${placeholder}: ${typeString};`);
                }
            }
            // Don't add preamble declarations without types - they don't provide type attribution
        }

        // Build the template string with placeholders for captures and raw code
        let result = '';
        for (let i = 0; i < templateParts.length; i++) {
            result += templateParts[i];
            if (i < captures.length) {
                const capture = captures[i];

                // Check if this is a RawCode instance - splice directly
                if (capture instanceof RawCode || (capture && typeof capture === 'object' && (capture as any)[RAW_CODE_SYMBOL])) {
                    result += (capture as RawCode).code;
                } else {
                    // Use symbol to access capture name without triggering Proxy
                    const captureName = (capture as any)[CAPTURE_NAME_SYMBOL] || capture.getName();
                    result += PlaceholderUtils.createCapture(captureName, undefined);
                }
            }
        }

        // Always wrap in function body - let the parser decide what it is,
        // then we'll extract intelligently based on what was parsed
        const templateString = `function ${WRAPPER_FUNCTION_NAME}() { ${result} }`;

        // Add preamble to context statements (so they're skipped during extraction)
        const contextWithPreamble = preamble.length > 0
            ? [...contextStatements, ...preamble]
            : contextStatements;

        // Filter out RawCode from captures for cache and marker attachment
        const actualCaptures = captures.filter(c =>
            !(c instanceof RawCode || (c && typeof c === 'object' && (c as any)[RAW_CODE_SYMBOL]))
        ) as (Capture | Any)[];

        // Use cache to get or parse the compilation unit
        const cu = await templateCache.getOrParse(
            templateString,
            actualCaptures,
            contextWithPreamble,
            dependencies
        );

        // Check if there are any statements
        if (!cu.statements || cu.statements.length === 0) {
            throw new Error(`Failed to parse pattern code (no statements):\n${templateString}`);
        }

        // The pattern code is always the last statement (after context + preamble)
        const lastStatement = cu.statements[cu.statements.length - 1];

        // Extract from wrapper using shared utility
        const extracted = PlaceholderUtils.extractFromWrapper(lastStatement, 'Pattern');

        // Attach CaptureMarkers to capture identifiers (only for actual captures, not raw code)
        const visitor = new MarkerAttachmentVisitor(actualCaptures);
        return (await visitor.visit(extracted, undefined))!;
    }
}

/**
 * Visitor that attaches CaptureMarkers to capture identifiers in pattern ASTs.
 * This allows efficient capture detection without string parsing during matching.
 * Used by TemplateEngine.getPatternTree() for pattern-specific processing.
 */
class MarkerAttachmentVisitor extends JavaScriptVisitor<undefined> {
    constructor(private readonly captures: (Capture | Any)[]) {
        super();
    }

    /**
     * Attaches CaptureMarker to capture identifiers.
     */
    protected override async visitIdentifier(ident: J.Identifier, p: undefined): Promise<J | undefined> {
        // First call parent to handle standard visitation
        const visited = await super.visitIdentifier(ident, p);
        if (!visited || visited.kind !== J.Kind.Identifier) {
            return visited;
        }
        ident = visited as J.Identifier;

        // Check if this is a capture placeholder
        if (ident.simpleName?.startsWith(PlaceholderUtils.CAPTURE_PREFIX)) {
            const captureInfo = PlaceholderUtils.parseCapture(ident.simpleName);
            if (captureInfo) {
                // Find the original capture object to get variadic options and constraint
                const captureObj = this.captures.find(c => c.getName() === captureInfo.name);
                const variadicOptions = captureObj?.getVariadicOptions();
                const constraint = captureObj?.getConstraint?.();

                // Add CaptureMarker to the Identifier with constraint
                const marker = new CaptureMarker(captureInfo.name, variadicOptions, constraint);
                return updateIfChanged(ident, {
                    markers: {
                        ...ident.markers,
                        markers: [...ident.markers.markers, marker]
                    }
                });
            }
        }

        return ident;
    }

    /**
     * Propagates markers from element to RightPadded wrapper.
     */
    public override async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, p: undefined): Promise<J.RightPadded<T> | undefined> {
        // For tree types, the padded value IS the element (intersection type)
        const rightElement = getPaddedElement(right);
        if (!isTree(rightElement)) {
            return right;
        }

        const visitedElement = await this.visit(rightElement as J, p);
        if (visitedElement && visitedElement !== rightElement as Tree) {
            const result = await produceAsync<J.RightPadded<T>>(right, async (draft: any) => {
                // Visit element first
                if (rightElement && (rightElement as any).kind) {
                    // Check if element has a CaptureMarker
                    const elementMarker = PlaceholderUtils.getCaptureMarker(visitedElement);
                    if (elementMarker) {
                        draft.padding.markers.markers.push(elementMarker);
                    } else {
                        // For tree types, merge visited element back with padding
                        Object.assign(draft, visitedElement);
                    }
                }
            });
            return result!;
        }

        return right;
    }

    /**
     * Propagates markers from expression to ExpressionStatement.
     */
    protected override async visitExpressionStatement(expressionStatement: JS.ExpressionStatement, p: undefined): Promise<J | undefined> {
        // Visit the expression
        const visitedExpression = await this.visit(expressionStatement.expression, p);

        // Check if expression has a CaptureMarker
        const expressionMarker = PlaceholderUtils.getCaptureMarker(visitedExpression as any);
        if (expressionMarker) {
            return updateIfChanged(expressionStatement, {
                markers: {
                    ...expressionStatement.markers,
                    markers: [...expressionStatement.markers.markers, expressionMarker]
                },
            });
        }

        // No marker to move, just update with visited expression
        return updateIfChanged(expressionStatement, {
            expression: visitedExpression
        });
    }

    /**
     * Propagates markers from name identifier to BindingElement.
     * This handles destructuring patterns like {${props}} where the capture marker
     * is on the identifier but needs to be on the BindingElement for container matching.
     */
    protected override async visitBindingElement(bindingElement: JS.BindingElement, p: undefined): Promise<J | undefined> {
        // Visit the name
        const visitedName = await this.visit(bindingElement.name, p);

        // Check if name has a CaptureMarker
        const nameMarker = PlaceholderUtils.getCaptureMarker(visitedName as any);
        if (nameMarker) {
            return updateIfChanged(bindingElement, {
                name: visitedName,
                markers: {
                    ...bindingElement.markers,
                    markers: [...bindingElement.markers.markers, nameMarker]
                },
            });
        }

        // No marker to move, just update with visited name
        return updateIfChanged(bindingElement, {
            name: visitedName
        });
    }
}

/**
 * Helper class for applying a template to an AST.
 */
export class TemplateApplier {
    constructor(
        private readonly cursor: Cursor,
        private readonly coordinates: JavaCoordinates,
        private readonly ast: J
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
            case 'STATEMENT_PREFIX':
            case 'BLOCK_END':
                return this.applyInternal();
            default:
                throw new Error(`Unsupported location: ${loc}`);
        }
    }

    /**
     * Applies the template to an expression.
     *
     * @returns A Promise resolving to the modified AST
     */
    private async applyInternal(): Promise<J | undefined> {
        const {tree} = this.coordinates;

        if (!tree) {
            return this.ast;
        }

        const originalTree = tree as J;
        const resultToUse = this.wrapTree(originalTree, this.ast);
        return this.format(resultToUse, originalTree);
    }

    private async format(resultToUse: J, originalTree: J) {
        // Create a copy of the AST with the prefix from the target
        const result = {
            ...resultToUse,
            // We temporarily set the ID so that the formatter can identify the tree
            id: originalTree.id,
            prefix: originalTree.prefix
        };

        // Apply auto-formatting to the result
        const formatted =
            await maybeAutoFormat(originalTree, result, null, undefined, this.cursor?.parent);

        // Restore the original ID
        return {...formatted, id: resultToUse.id};
    }

    private wrapTree(originalTree: J, resultToUse: J) {
        const parentTree = this.cursor?.parentTree()?.value;

        // Only apply wrapping logic if we have parent context
        if (parentTree) {
            // FIXME: This is a heuristic to determine if the parent expects a statement child
            const parentExpectsStatement = parentTree.kind === J.Kind.Block ||
                parentTree.kind === J.Kind.Case ||
                parentTree.kind === J.Kind.DoWhileLoop ||
                parentTree.kind === J.Kind.ForEachLoop ||
                parentTree.kind === J.Kind.ForLoop ||
                parentTree.kind === J.Kind.If ||
                parentTree.kind === J.Kind.IfElse ||
                parentTree.kind === J.Kind.WhileLoop ||
                parentTree.kind === JS.Kind.CompilationUnit ||
                parentTree.kind === JS.Kind.ForInLoop;
            const originalIsStatement = isStatement(originalTree);

            const resultIsStatement = isStatement(resultToUse);
            const resultIsExpression = isExpression(resultToUse);

            // Determine context and wrap if needed
            if (parentExpectsStatement && originalIsStatement) {
                // Statement context: wrap in ExpressionStatement if result is not a statement
                if (!resultIsStatement && resultIsExpression) {
                    resultToUse = {
                        kind: JS.Kind.ExpressionStatement,
                        id: randomId(),
                        prefix: resultToUse.prefix,
                        markers: resultToUse.markers,
                        expression: { ...resultToUse, prefix: emptySpace }
                    } as JS.ExpressionStatement;
                }
            } else if (!parentExpectsStatement) {
                // Expression context: wrap in StatementExpression if result is statement-only
                if (resultIsStatement && !resultIsExpression) {
                    const stmt = resultToUse as Statement;
                    resultToUse = {
                        kind: JS.Kind.StatementExpression,
                        id: randomId(),
                        prefix: stmt.prefix,
                        markers: stmt.markers,
                        statement: { ...stmt, prefix: emptySpace }
                    } as JS.StatementExpression;
                }
            }
        }
        return resultToUse;
    }
}
