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
import {JS, JSX} from "./tree";
import {JavaScriptVisitor} from "./visitor";
import {
    isJava,
    isSpace,
    J,
    lastWhitespace,
    normalizeSpaceIndent,
    replaceIndentAfterLastNewline,
    replaceLastWhitespace,
    spaceContainsNewline,
    stripLeadingIndent
} from "../java";
import {produce} from "immer";
import {Cursor, isScope, isTree, Tree} from "../tree";
import {mapAsync} from "../util";
import {produceAsync} from "../visitor";
import {TabsAndIndentsStyle} from "./style";
import {findMarker} from "../markers";

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
            // If Binary has newline OR parent is in align mode, children align
            const shouldAlign = hasNewline || parentIndentKind === 'align';
            cursor.messages.set("indentKind", shouldAlign ? 'align' : 'continuation');
        } else {
            cursor.messages.set("indentKind", this.computeIndentKind(tree));
        }
    }

    private getParentIndentContext(cursor: Cursor): [string, IndentKind] {
        // Find the nearest myIndent and the nearest indentKind separately
        // Container/RightPadded/LeftPadded inherit myIndent but don't have indentKind
        let parentIndent: string | undefined;
        let parentKind: IndentKind | undefined;

        for (let c = cursor.parent; c != null; c = c.parent) {
            if (parentIndent === undefined) {
                const indent = c.messages.get("myIndent") as string | undefined;
                if (indent !== undefined) {
                    parentIndent = indent;
                }
            }

            if (parentKind === undefined) {
                const kind = c.messages.get("indentKind") as IndentKind | undefined;
                if (kind !== undefined) {
                    parentKind = kind;
                }
            }

            // Found both, we can stop
            if (parentIndent !== undefined && parentKind !== undefined) {
                break;
            }
        }

        return [parentIndent ?? "", parentKind ?? 'continuation'];
    }

    private computeMyIndent(tree: J, parentMyIndent: string, parentIndentKind: IndentKind): string {
        // CompilationUnit is the root - it always has myIndent="" regardless of prefix content
        if (tree.kind === JS.Kind.CompilationUnit) {
            return "";
        }
        // TemplateExpressionSpan: reset indent context - template literal content determines its own indentation
        // The expression inside ${...} should be indented based on where it appears in the template, not outer code
        if (tree.kind === JS.Kind.TemplateExpressionSpan) {
            // Extract base indent from the expression's prefix whitespace (after the last newline)
            const span = tree as JS.TemplateExpression.Span;
            const prefix = span.expression?.prefix?.whitespace ?? "";
            const lastNewline = prefix.lastIndexOf("\n");
            if (lastNewline >= 0) {
                return prefix.slice(lastNewline + 1);
            }
            return "";
        }
        if (tree.kind === J.Kind.IfElse || parentIndentKind === 'align') {
            return parentMyIndent;
        }
        // Only add indent if this element starts on a new line
        // Check both the element's prefix and any Spread marker's prefix
        const hasNewline = this.prefixContainsNewline(tree);
        if (!hasNewline) {
            // Special case for JSX: children of JsxTag don't have newlines in their prefix
            // (newlines are in text Literal nodes), but nested tags should still get block indent
            if (this.isNestedJsxTag(tree)) {
                return parentMyIndent + this.singleIndent;
            }
            return parentMyIndent;
        }
        // Add indent for block children or continuation
        return parentMyIndent + this.singleIndent;
    }

    private prefixContainsNewline(tree: J): boolean {
        // Check the element's own prefix
        if (tree.prefix?.whitespace?.includes("\n") ||
            tree.prefix?.comments?.some(c => c.suffix.includes("\n"))) {
            return true;
        }
        // For elements with Spread marker, check the Spread marker's prefix
        const spreadMarker = tree.markers?.markers?.find(m => m.kind === JS.Markers.Spread) as { prefix: J.Space } | undefined;
        if (spreadMarker && spaceContainsNewline(spreadMarker.prefix)) {
            return true;
        }
        return false;
    }

    private isNestedJsxTag(tree: J): boolean {
        // Check if this is a JsxTag whose parent is also a JsxTag
        if (tree.kind !== JS.Kind.JsxTag) {
            return false;
        }
        const parentTree = this.cursor.parentTree();
        return parentTree !== undefined && parentTree.value.kind === JS.Kind.JsxTag;
    }

    private computeIndentKind(tree: J): IndentKind {
        switch (tree.kind) {
            case J.Kind.Block:
            case J.Kind.Case:
            case JS.Kind.JsxTag:
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

        // Check if the element has a Spread marker - if so, normalize its prefix instead
        const spreadMarker = result.markers?.markers?.find(m => m.kind === JS.Markers.Spread) as { prefix: J.Space } | undefined;
        if (spreadMarker && spaceContainsNewline(spreadMarker.prefix)) {
            const normalizedPrefix = normalizeSpaceIndent(spreadMarker.prefix, myIndent);
            if (normalizedPrefix !== spreadMarker.prefix) {
                result = produce(result, draft => {
                    const spreadIdx = draft.markers.markers.findIndex(m => m.kind === JS.Markers.Spread);
                    if (spreadIdx !== -1) {
                        (draft.markers.markers[spreadIdx] as any).prefix = normalizedPrefix;
                    }
                });
            }
        } else if (result.prefix && spaceContainsNewline(result.prefix)) {
            // Normalize the entire prefix space including comment suffixes
            const normalizedPrefix = normalizeSpaceIndent(result.prefix, myIndent);
            if (normalizedPrefix !== result.prefix) {
                result = produce(result, draft => {
                    draft.prefix = normalizedPrefix;
                });
            }
        }

        if (result.kind === J.Kind.Block) {
            result = this.normalizeBlockEnd(result as J.Block, myIndent);
        } else if (result.kind === J.Kind.Literal && this.isInsideJsxTag()) {
            result = this.normalizeJsxTextContent(result as J.Literal, myIndent);
        }

        return result;
    }

    private isInsideJsxTag(): boolean {
        const parentTree = this.cursor.parentTree();
        return parentTree !== undefined && parentTree.value.kind === JS.Kind.JsxTag;
    }

    private normalizeJsxTextContent(literal: J.Literal, myIndent: string): J.Literal {
        if (!literal.valueSource || !literal.valueSource.includes("\n")) {
            return literal;
        }

        // Check if this literal is the last child of a JsxTag - if so, its trailing whitespace
        // should use the parent tag's indent, not the content indent
        const parentIndent = this.cursor.parentTree()!.messages.get("myIndent") as string | undefined;
        const isLastChild = parentIndent !== undefined && this.isLastChildOfJsxTag(literal);

        // For JSX text content, the newline is in the value, not the prefix.
        // Since the content IS effectively on a new line, it should get block child indent.
        // myIndent is the parent's indent (because Literal prefix has no newline),
        // so we need to add singleIndent for content lines.
        const contentIndent = myIndent + this.singleIndent;

        // Split by newlines and normalize each line's indentation
        const lines = literal.valueSource.split('\n');
        const result: string[] = [];

        for (let i = 0; i < lines.length; i++) {
            if (i === 0) {
                // Content before first newline stays as-is
                result.push(lines[i]);
                continue;
            }

            const content = stripLeadingIndent(lines[i]);

            if (content === '') {
                // Line has only whitespace (or is empty)
                if (isLastChild && i === lines.length - 1) {
                    // Trailing whitespace of last child - use parent indent for closing tag alignment
                    result.push(parentIndent!);
                } else if (i < lines.length - 1) {
                    // Empty line in the middle (followed by more lines) - keep empty
                    result.push('');
                } else {
                    // Trailing whitespace of non-last-child - add content indent
                    result.push(contentIndent);
                }
            } else {
                // Line has content - add proper indent
                result.push(contentIndent + content);
            }
        }

        const normalizedValueSource = result.join('\n');
        if (normalizedValueSource === literal.valueSource) {
            return literal;
        }
        return produce(literal, draft => {
            draft.valueSource = normalizedValueSource;
        });
    }

    private isLastChildOfJsxTag(literal: J.Literal): boolean {
        const parentCursor = this.cursor.parentTree();
        if (parentCursor && parentCursor.value.kind === JS.Kind.JsxTag) {
            const tag = parentCursor.value as JSX.Tag;
            if (tag.children && tag.children.length > 0) {
                const lastChild = tag.children[tag.children.length - 1];
                // Compare by id since object references might differ after transformations
                return lastChild.kind === J.Kind.Literal && (lastChild as J.Literal).id === literal.id;
            }
        }
        return false;
    }

    private normalizeBlockEnd(block: J.Block, myIndent: string): J.Block {
        const effectiveLastWs = lastWhitespace(block.end);
        if (!effectiveLastWs.includes("\n")) {
            return block;
        }
        return produce(block, draft => {
            draft.end = replaceLastWhitespace(draft.end, ws => replaceIndentAfterLastNewline(ws, myIndent));
        });
    }

    public async visitContainer<T extends J>(container: J.Container<T>, p: P): Promise<J.Container<T>> {
        // Create cursor for this container
        this.cursor = new Cursor(container, this.cursor);

        // Pre-visit hook: set up cursor messages
        this.preVisitContainer(container);

        // Visit children (similar to base visitor but without cursor management)
        let ret = (await produceAsync<J.Container<T>>(container, async draft => {
            draft.before = await this.visitSpace(container.before, p);
            (draft.elements as J.RightPadded<J>[]) = await mapAsync(container.elements, e => this.visitRightPadded(e, p));
            draft.markers = await this.visitMarkers(container.markers, p);
        }))!;

        // Post-visit hook: normalize indentation
        ret = this.postVisitContainer(ret);

        // Restore cursor
        this.cursor = this.cursor.parent!;

        return ret;
    }

    private preVisitContainer<T extends J>(container: J.Container<T>): void {
        let myIndent = this.cursor.parent?.messages.get("myIndent") as string ?? "";

        // Check if we're in a method chain - use chainedIndent if available
        // This ensures arguments inside method chains like `.select(arg)` inherit the chain's indent level
        // BUT stop at scope boundaries:
        // 1. Blocks - nested code inside callbacks should NOT use outer chainedIndent
        // 2. Other Containers - nested function call arguments should NOT use outer chainedIndent
        for (let c = this.cursor.parent; c; c = c.parent) {
            // Stop searching if we hit a Block (function body, arrow function body, etc.)
            // This prevents chainedIndent from leaking into nested scopes
            if (c.value?.kind === J.Kind.Block) {
                break;
            }
            // Stop searching if we hit another Container (arguments of another function call)
            // This prevents chainedIndent from leaking into nested function calls
            if (c.value?.kind === J.Kind.Container) {
                break;
            }
            const chainedIndent = c.messages.get("chainedIndent") as string | undefined;
            if (chainedIndent !== undefined) {
                myIndent = chainedIndent;
                break;
            }
        }

        this.cursor.messages.set("myIndent", myIndent);
        this.cursor.messages.set("indentKind", 'continuation');
    }

    private postVisitContainer<T extends J>(container: J.Container<T>): J.Container<T> {
        let parentIndent = this.cursor.parent?.messages.get("myIndent") as string ?? "";

        // Check for chainedIndent for closing delimiter alignment in method chains
        // BUT stop at scope boundaries:
        // 1. Blocks - nested code inside callbacks should NOT use outer chainedIndent
        // 2. Other Containers - nested function call arguments should NOT use outer chainedIndent
        for (let c = this.cursor.parent; c; c = c.parent) {
            // Stop searching if we hit a Block (function body, arrow function body, etc.)
            if (c.value?.kind === J.Kind.Block) {
                break;
            }
            // Stop searching if we hit another Container (arguments of another function call)
            if (c.value?.kind === J.Kind.Container) {
                break;
            }
            const chainedIndent = c.messages.get("chainedIndent") as string | undefined;
            if (chainedIndent !== undefined) {
                parentIndent = chainedIndent;
                break;
            }
        }

        // Normalize the last element's after whitespace (closing delimiter like `)`)
        // The closing delimiter should align with the parent's indent level
        if (container.elements.length > 0) {
            const effectiveLastWs = lastWhitespace(container.elements[container.elements.length - 1].after);
            if (effectiveLastWs.includes("\n")) {
                return produce(container, draft => {
                    const lastDraft = draft.elements[draft.elements.length - 1];
                    lastDraft.after = replaceLastWhitespace(lastDraft.after, ws => replaceIndentAfterLastNewline(ws, parentIndent));
                });
            }
        }

        return container;
    }

    public async visitLeftPadded<T extends J | J.Space | number | string | boolean>(
        left: J.LeftPadded<T>,
        p: P
    ): Promise<J.LeftPadded<T> | undefined> {
        // Create cursor for this LeftPadded
        this.cursor = new Cursor(left, this.cursor);

        // Pre-visit hook: set up cursor messages
        this.preVisitLeftPadded(left);

        // Visit children (similar to base visitor but without cursor management)
        let ret = await produceAsync<J.LeftPadded<T>>(left, async draft => {
            draft.before = await this.visitSpace(left.before, p);
            if (isTree(left.element)) {
                (draft.element as J) = await this.visitDefined(left.element, p);
            } else if (isSpace(left.element)) {
                (draft.element as J.Space) = await this.visitSpace(left.element, p);
            }
            draft.markers = await this.visitMarkers(left.markers, p);
        });

        // Post-visit hook: normalize indentation
        ret = this.postVisitLeftPadded(ret);

        // Restore cursor
        this.cursor = this.cursor.parent!;

        return ret;
    }

    private preVisitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>): void {
        // Get parent indent from parent cursor
        const parentIndent = this.cursor.parent?.messages.get("myIndent") as string ?? "";
        const hasNewline = left.before.whitespace.includes("\n");

        // Check if parent is a Binary in align mode - if so, don't add continuation indent
        // (Binary sets indentKind='align' when it's already on a continuation line)
        const parentValue = this.cursor.parent?.value;
        const parentIndentKind = this.cursor.parent?.messages.get("indentKind");
        const shouldAlign = parentValue?.kind === J.Kind.Binary && parentIndentKind === 'align';

        // Compute myIndent INCLUDING continuation if applicable
        // This ensures child elements see the correct parent indent
        let myIndent = parentIndent;
        if (hasNewline && !shouldAlign) {
            myIndent = parentIndent + this.singleIndent;
        }

        this.cursor.messages.set("myIndent", myIndent);
        this.cursor.messages.set("hasNewline", hasNewline);
    }

    private postVisitLeftPadded<T extends J | J.Space | number | string | boolean>(
        left: J.LeftPadded<T> | undefined
    ): J.LeftPadded<T> | undefined {
        if (left === undefined) {
            return undefined;
        }

        const hasNewline = this.cursor.messages.get("hasNewline") as boolean ?? false;
        if (!hasNewline) {
            return left;
        }

        // Use the myIndent we computed in preVisitLeftPadded (which includes continuation if applicable)
        const targetIndent = this.cursor.messages.get("myIndent") as string ?? "";
        return produce(left, draft => {
            draft.before.whitespace = replaceIndentAfterLastNewline(draft.before.whitespace, targetIndent);
        });
    }

    public async visitRightPadded<T extends J | boolean>(
        right: J.RightPadded<T>,
        p: P
    ): Promise<J.RightPadded<T> | undefined> {
        // Create cursor for this RightPadded
        this.cursor = new Cursor(right, this.cursor);

        // Pre-visit hook: set up cursor messages, propagate continuation if after has newline
        this.preVisitRightPadded(right);

        // Visit children (similar to base visitor but without cursor management)
        let ret = await produceAsync<J.RightPadded<T>>(right, async draft => {
            if (isTree(right.element)) {
                (draft.element as J) = await this.visitDefined(right.element, p);
            }
            draft.after = await this.visitSpace(right.after, p);
            draft.markers = await this.visitMarkers(right.markers, p);
        });

        // Restore cursor
        this.cursor = this.cursor.parent!;

        if (ret?.element === undefined) {
            return undefined;
        }
        return ret;
    }

    private preVisitRightPadded<T extends J | boolean>(right: J.RightPadded<T>): void {
        // Get parent indent from parent cursor
        const parentIndent = this.cursor.parent?.messages.get("myIndent") as string ?? "";
        const parentIndentKind = this.cursor.parent?.messages.get("indentKind") as string ?? "";

        // Check if the `after` has a newline (e.g., in method chains like `db\n    .from()`)
        const hasNewline = right.after.whitespace.includes("\n");

        // Only apply chainedIndent logic for method chains (when parent is a MethodInvocation)
        const parentKind = this.cursor.parent?.value?.kind;
        const isMethodChain = parentKind === J.Kind.MethodInvocation;

        let myIndent = parentIndent;
        if (hasNewline && isMethodChain) {
            // Search up the cursor hierarchy for an existing chainedIndent (to avoid stacking in method chains)
            let existingChainedIndent: string | undefined;
            for (let c = this.cursor.parent; c; c = c.parent) {
                existingChainedIndent = c.messages.get("chainedIndent") as string | undefined;
                if (existingChainedIndent !== undefined) {
                    break;
                }
            }
            if (existingChainedIndent === undefined) {
                myIndent = parentIndent + this.singleIndent;
                // Set chainedIndent on parent so further chain elements don't stack
                this.cursor.parent?.messages.set("chainedIndent", myIndent);
            } else {
                myIndent = existingChainedIndent;
            }
        } else if (parentIndentKind !== 'align' && isJava(right.element) && this.elementPrefixContainsNewline(right.element as J)) {
            myIndent = parentIndent + this.singleIndent;
            // For spread elements with newlines, mark continuation as established
            // This allows subsequent elements on the SAME line to inherit the continuation level
            const element = right.element as J;
            if (this.isSpreadElement(element)) {
                this.cursor.parent?.messages.set("continuationIndent", myIndent);
            }
        } else if (isJava(right.element) && !this.elementPrefixContainsNewline(right.element as J)) {
            // Element has no newline - check if a previous sibling established continuation
            const continuationIndent = this.cursor.parent?.messages.get("continuationIndent") as string | undefined;
            if (continuationIndent !== undefined) {
                myIndent = continuationIndent;
            }
        }

        this.cursor.messages.set("myIndent", myIndent);
        // Set 'align' for most RightPadded elements to prevent double-continuation
        // EXCEPT when Parentheses wraps a Binary expression - in that case, the Binary's
        // children need continuation mode to get proper indentation for multi-line operands
        const rightPaddedParentKind = this.cursor.parent?.value?.kind;
        const elementKind = (right.element as any)?.kind;
        const isParenthesesWrappingBinary = rightPaddedParentKind === J.Kind.Parentheses &&
            elementKind === J.Kind.Binary;
        if (!isParenthesesWrappingBinary) {
            this.cursor.messages.set("indentKind", "align");
        }
    }

    private elementPrefixContainsNewline(element: J): boolean {
        // Check the element's own prefix
        if (lastWhitespace(element.prefix).includes("\n")) {
            return true;
        }
        // Also check for Spread marker's prefix
        const spreadMarker = element.markers?.markers?.find(m => m.kind === JS.Markers.Spread) as { prefix: J.Space } | undefined;
        if (spreadMarker && spaceContainsNewline(spreadMarker.prefix)) {
            return true;
        }
        return false;
    }

    /**
     * Check if an element is a spread - either directly marked with Spread marker,
     * or a PropertyAssignment whose name has a Spread marker (for object spreads).
     */
    private isSpreadElement(element: J): boolean {
        // Direct spread marker on element
        if (findMarker(element, JS.Markers.Spread)) {
            return true;
        }
        // PropertyAssignment wrapping a spread (for object spread like `...obj`)
        if (element.kind === JS.Kind.PropertyAssignment) {
            const propAssign = element as JS.PropertyAssignment;
            const nameElement = propAssign.name?.element;
            if (findMarker(nameElement, JS.Markers.Spread)) {
                return true;
            }
        }
        return false;
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
        return isJava(v) &&
            v.kind !== J.Kind.Container &&
            v.kind !== J.Kind.LeftPadded &&
            v.kind !== J.Kind.RightPadded;
    }
}
