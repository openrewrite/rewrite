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
import {emptySpace, J, Statement, Type} from '../../java';
import {Any, Capture, JavaScriptParser, JavaScriptVisitor, JS} from '..';
import {create as produce} from 'mutative';
import {CaptureMarker, dedentTemplate, PlaceholderUtils, randomizeIds, retainIds, treeIds, WRAPPER_FUNCTION_NAME} from './utils';
import {CAPTURE_NAME_SYMBOL, CAPTURE_TYPE_SYMBOL, CaptureImpl, CaptureValue, RAW_CODE_SYMBOL, RawCode} from './capture';
import {PlaceholderReplacementVisitor} from './placeholder-replacement';
import {maybeParenthesize, parenthesize, requiredPrecedence, startsWithDeclarationToken} from './precedence';
import {JavaCoordinates} from './template';
import {maybeAutoFormat} from '../format';
import {renameBindings} from './bindings';
import {isExpression, isStatement} from '../parser-utils';
import {randomId} from '../../uuid';
import * as ts from "../compiler";
import {DependencyWorkspace} from "../dependency-workspace";
import {ModuleScopeBinding, moduleScopeBindings} from '../add-import';
import {walk} from '../scope';
import {isIdentifier} from '../../java';
import {findMarker, MarkersKind, ParseExceptionResult} from '../../markers';

/** A module a template's context binds, and whether the parse resolved it well enough to attribute. */
export interface ContextBinding extends ModuleScopeBinding {
    attributed: boolean;
}
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
    templateParsers.clear();
}

// Every template parses under `template.tsx`, so one parser per workspace carries its program
// from one compile to the next; see `JavaScriptParser.parse`.
const templateParsers: Map<string, JavaScriptParser> = new Map();

function templateParser(workspaceDir?: string, types?: string[]): JavaScriptParser {
    // `types` changes what the compiler loads, so parsers cannot be shared across differing sets.
    // An empty list loads nothing where absence loads the defaults, so the two encode apart.
    const key = `${workspaceDir ?? ""}::${JSON.stringify(types ?? null)}`;
    let parser = templateParsers.get(key);
    if (!parser) {
        parser = new JavaScriptParser({relativeTo: workspaceDir, sourceFileCache: templateSourceFileCache, types});
        templateParsers.set(key, parser);
    }
    return parser;
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
        dependencies: Record<string, string>,
        types: string[] | undefined
    ): string {
        // Use the actual template string (with placeholders) as the primary key
        const templateKey = templateString;

        // Capture names
        const capturesKey = captures.map(c => c.getName()).join(',');

        // Context statements
        const contextKey = contextStatements.join(';');

        // Dependencies
        const depsKey = JSON.stringify(dependencies || {});

        return `${templateKey}::${capturesKey}::${contextKey}::${depsKey}::${JSON.stringify(types ?? null)}`;
    }

    /**
     * Gets a cached compilation unit or creates and caches a new one.
     */
    async getOrParse(
        templateString: string,
        captures: (Capture | Any)[],
        contextStatements: string[],
        dependencies: Record<string, string>,
        types?: string[]
    ): Promise<JS.CompilationUnit> {
        const key = this.generateKey(templateString, captures, contextStatements, dependencies, types);

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
        const parser = templateParser(workspaceDir, types);
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

/** Whether a name reads as a TypeScript type reference: a dotted chain of identifiers. */
function isTypeReference(name: string): boolean {
    return /^[A-Za-z_$][\w$]*(\.[A-Za-z_$][\w$]*)*$/.test(name);
}

/**
 * The compiler's diagnostic for code that did not parse, as one line: the marker states it ahead
 * of a stack, and its positions index the whole parsed text, context statements included.
 */
function parseFailureReason(cu: JS.CompilationUnit): string {
    const failure = findMarker<ParseExceptionResult>(cu, MarkersKind.ParseExceptionResult);
    return failure ? failure.message.split('\n')[0].replace(/:$/, '') : 'no statements';
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
    /** The template parsed with its context, which is what gives its code types to attribute against. */
    private static async parseWithContext(
        templateParts: TemplateStringsArray,
        parameters: Parameter[],
        contextStatements: string[],
        dependencies: Record<string, string>,
        types: string[] | undefined
    ): Promise<JS.CompilationUnit> {
        // A capture's declared type reaches the parse as a declaration, so it belongs with context.
        const preamble = TemplateEngine.parameterPreamble(parameters);
        const templateString = TemplateEngine.buildTemplateString(templateParts, parameters);
        const contextWithPreamble = preamble.length > 0
            ? [...contextStatements, ...preamble]
            : contextStatements;
        return TemplateEngine.parseOrThrow('template', templateString, [], contextWithPreamble, dependencies, types);
    }

    /**
     * The parse behind every template and pattern. Code that fails to parse yields a compilation
     * unit with no statements, which every caller goes on to read, so it fails here naming the code.
     */
    private static async parseOrThrow(
        kind: 'template' | 'pattern',
        templateString: string,
        captures: (Capture | Any)[],
        contextStatements: string[],
        dependencies: Record<string, string>,
        types: string[] | undefined
    ): Promise<JS.CompilationUnit> {
        const cu = await templateCache.getOrParse(templateString, captures, contextStatements, dependencies, types);
        if (!cu.statements || cu.statements.length === 0) {
            throw new Error(`Failed to parse ${kind} code (${parseFailureReason(cu)}):\n${templateString}`);
        }
        return cu;
    }

    /**
     * The modules the template's context binds, for the caller to bind in the file being edited.
     * An `import` or `require` states one; anything else — a `declare`, a helper signature — types
     * the template without asking for a binding.
     */
    static async getContextBindings(
        templateParts: TemplateStringsArray,
        parameters: Parameter[],
        contextStatements: string[] = [],
        dependencies: Record<string, string> = {},
        types?: string[]
    ): Promise<ContextBinding[]> {
        const cu = await TemplateEngine.parseWithContext(templateParts, parameters, contextStatements, dependencies, types);
        // The template's own code is the last statement, so everything ahead of it is context.
        const context = {...cu, statements: cu.statements.slice(0, -1)};
        const attributed = new Set<string>();
        walk(context.statements, node => {
            if (isIdentifier(node) && (node.type !== undefined || node.fieldType !== undefined)) {
                attributed.add(node.simpleName);
            }
            return true;
        });
        return moduleScopeBindings(context)
            .filter(b => b.module !== undefined)
            .map(b => ({...b, attributed: attributed.has(b.name)}));
    }

    static async getTemplateTree(
        templateParts: TemplateStringsArray,
        parameters: Parameter[],
        contextStatements: string[] = [],
        dependencies: Record<string, string> = {},
        types?: string[]
    ): Promise<J> {
        const cu = await TemplateEngine.parseWithContext(templateParts, parameters, contextStatements, dependencies, types);

        // The template code is always the last statement (after context + preamble)
        const lastStatement = cu.statements[cu.statements.length - 1].element;

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
     * @param format Whether to fit the result to where it lands
     * @param renames Local names for the template's declared bindings, keyed as declared
     * @param modules The module each declared binding names, keyed as declared
     * @returns A Promise resolving to the generated AST node
     */
    static async applyTemplateFromAst(
        ast: J,
        parameters: Parameter[],
        cursor: Cursor,
        coordinates: JavaCoordinates,
        values: Pick<Map<string, J>, 'get'> = new Map(),
        wrappersMap: Pick<Map<string, J.RightPadded<J> | J.RightPadded<J>[]>, 'get'> = new Map(),
        format: boolean = true,
        renames: Record<string, string> = {},
        modules: Record<string, string> = {}
    ): Promise<J | undefined> {
        // Create substitutions map for placeholders
        const substitutions = new Map<string, Parameter>();
        for (let i = 0; i < parameters.length; i++) {
            const placeholder = `${PlaceholderUtils.PLACEHOLDER_PREFIX}${i}__`;
            substitutions.set(placeholder, parameters[i]);
        }

        // Before substitution, so that ids carried over from the source tree survive this pass
        const fresh = await randomizeIds(ast);

        const bound = Object.keys(renames).length > 0
            ? await renameBindings(fresh.tree as J, renames, modules)
            : fresh.tree;

        // Unsubstitute placeholders with actual parameter values and match results
        const visitor = new PlaceholderReplacementVisitor(substitutions, values, wrappersMap);
        const unsubstitutedAst = (await visitor.visit(bound, null))!;

        // An id may only be kept where the node answering to it is leaving the tree, which is the
        // subtree this application replaces. A parameter named twice, or spliced in from somewhere
        // that stays, would otherwise put one id in two places.
        const retainable = new Set(fresh.ids);
        if (coordinates.tree) {
            for (const id of await treeIds(coordinates.tree as J)) {
                retainable.add(id);
            }
        }
        const uniqueAst = await retainIds(unsubstitutedAst, retainable);

        // Apply the template to the current AST
        return new TemplateApplier(cursor, coordinates, uniqueAst, format).apply();
    }

    /**
     * Generates type preamble declarations for captures/parameters with type annotations.
     *
     * @param parameters The parameters
     * @returns Array of preamble statements
     */
    /** The declarations that give a capture's placeholder its type while the pattern is parsed. */
    static capturePreamble(captures: (Capture | Any | RawCode)[]): string[] {
        const preamble: string[] = [];
        for (const capture of captures) {
            // Raw code is spliced in as source, so it declares nothing
            if (capture instanceof RawCode || (capture && typeof capture === 'object' && (capture as any)[RAW_CODE_SYMBOL])) {
                continue;
            }

            const captureName = (capture as any)[CAPTURE_NAME_SYMBOL] || capture.getName();
            const captureType = (capture as any)[CAPTURE_TYPE_SYMBOL];
            if (captureType) {
                const typeString = typeof captureType === 'string'
                    ? captureType
                    : this.typeToString(captureType);
                // `any` attributes nothing, so a declaration for it would only cost a parse
                if (typeString !== 'any') {
                    const placeholder = PlaceholderUtils.createCapture(captureName, undefined);
                    preamble.push(`let ${placeholder}: ${typeString};`);
                }
            }
        }
        return preamble;
    }

    /** The parameter counterpart of {@link capturePreamble}. */
    static parameterPreamble(parameters: Parameter[]): string[] {
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
        return `function ${WRAPPER_FUNCTION_NAME}() { ${dedentTemplate(result)} }`;
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
            // A module qualifies its types by where they come from — `src/a.Foo` — and a path that
            // is no type reference cannot be declared for. Naming a module's type takes a `context`
            // statement of the caller's own, which `type` then names.
            return isTypeReference(classType.fullyQualifiedName) ? classType.fullyQualifiedName : 'any';
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
     * Gets the parsed and extracted pattern tree, with placeholder identifiers left bare;
     * `attachCaptureMarkers` binds it to a particular set of captures.
     *
     * @param templateParts The string parts of the template
     * @param captures The captures between the string parts (can include RawCode)
     * @param contextStatements Context declarations (imports, types, etc.) to prepend for type attribution
     * @param dependencies NPM dependencies for type attribution
     * @returns A Promise resolving to the extracted pattern AST
     */
    static async getPatternTree(
        templateParts: TemplateStringsArray,
        captures: (Capture | Any | RawCode)[],
        contextStatements: string[] = [],
        dependencies: Record<string, string> = {},
        types?: string[]
    ): Promise<J> {
        const preamble = TemplateEngine.capturePreamble(captures);

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

        const cu = await TemplateEngine.parseOrThrow(
            'pattern',
            templateString,
            actualCaptures,
            contextWithPreamble,
            dependencies,
            types
        );

        // The pattern code is always the last statement (after context + preamble)
        const lastStatement = cu.statements[cu.statements.length - 1].element;

        // Extract from wrapper using shared utility
        return PlaceholderUtils.extractFromWrapper(lastStatement, 'Pattern');
    }

    /**
     * Binds a pattern tree to a set of captures by attaching a CaptureMarker, carrying that
     * capture's constraint and variadic options, to each placeholder identifier.
     */
    static async attachCaptureMarkers(tree: J, captures: (Capture | Any | RawCode)[]): Promise<J> {
        const actualCaptures = captures.filter(c =>
            !(c instanceof RawCode || (c && typeof c === 'object' && (c as any)[RAW_CODE_SYMBOL]))
        ) as (Capture | Any)[];
        return (await new MarkerAttachmentVisitor(actualCaptures).visit(tree, undefined))!;
    }
}

/**
 * Visitor that attaches CaptureMarkers to capture identifiers in pattern ASTs.
 * This allows efficient capture detection without string parsing during matching.
 * Reached through TemplateEngine.attachCaptureMarkers().
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
        if (!isTree(right.element)) {
            return right;
        }

        const visitedElement = await this.visit(right.element as J, p);
        if (visitedElement && visitedElement !== right.element as Tree) {
            const result = await produceAsync<J.RightPadded<T>>(right, async (draft: any) => {
                // Visit element first
                if (right.element && (right.element as any).kind) {
                    // Check if element has a CaptureMarker
                    const elementMarker = PlaceholderUtils.getCaptureMarker(visitedElement);
                    if (elementMarker) {
                        draft.markers.markers.push(elementMarker);
                    } else {
                        draft.element = visitedElement;
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
        private readonly ast: J,
        private readonly shouldFormat: boolean = true
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
        let resultToUse = this.wrapTree(originalTree, this.ast);
        const slot = this.replacedSlot(originalTree);
        if (slot) {
            // `format` substitutes the target's prefix, so decide against the prefix that will print
            resultToUse = maybeParenthesize(slot[0], slot[1], {...resultToUse, prefix: originalTree.prefix});
        }
        return this.format(resultToUse, originalTree);
    }

    /** The node holding the tree being replaced and the id it holds it under, per the cursor. */
    private replacedSlot(originalTree: J): [J, string] | undefined {
        const frames: J[] = [];
        for (let c: Cursor | undefined = this.cursor; c && frames.length < 2; c = c.parent) {
            if (isTree(c.value)) {
                frames.push(c.value as J);
            }
        }
        if (frames.length === 0) {
            return undefined;
        }

        // The visitor may sit on the node holding what is being replaced rather than on it
        if (requiredPrecedence(frames[0], originalTree.id) !== undefined) {
            return [frames[0], originalTree.id];
        }
        if (frames.length < 2) {
            return undefined;
        }
        if (requiredPrecedence(frames[1], originalTree.id) !== undefined) {
            return [frames[1], originalTree.id];
        }
        // A chained rule rewrites the previous rule's detached result, leaving the cursor on the node
        // that result stands in for; a wrong guess here only ever adds parentheses, never drops them
        return frames[0].id !== originalTree.id && requiredPrecedence(frames[1], frames[0].id) !== undefined ?
            [frames[1], frames[0].id] : undefined;
    }

    private async format(resultToUse: J, originalTree: J) {
        // Create a copy of the AST with the prefix from the target
        const result = {
            ...resultToUse,
            // We temporarily set the ID so that the formatter can identify the tree
            id: originalTree.id,
            prefix: originalTree.prefix
        };

        if (!this.shouldFormat) {
            return {...result, id: resultToUse.id};
        }

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
                const needsGrouping = resultIsExpression && startsWithDeclarationToken(resultToUse);
                // Statement context: wrap in ExpressionStatement if result is not a statement
                if (needsGrouping || (!resultIsStatement && resultIsExpression)) {
                    const expression = needsGrouping ? parenthesize(resultToUse) : resultToUse;
                    resultToUse = {
                        kind: JS.Kind.ExpressionStatement,
                        id: randomId(),
                        prefix: expression.prefix,
                        markers: expression.markers,
                        expression: { ...expression, prefix: emptySpace }
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
