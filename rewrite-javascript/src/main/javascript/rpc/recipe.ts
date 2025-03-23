import {Minutes, Recipe, RecipeDescriptor} from "../recipe";
import {RewriteRpc} from "./rewrite-rpc";
import {TreeVisitor} from "../visitor";
import {ExecutionContext} from "../execution";

export class RpcRecipe extends Recipe {
    private readonly remoteId: string;
    private readonly rpc: RewriteRpc;
    private readonly _descriptor: RecipeDescriptor;
    private readonly editVisitor: string
    private readonly scanVisitor?: string;

    displayName: string = this.descriptor.displayName;
    description: string = this.descriptor.description;
    tags: string[] = this.descriptor.tags;
    estimatedEffortPerOccurrence: Minutes = this.descriptor.estimatedEffortPerOccurrence;

    constructor(rpc: RewriteRpc, remoteId: string, descriptor: RecipeDescriptor,
                editVisitor: string, scanVisitor?: string) {
        super();
        this.rpc = rpc;
        this.remoteId = remoteId;
        this._descriptor = descriptor;
        this.editVisitor = editVisitor;
        this.scanVisitor = scanVisitor;
    }

    get descriptor(): RecipeDescriptor {
        return this._descriptor;
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
