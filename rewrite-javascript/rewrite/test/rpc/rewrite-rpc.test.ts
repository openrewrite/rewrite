// noinspection JSUnusedLocalSymbols,TypeScriptCheckImport,JSUnusedGlobalSymbols

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
import {Cursor, JavaScript, Recipe, RecipeMarketplace, rootCursor} from "../../src";
import {RewriteRpc} from "../../src/rpc/rewrite-rpc";
import {PlainText, text} from "../../src/text";
import {json, Json} from "../../src/json";
import {RecipeSpec} from "../../src/test";
import {PassThrough} from "node:stream";
import * as rpc from "vscode-jsonrpc/node";
import {activate} from "../../fixtures/example-recipe";
import {activate as activateCompositeWithJavaDelegate} from "../../fixtures/composite-with-java-delegate";
import {activate as activateJavaDelegatePrecondition} from "../../fixtures/java-delegate-precondition";
import {
    findNodeResolutionResult,
    javascript,
    JavaScriptVisitor,
    JS,
    npm,
    packageJson,
    typescript
} from "../../src/javascript";
import {J} from "../../src/java";
import {withDir} from "tmp-promise";
import {PrepareRecipe, PrepareRecipeResponse} from "../../src/rpc/request/prepare-recipe";
import {Print} from "../../src/rpc/request/print";
import {RpcObjectState} from "../../src/rpc/queue";
import * as fs from "fs";
import * as path from "path";

describe("Rewrite RPC", () => {
    const spec = new RecipeSpec();

    let server: RewriteRpc;
    let serverMarketplace: RecipeMarketplace;
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
            batchSize: 1
        });

        const serverConnection = rpc.createMessageConnection(
            new rpc.StreamMessageReader(clientToServer),
            new rpc.StreamMessageWriter(serverToClient)
        );
        serverMarketplace = new RecipeMarketplace();
        await activate(serverMarketplace);
        server = new RewriteRpc(serverConnection, {
            marketplace: serverMarketplace
        });
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

    test("print subtree", () => spec.rewriteRun(
        {
            //language=typescript
            ...typescript("console.log('hello');"),
            beforeRecipe: async (cu: JS.CompilationUnit) =>
                await (new class extends JavaScriptVisitor<any> {
                    protected async visitMethodInvocation(method: J.MethodInvocation, _: any): Promise<J | undefined> {
                        //language=typescript
                        expect(await client.print(method, this.cursor!.parent!))
                            .toEqual("console.log('hello')");
                        return method;
                    }
                }).visit(cu, 0)
        }
    ));

    test("print subtrees whose text depends on what encloses them", () => spec.rewriteRun(
        {
            //language=typescript
            ...typescript("const literal = { a: 1, b: 2 }, cast = <string>literal, first = items?.[0], called = a?.b();"),
            beforeRecipe: async (cu: JS.CompilationUnit) => {
                const printed: string[] = [];
                const withoutCursor: string[] = [];
                await (new class extends JavaScriptVisitor<any> {
                    protected async preVisit(tree: J, _: any): Promise<J | undefined> {
                        const parent = this.cursor.parentTree()?.value?.kind;
                        if (tree.kind === J.Kind.Block && parent === J.Kind.NewClass ||
                            tree.kind === J.Kind.ControlParentheses && parent === J.Kind.TypeCast ||
                            tree.kind === J.Kind.Identifier && (parent === J.Kind.ArrayAccess || parent === J.Kind.MethodInvocation) &&
                            tree.markers.markers.length > 0) {
                            printed.push(await client.print(tree, this.cursor.parent!));
                            client.localObjects.set(tree.id.toString(), tree);
                            withoutCursor.push(await client.connection.sendRequest(
                                new rpc.RequestType<Print, string, Error>("Print"), new Print(tree.id, cu.kind)));
                        }
                        return tree;
                    }
                }).visit(cu, 0);
                expect(printed).toEqual(["{ a: 1, b: 2 }", "<string>", "items?.", "a"]);
                expect(withoutCursor).toEqual(["{ a: 1 b: 2 }", "(string)", "items?", "a?"]);
                return cu;
            }
        }
    ));

    test("parse", async () => {
        const sourceFile = (await client.parse([{
            text: "console.info('hello',)",
            sourcePath: "hello.js"
        }], JS.Kind.CompilationUnit))[0];
        expect(sourceFile.kind).toEqual(JS.Kind.CompilationUnit);
        expect(sourceFile.sourcePath).toEqual("hello.js");
        return sourceFile;
    });

    test("parse an input that names a file without giving its text", async () => {
        await withDir(async dir => {
            fs.writeFileSync(path.join(dir.path, "hello.ts"), "console.info('hello')");
            const sourceFile = (await client.parse(
                [{text: null, sourcePath: path.join(dir.path, "hello.ts")} as any],
                JS.Kind.CompilationUnit, dir.path))[0];
            expect(sourceFile.kind).toEqual(JS.Kind.CompilationUnit);
            expect(sourceFile.sourcePath).toEqual("hello.ts");
            expect(await client.print(sourceFile)).toEqual("console.info('hello')");
        }, {unsafeCleanup: true});
    });

    test("a receive failure surfaces when the peer cannot roll it back, and the refs it sent are kept", async () => {
        const toPeer = new PassThrough();
        const fromPeer = new PassThrough();
        // a peer that predates AbortGetObject answers it with "method not found", and still counts ref 3 as sent
        const peer = rpc.createMessageConnection(new rpc.StreamMessageReader(toPeer), new rpc.StreamMessageWriter(fromPeer));
        peer.onRequest("GetObject", (request: { id: string }) => request.id === "1" ?
            [{state: RpcObjectState.ADD, value: "shared", ref: 3}, {state: RpcObjectState.ADD, ref: 7}, {state: RpcObjectState.END_OF_OBJECT}] :
            [{state: RpcObjectState.ADD, ref: 3}, {state: RpcObjectState.END_OF_OBJECT}]);
        peer.listen();

        const receiver = new RewriteRpc(rpc.createMessageConnection(
            new rpc.StreamMessageReader(fromPeer), new rpc.StreamMessageWriter(toPeer)), {});
        await expect(receiver.getObject("1")).rejects.toThrow("Expected END_OF_OBJECT but got: ADD");
        expect(await receiver.getObject("2")).toEqual("shared");
    });

    test("parse package.json with PackageJsonParser", async () => {
        // Parser type is automatically detected from the file path
        const sourceFile = (await client.parse([{
            text: JSON.stringify({
                name: "test-project",
                version: "1.0.0",
                dependencies: {
                    "lodash": "^4.17.21"
                }
            }, null, 2),
            sourcePath: "package.json"
        }], Json.Kind.Document))[0];
        expect(sourceFile.kind).toEqual(Json.Kind.Document);
        expect(sourceFile.sourcePath).toEqual("package.json");
        // Check that the NodeResolutionResult marker is attached
        const marker = findNodeResolutionResult(sourceFile as Json.Document);
        expect(marker).toBeDefined();
        expect(marker!.name).toEqual("test-project");
        expect(marker!.version).toEqual("1.0.0");
        expect(marker!.dependencies).toHaveLength(1);
        expect(marker!.dependencies[0].name).toEqual("lodash");
    });

    test("getMarketplace", async () =>
        expect((await client.marketplace()).allRecipes().length).toBeGreaterThan(0)
    );

    test("prepareRecipe", async () => {
        const recipe = await client.prepareRecipe("org.openrewrite.example.text.change-text", {text: "hello"});
        expect(recipe.displayName).toEqual("Change text");
        expect(recipe.instanceName()).toEqual("Change text to 'hello'");
    });

    test("prepareRecipe rejects a missing required option", async () => {
        // The server validates required options when preparing a recipe. `text` is required, so
        // omitting it must fail rather than silently preparing a broken recipe.
        await expect(client.prepareRecipe("org.openrewrite.example.text.change-text", {}))
            .rejects.toThrow("Missing required option `text`");
    });

    test("prepareRecipe validates required options of child recipes", async () => {
        // The composite's child ChangeText is missing its required `text`. Validation recurses through
        // the whole prepared tree (like the C# server), so preparing the composite must fail.
        await expect(client.prepareRecipe("org.openrewrite.example.text.composite-with-invalid-child"))
            .rejects.toThrow("Missing required option `text`");
    });

    // TODO: Re-enable once @openrewrite/recipes-npm is updated to use RecipeMarketplace API
    test.skip("installRecipes", async () => {
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

    test("runScanningRecipeThatEdits", async () => {
        // This test verifies that the accumulator from the scanning phase
        // is correctly passed to the editor phase over RPC.
        spec.recipe = await client.prepareRecipe("org.openrewrite.example.text.scanning-editor");
        await spec.rewriteRun(
            text("file1", "file1 (count: 2)"),
            text("file2", "file2 (count: 2)")
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

    test("prepareRecipeWithRpcSubRecipeInRecipeList", async () => {
        // A composite recipe whose recipeList() mixes a local recipe with an
        // already-prepared remote (RpcRecipe) sub-recipe — the shape of a
        // framework-upgrade composite listing a Java recipe prepared over RPC.
        // Preparing it must not try to re-install the RpcRecipe by its
        // (no-arg-incompatible) constructor.
        const recipe = await client.prepareRecipe("org.openrewrite.example.text.with-rpc-sub-recipe");
        const descriptor = await recipe.descriptor();
        expect(descriptor.recipeList.map(r => r.name)).toContain(
            "org.openrewrite.example.text.remote-change-text"
        );
    });

    test("sameTypeChildrenPreserveDistinctOptions", async () => {
        // A composite whose recipeList() yields multiple instances of the same recipe class with
        // different option values must keep each prepared child its own options, rather than
        // collapsing them.
        const recipe = await client.prepareRecipe("org.openrewrite.example.text.same-type-children");
        const descriptor = await recipe.descriptor();

        expect(descriptor.recipeList.map(r => r.name)).toEqual([
            "org.openrewrite.example.text.change-text",
            "org.openrewrite.example.text.change-text",
            "org.openrewrite.example.text.change-text"
        ]);

        const texts = descriptor.recipeList.map(
            r => r.options.find(o => o.name === "text")?.value
        );
        expect(texts).toEqual(["a", "b", "c"]);
    });

    test("preparing an unknown recipe id delegates to the host instead of failing", async () => {
        const response: PrepareRecipeResponse = await (client as any).connection.sendRequest(
            new rpc.RequestType<PrepareRecipe, PrepareRecipeResponse, Error>("PrepareRecipe"),
            new PrepareRecipe("org.openrewrite.javascript.UpgradeDependencyVersion", {newVersion: "19.x"})
        );
        expect(response.delegatesTo).toEqual({
            recipeName: "org.openrewrite.javascript.UpgradeDependencyVersion",
            options: {newVersion: "19.x"}
        });
    });

    test("a composite's Java-delegate children are emitted as delegatesTo with the options as passed", async () => {
        await activateCompositeWithJavaDelegate(serverMarketplace);
        const response: PrepareRecipeResponse = await (client as any).connection.sendRequest(
            new rpc.RequestType<PrepareRecipe, PrepareRecipeResponse, Error>("PrepareRecipe"),
            new PrepareRecipe("org.openrewrite.example.npm.composite-with-java-delegate")
        );
        expect(response.recipeList!.map(child => child.delegatesTo)).toEqual([
            {recipeName: "org.openrewrite.example.host.replace-hello", options: {}},
            {recipeName: "org.openrewrite.text.FindAndReplace", options: {find: "goodbye", replace: "farewell"}}
        ]);
    });

    test("a Java-delegate precondition is sent as a named visitor for the host to gate on", async () => {
        await activateJavaDelegatePrecondition(serverMarketplace);
        const response: PrepareRecipeResponse = await (client as any).connection.sendRequest(
            new rpc.RequestType<PrepareRecipe, PrepareRecipeResponse, Error>("PrepareRecipe"),
            new PrepareRecipe("org.openrewrite.example.npm.find-identifier-gated-by-java-recipe")
        );
        expect(response.editPreconditions).toContainEqual(
            {visitorName: "org.openrewrite.text.Find", visitorOptions: {find: "gate"}}
        );
    });

    // Older Java hosts reject unknown response fields, so causesAnotherCycle is only sent when asked for.
    describe("causesAnotherCycle", () => {
        class CausesAnotherCycle extends Recipe {
            name = "org.openrewrite.example.text.causes-another-cycle"
            displayName = "Causes another cycle"
            description = "Causes another cycle."
            readonly causesAnotherCycle = true
        }

        const prepare = (request: PrepareRecipe): Promise<PrepareRecipeResponse> =>
            (client as any).connection.sendRequest(
                new rpc.RequestType<PrepareRecipe, PrepareRecipeResponse, Error>("PrepareRecipe"), request);

        beforeEach(() => serverMarketplace.install(CausesAnotherCycle, JavaScript));

        test("omitted for hosts that did not ask for it", async () => {
            const response = await prepare(new PrepareRecipe("org.openrewrite.example.text.causes-another-cycle"));
            expect(response).not.toHaveProperty("causesAnotherCycle");
        });

        test("omitted when false", async () => {
            const response = await prepare(new PrepareRecipe("org.openrewrite.example.text.with-recipe-list", {}, true));
            expect(response).not.toHaveProperty("causesAnotherCycle");
        });

        test("sent when requested", async () => {
            const response = await prepare(new PrepareRecipe("org.openrewrite.example.text.causes-another-cycle", {}, true));
            expect(response.causesAnotherCycle).toBe(true);
        });
    });

    test("runRecipeWithCrossModuleRecipeList", async () => {
        spec.recipe = await client.prepareRecipe("org.openrewrite.example.text.cross-module-recipe-list");
        await spec.rewriteRun(
            text(
                "hi",
                "cross-module"
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

    test("JavaType.Class codecs across RPC boundaries", async () => {
        await withDir(async repo => {
            spec.recipe = await client.prepareRecipe("org.openrewrite.example.javascript.mark-class-types");

            //language=typescript
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(
                        `
                            import _ from 'lodash';

                            const result = _.map([1, 2, 3], n => n * 2);
                        `,
                        `
                            import /*~~(_.LoDashStatic)~~>*/_ from 'lodash';

                            const result = /*~~(_.LoDashStatic)~~>*/_.map([1, 2, 3], n => n * 2);
                        `
                    ),
                    //language=json
                    packageJson(
                        `
                          {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                              "lodash": "^4.17.21"
                            },
                            "devDependencies": {
                              "@types/lodash": "^4.14.195"
                            }
                          }
                        `
                    )
                )
            );
        }, {unsafeCleanup: true});
    });
});
