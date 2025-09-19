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
import {RpcCodec, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {PlainText} from "./tree";
import {createDraft, Draft, finishDraft} from "immer";
import {TreeKind} from "../tree";

async function receiveSnippet(before: PlainText.Snippet, q: RpcReceiveQueue): Promise<PlainText.Snippet | undefined> {
    const draft: Draft<PlainText.Snippet> = createDraft(before);
    draft.id = await q.receive(before.id);
    draft.markers = await q.receive(before.markers);
    draft.text = await q.receive(before.text);
    return finishDraft(draft);
}

const textCodec: RpcCodec<PlainText> = {
    async rpcReceive(before: PlainText, q: RpcReceiveQueue): Promise<PlainText> {
        const draft: Draft<PlainText> = createDraft(before);
        draft.id = await q.receive(before.id);
        draft.markers = await q.receive(before.markers);
        draft.sourcePath = await q.receive(before.sourcePath);
        draft.charsetName = await q.receive(before.charsetName);
        draft.charsetBomMarked = await q.receive(before.charsetBomMarked);
        draft.checksum = await q.receive(before.checksum);
        draft.fileAttributes = await q.receive(before.fileAttributes);
        draft.text = await q.receive(before.text);
        draft.snippets = (await q.receiveList(before.snippets, snippet => receiveSnippet(snippet, q)))!;
        return finishDraft(draft);
    },

    async rpcSend(after: PlainText, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, p => p.id);
        await q.getAndSend(after, p => p.markers);
        await q.getAndSend(after, p => p.sourcePath);
        await q.getAndSend(after, p => p.charsetName);
        await q.getAndSend(after, p => p.charsetBomMarked);
        await q.getAndSend(after, p => p.checksum);
        await q.getAndSend(after, p => p.fileAttributes);
        await q.getAndSend(after, p => p.text);
        await q.getAndSendList(after, a => a.snippets, s => s.id, async (snippet) => {
            await q.getAndSend(snippet, p => p.id);
            await q.getAndSend(snippet, p => p.markers);
            await q.getAndSend(snippet, p => p.text);
        });
    }
}

Object.values(PlainText.Kind).forEach(kind => {
    if (!Object.values(TreeKind).includes(kind as any)) {
        RpcCodecs.registerCodec(kind, textCodec);
    }
});
