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
    createScanner,
    LanguageVariant,
    NodeFlags,
    skipTrivia,
    type Node,
    type NodeArray,
    type SourceFile,
    SyntaxKind,
} from "typescript/unstable/ast";

// The compiler sends only the semantic children, so `forEachChild` skips punctuation. Scanning each
// gap between children recovers the tokens whose offsets the LST hangs whitespace on. They come back
// as nodes, since the parser reads a child's text and position the same way whichever kind it is.

// Real nodes carry fields like `elements` and `types`, so a list is tagged with a symbol that
// nothing in the compiler's tree can collide with.
const SYNTAX_LIST = Symbol("syntaxList");

// A scanned token has no counterpart in the compiler process, so it cannot be handed back to the
// checker; callers ask this before doing so.
const SCANNED = Symbol("scanned");

export function isScanned(node: Node): boolean {
    return (node as unknown as Record<symbol, unknown>)[SCANNED] === true;
}

interface SyntaxListNode extends Node {
    readonly [SYNTAX_LIST]: NodeArray<Node>;
}

const isSyntaxList = (n: Node): n is SyntaxListNode => (n as SyntaxListNode)[SYNTAX_LIST] !== undefined;
const isToken = (k: SyntaxKind) => k >= SyntaxKind.FirstToken && k <= SyntaxKind.LastToken;
const isJSDoc = (k: SyntaxKind) => k >= SyntaxKind.FirstJSDocNode && k <= SyntaxKind.LastJSDocNode;

/** Stands for a token the compiler leaves out of the tree, backed by the source text it was read from. */
function scannedNode(kind: SyntaxKind, pos: number, end: number, sourceFile: SourceFile, parent: Node,
                     elements?: NodeArray<Node>): Node {
    // Trivia is whitespace and comments both, so the token starts wherever skipping it lands.
    const start = () => Math.min(skipTrivia(sourceFile.text, pos), end);
    const leadingTriviaWidth = () => start() - pos;
    const node = {
        [SCANNED]: true,
        kind, pos, end, parent,
        flags: 0,
        forEachChild: () => undefined,
        getSourceFile: () => sourceFile,
        getStart: () => start(),
        getFullStart: () => pos,
        getEnd: () => end,
        getWidth: () => end - start(),
        getFullWidth: () => end - pos,
        getLeadingTriviaWidth: () => leadingTriviaWidth(),
        getFullText: () => sourceFile.text.slice(pos, end),
        getText: () => sourceFile.text.slice(start(), end),
    };
    return (elements ? {...node, [SYNTAX_LIST]: elements} : node) as unknown as Node;
}

function scanBetween(sf: SourceFile, from: number, to: number, parent: Node, out: Node[]): void {
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
        out.push(scannedNode(kind, scanner.getTokenFullStart(), scanner.getTokenEnd(), sf, parent));
    }
}

// Callers compare children by identity to find a node among its siblings, so a node's children are
// built once and handed back on every later ask.
const childCache = new WeakMap<Node, Node[]>();

export function childrenOf(node: Node, sourceFile: SourceFile = node.getSourceFile()): Node[] {
    const cached = childCache.get(node);
    if (cached) {
        return cached;
    }
    const built = buildChildren(node, sourceFile);
    childCache.set(node, built);
    return built;
}

function buildChildren(node: Node, sourceFile: SourceFile): Node[] {
    if (isSyntaxList(node)) {
        return syntaxListChildren(node, sourceFile);
    }
    if (isToken(node.kind)) {
        return [];
    }
    const children: Node[] = [];
    let pos = node.pos;
    // JSDoc reaches the tree only through `node.jsDoc`, and the parser asks for none of it, so the
    // children stop at what `forEachChild` yields.
    node.forEachChild(
        child => {
            // The compiler turns JSDoc types into nodes of the declaration they annotate. They sit
            // at the comment's offsets, where the source holds a comment rather than a type.
            if (child.flags & NodeFlags.Reparsed) {
                return undefined;
            }
            scanBetween(sourceFile, pos, child.pos, node, children);
            children.push(child);
            pos = child.end;
            return undefined;
        },
        nodes => {
            scanBetween(sourceFile, pos, nodes.pos, node, children);
            children.push(scannedNode(SyntaxKind.SyntaxList, nodes.pos, nodes.end, sourceFile, node, nodes));
            pos = nodes.end;
            return undefined;
        });
    scanBetween(sourceFile, pos, node.end, node, children);
    return children;
}

function syntaxListChildren(list: SyntaxListNode, sourceFile: SourceFile): Node[] {
    const out: Node[] = [];
    let pos = list.pos;
    for (const element of list[SYNTAX_LIST]) {
        scanBetween(sourceFile, pos, element.pos, list, out);
        out.push(element);
        pos = element.end;
    }
    scanBetween(sourceFile, pos, list.end, list, out);
    return out;
}

export function childCountOf(node: Node, sourceFile: SourceFile = node.getSourceFile()): number {
    return childrenOf(node, sourceFile).length;
}

export function childAt(node: Node, index: number, sourceFile: SourceFile = node.getSourceFile()): Node {
    return childrenOf(node, sourceFile)[index];
}

export function firstTokenOf(node: Node, sourceFile: SourceFile = node.getSourceFile()): Node | undefined {
    const child = childrenOf(node, sourceFile).find(c => !isJSDoc(c.kind));
    if (!child) {
        return undefined;
    }
    return child.kind < SyntaxKind.FirstNode ? child : firstTokenOf(child, sourceFile);
}

export function lastTokenOf(node: Node, sourceFile: SourceFile = node.getSourceFile()): Node | undefined {
    const children = childrenOf(node, sourceFile);
    const child = children[children.length - 1];
    if (!child) {
        return undefined;
    }
    return child.kind < SyntaxKind.FirstNode ? child : lastTokenOf(child, sourceFile);
}
