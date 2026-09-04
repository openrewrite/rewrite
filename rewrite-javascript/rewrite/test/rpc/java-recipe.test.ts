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
import {prepareJavaRecipe} from "../../src/rpc";

describe("prepareJavaRecipe", () => {
    // Test files share a worker, so unset any connection a neighbour registered.
    const GLOBAL_KEY = Symbol.for("org.openrewrite.rpc.RewriteRpc.global");
    let saved: any;
    beforeEach(() => {
        saved = (globalThis as any)[GLOBAL_KEY];
        delete (globalThis as any)[GLOBAL_KEY];
    });
    afterEach(() => {
        (globalThis as any)[GLOBAL_KEY] = saved;
    });

    test("builds a placeholder carrying the options without an active connection", async () => {
        const recipe = await prepareJavaRecipe("org.openrewrite.text.FindAndReplace", {find: "Hello", replace: "Goodbye"});
        expect(recipe.name).toEqual("org.openrewrite.text.FindAndReplace");
        const descriptor = await recipe.descriptor();
        expect(descriptor.options.map(o => [o.name, o.value])).toEqual([["find", "Hello"], ["replace", "Goodbye"]]);
        expect(descriptor.recipeList).toEqual([]);
    });
});
