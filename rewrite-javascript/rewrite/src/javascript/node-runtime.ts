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
import {findMarker, Marker, Markers} from "../markers";
import {randomId, UUID} from "../uuid";
import {RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {updateIfChanged} from "../util";
import {PackageManager} from "./node-resolution-result";
import * as fs from "fs";

export const NodeRuntimeKind = "org.openrewrite.javascript.marker.NodeRuntime" as const;

/**
 * The machine and the package manager release that built a Node.js project's LST, as `JavaVersion` records the JDK
 * that built a Java one. The lock regeneration installs the project as that release, on that Node and platform.
 *
 * The {@link PackageJsonParser} attaches one to every package.json document it produces, whether or not the document
 * gets a {@link NodeResolutionResult}: one instance per project root, which its workspace members share.
 */
export interface NodeRuntime extends Marker {
    readonly kind: typeof NodeRuntimeKind;
    readonly id: UUID;

    /**
     * `process.version` of the parser, such as `v24.19.0`.
     */
    readonly nodeVersion: string;

    /**
     * `process.platform` of the parser, such as `linux` or `darwin`.
     */
    readonly os: string;

    /**
     * `process.arch` of the parser, such as `x64` or `arm64`.
     */
    readonly cpu: string;

    /**
     * `glibc` or `musl` on Linux, as npm-install-checks 8.0.0 detects it ({@link libc}); absent on other platforms and
     * where it cannot be told.
     */
    readonly libc?: string;

    /**
     * The package manager the parser found for the project by its lock file, as {@link NodeResolutionResult} names
     * it; absent when it found none.
     */
    readonly packageManager?: PackageManager;

    /**
     * What that package manager's `--version` answers when run in the project's root directory, the release that
     * installs the project on the parser's machine ({@link runVersion}); absent when it is not installed, fails, or
     * answers something that is no version.
     */
    readonly packageManagerVersion?: string;
}

/**
 * A {@link NodeRuntime} of the running process, for a project the parser found `packageManager` for.
 *
 * @param packageManager the package manager the parser found by the project's lock file, if any
 * @param packageManagerVersion what its `--version` answered in the project's root directory, if anything
 */
export function createNodeRuntimeMarker(packageManager?: PackageManager, packageManagerVersion?: string): NodeRuntime {
    return {
        kind: NodeRuntimeKind,
        id: randomId(),
        nodeVersion: process.version,
        os: process.platform,
        cpu: process.arch,
        libc: libc(process.platform) ?? undefined,
        packageManager,
        packageManagerVersion
    };
}

/**
 * The {@link NodeRuntime} of a package.json document, if the parser attached one.
 */
export function findNodeRuntime(tree: { markers: Markers }): NodeRuntime | undefined {
    return findMarker<NodeRuntime>(tree, NodeRuntimeKind);
}

/*
 * Port of npm-install-checks 8.0.0 lib/current-env.js libc(), distributed under the BSD 2-Clause License:
 *
 * Copyright (c) Robert Kowalski and Isaac Z. Schlueter ("Authors")
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHORS OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

function isMusl(file: string): boolean {
    return file.includes('libc.musl-') || file.includes('ld-musl-');
}

const LDD_PATH = '/usr/bin/ldd';

function getFamilyFromFilesystem(): string | null | undefined {
    try {
        const content = fs.readFileSync(LDD_PATH, 'utf-8');
        if (content.includes('musl')) {
            return 'musl';
        }
        if (content.includes('GNU C Library')) {
            return 'glibc';
        }
        return null;
    } catch {
        return undefined;
    }
}

function getFamilyFromReport(): string | null {
    // excludeNetwork is newer than the type declarations the module builds against
    const processReport = process.report as NodeJS.ProcessReport & { excludeNetwork?: boolean };
    const originalExclude = processReport.excludeNetwork;
    processReport.excludeNetwork = true;
    const report = processReport.getReport() as {
        header?: { glibcVersionRuntime?: string };
        sharedObjects?: unknown;
    };
    processReport.excludeNetwork = originalExclude;
    if (report.header?.glibcVersionRuntime) {
        family = 'glibc';
    } else if (Array.isArray(report.sharedObjects) && report.sharedObjects.some(isMusl)) {
        family = 'musl';
    } else {
        family = null;
    }
    return family;
}

let family: string | null | undefined;

/**
 * The C library family of a machine running `osName`, as npm-install-checks 8.0.0 detects it: `glibc` or `musl` on
 * Linux, from `/usr/bin/ldd` or else the process report, and null there when it cannot be told; undefined on other
 * platforms. The answer is detected once per process.
 */
export function libc(osName: string): string | null | undefined {
    if (osName !== 'linux') {
        return undefined;
    }
    if (family === undefined) {
        family = getFamilyFromFilesystem();
        if (family === undefined) {
            family = getFamilyFromReport();
        }
    }
    return family;
}

/**
 * Register RPC codec for NodeRuntime, in the order the Java marker sends and receives its fields.
 */
RpcCodecs.registerCodec(NodeRuntimeKind, {
    rpcReceive(before: NodeRuntime, q: RpcReceiveQueue): NodeRuntime {
        return updateIfChanged(before, {
            id: q.receive(before.id),
            nodeVersion: q.receive(before.nodeVersion),
            os: q.receive(before.os),
            cpu: q.receive(before.cpu),
            libc: q.receive(before.libc),
            packageManager: q.receive(before.packageManager),
            packageManagerVersion: q.receive(before.packageManagerVersion),
        });
    },

    rpcSend(after: NodeRuntime, q: RpcSendQueue): void {
        q.getAndSend(after, a => a.id);
        q.getAndSend(after, a => a.nodeVersion);
        q.getAndSend(after, a => a.os);
        q.getAndSend(after, a => a.cpu);
        q.getAndSend(after, a => a.libc);
        q.getAndSend(after, a => a.packageManager);
        q.getAndSend(after, a => a.packageManagerVersion);
    }
});
