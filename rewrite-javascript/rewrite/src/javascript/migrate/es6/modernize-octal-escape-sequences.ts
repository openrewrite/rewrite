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

import {Option, Recipe} from "../../../recipe";
import {TreeVisitor} from "../../../visitor";
import {ExecutionContext} from "../../../execution";
import {JavaScriptVisitor} from "../../visitor";
import {J} from "../../../java";
import {JS} from "../../tree";
import {create as produce} from "mutative";

export class ModernizeOctalEscapeSequences extends Recipe {
    name = "org.openrewrite.javascript.migrate.es6.modernize-octal-escape-sequences";
    displayName = "Modernize octal escape sequences";
    description = "Convert legacy octal escape sequences in string literals (e.g., `\\1`, `\\123`) to hex escape sequences (e.g., `\\x01`, `\\x53`) or Unicode escape sequences (e.g., `\\u0001`, `\\u0053`). The `\\0` escape is left alone unless a digit follows it.";

    @Option({
        displayName: "Use Unicode escapes",
        description: "Use Unicode escape sequences (`\\uXXXX`) instead of hex escape sequences (`\\xXX`). Default is `false`.",
        required: false,
        example: "true"
    })
    useUnicodeEscapes: boolean;

    constructor(options?: { useUnicodeEscapes?: boolean }) {
        super(options);
        this.useUnicodeEscapes ??= false;
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        const useUnicode = this.useUnicodeEscapes;
        return new class extends JavaScriptVisitor<ExecutionContext> {

            protected async visitLiteral(literal: J.Literal, _ctx: ExecutionContext): Promise<J | undefined> {
                const valueSource = literal.valueSource;
                if (!valueSource || (valueSource[0] !== '"' && valueSource[0] !== "'") ||
                    this.cursor.parentTree()?.value.kind === JS.Kind.JsxAttribute) {
                    return literal;
                }

                const modernized = valueSource.replace(
                    /\\([0-3][0-7]{0,2}|[4-7][0-7]?|[\s\S])/g,
                    (escape: string, body: string, offset: number) => {
                        if (!/^[0-7]/.test(body) || (body === '0' && !/[0-9]/.test(valueSource[offset + 2] ?? ''))) {
                            return escape;
                        }
                        const code = parseInt(body, 8);
                        return useUnicode ?
                            `\\u${code.toString(16).padStart(4, '0')}` :
                            `\\x${code.toString(16).padStart(2, '0')}`;
                    });

                return modernized === valueSource ? literal : produce(literal, draft => {
                    draft.valueSource = modernized;
                });
            }
        }
    }
}
