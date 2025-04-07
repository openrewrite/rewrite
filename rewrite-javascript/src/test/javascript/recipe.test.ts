import {Minutes, Option, Recipe, RecipeRegistry, Registered} from "../../main/javascript";
import {describe} from "@jest/globals";

describe("recipes", () => {
    @Registered("org.openrewrite.my-recipe")
    class MyRecipe extends Recipe {
        displayName: string = "My recipe name";
        description: string = "My recipe description.";
        estimatedEffortPerOccurrence: Minutes = 1;

        @Option({
            displayName: "Text",
            description: "Text to change to"
        })
        text!: string;

        // Optional constructor if you want to make it easier to programmatically instantiate.
        constructor(options: { text: string }) {
            super();
            if (options) {
                this.text = options.text;
            }
        }
    }

    test("register a recipe with options", () => {
        const recipe = RecipeRegistry.all.get(
            "org.openrewrite.my-recipe")
        expect(recipe).toBeDefined()
        expect(new recipe!()).toBeInstanceOf(MyRecipe)

        expect(new recipe!().descriptor).toEqual({
            displayName: "My recipe name",
            instanceName: "My recipe name",
            description: "My recipe description.",
            estimatedEffortPerOccurrence: 1,
            options: [
                {
                    description: "Text to change to",
                    displayName: "Text",
                    name: "text"
                }
            ],
            tags: []
        })
    })
})
