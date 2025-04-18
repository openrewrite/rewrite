import {describe} from "@jest/globals";
import {RecipeSpec} from "../../../main/javascript/test";
import {text} from "../../../main/javascript/text";
import {ChangeText} from "../example-recipe";

describe("rewrite test", () => {
    const spec = new RecipeSpec();
    spec.recipe = new ChangeText({text: "changed"});

    test("a recipe that makes no changes", () => spec.rewriteRun(
        text("test")
    ));
});
