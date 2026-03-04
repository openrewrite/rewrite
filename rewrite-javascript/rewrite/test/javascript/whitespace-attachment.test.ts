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
import {JavaScriptParser} from "../../src/javascript";
import {JavaScriptPrinter} from "../../src/javascript/print";
import {MarkerPrinter, PrintOutputCapture} from "../../src/print";
import {J} from "../../src/java";
import {Tree} from "../../src/tree";

function prettifyKind(kind: string): string {
    const match = kind.match(/\.([A-Z]+)\$(.+)$/);
    if (match) {
        return `${match[1]}.${match[2]}`;
    }
    return kind;
}


class OutputNode {
    constructor(
        public readonly element: Tree,
        public readonly children: (OutputNode | string)[] = []
    ) {}

    addChild(child: OutputNode | string): void {
        this.children.push(child);
    }

    toString(): string {
        const childrenStr = this.children.map(child => {
            if (typeof child === 'string') {
                return "Text(" + child + ")";
            } else {
                return child.toString();
            }
        }).join(', ');

        return `${prettifyKind(this.element.kind)}{${childrenStr}}`;
    }
}

/**
 * Custom PrintOutputCapture that builds a tree structure instead of a flat string
 */
class TreeStructurePrintOutputCapture extends PrintOutputCapture {
    public rootNodes: OutputNode[] = [];
    private nodeStack: OutputNode[] = [];

    constructor(markerPrinter: MarkerPrinter = MarkerPrinter.DEFAULT) {
        super(markerPrinter);
    }

    startNode(element: Tree): void {
        const node = new OutputNode(element);

        if (this.nodeStack.length > 0) {
            // Add to parent's children
            this.nodeStack[this.nodeStack.length - 1].addChild(node);
        } else {
            // This is a root node
            this.rootNodes.push(node);
        }

        this.nodeStack.push(node);
    }

    /**
     * Called when visiting ends for an LST element (afterSyntax)
     */
    endNode(): void {
        this.nodeStack.pop();
    }

    /**
     * Override append to add string content to the current node
     */
    override append(text: string | undefined): PrintOutputCapture {
        if (text && text.length > 0) {
            if (this.nodeStack.length > 0) {
                this.nodeStack[this.nodeStack.length - 1].addChild(text);
            }
            super.append(text);
        }
        return this;
    }

    /**
     * Get the tree structure as a formatted string
     */
    getTreeStructure(): string {
        return this.rootNodes.map(node => node.toString()).join('\n');
    }
}

/**
 * Custom JavaScriptPrinter that uses TreeStructurePrintOutputCapture
 */
class TreeCapturingJavaScriptPrinter extends JavaScriptPrinter {
    protected override async beforeSyntax(j: J, p: PrintOutputCapture): Promise<void> {
        if (p instanceof TreeStructurePrintOutputCapture) {
            p.startNode(j);
        }
        await super['beforeSyntax'](j, p);
    }

    protected override async afterSyntax(j: J, p: PrintOutputCapture): Promise<void> {
        await super['afterSyntax'](j, p);
        if (p instanceof TreeStructurePrintOutputCapture) {
            p.endNode();
        }
    }
}

function findWhitespaceViolations(rootNodes: OutputNode[]): string[] {
    const violations: string[] = [];

    function checkNode(node: OutputNode, path: string = 'root'): void {
        if (node.children.length > 0) {
            const firstChild = node.children[0];

            // Check if first child is a node (not text)
            if (firstChild instanceof OutputNode) {
                // Skip EnumValueSet - its children intentionally have their own prefix for uniform formatting
                if (node.element.kind === J.Kind.EnumValueSet) {
                    // Don't check whitespace attachment for EnumValueSet children
                } else if (firstChild.children.length > 0) {
                    // Check if the grandchild exists and is text with non-empty whitespace
                    const grandchild = firstChild.children[0];
                    if (typeof grandchild === 'string' && grandchild.trim() === '' && grandchild.length > 0) {
                        const parentKind = prettifyKind(node.element.kind);
                        const childKind = prettifyKind(firstChild.element.kind);
                        violations.push(`${parentKind} has child ${childKind} starting with whitespace |${grandchild}|. The whitespace should rather be attached to ${parentKind} around ${firstChild.toString()}.`);
                    }
                }
            }
        }

        // Recursively check all child nodes
        node.children.forEach((child, index) => {
            if (child instanceof OutputNode) {
                checkNode(child, `${path}[${index}]`);
            }
        });
    }

    rootNodes.forEach((node, index) => {
        checkNode(node, `root[${index}]`);
    });

    return violations;
}

describe('whitespace should be attached to the outermost element', () => {
    test.each([
        "const c =  function(): number { return 116; };",
        "const x = new Date();",
        "async function m(): void { await Promise.resolve(); }",
        "class ResponseHandler extends EventEmitter<{ success: string; error: Error }> {}",
        "import React = require('react');",
        `export const enum Result { Good = "Good", Bad = "Bad" }`,
        "for (let i = 1; i < [4, 3, 6].length; i++) {",
        `import "./rpc"; declare module "./tree" {}`,
        "const userScores = new Map<string, number>()",
        "function* generateUsers() { yield { id: 1 } };",
        "type T = undefined extends undefined ? string : never;",
        "const FirstEntity = class FirstEntityClass {};",
        `
        for (
          let i = 0; i < numberOfConnections; i++
        ) {}
        `
    ])('%s', async (sourceCode) => {
        // given
        const parser = new JavaScriptParser();
        const cu = await (await parser.parse({text: sourceCode, sourcePath: 'test.ts'}).next()).value;
        const capture = new TreeStructurePrintOutputCapture(MarkerPrinter.SANITIZED);
        const printer = new TreeCapturingJavaScriptPrinter();

        // when
        await printer.visit(cu, capture);

        // then
        const violations = findWhitespaceViolations(capture.rootNodes);
        expect(violations).toEqual([]);
    });
});
