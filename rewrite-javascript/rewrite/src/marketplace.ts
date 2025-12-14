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
     */
    public async install(
        recipeClass: RecipeConstructor,
        categoryPath: CategoryDescriptor[]
    ): Promise<void> {
        await this.root.install(recipeClass, categoryPath);
    }

    public categories(): RecipeMarketplace.Category[] {
        return this.root.categories;
    }

    public findRecipe(name: string): [ RecipeDescriptor, RecipeConstructor] | undefined {
        return this.root.findRecipe(name)
    }

    public allRecipes(): RecipeDescriptor[] {
        return this.root.allRecipes()
    }
}

export namespace RecipeMarketplace {
    export class Category {
        readonly categories: Category[] = [];
        readonly recipes: Map<RecipeDescriptor, RecipeConstructor> = new Map();

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
         * @param recipeConstructor The recipe class to install
         * @param categoryPath Category path from shallowest to deepest
         */
        public async install<T extends Recipe>(
            recipeConstructor: new (options?: any) => T,
            categoryPath: CategoryDescriptor[]
        ): Promise<void> {
            if (categoryPath.length === 0) {
                try {
                    const recipe = new recipeConstructor({});
                    this.recipes.set(await recipe.descriptor(), recipeConstructor);
                } catch (e) {
                    throw new Error(`Failed to install recipe. Ensure the constructor can be called without any arguments.`);
                }
                return;
            }

            // Get the first category in the path
            const firstCategory = categoryPath[0];
            const targetCategory = this.findOrCreateCategory(firstCategory);

            // Recursively add to the child category
            await targetCategory.install(recipeConstructor, categoryPath.slice(1));
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

        public findRecipe(name: string): [ RecipeDescriptor, RecipeConstructor] | undefined {
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
