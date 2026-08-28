import {JavaScriptVisitor} from "./visitor";
import {ElementRemovalFormatter, emptySpace, isIdentifier, J, rightPadded, singleSpace, space, Statement, Type} from "../java";
import {JS, JSX} from "./tree";
import {randomId} from "../uuid";
import {emptyMarkers, markers} from "../markers";
import {getStyle, PrettierStyle, SpacesStyle, StyleKind} from "./style";
import {Cursor} from "../tree";
import {bindingNames, namesDeclaredIn} from "./scope";
import {create as produce, Draft} from "mutative";

export type QuoteChar = "'" | '"';

export enum ImportStyle {
    ES6Named,      // import { x } from 'module'
    ES6Namespace,  // import * as x from 'module'
    ES6Default,    // import x from 'module'
    CommonJS       // const x = require('module')
}

export interface AddImportOptions {
    /** The module name (e.g., 'fs', 'react') to import from.
     * Pass a `J.Literal` to reuse its source form verbatim, which carries the quoting,
     * escapes and unicode form of a specifier being moved from elsewhere in the source. */
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
     * A `J.Literal` module carries its own quoting and takes precedence over this. */
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

    // An import already serving this request answers it; queuing one would, on the next cycle,
    // derive a suffixed name from the binding this call just added. A caller that named a preference
    // takes whatever comes back; one that did not assumes the name it derived, so a binding under
    // any other name would leave the references it emits unbound.
    for (const binding of moduleScopeBindings(cu)) {
        if (binding.module === module && binding.member === memberName(options.member) &&
            binding.typeOnly === typeOnly &&
            (options.preferredName !== undefined || anyNameAnswers(options) ||
                binding.name === derived)) {
            return binding.name;
        }
    }

    if (refuseCreate) {
        return undefined;
    }

    // Only the module scope answers for a name, but any scope in the file occupies one. The queue
    // gives every later request for this module the name chosen here, so it has to clear the scopes
    // those references will sit in, which are not known yet.
    const name = deconflict(derived, takenNames(namesDeclaredIn(cu), visitor));
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
interface ModuleScopeBinding {
    name: string;
    module?: string;
    /** Carries the {@link AddImportOptions.member} spelling, so `undefined` means a default import. */
    member?: string;
    typeOnly?: boolean;
}

function cursorOf(visitor: JavaScriptVisitor<any>): Cursor | undefined {
    // `cursor` is protected on `TreeVisitor`, and `bindImport`/`maybeRemoveImport` are free
    // functions, so reaching it takes a cast.
    return (visitor as unknown as { cursor?: Cursor }).cursor;
}

function compilationUnitOf(cursor: Cursor): JS.CompilationUnit | undefined {
    return cursor.firstEnclosing((v): v is JS.CompilationUnit => v?.kind === JS.Kind.CompilationUnit);
}

/** What the file's imports and `require`s bind at module scope, and the module each name comes from. */
function moduleScopeBindings(cu: JS.CompilationUnit): ModuleScopeBinding[] {
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
        switch (statement?.kind) {
            case JS.Kind.Import:
                bindings.push(...importBindings(statement as JS.Import));
                break;
            case J.Kind.VariableDeclarations:
                declaredByVariables(statement as J.VariableDeclarations);
                break;
            case JS.Kind.ScopedVariableDeclarations:
                for (const variable of (statement as JS.ScopedVariableDeclarations).variables) {
                    if (variable.element?.kind === J.Kind.VariableDeclarations) {
                        declaredByVariables(variable.element as J.VariableDeclarations);
                    }
                }
                break;
        }
    }

    return bindings;
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
function requiredModuleOf(methodInv: J.MethodInvocation): string | undefined {
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
 * Names in scope, plus those pending `AddImport`s on the `afterVisit` queue have claimed. A queued
 * `RemoveImport` does not free one: it removes only what the file leaves unused, and binding a name
 * it keeps is an error, where an unnecessary suffix merely reads oddly.
 */
function takenNames(inScope: ReadonlySet<string>, visitor: JavaScriptVisitor<any>): Set<string> {
    const taken = new Set<string>(inScope);

    for (const v of visitor.afterVisit || []) {
        if (v instanceof AddImport && v.bindingName) {
            taken.add(v.bindingName);
        }
    }

    return taken;
}

function deconflict(derived: string, taken: Set<string>): string {
    if (!taken.has(derived)) {
        return derived;
    }
    let suffix = 1;
    while (taken.has(`${derived}_${suffix}`)) {
        suffix++;
    }
    return `${derived}_${suffix}`;
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

    const prettier = getStyle(StyleKind.PrettierStyle, cu) as PrettierStyle | undefined;
    if (prettier?.kind === StyleKind.PrettierStyle && !prettier.ignored) {
        const singleQuote = prettier.config.singleQuote;
        if (typeof singleQuote === 'boolean') {
            return singleQuote ? "'" : '"';
        }
    }

    return double > single ? '"' : "'";
}

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

        // Add the import using the appropriate style
        if (importStyle === ImportStyle.CommonJS) {
            // TODO: Implement CommonJS require creation
            // For now, fall back to ES6 imports
            // return this.addCommonJSRequire(compilationUnit, p);
        }

        // Add ES6 import (handles ES6Named, ES6Namespace, ES6Default)
        return this.produceJavaScript(compilationUnit, p, async draft => {
            // Find the position to insert the import
            const insertionIndex = this.findImportInsertionIndex(compilationUnit);

            const newImport = await this.createImportStatement(compilationUnit, insertionIndex, p);

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
                    return this.produceJavaScript(compilationUnit, p, async draft => {
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
                    });
                }

                // Case 2: Default import without named bindings - add named bindings
                // Transform: import React from 'react' -> import React, { useState } from 'react'
                if (importClause.name && !importClause.namedBindings) {
                    return this.produceJavaScript(compilationUnit, p, async draft => {
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
                    });
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

        // Create the module specifier
        // For side-effect imports, use emptySpace since space comes from LeftPadded.before
        // For regular imports with import clause, use emptySpace since space comes from LeftPadded.before
        // However, the printer expects the space after 'from' in the literal's prefix
        // Note: value is the unquoted module name; valueSource and unicodeEscapes are its printed form
        let valueSource = this.moduleValueSource;
        if (valueSource === undefined) {
            const quote = this.quoteStyle ?? await detectQuote(compilationUnit);
            valueSource = `${quote}${this.module}${quote}`;
        }
        const moduleSpecifier: J.Literal = {
            id: randomId(),
            kind: J.Kind.Literal,
            prefix: this.sideEffectOnly ? emptySpace : singleSpace,
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
                prefix: singleSpace,
                markers: emptyMarkers,
                propertyName: rightPadded(propertyName, singleSpace),
                alias: aliasIdentifier
            };

            importClause = {
                id: randomId(),
                kind: JS.Kind.ImportClause,
                prefix: this.typeOnly ? singleSpace : emptySpace,
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
                prefix: singleSpace,
                markers: emptyMarkers,
                annotations: [],
                simpleName: this.bindingName!,
                type: undefined,
                fieldType: undefined
            };

            importClause = {
                id: randomId(),
                kind: JS.Kind.ImportClause,
                prefix: this.typeOnly ? singleSpace : emptySpace,
                markers: emptyMarkers,
                typeOnly: this.typeOnly,
                name: rightPadded(defaultName, emptySpace),
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
                prefix: singleSpace,
                markers: emptyMarkers,
                elements: {
                    kind: J.Kind.Container,
                    before: emptySpace,
                    elements: [rightPadded(importSpecWithSpacing, braceSpace)],
                    markers: emptyMarkers
                }
            };

            importClause = {
                id: randomId(),
                kind: JS.Kind.ImportClause,
                prefix: this.typeOnly ? singleSpace : emptySpace,
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
                before: singleSpace,
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
        if (isIdentifier(specifierNode) && specifierNode.simpleName === key) {
            return specifierNode.simpleName;
        }
        if (specifierNode.kind === JS.Kind.Alias) {
            const alias = specifierNode as JS.Alias;
            const propertyName = alias.propertyName.element;
            if (isIdentifier(propertyName) && propertyName.simpleName === key && isIdentifier(alias.alias)) {
                return alias.alias.simpleName;
            }
        }
    }
    return undefined;
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
            return {localName, onlyMemberOfStatement: isOnlyMember(element as JS.Import)};
        }
    }
    return undefined;
}

/**
 * Binds `member` under the name `local` already carries, so the file's references to it still
 * resolve. `local` itself becomes the alias, keeping the type attribution it holds, and the alias
 * takes its prefix: that whitespace separates the specifier from a `type` keyword before it.
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
        const specifierNode = entry.element.specifier;
        const matches = (specifierNode.kind === J.Kind.Identifier && specifierNode.simpleName === key) ||
            (specifierNode.kind === JS.Kind.Alias &&
                (specifierNode as JS.Alias).propertyName.element.kind === J.Kind.Identifier &&
                ((specifierNode as JS.Alias).propertyName.element as J.Identifier).simpleName === key);
        if (matches) {
            formatter.markRemoved(entry.element);
        } else {
            kept.push({...entry, element: formatter.processKept(entry.element)});
        }
    }
    const updatedNamedImports: JS.NamedImports = {...namedImports, elements: {...namedImports.elements, elements: kept}};
    return {...jsImport, importClause: {...importClause, namedBindings: updatedNamedImports}};
}

/**
 * Moves the binding `from` names to `to`, keeping the local name it already had. In place when
 * the statement that carries it binds nothing else — module and member specifier rewritten there
 * directly; otherwise the old specifier drops and {@link bindImport} queues the replacement,
 * aliased to the preserved name.
 *
 * Not built on `RemoveImport`/`maybeUnbind`: those only drop a binding once nothing references
 * it, but a rebind moves one that is still in use — removal here has to be unconditional.
 */
export class RebindImport<P> extends JavaScriptVisitor<P> {
    constructor(
        readonly from: {module: string; member?: string},
        readonly to: {module: string; member?: string},
        readonly localName: string
    ) {
        super();
    }

    private transformedInPlace = false;
    private typeOnly = false;

    override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: P): Promise<J | undefined> {
        const visited = await super.visitJsCompilationUnit(cu, p) as JS.CompilationUnit;
        if (!this.transformedInPlace) {
            bindImport(this, {
                module: this.to.module,
                member: this.to.member,
                alias: this.localName,
                typeOnly: this.typeOnly,
                onlyIfReferenced: false
            });
        }
        return visited;
    }

    override async visitImportDeclaration(jsImport: JS.Import, p: P): Promise<J | undefined> {
        const imp = await super.visitImportDeclaration(jsImport, p) as JS.Import;

        const key = memberName(this.from.member);
        if (importBinds(imp, this.from.module, this.from.member) === undefined) {
            return imp;
        }
        this.typeOnly = imp.importClause?.typeOnly ?? false;

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

            // A named specifier's local name has to stay put in the source, since it is what the
            // rest of the file already reads; default and namespace imports carry that name on
            // the clause itself, which needs no edit for a module-only move.
            const toKey = memberName(this.to.member);
            if (key !== undefined && key !== '*' && toKey !== undefined && toKey !== key) {
                const importClause = draft.importClause;
                if (importClause?.namedBindings?.kind === JS.Kind.NamedImports) {
                    const namedImports = importClause.namedBindings as Draft<JS.NamedImports>;
                    for (const elem of namedImports.elements.elements) {
                        const specifier = elem.element;
                        if (specifier.specifier.kind === J.Kind.Identifier && specifier.specifier.simpleName === key) {
                            specifier.specifier = aliasing(specifier.specifier as Draft<J.Identifier>, toKey) as Draft<JS.Alias>;
                        } else if (specifier.specifier.kind === JS.Kind.Alias) {
                            const aliasNode = specifier.specifier as Draft<JS.Alias>;
                            const propertyName = aliasNode.propertyName.element;
                            if (propertyName.kind === J.Kind.Identifier && propertyName.simpleName === key) {
                                propertyName.simpleName = toKey;
                            }
                        }
                    }
                }
            }
        });
    }
}
