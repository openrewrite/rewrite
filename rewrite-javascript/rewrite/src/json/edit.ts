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
import {
    detectIndent,
    emptySpace,
    getMemberKeyName,
    isArray,
    isLiteral,
    isMember,
    isObject,
    Json,
    rightPadded,
    space
} from "./tree";

/**
 * Pure, immutable helpers for reading and editing JSON objects while keeping a document's formatting.
 * Every editing function returns its input (the same reference) when there is nothing to change.
 */

export type JsonRoot = Json.Document | Json.Value;

export interface LayoutOptions {
    /**
     * The indentation unit, e.g. `"  "`, used to lay out a member or element added to an empty object or array,
     * which has no sibling to copy the indentation from. Without it, the addition stays on the same line: `{"a": 1}`,
     * `["a"]`. The `*At` functions default it to the document's {@link detectIndent} when given a document laid out
     * over several lines.
     */
    indent?: string;
    /** How deep the object or array is nested, the root being 0. Used together with {@link indent}. */
    depth?: number;
}

export interface SetMemberOptions extends LayoutOptions {
    /**
     * Where a new member goes: `"last"` (the default), `"first"`, or `"sorted"`, which places it
     * before the first member whose key sorts after it, the way npm keeps its dependency sections.
     * A member that already exists keeps its place.
     */
    position?: "first" | "last" | "sorted";
}

export interface AppendElementOptions extends LayoutOptions {
    /** Whether to leave the array unchanged if one of its elements already holds the same data as the value. */
    unique?: boolean;
}

export interface SetAtOptions extends SetMemberOptions {
    /**
     * Where the objects that {@link createMissing} creates go among their siblings: `"last"` (the default),
     * `"first"`, or `"sorted"`. {@link SetMemberOptions.position} applies to the last key of the path only.
     */
    parentPosition?: "first" | "last" | "sorted";
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
export function isEmptyContainer(container: Json.Object | Json.Array): boolean {
    const elements: Json.RightPadded<Json>[] = isObject(container) ? container.members : container.values;
    return elements.length === 0 || (elements.length === 1 && elements[0].element.kind === Json.Kind.Empty);
}

/**
 * The members of an object, without the `Json.Empty` of an empty object or a trailing comma.
 */
export function membersOf(obj: Json.Object): Json.Member[] {
    return obj.members.map(m => m.element).filter(isMember);
}

/**
 * The first member of an object with the given key, matched by {@link getMemberKeyName}.
 */
export function findMember(obj: Json.Object, key: string): Json.Member | undefined {
    return membersOf(obj).find(m => getMemberKeyName(m) === key);
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
        current = findMember(current, key)?.value;
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
        for (const m of membersOf(json)) {
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
 * The member keys from the document root down to the cursor, or `undefined` if a key on the way can't be read.
 * Array elements on the way contribute no step.
 */
export function pathOf(cursor: Cursor): string[] | undefined {
    const path: string[] = [];
    for (const value of cursor.asArray()) {
        if (value?.kind === Json.Kind.Member) {
            const key = getMemberKeyName(value as Json.Member);
            if (key === undefined) {
                return undefined;
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
 * To change only the formatting or markers of a value, use {@link updateMember}, which keeps the prefix it is given.
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

    const present = presentIndexes(obj.members);
    const insertAt = options.position === "first" ? 0 :
        options.position === "sorted" ? sortedIndex(obj.members, present, key) :
            present.length;
    const neighbour = present.length === 0 ? undefined :
        obj.members[present[Math.min(insertAt, present.length - 1)]].element as Json.Member;
    const added = newMember(key, value, emptySpace, neighbour?.key.after ?? emptySpace, space(neighbour?.value.prefix.whitespace ?? " "));
    return {...obj, members: insertIntoList(obj.members, insertAt, added, options)};
}

/**
 * Replaces the value of an existing member with what `fn` makes of it. A value that `fn` derives from the old one
 * keeps the prefix `fn` gives it; a new value without a prefix is placed after the same space as the old value.
 */
export function updateMember(obj: Json.Object, key: string, fn: (old: Json.Value) => Json.Value): Json.Object {
    const index = indexOfMember(obj, key);
    if (index < 0) {
        return obj;
    }
    const existing = obj.members[index].element as Json.Member;
    const value = fn(existing.value);
    if (value === existing.value) {
        return obj;
    }
    return withElementAt(obj, index, {...existing, value: placedAs(value, existing.value)});
}

/**
 * Removes a member. The space before the closing brace stays where it is, a removed first member's
 * indentation passes to the next one, and an object left without members holds a single `Json.Empty`.
 * A comment on the line before the member stays, and one on the member's own line goes with it.
 * An object left without members prints as `{}` unless a comment remains in it.
 */
export function removeMember(obj: Json.Object, key: string): Json.Object {
    const index = indexOfMember(obj, key);
    if (index < 0) {
        return obj;
    }
    return {...obj, members: removeFromList(obj.members, index)};
}

/**
 * The elements of an array, without the `Json.Empty` of an empty array or a trailing comma.
 */
export function elementsOf(arr: Json.Array): Json.Value[] {
    return arr.values.map(v => v.element).filter(v => v.kind !== Json.Kind.Empty);
}

/**
 * Adds an element after the last one. It copies its indentation from a sibling; in an array without elements,
 * it is laid out by {@link LayoutOptions.indent} and {@link LayoutOptions.depth}, or stays inline: `["a"]`.
 */
export function appendElement(arr: Json.Array, value: Json.Value, options: AppendElementOptions = {}): Json.Array {
    const elements = elementsOf(arr);
    if (options.unique && elements.some(e => sameData(e, value))) {
        return arr;
    }
    return withValues(arr, insertIntoList(arr.values, elements.length, value, options));
}

/**
 * Adds an element before the one at `index`, or after the last one if `index` is past the end.
 */
export function insertElement(arr: Json.Array, index: number, value: Json.Value, options: LayoutOptions = {}): Json.Array {
    const count = elementsOf(arr).length;
    return withValues(arr, insertIntoList(arr.values, Math.max(0, Math.min(index, count)), value, options));
}

/**
 * Removes the elements that `pred` matches, with the same handling of whitespace and comments as {@link removeMember}.
 * An array left without elements prints as `[]` unless a comment remains in it.
 */
export function removeElements(arr: Json.Array, pred: (value: Json.Value, index: number) => boolean): Json.Array {
    return mapElements(arr, (v, i) => pred(v, i) ? undefined : v);
}

/**
 * Replaces each element with what `fn` makes of it, or removes it where `fn` returns `undefined`.
 * Replacing and removing in one pass covers e.g. replacing the first match and dropping the others.
 * A value that `fn` derives from the old one keeps the prefix `fn` gives it; a new value without a prefix takes the
 * old element's place. Elements are compared by reference: return the element itself to leave it unchanged.
 */
export function mapElements(arr: Json.Array, fn: (value: Json.Value, index: number) => Json.Value | undefined): Json.Array {
    const removed: Json.Value[] = [];
    let index = 0;
    let values: Json.RightPadded<Json>[] = arr.values.map(padded => {
        const old = padded.element;
        if (old.kind === Json.Kind.Empty) {
            return padded;
        }
        const value = fn(old, index++);
        if (value === undefined) {
            removed.push(old);
            return padded;
        }
        return value === old ? padded : {...padded, element: placedAs(value, old)};
    });
    for (const old of removed) {
        values = removeFromList(values, values.findIndex(p => p.element.id === old.id));
    }
    return values.every((p, i) => p === arr.values[i]) && values.length === arr.values.length ? arr : withValues(arr, values);
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
export function jsonLiteral(value: string | number | boolean | null, prefix: Json.Space = emptySpace): Json.Literal {
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
        return jsonLiteral(value);
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

function withValues(arr: Json.Array, values: Json.RightPadded<Json>[]): Json.Array {
    return {...arr, values: values as Json.RightPadded<Json.Value>[]};
}

function presentIndexes(list: Json.RightPadded<Json>[]): number[] {
    return list.map((_, i) => i).filter(i => list[i].element.kind !== Json.Kind.Empty);
}

/**
 * Inserts an element into the members of an object or the values of an array, before the `insertAt`-th element
 * that isn't `Json.Empty`, or after the last one. The element's leading space comes from where it is placed.
 */
function insertIntoList(list: Json.RightPadded<Json>[], insertAt: number, added: Json, options: LayoutOptions): Json.RightPadded<Json>[] {
    const present = presentIndexes(list);
    if (present.length === 0) {
        const closing = list.length > 0 ? list[list.length - 1].after : emptySpace;
        const layout = emptyLayout(closing, options);
        return [rightPadded<Json>(withLeadingSpace(added, space(layout.element)), layout.closing)];
    }

    const following = followingWhitespace(list, present);
    const placed = withLeadingSpace(added, space(following));
    const elements = [...list];
    if (insertAt < present.length) {
        const at = present[insertAt];
        const next = elements[at].element;
        const nextSpace = leadingSpace(next);
        const {ending, rest} = splitAtLineEnd(nextSpace);
        if (ending.length > 0) {
            elements[at] = {...elements[at], element: withLeadingSpace(next, rest)};
            elements.splice(at, 0, rightPadded<Json>(withLeadingSpace(placed, withLeadingComments(nextSpace.whitespace, ending, space(following))), emptySpace));
        } else if (insertAt === 0) {
            elements[at] = {...elements[at], element: withLeadingWhitespace(next, following)};
            elements.splice(at, 0, rightPadded<Json>(withLeadingSpace(placed, space(leadingWhitespace(next))), emptySpace));
        } else {
            elements.splice(at, 0, rightPadded<Json>(placed, emptySpace));
        }
    } else {
        const last = present[present.length - 1];
        if (last === elements.length - 1) {
            const {trailing, closing} = splitTrailingComment(elements[last].after, following);
            elements.push(rightPadded<Json>(trailing ? withLeadingSpace(placed, trailing) : placed, closing));
            elements[last] = {...elements[last], after: emptySpace};
        } else {
            elements.splice(last + 1, 0, rightPadded<Json>(placed, emptySpace));
        }
    }
    return elements;
}

/**
 * Removes the element at `index` from the members of an object or the values of an array. The space before the
 * closing bracket stays where it is, a removed first element's indentation passes to the next one, a comment on the
 * line before the element stays and one on the element's own line goes with it. A list left without elements holds
 * a single `Json.Empty`, which prints without space unless a comment remains.
 */
function removeFromList(list: Json.RightPadded<Json>[], index: number): Json.RightPadded<Json>[] {
    const removed = list[index];
    const removedSpace = leadingSpace(removed.element);
    const kept = commentsEndingPreviousLine(removedSpace);
    const keep = (to: Json.Space) => withLeadingComments(removedSpace.whitespace, kept, splitAtLineEnd(to).rest);
    const elements = list.filter((_, i) => i !== index);

    if (presentIndexes(elements).length === 0) {
        const closing = keep(elements.length > 0 ? elements[elements.length - 1].after : removed.after);
        return [rightPadded<Json>(emptyElement(), closing.comments.length === 0 ? emptySpace : closing)];
    }
    if (index === elements.length) {
        elements[index - 1] = {...elements[index - 1], after: keep(removed.after)};
    } else if (elements[index].element.kind === Json.Kind.Empty) {
        elements[index] = {...elements[index], after: keep(elements[index].after)};
    } else {
        const next = withLeadingSpace(elements[index].element, keep(leadingSpace(elements[index].element)));
        const element = kept.length === 0 && index === 0 ? withLeadingWhitespace(next, leadingWhitespace(removed.element)) : next;
        elements[index] = {...elements[index], element};
    }
    return elements;
}

/**
 * A replacement value as `fn` gave it if it derives from the old one, otherwise after the old value's prefix
 * unless it brings its own.
 */
function placedAs(value: Json.Value, old: Json.Value): Json.Value {
    return value.id !== old.id && isEmptySpace(value.prefix) ? withPrefix(value, old.prefix) : value;
}

function indexOfMember(obj: Json.Object, key: string): number {
    return obj.members.findIndex(m => isMember(m.element) && getMemberKeyName(m.element) === key);
}

function withElementAt(obj: Json.Object, index: number, element: Json.Member): Json.Object {
    const elements = [...obj.members];
    elements[index] = {...elements[index], element};
    return {...obj, members: elements};
}

function isEmptySpace(s: Json.Space): boolean {
    return s.whitespace === "" && s.comments.length === 0;
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
        key: rightPadded<Json.Key>(jsonLiteral(key), keyAfter),
        value: withPrefix(value, valuePrefix)
    };
}

/**
 * Whether two values hold the same data: literals that decode to the same value, arrays with the same elements
 * in the same order, and objects with the same members in any order.
 */
function sameData(a: Json.Value, b: Json.Value): boolean {
    if (a === b) {
        return true;
    }
    if (isLiteral(a) && isLiteral(b)) {
        const plain = toPlain(a);
        return plain === toPlain(b) && (typeof plain !== "number" || sameInteger(a.source, b.source));
    }
    if (isArray(a) && isArray(b)) {
        const as = a.values.map(v => v.element).filter(v => v.kind !== Json.Kind.Empty);
        const bs = b.values.map(v => v.element).filter(v => v.kind !== Json.Kind.Empty);
        return as.length === bs.length && as.every((v, i) => sameData(v, bs[i]));
    }
    if (isObject(a) && isObject(b)) {
        const as = membersOf(a);
        const bs = membersOf(b);
        return as.length === bs.length && as.every(m => {
            const key = getMemberKeyName(m);
            const other = key === undefined ? undefined : findMember(b, key);
            return other !== undefined && sameData(m.value, other.value);
        });
    }
    return false;
}

/**
 * Integers beyond double precision decode to the same number, so their sources are compared exactly.
 */
function sameInteger(a: string, b: string): boolean {
    const integer = /^-?\d+$/;
    return !integer.test(a) || !integer.test(b) || BigInt(a) === BigInt(b);
}

/**
 * The indentation of a member that isn't the first, which may differ from the first one's in an object
 * laid out on one line.
 */
function followingWhitespace(list: Json.RightPadded<Json>[], present: number[]): string {
    if (present.length > 1) {
        const second = leadingSpace(list[present[1]].element);
        return lastLineBreak(second) ?? second.whitespace;
    }
    return lastLineBreak(leadingSpace(list[present[0]].element)) ?? " ";
}

/**
 * The line break and indentation that the last line of a space starts with, looking past its comments.
 */
function lastLineBreak(s: Json.Space): string | undefined {
    const text = [s.whitespace, ...s.comments.map(c => c.suffix)].filter(t => t.includes("\n")).pop();
    return text === undefined ? undefined : "\n" + text.substring(text.lastIndexOf("\n") + 1);
}

function sortedIndex(list: Json.RightPadded<Json>[], present: number[], key: string): number {
    const index = present.findIndex(i => {
        const m = list[i].element;
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

/**
 * The comments at the start of a member's prefix that end the line before it, such as one after the previous
 * member's comma. They describe what comes before the member, so they stay when it is removed.
 */
function commentsEndingPreviousLine(prefix: Json.Space): Json.Comment[] {
    if (prefix.whitespace.includes("\n")) {
        return [];
    }
    const end = prefix.comments.findIndex(c => c.suffix.includes("\n"));
    return end < 0 ? [] : prefix.comments.slice(0, end + 1);
}

function splitAtLineEnd(s: Json.Space): { ending: Json.Comment[], rest: Json.Space } {
    const ending = commentsEndingPreviousLine(s);
    if (ending.length === 0) {
        return {ending, rest: s};
    }
    return {
        ending,
        rest: {kind: Json.Kind.Space, whitespace: ending[ending.length - 1].suffix, comments: s.comments.slice(ending.length)}
    };
}

function withLeadingComments(whitespace: string, comments: Json.Comment[], to: Json.Space): Json.Space {
    if (comments.length === 0) {
        return to;
    }
    const last = comments.length - 1;
    return {
        kind: Json.Kind.Space,
        whitespace,
        comments: [...comments.slice(0, last), {...comments[last], suffix: to.whitespace}, ...to.comments]
    };
}

function emptyLayout(closing: Json.Space, options: LayoutOptions): { element: string, closing: Json.Space } {
    const lineBreak = lastLineBreak(closing);
    if (lineBreak !== undefined) {
        return {element: lineBreak + (options.indent ?? "    "), closing};
    }
    if (options.indent !== undefined) {
        const base = "\n" + options.indent.repeat(options.depth ?? 0);
        return {element: base + options.indent, closing: space(base)};
    }
    return {element: "", closing};
}

/**
 * LSTs built before the parsers put a member's indentation on the member (#8644) have it on the key.
 */
function indentationOnKey(m: Json.Member): boolean {
    return m.prefix.whitespace === "" && m.prefix.comments.length === 0 && m.key.element.prefix.whitespace !== "";
}

/**
 * The space an element starts with: its prefix, or a member's key's prefix where the indentation is on the key.
 */
function leadingSpace(element: Json): Json.Space {
    return isMember(element) && indentationOnKey(element) ? element.key.element.prefix : element.prefix;
}

function withLeadingSpace<T extends Json>(element: T, prefix: Json.Space): T {
    if (leadingSpace(element) === prefix) {
        return element;
    }
    if (isMember(element) && indentationOnKey(element)) {
        return {...element, key: {...element.key, element: {...element.key.element, prefix}}};
    }
    return {...element, prefix};
}

function leadingWhitespace(element: Json): string {
    return leadingSpace(element).whitespace;
}

function withLeadingWhitespace<T extends Json>(element: T, whitespace: string): T {
    const prefix = leadingSpace(element);
    return prefix.whitespace === whitespace ? element : withLeadingSpace(element, {...prefix, whitespace});
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
    const first = membersOf(obj)[0];
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
    const child = findMember(obj, key)?.value;
    if (child === undefined) {
        return options.createMissing ?
            setMember(obj, key, setIn(emptyObject(), rest, value, nested), {...options, position: options.parentPosition ?? "last"}) :
            obj;
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
    const child = findMember(obj, key)?.value;
    if (child === undefined || !isObject(child)) {
        return obj;
    }
    const updated = removeIn(child, rest, pruneEmptyParents);
    if (updated === child) {
        return obj;
    }
    return pruneEmptyParents && isEmptyContainer(updated) ? removeMember(obj, key) : updateMember(obj, key, () => updated);
}
