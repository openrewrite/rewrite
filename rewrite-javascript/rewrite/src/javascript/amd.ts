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
import {emptyMarkers, findMarker} from "../markers";
import {randomId, UUID} from "../uuid";
import {
    emptyContainer,
    emptySpace,
    Expression,
    isIdentifier,
    isLiteral,
    J,
    rightPadded,
    space,
    spaceContainsNewline,
    Statement,
    TrailingComma
} from "../java";
import {JS} from "./tree";
import {cursorOf, deconflict, namesDeclaredWithin, scopeOf} from "./scope";
import {Cursor} from "../tree";
import {JavaScriptVisitor} from "./visitor";
import {ExecutionContext} from "../execution";

/** RequireJS and Dojo write `define`; UI5 namespaces it as `sap.ui.define`. */
export const DEFAULT_AMD_CALLEES: readonly string[] = ["define", "require"];

export interface AmdBlock {
    /** -1 where the block is written without one, as `define(factory)`. */
    readonly dependenciesIndex: number;
    readonly dependencies: J.NewArray;
    readonly factoryIndex: number;
    readonly factory: J.MethodDeclaration | J.Lambda;
}

export function amdBlockOf(
    call: J.MethodInvocation,
    callees: readonly string[] = DEFAULT_AMD_CALLEES
): AmdBlock | undefined {
    if (!isAmdCallee(call, callees)) {
        return undefined;
    }
    const args = call.arguments.elements;
    const dependenciesIndex = args.findIndex(arg => arg.element.kind === J.Kind.NewArray);
    const factoryIndex = dependenciesIndex >= 0 ?
        dependenciesIndex + 1 :
        args.findIndex(arg => asFactory(arg.element) !== undefined);
    const factory = factoryIndex >= 0 && factoryIndex < args.length ?
        asFactory(args[factoryIndex].element) :
        undefined;
    if (factory === undefined) {
        return undefined;
    }
    const dependencies = dependenciesIndex >= 0 ?
        args[dependenciesIndex].element as J.NewArray :
        noDependencies();
    return {dependenciesIndex, dependencies, factoryIndex, factory};
}

/** Stands in for the array a block written without one would have had. */
function noDependencies(): J.NewArray {
    return {
        kind: J.Kind.NewArray,
        id: randomId(),
        prefix: emptySpace,
        markers: emptyMarkers,
        dimensions: [],
        initializer: emptyContainer<Expression>()
    };
}

/**
 * A callee written without a dot (`"define"`) matches by simple name on any receiver, so it
 * also matches `foo.define(...)`; a dotted callee (`"sap.ui.define"`) requires the whole path.
 */
function isAmdCallee(call: J.MethodInvocation, callees: readonly string[]): boolean {
    // The name check first: this runs for every call in a file, and almost none are these.
    const simpleName = call.name.simpleName;
    return callees.some(callee => {
        const last = callee.substring(callee.lastIndexOf(".") + 1);
        return simpleName === last && (callee === last || namespaceOf(call) === callee);
    });
}

/** The dotted path a call is written under, `sap.ui.define` for `sap.ui.define(…)`. */
function namespaceOf(call: J.MethodInvocation): string | undefined {
    const segments: string[] = [call.name.simpleName];
    let node: J | undefined = call.select?.element;
    while (node !== undefined) {
        if (node.kind === J.Kind.FieldAccess) {
            const access = node as J.FieldAccess;
            segments.unshift(access.name.element.simpleName);
            node = access.target;
        } else if (isIdentifier(node)) {
            segments.unshift(node.simpleName);
            return segments.join(".");
        } else {
            return undefined;
        }
    }
    return segments.join(".");
}

/** A factory is written either as a function expression or as an arrow function. */
function asFactory(argument: J): J.MethodDeclaration | J.Lambda | undefined {
    if (argument.kind === JS.Kind.StatementExpression) {
        const statement = (argument as JS.StatementExpression).statement;
        return statement.kind === J.Kind.MethodDeclaration ? statement as J.MethodDeclaration : undefined;
    }
    return argument.kind === JS.Kind.ArrowFunction ? (argument as JS.ArrowFunction).lambda : undefined;
}

/**
 * The elements of a `[]` or `()`, which the parser fills with a single `J.Empty` when
 * there are none.
 */
export function present<T extends J>(elements: readonly J.RightPadded<T>[]): J.RightPadded<T>[] {
    return elements.length === 1 && elements[0].element.kind === J.Kind.Empty ? [] : [...elements];
}

export function elementsOf(block: AmdBlock): J.RightPadded<Expression>[] {
    return present(block.dependencies.initializer?.elements ?? []);
}

export function parametersOf(block: AmdBlock): J.RightPadded<J>[] {
    return present(block.factory.kind === J.Kind.MethodDeclaration ?
        (block.factory as J.MethodDeclaration).parameters.elements :
        normalizeArrowParameters((block.factory as J.Lambda).parameters.parameters));
}

/**
 * An arrow function's parameter list isn't built the way every other comma-separated list in
 * the tree is: a trailing comma is an extra `J.Empty` entry rather than a `TrailingComma`
 * marker, and a parameter's own trailing whitespace sits on the identifier's declaration
 * rather than on the list entry that holds it. Folding both into the shape the rest of this
 * module already handles lets append/remove treat every kind of factory alike.
 */
function normalizeArrowParameters(raw: readonly J.RightPadded<J>[]): J.RightPadded<J>[] {
    if (raw.length === 1 && raw[0].element.kind === J.Kind.Empty) {
        return [...raw];
    }
    const trailingEmpty = raw.length > 1 && raw[raw.length - 1].element.kind === J.Kind.Empty ?
        raw[raw.length - 1] : undefined;
    const real = (trailingEmpty === undefined ? raw : raw.slice(0, -1)).map(foldDeclarationTrailingSpace);
    if (trailingEmpty === undefined) {
        return real;
    }
    const last = real[real.length - 1];
    const marker: TrailingComma = {kind: J.Markers.TrailingComma, id: randomId(), suffix: trailingEmpty.after};
    return [...real.slice(0, -1), {...last, markers: {...last.markers, markers: [...last.markers.markers, marker]}}];
}

/** Moves a VariableDeclarations parameter's trailing whitespace from the identifier's own declaration up to the list entry, when the entry doesn't already carry any. */
function foldDeclarationTrailingSpace(entry: J.RightPadded<J>): J.RightPadded<J> {
    if (entry.element.kind !== J.Kind.VariableDeclarations ||
        entry.after.whitespace !== "" || entry.after.comments.length > 0) {
        return entry;
    }
    const declaration = entry.element as J.VariableDeclarations;
    const variable = declaration.variables[0];
    if (variable === undefined) {
        return entry;
    }
    const folded: J.VariableDeclarations = {
        ...declaration,
        variables: [{...variable, after: emptySpace}, ...declaration.variables.slice(1)]
    };
    return {...entry, element: folded, after: variable.after};
}

export function dependencyNames(block: AmdBlock): string[] {
    return elementsOf(block).map(padded => {
        const element = padded.element;
        return isLiteral(element) && typeof element.value === "string" ? element.value : "";
    });
}

export function parameterNames(block: AmdBlock): (string | undefined)[] {
    return parametersOf(block).map(padded => identifierOf(padded.element)?.simpleName);
}

export function identifierOf(parameter: J): J.Identifier | undefined {
    if (parameter.kind === J.Kind.VariableDeclarations) {
        const name = (parameter as J.VariableDeclarations).variables[0]?.element.name;
        return name !== undefined && isIdentifier(name) ? name : undefined;
    }
    return isIdentifier(parameter) ? parameter : undefined;
}

/**
 * Where an entry's leading whitespace sits. A dependency string carries it directly, while
 * a factory parameter carries it on the identifier inside the declaration that wraps it.
 */
interface Slot<T extends J> {
    prefixOf(element: T): J.Space;

    withPrefix(element: T, prefix: J.Space): T;
}

const dependencySlot: Slot<Expression> = {
    prefixOf: element => element.prefix,
    withPrefix: (element, prefix) => ({...element, prefix})
};

const parameterSlot: Slot<J> = {
    prefixOf: element => identifierOf(element)?.prefix ?? element.prefix,
    withPrefix: (element, prefix) => {
        if (element.kind !== J.Kind.VariableDeclarations) {
            return {...element, prefix};
        }
        const declaration = element as J.VariableDeclarations;
        const variable = declaration.variables[0];
        if (variable === undefined) {
            return element;
        }
        return {
            ...declaration,
            variables: [
                {...variable, element: {...variable.element, name: {...variable.element.name, prefix}}},
                ...declaration.variables.slice(1)
            ]
        };
    }
};

/**
 * A trailing comma is a marker on the entry it follows, so it has to travel to whichever
 * entry ends up last. Left where it was, it prints as a second comma and the array gains a
 * hole, which shifts every dependency after it out of step with its parameter.
 */
function moveTrailingComma<T extends J>(
    from: J.RightPadded<T>,
    to: J.RightPadded<T>
): {from: J.RightPadded<T>, to: J.RightPadded<T>} {
    const trailing = findMarker<TrailingComma>(from, J.Markers.TrailingComma);
    if (trailing === undefined) {
        return {from, to};
    }
    return {
        from: {...from, markers: {...from.markers, markers: from.markers.markers.filter(m => m !== trailing)}},
        to: {...to, markers: {...to.markers, markers: [...to.markers.markers, trailing]}}
    };
}

/**
 * The whitespace that separates one entry from the next, taken from the entries already
 * there so that a block listing its dependencies one per line keeps doing so.
 */
function separator<T extends J>(entries: readonly J.RightPadded<T>[], slot: Slot<T>): J.Space {
    if (entries.length >= 2) {
        return slot.prefixOf(entries[1].element);
    }
    const first = slot.prefixOf(entries[0].element);
    return spaceContainsNewline(first) ? first : space(" ");
}

/**
 * Appends `element`. The trailing entry's padding holds the whitespace before the closing
 * bracket, so it has to move along with the position.
 */
function appendEntry<T extends J>(
    entries: readonly J.RightPadded<T>[],
    element: T,
    slot: Slot<T>
): J.RightPadded<T>[] {
    const last = entries[entries.length - 1];
    if (last === undefined) {
        return [rightPadded<T>(element, emptySpace)];
    }
    const positioned = slot.withPrefix(element, separator(entries, slot));
    const trailing = splitTrailingComments(last.after);
    const moved = moveTrailingComma({...last, after: trailing.comments}, rightPadded<T>(positioned, trailing.whitespace));
    return [...entries.slice(0, -1), moved.from, moved.to];
}

/**
 * Splits an entry's trailing padding when it stops being last. Plain whitespace only ever
 * positioned the closing delimiter, so it travels to whichever entry becomes the new last. A
 * comment documents the entry it follows and keeps its own trailing newline, so it stays put
 * whole — a line comment with nothing left after it would otherwise swallow the comma the
 * printer inserts right after this padding.
 */
function splitTrailingComments(after: J.Space): {comments: J.Space, whitespace: J.Space} {
    return after.comments.length === 0 ?
        {comments: {...after, whitespace: ""}, whitespace: space(after.whitespace)} :
        {comments: after, whitespace: emptySpace};
}

/**
 * The plain-whitespace part of a leading prefix whose entry is about to be removed. A comment in
 * that prefix documents the entry it precedes and is dropped along with it; the whitespace only
 * ever positioned that entry after the opening bracket or a comma, so it's what travels to
 * whichever entry takes its place.
 */
function leadingWhitespaceOf(prefix: J.Space): string {
    return prefix.comments.length === 0 ? prefix.whitespace : "";
}

/**
 * Removes the entry at `index`. The first entry carries no separating whitespace and the
 * last carries the whitespace before the closing bracket, so removing either hands its
 * padding to whichever entry takes its place.
 */
function removeEntry<T extends J>(
    entries: readonly J.RightPadded<T>[],
    index: number,
    slot: Slot<T>
): J.RightPadded<T>[] {
    if (index < 0 || index >= entries.length) {
        return [...entries];
    }
    const remaining = [...entries];
    const [removed] = remaining.splice(index, 1);
    if (remaining.length === 0) {
        return remaining;
    }
    if (index === 0) {
        // Spread the survivor's own prefix rather than replacing it outright, so a comment it
        // already carries survives; only its plain whitespace is replaced.
        const whitespace = leadingWhitespaceOf(slot.prefixOf(removed.element));
        const survivor = remaining[0];
        remaining[0] = {
            ...survivor,
            element: slot.withPrefix(survivor.element, {...slot.prefixOf(survivor.element), whitespace})
        };
    } else if (index === remaining.length) {
        const last = {...remaining[index - 1], after: removed.after};
        remaining[index - 1] = moveTrailingComma(removed, last).to;
    }
    return remaining;
}

function emptyExpression(): J.Empty {
    return {kind: J.Kind.Empty, id: randomId(), prefix: emptySpace, markers: emptyMarkers};
}

function withElements(dependencies: J.NewArray, elements: J.RightPadded<Expression>[]): J.NewArray {
    const initializer = dependencies.initializer!;
    return {
        ...dependencies,
        initializer: {
            ...initializer,
            elements: elements.length === 0 ? [rightPadded<Expression>(emptyExpression(), emptySpace)] : elements
        }
    };
}

function withParameters(
    factory: J.MethodDeclaration | J.Lambda,
    parameters: J.RightPadded<J>[]
): J.MethodDeclaration | J.Lambda {
    const elements = parameters.length === 0 ? [rightPadded<J>(emptyExpression(), emptySpace)] : parameters;
    if (factory.kind === J.Kind.MethodDeclaration) {
        const method = factory as J.MethodDeclaration;
        return {...method, parameters: {...method.parameters, elements: elements as J.RightPadded<Statement>[]}};
    }
    const lambda = factory as J.Lambda;
    // Unparenthesized arrow parameters are only valid JavaScript for exactly one parameter.
    if (lambda.parameters.parenthesized || parameters.length === 1) {
        return {...lambda, parameters: {...lambda.parameters, parameters: elements}};
    }
    return parenthesize(lambda, elements);
}

/**
 * A bare arrow parameter has no closing delimiter of its own — the space before `=>` is the
 * parameter's trailing padding, so parenthesizing moves its whitespace to `arrow` and leaves any
 * comment with the parameter. The padding comes from the original list, since `elements` stands
 * in for an emptied list with a placeholder that carries none.
 */
function parenthesize(lambda: J.Lambda, elements: J.RightPadded<J>[]): J.Lambda {
    const original = normalizeArrowParameters(lambda.parameters.parameters);
    const trailing = original[original.length - 1]?.after ?? lambda.arrow;
    const last = elements[elements.length - 1];
    return {
        ...lambda,
        parameters: {...lambda.parameters, parenthesized: true, parameters: [...elements.slice(0, -1), {...last, after: emptySpace}]},
        arrow: space(trailing.whitespace)
    };
}

function identifier(name: string): J.Identifier {
    return {
        kind: J.Kind.Identifier,
        id: randomId(),
        prefix: emptySpace,
        markers: emptyMarkers,
        annotations: [],
        simpleName: name,
        type: undefined,
        fieldType: undefined
    };
}

function dependencyLiteral(module: string, quote: string): J.Literal {
    return {
        kind: J.Kind.Literal,
        id: randomId(),
        prefix: emptySpace,
        markers: emptyMarkers,
        value: module,
        valueSource: `${quote}${module}${quote}`
    };
}

/** The quote the block's own dependencies use, so a new one matches them. */
function quoteOf(block: AmdBlock): string {
    for (const padded of elementsOf(block)) {
        const source = isLiteral(padded.element) ? (padded.element as J.Literal).valueSource : undefined;
        const quote = source?.charAt(0);
        if (quote === '"' || quote === "'") {
            return quote;
        }
    }
    return '"';
}

function parameterDeclaration(name: string): J.VariableDeclarations {
    return {
        kind: J.Kind.VariableDeclarations,
        id: randomId(),
        prefix: emptySpace,
        markers: emptyMarkers,
        leadingAnnotations: [],
        modifiers: [],
        variables: [rightPadded({
            kind: J.Kind.NamedVariable,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            name: identifier(name),
            dimensionsAfterName: []
        } as J.VariableDeclarations.NamedVariable, emptySpace)]
    };
}

/**
 * Adds a dependency and its binding, keeping the array and the parameter list index-aligned.
 * Refuses (returns `undefined`) unless the two are already the same length: fewer parameters
 * than dependencies would invent a binding for one an author left unbound on purpose, and more
 * would pair the new dependency with an existing surplus parameter instead of a fresh one —
 * either way corrupting the pairing this module exists to protect.
 */
export function withDependency(
    call: J.MethodInvocation,
    block: AmdBlock,
    module: string,
    binding: string
): J.MethodInvocation | undefined {
    if (parametersOf(block).length !== elementsOf(block).length) {
        return undefined;
    }
    const dependencies = withElements(
        block.dependencies,
        appendEntry(elementsOf(block), dependencyLiteral(module, quoteOf(block)), dependencySlot));
    const factory = withParameters(
        block.factory,
        appendEntry(parametersOf(block), parameterDeclaration(binding), parameterSlot));
    return withParts(call, block, dependencies, factory);
}

/**
 * Adds a dependency the factory takes no parameter for, as AMD allows for a module loaded only
 * for its side effects. Refuses where parameters outnumber dependencies, since the appended one
 * would land on a surplus parameter and bind after all — as it would in `define(factory)`, whose
 * parameters come from the loader.
 */
export function withUnboundDependency(
    call: J.MethodInvocation,
    block: AmdBlock,
    module: string
): J.MethodInvocation | undefined {
    if (parametersOf(block).length > elementsOf(block).length) {
        return undefined;
    }
    const dependencies = withElements(
        block.dependencies,
        appendEntry(elementsOf(block), dependencyLiteral(module, quoteOf(block)), dependencySlot));
    return withParts(call, block, dependencies, block.factory);
}

export function withoutDependencyAt(
    call: J.MethodInvocation,
    block: AmdBlock,
    index: number
): J.MethodInvocation {
    const dependencies = withElements(block.dependencies, removeEntry(elementsOf(block), index, dependencySlot));
    const parameters = parametersOf(block);
    const factory = index >= parameters.length ?
        block.factory :
        withParameters(block.factory, removeEntry(parameters, index, parameterSlot));
    return withParts(call, block, dependencies, factory);
}

/**
 * Swaps the module the dependency at `index` names, leaving its parameter and every other
 * entry's position untouched — simpler than a removal and a fresh append, which would shift
 * every dependency after it and re-litigate the parameter pairing this module exists to protect.
 */
export function withDependencyModuleAt(
    call: J.MethodInvocation,
    block: AmdBlock,
    index: number,
    module: string
): J.MethodInvocation {
    const quote = quoteOf(block);
    const elements = [...elementsOf(block)];
    const entry = elements[index];
    const literal = entry.element as J.Literal;
    const updated: J.Literal = {...literal, value: module, valueSource: `${quote}${module}${quote}`};
    elements[index] = {...entry, element: updated};
    const dependencies = withElements(block.dependencies, elements);
    return withParts(call, block, dependencies, block.factory);
}

function withParts(
    call: J.MethodInvocation,
    block: AmdBlock,
    dependencies: J.NewArray,
    factory: J.MethodDeclaration | J.Lambda
): J.MethodInvocation {
    const args = [...call.arguments.elements];
    let factoryIndex = block.factoryIndex;
    if (block.dependenciesIndex >= 0) {
        args[block.dependenciesIndex] = {...args[block.dependenciesIndex], element: dependencies};
    } else if (present(dependencies.initializer?.elements ?? []).length > 0) {
        // The array takes the position the factory held, so it inherits what led up to it.
        const leading = args[factoryIndex].element;
        args[factoryIndex] = {...args[factoryIndex], element: {...leading, prefix: space(" ")}};
        args.splice(factoryIndex, 0, rightPadded<Expression>({...dependencies, prefix: leading.prefix}, emptySpace));
        factoryIndex += 1;
    }
    args[factoryIndex] = {...args[factoryIndex], element: restoreFactory(args[factoryIndex].element, factory)};
    return {...call, arguments: {...call.arguments, elements: args}};
}

/** Puts the rewritten factory back into whichever wrapper the original was written as. */
function restoreFactory(original: Expression, factory: J.MethodDeclaration | J.Lambda): Expression {
    if (original.kind === JS.Kind.StatementExpression) {
        const statementExpression: JS.StatementExpression = {
            ...(original as JS.StatementExpression),
            statement: factory as J.MethodDeclaration
        };
        return statementExpression;
    }
    const arrowFunction: JS.ArrowFunction = {...(original as JS.ArrowFunction), lambda: factory as J.Lambda};
    return arrowFunction;
}

/** The subset of `MaybeBindOptions` that selects which callees introduce an AMD block. */
export interface AmdCalleeOptions {
    /** Callees that introduce an AMD block. */
    amdCallee?: string | readonly string[];
}

export function calleesOf(options?: AmdCalleeOptions): readonly string[] {
    const callee = options?.amdCallee;
    return callee === undefined ? DEFAULT_AMD_CALLEES : typeof callee === "string" ? [callee] : callee;
}

/** The nearest AMD block the cursor sits inside, which is the one a binding belongs to. */
export function enclosingAmdBlock(
    from: Cursor | JavaScriptVisitor<any>,
    options?: AmdCalleeOptions
): {call: J.MethodInvocation, block: AmdBlock} | undefined {
    const callees = calleesOf(options);
    let cursor = from instanceof Cursor ? from : cursorOf(from);
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

/** The conventional local name for a module: its last path segment. */
export function lastSegment(module: string): string {
    return module.substring(module.lastIndexOf("/") + 1);
}

// Always-reserved words, plus the strict-mode-only ones (module code is always strict) and
// `await`/`yield`, contextual elsewhere but a trap to bind regardless of where they're legal.
const RESERVED_WORDS = new Set([
    "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do",
    "else", "enum", "export", "extends", "false", "finally", "for", "function", "if", "import",
    "in", "instanceof", "new", "null", "return", "super", "switch", "this", "throw", "true", "try",
    "typeof", "var", "void", "while", "with",
    "await", "implements", "interface", "let", "package", "private", "protected", "public",
    "static", "yield"
]);

/**
 * `lastSegment(module)`, or `undefined` where that string cannot bind a name — a scoped
 * package's `@scope/my-lib`, a subpath's `lodash.merge`, a `-`/`.`-bearing name, or a reserved
 * word are all real module strings that are not legal identifiers. ASCII identifiers only —
 * stricter than JavaScript itself, which allows Unicode, but refusing there is safe.
 */
export function derivedBindingName(module: string): string | undefined {
    const segment = lastSegment(module);
    return /^[A-Za-z_$][A-Za-z0-9_$]*$/.test(segment) && !RESERVED_WORDS.has(segment) ? segment : undefined;
}

/** Queued reservations already targeting this block: the shared source for both dedup and deconfliction. */
function queuedFor(visitor: JavaScriptVisitor<any>, blockId: UUID): AddAmdDependency<any>[] {
    return visitor.afterVisit.filter((v): v is AddAmdDependency<any> => v instanceof AddAmdDependency && v.blockId === blockId);
}

/**
 * A local binding for `module` on the AMD lane, creating one where the block does not already
 * have it, or `undefined` where no safe binding exists.
 *
 * The name is decided from the cursor and returned at once; the edit that creates the binding
 * is deferred onto `visitor.afterVisit`, as `bindImport`'s is.
 */
export function bindAmd(
    visitor: JavaScriptVisitor<any>,
    amd: {call: J.MethodInvocation, block: AmdBlock},
    module: string,
    preferredName: string | undefined,
    callees: readonly string[],
    pinned: boolean = false
): string | undefined {
    const modules = dependencyNames(amd.block);
    const bindings = parameterNames(amd.block);

    const declared = module === "" ? -1 : modules.indexOf(module);
    if (declared >= 0) {
        const bound = bindings[declared];
        if (bound === undefined) {
            // A parameter that is not a plain name gives the module no binding to hand back.
            return undefined;
        }
        // Only code the factory encloses can shadow its parameter, so a cursor elsewhere in the
        // block — on the `define` call, or on its dependency array — has nothing to measure.
        const cursor = cursorOf(visitor);
        const withinFactory = cursor?.firstEnclosing((v): v is J => v === amd.block.factory);
        if (withinFactory === undefined || scopeOf(cursor!).declaringScope(bound) === amd.block.factory) {
            return bound;
        }
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

    if (preferredName === undefined && derivedBindingName(module) === undefined) {
        return undefined;
    }
    // A parameter is in scope across the whole factory, and the queue hands this name to later
    // requests at cursors inside it, so every name the factory declares is one it has to clear.
    const taken = [...bindings, ...namesDeclaredWithin(amd.block.factory), ...queued.map(v => v.binding)];
    const requested = preferredName ?? derivedBindingName(module)!;
    const binding = deconflict(requested, candidate => taken.includes(candidate));
    if (pinned && binding !== requested) {
        // An alias is bound verbatim or not at all, since the caller may already have emitted
        // code naming it, and a deconflicted spelling would leave that unbound.
        return undefined;
    }
    visitor.afterVisit.push(new AddAmdDependency(amd.call.id, module, binding, callees));
    return binding;
}

/** The factory's body: a `J.Block` for `function(){}` and most arrows, or the bare expression for `() => expr`. */
export function bodyOf(block: AmdBlock): J | undefined {
    return block.factory.kind === J.Kind.MethodDeclaration ?
        (block.factory as J.MethodDeclaration).body :
        (block.factory as J.Lambda).body;
}

/**
 * Whether the factory body references `name`. This counts any identifier of that name,
 * including a member in a field access, since a stray dependency is fine where a shadowed one
 * is not. `missingBodyAnswer` covers a body-less factory: `false` for an addition (nothing to
 * conflict with), `true` for a removal (nothing to prove the binding unused).
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
 * Applies the dependency a `maybeBind` call settled on. The block is found by id: its caller
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
        // mismatch bindAmd's own check already cleared — an error here for the same reason a missing
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
 * Applies a `maybeRebind` call's decision on the AMD lane: swaps the dependency's module in
 * place, found by the block's tree id and the module it named when the caller asked.
 */
export class RebindAmdDependency<P> extends JavaScriptVisitor<P> {
    constructor(
        readonly blockId: UUID,
        readonly fromModule: string,
        readonly toModule: string,
        readonly callees: readonly string[]
    ) {
        super();
    }

    private applied = false;

    override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: P): Promise<J | undefined> {
        const visited = await super.visitJsCompilationUnit(cu, p) as JS.CompilationUnit;
        if (!this.applied) {
            throw new Error(`No AMD block ${this.blockId} to rebind '${this.fromModule}' to '${this.toModule}' on`);
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
        const index = this.fromModule === "" ? -1 : dependencyNames(block).indexOf(this.fromModule);
        if (index < 0) {
            throw new Error(
                `AMD block ${this.blockId} no longer has dependency '${this.fromModule}' to rebind to '${this.toModule}'`);
        }
        return withDependencyModuleAt(visited, block, index, this.toModule);
    }
}

/**
 * The AMD counterpart to `RemoveImport`: drops a dependency the block's body no longer names,
 * along with the parameter bound to it. A dependency the factory takes no parameter for is
 * loaded for its side effects and stays, matching `bindAmd`'s own read of such a block. A
 * `member`-scoped request is a no-op here — see `maybeUnbind`.
 */
export class RemoveAmdDependency<P> extends JavaScriptVisitor<P> {
    constructor(
        readonly module: string,
        readonly member?: string,
        readonly callees: readonly string[] = DEFAULT_AMD_CALLEES
    ) {
        super();
    }

    override async visitMethodInvocation(m: J.MethodInvocation, p: P): Promise<J | undefined> {
        const visited = await super.visitMethodInvocation(m, p) as J.MethodInvocation;
        if (this.member !== undefined) {
            return visited;
        }
        const block = amdBlockOf(visited, this.callees);
        if (block === undefined) {
            return visited;
        }
        const index = dependencyNames(block).indexOf(this.module);
        if (index < 0) {
            return visited;
        }
        const binding = parameterNames(block)[index];
        if (binding === undefined || await references(block, binding, true)) {
            return visited;
        }
        return withoutDependencyAt(visited, block, index);
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
    options?: AmdCalleeOptions
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
            // A block written `define(factory)` takes `require`, `exports` and `module` from the
            // loader, so a parameter there is positional against arguments no dependency list names.
            if (block.dependenciesIndex < 0 || parameterNames(block).length !== elementsOf(block).length) {
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
