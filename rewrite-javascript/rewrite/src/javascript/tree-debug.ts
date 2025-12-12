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
import {Comment, isIdentifier, isJava, isLiteral, J, TextComment} from "../java";
import {JS} from "./tree";
import {JavaScriptVisitor} from "./visitor";
import * as fs from "fs";

/**
 * Options for controlling LST debug output.
 */
export interface LstDebugOptions {
    /** Include cursor messages in output. Default: true */
    includeCursorMessages?: boolean;
    /** Include markers in output. Default: false */
    includeMarkers?: boolean;
    /** Include IDs in output. Default: false */
    includeIds?: boolean;
    /** Maximum depth to traverse. Default: unlimited (-1) */
    maxDepth?: number;
    /** Properties to always exclude (in addition to defaults). */
    excludeProperties?: string[];
    /** Output destination: 'console' or a file path. Default: 'console' */
    output?: 'console' | string;
    /** Indent string for nested output. Default: '  ' */
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
 * Subscript digits for counts.
 */
const SUBSCRIPTS = ['', '₁', '₂', '₃', '₄', '₅', '₆', '₇', '₈', '₉'];

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

    // If child is provided, use cursor.value as parent; otherwise use cursor.parent.value
    const actualChild = child ?? cursor.value;
    const parent = child ? cursor.value : cursor.parent?.value;

    if (!parent || typeof parent !== 'object') {
        return undefined;
    }

    // Properties to skip when searching
    const skipProps = new Set(['kind', 'id', 'prefix', 'markers', 'type', 'methodType', 'variableType', 'fieldType', 'constructorType']);

    // Special case: if parent is a Container, we need to look at grandparent to find the property name
    if ((parent as any).kind === J.Kind.Container) {
        const container = parent as J.Container<any>;
        const grandparent = child ? cursor.parent?.value : cursor.parent?.parent?.value;

        // Find the index of actualChild in container.elements
        let childIndex = -1;
        if (container.elements) {
            for (let i = 0; i < container.elements.length; i++) {
                if (container.elements[i] === actualChild) {
                    childIndex = i;
                    break;
                }
            }
        }

        // Find which property of grandparent holds this container
        if (grandparent && typeof grandparent === 'object') {
            for (const [key, value] of Object.entries(grandparent)) {
                if (skipProps.has(key)) continue;
                if (value === parent) {
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
        if (value === actualChild) {
            return key;
        }

        // Check if child is in an array
        if (Array.isArray(value)) {
            for (let i = 0; i < value.length; i++) {
                if (value[i] === actualChild) {
                    return `${key}[${i}]`;
                }
                // Check inside RightPadded/LeftPadded wrappers
                if (value[i] && typeof value[i] === 'object') {
                    if (value[i].element === actualChild) {
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
                    if (rp === actualChild) {
                        return `${key}[${i}]`;
                    }
                    if (rp.element === actualChild) {
                        return `${key}[${i}].element`;
                    }
                }
            }
        }

        // Check inside LeftPadded/RightPadded
        if (value && typeof value === 'object') {
            if ((value as any).kind === J.Kind.LeftPadded || (value as any).kind === J.Kind.RightPadded) {
                if ((value as any).element === actualChild) {
                    return `${key}.element`;
                }
            }
        }
    }

    return undefined;
}

/**
 * Format a literal value for inline display.
 */
function formatLiteralValue(lit: J.Literal): string {
    if (lit.valueSource !== undefined) {
        // Truncate long literals
        return lit.valueSource.length > 20
            ? lit.valueSource.substring(0, 17) + '...'
            : lit.valueSource;
    }
    return String(lit.value);
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
 * Usage from within a visitor:
 * ```typescript
 * class MyVisitor extends JavaScriptVisitor<P> {
 *     private debug = new LstDebugPrinter({ includeCursorMessages: true });
 *
 *     async visitMethodInvocation(mi: J.MethodInvocation, p: P) {
 *         this.debug.print(mi, this.cursor);
 *         return super.visitMethodInvocation(mi, p);
 *     }
 * }
 * ```
 */
export class LstDebugPrinter {
    private readonly options: Required<LstDebugOptions>;
    private outputLines: string[] = [];

    constructor(options: LstDebugOptions = {}) {
        this.options = {...DEFAULT_OPTIONS, ...options};
    }

    /**
     * Print a tree node with optional cursor context.
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
                if (value.length === 0) {
                    this.outputLines.push(`${this.indent(depth)}${key}: []`);
                } else if (value.every(v => this.isRightPadded(v))) {
                    this.outputLines.push(`${this.indent(depth)}${key}: [${value.length} RightPadded elements]`);
                    for (let i = 0; i < value.length; i++) {
                        this.outputLines.push(`${this.indent(depth + 1)}[${i}]:`);
                        this.printRightPadded(value[i], undefined, depth + 2);
                    }
                } else if (value.every(v => isJava(v))) {
                    this.outputLines.push(`${this.indent(depth)}${key}: [${value.length} elements]`);
                    for (let i = 0; i < value.length; i++) {
                        this.outputLines.push(`${this.indent(depth + 1)}[${i}]:`);
                        this.printNode(value[i], undefined, depth + 2);
                    }
                } else {
                    this.outputLines.push(`${this.indent(depth)}${key}: [${value.length} items]`);
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
        this.outputLines.push(`${this.indent(depth + 1)}before: ${formatSpace(lp.before)}`);

        if (lp.element !== undefined) {
            if (isJava(lp.element)) {
                this.outputLines.push(`${this.indent(depth + 1)}element:`);
                this.printNode(lp.element, undefined, depth + 2);
            } else if (this.isSpace(lp.element)) {
                this.outputLines.push(`${this.indent(depth + 1)}element: ${formatSpace(lp.element)}`);
            } else {
                this.outputLines.push(`${this.indent(depth + 1)}element: ${JSON.stringify(lp.element)}`);
            }
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

        if (rp.element !== undefined) {
            if (isJava(rp.element)) {
                this.outputLines.push(`${this.indent(depth + 1)}element:`);
                this.printNode(rp.element, undefined, depth + 2);
            } else {
                this.outputLines.push(`${this.indent(depth + 1)}element: ${JSON.stringify(rp.element)}`);
            }
        }

        this.outputLines.push(`${this.indent(depth + 1)}after: ${formatSpace(rp.after)}`);
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

    private isLeftPadded(value: any): value is J.LeftPadded<any> {
        return value !== null &&
            typeof value === 'object' &&
            value.kind === J.Kind.LeftPadded;
    }

    private isRightPadded(value: any): value is J.RightPadded<any> {
        return value !== null &&
            typeof value === 'object' &&
            value.kind === J.Kind.RightPadded;
    }

    private indent(depth: number): string {
        return this.options.indent.repeat(depth);
    }

    private flush(): void {
        const output = this.outputLines.join('\n');

        if (this.options.output === 'console') {
            console.info(output);
        } else {
            fs.appendFileSync(this.options.output, output + '\n\n');
        }

        this.outputLines = [];
    }
}

/**
 * A visitor that prints the LST structure as it traverses.
 * Useful for debugging the entire tree or a subtree.
 *
 * Usage:
 * ```typescript
 * const debugVisitor = new LstDebugVisitor({ maxDepth: 3 });
 * await debugVisitor.visit(tree, ctx);
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
            line += `prefix=${prefix}}`;

            console.info(line);

            // Show cursor messages on a separate indented line
            if (messages !== '<no messages>') {
                console.info(`${indent}  ⤷ ${messages}`);
            }
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
            line += `Container<${container.elements.length}>{before=${before}}`;

            console.info(line);

            if (messages !== '<no messages>') {
                console.info(`${indent}  ⤷ ${messages}`);
            }
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
            const before = formatSpace(left.before);
            // Pass left as the child since cursor.value is the parent node
            const propPath = findPropertyPath(this.cursor, left);

            let line = indent;
            if (propPath) {
                line += `${propPath}: `;
            }
            line += `LeftPadded{before=${before}`;

            // Show element value if it's a primitive (string, number, boolean)
            if (left.element !== null && left.element !== undefined) {
                const elemType = typeof left.element;
                if (elemType === 'string' || elemType === 'number' || elemType === 'boolean') {
                    line += ` element=${JSON.stringify(left.element)}`;
                }
            }
            line += '}';

            console.info(line);

            if (messages !== '<no messages>') {
                console.info(`${indent}  ⤷ ${messages}`);
            }
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
            const after = formatSpace(right.after);
            // Pass right as the child since cursor.value is the parent node
            const propPath = findPropertyPath(this.cursor, right);

            let line = indent;
            if (propPath) {
                line += `${propPath}: `;
            }
            line += `RightPadded{after=${after}`;

            // Show element value if it's a primitive (string, number, boolean)
            if (right.element !== null && right.element !== undefined) {
                const elemType = typeof right.element;
                if (elemType === 'string' || elemType === 'number' || elemType === 'boolean') {
                    line += ` element=${JSON.stringify(right.element)}`;
                }
            }
            line += '}';

            console.info(line);

            if (messages !== '<no messages>') {
                console.info(`${indent}  ⤷ ${messages}`);
            }
        }
        this.depth++;
        const result = await super.visitRightPadded(right, p);
        this.depth--;
        return result;
    }
}

/**
 * Convenience function to print a tree node.
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
