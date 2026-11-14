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

import * as path from 'path';
import {Cursor, isSourceFile, randomId, SourceFile, Tree} from '../..';
import {Marker, Markers, MarkersKind} from '../../markers';
import {MarkerPrinter, PrintOutputCapture, TreePrinters} from '../../print';
import {J, Type} from '../../java';
import {JavaScriptVisitor, JS} from '../index';
import {DebugCallbacks, Focus, MarkFocus, PatternMatchingComparator} from './comparator';
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
 * Marks the code a capture holds in a successful match, with a circled letter for the capture's number.
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

    /** Ⓐ for the first capture through Ⓩ for the 26th, and the number in brackets after that. */
    symbol(): string {
        return this.captureNumber >= 1 && this.captureNumber <= 26 ?
            String.fromCodePoint(0x24B6 + this.captureNumber - 1) : `[${this.captureNumber}]`;
    }
}

/**
 * Whether debug output marks mismatches with 👉…👈 rather than colours and inverse video. FORCE_COLOR asks
 * for colours, and otherwise NO_COLOR and a Claude Code session ask for markers. Then stderr decides where
 * it is a terminal. A test runner's worker cannot see the terminal, so the terminal's own colour settings
 * decide.
 */
export function shouldUseEmoji(): boolean {
    if (process.env.FORCE_COLOR && process.env.FORCE_COLOR !== '0') {
        return false;
    }
    if (process.env.CLAUDECODE === '1' || process.env.NO_COLOR) {
        return true;
    }
    if (process.stderr.isTTY) {
        return false;
    }
    return !(process.env.COLORTERM || process.env.TERM?.includes('color'));
}

/**
 * The first frame of `stack` outside this package, as `file:line:column (function)` with `file` relative
 * to the working directory. The stack's own first frame names the package's root.
 */
export function callSite(stack: string | undefined): string | undefined {
    const frames = (stack ?? '').split('\n')
        .map(line => /^\s*at (?:async )?(?:(.+?) \()?(?:file:\/\/)?(.+?):(\d+):(\d+)\)?$/.exec(line))
        .filter((frame): frame is RegExpExecArray => frame !== null);
    const own = frames[0]?.[2];
    const rootEnd = own ? own.lastIndexOf('/javascript/templating/') : -1;
    const root = rootEnd >= 0 ? own!.slice(0, rootEnd + 1) : undefined;
    const caller = frames.find(([, , file]) => !(root && file.startsWith(root)) && !file.startsWith('node:'));
    if (!caller) {
        return undefined;
    }
    const [, fn, file, line, column] = caller;
    return `${path.relative(process.cwd(), file)}:${line}:${column}${fn ? ` (${fn})` : ''}`;
}

/** What a mismatch mark prints before and after the node it is on. */
export function mismatchDelimiters(emoji: boolean = shouldUseEmoji()): [string, string] {
    return emoji ? ['👉', '👈'] : ['\x1b[7m', '\x1b[27m'];
}

/** Marks where a tree starts, for {@link sourceLocation}. */
class PositionMarker implements Marker {
    readonly kind = 'org.openrewrite.javascript.templating.PositionMarker';
    readonly id = randomId();

    constructor(readonly index: number) {
    }
}

const positionsBySourceFile = new WeakMap<object, Map<Tree, { line: number, column: number }>>();

/** Where each tree of `sourceFile` starts, from one print of the file with every tree's start marked. */
async function positionsIn(sourceFile: SourceFile): Promise<Map<Tree, { line: number, column: number }>> {
    let positions = positionsBySourceFile.get(sourceFile);
    if (positions) {
        return positions;
    }
    const trees: Tree[] = [];
    const marked = await new class extends JavaScriptVisitor<undefined> {
        override async visit<R extends J>(tree: Tree | null | undefined, p: undefined): Promise<R | undefined> {
            if (!tree) {
                return tree as R | undefined;
            }
            const index = trees.push(tree) - 1;
            const visited = await super.visit(tree, p) as J | undefined;
            return visited && {
                ...visited,
                markers: {...visited.markers, markers: [...visited.markers.markers, new PositionMarker(index)]}
            } as R;
        }
    }().visit(sourceFile as unknown as J, undefined);
    const printed = await TreePrinters.printer(sourceFile).print(marked!, new PrintOutputCapture({
        beforeSyntax: marker => marker instanceof PositionMarker ? `\0${marker.index}\0` : '',
        beforePrefix: () => '',
        afterSyntax: () => ''
    }));
    positions = new Map();
    let line = 1;
    let column = 1;
    for (let i = 0; i < printed.length; i++) {
        if (printed[i] === '\0') {
            const end = printed.indexOf('\0', i + 1);
            positions.set(trees[Number(printed.slice(i + 1, end))], {line, column});
            i = end;
        } else if (printed[i] === '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
    }
    positionsBySourceFile.set(sourceFile, positions);
    return positions;
}

/**
 * The source file `cursor` leads to, and where `tree` starts in it. A file that does not hold
 * `tree` itself, as for a node a visitor built, gives its path alone.
 */
export async function sourceLocation(tree: J, cursor: Cursor | undefined):
    Promise<{ sourcePath: string, line?: number, column?: number } | undefined> {
    const sourceFile = isSourceFile(tree) ? tree : cursor?.firstEnclosing(isSourceFile);
    if (!sourceFile) {
        return undefined;
    }
    return {sourcePath: sourceFile.sourcePath, ...(await positionsIn(sourceFile)).get(tree)};
}

/**
 * `printed` with the mark around `element`'s code moved to `focus` within that code. It stays as it
 * is where the mark does not hold exactly what `element` prints on its own.
 */
export async function narrowMark(printed: string, element: J, focus: MarkFocus,
                                 [before, after]: [string, string]): Promise<string> {
    const printer = TreePrinters.printer(JS.Kind.CompilationUnit);
    const code = (await printer.print(element)).trim();
    const at = printed.indexOf(before + code + after);
    let range: [number, number] | undefined;
    if (focus === 'start') {
        range = [0, 0];
    } else if (focus === 'operator') {
        range = await operatorRange(element, code);
    } else {
        const child = (await printer.print(focus.after)).trim();
        const end = code.indexOf(child) + child.length;
        range = child && end >= child.length ? [end, end] : undefined;
    }
    if (at < 0 || !range) {
        return printed;
    }
    const [start, end] = range;
    return printed.slice(0, at) + code.slice(0, start) + before + code.slice(start, end) + after +
        code.slice(end) + printed.slice(at + before.length + code.length + after.length);
}

/**
 * The lines within one line of a mark, with `…` for each run of two or more lines left out. A mark
 * over more than six lines keeps its first two and last two.
 */
export function aroundMarks(lines: string[], [before, after]: [string, string]): string[] {
    let open = 0;
    const marked = lines.map(line => {
        const inside = open > 0 || line.includes(before) || line.includes(after);
        open += line.split(before).length - line.split(after).length;
        return inside;
    });
    if (!marked.includes(true)) {
        return lines;
    }
    const kept = lines.map((_, i) => marked[i] || marked[i - 1] === true || marked[i + 1] === true);
    for (let start = 0; start < lines.length; start++) {
        let end = start;
        while (end < lines.length && marked[end]) {
            end++;
        }
        if (end - start > 6) {
            kept.fill(false, start + 2, end - 2);
        }
        start = end;
    }
    const result: string[] = [];
    for (let i = 0; i < lines.length;) {
        let j = i;
        while (j < lines.length && kept[j] === kept[i]) {
            j++;
        }
        result.push(...(!kept[i] && j - i >= 2 ? ['…'] : lines.slice(i, j)));
        i = j;
    }
    return result;
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
            return marker.symbol();
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
            return marker.symbol();
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
        if (!this.resumePending && !text.includes('\x1b') && !text.includes('\n')) {
            super.append(text);
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
    constructor(targetElement: JTree, emoji?: boolean);
    /**
     * Create a visitor to mark multiple captured elements with CaptureHighlightMarkers.
     */
    constructor(captures: Map<number, JTree | JTree[]>);
    constructor(targetOrCaptures: JTree | Map<number, JTree | JTree[]>, emoji: boolean = shouldUseEmoji()) {
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
            const [before, after] = mismatchDelimiters(emoji);
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

/** The value at a property path such as `lambda.parameters.parameters` or `statements[2]`. */
function at(node: any, path: string | undefined): any {
    if (!path) {
        return node;
    }
    return path.split(/\.|\[(\d+)]/).filter(Boolean).reduce((current, property) => current?.[property], node);
}

/** The code `value` stands for, as one line, or "none" where there is none. */
export async function describe(value: any): Promise<string> {
    const elements = Array.isArray(value) ? value :
        value?.kind === J.Kind.Container ? value.elements :
            value === undefined || value === null ? [] : [value];
    const printer = TreePrinters.printer(JS.Kind.CompilationUnit);
    const printed: string[] = [];
    for (let element of elements) {
        if (element?.kind === J.Kind.LeftPadded || element?.kind === J.Kind.RightPadded) {
            element = element.element;
        }
        const text = isMarkable(element) ? (await printer.print(element as J)).trim() : String(element);
        if (text) {
            printed.push(text.split('\n')[0] + (text.includes('\n') ? ' …' : ''));
        }
    }
    return printed.length > 0 ? printed.join(', ') : 'none';
}

/** A tree's kind in words, followed by its kind name, such as `arrow function (JS$ArrowFunction)`. */
export function describeKind(tree: any): string {
    // A function written where an expression goes is held as a statement, which names its kind
    const named = tree?.kind === JS.Kind.StatementExpression ? tree.statement : tree;
    return `${kindInWords(tree)} (${shortKind(named)})`;
}

function shortKind(tree: any): string {
    return tree?.kind ? tree.kind.substring(tree.kind.lastIndexOf('.') + 1) : 'unknown';
}

function kindInWords(tree: any): string {
    // A function written where an expression goes is held as a statement
    return tree?.kind === JS.Kind.StatementExpression && tree.statement?.kind === J.Kind.MethodDeclaration ?
        'function expression' : inWords(shortKind(tree).replace(/^[A-Za-z]+\$/, '').replace(/\$/g, ' '));
}

function inWords(name: string): string {
    return name.replace(/([a-z])([A-Z])/g, '$1 $2').toLowerCase();
}

/** Properties whose names say something other than what TypeScript calls them where they appear. */
const PROPERTY_WORDS: Record<string, string> = {
    [`${J.Kind.MethodInvocation}.typeParameters`]: 'type arguments',
    [`${JS.Kind.FunctionCall}.typeParameters`]: 'type arguments',
    [`${J.Kind.NewClass}.typeParameters`]: 'type arguments',
    [`${JS.Kind.ImportClause}.name`]: 'default import',
    [`${JS.Kind.ImportClause}.namedBindings`]: 'named imports'
};

/**
 * The code `tree` writes besides its operands, such as the `+` of a binary or the `!` of a unary.
 * Each operand prints at the start or the end of its operation, so what remains is the operator.
 */
async function operatorOf(tree: any): Promise<string | undefined> {
    const code = (await TreePrinters.printer(JS.Kind.CompilationUnit).print(tree)).trim();
    const range = await operatorRange(tree, code);
    return range ? code.slice(...range) : undefined;
}

/** Where the operator is in `code`, which is what `tree` prints. */
async function operatorRange(tree: any, code: string): Promise<[number, number] | undefined> {
    const printer = TreePrinters.printer(JS.Kind.CompilationUnit);
    let start = 0;
    let end = code.length;
    const operands = [tree.left ?? tree.variable ?? tree.expression, tree.right ?? tree.assignment ?? tree.expression];
    for (const operand of operands) {
        const text = operand ? (await printer.print(operand)).trim() : '';
        if (text && code.startsWith(text, start)) {
            start += text.length;
        } else if (text && code.slice(start, end).endsWith(text)) {
            end -= text.length;
        }
    }
    while (start < end && /\s/.test(code[start])) {
        start++;
    }
    while (end > start && /\s/.test(code[end - 1])) {
        end--;
    }
    return start < end ? [start, end] : undefined;
}

/** How the pattern's own text writes a capture, which the debug output shows as `🪝name`. */
function placeholder(marker: CaptureMarker): string {
    return `${PlaceholderUtils.CAPTURE_PREFIX}${marker.captureName}__`;
}

/**
 * Explains why a pattern does not match, and changes nothing about matching itself. It records
 * what the base class reports through its mismatch methods and hooks, with the nodes to mark.
 */
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
        if (typeof value === 'bigint') return `${value}n`;

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

    /** A property's name and the code it holds, such as `modifiers: async`. */
    private async describeProperty(propertyName: string | undefined, value: any): Promise<string> {
        return propertyName ? `${this.propertyLabel(propertyName)}: ${await describe(value)}` : describe(value);
    }

    /**
     * The property in words: the last name in its path, or for a container's `elements` or a padded
     * value, the name its owner holds it under.
     */
    private propertyLabel(propertyName: string | undefined): string {
        let owner = this.cursor?.value;
        let name = propertyName?.replace(/\[\d+]$/, '').split('.').pop();
        const wrapper = this.cursor?.value;
        if (propertyName === undefined || propertyName === 'elements' && wrapper?.kind === J.Kind.Container) {
            owner = this.cursor?.parent?.value;
            name = owner ? Object.keys(owner).find(key => owner[key] === wrapper) : undefined;
        } else if (name === 'statements' && wrapper?.kind === J.Kind.Block &&
            this.cursor?.parent?.value?.kind === J.Kind.NewClass) {
            // An object literal holds its properties as the statements of a block
            return 'properties';
        }
        return name ? PROPERTY_WORDS[`${owner?.kind}.${name}`] ?? inWords(name) : 'value';
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
    private fail<T>(t: T, reason: MatchExplanation['reason'], expected: string | Promise<string>,
                    actual: string | Promise<string>, details: string | undefined, patternElement: any,
                    targetElement: any, focus?: Focus): T {
        if (this.match) {
            this.explain(reason, expected, actual, details, patternElement, targetElement, focus);
        }
        this.match = false;
        return t;
    }

    private explain(reason: MatchExplanation['reason'], expected: string | Promise<string>,
                    actual: string | Promise<string>, details: string | undefined, patternElement: any,
                    targetElement: any, focus?: Focus): void {
        this.debug.setExplanation(reason, expected, actual, details,
            this.markable(patternElement, this.cursor), this.markable(targetElement, this.targetCursor), focus);
    }

    /** An abort that names no values leaves the explanation to the matcher's generic one. */
    protected override abort<T>(t: T, reason?: string, propertyName?: string, expected?: any, actual?: any): T {
        if (reason && (expected !== undefined || actual !== undefined)) {
            return this.fail(t, reason as MatchExplanation['reason'],
                this.formatValue(expected), this.formatValue(actual),
                undefined, this.cursor?.value, this.targetCursor?.value);
        }
        return super.abort(t, reason, propertyName, expected, actual);
    }

    protected override kindMismatch<T extends JTree>(): T {
        const pattern = this.cursor?.value as T;
        const target = this.targetCursor?.value as any;
        // Operations of one name in words, such as JavaScript's `===` and Java's `-`, differ in their operators
        if (kindInWords(pattern) === kindInWords(target) && (pattern as any)?.operator && target?.operator) {
            const describeOperator = async (tree: any) => `operator: ${await operatorOf(tree) ?? describeKind(tree)}`;
            return this.fail(pattern, 'kind-mismatch', describeOperator(pattern), describeOperator(target), undefined,
                pattern, target, {pattern: 'operator', target: 'operator'});
        }
        const names: string[] = [J.Kind.Identifier, J.Kind.FieldAccess];
        const note = names.includes(pattern?.kind as string) && names.includes(target?.kind) ?
            'A pattern matches a name as it is written, not what it refers to. ' +
            'Write it as the source does, or put a capture in its place.' : undefined;
        return this.fail(pattern, 'kind-mismatch', describeKind(pattern), describeKind(target), note,
            pattern, target);
    }

    protected override structuralMismatch<T extends JTree>(propertyName?: string): T {
        const pattern = this.cursor?.value as T;
        const target = this.targetCursor?.value as any;
        const expectedValue = propertyName ? at(pattern, propertyName) : pattern;
        const actualValue = propertyName ? at(target, propertyName) : target;
        const onlyOneSide = (expectedValue === undefined) !== (actualValue === undefined);
        // The side without the property is marked where it would go
        const missing = (node: any): MarkFocus | undefined =>
            propertyName === 'typeParameters' && node?.name ? {after: node.name} :
                propertyName === 'select' ? 'start' : undefined;
        return this.fail(pattern, 'structural-mismatch',
            this.describeProperty(propertyName, expectedValue), this.describeProperty(propertyName, actualValue),
            undefined, onlyOneSide ? expectedValue ?? pattern : pattern, onlyOneSide ? actualValue ?? target : target,
            onlyOneSide ? {
                pattern: expectedValue === undefined ? missing(pattern) : undefined,
                target: actualValue === undefined ? missing(target) : undefined
            } : undefined);
    }

    protected override arrayLengthMismatch<T extends JTree>(propertyName: string): T {
        const pattern = this.cursor?.value as T;
        const target = this.targetCursor?.value as any;
        const label = this.propertyLabel(propertyName);
        const hint = label === 'arguments' || label === 'properties' ?
            `A capture matches one ${label === 'arguments' ? 'argument' : 'property'}. ` +
            'capture({variadic: true}) matches any number of them.' : undefined;
        const expected: any[] = at(pattern, propertyName) ?? [];
        const actual: any[] = at(target, propertyName) ?? [];
        // A modifier only one side has is marked, and the other side where it would go
        const modifier = label === 'modifiers' && (expected.length === 0) !== (actual.length === 0);
        return this.fail(pattern, 'array-length-mismatch',
            this.describeProperty(propertyName, expected), this.describeProperty(propertyName, actual), hint,
            modifier && expected.length > 0 ? expected[0] : pattern,
            modifier && actual.length > 0 ? actual[0] : target,
            modifier ? {
                pattern: expected.length === 0 ? 'start' : undefined,
                target: actual.length === 0 ? 'start' : undefined
            } : undefined);
    }

    protected override valueMismatch<T extends JTree>(propertyName?: string, expected?: any, actual?: any): T {
        const pattern = this.cursor?.value as any;
        const target = this.targetCursor?.value as any;
        const padded = pattern?.kind === J.Kind.LeftPadded || pattern?.kind === J.Kind.RightPadded;
        if (!propertyName && padded && typeof expected === 'string') {
            const label = this.propertyLabel(undefined);
            const describeOperator = async (tree: any, value: string) => `${label}: ${await operatorOf(tree) ?? value}`;
            const patternHolder = this.cursor?.parent?.value;
            const targetHolder = this.targetCursor?.parent?.value;
            return this.fail(pattern as T, 'value-mismatch', describeOperator(patternHolder, expected),
                describeOperator(targetHolder, actual), undefined, patternHolder, targetHolder,
                {pattern: 'operator', target: 'operator'});
        }
        const path = propertyName ? propertyName.split('.') : [];

        // The mismatch shows on the node holding the property, and a call's method type on the call's name
        const holder = pattern?.kind === J.Kind.MethodInvocation && path[0] === 'methodType' ?
            'name' : path.slice(0, -1).join('.');
        const declared = path[path.length - 1] === 'declaringType';
        const asWritten = (tree: any) => tree?.kind === J.Kind.Literal && (!propertyName || propertyName === 'value') ?
            tree.valueSource : undefined;
        const describeValue = (value: any, tree: any) => declared ? `declared in ${this.formatValue(value)}` :
            asWritten(tree) ?? this.formatValue(value);
        const note = declared ? 'A pattern matches a call only where it calls the same declaration. ' +
            'Its context option declares what its own calls refer to.' : undefined;
        return this.fail(pattern as T, 'value-mismatch',
            describeValue(expected !== undefined ? expected : at(pattern, propertyName), pattern),
            describeValue(actual !== undefined ? actual : at(target, propertyName), target),
            note, at(pattern, holder), at(target, holder));
    }

    protected override satisfiesConstraint(marker: CaptureMarker, patternElement: JTree, target: J | J[],
                                           cursor: Cursor): boolean {
        const capture = Array.isArray(target) ?
            `variadic capture: ${marker.captureName} (${target.length} elements)` : `capture: ${marker.captureName}`;
        this.debug.log('debug', 'constraint', `Evaluating constraint for ${capture}`);
        let satisfied: boolean;
        try {
            satisfied = super.satisfiesConstraint(marker, patternElement, target, cursor);
        } catch (e) {
            const on = Array.isArray(target) ? `${target.length} elements` : describeKind(target);
            const message = (e as Error)?.message ?? e;
            throw new Error(`The constraint of capture ${marker.captureName} threw on ${on}: ${message}`);
        }
        if (satisfied) {
            this.debug.log('debug', 'constraint', `Constraint passed for ${capture}`);
        } else {
            this.debug.log('info', 'constraint', `Constraint failed for ${capture}`);
            if (this.match) {
                const constraint = String(marker.constraint).split('\n')[0];
                this.explain('constraint-failed', `${placeholder(marker)} satisfying ${constraint}`, describe(target),
                    undefined, patternElement,
                    Array.isArray(target) && target.length !== 1 ? this.targetCursor?.value : [target].flat()[0]);
            }
        }
        return satisfied;
    }

    protected override explainCaptureConflict(marker: CaptureMarker, patternElement: JTree, targets: J[]): void {
        const bound = marker.captureName ? this.matcher.getCapture(marker.captureName) : undefined;
        if (bound === undefined || !this.match) {
            return;
        }
        this.explain('value-mismatch',
            describe(bound).then(code => `${code} (what ${placeholder(marker)} holds already)`),
            describe(targets),
            'A capture the pattern names twice binds the same code at both places.',
            patternElement,
            targets.length === 1 ? targets[0] : this.targetCursor?.value);
    }
}
