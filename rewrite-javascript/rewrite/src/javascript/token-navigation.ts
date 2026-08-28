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
import ts from "./compiler";

// Punctuation and keyword tokens are absent from node fields, and the LST needs their offsets to
// place whitespace. The parser reaches them only through these five calls, keeping its dependency
// on the TypeScript AST's token surface in one file.

export function childrenOf(node: ts.Node, sourceFile?: ts.SourceFile): readonly ts.Node[] {
    return node.getChildren(sourceFile);
}

export function childCountOf(node: ts.Node, sourceFile?: ts.SourceFile): number {
    return node.getChildCount(sourceFile);
}

export function childAt(node: ts.Node, index: number, sourceFile?: ts.SourceFile): ts.Node {
    return node.getChildAt(index, sourceFile);
}

export function firstTokenOf(node: ts.Node, sourceFile?: ts.SourceFile): ts.Node | undefined {
    return node.getFirstToken(sourceFile);
}

export function lastTokenOf(node: ts.Node, sourceFile?: ts.SourceFile): ts.Node | undefined {
    return node.getLastToken(sourceFile);
}
