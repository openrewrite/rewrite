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
import {emptyMarkers} from "../markers";
import {randomId} from "../uuid";
import {emptyContainer, emptySpace, Expression, isIdentifier, isLiteral, J} from "../java";
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
        (block.factory as J.Lambda).parameters.parameters);
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
