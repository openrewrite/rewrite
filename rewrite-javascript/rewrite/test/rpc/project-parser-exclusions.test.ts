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
