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

import {Cursor, Tree} from "../tree";
import {Comment, isIdentifier, isJava, isLiteral, J, TextComment, getPaddedElement, isPrimitiveLeftPadded, isPrimitiveRightPadded} from "../java";
import {JS} from "./tree";
import {JavaScriptVisitor} from "./visitor";
import * as fs from "fs";

/**
 * Options for controlling LST debug output.
 *
 * @example
 * // Output to a file instead of console
 * const options: LstDebugOptions = { output: '/tmp/debug.txt' };
 *
 * @example
 * // Minimal output - just the tree structure
 * const options: LstDebugOptions = { includeCursorMessages: false };
 */
export interface LstDebugOptions {
    /** Include cursor messages (like indentContext) in output. Default: true */
    includeCursorMessages?: boolean;
    /** Include markers in output. Default: false */
    includeMarkers?: boolean;
    /** Include node IDs in output. Default: false */
    includeIds?: boolean;
    /** Maximum depth to traverse (for print/recursive methods). Default: unlimited (-1) */
    maxDepth?: number;
    /** Properties to always exclude (in addition to defaults like 'type'). */
    excludeProperties?: string[];
    /** Output destination: 'console' or a file path. Default: 'console' */
    output?: 'console' | string;
    /** Indent string for nested output. Default: '  ' (2 spaces) */
    indent?: string;
}

const DEFAULT_OPTIONS: Required<LstDebugOptions> = {
    includeCursorMessages: true,
    includeMarkers: false,
    includeIds: false,
    maxDepth: -1,
    excludeProperties: [],
    output: 'console',
    indent: '  ',
};

/**
 * Properties to always exclude from debug output (noisy/large).
 */
const EXCLUDED_PROPERTIES = new Set([
    'type',           // JavaType - very verbose
    'methodType',     // JavaType.Method
    'variableType',   // JavaType.Variable
    'fieldType',      // JavaType.Variable
    'constructorType', // JavaType.Method
]);

/**
 * Subscript digits for counts (index 0-9 maps to ₀-₉).
 */
const SUBSCRIPTS = ['₀', '₁', '₂', '₃', '₄', '₅', '₆', '₇', '₈', '₉'];

/**
 * Format a count as subscript. Count of 1 is implicit (empty string).
 */
function subscript(count: number): string {
    if (count <= 1) return '';
    if (count < 10) return SUBSCRIPTS[count];
    // For counts >= 10, build from individual digits
    return String(count).split('').map(d => SUBSCRIPTS[parseInt(d)]).join('');
}

/**
 * Format whitespace string for readable debug output.
 * Uses compact notation with subscript counts:
 * - '\n' = 1 newline (implicit ₁)
 * - '\n₂' = 2 newlines
 * - '·₄' = 4 spaces
 * - '-₂' = 2 tabs
 * - '\n·₄' = newline + 4 spaces
 * - '\n·₄-₂' = newline + 4 spaces + 2 tabs
 */
export function formatWhitespace(whitespace: string | undefined): string {
    if (whitespace === undefined || whitespace === '') {
        return "''";
    }

    let result = '';
    let newlineCount = 0;
    let spaceCount = 0;
    let tabCount = 0;

    const flushCounts = () => {
        if (newlineCount > 0) {
            result += '\\n' + subscript(newlineCount);
            newlineCount = 0;
        }
        if (spaceCount > 0) {
            result += '·' + subscript(spaceCount);
            spaceCount = 0;
        }
        if (tabCount > 0) {
            result += '-' + subscript(tabCount);
            tabCount = 0;
        }
    };

    for (let i = 0; i < whitespace.length; i++) {
        const c = whitespace[i];
        if (c === '\n') {
            // Flush spaces/tabs before starting newline count
            if (spaceCount > 0 || tabCount > 0) {
                flushCounts();
            }
            newlineCount++;
        } else if (c === '\r') {
            flushCounts();
            result += '\\r';
        } else if (c === ' ') {
            // Flush newlines and tabs before counting spaces
            if (newlineCount > 0) {
                result += '\\n' + subscript(newlineCount);
                newlineCount = 0;
            }
            if (tabCount > 0) {
                result += '-' + subscript(tabCount);
                tabCount = 0;
            }
            spaceCount++;
        } else if (c === '\t') {
            // Flush newlines and spaces before counting tabs
            if (newlineCount > 0) {
                result += '\\n' + subscript(newlineCount);
                newlineCount = 0;
            }
            if (spaceCount > 0) {
                result += '·' + subscript(spaceCount);
                spaceCount = 0;
            }
            tabCount++;
        } else {
            flushCounts();
            // Unexpected character (probably a bug in the parser)
            result += c;
        }
    }

    flushCounts();

    return `'${result}'`;
}

/**
 * Format a single comment for debug output.
 */
function formatComment(comment: Comment): string {
    const textComment = comment as TextComment;
    const text = textComment.text ?? '';
    // Truncate long comments
    const truncated = text.length > 20 ? text.substring(0, 17) + '...' : text;

    if (textComment.multiline) {
        return `/*${truncated}*/`;
    } else {
        return `//${truncated}`;
    }
}

/**
 * Format a J.Space for debug output.
 *
 * Compact format:
 * - Empty space: `''`
 * - Whitespace only: `'\n·₄'`
 * - Comment only: `//comment`
 * - Comment with suffix: `//comment'\n'`
 * - Multiple comments: `//c1'\n' + //c2'\n·₄'`
 */
export function formatSpace(space: J.Space | undefined): string {
    if (space === undefined) {
        return '<undefined>';
    }

    const hasComments = space.comments.length > 0;
    const hasWhitespace = space.whitespace !== undefined && space.whitespace !== '';

    // Completely empty
    if (!hasComments && !hasWhitespace) {
        return "''";
    }

    // Whitespace only (common case) - just show the formatted whitespace
    if (!hasComments) {
        return formatWhitespace(space.whitespace);
    }

    // Format comments with their suffixes
    const parts: string[] = [];
    for (const comment of space.comments) {
        let part = formatComment(comment);
        // Add suffix if present
        if (comment.suffix && comment.suffix !== '') {
            part += formatWhitespace(comment.suffix);
        }
        parts.push(part);
    }

    // Add trailing whitespace if present
    if (hasWhitespace) {
        parts.push(formatWhitespace(space.whitespace));
    }

    return parts.join(' + ');
}

/**
 * Format cursor messages for debug output.
 * Returns '<no messages>' if no messages, otherwise returns '⟨key=value, ...⟩'
 */
export function formatCursorMessages(cursor: Cursor | undefined): string {
    if (!cursor || cursor.messages.size === 0) {
        return '<no messages>';
    }

    const entries: string[] = [];
    cursor.messages.forEach((value, key) => {
        let valueStr: string;
        if (Array.isArray(value)) {
            valueStr = `[${value.map(v => JSON.stringify(v)).join(', ')}]`;
        } else if (value instanceof Set) {
            // Handle Set - convert to array notation
            const items = Array.from(value).map(v => JSON.stringify(v)).join(', ');
            valueStr = `{${items}}`;
        } else if (typeof value === 'object' && value !== null) {
            valueStr = JSON.stringify(value);
        } else {
            valueStr = String(value);
        }
        const keyStr = typeof key === 'symbol' ? key.toString() : String(key);
        entries.push(`${keyStr}=${valueStr}`);
    });

    return `⟨${entries.join(', ')}⟩`;
}

/**
 * Get a short type name from a kind string.
 */
function shortTypeName(kind: string | undefined): string {
    if (!kind) return 'Unknown';
    // Extract the last part after the last dot
    const lastDot = kind.lastIndexOf('.');
    return lastDot >= 0 ? kind.substring(lastDot + 1) : kind;
}

/**
 * Format markers for debug output.
 * Returns empty string if no markers, otherwise returns ' markers=[Name1, Name2]'
 */
function formatMarkers(node: any): string {
    const markers = node?.markers?.markers;
    if (!markers || !Array.isArray(markers) || markers.length === 0) {
        return '';
    }
    const names = markers.map((m: any) => shortTypeName(m.kind));
    return ` markers=[${names.join(', ')}]`;
}

/**
 * Find which property of the parent contains the given child element.
 * Returns the property name, or property name with array index if in an array.
 * Returns undefined if the relationship cannot be determined.
 *
 * @param cursor - The cursor at the current position
 * @param child - Optional: the actual child node being visited (for RightPadded/LeftPadded/Container visits where cursor.value is the parent)
 */
export function findPropertyPath(cursor: Cursor | undefined, child?: any): string | undefined {
    if (!cursor) {
        return undefined;
    }

    // The child we're looking for
    const actualChild = child ?? cursor.value;

    // Find the parent that should contain this child as a property.
    // When child is explicitly provided (e.g., from visitRightPadded), cursor.value IS the parent.
    // When child is not provided, we're looking for cursor.value in cursor.parent.value.
    let parent = child ? cursor.value : cursor.parent?.value;

    if (!parent || typeof parent !== 'object') {
        return undefined;
    }

    // Properties to skip when searching
    const skipProps = new Set(['kind', 'id', 'prefix', 'markers', 'type', 'methodType', 'variableType', 'fieldType', 'constructorType']);

    // Helper to compare elements - use id comparison for Tree nodes (handles immutability)
    const sameElement = (a: any, b: any): boolean => {
        if (a === b) return true;
        // If both have 'id' property (Tree nodes), compare by id
        if (a && b && typeof a === 'object' && typeof b === 'object' && 'id' in a && 'id' in b) {
            return a.id === b.id;
        }
        return false;
    };

    // Special case: if parent is a Container, we need to look at grandparent to find the property name
    if ((parent as any).kind === J.Kind.Container) {
        const container = parent as J.Container<any>;
        const grandparent = cursor.parent?.parent?.value;

        // Find the index of actualChild in container.elements
        let childIndex = -1;
        if (container.elements) {
            for (let i = 0; i < container.elements.length; i++) {
                if (sameElement(container.elements[i], actualChild)) {
                    childIndex = i;
                    break;
                }
            }
        }

        // Find which property of grandparent holds this container
        if (grandparent && typeof grandparent === 'object') {
            for (const [key, value] of Object.entries(grandparent)) {
                if (skipProps.has(key)) continue;
                if (sameElement(value, parent)) {
                    if (childIndex >= 0) {
                        return `${key}[${childIndex}]`;
                    }
                    return key;
                }
            }
        }

        // Fallback: just show the index
        if (childIndex >= 0) {
            return `[${childIndex}]`;
        }
    }

    for (const [key, value] of Object.entries(parent)) {
        if (skipProps.has(key)) continue;

        // Direct match
        if (sameElement(value, actualChild)) {
            return key;
        }

        // Check if child is in an array
        if (Array.isArray(value)) {
            for (let i = 0; i < value.length; i++) {
                if (sameElement(value[i], actualChild)) {
                    return `${key}[${i}]`;
                }
                // Check inside RightPadded/LeftPadded wrappers
                if (value[i] && typeof value[i] === 'object') {
                    if (sameElement(value[i].element, actualChild)) {
                        return `${key}[${i}].element`;
                    }
                }
            }
        }

        // Check inside Container
        if (value && typeof value === 'object' && (value as any).kind === J.Kind.Container) {
            const container = value as J.Container<any>;
            if (container.elements) {
                for (let i = 0; i < container.elements.length; i++) {
                    const rp = container.elements[i];
                    if (sameElement(rp, actualChild)) {
                        return `${key}[${i}]`;
                    }
                    if (sameElement(rp.element, actualChild)) {
                        return `${key}[${i}].element`;
                    }
                }
            }
        }

        // Check inside LeftPadded/RightPadded
        // With new type system: primitives have .element property, intersection types don't
        if (value && typeof value === 'object' && 'padding' in value) {
            const padding = (value as any).padding;
            const isPadded = padding && typeof padding === 'object' &&
                ('before' in padding || 'after' in padding);
            if (isPadded && 'element' in value) {
                // Primitive wrapper - check .element
                if (sameElement((value as any).element, actualChild)) {
                    return `${key}.element`;
                }
            }
        }
    }

    return undefined;
}

/**
 * Escape special characters in a string for display.
 */
function escapeString(str: string): string {
    return str
        .replace(/\\/g, '\\\\')
        .replace(/\n/g, '\\n')
        .replace(/\r/g, '\\r')
        .replace(/\t/g, '\\t');
}

/**
 * Format a literal value for inline display.
 */
function formatLiteralValue(lit: J.Literal): string {
    let value: string;
    if (lit.valueSource !== undefined) {
        value = lit.valueSource;
    } else {
        value = String(lit.value);
    }
    // Escape special characters
    value = escapeString(value);
    // Truncate long literals
    return value.length > 20 ? value.substring(0, 17) + '...' : value;
}

/**
 * Get a compact inline summary for certain node types.
 * - For Identifier: shows "simpleName"
 * - For Literal: shows the value
 * - For other nodes: shows all Identifier/Literal properties inline
 */
function getNodeSummary(node: J): string | undefined {
    if (isIdentifier(node)) {
        return `"${node.simpleName}"`;
    }
    if (isLiteral(node)) {
        return formatLiteralValue(node);
    }

    // For other nodes, find all Identifier and Literal properties
    const parts: string[] = [];
    const skipProps = new Set(['kind', 'id', 'prefix', 'markers', 'type', 'methodType', 'variableType', 'fieldType', 'constructorType']);

    for (const [key, value] of Object.entries(node)) {
        if (skipProps.has(key)) continue;
        if (value === null || value === undefined) continue;

        if (isIdentifier(value)) {
            parts.push(`${key}='${value.simpleName}'`);
        } else if (isLiteral(value)) {
            parts.push(`${key}=${formatLiteralValue(value)}`);
        }
    }

    return parts.length > 0 ? parts.join(' ') : undefined;
}

/**
 * LST Debug Printer - prints LST nodes in a readable format.
 *
 * This is a STATEFUL object that tracks cursor depth across calls to provide
 * proper indentation. Create one instance as a field in your visitor and reuse it.
 *
 * Two main methods:
 * - `log()`: Prints a single node WITHOUT recursing into children. Tracks cursor
 *   hierarchy across calls to show proper indentation.
 * - `print()`: Prints a node AND all its children recursively.
 *
 * Usage from within a visitor (recommended pattern):
 * ```typescript
 * class TabsAndIndentsVisitor extends JavaScriptVisitor<P> {
 *     // Create as a field - it tracks state across calls
 *     private debug = new LstDebugPrinter();
 *
 *     async visitBlock(block: J.Block, p: P) {
 *         // Log this node with automatic indentation based on cursor depth
 *         this.debug.log(block, this.cursor, "visiting block");
 *         return super.visitBlock(block, p);
 *     }
 *
 *     async visitMethodInvocation(mi: J.MethodInvocation, p: P) {
 *         this.debug.log(mi, this.cursor);
 *         return super.visitMethodInvocation(mi, p);
 *     }
 * }
 * ```
 *
 * Output will be properly indented based on tree depth:
 * ```
 * CompilationUnit{prefix=''}
 *   statements[0]: ClassDeclaration{name='Foo' prefix=''}
 *     body: Block{prefix='·'}
 *       // visiting block
 *       statements[0]: MethodDeclaration{name='bar' prefix='\n·₄'}
 * ```
 *
 * To reset indentation tracking (e.g., between files):
 * ```typescript
 * this.debug.reset();
 * ```
 *
 * To print an entire subtree with recursion:
 * ```typescript
 * this.debug.print(subtree, this.cursor, "dumping subtree");
 * ```
 */
export class LstDebugPrinter {
    private readonly options: Required<LstDebugOptions>;
    private outputLines: string[] = [];

    /**
     * Cache of cursor depth to avoid recalculating.
     * Uses WeakMap so cursors can be garbage collected.
     */
    private depthCache = new WeakMap<Cursor, number>();

    constructor(options: LstDebugOptions = {}) {
        this.options = {...DEFAULT_OPTIONS, ...options};
        // Truncate output file at start of session
        if (this.options.output !== 'console') {
            fs.writeFileSync(this.options.output, '');
        }
    }

    /**
     * Clear the depth cache. Call this when starting a new tree
     * to free memory from previous traversals.
     */
    reset(): void {
        this.depthCache = new WeakMap<Cursor, number>();
    }

    /**
     * Log a single node WITHOUT recursing into children.
     * Use this from visitor methods to log individual nodes as they are visited.
     *
     * When called with a cursor, tracks the cursor hierarchy across calls to
     * provide proper indentation showing the tree structure.
     *
     * Output format: `// label` (if provided), then indented `TypeName{summary prefix=...}`
     * with cursor messages on a separate line if present.
     *
     * @param node The node to log
     * @param cursor Optional cursor for context, messages, and depth tracking
     * @param label Optional label to identify this log entry
     */
    log(node: Tree | J.Container<any> | J.LeftPadded<any> | J.RightPadded<any>, cursor?: Cursor, label?: string): void {
        this.outputLines = [];

        // Calculate indentation based on cursor depth
        const depth = this.calculateDepth(cursor);
        const indent = this.options.indent.repeat(depth);

        if (label) {
            this.outputLines.push(`${indent}// ${label}`);
        }

        // Build single-line output for the node
        let line = indent;

        // Find property path from cursor
        // When node === cursor.value, don't pass node - we want to find cursor.value in cursor.parent.value
        // When node !== cursor.value (e.g., for RightPadded/LeftPadded/Container), pass node to find it in cursor.value
        const propPath = findPropertyPath(cursor, node === cursor?.value ? undefined : node);
        if (propPath) {
            line += `${propPath}: `;
        }

        if (this.isContainer(node)) {
            const container = node as J.Container<any>;
            const before = formatSpace(container.before);
            line += `Container<${container.elements?.length ?? 0}>{before=${before}${formatMarkers(container)}}`;
        } else if (this.isLeftPadded(node)) {
            const lp = node as any;
            const before = formatSpace(lp.padding.before);
            line += `LeftPadded{before=${before}`;
            // Show element value if it's a primitive (has .element property)
            if ('element' in lp && lp.element !== null && lp.element !== undefined) {
                const elemType = typeof lp.element;
                if (elemType === 'string' || elemType === 'number' || elemType === 'boolean') {
                    line += ` element=${JSON.stringify(lp.element)}`;
                }
            }
            // Use padding.markers for marker formatting
            line += `${formatMarkers({markers: lp.padding.markers})}}`;
        } else if (this.isRightPadded(node)) {
            const rp = node as any;
            const after = formatSpace(rp.padding.after);
            line += `RightPadded{after=${after}`;
            // Show element value if it's a primitive (has .element property)
            if ('element' in rp && rp.element !== null && rp.element !== undefined) {
                const elemType = typeof rp.element;
                if (elemType === 'string' || elemType === 'number' || elemType === 'boolean') {
                    line += ` element=${JSON.stringify(rp.element)}`;
                }
            }
            // Use padding.markers for marker formatting
            line += `${formatMarkers({markers: rp.padding.markers})}}`;
        } else if (isJava(node)) {
            const jNode = node as J;
            const typeName = shortTypeName(jNode.kind);
            line += `${typeName}{`;
            const summary = getNodeSummary(jNode);
            if (summary) {
                line += `${summary} `;
            }
            line += `prefix=${formatSpace(jNode.prefix)}${formatMarkers(jNode)}}`;
            // For intersection-padded J nodes, also show the padding info
            const paddingInfo = this.isIntersectionPadded(node);
            if (paddingInfo) {
                const padding = (node as any).padding;
                if (paddingInfo.isLeft) {
                    line += ` (LeftPadded before=${formatSpace(padding.before)})`;
                }
                if (paddingInfo.isRight) {
                    line += ` (RightPadded after=${formatSpace(padding.after)})`;
                }
            }
        } else {
            line += `<unknown: ${typeof node}>`;
        }

        this.outputLines.push(line);

        // Add cursor messages on separate line if present
        if (this.options.includeCursorMessages && cursor) {
            const messages = formatCursorMessages(cursor);
            if (messages !== '<no messages>') {
                this.outputLines.push(`${indent}  ⤷ ${messages}`);
            }
        }

        this.flush();
    }

    /**
     * Print a tree node AND all its children recursively.
     * Use this to dump an entire subtree structure.
     *
     * @param tree The tree node to print
     * @param cursor Optional cursor for context
     * @param label Optional label to identify this debug output (shown as comment before output)
     */
    print(tree: Tree | J.Container<any> | J.LeftPadded<any> | J.RightPadded<any>, cursor?: Cursor, label?: string): void {
        this.outputLines = [];
        if (label) {
            this.outputLines.push(`// ${label}`);
        }
        this.printNode(tree, cursor, 0);
        this.flush();
    }

    /**
     * Print the cursor path from root to current position.
     */
    printCursorPath(cursor: Cursor): void {
        this.outputLines = [];
        this.outputLines.push('=== Cursor Path ===');

        const path: Cursor[] = [];
        for (let c: Cursor | undefined = cursor; c; c = c.parent) {
            path.unshift(c);
        }

        for (let i = 0; i < path.length; i++) {
            const c = path[i];
            const indent = this.options.indent.repeat(i);
            const kind = (c.value as any)?.kind;
            const typeName = shortTypeName(kind);

            let line = `${indent}${typeName}`;
            if (this.options.includeCursorMessages && c.messages.size > 0) {
                line += ` [${formatCursorMessages(c)}]`;
            }
            this.outputLines.push(line);
        }

        this.flush();
    }

    /**
     * Calculate the depth of the cursor by counting parent chain length.
     * Uses caching to avoid repeated traversals.
     */
    private calculateDepth(cursor: Cursor | undefined): number {
        if (!cursor) {
            return 0;
        }

        // Check cache first
        const cached = this.depthCache.get(cursor);
        if (cached !== undefined) {
            return cached;
        }

        // Count depth by walking parent chain
        let depth = 0;
        for (let c: Cursor | undefined = cursor; c; c = c.parent) {
            depth++;
        }
        // Subtract 2 to skip root cursor and start CompilationUnit at depth 0
        depth = Math.max(0, depth - 2);

        // Cache the result
        this.depthCache.set(cursor, depth);
        return depth;
    }

    private printNode(
        node: any,
        cursor: Cursor | undefined,
        depth: number
    ): void {
        if (this.options.maxDepth >= 0 && depth > this.options.maxDepth) {
            this.outputLines.push(`${this.indent(depth)}...`);
            return;
        }

        if (node === null || node === undefined) {
            this.outputLines.push(`${this.indent(depth)}null`);
            return;
        }

        // Handle special types
        if (this.isSpace(node)) {
            this.outputLines.push(`${this.indent(depth)}${formatSpace(node)}`);
            return;
        }

        if (this.isContainer(node)) {
            this.printContainer(node, cursor, depth);
            return;
        }

        if (this.isLeftPadded(node)) {
            this.printLeftPadded(node, cursor, depth);
            return;
        }

        if (this.isRightPadded(node)) {
            this.printRightPadded(node, cursor, depth);
            return;
        }

        if (isJava(node)) {
            this.printJavaNode(node, cursor, depth);
            return;
        }

        // Primitive or unknown type
        if (typeof node !== 'object') {
            this.outputLines.push(`${this.indent(depth)}${JSON.stringify(node)}`);
            return;
        }

        // Generic object - print as JSON-like
        this.printGenericObject(node, depth);
    }

    private printJavaNode(node: J, cursor: Cursor | undefined, depth: number): void {
        const typeName = shortTypeName(node.kind);
        let header = `${this.indent(depth)}${typeName}`;

        // Add inline summary for certain types (Identifier, Literal)
        const summary = getNodeSummary(node);
        if (summary) {
            header += ` ${summary}`;
        }

        // Add cursor messages if available
        if (this.options.includeCursorMessages && cursor) {
            const messages = formatCursorMessages(cursor);
            if (messages !== '<no messages>') {
                header += ` [${messages}]`;
            }
        }

        // Add ID if requested
        if (this.options.includeIds && node.id) {
            header += ` (id=${node.id.substring(0, 8)}...)`;
        }

        this.outputLines.push(header);

        // Print prefix
        if (node.prefix) {
            this.outputLines.push(`${this.indent(depth + 1)}prefix: ${formatSpace(node.prefix)}`);
        }

        // Print markers if requested
        if (this.options.includeMarkers && node.markers?.markers?.length > 0) {
            this.outputLines.push(`${this.indent(depth + 1)}markers: [${node.markers.markers.map((m: any) => shortTypeName(m.kind)).join(', ')}]`);
        }

        // Print padding info for intersection-padded J nodes
        const paddingInfo = this.isIntersectionPadded(node);
        if (paddingInfo) {
            const padding = (node as any).padding;
            if (paddingInfo.isLeft) {
                this.outputLines.push(`${this.indent(depth + 1)}(LeftPadded) before: ${formatSpace(padding.before)}`);
            }
            if (paddingInfo.isRight) {
                this.outputLines.push(`${this.indent(depth + 1)}(RightPadded) after: ${formatSpace(padding.after)}`);
            }
            // Show padding markers if present and different from node markers
            if (padding.markers?.markers?.length > 0) {
                this.outputLines.push(`${this.indent(depth + 1)}(padding) markers: [${padding.markers.markers.map((m: any) => shortTypeName(m.kind)).join(', ')}]`);
            }
        }

        // Print other properties
        this.printNodeProperties(node, cursor, depth + 1);
    }

    private printNodeProperties(node: any, cursor: Cursor | undefined, depth: number): void {
        const excludedProps = new Set([
            ...EXCLUDED_PROPERTIES,
            ...this.options.excludeProperties,
            'kind',
            'id',
            'prefix',
            'markers',
            'padding', // Handled specially in printJavaNode
        ]);

        for (const [key, value] of Object.entries(node)) {
            if (excludedProps.has(key)) continue;
            if (value === undefined || value === null) continue;

            if (this.isSpace(value)) {
                this.outputLines.push(`${this.indent(depth)}${key}: ${formatSpace(value)}`);
            } else if (this.isContainer(value)) {
                this.outputLines.push(`${this.indent(depth)}${key}:`);
                this.printContainer(value, undefined, depth + 1);
            } else if (this.isLeftPadded(value)) {
                this.outputLines.push(`${this.indent(depth)}${key}:`);
                this.printLeftPadded(value, undefined, depth + 1);
            } else if (this.isRightPadded(value)) {
                this.outputLines.push(`${this.indent(depth)}${key}:`);
                this.printRightPadded(value, undefined, depth + 1);
            } else if (Array.isArray(value)) {
                // Use explicit any[] cast to avoid TypeScript narrowing issues with every()
                const arr = value as any[];
                const arrLen = arr.length;
                if (arrLen === 0) {
                    this.outputLines.push(`${this.indent(depth)}${key}: []`);
                } else if (arr.every((v: any) => this.isRightPadded(v))) {
                    this.outputLines.push(`${this.indent(depth)}${key}: [${arrLen} RightPadded elements]`);
                    for (let i = 0; i < arrLen; i++) {
                        this.outputLines.push(`${this.indent(depth + 1)}[${i}]:`);
                        this.printRightPadded(arr[i] as J.RightPadded<any>, undefined, depth + 2);
                    }
                } else if (arr.every((v: any) => isJava(v))) {
                    this.outputLines.push(`${this.indent(depth)}${key}: [${arrLen} elements]`);
                    for (let i = 0; i < arrLen; i++) {
                        this.outputLines.push(`${this.indent(depth + 1)}[${i}]:`);
                        this.printNode(arr[i] as J, undefined, depth + 2);
                    }
                } else {
                    this.outputLines.push(`${this.indent(depth)}${key}: [${arrLen} items]`);
                }
            } else if (isJava(value)) {
                this.outputLines.push(`${this.indent(depth)}${key}:`);
                this.printNode(value, undefined, depth + 1);
            } else if (typeof value === 'object') {
                // Skip complex objects that aren't J nodes
                this.outputLines.push(`${this.indent(depth)}${key}: <object>`);
            } else {
                this.outputLines.push(`${this.indent(depth)}${key}: ${JSON.stringify(value)}`);
            }
        }
    }

    private printContainer(container: J.Container<any>, cursor: Cursor | undefined, depth: number): void {
        const elemCount = container.elements?.length ?? 0;
        let header = `${this.indent(depth)}Container<${elemCount} elements>`;

        if (this.options.includeCursorMessages && cursor) {
            const messages = formatCursorMessages(cursor);
            if (messages !== '<no messages>') {
                header += ` [${messages}]`;
            }
        }

        this.outputLines.push(header);
        this.outputLines.push(`${this.indent(depth + 1)}before: ${formatSpace(container.before)}`);

        if (container.elements && container.elements.length > 0) {
            this.outputLines.push(`${this.indent(depth + 1)}elements:`);
            for (let i = 0; i < container.elements.length; i++) {
                this.outputLines.push(`${this.indent(depth + 2)}[${i}]:`);
                this.printRightPadded(container.elements[i], undefined, depth + 3);
            }
        }
    }

    private printLeftPadded(lp: J.LeftPadded<any>, cursor: Cursor | undefined, depth: number): void {
        let header = `${this.indent(depth)}LeftPadded`;

        if (this.options.includeCursorMessages && cursor) {
            const messages = formatCursorMessages(cursor);
            if (messages !== '<no messages>') {
                header += ` [${messages}]`;
            }
        }

        this.outputLines.push(header);
        this.outputLines.push(`${this.indent(depth + 1)}before: ${formatSpace((lp as any).padding.before)}`);

        // With the new type system:
        // - For tree types (J): The padded value IS the tree with padding mixed in (no .element)
        // - For primitives: There's an .element property
        const hasPrimitiveElement = 'element' in lp;
        const element = hasPrimitiveElement ? (lp as any).element : lp;

        if (hasPrimitiveElement) {
            // Primitive wrapper case
            const primitiveElement = (lp as any).element;
            if (this.isSpace(primitiveElement)) {
                this.outputLines.push(`${this.indent(depth + 1)}element: ${formatSpace(primitiveElement)}`);
            } else {
                this.outputLines.push(`${this.indent(depth + 1)}element: ${JSON.stringify(primitiveElement)}`);
            }
        } else if (isJava(element)) {
            // Tree type (intersection type) - element IS the padded value
            this.outputLines.push(`${this.indent(depth + 1)}element:`);
            this.printNode(element, undefined, depth + 2);
        }
    }

    private printRightPadded(rp: J.RightPadded<any>, cursor: Cursor | undefined, depth: number): void {
        let header = `${this.indent(depth)}RightPadded`;

        if (this.options.includeCursorMessages && cursor) {
            const messages = formatCursorMessages(cursor);
            if (messages !== '<no messages>') {
                header += ` [${messages}]`;
            }
        }

        this.outputLines.push(header);

        // With the new type system:
        // - For tree types (J): The padded value IS the tree with padding mixed in (no .element)
        // - For primitives (like boolean): There's an .element property
        const hasPrimitiveElement = 'element' in rp;
        const element = hasPrimitiveElement ? (rp as any).element : rp;

        if (hasPrimitiveElement) {
            // Primitive wrapper case
            this.outputLines.push(`${this.indent(depth + 1)}element: ${JSON.stringify((rp as any).element)}`);
        } else if (isJava(element)) {
            // Tree type (intersection type) - element IS the padded value
            this.outputLines.push(`${this.indent(depth + 1)}element:`);
            this.printNode(element, undefined, depth + 2);
        }

        this.outputLines.push(`${this.indent(depth + 1)}after: ${formatSpace((rp as any).padding.after)}`);
    }

    private printGenericObject(obj: any, depth: number): void {
        this.outputLines.push(`${this.indent(depth)}{`);
        for (const [key, value] of Object.entries(obj)) {
            if (typeof value === 'object' && value !== null) {
                this.outputLines.push(`${this.indent(depth + 1)}${key}: <object>`);
            } else {
                this.outputLines.push(`${this.indent(depth + 1)}${key}: ${JSON.stringify(value)}`);
            }
        }
        this.outputLines.push(`${this.indent(depth)}}`);
    }

    private isSpace(value: any): value is J.Space {
        return value !== null &&
            typeof value === 'object' &&
            'whitespace' in value &&
            'comments' in value &&
            !('kind' in value);
    }

    private isContainer(value: any): value is J.Container<any> {
        return value !== null &&
            typeof value === 'object' &&
            value.kind === J.Kind.Container;
    }

    private isLeftPadded(value: any): boolean {
        return isPrimitiveLeftPadded(value);
    }

    private isRightPadded(value: any): boolean {
        return isPrimitiveRightPadded(value);
    }

    /**
     * Check if a value is an intersection-padded J node (has padding but no .element wrapper).
     */
    private isIntersectionPadded(value: any): { isLeft: boolean; isRight: boolean } | false {
        if (value === null || typeof value !== 'object') return false;
        if (!('padding' in value) || !value.padding || typeof value.padding !== 'object') return false;
        if ('element' in value) return false; // Primitive wrapper, not intersection
        const hasLeft = 'before' in value.padding;
        const hasRight = 'after' in value.padding;
        if (hasLeft || hasRight) {
            return { isLeft: hasLeft, isRight: hasRight };
        }
        return false;
    }

    private indent(depth: number): string {
        return this.options.indent.repeat(depth);
    }

    private flush(): void {
        const output = this.outputLines.join('\n');

        if (this.options.output === 'console') {
            console.info(output);
        } else {
            fs.appendFileSync(this.options.output, output + '\n');
        }

        this.outputLines = [];
    }
}

/**
 * A visitor that prints the LST structure as it traverses, showing each node
 * with proper indentation to visualize the tree hierarchy.
 *
 * Use this to print an entire tree or subtree with full traversal. Each node
 * is printed as it's visited, with indentation showing the tree depth.
 *
 * For logging individual nodes from within your own visitor without recursion,
 * use `LstDebugPrinter.log()` or `debugLog()` instead.
 *
 * Usage:
 * ```typescript
 * // Print entire tree structure during traversal
 * const debugVisitor = new LstDebugVisitor();
 * await debugVisitor.visit(tree, ctx);
 *
 * // With options
 * const debugVisitor = new LstDebugVisitor(
 *     { includeCursorMessages: true },
 *     { printPreVisit: true, printPostVisit: false }
 * );
 * await debugVisitor.visit(subtree, ctx);
 * ```
 */
export class LstDebugVisitor<P> extends JavaScriptVisitor<P> {
    private readonly printer: LstDebugPrinter;
    private readonly printPreVisit: boolean;
    private readonly printPostVisit: boolean;
    private depth = 0;

    constructor(
        options: LstDebugOptions = {},
        config: { printPreVisit?: boolean; printPostVisit?: boolean } = {}
    ) {
        super();
        this.printer = new LstDebugPrinter(options);
        this.printPreVisit = config.printPreVisit ?? true;
        this.printPostVisit = config.printPostVisit ?? false;
    }

    public async visitContainer<T extends J>(container: J.Container<T>, p: P): Promise<J.Container<T>> {
        if (this.printPreVisit) {
            const indent = '  '.repeat(this.depth);
            const messages = formatCursorMessages(this.cursor);
            const before = formatSpace(container.before);
            // Pass container as the child since cursor.value is the parent node
            const propPath = findPropertyPath(this.cursor, container);

            let line = indent;
            if (propPath) {
                line += `${propPath}: `;
            }
            line += `Container<${container.elements.length}>{before=${before}${formatMarkers(container)}}`;

            // Append cursor messages on same line to avoid empty line issues
            if (messages !== '<no messages>') {
                line += ` ${messages}`;
            }
            console.info(line);
        }
        this.depth++;
        const result = await super.visitContainer(container, p);
        this.depth--;
        return result;
    }

    public async visitLeftPadded<T extends J | J.Space | number | string | boolean>(
        left: J.LeftPadded<T>,
        p: P
    ): Promise<J.LeftPadded<T> | undefined> {
        if (this.printPreVisit) {
            const indent = '  '.repeat(this.depth);
            const messages = formatCursorMessages(this.cursor);
            const before = formatSpace(left.padding.before);
            // Pass left as the child since cursor.value is the parent node
            const propPath = findPropertyPath(this.cursor, left);

            let line = indent;
            if (propPath) {
                line += `${propPath}: `;
            }
            line += `LeftPadded{before=${before}`;

            // Show element value if it's a primitive (string, number, boolean)
            // With new type system: primitives have .element, tree types don't
            const hasPrimitiveElement = 'element' in left;
            if (hasPrimitiveElement) {
                const elem = (left as any).element;
                const elemType = typeof elem;
                if (elemType === 'string' || elemType === 'number' || elemType === 'boolean') {
                    line += ` element=${JSON.stringify(elem)}`;
                }
            }
            line += `${formatMarkers({markers: left.padding.markers})}}`;

            // Append cursor messages on same line to avoid empty line issues
            if (messages !== '<no messages>') {
                line += ` ${messages}`;
            }
            console.info(line);
        }
        this.depth++;
        const result = await super.visitLeftPadded(left, p);
        this.depth--;
        return result;
    }

    public async visitRightPadded<T extends J | boolean>(
        right: J.RightPadded<T>,
        p: P
    ): Promise<J.RightPadded<T> | undefined> {
        if (this.printPreVisit) {
            const indent = '  '.repeat(this.depth);
            const messages = formatCursorMessages(this.cursor);
            const after = formatSpace(right.padding.after);
            // Pass right as the child since cursor.value is the parent node
            const propPath = findPropertyPath(this.cursor, right);

            let line = indent;
            if (propPath) {
                line += `${propPath}: `;
            }
            line += `RightPadded{after=${after}`;

            // Show element value if it's a primitive (string, number, boolean)
            // With new type system: primitives have .element, tree types don't
            const hasPrimitiveElement = 'element' in right;
            if (hasPrimitiveElement) {
                const elem = (right as any).element;
                const elemType = typeof elem;
                if (elemType === 'string' || elemType === 'number' || elemType === 'boolean') {
                    line += ` element=${JSON.stringify(elem)}`;
                }
            }
            line += `${formatMarkers({markers: right.padding.markers})}}`;

            // Append cursor messages on same line to avoid empty line issues
            if (messages !== '<no messages>') {
                line += ` ${messages}`;
            }
            console.info(line);
        }
        this.depth++;
        const result = await super.visitRightPadded(right, p);
        this.depth--;
        return result;
    }

    protected async preVisit(tree: J, _p: P): Promise<J | undefined> {
        if (this.printPreVisit) {
            const typeName = shortTypeName(tree.kind);
            const indent = '  '.repeat(this.depth);
            const messages = formatCursorMessages(this.cursor);
            const prefix = formatSpace(tree.prefix);
            const summary = getNodeSummary(tree);
            const propPath = findPropertyPath(this.cursor);

            let line = indent;
            if (propPath) {
                line += `${propPath}: `;
            }
            line += `${typeName}{`;
            if (summary) {
                line += `${summary} `;
            }
            line += `prefix=${prefix}${formatMarkers(tree)}}`;

            // Append cursor messages on same line to avoid empty line issues
            if (messages !== '<no messages>') {
                line += ` ${messages}`;
            }
            console.info(line);
        }
        this.depth++;
        return tree;
    }

    protected async postVisit(tree: J, _p: P): Promise<J | undefined> {
        this.depth--;
        if (this.printPostVisit) {
            const typeName = shortTypeName(tree.kind);
            const indent = '  '.repeat(this.depth);
            console.info(`${indent}← ${typeName}`);
        }
        return tree;
    }
}

/**
 * Convenience function to log a single node (no recursion).
 * Use this from visitor methods to log individual nodes as they are visited.
 *
 * @param node The node to log
 * @param cursor Optional cursor for context and messages
 * @param label Optional label to identify this log entry
 * @param options Optional debug options
 */
export function debugLog(node: Tree, cursor?: Cursor, label?: string, options?: LstDebugOptions): void {
    new LstDebugPrinter(options).log(node, cursor, label);
}

/**
 * Convenience function to print a tree node AND all its children recursively.
 * Use this to dump an entire subtree structure.
 *
 * @param tree The tree node to print
 * @param cursor Optional cursor for context
 * @param label Optional label to identify this debug output
 * @param options Optional debug options
 */
export function debugPrint(tree: Tree, cursor?: Cursor, label?: string, options?: LstDebugOptions): void {
    new LstDebugPrinter(options).print(tree, cursor, label);
}

/**
 * Convenience function to print cursor path.
 */
export function debugPrintCursorPath(cursor: Cursor, options?: LstDebugOptions): void {
    new LstDebugPrinter(options).printCursorPath(cursor);
}

/**
 * Create a debug printer if debugging is enabled, otherwise return undefined.
 *
 * This is useful for visitors that want to optionally enable debugging via
 * constructor parameters or configuration.
 *
 * @param enabled Whether debugging is enabled
 * @param options Debug options (including output file path)
 * @returns LstDebugPrinter if enabled, undefined otherwise
 *
 * @example
 * class MyVisitor extends JavaScriptVisitor<P> {
 *     private debug?: LstDebugPrinter;
 *
 *     constructor(enableDebug?: boolean | LstDebugOptions) {
 *         super();
 *         this.debug = createDebugPrinter(enableDebug);
 *     }
 *
 *     async visitBlock(block: J.Block, p: P) {
 *         this.debug?.log(block, this.cursor);
 *         return super.visitBlock(block, p);
 *     }
 * }
 *
 * // Usage:
 * new MyVisitor(true);                              // Enable with defaults
 * new MyVisitor({ output: '/tmp/debug.txt' });      // Enable with options
 * new MyVisitor(false);                             // Disabled
 * new MyVisitor();                                  // Disabled (default)
 */
export function createDebugPrinter(enabled?: boolean | LstDebugOptions): LstDebugPrinter | undefined {
    if (enabled === undefined || enabled === false) {
        return undefined;
    }
    if (enabled === true) {
        return new LstDebugPrinter();
    }
    return new LstDebugPrinter(enabled);
}
