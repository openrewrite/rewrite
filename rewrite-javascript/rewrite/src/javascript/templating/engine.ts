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
import {Cursor, isTree, Markers, markers, produceAsync, Tree, updateIfChanged} from '../..';
import {emptySpace, J, Statement, Type, TypedTree} from '../../java';
import {Any, Capture, JavaScriptParser, JavaScriptVisitor, JS} from '..';
import {create as produce} from 'mutative';
import {
    CaptureMarker, dedentTemplate, PlaceholderUtils, randomizeIds, retainIds, slotOf, TEMPLATE_MODULE, TemplateSlot, treeIds, wrapCode
} from './utils';
import {CAPTURE_NAME_SYMBOL, CAPTURE_TYPE_SYMBOL, CaptureImpl, CaptureValue, RAW_CODE_SYMBOL, RawCode, TemplateParamImpl} from './capture';
import {PlaceholderReplacementVisitor, SubstitutedValue} from './placeholder-replacement';
import {
    isTrailingMarker, maybeParenthesize, maybeParenthesizeType, parenthesize, requiredPrecedence, startsWithDeclarationToken
} from './precedence';
import {JavaCoordinates} from './template';
import {autoIndent, maybeAutoFormat} from '../format';
import {getPrettierStyle} from '../format/prettier-format';
import {renameBindings} from './bindings';
import {isExpression, isStatement} from '../parser-utils';
import {randomId} from '../../uuid';
import ts from "typescript";
import {DependencyWorkspace} from "../dependency-workspace";
import {ModuleScopeBinding, moduleScopeBindings} from '../add-import';
import {namesReferencedWithin} from '../scope';
import {findMarker, MarkersKind, ParseExceptionResult} from '../../markers';

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
        const parseGenerator = parser.parse({text: fullTemplateString, sourcePath: `${TEMPLATE_MODULE}.tsx`});
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

/** A type as TypeScript writes it, and how tightly it binds, which is where it needs parentheses. */
interface WrittenType {
    readonly text: string;
    readonly binding: 'union' | 'intersection' | 'primary';
}

/** The TypeScript type of each primitive keyword: the parser types a bigint as a long, and undefined with none. */
const primitiveTypes = new Map([
    ['String', 'string'], ['boolean', 'boolean'], ['void', 'void'], ['null', 'null'], ['', 'undefined'],
    ['long', 'bigint'], ['double', 'number'], ['float', 'number'], ['int', 'number'], ['short', 'number'],
    ['byte', 'number']
]);

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
    /** The template parsed with its context, which is what gives its code types to attribute against. */
    private static async parseWithContext(
        templateParts: TemplateStringsArray,
        parameters: Parameter[],
        contextStatements: string[],
        dependencies: Record<string, string>,
        types: string[] | undefined,
        expression: TemplateSlot = false,
        preamble: string[] = TemplateEngine.parameterPreamble(parameters)
    ): Promise<JS.CompilationUnit> {
        // A capture's type reaches the parse as a declaration, so it belongs with context.
        const templateString = TemplateEngine.buildTemplateString(templateParts, parameters, expression);
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
     * The modules the template's context binds and its code reads, for the caller to bind in the
     * file being edited. An `import` or `require` states one; anything else — a `declare`, a helper
     * signature — types the template without asking for a binding.
     */
    static async getContextBindings(
        templateParts: TemplateStringsArray,
        parameters: Parameter[],
        contextStatements: string[] = [],
        dependencies: Record<string, string> = {},
        types?: string[]
    ): Promise<ModuleScopeBinding[]> {
        // The bindings are the context's alone, so either reading of code opening with `{` serves
        const cu = await TemplateEngine.parseWithContext(templateParts, parameters, contextStatements, dependencies, types)
            .catch(e => opensWithBrace(templateParts) ?
                TemplateEngine.parseWithContext(templateParts, parameters, contextStatements, dependencies, types, true) :
                Promise.reject(e));
        // The template's own code is the last statement, so everything ahead of it is context, and a
        // name the code reads without binding it itself resolves to the context's module-scope binding.
        const context = {...cu, statements: cu.statements.slice(0, -1)};
        const referenced = namesReferencedWithin(cu.statements[cu.statements.length - 1]);
        return moduleScopeBindings(context).filter(b => b.module !== undefined && referenced.has(b.name));
    }

    /**
     * Gets the parsed and extracted template tree (before value substitution).
     * This is the cacheable part of template processing.
     *
     * @param templateParts The string parts of the template
     * @param parameters The parameters between the string parts
     * @param contextStatements Context declarations (imports, types, etc.) to prepend for type attribution
     * @param dependencies NPM dependencies for type attribution
     * @param expression Whether to parse the code as an expression
     * @param preamble The parameters' declarations, from {@link parameterPreamble}
     * @returns A Promise resolving to the extracted template AST
     */
    static async getTemplateTree(
        templateParts: TemplateStringsArray,
        parameters: Parameter[],
        contextStatements: string[] = [],
        dependencies: Record<string, string> = {},
        types?: string[],
        expression: TemplateSlot = false,
        preamble?: string[]
    ): Promise<J> {
        const cu = await TemplateEngine.parseWithContext(templateParts, parameters, contextStatements, dependencies, types, expression, preamble);

        // The template code is always the last statement (after context + preamble)
        const lastStatement = cu.statements[cu.statements.length - 1].element;

        // Extract from wrapper using shared utility
        const extracted = PlaceholderUtils.extractFromWrapper(lastStatement, 'Template', expression);

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
     * @param patternPrefixes The matched pattern's capture prefixes, given only under `format: false`
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
        patternPrefixes?: Pick<Map<string, J.Space>, 'get'>
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
            ? await renameBindings(fresh.tree as J, renames)
            : fresh.tree;

        // Unsubstitute placeholders with actual parameter values and match results
        const visitor = new PlaceholderReplacementVisitor(substitutions, values, wrappersMap, patternPrefixes);
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
        return new TemplateApplier(cursor, coordinates, uniqueAst, format, visitor.substituted).apply();
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

    /**
     * The parameter counterpart of {@link capturePreamble}. A capture without a declared type takes
     * the type of the expression bound to it, as an expression parameter takes its own. A variadic
     * capture binds a list, which has no one type to declare.
     */
    static parameterPreamble(parameters: Parameter[], values?: Pick<Map<string, J | J[]>, 'get'>): string[] {
        const preamble: string[] = [];
        for (let i = 0; i < parameters.length; i++) {
            const type = this.parameterType(parameters[i].value, values);
            const typeString = typeof type === 'string' ? type : type && this.typeToString(type);
            if (typeString && typeString !== 'any') {
                preamble.push(`let ${PlaceholderUtils.PLACEHOLDER_PREFIX}${i}__: ${typeString};`);
            }
        }
        return preamble;
    }

    private static parameterType(param: any, values?: Pick<Map<string, J | J[]>, 'get'>): Type | string | undefined {
        // A Capture may be a Proxy, so it is recognised by its symbol
        if (param instanceof CaptureImpl || (param && typeof param === 'object' && param[CAPTURE_NAME_SYMBOL])) {
            return param[CAPTURE_TYPE_SYMBOL] ?? this.treeType(values?.get(param[CAPTURE_NAME_SYMBOL] || param.getName()));
        } else if (param instanceof CaptureValue) {
            // The root's declared type is the root's alone, not that of a property or element of it
            return values && this.treeType(param.resolve(values));
        } else if (param instanceof TemplateParamImpl) {
            return this.treeType(values?.get(param.name));
        }
        return this.treeType(param);
    }

    private static treeType(value: unknown): Type | undefined {
        // A declaration's type is that of what it declares, which is no value the placeholder holds
        return isTree(value) && (isExpression(value as J) || !isStatement(value as J)) ?
            TypedTree.getType(value as TypedTree) : undefined;
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
        parameters: Parameter[],
        expression: TemplateSlot
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
        return wrapCode(dedentTemplate(result), expression);
    }

    /**
     * Converts a Type instance to a TypeScript type string.
     *
     * @param type The Type instance
     * @param enclosing The types that `type` is a type argument, element type, member or bound of
     * @returns A TypeScript type string
     */
    private static typeToString(type: Type, enclosing: Type[] = []): string {
        return this.writeType(type, enclosing).text;
    }

    /** {@link typeToString}, with how tightly the type it writes binds, to parenthesize it by. */
    private static writeType(type: Type, enclosing: Type[]): WrittenType {
        // A recursive type, such as `type A = Array<A>`, is any where it holds itself
        if (enclosing.includes(type)) {
            return {text: 'any', binding: 'primary'};
        }
        const nested = [...enclosing, type];
        const primary = (text: string): WrittenType => ({text, binding: 'primary'});
        const typeArguments = (args: Type[]) => args.map(t => this.typeToString(t, nested)).join(', ');

        switch (type.kind) {
            case Type.Kind.Class:
            case Type.Kind.ShallowClass: {
                const classType = type as Type.Class;
                const name = this.className(classType);
                // A generic type the parser leaves without its arguments, as it does `Array<T>` where `T`
                // is a type variable, takes what each of its own type variables stands for
                return primary(!name ? 'any' :
                    classType.typeParameters?.length ? `${name}<${typeArguments(classType.typeParameters)}>` : name);
            }
            case Type.Kind.Parameterized: {
                // An array, a map or any other generic type, with its type arguments
                const {type: base, typeParameters} = type as Type.Parameterized;
                if (typeParameters.length === 0) {
                    return this.writeType(base, nested);
                }
                const name = this.className(base);
                return primary(name ? `${name}<${typeArguments(typeParameters)}>` : 'any');
            }
            case Type.Kind.Primitive:
                return primary(primitiveTypes.get((type as Type.Primitive).keyword) ?? 'any');
            case Type.Kind.Array: {
                const elemType = this.writeType((type as Type.Array).elemType, nested);
                return primary(`${elemType.binding === 'primary' ? elemType.text : `(${elemType.text})`}[]`);
            }
            case Type.Kind.Union: {
                // A member that could be anything makes the union so too
                const members = this.writeMembers((type as Type.Union).bounds, nested);
                const top = members.find(m => m.text === 'any') ?? members.find(m => m.text === 'unknown');
                return top ?? (members.length === 1 ? members[0] :
                    {text: members.map(m => m.text).join(' | '), binding: 'union'});
            }
            case Type.Kind.Intersection: {
                // A value of an intersection is a value of each member, so the members that can be written
                // still type it, where the rest would make it any
                const all = this.writeMembers((type as Type.Intersection).bounds, nested);
                const members = all.filter(m => m.text !== 'any' && m.text !== 'unknown');
                return members.length === 0 ? primary(all.some(m => m.text === 'any') ? 'any' : 'unknown') :
                    members.length === 1 ? members[0] : {
                        text: members.map(m => m.binding === 'union' ? `(${m.text})` : m.text).join(' & '),
                        binding: 'intersection'
                    };
            }
            case Type.Kind.GenericTypeVariable: {
                // No declaration can name a type variable of the code being edited, so it is what bounds
                // it, or unknown, as TypeScript reads an unconstrained one
                const bounds = (type as Type.GenericTypeVariable).bounds;
                return bounds.length === 0 ? primary('unknown') : this.writeType(bounds.length === 1 ? bounds[0] :
                    {kind: Type.Kind.Intersection, bounds} as Type.Intersection, nested);
            }
            default:
                // A method, a variable, an annotation or an unknown type
                return primary('any');
        }
    }

    /** The distinct members of a union or an intersection as TypeScript writes them. */
    private static writeMembers(bounds: Type[], enclosing: Type[]): WrittenType[] {
        const members = new Map<string, WrittenType>();
        for (const bound of bounds) {
            const member = this.writeType(bound, enclosing);
            members.set(member.text, member);
        }
        return [...members.values()];
    }

    /**
     * The name of a class, where it is a type reference. A module qualifies its types by where they come from
     * — `src/a.Foo` — and naming one takes a `context` statement of the caller's own, which `type` then names.
     */
    private static className(type: Type): string | undefined {
        const name = (type.kind === Type.Kind.Class || type.kind === Type.Kind.ShallowClass) ?
            (type as Type.Class).fullyQualifiedName : undefined;
        return name !== undefined && isTypeReference(name) ? name : undefined;
    }

    /**
     * Gets the parsed and extracted pattern tree, with placeholder identifiers left bare;
     * `attachCaptureMarkers` binds it to a particular set of captures.
     *
     * @param templateParts The string parts of the template
     * @param captures The captures between the string parts (can include RawCode)
     * @param contextStatements Context declarations (imports, types, etc.) to prepend for type attribution
     * @param dependencies NPM dependencies for type attribution
     * @param expression Whether to parse the code as an expression
     * @returns A Promise resolving to the extracted pattern AST
     */
    static async getPatternTree(
        templateParts: TemplateStringsArray,
        captures: (Capture | Any | RawCode)[],
        contextStatements: string[] = [],
        dependencies: Record<string, string> = {},
        types?: string[],
        expression: boolean = false
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
        const templateString = wrapCode(result, expression);

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
        return PlaceholderUtils.extractFromWrapper(lastStatement, 'Pattern', expression);
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

    /**
     * Promotes a variadic capture's marker from a shorthand property's name to the property, so
     * that `{${props}}` matches the object literal's properties as a sequence. A scalar capture
     * stays on the name, binding the identifier a shorthand property consists of.
     */
    protected override async visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, p: undefined): Promise<J | undefined> {
        const visited = await super.visitPropertyAssignment(propertyAssignment, p) as JS.PropertyAssignment;
        const nameMarker = visited.initializer === undefined ? PlaceholderUtils.getCaptureMarker(visited.name) : undefined;
        if (!nameMarker?.variadicOptions) {
            return visited;
        }
        return updateIfChanged(visited, {
            name: {
                ...visited.name,
                markers: {...visited.name.markers, markers: visited.name.markers.markers.filter(m => m !== nameMarker)}
            },
            markers: {
                ...visited.markers,
                markers: [...visited.markers.markers, nameMarker]
            },
        });
    }
}

// FIXME: This is a heuristic to determine if the parent expects a statement child
function expectsStatement(parentTree: J, childId: string): boolean {
    if (parentTree.kind === J.Kind.Case) {
        // the labels of a case are expressions
        return !(parentTree as J.Case).caseLabels.elements.some(label => label.element.id === childId);
    }
    return parentTree.kind === J.Kind.Block ||
        parentTree.kind === J.Kind.DoWhileLoop ||
        parentTree.kind === J.Kind.ForEachLoop ||
        parentTree.kind === J.Kind.ForLoop ||
        parentTree.kind === J.Kind.If ||
        parentTree.kind === J.Kind.IfElse ||
        parentTree.kind === J.Kind.Label ||
        parentTree.kind === J.Kind.WhileLoop ||
        parentTree.kind === JS.Kind.CompilationUnit ||
        parentTree.kind === JS.Kind.ForInLoop ||
        parentTree.kind === JS.Kind.WithStatement;
}

/** Whether the code opens with `{`, which is a block where a statement goes and an object literal elsewhere. */
export function opensWithBrace(templateParts: TemplateStringsArray): boolean {
    return templateParts[0].trimStart().startsWith('{');
}

/** Whether code opening with `{` that replaces `tree`, which `cursor` points at, is an object literal. */
export async function replacedByObjectLiteral(tree: J, cursor: Cursor): Promise<boolean> {
    if (tree.kind === J.Kind.Block) {
        return false;
    } else if (!isStatement(tree)) {
        return true;
    }
    const parentTree = await replacedParent(tree, cursor);
    return parentTree !== undefined && !expectsStatement(parentTree, tree.id);
}

/** The trees templates made, each in place of the tree it replaced, even where it keeps the id of a capture. */
const templateResults = new WeakSet<J>();

/**
 * The tree that holds `tree`. The cursor points at `tree`, at what it stands in for, or at a tree that
 * holds it, as where a recipe applies a template to a child of the node it visits.
 */
async function replacedParent(tree: J, cursor: Cursor | undefined): Promise<J | undefined> {
    const owner = cursor?.value;
    let holder: J | undefined;
    if (isTree(owner) && owner.id !== tree.id) {
        // A chained rule's result stands in for the owner, though it can keep the id of a capture inside it
        const held = (t: J) => t === tree || (t.id === tree.id && !templateResults.has(tree));
        await new class extends JavaScriptVisitor<undefined> {
            protected override async preVisit(t: J, p: undefined): Promise<J | undefined> {
                if (holder === undefined && held(t)) {
                    holder = this.cursor.parentTree()?.value;
                }
                if (holder !== undefined) {
                    this.stopAfterPreVisit();
                }
                return t;
            }
        }().visit(owner, undefined);
    }
    return holder ?? cursor?.parentTree()?.value;
}

/** The comments inside `tree`, other than on its own prefix. */
function innerComments(tree: J): J.Space["comments"][number][] {
    const found: J.Space["comments"][number][] = [];
    const collect = (node: unknown, top: boolean) => {
        if (!node || typeof node !== 'object') {
            return;
        }
        if (Array.isArray(node)) {
            node.forEach(n => collect(n, false));
            return;
        }
        for (const [key, value] of Object.entries(node)) {
            if (key === 'type' || key === 'markers' || top && key === 'prefix') {
                continue;
            }
            if ((value as {kind?: string} | undefined)?.kind === J.Kind.Space) {
                found.push(...(value as J.Space).comments);
            } else {
                collect(value, false);
            }
        }
    };
    collect(tree, true);
    return found;
}

/**
 * Helper class for applying a template to an AST.
 */
export class TemplateApplier {
    constructor(
        private readonly cursor: Cursor,
        private readonly coordinates: JavaCoordinates,
        private readonly ast: J,
        private readonly shouldFormat: boolean = true,
        private readonly substituted: boolean = false
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
            return this.substituted ? new SubstitutedLayoutVisitor().visit(this.ast, undefined) : this.ast;
        }

        const originalTree = tree as J;
        const parentTree = await replacedParent(originalTree, this.cursor);
        const typeSlot = slotOf(originalTree, this.cursor) === 'type';
        // A replaced node spliced back in as a capture carries its own trailing markers. A type's `?` marks
        // the tuple element it fills instead, so `${t}<any>` replacing `X` in `[X?]` gives `[X<any>?]`.
        const trailing = trailingMarkers(originalTree);
        const spliced = (await treeIds(this.ast)).has(originalTree.id);
        const ast = trailing && spliced && typeSlot ? await withoutTrailingMarkers(this.ast, originalTree.id) : this.ast;
        let resultToUse = this.wrapTree(originalTree, ast, parentTree);
        const slotMarkers = trailing && (typeSlot || !spliced) ? trailing : undefined;
        if (slotMarkers) {
            resultToUse = {...resultToUse, markers: withMarkers(resultToUse.markers, slotMarkers)};
        }
        // A comment inside a replaced type, `R./* why */ReactChild`, stays with what replaces it
        const inner = typeSlot ? innerComments(originalTree) : [];
        if (typeSlot) {
            // A type replacing a type: parenthesized by the type slot's own rules, not an expression's
            resultToUse = maybeParenthesizeType(parentTree, originalTree.id, {...resultToUse, prefix: originalTree.prefix});
        } else {
            const slot = this.replacedSlot(originalTree, parentTree);
            // `format` substitutes the target's prefix, so decide against the prefix that will print
            resultToUse = maybeParenthesize(slot?.[0], slot?.[1] ?? originalTree.id,
                {...resultToUse, prefix: originalTree.prefix}, slotMarkers);
        }
        let result = await this.format(resultToUse, originalTree);
        if (inner.length) {
            result = {...result, prefix: {...result.prefix, comments: [...result.prefix.comments, ...inner]}};
        }
        templateResults.add(result);
        return result;
    }

    /** The node holding the tree being replaced and the id it holds it under. */
    private replacedSlot(originalTree: J, parentTree: J | undefined): [J, string] | undefined {
        if (parentTree === undefined) {
            return undefined;
        } else if (requiredPrecedence(parentTree, originalTree.id) !== undefined) {
            return [parentTree, originalTree.id];
        }
        // A chained rule rewrites the previous rule's detached result, leaving the cursor on the node
        // that result stands in for; a wrong guess here only ever adds parentheses, never drops them
        const standIn = this.cursor?.value;
        return isTree(standIn) && standIn.id !== originalTree.id &&
            requiredPrecedence(parentTree, standIn.id) !== undefined ? [parentTree, standIn.id] : undefined;
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
            const unmarked = this.substituted ? await new SubstitutedLayoutVisitor().visit<J>(result, undefined) : result;
            return {...unmarked!, id: resultToUse.id};
        }

        // A recipe can apply a template to a child while its cursor still points at the
        // owning node (for example, a JSX attribute's literal value). Keep that owner
        // in the formatting path; dropping it loses the attribute's line indentation.
        const owner = this.cursor?.value;
        const parent = isTree(owner) && owner.id !== originalTree.id &&
            (await treeIds(owner as J)).has(originalTree.id) ? this.cursor : this.cursor?.parent;
        const formatted =
            await maybeAutoFormat(originalTree, result, null, undefined, parent);
        // A value Prettier lays out keeps Prettier's layout
        let laidOut = formatted;
        if (this.substituted) {
            const prettierStyle = getPrettierStyle(result, parent);
            const prettierLaysOut = prettierStyle !== undefined && !prettierStyle.ignored;
            const restore = new SubstitutedLayoutVisitor(prettierLaysOut ? undefined : await substitutedValues(result));
            laidOut = (await restore.visit<J>(formatted, undefined))!;
            if (restore.restored) {
                laidOut = await autoIndent(laidOut, null, parent);
            }
        }

        // Restore the original ID
        return {...laidOut!, id: resultToUse.id};
    }

    private wrapTree(originalTree: J, resultToUse: J, parentTree: J | undefined) {
        // A `for` head holds each expression of its init and update in an expression statement, as the parser does
        if (parentTree?.kind === J.Kind.ForLoopControl &&
            (parentTree as J.ForLoop.Control).condition?.element.id !== originalTree.id) {
            return isExpression(resultToUse) && resultToUse.kind !== JS.Kind.ExpressionStatement ? {
                kind: JS.Kind.ExpressionStatement,
                id: randomId(),
                prefix: resultToUse.prefix,
                markers: wrapperMarkers(resultToUse),
                expression: {...resultToUse, prefix: emptySpace}
            } as JS.ExpressionStatement : resultToUse;
        }

        // Only apply wrapping logic if we have parent context
        if (parentTree) {
            const parentExpectsStatement = expectsStatement(parentTree, originalTree.id);
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
                        markers: wrapperMarkers(expression),
                        expression: { ...expression, prefix: emptySpace }
                    } as JS.ExpressionStatement;
                }
            } else if (!parentExpectsStatement && isExpression(originalTree)) {
                // Expression context: wrap in StatementExpression if result is statement-only. A slot that held
                // a statement-only node, such as an arrow function's block body, takes one unwrapped.
                if (resultIsStatement && !resultIsExpression) {
                    const stmt = resultToUse as Statement;
                    resultToUse = {
                        kind: JS.Kind.StatementExpression,
                        id: randomId(),
                        prefix: stmt.prefix,
                        markers: wrapperMarkers(stmt),
                        statement: { ...stmt, prefix: emptySpace }
                    } as JS.StatementExpression;
                }
            }
        }
        return resultToUse;
    }
}

/**
 * The trailing markers of the node a template replaces. Each prints after whatever node fills the slot,
 * so a pattern does not match on it and the replacement keeps it.
 */
function trailingMarkers(replaced: J): Markers | undefined {
    const trailing = replaced.markers.markers.filter(isTrailingMarker);
    return trailing.length === 0 ? undefined : markers(...trailing);
}

/** `tree` with the trailing markers taken off the node `id` names. */
async function withoutTrailingMarkers(tree: J, id: string): Promise<J> {
    return (await new class extends JavaScriptVisitor<undefined> {
        protected override async preVisit(t: J): Promise<J | undefined> {
            if (t.id !== id) {
                return t;
            }
            this.stopAfterPreVisit();
            return {...t, markers: {...t.markers, markers: t.markers.markers.filter(m => !isTrailingMarker(m))}};
        }
    }().visit<J>(tree, undefined))!;
}

/** `own`, plus each marker of `added` whose kind `own` does not already hold. */
function withMarkers(own: Markers, added: Markers): Markers {
    const missing = added.markers.filter(m => !own.markers.some(o => o.kind === m.kind));
    return missing.length === 0 ? own : {...own, markers: [...own.markers, ...missing]};
}

/** The markers of a node wrapping `wrapped`, less the {@link SubstitutedValue} that belongs to `wrapped` alone. */
function wrapperMarkers(wrapped: J): J['markers'] {
    return {...wrapped.markers, markers: wrapped.markers.markers.filter(m => m.kind !== SubstitutedValue.KIND)};
}

/** The substituted values in `tree`, by the id of the {@link SubstitutedValue} marking each. */
async function substitutedValues(tree: J): Promise<Map<string, J>> {
    const values = new Map<string, J>();
    await new class extends JavaScriptVisitor<undefined> {
        protected override async preVisit(t: J): Promise<J | undefined> {
            const marker = findMarker<SubstitutedValue>(t, SubstitutedValue.KIND);
            if (marker) {
                values.set(marker.id, t);
            }
            return t;
        }
    }().visit(tree, undefined);
    return values;
}

/**
 * Puts each substituted value back as it was in `unformatted`, under the prefix its marker assigns it,
 * and drops its {@link SubstitutedValue} marker. Re-indenting them is left to the indent pass.
 */
class SubstitutedLayoutVisitor extends JavaScriptVisitor<undefined> {
    /** Whether any value was put back. */
    restored = false;

    constructor(private readonly unformatted: Map<string, J> = new Map()) {
        super();
    }

    override async visit<R extends J>(tree: Tree, p: undefined, parent?: Cursor): Promise<R | undefined> {
        const marker = isTree(tree) ? findMarker<SubstitutedValue>(tree as J, SubstitutedValue.KIND) : undefined;
        if (!marker) {
            return super.visit(tree, p, parent);
        }
        const formatted = tree as J;
        const before = this.unformatted.get(marker.id);
        this.restored ||= before !== undefined;
        const value = before ? {...before, prefix: marker.ownPrefix ? before.prefix : formatted.prefix} : formatted;
        return {
            ...value,
            markers: {...value.markers, markers: value.markers.markers.filter(m => m !== marker)}
        } as J as R;
    }
}
