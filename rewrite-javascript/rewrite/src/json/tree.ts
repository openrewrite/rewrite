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
import {Markers} from "../markers";
import {SourceFile, Tree} from "../tree";


export interface Json extends Tree {
    readonly prefix: Json.Space
}

export namespace Json {
    export const Kind = {
        Array: "org.openrewrite.json.tree.Json$Array",
        Document: "org.openrewrite.json.tree.Json$Document",
        Empty: "org.openrewrite.json.tree.Json$Empty",
        Identifier: "org.openrewrite.json.tree.Json$Identifier",
        Literal: "org.openrewrite.json.tree.Json$Literal",
        Member: "org.openrewrite.json.tree.Json$Member",
        Object: "org.openrewrite.json.tree.Json$JsonObject",
        Space: "org.openrewrite.json.tree.Space",
        Comment: "org.openrewrite.json.tree.Comment",
        RightPadded: "org.openrewrite.json.tree.RightPadded"
    } as const

    export type Key = Identifier | Literal

    export type Value = Array | Empty | Json.Object | Literal

    export interface Comment {
        readonly kind: typeof Kind.Comment
        readonly multiline: boolean
        readonly text: string
        readonly suffix: string
        readonly markers: Markers
    }

    export interface Array extends Json {
        readonly kind: typeof Kind.Array
        readonly values: RightPadded<Value>[]
    }

    export interface Document extends SourceFile, Json {
        readonly kind: typeof Kind.Document
        readonly value: Value
        readonly eof: Space
    }

    export interface Empty extends Json {
        readonly kind: typeof Kind.Empty
    }

    export interface Identifier extends Json {
        readonly kind: typeof Kind.Identifier
        readonly name: string
    }

    export interface Literal extends Json {
        readonly kind: typeof Kind.Literal
        readonly source: string
        readonly value?: any
    }

    export interface Member extends Json {
        readonly kind: typeof Kind.Member
        readonly key: RightPadded<Key>
        readonly value: Value
    }

    export interface Object extends Json {
        readonly kind: typeof Kind.Object
        readonly members: RightPadded<Json>[]
    }

    export interface Space {
        readonly kind: typeof Kind.Space
        readonly comments: Comment[]
        readonly whitespace: string
    }

    export interface RightPadded<T extends Json> {
        readonly kind: typeof Json.Kind.RightPadded
        readonly element: T
        readonly after: Space
        readonly markers: Markers
    }
}

export function space(whitespace: string): Json.Space {
    return {
        kind: Json.Kind.Space,
        comments: [],
        whitespace: whitespace
    }
}

export const emptySpace: Json.Space = {
    kind: Json.Kind.Space,
    comments: [],
    whitespace: ""
}

const jsonKindValues = new Set(Object.values(Json.Kind));

export function isJson(tree: any): tree is Json {
    return jsonKindValues.has(tree["kind"]);
}
