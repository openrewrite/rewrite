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
    ExecutionContext, markers, Markers,
    Option, randomId,
    Recipe,
    RecipeRegistry,
    ScanningRecipe,
    SourceFile,
    Transient, Tree,
    TreeVisitor
} from "@openrewrite/rewrite";
import {PlainText, PlainTextParser, PlainTextVisitor} from "@openrewrite/rewrite/text";
import {Json, JsonVisitor} from "@openrewrite/rewrite/json";
import {createDraft, finishDraft} from "immer";
import {JavaScriptVisitor} from "@openrewrite/rewrite/javascript";
import {J} from "@openrewrite/rewrite/java";

export function activate(registry: RecipeRegistry) {
    registry.register(ChangeText);
    registry.register(CreateText);
    registry.register(ChangeVersion);
    registry.register(RecipeWithRecipeList);
    registry.register(ReplaceId);
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
    name = "org.openrewrite.example.text.create-text";
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

        const results: SourceFile[] = [];
        for await (const file of new PlainTextParser().parse({text: this.text, sourcePath: this.sourcePath})) {
            results.push(file);
        }
        return results;
    }
}

export class ChangeText extends Recipe {
    name = "org.openrewrite.example.text.change-text"
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

export class ChangeVersion extends Recipe {
    name = "org.openrewrite.example.npm.change-version";
    displayName = "Change version in `package.json`";
    description = "Change the version in both `package.json` and `package-lock.json`.";

    @Option({
        displayName: "Version",
        description: "The version to change to."
    })
    version!: string

    constructor(options: { version: string }) {
        super(options);
    }

    get editor(): JsonVisitor<ExecutionContext> {
        const v = this.version;
        return new class extends JsonVisitor<ExecutionContext> {
            protected async visitDocument(document: Json.Document, p: ExecutionContext): Promise<Json | undefined> {
                // Only visit package.json and package-lock.json files
                if (!(document.sourcePath.endsWith("package.json") || document.sourcePath.endsWith("package-lock.json"))) {
                    return document;
                }
                return super.visitDocument(document, p);
            }

            protected async visitMember(member: Json.Member, p: ExecutionContext): Promise<Json | undefined> {
                return this.produceJson<Json.Member>(await super.visitMember(member, p), p, draft => {
                    let key = member.key.element;
                    if (key.kind === Json.Kind.Literal && key.value === "version") {
                        if (draft.value.kind === Json.Kind.Literal) {
                            draft.value.value = v;
                            draft.value.source = `"${v}"`;
                        }
                    }
                });
            }
        };
    }
}

export class RecipeWithRecipeList extends Recipe {
    name = "org.openrewrite.example.text.with-recipe-list"
    displayName = "A recipe that has a recipe list";
    description = "To verify that it is possible for a recipe list to be called over RPC.";

    async recipeList() {
        return [new ChangeText({text: "hello"})];
    }
}

export class ReplaceId extends Recipe {
    name = "org.openrewrite.example.javascript.replace-id"
    displayName = "Replace IDs";
    description = "Replaces the ID of every `Tree` and `Marker` object in a JavaScript source.";

    get editor(): TreeVisitor<any, ExecutionContext> {
        return new class extends JavaScriptVisitor<ExecutionContext> {
            protected async preVisit(tree: J, _p: ExecutionContext): Promise<J | undefined> {
                let draft = createDraft(tree);
                draft.id = randomId();
                return finishDraft(draft);
            }

            protected async visitMarkers(markers: Markers, p: ExecutionContext): Promise<Markers> {
                let draft = createDraft(markers);
                draft.id = randomId();
                return super.visitMarkers(finishDraft(draft), p);
            }
        }
    }
}
