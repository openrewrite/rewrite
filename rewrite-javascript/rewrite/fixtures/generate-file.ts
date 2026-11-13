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
import {ExecutionContext, Option, ScanningRecipe, SourceFile, TreeVisitor} from "@openrewrite/rewrite";
import {JavaScriptParser} from "@openrewrite/rewrite/javascript";
import {JsonParser} from "@openrewrite/rewrite/json";

export class GenerateFile extends ScanningRecipe<{ exists: boolean }> {
    name = "org.openrewrite.example.generate-file";
    displayName = "Generate a JavaScript or JSON file";
    description = "Generate a JavaScript or JSON file, parsed according to its extension, unless it already exists.";

    @Option({
        displayName: "Text",
        description: "The content of the file to create."
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

    initialValue(_ctx: ExecutionContext): { exists: boolean } {
        return {exists: false};
    }

    async scanner(acc: { exists: boolean }): Promise<TreeVisitor<any, ExecutionContext>> {
        const sourcePath = this.sourcePath;
        return new class extends TreeVisitor<any, ExecutionContext> {
            protected async preVisit(tree: any, _ctx: ExecutionContext): Promise<any> {
                if (tree.sourcePath === sourcePath) {
                    acc.exists = true;
                }
                this.stopAfterPreVisit();
                return tree;
            }
        };
    }

    async generate(acc: { exists: boolean }, _ctx: ExecutionContext): Promise<SourceFile[]> {
        if (acc.exists) {
            return [];
        }
        const input = {text: this.text, sourcePath: this.sourcePath};
        if (this.sourcePath.endsWith(".json")) {
            const results: SourceFile[] = [];
            for await (const file of new JsonParser().parse(input)) {
                results.push(file);
            }
            return results;
        }
        return [await new JavaScriptParser().parseOnly(input)];
    }
}
