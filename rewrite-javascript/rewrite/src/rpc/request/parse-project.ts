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
import * as rpc from "vscode-jsonrpc/node";
import * as path from "path";
import {ExecutionContext} from "../../execution";
import {randomId, UUID} from "../../uuid";
import {SourceFile} from "../../tree";
import {Parsers} from "../../parser";
import {withMetrics} from "./metrics";
import {replaceMarkerByKind} from "../../markers";

/**
 * Response item with object ID and source file type for proper deserialization.
 */
export interface ParseProjectResponseItem {
    id: UUID;
    sourceFileType: string;
    /**
     * Relative source path of the discovered file. Used by the Java side to report
     * per-file failures (e.g., a failed `GetObject` deserialization) without aborting
     * the rest of the stream. Optional for older callers.
     */
    sourcePath?: string;
}

/**
 * RPC request to parse an entire project directory.
 * Discovers and parses:
 * - JavaScript/TypeScript source files
 * - package.json files (with NodeResolutionResult markers)
 * - Lock files (package-lock.json as JSON, yarn.lock/pnpm-lock.yaml as YAML)
 *
 * Uses ProjectParser for file discovery and Prettier detection.
 */
export class ParseProject {
    constructor(
        private readonly projectPath: string,
        private readonly exclusions?: string[],
        /**
         * Optional path to make source file paths relative to.
         * If not specified, paths are relative to projectPath.
         * Use this when parsing a subdirectory but wanting paths relative to the repository root.
         */
        private readonly relativeTo?: string,
        /**
         * Optional subset of files to serialize and return, identified by their source path relative to
         * `relativeTo` (or `projectPath` when not set), normalized to forward slashes. When omitted the
         * whole project is returned. When present the whole project is still discovered and type-checked,
         * but only these files are serialized and returned.
         */
        private readonly files?: string[],
        /**
         * Forward-compatibility carrier for additional, as-yet-undefined parsing options. Reserved for a
         * future optimization (e.g. true incremental re-parsing); currently ignored by the server.
         */
        private readonly options?: Record<string, unknown>
    ) {}

    static handle(
        connection: rpc.MessageConnection,
        localObjects: Map<string, ((input: string) => any) | any>,
        metricsCsv?: string
    ): void {
        connection.onRequest(
            new rpc.RequestType<ParseProject, ParseProjectResponseItem[], Error>("ParseProject"),
            withMetrics<ParseProject, ParseProjectResponseItem[]>(
                "ParseProject",
                metricsCsv,
                (context) => async (request) => {
                    context.target = request.projectPath;

                    // Dynamic import to break circular dependency
                    const {DEFAULT_EXCLUSIONS, ProjectParser} = await import("../../javascript/index.js");

                    const projectPath = path.resolve(request.projectPath);
                    const exclusions = request.exclusions ?? DEFAULT_EXCLUSIONS;
                    // Use relativeTo if specified, otherwise default to projectPath
                    const relativeTo = request.relativeTo ? path.resolve(request.relativeTo) : projectPath;

                    // Use ProjectParser for file discovery and Prettier detection
                    const projectParser = new ProjectParser(projectPath, {exclusions});
                    const discovered = await projectParser.discoverFiles();
                    const prettierLoader = await projectParser.createPrettierLoader();

                    // Optional subset of files to serialize and return. When provided, the whole project is
                    // still discovered and (for JS/TS) type-checked so types resolve exactly as in a full
                    // parse, but only the files in this set are returned. Entries and response sourcePaths
                    // share the same base (relativeTo, else projectPath) and are normalized to "/". A subset
                    // entry that discovery wouldn't have parsed simply matches nothing.
                    const toForwardSlashes = (p: string) => p.split(/[\\/]/).join("/");
                    const subset = request.files != null
                        ? new Set(request.files.map(toForwardSlashes))
                        : undefined;
                    const inSubset = (absolutePath: string): boolean =>
                        !subset || subset.has(toForwardSlashes(path.relative(relativeTo, absolutePath)));

                    // For everything except JS/TS there is no cross-file type context, so we can restrict
                    // discovery to the subset up front. JS/TS files are intentionally left whole here and
                    // filtered at serialization time below (see the jsFiles block) to preserve type context.
                    if (subset) {
                        discovered.packageJsonFiles = discovered.packageJsonFiles.filter(inSubset);
                        discovered.lockFiles.json = discovered.lockFiles.json.filter(inSubset);
                        discovered.lockFiles.yaml = discovered.lockFiles.yaml.filter(inSubset);
                        discovered.lockFiles.text = discovered.lockFiles.text.filter(inSubset);
                        discovered.jsonFiles = discovered.jsonFiles.filter(inSubset);
                        discovered.yamlFiles = discovered.yamlFiles.filter(inSubset);
                        discovered.textFiles = discovered.textFiles.filter(inSubset);
                    }

                    const resultItems: ParseProjectResponseItem[] = [];
                    const ctx = new ExecutionContext();

                    // Parse package.json files (these get NodeResolutionResult markers)
                    if (discovered.packageJsonFiles.length > 0) {
                        const parser = Parsers.createParser("packageJson", {
                            ctx,
                            relativeTo
                        });
                        const generator = parser.parse(...discovered.packageJsonFiles);

                        for (const filePath of discovered.packageJsonFiles) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return { ...sourceFile, id };
                            });
                            resultItems.push({
                                id,
                                sourceFileType: "org.openrewrite.json.tree.Json$Document", // break cycle
                                sourcePath: path.relative(relativeTo, filePath)
                            });
                        }
                    }

                    // Parse JSON lock files
                    if (discovered.lockFiles.json.length > 0) {
                        const parser = Parsers.createParser("json", {ctx, relativeTo});
                        const generator = parser.parse(...discovered.lockFiles.json);

                        for (const filePath of discovered.lockFiles.json) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return { ...sourceFile, id };
                            });
                            resultItems.push({
                                id,
                                sourceFileType: "org.openrewrite.json.tree.Json$Document", // break cycle
                                sourcePath: path.relative(relativeTo, filePath)
                            });
                        }
                    }

                    // Parse YAML lock files
                    if (discovered.lockFiles.yaml.length > 0) {
                        const parser = Parsers.createParser("yaml", {ctx, relativeTo});
                        const generator = parser.parse(...discovered.lockFiles.yaml);

                        for (const filePath of discovered.lockFiles.yaml) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return { ...sourceFile, id };
                            });
                            resultItems.push({
                                id,
                                sourceFileType: "org.openrewrite.yaml.tree.Yaml$Documents", // break cycle
                                sourcePath: path.relative(relativeTo, filePath)
                            });
                        }
                    }

                    // Parse text lock files (yarn.lock Classic)
                    if (discovered.lockFiles.text.length > 0) {
                        const parser = Parsers.createParser("plainText", {ctx, relativeTo});
                        const generator = parser.parse(...discovered.lockFiles.text);

                        for (const filePath of discovered.lockFiles.text) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return { ...sourceFile, id };
                            });
                            resultItems.push({
                                id,
                                sourceFileType: "org.openrewrite.text.PlainText", // break cycle
                                sourcePath: path.relative(relativeTo, filePath)
                            });
                        }
                    }

                    // Parse JavaScript/TypeScript source files
                    if (discovered.jsFiles.length > 0 && subset) {
                        // Subset mode: parse the WHOLE project for type context (a single TypeScript program
                        // over every JS/TS file), but serialize and return only the files in the subset.
                        //
                        // We eagerly drain the parser into an array keyed by file path so we can pick the
                        // subset out by path. The full-project branch below relies on the parse generator
                        // being consumed in lockstep with resultItems, which no longer holds once non-subset
                        // files are skipped — hence the separate, eager handling here.
                        const parser = Parsers.createParser("javascript", {ctx, relativeTo});
                        const detection = await prettierLoader.detectPrettier();

                        const parsedFiles: { sourceFile: SourceFile, filePath: string }[] = [];
                        let fileIndex = 0;
                        for await (const sourceFile of parser.parse(...discovered.jsFiles)) {
                            parsedFiles.push({sourceFile, filePath: discovered.jsFiles[fileIndex++]});
                        }

                        // Without Prettier we sample parsed files to build an Autodetect marker. We've parsed
                        // the whole project here, so the sample is still project-wide; buildAutodetectMarker
                        // falls back to defaults if the sample is empty rather than crashing.
                        const autodetectMarker = detection.available
                            ? undefined
                            : await projectParser.buildAutodetectMarker(parsedFiles.map(p => p.sourceFile));

                        for (const {sourceFile, filePath} of parsedFiles) {
                            if (!inSubset(filePath)) {
                                continue;
                            }
                            const id = randomId();
                            localObjects.set(id, async (newId: string) => {
                                let markers = sourceFile.markers;
                                if (detection.available) {
                                    const prettierMarker = await prettierLoader.getConfigMarker(filePath);
                                    if (prettierMarker) {
                                        markers = replaceMarkerByKind(markers, prettierMarker);
                                    }
                                } else {
                                    markers = replaceMarkerByKind(markers, autodetectMarker!);
                                }
                                return {...sourceFile, id: newId, markers};
                            });
                            resultItems.push({
                                id,
                                sourceFileType: "org.openrewrite.javascript.tree.JS$CompilationUnit", // break cycle
                                sourcePath: path.relative(relativeTo, filePath)
                            });
                        }
                    } else if (discovered.jsFiles.length > 0) {
                        const parser = Parsers.createParser("javascript", {
                            ctx,
                            relativeTo
                        });

                        // Check if Prettier is available
                        const detection = await prettierLoader.detectPrettier();

                        if (detection.available) {
                            // Prettier is available: add per-file PrettierStyle markers
                            const generator = parser.parse(...discovered.jsFiles);

                            for (const filePath of discovered.jsFiles) {
                                const id = randomId();
                                localObjects.set(id, async (id: string) => {
                                    const sourceFile: SourceFile = (await generator.next()).value;
                                    // Add PrettierStyle marker if Prettier is available
                                    const prettierMarker = await prettierLoader.getConfigMarker(filePath);
                                    return {
                                        ...sourceFile,
                                        id,
                                        markers: prettierMarker
                                            ? replaceMarkerByKind(sourceFile.markers, prettierMarker)
                                            : sourceFile.markers
                                    };
                                });
                                resultItems.push({
                                    id,
                                    sourceFileType: "org.openrewrite.javascript.tree.JS$CompilationUnit", // break cycle
                                    sourcePath: path.relative(relativeTo, filePath)
                                });
                            }
                        } else {
                            // Prettier is NOT available: auto-detect styles from parsed files
                            // Parse all files first to sample them
                            const parsedFiles: {id: string, sourceFile: SourceFile, filePath: string}[] = [];
                            let fileIndex = 0;
                            for await (const sourceFile of parser.parse(...discovered.jsFiles)) {
                                const id = randomId();
                                parsedFiles.push({id, sourceFile, filePath: discovered.jsFiles[fileIndex++]});
                            }

                            // Sample all parsed files and build Autodetect marker using ProjectParser helper
                            const autodetectMarker = await projectParser.buildAutodetectMarker(
                                parsedFiles.map(p => p.sourceFile)
                            );

                            // Store thunks that add the Autodetect marker
                            for (const {id, sourceFile, filePath} of parsedFiles) {
                                localObjects.set(id, async (newId: string) => {
                                    return {
                                        ...sourceFile,
                                        id: newId,
                                        markers: replaceMarkerByKind(sourceFile.markers, autodetectMarker)
                                    };
                                });
                                resultItems.push({
                                    id,
                                    sourceFileType: "org.openrewrite.javascript.tree.JS$CompilationUnit", // break cycle
                                    sourcePath: path.relative(relativeTo, filePath)
                                });
                            }
                        }
                    }

                    // Parse other YAML files
                    if (discovered.yamlFiles.length > 0) {
                        const parser = Parsers.createParser("yaml", {ctx, relativeTo});
                        const generator = parser.parse(...discovered.yamlFiles);

                        for (const filePath of discovered.yamlFiles) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return { ...sourceFile, id };
                            });
                            resultItems.push({
                                id,
                                sourceFileType: "org.openrewrite.yaml.tree.Yaml$Documents", // break cycle
                                sourcePath: path.relative(relativeTo, filePath)
                            });
                        }
                    }

                    // Parse other JSON files
                    if (discovered.jsonFiles.length > 0) {
                        const parser = Parsers.createParser("json", {ctx, relativeTo});
                        const generator = parser.parse(...discovered.jsonFiles);

                        for (const filePath of discovered.jsonFiles) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return { ...sourceFile, id };
                            });
                            resultItems.push({
                                id,
                                sourceFileType: "org.openrewrite.json.tree.Json$Document", // break cycle
                                sourcePath: path.relative(relativeTo, filePath)
                            });
                        }
                    }

                    // Parse text config files (.prettierignore, .gitignore, etc.)
                    if (discovered.textFiles.length > 0) {
                        const parser = Parsers.createParser("plainText", {ctx, relativeTo});
                        const generator = parser.parse(...discovered.textFiles);

                        for (const filePath of discovered.textFiles) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return { ...sourceFile, id };
                            });
                            resultItems.push({
                                id,
                                sourceFileType: "org.openrewrite.text.PlainText", // break cycle
                                sourcePath: path.relative(relativeTo, filePath)
                            });
                        }
                    }

                    return resultItems;
                }
            )
        );
    }
}
