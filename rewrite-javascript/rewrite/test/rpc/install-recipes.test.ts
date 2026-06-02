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
import {RecipeMarketplace} from "../../src";
import {InstallRecipes, InstallRecipesResponse} from "../../src/rpc/request/install-recipes";

describe("InstallRecipes", () => {

    type RequestHandler = (request: InstallRecipes) => Promise<InstallRecipesResponse>;

    function captureHandler(installDir: string, marketplace: RecipeMarketplace): RequestHandler {
        let capturedHandler: RequestHandler | undefined;

        const dummyConnection = {
            onRequest: (_requestType: any, handler: RequestHandler) => {
                capturedHandler = handler;
            }
        } as any;

        InstallRecipes.handle(dummyConnection, installDir, marketplace);

        if (!capturedHandler) {
            throw new Error("Handler was not registered");
        }

        return capturedHandler;
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
});
