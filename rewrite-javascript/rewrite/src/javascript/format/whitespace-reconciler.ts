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
import {isIdentifier, isLiteral, isSpace, J, Type} from '../../java';
import {JS} from "../tree";
import {isTree} from "../../tree";

/**
 * Union type for all tree node types that the reconciler handles.
 * This includes J nodes and their wrapper types.
 */
type TreeNode = J | J.RightPadded<J> | J.LeftPadded<J> | J.Container<J> | J.Space;

/**
 * Compares two tree nodes for equality by ID.
 * Uses ID comparison if both nodes are trees with IDs, otherwise falls back to reference equality.
 * This is important because visitors may create new objects during transformation.
 */
function isSameNode(a: unknown, b: unknown): boolean {
    if (a === b) return true;
    if (a == null || b == null) return false;
    // Compare by ID if both have IDs (for tree nodes)
    if (isTree(a) && isTree(b)) {
        return a.id === b.id;
    }
    return false;
}

/**
 * Type guard to check if a value has a kind property (is a tree node or wrapper).
 */
function hasKind(value: unknown): value is { kind: string } {
    return value !== null &&
        typeof value === 'object' &&
        'kind' in value &&
        typeof (value as { kind: unknown }).kind === 'string';
}

/**
 * Tree nodes that can be visited by visitNode (excludes Space which is handled separately).
 */
type VisitableNode = Exclude<TreeNode, J.Space>;

/**
 * Type guard to check if a value is a VisitableNode (J, LeftPadded, RightPadded, or Container).
 * This is used after hasKind() to further narrow the type for visitNode().
 */
function isVisitableNode(value: { kind: string }): value is VisitableNode {
    const kind = value.kind;
    // Check for wrapper kinds first (more specific)
    if (kind === J.Kind.LeftPadded || kind === J.Kind.RightPadded || kind === J.Kind.Container) {
        return true;
    }
    // All other non-Space tree nodes with valid kind strings are J nodes
    // Space is handled separately before this check
    return kind !== J.Kind.Space && !kind.startsWith('org.openrewrite.java.tree.JavaType$');
}

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
 *
 * When stopAfter is specified, the reconciler stops applying changes after exiting
 * that node, leaving all subsequent nodes with their original whitespace.
 */
export class WhitespaceReconciler {
    /**
     * Flag indicating whether the trees have compatible structure.
     */
    private compatible: boolean = true;

    /**
     * The subtree to reconcile (by reference). If undefined, reconcile everything.
     * Can be a J node, RightPadded, LeftPadded, or Container.
     */
    private targetSubtree?: TreeNode;

    /**
     * The node to stop after (by reference). Once we exit this node, stop reconciling.
     * Can be a J node, RightPadded, LeftPadded, or Container.
     */
    private stopAfterNode?: TreeNode;

    /**
     * Tracks the reconciliation state:
     * - 'searching': Walking but not yet inside target subtree
     * - 'reconciling': Inside target subtree, applying changes
     * - 'done': Exited target subtree or stopAfter node, no more changes
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
     * @param stopAfter Optional node to stop reconciliation after (by reference).
     *                  Once we exit this node, no more changes are applied.
     * @returns The original tree with whitespace from the formatted tree
     */
    reconcile(original: J, formatted: J, targetSubtree?: TreeNode, stopAfter?: TreeNode): J {
        this.compatible = true;
        this.targetSubtree = targetSubtree;
        this.stopAfterNode = stopAfter;
        this.reconcileState = targetSubtree ? 'searching' : 'reconciling';

        // We know original and formatted are J nodes, so result will be J
        return this.visitNode(original, formatted) as J;
    }

    /**
     * Returns whether the reconciliation was successful (structures were compatible).
     */
    isCompatible(): boolean {
        return this.compatible;
    }

    /**
     * Marks structure as incompatible and returns the original unchanged.
     */
    private structureMismatch<T>(t: T): T {
        this.compatible = false;
        return t;
    }

    /**
     * Visit a property value, handling all the different types appropriately.
     * This is the central entry point for visiting any node, including wrappers.
     *
     * @returns The reconciled value (original structure with formatted whitespace)
     */
    private visitProperty(original: unknown, formatted: unknown): unknown {
        // Handle null/undefined
        if (original == null || formatted == null) {
            if (original !== formatted) {
                return this.structureMismatch(original);
            }
            return original;
        }

        // Type nodes - short-circuit, keep original (types are expensive to compute)
        // Check this first as isType already validates the kind property
        if (Type.isType(original)) {
            return original;
        }

        // Check if this is a tree node (has a kind property)
        if (!hasKind(original)) {
            // Primitive values or non-tree objects - copy from formatted when reconciling
            // This handles things like valueSource (quote style) which is formatting
            if (this.shouldReconcile() && formatted !== original) {
                return formatted;
            }
            return original;
        }

        // Space nodes - copy when reconciling, don't recurse
        if (isSpace(original)) {
            return this.shouldReconcile() ? formatted : original;
        }

        // Track entering target subtree (using ID equality for tree nodes)
        const isTargetSubtree = isSameNode(original, this.targetSubtree);
        const isStopAfterNode = isSameNode(original, this.stopAfterNode);
        const previousState = this.reconcileState;
        if (isTargetSubtree && this.reconcileState === 'searching') {
            this.reconcileState = 'reconciling';
        }

        try {
            // All tree nodes (J, RightPadded, LeftPadded, Container) go through visitNode
            // After hasKind() and isSpace()/isType() checks, we know this is a VisitableNode
            if (!isVisitableNode(original) || !hasKind(formatted) || !isVisitableNode(formatted)) {
                return this.structureMismatch(original);
            }
            return this.visitNode(original, formatted);
        } finally {
            // Track exiting the target subtree
            if (isTargetSubtree && previousState === 'searching') {
                this.reconcileState = 'done';
            }
            // Track exiting the stopAfter node - stop reconciling after this
            if (isStopAfterNode && previousState === 'reconciling') {
                this.reconcileState = 'done';
            }
        }
    }

    /**
     * Visit all properties of a tree node (J, RightPadded, LeftPadded, Container).
     * Copies Space values and markers when reconciling, visits everything else.
     *
     * Note: The return type may differ from the input type in cases of semantic
     * equivalence (e.g., Identifier↔Literal with quoteProps), but will always
     * be a valid VisitableNode.
     *
     * @param original Tree node with kind property
     * @param formatted Corresponding formatted tree node
     * @returns The original with whitespace from formatted applied
     */
    private visitNode(
        original: VisitableNode,
        formatted: VisitableNode
    ): VisitableNode {
        if (!this.compatible) {
            return original;
        }

        // Check if kinds match
        if (original.kind !== formatted.kind) {
            // Check if this is a valid semantic equivalence (e.g., quoteProps changing Identifier↔Literal)
            if (this.shouldReconcile() && this.isSemanticEquivalent(original, formatted)) {
                // Use the formatted node but preserve type information from original
                // isSemanticEquivalent only returns true for Identifier/Literal pairs
                return this.copyWithPreservedTypes(
                    original as J.Identifier | J.Literal,
                    formatted as J.Identifier | J.Literal
                );
            }
            return this.structureMismatch(original);
        }

        let result: VisitableNode = original;

        // Visit all properties
        for (const key of Object.keys(original)) {
            // Skip: kind, id, type properties
            if (key === 'kind' || key === 'id' ||
                key === 'type' || key === 'fieldType' || key === 'variableType' ||
                key === 'methodType' || key === 'constructorType' ||
                original.kind === JS.Kind.CompilationUnit && key == 'charsetName' ||
                // TODO In Java `null` and `undefined` are both the same
                original.kind === J.Kind.Literal && key === 'value') {
                continue;
            }

            const originalValue = (original as Record<string, unknown>)[key];
            const formattedValue = (formatted as Record<string, unknown>)[key];

            // Space values and markers: copy from formatted when reconciling
            if ((isSpace(originalValue)) || key === 'markers') {
                if (this.shouldReconcile() && formattedValue !== originalValue) {
                    result = { ...result, [key]: formattedValue } as VisitableNode;
                }
                continue;
            }

            // Handle arrays
            if (Array.isArray(originalValue)) {
                if (!Array.isArray(formattedValue) || originalValue.length !== formattedValue.length) {
                    if (originalValue.length === 0 && formattedValue === undefined && original.kind == J.Kind.ArrayType) {
                        // TODO Somehow J.ArrayType#annotations ends up as `[]`
                        continue;
                    }
                    return this.structureMismatch(original);
                }

                const newArray: unknown[] = [];
                let changed = false;
                for (let i = 0; i < originalValue.length; i++) {
                    const visited = this.visitProperty(originalValue[i], formattedValue[i]);
                    if (!this.compatible) return original;
                    newArray.push(visited);
                    if (visited !== originalValue[i]) {
                        changed = true;
                    }
                }

                if (changed) {
                    result = { ...result, [key]: newArray } as VisitableNode;
                }
            } else {
                // Visit the property
                const visited = this.visitProperty(originalValue, formattedValue);
                if (!this.compatible) return original;

                if (visited !== originalValue) {
                    result = { ...result, [key]: visited } as VisitableNode;
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

    /**
     * Checks if two nodes with different kinds are semantically equivalent.
     * This handles cases like Prettier's quoteProps option which can change
     * property names between Identifier and Literal forms.
     */
    private isSemanticEquivalent(original: VisitableNode, formatted: VisitableNode): boolean {
        // Identifier → Literal equivalence (for property names with quoteProps)
        if (isIdentifier(original) && isLiteral(formatted)) {
            return original.simpleName === formatted.value;
        }

        // Literal → Identifier equivalence
        if (isLiteral(original) && isIdentifier(formatted)) {
            return original.value === formatted.simpleName;
        }

        return false;
    }

    /**
     * Creates a copy of the formatted node with type information preserved from the original.
     * Used when we accept a structural change from Prettier (Identifier ↔ Literal)
     * but need to keep type attribution from the original.
     *
     * Only preserves `type` and `fieldType` since those are the only type-related
     * fields on J.Identifier and J.Literal.
     */
    private copyWithPreservedTypes(
        original: J.Identifier | J.Literal,
        formatted: J.Identifier | J.Literal
    ): J.Identifier | J.Literal {
        const result: Record<string, unknown> = { ...formatted };

        // Preserve type attribution - both Identifier and Literal have `type`
        if (original.type !== undefined) {
            result.type = original.type;
        }

        // Preserve fieldType - only Identifier has this, but safe to check
        if ('fieldType' in original && original.fieldType !== undefined) {
            result.fieldType = original.fieldType;
        }

        // Cast via unknown since we're modifying a copy of a valid Identifier/Literal
        return result as unknown as J.Identifier | J.Literal;
    }
}
