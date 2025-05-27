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
import {TreePrinters} from "../print";
import {SourceFile} from "../tree";
import dedent from "dedent";
import {Result, scheduleRun} from "../run";
import {SnowflakeId} from "@akashrajpurohit/snowflake-id";
import {mapAsync} from "../util";
import {ParseErrorKind} from "../parse-error";
import {MarkersKind, ParseExceptionResult} from "../markers";

export interface SourceSpec<T extends SourceFile> {
    kind: string,
    before: string | null,
    after?: AfterRecipeText
    path?: string,
    parser: (ctx: ExecutionContext) => Parser,
    beforeRecipe?: (sourceFile: T) => T | void | Promise<T>,
    afterRecipe?: (sourceFile: T) => T | void | Promise<T>,
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

    async rewriteRun(...sourceSpecs: SourceSpec<any>[]): Promise<void> {
        const specsByKind = sourceSpecs.reduce((groups, spec) => {
            const kind = spec.kind;
            if (!groups[kind]) {
                groups[kind] = [];
            }
            groups[kind].push(spec);
            return groups;
        }, {} as { [kind: string]: SourceSpec<any>[] });

        for (const kind in specsByKind) {
            const specs = specsByKind[kind];
            const parsed = await this.parse(specs);
            await this.expectNoParseFailures(parsed);
            this.checkParsePrintIdempotence && await this.expectParsePrintIdempotence(parsed);
            const changeset = (await scheduleRun(this.recipe,
                parsed.map(([_, sourceFile]) => sourceFile),
                this.recipeExecutionContext)).changeset;
            await this.expectResultsToMatchAfter(specs, changeset, parsed);
            await this.expectGeneratedFiles(specs, changeset);
        }

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
            expect(await TreePrinters.print(sourceFile)).toEqual(beforeSource);
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
                    expect(await TreePrinters.print(after)).toEqual(dedent(spec.before!));
                    // TODO: Consider throwing an error, as there should typically have been no change to the LST
                    // fail("Expected after to be undefined.");
                }
                if (spec.afterRecipe) {
                    spec.afterRecipe(matchingSpec![1]);
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
        expect(after).toBeDefined();
        const actualAfter = await TreePrinters.print(after!);
        const afterSource = typeof spec.after === "function" ?
            (spec.after as (actual: string) => string)(actualAfter) : spec.after as string;
        expect(actualAfter).toEqual(afterSource);
        if (spec.afterRecipe) {
            spec.afterRecipe(actualAfter);
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
                before.push([spec, {text: dedent(spec.before), sourcePath: sourcePath}]);
            }
        }
        const parsed = await specs[0].parser(this.executionContext).parse(...before.map(([_, parserInput]) => parserInput));
        const specToParsed: [SourceSpec<any>, SourceFile][] = before.map(([spec, _], i) => [spec, parsed[i]]);
        return await mapAsync(specToParsed, async ([spec, sourceFile]) => {
            const b = spec.beforeRecipe ? spec.beforeRecipe(sourceFile) : sourceFile;
            if (b !== undefined) {
                if (b instanceof Promise) {
                    return [spec, await b];
                }
                return [spec, b as SourceFile];
            }
            return [spec, sourceFile];
        });
    }
}

class NoopRecipe extends Recipe {
    name = "org.openrewrite.noop";
    displayName = "Do nothing";
    description = "Default no-op test, does nothing.";

    get editor(): TreeVisitor<any, ExecutionContext> {
        return noopVisitor();
    }
}

export type AfterRecipeText = string | ((actual: string) => string | undefined) | undefined | null;

export function dedentAfter(s?: AfterRecipeText): AfterRecipeText {
    if (s !== null) {
        if (s === undefined) {
            return undefined;
        }
        if (typeof s === "function") {
            return (actual: string): string | undefined => {
                const raw = s(actual);
                return raw ? dedent(raw) : undefined;
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

    get editor(): TreeVisitor<any, any> {
        return this.visitor;
    }
}

export function fromVisitor(visitor: TreeVisitor<any, any>): Recipe {
    return new AdHocRecipe(visitor);
}

