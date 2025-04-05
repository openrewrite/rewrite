import {RpcCodec, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {PlainText, PlainTextKind, Snippet} from "./tree";
import {createDraft, Draft, finishDraft} from "immer";

async function receiveSnippet(before: Snippet, q: RpcReceiveQueue): Promise<Snippet | undefined> {
    const draft: Draft<Snippet> = createDraft(before);
    draft.id = await q.receive(before.id);
    draft.markers = await q.receiveMarkers(before.markers);
    draft.text = await q.receive(before.text);
    return finishDraft(draft);
}

const textCodec: RpcCodec<PlainText> = {
    async rpcReceive(before: PlainText, q: RpcReceiveQueue): Promise<PlainText> {
        const draft: Draft<PlainText> = createDraft(before);
        draft.id = await q.receive(before.id);
        draft.markers = await q.receiveMarkers(before.markers);
        draft.sourcePath = await q.receive(before.sourcePath);
        draft.charsetName = await q.receive(before.charsetName);
        draft.charsetBomMarked = await q.receive(before.charsetBomMarked);
        draft.checksum = await q.receive(before.checksum);
        draft.fileAttributes = await q.receive(before.fileAttributes);
        draft.snippets = (await q.receiveList(before.snippets, snippet => receiveSnippet(snippet, q)))!;
        draft.text = await q.receive(before.text);
        return finishDraft(draft);
    },

    async rpcSend(after: PlainText, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, p => p.id);
        await q.sendMarkers(after, p => p.markers);
        await q.getAndSend(after, p => p.sourcePath);
        await q.getAndSend(after, p => p.charsetName);
        await q.getAndSend(after, p => p.charsetBomMarked);
        await q.getAndSend(after, p => p.checksum);
        await q.getAndSend(after, p => p.fileAttributes);
        await q.getAndSendList(after, a => a.snippets, s => s.id, async (snippet) => {
            await q.getAndSend(snippet, p => p.id);
            await q.sendMarkers(after, p => p.markers);
            await q.getAndSend(snippet, p => p.text);
        });
        await q.getAndSend(after, p => p.text);
    }
}

Object.values(PlainTextKind).forEach(kind => {
    RpcCodecs.registerCodec(kind, textCodec);
});
