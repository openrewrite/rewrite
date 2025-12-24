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
import {RecipeDescriptor} from "../../recipe";

export interface GetMarketplaceResponseRow {
    readonly descriptor: RecipeDescriptor
    readonly categories: CategoryDescriptor[]
}

/**
 * Converts GetMarketplace response rows into a hydrated RecipeMarketplace.
 * This is used by the RPC client to reconstruct the marketplace structure.
 */
export async function toMarketplace(rows: GetMarketplaceResponseRow[]): Promise<RecipeMarketplace> {
    const marketplace = new RecipeMarketplace();
    for (const row of rows) {
        await marketplace.root.install(row.descriptor, row.categories);
    }
    return marketplace;
}

export class GetMarketplace {
    static handle(connection: rpc.MessageConnection, marketplace: RecipeMarketplace, metricsCsv?: string): void {
        connection.onRequest(
            new rpc.RequestType0<GetMarketplaceResponseRow[], Error>("GetMarketplace"),
            withMetrics0<GetMarketplaceResponseRow[]>(
                "GetMarketplace",
                metricsCsv,
                (context) => async () => {
                    const rows: GetMarketplaceResponseRow[] = [];

                    function collectRecipes(category: RecipeMarketplace.Category, categoryPath: CategoryDescriptor[]): void {
                        const currentPath = [...categoryPath, category.descriptor];

                        // Add all recipes in this category
                        for (const recipe of category.recipes.keys()) {
                            rows.push({
                                descriptor: recipe,
                                categories: currentPath
                            });
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
                    return rows;
                }
            )
        );
    }
}
