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
import {Cursor, isTree} from "../tree";
import {J} from "../java";
import {JS} from "./tree";
// scope.ts sits below the visitor, so this stays type-only.
import type {JavaScriptVisitor} from "./visitor";

const noNames: ReadonlySet<string> = new Set();

/** One scope: the names it binds itself, the scopes around it, and what they answer together. */
export interface Scope {
    /** The names this scope binds itself, which is not what it reaches: for that, ask `declares`. */
    names(): ReadonlySet<string>;

    /**
     * Visits this scope and then each one enclosing it, innermost first, stopping where `visit`
     * returns false. Builds a scope per step, so `declares` is the cheaper way to ask about a
     * name already in hand.
     */
    walk(visit: (scope: Scope) => boolean): void;

    /** Whether this scope or one enclosing it binds `name`. */
    declares(name: string): boolean;

    /**
     * The node owning the innermost scope that binds `name` — the compilation unit for a
     * module-scope binding, otherwise the function, block or loop holding it — or undefined where
     * nothing in scope does. A caller holding a declaration asks whether this is the node it came
     * from: anything nearer shadows it.
     */
    declaringScope(name: string): J | undefined;
}

/**
 * The innermost scope holding `cursor`, which a visitor's own cursor rarely is: it stands on
 * whatever node it is visiting. Where a declaration's shape leaves its reach unreadable the answer
 * counts it rather than miss it, so a name reported here may not truly reach the cursor.
 */
export function scopeOf(cursor: Cursor): Scope {
    return scopeAt(enclosingScopeCursor(cursor) ?? cursor);
}

function scopeAt(cursor: Cursor): Scope {
    return {
        names: () => frameBindings(cursor.value, cursor.parent?.value),
        walk: visit => {
            for (let c: Cursor | undefined = cursor; c; c = enclosingScopeCursor(c.parent)) {
                if (!visit(scopeAt(c))) {
                    return;
                }
            }
        },
        declares: name => declaringScopeOf(cursor, name) !== undefined,
        declaringScope: name => declaringScopeOf(cursor, name)
    };
}

function enclosingScopeCursor(from: Cursor | undefined): Cursor | undefined {
    for (let c = from; c; c = c.parent) {
        if (scopeKinds.has((c.value as { kind?: string } | undefined)?.kind!)) {
            return c;
        }
    }
    return undefined;
}

/** The nodes {@link readBindings} answers for; everything else binds nothing and is not a scope. */
const scopeKinds = new Set<string>([
    JS.Kind.CompilationUnit, J.Kind.Block, J.Kind.MethodDeclaration, J.Kind.Lambda,
    J.Kind.ClassDeclaration, J.Kind.TryCatch, J.Kind.ForLoop, J.Kind.ForEachLoop, JS.Kind.ForInLoop
]);

/**
 * Every name the file declares, wherever it sits. A binding the whole file shares is referenced from
 * sites that are not known when it is named, so it has to steer clear of every name that could
 * shadow it at one of them.
 */
export function namesDeclaredIn(cu: JS.CompilationUnit): ReadonlySet<string> {
    return namesDeclaredWithin(cu);
}

/**
 * Every name the file spells, declared or not — what a name has to be absent from to be free. An
 * ambient global is spelled and declared nowhere, and binding over one would capture its uses.
 */
export function namesUsedIn(cu: JS.CompilationUnit): ReadonlySet<string> {
    return namesUsedWithin(cu);
}

/** As {@link namesUsedIn}, over one subtree, for a name that only has to be free across that much. */
export function namesUsedWithin(node: unknown, cacheKey: object = node as object): ReadonlySet<string> {
    if (cacheKey === null || typeof cacheKey !== 'object') {
        return noNames;
    }
    const cached = used.get(cacheKey);
    if (cached) {
        return cached;
    }
    const names = new Set<string>();
    walk(node, node => {
        if (node.kind === J.Kind.Identifier) {
            names.add((node as J.Identifier).simpleName);
        }
        return true;
    });
    used.set(cacheKey, names);
    return names;
}

/** As {@link namesDeclaredIn}, over one subtree, for a binding shared across only that much of a file. */
export function namesDeclaredWithin(node: unknown, cacheKey: object = node as object): ReadonlySet<string> {
    if (cacheKey === null || typeof cacheKey !== 'object') {
        return noNames;
    }
    const cached = declared.get(cacheKey);
    if (cached) {
        return cached;
    }

    const names = new Set<string>();
    const collect = (node: any): boolean => {
        declarationNames(node).forEach(name => names.add(name));
        if (node.kind !== J.Kind.ClassDeclaration) {
            return true;
        }
        // The walk ends at this branch, so the class's own type parameters are read here.
        typeParameterNames(node).forEach(name => names.add(name));
        // A member's name is not one the file binds, though the code inside one still declares names.
        for (const member of (node as J.ClassDeclaration).body?.statements ?? []) {
            const element = unwrap(member);
            walk(element, node => node === element || collect(node));
        }
        return false;
    };
    walk(node, collect);

    declared.set(cacheKey, names);
    return names;
}

/**
 * Every name a binding pattern introduces. `member` is the property a name takes its value from,
 * which only a name an object pattern binds directly has: anything deeper reads a property of a
 * property, an array element is chosen by position, and a rest name gathers what nothing claimed.
 */
export function bindingNames(pattern: J | undefined): { name: string; member?: string }[] {
    switch (pattern?.kind) {
        case J.Kind.Identifier: {
            const simpleName = (pattern as J.Identifier).simpleName;
            return simpleName ? [{name: simpleName}] : [];
        }
        case JS.Kind.Spread:
            return bindingNames((pattern as JS.Spread).expression);
        case JS.Kind.ArrayBindingPattern:
            return unnamedMembers((pattern as JS.ArrayBindingPattern).elements.elements
                .flatMap(element => bindingNames(unwrap(element))));
        case JS.Kind.ObjectBindingPattern:
            return (pattern as JS.ObjectBindingPattern).bindings.elements
                .flatMap(element => bindingNames(unwrap(element)));
        case JS.Kind.BindingElement: {
            const element = pattern as JS.BindingElement;
            if (element.name?.kind !== J.Kind.Identifier) {
                return unnamedMembers(bindingNames(element.name as J));
            }
            const name = (element.name as J.Identifier).simpleName;
            const propertyName = unwrap(element.propertyName);
            return [{
                name,
                member: propertyName?.kind === J.Kind.Identifier ? (propertyName as J.Identifier).simpleName : name
            }];
        }
        default:
            return [];
    }
}

function unnamedMembers(bound: { name: string }[]): { name: string }[] {
    return bound.map(({name}) => ({name}));
}

function declaringScopeOf(cursor: Cursor, name: string): J | undefined {
    for (let c: Cursor | undefined = cursor; c; c = c.parent) {
        if (frameBindings(c.value, c.parent?.value).has(name)) {
            return c.value;
        }
    }
    return undefined;
}

/**
 * What one node on the cursor path binds, `parent` being the node it hangs from. Only these two
 * answers turn on the parent; a node alone settles the rest, which is what makes them cacheable.
 */
function frameBindings(node: any, parent: any): ReadonlySet<string> {
    // A class body holds members, which are reached through an instance rather than by name.
    if (node?.kind === J.Kind.Block && parent?.kind === J.Kind.ClassDeclaration) {
        return noNames;
    }
    // A function expression is the only function whose own name its body reaches: a declaration's
    // name belongs to the enclosing block, a method's to an instance.
    if (node?.kind === J.Kind.MethodDeclaration && parent?.kind === JS.Kind.StatementExpression) {
        const self = bindingNames((node as J.MethodDeclaration).name).map(bound => bound.name);
        return new Set([...self, ...ownBindings(node)]);
    }
    return ownBindings(node);
}

function ownBindings(node: any): ReadonlySet<string> {
    if (typeof node !== 'object' || node === null) {
        return noNames;
    }
    let names = frames.get(node);
    if (!names) {
        frames.set(node, names = new Set(readBindings(node)));
    }
    return names;
}


function readBindings(node: any): string[] {
    switch (node.kind) {
        case JS.Kind.CompilationUnit: {
            const statements = (node as JS.CompilationUnit).statements;
            return [...declaredNames(statements), ...hoistedNames(statements)];
        }
        case J.Kind.Block:
            return blockScopedNames((node as J.Block).statements);
        case J.Kind.MethodDeclaration: {
            const method = node as J.MethodDeclaration;
            return [...declaredNames(method.parameters.elements), ...hoistedNames(method.body)];
        }
        case J.Kind.Lambda: {
            const lambda = node as J.Lambda;
            return [...declaredNames(lambda.parameters.parameters), ...hoistedNames(lambda.body)];
        }
        case J.Kind.ClassDeclaration:
            return bindingNames((node as J.ClassDeclaration).name).map(bound => bound.name);
        case J.Kind.TryCatch:
            return declaredNames([(node as J.Try.Catch).parameter.tree]);
        case J.Kind.ForLoop:
            return declaredNames((node as J.ForLoop).control.init);
        case J.Kind.ForEachLoop:
            return declaredNames([(node as J.ForEachLoop).control.variable]);
        case JS.Kind.ForInLoop:
            return declaredNames([(node as JS.ForInLoop).control.variable]);
        default:
            return [];
    }
}

/**
 * The names statements bind in the block holding them — the complement of what
 * {@link hoistedNames} claims, so `function (x) { var x = 1; }` reads as one binding, not two.
 */
function blockScopedNames(statements: any[]): string[] {
    return statements.flatMap(statement => {
        const element = unwrap(statement);
        switch (element?.kind) {
            case J.Kind.MethodDeclaration:
                return [];
            case J.Kind.VariableDeclarations:
            case JS.Kind.ScopedVariableDeclarations:
                return isBlockScoped(element) ? declarationNames(element) : [];
            case J.Kind.Case:
                return blockScopedNames((element as J.Case).statements.elements);
            default:
                return declarationNames(element);
        }
    });
}

function isBlockScoped(declaration: any): boolean {
    return ((declaration.modifiers ?? []) as J.Modifier[]).some(m => blockScoped.has(m.keyword!));
}

/** The names statements declare directly in the scope holding them. */
function declaredNames(statements: any[]): string[] {
    return statements.flatMap(statement => declarationNames(unwrap(statement)));
}

function declarationNames(statement: any): string[] {
    switch (statement?.kind) {
        case JS.Kind.Import:
            return importNames(statement as JS.Import);
        case J.Kind.VariableDeclarations:
            return (statement as J.VariableDeclarations).variables
                .flatMap(variable => bindingNames(unwrap(variable)?.name))
                .map(bound => bound.name);
        case JS.Kind.ScopedVariableDeclarations:
            return declaredNames((statement as JS.ScopedVariableDeclarations).variables);
        case J.Kind.MethodDeclaration:
            return bindingNames((statement as J.MethodDeclaration).name).map(bound => bound.name);
        case J.Kind.ClassDeclaration:
            return bindingNames((statement as J.ClassDeclaration).name).map(bound => bound.name);
        case JS.Kind.NamespaceDeclaration:
            return bindingNames(unwrap((statement as JS.NamespaceDeclaration).name)).map(bound => bound.name);
        case JS.Kind.TypeDeclaration:
            return bindingNames(unwrap((statement as JS.TypeDeclaration).name)).map(bound => bound.name);
        case J.Kind.TypeParameter:
            // Reached by namesDeclaredWithin's walk, never through a statement list: a type
            // parameter binds across the declaration carrying it, not in the scope that one sits in.
            return bindingNames((statement as J.TypeParameter).name).map(bound => bound.name);
        case J.Kind.Case:
            // The cases of a switch share the block it opens, so each one's declarations bind in all.
            return declaredNames((statement as J.Case).statements.elements);
        default:
            return [];
    }
}

/**
 * The names a declaration's type parameters bind. A class keeps them in a container and everything
 * else in a `J.TypeParameters`, and both hold the same right-padded list.
 */
function typeParameterNames(node: any): string[] {
    const held = node?.typeParameters;
    const parameters = held?.kind === J.Kind.TypeParameters
        ? (held as J.TypeParameters).typeParameters
        : (held as J.Container<J.TypeParameter> | undefined)?.elements;
    return (parameters ?? []).flatMap(parameter => declarationNames(unwrap(parameter)));
}

/** The names an import binds, which for an aliased or namespace specifier is the alias. */
function importNames(jsImport: JS.Import): string[] {
    const importClause = jsImport.importClause;
    if (!importClause) {
        return [];
    }
    const names = bindingNames(unwrap(importClause.name)).map(bound => bound.name);
    const namedBindings = importClause.namedBindings;
    if (namedBindings?.kind === JS.Kind.NamedImports) {
        for (const element of (namedBindings as JS.NamedImports).elements.elements) {
            const specifier = unwrap(element);
            if (specifier?.kind === JS.Kind.ImportSpecifier) {
                names.push(...aliasedName((specifier as JS.ImportSpecifier).specifier));
            }
        }
    } else {
        names.push(...aliasedName(namedBindings));
    }
    return names;
}

function aliasedName(specifier: J | undefined): string[] {
    const name = specifier?.kind === JS.Kind.Alias ? (specifier as JS.Alias).alias : specifier;
    return bindingNames(name).map(bound => bound.name);
}

const blockScoped = new Set(['let', 'const', 'using']);

// A walk of an immutable subtree has one answer, so it runs once. Keyed on the subtree itself, a
// replaced one is walked afresh: every call site in a function asks what that body hoists, every
// import added to a file asks what that file declares, and every scope is asked what it binds by
// each of the call sites it encloses.
const hoisted = new WeakMap<object, string[]>();
const declared = new WeakMap<object, ReadonlySet<string>>();
const used = new WeakMap<object, ReadonlySet<string>>();
const frames = new WeakMap<object, ReadonlySet<string>>();

/**
 * The names blocks under `scope` hoist out to it. A `var` or function declaration reaches the whole
 * function it sits in, so one nested in a block is in scope outside that block; only a `let`, `const`
 * or `using` keyword says otherwise.
 */
function hoistedNames(scope: any): string[] {
    if (typeof scope !== 'object' || scope === null) {
        return [];
    }
    const cached = hoisted.get(scope);
    if (cached) {
        return cached;
    }

    const names: string[] = [];
    const collect = (node: any): boolean => {
        switch (node.kind) {
            case J.Kind.MethodDeclaration:
                names.push(...declarationNames(node));
                return false;
            case J.Kind.VariableDeclarations:
            case JS.Kind.ScopedVariableDeclarations:
                if (!isBlockScoped(node)) {
                    names.push(...declarationNames(node));
                }
                return false;
            case J.Kind.TryCatch:
                // A catch parameter carries no keyword to read, and binds only in the catch block.
                walk((node as J.Try.Catch).body, collect);
                return false;
            case JS.Kind.StatementExpression:
                // A function or class used as an expression names itself for its own body alone.
                return false;
            case J.Kind.Lambda:
            case J.Kind.ClassDeclaration:
                return false;
            default:
                return true;
        }
    };
    walk(scope, collect);
    hoisted.set(scope, names);
    return names;
}

/**
 * Visits every LST node under `node`, leaving a subtree unvisited where `visit` returns false.
 * `visit` sees nodes, never the padding holding them. Only what `isTree` accepts is descended
 * into, which a `JavaType` is not, so the walk stays in the tree the source spells rather than
 * entering the type graph — cyclic and shared, where it would not terminate.
 */
export function walk(node: unknown, visit: (node: any) => boolean): void {
    if (Array.isArray(node)) {
        node.forEach(child => walk(child, visit));
        return;
    }
    const kind = (node as any)?.kind;
    if (kind === J.Kind.RightPadded || kind === J.Kind.LeftPadded) {
        walk((node as J.RightPadded<any>).element, visit);
    } else if (kind === J.Kind.Container) {
        walk((node as J.Container<any>).elements, visit);
    } else if (isTree(node) && visit(node)) {
        // Markers hang off a node rather than being part of the code it holds.
        Object.entries(node).forEach(([key, value]) => key !== 'markers' && walk(value, visit));
    }
}

/** The element a padding wrapper holds, or the node itself. */
function unwrap(node: any): any {
    return node?.kind === J.Kind.RightPadded || node?.kind === J.Kind.LeftPadded ? unwrap(node.element) : node;
}

/** `cursor` is protected on `TreeVisitor` and these APIs are free functions, so reaching it takes a cast. */
export function cursorOf(visitor: JavaScriptVisitor<any>): Cursor | undefined {
    return (visitor as unknown as {cursor?: Cursor}).cursor;
}

/** The compilation unit a cursor sits in, or the one a visitor is currently positioned in. */
export function compilationUnitOf(from: Cursor | JavaScriptVisitor<any>): JS.CompilationUnit | undefined {
    const cursor = from instanceof Cursor ? from : cursorOf(from);
    return cursor?.firstEnclosing((v): v is JS.CompilationUnit => v?.kind === JS.Kind.CompilationUnit);
}

/** `preferred`, or the first `preferred_N` that `isTaken` rejects, so a new name never shadows one in scope. */
export function deconflict(preferred: string, isTaken: (name: string) => boolean): string {
    if (!isTaken(preferred)) {
        return preferred;
    }
    for (let suffix = 1; ; suffix++) {
        const candidate = `${preferred}_${suffix}`;
        if (!isTaken(candidate)) {
            return candidate;
        }
    }
}

/**
 * The `J.VariableDeclarations` a statement declares — itself for a bare `const x = …`, one per
 * declarator for `const a = …, b = …`, which the parser wraps in a `JS.ScopedVariableDeclarations`
 * instead.
 */
export function declarationsOf(statement: J | undefined): J.VariableDeclarations[] {
    if (statement?.kind === J.Kind.VariableDeclarations) {
        return [statement as J.VariableDeclarations];
    }
    if (statement?.kind === JS.Kind.ScopedVariableDeclarations) {
        return (statement as JS.ScopedVariableDeclarations).variables
            .map(v => v.element)
            .filter((v): v is J.VariableDeclarations => v?.kind === J.Kind.VariableDeclarations);
    }
    return [];
}

/**
 * Whether the identifier references a binding rather than naming one, which is what tells a rename
 * which occurrences to follow. A name its parent introduces does not — a property, a method, a
 * variable, a type parameter — nor one drawn from a namespace of its own: a statement label,
 * declaring (`x:`) or referencing (`break x`), and a JSX attribute's prop. Position is all this
 * reads: an import specifier's own name answers true, and a type position reads alike to a value.
 */
export function isValueReference(cursor: Cursor, identifier: J.Identifier): boolean {
    let c: Cursor | undefined = cursor.parent;
    while (c && isPadding(c.value)) {
        c = c.parent;
    }
    const parent = c?.value as
        { kind?: string; name?: unknown; key?: unknown; label?: unknown; select?: unknown } | undefined;

    // A call names a member of whatever it selects from. With nothing selected there is no member,
    // and its `name` is a reference to the function being called.
    if (parent?.kind === J.Kind.MethodInvocation && !parent.select) {
        return true;
    }

    // A JSX attribute keeps its prop name on `key`; the three label-bearing nodes keep theirs on
    // `label`. Every other kind that names rather than references keeps it on `name`.
    const named = parent?.kind === JS.Kind.JsxAttribute ? parent.key : (parent?.name ?? parent?.label);
    return named !== identifier && (named as { element?: unknown } | undefined)?.element !== identifier;
}

function isPadding(value: unknown): boolean {
    const kind = (value as { kind?: string } | undefined)?.kind;
    return kind === J.Kind.RightPadded || kind === J.Kind.LeftPadded || kind === J.Kind.Container;
}
