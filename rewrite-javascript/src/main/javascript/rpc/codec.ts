import {RpcReceiveQueue, RpcSendQueue} from "./queue";

export interface RpcCodec<T> {
    rpcSend(after: T, q: RpcSendQueue): void;

    rpcReceive(before: T, q: RpcReceiveQueue): Promise<T>;
}

export class RpcCodecs {
    private static codecs = new Map<string, RpcCodec<any>>();

    static registerCodec(type: string, codec: RpcCodec<any>): void {
        this.codecs.set(type, codec);
    }

    static getCodec(type: string): RpcCodec<any> | undefined {
        return this.codecs.get(type);
    }

    static getCodecForInstance(before: any): RpcCodec<any> | undefined {
        if ("kind" in Object.keys(before)) {
            return RpcCodecs.getCodec(before["kind"] as string)
        }
    }
}
