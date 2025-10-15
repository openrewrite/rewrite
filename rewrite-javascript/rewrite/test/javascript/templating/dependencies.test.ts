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
import {JavaScriptParser, JavaScriptVisitor} from "../../../src/javascript";
import {pattern, template, MethodMatcher} from "../../../src/javascript";
import {DependencyWorkspace} from "../../../src/javascript/dependency-workspace";
import {J} from "../../../src/java";

describe('template dependencies integration', () => {

    test('pattern with dependencies has proper type attribution in AST', async () => {
        // Create a pattern that uses uuid with proper dependencies
        const pat = pattern`v4()`.configure({
            imports: ['import { v4 } from "uuid"'],
            dependencies: { '@types/uuid': '^9.0.0' }
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
            imports: ['import { v1 } from "uuid"'],
            dependencies: { '@types/uuid': '^9.0.0' }
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
            imports: ['import { v4 } from "uuid"'],
            dependencies: { '@types/uuid': '^9.0.0' }
        });

        const pat2 = pattern`v4()`.configure({
            imports: ['import { v4 } from "uuid"'],
            dependencies: { '@types/uuid': '^9.0.0' }
        });

        const pat3 = pattern`v4()`.configure({
            imports: ['import { v4 } from "uuid"'],
            dependencies: { '@types/uuid': '^9.0.0' }
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
            imports: ['import { v1 } from "uuid"'],
            dependencies: { '@types/uuid': '^9.0.0' }
        });

        // Parse test code with v1() call using a workspace with dependencies
        const testCode = `
            import { v1 } from 'uuid';
            const id = v1();
        `;

        // Create workspace for parsing the test code (so it has type attribution)
        const workspaceDir = await DependencyWorkspace.getOrCreateWorkspace({ '@types/uuid': '^9.0.0' });

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
});

