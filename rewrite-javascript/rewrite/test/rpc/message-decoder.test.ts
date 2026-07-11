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
import {PassThrough} from "stream";
import {StreamMessageReader, StreamMessageWriter} from "vscode-jsonrpc/node";
import {chunkedJsonDecoder, parseJsonBytes} from "../../src/rpc/message-decoder";
import {chunkedJsonEncoder} from "../../src/rpc/message-encoder";

const utf8 = (json: string): Buffer => Buffer.from(json, "utf-8");

// Values whose JSON text must parse to exactly what JSON.parse produces for it.
const parityCases: [string, unknown][] = [
    ["empty object", {}],
    ["empty array", []],
    ["null", null],
    ["true", true],
    ["false", false],
    ["zero", 0],
    ["negative zero", -0],
    ["float", 1.5],
    ["large exponent", 1e21],
    ["small exponent", -1e-7],
    ["empty string", ""],
    ["plain string", "simple"],
    ["quotes and backslash", 'with "quotes" and \\ backslash'],
    ["escaped quote at token end", '\\"'],
    ["control chars", "null\u0000 newline\n tab\t"],
    ["unicode", "café 你好 😀 π"],
    ["escaped surrogate pair", "😀"],
    ["number array", [1, 2, 3]],
    ["nested object", {a: {b: [1, {c: "x"}], d: true}}],
    ["escaped keys", {'a"b': 1, "π": 2, "": 3}],
    ["array of objects", [{a: 1}, {a: 2}]],
    ["jsonrpc-shaped message", {
        jsonrpc: "2.0",
        id: 7,
        result: {
            id: "abc",
            descriptor: {name: "org.example.Recipe", description: "Does a thing.", recipeList: []},
            editVisitor: "edit:abc",
            recipeList: [
                {id: "d1", descriptor: {name: "org.example.Child", recipeList: []}, delegatesTo: {recipeName: "org.example.Java", options: {x: 1}}}
            ]
        }
    }],
];

describe("chunked JSON message decoder", () => {
    test.each(parityCases)("parseJsonBytes matches JSON.parse: %s", (_name, value) => {
        const json = JSON.stringify(value);
        expect(parseJsonBytes(utf8(json), "utf-8")).toEqual(JSON.parse(json));
    });

    test("tolerates insignificant whitespace between every token", () => {
        const json = ' { "a" : [ 1 , { "b" : null } ] , "c" : "x" } \r\n\t';
        expect(parseJsonBytes(utf8(json), "utf-8")).toEqual(JSON.parse(json));
    });

    test("last duplicate key wins, as in JSON.parse", () => {
        const json = '{"a":1,"a":2}';
        expect(parseJsonBytes(utf8(json), "utf-8")).toEqual(JSON.parse(json));
    });

    test("__proto__ becomes an own property rather than polluting the prototype", () => {
        const parsed = parseJsonBytes(utf8('{"__proto__":{"polluted":true}}'), "utf-8") as object;
        expect(Object.getPrototypeOf(parsed)).toBe(Object.prototype);
        expect(Object.prototype.hasOwnProperty.call(parsed, "__proto__")).toBe(true);
        expect(({} as Record<string, unknown>).polluted).toBeUndefined();
    });

    test("deep nesting does not overflow the stack", () => {
        // An LST/recipe tree can nest far deeper than a recursive-descent parser's frame budget.
        const depth = 50_000;
        const json = "[".repeat(depth) + "1" + "]".repeat(depth);
        let node = parseJsonBytes(utf8(json), "utf-8") as unknown[];
        for (let d = 1; d < depth; d++) {
            node = node[0] as unknown[];
        }
        expect(node[0]).toBe(1);
    });

    test.each([
        ["empty input", ""],
        ["unterminated object", "{"],
        ["unterminated string", '"abc'],
        ["trailing comma in array", "[1,]"],
        ["trailing comma in object", '{"a":1,}'],
        ["missing colon", '{"a" 1}'],
        ["unquoted key", "{a:1}"],
        ["truncated literal", "tru"],
        ["leading zero", "01"],
        ["bare minus", "-"],
        ["two values", "1 2"],
        ["trailing garbage", '{"a":1}x'],
    ])("rejects malformed JSON: %s", (_name, json) => {
        expect(() => parseJsonBytes(utf8(json), "utf-8")).toThrow(SyntaxError);
        expect(() => JSON.parse(json)).toThrow(SyntaxError); // the reference rejects it too
    });

    test("decodes a message larger than V8's max string length", async () => {
        // The regression: the default `application/json` decoder does JSON.parse(buffer.toString(charset)),
        // so any inbound message above the string cap dies before it is ever parsed. Padding with
        // insignificant whitespace keeps the *parsed* value tiny while the *encoded* body clears the cap.
        const max = constants.MAX_STRING_LENGTH;
        const head = '{"jsonrpc":"2.0","id":1,"method":"GetObject","params":[';
        const tail = "1]}";
        const body = Buffer.alloc(max + 64, 0x20 /* space */);
        body.write(head, 0, "utf-8");
        body.write(tail, body.length - tail.length, "utf-8");
        expect(body.byteLength).toBeGreaterThan(max);

        // This is the exact wall, and the exact error text, seen in rpc.log.
        expect(() => body.toString("utf-8")).toThrow(/Cannot create a string longer than 0x1fffffe8 characters/);

        await expect(chunkedJsonDecoder.decode(body, {charset: "utf-8"})).resolves.toEqual({
            jsonrpc: "2.0",
            id: 1,
            method: "GetObject",
            params: [1],
        });
    }, 300_000);

    test("round-trips through StreamMessageWriter/Reader framing", async () => {
        // Proves the reader honors the contentTypeDecoder option and that it composes with the
        // chunked encoder on the other side of the wire.
        const pipe = new PassThrough();
        const writer = new StreamMessageWriter(pipe, {contentTypeEncoder: chunkedJsonEncoder});
        const reader = new StreamMessageReader(pipe, {contentTypeDecoder: chunkedJsonDecoder});
        const msg = {
            jsonrpc: "2.0",
            id: 5,
            result: {
                recipeList: Array.from({length: 5_000}, (_, i) => ({name: `org.example.R${i}`, description: "😀 does a thing"}))
            }
        };
        const received = new Promise<unknown>((resolve) => reader.listen(m => resolve(m)));
        await writer.write(msg as never);
        expect(await received).toEqual(msg);
    });
});
