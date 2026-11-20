/*
 * Copyright 2026 the original author or authors.
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
import {
    Column,
    DATA_TABLE_STORE,
    DataTable,
    ExecutionContext,
    foundSearchResult,
    InMemoryDataTableStore,
    Recipe,
    RecipeRef,
    ScanningRecipe,
    SourceFile,
    TreeVisitor
} from "../src";
import {scheduleRun} from "../src/run";
import {PlainText, PlainTextParser, PlainTextVisitor, text} from "../src/text";
import {RecipeSpec} from "../src/test";

class Composite extends Recipe {
    name = "org.openrewrite.test.composite";
    displayName = "Composite";
    description = "Only contributes a recipe list.";

    constructor(private readonly children: Recipe[]) {
        super();
    }

    async recipeList(): Promise<Recipe[]> {
        return this.children;
    }
}

class Guarded extends ScanningRecipe<{}> {
    name = "org.openrewrite.test.guarded";
    displayName = "Guarded";
    description = "Tags each source file it edits, as do the recipes in its recipe list.";

    constructor(private readonly conditions: (Recipe | RecipeRef)[],
                private readonly children: Recipe[] = [],
                private readonly label = "own") {
        super();
    }

    initialValue(): {} {
        return {};
    }

    async editPreconditions(): Promise<(Recipe | RecipeRef)[]> {
        return this.conditions;
    }

    async recipeList(): Promise<Recipe[]> {
        return this.children;
    }

    async editorWithData(): Promise<TreeVisitor<any, ExecutionContext>> {
        return tagVisitor(this.label);
    }
}

function tagVisitor(label: string): TreeVisitor<any, ExecutionContext> {
    return new class extends PlainTextVisitor<ExecutionContext> {
        override async visitText(t: PlainText): Promise<PlainText | undefined> {
            return t.text.includes(`[${label}]`) ? t : {...t, text: `${t.text}[${label}]`};
        }
    };
}

class Tag extends Recipe {
    name = "org.openrewrite.test.tag";
    displayName = "Tag";
    description = "Appends a label to each source file.";

    constructor(private readonly label: string) {
        super();
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return tagVisitor(this.label);
    }
}

class ScanningTag extends ScanningRecipe<{ scanned: number }> {
    name = "org.openrewrite.test.scanning-tag";
    displayName = "Scanning tag";
    description = "Counts the source files and appends a label to each.";

    readonly scannedPerRun: number[] = [];

    constructor(private readonly label: string) {
        super();
    }

    initialValue(): { scanned: number } {
        const acc = {scanned: 0};
        this.scannedPerRun.push(0);
        return acc;
    }

    async scanner(acc: { scanned: number }): Promise<TreeVisitor<any, ExecutionContext>> {
        const scannedPerRun = this.scannedPerRun;
        return new class extends PlainTextVisitor<ExecutionContext> {
            override async visitText(t: PlainText): Promise<PlainText | undefined> {
                acc.scanned++;
                scannedPerRun[scannedPerRun.length - 1] = acc.scanned;
                return t;
            }
        };
    }

    async editorWithData(): Promise<TreeVisitor<any, ExecutionContext>> {
        return tagVisitor(this.label);
    }
}

class Contains extends Recipe {
    name = "org.openrewrite.test.contains";
    displayName = "Contains";
    description = "Marks a source file that contains a word as found.";

    evaluations = 0;

    constructor(private readonly word: string) {
        super();
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        const recipe = this;
        return new class extends PlainTextVisitor<ExecutionContext> {
            override async visitText(t: PlainText): Promise<PlainText | undefined> {
                recipe.evaluations++;
                return t.text.includes(recipe.word) ? foundSearchResult(t) : t;
            }
        };
    }
}

class ContainsWithConditions extends ScanningRecipe<{}> {
    name = "org.openrewrite.test.contains-with-conditions";
    displayName = "Contains with conditions";
    description = "Marks a source file that contains a word as found, if it meets the conditions.";

    constructor(private readonly word: string, private readonly conditions: Recipe[]) {
        super();
    }

    initialValue(): {} {
        return {};
    }

    async editPreconditions(): Promise<Recipe[]> {
        return this.conditions;
    }

    async editorWithData(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new Contains(this.word).editor();
    }
}

class RepositoryContains extends ScanningRecipe<{ found: boolean }> {
    name = "org.openrewrite.test.repository-contains";
    displayName = "Repository contains";
    description = "Marks every source file as found when any source file contains a word.";

    constructor(private readonly word: string) {
        super();
    }

    initialValue(): { found: boolean } {
        return {found: false};
    }

    async scanner(acc: { found: boolean }): Promise<TreeVisitor<any, ExecutionContext>> {
        const word = this.word;
        return new class extends PlainTextVisitor<ExecutionContext> {
            override async visitText(t: PlainText): Promise<PlainText | undefined> {
                acc.found ||= t.text.includes(word);
                return t;
            }
        };
    }

    async editorWithData(acc: { found: boolean }): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends PlainTextVisitor<ExecutionContext> {
            override async visitText(t: PlainText): Promise<PlainText | undefined> {
                return acc.found ? foundSearchResult(t) : t;
            }
        };
    }
}

class Replace extends Recipe {
    name = "org.openrewrite.test.replace";
    displayName = "Replace";
    description = "Replaces a word.";

    constructor(private readonly from: string, private readonly to: string) {
        super();
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        const {from, to} = this;
        return new class extends PlainTextVisitor<ExecutionContext> {
            override async visitText(t: PlainText): Promise<PlainText | undefined> {
                return t.text.includes(from) ? {...t, text: t.text.replace(from, to)} : t;
            }
        };
    }
}

class Generate extends ScanningRecipe<{}> {
    name = "org.openrewrite.test.generate";
    displayName = "Generate";
    description = "Generates a text file.";

    constructor(private readonly sourcePath: string, private readonly content: string) {
        super();
    }

    initialValue(): {} {
        return {};
    }

    async generate(): Promise<SourceFile[]> {
        return parse([this.sourcePath, this.content]);
    }
}

class Word {
    @Column({displayName: "Word", description: "The word."})
    readonly word: string;

    constructor(word: string) {
        this.word = word;
    }

    static dataTable = new DataTable<Word>("org.openrewrite.test.words", "Words", "Recorded words.", Word);
}

class RecordWord extends Recipe {
    name = "org.openrewrite.test.record-word";
    displayName = "Record word";
    description = "Records each source file and marks it as found.";

    constructor(private readonly word: string) {
        super();
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        const word = this.word;
        return new class extends PlainTextVisitor<ExecutionContext> {
            override async visitText(t: PlainText, ctx: ExecutionContext): Promise<PlainText | undefined> {
                Word.dataTable.insertRow(ctx, new Word(word));
                return foundSearchResult(t);
            }
        };
    }
}

async function parse(...sources: [string, string][]): Promise<SourceFile[]> {
    const parsed: SourceFile[] = [];
    for await (const sourceFile of new PlainTextParser().parse(
        ...sources.map(([sourcePath, text]) => ({text, sourcePath})))) {
        parsed.push(sourceFile);
    }
    return parsed;
}

async function run(recipe: Recipe, sources: SourceFile[] | [string, string][],
                   ctx = new ExecutionContext()): Promise<Record<string, string | undefined>> {
    const before = sources.length > 0 && Array.isArray(sources[0]) ?
        await parse(...sources as [string, string][]) : sources as SourceFile[];
    const {changeset} = await scheduleRun(recipe, before, ctx);
    const after: Record<string, string | undefined> = {};
    for (const result of changeset) {
        const sourceFile = (result.after ?? result.before) as PlainText;
        after[sourceFile.sourcePath] = result.after ? (result.after as PlainText).text : undefined;
    }
    return after;
}

describe("edit preconditions", () => {
    test("gate the editor of the recipe and of every recipe in its recipe list", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new Guarded([new Contains("go")], [
            new Tag("a"),
            new Composite([new Tag("b"), new ScanningTag("c")])
        ]);
        await spec.rewriteRun(
            text("go", "go[own][a][b][c]"),
            text("stop")
        );
    });

    test("a recipe in the recipe list with conditions of its own also needs them", async () => {
        const result = await run(new Guarded([new Contains("a")], [
            new Guarded([new Contains("b")], [new Tag("deep")], "inner")
        ]), [["ab.txt", "a b"], ["a.txt", "a"], ["b.txt", "b"]]);

        expect(result).toEqual({"ab.txt": "a b[own][inner][deep]", "a.txt": "a[own]", "b.txt": "b"});
    });

    test("are evaluated once per source file", async () => {
        // given
        const contains = new Contains("go");

        // when
        await run(new Guarded([contains], [new Tag("a"), new Composite([new Tag("b")])]),
            [["go.txt", "go"], ["stop.txt", "stop"]]);

        // then
        expect(contains.evaluations).toBe(2);
    });

    test("see the edits of the recipes that ran before", async () => {
        const result = await run(new Composite([
            new Replace("x", "go"),
            new Guarded([new Contains("go")])
        ]), [["x.txt", "x"], ["y.txt", "y"]]);

        expect(result).toEqual({"x.txt": "go[own]", "y.txt": "y"});
    });

    test("need every condition to match", async () => {
        const result = await run(new Guarded([new Contains("a"), new Contains("b")]),
            [["ab.txt", "a b"], ["a.txt", "a"]]);

        expect(result).toEqual({"ab.txt": "a b[own]", "a.txt": "a"});
    });

    test("a condition with a recipe list matches when any recipe in it does", async () => {
        const result = await run(new Guarded([new Composite([new Contains("a"), new Contains("b")])]),
            [["a.txt", "a"], ["b.txt", "b"], ["c.txt", "c"]]);

        expect(result).toEqual({"a.txt": "a[own]", "b.txt": "b[own]", "c.txt": "c"});
    });

    test("a condition with conditions of its own matches only when they do", async () => {
        const result = await run(new Guarded([new ContainsWithConditions("a", [new Contains("b")])]),
            [["ab.txt", "a b"], ["a.txt", "a"]]);

        expect(result).toEqual({"ab.txt": "a b[own]", "a.txt": "a"});
    });

    test("scanning conditions scan into accumulators of their own", async () => {
        const result = await run(new Composite([
            new Guarded([new RepositoryContains("marker")], [], "one"),
            new Guarded([new RepositoryContains("absent")], [], "two")
        ]), [["marker.txt", "marker"], ["other.txt", "other"]]);

        expect(result).toEqual({"marker.txt": "marker[one]", "other.txt": "other[one]"});
    });

    test("scanning and generation are not affected", async () => {
        // given
        const scanningTag = new ScanningTag("c");

        // when
        const result = await run(new Guarded([new Contains("go")], [
            scanningTag,
            new Generate("go-generated.txt", "go generated"),
            new Generate("stop-generated.txt", "stop generated")
        ]), [["go.txt", "go"], ["stop.txt", "stop"]]);

        // then
        expect(scanningTag.scannedPerRun).toEqual([2]);
        expect(result).toEqual({
            "go.txt": "go[own][c]",
            "stop.txt": "stop",
            "go-generated.txt": "go generated[own][c]",
            "stop-generated.txt": "stop generated"
        });
    });

    test("conditions write no data table rows", async () => {
        // given
        const store = new InMemoryDataTableStore();
        const ctx = new ExecutionContext({[DATA_TABLE_STORE]: store});

        // when
        await run(new Guarded([new RecordWord("condition")], [new RecordWord("edit")]), [["a.txt", "a"]], ctx);

        // then
        expect(store.getRows(Word.dataTable.descriptor.name)).toEqual([new Word("edit")]);
    });

    test("a recipe reference runs its local visitor", async () => {
        const reference = new RecipeRef("org.openrewrite.test.Reference", {}, await new Contains("go").editor());

        const result = await run(new Guarded([reference]), [["go.txt", "go"], ["stop.txt", "stop"]]);

        expect(result).toEqual({"go.txt": "go[own]", "stop.txt": "stop"});
    });

    test("decisions do not carry over to the next run", async () => {
        // given
        const recipe = new Guarded([new Contains("go")], [new Tag("a")]);
        const [goes] = await parse(["file.txt", "go"]);
        await run(recipe, [goes]);

        // when
        const result = await run(recipe, [{...goes, text: "stop"} as PlainText]);

        // then
        expect(result).toEqual({"file.txt": "stop"});
    });

    test("the descriptor lists the conditions", async () => {
        // given
        const recipe = new Guarded([new Contains("go"), new RecipeRef("org.openrewrite.FindSourceFiles", {filePattern: "*.txt"})]);

        // when
        const descriptor = await recipe.descriptor();

        // then
        expect(descriptor.preconditions.map(p => p.name))
            .toEqual(["org.openrewrite.test.contains", "org.openrewrite.FindSourceFiles"]);
        expect(descriptor.preconditions[1].options).toEqual([
            {name: "filePattern", value: "*.txt", displayName: "filePattern", description: "", required: false}
        ]);
    });
});
