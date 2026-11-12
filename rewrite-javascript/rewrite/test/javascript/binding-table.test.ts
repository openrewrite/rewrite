import {fromVisitor, RecipeSpec, SourceSpec} from "../../src/test";
import {
    AddImport, AddImportOptions, bindingTable, javascript, JavaScriptVisitor, JS, maybeBind, MaybeBindOptions, ModuleBinding,
    maybeRebind, MaybeRebindOptions, moduleScopeBindings, ModuleScopeBinding, typescript
} from "../../src/javascript";
import {J} from "../../src/java";

/** A row without the nodes it points at, which a test spells as the rest of it. */
type Row = Omit<ModuleBinding, "statement" | "declaration">;

/** `bindingTable` and `moduleScopeBindings` for the one file `source` parses to. */
async function tablesOf(source: SourceSpec<any>): Promise<{rows: Row[], scope: ModuleScopeBinding[]}> {
    const seen: {rows: Row[], scope: ModuleScopeBinding[]} = {rows: [], scope: []};
    const spec = new RecipeSpec();
    spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
        override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
            seen.rows = bindingTable(cu).map(binding => {
                const {statement, declaration, ...row} = binding as ModuleBinding & {declaration?: J.VariableDeclarations};
                return row;
            });
            seen.scope = moduleScopeBindings(cu);
            return cu;
        }
    });
    await spec.rewriteRun(source);
    return seen;
}

/** A spec whose recipe calls `maybeBind` from the compilation unit, keeping the first cycle's answer in `bound`. */
function binding(options: MaybeBindOptions, bound: {name?: string}): RecipeSpec {
    const spec = new RecipeSpec();
    let asked = false;
    spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
        override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
            if (!asked) {
                asked = true;
                bound.name = maybeBind(this, options);
            }
            return cu;
        }
    });
    return spec;
}

/** A spec whose recipe calls `maybeRebind` from the compilation unit, keeping the first cycle's answer in `bound`. */
function rebinding(options: MaybeRebindOptions, bound: {name?: string}): RecipeSpec {
    const spec = new RecipeSpec();
    let asked = false;
    spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
        override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
            if (!asked) {
                asked = true;
                bound.name = maybeRebind(this, options);
            }
            return cu;
        }
    });
    return spec;
}

/** A spec whose recipe is one `AddImport` of `options`. */
function adding(options: AddImportOptions): RecipeSpec {
    const spec = new RecipeSpec();
    spec.recipe = fromVisitor(new AddImport(options));
    return spec;
}

describe("bindingTable", () => {
    test("an import binds its default, namespace and named members each under their own form", async () => {
        const {rows} = await tablesOf(
            //language=typescript
            typescript(`
                import React, {useState, useEffect as effect} from "react";
                import * as ReactDOM from "react-dom";
            `)
        );
        expect(rows).toEqual([
            {form: "default", name: "React", module: "react", typeOnly: false},
            {form: "named", name: "useState", member: "useState", module: "react", typeOnly: false},
            {form: "named", name: "effect", member: "useEffect", module: "react", typeOnly: false},
            {form: "namespace", name: "ReactDOM", module: "react-dom", typeOnly: false}
        ]);
    });

    test("a type-only clause marks every name it binds, and a specifier's own type marks that one", async () => {
        const {rows} = await tablesOf(
            //language=typescript
            typescript(`
                import type {FC} from "react";
                import {type SFC, Component} from "react";
            `)
        );
        expect(rows).toEqual([
            {form: "named", name: "FC", member: "FC", module: "react", typeOnly: true},
            {form: "named", name: "SFC", member: "SFC", module: "react", typeOnly: true},
            {form: "named", name: "Component", member: "Component", module: "react", typeOnly: false}
        ]);
    });

    test("import X = require binds the whole module, while import X = A.B names a namespace and binds none", async () => {
        const {rows} = await tablesOf(
            //language=typescript
            typescript(`
                namespace Shapes {
                    export namespace Polygons {
                        export const sides = 3;
                    }
                }
                import fs = require("fs");
                import type path = require("path");
                import Polygons = Shapes.Polygons;
            `)
        );
        expect(rows).toEqual([
            {form: "import-equals", name: "fs", module: "fs", typeOnly: false},
            {form: "import-equals", name: "path", module: "path", typeOnly: true}
        ]);
    });

    test("a require binds the whole module, and a destructuring one each member its pattern reads", async () => {
        const {rows} = await tablesOf(
            //language=javascript
            javascript(`
                const fs = require("fs");
                const {join, resolve: res} = require("path");
            `)
        );
        expect(rows).toEqual([
            {form: "require", name: "fs", module: "fs", typeOnly: false, reassignable: false},
            {form: "require-member", name: "join", member: "join", module: "path", typeOnly: false, reassignable: false},
            {form: "require-member", name: "res", member: "resolve", module: "path", typeOnly: false, reassignable: false}
        ]);
    });

    test("a let or var require is reassignable, since the file may assign the name another value", async () => {
        const {rows} = await tablesOf(
            //language=javascript
            javascript(`
                let path = require("path");
                var {cpus} = require("os");
            `)
        );
        expect(rows).toEqual([
            {form: "require", name: "path", module: "path", typeOnly: false, reassignable: true},
            {form: "require-member", name: "cpus", member: "cpus", module: "os", typeOnly: false, reassignable: true}
        ]);
    });

    test("a name a require's pattern reads through no one member is a require-pattern row", async () => {
        const {rows} = await tablesOf(
            //language=javascript
            javascript(`
                const {readFile, ...rest} = require("fs");
                const {posix: {join}} = require("path");
                const [first] = require("list");
            `)
        );
        expect(rows).toEqual([
            {form: "require-member", name: "readFile", member: "readFile", module: "fs", typeOnly: false, reassignable: false},
            {form: "require-pattern", name: "rest", module: "fs", typeOnly: false, reassignable: false},
            {form: "require-pattern", name: "join", module: "path", typeOnly: false, reassignable: false},
            {form: "require-pattern", name: "first", module: "list", typeOnly: false, reassignable: false}
        ]);
    });

    test("an awaited dynamic import binds the module namespace object", async () => {
        const {rows} = await tablesOf(
            //language=typescript
            typescript(`
                const os = await import("os");
                let lazy = await import.defer("lazy");
            `)
        );
        expect(rows).toEqual([
            {form: "dynamic-import", name: "os", module: "os", typeOnly: false, reassignable: false},
            {form: "dynamic-import", name: "lazy", module: "lazy", typeOnly: false, reassignable: true}
        ]);
    });

    test("each declarator of a statement with several gives its own rows", async () => {
        const {rows} = await tablesOf(
            //language=javascript
            javascript(`
                const a = 1, b = require("b"), {c} = require("c");
            `)
        );
        expect(rows).toEqual([
            {form: "require", name: "b", module: "b", typeOnly: false, reassignable: false},
            {form: "require-member", name: "c", member: "c", module: "c", typeOnly: false, reassignable: false}
        ]);
    });

    test("only the top level binds, and a side-effect import or require binds no name", async () => {
        const {rows} = await tablesOf(
            //language=javascript
            javascript(`
                require("setup");
                function load() {
                    const fs = require("fs");
                    return fs;
                }
            `)
        );
        expect(rows).toEqual([]);
    });

    test("a default import and a whole require are different forms, though moduleScopeBindings gives both no member", async () => {
        const {rows, scope} = await tablesOf(
            //language=typescript
            typescript(`
                import React from "react";
                const fs = require("fs");
            `)
        );
        expect(rows.map(r => r.form)).toEqual(["default", "require"]);
        expect(scope).toEqual([
            {name: "React", module: "react", member: undefined, typeOnly: false},
            {name: "fs", module: "fs", member: undefined, typeOnly: false}
        ]);
    });
});

describe("moduleScopeBindings over the binding table", () => {
    test("rows read as AddImport requests: no member for a default or a whole require, '*' for a namespace", async () => {
        const {scope} = await tablesOf(
            //language=typescript
            typescript(`
                import * as R from "react";
                import {useState as s} from "react";
                const {join} = require("path");
            `)
        );
        expect(scope).toEqual([
            {name: "R", module: "react", member: "*", typeOnly: false},
            {name: "s", module: "react", member: "useState", typeOnly: false},
            {name: "join", module: "path", member: "join", typeOnly: false}
        ]);
    });

    test("import X = require binds its module, import X = A.B and a rest element keep only their name, and await import has no row", async () => {
        const {scope} = await tablesOf(
            //language=typescript
            typescript(`
                namespace Shapes {
                    export const sides = 3;
                }
                import fs = require("fs");
                import S = Shapes;
                const {a, ...rest} = require("r");
                const os = await import("os");
            `)
        );
        // Before: {name: "fs", module: undefined, member: undefined, typeOnly: false}
        expect(scope).toEqual([
            {name: "fs", module: "fs", member: undefined, typeOnly: false},
            {name: "S", module: undefined, member: undefined, typeOnly: false},
            {name: "a", module: "r", member: "a", typeOnly: false},
            {name: "rest"}
        ]);
    });
});

describe("import X = require binds its module for every reader", () => {
    test("a type-only import X = require is the type-only row of its whole module", async () => {
        const {scope} = await tablesOf(
            //language=typescript
            typescript(`
                import type path = require("path");

                let parsed: path.ParsedPath;
            `)
        );
        // Before: {name: "path", module: undefined, member: undefined, typeOnly: true}
        expect(scope).toEqual([{name: "path", module: "path", member: undefined, typeOnly: true}]);
        // Before: undefined, so a caller looking for a type-only binding of the whole module found none
        expect(scope.find(b => b.module === "path" && b.member === undefined && b.typeOnly)?.name).toBe("path");
    });

    test("AddImport of the module finds import X = require and adds nothing", async () => {
        // Before: `import fs from "fs";` was added below it, a second declaration of `fs`
        await adding({module: "fs", onlyIfReferenced: false}).rewriteRun(
            //language=typescript
            typescript(`
                import fs = require("fs");

                fs.readFileSync("config.json");
            `)
        );
    });

    test("maybeBind of the module as a value reuses a type-only import X = require, dropping its type", async () => {
        const bound: {name?: string} = {};
        // Before: answered "path_1" and added `import path_1 from "path";` beside it
        await binding({module: "path", onlyIfReferenced: false}, bound).rewriteRun(
            //language=typescript
            typescript(
                `
                    import type path = require("path");

                    let parsed: path.ParsedPath;
                `,
                `
                    import path = require("path");

                    let parsed: path.ParsedPath;
                `
            )
        );
        expect(bound.name).toBe("path");
    });
});

describe("a whole require answers a namespace request, since CommonJS has no default/namespace split", () => {
    test("AddImport of the namespace finds a require under that name and adds nothing", async () => {
        // Before: a second `const fs = require("fs");` was added
        await adding({module: "fs", member: "*", alias: "fs", onlyIfReferenced: false}).rewriteRun(
            //language=javascript
            javascript(`
                const fs = require("fs");

                fs.readFileSync("config.json");
            `)
        );
    });

    test("AddImport of the namespace finds import X = require under that name and adds nothing", async () => {
        // Before: `import * as fs from "fs";` was added below it, a second declaration of `fs`
        await adding({module: "fs", member: "*", alias: "fs", onlyIfReferenced: false}).rewriteRun(
            //language=typescript
            typescript(`
                import fs = require("fs");

                fs.readFileSync("config.json");
            `)
        );
    });
});

describe("an export const require is not reassignable, though no binding can leave it", () => {
    test("the table reads export const as const", async () => {
        const {rows} = await tablesOf(
            //language=typescript
            typescript(`
                export const fs = require("fs");
                export let path = require("path");
            `)
        );
        expect(rows).toEqual([
            {form: "require", name: "fs", module: "fs", typeOnly: false, reassignable: false},
            {form: "require", name: "path", module: "path", typeOnly: false, reassignable: true}
        ]);
    });

    test("maybeRebind keeps a member in an export const destructuring beside other names", async () => {
        const bound: {name?: string} = {};
        // Moving it out would give it a plain `const {New} = require("legacy-v2")`, and the file would stop exporting `Old`
        await rebinding({from: {module: "legacy", member: "Old"}, to: {module: "legacy-v2", member: "New"}}, bound)
            .rewriteRun(
                //language=typescript
                typescript(`
                    export const {Old, helper} = require("legacy");

                    Old(helper());
                `)
            );
        expect(bound.name).toBeUndefined();
    });

    test("maybeRebind of an export const member rewrites it in place rather than joining the target's require", async () => {
        const bound: {name?: string} = {};
        // Joining would drop the exported statement for `const {New} = require("legacy-v2")`, and a rewrite in place
        // under the target's name would declare `New` twice, so it keeps its own name
        await rebinding({from: {module: "legacy", member: "Old"}, to: {module: "legacy-v2", member: "New"}}, bound)
            .rewriteRun(
                //language=typescript
                typescript(
                    `
                        export const {Old} = require("legacy");
                        const {New} = require("legacy-v2");

                        New(Old());
                    `,
                    `
                        export const {New: Old} = require("legacy-v2");
                        const {New} = require("legacy-v2");

                        New(Old());
                    `
                )
            );
        expect(bound.name).toBe("Old");
    });
});
