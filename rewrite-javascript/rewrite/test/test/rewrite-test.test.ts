import {describe} from "@jest/globals";
import {RecipeSpec} from "@openrewrite/rewrite/test";
import {text} from "@openrewrite/rewrite/text";
import {json} from "@openrewrite/rewrite/json";
import {ChangeText} from "../../fixtures/change-text";
import {Cursor, ExecutionContext, ScanningRecipe, Tree, TreeVisitor} from "../../src";

describe("rewrite test", () => {
    const spec = new RecipeSpec();

    test("a recipe that makes no changes", async () => {
        spec.recipe = new ChangeText({text: "test"});
        await spec.rewriteRun(
            text("test")
        )
    });

    test("beforeRecipe", async () => {
        let count = 0;
        await spec.rewriteRun(
            {
                ...text("test"),
                beforeRecipe: () => {
                    count++;
                }
            }
        )
        expect(count).toEqual(1)
    });

    test("customize the path of a source spec", () => spec.rewriteRun(
        {
            ...json(
                `{"type": "object"}`,
            ),
            path: "source.json",
            beforeRecipe: actual => {
                expect(actual.sourcePath).toEqual("source.json");
            }
        }
    ));

    test("two kinds of sources and a scanning recipe", async () => {
        // given
        const sut = new RecipeSpec();
        let countOfAccumulators: number = 0;
        interface Accum {}
        sut.recipe = new class extends ScanningRecipe<Accum> {
            name = "ad-hoc";
            displayName = "ad-hoc";
            description = "ad-hoc";

            initialValue(ctx: ExecutionContext): Accum {
                countOfAccumulators++;
                return {} satisfies Accum;
            }

            async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
                return new class extends TreeVisitor<any, ExecutionContext> {
                    async visit<R extends any>(tree: Tree, p: ExecutionContext, parent?: Cursor): Promise<R | undefined> {
                        return undefined;
                    }
                };
            }
        };

        // when
        await sut.rewriteRun(
            json('{"A": "a"}'),
            text("just a regular text"));

        // test
        expect(countOfAccumulators).toBe(1);
    });
});
