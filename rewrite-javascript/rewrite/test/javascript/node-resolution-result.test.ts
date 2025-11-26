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
    Dependency,
    findNodeResolutionResult,
    NodeResolutionResultQueries,
    npm,
    packageJson,
    packageLockJson,
    typescript
} from "../../src/javascript";
import {Json} from "../../src/json";
import {withDir} from "tmp-promise";
import {describe} from "@jest/globals";

describe("NodeResolutionResult marker", () => {

    test("should attach NodeResolutionResult marker to package.json", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    {...packageJson(`
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
                    `), afterRecipe: async (doc: Json.Document) => {
                        const nodeResolutionResult = findNodeResolutionResult(doc);
                        expect(nodeResolutionResult).toBeDefined();
                        expect(nodeResolutionResult?.name).toBe("test-project");
                        expect(nodeResolutionResult?.version).toBe("1.0.0");
                        expect(nodeResolutionResult?.description).toBe("Test project");
                    }}
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
                    typescript(`const x = 1;`),
                    {...packageJson(`
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
                    `), afterRecipe: async (doc: Json.Document) => {
                        const nodeResolutionResult = findNodeResolutionResult(doc);
                        expect(nodeResolutionResult).toBeDefined();

                        // Check dependencies
                        expect(nodeResolutionResult!.dependencies).toHaveLength(1);
                        expect(nodeResolutionResult!.dependencies[0].name).toBe("react");
                        expect(nodeResolutionResult!.dependencies[0].versionConstraint).toBe("^18.2.0");

                        // Check devDependencies
                        expect(nodeResolutionResult!.devDependencies).toHaveLength(2);
                        expect(nodeResolutionResult!.devDependencies.map(d => d.name)).toContain("jest");
                        expect(nodeResolutionResult!.devDependencies.map(d => d.name)).toContain("typescript");

                        // Check peerDependencies
                        expect(nodeResolutionResult!.peerDependencies).toHaveLength(1);
                        expect(nodeResolutionResult!.peerDependencies[0].name).toBe("react-dom");
                        expect(nodeResolutionResult!.peerDependencies[0].versionConstraint).toBe("^18.2.0");

                        // Check optionalDependencies
                        expect(nodeResolutionResult!.optionalDependencies).toHaveLength(1);
                        expect(nodeResolutionResult!.optionalDependencies[0].name).toBe("fsevents");

                        // Check bundledDependencies
                        expect(nodeResolutionResult!.bundledDependencies).toHaveLength(1);
                        expect(nodeResolutionResult!.bundledDependencies[0].name).toBe("bundled-package");
                    }}
                )
            );
        }, {unsafeCleanup: true});
    });

    test("NodeResolutionResultQueries.getAllDependencies should return all dependencies", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    {...packageJson(`
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
                    `), afterRecipe: async (doc: Json.Document) => {
                        const nodeResolutionResult = findNodeResolutionResult(doc);
                        expect(nodeResolutionResult).toBeDefined();

                        const allDeps = NodeResolutionResultQueries.getAllDependencies(nodeResolutionResult!);
                        expect(allDeps).toHaveLength(2);
                        expect(allDeps.map(d => d.name)).toContain("react");
                        expect(allDeps.map(d => d.name)).toContain("jest");
                    }}
                )
            );
        }, {unsafeCleanup: true});
    });

    test("NodeResolutionResultQueries.hasDependency should find dependencies", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    {...packageJson(`
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
                    `), afterRecipe: async (doc: Json.Document) => {
                        const nodeResolutionResult = findNodeResolutionResult(doc);
                        expect(nodeResolutionResult).toBeDefined();

                        // Should find dependency in any scope
                        expect(NodeResolutionResultQueries.hasDependency(nodeResolutionResult!, "react")).toBe(true);
                        expect(NodeResolutionResultQueries.hasDependency(nodeResolutionResult!, "jest")).toBe(true);
                        expect(NodeResolutionResultQueries.hasDependency(nodeResolutionResult!, "nonexistent")).toBe(false);

                        // Should find dependency in specific scope
                        expect(NodeResolutionResultQueries.hasDependency(nodeResolutionResult!, "react", "dependencies")).toBe(true);
                        expect(NodeResolutionResultQueries.hasDependency(nodeResolutionResult!, "react", "devDependencies")).toBe(false);
                        expect(NodeResolutionResultQueries.hasDependency(nodeResolutionResult!, "jest", "devDependencies")).toBe(true);
                    }}
                )
            );
        }, {unsafeCleanup: true});
    });

    test("NodeResolutionResultQueries.findDependency should return dependency details", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    {...packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "react": "^18.2.0"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                        const nodeResolutionResult = findNodeResolutionResult(doc);
                        const reactDep = NodeResolutionResultQueries.findDependency(nodeResolutionResult!, "react");

                        expect(reactDep).toBeDefined();
                        expect(reactDep?.name).toBe("react");
                        expect(reactDep?.versionConstraint).toBe("^18.2.0");
                    }}
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
                    typescript(`const x = 1;`),
                    {...packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "engines": {
                                "node": ">=18.0.0",
                                "npm": ">=9.0.0"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                        const nodeResolutionResult = findNodeResolutionResult(doc);
                        expect(nodeResolutionResult).toBeDefined();
                        expect(nodeResolutionResult!.engines).toEqual({
                            "node": ">=18.0.0",
                            "npm": ">=9.0.0"
                        });
                    }}
                )
            );
        }, {unsafeCleanup: true});
    });

    test("NodeResolutionResultQueries.findDependencies should filter by predicate", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    {...packageJson(`
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
                    `), afterRecipe: async (doc: Json.Document) => {
                        const nodeResolutionResult = findNodeResolutionResult(doc);
                        expect(nodeResolutionResult).toBeDefined();

                        // Find all dependencies starting with 'react'
                        const reactDeps = NodeResolutionResultQueries.findDependencies(
                            nodeResolutionResult!,
                            dep => dep.name.startsWith('react')
                        );
                        expect(reactDeps).toHaveLength(1);
                        expect(reactDeps[0].name).toBe('react');

                        // Find all dependencies with version constraint starting with '^29'
                        const v29Deps = NodeResolutionResultQueries.findDependencies(
                            nodeResolutionResult!,
                            dep => dep.versionConstraint.startsWith('^29')
                        );
                        expect(v29Deps).toHaveLength(1);
                        expect(v29Deps[0].name).toBe('jest');
                    }}
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
                    typescript(`const x = 1;`),
                    {...packageJson(`
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
                    `), afterRecipe: async (doc: Json.Document) => {
                        const nodeResolutionResult = findNodeResolutionResult(doc);
                        expect(nodeResolutionResult).toBeDefined();

                        // Both dependencies and peerDependencies have react@^18.2.0
                        // They should be the same instance due to deduplication
                        const depReact = nodeResolutionResult!.dependencies[0];
                        const peerReact = nodeResolutionResult!.peerDependencies[0];

                        expect(depReact.name).toBe("react");
                        expect(peerReact.name).toBe("react");
                        expect(depReact.versionConstraint).toBe("^18.2.0");
                        expect(peerReact.versionConstraint).toBe("^18.2.0");

                        // Should be the exact same object reference
                        expect(depReact).toBe(peerReact);
                    }}
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
                    typescript(`const x = 1;`),
                    {...packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "lodash": "^4.17.0"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                        const nodeResolutionResult = findNodeResolutionResult(doc);
                        expect(nodeResolutionResult).toBeDefined();
                        expect(nodeResolutionResult!.resolvedDependencies.length).toBeGreaterThan(0);

                        // Resolve using the helper
                        const resolvedLodash = NodeResolutionResultQueries.getResolvedDependency(nodeResolutionResult!, "lodash");
                        expect(resolvedLodash).toBeDefined();
                        expect(resolvedLodash!.name).toBe("lodash");
                        expect(resolvedLodash!.version).toBe("4.17.20");
                        expect(resolvedLodash!.license).toBe("MIT");
                    }},
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
                                    "version": "4.17.20",
                                    "resolved": "https://registry.npmjs.org/lodash/-/lodash-4.17.20.tgz",
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
                    typescript(`const x = 1;`),
                    {...packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "express": "^4.18.0"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                        const nodeResolutionResult = findNodeResolutionResult(doc);
                        expect(nodeResolutionResult).toBeDefined();
                        expect(nodeResolutionResult!.resolvedDependencies.length).toBeGreaterThan(0);

                        // Resolve express using the helper
                        const resolvedExpress = NodeResolutionResultQueries.getResolvedDependency(nodeResolutionResult!, "express");
                        expect(resolvedExpress).toBeDefined();
                        expect(resolvedExpress!.name).toBe("express");
                        expect(resolvedExpress!.version).toBe("4.18.2");

                        // Express should have transitive dependencies
                        expect(resolvedExpress!.dependencies).toBeDefined();
                        expect(resolvedExpress!.dependencies!.length).toBeGreaterThan(0);

                        // body-parser should also be in the resolved dependencies list
                        const resolvedBodyParser = NodeResolutionResultQueries.getResolvedDependency(nodeResolutionResult!, "body-parser");
                        expect(resolvedBodyParser).toBeDefined();
                        expect(resolvedBodyParser!.name).toBe("body-parser");
                        expect(resolvedBodyParser!.version).toBe("1.20.1");
                    }},
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
                    typescript(`const x = 1;`),
                    {...packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "chalk": "^4.1.0",
                                "yargs": "^17.0.0"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                        const nodeResolutionResult = findNodeResolutionResult(doc);
                        expect(nodeResolutionResult).toBeDefined();
                        expect(nodeResolutionResult!.resolvedDependencies.length).toBeGreaterThan(0);

                        // Resolve chalk and yargs using the helper
                        const resolvedChalk = NodeResolutionResultQueries.getResolvedDependency(nodeResolutionResult!, "chalk");
                        const resolvedYargs = NodeResolutionResultQueries.getResolvedDependency(nodeResolutionResult!, "yargs");
                        expect(resolvedChalk).toBeDefined();
                        expect(resolvedYargs).toBeDefined();

                        // Both chalk and yargs depend on 'supports-color@^7.1.0' in our mock lock file
                        const supportsColorFromChalk = resolvedChalk!.dependencies!.find((d: Dependency) => d.name === "supports-color");
                        const supportsColorFromYargs = resolvedYargs!.dependencies!.find((d: Dependency) => d.name === "supports-color");
                        expect(supportsColorFromChalk).toBeDefined();
                        expect(supportsColorFromYargs).toBeDefined();

                        // The Dependency objects should be the same instance (deduplication)
                        expect(supportsColorFromChalk).toBe(supportsColorFromYargs);

                        // supports-color should be in the resolved dependencies list
                        const resolvedSupportsColor = NodeResolutionResultQueries.getResolvedDependency(nodeResolutionResult!, "supports-color");
                        expect(resolvedSupportsColor).toBeDefined();
                        expect(resolvedSupportsColor!.version).toBe("7.2.0");
                    }},
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

    test("NodeResolutionResultQueries.resolve and findResolved should work correctly", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    {...packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "lodash": "^4.17.0"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                        const nodeResolutionResult = findNodeResolutionResult(doc);
                        expect(nodeResolutionResult).toBeDefined();

                        // Test getResolvedDependency() helper
                        const resolvedViaHelper = NodeResolutionResultQueries.getResolvedDependency(nodeResolutionResult!, "lodash");
                        expect(resolvedViaHelper).toBeDefined();
                        expect(resolvedViaHelper!.version).toBe("4.17.21");

                        // Test findResolved() convenience method
                        const resolvedViaFindResolved = NodeResolutionResultQueries.findResolved(nodeResolutionResult!, "lodash");
                        expect(resolvedViaFindResolved).toBeDefined();
                        expect(resolvedViaFindResolved!.version).toBe("4.17.21");

                        // Both should return the same instance
                        expect(resolvedViaHelper).toBe(resolvedViaFindResolved);

                        // Test with non-existent package
                        const nonExistent = NodeResolutionResultQueries.findResolved(nodeResolutionResult!, "nonexistent");
                        expect(nonExistent).toBeUndefined();
                    }},
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

    test("should include engines in ResolvedDependency", async () => {
        const spec = new RecipeSpec();
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    {...packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "typescript": "^5.0.0"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                        const nodeResolutionResult = findNodeResolutionResult(doc);
                        expect(nodeResolutionResult).toBeDefined();

                        // Use typescript which has engine requirements
                        const resolvedPkg = NodeResolutionResultQueries.findResolved(nodeResolutionResult!, "typescript");
                        expect(resolvedPkg).toBeDefined();
                        expect(resolvedPkg!.engines).toEqual({
                            "node": ">=14.17"
                        });
                    }},
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
