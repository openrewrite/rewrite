import {describe} from "@jest/globals";
import {RecipeSpec} from "../../src/test";
import {text} from "../../src/text";
import {ChangeText} from "../example-recipe";

describe("rewrite test", () => {
    const spec = new RecipeSpec();

    test("a recipe that makes no changes", async () => {
        spec.recipe = new ChangeText({text: "test"});
        await spec.rewriteRun(
            text("test")
        )
    });
});
