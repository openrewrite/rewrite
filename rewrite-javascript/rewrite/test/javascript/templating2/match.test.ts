/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {JavaScriptVisitor, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";
import {capture, match} from "../../../src/javascript/templating2";
import {produce} from "immer";

describe('match extraction', () => {
    const spec = new RecipeSpec();

    test('extract parts of a binary expression', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                if (binary.operator.element === J.Binary.Type.Addition) {

                    // Create a pattern that matches "a + b"
                    const pattern = match`${capture('a')} + ${capture('b')}`;
                    const matcher = pattern.against(binary);
                    if (await matcher.matches()) {
                        // Extract the captured parts
                        const a = matcher.get('a');
                        const b = matcher.get('b');

                        // First, create deep copies of the operands to avoid modifying the originals
                        const newLeft = produce({} as J, draft => {
                            Object.assign(draft, b);
                            // Use the prefix from the original left operand
                            draft.prefix = binary.left.prefix;
                        });

                        const newRight = produce({} as J, draft => {
                            Object.assign(draft, a);
                            // Use the prefix from the original right operand
                            draft.prefix = binary.right.prefix;
                        });

                        // Create a new binary expression with the swapped operands
                        return produce(binary, draft => {
                            draft.left = newLeft;
                            draft.right = newRight;
                        });
                    }
                }
                return binary;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript('const result = 1 + 2;', 'const result = 2 + 1;'),
        );
    });
});
