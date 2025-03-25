import {Markers} from "./markers";
import {UUID} from "./uuid";

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

export class Cursor {
    private _messages?: Map<string, any>;

    constructor(public readonly value: any, public readonly parent?: Cursor) {
    }

    get messages(): Map<string, any> {
        if (!this._messages) {
            this._messages = new Map<string, any>();
        }
        return this._messages;
    }

    asArray(): any[] {
        const path: any[] = [];
        let current: Cursor | undefined = this;
        while (current !== undefined) {
            path.push(current.value());
            current = current.parent;
        }
        return path.reverse();
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
