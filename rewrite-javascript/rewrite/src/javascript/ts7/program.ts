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
import {API, type Project, type Snapshot} from "typescript/unstable/sync";
import type {SourceFile} from "typescript/unstable/ast";
import type {FileSystem} from "typescript/unstable/fs";
import {memoizing} from "./checker";

// The compiler runs as its own process that owns parsing and module resolution, so a program is
// described to it rather than built here: sources the parser holds in memory are served through
// filesystem callbacks, and returning undefined defers to the real filesystem for node_modules and
// the bundled lib files, which the server then resolves against on its own.

/** One program within a parse: the files it is rooted at, and the options they compile under. */
export interface ProjectRequest {
    readonly key: string;
    readonly options: Record<string, unknown>;
    readonly rootFiles: readonly string[];
}

export interface ParseSession {
    projectFor(key: string): Project;

    close(): void;
}

/**
 * A compiler process and the in-memory files it can see. Starting one and loading its libraries
 * costs far more than a parse does, so a caller that parses repeatedly keeps one and opens a
 * project per batch.
 */
export class Compiler {
    private readonly files = new Map<string, string>();
    private api: API | undefined;
    private snapshot: Snapshot | undefined;
    private openConfigs: string[] = [];
    private projectCount = 0;

    constructor(private readonly root: string) {
    }

    /**
     * Opens one project per entry in `projects` over `sources`, which are keyed by absolute path
     * and exist only in memory. Each project's `options` are written into the tsconfig the compiler
     * reads for it.
     */
    open(sources: ReadonlyMap<string, string>, projects: readonly ProjectRequest[]): ParseSession {
        // The compiler is reached over a protocol that cannot carry a lone surrogate.
        const restore = new Set<string>();
        for (const [file, text] of sources) {
            const carriable = wellFormed(text);
            this.files.set(file, carriable);
            // A byte order mark is dropped from the text the compiler reports while its node
            // positions go on counting it, so those files are given their own text back as well.
            if (carriable !== text || text.charCodeAt(0) === BYTE_ORDER_MARK) {
                restore.add(file);
            }
        }

        const configOf = new Map<string, string>();
        for (const {key, options, rootFiles} of projects) {
            // Each batch gets its own config, so the compiler reads a fresh one rather than being
            // told that a file it has already parsed now says something else.
            const configPath = path.join(this.root, `tsconfig.${this.projectCount++}.json`);
            configOf.set(key, configPath);
            // The program is rooted at the files it was given. A glob would take in everything
            // under `root`, which for a parse rooted at a project directory is the whole project.
            this.files.set(configPath, JSON.stringify(rootFiles.length > 0
                ? {compilerOptions: options, files: rootFiles}
                : {compilerOptions: options, include: ["**/*.ts", "**/*.tsx", "**/*.js", "**/*.jsx"]}));
        }
        const configPaths = [...configOf.values()];

        const api = this.compilerApi();
        const previous = {snapshot: this.snapshot, configs: this.openConfigs};
        // A path parsed before may carry different text now, and the compiler holds what it read
        // last, so every source is announced as changed rather than left to look unchanged.
        const changed = [...sources.keys(), ...configPaths];
        this.snapshot = api.updateSnapshot({
            openProjects: configPaths,
            ...(previous.configs.length > 0 ? {closeProjects: previous.configs} : {}),
            fileChanges: {changed},
        });
        this.openConfigs = configPaths;
        previous.snapshot?.dispose();
        const snapshot = this.snapshot;

        const opened = new Map<string, Project>();
        return {
            projectFor: key => {
                let project = opened.get(key);
                if (!project) {
                    const configPath = configOf.get(key);
                    const found = configPath === undefined ? undefined : snapshot.getProject(configPath);
                    if (!found) {
                        throw new Error(`TypeScript did not open a project at ${configPath ?? key}`);
                    }
                    restoreInputText(found, sources, restore);
                    const checker = memoizing(found.checker);
                    opened.set(key, project = new Proxy(found, {
                        get: (target, property: string, receiver) =>
                            property === "checker" ? checker : Reflect.get(target, property, receiver),
                    }));
                }
                return project;
            },
            close: () => this.forget(sources.keys(), configPaths),
        };
    }

    /** The project options `configFilePath` states, with `extends` resolved, as tsconfig JSON names. */
    parseConfigFile(configFilePath: string): Record<string, unknown> {
        return this.compilerApi().parseConfigFile(configFilePath).options;
    }

    /** Shuts the compiler down. A later `open` starts a new one. */
    close(): void {
        this.snapshot?.dispose();
        this.snapshot = undefined;
        this.openConfigs = [];
        this.api?.close();
        this.api = undefined;
        this.files.clear();
    }

    private compilerApi(): API {
        return this.api ??= new API({cwd: this.root, fs: this.fileSystem()});
    }

    private forget(sources: Iterable<string>, configPaths: Iterable<string>): void {
        for (const file of sources) {
            this.files.delete(file);
        }
        for (const configPath of configPaths) {
            this.files.delete(configPath);
        }
    }

    private fileSystem(): FileSystem {
        const files = this.files;
        return {
            readFile: file => files.get(file),
            fileExists: file => (files.has(file) ? true : undefined),
            // A directory that only holds in-memory sources exists as far as resolution is
            // concerned, or the compiler will not look inside it for them.
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
                // A directory holding in-memory sources may also exist on disk, and the server
                // needs one listing covering both.
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
    }
}

const BYTE_ORDER_MARK = 0xFEFF;

/**
 * `text` with each unpaired surrogate replaced by U+FFFD, one character for one so that offsets
 * are unmoved. The methods that do it postdate the `lib` this project compiles against.
 */
function wellFormed(text: string): string {
    const string = text as unknown as { isWellFormed(): boolean, toWellFormed(): string };
    return string.isWellFormed() ? text : string.toWellFormed();
}

/** Puts a file's own text on its {@link SourceFile}, in place of what the compiler read. */
function restoreInputText(project: Project, sources: ReadonlyMap<string, string>,
                          restore: ReadonlySet<string>): void {
    for (const rootFile of project.rootFiles) {
        const text = restore.has(rootFile) ? sources.get(rootFile) : undefined;
        if (text !== undefined) {
            Object.defineProperty(project.program.getSourceFile(rootFile), "text",
                {value: text, configurable: true});
        }
    }
}

/** Opens a single project for a single use, shutting the compiler down with it. */
export function openSession(
    root: string,
    sources: ReadonlyMap<string, string>,
    compilerOptions: Record<string, unknown>,
    rootFiles?: readonly string[],
): {project: Project, close(): void} {
    const compiler = new Compiler(root);
    const key = "";
    const session = compiler.open(sources, [{key, options: compilerOptions, rootFiles: rootFiles ?? [...sources.keys()]}]);
    return {project: session.projectFor(key), close: () => compiler.close()};
}
