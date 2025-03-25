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
}
