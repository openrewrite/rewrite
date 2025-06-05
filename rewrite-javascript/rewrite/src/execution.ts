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
import {RpcCodec, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "./rpc";
import {ParseError, ParseErrorKind} from "./parse-error";
import {createDraft, Draft, finishDraft} from "immer";

export class ExecutionContext {
    readonly kind: string = "org.openrewrite.ExecutionContext"

    constructor(public readonly messages: { [key: string | symbol]: any } = {}) {
    }
}

const executionContextCodec: RpcCodec<ExecutionContext> = {
    async rpcSend(_after: ExecutionContext, _q: RpcSendQueue): Promise<void> {
    },

    async rpcReceive(_before: ExecutionContext, _q: RpcReceiveQueue): Promise<ExecutionContext> {
        return new ExecutionContext();
    }
}

RpcCodecs.registerCodec("org.openrewrite.ExecutionContext", executionContextCodec);

// FIXME moved here from `parse-error.ts` due to a suspected cyclic dependency issue
RpcCodecs.registerCodec(ParseErrorKind, {
    async rpcReceive(before: ParseError, q: RpcReceiveQueue): Promise<ParseError> {
        const draft: Draft<ParseError> = createDraft(before);
        draft.id = await q.receive(before.id);
        draft.markers = await q.receiveMarkers(before.markers);
        draft.sourcePath = await q.receive(before.sourcePath);
        draft.charsetName = await q.receive(before.charsetName);
        draft.charsetBomMarked = await q.receive(before.charsetBomMarked);
        draft.checksum = await q.receive(before.checksum);
        draft.fileAttributes = await q.receive(before.fileAttributes);
        draft.text = await q.receive(before.text);
        return finishDraft(draft);
    },

    async rpcSend(after: ParseError, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, p => p.id);
        await q.sendMarkers(after, p => p.markers);
        await q.getAndSend(after, p => p.sourcePath);
        await q.getAndSend(after, p => p.charsetName);
        await q.getAndSend(after, p => p.charsetBomMarked);
        await q.getAndSend(after, p => p.checksum);
        await q.getAndSend(after, p => p.fileAttributes);
        await q.getAndSend(after, p => p.text);
    }
});
