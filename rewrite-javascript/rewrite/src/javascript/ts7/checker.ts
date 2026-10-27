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
    type Declaration,
    isExpressionWithTypeArguments,
    isIdentifier,
    isModuleDeclaration,
    isQualifiedName,
    isStringLiteral,
    isTypeNode,
    isTypeReferenceNode,
    type Node,
    type SourceFile,
    type TypeNode,
} from "typescript/ast";
import type {API, APIRequestGenerator, Checker, Signature, Symbol as Symbol7, Type} from "typescript/sync";

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
    if (!typeNode) {
        return checker.getTypeAtLocation(node);
    }
    // The name a reference resolves to stands for the type it declares, `Box` in `Box<string>` for the
    // generic `Box<T>`, as TypeScript 6 attributes it. The reference itself carries the instantiation.
    if (isTypeReferenceNode(typeNode) && namesReferencedType(node, typeNode.typeName)) {
        const symbol = checker.getSymbolAtLocation(node);
        const declared = symbol && checker.getDeclaredTypeOfSymbol(symbol);
        if (declared) {
            return declared;
        }
    }
    return checker.getTypeFromTypeNode(typeNode);
}

/** Whether `node` is `typeName` or, where that is qualified, the name it ends in. */
function namesReferencedType(node: Node, typeName: Node): boolean {
    const named = isQualifiedName(typeName) ? [typeName, typeName.right] : [typeName];
    return named.some(n => n.pos === node.pos && n.end === node.end);
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
export function declarationsOf(symbol: Symbol7): Declaration[] {
    return symbol.declarations
        .map(handle => handle.resolve())
        .filter((node): node is Declaration => node !== undefined);
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
    /**
     * Asks about every name in a file at once, so the questions that follow are answered locally.
     * Returns the names.
     */
    prefetch(sourceFile: SourceFile): readonly Node[];

    /**
     * Asks every question in one round trip, and the answers are there when the questions are asked
     * again one at a time. A question that fails is not remembered, so asking it again reaches the checker.
     */
    ask(questions: readonly Question[]): void;
}

/**
 * A question to the checker, or to one of its objects such as `type.getSymbol.gen()`. An object
 * keeps the answers it was given, so only the checker's own need remembering.
 */
export type Question = readonly [method: string & keyof Checker, ...args: unknown[]] | APIRequestGenerator;

const failed = Symbol("failed");

/** Answers `failed` for a question the checker could not answer, so the rest of its batch survives. */
function* settled(question: APIRequestGenerator): APIRequestGenerator {
    try {
        return yield* question;
    } catch {
        return failed;
    }
}

export function memoizing(checker: Checker, api: Pick<API, "batch">): MemoizingChecker {
    const perNode = new WeakMap<object, Map<string, unknown>>();
    const perId = new Map<string, unknown>();
    const idOf = (o: unknown) => (o as {id?: number} | undefined)?.id;
    const withNode = (node: unknown, key: string): readonly [Map<string, unknown>, string] => {
        let slots = perNode.get(node as object);
        if (!slots) {
            perNode.set(node as object, slots = new Map());
        }
        return [slots, key];
    };

    // Only the questions whose answer depends on nothing but the arguments are remembered, keyed by
    // the method and the arguments. The answer about a node is held with the node.
    const slotOf = (method: string, args: readonly unknown[]): readonly [Map<string, unknown>, string] | undefined => {
        const [first, second] = args;
        switch (method) {
            case "getTypeAtLocation":
            case "getSymbolAtLocation":
            case "getTypeFromTypeNode":
            case "getResolvedSignature":
                return Array.isArray(first) ? undefined : withNode(first, method);
            case "getTypeOfSymbolAtLocation":
                return withNode(second, `${method}:${idOf(first)}`);
            case "getDeclaredTypeOfSymbol":
            case "getAliasedSymbol":
            case "getTypeOfSymbol":
            case "getTypeArguments":
            case "getPropertiesOfType":
            case "getBaseTypes":
            case "getReturnTypeOfSignature":
                return Array.isArray(first) ? undefined : [perId, `${method}:${idOf(first)}`];
            case "typeToString":
                return args.length === 1 ? [perId, `${method}:${idOf(first)}`] : undefined;
            case "getSignaturesOfType":
                return [perId, `${method}:${idOf(first)}:${String(second)}`];
            default:
                return undefined;
        }
    };
    const remembered = (method: string, args: readonly unknown[], answer: (...args: unknown[]) => unknown): unknown => {
        const slot = slotOf(method, args);
        if (!slot) {
            return answer(...args);
        }
        const [slots, key] = slot;
        if (!slots.has(key)) {
            slots.set(key, answer(...args));
        }
        return slots.get(key);
    };

    // A name is what the parser asks about, and asking for all of them in one request costs a single
    // round trip where asking one at a time costs thousands.
    const prefetch = (sourceFile: SourceFile): readonly Node[] => {
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
            return names;
        }
        const symbols = checker.getSymbolAtLocation(names);
        const types = checker.getTypeAtLocation(names);
        names.forEach((name, i) => {
            remembered("getSymbolAtLocation", [name], () => symbols[i]);
            remembered("getTypeAtLocation", [name], () => types[i]);
        });
        return names;
    };

    const ask = (questions: readonly Question[]): void => {
        const store: ((answer: unknown) => void)[] = [];
        const generators: APIRequestGenerator[] = [];
        const asking = new Map<Map<string, unknown>, Set<string>>();
        for (const question of questions) {
            if (!Array.isArray(question)) {
                generators.push(settled(question as APIRequestGenerator));
                store.push(() => undefined);
                continue;
            }
            const [method, ...args] = question as readonly [string, ...unknown[]];
            const slot = slotOf(method, args);
            if (!slot || slot[0].has(slot[1])) {
                continue;
            }
            let keys = asking.get(slot[0]);
            if (!keys) {
                asking.set(slot[0], keys = new Set());
            }
            if (keys.has(slot[1])) {
                continue;
            }
            keys.add(slot[1]);
            const gen = (checker as unknown as Record<string, {gen(...a: unknown[]): APIRequestGenerator}>)[method].gen;
            generators.push(settled(gen(...args)));
            store.push(answer => {
                if (answer !== failed) {
                    slot[0].set(slot[1], answer);
                }
            });
        }
        if (generators.length > 0) {
            api.batch(...generators).forEach((answer, i) => store[i](answer));
        }
    };

    const methods = new Map<string, unknown>([["prefetch", prefetch], ["ask", ask]]);
    return new Proxy(checker, {
        get(target, property: string, receiver) {
            let method = methods.get(property);
            if (method === undefined) {
                const value = Reflect.get(target, property, receiver);
                if (typeof value !== "function") {
                    return value;
                }
                const call = (value as (...a: unknown[]) => unknown).bind(target);
                methods.set(property, method = (...args: unknown[]) => remembered(property, args, call));
            }
            return method;
        },
    }) as MemoizingChecker;
}
