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
import * as fsp from "fs/promises";
import * as path from "path";
import {spawnSync} from "child_process";

/**
 * Default directory exclusions for file/directory discovery.
 */
export const DEFAULT_DIR_EXCLUSIONS = new Set([
    "node_modules",
    ".git",
    ".svn",
    ".hg",
    "dist",
    "build",
    "out",
    "coverage",
    ".next",
    ".nuxt",
    ".output"
]);

/**
 * Checks if a path is ignored by Git.
 * Returns true if the path is ignored, false otherwise.
 * Falls back to false if not in a git repository.
 */
export function isGitIgnored(filePath: string, cwd?: string): boolean {
    const result = spawnSync("git", ["check-ignore", "-q", filePath], {
        cwd: cwd || path.dirname(filePath),
        encoding: "utf8"
    });
    // Exit code 0 means the file IS ignored
    // Exit code 1 means the file is NOT ignored
    // Other exit codes indicate git errors (not a repo, etc.)
    return result.status === 0;
}

/**
 * Checks if we're in a git repository.
 */
export function isInGitRepo(dir: string): boolean {
    const result = spawnSync("git", ["rev-parse", "--git-dir"], {
        cwd: dir,
        encoding: "utf8"
    });
    return result.status === 0;
}

export interface WalkDirsOptions {
    /**
     * Maximum depth to descend. 0 means only immediate children.
     * undefined means unlimited.
     */
    maxDepth?: number;

    /**
     * Directory names to exclude. Defaults to DEFAULT_DIR_EXCLUSIONS.
     */
    excludeDirs?: Set<string>;

    /**
     * Whether to respect .gitignore. Defaults to false.
     */
    respectGitIgnore?: boolean;

    /**
     * Only include directories containing a file with this name.
     * Useful for finding package directories by looking for package.json.
     */
    mustContainFile?: string;
}

/**
 * Recursively walks directories starting from a given path.
 * Returns all subdirectory paths (not including the root).
 */
export async function walkDirs(
    rootDir: string,
    options: WalkDirsOptions = {}
): Promise<string[]> {
    const {
        maxDepth,
        excludeDirs = DEFAULT_DIR_EXCLUSIONS,
        respectGitIgnore = false,
        mustContainFile
    } = options;

    const results: string[] = [];
    const inGitRepo = respectGitIgnore && isInGitRepo(rootDir);

    async function walk(dir: string, depth: number): Promise<void> {
        if (maxDepth !== undefined && depth > maxDepth) {
            return;
        }

        let entries: fs.Dirent[];
        try {
            entries = await fsp.readdir(dir, {withFileTypes: true});
        } catch {
            return;
        }

        for (const entry of entries) {
            if (!entry.isDirectory()) {
                continue;
            }

            // Skip excluded directories
            if (excludeDirs.has(entry.name)) {
                continue;
            }

            // Skip hidden directories
            if (entry.name.startsWith('.')) {
                continue;
            }

            const fullPath = path.join(dir, entry.name);

            // Check git ignore if enabled
            if (inGitRepo && isGitIgnored(fullPath, rootDir)) {
                continue;
            }

            // Check for required file if specified
            if (mustContainFile) {
                const requiredFile = path.join(fullPath, mustContainFile);
                if (fs.existsSync(requiredFile)) {
                    results.push(fullPath);
                }
            } else {
                results.push(fullPath);
            }

            // Recurse into subdirectory
            await walk(fullPath, depth + 1);
        }
    }

    await walk(rootDir, 0);
    return results;
}

/**
 * Gets files tracked by git (and untracked but not ignored files).
 * Falls back to empty array if not in a git repository.
 */
export function getGitTrackedFiles(dir: string): string[] {
    const files: string[] = [];

    // Get tracked files
    const tracked = spawnSync("git", ["ls-files"], {
        cwd: dir,
        encoding: "utf8"
    });

    if (tracked.status !== 0 || tracked.error) {
        return [];
    }

    if (tracked.stdout) {
        for (const line of tracked.stdout.split("\n")) {
            const trimmed = line.trim();
            if (trimmed) {
                const fullPath = path.join(dir, trimmed);
                if (fs.existsSync(fullPath)) {
                    files.push(fullPath);
                }
            }
        }
    }

    // Get untracked but not ignored files
    const untracked = spawnSync("git", ["ls-files", "--others", "--exclude-standard"], {
        cwd: dir,
        encoding: "utf8"
    });

    if (untracked.stdout) {
        for (const line of untracked.stdout.split("\n")) {
            const trimmed = line.trim();
            if (trimmed) {
                const fullPath = path.join(dir, trimmed);
                if (fs.existsSync(fullPath)) {
                    files.push(fullPath);
                }
            }
        }
    }

    return files;
}
