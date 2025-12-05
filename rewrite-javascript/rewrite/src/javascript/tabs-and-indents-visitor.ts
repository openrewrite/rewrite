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
import {isJavaScript, JS, JSX} from "./tree";
import {JavaScriptVisitor} from "./visitor";
import {isJava, J, lastWhitespace, replaceLastWhitespace} from "../java";
import {produce} from "immer";
import {Cursor, isScope, Tree} from "../tree";
import {TabsAndIndentsStyle} from "./style";

type IndentKind = 'block' | 'continuation' | 'align';
export class TabsAndIndentsVisitor<P> extends JavaScriptVisitor<P> {
    private readonly singleIndent: string;

    constructor(private readonly tabsAndIndentsStyle: TabsAndIndentsStyle, private stopAfter?: Tree) {
        super();

        if (this.tabsAndIndentsStyle.useTabCharacter) {
            this.singleIndent = "\t";
        } else {
            this.singleIndent = " ".repeat(this.tabsAndIndentsStyle.indentSize);
        }
    }

    protected async preVisit(tree: J, _p: P): Promise<J | undefined> {
        this.setupCursorMessagesForTree(this.cursor, tree);
        return tree;
    }

    private setupCursorMessagesForTree(cursor: Cursor, tree: J): void {
        const [parentMyIndent, parentIndentKind] = this.getParentIndentContext(cursor);
        const myIndent = this.computeMyIndent(tree, parentMyIndent, parentIndentKind);
        cursor.messages.set("myIndent", myIndent);

        // For Binary, behavior depends on whether it's already on a continuation line
        if (tree.kind === J.Kind.Binary) {
            const hasNewline = tree.prefix?.whitespace?.includes("\n") ||
                tree.prefix?.comments?.some(c => c.suffix.includes("\n"));
            // If Binary has newline, children align. Otherwise, children get continuation.
            cursor.messages.set("indentKind", hasNewline ? 'align' : 'continuation');
            // For LeftPadded children (like operator), same rule applies
            cursor.messages.set("leftPaddedContinuation", hasNewline ? 'align' : 'propagate');
        } else {
            cursor.messages.set("indentKind", this.computeIndentKind(tree));
            cursor.messages.set("leftPaddedContinuation", 'propagate');
        }
    }

    private getParentIndentContext(cursor: Cursor): [string, IndentKind] {
        for (let c = cursor.parent; c != null; c = c.parent) {
            const indent = c.messages.get("myIndent") as string | undefined;
            if (indent !== undefined) {
                const kind = c.messages.get("indentKind") as IndentKind ?? 'continuation';
                return [indent, kind];
            }
        }
        return ["", 'continuation'];
    }

    private computeMyIndent(tree: J, parentMyIndent: string, parentIndentKind: IndentKind): string {
        if (tree.kind === J.Kind.IfElse || parentIndentKind === 'align') {
            return parentMyIndent;
        }
        if (parentIndentKind === 'block') {
            return parentMyIndent + this.singleIndent;
        }
        const hasNewline = tree.prefix?.whitespace?.includes("\n") ||
            tree.prefix?.comments?.some(c => c.suffix.includes("\n"));
        return hasNewline ? parentMyIndent + this.singleIndent : parentMyIndent;
    }

    private computeIndentKind(tree: J): IndentKind {
        switch (tree.kind) {
            case J.Kind.Block:
            case J.Kind.Case:
                return 'block';
            case JS.Kind.CompilationUnit:
                return 'align';
            default:
                return 'continuation';
        }
    }

    override async postVisit(tree: J, _p: P): Promise<J | undefined> {
        if (this.stopAfter != null && isScope(this.stopAfter, tree)) {
            this.cursor?.root.messages.set("stop", true);
        }

        const myIndent = this.cursor.messages.get("myIndent") as string | undefined;
        if (myIndent === undefined) {
            return tree;
        }

        let result = tree;
        if (result.prefix?.whitespace?.includes("\n")) {
            result = produce(result, draft => {
                draft.prefix!.whitespace = this.combineIndent(draft.prefix!.whitespace, myIndent);
            });
        }

        if (result.kind === J.Kind.Block) {
            result = this.normalizeBlockEnd(result as J.Block, myIndent);
        } else if (result.kind === JS.Kind.JsxTag) {
            result = this.normalizeJsxTagEnd(result as JSX.Tag, myIndent);
        }

        return result;
    }

    private normalizeBlockEnd(block: J.Block, myIndent: string): J.Block {
        const effectiveLastWs = lastWhitespace(block.end);
        if (!effectiveLastWs.includes("\n")) {
            return block;
        }
        return produce(block, draft => {
            draft.end = replaceLastWhitespace(draft.end, ws => this.combineIndent(ws, myIndent));
        });
    }

    private normalizeJsxTagEnd(tag: JSX.Tag, myIndent: string): JSX.Tag {
        if (!tag.children || tag.children.length === 0) {
            return tag;
        }
        const lastChild = tag.children[tag.children.length - 1];
        if (lastChild.kind !== J.Kind.Literal || !lastChild.prefix.whitespace.includes("\n")) {
            return tag;
        }
        return produce(tag, draft => {
            const lastChildDraft = draft.children![draft.children!.length - 1];
            lastChildDraft.prefix.whitespace = this.combineIndent(lastChildDraft.prefix.whitespace, myIndent);
        });
    }

    public async visitContainer<T extends J>(container: J.Container<T>, p: P): Promise<J.Container<T>> {
        const parentIndent = this.cursor.messages.get("myIndent") as string ?? "";
        const elementsIndent = container.before.whitespace.includes("\n")
            ? parentIndent + this.singleIndent
            : parentIndent;

        const savedMyIndent = this.cursor.messages.get("myIndent");
        this.cursor.messages.set("myIndent", elementsIndent);
        let ret = await super.visitContainer(container, p);
        if (savedMyIndent !== undefined) {
            this.cursor.messages.set("myIndent", savedMyIndent);
        }

        if (ret.before.whitespace.includes("\n")) {
            ret = produce(ret, draft => {
                draft.before.whitespace = this.combineIndent(draft.before.whitespace, elementsIndent);
            });
        }

        if (ret.elements.length > 0) {
            const effectiveLastWs = lastWhitespace(ret.elements[ret.elements.length - 1].after);
            if (effectiveLastWs.includes("\n")) {
                ret = produce(ret, draft => {
                    const lastDraft = draft.elements[draft.elements.length - 1];
                    lastDraft.after = replaceLastWhitespace(lastDraft.after, ws => this.combineIndent(ws, parentIndent));
                });
            }
        }

        return ret;
    }

    public async visitLeftPadded<T extends J | J.Space | number | string | boolean>(
        left: J.LeftPadded<T>,
        p: P
    ): Promise<J.LeftPadded<T> | undefined> {
        const parentIndent = this.cursor.messages.get("myIndent") as string ?? "";
        const continuation = this.cursor.messages.get("leftPaddedContinuation") as string ?? 'propagate';
        const hasNewline = left.before.whitespace.includes("\n");
        const shouldPropagate = hasNewline && continuation === 'propagate';

        // For 'propagate' mode, update myIndent for children so nested structures get proper indent
        const savedMyIndent = this.cursor.messages.get("myIndent");
        if (shouldPropagate) {
            this.cursor.messages.set("myIndent", parentIndent + this.singleIndent);
        }

        const ret = await super.visitLeftPadded(left, p);

        // Restore myIndent
        if (savedMyIndent !== undefined) {
            this.cursor.messages.set("myIndent", savedMyIndent);
        } else if (shouldPropagate) {
            this.cursor.messages.delete("myIndent");
        }

        if (ret === undefined || !hasNewline) {
            return ret;
        }
        return produce(ret, draft => {
            draft.before.whitespace = this.combineIndent(draft.before.whitespace, parentIndent + this.singleIndent);
        });
    }

    async visit<R extends J>(tree: Tree, p: P, parent?: Cursor): Promise<R | undefined> {
        if (this.cursor?.getNearestMessage("stop") != null) {
            return tree as R;
        }

        if (parent) {
            this.cursor = new Cursor(tree, parent);
            this.setupAncestorIndents();
        }

        return await super.visit(tree, p) as R;
    }

    private setupAncestorIndents(): void {
        const path: Cursor[] = [];
        let anchorCursor: Cursor | undefined;
        let anchorIndent = "";

        for (let c = this.cursor.parent; c; c = c.parent) {
            path.push(c);
            const v = c.value;

            if (this.isActualJNode(v) && !anchorCursor && v.prefix) {
                const ws = lastWhitespace(v.prefix);
                const idx = ws.lastIndexOf('\n');
                if (idx !== -1) {
                    anchorCursor = c;
                    anchorIndent = ws.substring(idx + 1);
                }
            }

            if (v.kind === JS.Kind.CompilationUnit) {
                if (!anchorCursor) {
                    anchorCursor = c;
                    anchorIndent = "";
                }
                break;
            }
        }

        if (path.length === 0) return;
        path.reverse();

        for (const c of path) {
            const v = c.value;
            if (!this.isActualJNode(v)) continue;

            const savedCursor = this.cursor;
            this.cursor = c;
            if (c === anchorCursor) {
                c.messages.set("myIndent", anchorIndent);
                c.messages.set("indentKind", this.computeIndentKind(v));
            } else {
                this.setupCursorMessagesForTree(c, v);
            }
            this.cursor = savedCursor;
        }
    }

    private isActualJNode(v: any): v is J {
        return (isJava(v) || isJavaScript(v)) &&
            v.kind !== J.Kind.Container &&
            v.kind !== J.Kind.LeftPadded &&
            v.kind !== J.Kind.RightPadded;
    }

    private combineIndent(oldWs: string, newIndent: string): string {
        const lastNewline = oldWs.lastIndexOf("\n");
        return oldWs.substring(0, lastNewline + 1) + newIndent;
    }
}
