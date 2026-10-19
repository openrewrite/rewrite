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
import {Recipe} from "../recipe";
import {ExecutionContext} from "../execution";
import {noopVisitor, TreeVisitor} from "../visitor";
import {Parser} from "../parser";
import {MarkerPrinter, PrintOutputCapture, TreePrinters} from "../print";
import {Cursor, isTree, rootCursor, SourceFile} from "../tree";
import {Result, scheduleRun} from "../run";
import {mapAsync, trimIndent} from "../util";
import {isParseError, ParseErrorKind} from "../parse-error";
import {Marker, Markers, MarkersKind, ParseExceptionResult} from "../markers";
import {isNamedStyles} from "../style";
import {JavaScriptVisitor} from "../javascript";
import {attributionHint, dumpTypesIfRequested} from "../javascript/type-report";
import {J, Type} from "../java";

export interface SourceSpec<T extends SourceFile> {
    kind: string,
    before: string | null,
    after?: AfterRecipeText
    path?: string,
    parser: (ctx: ExecutionContext) => Parser,
    beforeRecipe?: (sourceFile: T) => T | void | Promise<T> | Promise<void>,
    afterRecipe?: (sourceFile: T) => T | void | Promise<T> | Promise<void>,
    ext: string
}

/** The fields a test configures on a spec, as opposed to the machinery it runs with. */
interface RecipeSpecConfiguration {
    checkParsePrintIdempotence: boolean;
    allowEmptyDiff: boolean;
    checkReportingMarkersOnly: boolean;
    recipe: Recipe;
    recipeExecutionContext: ExecutionContext;
    dataTableAssertions: { [key: string]: (rows: any[]) => void };
}

export class RecipeSpec {
    checkParsePrintIdempotence: boolean = true
    allowEmptyDiff: boolean = false

    /**
     * Fails a run whose recipe leaves markers on the LST that neither its sources nor a fresh parse of
     * its output carry.
     */
    checkReportingMarkersOnly: boolean = true

    recipe: Recipe = new NoopRecipe()

    /**
     * Used for both parsing and recipe execution unless an alternative recipe execution context is set with
     * recipeExecutionContext.
     */
    executionContext: ExecutionContext = new ExecutionContext();

    /**
     * If not specified, will share executionContext instance with the parsing phase.
     */
    recipeExecutionContext: ExecutionContext = this.executionContext;

    private dataTableAssertions: { [key: string]: (rows: any[]) => void } = {}

    // A suite typically declares one spec and configures it per test, which makes the spec a
    // channel between tests unless each test starts from the same configuration. A consumer opts
    // into that reset by calling `trackSuiteConfiguration` from a vitest setup file, as
    // `test/setup.ts` does; until it does, `live` is undefined and no spec is retained here.
    private static live?: RecipeSpec[];
    private suiteConfiguration?: RecipeSpecConfiguration;

    constructor() {
        RecipeSpec.live?.push(this);
    }

    dataTable<Row>(name: string, allRows: (rows: Row[]) => void) {
        this.dataTableAssertions[name] = allRows;
    }

    private configuration(): RecipeSpecConfiguration {
        return {
            checkParsePrintIdempotence: this.checkParsePrintIdempotence,
            allowEmptyDiff: this.allowEmptyDiff,
            checkReportingMarkersOnly: this.checkReportingMarkersOnly,
            recipe: this.recipe,
            recipeExecutionContext: this.recipeExecutionContext,
            dataTableAssertions: {...this.dataTableAssertions}
        };
    }

    /** Starts recording specs, so that the calls below have something to act on. */
    static trackSuiteConfiguration(): void {
        RecipeSpec.live = [];
    }

    /** Records the configuration each test in the current suite starts from. */
    static captureSuiteConfiguration(): void {
        for (const spec of RecipeSpec.live ?? []) {
            spec.suiteConfiguration = spec.configuration();
        }
    }

    /** Undoes whatever the previous test configured. */
    static restoreSuiteConfiguration(): void {
        for (const spec of RecipeSpec.live ?? []) {
            if (spec.suiteConfiguration) {
                Object.assign(spec, spec.suiteConfiguration);
                spec.dataTableAssertions = {...spec.suiteConfiguration.dataTableAssertions};
            }
        }
    }

    /** Bounds the registry to the suite that is running, which is all it has to span. */
    static forgetSuiteSpecs(): void {
        RecipeSpec.live &&= [];
    }

    /**
     * Setting `REWRITE_JAVASCRIPT_DUMP_TYPES` prints each parsed file's type attribution, and a
     * recipe that produces no change names the nodes that carry none. See the README section
     * "Inspecting type attribution".
     */
    async rewriteRun(...sourceSpecs: (SourceSpec<any> | Generator<SourceSpec<any>, void, unknown> | AsyncGenerator<SourceSpec<any>, void, unknown>)[]): Promise<void> {
        // Flatten generators into a list of sourceSpecs
        const flattenedSpecs: SourceSpec<any>[] = [];
        for (const specOrGenerator of sourceSpecs) {
            if (specOrGenerator && typeof (specOrGenerator as any).next === 'function') {
                // Check if it's an async generator
                if (typeof (specOrGenerator as any)[Symbol.asyncIterator] === 'function') {
                    for await (const spec of specOrGenerator as AsyncGenerator<SourceSpec<any>, void, unknown>) {
                        flattenedSpecs.push(spec);
                    }
                } else {
                    // Sync generator
                    for (const spec of specOrGenerator as Generator<SourceSpec<any>, void, unknown>) {
                        flattenedSpecs.push(spec);
                    }
                }
            } else {
                flattenedSpecs.push(specOrGenerator as SourceSpec<any>);
            }
        }

        const specsByKind = flattenedSpecs.reduce((groups, spec) => {
            const kind = spec.kind;
            if (!groups[kind]) {
                groups[kind] = [];
            }
            groups[kind].push(spec);
            return groups;
        }, {} as { [kind: string]: SourceSpec<any>[] });

        const allParsed: [SourceSpec<any>, SourceFile][] = [];
        for (const kind in specsByKind) {
            const specs = specsByKind[kind];
            const parsed = await this.parse(specs);
            await this.expectNoParseFailures(parsed);
            for (const [_, sourceFile] of parsed) {
                await dumpTypesIfRequested(sourceFile);
            }
            await this.expectWhitespaceNotToContainNonwhitespaceCharacters(parsed);
            this.checkParsePrintIdempotence && await this.expectParsePrintIdempotence(parsed);
            allParsed.push(...parsed);
        }

        // Apply beforeRecipe hooks (after idempotence check, before recipe execution)
        for (let i = 0; i < allParsed.length; i++) {
            const [spec, sourceFile] = allParsed[i];
            if (spec.beforeRecipe) {
                const b = spec.beforeRecipe(sourceFile);
                if (b !== undefined) {
                    if (b instanceof Promise) {
                        const mapped = await b;
                        if (mapped === undefined) {
                            throw new Error("Expected beforeRecipe to return a SourceFile, but got undefined. Did you forget a return statement?");
                        }
                        allParsed[i] = [spec, mapped];
                    } else {
                        allParsed[i] = [spec, b as SourceFile];
                    }
                }
            }
        }

        const changeset = (await scheduleRun(this.recipe,
            allParsed.map(([_, sourceFile]) => sourceFile),
            this.recipeExecutionContext)).changeset;

        this.checkReportingMarkersOnly && await this.expectNoAddedMarkers(flattenedSpecs, allParsed, changeset);
        await this.expectResultsToMatchAfter(flattenedSpecs, changeset, allParsed);
        await this.expectGeneratedFiles(flattenedSpecs, changeset);

        // for (const [name, assertion] of Object.entries(this.dataTableAssertions)) {
        //     assertion(getRows(name, this.recipeExecutionContext));
        // }
    }

    private async expectNoParseFailures(parsed: [SourceSpec<any>, SourceFile][]) {
        for (const [_, sourceFile] of parsed) {
            if (sourceFile.kind === ParseErrorKind) {
                throw new Error("Parsed source contains a ParseError: " +
                    (sourceFile.markers.markers.find(m => m.kind === MarkersKind.ParseExceptionResult)! as ParseExceptionResult).message);
            }
        }
    }

    private async expectParsePrintIdempotence(parsed: [SourceSpec<any>, SourceFile][]) {
        for (const [spec, sourceFile] of parsed) {
            const beforeSource = dedent(spec.before!);
            const printed = await TreePrinters.print(sourceFile);
            if (printed !== beforeSource) {
                throw new Error(
                    `Parse-print idempotence check failed.\n` +
                    `Expected:\n${beforeSource}\n\nActual:\n${printed}`
                );
            }
        }
    }

    /**
     * A marker in the output that no source carries in that kind of place, and that a fresh parse of the
     * output does not produce either, is one the recipe put there for its own use.
     */
    private async expectNoAddedMarkers(specs: SourceSpec<any>[], parsed: [SourceSpec<any>, SourceFile][],
                                       changeset: Result[]) {
        const explained = new Set<string>();
        for (const [_, sourceFile] of parsed) {
            for (const placement of markerPlacements(sourceFile).keys()) {
                explained.add(placement);
            }
        }

        const added: string[] = [];
        for (const {before, after} of changeset) {
            if (!after || after === before) {
                continue;
            }
            let unexplained = [...markerPlacements(after)].filter(([placement, found]) =>
                !explained.has(placement) && !mayBeAdded(found));
            if (unexplained.length > 0) {
                const spec = before ?
                    parsed.find(([_, sourceFile]) => sourceFile === before)?.[0] :
                    specs.find(s => s.path === after.sourcePath);
                const reparsed = spec ? await this.freshParsePlacements(spec, after) : new Set<string>();
                unexplained = unexplained.filter(([placement]) => !reparsed.has(placement));
            }
            if (unexplained.length > 0) {
                const note = !before ? " (generated)" :
                    await TreePrinters.print(before) === await TreePrinters.print(after) ?
                        " (the printed source is unchanged)" : "";
                added.push(`  ${after.sourcePath}${note}`);
                for (const [_, placement] of unexplained) {
                    added.push(`    ${await describePlacement(after, placement)}`);
                }
            }
        }

        if (added.length > 0) {
            throw new Error([
                "The recipe added markers to the LST that neither the sources it was given nor a fresh parse of " +
                "its output contain:",
                ...added,
                "Markers travel with the LST to every recipe that runs after this one and to every tool that reads " +
                "the result, so they are not a place for a recipe to keep information for itself. Instead:",
                "  - share information between the visit methods of one visitor with cursor messages, " +
                "e.g. this.cursor.messages.set(key, value) and this.cursor.getNearestMessage(key)",
                "  - share information between source files, or from scanning to editing, through the accumulator " +
                "of a ScanningRecipe",
                "  - keep the recipe idempotent by recognizing code it has already changed, " +
                "rather than tagging that code",
                "  - report a finding with foundSearchResult, and a problem with markupError or markupWarn"
            ].join("\n"));
        }
    }

    /**
     * Code a recipe writes brings along the syntax markers the parser gives that code. A file that does not
     * parse again explains nothing.
     */
    private async freshParsePlacements(spec: SourceSpec<any>, after: SourceFile): Promise<Set<string>> {
        try {
            // Without the comments that search results and markup print as
            const out = new PrintOutputCapture(MarkerPrinter.SANITIZED);
            const text = await TreePrinters.printer(after).print(after, out);
            const reparsed = await spec.parser(this.executionContext).parseOne({text, sourcePath: after.sourcePath});
            return isParseError(reparsed) ? new Set() : new Set(markerPlacements(reparsed).keys());
        } catch {
            return new Set();
        }
    }

    private async expectResultsToMatchAfter(specs: SourceSpec<any>[], changeset: Result[], parsed: [SourceSpec<any>, SourceFile][]) {
        for (const spec of specs) {
            const matchingSpec = parsed.find(([s, _]) => s === spec);
            const result = changeset.find(c => {
                if (c.before) {
                    return c.before === matchingSpec![1];
                } else if (c.after) {
                    const matchingSpec = specs.find(s => s.path === c.after!.sourcePath);
                    return !!matchingSpec;
                }
            });
            const after = result?.after;

            if (!spec.after) {
                if (after && after !== result?.before) {
                    const actual = await TreePrinters.print(after);
                    const expected = dedent(spec.before!);
                    if (actual === expected) {
                        if (!this.allowEmptyDiff) {
                            throw new Error(
                                "An empty diff was generated. The recipe incorrectly " +
                                "changed the AST without changing the printed output."
                            );
                        }
                    } else {
                        throw new Error(
                            "Expected no change but recipe modified the file.\n" +
                            `Expected:\n${expected}\n\nActual:\n${actual}`
                        );
                    }
                }
                if (spec.afterRecipe) {
                    await spec.afterRecipe(matchingSpec![1]);
                }
            } else {
                await this.expectAfter(spec, after, matchingSpec?.[1]);
            }
        }
    }

    private async expectGeneratedFiles(specs: SourceSpec<any>[], changeset: Result[]) {
        for (const spec of specs) {
            if (spec.before) {
                continue;
            }
            if (!spec.after) {
                throw new Error("Expected after to be defined if before is undefined.");
            }
            const after = changeset.find(c => c.after?.sourcePath === spec.path);
            await this.expectAfter(spec, after?.after);
        }
    }

    private async expectAfter(spec: SourceSpec<any>, after?: SourceFile, before?: SourceFile) {
        if (!after) {
            throw new Error('Expected for recipe to have produced a change for file:\n' + trimIndent(spec.before))
        }
        await new ValidateWhitespaceVisitor().visit(after, this.executionContext);
        const actualAfter = await TreePrinters.print(after);
        const afterSource = typeof spec.after === "function" ?
            (spec.after as (actual: string) => string)(actualAfter) : spec.after as string;
        if (actualAfter !== afterSource) {
            // A tree the recipe returned unchanged usually means it never fired.
            const hint = after === before ? await attributionHint(after) : "";
            throw new Error(
                `Recipe output does not match expected.\n` +
                `Expected:\n${afterSource}\n\nActual:\n${actualAfter}${hint}`
            );
        }
        if (spec.afterRecipe) {
            await spec.afterRecipe(after);
        }
    }

    /**
     * Parse the whole group together so the sources can reference once another.
     */
    private async parse(specs: SourceSpec<any>[]): Promise<[SourceSpec<any>, SourceFile][]> {
        // The path is a function of position, so a test parses under the same path on every run,
        // which is what lets a parser pooled across specs reuse its program.
        let unnamed = 0;
        const before: [SourceSpec<any>, { text: string, sourcePath: string }][] = [];
        for (const spec of specs) {
            if (spec.before) {
                const sourcePath = spec.path || `source-${unnamed++}.${spec.ext}`;
                before.push([spec, {text: dedent(spec.before), sourcePath: sourcePath}]);
            }
        }
        const parser = specs[0].parser(this.executionContext);
        const parsed: SourceFile[] = [];
        for await (const sourceFile of parser.parse(...before.map(([_, parserInput]) => parserInput))) {
            parsed.push(sourceFile);
        }
        return before.map(([spec, _], i): [SourceSpec<any>, SourceFile] => [spec, parsed[i]]);
    }

    private async expectWhitespaceNotToContainNonwhitespaceCharacters(parsed: [SourceSpec<any>, SourceFile][]) {
        const validator = new ValidateWhitespaceVisitor();
        for (const [_, sourceFile] of parsed) {
            await validator.visit(sourceFile, this.executionContext);
        }
    }
}

class ValidateWhitespaceVisitor extends JavaScriptVisitor<ExecutionContext> {
    public override async visitSpace(space: J.Space, p: ExecutionContext): Promise<J.Space> {
        const ret = super.visitSpace(space, p);
        if (!/^\s*$/.test(space.whitespace)) {
            throw new Error(
                `Whitespace contains non-whitespace characters: ${JSON.stringify(space.whitespace)}`
            );
        }
        return ret;
    }
}

/** Markers that report on an LST rather than describe it, which a recipe is free to leave. */
const reportingMarkerKinds = new Set<string>([
    MarkersKind.SearchResult,
    MarkersKind.MarkupError,
    MarkersKind.MarkupWarn,
    MarkersKind.MarkupInfo,
    MarkersKind.MarkupDebug,
    MarkersKind.RecipesThatMadeChanges,
]);

/** Markers that rewrite's own Java recipes still leave for themselves, as the Java harness allows. */
const rewriteRecipeMarkerKinds = new Set<string>([
    "org.openrewrite.marker.AlreadyReplaced",
    "org.openrewrite.maven.marker.AlreadyIncremented",
    "org.openrewrite.yaml.MultilineScalarChanged",
    "org.openrewrite.java.search.UsesMethod$MethodMatch",
]);

function mayBeAdded({marker, element}: MarkerPlacement): boolean {
    // A recipe that formats with a style it was given passes the style on to later formatting
    return reportingMarkerKinds.has(marker.kind) || rewriteRecipeMarkerKinds.has(marker.kind) ||
        !element && isNamedStyles(marker);
}

interface MarkerPlacement {
    readonly marker: Marker
    readonly markerKind: string
    /** What carries the marker, absent for the source file itself. */
    readonly element?: any
    readonly cursor: Cursor
}

/**
 * The kinds of marker a source file carries, by whether they sit on the source file itself or on an element
 * within it, padding included, each with the first element found carrying it.
 */
function markerPlacements(sourceFile: SourceFile): Map<string, MarkerPlacement> {
    const placements = new Map<string, MarkerPlacement>();
    const visited = new Set<object>();
    const stack: [any, Cursor][] = [[sourceFile, rootCursor()]];
    while (stack.length > 0) {
        const [value, cursor] = stack.pop()!;
        // Type attribution can be cyclic, and reaches far beyond the file
        if (value === null || typeof value !== "object" || visited.has(value) || Type.isType(value)) {
            continue;
        }
        visited.add(value);
        let enclosing = cursor;
        if (value.markers?.kind === MarkersKind.Markers) {
            const element = value === sourceFile ? undefined : value;
            for (const marker of (value.markers as Markers).markers) {
                const placement = `${element ? "element" : "source file"} ${marker.kind}`;
                if (!placements.has(placement)) {
                    placements.set(placement, {marker, markerKind: marker.kind, element, cursor});
                }
            }
            enclosing = new Cursor(value, cursor);
        }
        // Pushed in reverse, so the walk meets elements in the order they print
        const fields = Object.entries(value);
        for (let i = fields.length - 1; i >= 0; i--) {
            // A marker's own fields are not places in the LST
            if (fields[i][0] !== "markers") {
                stack.push([fields[i][1], enclosing]);
            }
        }
    }
    return placements;
}

/** Reads as "my.Visited on J.MethodInvocation `foo()`", or as "my.Visited on the source file". */
async function describePlacement(sourceFile: SourceFile, placement: MarkerPlacement): Promise<string> {
    const {markerKind, element, cursor} = placement;
    if (!element) {
        return `${markerKind} on the source file`;
    }
    const kind = String(element.kind).replace(/^.*\./, "").replace(/\$/g, ".");
    // Padding is shown by what it pads
    const [tree, enclosing] = isTree(element) ? [element, cursor] : [element.element, new Cursor(element, cursor)];
    let snippet = "";
    if (isTree(tree)) {
        try {
            const printed = await TreePrinters.printer(sourceFile)
                .print(tree, new PrintOutputCapture(MarkerPrinter.SANITIZED), enclosing);
            const line = printed.trim().split("\n")[0].trim();
            snippet = line.length > 60 ? `${line.substring(0, 60)}...` : line;
        } catch {
            // the kind of element alone has to do
        }
    }
    return `${markerKind} on ${kind}${snippet ? ` \`${snippet}\`` : ""}`;
}

class NoopRecipe extends Recipe {
    name = "org.openrewrite.noop";
    displayName = "Do nothing";
    description = "Default no-op test, does nothing.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return noopVisitor();
    }
}

export type AfterRecipeText = string | ((actual: string) => string | undefined) | undefined | null;

/**
 * Simple dedent implementation that removes common leading whitespace from each line.
 *
 * Behavior:
 * - Removes ONE leading newline if present (for template string ergonomics)
 * - Preserves trailing newlines (important for testing formatters like Prettier)
 * - For lines with content: removes common indentation
 * - For lines with only whitespace: removes common indentation, preserving remaining spaces
 *
 * Examples:
 * - `\n  code` → `code` (single leading newline removed)
 * - `\n\n  code` → `\ncode` (first newline removed, second preserved)
 * - `  code\n` → `code\n` (trailing newline preserved)
 * - `  code\n\n` → `code\n\n` (trailing newlines preserved)
 */
export function dedent(s: string): string {
    if (!s) return s;

    // Remove single leading newline for ergonomics
    const start = s.charCodeAt(0) === 10 ? 1 : 0;  // 10 = '\n'

    if (start >= s.length) return '';

    const str = start > 0 ? s.slice(start) : s;
    const lines = str.split('\n');

    // Always consider all lines for minIndent calculation
    // If first line has content at column 0, minIndent will be 0 and no dedenting happens
    const startLine = 0;

    // Find minimum indentation
    let minIndent = Infinity;
    for (let i = startLine; i < lines.length; i++) {
        const line = lines[i];
        let indent = 0;
        for (let j = 0; j < line.length; j++) {
            const ch = line.charCodeAt(j);
            if (ch === 32 || ch === 9) {  // ' ' or '\t'
                indent++;
            } else {
                // Found non-whitespace, update minIndent
                if (indent < minIndent) minIndent = indent;
                break;
            }
        }
    }

    // If all lines are empty or no indentation
    if (minIndent === Infinity || minIndent === 0) {
        return lines.join('\n');
    }

    // Remove common indentation from all lines
    return lines.map(line =>
        line.length >= minIndent ? line.slice(minIndent) : ''
    ).join('\n');
}

export function dedentAfter(s?: AfterRecipeText): AfterRecipeText {
    if (s !== null) {
        if (s === undefined) {
            return undefined;
        }
        if (typeof s === "function") {
            return (actual: string): string | undefined => {
                const raw = s(actual);
                return raw;
            };
        }
        return () => dedent(s);
    }
    return null;
}


export class AdHocRecipe extends Recipe {
    name = "org.openrewrite.adhoc"
    displayName = "ad-hoc"
    description = "ad-hoc."

    constructor(private visitor: TreeVisitor<any, any>) {
        super();
    }

    async editor(): Promise<TreeVisitor<any, any>> {
        return this.visitor;
    }
}

export function fromVisitor(visitor: TreeVisitor<any, any>): Recipe {
    return new AdHocRecipe(visitor);
}
