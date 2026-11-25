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
 * A change to the prefix of the node after an edited one, such as the line break a block scalar ends with or the
 * comment on a removed entry's line. Recorded on the edited node, it is applied by the parent that places it.
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
 * Applies a node's edits, given the prefix of the node after it.
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
 * Applies the children's edits to the entries after them. The last child's edits in a block collection pass to the
 * result, for its parent to apply.
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
 * Applies the block's edits to the prefix of the document end.
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
