import {afterEach, beforeEach, describe, expect, test} from "@jest/globals";
import {
    createExecutionContext,
    Cursor,
    ExecutionContext, Option,
    Recipe, Registered,
    rootCursor,
    TreeVisitor
} from "../../../main/javascript";
import {RewriteRpc} from "../../../main/javascript/rpc";
import {PlainText, PlainTextVisitor, text} from "../../../main/javascript/text";
import {RecipeSpec, SourceSpec} from "../../../main/javascript/test";
import {PassThrough} from "node:stream";
import * as rpc from "vscode-jsonrpc/node";

describe("RewriteRpcTest", () => {
    class ChangeTextVisitor<P> extends PlainTextVisitor<P> {
        text: string = "Hello world!";

        visitText(text: PlainText, p: P): Promise<PlainText> {
            return this.produceTree(text, p, draft => {
                draft.text = "Hello World!";
            })
        }
    }

    @Registered("org.openrewrite.text.change-text")
    class ChangeText extends Recipe {
        displayName = "Change text";
        description = "Change the text of a file.";

        @Option({
            displayName: "Text",
            description: "Text to change to"
        })
        text: string = "Hello World!"

        get editor(): TreeVisitor<any, ExecutionContext> {
            let visitor = new ChangeTextVisitor<ExecutionContext>();
            visitor.text = this.text;
            return visitor
        }
    }

    @Registered("org.openrewrite.text.with-recipe-list")
    class RecipeWithRecipeList extends Recipe {
        displayName = "A recipe that has a recipe list";
        description = "To verify that it is possible for a recipe list to be called over RPC.";
        recipeList = [new ChangeText()]
    }

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
        ), 1).listen();

        server = new RewriteRpc(rpc.createMessageConnection(
            new rpc.StreamMessageReader(clientToServer),
            new rpc.StreamMessageWriter(serverToClient)
        )).listen();
    });

    afterEach(() => {
        server.end();
        client.end();
    });

    test("sendReceiveExecutionContext", async () => {
        const ctx = createExecutionContext();
        ctx.set("key", "value");

        client.localObjects.set("123", ctx);
        const received = await server.getObject<ExecutionContext>("123");
        expect(received.get("key")).toEqual("value");
    });

    test("print", () => {
        spec.rewriteRun(
            text(
                "Hello Jon!",
                (spec: SourceSpec<PlainText>) => {
                    spec.beforeRecipe = (text: PlainText) => {
                        expect(server.print(text)).toEqual("Hello Jon!");
                        return text;
                    }
                }
            )
        );
    });

    test("getRecipes", async () => {
        expect(await server.recipes()).not.toEqual([]);
    });


    test("prepareRecipe", async () => {
        const recipe = await server.prepareRecipe("org.openrewrite.text.change-text", {text: "hello"});
        expect(recipe.displayName).toEqual("Change text");
    });

    // test("runRecipe", () => {
    //     const latch = new CountDownLatch(1);
    //     spec.rewriteRun(
    //         spec => spec
    //             .recipe(server.prepareRecipe("org.openrewrite.text.Find", {find: "hello"}))
    //             .validateRecipeSerialization(false)
    //             .dataTable(TextMatches.Row, rows => {
    //                 expect(rows).toContainEqual(new TextMatches.Row("hello.txt", "~~>Hello Jon!"));
    //                 latch.countDown();
    //             }),
    //         text(
    //             "Hello Jon!",
    //             "~~>Hello Jon!",
    //             spec => spec.path("hello.txt")
    //         )
    //     );
    //
    //     expect(latch.getCount()).toEqual(0);
    // });
    //
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
    //
    // test("runRecipeWithRecipeList", () => {
    //     spec.recipe = server.prepareRecipe("org.openrewrite.rpc.RewriteRpcTest$RecipeWithRecipeList", {});
    //     spec.rewriteRun(
    //         text(
    //             "hi",
    //             "hello"
    //         )
    //     );
    // });

    test("getCursor", async () => {
        const parent = rootCursor();
        const c1 = new Cursor(0, parent);
        const c2 = new Cursor(1, c1);

        const clientC2 = await client.getCursor(server.getCursorIds(c2));
        expect(clientC2.value).toEqual(1);
        expect(clientC2.parent!.value).toEqual(0);
        expect(clientC2.parent!.value).toEqual(parent);
    });
});
