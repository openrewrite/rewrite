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
import {emptyMarkers, Parser, ParserInput, ParserSourceReader, randomId} from "../";
import {
    emptySpace,
    Json,
    JsonArray,
    JsonDocument,
    JsonKind,
    JsonObject,
    JsonRightPadded,
    JsonValue,
    Literal,
    Member,
    space
} from "./tree";

export class JsonParser extends Parser<JsonDocument> {

    async parse(...sourcePaths: ParserInput[]): Promise<JsonDocument[]> {
        return sourcePaths.map(sourcePath => {
            return {
                ...new ParseJsonReader(sourcePath).parse(),
                sourcePath: this.relativePath(sourcePath)
            };
        });
    }
}

class ParseJsonReader extends ParserSourceReader {
    constructor(sourcePath: ParserInput) {
        super(sourcePath);
    }

    private prefix() {
        return space(this.whitespace());
    }

    parse(): Omit<JsonDocument, "sourcePath"> {
        return {
            kind: JsonKind.Document,
            id: randomId(),
            prefix: this.prefix(),
            markers: emptyMarkers,
            value: this.json(JSON.parse(this.source)) as JsonValue,
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
                kind: JsonKind.Array,
                ...base,
                values: parsed.map(p => {
                    const value = {
                        kind: JsonKind.RightPadded,
                        element: this.json(p),
                        after: space(this.whitespace())
                    };
                    this.cursor++;
                    return value;
                })
            } as JsonArray;
        } else if (parsed !== null && typeof parsed === "object") {
            this.cursor++; // skip '{'
            return {
                kind: JsonKind.Object,
                ...base,
                members: Object.keys(parsed).map(key => {
                    const member = {
                        kind: JsonKind.RightPadded,
                        element: this.member(parsed, key),
                        after: space(this.whitespace())
                    } as JsonRightPadded<Member>;
                    this.cursor++;
                    return member;
                })
            } as JsonObject;
        } else if (typeof parsed === "string") {
            this.cursor += parsed.length + 2;
            return {
                kind: JsonKind.Literal,
                ...base,
                source: `"${parsed}"`,
                value: parsed
            } as Literal;
        } else if (typeof parsed === "number") {
            this.cursor += parsed.toString().length;
            return {
                kind: JsonKind.Literal,
                ...base,
                source: parsed.toString(),
                value: parsed.toString(),
            } as Literal;
        } else {
            throw new Error(`Unsupported JSON type: ${parsed}`);
        }
    }

    private member(parsed: any, key: string) {
        return {
            kind: JsonKind.Member,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            key: {
                kind: JsonKind.RightPadded,
                markers: emptyMarkers,
                element: this.json(key),
                after: space(this.sourceBefore(":")),
            },
            value: this.json(parsed[key])
        } as Member;
    }
}
