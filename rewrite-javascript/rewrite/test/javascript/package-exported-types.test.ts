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
import * as fs from "fs";
import * as path from "path";
import {withDir} from "tmp-promise";
import {exportedTypes} from "../../src/javascript/package-exported-types";
import {JavaScriptTypeMapping} from "../../src/javascript/type-mapping";
import {Type} from "../../src/java";
import {DependencyTypes} from "../../src/rpc/request/dependency-types";
import {setLastParsedProject} from "../../src/rpc/request/last-parsed-project";
import {RpcObjectData, RpcObjectState} from "../../src/rpc/queue";

const ENTRY = `
export declare class Greeter {
    name: string;
    constructor(name: string);
    greet(greeting: string): string;
    reset(): void;
}

export interface Repository<T> {
    find(id: string): T | undefined;
    save(entity: T): void;
}

export declare enum Color { Red, Green, Blue }

export type Alias = Greeter;
`;

/** Write a package under `<root>/node_modules/<name>` from a map of relative path -> content. */
function writeFiles(root: string, name: string, files: Record<string, string>): {pkg: string, nodeModules: string} {
    const nodeModules = path.join(root, "node_modules");
    const pkg = path.join(nodeModules, name);
    for (const [rel, content] of Object.entries(files)) {
        const p = path.join(pkg, rel);
        fs.mkdirSync(path.dirname(p), {recursive: true});
        fs.writeFileSync(p, content);
    }
    return {pkg, nodeModules};
}

function writePackage(root: string): {pkg: string, nodeModules: string} {
    return writeFiles(root, "exported-types-fixture", {
        "package.json": JSON.stringify({name: "exported-types-fixture", version: "1.0.0", types: "index.d.ts"}),
        "index.d.ts": ENTRY,
    });
}

function fqns(types: Type.FullyQualified[]): string[] {
    return types.map(t => Type.FullyQualified.getFullyQualifiedName(t));
}

describe("exportedTypes", () => {
    test("enumerates own public types with populated methods", async () => {
        await withDir(async (root) => {
            const {pkg, nodeModules} = writePackage(root.path);

            const types = exportedTypes([pkg], [nodeModules]);
            const byFqn = new Map(types.map(t => [Type.FullyQualified.getFullyQualifiedName(t), t]));

            const greeter = byFqn.get("exported-types-fixture.Greeter");
            expect(greeter).toBeDefined();
            expect(Type.isClass(greeter!)).toBe(true);
            const greeterClass = greeter as Type.Class;
            expect(greeterClass.classKind).toBe(Type.Class.Kind.Class);

            // The gap fix: methods must come out non-empty (the normal mapping path skips them).
            const methodNames = greeterClass.methods.map(m => m.name);
            expect(methodNames).toContain("greet");
            expect(methodNames).toContain("reset");
            expect(methodNames).toContain("<constructor>");

            const greet = greeterClass.methods.find(m => m.name === "greet")!;
            expect(greet.declaringType).toBe(greeterClass);
            expect(greet.parameterNames).toEqual(["greeting"]);

            const repo = byFqn.get("exported-types-fixture.Repository") as Type.Class | undefined;
            expect(repo).toBeDefined();
            expect(repo!.methods.map(m => m.name).sort()).toEqual(["find", "save"]);

            expect(byFqn.has("exported-types-fixture.Color")).toBe(true);
        }, {unsafeCleanup: true});
    }, 60000);

    test("resolves the declaration entry from an exports types condition", async () => {
        // Modern package: no top-level `types`, no root index.d.ts; types live only under
        // exports["."].{import,require}.types.
        await withDir(async (root) => {
            const {pkg, nodeModules} = writeFiles(root.path, "exports-fixture", {
                "package.json": JSON.stringify({
                    name: "exports-fixture", version: "1.0.0",
                    exports: {".": {
                        import: {types: "./dist/index.d.ts", default: "./dist/index.js"},
                        require: {types: "./dist/index.d.cts", default: "./dist/index.cjs"},
                    }},
                }),
                "dist/index.d.ts": `export declare class Widget { render(): void; }\nexport interface Opts { flag: boolean; }\n`,
            });

            const names = fqns(exportedTypes([pkg], [nodeModules]));
            expect(names).toContain("exports-fixture.Widget");
            expect(names).toContain("exports-fixture.Opts");
        }, {unsafeCleanup: true});
    }, 60000);

    test("merged interface declarations keep methods from every declaration", async () => {
        await withDir(async (root) => {
            const {pkg, nodeModules} = writeFiles(root.path, "merge-fixture", {
                "package.json": JSON.stringify({name: "merge-fixture", version: "1.0.0", types: "index.d.ts"}),
                "index.d.ts": `export interface Req { a(): void; }\nexport interface Req { b(): void; }\n`,
            });

            const types = exportedTypes([pkg], [nodeModules]);
            const req = types.find(t => Type.FullyQualified.getFullyQualifiedName(t) === "merge-fixture.Req") as Type.Class | undefined;
            expect(req).toBeDefined();
            expect(req!.methods.map(m => m.name).sort()).toEqual(["a", "b"]);
        }, {unsafeCleanup: true});
    }, 60000);

    test("isolates a throwing export and continues with the rest", async () => {
        await withDir(async (root) => {
            const {pkg, nodeModules} = writeFiles(root.path, "isolation-fixture", {
                "package.json": JSON.stringify({name: "isolation-fixture", version: "1.0.0", types: "index.d.ts"}),
                "index.d.ts": `export declare class Good { ok(): void; }\nexport declare class Bad { no(): void; }\n`,
            });

            // The "deliberately odd export": force mapping of Bad to throw and assert Good survives.
            const original = JavaScriptTypeMapping.prototype.declarationType;
            const spy = vi.spyOn(JavaScriptTypeMapping.prototype, "declarationType")
                .mockImplementation(function (this: JavaScriptTypeMapping, node: any) {
                    if (node?.name?.getText?.() === "Bad") {
                        throw new Error("deliberately odd export");
                    }
                    return original.call(this, node);
                });
            try {
                const names = fqns(exportedTypes([pkg], [nodeModules]));
                expect(names).toContain("isolation-fixture.Good");
                expect(names).not.toContain("isolation-fixture.Bad");
            } finally {
                spy.mockRestore();
            }
        }, {unsafeCleanup: true});
    }, 60000);

    test("enumerates an ambient-module-only declaration entry", async () => {
        // Script/ambient entry: no top-level export, only `declare module "x" {}`.
        await withDir(async (root) => {
            const {pkg, nodeModules} = writeFiles(root.path, "ambient-fixture", {
                "package.json": JSON.stringify({name: "ambient-fixture", version: "1.0.0", types: "index.d.ts"}),
                "index.d.ts": `declare module "ambient-fixture" {\n  export class Ambient { ping(): void; }\n  export interface Cfg { on: boolean; }\n}\n`,
            });

            const names = fqns(exportedTypes([pkg], [nodeModules]));
            expect(names).toContain("ambient-fixture.Ambient");
            expect(names).toContain("ambient-fixture.Cfg");
        }, {unsafeCleanup: true});
    }, 60000);

    test("enumerates ambient global types from a declaration FILE by bare FQN", async () => {
        // A lib.*.d.ts FILE of ambient globals (no module export) is enumerated directly. Its
        // types must come out with bare FQNs — the same names the parser emits at a use site.
        await withDir(async (root) => {
            const lib = path.join(root.path, "lib.fake.d.ts");
            fs.writeFileSync(lib, `
interface FakeArray<T> { push(item: T): number; length: number; }
interface FakePromise<T> { then(cb: (v: T) => void): FakePromise<T>; }
declare namespace FakeIntl { interface FakeFormat { format(n: number): string; } }
`);

            const names = fqns(exportedTypes([lib], [lib]));
            expect(names).toContain("FakeArray");
            expect(names).toContain("FakePromise");
            expect(names).toContain("FakeIntl.FakeFormat");

            const arr = exportedTypes([lib], [lib])
                .find(t => Type.FullyQualified.getFullyQualifiedName(t) === "FakeArray") as Type.Class | undefined;
            expect(arr).toBeDefined();
            expect(arr!.methods.map(m => m.name)).toContain("push");
        }, {unsafeCleanup: true});
    }, 60000);

    test("default-exported class uses the same FQN as the use site", async () => {
        // Parity verified against the mapper's use-site FQN: both resolve to <pkg>.<class name>.
        await withDir(async (root) => {
            const {pkg, nodeModules} = writeFiles(root.path, "default-fixture", {
                "package.json": JSON.stringify({name: "default-fixture", version: "1.0.0", types: "index.d.ts"}),
                "index.d.ts": `export default class Foo { bar(): void; }\n`,
            });

            const types = exportedTypes([pkg], [nodeModules]);
            const foo = types.find(t => Type.FullyQualified.getFullyQualifiedName(t) === "default-fixture.Foo") as Type.Class | undefined;
            expect(foo).toBeDefined();
            expect(foo!.methods.map(m => m.name)).toContain("bar");
        }, {unsafeCleanup: true});
    }, 60000);
});

/** Register a DependencyTypes handler on a mock connection and return the captured request handler. */
function captureHandler(batchSize: number): (req: any) => Promise<RpcObjectData[]> {
    let handler: ((req: any) => Promise<RpcObjectData[]>) | undefined;
    const connection = {onRequest: (_t: any, h: any) => { handler = h; }} as any;
    DependencyTypes.handle(connection, batchSize);
    return handler!;
}

describe("DependencyTypes.handle", () => {
    test("resolves an npm coordinate and returns batchSize slices that reassemble the single-shot output", async () => {
        await withDir(async (root) => {
            writePackage(root.path);
            setLastParsedProject(root.path);
            const req = {name: "exported-types-fixture", version: "1.0.0"};

            // Single-shot reference: whole list in one response.
            const singleShot = await captureHandler(100000)(req);
            expect(singleShot.length).toBeGreaterThan(2);
            expect(singleShot.some(d => d.value === "exported-types-fixture.Greeter")).toBe(true);
            expect(singleShot[singleShot.length - 1].state).toBe(RpcObjectState.END_OF_OBJECT);

            // Paginated: drain 2-at-a-time until the slice carrying END_OF_OBJECT.
            const handler = captureHandler(2);
            const slices: RpcObjectData[][] = [];
            const concatenated: RpcObjectData[] = [];
            for (;;) {
                const batch = await handler(req);
                slices.push(batch);
                concatenated.push(...batch);
                if (batch.some(d => d.state === RpcObjectState.END_OF_OBJECT)) {
                    break;
                }
            }

            // (a) every slice respects batchSize
            expect(slices.length).toBeGreaterThan(1);
            for (const s of slices) {
                expect(s.length).toBeLessThanOrEqual(2);
            }

            // (b) concatenation equals the single-shot output, END_OF_OBJECT last
            expect(concatenated).toEqual(singleShot);
            expect(concatenated[concatenated.length - 1].state).toBe(RpcObjectState.END_OF_OBJECT);

            // (c) the pending entry is evicted after the final slice: a fresh request rebuilds
            // and returns the first batch again (a non-evicted map would splice an empty slice).
            const afterDrain = await handler(req);
            expect(afterDrain.length).toBeGreaterThan(0);
            expect(afterDrain).toEqual(slices[0]);
        }, {unsafeCleanup: true});
    }, 60000);

    test("resolves a scoped package directory", async () => {
        await withDir(async (root) => {
            writeFiles(root.path, "@scope/scoped-fixture", {
                "package.json": JSON.stringify({name: "@scope/scoped-fixture", version: "1.0.0", types: "index.d.ts"}),
                "index.d.ts": `export declare class Thing { spin(): void; }\n`,
            });
            setLastParsedProject(root.path);

            const data = await captureHandler(100000)({name: "@scope/scoped-fixture", version: "1.0.0"});
            expect(data.some(d => typeof d.value === "string" && d.value.endsWith(".Thing"))).toBe(true);
        }, {unsafeCleanup: true});
    }, 60000);

    test("falls back to the DefinitelyTyped package when the package ships no types", async () => {
        await withDir(async (root) => {
            writeFiles(root.path, "plainlib", {
                "package.json": JSON.stringify({name: "plainlib", version: "2.0.0", main: "index.js"}),
                "index.js": `module.exports.helper = function () {};\n`,
            });
            writeFiles(root.path, "@types/plainlib", {
                "package.json": JSON.stringify({name: "@types/plainlib", version: "2.0.0", types: "index.d.ts"}),
                "index.d.ts": `export declare class Helper { assist(): void; }\n`,
            });
            setLastParsedProject(root.path);

            const data = await captureHandler(100000)({name: "plainlib", version: "2.0.0"});
            expect(data.some(d => d.value === "plainlib.Helper")).toBe(true);
        }, {unsafeCleanup: true});
    }, 60000);

    test("maps a scoped package to its @types/scope__pkg fallback", async () => {
        await withDir(async (root) => {
            writeFiles(root.path, "@scope/untyped", {
                "package.json": JSON.stringify({name: "@scope/untyped", version: "1.0.0", main: "index.js"}),
                "index.js": `module.exports = {};\n`,
            });
            writeFiles(root.path, "@types/scope__untyped", {
                "package.json": JSON.stringify({name: "@types/scope__untyped", version: "1.0.0", types: "index.d.ts"}),
                "index.d.ts": `export declare class Untyped { u(): void; }\n`,
            });
            setLastParsedProject(root.path);

            const data = await captureHandler(100000)({name: "@scope/untyped", version: "1.0.0"});
            expect(data.some(d => typeof d.value === "string" && d.value.endsWith(".Untyped"))).toBe(true);
        }, {unsafeCleanup: true});
    }, 60000);

    test("resolves a runtime declaration file from the project's installed TypeScript", async () => {
        await withDir(async (root) => {
            writeFiles(root.path, "typescript", {
                "lib/lib.fake.d.ts": `interface FakeThing { poke(): void; }\n`,
            });
            setLastParsedProject(root.path);

            const data = await captureHandler(100000)({name: "lib.fake", version: null});
            expect(data.some(d => d.value === "FakeThing")).toBe(true);
        }, {unsafeCleanup: true});
    }, 60000);

    test("falls back to the engine's own TypeScript lib directory", async () => {
        await withDir(async (root) => {
            // No node_modules/typescript in the project; lib.decorators comes from the engine's.
            setLastParsedProject(root.path);

            const data = await captureHandler(100000)({name: "lib.decorators"});
            expect(data.some(d => d.value === "ClassDecoratorContext")).toBe(true);
        }, {unsafeCleanup: true});
    }, 60000);

    test("throws for a package that is not installed", async () => {
        await withDir(async (root) => {
            setLastParsedProject(root.path);
            await expect(captureHandler(100)({name: "not-installed", version: "1.0.0"}))
                .rejects.toThrow(/not installed/);
            await expect(captureHandler(100)({name: "lib.no-such-lib"}))
                .rejects.toThrow(/No TypeScript declaration file/);
        }, {unsafeCleanup: true});
    }, 60000);
});
