import {afterEach, beforeEach, describe, expect, test} from "@jest/globals";
import {
    createExecutionContext,
    Cursor,
    ExecutionContext,
    isExecutionContext,
    rootCursor
} from "../../../main/javascript";
import {RewriteRpc} from "../../../main/javascript/rpc";
import {PlainText, text} from "../../../main/javascript/text";
import {RecipeSpec, SourceSpec} from "../../../main/javascript/test";
import {PassThrough} from "node:stream";
import * as rpc from "vscode-jsonrpc/node";
import "../example-recipe";
import {ReplacedText} from "../example-recipe";

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

    test("sendReceiveExecutionContext", async () => {
        const ctx = createExecutionContext();
        ctx["key"] = "value";
        client.localObjects.set("123", ctx);
        const received = await server.getObject<ExecutionContext>("123");
        expect(received["key"]).toEqual("value");
        expect(isExecutionContext(received));
    });

    test("print", () => spec.rewriteRun(
        text(
            "Hello Jon!",
            (spec: SourceSpec<PlainText>) => {
                spec.beforeRecipe = async (text: PlainText) => {
                    expect(await server.print(text)).toEqual("Hello Jon!");
                    return text;
                }
            }
        )
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
        spec.dataTable("org.openrewrite.text.replaced-text", (rows: ReplacedText[]) => {
            // Prove that data tables can flow back over the RPC connection to the controlling
            // processes' ExecutionContext.
            expect(rows).toContain(new ReplacedText("hello.txt", "hello"));
        });
        await spec.rewriteRun(
            text(
                "Hello Jon!",
                "hello",
                spec => {
                    spec.path = "hello.txt";
                }
            )
        );
    });

    // test("runScanningRecipeThatGenerates", () => {
    //     spec.rewriteRun(
    //         spec => spec
    //             .recipe(server.prepareRecipe("org.openrewrite.text.CreateTextFile", {
    //                 fileContents: "hello",
    //                 relativeFileName: "hello.txt"
    //             }))
    //             .validateRecipeSerialization(false),
    //         text(
    //             null,
    //             "hello",
    //             spec => spec.path("hello.txt")
    //         )
    //     );
    // });

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
