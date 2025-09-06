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
import {MessageConnection} from "vscode-jsonrpc/node";
import {Recipe, RecipeDescriptor, RecipeRegistry, ScanningRecipe} from "../../recipe";
import {SnowflakeId} from "@akashrajpurohit/snowflake-id";
import {Check} from "../../preconditions";
import {RpcRecipe} from "../recipe";
import {TreeVisitor} from "../../visitor";
import {ExecutionContext} from "../../execution";

export class PrepareRecipe {
    constructor(private readonly id: string, private readonly options?: any) {
    }

    static handle(connection: MessageConnection,
                  registry: RecipeRegistry,
                  preparedRecipes: Map<String, Recipe>) {
        const snowflake = SnowflakeId();
        connection.onRequest(new rpc.RequestType<PrepareRecipe, PrepareRecipeResponse, Error>("PrepareRecipe"), async (request) => {
            const id = snowflake.generate();
            const recipeCtor = registry.all.get(request.id);
            if (!recipeCtor) {
                throw new Error(`Could not find recipe with id ${request.id}`);
            }
            let recipe = new recipeCtor(request.options);

            // For preconditions that can be evaluated on the remote peer, let the remote peer
            // evaluate them and know that we will only have to do the visit work if the
            // precondition passes.
            const editor = await recipe.editor()
            let editPreconditionVisitor: string | undefined = undefined;
            if (editor instanceof Check && editor.check instanceof RpcRecipe) {
                editPreconditionVisitor = editor.check.editVisitor;
                recipe = Object.assign(
                    Object.create(Object.getPrototypeOf(recipe)),
                    recipe,
                    {
                        async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
                            return editor.v;
                        }
                    }
                )
            }

            preparedRecipes.set(id, recipe);

            return {
                id: id,
                descriptor: await recipe.descriptor(),
                editVisitor: `edit:${id}`,
                editPreconditionVisitor: editPreconditionVisitor,
                scanVisitor: recipe instanceof ScanningRecipe ? `scan:${id}` : undefined,
                // TODO don't yet support short-circuiting preconditions on the scanning phase
                scanPreconditionVisitor: undefined
            }
        });
    }
}

export interface PrepareRecipeResponse {
    id: string
    descriptor: RecipeDescriptor
    editVisitor: string
    editPreconditionVisitor?: string,
    scanVisitor?: string
    scanPreconditionVisitor?: string
}
