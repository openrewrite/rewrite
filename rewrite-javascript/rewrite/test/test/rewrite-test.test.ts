import {fromVisitor, RecipeSpec} from "../../src/test";
import {text} from "../../src/text";
import {json} from "../../src/json";
import {ChangeText} from "../../fixtures/change-text";
import {ExecutionContext, Recipe, ScanningRecipe, TreeVisitor} from "../../src";
import {foundSearchResult, Marker, Markers, MarkersKind} from "../../src/markers";
import {PlainText, PlainTextVisitor} from "../../src/text";
import {randomId} from "../../src/uuid";
import {J} from "../../src/java";
import {JavaScriptVisitor, JS, typescript} from "../../src/javascript";

describe("rewrite test", () => {
    test("a recipe that makes no changes", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new ChangeText({text: "test"});
        await spec.rewriteRun(
            text("test")
        )
    });

    test("beforeRecipe", async () => {
        const sut = new RecipeSpec();
        let count = 0;
        await sut.rewriteRun(
            {
                ...text("test"),
                beforeRecipe: () => {
                    count++;
                }
            }
        )
        expect(count).toEqual(1)
    });

    test("customize the path of a source spec", () => new RecipeSpec().rewriteRun(
        {
            ...json(
                `{"type": "object"}`,
            ),
            path: "source.json",
            beforeRecipe: actual => {
                expect(actual.sourcePath).toEqual("source.json");
            }
        }
    ));

    test("beforeRecipe can modify AST without breaking idempotence check", async () => {
        // given
        const sut = new RecipeSpec();
        const markerFound: boolean[] = [];

        // when
        await sut.rewriteRun(
            {
                ...text("hello"),
                beforeRecipe: (sourceFile: PlainText) => {
                    return foundSearchResult(sourceFile, "test");
                },
                afterRecipe: (sourceFile: PlainText) => {
                    const marker = sourceFile.markers.markers
                        .find(m => m.kind === MarkersKind.SearchResult);
                    markerFound.push(marker !== undefined);
                }
            }
        );

        // then
        expect(markerFound).toEqual([true]);
    });

    test("two kinds of sources and a scanning recipe", async () => {
        // given
        const sut = new RecipeSpec();
        let countOfAccumulators: number = 0;
        sut.recipe = new class extends ScanningRecipe<{}> {
            name = "ad-hoc";
            displayName = "ad-hoc";
            description = "ad-hoc";

            initialValue(ctx: ExecutionContext): {} {
                countOfAccumulators++;
                return {};
            }
        };

        // when
        await sut.rewriteRun(
            json('{"A": "a"}'),
            text("just a regular text"));

        // test
        expect(countOfAccumulators).toBe(1);
    });

    describe("empty diff detection", () => {
        // A recipe that changes the AST (replaces the id) without affecting printed output
        class GhostChangeRecipe extends Recipe {
            name = "org.openrewrite.test.ghost-change";
            displayName = "Ghost change";
            description = "Changes the AST without changing the printed output.";

            async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
                return new class extends PlainTextVisitor<ExecutionContext> {
                    protected override async visitText(text: PlainText, ctx: ExecutionContext): Promise<PlainText | undefined> {
                        return {...text, id: randomId()};
                    }
                };
            }
        }

        test("raises error on empty diff by default", async () => {
            const sut = new RecipeSpec();
            sut.recipe = new GhostChangeRecipe();
            await expect(sut.rewriteRun(text("hello")))
                .rejects.toThrow("empty diff");
        });

        test("allowEmptyDiff suppresses the error", async () => {
            const sut = new RecipeSpec();
            sut.recipe = new GhostChangeRecipe();
            sut.allowEmptyDiff = true;
            await sut.rewriteRun(text("hello"));
        });
    });

    test("a spec returns to its suite configuration between tests", () => {
        // Independent of whether the harness already switched tracking on
        RecipeSpec.trackSuiteConfiguration();

        const sut = new RecipeSpec();
        const suiteRecipe = sut.recipe;
        sut.allowEmptyDiff = true;
        RecipeSpec.captureSuiteConfiguration();

        sut.recipe = new ChangeText({text: "a test's own recipe"});
        sut.allowEmptyDiff = false;
        sut.checkParsePrintIdempotence = false;
        sut.checkReportingMarkersOnly = false;
        RecipeSpec.restoreSuiteConfiguration();

        expect(sut.recipe).toBe(suiteRecipe);
        expect(sut.allowEmptyDiff).toBe(true);
        expect(sut.checkParsePrintIdempotence).toBe(true);
        expect(sut.checkReportingMarkersOnly).toBe(true);
    });

    describe("added markers", () => {
        function withMarker<T extends { markers: Markers }>(t: T, marker: Marker): T {
            return {...t, markers: {...t.markers, markers: [...t.markers.markers, marker]}};
        }

        // Renames foo() to bar(), and tags what it renamed
        const renameAndTag = () => fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
            protected async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
                const m = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                return withMarker({...m, name: {...m.name, simpleName: "bar"}}, {kind: "my.Visited", id: randomId()});
            }
        });

        test("a marker the recipe adds for itself fails the run", async () => {
            const sut = new RecipeSpec();
            sut.recipe = renameAndTag();
            await expect(sut.rewriteRun({...typescript(`foo()`, `bar()`), path: "src/a.ts"})).rejects.toThrow(
                "The recipe added markers to the LST that neither the sources it was given nor a fresh parse of " +
                "its output contain:\n" +
                "  src/a.ts\n" +
                "    my.Visited on J.MethodInvocation `bar()`\n" +
                "Markers travel with the LST to every recipe that runs after this one and to every tool that reads " +
                "the result, so they are not a place for a recipe to keep information for itself. Instead:\n" +
                "  - share information between the visit methods of one visitor with cursor messages, " +
                "e.g. this.cursor.messages.set(key, value) and this.cursor.getNearestMessage(key)\n" +
                "  - share information between source files, or from scanning to editing, through the accumulator " +
                "of a ScanningRecipe\n" +
                "  - keep the recipe idempotent by recognizing code it has already changed, " +
                "rather than tagging that code\n" +
                "  - report a finding with foundSearchResult, and a problem with markupError or markupWarn"
            );
        });

        test("a marker added without changing the text is reported as one, not as an empty diff", async () => {
            const sut = new RecipeSpec();
            sut.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
                protected async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
                    const m = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                    return withMarker(m, {kind: "my.Visited", id: randomId()});
                }
            });
            await expect(sut.rewriteRun({...typescript(`foo()`), path: "src/a.ts"})).rejects.toThrow(
                "  src/a.ts (the printed source is unchanged)\n" +
                "    my.Visited on J.MethodInvocation `foo()`\n"
            );
        });

        test("a kind of marker the sources carry on the source file is not explained on an element", async () => {
            const buildMetadata: Marker = {kind: "my.BuildMetadata", id: randomId()};
            const sut = new RecipeSpec();
            sut.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
                protected async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
                    const m = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                    return withMarker(m, buildMetadata);
                }
            });
            await expect(sut.rewriteRun({
                ...typescript(`foo()`),
                path: "src/a.ts",
                beforeRecipe: (cu: JS.CompilationUnit) => withMarker(cu, buildMetadata)
            })).rejects.toThrow(
                "  src/a.ts (the printed source is unchanged)\n" +
                "    my.BuildMetadata on J.MethodInvocation `foo()`\n"
            );
        });

        test("a search result is the recipe's to add", () => {
            const sut = new RecipeSpec();
            sut.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
                protected async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
                    return foundSearchResult(await super.visitMethodInvocation(method, p) as J.MethodInvocation);
                }
            });
            return sut.rewriteRun(typescript(`foo()`, `/*~~>*/foo()`));
        });

        test("a syntax marker that a fresh parse of the output also has is explained", () => {
            const sut = new RecipeSpec();
            sut.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
                protected async visitJsCompilationUnit(cu: JS.CompilationUnit, p: ExecutionContext): Promise<J | undefined> {
                    const terminated: JS.CompilationUnit = {
                        ...cu,
                        statements: cu.statements.map(s => withMarker(s, {kind: J.Markers.Semicolon, id: randomId()}))
                    };
                    return terminated;
                }
            });
            return sut.rewriteRun(typescript(`foo()`, `foo();`));
        });

        test("checkReportingMarkersOnly turns the check off", () => {
            const sut = new RecipeSpec();
            sut.recipe = renameAndTag();
            sut.checkReportingMarkersOnly = false;
            return sut.rewriteRun(typescript(`foo()`, `bar()`));
        });
    });
});
