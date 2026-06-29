/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {JavaScript, Recipe, RecipeMarketplace} from "../src";
import {activate} from "../fixtures/example-recipe";
import {ChangeText} from "../fixtures/change-text";

describe("recipes", () => {

    // gh-7968: a failure while computing a recipe's descriptor (e.g. a sub-recipe
    // in recipeList() that needs an RPC connection) is dropped at the JSON-RPC
    // boundary, leaving only the generic "constructor" hint. The marketplace must
    // fold the underlying cause into the message so it survives to the caller.
    test("install surfaces the underlying cause when descriptor computation fails", async () => {
        class FailingRecipe extends Recipe {
            name = "org.openrewrite.example.failing";
            displayName = "Failing recipe";
            description = "Throws while resolving its recipe list.";

            async recipeList(): Promise<Recipe[]> {
                throw new Error("no active RewriteRpc connection");
            }
        }

        const marketplace = new RecipeMarketplace();
        await expect(marketplace.install(FailingRecipe, JavaScript)).rejects.toThrow(
            /Cause: no active RewriteRpc connection/
        );
    });

    test("register a recipe with options", async () => {
        const marketplace = new RecipeMarketplace();
        await activate(marketplace);
        const result = marketplace.findRecipe("org.openrewrite.example.text.change-text");
        expect(result).toBeDefined();
        const [descriptor, RecipeClass] = result!;
        expect(RecipeClass).toBeDefined();
        expect(new RecipeClass!()).toBeInstanceOf(ChangeText);

        expect(descriptor).toEqual({
            name: "org.openrewrite.example.text.change-text",
            displayName: "Change text",
            instanceName: "Change text to 'undefined'",
            description: "Change the text of a file.",
            estimatedEffortPerOccurrence: 5,
            options: [
                {
                    description: "Text to change to",
                    displayName: "Text",
                    name: "text",
                    required: true,
                    value: undefined
                }
            ],
            preconditions: [],
            recipeList: [],
            tags: [],
            dataTables: [],
            maintainers: [],
            contributors: [],
            examples: []
        });
    });
});
