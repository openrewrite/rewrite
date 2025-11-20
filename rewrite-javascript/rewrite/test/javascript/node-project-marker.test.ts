// noinspection JSUnusedLocalSymbols,TypeScriptMissingConfigOption,TypeScriptCheckImport

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
import {RecipeSpec} from "../../src/test";
import {
    DependencyScope,
    findNodeProject,
    JS,
    NodeProject,
    NodeProjectQueries,
    npm,
    packageJson,
    typescript
} from "../../src/javascript";
import {withDir} from "tmp-promise";
import {describe} from "@jest/globals";

describe("NodeProject marker", () => {

    test("should attach NodeProject marker when package.json exists", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {...typescript(`const x = 1;`), afterRecipe: async (cu: JS.CompilationUnit) => {
                        const nodeProject = findNodeProject(cu);
                        expect(nodeProject).toBeDefined();
                        expect(nodeProject?.name).toBe("test-project");
                        expect(nodeProject?.version).toBe("1.0.0");
                        expect(nodeProject?.description).toBe("Test project");
                        expect(nodeProject?.packageJsonPath).toContain("package.json");
                    }},
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "description": "Test project",
                            "dependencies": {
                                "react": "^18.2.0",
                                "typescript": "^5.0.0"
                            },
                            "devDependencies": {
                                "jest": "^29.0.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should parse all dependency scopes correctly", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {...typescript(`const x = 1;`), afterRecipe: async (cu: JS.CompilationUnit) => {
                        const nodeProject = findNodeProject(cu);
                        expect(nodeProject).toBeDefined();

                        // Check dependencies
                        expect(nodeProject!.dependencies).toHaveLength(1);
                        expect(nodeProject!.dependencies[0].name).toBe("react");
                        expect(nodeProject!.dependencies[0].version).toBe("^18.2.0");
                        expect(nodeProject!.dependencies[0].scope).toBe(DependencyScope.Dependencies);
                        expect(nodeProject!.dependencies[0].depth).toBe(0);

                        // Check devDependencies
                        expect(nodeProject!.devDependencies).toHaveLength(2);
                        expect(nodeProject!.devDependencies[0].scope).toBe(DependencyScope.DevDependencies);

                        // Check peerDependencies
                        expect(nodeProject!.peerDependencies).toHaveLength(1);
                        expect(nodeProject!.peerDependencies[0].name).toBe("react-dom");
                        expect(nodeProject!.peerDependencies[0].scope).toBe(DependencyScope.PeerDependencies);

                        // Check optionalDependencies
                        expect(nodeProject!.optionalDependencies).toHaveLength(1);
                        expect(nodeProject!.optionalDependencies[0].scope).toBe(DependencyScope.OptionalDependencies);

                        // Check bundledDependencies
                        expect(nodeProject!.bundledDependencies).toHaveLength(1);
                        expect(nodeProject!.bundledDependencies[0].scope).toBe(DependencyScope.BundledDependencies);
                    }},
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "react": "^18.2.0"
                            },
                            "devDependencies": {
                                "jest": "^29.0.0",
                                "typescript": "^5.0.0"
                            },
                            "peerDependencies": {
                                "react-dom": "^18.2.0"
                            },
                            "optionalDependencies": {
                                "fsevents": "^2.3.0"
                            },
                            "bundledDependencies": ["bundled-package"]
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should share same marker instance across multiple files", async () => {
        const spec = new RecipeSpec();
        let marker1: NodeProject | undefined;

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {...typescript(`const x = 1;`), path: "file1.ts", afterRecipe: async (cu: JS.CompilationUnit) => {
                        marker1 = findNodeProject(cu);
                        expect(marker1).toBeDefined();
                    }},
                    {...typescript(`const y = 2;`), path: "file2.ts", afterRecipe: async (cu: JS.CompilationUnit) => {
                        const marker2 = findNodeProject(cu);
                        expect(marker2).toBeDefined();

                        // Should be the exact same object reference
                        expect(marker1).toBe(marker2);
                    }},
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "react": "^18.2.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("NodeProjectQueries.getAllDependencies should return all dependencies", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {...typescript(`const x = 1;`), afterRecipe: async (cu: JS.CompilationUnit) => {
                        const nodeProject = findNodeProject(cu);
                        expect(nodeProject).toBeDefined();

                        const allDeps = NodeProjectQueries.getAllDependencies(nodeProject!);
                        expect(allDeps).toHaveLength(2);
                        expect(allDeps.map(d => d.name)).toContain("react");
                        expect(allDeps.map(d => d.name)).toContain("jest");
                    }},
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "react": "^18.2.0"
                            },
                            "devDependencies": {
                                "jest": "^29.0.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("NodeProjectQueries.hasDependency should find dependencies", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {...typescript(`const x = 1;`), afterRecipe: async (cu: JS.CompilationUnit) => {
                        const nodeProject = findNodeProject(cu);
                        expect(nodeProject).toBeDefined();

                        // Should find dependency in any scope
                        expect(NodeProjectQueries.hasDependency(nodeProject!, "react")).toBe(true);
                        expect(NodeProjectQueries.hasDependency(nodeProject!, "jest")).toBe(true);
                        expect(NodeProjectQueries.hasDependency(nodeProject!, "nonexistent")).toBe(false);

                        // Should find dependency in specific scope
                        expect(NodeProjectQueries.hasDependency(nodeProject!, "react", DependencyScope.Dependencies)).toBe(true);
                        expect(NodeProjectQueries.hasDependency(nodeProject!, "react", DependencyScope.DevDependencies)).toBe(false);
                        expect(NodeProjectQueries.hasDependency(nodeProject!, "jest", DependencyScope.DevDependencies)).toBe(true);
                    }},
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "react": "^18.2.0"
                            },
                            "devDependencies": {
                                "jest": "^29.0.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("NodeProjectQueries.findDependency should return dependency details", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {...typescript(`const x = 1;`), afterRecipe: async (cu: JS.CompilationUnit) => {
                        const nodeProject = findNodeProject(cu);
                        const reactDep = NodeProjectQueries.findDependency(nodeProject!, "react");

                        expect(reactDep).toBeDefined();
                        expect(reactDep?.name).toBe("react");
                        expect(reactDep?.version).toBe("^18.2.0");
                        expect(reactDep?.scope).toBe(DependencyScope.Dependencies);
                        expect(reactDep?.depth).toBe(0);
                    }},
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "react": "^18.2.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should handle package.json with scripts and engines", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {...typescript(`const x = 1;`), afterRecipe: async (cu: JS.CompilationUnit) => {
                        const nodeProject = findNodeProject(cu);
                        expect(nodeProject).toBeDefined();
                        expect(nodeProject!.scripts).toEqual({
                            "build": "tsc",
                            "test": "jest"
                        });
                        expect(nodeProject!.engines).toEqual({
                            "node": ">=18.0.0",
                            "npm": ">=9.0.0"
                        });
                        expect(nodeProject!.repository).toEqual({
                            type: "git",
                            url: "https://github.com/test/test-project.git"
                        });
                    }},
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "scripts": {
                                "build": "tsc",
                                "test": "jest"
                            },
                            "engines": {
                                "node": ">=18.0.0",
                                "npm": ">=9.0.0"
                            },
                            "repository": {
                                "type": "git",
                                "url": "https://github.com/test/test-project.git"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should not attach marker when package.json does not exist", async () => {
        const spec = new RecipeSpec();
        await spec.rewriteRun(
            {...typescript(`const x = 1;`), afterRecipe: async (cu: JS.CompilationUnit) => {
                // Should not have NodeProject marker
                const nodeProject = findNodeProject(cu);
                expect(nodeProject).toBeUndefined();
            }}
        );
    });

    test("NodeProjectQueries.findDependencies should filter by predicate", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {...typescript(`const x = 1;`), afterRecipe: async (cu: JS.CompilationUnit) => {
                        const nodeProject = findNodeProject(cu);
                        expect(nodeProject).toBeDefined();

                        // Find all dependencies starting with 'react'
                        const reactDeps = NodeProjectQueries.findDependencies(
                            nodeProject!,
                            dep => dep.name.startsWith('react')
                        );
                        expect(reactDeps).toHaveLength(1);
                        expect(reactDeps[0].name).toBe('react');

                        // Find all dev dependencies
                        const devDeps = NodeProjectQueries.findDependencies(
                            nodeProject!,
                            dep => dep.scope === DependencyScope.DevDependencies
                        );
                        expect(devDeps).toHaveLength(1);
                        expect(devDeps[0].name).toBe('jest');
                    }},
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "react": "^18.2.0",
                                "vue": "^3.0.0"
                            },
                            "devDependencies": {
                                "jest": "^29.0.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });
});
