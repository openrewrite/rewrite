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
import {dir} from "tmp-promise";

describe("ParseProject file discovery", () => {
    // Helper to simulate the file discovery logic from parse-project.ts
    function discoverFiles(
        projectPath: string,
        exclusions: string[]
    ): {
        sourceFiles: string[];
        packageJsonFiles: string[];
        lockFiles: Array<{path: string; parser: "json" | "yaml"}>;
    } {
        const picomatch = require("picomatch");

        const sourceExtensions = new Set([
            ".js", ".jsx", ".ts", ".tsx", ".mjs", ".cjs", ".mts", ".cts"
        ]);

        const isExcluded = picomatch(exclusions);

        const sourceFiles: string[] = [];
        const packageJsonFiles: string[] = [];

        function walkDirectory(dir: string, relativePath: string = "") {
            let entries: fs.Dirent[];
            try {
                entries = fs.readdirSync(dir, {withFileTypes: true});
            } catch {
                return;
            }

            for (const entry of entries) {
                const fullPath = path.join(dir, entry.name);
                const relPath = relativePath ? `${relativePath}/${entry.name}` : entry.name;

                if (isExcluded(relPath) || isExcluded(`${relPath}/`)) {
                    continue;
                }

                if (entry.isDirectory()) {
                    if (entry.name === "node_modules") {
                        continue;
                    }
                    walkDirectory(fullPath, relPath);
                } else if (entry.isFile()) {
                    const ext = path.extname(entry.name).toLowerCase();

                    if (entry.name === "package.json") {
                        packageJsonFiles.push(fullPath);
                    } else if (sourceExtensions.has(ext)) {
                        sourceFiles.push(fullPath);
                    }
                }
            }
        }

        walkDirectory(projectPath);

        // Find lock files
        const LOCK_FILES = [
            {filename: "package-lock.json", parser: "json" as const},
            {filename: "bun.lock", parser: "json" as const},
            {filename: "pnpm-lock.yaml", parser: "yaml" as const},
            {filename: "yarn.lock", parser: "yaml" as const}
        ];

        const lockFiles: Array<{path: string; parser: "json" | "yaml"}> = [];
        const checkedDirs = new Set<string>();

        checkedDirs.add(projectPath);

        for (const pkgJson of packageJsonFiles) {
            checkedDirs.add(path.dirname(pkgJson));
        }

        for (const dir of checkedDirs) {
            for (const lockConfig of LOCK_FILES) {
                const lockPath = path.join(dir, lockConfig.filename);
                if (fs.existsSync(lockPath)) {
                    lockFiles.push({
                        path: lockPath,
                        parser: lockConfig.parser
                    });
                    break;
                }
            }
        }

        return {sourceFiles, packageJsonFiles, lockFiles};
    }

    it("should discover JavaScript and TypeScript files", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            // Create project structure
            fs.writeFileSync(path.join(tmpDir.path, "index.js"), "const x = 1;");
            fs.writeFileSync(path.join(tmpDir.path, "app.ts"), "const y: number = 2;");
            fs.mkdirSync(path.join(tmpDir.path, "src"));
            fs.writeFileSync(path.join(tmpDir.path, "src", "utils.mjs"), "export const z = 3;");

            const result = discoverFiles(tmpDir.path, []);

            expect(result.sourceFiles).toHaveLength(3);
            expect(result.sourceFiles.some(f => f.endsWith("index.js"))).toBe(true);
            expect(result.sourceFiles.some(f => f.endsWith("app.ts"))).toBe(true);
            expect(result.sourceFiles.some(f => f.endsWith("utils.mjs"))).toBe(true);
        } finally {
            await tmpDir.cleanup();
        }
    });

    it("should discover package.json files", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            fs.writeFileSync(
                path.join(tmpDir.path, "package.json"),
                JSON.stringify({name: "root", version: "1.0.0"})
            );
            fs.mkdirSync(path.join(tmpDir.path, "packages", "sub"), {recursive: true});
            fs.writeFileSync(
                path.join(tmpDir.path, "packages", "sub", "package.json"),
                JSON.stringify({name: "sub", version: "1.0.0"})
            );

            const result = discoverFiles(tmpDir.path, []);

            expect(result.packageJsonFiles).toHaveLength(2);
        } finally {
            await tmpDir.cleanup();
        }
    });

    it("should skip node_modules directory", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            fs.writeFileSync(path.join(tmpDir.path, "index.js"), "const x = 1;");
            fs.mkdirSync(path.join(tmpDir.path, "node_modules", "lodash"), {recursive: true});
            fs.writeFileSync(
                path.join(tmpDir.path, "node_modules", "lodash", "index.js"),
                "module.exports = {};"
            );

            const result = discoverFiles(tmpDir.path, []);

            expect(result.sourceFiles).toHaveLength(1);
            expect(result.sourceFiles[0]).not.toContain("node_modules");
        } finally {
            await tmpDir.cleanup();
        }
    });

    it("should respect exclusion patterns", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            fs.writeFileSync(path.join(tmpDir.path, "index.js"), "const x = 1;");
            fs.mkdirSync(path.join(tmpDir.path, "vendor"));
            fs.writeFileSync(path.join(tmpDir.path, "vendor", "lib.js"), "const y = 2;");
            fs.mkdirSync(path.join(tmpDir.path, "dist"));
            fs.writeFileSync(path.join(tmpDir.path, "dist", "bundle.js"), "const z = 3;");

            const result = discoverFiles(tmpDir.path, ["vendor/**", "dist/**"]);

            expect(result.sourceFiles).toHaveLength(1);
            expect(result.sourceFiles[0]).toContain("index.js");
        } finally {
            await tmpDir.cleanup();
        }
    });

    it("should discover lock files", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            fs.writeFileSync(
                path.join(tmpDir.path, "package.json"),
                JSON.stringify({name: "test", version: "1.0.0"})
            );
            fs.writeFileSync(
                path.join(tmpDir.path, "package-lock.json"),
                JSON.stringify({name: "test", lockfileVersion: 3})
            );

            const result = discoverFiles(tmpDir.path, []);

            expect(result.lockFiles).toHaveLength(1);
            expect(result.lockFiles[0].path).toContain("package-lock.json");
            expect(result.lockFiles[0].parser).toBe("json");
        } finally {
            await tmpDir.cleanup();
        }
    });

    it("should discover yarn.lock as YAML", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            fs.writeFileSync(
                path.join(tmpDir.path, "package.json"),
                JSON.stringify({name: "test", version: "1.0.0"})
            );
            fs.writeFileSync(
                path.join(tmpDir.path, "yarn.lock"),
                "# yarn lockfile v1\n"
            );

            const result = discoverFiles(tmpDir.path, []);

            expect(result.lockFiles).toHaveLength(1);
            expect(result.lockFiles[0].path).toContain("yarn.lock");
            expect(result.lockFiles[0].parser).toBe("yaml");
        } finally {
            await tmpDir.cleanup();
        }
    });

    it("should prioritize package-lock.json over yarn.lock", async () => {
        const tmpDir = await dir({unsafeCleanup: true});
        try {
            fs.writeFileSync(
                path.join(tmpDir.path, "package.json"),
                JSON.stringify({name: "test", version: "1.0.0"})
            );
            fs.writeFileSync(
                path.join(tmpDir.path, "package-lock.json"),
                JSON.stringify({name: "test", lockfileVersion: 3})
            );
            fs.writeFileSync(
                path.join(tmpDir.path, "yarn.lock"),
                "# yarn lockfile v1\n"
            );

            const result = discoverFiles(tmpDir.path, []);

            // Should only have one lock file (package-lock.json takes priority)
            expect(result.lockFiles).toHaveLength(1);
            expect(result.lockFiles[0].path).toContain("package-lock.json");
        } finally {
            await tmpDir.cleanup();
        }
    });
});
