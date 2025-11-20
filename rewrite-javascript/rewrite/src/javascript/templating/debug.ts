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

/**
 * Consolidated debug module containing all pattern debugging functionality:
 * - Marker classes and utilities for highlighting mismatches
 * - Custom marker printer for rendering debug markers
 * - Debug-enabled pattern matching comparator with detailed logging
 */

import {Cursor, randomId, Tree} from '../..';
import {Marker, Markers, MarkersKind} from '../../markers';
import {MarkerPrinter, PrintOutputCapture} from '../../print';
import {J} from '../../java';
import {JavaScriptVisitor} from '../index';
import {DebugCallbacks, PatternMatchingComparator} from './comparator';
import {PlaceholderUtils} from './utils';
import {JTree} from "../comparator";

// ============================================================================
// Marker Classes and Utilities
// ============================================================================

/**
 * Custom marker for highlighting pattern mismatches in debug output.
 * Contains before/after strings to render around the marked element.
 */
export class PatternMismatchMarker implements Marker {
    readonly kind = 'org.openrewrite.javascript.templating.PatternMismatchMarker';
    readonly id: string;

    constructor(
        public readonly before: string,
        public readonly after: string,
        id?: string
    ) {
        this.id = id || randomId();
    }
}

/**
 * Custom marker for highlighting captures in successful pattern matches.
 * Contains the capture number (0-10) which is rendered as a numbered emoji.
 */
export class CaptureHighlightMarker implements Marker {
    readonly kind = 'org.openrewrite.javascript.templating.CaptureHighlightMarker';
    readonly id: string;

    constructor(
        public readonly captureNumber: number,
        id?: string
    ) {
        this.id = id || randomId();
    }

    /**
     * Gets the emoji representation for this capture number.
     * Supports 0-10 using keycap emojis.
     */
    getEmoji(): string {
        const emojis = ['0️⃣', '1️⃣', '2️⃣', '3️⃣', '4️⃣', '5️⃣', '6️⃣', '7️⃣', '8️⃣', '9️⃣', '🔟'];
        return emojis[this.captureNumber] || `[${this.captureNumber}]`;
    }
}

/**
 * Determines whether to use emoji markers (👉👈) or ANSI escape codes (inverse video).
 * Returns true for emoji markers, false for ANSI codes.
 *
 * Detection logic:
 * 1. CLAUDECODE env var → emoji (LLM execution context)
 * 2. TTY detected → ANSI codes (terminal with escape code support)
 * 3. No TTY but color indicators present → ANSI codes (e.g., IDE test runners)
 * 4. Otherwise → emoji (safe fallback for pipes, redirects, etc.)
 */
export function shouldUseEmoji(): boolean {
    // Explicit LLM execution (Claude Code)
    if (process.env.CLAUDECODE === '1') {
        return true;
    }

    // TTY explicitly detected → use ANSI codes
    if (process.stdout.isTTY) {
        return false;
    }

    // TTY detection unavailable but color support indicators present → use ANSI codes
    // This handles IDE test runners (like IntelliJ IDEA) that support ANSI but don't expose TTY
    if (process.stdout.isTTY === undefined) {
        // Check for standard color support indicators
        const hasColorSupport =
            process.env.COLORTERM !== undefined ||  // Color terminal indicator
            (process.env.TERM && process.env.TERM.includes('color')) ||  // Terminal type includes 'color'
            process.env.FORCE_COLOR !== undefined;  // Standard force-color flag (used by chalk, supports-color, etc.)

        if (hasColorSupport) {
            return false;  // Use ANSI codes
        }
    }

    // Default to emoji (safer fallback for pipes, redirects, etc.)
    return true;
}

// ============================================================================
// Marker Printer
// ============================================================================

/**
 * Custom MarkerPrinter for pattern debugging that renders PatternMismatchMarker
 * and CaptureHighlightMarker with custom before/after strings.
 */
export const PATTERN_DEBUG_MARKER_PRINTER: MarkerPrinter = {
    beforeSyntax(marker: Marker, cursor: Cursor, commentWrapper: (input: string) => string): string {
        if (marker instanceof PatternMismatchMarker) {
            return marker.before;
        }
        if (marker instanceof CaptureHighlightMarker) {
            return marker.getEmoji() + '';
        }
        return MarkerPrinter.DEFAULT.beforeSyntax(marker, cursor, commentWrapper);
    },

    beforePrefix(marker: Marker, cursor: Cursor, commentWrapper: (input: string) => string): string {
        return MarkerPrinter.DEFAULT.beforePrefix(marker, cursor, commentWrapper);
    },

    afterSyntax(marker: Marker, cursor: Cursor, commentWrapper: (input: string) => string): string {
        if (marker instanceof PatternMismatchMarker) {
            return marker.after;
        }
        if (marker instanceof CaptureHighlightMarker) {
            return marker.getEmoji() + '';
        }
        return MarkerPrinter.DEFAULT.afterSyntax(marker, cursor, commentWrapper);
    }
};

// ============================================================================
// ANSI-Aware Print Output Capture
// ============================================================================

/**
 * A print output capture that tracks active ANSI escape codes and reapplies them
 * after newlines to ensure multi-line elements maintain their styling.
 *
 * This is necessary because ANSI escape codes like \x1b[7m (inverted colors) don't
 * persist across newlines - each line needs to be individually styled.
 */
export class AnsiAwarePrintOutputCapture extends PrintOutputCapture {
    private activeAnsiCodes: string[] = [];

    /**
     * Appends text, tracking ANSI escape codes and reapplying them after newlines.
     */
    override append(text: string | undefined): AnsiAwarePrintOutputCapture {
        if (!text || text.length === 0) {
            return this;
        }

        // Process the text character by character to handle ANSI codes and newlines
        let i = 0;
        while (i < text.length) {
            // Check for ANSI escape sequence
            if (text[i] === '\x1b' && i + 1 < text.length && text[i + 1] === '[') {
                // Find the end of the ANSI sequence (ends with a letter)
                let j = i + 2;
                while (j < text.length && !/[a-zA-Z]/.test(text[j])) {
                    j++;
                }
                if (j < text.length) {
                    j++; // Include the terminating letter
                    const ansiCode = text.substring(i, j);
                    super.append(ansiCode);

                    // Track state-changing codes
                    // \x1b[7m = inverted colors (start)
                    // \x1b[27m = normal colors (end inverted)
                    // \x1b[0m = reset all
                    if (ansiCode === '\x1b[7m') {
                        this.activeAnsiCodes.push(ansiCode);
                    } else if (ansiCode === '\x1b[27m') {
                        // Remove inverted colors from stack
                        this.activeAnsiCodes = this.activeAnsiCodes.filter(code => code !== '\x1b[7m');
                    } else if (ansiCode === '\x1b[0m' || ansiCode === '\x1b[m') {
                        // Reset clears all active codes
                        this.activeAnsiCodes = [];
                    }

                    i = j;
                    continue;
                }
            }

            // Check for newline
            if (text[i] === '\n') {
                // First, close any active ANSI codes before the newline
                for (let j = this.activeAnsiCodes.length - 1; j >= 0; j--) {
                    if (this.activeAnsiCodes[j] === '\x1b[7m') {
                        super.append('\x1b[27m');
                    }
                }

                // Add the newline
                super.append('\n');

                // Reapply active ANSI codes after the newline
                for (const code of this.activeAnsiCodes) {
                    super.append(code);
                }

                i++;
                continue;
            }

            // Regular character
            super.append(text[i]);
            i++;
        }

        return this;
    }
}

// ============================================================================
// Element Marker Visitor
// ============================================================================

/**
 * Visitor that marks elements with markers for highlighting in debug output.
 * Uses referential equality to find target elements since nodes aren't mutated during matching.
 *
 * Can mark either:
 * - A single element with PatternMismatchMarker (for failures)
 * - Multiple elements with CaptureHighlightMarkers (for successes)
 */
export class ElementMarkerVisitor extends JavaScriptVisitor<undefined> {
    private readonly elementMarkers: Map<JTree, Marker>;

    /**
     * Create a visitor to mark a single element with a PatternMismatchMarker.
     */
    constructor(targetElement: JTree);
    /**
     * Create a visitor to mark multiple captured elements with CaptureHighlightMarkers.
     */
    constructor(captures: Map<number, JTree | JTree[]>);
    constructor(targetOrCaptures: JTree | Map<number, JTree | JTree[]>) {
        super();
        this.elementMarkers = new Map();

        if (targetOrCaptures instanceof Map) {
            // Multiple captures - build map of element -> CaptureHighlightMarker
            for (const [captureNum, captured] of targetOrCaptures.entries()) {
                const marker = new CaptureHighlightMarker(captureNum);
                if (Array.isArray(captured)) {
                    // For variadic captures, mark each element with the same number
                    for (const element of captured) {
                        this.elementMarkers.set(element, marker);
                    }
                } else {
                    this.elementMarkers.set(captured, marker);
                }
            }
        } else {
            // Single element - create PatternMismatchMarker
            const useEmoji = shouldUseEmoji();
            const before = useEmoji ? '👉' : '\x1b[7m';
            const after = useEmoji ? '👈' : '\x1b[27m';
            this.elementMarkers.set(targetOrCaptures, new PatternMismatchMarker(before, after));
        }
    }

    override async visit<R extends J>(tree: Tree | null | undefined, p: undefined): Promise<R | undefined> {
        if (!tree) return tree as R | undefined;

        // Check if this element should be marked
        const marker = this.elementMarkers.get(tree as JTree);
        if (marker) {
            const updatedMarkers = [...(tree as J).markers.markers, marker];
            const newMarkers: Markers = {
                kind: MarkersKind.Markers,
                id: (tree as J).markers.id,
                markers: updatedMarkers
            };
            return {...(tree as J), markers: newMarkers} as R;
        }

        return await super.visit(tree, p);
    }

    override async visitContainer<T extends J>(container: J.Container<T>, p: undefined): Promise<J.Container<T>> {
        // Check if this container should be marked
        const marker = this.elementMarkers.get(container);
        if (marker) {
            const updatedMarkers = [...(container as any).markers.markers, marker];
            const newMarkers: Markers = {
                kind: MarkersKind.Markers,
                id: (container as any).markers.id,
                markers: updatedMarkers
            };
            return {...container, markers: newMarkers} as J.Container<T>;
        }
        return await super.visitContainer(container, p);
    }
}

// ============================================================================
// Debug Pattern Matching Comparator
// ============================================================================

export class DebugPatternMatchingComparator extends PatternMatchingComparator {
    private get debug(): DebugCallbacks {
        return this.matcher.debug!;
    }

    /**
     * Extracts the last segment of a kind string (after the last dot).
     * For example: "org.openrewrite.java.tree.J.MethodInvocation" -> "MethodInvocation"
     */
    private formatKind(kind: string | undefined): string {
        if (!kind) return 'unknown';
        return kind.substring(kind.lastIndexOf('.') + 1);
    }

    /**
     * Formats a value for display in error messages.
     */
    private formatValue(value: any): string {
        if (value === null) return 'null';
        if (value === undefined) return 'undefined';
        if (typeof value === 'string') return `"${value}"`;
        if (typeof value === 'number' || typeof value === 'boolean') return String(value);

        // For objects with a kind property (LST nodes)
        if (value && typeof value === 'object' && value.kind) {
            const kind = this.formatKind(value.kind);

            // Show key identifying properties for common node types
            if (value.simpleName) return `${kind}("${value.simpleName}")`;
            if (value.value !== undefined) return `${kind}(${this.formatValue(value.value)})`;

            return kind;
        }

        return String(value);
    }

    /**
     * Override abort to capture explanation when debug is enabled.
     * Only sets explanation on the first abort call (when this.match is still true).
     * This preserves the most specific explanation closest to the actual mismatch.
     */
    protected override abort<T>(t: T, reason?: string, propertyName?: string, expected?: any, actual?: any): T {
        // If already aborted, don't overwrite the explanation
        // The first abort is typically the most specific
        if (!this.match) {
            return t;
        }

        // If we have context about the mismatch, capture it
        if (reason && this.debug && (expected !== undefined || actual !== undefined)) {
            const expectedStr = this.formatValue(expected);
            const actualStr = this.formatValue(actual);

            this.debug.setExplanation(
                reason as any,
                expectedStr,
                actualStr,
                'Property values do not match',
                this.cursor?.value,  // pattern element
                this.targetCursor?.value  // target element
            );
        }

        // Set `this.match = false`
        return super.abort(t, reason, propertyName, expected, actual);
    }

    /**
     * Override helper methods to extract detailed context from cursors.
     */

    protected override kindMismatch<T extends JTree>(): T {
        const pattern = this.cursor?.value as T;
        const target = this.targetCursor?.value as JTree;
        // Pass the full kind strings - formatValue() will detect and format them
        return this.abort(pattern, 'kind-mismatch', 'kind', this.formatKind(pattern?.kind), this.formatKind(target?.kind)) as T;
    }

    protected override structuralMismatch<T extends JTree>(propertyName: string): T {
        const pattern = this.cursor?.value as T;
        const target = this.targetCursor?.value as any;
        const expectedValue = (pattern as any)?.[propertyName];
        const actualValue = target?.[propertyName];
        return this.abort(pattern, 'structural-mismatch', propertyName, expectedValue, actualValue) as T;
    }

    protected override arrayLengthMismatch<T extends JTree>(propertyName: string): T {
        const pattern = this.cursor?.value as T;
        const target = this.targetCursor?.value as any;
        const expectedArray = (pattern as any)?.[propertyName];
        const actualArray = target?.[propertyName];
        const expectedLen = Array.isArray(expectedArray) ? expectedArray.length : 'not an array';
        const actualLen = Array.isArray(actualArray) ? actualArray.length : 'not an array';

        // For container mismatches, we want to mark the Container itself
        // Containers have a markers property and can be marked using object identity

        // Store the parent elements in the explanation for precise marker placement
        this.debug.setExplanation(
            'array-length-mismatch',
            String(expectedLen),
            String(actualLen),
            `Array length mismatch in ${propertyName}`,
            pattern,
            target
        );

        // Mark this as a match failure
        this.match = false;
        return pattern as T;
    }

    protected override valueMismatch<T extends JTree>(propertyName?: string, expected?: any, actual?: any): T {
        const pattern = this.cursor?.value as T;
        const target = this.targetCursor?.value as any;

        // Helper to navigate nested property paths
        const getNestedValue = (obj: any, path: string) => {
            return path.split('.').reduce((current, prop) => current?.[prop], obj);
        };

        // Helper to get the parent object (all but last property in path)
        const getParentObject = (obj: any, path: string) => {
            const parts = path.split('.');
            if (parts.length === 1) return obj;
            return parts.slice(0, -1).reduce((current, prop) => current?.[prop], obj);
        };

        // If propertyName is provided, navigate to the parent object containing the mismatched property
        // This gives us the most specific element to mark in debug output
        if (propertyName) {
            let patternParent = getParentObject(pattern, propertyName);
            let targetParent = getParentObject(target, propertyName);

            // Unwrap padded elements (JLeftPadded/JRightPadded) to get the actual element with an id
            // These wrappers don't have ids and can't be marked
            const patternKind = (patternParent as JTree)?.kind;
            const targetKind = (targetParent as JTree)?.kind;
            const isPatternPadded = patternKind === 'org.openrewrite.java.tree.JLeftPadded' ||
                                    patternKind === 'org.openrewrite.java.tree.JRightPadded';
            const isTargetPadded = targetKind === 'org.openrewrite.java.tree.JLeftPadded' ||
                                   targetKind === 'org.openrewrite.java.tree.JRightPadded';

            if (isPatternPadded) {
                patternParent = (patternParent as J.LeftPadded<any> | J.RightPadded<any>).element;
            }
            if (isTargetPadded) {
                targetParent = (targetParent as J.LeftPadded<any> | J.RightPadded<any>).element;
            }

            // Store the parent objects (which contain the mismatched property) in the explanation
            // This allows precise marker placement on the container of the mismatched value
            this.debug.setExplanation(
                'value-mismatch',
                this.formatValue(expected !== undefined ? expected : getNestedValue(pattern, propertyName)),
                this.formatValue(actual !== undefined ? actual : getNestedValue(target, propertyName)),
                'Property values do not match',
                patternParent,
                targetParent
            );

            // Mark this as a match failure
            this.match = false;
            return pattern as T;
        } else {
            // No property name - compare whole objects
            return this.abort(pattern, 'value-mismatch', propertyName, pattern, target) as T;
        }
    }

    override async visit<R extends J>(j: Tree, p: J, parent?: Cursor): Promise<R | undefined> {
        const captureMarker = PlaceholderUtils.getCaptureMarker(j)!;
        if (captureMarker) {
            const savedTargetCursor = this.targetCursor;
            const cursorAtCapturedNode = this.targetCursor !== undefined
                ? new Cursor(p, this.targetCursor)
                : new Cursor(p);
            this.targetCursor = cursorAtCapturedNode;
            try {
                if (captureMarker.constraint && !captureMarker.variadicOptions) {
                    this.debug.log('debug', 'constraint', `Evaluating constraint for capture: ${captureMarker.captureName}`);
                    const constraintResult = captureMarker.constraint(p, this.buildConstraintContext(cursorAtCapturedNode));
                    if (!constraintResult) {
                        this.debug.log('info', 'constraint', `Constraint failed for capture: ${captureMarker.captureName}`);
                        this.debug.setExplanation('constraint-failed', `Capture ${captureMarker.captureName} with valid constraint`, `Constraint failed for ${(p as JTree).kind}`, `Constraint evaluation returned false`);
                        return this.abort(j) as R;
                    }
                    this.debug.log('debug', 'constraint', `Constraint passed for capture: ${captureMarker.captureName}`);
                }

                const success = this.matcher.handleCapture(captureMarker, p, undefined);
                if (!success) {
                    return this.abort(j) as R;
                }
                return j as R;
            } finally {
                this.targetCursor = savedTargetCursor;
            }
        }

        return await super.visit(j, p, parent);
    }

    protected override async visitElement<T extends J>(j: T, other: T): Promise<T> {
        if (!this.match) {
            return j;
        }

        const kindStr = this.formatKind(j.kind);
        if (j.kind !== other.kind) {
            return this.abort(j, 'kind-mismatch', 'kind', kindStr, this.formatKind(other.kind));
        }

        for (const key of Object.keys(j)) {
            if (key.startsWith('_') || key === 'kind' || key === 'id' || key === 'markers' || key === 'prefix') {
                continue;
            }

            const jValue = (j as any)[key];
            const otherValue = (other as any)[key];

            if (Array.isArray(jValue)) {
                if (!Array.isArray(otherValue) || jValue.length !== otherValue.length) {
                    return this.abort(j, 'array-length-mismatch', key, jValue.length,
                        Array.isArray(otherValue) ? otherValue.length : otherValue);
                }

                for (let i = 0; i < jValue.length; i++) {
                    await this.visitProperty(jValue[i], otherValue[i]);
                    if (!this.match) {
                        return j;
                    }
                }
            } else {
                await this.visitProperty(jValue, otherValue);
                if (!this.match) {
                    return j;
                }
            }
        }

        return j;
    }

    override async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, p: J.RightPadded<T>): Promise<J.RightPadded<T>> {
        if (!this.match) {
            return right;
        }

        const captureMarker = PlaceholderUtils.getCaptureMarker(right);
        if (captureMarker) {
            const isRightPadded = p.kind === J.Kind.RightPadded;
            const targetWrapper = isRightPadded ? p : undefined;
            const targetElement = isRightPadded ? targetWrapper!.element : (p as any);

            const savedTargetCursor = this.targetCursor;
            const cursorAtCapturedNode = this.targetCursor !== undefined
                ? (targetWrapper ? new Cursor(targetWrapper, this.targetCursor) : new Cursor(targetElement, this.targetCursor))
                : (targetWrapper ? new Cursor(targetWrapper) : new Cursor(targetElement));
            this.targetCursor = cursorAtCapturedNode;
            try {
                if (captureMarker.constraint && !captureMarker.variadicOptions) {
                    this.debug.log('debug', 'constraint', `Evaluating constraint for wrapped capture: ${captureMarker.captureName}`);
                    const constraintResult = captureMarker.constraint(targetElement as J, this.buildConstraintContext(cursorAtCapturedNode));
                    if (!constraintResult) {
                        this.debug.log('info', 'constraint', `Constraint failed for wrapped capture: ${captureMarker.captureName}`);
                        this.debug.setExplanation('constraint-failed', `Capture ${captureMarker.captureName} with valid constraint`, `Constraint failed for ${(targetElement as any).kind}`, `Constraint evaluation returned false`);
                        return this.abort(right);
                    }
                    this.debug.log('debug', 'constraint', `Constraint passed for wrapped capture: ${captureMarker.captureName}`);
                }

                const success = this.matcher.handleCapture(captureMarker, targetElement as J, targetWrapper as J.RightPadded<J> | undefined);
                if (!success) {
                    return this.abort(right);
                }
                return right;
            } finally {
                this.targetCursor = savedTargetCursor;
            }
        }

        return await super.visitRightPadded(right, p);
    }

    override async visitContainer<T extends J>(container: J.Container<T>, p: J.Container<T>): Promise<J.Container<T>> {
        if (!this.match) {
            return container;
        }

        const isContainer = p.kind === J.Kind.Container;
        if (!isContainer) {
            return this.abort(container);
        }
        const otherContainer = p;

        const hasVariadicCapture = container.elements.some(elem =>
            PlaceholderUtils.isVariadicCapture(elem)
        );

        const savedCursor = this.cursor;
        const savedTargetCursor = this.targetCursor;
        this.cursor = new Cursor(container, this.cursor);
        this.targetCursor = new Cursor(otherContainer, this.targetCursor);
        try {
            if (hasVariadicCapture) {
                if (!await this.matchSequence(container.elements as J.RightPadded<J>[], otherContainer.elements as J.RightPadded<J>[], true)) {
                    return this.arrayLengthMismatch<J.Container<T>>('elements');
                }
            } else {
                // Non-variadic path - track indices
                if (container.elements.length !== otherContainer.elements.length) {
                    return this.arrayLengthMismatch<J.Container<T>>('elements');
                }

                for (let i = 0; i < container.elements.length; i++) {
                    if (!await this.visitRightPadded(container.elements[i], otherContainer.elements[i]) || !this.match) {
                        return container;
                    }
                }
            }
        } finally {
            this.cursor = savedCursor;
            this.targetCursor = savedTargetCursor;
        }

        return container;
    }

    /**
     * Override visitContainerProperty to add path tracking with property context.
     */
    protected override async visitContainerProperty<T extends J>(
        propertyName: string,
        container: J.Container<T>,
        otherContainer: J.Container<T>
    ): Promise<J.Container<T>> {
        // Get parent from cursor
        const parent = this.cursor?.value as J;

        // Push path for the property
        this.formatKind((parent as any)?.kind);

        // Update targetCursor to point to the container so that if there's a mismatch,
        // the cursor will be on the container itself (for proper marker placement)
        const savedTargetCursor = this.targetCursor;
        if (this.targetCursor !== undefined) {
            this.targetCursor = new Cursor(otherContainer, this.targetCursor);
        }

        try {
            await this.visitContainer(container, otherContainer as any);
            return container;
        } finally {
            this.targetCursor = savedTargetCursor;
        }
    }

    protected override async visitArrayProperty<T>(
        parent: J,
        propertyName: string,
        array1: T[],
        array2: T[],
        visitor: (item1: T, item2: T, index: number) => Promise<void>
    ): Promise<void> {
        // Check length mismatch
        if (array1.length !== array2.length) {
            this.arrayLengthMismatch(propertyName);
            return;
        }

        // Visit each element with index tracking
        for (let i = 0; i < array1.length; i++) {
            await visitor(array1[i], array2[i], i);
            if (!this.match) {
                return;
            }
        }
    }

    protected override async matchSequence(
        patternElements: J.RightPadded<J>[],
        targetElements: J.RightPadded<J>[],
        filterEmpty: boolean
    ): Promise<boolean> {
        return await super.matchSequence(patternElements, targetElements, filterEmpty);
    }

    protected override async visitSequenceElement(
        patternWrapper: J.RightPadded<J>,
        targetWrapper: J.RightPadded<J>,
        targetIdx: number
    ): Promise<boolean> {
        // Save current state for backtracking (both match state and capture bindings)
        const savedMatch = this.match;
        const savedState = this.matcher.saveState();

        await this.visitRightPadded(patternWrapper, targetWrapper as any);

        if (!this.match) {
            // Preserve explanation before restoring state
            const explanation = this.debug.getExplanation();
            // Restore state on match failure
            this.match = savedMatch;
            this.matcher.restoreState(savedState);
            // Restore the explanation if one was set during matching
            if (explanation) {
                this.debug.restoreExplanation(explanation);
            }
            return false;
        }

        return true;
    }

    protected override async matchSequenceOptimized(
        patternElements: J.RightPadded<J>[],
        targetElements: J.RightPadded<J>[],
        patternIdx: number,
        targetIdx: number,
        filterEmpty: boolean
    ): Promise<boolean> {
        if (patternIdx >= patternElements.length) {
            return targetIdx >= targetElements.length;
        }

        const patternWrapper = patternElements[patternIdx];
        const captureMarker = PlaceholderUtils.getCaptureMarker(patternWrapper);
        const isVariadic = captureMarker?.variadicOptions !== undefined;

        if (isVariadic) {
            const variadicOptions = captureMarker!.variadicOptions;
            const min = variadicOptions?.min ?? 0;
            const max = variadicOptions?.max ?? Infinity;

            let nonVariadicRemainingPatterns = 0;
            let allRemainingPatternsAreDeterministic = true;
            for (let i = patternIdx + 1; i < patternElements.length; i++) {
                const nextCaptureMarker = PlaceholderUtils.getCaptureMarker(patternElements[i]);
                const nextIsVariadic = nextCaptureMarker?.variadicOptions !== undefined;
                if (!nextIsVariadic) {
                    nonVariadicRemainingPatterns++;
                }
                if (nextCaptureMarker) {
                    allRemainingPatternsAreDeterministic = false;
                }
            }
            const remainingTargetElements = targetElements.length - targetIdx;
            const maxPossible = Math.min(remainingTargetElements - nonVariadicRemainingPatterns, max);

            let pivotDetected = false;
            let pivotAt = -1;

            // Skip pivot detection if we're using deterministic optimization
            // (when all remaining patterns are literals, there's only ONE valid consumption amount)
            const useDeterministicOptimization = allRemainingPatternsAreDeterministic && maxPossible >= min && maxPossible <= max;

            if (!useDeterministicOptimization && patternIdx + 1 < patternElements.length && min <= maxPossible) {
                const nextPattern = patternElements[patternIdx + 1];

                for (let tryConsume = min; tryConsume <= maxPossible; tryConsume++) {
                    if (targetIdx + tryConsume < targetElements.length) {
                        const candidateElement = targetElements[targetIdx + tryConsume];

                        if (filterEmpty && candidateElement.element.kind === J.Kind.Empty) {
                            continue;
                        }

                        const savedMatch = this.match;
                        const savedState = this.matcher.saveState();

                        await this.visitRightPadded(nextPattern, candidateElement as any);
                        const matchesNext = this.match;

                        this.match = savedMatch;
                        this.matcher.restoreState(savedState);

                        if (matchesNext) {
                            pivotDetected = true;
                            pivotAt = tryConsume;
                            break;
                        }
                    }
                }
            }

            const consumptionOrder: number[] = [];
            // OPTIMIZATION: If all remaining patterns are deterministic (literals, not captures),
            // there's only ONE mathematically valid consumption amount. Skip backtracking entirely.
            // Example: foo(${args}, 999) matching foo(1,2,42) -> args MUST be [1,2], only try consume=2
            if (useDeterministicOptimization) {
                consumptionOrder.push(maxPossible);
            } else if (pivotDetected && pivotAt >= 0) {
                consumptionOrder.push(pivotAt);
                for (let c = maxPossible; c >= min; c--) {
                    if (c !== pivotAt) {
                        consumptionOrder.push(c);
                    }
                }
            } else {
                for (let c = maxPossible; c >= min; c--) {
                    consumptionOrder.push(c);
                }
            }

            for (const consume of consumptionOrder) {
                // Capture elements for this consumption amount
                // For empty argument lists, there will be a single J.Empty element that we need to filter out
                const rawWrappers = targetElements.slice(targetIdx, targetIdx + consume);
                const capturedWrappers = filterEmpty
                    ? rawWrappers.filter(w => w.element.kind !== J.Kind.Empty)
                    : rawWrappers;
                const capturedElements: J[] = capturedWrappers.map(w => w.element);

                // Check min/max constraints against filtered elements
                if (capturedElements.length < min || capturedElements.length > max) {
                    continue;
                }

                if (captureMarker.constraint) {
                    this.debug.log('debug', 'constraint', `Evaluating variadic constraint for capture: ${captureMarker.captureName} (${capturedElements.length} elements)`);
                    const cursor = this.targetCursor || new Cursor(targetElements[0]);
                    const constraintResult = captureMarker.constraint(capturedElements as any, this.buildConstraintContext(cursor));
                    if (!constraintResult) {
                        this.debug.log('info', 'constraint', `Variadic constraint failed for capture: ${captureMarker.captureName}`);
                        continue;
                    }
                    this.debug.log('debug', 'constraint', `Variadic constraint passed for capture: ${captureMarker.captureName}`);
                }

                const savedState = this.matcher.saveState();

                const success = this.matcher.handleVariadicCapture(captureMarker, capturedElements, capturedWrappers);
                if (!success) {
                    this.matcher.restoreState(savedState);
                    continue;
                }

                const restMatches = await this.matchSequenceOptimized(
                    patternElements,
                    targetElements,
                    patternIdx + 1,
                    targetIdx + consume,
                    filterEmpty
                );

                if (restMatches) {
                    return true;
                }

                // Preserve explanation from this failed attempt before restoring state
                // This is especially important when using deterministic optimization (only one attempt)
                const currentExplanation = this.debug.getExplanation();
                this.matcher.restoreState(savedState);
                // Restore the explanation if one was set during this attempt
                if (currentExplanation) {
                    this.debug.restoreExplanation(currentExplanation);
                }
            }

            return false;
        } else {
            if (targetIdx >= targetElements.length) {
                return false;
            }

            const targetWrapper = targetElements[targetIdx];
            const targetElement = targetWrapper.element;

            if (filterEmpty && targetElement.kind === J.Kind.Empty) {
                return false;
            }

            if (!await this.visitSequenceElement(patternWrapper, targetWrapper, targetIdx)) {
                return false;
            }

            return await this.matchSequenceOptimized(
                patternElements,
                targetElements,
                patternIdx + 1,
                targetIdx + 1,
                filterEmpty
            );
        }
    }
}
