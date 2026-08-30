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
    /** Mirrors `org.openrewrite.ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT`. */
    static readonly REQUIRE_PRINT_EQUALS_INPUT = "org.openrewrite.requirePrintEqualsInput";

    readonly kind: string = "org.openrewrite.InMemoryExecutionContext"

    constructor(public readonly messages: { [key: string | symbol]: any } = {}) {
    }

    /**
     * Option maps are loosely typed across peers, so a value that arrived as a string counts the same as
     * its boolean form. The spellings accepted are Go's `strconv.ParseBool`, which peers parse with;
     * anything else falls back to `defaultValue`.
     */
    getBoolean(key: string, defaultValue: boolean): boolean {
        const value = this.messages[key];
        if (typeof value === "boolean") {
            return value;
        }
        switch (value) {
            case "1": case "t": case "T": case "true": case "TRUE": case "True":
                return true;
            case "0": case "f": case "F": case "false": case "FALSE": case "False":
                return false;
            default:
                return defaultValue;
        }
    }
}

const executionContextCodec: RpcCodec<ExecutionContext> = {
    rpcNew(): ExecutionContext {
        return new ExecutionContext();
    },

    async rpcSend(_after: ExecutionContext, _q: RpcSendQueue): Promise<void> {
    },

    async rpcReceive(_before: ExecutionContext, _q: RpcReceiveQueue): Promise<ExecutionContext> {
        return new ExecutionContext();
    }
}

RpcCodecs.registerCodec("org.openrewrite.InMemoryExecutionContext", executionContextCodec);
