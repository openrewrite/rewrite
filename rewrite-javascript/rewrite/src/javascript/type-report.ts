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
import {Cursor, SourceFile, Tree} from "../tree";
import {isJava, J, Type} from "../java";
import {PrintOutputCapture} from "../print";
import {JS} from "./tree";
import {JavaScriptPrinter} from "./print";
import {MethodMatcher} from "./method-matcher";

/**
 * Print a parsed LST annotated with its type attribution, answering "what type did this
 * expression get, and if none, where was it lost?". The README section "Inspecting type
 * attribution" describes the output and the `rewrite-javascript-types` command.
 */

// A slot left undefined and one holding `Type.unknownType` render differently, since
// they are different ways for attribution to be absent.
const NONE = "<none>";
const UNKNOWN = "<unknown>";
const NO_SUPERTYPE = "(none recorded)";

// Nodes whose type slot a recipe gates on. `all` lists every other node with a type slot too.
const DEFAULT_KINDS: ReadonlySet<string> = new Set([
    J.Kind.MethodInvocation,
    JS.Kind.FunctionCall,
    J.Kind.NewClass,
    J.Kind.MethodDeclaration,
    J.Kind.ClassDeclaration,
    J.Kind.NamedVariable,
]);

const CALL_KINDS: ReadonlySet<string> = new Set([J.Kind.MethodInvocation, JS.Kind.FunctionCall, J.Kind.NewClass]);

const TYPE_SLOTS = ["type", "methodType", "constructorType", "variableType", "fieldType"];

// The matcher reads either spelling of these primitives. A TypeScript author writes this one.
const TS_SPELLING: Record<string, string> = {double: "number", String: "string"};

const MAX_TYPE_DEPTH = 3;
const EXCERPT_WIDTH = 40;

export const DUMP_TYPES_ENV = "REWRITE_JAVASCRIPT_DUMP_TYPES";

/** One reported node: where it is, what it reads as, and what type it got. */
export interface TypeEntry {
    line: number;
    column: number;
    kind: string;
    source: string;
    type: string;
    /** The kind of the rendered type, which decides the check a recipe needs (`Primitive`, `Class`, ...). */
    typeKind: string;
    missing: boolean;
    note?: string;
    cause?: TypeEntry;
    /** A call's arguments, each with the type a pattern capture over it sees. */
    arguments?: TypeEntry[];
    supertypes?: string;
}

/** The source-ordered entries reported for one source file. */
export interface TypeReport {
    sourcePath: string;
    /** The nodes the listing covers, before `onlyMissing` narrows it. */
    nodeCount: number;
    entries: TypeEntry[];
}

export interface TypeReportOptions {
    /** List only the nodes whose type is missing. */
    onlyMissing?: boolean;
    /** List every node with a type slot, not just calls and declarations. */
    all?: boolean;
    /** Show each declaring type's ancestry. */
    supertypes?: boolean;
}

/**
 * Render a type by name, so the output is stable across runs. Type graphs are cyclic, so
 * nesting stops at a type already being rendered and at a fixed depth.
 */
export function renderType(type: Type | undefined, depth = 0, seen: ReadonlySet<Type> = new Set()): string {
    if (type === undefined || type === null) {
        return NONE;
    }
    if (type.kind === Type.Kind.Unknown) {
        return UNKNOWN;
    }
    if (Type.isPrimitive(type)) {
        return type.keyword;
    }
    if (depth >= MAX_TYPE_DEPTH || seen.has(type)) {
        return "...";
    }
    const inner = new Set(seen).add(type);
    const nested = (t: Type | undefined) => renderType(t, depth + 1, inner);

    switch (type.kind) {
        case Type.Kind.Parameterized: {
            const p = type as Type.Parameterized;
            const params = (p.typeParameters ?? []).map(nested).join(", ");
            return params ? `${nested(p.type)}<${params}>` : nested(p.type);
        }
        case Type.Kind.Class:
        case Type.Kind.ShallowClass:
            return (type as Type.Class).fullyQualifiedName || UNKNOWN;
        case Type.Kind.Annotation:
            return `@${nested((type as Type.Annotation).type)}`;
        case Type.Kind.Union:
        case Type.Kind.Intersection: {
            // A union of overloads repeats its bounds, which reads as noise.
            const bounds = [...new Set((type as Type.Union).bounds.map(nested))];
            return bounds.length ? bounds.join(type.kind === Type.Kind.Union ? " | " : " & ") : UNKNOWN;
        }
        case Type.Kind.GenericTypeVariable: {
            const g = type as Type.GenericTypeVariable;
            const bounds = (g.bounds ?? []).map(nested).join(" & ");
            return bounds ? `${g.name} extends ${bounds}` : g.name;
        }
        case Type.Kind.Array:
            return `${nested((type as Type.Array).elemType)}[]`;
        case Type.Kind.Method:
            return renderMethod(type as Type.Method, depth, inner);
        case Type.Kind.Variable: {
            const v = type as Type.Variable;
            return `${nested(v.owner)}#${v.name}: ${nested(v.type)}`;
        }
        default:
            return shortKind(type.kind);
    }
}

/** Render a method type as `<declaring type> <name>(..) -> <return type>`. */
export function renderMethod(method: Type.Method | undefined, depth = 0, seen: ReadonlySet<Type> = new Set()): string {
    if (method === undefined) {
        return NONE;
    }
    return `${renderType(method.declaringType, depth + 1, seen)} ${method.name}(..) -> ` +
        renderType(method.returnType, depth + 1, seen);
}

/** The rendered type of a node's load-bearing type slot, or `undefined` for a node with none. */
export function renderNodeType(node: J): string | undefined {
    return hasTypeSlot(node) ? describe(node).type : undefined;
}

export async function buildTypeReport(sourceFile: SourceFile, options: TypeReportOptions = {}): Promise<TypeReport> {
    const {located, printed} = await locate(sourceFile);
    const starts = lineStarts(printed);

    const built = located.map((item, i): TypeEntry => {
        const content = item.content ?? item.start;
        const [line, column] = lineColumn(starts, content);
        const body = descendant(located, i, bodyOf(item.node));
        const stop = body === undefined ? item.end : located[body].start;
        return {
            line,
            column,
            kind: nodeKind(item.node.kind),
            source: excerpt(printed.substring(content, stop)),
            ...describe(item.node),
            supertypes: options.supertypes ? supertypeChain(item.node) : undefined,
        };
    });

    // A call's name carries no type of its own. Its method type is the call's row.
    const callNames = new Set<J>(located
        .filter(item => item.node.kind === J.Kind.MethodInvocation)
        .map(item => (item.node as J.MethodInvocation).name));

    const entries: TypeEntry[] = [];
    let nodeCount = 0;
    located.forEach((item, i) => {
        if (options.all ? !hasTypeSlot(item.node) || callNames.has(item.node) : !DEFAULT_KINDS.has(item.node.kind)) {
            return;
        }
        nodeCount++;
        let entry = built[i];
        const args = argumentsOf(item.node)
            .map(arg => descendant(located, i, arg))
            .filter((index): index is number => index !== undefined)
            .map(index => built[index]);
        if (args.length > 0) {
            entry = {...entry, arguments: args};
        }
        if (entry.missing) {
            const cause = descendant(located, i, receiverOf(item.node));
            if (cause !== undefined) {
                entry = {...entry, cause: built[cause]};
            }
        }
        if (!options.onlyMissing || entry.missing) {
            entries.push(entry);
        }
    });
    return {sourcePath: sourceFile.sourcePath, nodeCount, entries};
}

/** Format a report as the table {@link printTypes} writes. */
export function formatTypeReport(report: TypeReport): string {
    const rows: string[][] = [];
    for (const entry of report.entries) {
        let type = entry.missing ? `⚠ ${entry.type}` : entry.type;
        if (entry.note) {
            type += `  [⚠ ${entry.note}]`;
        }
        rows.push([`${entry.line}:${entry.column}`, entry.kind, entry.source, type]);
        if (entry.supertypes) {
            rows.push([`${entry.line}:${entry.column}`, "  └ supertypes", "", entry.supertypes]);
        }
        if (entry.cause) {
            const cause = entry.cause;
            rows.push([`${cause.line}:${cause.column}`, `  └ select:${cause.kind}`, cause.source, cause.type]);
        }
        entry.arguments?.forEach((arg, n) => rows.push([
            `${arg.line}:${arg.column}`, `  └ arg${n}:${arg.kind}`, arg.source, `${arg.type} (${arg.typeKind})`
        ]));
    }
    return table([["line:col", "kind", "source", "type"], ...rows]);
}

/** The report as JSON, for a test or CI check that asserts a fixture gained attribution. */
export function typeReportToJson(report: TypeReport): string {
    return JSON.stringify({
        sourcePath: report.sourcePath,
        nodeCount: report.nodeCount,
        missingCount: report.entries.filter(e => e.missing).length,
        entries: report.entries,
    }, null, 2) + "\n";
}

/** Print a source-ordered listing of the LST's type attribution. */
export async function printTypes(
    sourceFile: SourceFile,
    options: TypeReportOptions & { json?: boolean, out?: (text: string) => void } = {}
): Promise<TypeReport> {
    const out = options.out ?? (text => process.stdout.write(text));
    const report = await buildTypeReport(sourceFile, options);
    out(options.json ? typeReportToJson(report) : formatTypeReport(report));
    return report;
}

/**
 * Print the report when {@link DUMP_TYPES_ENV} asks for it. Runs on every test parse, so it
 * stays silent unless asked and reports its own failure rather than failing the test.
 */
export async function dumpTypesIfRequested(sourceFile: SourceFile): Promise<boolean> {
    if (sourceFile.kind !== JS.Kind.CompilationUnit) {
        return false;
    }
    const flags = new Set((process.env[DUMP_TYPES_ENV] ?? "")
        .split(",").map(f => f.trim().toLowerCase()).filter(f => f));
    if (flags.size === 0 || [...flags].every(f => ["0", "false", "off", "no"].includes(f))) {
        return false;
    }
    const header = `\n--- type attribution: ${sourceFile.sourcePath} ---\n`;
    try {
        const report = await buildTypeReport(sourceFile, {
            onlyMissing: flags.has("missing"),
            all: flags.has("all"),
            supertypes: flags.has("supertypes"),
        });
        console.log(header + formatTypeReport(report));
    } catch (e) {
        console.log(`${header}(could not report type attribution: ${e})`);
    }
    return true;
}

/** The unattributed rows, for appending to a failing assertion. */
export async function attributionHint(sourceFile: SourceFile, limit = 12): Promise<string> {
    if (sourceFile.kind !== JS.Kind.CompilationUnit) {
        return "";
    }
    let missing: TypeEntry[];
    try {
        missing = (await buildTypeReport(sourceFile, {onlyMissing: true})).entries;
    } catch {
        return "";
    }
    if (missing.length === 0) {
        return "";
    }
    const shown = missing.slice(0, limit).map(e => ({...e, arguments: undefined}));
    const lines = formatTypeReport({sourcePath: sourceFile.sourcePath, nodeCount: shown.length, entries: shown})
        .trimEnd().split("\n").map(line => `  ${line}`).join("\n");
    const more = missing.length > limit ? `\n  ... and ${missing.length - limit} more` : "";
    return `\n\nNodes with no type attribution (a recipe gated on one of these cannot fire):\n${lines}${more}`;
}

interface Located {
    node: J;
    depth: number;
    /** Offset of the node's prefix. */
    start: number;
    /** Offset of its first source character. */
    content?: number;
    end: number;
}

/**
 * Prints the tree, recording each node's source offsets, since LST nodes carry no positions.
 * A node's text starts where the `beforeSyntax` printing its prefix ends, or after its first
 * `visitSpace` when it has none. A node printed inline without a visit, such as a
 * `NamedVariable`, opens on `beforeSyntax` and closes on `afterSyntax`.
 */
class LocatingPrinter extends JavaScriptPrinter {
    readonly located: Located[] = [];
    private readonly open: { entry: Located, inline: boolean }[] = [];
    private pending?: Located;

    override async visit<R extends J>(tree: Tree, p: PrintOutputCapture, parent?: Cursor): Promise<R | undefined> {
        if (!isJava(tree)) {
            return super.visit(tree, p, parent);
        }
        const depth = this.open.length;
        this.enter(tree, p, false);
        try {
            return await super.visit(tree, p, parent);
        } finally {
            while (this.open.length > depth) {
                this.exit(p);
            }
        }
    }

    protected override async beforeSyntax(j: J, p: PrintOutputCapture): Promise<void> {
        if (this.open[this.open.length - 1]?.entry.node !== j) {
            this.enter(j, p, true);
        }
        const {entry} = this.open[this.open.length - 1];
        await super.beforeSyntax(j, p);
        entry.content = p.out.length;
        if (this.pending === entry) {
            this.pending = undefined;
        }
    }

    protected override async afterSyntax(j: J, p: PrintOutputCapture): Promise<void> {
        await super.afterSyntax(j, p);
        const top = this.open[this.open.length - 1];
        if (top?.inline && top.entry.node === j) {
            this.exit(p);
        }
    }

    override async visitSpace(space: J.Space | undefined, p: PrintOutputCapture): Promise<J.Space> {
        const result = await super.visitSpace(space, p);
        this.prefixPrinted(p);
        return result;
    }

    private enter(node: J, p: PrintOutputCapture, inline: boolean): void {
        this.prefixPrinted(p, true);
        const entry: Located = {node, depth: this.open.length, start: p.out.length, end: 0};
        this.located.push(entry);
        this.open.push({entry, inline});
        this.pending = entry;
    }

    private exit(p: PrintOutputCapture): void {
        this.prefixPrinted(p, true);
        this.open.pop()!.entry.end = p.out.length;
    }

    private prefixPrinted(p: PrintOutputCapture, fallback = false): void {
        if (this.pending) {
            this.pending.content = fallback ? this.pending.start : p.out.length;
            this.pending = undefined;
        }
    }
}

async function locate(sourceFile: SourceFile): Promise<{ located: Located[], printed: string }> {
    const printer = new LocatingPrinter();
    const capture = new PrintOutputCapture();
    await printer.visit(sourceFile, capture);
    return {located: printer.located, printed: capture.out};
}

// `type` also names fields that hold no Type, so a slot counts only when it is empty or typed.
function hasTypeSlot(node: J): boolean {
    return TYPE_SLOTS.some(slot => slot in node &&
        ((node as any)[slot] === undefined || Type.isType((node as any)[slot])));
}

function methodTypeOf(node: J): Type.Method | undefined {
    return node.kind === J.Kind.NewClass ? (node as J.NewClass).constructorType : (node as any).methodType;
}

function describe(node: J): { type: string, typeKind: string, missing: boolean, note?: string } {
    if ("methodType" in node || "constructorType" in node) {
        const method = methodTypeOf(node);
        if (method === undefined) {
            return {type: NONE, typeKind: NONE, missing: true};
        }
        // The declaring type is what MethodMatcher gates on, so it alone decides whether the
        // row is missing. An unresolved return type shows in the text.
        const declaring = renderType(method.declaringType);
        const missing = isMissing(declaring);
        const {pattern, note} = missing ? {pattern: `${declaring} ${method.name}(..)`, note: undefined} :
            matcherPattern(method, declaring);
        return {type: `${pattern} -> ${renderType(method.returnType)}`, typeKind: typeKindOf(method), missing, note};
    }
    let type: Type | undefined;
    if (node.kind === J.Kind.NamedVariable) {
        type = (node as J.VariableDeclarations.NamedVariable).variableType?.type;
    } else if (node.kind === J.Kind.Identifier && (node as J.Identifier).fieldType) {
        type = (node as J.Identifier).fieldType!.type;
    } else if (Type.isType((node as any).type)) {
        type = (node as any).type;
    }
    const rendered = renderType(type);
    return {type: rendered, typeKind: typeKindOf(type), missing: isMissing(rendered)};
}

function isMissing(rendered: string): boolean {
    return rendered === NONE || rendered === UNKNOWN;
}

function typeKindOf(type: Type | undefined): string {
    if (type === undefined || type === null) {
        return NONE;
    }
    return type.kind === Type.Kind.Union ? "Union" : shortKind(type.kind);
}

/**
 * The most specific {@link MethodMatcher} pattern that matches `method`. It names the parameter
 * types as the matcher does, or uses `(..)` when one has no such name. Each candidate is checked
 * against the matcher, and a note says when neither matches.
 */
function matcherPattern(method: Type.Method, declaring: string): { pattern: string, note?: string } {
    const params = method.parameterTypes.map(MethodMatcher.typeName).map(name => TS_SPELLING[name] ?? name);
    const candidates = [`${declaring} ${method.name}(..)`];
    if (!params.includes("unknown")) {
        candidates.unshift(`${declaring} ${method.name}(${params.join(", ")})`);
    }
    let note = "pattern does not match this call";
    for (const candidate of candidates) {
        try {
            if (new MethodMatcher(candidate).matches(method)) {
                return {pattern: candidate};
            }
        } catch (e) {
            note = `pattern unparseable: ${e instanceof Error ? e.message : e}`;
        }
    }
    return {pattern: candidates[candidates.length - 1], note};
}

/**
 * The declaring type's ancestry, which bounds how general a pattern can be. A type recording
 * no supertype can only be matched by its own name or a wildcard.
 */
function supertypeChain(node: J): string | undefined {
    let current: Type | undefined = methodTypeOf(node)?.declaringType;
    if (current === undefined || current.kind === Type.Kind.Unknown) {
        return undefined;
    }
    const chain: string[] = [];
    const seen = new Set<Type>();
    while (current && !seen.has(current)) {
        seen.add(current);
        chain.push(renderType(current));
        const cls: Type | undefined = Type.isParameterized(current) ? current.type : current;
        current = (cls as Type.Class | undefined)?.supertype;
    }
    if (chain.length === 1) {
        chain.push(NO_SUPERTYPE);
    }
    return chain.join(" <: ");
}

function argumentsOf(node: J): J[] {
    const args = (node as J.MethodInvocation | J.NewClass | JS.FunctionCall).arguments;
    return CALL_KINDS.has(node.kind) && args ?
        args.elements.map(arg => arg.element).filter(arg => arg.kind !== J.Kind.Empty) : [];
}

function receiverOf(node: J): J | undefined {
    if (node.kind === J.Kind.MethodInvocation) {
        return (node as J.MethodInvocation).select?.element;
    }
    if (node.kind === JS.Kind.FunctionCall) {
        return (node as JS.FunctionCall).function?.element;
    }
    return undefined;
}

/** The block a declaration owns, so its excerpt can stop at the signature. */
function bodyOf(node: J): J | undefined {
    if (node.kind === J.Kind.MethodDeclaration) {
        return (node as J.MethodDeclaration).body;
    }
    if (node.kind === J.Kind.ClassDeclaration) {
        return (node as J.ClassDeclaration).body;
    }
    return undefined;
}

/**
 * Index of `child` within the node at `parent`. A recipe can leave the same node object in two
 * slots, so a child is identified by where this traversal reached it.
 */
function descendant(located: Located[], parent: number, child: J | undefined): number | undefined {
    if (child === undefined) {
        return undefined;
    }
    for (let i = parent + 1; i < located.length && located[i].start < located[parent].end; i++) {
        if (located[i].node === child) {
            return i;
        }
    }
    return undefined;
}

function lineStarts(text: string): number[] {
    const starts = [0];
    for (let i = 0; i < text.length; i++) {
        if (text[i] === "\n") {
            starts.push(i + 1);
        }
    }
    return starts;
}

function lineColumn(starts: number[], offset: number): [number, number] {
    let lo = 0, hi = starts.length - 1;
    while (lo < hi) {
        const mid = (lo + hi + 1) >> 1;
        if (starts[mid] <= offset) {
            lo = mid;
        } else {
            hi = mid - 1;
        }
    }
    return [lo + 1, offset - starts[lo] + 1];
}

function excerpt(text: string, width = EXCERPT_WIDTH): string {
    const collapsed = text.split(/\s+/).filter(s => s).join(" ");
    return collapsed.length <= width ? collapsed : collapsed.substring(0, width - 3) + "...";
}

// `JS.Binary` and `J.Binary` are visited by different methods, so a JavaScript-only kind keeps its namespace.
function nodeKind(kind: string): string {
    const short = shortKind(kind);
    return kind.startsWith("org.openrewrite.javascript.") ?
        `${kind.substring(kind.lastIndexOf(".") + 1, kind.indexOf("$"))}.${short}` : short;
}

function shortKind(kind: string): string {
    return kind.substring(kind.lastIndexOf("$") + 1);
}

function table(rows: string[][]): string {
    const widths = [0, 1, 2].map(i => Math.max(...rows.map(r => r[i].length)));
    return rows.map(r =>
        (r.slice(0, 3).map((cell, i) => cell.padEnd(widths[i])).join("  ") + "  " + r[3]).trimEnd()
    ).join("\n") + "\n";
}
