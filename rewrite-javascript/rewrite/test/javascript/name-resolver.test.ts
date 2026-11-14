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
import * as fs from "node:fs";
import * as os from "node:os";
import * as path from "node:path";
import {JavaScriptParser, JavaScriptVisitor, JS, RESOLVE_NAMES, ResolvedNames} from "../../src/javascript";
import {J, Type} from "../../src/java";
import {ExecutionContext} from "../../src/execution";
import {SourceFile} from "../../src";
import {TreePrinters} from "../../src/print";
import {trimIndent} from "../../src/util";

const RESOLVING = {[RESOLVE_NAMES]: "true"};

type Row = [text: string, kind: string, name: string];
type PackagedRow = [...Row, pkg: string | null];

function writeProject(files: Record<string, string>, links: Record<string, string>): string {
    const project = fs.mkdtempSync(path.join(os.tmpdir(), "rewrite-name-resolver-"));
    for (const [file, text] of Object.entries(files)) {
        const target = path.join(project, file);
        fs.mkdirSync(path.dirname(target), {recursive: true});
        fs.writeFileSync(target, text);
    }
    for (const [link, target] of Object.entries(links)) {
        fs.mkdirSync(path.dirname(path.join(project, link)), {recursive: true});
        fs.symlinkSync(path.join(project, target), path.join(project, link), "dir");
    }
    return project;
}

/** Writes a project, links each of `links` to the path in it that it names, and parses `entries`. */
async function parseAll(files: Record<string, string>, entries: string[], messages: Record<string, string> = RESOLVING,
                        links: Record<string, string> = {}): Promise<SourceFile[]> {
    const project = writeProject(files, links);
    try {
        const parser = new JavaScriptParser({ctx: new ExecutionContext(messages), relativeTo: project});
        const parsed: SourceFile[] = [];
        for await (const sourceFile of parser.parse(...entries.map(entry => path.join(project, entry)))) {
            parsed.push(sourceFile);
        }
        return parsed;
    } finally {
        fs.rmSync(project, {recursive: true, force: true});
    }
}

async function parse(files: Record<string, string>, entry: string, messages: Record<string, string> = RESOLVING,
                     links: Record<string, string> = {}): Promise<SourceFile> {
    const [sourceFile] = await parseAll(files, [entry], messages, links);
    return sourceFile;
}

function markerOf(sourceFile: SourceFile): ResolvedNames | undefined {
    expect(sourceFile.kind).toBe(JS.Kind.CompilationUnit);
    return sourceFile.markers.markers.find(m => m.kind === JS.Markers.ResolvedNames) as ResolvedNames | undefined;
}

/** Every row of a file, as the text it stands on, its kind, its name and the package of that name. */
async function rowsOf(sourceFile: SourceFile): Promise<PackagedRow[]> {
    const {names, packages, kinds, spans} = markerOf(sourceFile)!;
    const printed = await TreePrinters.print(sourceFile);
    const rows: PackagedRow[] = [];
    for (let i = 0; i < spans.length; i += 4) {
        rows.push([printed.substring(spans[i], spans[i + 1]), kinds[spans[i + 3]], names[spans[i + 2]], packages[spans[i + 2]]]);
    }
    return rows;
}

async function packaged(files: Record<string, string>, entry: string, links?: Record<string, string>): Promise<PackagedRow[]> {
    return rowsOf(await parse(files, entry, RESOLVING, links));
}

async function resolved(files: Record<string, string>, entry: string, links?: Record<string, string>): Promise<Row[]> {
    return (await packaged(files, entry, links)).map(([text, kind, name]) => [text, kind, name]);
}

const DECLARATION_KINDS = ["class", "interface", "enum", "enum-member", "method", "constructor", "field", "module"];

/** Whether a reference opens a file, or else finds the declaration of its name by the kinds a lookup for it goes by. */
function meets(reference: PackagedRow, declarations: PackagedRow[], files: string[]): boolean {
    const types = ["class", "interface", "enum", "module"];
    const [, kind, name] = reference;
    if (files.includes(name)) {
        return true;
    }
    const has = (kinds: string[], wanted: string) => declarations.some(([, k, n]) => kinds.includes(k) && n === wanted);
    switch (kind) {
        case "method-call":
            return has(["method"], name);
        case "field-access":
            return has(["field", "enum-member"], name);
        case "constructor-call": {
            const type = name.replace(/\.<init>$/, "");
            return has(["constructor"], `${type}.<constructor>`) || has(types, type);
        }
        case "type-ref":
            return has(types, name);
        case "import-ref":
            return has([...types, "method", "field"], name);
        default:
            throw new Error(`not a reference: ${kind}`);
    }
}

/** The declarations of a package's own build, where its files are the repository. */
async function ownDeclarations(source: Record<string, string>): Promise<PackagedRow[]> {
    const code = Object.keys(source).filter(file => /\.[cm]?[jt]sx?$/.test(file));
    const rows = (await Promise.all((await parseAll(source, code)).map(rowsOf))).flat();
    return rows.filter(([, kind]) => DECLARATION_KINDS.includes(kind));
}

function installed(pkg: string, source: Record<string, string>): Record<string, string> {
    return Object.fromEntries(Object.entries(source).map(([file, text]) => [`node_modules/${pkg}/${file}`, text]));
}

describe("the marker", () => {
    test("is absent unless the parse asks for it", async () => {
        expect(markerOf(await parse({"a.js": "const a = 1;"}, "a.js", {}))).toBeUndefined();
        expect(markerOf(await parse({"a.js": "const a = 1;"}, "a.js", {[RESOLVE_NAMES]: "false"}))).toBeUndefined();
    });

    test("holds each name and each kind once", async () => {
        const {names, packages, kinds, spans} = markerOf(await parse({
            "a.js": trimIndent(`
                const a = 1;
                a;
                a;
            `)
        }, "a.js"))!;
        expect(names).toEqual(["a.a"]);
        expect(packages).toEqual([null]);
        expect(kinds).toEqual(["field", "field-access"]);
        expect(spans).toEqual([6, 7, 0, 0, 13, 14, 0, 1, 16, 17, 0, 1]);
    });

    test("keeps one name apart in the two packages that declare it", async () => {
        const typed = (name: string) => ({
            [`node_modules/${name}/package.json`]: JSON.stringify({name, version: "1.0.0", types: "index.d.ts"}),
            [`node_modules/${name}/index.d.ts`]: "export declare function go(): void;",
        });
        const {names, packages} = markerOf(await parse({
            ...typed("one"),
            ...typed("two"),
            "use.ts": trimIndent(`
                import {go} from "one";
                import {go as other} from "two";
                go();
                other();
            `)
        }, "use.ts"))!;
        expect(names.map((name, i) => [name, packages[i]])).toEqual(expect.arrayContaining([
            ["index.d.go", "one"],
            ["index.d.go", "two"],
        ]));
    });

    test("counts offsets into a file with a byte order mark as it prints", async () => {
        const sourceFile = await parse({"a.js": "\uFEFFconst first = 1;\nfirst;\n"}, "a.js");
        const printed = await TreePrinters.print(sourceFile);
        expect(printed.charCodeAt(0)).toBe(0xFEFF);
        const {spans} = markerOf(sourceFile)!;
        expect(printed.substring(spans[0], spans[1])).toBe("first");
        expect(printed.substring(spans[4], spans[5])).toBe("first");
    });

    test("counts a character beyond the basic plane as its two code units", async () => {
        const sourceFile = await parse({
            "a.js": trimIndent(`
                const a = '😀';
                const b = 1;
            `)
        }, "a.js");
        const printed = await TreePrinters.print(sourceFile);
        const {spans} = markerOf(sourceFile)!;
        expect(printed.substring(spans[4], spans[5])).toBe("b");
    });

    test("counts a lone surrogate at the length of the escape it prints as", async () => {
        const parser = new JavaScriptParser({ctx: new ExecutionContext(RESOLVING)});
        const sourceFile = await parser.parseOne({text: "const a = '\uD83D';\nconst b = 1;\n", sourcePath: "a.js"});
        const printed = await TreePrinters.print(sourceFile);
        const {spans} = markerOf(sourceFile)!;
        expect(printed.substring(spans[4], spans[5])).toBe("b");
    });
});

describe("a module", () => {
    test("has no row of its own", async () => {
        expect(await rowsOf(await parse({"a.js": "const a = 1;"}, "a.js"))).toEqual([["a", "field", "a.a", null]]);
        expect(await rowsOf(await parse({"a.js": ""}, "a.js"))).toEqual([]);
    });

    test("is named by its file, keeping every extension", async () => {
        expect(await resolved({
            "src/m.test.js": "export const a = 1;",
            "src/use.js": `import "./m.test.js";`,
        }, "src/use.js")).toEqual([[`"./m.test.js"`, "import-ref", "src/m.test.js"]]);
    });

    test("names its members without its extension", async () => {
        const [lib, use] = await parseAll({
            "src/lib.js": "export const a = 1;",
            "src/use.js": trimIndent(`
                import * as lib from "./lib.js";
                lib.a;
            `)
        }, ["src/lib.js", "src/use.js"]);
        expect(await rowsOf(lib)).toEqual([["a", "field", "src/lib.a", null]]);
        expect(await rowsOf(use)).toEqual([
            ["lib", "import-ref", "src/lib.js", null],
            [`"./lib.js"`, "import-ref", "src/lib.js", null],
            ["lib", "type-ref", "src/lib.js", null],
            ["a", "field-access", "src/lib.a", null],
        ]);
    });

    test("names what is assigned over its binding without its extension", async () => {
        expect(await resolved({
            "lib.js": "export const a = 1;",
            "use.js": trimIndent(`
                import * as lib from "./lib.js";
                lib = {b: 1};
            `)
        }, "use.js")).toContainEqual(["b", "field", "lib.b"]);
    });

    test("in JSON is named by its file too", async () => {
        const names = await resolved({
            "package.json": JSON.stringify({version: "1.0.0"}),
            "use.js": trimIndent(`
                import manifest from "./package.json";
                manifest;
            `)
        }, "use.js");
        expect(names).toEqual([
            ["manifest", "import-ref", "package.json"],
            [`"./package.json"`, "import-ref", "package.json"],
            ["manifest", "type-ref", "package.json"],
        ]);
    });

    test("that declares nothing of a name is still named by its file", async () => {
        expect(await resolved({
            "lib.js": "// Nothing else.\n",
            "use.js": `import "./lib.js";`,
        }, "use.js")).toEqual([[`"./lib.js"`, "import-ref", "lib.js"]]);
    });
});

describe("a declaration's name", () => {
    test("at the top of a module", async () => {
        expect(await resolved({
            "src/m.js": trimIndent(`
                function f() {}
                const g = () => {};
                const v = 1;
            `)
        }, "src/m.js")).toEqual([
            ["f", "method", "src/m.f"],
            ["g", "method", "src/m.g"],
            ["v", "field", "src/m.v"],
        ]);
    });

    test("of a class and its members", async () => {
        expect(await resolved({
            "src/m.js": trimIndent(`
                class C {
                    label = 1;
                    constructor() {
                        this.x = 1;
                    }
                    render() {}
                    get size() {
                        return 1;
                    }
                }
            `)
        }, "src/m.js")).toEqual([
            ["C", "class", "src/m.C"],
            ["label", "field", "src/m.C.label"],
            ["constructor", "constructor", "src/m.C.<constructor>"],
            ["x", "field", "src/m.C.x"],
            ["render", "method", "src/m.C.render"],
            ["size", "field", "src/m.C.size"],
        ]);
    });

    test("of a private member keeps its hash", async () => {
        const names = await resolved({
            "a.js": trimIndent(`
                class Box {
                    #size = 1;
                    grow() {
                        return this.#size;
                    }
                }
            `)
        }, "a.js");
        expect(names.filter(([text]) => text === "#size")).toEqual([
            ["#size", "field", "a.Box.#size"],
            ["#size", "field-access", "a.Box.#size"],
        ]);
    });

    test("of what a constructor function declares", async () => {
        const names = await resolved({
            "src/m.js": trimIndent(`
                function F() {
                    this.x = 1;
                }
                F.prototype.y = function () {};
                F.z = 2;
            `)
        }, "src/m.js");
        expect(names).toEqual(expect.arrayContaining([
            ["F", "method", "src/m.F"],
            ["x", "field", "src/m.F.x"],
            ["y", "method", "src/m.F.y"],
            ["z", "field", "src/m.F.z"],
        ]));
    });

    test("does not take the container of an expando for a declaration", async () => {
        expect((await resolved({
            "src/m.js": trimIndent(`
                function F() {}
                F.z = 2;
            `)
        }, "src/m.js")).filter(([text]) => text === "F")).toEqual([
            ["F", "method", "src/m.F"],
            ["F", "method-call", "src/m.F"],
        ]);
    });

    test("of what a prototype object declares", async () => {
        expect(await resolved({
            "src/m.js": trimIndent(`
                function F() {}
                F.prototype = {
                    y() {},
                };
            `)
        }, "src/m.js")).toContainEqual(["y", "method", "src/m.F.y"]);
    });

    test("of an object literal's members", async () => {
        expect(await resolved({
            "src/m.js": trimIndent(`
                const o = {
                    a() {},
                    b: 1,
                    nested: {
                        c: 2,
                    },
                };
            `)
        }, "src/m.js")).toEqual([
            ["o", "field", "src/m.o"],
            ["a", "method", "src/m.o.a"],
            ["b", "field", "src/m.o.b"],
            ["nested", "field", "src/m.o.nested"],
            ["c", "field", "src/m.o.nested.c"],
        ]);
    });

    test("of an object literal with no binding stands on where it is", async () => {
        const names = await resolved({
            "src/m.js": trimIndent(`
                function go() {}
                go({
                    key: 1,
                });
                go({
                    key: 2,
                });
            `)
        }, "src/m.js");
        expect(names.filter(([text]) => text === "key")).toEqual([
            ["key", "field", "src/m.<object:2:4>.key"],
            ["key", "field", "src/m.<object:5:4>.key"],
        ]);
    });

    test("counts the line of an object literal by line feeds alone, as the search index does", async () => {
        expect(await resolved({
            "a.js": "const a = '\u2028';\rgo({key: 1});\nfunction go() {}\n",
        }, "a.js")).toContainEqual(["key", "field", "a.<object:1:19>.key"]);
    });

    test("of a named function passed as a value stands on where it is too", async () => {
        const names = await resolved({
            "src/m.js": trimIndent(`
                function go() {}
                go(function named() {});
                go(function named() {});
            `)
        }, "src/m.js");
        expect(names.filter(([text]) => text === "named")).toEqual([
            ["named", "method", "src/m.<object:2:4>.named"],
            ["named", "method", "src/m.<object:3:4>.named"],
        ]);
    });

    test("of exports.x = and module.exports.x = is the module's", async () => {
        expect(await resolved({
            "src/m.js": trimIndent(`
                exports.x = 1;
                module.exports.y = 2;
            `)
        }, "src/m.js")).toEqual([
            ["exports", "type-ref", "src/m.js"],
            ["x", "field", "src/m.x"],
            ["module", "type-ref", "src/m.js"],
            ["exports", "type-ref", "src/m.js"],
            ["y", "field", "src/m.y"],
        ]);
    });

    test("of module.exports = {} is the module's too", async () => {
        expect(await resolved({
            "src/m.js": trimIndent(`
                module.exports = {
                    a() {},
                    b: 1,
                };
            `)
        }, "src/m.js")).toEqual([
            ["module", "type-ref", "src/m.js"],
            ["exports", "import-ref", "src/m.js"],
            ["a", "method", "src/m.a"],
            ["b", "field", "src/m.b"],
        ]);
    });

    test("of an alias of module.exports is the module's, and no call is on its declaration's line", async () => {
        const names = await resolved({
            "src/m.js": trimIndent(`
                const app = exports = module.exports = {};
                app.enabled = function enabled() {};
            `)
        }, "src/m.js");
        expect(names).toContainEqual(["enabled", "method", "src/m.enabled"]);
        expect(names).not.toContainEqual(["enabled", "method-call", "src/m.enabled"]);
    });

    test("that a CommonJS file gives itself is the module, wherever it is read", async () => {
        const names = await resolved({
            "src/m.js": trimIndent(`
                module.exports = {};
                if (!module.parent) {
                    console.log(module);
                }
            `)
        }, "src/m.js");
        expect(names.filter(([text]) => text === "module")).toEqual(Array(3).fill(["module", "type-ref", "src/m.js"]));
    });

    test("that merely spells module or exports is its own", async () => {
        expect(await resolved({
            "src/m.js": trimIndent(`
                module.exports = {};
                const config = {
                    module: 1,
                };
                function go(exports) {
                    return exports.x;
                }
                config.module;
            `)
        }, "src/m.js")).toEqual(expect.arrayContaining([
            ["module", "field", "src/m.config.module"],
            ["module", "field-access", "src/m.config.module"],
        ]));
    });

    test("of an assigned function is its binding's, and a call by its own name calls that", async () => {
        expect(await resolved({
            "src/m.js": trimIndent(`
                const f = function own() {
                    return own();
                };
            `)
        }, "src/m.js")).toEqual([
            ["f", "method", "src/m.f"],
            ["own", "method-call", "src/m.f"],
        ]);
    });

    test("of a function assigned to something untyped is its own", async () => {
        expect(await resolved({
            "src/m.js": trimIndent(`
                const req = JSON.parse("{}");
                req.header = function header() {};
            `)
        }, "src/m.js")).toContainEqual(["header", "method", "src/m.header"]);
    });

    test("of what module.exports = holds is its own", async () => {
        expect(await resolved({"src/m.js": "module.exports = function named() {};"}, "src/m.js")).toEqual([
            ["module", "type-ref", "src/m.js"],
            ["exports", "import-ref", "src/m.named"],
            ["named", "method", "src/m.named"],
        ]);
        expect(await resolved({"src/m.js": "module.exports = class Named {};"}, "src/m.js")).toEqual([
            ["module", "type-ref", "src/m.js"],
            ["exports", "import-ref", "src/m.Named"],
            ["Named", "class", "src/m.Named"],
        ]);
    });

    test("of an anonymous value of module.exports stands on exports", async () => {
        expect(await resolved({"src/m.js": "module.exports = function () {};"}, "src/m.js")).toEqual([
            ["module", "type-ref", "src/m.js"],
            ["exports", "method", "src/m.default"],
        ]);
    });

    test("of an anonymous default export stands on default", async () => {
        expect(await resolved({"src/m.js": "export default function () {}"}, "src/m.js"))
            .toEqual([["default", "method", "src/m.default"]]);
        expect(await resolved({"src/m.js": "export default class {}"}, "src/m.js"))
            .toEqual([["default", "class", "src/m.default"]]);
        expect(await resolved({"src/m.js": "export default {a: 1};"}, "src/m.js")).toEqual([
            ["default", "field", "src/m.default"],
            ["a", "field", "src/m.default.a"],
        ]);
    });

    test("of a named default export is its own", async () => {
        expect(await resolved({"src/m.js": "export default function main() {}"}, "src/m.js"))
            .toEqual([["main", "method", "src/m.main"]]);
    });

    test("of a key that is a string or a number", async () => {
        expect(await resolved({
            "a.js": trimIndent(`
                const o = {
                    "a-b": 1,
                    2: 3,
                };
            `)
        }, "a.js")).toEqual([
            ["o", "field", "a.o"],
            [`"a-b"`, "field", "a.o.a-b"],
            ["2", "field", "a.o.2"],
        ]);
    });

    test("of a parameter property is its class's", async () => {
        expect(await resolved({
            "box.ts": trimIndent(`
                const base = 1;
                class Box {
                    constructor(private size = base, other = size) {
                        size;
                    }
                }
            `)
        }, "box.ts")).toEqual([
            ["base", "field", "box.base"],
            ["Box", "class", "box.Box"],
            ["constructor", "constructor", "box.Box.<constructor>"],
            ["size", "field", "box.Box.size"],
            ["base", "field-access", "box.base"],
        ]);
    });

    test("of a namespace and what it holds, exported or not", async () => {
        expect(await resolved({
            "a.ts": trimIndent(`
                namespace N {
                    export const v = 1;
                    const hidden = 2;
                }
                N.v;
            `)
        }, "a.ts")).toEqual([
            ["N", "module", "a.N"],
            ["v", "field", "a.N.v"],
            ["hidden", "field", "a.N.hidden"],
            ["N", "type-ref", "a.N"],
            ["v", "field-access", "a.N.v"],
        ]);
    });

    test("of a namespace in a declaration file of the repository stands on a row of ours", async () => {
        expect(await resolved({
            "types.d.ts": trimIndent(`
                declare namespace N {
                    const v: number;
                }
            `)
        }, "types.d.ts")).toContainEqual(["N", "module", "N"]);
    });

    test("of a type literal's members is what the literal is bound to", async () => {
        expect(await resolved({
            "a.ts": trimIndent(`
                type T = {
                    a: string;
                };
            `)
        }, "a.ts")).toContainEqual(["a", "field", "a.T.a"]);
    });

    test("of what a module adds to the global scope is the module's", async () => {
        expect(await resolved({
            "a.ts": trimIndent(`
                export {};
                declare global {
                    const shared: number;
                }
            `)
        }, "a.ts")).toContainEqual(["shared", "field", "a.shared"]);
    });

    test("of a type parameter is its owner's", async () => {
        expect(await resolved({
            "a.ts": trimIndent(`
                class Box<T> {
                    value: T;
                }
            `)
        }, "a.ts")).toEqual([
            ["Box", "class", "a.Box"],
            ["T", "interface", "a.Box.T"],
            ["value", "field", "a.Box.value"],
            ["T", "type-ref", "a.Box.T"],
        ]);
    });

    test("in a declaration file is the parser's, which drops the .d of a type", async () => {
        const names = await resolved({
            "types/route.d.ts": trimIndent(`
                export interface Route {
                    path: string;
                }
                export declare function go(): void;
            `),
        }, "types/route.d.ts");
        expect(names).toEqual([
            ["Route", "interface", "types/route.Route"],
            ["path", "field", "types/route.Route.path"],
            ["go", "method", "types/route.d.go"],
        ]);
    });

    test("is one for two declarations of one symbol, which answer together", async () => {
        const names = await resolved({
            "a.ts": trimIndent(`
                function go(a: string): void;
                function go(a: number): void;
                function go(a: any) {}
                interface Box {
                    size: number;
                }
                class Box {}
            `)
        }, "a.ts");
        expect(names).toEqual([
            ["go", "method", "a.go"],
            ["go", "method", "a.go"],
            ["go", "method", "a.go"],
            ["Box", "interface", "a.Box"],
            ["size", "field", "a.Box.size"],
            ["Box", "class", "a.Box"],
        ]);
    });
});

describe("a reference's kind", () => {
    test("of a function is a call, called or passed", async () => {
        expect(await resolved({
            "a.js": trimIndent(`
                function go() {}
                go();
                [].map(go);
            `)
        }, "a.js")).toEqual([
            ["go", "method", "a.go"],
            ["go", "method-call", "a.go"],
            ["map", "method-call", "Array.map"],
            ["go", "method-call", "a.go"],
        ]);
    });

    test("of a variable or property holding a function is a call", async () => {
        const names = await resolved({
            "a.js": trimIndent(`
                const arrow = () => 1;
                const api = {
                    run: () => 1,
                };
                arrow();
                api.run();
            `)
        }, "a.js");
        expect(names).toEqual(expect.arrayContaining([
            ["arrow", "method", "a.arrow"],
            ["arrow", "method-call", "a.arrow"],
            ["run", "method", "a.api.run"],
            ["run", "method-call", "a.api.run"],
        ]));
    });

    test("of a class is a type reference, and a constructor call under new", async () => {
        const names = await resolved({
            "a.js": trimIndent(`
                class Box {
                    static make() {}
                }
                Box.make();
                new Box();
            `)
        }, "a.js");
        expect(names.filter(([text]) => text === "Box")).toEqual([
            ["Box", "class", "a.Box"],
            ["Box", "type-ref", "a.Box"],
            ["Box", "constructor-call", "a.Box.<init>"],
        ]);
    });

    test("of a class under new on a namespace is a constructor call too", async () => {
        expect(await resolved({
            "lib.js": "export class Widget {}",
            "use.js": trimIndent(`
                import * as lib from "./lib.js";
                new lib.Widget();
            `)
        }, "use.js")).toContainEqual(["Widget", "constructor-call", "lib.Widget.<init>"]);
    });

    test("of a class bound to a variable is a class's", async () => {
        const names = await resolved({
            "a.js": trimIndent(`
                const Box = class {
                    constructor() {}
                };
                new Box();
            `)
        }, "a.js");
        expect(names).toEqual([
            ["Box", "class", "a.Box"],
            ["constructor", "constructor", "a.Box.<constructor>"],
            ["Box", "constructor-call", "a.Box.<init>"],
        ]);
    });

    test("of a class where an interface of the same name comes first is still the class", async () => {
        const names = await resolved({
            "a.ts": trimIndent(`
                interface Widget {
                    size: number;
                }
                class Widget {}
                const w: Widget = new Widget();
            `)
        }, "a.ts");
        expect(names.filter(([text]) => text === "Widget")).toEqual([
            ["Widget", "interface", "a.Widget"],
            ["Widget", "class", "a.Widget"],
            ["Widget", "type-ref", "a.Widget"],
            ["Widget", "constructor-call", "a.Widget.<init>"],
        ]);
    });

    test("under new of a plain function is a call", async () => {
        expect(await resolved({
            "a.js": trimIndent(`
                function Legacy() {}
                new Legacy();
            `)
        }, "a.js")).toContainEqual(["Legacy", "method-call", "a.Legacy"]);
    });

    test("of an interface or a type alias is a type reference", async () => {
        const names = await resolved({
            "a.ts": trimIndent(`
                interface I {}
                type T = string;
                let i: I;
                let t: T;
            `)
        }, "a.ts");
        expect(names).toEqual([
            ["I", "interface", "a.I"],
            ["T", "interface", "a.T"],
            ["i", "field", "a.i"],
            ["I", "type-ref", "a.I"],
            ["t", "field", "a.t"],
            ["T", "type-ref", "a.T"],
        ]);
    });

    test("of an enum is a type reference, and of its member a field access", async () => {
        expect(await resolved({
            "a.ts": trimIndent(`
                enum E {
                    A,
                }
                E.A;
            `)
        }, "a.ts")).toEqual([
            ["E", "enum", "a.E"],
            ["A", "enum-member", "a.E.A"],
            ["E", "type-ref", "a.E"],
            ["A", "field-access", "a.E.A"],
        ]);
    });

    test("of a variable, a property, an accessor or this.x = is a field access", async () => {
        const names = await resolved({
            "a.js": trimIndent(`
                const v = 1;
                class Box {
                    label = 1;
                    constructor() {
                        this.x = 1;
                    }
                    get size() {
                        return 1;
                    }
                }
                const box = new Box();
                v;
                box.label;
                box.x;
                box.size;
            `)
        }, "a.js");
        expect(names).toEqual(expect.arrayContaining([
            ["v", "field-access", "a.v"],
            ["label", "field-access", "a.Box.label"],
            ["x", "field-access", "a.Box.x"],
            ["size", "field-access", "a.Box.size"],
        ]));
    });

    test("of a module is a type reference", async () => {
        expect(await resolved({
            "lib.js": "export const a = 1;",
            "use.js": trimIndent(`
                import * as lib from "./lib.js";
                lib.a;
            `)
        }, "use.js")).toEqual([
            ["lib", "import-ref", "lib.js"],
            [`"./lib.js"`, "import-ref", "lib.js"],
            ["lib", "type-ref", "lib.js"],
            ["a", "field-access", "lib.a"],
        ]);
    });

    test("of a binding, a specifier and a module's string is an import, whatever it names", async () => {
        expect(await resolved({
            "lib.js": trimIndent(`
                export function greet() {}
                export const value = 1;
                export class Box {}
            `),
            "use.js": trimIndent(`
                import {greet, value, Box} from "./lib.js";
                export {greet as hello};
                greet();
                value;
                Box;
            `)
        }, "use.js")).toEqual([
            ["greet", "import-ref", "lib.greet"],
            ["value", "import-ref", "lib.value"],
            ["Box", "import-ref", "lib.Box"],
            [`"./lib.js"`, "import-ref", "lib.js"],
            ["greet", "import-ref", "lib.greet"],
            ["hello", "import-ref", "lib.greet"],
            ["greet", "method-call", "lib.greet"],
            ["value", "field-access", "lib.value"],
            ["Box", "type-ref", "lib.Box"],
        ]);
    });
});

describe("a reference's name", () => {
    test("is its declaration's, however it is reached", async () => {
        const files = {
            "lib.js": trimIndent(`
                export function named() {}
                export default function main() {}
                function hidden() {}
                export {hidden as renamed};
                export const api = {
                    run() {},
                    level: 3,
                };
                export class Widget {
                    constructor() {
                        this.size = 1;
                    }
                    render() {}
                }
            `),
            "barrel.js": `export {named as forwarded} from "./lib.js";`,
            "cjs.js": trimIndent(`
                function helper() {}
                exports.helper = helper;
                exports.value = 1;
                module.exports.other = 2;
            `),
            "legacy.js": trimIndent(`
                function Legacy() {
                    this.name = "x";
                }
                Legacy.prototype.speak = function () {};
                Legacy.create = function () {};
                module.exports = Legacy;
            `),
            "whole.js": trimIndent(`
                module.exports = {
                    alpha() {},
                    beta: () => 1,
                };
            `),
            "use.js": trimIndent(`
                import main, {named, renamed, api, Widget} from "./lib.js";
                import {forwarded} from "./barrel.js";
                import * as lib from "./lib.js";
                main();
                named();
                renamed();
                forwarded();
                api.run();
                api.level;
                new Widget().render();
                new Widget().size;
                lib.named();
            `),
            "require.js": trimIndent(`
                const cjs = require("./cjs.js");
                const {helper} = require("./cjs.js");
                const Legacy = require("./legacy.js");
                const whole = require("./whole.js");
                cjs.helper();
                cjs.value;
                cjs.other;
                helper();
                new Legacy().speak();
                new Legacy().name;
                Legacy.create();
                whole.alpha();
                whole.beta();
            `),
        };
        const parsed = await parseAll(files, Object.keys(files));
        const rows = (await Promise.all(parsed.map(rowsOf))).flat();
        const declarations = rows.filter(([, kind]) => DECLARATION_KINDS.includes(kind));
        const references = rows.filter(([, kind, name]) => !DECLARATION_KINDS.includes(kind) && name !== "Function.prototype");
        expect(references.length).toBeGreaterThan(40);
        expect(references.filter(row => !meets(row, declarations, Object.keys(files)))).toEqual([]);
    });

    test("follows an import through to what it binds", async () => {
        const names = await resolved({
            "lib.js": trimIndent(`
                function hidden() {}
                export {hidden as renamed};
            `),
            "barrel.js": `export {renamed as forwarded} from "./lib.js";`,
            "use.js": trimIndent(`
                import {forwarded} from "./barrel.js";
                forwarded();
            `)
        }, "use.js");
        expect(names).toContainEqual(["forwarded", "method-call", "lib.hidden"]);
    });

    test("goes to the implementation of an overload", async () => {
        const names = await resolved({
            "a.ts": trimIndent(`
                export function go(a: string): void;
                export function go(a: any) {}
            `),
            "use.ts": trimIndent(`
                import {go} from "./a";
                go("x");
            `)
        }, "use.ts");
        expect(names).toContainEqual(["go", "method-call", "a.go"]);
    });

    test("read through a union is the first member that declares it, with its owner", async () => {
        expect(await resolved({
            "a.ts": trimIndent(`
                interface A {
                    shared: number;
                }
                interface B {
                    shared: number;
                }
                declare const either: A | B;
                either.shared;
            `)
        }, "a.ts")).toContainEqual(["shared", "field-access", "a.A.shared"]);
    });

    test("of a destructured property is the property's, and the binding its own", async () => {
        const names = await resolved({
            "a.ts": trimIndent(`
                interface Options {
                    port: number;
                }
                declare const options: Options;
                const {port} = options;
                const {port: renamed} = options;
            `)
        }, "a.ts");
        expect(names).toEqual(expect.arrayContaining([
            ["port", "field-access", "a.Options.port"],
            ["port", "field", "a.port"],
            ["renamed", "field", "a.renamed"],
        ]));
    });

    test("of a require's destructured name is an import of what it reads", async () => {
        const names = await resolved({
            "lib.js": trimIndent(`
                function helper() {}
                module.exports = {helper};
            `),
            "use.js": trimIndent(`
                const {helper} = require("./lib.js");
                helper();
            `)
        }, "use.js");
        expect(names).toEqual([
            ["helper", "import-ref", "lib.helper"],
            [`"./lib.js"`, "import-ref", "lib.js"],
            ["helper", "method-call", "lib.helper"],
        ]);
    });

    test("of a name destructured from a typed require is an import of the property", async () => {
        expect(await resolved({
            "a.ts": trimIndent(`
                interface Lib {
                    helper(): void;
                }
                declare function require(id: string): Lib;
                const {helper} = require("./lib");
            `)
        }, "a.ts")).toEqual(expect.arrayContaining([
            ["helper", "import-ref", "a.Lib.helper"],
            ["helper", "method", "a.helper"],
        ]));
    });

    test("of an object key is the property it fills, and the key a declaration as well", async () => {
        const names = await resolved({
            "a.ts": trimIndent(`
                interface Options {
                    port: number;
                }
                const options: Options = {port: 2};
            `)
        }, "a.ts");
        expect(names.filter(([text]) => text === "port")).toEqual([
            ["port", "field", "a.Options.port"],
            ["port", "field-access", "a.Options.port"],
            ["port", "field", "a.options.port"],
        ]);
    });

    test("of object shorthand is the value's first, and the property's after", async () => {
        expect(await resolved({
            "a.js": trimIndent(`
                const value = 1;
                export const box = {value};
            `)
        }, "a.js")).toEqual([
            ["value", "field", "a.value"],
            ["box", "field", "a.box"],
            ["value", "field-access", "a.value"],
            ["value", "field", "a.box.value"],
        ]);
    });

    test("of a property that restates a value of its name is that value", async () => {
        const names = await resolved({
            "lib.js": trimIndent(`
                function helper() {}
                class Box {}
                module.exports = {helper, Box, other: 1, again: helper};
            `),
        }, "lib.js");
        expect(names.filter(([text]) => text === "helper")).toEqual([
            ["helper", "method", "lib.helper"],
            ["helper", "method-call", "lib.helper"],
            ["helper", "method-call", "lib.helper"],
        ]);
        expect(names.filter(([text]) => text === "Box")).toEqual([
            ["Box", "class", "lib.Box"],
            ["Box", "type-ref", "lib.Box"],
        ]);
    });

    test("of a key restating its value by name is that value too", async () => {
        const names = await resolved({
            "lib.js": trimIndent(`
                function helper() {}
                module.exports = {helper: helper, other: 1};
            `),
        }, "lib.js");
        expect(names.filter(([text]) => text === "helper")).toEqual([
            ["helper", "method", "lib.helper"],
            ["helper", "method-call", "lib.helper"],
            ["helper", "method-call", "lib.helper"],
        ]);
    });

    test("of a JSX tag is the component's, and of an attribute its prop's", async () => {
        const names = await resolved({
            "app.tsx": trimIndent(`
                interface Props {
                    title: string;
                }
                function Child(props: Props) {
                    return null;
                }
                export const app = <Child title="x"></Child>;
            `)
        }, "app.tsx");
        expect(names.filter(([text]) => text === "Child")).toEqual([
            ["Child", "method", "app.Child"],
            ["Child", "method-call", "app.Child"],
            ["Child", "method-call", "app.Child"],
        ]);
        expect(names).toContainEqual(["title", "field-access", "app.Props.title"]);
    });

    test("is left out of a JSX attribute with no prop to fill, which declares nothing", async () => {
        const names = await resolved({
            "app.jsx": trimIndent(`
                const Any = JSON.parse("{}");
                export const app = <Any title="x"/>;
            `)
        }, "app.jsx");
        expect(names.map(([text]) => text)).not.toContain("title");
    });

    test("in a workspace package resolves through its link to the repository", async () => {
        expect(await resolved({
            "packages/lib/package.json": JSON.stringify({name: "@acme/lib", version: "1.0.0", main: "src/index.ts"}),
            "packages/lib/src/index.ts": "export function shared() {}",
            "app/use.ts": trimIndent(`
                import {shared} from "@acme/lib";
                shared();
            `)
        }, "app/use.ts", {"node_modules/@acme/lib": "packages/lib"})).toContainEqual(["shared", "method-call", "packages/lib/src/index.shared"]);
    });
});

describe("a module specifier", () => {
    test("each way of naming a module is an import of it", async () => {
        const names = await resolved({
            "lib.js": "export const a = 1;",
            "use.js": trimIndent(`
                import "./lib.js";
                export {a} from "./lib.js";
                const b = require("./lib.js");
                const c = import("./lib.js");
            `)
        }, "use.js");
        expect(names.filter(([text]) => text === `"./lib.js"`)).toEqual(Array(4).fill([`"./lib.js"`, "import-ref", "lib.js"]));
    });

    test("in a TypeScript file too", async () => {
        const names = await resolved({
            "lib.ts": "export const a = 1;",
            "use.ts": trimIndent(`
                import lib = require("./lib");
                declare const require: any;
                const b = require("./lib");
            `)
        }, "use.ts");
        expect(names.filter(([text]) => text === `"./lib"`)).toEqual(Array(2).fill([`"./lib"`, "import-ref", "lib.ts"]));
    });

    test("augmenting a module in a module is an import of it", async () => {
        const files = {
            "lib.ts": "export interface Box {}",
            "use.ts": trimIndent(`
                export {};
                declare module "./lib" {
                    interface Box {
                        extra: string;
                    }
                }
            `)
        };
        const [, use] = await parseAll(files, ["lib.ts", "use.ts"]);
        expect(await rowsOf(use)).toEqual([
            [`"./lib"`, "import-ref", "lib.ts", null],
            ["Box", "interface", "lib.Box", null],
            ["extra", "field", "lib.Box.extra", null],
        ]);
    });

    test("augmenting a module that is not in the build is an import of the file it resolves to", async () => {
        expect(await resolved({
            "lib.ts": "export interface Box {}",
            "use.ts": trimIndent(`
                export {};
                declare module "./lib" {
                    interface Box {}
                }
            `)
        }, "use.ts")).toContainEqual([`"./lib"`, "import-ref", "lib.ts"]);
    });

    test("declaring a module in a script names the file that declares it", async () => {
        expect(await resolved({
            "shims.d.ts": trimIndent(`
                declare module "untyped" {
                    export function go(): void;
                }
            `),
        }, "shims.d.ts")).toEqual([
            [`"untyped"`, "import-ref", "shims.d.ts"],
            ["go", "method", "shims.d.go"],
        ]);
    });

    test("that resolves to nothing has no row", async () => {
        expect(await resolved({"use.js": `import "./missing.js";`}, "use.js")).toEqual([]);
    });
});

describe("what is left out", () => {
    test("a parameter", async () => {
        expect(await resolved({
            "a.js": trimIndent(`
                function go(p) {
                    return p;
                }
            `)
        }, "a.js")).toEqual([["go", "method", "a.go"]]);
    });

    test("a variable inside a function", async () => {
        expect(await resolved({
            "a.js": trimIndent(`
                function go() {
                    const inner = () => 1;
                    return inner();
                }
            `)
        }, "a.js")).toEqual([["go", "method", "a.go"]]);
    });

    test("a class inside a function, and what it declares", async () => {
        expect(await resolved({
            "a.js": trimIndent(`
                function go() {
                    class Inner {
                        constructor() {
                            this.x = 1;
                        }
                    }
                }
            `)
        }, "a.js")).toEqual([["go", "method", "a.go"]]);
    });

    test("a type parameter of a function, a mapped type or an inferred type", async () => {
        expect((await resolved({
            "a.ts": trimIndent(`
                function go<T>(a: T) {}
                type M = {[K in "a"]: K};
                type I<A> = A extends (infer U)[] ? U : never;
            `)
        }, "a.ts")).map(([text]) => text)).toEqual(["go", "M", "I", "A", "A"]);
    });

    test("a read through an index signature, which declares no name", async () => {
        expect((await resolved({
            "a.ts": trimIndent(`
                interface Bag {
                    [key: string]: number;
                }
                declare const bag: Bag;
                bag.anything;
            `)
        }, "a.ts")).map(([text]) => text)).not.toContain("anything");
    });

    test("a name on a value typed any", async () => {
        const names = await resolved({
            "a.js": trimIndent(`
                const value = JSON.parse('{}');
                value.member;
            `)
        }, "a.js");
        expect(names.map(([text]) => text)).toEqual(["value", "JSON", "parse", "value"]);
    });

    test("a name that resolves to nothing", async () => {
        expect(await resolved({"a.js": "undeclared();"}, "a.js")).toEqual([]);
    });
});

describe("a binding to a module that does not resolve", () => {
    test("declares itself, as a require of the platform does where its types are not installed", async () => {
        expect(await resolved({
            "use.js": trimIndent(`
                const http = require("node:http");
                http.createServer();
            `)
        }, "use.js")).toEqual([
            ["http", "field", "use.http"],
            ["http", "field-access", "use.http"],
        ]);
    });

    test("declares each name an import binds, though not the names it reads", async () => {
        expect(await resolved({
            "use.js": trimIndent(`
                import x, {named as local} from "unresolvable";
                import * as ns from "unresolvable";
                x.go();
                local();
                ns.member;
            `)
        }, "use.js")).toEqual([
            ["x", "field", "use.x"],
            ["local", "field", "use.local"],
            ["ns", "field", "use.ns"],
            ["x", "field-access", "use.x"],
            ["local", "field-access", "use.local"],
            ["ns", "field-access", "use.ns"],
        ]);
    });

    test("declares a member it binds by name", async () => {
        expect(await resolved({
            "use.js": trimIndent(`
                const resolve = require("node:path").resolve;
                const {join, sep: separator} = require("node:path");
                resolve("a");
                join("b");
                separator;
            `)
        }, "use.js")).toEqual([
            ["resolve", "field", "use.resolve"],
            ["join", "field", "use.join"],
            ["separator", "field", "use.separator"],
            ["resolve", "field-access", "use.resolve"],
            ["join", "field-access", "use.join"],
            ["separator", "field-access", "use.separator"],
        ]);
    });

    test("is what an export of it goes to, from wherever it is read", async () => {
        const files = {
            "lib.js": trimIndent(`
                const http = require("node:http");
                exports.server = http;
            `),
            "use.js": trimIndent(`
                const lib = require("./lib.js");
                lib.server;
            `),
        };
        const [lib, use] = await parseAll(files, ["lib.js", "use.js"]);
        expect(await rowsOf(lib)).toEqual([
            ["http", "field", "lib.http", null],
            ["exports", "type-ref", "lib.js", null],
            ["server", "import-ref", "lib.http", null],
            ["http", "field-access", "lib.http", null],
        ]);
        expect(await rowsOf(use)).toContainEqual(["server", "field-access", "lib.http", null]);
    });

    test("inside a function is local to it", async () => {
        expect(await resolved({
            "a.js": trimIndent(`
                function go() {
                    const http = require("node:http");
                    return http;
                }
            `)
        }, "a.js")).toEqual([["go", "method", "a.go"]]);
    });

    test("that re-exports declares nothing, as there is nothing here it binds", async () => {
        expect(await resolved({
            "lib.js": trimIndent(`
                export {re} from "unresolvable";
                export * as all from "unresolvable";
            `)
        }, "lib.js")).toEqual([]);
    });
});

describe("a name in a package", () => {
    const typedSource = {
        "package.json": JSON.stringify({name: "typed", version: "1.2.3", types: "index.d.ts"}),
        "index.d.ts": trimIndent(`
            export declare function go(): void;
            export declare class Box {
                constructor(size: number);
                size: number;
                open(): void;
            }
            export interface Lid {
                closed: boolean;
            }
            export declare namespace ns {
                function deep(): void;
                interface Inner {}
            }
        `),
    };
    const typedPackage = {
        "node_modules/typed/package.json": typedSource["package.json"],
        "node_modules/typed/index.d.ts": typedSource["index.d.ts"],
    };
    const useTyped = trimIndent(`
        import {go} from "typed";
        go();
    `);

    test("carries the package that declares it", async () => {
        expect(await packaged({...typedPackage, "use.js": useTyped}, "use.js")).toEqual([
            ["go", "import-ref", "index.d.go", "typed"],
            [`"typed"`, "import-ref", "index.d.ts", "typed"],
            ["go", "method-call", "index.d.go", "typed"],
        ]);
    });

    test("is what the parser names it when the package is itself the source", async () => {
        const declared: string[] = [];
        await new class extends JavaScriptVisitor<void> {
            protected override async visitClassDeclaration(type: J.ClassDeclaration, p: void) {
                declared.push(Type.FullyQualified.getFullyQualifiedName(type.name.type as Type.FullyQualified));
                return super.visitClassDeclaration(type, p);
            }

            protected override async visitMethodDeclaration(method: J.MethodDeclaration, p: void) {
                const {declaringType, name} = method.methodType!;
                declared.push(`${Type.FullyQualified.getFullyQualifiedName(declaringType)}.${name}`);
                return super.visitMethodDeclaration(method, p);
            }
        }().visit(await parse(typedSource, "index.d.ts", {}), undefined);
        const names = await packaged({
            ...typedPackage,
            "use.ts": trimIndent(`
                import {go, Box, Lid, ns} from "typed";
                go();
                ns.deep();
                const box = new Box(1);
                box.open();
                let lid: Lid;
                let inner: ns.Inner;
            `)
        }, "use.ts");
        expect(declared).toEqual(expect.arrayContaining(["index.d.go", "index.Box", "index.Box.open", "ns.deep", "index.Lid"]));
        expect(names.map(([, , name]) => name)).toEqual(expect.arrayContaining(declared.filter(name => !name.endsWith(".<constructor>"))));
    });

    test("of a member is its owner's and its own", async () => {
        const names = await packaged({
            ...typedPackage,
            "use.ts": trimIndent(`
                import {Box, Lid} from "typed";
                new Box(1).size;
                declare const lid: Lid;
                lid.closed;
            `)
        }, "use.ts");
        expect(names).toEqual(expect.arrayContaining([
            ["Box", "constructor-call", "index.Box.<init>", "typed"],
            ["size", "field-access", "index.Box.size", "typed"],
            ["closed", "field-access", "index.Lid.closed", "typed"],
        ]));
    });

    test("of a namespace is its file, where a bare name would match anything of that name", async () => {
        const names = await packaged({
            ...typedPackage,
            "use.ts": trimIndent(`
                import {ns} from "typed";
                ns.deep();
            `)
        }, "use.ts");
        expect(names).toEqual([
            ["ns", "import-ref", "index.d.ts", "typed"],
            [`"typed"`, "import-ref", "index.d.ts", "typed"],
            ["ns", "type-ref", "index.d.ts", "typed"],
            ["deep", "method-call", "ns.deep", "typed"],
        ]);
    });

    test("keeps its own name on what a namespace holds, which is not its file's", async () => {
        const names = await packaged({
            "node_modules/held/package.json": JSON.stringify({name: "held", version: "1.0.0", types: "index.d.ts"}),
            "node_modules/held/index.d.ts": trimIndent(`
                export declare namespace ns {
                    const value: number;
                }
            `),
            "use.ts": trimIndent(`
                import {ns} from "held";
                ns.value;
            `)
        }, "use.ts");
        expect(names).toContainEqual(["value", "field-access", "index.ns.value", "held"]);
    });

    test("of a property or a variable holding a function is a call, as a function is", async () => {
        const names = await packaged({
            "node_modules/held/package.json": JSON.stringify({name: "held", version: "1.0.0", types: "index.d.ts"}),
            "node_modules/held/index.d.ts": trimIndent(`
                export interface Options {
                    read: () => void;
                    open(): void;
                }
                export declare const run: () => void;
            `),
            "use.ts": trimIndent(`
                import {Options, run} from "held";
                declare const options: Options;
                options.read();
                options.open();
                run();
            `)
        }, "use.ts");
        expect(names).toEqual(expect.arrayContaining([
            ["read", "method-call", "index.Options.read", "held"],
            ["open", "method-call", "index.Options.open", "held"],
            ["run", "method-call", "index.d.run", "held"],
        ]));
    });

    test("of a global variable holding a function is the method its own build declares", async () => {
        const source = {
            "package.json": JSON.stringify({name: "@types/node", version: "22.10.0", types: "module.d.ts"}),
            "module.d.ts": trimIndent(`
                declare module "module" {
                    global {
                        namespace NodeJS {
                            interface Require {
                                (id: string): any;
                            }
                        }
                        var require: NodeJS.Require;
                    }
                }
            `),
        };
        const references = (await packaged({
            ...installed("@types/node", source),
            "lib.js": "exports.a = 1;",
            "use.js": `require("./lib.js");`,
        }, "use.js")).filter(([, , , pkg]) => pkg === "@types/node");
        const declarations = await ownDeclarations(source);
        expect(references).toEqual([["require", "method-call", "module.d.require", "@types/node"]]);
        expect(references.filter(reference => !meets(reference, declarations, Object.keys(source)))).toEqual([]);
    });

    test("of a property whose type is callable is the method its own build declares", async () => {
        const source = {
            "package.json": JSON.stringify({name: "@types/router", version: "1.0.0", types: "index.d.ts"}),
            "index.d.ts": trimIndent(`
                export interface Handler {
                    (request: unknown, next: () => void): void;
                }
                export interface Matcher<T> {
                    (path: string, ...handlers: Handler[]): T;
                }
                export interface Router {
                    get: Matcher<this>;
                    use: Matcher<this> & ((...handlers: Handler[]) => this);
                }
            `),
        };
        const references = (await packaged({
            ...installed("@types/router", source),
            "use.ts": trimIndent(`
                import type {Router} from "router";
                declare const app: Router;
                app.get("/", (request, next) => next()).use((request, next) => next());
            `),
        }, "use.ts")).filter(([, , , pkg]) => pkg === "@types/router");
        const declarations = await ownDeclarations(source);
        expect(references).toEqual(expect.arrayContaining([
            ["get", "method-call", "index.Router.get", "@types/router"],
            ["use", "method-call", "index.Router.use", "@types/router"],
        ]));
        expect(references.filter(reference => !meets(reference, declarations, Object.keys(source)))).toEqual([]);
    });

    test("of a callable value given to export = is the method its own build declares", async () => {
        const source = {
            "package.json": JSON.stringify({name: "@types/errors", version: "2.0.0", types: "index.d.ts"}),
            "index.d.ts": trimIndent(`
                export = createError;
                declare const createError: createError.Create & {
                    isError(value: unknown): boolean;
                };
                declare namespace createError {
                    type Create = (status?: number) => Error;
                }
            `),
        };
        const references = (await packaged({
            ...installed("@types/errors", source),
            "use.js": trimIndent(`
                const createError = require("errors");
                createError(404);
            `),
        }, "use.js")).filter(([, , , pkg]) => pkg === "@types/errors");
        const declarations = await ownDeclarations(source);
        expect(references).toContainEqual(["createError", "method-call", "index.d.createError", "@types/errors"]);
        expect(references.filter(reference => !meets(reference, declarations, Object.keys(source)))).toEqual([]);
    });

    test("keeps the scope of a scoped package", async () => {
        expect(await packaged({
            "node_modules/@acme/typed/package.json": JSON.stringify({name: "@acme/typed", version: "2.0.0", types: "lib/index.d.ts"}),
            "node_modules/@acme/typed/lib/index.d.ts": "export declare function go(): void;",
            "use.js": trimIndent(`
                import {go} from "@acme/typed";
                go();
            `)
        }, "use.js")).toContainEqual(["go", "method-call", "lib/index.d.go", "@acme/typed"]);
    });

    test("of a DefinitelyTyped package carries its own id", async () => {
        expect(await packaged({
            "node_modules/@types/typed/package.json": JSON.stringify({name: "@types/typed", version: "1.0.0", types: "index.d.ts"}),
            "node_modules/@types/typed/index.d.ts": "export declare function go(): void;",
            "use.ts": trimIndent(`
                import {go} from "typed";
                go();
            `)
        }, "use.ts")).toContainEqual(["go", "method-call", "index.d.go", "@types/typed"]);
    });

    test("in a content addressed store is named as any other", async () => {
        expect(await packaged({
            "node_modules/.pnpm/typed@1.2.3/node_modules/typed/package.json": typedPackage["node_modules/typed/package.json"],
            "node_modules/.pnpm/typed@1.2.3/node_modules/typed/index.d.ts": typedPackage["node_modules/typed/index.d.ts"],
            "use.js": useTyped
        }, "use.js", {"node_modules/typed": "node_modules/.pnpm/typed@1.2.3/node_modules/typed"}))
            .toContainEqual(["go", "method-call", "index.d.go", "typed"]);
    });

    test("of an ambient module is its file, and of its function the file's module", async () => {
        const names = await packaged({
            "node_modules/@types/node/package.json": JSON.stringify({name: "@types/node", version: "22.10.0", types: "fs.d.ts"}),
            "node_modules/@types/node/fs.d.ts": trimIndent(`
                declare module "fs" {
                    export function readFileSync(path: string): string;
                }
            `),
            "use.js": trimIndent(`
                const fs = require("fs");
                fs.readFileSync("x");
            `)
        }, "use.js");
        expect(names).toEqual([
            ["fs", "import-ref", "fs.d.ts", "@types/node"],
            [`"fs"`, "import-ref", "fs.d.ts", "@types/node"],
            ["fs", "type-ref", "fs.d.ts", "@types/node"],
            ["readFileSync", "method-call", "fs.d.readFileSync", "@types/node"],
        ]);
    });

    test("in the default library has no package, and keeps its global name", async () => {
        const names = await packaged({"a.js": "[1].map(String); parseInt('1');"}, "a.js");
        expect(names).toEqual([
            ["map", "method-call", "Array.map", null],
            ["String", "type-ref", "String", null],
            ["parseInt", "method-call", "lib/lib.es5.d.parseInt", null],
        ]);
    });

    test("in the default library is a call where a property holds a function too", async () => {
        expect(await packaged({"a.js": "document.onclick;"}, "a.js"))
            .toContainEqual(["onclick", "method-call", "GlobalEventHandlers.onclick", null]);
    });

    test("of a namespace in the default library is its file", async () => {
        expect((await packaged({"a.js": "Reflect.ownKeys({});"}, "a.js"))[0])
            .toEqual(["Reflect", "type-ref", "lib/lib.es2015.reflect.d.ts", null]);
    });

    test("under new of a global declared by an interface and a variable is a constructor call", async () => {
        expect(await packaged({"a.js": "new Date();"}, "a.js")).toEqual([["Date", "constructor-call", "Date.<init>", null]]);
    });

    test("in an interface named after its constructor keeps the parser's spelling", async () => {
        expect(await packaged({"a.js": "Object.keys({});"}, "a.js")).toContainEqual(["keys", "method-call", "Object.keys", null]);
    });

    test("bound to an untyped package is that package's module", async () => {
        const main = "lib/main.js";
        expect(await packaged({
            "node_modules/untyped/package.json": JSON.stringify({name: "untyped", version: "4.5.6", main: "lib/main.js"}),
            "node_modules/untyped/lib/main.js": "module.exports = function () {};",
            "use.js": trimIndent(`
                import imported from "untyped";
                imported();
                const required = require("untyped");
                required.member;
            `)
        }, "use.js")).toEqual([
            ["imported", "import-ref", main, "untyped"],
            [`"untyped"`, "import-ref", main, "untyped"],
            ["imported", "type-ref", main, "untyped"],
            ["required", "import-ref", main, "untyped"],
            [`"untyped"`, "import-ref", main, "untyped"],
            ["required", "type-ref", main, "untyped"],
        ]);
    });

    test("exported from a binding to an untyped package is that package's module", async () => {
        expect(await packaged({
            "node_modules/untyped/package.json": JSON.stringify({name: "untyped", version: "4.5.6", main: "main.js"}),
            "node_modules/untyped/main.js": "module.exports = function () {};",
            "use.js": trimIndent(`
                const Router = require("untyped");
                exports.Router = Router;
                exports.Route = Router.Route;
            `)
        }, "use.js")).toContainEqual(["Route", "import-ref", "main.js", "untyped"]);
    });

    test("with no name in its manifest goes by the directory it is installed in", async () => {
        expect(await packaged({
            "node_modules/@acme/nameless/package.json": JSON.stringify({version: "3.0.0", types: "index.d.ts"}),
            "node_modules/@acme/nameless/index.d.ts": "export declare function go(): void;",
            "use.ts": trimIndent(`
                import {go} from "@acme/nameless";
                go();
            `)
        }, "use.ts")).toContainEqual(["go", "method-call", "index.d.go", "@acme/nameless"]);
    });

    test("with no version in its manifest is named all the same, since the chain has the version", async () => {
        expect(await packaged({
            "node_modules/versionless/package.json": JSON.stringify({name: "versionless", types: "index.d.ts"}),
            "node_modules/versionless/index.d.ts": "export declare function go(): void;",
            "use.ts": trimIndent(`
                import {go} from "versionless";
                go();
            `)
        }, "use.ts")).toContainEqual(["go", "method-call", "index.d.go", "versionless"]);
    });
});
