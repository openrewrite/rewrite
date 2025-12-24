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

export type RecipeConstructor = new (options?: any) => Recipe;

function isRecipeConstructor(recipe: RecipeConstructor | RecipeDescriptor): recipe is RecipeConstructor {
    return typeof recipe === 'function';
}

export class RecipeMarketplace {
    readonly root: RecipeMarketplace.Category = new RecipeMarketplace.Category({
        displayName: "Root",
        description: "This is the root of all categories. When displaying the category hierarchy of a marketplace, this is typically not shown."
    });

    /**
     * Install a recipe into the marketplace under the specified category path.
     * If a RecipeConstructor is provided, it is instantiated to extract its descriptor.
     * If a RecipeDescriptor is provided, it is used directly (for client-side hydration).
     * Categories are specified top-down (shallowest to deepest).
     * Intermediate categories are created as needed.
     *
     * @param recipe The recipe class or descriptor to install
     * @param categoryPath Category path from shallowest to deepest (e.g., ["Java", "Search"])
     */
    public async install(
        recipe: RecipeConstructor | RecipeDescriptor,
        categoryPath: CategoryDescriptor[]
    ): Promise<void> {
        await this.root.install(recipe, categoryPath);
    }

    public categories(): RecipeMarketplace.Category[] {
        return this.root.categories;
    }

    public findRecipe(name: string): [RecipeDescriptor, RecipeConstructor | undefined] | undefined {
        return this.root.findRecipe(name)
    }

    public allRecipes(): RecipeDescriptor[] {
        return this.root.allRecipes()
    }
}

export namespace RecipeMarketplace {
    export class Category {
        readonly categories: Category[] = [];
        readonly recipes: Map<RecipeDescriptor, RecipeConstructor | undefined> = new Map();

        constructor(
            readonly descriptor: CategoryDescriptor,
        ) {
        }

        /**
         * Install a recipe into this category or a subcategory.
         * If a RecipeConstructor is provided, it is instantiated to extract its descriptor.
         * If a RecipeDescriptor is provided, it is used directly (for client-side hydration).
         * Categories are specified top-down (shallowest to deepest).
         * Intermediate categories are created as needed.
         *
         * @param recipe The recipe class or descriptor to install
         * @param categoryPath Category path from shallowest to deepest
         */
        public async install(
            recipe: RecipeConstructor | RecipeDescriptor,
            categoryPath: CategoryDescriptor[]
        ): Promise<void> {
            if (categoryPath.length === 0) {
                if (isRecipeConstructor(recipe)) {
                    try {
                        const recipeInst = new recipe({});
                        this.recipes.set(await recipeInst.descriptor(), recipe);
                    } catch (e) {
                        throw new Error(`Failed to install recipe. Ensure the constructor can be called without any arguments.`);
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

        public findRecipe(name: string): [RecipeDescriptor, RecipeConstructor | undefined] | undefined {
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

        public allRecipes(): RecipeDescriptor[] {
            const result: RecipeDescriptor[] = [...this.recipes.keys()];
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
