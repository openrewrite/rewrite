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
 * The Java `org.openrewrite.csharp.marker.MSBuildProject` marker, which the Java host attaches to
 * the Xml document of each .csproj file. A JavaScript recipe run over a repository that also holds
 * .NET projects receives it on those files, and the Java codec streams it property by property, so
 * this side needs a codec for it and every nested type to read exactly the messages Java sends. The
 * field order in each codec is the wire contract and mirrors `rpcSend`/`rpcReceive` in the Java class.
 */

export const MSBuildProjectKind = "org.openrewrite.csharp.marker.MSBuildProject" as const;
export const MSBuildTargetFrameworkKind = "org.openrewrite.csharp.marker.MSBuildProject$TargetFramework" as const;
export const MSBuildPackageReferenceKind = "org.openrewrite.csharp.marker.MSBuildProject$PackageReference" as const;
export const MSBuildResolvedPackageKind = "org.openrewrite.csharp.marker.MSBuildProject$ResolvedPackage" as const;
export const MSBuildProjectReferenceKind = "org.openrewrite.csharp.marker.MSBuildProject$ProjectReference" as const;
export const MSBuildPropertyValueKind = "org.openrewrite.csharp.marker.MSBuildProject$PropertyValue" as const;
export const MSBuildPackageSourceKind = "org.openrewrite.csharp.marker.MSBuildProject$PackageSource" as const;

/**
 * An MSBuild property value, with the file that defines it.
 */
export interface MSBuildPropertyValue {
    readonly kind: typeof MSBuildPropertyValueKind;
    readonly value: string;
    /**
     * The Java `Path`, sent as its string form.
     */
    readonly definedIn?: string;
}

/**
 * A NuGet package source from nuget.config.
 */
export interface MSBuildPackageSource {
    readonly kind: typeof MSBuildPackageSourceKind;
    readonly key: string;
    readonly url: string;
}

/**
 * A NuGet package reference declared in the .csproj or Directory.Packages.props.
 */
export interface MSBuildPackageReference {
    readonly kind: typeof MSBuildPackageReferenceKind;
    readonly include: string;
    readonly requestedVersion?: string;
    readonly resolvedVersion?: string;
}

/**
 * A NuGet package resolved by restore. The `dependencies` of each entry point at other entries of
 * the same graph, which may contain cycles.
 */
export interface MSBuildResolvedPackage {
    readonly kind: typeof MSBuildResolvedPackageKind;
    readonly name: string;
    readonly resolvedVersion: string;
    readonly dependencies: MSBuildResolvedPackage[];
    readonly depth: number;
    readonly type: string;
    readonly compileAssemblies: string[];
    readonly runtimeAssemblies: string[];
    readonly frameworkAssemblies: string[];
    readonly buildFiles: string[];
    readonly buildMultiTargetingFiles: string[];
    readonly contentFiles: string[];
    readonly runtimeTargets: string[];
    readonly resourceAssemblies: string[];
    readonly analyzerAssemblies: string[];
    readonly hasInstallScripts: boolean;
    readonly hasXdtTransforms: boolean;
    readonly hasLegacyContentFolder: boolean;
    /**
     * Declared dependency version range per child package id.
     */
    readonly dependencyRanges: Readonly<Record<string, string>>;
}

/**
 * A project-to-project reference from the restore graph. Its `resolvedPackages` are the same
 * instances the owning target framework's `resolvedPackages` holds.
 */
export interface MSBuildProjectReference {
    readonly kind: typeof MSBuildProjectReferenceKind;
    readonly include: string;
    readonly name?: string;
    readonly depth: number;
    readonly projectReferences: MSBuildProjectReference[];
    readonly resolvedPackages: MSBuildResolvedPackage[];
}

/**
 * What MSBuild evaluated for one target framework of the project.
 */
export interface MSBuildTargetFramework {
    readonly kind: typeof MSBuildTargetFrameworkKind;
    readonly targetFramework: string;
    readonly packageReferences: MSBuildPackageReference[];
    readonly resolvedPackages: MSBuildResolvedPackage[];
    readonly projectReferences: MSBuildProjectReference[];
}

export interface MSBuildProject extends Marker {
    readonly kind: typeof MSBuildProjectKind;
    readonly id: UUID;
    readonly sdk?: string;
    readonly properties: Readonly<Record<string, MSBuildPropertyValue>>;
    readonly packageSources: MSBuildPackageSource[];
    readonly targetFrameworks: MSBuildTargetFramework[];
}

const resolvedId = (rp: MSBuildResolvedPackage) => `${rp.name}@${rp.resolvedVersion}`;

/**
 * Java sends a map as two parallel lists, its keys and then its values. This zips them back, keeping
 * the map it had before when neither list changed.
 */
function zip<V>(keys: string[] | undefined, values: V[] | undefined,
                before: Readonly<Record<string, V>> | undefined): Readonly<Record<string, V>> {
    const entries: [string, V][] = [];
    if (keys && values) {
        for (let i = 0; i < keys.length && i < values.length; i++) {
            entries.push([keys[i], values[i]]);
        }
    }
    if (before) {
        const beforeEntries = Object.entries(before);
        if (beforeEntries.length === entries.length &&
            beforeEntries.every(([k, v], i) => entries[i][0] === k && entries[i][1] === v)) {
            return before;
        }
    }
    // fromEntries defines own properties, so a key like `__proto__` stays an entry
    return Object.fromEntries(entries);
}

function receiveStringList(q: RpcReceiveQueue, before: string[] | undefined): string[] {
    return q.receiveListDefined(before, s => q.receive(s));
}

function sendStringList<T>(q: RpcSendQueue, after: T, values: (parent: T) => string[] | undefined): void {
    q.getAndSendList(after, values, s => s, s => q.getAndSend(s, x => x));
}

RpcCodecs.registerCodec(MSBuildPropertyValueKind, {
    rpcReceive(before: MSBuildPropertyValue, q: RpcReceiveQueue): MSBuildPropertyValue {
        return updateIfChanged(before, {
            value: q.receive(before.value),
            definedIn: q.receive(before.definedIn),
        });
    },

    rpcSend(after: MSBuildPropertyValue, q: RpcSendQueue): void {
        q.getAndSend(after, a => a.value);
        q.getAndSend(after, a => a.definedIn);
    }
});

RpcCodecs.registerCodec(MSBuildPackageSourceKind, {
    rpcReceive(before: MSBuildPackageSource, q: RpcReceiveQueue): MSBuildPackageSource {
        return updateIfChanged(before, {
            key: q.receive(before.key),
            url: q.receive(before.url),
        });
    },

    rpcSend(after: MSBuildPackageSource, q: RpcSendQueue): void {
        q.getAndSend(after, a => a.key);
        q.getAndSend(after, a => a.url);
    }
});

RpcCodecs.registerCodec(MSBuildPackageReferenceKind, {
    rpcReceive(before: MSBuildPackageReference, q: RpcReceiveQueue): MSBuildPackageReference {
        return updateIfChanged(before, {
            include: q.receive(before.include),
            requestedVersion: q.receive(before.requestedVersion),
            resolvedVersion: q.receive(before.resolvedVersion),
        });
    },

    rpcSend(after: MSBuildPackageReference, q: RpcSendQueue): void {
        q.getAndSend(after, a => a.include);
        q.getAndSend(after, a => a.requestedVersion);
        q.getAndSend(after, a => a.resolvedVersion);
    }
});

RpcCodecs.registerCodec(MSBuildResolvedPackageKind, {
    rpcReceive(before: MSBuildResolvedPackage, q: RpcReceiveQueue): MSBuildResolvedPackage {
        // Populated in place: Java sends resolved packages only by ref, so the receive queue
        // registered this instance for its ref before calling the codec, and a back-reference
        // in the dependency graph resolves to the finished object.
        const rp = castDraft(before);
        rp.name = q.receive(before.name);
        rp.resolvedVersion = q.receive(before.resolvedVersion);
        rp.dependencies = castDraft(q.receiveListDefined(before.dependencies));
        rp.depth = q.receive(before.depth);
        rp.type = q.receive(before.type);
        rp.compileAssemblies = receiveStringList(q, before.compileAssemblies);
        rp.runtimeAssemblies = receiveStringList(q, before.runtimeAssemblies);
        rp.frameworkAssemblies = receiveStringList(q, before.frameworkAssemblies);
        rp.buildFiles = receiveStringList(q, before.buildFiles);
        rp.buildMultiTargetingFiles = receiveStringList(q, before.buildMultiTargetingFiles);
        rp.contentFiles = receiveStringList(q, before.contentFiles);
        rp.runtimeTargets = receiveStringList(q, before.runtimeTargets);
        rp.resourceAssemblies = receiveStringList(q, before.resourceAssemblies);
        rp.analyzerAssemblies = receiveStringList(q, before.analyzerAssemblies);
        rp.hasInstallScripts = q.receive(before.hasInstallScripts);
        rp.hasXdtTransforms = q.receive(before.hasXdtTransforms);
        rp.hasLegacyContentFolder = q.receive(before.hasLegacyContentFolder);
        const beforeRanges = before.dependencyRanges ?? {};
        const rangeKeys = receiveStringList(q, Object.keys(beforeRanges));
        const rangeValues = receiveStringList(q, Object.values(beforeRanges));
        rp.dependencyRanges = zip(rangeKeys, rangeValues, before.dependencyRanges);
        return before;
    },

    rpcSend(after: MSBuildResolvedPackage, q: RpcSendQueue): void {
        q.getAndSend(after, a => a.name);
        q.getAndSend(after, a => a.resolvedVersion);
        q.getAndSendList(after, a => a.dependencies?.map(d => asRef(d)), resolvedId);
        q.getAndSend(after, a => a.depth);
        q.getAndSend(after, a => a.type);
        sendStringList(q, after, a => a.compileAssemblies);
        sendStringList(q, after, a => a.runtimeAssemblies);
        sendStringList(q, after, a => a.frameworkAssemblies);
        sendStringList(q, after, a => a.buildFiles);
        sendStringList(q, after, a => a.buildMultiTargetingFiles);
        sendStringList(q, after, a => a.contentFiles);
        sendStringList(q, after, a => a.runtimeTargets);
        sendStringList(q, after, a => a.resourceAssemblies);
        sendStringList(q, after, a => a.analyzerAssemblies);
        q.getAndSend(after, a => a.hasInstallScripts);
        q.getAndSend(after, a => a.hasXdtTransforms);
        q.getAndSend(after, a => a.hasLegacyContentFolder);
        // Java sends an absent map as two empty lists
        sendStringList(q, after, a => Object.keys(a.dependencyRanges ?? {}));
        sendStringList(q, after, a => Object.values(a.dependencyRanges ?? {}));
    }
});

RpcCodecs.registerCodec(MSBuildProjectReferenceKind, {
    rpcReceive(before: MSBuildProjectReference, q: RpcReceiveQueue): MSBuildProjectReference {
        // Populated in place for the same reason as a resolved package: Java sends project
        // references only by ref, and the project graph may close back onto this instance.
        const pr = castDraft(before);
        pr.include = q.receive(before.include);
        pr.name = q.receive(before.name);
        pr.depth = q.receive(before.depth);
        pr.projectReferences = castDraft(q.receiveListDefined(before.projectReferences));
        pr.resolvedPackages = castDraft(q.receiveListDefined(before.resolvedPackages));
        return before;
    },

    rpcSend(after: MSBuildProjectReference, q: RpcSendQueue): void {
        q.getAndSend(after, a => a.include);
        q.getAndSend(after, a => a.name);
        q.getAndSend(after, a => a.depth);
        q.getAndSendList(after, a => a.projectReferences?.map(p => asRef(p)), p => p.include);
        q.getAndSendList(after, a => a.resolvedPackages?.map(r => asRef(r)), resolvedId);
    }
});

RpcCodecs.registerCodec(MSBuildTargetFrameworkKind, {
    rpcReceive(before: MSBuildTargetFramework, q: RpcReceiveQueue): MSBuildTargetFramework {
        return updateIfChanged(before, {
            targetFramework: q.receive(before.targetFramework),
            packageReferences: q.receiveListDefined(before.packageReferences),
            resolvedPackages: q.receiveListDefined(before.resolvedPackages),
            projectReferences: q.receiveListDefined(before.projectReferences),
        });
    },

    rpcSend(after: MSBuildTargetFramework, q: RpcSendQueue): void {
        q.getAndSend(after, a => a.targetFramework);
        q.getAndSendList(after, a => a.packageReferences, pr => pr.include);
        // Java's getAndSendListAsRef: each element travels once, later occurrences as its ref
        q.getAndSendList(after, a => a.resolvedPackages?.map(r => asRef(r)), resolvedId);
        q.getAndSendList(after, a => a.projectReferences?.map(p => asRef(p)), p => p.include);
    }
});

RpcCodecs.registerCodec(MSBuildProjectKind, {
    rpcReceive(before: MSBuildProject, q: RpcReceiveQueue): MSBuildProject {
        const id = q.receive(before.id);
        const sdk = q.receive(before.sdk);
        const beforeProperties = before.properties ?? {};
        const keys = q.receiveList(Object.keys(beforeProperties), k => q.receive(k));
        const values = q.receiveList(Object.values(beforeProperties));
        return updateIfChanged(before, {
            id,
            sdk,
            properties: zip(keys, values, before.properties),
            packageSources: q.receiveListDefined(before.packageSources),
            targetFrameworks: q.receiveListDefined(before.targetFrameworks),
        });
    },

    rpcSend(after: MSBuildProject, q: RpcSendQueue): void {
        q.getAndSend(after, a => a.id);
        q.getAndSend(after, a => a.sdk);
        // The properties map travels as parallel lists: keys, then values through their codec
        q.getAndSendList(after, a => Object.keys(a.properties ?? {}), k => k, k => q.getAndSend(k, x => x));
        q.getAndSendList(after, a => Object.values(a.properties ?? {}), v => v.value);
        q.getAndSendList(after, a => a.packageSources, ps => ps.key);
        q.getAndSendList(after, a => a.targetFrameworks, tf => tf.targetFramework);
    }
});
