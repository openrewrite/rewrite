/*
 * Copyright 2026 the original author or authors.
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
import {ExecutionContext, noopVisitor, Option, Recipe, TreeVisitor} from "../src";

class RecipeWithSecret extends Recipe {
    name = "org.example.RecipeWithSecret";
    displayName = "Recipe with secret";
    description = "Recipe with secret option.";

    @Option({
        displayName: "API token",
        description: "API token used by the recipe.",
        secret: true,
    })
    apiToken!: string;

    constructor(options: { apiToken: string }) {
        super(options);
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return noopVisitor();
    }
}

describe("secret option", () => {
    test("propagates secret=true through the recipe descriptor", async () => {
        const recipe = new RecipeWithSecret({apiToken: "hunter2"});
        const descriptor = await recipe.descriptor();

        expect(descriptor.options).toHaveLength(1);
        const opt = descriptor.options[0];
        expect(opt.name).toBe("apiToken");
        expect(opt.secret).toBe(true);
        // Raw value is preserved on the descriptor; redaction is a persistence
        // boundary concern, not a source-level concern.
        expect(opt.value).toBe("hunter2");
    });

    test("secret defaults to undefined (i.e. non-secret) when not specified", async () => {
        class RecipeWithoutSecret extends Recipe {
            name = "org.example.RecipeWithoutSecret";
            displayName = "Recipe";
            description = "Recipe.";

            @Option({
                displayName: "Pattern",
                description: "A pattern.",
            })
            pattern!: string;

            constructor(options: { pattern: string }) {
                super(options);
            }

            async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
                return noopVisitor();
            }
        }

        const recipe = new RecipeWithoutSecret({pattern: "foo"});
        const descriptor = await recipe.descriptor();
        const opt = descriptor.options[0];
        expect(opt.secret).toBeUndefined();
    });
});
