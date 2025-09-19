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
import {Minutes, Recipe, RecipeDescriptor, ScanningRecipe} from "../recipe";
import {RewriteRpc} from "./rewrite-rpc";
import {noopVisitor, TreeVisitor} from "../visitor";
import {ExecutionContext} from "../execution";
import {SourceFile, Tree} from "../tree";

export class RpcRecipe extends ScanningRecipe<number> {
    name: string = this._descriptor.name;
    displayName: string = this._descriptor.displayName;
    description: string = this._descriptor.description;
    tags: string[] = this._descriptor.tags;
    estimatedEffortPerOccurrence: Minutes = this._descriptor.estimatedEffortPerOccurrence;

    constructor(private readonly rpc: RewriteRpc,
                private readonly remoteId: string,
                private readonly _descriptor: RecipeDescriptor,
                readonly editVisitor: string,
                readonly scanVisitor?: string) {
        super();
    }

    async descriptor(): Promise<RecipeDescriptor> {
        return this._descriptor;
    }

    instanceName(): string {
        return this._descriptor.instanceName;
    }

    initialValue(_ctx: ExecutionContext) {
        return 0
    }

    async editorWithData(_acc: number): Promise<TreeVisitor<any, ExecutionContext>> {
        return this.editVisitor ? new RpcVisitor(this.rpc, this.editVisitor) : noopVisitor();
    }

    async scanner(_acc: number): Promise<TreeVisitor<any, ExecutionContext>> {
        return this.scanVisitor ? new RpcVisitor(this.rpc, this.scanVisitor) : noopVisitor();
    }

    async generate(_acc: number, ctx: ExecutionContext): Promise<SourceFile[]> {
        return this.rpc.generate(this.remoteId, ctx);
    }

    async recipeList(): Promise<Recipe[]> {
        const recipeList: Recipe[] = [];
        for (const r of this._descriptor.recipeList) {
            const opts = Object.fromEntries(r.options.map(opt => [opt.name, opt.value]));
            recipeList.push(await this.rpc.prepareRecipe(r.name, opts));
        }
        return recipeList;
    }

    async onComplete(ctx: ExecutionContext): Promise<void> {
        // This will merge data tables from the remote into the local context.
        //
        // When multiple recipes ran on the same RPC peer, they will all have been
        // adding to the same ExecutionContext instance on that peer, and so really
        // a CHANGE will only be returned for the first of any recipes on that peer.
        // It doesn't matter which one added data table entries, because they all share
        // the same view of the data tables.
        if ("org.openrewrite.rpc.id" in ctx) {
            const updated = await this.rpc.getObject(ctx.messages["org.openrewrite.rpc.id"]);
            Object.assign(ctx, updated);
        }
    }
}

export class RpcVisitor extends TreeVisitor<Tree, ExecutionContext> {
    constructor(
        private readonly rpc: RewriteRpc,
        private readonly visitorName: string
    ) {
        super();
    }

    async isAcceptable(sourceFile: SourceFile, _: ExecutionContext): Promise<boolean> {
        return (await this.rpc.languages()).includes(sourceFile.kind);
    }

    protected async preVisit(tree: Tree, ctx: ExecutionContext): Promise<Tree | undefined> {
        this.stopAfterPreVisit();
        return this.rpc.visit(tree as SourceFile, this.visitorName, ctx);
    }
}
