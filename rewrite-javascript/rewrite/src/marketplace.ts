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
import {Recipe, RecipeDescriptor} from "./recipe";

export class RecipeMarketplace {
    readonly root: RecipeMarketplace.Category = new RecipeMarketplace.Category({
        displayName: "Root",
        description: "This is the root of all categories. When displaying the category hierarchy of a marketplace, this is typically not shown."
    });

    /**
     * Install a recipe class into the marketplace under the specified category path.
     * The recipe is instantiated to extract its descriptor and create a listing.
     * Categories are specified top-down (shallowest to deepest).
     * Intermediate categories are created as needed.
     *
     * @param recipeClass The recipe class to install
     * @param categoryPath Category path from shallowest to deepest (e.g., ["Java", "Search"])
     * @param bundle Optional bundle information (defaults to npm @openrewrite/rewrite)
     */
    public async install<T extends Recipe>(
        recipeClass: new (options?: any) => T,
        categoryPath: CategoryDescriptor[],
        bundle: RecipeBundle
    ): Promise<void> {
        await this.root.install(recipeClass, categoryPath, bundle);
    }

    public categories(): RecipeMarketplace.Category[] {
        return this.root.categories;
    }

    public findRecipe(name: string): RecipeDescriptor | undefined {
        return this.root.findRecipe(name);
    }
}

export namespace RecipeMarketplace {
    export class Category {
        readonly categories: Category[] = [];
        readonly recipes: RecipeDescriptor[] = [];

        constructor(
            readonly descriptor: CategoryDescriptor,
        ) {
        }

        /**
         * Install a recipe class into this category or a subcategory.
         * The recipe is instantiated to extract its descriptor and create a listing.
         * Categories are specified top-down (shallowest to deepest).
         * Intermediate categories are created as needed.
         *
         * @param recipeClass The recipe class to install
         * @param categoryPath Category path from shallowest to deepest
         * @param bundle Bundle information for the recipe
         */
        public async install<T extends Recipe>(
            recipeClass: new (options?: any) => T,
            categoryPath: CategoryDescriptor[],
            bundle: RecipeBundle
        ): Promise<void> {
            if (categoryPath.length === 0) {
                try {
                    const recipe = new recipeClass({});
                    this.recipes.push(await recipe.descriptor());
                } catch (e) {
                    throw new Error(`Failed to install recipe. Ensure the constructor can be called without any arguments.`);
                }
                return;
            }

            // Get the first category in the path
            const firstCategory = categoryPath[0];
            const targetCategory = this.findOrCreateCategory(firstCategory);

            // Recursively add to the child category
            await targetCategory.install(recipeClass, categoryPath.slice(1), bundle);
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

        public findRecipe(name: string): RecipeDescriptor | undefined {
            for (const recipe of this.recipes) {
                if (recipe.name === name) {
                    return recipe;
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

        public allRecipes(): RecipeDescriptor[] {
            const result: RecipeDescriptor[] = [...this.recipes];
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

export interface RecipeBundle {
    readonly packageEcosystem: string
    readonly packageName: string
    readonly version: string
    readonly team?: string
}
