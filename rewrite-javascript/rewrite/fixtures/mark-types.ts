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

export class MarkTypes extends Recipe {
    name = "org.openrewrite.example.javascript.mark-types"
    displayName = "Mark types";
    description = "Mark nodes with their type information to verify RPC type codec functionality.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JavaScriptVisitor<ExecutionContext> {
            protected async visitLiteral(literal: J.Literal, ctx: ExecutionContext): Promise<J.Literal> {
                const visited = await super.visitLiteral(literal, ctx) as J.Literal;
                if (literal.type) {
                    const typeDescription = Type.isPrimitive(literal.type) 
                        ? (literal.type.keyword || 'None')
                        : 'unknown';
                    return foundSearchResult(visited, typeDescription);
                }
                return visited;
            }

            protected async visitClassDeclaration(classDecl: J.ClassDeclaration, ctx: ExecutionContext): Promise<J> {
                const visited = await super.visitClassDeclaration(classDecl, ctx) as J.ClassDeclaration;
                if (classDecl.name?.type) {
                    // Avoid accessing complex properties that might have circular references
                    // Just use the simple name which should be safe
                    return foundSearchResult(visited, classDecl.name.simpleName);
                }
                return visited;
            }
        };
    }
}