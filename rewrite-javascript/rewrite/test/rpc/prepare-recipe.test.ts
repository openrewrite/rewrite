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
import {ExecutionContext, ScanningRecipe, TreeVisitor} from "../../src";
import {check} from "../../src/preconditions";
import {hasSourcePath} from "../../src/javascript/preconditions";
import {JsonVisitor} from "../../src/json";
import {PrepareRecipe} from "../../src/rpc/request/prepare-recipe";

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

describe("PrepareRecipe", () => {
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
        expect(recipe.scannedAccumulators).toEqual([undefined, acc]);
        expect(recipe.scannedAccumulators[1]).toBe(acc);
        expect(preconditions).toEqual([
            {visitorName: "org.openrewrite.FindSourceFiles", visitorOptions: {filePattern: "**/package.json"}},
            {visitorName: "org.openrewrite.rpc.internal.FindTreesOfType", visitorOptions: {type: "org.openrewrite.json.tree.Json"}}
        ]);
    });
});
