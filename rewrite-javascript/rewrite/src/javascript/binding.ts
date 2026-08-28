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
import {Cursor} from "../tree";
import {JavaScriptVisitor} from "./visitor";
import {scopeOf, walk} from "./scope";
import {AddImportOptions, bindImport, moduleNameOf} from "./add-import";
import {RemoveImport} from "./remove-import";
import {
    AmdCalleeOptions, amdBlockOf, bindAmd, calleesOf, dependencyNames, enclosingAmdBlock, lastSegment,
    parameterNames, RemoveAmdDependency
} from "./amd";

export interface MaybeBindOptions extends AddImportOptions {
    /** Callees that introduce an AMD block. UI5 writes `sap.ui.define`, RequireJS and Dojo `define`. */
    amdCallee?: string | readonly string[];
}

export interface MaybeUnbindOptions {
    module: string;

    /** The member to remove; unset removes every unused binding of the module. */
    member?: string;

    /** Callees that introduce an AMD block. UI5 writes `sap.ui.define`, RequireJS and Dojo `define`. */
    amdCallee?: string | readonly string[];
}

export interface ModuleBindings {
    /** The module `localName` refers to, or undefined when it is not a module binding. */
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

/** `cursor` is protected on `TreeVisitor` and this API is free functions, so reaching it takes a cast. */
function cursorOf(visitor: JavaScriptVisitor<any>): Cursor | undefined {
    return (visitor as unknown as {cursor?: Cursor}).cursor;
}

function compilationUnitOf(visitor: JavaScriptVisitor<any>): JS.CompilationUnit | undefined {
    return cursorOf(visitor)?.firstEnclosing(
        (v): v is JS.CompilationUnit => v?.kind === JS.Kind.CompilationUnit);
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
                return index < 0 ? undefined : modules[index];
            },
            bindingOf: module => {
                const index = modules.indexOf(module);
                return index < 0 ? undefined : bindings[index];
            }
        };
    }

    const cu = compilationUnitOf(visitor);
    const bound = cu === undefined ? [] : moduleObjectBindings(cu);
    return {
        moduleSystem: cu === undefined ? "esm" :
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
     * no such split: `"require"` answers either.
     */
    shape: "default" | "namespace" | "require";
}

/** Whether the file binds its modules with `require`, which decides whether a create is possible. */
function isCommonJs(cu: JS.CompilationUnit): boolean {
    if (cu.sourcePath.endsWith(".cjs")) {
        return true;
    }
    // Node treats these as ES modules regardless of what they contain, the same way
    // `AddImport`'s own `determineImportStyle` reads them as ES6-preferring; `.js`/`.ts`/`.tsx`
    // stay ambiguous and fall through to the statements below.
    if (cu.sourcePath.endsWith(".mjs") || cu.sourcePath.endsWith(".mts")) {
        return false;
    }
    let requires = false;
    for (const stmt of cu.statements) {
        if (stmt.element?.kind === JS.Kind.Import) {
            return false;
        }
        if (requiredModule(stmt.element) !== undefined) {
            requires = true;
        }
    }
    return requires;
}

/** The module a top-level `const X = require("m")` names, for the one variable it declares. */
function requiredModule(statement: J | undefined): string | undefined {
    if (statement?.kind !== J.Kind.VariableDeclarations) {
        return undefined;
    }
    const variables = (statement as J.VariableDeclarations).variables;
    const initializer = variables.length === 1 ? variables[0].element?.initializer?.element : undefined;
    if (initializer?.kind !== J.Kind.MethodInvocation) {
        return undefined;
    }
    const call = initializer as J.MethodInvocation;
    // `obj.require('x')` selects a method rather than loading a module, matching add-import.ts's
    // own `requiredModuleOf`.
    if (call.select || call.name.simpleName !== "require") {
        return undefined;
    }
    const argument = call.arguments.elements[0]?.element;
    return argument?.kind === J.Kind.Literal && typeof (argument as J.Literal).value === "string"
        ? (argument as J.Literal).value as string
        : undefined;
}

/**
 * The module a top-level `const X = await import("m")` names, for the one variable it declares.
 * A dynamic import resolves to the module namespace object, the same value `import * as X`
 * binds, so it shares that shape rather than getting one of its own.
 */
function dynamicallyImportedModule(statement: J | undefined): string | undefined {
    if (statement?.kind !== J.Kind.VariableDeclarations) {
        return undefined;
    }
    const variables = (statement as J.VariableDeclarations).variables;
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

/** Bindings from top-level `const X = <moduleOf-recognised call>` statements, all of one shape. */
function wholeModuleBindingsVia(
    cu: JS.CompilationUnit,
    moduleOf: (statement: J | undefined) => string | undefined,
    shape: ModuleObjectBinding["shape"]
): ModuleObjectBinding[] {
    const bindings: ModuleObjectBinding[] = [];
    for (const stmt of cu.statements) {
        const module = moduleOf(stmt.element);
        const name = module === undefined ? undefined :
            (stmt.element as J.VariableDeclarations).variables[0]?.element?.name;
        if (module !== undefined && name?.kind === J.Kind.Identifier) {
            bindings.push({name: (name as J.Identifier).simpleName, module, shape});
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
        if (clause?.name?.element?.kind === J.Kind.Identifier) {
            bindings.push({name: (clause.name.element as J.Identifier).simpleName, module, shape: "default"});
        }
        // `namedBindings` is a `JS.Alias` only for `import * as X from "m"`; a renamed
        // named import (`{a as b}`) nests its alias inside `NamedImports` instead.
        const named = clause?.namedBindings;
        if (named?.kind === JS.Kind.Alias) {
            const alias = (named as JS.Alias).alias;
            if (alias?.kind === J.Kind.Identifier) {
                bindings.push({name: (alias as J.Identifier).simpleName, module, shape: "namespace"});
            }
        }
    }
    bindings.push(...wholeModuleBindingsVia(cu, requiredModule, "require"));
    bindings.push(...wholeModuleBindingsVia(cu, dynamicallyImportedModule, "namespace"));
    return bindings;
}

/** Whether `binding`'s shape satisfies a whole-module request for the namespace form when `wantsNamespace`. */
function answersWholeModuleRequest(binding: ModuleObjectBinding, wantsNamespace: boolean): boolean {
    return binding.shape === "require" || binding.shape === (wantsNamespace ? "namespace" : "default");
}

/**
 * A local binding for `module` or one of its members, creating one where none exists, or
 * `undefined` where no safe binding is possible. The lane — AMD or ESM/CommonJS — is decided
 * from the cursor; AMD binds only the whole module, so a `member` request there refuses rather
 * than guess. `onlyIfReferenced` defaults to true, so the import may never appear.
 */
export function maybeBind(
    visitor: JavaScriptVisitor<any>,
    options: MaybeBindOptions
): string | undefined {
    const module = moduleNameOf(options.module);

    const amd = enclosingAmdBlock(visitor, options);
    if (amd !== undefined) {
        if (options.member !== undefined || options.sideEffectOnly) {
            // Nor can a factory parameter load a module without binding it to a name.
            return undefined;
        }
        const namesInScope = scopeOf(cursorOf(visitor)!).names();
        return bindAmd(visitor, amd, module, options.preferredName, namesInScope, calleesOf(options));
    }

    const isWholeModule = !options.sideEffectOnly && (options.member === undefined || options.member === "*");
    if (isWholeModule) {
        const cu = compilationUnitOf(visitor);
        const bound = cu && moduleObjectBindings(cu).find(b =>
            b.module === module && answersWholeModuleRequest(b, options.member === "*"));
        if (bound !== undefined) {
            return bound.name;
        }
    }

    // `bindImport`'s own lookup finds and reuses a member-specific binding on its own, so
    // refusal here only has to gate the point where it would create a new one.
    const refuseCreate = moduleBindings(visitor, options).moduleSystem === "commonjs";
    return bindImport(visitor, {
        ...options,
        preferredName: options.preferredName ?? (isWholeModule ? lastSegment(module) : undefined)
    }, refuseCreate);
}

/**
 * Removes `module`'s import(s) where unused, or one `member` of it — `'default'` and `'*'` select
 * the default and namespace import regardless of local name. A member-scoped request does not
 * apply to an AMD dependency, which binds a module rather than one of its members.
 */
export function maybeUnbind(visitor: JavaScriptVisitor<any>, options: MaybeUnbindOptions): void {
    for (const v of visitor.afterVisit || []) {
        if (v instanceof RemoveImport && v.module === options.module && v.member === options.member) {
            return;
        }
    }
    visitor.afterVisit.push(new RemoveImport(options.module, options.member));
    // Both queue unconditionally so the caller need not know which lane the file uses: each
    // visitor removes whatever matching construct it finds, ESM import or AMD dependency, and a
    // file with both gets both removed.
    visitor.afterVisit.push(new RemoveAmdDependency(options.module, options.member, calleesOf(options)));
}

/**
 * @deprecated Use {@link maybeUnbind} instead — this is a call-shape change, not a behaviour
 * change: `maybeRemoveImport(v, module, member)` is `maybeUnbind(v, {module, member})`.
 */
export function maybeRemoveImport(visitor: JavaScriptVisitor<any>, module: string, member?: string): void {
    maybeUnbind(visitor, {module, member});
}

/**
 * @deprecated Use {@link maybeBind} instead — this is a rename, not a behaviour change, beyond
 * `maybeBind` additionally binding through an AMD factory parameter where this only ever imports.
 */
export function maybeAddImport(
    visitor: JavaScriptVisitor<any>,
    options: AddImportOptions & { sideEffectOnly: true }
): undefined;
export function maybeAddImport(
    visitor: JavaScriptVisitor<any>,
    options: AddImportOptions & { sideEffectOnly?: false }
): string;
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
