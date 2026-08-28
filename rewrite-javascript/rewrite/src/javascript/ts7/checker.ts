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

/** The dotted chain of symbol names from `symbol` up to the outermost symbol enclosing it. */
export function fullyQualifiedNameOf(symbol: Symbol7): string {
    const parts: string[] = [];
    let current: Symbol7 | undefined = symbol;
    while (current) {
        parts.unshift(declaredNameOf(current));
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
