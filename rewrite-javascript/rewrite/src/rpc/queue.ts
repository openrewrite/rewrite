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
import * as rpc from "vscode-jsonrpc/node";
import {emptyMarkers, Markers} from "../markers";
import {saveTrace, trace} from "./trace";
import {updateIfChanged} from "../util";
import {isRef, ReferenceMap} from "../reference";

/**
 * Interface representing an RPC codec that defines methods
 * for sending and receiving objects in an RPC communication.
 */
export interface RpcCodec<T> {
    /**
     * Creates a new instance of the object type with proper constructor defaults.
     * If not provided, a plain object `{kind: type}` will be created.
     *
     * @returns A new instance of the object type.
     */
    rpcNew?(): T;

    /**
     * Serializes and sends an object over an RPC send queue.
     *
     * @param after - The object to be sent.
     * @param q - The RPC send queue where the object will be enqueued.
     */
    rpcSend(after: T, q: RpcSendQueue): Promise<void>;

    /**
     * Receives and deserializes an object from a sync receive queue.
     *
     * @param before - The initial object state before deserialization.
     * @param q - The sync receive queue where the object data is retrieved.
     * @returns The deserialized object.
     */
    rpcReceive(before: T, q: RpcReceiveQueue): T;
}

/**
 * A registry for managing RPC codecs based on object types.
 */
export class RpcCodecs {
    private static nonTreeCodecs = new Map<string, RpcCodec<any>>();

    /**
     * The first key is on sourceFileType and the second on object type
     */
    private static treeCodecs = new Map<string, Map<string, RpcCodec<any>>>();

    /**
     * Registers an RPC codec for a given type.
     *
     * @param type - The string identifier of the object type.
     * @param codec - The codec implementation to be registered.
     * @param sourceFileType The source file type of the source file containing (or will contain) this element.
     */
    static registerCodec(type: string, codec: RpcCodec<any>, sourceFileType?: string): void {
        if (sourceFileType) {
            let codecsForSourceFile = this.treeCodecs.get(sourceFileType);
            if (!codecsForSourceFile) {
                codecsForSourceFile = new Map<string, RpcCodec<any>>();
                this.treeCodecs.set(sourceFileType, codecsForSourceFile);
            }
            codecsForSourceFile.set(type, codec);
        } else {
            this.nonTreeCodecs.set(type, codec);
        }
    }

    /**
     * Retrieves the registered codec for a given type.
     *
     * @param type - The string identifier of the object type.
     * @param sourceFileType The source file type of the source file containing (or will contain) this element.
     * @returns The corresponding `RpcCodec`, or `undefined` if not found.
     */
    static forType(type: string, sourceFileType?: string): RpcCodec<any> | undefined {
        if (sourceFileType) {
            const treeCodec = this.treeCodecs.get(sourceFileType)?.get(type);
            return treeCodec || this.nonTreeCodecs.get(type);
        }
        return this.nonTreeCodecs.get(type);
    }

    /**
     * Determines the appropriate codec for an instance based on its `kind` property.
     *
     * @param before - The object instance to find a codec for.
     * @param sourceFileType The source file type of the source file containing (or will contain) this element.
     * @returns The corresponding `RpcCodec`, or `undefined` if no matching codec is found.
     */
    static forInstance(before: any, sourceFileType?: string): RpcCodec<any> | undefined {
        if (before !== undefined && before !== null && typeof before === "object" && "kind" in before) {
            return RpcCodecs.forType(before["kind"] as string, sourceFileType);
        }
    }
}

export class RpcSendQueue {
    private q: RpcRawMessage[] = [];

    private before?: any;

    constructor(private readonly refs: ReferenceMap,
                private readonly sourceFileType: string | undefined,
                private readonly tracing: boolean) {
    }

    async generate(after: any, before: any): Promise<RpcRawMessage[]> {
        await this.send(after, before);

        const result = this.q;
        result.push(rpcMsg(RpcObjectState.END_OF_OBJECT));

        this.q = [];
        return result;
    }

    private put(msg: RpcRawMessage): void {
        this.q.push(msg);
    }

    private trace(): string | null {
        return this.tracing ? trace("Sender") ?? null : null;
    }

    getAndSend<T, U>(parent: T,
                     value: (parent: T) => U | undefined,
                     onChange?: (value: U) => Promise<any>): Promise<void> {
        const after = value(parent);
        const before = this.before === undefined ? undefined : value(this.before as T);
        return this.send(after, before, onChange && (() => onChange(after!)));
    }

    getAndSendList<T, U>(parent: T,
                         values: (parent: T) => U[] | undefined,
                         id: (value: U) => any,
                         onChange?: (value: U) => Promise<any>): Promise<void> {
        const after = values(parent);
        const before = this.before === undefined ? undefined : values(this.before as T);
        return this.sendList(after, before, id, onChange);
    }

    send<T>(after: T | undefined, before: T | undefined, onChange?: (() => Promise<any>)): Promise<void> {
        return saveTrace(this.tracing, async () => {
            if (before === after) {
                this.put(rpcMsg(RpcObjectState.NO_CHANGE, null, null, null, this.trace()));
            } else if (before === undefined || (after !== undefined && this.typesAreDifferent(after, before))) {
                // Treat as ADD when before is undefined OR types differ (it's a new object, not a change)
                await this.add(after, onChange);
            } else if (after === undefined) {
                this.put(rpcMsg(RpcObjectState.DELETE, null, null, null, this.trace()));
            } else {
                let afterCodec = onChange ? undefined : RpcCodecs.forInstance(after, this.sourceFileType);
                this.put(rpcMsg(RpcObjectState.CHANGE, null, onChange || afterCodec ? null : after, null, this.trace()));
                await this.doChange(after, before, onChange, afterCodec);
            }
        });
    }

    sendList<T>(after: T[] | undefined,
                before: T[] | undefined,
                id: (value: T) => any,
                onChange?: (value: T) => Promise<any>): Promise<void> {
        return this.send(after, before, async () => {
            if (!after) {
                throw new Error("A DELETE event should have been sent.");
            }

            const beforeIdx = this.putListPositions(after, before, id);

            for (const anAfter of after) {
                const beforePos = beforeIdx.get(id(anAfter));
                const onChangeRun = onChange ? () => onChange(anAfter) : undefined;
                if (!beforePos) {
                    await this.add(anAfter, onChangeRun);
                } else {
                    const aBefore = before?.[beforePos];
                    if (aBefore === anAfter) {
                        this.put(rpcMsg(RpcObjectState.NO_CHANGE, null, null, null, this.trace()));
                    } else if (anAfter !== undefined && this.typesAreDifferent(anAfter, aBefore)) {
                        // Type changed - treat as ADD
                        await this.add(anAfter, onChangeRun);
                    } else {
                        this.put(rpcMsg(RpcObjectState.CHANGE, null, null, null, this.trace()));
                        await this.doChange(anAfter, aBefore, onChangeRun, RpcCodecs.forInstance(anAfter, this.sourceFileType));
                    }
                }
            }
        });
    }

    private putListPositions<T>(after: T[],
                                before: T[] | undefined,
                                id: (value: T) => any): Map<any, number> {
        const beforeIdx = new Map<any, number>();
        if (before) {
            for (let i = 0; i < before.length; i++) {
                beforeIdx.set(id(before[i]), i);
            }
        }
        const positions: number[] = [];
        for (const t of after) {
            const beforePos = beforeIdx.get(id(t));
            positions.push(beforePos === undefined ? -1 : beforePos);
        }
        this.put(rpcMsg(RpcObjectState.CHANGE, null, positions, null, this.trace()));
        return beforeIdx;
    }

    private async add(after: any, onChange: (() => Promise<any>) | undefined): Promise<void> {
        let ref: number | undefined;
        if (isRef(after)) {
            ref = this.refs.get(after);
            if (ref) {
                this.put(rpcMsg(RpcObjectState.ADD, null, null, ref, this.trace()));
                return;
            }
            ref = this.refs.create(after);
        }
        let afterCodec = onChange ? undefined : RpcCodecs.forInstance(after, this.sourceFileType);
        this.put(rpcMsg(
            RpcObjectState.ADD,
            this.getValueType(after),
            onChange || afterCodec ? null : after,
            ref,
            this.trace()
        ));
        await this.doChange(after, undefined, onChange, afterCodec);
    }

    private async doChange(after: any, before: any, onChange?: () => Promise<void>, afterCodec?: RpcCodec<any>): Promise<void> {
        const lastBefore = this.before;
        this.before = before;
        try {
            if (onChange) {
                if (after !== undefined) {
                    await onChange();
                }
            } else if (afterCodec) {
                await afterCodec.rpcSend(after, this);
            }
        } finally {
            this.before = lastBefore;
        }
    }

    private typesAreDifferent(after: any, before: any): boolean {
        const afterKind = after !== undefined && after !== null && typeof after === "object" ? after["kind"] : undefined;
        const beforeKind = before !== undefined && before !== null && typeof before === "object" ? before["kind"] : undefined;
        return afterKind !== undefined && beforeKind !== undefined && afterKind !== beforeKind;
    }

    private getValueType(after?: any): string | undefined {
        if (after !== undefined && after !== null && typeof after === "object" && "kind" in after) {
            return after["kind"];
        }
    }
}

/**
 * Synchronous reader function type for deserialization.
 * Takes a before state and queue, returns the deserialized after state.
 * May return undefined if the element should be filtered out (e.g., null in lists).
 */
export type SyncReader<T> = (before: T, q: RpcReceiveQueue) => T | undefined;

/**
 * A synchronous RPC receive queue that reads from pre-fetched data.
 *
 * This class:
 * - Requires all data to be fetched upfront (passed in constructor)
 * - Has no async/await - all operations are synchronous
 * - Uses function-based readers instead of async visitor codecs
 *
 * This eliminates the async overhead and GC pressure from the original implementation.
 */
export class RpcReceiveQueue {
    private batchIndex: number = 0;
    private pos: number = 0;

    /**
     * @param batches Array of batches, where each batch is an array of compact RPC messages.
     *                This avoids copying when multiple batches are received.
     * @param refs Map of reference IDs to previously received objects
     * @param sourceFileType The source file type for codec lookup
     * @param tracing Whether to log trace information for debugging
     * @param logger Optional logger for trace output
     */
    constructor(
        private readonly batches: RpcRawMessage[][],
        private readonly refs: Map<number, any>,
        private readonly sourceFileType: string | undefined,
        tracing: boolean = false,
        logger?: rpc.Logger
    ) {
        // Only override take() when tracing is enabled - keeps fast prototype method for common case
        if (tracing && logger) {
            this.take = () => {
                // Advance to next batch if current is exhausted
                while (this.batchIndex < this.batches.length &&
                       this.pos >= this.batches[this.batchIndex].length) {
                    this.batchIndex++;
                    this.pos = 0;
                }
                if (this.batchIndex >= this.batches.length) {
                    throw new Error(`Unexpected end of RPC data`);
                }
                const msg = this.batches[this.batchIndex][this.pos++] as RpcRawMessage;
                if (msg[RpcField.Trace]) {
                    logger.info(`[${msg[RpcField.State]}, ${msg[RpcField.ValueType]}, ${JSON.stringify(msg[RpcField.Value])}, ${msg[RpcField.Ref]}]`);
                    logger.info(`  Sender: ${msg[RpcField.Trace]}`);
                    logger.info(`  Receiver: ${trace("Receiver") || 'No receiver trace'}`);
                }
                return msg;
            };
        }
    }

    /**
     * Take the next message from the pre-fetched batches.
     * Returns the raw compact array directly - no conversion needed.
     */
    take(): RpcRawMessage {
        // Advance to next batch if current is exhausted
        while (this.batchIndex < this.batches.length &&
               this.pos >= this.batches[this.batchIndex].length) {
            this.batchIndex++;
            this.pos = 0;
        }
        if (this.batchIndex >= this.batches.length) {
            throw new Error(`Unexpected end of RPC data`);
        }
        return this.batches[this.batchIndex][this.pos++] as RpcRawMessage;
    }

    /**
     * Check if a batch of RPC data ends with END_OF_OBJECT.
     * Used to determine if more batches need to be fetched.
     */
    static isComplete(batch: any[][]): boolean {
        return batch.length > 0 && batch[batch.length - 1][RpcField.State] === RpcObjectState.END_OF_OBJECT;
    }

    /**
     * Verify that the next message is END_OF_OBJECT.
     * Call this after deserialization to ensure all data was consumed correctly.
     */
    verifyComplete(): void {
        const finalMsg = this.take();
        if (finalMsg[RpcField.State] !== RpcObjectState.END_OF_OBJECT) {
            throw new Error(`Expected END_OF_OBJECT but got ${finalMsg[RpcField.State]}`);
        }
    }

    /**
     * Synchronously receive a value.
     *
     * @param before - The previous state of the object (for delta application)
     * @param reader - Optional custom reader function for complex types
     * @returns The deserialized value
     */
    receive<T>(before: T | undefined, reader?: SyncReader<T>): T | undefined {
        return this.doReceive(before, reader);
    }

    private doReceive<T>(before: T | undefined, reader?: SyncReader<T>): T | undefined {
        const msg = this.take();
        const state = msg[RpcField.State];
        let ref: number | null | undefined;

        switch (state) {
            case RpcObjectState.NO_CHANGE:
                return before;

            case RpcObjectState.DELETE:
                return undefined;

            case RpcObjectState.ADD:
                ref = msg[RpcField.Ref];
                const valueType = msg[RpcField.ValueType];
                const value = msg[RpcField.Value];
                if (ref != null && valueType == null && value == null) {
                    // Pure reference to an existing object
                    if (this.refs.has(ref)) {
                        return this.refs.get(ref) as T;
                    } else {
                        throw new Error(`Received a reference to an object that was not previously sent: ${ref}`);
                    }
                } else {
                    // New object or forward declaration with ref
                    before = valueType == null
                        ? value
                        : this.newObj(valueType);
                    if (ref != null) {
                        this.refs.set(ref, before);
                    }
                }
            // Intentional fall-through to CHANGE...

            case RpcObjectState.CHANGE:
                let after: T | undefined;
                let codec: RpcCodec<T> | undefined;

                if (reader) {
                    // Use provided reader function
                    after = reader(before!, this);
                } else if ((codec = RpcCodecs.forInstance(before, this.sourceFileType))) {
                    // Use registered codec
                    after = codec.rpcReceive(before!, this);
                } else {
                    const msgValue = msg[RpcField.Value];
                    const msgValueType = msg[RpcField.ValueType];
                    if (msgValue != null) {
                        // Use the value directly
                        after = msgValueType
                            ? {kind: msgValueType, ...msgValue} as T
                            : msgValue as T;
                    } else {
                        after = before;
                    }
                }

                if (ref != null) {
                    this.refs.set(ref, after);
                }
                return after;

            default:
                throw new Error(`Unknown state type ${state}`);
        }
    }

    /**
     * Synchronously receive a list.
     *
     * @param before - The previous state of the list
     * @param reader - Optional custom reader function for list elements
     * @returns The deserialized list
     */
    receiveList<T>(before: T[] | undefined, reader?: SyncReader<T>): T[] | undefined {
        const msg = this.take();
        const state = msg[RpcField.State];

        switch (state) {
            case RpcObjectState.NO_CHANGE:
                return before;

            case RpcObjectState.DELETE:
                return undefined;

            case RpcObjectState.ADD:
                before = [];
            // Intentional fall-through to CHANGE...

            case RpcObjectState.CHANGE:
                // The next message should be a CHANGE with a list of positions
                const posMsg = this.take();
                const positions = posMsg[RpcField.Value] as number[];
                if (!positions) {
                    throw new Error(`Expected positions array but got: ${JSON.stringify(posMsg)}`);
                }

                const after: T[] = new Array(positions.length);
                for (let i = 0; i < positions.length; i++) {
                    const beforeIdx = positions[i];
                    const b: T | undefined = beforeIdx >= 0 ? before![beforeIdx] : undefined;
                    after[i] = this.receive<T>(b, reader)!;
                }
                return after;

            default:
                throw new Error(`${state} is not supported for lists.`);
        }
    }

    /**
     * Synchronously receive Markers.
     *
     * @param markers - The previous state of the markers
     * @returns The deserialized markers
     */
    receiveMarkers(markers?: Markers): Markers {
        if (markers === undefined) {
            markers = emptyMarkers;
        }
        return this.receive(markers, m => {
            return updateIfChanged(m, {
                id: this.receive(m.id),
                markers: this.receiveListDefined(m.markers),
            });
        })!;
    }

    /**
     * Synchronously receive a list that is guaranteed to be defined.
     */
    receiveListDefined<T>(before: T[] | undefined, reader?: SyncReader<T>): T[] {
        return this.receiveList(before, reader)!;
    }

    private newObj<T>(type: string): T {
        // First try the reader registry for rpcNew-like functionality
        // For now, just create a basic object with the kind
        return {kind: type} as T;
    }
}

export enum RpcObjectState {
    NO_CHANGE = 0,
    ADD = 1,
    DELETE = 2,
    CHANGE = 3,
    END_OF_OBJECT = 4
}

/**
 * Array indices for compact RPC receive data format.
 * Format: [state, valueType, value, ref?, trace?]
 */
const enum RpcField {
    State = 0,
    ValueType = 1,
    Value = 2,
    Ref = 3,
    Trace = 4
}

/**
 * Compact array format matching serialization format of Java's RpcObjectData.
 * This is the wire format - we access it directly via indices to avoid object creation.
 */
export type RpcRawMessage = [
    state: RpcObjectState,
    valueType: string | null,
    value: any,
    ref?: number | null,
    trace?: string | null
];

/**
 * Construct a compact RpcRawMessage array.
 * Only includes ref/trace slots when needed to minimize array size.
 */
function rpcMsg(
    state: RpcObjectState,
    valueType?: string | null,
    value?: any,
    ref?: number | null,
    trace?: string | null
): RpcRawMessage {
    const msg: RpcRawMessage = [state, valueType ?? null, value ?? null];
    if (ref !== undefined && ref !== null) {
        msg.push(ref);
        if (trace !== undefined && trace !== null) {
            msg.push(trace);
        }
    } else if (trace !== undefined && trace !== null) {
        msg.push(null); // ref slot
        msg.push(trace);
    }
    return msg;
}
