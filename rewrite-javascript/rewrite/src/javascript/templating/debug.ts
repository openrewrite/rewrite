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
import {MarkerPrinter, PrintOutputCapture, TreePrinters} from '../../print';
import {J, Type} from '../../java';
import {JavaScriptVisitor, JS} from '../index';
import {DebugCallbacks, PatternMatchingComparator} from './comparator';
import {CaptureMarker, PlaceholderUtils} from './utils';
import {MatchExplanation} from './types';
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

/** What a mismatch marker prints before and after the mismatched node. */
export function mismatchDelimiters(): [string, string] {
    return shouldUseEmoji() ? ['👉', '👈'] : ['\x1b[7m', '\x1b[27m'];
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
 * A print output capture that closes active ANSI escape codes at each newline and resumes them at the
 * next line's first non-blank character. Each line of debug output is then styled on its own, and its
 * indentation stays unstyled so that it can be dedented.
 */
export class AnsiAwarePrintOutputCapture extends PrintOutputCapture {
    private activeAnsiCodes: string[] = [];
    private resumePending = false;

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
                for (let j = this.activeAnsiCodes.length - 1; j >= 0 && !this.resumePending; j--) {
                    if (this.activeAnsiCodes[j] === '\x1b[7m') {
                        super.append('\x1b[27m');
                    }
                }

                super.append('\n');
                this.resumePending = this.activeAnsiCodes.length > 0;
                i++;
                continue;
            }

            if (this.resumePending && text[i] !== ' ' && text[i] !== '\t') {
                for (const code of this.activeAnsiCodes) {
                    super.append(code);
                }
                this.resumePending = false;
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
            const [before, after] = mismatchDelimiters();
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

/** Whether {@link ElementMarkerVisitor} can mark `value`, which it does for the trees and containers it visits. */
function isMarkable(value: any): value is JTree {
    return !!value?.markers && value.kind !== J.Kind.LeftPadded && value.kind !== J.Kind.RightPadded;
}

async function printCode(nodes: J[]): Promise<string> {
    const printer = TreePrinters.printer(JS.Kind.CompilationUnit);
    const printed = await Promise.all(nodes.map(async node => (await printer.print(node)).trim()));
    return printed.join(', ');
}

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

        if (Type.isType(value)) {
            return Type.isFullyQualified(value) ?
                Type.FullyQualified.getFullyQualifiedName(value) :
                this.formatKind((value as Type).kind);
        }
        if (value.kind === J.Kind.LeftPadded || value.kind === J.Kind.RightPadded) {
            return this.formatValue(value.element);
        }

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
     * The node a mismatch is shown on. That is `element`, or the element a padding wrapper holds,
     * where it can be marked, and otherwise the nearest node on `cursor` that can.
     */
    private markable(element: any, cursor: Cursor | undefined): JTree | undefined {
        if (element?.kind === J.Kind.LeftPadded || element?.kind === J.Kind.RightPadded) {
            element = element.element;
        }
        if (isMarkable(element)) {
            return element;
        }
        for (let c = cursor; c; c = c.parent) {
            if (isMarkable(c.value)) {
                return c.value;
            }
        }
        return undefined;
    }

    /** Records why matching failed, unless an earlier mismatch already did, and stops the match. */
    private fail<T>(t: T, reason: MatchExplanation['reason'], expected: string, actual: string, details: string,
                    patternElement: any, targetElement: any): T {
        if (this.match) {
            this.explain(reason, expected, actual, details, patternElement, targetElement);
        }
        this.match = false;
        return t;
    }

    private explain(reason: MatchExplanation['reason'], expected: string, actual: string, details: string,
                    patternElement: any, targetElement: any): void {
        this.debug.setExplanation(reason, expected, actual, details,
            this.markable(patternElement, this.cursor), this.markable(targetElement, this.targetCursor));
    }

    private failConstraint<T>(t: T, marker: CaptureMarker, target: J): T {
        return this.fail(t, 'constraint-failed', `Capture ${marker.captureName} with valid constraint`,
            `Constraint failed for ${this.formatKind(target.kind)}`, 'Constraint evaluation returned false', t, target);
    }

    /** Explains a capture the pattern names twice that binds different code the second time. */
    private async explainBoundCapture(marker: CaptureMarker, patternElement: any, targets: J[]): Promise<void> {
        const bound = marker.captureName ? this.matcher.saveState().storage.get(marker.captureName) : undefined;
        if (bound === undefined) {
            return;
        }
        const boundCode = (Array.isArray(bound) ? bound : [bound]).map(b =>
            b.kind === J.Kind.RightPadded ? (b as J.RightPadded<J>).element : b as J);
        this.explain('value-mismatch',
            `${await printCode(boundCode)} (what this capture holds already)`,
            await printCode(targets),
            'A capture the pattern names twice binds the same code at both places',
            patternElement,
            targets.length === 1 ? targets[0] : this.targetCursor?.value);
    }

    /** An abort that names no values leaves the explanation to the matcher's generic one. */
    protected override abort<T>(t: T, reason?: string, propertyName?: string, expected?: any, actual?: any): T {
        if (reason && (expected !== undefined || actual !== undefined)) {
            return this.fail(t, reason as MatchExplanation['reason'],
                this.formatValue(expected), this.formatValue(actual),
                'Property values do not match', this.cursor?.value, this.targetCursor?.value);
        }
        return super.abort(t, reason, propertyName, expected, actual);
    }

    /**
     * Override helper methods to extract detailed context from cursors.
     */

    protected override kindMismatch<T extends JTree>(): T {
        const pattern = this.cursor?.value as T;
        const target = this.targetCursor?.value as JTree;
        return this.fail(pattern, 'kind-mismatch', this.formatKind(pattern?.kind), this.formatKind(target?.kind),
            'Node kinds differ', pattern, target);
    }

    protected override structuralMismatch<T extends JTree>(propertyName: string): T {
        const pattern = this.cursor?.value as T;
        const target = this.targetCursor?.value as any;
        const expectedValue = (pattern as any)?.[propertyName];
        const actualValue = target?.[propertyName];
        if ((expectedValue === undefined) !== (actualValue === undefined)) {
            return this.fail(pattern, 'structural-mismatch',
                expectedValue === undefined ? 'none' : propertyName,
                actualValue === undefined ? 'none' : propertyName,
                `Only one side has ${propertyName}`, expectedValue ?? pattern, actualValue ?? target);
        }
        return this.abort(pattern, 'structural-mismatch', propertyName, expectedValue, actualValue) as T;
    }

    protected override arrayLengthMismatch<T extends JTree>(propertyName: string): T {
        const pattern = this.cursor?.value as T;
        const target = this.targetCursor?.value as any;
        const expectedArray = (pattern as any)?.[propertyName];
        const actualArray = target?.[propertyName];
        const expectedLen = Array.isArray(expectedArray) ? expectedArray.length : 'not an array';
        const actualLen = Array.isArray(actualArray) ? actualArray.length : 'not an array';
        return this.fail(pattern, 'array-length-mismatch', String(expectedLen), String(actualLen),
            `Array length mismatch in ${propertyName}`, pattern, target);
    }

    protected override valueMismatch<T extends JTree>(propertyName?: string, expected?: any, actual?: any): T {
        const pattern = this.cursor?.value as any;
        const target = this.targetCursor?.value as any;
        const path = propertyName ? propertyName.split('.') : [];
        const at = (node: any, properties: string[]) =>
            properties.reduce((current, property) => current?.[property], node);

        // The mismatch shows on the node holding the property, and a call's method type on the call's name
        const holder = pattern?.kind === J.Kind.MethodInvocation && path[0] === 'methodType' ?
            ['name'] : path.slice(0, -1);
        const describe = (value: any) => path[path.length - 1] === 'declaringType' ?
            `declared in ${this.formatValue(value)}` : this.formatValue(value);
        return this.fail(pattern as T, 'value-mismatch',
            describe(expected !== undefined ? expected : at(pattern, path)),
            describe(actual !== undefined ? actual : at(target, path)),
            'Property values do not match',
            at(pattern, holder), at(target, holder));
    }

    override async visit<R extends J>(j: Tree, p: J, parent?: Cursor): Promise<R | undefined> {
        const capture = this.captureAt(j, p);
        if (capture) {
            const {marker: captureMarker, target} = capture;
            const savedTargetCursor = this.targetCursor;
            const cursorAtCapturedNode = this.targetCursor !== undefined
                ? new Cursor(target, this.targetCursor)
                : new Cursor(target);
            this.targetCursor = cursorAtCapturedNode;
            try {
                if (captureMarker.constraint && !captureMarker.variadicOptions) {
                    this.debug.log('debug', 'constraint', `Evaluating constraint for capture: ${captureMarker.captureName}`);
                    const constraintResult = captureMarker.constraint(target, this.buildConstraintContext(cursorAtCapturedNode));
                    if (!constraintResult) {
                        this.debug.log('info', 'constraint', `Constraint failed for capture: ${captureMarker.captureName}`);
                        return this.failConstraint(j as R, captureMarker, target);
                    }
                    this.debug.log('debug', 'constraint', `Constraint passed for capture: ${captureMarker.captureName}`);
                }

                const success = await this.matcher.handleCapture(captureMarker, target, undefined);
                if (!success) {
                    await this.explainBoundCapture(captureMarker, j, [target]);
                    return this.abort(j) as R;
                }
                return j as R;
            } finally {
                this.targetCursor = savedTargetCursor;
            }
        }

        return await super.visit(j, p, parent);
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
                        return this.failConstraint(right, captureMarker, targetElement as J);
                    }
                    this.debug.log('debug', 'constraint', `Constraint passed for wrapped capture: ${captureMarker.captureName}`);
                }

                const success = await this.matcher.handleCapture(captureMarker, targetElement as J, targetWrapper as J.RightPadded<J> | undefined);
                if (!success) {
                    await this.explainBoundCapture(captureMarker, right, [targetElement as J]);
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

                const success = await this.matcher.handleVariadicCapture(captureMarker, capturedElements, capturedWrappers);
                if (!success) {
                    this.matcher.restoreState(savedState);
                    await this.explainBoundCapture(captureMarker, patternWrapper, capturedElements);
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
