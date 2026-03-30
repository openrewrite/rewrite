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

export class MarkPrimitiveTypes extends Recipe {
    name = "org.openrewrite.example.javascript.mark-primitive-types"
    displayName = "Mark primitive types";
    description = "Mark literals with their primitive type information to verify RPC type codec functionality.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JavaScriptVisitor<ExecutionContext> {
            protected async visitLiteral(literal: J.Literal, ctx: ExecutionContext): Promise<J.Literal> {
                const visited = await super.visitLiteral(literal, ctx) as J.Literal;
                if (literal.type && Type.isPrimitive(literal.type)) {
                    const typeDescription = literal.type.keyword || 'None';
                    return foundSearchResult(visited, typeDescription);
                }
                return visited;
            }
        };
    }
}