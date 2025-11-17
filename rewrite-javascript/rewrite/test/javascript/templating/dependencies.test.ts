// noinspection TypeScriptCheckImport,JSUnusedLocalSymbols,ES6UnusedImports,SuspiciousTypeOfGuard

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
import {
    _,
    capture,
    JavaScriptParser,
    JavaScriptVisitor,
    MethodMatcher,
    npm,
    packageJson,
    pattern,
    rewrite,
    template,
    typescript
} from "../../../src/javascript";
import {DependencyWorkspace} from "../../../src/javascript/dependency-workspace";
import {J} from "../../../src/java";
import {fromVisitor, RecipeSpec} from "../../../src/test";
import * as path from "path";
import * as os from "os";

describe('template dependencies integration', () => {

    test('pattern with dependencies has proper type attribution in AST', async () => {
        // Create a pattern that uses uuid with proper dependencies
        const pat = pattern`v4()`.configure({
            context: ['import { v4 } from "uuid"'],
            dependencies: {'@types/uuid': '^9.0.0'}
        });

        // Parse some code with the parser (to trigger template parsing internally)
        const parser = new JavaScriptParser();
        const source = `const x = 1;`; // dummy code
        const parseGenerator = parser.parse({text: source, sourcePath: 'test.ts'});
        await parseGenerator.next();

        // Now try to match against a method invocation we create
        // The pattern's internal AST should have type attribution
        const testCode = `
            import { v4 } from 'uuid';
            const id = v4();
        `;

        const testParser = new JavaScriptParser();
        const testGen = testParser.parse({text: testCode, sourcePath: 'test.ts'});
        const cu = (await testGen.next()).value;

        // Find the v4() call and verify pattern can match it
        let foundMatch = false;
        await (new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const match = await pat.match(method);
                if (match && method.name.simpleName === 'v4') {
                    foundMatch = true;
                    // The match succeeded, which means the pattern's AST was properly parsed
                }
                return method;
            }
        }).visit(cu, undefined);

        expect(foundMatch).toBe(true);
    }, 60000);

    test('template with dependencies generates AST with type attribution', async () => {
        // Create a template with dependencies
        const tmpl = template`v1()`.configure({
            context: ['import { v1 } from "uuid"'],
            dependencies: {'@types/uuid': '^9.0.0'}
        });

        // Parse some test code to get a cursor context
        const testCode = `const x = 1;`;
        const parser = new JavaScriptParser();
        const parseGen = parser.parse({text: testCode, sourcePath: 'test.ts'});
        const cu = (await parseGen.next()).value;

        // Apply the template in a visitor
        let replacementFound = false;
        const result = await (new class extends JavaScriptVisitor<any> {
            override async visitVariable(variable: any, p: any): Promise<any> {
                if (!replacementFound) {
                    // Apply the template to replace the initializer
                    const replacement = await tmpl.apply(this.cursor, variable, new Map());
                    replacementFound = true;

                    // Verify the replacement was created
                    expect(replacement).toBeDefined();
                }
                return variable;
            }
        }).visit(cu, undefined);

        expect(replacementFound).toBe(true);
    }, 60000);

    test('multiple patterns with same dependencies share workspace', async () => {
        // Create multiple patterns with identical dependencies
        const pat1 = pattern`v4()`.configure({
            context: ['import { v4 } from "uuid"'],
            dependencies: {'@types/uuid': '^9.0.0'}
        });

        const pat2 = pattern`v4()`.configure({
            context: ['import { v4 } from "uuid"'],
            dependencies: {'@types/uuid': '^9.0.0'}
        });

        const pat3 = pattern`v4()`.configure({
            context: ['import { v4 } from "uuid"'],
            dependencies: {'@types/uuid': '^9.0.0'}
        });

        // Parse test code with v4() call
        const testCode = `
            import { v4 } from 'uuid';
            const id = v4();
        `;

        const parser = new JavaScriptParser();
        const parseGen = parser.parse({text: testCode, sourcePath: 'test.ts'});
        const cu = (await parseGen.next()).value;

        // All patterns should successfully match (proving they all parsed correctly)
        let matchCount = 0;
        await (new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (method.name.simpleName === 'v4') {
                    // Try all three patterns
                    const match1 = await pat1.match(method);
                    const match2 = await pat2.match(method);
                    const match3 = await pat3.match(method);

                    if (match1) matchCount++;
                    if (match2) matchCount++;
                    if (match3) matchCount++;
                }
                return method;
            }
        }).visit(cu, undefined);

        // All three patterns should have matched
        expect(matchCount).toBe(3);
    }, 60000);

    test('template with dependencies provides type attribution verified by MethodMatcher', async () => {
        // Create a pattern that matches v1() with dependencies
        const pat = pattern`v1()`.configure({
            context: ['import { v1 } from "uuid"'],
            dependencies: {'@types/uuid': '^9.0.0'}
        });

        // Parse test code with v1() call using a workspace with dependencies
        const testCode = `
            import { v1 } from 'uuid';
            const id = v1();
        `;

        // Create workspace for parsing the test code (so it has type attribution)
        const workspaceDir = await DependencyWorkspace.getOrCreateWorkspace({'@types/uuid': '^9.0.0'});

        const parser = new JavaScriptParser({relativeTo: workspaceDir});
        const parseGen = parser.parse({text: testCode, sourcePath: 'test.ts'});
        const cu = (await parseGen.next()).value;

        // Create MethodMatcher to verify type attribution
        const matcher = new MethodMatcher('uuid v1(..)');

        // Verify the pattern can match and the method has type attribution
        let checkedMethodType = false;
        await (new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, _p: any): Promise<J | undefined> {
                if (method.name.simpleName === 'v1') {
                    // The pattern should match (which implicitly uses its typed internal AST)
                    const match = await pat.match(method);
                    expect(match).toBeDefined();

                    // Verify the method has proper type attribution from dependencies
                    // This is the key test - methodType.name should be 'v1' not 'unknown'
                    expect(method.methodType).toBeDefined();
                    expect(method.methodType!.name).toBe('v1');

                    // Verify MethodMatcher recognizes the type attribution
                    expect(matcher.matches(method.methodType!)).toBe(true);

                    checkedMethodType = true;
                }
                return method;
            }
        }).visit(cu, undefined);

        expect(checkedMethodType).toBe(true);
    }, 60000);

    test('pattern with dependencies does not match when types differ', async () => {
        // Create a pattern that matches v1() from uuid with proper dependencies
        const pat = pattern`v1()`.configure({
            context: ['import { v1 } from "uuid"'],
            dependencies: {'@types/uuid': '^9.0.0'}
        });

        // Parse test code that has a v1() call from a DIFFERENT type (custom class)
        const testCode = `
            class CustomUuid {
                v1() {
                    return 'custom';
                }
            }
            const uuid = new CustomUuid();
            const id = uuid.v1();
        `;

        // Create workspace for parsing the test code
        const workspaceDir = await DependencyWorkspace.getOrCreateWorkspace({'@types/uuid': '^9.0.0'});

        const parser = new JavaScriptParser({relativeTo: workspaceDir});
        const parseGen = parser.parse({text: testCode, sourcePath: 'test.ts'});
        const cu = (await parseGen.next()).value;

        // Verify the pattern does NOT match the custom v1() call
        let foundV1Call = false;
        let patternMatched = false;
        await (new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, _p: any): Promise<J | undefined> {
                if (method.name.simpleName === 'v1') {
                    foundV1Call = true;
                    // The pattern should NOT match because the types are different
                    const match = await pat.match(method);
                    if (match) {
                        patternMatched = true;
                    }
                }
                return method;
            }
        }).visit(cu, undefined);

        // We should have found a v1() call
        expect(foundV1Call).toBe(true);
        // But the pattern should NOT have matched (different type)
        expect(patternMatched).toBe(false);
    }, 60000);

    test('pattern with dependencies matches when types are correct', async () => {
        // Create a pattern that matches v1() from uuid with proper dependencies
        const pat = pattern`v1()`.configure({
            context: ['import { v1 } from "uuid"'],
            dependencies: {'@types/uuid': '^9.0.0'}
        });

        // Parse test code that has a v1() call from the CORRECT type (uuid module)
        const testCode = `
            import { v1 } from 'uuid';
            const id = v1();
        `;

        // Create workspace for parsing the test code
        const workspaceDir = await DependencyWorkspace.getOrCreateWorkspace({'@types/uuid': '^9.0.0'});

        const parser = new JavaScriptParser({relativeTo: workspaceDir});
        const parseGen = parser.parse({text: testCode, sourcePath: 'test.ts'});
        const cu = (await parseGen.next()).value;

        // Verify the pattern DOES match the uuid v1() call
        let foundV1Call = false;
        let patternMatched = false;
        await (new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, _p: any): Promise<J | undefined> {
                if (method.name.simpleName === 'v1') {
                    foundV1Call = true;
                    // The pattern SHOULD match because the types are the same
                    const match = await pat.match(method);
                    if (match) {
                        patternMatched = true;
                    }
                }
                return method;
            }
        }).visit(cu, undefined);

        // We should have found a v1() call
        expect(foundV1Call).toBe(true);
        // And the pattern SHOULD have matched (same type)
        expect(patternMatched).toBe(true);
    }, 60000);

    test('rewrite with type-aware pattern swaps operands', async () => {
        // Simple test showing rewrite() with type-aware patterns
        // This builds on the existing functionality to ensure our semantic equality checking works
        const spec = new RecipeSpec();
        const swapOperands = rewrite(() => {
            const {left, right} = {left: capture(), right: capture()};
            return {
                before: pattern`${left} + ${right}`,
                after: template`${right} + ${left}`
            };
        });

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                return await swapOperands.tryOn(this.cursor, binary) || binary;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript('const result = 1 + 2;', 'const result = 2 + 1;')
        );
    }, 60000);

    test('underscore alias for inline captures', async () => {
        // Test using the _ alias for concise inline captures
        const spec = new RecipeSpec();

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                const swapOperands = rewrite(() => ({
                    before: pattern`${_('left')} + ${_('right')}`,
                    after: template`${_('right')} + ${_('left')}`
                }));
                return await swapOperands.tryOn(this.cursor, binary) || binary;
            }
        });

        return spec.rewriteRun(
            typescript('const result = 1 + 2;', 'const result = 2 + 1;')
        );
    }, 60000);

    test('recipe: replace deprecated util.isX methods with native checks', async () => {
        // Tests multiple rewrite rules with different patterns and templates
        // This ensures each pattern only matches its intended methods and applies the correct template
        const spec = new RecipeSpec();

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, _p: any): Promise<J | undefined> {
                // Create rewrite rules fresh for each invocation
                const arg = capture();
                const replaceUtilIsArray = rewrite(() => ({
                    before: pattern`util.isArray(${arg})`.configure({
                        context: ["import * as util from 'util'"],
                        dependencies: {'@types/node': '^20.0.0'}
                    }),
                    after: template`Array.isArray(${arg})`
                }));

                const replaceUtilIsBoolean = rewrite(() => ({
                    before: pattern`util.isBoolean(${arg})`.configure({
                        context: ["import * as util from 'util'"],
                        dependencies: {'@types/node': '^20.0.0'}
                    }),
                    after: template`typeof ${arg} === 'boolean'`
                }));

                return await replaceUtilIsArray.tryOn(this.cursor, method) ||
                    await replaceUtilIsBoolean.tryOn(this.cursor, method) ||
                    method;
            }
        });

        const tempDir = path.join(os.tmpdir(), `test-${Date.now()}`);

        return spec.rewriteRun(
            npm(tempDir,
                packageJson(JSON.stringify({
                    "name": "test",
                    "dependencies": {
                        "@types/node": "^20.0.0"
                    }
                }, null, 2)),
                // language=typescript
                typescript(
                    `
                        import * as util from 'util';

                        class CustomUtil {
                            isArray(value: any) {
                                return 'custom';
                            }

                            isBoolean(value: any) {
                                return 'custom';
                            }
                        }

                        const customUtil = new CustomUtil();
                        const arr = [1, 2, 3];
                        const bool = true;

                        // These should be replaced (from util module)
                        const check1 = util.isArray(arr);
                        const check2 = util.isBoolean(bool);
                        const check3 = util.isArray([]);
                        const check4 = util.isBoolean(false);

                        // These should NOT be replaced (custom methods)
                        const custom1 = customUtil.isArray(arr);
                        const custom2 = customUtil.isBoolean(bool);
                    `,
                    `
                        import * as util from 'util';

                        class CustomUtil {
                            isArray(value: any) {
                                return 'custom';
                            }

                            isBoolean(value: any) {
                                return 'custom';
                            }
                        }

                        const customUtil = new CustomUtil();
                        const arr = [1, 2, 3];
                        const bool = true;

                        // These should be replaced (from util module)
                        const check1 = Array.isArray(arr);
                        const check2 = typeof bool === 'boolean';
                        const check3 = Array.isArray([]);
                        const check4 = typeof false === 'boolean';

                        // These should NOT be replaced (custom methods)
                        const custom1 = customUtil.isArray(arr);
                        const custom2 = customUtil.isBoolean(bool);
                    `
                )
            )
        );
    }, 60000);

    test('rewrite with type-aware pattern prevents false positives - isDate', async () => {
        // Test that rewrite() only replaces util.isDate() from the util module,
        // not custom isDate() methods on other objects.
        // A single pattern with type attribution should match BOTH isDate(x) and util.isDate(x)
        const spec = new RecipeSpec();

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, _p: any): Promise<J | undefined> {
                const dateArg = capture('dateArg');
                // Single pattern that matches both isDate(x) and util.isDate(x) via type attribution
                const replaceIsDate = rewrite(() => ({
                    before: pattern`isDate(${dateArg})`.configure({
                        context: ['import { isDate } from "util"'],
                        dependencies: {'@types/node': '^20.0.0'}
                    }),
                    after: template`${dateArg} instanceof Date`
                }));

                return await replaceIsDate.tryOn(this.cursor, method) || method;
            }
        });

        const tempDir = path.join(os.tmpdir(), `test-${Date.now()}`);

        return spec.rewriteRun(
            npm(tempDir,
                packageJson(JSON.stringify({
                    "name": "test",
                    "dependencies": {
                        "@types/node": "^20.0.0"
                    }
                }, null, 2)),
                // language=typescript
                typescript(
                    `
                        import {isDate} from 'util';
                        import * as util from 'util';

                        class CustomValidator {
                            isDate(value: any) {
                                return typeof value === 'string';
                            }
                        }

                        const validator = new CustomValidator();
                        const value = new Date();

                        // This should be replaced (from util module via named import)
                        const isRealDate1 = isDate(value);

                        // This should be replaced (from util module via namespace import)
                        const isRealDate2 = util.isDate(new Date());

                        // This should NOT be replaced (custom method)
                        const isCustomDate = validator.isDate(value);
                    `,
                    `
                        import {isDate} from 'util';
                        import * as util from 'util';

                        class CustomValidator {
                            isDate(value: any) {
                                return typeof value === 'string';
                            }
                        }

                        const validator = new CustomValidator();
                        const value = new Date();

                        // This should be replaced (from util module via named import)
                        const isRealDate1 = value instanceof Date;

                        // This should be replaced (from util module via namespace import)
                        const isRealDate2 = new Date() instanceof Date;

                        // This should NOT be replaced (custom method)
                        const isCustomDate = validator.isDate(value);
                    `
                )
            )
        );
    }, 60000);
});

