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
import {ExecutionContext, foundSearchResult, Recipe, TreeVisitor} from "@openrewrite/rewrite";
import {JavaScriptVisitor} from "@openrewrite/rewrite/javascript";
import {J, Type} from "@openrewrite/rewrite/java";

export class MarkClassTypes extends Recipe {
    name = "org.openrewrite.example.javascript.mark-class-types"
    displayName = "Mark class types";
    description = "Mark identifiers with their class type information to verify RPC type codec functionality.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JavaScriptVisitor<ExecutionContext> {
            protected async visitIdentifier(ident: J.Identifier, ctx: ExecutionContext): Promise<J.Identifier> {
                const visited = await super.visitIdentifier(ident, ctx) as J.Identifier;

                // Mark lodash identifier with its type
                if (ident.simpleName === '_' && ident.type && Type.isClass(ident.type)) {
                    for (const member of ident.type.members) {
                        if (member.owner !== ident.type) {
                            throw new Error("Member owner does not match class that owns the member");
                        }
                    }

                    // Access the fullyQualifiedName to prove the circular reference handling works
                    // The type should have members that reference back to owner, but asRef() handles this
                    return foundSearchResult(visited, ident.type.fullyQualifiedName);
                }

                return visited;
            }
        };
    }
}
