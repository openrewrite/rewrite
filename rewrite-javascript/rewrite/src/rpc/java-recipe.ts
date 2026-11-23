/*
 * Copyright 2026 the original author or authors.
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
import {RewriteRpc} from "./rewrite-rpc";
import type {RpcRecipe} from "./recipe";
import {Recipe, RecipeDescriptor, ScanningRecipe} from "../recipe";
import {TreeVisitor} from "../visitor";
import {ExecutionContext} from "../execution";
import {SourceFile} from "../tree";

/**
 * A stand-in for a Java recipe, composable like any other recipe (a `recipeList()` entry, a
 * `check(...)` gate, a `RecipeSpec.recipe`). The Java host resolves it by name: from `delegatesTo`
 * when it prepares the enclosing recipe, or by class name when it gates a visitor, like a `RecipeRef`.
 * Only when this process runs it itself (a test, a JS-hosted run) is it prepared over the active
 * {@link RewriteRpc} connection, on first use.
 */
export class DelegatingRecipe extends ScanningRecipe<number> {
    readonly name: string;
    readonly displayName: string;
    readonly description: string;
    private delegate?: {rpc: RewriteRpc, recipe: Promise<RpcRecipe>};

    constructor(readonly javaRecipeName: string,
                readonly delegatesToOptions: Record<string, any>) {
        super();
        this.name = javaRecipeName;
        this.displayName = javaRecipeName;
        this.description = `Delegates to the Java recipe \`${javaRecipeName}\`.`;
    }

    // Local, with an empty recipe list: the host fills the subtree in, and recipeList() prepares the delegate.
    async descriptor(): Promise<RecipeDescriptor> {
        return {
            name: this.name,
            displayName: this.displayName,
            instanceName: this.instanceName(),
            description: this.description,
            tags: this.tags,
            estimatedEffortPerOccurrence: this.estimatedEffortPerOccurrence,
            options: Object.entries(this.delegatesToOptions).map(([name, value]) => ({
                name,
                value,
                displayName: name,
                description: "",
                required: false
            })),
            preconditions: [],
            recipeList: [],
            dataTables: this.dataTables,
            maintainers: [],
            contributors: [],
            examples: []
        };
    }

    async recipeList(): Promise<Recipe[]> {
        return (await this.prepared()).recipeList();
    }

    initialValue(_ctx: ExecutionContext): number {
        return 0;
    }

    async editorWithData(acc: number): Promise<TreeVisitor<any, ExecutionContext>> {
        return (await this.prepared()).editorWithData(acc);
    }

    async scanner(acc: number): Promise<TreeVisitor<any, ExecutionContext>> {
        return (await this.prepared()).scanner(acc);
    }

    async generate(acc: number, ctx: ExecutionContext): Promise<SourceFile[]> {
        return (await this.prepared()).generate(acc, ctx);
    }

    async onComplete(ctx: ExecutionContext): Promise<void> {
        if (this.delegate) {
            await (await this.delegate.recipe).onComplete(ctx);
        }
    }

    private prepared(): Promise<RpcRecipe> {
        const rpc = RewriteRpc.get();
        if (!rpc) {
            throw new Error(
                `Cannot run Java recipe "${this.javaRecipeName}": no active RewriteRpc connection.\n` +
                "  • Tests: spawn one via JavaRpcTestServer.start() — see " +
                "@openrewrite/rewrite/test/java-rpc.\n" +
                "  • Production: the Java host provides one automatically when it " +
                "loads the TS recipe artifact."
            );
        }
        // Prepared once per connection; a failed prepare is dropped so the next use retries.
        if (!this.delegate || this.delegate.rpc !== rpc) {
            const recipe = rpc.prepareRecipe(this.javaRecipeName, this.delegatesToOptions);
            this.delegate = {rpc, recipe};
            recipe.catch(() => {
                if (this.delegate?.recipe === recipe) {
                    this.delegate = undefined;
                }
            });
        }
        return this.delegate.recipe;
    }
}

/**
 * Reference a Java recipe by name from a TS recipe:
 *
 * ```ts
 * export function addDependency(options: AddDependencyOptions): Promise<Recipe> {
 *     return prepareJavaRecipe("org.openrewrite.javascript.AddDependency", options);
 * }
 * ```
 *
 * There is no in-process fallback by design: a Java recipe's editor is the single source of
 * truth. For preconditions that need a graceful no-RPC fallback, use the {@link RecipeRef}
 * pattern via `usesType` / `usesMethod`, which carries a `localVisitor`.
 */
export async function prepareJavaRecipe(
    id: string,
    options?: Record<string, any>,
): Promise<DelegatingRecipe> {
    return new DelegatingRecipe(id, options ?? {});
}
