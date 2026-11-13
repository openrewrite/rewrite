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
            const warn = vi.spyOn(console, "warn").mockImplementation(() => {});
            try {
                const names = fqns(exportedTypes([pkg], [nodeModules]));
                expect(names).toContain("isolation-fixture.Good");
                expect(names).not.toContain("isolation-fixture.Bad");
                expect(warn).toHaveBeenCalledWith(
                    "exportedTypes: skipping export 'Bad': Error: deliberately odd export");
            } finally {
                spy.mockRestore();
                warn.mockRestore();
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

    test("resolves types published only under a require condition", async () => {
        await withDir(async (root) => {
            const {pkg, nodeModules} = writeFiles(root.path, "cjs-fixture", {
                "package.json": JSON.stringify({
                    name: "cjs-fixture", version: "1.0.0",
                    exports: {".": {require: {types: "./dist/index.d.cts", default: "./dist/index.cjs"}}},
                }),
                "dist/index.d.cts": `export declare class CjsWidget { w(): void; }\n`,
                "dist/index.cjs": ``,
            });

            expect(fqns(exportedTypes([pkg], [nodeModules]))).toContain("cjs-fixture.CjsWidget");
        }, {unsafeCleanup: true});
    }, 60000);

    test("takes the entry from the package's own directory, symlinked or not", async () => {
        await withDir(async (root) => {
            // npm alias: the directory name differs from the package's own declared name.
            const {pkg: aliased} = writeFiles(root.path, "alias-fixture", {
                "package.json": JSON.stringify({name: "real-fixture", version: "1.0.0", types: "index.d.ts"}),
                "index.d.ts": `export declare class FromAliasCopy { a(): void; }\n`,
            });
            writeFiles(root.path, "real-fixture", {
                "package.json": JSON.stringify({name: "real-fixture", version: "2.0.0", types: "index.d.ts"}),
                "index.d.ts": `export declare class FromRealCopy { r(): void; }\n`,
            });
            expect(fqns(exportedTypes([aliased], []))).toEqual(["alias-fixture.FromAliasCopy", "alias-fixture", "npm:real-fixture"]);

            // pnpm: node_modules/<name> is a symlink into the content-addressed store.
            const {pkg: stored} = writeFiles(root.path, path.join(".pnpm", "pnpm-fixture@1.0.0", "node_modules", "pnpm-fixture"), {
                "package.json": JSON.stringify({
                    name: "pnpm-fixture", version: "1.0.0",
                    exports: {".": {types: "./dist/index.d.ts", default: "./dist/index.js"}},
                }),
                "dist/index.d.ts": `export declare class PnpmWidget { p(): void; }\n`,
            });
            const link = path.join(root.path, "node_modules", "pnpm-fixture");
            fs.symlinkSync(stored, link, "dir");
            expect(fqns(exportedTypes([link], []))).toContain("pnpm-fixture.PnpmWidget");
        }, {unsafeCleanup: true});
    }, 60000);


    test("falls back to the declared types file when an exports map has no types condition", async () => {
        await withDir(async (root) => {
            const {pkg, nodeModules} = writeFiles(root.path, "mispublished-fixture", {
                "package.json": JSON.stringify({
                    name: "mispublished-fixture", version: "1.0.0",
                    types: "dist/types.d.ts",
                    exports: {".": "./dist/index.js"},
                }),
                "dist/index.js": ``,
                "dist/types.d.ts": `export declare class Mis { m(): void; }\n`,
            });

            expect(fqns(exportedTypes([pkg], [nodeModules]))).toContain("mispublished-fixture.Mis");
        }, {unsafeCleanup: true});
    }, 60000);


    test("types a transitive dependency through its node + import exports conditions", async () => {
        await withDir(async (root) => {
            writeFiles(root.path, "modern-dep", {
                "package.json": JSON.stringify({
                    name: "modern-dep", version: "1.0.0",
                    exports: {".": {
                        browser: {types: "./dist/browser.d.ts", default: "./dist/browser.js"},
                        node: {
                            import: {types: "./dist/node.d.ts", default: "./dist/node.mjs"},
                            require: {types: "./dist/node.d.cts", default: "./dist/node.cjs"},
                        },
                    }},
                }),
                "dist/node.d.ts": `export declare class Widget { render(): void; }\n`,
                "dist/node.d.cts": `export declare class RequireWidget { render(): void; }\n`,
                "dist/browser.d.ts": `export declare class BrowserWidget { render(): void; }\n`,
            });
            const {pkg, nodeModules} = writeFiles(root.path, "consumer-fixture", {
                "package.json": JSON.stringify({name: "consumer-fixture", version: "1.0.0", types: "index.d.ts"}),
                "index.d.ts": `import {Widget} from "modern-dep";\nexport declare class Consumer { widget(): Widget; }\n`,
            });

            const consumer = exportedTypes([pkg], [nodeModules])
                .find(t => Type.FullyQualified.getFullyQualifiedName(t) === "consumer-fixture.Consumer") as Type.Class;
            const widget = consumer.methods.find(m => m.name === "widget")!;
            // Only the node/import branch declares `Widget`, so its FQN pins which conditions matched.
            expect(Type.signature(widget.returnType)).toBe("modern-dep.Widget");
        }, {unsafeCleanup: true});
    }, 60000);

    test("types a package against TypeScript's lib however many packages came before it", async () => {
        // Every program after the first shares the default lib's parsed and bound files.
        await withDir(async (root) => {
            const returnTypes = (name: string) => {
                const {pkg, nodeModules} = writeFiles(root.path, name, {
                    "package.json": JSON.stringify({name, version: "1.0.0", types: "index.d.ts"}),
                    "index.d.ts": `export declare class Loader { load(): Promise<string[]>; sizes(): Map<string, number>; }\n`,
                });
                const loader = exportedTypes([pkg], [nodeModules])
                    .find(t => Type.FullyQualified.getFullyQualifiedName(t) === `${name}.Loader`) as Type.Class;
                return loader.methods.map(m => `${m.name}: ${Type.signature(m.returnType)}`).sort();
            };

            const first = returnTypes("first-lib-fixture");
            expect(first.join("\n")).toContain("Promise");
            expect(first.join("\n")).toContain("Map");
            expect(returnTypes("second-lib-fixture")).toEqual(first);
        }, {unsafeCleanup: true});
    }, 60000);

    describe("what a package does not own", () => {
        test("leaves out the placeholder an object type maps to", async () => {
            await withDir(async (root) => {
                const {pkg, nodeModules} = writeFiles(root.path, "placeholder-fixture", {
                    "package.json": JSON.stringify({name: "placeholder-fixture", version: "1.0.0", types: "index.d.ts"}),
                    "index.d.ts": `declare const defaults: { a: number };\nexport type Options = typeof defaults;\nexport type Anything = object;\nexport declare class Keep {}\n`,
                });

                expect(fqns(exportedTypes([pkg], [nodeModules])).sort()).toEqual(["npm:placeholder-fixture", "placeholder-fixture", "placeholder-fixture.Keep"]);
            }, {unsafeCleanup: true});
        }, 60000);

        test("leaves out a global an exported alias renames", async () => {
            await withDir(async (root) => {
                const {pkg, nodeModules} = writeFiles(root.path, "global-alias-fixture", {
                    "package.json": JSON.stringify({name: "global-alias-fixture", version: "1.0.0", types: "index.d.ts"}),
                    "index.d.ts": `export type Callback = Function;\nexport type Mark = Symbol;\nexport type Failure = Error;\nexport declare class Keep {}\n`,
                });

                expect(fqns(exportedTypes([pkg], [nodeModules])).sort()).toEqual(["global-alias-fixture", "global-alias-fixture.Keep", "npm:global-alias-fixture"]);
            }, {unsafeCleanup: true});
        }, 60000);

        test("leaves out a global an ambient declaration merges with", async () => {
            await withDir(async (root) => {
                const {pkg, nodeModules} = writeFiles(root.path, "global-merge-fixture", {
                    "package.json": JSON.stringify({name: "global-merge-fixture", version: "1.0.0", types: "index.d.ts"}),
                    "index.d.ts": `interface Function { extra(): void; }\ninterface OwnGlobal { own(): void; }\n`,
                });

                expect(fqns(exportedTypes([pkg], [nodeModules])).sort()).toEqual(["OwnGlobal", "global-merge-fixture", "npm:global-merge-fixture"]);
            }, {unsafeCleanup: true});
        }, 60000);

        test("leaves out what another package declares, however it is reached", async () => {
            await withDir(async (root) => {
                writeFiles(root.path, "other-fixture", {
                    "package.json": JSON.stringify({name: "other-fixture", version: "1.0.0", types: "index.d.ts"}),
                    "index.d.ts": `export declare class Foreign {}\nexport declare class Renamed {}\n`,
                });
                const {pkg, nodeModules} = writeFiles(root.path, "reexport-fixture", {
                    "package.json": JSON.stringify({name: "reexport-fixture", version: "1.0.0", types: "index.d.ts"}),
                    "index.d.ts": `export {Foreign} from "other-fixture";\nexport type Local = import("other-fixture").Renamed;\nexport declare class Keep {}\n`,
                });

                expect(fqns(exportedTypes([pkg], [nodeModules])).sort()).toEqual(["npm:reexport-fixture", "reexport-fixture", "reexport-fixture.Keep"]);
            }, {unsafeCleanup: true});
        }, 60000);

        test("leaves out a package nested in its own node_modules", async () => {
            await withDir(async (root) => {
                const {pkg, nodeModules} = writeFiles(root.path, "nesting-fixture", {
                    "package.json": JSON.stringify({name: "nesting-fixture", version: "1.0.0", types: "index.d.ts"}),
                    "index.d.ts": `export {Nested} from "nested-fixture";\nexport declare class Keep {}\n`,
                    "node_modules/nested-fixture/package.json": JSON.stringify({name: "nested-fixture", version: "1.0.0", types: "index.d.ts"}),
                    "node_modules/nested-fixture/index.d.ts": `export declare class Nested {}\n`,
                });

                expect(fqns(exportedTypes([pkg], [nodeModules])).sort()).toEqual(["nesting-fixture", "nesting-fixture.Keep", "npm:nesting-fixture"]);
            }, {unsafeCleanup: true});
        }, 60000);

        test("keeps an alias that names a structure, and one of the package's own classes", async () => {
            await withDir(async (root) => {
                const {pkg, nodeModules} = writeFiles(root.path, "own-alias-fixture", {
                    "package.json": JSON.stringify({name: "own-alias-fixture", version: "1.0.0", types: "index.d.ts"}),
                    "index.d.ts": `declare class Hidden {}\nexport type Shape = { a: number };\nexport type Renamed = Hidden;\n`,
                });

                expect(fqns(exportedTypes([pkg], [nodeModules]))).toEqual(expect.arrayContaining(["own-alias-fixture.Shape", "own-alias-fixture.Hidden"]));
            }, {unsafeCleanup: true});
        }, 60000);

        test("a runtime declaration file keeps the globals it adds to", async () => {
            await withDir(async (root) => {
                const lib = path.join(root.path, "lib.fake.d.ts");
                fs.writeFileSync(lib, `interface Array<T> { fakeMember(): T; }\n`);

                expect(fqns(exportedTypes([lib], [lib]))).toEqual(["Array"]);
            }, {unsafeCleanup: true});
        }, 60000);
    });

    describe("the module a package defines", () => {
        function moduleEntry(types: Type.FullyQualified[], name: string): Type.Class | undefined {
            return types.find(t => Type.FullyQualified.getFullyQualifiedName(t) === name) as Type.Class | undefined;
        }

        test("is an empty class named after the package", async () => {
            await withDir(async (root) => {
                const {pkg, nodeModules} = writePackage(root.path);

                const entry = moduleEntry(exportedTypes([pkg], [nodeModules]), "exported-types-fixture");
                expect(entry).toMatchObject({kind: Type.Kind.Class, classKind: Type.Class.Kind.Class, methods: [], members: []});
            }, {unsafeCleanup: true});
        }, 60000);

        test("is all an untyped package defines", async () => {
            await withDir(async (root) => {
                const {pkg} = writeFiles(root.path, "untyped-fixture", {
                    "package.json": JSON.stringify({name: "untyped-fixture", version: "1.0.0", main: "index.js"}),
                    "index.js": `module.exports = function () {};\n`,
                });

                expect(fqns(exportedTypes([pkg], []))).toEqual(["untyped-fixture", "npm:untyped-fixture"]);
            }, {unsafeCleanup: true});
        }, 60000);

        test("keeps the scope of a scoped package", async () => {
            await withDir(async (root) => {
                const {pkg} = writeFiles(root.path, "@scope/untyped-fixture", {
                    "package.json": JSON.stringify({name: "@scope/untyped-fixture", version: "1.0.0", main: "index.js"}),
                    "index.js": ``,
                });

                expect(fqns(exportedTypes([pkg], []))).toEqual(["@scope/untyped-fixture", "npm:@scope/untyped-fixture"]);
            }, {unsafeCleanup: true});
        }, 60000);

        test("of a DefinitelyTyped package is the package it types", async () => {
            await withDir(async (root) => {
                const typing = {"package.json": JSON.stringify({version: "1.0.0", types: "index.d.ts"}), "index.d.ts": `export declare class Thing {}\n`};
                const plain = writeFiles(root.path, "@types/plain-fixture", typing);
                const scoped = writeFiles(root.path, "@types/babel__core", typing);

                expect(fqns(exportedTypes([plain.pkg], [plain.nodeModules]))).toContain("plain-fixture");
                expect(fqns(exportedTypes([scoped.pkg], [scoped.nodeModules]))).toContain("@babel/core");
            }, {unsafeCleanup: true});
        }, 60000);

        test("stands beside the package's own id, which is what a symbol names its package by", async () => {
            await withDir(async (root) => {
                const typing = (name?: string) => ({
                    "package.json": JSON.stringify({name, version: "1.0.0", types: "index.d.ts"}),
                    "index.d.ts": `export declare class Thing {}\n`,
                });
                const named = writeFiles(root.path, "@types/named-fixture", typing("@types/named-fixture"));
                const nameless = writeFiles(root.path, "@types/nameless-fixture", typing());
                const scoped = writeFiles(root.path, "@types/babel__core", typing("@types/babel__core"));

                expect(fqns(exportedTypes([named.pkg], [named.nodeModules]))).toEqual(expect.arrayContaining(["named-fixture", "npm:@types/named-fixture"]));
                expect(fqns(exportedTypes([nameless.pkg], [nameless.nodeModules]))).toEqual(expect.arrayContaining(["nameless-fixture", "npm:@types/nameless-fixture"]));
                expect(fqns(exportedTypes([scoped.pkg], [scoped.nodeModules]))).toEqual(expect.arrayContaining(["@babel/core", "npm:@types/babel__core"]));
            }, {unsafeCleanup: true});
        }, 60000);

        test("is never the package's id, even where the two are the same name", async () => {
            await withDir(async (root) => {
                const {pkg} = writeFiles(root.path, "same-fixture", {
                    "package.json": JSON.stringify({name: "same-fixture", version: "1.0.0", main: "index.js"}),
                    "index.js": ``,
                });

                expect(fqns(exportedTypes([pkg], []))).toEqual(["same-fixture", "npm:same-fixture"]);
            }, {unsafeCleanup: true});
        }, 60000);

        test("defines an empty class for an id, as it does for a module", async () => {
            await withDir(async (root) => {
                const {pkg, nodeModules} = writeFiles(root.path, "@types/empty-fixture", {
                    "package.json": JSON.stringify({name: "@types/empty-fixture", version: "1.0.0", types: "index.d.ts"}),
                    "index.d.ts": `export declare class Thing {}\n`,
                });

                const entry = moduleEntry(exportedTypes([pkg], [nodeModules]), "npm:@types/empty-fixture");
                expect(entry).toMatchObject({kind: Type.Kind.Class, classKind: Type.Class.Kind.Class, methods: [], members: []});
            }, {unsafeCleanup: true});
        }, 60000);

        test("goes by any namespace the package exports for itself too", async () => {
            await withDir(async (root) => {
                const {pkg, nodeModules} = writeFiles(root.path, "umd-fixture", {
                    "package.json": JSON.stringify({name: "umd-fixture", version: "1.0.0", types: "index.d.ts"}),
                    "index.d.ts": `export as namespace UmdGlobal;\nexport declare function run(): void;\n`,
                });

                expect(fqns(exportedTypes([pkg], [nodeModules])).sort()).toEqual(["UmdGlobal", "npm:umd-fixture", "umd-fixture"]);
            }, {unsafeCleanup: true});
        }, 60000);

        test("gives way to a type of the same name", async () => {
            await withDir(async (root) => {
                const {pkg, nodeModules} = writeFiles(root.path, "GlobalThing", {
                    "package.json": JSON.stringify({name: "GlobalThing", version: "1.0.0", types: "index.d.ts"}),
                    "index.d.ts": `declare class GlobalThing { run(): void; }\n`,
                });

                const entry = moduleEntry(exportedTypes([pkg], [nodeModules]), "GlobalThing");
                expect(entry?.methods.map(m => m.name)).toEqual(["run"]);
            }, {unsafeCleanup: true});
        }, 60000);

        test("a runtime declaration file defines no module", async () => {
            await withDir(async (root) => {
                const lib = path.join(root.path, "lib.fake.d.ts");
                fs.writeFileSync(lib, `interface FakeThing { poke(): void; }\n`);

                expect(fqns(exportedTypes([lib], [lib]))).toEqual(["FakeThing"]);
            }, {unsafeCleanup: true});
        }, 60000);
    });
});

/** Register a DependencyTypes handler on a mock connection and return the captured request handler. */
function captureHandler(batchSize: number): (req: any) => Promise<RpcObjectData[]> {
    let handler: ((req: any) => Promise<RpcObjectData[]>) | undefined;
    const connection = {onRequest: (_t: any, h: any) => { handler = h; }} as any;
    DependencyTypes.handle(connection, batchSize);
    return handler!;
}

/** The fully qualified names a response carries, among its other string values. */
function names(data: RpcObjectData[]): string[] {
    return data.filter(d => typeof d.value === "string").map(d => d.value as string);
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

    test("resolves against the node_modules of the directory the request names, not the parsed project's", async () => {
        await withDir(async (root) => {
            const fetched = path.join(root.path, "fetched", "exported-types-fixture", "1.0.0");
            writePackage(fetched);
            // A project with no node_modules at all, so only the request's path can find the package.
            setLastParsedProject(path.join(root.path, "empty-project"));

            const data = await captureHandler(100000)({name: "exported-types-fixture", version: "1.0.0", path: fetched});
            expect(data.some(d => d.value === "exported-types-fixture.Greeter")).toBe(true);
            expect(data[data.length - 1].state).toBe(RpcObjectState.END_OF_OBJECT);
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

    test("a fallback goes by the package it stands for, and its DefinitelyTyped id stays with that package", async () => {
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

            const fallback = names(await captureHandler(100000)({name: "plainlib", version: "2.0.0"}));
            expect(fallback).toEqual(expect.arrayContaining(["plainlib", "npm:plainlib", "plainlib.Helper"]));
            expect(fallback).not.toContain("npm:@types/plainlib");

            const definitelyTyped = names(await captureHandler(100000)({name: "@types/plainlib", version: "2.0.0"}));
            expect(definitelyTyped).toContain("npm:@types/plainlib");
        }, {unsafeCleanup: true});
    }, 60000);

    test("falls back for an untyped package installed under an alias, whose own names are two", async () => {
        await withDir(async (root) => {
            writeFiles(root.path, "aliaslib", {
                "package.json": JSON.stringify({name: "plainlib", version: "2.0.0", main: "index.js"}),
                "index.js": `module.exports.helper = function () {};\n`,
            });
            writeFiles(root.path, "@types/aliaslib", {
                "package.json": JSON.stringify({name: "@types/aliaslib", version: "2.0.0", types: "index.d.ts"}),
                "index.d.ts": `export declare class Helper { assist(): void; }\n`,
            });
            setLastParsedProject(root.path);

            const data = await captureHandler(100000)({name: "aliaslib", version: "2.0.0"});
            expect(data.some(d => d.value === "aliaslib.Helper")).toBe(true);
            expect(names(data)).toEqual(expect.arrayContaining(["aliaslib", "npm:plainlib"]));
            expect(names(data)).not.toContain("npm:@types/aliaslib");
        }, {unsafeCleanup: true});
    }, 60000);

    test("an untyped package with no DefinitelyTyped package defines its module and stops there", async () => {
        await withDir(async (root) => {
            writeFiles(root.path, "plainlib", {
                "package.json": JSON.stringify({name: "plainlib", version: "2.0.0", main: "index.js"}),
                "index.js": `module.exports.helper = function () {};\n`,
            });
            setLastParsedProject(root.path);

            const data = await captureHandler(100000)({name: "plainlib", version: "2.0.0"});
            expect(data.filter(d => typeof d.value === "string").map(d => d.value)).toContain("plainlib");
            expect(data[data.length - 1].state).toBe(RpcObjectState.END_OF_OBJECT);
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
