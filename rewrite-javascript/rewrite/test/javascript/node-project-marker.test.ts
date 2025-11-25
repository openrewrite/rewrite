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
    packageLockJson,
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

    test("should populate resolutions from package-lock.json", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {...typescript(`const x = 1;`), afterRecipe: async (cu: JS.CompilationUnit) => {
                        const nodeProject = findNodeProject(cu);
                        expect(nodeProject).toBeDefined();
                        expect(nodeProject!.resolutions).toBeDefined();
                        expect(nodeProject!.resolutions!.size).toBeGreaterThan(0);

                        // Find the lodash dependency request
                        const lodashDep = NodeProjectQueries.findDependency(nodeProject!, "lodash");
                        expect(lodashDep).toBeDefined();

                        // Resolve it using the resolutions map
                        const resolvedLodash = nodeProject!.resolutions!.get(lodashDep!);
                        expect(resolvedLodash).toBeDefined();
                        expect(resolvedLodash!.name).toBe("lodash");
                        expect(resolvedLodash!.version).toBe("4.17.21");
                        expect(resolvedLodash!.license).toBe("MIT");
                    }},
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "lodash": "^4.17.0"
                            }
                        }
                    `),
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

    test("should resolve transitive dependencies from package-lock.json", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {...typescript(`const x = 1;`), afterRecipe: async (cu: JS.CompilationUnit) => {
                        const nodeProject = findNodeProject(cu);
                        expect(nodeProject).toBeDefined();
                        expect(nodeProject!.resolutions).toBeDefined();

                        // Find and resolve express
                        const expressDep = NodeProjectQueries.findDependency(nodeProject!, "express");
                        expect(expressDep).toBeDefined();

                        const resolvedExpress = nodeProject!.resolutions!.get(expressDep!);
                        expect(resolvedExpress).toBeDefined();
                        expect(resolvedExpress!.name).toBe("express");
                        expect(resolvedExpress!.version).toBe("4.18.2");

                        // Express should have transitive dependencies
                        expect(resolvedExpress!.dependencies).toBeDefined();
                        expect(resolvedExpress!.dependencies!.length).toBeGreaterThan(0);

                        // Find body-parser in express's dependencies
                        const bodyParserDep = resolvedExpress!.dependencies!.find(d => d.name === "body-parser");
                        expect(bodyParserDep).toBeDefined();

                        // Resolve body-parser
                        const resolvedBodyParser = nodeProject!.resolutions!.get(bodyParserDep!);
                        expect(resolvedBodyParser).toBeDefined();
                        expect(resolvedBodyParser!.name).toBe("body-parser");
                        expect(resolvedBodyParser!.version).toBe("1.20.1");
                    }},
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "express": "^4.18.0"
                            }
                        }
                    `),
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
                                        "express": "^4.18.0"
                                    }
                                },
                                "node_modules/express": {
                                    "version": "4.18.2",
                                    "resolved": "https://registry.npmjs.org/express/-/express-4.18.2.tgz",
                                    "license": "MIT",
                                    "dependencies": {
                                        "body-parser": "1.20.1",
                                        "cookie": "0.5.0"
                                    }
                                },
                                "node_modules/body-parser": {
                                    "version": "1.20.1",
                                    "resolved": "https://registry.npmjs.org/body-parser/-/body-parser-1.20.1.tgz",
                                    "license": "MIT",
                                    "dependencies": {
                                        "bytes": "3.1.2"
                                    }
                                },
                                "node_modules/cookie": {
                                    "version": "0.5.0",
                                    "resolved": "https://registry.npmjs.org/cookie/-/cookie-0.5.0.tgz",
                                    "license": "MIT"
                                },
                                "node_modules/bytes": {
                                    "version": "3.1.2",
                                    "resolved": "https://registry.npmjs.org/bytes/-/bytes-3.1.2.tgz",
                                    "license": "MIT"
                                }
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should reuse ResolvedDependency instances for same name+version", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {...typescript(`const x = 1;`), afterRecipe: async (cu: JS.CompilationUnit) => {
                        const nodeProject = findNodeProject(cu);
                        expect(nodeProject).toBeDefined();
                        expect(nodeProject!.resolutions).toBeDefined();

                        // Both 'chalk' and 'yargs' depend on various packages
                        // We'll verify that identical Dependency requests get deduplicated
                        const chalkDep = NodeProjectQueries.findDependency(nodeProject!, "chalk");
                        const yargsDep = NodeProjectQueries.findDependency(nodeProject!, "yargs");
                        expect(chalkDep).toBeDefined();
                        expect(yargsDep).toBeDefined();

                        const resolvedChalk = nodeProject!.resolutions!.get(chalkDep!);
                        const resolvedYargs = nodeProject!.resolutions!.get(yargsDep!);
                        expect(resolvedChalk).toBeDefined();
                        expect(resolvedYargs).toBeDefined();

                        // Both chalk and yargs depend on 'supports-color@^7.1.0' in our mock lock file
                        const supportsColorFromChalk = resolvedChalk!.dependencies!.find(d => d.name === "supports-color");
                        const supportsColorFromYargs = resolvedYargs!.dependencies!.find(d => d.name === "supports-color");
                        expect(supportsColorFromChalk).toBeDefined();
                        expect(supportsColorFromYargs).toBeDefined();

                        // The Dependency objects should be the same instance (deduplication)
                        expect(supportsColorFromChalk).toBe(supportsColorFromYargs);

                        // And they should resolve to the same ResolvedDependency
                        const resolvedSupportsColorFromChalk = nodeProject!.resolutions!.get(supportsColorFromChalk!);
                        const resolvedSupportsColorFromYargs = nodeProject!.resolutions!.get(supportsColorFromYargs!);
                        expect(resolvedSupportsColorFromChalk).toBe(resolvedSupportsColorFromYargs);
                    }},
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "chalk": "^4.1.0",
                                "yargs": "^17.0.0"
                            }
                        }
                    `),
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
                                        "chalk": "^4.1.0",
                                        "yargs": "^17.0.0"
                                    }
                                },
                                "node_modules/chalk": {
                                    "version": "4.1.2",
                                    "license": "MIT",
                                    "dependencies": {
                                        "supports-color": "^7.1.0"
                                    }
                                },
                                "node_modules/yargs": {
                                    "version": "17.7.2",
                                    "license": "MIT",
                                    "dependencies": {
                                        "supports-color": "^7.1.0"
                                    }
                                },
                                "node_modules/supports-color": {
                                    "version": "7.2.0",
                                    "license": "MIT"
                                }
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("NodeProjectQueries.resolve and findResolved should work correctly", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {...typescript(`const x = 1;`), afterRecipe: async (cu: JS.CompilationUnit) => {
                        const nodeProject = findNodeProject(cu);
                        expect(nodeProject).toBeDefined();

                        // Test resolve() with explicit dependency
                        const lodashDep = NodeProjectQueries.findDependency(nodeProject!, "lodash");
                        const resolvedViaResolve = NodeProjectQueries.resolve(nodeProject!, lodashDep!);
                        expect(resolvedViaResolve).toBeDefined();
                        expect(resolvedViaResolve!.version).toBe("4.17.21");

                        // Test findResolved() convenience method
                        const resolvedViaFindResolved = NodeProjectQueries.findResolved(nodeProject!, "lodash");
                        expect(resolvedViaFindResolved).toBeDefined();
                        expect(resolvedViaFindResolved!.version).toBe("4.17.21");

                        // Both should return the same instance
                        expect(resolvedViaResolve).toBe(resolvedViaFindResolved);

                        // Test with non-existent package
                        const nonExistent = NodeProjectQueries.findResolved(nodeProject!, "nonexistent");
                        expect(nonExistent).toBeUndefined();
                    }},
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "lodash": "^4.17.0"
                            }
                        }
                    `),
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
                                    "license": "MIT"
                                }
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should not have resolutions when package-lock.json is missing", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {...typescript(`const x = 1;`), afterRecipe: async (cu: JS.CompilationUnit) => {
                        const nodeProject = findNodeProject(cu);
                        expect(nodeProject).toBeDefined();
                        // Without package-lock.json, resolutions should be undefined
                        expect(nodeProject!.resolutions).toBeUndefined();
                    }},
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "lodash": "^4.17.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("should include engines in ResolvedDependency", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    {...typescript(`const x = 1;`), afterRecipe: async (cu: JS.CompilationUnit) => {
                        const nodeProject = findNodeProject(cu);
                        expect(nodeProject).toBeDefined();

                        // Use typescript which has engine requirements
                        const resolvedPkg = NodeProjectQueries.findResolved(nodeProject!, "typescript");
                        expect(resolvedPkg).toBeDefined();
                        expect(resolvedPkg!.engines).toEqual({
                            "node": ">=14.17"
                        });
                    }},
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "typescript": "^5.0.0"
                            }
                        }
                    `),
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
                                        "typescript": "^5.0.0"
                                    }
                                },
                                "node_modules/typescript": {
                                    "version": "5.3.3",
                                    "license": "Apache-2.0",
                                    "engines": {
                                        "node": ">=14.17"
                                    }
                                }
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });
});
