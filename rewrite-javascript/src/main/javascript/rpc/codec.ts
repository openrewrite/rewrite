import {RpcReceiveQueue, RpcSendQueue} from "./queue";

/**
 * Interface representing an RPC codec that defines methods
 * for sending and receiving objects in an RPC communication.
 */
export interface RpcCodec<T> {
    /**
     * Serializes and sends an object over an RPC send queue.
     *
     * @param after - The object to be sent.
     * @param q - The RPC send queue where the object will be enqueued.
     */
    rpcSend(after: T, q: RpcSendQueue): Promise<void>;

    /**
     * Receives and deserializes an object from an RPC receive queue.
     *
     * @param before - The initial object state before deserialization.
     * @param q - The RPC receive queue where the object data is retrieved.
     * @returns A Promise resolving to the deserialized object.
     */
    rpcReceive(before: T, q: RpcReceiveQueue): Promise<T>;
}

/**
 * A registry for managing RPC codecs based on object types.
 */
export class RpcCodecs {
    private static codecs = new Map<string, RpcCodec<any>>();

    /**
     * Registers an RPC codec for a given type.
     *
     * @param type - The string identifier of the object type.
     * @param codec - The codec implementation to be registered.
     */
    static registerCodec(type: string, codec: RpcCodec<any>): void {
        this.codecs.set(type, codec);
    }

    /**
     * Retrieves the registered codec for a given type.
     *
     * @param type - The string identifier of the object type.
     * @returns The corresponding `RpcCodec`, or `undefined` if not found.
     */
    static forType(type: string): RpcCodec<any> | undefined {
        return this.codecs.get(type);
    }

    /**
     * Determines the appropriate codec for an instance based on its `kind` property.
     *
     * @param before - The object instance to find a codec for.
     * @returns The corresponding `RpcCodec`, or `undefined` if no matching codec is found.
     */
    static forInstance(before: any): RpcCodec<any> | undefined {
        if (before !== undefined && typeof before === "object" && "kind" in before) {
            return RpcCodecs.forType(before["kind"] as string);
        }
    }
}
