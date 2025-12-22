// noinspection JSUnusedLocalSymbols,TypeScriptCheckImport,ES6UnusedImports,PointlessBooleanExpressionJS,UnreachableCodeJS,InfiniteLoopJS

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

/**
 * Tests for WrappingAndBracesVisitor - handles wrapping and braces formatting.
 *
 * GUIDELINES FOR TEST AUTHORS:
 *
 * 1. COMPACT TESTS: Prefer fewer, more comprehensive tests over many small focused tests.
 *    Since test output shows the full source diff, it's more efficient to combine related
 *    wrapping and braces scenarios into a single test with multiple variations in the source text.
 *
 * 2. SCOPE: This file should contain tests specific to WrappingAndBracesVisitor behavior and
 *    WrappingAndBracesStyle settings. For full formatter integration tests, use format.test.ts.
 */

import {fromVisitor, RecipeSpec} from "../../../src/test";
import {IntelliJ, typescript, WrappingAndBracesStyle, WrappingAndBracesVisitor} from "../../../src/javascript";
import {Draft, produce} from "immer";
import {Style} from "../../../src";

type StyleCustomizer<T extends Style> = (draft: Draft<T>) => void;

function wrappingAndBraces(customizer?: StyleCustomizer<WrappingAndBracesStyle>): WrappingAndBracesStyle {
    return customizer
        ? produce(IntelliJ.TypeScript.wrappingAndBraces(), draft => customizer(draft))
        : IntelliJ.TypeScript.wrappingAndBraces();
}

describe('WrappingAndBracesVisitor', () => {
    const spec = new RecipeSpec()

    describe('keepWhenReformatting.simpleBlocksInOneLine', () => {
        test('false (default) - empty blocks get newline', () => {
            spec.recipe = fromVisitor(new WrappingAndBracesVisitor(wrappingAndBraces(draft => {
                draft.keepWhenReformatting.simpleBlocksInOneLine = false;
            })));
            return spec.rewriteRun(
                // @formatter:off
                //language=typescript
                typescript(`
                    class A {}
                    if (true) {}
                    while (true) {}
                    for (let i = 0; i < 10; i++) {}
                    try {} catch (e) {}
                    `,
                    `
                    class A {
                    }
                    if (true) {
                    }
                    while (true) {
                    }
                    for (let i = 0; i < 10; i++) {
                    }
                    try {
                    } catch (e) {
                    }
                    `),
                // @formatter:on
            )
        });

        test('true - empty blocks stay on one line', () => {
            spec.recipe = fromVisitor(new WrappingAndBracesVisitor(wrappingAndBraces(draft => {
                draft.keepWhenReformatting.simpleBlocksInOneLine = true;
            })));
            return spec.rewriteRun(
                // @formatter:off
                //language=typescript
                typescript(`
                    if (true) {
                    }
                    while (true) {
                    }
                    `,
                    `
                    if (true) {}
                    while (true) {}
                    `),
                // @formatter:on
            )
        });
    });

    describe('keepWhenReformatting.simpleMethodsInOneLine', () => {
        test('false (default) - empty method/function bodies get newline', () => {
            spec.recipe = fromVisitor(new WrappingAndBracesVisitor(wrappingAndBraces(draft => {
                draft.keepWhenReformatting.simpleMethodsInOneLine = false;
            })));
            return spec.rewriteRun(
                // @formatter:off
                //language=typescript
                typescript(`
                    function foo() {}
                    const bar = () => {};
                    `,
                    `
                    function foo() {
                    }
                    const bar = () => {
                    };
                    `),
                // @formatter:on
            )
        });

        test('true - empty method/function bodies stay on one line', () => {
            spec.recipe = fromVisitor(new WrappingAndBracesVisitor(wrappingAndBraces(draft => {
                draft.keepWhenReformatting.simpleMethodsInOneLine = true;
            })));
            return spec.rewriteRun(
                // @formatter:off
                //language=typescript
                typescript(`
                    function foo() {
                    }
                    const bar = () => {
                    };
                    class A {
                        method() {
                        }
                    }
                    `,
                    `
                    function foo() {}
                    const bar = () => {};
                    class A {
                        method() {}
                    }
                    `),
                // @formatter:on
            )
        });
    });

    describe('combined options', () => {
        test('simpleBlocksInOneLine=true, simpleMethodsInOneLine=false', () => {
            spec.recipe = fromVisitor(new WrappingAndBracesVisitor(wrappingAndBraces(draft => {
                draft.keepWhenReformatting.simpleBlocksInOneLine = true;
                draft.keepWhenReformatting.simpleMethodsInOneLine = false;
            })));
            return spec.rewriteRun(
                // @formatter:off
                //language=typescript
                typescript(`
                    if (true) {
                    }
                    function foo() {}
                    `,
                    `
                    if (true) {}
                    function foo() {
                    }
                    `),
                // @formatter:on
            )
        });

        test('simpleBlocksInOneLine=false, simpleMethodsInOneLine=true', () => {
            spec.recipe = fromVisitor(new WrappingAndBracesVisitor(wrappingAndBraces(draft => {
                draft.keepWhenReformatting.simpleBlocksInOneLine = false;
                draft.keepWhenReformatting.simpleMethodsInOneLine = true;
            })));
            return spec.rewriteRun(
                // @formatter:off
                //language=typescript
                typescript(`
                    if (true) {}
                    function foo() {
                    }
                    `,
                    `
                    if (true) {
                    }
                    function foo() {}
                    `),
                // @formatter:on
            )
        });
    });

    describe('object literals exemption', () => {
        test('empty object literals always formatted as {} regardless of simpleBlocksInOneLine=false', () => {
            spec.recipe = fromVisitor(new WrappingAndBracesVisitor(wrappingAndBraces(draft => {
                draft.keepWhenReformatting.simpleBlocksInOneLine = false;
            })));
            return spec.rewriteRun(
                // @formatter:off
                //language=typescript
                typescript(`
                    const a = {};
                    const b = { x: 1 };
                    const c = {
                        y: 2
                    };
                    `),
                // Object literals with content are preserved, but empty ones stay on one line
                // @formatter:on
            )
        });

        test('empty object literals with newlines are collapsed to single line', () => {
            spec.recipe = fromVisitor(new WrappingAndBracesVisitor(wrappingAndBraces(draft => {
                draft.keepWhenReformatting.simpleBlocksInOneLine = false;
            })));
            return spec.rewriteRun(
                // @formatter:off
                //language=typescript
                typescript(`
                    const a = {
                    };
                    `,
                    `
                    const a = {};
                    `),
                // @formatter:on
            )
        });

        test('empty object literals collapsed regardless of simpleBlocksInOneLine=true', () => {
            spec.recipe = fromVisitor(new WrappingAndBracesVisitor(wrappingAndBraces(draft => {
                draft.keepWhenReformatting.simpleBlocksInOneLine = true;
            })));
            return spec.rewriteRun(
                // @formatter:off
                //language=typescript
                typescript(`
                    const a = {
                    };
                    `,
                    `
                    const a = {};
                    `),
                // @formatter:on
            )
        });
    });

    describe('elseOnNewLine', () => {
        // Note: WrappingAndBracesVisitor only handles the newline, not the space before 'else'.
        // SpacesVisitor handles the space. When testing in isolation, we see the raw behavior.
        test('false (default) - else on same line', () => {
            spec.recipe = fromVisitor(new WrappingAndBracesVisitor(wrappingAndBraces(draft => {
                draft.ifStatement.elseOnNewLine = false;
            })));
            return spec.rewriteRun(
                // @formatter:off
                //language=typescript
                typescript(`
                    if (true) {
                        console.log("a");
                    }
                    else {
                        console.log("b");
                    }
                    `,
                    // The newline is removed, leaving no whitespace before else
                    `
                    if (true) {
                        console.log("a");
                    }else {
                        console.log("b");
                    }
                    `),
                // @formatter:on
            )
        });

        test('true - else on new line', () => {
            spec.recipe = fromVisitor(new WrappingAndBracesVisitor(wrappingAndBraces(draft => {
                draft.ifStatement.elseOnNewLine = true;
            })));
            return spec.rewriteRun(
                // @formatter:off
                //language=typescript
                typescript(`
                    if (true) {
                        console.log("a");
                    } else {
                        console.log("b");
                    }
                    `,
                    // The newline is added, preserving the space after }
                    `
                    if (true) {
                        console.log("a");
                    }
                     else {
                        console.log("b");
                    }
                    `),
                // @formatter:on
            )
        });
    });
});
