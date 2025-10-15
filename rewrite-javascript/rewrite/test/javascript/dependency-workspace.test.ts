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
import {DependencyWorkspace} from "../../src/javascript/dependency-workspace";
import {JavaScriptParser, JavaScriptVisitor, JS, npm, packageJson, typescript} from "../../src/javascript";
import {J} from "../../src/java";
import {RecipeSpec} from "../../src/test";
import {withDir} from "tmp-promise";
import * as fs from "fs";
import * as path from "path";

describe('dependency workspace', () => {
    afterAll(() => {
        // Clean up after tests
        DependencyWorkspace.clearCache();
    });

    test('creates workspace with package.json', async () => {
        const dependencies = {
            'lodash': '^4.17.21'
        };

        const workspaceDir = await DependencyWorkspace.getOrCreateWorkspace(dependencies);

        // Check workspace exists
        expect(fs.existsSync(workspaceDir)).toBe(true);

        // Check package.json exists
        const packageJsonPath = path.join(workspaceDir, 'package.json');
        expect(fs.existsSync(packageJsonPath)).toBe(true);

        // Check package.json content
        const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf-8'));
        expect(packageJson.dependencies).toEqual(dependencies);

        // Check node_modules exists
        const nodeModulesPath = path.join(workspaceDir, 'node_modules');
        expect(fs.existsSync(nodeModulesPath)).toBe(true);

        // Check lodash was installed
        const lodashPath = path.join(nodeModulesPath, 'lodash');
        expect(fs.existsSync(lodashPath)).toBe(true);
    }, 60000); // 60 second timeout for npm install

    test('caches workspace for same dependencies', async () => {
        const dependencies = {
            'uuid': '^9.0.0'
        };

        const workspace1 = await DependencyWorkspace.getOrCreateWorkspace(dependencies);
        const workspace2 = await DependencyWorkspace.getOrCreateWorkspace(dependencies);

        // Should return the same workspace directory
        expect(workspace1).toBe(workspace2);
    }, 60000);

    test('creates different workspaces for different dependencies', async () => {
        const deps1 = { 'lodash': '^4.17.21' };
        const deps2 = { 'uuid': '^9.0.0' };

        const workspace1 = await DependencyWorkspace.getOrCreateWorkspace(deps1);
        const workspace2 = await DependencyWorkspace.getOrCreateWorkspace(deps2);

        // Should return different workspace directories
        expect(workspace1).not.toBe(workspace2);
    }, 60000);

    test('parser uses workspace for type attribution', async () => {
        // Only @types packages are needed for type attribution (not runtime packages)
        const dependencies = {
            '@types/uuid': '^9.0.0'
        };

        // Create a workspace directory with the dependencies
        const workspaceDir = await DependencyWorkspace.getOrCreateWorkspace(dependencies);

        // Create parser with workspace directory as relativeTo
        const parser = new JavaScriptParser({relativeTo: workspaceDir});

        // Parse code that uses the dependency
        const source = `
            import { v4 } from 'uuid';
            const id = v4();
        `;

        const parseGenerator = parser.parse({text: source, sourcePath: 'test.ts'});
        const cu: JS.CompilationUnit = (await parseGenerator.next()).value as JS.CompilationUnit;

        // Verify type attribution exists
        let foundMethodInvocation = false;
        await (new class extends JavaScriptVisitor<any> {
            protected async visitMethodInvocation(method: J.MethodInvocation, _: any): Promise<J | undefined> {
                if (method.name.simpleName === 'v4') {
                    foundMethodInvocation = true;
                    // Verify that the method has type information from the workspace dependencies
                    expect(method.methodType).toBeDefined();
                    expect(method.methodType?.name).toBe('v4');
                }
                return method;
            }
        }).visit(cu, undefined);

        expect(foundMethodInvocation).toBe(true);
    }, 60000);

    test('type attribution with on-disk dependencies (comparison)', async () => {
        const spec = new RecipeSpec();

        await withDir(async repo => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {
                        //language=typescript
                        ...typescript(
                            `
                                import { v4 } from 'uuid';
                                const id = v4();
                            `
                        ),
                        afterRecipe: async cu => {
                            let foundMethodInvocation = false;
                            await (new class extends JavaScriptVisitor<any> {
                                protected async visitMethodInvocation(method: J.MethodInvocation, _: any): Promise<J | undefined> {
                                    if (method.name.simpleName === 'v4') {
                                        foundMethodInvocation = true;
                                        // Verify type attribution works with on-disk dependencies (baseline comparison)
                                        expect(method.methodType).toBeDefined();
                                        expect(method.methodType?.name).toBe('v4');
                                    }
                                    return method;
                                }
                            }).visit(cu, undefined);
                            expect(foundMethodInvocation).toBe(true);
                        }
                    },
                    //language=json
                    packageJson(
                        `
                          {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                              "uuid": "^9.0.0",
                              "@types/uuid": "^9.0.0"
                            }
                          }
                        `
                    )
                )
            );
        }, {unsafeCleanup: true});
    }, 60000);
});
