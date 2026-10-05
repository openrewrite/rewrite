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
import {PassThrough} from "stream";
import {StreamMessageReader, StreamMessageWriter} from "vscode-jsonrpc/node";
import {chunkedJsonEncoder, jsonFragments} from "../../src/rpc/message-encoder";

// Values whose fragment stream must concatenate to exactly JSON.stringify(value). Excludes
// top-level undefined/function/symbol (JSON.stringify yields undefined there), which never occurs
// as a whole JSON-RPC message.
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
    ["NaN -> null", NaN],
    ["Infinity -> null", Infinity],
    ["-Infinity -> null", -Infinity],
    ["empty string", ""],
    ["plain string", "simple"],
    ["quotes and backslash", 'with "quotes" and \\ backslash'],
    ["control chars", "null\u0000 newline\n tab\t"],
    ["unicode", "café 你好 😀 π"],
    ["number array", [1, 2, 3]],
    ["mixed ignored array", [undefined, null, () => 1, Symbol("s"), 4]],
    ["sparse array", [, , 3]],
    ["nested object", {a: {b: [1, {c: "x"}], d: true}}],
    ["dropped undefined prop", {a: undefined, b: 2, c: undefined}],
    ["dropped function/symbol prop", {a: () => 1, b: Symbol("s"), c: 3}],
    ["escaped keys", {'a"b': 1, "π": 2, "": 3}],
    ["date via toJSON", new Date(0)],
    ["object holding date", {when: new Date(1234567890123)}],
    ["nested toJSON", {a: [new Date(0), {b: new Date(1)}]}],
    ["jsonrpc-shaped message", {
        jsonrpc: "2.0",
        id: 7,
        result: {
            id: "abc",
            descriptor: {name: "org.example.Recipe", description: "Does a thing.", recipeList: []},
            editVisitor: "edit:abc",
            editPreconditions: [],
            scanPreconditions: [],
            recipeList: [
                {id: "d1", descriptor: {name: "org.example.Child", recipeList: []}, delegatesTo: {recipeName: "org.example.Java", options: {x: 1}}}
            ]
        }
    }],
];

describe("chunked JSON message encoder", () => {
    test.each(parityCases)("jsonFragments concatenates to JSON.stringify: %s", (_name, value) => {
        expect([...jsonFragments(value)].join("")).toBe(JSON.stringify(value));
    });

    test.each(parityCases)("encode() produces JSON.stringify bytes: %s", async (_name, value) => {
        // A JSON-RPC message is always an object; wrap scalars so the encoder gets a Message shape.
        const msg = {jsonrpc: "2.0", id: 1, result: value} as any;
        const bytes = await chunkedJsonEncoder.encode(msg, {charset: "utf-8"});
        const expected = Buffer.from(JSON.stringify(msg), "utf-8");
        expect(Buffer.from(bytes).equals(expected)).toBe(true);
    });

    test("multi-buffer path stays byte-identical past the flush threshold", async () => {
        // A payload whose serialized form comfortably exceeds the 1 MiB flush threshold, forcing
        // several Buffer flushes + concat — the path a large recipe tree exercises.
        const big = {
            jsonrpc: "2.0",
            id: 42,
            result: {
                recipeList: Array.from({length: 40_000}, (_, i) => ({
                    name: `org.example.Recipe${i}`,
                    description: "A recipe that does something moderately interesting. 你好 😀",
                    recipeList: [],
                }))
            }
        } as any;
        const expected = Buffer.from(JSON.stringify(big), "utf-8");
        expect(expected.length).toBeGreaterThan(1 << 20); // actually crossed the threshold
        const bytes = await chunkedJsonEncoder.encode(big, {charset: "utf-8"});
        expect(Buffer.from(bytes).equals(expected)).toBe(true);
    });

    test("surrogate pairs survive fragment boundaries", async () => {
        // Emoji (surrogate pairs) live entirely within a single scalar fragment, so UTF-8 encoding
        // per flush never splits one. Repeat enough to span a flush boundary.
        const value = {s: "😀".repeat(400_000)};
        const msg = {jsonrpc: "2.0", id: 1, result: value} as any;
        const bytes = await chunkedJsonEncoder.encode(msg, {charset: "utf-8"});
        expect(Buffer.from(bytes).equals(Buffer.from(JSON.stringify(msg), "utf-8"))).toBe(true);
    });

    test("round-trips through StreamMessageWriter/Reader framing", async () => {
        // Wire the encoder into a real StreamMessageWriter and read it back with the default
        // StreamMessageReader: proves the option is honored and the Content-Length framing is
        // correct across the multi-buffer output, not just that encode() bytes match in isolation.
        const pipe = new PassThrough();
        const writer = new StreamMessageWriter(pipe, {contentTypeEncoder: chunkedJsonEncoder});
        const reader = new StreamMessageReader(pipe);
        const msg = {
            jsonrpc: "2.0",
            id: 5,
            result: {
                recipeList: Array.from({length: 5_000}, (_, i) => ({name: `org.example.R${i}`, description: "😀 does a thing"}))
            }
        };
        const received = new Promise<unknown>((resolve) => reader.listen(m => resolve(m)));
        await writer.write(msg as any);
        expect(await received).toEqual(msg);
    });
});
