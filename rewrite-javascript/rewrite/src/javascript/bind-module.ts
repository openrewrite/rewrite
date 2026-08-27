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
import {isIdentifier, J} from "../java";
import {JS} from "./tree";
import {Cursor} from "../tree";
import {JavaScriptVisitor} from "./visitor";
import {ExecutionContext} from "../execution";
import {UUID} from "../uuid";
import {maybeAddImport, moduleNameOf, QuoteChar} from "./add-import";
import {
    AmdBlock, amdBlockOf, DEFAULT_AMD_CALLEES, dependencyNames, elementsOf, parameterNames, present, withDependency,
    withoutDependencyAt
} from "./amd";

export type EsmBindingForm = "default" | "namespace";

export interface BindModuleOptions {
    /**
     * Preferred local name, defaulting to the module's last path segment. A preference: an
     * existing binding or a name already in scope overrides it.
     */
    binding?: string;

    /** The form to create on the ESM lane. Ignored on the AMD lane. */
    esmForm?: EsmBindingForm;

    /** Quote character for a created ESM specifier, defaulting to the file's own. */
    quoteStyle?: QuoteChar;

    /** Callees that introduce an AMD block. */
    amdCallee?: string | readonly string[];
}

export interface ModuleBindings {
    /** The module `localName` refers to, or undefined when it is not a module binding. */
    moduleOf(localName: string): string | undefined;

    /** The local name bound to `module`, or undefined when nothing binds it. */
    bindingOf(module: string): string | undefined;

    /**
     * The lane these bindings come from, and the one `bindModule` would use. `"none"` is a
     * plain script — no import, export, `require` binding, or enclosing AMD block — which
     * `bindModule` still turns into a module on request; a caller that must not do that checks
     * for `"none"` itself.
     */
    readonly moduleSystem: "esm" | "amd" | "commonjs" | "none";
}

export function calleesOf(options?: BindModuleOptions): readonly string[] {
    const callee = options?.amdCallee;
    return callee === undefined ? DEFAULT_AMD_CALLEES : typeof callee === "string" ? [callee] : callee;
}

export function isAmdBlock(node: J, options?: Pick<BindModuleOptions, "amdCallee">): boolean {
    return node.kind === J.Kind.MethodInvocation &&
        amdBlockOf(node as J.MethodInvocation, calleesOf(options)) !== undefined;
}

/** `cursor` is protected on `TreeVisitor` and this API is free functions, so reaching it takes a cast. */
export function cursorOf(visitor: JavaScriptVisitor<any>): Cursor | undefined {
    return (visitor as unknown as {cursor?: Cursor}).cursor;
}

/** The nearest AMD block the cursor sits inside, which is the one a binding belongs to. */
export function enclosingAmdBlock(
    visitor: JavaScriptVisitor<any>,
    options?: BindModuleOptions
): {call: J.MethodInvocation, block: AmdBlock} | undefined {
    const callees = calleesOf(options);
    let cursor = cursorOf(visitor);
    while (cursor !== undefined) {
        const value = cursor.value as J | undefined;
        if (value?.kind === J.Kind.MethodInvocation) {
            const block = amdBlockOf(value as J.MethodInvocation, callees);
            if (block !== undefined) {
                return {call: value as J.MethodInvocation, block};
            }
        }
        cursor = cursor.parent;
    }
    return undefined;
}

export function compilationUnitOf(visitor: JavaScriptVisitor<any>): JS.CompilationUnit | undefined {
    return cursorOf(visitor)?.firstEnclosing(
        (v): v is JS.CompilationUnit => v?.kind === JS.Kind.CompilationUnit);
}

export function moduleBindings(
    visitor: JavaScriptVisitor<any>,
    options?: Pick<BindModuleOptions, "amdCallee">
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
    });
}

interface ModuleObjectBinding {
    name: string;
    module: string;
}

/** Whether the file binds its modules with `require`, which decides whether a create is possible. */
function isCommonJs(cu: JS.CompilationUnit): boolean {
    if (cu.sourcePath.endsWith(".cjs")) {
        return true;
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
    const argument = present(call.arguments.elements)[0]?.element;
    return argument?.kind === J.Kind.Literal && typeof (argument as J.Literal).value === "string"
        ? (argument as J.Literal).value as string
        : undefined;
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
            bindings.push({name: (clause.name.element as J.Identifier).simpleName, module});
        }
        // `namedBindings` is a `JS.Alias` only for `import * as X from "m"`; a renamed
        // named import (`{a as b}`) nests its alias inside `NamedImports` instead.
        const named = clause?.namedBindings;
        if (named?.kind === JS.Kind.Alias) {
            const alias = (named as JS.Alias).alias;
            if (alias?.kind === J.Kind.Identifier) {
                bindings.push({name: (alias as J.Identifier).simpleName, module});
            }
        }
    }
    for (const stmt of cu.statements) {
        const module = requiredModule(stmt.element);
        const name = module === undefined ? undefined :
            (stmt.element as J.VariableDeclarations).variables[0]?.element?.name;
        if (module !== undefined && name?.kind === J.Kind.Identifier) {
            bindings.push({name: (name as J.Identifier).simpleName, module});
        }
    }
    return bindings;
}

/**
 * A local binding for `module`, creating one where the file does not already have it, or
 * `undefined` where no safe binding exists.
 *
 * The name is decided from the cursor and returned at once; the edit that creates the binding
 * is deferred onto `visitor.afterVisit`, as `maybeAddImport`'s is.
 */
export async function bindModule(
    visitor: JavaScriptVisitor<any>,
    module: string | J.Literal,
    options?: BindModuleOptions
): Promise<string | undefined> {
    const moduleName = moduleNameOf(module);
    const amd = enclosingAmdBlock(visitor, options);
    if (amd !== undefined) {
        return bindAmd(visitor, amd, moduleName, options);
    }
    if (compilationUnitOf(visitor) === undefined) {
        // Without a compilation unit there is no lane and no lookup, and the caller emits a
        // reference against whatever comes back.
        return undefined;
    }
    const bindings = moduleBindings(visitor, options);
    const bound = bindings.bindingOf(moduleName);
    if (bound !== undefined) {
        return bound;
    }
    if (bindings.moduleSystem === "commonjs") {
        // A `require` answers for a module it already binds, but creating one is unimplemented,
        // and an `import` in a file that has none would be the wrong form.
        return undefined;
    }
    return maybeAddImport(visitor, {
        module,
        member: options?.esmForm === "namespace" ? "*" : "default",
        preferredName: options?.binding ?? lastSegment(moduleName),
        quoteStyle: options?.quoteStyle,
        onlyIfReferenced: false
    });
}

/** The conventional local name for a module: its last path segment. */
export function lastSegment(module: string): string {
    return module.substring(module.lastIndexOf("/") + 1);
}

async function bindAmd(
    visitor: JavaScriptVisitor<any>,
    amd: {call: J.MethodInvocation, block: AmdBlock},
    module: string,
    options?: BindModuleOptions
): Promise<string | undefined> {
    const modules = dependencyNames(amd.block);
    const bindings = parameterNames(amd.block);

    const declared = modules.indexOf(module);
    if (declared >= 0) {
        return bindings[declared];
    }

    // Two calls naming the same module at the same block are the same request (ADR 0013),
    // answered from whichever reservation is already queued rather than doubling the dependency.
    const queued = queuedFor(visitor, amd.call.id);
    const reserved = queued.find(v => v.module === module)?.binding;
    if (reserved !== undefined) {
        return reserved;
    }

    // A parameter can only be appended at the end, so a block whose dependency and parameter
    // counts already disagree would bind the new module against the wrong parameter either way.
    if (bindings.length !== elementsOf(amd.block).length) {
        return undefined;
    }

    const taken = [...bindings, ...await declaredNames(amd.block, visitor), ...queued.map(v => v.binding)];
    const binding = deconflict(options?.binding ?? lastSegment(module), taken);
    visitor.afterVisit.push(new AddAmdDependency(amd.call.id, module, binding, calleesOf(options)));
    return binding;
}

function deconflict(preferred: string, taken: readonly (string | undefined)[]): string {
    if (!taken.includes(preferred)) {
        return preferred;
    }
    for (let suffix = 1; ; suffix++) {
        const candidate = `${preferred}_${suffix}`;
        if (!taken.includes(candidate)) {
            return candidate;
        }
    }
}

/** Queued reservations already targeting this block: the shared source for both dedup and deconfliction. */
function queuedFor(visitor: JavaScriptVisitor<any>, blockId: UUID): AddAmdDependency<any>[] {
    return visitor.afterVisit.filter((v): v is AddAmdDependency<any> => v instanceof AddAmdDependency && v.blockId === blockId);
}

/**
 * The names the body declares. A new parameter has to avoid all of them, not just the other
 * parameters: a `var Log` in the body would shadow a parameter of the same name, and the
 * rewritten code would quietly read the local instead of the module.
 */
async function declaredNames(block: AmdBlock, visitor: JavaScriptVisitor<any>): Promise<string[]> {
    const body = bodyOf(block);
    if (body === undefined) {
        return [];
    }
    const names: string[] = [];
    const collector = new class extends JavaScriptVisitor<ExecutionContext> {
        override async visitVariableDeclarations(v: J.VariableDeclarations, c: ExecutionContext) {
            for (const variable of v.variables) {
                names.push(...bindingNames(variable.element.name));
            }
            return super.visitVariableDeclarations(v, c);
        }

        override async visitMethodDeclaration(m: J.MethodDeclaration, c: ExecutionContext) {
            names.push(m.name.simpleName);
            return super.visitMethodDeclaration(m, c);
        }

        override async visitClassDeclaration(k: J.ClassDeclaration, c: ExecutionContext) {
            names.push(k.name.simpleName);
            return super.visitClassDeclaration(k, c);
        }
    };
    await collector.visit(body, new ExecutionContext());
    return names;
}

/**
 * Every identifier a binding pattern introduces, recursing through nested destructuring so
 * `const {a: {b}} = m` yields `b`. A plain identifier introduces itself.
 */
function bindingNames(pattern: J | undefined): string[] {
    if (isIdentifier(pattern)) {
        return [pattern.simpleName];
    }
    if (pattern?.kind === JS.Kind.Spread) {
        return bindingNames((pattern as JS.Spread).expression);
    }
    const elements = pattern?.kind === JS.Kind.ObjectBindingPattern
        ? (pattern as JS.ObjectBindingPattern).bindings.elements
        : pattern?.kind === JS.Kind.ArrayBindingPattern
            ? (pattern as JS.ArrayBindingPattern).elements.elements
            : undefined;
    if (elements === undefined) {
        return [];
    }
    const names: string[] = [];
    for (const elem of elements) {
        if (elem.element?.kind === JS.Kind.BindingElement) {
            names.push(...bindingNames((elem.element as JS.BindingElement).name));
        }
    }
    return names;
}

/** The factory's body: a `J.Block` for `function(){}` and most arrows, or the bare expression for `() => expr`. */
export function bodyOf(block: AmdBlock): J | undefined {
    return block.factory.kind === J.Kind.MethodDeclaration ?
        (block.factory as J.MethodDeclaration).body :
        (block.factory as J.Lambda).body;
}

/**
 * Whether the factory body references `name`. The pool is wider than `declaredNames`'s: it
 * matches any identifier of that name, including a member in a field access, since a stray
 * dependency is fine where a shadowed one is not. `missingBodyAnswer` covers a body-less
 * factory: `false` for an addition (nothing to conflict with), `true` for a removal (nothing
 * to prove the binding unused).
 */
export async function references(block: AmdBlock, name: string, missingBodyAnswer: boolean): Promise<boolean> {
    const body = bodyOf(block);
    if (body === undefined) {
        return missingBodyAnswer;
    }
    let found = false;
    const finder = new class extends JavaScriptVisitor<ExecutionContext> {
        override async visitIdentifier(id: J.Identifier, c: ExecutionContext) {
            if (id.simpleName === name) {
                found = true;
            }
            return super.visitIdentifier(id, c);
        }
    };
    await finder.visit(body, new ExecutionContext());
    return found;
}

/**
 * Every name the factory body references, one walk for the whole block rather than one per
 * name. `missingBodyAnswer` means what it means on `references`: the answer `.has` gives, for
 * every name, when the factory has no body to walk.
 */
export async function namesUsed(block: AmdBlock, missingBodyAnswer: boolean): Promise<{has(name: string): boolean}> {
    const body = bodyOf(block);
    if (body === undefined) {
        return {has: () => missingBodyAnswer};
    }
    const names = new Set<string>();
    const collector = new class extends JavaScriptVisitor<ExecutionContext> {
        override async visitIdentifier(id: J.Identifier, c: ExecutionContext) {
            names.add(id.simpleName);
            return super.visitIdentifier(id, c);
        }
    };
    await collector.visit(body, new ExecutionContext());
    return names;
}

/**
 * Applies the dependency a `bindModule` call settled on. The block is found by id: its caller
 * has already emitted a reference to the binding, so a block that cannot be found is an error
 * rather than a skipped edit.
 */
export class AddAmdDependency<P> extends JavaScriptVisitor<P> {
    constructor(
        readonly blockId: UUID,
        readonly module: string,
        readonly binding: string,
        readonly callees: readonly string[]
    ) {
        super();
    }

    private applied = false;

    override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: P): Promise<J | undefined> {
        const visited = await super.visitJsCompilationUnit(cu, p) as JS.CompilationUnit;
        if (!this.applied) {
            throw new Error(
                `No AMD block ${this.blockId} to declare '${this.module}' on, but '${this.binding}' was reported bound`);
        }
        return visited;
    }

    override async visitMethodInvocation(m: J.MethodInvocation, p: P): Promise<J | undefined> {
        const visited = await super.visitMethodInvocation(m, p) as J.MethodInvocation;
        if (visited.id !== this.blockId) {
            return visited;
        }
        const block = amdBlockOf(visited, this.callees);
        if (block === undefined) {
            return visited;
        }
        this.applied = true;
        if (!(await references(block, this.binding, false))) {
            // Nothing referenced the binding, so the caller asked and then did not use the answer.
            return visited;
        }
        // Another edit in the same visit can drop or add a factory parameter, reopening a count
        // mismatch bindAmd's check already cleared — an error here for the same reason a missing
        // block is one.
        const dependency = withDependency(visited, block, this.module, this.binding);
        if (dependency === undefined) {
            throw new Error(
                `AMD block ${this.blockId} no longer has matching dependency and parameter counts, ` +
                `so '${this.module}' could not be declared for '${this.binding}', which was reported bound`);
        }
        return dependency;
    }
}

/**
 * Drops AMD bindings a rewrite left unreferenced. One already unused beforehand stays: it is
 * loaded for its side effects, so dropping it changes what the module loads. Needing the tree as
 * it stood before the rewrite is why this takes both and does not defer. Blocks are matched by
 * the call's id, so one a rewrite rebuilds rather than edits is left alone.
 */
export async function removeNewlyUnusedAmdBindings(
    before: JS.CompilationUnit,
    after: JS.CompilationUnit,
    ctx: ExecutionContext,
    options?: BindModuleOptions
): Promise<JS.CompilationUnit> {
    const callees = calleesOf(options);
    const usedBeforeByBlock = new Map<UUID, Set<string>>();
    const collector = new class extends JavaScriptVisitor<ExecutionContext> {
        override async visitMethodInvocation(m: J.MethodInvocation, c: ExecutionContext) {
            const block = amdBlockOf(m, callees);
            if (block !== undefined) {
                usedBeforeByBlock.set(m.id, await usedBindings(block));
            }
            return super.visitMethodInvocation(m, c);
        }
    };
    await collector.visit(before, ctx);
    if (usedBeforeByBlock.size === 0) {
        return after;
    }

    const sweeper = new class extends JavaScriptVisitor<ExecutionContext> {
        override async visitMethodInvocation(m: J.MethodInvocation, c: ExecutionContext): Promise<J | undefined> {
            let call = await super.visitMethodInvocation(m, c) as J.MethodInvocation;
            const usedBefore = usedBeforeByBlock.get(call.id);
            let block = usedBefore === undefined ? undefined : amdBlockOf(call, callees);
            if (usedBefore === undefined || block === undefined) {
                return call;
            }
            // withoutDependencyAt only edits the dependency array and parameter list, never the
            // body, so which names it references can't change between removals.
            const usedNow = await namesUsed(block, true);
            for (; ;) {
                const bindings = parameterNames(block);
                const goneIndex = bindings.findIndex(binding =>
                    binding !== undefined && usedBefore.has(binding) && !usedNow.has(binding));
                if (goneIndex < 0) {
                    return call;
                }
                call = withoutDependencyAt(call, block, goneIndex);
                // A removal shifts the indices of the dependencies after it, so the block is
                // re-derived from `call` rather than reused with stale offsets.
                const next = amdBlockOf(call, callees);
                if (next === undefined) {
                    return call;
                }
                block = next;
            }
        }
    };
    return await sweeper.visit(after, ctx) as JS.CompilationUnit;
}

/** The block's parameter names that its body actually references, from a single walk of it. */
async function usedBindings(block: AmdBlock): Promise<Set<string>> {
    const usedNames = await namesUsed(block, false);
    const used = new Set<string>();
    for (const binding of parameterNames(block)) {
        if (binding !== undefined && usedNames.has(binding)) {
            used.add(binding);
        }
    }
    return used;
}
