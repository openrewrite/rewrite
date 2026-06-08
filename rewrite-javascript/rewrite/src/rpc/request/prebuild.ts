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
import {withMetrics} from "./metrics";
import type {ProjectDescriptor} from "../../javascript/workspace-discovery";

/**
 * Result of the Prebuild RPC: the independent projects discovered in the repository
 * (workspace-aware), each with its source sets, config-input watch-set, and parser
 * settings. The JavaScript analog of "invoke the build tool" — it produces the build
 * descriptor a consumer uses to drive builds; it does not parse sources.
 *
 * v1 is read-only: no dependency install, and {@link ProjectDescriptor.resolution} is
 * always null.
 */
export interface PrebuildResult {
    projects: ProjectDescriptor[];
}

/**
 * RPC request to compute the build descriptor for a repository (or partition) root.
 *
 * Mirrors {@link ParseProject}'s shape: optional {@code exclusions}, a {@code relativeTo}
 * base for returned paths, and a forward-compat {@code options} carrier (unused in v1).
 */
export class Prebuild {
    constructor(
        private readonly repositoryRoot: string,
        private readonly exclusions?: string[],
        /**
         * Optional base for all returned paths. Defaults to {@code repositoryRoot}.
         * Use when prebuilding a subdirectory but wanting paths relative to the repo root.
         */
        private readonly relativeTo?: string,
        /**
         * Forward-compat carrier for future options (e.g. stateful-session hints).
         * Unused in v1.
         */
        private readonly options?: Record<string, unknown>
    ) {}

    static handle(connection: rpc.MessageConnection, metricsCsv?: string): void {
        connection.onRequest(
            new rpc.RequestType<Prebuild, PrebuildResult, Error>("Prebuild"),
            withMetrics<Prebuild, PrebuildResult>(
                "Prebuild",
                metricsCsv,
                (context) => async (request) => {
                    context.target = request.repositoryRoot;

                    // Dynamic import to break circular dependency (cf. parse-project.ts).
                    const {discoverProjects} = await import("../../javascript/index.js");

                    const repositoryRoot = path.resolve(request.repositoryRoot);
                    const relativeTo = request.relativeTo ? path.resolve(request.relativeTo) : repositoryRoot;

                    const projects = await discoverProjects(repositoryRoot, {
                        exclusions: request.exclusions,
                        relativeTo
                    });

                    return {projects};
                }
            )
        );
    }
}
