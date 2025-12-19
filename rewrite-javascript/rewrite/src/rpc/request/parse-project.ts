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
import {produce} from "immer";
import {SourceFile} from "../../tree";
import {withMetrics} from "./metrics";
import {
    JS,
    JavaScriptParser,
    PackageJsonParser
} from "../../javascript";
import {ProjectParser, DEFAULT_EXCLUSIONS} from "../../javascript/project-parser";
import {Json, JsonParser} from "../../json";
import {Yaml, YamlParser} from "../../yaml";
import {PlainTextParser, PlainText} from "../../text";
import {PrettierConfigLoader} from "../../javascript/format/prettier-config-loader";

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
        private readonly exclusions?: string[]
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

                    const projectPath = path.resolve(request.projectPath);
                    const exclusions = request.exclusions ?? DEFAULT_EXCLUSIONS;

                    // Use ProjectParser for file discovery
                    const projectParser = new ProjectParser(projectPath, {exclusions});
                    const discovered = await projectParser.discoverFiles();

                    const resultItems: ParseProjectResponseItem[] = [];
                    const ctx = new ExecutionContext();

                    // Detect Prettier configuration once for the project
                    const prettierLoader = new PrettierConfigLoader(projectPath);
                    await prettierLoader.detectPrettier();

                    // Parse package.json files (these get NodeResolutionResult markers)
                    if (discovered.packageJsonFiles.length > 0) {
                        const packageJsonParser = new PackageJsonParser({
                            ctx,
                            relativeTo: projectPath
                        });
                        const generator = packageJsonParser.parse(...discovered.packageJsonFiles);

                        for (const _ of discovered.packageJsonFiles) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return produce(sourceFile, (draft) => {
                                    draft.id = id;
                                });
                            });
                            resultItems.push({
                                id,
                                sourceFileType: Json.Kind.Document
                            });
                        }
                    }

                    // Parse JSON lock files
                    if (discovered.lockFiles.json.length > 0) {
                        const jsonParser = new JsonParser({ctx, relativeTo: projectPath});
                        const generator = jsonParser.parse(...discovered.lockFiles.json);

                        for (const _ of discovered.lockFiles.json) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return produce(sourceFile, (draft) => {
                                    draft.id = id;
                                });
                            });
                            resultItems.push({
                                id,
                                sourceFileType: Json.Kind.Document
                            });
                        }
                    }

                    // Parse YAML lock files
                    if (discovered.lockFiles.yaml.length > 0) {
                        const yamlParser = new YamlParser({ctx, relativeTo: projectPath});
                        const generator = yamlParser.parse(...discovered.lockFiles.yaml);

                        for (const _ of discovered.lockFiles.yaml) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return produce(sourceFile, (draft) => {
                                    draft.id = id;
                                });
                            });
                            resultItems.push({
                                id,
                                sourceFileType: Yaml.Kind.Documents
                            });
                        }
                    }

                    // Parse text lock files (yarn.lock Classic)
                    if (discovered.lockFiles.text.length > 0) {
                        const textParser = new PlainTextParser({ctx, relativeTo: projectPath});
                        const generator = textParser.parse(...discovered.lockFiles.text);

                        for (const _ of discovered.lockFiles.text) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return produce(sourceFile, (draft) => {
                                    draft.id = id;
                                });
                            });
                            resultItems.push({
                                id,
                                sourceFileType: PlainText.Kind.PlainText
                            });
                        }
                    }

                    // Parse JavaScript/TypeScript source files
                    if (discovered.jsFiles.length > 0) {
                        const jsParser = new JavaScriptParser({
                            ctx,
                            relativeTo: projectPath
                        });
                        const generator = jsParser.parse(...discovered.jsFiles);

                        for (const filePath of discovered.jsFiles) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                // Add PrettierStyle marker if Prettier is available
                                const prettierMarker = await prettierLoader.getConfigMarker(filePath);
                                return produce(sourceFile, (draft) => {
                                    draft.id = id;
                                    if (prettierMarker) {
                                        draft.markers.markers = draft.markers.markers.concat([prettierMarker]);
                                    }
                                });
                            });
                            resultItems.push({
                                id,
                                sourceFileType: JS.Kind.CompilationUnit
                            });
                        }
                    }

                    // Parse other YAML files
                    if (discovered.yamlFiles.length > 0) {
                        const yamlParser = new YamlParser({ctx, relativeTo: projectPath});
                        const generator = yamlParser.parse(...discovered.yamlFiles);

                        for (const _ of discovered.yamlFiles) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return produce(sourceFile, (draft) => {
                                    draft.id = id;
                                });
                            });
                            resultItems.push({
                                id,
                                sourceFileType: Yaml.Kind.Documents
                            });
                        }
                    }

                    // Parse other JSON files
                    if (discovered.jsonFiles.length > 0) {
                        const jsonParser = new JsonParser({ctx, relativeTo: projectPath});
                        const generator = jsonParser.parse(...discovered.jsonFiles);

                        for (const _ of discovered.jsonFiles) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return produce(sourceFile, (draft) => {
                                    draft.id = id;
                                });
                            });
                            resultItems.push({
                                id,
                                sourceFileType: Json.Kind.Document
                            });
                        }
                    }

                    // Parse text config files (.prettierignore, .gitignore, etc.)
                    if (discovered.textFiles.length > 0) {
                        const textParser = new PlainTextParser({ctx, relativeTo: projectPath});
                        const generator = textParser.parse(...discovered.textFiles);

                        for (const _ of discovered.textFiles) {
                            const id = randomId();
                            localObjects.set(id, async (id: string) => {
                                const sourceFile: SourceFile = (await generator.next()).value;
                                return produce(sourceFile, (draft) => {
                                    draft.id = id;
                                });
                            });
                            resultItems.push({
                                id,
                                sourceFileType: PlainText.Kind.PlainText
                            });
                        }
                    }

                    return resultItems;
                }
            )
        );
    }
}
