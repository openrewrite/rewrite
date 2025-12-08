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
import {emptyMarkers, markers, MarkersKind, ParseExceptionResult} from "../markers";
import {Parser, ParserInput, parserInputRead, ParserSourceReader} from "../parser";
import {randomId} from "../uuid";
import {SourceFile} from "../tree";
import {emptySpace, Json, space} from "./tree";
import {ParseError, ParseErrorKind} from "../parse-error";

export class JsonParser extends Parser {

    async *parse(...sourcePaths: ParserInput[]): AsyncGenerator<SourceFile> {
        for (const sourcePath of sourcePaths) {
            try {
                yield {
                    ...new ParseJsonReader(sourcePath).parse(),
                    sourcePath: this.relativePath(sourcePath)
                };
            } catch (e: any) {
                // Return a ParseError for files that can't be parsed (e.g., JSONC with comments)
                const text = parserInputRead(sourcePath);
                const parseError: ParseError = {
                    kind: ParseErrorKind,
                    id: randomId(),
                    markers: markers({
                        kind: MarkersKind.ParseExceptionResult,
                        id: randomId(),
                        parserType: "JsonParser",
                        exceptionType: e.name || "Error",
                        message: e.message || "Unknown parse error"
                    } satisfies ParseExceptionResult as ParseExceptionResult),
                    sourcePath: this.relativePath(sourcePath),
                    text
                };
                yield parseError;
            }
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
            let values: Json.RightPadded<Json.Value>[];
            if (parsed.length === 0) {
                // Empty array - capture whitespace in an Empty element
                const afterWhitespace = this.whitespace();
                values = [{
                    kind: Json.Kind.RightPadded,
                    element: {
                        kind: Json.Kind.Empty,
                        id: randomId(),
                        prefix: emptySpace,
                        markers: emptyMarkers
                    } satisfies Json.Empty as Json.Empty,
                    after: space(afterWhitespace),
                    markers: emptyMarkers
                } satisfies Json.RightPadded<Json.Value> as Json.RightPadded<Json.Value>];
            } else {
                values = parsed.map((p, i) => {
                    const element = this.json(p) as Json.Value;
                    const afterWhitespace = this.whitespace();
                    // Check if there's a comma after this element
                    const hasComma = this.source[this.cursor] === ',';
                    if (hasComma) {
                        this.cursor++; // skip ','
                    }
                    return {
                        kind: Json.Kind.RightPadded,
                        element,
                        after: space(afterWhitespace),
                        markers: emptyMarkers
                    } satisfies Json.RightPadded<Json.Value> as Json.RightPadded<Json.Value>;
                });
            }
            this.cursor++; // skip ']'
            return {
                kind: Json.Kind.Array,
                ...base,
                values
            } satisfies Json.Array as Json.Array;
        } else if (parsed !== null && typeof parsed === "object") {
            this.cursor++; // skip '{'
            const keys = Object.keys(parsed);
            let members: Json.RightPadded<Json.Member>[];
            if (keys.length === 0) {
                // Empty object - capture whitespace in an Empty element
                const afterWhitespace = this.whitespace();
                members = [{
                    kind: Json.Kind.RightPadded,
                    element: {
                        kind: Json.Kind.Empty,
                        id: randomId(),
                        prefix: emptySpace,
                        markers: emptyMarkers
                    } satisfies Json.Empty as Json.Empty as unknown as Json.Member,
                    after: space(afterWhitespace),
                    markers: emptyMarkers
                } satisfies Json.RightPadded<Json.Member> as Json.RightPadded<Json.Member>];
            } else {
                members = keys.map((key, i) => {
                    const element = this.member(parsed, key);
                    const afterWhitespace = this.whitespace();
                    // Check if there's a comma after this element
                    const hasComma = this.source[this.cursor] === ',';
                    if (hasComma) {
                        this.cursor++; // skip ','
                    }
                    return {
                        kind: Json.Kind.RightPadded,
                        element,
                        after: space(afterWhitespace),
                        markers: emptyMarkers
                    } satisfies Json.RightPadded<Json.Member> as Json.RightPadded<Json.Member>;
                });
            }
            this.cursor++; // skip '}'
            return {
                kind: Json.Kind.Object,
                ...base,
                members
            } satisfies Json.Object as Json.Object;
        } else if (typeof parsed === "string") {
            // Extract original source to preserve escape sequences
            const sourceStart = this.cursor;
            this.cursor++; // skip opening quote
            while (this.cursor < this.source.length) {
                const char = this.source[this.cursor];
                if (char === '\\') {
                    this.cursor += 2; // skip escape sequence
                } else if (char === '"') {
                    this.cursor++; // skip closing quote
                    break;
                } else {
                    this.cursor++;
                }
            }
            const source = this.source.slice(sourceStart, this.cursor);
            return {
                kind: Json.Kind.Literal,
                ...base,
                source,
                value: parsed
            } satisfies Json.Literal as Json.Literal;
        } else if (typeof parsed === "number") {
            // Extract original source to preserve precision for large numbers
            const sourceStart = this.cursor;
            // Numbers can have optional sign, digits, decimal point, and exponent
            while (this.cursor < this.source.length) {
                const char = this.source[this.cursor];
                if (/[\d.eE+\-]/.test(char)) {
                    this.cursor++;
                } else {
                    break;
                }
            }
            const source = this.source.slice(sourceStart, this.cursor);
            return {
                kind: Json.Kind.Literal,
                ...base,
                source,
                value: parsed,
            } satisfies Json.Literal as Json.Literal;
        } else if (typeof parsed === "boolean") {
            const source = parsed ? "true" : "false";
            this.cursor += source.length;
            return {
                kind: Json.Kind.Literal,
                ...base,
                source,
                value: parsed,
            } satisfies Json.Literal as Json.Literal;
        } else if (parsed === null) {
            this.cursor += 4; // "null".length
            return {
                kind: Json.Kind.Literal,
                ...base,
                source: "null",
                value: null,
            } satisfies Json.Literal as Json.Literal;
        } else {
            throw new Error(`Unsupported JSON type: ${typeof parsed}`);
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
