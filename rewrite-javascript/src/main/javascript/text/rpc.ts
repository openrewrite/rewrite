import {RpcCodec, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {PlainText, PlainTextKind, Snippet} from "./tree";
import {createDraft, Draft, finishDraft} from "immer";

async function receiveSnippet(before: Snippet, q: RpcReceiveQueue): Promise<Snippet | undefined> {
    const draft: Draft<Snippet> = createDraft(before);
    draft.id = await q.receive(before.id);
    draft.markers = await q.receiveMarkers(before);
    draft.text = await q.receive(before.text);
    return finishDraft(draft);
}

const textCodec: RpcCodec<PlainText> = {
    async rpcReceive(before: PlainText, q: RpcReceiveQueue): Promise<PlainText> {
        const draft: Draft<PlainText> = createDraft(before);
        draft.snippets = (await q.receiveList(before.snippets, snippet => receiveSnippet(snippet, q)))!;
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
    }
}

Object.values(PlainTextKind).forEach(kind => {
    RpcCodecs.registerCodec(kind, textCodec);
});
