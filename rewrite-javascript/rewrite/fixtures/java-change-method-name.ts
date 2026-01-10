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
import {AsyncTreeVisitor, ExecutionContext, isSourceFile, Recipe, SourceFile, Tree} from "@openrewrite/rewrite";
import {RewriteRpc} from "@openrewrite/rewrite/rpc";
import {JS} from "@openrewrite/rewrite/javascript";

/**
 * A JavaScript recipe that delegates to the Java ChangeMethodName recipe.
 * This tests the round-trip: Java -> JS (this recipe) -> Java (ChangeMethodName) -> JS -> Java
 *
 * Note: We use editor() instead of recipeList() because recipeList() would cause
 * the nested recipe to be re-prepared when RpcRecipe.getRecipeList() is called,
 * which would send the request back to JS instead of using the already-prepared Java recipe.
 */
export class JavaChangeMethodName extends Recipe {
    name = "org.openrewrite.example.java.change-method-name";
    displayName = "Change method name (via Java)";
    description = "Delegates to the Java ChangeMethodName recipe to test npm ecosystem recipe invocation.";

    private javaRecipe?: Recipe;

    constructor(private readonly options: {
        methodPattern: string;
        newMethodName: string;
        matchOverrides?: boolean;
        ignoreDefinition?: boolean;
    }) {
        super();
    }

    async editor(): Promise<AsyncTreeVisitor<any, ExecutionContext>> {
        const rpc = RewriteRpc.get();
        if (!rpc) {
            throw new Error("RewriteRpc not available - cannot delegate to Java recipe");
        }

        // Prepare the Java recipe via RPC (this goes to Java and back)
        if (!this.javaRecipe) {
            this.javaRecipe = await rpc.prepareRecipe(
                "org.openrewrite.java.ChangeMethodName",
                this.options
            );
        }

        // Get the Java recipe's editor visitor
        const javaEditor = await this.javaRecipe!.editor();

        // Wrap it to only apply to JavaScript/TypeScript files (not JSON)
        return new class extends AsyncTreeVisitor<Tree, ExecutionContext> {
            async isAcceptable(sourceFile: SourceFile, _ctx: ExecutionContext): Promise<boolean> {
                // Only apply to JS/TS files, not JSON
                return sourceFile.kind === JS.Kind.CompilationUnit;
            }

            protected async preVisit(tree: Tree, ctx: ExecutionContext): Promise<Tree | undefined> {
                if (isSourceFile(tree) && tree.kind !== JS.Kind.CompilationUnit) {
                    return tree;
                }
                // Delegate to the Java recipe's visitor
                return javaEditor.visit(tree, ctx, this.cursor);
            }
        }();
    }
}
