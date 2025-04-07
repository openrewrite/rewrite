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
