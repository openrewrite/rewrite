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
import {isIdentifier, isStringLiteral, J} from "../java";
import {JS, moduleExportName} from "./tree";
import {JavaScriptVisitor} from "./visitor";
import {
    bindingNames, compilationUnitOf, cursorOf, declarationsOf, isFree, isReference, namesUsedIn, referencesOf, resolve,
    scopeOf, valuesDeclaredAtTopLevel
} from "./scope";
import {Cursor, isTree} from "../tree";
import {
    AddImportOptions, bindImport, bindingShape, existingImportBinding, ExistingImportBinding, hasEsmSyntax, ImportStyle,
    isCommonJs, memberName, moduleNameOf, moduleScopeBindings, nameTaken, RebindGroup, RebindImport, requireBinds,
    requireDeclarationOf, requiredModuleOf, requiredModuleOfDeclaration, requireJoinTarget
} from "./add-import";
import {RemoveImport} from "./remove-import";
import {
    importTypeMember, memberRead, objectRead, RenameMemberSpellings, reExportDeclaration, reExportedName
} from "./member-spellings";
import {sameModule} from "./package-name";
import {
    AmdCalleeOptions, amdBlockOf, bindAmd, calleesOf, dependencyNames, derivedBindingName, enclosingAmdBlock,
    isBindableName, parameterNames, RebindAmdDependency, RemoveAmdDependency
} from "./amd";

/**
 * A bare string is shorthand for `{module}`, the overwhelmingly common call with nothing else to
 * configure. A factory parameter binds a whole module under a name and nothing else, so on the AMD
 * lane `member`, `typeOnly`, `sideEffectOnly`, `onlyIfReferenced`, `quoteStyle` and `style` do not
 * apply: the first three refuse, and the rest have nothing to shape.
 */
export interface MaybeBindOptions extends AddImportOptions {
    /** Callees that introduce an AMD block. UI5 writes `sap.ui.define`, RequireJS and Dojo `define`. */
    amdCallee?: string | readonly string[];
}

/** A bare string is shorthand for `{module}`, the overwhelmingly common call with nothing else to configure. */
export interface MaybeUnbindOptions {
    module: string;

    /** The member to remove; unset removes every unused binding of the module. */
    member?: string;

    /** Callees that introduce an AMD block. UI5 writes `sap.ui.define`, RequireJS and Dojo `define`. */
    amdCallee?: string | readonly string[];
}

export interface MaybeRebindOptions {
    from: {module: string; member?: string};

    /**
     * `alias` names the moved binding, taken verbatim. Left unset, an alias on the `from` side
     * survives the move — `{Old as X}` becomes `{New as X}`, since `X` is a name the file chose —
     * while an unaliased binding takes the new member's name where that name is free. Pin `alias`
     * to the old local name to keep it. See CLAUDE.md: Which name a rebind binds.
     */
    to: {module: string; member?: string; alias?: string};

    /** Callees that introduce an AMD block. UI5 writes `sap.ui.define`, RequireJS and Dojo `define`. */
    amdCallee?: string | readonly string[];
}

export interface ModuleBindings {
    /**
     * The module `localName` refers to, or undefined when it is not a module binding — of any
     * shape, so a namespace import's name answers here even though `maybeBind` will not reuse it
     * for a plain `{module}` request. See `bindingOf`.
     */
    moduleOf(localName: string): string | undefined;

    /**
     * The local name bound to `module`, or undefined when nothing binds it — of any shape: a
     * namespace import counts, even though `maybeBind` will not treat it as answering a plain
     * `{module}` request. What a name can stand in for is `maybeBind`'s question, not this one's.
     */
    bindingOf(module: string): string | undefined;

    /**
     * The lane these bindings come from, and the one `maybeBind` would use. `"none"` is a
     * plain script — no import, export, `require` call, `exports` access, or enclosing AMD block — which
     * `maybeBind` still turns into a module on request; a caller that must not do that checks
     * for `"none"` itself.
     */
    readonly moduleSystem: "esm" | "amd" | "commonjs" | "none";

    /**
     * The export of a module that `ref` names, read where the cursor (or the visitor's) stands on it
     * or on the node holding it, or undefined where it names none: a local, a binding nearer than the
     * module's, an unbound global, or a module object's own name. `ref` is an identifier, a member
     * read through a module object (`NS.X`, `(NS).X`, `NS["X"]`, `NS.f()`), the `X` of
     * `export {X} from "m"`, or an `import()` type: the spellings `maybeRebind` moves.
     * `S` with `import {SFC as S} from "react"`, `R.SFC` with `import * as R from "react"`, and
     * `import("react").SFC` → `{module: "react", member: "SFC"}`
     */
    exportOf(ref: J, options?: ExportOfOptions): ModuleExport | undefined;
}

/** A module export a reference names, and the binding the file reaches it through. */
export interface ModuleExport {
    module: string;

    /** `"SFC"` for `SFC`, for `S` of `{SFC as S}` and for `R.SFC`; `"default"` for a default import's own name. */
    member: string;

    /**
     * A type-only import or re-export binds it, or an `import()` type spells it, so it stands for no
     * value at runtime.
     */
    typeOnly: boolean;

    /**
     * How the file reaches it, so a replacement can be spelled alike: `{kind: "namespace", name: "R"}`
     * for `R.SFC` with `import * as R`, `{kind: "destructured", name: "s"}` for `s` after a top-level
     * `const {useState: s} = React`. `name` is the local name read; an `import()` type and the `X` of
     * `export {X} from "m"` read none.
     */
    via: {
        kind: "named" | "default" | "namespace" | "require" | "amd" | "global" | "import-type" | "destructured" |
            "re-export";
        name?: string;
    };
}

/** What `exportOf` reads as a module object beyond the bindings that are one by their syntax. */
export interface ExportOfOptions {
    /**
     * Modules whose default import is the module itself, as `export =` typings make it under
     * `esModuleInterop`: with `["react"]`, `React.SFC` reads `SFC` through `import React from "react"`.
     * Type attribution does not tell such a default from a default export, so the caller names them.
     */
    defaultIsModule?: readonly string[];

    /** Globals that stand for a module where nothing in the file binds them, as UMD typings declare: `{React: "react"}`. */
    globals?: Readonly<Record<string, string>>;
}

export function moduleBindings(
    from: Cursor | JavaScriptVisitor<any>,
    options?: AmdCalleeOptions
): ModuleBindings {
    const amd = enclosingAmdBlock(from, options);
    if (amd !== undefined) {
        const modules = dependencyNames(amd.block);
        const bindings = parameterNames(amd.block);
        return {
            moduleSystem: "amd",
            moduleOf: localName => {
                const index = bindings.indexOf(localName);
                // `dependencyNames` pads a non-literal element with "" to hold its position; that
                // filler names no module, so it answers neither lookup.
                return index < 0 || modules[index] === "" ? undefined : modules[index];
            },
            bindingOf: module => {
                const index = module === "" ? -1 : modules.indexOf(module);
                return index < 0 ? undefined : bindings[index];
            },
            // A factory parameter binds a whole module, so only a member read through it names an export
            exportOf: (ref, options) => exportRead(from, ref, amd.block.factory, () => undefined, name => {
                const index = bindings.indexOf(name);
                return index < 0 || modules[index] === "" ? undefined : {module: modules[index], kind: "amd", typeOnly: false};
            }, options)
        };
    }

    const cu = compilationUnitOf(from);
    const bound = cu === undefined ? [] : moduleObjectBindings(cu);
    const defaultIsModule = (b: ModuleObjectBinding, options?: ExportOfOptions) =>
        options?.defaultIsModule?.some(module => sameModule(module, b.module)) ?? false;
    return {
        // Read on demand, so a caller asking only for bindings never reads the file's module syntax.
        get moduleSystem() {
            return cu === undefined ? "none" : isCommonJs(cu) ? "commonjs" : hasEsmSyntax(cu) ? "esm" : "none";
        },
        moduleOf: localName => bound.find(b => b.name === localName)?.module,
        bindingOf: module => bound.find(b => sameModule(b.module, module))?.name,
        exportOf: (ref, options) => {
            if (cu === undefined) {
                return undefined;
            }
            const moduleObject = (name: string): ModuleObjectRead | undefined => {
                const object = bound.find(b => b.name === name && !isReassigned(cu, b));
                return object === undefined || object.shape === "default" && !defaultIsModule(object, options)
                    ? undefined
                    : {module: object.module, kind: object.shape, typeOnly: object.typeOnly};
            };
            return exportRead(from, ref, cu, name => {
                const object = bound.find(b => b.name === name);
                if (object !== undefined) {
                    // A module object's own name names no one export, unless it is a default that is not the module
                    return object.shape === "default" && !defaultIsModule(object, options)
                        ? {module: object.module, member: "default", typeOnly: object.typeOnly, via: {kind: "default", name}}
                        : undefined;
                }
                const named = moduleScopeBindings(cu).find(b => b.name === name && b.member !== undefined && b.member !== "*");
                return named?.module === undefined
                    ? destructuredExport(cu, name, moduleObject)
                    : {module: named.module, member: named.member!, typeOnly: named.typeOnly ?? false,
                        via: {kind: "named", name}};
            }, moduleObject, options);
        }
    };
}

/**
 * What `name` reads where the file's top level destructures it from a module object:
 * `s` in `const {useState: s} = React` → `useState` of React's module. Only the top level, as with imports.
 */
function destructuredExport(
    cu: JS.CompilationUnit,
    name: string,
    moduleObject: (name: string) => ModuleObjectRead | undefined
): ModuleExport | undefined {
    // At the top level nothing nearer can bind `NS`, so its name is enough
    const root = new Cursor(cu);
    for (const statement of cu.statements) {
        for (const declaration of declarationsOf(statement.element)) {
            for (const variable of declaration.variables) {
                const member = bindingNames(variable.element.name).find(b => b.name === name)?.member;
                const object = objectRead(variable.element.initializer?.element, root)?.object;
                const read = member === undefined || object === undefined ? undefined : moduleObject(object.simpleName);
                if (read !== undefined) {
                    return {module: read.module, member: member!, typeOnly: read.typeOnly,
                        via: {kind: "destructured", name}};
                }
            }
        }
    }
    return undefined;
}

/**
 * `X` in `export {X} from "m"` and `export {X as Y} from "m"`, which reads `X` of `m` with no binding in the file.
 * `Y` is the name the file publishes, so it reads nothing.
 */
function reExportRead(at: Cursor): ModuleExport | undefined {
    const specifier = [at.parentTree(), at.parentTree(2)].find(c => c?.value.kind === JS.Kind.ExportSpecifier);
    const read = specifier === undefined ? undefined : reExportedName(specifier.value);
    if (specifier === undefined || read?.id !== (at.value as J).id) {
        return undefined;
    }
    // The specifier's list, then the declaration
    const reExport = reExportDeclaration(specifier.parentTree(2)?.value);
    return reExport && {
        module: reExport.module, member: moduleExportName(read),
        typeOnly: reExport.declaration.typeOnly.element || (specifier.value as JS.ExportSpecifier).typeOnly.element,
        via: {kind: "re-export"}
    };
}

/** A binding of a whole module, which a member is read through: `NS` of `import * as NS`. */
interface ModuleObjectRead {
    module: string;
    kind: ModuleExport["via"]["kind"];
    typeOnly: boolean;
}

/**
 * `exportOf` for a lane whose bindings `owner`'s scope declares: the compilation unit's, or an AMD
 * factory's. A name resolving to `owner` reads its binding there; one resolving nowhere may be a
 * module's global. `namedExport` answers a name bound to a member, `moduleObject` one bound to a module.
 */
function exportRead(
    from: Cursor | JavaScriptVisitor<any>,
    ref: J,
    owner: J,
    namedExport: (name: string) => ModuleExport | undefined,
    moduleObject: (name: string) => ModuleObjectRead | undefined,
    options: ExportOfOptions = {}
): ModuleExport | undefined {
    const cursor = from instanceof Cursor ? from : cursorOf(from);
    if (cursor === undefined) {
        return undefined;
    }
    if (ref.kind === JS.Kind.ImportType) {
        return importTypeExport(ref as JS.ImportType);
    }
    // By id, so a visitor's own copy of the node it stands on is that node
    let at = (cursor.value as J | undefined)?.id === ref.id ? cursor : new Cursor(ref, cursor);
    const reExported = reExportRead(at);
    if (reExported !== undefined) {
        return reExported;
    }
    // `f()` and `NS["f"]()` name what they call
    const callee = ref.kind === J.Kind.MethodInvocation && (ref as J.MethodInvocation).select === undefined
        ? (ref as J.MethodInvocation).name
        : ref.kind === JS.Kind.FunctionCall ? (ref as JS.FunctionCall).function?.element : undefined;
    if (callee !== undefined) {
        at = new Cursor(callee, at);
    }
    const name = callee ?? ref;
    // `resolve` answers nothing for a name that declares or names rather than reads
    if (isIdentifier(name)) {
        return resolve(at, name)?.id === owner.id ? namedExport(name.simpleName) : undefined;
    }
    // `NS.X`, `(NS).X`, `NS["X"]` and `NS.X()`, as `maybeRebind` reads them
    const read = memberRead(name, at);
    if (read === undefined) {
        return undefined;
    }
    const {object, at: objectAt, member} = read;
    const global = options.globals?.[object.simpleName];
    const reached = resolve(objectAt, object)?.id === owner.id ? moduleObject(object.simpleName) :
        global !== undefined && isFree(objectAt, object) ? {module: global, kind: "global" as const, typeOnly: false} :
            undefined;
    return reached && {module: reached.module, member, typeOnly: reached.typeOnly,
        via: {kind: reached.kind, name: object.simpleName}};
}

/** `import("m").X`, which names `X` of `m` with no binding in the file; `import("m").X.Y` names `X` too. */
function importTypeExport(importType: JS.ImportType): ModuleExport | undefined {
    const named = importTypeMember(importType);
    return named && {...named, typeOnly: true, via: {kind: "import-type"}};
}

export function isAmdBlock(node: J, options?: AmdCalleeOptions): boolean {
    return node.kind === J.Kind.MethodInvocation &&
        amdBlockOf(node as J.MethodInvocation, calleesOf(options)) !== undefined;
}

interface ModuleObjectBinding {
    name: string;
    module: string;

    /**
     * `"default"` and `"namespace"` bind different values — a namespace object's default sits at
     * `.default` — so only one answers a whole-module request of the matching form. CommonJS has
     * no such split: `"require"` answers either. A caller wanting the namespace asks for
     * `member: "*"`, which is what a module exporting no default is bound by.
     */
    shape: "default" | "namespace" | "require";

    /** A type-only import erases, so the name it binds stands for no value at runtime. */
    typeOnly: boolean;

    /** Whether a `let` or `var` declares it, which the file may assign another value to. Never an import. */
    reassignable?: boolean;
}

/**
 * Whether the file assigns another value to the module object, so reads through it may not read the module.
 * `let R = require("m"); R = other` → true; `let R = require("m")` alone → false.
 */
function isReassigned(cu: JS.CompilationUnit, binding: ModuleObjectBinding): boolean {
    return binding.reassignable === true && referencesOf(new Cursor(cu), binding.name).some(r => r.writes);
}

/**
 * The module a `const X = await import("m")` declaration names, for the one variable it declares.
 * A dynamic import resolves to the module namespace object, the same value `import * as X`
 * binds, so it shares that shape rather than getting one of its own.
 */
function dynamicallyImportedModule(declaration: J.VariableDeclarations): string | undefined {
    const variables = declaration.variables;
    const initializer = variables.length === 1 ? variables[0].element?.initializer?.element : undefined;
    const awaited = initializer?.kind === JS.Kind.Await ? (initializer as JS.Await).expression : undefined;
    // `import(...)` is a keyword, not an identifier, so the parser maps it to `JS.FunctionCall`
    // rather than the `J.MethodInvocation` an ordinary call gets.
    if (awaited?.kind !== JS.Kind.FunctionCall) {
        return undefined;
    }
    const call = awaited as JS.FunctionCall;
    const callee = call.function?.element;
    if (!isImportIdentifier(callee) && !isImportDefer(callee)) {
        return undefined;
    }
    const argument = call.arguments.elements[0]?.element;
    return isStringLiteral(argument) ? moduleNameOf(argument) : undefined;
}

function isImportIdentifier(expression: J | undefined): boolean {
    return expression?.kind === J.Kind.Identifier && (expression as J.Identifier).simpleName === "import";
}

function isImportDefer(expression: J | undefined): boolean {
    return expression?.kind === J.Kind.FieldAccess && isImportIdentifier((expression as J.FieldAccess).target) &&
        (expression as J.FieldAccess).name.element.simpleName === "defer";
}

/** Bindings from top-level `const X = <moduleOf-recognised call>` declarations, all of one shape. */
function wholeModuleBindingsVia(
    cu: JS.CompilationUnit,
    moduleOf: (declaration: J.VariableDeclarations) => string | undefined,
    shape: ModuleObjectBinding["shape"]
): ModuleObjectBinding[] {
    const bindings: ModuleObjectBinding[] = [];
    for (const stmt of cu.statements) {
        for (const declaration of declarationsOf(stmt.element)) {
            const module = moduleOf(declaration);
            const name = declaration.variables[0]?.element?.name;
            if (module !== undefined && name?.kind === J.Kind.Identifier) {
                bindings.push({name: (name as J.Identifier).simpleName, module, shape, typeOnly: false,
                    reassignable: !declaresConst(stmt.element)});
            }
        }
    }
    return bindings;
}

/** Whether `statement` declares `const`, which a list (`const a = 1, NS = require("m")`) spells on the statement. */
function declaresConst(statement: J | undefined): boolean {
    const modifiers = statement?.kind === J.Kind.VariableDeclarations ? (statement as J.VariableDeclarations).modifiers :
        statement?.kind === JS.Kind.ScopedVariableDeclarations ? (statement as JS.ScopedVariableDeclarations).modifiers : [];
    return modifiers.some(m => m.keyword === "const");
}

/**
 * Only whole-module bindings: a named member does not name the module object.
 * `import X = require("m")` counts as `const X = require("m")`.
 */
function moduleObjectBindings(cu: JS.CompilationUnit): ModuleObjectBinding[] {
    const bindings: ModuleObjectBinding[] = [];
    for (const stmt of cu.statements) {
        const statement = stmt.element;
        if (statement?.kind !== JS.Kind.Import) {
            continue;
        }
        const jsImport = statement as JS.Import;
        const clause = jsImport.importClause;
        const typeOnly = clause?.typeOnly ?? false;
        const initializer = jsImport.initializer?.element;
        const required = initializer?.kind === J.Kind.MethodInvocation
            ? requiredModuleOf(initializer as J.MethodInvocation)
            : undefined;
        if (required !== undefined && clause?.name?.element?.kind === J.Kind.Identifier) {
            bindings.push({name: (clause.name.element as J.Identifier).simpleName, module: required, shape: "require", typeOnly});
            continue;
        }
        const specifier = jsImport.moduleSpecifier?.element;
        if (!isStringLiteral(specifier)) {
            continue;
        }
        const module = moduleNameOf(specifier);
        if (clause?.name?.element?.kind === J.Kind.Identifier) {
            bindings.push({name: (clause.name.element as J.Identifier).simpleName, module, shape: "default", typeOnly});
        }
        // `namedBindings` is a `JS.Alias` only for `import * as X from "m"`; a renamed
        // named import (`{a as b}`) nests its alias inside `NamedImports` instead.
        const named = clause?.namedBindings;
        if (named?.kind === JS.Kind.Alias) {
            const alias = (named as JS.Alias).alias;
            if (alias?.kind === J.Kind.Identifier) {
                bindings.push({name: (alias as J.Identifier).simpleName, module, shape: "namespace", typeOnly});
            }
        }
    }
    bindings.push(...wholeModuleBindingsVia(cu, requiredModuleOfDeclaration, "require"));
    bindings.push(...wholeModuleBindingsVia(cu, dynamicallyImportedModule, "namespace"));
    return bindings;
}

/**
 * Whether `binding` answers a whole-module request: the namespace form when `wantsNamespace`, and
 * never a type-only import for a value, which erases and would leave the reference unbound.
 */
function answersWholeModuleRequest(binding: ModuleObjectBinding, wantsNamespace: boolean, typeOnly: boolean): boolean {
    return (typeOnly || !binding.typeOnly) &&
        (binding.shape === "require" || binding.shape === (wantsNamespace ? "namespace" : "default"));
}

/**
 * A local binding for `module` or one of its members, creating one where none exists, or
 * `undefined` where no safe binding is possible. The lane — AMD or ESM/CommonJS — is decided
 * from the cursor; AMD binds only the whole module, so a `member` request there refuses rather
 * than guess. `onlyIfReferenced` defaults to true, so the import may never appear.
 */
export function maybeBind(
    visitor: JavaScriptVisitor<any>,
    options: MaybeBindOptions | string
): string | undefined {
    if (typeof options === "string") {
        options = {module: options};
    }
    const module = moduleNameOf(options.module);
    const key = memberName(options.member);

    const amd = enclosingAmdBlock(visitor, options);
    if (amd !== undefined) {
        if (key !== undefined || options.sideEffectOnly) {
            // Nor can a factory parameter load a module without binding it to a name.
            return undefined;
        }
        return bindAmd(visitor, amd, module, options.alias ?? options.preferredName,
            calleesOf(options), options.alias !== undefined);
    }

    const cu = compilationUnitOf(visitor);
    const isWholeModule = !options.sideEffectOnly && (key === undefined || key === "*");
    if (isWholeModule && cu) {
        const scope = scopeOf(cursorOf(visitor)!);
        const bound = moduleObjectBindings(cu).find(b =>
            sameModule(b.module, module) && answersWholeModuleRequest(b, key === "*", options.typeOnly ?? false) &&
            // A pinned alias asks for a binding of that name, so another name for the same
            // module does not answer it; `bindImport`'s own lookup applies the same rule.
            (options.alias === undefined || b.name === options.alias) &&
            scope.declaringScope(b.name) === cu);
        if (bound !== undefined) {
            return bound.name;
        }
    }

    if (isWholeModule && options.alias === undefined && options.preferredName === undefined &&
        derivedBindingName(module) === undefined) {
        // The module's last path segment is not a legal identifier, and the caller named none
        // of its own — there is no name left to bind it to.
        return undefined;
    }

    // `bindImport`'s own lookup finds and reuses a member-specific binding on its own, so
    // refusal here only has to gate the point where it would create a new one.
    const refuseCreate = (options.typeOnly ?? false) && (options.style === undefined
        ? cu !== undefined && isCommonJs(cu)
        : options.style === ImportStyle.CommonJS);
    return bindImport(visitor, {
        ...options,
        preferredName: options.preferredName ?? (isWholeModule ? derivedBindingName(module) : undefined)
    }, refuseCreate);
}

/**
 * Removes `module`'s import(s) where unused, or one `member` of it — `'default'` and `'*'` select
 * the default and namespace import regardless of local name. A member-scoped request does not
 * apply to an AMD dependency, which binds a module rather than one of its members.
 */
export function maybeUnbind(visitor: JavaScriptVisitor<any>, options: MaybeUnbindOptions | string): void {
    if (typeof options === "string") {
        options = {module: options};
    }
    const callees = calleesOf(options);
    const queued = visitor.afterVisit || [];
    if (!queued.some(v => v instanceof RemoveImport && sameModule(v.module, options.module) && v.member === options.member)) {
        visitor.afterVisit.push(new RemoveImport(options.module, options.member));
    }
    // Both queue unconditionally so the caller need not know which lane the file uses: each
    // visitor removes whatever matching construct it finds, ESM import or AMD dependency, and a
    // file with both gets both removed. `amdCallee` says which block a call means, not how
    // anything prints, so — unlike `preferredName`/`quoteStyle` on the bind side — it is part of
    // the dedup key.
    if (!queued.some(v => v instanceof RemoveAmdDependency && v.module === options.module &&
        v.member === options.member && sameCallees(v.callees, callees))) {
        visitor.afterVisit.push(new RemoveAmdDependency(options.module, options.member, callees));
    }
}

function sameCallees(a: readonly string[], b: readonly string[]): boolean {
    return a.length === b.length && a.every((callee, i) => callee === b[i]);
}

/**
 * Moves the binding for `from` to `to` and answers with the name it now carries, or `undefined`
 * where nothing binds `from` or the move isn't safe. One call moves one binding.
 * Spellings through the module (`NS.Old`, `import("m").Old`, `export {Old} from "m"`) move too, also
 * when nothing binds `from` and the answer is `undefined`, but not when the binding can't move
 * (`const {Old, ...rest} = require("m")`); see {@link RenameMemberSpellings}.
 * Named members of one import moved to one module, a call each, end up in one import of it,
 * whether the calls share a visit or come from separate recipes.
 * On an ES import, every type in the file naming what moved is renamed the way a parse of the result names it.
 * It is renamed, not re-resolved, so a moved member keeps its old declaration's signature.
 */
export function maybeRebind(visitor: JavaScriptVisitor<any>, options: MaybeRebindOptions): string | undefined {
    const amd = enclosingAmdBlock(visitor, options);
    if (amd !== undefined) {
        if (memberName(options.from.member) !== undefined || memberName(options.to.member) !== undefined) {
            return undefined;
        }
        const index = dependencyNames(amd.block).indexOf(options.from.module);
        const binding = index < 0 ? undefined : parameterNames(amd.block)[index];
        if (binding === undefined ||
            // An AMD binding's name is its factory parameter, so naming it something else means
            // rewriting the factory's references, which this lane's dependency swap does not reach.
            (options.to.alias !== undefined && options.to.alias !== binding)) {
            return undefined;
        }
        const callees = calleesOf(options);
        const from = options.from.module;
        if (!(visitor.afterVisit || []).some(v => v instanceof RebindAmdDependency &&
            v.blockId === amd.call.id && v.fromModule === from && sameCallees(v.callees, callees))) {
            visitor.afterVisit.push(new RebindAmdDependency(amd.call.id, from, options.to.module, callees));
        }
        return binding;
    }

    const cu = compilationUnitOf(visitor);
    if (cu === undefined) {
        return undefined;
    }
    const existing = existingImportBinding(cu, options.from.module, options.from.member);
    if (existing === undefined) {
        const bound = moduleScopeBindings(cu).some(b => b.module !== undefined &&
            sameModule(b.module, options.from.module) && b.member === memberName(options.from.member));
        if (!bound) {
            queueRenameMemberSpellings(visitor, cu, options);
        }
        return undefined;
    }
    // A binding leaving a `let` or `var` destructuring would land in a `const` require.
    if (existing.viaRequire && existing.reassignable && !existing.onlyMemberOfStatement) {
        return undefined;
    }
    // `RebindImport` replaces an import it cannot rewrite in place, and a CommonJS file can gain no import.
    if (!existing.viaRequire && isCommonJs(cu) &&
        (!existing.onlyMemberOfStatement || bindingShape(options.from.member) !== bindingShape(options.to.member))) {
        return undefined;
    }
    const boundName = rebindingName(visitor, cu, options, existing);
    if (boundName === undefined) {
        return undefined;
    }
    const queued = visitor.afterVisit || [];
    if (!queued.some(v => v instanceof RebindImport &&
        sameModule(v.from.module, options.from.module) && v.from.member === options.from.member &&
        v.localName === existing.localName)) {
        const statement = existing.statement;
        let group: RebindGroup | undefined;
        if (statement.kind === JS.Kind.Import) {
            group = queued.find((v): v is RebindImport<any> =>
                    v instanceof RebindImport && v.group?.sourceId === statement.id)?.group ??
                new RebindGroup(statement.id, statement as JS.Import);
            group.add(existing.localName, boundName, options.to, existing.typeOnly);
        }
        visitor.afterVisit.push(new RebindImport(options.from, options.to, existing.localName, boundName, group));
    }
    queueRenameMemberSpellings(visitor, cu, options);
    return boundName;
}

/**
 * Queues one {@link RenameMemberSpellings} per move of a named member, after any `RebindImport` of the move.
 * Skips `import D from "m"`: `D.Old` reads the default export, which is the module only with `export =`.
 * Skips a `let` or `var` `NS` of the module that the file assigns to: after `NS = other`, `NS.Old` may not
 * read the module. Only those are looked for, since finding an assignment reads the whole file.
 */
function queueRenameMemberSpellings(
    visitor: JavaScriptVisitor<any>,
    cu: JS.CompilationUnit,
    options: MaybeRebindOptions
): void {
    const from = memberName(options.from.member);
    const to = memberName(options.to.member);
    if (from === undefined || to === undefined || bindingShape(from) !== "named" || bindingShape(to) !== "named" ||
        (visitor.afterVisit || []).some(v => v instanceof RenameMemberSpellings && v.from.member === from &&
            sameModule(v.from.module, options.from.module) && v.to.member === to && sameModule(v.to.module, options.to.module))) {
        return;
    }
    const moduleObjects = new Set(moduleObjectBindings(cu)
        .filter(b => b.shape !== "default" && sameModule(b.module, options.from.module) && !isReassigned(cu, b))
        .map(b => b.name));
    visitor.afterVisit.push(new RenameMemberSpellings(
        {module: options.from.module, member: from}, {module: options.to.module, member: to}, moduleObjects));
}

/**
 * The name the moved binding takes, or undefined where a pinned alias cannot be bound verbatim,
 * since deconflicting it would leave the references the caller emits unbound, or would split the
 * binding from a value of the same name (`sharesNameWithValue`). The rule is
 * CLAUDE.md: Which name a rebind binds.
 */
function rebindingName(
    visitor: JavaScriptVisitor<any>,
    cu: JS.CompilationUnit,
    options: MaybeRebindOptions,
    existing: ExistingImportBinding
): string | undefined {
    const taken = (name: string) => nameTaken(name, noNames, visitor) ||
        (namesUsedIn(cu).has(name) && !reusesTargetImport(cu, name, options.to, existing));
    const alias = options.to.alias;
    if (sharesNameWithValue(cu, existing)) {
        return alias === undefined || alias === existing.localName ? existing.localName : undefined;
    }
    if (alias !== undefined) {
        return isBindableName(alias) && (alias === existing.localName || !taken(alias)) ? alias : undefined;
    }
    const member = memberName(options.to.member);
    if (existing.aliased || member === undefined || member === '*' || member === existing.localName) {
        return existing.localName;
    }
    return taken(member) ? existing.localName : member;
}

/**
 * Whether a module-scope value shares the import's name, so the move keeps it as an alias:
 * `import type {Old} from "m"; const Old = 1` becomes `import type {New as Old} from "m2"; const Old = 1`.
 */
function sharesNameWithValue(cu: JS.CompilationUnit, existing: ExistingImportBinding): boolean {
    return !existing.viaRequire && valuesDeclaredAtTopLevel(cu).has(existing.localName);
}

const noNames: ReadonlySet<string> = new Set();

/** Whether the moved binding can merge into a value import of `to`'s named member that `name` binds. */
function reusesTargetImport(
    cu: JS.CompilationUnit,
    name: string,
    to: MaybeRebindOptions["to"],
    moved: ExistingImportBinding
): boolean {
    const target = existingImportBinding(cu, to.module, to.member);
    // An import and a require of one module are separate declarations of the name.
    // A require takes the move in only where `AddImport` joins it.
    const joinTarget = moved.viaRequire ? requireJoinTarget(cu, to.module) : undefined;
    const joined = !moved.viaRequire || (!moved.reassignable && joinTarget !== undefined &&
        requireBinds(joinTarget, to.module, to.member) === name);
    return target?.localName === name && target.viaRequire === moved.viaRequire && joined &&
        bindingShape(to.member) === "named" && !target.typeOnly && !moved.typeOnly && onlyReferences(cu, name);
}

/**
 * Whether every spelling of `name` outside the imports is a reference. Any binder spells its name
 * in a declaring position, so each reference then reads the import, whatever kind of scope it is in.
 */
function onlyReferences(cu: JS.CompilationUnit, name: string): boolean {
    let references = true;
    const visit = (node: any, parent: Cursor): void => {
        if (!references) {
            return;
        }
        if (Array.isArray(node)) {
            node.forEach(child => visit(child, parent));
            return;
        }
        const cursor = new Cursor(node, parent);
        if (node?.kind === J.Kind.Identifier && node.simpleName === name) {
            references = isReference(cursor, node);
        } else if (isTree(node) || node?.kind === J.Kind.RightPadded || node?.kind === J.Kind.LeftPadded ||
            node?.kind === J.Kind.Container) {
            Object.entries(node).forEach(([key, value]) => key !== 'markers' && visit(value, cursor));
        }
    };
    const root = new Cursor(cu);
    // An import, a top-level require or an `export {…}` binds nothing the name could collide with.
    cu.statements
        .filter(s => s.element?.kind !== JS.Kind.Import && s.element?.kind !== JS.Kind.ExportDeclaration &&
            requireDeclarationOf(s.element) === undefined)
        .forEach(s => visit(s, root));
    return references;
}

/**
 * @deprecated Use {@link maybeUnbind} instead — this is a call-shape change, not a behaviour
 * change: `maybeRemoveImport(v, module, member)` is `maybeUnbind(v, {module, member})`.
 */
export function maybeRemoveImport(visitor: JavaScriptVisitor<any>, module: string, member?: string): void {
    maybeUnbind(visitor, {module, member});
}

/**
 * @deprecated Use {@link maybeBind} instead. Beyond binding through an AMD factory parameter,
 * `maybeBind` binds with `require` where the file binds its modules that way, and returns
 * `undefined` where no legal identifier can be derived from the module and none was named.
 */
export function maybeAddImport(
    visitor: JavaScriptVisitor<any>,
    options: AddImportOptions & { sideEffectOnly: true }
): undefined;
export function maybeAddImport(
    visitor: JavaScriptVisitor<any>,
    options: AddImportOptions & { sideEffectOnly?: false }
): string | undefined;
export function maybeAddImport(
    visitor: JavaScriptVisitor<any>,
    options: AddImportOptions
): string | undefined;
export function maybeAddImport(
    visitor: JavaScriptVisitor<any>,
    options: AddImportOptions
): string | undefined {
    return maybeBind(visitor, options);
}
