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
import {Markers, SourceFile, Tree, TreeKind} from "../";

export const JsonKind = {
    ...TreeKind,
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

export type JsonValue = JsonArray | Empty | JsonObject | Literal

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

export const emptySpace: Space = {
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

export interface JsonDocument extends SourceFile, Json {
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
