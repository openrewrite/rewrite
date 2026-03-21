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
import {emptyMarkers, Markers} from "../markers";
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
        RightPadded: "org.openrewrite.json.tree.JsonRightPadded"
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

// Cache for Space objects - reuse for common whitespace patterns (flyweight pattern)
const spaceCache = new Map<string, Json.Space>();
const SPACE_CACHE_MAX_LENGTH = 100;

export const emptySpace: Json.Space = {
    kind: Json.Kind.Space,
    comments: [],
    whitespace: ""
};

const singleSpace: Json.Space = {
    kind: Json.Kind.Space,
    comments: [],
    whitespace: " "
};

/**
 * Parse a formatting string into a Space object, extracting comments.
 * Mirrors the Java Space.format() method.
 */
export function space(formatting: string): Json.Space {
    if (formatting.length === 0) {
        return emptySpace;
    }
    if (formatting === " ") {
        return singleSpace;
    }

    // Check cache first for short strings
    const cacheable = formatting.length <= SPACE_CACHE_MAX_LENGTH;
    if (cacheable) {
        const cached = spaceCache.get(formatting);
        if (cached !== undefined) {
            return cached;
        }
    }

    // No comment markers means simple whitespace-only Space
    if (!formatting.includes('/')) {
        const result: Json.Space = {
            kind: Json.Kind.Space,
            comments: [],
            whitespace: formatting
        };
        if (cacheable) {
            spaceCache.set(formatting, result);
        }
        return result;
    }

    // Parse comments out of the formatting string
    let prefix = '';
    let comment = '';
    const comments: Json.Comment[] = [];

    let inSingleLineComment = false;
    let inMultiLineComment = false;
    let last = '';

    for (let i = 0; i < formatting.length; i++) {
        const c = formatting[i];

        if (c === '/') {
            if (inSingleLineComment) {
                comment += c;
            } else if (last === '/' && !inMultiLineComment) {
                // Start of single-line comment
                inSingleLineComment = true;
                comment = '';
                prefix = prefix.slice(0, -1); // Remove the first '/'
            } else if (last === '*' && inMultiLineComment && comment.length > 0) {
                // End of multi-line comment
                inMultiLineComment = false;
                comment = comment.slice(0, -1); // Trim the last '*'
                comments.push({
                    kind: Json.Kind.Comment,
                    multiline: true,
                    text: comment,
                    suffix: prefix.slice(0, -1),
                    markers: emptyMarkers
                });
                prefix = '';
                comment = '';
                continue;
            } else if (inMultiLineComment) {
                comment += c;
            } else {
                prefix += c;
            }
        } else if (c === '\r' || c === '\n') {
            if (inSingleLineComment) {
                // End of single-line comment
                inSingleLineComment = false;
                comments.push({
                    kind: Json.Kind.Comment,
                    multiline: false,
                    text: comment,
                    suffix: prefix,
                    markers: emptyMarkers
                });
                prefix = c;
                comment = '';
            } else if (!inMultiLineComment) {
                prefix += c;
            } else {
                comment += c;
            }
        } else if (c === '*') {
            if (inSingleLineComment) {
                comment += c;
            } else if (last === '/' && !inMultiLineComment) {
                // Start of multi-line comment
                inMultiLineComment = true;
                comment = '';
            } else {
                comment += c;
            }
        } else {
            if (inSingleLineComment || inMultiLineComment) {
                comment += c;
            } else {
                prefix += c;
            }
        }
        last = c;
    }

    // If a file ends with a single-line comment there may be no terminating newline
    if (comment.length > 0 || inSingleLineComment) {
        comments.push({
            kind: Json.Kind.Comment,
            multiline: false,
            text: comment,
            suffix: prefix,
            markers: emptyMarkers
        });
        prefix = '';
    }

    // Shift the whitespace on each comment forward to be a suffix of the comment before it,
    // and the whitespace on the first comment to be the whitespace of the tree element.
    // The remaining prefix is the suffix of the last comment.
    let whitespace = prefix;
    if (comments.length > 0) {
        for (let i = comments.length - 1; i >= 0; i--) {
            const c = comments[i];
            const next = c.suffix;
            comments[i] = {
                ...c,
                suffix: whitespace
            };
            whitespace = next;
        }
    }

    const result: Json.Space = {
        kind: Json.Kind.Space,
        comments,
        whitespace
    };
    if (cacheable) {
        spaceCache.set(formatting, result);
    }
    return result;
}

const jsonKindValues = new Set(Object.values(Json.Kind));

export function isJson(tree: any): tree is Json {
    return jsonKindValues.has(tree["kind"]);
}

export function isLiteral(json: Json): json is Json.Literal {
    return json.kind === Json.Kind.Literal;
}

export function isObject(json: Json): json is Json.Object {
    return json.kind === Json.Kind.Object;
}

export function isArray(json: Json): json is Json.Array {
    return json.kind === Json.Kind.Array;
}

export function isIdentifier(json: Json): json is Json.Identifier {
    return json.kind === Json.Kind.Identifier;
}

export function isMember(json: Json): json is Json.Member {
    return json.kind === Json.Kind.Member;
}

/**
 * Gets the key name from a Json.Member.
 * Handles both string literals (quoted keys) and identifiers (unquoted keys).
 *
 * @param member The JSON member to extract the key name from
 * @returns The key name as a string, or undefined if extraction fails
 */
export function getMemberKeyName(member: Json.Member): string | undefined {
    const key = member.key.element;
    if (isLiteral(key)) {
        const source = key.source;
        // Remove quotes from string literal
        if (source.startsWith('"') && source.endsWith('"')) {
            return source.slice(1, -1);
        }
        return source;
    } else if (isIdentifier(key)) {
        return key.name;
    }
    return undefined;
}

/**
 * Detects the indentation used in a JSON document by examining existing members or array elements.
 * Returns the base indent string (e.g., "  " for 2 spaces, "    " for 4 spaces).
 *
 * @param doc The JSON document to analyze
 * @returns The detected indent string, or "    " (4 spaces) as default
 */
export function detectIndent(doc: Json.Document): string {
    const defaultIndent = '    '; // Default to 4 spaces

    if (isObject(doc.value)) {
        if (doc.value.members && doc.value.members.length > 0) {
            // Look at the prefix of the first member's key to detect indentation
            const firstMemberRightPadded = doc.value.members[0];
            const firstMember = firstMemberRightPadded.element as Json.Member;
            const prefix = firstMember.key.element.prefix.whitespace;
            // Extract just the spaces/tabs after the newline
            const match = prefix.match(/\n([ \t]+)/);
            if (match) {
                return match[1];
            }
        }
    } else if (isArray(doc.value)) {
        if (doc.value.values && doc.value.values.length > 0) {
            // Look at the prefix of the first array element to detect indentation
            const firstElement = doc.value.values[0].element;
            const prefix = firstElement.prefix.whitespace;
            // Extract just the spaces/tabs after the newline
            const match = prefix.match(/\n([ \t]+)/);
            if (match) {
                return match[1];
            }
        }
    }

    return defaultIndent;
}

/**
 * Creates a RightPadded wrapper for a JSON element.
 *
 * @param element The JSON element to wrap
 * @param after The trailing space after the element
 * @param markers Optional markers to attach
 * @returns A RightPadded wrapper containing the element
 */
export function rightPadded<T extends Json>(element: T, after: Json.Space, markers?: Markers): Json.RightPadded<T> {
    return {
        kind: Json.Kind.RightPadded,
        element,
        after,
        markers: markers ?? emptyMarkers
    };
}
