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
import {spawnSync} from "child_process";
import * as fs from "fs";
import * as path from "path";
import {dir} from "tmp-promise";
import {ProjectParser} from "../../src/javascript/project-parser";

function write(root: string, relativePath: string, content = "export const x = 1;\n"): void {
    const file = path.join(root, relativePath);
    fs.mkdirSync(path.dirname(file), {recursive: true});
    fs.writeFileSync(file, content);
}

/** A git repo whose `dist/` is committed source and whose `build/` is ignored output. */
function project(root: string): void {
    write(root, ".gitignore", "build/\n");
    write(root, "dist/keep.ts");
    write(root, "build/out.ts");
    write(root, "src/app.ts");
    write(root, "vendor.min.js");
    write(root, "node_modules/left-pad/index.js");

    spawnSync("git", ["init"], {cwd: root});
    // Staging is enough for `git ls-files`; the tests never need a commit.
    spawnSync("git", ["add", "dist/keep.ts", "vendor.min.js", "node_modules/left-pad/index.js"],
        {cwd: root});
}

async function discovered(root: string, options = {}): Promise<string[]> {
    const files = await new ProjectParser(root, options).discoverFiles();
    return files.jsFiles.map(f => path.relative(root, f).split(path.sep).join("/")).sort();
}

describe("ProjectParser exclusions", () => {
    it("lets .gitignore decide which directories are output when git discovery is used", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            project(tmpDir.path);
            expect(await discovered(tmpDir.path)).toEqual(["dist/keep.ts", "src/app.ts"]);
        } finally {
            await tmpDir.cleanup();
        }
    });

    it("still excludes committed bundles and dependency trees, which are never project source", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            project(tmpDir.path);
            const files = await discovered(tmpDir.path);
            expect(files).not.toContain("vendor.min.js");
            expect(files).not.toContain("node_modules/left-pad/index.js");
        } finally {
            await tmpDir.cleanup();
        }
    });

    it("honours caller exclusions over git tracking", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            project(tmpDir.path);
            const files = await discovered(tmpDir.path, {exclusions: ["**/dist/**"]});
            expect(files).not.toContain("dist/keep.ts");
            expect(files).toContain("src/app.ts");
        } finally {
            await tmpDir.cleanup();
        }
    });

    it("falls back to the default directory exclusions when walking without git", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            project(tmpDir.path);
            expect(await discovered(tmpDir.path, {useGit: false})).toEqual(["src/app.ts"]);
        } finally {
            await tmpDir.cleanup();
        }
    });
});

/** A git repo whose project lives in `ui/`, with the only `.gitignore` at the root. */
function nestedProject(root: string): string {
    write(root, ".gitignore", "target/\n");
    write(root, "pom.xml", "<project/>\n");
    write(root, "ui/package.json", '{"name": "ui"}\n');
    write(root, "ui/src/app.ts");
    write(root, "ui/dist/keep.ts");
    write(root, "ui/target/classes/static/browser/app.js");

    spawnSync("git", ["init"], {cwd: root});
    spawnSync("git", ["add", "ui/dist/keep.ts"], {cwd: root});
    return path.join(root, "ui");
}

describe("ProjectParser discovery in a project below the git root", () => {
    it("honours the repository root .gitignore", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            const files = await discovered(nestedProject(tmpDir.path));
            expect(files).toContain("src/app.ts");
            expect(files).not.toContain("target/classes/static/browser/app.js");
        } finally {
            await tmpDir.cleanup();
        }
    });

    it("keeps a tracked output directory, which is the project's own source", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            expect(await discovered(nestedProject(tmpDir.path))).toContain("dist/keep.ts");
        } finally {
            await tmpDir.cleanup();
        }
    });

    it("recognizes a work tree whose .git is a file", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            const root = path.join(tmpDir.path, "repo");
            nestedProject(root);
            const git = (...args: string[]) => spawnSync("git", args, {cwd: root, encoding: "utf8"});
            git("add", "-A");
            git("-c", "user.email=t@t", "-c", "user.name=t", "commit", "--no-verify", "-m", "initial");
            const worktree = path.join(tmpDir.path, "linked");
            git("worktree", "add", "--detach", worktree);
            expect(fs.statSync(path.join(worktree, ".git")).isFile()).toBe(true);
            // Ignored output is not checked out, so this work tree needs its own copy.
            write(worktree, "ui/target/classes/static/browser/app.js");

            const files = await discovered(path.join(worktree, "ui"));
            expect(files).toContain("dist/keep.ts");
            expect(files).not.toContain("target/classes/static/browser/app.js");
        } finally {
            await tmpDir.cleanup();
        }
    });

    it("falls back to the walk for a project the repository ignores entirely", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            write(tmpDir.path, ".gitignore", "ui/\n");
            write(tmpDir.path, "ui/src/app.ts");
            spawnSync("git", ["init"], {cwd: tmpDir.path});
            expect(await discovered(path.join(tmpDir.path, "ui"))).toEqual(["src/app.ts"]);
        } finally {
            await tmpDir.cleanup();
        }
    });

    it("walks with the default exclusions when no work tree encloses the project", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            write(tmpDir.path, "src/app.ts");
            write(tmpDir.path, "dist/keep.ts");
            write(tmpDir.path, "build/out.ts");
            expect(await discovered(tmpDir.path)).toEqual(["src/app.ts"]);
        } finally {
            await tmpDir.cleanup();
        }
    });
});
