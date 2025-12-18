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
 * Tests for AutoformatVisitor with Prettier integration.
 */

import {fromVisitor, RecipeSpec} from "../../../src/test";
import {
    autoFormat,
    AutoformatVisitor,
    JavaScriptVisitor,
    typescript
} from "../../../src/javascript";

describe('AutoformatVisitor with Prettier', () => {
    const prettierSpec = new RecipeSpec()
    // Use Prettier for these tests
    prettierSpec.recipe = fromVisitor(new AutoformatVisitor(undefined, undefined, { usePrettier: true }));

    test('formats basic code with Prettier', () => {
        return prettierSpec.rewriteRun(
            // @formatter:off
            //language=typescript
            // Note: whitespace reconciler only copies whitespace, not tokens like semicolons
            // Prettier adds trailing newline
            typescript(`const x=1+2`,
                `const x = 1 + 2

`)
            // @formatter:on
        )
    });

    test('subtree formatting with Prettier preserves indentation', async () => {
        // This test verifies that subtree formatting (triggered via maybeAutoFormat)
        // works correctly with the Prettier pruning optimization
        const visitor = new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: any, p: any): Promise<any> {
                // Only format the info() call to simulate template replacement
                if (methodInvocation.name?.simpleName === 'info') {
                    return await autoFormat(methodInvocation, p, undefined, this.cursor.parent, undefined, { usePrettier: true });
                }
                return super.visitMethodInvocation(methodInvocation, p);
            }
        }();

        const testSpec = new RecipeSpec();
        testSpec.recipe = fromVisitor(visitor);

        // Test with multiple statements - the pruning should handle this correctly
        return testSpec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `
                function first() {
                    console.log("first");
                }
                function second() {
                    logger.info("second");
                }
                function third() {
                    console.log("third");
                }
                `
            )
            // @formatter:on
        )
    });

    test('subtree formatting in nested block with Prettier', async () => {
        // This tests the pruning in nested blocks
        const visitor = new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: any, p: any): Promise<any> {
                if (methodInvocation.name?.simpleName === 'slice') {
                    return await autoFormat(methodInvocation, p, undefined, this.cursor.parent, undefined, { usePrettier: true });
                }
                return super.visitMethodInvocation(methodInvocation, p);
            }
        }();

        const testSpec = new RecipeSpec();
        testSpec.recipe = fromVisitor(visitor);

        return testSpec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `
                class MyClass {
                    method1() {
                        const a = 1;
                    }
                    method2() {
                        const b = arr.slice();
                    }
                    method3() {
                        const c = 3;
                    }
                }
                `
            )
            // @formatter:on
        )
    });

    test('formats multi-line code - Prettier may collapse short lines', () => {
        // Prettier collapses multi-line code to single line if it fits within printWidth
        return prettierSpec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `function foo(
    a:number,
    b:string
) {
    return a+b;
}`,
                `function foo(a: number, b: string) {
    return a + b;
}

`)
            // @formatter:on
        )
    });

    test('subtree formatting fixes spacing in function call', async () => {
        // Test scenario:
        // - Function with multiple statements
        // - Function call that isn't the first statement (console.log is 3rd statement)
        // - Format the method invocation to simulate template replacement
        // - Prettier fixes the spacing in just the formatted call, not surrounding statements
        const visitor = new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: any, p: any): Promise<any> {
                // Only format the console.log() call to simulate template replacement
                if (methodInvocation.name?.simpleName === 'log') {
                    return await autoFormat(methodInvocation, p, undefined, this.cursor.parent, undefined, { usePrettier: true });
                }
                return super.visitMethodInvocation(methodInvocation, p);
            }
        }();

        const testSpec = new RecipeSpec();
        testSpec.recipe = fromVisitor(visitor);

        return testSpec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `
                function process() {
                    const first = 1;
                    const second  = 2;
                    console.log("before",
                        "target"   , "after",
                    {key:"value"}  );
                    const third =3;
                }
                `,
                `
                function process() {
                    const first = 1;
                    const second  = 2;
                    console.log("before", "target", "after", { key: "value" });
                    const third =3;
                }
                `
            )
            // @formatter:on
        )
    });
});
