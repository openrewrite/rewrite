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
import {RecipeDescriptor, RecipeRegistry} from "../../recipe";
import {withMetrics0} from "./metrics";

export class GetRecipes {
    static handle(connection: rpc.MessageConnection, registry: RecipeRegistry, metricsCsv?: string): void {
        connection.onRequest(
            new rpc.RequestType0<({ name: string } & RecipeDescriptor)[], Error>("GetRecipes"),
            withMetrics0<({ name: string } & RecipeDescriptor)[]>(
                "GetRecipes",
                metricsCsv,
                (context) => async () => {
                    const recipes = [];
                    for (const [_name, recipe] of registry.all.entries()) {
                        recipes.push(await new recipe().descriptor());
                    }
                    context.target = '';
                    return recipes;
                }
            )
        );
    }
}
