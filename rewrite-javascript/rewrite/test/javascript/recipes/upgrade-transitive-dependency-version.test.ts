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
    applyOverrideToPackageJson,
    npm,
    packageJson,
    PackageManager,
    parseDependencyPath,
    typescript,
    UpgradeTransitiveDependencyVersion
} from "../../../src/javascript";
import {RecipeSpec} from "../../../src/test";
import {withDir} from "tmp-promise";

describe("UpgradeTransitiveDependencyVersion", () => {

    test("adds override for transitive dependency (npm)", async () => {
        const spec = new RecipeSpec();
        // is-odd is a transitive dependency of is-even@1.0.0 (depends on is-odd@^0.1.2)
        // Upgrading to ^3.0.0 which exists
        spec.recipe = new UpgradeTransitiveDependencyVersion({
            packageName: "is-odd",
            newVersion: "^3.0.0"
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "is-even": "^1.0.0"
                            }
                        }
                    `, (actual: string) => {
                        const pkg = JSON.parse(actual);
                        // Should have added overrides section
                        expect(pkg.overrides).toBeDefined();
                        expect(pkg.overrides["is-odd"]).toBe("^3.0.0");
                        // Should NOT have modified dependencies
                        expect(pkg.dependencies["is-even"]).toBe("^1.0.0");
                        return actual;
                    })
                )
            );
        }, {unsafeCleanup: true});
    });

    test("adds scoped override with dependencyPath (npm)", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new UpgradeTransitiveDependencyVersion({
            packageName: "is-odd",
            newVersion: "^3.0.0",
            dependencyPath: "is-even"
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "is-even": "^1.0.0"
                            }
                        }
                    `, (actual: string) => {
                        const pkg = JSON.parse(actual);
                        // Should have nested override structure for npm
                        expect(pkg.overrides).toBeDefined();
                        expect(pkg.overrides["is-even"]).toBeDefined();
                        expect(pkg.overrides["is-even"]["is-odd"]).toBe("^3.0.0");
                        return actual;
                    })
                )
            );
        }, {unsafeCleanup: true});
    });

    test("does not add override if package is a direct dependency", async () => {
        const spec = new RecipeSpec();
        // is-odd is a direct dependency, so this recipe should not process it
        spec.recipe = new UpgradeTransitiveDependencyVersion({
            packageName: "is-odd",
            newVersion: "^3.0.0"
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    // No changes expected - is-odd is a direct dependency
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "is-odd": "^2.0.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    test("does not add override if resolved version already satisfies constraint", async () => {
        const spec = new RecipeSpec();
        // is-odd is a transitive dependency at version that already satisfies ^3.0.0
        spec.recipe = new UpgradeTransitiveDependencyVersion({
            packageName: "is-number",
            newVersion: "^6.0.0"  // is-odd@3.0.0 depends on is-number@^6.0.0
        });

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(`const x = 1;`),
                    // No changes expected - is-number is already at ^6.0.0
                    packageJson(`
                        {
                            "name": "test-project",
                            "version": "1.0.0",
                            "dependencies": {
                                "is-odd": "^3.0.0"
                            }
                        }
                    `)
                )
            );
        }, {unsafeCleanup: true});
    });

    describe("parseDependencyPath", () => {

        test("parses simple path with > separator", () => {
            const result = parseDependencyPath("express>accepts");
            expect(result).toEqual([
                {name: "express"},
                {name: "accepts"}
            ]);
        });

        test("parses simple path with / separator", () => {
            const result = parseDependencyPath("express/accepts");
            expect(result).toEqual([
                {name: "express"},
                {name: "accepts"}
            ]);
        });

        test("parses path with version constraint", () => {
            const result = parseDependencyPath("express@4.0.0>accepts");
            expect(result).toEqual([
                {name: "express", version: "4.0.0"},
                {name: "accepts"}
            ]);
        });

        test("parses scoped package", () => {
            const result = parseDependencyPath("@scope/pkg>dep");
            expect(result).toEqual([
                {name: "@scope/pkg"},
                {name: "dep"}
            ]);
        });

        test("parses scoped package with version", () => {
            const result = parseDependencyPath("@scope/pkg@1.0.0>dep");
            expect(result).toEqual([
                {name: "@scope/pkg", version: "1.0.0"},
                {name: "dep"}
            ]);
        });

        test("parses multi-level path", () => {
            const result = parseDependencyPath("a>b>c");
            expect(result).toEqual([
                {name: "a"},
                {name: "b"},
                {name: "c"}
            ]);
        });

    });

    describe("applyOverrideToPackageJson", () => {

        test("npm: global override", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.Npm,
                "lodash",
                "^4.17.21"
            );
            expect(result.overrides).toEqual({lodash: "^4.17.21"});
        });

        test("npm: scoped override", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.Npm,
                "accepts",
                "^2.0.0",
                [{name: "express"}]
            );
            expect(result.overrides).toEqual({
                express: {accepts: "^2.0.0"}
            });
        });

        test("npm: nested scoped override", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.Npm,
                "mime-types",
                "^3.0.0",
                [{name: "express"}, {name: "accepts"}]
            );
            expect(result.overrides).toEqual({
                express: {accepts: {"mime-types": "^3.0.0"}}
            });
        });

        test("npm: merges with existing overrides", () => {
            const result = applyOverrideToPackageJson(
                {name: "test", overrides: {lodash: "^4.17.20"}},
                PackageManager.Npm,
                "underscore",
                "^1.13.0"
            );
            expect(result.overrides).toEqual({
                lodash: "^4.17.20",
                underscore: "^1.13.0"
            });
        });

        test("yarn: global resolution", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.YarnClassic,
                "lodash",
                "^4.17.21"
            );
            expect(result.resolutions).toEqual({lodash: "^4.17.21"});
        });

        test("yarn: scoped resolution", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.YarnBerry,
                "accepts",
                "^2.0.0",
                [{name: "express"}]
            );
            expect(result.resolutions).toEqual({"express/accepts": "^2.0.0"});
        });

        test("pnpm: global override", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.Pnpm,
                "lodash",
                "^4.17.21"
            );
            expect(result.pnpm.overrides).toEqual({lodash: "^4.17.21"});
        });

        test("pnpm: scoped override", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.Pnpm,
                "accepts",
                "^2.0.0",
                [{name: "express"}]
            );
            expect(result.pnpm.overrides).toEqual({"express>accepts": "^2.0.0"});
        });

        test("pnpm: multi-level scoped override", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.Pnpm,
                "mime-types",
                "^3.0.0",
                [{name: "express"}, {name: "accepts"}]
            );
            expect(result.pnpm.overrides).toEqual({"express>accepts>mime-types": "^3.0.0"});
        });

        test("bun: uses npm-style overrides", () => {
            const result = applyOverrideToPackageJson(
                {name: "test"},
                PackageManager.Bun,
                "lodash",
                "^4.17.21"
            );
            expect(result.overrides).toEqual({lodash: "^4.17.21"});
        });

    });

});
