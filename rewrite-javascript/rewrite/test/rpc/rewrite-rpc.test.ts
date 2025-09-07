// noinspection JSUnusedLocalSymbols

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
import {afterEach, beforeEach, describe, expect, test} from "@jest/globals";
import {Cursor, RecipeRegistry, rootCursor} from "@openrewrite/rewrite";
import {RewriteRpc} from "@openrewrite/rewrite/rpc";
import {PlainText, text} from "@openrewrite/rewrite/text";
import {json} from "@openrewrite/rewrite/json";
import {RecipeSpec} from "@openrewrite/rewrite/test";
import {PassThrough} from "node:stream";
import * as rpc from "vscode-jsonrpc/node";
import {activate} from "../../fixtures/example-recipe";
import {javascript, JS} from "@openrewrite/rewrite/javascript";
import fs from "node:fs";

describe("Rewrite RPC", () => {
    const spec = new RecipeSpec();

    let server: RewriteRpc;
    let client: RewriteRpc;

    beforeEach(async () => {
        // Create in-memory streams to simulate the pipes.
        const clientToServer = new PassThrough();
        const serverToClient = new PassThrough();

        const clientConnection = rpc.createMessageConnection(
            new rpc.StreamMessageReader(serverToClient),
            new rpc.StreamMessageWriter(clientToServer)
        );
        client = new RewriteRpc(clientConnection, {
            batchSize: 1,
            traceGetObjectOutput: true,
            traceGetObjectInput: fs.createWriteStream('client.txt')
        });

        const serverConnection = rpc.createMessageConnection(
            new rpc.StreamMessageReader(clientToServer),
            new rpc.StreamMessageWriter(serverToClient)
        );
        const registry = new RecipeRegistry();
        activate(registry);
        server = new RewriteRpc(serverConnection, {
            registry: registry,
            traceGetObjectOutput: true,
            traceGetObjectInput: fs.createWriteStream('server.txt')
        });
    });

    afterEach(() => {
        server.end();
        client.end();
        fs.unlink('client.txt', () => {
        });
        fs.unlink('server.txt', () => {
        });
    });

    test("print", () => spec.rewriteRun(
        {
            ...text("Hello Jon!"),
            beforeRecipe: async (text: PlainText) => {
                expect(await client.print(text)).toEqual("Hello Jon!");
                return text;
            }
        }
    ));

    test("parse", async () => {
        let sourceFile = (await client.parse([{
            text: "console.info('hello',)",
            sourcePath: "hello.js"
        }]))[0];
        expect(sourceFile.kind).toEqual(JS.Kind.CompilationUnit);
        expect(sourceFile.sourcePath).toEqual("hello.js");
        return sourceFile;
    });

    test("getRecipes", async () =>
        expect((await client.recipes()).length).toBeGreaterThan(0)
    );

    test("prepareRecipe", async () => {
        const recipe = await client.prepareRecipe("org.openrewrite.example.text.change-text", {text: "hello"});
        expect(recipe.displayName).toEqual("Change text");
        expect(recipe.instanceName()).toEqual("Change text to 'hello'");
    });

    test("installRecipes", async () => {
        const installed = await client.installRecipes(
            {packageName: "@openrewrite/recipes-npm"}
        );
        expect(installed.recipesInstalled).toBeGreaterThan(0);
    });

    test("runRecipe", async () => {
        spec.recipe = await client.prepareRecipe("org.openrewrite.example.text.change-text", {text: "hello"});
        await spec.rewriteRun(
            {
                ...text(
                    "Hello Jon!",
                    "hello"
                ),
                path: "hello.txt"
            }
        );
    });

    test("languages", async () => {
        expect(await client.languages()).toContainEqual(JS.Kind.CompilationUnit);
    });

    test("runSearchRecipe", async () => {
        spec.recipe = await client.prepareRecipe("org.openrewrite.example.javascript.find-identifier", {identifier: "hello"});
        await spec.rewriteRun(
            //language=javascript
            javascript(
                "const hello = 'world'",
                "const /*~~>*/hello = 'world'"
            )
        );
    });

    test("run a JSON recipe", async () => {
        spec.recipe = await client.prepareRecipe("org.openrewrite.example.npm.change-version", {version: "1.0.0"});
        await spec.rewriteRun(
            {
                //language=json
                ...json(
                    `
                      {
                        "name": "@openrewrite/rewrite-example",
                        "version": "0"
                      }
                    `,
                    `
                      {
                        "name": "@openrewrite/rewrite-example",
                        "version": "1.0.0"
                      }
                    `
                ),
                path: "package.json"
            }
        );
    });

    test("runScanningRecipeThatGenerates", async () => {
        spec.recipe = await client.prepareRecipe("org.openrewrite.example.text.create-text", {
            text: "hello",
            sourcePath: "hello.txt"
        });
        await spec.rewriteRun(
            {
                ...text(
                    null,
                    "hello"
                ),
                path: "hello.txt"
            }
        );
    });

    test("runRecipeWithPreconditions", async () => {
        spec.recipe = await client.prepareRecipe("org.openrewrite.example.javascript.find-identifier-with-path", {
            identifier: "hello",
            requiredPath: "hello.js"
        });
        await spec.rewriteRun(
            {
                //language=javascript
                ...javascript(
                    "const hello = 'world'",
                    "const /*~~>*/hello = 'world'"
                ),
                path: "hello.js"
            }
        );
    });

    test("runRecipeWithRecipeList", async () => {
        spec.recipe = await client.prepareRecipe("org.openrewrite.example.text.with-recipe-list");
        await spec.rewriteRun(
            text(
                "hi",
                "hello"
            )
        );
    });

    test("runRecipeUpdatingAllTrees", async () => {
        spec.recipe = await client.prepareRecipe("org.openrewrite.example.javascript.replace-id");
        await spec.rewriteRun(
            javascript(
                //language=javascript
                `
                    function foo() {
                    }
                `
            )
        );
    });

    test("getCursor", async () => {
        const parent = rootCursor();
        const c1 = new Cursor({k: 0}, parent);
        const c2 = new Cursor({k: 1}, c1);

        const clientC2 = await client.getCursor(server.getCursorIds(c2));
        expect(clientC2.value).toEqual({k: 1});
        expect(clientC2.parent!.value).toEqual({k: 0});
        expect(clientC2.parent!.parent!.value).toEqual("root");
    });
});
