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
import {ExecutionContext, ScanningRecipe, TreeVisitor} from "@openrewrite/rewrite";
import {PlainText, PlainTextVisitor} from "@openrewrite/rewrite/text";

/**
 * A test recipe that uses the accumulator in the editor phase.
 * During scanning, it counts the number of text files.
 * During editing, it appends the count to each text file.
 */
export class ScanningEditor extends ScanningRecipe<{ count: number }> {
    name = "org.openrewrite.example.text.scanning-editor";
    displayName = "Scanning editor";
    description = "Appends the count of text files to each file.";

    initialValue(_ctx: ExecutionContext): { count: number } {
        return {count: 0};
    }

    async scanner(acc: { count: number }): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends PlainTextVisitor<ExecutionContext> {
            async visitText(text: PlainText, _ctx: ExecutionContext): Promise<PlainText | undefined> {
                acc.count++;
                return text;
            }
        };
    }

    async editorWithData(acc: { count: number }): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends PlainTextVisitor<ExecutionContext> {
            async visitText(text: PlainText, _ctx: ExecutionContext): Promise<PlainText | undefined> {
                // Append the count from scanning to the text
                return {
                    ...text,
                    text: text.text + ` (count: ${acc.count})`
                };
            }
        };
    }
}
