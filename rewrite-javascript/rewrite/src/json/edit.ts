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
import {emptyMarkers} from "../markers";
import {randomId} from "../uuid";
import {Cursor} from "../tree";
import {detectIndent, emptySpace, getMemberKeyName, isArray, isLiteral, isMember, isObject, Json, rightPadded, space} from "./tree";

/**
 * Pure, immutable helpers for reading and editing JSON objects while keeping a document's formatting.
 * Every editing function returns its input (the same reference) when there is nothing to change.
 */

export type JsonRoot = Json.Document | Json.Value;

export interface SetMemberOptions {
    /**
     * Where a new member goes: `"last"` (the default), `"first"`, or `"sorted"`, which places it
     * before the first member whose key sorts after it, the way npm keeps its dependency sections.
     * A member that already exists keeps its place.
     */
    position?: "first" | "last" | "sorted";
    /**
     * The indentation unit, e.g. `"  "`, used to lay out a member added to an object that has no member
     * to copy the indentation from. Without it, a member added to `{}` stays on the same line.
     * The `*At` functions default it to the document's {@link detectIndent} when given a document laid out
     * over several lines.
     */
    indent?: string;
    /** How deep the object is nested, the root object being 0. Used together with {@link indent}. */
    depth?: number;
}

export interface SetAtOptions extends SetMemberOptions {
    /** Whether to create the objects missing along the path. Otherwise a missing step leaves the root unchanged. */
    createMissing?: boolean;
}

export interface RemoveAtOptions {
    /** Whether to also remove the objects along the path that the removal leaves empty. The root object always stays. */
    pruneEmptyParents?: boolean;
}

/**
 * Whether an object or array holds nothing, which the LST represents as a single `Json.Empty`.
 */
export function isEmpty(container: Json.Object | Json.Array): boolean {
    const elements: Json.RightPadded<Json>[] = isObject(container) ? container.members : container.values;
    return elements.length === 0 || (elements.length === 1 && elements[0].element.kind === Json.Kind.Empty);
}

/**
 * The members of an object, without the `Json.Empty` of an empty object or a trailing comma.
 */
export function members(obj: Json.Object): Json.Member[] {
    return obj.members.map(m => m.element).filter(isMember);
}

/**
 * The first member of an object with the given key, matched by {@link getMemberKeyName}.
 */
export function member(obj: Json.Object, key: string): Json.Member | undefined {
    return members(obj).find(m => getMemberKeyName(m) === key);
}

/**
 * The value at a path of member keys, or `undefined` if a step is missing or isn't an object.
 */
export function valueAt(root: JsonRoot, path: readonly string[]): Json.Value | undefined {
    let current: Json.Value | undefined = root.kind === Json.Kind.Document ? (root as Json.Document).value : root as Json.Value;
    for (const key of path) {
        if (current === undefined || !isObject(current)) {
            return undefined;
        }
        current = member(current, key)?.value;
    }
    return current;
}

/**
 * The plain JavaScript value of a literal, array, object or document, or `undefined` where it isn't plain data.
 * A literal is read from its source, so it doesn't depend on how a parser or RPC typed its `value`.
 */
export function toPlain(json: Json): unknown {
    if (json.kind === Json.Kind.Document) {
        return toPlain((json as Json.Document).value);
    }
    if (isLiteral(json)) {
        if (!json.source.startsWith("'")) {
            try {
                return JSON.parse(json.source);
            } catch {
                // a JSON5 literal, such as a hexadecimal number
            }
        }
        return json.value;
    }
    if (isArray(json)) {
        const values = json.values.map(v => v.element).filter(v => v.kind !== Json.Kind.Empty).map(toPlain);
        return values.includes(undefined) ? undefined : values;
    }
    if (isObject(json)) {
        const entries: [string, unknown][] = [];
        for (const m of members(json)) {
            const key = getMemberKeyName(m);
            const value = toPlain(m.value);
            if (key === undefined || value === undefined) {
                return undefined;
            }
            entries.push([key, value]);
        }
        return Object.fromEntries(entries);
    }
    return undefined;
}

/**
 * The member keys from the document root down to the cursor. Array elements on the way contribute no step.
 */
export function pathOf(cursor: Cursor): string[] {
    const path: string[] = [];
    for (const value of cursor.asArray()) {
        if (value?.kind === Json.Kind.Member) {
            const key = getMemberKeyName(value as Json.Member);
            if (key === undefined) {
                return [];
            }
            path.unshift(key);
        }
    }
    return path;
}

/**
 * Adds a member, or replaces the value of an existing one unless it already holds the same data.
 *
 * The value is placed after the colon with the spacing already there: the replaced value's, or a sibling's.
 * A new member copies its indentation from the sibling it is placed next to; in an object without members,
 * it is laid out by {@link SetMemberOptions.indent} and {@link SetMemberOptions.depth}.
 * To change only the formatting or markers of a value, use {@link updateMember}.
 */
export function setMember(obj: Json.Object, key: string, value: Json.Value, options: SetMemberOptions = {}): Json.Object {
    const index = indexOfMember(obj, key);
    if (index >= 0) {
        const existing = obj.members[index].element as Json.Member;
        if (sameData(existing.value, value)) {
            return obj;
        }
        return withElementAt(obj, index, {...existing, value: withPrefix(value, existing.value.prefix)});
    }

    const present = obj.members.map((m, i) => i).filter(i => obj.members[i].element.kind !== Json.Kind.Empty);
    if (present.length === 0) {
        const closing = obj.members.length > 0 ? obj.members[obj.members.length - 1].after : emptySpace;
        const layout = emptyObjectLayout(closing, options);
        const added = newMember(key, value, space(layout.member), emptySpace, space(" "));
        return {...obj, members: [rightPadded<Json>(added, layout.closing)]};
    }

    const insertAt = options.position === "first" ? 0 :
        options.position === "sorted" ? sortedIndex(obj, present, key) :
            present.length;
    const neighbour = obj.members[present[Math.min(insertAt, present.length - 1)]].element as Json.Member;
    const following = followingWhitespace(obj, present);
    const added = newMember(key, value, space(insertAt === 0 ? leadingWhitespace(neighbour) : following),
        neighbour.key.after, space(neighbour.value.prefix.whitespace));

    const elements = [...obj.members];
    if (insertAt === 0) {
        elements[present[0]] = {...elements[present[0]], element: withLeadingWhitespace(neighbour, following)};
    }
    if (insertAt < present.length) {
        elements.splice(present[insertAt], 0, rightPadded<Json>(added, emptySpace));
    } else {
        const last = present[present.length - 1];
        if (last === elements.length - 1) {
            const {trailing, closing} = splitTrailingComment(elements[last].after, following);
            elements.push(rightPadded<Json>(trailing ? {...added, prefix: trailing} : added, closing));
            elements[last] = {...elements[last], after: emptySpace};
        } else {
            elements.splice(last + 1, 0, rightPadded<Json>(added, emptySpace));
        }
    }
    return {...obj, members: elements};
}

/**
 * Replaces the value of an existing member with what `fn` makes of it, after the same space as the old value.
 */
export function updateMember(obj: Json.Object, key: string, fn: (old: Json.Value) => Json.Value): Json.Object {
    const index = indexOfMember(obj, key);
    if (index < 0) {
        return obj;
    }
    const existing = obj.members[index].element as Json.Member;
    const value = fn(existing.value);
    return value === existing.value ? obj : withElementAt(obj, index, {...existing, value: withPrefix(value, existing.value.prefix)});
}

/**
 * Removes a member. The space before the closing brace stays where it is, a removed first member's
 * indentation passes to the next one, and an object left without members holds a single `Json.Empty`.
 */
export function removeMember(obj: Json.Object, key: string): Json.Object {
    const index = indexOfMember(obj, key);
    if (index < 0) {
        return obj;
    }
    const removed = obj.members[index];
    const elements = obj.members.filter((_, i) => i !== index);
    const present = elements.filter(m => m.element.kind !== Json.Kind.Empty);

    if (present.length === 0) {
        const closing = elements.length > 0 ? elements[elements.length - 1].after : removed.after;
        return {...obj, members: [rightPadded<Json>(emptyElement(), closing)]};
    }
    if (index === 0 && isMember(elements[0].element)) {
        const next = elements[0].element;
        elements[0] = {...elements[0], element: withLeadingWhitespace(next, leadingWhitespace(removed.element as Json.Member))};
    }
    if (index === elements.length) {
        elements[index - 1] = {...elements[index - 1], after: removed.after};
    }
    return {...obj, members: elements};
}

/**
 * Sets the value at a path of member keys with {@link setMember}, rebuilding the objects along the path.
 * Returns `root` unchanged if the path is empty, if a step isn't an object, or if a step is missing
 * and {@link SetAtOptions.createMissing} isn't set.
 */
export function setAt<T extends JsonRoot>(root: T, path: readonly string[], value: Json.Value, options: SetAtOptions = {}): T {
    return editRoot(root, path, options, (obj, opts) => setIn(obj, path, value, opts));
}

/**
 * Replaces the value at a path of member keys with what `fn` makes of it, rebuilding the objects along the path.
 * Returns `root` unchanged if the path doesn't resolve.
 */
export function updateAt<T extends JsonRoot>(root: T, path: readonly string[], fn: (old: Json.Value) => Json.Value): T {
    return editRoot(root, path, {}, obj => updateIn(obj, path, fn));
}

/**
 * Removes the member at a path of member keys with {@link removeMember}, rebuilding the objects along the path.
 * Returns `root` unchanged if the path doesn't resolve.
 */
export function removeAt<T extends JsonRoot>(root: T, path: readonly string[], options: RemoveAtOptions = {}): T {
    return editRoot(root, path, {}, obj => removeIn(obj, path, options.pruneEmptyParents ?? false));
}

/**
 * A literal with its `source` written as JSON and its `value` set.
 */
export function literal(value: string | number | boolean | null, prefix: Json.Space = emptySpace): Json.Literal {
    if (typeof value === "number" && !Number.isFinite(value)) {
        throw new RangeError(`${value} can't be written as JSON`);
    }
    return {
        kind: Json.Kind.Literal,
        id: randomId(),
        prefix,
        markers: emptyMarkers,
        source: JSON.stringify(value),
        value
    };
}

/**
 * A JSON value for plain data, laid out one element per line like `JSON.stringify(value, null, indent)`.
 * `depth` is how deep the value will be nested, so that its elements line up with the surrounding document.
 * Object properties that are `undefined` are left out and a `toJSON` method is called, as `JSON.stringify` does.
 */
export function fromPlain(value: unknown, indent: string, depth: number = 0): Json.Value {
    if (typeof (value as { toJSON?: unknown } | null | undefined)?.toJSON === "function") {
        return fromPlain((value as { toJSON(): unknown }).toJSON(), indent, depth);
    }
    if (value === null || typeof value === "string" || typeof value === "number" || typeof value === "boolean") {
        return literal(value);
    }
    const inner = space("\n" + indent.repeat(depth + 1));
    const closing = space("\n" + indent.repeat(depth));
    if (Array.isArray(value)) {
        if (value.length === 0) {
            return emptyArray();
        }
        return {
            kind: Json.Kind.Array,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            values: value.map((v, i) => rightPadded(
                withPrefix(fromPlain(v === undefined ? null : v, indent, depth + 1), inner),
                i === value.length - 1 ? closing : emptySpace
            ))
        };
    }
    if (typeof value === "object") {
        const entries = Object.entries(value).filter(([, v]) => v !== undefined);
        if (entries.length === 0) {
            return emptyObject();
        }
        return {
            kind: Json.Kind.Object,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            members: entries.map(([k, v], i) => rightPadded<Json>(
                newMember(k, fromPlain(v, indent, depth + 1), inner, emptySpace, space(" ")),
                i === entries.length - 1 ? closing : emptySpace
            ))
        };
    }
    throw new TypeError(`A ${typeof value} can't be written as JSON`);
}

/**
 * An object without members: `{}`.
 */
export function emptyObject(prefix: Json.Space = emptySpace): Json.Object {
    return {
        kind: Json.Kind.Object,
        id: randomId(),
        prefix,
        markers: emptyMarkers,
        members: [rightPadded<Json>(emptyElement(), emptySpace)]
    };
}

/**
 * An array without elements: `[]`.
 */
export function emptyArray(prefix: Json.Space = emptySpace): Json.Array {
    return {
        kind: Json.Kind.Array,
        id: randomId(),
        prefix,
        markers: emptyMarkers,
        values: [rightPadded<Json.Value>(emptyElement(), emptySpace)]
    };
}

function indexOfMember(obj: Json.Object, key: string): number {
    return obj.members.findIndex(m => isMember(m.element) && getMemberKeyName(m.element) === key);
}

function withElementAt(obj: Json.Object, index: number, element: Json.Member): Json.Object {
    const elements = [...obj.members];
    elements[index] = {...elements[index], element};
    return {...obj, members: elements};
}

function withPrefix<T extends Json>(json: T, prefix: Json.Space): T {
    return json.prefix === prefix ? json : {...json, prefix};
}

function emptyElement(): Json.Empty {
    return {kind: Json.Kind.Empty, id: randomId(), prefix: emptySpace, markers: emptyMarkers};
}

function newMember(key: string, value: Json.Value, prefix: Json.Space, keyAfter: Json.Space, valuePrefix: Json.Space): Json.Member {
    return {
        kind: Json.Kind.Member,
        id: randomId(),
        prefix,
        markers: emptyMarkers,
        key: rightPadded<Json.Key>(literal(key), keyAfter),
        value: withPrefix(value, valuePrefix)
    };
}

function sameData(a: Json.Value, b: Json.Value): boolean {
    if (a === b) {
        return true;
    }
    const plain = toPlain(a);
    return plain !== undefined && JSON.stringify(plain) === JSON.stringify(toPlain(b));
}

/**
 * The indentation of a member that isn't the first, which may differ from the first one's in an object
 * laid out on one line.
 */
function followingWhitespace(obj: Json.Object, present: number[]): string {
    if (present.length > 1) {
        return leadingWhitespace(obj.members[present[1]].element as Json.Member);
    }
    const first = leadingWhitespace(obj.members[present[0]].element as Json.Member);
    return first.includes("\n") ? first : " ";
}

function sortedIndex(obj: Json.Object, present: number[], key: string): number {
    const index = present.findIndex(i => {
        const m = obj.members[i].element;
        const name = isMember(m) ? getMemberKeyName(m) : undefined;
        return name !== undefined && name.localeCompare(key, "en") > 0;
    });
    return index < 0 ? present.length : index;
}

/**
 * A comment on the same line as the last member, which goes after the comma that a member appended to it adds,
 * the way the parsers attach such a comment to the member after it.
 */
function splitTrailingComment(after: Json.Space, following: string): { trailing?: Json.Space, closing: Json.Space } {
    if (after.comments.length === 0 || after.whitespace.includes("\n")) {
        return {closing: after};
    }
    const [comment, ...rest] = after.comments;
    return {
        trailing: {kind: Json.Kind.Space, whitespace: after.whitespace, comments: [{...comment, suffix: following}]},
        closing: rest.length === 0 ? space(comment.suffix) : {kind: Json.Kind.Space, whitespace: comment.suffix, comments: rest}
    };
}

function emptyObjectLayout(closing: Json.Space, options: SetMemberOptions): { member: string, closing: Json.Space } {
    const lineBreak = [closing.whitespace, ...closing.comments.map(c => c.suffix)].filter(s => s.includes("\n")).pop();
    if (lineBreak !== undefined) {
        return {member: "\n" + lineBreak.substring(lineBreak.lastIndexOf("\n") + 1) + (options.indent ?? "    "), closing};
    }
    if (options.indent !== undefined) {
        const base = "\n" + options.indent.repeat(options.depth ?? 0);
        return {member: base + options.indent, closing: space(base)};
    }
    return {member: "", closing};
}

/**
 * LSTs built before the parsers put a member's indentation on the member (#8644) have it on the key.
 */
function indentationOnKey(m: Json.Member): boolean {
    return m.prefix.whitespace === "" && m.prefix.comments.length === 0 && m.key.element.prefix.whitespace !== "";
}

function leadingWhitespace(m: Json.Member): string {
    return indentationOnKey(m) ? m.key.element.prefix.whitespace : m.prefix.whitespace;
}

function withLeadingWhitespace(m: Json.Member, whitespace: string): Json.Member {
    if (leadingWhitespace(m) === whitespace) {
        return m;
    }
    if (indentationOnKey(m)) {
        const key = m.key.element;
        return {...m, key: {...m.key, element: {...key, prefix: {...key.prefix, whitespace}}}};
    }
    return {...m, prefix: {...m.prefix, whitespace}};
}

function editRoot<T extends JsonRoot>(root: T, path: readonly string[], options: SetMemberOptions,
                                      edit: (obj: Json.Object, options: SetMemberOptions) => Json.Object): T {
    if (path.length === 0) {
        return root;
    }
    if (root.kind === Json.Kind.Document) {
        const doc = root as Json.Document;
        if (!isObject(doc.value)) {
            return root;
        }
        const value = edit(doc.value, {...options, indent: options.indent ?? documentIndent(doc, doc.value)});
        return value === doc.value ? root : {...doc, value} as T;
    }
    return isObject(root as Json.Value) ? edit(root as Json.Object, options) as T : root;
}

function documentIndent(doc: Json.Document, obj: Json.Object): string | undefined {
    const first = members(obj)[0];
    if (first === undefined) {
        return detectIndent(doc);
    }
    const whitespace = leadingWhitespace(first);
    return whitespace.includes("\n") ? whitespace.match(/\n([ \t]+)$/)?.[1] ?? detectIndent(doc) : undefined;
}

function setIn(obj: Json.Object, path: readonly string[], value: Json.Value, options: SetAtOptions): Json.Object {
    const [key, ...rest] = path;
    if (rest.length === 0) {
        return setMember(obj, key, value, options);
    }
    const nested = {...options, depth: (options.depth ?? 0) + 1};
    const child = member(obj, key)?.value;
    if (child === undefined) {
        return options.createMissing ? setMember(obj, key, setIn(emptyObject(), rest, value, nested), options) : obj;
    }
    return isObject(child) ? updateMember(obj, key, () => setIn(child, rest, value, nested)) : obj;
}

function updateIn(obj: Json.Object, path: readonly string[], fn: (old: Json.Value) => Json.Value): Json.Object {
    const [key, ...rest] = path;
    if (rest.length === 0) {
        return updateMember(obj, key, fn);
    }
    return updateMember(obj, key, child => isObject(child) ? updateIn(child, rest, fn) : child);
}

function removeIn(obj: Json.Object, path: readonly string[], pruneEmptyParents: boolean): Json.Object {
    const [key, ...rest] = path;
    if (rest.length === 0) {
        return removeMember(obj, key);
    }
    const child = member(obj, key)?.value;
    if (child === undefined || !isObject(child)) {
        return obj;
    }
    const updated = removeIn(child, rest, pruneEmptyParents);
    if (updated === child) {
        return obj;
    }
    return pruneEmptyParents && isEmpty(updated) ? removeMember(obj, key) : updateMember(obj, key, () => updated);
}
