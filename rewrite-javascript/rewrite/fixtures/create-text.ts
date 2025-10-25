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
import {ExecutionContext, Option, ScanningRecipe, SourceFile} from "@openrewrite/rewrite";
import {PlainTextParser} from "@openrewrite/rewrite/text";

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
