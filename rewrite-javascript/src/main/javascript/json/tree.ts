import {Markers, SourceFile, Tree} from "../";

export const JsonKind = {
    Array: "org.openrewrite.json.tree.Json$Array",
    Document: "org.openrewrite.json.tree.Json$Document",
    Empty: "org.openrewrite.json.tree.Json$Empty",
    Identifier: "org.openrewrite.json.tree.Json$Identifier",
    Literal: "org.openrewrite.json.tree.Json$Literal",
    Member: "org.openrewrite.json.tree.Json$Member",
    Object: "org.openrewrite.json.tree.Json$Object",
    Space: "org.openrewrite.json.tree.Space",
    Comment: "org.openrewrite.json.tree.Comment",
    RightPadded: "org.openrewrite.json.tree.JsonRightPadded"
} as const

export type JsonKey = Identifier | Literal

export type JsonValue = JsonArray | Empty | JsonObject

const jsonKindValues = new Set(Object.values(JsonKind));

export function isJson(tree: any): tree is Json {
    return jsonKindValues.has(tree["kind"]);
}

export interface Space {
    readonly kind: typeof JsonKind.Space
    readonly comments: Comment[]
    readonly whitespace: string
}

export function space(whitespace: string): Space {
    return {
        kind: JsonKind.Space,
        comments: [],
        whitespace: whitespace
    }
}

export const EmptySpace: Space = {
    kind: JsonKind.Space,
    comments: [],
    whitespace: ""
}

export interface Comment {
    readonly kind: typeof JsonKind.Comment
    readonly multiline: boolean
    readonly text: string
    readonly suffix: string
    readonly markers: Markers
}

export interface Json extends Tree {
    readonly prefix: Space
}

export interface JsonArray extends Json {
    readonly kind: typeof JsonKind.Array
    readonly values: JsonRightPadded<JsonValue>[]
}

export interface Document extends SourceFile, Json {
    readonly kind: typeof JsonKind.Document
    readonly value: JsonValue
    readonly eof: Space
}

export interface Empty extends Json {
    readonly kind: typeof JsonKind.Empty
}

export interface Identifier extends Json {
    readonly kind: typeof JsonKind.Identifier
    readonly name: string
}

export interface Literal extends Json {
    readonly kind: typeof JsonKind.Literal
    readonly source: string
    readonly value?: Object
}

export interface Member extends Json {
    readonly kind: typeof JsonKind.Member
    readonly key: JsonRightPadded<JsonKey>
    readonly value: JsonValue
}

export interface JsonObject extends Json {
    readonly kind: typeof JsonKind.Object
    readonly members: JsonRightPadded<Json>[]
}

export interface JsonRightPadded<T extends Json> {
    readonly kind: typeof JsonKind.RightPadded
    readonly element: T
    readonly after: Space
    readonly markers: Markers
}
