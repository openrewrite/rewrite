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
import {ExecutionContext, Recipe, ScanningRecipe, TreeVisitor} from "../../src";
import {check} from "../../src/preconditions";
import {hasSourcePath} from "../../src/javascript/preconditions";
import {JsonVisitor} from "../../src/json";
import {PrepareRecipe} from "../../src/rpc/request/prepare-recipe";
import {RpcRecipe} from "../../src/rpc/recipe";

class ScanningRecipeWithPrecondition extends ScanningRecipe<Map<string, string>> {
    name = "org.openrewrite.example.scanner-with-precondition";
    displayName = "Scanner with precondition";
    description = "Gates its scanner with a serializable precondition.";

    readonly scannedAccumulators: Map<string, string>[] = [];
    readonly scanVisitor = new JsonVisitor<ExecutionContext>();

    initialValue(): Map<string, string> {
        return new Map();
    }

    async scanner(acc: Map<string, string>): Promise<TreeVisitor<any, ExecutionContext>> {
        this.scannedAccumulators.push(acc);
        return check(hasSourcePath("**/package.json"), this.scanVisitor);
    }
}

class RecipeWithPrecondition extends Recipe {
    name = "org.openrewrite.example.editor-with-precondition";
    displayName = "Editor with precondition";
    description = "Gates its editor with a serializable precondition.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return check(hasSourcePath("**/package.json"), new JsonVisitor<ExecutionContext>());
    }
}

describe("PrepareRecipe", () => {
    test("an editor gated by a serializable precondition is built again for each visit", async () => {
        // given
        const prepared = await PrepareRecipe["optimizePreconditions"](new RecipeWithPrecondition(), "edit", []);

        // when
        const first = await prepared.editor();
        const second = await prepared.editor();

        // then
        expect(first).toBeInstanceOf(JsonVisitor);
        expect(second).not.toBe(first);
    });

    test("a scanner gated by a serializable precondition reaches the original scanner", async () => {
        // given
        const recipe = new ScanningRecipeWithPrecondition();
        const preconditions: any[] = [];
        const prepared = await PrepareRecipe["optimizePreconditions"](recipe, "scan", preconditions) as ScanningRecipeWithPrecondition;
        const acc = new Map([["key", "value"]]);

        // when
        const scanner = await prepared.scanner(acc);

        // then
        expect(scanner).toBe(recipe.scanVisitor);
        expect(recipe.scannedAccumulators).toEqual([new Map(), acc]);
        expect(recipe.scannedAccumulators[1]).toBe(acc);
        expect(preconditions).toEqual([
            {visitorName: "org.openrewrite.FindSourceFiles", visitorOptions: {filePattern: "**/package.json"}},
            {visitorName: "org.openrewrite.rpc.internal.FindTreesOfType", visitorOptions: {type: "org.openrewrite.json.tree.Json"}}
        ]);
    });

    test("a scanner gated by a recipe of the host sends that recipe's editor as its precondition", async () => {
        // given
        const hostRecipe = new RpcRecipe(undefined as any, "host-recipe", {tags: []} as any, "edit:host-recipe");
        const recipe = new class extends ScanningRecipeWithPrecondition {
            async scanner(): Promise<TreeVisitor<any, ExecutionContext>> {
                return check(hostRecipe, this.scanVisitor);
            }
        };
        const preconditions: any[] = [];

        // when
        await PrepareRecipe["optimizePreconditions"](recipe, "scan", preconditions);

        // then
        expect(preconditions[0]).toEqual({visitorName: "edit:host-recipe"});
    });

    test("a scanner that cannot be built outside a run keeps its precondition", async () => {
        // given
        const recipe = new class extends ScanningRecipeWithPrecondition {
            initialValue(): Map<string, string> {
                throw new Error("only available during a run");
            }
        };
        const preconditions: any[] = [];

        // when
        const prepared = await PrepareRecipe["optimizePreconditions"](recipe, "scan", preconditions);

        // then
        expect(prepared).toBe(recipe);
        expect(preconditions).toEqual([]);
    });
});
