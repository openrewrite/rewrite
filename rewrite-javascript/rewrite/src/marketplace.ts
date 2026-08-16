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
import {Minutes, OptionDescriptor, Recipe, RecipeDescriptor} from "./recipe";
import {DataTableDescriptor} from "./data-table";

export type RecipeConstructor = new (options?: any) => Recipe;

/**
 * Listing-weight view of a recipe: everything the marketplace needs to answer InstallRecipes and
 * GetMarketplace without holding the full recursive descriptor. The full tree is materialized lazily
 * per recipe by PrepareRecipe. recipeCount is 1 + every transitive recipeList entry, computed once at
 * install time (the host uses it as a sort key).
 */
export interface RecipeListing {
    readonly name: string
    readonly displayName: string
    readonly description: string
    readonly estimatedEffortPerOccurrence?: Minutes
    readonly options?: ({ name: string, value?: any } & OptionDescriptor)[]
    readonly dataTables?: DataTableDescriptor[]
    readonly recipeCount: number
}

function isRecipeConstructor(recipe: RecipeConstructor | RecipeListing): recipe is RecipeConstructor {
    return typeof recipe === 'function';
}

/**
 * The count of this recipe and all recipes nested transitively in its recipeList.
 */
function countRecipes(descriptor: RecipeDescriptor): number {
    let count = 1;
    for (const sub of descriptor.recipeList ?? []) {
        count += countRecipes(sub);
    }
    return count;
}

/**
 * Derives the listing-weight view from a full descriptor, collapsing its recursive recipeList to a
 * count. Called once at install time so the marketplace never re-walks the tree to list.
 */
export function toRecipeListing(descriptor: RecipeDescriptor): RecipeListing {
    return {
        name: descriptor.name,
        displayName: descriptor.displayName ?? descriptor.name,
        description: descriptor.description ?? "",
        estimatedEffortPerOccurrence: descriptor.estimatedEffortPerOccurrence,
        options: descriptor.options,
        dataTables: descriptor.dataTables,
        recipeCount: countRecipes(descriptor),
    };
}

export class RecipeMarketplace {
    readonly root: RecipeMarketplace.Category = new RecipeMarketplace.Category({
        displayName: "Root",
        description: "This is the root of all categories. When displaying the category hierarchy of a marketplace, this is typically not shown."
    });

    /**
     * Install a recipe into the marketplace under the specified category path.
     * A RecipeConstructor is instantiated once to derive its {@link RecipeListing} (and the
     * constructor is retained so PrepareRecipe can later build the full tree). A RecipeListing is
     * stored directly (for client-side hydration). Categories are specified top-down (shallowest to
     * deepest); intermediate categories are created as needed.
     *
     * @param recipe The recipe class or listing to install
     * @param categoryPath Category path from shallowest to deepest (e.g., ["Java", "Search"])
     */
    public async install(
        recipe: RecipeConstructor | RecipeListing,
        categoryPath: CategoryDescriptor[]
    ): Promise<void> {
        await this.root.install(recipe, categoryPath);
    }

    public categories(): RecipeMarketplace.Category[] {
        return this.root.categories;
    }

    public findRecipe(name: string): [RecipeListing, RecipeConstructor | undefined] | undefined {
        return this.root.findRecipe(name)
    }

    public allRecipes(): RecipeListing[] {
        return this.root.allRecipes()
    }
}

export namespace RecipeMarketplace {
    export class Category {
        readonly categories: Category[] = [];
        readonly recipes: Map<RecipeListing, RecipeConstructor | undefined> = new Map();

        constructor(
            readonly descriptor: CategoryDescriptor,
        ) {
        }

        /**
         * Install a recipe into this category or a subcategory.
         * A RecipeConstructor is instantiated once to derive its {@link RecipeListing} (the
         * constructor is retained for PrepareRecipe). A RecipeListing is stored directly (for
         * client-side hydration). Categories are specified top-down (shallowest to deepest);
         * intermediate categories are created as needed.
         *
         * @param recipe The recipe class or listing to install
         * @param categoryPath Category path from shallowest to deepest
         */
        public async install(
            recipe: RecipeConstructor | RecipeListing,
            categoryPath: CategoryDescriptor[]
        ): Promise<void> {
            if (categoryPath.length === 0) {
                if (isRecipeConstructor(recipe)) {
                    try {
                        const recipeInst = new recipe({});
                        this.recipes.set(toRecipeListing(await recipeInst.descriptor()), recipe);
                    } catch (e) {
                        // Surface the underlying cause inline: it is dropped at the
                        // JSON-RPC serialization boundary (only `message` survives), so
                        // folding it into the message is the only way it reaches the
                        // caller's logs. Without this, a failure deeper in the recipe
                        // (e.g. a sub-recipe needing an RPC connection) is hidden behind
                        // the generic "constructor" hint. See gh-7968.
                        const cause = e instanceof Error ? e.message : String(e);
                        const err = new Error(`Failed to install recipe '${recipe.name}'. Ensure the constructor can be called without any arguments. Cause: ${cause}`);
                        (err as any).cause = e;
                        throw err;
                    }
                } else {
                    this.recipes.set(recipe, undefined);
                }
                return;
            }

            // Get the first category in the path
            const firstCategory = categoryPath[0];
            const targetCategory = this.findOrCreateCategory(firstCategory);

            // Recursively add to the child category
            await targetCategory.install(recipe, categoryPath.slice(1));
        }

        private findOrCreateCategory(categoryDescriptor: CategoryDescriptor): Category {
            for (const category of this.categories) {
                if (category.descriptor.displayName === categoryDescriptor.displayName) {
                    return category;
                }
            }
            const newCategory = new Category(categoryDescriptor);
            this.categories.push(newCategory);
            return newCategory;
        }

        public findRecipe(name: string): [RecipeListing, RecipeConstructor | undefined] | undefined {
            for (const [recipe, ctor] of this.recipes.entries()) {
                if (recipe.name === name) {
                    return [recipe, ctor];
                }
            }
            for (const category of this.categories) {
                const found = category.findRecipe(name);
                if (found) {
                    return found;
                }
            }
            return undefined;
        }

        public allRecipes(): RecipeListing[] {
            const result: RecipeListing[] = [...this.recipes.keys()];
            for (const category of this.categories) {
                result.push(...category.allRecipes());
            }
            return result;
        }
    }
}

export const JavaScript: CategoryDescriptor[] = [{displayName: "JavaScript"}]

export interface CategoryDescriptor {
    displayName: string,
    description?: string
}
