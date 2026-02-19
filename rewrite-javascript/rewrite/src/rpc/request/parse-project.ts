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
        private readonly relativeTo?: string
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

                    const resultItems: ParseProjectResponseItem[] = [];
                    const ctx = new ExecutionContext();

                    // Parse package.json files (these get NodeResolutionResult markers)
                    if (discovered.packageJsonFiles.length > 0) {
                        const parser = Parsers.createParser("packageJson", {
                            ctx,
                            relativeTo
                        });
                        const generator = parser.parse(...discovered.packageJsonFiles);

                        for (const _ of discovered.packageJsonFiles) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return { ...sourceFile, id };
                            });
                            resultItems.push({
                                id,
                                sourceFileType: "org.openrewrite.json.tree.Json$Document" // break cycle
                            });
                        }
                    }

                    // Parse JSON lock files
                    if (discovered.lockFiles.json.length > 0) {
                        const parser = Parsers.createParser("json", {ctx, relativeTo});
                        const generator = parser.parse(...discovered.lockFiles.json);

                        for (const _ of discovered.lockFiles.json) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return { ...sourceFile, id };
                            });
                            resultItems.push({
                                id,
                                sourceFileType: "org.openrewrite.json.tree.Json$Document" // break cycle
                            });
                        }
                    }

                    // Parse YAML lock files
                    if (discovered.lockFiles.yaml.length > 0) {
                        const parser = Parsers.createParser("yaml", {ctx, relativeTo});
                        const generator = parser.parse(...discovered.lockFiles.yaml);

                        for (const _ of discovered.lockFiles.yaml) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return { ...sourceFile, id };
                            });
                            resultItems.push({
                                id,
                                sourceFileType: "org.openrewrite.yaml.tree.Yaml$Documents" // break cycle
                            });
                        }
                    }

                    // Parse text lock files (yarn.lock Classic)
                    if (discovered.lockFiles.text.length > 0) {
                        const parser = Parsers.createParser("plainText", {ctx, relativeTo});
                        const generator = parser.parse(...discovered.lockFiles.text);

                        for (const _ of discovered.lockFiles.text) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return { ...sourceFile, id };
                            });
                            resultItems.push({
                                id,
                                sourceFileType: "org.openrewrite.text.PlainText" // break cycle
                            });
                        }
                    }

                    // Parse JavaScript/TypeScript source files
                    if (discovered.jsFiles.length > 0) {
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
                                    sourceFileType: "org.openrewrite.javascript.tree.JS$CompilationUnit" // break cycle
                                });
                            }
                        } else {
                            // Prettier is NOT available: auto-detect styles from parsed files
                            // Parse all files first to sample them
                            const parsedFiles: {id: string, sourceFile: SourceFile}[] = [];
                            for await (const sourceFile of parser.parse(...discovered.jsFiles)) {
                                const id = randomId();
                                parsedFiles.push({id, sourceFile});
                            }

                            // Sample all parsed files and build Autodetect marker using ProjectParser helper
                            const autodetectMarker = await projectParser.buildAutodetectMarker(
                                parsedFiles.map(p => p.sourceFile)
                            );

                            // Store thunks that add the Autodetect marker
                            for (const {id, sourceFile} of parsedFiles) {
                                localObjects.set(id, async (newId: string) => {
                                    return {
                                        ...sourceFile,
                                        id: newId,
                                        markers: replaceMarkerByKind(sourceFile.markers, autodetectMarker)
                                    };
                                });
                                resultItems.push({
                                    id,
                                    sourceFileType: "org.openrewrite.javascript.tree.JS$CompilationUnit" // break cycle
                                });
                            }
                        }
                    }

                    // Parse other YAML files
                    if (discovered.yamlFiles.length > 0) {
                        const parser = Parsers.createParser("yaml", {ctx, relativeTo});
                        const generator = parser.parse(...discovered.yamlFiles);

                        for (const _ of discovered.yamlFiles) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return { ...sourceFile, id };
                            });
                            resultItems.push({
                                id,
                                sourceFileType: "org.openrewrite.yaml.tree.Yaml$Documents" // break cycle
                            });
                        }
                    }

                    // Parse other JSON files
                    if (discovered.jsonFiles.length > 0) {
                        const parser = Parsers.createParser("json", {ctx, relativeTo});
                        const generator = parser.parse(...discovered.jsonFiles);

                        for (const _ of discovered.jsonFiles) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return { ...sourceFile, id };
                            });
                            resultItems.push({
                                id,
                                sourceFileType: "org.openrewrite.json.tree.Json$Document" // break cycle
                            });
                        }
                    }

                    // Parse text config files (.prettierignore, .gitignore, etc.)
                    if (discovered.textFiles.length > 0) {
                        const parser = Parsers.createParser("plainText", {ctx, relativeTo});
                        const generator = parser.parse(...discovered.textFiles);

                        for (const _ of discovered.textFiles) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return { ...sourceFile, id };
                            });
                            resultItems.push({
                                id,
                                sourceFileType: "org.openrewrite.text.PlainText" // break cycle
                            });
                        }
                    }

                    return resultItems;
                }
            )
        );
    }
}
