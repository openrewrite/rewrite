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

        // Prettify the kind: org.openrewrite.javascript.tree.JS$CompilationUnit -> JS.CompilationUnit
        const prettifyKind = (kind: string): string => {
            const match = kind.match(/\.([A-Z]+)\$(.+)$/);
            if (match) {
                return `${match[1]}.${match[2]}`;
            }
            return kind;
        };

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

describe('whitespace attachment', () => {
    test('simple variable declaration', async () => {
        // given
        const parser = new JavaScriptParser();
        const sourceCode = "const c =  function(): number { return 116; };";
        const cu = await (await parser.parse({text: sourceCode, sourcePath: 'test.ts'}).next()).value;
        const capture = new TreeStructurePrintOutputCapture(MarkerPrinter.SANITIZED);
        const printer = new TreeCapturingJavaScriptPrinter();

        // when
        await printer.visit(cu, capture);

        // then
        // Check for problematic whitespace attachment:
        // - a node starts
        // - its first child is another node (not text)
        // - the first child of that child (grandchild) is text containing non-empty whitespace
        process.stdout.write(capture.rootNodes[0].toString());
        const violations: string[] = [];

        function checkNode(node: OutputNode, path: string = 'root'): void {
            if (node.children.length > 0) {
                const firstChild = node.children[0];

                // Check if first child is a node (not text)
                if (firstChild instanceof OutputNode) {
                    // Check if the grandchild exists and is text with non-empty whitespace
                    if (firstChild.children.length > 0) {
                        const grandchild = firstChild.children[0];
                        if (typeof grandchild === 'string' && grandchild.trim() === '' && grandchild.length > 0) {
                            violations.push(`${path} -> Node -> "${grandchild}"`);
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

        capture.rootNodes.forEach((node, index) => {
            checkNode(node, `root[${index}]`);
        });

        expect(violations).toEqual([]);
    });
});
