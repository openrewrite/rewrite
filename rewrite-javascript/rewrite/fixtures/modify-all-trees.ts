// noinspection JSUnusedGlobalSymbols

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
import {
    ExecutionContext,
    marker,
    Marker,
    randomId,
    Recipe,
    RecipeRegistry,
    Tree,
    TreeVisitor
} from "@openrewrite/rewrite";

export function activate(registry: RecipeRegistry) {
    registry.register(ModifyAllTrees);
}

const changed: Marker = marker(randomId());

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
