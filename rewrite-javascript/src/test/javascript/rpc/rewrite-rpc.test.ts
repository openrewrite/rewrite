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
import {Cursor, rootCursor} from "../../../main/javascript";
import {RewriteRpc} from "../../../main/javascript/rpc";
import {PlainText, text} from "../../../main/javascript/text";
import {RecipeSpec} from "../../../main/javascript/test";
import {PassThrough} from "node:stream";
import * as rpc from "vscode-jsonrpc/node";
import "../example-recipe";

describe("RewriteRpcTest", () => {
    const spec = new RecipeSpec();

    let server: RewriteRpc;
    let client: RewriteRpc;

    const logger: rpc.Logger = {
        error: (msg: string) => console.log(`[Error] ${msg}\n`),
        warn: (msg: string) => console.log(`[Warn] ${msg}\n`),
        info: (msg: string) => console.log(`[Info] ${msg}\n`),
        log: (msg: string) => console.log(`[Log] ${msg}\n`)
    };

    beforeEach(async () => {
        // Create in-memory streams to simulate the pipes.
        const clientToServer = new PassThrough();
        clientToServer.on('data', chunk => {
            console.debug('[server] ⇦ received:', `'${chunk.toString()}'`);
            console.debug(`[server] ⇦ received: [${Array.from(chunk).join(", ")}]`);
        });

        const serverToClient = new PassThrough();
        serverToClient.on('data', chunk => {
            // console.debug('[client] ⇦ received:', `'${chunk.toString()}'`);
        });

        const clientConnection = rpc.createMessageConnection(
            new rpc.StreamMessageReader(serverToClient),
            new rpc.StreamMessageWriter(clientToServer),
            logger
        );
        await clientConnection.trace(rpc.Trace.Verbose, logger);
        client = new RewriteRpc(clientConnection, 1);

        const serverConnection = rpc.createMessageConnection(
            new rpc.StreamMessageReader(clientToServer),
            new rpc.StreamMessageWriter(serverToClient),
            logger
        );
        server = new RewriteRpc(serverConnection);
    });

    afterEach(() => {
        server.end();
        client.end();
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

    test("getRecipes", async () =>
        expect((await client.recipes()).length).toBeGreaterThan(0)
    );

    test("prepareRecipe", async () => {
        const recipe = await client.prepareRecipe("org.openrewrite.text.change-text", {text: "hello"});
        expect(recipe.displayName).toEqual("Change text");
        expect(recipe.instanceName()).toEqual("Change text to 'hello'");
    });

    test("runRecipe", async () => {
        spec.recipe = await client.prepareRecipe("org.openrewrite.text.change-text", {text: "hello"});
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

    test("runScanningRecipeThatGenerates", async () => {
        spec.recipe = await client.prepareRecipe("org.openrewrite.text.create-text", {
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

    test("runRecipeWithRecipeList", async () => {
        spec.recipe = await client.prepareRecipe("org.openrewrite.text.with-recipe-list");
        await spec.rewriteRun(
            text(
                "hi",
                "hello"
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
