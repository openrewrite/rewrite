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
import {Cursor, isTree, Tree} from "../tree";
import {J} from "../java";
import {UUID} from "../uuid";
import {JS} from "./tree";
// scope.ts sits below the visitor, so this stays type-only.
import type {JavaScriptVisitor} from "./visitor";

const noNames: ReadonlySet<string> = new Set();

/**
 * What a name means where it is bound: a value, a type or a namespace, which is what TypeScript
 * resolves a name against. A `const` binds a value, an interface a type, a class both, and the
 * qualifier before a dot in a type reads a namespace, which an interface does not declare. A read
 * with one meaning is hidden only by a binding with that meaning.
 */
export type Meaning = 'value' | 'type' | 'namespace';

/** The names a scope binds, each with the meanings it has there. */
type Bindings = ReadonlyMap<string, ReadonlySet<Meaning>>;

const noBindings: Bindings = new Map();

/** Whether `bindings` holds `name` with `meaning`, or with any meaning where none is asked for. */
function binds(bindings: Bindings, name: string, meaning?: Meaning): boolean {
    const meanings = bindings.get(name);
    return meanings !== undefined && (meaning === undefined || meanings.has(meaning));
}

/** One name a declaration binds, and the meanings it has there. */
interface Bound {
    name: string;
    meanings: readonly Meaning[];
}

function bound(names: string[], ...meanings: Meaning[]): Bound[] {
    return names.map(name => ({name, meanings}));
}

function toBindings(bound: Bound[]): Bindings {
    const bindings = new Map<string, Set<Meaning>>();
    for (const {name, meanings} of bound) {
        let held = bindings.get(name);
        if (!held) {
            bindings.set(name, held = new Set());
        }
        meanings.forEach(meaning => held.add(meaning));
    }
    return bindings;
}

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

    /**
     * Whether this scope or one enclosing it binds `name`. Any meaning counts unless one is asked
     * for, since a new binding collides with a type of its name as much as with a value.
     */
    declares(name: string, meaning?: Meaning): boolean;

    /**
     * The node owning the innermost scope that binds `name`, or undefined. A caller holding a
     * declaration compares this to the node it came from, since anything nearer shadows it.
     */
    declaringScope(name: string, meaning?: Meaning): J | undefined;
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
        names: () => new Set(frameBindings(cursor.value, cursor.parent?.value).keys()),
        walk: visit => {
            for (let c: Cursor | undefined = cursor; c; c = enclosingScopeCursor(c.parent)) {
                if (!visit(scopeAt(c))) {
                    return;
                }
            }
        },
        declares: (name, meaning) => declaringScopeOf(cursor, name, meaning) !== undefined,
        declaringScope: (name, meaning) => declaringScopeOf(cursor, name, meaning)
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
    JS.Kind.CompilationUnit, J.Kind.Block, J.Kind.MethodDeclaration, J.Kind.Lambda, JS.Kind.ArrowFunction,
    J.Kind.ClassDeclaration, J.Kind.TryCatch, J.Kind.ForLoop, J.Kind.ForEachLoop, JS.Kind.ForInLoop,
    JS.Kind.NamespaceDeclaration, JS.Kind.TypeDeclaration, JS.Kind.FunctionType, JS.Kind.MappedType,
    JS.Kind.ConditionalType
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
        declarationNames(node).forEach(({name}) => names.add(name));
        const members = membersOf(node);
        if (!members) {
            return true;
        }
        // The walk ends at this branch, so what else the node holds is read here.
        if (node.kind === J.Kind.ClassDeclaration) {
            typeParameterNames(node).forEach(({name}) => names.add(name));
        } else if (node.kind === J.Kind.NewClass) {
            walk((node as J.NewClass).arguments, collect);
        }
        // A member's name is not one the file binds, though the code inside one still declares names.
        for (const member of members.statements) {
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
 * The names `node` reads and nothing within it binds, so each one reaches a binding further out.
 * A name its parent introduces rather than reads is not one, nor is a name an inner scope rebinds:
 * that reference reads the rebinding. A value position is hidden only by a value binding, a type
 * position only by a type.
 */
export function namesReferencedWithin(node: unknown, cacheKey: object = node as object): ReadonlySet<string> {
    if (cacheKey === null || typeof cacheKey !== 'object') {
        return noNames;
    }
    const cached = referenced.get(cacheKey);
    if (cached) {
        return cached;
    }
    const names = new Set<string>();
    readReferences(node, undefined, undefined, 'value', (identifier, reading, scopes) => {
        if (reading !== undefined && !bindingFrame(scopes, identifier.simpleName, reading)) {
            names.add(identifier.simpleName);
        }
    });
    referenced.set(cacheKey, names);
    return names;
}

/** The scopes enclosing a node, innermost first, each paired with what {@link frameBindings} reads. */
interface Frames {
    node: unknown;
    parent: unknown;
    outer?: Frames;
}

/** The innermost of `scopes` binding `name` as `reading` asks, or undefined where none does. */
function bindingFrame(scopes: Frames | undefined, name: string, reading: Meaning | 'any'): Frames | undefined {
    for (let scope = scopes; scope; scope = scope.outer) {
        if (binds(frameBindings(scope.node, scope.parent), name, reading === 'any' ? undefined : reading)) {
            return scope;
        }
    }
    return undefined;
}

/**
 * What an identifier reads with: one meaning, any meaning a binding of its name has, which is
 * how an export clause or an `import a = NS.b` reads, or undefined where it names rather than reads.
 */
type Reading = Meaning | 'any' | undefined;

/** Told of each identifier a walk finds: what it reads with, and the scopes around it. */
type OnIdentifier = (identifier: J.Identifier, reading: Reading, scopes: Frames | undefined) => void;

/**
 * Walks `node` top-down, telling `onIdentifier` of every identifier. `reading` is what `node`
 * itself reads with, or undefined under a slot that names rather than reads.
 */
function readReferences(
    node: unknown, parent: unknown, scopes: Frames | undefined, reading: Reading, onIdentifier: OnIdentifier
): void {
    if (Array.isArray(node)) {
        node.forEach(child => readReferences(child, parent, scopes, reading, onIdentifier));
        return;
    }
    const kind = (node as { kind?: string } | undefined)?.kind;
    if (kind === J.Kind.RightPadded || kind === J.Kind.LeftPadded) {
        // A naming position reads against the tree parent, so padding forwards the one it was handed.
        readReferences((node as J.RightPadded<any>).element, parent, scopes, reading, onIdentifier);
        return;
    }
    if (kind === J.Kind.Container) {
        readReferences((node as J.Container<any>).elements, parent, scopes, reading, onIdentifier);
        return;
    }
    if (!isTree(node)) {
        return;
    }
    if (kind === J.Kind.Identifier) {
        onIdentifier(node as J.Identifier, reads(node as J.Identifier, parent) ? reading : undefined, scopes);
    }
    const within = scopeKinds.has(kind!) ? {node, parent, outer: scopes} : scopes;
    Object.entries(node as object).forEach(([key, value]) =>
        key !== 'markers' &&
        readReferences(value, node, reachOf(node, parent, key, within), meaningOf(node, key, reading), onIdentifier));
}

/**
 * The scopes the slot `key` of `node` resolves against, `within` being those `node` sits in. A
 * conditional type's `infer` names reach its extends clause and true branch alone, so its check
 * type and false branch read past them.
 */
function reachOf(node: any, parent: any, key: string, within: Frames | undefined): Frames | undefined {
    const pastInferred =
        node.kind === JS.Kind.ConditionalType && key === 'checkType' ||
        node.kind === J.Kind.Ternary && parent?.kind === JS.Kind.ConditionalType && key === 'falsePart';
    return pastInferred ? within?.outer : within;
}

/**
 * What a read in slot `key` of `node` reads with, given what `node` itself reads with. Undefined
 * is a slot that names rather than reads, as does everything under it: an import declares what it
 * spells, an alias's new name is its own, and a re-export's clause names another module's members.
 */
function meaningOf(node: any, key: string, reading: Reading): Reading {
    if (reading === undefined) {
        return undefined;
    }
    if (key === 'typeParameters' || key === 'typeArguments') {
        return 'type';
    }
    switch (node.kind) {
        case JS.Kind.Import:
            // `import a = NS.b` reads `NS` as whatever it is; the rest of an import declares.
            return key === 'initializer' ? 'any' : undefined;
        case JS.Kind.Alias:
            return key === 'alias' ? undefined : reading;
        case JS.Kind.ExportDeclaration:
            if (key !== 'exportClause') {
                return reading;
            }
            return (node as JS.ExportDeclaration).moduleSpecifier === undefined ? 'any' : undefined;
        case JS.Kind.ImportType:
            // `import('m').Foo` names a member of `m`.
            return key === 'qualifier' ? undefined : reading;
        case JS.Kind.NamespaceDeclaration:
            return key === 'name' ? undefined : reading;
        case J.Kind.FieldAccess:
            // In a type, `NS.Foo` reads the namespace `NS`, as `A.B.C` reads `A`.
            return key === 'target' && reading === 'type' ? 'namespace' : reading;
        case JS.Kind.TypeInfo:
        case JS.Kind.TypeDeclaration:
        case J.Kind.TypeParameter:
            return 'type';
        case JS.Kind.TypeQuery:
            // `typeof x` reads the value `x` from within a type.
            return key === 'typeExpression' ? 'value' : reading;
        case JS.Kind.TypePredicate:
            // `x is Foo` names the parameter `x`, a value, from within a return type.
            return key === 'parameterName' ? 'value' : reading;
        case JS.Kind.ComputedPropertyName:
            // `[key]` reads the value `key`, in a type member as much as in an object literal.
            return 'value';
        case JS.Kind.IndexSignatureDeclaration:
            return key === 'typeExpression' ? 'type' : reading;
        case JS.Kind.As:
            return key === 'right' ? 'type' : reading;
        case JS.Kind.SatisfiesExpression:
            return key === 'satisfiesType' ? 'type' : reading;
        case J.Kind.TypeCast:
            return key === 'class' ? 'type' : reading;
        case J.Kind.ClassDeclaration:
            if ((node as J.ClassDeclaration).classKind.type === J.ClassDeclaration.Kind.Type.Interface) {
                return 'type';
            }
            return key === 'implements' ? 'type' : reading;
        default:
            return reading;
    }
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

function boundNames(pattern: J | undefined): string[] {
    return bindingNames(pattern).map(bound => bound.name);
}

function unnamedMembers(bound: { name: string }[]): { name: string }[] {
    return bound.map(({name}) => ({name}));
}

/**
 * The block of members a class, object literal or type literal holds. A member is reached through
 * the value or type holding it, so its name binds nowhere.
 */
function membersOf(node: any): J.Block | undefined {
    switch (node?.kind) {
        case J.Kind.ClassDeclaration:
            return (node as J.ClassDeclaration).body;
        case J.Kind.NewClass:
            return (node as J.NewClass).body;
        case JS.Kind.TypeLiteral:
            return (node as JS.TypeLiteral).members;
        default:
            return undefined;
    }
}

function declaringScopeOf(cursor: Cursor, name: string, meaning?: Meaning): J | undefined {
    for (let c: Cursor | undefined = cursor; c; c = c.parent) {
        if (binds(frameBindings(c.value, c.parent?.value), name, meaning)) {
            return c.value;
        }
    }
    return undefined;
}

/**
 * What one node on the cursor path binds, `parent` being the node it hangs from. Only these two
 * answers turn on the parent; a node alone settles the rest, which is what makes them cacheable.
 */
function frameBindings(node: any, parent: any): Bindings {
    if (node?.kind === J.Kind.Block && membersOf(parent) === node) {
        return noBindings;
    }
    // A function expression is the only function whose own name its body reaches: a declaration's
    // name belongs to the enclosing block, a method's to an instance.
    if (node?.kind === J.Kind.MethodDeclaration && parent?.kind === JS.Kind.StatementExpression) {
        let bindings = selfNamed.get(node);
        if (!bindings) {
            const self = bound(boundNames((node as J.MethodDeclaration).name), 'value');
            selfNamed.set(node, bindings = toBindings([...self, ...readBindings(node)]));
        }
        return bindings;
    }
    return ownBindings(node);
}

function ownBindings(node: any): Bindings {
    if (typeof node !== 'object' || node === null) {
        return noBindings;
    }
    let bindings = frames.get(node);
    if (!bindings) {
        frames.set(node, bindings = toBindings(readBindings(node)));
    }
    return bindings;
}

function readBindings(node: any): Bound[] {
    switch (node.kind) {
        case JS.Kind.CompilationUnit: {
            const statements = (node as JS.CompilationUnit).statements;
            return [...declaredNames(statements), ...hoistedNames(statements)];
        }
        case J.Kind.Block:
            return blockScopedNames((node as J.Block).statements);
        case J.Kind.MethodDeclaration: {
            const method = node as J.MethodDeclaration;
            return [
                ...typeParameterNames(method),
                ...declaredNames(method.parameters.elements),
                ...hoistedNames(method.body)
            ];
        }
        case J.Kind.Lambda: {
            const lambda = node as J.Lambda;
            return [...declaredNames(lambda.parameters.parameters), ...hoistedNames(lambda.body)];
        }
        case J.Kind.ClassDeclaration:
            return [...declarationNames(node), ...typeParameterNames(node)];
        // A namespace is where the functions and `var`s in its body hoist to, its block binding the rest.
        case JS.Kind.NamespaceDeclaration:
            return hoistedNames((node as JS.NamespaceDeclaration).body);
        // An arrow's type parameters and return type sit around its lambda, so the arrow binds the
        // parameters its return type reads, a predicate's among them.
        case JS.Kind.ArrowFunction:
            return [...typeParameterNames(node), ...declaredNames((node as JS.ArrowFunction).lambda.parameters.parameters)];
        case JS.Kind.TypeDeclaration:
            return typeParameterNames(node);
        case JS.Kind.FunctionType:
            return [...typeParameterNames(node), ...declaredNames((node as JS.FunctionType).parameters.elements)];
        case JS.Kind.MappedType:
            return declarationNames(unwrap((node as JS.MappedType).keysRemapping.typeParameter));
        case JS.Kind.ConditionalType:
            return inferredNames((node as JS.ConditionalType).condition.element.condition);
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
function blockScopedNames(statements: any[]): Bound[] {
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
function declaredNames(statements: any[]): Bound[] {
    return statements.flatMap(statement => declarationNames(unwrap(statement)));
}

/** The names a declaration binds, each with the meanings it has there. */
function declarationNames(statement: any): Bound[] {
    switch (statement?.kind) {
        case JS.Kind.Import:
            return importNames(statement as JS.Import);
        case J.Kind.VariableDeclarations:
            return bound((statement as J.VariableDeclarations).variables
                .flatMap(variable => boundNames(unwrap(variable)?.name)), 'value');
        case JS.Kind.ScopedVariableDeclarations:
            return declaredNames((statement as JS.ScopedVariableDeclarations).variables);
        case J.Kind.MethodDeclaration:
            return bound(boundNames((statement as J.MethodDeclaration).name), 'value');
        case J.Kind.ClassDeclaration: {
            const declaration = statement as J.ClassDeclaration;
            switch (declaration.classKind.type) {
                case J.ClassDeclaration.Kind.Type.Interface:
                    return bound(boundNames(declaration.name), 'type');
                case J.ClassDeclaration.Kind.Type.Enum:
                    // An enum's members are read as `E.A` in a type, so it is a namespace too.
                    return bound(boundNames(declaration.name), 'value', 'type', 'namespace');
                default:
                    return bound(boundNames(declaration.name), 'value', 'type');
            }
        }
        case JS.Kind.NamespaceDeclaration: {
            const declaration = statement as JS.NamespaceDeclaration;
            const names = boundNames(rootName(unwrap(declaration.name)));
            return isInstantiated(declaration) ? bound(names, 'namespace', 'value') : bound(names, 'namespace');
        }
        case JS.Kind.TypeDeclaration:
            return bound(boundNames(unwrap((statement as JS.TypeDeclaration).name)), 'type');
        case J.Kind.TypeParameter:
            // A type parameter binds across the declaration carrying it, not in the scope that one
            // sits in, so no statement list leads here.
            return bound(boundNames((statement as J.TypeParameter).name), 'type');
        case JS.Kind.MappedTypeParameter:
            return bound(boundNames((statement as JS.MappedType.Parameter).name as J), 'type');
        case J.Kind.Case:
            // The cases of a switch share the block it opens, so each one's declarations bind in all.
            return declaredNames((statement as J.Case).statements.elements);
        default:
            return [];
    }
}

/**
 * Whether a namespace exists at runtime, which it does once its body holds something other than
 * interfaces, type aliases, const enums, unexported imports and namespaces that do not. That is
 * TypeScript's rule, and only such a namespace binds a value.
 */
function isInstantiated(declaration: JS.NamespaceDeclaration): boolean {
    return (declaration.body?.statements ?? []).some(statement => {
        const element = unwrap(statement);
        switch (element?.kind) {
            case JS.Kind.TypeDeclaration:
                return false;
            case J.Kind.ClassDeclaration: {
                const classKind = (element as J.ClassDeclaration).classKind.type;
                return classKind !== J.ClassDeclaration.Kind.Type.Interface &&
                    !(classKind === J.ClassDeclaration.Kind.Type.Enum && isConst(element));
            }
            case JS.Kind.Import:
                return (element as JS.Import).modifiers.some(modifier => modifier.keyword === 'export');
            case JS.Kind.NamespaceDeclaration:
                return isInstantiated(element as JS.NamespaceDeclaration);
            default:
                return true;
        }
    });
}

function isConst(declaration: J.ClassDeclaration): boolean {
    return declaration.modifiers.some(modifier => modifier.keyword === 'const');
}

/** The identifier a dotted name `A.B.C` declares, which is `A`: the rest are members of it. */
function rootName(name: J | undefined): J | undefined {
    return name?.kind === J.Kind.FieldAccess ? rootName((name as J.FieldAccess).target) : name;
}

/**
 * The names a declaration's type parameters bind. A class keeps them in a container and everything
 * else in a `J.TypeParameters`, and both hold the same right-padded list.
 */
function typeParameterNames(node: any): Bound[] {
    const held = node?.typeParameters;
    const parameters = held?.kind === J.Kind.TypeParameters
        ? (held as J.TypeParameters).typeParameters
        : (held as J.Container<J.TypeParameter> | undefined)?.elements;
    return (parameters ?? []).flatMap(parameter => declarationNames(unwrap(parameter)));
}

/**
 * The names the `infer` declarations in a conditional type's extends clause bind. A conditional
 * type nested in the clause keeps its own `infer` names to itself.
 */
function inferredNames(extendsType: unknown): Bound[] {
    const names: Bound[] = [];
    walk(extendsType, node => {
        if (node.kind === JS.Kind.InferType) {
            names.push(...declarationNames(unwrap((node as JS.InferType).typeParameter)));
        }
        return node.kind !== JS.Kind.ConditionalType;
    });
    return names;
}

/**
 * The names an import binds, which for an aliased or namespace specifier is the alias, each with
 * every meaning. A `type` import is no exception: `typeof X` reads it as a value from within a
 * type, and a value use outside one is the checker's error rather than a read of a free name.
 */
function importNames(jsImport: JS.Import): Bound[] {
    const importClause = jsImport.importClause;
    if (!importClause) {
        return [];
    }
    const names = boundNames(unwrap(importClause.name));
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
    return bound(names, 'value', 'type', 'namespace');
}

function aliasedName(specifier: J | undefined): string[] {
    const name = specifier?.kind === JS.Kind.Alias ? (specifier as JS.Alias).alias : specifier;
    return boundNames(name);
}

const blockScoped = new Set(['let', 'const', 'using']);

// A walk of an immutable subtree has one answer, so it runs once. Keyed on the subtree itself, a
// replaced one is walked afresh: every call site in a function asks what that body hoists, every
// import added to a file asks what that file declares, and every scope is asked what it binds by
// each of the call sites it encloses.
const hoisted = new WeakMap<object, Bound[]>();
const declared = new WeakMap<object, ReadonlySet<string>>();
const used = new WeakMap<object, ReadonlySet<string>>();
const referenced = new WeakMap<object, ReadonlySet<string>>();
const resolved = new WeakMap<Tree, Resolutions>();
const frames = new WeakMap<object, Bindings>();
const selfNamed = new WeakMap<object, Bindings>();

/**
 * The names blocks under `scope` hoist out to it. A `var` or function declaration reaches the whole
 * function it sits in, so one nested in a block is in scope outside that block; only a `let`, `const`
 * or `using` keyword says otherwise.
 */
function hoistedNames(scope: any): Bound[] {
    if (typeof scope !== 'object' || scope === null) {
        return [];
    }
    const cached = hoisted.get(scope);
    if (cached) {
        return cached;
    }

    const names: Bound[] = [];
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
            case J.Kind.NewClass:
            case JS.Kind.TypeLiteral:
            case JS.Kind.NamespaceDeclaration:
                // Nothing in an expression, a type or a namespace hoists past it.
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
 * Whether the identifier may be renamed in place. A name its parent introduces may not — a
 * property, a method, a variable, a type parameter — nor one drawn from a namespace of its own: a
 * statement label, declaring (`x:`) or referencing (`break x`), and a JSX attribute's prop.
 * Position is all this reads: an import specifier's own name answers true, and a type position
 * reads alike to a value. A shorthand property `{x}` answers false though it also reads `x`, since
 * a rename has to expand it to `{x: y}`. What `x` reads is {@link resolve}'s question.
 */
export function isReference(cursor: Cursor, identifier: J.Identifier): boolean {
    let c: Cursor | undefined = cursor.parent;
    while (c && isPadding(c.value)) {
        c = c.parent;
    }
    return references(identifier, c?.value);
}

/** @deprecated Use {@link isReference}, the same function under a name that does not suggest it answers for values alone. */
export const isValueReference = isReference;

/**
 * The node owning the scope that binds what `identifier` reads, or undefined where it reads a
 * global or reads nothing: a name its parent introduces, an import's own name, a re-export's.
 * The first question about any identifier resolves every one in the cursor's tree by id, so an
 * identifier the tree does not hold, such as one a recipe built, is refused, as is one whose id
 * the tree holds at two positions. A value read is hidden only by a value of its name and a type
 * read only by a type, so a shorthand `{x}` resolves what `x` reads.
 */
export function resolve(cursor: Cursor, identifier: J.Identifier): J | undefined {
    let root: Tree | undefined;
    for (let c: Cursor | undefined = cursor; c; c = c.parent) {
        if (isTree(c.value)) {
            root = c.value;
        }
    }
    const found = root && resolutions(root);
    if (!found?.answers.has(identifier.id)) {
        throw new Error(`\`${identifier.simpleName}\` is not in the tree the cursor stands in, so nothing resolves it`);
    }
    if (found.doubled.has(identifier.id)) {
        throw new Error(`\`${identifier.simpleName}\` stands at two positions of the tree, so neither resolves it`);
    }
    return found.answers.get(identifier.id);
}

/** Every identifier under one tree by id, with the node owning the scope binding what it reads, if it reads. */
interface Resolutions {
    answers: ReadonlyMap<UUID, J | undefined>;
    /** The ids at more than one position, whose single answer would be the last position's. */
    doubled: ReadonlySet<UUID>;
}

function resolutions(root: Tree): Resolutions {
    let found = resolved.get(root);
    if (!found) {
        const answers = new Map<UUID, J | undefined>();
        const doubled = new Set<UUID>();
        readReferences(root, undefined, undefined, 'value', (identifier, reading, scopes) => {
            if (answers.has(identifier.id)) {
                doubled.add(identifier.id);
            }
            answers.set(identifier.id, reading && bindingFrame(scopes, identifier.simpleName, reading)?.node as J | undefined);
        });
        resolved.set(root, found = {answers, doubled});
    }
    return found;
}

/** The position half of {@link isReference}, against a parent already found. */
function references(identifier: J.Identifier, parent: unknown): boolean {
    const owner = parent as {
        kind?: string; name?: unknown; key?: unknown; label?: unknown; select?: unknown;
        propertyName?: unknown;
    } | undefined;

    // A call names a member of whatever it selects from. With nothing selected there is no member,
    // and its `name` is a reference to the function being called.
    if (owner?.kind === J.Kind.MethodInvocation && !owner.select) {
        return true;
    }

    // A binding element names the property it destructures and binds under `name`, so both slots
    // name rather than reference.
    if (owner?.kind === JS.Kind.BindingElement && holds(owner.propertyName, identifier)) {
        return false;
    }

    // A JSX attribute keeps its prop name on `key`; the three label-bearing nodes keep theirs on
    // `label`. Every other kind that names rather than references keeps it on `name`.
    return !holds(owner?.kind === JS.Kind.JsxAttribute ? owner.key : (owner?.name ?? owner?.label), identifier);
}

/** Whether a node slot, padded or not, is the identifier itself. */
function holds(slot: unknown, identifier: J.Identifier): boolean {
    return slot === identifier || (slot as { element?: unknown } | undefined)?.element === identifier;
}

/** As {@link references}, for a collector rather than a renamer, so a shorthand property counts. */
function reads(identifier: J.Identifier, parent: unknown): boolean {
    const owner = parent as { kind?: string; initializer?: unknown } | undefined;
    return (owner?.kind === JS.Kind.PropertyAssignment && owner.initializer === undefined) ||
        references(identifier, parent);
}

function isPadding(value: unknown): boolean {
    const kind = (value as { kind?: string } | undefined)?.kind;
    return kind === J.Kind.RightPadded || kind === J.Kind.LeftPadded || kind === J.Kind.Container;
}
