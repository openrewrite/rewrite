import {Recipe} from "../recipe";
import {createExecutionContext, ExecutionContext} from "../execution";
import {noopVisitor, TreeVisitor} from "../visitor";
import {Parser, readSourceSync} from "../parser";
import {TreePrinters} from "../print";
import {SourceFile} from "../tree";
import dedent from "dedent";

export interface SourceSpec {
    before?: string
    after?: AfterRecipe
    path?: string,
    parser: () => Parser,
    executionContext?: ExecutionContext,
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

    async rewriteRun(...sourceSpecs: SourceSpec[]): Promise<void> {
        for (const spec of sourceSpecs) {
            // TODO more validation to implement here, along with grouping source specs for execution
            //  by a single parser when there are interdependencies, etc.

            // TODO also need to implement when scanning recipes generate files
            expect(spec.before).toBeDefined();

            const parserCtx: ExecutionContext = (spec.executionContext ? new Map<string | symbol, any>([
                ...Array.from(this.executionContext.entries()), ...Array.from(spec.executionContext.entries())
            ]) : this.executionContext) as ExecutionContext

            let beforeSource = readSourceSync(parserCtx, spec.before!);
            const beforeParsed = spec.parser().parse(parserCtx, spec.before, spec.before!)[0];

            expect(await TreePrinters.print(beforeParsed)).toEqual(beforeSource);

            let afterFn: () => string
            if (spec.after) {
                if (typeof spec.after === "function") {
                    afterFn = () => (spec.after as () => string)();
                } else {
                    afterFn = () => spec.after as string;
                }
            } else {
                afterFn = () => beforeSource
            }

            const after: SourceFile | undefined = await this.recipe.editor.visit(beforeParsed, this.recipeExecutionContext);
            if (!after) {
                if (spec.after) {
                    fail(`${spec.path} was deleted unexpectedly`)
                }
            } else {
                if (spec.after === null) {
                    fail(`Expected ${spec.path} to be deleted`)
                }
                const afterSource = afterFn();
                expect(await TreePrinters.print(after)).toEqual(afterSource);
            }
        }
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

export type AfterRecipe = string | (() => string | undefined) | undefined | null;

export function dedentAfter(s?: AfterRecipe): AfterRecipe {
    if (s !== null) {
        if (s === undefined) {
            return undefined;
        }
        if (typeof s === "function") {
            return (): string | undefined => {
                const raw = s();
                return raw ? dedent(raw) : undefined;
            };
        }
        return () => dedent(s);
    }
    return null;
}
