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
import {ReplacedText} from "./replaced-text";
import {ExecutionContext, Option, Recipe, Transient, TreeVisitor} from "@openrewrite/rewrite";
import {PlainText, PlainTextVisitor} from "@openrewrite/rewrite/text";

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

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
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
