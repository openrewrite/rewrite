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
    npm,
    packageJson,
    prettierFormat,
    prettierStyle,
    PrettierStyle,
    typescript
} from "../../../src/javascript";
import {PrettierConfigLoader} from "../../../src/javascript/format/prettier-config-loader";
import {json} from "../../../src/json";
import {text} from "../../../src/text";
import {J, Statement} from "../../../src/java";
import {randomId, TreePrinters} from "../../../src";
import {withDir} from "tmp-promise";
import * as path from "path";
import * as fsp from "fs/promises";

// A simple PrettierStyle with default configuration
const defaultPrettierStyle = prettierStyle(randomId(), {});

describe('AutoformatVisitor with Prettier', () => {
    const prettierSpec = new RecipeSpec()
    // Use Prettier for these tests by passing PrettierStyle
    prettierSpec.recipe = fromVisitor(new AutoformatVisitor(undefined, [defaultPrettierStyle]));

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

    test('subtree formatting with Prettier applies Prettier defaults', async () => {
        // This test verifies that subtree formatting (triggered via maybeAutoFormat)
        // works correctly with the Prettier pruning optimization.
        // Note: Prettier's default tabWidth is 2, so the formatted line will have
        // 2-space indentation even if surrounding code has different indentation.
        const visitor = new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: any, p: any): Promise<any> {
                // Only format the info() call to simulate template replacement
                if (methodInvocation.name?.simpleName === 'info') {
                    return await autoFormat(methodInvocation, p, undefined, this.cursor.parent, [defaultPrettierStyle]);
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
                `,
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
                    return await autoFormat(methodInvocation, p, undefined, this.cursor.parent, [defaultPrettierStyle]);
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
        // Note: Prettier's default tabWidth is 2, so output uses 2-space indentation
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
        // - Visitor modifies the method name (log -> info) then calls autoFormat
        // - Prettier fixes the spacing in just the formatted call, not surrounding statements
        // This simulates what visitors often do: modify an AST node and then format it
        const visitor = new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: J.MethodInvocation, p: any): Promise<J | undefined> {
                // Change console.log() to console.info() and format
                if (methodInvocation.name?.simpleName === 'log') {
                    // Modify the method name from 'log' to 'info'
                    const modified: J.MethodInvocation = {
                        ...methodInvocation,
                        name: {
                            ...methodInvocation.name,
                            simpleName: 'info'
                        }
                    };
                    // Format the modified node - this is the typical pattern in visitors
                    return await autoFormat(modified, p, undefined, this.cursor.parent, [defaultPrettierStyle]);
                }
                return super.visitMethodInvocation(methodInvocation, p);
            }
        }();

        const testSpec = new RecipeSpec();
        testSpec.recipe = fromVisitor(visitor);

        return testSpec.rewriteRun(
            // @formatter:off
            //language=typescript
            // Note: subtree formatting uses Prettier's 2-space default, so the formatted
            // line has slightly different indentation than surrounding context
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
                  console.info("before", "target", "after", { key: "value" });
                    const third =3;
                }
                `
            )
            // @formatter:on
        )
    });

    test('Prettier adds parentheses to single-parameter arrow function', () => {
        // Prettier's default arrowParens: 'always' adds parentheses around single parameters
        // This tests that the Lambda.Parameters.parenthesized boolean gets updated
        return prettierSpec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `const fn = x => x + 1`,
                `const fn = (x) => x + 1;
`
            )
            // @formatter:on
        );
    });
});

describe('Prettier eof handling', () => {
    test('Prettier adds trailing newline via eof', async () => {
        const parser = new JavaScriptParser();

        // Parse code without trailing newline
        const sourceFile = await parser.parseOne({
            sourcePath: 'test.ts',
            text: 'const x = 1;'  // No trailing newline
        }) as JS.CompilationUnit;

        // Original has no trailing whitespace in eof
        expect(sourceFile.eof.whitespace).toBe('');

        // Format with Prettier
        const formatted = await prettierFormat(sourceFile, { semi: true });

        // Prettier adds trailing newline - this is stored in eof
        expect(formatted.eof.whitespace).toBe('\n');

        // Verify the printed output reflects this
        const formattedPrinted = await TreePrinters.print(formatted);
        expect(formattedPrinted).toBe('const x = 1;\n');
    });

    test('no double newline when original already has trailing newline', async () => {
        const parser = new JavaScriptParser();

        // Parse code WITH trailing newline
        const sourceFile = await parser.parseOne({
            sourcePath: 'test.ts',
            text: 'const x = 1;\n'  // Has trailing newline
        }) as JS.CompilationUnit;

        // Original eof already has the newline
        expect(sourceFile.eof.whitespace).toBe('\n');

        // Format with Prettier
        const formatted = await prettierFormat(sourceFile, { semi: true });

        // Formatted eof still has just one newline
        expect(formatted.eof.whitespace).toBe('\n');

        // Verify we don't get double newline in output
        const formattedPrinted = await TreePrinters.print(formatted);
        expect(formattedPrinted).toBe('const x = 1;\n');
        expect(formattedPrinted.endsWith('\n\n')).toBe(false);
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
        // With intersection types, padding markers are in statement.padding.markers
        const originalStatements = (sourceFile as any).statements as J.RightPadded<Statement>[];
        const originalPaddingMarkers = (originalStatements[0] as any)?.padding?.markers?.markers || [];
        const originalHasSemicolon = originalPaddingMarkers.some((m: any) => m.kind === J.Markers.Semicolon);
        expect(originalHasSemicolon).toBe(false);

        // Format with Prettier (semi: true by default adds semicolons)
        const formatted = await prettierFormat(sourceFile, { semi: true });

        // Check if the formatted tree has the Semicolon marker
        // With intersection types, padding markers are in statement.padding.markers
        const formattedStatements = (formatted as any).statements as J.RightPadded<Statement>[];
        const formattedPaddingMarkers = (formattedStatements[0] as any)?.padding?.markers?.markers || [];
        const formattedHasSemicolon = formattedPaddingMarkers.some((m: any) => m.kind === J.Markers.Semicolon);

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
        // With intersection types, padding markers are in statement.padding.markers
        const originalStatements = (sourceFile as any).statements as J.RightPadded<Statement>[];
        const originalPaddingMarkers = (originalStatements[0] as any)?.padding?.markers?.markers || [];
        const originalHasSemicolon = originalPaddingMarkers.some((m: any) => m.kind === J.Markers.Semicolon);
        expect(originalHasSemicolon).toBe(true);

        // Format with Prettier
        const formatted = await prettierFormat(sourceFile, { semi: true });

        // Semicolon marker should still be present
        // With intersection types, padding markers are in statement.padding.markers
        const formattedStatements = (formatted as any).statements as J.RightPadded<Statement>[];
        const formattedPaddingMarkers = (formattedStatements[0] as any)?.padding?.markers?.markers || [];
        const formattedHasSemicolon = formattedPaddingMarkers.some((m: any) => m.kind === J.Markers.Semicolon);
        expect(formattedHasSemicolon).toBe(true);
    });
});

describe('Prettier auto-detection integration', () => {
    // These tests verify that Prettier config is automatically detected from .prettierrc
    // when prettier is installed as a dependency.

    function prettierrc(config: Record<string, unknown>) {
        return {
            ...json(JSON.stringify(config, null, 2)),
            path: '.prettierrc'
        };
    }

    test('formats using .prettierrc config when Prettier is a dependency', async () => {
        await withDir(async (repo) => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AutoformatVisitor());

            await spec.rewriteRun(
                npm(
                    repo.path,
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "devDependencies": {
                                "prettier": "^3.0.0"
                            }
                        }
                    `),
                    // .prettierrc with single quotes, no semicolons
                    prettierrc({ singleQuote: true, semi: false }),
                    // Input has double quotes and semicolon
                    // Output should have single quotes and no semicolon per .prettierrc
                    typescript(
                        `const x = "hello";`,
                        `const x = 'hello'
`
                    )
                )
            );
        }, {unsafeCleanup: true});
    });

    test('detects Prettier in monorepo parent directory', async () => {
        // This test verifies that PrettierConfigLoader scans upward from projectRoot
        // to find Prettier installed in a monorepo root
        await withDir(async (repo) => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AutoformatVisitor());

            // Monorepo structure:
            // /
            //   package.json (with prettier)
            //   .prettierrc (config at root)
            //   apps/
            //     web/
            //       package.json (no prettier dependency)
            //       src/
            //         test.ts
            await spec.rewriteRun(
                npm(
                    repo.path,
                    packageJson(`
                        {
                            "name": "monorepo",
                            "private": true,
                            "devDependencies": {
                                "prettier": "^3.0.0"
                            }
                        }
                    `),
                    // .prettierrc at monorepo root
                    prettierrc({ singleQuote: true, semi: false }),
                    // Nested package without prettier
                    {
                        ...packageJson(`
                            {
                                "name": "@apps/web",
                                "version": "1.0.0"
                            }
                        `),
                        path: 'apps/web/package.json'
                    },
                    // Source file in nested package
                    {
                        ...typescript(
                            `const x = "hello";`,
                            `const x = 'hello'
`
                        ),
                        path: 'apps/web/src/test.ts'
                    }
                )
            );
        }, {unsafeCleanup: true});
    });
});

describe('Prettier .prettierignore handling', () => {
    test('resolveConfig returns config for ignored files (does not check ignore)', async () => {
        // This test explores what Prettier's resolveConfig() returns for ignored files
        // Spoiler: resolveConfig() does NOT check .prettierignore - it still returns config
        // We need to use getFileInfo() or check() to determine if a file is ignored
        await withDir(async (repo) => {
            // Set up a minimal project with .prettierignore
            await fsp.mkdir(path.join(repo.path, 'node_modules', 'prettier'), { recursive: true });
            await fsp.writeFile(
                path.join(repo.path, 'node_modules', 'prettier', 'package.json'),
                JSON.stringify({ name: 'prettier', version: '3.0.0' })
            );
            await fsp.writeFile(
                path.join(repo.path, 'package.json'),
                JSON.stringify({ name: 'test', devDependencies: { prettier: '^3.0.0' } })
            );
            await fsp.writeFile(
                path.join(repo.path, '.prettierrc'),
                JSON.stringify({ singleQuote: true })
            );
            await fsp.writeFile(
                path.join(repo.path, '.prettierignore'),
                'ignored.ts\ndist/**\n'
            );
            await fsp.writeFile(path.join(repo.path, 'normal.ts'), 'const x = 1;');
            await fsp.writeFile(path.join(repo.path, 'ignored.ts'), 'const x = 1;');

            const loader = new PrettierConfigLoader(repo.path);
            const detection = await loader.detectPrettier();

            expect(detection.available).toBe(true);
            expect(detection.bundledPrettier).toBeDefined();

            // resolveConfig returns config for BOTH files - it doesn't check .prettierignore
            const normalConfig = await detection.bundledPrettier!.resolveConfig(
                path.join(repo.path, 'normal.ts')
            );
            const ignoredConfig = await detection.bundledPrettier!.resolveConfig(
                path.join(repo.path, 'ignored.ts')
            );

            // Both get the same config - resolveConfig doesn't care about .prettierignore
            expect(normalConfig).toEqual({ singleQuote: true });
            expect(ignoredConfig).toEqual({ singleQuote: true });

            // To check if a file is ignored, we need getFileInfo() WITH ignorePath option
            // By default, getFileInfo() does NOT check .prettierignore!
            const ignorePath = path.join(repo.path, '.prettierignore');

            const normalInfo = await detection.bundledPrettier!.getFileInfo(
                path.join(repo.path, 'normal.ts'),
                { ignorePath }
            );
            const ignoredInfo = await detection.bundledPrettier!.getFileInfo(
                path.join(repo.path, 'ignored.ts'),
                { ignorePath }
            );

            expect(normalInfo.ignored).toBe(false);
            expect(ignoredInfo.ignored).toBe(true);
        }, { unsafeCleanup: true });
    });

    test('files in .prettierignore should not be formatted', async () => {
        // This test verifies that ignored files are NOT formatted
        await withDir(async (repo) => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new AutoformatVisitor());

            await spec.rewriteRun(
                npm(
                    repo.path,
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "devDependencies": {
                                "prettier": "^3.0.0"
                            }
                        }
                    `),
                    // .prettierrc with single quotes, no semicolons
                    {
                        ...json(JSON.stringify({ singleQuote: true, semi: false }, null, 2)),
                        path: '.prettierrc'
                    },
                    // .prettierignore - ignore dist folder
                    {
                        ...text('dist/**\n'),
                        path: '.prettierignore'
                    },
                    // Normal file - should be formatted
                    {
                        ...typescript(
                            `const x = "hello";`,
                            `const x = 'hello'
`
                        ),
                        path: 'src/normal.ts'
                    },
                    // Ignored file - should NOT be formatted (no "after" means no change expected)
                    {
                        ...typescript(`const x = "hello";`),
                        path: 'dist/ignored.ts'
                    }
                )
            );
        }, { unsafeCleanup: true });
    });
});

describe('Prettier stopAfter support', () => {
    test('stopAfter limits formatting to only part of the subtree', async () => {
        // This test verifies that stopAfter works with Prettier formatting.
        // We format a Lambda but stop after the parameters, leaving the body unformatted.
        const visitor = new class extends JavaScriptVisitor<any> {
            override async visitLambda(lambda: J.Lambda, p: any): Promise<J | undefined> {
                // Format the lambda but stop after parameters
                // The body should remain with its original (bad) formatting
                return await autoFormat(lambda, p, lambda.parameters, this.cursor.parent, [defaultPrettierStyle]);
            }
        }();

        const testSpec = new RecipeSpec();
        testSpec.recipe = fromVisitor(visitor);

        return testSpec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                // Input: badly formatted parameters AND body
                `const fn = (a:number,b:string) => {return a+b}`,
                // Output: parameters are formatted (spaces added), but body is NOT formatted
                // Note: the body keeps its original bad formatting (no spaces around +)
                `const fn = (a: number, b: string) => {return a+b}`
            )
            // @formatter:on
        );
    });

    test('stopAfter with deeply nested node', async () => {
        // Test stopAfter with a more complex case: stop after a specific parameter
        const visitor = new class extends JavaScriptVisitor<any> {
            override async visitLambda(lambda: J.Lambda, p: any): Promise<J | undefined> {
                // Get the first parameter to use as stopAfter
                const params = lambda.parameters as J.Lambda.Parameters;
                if (params.parameters.length > 0) {
                    const firstParam = params.parameters[0];
                    // Format the lambda but stop after the first parameter
                    return await autoFormat(lambda, p, firstParam, this.cursor.parent, [defaultPrettierStyle]);
                }
                return lambda;
            }
        }();

        const testSpec = new RecipeSpec();
        testSpec.recipe = fromVisitor(visitor);

        return testSpec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                // Input: multiple parameters with bad formatting
                `const fn = (a:number,b:string,c:boolean) => a+b`,
                // Output: only first parameter is formatted, rest keeps bad formatting
                `const fn = (a: number,b:string,c:boolean) => a+b`
            )
            // @formatter:on
        );
    });
});

describe('Prettier quoteProps option', () => {
    test('quoteProps as-needed removes unnecessary quotes', async () => {
        // quoteProps: "as-needed" removes quotes from property names that don't need them
        const quotePropsStyle = prettierStyle(randomId(), { quoteProps: 'as-needed' });
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new AutoformatVisitor(undefined, [quotePropsStyle]));

        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `const obj = { "foo": 1, "bar": 2 }`,
                `const obj = { foo: 1, bar: 2 };
`
            )
            // @formatter:on
        );
    });

    test('quoteProps consistent adds quotes when one property needs them', async () => {
        // quoteProps: "consistent" quotes all properties if at least one needs quoting
        const quotePropsStyle = prettierStyle(randomId(), { quoteProps: 'consistent' });
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new AutoformatVisitor(undefined, [quotePropsStyle]));

        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `const obj = { foo: 1, "bar-baz": 2 }`,
                `const obj = { "foo": 1, "bar-baz": 2 };
`
            )
            // @formatter:on
        );
    });

    test('quoteProps preserve keeps original quoting', async () => {
        // quoteProps: "preserve" keeps the input's use of quotes
        const quotePropsStyle = prettierStyle(randomId(), { quoteProps: 'preserve' });
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new AutoformatVisitor(undefined, [quotePropsStyle]));

        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `const obj = { "foo": 1, bar: 2 }`,
                `const obj = { "foo": 1, bar: 2 };
`
            )
            // @formatter:on
        );
    });
});
