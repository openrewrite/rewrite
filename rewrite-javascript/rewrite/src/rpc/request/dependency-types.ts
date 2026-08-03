/*
 * Copyright 2026 the original author or authors.
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
import * as fs from "fs";
import * as path from "path";
import {RpcObjectData, RpcSendQueue} from "../queue";
import {ReferenceMap, asRef} from "../../reference";
import {JavaSender} from "../../java/rpc";
import {Type} from "../../java";
import {withMetrics} from "./metrics";
import {lastParsedProject} from "./last-parsed-project";

const JAVA_TYPE_CLASS = "org.openrewrite.java.tree.JavaType$Class";

type ExportedTypes = typeof import("../../javascript/package-exported-types.js")["exportedTypes"];

/**
 * Handle a DependencyTypes request: resolve one dependency, named by its npm coordinate, against
 * the most recently parsed project's node_modules, enumerate its public types, and stream them
 * back terminated by END_OF_OBJECT. Built once, returned in {@code batchSize} slices across the
 * client's repeated pulls. A {@code null} version names a TypeScript runtime declaration file
 * (e.g. {@code lib.dom}) instead of an installed package.
 */
export class DependencyTypes {
    constructor(private readonly name: string,
                private readonly version?: string | null) {
    }

    static handle(connection: rpc.MessageConnection, batchSize: number, metricsCsv?: string): void {
        // Keyed by coordinate: holds the remaining slices of an in-flight
        // response across the client's repeated pulls. Evicted once fully drained.
        const pending = new Map<string, RpcObjectData[]>();

        connection.onRequest(
            new rpc.RequestType<DependencyTypes, RpcObjectData[], Error>("DependencyTypes"),
            withMetrics<DependencyTypes, RpcObjectData[]>(
                "DependencyTypes",
                metricsCsv,
                (context) => async (request) => {
                    const version = request.version ?? null;
                    context.target = version ? `${request.name}@${version}` : request.name;

                    const key = JSON.stringify([request.name, version]);
                    if (!pending.has(key)) {
                        // Dynamic import to break the circular dependency with the javascript module.
                        const {exportedTypes} = await import("../../javascript/package-exported-types.js");
                        const types = version !== null
                            ? packageTypes(request.name, exportedTypes)
                            : exportedTypes([runtimeLibFile(request.name)], []);

                        const q = new RpcSendQueue(new ReferenceMap(), JAVA_TYPE_CLASS, false);
                        const sender = new JavaSender();
                        // FQNs this dependency defines, sent before the types so the caller can tell
                        // defined types from references up front, then the types themselves.
                        await q.sendList(types.map(t => Type.FullyQualified.getFullyQualifiedName(t)), undefined, s => s);
                        await q.sendList(
                            types.map(t => asRef(t)),
                            undefined,
                            t => Type.signature(t),
                            async t => {
                                await sender.visitType(t, q);
                            }
                        );
                        pending.set(key, q.finish());
                    }

                    const data = pending.get(key)!;
                    const batch = data.splice(0, batchSize);
                    if (data.length === 0) {
                        pending.delete(key);
                    }
                    return batch;
                }
            )
        );
    }
}

function projectNodeModules(): string {
    const project = lastParsedProject();
    if (!project) {
        throw new Error("No project has been parsed yet; cannot resolve a dependency coordinate");
    }
    return path.join(project, "node_modules");
}

/** The installed package's own types, falling back to its DefinitelyTyped package when it ships none. */
function packageTypes(name: string, exportedTypes: ExportedTypes): Type.FullyQualified[] {
    const nodeModules = projectNodeModules();
    const pkgDir = path.join(nodeModules, ...name.split("/"));
    if (!fs.existsSync(pkgDir)) {
        throw new Error(`Package '${name}' is not installed under ${nodeModules}`);
    }
    const types = exportedTypes([pkgDir], []);
    if (types.length > 0) {
        return types;
    }
    // @scope/pkg -> @types/scope__pkg
    const typesDir = path.join(nodeModules, "@types",
        name.startsWith("@") ? name.slice(1).replace("/", "__") : name);
    return fs.existsSync(typesDir) ? exportedTypes([typesDir], []) : types;
}

/** A runtime declaration file (e.g. lib.dom.d.ts): the project's installed TypeScript first, else the engine's own. */
function runtimeLibFile(name: string): string {
    for (const lib of [
        path.join(projectNodeModules(), "typescript", "lib", `${name}.d.ts`),
        path.join(path.dirname(require.resolve("typescript")), `${name}.d.ts`),
    ]) {
        if (fs.existsSync(lib)) {
            return lib;
        }
    }
    throw new Error(`No TypeScript declaration file for '${name}'`);
}
