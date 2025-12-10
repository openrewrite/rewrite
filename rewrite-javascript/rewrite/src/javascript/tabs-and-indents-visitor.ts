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
type IndentContext = [number, IndentKind];  // [indent, kind]
export class TabsAndIndentsVisitor<P> extends JavaScriptVisitor<P> {
    private readonly indentSize: number;
    private readonly useTabCharacter: boolean;

    constructor(private readonly tabsAndIndentsStyle: TabsAndIndentsStyle, private stopAfter?: Tree) {
        super();
        this.indentSize = this.tabsAndIndentsStyle.indentSize;
        this.useTabCharacter = this.tabsAndIndentsStyle.useTabCharacter;
    }

    private indentString(indent: number): string {
        if (this.useTabCharacter) {
            return "\t".repeat(Math.floor(indent / this.indentSize));
        }
        return " ".repeat(indent);
    }

    protected async preVisit(tree: J, _p: P): Promise<J | undefined> {
        this.setupCursorMessagesForTree(this.cursor, tree);
        return tree;
    }

    private setupCursorMessagesForTree(cursor: Cursor, tree: J): void {
        // Check if this MethodInvocation starts a method chain (select.after has newline)
        if (tree.kind === J.Kind.MethodInvocation) {
            const mi = tree as J.MethodInvocation;
            if (mi.select && mi.select.after.whitespace.includes("\n")) {
                // This MethodInvocation has a chained method call after it
                // Store the ORIGINAL parent indent context in "chainedIndentContext"
                // This will be propagated down and used when we reach the chain's innermost element
                const parentContext = this.getParentIndentContext(cursor);
                cursor.messages.set("chainedIndentContext", parentContext);
                // For children (arguments), use continuation indent
                // But the prefix will be normalized in postVisit using chainedIndentContext
                const [parentIndent, parentIndentKind] = parentContext;
                cursor.messages.set("indentContext", [parentIndent + this.indentSize, parentIndentKind] as IndentContext);
                return;
            }
            // Check if we're at the base of a chain (no select) and parent has chainedIndentContext
            if (!mi.select) {
                const chainedContext = cursor.parent?.messages.get("chainedIndentContext") as IndentContext | undefined;
                if (chainedContext !== undefined) {
                    // Consume the chainedIndentContext - this is the base of the chain
                    // The base element gets the original indent (no extra continuation)
                    // Propagate chainedIndentContext so the `name` child knows not to add indent
                    cursor.messages.set("indentContext", chainedContext);
                    cursor.messages.set("chainedIndentContext", chainedContext);
                    return;
                }
            }
        }

        // Check if we're the `name` of a MethodInvocation at the base of a chain
        if (tree.kind === J.Kind.Identifier) {
            const parentValue = cursor.parent?.value;
            if (parentValue?.kind === J.Kind.MethodInvocation) {
                const parentMi = parentValue as J.MethodInvocation;
                // Check if parent has chainedIndentContext (meaning it's at the base of a chain)
                // and we are the `name` (not an argument or something else)
                const parentChainedContext = cursor.parent?.messages.get("chainedIndentContext") as IndentContext | undefined;
                if (parentChainedContext !== undefined && !parentMi.select) {
                    // We're the name of a chain-base MethodInvocation - use the chained indent directly
                    cursor.messages.set("indentContext", parentChainedContext);
                    return;
                }
            }
        }

        const [parentMyIndent, parentIndentKind] = this.getParentIndentContext(cursor);
        const myIndent = this.computeMyIndent(tree, parentMyIndent, parentIndentKind);

        // For Binary, behavior depends on whether it's already on a continuation line
        let indentKind: IndentKind;
        if (tree.kind === J.Kind.Binary) {
            // If Binary has newline OR parent is in align mode, children align
            const hasNewline = this.prefixContainsNewline(tree);
            indentKind = (hasNewline || parentIndentKind === 'align') ? 'align' : 'continuation';
        } else {
            indentKind = this.computeIndentKind(tree);
        }

        cursor.messages.set("indentContext", [myIndent, indentKind] as IndentContext);
    }

    private getParentIndentContext(cursor: Cursor): IndentContext {
        // Walk up the cursor chain to find the nearest indent context
        // We need to walk because intermediate nodes like RightPadded may not have context set
        for (let c = cursor.parent; c != null; c = c.parent) {
            // chainedIndentContext stores the original context - prefer it
            const chainedContext = c.messages.get("chainedIndentContext") as IndentContext | undefined;
            if (chainedContext !== undefined) {
                return chainedContext;
            }

            const context = c.messages.get("indentContext") as IndentContext | undefined;
            if (context !== undefined) {
                return context;
            }
        }

        return [0, 'continuation'];
    }

    private computeMyIndent(tree: J, parentMyIndent: number, parentIndentKind: IndentKind): number {
        // CompilationUnit is the root - it always has myIndent=0 regardless of prefix content
        if (tree.kind === JS.Kind.CompilationUnit) {
            return 0;
        }
        // TemplateExpressionSpan: reset indent context - template literal content determines its own indentation
        // The expression inside ${...} should be indented based on where it appears in the template, not outer code
        if (tree.kind === JS.Kind.TemplateExpressionSpan) {
            // Extract base indent from the expression's prefix whitespace (after the last newline)
            const span = tree as JS.TemplateExpression.Span;
            const prefix = span.expression?.prefix?.whitespace ?? "";
            const lastNewline = prefix.lastIndexOf("\n");
            if (lastNewline >= 0) {
                return prefix.length - lastNewline - 1;
            }
            return 0;
        }
        if (tree.kind === J.Kind.IfElse || parentIndentKind === 'align') {
            return parentMyIndent;
        }
        // Certain structures don't add indent for themselves - they stay at parent level
        // - TryCatch: catch clause is part of try statement
        // - TypeLiteral: { members } in type definitions
        // - EnumValueSet: enum members get same indent as the set itself
        if (tree.kind === J.Kind.TryCatch || tree.kind === JS.Kind.TypeLiteral || tree.kind === J.Kind.EnumValueSet) {
            return parentMyIndent;
        }
        // Only add indent if this element starts on a new line
        // Check both the element's prefix and any Spread marker's prefix
        const hasNewline = this.prefixContainsNewline(tree);
        if (!hasNewline) {
            // Special case for JSX: children of JsxTag don't have newlines in their prefix
            // (newlines are in text Literal nodes), but JSX children should still get block indent
            if (this.isJsxChildElement(tree)) {
                return parentMyIndent + this.indentSize;
            }
            return parentMyIndent;
        }
        // Add indent for block children or continuation
        return parentMyIndent + this.indentSize;
    }

    private prefixContainsNewline(tree: J): boolean {
        // Check if the element starts on a new line (only the last whitespace matters)
        if (tree.prefix && lastWhitespace(tree.prefix).includes("\n")) {
            return true;
        }
        // For elements with Spread marker, check the Spread marker's prefix
        const spreadMarker = tree.markers?.markers?.find(m => m.kind === JS.Markers.Spread) as { prefix: J.Space } | undefined;
        if (spreadMarker && spaceContainsNewline(spreadMarker.prefix)) {
            return true;
        }
        return false;
    }

    private isJsxChildElement(tree: J): boolean {
        // Check if this is a JSX child element whose parent is a JsxTag
        // JSX children (JsxTag, JsxEmbeddedExpression) don't have newlines in their own prefix
        // (newlines are in text Literal nodes), but they should still get block indent
        if (tree.kind !== JS.Kind.JsxTag && tree.kind !== JS.Kind.JsxEmbeddedExpression) {
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
            case JS.Kind.TypeLiteral:
            case J.Kind.EnumValueSet:
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

        // If parent has chainedIndentContext but no indentContext yet, we're exiting a chain element
        // Set indentContext on parent = [chainedIndent + indentSize, chainedIndentKind]
        // This only happens once at the innermost element; subsequent parents will already have indentContext
        const parentChainedContext = this.cursor.parent?.messages.get("chainedIndentContext") as IndentContext | undefined;
        const parentHasIndentContext = this.cursor.parent?.messages.has("indentContext");
        if (parentChainedContext !== undefined && !parentHasIndentContext) {
            const [chainedIndent, chainedIndentKind] = parentChainedContext;
            this.cursor.parent!.messages.set("indentContext", [chainedIndent + this.indentSize, chainedIndentKind] as IndentContext);
        }

        const indentContext = this.cursor.messages.get("indentContext") as IndentContext | undefined;
        if (indentContext === undefined) {
            return tree;
        }
        let [myIndent] = indentContext;

        // For chain-start MethodInvocations, the prefix contains whitespace before the chain BASE
        // Use chainedIndentContext (the original indent) for the prefix, not the continuation indent
        const chainedContext = this.cursor.messages.get("chainedIndentContext") as IndentContext | undefined;
        if (chainedContext !== undefined && tree.kind === J.Kind.MethodInvocation) {
            const mi = tree as J.MethodInvocation;
            if (mi.select && mi.select.after.whitespace.includes("\n")) {
                // This is a chain-start - use original indent for prefix normalization
                myIndent = chainedContext[0];
            }
        }

        let result = tree;
        const indentStr = this.indentString(myIndent);

        // Check if the element has a Spread marker - if so, normalize its prefix instead
        const spreadMarker = result.markers?.markers?.find(m => m.kind === JS.Markers.Spread) as { prefix: J.Space } | undefined;
        if (spreadMarker && spaceContainsNewline(spreadMarker.prefix)) {
            const normalizedPrefix = normalizeSpaceIndent(spreadMarker.prefix, indentStr);
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
            const normalizedPrefix = normalizeSpaceIndent(result.prefix, indentStr);
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

    private normalizeJsxTextContent(literal: J.Literal, myIndent: number): J.Literal {
        if (!literal.valueSource || !literal.valueSource.includes("\n")) {
            return literal;
        }

        // Check if this literal is the last child of a JsxTag - if so, its trailing whitespace
        // should use the parent tag's indent, not the content indent
        const parentContext = this.cursor.parentTree()!.messages.get("indentContext") as IndentContext | undefined;
        const parentIndent = parentContext?.[0];
        const isLastChild = parentIndent !== undefined && this.isLastChildOfJsxTag(literal);

        // For JSX text content, the newline is in the value, not the prefix.
        // Since the content IS effectively on a new line, it should get block child indent.
        // myIndent is the parent's indent (because Literal prefix has no newline),
        // so we need to add indentSize for content lines.
        const contentIndent = myIndent + this.indentSize;

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
                    result.push(this.indentString(parentIndent!));
                } else if (i < lines.length - 1) {
                    // Empty line in the middle (followed by more lines) - keep empty
                    result.push('');
                } else {
                    // Trailing whitespace of non-last-child - add content indent
                    result.push(this.indentString(contentIndent));
                }
            } else {
                // Line has content - add proper indent
                result.push(this.indentString(contentIndent) + content);
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

    private normalizeBlockEnd(block: J.Block, myIndent: number): J.Block {
        const effectiveLastWs = lastWhitespace(block.end);
        if (!effectiveLastWs.includes("\n")) {
            return block;
        }
        return produce(block, draft => {
            draft.end = replaceLastWhitespace(draft.end, ws => replaceIndentAfterLastNewline(ws, this.indentString(myIndent)));
        });
    }

    public async visitContainer<T extends J>(container: J.Container<T>, p: P): Promise<J.Container<T>> {
        // Create cursor for this container
        this.cursor = new Cursor(container, this.cursor);

        // Pre-visit hook: set up cursor messages
        this.preVisitContainer();

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

    private preVisitContainer(): void {
        const parentContext = this.cursor.parent?.messages.get("indentContext") as IndentContext | undefined;
        const [myIndent] = parentContext ?? [0, 'continuation'];
        this.cursor.messages.set("indentContext", [myIndent, 'continuation'] as IndentContext);
    }

    private postVisitContainer<T extends J>(container: J.Container<T>): J.Container<T> {
        const parentContext = this.cursor.parent?.messages.get("indentContext") as IndentContext | undefined;
        const [parentIndent] = parentContext ?? [0, 'continuation'];

        // Normalize the last element's after whitespace (closing delimiter like `)`)
        // The closing delimiter should align with the parent's indent level
        if (container.elements.length > 0) {
            const effectiveLastWs = lastWhitespace(container.elements[container.elements.length - 1].after);
            if (effectiveLastWs.includes("\n")) {
                return produce(container, draft => {
                    const lastDraft = draft.elements[draft.elements.length - 1];
                    lastDraft.after = replaceLastWhitespace(lastDraft.after, ws => replaceIndentAfterLastNewline(ws, this.indentString(parentIndent)));
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
        const parentContext = this.cursor.parent?.messages.get("indentContext") as IndentContext | undefined;
        const [parentIndent, parentIndentKind] = parentContext ?? [0, 'continuation'];
        const hasNewline = left.before.whitespace.includes("\n");

        // Check if parent is a Binary in align mode - if so, don't add continuation indent
        const parentValue = this.cursor.parent?.value;
        const shouldAlign = parentValue?.kind === J.Kind.Binary && parentIndentKind === 'align';

        // Compute myIndent INCLUDING continuation if applicable
        let myIndent = parentIndent;
        if (hasNewline && !shouldAlign) {
            myIndent = parentIndent + this.indentSize;
        }

        this.cursor.messages.set("indentContext", [myIndent, 'continuation'] as IndentContext);
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

        // Use the indent we computed in preVisitLeftPadded (which includes continuation if applicable)
        const context = this.cursor.messages.get("indentContext") as IndentContext | undefined;
        const [targetIndent] = context ?? [0, 'continuation'];
        return produce(left, draft => {
            draft.before.whitespace = replaceIndentAfterLastNewline(draft.before.whitespace, this.indentString(targetIndent));
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
        const parentContext = this.cursor.parent?.messages.get("indentContext") as IndentContext | undefined;
        const [parentIndent, parentIndentKind] = parentContext ?? [0, 'continuation'];

        // Check if parent has chainedIndentContext - if so, this is the select of a method chain
        // Propagate chainedIndentContext but do NOT set indentContext
        const parentChainedContext = this.cursor.parent?.messages.get("chainedIndentContext") as IndentContext | undefined;
        if (parentChainedContext !== undefined) {
            this.cursor.messages.set("chainedIndentContext", parentChainedContext);
            // Do NOT set indentContext - child elements will use chainedIndentContext
            return;
        }

        // Check if Parentheses wraps a Binary expression - if so, let Binary handle its own indent
        const rightPaddedParentKind = this.cursor.parent?.value?.kind;
        const elementKind = (right.element as any)?.kind;
        const isParenthesesWrappingBinary = rightPaddedParentKind === J.Kind.Parentheses &&
            elementKind === J.Kind.Binary;

        // Check if this is a NamedVariable inside a VariableDeclarations with decorators
        // In this case, the newline between decorator and variable name should NOT cause continuation indent
        const isVariableAfterDecorator = elementKind === J.Kind.NamedVariable &&
            rightPaddedParentKind === J.Kind.VariableDeclarations &&
            ((this.cursor.parent?.value as J.VariableDeclarations)?.leadingAnnotations?.length ?? 0) > 0;

        let myIndent = parentIndent;
        if (!isParenthesesWrappingBinary && !isVariableAfterDecorator && parentIndentKind !== 'align' && isJava(right.element) && this.prefixContainsNewline(right.element as J)) {
            myIndent = parentIndent + this.indentSize;
            // For spread elements with newlines, mark continuation as established
            const element = right.element as J;
            if (this.isSpreadElement(element)) {
                this.cursor.parent?.messages.set("continuationIndent", myIndent);
            }
        } else if (isJava(right.element) && !this.prefixContainsNewline(right.element as J)) {
            // Element has no newline - check if a previous sibling established continuation
            const continuationIndent = this.cursor.parent?.messages.get("continuationIndent") as number | undefined;
            if (continuationIndent !== undefined) {
                myIndent = continuationIndent;
            }
        }

        // Set 'align' for most RightPadded elements to prevent double-continuation
        // EXCEPT when Parentheses wraps a Binary expression - use continuation so Binary children align
        const indentKind: IndentKind = isParenthesesWrappingBinary ? 'continuation' : 'align';
        this.cursor.messages.set("indentContext", [myIndent, indentKind] as IndentContext);
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
        let anchorIndent = 0;

        for (let c = this.cursor.parent; c; c = c.parent) {
            path.push(c);
            const v = c.value;

            if (this.isActualJNode(v) && !anchorCursor && v.prefix) {
                const ws = lastWhitespace(v.prefix);
                const idx = ws.lastIndexOf('\n');
                if (idx !== -1) {
                    anchorCursor = c;
                    anchorIndent = ws.length - idx - 1;
                }
            }

            if (v.kind === JS.Kind.CompilationUnit) {
                if (!anchorCursor) {
                    anchorCursor = c;
                    anchorIndent = 0;
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
                c.messages.set("indentContext", [anchorIndent, this.computeIndentKind(v)] as IndentContext);
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
