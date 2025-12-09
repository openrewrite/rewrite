import {describe} from "@jest/globals";
import {RecipeSpec} from "../../src/test";
import {text} from "../../src/text";
import {json} from "../../src/json";
import {ChangeText} from "../../fixtures/change-text";
import {ExecutionContext, ScanningRecipe} from "../../src";

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
        sut.recipe = new class extends ScanningRecipe<{}> {
            name = "ad-hoc";
            displayName = "ad-hoc";
            description = "ad-hoc";

            initialValue(ctx: ExecutionContext): {} {
                countOfAccumulators++;
                return {};
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
