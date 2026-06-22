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
import {JavaScript, Recipe, RecipeMarketplace} from "@openrewrite/rewrite";
import {prepareJavaRecipe} from "@openrewrite/rewrite/rpc";

export async function activate(marketplace: RecipeMarketplace) {
    await marketplace.install(CompositeWithJavaDelegate, JavaScript);
}

/**
 * Mirrors the shape of Angular's `UpgradeToAngular19`: a JS composite whose
 * {@code recipeList()} contains a recipe that delegates entirely to a Java recipe
 * (here {@code org.openrewrite.javascript.UpgradeDependencyVersion}, via
 * {@link prepareJavaRecipe}).
 *
 * The host re-prepares each child of this composite by id while building
 * {@code RpcRecipe.getRecipeList()}. The Java-delegate child is an {@code RpcRecipe}
 * that {@code installSubRecipes} does not register in this server's marketplace, so the
 * re-prepare misses here and the server must answer with {@code delegatesTo} (rather than
 * throwing) so the host resolves it from its own marketplace as a local Java recipe.
 */
class CompositeWithJavaDelegate extends Recipe {
    name = "org.openrewrite.example.npm.composite-with-java-delegate";
    displayName = "Composite with a Java-delegate child";
    description = "A JS composite whose recipe list contains a recipe that delegates to a Java recipe.";

    async recipeList(): Promise<Recipe[]> {
        return [
            await prepareJavaRecipe("org.openrewrite.javascript.UpgradeDependencyVersion", {
                packagePattern: "@angular/*",
                newVersion: "19.x"
            })
        ];
    }
}
