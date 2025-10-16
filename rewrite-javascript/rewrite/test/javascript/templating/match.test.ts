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
import {
    capture,
    JavaScriptParser,
    JavaScriptVisitor,
    pattern,
    rewrite,
    template,
    typescript
} from "../../../src/javascript";
import {J} from "../../../src/java";
import {createDraft, produce} from "immer";

describe('match extraction', () => {
    const spec = new RecipeSpec();

    test('extract parts of a binary expression using string names', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                if (binary.operator.element === J.Binary.Type.Addition) {

                    // Create a pattern that matches "a + b"
                    const m = await pattern`${{name: "left"}} + ${"right"}`.match(binary);
                    if (m) {
                        // Extract the captured parts
                        // Create a new binary expression with the swapped operands
                        return produce(binary, draft => {
                            draft.left = createDraft((m.get("right"))!);
                            draft.prefix = binary.left.prefix;
                            draft.right = createDraft((m.get("left"))!);
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

    test('extract parts of a binary expression using capture objects', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {

            override async visitBinary(binary: J.Binary, _p: any): Promise<J | undefined> {
                // Create capture objects
                const left = capture(), right = capture();

                // Create a pattern that matches "a + b" using the capture objects
                const m = await pattern`${left} + ${right}`.match(binary);
                if (m) {
                    return await template`${right} + ${left}`.apply(this.cursor, binary, m);
                }
                return binary;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript('const result = 1 + 2;', 'const result = 2 + 1;'),
        );
    });

    test('extract parts of a binary expression using replace function', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {

                const swapOperands = rewrite(() => ({
                        before: pattern`${capture('left')} + ${capture('right')}`,
                        after: template`${capture('right')} + ${capture('left')}`
                    })
                );
                return await swapOperands.tryOn(this.cursor, binary);
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript('const result = 1 + 2;', 'const result = 2 + 1;'),
        );
    });

    test('extract parts of a binary expression using unnamed captures', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                if (binary.operator.element === J.Binary.Type.Addition) {
                    // Create capture objects without explicit names
                    const {left, right} = {left: capture(), right: capture()};

                    // Create a pattern that matches "a + b" using the capture objects
                    const m = await pattern`${left} + ${right}`.match(binary);
                    if (m) {
                        // Extract the captured parts
                        const leftValue = m.get(left);
                        const rightValue = m.get(right);

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

    test('extract parts using inline named captures', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                if (binary.operator.element === J.Binary.Type.Addition) {
                    // Use inline named captures
                    const m = await pattern`${capture('left')} + ${capture('right')}`.match(binary);
                    if (m) {
                        // Can retrieve by string name
                        return await template`${capture('right')} + ${capture('left')}`.apply(this.cursor, binary, m);
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

    test('pattern with non-existent dependency fails', async () => {
        // Verify that specifying a non-existent package causes npm install to fail
        const nonExistentPackage = 'this-package-definitely-does-not-exist-12345';

        const pat = pattern`${capture('left')} + ${capture('right')}`
            .configure({
                imports: [`import { SomeType } from "${nonExistentPackage}"`],
                dependencies: { [nonExistentPackage]: '^1.0.0' }
            });

        // Create dummy code to trigger pattern parsing (which requires workspace creation)
        const testCode = 'const result = 1 + 2;';
        const parser = new JavaScriptParser();
        const parseGen = parser.parse({text: testCode, sourcePath: 'test.ts'});
        const cu = (await parseGen.next()).value;

        // Try to match - this should fail because npm install will fail for non-existent package
        await expect(async () => {
            await (new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, _p: any): Promise<J | undefined> {
                    // This should throw when trying to create workspace
                    await pat.match(binary);
                    return binary;
                }
            }).visit(cu, undefined);
        }).rejects.toThrow(/Failed to create dependency workspace/);
    });
});
