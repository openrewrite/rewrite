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
    createNodeResolutionResultMarker,
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

describe("NodeResolutionResult marker", () => {

    test.skip("should attach NodeResolutionResult marker to package.json", async () => {
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
                        }
                    }
                )
            );
        }, {unsafeCleanup: true});
    });

    test.skip("should parse all dependency scopes correctly", async () => {
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
                        }
                    }
                )
            );
        }, {unsafeCleanup: true});
    });

    test.skip("NodeResolutionResultQueries.getAllDependencies should return all dependencies", async () => {
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
                        }
                    }
                )
            );
        }, {unsafeCleanup: true});
    });

    test.skip("NodeResolutionResultQueries.hasDependency should find dependencies", async () => {
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
                        }
                    }
                )
            );
        }, {unsafeCleanup: true});
    });

    test.skip("NodeResolutionResultQueries.findDependency should return dependency details", async () => {
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
                                "react": "^18.2.0"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                            const nodeResolutionResult = findNodeResolutionResult(doc);
                            const reactDep = NodeResolutionResultQueries.findDependency(nodeResolutionResult!, "react");

                            expect(reactDep).toBeDefined();
                            expect(reactDep?.name).toBe("react");
                            expect(reactDep?.versionConstraint).toBe("^18.2.0");
                        }
                    }
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
                    {
                        ...packageJson(`
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
                        }
                    }
                )
            );
        }, {unsafeCleanup: true});
    });

    test.skip("NodeResolutionResultQueries.findDependencies should filter by predicate", async () => {
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
                        }
                    }
                )
            );
        }, {unsafeCleanup: true});
    });

    test.skip("should deduplicate identical dependency requests", async () => {
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
                        }
                    }
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
                            const nodeResolutionResult = findNodeResolutionResult(doc);
                            expect(nodeResolutionResult).toBeDefined();
                            expect(nodeResolutionResult!.resolvedDependencies.length).toBeGreaterThan(0);

                            // Resolve using the resolved property on Dependency
                            const lodashDep = nodeResolutionResult!.dependencies.find(d => d.name === "lodash");
                            expect(lodashDep?.resolved).toBeDefined();
                            expect(lodashDep!.resolved!.name).toBe("lodash");
                            expect(lodashDep!.resolved!.version).toBe("4.17.20");
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
                    {
                        ...packageJson(`
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

                            // Resolve express using the resolved property
                            const expressDep = nodeResolutionResult!.dependencies.find(d => d.name === "express");
                            const resolvedExpress = expressDep?.resolved;
                            expect(resolvedExpress).toBeDefined();
                            expect(resolvedExpress!.name).toBe("express");
                            expect(resolvedExpress!.version).toBe("4.18.2");

                            // Express should have transitive dependencies
                            expect(resolvedExpress!.dependencies).toBeDefined();
                            expect(resolvedExpress!.dependencies!.length).toBeGreaterThan(0);

                            // Navigate to body-parser through express's dependencies using resolved property
                            const bodyParserDep = resolvedExpress!.dependencies!.find(d => d.name === "body-parser");
                            expect(bodyParserDep?.resolved).toBeDefined();
                            expect(bodyParserDep!.resolved!.name).toBe("body-parser");
                            expect(bodyParserDep!.resolved!.version).toBe("1.20.1");
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
                    {
                        ...packageJson(`
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

                            // Resolve chalk and yargs using the resolved property
                            const chalkDep = nodeResolutionResult!.dependencies.find(d => d.name === "chalk");
                            const yargsDep = nodeResolutionResult!.dependencies.find(d => d.name === "yargs");
                            expect(chalkDep?.resolved).toBeDefined();
                            expect(yargsDep?.resolved).toBeDefined();

                            // Both chalk and yargs depend on 'supports-color@^7.1.0' in our mock lock file
                            const supportsColorFromChalk = chalkDep!.resolved!.dependencies!.find((d: Dependency) => d.name === "supports-color");
                            const supportsColorFromYargs = yargsDep!.resolved!.dependencies!.find((d: Dependency) => d.name === "supports-color");
                            expect(supportsColorFromChalk).toBeDefined();
                            expect(supportsColorFromYargs).toBeDefined();

                            // Both should have the same version constraint
                            expect(supportsColorFromChalk!.versionConstraint).toBe("^7.1.0");
                            expect(supportsColorFromYargs!.versionConstraint).toBe("^7.1.0");

                            // The resolved ResolvedDependency objects should be the same instance (deduplication)
                            expect(supportsColorFromChalk!.resolved).toBe(supportsColorFromYargs!.resolved);

                            // supports-color should resolve to 7.2.0 via the resolved property
                            expect(supportsColorFromChalk!.resolved).toBeDefined();
                            expect(supportsColorFromChalk!.resolved!.version).toBe("7.2.0");
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

    test("should navigate dependency tree using resolved property", async () => {
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
                                "express": "^4.18.0"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                            const nodeResolutionResult = findNodeResolutionResult(doc);
                            expect(nodeResolutionResult).toBeDefined();

                            // Navigate: project -> express -> body-parser -> bytes
                            const express = nodeResolutionResult!.dependencies.find(d => d.name === "express")?.resolved;
                            expect(express).toBeDefined();
                            expect(express!.version).toBe("4.18.2");

                            const bodyParser = express!.dependencies?.find(d => d.name === "body-parser")?.resolved;
                            expect(bodyParser).toBeDefined();
                            expect(bodyParser!.version).toBe("1.20.1");

                            const bytes = bodyParser!.dependencies?.find(d => d.name === "bytes")?.resolved;
                            expect(bytes).toBeDefined();
                            expect(bytes!.version).toBe("3.1.2");

                            // Non-existent transitive dependency returns undefined
                            const nonExistent = express!.dependencies?.find(d => d.name === "nonexistent")?.resolved;
                            expect(nonExistent).toBeUndefined();
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
                                        "express": "^4.18.0"
                                    }
                                },
                                "node_modules/express": {
                                    "version": "4.18.2",
                                    "license": "MIT",
                                    "dependencies": {
                                        "body-parser": "1.20.1"
                                    }
                                },
                                "node_modules/body-parser": {
                                    "version": "1.20.1",
                                    "license": "MIT",
                                    "dependencies": {
                                        "bytes": "3.1.2"
                                    }
                                },
                                "node_modules/bytes": {
                                    "version": "3.1.2",
                                    "license": "MIT"
                                }
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("getAllResolvedVersions should return multiple versions of same package", () => {
        // Test the marker creation directly without npm install since we need fake packages
        const packageJsonContent = {
            name: "test-project",
            version: "1.0.0",
            dependencies: {
                "package-a": "^1.0.0",
                "package-b": "^1.0.0"
            }
        };

        // This lock file has two versions of lodash due to different dependency constraints
        const packageLockContent = {
            name: "test-project",
            version: "1.0.0",
            lockfileVersion: 3,
            packages: {
                "": {
                    name: "test-project",
                    version: "1.0.0",
                    dependencies: {
                        "package-a": "^1.0.0",
                        "package-b": "^1.0.0"
                    }
                },
                "node_modules/package-a": {
                    version: "1.0.0",
                    dependencies: {
                        lodash: "^4.17.0"
                    }
                },
                "node_modules/package-b": {
                    version: "1.0.0",
                    dependencies: {
                        lodash: "^3.10.0"
                    }
                },
                "node_modules/lodash": {
                    version: "4.17.21",
                    license: "MIT"
                },
                "node_modules/package-b/node_modules/lodash": {
                    version: "3.10.1",
                    license: "MIT"
                }
            }
        };

        const marker = createNodeResolutionResultMarker(
            "package.json",
            packageJsonContent,
            packageLockContent
        );

        // Both lodash versions should be in resolvedDependencies
        const lodashVersions = NodeResolutionResultQueries.getAllResolvedVersions(marker, "lodash");
        expect(lodashVersions).toHaveLength(2);
        const versions = lodashVersions.map(l => l.version).sort();
        expect(versions).toEqual(["3.10.1", "4.17.21"]);
    });

    test.skip("should include engines in ResolvedDependency", async () => {
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
                                "typescript": "^5.0.0"
                            }
                        }
                    `), afterRecipe: async (doc: Json.Document) => {
                            const nodeResolutionResult = findNodeResolutionResult(doc);
                            expect(nodeResolutionResult).toBeDefined();

                            // Use typescript which has engine requirements
                            const tsDep = nodeResolutionResult!.dependencies.find(d => d.name === "typescript");
                            expect(tsDep?.resolved).toBeDefined();
                            expect(tsDep!.resolved!.engines).toEqual({
                                "node": ">=14.17"
                            });
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

    test("should normalize engines from legacy array format to object format", () => {
        // Some older packages have engines as an array like ["node >=0.6.0"]
        // instead of the standard object format {"node": ">=0.6.0"}
        const packageJsonContent = {
            name: "test-project",
            version: "1.0.0",
            dependencies: {
                "legacy-package": "^1.0.0"
            }
        };

        const packageLockContent = {
            name: "test-project",
            version: "1.0.0",
            lockfileVersion: 3,
            packages: {
                "": {
                    name: "test-project",
                    version: "1.0.0",
                    dependencies: {
                        "legacy-package": "^1.0.0"
                    }
                },
                "node_modules/legacy-package": {
                    version: "1.0.0",
                    license: "MIT",
                    // Legacy array format for engines
                    engines: [
                        "node >=0.6.0"
                    ]
                }
            }
        };

        const marker = createNodeResolutionResultMarker(
            "package.json",
            packageJsonContent,
            packageLockContent
        );

        const legacyDep = marker.dependencies.find(d => d.name === "legacy-package");
        expect(legacyDep?.resolved).toBeDefined();
        // Should be normalized to object format
        expect(legacyDep!.resolved!.engines).toEqual({
            "node": ">=0.6.0"
        });
    });

    test("should normalize engines with multiple entries from legacy array format", () => {
        const packageJsonContent = {
            name: "test-project",
            version: "1.0.0",
            dependencies: {
                "legacy-package": "^1.0.0"
            }
        };

        const packageLockContent = {
            name: "test-project",
            version: "1.0.0",
            lockfileVersion: 3,
            packages: {
                "": {
                    name: "test-project",
                    version: "1.0.0",
                    dependencies: {
                        "legacy-package": "^1.0.0"
                    }
                },
                "node_modules/legacy-package": {
                    version: "1.0.0",
                    license: "MIT",
                    // Legacy array format with multiple entries
                    engines: [
                        "node >=0.6.0",
                        "npm >=1.0.0"
                    ]
                }
            }
        };

        const marker = createNodeResolutionResultMarker(
            "package.json",
            packageJsonContent,
            packageLockContent
        );

        const legacyDep = marker.dependencies.find(d => d.name === "legacy-package");
        expect(legacyDep?.resolved).toBeDefined();
        // Should be normalized to object format with both entries
        expect(legacyDep!.resolved!.engines).toEqual({
            "node": ">=0.6.0",
            "npm": ">=1.0.0"
        });
    });
});
