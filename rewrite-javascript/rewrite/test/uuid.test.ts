/*
 * Copyright 2026 the original author or authors.
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

import {execFileSync} from "node:child_process";
import * as fs from "node:fs";
import * as os from "node:os";
import * as path from "node:path";
import * as ts from "typescript";
import {randomId} from "../src/uuid";

describe("randomId", () => {
    test("generates valid UUID v4 format", () => {
        const uuid = randomId();
        // UUID v4 format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
        // where y is one of 8, 9, a, or b
        const uuidV4Regex = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
        expect(uuid).toMatch(uuidV4Regex);
    });

    test("generates unique IDs on each call", () => {
        const ids = new Set<string>();
        const iterations = 1000;

        for (let i = 0; i < iterations; i++) {
            ids.add(randomId());
        }

        expect(ids.size).toBe(iterations);
    });
});

// V8 keeps `a + b` as an unflattened cons-string until something forces a flatten. An LST `id` is
// only ever compared or serialized, so a cons-string `id` is retained as a rope of tiny heap nodes
// for the life of the tree. Detecting that needs `%DebugPrint`, which requires --allow-natives-syntax
// at parse time, so the check runs in a child node process rather than inside the vitest worker.
function v8StringType(expr: string, setup = ""): string {
    const script = `${setup}\nconst s = ${expr};\n%DebugPrint(s);`;
    const out = execFileSync(process.execPath, ["--allow-natives-syntax", "-e", script], {encoding: "utf8"});
    const match = out.match(/type:\s*([A-Z_]*STRING_TYPE)/);
    if (!match) {
        throw new Error(`Could not determine V8 string type from %DebugPrint output:\n${out}`);
    }
    return match[1];
}

function transpiledRandomIdModule(): string {
    const source = fs.readFileSync(path.resolve(__dirname, "../src/uuid.ts"), "utf8");
    const js = ts.transpileModule(source, {
        compilerOptions: {module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020},
    }).outputText;
    const file = path.join(fs.mkdtempSync(path.join(os.tmpdir(), "uuid-test-")), "uuid.js");
    fs.writeFileSync(file, js);
    return file;
}

describe("randomId string representation", () => {
    test("V8 string type detection distinguishes flat from cons strings", () => {
        // given / when / then
        expect(v8StringType("require('crypto').randomUUID()")).toBe("CONS_ONE_BYTE_STRING_TYPE");
        expect(v8StringType(
            "Buffer.from(require('crypto').randomUUID(),'latin1').toString('latin1')"
        )).toBe("SEQ_ONE_BYTE_STRING_TYPE");
    });

    test("randomId() returns a flat (SEQ) string, not a cons-string rope", () => {
        // given the actual randomId() from src/uuid.ts
        const module = transpiledRandomIdModule();

        // when
        const type = v8StringType("require(" + JSON.stringify(module) + ").randomId()");

        // then
        expect(type).toBe("SEQ_ONE_BYTE_STRING_TYPE");
    });
});

// Holds the ids alive so we measure retained size, not transient allocation, and forces GC around
// the measurement — so --expose-gc must run in a child process (vitest workers don't expose gc()).
function heapBytesHoldingIds(genExpr: string, count: number): number {
    const script = `
        if (typeof global.gc !== 'function') throw new Error('run with --expose-gc');
        const crypto = require('crypto');
        const gen = ${genExpr};
        global.gc(); global.gc();
        const before = process.memoryUsage().heapUsed;
        const ids = new Array(${count});
        for (let i = 0; i < ${count}; i++) ids[i] = gen();
        global.gc(); global.gc();
        const after = process.memoryUsage().heapUsed;
        if (ids[${count} - 1].length !== 36) throw new Error('unexpected id length');
        process.stdout.write(String(after - before));
    `;
    const out = execFileSync(process.execPath, ["--expose-gc", "-e", script], {encoding: "utf8"});
    return parseInt(out, 10);
}

describe("randomId memory footprint under load", () => {
    const ONE_MILLION = 1_000_000;
    const MB = 1024 * 1024;

    test("one million ids stay far below the cons-string rope cost", () => {
        // given the actual randomId() from src/uuid.ts, and the pre-fix cons-string baseline
        const module = transpiledRandomIdModule();
        const flatGen = "require(" + JSON.stringify(module) + ").randomId";
        const consGen = "() => crypto.randomUUID()";

        // when
        const flatBytes = heapBytesHoldingIds(flatGen, ONE_MILLION);
        const consBytes = heapBytesHoldingIds(consGen, ONE_MILLION);

        // then flat ids are a fraction of the rope cost (measured ~61MB vs ~465MB, ~7.6x)
        expect(flatBytes).toBeLessThan(150 * MB);
        expect(consBytes / flatBytes).toBeGreaterThan(3);
    });
});
