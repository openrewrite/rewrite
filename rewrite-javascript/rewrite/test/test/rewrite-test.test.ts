import {describe} from "@jest/globals";
import {RecipeSpec} from "../../src/test";
import {text} from "../../src/text";
import {json} from "../../src/json";
import {ChangeText} from "../../fixtures/change-text";
import {ExecutionContext, Recipe, ScanningRecipe, TreeVisitor} from "../../src";
import {foundSearchResult, MarkersKind} from "../../src/markers";
import {PlainText, PlainTextVisitor} from "../../src/text";
import {randomId} from "../../src/uuid";

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
});
