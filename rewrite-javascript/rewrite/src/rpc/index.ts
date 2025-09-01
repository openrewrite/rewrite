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
import {createDraft, finishDraft} from "immer";
import {Markers, MarkersKind, SearchResult} from "../markers";

export * from "./queue"
export * from "../reference"
export * from "./rewrite-rpc"

RpcCodecs.registerCodec(TreeKind.Checksum, {
    async rpcReceive(before: Checksum, q: RpcReceiveQueue): Promise<Checksum> {
        const draft = createDraft(before);
        draft.algorithm = await q.receive(before.algorithm);
        draft.value = await q.receive(before.value);
        return finishDraft(draft) as Checksum;
    },

    async rpcSend(after: Checksum, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, c => c.algorithm);
        await q.getAndSend(after, c => c.value);
    }
});

RpcCodecs.registerCodec(TreeKind.FileAttributes, {
    async rpcReceive(before: FileAttributes, q: RpcReceiveQueue): Promise<FileAttributes> {
        const draft = createDraft(before);
        draft.creationDate = await q.receive(before.creationDate);
        draft.lastModifiedTime = await q.receive(before.lastModifiedTime);
        draft.lastAccessTime = await q.receive(before.lastAccessTime);
        draft.isReadable = await q.receive(before.isReadable);
        draft.isWritable = await q.receive(before.isWritable);
        draft.isExecutable = await q.receive(before.isExecutable);
        draft.size = await q.receive(before.size);
        return finishDraft(draft) as FileAttributes;
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
    async rpcReceive(before: Markers, q: RpcReceiveQueue): Promise<any> {
        const draft = createDraft(before);
        draft.id = await q.receive(before.id);
        draft.markers = (await q.receiveList(before.markers))!;
        return finishDraft(draft);
    },

    async rpcSend(after: Markers, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, m => m.id);
        await q.getAndSendList(after, m => m.markers, m => m.id);
    }
});

// Register codecs for all Java markers with additional properties
RpcCodecs.registerCodec(MarkersKind.SearchResult, {
    async rpcReceive(before: SearchResult, q: RpcReceiveQueue): Promise<SearchResult> {
        const draft = createDraft(before);
        draft.id = await q.receive(before.id);
        draft.description = await q.receive(before.description)
        return finishDraft(draft);
    },

    async rpcSend(after: SearchResult, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.description);
    }
});
