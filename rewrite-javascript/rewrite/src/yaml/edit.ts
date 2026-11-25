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
import {parseDocument} from "yaml";
import {emptyMarkers} from "../markers";
import {randomId} from "../uuid";
import {Cursor} from "../tree";
import {isMapping, isScalar, isSequence, Yaml} from "./tree";
import {
    carryPending,
    childOf,
    closingOf,
    Container,
    ContainerEntry,
    PendingEdit,
    settleChildren,
    settleDocument,
    withChild,
    withClosing,
    withEntries,
    withPending
} from "./pending";

/**
 * Immutable helpers that edit YAML while keeping its formatting and comments. An edit that changes nothing returns
 * its input.
 *
 * An edit can also change the prefix of the node after the edited one. When that node is outside the edited
 * mapping or sequence, the change is applied when the result is placed in its parent, by these helpers or by
 * `YamlVisitor`.
 */

export type YamlRoot = Yaml.Documents | Yaml.Document | Yaml.Block;

export type YamlPrimitive = string | number | boolean | null;

/**
 * A node, or plain data for a scalar.
 */
export type YamlValue = Yaml.Block | YamlPrimitive;

export interface SetEntryOptions {
    /** Where a new entry goes; `"sorted"` places it by key. Defaults to `"last"`. */
    position?: "first" | "last" | "sorted";
}

export interface AppendItemOptions {
    /** Whether to skip a value that an item already holds. */
    unique?: boolean;
}

/**
 * The key of an entry, decoded if quoted, or `undefined` for an alias.
 */
export function entryKey(entry: Yaml.MappingEntry): string | undefined {
    const key = entry.key;
    if (!isScalar(key)) {
        return undefined;
    }
    if (key.style === Yaml.ScalarStyle.PLAIN) {
        return key.value;
    }
    const value = scalarPlain(key);
    return typeof value === "string" ? value : undefined;
}

/**
 * The first entry with the given key.
 */
export function findEntry(mapping: Yaml.Mapping, key: string): Yaml.MappingEntry | undefined {
    return mapping.entries.find(e => entryKey(e) === key);
}

export function itemsOf(seq: Yaml.Sequence): Yaml.Block[] {
    rememberItemColumns(seq);
    return seq.entries.map(e => e.block);
}

/**
 * The value at a path of keys, or `undefined` if the path doesn't resolve. Reads the first of several documents.
 */
export function valueAt(root: YamlRoot, path: readonly string[]): Yaml.Block | undefined {
    let current = blockOf(root);
    for (const key of path) {
        if (current === undefined || !isMapping(current)) {
            return undefined;
        }
        current = findEntry(current, key)?.value;
    }
    return current;
}

/**
 * The plain value of a node under the YAML 1.2 core schema, with aliases and `<<` merges resolved, or `undefined`
 * where it isn't plain data, such as a custom tag. Reads the first of several documents.
 */
export function toPlain(yaml: Yaml): unknown {
    return plainOf(yaml, new Map());
}

/**
 * The keys from the root down to the cursor, skipping sequence items, or `undefined` if a key can't be read.
 */
export function pathOf(cursor: Cursor): string[] | undefined {
    const path: string[] = [];
    for (const value of cursor.asArray()) {
        if (value?.kind === Yaml.Kind.MappingEntry) {
            const key = entryKey(value as Yaml.MappingEntry);
            if (key === undefined) {
                return undefined;
            }
            path.unshift(key);
        }
    }
    return path;
}

/**
 * A scalar, unquoted where it reads back as the same value.
 */
export function yamlScalar(value: YamlPrimitive, prefix: string = ""): Yaml.Scalar {
    const {style, text} = written(value, Yaml.ScalarStyle.PLAIN);
    return newScalar(style, text, prefix);
}

/**
 * Sets a scalar's value, keeping its quoting where the value allows. A multi-line string keeps a block scalar, and a
 * core schema tag such as `!!str` is dropped.
 */
export function setScalarValue(scalar: Yaml.Scalar, value: YamlPrimitive): Yaml.Scalar {
    if (scalarPlain(scalar) === value) {
        return scalar;
    }
    const dropTag = scalar.tag !== undefined && isCoreTag(scalar.tag);
    const tag = dropTag ? undefined : scalar.tag;
    const prefix = (isEmptyValue(scalar) ? " " : scalar.prefix) + (dropTag ? scalar.tag!.prefix : "");
    if (typeof value === "string" && value.includes("\n") &&
        (scalar.style === Yaml.ScalarStyle.LITERAL || scalar.style === Yaml.ScalarStyle.FOLDED)) {
        const text = literalText(scalar, value);
        if (text !== undefined) {
            return replaced(scalar, {...scalar, prefix, tag, style: Yaml.ScalarStyle.LITERAL, value: text});
        }
    }
    const {style, text} = written(value, scalar.style);
    return replaced(scalar, {...scalar, prefix, tag, style, value: text});
}

/**
 * Adds an entry laid out like its neighbour, or sets the value of an existing one.
 */
export function setEntry(mapping: Yaml.Mapping, key: string, value: YamlValue, options: SetEntryOptions = {}): Yaml.Mapping {
    const index = indexOfEntry(mapping, key);
    if (index >= 0) {
        return replaceChild(mapping, index, valueFor(mapping.entries[index].value, value));
    }
    const at = options.position === "first" ? 0 :
        options.position === "sorted" ? sortedIndex(mapping, key) :
            mapping.entries.length;
    return settleChildren(insertInto(mapping, at, newEntry(mapping, at, key, value)));
}

/**
 * Replaces the value of an entry with what `fn` makes of it.
 */
export function updateEntry(mapping: Yaml.Mapping, key: string, fn: (old: Yaml.Block) => YamlValue): Yaml.Mapping {
    const index = indexOfEntry(mapping, key);
    if (index < 0) {
        return mapping;
    }
    const old = mapping.entries[index].value;
    return replaceChild(mapping, index, valueFor(old, fn(old)));
}

/**
 * Removes an entry with the comment on its line. A block mapping left empty becomes `{}`.
 */
export function removeEntry(mapping: Yaml.Mapping, key: string): Yaml.Mapping {
    const index = indexOfEntry(mapping, key);
    return index < 0 ? mapping : removeFrom(mapping, index);
}

/**
 * Replaces each item with what `fn` makes of it, or removes it where `fn` returns `undefined`. A block sequence left
 * empty becomes `[]`.
 */
export function mapItems(seq: Yaml.Sequence, fn: (item: Yaml.Block, index: number) => YamlValue | undefined): Yaml.Sequence {
    rememberItemColumns(seq);
    const entries: ContainerEntry[] = [...seq.entries];
    const removed: number[] = [];
    seq.entries.forEach((entry, i) => {
        const result = fn(entry.block, i);
        if (result === undefined) {
            removed.push(i);
        } else {
            entries[i] = withChild(entry, valueFor(entry.block, result));
        }
    });
    let updated = entries.every((e, i) => e === seq.entries[i]) ? seq : settleChildren(withEntries(seq, entries));
    for (const i of removed.reverse()) {
        updated = removeFrom(updated, i);
    }
    return updated;
}

export function removeItems(seq: Yaml.Sequence, pred: (item: Yaml.Block, index: number) => boolean): Yaml.Sequence {
    return mapItems(seq, (item, i) => pred(item, i) ? undefined : item);
}

/**
 * Adds an item laid out like the last one.
 */
export function appendItem(seq: Yaml.Sequence, value: YamlValue, options: AppendItemOptions = {}): Yaml.Sequence {
    const block = isNode(value) ? value : yamlScalar(value);
    if (options.unique && seq.entries.some(e => sameData(e.block, block))) {
        return seq;
    }
    const flow = isFlow(seq);
    const sibling = seq.entries[seq.entries.length - 1]?.block;
    const itemPrefix = flow ? "" : sibling !== undefined && sibling.prefix !== "" && !sibling.prefix.includes("\n") ? sibling.prefix : " ";
    const entry: Yaml.SequenceEntry = {
        kind: Yaml.Kind.SequenceEntry,
        id: randomId(),
        prefix: "",
        markers: emptyMarkers,
        block: withPrefixIfEmpty(block, itemPrefix),
        dash: !flow,
        trailingCommaPrefix: undefined
    };
    return settleChildren(insertInto(seq, seq.entries.length, entry));
}

/**
 * {@link setEntry} at a path of keys. Edits the first of several documents.
 */
export function setAt<T extends YamlRoot>(root: T, path: readonly string[], value: YamlValue, options: SetEntryOptions = {}): T {
    return editRoot(root, path, mapping => setIn(mapping, path, value, options));
}

/**
 * {@link updateEntry} at a path of keys.
 */
export function updateAt<T extends YamlRoot>(root: T, path: readonly string[], fn: (old: Yaml.Block) => YamlValue): T {
    return editRoot(root, path, mapping => updateIn(mapping, path, fn));
}

/**
 * {@link removeEntry} at a path of keys.
 */
export function removeAt<T extends YamlRoot>(root: T, path: readonly string[]): T {
    return editRoot(root, path, mapping => removeIn(mapping, path));
}

function blockOf(root: YamlRoot): Yaml.Block | undefined {
    if (root.kind === Yaml.Kind.Documents) {
        return (root as Yaml.Documents).documents[0]?.block;
    }
    return root.kind === Yaml.Kind.Document ? (root as Yaml.Document).block : root as Yaml.Block;
}

function editRoot<T extends YamlRoot>(root: T, path: readonly string[], edit: (mapping: Yaml.Mapping) => Yaml.Mapping): T {
    if (path.length === 0) {
        return root;
    }
    if (root.kind === Yaml.Kind.Documents) {
        const documents = root as Yaml.Documents;
        const first = documents.documents[0];
        if (first === undefined) {
            return root;
        }
        const document = editRoot(first, path, edit);
        return document === first ? root : {...documents, documents: [document, ...documents.documents.slice(1)]} as T;
    }
    if (root.kind === Yaml.Kind.Document) {
        const document = root as Yaml.Document;
        if (!isMapping(document.block)) {
            return root;
        }
        const block = edit(document.block);
        return block === document.block ? root : settleDocument({...document, block}) as T;
    }
    return isMapping(root as Yaml) ? edit(root as Yaml.Mapping) as T : root;
}

function setIn(mapping: Yaml.Mapping, path: readonly string[], value: YamlValue, options: SetEntryOptions): Yaml.Mapping {
    const [key, ...rest] = path;
    if (rest.length === 0) {
        return setEntry(mapping, key, value, options);
    }
    const child = findEntry(mapping, key)?.value;
    return child !== undefined && isMapping(child) ? updateEntry(mapping, key, () => setIn(child, rest, value, options)) : mapping;
}

function updateIn(mapping: Yaml.Mapping, path: readonly string[], fn: (old: Yaml.Block) => YamlValue): Yaml.Mapping {
    const [key, ...rest] = path;
    if (rest.length === 0) {
        return updateEntry(mapping, key, fn);
    }
    return updateEntry(mapping, key, child => isMapping(child) ? updateIn(child, rest, fn) : child);
}

function removeIn(mapping: Yaml.Mapping, path: readonly string[]): Yaml.Mapping {
    const [key, ...rest] = path;
    if (rest.length === 0) {
        return removeEntry(mapping, key);
    }
    const child = findEntry(mapping, key)?.value;
    return child !== undefined && isMapping(child) ? updateEntry(mapping, key, () => removeIn(child, rest)) : mapping;
}

function plainOf(yaml: Yaml, anchors: Map<string, unknown>): unknown {
    let value: unknown;
    switch (yaml.kind) {
        case Yaml.Kind.Documents: {
            const first = (yaml as Yaml.Documents).documents[0];
            return first === undefined ? undefined : plainOf(first, anchors);
        }
        case Yaml.Kind.Document:
            return plainOf((yaml as Yaml.Document).block, anchors);
        case Yaml.Kind.Alias:
            return anchors.get((yaml as Yaml.Alias).anchor.key);
        case Yaml.Kind.Scalar:
            value = scalarPlain(yaml as Yaml.Scalar);
            break;
        case Yaml.Kind.Sequence: {
            const items = (yaml as Yaml.Sequence).entries.map(e => plainOf(e.block, anchors));
            value = items.includes(undefined) ? undefined : items;
            break;
        }
        case Yaml.Kind.Mapping:
            value = mappingPlain(yaml as Yaml.Mapping, anchors);
            break;
        default:
            return undefined;
    }
    const anchor = (yaml as Yaml.Scalar | Yaml.Sequence | Yaml.Mapping).anchor;
    if (anchor !== undefined && value !== undefined) {
        anchors.set(anchor.key, value);
    }
    return value;
}

function mappingPlain(mapping: Yaml.Mapping, anchors: Map<string, unknown>): Record<string, unknown> | undefined {
    const merged: Record<string, unknown>[] = [];
    const own: [string, unknown][] = [];
    for (const entry of mapping.entries) {
        const key = entryKey(entry);
        const value = key === undefined ? undefined : plainOf(entry.value, anchors);
        if (key === undefined || value === undefined) {
            return undefined;
        }
        if (key === "<<" && isScalar(entry.key) && entry.key.style === Yaml.ScalarStyle.PLAIN) {
            const sources = Array.isArray(value) ? value : [value];
            if (!sources.every(isPlainObject)) {
                return undefined;
            }
            merged.push(...sources);
        } else {
            own.push([key, value]);
        }
    }
    // The mapping's own keys win over merged ones, and an earlier merge source over a later one.
    let result: Record<string, unknown> = {};
    for (const source of merged.reverse()) {
        result = {...result, ...source};
    }
    return {...result, ...Object.fromEntries(own)};
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

const decodedScalars = new WeakMap<Yaml.Scalar, unknown>();

function scalarPlain(scalar: Yaml.Scalar): unknown {
    if (decodedScalars.has(scalar)) {
        return decodedScalars.get(scalar);
    }
    const value = decode(scalar.style, scalar.value, scalar.tag);
    decodedScalars.set(scalar, value);
    return value;
}

const CORE_TAGS = new Set(["str", "int", "float", "bool", "null"]);

function isCoreTag(tag: Yaml.Tag): boolean {
    return tag.tagKind === Yaml.TagKind.IMPLICIT_GLOBAL && CORE_TAGS.has(tag.name);
}

/**
 * Decodes a scalar with the `yaml` package, which handles escapes, folding and the core schema's types.
 */
function decode(style: Yaml.ScalarStyle, value: string, tag?: Yaml.Tag): unknown {
    if (tag !== undefined && !isCoreTag(tag)) {
        return undefined;
    }
    if (tag === undefined && style === Yaml.ScalarStyle.PLAIN) {
        if (/^[A-Za-z_][\w\-\/.]*$/.test(value) && !/^(?:true|True|TRUE|false|False|FALSE|null|Null|NULL)$/.test(value)) {
            return value;
        }
        if (/^-?(?:0|[1-9]\d{0,14})$/.test(value)) {
            return Number(value);
        }
    }
    let source: string;
    switch (style) {
        case Yaml.ScalarStyle.DOUBLE_QUOTED:
            source = `"${value}"`;
            break;
        case Yaml.ScalarStyle.SINGLE_QUOTED:
            source = `'${value}'`;
            break;
        case Yaml.ScalarStyle.LITERAL:
        case Yaml.ScalarStyle.FOLDED: {
            const lineBreak = value.indexOf("\n");
            // An indentation indicator is relative to the parent, which isn't known here.
            if (/\d/.test((lineBreak < 0 ? value : value.slice(0, lineBreak)).replace(/#.*$/, ""))) {
                return undefined;
            }
            source = (style === Yaml.ScalarStyle.LITERAL ? "|" : ">") + value;
            break;
        }
        default:
            source = value;
    }
    const document = parseDocument(tag === undefined ? source : `!!${tag.name} ${source}`);
    return document.errors.length > 0 || document.warnings.length > 0 ? undefined : document.toJS();
}

function newScalar(style: Yaml.ScalarStyle, value: string, prefix: string): Yaml.Scalar {
    return {
        kind: Yaml.Kind.Scalar,
        id: randomId(),
        prefix,
        markers: emptyMarkers,
        style,
        anchor: undefined,
        tag: undefined,
        value
    };
}

function written(value: YamlPrimitive, preferred: Yaml.ScalarStyle): { style: Yaml.ScalarStyle, text: string } {
    if (typeof value !== "string") {
        return {style: Yaml.ScalarStyle.PLAIN, text: plainText(value)!};
    }
    if (preferred === Yaml.ScalarStyle.SINGLE_QUOTED && !/[\r\n]/.test(value)) {
        return {style: Yaml.ScalarStyle.SINGLE_QUOTED, text: value.replace(/'/g, "''")};
    }
    if (preferred !== Yaml.ScalarStyle.DOUBLE_QUOTED) {
        const plain = plainText(value);
        if (plain !== undefined) {
            return {style: Yaml.ScalarStyle.PLAIN, text: plain};
        }
    }
    return {style: Yaml.ScalarStyle.DOUBLE_QUOTED, text: JSON.stringify(value).slice(1, -1)};
}

function plainText(value: YamlPrimitive): string | undefined {
    if (value === null || typeof value === "boolean") {
        return String(value);
    }
    if (typeof value === "number") {
        if (!Number.isFinite(value)) {
            throw new RangeError(`${value} can't be written as a YAML scalar`);
        }
        return String(value);
    }
    return isPlainSafe(value) && decode(Yaml.ScalarStyle.PLAIN, value) === value ? value : undefined;
}

/**
 * Whether a string can be written unquoted, in a block or flow collection.
 */
function isPlainSafe(s: string): boolean {
    return s.length > 0 &&
        !/^[\s,\[\]{}#&*!|>'"%@`]/.test(s) &&
        !/^[-?:](\s|$)/.test(s) &&
        !/[\r\n\t,\[\]{}]/.test(s) &&
        !/: |:$| #|\s$/.test(s);
}

/**
 * The source of a `|` block scalar for `value`, laid out like `old`, or `undefined` if it can't be written simply.
 */
function literalText(old: Yaml.Scalar, value: string): string | undefined {
    const lineBreak = old.value.indexOf("\n");
    if (lineBreak < 0 || value === "" || value.endsWith("\n\n") || /^[ \t]/.test(value)) {
        return undefined;
    }
    const comment = old.value.slice(0, lineBreak).replace(/^[-+]/, "");
    const indent = /^([ \t]+)\S/m.exec(old.value.slice(lineBreak + 1))?.[1];
    if (indent === undefined || /^\d/.test(comment)) {
        return undefined;
    }
    const eol = eolOf(old.value);
    const content = value.endsWith("\n") ? value.slice(0, -1) : value;
    const text = (value.endsWith("\n") ? "" : "-") + comment.replace(/\r$/, "") + eol +
        content.split("\n").map(line => line === "" ? "" : indent + line).join(eol) + tailOf(old);
    return decode(Yaml.ScalarStyle.LITERAL, text) === value ? text : undefined;
}

function isNode(value: YamlValue | undefined): value is Yaml.Block {
    return typeof value === "object" && value !== null;
}

function isFlow(container: Container): boolean {
    return closingOf(container) !== undefined;
}

function isBlockCollection(node: Yaml): boolean {
    return (isMapping(node) || isSequence(node)) && !isFlow(node);
}

/**
 * An empty value, as in `key:`. LSTs built before #10155 have the line break after it in its prefix.
 */
function isEmptyValue(node: Yaml): boolean {
    return isScalar(node) && node.style === Yaml.ScalarStyle.PLAIN && node.value === "" &&
        node.anchor === undefined && node.tag === undefined;
}

/**
 * The line breaks a node ends with that belong before the next node: those after a block scalar or an empty value.
 */
function tailOf(node: Yaml): string {
    if (isScalar(node)) {
        if (node.style === Yaml.ScalarStyle.LITERAL || node.style === Yaml.ScalarStyle.FOLDED) {
            // The Java parser keeps the blank lines after a block scalar in it.
            return /(\r?\n)+$/.exec(node.value)?.[0] ?? "";
        }
        return isEmptyValue(node) ? node.prefix : "";
    }
    if (isBlockCollection(node)) {
        const entries = (node as Container).entries;
        return entries.length === 0 ? "" : tailOf(childOf(entries[entries.length - 1]));
    }
    return "";
}

function tailAt(container: Container): string {
    return isFlow(container) || container.entries.length === 0 ? "" : tailOf(childOf(container.entries[container.entries.length - 1]));
}

function strip(tail: string, space: string): string {
    return tail !== "" && space.startsWith(tail) ? space.slice(tail.length) : space;
}

function lastLine(space: string): string {
    return space.substring(space.lastIndexOf("\n") + 1);
}

function lines(space: string): { head: string, mid: string[], last: string } {
    const parts = space.split(/\r?\n/);
    return {head: parts[0], mid: parts.slice(1, -1), last: parts[parts.length - 1]};
}

/**
 * The line break the spaces use.
 */
function eolOf(...spaces: string[]): string {
    return spaces.some(s => s.includes("\r\n")) ? "\r\n" : "\n";
}

function lineBreakAt(space: string): number {
    return /\r?\n/.exec(space)?.index ?? -1;
}

const isNotBlank = (line: string) => line.trim() !== "";

/**
 * The space left when a node between `before` and `after` is removed. The comment on the removed node's line goes;
 * other comments stay.
 */
function joinGaps(before: string, after: string, first: boolean): string {
    if (!after.includes("\n")) {
        return before;
    }
    const eol = eolOf(before, after);
    const a = lines(after);
    const afterLines = first ? a.mid.filter(isNotBlank) : a.mid;
    if (!before.includes("\n")) {
        return afterLines.length === 0 ? before :
            before + [afterLines[0].trimStart(), ...afterLines.slice(1), a.last].join(eol);
    }
    const b = lines(before);
    const beforeLines = first ? b.mid : b.mid.filter(isNotBlank);
    return [b.head, ...beforeLines, ...afterLines, a.last].join(eol);
}

function withPrefixIfEmpty<T extends Yaml.Block>(block: T, prefix: string): T {
    return block.prefix === "" && !isBlockCollection(block) ? carryPending(block, {...block, prefix}) : block;
}

/**
 * Places a new value where an old one was. A value derived from the old one is kept as it is.
 */
function placedAs<T extends Yaml.Block>(value: T, old: Yaml.Block): T {
    if (value.id === old.id) {
        return value;
    }
    return replaced(old, withPrefixIfEmpty(value, isEmptyValue(old) || isBlockCollection(old) ? " " : old.prefix));
}

function replaced<T extends Yaml>(old: Yaml, value: T): T {
    const before = tailOf(old);
    if (value === old || before === tailOf(value)) {
        return value;
    }
    return withPending(value, (node, next) => ({node, next: strip(tailOf(node), before + next)}));
}

function valueFor(old: Yaml.Block, value: YamlValue): Yaml.Block {
    if (!isNode(value)) {
        return isScalar(old) ? setScalarValue(old, value) : placedAs(yamlScalar(value), old);
    }
    if (value === old || (value.id !== old.id && sameData(old, value))) {
        return old;
    }
    return placedAs(value, old);
}

function sameData(a: Yaml.Block, b: Yaml.Block): boolean {
    if (a === b) {
        return true;
    }
    const plainA = toPlain(a);
    return plainA !== undefined && deepEqual(plainA, toPlain(b));
}

function deepEqual(a: unknown, b: unknown): boolean {
    if (a === b) {
        return true;
    }
    if (Array.isArray(a) && Array.isArray(b)) {
        return a.length === b.length && a.every((v, i) => deepEqual(v, b[i]));
    }
    if (isPlainObject(a) && isPlainObject(b)) {
        const keys = Object.keys(a);
        return keys.length === Object.keys(b).length && keys.every(k => Object.prototype.hasOwnProperty.call(b, k) && deepEqual(a[k], b[k]));
    }
    return false;
}

function indexOfEntry(mapping: Yaml.Mapping, key: string): number {
    return mapping.entries.findIndex(e => entryKey(e) === key);
}

function sortedIndex(mapping: Yaml.Mapping, key: string): number {
    const index = mapping.entries.findIndex(e => {
        const name = entryKey(e);
        return name !== undefined && name.localeCompare(key, "en") > 0;
    });
    return index < 0 ? mapping.entries.length : index;
}

function replaceChild<T extends Container>(container: T, index: number, child: Yaml.Block): T {
    const entries: ContainerEntry[] = [...container.entries];
    const entry = withChild(entries[index], child);
    if (entry === entries[index]) {
        return container;
    }
    entries[index] = entry;
    return settleChildren(withEntries(container, entries));
}

function newEntry(mapping: Yaml.Mapping, at: number, key: string, value: YamlValue): Yaml.MappingEntry {
    const sibling = mapping.entries[Math.min(at, mapping.entries.length - 1)];
    const siblingValue = sibling?.value;
    const valuePrefix = siblingValue !== undefined && !isBlockCollection(siblingValue) && !isEmptyValue(siblingValue) &&
    !siblingValue.prefix.includes("\n") ? siblingValue.prefix : " ";
    return {
        kind: Yaml.Kind.MappingEntry,
        id: randomId(),
        prefix: "",
        markers: emptyMarkers,
        key: isPlainSafe(key) ? newScalar(Yaml.ScalarStyle.PLAIN, key, "") : yamlScalar(key),
        beforeMappingValueIndicator: sibling?.beforeMappingValueIndicator ?? "",
        value: withPrefixIfEmpty(isNode(value) ? value : yamlScalar(value), valuePrefix)
    };
}

/**
 * The columns of collections that start on their item's dash line, which a single entry doesn't show.
 */
const itemColumns = new WeakMap<Container, number>();

function rememberItemColumns(seq: Yaml.Sequence): void {
    seq.entries.forEach((entry, i) => {
        const item = entry.block;
        if (!(isMapping(item) || isSequence(item)) || isFlow(item) || item.anchor !== undefined || item.tag !== undefined ||
            item.entries.length === 0 || item.entries[0].prefix.includes("\n")) {
            return;
        }
        const space = (i > 0 ? tailOf(childOf(seq.entries[i - 1])) : "") + entry.prefix;
        const dash = space.includes("\n") ? lastLine(space).length :
            i === 0 ? itemColumns.get(seq) ?? (space === "" ? 0 : undefined) : undefined;
        if (dash !== undefined) {
            itemColumns.set(item, dash + 1 + item.prefix.length + item.entries[0].prefix.length);
        }
    });
}

/**
 * The line break and indentation for a new entry of a block collection.
 */
function lineIndentOf(container: Container): string {
    for (let i = container.entries.length - 1; i >= 0; i--) {
        const space = (i > 0 ? tailOf(childOf(container.entries[i - 1])) : "") + container.entries[i].prefix;
        if (space.includes("\n")) {
            return eolOf(space) + lastLine(space);
        }
    }
    const column = itemColumns.get(container);
    if (column !== undefined) {
        return "\n" + " ".repeat(column);
    }
    if (container.entries[0]?.prefix === "") {
        return "\n";
    }
    throw new Error("Can't tell how far to indent a new entry of a mapping or sequence that starts on the line of " +
        "its item's dash. Get it from the sequence with itemsOf or mapItems, which record its indentation.");
}

function insertInto<T extends Container>(container: T, at: number, entry: ContainerEntry): T {
    if (isFlow(container)) {
        return (isMapping(container) ?
            insertIntoFlowMapping(container, at, entry as Yaml.MappingEntry) :
            insertIntoFlowSequence(container as Yaml.Sequence, at, entry as Yaml.SequenceEntry)) as T;
    }
    const entries: ContainerEntry[] = [...container.entries];
    const lineIndent = lineIndentOf(container);
    if (at >= entries.length) {
        const lastTail = tailOf(childOf(entries[entries.length - 1]));
        const added = {...entry, prefix: strip(lastTail, lineIndent)};
        const result = withEntries(container, [...entries, added]);
        // The old last entry's line break now precedes the added one, so what follows needs one too.
        return withPending(result, lastTail === "" ? keepLineComment(added.id) :
            (node, next) => ({node, next: strip(tailAt(node as Container), lastTail + next)}));
    }
    const previousTail = at > 0 ? tailOf(childOf(entries[at - 1])) : "";
    const space = previousTail + entries[at].prefix;
    const addedTail = tailOf(childOf(entry));
    let added: ContainerEntry;
    let nextPrefix: string;
    if (space.includes("\n")) {
        const {head, mid, last} = lines(space);
        const eol = eolOf(space);
        added = {...entry, prefix: strip(previousTail, head + eol + last)};
        nextPrefix = strip(addedTail, ["", ...mid, last].join(eol));
    } else {
        added = {...entry, prefix: strip(previousTail, space)};
        nextPrefix = strip(addedTail, lineIndent);
    }
    entries[at] = {...entries[at], prefix: nextPrefix};
    entries.splice(at, 0, added);
    return withEntries(container, entries);
}

/**
 * Keeps the comment on the old last entry's line there, after an entry is appended.
 */
function keepLineComment(id: string): PendingEdit {
    return (node, next) => {
        const lineBreak = lineBreakAt(next);
        const container = node as Container;
        const index = container.entries.findIndex(e => e.id === id);
        if (lineBreak < 0 || next.slice(0, lineBreak).trim() === "" || index < 0) {
            return {node, next};
        }
        const entries: ContainerEntry[] = [...container.entries];
        entries[index] = {...entries[index], prefix: next.slice(0, lineBreak) + entries[index].prefix};
        return {node: {...container, entries} as Container, next: next.slice(lineBreak)};
    };
}

/**
 * The space after a comma in a flow collection.
 */
function flowSeparator(spaces: string[]): string {
    const sample = spaces[spaces.length - 1];
    if (sample.includes("\n")) {
        return eolOf(sample) + lastLine(sample);
    }
    return spaces.length > 1 ? sample : " ";
}

/**
 * Whether a flow sequence item can hold the space before it, as the Java parser puts it on a scalar or alias.
 */
function holdsLead(block: Yaml.Block): boolean {
    return isScalar(block) || block.kind === Yaml.Kind.Alias;
}

function leadOf(entry: Yaml.SequenceEntry): string {
    return entry.prefix + (holdsLead(entry.block) ? entry.block.prefix : "");
}

/**
 * Whether a flow sequence uses the Java parser's layout.
 */
function leadsOnItems(seq: Yaml.Sequence): boolean {
    return seq.entries.some(e => e.prefix === "" && holdsLead(e.block) && e.block.prefix !== "");
}

function withLead(entry: Yaml.SequenceEntry, lead: string, onItem: boolean): Yaml.SequenceEntry {
    const block = entry.block;
    if (!holdsLead(block)) {
        return {...entry, prefix: lead};
    }
    const itemPrefix = onItem ? lead : "";
    return {
        ...entry,
        prefix: onItem ? "" : lead,
        block: block.prefix === itemPrefix ? block : carryPending(block, {...block, prefix: itemPrefix})
    };
}

function insertIntoFlowSequence(seq: Yaml.Sequence, at: number, entry: Yaml.SequenceEntry): Yaml.Sequence {
    const entries = [...seq.entries];
    if (entries.length === 0) {
        return withEntries(seq, [withLead({...entry, trailingCommaPrefix: undefined}, "", false)]);
    }
    const onItem = leadsOnItems(seq);
    let closing = seq.closingBracketPrefix!;
    const separator = flowSeparator(entries.map(leadOf));
    const commaPrefix = entries.find(e => e.trailingCommaPrefix !== undefined)?.trailingCommaPrefix ?? "";
    if (at >= entries.length) {
        const last = entries[entries.length - 1];
        let lead = separator;
        const lineBreak = lineBreakAt(closing);
        if (lineBreak >= 0 && separator.includes("\n") && closing.slice(0, lineBreak).trim() !== "") {
            lead = closing.slice(0, lineBreak) + separator;
            closing = closing.slice(lineBreak);
        }
        entries[entries.length - 1] = {...last, trailingCommaPrefix: last.trailingCommaPrefix ?? commaPrefix};
        entries.push(withLead({...entry, trailingCommaPrefix: last.trailingCommaPrefix}, lead, onItem));
    } else if (at === 0) {
        entries.unshift(withLead({...entry, trailingCommaPrefix: commaPrefix}, leadOf(entries[0]), onItem));
        entries[1] = withLead(entries[1], separator, onItem);
    } else {
        entries.splice(at, 0, withLead({...entry, trailingCommaPrefix: commaPrefix}, separator, onItem));
    }
    return withClosing(withEntries(seq, entries), closing);
}

function splitComma(space: string): { before: string, after: string } | undefined {
    const comma = /^(\s*),/.exec(space);
    return comma ? {before: comma[1], after: space.slice(comma[0].length)} : undefined;
}

function afterComma(space: string): string {
    return splitComma(space)?.after ?? space;
}

function insertIntoFlowMapping(mapping: Yaml.Mapping, at: number, entry: Yaml.MappingEntry): Yaml.Mapping {
    const entries = [...mapping.entries];
    if (entries.length === 0) {
        return withEntries(mapping, [{...entry, prefix: ""}]);
    }
    const separator = "," + flowSeparator(entries.map((e, i) => i === 0 ? e.prefix : afterComma(e.prefix)));
    if (at >= entries.length) {
        entries.push({...entry, prefix: separator});
    } else if (at === 0) {
        entries.unshift({...entry, prefix: entries[0].prefix});
        entries[1] = {...entries[1], prefix: separator};
    } else {
        entries.splice(at, 0, {...entry, prefix: separator});
    }
    return withEntries(mapping, entries);
}

function removeFrom<T extends Container>(container: T, index: number): T {
    if (isFlow(container)) {
        return (isMapping(container) ?
            removeFromFlowMapping(container, index) :
            removeFromFlowSequence(container as Yaml.Sequence, index)) as T;
    }
    const entries: ContainerEntry[] = [...container.entries];
    const removed = entries[index];
    const previousTail = index > 0 ? tailOf(childOf(entries[index - 1])) : "";
    const before = previousTail + removed.prefix;
    const removedTail = tailOf(childOf(removed));
    const first = index === 0;
    entries.splice(index, 1);
    if (index < entries.length) {
        const next = entries[index];
        entries[index] = {...next, prefix: strip(previousTail, joinGaps(before, removedTail + next.prefix, first))};
        return withEntries(container, entries);
    }
    if (entries.length > 0) {
        return withPending(withEntries(container, entries), (node, next) =>
            ({node, next: strip(tailAt(node as Container), joinGaps(before, removedTail + next, first))}));
    }
    // What follows an emptied collection goes on the next line.
    return withPending(emptyFlow(container, before.includes("\n") ? " " : before), (node, next) =>
        ({node, next: joinGaps(before.includes("\n") ? before : eolOf(next), removedTail + next, true)}));
}

function emptyFlow<T extends Container>(container: T, opening: string): T {
    return carryPending(container, (isMapping(container) ?
        {...container, entries: [], openingBracePrefix: opening, closingBracePrefix: ""} :
        {...container, entries: [], openingBracketPrefix: opening, closingBracketPrefix: ""}) as T);
}

function removeFromFlowSequence(seq: Yaml.Sequence, index: number): Yaml.Sequence {
    const onItem = leadsOnItems(seq);
    const entries = [...seq.entries];
    const removed = entries[index];
    const removedLead = leadOf(removed);
    let closing = seq.closingBracketPrefix!;
    entries.splice(index, 1);
    if (entries.length === 0) {
        const space = joinGaps(removedLead, closing, true);
        closing = space.includes("#") ? space : "";
    } else if (index < entries.length) {
        entries[index] = withLead(entries[index], joinGaps(removedLead, leadOf(entries[index]), index === 0), onItem);
    } else {
        entries[index - 1] = {...entries[index - 1], trailingCommaPrefix: removed.trailingCommaPrefix};
        if (closing.includes("\n")) {
            closing = joinGaps(removedLead, closing, false);
        }
    }
    return withClosing(withEntries(seq, entries), closing);
}

function removeFromFlowMapping(mapping: Yaml.Mapping, index: number): Yaml.Mapping {
    const entries = [...mapping.entries];
    const removed = entries[index];
    const removedAfter = index === 0 ? removed.prefix : afterComma(removed.prefix);
    let closing = mapping.closingBracePrefix!;
    entries.splice(index, 1);
    if (entries.length === 0) {
        const space = joinGaps(removedAfter, afterComma(closing), true);
        closing = space.includes("#") ? space : "";
    } else if (index === 0) {
        entries[0] = {...entries[0], prefix: joinGaps(removed.prefix, afterComma(entries[0].prefix), true)};
    } else if (index < entries.length) {
        const comma = splitComma(removed.prefix)?.before ?? "";
        entries[index] = {...entries[index], prefix: comma + "," + joinGaps(removedAfter, afterComma(entries[index].prefix), false)};
    } else {
        // A trailing comma stays.
        const trailing = splitComma(closing);
        const after = trailing ? trailing.after : closing;
        const space = after.includes("\n") ? joinGaps(removedAfter, after, false) : after;
        closing = trailing ? (splitComma(removed.prefix)?.before ?? "") + "," + space : space;
    }
    return withClosing(withEntries(mapping, entries), closing);
}
