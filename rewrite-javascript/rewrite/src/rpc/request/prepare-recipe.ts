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
import {withMetrics} from "./metrics";

export class PrepareRecipe {
    constructor(private readonly id: string, private readonly options?: any) {
    }

    static handle(connection: MessageConnection,
                  registry: RecipeRegistry,
                  preparedRecipes: Map<String, Recipe>,
                  metricsCsv?: string) {
        const snowflake = SnowflakeId();
        connection.onRequest(
            new rpc.RequestType<PrepareRecipe, PrepareRecipeResponse, Error>("PrepareRecipe"),
            withMetrics<PrepareRecipe, PrepareRecipeResponse>(
                "PrepareRecipe",
                metricsCsv,
                (context) => async (request) => {
                    context.target = request.id;
                    const id = snowflake.generate();
                    const recipeCtor = registry.all.get(request.id);
                    if (!recipeCtor) {
                        throw new Error(`Could not find recipe with id ${request.id}`);
                    }
                    let recipe = new recipeCtor(request.options);

                    const editPreconditions: Precondition[] = [];
                    recipe = await this.optimizePreconditions(recipe, "edit", editPreconditions);

                    const scanPreconditions: Precondition[] = [];
                    recipe = await this.optimizePreconditions(recipe, "scan", scanPreconditions);

                    preparedRecipes.set(id, recipe);

                    const result = {
                        id: id,
                        descriptor: await recipe.descriptor(),
                        editVisitor: `edit:${id}`,
                        editPreconditions: editPreconditions,
                        scanVisitor: recipe instanceof ScanningRecipe ? `scan:${id}` : undefined,
                        scanPreconditions: scanPreconditions
                    };

                    return result;
                }
            )
        );
    }

    /**
     * For preconditions that can be evaluated on the remote peer, let the remote peer
     * evaluate them and know that we will only have to do the visit work if the
     * precondition passes.
     */
    private static async optimizePreconditions(recipe: Recipe, phase: "edit" | "scan", preconditions: Precondition[]): Promise<Recipe> {
        let visitor: TreeVisitor<any, ExecutionContext>;
        if (phase === "edit") {
            visitor = await recipe.editor();
        } else if (phase === "scan") {
            if (recipe instanceof ScanningRecipe) {
                visitor = await recipe.scanner(undefined);
            } else {
                return recipe;
            }
        }

        if (visitor! instanceof Check) {
            if (visitor.check instanceof RpcRecipe) {
                preconditions.push({visitorName: phase === "edit" ? visitor.check.editVisitor : visitor.check.scanVisitor!});
                recipe = Object.assign(
                    Object.create(Object.getPrototypeOf(recipe)),
                    recipe,
                    phase === "edit" ?
                        {
                            async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
                                return visitor.v;
                            }
                        } :
                        {
                            async scanner(acc: any): Promise<TreeVisitor<any, ExecutionContext>> {
                                const checkVisitor = await (recipe as ScanningRecipe<any>).scanner(acc);
                                return (checkVisitor as Check<any>).v;
                            }
                        }
                )
            }
            this.visitorTypePrecondition(preconditions, visitor.v);
        } else {
            this.visitorTypePrecondition(preconditions, visitor!);
        }
        return recipe;
    }

    private static visitorTypePrecondition(preconditions: Precondition[], v: TreeVisitor<any, ExecutionContext>): Precondition[] {
        let treeType: string | undefined;
        
        // Use CommonJS require to defer loading and avoid circular dependencies
        const {JsonVisitor} = require("../../json");
        const {JavaScriptVisitor} = require("../../javascript");
        const {JavaVisitor} = require("../../java");
        
        if (v instanceof JsonVisitor) {
            treeType = "org.openrewrite.json.tree.Json";
        } else if (v instanceof JavaScriptVisitor) {
            // Order is important here! JavaScriptVisitor is a subclass of JavaVisitor
            // and so must appear first in these conditional statements
            treeType = "org.openrewrite.javascript.tree.JS";
        } else if (v instanceof JavaVisitor) {
            treeType = "org.openrewrite.java.tree.J";
        }
        if (treeType) {
            preconditions.push({
                visitorName: "org.openrewrite.rpc.internal.FindTreesOfType",
                visitorOptions: {type: treeType}
            });
        }
        return preconditions;
    }
}

export interface PrepareRecipeResponse {
    id: string
    descriptor: RecipeDescriptor
    editVisitor: string
    editPreconditions: Precondition[]
    scanVisitor?: string
    scanPreconditions: Precondition[]
}

export interface Precondition {
    visitorName: string
    visitorOptions?: {}
}
