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
import {ExecutionContext, foundSearchResult, Option, Recipe, TreeVisitor} from "@openrewrite/rewrite";
import {JavaScriptVisitor} from "@openrewrite/rewrite/javascript";
import {J} from "@openrewrite/rewrite/java";

export class FindIdentifier extends Recipe {
    name = "org.openrewrite.example.javascript.find-identifier"
    displayName = "Find identifier";
    description = "Find matching identifiers in JavaScript code.";

    @Option({
        displayName: "Identifier",
        description: "The identifier to find."
    })
    identifier!: string;

    constructor(options: { identifier: string }) {
        super(options);
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        const identifier = this.identifier;
        return new class extends JavaScriptVisitor<ExecutionContext> {
            protected async visitIdentifier(ident: J.Identifier, ctx: ExecutionContext): Promise<J | undefined> {
                if (ident.simpleName === identifier) {
                    return foundSearchResult(ident);
                }
                return super.visitIdentifier(ident, ctx);
            }
        };
    }
}
