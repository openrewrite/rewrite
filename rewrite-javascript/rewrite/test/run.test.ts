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
import {ExecutionContext, Recipe, ScanningRecipe, SourceFile, TreeVisitor} from "../src";
import {scheduleRun} from "../src/run";
import {PlainText, PlainTextParser, PlainTextVisitor, text} from "../src/text";
import {RecipeSpec} from "../src/test";
import {ScanningEditor} from "../fixtures/scanning-editor";

class Composite extends Recipe {
    name = "org.openrewrite.test.composite";
    displayName = "Composite";
    description = "A plain composite recipe that only contributes a recipe list.";

    constructor(private readonly children: Recipe[]) {
        super();
    }

    async recipeList(): Promise<Recipe[]> {
        return this.children;
    }
}

class InliningComposite extends Recipe {
    name = "org.openrewrite.test.inlining-composite";
    displayName = "Inlining composite";
    description = "A plain composite that instantiates its sub-recipes inside recipeList().";

    async recipeList(): Promise<Recipe[]> {
        return [new ScanningEditor()];
    }
}

class Exclaim extends Recipe {
    name = "org.openrewrite.test.exclaim";
    displayName = "Exclaim";
    description = "Appends an exclamation mark to each text file.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends PlainTextVisitor<ExecutionContext> {
            override async visitText(t: PlainText): Promise<PlainText | undefined> {
                return {...t, text: t.text + "!"};
            }
        };
    }
}

class DeleteFile extends Recipe {
    name = "org.openrewrite.test.delete-file";
    displayName = "Delete file";
    description = "Deletes every text file it visits.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends PlainTextVisitor<ExecutionContext> {
            override async visitText(): Promise<PlainText | undefined> {
                return undefined;
            }
        };
    }
}

async function parse(...sources: string[]): Promise<SourceFile[]> {
    const parsed: SourceFile[] = [];
    for await (const sourceFile of new PlainTextParser().parse(
        ...sources.map((text, i) => ({text, sourcePath: `file${i}.txt`})))) {
        parsed.push(sourceFile);
    }
    return parsed;
}

describe("scheduleRun", () => {
    describe("scanning phase traversal", () => {
        test("scanning recipe one level under a plain composite", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new Composite([new ScanningEditor()]);
            await spec.rewriteRun(
                text("hello", "hello (count: 1)")
            );
        });

        test("scanning recipe several levels deep", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new Composite([new Composite([new Composite([new ScanningEditor()])])]);
            await spec.rewriteRun(
                text("hello", "hello (count: 1)")
            );
        });

        test("scanning recipe positioned after a plain sibling", async () => {
            // The root is itself a scanning recipe, so traversal starts out fine. The plain
            // sibling in front of the scanning recipe is what used to abort the recipe list.
            class ScanningRoot extends ScanningRecipe<{}> {
                name = "org.openrewrite.test.scanning-root";
                displayName = "Scanning root";
                description = "A scanning recipe that contributes a recipe list.";

                initialValue(): {} {
                    return {};
                }

                async recipeList(): Promise<Recipe[]> {
                    return [new Composite([]), new ScanningEditor()];
                }
            }

            const spec = new RecipeSpec();
            spec.recipe = new ScanningRoot();
            await spec.rewriteRun(
                text("hello", "hello (count: 1)")
            );
        });

        test("scanning recipe instantiated inside recipeList()", async () => {
            // The recipe list is resolved once per run, so the scan phase and the edit phase
            // see the same sub-recipe instance and therefore the same accumulator.
            const spec = new RecipeSpec();
            spec.recipe = new InliningComposite();
            await spec.rewriteRun(
                text("alpha", "alpha (count: 2)"),
                text("beta", "beta (count: 2)")
            );
        });

        test("accumulator sees every file before any file is edited", async () => {
            const spec = new RecipeSpec();
            spec.recipe = new Composite([new ScanningEditor()]);
            await spec.rewriteRun(
                text("alpha", "alpha (count: 3)"),
                text("beta", "beta (count: 3)"),
                text("gamma", "gamma (count: 3)")
            );
        });

        test("a scanner that returns undefined does not halt the run", async () => {
            // Scanning must not modify the tree, so the visit result is discarded entirely,
            // matching the Java RecipeRunCycle, which keeps the original source file.
            class DeletingScanner extends ScanningRecipe<{}> {
                name = "org.openrewrite.test.deleting-scanner";
                displayName = "Deleting scanner";
                description = "A scanner whose visitor returns undefined.";

                initialValue(): {} {
                    return {};
                }

                async scanner(): Promise<TreeVisitor<any, ExecutionContext>> {
                    return new class extends PlainTextVisitor<ExecutionContext> {
                        override async visitText(): Promise<PlainText | undefined> {
                            return undefined;
                        }
                    };
                }
            }

            const spec = new RecipeSpec();
            spec.recipe = new Composite([new DeletingScanner(), new ScanningEditor()]);
            await spec.rewriteRun(
                text("hello", "hello (count: 1)")
            );
        });
    });

    test("onComplete of a nested recipe finishes before the run returns", async () => {
        const completed: string[] = [];

        class Completing extends Recipe {
            name = "org.openrewrite.test.completing";
            displayName = "Completing";
            description = "Records an asynchronous onComplete.";

            constructor(private readonly label: string) {
                super();
            }

            override async onComplete(): Promise<void> {
                await new Promise(resolve => setTimeout(resolve, 0));
                completed.push(this.label);
            }
        }

        await scheduleRun(
            new Composite([new Completing("child"), new Composite([new Completing("grandchild")])]),
            await parse("hello"),
            new ExecutionContext()
        );

        expect(completed).toEqual(["child", "grandchild"]);
    });

    describe("edit phase short-circuit", () => {
        test("a deleted file stops being visited", async () => {
            const before = await parse("hello");
            const {changeset} = await scheduleRun(
                new Composite([new DeleteFile(), new Exclaim()]),
                before,
                new ExecutionContext()
            );

            expect(changeset).toHaveLength(1);
            expect(changeset[0].before).toBe(before[0]);
            expect(changeset[0].after).toBeUndefined();
        });

        test("a deleted file stops being visited when the run also scans", async () => {
            const before = await parse("hello");
            const {changeset} = await scheduleRun(
                new Composite([new ScanningEditor(), new DeleteFile(), new Exclaim()]),
                before,
                new ExecutionContext()
            );

            expect(changeset).toHaveLength(1);
            expect(changeset[0].before).toBe(before[0]);
            expect(changeset[0].after).toBeUndefined();
        });
    });
});
