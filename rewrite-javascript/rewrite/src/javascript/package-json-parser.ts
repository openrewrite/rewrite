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
import {Parser, ParserInput, parserInputFile, parserInputRead, ParserOptions, Parsers} from "../parser";
import {SourceFile} from "../tree";
import {Json, JsonParser} from "../json";
import {createNodeResolutionResultMarker, NodeResolutionResult} from "./node-resolution-result";
import * as fs from "fs";
import * as path from "path";

export interface PackageJsonParserOptions extends ParserOptions {
    /**
     * If true, skips reading and parsing lock files for dependency resolution.
     * The NodeResolutionResult marker will still be created, but without resolved dependencies.
     */
    skipDependencyResolution?: boolean;
}

/**
 * A parser for package.json files that wraps the JsonParser.
 *
 * Similar to how MavenParser wraps XmlParser in Java, this parser:
 * - Parses package.json files as JSON documents
 * - Attaches NodeResolutionResult markers with dependency information
 * - Optionally reads corresponding lock files (package-lock.json, yarn.lock, etc.)
 *   to provide resolved dependency versions
 */
export class PackageJsonParser extends Parser {
    private readonly jsonParser: JsonParser;
    private readonly skipDependencyResolution: boolean;

    constructor(options: PackageJsonParserOptions = {}) {
        super(options);
        this.jsonParser = new JsonParser(options);
        this.skipDependencyResolution = options.skipDependencyResolution ?? false;
    }

    /**
     * Accepts package.json files.
     */
    accept(sourcePath: string): boolean {
        const fileName = path.basename(sourcePath);
        return fileName === 'package.json';
    }

    async *parse(...inputs: ParserInput[]): AsyncGenerator<SourceFile> {
        // Group inputs by directory to share NodeResolutionResult markers
        const inputsByDir = new Map<string, ParserInput[]>();

        for (const input of inputs) {
            const filePath = parserInputFile(input);
            const dir = path.dirname(filePath);

            if (!inputsByDir.has(dir)) {
                inputsByDir.set(dir, []);
            }
            inputsByDir.get(dir)!.push(input);
        }

        // Process each directory's package.json files
        for (const [dir, dirInputs] of inputsByDir) {
            // Create a shared marker for this directory
            let marker: NodeResolutionResult | null = null;

            for (const input of dirInputs) {
                // Parse as JSON first
                const jsonGenerator = this.jsonParser.parse(input);
                const jsonResult = await jsonGenerator.next();

                if (jsonResult.done || !jsonResult.value) {
                    continue;
                }

                const jsonDoc = jsonResult.value as Json.Document;

                // Create NodeResolutionResult marker if not already created for this directory
                if (!marker) {
                    marker = this.createMarker(input, dir);
                }

                // Attach the marker to the JSON document
                if (marker) {
                    yield {
                        ...jsonDoc,
                        markers: {
                            ...jsonDoc.markers,
                            markers: [...jsonDoc.markers.markers, marker]
                        }
                    };
                } else {
                    yield jsonDoc;
                }
            }
        }
    }

    /**
     * Creates a NodeResolutionResult marker from the package.json content and optional lock file.
     */
    private createMarker(input: ParserInput, dir: string): NodeResolutionResult | null {
        try {
            const content = parserInputRead(input);
            const packageJson = JSON.parse(content);

            // Determine the relative path for the marker
            const filePath = parserInputFile(input);
            const relativePath = this.relativeTo
                ? path.relative(this.relativeTo, filePath)
                : filePath;

            // Try to read lock file if dependency resolution is not skipped
            // Use relativeTo directory if available (for tests), otherwise use the directory from input path
            let lockContent: any = undefined;
            if (!this.skipDependencyResolution) {
                const lockDir = this.relativeTo || dir;
                lockContent = this.tryReadLockFile(lockDir);
            }

            return createNodeResolutionResultMarker(relativePath, packageJson, lockContent);
        } catch (error) {
            console.warn(`Failed to create NodeResolutionResult marker: ${error}`);
            return null;
        }
    }

    /**
     * Attempts to read and parse a lock file from the given directory.
     * Tries package-lock.json first, then other lock file formats.
     *
     * @returns Parsed lock file content, or undefined if no lock file found
     */
    private tryReadLockFile(dir: string): any {
        // Try package-lock.json (npm)
        const npmLockPath = path.join(dir, 'package-lock.json');
        if (fs.existsSync(npmLockPath)) {
            try {
                const content = fs.readFileSync(npmLockPath, 'utf-8');
                return JSON.parse(content);
            } catch (error) {
                // Silently ignore parse errors
            }
        }

        // TODO: Add support for yarn.lock and pnpm-lock.yaml
        // These have different formats and would need separate parsing

        return undefined;
    }
}

// Register with the Parsers registry for RPC support
Parsers.registerParser("packageJson", PackageJsonParser);
