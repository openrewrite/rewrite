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
