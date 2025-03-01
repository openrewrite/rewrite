import {Marker} from "../markers";

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
    private readonly batchSize: number
    private batch: RpcObjectData[] = [];
    private readonly drain: (batch: RpcObjectData[]) => void;

    private readonly refs: WeakMap<Object, number>;
    private refCount = 1

    private before?: any;

    constructor(batchSize: number,
                drain: (batch: RpcObjectData[]) => void,
                refs: WeakMap<Object, number>) {
        this.batchSize = batchSize
        this.drain = drain;
        this.refs = refs;
    }

    put(rpcObjectData: RpcObjectData): void {
        this.batch.push(rpcObjectData);
        if (this.batch.length === this.batchSize) {
            this.flush();
        }
    }

    flush(): void {
        if (this.batch.length === 0) {
            return;
        }
        this.drain([...this.batch]);
        this.batch = [];
    }

    sendMarkers<T>(parent: T, markersFn: (parent: T) => any): void {
        this.getAndSend(parent, markersFn, (markersRef) => {
            this.getAndSendList(markersRef,
                (m) => m.getMarkers(),
                (marker: Marker) => marker.id
            );
        });
    }

    getAndSend<T, U>(parent: T,
                     value: (parent: T) => U | undefined,
                     onChange?: (value: U) => void): void {
        const after = value(parent);
        const before = this.before === undefined ? undefined : value(this.before as T);
        this.send(after, before, onChange ? () => onChange(after!) : undefined);
    }

    getAndSendList<T, U>(parent: T,
                         values: (parent: T) => U[] | undefined,
                         id: (value: U) => any,
                         onChange?: (value: U) => void): void {
        const after = values(parent);
        const before = this.before === undefined ? undefined : values(this.before as T);
        this.sendList(after, before, id, onChange);
    }

    send<T>(after: T | undefined, before: T | undefined, onChange: (() => void) | undefined): void {
        if (before === after) {
            this.put({state: RpcObjectState.NO_CHANGE});
        } else if (before === undefined) {
            this.add(after, onChange);
        } else if (after === undefined) {
            this.put({state: RpcObjectState.DELETE});
        } else {
            this.put({state: RpcObjectState.CHANGE, value: onChange ? after : undefined});
            this.doChange(after, before, onChange);
        }
    }

    sendList<T>(after: T[] | undefined,
                before: T[] | undefined,
                id: (value: T) => any,
                onChange?: (value: T) => void): void {
        this.send(after, before, () => {
            if (!after) {
                throw new Error("A DELETE event should have been sent.");
            }

            const beforeIdx = this.putListPositions(after, before, id);

            for (const anAfter of after) {
                const beforePos = beforeIdx.get(id(anAfter));
                const onChangeRun = onChange ? () => onChange(anAfter) : undefined;
                if (!beforePos) {
                    this.add(anAfter, onChangeRun);
                } else {
                    const aBefore = before ? before[beforePos] : undefined;
                    if (aBefore === anAfter) {
                        this.put({state: RpcObjectState.NO_CHANGE});
                    } else {
                        this.put({state: RpcObjectState.CHANGE});
                        this.doChange(anAfter, aBefore, onChangeRun);
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

    private add(after: any, onChange: (() => void) | undefined): void {
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
        this.doChange(after, undefined, onChange);
    }

    private doChange(after: any, before: any, onChange: (() => void) | undefined): void {
        if (onChange) {
            const lastBefore = this.before;
            this.before = before;
            if (after !== undefined) {
                onChange();
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
