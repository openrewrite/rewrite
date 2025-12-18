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
     * The subtree to reconcile (by reference). If undefined, reconcile everything.
     * Can be a J node, RightPadded, LeftPadded, or Container.
     */
    private targetSubtree?: any;

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
     * @param targetSubtree Optional subtree to limit reconciliation to (by reference).
     *                      Can be a J node, RightPadded, LeftPadded, or Container.
     *                      If provided, only this subtree and its descendants will have
     *                      whitespace and markers applied.
     * @returns The original tree with whitespace from the formatted tree
     */
    async reconcile(original: J, formatted: J, targetSubtree?: any): Promise<J> {
        this.compatible = true;
        this.formattedCursor = undefined;
        this.cursor = new Cursor(undefined, undefined);
        this.targetSubtree = targetSubtree;
        this.reconcileState = targetSubtree ? 'searching' : 'reconciling';

        const result = await this.visit(original, formatted);
        return result ?? original;
    }

    /**
     * Returns whether the reconciliation was successful (structures were compatible).
     */
    isCompatible(): boolean {
        return this.compatible;
    }

    /**
     * Override visit for J nodes. Updates the formatted cursor.
     * Note: Target subtree tracking is handled in visitProperty.
     */
    override async visit<R extends J>(tree: Tree, p: J, _parent?: Cursor): Promise<R | undefined> {
        if (!this.compatible) return tree as R;

        // Update formattedCursor
        const savedFormattedCursor = this.formattedCursor;
        this.formattedCursor = new Cursor(p, this.formattedCursor);

        try {
            const result = await this.visitNode(tree, p);
            return result as R;
        } finally {
            this.formattedCursor = savedFormattedCursor;
        }
    }

    /**
     * Marks structure as incompatible and returns the original unchanged.
     */
    protected structureMismatch<T>(t: T): T {
        this.compatible = false;
        return t;
    }

    /**
     * Visit a property value, handling all the different types appropriately.
     * This is the central entry point for visiting any node, including wrappers.
     */
    protected async visitProperty(original: any, formatted: any): Promise<any> {
        // Handle null/undefined
        if (original == null || formatted == null) {
            if (original !== formatted) {
                return this.structureMismatch(original);
            }
            return original;
        }

        const kind = original.kind;

        // Type nodes - short-circuit, keep original (types don't have whitespace)
        if (typeof kind === 'string' && kind.startsWith('org.openrewrite.java.tree.JavaType$')) {
            return original;
        }

        // Primitive values or non-tree objects - return original unchanged
        if (kind === undefined || typeof kind !== 'string') {
            return original;
        }

        // Space nodes - copy when reconciling, don't recurse
        if (kind === J.Kind.Space) {
            return this.shouldReconcile() ? formatted : original;
        }

        // Track entering target subtree (using referential equality)
        const isTargetSubtree = this.targetSubtree !== undefined && original === this.targetSubtree;
        const previousState = this.reconcileState;
        if (isTargetSubtree && this.reconcileState === 'searching') {
            this.reconcileState = 'reconciling';
        }

        try {
            // All tree nodes (J, RightPadded, LeftPadded, Container) go through visitNode
            return await this.visitNode(original, formatted);
        } finally {
            // Track exiting the target subtree
            if (isTargetSubtree && previousState === 'searching') {
                this.reconcileState = 'done';
            }
        }
    }

    /**
     * Visit all properties of a tree node (J, RightPadded, LeftPadded, Container).
     * Copies Space values and markers when reconciling, visits everything else.
     */
    protected async visitNode(original: any, formatted: any): Promise<any> {
        if (!this.compatible) {
            return original;
        }

        // Check if kinds match
        if (original.kind !== formatted.kind) {
            return this.structureMismatch(original);
        }

        let result = original;

        // Visit all properties
        for (const key of Object.keys(original)) {
            // Skip: kind, id, type properties
            if (key === 'kind' || key === 'id' ||
                key === 'type' || key === 'fieldType' || key === 'variableType' ||
                key === 'methodType' || key === 'constructorType') {
                continue;
            }

            const originalValue = (original as any)[key];
            const formattedValue = (formatted as any)[key];

            // Space values and markers: copy from formatted when reconciling
            if (originalValue?.kind === J.Kind.Space || key === 'markers') {
                if (this.shouldReconcile() && formattedValue !== originalValue) {
                    result = produce(result, (draft: any) => {
                        draft[key] = formattedValue;
                    });
                }
                continue;
            }

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
     * Check if we should apply whitespace changes at the current position.
     */
    private shouldReconcile(): boolean {
        return this.reconcileState === 'reconciling';
    }
}
