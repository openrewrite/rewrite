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

export class ExecutionContext {
    readonly kind: string = "org.openrewrite.InMemoryExecutionContext"

    constructor(public readonly messages: { [key: string | symbol]: any } = {}) {
    }
}

/**
 * Messages whose keys start with this prefix are shared with remote RPC peers
 * when an {@link ExecutionContext} is transferred. All other messages are
 * process-local and never cross the wire. Values of shared messages must be
 * limited to strings and lists of strings so that every peer language can
 * consume them without dedicated codecs.
 *
 * Must stay in sync with `ExecutionContext.RPC_SHARED_MESSAGE_PREFIX` in Java.
 */
export const RPC_SHARED_MESSAGE_PREFIX = "org.openrewrite.rpc.shared.";

/**
 * The messages under {@link RPC_SHARED_MESSAGE_PREFIX} as a list of
 * `[key, value]` pairs sorted by key, or undefined when there are none.
 */
function rpcSharedMessages(ctx: ExecutionContext): [string, any][] | undefined {
    const entries = Object.keys(ctx.messages)
        .filter(key => key.startsWith(RPC_SHARED_MESSAGE_PREFIX) && ctx.messages[key] !== undefined)
        .sort()
        .map(key => [key, ctx.messages[key]] as [string, any]);
    return entries.length === 0 ? undefined : entries;
}

const executionContextCodec: RpcCodec<ExecutionContext> = {
    rpcNew(): ExecutionContext {
        return new ExecutionContext();
    },

    async rpcSend(after: ExecutionContext, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, rpcSharedMessages);
    },

    async rpcReceive(before: ExecutionContext, q: RpcReceiveQueue): Promise<ExecutionContext> {
        const after: [string, any][] | undefined = await q.receive(rpcSharedMessages(before));
        const retained = new Set<string>();
        if (after) {
            for (const [key, value] of after) {
                before.messages[key] = value;
                retained.add(key);
            }
        }
        for (const key of Object.keys(before.messages)) {
            if (key.startsWith(RPC_SHARED_MESSAGE_PREFIX) && !retained.has(key)) {
                delete before.messages[key];
            }
        }
        return before;
    }
}

RpcCodecs.registerCodec("org.openrewrite.InMemoryExecutionContext", executionContextCodec);
