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

/** The names code at some position can reach unqualified. */
export interface Scope {
    /** Whether this scope or one enclosing it binds `name`. */
    declares(name: string): boolean;

    names(): ReadonlySet<string>;
}

/**
 * What the code at `cursor` can name: what every enclosing scope binds, plus the declarations that
 * hoist into those scopes from blocks that do not enclose it. Where a declaration's shape leaves its
 * reach unreadable the answer counts it rather than miss it, so a name reported here may not truly
 * reach the cursor.
 */
export function scopeOf(cursor: Cursor): Scope {
    let resolved: Set<string> | undefined;
    const names = () => resolved ??= namesInScope(cursor);
    return {declares: name => names().has(name), names};
}

/**
 * Every name the file declares, wherever it sits. A binding the whole file shares is referenced from
 * sites that are not known when it is named, so it has to steer clear of every name that could
 * shadow it at one of them.
 */
export function namesDeclaredIn(cu: JS.CompilationUnit): ReadonlySet<string> {
    const cached = declared.get(cu);
    if (cached) {
        return cached;
    }

    const names = new Set<string>();
    const collect = (node: any): boolean => {
        declarationNames(node).forEach(name => names.add(name));
        if (node.kind !== J.Kind.ClassDeclaration) {
            return true;
        }
        // A member's name is not one the file binds, though the code inside one still declares names.
        for (const member of (node as J.ClassDeclaration).body?.statements ?? []) {
            const element = unwrap(member);
            walk(element, node => node === element || collect(node));
        }
        return false;
    };
    walk(cu.statements, collect);

    declared.set(cu, names);
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

function namesInScope(cursor: Cursor): Set<string> {
    const names = new Set<string>();
    for (let c: Cursor | undefined = cursor; c; c = c.parent) {
        for (const name of frameBindings(c.value, c.parent?.value)) {
            names.add(name);
        }
    }
    return names;
}

/** What one node on the cursor path binds, `parent` being the node it hangs from. */
function frameBindings(node: any, parent: any): string[] {
    switch (node?.kind) {
        case JS.Kind.CompilationUnit: {
            const statements = (node as JS.CompilationUnit).statements;
            return [...declaredNames(statements), ...hoistedNames(statements)];
        }
        case J.Kind.Block:
            // A class body holds members, which are reached through an instance rather than by name.
            return parent?.kind === J.Kind.ClassDeclaration ? [] : declaredNames((node as J.Block).statements);
        case J.Kind.MethodDeclaration: {
            const method = node as J.MethodDeclaration;
            // A function expression is the only function whose own name its body reaches: a
            // declaration's name belongs to the enclosing block, a method's to an instance.
            const self = parent?.kind === JS.Kind.StatementExpression ? bindingNames(method.name) : [];
            return [
                ...self.map(bound => bound.name),
                ...declaredNames(method.parameters.elements),
                ...hoistedNames(method.body)
            ];
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
        case J.Kind.Case:
            // The cases of a switch share the block it opens, so each one's declarations bind in all.
            return declaredNames((statement as J.Case).statements.elements);
        default:
            return [];
    }
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
// replaced one is walked afresh: every call site in a function asks what that body hoists, and every
// import added to a file asks what that file declares.
const hoisted = new WeakMap<object, string[]>();
const declared = new WeakMap<JS.CompilationUnit, ReadonlySet<string>>();

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
                if (!((node.modifiers ?? []) as J.Modifier[]).some(m => blockScoped.has(m.keyword!))) {
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

/** Visits every LST node under `node`, leaving a subtree unvisited where `visit` returns false. */
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
