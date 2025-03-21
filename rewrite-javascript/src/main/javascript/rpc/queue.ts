import {Marker} from "../markers";
import {RpcCodecs} from "./codec";
import {Queue} from "async-await-queue";

const REFERENCE_KEY = Symbol("org.openrewrite.rpc.Reference");

export interface Reference {
    [REFERENCE_KEY]: true;
}

export function asRef<T>(obj: T): T & Reference {
    return {...obj, [REFERENCE_KEY]: true};
}

function isRef(obj?: any): obj is Reference {
    return obj !== undefined && obj[REFERENCE_KEY] === true;
}

export class RpcSendQueue {
    private readonly batch = new Queue(2, 100);
    private readonly refs: WeakMap<Object, number>;

    private resolver?: (d: RpcObjectData) => void;
    private next?: Promise<RpcObjectData>;

    private refCount = 1
    private before?: any;

    constructor(refs: WeakMap<Object, number>) {
        this.refs = refs;
    }

    async* generate(after: any, before: any): AsyncGenerator<RpcObjectData> {
        const afterCodec = RpcCodecs.forInstance(after);
        const onChange = afterCodec ? async () => {
            afterCodec?.rpcSend(after, this);
        } : undefined;
        const top: Promise<void> = this.send(after, before, onChange);

        let data;
        while(/*!top.resolved ||*/ (data = await this.next)) {
            yield data;
        }

        await top;
        return {state: RpcObjectState.END_OF_OBJECT};
    }

    private put(d: RpcObjectData): void {
        // await next is undefined

        // What do I do here to make d available to the generator?
        this.next = Promise.resolve(this.resolver);
    }

    async sendMarkers<T>(parent: T, markersFn: (parent: T) => any): Promise<void> {
        await this.getAndSend(parent, markersFn, async (markersRef) => {
            await this.getAndSendList(markersRef,
                (m) => m.getMarkers(),
                (marker: Marker) => marker.id
            );
        });
    }

    async getAndSend<T, U>(parent: T,
                           value: (parent: T) => U | undefined,
                           onChange?: (value: U) => Promise<any>): Promise<void> {
        const after = value(parent);
        const before = this.before === undefined ? undefined : value(this.before as T);
        await this.send(after, before, onChange ? () => onChange(after!) : undefined);
    }

    async getAndSendList<T, U>(parent: T,
                               values: (parent: T) => U[] | undefined,
                               id: (value: U) => any,
                               onChange?: (value: U) => Promise<any>): Promise<void> {
        const after = values(parent);
        const before = this.before === undefined ? undefined : values(this.before as T);
        await this.sendList(after, before, id, onChange);
    }

    async send<T>(after: T | undefined, before: T | undefined, onChange: (() => Promise<any>) | undefined): Promise<void> {
        if (before === after) {
            this.put({state: RpcObjectState.NO_CHANGE});
        } else if (before === undefined) {
            await this.add(after, onChange);
        } else if (after === undefined) {
            this.put({state: RpcObjectState.DELETE});
        } else {
            this.put({state: RpcObjectState.CHANGE, value: onChange ? after : undefined});
            await this.doChange(after, before, onChange);
        }
    }

    async sendList<T>(after: T[] | undefined,
                      before: T[] | undefined,
                      id: (value: T) => any,
                      onChange?: (value: T) => Promise<any>): Promise<void> {
        await this.send(after, before, async () => {
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
                        await this.doChange(anAfter, aBefore, onChangeRun);
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
            if (this.refs.has(after)) {
                this.put({
                    state: RpcObjectState.ADD,
                    valueType: this.getValueType(after),
                    ref: this.refs.get(after)
                });
                return;
            }
            ref = this.refCount++;
            this.refs.set(after, ref);
        }
        this.put({
            state: RpcObjectState.ADD,
            valueType: this.getValueType(after),
            value: onChange ? undefined : after,
            ref: ref
        });
        await this.doChange(after, undefined, onChange);
    }

    private async doChange(after: any, before: any, onChange: (() => Promise<void>) | undefined): Promise<void> {
        if (onChange) {
            const lastBefore = this.before;
            this.before = before;
            if (after !== undefined) {
                await onChange();
            }
            this.before = lastBefore;
        }
    }

    private getValueType(after?: any): string | undefined {
        if (after === undefined) {
            return undefined;
        }
        const type = after.constructor;
        if (/* is primitive */ ((typeof type !== "object" && typeof type !== "function") || type === undefined) ||
            type.name.startsWith("java.lang") ||
            type instanceof Uint8Array ||
            Array.isArray(after)) {
            return undefined;
        }
        return type.name;
    }
}

export class RpcReceiveQueue {
    private batch: RpcObjectData[] = [];
    private refs: Map<number, any>;
    private readonly pull: () => Promise<RpcObjectData[]>;

    constructor(refs: Map<number, any>, pull: () => Promise<RpcObjectData[]>) {
        this.refs = refs;
        this.pull = pull;
    }

    async take(): Promise<RpcObjectData> {
        if (this.batch.length === 0) {
            this.batch = await this.pull();
        }
        return this.batch.shift()!;
    }

    async receiveAndGet<T, U extends any | undefined>(before: T | undefined, apply: (before: T) => U): Promise<U> {
        return await this.receive(before === undefined ? undefined : apply(before)) as U;
    }

    async receiveMarkers(markers: any): Promise<any> {
        return this.receive(markers, (m: any) => m.withMarkers(this.receiveList(m.getMarkers())));
    }

    async receive<T extends any | undefined>(
        before: T | undefined,
        onChange?: (before: T) => T | Promise<T | undefined> | undefined
    ): Promise<T> {
        const message = await this.take();
        let ref: number | undefined;
        switch (message.state) {
            case RpcObjectState.NO_CHANGE:
                return before!;
            case RpcObjectState.DELETE:
                return undefined as T;
            case RpcObjectState.ADD:
                ref = message.ref;
                if (ref !== undefined && this.refs.has(ref)) {
                    return this.refs.get(ref);
                }
                before = !onChange || message.valueType === undefined ? message.value : this.newObj(message.valueType);
            // Intentional fall-through...
            case RpcObjectState.CHANGE:
                const after = onChange ? onChange(before!) : message.value;
                if (ref !== undefined) {
                    this.refs.set(ref, after);
                }
                return after;
            default:
                throw new Error(`Unknown state type ${message.state}`);
        }
    }

    async receiveListDefined<T>(
        before: T[] | undefined,
        onChange?: (before: T) => T | Promise<T | undefined> | undefined
    ): Promise<T[]> {
        return (await this.receiveList(before, onChange))!;
    }

    async receiveList<T>(
        before: T[] | undefined,
        onChange?: (before: T) => T | Promise<T | undefined> | undefined
    ): Promise<T[] | undefined> {
        const msg = await this.take();
        switch (msg.state) {
            case RpcObjectState.NO_CHANGE:
                return before;
            case RpcObjectState.DELETE:
                return undefined;
            case RpcObjectState.ADD:
                before = [];
            // Intentional fall-through...
            case RpcObjectState.CHANGE:
                const positions = (await this.take()).value as number[];
                const after: T[] = new Array(positions.length);
                for (let i = 0; i < positions.length; i++) {
                    const beforeIdx = positions[i];
                    const b: T = await (beforeIdx >= 0 ? before![beforeIdx] as T : undefined) as T;
                    let received: Promise<T> = this.receive<T>(b, onChange);
                    after[i] = await received;
                }
                return after;
            default:
                throw new Error(`${msg.state} is not supported for lists.`);
        }
    }

    private newObj<T>(type: string): T {
        const clazz = require(type);
        return new clazz();
    }
}

export interface RpcObjectData {
    state: RpcObjectState
    valueType?: string
    value?: any
    ref?: number
}

export enum RpcObjectState {
    NO_CHANGE,
    ADD,
    DELETE,
    CHANGE,
    END_OF_OBJECT
}
