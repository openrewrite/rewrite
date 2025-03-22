import {describe, beforeEach, afterEach, test, expect} from "@jest/globals";
import {Recipe, rootCursor} from "../../../main/javascript";
import {RewriteRpc} from "../../../main/javascript/rpc";

class ChangeText extends PlainTextVisitor<number> {
    visitText(text: PlainText, p: number): PlainText {
        return text.withText("Hello World!");
    }
}

class RecipeWithRecipeList extends Recipe {
    getDisplayName(): string {
        return "A recipe that has a recipe list";
    }

    getDescription(): string {
        return "To verify that it is possible for a recipe list to be called over RPC.";
    }

    buildRecipeList(recipes: RecipeList): void {
        recipes.recipe(new org.openrewrite.text.ChangeText("hello"));
    }
}

describe("RewriteRpcTest", () => {
    let server: RewriteRpc;
    let client: RewriteRpc;

    beforeEach(() => {
        const serverOut = new PipedOutputStream();
        const clientOut = new PipedOutputStream();
        const serverIn = new PipedInputStream(clientOut);
        const clientIn = new PipedInputStream(serverOut);

        const serverJsonRpc = new JsonRpc(new TraceMessageHandler("server",
            new HeaderDelimitedMessageHandler(serverIn, serverOut)));
        server = new RewriteRpc(serverJsonRpc, env).batchSize(1).timeout(Duration.ofMinutes(10));

        const clientJsonRpc = new JsonRpc(new TraceMessageHandler("client",
            new HeaderDelimitedMessageHandler(clientIn, clientOut)));
        client = new RewriteRpc(clientJsonRpc, env).batchSize(1).timeout(Duration.ofMinutes(10));
    });

    afterEach(() => {
        server.shutdown();
        client.shutdown();
    });

    test("sendReceiveExecutionContext", () => {
        const ctx = new InMemoryExecutionContext();
        ctx.putMessage("key", "value");

        client.localObjects.set("123", ctx);
        const received = server.getObject("123");
        expect(received.getMessage("key")).toEqual("value");
    });

    test("sendReceiveIdempotence", () => {
        rewriteRun(
            spec => spec.recipe(toRecipe(() => new TreeVisitor<>()
        {
            visit(tree
        :
            Tree, ctx
        :
            ExecutionContext
        ):
            Tree
            {
                const t = server.visit(tree as SourceFile, ChangeText.name, 0);
                this.stopAfterPreVisit();
                return t;
            }
        }
    )),
        text(
            "Hello Jon!",
            "Hello World!"
        )
    )
        ;
    });

    test("print", () => {
        rewriteRun(
            text(
                "Hello Jon!",
                spec => spec.beforeRecipe(text => {
                    expect(server.print(text)).toEqual("Hello Jon!");
                })
            )
        );
    });

    test("getRecipes", () => {
        expect(server.getRecipes()).not.toBeEmpty();
    });

    test("prepareRecipe", () => {
        const recipe = server.prepareRecipe("org.openrewrite.text.Find", {find: "hello"});
        expect(recipe.getDescriptor().getDisplayName()).toEqual("Find text");
    });

    test("runRecipe", () => {
        const latch = new CountDownLatch(1);
        rewriteRun(
            spec => spec
                .recipe(server.prepareRecipe("org.openrewrite.text.Find", {find: "hello"}))
                .validateRecipeSerialization(false)
                .dataTable(TextMatches.Row, rows => {
                    expect(rows).toContainEqual(new TextMatches.Row("hello.txt", "~~>Hello Jon!"));
                    latch.countDown();
                }),
            text(
                "Hello Jon!",
                "~~>Hello Jon!",
                spec => spec.path("hello.txt")
            )
        );

        expect(latch.getCount()).toEqual(0);
    });

    test("runScanningRecipeThatGenerates", () => {
        rewriteRun(
            spec => spec
                .recipe(server.prepareRecipe("org.openrewrite.text.CreateTextFile", {
                    fileContents: "hello",
                    relativeFileName: "hello.txt"
                }))
                .validateRecipeSerialization(false),
            text(
                null,
                "hello",
                spec => spec.path("hello.txt")
            )
        );
    });

    test("runRecipeWithRecipeList", () => {
        rewriteRun(
            spec => spec
                .recipe(server.prepareRecipe("org.openrewrite.rpc.RewriteRpcTest$RecipeWithRecipeList", {}))
                .validateRecipeSerialization(false),
            text(
                "hi",
                "hello"
            )
        );
    });

    test("getCursor", () => {
        const parent = rootCursor();
        const c1 = new Cursor(parent, 0);
        const c2 = new Cursor(c1, 1);

        const clientC2 = client.getCursor(server.getCursorIds(c2));
        expect(clientC2.getValue()).toEqual(1);
        expect(clientC2.getParentOrThrow().getValue()).toEqual(0);
        expect(clientC2.getParentOrThrow(2).getValue()).toEqual(Cursor.ROOT_VALUE);
    });
});
