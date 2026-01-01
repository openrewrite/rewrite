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
import {RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {PlainText} from "./tree";
import {updateIfChanged} from "../util";

async function receiveSnippet(before: PlainText.Snippet, q: RpcReceiveQueue): Promise<PlainText.Snippet | undefined> {
    return updateIfChanged(before, {
        id: await q.receive(before.id),
        markers: await q.receive(before.markers),
        text: await q.receive(before.text),
    });
}

// Register codec for all PlainText AST node types
for (const kind of Object.values(PlainText.Kind)) {
    RpcCodecs.registerCodec(kind as string, {
        async rpcReceive(before: PlainText, q: RpcReceiveQueue): Promise<PlainText> {
            return updateIfChanged(before, {
                id: await q.receive(before.id),
                markers: await q.receive(before.markers),
                sourcePath: await q.receive(before.sourcePath),
                charsetName: await q.receive(before.charsetName),
                charsetBomMarked: await q.receive(before.charsetBomMarked),
                checksum: await q.receive(before.checksum),
                fileAttributes: await q.receive(before.fileAttributes),
                text: await q.receive(before.text),
                snippets: (await q.receiveList(before.snippets, snippet => receiveSnippet(snippet, q)))!,
            });
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
    }, PlainText.Kind.PlainText);
}
