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
    JavaScriptParser,
    JavaScriptVisitor,
    JS,
    typescript
} from "../../../src/javascript";
import {J, Statement} from "../../../src/java";
import {prettierFormat} from "../../../src/javascript/prettier-format";

describe('AutoformatVisitor with Prettier', () => {
    const prettierSpec = new RecipeSpec()
    // Use Prettier for these tests
    prettierSpec.recipe = fromVisitor(new AutoformatVisitor(undefined, undefined, { usePrettier: true }));

    test('formats basic code with Prettier', () => {
        return prettierSpec.rewriteRun(
            // @formatter:off
            //language=typescript
            // Prettier adds semicolon and trailing newline
            typescript(`const x=1+2`,
                `const x = 1 + 2;

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

describe('Prettier marker reconciliation', () => {
    test('Prettier adds Semicolon marker when adding semicolon', async () => {
        // Parse code WITHOUT a semicolon
        const parser = new JavaScriptParser();
        const sourceFile = await parser.parseOne({
            sourcePath: 'test.ts',
            text: 'const x = 1'  // No semicolon
        }) as JS.CompilationUnit;

        // Verify the original has no Semicolon marker on the statement
        const originalStatements = (sourceFile as any).statements as J.RightPadded<Statement>[];
        const originalMarkers = originalStatements[0]?.markers?.markers || [];
        const originalHasSemicolon = originalMarkers.some((m: any) => m.kind === J.Markers.Semicolon);
        expect(originalHasSemicolon).toBe(false);

        // Format with Prettier (semi: true by default adds semicolons)
        const formatted = await prettierFormat(sourceFile, { semi: true });

        // Check if the formatted tree has the Semicolon marker
        const formattedStatements = (formatted as any).statements as J.RightPadded<Statement>[];
        const formattedMarkers = formattedStatements[0]?.markers?.markers || [];
        const formattedHasSemicolon = formattedMarkers.some((m: any) => m.kind === J.Markers.Semicolon);

        // This test currently FAILS because whitespace reconciler doesn't copy markers
        // Once fixed, this should pass
        expect(formattedHasSemicolon).toBe(true);
    });

    test('Prettier preserves Semicolon marker when present', async () => {
        // Parse code WITH a semicolon
        const parser = new JavaScriptParser();
        const sourceFile = await parser.parseOne({
            sourcePath: 'test.ts',
            text: 'const x = 1;'  // Has semicolon
        }) as JS.CompilationUnit;

        // Verify the original has Semicolon marker
        const originalStatements = (sourceFile as any).statements as J.RightPadded<Statement>[];
        const originalMarkers = originalStatements[0]?.markers?.markers || [];
        const originalHasSemicolon = originalMarkers.some((m: any) => m.kind === J.Markers.Semicolon);
        expect(originalHasSemicolon).toBe(true);

        // Format with Prettier
        const formatted = await prettierFormat(sourceFile, { semi: true });

        // Semicolon marker should still be present
        const formattedStatements = (formatted as any).statements as J.RightPadded<Statement>[];
        const formattedMarkers = formattedStatements[0]?.markers?.markers || [];
        const formattedHasSemicolon = formattedMarkers.some((m: any) => m.kind === J.Markers.Semicolon);
        expect(formattedHasSemicolon).toBe(true);
    });
});
