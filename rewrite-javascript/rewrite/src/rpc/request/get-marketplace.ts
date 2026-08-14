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
import * as rpc from "vscode-jsonrpc/node";
import {withMetrics0} from "./metrics";
import {CategoryDescriptor, RecipeListing, RecipeMarketplace} from "../../marketplace";
import {Minutes, OptionDescriptor} from "../../recipe";
import {DataTableDescriptor} from "../../data-table";

export interface GetMarketplaceResponseRow {
    /**
     * Listing-weight fields the host builds a RecipeListing from. The full descriptor is fetched
     * lazily per recipe via the separate PrepareRecipe RPC when detail or execution is needed.
     */
    readonly name?: string
    readonly displayName?: string
    readonly description?: string
    readonly estimatedEffortPerOccurrence?: Minutes
    readonly options?: ({ name: string, value?: any } & OptionDescriptor)[]
    readonly dataTables?: DataTableDescriptor[]
    /** 1 + every transitive recipeList entry; the host uses it as a sort key. */
    readonly recipeCount?: number
    readonly categoryPaths: CategoryDescriptor[][]
    /**
     * The package this recipe was contributed by, recorded at install time. Lets the host attribute
     * each row to its own bundle instead of force-tagging every row with the one requested bundle.
     * Undefined for recipes installed from a local path (no package identity).
     */
    readonly packageName?: string
}

/**
 * The {@link RecipeListing} a row stands for, built from its listing-weight fields.
 */
function rowToListing(row: GetMarketplaceResponseRow): RecipeListing {
    const name = row.name ?? "";
    return {
        name,
        displayName: row.displayName ?? name,
        description: row.description ?? "",
        estimatedEffortPerOccurrence: row.estimatedEffortPerOccurrence,
        options: row.options,
        dataTables: row.dataTables,
        recipeCount: row.recipeCount ?? 1,
    };
}

/**
 * Converts GetMarketplace response rows into a hydrated RecipeMarketplace of {@link RecipeListing}s.
 * This is used by the RPC client to reconstruct the marketplace structure.
 */
export async function toMarketplace(rows: GetMarketplaceResponseRow[]): Promise<RecipeMarketplace> {
    const marketplace = new RecipeMarketplace();
    for (const row of rows) {
        const listing = rowToListing(row);
        for (const categoryPath of row.categoryPaths) {
            await marketplace.root.install(listing, categoryPath);
        }
    }
    return marketplace;
}

export class GetMarketplace {
    static handle(connection: rpc.MessageConnection, marketplace: RecipeMarketplace,
                  recipeOrigin: Map<string, string>, metricsCsv?: string): void {
        connection.onRequest(
            new rpc.RequestType0<GetMarketplaceResponseRow[], Error>("GetMarketplace"),
            withMetrics0<GetMarketplaceResponseRow[]>(
                "GetMarketplace",
                metricsCsv,
                (context) => async () => {
                    // Serve the RecipeListings the marketplace already holds, grouping by name and
                    // collecting each recipe's category paths. recipeCount was computed once at
                    // install time; the full descriptor is fetched lazily per recipe via PrepareRecipe.
                    const rowByRecipeId = new Map<string, GetMarketplaceResponseRow & { categoryPaths: CategoryDescriptor[][] }>();

                    function collectRecipes(category: RecipeMarketplace.Category, categoryPath: CategoryDescriptor[]): void {
                        const currentPath = [...categoryPath, category.descriptor];

                        // Add all recipes in this category
                        for (const recipe of category.recipes.keys()) {
                            const existing = rowByRecipeId.get(recipe.name);
                            if (existing) {
                                existing.categoryPaths.push(currentPath);
                            } else {
                                rowByRecipeId.set(recipe.name, {
                                    name: recipe.name,
                                    displayName: recipe.displayName,
                                    description: recipe.description,
                                    estimatedEffortPerOccurrence: recipe.estimatedEffortPerOccurrence,
                                    options: recipe.options,
                                    dataTables: recipe.dataTables,
                                    recipeCount: recipe.recipeCount,
                                    categoryPaths: [currentPath],
                                    packageName: recipeOrigin.get(recipe.name)
                                });
                            }
                        }

                        // Recursively process subcategories
                        for (const subcategory of category.categories) {
                            collectRecipes(subcategory, currentPath);
                        }
                    }

                    // Start from each top-level category (skipping the root)
                    for (const category of marketplace.categories()) {
                        collectRecipes(category, []);
                    }

                    context.target = '';
                    return Array.from(rowByRecipeId.values());
                }
            )
        );
    }
}
