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
import {Parser, ParserInput, parserInputRead} from "../parser";
import {randomId} from "../uuid";
import {SourceFile} from "../tree";
import {emptySpace, Json, space} from "./tree";
import {ParseError, ParseErrorKind} from "../parse-error";
import {createScanner} from "jsonc-parser";

// Define token types locally to avoid const enum issues with isolatedModules
const Token = {
    OpenBraceToken: 1,
    CloseBraceToken: 2,
    OpenBracketToken: 3,
    CloseBracketToken: 4,
    CommaToken: 5,
    ColonToken: 6,
    NullKeyword: 7,
    TrueKeyword: 8,
    FalseKeyword: 9,
    StringLiteral: 10,
    NumericLiteral: 11,
    LineCommentTrivia: 12,
    BlockCommentTrivia: 13,
    LineBreakTrivia: 14,
    Trivia: 15,
    Unknown: 16,
    EOF: 17
} as const;

type TokenType = typeof Token[keyof typeof Token];

const TokenNames: Record<number, string> = {
    [Token.OpenBraceToken]: 'OpenBraceToken',
    [Token.CloseBraceToken]: 'CloseBraceToken',
    [Token.OpenBracketToken]: 'OpenBracketToken',
    [Token.CloseBracketToken]: 'CloseBracketToken',
    [Token.CommaToken]: 'CommaToken',
    [Token.ColonToken]: 'ColonToken',
    [Token.NullKeyword]: 'NullKeyword',
    [Token.TrueKeyword]: 'TrueKeyword',
    [Token.FalseKeyword]: 'FalseKeyword',
    [Token.StringLiteral]: 'StringLiteral',
    [Token.NumericLiteral]: 'NumericLiteral',
    [Token.LineCommentTrivia]: 'LineCommentTrivia',
    [Token.BlockCommentTrivia]: 'BlockCommentTrivia',
    [Token.LineBreakTrivia]: 'LineBreakTrivia',
    [Token.Trivia]: 'Trivia',
    [Token.Unknown]: 'Unknown',
    [Token.EOF]: 'EOF'
};

export class JsonParser extends Parser {

    async *parse(...sourcePaths: ParserInput[]): AsyncGenerator<SourceFile> {
        for (const sourcePath of sourcePaths) {
            try {
                yield {
                    ...new JsoncParserReader(parserInputRead(sourcePath)).parse(),
                    sourcePath: this.relativePath(sourcePath)
                };
            } catch (e: any) {
                // Return a ParseError for files that can't be parsed
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

/**
 * Parser that uses jsonc-parser's scanner for tokenization.
 * This is significantly faster than our custom character-by-character parsing.
 */
class JsoncParserReader {
    private readonly source: string;
    private readonly scanner: ReturnType<typeof createScanner>;
    // Use explicit number type to prevent TypeScript from narrowing after switch cases
    // (TypeScript doesn't know that consumeTrivia() and advance() modify this.token)
    private token: number = 0;
    private tokenOffset: number = 0;
    private tokenLength: number = 0;
    private tokenValue: string = '';

    constructor(source: string) {
        this.source = source;
        // ignoreTrivia = false to get whitespace and comments
        this.scanner = createScanner(source, false);
        this.advance();
    }

    private advance(): number {
        this.token = this.scanner.scan();
        this.tokenOffset = this.scanner.getTokenOffset();
        this.tokenLength = this.scanner.getTokenLength();
        this.tokenValue = this.scanner.getTokenValue();
        return this.token;
    }

    /**
     * Get current token. This method helps TypeScript understand that the token
     * may have changed after calling consumeTrivia() or advance().
     */
    private currentToken(): number {
        return this.token;
    }

    /**
     * Consumes all trivia (whitespace and comments) and returns them as a single string.
     */
    private consumeTrivia(): string {
        let trivia = '';
        while (
            this.token === Token.Trivia ||
            this.token === Token.LineBreakTrivia ||
            this.token === Token.LineCommentTrivia ||
            this.token === Token.BlockCommentTrivia
        ) {
            trivia += this.source.slice(this.tokenOffset, this.tokenOffset + this.tokenLength);
            this.advance();
        }
        return trivia;
    }

    private prefix(): Json.Space {
        return space(this.consumeTrivia());
    }

    parse(): Omit<Json.Document, "sourcePath"> {
        const prefix = this.prefix();

        // Handle empty document
        if (this.token === Token.EOF) {
            return {
                kind: Json.Kind.Document,
                id: randomId(),
                prefix,
                markers: emptyMarkers,
                value: {
                    kind: Json.Kind.Literal,
                    id: randomId(),
                    prefix: emptySpace,
                    markers: emptyMarkers,
                    source: this.source,
                    value: ''
                } satisfies Json.Literal as Json.Literal as Json.Value,
                eof: emptySpace
            };
        }

        const value = this.parseValue() as Json.Value;
        const eof = this.prefix();

        return {
            kind: Json.Kind.Document,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            value,
            eof
        };
    }

    private parseValue(): Json {
        const prefix = this.prefix();
        const base = {
            id: randomId(),
            prefix,
            markers: emptyMarkers
        };

        switch (this.token) {
            case Token.OpenBraceToken:
                return this.parseObject(base);
            case Token.OpenBracketToken:
                return this.parseArray(base);
            case Token.StringLiteral:
                return this.parseStringLiteral(base);
            case Token.NumericLiteral:
                return this.parseNumericLiteral(base);
            case Token.TrueKeyword:
                this.advance();
                return {
                    kind: Json.Kind.Literal,
                    ...base,
                    source: 'true',
                    value: true
                } satisfies Json.Literal as Json.Literal;
            case Token.FalseKeyword:
                this.advance();
                return {
                    kind: Json.Kind.Literal,
                    ...base,
                    source: 'false',
                    value: false
                } satisfies Json.Literal as Json.Literal;
            case Token.NullKeyword:
                this.advance();
                return {
                    kind: Json.Kind.Literal,
                    ...base,
                    source: 'null',
                    value: null
                } satisfies Json.Literal as Json.Literal;
            default:
                throw new Error(`Unexpected token ${TokenNames[this.token] || this.token} at offset ${this.tokenOffset}`);
        }
    }

    private parseObject(base: { id: string; prefix: Json.Space; markers: typeof emptyMarkers }): Json.Object {
        this.advance(); // consume '{'

        const members: Json.RightPadded<Json.Member>[] = [];

        // Check for empty object
        const afterOpen = this.consumeTrivia();
        if (this.token === Token.CloseBraceToken) {
            this.advance(); // consume '}'
            members.push({
                kind: Json.Kind.RightPadded,
                element: {
                    kind: Json.Kind.Empty,
                    id: randomId(),
                    prefix: emptySpace,
                    markers: emptyMarkers
                } satisfies Json.Empty as Json.Empty as unknown as Json.Member,
                after: space(afterOpen),
                markers: emptyMarkers
            } satisfies Json.RightPadded<Json.Member> as Json.RightPadded<Json.Member>);
        } else {
            // Parse members
            // Put back the trivia by prepending it to the key's prefix
            let pendingTrivia = afterOpen;

            while (true) {
                const member = this.parseMember(pendingTrivia);
                pendingTrivia = '';

                const afterMember = this.consumeTrivia();

                if (this.token === Token.CommaToken) {
                    this.advance(); // consume ','

                    // Check for trailing comma
                    const afterComma = this.consumeTrivia();
                    if (this.currentToken() === Token.CloseBraceToken) {
                        // Trailing comma
                        members.push({
                            kind: Json.Kind.RightPadded,
                            element: member,
                            after: space(afterMember),
                            markers: emptyMarkers
                        } satisfies Json.RightPadded<Json.Member> as Json.RightPadded<Json.Member>);

                        this.advance(); // consume '}'
                        members.push({
                            kind: Json.Kind.RightPadded,
                            element: {
                                kind: Json.Kind.Empty,
                                id: randomId(),
                                prefix: emptySpace,
                                markers: emptyMarkers
                            } satisfies Json.Empty as Json.Empty as unknown as Json.Member,
                            after: space(afterComma),
                            markers: emptyMarkers
                        } satisfies Json.RightPadded<Json.Member> as Json.RightPadded<Json.Member>);
                        break;
                    } else {
                        // More members - save trivia for next member's prefix
                        members.push({
                            kind: Json.Kind.RightPadded,
                            element: member,
                            after: space(afterMember),
                            markers: emptyMarkers
                        } satisfies Json.RightPadded<Json.Member> as Json.RightPadded<Json.Member>);
                        pendingTrivia = afterComma;
                    }
                } else if (this.token === Token.CloseBraceToken) {
                    this.advance(); // consume '}'
                    members.push({
                        kind: Json.Kind.RightPadded,
                        element: member,
                        after: space(afterMember),
                        markers: emptyMarkers
                    } satisfies Json.RightPadded<Json.Member> as Json.RightPadded<Json.Member>);
                    break;
                } else {
                    throw new Error(`Expected ',' or '}' at offset ${this.tokenOffset}, found ${TokenNames[this.token] || this.token}`);
                }
            }
        }

        return {
            kind: Json.Kind.Object,
            ...base,
            members
        } satisfies Json.Object as Json.Object;
    }

    private parseArray(base: { id: string; prefix: Json.Space; markers: typeof emptyMarkers }): Json.Array {
        this.advance(); // consume '['

        const values: Json.RightPadded<Json.Value>[] = [];

        // Check for empty array
        const afterOpen = this.consumeTrivia();
        if (this.token === Token.CloseBracketToken) {
            this.advance(); // consume ']'
            values.push({
                kind: Json.Kind.RightPadded,
                element: {
                    kind: Json.Kind.Empty,
                    id: randomId(),
                    prefix: emptySpace,
                    markers: emptyMarkers
                } satisfies Json.Empty as Json.Empty,
                after: space(afterOpen),
                markers: emptyMarkers
            } satisfies Json.RightPadded<Json.Value> as Json.RightPadded<Json.Value>);
        } else {
            // Parse values - need to handle the trivia we already consumed
            // by putting it back as a "pending" prefix
            let pendingTrivia = afterOpen;

            while (true) {
                // For first value, prepend the afterOpen trivia
                const valuePrefix = pendingTrivia + this.consumeTrivia();
                pendingTrivia = '';

                const valueBase = {
                    id: randomId(),
                    prefix: space(valuePrefix),
                    markers: emptyMarkers
                };

                let value: Json.Value;
                switch (this.token) {
                    case Token.OpenBraceToken:
                        value = this.parseObject(valueBase);
                        break;
                    case Token.OpenBracketToken:
                        value = this.parseArray(valueBase);
                        break;
                    case Token.StringLiteral:
                        value = this.parseStringLiteral(valueBase);
                        break;
                    case Token.NumericLiteral:
                        value = this.parseNumericLiteral(valueBase);
                        break;
                    case Token.TrueKeyword:
                        this.advance();
                        value = {
                            kind: Json.Kind.Literal,
                            ...valueBase,
                            source: 'true',
                            value: true
                        } satisfies Json.Literal as Json.Literal;
                        break;
                    case Token.FalseKeyword:
                        this.advance();
                        value = {
                            kind: Json.Kind.Literal,
                            ...valueBase,
                            source: 'false',
                            value: false
                        } satisfies Json.Literal as Json.Literal;
                        break;
                    case Token.NullKeyword:
                        this.advance();
                        value = {
                            kind: Json.Kind.Literal,
                            ...valueBase,
                            source: 'null',
                            value: null
                        } satisfies Json.Literal as Json.Literal;
                        break;
                    default:
                        throw new Error(`Unexpected token ${TokenNames[this.token] || this.token} in array at offset ${this.tokenOffset}`);
                }

                const afterValue = this.consumeTrivia();

                if (this.currentToken() === Token.CommaToken) {
                    this.advance(); // consume ','

                    // Check for trailing comma
                    const afterComma = this.consumeTrivia();
                    if (this.currentToken() === Token.CloseBracketToken) {
                        // Trailing comma
                        values.push({
                            kind: Json.Kind.RightPadded,
                            element: value,
                            after: space(afterValue),
                            markers: emptyMarkers
                        } satisfies Json.RightPadded<Json.Value> as Json.RightPadded<Json.Value>);

                        this.advance(); // consume ']'
                        values.push({
                            kind: Json.Kind.RightPadded,
                            element: {
                                kind: Json.Kind.Empty,
                                id: randomId(),
                                prefix: emptySpace,
                                markers: emptyMarkers
                            } satisfies Json.Empty as Json.Empty,
                            after: space(afterComma),
                            markers: emptyMarkers
                        } satisfies Json.RightPadded<Json.Value> as Json.RightPadded<Json.Value>);
                        break;
                    } else {
                        // More values
                        values.push({
                            kind: Json.Kind.RightPadded,
                            element: value,
                            after: space(afterValue),
                            markers: emptyMarkers
                        } satisfies Json.RightPadded<Json.Value> as Json.RightPadded<Json.Value>);
                        pendingTrivia = afterComma;
                    }
                } else if (this.currentToken() === Token.CloseBracketToken) {
                    this.advance(); // consume ']'
                    values.push({
                        kind: Json.Kind.RightPadded,
                        element: value,
                        after: space(afterValue),
                        markers: emptyMarkers
                    } satisfies Json.RightPadded<Json.Value> as Json.RightPadded<Json.Value>);
                    break;
                } else {
                    throw new Error(`Expected ',' or ']' at offset ${this.tokenOffset}, found ${TokenNames[this.currentToken()] || this.currentToken()}`);
                }
            }
        }

        return {
            kind: Json.Kind.Array,
            ...base,
            values
        } satisfies Json.Array as Json.Array;
    }

    private parseMember(pendingTrivia: string): Json.Member {
        const keyPrefix = pendingTrivia + this.consumeTrivia();
        const key = this.parseKey({
            id: randomId(),
            prefix: space(keyPrefix),
            markers: emptyMarkers
        });

        const afterKey = this.consumeTrivia();
        if (this.token !== Token.ColonToken) {
            throw new Error(`Expected ':' at offset ${this.tokenOffset}, found ${TokenNames[this.token] || this.token}`);
        }
        this.advance(); // consume ':'

        const value = this.parseValue() as Json.Value;

        return {
            kind: Json.Kind.Member,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            key: {
                kind: Json.Kind.RightPadded,
                markers: emptyMarkers,
                element: key as Json.Key,
                after: space(afterKey)
            } satisfies Json.RightPadded<Json.Key> as Json.RightPadded<Json.Key>,
            value
        } satisfies Json.Member as Json.Member;
    }

    /**
     * Parses a key which is a string literal in standard JSON/JSONC.
     * Note: jsonc-parser doesn't support JSON5 unquoted identifiers.
     */
    private parseKey(base: { id: string; prefix: Json.Space; markers: typeof emptyMarkers }): Json.Literal {
        if (this.token !== Token.StringLiteral) {
            throw new Error(`Expected string key at offset ${this.tokenOffset}, found ${TokenNames[this.token] || this.token}`);
        }
        return this.parseStringLiteral(base);
    }

    private parseStringLiteral(base: { id: string; prefix: Json.Space; markers: typeof emptyMarkers }): Json.Literal {
        const source = this.source.slice(this.tokenOffset, this.tokenOffset + this.tokenLength);
        const value = this.tokenValue;
        this.advance();

        return {
            kind: Json.Kind.Literal,
            ...base,
            source,
            value
        } satisfies Json.Literal as Json.Literal;
    }

    private parseNumericLiteral(base: { id: string; prefix: Json.Space; markers: typeof emptyMarkers }): Json.Literal {
        const source = this.source.slice(this.tokenOffset, this.tokenOffset + this.tokenLength);
        const value = parseFloat(source);
        this.advance();

        return {
            kind: Json.Kind.Literal,
            ...base,
            source,
            value
        } satisfies Json.Literal as Json.Literal;
    }
}
