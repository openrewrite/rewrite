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
import {setAutoFreeze} from "immer";
import {LRUCache} from "lru-cache";

// this is required because otherwise `asRef()` won't work for objects created using immer
setAutoFreeze(false);

const REFERENCE_KEY = Symbol("org.openrewrite.rpc.Reference");

export interface Reference {
    [REFERENCE_KEY]: true;
}

export function asRef<T extends {} | undefined>(obj: T): T extends undefined ? undefined : T & Reference {
    if (obj === undefined) {
        return undefined as any;
    } else if (isRef(obj)) {
        return obj as any;
    }
    try {
        // Spread would create a new object. This can be used multiple times on the
        // same object without changing the reference.
        Object.assign(obj, {[REFERENCE_KEY]: true});
        return obj as any;
    } catch (Error) {
        return obj as any;
    }
}

export function isRef(obj?: any): obj is Reference {
    return obj !== undefined && obj !== null && obj[REFERENCE_KEY] === true;
}

export class ReferenceMap {
    private refs = new ObjectIdentityMap<number>();
    private refsById = new MemoryAwareLRU<number, Reference>({
        max: 1000,
        dispose: (value: Reference, key: number) => {
            // When an entry is evicted from refsById, remove it from the forward mapping
            this.refs.delete(value);
        }
    });
    private refCount = 0;

    has(obj: Reference): boolean {
        return this.refs.has(obj);
    }

    get(obj: Reference): number | undefined {
        return this.refs.get(obj);
    }

    getByRefId(refId: number): Reference | undefined {
        return this.refsById.get(refId);
    }

    create(obj: Reference): number {
        const ref = this.refCount++;
        this.refs.set(obj, ref);
        this.refsById.set(ref, obj);
        return ref;
    }

    clear() {
        this.refs.clear();
        this.refsById.clear();
        this.refCount = 0;
    }

    deleteByRefId(refId: number) {
        const obj = this.refsById.get(refId);
        if (obj) {
            this.refs.delete(obj);
            this.refsById.delete(refId);
        }
    }

    set(ref: Reference, refId: number) {
        this.refs.set(ref, refId);
        this.refsById.set(refId, ref);
    }
}

export class ObjectIdentityMap<V> {
    constructor(private objectMap = new WeakMap<{}, V>(),
                private readonly primitiveMap = new Map<any, V>()) {
    }

    set(key: any, value: V): void {
        if (typeof key === 'object' && key !== null) {
            this.objectMap.set(key, value);
        } else {
            this.primitiveMap.set(key, value);
        }
    }

    get(key: any): V | undefined {
        if (typeof key === 'object' && key !== null) {
            return this.objectMap.get(key);
        } else {
            return this.primitiveMap.get(key);
        }
    }

    has(key: any): boolean {
        if (typeof key === 'object' && key !== null) {
            return this.objectMap.has(key);
        } else {
            return this.primitiveMap.has(key);
        }
    }

    delete(key: any): boolean {
        if (typeof key === 'object' && key !== null) {
            return this.objectMap.delete(key);
        } else {
            return this.primitiveMap.delete(key);
        }
    }

    clear() {
        this.objectMap = new WeakMap<{}, V>();
        this.primitiveMap.clear();
    }
}

export type MemoryAwareLRUOptions<K, V, FC = unknown> = LRUCache.Options<K, V, FC> & {
    minFreeRatio?: number;
    resizeFactor?: number;
};

export class MemoryAwareLRU<K extends {}, V extends {}, FC = unknown> extends LRUCache<K, V, FC> {
    private readonly minFreeRatio: number;
    private readonly resizeFactor: number;

    constructor(options: MemoryAwareLRUOptions<K, V, FC>) {
        super(options);
        this.minFreeRatio = options.minFreeRatio ?? 0.1;
        this.resizeFactor = options.resizeFactor ?? 0.9;
    }

    private evictToSize(targetSize: number): void {
        let oldestKeys = this.rkeys();
        while (this.size > targetSize) {
            const oldestKey = oldestKeys.next().value;
            if (oldestKey !== undefined) {
                this.delete(oldestKey);
            } else {
                break;
            }
        }
    }

    set(key: K, value: V, options?: LRUCache.SetOptions<K, V, FC>): this {
        const memUsage = process.memoryUsage();

        if (memUsage.heapUsed > (1 - this.minFreeRatio) * memUsage.heapTotal) {
            const newMax = Math.floor(this.max * this.resizeFactor);
            this.evictToSize(newMax);
        }

        return super.set(key, value, options);
    }
}
