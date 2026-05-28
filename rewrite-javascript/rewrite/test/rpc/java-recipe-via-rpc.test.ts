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
import {describeJavaRpc, testJavaRpc} from "../../src/test/java-rpc";
import {RecipeSpec} from "../../src/test";
import {text} from "../../src/text";

describeJavaRpc("Java recipe via RPC", () => {
    testJavaRpc("prepareRecipe returns a descriptor", async ({javaRpc}) => {
        const recipe = await javaRpc.rpc.prepareRecipe(
            "org.openrewrite.text.FindAndReplace",
            {find: "Hello", replace: "Goodbye"},
        );
        expect(recipe.name).toEqual("org.openrewrite.text.FindAndReplace");
        expect(recipe.displayName).toBeDefined();
        expect(recipe.editVisitor).toBeDefined();
    });

    testJavaRpc("FindAndReplace edits a parsed plain-text source", async ({javaRpc}) => {
        const spec = new RecipeSpec();
        spec.recipe = await javaRpc.rpc.prepareRecipe(
            "org.openrewrite.text.FindAndReplace",
            {find: "Hello", replace: "Goodbye"},
        );
        await spec.rewriteRun(
            {
                ...text("Hello, world!", "Goodbye, world!"),
                path: "greeting.txt",
            },
        );
    });
});
