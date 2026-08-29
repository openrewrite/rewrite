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
import {J} from "../java";
import {JS} from "./tree";
import {JavaScriptVisitor} from "./visitor";
import {compilationUnitOf, cursorOf, declarationsOf, namesUsedIn, scopeOf, walk} from "./scope";
import {
    AddImportOptions, bindImport, existingImportBinding, ExistingImportBinding, memberName, moduleNameOf,
    nameTaken, RebindImport, requiredModuleOf
} from "./add-import";
import {RemoveImport} from "./remove-import";
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
     * plain script — no import, export, `require` binding, or enclosing AMD block — which
     * `maybeBind` still turns into a module on request; a caller that must not do that checks
     * for `"none"` itself.
     */
    readonly moduleSystem: "esm" | "amd" | "commonjs" | "none";
}

export function moduleBindings(
    visitor: JavaScriptVisitor<any>,
    options?: AmdCalleeOptions
): ModuleBindings {
    const amd = enclosingAmdBlock(visitor, options);
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
            }
        };
    }

    const cu = compilationUnitOf(visitor);
    const bound = cu === undefined ? [] : moduleObjectBindings(cu);
    return {
        moduleSystem: cu === undefined ? "none" :
            isCommonJs(cu) ? "commonjs" :
            hasEsmSyntax(cu) ? "esm" : "none",
        moduleOf: localName => bound.find(b => b.name === localName)?.module,
        bindingOf: module => bound.find(b => b.module === module)?.name
    };
}

export function isAmdBlock(node: J, options?: AmdCalleeOptions): boolean {
    return node.kind === J.Kind.MethodInvocation &&
        amdBlockOf(node as J.MethodInvocation, calleesOf(options)) !== undefined;
}

/** Whether a top-level statement already marks the file as a module: an import or any export form. */
function hasEsmSyntax(cu: JS.CompilationUnit): boolean {
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
}

/** Whether the file binds its modules with `require`, which decides whether a create is possible. */
function isCommonJs(cu: JS.CompilationUnit): boolean {
    if (cu.sourcePath.endsWith(".cjs") || cu.sourcePath.endsWith(".cts")) {
        return true;
    }
    // Node treats these as ES modules regardless of what they contain, the same way
    // `AddImport`'s own `determineImportStyle` reads them as ES6-preferring; `.js`/`.ts`/`.tsx`
    // stay ambiguous and fall through to the statements below.
    if (cu.sourcePath.endsWith(".mjs") || cu.sourcePath.endsWith(".mts")) {
        return false;
    }
    if (hasEsmSyntax(cu)) {
        return false;
    }
    return cu.statements.some(stmt =>
        declarationsOf(stmt.element).some(d => requiredModule(d) !== undefined));
}

/** The module a `const X = require("m")` declaration names, for the one variable it declares. */
function requiredModule(declaration: J.VariableDeclarations): string | undefined {
    const variables = declaration.variables;
    const initializer = variables.length === 1 ? variables[0].element?.initializer?.element : undefined;
    return initializer?.kind === J.Kind.MethodInvocation
        ? requiredModuleOf(initializer as J.MethodInvocation)
        : undefined;
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
    if (callee?.kind !== J.Kind.Identifier || (callee as J.Identifier).simpleName !== "import") {
        return undefined;
    }
    const argument = call.arguments.elements[0]?.element;
    return argument?.kind === J.Kind.Literal && typeof (argument as J.Literal).value === "string"
        ? (argument as J.Literal).value as string
        : undefined;
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
                bindings.push({name: (name as J.Identifier).simpleName, module, shape, typeOnly: false});
            }
        }
    }
    return bindings;
}

/** Only whole-module bindings: a named member does not name the module object. */
function moduleObjectBindings(cu: JS.CompilationUnit): ModuleObjectBinding[] {
    const bindings: ModuleObjectBinding[] = [];
    for (const stmt of cu.statements) {
        const statement = stmt.element;
        if (statement?.kind !== JS.Kind.Import) {
            continue;
        }
        const jsImport = statement as JS.Import;
        const specifier = jsImport.moduleSpecifier?.element;
        if (specifier?.kind !== J.Kind.Literal) {
            continue;
        }
        const module = (specifier as J.Literal).value;
        if (typeof module !== "string") {
            continue;
        }
        const clause = jsImport.importClause;
        const typeOnly = clause?.typeOnly ?? false;
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
    bindings.push(...wholeModuleBindingsVia(cu, requiredModule, "require"));
    bindings.push(...wholeModuleBindingsVia(cu, dynamicallyImportedModule, "namespace"));
    return bindings;
}

/**
 * Whether `binding` answers a whole-module request: the namespace form when `wantsNamespace`, and
 * never a type-only import for a value, which erases and would leave the reference unbound.
 */
function answersWholeModuleRequest(binding: ModuleObjectBinding, wantsNamespace: boolean, typeOnly: boolean): boolean {
    return binding.typeOnly === typeOnly &&
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
            b.module === module && answersWholeModuleRequest(b, key === "*", options.typeOnly ?? false) &&
            // A pinned alias asks for a binding of that name, so another name for the same
            // module does not answer it; `bindImport`'s own lookup applies the same rule.
            (options.alias === undefined || b.name === options.alias) &&
            scope.declaringScope(b.name) === cu);
        if (bound !== undefined) {
            return bound.name;
        }
    }

    if (isWholeModule && options.preferredName === undefined && derivedBindingName(module) === undefined) {
        // The module's last path segment is not a legal identifier, and the caller named no
        // preference of its own — there is no name left to bind it to.
        return undefined;
    }

    // `bindImport`'s own lookup finds and reuses a member-specific binding on its own, so
    // refusal here only has to gate the point where it would create a new one.
    const refuseCreate = cu !== undefined && isCommonJs(cu);
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
    if (!queued.some(v => v instanceof RemoveImport && v.module === options.module && v.member === options.member)) {
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

/** Which of an import clause's three slots `member` binds — `import`, `import *`, or `import {}`. */
function bindingShape(member: string | undefined): "default" | "namespace" | "named" {
    const key = memberName(member);
    return key === undefined ? "default" : key === "*" ? "namespace" : "named";
}

/**
 * Moves the binding for `from` to `to` and answers with the name it now carries — the primitive
 * behind a member rename or a module move. Returns `undefined`, changing nothing, where the move
 * is not safely expressible; the refusals and the choice of name are listed in CLAUDE.md:
 * JavaScript module bindings.
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
        return undefined;
    }
    if (existing.onlyMemberOfStatement && bindingShape(options.from.member) !== bindingShape(options.to.member)) {
        return undefined;
    }
    if (!existing.onlyMemberOfStatement && isCommonJs(cu)) {
        return undefined;
    }
    const boundName = rebindingName(visitor, cu, options, existing);
    if (boundName === undefined) {
        return undefined;
    }
    if (!(visitor.afterVisit || []).some(v => v instanceof RebindImport &&
        v.from.module === options.from.module && v.from.member === options.from.member &&
        v.localName === existing.localName)) {
        visitor.afterVisit.push(new RebindImport(options.from, options.to, existing.localName, boundName));
    }
    return boundName;
}

/**
 * The name the moved binding takes, or undefined where a pinned alias cannot be bound verbatim,
 * since deconflicting it would leave the references the caller emits unbound. The rule is
 * CLAUDE.md: Which name a rebind binds.
 */
function rebindingName(
    visitor: JavaScriptVisitor<any>,
    cu: JS.CompilationUnit,
    options: MaybeRebindOptions,
    existing: ExistingImportBinding
): string | undefined {
    const taken = (name: string) => nameTaken(name, namesUsedIn(cu), visitor);
    const alias = options.to.alias;
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
 * @deprecated Use {@link maybeUnbind} instead — this is a call-shape change, not a behaviour
 * change: `maybeRemoveImport(v, module, member)` is `maybeUnbind(v, {module, member})`.
 */
export function maybeRemoveImport(visitor: JavaScriptVisitor<any>, module: string, member?: string): void {
    maybeUnbind(visitor, {module, member});
}

/**
 * @deprecated Use {@link maybeBind} instead. Beyond binding through an AMD factory parameter,
 * `maybeBind` returns `undefined` rather than creating an import where the file binds its modules
 * with `require`, or where no legal identifier can be derived from the module and none was named.
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
