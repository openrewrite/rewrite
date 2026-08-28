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

export * from "typescript/unstable/ast";
export * from "typescript/unstable/sync";
// ModifierFlags comes from both entry points; the syntax one is what the parser reads.
export {ModifierFlags} from "typescript/unstable/ast";

export type {
    MethodSignatureDeclaration as MethodSignature,
    PropertySignatureDeclaration as PropertySignature,
    SignatureDeclaration as SignatureDeclarationBase,
} from "typescript/unstable/ast";

import type {Declaration, DeclarationName} from "typescript/unstable/ast";

// `Declaration` says only that a node declares something; the declarations that carry a name do so
// on their own interfaces, and this is the shape of the ones the parser reads a name from.
export type NamedDeclaration = Declaration & {readonly name?: DeclarationName};
export {
    isMethodSignatureDeclaration as isMethodSignature,
    isParameterDeclaration as isParameter,
    isPropertySignatureDeclaration as isPropertySignature,
} from "typescript/unstable/ast";
export type {Checker as TypeChecker} from "typescript/unstable/sync";

import {skipTrivia, type Node, type NodeArray, type SourceFile} from "typescript/unstable/ast";

/** Whether a source file is a module, which its export or import marks it as being. */
export function isExternalModule(sourceFile: SourceFile): boolean {
    return sourceFile.externalModuleIndicator !== undefined;
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
