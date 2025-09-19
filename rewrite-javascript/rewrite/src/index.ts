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
import {RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "./rpc";
import {createDraft, Draft, finishDraft} from "immer";
import {MarkersKind, ParseExceptionResult} from "./markers";
import {RecipeRegistry} from "./recipe";

export * from "./data-table";
export * from "./execution";
export * from "./markers";
export * from "./style";
export * from "./print";
export * from "./tree";
export * from "./visitor";
export * from "./parser";
export * from "./parse-error";
export * from "./preconditions";
export * from "./uuid";
export * from "./util";
export * from "./recipe";
export * from "./run";

// register all recipes in this package
export async function activate(registry: RecipeRegistry): Promise<void> {
    const {OrderImports} = await import("./recipe/order-imports.js");
    registry.register(OrderImports);
}

RpcCodecs.registerCodec(MarkersKind.ParseExceptionResult, {
    async rpcSend(after: ParseExceptionResult, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.parserType);
        await q.getAndSend(after, a => a.exceptionType);
        await q.getAndSend(after, a => a.message);
        await q.getAndSend(after, a => a.treeType);
    },
    async rpcReceive(before: ParseExceptionResult, q: RpcReceiveQueue): Promise<ParseExceptionResult> {
        const draft: Draft<ParseExceptionResult> = createDraft(before);
        draft.id = await q.receive(before.id);
        draft.parserType = await q.receive(before.parserType);
        draft.exceptionType = await q.receive(before.exceptionType);
        draft.message = await q.receive(before.message);
        draft.treeType = await q.receive(before.treeType);
        return finishDraft(draft);
    }
});
