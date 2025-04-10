import {RecipeRegistry} from "../../main/javascript";
import {describe} from "@jest/globals";
import {ChangeText} from "./example-recipe";

describe("recipes", () => {
    test("register a recipe with options", async () => {
        const recipe = RecipeRegistry.all.get(
            "org.openrewrite.text.change-text")
        expect(recipe).toBeDefined()
        expect(new recipe!()).toBeInstanceOf(ChangeText)

        expect(await new recipe!().descriptor()).toEqual({
            name: "org.openrewrite.text.change-text",
            displayName: "Change text",
            instanceName: "Change text to 'undefined'",
            description: "Change the text of a file.",
            estimatedEffortPerOccurrence: 5,
            options: [
                {
                    description: "Text to change to",
                    displayName: "Text",
                    name: "text",
                    value: undefined
                }
            ],
            recipeList: [],
            tags: []
        })
    })
})
