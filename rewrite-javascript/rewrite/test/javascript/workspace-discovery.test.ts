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
import * as fs from "fs";
import * as path from "path";
import {dir, DirectoryResult} from "tmp-promise";
import {discoverProjects} from "../../src/javascript/workspace-discovery";

describe("workspace-discovery", () => {
    let tmp: DirectoryResult;
    let root: string;

    beforeEach(async () => {
        tmp = await dir({unsafeCleanup: true});
        root = fs.realpathSync(tmp.path);
    });

    afterEach(async () => {
        await tmp.cleanup();
    });

    function write(rel: string, content: string): void {
        const abs = path.join(root, rel);
        fs.mkdirSync(path.dirname(abs), {recursive: true});
        fs.writeFileSync(abs, content);
    }

    it("returns one project for a standalone package.json", async () => {
        write("package.json", JSON.stringify({name: "app", version: "1.0.0"}));
        write("src/index.ts", "export const x = 1;");

        const projects = await discoverProjects(root);

        expect(projects).toHaveLength(1);
        expect(projects[0].path).toBe("");
        // No lockfile → default Npm
        expect(projects[0].packageManager).toBe("Npm");
        expect(projects[0].resolution).toBeNull();
    });

    it("emits only a main source set when there are no test files", async () => {
        write("package.json", JSON.stringify({name: "app"}));
        write("src/index.ts", "export const x = 1;");

        const [project] = await discoverProjects(root);

        expect(project.sourceSets).toHaveLength(1);
        expect(project.sourceSets[0].name).toBe("main");
        expect(project.sourceSets[0].includes).toBeUndefined();
        expect(project.sourceSets[0].excludes).toBeUndefined();
    });

    it("splits main/test by convention and sets tsconfigPath on both sets", async () => {
        write("package.json", JSON.stringify({name: "app"}));
        write("tsconfig.json", JSON.stringify({compilerOptions: {strict: true}}));
        write("src/index.ts", "export const x = 1;");
        write("src/foo.test.ts", "test('x', () => {});");

        const [project] = await discoverProjects(root);

        const names = project.sourceSets.map(s => s.name);
        expect(names).toEqual(["main", "test"]);

        const main = project.sourceSets.find(s => s.name === "main")!;
        const test = project.sourceSets.find(s => s.name === "test")!;
        // Test set selects the conventional globs; main excludes them.
        expect(test.includes).toContain("**/*.test.*");
        expect(main.excludes).toContain("**/*.test.*");

        expect(main.parserSettings.tsconfigPath).toBe("tsconfig.json");
        expect(test.parserSettings.tsconfigPath).toBe("tsconfig.json");
    });

    it("resolves the configInputs watch-set including the local tsconfig extends chain and references", async () => {
        write("package.json", JSON.stringify({name: "app"}));
        write("package-lock.json", "{}");
        write("tsconfig.json", JSON.stringify({
            extends: "./tsconfig.base.json",
            references: [{path: "./tsconfig.node.json"}]
        }));
        write("tsconfig.base.json", JSON.stringify({compilerOptions: {strict: true}}));
        write("tsconfig.node.json", JSON.stringify({compilerOptions: {}}));
        write(".prettierrc", "{}");
        write("vitest.config.ts", "export default {};");
        write("src/index.ts", "export const x = 1;");

        const [project] = await discoverProjects(root);

        expect(new Set(project.configInputs)).toEqual(new Set([
            "package.json",
            "package-lock.json",
            "tsconfig.json",
            "tsconfig.base.json",
            "tsconfig.node.json",
            ".prettierrc",
            "vitest.config.ts"
        ]));
    });

    it("resolves a tsconfig reference that points at a directory to its tsconfig.json", async () => {
        write("package.json", JSON.stringify({name: "app"}));
        write("tsconfig.json", JSON.stringify({references: [{path: "./sub"}]}));
        write("sub/tsconfig.json", JSON.stringify({compilerOptions: {}}));
        write("src/index.ts", "export const x = 1;");

        const [project] = await discoverProjects(root);

        expect(project.configInputs).toContain("sub/tsconfig.json");
    });

    it("skips npm-package tsconfig extends (not a watchable local file)", async () => {
        write("package.json", JSON.stringify({name: "app"}));
        write("tsconfig.json", JSON.stringify({extends: "@tsconfig/node20/tsconfig.json"}));
        write("src/index.ts", "export const x = 1;");

        const [project] = await discoverProjects(root);

        expect(project.configInputs).toContain("tsconfig.json");
        expect(project.configInputs.some(p => p.includes("node_modules") || p.includes("@tsconfig"))).toBe(false);
    });

    it("fans out npm workspaces to one project per member, excluding the root manager", async () => {
        write("package.json", JSON.stringify({name: "root", private: true, workspaces: ["packages/*"]}));
        write("package-lock.json", "{}");
        write("packages/a/package.json", JSON.stringify({name: "a"}));
        write("packages/a/src/index.ts", "export const a = 1;");
        write("packages/b/package.json", JSON.stringify({name: "b"}));
        write("packages/b/src/index.ts", "export const b = 2;");

        const projects = await discoverProjects(root);

        expect(projects.map(p => p.path).sort()).toEqual(["packages/a", "packages/b"]);
        // The shared root lock file is each member's nearest lock file.
        for (const project of projects) {
            expect(project.packageManager).toBe("Npm");
            expect(project.configInputs).toContain("package-lock.json");
        }
        const a = projects.find(p => p.path === "packages/a")!;
        expect(a.configInputs).toContain("packages/a/package.json");
    });

    it("fans out a pnpm workspace and detects pnpm from the root lock file", async () => {
        write("pnpm-workspace.yaml", "packages:\n  - 'packages/*'\n");
        write("package.json", JSON.stringify({name: "root"}));
        write("pnpm-lock.yaml", "lockfileVersion: '6.0'\n");
        write("packages/a/package.json", JSON.stringify({name: "a"}));
        write("packages/a/index.ts", "export const a = 1;");

        const projects = await discoverProjects(root);

        expect(projects.map(p => p.path)).toEqual(["packages/a"]);
        expect(projects[0].packageManager).toBe("Pnpm");
    });

    it("distinguishes yarn Berry from Classic via the lock file content", async () => {
        write("package.json", JSON.stringify({name: "berry"}));
        write("yarn.lock", "__metadata:\n  version: 6\n");
        write("index.ts", "export const x = 1;");

        const [berry] = await discoverProjects(root);
        expect(berry.packageManager).toBe("YarnBerry");
    });

    it("prunes nested package.json that is not a workspace member", async () => {
        write("package.json", JSON.stringify({name: "root"}));
        write("tools/sub/package.json", JSON.stringify({name: "sub"}));
        write("index.ts", "export const x = 1;");

        const projects = await discoverProjects(root);

        expect(projects.map(p => p.path)).toEqual([""]);
    });

    it("honors workspace negation (!) exclusion patterns", async () => {
        write("package.json", JSON.stringify({
            name: "root", private: true, workspaces: ["packages/*", "!packages/excluded"]
        }));
        write("packages/keep/package.json", JSON.stringify({name: "keep"}));
        write("packages/keep/index.ts", "export const k = 1;");
        write("packages/excluded/package.json", JSON.stringify({name: "excluded"}));
        write("packages/excluded/index.ts", "export const e = 1;");

        const projects = await discoverProjects(root);

        expect(projects.map(p => p.path)).toEqual(["packages/keep"]);
    });

    it("matches workspace globs written with a trailing slash", async () => {
        write("package.json", JSON.stringify({name: "root", workspaces: ["packages/*/"]}));
        write("packages/a/package.json", JSON.stringify({name: "a"}));
        write("packages/a/index.ts", "export const a = 1;");

        const projects = await discoverProjects(root);

        expect(projects.map(p => p.path)).toEqual(["packages/a"]);
    });

    it("expands brace workspace globs", async () => {
        write("package.json", JSON.stringify({name: "root", workspaces: ["{apps,libs}/*"]}));
        write("apps/web/package.json", JSON.stringify({name: "web"}));
        write("apps/web/index.ts", "export const w = 1;");
        write("libs/ui/package.json", JSON.stringify({name: "ui"}));
        write("libs/ui/index.ts", "export const u = 1;");
        write("other/x/package.json", JSON.stringify({name: "x"}));
        write("other/x/index.ts", "export const x = 1;");

        const projects = await discoverProjects(root);

        expect(projects.map(p => p.path).sort()).toEqual(["apps/web", "libs/ui"]);
    });

    it("falls back to the corepack packageManager field when there is no lock file", async () => {
        write("package.json", JSON.stringify({name: "app", packageManager: "pnpm@8.15.0"}));
        write("index.ts", "export const x = 1;");

        const [project] = await discoverProjects(root);

        expect(project.packageManager).toBe("Pnpm");
    });

    it("infers yarn Berry vs Classic from the corepack packageManager version", async () => {
        write("a/package.json", JSON.stringify({name: "a", packageManager: "yarn@1.22.19"}));
        write("a/index.ts", "export const a = 1;");
        write("b/package.json", JSON.stringify({name: "b", packageManager: "yarn@4.1.0"}));
        write("b/index.ts", "export const b = 1;");

        const projects = await discoverProjects(root);

        expect(projects.find(p => p.path === "a")!.packageManager).toBe("YarnClassic");
        expect(projects.find(p => p.path === "b")!.packageManager).toBe("YarnBerry");
    });
});
