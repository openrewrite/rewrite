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

export interface SourceSpec<T extends SourceFile> {
    kind: string,
    before: string | null,
    after?: AfterRecipe
    path?: string,
    parser: (ctx: ExecutionContext) => Parser<T>,
    beforeRecipe?: (sourceFile: T) => T | void | Promise<T>
}

export class RecipeSpec {
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
            await this.expectParsePrintIdempotence(parsed);
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

    private async expectParsePrintIdempotence(parsed: [SourceSpec<any>, SourceFile][]) {
        for (const [spec, sourceFile] of parsed) {
            const beforeSource = dedent(spec.before!);
            expect(await TreePrinters.print(sourceFile)).toEqual(beforeSource);
        }
    }

    private async expectResultsToMatchAfter(specs: SourceSpec<any>[], changeset: Result[], parsed: [SourceSpec<any>, SourceFile][]) {
        for (const spec of specs) {
            const after = changeset.find(c => {
                if (c.before) {
                    const matchingSpec = parsed.find(([s, _]) => s === spec);
                    return c.before === matchingSpec![1];
                } else if (c.after) {
                    const matchingSpec = specs.find(s => s.path === c.after!.sourcePath);
                    return !!matchingSpec;
                }
            })?.after;

            if (!spec.after) {
                expect(after).not.toBeDefined();
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
    }

    /**
     * Parse the whole group together so the sources can reference once another.
     */
    private async parse(specs: SourceSpec<any>[]): Promise<[SourceSpec<any>, SourceFile][]> {
        const before: [SourceSpec<any>, { text: string, sourcePath: string }][] = [];
        for (const spec of specs) {
            if (spec.before) {
                const sourcePath = `${SnowflakeId().generate()}.txt`;
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

export type AfterRecipe = string | ((actual: string) => string | undefined) | undefined | null;

export function dedentAfter(s?: AfterRecipe): AfterRecipe {
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
