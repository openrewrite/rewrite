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
import {RecipeMarketplace} from "../src";
import {describe} from "@jest/globals";
import {activate} from "../fixtures/example-recipe";
import {ChangeText} from "../fixtures/change-text";

describe("recipes", () => {

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
            recipeList: [],
            tags: [],
            dataTables: []
        });
    });
});
