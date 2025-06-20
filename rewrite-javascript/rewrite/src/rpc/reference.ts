/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    private refs = new WeakMap<Reference, number>();
    private refCount = 0;

    has(obj: Reference): boolean {
        return this.refs.has(obj);
    }

    get(obj: Reference): number | undefined {
        return this.refs.get(obj);
    }

    create(obj: Reference): number {
        const ref = this.refCount++;
        this.refs.set(obj, ref);
        return ref;
    }

    clear() {
        this.refs = new WeakMap<Reference, number>();
        this.refCount = 0;
    }
}