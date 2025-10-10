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
import {Recipe, ScanningRecipe} from "../../recipe";
import {Cursor, rootCursor} from "../../tree";
import {ExecutionContext} from "../../execution";
import {withMetrics} from "./metrics";

export interface GenerateResponse {
    ids: string[]
    sourceFileTypes: string[]
}

export class Generate {
    constructor(private readonly id: string, private readonly p: string) {
    }

    static handle(connection: rpc.MessageConnection,
                  localObjects: Map<string, any>,
                  preparedRecipes: Map<String, Recipe>,
                  recipeCursors: WeakMap<Recipe, Cursor>,
                  getObject: (id: string) => any,
                  metricsCsv?: string): void {
        connection.onRequest(
            new rpc.RequestType<Generate, GenerateResponse, Error>("Generate"),
            withMetrics<Generate, GenerateResponse>(
                "Generate",
                metricsCsv,
                (context) => async (request) => {
                    context.target = request.id;
                    const recipe = preparedRecipes.get(request.id);
                    const response = {
                        ids: [],
                        sourceFileTypes: []
                    } as GenerateResponse;

                    if (recipe && recipe instanceof ScanningRecipe) {
                        let cursor = recipeCursors.get(recipe);
                        if (!cursor) {
                            cursor = rootCursor();
                            recipeCursors.set(recipe, cursor);
                        }
                        const ctx = getObject(request.p) as ExecutionContext;
                        const acc = recipe.accumulator(cursor, ctx);
                        const generated = await recipe.generate(acc, ctx)

                        for (const g of generated) {
                            localObjects.set(g.id.toString(), g);
                            response.ids.push(g.id.toString());
                            response.sourceFileTypes.push(g.kind);
                        }

                    }
                    return response;
                }
            )
        );
    }
}
