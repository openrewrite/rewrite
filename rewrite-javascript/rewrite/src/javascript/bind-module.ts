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
    AmdBlock, amdBlockOf, DEFAULT_AMD_CALLEES, dependencyNames, elementsOf, parameterNames, present, withDependency
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

    /** The lane these bindings come from, and the one `bindModule` would use. */
    readonly moduleSystem: "esm" | "amd" | "commonjs";
}

export function calleesOf(options?: BindModuleOptions): readonly string[] {
    const callee = options?.amdCallee;
    return callee === undefined ? DEFAULT_AMD_CALLEES : typeof callee === "string" ? [callee] : callee;
}

export function isAmdBlock(node: J, options?: BindModuleOptions): boolean {
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
    options?: BindModuleOptions
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
        moduleSystem: cu !== undefined && isCommonJs(cu) ? "commonjs" : "esm",
        moduleOf: localName => bound.find(b => b.name === localName)?.module,
        bindingOf: module => bound.find(b => b.module === module)?.name
    };
}

interface ModuleObjectBinding {
    name: string;
    module: string;
}

/** Whether the file binds its modules with `require`, which decides whether a create is possible. */
function isCommonJs(cu: JS.CompilationUnit): boolean {
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
    if (call.name.simpleName !== "require") {
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

    // A parameter can only be appended at the end, so a block declaring more dependencies than
    // the factory takes parameters for would bind the new module against the wrong one.
    if (bindings.length < elementsOf(amd.block).length) {
        return undefined;
    }

    const taken = [
        ...bindings,
        ...await declaredNames(amd.block, visitor),
        ...reservedNames(visitor, amd.call.id)
    ];
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

/** The queue is the reservation record, so calls competing for one name resolve against it. */
function reservedNames(visitor: JavaScriptVisitor<any>, blockId: UUID): string[] {
    return (visitor.afterVisit ?? [])
        .filter((v): v is AddAmdDependency<any> => v instanceof AddAmdDependency && v.blockId === blockId)
        .map(v => v.binding);
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
                if (isIdentifier(variable.element.name)) {
                    names.push((variable.element.name as J.Identifier).simpleName);
                }
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

function bodyOf(block: AmdBlock): J.Block | undefined {
    const body = block.factory.kind === J.Kind.MethodDeclaration ?
        (block.factory as J.MethodDeclaration).body :
        (block.factory as J.Lambda).body;
    return body !== undefined && body.kind === J.Kind.Block ? body as J.Block : undefined;
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
        // withDependency only refuses on the parameter gap bindAmd already ruled out before queuing.
        return withDependency(visited, block, this.module, this.binding) ?? visited;
    }
}
