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

function receiveSnippet(before: PlainText.Snippet, q: RpcReceiveQueue): PlainText.Snippet | undefined {
    return updateIfChanged(before, {
        id: q.receive(before.id),
        markers: q.receiveMarkers(before.markers),
        text: q.receive(before.text),
    });
}

// Register codec for all PlainText AST node types
for (const kind of Object.values(PlainText.Kind)) {
    RpcCodecs.registerCodec(kind as string, {
        rpcReceive(before: PlainText, q: RpcReceiveQueue): PlainText {
            return updateIfChanged(before, {
                id: q.receive(before.id),
                markers: q.receiveMarkers(before.markers),
                sourcePath: q.receive(before.sourcePath),
                charsetName: q.receive(before.charsetName),
                charsetBomMarked: q.receive(before.charsetBomMarked),
                checksum: q.receive(before.checksum),
                fileAttributes: q.receive(before.fileAttributes),
                text: q.receive(before.text),
                snippets: q.receiveListDefined(before.snippets, snippet => receiveSnippet(snippet, q)),
            });
        },

        rpcSend(after: PlainText, q: RpcSendQueue): void {
            q.getAndSend(after, p => p.id);
            q.getAndSend(after, p => p.markers);
            q.getAndSend(after, p => p.sourcePath);
            q.getAndSend(after, p => p.charsetName);
            q.getAndSend(after, p => p.charsetBomMarked);
            q.getAndSend(after, p => p.checksum);
            q.getAndSend(after, p => p.fileAttributes);
            q.getAndSend(after, p => p.text);
            q.getAndSendList(after, a => a.snippets, s => s.id, async (snippet) => {
                q.getAndSend(snippet, p => p.id);
                q.getAndSend(snippet, p => p.markers);
                q.getAndSend(snippet, p => p.text);
            });
        }
    }, PlainText.Kind.PlainText);
}
