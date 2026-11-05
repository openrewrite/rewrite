/*
 * Copyright 2026 the original author or authors.
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
import {Cursor} from "../tree";
import {J} from "../java";

type Callback = (tree: J) => Promise<J>;

const callbacks = new WeakMap<Cursor, Callback[]>();
const none: readonly Callback[] = [];

/**
 * Has `callback` rewrite the tree `cursor` points at once the visitor is done with it and its children.
 * A change a child cannot make by itself, such as to the padding its parent holds it in, goes through here.
 */
export function afterChildren(cursor: Cursor, callback: Callback): void {
    const pending = callbacks.get(cursor);
    if (pending) {
        pending.push(callback);
    } else {
        callbacks.set(cursor, [callback]);
    }
}

/** The callbacks {@link afterChildren} registered for `cursor`, which are then no longer registered. */
export function takeAfterChildren(cursor: Cursor): readonly Callback[] {
    const pending = callbacks.get(cursor);
    if (!pending) {
        return none;
    }
    callbacks.delete(cursor);
    return pending;
}
