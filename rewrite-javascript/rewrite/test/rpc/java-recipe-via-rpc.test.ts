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
import {RecipeSpec} from "../../src/test";
import {text} from "../../src/text";
import {findTestClasspath, JavaRpcTestServer} from "../../src/rpc/java-rpc-client";

// Skip the whole suite when the classpath isn't generated yet (e.g. running tests
// without first invoking `:rewrite-javascript:generateTestClasspath` and without
// REWRITE_JAVASCRIPT_CLASSPATH set). Failing loudly here would be hostile to
// contributors who haven't set up the Java side.
const classpath = findTestClasspath();
const describeRpc = classpath ? describe : describe.skip;

if (!classpath) {
    // eslint-disable-next-line no-console
    console.warn(
        "[java-recipe-via-rpc] Skipping: Java RPC test classpath not configured. " +
        "Run `./gradlew :rewrite-javascript:generateTestClasspath` or set " +
        "REWRITE_JAVASCRIPT_CLASSPATH to enable."
    );
}

describeRpc("Java recipe via RPC", () => {
    let server: JavaRpcTestServer;

    beforeAll(async () => {
        server = await JavaRpcTestServer.start();
    }, 120_000);

    afterAll(async () => {
        if (server) {
            await server.dispose();
        }
    });

    test("prepareRecipe returns a descriptor", async () => {
        const recipe = await server.rpc.prepareRecipe(
            "org.openrewrite.text.FindAndReplace",
            {find: "Hello", replace: "Goodbye"},
        );
        expect(recipe.name).toEqual("org.openrewrite.text.FindAndReplace");
        expect(recipe.displayName).toBeDefined();
        expect(recipe.editVisitor).toBeDefined();
    }, 60_000);

    test("FindAndReplace edits a parsed plain-text source", async () => {
        const spec = new RecipeSpec();
        spec.recipe = await server.rpc.prepareRecipe(
            "org.openrewrite.text.FindAndReplace",
            {find: "Hello", replace: "Goodbye"},
        );
        await spec.rewriteRun(
            {
                ...text("Hello, world!", "Goodbye, world!"),
                path: "greeting.txt",
            },
        );
    }, 60_000);
});
