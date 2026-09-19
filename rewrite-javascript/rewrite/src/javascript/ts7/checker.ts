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
import {
    isExpressionWithTypeArguments,
    isIdentifier,
    isModuleDeclaration,
    isQualifiedName,
    isStringLiteral,
    isTypeNode,
    type Node,
    type SourceFile,
    type TypeNode,
} from "typescript/unstable/ast";
import type {Checker, Signature, Symbol as Symbol7, Type} from "typescript/unstable/sync";

// The checker runs in the Go process and answers over IPC, which shapes three things the parser has
// to account for: a type-position identifier resolves through its enclosing type node, a symbol's
// declarations arrive as handles, and the naming and ambient-module queries have no counterpart.

/**
 * The type at `node`, resolving an identifier that names a type through the type node containing it.
 * Asking the checker about such an identifier directly yields `any`, since the name is a reference
 * to a type rather than an expression with one.
 */
export function typeAtLocation(checker: Checker, node: Node): Type | undefined {
    const typeNode = enclosingTypeNode(node);
    return typeNode ? checker.getTypeFromTypeNode(typeNode) : checker.getTypeAtLocation(node);
}

function enclosingTypeNode(node: Node): TypeNode | undefined {
    if (!isIdentifier(node) && !isQualifiedName(node)) {
        return undefined;
    }
    // A type name reaches its type node through the qualified names it is nested in, so `React` and
    // `Ref` in `React.Ref<T>` both arrive at the same TypeReference.
    let name: Node = node;
    while (name.parent && isQualifiedName(name.parent)) {
        name = name.parent;
    }
    const parent: Node | undefined = name.parent;
    return parent && (isTypeNode(parent) || isExpressionWithTypeArguments(parent))
        ? parent as TypeNode
        : undefined;
}

/**
 * A symbol's declarations, which cross the IPC boundary as handles. Each resolves against the
 * project that produced it, so callers need no project of their own.
 */
export function declarationsOf(symbol: Symbol7): Node[] {
    return symbol.declarations
        .map(handle => handle.resolve())
        .filter((node): node is Node => node !== undefined);
}

/**
 * The name a symbol declares. A class or interface exported as the default carries `default` as its
 * symbol name, and the name it was written with is only on the declaration.
 */
export function declaredNameOf(symbol: Symbol7): string {
    if (symbol.name !== "default") {
        return symbol.name;
    }
    for (const declaration of declarationsOf(symbol)) {
        const name = (declaration as {name?: {text?: string}}).name;
        if (name?.text) {
            return name.text;
        }
    }
    return symbol.name;
}

/** A symbol's value declaration, resolved from its handle. */
export function valueDeclarationOf(symbol: Symbol7): Node | undefined {
    return symbol.valueDeclaration?.resolve();
}

/** The compiler's own name for the scope a `declare global` block augments. */
const GLOBAL_SCOPE = "__global";

/** The dotted chain of symbol names from `symbol` up to the outermost symbol enclosing it. */
export function fullyQualifiedNameOf(symbol: Symbol7): string {
    const parts: string[] = [];
    let current: Symbol7 | undefined = symbol;
    while (current) {
        const name = declaredNameOf(current);
        parts.unshift(name === GLOBAL_SCOPE ? "global" : name);
        current = current.getParent();
    }
    return parts.join(".");
}

/** The `declare module "..."` blocks in `sourceFile`, which is where ambient modules are declared. */
export function ambientModulesIn(sourceFile: SourceFile): Node[] {
    return sourceFile.statements.filter(
        statement => isModuleDeclaration(statement) && isStringLiteral(statement.name));
}

/** Renders a signature as its parameter and return types, for keying a cache of mapped methods. */
export function signatureKeyOf(checker: Checker, signature: Signature): string {
    const parameters = signature.getParameters()
        .map(parameter => {
            const type = checker.getTypeOfSymbol(parameter);
            return type ? checker.typeToString(type) : "?";
        })
        .join(",");
    const returnType = checker.getReturnTypeOfSignature(signature);
    return `(${parameters}) => ${returnType ? checker.typeToString(returnType) : "?"}`;
}

/**
 * The checker with its answers remembered. Every question crosses a process boundary, and the
 * parser asks the same ones repeatedly — a symbol's type at a location four times over, a type's
 * arguments six — so the answers are held for the life of the project they were asked about.
 */
export interface MemoizingChecker extends Checker {
    /** Asks about every name in a file at once, so the questions that follow are answered locally. */
    prefetch(sourceFile: SourceFile): void;
}

export function memoizing(checker: Checker): MemoizingChecker {
    const perNode = new WeakMap<object, Map<string, unknown>>();
    const perId = new Map<string, unknown>();

    const byNode = <T>(method: string, node: object, compute: () => T): T => {
        let slots = perNode.get(node);
        if (!slots) {
            perNode.set(node, slots = new Map());
        }
        if (!slots.has(method)) {
            slots.set(method, compute());
        }
        return slots.get(method) as T;
    };
    const byId = <T>(key: string, compute: () => T): T => {
        if (!perId.has(key)) {
            perId.set(key, compute());
        }
        return perId.get(key) as T;
    };

    // A name is what the parser asks about, and asking for all of them in one request costs a single
    // round trip where asking one at a time costs thousands.
    const prefetch = (sourceFile: SourceFile): void => {
        const names: Node[] = [];
        const walk = (node: Node): void => {
            if (isIdentifier(node)) {
                names.push(node);
            }
            node.forEachChild(child => {
                walk(child);
                return undefined;
            });
        };
        walk(sourceFile as unknown as Node);
        if (names.length === 0) {
            return;
        }
        const symbols = checker.getSymbolAtLocation(names);
        const types = checker.getTypeAtLocation(names);
        names.forEach((name, i) => {
            byNode("getSymbolAtLocation", name, () => symbols[i]);
            byNode("getTypeAtLocation", name, () => types[i]);
        });
    };

    // Only the questions whose answer depends on nothing but the arguments are remembered; the rest
    // reach the checker as they are.
    return new Proxy(checker, {
        get(target, property: string, receiver) {
            if (property === "prefetch") {
                return prefetch;
            }
            const value = Reflect.get(target, property, receiver);
            if (typeof value !== "function") {
                return value;
            }
            const call = (value as (...a: unknown[]) => unknown).bind(target);
            switch (property) {
                case "getTypeAtLocation":
                case "getSymbolAtLocation":
                case "getTypeFromTypeNode":
                case "getResolvedSignature":
                    return (node: object) => Array.isArray(node)
                        ? call(node)
                        : byNode(property, node, () => call(node));
                case "getTypeOfSymbolAtLocation":
                    return (symbol: {id: number}, location: object) =>
                        byNode(`${property}:${symbol?.id}`, location, () => call(symbol, location));
                case "getDeclaredTypeOfSymbol":
                case "getAliasedSymbol":
                case "getTypeOfSymbol":
                    return (symbol: {id: number} | unknown[]) => Array.isArray(symbol)
                        ? call(symbol)
                        : byId(`${property}:${(symbol as {id: number})?.id}`, () => call(symbol));
                case "getTypeArguments":
                case "getPropertiesOfType":
                case "getBaseTypes":
                case "getReturnTypeOfSignature":
                    return (type: {id: number}) => byId(`${property}:${type?.id}`, () => call(type));
                case "getSignaturesOfType":
                    return (type: {id: number}, kind: unknown) =>
                        byId(`${property}:${type?.id}:${String(kind)}`, () => call(type, kind));
                default:
                    return call;
            }
        },
    }) as MemoizingChecker;
}
