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
import {Marker, replaceMarkerByKind} from "../../markers";
import {setLastParsedProject} from "./last-parsed-project";

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
         * Parser options, as on the `Parse` request.
         */
        private readonly options?: { [key: string]: string }
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
                    const {ProjectParser} = await import("../../javascript/index.js");

                    const projectPath = path.resolve(request.projectPath);
                    setLastParsedProject(projectPath);
                    // Use relativeTo if specified, otherwise default to projectPath
                    const relativeTo = request.relativeTo ? path.resolve(request.relativeTo) : projectPath;

                    // Undefined when unset, so the parser can match exclusions to its discovery mode.
                    const projectParser = new ProjectParser(projectPath, {exclusions: request.exclusions});
                    const discovered = await projectParser.discoverFiles();
                    const prettierLoader = await projectParser.createPrettierLoader();

                    const resultItems: ParseProjectResponseItem[] = [];
                    const ctx = new ExecutionContext({...request.options});

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
                    if (discovered.jsFiles.length > 0) {
                        const parser = Parsers.createParser("javascript", {
                            ctx,
                            relativeTo
                        });

                        const prettierMarkers = new Map<string, Marker>();
                        for (const filePath of discovered.jsFiles) {
                            const marker = await prettierLoader.getConfigMarker(filePath);
                            if (marker) {
                                prettierMarkers.set(filePath, marker);
                            }
                        }
                        const generator = parser.parse(...discovered.jsFiles);
                        if (prettierMarkers.size === discovered.jsFiles.length) {
                            for (const filePath of discovered.jsFiles) {
                                const marker = prettierMarkers.get(filePath)!;
                                const id = randomId();
                                localObjects.set(id, async (id: string) => {
                                    const sourceFile: SourceFile = (await generator.next()).value;
                                    return {
                                        ...sourceFile,
                                        id,
                                        markers: replaceMarkerByKind(sourceFile.markers, marker)
                                    };
                                });
                                resultItems.push({
                                    id,
                                    sourceFileType: "org.openrewrite.javascript.tree.JS$CompilationUnit",
                                    sourcePath: path.relative(relativeTo, filePath)
                                });
                            }
                        } else {
                            const parsedFiles: {id: string, sourceFile: SourceFile, filePath: string}[] = [];
                            let fileIndex = 0;
                            for await (const sourceFile of generator) {
                                parsedFiles.push({id: randomId(), sourceFile, filePath: discovered.jsFiles[fileIndex++]});
                            }
                            const autodetectMarker = await projectParser.buildAutodetectMarker(
                                parsedFiles.filter(p => !prettierMarkers.has(p.filePath)).map(p => p.sourceFile)
                            );
                            for (const {id, sourceFile, filePath} of parsedFiles) {
                                localObjects.set(id, async (newId: string) => ({
                                    ...sourceFile,
                                    id: newId,
                                    markers: replaceMarkerByKind(sourceFile.markers, prettierMarkers.get(filePath) ?? autodetectMarker)
                                }));
                                resultItems.push({
                                    id,
                                    sourceFileType: "org.openrewrite.javascript.tree.JS$CompilationUnit",
                                    sourcePath: path.relative(relativeTo, filePath)
                                });
                            }
                        }
                    }

                    // Unparseable JS/TS files (currently: too large) are recorded as
                    // Quarks. The Java side builds the Quark locally from sourcePath,
                    // so no GetObject round-trip and no content crosses the wire.
                    for (const filePath of discovered.unparseableFiles) {
                        resultItems.push({
                            id: randomId(),
                            sourceFileType: "org.openrewrite.quark.Quark", // break cycle
                            sourcePath: path.relative(relativeTo, filePath)
                        });
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
