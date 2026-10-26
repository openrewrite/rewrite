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
import path from "node:path";
import {API, type Project, type Snapshot} from "typescript/sync";
import {memoizing, type MemoizingChecker} from "./checker";

// The compiler runs as its own process that owns parsing and module resolution, so a program is
// described to it rather than built here. Each snapshot layers the sources the parser holds in
// memory over the real filesystem, which the server reads for node_modules and the bundled lib files.

/** One program within a parse: the files it is rooted at, and the options they compile under. */
export interface ProjectRequest {
    readonly key: string;
    readonly options: Record<string, unknown>;
    readonly rootFiles: readonly string[];
}

/** A compiler project whose checker remembers its answers. */
export type MemoizedProject = Project & {readonly checker: MemoizingChecker};

export interface ParseSession {
    projectFor(key: string): MemoizedProject;

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

        const previous = {snapshot: this.snapshot, configs: this.openConfigs};
        const fileSystem = {kind: "layer", files: Object.fromEntries(this.files)} as const;
        // Building on the previous snapshot lets the compiler keep what it already loaded.
        const snapshot = this.snapshot = previous.snapshot
            ? previous.snapshot.update({openProjects: configPaths, closeProjects: previous.configs, fileSystem})
            : this.compilerApi().createSnapshot({openProjects: configPaths, fileSystem});
        this.openConfigs = configPaths;
        if (previous.snapshot !== snapshot) {
            previous.snapshot?.dispose();
        }

        const opened = new Map<string, MemoizedProject>();
        return {
            projectFor: key => {
                let project = opened.get(key);
                if (!project) {
                    const configPath = configOf.get(key);
                    const found = configPath === undefined ? undefined : snapshot.getConfiguredProject(configPath);
                    if (!found) {
                        throw new Error(`TypeScript did not open a project at ${configPath ?? key}`);
                    }
                    restoreInputText(found, sources, restore);
                    const checker = memoizing(found.checker, this.compilerApi());
                    opened.set(key, project = new Proxy(found, {
                        get: (target, property: string, receiver) =>
                            property === "checker" ? checker : Reflect.get(target, property, receiver),
                    }) as MemoizedProject);
                }
                return project;
            },
            close: () => this.forget(sources.keys(), configPaths),
        };
    }

    /** The project options `configFilePath` states, with `extends` resolved, as tsconfig JSON names. */
    parseConfigFile(configFilePath: string): Record<string, unknown> {
        return this.compilerApi().parseConfigFile(configFilePath).options as Record<string, unknown>;
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
        return this.api ??= new API({cwd: this.root});
    }

    private forget(sources: Iterable<string>, configPaths: Iterable<string>): void {
        for (const file of sources) {
            this.files.delete(file);
        }
        for (const configPath of configPaths) {
            this.files.delete(configPath);
        }
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

/** Puts a file's own text on its source file, in place of what the compiler read. */
function restoreInputText(project: Project, sources: ReadonlyMap<string, string>,
                          restore: ReadonlySet<string>): void {
    for (const file of restore) {
        const sourceFile = project.program.getSourceFile(file);
        if (sourceFile !== undefined) {
            Object.defineProperty(sourceFile, "text", {value: sources.get(file), configurable: true});
        }
    }
}

/** Opens a single project for a single use, shutting the compiler down with it. */
export function openSession(
    root: string,
    sources: ReadonlyMap<string, string>,
    compilerOptions: Record<string, unknown>,
    rootFiles?: readonly string[],
): {project: MemoizedProject, close(): void} {
    const compiler = new Compiler(root);
    const key = "";
    const session = compiler.open(sources, [{key, options: compilerOptions, rootFiles: rootFiles ?? [...sources.keys()]}]);
    return {project: session.projectFor(key), close: () => compiler.close()};
}
