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
import fs from "node:fs";
import path from "node:path";
import {API, type Project} from "typescript/unstable/sync";
import type {FileSystem} from "typescript/unstable/fs";

// The TypeScript 7 compiler runs as a Go process that owns parsing and module resolution, so a
// program is described to it rather than built here: sources the parser holds in memory are served
// through filesystem callbacks, and returning undefined defers to the real filesystem for
// node_modules and the bundled lib files, which the server then resolves against on its own.

export interface ParseSession {
    readonly project: Project;

    close(): void;
}

/**
 * Opens a project over `sources`, which are keyed by absolute path and exist only in memory.
 * `compilerOptions` is written into the tsconfig the Go server reads.
 */
export function openSession(
    root: string,
    sources: ReadonlyMap<string, string>,
    compilerOptions: Record<string, unknown>,
    rootFiles?: readonly string[],
): ParseSession {
    const configPath = path.join(root, "tsconfig.json");
    const files = new Map(sources);
    // The program is rooted at the files it was given. A glob would take in everything under
    // `root`, which for a parse rooted at a project directory is the whole project.
    const roots = rootFiles ?? [...sources.keys()];
    files.set(configPath, JSON.stringify(roots.length > 0
        ? {compilerOptions, files: roots}
        : {compilerOptions, include: ["**/*.ts", "**/*.tsx", "**/*.js", "**/*.jsx"]}));

    const fileSystem: FileSystem = {
        readFile: file => files.get(file),
        fileExists: file => (files.has(file) ? true : undefined),
        // A directory that only holds in-memory sources exists as far as resolution is concerned,
        // or the compiler will not look inside it for them.
        directoryExists: directory => {
            const prefix = directory.endsWith(path.sep) ? directory : directory + path.sep;
            return [...files.keys()].some(file => file.startsWith(prefix)) ? true : undefined;
        },
        getAccessibleEntries: directory => {
            const prefix = directory.endsWith(path.sep) ? directory : directory + path.sep;
            const virtual: string[] = [];
            const virtualDirectories = new Set<string>();
            for (const file of files.keys()) {
                if (!file.startsWith(prefix)) {
                    continue;
                }
                // A file nested below `directory` also makes the directory holding it visible,
                // which is how the compiler finds its way down to that file.
                const rest = file.slice(prefix.length);
                const slash = rest.indexOf(path.sep);
                if (slash === -1) {
                    virtual.push(rest);
                } else {
                    virtualDirectories.add(rest.slice(0, slash));
                }
            }
            if (virtual.length === 0 && virtualDirectories.size === 0) {
                return undefined;
            }
            // A directory holding in-memory sources may also exist on disk, and the server needs
            // one listing covering both.
            const entries = {files: [] as string[], directories: [] as string[]};
            try {
                for (const entry of fs.readdirSync(directory, {withFileTypes: true})) {
                    (entry.isDirectory() ? entries.directories : entries.files).push(entry.name);
                }
            } catch {
                // Purely virtual directory.
            }
            return {
                files: [...new Set([...entries.files, ...virtual])],
                directories: [...new Set([...entries.directories, ...virtualDirectories])],
            };
        },
        realpath: () => undefined,
    };

    const api = new API({cwd: root, fs: fileSystem});
    const project = api.updateSnapshot({openProjects: [configPath]}).getProjects()[0];
    if (!project) {
        api.close();
        throw new Error(`TypeScript did not open a project at ${configPath}`);
    }
    return {project, close: () => api.close()};
}
