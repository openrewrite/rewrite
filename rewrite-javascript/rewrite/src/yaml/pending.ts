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
import {isMapping, isMappingEntry, Yaml} from "./tree";

/**
 * Some edits also change the prefix of the node printed after the edited one: a `|` block scalar and an empty value
 * print the line break that ends them, and a comment at the end of an entry's line is in the prefix of what follows
 * it. When that node isn't in the same mapping or sequence, the edit is recorded on the edited node and applied by
 * whichever parent places it next to what follows: the editing functions in `edit.ts`, `YamlVisitor`, or the document.
 */
export type PendingEdit = (node: Yaml, next: string) => { node: Yaml, next: string };

const pendingEdits = new WeakMap<Yaml, PendingEdit[]>();

export type Container = Yaml.Mapping | Yaml.Sequence;
export type ContainerEntry = Yaml.MappingEntry | Yaml.SequenceEntry;

export function withPending<T extends Yaml>(node: T, ...edits: PendingEdit[]): T {
    if (edits.length > 0) {
        pendingEdits.set(node, [...(pendingEdits.get(node) ?? []), ...edits]);
    }
    return node;
}

/**
 * Moves the edits recorded on a node to the node rebuilt from it.
 */
export function carryPending<T extends Yaml>(from: Yaml, to: T): T {
    const edits = from === to ? undefined : pendingEdits.get(from);
    if (edits) {
        pendingEdits.delete(from);
        withPending(to, ...edits);
    }
    return to;
}

/**
 * Applies the edits recorded on a node, given the prefix of the node printed after it.
 */
export function applyPending(node: Yaml, next: string): { node: Yaml, next: string } {
    const edits = pendingEdits.get(node);
    if (!edits) {
        return {node, next};
    }
    pendingEdits.delete(node);
    let result = {node, next};
    for (const edit of edits) {
        result = edit(result.node, result.next);
    }
    return result;
}

export function childOf(entry: ContainerEntry): Yaml.Block {
    return isMappingEntry(entry) ? entry.value : entry.block;
}

export function withChild<E extends ContainerEntry>(entry: E, child: Yaml.Block): E {
    if (childOf(entry) === child) {
        return entry;
    }
    return (isMappingEntry(entry) ? {...entry, value: child} : {...entry, block: child}) as E;
}

export function closingOf(container: Container): string | undefined {
    return isMapping(container) ? container.closingBracePrefix : container.closingBracketPrefix;
}

export function withClosing<T extends Container>(container: T, closing: string | undefined): T {
    if (closingOf(container) === closing) {
        return container;
    }
    return carryPending(container, (isMapping(container) ?
        {...container, closingBracePrefix: closing} :
        {...container, closingBracketPrefix: closing}) as T);
}

export function withEntries<T extends Container>(container: T, entries: ContainerEntry[]): T {
    return carryPending(container, {...container, entries} as T);
}

/**
 * Applies the edits recorded on the children of a mapping or sequence to the entries after them. The edits of
 * the last child of a block mapping or sequence are recorded on the result instead, for its own parent to apply.
 */
export function settleChildren<T extends Container>(container: T): T {
    const entries: ContainerEntry[] = [...container.entries];
    let closing = closingOf(container);
    let escaping: PendingEdit | undefined;
    let changed = false;
    for (let i = 0; i < entries.length; i++) {
        const child = childOf(entries[i]);
        if (!pendingEdits.has(child)) {
            continue;
        }
        changed = true;
        if (i + 1 < entries.length) {
            const result = applyPending(child, entries[i + 1].prefix);
            entries[i] = withChild(entries[i], result.node as Yaml.Block);
            entries[i + 1] = {...entries[i + 1], prefix: result.next};
        } else if (closing !== undefined) {
            const result = applyPending(child, closing);
            entries[i] = withChild(entries[i], result.node as Yaml.Block);
            closing = result.next;
        } else {
            const id = entries[i].id;
            const edits = pendingEdits.get(child)!;
            pendingEdits.delete(child);
            escaping = (node, next) => {
                const parent = node as Container;
                const index = parent.entries.findIndex(e => e.id === id);
                if (index < 0) {
                    return {node, next};
                }
                let result: { node: Yaml, next: string } = {node: childOf(parent.entries[index]), next};
                for (const edit of edits) {
                    result = edit(result.node, result.next);
                }
                const updated: ContainerEntry[] = [...parent.entries];
                updated[index] = withChild(updated[index], result.node as Yaml.Block);
                return {node: {...parent, entries: updated} as Container, next: result.next};
            };
        }
    }
    if (!changed) {
        return container;
    }
    const result = withClosing(withEntries(container, entries), closing);
    return escaping ? withPending(result, escaping) : result;
}

/**
 * Applies the edits recorded on a document's block to the space before the document's end.
 */
export function settleDocument(document: Yaml.Document): Yaml.Document {
    const result = applyPending(document.block, document.end.prefix);
    if (result.node === document.block && result.next === document.end.prefix) {
        return document;
    }
    return {
        ...document,
        block: result.node as Yaml.Block,
        end: result.next === document.end.prefix ? document.end : {...document.end, prefix: result.next}
    };
}
