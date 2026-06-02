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
import {ExecutionContext, Recipe, TreeVisitor} from "@openrewrite/rewrite";
import {PlainText, PlainTextVisitor} from "@openrewrite/rewrite/text";

/**
 * Simulates a recipe from "another module" that is NOT registered in the marketplace.
 * This would be the case when module A depends on module B (npm dependency) and
 * uses module B's recipes in its recipeList(), but module B's activate() was never called.
 */
export class UnregisteredRecipe extends Recipe {
    name = "org.openrewrite.example.text.unregistered"
    displayName = "Unregistered recipe";
    description = "A recipe that is not registered in the marketplace.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends PlainTextVisitor<ExecutionContext> {
            protected override async visitText(text: PlainText, ctx: ExecutionContext): Promise<PlainText | undefined> {
                return this.produceTree(text, ctx, draft => {
                    draft.text = "cross-module";
                })
            }
        }
    }
}

/**
 * A recipe that IS registered in the marketplace, and delegates to
 * UnregisteredRecipe (from "another module") via recipeList().
 */
export class CrossModuleRecipeList extends Recipe {
    name = "org.openrewrite.example.text.cross-module-recipe-list"
    displayName = "Cross-module recipe list";
    description = "A recipe that uses a sub-recipe from another module via recipeList().";

    async recipeList() {
        return [new UnregisteredRecipe()];
    }
}
