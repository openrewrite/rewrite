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

// The one place the parser binds to the compiler. TypeScript serves the syntax tree and the type
// system from separate entry points and spells some names differently from the release before it,
// so both are gathered here under the single `ts` namespace the parser reads.

export * from "typescript/ast";
export * from "typescript/sync";
// ModifierFlags comes from both entry points; the syntax one is what the parser reads.
export {ModifierFlags} from "typescript/ast";

export type {
    MethodSignatureDeclaration as MethodSignature,
    PropertySignatureDeclaration as PropertySignature,
    SignatureDeclaration as SignatureDeclarationBase,
} from "typescript/ast";

import type {Declaration, DeclarationName} from "typescript/ast";

// `Declaration` says only that a node declares something; the declarations that carry a name do so
// on their own interfaces, and this is the shape of the ones the parser reads a name from.
export type NamedDeclaration = Declaration & {readonly name?: DeclarationName};
export {
    isMethodSignatureDeclaration as isMethodSignature,
    isParameterDeclaration as isParameter,
    isPropertySignatureDeclaration as isPropertySignature,
} from "typescript/ast";
export type {Checker as TypeChecker} from "typescript/sync";

import {
    isBinaryExpression,
    isCallExpression,
    isClassDeclaration,
    isClassExpression,
    isIdentifier,
    isNoSubstitutionTemplateLiteral,
    isPropertyAccessExpression,
    isStringLiteral,
    NodeFlags,
    skipTrivia,
    SyntaxKind,
    type ClassDeclaration,
    type ClassExpression,
    type Node,
    type NodeArray,
    type NoSubstitutionTemplateLiteral,
    type SourceFile,
    type StringLiteral,
} from "typescript/ast";

/** Whether a source file is a module, which its export or import marks it as being. */
export function isExternalModule(sourceFile: SourceFile): boolean {
    return sourceFile.externalModuleIndicator !== undefined;
}

const commonJsModules = new WeakMap<SourceFile, boolean>();

/**
 * Whether a JavaScript file is a CommonJS module, which a `require('m')` call or an assignment to
 * `exports` or `module.exports` marks it as being. The compiler does not report it, so it is read
 * off the tree.
 */
export function isCommonJsModule(sourceFile: SourceFile): boolean {
    let known = commonJsModules.get(sourceFile);
    if (known === undefined) {
        commonJsModules.set(sourceFile, known = /\.[cm]?jsx?$/.test(sourceFile.fileName) &&
            !!sourceFile.forEachChild(function marks(node): true | undefined {
                return isRequireCall(node) || isExportsAssignment(node) || node.forEachChild(marks);
            }));
    }
    return known;
}

function isRequireCall(node: Node): boolean {
    return isCallExpression(node) && isIdentifier(node.expression) && node.expression.text === "require" &&
        node.arguments.length === 1 && isStringLiteralLike(node.arguments[0]);
}

function isExportsAssignment(node: Node): boolean {
    if (!isBinaryExpression(node) || node.operatorToken.kind !== SyntaxKind.EqualsToken) {
        return false;
    }
    let target: Node = node.left;
    while (isPropertyAccessExpression(target)) {
        if (isIdentifier(target.expression) && target.expression.text === "module" && target.name.text === "exports") {
            return true;
        }
        target = target.expression;
    }
    return isIdentifier(target) && target.text === "exports" && target !== node.left;
}

export function isStringLiteralLike(node: Node): node is StringLiteral | NoSubstitutionTemplateLiteral {
    return isStringLiteral(node) || isNoSubstitutionTemplateLiteral(node);
}

export function isClassLike(node: Node): node is ClassDeclaration | ClassExpression {
    return isClassDeclaration(node) || isClassExpression(node);
}

/**
 * Whether a comma follows the last element of `nodes`. The compiler declares `hasTrailingComma` on
 * a NodeArray but sends it unset, so the answer is read back off the source.
 */
export function hasTrailingComma(nodes: NodeArray<Node>, sourceFile: SourceFile): boolean {
    if (nodes.length === 0) {
        return false;
    }
    const afterLast = skipTrivia(sourceFile.text, nodes[nodes.length - 1].end);
    return sourceFile.text[afterLast] === ",";
}

/**
 * The `?` a member carries, if any. A named member holds whichever of `?` and `!` follows its name
 * in one `postfixToken`, while the other declarations that can take a `?` keep their own field.
 * A parameter JSDoc names in brackets also gets a `?`, which the source does not hold.
 */
export function questionTokenOf(node: Node): Node | undefined {
    const token = (node as {postfixToken?: Node; questionToken?: Node}).postfixToken
        ?? (node as {questionToken?: Node}).questionToken;
    return token?.kind === SyntaxKind.QuestionToken && !isReparsed(token) ? token : undefined;
}

/** The `!` a member carries, if any; it shares `postfixToken` with `?`. */
export function exclamationTokenOf(node: Node): Node | undefined {
    const token = (node as {postfixToken?: Node; exclamationToken?: Node}).postfixToken
        ?? (node as {exclamationToken?: Node}).exclamationToken;
    return token?.kind === SyntaxKind.ExclamationToken ? token : undefined;
}

/**
 * Whether a node came from a JSDoc comment rather than from syntax. The compiler turns JSDoc types
 * into nodes of the declaration they annotate, positioned inside the comment they were read from.
 */
export function isReparsed(node: Node | undefined): boolean {
    return node !== undefined && (node.flags & NodeFlags.Reparsed) !== 0;
}
