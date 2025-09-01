import {describe} from "@jest/globals";
import {RecipeSpec} from "@openrewrite/rewrite/test";
import {text} from "@openrewrite/rewrite/text";
import {json} from "@openrewrite/rewrite/json";
import {ChangeText} from "../../fixtures/change-text";

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
});
