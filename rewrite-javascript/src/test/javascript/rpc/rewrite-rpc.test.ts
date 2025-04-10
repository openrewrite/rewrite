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

    beforeEach(() => {
        // Create in-memory streams to simulate the pipes.
        const clientToServer = new PassThrough();
        const serverToClient = new PassThrough();

        client = new RewriteRpc(rpc.createMessageConnection(
            new rpc.StreamMessageReader(serverToClient),
            new rpc.StreamMessageWriter(clientToServer)
        ), 1);

        server = new RewriteRpc(rpc.createMessageConnection(
            new rpc.StreamMessageReader(clientToServer),
            new rpc.StreamMessageWriter(serverToClient)
        ));
    });

    afterEach(() => {
        server.end();
        client.end();
    });

    test("print", () => spec.rewriteRun(
        {
            ...text("Hello Jon!"),
            beforeRecipe: async (text: PlainText) => {
                expect(await server.print(text)).toEqual("Hello Jon!");
                return text;
            }
        }
    ));

    test("getRecipes", async () =>
        expect((await server.recipes()).length).toBeGreaterThan(0)
    );

    test("prepareRecipe", async () => {
        const recipe = await server.prepareRecipe("org.openrewrite.text.change-text", {text: "hello"});
        expect(recipe.displayName).toEqual("Change text");
        expect(recipe.instanceName()).toEqual("Change text to 'hello'");
    });

    test("runRecipe", async () => {
        spec.recipe = await server.prepareRecipe("org.openrewrite.text.change-text", {text: "hello"});
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
        spec.recipe = await server.prepareRecipe("org.openrewrite.text.create-text", {
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
        spec.recipe = await server.prepareRecipe("org.openrewrite.text.with-recipe-list");
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
