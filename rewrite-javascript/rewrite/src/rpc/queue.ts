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
import {emptyMarkers, Marker, Markers, MarkersKind} from "../markers";
import {saveTrace, trace} from "./trace";
import {createDraft, finishDraft} from "immer";
import {asRef, isRef, Reference, ReferenceMap} from "../reference";
import {Writable} from "node:stream";

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
        if (before !== undefined && before !== null && typeof before === "object" && "kind" in before) {
            return RpcCodecs.forType(before["kind"] as string);
        }
    }
}

export class RpcSendQueue {
    private q: RpcObjectData[] = [];

    private before?: any;

    constructor(private readonly refs: ReferenceMap, private readonly trace: boolean) {
    }

    async generate(after: any, before: any): Promise<RpcObjectData[]> {
        await this.send(after, before);

        const result = this.q;
        result.push({state: RpcObjectState.END_OF_OBJECT});

        this.q = [];
        return result;
    }

    private put(d: RpcObjectData): void {
        if (this.trace) {
            d.trace = trace("Sender");
        }
        this.q.push(d);
    }

    sendMarkers<T extends { markers: Markers }>(parent: T, markersFn: (parent: T) => any): Promise<void> {
        return this.getAndSend(parent, t2 => asRef(markersFn(t2)), async (markersRef: Markers & Reference) => {
            await this.getAndSend(markersRef, m => m.id);
            await this.getAndSendList(markersRef,
                (m) => m.markers,
                (marker: Marker) => marker.id
            );
        });
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
        return saveTrace(this.trace, async () => {
            if (before === after) {
                this.put({state: RpcObjectState.NO_CHANGE});
            } else if (before === undefined) {
                await this.add(after, onChange);
            } else if (after === undefined) {
                this.put({state: RpcObjectState.DELETE});
            } else {
                let afterCodec = onChange ? undefined : RpcCodecs.forInstance(after);
                this.put({state: RpcObjectState.CHANGE, value: onChange || afterCodec ? undefined : after});
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
                    const aBefore = before ? before[beforePos] : undefined;
                    if (aBefore === anAfter) {
                        this.put({state: RpcObjectState.NO_CHANGE});
                    } else {
                        this.put({state: RpcObjectState.CHANGE});
                        await this.doChange(anAfter, aBefore, onChangeRun, RpcCodecs.forInstance(anAfter));
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
        this.put({state: RpcObjectState.CHANGE, value: positions});
        return beforeIdx;
    }

    private async add(after: any, onChange: (() => Promise<any>) | undefined): Promise<void> {
        let ref: number | undefined;
        if (isRef(after)) {
            ref = this.refs.get(after);
            if (ref) {
                this.put({
                    state: RpcObjectState.ADD,
                    ref
                });
                return;
            }
            ref = this.refs.create(after);
        }
        let afterCodec = onChange ? undefined : RpcCodecs.forInstance(after);
        this.put({
            state: RpcObjectState.ADD,
            valueType: this.getValueType(after),
            value: onChange || afterCodec ? undefined : after,
            ref: ref
        });
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

    private getValueType(after?: any): string | undefined {
        if (after !== undefined && after !== null && typeof after === "object" && "kind" in after) {
            return after["kind"];
        }
    }
}

export class RpcReceiveQueue {
    private batch: RpcObjectData[] = [];

    constructor(private readonly refs: Map<number, any>,
                private readonly pull: () => Promise<RpcObjectData[]>,
                private readonly logFile?: Writable) {
    }

    async take(): Promise<RpcObjectData> {
        if (this.batch.length === 0) {
            this.batch = await this.pull();
        }
        return this.batch.shift()!;
    }

    receiveMarkers(markers?: Markers): Promise<Markers> {
        if (markers === undefined) {
            markers = emptyMarkers;
        }
        return this.receive(markers, async m => {
            return saveTrace(this.logFile, async () => {
                const draft = createDraft(markers!);
                draft.id = await this.receive(m.id);
                draft.markers = (await this.receiveList(m.markers))!;
                return finishDraft(draft);
            })
        })
    }

    receive<T extends any | undefined>(
        before: T | undefined,
        onChange?: (before: T) => T | Promise<T | undefined> | undefined
    ): Promise<T> {
        return saveTrace(this.logFile, async () => {
            const message = await this.take();
            this.traceMessage(message);
            let ref: number | undefined;
            switch (message.state) {
                case RpcObjectState.NO_CHANGE:
                    return before!;
                case RpcObjectState.DELETE:
                    return undefined as T;
                case RpcObjectState.ADD:
                    ref = message.ref;
                    if (ref !== undefined && message.valueType === undefined && message.value === undefined) {
                        // This is a pure reference to an existing object
                        if (this.refs.has(ref)) {
                            return this.refs.get(ref);
                        } else {
                            throw new Error(`Received a reference to an object that was not previously sent: ${ref}`);
                        }
                    } else {
                        // This is either a new object or a forward declaration with ref
                        before = message.valueType === undefined ?
                            message.value :
                            this.newObj(message.valueType);
                    }
                // Intentional fall-through...
                case RpcObjectState.CHANGE:
                    let after;
                    let codec;
                    if (onChange) {
                        after = await onChange(before!);
                    } else if ((codec = RpcCodecs.forInstance(before))) {
                        after = await codec.rpcReceive(before, this);
                    } else if (message.value !== undefined) {
                        after = message.valueType ? {kind: message.valueType, ...message.value} : message.value;
                    } else {
                        after = before;
                    }
                    if (ref !== undefined) {
                        this.refs.set(ref, after);
                    }
                    return after;
                default:
                    throw new Error(`Unknown state type ${message.state}`);
            }
        });
    }

    async receiveListDefined<T>(
        before: T[] | undefined,
        onChange?: (before: T) => T | Promise<T | undefined> | undefined
    ): Promise<T[]> {
        return (await this.receiveList(before, onChange))!;
    }

    receiveList<T>(
        before: T[] | undefined,
        onChange?: (before: T) => T | Promise<T | undefined> | undefined
    ): Promise<T[] | undefined> {
        return saveTrace(this.logFile, async () => {
            const message = await this.take();
            this.traceMessage(message);
            switch (message.state) {
                case RpcObjectState.NO_CHANGE:
                    return before;
                case RpcObjectState.DELETE:
                    return undefined;
                case RpcObjectState.ADD:
                    before = [];
                // Intentional fall-through...
                case RpcObjectState.CHANGE:
                    // The next message should be a CHANGE with a list of positions
                    const d = await this.take();
                    const positions = d.value as number[];
                    if (!positions) {
                        throw new Error(`Expected positions array but got: ${JSON.stringify(d)}`);
                    }
                    const after: T[] = new Array(positions.length);
                    for (let i = 0; i < positions.length; i++) {
                        const beforeIdx = positions[i];
                        const b: T = await (beforeIdx >= 0 ? before![beforeIdx] as T : undefined) as T;
                        let received: Promise<T> = this.receive<T>(b, onChange);
                        after[i] = await received;
                    }
                    return after;
                default:
                    throw new Error(`${message.state} is not supported for lists.`);
            }
        });
    }

    private traceMessage(message: RpcObjectData) {
        if (this.logFile && message.trace) {
            const sendTrace = message.trace;
            delete message.trace;
            this.logFile.write(`${JSON.stringify(message)}\n`);
            this.logFile.write(`  ${sendTrace}\n`);
            this.logFile.write(`  ${trace("Receiver")}\n`);
        }
    }

    private newObj<T>(type: string): T {
        return {
            kind: type
        } as T;
    }
}

/**
 * Refer to RpcObjectData.java for a description of these fields.
 */
export interface RpcObjectData {
    state: RpcObjectState
    valueType?: string
    value?: any
    ref?: number
    trace?: string
}

export enum RpcObjectState {
    NO_CHANGE = "NO_CHANGE",
    ADD = "ADD",
    DELETE = "DELETE",
    CHANGE = "CHANGE",
    END_OF_OBJECT = "END_OF_OBJECT"
}
