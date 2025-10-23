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

import {Recipe} from "../../../recipe";
import {TreeVisitor} from "../../../visitor";
import {ExecutionContext} from "../../../execution";
import {JavaScriptVisitor} from "../../visitor";
import {J} from "../../../java";
import {produce} from "immer";

export class ModernizeOctalLiterals extends Recipe {
    name = "org.openrewrite.javascript.migrate.es6.modernize-octal-literals";
    displayName = "Modernize octal literals";
    description = "Convert old-style octal literals (e.g., `0777`) to modern ES6 syntax (e.g., `0o777`).";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JavaScriptVisitor<ExecutionContext> {

            protected async visitLiteral(literal: J.Literal, _ctx: ExecutionContext): Promise<J | undefined> {
                // Only process numeric literals
                if (typeof literal.value !== 'number') {
                    return literal;
                }

                const valueSource = literal.valueSource;
                if (!valueSource) {
                    return literal;
                }

                // Check if this is an old-style octal literal
                // Old-style: starts with 0 followed by one or more octal digits (0-7)
                // We need to ensure it's not: 0, 0x (hex), 0b (binary), 0o (modern octal), or decimal with decimal point
                const oldStyleOctalPattern = /^0([0-7]+)$/;
                const match = valueSource.match(oldStyleOctalPattern);

                if (match) {
                    // Convert to modern ES6 octal syntax
                    const octalDigits = match[1];
                    const modernOctal = `0o${octalDigits}`;

                    return produce(literal, draft => {
                        draft.valueSource = modernOctal;
                    });
                }

                return literal;
            }
        }
    }
}
