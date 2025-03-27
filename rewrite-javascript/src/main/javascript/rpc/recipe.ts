import {Minutes, Recipe, RecipeDescriptor} from "../recipe";
import {RewriteRpc} from "./rewrite-rpc";
import {TreeVisitor} from "../visitor";
import {ExecutionContext} from "../execution";

export class RpcRecipe extends Recipe {
    displayName: string = this.descriptor.displayName;
    description: string = this.descriptor.description;
    tags: string[] = this.descriptor.tags;
    estimatedEffortPerOccurrence: Minutes = this.descriptor.estimatedEffortPerOccurrence;

    constructor(private readonly rpc: RewriteRpc,
                private readonly remoteId: string,
                private readonly _descriptor: RecipeDescriptor,
                private readonly editVisitor: string,
                private readonly scanVisitor?: string) {
        super();
    }

    get descriptor(): RecipeDescriptor {
        return this._descriptor;
    }

    instanceName(): string {
        return this._descriptor.instanceName;
    }

    get editor(): TreeVisitor<any, ExecutionContext> {
        const rpc = this.rpc;
        const editVisitor = this.editVisitor;
        return new class extends TreeVisitor<any, ExecutionContext> {
            protected async preVisit(tree: any, ctx: ExecutionContext): Promise<any> {
                const t = await rpc.visit(tree, editVisitor, ctx);
                this.stopAfterPreVisit();
                return t;
            }
        };
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
            const updated = await this.rpc.getObject(ctx["org.openrewrite.rpc.id"]);
            Object.assign(ctx, updated);
        }
    }
}
