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
 * What a name means where it is bound, a value, a type or a namespace, which is what TypeScript
 * resolves a name against. A `const` binds a value, an interface a type, a class both, and the
 * qualifier before a dot in a type reads a namespace, which an interface does not declare. A read
 * with one meaning is hidden only by a binding with that meaning.
 */
export type Meaning = 'value' | 'type' | 'namespace';

/** One name a scope binds, with every meaning it has there and the declarations giving it each. */
interface Binding {
    meanings: ReadonlySet<Meaning>;
    declarations: readonly Bound[];
}

type Bindings = ReadonlyMap<string, Binding>;

const noBindings: Bindings = new Map();

/** Whether `bindings` holds `name` with `meaning`, or with any meaning where none is asked for. */
function binds(bindings: Bindings, name: string, meaning?: Meaning): boolean {
    const binding = bindings.get(name);
    return binding !== undefined && (meaning === undefined || binding.meanings.has(meaning));
}

/** One name a declaration binds, as the identifier spelling it, and the meanings it has there. */
interface Bound {
    identifier: J.Identifier;
    meanings: readonly Meaning[];
}

function bound(identifiers: J.Identifier[], ...meanings: Meaning[]): Bound[] {
    return identifiers.map(identifier => ({identifier, meanings}));
}

function toBindings(bound: Bound[]): Bindings {
    const bindings = new Map<string, { meanings: Set<Meaning>; declarations: Bound[] }>();
    for (const declaration of bound) {
        const name = declaration.identifier.simpleName;
        let held = bindings.get(name);
        if (!held) {
            bindings.set(name, held = {meanings: new Set(), declarations: []});
        }
        declaration.meanings.forEach(meaning => held.meanings.add(meaning));
        held.declarations.push(declaration);
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
        names: () => new Set(bindingsAt(cursor).keys()),
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
    JS.Kind.CompilationUnit, J.Kind.Block, J.Kind.MethodDeclaration, JS.Kind.ComputedPropertyMethodDeclaration,
    J.Kind.Lambda, JS.Kind.ArrowFunction, J.Kind.ClassDeclaration, J.Kind.TryCatch, J.Kind.ForLoop,
    J.Kind.ForEachLoop, JS.Kind.ForInLoop, JS.Kind.NamespaceDeclaration, JS.Kind.TypeDeclaration,
    JS.Kind.FunctionType, JS.Kind.MappedType, JS.Kind.ConditionalType
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
    const collect = (node: Tree): boolean => {
        declarationNames(node).forEach(({identifier}) => names.add(identifier.simpleName));
        const members = membersOf(node);
        if (!members) {
            return true;
        }
        // The walk ends at this branch, so what else the node holds is read here.
        if (node.kind === J.Kind.ClassDeclaration) {
            typeParameterNames(node).forEach(({identifier}) => names.add(identifier.simpleName));
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
    treesIn(node).forEach(tree => readReferences(tree, undefined, undefined, 'value', (identifier, reading, scopes) => {
        if (reading !== undefined && !bindingFrame(scopes, identifier.simpleName, reading)) {
            names.add(identifier.simpleName);
        }
    }));
    referenced.set(cacheKey, names);
    return names;
}

/** The scopes enclosing a node, innermost first, each paired with what {@link frameBindings} reads. */
interface Frames {
    node: Tree;
    parent: Tree | undefined;
    outer?: Frames;
}

/** The innermost of `scopes` binding `name` as `reading` asks, or undefined where none does. */
function bindingFrame(
    scopes: Frames | undefined, name: string, reading: Exclude<Reading, undefined>
): Frames | undefined {
    for (let scope = scopes; scope; scope = scope.outer) {
        if (binds(frameBindings(scope.node, scope.parent), name, meaningRead(reading))) {
            return scope;
        }
    }
    return undefined;
}

/**
 * What an identifier reads with. That is one meaning, or `'any'`, which is how an export clause or
 * an `import a = NS.b` reads, or `'write'`, a value it assigns to. Undefined is a slot that names
 * rather than reads.
 */
type Reading = Meaning | 'any' | 'write' | undefined;

/** The meaning a reading resolves against, or undefined for any. */
function meaningRead(reading: Exclude<Reading, undefined>): Meaning | undefined {
    return reading === 'any' ? undefined : reading === 'write' ? 'value' : reading;
}

/**
 * Told of each identifier a walk finds, with what it reads with and the scopes around it. `trail`
 * holds the nodes from the walk's root down to it, and changes as the walk goes on.
 */
type OnIdentifier =
    (identifier: J.Identifier, reading: Reading, scopes: Frames | undefined, trail: readonly Tree[]) => void;

/**
 * Walks `node` top-down, telling `onIdentifier` of every identifier. `reading` is what `node`
 * itself reads with, or undefined under a slot that names rather than reads.
 */
function readReferences(
    node: Tree, parent: Tree | undefined, scopes: Frames | undefined, reading: Reading, onIdentifier: OnIdentifier,
    trail: Tree[] = []
): void {
    trail.push(node);
    if (node.kind === J.Kind.Identifier) {
        onIdentifier(node as J.Identifier, reading, scopes, trail);
    }
    const within = framesAt(node, parent, scopes);
    for (const [key, value] of Object.entries(node)) {
        if (key !== 'markers') {
            const [below, reads] = descend(node, parent, key, within, reading);
            // A naming position reads against the tree parent, so the padding between is stepped over.
            treesIn(value).forEach(child => readReferences(child, node, below, reads, onIdentifier, trail));
        }
    }
    trail.pop();
}

/** The scopes a node sits in, with its own in front where it is one. */
function framesAt(node: Tree, parent: Tree | undefined, scopes: Frames | undefined): Frames | undefined {
    return scopeKinds.has(node.kind) ? {node, parent, outer: scopes} : scopes;
}

/** The scopes the slot `key` of `node` resolves against, and what a read in it reads with. */
function descend(
    node: Tree, parent: Tree | undefined, key: string, within: Frames | undefined, reading: Reading
): [Frames | undefined, Reading] {
    return [reachOf(node, parent, key, within), meaningOf(node, key, reading)];
}

/**
 * The scopes the slot `key` of `node` resolves against, `within` being those `node` sits in. A
 * conditional type's `infer` names reach its extends clause and true branch alone, so its check
 * type and false branch read past them.
 */
function reachOf(node: Tree, parent: Tree | undefined, key: string, within: Frames | undefined): Frames | undefined {
    const pastInferred =
        node.kind === JS.Kind.ConditionalType && key === 'checkType' ||
        node.kind === J.Kind.Ternary && parent?.kind === JS.Kind.ConditionalType && key === 'falsePart';
    return pastInferred ? within?.outer : within;
}

/**
 * What a read in slot `key` of `node` reads with, given what `node` itself reads with. Undefined
 * is a slot that names rather than reads, and so is everything under it. A whole import declares
 * what it spells, and a re-export's clause names another module's members.
 */
function meaningOf(node: Tree, key: string, reading: Reading): Reading {
    // A computed key and a binding's default value read from within a naming slot.
    if (node.kind === JS.Kind.ComputedPropertyName || node.kind === JS.Kind.BindingElement && key === 'initializer') {
        return 'value';
    }
    if (reading === undefined) {
        return undefined;
    }
    if (reading === 'write') {
        return carriesWrite(node, key) ? 'write' : meaningOf(node, key, 'value');
    }
    if (key === 'typeParameters' || key === 'typeArguments') {
        return 'type';
    }
    if (key === 'label' || key === namingSlots[node.kind] || key === 'name' && !readsItsName(node)) {
        return undefined;
    }
    if (writes(node, key)) {
        return 'write';
    }
    switch (node.kind) {
        case JS.Kind.Import:
            // `import a = NS.b` reads `NS` as whatever it is. The rest of an import declares.
            return key === 'initializer' ? 'any' : undefined;
        case JS.Kind.ExportDeclaration:
            if (key !== 'exportClause') {
                return reading;
            }
            return (node as JS.ExportDeclaration).moduleSpecifier === undefined ? 'any' : undefined;
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

/** Whether slot `key` of `node` is one {@link isWrite} counts as assigned to. A declaring loop head names instead. */
function writes(node: Tree, key: string): boolean {
    switch (node.kind) {
        case J.Kind.Assignment:
        case J.Kind.AssignmentOperation:
        case JS.Kind.AssignmentOperation:
            return key === 'variable';
        case J.Kind.Unary:
            return key === 'expression' && incrementing.has((node as J.Unary).operator.element);
        case J.Kind.ForEachLoopControl:
            return key === 'variable';
        default:
            return false;
    }
}

const incrementing = new Set<J.Unary.Type>([
    J.Unary.Type.PreIncrement, J.Unary.Type.PreDecrement, J.Unary.Type.PostIncrement, J.Unary.Type.PostDecrement
]);

/**
 * Whether slot `key` of `node` passes a write on to what it holds, which parentheses, a type
 * assertion and the shapes of a destructuring target do. Anything else under a target reads, as
 * `x` does in `x.y = 1`.
 */
function carriesWrite(node: Tree, key: string): boolean {
    switch (node.kind) {
        case J.Kind.Parentheses:
            return key === 'tree';
        case JS.Kind.As:
            return key === 'left';
        case JS.Kind.ExpressionStatement:
        case JS.Kind.Spread:
        case JS.Kind.SatisfiesExpression:
        case J.Kind.TypeCast:
            return key === 'expression';
        case J.Kind.NewArray:
            return key === 'initializer';
        case J.Kind.NewClass:
            return key === 'body';
        case J.Kind.Block:
            return key === 'statements';
        case JS.Kind.PropertyAssignment:
            return key === (isShorthand(node) ? 'name' : 'initializer');
        case JS.Kind.ArrayBindingPattern:
            return key === 'elements';
        case JS.Kind.ObjectBindingPattern:
            return key === 'bindings';
        case JS.Kind.BindingElement:
            return key === 'name';
        default:
            return false;
    }
}

/**
 * Whether a node's `name` slot reads rather than names. A call names a member of whatever it
 * selects from, so with nothing selected its name is the function called, and a shorthand `{x}`
 * reads the `x` it also names.
 */
function readsItsName(node: Tree): boolean {
    return node.kind === J.Kind.MethodInvocation && !(node as J.MethodInvocation).select || isShorthand(node);
}

/** A shorthand property `{x}` or `{x = 1}`, which reads the `x` it names. */
function isShorthand(node: Tree | undefined): boolean {
    return node?.kind === JS.Kind.PropertyAssignment &&
        (node as JS.PropertyAssignment).assigmentToken === JS.PropertyAssignment.Token.Equals;
}

/** The slot, beside `name` and `label`, in which a node names rather than reads. */
const namingSlots: Record<string, string> = {
    [JS.Kind.Alias]: 'alias',
    [JS.Kind.BindingElement]: 'propertyName',
    [JS.Kind.ImportType]: 'qualifier',
    [JS.Kind.JsxAttribute]: 'key'
};

/**
 * Every name a binding pattern introduces. `member` is the property a name takes its value from,
 * which only a name an object pattern binds directly has: anything deeper reads a property of a
 * property, an array element is chosen by position, and a rest name gathers what nothing claimed.
 */
export function bindingNames(pattern: J | undefined): { name: string; member?: string }[] {
    return patternBindings(pattern).map(({identifier, member}) =>
        member === undefined ? {name: identifier.simpleName} : {name: identifier.simpleName, member});
}

/** As {@link bindingNames}, with the identifier each name is read from. */
function patternBindings(pattern: J | undefined): { identifier: J.Identifier; member?: string }[] {
    switch (pattern?.kind) {
        case J.Kind.Identifier: {
            const identifier = pattern as J.Identifier;
            return identifier.simpleName ? [{identifier}] : [];
        }
        case JS.Kind.Spread:
            return patternBindings((pattern as JS.Spread).expression);
        case JS.Kind.ArrayBindingPattern:
            return unnamedMembers((pattern as JS.ArrayBindingPattern).elements.elements
                .flatMap(element => patternBindings(unwrap(element))));
        case JS.Kind.ObjectBindingPattern:
            return (pattern as JS.ObjectBindingPattern).bindings.elements
                .flatMap(element => patternBindings(unwrap(element)));
        case JS.Kind.BindingElement: {
            const element = pattern as JS.BindingElement;
            if (element.name?.kind !== J.Kind.Identifier) {
                return unnamedMembers(patternBindings(element.name as J));
            }
            const identifier = element.name as J.Identifier;
            const propertyName = unwrap(element.propertyName);
            return [{
                identifier,
                member: propertyName?.kind === J.Kind.Identifier
                    ? (propertyName as J.Identifier).simpleName
                    : identifier.simpleName
            }];
        }
        default:
            return [];
    }
}

function boundIdentifiers(pattern: J | undefined): J.Identifier[] {
    return patternBindings(pattern).map(bound => bound.identifier);
}

function unnamedMembers(bound: { identifier: J.Identifier }[]): { identifier: J.Identifier }[] {
    return bound.map(({identifier}) => ({identifier}));
}

/**
 * The block of members a class, object literal or type literal holds. A member is reached through
 * the value or type holding it, so its name binds nowhere.
 */
function membersOf(node: Tree | undefined): J.Block | undefined {
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
        if (binds(bindingsAt(c), name, meaning)) {
            return c.value;
        }
    }
    return undefined;
}

/** What the node a cursor stands on binds, which is nothing where it stands on padding or the root. */
function bindingsAt(cursor: Cursor): Bindings {
    const parent = cursor.parent?.value;
    return isTree(cursor.value) ? frameBindings(cursor.value, isTree(parent) ? parent : undefined) : noBindings;
}

/**
 * What one node on the cursor path binds, `parent` being the node it hangs from. Only these two
 * answers turn on the parent; a node alone settles the rest, which is what makes them cacheable.
 */
function frameBindings(node: Tree, parent: Tree | undefined): Bindings {
    if (node.kind === J.Kind.Block && membersOf(parent) === node) {
        // An enum's members are in scope in its body, where an initializer may name an earlier one.
        return isEnum(parent) ? toBindings(enumMemberNames(node as J.Block)) : noBindings;
    }
    // An arrow binds its lambda's parameters itself, since its return type reads them.
    if (node.kind === J.Kind.Lambda && parent?.kind === JS.Kind.ArrowFunction) {
        return noBindings;
    }
    // A function expression is the only function whose own name its body reaches: a declaration's
    // name belongs to the enclosing block, a method's to an instance.
    if (node.kind === J.Kind.MethodDeclaration && parent?.kind === JS.Kind.StatementExpression) {
        let bindings = selfNamed.get(node);
        if (!bindings) {
            const self = bound(boundIdentifiers((node as J.MethodDeclaration).name), 'value');
            selfNamed.set(node, bindings = toBindings([...self, ...readBindings(node)]));
        }
        return bindings;
    }
    return ownBindings(node);
}

function ownBindings(node: Tree): Bindings {
    let bindings = frames.get(node);
    if (!bindings) {
        frames.set(node, bindings = toBindings(readBindings(node)));
    }
    return bindings;
}

function readBindings(node: Tree): Bound[] {
    switch (node.kind) {
        case JS.Kind.CompilationUnit: {
            const statements = (node as JS.CompilationUnit).statements;
            return [...declaredNames(statements), ...hoistedNames(statements)];
        }
        case J.Kind.Block:
            return blockScopedNames((node as J.Block).statements);
        case J.Kind.MethodDeclaration:
        case JS.Kind.ComputedPropertyMethodDeclaration: {
            const method = node as J.MethodDeclaration | JS.ComputedPropertyMethodDeclaration;
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
            return isGlobalAugmentation(node as JS.NamespaceDeclaration)
                ? []
                : hoistedNames((node as JS.NamespaceDeclaration).body);
        case JS.Kind.ArrowFunction:
            return [...typeParameterNames(node), ...readBindings((node as JS.ArrowFunction).lambda)];
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
        // A loop head's `var` hoists past the loop, as one anywhere in a function body does.
        case J.Kind.ForLoop:
            return blockScopedNames((node as J.ForLoop).control.init);
        case J.Kind.ForEachLoop:
            return blockScopedNames([(node as J.ForEachLoop).control.variable]);
        case JS.Kind.ForInLoop:
            return blockScopedNames([(node as JS.ForInLoop).control.variable]);
        default:
            return [];
    }
}

/**
 * The names statements bind in the block holding them — the complement of what
 * {@link hoistedNames} claims, so `function (x) { var x = 1; }` reads as one binding, not two.
 */
function blockScopedNames(statements: readonly unknown[]): Bound[] {
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

function isBlockScoped(declaration: Tree): boolean {
    return ((declaration as J.VariableDeclarations).modifiers ?? []).some(m => blockScoped.has(m.keyword!));
}

/** The names statements declare directly in the scope holding them. */
function declaredNames(statements: readonly unknown[]): Bound[] {
    return statements.flatMap(statement => declarationNames(unwrap(statement)));
}

/** The names a declaration binds, each with the meanings it has there. */
function declarationNames(statement: Tree | undefined): Bound[] {
    switch (statement?.kind) {
        case JS.Kind.Import:
            return importNames(statement as JS.Import);
        case J.Kind.VariableDeclarations:
            return bound((statement as J.VariableDeclarations).variables.flatMap(variable => {
                const named = unwrap(variable) as J.VariableDeclarations.NamedVariable | undefined;
                return boundIdentifiers(named?.name);
            }), 'value');
        case JS.Kind.ScopedVariableDeclarations:
            return declaredNames((statement as JS.ScopedVariableDeclarations).variables);
        case J.Kind.MethodDeclaration:
            return bound(boundIdentifiers((statement as J.MethodDeclaration).name), 'value');
        case J.Kind.ClassDeclaration: {
            const declaration = statement as J.ClassDeclaration;
            switch (declaration.classKind.type) {
                case J.ClassDeclaration.Kind.Type.Interface:
                    return bound(boundIdentifiers(declaration.name), 'type');
                case J.ClassDeclaration.Kind.Type.Enum:
                    // An enum's members are read as `E.A` in a type, so it is a namespace too.
                    return bound(boundIdentifiers(declaration.name), 'value', 'type', 'namespace');
                default:
                    return bound(boundIdentifiers(declaration.name), 'value', 'type');
            }
        }
        case JS.Kind.NamespaceDeclaration: {
            const declaration = statement as JS.NamespaceDeclaration;
            if (isGlobalAugmentation(declaration)) {
                return [];
            }
            const names = boundIdentifiers(rootName(unwrap(declaration.name)));
            return isInstantiated(declaration) ? bound(names, 'namespace', 'value') : bound(names, 'namespace');
        }
        case JS.Kind.TypeDeclaration:
            return bound(boundIdentifiers(unwrap((statement as JS.TypeDeclaration).name)), 'type');
        case J.Kind.TypeParameter:
            // A type parameter binds across the declaration carrying it, not in the scope that one
            // sits in, so no statement list leads here.
            return bound(boundIdentifiers((statement as J.TypeParameter).name), 'type');
        case JS.Kind.MappedTypeParameter:
            return bound(boundIdentifiers((statement as JS.MappedType.Parameter).name as J), 'type');
        case J.Kind.Case:
            // The cases of a switch share the block it opens, so each one's declarations bind in all.
            return declaredNames((statement as J.Case).statements.elements);
        default:
            return [];
    }
}

/**
 * Whether a namespace binds a value, which it does once its body holds something other than
 * interfaces, type aliases, unexported imports and namespaces that do not. That is the binder's
 * rule, under which a `const enum` counts, whatever the emitter later drops.
 */
function isInstantiated(declaration: JS.NamespaceDeclaration): boolean {
    return (declaration.body?.statements ?? []).some(statement => {
        const element = unwrap(statement);
        switch (element?.kind) {
            case JS.Kind.TypeDeclaration:
                return false;
            case J.Kind.ClassDeclaration:
                return (element as J.ClassDeclaration).classKind.type !== J.ClassDeclaration.Kind.Type.Interface;
            case JS.Kind.Import:
                return (element as JS.Import).modifiers.some(modifier => modifier.keyword === 'export');
            case JS.Kind.NamespaceDeclaration:
                return isInstantiated(element as JS.NamespaceDeclaration);
            default:
                return true;
        }
    });
}

/** `declare global { … }`, whose declarations the whole file sees, as if written at its top. */
function isGlobalAugmentation(declaration: JS.NamespaceDeclaration): boolean {
    const name = unwrap(declaration.name);
    return declaration.keywordType.element === JS.NamespaceDeclaration.KeywordType.Empty &&
        name?.kind === J.Kind.Identifier && (name as J.Identifier).simpleName === 'global';
}

function isEnum(node: Tree | undefined): boolean {
    return node?.kind === J.Kind.ClassDeclaration &&
        (node as J.ClassDeclaration).classKind.type === J.ClassDeclaration.Kind.Type.Enum;
}

function enumMemberNames(body: J.Block): Bound[] {
    return body.statements.flatMap(statement => {
        const element = unwrap(statement);
        return element?.kind === J.Kind.EnumValueSet
            ? bound((element as J.EnumValueSet).enums.map(value => value.element.name), 'value')
            : [];
    });
}

/** The identifier a dotted name `A.B.C` declares, which is `A`: the rest are members of it. */
function rootName(name: J | undefined): J | undefined {
    return name?.kind === J.Kind.FieldAccess ? rootName((name as J.FieldAccess).target) : name;
}

/**
 * The names a declaration's type parameters bind. A class keeps them in a container and everything
 * else in a `J.TypeParameters`, and both hold the same right-padded list.
 */
function typeParameterNames(node: Tree): Bound[] {
    const held = (node as { typeParameters?: J.TypeParameters | J.Container<J.TypeParameter> }).typeParameters;
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
    const names = boundIdentifiers(unwrap(importClause.name));
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

function aliasedName(specifier: J | undefined): J.Identifier[] {
    const name = specifier?.kind === JS.Kind.Alias ? (specifier as JS.Alias).alias : specifier;
    return boundIdentifiers(name);
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
const slots = new WeakMap<Tree, Map<UUID, string>>();
const frames = new WeakMap<object, Bindings>();
const selfNamed = new WeakMap<object, Bindings>();

/**
 * The names blocks under `scope` hoist out to it. A `var` or function declaration reaches the whole
 * function it sits in, so one nested in a block is in scope outside that block; only a `let`, `const`
 * or `using` keyword says otherwise.
 */
function hoistedNames(scope: unknown): Bound[] {
    if (typeof scope !== 'object' || scope === null) {
        return [];
    }
    const cached = hoisted.get(scope);
    if (cached) {
        return cached;
    }

    const names: Bound[] = [];
    const collect = (node: Tree): boolean => {
        switch (node.kind) {
            case J.Kind.MethodDeclaration:
                names.push(...declarationNames(node));
                return false;
            case JS.Kind.ComputedPropertyMethodDeclaration:
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
            case JS.Kind.NamespaceDeclaration:
                // A namespace keeps its hoisting to itself, a global augmentation gives it to the file.
                return isGlobalAugmentation(node as JS.NamespaceDeclaration);
            case J.Kind.Lambda:
            case J.Kind.ClassDeclaration:
            case J.Kind.NewClass:
            case JS.Kind.TypeLiteral:
                // Nothing in an expression or a type hoists past it.
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
export function walk(node: unknown, visit: (node: Tree) => boolean): void {
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

/** The tree a padding wrapper holds, or the node itself where it is a tree. */
function unwrap(node: unknown): J | undefined {
    const kind = (node as { kind?: string } | undefined)?.kind;
    if (kind === J.Kind.RightPadded || kind === J.Kind.LeftPadded) {
        return unwrap((node as { element?: unknown }).element);
    }
    return isTree(node) ? node as J : undefined;
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
 * Whether the identifier may be renamed in place, which is so where its slot reads rather than
 * names. A shorthand property `{x}` answers false though it reads `x`, since a rename has to
 * expand it to `{x: y}`. Position is all this reads, so a type position reads alike to a value,
 * and what `x` reads is {@link resolve}'s question.
 */
export function isReference(cursor: Cursor, identifier: J.Identifier): boolean {
    const path = pathTo(cursor, identifier);
    return !isShorthand(path[path.length - 1]) && position(path, identifier)[1] !== undefined;
}

/**
 * The tree nodes from the nearest compilation unit on `cursor`, or its outermost tree, down to the
 * one holding `identifier`, which is the cursor's own node or that node's parent.
 */
function pathTo(cursor: Cursor, identifier: J.Identifier): Tree[] {
    const path = treePath(cursor);
    if (path[path.length - 1] === identifier) {
        path.pop();
    }
    return path;
}

/** The cursors on `cursor`'s chain holding tree nodes, down from the nearest compilation unit or outermost tree. */
function treeCursors(cursor: Cursor): Cursor[] {
    const cursors: Cursor[] = [];
    for (let c: Cursor | undefined = cursor; c; c = c.parent) {
        if (isTree(c.value)) {
            cursors.unshift(c);
        }
        if (c.value?.kind === JS.Kind.CompilationUnit) {
            break;
        }
    }
    return cursors;
}

function treePath(cursor: Cursor): Tree[] {
    return treeCursors(cursor).map(c => c.value as Tree);
}

/**
 * The scopes around `leaf` and what it reads with, folded down `path`, which ends at the node
 * holding `leaf`, and what each node of `path` reads with. The fold runs to the end since a naming
 * slot can hold a read deeper down.
 */
function position(path: Tree[], leaf: Tree): [Frames | undefined, Reading, Reading[]] {
    let scopes: Frames | undefined;
    let reading: Reading = 'value';
    const readings: Reading[] = [];
    for (let i = 0; i < path.length; i++) {
        readings.push(reading);
        const child = path[i + 1] ?? leaf;
        const key = slotOf(path[i], child);
        if (key === undefined) {
            const spelled = (child as Partial<J.Identifier>).simpleName ?? child.kind;
            throw new Error(`\`${spelled}\` is not under the cursor it is resolved from`);
        }
        [scopes, reading] = descend(path[i], path[i - 1], key, framesAt(path[i], path[i - 1], scopes), reading);
    }
    return [scopes, reading, readings];
}

/**
 * The slot of `node` holding `child`, by id, so a copy or a rebuilt node is found as the original.
 * A node's slots are indexed once, through padding, containers and lists.
 */
function slotOf(node: Tree, child: Tree): string | undefined {
    let index = slots.get(node);
    if (!index) {
        index = new Map();
        for (const [key, value] of Object.entries(node)) {
            if (key !== 'markers') {
                for (const held of treesIn(value)) {
                    index.set(held.id, key);
                }
            }
        }
        slots.set(node, index);
    }
    return index.get(child.id);
}

/** The tree nodes a slot holds directly, through padding, containers and lists. */
function treesIn(slot: unknown): Tree[] {
    if (Array.isArray(slot)) {
        return slot.flatMap(treesIn);
    }
    const kind = (slot as { kind?: string } | undefined)?.kind;
    if (kind === J.Kind.RightPadded || kind === J.Kind.LeftPadded) {
        return treesIn((slot as J.RightPadded<any>).element);
    }
    if (kind === J.Kind.Container) {
        return treesIn((slot as J.Container<any>).elements);
    }
    return isTree(slot) ? [slot] : [];
}

/**
 * @deprecated Use {@link isReference}. Unlike this function once did, it answers false for an
 * import's own name, an alias and a re-export's specifier, which name rather than read.
 */
export const isValueReference = isReference;

/**
 * The node owning the scope that binds what `identifier` reads, or undefined where it reads a
 * global or reads nothing. Position is what resolves it, so the cursor stands on the identifier
 * or on the node holding it, and a node the cursor does not hold is refused.
 */
export function resolve(cursor: Cursor, identifier: J.Identifier): J | undefined {
    const [scopes, reading] = position(pathTo(cursor, identifier), identifier);
    return reading && (bindingFrame(scopes, identifier.simpleName, reading)?.node as J | undefined);
}

/**
 * Whether the identifier is assigned to where it stands. An assignment's target, plain or compound,
 * is, and so are the operand of `++` or `--`, the head of a for-in or for-of declaring nothing, and
 * a name in such a target's destructuring pattern. A write resolves as a value read, since
 * `x = 2` assigns to the `x` it reads. The cursor is as {@link resolve} takes it.
 */
export function isWrite(cursor: Cursor, identifier: J.Identifier): boolean {
    return position(pathTo(cursor, identifier), identifier)[1] === 'write';
}

/** One identifier referring to a binding, from {@link referencesOf}. */
export interface Reference {
    /** The identifier's position, holding the tree nodes above it with no padding between them. */
    cursor: Cursor;
    identifier: J.Identifier;
    /** Whether the identifier is one declaring the binding. */
    declares: boolean;
    /** Whether the identifier is assigned to, as {@link isWrite} answers. */
    writes: boolean;
}

/**
 * Every identifier referring to the binding `name` has at `cursor`, the ones declaring it and the
 * ones resolving to it, in the order the tree holds them, which a type annotation can depart from.
 * The binding is the innermost one of the name with `meaning`, or with any meaning where none is
 * asked for. A read with any meaning, as an export clause reads, refers to every meaning it finds.
 * A name bound nowhere has no references. Each reference's cursor continues the caller's chain.
 */
export function referencesOf(cursor: Cursor, name: string, meaning?: Meaning): Reference[] {
    const cursors = treeCursors(cursor);
    const path = cursors.map(c => c.value as Tree);
    if (path.length === 0) {
        return [];
    }
    const [scopes, reading, readings] = position(path.slice(0, -1), path[path.length - 1]);
    readings.push(reading);
    const here = framesAt(path[path.length - 1], path[path.length - 2], scopes);
    const frame = bindingFrame(here, name, meaning ?? 'any');
    if (!frame) {
        return [];
    }
    const declarations = new Set(frameBindings(frame.node, frame.parent).get(name)!.declarations
        .filter(declaration => meaning === undefined || declaration.meanings.includes(meaning))
        .map(declaration => declaration.identifier.id));
    // A class binds its own name in its body too, so a frame further out declaring the same
    // identifier holds the same binding, and its references sit under that frame.
    const sharesBinding = (scope: Frames | undefined): boolean => scope !== undefined &&
        (frameBindings(scope.node, scope.parent).get(name)?.declarations ?? [])
            .some(declaration => declarations.has(declaration.identifier.id));
    let root = frame;
    for (let outer = frame.outer; outer; outer = outer.outer) {
        if (sharesBinding(outer)) {
            root = outer;
        }
    }
    const at = path.indexOf(root.node);
    const references: Reference[] = [];
    readReferences(root.node, root.parent, root.outer, readings[at], (identifier, reads, scopes, trail) => {
        if (identifier.simpleName !== name) {
            return;
        }
        const declares = declarations.has(identifier.id);
        const refers = !declares && reads !== undefined &&
            (meaning === undefined || reads === 'any' || meaningRead(reads) === meaning) &&
            sharesBinding(bindingFrame(scopes, name, reads));
        if (declares || refers) {
            const standing = trail.reduce((above, node) => new Cursor(node, above), cursors[at].parent);
            references.push({cursor: standing!, identifier, declares, writes: reads === 'write'});
        }
    });
    return references;
}
