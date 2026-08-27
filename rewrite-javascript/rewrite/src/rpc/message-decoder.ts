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
import {constants} from "buffer";
import type {Message, MessageReaderOptions} from "vscode-jsonrpc/node";

/**
 * The content-type decoder shape vscode-jsonrpc's {@link MessageReaderOptions} accepts. Derived
 * from the reader options rather than deep-imported, because the concrete `ContentTypeDecoder`
 * type is not re-exported from the package entrypoint.
 */
export type JsonRpcContentTypeDecoder = NonNullable<MessageReaderOptions["contentTypeDecoder"]>;

const TAB = 0x09, NEWLINE = 0x0a, CARRIAGE_RETURN = 0x0d, SPACE = 0x20;
const QUOTE = 0x22, PLUS = 0x2b, COMMA = 0x2c, MINUS = 0x2d, PERIOD = 0x2e;
const ZERO = 0x30, NINE = 0x39, COLON = 0x3a, UPPER_E = 0x45;
const OPEN_BRACKET = 0x5b, BACKSLASH = 0x5c, CLOSE_BRACKET = 0x5d;
const LOWER_A = 0x61, LOWER_E = 0x65, LOWER_F = 0x66, LOWER_N = 0x6e, LOWER_T = 0x74, LOWER_Z = 0x7a;
const OPEN_BRACE = 0x7b, CLOSE_BRACE = 0x7d;

function isNumberByte(c: number): boolean {
    return (c >= ZERO && c <= NINE) || c === MINUS || c === PLUS || c === PERIOD || c === LOWER_E || c === UPPER_E;
}

interface Frame {
    readonly array?: unknown[];
    readonly object?: Record<string, unknown>;
    key?: string;
}

/**
 * Parses JSON directly from its UTF-8 bytes, never materializing the whole document as one JS
 * string. Structural punctuation is recognized byte-by-byte, while every scalar and every object key
 * is handed to the engine's `JSON.parse` as its own small slice — so escaping, number formatting and
 * literal validation match it exactly. This is the mirror of how `jsonFragments` defers to
 * `JSON.stringify`, and it accepts precisely the grammar `JSON.parse` accepts.
 *
 * Byte-level scanning is safe for the ASCII-compatible charsets vscode-jsonrpc supports (`utf-8`,
 * `ascii`): a UTF-8 continuation byte is always >= 0x80, so it can never be mistaken for a quote,
 * backslash, or structural character. Containers are tracked on an explicit stack, so nesting depth
 * is bounded by heap rather than by the call stack.
 */
export function parseJsonBytes(buf: Buffer, charset: BufferEncoding): unknown {
    const len = buf.length;
    let i = 0;

    const fail = (what: string): never => {
        throw new SyntaxError(`${what} in JSON at position ${i}`);
    };

    // Advance past insignificant whitespace and return the next byte, which must exist.
    const seek = (): number => {
        while (i < len) {
            const c = buf[i];
            if (c !== SPACE && c !== TAB && c !== NEWLINE && c !== CARRIAGE_RETURN) {
                return c;
            }
            i++;
        }
        return fail("Unexpected end of JSON input");
    };

    const token = (start: number, end: number): unknown => JSON.parse(buf.toString(charset, start, end));

    const readString = (): string => {
        const start = i++; // opening quote
        while (i < len) {
            const c = buf[i];
            if (c === BACKSLASH) {
                i += 2; // the byte after a backslash is ASCII, so it can never be the closing quote
            } else if (c === QUOTE) {
                return token(start, ++i) as string;
            } else {
                i++;
            }
        }
        return fail("Unterminated string");
    };

    const readKey = (): string => {
        if (seek() !== QUOTE) {
            fail("Expected property name");
        }
        const key = readString();
        if (seek() !== COLON) {
            fail("Expected ':' after property name");
        }
        i++;
        return key;
    };

    // JSON.parse gives __proto__ an own data property rather than setting the prototype.
    const setProperty = (object: Record<string, unknown>, key: string, value: unknown): void => {
        if (key === "__proto__") {
            Object.defineProperty(object, key, {value, writable: true, enumerable: true, configurable: true});
        } else {
            object[key] = value;
        }
    };

    const parseValue = (): unknown => {
        const stack: Frame[] = [];
        let value: unknown;

        nextValue: for (; ;) {
            const c = seek();
            if (c === OPEN_BRACE) {
                i++;
                const frame: Frame = {object: {}};
                if (seek() === CLOSE_BRACE) {
                    i++;
                    value = frame.object;
                } else {
                    frame.key = readKey();
                    stack.push(frame);
                    continue nextValue;
                }
            } else if (c === OPEN_BRACKET) {
                i++;
                const frame: Frame = {array: []};
                if (seek() === CLOSE_BRACKET) {
                    i++;
                    value = frame.array;
                } else {
                    stack.push(frame);
                    continue nextValue;
                }
            } else if (c === QUOTE) {
                value = readString();
            } else if (c === LOWER_T || c === LOWER_F || c === LOWER_N) {
                const start = i;
                while (i < len && buf[i] >= LOWER_A && buf[i] <= LOWER_Z) {
                    i++;
                }
                value = token(start, i); // true | false | null, validated by JSON.parse
            } else {
                const start = i;
                while (i < len && isNumberByte(buf[i])) {
                    i++;
                }
                if (i === start) {
                    fail(`Unexpected token ${JSON.stringify(buf.toString(charset, i, i + 1))}`);
                }
                value = token(start, i);
            }

            // Attach the finished value to its container, closing containers as they end.
            for (; ;) {
                const frame = stack[stack.length - 1];
                if (frame === undefined) {
                    return value;
                }
                if (frame.array !== undefined) {
                    frame.array.push(value);
                    const c = seek();
                    if (c === COMMA) {
                        i++;
                        continue nextValue;
                    }
                    if (c !== CLOSE_BRACKET) {
                        fail("Expected ',' or ']' after array element");
                    }
                    i++;
                    value = frame.array;
                } else {
                    setProperty(frame.object!, frame.key!, value);
                    const c = seek();
                    if (c === COMMA) {
                        i++;
                        frame.key = readKey();
                        continue nextValue;
                    }
                    if (c !== CLOSE_BRACE) {
                        fail("Expected ',' or '}' after property value");
                    }
                    i++;
                    value = frame.object!;
                }
                stack.pop();
            }
        }
    };

    const value = parseValue();
    while (i < len) {
        const c = buf[i];
        if (c !== SPACE && c !== TAB && c !== NEWLINE && c !== CARRIAGE_RETURN) {
            fail("Unexpected non-whitespace character after JSON");
        }
        i++;
    }
    return value;
}

/**
 * A drop-in replacement for vscode-jsonrpc's default `application/json` content-type decoder, which
 * does {@code JSON.parse(buffer.toString(charset))}. That intermediate string is capped at V8's
 * String.kMaxLength (~512 MB), so an inbound message above the cap throws "Cannot create a string
 * longer than 0x1fffffe8 characters" before it is ever parsed — the reader drops the message, the
 * peer's request is never answered, and the call hangs until it times out. Parsing straight from the
 * bytes raises the inbound ceiling to Node's Buffer.MAX_LENGTH, matching what {@link
 * chunkedJsonEncoder} does for outbound messages.
 *
 * A UTF-8 (or ASCII) decode never yields more UTF-16 code units than there are bytes, so a body at
 * or under the cap cannot overflow the intermediate string; those take the faster single-string
 * parse, and only genuinely oversized bodies pay for the byte-level walk.
 */
export const chunkedJsonDecoder: JsonRpcContentTypeDecoder = {
    name: "application/json",
    decode(buffer: Uint8Array, options: { charset: BufferEncoding }): Promise<Message> {
        try {
            const buf = Buffer.isBuffer(buffer) ?
                buffer :
                Buffer.from(buffer.buffer, buffer.byteOffset, buffer.byteLength);
            return Promise.resolve((buf.byteLength <= constants.MAX_STRING_LENGTH ?
                JSON.parse(buf.toString(options.charset)) :
                parseJsonBytes(buf, options.charset)) as Message);
        } catch (err) {
            return Promise.reject(err);
        }
    }
};
