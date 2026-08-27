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
import {randomId} from "../uuid";
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
 * Splits a leading prefix, mirroring `splitTrailingComments`: a comment documents the entry it
 * precedes and is dropped along with it, while plain whitespace only ever positioned that entry
 * after the opening bracket or a comma, so it travels to whichever entry takes its place.
 */
function splitLeadingComments(prefix: J.Space): {comments: J.Space, whitespace: J.Space} {
    return prefix.comments.length === 0 ?
        {comments: {...prefix, whitespace: ""}, whitespace: space(prefix.whitespace)} :
        {comments: prefix, whitespace: emptySpace};
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
        const leading = splitLeadingComments(slot.prefixOf(removed.element));
        const survivor = remaining[0];
        remaining[0] = {
            ...survivor,
            element: slot.withPrefix(survivor.element, {...slot.prefixOf(survivor.element), whitespace: leading.whitespace.whitespace})
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
 * A bare arrow parameter has no closing delimiter of its own — the space before `=>` sits on
 * the parameter itself. Wrapping it in parens turns that into the space before the new `)`,
 * so it has to move to `arrow` instead, where it will actually print before `=>` again.
 */
function parenthesize(lambda: J.Lambda, elements: J.RightPadded<J>[]): J.Lambda {
    const last = elements[elements.length - 1];
    const arrowSpace = last.after.whitespace === "" ? lambda.arrow : last.after;
    return {
        ...lambda,
        parameters: {...lambda.parameters, parenthesized: true, parameters: [...elements.slice(0, -1), {...last, after: emptySpace}]},
        arrow: arrowSpace
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
