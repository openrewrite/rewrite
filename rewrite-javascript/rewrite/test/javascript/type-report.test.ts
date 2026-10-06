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
import * as fs from "fs";
import * as os from "os";
import * as path from "path";
import {
    buildTypeReport,
    DUMP_TYPES_ENV,
    formatTypeReport,
    JavaScriptParser,
    JS,
    LstDebugPrinter,
    renderType,
    typescript
} from "../../src/javascript";
import {main} from "../../src/javascript/type-report-cli";
import {Type} from "../../src/java";
import {RecipeSpec} from "../../src/test";

const parse = async (text: string) =>
    (await new JavaScriptParser().parse({text, sourcePath: "t.ts"}).next()).value as JS.CompilationUnit;

const UNTYPED_RECEIVER = `
function f(arr) {
    arr.tostring();
    console.log(1);
}
`.trimStart();

describe("type report", () => {

    test("lists calls and declarations, naming the receiver that lost a call's type", async () => {
        const report = await buildTypeReport(await parse(UNTYPED_RECEIVER));

        expect(formatTypeReport(report)).toBe([
            "line:col  kind                   source           type",
            "1:1       MethodDeclaration      function f(arr)  t f(..) -> void",
            "1:12      NamedVariable          arr              ⚠ <unknown>",
            "2:5       MethodInvocation       arr.tostring()   ⚠ <unknown> tostring(..) -> <unknown>",
            "2:5         └ select:Identifier  arr              <unknown>",
            "3:5       MethodInvocation       console.log(1)   Console log(..) -> void",
            "3:17        └ arg0:Literal       1                double (Primitive)",
            "",
        ].join("\n"));
    });

    test("places a node at its first character, past a prefix printed inside its visit", async () => {
        const report = await buildTypeReport(await parse(`import * as  fs from "fs";`), {all: true});

        expect(report.entries.find(e => e.kind === "Identifier" && e.source === "fs")?.column).toBe(14);
    });

    test("renders the most specific pattern the matcher accepts, in TypeScript spelling", async () => {
        const [call] = (await buildTypeReport(await parse(`"a".charAt(1);`))).entries;

        expect(call.type).toBe("String charAt(number) -> String");
    });

    test("lists a call's name only as part of the call", async () => {
        const report = await buildTypeReport(await parse("console.log(1);"), {all: true});

        expect(report.entries.map(e => e.source)).not.toContain("log");
    });

    test("spells a JavaScript-only kind with its namespace, since it has its own visit method", async () => {
        const report = await buildTypeReport(await parse("const same = 1 === 2;"), {all: true});

        expect(report.entries.map(e => e.kind)).toContain("JS.Binary");
    });

    test("keeps a slot the parser left empty apart from an unknown type", () => {
        expect(renderType(undefined)).toBe("<none>");
        expect(renderType(Type.unknownType)).toBe("<unknown>");
    });

    test("narrows to missing rows and shows a declaring type's ancestry", async () => {
        const cu = await parse(`${UNTYPED_RECEIVER}class A extends Error { m() {} }\nnew A().m();\n`);

        const missing = await buildTypeReport(cu, {onlyMissing: true});
        expect(missing.entries.map(e => e.source)).toEqual(["arr", "arr.tostring()"]);

        const withAncestry = await buildTypeReport(cu, {supertypes: true});
        expect(withAncestry.entries.find(e => e.source === "new A().m()")?.supertypes).toBe("A <: Error");
    });

    test("tree view shows each node's type", async () => {
        const logs: string[] = [];
        const info = vi.spyOn(console, "info").mockImplementation((msg: string) => logs.push(msg));

        new LstDebugPrinter({includeCursorMessages: false, includeTypes: true}).print(await parse("console.log(1);"));

        info.mockRestore();
        expect(logs.join("\n")).toMatch(/MethodInvocation.*: Console log\(\.\.\) -> void/);
    });
});

describe("type report in RecipeSpec", () => {

    test("a missing change names the nodes that carry no type", async () => {
        await expect(new RecipeSpec().rewriteRun(
            typescript(UNTYPED_RECEIVER, UNTYPED_RECEIVER.replace("tostring", "toString"))
        )).rejects.toThrow(/Nodes with no type attribution[\s\S]*arr\.tostring\(\)[\s\S]*└ select:Identifier/);
    });

    test(`${DUMP_TYPES_ENV} prints each parsed file's attribution`, async () => {
        const logs: string[] = [];
        vi.spyOn(console, "log").mockImplementation((msg: string) => logs.push(msg));
        process.env[DUMP_TYPES_ENV] = "missing";
        try {
            await new RecipeSpec().rewriteRun(typescript(UNTYPED_RECEIVER));
        } finally {
            delete process.env[DUMP_TYPES_ENV];
        }

        const out = logs.join("\n");
        expect(out).toContain("--- type attribution:");
        expect(out).toContain("arr.tostring()");
        expect(out).not.toContain("console.log(1)");
    });
});

describe("rewrite-javascript-types", () => {

    test("reports a file on disk against its project, and rejects a flag the mode does not read", async () => {
        const project = fs.mkdtempSync(path.join(os.tmpdir(), "type-report-"));
        fs.writeFileSync(path.join(project, "package.json"), "{}");
        fs.mkdirSync(path.join(project, "src"));
        const file = path.join(project, "src", "f.ts");
        fs.writeFileSync(file, UNTYPED_RECEIVER);

        const written: string[] = [];
        vi.spyOn(process.stdout, "write").mockImplementation((s: any) => {
            written.push(String(s));
            return true;
        });
        expect(await main([file, "--json", "--only-missing"])).toBe(0);
        const json = JSON.parse(written.join(""));
        expect(json.sourcePath).toBe(path.join("src", "f.ts"));
        expect([json.nodeCount, json.missingCount]).toEqual([4, 2]);

        vi.spyOn(process.stderr, "write").mockImplementation(() => true);
        expect(await main([file, "--tree", "--json"])).not.toBe(0);
    });
});
