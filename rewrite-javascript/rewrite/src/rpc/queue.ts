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
    private q: RpcObjectData[] = [];

    private _before?: any;

    constructor(private readonly refs: ReferenceMap,
                private readonly sourceFileType: string | undefined,
                private readonly trace: boolean) {
    }

    get before(): any {
        return this._before;
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

    getAndSend<T, U>(parent: T,
                     value: (parent: T) => U | undefined,
                     onChange?: (value: U) => Promise<any>,
                     valueType?: string): Promise<void> {
        const after = value(parent);
        const before = this._before === undefined ? undefined : value(this._before as T);
        return this.send(after, before, onChange && (() => onChange(after!)), valueType);
    }

    getAndSendList<T, U>(parent: T,
                         values: (parent: T) => U[] | undefined,
                         id: (value: U) => any,
                         onChange?: (value: U) => Promise<any>,
                         valueType?: string): Promise<void> {
        const after = values(parent);
        const before = this._before === undefined ? undefined : values(this._before as T);
        return this.sendList(after, before, id, onChange, valueType);
    }

    send<T>(after: T | undefined, before: T | undefined, onChange?: (() => Promise<any>), valueType?: string): Promise<void> {
        return saveTrace(this.trace, async () => {
            if (before === after) {
                this.put({state: RpcObjectState.NO_CHANGE});
            } else if (before === undefined || (after !== undefined && this.typesAreDifferent(after, before))) {
                // Treat as ADD when before is undefined OR types differ (it's a new object, not a change)
                await this.add(after, onChange, valueType);
            } else if (after === undefined) {
                this.put({state: RpcObjectState.DELETE});
            } else {
                let afterCodec = onChange ? undefined : RpcCodecs.forInstance(after, this.sourceFileType);
                this.put({state: RpcObjectState.CHANGE, value: onChange || afterCodec ? undefined : after});
                await this.doChange(after, before, onChange, afterCodec);
            }
        });
    }

    sendList<T>(after: T[] | undefined,
                before: T[] | undefined,
                id: (value: T) => any,
                onChange?: (value: T) => Promise<any>,
                valueType?: string): Promise<void> {
        return this.send(after, before, async () => {
            if (!after) {
                throw new Error("A DELETE event should have been sent.");
            }

            const beforeIdx = this.putListPositions(after, before, id);

            for (const anAfter of after) {
                const beforePos = beforeIdx.get(id(anAfter));
                const onChangeRun = onChange ? () => onChange(anAfter) : undefined;
                if (!beforePos) {
                    await this.add(anAfter, onChangeRun, valueType);
                } else {
                    const aBefore = before?.[beforePos];
                    if (aBefore === anAfter) {
                        this.put({state: RpcObjectState.NO_CHANGE});
                    } else if (anAfter !== undefined && this.typesAreDifferent(anAfter, aBefore)) {
                        // Type changed - treat as ADD
                        await this.add(anAfter, onChangeRun, valueType);
                    } else {
                        this.put({state: RpcObjectState.CHANGE});
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
        this.put({state: RpcObjectState.CHANGE, value: positions});
        return beforeIdx;
    }

    private async add(after: any, onChange: (() => Promise<any>) | undefined, valueType?: string): Promise<void> {
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
        let afterCodec = onChange ? undefined : RpcCodecs.forInstance(after, this.sourceFileType);
        this.put({
            state: RpcObjectState.ADD,
            valueType: valueType ?? this.getValueType(after),
            value: onChange || afterCodec ? undefined : after,
            ref: ref
        });
        await this.doChange(after, undefined, onChange, afterCodec);
    }

    private async doChange(after: any, before: any, onChange?: () => Promise<void>, afterCodec?: RpcCodec<any>): Promise<void> {
        const lastBefore = this._before;
        this._before = before;
        try {
            if (onChange) {
                if (after !== undefined) {
                    await onChange();
                }
            } else if (afterCodec) {
                await afterCodec.rpcSend(after, this);
            }
        } finally {
            this._before = lastBefore;
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

export class RpcReceiveQueue {
    private batch: RpcObjectData[] = [];

    constructor(private readonly refs: Map<number, any>,
                private readonly sourceFileType: string | undefined,
                private readonly pull: () => Promise<RpcObjectData[]>,
                private readonly logger: rpc.Logger | undefined,
                private readonly trace: boolean) {
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
            return saveTrace(this.trace, async () => {
                return updateIfChanged(markers!, {
                    id: await this.receive(m.id),
                    markers: (await this.receiveList(m.markers))!,
                });
            })
        })
    }

    receive<T extends any | undefined>(
        before: T | undefined,
        onChange?: (before: T) => T | Promise<T | undefined> | undefined
    ): Promise<T> {
        return saveTrace(this.trace, async () => {
            const message = await this.take();
            RpcObjectData.logTrace(message, this.trace, this.logger);
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
                        if (ref !== undefined) {
                            // For an object like JavaType that we will mutate in place rather than using
                            // immutable updates because of its cyclic nature, the before instance will ultimately
                            // be the same as the after instance below.
                            this.refs.set(ref, before);
                        }
                    }
                // Intentional fall-through...
                case RpcObjectState.CHANGE:
                    let after;
                    let codec;
                    if (onChange) {
                        after = await onChange(before!);
                    } else if ((codec = RpcCodecs.forInstance(before, this.sourceFileType))) {
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
        return saveTrace(this.trace, async () => {
            const message = await this.take();
            RpcObjectData.logTrace(message, this.trace, this.logger);
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
                    let allUnchanged = before !== undefined && positions.length === before.length;
                    for (let i = 0; i < positions.length; i++) {
                        const beforeIdx = positions[i];
                        const b: T = await (beforeIdx >= 0 ? before![beforeIdx] as T : undefined) as T;
                        let received: T = await this.receive<T>(b, onChange);
                        after[i] = received;
                        // Check if this element is unchanged and in the same position
                        if (allUnchanged && (beforeIdx !== i || received !== b)) {
                            allUnchanged = false;
                        }
                    }
                    // Preserve array identity if nothing changed
                    return allUnchanged ? before! : after;
                default:
                    throw new Error(`${message.state} is not supported for lists.`);
            }
        });
    }

    private newObj<T>(type: string): T {
        const codec = RpcCodecs.forType(type, this.sourceFileType);
        if (codec?.rpcNew) {
            return codec.rpcNew();
        }
        return {kind: type} as T;
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

export namespace RpcObjectData {
    export function logTrace(message: RpcObjectData, enabled: boolean, logger: rpc.Logger | undefined): void {
        if (enabled && logger && message.trace) {
            const sendTrace = message.trace;
            delete message.trace;
            logger.info(`${JSON.stringify(message)}`);
            logger.info(`  ${sendTrace || 'No sender trace'}`);
            logger.info(`  ${trace("Receiver") || 'No receiver trace'}`);
        }
    }
}

export enum RpcObjectState {
    NO_CHANGE = "NO_CHANGE",
    ADD = "ADD",
    DELETE = "DELETE",
    CHANGE = "CHANGE",
    END_OF_OBJECT = "END_OF_OBJECT"
}
