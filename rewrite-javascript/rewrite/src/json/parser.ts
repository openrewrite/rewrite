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
import {emptyMarkers} from "../markers";
import {Parser, ParserInput, ParserSourceReader} from "../parser";
import {randomId} from "../uuid";
import {SourceFile} from "../tree";
import {emptySpace, Json, space} from "./tree";

export class JsonParser extends Parser {

    async *parse(...sourcePaths: ParserInput[]): AsyncGenerator<SourceFile> {
        for (const sourcePath of sourcePaths) {
            yield {
                ...new ParseJsonReader(sourcePath).parse(),
                sourcePath: this.relativePath(sourcePath)
            };
        }
    }
}

class ParseJsonReader extends ParserSourceReader {
    constructor(sourcePath: ParserInput) {
        super(sourcePath);
    }

    private prefix() {
        return space(this.whitespace());
    }

    parse(): Omit<Json.Document, "sourcePath"> {
        return {
            kind: Json.Kind.Document,
            id: randomId(),
            prefix: this.prefix(),
            markers: emptyMarkers,
            value: this.json(JSON.parse(this.source)) as Json.Value,
            eof: space(this.source.slice(this.cursor))
        }
    }

    private json(parsed: any): Json {
        const base = {
            id: randomId(),
            prefix: this.prefix(),
            markers: emptyMarkers
        }
        if (Array.isArray(parsed)) {
            this.cursor++; // skip '['
            return {
                kind: Json.Kind.Array,
                ...base,
                values: parsed.map(p => {
                    const value = {
                        kind: Json.Kind.RightPadded,
                        element: this.json(p) as Json.Value,
                        after: space(this.whitespace()),
                        markers: emptyMarkers
                    } satisfies Json.RightPadded<Json.Value> as Json.RightPadded<Json.Value>;
                    this.cursor++;
                    return value;
                })
            } satisfies Json.Array as Json.Array;
        } else if (parsed !== null && typeof parsed === "object") {
            this.cursor++; // skip '{'
            return {
                kind: Json.Kind.Object,
                ...base,
                members: Object.keys(parsed).map(key => {
                    const member = {
                        kind: Json.Kind.RightPadded,
                        element: this.member(parsed, key),
                        after: space(this.whitespace()),
                        markers: emptyMarkers
                    } satisfies Json.RightPadded<Json.Member> as Json.RightPadded<Json.Member>;
                    this.cursor++;
                    return member;
                })
            } satisfies Json.Object as Json.Object;
        } else if (typeof parsed === "string") {
            this.cursor += parsed.length + 2;
            return {
                kind: Json.Kind.Literal,
                ...base,
                source: `"${parsed}"`,
                value: parsed
            } satisfies Json.Literal as Json.Literal;
        } else if (typeof parsed === "number") {
            this.cursor += parsed.toString().length;
            return {
                kind: Json.Kind.Literal,
                ...base,
                source: parsed.toString(),
                value: parsed,
            } satisfies Json.Literal as Json.Literal;
        } else {
            throw new Error(`Unsupported JSON type: ${parsed}`);
        }
    }

    private member(parsed: any, key: string) {
        return {
            kind: Json.Kind.Member,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            key: {
                kind: Json.Kind.RightPadded,
                markers: emptyMarkers,
                element: this.json(key) as Json.Key,
                after: space(this.sourceBefore(":")),
            } satisfies Json.RightPadded<Json.Key> as Json.RightPadded<Json.Key>,
            value: this.json(parsed[key]) as Json.Value
        } satisfies Json.Member as Json.Member;
    }
}
