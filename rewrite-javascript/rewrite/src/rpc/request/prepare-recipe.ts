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
import {Recipe, RecipeDescriptor, ScanningRecipe} from "../../recipe";
import {SnowflakeId} from "@akashrajpurohit/snowflake-id";
import {Check, CheckArg, CompositePrecondition, RecipeRef} from "../../preconditions";
import {RpcRecipe} from "../recipe";
import {TreeVisitor} from "../../visitor";
import {ExecutionContext} from "../../execution";
import {withMetrics} from "./metrics";
import {RecipeMarketplace} from "../../marketplace";

export class PrepareRecipe {
    constructor(private readonly id: string, private readonly options?: any) {
    }

    static handle(connection: MessageConnection,
                  marketplace: RecipeMarketplace,
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
                    const recipeCtor = marketplace.findRecipe(request.id);
                    if (!recipeCtor) {
                        // A miss means the host owns this recipe (e.g. a sub-recipe that delegates to
                        // a Java recipe, produced by prepareJavaRecipe — an RpcRecipe hosted on the peer
                        // with no no-arg constructor to register here). Tell the host to resolve the id
                        // locally via delegatesTo rather than failing with "Could not find recipe ...".
                        const id = snowflake.generate();
                        return {
                            id: id,
                            descriptor: PrepareRecipe.delegateDescriptor(request.id),
                            editVisitor: `edit:${id}`,
                            editPreconditions: [],
                            scanPreconditions: [],
                            delegatesTo: {
                                recipeName: request.id,
                                options: request.options ?? {}
                            }
                        };
                    }
                    if (!recipeCtor[1]) {
                        throw new Error(`Recipe ${request.id} was installed without a constructor`);
                    }
                    return await PrepareRecipe.prepareInstance(new recipeCtor[1](request.options),
                        snowflake, preparedRecipes, marketplace);
                }
            )
        );
    }

    /**
     * Minimal stand-in descriptor for a recipe the host will resolve locally via
     * {@link PrepareRecipeResponse.delegatesTo}. The host reads only `delegatesTo` for
     * delegated recipes and ignores this descriptor, but the response type requires one.
     */
    private static delegateDescriptor(name: string): RecipeDescriptor {
        return {
            name: name,
            displayName: name,
            instanceName: name,
            description: "",
            tags: [],
            estimatedEffortPerOccurrence: 5,
            options: [],
            preconditions: [],
            recipeList: [],
            dataTables: [],
            maintainers: [],
            contributors: [],
            examples: []
        };
    }

    /**
     * Prepares a recipe instance and recursively its whole child tree, registering every node in
     * {@code preparedRecipes} and returning the prepared response with {@code recipeList} populated,
     * so the host builds the tree locally instead of a PrepareRecipe round-trip per child.
     *
     * Mirrors the C# server's PrepareInstance. Required options are validated as each node is
     * prepared (covering the whole tree). A child hosted on the peer (an {@link RpcRecipe}, e.g. from
     * prepareJavaRecipe) carries only {@code delegatesTo} for the host to resolve locally; every other
     * child carries its own {@code recipeList}. Delegating recipes forward validation to the recipe
     * they delegate to, so they are not validated here.
     */
    private static async prepareInstance(recipe: Recipe,
                                         snowflake: ReturnType<typeof SnowflakeId>,
                                         preparedRecipes: Map<String, Recipe>,
                                         marketplace: RecipeMarketplace): Promise<PrepareRecipeResponse> {
        const id = snowflake.generate();

        const editPreconditions: Precondition[] = [];
        recipe = await PrepareRecipe.optimizePreconditions(recipe, "edit", editPreconditions);
        const scanPreconditions: Precondition[] = [];
        recipe = await PrepareRecipe.optimizePreconditions(recipe, "scan", scanPreconditions);

        preparedRecipes.set(id, recipe);

        const descriptor = await recipe.descriptor();
        const isDelegating = 'javaRecipeName' in recipe;

        if (!isDelegating) {
            for (const option of descriptor.options) {
                if ((option.required ?? true) && option.value == null) {
                    throw new Error(`Missing required option \`${option.name}\` for recipe \`${descriptor.name}\`.`);
                }
            }
        }

        const response: PrepareRecipeResponse = {
            id: id,
            descriptor: descriptor,
            editVisitor: `edit:${id}`,
            editPreconditions: editPreconditions,
            scanVisitor: recipe instanceof ScanningRecipe ? `scan:${id}` : undefined,
            scanPreconditions: scanPreconditions
        };

        if (isDelegating) {
            response.delegatesTo = {
                recipeName: (recipe as any).javaRecipeName,
                options: (recipe as any).delegatesToOptions ?? {}
            };
            return response;
        }

        const childResponses: PrepareRecipeResponse[] = [];
        for (const child of await recipe.recipeList()) {
            if (child instanceof RpcRecipe) {
                // Hosted on the peer: emit delegatesTo (its name + the options its parent set) so the
                // host resolves it locally, matching how the by-name fallback used to resolve it.
                const childId = snowflake.generate();
                const childDescriptor = await child.descriptor();
                const options: Record<string, any> = {};
                for (const option of childDescriptor.options) {
                    if (option.value != null) {
                        options[option.name] = option.value;
                    }
                }
                childResponses.push({
                    id: childId,
                    descriptor: PrepareRecipe.delegateDescriptor(child.name),
                    editVisitor: `edit:${childId}`,
                    editPreconditions: [],
                    scanPreconditions: [],
                    delegatesTo: {recipeName: child.name, options}
                });
            } else {
                // Register a child that was instantiated in recipeList() but never installed, so a peer
                // that re-prepares children by name (rather than consuming recipeList) can still find it.
                if (!marketplace.findRecipe(child.name)) {
                    await marketplace.install(child.constructor as any, []);
                }
                childResponses.push(await PrepareRecipe.prepareInstance(child, snowflake, preparedRecipes, marketplace));
            }
        }
        response.recipeList = childResponses;

        return response;
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
            const wireEntry = this.conditionWireEntry(visitor.check, phase);
            if (wireEntry) {
                preconditions.push(wireEntry);
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
            await this.visitorTypePrecondition(preconditions, visitor.v);
        } else {
            await this.visitorTypePrecondition(preconditions, visitor!);
        }
        return recipe;
    }

    /**
     * Translate a precondition condition (operand) to a wire entry.
     *
     * Mirrors the Java {@code PrepareRecipeResponse.Precondition} schema:
     * leaves carry {@code visitorName} (+ optional options); composites
     * carry {@code op} ({@code "or"}/{@code "and"}/{@code "not"}) and a
     * nested {@code operands} list. Returns {@code undefined} when the
     * condition can't be serialized — the caller leaves the wrapper
     * intact so the gate runs in-process as a fallback.
     */
    private static conditionWireEntry(condition: CheckArg, phase: "edit" | "scan"): Precondition | undefined {
        if (condition instanceof CompositePrecondition) {
            const operands: Precondition[] = [];
            for (const operand of condition.operands) {
                const entry = this.conditionWireEntry(operand, phase);
                if (entry === undefined) {
                    return undefined;
                }
                operands.push(entry);
            }
            return {op: condition.op, operands};
        }
        // Common case: helpers like usesMethod / usesType return a lightweight
        // RecipeRef so the recipe author can declare a precondition without
        // firing an RPC at editor() time. The Java host's
        // PreparedRecipeCache.instantiateVisitor constructs the named recipe
        // via Jackson and uses its visitor.
        if (condition instanceof RecipeRef) {
            return {visitorName: condition.recipeName, visitorOptions: {...condition.options}};
        }
        if (condition instanceof RpcRecipe) {
            return {visitorName: phase === "edit" ? condition.editVisitor : condition.scanVisitor!};
        }
        return undefined;
    }

    private static async visitorTypePrecondition(preconditions: Precondition[], v: TreeVisitor<any, ExecutionContext>): Promise<Precondition[]> {
        let treeType: string | undefined;

        // Use dynamic import to defer loading and avoid circular dependencies
        const {JsonVisitor} = await import("../../json/index.js");
        const {JavaScriptVisitor} = await import("../../javascript/index.js");
        const {JavaVisitor} = await import("../../java/index.js");
        const {PlainTextVisitor} = await import("../../text/index.js");
        const {YamlVisitor} = await import("../../yaml/index.js");

        if (v instanceof JsonVisitor) {
            treeType = "org.openrewrite.json.tree.Json";
        } else if (v instanceof JavaScriptVisitor) {
            // Order is important here! JavaScriptVisitor is a subclass of JavaVisitor
            // and so must appear first in these conditional statements
            treeType = "org.openrewrite.javascript.tree.JS";
        } else if (v instanceof JavaVisitor) {
            treeType = "org.openrewrite.java.tree.J";
        } else if (v instanceof PlainTextVisitor) {
            treeType = "org.openrewrite.text.PlainText";
        } else if (v instanceof YamlVisitor) {
            treeType = "org.openrewrite.yaml.tree.Yaml";
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

export interface DelegatesTo {
    recipeName: string
    options: Record<string, any>
}

export interface PrepareRecipeResponse {
    id: string
    descriptor: RecipeDescriptor
    editVisitor: string
    editPreconditions: Precondition[]
    scanVisitor?: string
    scanPreconditions: Precondition[]
    delegatesTo?: DelegatesTo
    /**
     * The prepared child recipes of a composite. When present, the host builds the recipe tree
     * locally from these instead of re-preparing each child by name (the whole-tree optimization).
     */
    recipeList?: PrepareRecipeResponse[]
}

/**
 * Either a leaf (a single visitor identified by {@code visitorName} +
 * optional {@code visitorOptions}) or a composite of nested preconditions
 * joined by {@code op} ({@code "or"} / {@code "and"} / {@code "not"}).
 *
 * When {@code op} is undefined the entry is a leaf and {@code visitorName}
 * is required; when {@code op} is set, {@code operands} carries the
 * children and the visitor fields are ignored. The composite form mirrors
 * Java's {@code Preconditions.or}/{@code and}/{@code not} so remote
 * languages can express the same gate shapes the Java side does.
 */
export interface Precondition {
    visitorName?: string
    visitorOptions?: {}
    op?: "or" | "and" | "not"
    operands?: Precondition[]
}
