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
                        expect(nodeProject!.dependencies[0].versionConstraint).toBe("^18.2.0");

                        // Check devDependencies
                        expect(nodeProject!.devDependencies).toHaveLength(2);
                        expect(nodeProject!.devDependencies.map(d => d.name)).toContain("jest");
                        expect(nodeProject!.devDependencies.map(d => d.name)).toContain("typescript");

                        // Check peerDependencies
                        expect(nodeProject!.peerDependencies).toHaveLength(1);
                        expect(nodeProject!.peerDependencies[0].name).toBe("react-dom");
                        expect(nodeProject!.peerDependencies[0].versionConstraint).toBe("^18.2.0");

                        // Check optionalDependencies
                        expect(nodeProject!.optionalDependencies).toHaveLength(1);
                        expect(nodeProject!.optionalDependencies[0].name).toBe("fsevents");

                        // Check bundledDependencies
                        expect(nodeProject!.bundledDependencies).toHaveLength(1);
                        expect(nodeProject!.bundledDependencies[0].name).toBe("bundled-package");
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
                        expect(NodeProjectQueries.hasDependency(nodeProject!, "react", "dependencies")).toBe(true);
                        expect(NodeProjectQueries.hasDependency(nodeProject!, "react", "devDependencies")).toBe(false);
                        expect(NodeProjectQueries.hasDependency(nodeProject!, "jest", "devDependencies")).toBe(true);
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
                        expect(reactDep?.versionConstraint).toBe("^18.2.0");
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

    test("should handle package.json with engines", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {...typescript(`const x = 1;`), afterRecipe: async (cu: JS.CompilationUnit) => {
                        const nodeProject = findNodeProject(cu);
                        expect(nodeProject).toBeDefined();
                        expect(nodeProject!.engines).toEqual({
                            "node": ">=18.0.0",
                            "npm": ">=9.0.0"
                        });
                    }},
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "engines": {
                                "node": ">=18.0.0",
                                "npm": ">=9.0.0"
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

                        // Find all dependencies with version constraint starting with '^29'
                        const v29Deps = NodeProjectQueries.findDependencies(
                            nodeProject!,
                            dep => dep.versionConstraint.startsWith('^29')
                        );
                        expect(v29Deps).toHaveLength(1);
                        expect(v29Deps[0].name).toBe('jest');
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

    test("should deduplicate identical dependency requests", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {...typescript(`const x = 1;`), afterRecipe: async (cu: JS.CompilationUnit) => {
                        const nodeProject = findNodeProject(cu);
                        expect(nodeProject).toBeDefined();

                        // Both dependencies and peerDependencies have react@^18.2.0
                        // They should be the same instance due to deduplication
                        const depReact = nodeProject!.dependencies[0];
                        const peerReact = nodeProject!.peerDependencies[0];

                        expect(depReact.name).toBe("react");
                        expect(peerReact.name).toBe("react");
                        expect(depReact.versionConstraint).toBe("^18.2.0");
                        expect(peerReact.versionConstraint).toBe("^18.2.0");

                        // Should be the exact same object reference
                        expect(depReact).toBe(peerReact);
                    }},
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "react": "^18.2.0"
                            },
                            "peerDependencies": {
                                "react": "^18.2.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });
});
