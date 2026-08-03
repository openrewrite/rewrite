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
import {CategoryDescriptor, RecipeMarketplace} from "../../marketplace";
import {Minutes, OptionDescriptor, RecipeDescriptor} from "../../recipe";
import {DataTableDescriptor} from "../../data-table";

export interface GetMarketplaceResponseRow {
    /**
     * Listing-weight fields ({@link name} through {@link recipeCount}) carry everything the host
     * needs to list the marketplace without materializing recipes. Up-to-date engines populate
     * these and leave {@link descriptor} undefined; the full descriptor is fetched lazily per recipe
     * via the separate PrepareRecipe RPC. A defined {@link name} selects this path in
     * {@link toMarketplace}.
     */
    readonly name?: string
    readonly displayName?: string
    readonly description?: string
    readonly estimatedEffortPerOccurrence?: Minutes
    readonly options?: ({ name: string, value?: any } & OptionDescriptor)[]
    readonly dataTables?: DataTableDescriptor[]
    /** 1 + every transitive recipeList entry; the host uses it as a sort key. */
    readonly recipeCount?: number
    /**
     * The full, recursive descriptor. Retained for backward compatibility with peers that still
     * emit it, but no longer emitted by up-to-date engines — prefer the lightweight fields above.
     */
    readonly descriptor?: RecipeDescriptor
    readonly categoryPaths: CategoryDescriptor[][]
    /**
     * The package this recipe was contributed by, recorded at install time. Lets the host attribute
     * each row to its own bundle instead of force-tagging every row with the one requested bundle.
     * Undefined for recipes installed from a local path (no package identity).
     */
    readonly packageName?: string
}

/**
 * 1 (this recipe) plus every transitive entry in its recipeList. The host uses this as a sort key
 * in place of the recursive recipeList it would otherwise walk to compute it.
 */
function countRecipes(descriptor: RecipeDescriptor): number {
    let count = 1;
    for (const sub of descriptor.recipeList ?? []) {
        count += countRecipes(sub);
    }
    return count;
}

/**
 * Reconstructs the RecipeDescriptor a row stands for. Prefers the listing-weight fields (empty
 * recipeList/preconditions — detail is fetched lazily via PrepareRecipe); falls back to a
 * legacy peer's full {@link GetMarketplaceResponseRow.descriptor} when {@link name} is absent.
 */
function rowToDescriptor(row: GetMarketplaceResponseRow): RecipeDescriptor {
    if (row.name != null) {
        const displayName = row.displayName ?? row.name;
        return {
            name: row.name,
            displayName,
            instanceName: displayName,
            description: row.description ?? "",
            tags: [],
            estimatedEffortPerOccurrence: row.estimatedEffortPerOccurrence ?? 5,
            options: row.options ?? [],
            preconditions: [],
            recipeList: [],
            dataTables: row.dataTables ?? [],
            maintainers: [],
            contributors: [],
            examples: [],
        };
    }
    return row.descriptor!;
}

/**
 * Converts GetMarketplace response rows into a hydrated RecipeMarketplace.
 * This is used by the RPC client to reconstruct the marketplace structure.
 */
export async function toMarketplace(rows: GetMarketplaceResponseRow[]): Promise<RecipeMarketplace> {
    const marketplace = new RecipeMarketplace();
    for (const row of rows) {
        const descriptor = rowToDescriptor(row);
        for (const categoryPath of row.categoryPaths) {
            await marketplace.root.install(descriptor, categoryPath);
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
                    // Group recipes by name, collecting all category paths for each. Emit
                    // listing-weight fields only: the host builds its marketplace from these
                    // without the full recursive descriptor, which it fetches lazily per recipe
                    // via PrepareRecipe. recipeCount collapses the transitive recipeList.
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
                                    recipeCount: countRecipes(recipe),
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
