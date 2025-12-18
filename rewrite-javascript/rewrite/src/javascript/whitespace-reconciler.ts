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
import {JavaScriptVisitor} from './visitor';
import {J} from '../java';
import {Cursor, Tree} from "../tree";
import {produce} from "immer";

/**
 * A visitor that reconciles whitespace from a formatted tree into the original tree.
 * Walks both trees in parallel and copies whitespace (prefix, before, after) from
 * the formatted tree to the original.
 *
 * The result preserves the original AST's structure, types, and markers while
 * applying the formatted tree's whitespace.
 *
 * When a target subtree is specified, the reconciler only applies whitespace changes
 * to that subtree and its descendants, leaving surrounding code unchanged.
 */
export class WhitespaceReconcilerVisitor extends JavaScriptVisitor<J> {
    /**
     * Cursor tracking the current position in the formatted tree.
     */
    protected formattedCursor?: Cursor;

    /**
     * Flag indicating whether the trees have compatible structure.
     */
    protected compatible: boolean = true;

    /**
     * The ID of the subtree to reconcile. If undefined, reconcile everything.
     */
    private targetSubtreeId?: string;

    /**
     * Tracks the reconciliation state:
     * - 'searching': Walking but not yet inside target subtree
     * - 'reconciling': Inside target subtree, applying changes
     * - 'done': Exited target subtree, no more changes
     */
    private reconcileState: 'searching' | 'reconciling' | 'done' = 'reconciling';

    /**
     * Reconciles whitespace from a formatted tree into the original tree.
     *
     * @param original The original tree (with types, markers, etc.)
     * @param formatted The formatted tree (with desired whitespace)
     * @param targetSubtree Optional subtree to limit reconciliation to. If provided,
     *                      only this subtree and its descendants will have whitespace applied.
     * @returns The original tree with whitespace from the formatted tree
     */
    async reconcile(original: J, formatted: J, targetSubtree?: J): Promise<J> {
        this.compatible = true;
        this.formattedCursor = undefined;
        this.cursor = new Cursor(undefined, undefined);
        this.targetSubtreeId = targetSubtree?.id;
        this.reconcileState = targetSubtree ? 'searching' : 'reconciling';

        const result = await this.visit(original, formatted);
        return result ?? original;
    }

    /**
     * Check if we should apply whitespace changes at the current position.
     */
    private shouldReconcile(): boolean {
        return this.reconcileState === 'reconciling';
    }

    /**
     * Marks structure as incompatible and returns the original unchanged.
     */
    protected structureMismatch<T>(t: T): T {
        this.compatible = false;
        return t;
    }

    /**
     * Copies a Space from formatted tree. This is where the actual whitespace
     * reconciliation happens.
     *
     * Preserves the original whitespace if the formatted version would remove newlines,
     * as this indicates a structural change (e.g., collapsing multi-line to single-line).
     */
    protected reconcileSpace(original: J.Space, formatted: J.Space): J.Space {
        // Only apply whitespace changes when inside the target subtree
        if (!this.shouldReconcile()) {
            return original;
        }

        // Preserve original if formatted would remove newlines (structural change)
        const originalHasNewline = original.whitespace.includes('\n');
        const formattedHasNewline = formatted.whitespace.includes('\n');
        if (originalHasNewline && !formattedHasNewline) {
            return original;
        }

        // Return the formatted space - this is the core of reconciliation
        return formatted;
    }

    /**
     * Visit a property value, handling all the different types appropriately.
     */
    protected async visitProperty(original: any, formatted: any): Promise<any> {
        // Handle null/undefined
        if (original == null || formatted == null) {
            if (original !== formatted) {
                return this.structureMismatch(original);
            }
            return original;
        }

        const kind = (original as any).kind;

        // Space - copy from formatted
        if (kind === J.Kind.Space) {
            return this.reconcileSpace(original, formatted);
        }

        // Type nodes - short-circuit, keep original (types don't have whitespace)
        if (typeof kind === 'string' && kind.startsWith('org.openrewrite.java.tree.JavaType$')) {
            return original;
        }

        // RightPadded wrapper
        if (kind === J.Kind.RightPadded) {
            return this.visitRightPaddedReconcile(original, formatted);
        }

        // LeftPadded wrapper
        if (kind === J.Kind.LeftPadded) {
            return this.visitLeftPaddedReconcile(original, formatted);
        }

        // Container wrapper
        if (kind === J.Kind.Container) {
            return this.visitContainerReconcile(original, formatted);
        }

        // Tree node (has a kind property)
        if (kind !== undefined && typeof kind === 'string') {
            return this.visit(original, formatted);
        }

        // Primitive values - return original unchanged
        return original;
    }

    /**
     * Visit all properties of an element and copy prefix from formatted.
     */
    protected async visitElement<T extends J>(original: T, formatted: T): Promise<T> {
        if (!this.compatible) return original;

        // Check if kinds match
        if (original.kind !== formatted.kind) {
            return this.structureMismatch(original);
        }

        // Start with original, will copy prefix
        let result = original;

        // Copy prefix from formatted only when reconciling, and preserve original if newlines would be removed
        if (this.shouldReconcile() && 'prefix' in original && 'prefix' in formatted) {
            const originalPrefix = (original as any).prefix as J.Space;
            const formattedPrefix = (formatted as any).prefix as J.Space;
            const originalHasNewline = originalPrefix.whitespace.includes('\n');
            const formattedHasNewline = formattedPrefix.whitespace.includes('\n');
            if (!originalHasNewline || formattedHasNewline) {
                result = produce(result, (draft: any) => {
                    draft.prefix = formattedPrefix;
                });
            }
        }

        // Visit all child properties
        for (const key of Object.keys(original)) {
            // Skip: kind, id, markers, prefix (already copied), type properties
            if (key === 'kind' || key === 'id' || key === 'markers' || key === 'prefix' ||
                key === 'type' || key === 'fieldType' || key === 'variableType' ||
                key === 'methodType' || key === 'constructorType') {
                continue;
            }

            const originalValue = (original as any)[key];
            const formattedValue = (formatted as any)[key];

            // Handle arrays
            if (Array.isArray(originalValue)) {
                if (!Array.isArray(formattedValue) || originalValue.length !== formattedValue.length) {
                    return this.structureMismatch(original);
                }

                const newArray: any[] = [];
                let changed = false;
                for (let i = 0; i < originalValue.length; i++) {
                    const visited = await this.visitProperty(originalValue[i], formattedValue[i]);
                    if (!this.compatible) return original;
                    newArray.push(visited);
                    if (visited !== originalValue[i]) {
                        changed = true;
                    }
                }

                if (changed) {
                    result = produce(result, (draft: any) => {
                        draft[key] = newArray;
                    });
                }
            } else {
                // Visit the property
                const visited = await this.visitProperty(originalValue, formattedValue);
                if (!this.compatible) return original;

                if (visited !== originalValue) {
                    result = produce(result, (draft: any) => {
                        draft[key] = visited;
                    });
                }
            }
        }

        return result;
    }

    /**
     * Override visit to use visitElement for all nodes.
     */
    override async visit<R extends J>(tree: Tree, p: J, parent?: Cursor): Promise<R | undefined> {
        if (!this.compatible) return tree as R;

        const original = tree as J;
        const formatted = p;

        // Check kind match
        if (original.kind !== formatted.kind) {
            return this.structureMismatch(original) as R;
        }

        // Track entering the target subtree
        const isTargetSubtree = this.targetSubtreeId !== undefined && original.id === this.targetSubtreeId;
        const previousState = this.reconcileState;
        if (isTargetSubtree && this.reconcileState === 'searching') {
            this.reconcileState = 'reconciling';
        }

        // Update formattedCursor
        const savedFormattedCursor = this.formattedCursor;
        this.formattedCursor = new Cursor(formatted, this.formattedCursor);

        try {
            const result = await this.visitElement(original, formatted);
            return result as R;
        } finally {
            this.formattedCursor = savedFormattedCursor;
            // Track exiting the target subtree
            if (isTargetSubtree && previousState === 'searching') {
                this.reconcileState = 'done';
            }
        }
    }

    /**
     * Reconcile RightPadded - copy 'after' whitespace.
     */
    protected async visitRightPaddedReconcile<T extends J | boolean>(
        original: J.RightPadded<T>,
        formatted: J.RightPadded<T>
    ): Promise<J.RightPadded<T>> {
        if (!this.compatible) return original;

        if ((formatted as any).kind !== J.Kind.RightPadded) {
            return this.structureMismatch(original);
        }

        // Visit the element
        const visitedElement = await this.visitProperty(original.element, formatted.element);
        if (!this.compatible) return original;

        // Copy 'after' whitespace only when reconciling, and preserve original if newlines would be removed
        return produce(original, draft => {
            (draft as any).element = visitedElement;
            if (this.shouldReconcile()) {
                const originalHasNewline = original.after.whitespace.includes('\n');
                const formattedHasNewline = formatted.after.whitespace.includes('\n');
                if (!originalHasNewline || formattedHasNewline) {
                    draft.after = formatted.after;
                }
            }
        });
    }

    /**
     * Reconcile LeftPadded - copy 'before' whitespace.
     */
    protected async visitLeftPaddedReconcile<T extends J | J.Space | number | string | boolean>(
        original: J.LeftPadded<T>,
        formatted: J.LeftPadded<T>
    ): Promise<J.LeftPadded<T>> {
        if (!this.compatible) return original;

        if ((formatted as any).kind !== J.Kind.LeftPadded) {
            return this.structureMismatch(original);
        }

        // Visit the element
        const visitedElement = await this.visitProperty(original.element, formatted.element);
        if (!this.compatible) return original;

        // Copy 'before' whitespace only when reconciling, and preserve original if newlines would be removed
        return produce(original, draft => {
            (draft as any).element = visitedElement;
            if (this.shouldReconcile()) {
                const originalHasNewline = original.before.whitespace.includes('\n');
                const formattedHasNewline = formatted.before.whitespace.includes('\n');
                if (!originalHasNewline || formattedHasNewline) {
                    draft.before = formatted.before;
                }
            }
        });
    }

    /**
     * Reconcile Container - copy 'before' whitespace and visit elements.
     */
    protected async visitContainerReconcile<T extends J>(
        original: J.Container<T>,
        formatted: J.Container<T>
    ): Promise<J.Container<T>> {
        if (!this.compatible) return original;

        if ((formatted as any).kind !== J.Kind.Container) {
            return this.structureMismatch(original);
        }

        // Check array length
        if (original.elements.length !== formatted.elements.length) {
            return this.structureMismatch(original);
        }

        // Visit each element
        const newElements: J.RightPadded<T>[] = [];
        for (let i = 0; i < original.elements.length; i++) {
            const visited = await this.visitRightPaddedReconcile(
                original.elements[i],
                formatted.elements[i]
            );
            if (!this.compatible) return original;
            newElements.push(visited);
        }

        // Copy 'before' whitespace only when reconciling, and preserve original if newlines would be removed
        return produce(original, draft => {
            if (this.shouldReconcile()) {
                const originalHasNewline = original.before.whitespace.includes('\n');
                const formattedHasNewline = formatted.before.whitespace.includes('\n');
                if (!originalHasNewline || formattedHasNewline) {
                    draft.before = formatted.before;
                }
            }
            (draft as any).elements = newElements;
        });
    }
}
