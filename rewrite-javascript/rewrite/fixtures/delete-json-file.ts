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
import {ExecutionContext, Option, Recipe} from "@openrewrite/rewrite";
import {Json, JsonVisitor} from "@openrewrite/rewrite/json";

export class DeleteJsonFile extends Recipe {
    name = "org.openrewrite.example.json.delete-file";
    displayName = "Delete a JSON file";
    description = "Delete the JSON file at the given source path.";

    @Option({
        displayName: "Source path",
        description: "The source path of the file to delete."
    })
    sourcePath!: string

    constructor(options: { sourcePath: string }) {
        super(options);
    }

    async editor(): Promise<JsonVisitor<ExecutionContext>> {
        const sourcePath = this.sourcePath;
        return new class extends JsonVisitor<ExecutionContext> {
            protected async visitDocument(document: Json.Document, _ctx: ExecutionContext): Promise<Json | undefined> {
                return document.sourcePath === sourcePath ? undefined : document;
            }
        };
    }
}
