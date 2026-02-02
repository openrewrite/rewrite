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
import {
    findNodeResolutionResult,
    npm,
    Npmrc,
    NpmrcScope,
    packageJson,
    PackageJsonParser,
    packageLockJson,
    readNpmrcConfigs,
    typescript
} from "../../src/javascript";
import {Json} from "../../src/json";
import {RecipeSpec} from "../../src/test";
import {withDir} from "tmp-promise";
import * as fs from "fs";
import * as path from "path";

describe("PackageJsonParser", () => {

    test("should parse package.json and create NodeResolutionResult marker", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    {
                        ...packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "description": "A test project",
                            "dependencies": {
                                "lodash": "^4.17.21"
                            },
                            "devDependencies": {
                                "typescript": "^5.0.0"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                            const marker = findNodeResolutionResult(doc);
                            expect(marker).toBeDefined();
                            expect(marker!.name).toBe("test-project");
                            expect(marker!.version).toBe("1.0.0");
                            expect(marker!.description).toBe("A test project");

                            // Check dependencies
                            expect(marker!.dependencies).toHaveLength(1);
                            expect(marker!.dependencies[0].name).toBe("lodash");
                            expect(marker!.devDependencies).toHaveLength(1);
                            expect(marker!.devDependencies[0].name).toBe("typescript");
                        }
                    }
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should read package-lock.json for resolved dependencies", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    {
                        ...packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "lodash": "^4.17.0"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                            const marker = findNodeResolutionResult(doc);
                            expect(marker).toBeDefined();
                            expect(marker!.resolvedDependencies.length).toBeGreaterThan(0);

                            // Check resolved dependency using the resolved property
                            const lodashDep = marker!.dependencies.find(d => d.name === "lodash");
                            expect(lodashDep?.resolved).toBeDefined();
                            expect(lodashDep!.resolved!.version).toBe("4.17.21");
                            expect(lodashDep!.resolved!.license).toBe("MIT");
                        }
                    },
                    packageLockJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "lockfileVersion": 3,
                            "packages": {
                                "": {
                                    "name": "test-project",
                                    "version": "1.0.0",
                                    "dependencies": {
                                        "lodash": "^4.17.0"
                                    }
                                },
                                "node_modules/lodash": {
                                    "version": "4.17.21",
                                    "resolved": "https://registry.npmjs.org/lodash/-/lodash-4.17.21.tgz",
                                    "license": "MIT"
                                }
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should skip dependency resolution when option is set", async () => {
        // This test uses PackageJsonParser directly to test the skipDependencyResolution option
        await withDir(async (dir) => {
            // Write package.json
            const packageJsonContent = {
                name: "test-project",
                version: "1.0.0",
                dependencies: {
                    "lodash": "^4.17.0"
                }
            };
            fs.writeFileSync(
                path.join(dir.path, 'package.json'),
                JSON.stringify(packageJsonContent, null, 2)
            );

            // Write package-lock.json
            const packageLock = {
                name: "test-project",
                version: "1.0.0",
                lockfileVersion: 3,
                packages: {
                    "": {
                        name: "test-project",
                        version: "1.0.0",
                        dependencies: {
                            "lodash": "^4.17.0"
                        }
                    },
                    "node_modules/lodash": {
                        version: "4.17.21",
                        license: "MIT"
                    }
                }
            };
            fs.writeFileSync(
                path.join(dir.path, 'package-lock.json'),
                JSON.stringify(packageLock, null, 2)
            );

            // Parse with skipDependencyResolution
            const parser = new PackageJsonParser({
                relativeTo: dir.path,
                skipDependencyResolution: true
            });
            const results: Json.Document[] = [];
            for await (const result of parser.parse(path.join(dir.path, 'package.json'))) {
                results.push(result as Json.Document);
            }

            expect(results).toHaveLength(1);
            const marker = findNodeResolutionResult(results[0]);
            expect(marker).toBeDefined();
            // Should not have resolved dependencies since we skipped resolution
            expect(marker!.resolvedDependencies).toHaveLength(0);
        }, {unsafeCleanup: true});
    });

    test("should accept only package.json files", () => {
        const parser = new PackageJsonParser();

        expect(parser.accept("package.json")).toBe(true);
        expect(parser.accept("/some/path/package.json")).toBe(true);
        expect(parser.accept("package-lock.json")).toBe(false);
        expect(parser.accept("tsconfig.json")).toBe(false);
        expect(parser.accept("index.ts")).toBe(false);
    });

    test("should work without lock file", async () => {
        // This test specifically checks behavior when no lock file exists
        // Using PackageJsonParser directly to avoid npm() creating a workspace with lock file
        await withDir(async (dir) => {
            // Write only package.json (no lock file)
            const packageJsonContent = {
                name: "test-project",
                version: "1.0.0",
                dependencies: {
                    "react": "^18.2.0"
                }
            };
            fs.writeFileSync(
                path.join(dir.path, 'package.json'),
                JSON.stringify(packageJsonContent, null, 2)
            );

            // Parse
            const parser = new PackageJsonParser({relativeTo: dir.path});
            const results: Json.Document[] = [];
            for await (const result of parser.parse(path.join(dir.path, 'package.json'))) {
                results.push(result as Json.Document);
            }

            expect(results).toHaveLength(1);
            const marker = findNodeResolutionResult(results[0]);
            expect(marker).toBeDefined();
            expect(marker!.name).toBe("test-project");
            expect(marker!.dependencies).toHaveLength(1);
            expect(marker!.dependencies[0].name).toBe("react");
            // No lock file = no resolved dependencies
            expect(marker!.resolvedDependencies).toHaveLength(0);
        }, {unsafeCleanup: true});
    });

    test("should parse from text input", async () => {
        const packageJsonText = JSON.stringify({
            name: "inline-project",
            version: "2.0.0",
            dependencies: {
                "express": "^4.18.0"
            }
        }, null, 2);

        const parser = new PackageJsonParser();
        const results: Json.Document[] = [];
        for await (const result of parser.parse({text: packageJsonText, sourcePath: "package.json"})) {
            results.push(result as Json.Document);
        }

        expect(results).toHaveLength(1);
        const marker = findNodeResolutionResult(results[0]);
        expect(marker).toBeDefined();
        expect(marker!.name).toBe("inline-project");
        expect(marker!.version).toBe("2.0.0");
    });

    test("should handle all dependency scopes", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    {
                        ...packageJson(`
                        {
                            "name": "full-deps-project",
                            "version": "1.0.0",
                            "dependencies": {"react": "^18.2.0"},
                            "devDependencies": {"jest": "^29.0.0"},
                            "peerDependencies": {"react-dom": "^18.2.0"},
                            "optionalDependencies": {"fsevents": "^2.3.0"},
                            "bundledDependencies": ["bundled-pkg"]
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                            const marker = findNodeResolutionResult(doc);
                            expect(marker).toBeDefined();
                            expect(marker!.dependencies).toHaveLength(1);
                            expect(marker!.devDependencies).toHaveLength(1);
                            expect(marker!.peerDependencies).toHaveLength(1);
                            expect(marker!.optionalDependencies).toHaveLength(1);
                            expect(marker!.bundledDependencies).toHaveLength(1);
                        }
                    }
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should read project .npmrc configuration", async () => {
        await withDir(async (dir) => {
            // Write package.json
            const packageJsonContent = {
                name: "test-project",
                version: "1.0.0"
            };
            fs.writeFileSync(
                path.join(dir.path, 'package.json'),
                JSON.stringify(packageJsonContent, null, 2)
            );

            // Write project .npmrc
            fs.writeFileSync(
                path.join(dir.path, '.npmrc'),
                `# Project npm config
registry=https://registry.example.com/
@myorg:registry=https://myorg.registry.com/
save-exact=true
`
            );

            // Parse
            const parser = new PackageJsonParser({relativeTo: dir.path});
            const results: Json.Document[] = [];
            for await (const result of parser.parse(path.join(dir.path, 'package.json'))) {
                results.push(result as Json.Document);
            }

            expect(results).toHaveLength(1);
            const marker = findNodeResolutionResult(results[0]);
            expect(marker).toBeDefined();

            // Should have npmrcConfigs with at least the project config
            expect(marker!.npmrcConfigs).toBeDefined();
            const projectConfig = marker!.npmrcConfigs!.find(c => c.scope === NpmrcScope.Project);
            expect(projectConfig).toBeDefined();
            expect(projectConfig!.properties['registry']).toBe('https://registry.example.com/');
            expect(projectConfig!.properties['@myorg:registry']).toBe('https://myorg.registry.com/');
            expect(projectConfig!.properties['save-exact']).toBe('true');
        }, {unsafeCleanup: true});
    });

    test("should parse .npmrc with comments and empty lines", async () => {
        await withDir(async (dir) => {
            // Write package.json
            fs.writeFileSync(
                path.join(dir.path, 'package.json'),
                JSON.stringify({name: "test", version: "1.0.0"}, null, 2)
            );

            // Write .npmrc with various formats
            fs.writeFileSync(
                path.join(dir.path, '.npmrc'),
                `# This is a comment
; This is also a comment

registry=https://example.com/
  key-with-spaces = value-with-spaces
empty-value=
//registry.npmjs.org/:_authToken=\${NPM_TOKEN}
`
            );

            // Parse
            const parser = new PackageJsonParser({relativeTo: dir.path});
            const results: Json.Document[] = [];
            for await (const result of parser.parse(path.join(dir.path, 'package.json'))) {
                results.push(result as Json.Document);
            }

            const marker = findNodeResolutionResult(results[0]);
            expect(marker).toBeDefined();

            const projectConfig = marker!.npmrcConfigs!.find(c => c.scope === NpmrcScope.Project);
            expect(projectConfig).toBeDefined();
            expect(projectConfig!.properties['registry']).toBe('https://example.com/');
            expect(projectConfig!.properties['key-with-spaces']).toBe('value-with-spaces');
            expect(projectConfig!.properties['empty-value']).toBe('');
            expect(projectConfig!.properties['//registry.npmjs.org/:_authToken']).toBe('${NPM_TOKEN}');
        }, {unsafeCleanup: true});
    });

    test("readNpmrcConfigs should read from project directory", async () => {
        await withDir(async (dir) => {
            // Write .npmrc
            fs.writeFileSync(
                path.join(dir.path, '.npmrc'),
                'registry=https://test.registry.com/'
            );

            const configs = await readNpmrcConfigs(dir.path);
            const projectConfig = configs.find((c: Npmrc) => c.scope === NpmrcScope.Project);
            expect(projectConfig).toBeDefined();
            expect(projectConfig!.properties['registry']).toBe('https://test.registry.com/');
        }, {unsafeCleanup: true});
    });

    test("should correctly parse pnpm .pnpm directory with peer dependency context", async () => {
        // Tests that pnpm directory names with peer dependency context (e.g., name@version_peer@version)
        // are correctly parsed by stripping the peer dependency suffix
        await withDir(async (dir) => {
            // Write package.json
            const packageJsonContent = {
                name: "pnpm-project",
                version: "1.0.0",
                dependencies: {
                    "@babel/helper-module-transforms": "^7.28.3"
                }
            };
            fs.writeFileSync(
                path.join(dir.path, 'package.json'),
                JSON.stringify(packageJsonContent, null, 2)
            );

            // Create pnpm-style node_modules structure
            // pnpm stores packages in node_modules/.pnpm/<name>@<version>_<peer-context>/node_modules/<name>/
            const pnpmDir = path.join(dir.path, 'node_modules', '.pnpm');
            fs.mkdirSync(pnpmDir, {recursive: true});

            // Create directory with peer dependency context
            const pkgWithPeerDeps = '@babel+helper-module-transforms@7.28.3_@babel+core@7.28.5';
            const pkgInternalDir = path.join(pnpmDir, pkgWithPeerDeps, 'node_modules', '@babel', 'helper-module-transforms');
            fs.mkdirSync(pkgInternalDir, {recursive: true});
            fs.writeFileSync(
                path.join(pkgInternalDir, 'package.json'),
                JSON.stringify({
                    name: "@babel/helper-module-transforms",
                    version: "7.28.3",
                    license: "MIT"
                }, null, 2)
            );

            // Also create a simple package without peer context
            const simplePkg = 'lodash@4.17.21';
            const simplePkgDir = path.join(pnpmDir, simplePkg, 'node_modules', 'lodash');
            fs.mkdirSync(simplePkgDir, {recursive: true});
            fs.writeFileSync(
                path.join(simplePkgDir, 'package.json'),
                JSON.stringify({
                    name: "lodash",
                    version: "4.17.21",
                    license: "MIT"
                }, null, 2)
            );

            // Write pnpm-lock.yaml (empty but valid)
            fs.writeFileSync(
                path.join(dir.path, 'pnpm-lock.yaml'),
                'lockfileVersion: 9.0\n'
            );

            // Parse
            const parser = new PackageJsonParser({relativeTo: dir.path});
            const results: Json.Document[] = [];
            for await (const result of parser.parse(path.join(dir.path, 'package.json'))) {
                results.push(result as Json.Document);
            }

            expect(results).toHaveLength(1);
            const marker = findNodeResolutionResult(results[0]);
            expect(marker).toBeDefined();
            expect(marker!.resolvedDependencies.length).toBeGreaterThanOrEqual(2);

            // Check that the package with peer deps was parsed correctly
            const babelTransforms = marker!.resolvedDependencies.find(
                r => r.name === "@babel/helper-module-transforms"
            );
            expect(babelTransforms).toBeDefined();
            expect(babelTransforms!.version).toBe("7.28.3");  // Should be 7.28.3, not 7.28.5

            // Check simple package too
            const lodash = marker!.resolvedDependencies.find(r => r.name === "lodash");
            expect(lodash).toBeDefined();
            expect(lodash!.version).toBe("4.17.21");
        }, {unsafeCleanup: true});
    });

    test("should find lock file in subdirectory when relativeTo is parent directory", async () => {
        // This tests the scenario where relativeTo is the Git root but package.json
        // and its lock file are in a subdirectory (e.g., a workspace member)
        await withDir(async (rootDir) => {
            // Create a subdirectory structure: rootDir/subproject/
            const subprojectDir = path.join(rootDir.path, 'subproject');
            fs.mkdirSync(subprojectDir);

            // Write package.json in subdirectory
            const packageJsonContent = {
                name: "subproject",
                version: "1.0.0",
                dependencies: {
                    "lodash": "^4.17.0"
                }
            };
            fs.writeFileSync(
                path.join(subprojectDir, 'package.json'),
                JSON.stringify(packageJsonContent, null, 2)
            );

            // Write package-lock.json in subdirectory (not at root)
            const packageLock = {
                name: "subproject",
                version: "1.0.0",
                lockfileVersion: 3,
                packages: {
                    "": {
                        name: "subproject",
                        version: "1.0.0",
                        dependencies: {
                            "lodash": "^4.17.0"
                        }
                    },
                    "node_modules/lodash": {
                        version: "4.17.21",
                        license: "MIT"
                    }
                }
            };
            fs.writeFileSync(
                path.join(subprojectDir, 'package-lock.json'),
                JSON.stringify(packageLock, null, 2)
            );

            // Parse with relativeTo set to root directory (simulating Git root)
            const parser = new PackageJsonParser({relativeTo: rootDir.path});
            const results: Json.Document[] = [];
            for await (const result of parser.parse(path.join(subprojectDir, 'package.json'))) {
                results.push(result as Json.Document);
            }

            expect(results).toHaveLength(1);
            const marker = findNodeResolutionResult(results[0]);
            expect(marker).toBeDefined();
            expect(marker!.name).toBe("subproject");
            // Path should be relative to relativeTo
            expect(marker!.path).toBe(path.normalize("subproject/package.json"));
            // Should have found the lock file in the subdirectory
            expect(marker!.resolvedDependencies.length).toBeGreaterThan(0);
            // Check that dependency is resolved
            const lodashDep = marker!.dependencies.find(d => d.name === "lodash");
            expect(lodashDep?.resolved).toBeDefined();
            expect(lodashDep!.resolved!.version).toBe("4.17.21");
        }, {unsafeCleanup: true});
    });

});
