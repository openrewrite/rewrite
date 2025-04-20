import {RecipeRegistry} from "../src";

describe("dynamically require modules", () => {
    test("programmatically register a recipe through requiring it at runtime", () => {
        const allRecipes = RecipeRegistry.all;
        expect(allRecipes.size).toEqual(0);

        const recipeExamples = require.resolve("./example-recipe");
        require(recipeExamples);

        expect(allRecipes.size).toBeGreaterThan(0);
    });
});
