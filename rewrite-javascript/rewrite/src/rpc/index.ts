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
// rewrite-rpc is not exported here to avoid circular dependency
// Import directly from "./rewrite-rpc" if needed

RpcCodecs.registerCodec(TreeKind.Checksum, {
    async rpcReceive(before: Checksum, q: RpcReceiveQueue): Promise<Checksum> {
        return updateIfChanged(before, {
            algorithm: await q.receive(before.algorithm),
            value: await q.receive(before.value),
        });
    },

    async rpcSend(after: Checksum, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, c => c.algorithm);
        await q.getAndSend(after, c => c.value);
    }
});

RpcCodecs.registerCodec(TreeKind.FileAttributes, {
    async rpcReceive(before: FileAttributes, q: RpcReceiveQueue): Promise<FileAttributes> {
        return updateIfChanged(before, {
            creationDate: await q.receive(before.creationDate),
            lastModifiedTime: await q.receive(before.lastModifiedTime),
            lastAccessTime: await q.receive(before.lastAccessTime),
            isReadable: await q.receive(before.isReadable),
            isWritable: await q.receive(before.isWritable),
            isExecutable: await q.receive(before.isExecutable),
            size: await q.receive(before.size),
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
    async rpcReceive(before: Markers, q: RpcReceiveQueue): Promise<Markers> {
        return updateIfChanged(before, {
            id: await q.receive(before.id),
            markers: (await q.receiveList(before.markers))!,
        });
    },

    async rpcSend(after: Markers, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, m => m.id);
        await q.getAndSendList(after, m => m.markers.map(marker => asRef(marker)), m => m.id);
    }
});

// Register codecs for all Java markers with additional properties
RpcCodecs.registerCodec(MarkersKind.SearchResult, {
    async rpcReceive(before: SearchResult, q: RpcReceiveQueue): Promise<SearchResult> {
        return updateIfChanged(before, {
            id: await q.receive(before.id),
            description: await q.receive(before.description),
        });
    },

    async rpcSend(after: SearchResult, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.description);
    }
});

RpcCodecs.registerCodec(MarkersKind.MarkupError, {
    async rpcReceive(before: MarkupError, q: RpcReceiveQueue): Promise<MarkupError> {
        return updateIfChanged(before, {
            id: await q.receive(before.id),
            message: await q.receive(before.message),
            detail: await q.receive(before.detail),
        });
    },

    async rpcSend(after: MarkupError, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.message);
        await q.getAndSend(after, a => a.detail);
    }
});

RpcCodecs.registerCodec(MarkersKind.MarkupWarn, {
    async rpcReceive(before: MarkupWarn, q: RpcReceiveQueue): Promise<MarkupWarn> {
        return updateIfChanged(before, {
            id: await q.receive(before.id),
            message: await q.receive(before.message),
            detail: await q.receive(before.detail),
        });
    },

    async rpcSend(after: MarkupWarn, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.message);
        await q.getAndSend(after, a => a.detail);
    }
});

RpcCodecs.registerCodec(MarkersKind.MarkupInfo, {
    async rpcReceive(before: MarkupInfo, q: RpcReceiveQueue): Promise<MarkupInfo> {
        return updateIfChanged(before, {
            id: await q.receive(before.id),
            message: await q.receive(before.message),
            detail: await q.receive(before.detail),
        });
    },

    async rpcSend(after: MarkupInfo, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.message);
        await q.getAndSend(after, a => a.detail);
    }
});

RpcCodecs.registerCodec(MarkersKind.MarkupDebug, {
    async rpcReceive(before: MarkupDebug, q: RpcReceiveQueue): Promise<MarkupDebug> {
        return updateIfChanged(before, {
            id: await q.receive(before.id),
            message: await q.receive(before.message),
            detail: await q.receive(before.detail),
        });
    },

    async rpcSend(after: MarkupDebug, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.message);
        await q.getAndSend(after, a => a.detail);
    }
});
