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
import {Checksum, FileAttributes, TreeKind} from "../tree";
import {RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "./queue";
import {Markers, MarkersKind, MarkupDebug, MarkupError, MarkupInfo, MarkupWarn, SearchResult} from "../markers";
import {asRef} from "../reference";
import {updateIfChanged} from "../util";

export * from "./queue";
export * from "../reference";
export {RewriteRpc} from "./rewrite-rpc";

RpcCodecs.registerCodec(TreeKind.Checksum, {
    rpcReceive(before: Checksum, q: RpcReceiveQueue): Checksum {
        return updateIfChanged(before, {
            algorithm: q.receive(before.algorithm),
            value: q.receive(before.value),
        });
    },

    async rpcSend(after: Checksum, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, c => c.algorithm);
        await q.getAndSend(after, c => c.value);
    }
});

RpcCodecs.registerCodec(TreeKind.FileAttributes, {
    rpcReceive(before: FileAttributes, q: RpcReceiveQueue): FileAttributes {
        return updateIfChanged(before, {
            creationDate: q.receive(before.creationDate),
            lastModifiedTime: q.receive(before.lastModifiedTime),
            lastAccessTime: q.receive(before.lastAccessTime),
            isReadable: q.receive(before.isReadable),
            isWritable: q.receive(before.isWritable),
            isExecutable: q.receive(before.isExecutable),
            size: q.receive(before.size),
        });
    },

    async rpcSend(after: FileAttributes, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.creationDate);
        await q.getAndSend(after, a => a.lastModifiedTime);
        await q.getAndSend(after, a => a.lastAccessTime);
        await q.getAndSend(after, a => a.isReadable);
        await q.getAndSend(after, a => a.isWritable);
        await q.getAndSend(after, a => a.isExecutable);
        await q.getAndSend(after, a => a.size);
    }
});

RpcCodecs.registerCodec(MarkersKind.Markers, {
    rpcReceive(before: Markers, q: RpcReceiveQueue): Markers {
        // inlined `updateIfChanged()` for performance
        const id = q.receive(before.id)!;
        const markers = q.receiveList(before.markers)!;
        return id === before.id && markers === before.markers
            ? before
            : { ...before, id, markers };
    },

    async rpcSend(after: Markers, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, m => m.id);
        await q.getAndSendList(after, m => m.markers.map(marker => asRef(marker)), m => m.id);
    }
});

// Register codecs for all Java markers with additional properties
RpcCodecs.registerCodec(MarkersKind.SearchResult, {
    rpcReceive(before: SearchResult, q: RpcReceiveQueue): SearchResult {
        return updateIfChanged(before, {
            id: q.receive(before.id),
            description: q.receive(before.description),
        });
    },

    async rpcSend(after: SearchResult, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.description);
    }
});

RpcCodecs.registerCodec(MarkersKind.MarkupError, {
    rpcReceive(before: MarkupError, q: RpcReceiveQueue): MarkupError {
        return updateIfChanged(before, {
            id: q.receive(before.id),
            message: q.receive(before.message),
            detail: q.receive(before.detail),
        });
    },

    async rpcSend(after: MarkupError, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.message);
        await q.getAndSend(after, a => a.detail);
    }
});

RpcCodecs.registerCodec(MarkersKind.MarkupWarn, {
    rpcReceive(before: MarkupWarn, q: RpcReceiveQueue): MarkupWarn {
        return updateIfChanged(before, {
            id: q.receive(before.id),
            message: q.receive(before.message),
            detail: q.receive(before.detail),
        });
    },

    async rpcSend(after: MarkupWarn, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.message);
        await q.getAndSend(after, a => a.detail);
    }
});

RpcCodecs.registerCodec(MarkersKind.MarkupInfo, {
    rpcReceive(before: MarkupInfo, q: RpcReceiveQueue): MarkupInfo {
        return updateIfChanged(before, {
            id: q.receive(before.id),
            message: q.receive(before.message),
            detail: q.receive(before.detail),
        });
    },

    async rpcSend(after: MarkupInfo, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.message);
        await q.getAndSend(after, a => a.detail);
    }
});

RpcCodecs.registerCodec(MarkersKind.MarkupDebug, {
    rpcReceive(before: MarkupDebug, q: RpcReceiveQueue): MarkupDebug {
        return updateIfChanged(before, {
            id: q.receive(before.id),
            message: q.receive(before.message),
            detail: q.receive(before.detail),
        });
    },

    async rpcSend(after: MarkupDebug, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.message);
        await q.getAndSend(after, a => a.detail);
    }
});
