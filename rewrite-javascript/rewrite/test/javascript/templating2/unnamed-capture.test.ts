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
import {capture, match, template} from "../../../src/javascript/templating2";
import {createDraft, produce} from "immer";
import {JavaCoordinates} from "../../../src/javascript/templating";
import Mode = JavaCoordinates.Mode;

describe('unnamed capture', () => {
    const spec = new RecipeSpec();

    test('extract parts of a binary expression using unnamed captures', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                if (binary.operator.element === J.Binary.Type.Addition) {
                    // Create capture objects without explicit names
                    const {left, right} = {left: capture(), right: capture()};

                    // Create a pattern that matches "a + b" using the capture objects
                    const pattern = match`${left} + ${right}`;
                    const matcher = pattern.against(binary);

                    console.log("[DEBUG_LOG] Left capture name:", left.name);
                    console.log("[DEBUG_LOG] Right capture name:", right.name);
                    console.log("[DEBUG_LOG] Binary operator:", binary.operator.element);

                    const matches = await matcher.matches();
                    console.log("[DEBUG_LOG] Pattern matches:", matches);

                    if (matches) {
                        // Extract the captured parts
                        const leftValue = matcher.get(left);
                        const rightValue = matcher.get(right);

                        console.log("[DEBUG_LOG] Left capture name:", left.name);
                        console.log("[DEBUG_LOG] Right capture name:", right.name);
                        console.log("[DEBUG_LOG] Left value:", leftValue ? "defined" : "undefined");
                        console.log("[DEBUG_LOG] Right value:", rightValue ? "defined" : "undefined");

                        // Create a new binary expression with the swapped operands
                        return produce(binary, draft => {
                            draft.left = createDraft(rightValue!);
                            draft.prefix = binary.left.prefix;
                            draft.right = createDraft(leftValue!);
                            draft.right.prefix = binary.right.prefix;
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
