import {Recipe} from "../recipe";
import {createExecutionContext, ExecutionContext} from "../execution";
import {noopVisitor, TreeVisitor} from "../visitor";
import {Parser, PARSER_VOLUME, readSourceSync} from "../parser";
import {TreePrinters} from "../print";
import {SourceFile} from "../tree";
import dedent from "dedent";
import {Result, scheduleRun} from "../run";
import {getRows} from "../data-table";
import {SnowflakeId} from "@akashrajpurohit/snowflake-id";
import {memfs} from "memfs";

export interface SourceSpec<T extends SourceFile> {
    kind: string,
    before?: string
    after?: AfterRecipe
    path?: string,
    parser: () => Parser,
    beforeRecipe?: (sourceFile: T) => T | void | Promise<T>
}

export class RecipeSpec {
    recipe: Recipe = new NoopRecipe()

    /**
     * Used for both parsing and recipe execution unless an alternative recipe execution context is set with
     * recipeExecutionContext.
     */
    executionContext: ExecutionContext = createExecutionContext();

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
            const parsed = this.parse(specs);
            await this.expectParsePrintIdempotence(parsed);
            const changeset = (await scheduleRun(this.recipe,
                parsed.map(([_, sourceFile]) => sourceFile),
                this.recipeExecutionContext)).changeset;
            await this.expectResultsToMatchAfter(specs, changeset, parsed);
            await this.expectGeneratedFiles(specs, changeset);
        }

        for (const [name, assertion] of Object.entries(this.dataTableAssertions)) {
            assertion(getRows(name, this.recipeExecutionContext));
        }
    }

    private async expectParsePrintIdempotence(parsed: [SourceSpec<any>, SourceFile][]) {
        for (const [_, sourceFile] of parsed) {
            const beforeSource = readSourceSync(this.recipeExecutionContext, sourceFile.sourcePath);
            expect(await TreePrinters.print(sourceFile)).toEqual(beforeSource);
        }
    }

    private async expectResultsToMatchAfter(specs: SourceSpec<any>[], changeset: Result[], parsed: [SourceSpec<any>, SourceFile][]) {
        for (const spec of specs) {
            const after = changeset.find(c => {
                if (c.before) {
                    let matchingSpec = parsed.find(([s, _]) => s === spec);
                    return c.before === matchingSpec![1];
                }
            })?.after;

            if (!spec.after) {
                expect(after).not.toBeDefined();
            }
            await this.expectAfter(spec, after);
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
    private parse(specs: SourceSpec<any>[]): [SourceSpec<any>, SourceFile][] {
        const before: [SourceSpec<any>, string][] = [];
        const vol = memfs().vol
        vol.mkdirSync(process.cwd(), {recursive: true});
        this.executionContext[PARSER_VOLUME] = vol;
        for (const spec of specs) {
            if (spec.before) {
                const sourcePath = `${SnowflakeId().generate()}.txt`;
                vol.writeFileSync(`${process.cwd()}/${sourcePath}`, dedent(spec.before));
                before.push([spec, sourcePath]);
            }
        }
        const parsed = specs[0].parser().parse(this.executionContext, undefined, ...before.map(([_, sourcePath]) => sourcePath));
        return before.map(([spec, _], i) => [spec, parsed[i]]);
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
