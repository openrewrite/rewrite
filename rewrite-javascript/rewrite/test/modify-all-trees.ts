import {ExecutionContext, Marker, randomId, Recipe, RecipeRegistry, Tree, TreeVisitor} from "../src";

// noinspection JSUnusedGlobalSymbols
export function activate(registry: RecipeRegistry) {
    registry.register(ModifyAllTrees);
}

const changed: Marker = {
    kind: "org.openrewrite.java.test.marker.changed",
    id: randomId(),
};

class ModifyAllTrees extends Recipe {
    name: string = "org.openrewrite.java.test.modify-all-trees";
    displayName: string = "Modify all trees";
    description: string = "Add a `Marker` to all trees so that we can test " +
        "that each element is sent back to a remote RPC process.";

    get editor(): TreeVisitor<any, ExecutionContext> {
        return new class extends TreeVisitor<Tree, ExecutionContext> {
            protected async preVisit(tree: Tree, p: ExecutionContext): Promise<Tree> {
                return this.produceTree(tree, p, draft => {
                    draft.markers.markers.push(changed);
                });
            }
        };
    }
}
