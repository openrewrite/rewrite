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
import {capture, JavaScriptVisitor, pattern, rewrite, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";
import {createDraft, produce} from "immer";

describe('unnamed capture', () => {
    const spec = new RecipeSpec();

    test('extract parts of a binary expression using unnamed captures', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                if (binary.operator.element === J.Binary.Type.Addition) {
                    // Create capture objects without explicit names
                    const {left, right} = {left: capture(), right: capture()};

                    // Create a pattern that matches "a + b" using the capture objects
                    const p = pattern`${left} + ${right}`;
                    const matcher = p.against(binary);

                    const matches = await matcher.matches();

                    if (matches) {
                        // Extract the captured parts
                        const leftValue = matcher.get(left);
                        const rightValue = matcher.get(right);

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

    test('more complex example', () => {

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitTernary(ternary: J.Ternary, p: any): Promise<J | undefined> {

                const {obj, defaultValue, property} = {obj: capture(), defaultValue: capture(), property: capture()};
                
                // Use the new cleaner API - matcher.applyTemplate()
                const result = await pattern`${obj} === null || ${obj} === undefined ? ${defaultValue} : ${obj}.${property}`
                    .against(ternary)
                    .replaceWith(template`${obj}?.${property} ?? ${defaultValue}`, this.cursor);
                
                return result || await super.visitTernary(ternary, p);
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(`
                function getName(user) {
                    return user === null || user === undefined ? "default" : user.name;
                }
            `, `
                function getName(user) {
                    return user?.name ?? "default";
                }
            `),
        );
    });

    test('more complex example using replace', () => {

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitTernary(ternary: J.Ternary, p: any): Promise<J | undefined> {

                return await rewrite(() => {
                    const obj = capture(), defaultValue = capture(), property = capture();
                    return {
                        before: pattern`${obj} === null || ${obj} === undefined ? ${defaultValue} : ${obj}.${property}`,
                        after: template`${obj}?.${property} ?? ${defaultValue}`
                    };
                })
                        .tryOn(ternary, this.cursor) || super.visitTernary(ternary, p);
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(`
                function getName(user) {
                    return user === null || user === undefined ? "default" : user.name;
                }
            `, `
                function getName(user) {
                    return user?.name ?? "default";
                }
            `),
        );
    });
});
