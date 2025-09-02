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
        (obj as any)[REFERENCE_KEY] = true;
        return obj as any;
    } catch {
        // The object is frozen/sealed/non-extensible. Clone to a new object.
        const cloned = {...obj as any};
        cloned[REFERENCE_KEY] = true;
        return cloned;
    }
}

export function isRef(obj: any): obj is Reference {
    return obj !== undefined && obj[REFERENCE_KEY] === true;
}

export class ReferenceMap {
    private refs = new WeakMap<Reference, number>();
    private refsById = new Map<number, Reference>();
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
        this.refs = new WeakMap<Reference, number>();
        this.refsById = new Map<number, Reference>();
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