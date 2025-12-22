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
import {produce} from "immer";

export class ModernizeOctalEscapeSequences extends Recipe {
    name = "org.openrewrite.javascript.migrate.es6.modernize-octal-escape-sequences";
    displayName = "Modernize octal escape sequences";
    description = "Convert old-style octal escape sequences (e.g., `\\0`, `\\123`) to modern hex escape sequences (e.g., `\\x00`, `\\x53`) or Unicode escape sequences (e.g., `\\u0000`, `\\u0053`).";

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
                // Only process string literals
                if (typeof literal.value !== 'string') {
                    return literal;
                }

                const valueSource = literal.valueSource;
                if (!valueSource) {
                    return literal;
                }

                // Check if this string contains octal escape sequences
                // Octal escape sequences: \0 through \377 (1-3 octal digits)
                // Pattern: backslash followed by 1-3 octal digits (0-7)
                // We need to be careful not to match already escaped sequences
                const octalEscapePattern = /\\([0-7]{1,3})/g;

                let hasOctalEscapes = false;
                let modernized = valueSource;

                // Replace all octal escape sequences with hex or Unicode equivalents
                modernized = valueSource.replace(octalEscapePattern, (match, octalDigits) => {
                    hasOctalEscapes = true;
                    // Convert octal string to decimal number
                    const decimalValue = parseInt(octalDigits, 8);

                    if (useUnicode) {
                        // Convert to Unicode escape sequence (4 hex digits, zero-padded)
                        return `\\u${decimalValue.toString(16).padStart(4, '0')}`;
                    } else {
                        // Convert to hex escape sequence (2 hex digits, zero-padded)
                        return `\\x${decimalValue.toString(16).padStart(2, '0')}`;
                    }
                });

                if (hasOctalEscapes) {
                    return produce(literal, draft => {
                        draft.valueSource = modernized;
                    });
                }

                return literal;
            }
        }
    }
}
