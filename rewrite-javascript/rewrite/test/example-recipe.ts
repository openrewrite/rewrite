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
import {
    Column,
    DataTable,
    ExecutionContext,
    Option,
    Recipe,
    RecipeRegistry,
    ScanningRecipe,
    SourceFile,
    Transient,
    TreeVisitor
} from "../src";
import {PlainText, PlainTextParser, PlainTextVisitor} from "../src/text";

export function activate(registry: RecipeRegistry) {
    registry.register(ChangeText);
    registry.register(CreateText);
    registry.register(RecipeWithRecipeList);
}

export class ReplacedText {
    @Column({
        displayName: "Source Path",
        description: "The path of the file that was changed."
    })
    readonly sourcePath: string

    @Column({
        displayName: "Text",
        description: "The text that was replaced."
    })
    readonly text: string

    constructor(sourcePath: string, text: string) {
        this.sourcePath = sourcePath;
        this.text = text;
    }

    static dataTable = new DataTable<ReplacedText>(
        "org.openrewrite.text.replaced-text",
        "Replaced text",
        "Text that was replaced.",
        ReplacedText
    );
}

export class CreateText extends ScanningRecipe<{ exists: boolean }> {
    name = "org.openrewrite.text.create-text";
    displayName = "Create text file";
    description = "Create a new text file.";

    @Option({
        displayName: "Text",
        description: "Text to change to"
    })
    text!: string

    @Option({
        displayName: "Source path",
        description: "The source path of the file to create."
    })
    sourcePath!: string

    constructor(options: { text: string, sourcePath: string }) {
        super(options);
    }

    initialValue(_ctx: ExecutionContext): { exists: boolean; } {
        return {exists: false};
    }

    async generate(acc: { exists: boolean }, _ctx: ExecutionContext): Promise<SourceFile[]> {
        if (acc.exists) {
            return [];
        }
        return new PlainTextParser().parse({text: this.text, sourcePath: this.sourcePath});
    }
}

export class ChangeText extends Recipe {
    name = "org.openrewrite.text.change-text"
    displayName = "Change text";
    description = "Change the text of a file.";

    @Option({
        displayName: "Text",
        description: "Text to change to"
    })
    text!: string;

    constructor(options: { text: string }) {
        super(options);
    }

    @Transient
    replacedText = ReplacedText.dataTable;

    instanceName(): string {
        return `Change text to '${this.text}'`;
    }

    get editor(): TreeVisitor<any, ExecutionContext> {
        const toText = this.text;
        const replacedText = this.replacedText;
        return new class extends PlainTextVisitor<ExecutionContext> {
            visitText(text: PlainText, ctx: ExecutionContext): Promise<PlainText> {
                return this.produceTree(text, ctx, draft => {
                    if (draft.text != toText) {
                        replacedText.insertRow(ctx, new ReplacedText(text.sourcePath, draft.text));
                    }
                    draft.text = toText;
                })
            }
        }
    }
}

export class RecipeWithRecipeList extends Recipe {
    name = "org.openrewrite.text.with-recipe-list"
    displayName = "A recipe that has a recipe list";
    description = "To verify that it is possible for a recipe list to be called over RPC.";

    async recipeList() {
        return [new ChangeText({text: "hello"})];
    }
}
