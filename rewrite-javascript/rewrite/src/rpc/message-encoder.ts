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
import type {Message, MessageWriterOptions} from "vscode-jsonrpc/node";

/**
 * The content-type encoder shape vscode-jsonrpc's {@link MessageWriterOptions} accepts. Derived
 * from the writer options rather than deep-imported, because the concrete `ContentTypeEncoder`
 * type is not re-exported from the package entrypoint.
 */
export type JsonRpcContentTypeEncoder = NonNullable<MessageWriterOptions["contentTypeEncoder"]>;

// Flush the accumulated JSON text to a Buffer once it reaches this many UTF-16 code units. This
// keeps the transient string far below V8's ~512 MB String.kMaxLength while bounding how many
// intermediate Buffers we concatenate. A single scalar/key fragment is never split, so the pending
// string only ever exceeds the threshold by one whole fragment.
const FLUSH_THRESHOLD = 1 << 20; // 1 MiB

/**
 * True for values that {@code JSON.stringify} drops: an object property with such a value is
 * omitted, and such an array element serializes as {@code null}.
 */
function isJsonIgnored(value: unknown): boolean {
    return value === undefined || typeof value === "function" || typeof value === "symbol";
}

/**
 * Yields a value as the sequence of JSON text fragments that concatenate to exactly
 * {@code JSON.stringify(value)}, without ever building the whole document as one string. Every
 * scalar and every object key is serialized by the engine's {@code JSON.stringify} — so escaping,
 * number formatting, {@code undefined}/function/symbol handling, and {@code toJSON} all match it
 * exactly — and only the structural punctuation is emitted here.
 */
export function* jsonFragments(value: unknown): Generator<string> {
    // Mirror JSON.stringify's toJSON step before deciding container vs. scalar, so e.g. a Date
    // collapses to its string form rather than being walked as an object.
    if (value !== null && typeof value === "object" && typeof (value as { toJSON?: unknown }).toJSON === "function") {
        yield* jsonFragments((value as { toJSON(): unknown }).toJSON());
        return;
    }

    if (Array.isArray(value)) {
        yield "[";
        for (let i = 0; i < value.length; i++) {
            if (i > 0) {
                yield ",";
            }
            if (isJsonIgnored(value[i])) {
                yield "null";
            } else {
                yield* jsonFragments(value[i]);
            }
        }
        yield "]";
        return;
    }

    if (value !== null && typeof value === "object") {
        yield "{";
        let first = true;
        for (const key of Object.keys(value as object)) {
            const propValue = (value as Record<string, unknown>)[key];
            if (isJsonIgnored(propValue)) {
                continue;
            }
            if (!first) {
                yield ",";
            }
            first = false;
            yield JSON.stringify(key);
            yield ":";
            yield* jsonFragments(propValue);
        }
        yield "}";
        return;
    }

    // Scalar: string, number, boolean, or null (a bigint throws here, exactly as JSON.stringify does).
    yield JSON.stringify(value) as string;
}

/**
 * A drop-in replacement for vscode-jsonrpc's default `application/json` content-type encoder that
 * serializes a message to UTF-8 bytes without ever materializing the whole document as a single JS
 * string. The default encoder does {@code Buffer.from(JSON.stringify(msg))}; a large enough
 * PrepareRecipe response (a deep recipe tree) overflows V8's single-string limit and throws
 * "Cannot create a string longer than 0x1fffffe8 characters", hanging the RPC call. Streaming the
 * fragments into a Buffer raises the ceiling to Node's Buffer.MAX_LENGTH (multiple GB) while
 * producing byte-identical output. (True unbounded streaming needs chunked framing — Content-Length
 * still requires the whole body up front — so this is a ceiling raise, not a removal.)
 */
export const chunkedJsonEncoder: JsonRpcContentTypeEncoder = {
    name: "application/json",
    encode(msg: Message, options: { charset: BufferEncoding }): Promise<Uint8Array> {
        const buffers: Buffer[] = [];
        let pending = "";
        for (const fragment of jsonFragments(msg)) {
            pending += fragment;
            if (pending.length >= FLUSH_THRESHOLD) {
                buffers.push(Buffer.from(pending, options.charset));
                pending = "";
            }
        }
        if (pending.length > 0) {
            buffers.push(Buffer.from(pending, options.charset));
        }
        return Promise.resolve(Buffer.concat(buffers));
    }
};
