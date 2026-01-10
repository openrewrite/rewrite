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
import {Recipe, RecipeVisitor} from "../recipe";
import {ExecutionContext} from "../execution";
import {noopVisitor} from "../visitor";
import {Parser} from "../parser";
import {TreePrinters} from "../print";
import {SourceFile} from "../tree";
import {Result, scheduleRun} from "../run";
import {SnowflakeId} from "@akashrajpurohit/snowflake-id";
import {mapAsync, trimIndent} from "../util";
import {ParseErrorKind} from "../parse-error";
import {MarkersKind, ParseExceptionResult} from "../markers";
import {JavaScriptVisitor} from "../javascript";
import {J} from "../java";

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

export class RecipeSpec {
    checkParsePrintIdempotence: boolean = true

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

    dataTable<Row>(name: string, allRows: (rows: Row[]) => void) {
        this.dataTableAssertions[name] = allRows;
    }

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
            this.expectNoParseFailures(parsed);
            this.expectWhitespaceNotToContainNonwhitespaceCharacters(parsed);
            this.checkParsePrintIdempotence && this.expectParsePrintIdempotence(parsed);
            allParsed.push(...parsed);
        }

        const changeset = (await scheduleRun(this.recipe,
            allParsed.map(([_, sourceFile]) => sourceFile),
            this.recipeExecutionContext)).changeset;

        await this.expectResultsToMatchAfter(flattenedSpecs, changeset, allParsed);
        await this.expectGeneratedFiles(flattenedSpecs, changeset);

        // for (const [name, assertion] of Object.entries(this.dataTableAssertions)) {
        //     assertion(getRows(name, this.recipeExecutionContext));
        // }
    }

    private expectNoParseFailures(parsed: [SourceSpec<any>, SourceFile][]) {
        for (const [_, sourceFile] of parsed) {
            if (sourceFile.kind === ParseErrorKind) {
                throw new Error("Parsed source contains a ParseError: " +
                    (sourceFile.markers.markers.find(m => m.kind === MarkersKind.ParseExceptionResult)! as ParseExceptionResult).message);
            }
        }
    }

    private expectParsePrintIdempotence(parsed: [SourceSpec<any>, SourceFile][]) {
        for (const [spec, sourceFile] of parsed) {
            const beforeSource = dedent(spec.before!);
            expect(TreePrinters.print(sourceFile)).toEqual(beforeSource);
        }
    }

    private async expectResultsToMatchAfter(specs: SourceSpec<any>[], changeset: Result[], parsed: [SourceSpec<any>, SourceFile][]) {
        for (const spec of specs) {
            const matchingSpec = parsed.find(([s, _]) => s === spec);
            const after = changeset.find(c => {
                if (c.before) {
                    return c.before === matchingSpec![1];
                } else if (c.after) {
                    const matchingSpec = specs.find(s => s.path === c.after!.sourcePath);
                    return !!matchingSpec;
                }
            })?.after;

            if (!spec.after) {
                if (after) {
                    expect(TreePrinters.print(after)).toEqual(dedent(spec.before!));
                    // TODO: Consider throwing an error, as there should typically have been no change to the LST
                    // fail("Expected after to be undefined.");
                }
                if (spec.afterRecipe) {
                    await spec.afterRecipe(matchingSpec![1]);
                }
            } else {
                await this.expectAfter(spec, after);
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

    private async expectAfter(spec: SourceSpec<any>, after?: SourceFile) {
        if (!after) {
            throw new Error('Expected for recipe to have produced a change for file:\n' + trimIndent(spec.before))
        }
        expect(after).toBeDefined();
        new ValidateWhitespaceVisitor().visit(after!, this.executionContext);
        const actualAfter = TreePrinters.print(after!);
        const afterSource = typeof spec.after === "function" ?
            (spec.after as (actual: string) => string)(actualAfter) : spec.after as string;
        expect(actualAfter).toEqual(afterSource);
        if (spec.afterRecipe) {
            await spec.afterRecipe(after);
        }
    }

    /**
     * Parse the whole group together so the sources can reference once another.
     */
    private async parse(specs: SourceSpec<any>[]): Promise<[SourceSpec<any>, SourceFile][]> {
        let snowflake = SnowflakeId();
        const before: [SourceSpec<any>, { text: string, sourcePath: string }][] = [];
        for (const spec of specs) {
            if (spec.before) {
                const sourcePath = spec.path || `${snowflake.generate()}.${spec.ext}`;
                before.push([spec, {text: dedent(spec.before), sourcePath}]);
            }
        }
        const parser = specs[0].parser(this.executionContext);
        const parsed: SourceFile[] = [];
        for await (const sourceFile of parser.parse(...before.map(([_, parserInput]) => parserInput))) {
            parsed.push(sourceFile);
        }
        const specToParsed: [SourceSpec<any>, SourceFile][] = before.map(([spec, _], i) => [spec, parsed[i]]);
        return await mapAsync(specToParsed, async ([spec, sourceFile]) => {
            const b = spec.beforeRecipe ? spec.beforeRecipe(sourceFile) : sourceFile;
            if (b !== undefined) {
                if (b instanceof Promise) {
                    const mapped = await b;
                    if (mapped === undefined) {
                        throw new Error("Expected beforeRecipe to return a SourceFile, but got undefined. Did you forget a return statement?");
                    }
                    return [spec, mapped];
                }
                return [spec, b as SourceFile];
            }
            return [spec, sourceFile];
        });
    }

    private expectWhitespaceNotToContainNonwhitespaceCharacters(parsed: [SourceSpec<any>, SourceFile][]) {
        const validator = new ValidateWhitespaceVisitor();
        for (const [_, sourceFile] of parsed) {
            validator.visit(sourceFile, this.executionContext);
        }
    }
}

class ValidateWhitespaceVisitor extends JavaScriptVisitor<ExecutionContext> {
    public override visitSpace(space: J.Space, p: ExecutionContext): J.Space {
        const ret = super.visitSpace(space, p);
        expect(space.whitespace).toMatch(/^\s*$/);
        return ret;
    }
}

class NoopRecipe extends Recipe {
    name = "org.openrewrite.noop";
    displayName = "Do nothing";
    description = "Default no-op test, does nothing.";

    async editor(): Promise<RecipeVisitor> {
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

    constructor(private visitor: RecipeVisitor) {
        super();
    }

    async editor(): Promise<RecipeVisitor> {
        return this.visitor;
    }
}

export function fromVisitor(visitor: RecipeVisitor): Recipe {
    return new AdHocRecipe(visitor);
}
