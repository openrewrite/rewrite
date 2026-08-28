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
import {createScanner, LanguageVariant, type Node, type NodeArray, type SourceFile, SyntaxKind} from "typescript7/unstable/ast";

// The TypeScript 7 AST arrives deserialised from the Go compiler, which carries only the semantic
// children; `forEachChild` therefore skips punctuation. Scanning each gap between children recovers
// the tokens whose offsets the LST hangs whitespace on. ../token-navigation.ts is the same surface
// over the TypeScript 6 AST, where the compiler supplies these children directly.

/** A scanned token. It has no place in the Go compiler's tree, so it carries only what callers read. */
interface SyntheticToken {
    readonly kind: SyntaxKind;
    readonly pos: number;
    readonly end: number;
}

/** Wraps a NodeArray so a list's own punctuation stays reachable, mirroring TypeScript's SyntaxList. */
interface SyntaxListNode extends SyntheticToken {
    readonly [SYNTAX_LIST]: NodeArray<Node>;
}

// Real nodes carry fields like `elements` and `types`, so the wrapper is tagged with a symbol that
// nothing in the compiler's tree can collide with.
const SYNTAX_LIST = Symbol("syntaxList");

export type Child = Node | SyntheticToken | SyntaxListNode;

const isSyntaxList = (n: Child): n is SyntaxListNode => (n as SyntaxListNode)[SYNTAX_LIST] !== undefined;
const isToken = (k: SyntaxKind) => k >= SyntaxKind.FirstToken && k <= SyntaxKind.LastToken;
const isJSDoc = (k: SyntaxKind) => k >= SyntaxKind.FirstJSDocNode && k <= SyntaxKind.LastJSDocNode;

function scanBetween(sf: SourceFile, from: number, to: number, out: Child[]): void {
    if (to <= from) {
        return;
    }
    const scanner = createScanner(true, sf.languageVariant ?? LanguageVariant.Standard, sf.text);
    scanner.setText(sf.text, from, to - from);
    for (; ;) {
        const kind = scanner.scan();
        if (kind === SyntaxKind.EndOfFile) {
            break;
        }
        // A token's `pos` runs from the end of the previous one, so leading trivia belongs to it.
        out.push({kind, pos: scanner.getTokenFullStart(), end: scanner.getTokenEnd()});
    }
}

export function childrenOf(node: Child, sourceFile: SourceFile): Child[] {
    if (isSyntaxList(node)) {
        return syntaxListChildren(node, sourceFile);
    }
    if (isToken(node.kind)) {
        return [];
    }
    const children: Child[] = [];
    let pos = node.pos;
    // JSDoc reaches the tree only through `node.jsDoc`, and the parser asks for none of it, so the
    // children stop at what `forEachChild` yields.
    (node as Node).forEachChild(
        child => {
            scanBetween(sourceFile, pos, child.pos, children);
            children.push(child);
            pos = child.end;
            return undefined;
        },
        nodes => {
            scanBetween(sourceFile, pos, nodes.pos, children);
            children.push({kind: SyntaxKind.SyntaxList, pos: nodes.pos, end: nodes.end, [SYNTAX_LIST]: nodes});
            pos = nodes.end;
            return undefined;
        });
    scanBetween(sourceFile, pos, node.end, children);
    return children;
}

function syntaxListChildren(list: SyntaxListNode, sourceFile: SourceFile): Child[] {
    const out: Child[] = [];
    let pos = list.pos;
    for (const element of list[SYNTAX_LIST]) {
        scanBetween(sourceFile, pos, element.pos, out);
        out.push(element);
        pos = element.end;
    }
    scanBetween(sourceFile, pos, list.end, out);
    return out;
}

export function childCountOf(node: Child, sourceFile: SourceFile): number {
    return childrenOf(node, sourceFile).length;
}

export function childAt(node: Child, index: number, sourceFile: SourceFile): Child | undefined {
    return childrenOf(node, sourceFile)[index];
}

export function firstTokenOf(node: Child, sourceFile: SourceFile): Child | undefined {
    const child = childrenOf(node, sourceFile).find(c => !isJSDoc(c.kind));
    if (!child) {
        return undefined;
    }
    return child.kind < SyntaxKind.FirstNode ? child : firstTokenOf(child, sourceFile);
}

export function lastTokenOf(node: Child, sourceFile: SourceFile): Child | undefined {
    const children = childrenOf(node, sourceFile);
    const child = children[children.length - 1];
    if (!child) {
        return undefined;
    }
    return child.kind < SyntaxKind.FirstNode ? child : lastTokenOf(child, sourceFile);
}
