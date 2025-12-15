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
import {createDraft, finishDraft} from "immer";
import {ExecutionContext, Option, Recipe, TreeVisitor} from "@openrewrite/rewrite";
import {JavaScriptVisitor} from "@openrewrite/rewrite/javascript";
import {J, Type} from "@openrewrite/rewrite/java";
import {emptyMarkers, randomId} from "@openrewrite/rewrite";
import {singleSpace} from "@openrewrite/rewrite/java";

export class ReplaceAssignment extends Recipe {
    name = "org.openrewrite.example.javascript.replace-assignment"
    displayName = "Replace assignment with env var value";
    description = "Replaces the assignment of a variable with the value of an environment variable.";

    @Option({
        displayName: "Environment Variable Name",
        description: "The name of the environment variable whose value will replace the variable assignment.",
    })
    variable!: string;

    constructor(options: { variable: string }) {
        super(options);
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        const envVar = this.variable;
        const envVarValue = "'" + process.env[envVar] + "'";
        return new class extends JavaScriptVisitor<ExecutionContext> {
            protected async visitVariable(variable: J.VariableDeclarations.NamedVariable, c: ExecutionContext): Promise<J | undefined> {
                if ((variable.initializer!.element as J.Literal).valueSource === envVarValue) {
                    return super.visitVariable(variable, c);
                }
                let draft = createDraft(variable);
                const theLiteral: J.Literal = {
                    kind: J.Kind.Literal,
                    id: randomId(),
                    prefix: singleSpace,
                    markers: emptyMarkers,
                    valueSource: envVarValue,
                    type: Type.Primitive.String,
                };
                const leftPaddedLiteral: J.LeftPadded<J.Literal> = {
                    kind: J.Kind.LeftPadded,
                    markers: emptyMarkers,
                    element: theLiteral,
                    before: singleSpace,
                }
                draft.initializer = createDraft(leftPaddedLiteral);
                return finishDraft(draft);
            }
        }
    }
}
