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
import {castDraft} from "mutative";
import {Marker} from "../markers";
import {asRef} from "../reference";
import {updateIfChanged} from "../util";
import {UUID} from "../uuid";
import {RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "./queue";

/*
 * The Java `org.openrewrite.python.marker.PythonResolutionResult` marker, which the Java host
 * attaches to requirements.txt, setup.cfg, Pipfile and pyproject.toml. A JavaScript recipe run over
 * a repository that also holds Python project files receives it on those files, and the Java codec
 * streams it property by property, so this side needs a codec for it and every nested type to read
 * exactly the messages Java sends. The field order in each codec is the wire contract and mirrors
 * `rpcSend`/`rpcReceive` in the Java class.
 */

export const PythonResolutionResultKind = "org.openrewrite.python.marker.PythonResolutionResult" as const;
export const PythonDependencyKind = "org.openrewrite.python.marker.PythonResolutionResult$Dependency" as const;
export const PythonResolvedDependencyKind = "org.openrewrite.python.marker.PythonResolutionResult$ResolvedDependency" as const;
export const PythonSourceIndexKind = "org.openrewrite.python.marker.PythonResolutionResult$SourceIndex" as const;

/**
 * The Java `PythonResolutionResult.PackageManager` enum, sent as its constant name.
 */
export type PythonPackageManager = "Uv" | "Pip" | "Pipenv" | "Poetry" | "Pdm";

/**
 * A dependency requested in the project's metadata, parsed from a PEP 508 string.
 */
export interface PythonDependency {
    readonly kind: typeof PythonDependencyKind;
    readonly name: string;
    readonly versionConstraint?: string;
    readonly extras?: string[];
    readonly marker?: string;
    /**
     * The locked resolution of this request, the same instance the marker's
     * `resolvedDependencies` holds.
     */
    readonly resolved?: PythonResolvedDependency;
}

/**
 * A dependency locked by a lock file. The `dependencies` of each entry point at other entries of
 * the same graph, which may contain cycles.
 */
export interface PythonResolvedDependency {
    readonly kind: typeof PythonResolvedDependencyKind;
    readonly name: string;
    readonly version: string;
    readonly source?: string;
    readonly dependencies?: PythonResolvedDependency[];
}

export interface PythonSourceIndex {
    readonly kind: typeof PythonSourceIndexKind;
    readonly name: string;
    readonly url: string;
    readonly defaultIndex: boolean;
}

/**
 * The Java codec sends these maps as plain JSON values rather than property by property, so their
 * dependency entries arrive as Jackson's rendering of the Java objects, without a `kind`.
 */
export type PythonDependenciesByName = Readonly<Record<string, readonly unknown[]>>;

export interface PythonResolutionResult extends Marker {
    readonly kind: typeof PythonResolutionResultKind;
    readonly id: UUID;
    readonly name?: string;
    readonly version?: string;
    readonly description?: string;
    readonly license?: string;
    readonly path: string;
    readonly requiresPython?: string;
    readonly buildBackend?: string;
    readonly buildRequires: PythonDependency[];
    readonly dependencies: PythonDependency[];
    readonly optionalDependencies: PythonDependenciesByName;
    readonly dependencyGroups: PythonDependenciesByName;
    readonly constraintDependencies: PythonDependency[];
    readonly overrideDependencies: PythonDependency[];
    readonly resolvedDependencies: PythonResolvedDependency[];
    readonly packageManager?: PythonPackageManager;
    readonly sourceIndexes?: PythonSourceIndex[];
}

const dependencyId = (dep: PythonDependency) => `${dep.name}@${dep.versionConstraint}`;
const resolvedId = (resolved: PythonResolvedDependency) => `${resolved.name}@${resolved.version}`;

RpcCodecs.registerCodec(PythonDependencyKind, {
    async rpcReceive(before: PythonDependency, q: RpcReceiveQueue): Promise<PythonDependency> {
        // Populated in place: the receive queue registered this instance for its ref before
        // calling the codec, so a back-reference resolves to the finished object.
        const dep = castDraft(before);
        dep.name = await q.receive(before.name);
        dep.versionConstraint = await q.receive(before.versionConstraint);
        dep.extras = await q.receive(before.extras);
        dep.marker = await q.receive(before.marker);
        dep.resolved = await q.receive(before.resolved);
        return before;
    },

    async rpcSend(after: PythonDependency, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.name);
        await q.getAndSend(after, a => a.versionConstraint);
        await q.getAndSend(after, a => a.extras);
        await q.getAndSend(after, a => a.marker);
        await q.getAndSend(after, a => asRef(a.resolved));
    }
});

RpcCodecs.registerCodec(PythonResolvedDependencyKind, {
    async rpcReceive(before: PythonResolvedDependency, q: RpcReceiveQueue): Promise<PythonResolvedDependency> {
        const resolved = castDraft(before);
        resolved.name = await q.receive(before.name);
        resolved.version = await q.receive(before.version);
        resolved.source = await q.receive(before.source);
        resolved.dependencies = castDraft(await q.receiveList(before.dependencies));
        return before;
    },

    async rpcSend(after: PythonResolvedDependency, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.name);
        await q.getAndSend(after, a => a.version);
        await q.getAndSend(after, a => a.source);
        // Java sends an absent list as an empty one
        await q.getAndSendList(after, a => (a.dependencies ?? []).map(d => asRef(d)), resolvedId);
    }
});

RpcCodecs.registerCodec(PythonSourceIndexKind, {
    async rpcReceive(before: PythonSourceIndex, q: RpcReceiveQueue): Promise<PythonSourceIndex> {
        return updateIfChanged(before, {
            name: await q.receive(before.name),
            url: await q.receive(before.url),
            defaultIndex: await q.receive(before.defaultIndex),
        });
    },

    async rpcSend(after: PythonSourceIndex, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.name);
        await q.getAndSend(after, a => a.url);
        await q.getAndSend(after, a => a.defaultIndex);
    }
});

RpcCodecs.registerCodec(PythonResolutionResultKind, {
    async rpcReceive(before: PythonResolutionResult, q: RpcReceiveQueue): Promise<PythonResolutionResult> {
        return updateIfChanged(before, {
            id: await q.receive(before.id),
            name: await q.receive(before.name),
            version: await q.receive(before.version),
            description: await q.receive(before.description),
            license: await q.receive(before.license),
            path: await q.receive(before.path),
            requiresPython: await q.receive(before.requiresPython),
            buildBackend: await q.receive(before.buildBackend),
            buildRequires: await q.receiveListDefined(before.buildRequires),
            dependencies: await q.receiveListDefined(before.dependencies),
            optionalDependencies: await q.receive(before.optionalDependencies),
            dependencyGroups: await q.receive(before.dependencyGroups),
            constraintDependencies: await q.receiveListDefined(before.constraintDependencies),
            overrideDependencies: await q.receiveListDefined(before.overrideDependencies),
            resolvedDependencies: await q.receiveListDefined(before.resolvedDependencies),
            packageManager: await q.receive(before.packageManager),
            sourceIndexes: await q.receiveList(before.sourceIndexes),
        });
    },

    async rpcSend(after: PythonResolutionResult, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.name);
        await q.getAndSend(after, a => a.version);
        await q.getAndSend(after, a => a.description);
        await q.getAndSend(after, a => a.license);
        await q.getAndSend(after, a => a.path);
        await q.getAndSend(after, a => a.requiresPython);
        await q.getAndSend(after, a => a.buildBackend);
        await q.getAndSendList(after, a => a.buildRequires.map(d => asRef(d)), dependencyId);
        await q.getAndSendList(after, a => a.dependencies.map(d => asRef(d)), dependencyId);
        await q.getAndSend(after, a => a.optionalDependencies);
        await q.getAndSend(after, a => a.dependencyGroups);
        await q.getAndSendList(after, a => a.constraintDependencies.map(d => asRef(d)), dependencyId);
        await q.getAndSendList(after, a => a.overrideDependencies.map(d => asRef(d)), dependencyId);
        await q.getAndSendList(after, a => a.resolvedDependencies.map(r => asRef(r)), resolvedId);
        await q.getAndSend(after, a => a.packageManager);
        // Java sends an absent list as an empty one
        await q.getAndSendList(after, a => a.sourceIndexes ?? [], si => si.name);
    }
});
