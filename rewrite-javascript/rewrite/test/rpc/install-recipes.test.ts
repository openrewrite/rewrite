/*
 * Copyright 2026 the original author or authors.
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
import {withDir} from "tmp-promise";
import * as fs from "fs";
import * as path from "path";
import {createRequire, registerHooks} from "module";
import {ResponseError} from "vscode-jsonrpc/node";
import {RecipeMarketplace} from "../../src";
import {checkHostVersion, InstallRecipes, InstallRecipesResponse} from "../../src/rpc/request/install-recipes";
import {GetMarketplace, GetMarketplaceResponseRow} from "../../src/rpc/request/get-marketplace";

describe("InstallRecipes", () => {

    type RequestHandler = (request: InstallRecipes) => Promise<InstallRecipesResponse>;

    function captureHandler(installDir: string, marketplace: RecipeMarketplace,
                            recipeOrigin: Map<string, string> = new Map(), logger?: any): RequestHandler {
        let capturedHandler: RequestHandler | undefined;

        const dummyConnection = {
            onRequest: (_requestType: any, handler: RequestHandler) => {
                capturedHandler = handler;
            }
        } as any;

        InstallRecipes.handle(dummyConnection, installDir, marketplace, recipeOrigin, logger);

        if (!capturedHandler) {
            throw new Error("Handler was not registered");
        }

        return capturedHandler;
    }

    function captureGetMarketplace(marketplace: RecipeMarketplace,
                                   recipeOrigin: Map<string, string>): () => Promise<GetMarketplaceResponseRow[]> {
        let capturedHandler: ((token?: any) => Promise<GetMarketplaceResponseRow[]>) | undefined;

        const dummyConnection = {
            onRequest: (_requestType: any, handler: any) => {
                capturedHandler = handler;
            }
        } as any;

        GetMarketplace.handle(dummyConnection, marketplace, recipeOrigin);

        if (!capturedHandler) {
            throw new Error("GetMarketplace handler was not registered");
        }

        return () => capturedHandler!();
    }

    describe("local file path installation", () => {

        test("installs recipe module from file path", async () => {
            await withDir(async (dir) => {
                // given
                const recipeModulePath = path.join(dir.path, "test-recipe.js");
                fs.writeFileSync(recipeModulePath, `
                    module.exports = {
                        activate: async function(marketplace) {
                            await marketplace.install(
                                class TestRecipe {
                                    async descriptor() {
                                        return {
                                            name: "test.recipe",
                                            displayName: "Test Recipe",
                                            description: "A test recipe"
                                        };
                                    }
                                },
                                [{displayName: "Test"}]
                            );
                        }
                    };
                `);

                const installDir = path.join(dir.path, "recipes");
                const marketplace = new RecipeMarketplace();
                const handler = captureHandler(installDir, marketplace);

                // when
                const response = await handler({recipes: recipeModulePath} as any);

                // then
                expect(response.recipesInstalled).toBe(1);
                expect(marketplace.allRecipes().length).toBe(1);
                expect(marketplace.allRecipes()[0].name).toBe("test.recipe");
                expect(marketplace.allRecipes()[0].options).toBeUndefined();
            }, {unsafeCleanup: true});
        });

        test("does not return version for file path installation", async () => {
            await withDir(async (dir) => {
                // given
                const recipeModulePath = path.join(dir.path, "simple-recipe.js");
                fs.writeFileSync(recipeModulePath, `
                    module.exports = {
                        activate: function(marketplace) {}
                    };
                `);

                const installDir = path.join(dir.path, "recipes");
                const marketplace = new RecipeMarketplace();
                const handler = captureHandler(installDir, marketplace);

                // when
                const response = await handler({recipes: recipeModulePath} as any);

                // then
                expect(response.version).toBeUndefined();
            }, {unsafeCleanup: true});
        });

        test("returns correct count for multiple recipes", async () => {
            await withDir(async (dir) => {
                // given
                const recipeModulePath = path.join(dir.path, "multi-recipe.js");
                fs.writeFileSync(recipeModulePath, `
                    module.exports = {
                        activate: async function(marketplace) {
                            await marketplace.install(
                                class Recipe1 {
                                    async descriptor() {
                                        return { name: "multi.recipe1", displayName: "Recipe 1", description: "" };
                                    }
                                },
                                [{displayName: "Multi"}]
                            );
                            await marketplace.install(
                                class Recipe2 {
                                    async descriptor() {
                                        return { name: "multi.recipe2", displayName: "Recipe 2", description: "" };
                                    }
                                },
                                [{displayName: "Multi"}]
                            );
                            await marketplace.install(
                                class Recipe3 {
                                    async descriptor() {
                                        return { name: "multi.recipe3", displayName: "Recipe 3", description: "" };
                                    }
                                },
                                [{displayName: "Multi"}]
                            );
                        }
                    };
                `);

                const installDir = path.join(dir.path, "recipes");
                const marketplace = new RecipeMarketplace();
                const handler = captureHandler(installDir, marketplace);

                // when
                const response = await handler({recipes: recipeModulePath} as any);

                // then
                expect(response.recipesInstalled).toBe(3);
                expect(marketplace.allRecipes().length).toBe(3);
            }, {unsafeCleanup: true});
        });
    });

    describe("shared dependencies", () => {
        // `mutative` stands in for `@openrewrite/rewrite`, which vitest loads from source outside Node's
        // require.cache. The same mapping shares both.
        const hostRequire = createRequire(path.resolve(__dirname, "../../src/rpc/request/install-recipes.ts"));
        const hostMutativeRoot = path.dirname(hostRequire.resolve("mutative/package.json"));

        function writeRecipePackage(packageDir: string) {
            fs.mkdirSync(packageDir, {recursive: true});
            fs.writeFileSync(path.join(packageDir, "package.json"), JSON.stringify({name: "linked-recipes", main: "index.js"}));
            fs.writeFileSync(path.join(packageDir, "index.js"), `
                module.exports = { activate() {}, mutative: require("mutative") };
            `);
        }

        test("shares the host's copy with a package reached through a symlink", async () => {
            await withDir(async (dir) => {
                const base = fs.realpathSync(dir.path);
                const hostMutative = hostRequire("mutative");
                const install = (recipes: string) =>
                    captureHandler(path.join(base, "install"), new RecipeMarketplace())({recipes} as any);
                // In both cases `mutative` is reachable only from the real path of the package's entry file.

                const linkedPackage = path.join(base, "linked-package");
                writeRecipePackage(path.join(base, "a", "packages", "recipes"));
                fs.cpSync(hostMutativeRoot, path.join(base, "a", "node_modules", "mutative"), {recursive: true});
                fs.symlinkSync(path.join(base, "a", "packages", "recipes"), linkedPackage, "dir");
                await install(linkedPackage);
                expect(hostRequire(linkedPackage).mutative).toBe(hostMutative);

                const linkedEntry = path.join(base, "linked-entry");
                writeRecipePackage(path.join(base, "b", "packages", "recipes"));
                fs.cpSync(hostMutativeRoot, path.join(base, "b", "node_modules", "mutative"), {recursive: true});
                fs.mkdirSync(linkedEntry);
                fs.writeFileSync(path.join(linkedEntry, "package.json"), JSON.stringify({name: "linked-entry", main: "lib/index.js"}));
                fs.symlinkSync(path.join(base, "b", "packages", "recipes"), path.join(linkedEntry, "lib"), "dir");
                await install(linkedEntry);
                expect(hostRequire(linkedEntry).mutative).toBe(hostMutative);
            }, {unsafeCleanup: true});
        });

        test("shares the host's copy with a package whose dependency is a symlink", async () => {
            await withDir(async (dir) => {
                const base = fs.realpathSync(dir.path);
                const packageDir = path.join(base, "recipes");
                writeRecipePackage(packageDir);
                const linkedMutative = path.join(base, "mutative-build");
                fs.cpSync(hostMutativeRoot, linkedMutative, {recursive: true});
                fs.mkdirSync(path.join(packageDir, "node_modules"));
                fs.symlinkSync(linkedMutative, path.join(packageDir, "node_modules", "mutative"), "dir");

                const hostMutative = hostRequire("mutative");
                await captureHandler(path.join(base, "install"), new RecipeMarketplace())({recipes: packageDir} as any);

                expect(hostRequire(packageDir).mutative).toBe(hostMutative);
            }, {unsafeCleanup: true});
        });

        // Below Node 22.15 the copy is shared by mapping file paths, which needs the host's layout.
        test.runIf(typeof registerHooks === "function")("shares the host's copy with a package whose copy has another file layout", async () => {
            await withDir(async (dir) => {
                const base = fs.realpathSync(dir.path);
                const packageDir = path.join(base, "recipes");
                writeRecipePackage(packageDir);
                const ownMutative = path.join(packageDir, "node_modules", "mutative");
                fs.cpSync(path.join(hostMutativeRoot, "dist"), path.join(ownMutative, "lib"), {recursive: true});
                fs.writeFileSync(path.join(ownMutative, "package.json"), JSON.stringify({name: "mutative", main: "lib/index.js"}));

                const hostMutative = hostRequire("mutative");
                await captureHandler(path.join(base, "install"), new RecipeMarketplace())({recipes: packageDir} as any);

                expect(hostRequire(packageDir).mutative).toBe(hostMutative);
            }, {unsafeCleanup: true});
        });
    });

    describe("host version", () => {
        const peerRange = (range: string) =>
            ({name: "recipes", version: "1.0.0", peerDependencies: {"@openrewrite/rewrite": range}});

        test("warns when a development build of the host is outside the package's peer range", async () => {
            await withDir(async (dir) => {
                const packageDir = path.join(dir.path, "recipes");
                fs.mkdirSync(packageDir);
                fs.writeFileSync(path.join(packageDir, "package.json"), JSON.stringify({...peerRange("^8.97.0"), main: "index.js"}));
                fs.writeFileSync(path.join(packageDir, "index.js"), "module.exports = { activate() {} };");
                const warnings: string[] = [];
                const logger = {info() {}, log() {}, error() {}, warn: (m: string) => warnings.push(m)};

                await captureHandler(path.join(dir.path, "install"), new RecipeMarketplace(), new Map(), logger)({recipes: packageDir} as any);

                expect(warnings).toContain("recipes@1.0.0 requires @openrewrite/rewrite ^8.97.0, but this host runs 0.0.0");
            }, {unsafeCleanup: true});
        });

        test("refuses a released host outside the package's peer range", () => {
            let refusal: any;
            try {
                checkHostVersion(peerRange("^8.98.0"), "8.97.0");
            } catch (e) {
                refusal = e;
            }

            // The wire contract with IncompatibleRecipeBundleException in rewrite-core
            expect(refusal).toBeInstanceOf(ResponseError);
            expect(refusal.code).toBe(1001);
            expect(refusal.message).toBe("recipes@1.0.0 requires @openrewrite/rewrite ^8.98.0, but this host runs 8.97.0");
            expect(JSON.parse(refusal.data)).toEqual(
                {packageName: "recipes", packageVersion: "1.0.0", requiredRange: "^8.98.0", hostVersion: "8.97.0"});
        });

        test("accepts a snapshot of the host inside the package's peer range", () => {
            const warnings: string[] = [];
            checkHostVersion(peerRange("^8.97.0"), "8.98.0-20261103-120000", {warn: (m: string) => warnings.push(m)} as any);
            expect(warnings).toEqual([]);
        });

        test("checks only a semver peer range", () => {
            const plainDependency = {name: "recipes", version: "1.0.0", dependencies: {"@openrewrite/rewrite": "^9.0.0"}};
            expect(() => checkHostVersion(plainDependency, "8.97.0")).not.toThrow();

            expect(() => checkHostVersion(peerRange("workspace:*"), "8.97.0")).not.toThrow();
        });
    });

    describe("error handling", () => {

        test("throws error when module does not export activate function", async () => {
            await withDir(async (dir) => {
                // given
                const recipeModulePath = path.join(dir.path, "bad-recipe.js");
                fs.writeFileSync(recipeModulePath, `
                    module.exports = {
                        notActivate: function() {}
                    };
                `);

                const installDir = path.join(dir.path, "recipes");
                const marketplace = new RecipeMarketplace();
                const handler = captureHandler(installDir, marketplace);

                // when/then
                await expect(handler({recipes: recipeModulePath} as any))
                    .rejects.toThrow("does not export an 'activate' function");
            }, {unsafeCleanup: true});
        });

        test("throws error when module file does not exist", async () => {
            await withDir(async (dir) => {
                // given
                const nonExistentPath = path.join(dir.path, "nonexistent.js");
                const installDir = path.join(dir.path, "recipes");
                const marketplace = new RecipeMarketplace();
                const handler = captureHandler(installDir, marketplace);

                // when/then
                await expect(handler({recipes: nonExistentPath} as any))
                    .rejects.toThrow(/Failed to load recipe module/);
            }, {unsafeCleanup: true});
        });

        test("throws error when module has syntax error", async () => {
            await withDir(async (dir) => {
                // given
                const recipeModulePath = path.join(dir.path, "syntax-error.js");
                fs.writeFileSync(recipeModulePath, `
                    module.exports = {
                        activate: function( { // syntax error - missing closing paren
                `);

                const installDir = path.join(dir.path, "recipes");
                const marketplace = new RecipeMarketplace();
                const handler = captureHandler(installDir, marketplace);

                // when/then
                await expect(handler({recipes: recipeModulePath} as any))
                    .rejects.toThrow(/Failed to load recipe module/);
            }, {unsafeCleanup: true});
        });
    });

    describe("npm package installation", () => {

        test("creates package.json when installing npm package", async () => {
            await withDir(async (dir) => {
                // given
                const installDir = path.join(dir.path, "recipes");
                const packageJsonPath = path.join(installDir, "package.json");
                const marketplace = new RecipeMarketplace();
                const handler = captureHandler(installDir, marketplace);

                // when
                try {
                    await handler({recipes: {packageName: "nonexistent-pkg"}} as any);
                } catch {
                    // Expected to fail - package doesn't exist on npm
                }

                // then - package.json should be created before npm install runs
                expect(fs.existsSync(packageJsonPath)).toBe(true);
                const createdPackageJson = JSON.parse(fs.readFileSync(packageJsonPath, "utf8"));
                expect(createdPackageJson.name).toBe("openrewrite-recipes");
                expect(createdPackageJson.private).toBe(true);
            }, {unsafeCleanup: true});
        }, 60000);

        test("installs @openrewrite/recipes-nodejs from npm", async () => {
            await withDir(async (dir) => {
                // given
                const installDir = path.join(dir.path, "recipes");
                const marketplace = new RecipeMarketplace();
                const handler = captureHandler(installDir, marketplace);

                // when
                const response = await handler({recipes: {packageName: "@openrewrite/recipes-nodejs"}} as any);

                // then
                expect(response.recipesInstalled).toBeGreaterThan(0);
                expect(response.version).toBeDefined();
                expect(marketplace.allRecipes().length).toBeGreaterThan(0);

                const packageJsonPath = path.join(installDir, "package.json");
                expect(fs.existsSync(packageJsonPath)).toBe(true);

                const nodeModulesPath = path.join(installDir, "node_modules", "@openrewrite", "recipes-nodejs");
                expect(fs.existsSync(nodeModulesPath)).toBe(true);
            }, {unsafeCleanup: true});
        }, 120000);

        test("upgrades @openrewrite/recipes-nodejs from 0.36.0 to a later version", async () => {
            await withDir(async (dir) => {
                // given
                const installDir = path.join(dir.path, "recipes");
                fs.mkdirSync(installDir, {recursive: true});
                const packageJsonPath = path.join(installDir, "package.json");
                fs.writeFileSync(packageJsonPath, JSON.stringify({
                    name: "openrewrite-recipes",
                    version: "1.0.0",
                    private: true,
                    dependencies: {
                        "@openrewrite/recipes-nodejs": "0.36.0"
                    }
                }, null, 2));

                const marketplace = new RecipeMarketplace();
                const handler = captureHandler(installDir, marketplace);

                // when
                await handler({recipes: {packageName: "@openrewrite/recipes-nodejs"}} as any);

                // then
                const updatedPackageJson = JSON.parse(fs.readFileSync(packageJsonPath, "utf8"));
                const versionStr = updatedPackageJson.dependencies["@openrewrite/recipes-nodejs"];
                const match = versionStr.match(/(\d+)\.(\d+)\.(\d+)/);
                expect(match).not.toBeNull();
                const minorVersion = parseInt(match![2], 10);
                expect(minorVersion).toBeGreaterThan(36);
            }, {unsafeCleanup: true});
        }, 120000);
    });

    describe("recipe attribution", () => {

        test("attributes recipes installed from a local path to that path", async () => {
            await withDir(async (dir) => {
                const recipeModulePath = path.join(dir.path, "local-recipe.js");
                fs.writeFileSync(recipeModulePath, `
                    module.exports = {
                        activate: async function(marketplace) {
                            await marketplace.install(
                                class LocalRecipe {
                                    async descriptor() {
                                        return { name: "local.recipe", displayName: "Local", description: "" };
                                    }
                                },
                                [{displayName: "Local"}]
                            );
                        }
                    };
                `);

                const installDir = path.join(dir.path, "recipes");
                const marketplace = new RecipeMarketplace();
                const recipeOrigin = new Map<string, string>();
                const handler = captureHandler(installDir, marketplace, recipeOrigin);

                await handler({recipes: recipeModulePath} as any);

                expect(marketplace.allRecipes().length).toBe(1);
                // Attributed to the install path (the host's bundle identity), not left unattributed.
                expect(recipeOrigin.get("local.recipe")).toBe(recipeModulePath);
            }, {unsafeCleanup: true});
        });

        test("GetMarketplace tags each row with its recipe's origin package", async () => {
            await withDir(async (dir) => {
                const recipeModulePath = path.join(dir.path, "row-recipe.js");
                fs.writeFileSync(recipeModulePath, `
                    module.exports = {
                        activate: async function(marketplace) {
                            await marketplace.install(
                                class RowRecipe {
                                    async descriptor() {
                                        return { name: "row.recipe", displayName: "Row", description: "" };
                                    }
                                },
                                [{displayName: "Row"}]
                            );
                        }
                    };
                `);

                const installDir = path.join(dir.path, "recipes");
                const marketplace = new RecipeMarketplace();
                const recipeOrigin = new Map<string, string>();
                const install = captureHandler(installDir, marketplace, recipeOrigin);
                await install({recipes: recipeModulePath} as any);

                // Stand in for the attribution InstallRecipes records for a package install.
                recipeOrigin.set("row.recipe", "@example/recipes");

                const getMarketplace = captureGetMarketplace(marketplace, recipeOrigin);
                const rows = await getMarketplace();

                const row = rows.find(r => r.descriptor.name === "row.recipe");
                expect(row).toBeDefined();
                expect(row!.packageName).toBe("@example/recipes");
            }, {unsafeCleanup: true});
        });

        test("attributes npm-installed recipes to their package", async () => {
            await withDir(async (dir) => {
                const installDir = path.join(dir.path, "recipes");
                const marketplace = new RecipeMarketplace();
                const recipeOrigin = new Map<string, string>();
                const handler = captureHandler(installDir, marketplace, recipeOrigin);

                await handler({recipes: {packageName: "@openrewrite/recipes-nodejs"}} as any);

                expect(recipeOrigin.size).toBeGreaterThan(0);
                for (const recipe of marketplace.allRecipes()) {
                    expect(recipeOrigin.get(recipe.name)).toBe("@openrewrite/recipes-nodejs");
                }
            }, {unsafeCleanup: true});
        }, 120000);
    });
});
