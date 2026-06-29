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
import {Recipe, RecipeDescriptor} from "@openrewrite/rewrite";
import {RewriteRpc, RpcRecipe} from "@openrewrite/rewrite/rpc";
import {ChangeText} from "./change-text";

/**
 * Mirrors the shape of a real-world composite recipe (e.g. Angular's
 * `UpgradeToAngular21`) whose `recipeList()` mixes locally-defined recipes with
 * recipes that were prepared on a remote RPC peer (via `prepareJavaRecipe` /
 * `rpc.prepareRecipe`, which return {@link RpcRecipe} instances).
 *
 * The remote sub-recipe must not be re-installed by its constructor during
 * `PrepareRecipe` — `RpcRecipe` is a proxy that cannot be constructed without
 * arguments.
 */
export class RecipeWithRpcSubRecipe extends Recipe {
    name = "org.openrewrite.example.text.with-rpc-sub-recipe";
    displayName = "A recipe whose recipe list contains a remote (RPC) recipe";
    description = "To verify that an already-prepared remote recipe can sit in a recipe list.";

    async recipeList(): Promise<Recipe[]> {
        const descriptor: RecipeDescriptor = {
            name: "org.openrewrite.example.text.remote-change-text",
            displayName: "Remote change text",
            instanceName: "Remote change text",
            description: "A stand-in for a recipe prepared on a remote RPC peer.",
            tags: [],
            estimatedEffortPerOccurrence: 5,
            options: [],
            preconditions: [],
            recipeList: [],
            dataTables: [],
            maintainers: [],
            contributors: [],
            examples: []
        };
        return [
            new ChangeText({text: "hello"}),
            new RpcRecipe(RewriteRpc.get()!, "remote-id", descriptor, "")
        ];
    }
}
