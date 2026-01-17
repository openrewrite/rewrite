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
import {Markers} from "./markers";
import {UUID} from "./uuid";
import {PlainText} from "./text";
import {RpcCodecs} from "./rpc";

export const TreeKind = {
    Checksum: "org.openrewrite.Checksum",
    FileAttributes: "org.openrewrite.FileAttributes"
} as const

export interface Tree {
    readonly kind: string

    /**
     * Use randomId() to generate a unique id for a tree.
     */
    readonly id: UUID

    /**
     * It is possible to use EmptyMarkers here to represent no markers.
     */
    readonly markers: Markers
}

export function isTree(tree: any): tree is Tree {
    return (
        typeof tree === "object" &&
        tree !== null &&
        "id" in tree && "markers" in tree
    );
}

export function isScope(a: Tree, b: Tree): boolean {
    return a !== undefined && b !== undefined && (a === b || a.id === b.id);
}

export class Cursor {
    private _messages?: Map<string | symbol, any>;

    constructor(public readonly value: any, public readonly parent?: Cursor) {
    }

    get messages(): Map<string | symbol, any> {
        if (!this._messages) {
            this._messages = new Map<string, any>();
        }
        return this._messages;
    }

    getNearestMessage<T>(key: string): any {
        const t = this._messages == undefined ? undefined : this._messages.get(key);
        return t == null && this.parent != null ? this.parent.getNearestMessage<T>(key) : t;
    }

    asArray(): any[] {
        const path: any[] = [];
        let current: Cursor | undefined = this;
        while (current !== undefined) {
            path.push(current.value);
            current = current.parent;
        }
        return path;
    }

    parentTree(level: number = 1): Cursor | undefined {
        let c: Cursor | undefined = this.parent;
        let treeCount = 0;
        // Track the current value to skip duplicate cursors for the same object
        // This handles the case where visitRightPadded/visitLeftPadded creates a cursor
        // and then visitDefined creates another cursor for the same intersection-typed object
        const currentValue = this.value;
        while (c) {
            // Skip cursors with the same object reference as the current cursor
            // This prevents finding the "wrapper" cursor when looking for the true parent
            if (isTree(c.value) && c.value !== currentValue) {
                treeCount++;
                if (treeCount === level) {
                    return c;
                }
            }
            c = c.parent;
        }
        return undefined;
    }

    firstEnclosing<T>(match: (value: any) => value is T): T | undefined {
        let c: Cursor | undefined = this;
        while (c) {
            if (match(c.value)) {
                return c.value as T;
            }
            c = c.parent;
        }
        return undefined;
    }

    get root(): Cursor {
        let root: Cursor = this;
        while (root.parent) {
            root = root.parent;
        }
        return root;
    }
}

export function rootCursor(): Cursor {
    return new Cursor("root");
}

export interface Checksum {
    kind: typeof TreeKind.Checksum
    readonly algorithm: string,
    readonly value: ArrayBuffer
}

export interface FileAttributes {
    kind: typeof TreeKind.FileAttributes
    readonly creationDate?: Date
    readonly lastModifiedTime?: Date
    readonly lastAccessTime?: Date
    readonly isReadable: boolean
    readonly isWritable: boolean
    readonly isExecutable: boolean
    readonly size: number
}

export function isSourceFile(tree: any): tree is SourceFile {
    return isTree(tree) && "sourcePath" in tree
}

export interface SourceFile extends Tree {
    sourcePath: string
    charsetName?: string
    charsetBomMarked?: boolean
    checksum?: Checksum
    fileAttributes?: FileAttributes
}
