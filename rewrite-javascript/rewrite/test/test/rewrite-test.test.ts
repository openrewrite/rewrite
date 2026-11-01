import {fromVisitor, RecipeSpec} from "../../src/test";
import {text} from "../../src/text";
import {json} from "../../src/json";
import {ChangeText} from "../../fixtures/change-text";
import {ReplacedText} from "../../fixtures/replaced-text";
import {ScanningEditor} from "../../fixtures/scanning-editor";
import {
    Cursor,
    DATA_TABLE_STORE,
    ExecutionContext,
    InMemoryDataTableStore,
    noopVisitor,
    Option,
    Recipe,
    ScanningRecipe,
    SourceFile,
    TreeVisitor
} from "../../src";
import {emptyMarkers, findMarker, foundSearchResult, Marker, Markers, MarkersKind} from "../../src/markers";
import {PlainText, PlainTextParser, PlainTextVisitor} from "../../src/text";
import {randomId} from "../../src/uuid";
import {J} from "../../src/java";
import {JavaScriptParser, JavaScriptVisitor, JS, NodeResolutionResultKind, typescript} from "../../src/javascript";

function withMarker<T extends { markers: Markers }>(t: T, marker: Marker): T {
    return {...t, markers: {...t.markers, markers: [...t.markers.markers, marker]}};
}

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

    describe("generated and deleted files", () => {
        class GenerateText extends ScanningRecipe<{ exists: boolean }> {
            name = "org.openrewrite.test.generate-text";
            displayName = "Generate text";
            description = "Generates `generated.txt` unless it exists.";

            initialValue(_ctx: ExecutionContext): { exists: boolean } {
                return {exists: false};
            }

            async scanner(acc: { exists: boolean }): Promise<TreeVisitor<any, ExecutionContext>> {
                return new class extends PlainTextVisitor<ExecutionContext> {
                    protected override async visitText(text: PlainText, _ctx: ExecutionContext): Promise<PlainText | undefined> {
                        acc.exists ||= text.sourcePath === "generated.txt";
                        return text;
                    }
                };
            }

            async generate(acc: { exists: boolean }, _ctx: ExecutionContext): Promise<SourceFile[]> {
                if (acc.exists) {
                    return [];
                }
                const generated: SourceFile[] = [];
                for await (const file of new PlainTextParser().parse({text: "generated", sourcePath: "generated.txt"})) {
                    generated.push(file);
                }
                return generated;
            }
        }

        const deleteText = () => fromVisitor(new class extends PlainTextVisitor<ExecutionContext> {
            protected override async visitText(_text: PlainText, _ctx: ExecutionContext): Promise<PlainText | undefined> {
                return undefined;
            }
        });

        test("a generated file alongside another source", () => {
            const sut = new RecipeSpec();
            sut.recipe = new GenerateText();
            return sut.rewriteRun(
                {...text("existing"), path: "existing.txt"},
                {...text(null, "generated"), path: "generated.txt"}
            );
        });

        test("a generated file no spec describes fails", async () => {
            const sut = new RecipeSpec();
            sut.recipe = new GenerateText();
            const run = sut.rewriteRun({...text("existing"), path: "existing.txt"});
            await expect(run).rejects.toThrow("The recipe generated source files the test did not expect");
            await expect(run).rejects.toThrow("  generated.txt");
        });

        test("a generated file no spec describes fails before its parser markers are taken for added ones", async () => {
            const sut = new RecipeSpec();
            sut.recipe = new class extends ScanningRecipe<{}> {
                name = "org.openrewrite.test.generate-typescript";
                displayName = "Generate TypeScript";
                description = "Generates `generated.ts`.";

                initialValue(_ctx: ExecutionContext): {} {
                    return {};
                }

                async generate(_acc: {}, _ctx: ExecutionContext): Promise<SourceFile[]> {
                    const generated: SourceFile[] = [];
                    for await (const file of new JavaScriptParser().parse({text: "const a = 1;", sourcePath: "generated.ts"})) {
                        generated.push(file);
                    }
                    return generated;
                }
            };
            await expect(sut.rewriteRun({...text("existing"), path: "existing.txt"}))
                .rejects.toThrow("The recipe generated source files the test did not expect:\n  generated.ts");
        });

        test("an empty before is an empty file, not a generated one", () => {
            const sut = new RecipeSpec();
            sut.recipe = fromVisitor(new class extends PlainTextVisitor<ExecutionContext> {
                protected override async visitText(text: PlainText, _ctx: ExecutionContext): Promise<PlainText | undefined> {
                    return text.text === "" ? {...text, text: "filled"} : text;
                }
            });
            return sut.rewriteRun({...text("", "filled"), path: "empty.txt"});
        });

        test("after: null expects the file to be deleted", () => {
            const sut = new RecipeSpec();
            sut.recipe = deleteText();
            return sut.rewriteRun(
                {...text("doomed", null), path: "doomed.txt"}
            );
        });

        test("after: null fails when the file is not deleted", async () => {
            const sut = new RecipeSpec();
            await expect(sut.rewriteRun(
                {...text("kept", null), path: "kept.txt"}
            )).rejects.toThrow("Expected recipe to delete");
        });

        test("a deletion fails a spec that expects no change", async () => {
            const sut = new RecipeSpec();
            sut.recipe = deleteText();
            await expect(sut.rewriteRun(
                {...text("doomed"), path: "doomed.txt"}
            )).rejects.toThrow("deleted the file");
        });
    });

    describe("extra cycles", () => {
        // Merges the first two terms of a sum per visit, as a recipe that only looks one step ahead would
        const mergeFirstTwo = () => fromVisitor(new class extends PlainTextVisitor<ExecutionContext> {
            protected override async visitText(text: PlainText, _ctx: ExecutionContext): Promise<PlainText | undefined> {
                const merged = text.text.replace(/^(\w+(?:\+\w+)*),(\w+)/, "$1+$2");
                return merged === text.text ? text : {...text, text: merged};
            }
        });

        test("a recipe that changes its expected result again fails", async () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = mergeFirstTwo();

            // when
            const run = sut.rewriteRun({...text("a,b,c", "a+b,c"), path: "sum.txt"});

            // then
            await expect(run).rejects.toThrow(
                "Expected recipe to complete in 1 cycle, but cycle 2 changed the sources again:\n" +
                "  sum.txt\n" +
                "Expected:\na+b,c\n\nActual:\na+b+c\n"
            );
        });

        test("expectedCyclesThatMakeChanges compares the result of the last expected cycle", () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = mergeFirstTwo();
            sut.expectedCyclesThatMakeChanges = 2;

            // when, then
            return sut.rewriteRun(text("a,b,c", "a+b+c"));
        });

        test("expectedCyclesThatMakeChanges fails a recipe that settles sooner", async () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = mergeFirstTwo();
            sut.expectedCyclesThatMakeChanges = 2;

            // when
            const run = sut.rewriteRun(text("a,b", "a+b"));

            // then
            await expect(run).rejects.toThrow(
                "Expected recipe to complete in 2 cycles, but it settled after 1 cycle.");
        });

        test("cycles = 1 turns the check off", () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = mergeFirstTwo();
            sut.cycles = 1;

            // when, then
            return sut.rewriteRun(text("a,b,c", "a+b,c"));
        });

        test("a recipe that changes the LST again without changing the text fails", async () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = fromVisitor(new class extends PlainTextVisitor<ExecutionContext> {
                protected override async visitText(text: PlainText, _ctx: ExecutionContext): Promise<PlainText | undefined> {
                    return {...text, text: text.text.toUpperCase()};
                }
            });

            // when
            const run = sut.rewriteRun({...text("a", "A"), path: "a.txt"});

            // then
            await expect(run).rejects.toThrow(
                "  a.txt, whose LST it changed without changing the printed source");
        });

        test("a recipe that generates a file again fails", async () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = new class extends ScanningRecipe<{}> {
                name = "org.openrewrite.test.generate-always";
                displayName = "Generate always";
                description = "Generates `generated.txt` whether or not it exists.";

                initialValue(_ctx: ExecutionContext): {} {
                    return {};
                }

                async generate(_acc: {}, _ctx: ExecutionContext): Promise<SourceFile[]> {
                    const generated: SourceFile[] = [];
                    for await (const file of new PlainTextParser().parse({text: "generated", sourcePath: "generated.txt"})) {
                        generated.push(file);
                    }
                    return generated;
                }
            };

            // when
            const run = sut.rewriteRun({...text(null, "generated"), path: "generated.txt"});

            // then
            await expect(run).rejects.toThrow("  generated.txt, which it generated");
        });

        test("a search recipe settles even though it adds its marker on every run", () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
                protected async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
                    return foundSearchResult(await super.visitMethodInvocation(method, p) as J.MethodInvocation);
                }
            });

            // when, then
            return sut.rewriteRun(typescript(`foo()`, `/*~~>*/foo()`));
        });

        test("data tables are written by the first cycle only", async () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = fromVisitor(new class extends PlainTextVisitor<ExecutionContext> {
                protected override async visitText(text: PlainText, ctx: ExecutionContext): Promise<PlainText | undefined> {
                    ReplacedText.dataTable.insertRow(ctx, new ReplacedText(text.sourcePath, text.text));
                    return foundSearchResult(text);
                }
            });

            // when
            await sut.rewriteRun(text("found", "~~>found"));

            // then
            const store: InMemoryDataTableStore = sut.recipeExecutionContext.messages[DATA_TABLE_STORE];
            expect(store.getRows(ReplacedText.dataTable.descriptor.name)).toHaveLength(1);
        });

        test("a recipe that recognizes its output only by the shape of its LST fails on a fresh parse", async () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = fromVisitor(new class extends PlainTextVisitor<ExecutionContext> {
                protected override async visitText(text: PlainText, _ctx: ExecutionContext): Promise<PlainText | undefined> {
                    if (text.snippets.length > 0) {
                        return text;
                    }
                    const snippet: PlainText.Snippet = {kind: PlainText.Kind.Snippet, id: randomId(), markers: emptyMarkers, text: "!"};
                    return {...text, snippets: [snippet]};
                }
            });

            // when
            const run = sut.rewriteRun({...text("hi", "hi!"), path: "hi.txt"});

            // then
            await expect(run).rejects.toThrow(
                "Running the recipe on a fresh parse of its output changed the sources again:\n" +
                "  hi.txt\n" +
                "Expected:\nhi!\n\nActual:\nhi!!\n"
            );
        });

        test("a fresh parse keeps what the source file carries from outside its text", () => {
            // given
            const loud: Marker = {kind: "my.Loud", id: randomId()};
            const sut = new RecipeSpec();
            sut.recipe = fromVisitor(new class extends PlainTextVisitor<ExecutionContext> {
                protected override async visitText(text: PlainText, _ctx: ExecutionContext): Promise<PlainText | undefined> {
                    const cased = findMarker(text, "my.Loud") ? text.text.toUpperCase() : text.text.toLowerCase();
                    return cased === text.text ? text : {...text, text: cased};
                }
            });

            // when, then
            return sut.rewriteRun({
                ...text("Hi", "HI"),
                beforeRecipe: (sourceFile: PlainText) => ({...sourceFile, markers: {...sourceFile.markers, markers: [loud]}})
            });
        });
    });

    describe("unchanged execution context", () => {
        // Keeps what it has seen on the execution context, where an accumulator or cursor message belongs
        const rememberInContext = () => fromVisitor(new class extends PlainTextVisitor<ExecutionContext> {
            protected override async visitText(text: PlainText, ctx: ExecutionContext): Promise<PlainText | undefined> {
                ctx.messages["my.seen"] = [...(ctx.messages["my.seen"] ?? []), text.sourcePath];
                return text;
            }
        });

        test("a recipe that puts a message on the execution context fails", async () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = rememberInContext();

            // when
            const run = sut.rewriteRun(text("hello"));

            // then
            await expect(run).rejects.toThrow(
                "Keep state that spans source files in a ScanningRecipe's accumulator, and state within one source " +
                "file in cursor messages, with this.cursor.messages.set(key, value). org.openrewrite.adhoc put " +
                "messages on the ExecutionContext, which every recipe and source file in a run shares, so what it " +
                "keeps there leaks into the others:\n" +
                '  "my.seen" at PlainTextVisitor.visitText (test/test/rewrite-test.test.ts:');
            await expect(run).rejects.toThrow(
                "A recipe that must change the ExecutionContext opts out with spec.checkExecutionContextUnchanged = false.");
        });

        test("a recipe that only reads the execution context passes", () => {
            // given
            const sut = new RecipeSpec();
            sut.recipeExecutionContext = new ExecutionContext({"my.greeting": "hello"});
            sut.recipe = fromVisitor(new class extends PlainTextVisitor<ExecutionContext> {
                protected override async visitText(text: PlainText, ctx: ExecutionContext): Promise<PlainText | undefined> {
                    const greeting = ctx.messages["my.greeting"];
                    return text.text === greeting ? text : {...text, text: greeting};
                }
            });

            // when, then
            return sut.rewriteRun(text("hi", "hello"));
        });

        test("checkExecutionContextUnchanged turns the check off", async () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = rememberInContext();
            sut.checkExecutionContextUnchanged = false;

            // when
            await sut.rewriteRun({...text("hello"), path: "hello.txt"});

            // then
            expect(sut.recipeExecutionContext.messages["my.seen"]).toContain("hello.txt");
        });
    });

    describe("no edits in the scanning phase", () => {
        // Counts the texts it scans and upper-cases each, an edit the scanning phase throws away
        class ShoutWhileScanning extends ScanningRecipe<{ count: number }> {
            name = "org.openrewrite.test.shout-while-scanning";
            displayName = "Shout while scanning";
            description = "Counts texts while upper-casing them in its scanner.";

            initialValue(_ctx: ExecutionContext): { count: number } {
                return {count: 0};
            }

            async scanner(acc: { count: number }): Promise<TreeVisitor<any, ExecutionContext>> {
                return new class extends PlainTextVisitor<ExecutionContext> {
                    protected override async visitText(text: PlainText, _ctx: ExecutionContext): Promise<PlainText | undefined> {
                        acc.count++;
                        return {...text, text: text.text.toUpperCase()};
                    }
                };
            }
        }

        test("a scanner that edits a tree fails", async () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = new ShoutWhileScanning();

            // when
            const run = sut.rewriteRun({...text("hello"), path: "hello.txt"});

            // then
            await expect(run).rejects.toThrow(
                "Record what the scanner finds in the accumulator, and make the edits in editorWithData(acc). " +
                "org.openrewrite.test.shout-while-scanning's scanner changed hello.txt, and the scanning phase " +
                "discards edits. A scanner that reuses an editing visitor opts out with " +
                "spec.checkNoEditsInScanningPhase = false.");
        });

        test("a scanner that only records what it finds passes", () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = new ScanningEditor();

            // when, then
            return sut.rewriteRun(text("hello", "hello (count: 1)"));
        });

        test("checkNoEditsInScanningPhase turns the check off", () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = new ShoutWhileScanning();
            sut.checkNoEditsInScanningPhase = false;

            // when, then
            return sut.rewriteRun(text("hello"));
        });
    });

    describe("lightweight accumulators", () => {
        // Keeps whatever `keep` takes from each text it scans, or edits, in its accumulator
        class Keep<A> extends ScanningRecipe<A> {
            name = "org.openrewrite.test.keep";
            displayName = "Keep";
            description = "Keeps what its visitor is given in its accumulator.";

            constructor(private readonly initial: () => A,
                        private readonly keep: (acc: A, text: PlainText, cursor: Cursor) => void,
                        private readonly whileEditing: boolean = false) {
                super();
            }

            initialValue(_ctx: ExecutionContext): A {
                return this.initial();
            }

            async scanner(acc: A): Promise<TreeVisitor<any, ExecutionContext>> {
                return this.whileEditing ? noopVisitor() : this.keeper(acc);
            }

            async editorWithData(acc: A): Promise<TreeVisitor<any, ExecutionContext>> {
                return this.whileEditing ? this.keeper(acc) : noopVisitor();
            }

            private keeper(acc: A): TreeVisitor<any, ExecutionContext> {
                const keep = this.keep;
                return new class extends PlainTextVisitor<ExecutionContext> {
                    protected override async visitText(text: PlainText, _ctx: ExecutionContext): Promise<PlainText | undefined> {
                        keep(acc, text, this.cursor);
                        return text;
                    }
                };
            }
        }

        test("a source file kept in the accumulator fails", async () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = new Keep(() => ({sourceFiles: new Map<string, PlainText>()}),
                (acc, text) => acc.sourceFiles.set(text.sourcePath, text));

            // when
            const run = sut.rewriteRun({...text("foo"), path: "foo.txt"});

            // then
            await expect(run).rejects.toThrow(
                "A ScanningRecipe's accumulator should keep the values the recipe needs, such as package names, " +
                "versions, paths and ids, and no heavyweight objects, such as trees, cursors, visitors, execution " +
                "contexts and dependency models:\n" +
                "  org.openrewrite.test.keep keeps\n" +
                "    acc.sourceFiles[foo.txt] -> PlainText foo.txt, after scanning\n");
            await expect(run).rejects.toThrow(
                "A recipe that must keep a heavyweight object opts out with spec.checkLightweightAccumulators = false.");
        });

        test("a cursor kept in the accumulator fails", async () => {
            const sut = new RecipeSpec();
            sut.recipe = new Keep(() => [] as Cursor[], (acc, _text, cursor) => acc.push(cursor));
            await expect(sut.rewriteRun(text("foo"))).rejects.toThrow("    acc[0] -> Cursor, after scanning\n");
        });

        test("a dependency model kept in the accumulator fails", async () => {
            const sut = new RecipeSpec();
            sut.recipe = new Keep(() => ({resolution: undefined as Marker | undefined}),
                (acc, text) => acc.resolution = findMarker(text, NodeResolutionResultKind));
            await expect(sut.rewriteRun({
                ...text("foo"),
                beforeRecipe: (sourceFile: PlainText) => withMarker(sourceFile, {kind: NodeResolutionResultKind, id: randomId()})
            })).rejects.toThrow("    acc.resolution -> NodeResolutionResult, after scanning\n");
        });

        test("a source file kept while editing fails", async () => {
            const sut = new RecipeSpec();
            sut.recipe = new Keep(() => [] as PlainText[], (acc, text) => acc.push(text), true);
            await expect(sut.rewriteRun({...text("foo"), path: "foo.txt"}))
                .rejects.toThrow("    acc[0] -> PlainText foo.txt, after editing\n");
        });

        test("a collection of the accumulator's own is read without running its code", async () => {
            // given
            const ownCode = () => {
                throw new Error("The check ran code of the accumulator's own");
            };
            class Index extends Map<string, PlainText> {
                override forEach = ownCode;
                override entries = ownCode;
                override [Symbol.iterator] = ownCode;
            }
            const sut = new RecipeSpec();
            sut.recipe = new Keep(() => ({files: new Index(), get broken(): never { return ownCode(); }}),
                (acc, text) => Map.prototype.set.call(acc.files, text.sourcePath, text));

            // when
            const run = sut.rewriteRun({...text("foo"), path: "foo.txt"});

            // then
            await expect(run).rejects.toThrow("    acc.files[foo.txt] -> PlainText foo.txt, after scanning\n");
        });

        test("values and small markers kept in the accumulator pass", () => {
            const sut = new RecipeSpec();
            sut.recipe = new Keep(() => ({paths: [] as string[], markers: [] as Markers[]}),
                (acc, text) => {
                    acc.paths.push(text.sourcePath);
                    acc.markers.push(text.markers);
                });
            return sut.rewriteRun({
                ...text("foo"),
                beforeRecipe: (sourceFile: PlainText) => withMarker(sourceFile, {kind: "my.Team", id: randomId()})
            });
        });

        test("checkLightweightAccumulators turns the check off", () => {
            const sut = new RecipeSpec();
            sut.recipe = new Keep(() => [] as PlainText[], (acc, text) => acc.push(text));
            sut.checkLightweightAccumulators = false;
            return sut.rewriteRun(text("foo"));
        });
    });

    describe("empty sources", () => {
        class Capitalize extends Recipe {
            name = "org.openrewrite.test.capitalize";
            displayName = "Capitalize";
            description = "Capitalizes the first letter of each text.";

            @Option({displayName: "Checked", description: "Whether the recipe checks that the text has a first letter."})
            checked!: boolean;

            constructor(options: { checked: boolean }) {
                super(options);
            }

            async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
                return capitalize(this.checked);
            }
        }

        function capitalize(checked: boolean): TreeVisitor<any, ExecutionContext> {
            return new class extends PlainTextVisitor<ExecutionContext> {
                protected override async visitText(text: PlainText, _ctx: ExecutionContext): Promise<PlainText | undefined> {
                    if (checked && text.text.length === 0) {
                        return text;
                    }
                    const capitalized = text.text[0].toUpperCase() + text.text.substring(1);
                    return capitalized === text.text ? text : {...text, text: capitalized};
                }
            };
        }

        const team = (sourceFile: PlainText) => withMarker(sourceFile, {kind: "my.Team", id: randomId()});

        test("a recipe that throws on an empty file fails", async () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = new Capitalize({checked: false});

            // when
            const run = sut.rewriteRun({...text("hello", "Hello"), path: "hello.txt", beforeRecipe: team});

            // then
            await expect(run).rejects.toThrow(
                "Check for the structure the recipe edits before using it, so that it handles an empty file or " +
                "leaves it unchanged instead of throwing.\n" +
                "org.openrewrite.test.capitalize threw when run on an empty PlainText at hello.txt with the markers " +
                "of the test's source (Team):\n" +
                "    TypeError: Cannot read properties of undefined (reading 'toUpperCase')\n" +
                "        at PlainTextVisitor.visitText (test/test/rewrite-test.test.ts:");
            await expect(run).rejects.toThrow(
                "Then add a test with an empty source, such as typescript(\"\"), json(\"\") or yaml(\"\"), that " +
                "states the intended result.");
        });

        test("a recipe that checks the structure passes", () => {
            const sut = new RecipeSpec();
            sut.recipe = new Capitalize({checked: true});
            return sut.rewriteRun(text("hello", "Hello"));
        });

        test("an ad hoc recipe is not probed", () => {
            const sut = new RecipeSpec();
            sut.recipe = fromVisitor(capitalize(false));
            return sut.rewriteRun(text("hello", "Hello"));
        });

        test("the empty file has the markers the hooks gave the source", () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = new class extends Recipe {
                name = "org.openrewrite.test.requires-team";
                displayName = "Requires team";
                description = "Changes each text of a team, and throws on a text of none.";

                async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
                    return new class extends PlainTextVisitor<ExecutionContext> {
                        protected override async visitText(text: PlainText, _ctx: ExecutionContext): Promise<PlainText | undefined> {
                            if (!findMarker(text, "my.Team")) {
                                throw new Error("A text of no team");
                            }
                            return text.text === "changed" ? text : {...text, text: "changed"};
                        }
                    };
                }
            };

            // when, then
            return sut.rewriteRun({...text("original", "changed"), beforeRecipe: team});
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
        sut.checkExecutionContextUnchanged = false;
        sut.checkNoEditsInScanningPhase = false;
        sut.checkLightweightAccumulators = false;
        sut.cycles = 1;
        sut.expectedCyclesThatMakeChanges = 3;
        RecipeSpec.restoreSuiteConfiguration();

        expect(sut.recipe).toBe(suiteRecipe);
        expect(sut.allowEmptyDiff).toBe(true);
        expect(sut.checkParsePrintIdempotence).toBe(true);
        expect(sut.checkReportingMarkersOnly).toBe(true);
        expect(sut.checkExecutionContextUnchanged).toBe(true);
        expect(sut.checkNoEditsInScanningPhase).toBe(true);
        expect(sut.checkLightweightAccumulators).toBe(true);
        expect(sut.cycles).toBeUndefined();
        expect(sut.expectedCyclesThatMakeChanges).toBeUndefined();
    });

    describe("added markers", () => {
        // Renames foo() to bar(), and tags what it renamed
        const renameAndTag = () => fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
            protected async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
                const m = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                if (m.name.simpleName !== "foo") {
                    return m;
                }
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
                    if (cu.statements.every(s => findMarker(s, J.Markers.Semicolon))) {
                        return cu;
                    }
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

    describe("data table assertions", () => {
        const recordReplacedText = () => fromVisitor(new class extends PlainTextVisitor<ExecutionContext> {
            protected override async visitText(text: PlainText, ctx: ExecutionContext): Promise<PlainText | undefined> {
                ReplacedText.dataTable.insertRow(ctx, new ReplacedText(text.sourcePath, text.text));
                return text;
            }
        });

        test("an assertion receives the rows the recipe wrote", async () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = recordReplacedText();
            let received: ReplacedText[] | undefined;
            sut.dataTable<ReplacedText>(ReplacedText.dataTable.descriptor.name, rows => {
                received = rows;
            });

            // when
            await sut.rewriteRun({...text("hello"), path: "a.txt"});

            // then
            expect(received).toEqual([new ReplacedText("a.txt", "hello")]);
        });

        test("a failing assertion fails the run", async () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = recordReplacedText();
            sut.dataTable<ReplacedText>(ReplacedText.dataTable.descriptor.name, rows => {
                expect(rows).toHaveLength(2);
            });

            // when
            const run = sut.rewriteRun({...text("hello"), path: "a.txt"});

            // then
            await expect(run).rejects.toThrow("to have a length of 2 but got 1");
        });

        test("an assertion on a data table the recipe never wrote fails the run", async () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = recordReplacedText();
            sut.dataTable("org.openrewrite.NotWritten", () => {
            });

            // when
            const run = sut.rewriteRun(text("hello"));

            // then
            await expect(run).rejects.toThrow(
                "No data table named org.openrewrite.NotWritten\n" +
                `Found data tables: ${ReplacedText.dataTable.descriptor.name}`);
        });

        test("each run starts with no rows", async () => {
            // given
            const sut = new RecipeSpec();
            sut.recipe = recordReplacedText();
            await sut.rewriteRun({...text("first"), path: "first.txt"});
            let received: ReplacedText[] | undefined;
            sut.dataTable<ReplacedText>(ReplacedText.dataTable.descriptor.name, rows => {
                received = rows;
            });

            // when
            await sut.rewriteRun({...text("second"), path: "second.txt"});

            // then
            expect(received).toEqual([new ReplacedText("second.txt", "second")]);
        });
    });
});
