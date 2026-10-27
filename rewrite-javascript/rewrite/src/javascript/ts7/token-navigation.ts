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
import {getChildren, SyntaxKind, type Node, type SourceFile} from "typescript/ast";

// The compiler sends only its tree's own nodes, so `forEachChild` skips punctuation. `getChildren`
// materializes the tokens between them, whose offsets the LST hangs whitespace on, and leaves out
// the JSDoc types the compiler reparses into the tree.

// A token or list that `getChildren` materializes has no counterpart in the compiler process, so it
// cannot be handed to the checker. Callers ask this before doing so. The compiler's own children of a
// node are what `forEachChild` yields, so anything else under the same parent was materialized.
const treeChildren = new WeakMap<Node, Set<Node>>();

export function isScanned(node: Node): boolean {
    if (node.kind === SyntaxKind.SyntaxList) {
        return true;
    }
    const parent = node.parent;
    if (!parent) {
        return false;
    }
    let inTree = treeChildren.get(parent);
    if (!inTree) {
        const children = new Set<Node>();
        parent.forEachChild(
            child => {
                children.add(child);
                return undefined;
            },
            nodes => {
                nodes.forEach(element => children.add(element));
                return undefined;
            });
        treeChildren.set(parent, inTree = children);
    }
    return !inTree.has(node);
}

const isJSDoc = (k: SyntaxKind) => k >= SyntaxKind.FirstJSDocNode && k <= SyntaxKind.LastJSDocNode;

// Callers compare children by identity to find a node among its siblings, so a node's children are
// built once and handed back on every later ask.
const childCache = new WeakMap<Node, Node[]>();

export function childrenOf(node: Node, sourceFile: SourceFile = node.getSourceFile()): Node[] {
    let children = childCache.get(node);
    if (!children) {
        // A node's JSDoc comments come first among its children, and the parser reads none of them.
        children = getChildren(node, sourceFile).filter(child => !isJSDoc(child.kind));
        childCache.set(node, children);
    }
    return children;
}

export function childCountOf(node: Node, sourceFile: SourceFile = node.getSourceFile()): number {
    return childrenOf(node, sourceFile).length;
}

export function childAt(node: Node, index: number, sourceFile: SourceFile = node.getSourceFile()): Node {
    return childrenOf(node, sourceFile)[index];
}

export function firstTokenOf(node: Node, sourceFile: SourceFile = node.getSourceFile()): Node | undefined {
    const child = childrenOf(node, sourceFile)[0];
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
