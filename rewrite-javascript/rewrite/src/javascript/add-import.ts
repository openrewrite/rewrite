import {JavaScriptVisitor} from "./visitor";
import {ElementRemovalFormatter, emptySpace, isIdentifier, J, NameTree, rightPadded, singleSpace, space, Statement, Type} from "../java";
import {JS, JSX} from "./tree";
import {randomId, UUID} from "../uuid";
import {emptyMarkers, markers} from "../markers";
import {getStyle, SpacesStyle, StyleKind} from "./style";
import {bindingNames, compilationUnitOf, cursorOf, declarationsOf, deconflict, isValueReference, namesDeclaredIn, scopeOf, walk} from "./scope";
import {create as produce, Draft} from "mutative";
import {TypeVisitor} from "../java/type-visitor";
import {autoFormat} from "./format";
import {getPrettierStyle} from "./format/prettier-format";

export type QuoteChar = "'" | '"';

export enum ImportStyle {
    ES6Named,      // import { x } from 'module'
    ES6Namespace,  // import * as x from 'module'
    ES6Default,    // import x from 'module'
    CommonJS       // const x = require('module')
}

export interface AddImportOptions {
    /** The module name (e.g., 'fs', 'react') to import from.
     * Pass a `J.Literal` to reuse its source form, which carries the escapes and unicode form of a
     * specifier being moved from elsewhere in the source. A Prettier configuration settles the
     * quoting; see `quoteStyle`. */
    module: string | J.Literal;

    /** Optionally, the specific member to import from the module.
     * If not specified, adds a default import or namespace import.
     * Special values:
     * - 'default': Adds a default import from the module.
     *   When using 'default', the `alias` parameter is required.
     * - '*': Adds a namespace import (import * as alias from 'module').
     *   When using '*', the `alias` parameter is required.
     * Cannot be combined with `sideEffectOnly`. */
    member?: string;

    /** Optional alias for the imported member.
     * Required when member is 'default' or '*'.
     * Taken verbatim, never deconflicted: a caller that names an alias may already have
     * emitted code using it.
     * Cannot be combined with `sideEffectOnly`. */
    alias?: string;

    /** Preferred local name, deconflicted if the file already binds it. Stands in for `alias` where
     * `member` is 'default' or '*' and the caller wants a name it does not insist on.
     * Cannot be combined with `sideEffectOnly`. */
    preferredName?: string;

    /** If true, only add the import if the member is actually used in the file. Default: true
     * Cannot be combined with `sideEffectOnly`. */
    onlyIfReferenced?: boolean;

    /** If true, adds a side-effect import without bindings (e.g., `import 'module'` or `require('module')`).
     * Cannot be combined with `member`, `alias`, or `onlyIfReferenced`. */
    sideEffectOnly?: boolean;

    /** If true, adds a type-only import (e.g., `import type { Foo } from 'module'`).
     * Type-only imports are erased at compile time and do not generate runtime code.
     * Cannot be combined with `sideEffectOnly`. */
    typeOnly?: boolean;

    /** Optional import style to use. If not specified, auto-detects from file and existing imports */
    style?: ImportStyle;

    /** Quote character for the module specifier. If not specified, detected from the file.
     * A `J.Literal` module carries its own quoting and takes precedence over this. A Prettier
     * configuration outranks both, since running Prettier would arrive at its quote anyway. */
    quoteStyle?: QuoteChar;
}

/**
 * Ensures an import for `options.module` exists, queuing an `AddImport` edit where none already
 * serves the request. `maybeBind`'s ESM/CommonJS lane is this function; its own JSDoc carries the
 * return-value contract. `refuseCreate` answers `undefined` instead of queuing a new import,
 * without affecting whether an existing binding answers the request first.
 */
export function bindImport(
    visitor: JavaScriptVisitor<any>,
    options: AddImportOptions,
    refuseCreate?: boolean
): string | undefined {
    validate(options);
    const module = moduleNameOf(options.module);
    const sideEffectOnly = options.sideEffectOnly ?? false;
    const typeOnly = options.typeOnly ?? false;

    // A queued import binds the module as much as one already in the file, so it answers a later
    // request the same way; a name a merged request never emits would be referenced but not bound.
    for (const v of visitor.afterVisit || []) {
        if (!(v instanceof AddImport) || v.module !== module ||
            v.sideEffectOnly !== sideEffectOnly || v.typeOnly !== typeOnly) {
            continue;
        }
        // How a specifier prints, or what name would be nice, does not say which binding is wanted.
        // A pinned alias does: it asks for a binding of its own, which only the same request answers.
        const answers = options.alias
            ? v.alias === options.alias && v.member === options.member
            : memberName(v.member) === memberName(options.member);
        if (answers) {
            return v.bindingName;
        }
    }

    if (sideEffectOnly) {
        if (refuseCreate) {
            return undefined;
        }
        visitor.afterVisit.push(new AddImport(options));
        return undefined;
    }

    // A pinned alias is the whole answer, so the file has no say and nothing below needs to read it.
    if (options.alias) {
        if (refuseCreate) {
            return undefined;
        }
        visitor.afterVisit.push(new AddImport(options, options.alias));
        return options.alias;
    }

    const derived = derivedName(options);
    const cursor = cursorOf(visitor);
    const cu = cursor && compilationUnitOf(cursor);
    if (!cu) {
        if (refuseCreate) {
            return undefined;
        }
        visitor.afterVisit.push(new AddImport(options, derived));
        return derived;
    }

    // An import already serving this request answers it, under whatever name the file gave it —
    // only a pinned alias, returned above, asks for a binding of its own. Queuing a second import
    // would, on the next cycle, derive a suffixed name from the binding this call just added.
    const scope = scopeOf(cursor);
    for (const binding of moduleScopeBindings(cu)) {
        if (binding.module === module && binding.member === memberName(options.member) &&
            binding.typeOnly === typeOnly && scope.declaringScope(binding.name) === cu) {
            return binding.name;
        }
    }

    if (refuseCreate) {
        return undefined;
    }

    // Only the module scope answers for a name, but any scope in the file occupies one. The queue
    // gives every later request for this module the name chosen here, so it has to clear the scopes
    // those references will sit in, which are not known yet.
    const taken = takenNames(namesDeclaredIn(cu), visitor);
    const name = deconflict(derived, candidate => taken.has(candidate));
    visitor.afterVisit.push(new AddImport(options, name));
    return name;
}

/** Rejects a request on its own terms, whatever the queue already holds. */
function validate(options: AddImportOptions): void {
    // Validate that a name is provided when member is 'default'
    if (options.member === 'default' && !options.alias && !options.preferredName) {
        throw new Error("When member is 'default', the alias parameter is required");
    }

    // Validate that a name is provided when member is '*' (namespace import)
    if (options.member === '*' && !options.alias && !options.preferredName) {
        throw new Error("When member is '*', the alias parameter is required");
    }

    // Validate that sideEffectOnly is not combined with incompatible options
    if (options.sideEffectOnly) {
        if (options.member !== undefined) {
            throw new Error("Cannot combine sideEffectOnly with member");
        }
        if (options.alias !== undefined) {
            throw new Error("Cannot combine sideEffectOnly with alias");
        }
        if (options.preferredName !== undefined) {
            throw new Error("Cannot combine sideEffectOnly with preferredName");
        }
        if (options.onlyIfReferenced !== undefined) {
            throw new Error("Cannot combine sideEffectOnly with onlyIfReferenced");
        }
        if (options.typeOnly) {
            throw new Error("Cannot combine sideEffectOnly with typeOnly");
        }
    }
}

/**
 * A request that named no local name and asks for no member of the module. Having expressed no
 * preference it has none to disappoint, so whatever the file already calls that module answers it.
 */
function anyNameAnswers(options: AddImportOptions): boolean {
    return !options.sideEffectOnly && memberName(options.member) === undefined &&
        options.alias === undefined && options.preferredName === undefined;
}

/** The local name a request asks for, before the file has a say in it. */
function derivedName(options: AddImportOptions): string {
    return options.alias ?? options.preferredName ?? memberName(options.member) ?? moduleNameOf(options.module);
}

/** `'default'` and an absent member both name a default import, which binds no member name of its own. */
export function memberName(member: string | undefined): string | undefined {
    return member === 'default' ? undefined : member;
}

/** A name introduced into the file's module scope, and where it came from when that is an import. */
export interface ModuleScopeBinding {
    name: string;
    module?: string;
    /** Carries the {@link AddImportOptions.member} spelling, so `undefined` means a default import. */
    member?: string;
    typeOnly?: boolean;
}

/** What the file's imports and `require`s bind at module scope, and the module each name comes from. */
export function moduleScopeBindings(cu: JS.CompilationUnit): ModuleScopeBinding[] {
    const bindings: ModuleScopeBinding[] = [];

    const declaredByVariables = (varDecl: J.VariableDeclarations): void => {
        for (const variable of varDecl.variables) {
            const required = requiredModule(variable.element?.initializer?.element);
            if (required !== undefined) {
                bindings.push(...requireBindings(variable.element?.name, required));
            }
        }
    };

    for (const stmt of cu.statements) {
        const statement = stmt.element;
        if (statement?.kind === JS.Kind.Import) {
            bindings.push(...importBindings(statement as JS.Import));
            continue;
        }
        for (const declaration of declarationsOf(statement)) {
            declaredByVariables(declaration);
        }
    }

    return bindings;
}

/** Whether a top-level statement already marks the file as a module: an import or any export form. */
export function hasEsmSyntax(cu: JS.CompilationUnit): boolean {
    return cu.statements.some(stmt => {
        const element = stmt.element;
        // `export {a, b}`, `export * from` and `export default` are their own statement kinds;
        // `export class`/`function`/`const` instead carry `export` as a modifier on the
        // declaration itself, the same way TypeScript's own AST models it.
        const modifiers = (element as {modifiers?: J.Modifier[]} | undefined)?.modifiers;
        return element?.kind === JS.Kind.Import ||
            element?.kind === JS.Kind.ExportDeclaration ||
            element?.kind === JS.Kind.ExportAssignment ||
            (modifiers?.some(m => m.keyword === "export") ?? false);
    }) || hasTopLevelAwait(cu);
}

/**
 * Whether a statement holds an `await` outside any function of its own — legal only at module
 * top level, unlike an `await` inside an `async function`, which says nothing about the file.
 */
function hasTopLevelAwait(cu: JS.CompilationUnit): boolean {
    let found = false;
    walk(cu.statements, node => {
        if (found) {
            return false;
        }
        if (node.kind === JS.Kind.Await) {
            found = true;
            return false;
        }
        return node.kind !== J.Kind.MethodDeclaration && node.kind !== J.Kind.Lambda;
    });
    return found;
}

/** Whether the file binds its modules with `require`, which decides whether a create is possible. */
export function isCommonJs(cu: JS.CompilationUnit): boolean {
    if (cu.sourcePath.endsWith(".cjs") || cu.sourcePath.endsWith(".cts")) {
        return true;
    }
    // Node treats these as ES modules regardless of what they contain, the same way
    // `determineImportStyle` reads them as ES6-preferring; `.js`/`.ts`/`.tsx` stay ambiguous and
    // fall through to the statements below.
    if (cu.sourcePath.endsWith(".mjs") || cu.sourcePath.endsWith(".mts")) {
        return false;
    }
    if (hasEsmSyntax(cu)) {
        return false;
    }
    return cu.statements.some(stmt =>
        declarationsOf(stmt.element).some(d => requiredModuleOfDeclaration(d) !== undefined));
}

/** The module a `const X = require('m')` declaration names, for the one variable it declares. */
export function requiredModuleOfDeclaration(declaration: J.VariableDeclarations): string | undefined {
    return declaration.variables.length === 1
        ? requiredModule(declaration.variables[0].element?.initializer?.element)
        : undefined;
}

/** The module a `require('...')` initializer names, `undefined` for an initializer that is anything else. */
function requiredModule(initializer: J | undefined): string | undefined {
    return initializer?.kind === J.Kind.MethodInvocation
        ? requiredModuleOf(initializer as J.MethodInvocation)
        : undefined;
}

/** A `require(...)` call, whatever it is passed. `obj.require('x')` selects a method rather than loading a module. */
function isRequireCall(methodInv: J.MethodInvocation): boolean {
    return !methodInv.select && methodInv.name?.kind === J.Kind.Identifier &&
        (methodInv.name as J.Identifier).simpleName === 'require';
}

/**
 * The module a `require(...)` call loads. `obj.require('x')` selects a method rather than loading a
 * module, and a specifier that is not a string literal names none that can be read here.
 */
export function requiredModuleOf(methodInv: J.MethodInvocation): string | undefined {
    if (!isRequireCall(methodInv)) {
        return undefined;
    }
    const argument = methodInv.arguments?.elements[0]?.element;
    return argument?.kind === J.Kind.Literal && typeof (argument as J.Literal).value === 'string'
        ? moduleNameOf(argument as J.Literal)
        : undefined;
}

/** A `require` binds a module the way an import does, so the pool records it the same way. */
function requireBindings(pattern: J | undefined, module: string): ModuleScopeBinding[] {
    // A whole-module require binds no member, exactly as a default import does.
    if (pattern?.kind === J.Kind.Identifier) {
        return [{name: (pattern as J.Identifier).simpleName, module, member: undefined, typeOnly: false}];
    }
    return bindingNames(pattern).map(bound => bound.member === undefined
        ? {name: bound.name}
        : {name: bound.name, module, member: bound.member, typeOnly: false});
}

/** The member a specifier imports and the name it binds it under, which are the same absent an alias. */
function specifierBinding(specifier: JS.ImportSpecifier): { name: string; member: string } | undefined {
    if (specifier.specifier?.kind === J.Kind.Identifier) {
        const name = (specifier.specifier as J.Identifier).simpleName;
        return {name, member: name};
    }
    if (specifier.specifier?.kind !== JS.Kind.Alias) {
        return undefined;
    }
    const alias = specifier.specifier as JS.Alias;
    return alias.propertyName?.element?.kind === J.Kind.Identifier && alias.alias?.kind === J.Kind.Identifier
        ? {name: (alias.alias as J.Identifier).simpleName, member: (alias.propertyName.element as J.Identifier).simpleName}
        : undefined;
}

function importBindings(jsImport: JS.Import): ModuleScopeBinding[] {
    const moduleSpecifier = jsImport.moduleSpecifier?.element;
    const module = moduleSpecifier?.kind === J.Kind.Literal
        ? moduleNameOf(moduleSpecifier as J.Literal)
        : undefined;
    const importClause = jsImport.importClause;
    if (!importClause) {
        return [];
    }

    const typeOnly = importClause.typeOnly;
    const bindings: ModuleScopeBinding[] = [];

    if (importClause.name?.element?.kind === J.Kind.Identifier) {
        bindings.push({name: (importClause.name.element as J.Identifier).simpleName, module, member: undefined, typeOnly});
    }

    const namedBindings = importClause.namedBindings;
    if (namedBindings?.kind === J.Kind.Identifier) {
        bindings.push({name: (namedBindings as J.Identifier).simpleName, module, member: '*', typeOnly});
    } else if (namedBindings?.kind === JS.Kind.Alias) {
        const alias = (namedBindings as JS.Alias).alias;
        if (alias?.kind === J.Kind.Identifier) {
            bindings.push({name: (alias as J.Identifier).simpleName, module, member: '*', typeOnly});
        }
    } else if (namedBindings?.kind === JS.Kind.NamedImports) {
        for (const elem of (namedBindings as JS.NamedImports).elements.elements) {
            const bound = elem.element?.kind === JS.Kind.ImportSpecifier
                ? specifierBinding(elem.element as JS.ImportSpecifier)
                : undefined;
            if (bound) {
                bindings.push({...bound, module, typeOnly});
            }
        }
    }

    return bindings;
}

/**
 * `names`, plus those pending `AddImport`s on the `afterVisit` queue have claimed. A queued
 * `RemoveImport` does not free one: it removes only what the file leaves unused, and binding a name
 * it keeps is an error, where an unnecessary suffix merely reads oddly.
 */
export function takenNames(names: ReadonlySet<string>, visitor: JavaScriptVisitor<any>): Set<string> {
    const taken = new Set<string>(names);

    for (const v of visitor.afterVisit || []) {
        if (v instanceof AddImport && v.bindingName) {
            taken.add(v.bindingName);
        }
    }

    return taken;
}

/** As {@link takenNames}, for a caller asking about one name rather than probing repeatedly. */
export function nameTaken(name: string, names: ReadonlySet<string>, visitor: JavaScriptVisitor<any>): boolean {
    return names.has(name) ||
        (visitor.afterVisit || []).some(v => v instanceof AddImport && v.bindingName === name);
}

/**
 * The parser lifts surrogate pairs out of `valueSource` into `unicodeEscapes` and, when it does,
 * sets `value` to the quoted source (`parser.ts` `mapLiteral`), so neither field alone is the
 * module name. Reuniting them and stripping the quotes yields it for either shape of literal.
 */
export function moduleNameOf(module: string | J.Literal): string {
    if (typeof module === 'string') {
        return module;
    }
    const source = module.valueSource;
    if (source === undefined) {
        return String(module.value);
    }
    let restored = '';
    let cut = 0;
    for (const escape of module.unicodeEscapes ?? []) {
        restored += source.slice(cut, escape.valueSourceIndex) + String.fromCharCode(parseInt(escape.codePoint, 16));
        cut = escape.valueSourceIndex;
    }
    restored += source.slice(cut);
    const quote = restored.charAt(0);
    return (quote === "'" || quote === '"') && restored.endsWith(quote) && restored.length > 1
        ? restored.slice(1, -1)
        : restored;
}

function quoteOf(expression: J | undefined): QuoteChar | undefined {
    if (expression?.kind !== J.Kind.Literal) {
        return undefined;
    }
    const source = (expression as J.Literal).valueSource;
    const quote = source?.charAt(0);
    // JsxText carries raw text as its `valueSource`, so a leading quote alone does not make a
    // literal quote-delimited; requiring a matching pair keeps prose out of the tally.
    return (quote === "'" || quote === '"') && source!.length > 1 && source!.endsWith(quote)
        ? quote
        : undefined;
}

/**
 * Pick the quote character for a module specifier being added to `cu`, so that it agrees
 * with the file it lands in. An existing specifier is the most direct precedent; a Prettier
 * config states intent even where the file itself has not been formatted to match.
 */
async function detectQuote(cu: JS.CompilationUnit): Promise<QuoteChar> {
    // Static imports are top-level, so the highest-ranked signal needs no traversal to find.
    for (const statement of cu.statements) {
        const element = statement.element;
        const topLevelQuote = element?.kind === JS.Kind.Import
            ? quoteOf((element as JS.Import).moduleSpecifier?.element)
            : element?.kind === JS.Kind.ExportDeclaration
                ? quoteOf((element as JS.ExportDeclaration).moduleSpecifier?.element)
                : undefined;
        if (topLevelQuote) {
            return topLevelQuote;
        }
    }

    let importQuote: QuoteChar | undefined;
    let requireQuote: QuoteChar | undefined;
    let single = 0;
    let double = 0;

    await new class extends JavaScriptVisitor<void> {
        override async visitImportDeclaration(jsImport: JS.Import, p: void): Promise<J | undefined> {
            importQuote ??= quoteOf(jsImport.moduleSpecifier?.element);
            return super.visitImportDeclaration(jsImport, p);
        }

        override async visitExportDeclaration(exportDeclaration: JS.ExportDeclaration, p: void): Promise<J | undefined> {
            importQuote ??= quoteOf(exportDeclaration.moduleSpecifier?.element);
            return super.visitExportDeclaration(exportDeclaration, p);
        }

        override async visitMethodInvocation(method: J.MethodInvocation, p: void): Promise<J | undefined> {
            if (!method.select && method.name?.kind === J.Kind.Identifier &&
                (method.name as J.Identifier).simpleName === 'require') {
                requireQuote ??= quoteOf(method.arguments?.elements[0]?.element);
            }
            return super.visitMethodInvocation(method, p);
        }

        override async visitJsxAttribute(attribute: JSX.Attribute, p: void): Promise<J | undefined> {
            // JSX attribute quoting answers to Prettier's `jsxSingleQuote`, so it is no evidence
            // about the quote style of ordinary strings.
            return attribute;
        }

        override async visitLiteral(literal: J.Literal, p: void): Promise<J | undefined> {
            const quote = quoteOf(literal);
            if (quote === "'") {
                single++;
            } else if (quote === '"') {
                double++;
            }
            return super.visitLiteral(literal, p);
        }
    }().visit(cu, undefined);

    // Imports outrank requires; that ordering is what makes the import-only scan above sound.
    const specifierQuote = importQuote ?? requireQuote;
    if (specifierQuote) {
        return specifierQuote;
    }

    const prettier = getPrettierStyle(cu);
    if (prettier && !prettier.ignored) {
        const singleQuote = prettier.config.singleQuote;
        if (typeof singleQuote === 'boolean') {
            return singleQuote ? "'" : '"';
        }
    }

    return double > single ? '"' : "'";
}

/**
 * Lays out the import statement `AddImport` wrote, so that the file's own style — a Prettier
 * configuration's brace spacing and print width, say — reaches it.
 */
async function formatImport<P>(cu: JS.CompilationUnit, id: UUID, p: P): Promise<JS.CompilationUnit> {
    return await new class extends JavaScriptVisitor<P> {
        override async visitImportDeclaration(jsImport: JS.Import, p: P): Promise<J | undefined> {
            return jsImport.id === id ? await autoFormat(jsImport, p, undefined, this.cursor.parent) : jsImport;
        }
    }().visit(cu, p) as JS.CompilationUnit;
}

/**
 * As {@link formatImport}, for an import that gained specifiers. Short of a Prettier configuration
 * deciding the whole line, the statement's own text says more about its spacing than a
 * repository-wide style does, and the merge reads it directly.
 */
async function formatMergedImport<P>(cu: JS.CompilationUnit, id: UUID, p: P): Promise<JS.CompilationUnit> {
    return getPrettierStyle(cu)?.ignored === false ? formatImport(cu, id, p) : cu;
}

/**
 * Adds the import {@link AddImportOptions} describes, under the name derived there. An import
 * already in the file answers where it binds that same name, or where the request expressed no
 * preference at all. Reuse under some other name belongs to {@link bindImport}, whose caller
 * learns which name came back; a recipe driving this visitor is stuck with the one it derived.
 */
export class AddImport<P> extends JavaScriptVisitor<P> {
    readonly module: string;
    /** Set when the caller supplied the module specifier as a literal; printed verbatim. */
    readonly moduleValueSource?: string;
    readonly moduleUnicodeEscapes?: J.LiteralUnicodeEscape[];
    readonly member?: string;
    readonly alias?: string;
    readonly onlyIfReferenced: boolean;
    readonly sideEffectOnly: boolean;
    readonly typeOnly: boolean;
    readonly style?: ImportStyle;
    readonly quoteStyle?: QuoteChar;
    /** The local name this import binds; `undefined` for a side-effect import. */
    readonly bindingName?: string;
    /**
     * A default import the request named no local name for, so whatever the file already calls
     * that module answers it.
     */
    readonly anyNameAnswers: boolean;

    constructor(options: AddImportOptions, bindingName?: string) {
        super();

        validate(options);

        this.module = moduleNameOf(options.module);
        this.moduleValueSource = typeof options.module === 'string' ? undefined : options.module.valueSource;
        this.moduleUnicodeEscapes = typeof options.module === 'string' ? undefined : options.module.unicodeEscapes;
        this.member = options.member;
        this.alias = options.alias;
        this.onlyIfReferenced = options.onlyIfReferenced ?? true;
        this.sideEffectOnly = options.sideEffectOnly ?? false;
        this.typeOnly = options.typeOnly ?? false;
        this.style = options.style;
        this.quoteStyle = options.quoteStyle;
        this.bindingName = this.sideEffectOnly ? undefined : bindingName ?? derivedName(options);
        this.anyNameAnswers = anyNameAnswers(options);
    }

    /**
     * Extract module name from a module specifier literal
     */
    private getModuleName(moduleSpecifier: J): string | undefined {
        if (moduleSpecifier.kind !== J.Kind.Literal) {
            return undefined;
        }
        return moduleNameOf(moduleSpecifier as J.Literal);
    }


    /**
     * Determine the appropriate import style based on file type and existing imports
     */
    private determineImportStyle(compilationUnit: JS.CompilationUnit): ImportStyle {
        // If style was explicitly provided, use it
        if (this.style !== undefined) {
            return this.style;
        }

        // Check the file extension from sourcePath
        const sourcePath = compilationUnit.sourcePath;
        const isTypeScript = sourcePath.endsWith('.ts') ||
                            sourcePath.endsWith('.tsx') ||
                            sourcePath.endsWith('.mts') ||
                            sourcePath.endsWith('.cts');

        // Check for .cjs extension - must use CommonJS
        if (sourcePath.endsWith('.cjs')) {
            return ImportStyle.CommonJS;
        }

        // First, check if there's already an import from the same module
        // and match that style
        const existingStyleForModule = this.findExistingImportStyleForModule(compilationUnit);
        if (existingStyleForModule !== null) {
            return existingStyleForModule;
        }

        // For .mjs or TypeScript, prefer ES6
        if (sourcePath.endsWith('.mjs') || isTypeScript) {
            // If we're importing a member (but not 'default'), use named imports
            if (this.member !== undefined && this.member !== 'default') {
                return ImportStyle.ES6Named;
            }
            // Otherwise default import
            return ImportStyle.ES6Default;
        }

        // For .js files, check what style is predominantly being used
        let hasNamedImports = false;
        let hasNamespaceImports = false;
        let hasDefaultImports = false;
        let hasCommonJSRequires = false;

        for (const stmt of compilationUnit.statements) {
            const statement = stmt.element;

            // Check for ES6 imports
            if (statement?.kind === JS.Kind.Import) {
                const jsImport = statement as JS.Import;
                const importClause = jsImport.importClause;

                if (importClause) {
                    // Check for named bindings
                    if (importClause.namedBindings) {
                        if (importClause.namedBindings.kind === JS.Kind.NamedImports) {
                            hasNamedImports = true;
                        } else if (importClause.namedBindings.kind === J.Kind.Identifier ||
                                   importClause.namedBindings.kind === JS.Kind.Alias) {
                            // import * as x from 'module'
                            hasNamespaceImports = true;
                        }
                    }

                    // Check for default import
                    if (importClause.name) {
                        hasDefaultImports = true;
                    }
                }
            }

            // Check for CommonJS requires
            if (statement?.kind === J.Kind.VariableDeclarations) {
                const varDecl = statement as J.VariableDeclarations;
                if (varDecl.variables.length === 1) {
                    const namedVar = varDecl.variables[0].element;
                    const initializer = namedVar?.initializer?.element;
                    if (initializer?.kind === J.Kind.MethodInvocation &&
                        isRequireCall(initializer as J.MethodInvocation)) {
                        hasCommonJSRequires = true;
                    }
                }
            }
        }

        // Prefer matching the predominant style
        // If file uses CommonJS, stick with it
        if (hasCommonJSRequires && !hasNamedImports && !hasNamespaceImports && !hasDefaultImports) {
            return ImportStyle.CommonJS;
        }

        // If importing a member (but not 'default'), prefer named imports if they exist in the file
        if (this.member !== undefined && this.member !== 'default') {
            if (hasNamedImports) {
                return ImportStyle.ES6Named;
            }
            if (hasNamespaceImports) {
                return ImportStyle.ES6Namespace;
            }
        }

        // For default/whole module imports
        if (this.member === undefined || this.member === 'default') {
            if (hasNamespaceImports) {
                return ImportStyle.ES6Namespace;
            }
            if (hasDefaultImports) {
                return ImportStyle.ES6Default;
            }
        }

        // Default to named imports for members (except 'default'), default imports for modules
        return (this.member !== undefined && this.member !== 'default')
            ? ImportStyle.ES6Named
            : ImportStyle.ES6Default;
    }

    /**
     * Find the import style used for an existing import from the same module
     */
    private findExistingImportStyleForModule(compilationUnit: JS.CompilationUnit): ImportStyle | null {
        for (const stmt of compilationUnit.statements) {
            const statement = stmt.element;

            // Check ES6 imports
            if (statement?.kind === JS.Kind.Import) {
                const jsImport = statement as JS.Import;
                const moduleSpecifier = jsImport.moduleSpecifier?.element;

                if (moduleSpecifier) {
                    const moduleName = this.getModuleName(moduleSpecifier);

                    if (moduleName === this.module) {
                        const importClause = jsImport.importClause;
                        if (importClause?.namedBindings) {
                            if (importClause.namedBindings.kind === JS.Kind.NamedImports) {
                                return ImportStyle.ES6Named;
                            } else {
                                return ImportStyle.ES6Namespace;
                            }
                        }
                        if (importClause?.name) {
                            return ImportStyle.ES6Default;
                        }
                    }
                }
            }

            // Check CommonJS requires
            if (statement?.kind === J.Kind.VariableDeclarations) {
                const varDecl = statement as J.VariableDeclarations;
                if (varDecl.variables.length === 1) {
                    const namedVar = varDecl.variables[0].element;
                    const initializer = namedVar?.initializer?.element;

                    if (requiredModule(initializer) === this.module) {
                        return ImportStyle.CommonJS;
                    }
                }
            }
        }

        return null;
    }

    override async visitJsCompilationUnit(compilationUnit: JS.CompilationUnit, p: P): Promise<J | undefined> {
        // First, check if the import already exists
        const hasImport = await this.checkImportExists(compilationUnit);
        if (hasImport) {
            return compilationUnit;
        }

        // If onlyIfReferenced is true, check if the identifier is actually used
        // Skip this check for side-effect imports
        if (!this.sideEffectOnly && this.onlyIfReferenced) {
            const isReferenced = await this.checkIdentifierReferenced(compilationUnit);
            if (!isReferenced) {
                return compilationUnit;
            }
        }

        // Determine the appropriate import style
        const importStyle = this.determineImportStyle(compilationUnit);

        // For named imports, check if we can merge into an existing import from the same module
        // This handles both:
        // - Case 1: Existing import has named bindings - merge into them
        // - Case 2: Default import without named bindings - add named bindings
        // Don't try to merge default imports (member === 'default'), side-effect imports, or namespace imports (member === '*')
        if (!this.sideEffectOnly && this.member !== undefined && this.member !== 'default' && this.member !== '*') {
            const mergedCu = await this.tryMergeIntoExistingImport(compilationUnit, p);
            if (mergedCu !== compilationUnit) {
                return mergedCu;
            }
        }

        // TODO: create a `require` here. Until then the request goes unserved, since `import` would
        // make a file that binds its modules with `require` an ES module and change how everything
        // in it loads — `maybeBind` refuses it for the same reason. An explicit ES6 `style`
        // overrides, which is how a caller converting the file to ESM asks for one.
        if (importStyle === ImportStyle.CommonJS && isCommonJs(compilationUnit)) {
            return compilationUnit;
        }

        // Add ES6 import (handles ES6Named, ES6Namespace, ES6Default)
        // Find the position to insert the import
        const insertionIndex = this.findImportInsertionIndex(compilationUnit);
        const newImport = await this.createImportStatement(compilationUnit, insertionIndex, p);

        const withImport = await this.produceJavaScript(compilationUnit, p, async draft => {
            // Insert the import at the appropriate position
            // Create semicolon marker for the import statement
            const semicolonMarkers = markers({
                kind: J.Markers.Semicolon,
                id: randomId()
            });

            if (insertionIndex === 0) {
                // Insert at the beginning
                // The `after` space should be empty since semicolon is printed after it
                // The spacing comes from updating the next statement's prefix
                const updatedStatements = compilationUnit.statements.length > 0
                    ? [
                        rightPadded(newImport, emptySpace, semicolonMarkers),
                        {
                            ...compilationUnit.statements[0],
                            element: compilationUnit.statements[0].element
                                ? {...compilationUnit.statements[0].element, prefix: space("\n\n")}
                                : undefined
                        } as J.RightPadded<Statement>,
                        ...compilationUnit.statements.slice(1)
                    ]
                    : [rightPadded(newImport, emptySpace, semicolonMarkers)];

                draft.statements = updatedStatements;
            } else {
                // Insert after existing imports
                const before = compilationUnit.statements.slice(0, insertionIndex);
                const after = compilationUnit.statements.slice(insertionIndex);

                //The `after` space is empty, spacing comes from next statement's prefix
                // Ensure the next statement has at least one newline in its prefix
                if (after.length > 0 && after[0].element) {
                    const currentPrefix = after[0].element.prefix;
                    const needsNewline = !currentPrefix.whitespace.includes('\n');

                    const updatedNextStatement = needsNewline ? {
                        ...after[0],
                        element: {...after[0].element, prefix: space("\n" + currentPrefix.whitespace)}
                    } : after[0];

                    draft.statements = [
                        ...before,
                        rightPadded(newImport, emptySpace, semicolonMarkers),
                        updatedNextStatement,
                        ...after.slice(1)
                    ];
                } else {
                    draft.statements = [
                        ...before,
                        rightPadded(newImport, emptySpace, semicolonMarkers),
                        ...after
                    ];
                }
            }
        });

        return formatImport(withImport, newImport.id, p);
    }

    /**
     * Try to merge the new member into an existing import from the same module
     */
    private async tryMergeIntoExistingImport(compilationUnit: JS.CompilationUnit, p: P): Promise<JS.CompilationUnit> {
        for (let i = 0; i < compilationUnit.statements.length; i++) {
            const stmt = compilationUnit.statements[i];
            const statement = stmt.element;

            if (statement?.kind === JS.Kind.Import) {
                const jsImport = statement as JS.Import;
                const moduleSpecifier = jsImport.moduleSpecifier?.element;

                if (!moduleSpecifier) {
                    continue;
                }

                const moduleName = this.getModuleName(moduleSpecifier);

                // Check if this is an import from our target module
                if (moduleName !== this.module) {
                    continue;
                }

                const importClause = jsImport.importClause;
                if (!importClause) {
                    continue;
                }

                // Only merge into imports with matching typeOnly - don't mix type and value imports
                if (importClause.typeOnly !== this.typeOnly) {
                    continue;
                }

                // Case 1: Existing import has named bindings - merge into them
                if (importClause.namedBindings) {
                    // Only merge into NamedImports, not namespace imports
                    if (importClause.namedBindings.kind !== JS.Kind.NamedImports) {
                        continue;
                    }

                    // We found a matching import with named bindings - merge into it
                    return formatMergedImport(await this.produceJavaScript(compilationUnit, p, async draft => {
                        const namedImports = importClause.namedBindings as JS.NamedImports;
                        const existingElements = namedImports.elements.elements;

                        // Find the correct insertion position (alphabetical, case-insensitive)
                        const newName = this.bindingName!.toLowerCase();
                        let insertIndex = existingElements.findIndex(elem => {
                            if (elem.element?.kind === JS.Kind.ImportSpecifier) {
                                const name = specifierBinding(elem.element as JS.ImportSpecifier)?.name ?? '';
                                return newName.localeCompare(name.toLowerCase()) < 0;
                            }
                            return false;
                        });
                        if (insertIndex === -1) insertIndex = existingElements.length;

                        // Detect spacing style from existing elements:
                        // - firstElementPrefix: space after { (from first element's prefix)
                        // - trailingSpace: space before } (from last element's after)
                        const firstElementPrefix = existingElements[0]?.element?.prefix ?? emptySpace;
                        const lastIndex = existingElements.length - 1;
                        const trailingSpace = existingElements[lastIndex].after;

                        // Build the new elements array with proper spacing
                        const updatedNamedImports: JS.NamedImports = await this.produceJavaScript(
                            namedImports, p, async namedDraft => {
                                const newSpecifier = this.createImportSpecifier();

                                const newElements = existingElements.flatMap((elem, j) => {
                                    const results: J.RightPadded<JS.ImportSpecifier>[] = [];
                                    if (j === insertIndex) {
                                        // Insert new element here
                                        // First element gets the same prefix as the original first element
                                        // Other positions get a single space (separator after comma)
                                        const prefix = j === 0 ? firstElementPrefix : singleSpace;
                                        results.push(rightPadded({...newSpecifier, prefix}, emptySpace));
                                    }
                                    // Adjust existing element: if inserting before first, give it space prefix
                                    let adjusted = elem;
                                    if (j === 0 && insertIndex === 0 && elem.element) {
                                        adjusted = {...elem, element: {...elem.element, prefix: singleSpace}};
                                    }
                                    // Last element before a new trailing element loses its trailing space
                                    if (j === lastIndex && insertIndex > lastIndex) {
                                        adjusted = {...adjusted, after: emptySpace};
                                    }
                                    results.push(adjusted);
                                    return results;
                                });

                                // Append at end if inserting after all existing elements
                                if (insertIndex > lastIndex) {
                                    newElements.push(rightPadded({...newSpecifier, prefix: singleSpace}, trailingSpace));
                                }

                                namedDraft.elements = {...namedImports.elements, elements: newElements};
                            }
                        );

                        // Update the import with the new named imports
                        const updatedImport: JS.Import = await this.produceJavaScript(
                            jsImport, p, async importDraft => {
                                importDraft.importClause = await this.produceJavaScript(
                                    importClause, p, async clauseDraft => {
                                        clauseDraft.namedBindings = updatedNamedImports;
                                    }
                                );
                            }
                        );

                        // Replace the statement in the compilation unit
                        draft.statements = compilationUnit.statements.map((s, idx) =>
                            idx === i ? {...s, element: updatedImport} : s
                        );
                    }), jsImport.id, p);
                }

                // Case 2: Default import without named bindings - add named bindings
                // Transform: import React from 'react' -> import React, { useState } from 'react'
                if (importClause.name && !importClause.namedBindings) {
                    return formatMergedImport(await this.produceJavaScript(compilationUnit, p, async draft => {
                        const newSpecifier = this.createImportSpecifier();

                        // Get the spaces style for brace spacing
                        const spacesStyle = getStyle(StyleKind.SpacesStyle, compilationUnit) as SpacesStyle;
                        const braceSpace = spacesStyle.within.es6ImportExportBraces ? singleSpace : emptySpace;

                        // Create new NamedImports with a single element
                        // Apply brace spacing: space after { is in specifier's prefix, space before } is in after
                        const namedImports: JS.NamedImports = {
                            id: randomId(),
                            kind: JS.Kind.NamedImports,
                            prefix: singleSpace,
                            markers: emptyMarkers,
                            elements: {
                                kind: J.Kind.Container,
                                before: emptySpace,
                                elements: [rightPadded({...newSpecifier, prefix: braceSpace}, braceSpace)],
                                markers: emptyMarkers
                            }
                        };

                        // Update the import clause to include named bindings
                        // Also update name.after to emptySpace since the comma goes right after the name
                        const updatedImport: JS.Import = await this.produceJavaScript(
                            jsImport, p, async importDraft => {
                                importDraft.importClause = await this.produceJavaScript(
                                    importClause, p, async clauseDraft => {
                                        // Remove space after default name (comma goes right after)
                                        if (clauseDraft.name) {
                                            clauseDraft.name = {...clauseDraft.name, after: emptySpace};
                                        }
                                        clauseDraft.namedBindings = namedImports;
                                    }
                                );
                                // Ensure moduleSpecifier has proper space before 'from'
                                if (importDraft.moduleSpecifier) {
                                    importDraft.moduleSpecifier = {
                                        ...importDraft.moduleSpecifier,
                                        before: singleSpace
                                    };
                                }
                            }
                        );

                        // Replace the statement in the compilation unit
                        draft.statements = compilationUnit.statements.map((s, idx) =>
                            idx === i ? {...s, element: updatedImport} : s
                        );
                    }), jsImport.id, p);
                }
            }
        }

        return compilationUnit;
    }

    /**
     * Check if the import already exists in the compilation unit
     */
    private async checkImportExists(compilationUnit: JS.CompilationUnit): Promise<boolean> {
        for (const stmt of compilationUnit.statements) {
            const statement = stmt.element;

            // Check ES6 imports
            if (statement?.kind === JS.Kind.Import) {
                const jsImport = statement as JS.Import;
                if (this.isMatchingImport(jsImport)) {
                    return true;
                }
            }

            // Check CommonJS require statements
            if (statement?.kind === J.Kind.VariableDeclarations) {
                const varDecl = statement as J.VariableDeclarations;
                if (this.isMatchingRequire(varDecl)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the import matches what we're trying to add
     */
    private isMatchingImport(jsImport: JS.Import): boolean {
        const moduleSpecifier = jsImport.moduleSpecifier?.element;
        if (!moduleSpecifier || this.getModuleName(moduleSpecifier) !== this.module) {
            return false;
        }

        // A side-effect import is the clause-less one, and no import carrying bindings stands in
        // for it — nor it for them.
        const importClause = jsImport.importClause;
        if (!importClause || this.sideEffectOnly) {
            return !importClause && this.sideEffectOnly;
        }

        return importBindings(jsImport).some(binding => this.answeredBy(binding));
    }

    /** Whether a binding the file already has serves this request. */
    private answeredBy(binding: ModuleScopeBinding): boolean {
        return binding.module === this.module &&
            binding.member === memberName(this.member) &&
            binding.typeOnly === this.typeOnly &&
            (this.anyNameAnswers || binding.name === this.bindingName);
    }

    /**
     * Check if this is a matching CommonJS require statement
     */
    private isMatchingRequire(varDecl: J.VariableDeclarations): boolean {
        if (varDecl.variables.length !== 1) {
            return false;
        }
        const namedVar = varDecl.variables[0].element;
        const module = requiredModule(namedVar?.initializer?.element);
        if (module !== this.module) {
            return false;
        }
        return requireBindings(namedVar!.name, module).some(binding => this.answeredBy(binding));
    }

    /**
     * Extract the module name from a class type by traversing the owningClass chain
     * or extracting it from the FQN.
     */
    private getModuleFromClassType(classType: Type.Class): string | undefined {
        // Traverse owningClass chain to find the root
        let current: Type.Class = classType;
        while (current.owningClass && Type.isClass(current.owningClass)) {
            current = current.owningClass as Type.Class;
        }
        // If there's still an owningClass (non-Class type), use it
        if (current.owningClass) {
            return Type.FullyQualified.getFullyQualifiedName(current.owningClass);
        }
        // For top-level classes, extract module from FQN (e.g., "zod.ZodError" -> "zod")
        const fqn = current.fullyQualifiedName;
        const dotIndex = fqn.lastIndexOf('.');
        if (dotIndex > 0) {
            return fqn.substring(0, dotIndex);
        }
        // The FQN itself might be the module (e.g., "zod" for z from zod)
        return fqn;
    }

    /**
     * Extract the module name from a type (method, class, or variable).
     */
    private getModuleFromType(type: Type | undefined, fieldType: Type | undefined): string | undefined {
        if (type && Type.isMethod(type)) {
            return Type.FullyQualified.getFullyQualifiedName((type as Type.Method).declaringType);
        }
        if (type && Type.isClass(type)) {
            return this.getModuleFromClassType(type as Type.Class);
        }
        if (fieldType?.kind === Type.Kind.Variable) {
            const variableType = fieldType as Type.Variable;
            if (variableType.owner) {
                return Type.FullyQualified.getFullyQualifiedName(variableType.owner);
            }
        }
        return undefined;
    }

    /**
     * Check if a class type matches the expected module.
     * Handles direct FQN match, owningClass chain match, and FQN prefix match.
     */
    private classTypeMatchesModule(classType: Type.Class, expectedModule: string): boolean {
        const fqn = classType.fullyQualifiedName;
        // Direct match: class FQN equals the expected module (e.g., z from zod where z's type FQN is "zod")
        if (fqn === expectedModule) {
            return true;
        }
        // Check via owningClass chain or FQN prefix
        const moduleFromType = this.getModuleFromClassType(classType);
        return moduleFromType === expectedModule;
    }

    /**
     * Check if the identifier is actually referenced in the file
     */
    private async checkIdentifierReferenced(compilationUnit: JS.CompilationUnit): Promise<boolean> {
        // For namespace imports, we cannot use type attribution to detect usage
        // because the namespace itself is used as an identifier, not individual members.
        // For simplicity, we skip the onlyIfReferenced check for namespace imports.
        if (this.member === '*') {
            // TODO: Implement proper namespace usage detection by checking if alias identifier is used
            return true;
        }

        // Step 1: Find the expected declaring type by examining existing imports from the same module
        let expectedDeclaringType: string | undefined;

        for (const stmt of compilationUnit.statements) {
            const statement = stmt.element;

            if (statement?.kind === JS.Kind.Import) {
                const jsImport = statement as JS.Import;
                const moduleSpecifier = jsImport.moduleSpecifier?.element;

                if (!moduleSpecifier) {
                    continue;
                }

                const moduleName = this.getModuleName(moduleSpecifier);
                if (moduleName !== this.module) {
                    continue;  // Not the module we're interested in
                }

                // Found an existing import from our target module
                // Extract the declaring type from any imported member with type attribution
                const importClause = jsImport.importClause;
                if (importClause?.namedBindings?.kind === JS.Kind.NamedImports) {
                    const namedImports = importClause.namedBindings as JS.NamedImports;
                    for (const elem of namedImports.elements.elements) {
                        const specifier = elem.element;
                        if (specifier?.kind === JS.Kind.ImportSpecifier) {
                            const importSpec = specifier as JS.ImportSpecifier;
                            let identifier: J.Identifier | undefined;
                            if (importSpec.specifier?.kind === J.Kind.Identifier) {
                                identifier = importSpec.specifier as J.Identifier;
                            } else if (importSpec.specifier?.kind === JS.Kind.Alias) {
                                const aliasSpec = importSpec.specifier as JS.Alias;
                                if (aliasSpec.alias?.kind === J.Kind.Identifier) {
                                    identifier = aliasSpec.alias as J.Identifier;
                                }
                            }

                            expectedDeclaringType = this.getModuleFromType(identifier?.type, identifier?.fieldType);
                            if (expectedDeclaringType) {
                                break;  // Found it!
                            }
                        }
                    }
                }

                if (expectedDeclaringType) {
                    break;  // No need to scan more imports
                }
            }
        }

        // Step 2: Look for references that match
        const targetName = this.bindingName;
        const targetModule = this.module;
        let found = false;
        const self = this;

        // If no existing imports from this module, look for unresolved references
        // If there ARE existing imports, look for references with the expected declaring type

        const collector = new class extends JavaScriptVisitor<void> {
            override async visitIdentifier(identifier: J.Identifier, p: void): Promise<J | undefined> {
                if (identifier.simpleName === targetName) {
                    const type = identifier.type;
                    const fieldType = identifier.fieldType;
                    if (expectedDeclaringType) {
                        // We have an expected declaring type - check for exact match
                        if (type && Type.isMethod(type)) {
                            const declaringTypeName = Type.FullyQualified.getFullyQualifiedName((type as Type.Method).declaringType);
                            if (declaringTypeName === expectedDeclaringType) {
                                found = true;
                            }
                        }
                        else if (type && Type.isClass(type)) {
                            if (self.classTypeMatchesModule(type as Type.Class, expectedDeclaringType)) {
                                found = true;
                            }
                        }
                        else if (fieldType?.kind === Type.Kind.Variable) {
                            const ownerTypeName = (fieldType as Type.Variable).owner
                                ? Type.FullyQualified.getFullyQualifiedName((fieldType as Type.Variable).owner!)
                                : undefined;
                            if (ownerTypeName === expectedDeclaringType) {
                                found = true;
                            }
                        }
                        // Also check for unresolved references (member isn't imported yet)
                        else if (!type && !fieldType) {
                            found = true;
                        }
                    } else {
                        // No existing imports from this module - look for references that match
                        // 1. Unresolved references (no type/unknown type and no fieldType)
                        const isUnknownType = !type || type.kind === Type.Kind.Unknown;
                        if (isUnknownType && !fieldType) {
                            found = true;
                        }
                        // 2. References with fieldType matching the target module
                        else if (fieldType?.kind === Type.Kind.Variable) {
                            const variableType = fieldType as Type.Variable;
                            if (variableType.owner && Type.isClass(variableType.owner)) {
                                // Traverse owningClass chain to find the root module (handles nested namespaces)
                                // For example: React.forwardRef -> owner is "React" namespace -> owningClass is "react" module
                                let current: Type.Class = variableType.owner as Type.Class;

                                // Walk up the owningClass chain until we reach the root
                                while (current.owningClass && Type.isClass(current.owningClass)) {
                                    current = current.owningClass as Type.Class;
                                }

                                const moduleName = Type.FullyQualified.getFullyQualifiedName(current);
                                if (moduleName === targetModule) {
                                    found = true;
                                }
                            }
                        }
                        // 3. References with method type matching the target module
                        else if (type && Type.isMethod(type)) {
                            const methodType = type as Type.Method;
                            const declaringTypeName = Type.FullyQualified.getFullyQualifiedName(methodType.declaringType);
                            if (declaringTypeName === targetModule) {
                                found = true;
                            }
                        }
                    }
                }
                return super.visitIdentifier(identifier, p);
            }

            override async visitMethodInvocation(methodInvocation: J.MethodInvocation, p: void): Promise<J | undefined> {
                if (methodInvocation.methodType && methodInvocation.methodType.name === targetName) {
                    if (expectedDeclaringType) {
                        const declaringTypeName = Type.FullyQualified.getFullyQualifiedName(methodInvocation.methodType.declaringType);
                        if (declaringTypeName === expectedDeclaringType) {
                            found = true;
                        }
                    }
                }
                return super.visitMethodInvocation(methodInvocation, p);
            }

            override async visitFunctionCall(functionCall: JS.FunctionCall, p: void): Promise<J | undefined> {
                if (functionCall.methodType && functionCall.methodType.name === targetName) {
                    if (expectedDeclaringType) {
                        const declaringTypeName = Type.FullyQualified.getFullyQualifiedName(functionCall.methodType.declaringType);
                        if (declaringTypeName === expectedDeclaringType) {
                            found = true;
                        }
                    }
                }
                return super.visitFunctionCall(functionCall, p);
            }

            override async visitFieldAccess(fieldAccess: J.FieldAccess, p: void): Promise<J | undefined> {
                const type = fieldAccess.type;
                if (type && Type.isMethod(type)) {
                    const methodType = type as Type.Method;
                    if (methodType.name === targetName) {
                        if (expectedDeclaringType) {
                            const declaringTypeName = Type.FullyQualified.getFullyQualifiedName(methodType.declaringType);
                            if (declaringTypeName === expectedDeclaringType) {
                                found = true;
                            }
                        }
                    }
                }
                return super.visitFieldAccess(fieldAccess, p);
            }
        };

        await collector.visit(compilationUnit, undefined);

        return found;
    }

    /**
     * Create a new import statement
     */
    private async createImportStatement(compilationUnit: JS.CompilationUnit, insertionIndex: number, p: P): Promise<JS.Import> {
        // Determine the appropriate prefix (spacing before the import)
        const prefix = this.determineImportPrefix(compilationUnit, insertionIndex);

        // Whitespace throughout follows the parser's placement, so that formatting the result is a
        // no-op where the file's style already agrees with it.
        // Note: value is the unquoted module name; valueSource and unicodeEscapes are its printed form
        let valueSource = this.moduleValueSource;
        if (valueSource === undefined) {
            const quote = this.quoteStyle ?? await detectQuote(compilationUnit);
            valueSource = `${quote}${this.module}${quote}`;
        }
        const moduleSpecifier: J.Literal = {
            id: randomId(),
            kind: J.Kind.Literal,
            prefix: singleSpace,
            markers: emptyMarkers,
            value: this.module,
            valueSource,
            unicodeEscapes: this.moduleUnicodeEscapes,
            type: undefined
        };

        let importClause: JS.ImportClause | undefined;

        if (this.sideEffectOnly) {
            // Side-effect import: import 'module'
            importClause = undefined;
        } else if (this.member === '*') {
            // Namespace import: import * as alias from 'module'
            const propertyName: J.Identifier = {
                id: randomId(),
                kind: J.Kind.Identifier,
                prefix: emptySpace,
                markers: emptyMarkers,
                annotations: [],
                simpleName: '*',
                type: undefined,
                fieldType: undefined
            };

            const aliasIdentifier: J.Identifier = {
                id: randomId(),
                kind: J.Kind.Identifier,
                prefix: singleSpace,
                markers: emptyMarkers,
                annotations: [],
                simpleName: this.bindingName!,
                type: undefined,
                fieldType: undefined
            };

            const namespaceBinding: JS.Alias = {
                id: randomId(),
                kind: JS.Kind.Alias,
                prefix: this.typeOnly ? singleSpace : emptySpace,
                markers: emptyMarkers,
                propertyName: rightPadded(propertyName, singleSpace),
                alias: aliasIdentifier
            };

            importClause = {
                id: randomId(),
                kind: JS.Kind.ImportClause,
                prefix: singleSpace,
                markers: emptyMarkers,
                typeOnly: this.typeOnly,
                name: undefined,
                namedBindings: namespaceBinding
            };
        } else if (this.member === undefined || this.member === 'default') {
            // Default import: import target from 'module'
            // or: import alias from 'module' (when member === 'default')
            const defaultName: J.Identifier = {
                id: randomId(),
                kind: J.Kind.Identifier,
                prefix: this.typeOnly ? singleSpace : emptySpace,
                markers: emptyMarkers,
                annotations: [],
                simpleName: this.bindingName!,
                type: undefined,
                fieldType: undefined
            };

            importClause = {
                id: randomId(),
                kind: JS.Kind.ImportClause,
                prefix: singleSpace,
                markers: emptyMarkers,
                typeOnly: this.typeOnly,
                name: rightPadded(defaultName, singleSpace),
                namedBindings: undefined
            };
        } else {
            // Named import: import { member } from 'module'

            // Get the spaces style for brace spacing
            const spacesStyle = getStyle(StyleKind.SpacesStyle, compilationUnit) as SpacesStyle;
            const braceSpace = spacesStyle.within.es6ImportExportBraces ? singleSpace : emptySpace;

            const importSpec = this.createImportSpecifier();
            // Apply brace spacing: the space after { is in the specifier's prefix,
            // and the space before } is in the rightPadded's after
            const importSpecWithSpacing = {...importSpec, prefix: braceSpace};

            const namedImports: JS.NamedImports = {
                id: randomId(),
                kind: JS.Kind.NamedImports,
                prefix: emptySpace,
                markers: emptyMarkers,
                elements: {
                    kind: J.Kind.Container,
                    before: this.typeOnly ? singleSpace : emptySpace,
                    elements: [rightPadded(importSpecWithSpacing, braceSpace)],
                    markers: emptyMarkers
                }
            };

            importClause = {
                id: randomId(),
                kind: JS.Kind.ImportClause,
                prefix: singleSpace,
                markers: emptyMarkers,
                typeOnly: this.typeOnly,
                name: undefined,
                namedBindings: namedImports
            };
        }

        const jsImport: JS.Import = {
            id: randomId(),
            kind: JS.Kind.Import,
            prefix,
            markers: emptyMarkers,
            modifiers: [],
            importClause,
            moduleSpecifier: {
                kind: J.Kind.LeftPadded,
                // Bindings end at `}`, so the space before `from` sits here; a default import's name
                // and a side-effect import's literal carry their own
                before: importClause?.namedBindings ? singleSpace : emptySpace,
                element: moduleSpecifier,
                markers: emptyMarkers
            },
            initializer: undefined
        };

        return jsImport;
    }

    /**
     * Create an import specifier for a named import
     */
    private createImportSpecifier(): JS.ImportSpecifier {
        let specifier: J.Identifier | JS.Alias;

        // An alias equal to the member says nothing `{member}` alone does not.
        if (this.bindingName !== this.member) {
            // Aliased import: import { member as alias } from 'module'
            const propertyName: J.Identifier = {
                id: randomId(),
                kind: J.Kind.Identifier,
                prefix: emptySpace,
                markers: emptyMarkers,
                annotations: [],
                simpleName: this.member!,
                type: undefined,
                fieldType: undefined
            };

            const aliasName: J.Identifier = {
                id: randomId(),
                kind: J.Kind.Identifier,
                prefix: singleSpace,
                markers: emptyMarkers,
                annotations: [],
                simpleName: this.bindingName!,
                type: undefined,
                fieldType: undefined
            };

            specifier = {
                id: randomId(),
                kind: JS.Kind.Alias,
                prefix: emptySpace,
                markers: emptyMarkers,
                propertyName: rightPadded(propertyName, singleSpace),
                alias: aliasName
            };
        } else {
            // Regular import: import { member } from 'module'
            specifier = {
                id: randomId(),
                kind: J.Kind.Identifier,
                prefix: emptySpace,
                markers: emptyMarkers,
                annotations: [],
                simpleName: this.member!,
                type: undefined,
                fieldType: undefined
            };
        }

        return {
            id: randomId(),
            kind: JS.Kind.ImportSpecifier,
            prefix: emptySpace,
            markers: emptyMarkers,
            importType: {
                kind: J.Kind.LeftPadded,
                before: emptySpace,
                element: false,
                markers: emptyMarkers
            },
            specifier
        };
    }

    /**
     * Determine the appropriate spacing before the import statement
     */
    private determineImportPrefix(compilationUnit: JS.CompilationUnit, insertionIndex: number): J.Space {
        // If inserting at the beginning (index 0), use the prefix of the first statement
        // but only the whitespace part (preserve comments on the original first statement)
        if (insertionIndex === 0 && compilationUnit.statements.length > 0) {
            const firstPrefix = compilationUnit.statements[0].element?.prefix;
            if (firstPrefix) {
                // Keep only whitespace, not comments
                return {
                    kind: J.Kind.Space,
                    comments: [],
                    whitespace: firstPrefix.whitespace
                };
            }
            return emptySpace;
        }

        // If inserting after other statements, ensure we have at least one newline
        // to separate from the previous statement
        return space("\n");
    }

    /**
     * Find the index where the new import should be inserted
     */
    private findImportInsertionIndex(compilationUnit: JS.CompilationUnit): number {
        let lastImportIndex = -1;

        for (let i = 0; i < compilationUnit.statements.length; i++) {
            const statement = compilationUnit.statements[i].element;
            if (statement?.kind === JS.Kind.Import) {
                lastImportIndex = i;
            } else if (lastImportIndex >= 0) {
                // We've found a non-import after imports, insert after the last import
                return lastImportIndex + 1;
            }
        }

        // If we found imports, insert after them
        if (lastImportIndex >= 0) {
            return lastImportIndex + 1;
        }

        // No imports found, insert at the beginning
        return 0;
    }



}

/**
 * The local name `jsImport` binds `member` of `module` to, or `undefined` where it binds
 * something else. `'default'` and an absent `member` both mean the default import, matching
 * {@link memberName}.
 */
function importBinds(jsImport: JS.Import, module: string, member: string | undefined): string | undefined {
    const specifier = jsImport.moduleSpecifier?.element;
    if (specifier?.kind !== J.Kind.Literal || (specifier as J.Literal).value !== module) {
        return undefined;
    }
    const importClause = jsImport.importClause;
    if (!importClause) {
        return undefined;
    }
    const key = memberName(member);
    if (key === undefined) {
        const nameElem = importClause.name?.element;
        return nameElem && isIdentifier(nameElem) ? nameElem.simpleName : undefined;
    }
    if (key === '*') {
        const namedBindings = importClause.namedBindings;
        return namedBindings?.kind === JS.Kind.Alias && isIdentifier((namedBindings as JS.Alias).alias)
            ? ((namedBindings as JS.Alias).alias as J.Identifier).simpleName
            : undefined;
    }
    const namedBindings = importClause.namedBindings;
    if (namedBindings?.kind !== JS.Kind.NamedImports) {
        return undefined;
    }
    for (const elem of (namedBindings as JS.NamedImports).elements.elements) {
        const specifierNode = elem.element.specifier;
        if (!namedSpecifierImports(specifierNode, key)) {
            continue;
        }
        if (isIdentifier(specifierNode)) {
            return specifierNode.simpleName;
        }
        const alias = (specifierNode as JS.Alias).alias;
        if (isIdentifier(alias)) {
            return alias.simpleName;
        }
    }
    return undefined;
}

/** Whether `specifier` imports the member `key`, under whatever local name it binds it to. */
function namedSpecifierImports(specifier: JS.ImportSpecifier["specifier"], key: string): boolean {
    if (isIdentifier(specifier)) {
        return specifier.simpleName === key;
    }
    if (specifier.kind === JS.Kind.Alias) {
        const propertyName = (specifier as JS.Alias).propertyName.element;
        return isIdentifier(propertyName) && propertyName.simpleName === key;
    }
    return false;
}

/** Whether the named specifier binding `key` carries its own inline `type`, as in `{type a, b}`. */
function namedSpecifierIsTypeOnly(imp: JS.Import, key: string): boolean {
    const namedBindings = imp.importClause?.namedBindings;
    if (namedBindings?.kind !== JS.Kind.NamedImports) {
        return false;
    }
    for (const elem of (namedBindings as JS.NamedImports).elements.elements) {
        if (namedSpecifierImports(elem.element.specifier, key)) {
            return elem.element.importType.element;
        }
    }
    return false;
}

/** How many named specifiers `jsImport` carries, `0` for a default, namespace or side-effect import. */
function namedImportCount(jsImport: JS.Import): number {
    const namedBindings = jsImport.importClause?.namedBindings;
    return namedBindings?.kind === JS.Kind.NamedImports ? (namedBindings as JS.NamedImports).elements.elements.length : 0;
}

/**
 * Whether `jsImport`'s clause binds exactly one thing — a default, a namespace, or one named
 * specifier. That is the only shape a module-only move can rewrite in place: whichever binding
 * `importBinds` already matched is this one, so nothing else the clause carries goes along with it.
 */
function isOnlyMember(jsImport: JS.Import): boolean {
    const importClause = jsImport.importClause;
    if (!importClause) {
        return false;
    }
    const hasDefault = importClause.name !== undefined;
    const hasNamespace = importClause.namedBindings?.kind === JS.Kind.Alias;
    return (hasDefault ? 1 : 0) + (hasNamespace ? 1 : 0) + namedImportCount(jsImport) === 1;
}

export interface ExistingImportBinding {
    localName: string;
    onlyMemberOfStatement: boolean;

    /** Whether the source states this local name — `import {a as b}` — or takes it from the member. */
    aliased: boolean;
}

/**
 * The existing binding for `member` of `module`, read from `cu`'s own import statements — what
 * `maybeRebind` reads before committing to a `RebindImport` edit.
 */
export function existingImportBinding(
    cu: JS.CompilationUnit,
    module: string,
    member: string | undefined
): ExistingImportBinding | undefined {
    for (const stmt of cu.statements) {
        const element = stmt.element;
        if (element?.kind !== JS.Kind.Import) {
            continue;
        }
        const localName = importBinds(element as JS.Import, module, member);
        if (localName !== undefined) {
            return {
                localName,
                onlyMemberOfStatement: isOnlyMember(element as JS.Import),
                aliased: localName !== memberName(member)
            };
        }
    }
    return undefined;
}

/**
 * The `member as local` specifier, for an import binding `member` under `local` or an export
 * publishing `local`'s binding under that name. `local` itself becomes the alias, keeping the type
 * attribution it holds, and the alias takes its prefix: that whitespace separates the specifier
 * from a `type` keyword before it.
 */
function aliasing(local: J.Identifier, member: string): JS.Alias {
    const propertyName: J.Identifier = {
        id: randomId(),
        kind: J.Kind.Identifier,
        prefix: emptySpace,
        markers: emptyMarkers,
        annotations: [],
        simpleName: member,
        type: undefined,
        fieldType: undefined
    };
    return {
        id: randomId(),
        kind: JS.Kind.Alias,
        prefix: local.prefix,
        markers: emptyMarkers,
        propertyName: rightPadded(propertyName, singleSpace),
        alias: {...local, prefix: singleSpace}
    };
}

/**
 * Drops the binding for `member` from `jsImport`'s clause — the default, the namespace alias, or
 * one entry of the named list, whichever `member` names — keeping everything else the clause
 * binds. `ElementRemovalFormatter` carries the dropped binding's prefix onto whatever prints
 * next, the same way `RemoveImport` keeps formatting sane when trimming a list.
 */
function removeBinding(jsImport: JS.Import, member: string | undefined): JS.Import {
    const importClause = jsImport.importClause;
    if (!importClause) {
        return jsImport;
    }
    const key = memberName(member);

    if (key === undefined) {
        if (!importClause.name) {
            return jsImport;
        }
        const namedBindings = importClause.namedBindings;
        if (namedBindings?.kind === JS.Kind.NamedImports) {
            // `NamedImports` keeps the space before its own `{` on the container's `before`,
            // not on its own prefix, so the removed default's prefix has to land there instead.
            const namedImports = namedBindings as JS.NamedImports;
            const updated: JS.NamedImports = {
                ...namedImports,
                elements: {...namedImports.elements, before: importClause.name.element.prefix}
            };
            return {...jsImport, importClause: {...importClause, name: undefined, namedBindings: updated}};
        }
        if (namedBindings) {
            const formatter = new ElementRemovalFormatter<J>();
            formatter.markRemoved(importClause.name.element);
            return {...jsImport, importClause: {...importClause, name: undefined, namedBindings: formatter.processKept(namedBindings)}};
        }
        return {...jsImport, importClause: {...importClause, name: undefined}};
    }

    if (key === '*') {
        return {...jsImport, importClause: {...importClause, namedBindings: undefined}};
    }

    if (importClause.namedBindings?.kind !== JS.Kind.NamedImports) {
        return jsImport;
    }
    const namedImports = importClause.namedBindings as JS.NamedImports;
    const formatter = new ElementRemovalFormatter<JS.ImportSpecifier>();
    const kept: J.RightPadded<JS.ImportSpecifier>[] = [];
    for (const entry of namedImports.elements.elements) {
        if (namedSpecifierImports(entry.element.specifier, key)) {
            formatter.markRemoved(entry.element);
        } else {
            kept.push({...entry, element: formatter.processKept(entry.element)});
        }
    }
    if (kept.length === 0) {
        // An emptied brace list still prints, as `import D, {} from "m"`, so it goes with its
        // last member; the caller drops the whole statement when no default remains either.
        return {...jsImport, importClause: {...importClause, namedBindings: undefined}};
    }
    const updatedNamedImports: JS.NamedImports = {...namedImports, elements: {...namedImports.elements, elements: kept}};
    return {...jsImport, importClause: {...importClause, namedBindings: updatedNamedImports}};
}

/**
 * Moves the binding `from` names to `to`, binding it under `boundName`. In place when the
 * statement that carries it binds nothing else — module and member specifier rewritten there
 * directly; otherwise the old specifier drops and {@link bindImport} queues the replacement.
 * A `boundName` of its own renames the binding, and the file's references to it follow.
 *
 * Not built on `RemoveImport`/`maybeUnbind`: those only drop a binding once nothing references
 * it, but a rebind moves one that is still in use — removal here has to be unconditional.
 */
export class RebindImport<P> extends JavaScriptVisitor<P> {
    constructor(
        readonly from: {module: string; member?: string},
        readonly to: {module: string; member?: string},
        readonly localName: string,
        readonly boundName: string
    ) {
        super();
    }

    private transformedInPlace = false;
    private typeOnly = false;
    private readonly movedTypes = new MovedTypes(this.from, this.to);
    /** Identifiers settled as references to the binding, so a parent need not settle it again. */
    private readonly references = new Set<string>();

    private get renaming(): boolean {
        return this.boundName !== this.localName;
    }

    override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: P): Promise<J | undefined> {
        const visited = await super.visitJsCompilationUnit(cu, p) as JS.CompilationUnit;
        if (!this.transformedInPlace) {
            bindImport(this, {
                module: this.to.module,
                member: this.to.member,
                alias: this.boundName,
                typeOnly: this.typeOnly,
                onlyIfReferenced: false
            });
        }
        return visited;
    }

    override async visitImportDeclaration(jsImport: JS.Import, p: P): Promise<J | undefined> {
        const imp = await super.visitImportDeclaration(jsImport, p) as JS.Import;

        const key = memberName(this.from.member);
        // One call moves one binding, and no two imports bind the same local name, so the name
        // read from the matched statement is what picks it back out of the file.
        if (importBinds(imp, this.from.module, this.from.member) !== this.localName) {
            return imp;
        }
        // A moved named specifier's own inline `type` marks it type-only even where the clause
        // it's leaving is not — the replacement needs the same answer to stay type-safe.
        this.typeOnly = (imp.importClause?.typeOnly ?? false) ||
            (key !== undefined && key !== '*' && namedSpecifierIsTypeOnly(imp, key));

        if (!isOnlyMember(imp)) {
            return removeBinding(imp, this.from.member);
        }

        this.transformedInPlace = true;
        return produce(imp, draft => {
            const literal = draft.moduleSpecifier!.element as Draft<J.Literal>;
            literal.value = this.to.module;
            const originalSource = literal.valueSource || `"${this.from.module}"`;
            const quoteChar = originalSource.startsWith("'") ? "'" : '"';
            literal.valueSource = `${quoteChar}${this.to.module}${quoteChar}`;

            // A default or namespace import carries its local name on the clause itself; a named
            // one states the member alongside it, in the specifier.
            if (key === undefined || key === '*') {
                if (this.boundName !== this.localName) {
                    renameClauseBinding(draft.importClause, key, this.boundName);
                }
                return;
            }
            rewriteNamedSpecifier(draft.importClause, key, memberName(this.to.member) ?? key, this.boundName);
        });
    }

    override async visitIdentifier(identifier: J.Identifier, p: P): Promise<J | undefined> {
        if (!this.referencesBinding(identifier)) {
            return super.visitIdentifier(identifier, p);
        }
        this.references.add(identifier.id);
        // The name follows the binding, and so does what the name is attributed to; an aliased
        // binding keeps its name and still resolves somewhere new.
        const type = await this.movedTypes.visit(identifier.type, undefined);
        const fieldType = await this.movedTypes.visit(identifier.fieldType, undefined) as Type.Variable | undefined;
        return !this.renaming && type === identifier.type && fieldType === identifier.fieldType
            ? identifier
            : {...identifier, simpleName: this.boundName, type, fieldType} as J.Identifier;
    }

    override async visitMethodInvocation(method: J.MethodInvocation, p: P): Promise<J | undefined> {
        const m = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
        if (!this.referencesMovedBinding(m.name)) {
            return m;
        }
        const methodType = await this.movedTypes.visit(m.methodType, undefined) as Type.Method | undefined;
        return methodType === m.methodType ? m : {...m, methodType} as J.MethodInvocation;
    }

    override async visitNewClass(newClass: J.NewClass, p: P): Promise<J | undefined> {
        const nc = await super.visitNewClass(newClass, p) as J.NewClass;
        if (!this.referencesMovedBinding(nc.class)) {
            return nc;
        }
        const type = await this.movedTypes.visit(nc.type, undefined);
        const methodType = await this.movedTypes.visit(nc.methodType, undefined) as Type.Method | undefined;
        const constructorType = await this.movedTypes.visit(nc.constructorType, undefined) as Type.Method | undefined;
        return type === nc.type && methodType === nc.methodType && constructorType === nc.constructorType
            ? nc
            : {...nc, type, methodType, constructorType} as J.NewClass;
    }

    override async visitFunctionCall(functionCall: JS.FunctionCall, p: P): Promise<J | undefined> {
        const fc = await super.visitFunctionCall(functionCall, p) as JS.FunctionCall;
        if (!this.referencesMovedBinding(fc.function?.element)) {
            return fc;
        }
        const methodType = await this.movedTypes.visit(fc.methodType, undefined) as Type.Method | undefined;
        return methodType === fc.methodType ? fc : {...fc, methodType} as JS.FunctionCall;
    }

    /**
     * Whether `callee` is the identifier `visitIdentifier` settled as a reference to the binding.
     * A rewrite keeps the identifier's id, so the one it settled is the one still standing here.
     */
    private referencesMovedBinding(callee: J | undefined): boolean {
        return callee?.kind === J.Kind.Identifier && this.references.has(callee.id);
    }

    /**
     * A decorator and the other type-name positions reach the tree through this hook, which the
     * base visitor holds closed; a reference in one of them is a reference like any other.
     */
    protected override async visitTypeName<N extends NameTree>(nameTree: N, p: P): Promise<N> {
        return await this.visit(nameTree, p) as N;
    }

    /**
     * An export specifier's own name is the module's public surface, which its consumers read and
     * the rename leaves alone. See CLAUDE.md: Which name a rebind binds.
     */
    override async visitExportSpecifier(exportSpecifier: JS.ExportSpecifier, p: P): Promise<J | undefined> {
        if (!this.renaming) {
            return super.visitExportSpecifier(exportSpecifier, p);
        }
        // `export {a} from "m"` names `m`'s member rather than anything this file binds.
        const from = this.cursor.firstEnclosing(
            (v): v is JS.ExportDeclaration => (v as J | undefined)?.kind === JS.Kind.ExportDeclaration);
        if (from?.moduleSpecifier !== undefined) {
            return exportSpecifier;
        }
        const specifier = exportSpecifier.specifier;
        if (specifier.kind === J.Kind.Identifier && (specifier as J.Identifier).simpleName === this.localName) {
            return {...exportSpecifier, specifier: aliasing(specifier as J.Identifier, this.boundName)} as JS.ExportSpecifier;
        }
        if (specifier.kind === JS.Kind.Alias) {
            const alias = specifier as JS.Alias;
            const propertyName = alias.propertyName.element;
            return propertyName.kind === J.Kind.Identifier && (propertyName as J.Identifier).simpleName === this.localName
                ? {
                    ...exportSpecifier,
                    specifier: {
                        ...alias,
                        propertyName: {...alias.propertyName, element: {...propertyName, simpleName: this.boundName}}
                    }
                } as JS.ExportSpecifier
                : exportSpecifier;
        }
        return super.visitExportSpecifier(exportSpecifier, p);
    }

    /**
     * Whether the identifier at the cursor stands for the moved binding: the right name, in a
     * position that references rather than declares, reaching the module scope that binds it.
     */
    private referencesBinding(identifier: J.Identifier): boolean {
        return identifier.simpleName === this.localName &&
            // The specifier binding the name is the one edit that is not a reference to it.
            !this.cursor.firstEnclosing((v): v is JS.Import => (v as J | undefined)?.kind === JS.Kind.Import) &&
            isValueReference(this.cursor, identifier) &&
            scopeOf(this.cursor).declaringScope(this.localName)?.kind === JS.Kind.CompilationUnit;
    }

    /** A shorthand property's name slot is also the reference to the binding. */
    override async visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, p: P): Promise<J | undefined> {
        const name = propertyAssignment.name.element;
        if (this.renaming && propertyAssignment.initializer === undefined &&
            name.kind === J.Kind.Identifier && (name as J.Identifier).simpleName === this.localName &&
            scopeOf(this.cursor).declaringScope(this.localName)?.kind === JS.Kind.CompilationUnit) {
            // The key names a property rather than the binding, so it carries no attribution,
            // the same way `aliasing` builds a property name that stands for nothing.
            return {
                ...propertyAssignment,
                name: {...propertyAssignment.name, element: {...name, type: undefined, fieldType: undefined}},
                assigmentToken: JS.PropertyAssignment.Token.Colon,
                initializer: {...(name as J.Identifier), prefix: singleSpace, simpleName: this.boundName}
            } as JS.PropertyAssignment;
        }
        return super.visitPropertyAssignment(propertyAssignment, p);
    }
}

/**
 * Rewrites the attribution a move invalidates, onto the module and member it moved to. Applied at
 * a reference to the moved binding. See CLAUDE.md: What a rebind's attribution follows.
 */
class MovedTypes extends TypeVisitor<undefined> {
    private readonly onPath = new Set<Type>();
    private readonly answered = new Map<Type, Type | undefined>();
    private readonly renamed: ReadonlyMap<string, string>;
    private readonly fromMember?: string;
    private readonly toMember?: string;
    private readonly toModule: string;

    constructor(from: {module: string; member?: string}, to: {module: string; member?: string}) {
        super();
        // Where no member is named the two keys coincide, and both name the module.
        this.renamed = new Map([[from.module, to.module], [qualifiedName(from), qualifiedName(to)]]);
        this.fromMember = declaredMember(from);
        this.toMember = declaredMember(to);
        this.toModule = to.module;
    }

    /**
     * A type reached while it is still being visited is a cycle — a class holds a method whose
     * declaring type is that class — and answers with itself, which is what ends the walk. Every
     * type visited is remembered by its answer, so a graph whose references fan out or rejoin is
     * walked once rather than once per path that reaches into it. An answer settled inside a cycle
     * stands for the path it was on, so a walk reusing it can leave a rename unapplied.
     */
    override async visit<T extends Type>(type: T | undefined, p: undefined): Promise<T | undefined> {
        if (type === undefined || this.onPath.has(type)) {
            return type;
        }
        if (this.answered.has(type)) {
            return this.answered.get(type) as T | undefined;
        }
        this.onPath.add(type);
        try {
            const answer = await super.visit(type, p);
            this.answered.set(type, answer);
            return answer;
        } finally {
            this.onPath.delete(type);
        }
    }

    protected override async visitClass(aClass: Type.Class, p: undefined): Promise<Type | undefined> {
        const visited = await super.visitClass(aClass, p) as Type.Class;
        const moved = this.renamed.get(visited.fullyQualifiedName);
        return moved === undefined || moved === visited.fullyQualifiedName
            ? visited
            : {...visited, fullyQualifiedName: moved} as Type.Class;
    }

    protected override async visitMethod(method: Type.Method, p: undefined): Promise<Type | undefined> {
        const visited = await super.visitMethod(method, p) as Type.Method;
        return this.declaresMovedMember(visited.name, visited.declaringType)
            ? {...visited, name: this.toMember} as Type.Method
            : visited;
    }

    protected override async visitVariable(variable: Type.Variable, p: undefined): Promise<Type | undefined> {
        const visited = await super.visitVariable(variable, p) as Type.Variable;
        return this.declaresMovedMember(visited.name, visited.owner)
            ? {...visited, name: this.toMember} as Type.Variable
            : visited;
    }

    /**
     * Whether `name`, declared on `owner`, is the member that moved. `owner` has already followed
     * the move by the time this runs, so it is the module moved *to* that it has to name. A
     * default or namespace binding declares no member, so there is no name for one to take.
     */
    private declaresMovedMember(name: string, owner: Type | undefined): boolean {
        return this.toMember !== undefined && name === this.fromMember && this.toMember !== this.fromMember &&
            owner !== undefined && Type.isFullyQualified(owner) &&
            Type.FullyQualified.getFullyQualifiedName(owner) === this.toModule;
    }
}

/** A member's qualified name, or the module's own where no member is named. */
function qualifiedName(binding: {module: string; member?: string}): string {
    const key = memberName(binding.member);
    return key === undefined || key === '*' ? binding.module : `${binding.module}.${key}`;
}

/** The member the module declares, where the binding names one: a whole module declares none. */
function declaredMember(binding: {module: string; member?: string}): string | undefined {
    const key = memberName(binding.member);
    return key === undefined || key === '*' ? undefined : key;
}

/** Renames the identifier a default (`key` undefined) or namespace clause binds its module under. */
function renameClauseBinding(
    importClause: Draft<JS.ImportClause> | undefined,
    key: string | undefined,
    boundName: string
): void {
    const bound = key === undefined
        ? importClause?.name?.element
        : (importClause?.namedBindings as Draft<JS.Alias> | undefined)?.alias;
    if (bound?.kind === J.Kind.Identifier) {
        (bound as Draft<J.Identifier>).simpleName = boundName;
    }
}

/**
 * Rewrites the specifier importing `key` to import `member` under `boundName` — a bare `{member}`
 * where the two agree, since an alias saying the same thing is noise the source never had.
 */
function rewriteNamedSpecifier(
    importClause: Draft<JS.ImportClause> | undefined,
    key: string,
    member: string,
    boundName: string
): void {
    if (importClause?.namedBindings?.kind !== JS.Kind.NamedImports) {
        return;
    }
    for (const elem of (importClause.namedBindings as Draft<JS.NamedImports>).elements.elements) {
        const specifier = elem.element;
        const node = specifier.specifier;
        if (!namedSpecifierImports(node as JS.ImportSpecifier["specifier"], key)) {
            continue;
        }
        const alias = node.kind === JS.Kind.Alias ? node as Draft<JS.Alias> : undefined;
        const local = (alias?.alias ?? node) as Draft<J.Identifier>;
        if (local.kind !== J.Kind.Identifier) {
            continue;
        }
        if (boundName === member) {
            // An alias node holds the specifier's leading whitespace, so the bare identifier
            // standing in for it takes that prefix.
            specifier.specifier = {...local, prefix: (alias ?? local).prefix, simpleName: member};
        } else if (alias) {
            (alias.propertyName.element as Draft<J.Identifier>).simpleName = member;
            local.simpleName = boundName;
        } else {
            specifier.specifier = aliasing({...local, simpleName: boundName}, member) as Draft<JS.Alias>;
        }
    }
}
