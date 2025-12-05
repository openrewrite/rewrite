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

/**
 * Simple architecture for tabs and indents:
 *
 * 1. preVisit() computes and sets:
 *    - "myIndent": the indent for THIS element
 *    - "indentKind": strategy for children ('block' | 'continuation' | 'align')
 *
 * 2. Child's myIndent computation based on parent's indentKind:
 *    - 'block': myIndent = parent + singleIndent (always)
 *    - 'continuation': myIndent = parent + singleIndent only if has newline, else inherit
 *    - 'align': myIndent = parent (no additional indent, for top-level statements)
 *
 * 3. postVisit() applies myIndent to prefix (if has newline)
 *    - Skip for elements inside object literals
 *
 * 4. Special handling for closing delimiters:
 *    - Block.end: apply Block's myIndent
 *    - Container's last element's after: apply parent's myIndent
 *
 * IndentKind:
 * - 'block': all children get parent + singleIndent (for Block, Case, etc.)
 * - 'continuation' (default): children get parent + singleIndent only if on new line
 * - 'align': all children inherit parent's indent (for CompilationUnit top-level)
 */
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

    // ========== PHASE 1: preVisit - Compute myIndent and indentKind ==========

    protected async preVisit(tree: J, _p: P): Promise<J | undefined> {
        this.setupCursorMessagesForTree(this.cursor, tree);
        return tree;
    }

    /**
     * Sets up cursor messages (myIndent, indentKind) for a J tree node.
     * Factored out so it can be called during cursor initialization.
     */
    private setupCursorMessagesForTree(cursor: Cursor, tree: J): void {
        // Get parent's myIndent and indentKind
        const parentMyIndent = this.getParentMyIndentFrom(cursor);
        const parentIndentKind = this.getParentIndentKindFrom(cursor);

        // Compute this element's indent
        const myIndent = this.computeMyIndentForTree(cursor, tree, parentMyIndent, parentIndentKind);
        cursor.messages.set("myIndent", myIndent);

        // Set indent kind for OUR children (default: continuation)
        const indentKind = this.computeIndentKind(tree);
        cursor.messages.set("indentKind", indentKind);
    }

    /**
     * Get parent's myIndent from immediate parent (not walking up ancestors).
     */
    private getParentMyIndent(): string {
        return this.getParentMyIndentFrom(this.cursor);
    }

    private getParentMyIndentFrom(cursor: Cursor): string {
        for (let c = cursor.parent; c != null; c = c.parent) {
            const indent = c.messages.get("myIndent") as string | undefined;
            if (indent !== undefined) {
                return indent;
            }
        }
        return "";
    }

    private getParentIndentKindFrom(cursor: Cursor): IndentKind {
        for (let c = cursor.parent; c != null; c = c.parent) {
            const kind = c.messages.get("indentKind") as IndentKind | undefined;
            if (kind !== undefined) {
                return kind;
            }
        }
        return 'continuation';
    }

    /**
     * Compute what indent this element should have.
     */
    private computeMyIndentForTree(cursor: Cursor, tree: J, parentMyIndent: string, parentIndentKind: IndentKind): string {
        // Else clause: align with parent If
        if (tree.kind === J.Kind.IfElse) {
            return this.computeElseIndentFrom(cursor, parentMyIndent);
        }

        // 'align': inherit parent's indent directly (for top-level statements in CompilationUnit)
        if (parentIndentKind === 'align') {
            return parentMyIndent;
        }

        // Check if this element has a newline in its prefix
        const hasNewline = tree.prefix?.whitespace?.includes("\n") ||
            tree.prefix?.comments?.some(c => c.suffix.includes("\n"));

        if (parentIndentKind === 'block') {
            // 'block': always add singleIndent (regardless of newline)
            return parentMyIndent + this.singleIndent;
        }

        // 'continuation': add singleIndent only if has newline, else inherit
        if (hasNewline) {
            return parentMyIndent + this.singleIndent;
        }
        return parentMyIndent;
    }

    /**
     * Compute indent kind for OUR children.
     * - 'block': for Block, Case, etc. (all children get +indent)
     * - 'continuation' (default): children only get +indent if on new line
     * - 'align': children inherit parent's indent (for CompilationUnit top-level)
     */
    private computeIndentKind(tree: J): IndentKind {
        // Block: all statements get +indent
        if (tree.kind === J.Kind.Block) {
            return 'block';
        }

        // Case: all body statements get +indent
        if (tree.kind === J.Kind.Case) {
            return 'block';
        }

        // CompilationUnit: top-level statements stay at column 0
        if (tree.kind === JS.Kind.CompilationUnit) {
            return 'align';
        }

        // Default: continuation
        return 'continuation';
    }

    // ========== PHASE 2: postVisit - Apply myIndent ==========

    override async postVisit(tree: J, _p: P): Promise<J | undefined> {
        if (this.stopAfter != null && isScope(this.stopAfter, tree)) {
            this.cursor?.root.messages.set("stop", true);
        }

        let result = tree;

        // Apply myIndent to this element's prefix
        result = this.applyIndentToPrefix(result);

        // Handle Block's closing brace
        if (result.kind === J.Kind.Block) {
            result = this.normalizeBlockEnd(result as J.Block);
        }

        // Handle JSX tag closing
        if (result.kind === JS.Kind.JsxTag) {
            result = this.normalizeJsxTagEnd(result as JSX.Tag);
        }

        return result;
    }

    /**
     * Apply myIndent to this element's prefix if it has a newline.
     * Skip for elements inside object literals.
     */
    private applyIndentToPrefix(tree: J): J {
        if (!tree.prefix?.whitespace?.includes("\n")) {
            return tree;
        }

        // Skip for elements inside object literals (preserve author formatting)
        if (this.isInsideObjectLiteral()) {
            return tree;
        }

        const myIndent = this.cursor.messages.get("myIndent") as string | undefined;
        if (myIndent === undefined) {
            return tree;
        }

        return produce(tree, draft => {
            draft.prefix!.whitespace = this.combineIndent(draft.prefix!.whitespace, myIndent);
        });
    }

    /**
     * Check if we're inside an object literal (NewClass without class keyword).
     */
    private isInsideObjectLiteral(): boolean {
        for (let c = this.cursor.parent; c != null; c = c.parent) {
            if (c.value?.kind === J.Kind.NewClass) {
                const newClass = c.value as J.NewClass;
                if (!newClass.class) {
                    return true; // Object literal
                }
            }
        }
        return false;
    }

    /**
     * Normalize a Block's closing brace.
     */
    private normalizeBlockEnd(block: J.Block): J.Block {
        // Skip blocks inside object literals (even nested through lambdas)
        for (let c = this.cursor.parent; c != null; c = c.parent) {
            if (c.value?.kind === J.Kind.NewClass) {
                return block;
            }
        }

        const myIndent = this.cursor.messages.get("myIndent") as string | undefined;
        if (myIndent === undefined) {
            return block;
        }

        const effectiveLastWs = lastWhitespace(block.end);
        if (!effectiveLastWs.includes("\n")) {
            return block;
        }

        return produce(block, draft => {
            draft.end = replaceLastWhitespace(draft.end, (ws: string) => {
                return this.combineIndent(ws, myIndent);
            });
        });
    }

    /**
     * Normalize JSX tag's closing.
     */
    private normalizeJsxTagEnd(tag: JSX.Tag): JSX.Tag {
        if (!this.cursor.messages.get("jsxTagWithNewline")) {
            return tag;
        }

        const myIndent = this.cursor.messages.get("myIndent") as string | undefined;
        if (myIndent === undefined || !tag.children || tag.children.length === 0) {
            return tag;
        }

        const lastChild = tag.children[tag.children.length - 1];
        if (lastChild.kind !== J.Kind.Literal) {
            return tag;
        }

        return produce(tag, draft => {
            const lastChildDraft = draft.children![draft.children!.length - 1];
            if (lastChildDraft.prefix.whitespace.includes("\n")) {
                lastChildDraft.prefix.whitespace = this.combineIndent(lastChildDraft.prefix.whitespace, myIndent);
            }
        });
    }

    // ========== HELPER METHODS ==========

    /**
     * Else clause should align with its parent If statement.
     */
    private computeElseIndentFrom(cursor: Cursor, parentMyIndent: string): string {
        // Find the parent If's myIndent from the cursor chain
        for (let c = cursor.parent; c != null; c = c.parent) {
            if (c.value?.kind === J.Kind.If) {
                const ifIndent = c.messages.get("myIndent") as string | undefined;
                if (ifIndent !== undefined) {
                    return ifIndent;
                }
            }
        }
        return parentMyIndent;
    }

    // ========== WRAPPER TYPE HANDLERS ==========

    /**
     * Handle Container elements (e.g., method arguments).
     * - Container.before: continuation indent for first argument
     * - Last element's after: parent's myIndent for closing delimiter
     * Respects parent's indentKind.
     */
    public async visitContainer<T extends J>(container: J.Container<T>, p: P): Promise<J.Container<T>> {
        // Get parent's myIndent BEFORE super call (cursor is on parent of Container)
        // This is the MethodInvocation or similar that contains this Container
        const containerParentMyIndent = this.cursor.messages.get("myIndent") as string ?? "";

        // Compute the indent for elements in this container
        // Only add indent if container.before has newline - don't add based on 'block'
        // because computeMyIndent already handles that for child elements
        let containerElementsIndent: string;
        if (container.before.whitespace.includes("\n")) {
            containerElementsIndent = containerParentMyIndent + this.singleIndent;
        } else {
            containerElementsIndent = containerParentMyIndent;
        }

        // Set myIndent on current cursor so child elements can inherit it
        const savedMyIndent = this.cursor.messages.get("myIndent");
        this.cursor.messages.set("myIndent", containerElementsIndent);

        let ret = await super.visitContainer(container, p);

        // Restore parent's myIndent
        if (savedMyIndent !== undefined) {
            this.cursor.messages.set("myIndent", savedMyIndent);
        }

        // Skip modification for containers inside object literals
        if (this.isInsideObjectLiteral()) {
            return ret;
        }

        // Handle Container.before (whitespace before first element)
        if (ret.before.whitespace.includes("\n")) {
            ret = produce(ret, draft => {
                draft.before.whitespace = this.combineIndent(draft.before.whitespace, containerElementsIndent);
            });
        }

        // Handle the closing delimiter: last element's after space
        // Use replaceLastWhitespace to only change the final indent (preserving comment indents)
        if (ret.elements.length > 0) {
            const lastElement = ret.elements[ret.elements.length - 1];
            const effectiveLastWs = lastWhitespace(lastElement.after);
            if (effectiveLastWs.includes("\n")) {
                ret = produce(ret, draft => {
                    const lastDraft = draft.elements[draft.elements.length - 1];
                    lastDraft.after = replaceLastWhitespace(lastDraft.after, (ws: string) => {
                        return this.combineIndent(ws, containerParentMyIndent);
                    });
                });
            }
        }

        return ret;
    }

    /**
     * Handle LeftPadded elements (e.g., initializers).
     * Respects parent's indentKind.
     */
    public async visitLeftPadded<T extends J | J.Space | number | string | boolean>(
        left: J.LeftPadded<T>,
        p: P
    ): Promise<J.LeftPadded<T> | undefined> {
        const ret = await super.visitLeftPadded(left, p);
        if (ret === undefined) {
            return ret;
        }

        // Skip modification for LeftPadded inside object literals
        if (this.isInsideObjectLiteral()) {
            return ret;
        }

        // Apply continuation indent to LeftPadded.before if it has a newline
        if (ret.before.whitespace.includes("\n")) {
            const parentMyIndent = this.getParentMyIndent();
            const continuationIndent = parentMyIndent + this.singleIndent;
            return produce(ret, draft => {
                draft.before.whitespace = this.combineIndent(draft.before.whitespace, continuationIndent);
            });
        }

        return ret;
    }

    // ========== UTILITY METHODS ==========

    override async visitSpace(space: J.Space, p: P): Promise<J.Space> {
        const ret = await super.visitSpace(space, p);

        // Track if any space inside a JSX tag has a newline
        if (space.whitespace.includes("\n")) {
            let parentCursor = this.cursor.parent;
            while (parentCursor != null && parentCursor.value.kind !== JS.Kind.JsxTag) {
                parentCursor = parentCursor.parent;
            }
            if (parentCursor && parentCursor.value.kind === JS.Kind.JsxTag) {
                parentCursor.messages.set("jsxTagWithNewline", true);
            }
        }
        return ret;
    }

    async visit<R extends J>(tree: Tree, p: P, parent?: Cursor): Promise<R | undefined> {
        if (this.cursor?.getNearestMessage("stop") != null) {
            return tree as R;
        }

        // Initialize indent context when called with an explicit parent cursor
        if (parent) {
            this.cursor = new Cursor(tree, parent);
            this.initializeIndentFromCursor();
        }

        return await super.visit(tree, p) as R;
    }

    private initializeIndentFromCursor(): void {
        // When formatting a subtree, we need to:
        // 1. Walk UP to find an ancestor with a newline in its whitespace
        // 2. Extract the indent from that whitespace
        // 3. Find the nearest J node at or above that point to establish context
        // 4. Set up that J node with the CORRECT indent (accounting for where we found the newline)
        // 5. Walk DOWN from that J node to set up cursor messages for intermediate J nodes

        // Step 1: Walk up and collect the path, find ancestor with newline
        const path: Cursor[] = [];
        let cursorWithNewline: Cursor | undefined;
        let extractedIndent = "";
        let foundInWrapper = false;

        for (let c: Cursor | undefined = this.cursor.parent; c; c = c.parent) {
            path.push(c);
            const v = c.value;

            // CompilationUnit is the root - use it as anchor with empty indent
            if (v.kind === JS.Kind.CompilationUnit) {
                extractedIndent = "";
                cursorWithNewline = c;
                foundInWrapper = false;
                break;
            }

            let space: J.Space | undefined;
            if (v.kind === J.Kind.RightPadded) {
                space = v.after;
            } else if (v.kind === J.Kind.LeftPadded || v.kind === J.Kind.Container) {
                space = v.before;
            } else if (isJava(v) || isJavaScript(v)) {
                space = v.prefix;
            }

            if (space) {
                const ws = lastWhitespace(space);
                const idx = ws.lastIndexOf('\n');
                if (idx !== -1) {
                    extractedIndent = ws.substring(idx + 1);
                    cursorWithNewline = c;
                    foundInWrapper = !(isJava(v) || isJavaScript(v));
                    break;
                }
            }
        }

        if (!cursorWithNewline) {
            // No newline found in parent chain - likely top-level, nothing to initialize
            return;
        }

        // Step 2: Find the nearest J node at or above the cursor with newline
        // Also continue adding to path so we have the full path from J node down
        let jNodeCursor = cursorWithNewline;
        if (foundInWrapper) {
            // Found newline in a wrapper (Container.before, etc.) - walk up to find containing J node
            for (let c = cursorWithNewline.parent; c; c = c.parent) {
                path.push(c);  // Add to path so we have full path from J node
                if (this.isActualJNode(c.value)) {
                    jNodeCursor = c;
                    break;
                }
            }
        }

        // Step 3: Reverse the path so we go from ancestor down to parent of current element
        path.reverse();

        // Step 4: Set up the J node with the correct indent
        // KEY INSIGHT: If we found the newline in a wrapper (like Container.before inside a Block),
        // the extractedIndent is the indent of ITEMS in that container, not the Block itself.
        // Since Block has indentKind='block', it will add singleIndent to get child indent.
        // So we need to set Block's myIndent = extractedIndent - singleIndent.
        const jNodeValue = jNodeCursor.value;
        let myIndentForJNode = extractedIndent;

        if (foundInWrapper && (jNodeValue.kind === J.Kind.Block || jNodeValue.kind === J.Kind.Case)) {
            // The extracted indent is for items INSIDE the block/case, not the block itself
            // Subtract one indent level so that children get the correct indent
            if (extractedIndent.length >= this.singleIndent.length) {
                myIndentForJNode = extractedIndent.slice(0, -this.singleIndent.length);
            } else {
                myIndentForJNode = "";
            }
        }

        jNodeCursor.messages.set("myIndent", myIndentForJNode);
        if (jNodeValue.kind === J.Kind.Block || jNodeValue.kind === J.Kind.Case) {
            jNodeCursor.messages.set("indentKind", 'block');
        } else if (jNodeValue.kind === JS.Kind.CompilationUnit) {
            // CompilationUnit uses 'align' - children inherit indent directly
            jNodeCursor.messages.set("indentKind", 'align');
        } else {
            jNodeCursor.messages.set("indentKind", 'continuation');
        }

        // Step 5: Walk down from the J node, setting up any J nodes in between
        const jNodeIndex = path.indexOf(jNodeCursor);
        for (let i = jNodeIndex + 1; i < path.length; i++) {
            const c = path[i];
            const v = c.value;

            if (this.isActualJNode(v)) {
                // J node - set up myIndent and indentKind
                this.setupCursorMessagesForTree(c, v);
            }
            // Wrapper types don't need indentKind - they're transparent
        }
    }

    /**
     * Check if value is an actual J/JS node (not a wrapper like Container, LeftPadded, RightPadded).
     */
    private isActualJNode(v: any): boolean {
        if (!(isJava(v) || isJavaScript(v))) {
            return false;
        }
        // Exclude wrapper types
        return v.kind !== J.Kind.Container &&
               v.kind !== J.Kind.LeftPadded &&
               v.kind !== J.Kind.RightPadded;
    }

    private combineIndent(oldWs: string, newIndent: string): string {
        const lastNewline = oldWs.lastIndexOf("\n");
        return oldWs.substring(0, lastNewline + 1) + newIndent;
    }
}
