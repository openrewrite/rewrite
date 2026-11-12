import {fromVisitor, RecipeSpec} from "../../src/test";
import {withDir} from "tmp-promise";
import * as fs from "fs";
import * as path from "path";
import {
    JavaScriptVisitor, JS, javascript, npm, packageJson, tsx, typescript, moduleBindings, isAmdBlock, ModuleBindings, maybeBind,
    maybeAddImport, MaybeBindOptions, maybeUnbind, maybeRebind, MaybeRebindOptions, maybeRemoveImport,
    removeNewlyUnusedAmdBindings, ImportStyle, ExportOfOptions, ModuleExport
} from "../../src/javascript";
import {emptySpace, J, rightPadded, Type} from "../../src/java";
import {emptyMarkers} from "../../src/markers";
import {randomId} from "../../src/uuid";
import {Cursor} from "../../src/tree";
import {ExecutionContext, TreePrinters} from "../../src";

function captureBindings(seen: {moduleSystem?: string, module?: string, binding?: string},
                          localName: string = "Button", moduleName: string = "sap/m/Button") {
    return captureModuleBindings(bindings => {
        seen.moduleSystem = bindings.moduleSystem;
        seen.module = bindings.moduleOf(localName);
        seen.binding = bindings.bindingOf(moduleName);
    });
}

/** Runs `extract` with the file's `ModuleBindings`, visited from the compilation unit itself. */
function captureModuleBindings(extract: (bindings: ModuleBindings) => void) {
    return new class extends JavaScriptVisitor<any> {
        override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
            extract(moduleBindings(this));
            return super.visitJsCompilationUnit(cu, p);
        }
    };
}

describe("moduleBindings", () => {
    test("an ESM default import binds its module", async () => {
        const spec = new RecipeSpec();
        const seen: {moduleSystem?: string, module?: string, binding?: string} = {};
        spec.recipe = fromVisitor(captureBindings(seen));
        await spec.rewriteRun(typescript(`import Button from "sap/m/Button";`));
        expect(seen).toEqual({moduleSystem: "esm", module: "sap/m/Button", binding: "Button"});
    });

    test("a plain script with no import, export, require, or AMD block reports \"none\"", async () => {
        const spec = new RecipeSpec();
        const seen: {moduleSystem?: string} = {};
        spec.recipe = fromVisitor(captureBindings(seen));
        await spec.rewriteRun(typescript(`const x = 1;`));
        expect(seen.moduleSystem).toBe("none");
    });

    test("no compilation unit on the cursor reports \"none\", not a lane it cannot know", async () => {
        const spec = new RecipeSpec();
        const seen: {moduleSystem?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            constructor() {
                super();
                seen.moduleSystem = moduleBindings(this).moduleSystem;
            }
        });
        await spec.rewriteRun(typescript(`const x = 1;`));
        expect(seen.moduleSystem).toBe("none");
    });

    test("an export alone is enough to read as \"esm\", even with no import", async () => {
        const spec = new RecipeSpec();
        const seen: {moduleSystem?: string} = {};
        spec.recipe = fromVisitor(captureBindings(seen));
        await spec.rewriteRun(typescript(`export const x = 1;`));
        expect(seen.moduleSystem).toBe("esm");
    });

    test("a top-level await import is the file's only module syntax and reports \"esm\"", async () => {
        const spec = new RecipeSpec();
        const seen: {moduleSystem?: string} = {};
        spec.recipe = fromVisitor(captureBindings(seen));
        await spec.rewriteRun(typescript(`const Button = await import("sap/m/Button");`));
        expect(seen.moduleSystem).toBe("esm");
    });

    test("a require inside a function makes the file CommonJS, unless the function binds require itself", async () => {
        const spec = new RecipeSpec();
        const seen: {moduleSystem?: string} = {};
        spec.recipe = fromVisitor(captureBindings(seen));
        //language=javascript
        await spec.rewriteRun(javascript(`
            function f() {
                return require('fs');
            }`));
        expect(seen.moduleSystem).toBe("commonjs");

        await spec.rewriteRun(javascript(`define(function (require, exports) { const a = require("a"); exports.x = a; });`));
        expect(seen.moduleSystem).toBe("none");

        // An `await` inside a function is not a top-level one.
        //language=javascript
        await spec.rewriteRun(javascript(`
            async function f() {
                await g();
            }`));
        expect(seen.moduleSystem).toBe("none");
    });

    test("a selected require, unlike a bare one, does not read as a CommonJS binding", async () => {
        const spec = new RecipeSpec();
        const seen: {moduleSystem?: string} = {};
        spec.recipe = fromVisitor(captureBindings(seen));
        await spec.rewriteRun(javascript(`const other = foo.require("a/Other");`));
        expect(seen.moduleSystem).toBe("none");
    });

    test("a .cjs file reads as CommonJS even with no require call in it yet", async () => {
        const spec = new RecipeSpec();
        const seen: {moduleSystem?: string} = {};
        spec.recipe = fromVisitor(captureBindings(seen));
        await spec.rewriteRun({...javascript(`target();`), path: "module.cjs"});
        expect(seen.moduleSystem).toBe("commonjs");
    });

    test("a .cts file reads as CommonJS even with no require call in it yet", async () => {
        const spec = new RecipeSpec();
        const seen: {moduleSystem?: string} = {};
        spec.recipe = fromVisitor(captureBindings(seen));
        await spec.rewriteRun({...typescript(`target();`), path: "module.cts"});
        expect(seen.moduleSystem).toBe("commonjs");
    });

    test("a require among several declarators in one statement still reads as CommonJS", async () => {
        const spec = new RecipeSpec();
        const seen: {moduleSystem?: string} = {};
        spec.recipe = fromVisitor(captureBindings(seen));
        await spec.rewriteRun(javascript(`const a = require("x"), b = require("y");`));
        expect(seen.moduleSystem).toBe("commonjs");
    });

    test("an AMD block's parameters bind positionally to its dependencies", async () => {
        const spec = new RecipeSpec();
        const seen: {moduleSystem?: string, module?: string, binding?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitReturn(ret: J.Return, p: any): Promise<J | undefined> {
                const bindings = moduleBindings(this);
                seen.moduleSystem = bindings.moduleSystem;
                seen.module = bindings.moduleOf("Button");
                seen.binding = bindings.bindingOf("sap/m/Button");
                return super.visitReturn(ret, p);
            }
        });
        await spec.rewriteRun(javascript(`
            sap.ui.define(["sap/m/Button"], function (Button) {
                return Button;
            });
        `));
        expect(seen).toEqual({moduleSystem: "amd", module: "sap/m/Button", binding: "Button"});
    });

    test("a named ESM import does not bind the module object", async () => {
        const spec = new RecipeSpec();
        const seen: {module?: string, binding?: string} = {};
        spec.recipe = fromVisitor(captureModuleBindings(bindings => {
            seen.module = bindings.moduleOf("getElementById");
            seen.binding = bindings.bindingOf("sap/ui/core/Element");
        }));
        await spec.rewriteRun(typescript(`import {getElementById} from "sap/ui/core/Element";`));
        expect(seen).toEqual({module: undefined, binding: undefined});
    });

    test("an ESM namespace import binds its module", async () => {
        const spec = new RecipeSpec();
        const seen: {binding?: string} = {};
        spec.recipe = fromVisitor(captureModuleBindings(bindings => {
            seen.binding = bindings.bindingOf("sap/ui/core/Element");
        }));
        await spec.rewriteRun(typescript(`import * as Element from "sap/ui/core/Element";`));
        expect(seen.binding).toBe("Element");
    });

    test("require binds the module object but not a destructured member", async () => {
        const spec = new RecipeSpec();
        const seen: {moduleSystem?: string, elementBinding?: string, pathBinding?: string} = {};
        spec.recipe = fromVisitor(captureModuleBindings(bindings => {
            seen.moduleSystem = bindings.moduleSystem;
            seen.elementBinding = bindings.bindingOf("sap/ui/core/Element");
            seen.pathBinding = bindings.bindingOf("path");
        }));
        await spec.rewriteRun(javascript(`
            const Elem = require("sap/ui/core/Element");
            const {join} = require("path");
        `));
        expect(seen.moduleSystem).toBe("commonjs");
        expect(seen.elementBinding).toBe("Elem");

        expect(seen.pathBinding).toBeUndefined();
    });

    test("import X = require binds the module object, as a require does", async () => {
        const spec = new RecipeSpec();
        const seen: {module?: string, binding?: string} = {};
        spec.recipe = fromVisitor(captureModuleBindings(bindings => {
            seen.module = bindings.moduleOf("Elem");
            seen.binding = bindings.bindingOf("sap/ui/core/Element");
        }));
        //language=typescript
        await spec.rewriteRun(typescript(`import Elem = require("sap/ui/core/Element");`));
        expect(seen).toEqual({module: "sap/ui/core/Element", binding: "Elem"});
    });
});

/**
 * What `exportOf` answers for the last node of `source` that prints as `reference`, past any import
 * spelling it, with the cursor on that node, or on the parameterized type holding it, which is how a
 * visitor reaches `SFC` in `SFC<P>`.
 */
async function exportOfIn(source: string, reference: string, options?: ExportOfOptions): Promise<ModuleExport | undefined> {
    const answers: (ModuleExport | undefined)[] = [];
    const spec = new RecipeSpec();
    spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
        private async ask(node: J): Promise<void> {
            if ((await TreePrinters.printer(this.cursor).print(node)).trim() === reference) {
                answers.push(moduleBindings(this).exportOf(node, options));
            }
        }

        override async visitIdentifier(identifier: J.Identifier, p: any): Promise<J | undefined> {
            await this.ask(identifier);
            return super.visitIdentifier(identifier, p);
        }

        override async visitFieldAccess(fieldAccess: J.FieldAccess, p: any): Promise<J | undefined> {
            await this.ask(fieldAccess);
            return super.visitFieldAccess(fieldAccess, p);
        }

        override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
            await this.ask(method);
            return super.visitMethodInvocation(method, p);
        }

        override async visitArrayAccess(arrayAccess: J.ArrayAccess, p: any): Promise<J | undefined> {
            await this.ask(arrayAccess);
            return super.visitArrayAccess(arrayAccess, p);
        }

        override async visitFunctionCall(functionCall: JS.FunctionCall, p: any): Promise<J | undefined> {
            await this.ask(functionCall);
            return super.visitFunctionCall(functionCall, p);
        }

        override async visitImportType(importType: JS.ImportType, p: any): Promise<J | undefined> {
            await this.ask(importType);
            return super.visitImportType(importType, p);
        }

        override async visitParameterizedType(type: J.ParameterizedType, p: any): Promise<J | undefined> {
            await this.ask(type.class);
            return super.visitParameterizedType(type, p);
        }
    });
    await spec.rewriteRun(typescript(source));
    expect(answers.length).toBeGreaterThan(0);
    return answers[answers.length - 1];
}

describe("exportOf", () => {
    const react = (member: string, typeOnly: boolean, kind: ModuleExport["via"]["kind"], name?: string): ModuleExport =>
        ({module: "react", member, typeOnly, via: name === undefined ? {kind} : {kind, name}});

    test.each([
        ["a named import", `import {useState} from "react"; useState(0);`, "useState", undefined,
            react("useState", false, "named", "useState")],
        ["an export clause reads the binding it publishes", `import {FC} from "react"; export {FC};`, "FC", undefined,
            react("FC", false, "named", "FC")],
        ["a call names what its callee does", `import {useState} from "react"; useState(0);`, "useState(0)", undefined,
            react("useState", false, "named", "useState")],
        ["an aliased import names the member, not the alias", `import {SFC as S} from "react"; let a: S<{}>;`, "S", undefined,
            react("SFC", false, "named", "S")],
        ["a type-only import", `import type {FC} from "react"; let a: FC;`, "FC", undefined,
            react("FC", true, "named", "FC")],
        ["an inline type specifier", `import {type FC} from "react"; let a: FC;`, "FC", undefined,
            react("FC", true, "named", "FC")],
        ["a member through a namespace import", `import * as R from "react"; let a: R.SFC<{}>;`, "R.SFC", undefined,
            react("SFC", false, "namespace", "R")],
        ["a call through a namespace import", `import * as React from "react"; React.createElement("div");`,
            `React.createElement("div")`, undefined, react("createElement", false, "namespace", "React")],
        ["a member through a type-only namespace import", `import type * as React from "react"; let a: React.FC;`,
            "React.FC", undefined, react("FC", true, "namespace", "React")],
        ["a member through import-equals", `import React = require("react"); let a: React.FC;`, "React.FC", undefined,
            react("FC", false, "require", "React")],
        ["an import() type", `let a: import("react").SFC<{}>;`, `import("react").SFC<{}>`, undefined,
            react("SFC", true, "import-type")],
        ["a default import's own name", `import React from "react"; React;`, "React", undefined,
            react("default", false, "default", "React")],
        ["a member through a default import, where the caller says the default is the module",
            `import React from "react"; let a: React.SFC<{}>;`, "React.SFC", {defaultIsModule: ["react"]},
            react("SFC", false, "default", "React")],
        ["a member of a global the caller maps to a module", `let a: React.SFC<{}>;`, "React.SFC", {globals: {React: "react"}},
            react("SFC", false, "global", "React")],
        ["a value of the same name does not hide the global namespace a type reads",
            `const React = {}; let a: React.SFC<{}>;`, "React.SFC", {globals: {React: "react"}},
            react("SFC", false, "global", "React")],
        ["a member through a let require the file never reassigns",
            `let React = require("react"); React.createElement("div");`, `React.createElement("div")`, undefined,
            react("createElement", false, "require", "React")],
        ["a member through a module object in parentheses", `import * as React from "react"; (React).createElement("div");`,
            `(React).createElement("div")`, undefined, react("createElement", false, "namespace", "React")],
        ["a member read by its string name", `import * as React from "react"; const c = React["createElement"];`,
            `React["createElement"]`, undefined, react("createElement", false, "namespace", "React")],
        ["a call of a member read by its string name", `import * as React from "react"; React["createElement"]("div");`,
            `React["createElement"]("div")`, undefined, react("createElement", false, "namespace", "React")],
        ["a name the top level destructures from a module object",
            `import * as React from "react"; const {useState: useLocalState} = React; useLocalState(0);`, "useLocalState(0)",
            undefined, react("useState", false, "destructured", "useLocalState")],
        ["a name destructured from a default import, where the caller says the default is the module",
            `import React from "react"; const {useState} = React; useState(0);`, "useState(0)", {defaultIsModule: ["react"]},
            react("useState", false, "destructured", "useState")],
        ["the member a re-export reads", `export {useState as useLocalState} from "react";`, "useState", undefined,
            react("useState", false, "re-export")],
        ["a type-only re-export", `export type {FC} from "react";`, "FC", undefined, react("FC", true, "re-export")],
    ] as [string, string, string, ExportOfOptions | undefined, ModuleExport | undefined][])(
        "%s", async (_rule, source, reference, options, expected) => {
            expect(await exportOfIn(source, reference, options)).toEqual(expected);
        });

    test.each([
        ["a member through a default import, which reads a property of the default export",
            `import React from "react"; let a: React.SFC<{}>;`, "React.SFC", undefined],
        ["a default import's own name, where the default is the module", `import React from "react"; React;`, "React",
            {defaultIsModule: ["react"]}],
        ["a namespace import's own name", `import * as React from "react"; React;`, "React", undefined],
        ["a global the caller does not map", `let a: React.SFC<{}>;`, "React.SFC", undefined],
        ["a mapped global that the file declares as a namespace", `namespace React { export type SFC<T> = T; } let a: React.SFC<{}>;`,
            "React.SFC", {globals: {React: "react"}}],
        ["a type parameter shadowing the import", `import {FC} from "react"; function f<FC>(a: FC) {}`, "FC", undefined],
        ["a parameter shadowing the import", `import {useState} from "react"; function f(useState: number) { useState; }`,
            "useState", undefined],
        ["a member of a member", `import * as React from "react"; let a: React.JSX.Element;`, "React.JSX.Element", undefined],
        ["a name the file declares", `type FC = number; let a: FC;`, "FC", undefined],
        ["the name an import declares", `import {FC} from "react";`, "FC", undefined],
        ["a property that reads like an import", `import {FC} from "react"; o.FC;`, "o.FC", undefined],
        ["a member through a let require the file reassigns",
            `let React = require("react"); React = other; React.createElement("div");`, `React.createElement("div")`,
            undefined],
        ["a name destructured from a default import, which reads a property of the default export",
            `import React from "react"; const {useState} = React; useState(0);`, "useState(0)", undefined],
        ["the name a re-export publishes", `export {useState as useLocalState} from "react";`, "useLocalState", undefined],
    ] as [string, string, string, ExportOfOptions | undefined][])(
        "no export: %s", async (_rule, source, reference, options) => {
            expect(await exportOfIn(source, reference, options)).toBeUndefined();
        });

    // maybeRebind moves this one. exportOf reads only the top level's bindings, since `resolve` names the
    // function binding `useState`, not the declaration that would say it comes from React.
    test.fails("a name a function destructures from a module object", async () => {
        expect(await exportOfIn(
            //language=typescript
            `
            import * as React from "react";
            function useToggle() {
                const {useState} = React;
                return useState(false);
            }
            `,
            "useState(false)"
        )).toEqual(react("useState", false, "destructured", "useState"));
    });

    test("a cursor in place of the visitor, for a node the visitor's node holds", async () => {
        const spec = new RecipeSpec();
        const seen: (ModuleExport | undefined)[] = [];
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitParameterizedType(type: J.ParameterizedType, p: any): Promise<J | undefined> {
                seen.push(moduleBindings(new Cursor(type.class, this.cursor)).exportOf(type.class));
                return super.visitParameterizedType(type, p);
            }
        });
        await spec.rewriteRun(typescript(`import {SFC} from "react"; let a: SFC<{}>;`));
        expect(seen).toEqual([{module: "react", member: "SFC", typeOnly: false, via: {kind: "named", name: "SFC"}}]);
    });

    test("a member read through an AMD factory parameter", async () => {
        const spec = new RecipeSpec();
        const seen: (ModuleExport | undefined)[] = [];
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (method.name.simpleName === "extend") {
                    seen.push(moduleBindings(this).exportOf(method));
                }
                return super.visitMethodInvocation(method, p);
            }
        });
        await spec.rewriteRun(javascript(`
            sap.ui.define(["sap/m/Button"], function (Button) {
                return Button.extend("my.Button", {});
            });
        `));
        expect(seen).toEqual([{module: "sap/m/Button", member: "extend", typeOnly: false, via: {kind: "amd", name: "Button"}}]);
    });

    test("a named import of an export literally called \"*\" names that export", async () => {
        const read = await exportOfIn(
            //language=typescript
            `
                import {"*" as star} from "./exports-star";

                star();
            `,
            "star()"
        );
        // Before: undefined, since the lookup skipped the member "*", which `moduleScopeBindings` spells a namespace with
        expect(read).toEqual({module: "./exports-star", member: "*", typeOnly: false, via: {kind: "named", name: "star"}});
    });
});

describe("isAmdBlock", () => {
    test("a define block inside an otherwise-ESM file is still a block", async () => {
        const spec = new RecipeSpec();
        const blocks: string[] = [];
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (isAmdBlock(m)) {
                    blocks.push(m.name.simpleName);
                }
                return super.visitMethodInvocation(m, p);
            }
        });
        await spec.rewriteRun(javascript(`
            import x from "m";
            sap.ui.define(["a/B"], function (B) {});
        `));
        expect(blocks).toEqual(["define"]);
    });
});

function identifierRef(name: string): J.Identifier {
    return {
        kind: J.Kind.Identifier,
        id: randomId(),
        prefix: emptySpace,
        markers: emptyMarkers,
        annotations: [],
        simpleName: name,
        type: undefined,
        fieldType: undefined
    };
}

/** Rewrites `call` to read as `<name>.call`, the way a real recipe uses the name `maybeBind` returned. */
function withReference(call: J.MethodInvocation, name: string): J.MethodInvocation {
    const select = call.select?.element;
    if (select?.kind === J.Kind.Identifier && (select as J.Identifier).simpleName === name) {
        return call;
    }
    return {...call, select: rightPadded(identifierRef(name), emptySpace)};
}

/** A caller that uses the answer. */
function rebind(options: MaybeBindOptions | string, bound: {name?: string}) {
    return new class extends JavaScriptVisitor<any> {
        override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
            if (m.name.simpleName !== "target") {
                return super.visitMethodInvocation(m, p);
            }
            bound.name = maybeBind(this, options);
            return bound.name === undefined ? m : withReference(m, bound.name);
        }
    };
}

/** A caller that asks, then abandons: takes the name but never uses it. */
function askAndAbandon(module: string, bound: {name?: string}) {
    return new class extends JavaScriptVisitor<any> {
        override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
            if (m.name.simpleName !== "target") {
                return super.visitMethodInvocation(m, p);
            }
            bound.name = maybeBind(this, {module});
            return m;
        }
    };
}

describe("maybeBind", () => {
    test("AMD appends to both lists and reports the new name", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["sap/m/Button"], function (Button) { target(); });`,
            `sap.ui.define(["sap/m/Button", "sap/ui/core/Element"], function (Button, Element) { Element.target(); });`
        ));
        expect(bound.name).toBe("Element");
    });

    test("AMD accepts member: \"default\", which names the same whole module as no member at all", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (m.name.simpleName !== "target") {
                    return super.visitMethodInvocation(m, p);
                }
                bound.name = maybeBind(this, {module: "sap/ui/core/Element", member: "default"});
                return bound.name === undefined ? m : withReference(m, bound.name);
            }
        });
        await spec.rewriteRun(javascript(
            `sap.ui.define(["sap/m/Button"], function (Button) { target(); });`,
            `sap.ui.define(["sap/m/Button", "sap/ui/core/Element"], function (Button, Element) { Element.target(); });`
        ));
        expect(bound.name).toBe("Element");
    });

    test("AMD appends to an expression-bodied arrow factory same as a block-bodied one", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B"], (B) => target());`,
            `sap.ui.define(["a/B", "sap/ui/core/Element"], (B, Element) => Element.target());`
        ));
        expect(bound.name).toBe("Element");
    });

    test("AMD reuses an existing dependency's parameter and edits nothing on that lane", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["sap/ui/core/Element"], function (Elem) { target(); });`,
            `sap.ui.define(["sap/ui/core/Element"], function (Elem) { Elem.target(); });`
        ));
        expect(bound.name).toBe("Elem");
    });

    test("AMD reuses an existing dependency's parameter of an arrow factory", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["sap/ui/core/Element"], (Elem) => target());`,
            `sap.ui.define(["sap/ui/core/Element"], (Elem) => Elem.target());`
        ));
        expect(bound.name).toBe("Elem");
    });

    test("AMD avoids a name the factory body declares", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define([], function () { var Element = 1; target(); });`,
            `sap.ui.define(["sap/ui/core/Element"], function (Element_1) { var Element = 1; Element_1.target(); });`
        ));
        expect(bound.name).toBe("Element_1");
    });

    test("AMD avoids a global the factory body reads", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define([], function () { return function (d) { return d instanceof Element ? target() : null; }; });`,
            `sap.ui.define(["sap/ui/core/Element"], function (Element_1) { return function (d) { return d instanceof Element ? Element_1.target() : null; }; });`
        ));
        expect(bound.name).toBe("Element_1");
    });

    test("AMD avoids a global an expression-bodied arrow factory reads", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("my/lib/Object", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B"], (B) => Object.keys(target()));`,
            `sap.ui.define(["a/B", "my/lib/Object"], (B, Object_1) => Object.keys(Object_1.target()));`
        ));
        expect(bound.name).toBe("Object_1");
    });

    test("AMD binds a pinned alias over a global the factory reads, since taking it over is the caller's intent", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind({module: "sap/ui/thirdparty/jquery", alias: "jQuery"}, bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define([], function () { jQuery('x'); target(); });`,
            `sap.ui.define(["sap/ui/thirdparty/jquery"], function (jQuery) { jQuery('x'); jQuery.target(); });`
        ));
        expect(bound.name).toBe("jQuery");
    });

    test("AMD avoids a name a destructuring pattern binds", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define([], function () { const {Element} = window; target(); });`,
            `sap.ui.define(["sap/ui/core/Element"], function (Element_1) { const {Element} = window; Element_1.target(); });`
        ));
        expect(bound.name).toBe("Element_1");
    });

    test("AMD avoids a name a nested destructuring pattern binds", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Deep", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define([], function () { const [{Deep}] = window; target(); });`,
            `sap.ui.define(["sap/ui/core/Deep"], function (Deep_1) { const [{Deep}] = window; Deep_1.target(); });`
        ));
        expect(bound.name).toBe("Deep_1");
    });

    test("AMD avoids a name a rest element binds", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define([], function () { const [...Element] = window; target(); });`,
            `sap.ui.define(["sap/ui/core/Element"], function (Element_1) { const [...Element] = window; Element_1.target(); });`
        ));
        expect(bound.name).toBe("Element_1");
    });

    test("AMD refuses where a parameter would pair with the wrong dependency", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["sap/m/Button", "x/Y"], function (Button) { target(); });`
        ));
        expect(bound.name).toBeUndefined();
    });

    test("AMD refuses where a surplus parameter would pair with the new dependency", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["sap/m/Button"], function (Button, Extra) { target(); });`
        ));
        expect(bound.name).toBeUndefined();
    });

    test("a binding nothing goes on to reference is not written", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(askAndAbandon("sap/ui/core/Element", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["sap/m/Button"], function (Button) { target(); });`
        ));
        expect(bound.name).toBe("Element");
    });

    test("two calls for the same module at one block share the reservation", async () => {
        const spec = new RecipeSpec();
        const bound1: {name?: string} = {};
        const bound2: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (m.name.simpleName !== "target") {
                    return super.visitMethodInvocation(m, p);
                }
                const bound = bound1.name === undefined ? bound1 : bound2;
                bound.name = maybeBind(this, {module: "sap/ui/core/Element"});
                return bound.name === undefined ? m : withReference(m, bound.name);
            }
        });
        await spec.rewriteRun(javascript(
            `sap.ui.define([], function () { target(); target(); });`,
            `sap.ui.define(["sap/ui/core/Element"], function (Element) { Element.target(); Element.target(); });`
        ));
        expect(bound1.name).toBe("Element");
        expect(bound2.name).toBe("Element");
    });

    test("two different modules whose preferred names collide deconflict against each other", async () => {
        const spec = new RecipeSpec();
        const boundA: {name?: string} = {};
        const boundB: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (m.name.simpleName === "targetA") {
                    boundA.name = maybeBind(this, {module: "a/Element"});
                    return boundA.name === undefined ? m : withReference(m, boundA.name);
                }
                if (m.name.simpleName === "targetB") {
                    boundB.name = maybeBind(this, {module: "b/Element"});
                    return boundB.name === undefined ? m : withReference(m, boundB.name);
                }
                return super.visitMethodInvocation(m, p);
            }
        });
        await spec.rewriteRun(javascript(
            `sap.ui.define([], function () { targetA(); targetB(); });`,
            `sap.ui.define(["a/Element", "b/Element"], function (Element, Element_1) { Element.targetA(); Element_1.targetB(); });`
        ));
        expect(boundA.name).toBe("Element");
        expect(boundB.name).toBe("Element_1");
    });

    test("a block replaced before the deferred edit runs is an error, not a silent skip", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (m.name.simpleName === "target") {
                    bound.name = maybeBind(this, {module: "sap/ui/core/Element"});
                    return m;
                }
                const visited = await super.visitMethodInvocation(m, p) as J.MethodInvocation;
                // Simulate an unrelated edit that replaces the block's own node identity, so the
                // id the deferred edit was queued against no longer exists anywhere in the tree.
                return isAmdBlock(visited) ? {...visited, id: randomId()} : visited;
            }
        });
        await expect(spec.rewriteRun(javascript(
            `sap.ui.define(["sap/m/Button"], function (Button) { target(); });`
        ))).rejects.toThrow(/No AMD block/);
    });

    test("a parameter dropped elsewhere in the same visit rejects the queued dependency", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (m.name.simpleName === "target") {
                    const name = maybeBind(this, {module: "sap/ui/core/Element"});
                    return name === undefined ? m : withReference(m, name);
                }
                const visited = await super.visitMethodInvocation(m, p) as J.MethodInvocation;
                if (!isAmdBlock(visited)) {
                    return visited;
                }
                // Simulate an unrelated edit in the same visit that drops the factory's only
                // parameter without removing the dependency it was paired to, reopening the gap
                // bindAmd's own check had already cleared.
                const factoryArg = visited.arguments.elements[1].element as JS.StatementExpression;
                const method = factoryArg.statement as J.MethodDeclaration;
                const empty: J.Empty = {kind: J.Kind.Empty, id: randomId(), prefix: emptySpace, markers: emptyMarkers};
                const stripped: J.MethodDeclaration = {
                    ...method,
                    parameters: {...method.parameters, elements: [rightPadded(empty, emptySpace)]}
                };
                const strippedFactory: JS.StatementExpression = {...factoryArg, statement: stripped};
                const args = [...visited.arguments.elements];
                args[1] = {...args[1], element: strippedFactory};
                const withStrippedFactory: J.MethodInvocation = {...visited, arguments: {...visited.arguments, elements: args}};
                return withStrippedFactory;
            }
        });
        await expect(spec.rewriteRun(javascript(
            `sap.ui.define(["a/B"], function (B) { target(); });`
        ))).rejects.toThrow(/no longer has matching dependency and parameter counts/);
    });

    test("ESM creates a default import and reports its name", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        //language=typescript
        await spec.rewriteRun(typescript(
            `target();`,
            `
            import Element from 'sap/ui/core/Element';

            Element.target();`
        ));
        expect(bound.name).toBe("Element");
    });

    test("a bare string is shorthand for {module}", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (m.name.simpleName !== "target") {
                    return super.visitMethodInvocation(m, p);
                }
                bound.name = maybeBind(this, "sap/ui/core/Element");
                return bound.name === undefined ? m : withReference(m, bound.name);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `target();`,
            `
            import Element from 'sap/ui/core/Element';

            Element.target();`
        ));
        expect(bound.name).toBe("Element");
    });

    test("refuses rather than derive an illegal identifier from the module's last segment", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name = maybeBind(this, "lodash-es");
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await spec.rewriteRun(typescript(`const x = 1;`));
        expect(bound.name).toBeUndefined();
    });

    test("refuses rather than derive a reserved word from the module's last segment", async () => {
        const hardKeyword = new RecipeSpec();
        const hardKeywordBound: {name?: string} = {};
        hardKeyword.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                hardKeywordBound.name = maybeBind(this, "a/class");
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await hardKeyword.rewriteRun(typescript(`const x = 1;`));
        expect(hardKeywordBound.name).toBeUndefined();

        const contextualKeyword = new RecipeSpec();
        const contextualKeywordBound: {name?: string} = {};
        contextualKeyword.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                contextualKeywordBound.name = maybeBind(this, "a/await");
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await contextualKeyword.rewriteRun(typescript(`const x = 1;`));
        expect(contextualKeywordBound.name).toBeUndefined();
    });

    test("a pinned alias names a default or namespace import whose module derives no identifier", async () => {
        const spec = new RecipeSpec();
        const bound: {defaultName?: string, namespaceName?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.defaultName = maybeBind(this, {module: "react-dom", member: "default", alias: "ReactDOM", onlyIfReferenced: false});
                bound.namespaceName = maybeBind(this, {module: "prop-types", member: "*", alias: "PropTypes", onlyIfReferenced: false});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `const x = 1;`,
            `
            import ReactDOM from 'react-dom';
            import * as PropTypes from 'prop-types';

            const x = 1;`
        ));
        expect(bound).toEqual({defaultName: "ReactDOM", namespaceName: "PropTypes"});
    });

    test("AMD refuses where the declared dependency's parameter is not a name to hand back", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("a/B", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B"], function ({x}) { target(); });`
        ));
        expect(bound.name).toBeUndefined();
    });

    test("AMD reuses a parameter its own body redeclares", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("a/B", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B"], function (B) { var B = B || {}; target(); });`,
            `sap.ui.define(["a/B"], function (B) { var B = B || {}; B.target(); });`
        ));
        expect(bound.name).toBe("B");
    });

    test("AMD reuses a parameter for a cursor the factory does not enclose", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (isAmdBlock(m)) {
                    bound.name = maybeBind(this, "a/B");
                }
                return super.visitMethodInvocation(m, p);
            }
        });
        await spec.rewriteRun(javascript(`sap.ui.define(["a/B"], function (B) { B.x(); });`));
        expect(bound.name).toBe("B");
    });

    test("AMD binds a module afresh where the parameter it would reuse is shadowed", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("a/B", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B"], function (B) { function inner() { var B = 1; target(); return B; } });`,
            `sap.ui.define(["a/B", "a/B"], function (B, B_1) { function inner() { var B = 1; B_1.target(); return B; } });`
        ));
        expect(bound.name).toBe("B_1");
    });

    test("AMD reuses a later dependency on the module where the first one's parameter is shadowed", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("a/B", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B", "a/B"], function (B, B_1) { function inner() { var B = 1; target(); return B; } });`,
            `sap.ui.define(["a/B", "a/B"], function (B, B_1) { function inner() { var B = 1; B_1.target(); return B; } });`
        ));
        expect(bound.name).toBe("B_1");
    });

    test("ESM binds a module afresh where the import it would reuse is shadowed", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/m/Button", bound));
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import Button from "sap/m/Button";

            function inner() { const Button = 1; target(); return Button; }`,
            `
            import Button from "sap/m/Button";
            import Button_1 from "sap/m/Button";

            function inner() { const Button = 1; Button_1.target(); return Button; }`
        ));
        expect(bound.name).toBe("Button_1");

        // A `const` binds its whole block, so reaching the import ahead of one is a TDZ error
        // rather than a reference to the module — the ordering of the two does not change that.
        const declaredLater = new RecipeSpec();
        declaredLater.recipe = fromVisitor(rebind("sap/m/Button", bound));
        //language=typescript
        await declaredLater.rewriteRun(typescript(
            `
            import Button from "sap/m/Button";

            function inner() { target(); const Button = 1; return Button; }`,
            `
            import Button from "sap/m/Button";
            import Button_1 from "sap/m/Button";

            function inner() { Button_1.target(); const Button = 1; return Button; }`
        ));
        expect(bound.name).toBe("Button_1");
    });

    test("ESM binds a member afresh where the specifier it would reuse is shadowed", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind({module: "a/lib", member: "Widget"}, bound));
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {Widget} from "a/lib";

            function inner() { const Widget = 1; target(); return Widget; }`,
            `
            import {Widget, Widget as Widget_1} from "a/lib";

            function inner() { const Widget = 1; Widget_1.target(); return Widget; }`
        ));
        expect(bound.name).toBe("Widget_1");
    });

    test("ESM reuses a default import bound under another name", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import Elem from "sap/ui/core/Element";

            target();`,
            `
            import Elem from "sap/ui/core/Element";

            Elem.target();`
        ));
        expect(bound.name).toBe("Elem");
    });

    test("a namespace import answers a namespace request but not a plain module request", async () => {
        const namespaceRequest = new RecipeSpec();
        const namespaceName: {value?: string} = {};
        namespaceRequest.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                namespaceName.value = maybeBind(this, {module: "sap/m/Button", member: "*"});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await namespaceRequest.rewriteRun(typescript(
            `
            import * as Button from "sap/m/Button";

            target();`
        ));

        const bareRequest = new RecipeSpec();
        const bound: {name?: string} = {};
        bareRequest.recipe = fromVisitor(rebind("sap/m/Button", bound));
        //language=typescript
        await bareRequest.rewriteRun(typescript(
            `
            import * as Button from "sap/m/Button";

            target();`,
            `
            import * as Button from "sap/m/Button";
            import Button_1 from "sap/m/Button";

            Button_1.target();`
        ));

        expect(namespaceName.value).toBe("Button");
        expect(bound.name).toBe("Button_1");
    });

    test("a require binding answers a namespace request too, since CommonJS has no default/namespace split", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name = maybeBind(this, {module: "sap/ui/core/Element", member: "*"});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=javascript
        await spec.rewriteRun(javascript(
            `
            const Elem = require("sap/ui/core/Element");

            target();`
        ));
        expect(bound.name).toBe("Elem");
    });

    test("an import X = require binding answers a plain module request and a namespace request, as a require does", async () => {
        const spec = new RecipeSpec();
        let bound: (string | undefined)[] | undefined;
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound ??= [
                    maybeBind(this, {module: "sap/ui/core/Element", onlyIfReferenced: false}),
                    maybeBind(this, {module: "sap/ui/core/Element", member: "*", onlyIfReferenced: false})
                ];
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import Elem = require("sap/ui/core/Element");

                target();
            `
        ));
        expect(bound).toEqual(["Elem", "Elem"]);
    });

    test("a whole-module await import answers a namespace request but not a plain module request", async () => {
        const namespaceRequest = new RecipeSpec();
        const namespaceName: {value?: string} = {};
        namespaceRequest.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                namespaceName.value = maybeBind(this, {module: "sap/m/Button", member: "*"});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await namespaceRequest.rewriteRun(typescript(
            `
            const Button = await import("sap/m/Button");

            target();`
        ));

        const bareRequest = new RecipeSpec();
        const bound: {name?: string} = {};
        bareRequest.recipe = fromVisitor(rebind("sap/m/Button", bound));
        //language=typescript
        await bareRequest.rewriteRun(typescript(
            `
            const Button = await import("sap/m/Button");

            target();`,
            `
            import Button_1 from "sap/m/Button";

            const Button = await import("sap/m/Button");

            Button_1.target();`
        ));

        expect(namespaceName.value).toBe("Button");
        expect(bound.name).toBe("Button_1");
    });

    test("a whole-module await import.defer answers a namespace request", async () => {
        const spec = new RecipeSpec();
        const namespaceName: {value?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                namespaceName.value = maybeBind(this, {module: "sap/m/Button", member: "*"});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            const Button = await import.defer("sap/m/Button");

            target();`
        ));

        expect(namespaceName.value).toBe("Button");
    });

    test("a destructured await import answers neither request but still occupies its name", async () => {
        const spec = new RecipeSpec();
        let bound: (string | undefined)[] | undefined;
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                const answers = [
                    maybeBind(this, {module: "sap/m/Button", onlyIfReferenced: false}),
                    maybeBind(this, {module: "sap/m/Button", member: "*", onlyIfReferenced: false})
                ];
                bound ??= answers;
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            const {Button} = await import("sap/m/Button");

            target();`,
            `
            import Button_1 from "sap/m/Button";
            import * as Button_2 from "sap/m/Button";

            const {Button} = await import("sap/m/Button");

            target();`
        ));
        expect(bound).toEqual(["Button_1", "Button_2"]);
    });

    test("a CommonJS file answers with the binding its require already has", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        //language=javascript
        await spec.rewriteRun(javascript(
            `
            const Elem = require("sap/ui/core/Element");

            target();`,
            `
            const Elem = require("sap/ui/core/Element");

            Elem.target();`
        ));
        expect(bound.name).toBe("Elem");
    });

    test("a .mjs file with a require call still creates an import", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        await spec.rewriteRun({
            //language=javascript
            ...javascript(
                `
                const other = require("a/Other");

                target();`,
                `
                import Element from "sap/ui/core/Element";

                const other = require("a/Other");

                Element.target();`
            ),
            path: "module.mjs"
        });
        expect(bound.name).toBe("Element");
    });

    test("a nested require callback is the block a binding belongs to", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B"], function (B) { sap.ui.require([], function () { target(); }); });`,
            `sap.ui.define(["a/B"], function (B) { sap.ui.require(["sap/ui/core/Element"], function (Element) { Element.target(); }); });`
        ));
        expect(bound.name).toBe("Element");
    });

    test("the deprecated maybeAddImport still works, returning what the equivalent maybeBind call would", async () => {
        const bindWith = (bind: (visitor: JavaScriptVisitor<any>) => string | undefined) => {
            const bound: {name?: string} = {};
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                    if (m.name.simpleName === "target") {
                        bound.name = bind(this);
                    }
                    return super.visitMethodInvocation(m, p);
                }
            });
            //language=typescript
            return spec.rewriteRun(typescript(
                `target();`,
                `
                import {readFile} from 'fs';

                target();`
            )).then(() => bound.name);
        };

        const viaAddImport = await bindWith(v => maybeAddImport(v, {module: "fs", member: "readFile", onlyIfReferenced: false}));
        const viaBind = await bindWith(v => maybeBind(v, {module: "fs", member: "readFile", onlyIfReferenced: false}));
        expect(viaAddImport).toBe("readFile");
        expect(viaBind).toBe("readFile");
    });
});

    test("a value request turns a type-only import of the module into a value import", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("m", bound));
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import type X from "m";
            target();`,
            `
            import X from "m";
            X.target();`));
        expect(bound.name).toBe("X");
    });

    test("a file that exports is a module, whatever else it requires", async () => {
        // `export` settles the module system, so a `require` alongside it does not make the file
        // CommonJS and does not block creating an import.
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("m", bound));
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            export function f() {}
            const p = require("path");
            target();`,
            `
            import m from "m";

            export function f() {}
            const p = require("path");
            m.target();`));
        expect(bound.name).toBe("m");
    });

    test("an alias is bound verbatim or not at all", async () => {
        // A caller naming an alias may already have emitted code using it, so another name for
        // the same module does not answer the request and a deconflicted spelling is refused.
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (m.name.simpleName !== "target") {
                    return super.visitMethodInvocation(m, p);
                }
                bound.name = maybeBind(this, {module: "m", alias: "Pinned"});
                return bound.name === undefined ? m : withReference(m, bound.name);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import Y from "m";
            target();`,
            `
            import Y from "m";
            import Pinned from "m";
            Pinned.target();`));
        expect(bound.name).toBe("Pinned");
    });

    test("a factory parameter clears every name the factory declares, not only those in scope", async () => {
        // The parameter is visible across the whole body, and the queue hands its name to later
        // requests at cursors inside it, so a name declared in a nested scope still shadows it.
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("a/Theming", bound));
        await spec.rewriteRun(javascript(
            `sap.ui.define([], function () { target(); function f() { var Theming = 1; return Theming; } });`,
            `sap.ui.define(["a/Theming"], function (Theming_1) { Theming_1.target(); function f() { var Theming = 1; return Theming; } });`));
        expect(bound.name).toBe("Theming_1");
    });

/** Writes a package under `root`'s `node_modules`, so a parse there resolves and attributes it. */
function installPackage(root: string, name: string, files: Record<string, string>) {
    for (const [file, content] of Object.entries(files)) {
        const target = path.join(root, "node_modules", name, file);
        fs.mkdirSync(path.dirname(target), {recursive: true});
        fs.writeFileSync(target, content);
    }
}

const httpApi = `export declare class Http { get(url: string): string; post(url: string, body: Headers): string; }
export declare class Headers { append(name: string, value: string): Headers; }
export declare class HttpModule {}
export declare function request(url: string): string;
`;

/** The same API under new names, published from a subpath of a scoped package. */
function installHttpPackages(root: string) {
    installPackage(root, "legacy-http", {
        "package.json": `{"name":"legacy-http","version":"1.0.0","types":"index.d.ts"}`,
        "index.d.ts": httpApi,
        "compat/package.json": `{"types":"index.d.ts"}`,
        "compat/index.d.ts": httpApi
    });
    installPackage(root, "@acme/common", {
        "package.json": `{"name":"@acme/common","version":"1.0.0","types":"index.d.ts"}`,
        "index.d.ts": `export declare const VERSION: string;\n`,
        "http/package.json": `{"types":"index.d.ts"}`,
        "http/index.d.ts": httpApi
            .replace(/\bHttp\b/g, "HttpClient")
            .replace(/\bHeaders\b/g, "HttpHeaders")
            .replace(/\bHttpModule\b/g, "HttpClientModule")
    });
}

/** The old and the new name of one API, side by side in one package. */
function installHttpApi(root: string) {
    installPackage(root, "http-api", {
        "package.json": `{"name":"http-api","version":"1.0.0","types":"index.d.ts"}`,
        "index.d.ts": `
            export declare class Http { get(url: string): string; }
            export declare class HttpClient { get(url: string): string; }
            export declare function request(url: string): string;
            export declare function send(url: string): string;
        `
    });
}

/** The attribution of `before` once `move` ran on it, and of `after` parsed fresh, beside the packages `install` writes. */
async function attributionsAfter(
    move: MaybeRebindOptions,
    install: (root: string) => void,
    before: string,
    after: string
): Promise<{rebound: string[], parsed: string[]}> {
    const spec = new RecipeSpec();
    spec.recipe = rebindEach([move]);
    const attributions = {rebound: [] as string[], parsed: [] as string[]};
    await withDir(async (repo) => {
        install(repo.path);
        await spec.rewriteRun(npm(repo.path, {
            ...typescript(before, after),
            afterRecipe: async (cu: JS.CompilationUnit) => {
                attributions.rebound = await attributionOf(cu);
            }
        } as any, packageJson(`{"name":"t"}`)));
        await new RecipeSpec().rewriteRun(npm(repo.path, {
            ...typescript(after),
            afterRecipe: async (cu: JS.CompilationUnit) => {
                attributions.parsed = await attributionOf(cu);
            }
        } as any, packageJson(`{"name":"t"}`)));
    }, {unsafeCleanup: true});
    return attributions;
}

/** The signature of every type a tree outside the imports of `cu` is attributed to. */
async function attributionOf(cu: JS.CompilationUnit): Promise<string[]> {
    const render = Type.signature;
    const seen: string[] = [];
    await new class extends JavaScriptVisitor<any> {
        override async visitImportDeclaration(jsImport: JS.Import): Promise<J | undefined> {
            return jsImport;
        }
        override async visitIdentifier(i: J.Identifier, p: any): Promise<J | undefined> {
            seen.push(`${i.simpleName} ${render(i.type)} ${render(i.fieldType)}`);
            return super.visitIdentifier(i, p);
        }
        override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
            seen.push(`${m.name.simpleName}() ${render(m.methodType)}`);
            return super.visitMethodInvocation(m, p);
        }
        override async visitNewClass(nc: J.NewClass, p: any): Promise<J | undefined> {
            seen.push(`new ${render(nc.constructorType)}`);
            return super.visitNewClass(nc, p);
        }
    }().visit(cu, undefined);
    return seen;
}

function rebindOldToNew() {
    return new class extends JavaScriptVisitor<any> {
        override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
            maybeRebind(this, {from: {module: "m", member: "Old"}, to: {module: "m2", member: "New"}});
            return super.visitJsCompilationUnit(cu, p);
        }
    };
}

function rebindTo(to: MaybeRebindOptions["to"], bound: {name?: string}, from: MaybeRebindOptions["from"] = {module: "m", member: "Old"}) {
    return fromVisitor(new class extends JavaScriptVisitor<any> {
        override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
            bound.name ??= maybeRebind(this, {from, to});
            return super.visitJsCompilationUnit(cu, p);
        }
    });
}

/** Calls `maybeRebind` once per move, as a recipe moving several members does. */
function rebindEach(moves: MaybeRebindOptions[]) {
    return fromVisitor(new class extends JavaScriptVisitor<any> {
        override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
            for (const move of moves) {
                maybeRebind(this, move);
            }
            return super.visitJsCompilationUnit(cu, p);
        }
    });
}

describe("maybeBind and type-only imports", () => {
    /** Binds `options` once, from the compilation unit, recording the name it answers with. */
    function bindOnce(options: MaybeBindOptions, bound: {name?: string}) {
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name = maybeBind(this, {...options, onlyIfReferenced: false});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
    }

    test("a type-only request reuses a value import under the name the file gives it", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = bindOnce({module: "vitest", member: "Mocked", typeOnly: true}, bound);
        //language=typescript
        await spec.rewriteRun(typescript(`
            import {Mocked as M} from "vitest";
            let m: M<{}>;`));
        expect(bound.name).toBe("M");
    });

    test("a value request turns the type-only import of its member into a value import", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = bindOnce({module: "vitest", member: "Mocked"}, bound);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import type {Mocked as M} from "vitest";
            let m: M<{}>;`,
            `
            import {Mocked as M} from "vitest";
            let m: M<{}>;`
        ));
        expect(bound.name).toBe("M");
    });

    test("the members a type-only statement also imports stay type-only", async () => {
        const spec = new RecipeSpec();
        spec.recipe = bindOnce({module: "vitest", member: "Mocked"}, {});
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import type {MockInstance, Mocked} from "vitest";
            let m: Mocked<MockInstance>;`,
            `
            import {type MockInstance, Mocked} from "vitest";
            let m: Mocked<MockInstance>;`
        ));
    });

    test("a member marked type on its own is type-only, and a value request removes the mark", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = bindOnce({module: "vitest", member: "Mocked"}, bound);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {type Mocked, vi} from "vitest";
            let m: Mocked<typeof vi>;`,
            `
            import {Mocked, vi} from "vitest";
            let m: Mocked<typeof vi>;`
        ));
        expect(bound.name).toBe("Mocked");
    });

    test("a type-only request takes the name a value request queued for the same member", async () => {
        const spec = new RecipeSpec();
        let names: (string | undefined)[] | undefined;
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                const answers = [
                    maybeBind(this, {module: "vitest", member: "vi", onlyIfReferenced: false}),
                    maybeBind(this, {module: "vitest", member: "vi", typeOnly: true, onlyIfReferenced: false})
                ];
                names ??= answers;
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(`const vi = 1;`, `
            import {vi as vi_1} from 'vitest';

            const vi = 1;`));
        expect(names).toEqual(["vi_1", "vi_1"]);
    });

    test("a type-only request beside a type-only default import gets an import of its own", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = bindOnce({module: "react", member: "ReactElement", typeOnly: true}, bound);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import type R from "react";
                let a: R.ReactChild;
            `,
            `
                import type R from "react";
                import type {ReactElement} from "react";
                let a: R.ReactChild;
            `
        ));
        expect(bound.name).toBe("ReactElement");
    });

    test("a namespace import answers a type-only request for the whole module", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = bindOnce({module: "vitest", member: "*", typeOnly: true}, bound);
        //language=typescript
        await spec.rewriteRun(typescript(`
            import * as v from "vitest";
            let m: v.Mocked<{}>;`));
        expect(bound.name).toBe("v");
    });

    test("a member imported under its string-literal name answers a request for it", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = bindOnce({module: "lodash", member: "assign"}, bound);
        //language=typescript
        await spec.rewriteRun(typescript(`
            import { "assign" as merge } from "lodash";

            merge({}, {});`));
        expect(bound.name).toBe("merge");
    });
});

describe("maybeRebind", () => {
    test("an ESM member rename takes the new name, carrying the references that resolve to it", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name ??= maybeRebind(this, {
                    from: {module: "lodash", member: "extend"},
                    to: {module: "lodash", member: "assign"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { extend } from "lodash";

            extend({}, {});
            obj.extend();
            function f() { const extend = 1; return extend; }`,
            `
            import { assign } from "lodash";

            assign({}, {});
            obj.extend();
            function f() { const extend = 1; return extend; }`
        ));
        expect(bound.name).toBe("assign");
    });

    test("a pinned alias names the binding, and the references follow it", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name ??= maybeRebind(this, {
                    from: {module: "lodash", member: "extend"},
                    to: {module: "lodash", member: "assign", alias: "merge"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { extend } from "lodash";

            extend({}, {});`,
            `
            import { assign as merge } from "lodash";

            merge({}, {});`
        ));
        expect(bound.name).toBe("merge");
    });

    test("an aliased member keeps the alias, which is a name of the file's own choosing", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name ??= maybeRebind(this, {
                    from: {module: "lodash", member: "extend"},
                    to: {module: "lodash", member: "assign"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { extend as myExtend } from "lodash";

            myExtend({}, {});`,
            `
            import { assign as myExtend } from "lodash";

            myExtend({}, {});`
        ));
        expect(bound.name).toBe("myExtend");
    });

    test("an alias the new member name makes redundant is dropped", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name ??= maybeRebind(this, {
                    from: {module: "lodash", member: "extend"},
                    to: {module: "lodash", member: "assign"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { extend as assign } from "lodash";

            assign({}, {});`,
            `
            import { assign } from "lodash";

            assign({}, {});`
        ));
        expect(bound.name).toBe("assign");
    });

    test("a member imported under its string-literal name keeps that form and its quotes", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name ??= maybeRebind(this, {
                    from: {module: "@angular/http", member: "Http"},
                    to: {module: "@angular/common/http", member: "HttpClient"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {'Http' as AngularHttp} from '@angular/http';

            export class WidgetService { constructor(private http: AngularHttp) {} }`,
            `
            import {'HttpClient' as AngularHttp} from '@angular/common/http';

            export class WidgetService { constructor(private http: AngularHttp) {} }`
        ));
        expect(bound.name).toBe("AngularHttp");
    });

    test("a string-literal name escapes the quote it is spelled with", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "m", member: "a-b"}, to: {module: "m", member: "it's"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {'a-b' as x} from 'm';

            x();`,
            `
            import {'it\\'s' as x} from 'm';

            x();`
        ));
    });

    test("a string-literal name the new member makes redundant becomes a bare specifier", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name ??= maybeRebind(this, {
                    from: {module: "lodash", member: "extend"},
                    to: {module: "lodash", member: "assign"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { "extend" as assign } from "lodash";

            assign({}, {});`,
            `
            import { assign } from "lodash";

            assign({}, {});`
        ));
        expect(bound.name).toBe("assign");
    });

    test("a re-export keeps the name the module publishes, and the binding under it follows", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { Old } from "m";

            export { Old };
            const y = Old;`,
            `
            import { New } from "m2";

            export { New as Old };
            const y = New;`
        ));
    });

    test("an aliased re-export renames only the reference beside the name it publishes", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { Old } from "m";

            const X = 1;
            export { Old as Public };
            export { X as Old };`,
            `
            import { New } from "m2";

            const X = 1;
            export { New as Public };
            export { X as Old };`
        ));
    });

    test("a shorthand property keeps its key and takes the renamed binding as its value", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { Old } from "m";

            const p = {Old};
            const o = {Old: 1};`,
            `
            import { New } from "m2";

            const p = {Old: New};
            const o = {Old: 1};`
        ));
    });

    test("a pinned alias the file cannot bind verbatim refuses, leaving the import alone", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name = maybeRebind(this, {
                    from: {module: "m", member: "Old"},
                    to: {module: "m2", member: "New", alias: "taken"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(`
            import { Old } from "m";

            const taken = 1;
            Old();`));
        expect(bound.name).toBeUndefined();
    });

    test("an AMD alias pin refuses where it renames the parameter, and passes where it agrees", async () => {
        const amdRebind = (alias: string, bound: {name?: string}) => new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (m.name.simpleName !== "target") {
                    return super.visitMethodInvocation(m, p);
                }
                bound.name ??= maybeRebind(this, {from: {module: "a/Old"}, to: {module: "a/New", alias}});
                return m;
            }
        };

        const renames = new RecipeSpec();
        const renamesBound: {name?: string} = {};
        renames.recipe = fromVisitor(amdRebind("Renamed", renamesBound));
        await renames.rewriteRun(javascript(`sap.ui.define(["a/Old"], function (Old) { target(); });`));
        expect(renamesBound.name).toBeUndefined();

        const agrees = new RecipeSpec();
        const agreesBound: {name?: string} = {};
        agrees.recipe = fromVisitor(amdRebind("Old", agreesBound));
        await agrees.rewriteRun(javascript(
            `sap.ui.define(["a/Old"], function (Old) { target(); });`,
            `sap.ui.define(["a/New"], function (Old) { target(); });`));
        expect(agreesBound.name).toBe("Old");
    });

    test("an alias that is not a legal identifier refuses rather than emitting unparseable source", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name = maybeRebind(this, {
                    from: {module: "m", member: "Old"},
                    to: {module: "m2", member: "New", alias: "my-name"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(`
            import { Old } from "m";

            const y = Old;`));
        expect(bound.name).toBeUndefined();
    });

    test("a name the file only references, never declares, still counts as taken", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name ??= maybeRebind(this, {
                    from: {module: "m", member: "Old"},
                    to: {module: "m2", member: "Event"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        // `Event` is an ambient global: the file declares it nowhere, so a check reading only
        // declarations would call the name free and let the import shadow it here.
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { Old } from "m";

            function f(e: Event) { return Old(e); }`,
            `
            import { Event as Old } from "m2";

            function f(e: Event) { return Old(e); }`
        ));
        expect(bound.name).toBe("Old");
    });

    test("a name the file already binds to the target itself is free, pinned as an alias or not", async () => {
        const named: {name?: string} = {};
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m2", member: "New"}, named);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { New, Other } from "m2";
            import { Old } from "m";

            Old(New, Other);`,
            `
            import { New, Other } from "m2";

            New(New, Other);`
        ));
        expect(named.name).toBe("New");

        const pinned: {name?: string} = {};
        spec.recipe = rebindTo({module: "m2", member: "New", alias: "N"}, pinned);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { New as N } from "m2";
            import { Old } from "m";

            Old(N);`,
            `
            import { New as N } from "m2";

            N(N);`
        ));
        expect(pinned.name).toBe("N");
    });

    test("a target binding the move cannot merge into, or whose name the file also declares, still counts as taken", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                import type { New } from "m2";
                import { Old } from "m";

                Old(1 as unknown as New);`,
                `
                import type { New } from "m2";
                import { New as Old } from "m2";

                Old(1 as unknown as New);`
            ),
            //language=typescript
            typescript(
                `
                import { New } from "m2";
                import type { Old } from "m";

                let x: Old = New;`,
                `
                import { New } from "m2";
                import type { New as Old } from "m2";

                let x: Old = New;`
            ),
            //language=typescript
            typescript(
                `
                import { New } from "m2";
                import { Old } from "m";

                function f(New: number) { return Old(New); }`,
                `
                import { New, New as Old } from "m2";

                function f(New: number) { return Old(New); }`
            ),
            //language=typescript
            typescript(
                `
                import { New } from "m2";
                import { Old } from "m";

                enum E { New = 1, X = Old }
                New();`,
                `
                import { New, New as Old } from "m2";

                enum E { New = 1, X = Old }
                New();`
            ),
            //language=typescript
            typescript(
                `
                import { New } from "m2";
                import { Old } from "m";

                type M<K extends string> = { [New in K]: Old };
                New();`,
                `
                import { New, New as Old } from "m2";

                type M<K extends string> = { [New in K]: Old };
                New();`
            )
        );
    });

    test("a pinned alias on a namespace target, or a name a queued rebind claims, stays taken", async () => {
        const pinned: {name?: string} = {};
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m2", member: "*", alias: "NS"}, pinned, {module: "m", member: "*"});
        //language=typescript
        await spec.rewriteRun(typescript(`
            import * as NS from "m2";
            import * as Old from "m";

            Old.f(NS);`));
        expect(pinned.name).toBeUndefined();

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "m2", member: "New"}, to: {module: "m3", member: "New"}});
                maybeRebind(this, {from: {module: "m", member: "Old"}, to: {module: "m2", member: "New"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        spec.cycles = 1;
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { New } from "m2";
            import { Old } from "m";

            Old(New);`,
            `
            import { New } from "m3";
            import { New as Old } from "m2";

            Old(New);`
        ));
    });

    test("a reference in type-name position renames: a decorator, and a generic's own base", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { Old } from "m";

            @Old({})
            export class C {
                @Old() field: Old<string>;
            }`,
            `
            import { New } from "m2";

            @New({})
            export class C {
                @New() field: New<string>;
            }`
        ));
    });

    test("a name a bind queued earlier in the same visit has claimed is taken too", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeBind(this, {module: "other", member: "New", onlyIfReferenced: false});
                bound.name ??= maybeRebind(this, {
                    from: {module: "m", member: "Old"},
                    to: {module: "m2", member: "New"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { Old } from "m";

            const y = Old;`,
            `
            import { New as Old } from "m2";
            import { New } from "other";

            const y = Old;`
        ));
        expect(bound.name).toBe("Old");
    });

    test("a name a queued rebind claims answers a bind of its target and is taken for anything else", async () => {
        const spec = new RecipeSpec();
        const bound: {rebound?: string, target?: string, bound?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "m", member: "a"}, to: {module: "m2", member: "x"}});
                bound.rebound ??= maybeRebind(this, {from: {module: "m", member: "b"}, to: {module: "m3", member: "x"}});
                bound.target ??= maybeBind(this, {module: "m2", member: "x"});
                bound.bound ??= maybeBind(this, {module: "m4", member: "x", onlyIfReferenced: false});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {a, b} from "m";

            a(b);`,
            `
            import {x} from "m2";
            import {x as b} from "m3";
            import {x as x_1} from "m4";

            x(b);`
        ));
        expect(bound.rebound).toBe("b");
        expect(bound.target).toBe("x");
        expect(bound.bound).toBe("x_1");
    });

    test("a name in a namespace of its own — a nearer scope, a label — is left alone", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { Old } from "m";

            const z = Old;
            Old: for (;;) { break Old; }
            function f() { const Old = 1; return {Old}; }`,
            `
            import { New } from "m2";

            const z = New;
            Old: for (;;) { break Old; }
            function f() { const Old = 1; return {Old}; }`
        ));
    });

    test("a nearer binding hides only the reads of its own kind: a type hides no value read, a value no type read", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { Old } from "m";

            function f() { interface Old {} return Old.go(); }
            function g() { const Old = 1; let y: Old; return {Old}; }`,
            `
            import { New } from "m2";

            function f() { interface Old {} return New.go(); }
            function g() { const Old = 1; let y: New; return {Old}; }`
        ));
    });

    test("a binding whose name a module-scope value shares keeps it, as an alias", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = rebindTo({module: "m2", member: "New"}, bound);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import type { Old } from "m";

                const Old = 1;
                export const x = Old;
                export type C = Old;
                export { Old };
            `,
            `
                import type { New as Old } from "m2";

                const Old = 1;
                export const x = Old;
                export type C = Old;
                export { Old };
            `
        ));
        expect(bound.name).toBe("Old");
    });

    test("a call to a block's function stays on it when an import of that name moves", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import { Old } from "m";

                if (c) { function Old() {} Old(); }
                Old();
            `,
            `
                import { New as Old } from "m2";

                if (c) { function Old() {} Old(); }
                Old();
            `
        ));
    });

    test("a pinned alias cannot split an import from its same-named exported value", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = rebindTo({module: "m", member: "New", alias: "Renamed"}, bound);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import type {Old} from "m";
                const Old = 1;
                export {Old};
                export type T = Old;
            `
        ));
        expect(bound.name).toBeUndefined();
    });

    test("a value import of a type keeps its name beside a destructured value of that name", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import { Old } from "m";

                const { Old } = { Old: 1 };
                export const x = Old;
                export type C = Old<{}>;
            `,
            `
                import { New as Old } from "m2";

                const { Old } = { Old: 1 };
                export const x = Old;
                export type C = Old<{}>;
            `
        ));
    });

    test("a second specifier of the moved member stays as it stands", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = rebindTo({module: "m", member: "New"}, bound);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import { Old, Old as O } from "m";

                let a: Old;
                let b: O;
            `,
            `
                import { New, Old as O } from "m";

                let a: New;
                let b: O;
            `
        ));
        expect(bound.name).toBe("New");
    });

    test("a second specifier of the moved member stays as it stands when the move leaves the statement", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = rebindTo({module: "m2", member: "New"}, bound);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import { Old as O, Old } from "m";

                let a: Old;
                let b: O;
            `,
            `
                import { Old } from "m";
                import { New as O } from "m2";

                let a: Old;
                let b: O;
            `
        ));
        expect(bound.name).toBe("O");
    });

    test("a type-only binding moved beside a type-only default import of its target gets an import of its own", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = rebindTo({module: "react", member: "ReactElement"}, bound);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import type { Old } from "m";
                import type R from "react";

                let a: Old;
                let b: R.ReactChild;
            `,
            `
                import type { ReactElement } from "react";
                import type R from "react";

                let a: ReactElement;
                let b: R.ReactChild;
            `
        ));
        expect(bound.name).toBe("ReactElement");
    });

    test("a type-only import is renamed where `typeof` reads it as a value", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import type { Old } from "m";

            let y: typeof Old;
            let z: Old;`,
            `
            import type { New } from "m2";

            let y: typeof New;
            let z: New;`
        ));
    });

    test("a shorthand with a default expands like a bare one", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { Old } from "m";

            ({Old = 1} = o);`,
            `
            import { New } from "m2";

            ({Old: New = 1} = o);`
        ));
    });

    test("a rebind after one that expanded a shorthand still finds the reads", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "m", member: "Old"}, to: {module: "m2", member: "New"}});
                maybeRebind(this, {from: {module: "m2", member: "New"}, to: {module: "m3", member: "Newer"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        // The first rebind merges into the import the second one moves, and expands the shorthand on the way.
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { Old } from "m";
            import { New } from "m2";

            const o = {Old};
            New();`,
            `
            import { Newer } from "m3";

            const o = {Old: Newer};
            Newer();`
        ));
    });

    test("a re-export or an export alias of the target's name does not take it", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { New } from "m2";
            import { Old } from "m";

            export { New } from "./other";
            export { other as New };
            Old();`,
            `
            import { New } from "m2";

            export { New } from "./other";
            export { other as New };
            New();`
        ));
    });

    test("a re-export from another module names that module's member, not the binding", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { Old } from "m";

            export { Old } from "./other";
            const y = Old;`,
            `
            import { New } from "m2";

            export { Old } from "./other";
            const y = New;`
        ));
    });

    test("a second statement binding the same member is left intact, not given the first's name", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        spec.cycles = 1;
        // One call moves one binding, so the second statement is a later call's to make; sharing
        // the name read from the first would bind it twice.
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { Old } from "m";
            import { Old as Other } from "m";

            const y = Old;
            const z = Other;`,
            `
            import { New } from "m2";
            import { Old as Other } from "m";

            const y = New;
            const z = Other;`
        ));
    });

    test("the moved binding's attribution names what it moved to, and a sibling's is left alone", async () => {
        const spec = new RecipeSpec();
        const attribution: string[] = [];
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {
                    from: {module: "lodash", member: "extend"},
                    to: {module: "lodash-es", member: "assign"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await withDir(async (repo) => {
            await spec.rewriteRun(npm(repo.path, {
                //language=typescript
                ...typescript(
                    `
                    import { extend, flatten } from 'lodash';

                    extend({}, {});
                    flatten([[1]]);
                    `,
                    `
                    import { flatten } from 'lodash';
                    import { assign } from 'lodash-es';

                    assign({}, {});
                    flatten([[1]]);
                    `),
                afterRecipe: async (cu: any) => {
                    await new class extends JavaScriptVisitor<any> {
                        override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                            const declaringType = m.methodType?.declaringType;
                            attribution.push(`${declaringType && Type.isFullyQualified(declaringType)
                                ? Type.FullyQualified.getFullyQualifiedName(declaringType as any)
                                : undefined}.${m.methodType?.name}`);
                            return m;
                        }
                    }().visit(cu, undefined);
                }
            } as any, packageJson(`{"name":"t","dependencies":{"lodash":"^4.17.21","lodash-es":"^4.17.21","@types/lodash":"^4.14.202","@types/lodash-es":"^4.17.12"}}`)));
        }, {unsafeCleanup: true});
        // The member's own name moves with it; `flatten` was imported beside it and did not move.
        expect(attribution).toEqual(["lodash-es.assign", "lodash.flatten"]);
    });

    test("a member moved onto a default binding keeps a name, since a default declares none", async () => {
        const spec = new RecipeSpec();
        const names: (string | undefined)[] = [];
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {
                    from: {module: "lodash", member: "extend"},
                    to: {module: "lodash-es", member: "default"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await withDir(async (repo) => {
            await spec.rewriteRun(npm(repo.path, {
                //language=typescript
                ...typescript(
                    `
                    import { extend, flatten } from 'lodash';

                    extend({}, {});
                    flatten([[1]]);
                    `,
                    `
                    import { flatten } from 'lodash';
                    import extend from 'lodash-es';

                    extend({}, {});
                    flatten([[1]]);
                    `),
                afterRecipe: async (cu: any) => {
                    await new class extends JavaScriptVisitor<any> {
                        override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                            if (m.name.simpleName === "extend") {
                                names.push(m.methodType?.name);
                            }
                            return super.visitMethodInvocation(m, p);
                        }
                    }().visit(cu, undefined);
                }
            } as any, packageJson(`{"name":"t","dependencies":{"lodash":"^4.17.21","lodash-es":"^4.17.21","@types/lodash":"^4.14.202","@types/lodash-es":"^4.17.12","@types/node":"^20.0.0"}}`)));
        }, {unsafeCleanup: true});
        // `Type.Method.name` is declared `string`, so the move has to leave one behind.
        expect(names).toEqual(["extend"]);
    });

    test("a whole-module move carries the types named after the module", async () => {
        const spec = new RecipeSpec();
        const named: (string | undefined)[] = [];
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "events"}, to: {module: "node:stream"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await withDir(async (repo) => {
            await spec.rewriteRun(npm(repo.path, {
                //language=typescript
                ...typescript(
                    `
                    import ev from 'events';

                    const e = new ev();
                    `,
                    `
                    import ev from 'node:stream';

                    const e = new ev();
                    `),
                afterRecipe: async (cu: any) => {
                    await new class extends JavaScriptVisitor<any> {
                        override async visitNewClass(nc: J.NewClass, p: any): Promise<J | undefined> {
                            const d = nc.constructorType?.declaringType;
                            named.push(d && Type.isFullyQualified(d) ? Type.FullyQualified.getFullyQualifiedName(d as any) : undefined);
                            return super.visitNewClass(nc, p);
                        }
                    }().visit(cu, undefined);
                }
            } as any, packageJson(`{"name":"t","dependencies":{"@types/node":"^20.0.0"}}`)));
        }, {unsafeCleanup: true});
        expect(named).toEqual(["stream"]);
    }, 30000);

    test("a constructed reference's attribution moves with the binding", async () => {
        const spec = new RecipeSpec();
        const declaring: (string | undefined)[] = [];
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {
                    from: {module: "events", member: "EventEmitter"},
                    to: {module: "node:stream", member: "Stream"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await withDir(async (repo) => {
            await spec.rewriteRun(npm(repo.path, {
                //language=typescript
                ...typescript(
                    `
                    import { EventEmitter } from 'events';

                    const e = new EventEmitter();
                    `,
                    `
                    import { Stream } from 'node:stream';

                    const e = new Stream();
                    `),
                afterRecipe: async (cu: any) => {
                    await new class extends JavaScriptVisitor<any> {
                        override async visitNewClass(nc: J.NewClass, p: any): Promise<J | undefined> {
                            const d = nc.constructorType?.declaringType;
                            declaring.push(d && Type.isFullyQualified(d) ? Type.FullyQualified.getFullyQualifiedName(d as any) : undefined);
                            return super.visitNewClass(nc, p);
                        }
                    }().visit(cu, undefined);
                }
            } as any, packageJson(`{"name":"t","dependencies":{"lodash":"^4.17.21","lodash-es":"^4.17.21","@types/lodash":"^4.14.202","@types/lodash-es":"^4.17.12","@types/node":"^20.0.0"}}`)));
        }, {unsafeCleanup: true});
        expect(declaring).toEqual(["stream"]);
    });

    test("an optionally called reference's attribution moves with the binding", async () => {
        const spec = new RecipeSpec();
        const attribution: string[] = [];
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {
                    from: {module: "lodash", member: "extend"},
                    to: {module: "lodash-es", member: "assign"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await withDir(async (repo) => {
            await spec.rewriteRun(npm(repo.path, {
                //language=typescript
                ...typescript(
                    `
                    import { extend } from 'lodash';

                    extend?.({}, {});
                    `,
                    `
                    import { assign } from 'lodash-es';

                    assign?.({}, {});
                    `),
                afterRecipe: async (cu: any) => {
                    await new class extends JavaScriptVisitor<any> {
                        override async visitFunctionCall(fc: JS.FunctionCall, p: any): Promise<J | undefined> {
                            const d = fc.methodType?.declaringType;
                            attribution.push(`${d && Type.isFullyQualified(d) ? Type.FullyQualified.getFullyQualifiedName(d as any) : undefined}.${fc.methodType?.name}`);
                            return super.visitFunctionCall(fc, p);
                        }
                    }().visit(cu, undefined);
                }
            } as any, packageJson(`{"name":"t","dependencies":{"lodash":"^4.17.21","lodash-es":"^4.17.21","@types/lodash":"^4.14.202","@types/lodash-es":"^4.17.12"}}`)));
        }, {unsafeCleanup: true});
        // An optional call is a `JS.FunctionCall`, which carries its own method type.
        expect(attribution).toEqual(["lodash-es.assign"]);
    });

    test("a self-referential type on a moved reference terminates", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {
                    from: {module: "./util", member: "helper"},
                    to: {module: "./util2", member: "helper2"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        // A function imported from a relative module is attributed to a class holding a method
        // whose declaring type is that same class, and a reference carries it whole.
        await withDir(async (repo) => {
            await spec.rewriteRun(npm(repo.path,
                {...typescript(`export function helper() { return 1; }\n`), path: "util.ts"} as any,
                {...typescript(`export function helper2() { return 1; }\n`), path: "util2.ts"} as any,
                {...typescript(
                    //language=typescript
                    `
                    import { helper } from './util';

                    const x = helper;
                    `,
                    //language=typescript
                    `
                    import { helper2 } from './util2';

                    const x = helper2;
                    `), path: "main.ts"} as any,
                packageJson(`{"name":"t"}`)));
        }, {unsafeCleanup: true});
    }, 30000);

    test("every type naming a moved member names it as a fresh parse of the result would", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                for (const [from, to] of [["Http", "HttpClient"], ["Headers", "HttpHeaders"], ["HttpModule", "HttpClientModule"]]) {
                    maybeRebind(this, {from: {module: "legacy-http", member: from}, to: {module: "@acme/common/http", member: to}});
                }
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        const body = (http: string, headers: string, module: string) =>
            `class Service {\n` +
            `    constructor(private http: ${http}) {}\n` +
            `    send(extra: ${headers}[]) {\n` +
            `        const headers = new ${headers}();\n` +
            `        headers.append('a', 'b');\n` +
            `        request('x');\n` +
            `        return this.http.post('u', headers);\n` +
            `    }\n` +
            `}\n` +
            `const modules = [${module}];\n` +
            `export {${http === "Http" ? http : `${http} as Http`}};\n`;
        const after = `import {request} from 'legacy-http';\n` +
            `import {HttpHeaders, HttpClient, HttpClientModule} from '@acme/common/http';\n\n` +
            body("HttpClient", "HttpHeaders", "HttpClientModule");
        let rebound: string[] = [];
        let parsed: string[] = [];
        await withDir(async (repo) => {
            installHttpPackages(repo.path);
            await spec.rewriteRun(npm(repo.path, {
                ...typescript(`import {Headers, Http, HttpModule, request} from 'legacy-http';\n\n` +
                    body("Http", "Headers", "HttpModule"), after),
                afterRecipe: async (cu: JS.CompilationUnit) => {
                    rebound = await attributionOf(cu);
                }
            } as any, packageJson(`{"name":"t"}`)));
            await new RecipeSpec().rewriteRun(npm(repo.path, {
                ...typescript(after),
                afterRecipe: async (cu: JS.CompilationUnit) => {
                    parsed = await attributionOf(cu);
                }
            } as any, packageJson(`{"name":"t"}`)));
        }, {unsafeCleanup: true});
        expect(rebound).toEqual(parsed);
        // `request` was imported beside the moved members and stayed.
        expect(rebound).toEqual(expect.arrayContaining([
            "post() @acme/common.HttpClient{name=post,return=String,parameters=[String, @acme/common.HttpHeaders]}",
            "new @acme/common/http{name=HttpHeaders,return=@acme/common.HttpHeaders,parameters=[]}",
            "request() legacy-http{name=request,return=String,parameters=[String]}"
        ]));
    }, 60000);

    test("a whole-module move leaves the module's attribution alone while another binding of it remains", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "legacy-http", member: "*"}, to: {module: "@acme/common/http", member: "*"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        let attribution: string[] = [];
        await withDir(async (repo) => {
            installHttpPackages(repo.path);
            await spec.rewriteRun(npm(repo.path, {
                //language=typescript
                ...typescript(
                    `
                    import * as legacy from 'legacy-http';
                    import {request} from 'legacy-http';

                    request('x');
                    `,
                    `
                    import * as legacy from '@acme/common/http';
                    import {request} from 'legacy-http';

                    request('x');
                    `),
                afterRecipe: async (cu: JS.CompilationUnit) => {
                    attribution = await attributionOf(cu);
                }
            } as any, packageJson(`{"name":"t"}`)));
        }, {unsafeCleanup: true});
        // The namespace and `request` share one module object, and `request` did not move.
        expect(attribution).toContain("request() legacy-http{name=request,return=String,parameters=[String]}");
    }, 60000);

    test("a class another subpath of the package also exports keeps its name, since the two cannot be told apart", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "legacy-http", member: "Http"}, to: {module: "@acme/common/http", member: "HttpClient"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        let attribution: string[] = [];
        await withDir(async (repo) => {
            installHttpPackages(repo.path);
            await spec.rewriteRun(npm(repo.path, {
                //language=typescript
                ...typescript(
                    `
                    import {Http} from 'legacy-http';
                    import {Http as Compat} from 'legacy-http/compat';

                    let a: Http;
                    let b: Compat;
                    `,
                    `
                    import {HttpClient} from '@acme/common/http';
                    import {Http as Compat} from 'legacy-http/compat';

                    let a: HttpClient;
                    let b: Compat;
                    `),
                afterRecipe: async (cu: JS.CompilationUnit) => {
                    attribution = await attributionOf(cu);
                }
            } as any, packageJson(`{"name":"t"}`)));
        }, {unsafeCleanup: true});
        expect(attribution).toContain("Compat legacy-http.Http <null>");
    }, 60000);

    test("a class moved onto a default binding keeps its own name under the target's package", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "legacy-http", member: "Http"}, to: {module: "@acme/common/http", member: "default"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        let attribution: string[] = [];
        await withDir(async (repo) => {
            installHttpPackages(repo.path);
            await spec.rewriteRun(npm(repo.path, {
                //language=typescript
                ...typescript(
                    `
                    import {Http} from 'legacy-http';

                    let h: Http;
                    `,
                    `
                    import Http from '@acme/common/http';

                    let h: Http;
                    `),
                afterRecipe: async (cu: JS.CompilationUnit) => {
                    attribution = await attributionOf(cu);
                }
            } as any, packageJson(`{"name":"t"}`)));
        }, {unsafeCleanup: true});
        // A fresh parse names a default-exported class after the name it declares, never after the module.
        expect(attribution).toContain("Http @acme/common.Http <null>");
    }, 60000);

    test("an AMD dependency swap keeps the parameter and its index", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (m.name.simpleName !== "target") {
                    return super.visitMethodInvocation(m, p);
                }
                bound.name ??= maybeRebind(this, {from: {module: "a/Old"}, to: {module: "a/New"}});
                return m;
            }
        });
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B", "a/Old", "a/C"], function (B, Old, C) { target(); });`,
            `sap.ui.define(["a/B", "a/New", "a/C"], function (B, Old, C) { target(); });`
        ));
        expect(bound.name).toBe("Old");
    });

    test("an ESM module named with surrogate escapes is replaced whole, escapes included", async () => {
        // given
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "\ud83d\ude80/Old"}, to: {module: "a/New"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });

        // when / then
        //language=typescript
        await spec.rewriteRun(typescript(
            `import Old from "\\ud83d\\ude80/Old";\n\nOld.f();`,
            `
            import Old from "a/New";

            Old.f();`
        ));
    });

    test("an AMD dependency named with surrogate escapes is replaced whole, escapes included", async () => {
        // given
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (m.name.simpleName !== "target") {
                    return super.visitMethodInvocation(m, p);
                }
                maybeRebind(this, {from: {module: "\ud83d\ude80/Old"}, to: {module: "a/New"}});
                return m;
            }
        });

        // when / then
        await spec.rewriteRun(javascript(
            `sap.ui.define(["\\ud83d\\ude80/Old"], function (Old) { target(); });`,
            `sap.ui.define(["a/New"], function (Old) { target(); });`
        ));
    });

    test("an AMD dependency written as a template literal names its module without the backticks", async () => {
        // given
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (m.name.simpleName !== "target") {
                    return super.visitMethodInvocation(m, p);
                }
                maybeRebind(this, {from: {module: "a/Old"}, to: {module: "a/New"}});
                return m;
            }
        });

        // when / then
        await spec.rewriteRun(javascript(
            "sap.ui.define([`a/Old`], function (Old) { target(); });",
            `sap.ui.define(["a/New"], function (Old) { target(); });`
        ));
    });

    test("a rebind of a module nothing binds returns undefined and changes nothing", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name = maybeRebind(this, {from: {module: "nope"}, to: {module: "also-nope"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await spec.rewriteRun(typescript(`const x = 1;`));
        expect(bound.name).toBeUndefined();
    });

    test("a member rebind refuses on AMD, since a factory parameter cannot bind one", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (m.name.simpleName !== "target") {
                    return super.visitMethodInvocation(m, p);
                }
                bound.name = maybeRebind(this, {from: {module: "a/Old", member: "x"}, to: {module: "a/New"}});
                return m;
            }
        });
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/Old"], function (Old) { target(); });`
        ));
        expect(bound.name).toBeUndefined();
    });

    test("moving the default out of a mixed default+named import keeps the named siblings", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "react"}, to: {module: "preact"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import React, {useState, useMemo} from "react";

            React.foo();
            useState();
            useMemo();`,
            `
            import {useState, useMemo} from "react";
            import React from "preact";

            React.foo();
            useState();
            useMemo();`
        ));
    });

    test("moving the namespace out of a mixed default+namespace import keeps the default", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "m", member: "*"}, to: {module: "m2", member: "*"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import D, * as N from "m";

            D();
            N.foo();`,
            `
            import D from "m";
            import * as N from "m2";

            D();
            N.foo();`
        ));
    });

    test("members moved one call at a time out of one statement into one module land in one statement, under its header", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "m", member: "a"}, to: {module: "m2", member: "x"}});
                maybeRebind(this, {from: {module: "m", member: "b"}, to: {module: "m2", member: "y"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            // header
            import {a, b} from "m";

            a(b);`,
            `
            // header
            import {x, y} from "m2";

            x(y);`
        ));
    });

    test("an import nested in a module declaration is a binding of its own scope, left as it stands", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {Old} from "m";
            declare module "x" {
                import {Old} from "m";
            }

            Old();`,
            `
            import {New} from "m2";
            declare module "x" {
                import {Old} from "m";
            }

            New();`
        ));
    });

    test("the created replacement stays type-only, and the surviving specifier keeps its own formatting", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "m", member: "a"}, to: {module: "m2", member: "a"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import type { a, b} from "m";

            let x: a;
            let y: b;`,
            `
            import type { b} from "m";
            import type { a } from "m2";

            let x: a;
            let y: b;`
        ));
    });

    test("a rebind that changes default/named/namespace shape replaces the statement, except in a CommonJS file", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name ??= maybeRebind(this, {from: {module: "old", member: "act"}, to: {module: "new", member: "default"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            // header
            import {act} from "old";

            act();`,
            `
            // header
            import act from "new";

            act();`
        ));
        expect(bound.name).toBe("act");

        bound.name = undefined;
        //language=typescript
        await spec.rewriteRun({...typescript(`
            import {act} from "old";

            act();`), path: "a.cts"});
        expect(bound.name).toBeUndefined();

        // The dropped import was the file's only ES module syntax, which leaves it reading as CommonJS.
        await spec.rewriteRun({...javascript(
            //language=javascript
            `
            import {act} from "old";
            const fs = require("fs");

            act();`,
            //language=javascript
            `
            import act from "new";
            const fs = require("fs");

            act();`
        ), path: "a.js"});

        // `b` gains its only-member status from the sibling move, after both rebinds were queued.
        const sibling = new RecipeSpec();
        sibling.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "m", member: "a"}, to: {module: "m2", member: "x"}});
                maybeRebind(this, {from: {module: "m", member: "b"}, to: {module: "m3"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await sibling.rewriteRun(typescript(
            `
            import {a, b} from "m";

            a(b);`,
            `
            import {x} from "m2";
            import b from "m3";

            x(b);`
        ));
    });

    test("a moved specifier's own inline type marker survives even where the clause it left is not type-only", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "m", member: "a"}, to: {module: "m2", member: "a"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {type a, b} from "m";

            let x: a;
            let y: b;`,
            `
            import {b} from "m";
            import type {a} from "m2";

            let x: a;
            let y: b;`
        ));
    });

    function rebindMember(member: string) {
        return new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "m", member}, to: {module: "n", member}});
                return super.visitJsCompilationUnit(cu, p);
            }
        };
    }

    test("moving the last specifier out of a spaced list keeps the space before its brace", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindMember("c"));
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { a, b, c } from "m";

            const x = [a, b, c];`,
            `
            import { a, b } from "m";
            import { c } from "n";

            const x = [a, b, c];`
        ));
    });

    test("moving a middle specifier out of a spaced list", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindMember("b"));
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import { a, b, c } from "m";

            const x = [a, b, c];`,
            `
            import { a, c } from "m";
            import { b } from "n";

            const x = [a, b, c];`
        ));
    });

    test("moving the last specifier out of an unspaced list", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindMember("c"));
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {a, b, c} from "m";

            const x = [a, b, c];`,
            `
            import {a, b} from "m";
            import {c} from "n";

            const x = [a, b, c];`
        ));
    });

    test("moving the last specifier out of a one-per-line list keeps its trailing comma and line break", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindMember("b"));
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {
                a,
                b,
            } from "m";

            const x = [a, b];`,
            `
            import {
                a,
            } from "m";
            import {b} from "n";

            const x = [a, b];`
        ));
    });
});

describe("maybeRebind keeps the file's import layout", () => {
    /** One `maybeRebind` per move, in the order given, all in one visit. */
    function rebinding(...moves: [from: string, to: string, member: string, toMember?: string][]) {
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                for (const [from, to, member, toMember] of moves) {
                    maybeRebind(this, {from: {module: from, member}, to: {module: to, member: toMember ?? member}});
                }
                return super.visitJsCompilationUnit(cu, p);
            }
        });
    }

    test("members split across modules land where their import was, in the order written, one line per lone member", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebinding(
            ["@acme/ui", "@acme/ui/dialog", "DialogModule"],
            ["@acme/ui", "@acme/ui/button", "ButtonModule"],
            ["@acme/ui", "@acme/ui/dialog", "Dialog"]
        );
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {Module} from '@acme/core';
            import {
                DialogModule,
                ButtonModule,
                Dialog,
            } from '@acme/ui';
            import {Other} from 'other';

            const used = [Module, DialogModule, ButtonModule, Dialog, Other];`,
            `
            import {Module} from '@acme/core';
            import {ButtonModule} from '@acme/ui/button';
            import {
                DialogModule,
                Dialog,
            } from '@acme/ui/dialog';
            import {Other} from 'other';

            const used = [Module, DialogModule, ButtonModule, Dialog, Other];`
        ));
    });

    test("the member left last in a multi-line import collapses onto one line, sorted among the imports split from it", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebinding(
            ["@acme/ui", "@acme/ui/card", "CardModule"],
            ["@acme/ui", "@acme/ui/tooltip", "Tooltip"],
            ["@acme/ui", "@acme/ui/core", "DATE_FORMATS"]
        );
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {
                Tooltip as Tip,
                DATE_FORMATS,
                CardModule
            } from "@acme/ui";
            import {Other} from "other";

            const used = [Tip, DATE_FORMATS, CardModule, Other];`,
            `
            import {CardModule} from "@acme/ui/card";
            import {DATE_FORMATS} from "@acme/ui/core";
            import {Tooltip as Tip} from "@acme/ui/tooltip";
            import {Other} from "other";

            const used = [Tip, DATE_FORMATS, CardModule, Other];`
        ));
    });

    test("an import that replaces a dropped one takes its place", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebinding(["old", "new", "act", "default"]);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {a} from "first";
            import {act} from "old";
            import {z} from "last";

            act(a, z);`,
            `
            import {a} from "first";
            import act from "new";
            import {z} from "last";

            act(a, z);`
        ));
    });

    test("imports split from a dropped statement keep the comment before it, sorted below it", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebinding(["m", "z", "A"], ["m", "a", "B", "default"]);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {x} from "first";
            // ui
            import {A, B} from "m";

            use(x, A, B);`,
            `
            import {x} from "first";
            // ui
            import B from "a";
            import {A} from "z";

            use(x, A, B);`
        ));
    });

    test("a named member moved beside a default of the same module goes on one line", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebinding(["m", "n", "A"], ["m", "n", "B", "default"]);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {
                A,
                B,
            } from "m";

            use(A, B);`,
            `
            import {A} from "n";
            import B from "n";

            use(A, B);`
        ));
    });

    test("an import that replaces the file's last statement takes its place and its comment", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebinding(["m", "n", "A", "default"]);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {x} from "first";
            // ui
            import {A} from "m";`,
            `
            import {x} from "first";
            // ui
            import A from "n";`
        ));
    });

    test("a type-only member and a value moved to one module each go on one line", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebinding(["m", "n", "T"], ["m", "n", "V"]);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {
                type T,
                V,
            } from "m";

            let t: T = V;`,
            `
            import {V} from "n";
            import type {T} from "n";

            let t: T = V;`
        ));
    });

    test("members renamed within their module keep the order written, whichever rename runs first", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebinding(["lodash", "lodash", "zeta", "zed"], ["lodash", "lodash", "extend", "assign"]);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {zeta, extend, alpha} from "lodash";

            zeta(extend, alpha);`,
            `
            import {zed, assign, alpha} from "lodash";

            zed(assign, alpha);`
        ));
    });

    test("an import split from a multi-line list spaces its braces as the file's export lists do", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebinding(["m", "n", "b"]);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {
                a,
                b,
            } from "m";

            export { a, b };`,
            `
            import {
                a,
            } from "m";
            import { b } from "n";

            export { a, b };`
        ));
    });
});

// Why test.fails?
// scope.ts counts a block's function and a `declare global` variable as module-scope names,
// so we add an unnecessary alias. The code still compiles; these tests are here to make it better.
// See https://github.com/openrewrite/rewrite/pull/9632
describe("maybeRebind adds no alias the file doesn't need", () => {
    // Today: import type { New as Old } from "m2";
    test.fails("a type-only import beside a block's function of the same name moves without an alias", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import type { Old } from "m";
                if (c) { function Old() {} }
                let x: Old;
            `,
            `
                import type { New } from "m2";
                if (c) { function Old() {} }
                let x: New;
            `
        ));
    });

    // Today: import type { New as Old } from "m2";
    test.fails("a type-only import beside a declare global variable of the same name moves without an alias", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import type { Old } from "m";
                declare global { var Old: number }
                let x: Old;
            `,
            `
                import type { New } from "m2";
                declare global { var Old: number }
                let x: New;
            `
        ));
    });

    // Today: import { New as Old } from "m2";
    test.fails("a value import beside a block's function of the same name moves without an alias", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import { Old } from "m";
                if (c) { function Old() {} }
                Old();
            `,
            `
                import { New } from "m2";
                if (c) { function Old() {} }
                New();
            `
        ));
    });
});

function dropModule(module: string, member?: string) {
    return new class extends JavaScriptVisitor<any> {
        override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
            maybeUnbind(this, {module, member});
            return super.visitJsCompilationUnit(cu, p);
        }
    };
}

describe("maybeRebind and the member's other spellings", () => {
    test("a member read through a namespace import is renamed, as a type and as a value", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = rebindTo({module: "m", member: "New"}, bound);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import * as NS from "m";

                let a: NS.Old<{}>;
                let b: typeof NS.Old;
                NS.Old();
                new NS.Old();
            `,
            `
                import * as NS from "m";

                let a: NS.New<{}>;
                let b: typeof NS.New;
                NS.New();
                new NS.New();
            `
        ));
        expect(bound.name).toBeUndefined();
    });

    test("the named binding and the namespace spelling move together", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = rebindTo({module: "m", member: "New"}, bound);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import { Old } from "m";
                import * as NS from "m";

                let a: Old;
                let b: NS.Old;
            `,
            `
                import { New } from "m";
                import * as NS from "m";

                let a: New;
                let b: NS.New;
            `
        ));
        expect(bound.name).toBe("New");
    });

    test("a member read through a required module object is renamed", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m", member: "New"}, {});
        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    import NS = require("m");

                    let a: NS.Old;
                `,
                `
                    import NS = require("m");

                    let a: NS.New;
                `
            ),
            //language=javascript
            javascript(
                `
                    const NS = require("m");

                    NS.Old();
                `,
                `
                    const NS = require("m");

                    NS.New();
                `
            )
        );
    });

    test("a member read through a module object is renamed as a JSX tag, and through an awaited import", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m", member: "New"}, {});
        //language=tsx
        await spec.rewriteRun(tsx(
            `
                import * as NS from "m";
                const Lazy = await import("m");

                const a = <NS.Old x={1} />;
                const b = <Lazy.Old>hi</Lazy.Old>;
            `,
            `
                import * as NS from "m";
                const Lazy = await import("m");

                const a = <NS.New x={1} />;
                const b = <Lazy.New>hi</Lazy.New>;
            `
        ));
    });

    test("a reassignable module object is left alone", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m", member: "New"}, {});
        await spec.rewriteRun(
            //language=javascript
            javascript(`
                let NS = require("m");
                NS = {Old: () => 1};
                NS.Old();
            `),
            //language=javascript
            javascript(`
                var NS = require("m");
                NS = {Old: () => 1};
                NS.Old();
            `),
            //language=javascript
            javascript(`
                let NS = await import("m");
                NS = {Old: () => 1};
                NS.Old();
            `),
            //language=javascript
            javascript(`
                let NS = require("m");
                function reset() { NS = {Old: () => 1}; }
                NS.Old();
            `)
        );
    });

    test("a let or var module object the file never assigns to reads the member too", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m", member: "New"}, {});
        await spec.rewriteRun(
            //language=javascript
            javascript(
                `
                    let NS = require("m");
                    NS.Old();
                `,
                `
                    let NS = require("m");
                    NS.New();
                `
            ),
            //language=javascript
            javascript(
                `
                    var NS = require("m");
                    NS.Old();
                `,
                `
                    var NS = require("m");
                    NS.New();
                `
            ),
            //language=javascript
            javascript(
                `
                    let NS = await import("m");
                    NS.Old();
                `,
                `
                    let NS = await import("m");
                    NS.New();
                `
            )
        );
    });

    test("the types naming a member moved only through a module object name it as a fresh parse of the result would", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                for (const [from, to] of [["Http", "HttpClient"], ["request", "send"]]) {
                    maybeRebind(this, {from: {module: "http-api", member: from}, to: {module: "http-api", member: to}});
                }
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        const after = `
            import * as api from 'http-api';

            const client: api.HttpClient = new api.HttpClient();
            client.get('u');
            api.send('x');
        `;
        let rebound: string[] = [];
        let parsed: string[] = [];
        await withDir(async (repo) => {
            installHttpApi(repo.path);
            await spec.rewriteRun(npm(repo.path, {
                ...typescript(`
                    import * as api from 'http-api';

                    const client: api.Http = new api.Http();
                    client.get('u');
                    api.request('x');
                `, after),
                afterRecipe: async (cu: JS.CompilationUnit) => {
                    rebound = await attributionOf(cu);
                }
            } as any, packageJson(`{"name":"t"}`)));
            await new RecipeSpec().rewriteRun(npm(repo.path, {
                ...typescript(after),
                afterRecipe: async (cu: JS.CompilationUnit) => {
                    parsed = await attributionOf(cu);
                }
            } as any, packageJson(`{"name":"t"}`)));
        }, {unsafeCleanup: true});
        expect(rebound).toEqual(parsed);
    }, 60000);

    test("an import() type and a re-export moved within their module are typed as a fresh parse of the result types them", async () => {
        const {rebound, parsed} = await attributionsAfter(
            {from: {module: "http-api", member: "Http"}, to: {module: "http-api", member: "HttpClient"}},
            installHttpApi,
            `
                declare const client: import('http-api').Http;
                export {Http} from 'http-api';
                export {Http as Client} from 'http-api';
            `,
            `
                declare const client: import('http-api').HttpClient;
                export {HttpClient as Http} from 'http-api';
                export {HttpClient as Client} from 'http-api';
            `
        );
        expect(rebound).toEqual(parsed);
    }, 60000);

    test("an import() type and a re-export moved to another module are typed as a fresh parse of the result types them", async () => {
        const {rebound, parsed} = await attributionsAfter(
            {from: {module: "legacy-http", member: "Http"}, to: {module: "@acme/common/http", member: "HttpClient"}},
            installHttpPackages,
            `
                declare const client: import('legacy-http').Http;
                export {Http} from 'legacy-http';
            `,
            `
                declare const client: import('@acme/common/http').HttpClient;
                export {HttpClient as Http} from '@acme/common/http';
            `
        );
        expect(rebound).toEqual(parsed);
    }, 60000);

    test("a member read through its module object by a string-literal index moves too", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m", member: "New"}, {});
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import * as NS from "m";
                declare const other: any;
                const key = "Old";
                NS["Old"]();
                const v = NS['Old'];
                NS[key]();
                other["Old"]();
            `,
            `
                import * as NS from "m";
                declare const other: any;
                const key = "Old";
                NS["New"]();
                const v = NS['New'];
                NS[key]();
                other["Old"]();
            `
        ));
    });

    test("a parenthesized module object reads the member too", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m", member: "New"}, {});
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import * as NS from "m";
                (NS).Old();
                const v = ((NS)).Old;
            `,
            `
                import * as NS from "m";
                (NS).New();
                const v = ((NS)).New;
            `
        ));
    });

    test("a destructuring of the module object reads the member, keeping the name it binds", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m", member: "New"}, {});
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import * as NS from "m";
                declare const other: any;
                const {Old} = NS;
                const {Old: X, Other} = NS;
                const {Old: New} = NS;
                let {Old: Y = X} = (NS);
                const {Old: Z} = other;
                Old(); X(); New(); Y(); Z(); Other();
            `,
            `
                import * as NS from "m";
                declare const other: any;
                const {New: Old} = NS;
                const {New: X, Other} = NS;
                const {New} = NS;
                let {New: Y = X} = (NS);
                const {Old: Z} = other;
                Old(); X(); New(); Y(); Z(); Other();
            `
        ));
    });

    test("a module object stays constant in a declaration list", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m", member: "New"}, {});
        //language=javascript
        await spec.rewriteRun(javascript(
            `
            const NS = require("m"), Other = require("other");
            NS.Old();
            Other.Old();`,
            `
            const NS = require("m"), Other = require("other");
            NS.New();
            Other.Old();`
        ));
    });

    test("a default import, another module's namespace and a shadowed namespace are left alone", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m", member: "New"}, {});
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import D from "m";
                import * as Other from "other";
                import * as NS from "m";

                let a: D.Old;
                let b: Other.Old;
                function f(NS: any) { return NS.Old; }
            `
        ));
    });

    test("a move to another module leaves a namespace spelling alone, since the namespace still names the old module", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m2", member: "New"}, {});
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import * as NS from "m";

                let a: NS.Old;
            `
        ));
    });

    test("an import() type follows the move, to another module too", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m", member: "New"}, {});
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                let a: import("m").Old;
                let b: import("m").Old<{}>;
                let c: typeof import("m").Old;
                let d: import("m").Old.Inner;
                let e: typeof import("m").Old.factory;
                let other: import("m").Other.Old;
            `,
            `
                let a: import("m").New;
                let b: import("m").New<{}>;
                let c: typeof import("m").New;
                let d: import("m").New.Inner;
                let e: typeof import("m").New.factory;
                let other: import("m").Other.Old;
            `
        ));

        const toOther = new RecipeSpec();
        toOther.recipe = rebindTo({module: "m2", member: "New"}, {});
        //language=typescript
        await toOther.rewriteRun(typescript(
            `
                let a: import('m').Old;
                let b: import('m').Old.Inner;
            `,
            `
                let a: import('m2').New;
                let b: import('m2').New.Inner;
            `
        ));
    });

    test("a re-export straight from the module keeps the name it publishes", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m", member: "New"}, {});
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                export { Old } from "m";
                export { Old as Renamed } from "m";
                export type { Old as T } from "m";
            `,
            `
                export { New as Old } from "m";
                export { New as Renamed } from "m";
                export type { New as T } from "m";
            `
        ));
    });

    test("a re-export spelling the member as a string literal reads it by name", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m", member: "New"}, {});
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                export { "Old" } from "m";
                export { "Old" as Renamed } from "m";
            `,
            `
                export { New as "Old" } from "m";
                export { New as Renamed } from "m";
            `
        ));
    });

    test("a re-export moving to another module leaves what else its statement re-exports, and joins a re-export of the target", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m2", member: "New"}, {});
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                export { Old } from "m";
                export { Old as O, Other } from "m";
            `,
            `
                export { New as Old, New as O } from "m2";
                export { Other } from "m";
            `
        ));
    });

    test("a re-export with no re-export of the target to join takes a statement of its own after the one it left", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m2", member: "New"}, {});
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                export {
                    Other,
                    Old,
                } from "m";
                const x = 1;
            `,
            `
                export {
                    Other,
                } from "m";
                export {
                    New as Old,
                } from "m2";
                const x = 1;
            `
        ));
    });

    test("re-exports whose every member moves end up as one statement of the target", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindEach([
            {from: {module: "react", member: "Validator"}, to: {module: "prop-types", member: "Validator"}},
            {from: {module: "react", member: "ValidationMap"}, to: {module: "prop-types", member: "ValidationMap"}}
        ]);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                export { Validator, ValidationMap } from "react";
            `,
            `
                export { Validator, ValidationMap } from "prop-types";
            `
        ));
    });

    test("moved members leave the rest behind and join the target's re-export of the same kind", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindEach([
            {from: {module: "react", member: "Validator"}, to: {module: "prop-types", member: "Validator"}},
            {from: {module: "react", member: "ValidationMap"}, to: {module: "prop-types", member: "ValidationMap"}}
        ]);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                export type { Validator as V, ReactNode, ValidationMap } from "react";
                export { checkPropTypes } from "prop-types";
                export type { Requireable } from "prop-types";
            `,
            `
                export type { ReactNode } from "react";
                export { checkPropTypes } from "prop-types";
                export type { Requireable, Validator as V, ValidationMap } from "prop-types";
            `
        ));
    });

    test("a re-export moving to another module under the member's own name keeps its specifier as written", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m2", member: "Old"}, {});
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                export { Old, Old as Renamed, "Old" as Quoted } from "m";
            `,
            `
                export { Old, Old as Renamed, "Old" as Quoted } from "m2";
            `
        ));
    });

    test("a re-export a moved member empties goes, and the statement after it keeps its comment", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m2", member: "New"}, {});
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                export { Other } from "m2";
                export { Old } from "m";
                // Important docs
                export const x = 1;
            `,
            `
                export { Other, New as Old } from "m2";
                // Important docs
                export const x = 1;
            `
        ));
    });

    test("a re-export split between two modules keeps each specifier's comment with it", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m2", member: "New"}, {});
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                export { /* keep */ Other, /* move */ Old } from "m";
            `,
            `
                export { /* keep */ Other } from "m";
                export { /* move */ New as Old } from "m2";
            `
        ));
    });

    test("a comment trailing the re-export a moved member leaves stays on its line, also at the end of the file", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m2", member: "New"}, {});
        await spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    export { A, Old } from "m"; // note
                    export const x = 1;
                `,
                `
                    export { A } from "m"; // note
                    export { New as Old } from "m2";
                    export const x = 1;
                `
            ),
            //language=typescript
            typescript(
                `
                    export { A, Old } from "m"; // note
                `,
                `
                    export { A } from "m"; // note
                    export { New as Old } from "m2";
                `
            )
        );
    });

    test("a re-export joining an empty re-export of the target takes the place of its empty entry", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m2", member: "New"}, {});
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                export {Old} from "m";
                export {} from "m2";
            `,
            `
                export {New as Old} from "m2";
            `
        ));
    });

    test("a re-export joining the target's keeps its own comment, not another specifier's", async () => {
        const spec = new RecipeSpec();
        spec.recipe = rebindTo({module: "m2", member: "New"}, {});
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                export { A, /* b */ B } from "m2";
                export { /* move */ Old, Other } from "m";
            `,
            `
                export { A, /* b */ B, /* move */ New as Old } from "m2";
                export { Other } from "m";
            `
        ));
    });

    test("a refused require leaves all member spellings unchanged", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = rebindTo({module: "m", member: "New"}, bound);
        await spec.rewriteRun(
            //language=typescript
            typescript(`
                const {Old, ...rest} = require("m");
                const NS = require("m");
                Old();
                NS.Old();
                type T = import("m").Old;
                export {Old} from "m";
            `),
            //language=typescript
            typescript(`
                const {Old}: any = require("m");
                const NS = require("m");
                Old();
                NS.Old();
                type T = import("m").Old;
                export {Old} from "m";
            `)
        );
        expect(bound.name).toBeUndefined();
    });

    test("a move refused for the named binding renames no other spelling either", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = rebindTo({module: "m", member: "New", alias: "Taken"}, bound);
        //language=typescript
        await spec.rewriteRun(typescript(
            `
                import { Old } from "m";
                import * as NS from "m";

                const Taken = 1;
                let a: Old;
                let b: NS.Old;
            `
        ));
        expect(bound.name).toBeUndefined();
    });
});

describe("maybeUnbind on an AMD block", () => {
    test("an unreferenced dependency goes with its parameter", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(dropModule("sap/m/Button"));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["sap/m/Button", "a/C"], function (Button, C) { C.f(); });`,
            `sap.ui.define(["a/C"], function (C) { C.f(); });`
        ));
    });

    test("a dependency the body still names stays", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(dropModule("sap/m/Button"));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["sap/m/Button"], function (Button) { Button.f(); });`
        ));
    });

    test("a dependency bound to no parameter is left alone", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(dropModule("a/Side"));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/Side"], function () {});`
        ));
    });

    test("a member-scoped request does not apply, since a dependency binds a whole module", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(dropModule("sap/m/Button", "default"));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["sap/m/Button"], function (Button) {});`
        ));
    });

    test("a custom amdCallee reaches the removal lane", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeUnbind(this, {module: "sap/m/Button", amdCallee: "myDefine"});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await spec.rewriteRun(javascript(
            `myDefine(["sap/m/Button", "a/C"], function (Button, C) { C.f(); });`,
            `myDefine(["a/C"], function (C) { C.f(); });`
        ));
    });

    test("two calls differing only in amdCallee each queue their own removal", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeUnbind(this, {module: "a/B", amdCallee: "define"});
                maybeUnbind(this, {module: "a/B", amdCallee: "myDefine"});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=javascript
        await spec.rewriteRun(javascript(
            `
            define(["a/B", "a/C"], function (B, C) { C.f(); });
            myDefine(["a/B", "a/D"], function (B, D) { D.f(); });`,
            `
            define(["a/C"], function (C) { C.f(); });
            myDefine(["a/D"], function (D) { D.f(); });`
        ));
    });
});

describe("maybeUnbind on an ESM file", () => {
    test("removes the import; the AMD lane it also queues finds no block to act on", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(dropModule("sap/m/Button"));
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import Button from "sap/m/Button";

            export const x = 1;`,
            `export const x = 1;`
        ));
    });

    test("a bare string is shorthand for {module}", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeUnbind(this, "sap/m/Button");
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import Button from "sap/m/Button";

            export const x = 1;`,
            `export const x = 1;`
        ));
    });

    test("the deprecated maybeRemoveImport still works, doing what the equivalent maybeUnbind call would", async () => {
        const removeWith = (remove: (visitor: JavaScriptVisitor<any>) => void) => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                    remove(this);
                    return super.visitJsCompilationUnit(cu, p);
                }
            });
            //language=typescript
            return spec.rewriteRun(typescript(
                `
                import Button from "sap/m/Button";

                export const x = 1;`,
                `export const x = 1;`
            ));
        };

        await removeWith(v => maybeRemoveImport(v, "sap/m/Button"));
        await removeWith(v => maybeUnbind(v, {module: "sap/m/Button"}));
    });

    test("a block with no dependency array keeps its loader parameters", async () => {
        // `define(factory)` takes `require`, `exports` and `module` from the loader by position,
        // so dropping one there shifts what the rest receive.
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(renameThenSweep({exports: "elsewhere"}));
        await spec.rewriteRun(javascript(
            `define(function (require, exports, module) { exports.x(); });`,
            `define(function (require, exports, module) { elsewhere.x(); });`));
    });

    test("two calls for one move are one rebind", async () => {
        // Each matched reference asks, but the block is rewritten once: a second queued visitor
        // would find the dependency already moved and fail the recipe.
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (m.name.simpleName !== "target") {
                    return super.visitMethodInvocation(m, p);
                }
                maybeRebind(this, {from: {module: "a/Old"}, to: {module: "a/New"}});
                return m;
            }
        });
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/Old"], function (Old) { target(); target(); });`,
            `sap.ui.define(["a/New"], function (Old) { target(); target(); });`));
    });

    test("moving the only named member out leaves no empty braces", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "m", member: "a"}, to: {module: "m2", member: "a"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import D, {a} from "m";
            a();`,
            `
            import D from "m";
            import {a} from "m2";
            a();`));
    });
});

/** Renames a call's `select` identifier per `renames`, then sweeps whatever the rename orphaned. */
function renameThenSweep(renames: Record<string, string>) {
    return new class extends JavaScriptVisitor<ExecutionContext> {
        override async visitJsCompilationUnit(cu: JS.CompilationUnit, ctx: ExecutionContext): Promise<J | undefined> {
            const rewritten = await super.visitJsCompilationUnit(cu, ctx) as JS.CompilationUnit;
            return removeNewlyUnusedAmdBindings(cu, rewritten, ctx);
        }

        override async visitMethodInvocation(m: J.MethodInvocation, ctx: ExecutionContext): Promise<J | undefined> {
            const select = m.select?.element;
            const to = select?.kind === J.Kind.Identifier ? renames[(select as J.Identifier).simpleName] : undefined;
            if (to === undefined) {
                return super.visitMethodInvocation(m, ctx);
            }
            const renamedSelect: J.Identifier = {...(select as J.Identifier), simpleName: to};
            const renamed: J.MethodInvocation = {...m, select: {...m.select!, element: renamedSelect}};
            return renamed;
        }
    };
}

describe("removeNewlyUnusedAmdBindings", () => {
    test("a binding a rewrite stopped using goes", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(renameThenSweep({Old: "kept"}));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/Old", "a/Kept"], function (Old, kept) { Old.f(); });`,
            `sap.ui.define(["a/Kept"], function (kept) { kept.f(); });`
        ));
    });

    test("a binding that was already unused stays, being loaded for its side effects", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(renameThenSweep({Old: "kept"}));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/Side", "a/Kept"], function (side, kept) { kept.f(); });`
        ));
    });

    test("removing two non-adjacent bindings still pairs each survivor with its own dependency", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(renameThenSweep({B: "A", D: "C"}));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/A", "a/B", "a/C", "a/D"], function (A, B, C, D) { A.f(); B.f(); C.f(); D.f(); });`,
            `sap.ui.define(["a/A", "a/C"], function (A, C) { A.f(); A.f(); C.f(); C.f(); });`
        ));
    });
});

describe("maybeRebind JSX", () => {
    test("a JSX attribute key is a name, not a reference", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "m", member: "Old"}, to: {module: "m2", member: "New"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=tsx
        await spec.rewriteRun(tsx(
            `
            import { Old } from "m";

            const e = <div Old="x">{Old}</div>;`,
            `
            import { New } from "m2";

            const e = <div Old="x">{New}</div>;`
        ));
    });
});

describe("a node: specifier and the bare name of its built-in", () => {
    test("name one module to maybeUnbind, unless the built-in has no bare name", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeUnbind(this, {module: "util", member: "isArray"});
                maybeUnbind(this, {module: "node:util", member: "log"});
                maybeUnbind(this, {module: "test", member: "it"});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {isArray} from 'node:util';
            import {log} from 'util';
            import {it} from 'node:test';

            console.log(1);`,
            `
            import {it} from 'node:test';

            console.log(1);`
        ));
    });

    test("name one module to maybeBind, which keeps the spelling the file uses", async () => {
        const spec = new RecipeSpec();
        let bound: (string | undefined)[] | undefined;
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                const answers = [
                    maybeBind(this, {module: "util", member: "promisify"}),
                    maybeBind(this, {module: "util", member: "*"}),
                    moduleBindings(this).bindingOf("util"),
                    maybeBind(this, {module: "util", member: "inspect", onlyIfReferenced: false})
                ];
                bound ??= answers;
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import * as u from 'node:util';
            import {promisify as p} from 'node:util';

            p(u);`,
            `
            import * as u from 'node:util';
            import {inspect, promisify as p} from 'node:util';

            p(u);`
        ));
        expect(bound).toEqual(["p", "u", "u", "inspect"]);
    });

    test("name one module to maybeRebind's from", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "util", member: "isArray"}, to: {module: "lodash", member: "isArray"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {isArray} from 'node:util';

            isArray([]);`,
            `
            import {isArray} from 'lodash';

            isArray([]);`
        ));
    });

    test("a binding moved to another built-in keeps the node: scheme its import spelled", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "assert"}, to: {module: "assert/strict", member: "strict"}});
                maybeRebind(this, {from: {module: "net"}, to: {module: "tls"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import a from 'node:assert';
            import net from 'node:net';

            a(net);`,
            `
            import {strict as a} from 'node:assert/strict';
            import net from 'node:tls';

            a(net);`
        ));

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "assert", member: "strict"}, to: {module: "assert/strict", member: "strict"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        //language=typescript
        await spec.rewriteRun(typescript(
            `
            import {strict, ok} from 'node:assert';

            strict(ok);`,
            `
            import {ok} from 'node:assert';
            import {strict} from 'node:assert/strict';

            strict(ok);`
        ));
    });
});

describe("maybeRebind on a require", () => {
    function rebind(from: {module: string; member?: string}, to: {module: string; member?: string}, bound: (string | undefined)[] = []) {
        const spec = new RecipeSpec();
        const answered = new Set<string>();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                const name = maybeRebind(this, {from, to});
                if (!answered.has(cu.sourcePath)) {
                    answered.add(cu.sourcePath);
                    bound.push(name);
                }
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        return spec;
    }

    test("a whole module moves to a member, keeping the name the file chose and its node: spelling", async () => {
        const bound: (string | undefined)[] = [];
        const spec = rebind({module: "assert"}, {module: "assert/strict", member: "strict"}, bound);
        await spec.rewriteRun(
            //language=javascript
            javascript(`
                const strict = require('assert');

                strict(true);`,
                `
                const {strict} = require('assert/strict');

                strict(true);`),
            //language=javascript
            javascript(`
                const a = require('node:assert');

                a(true);`,
                `
                const {strict: a} = require('node:assert/strict');

                a(true);`)
        );
        expect(bound).toEqual(["strict", "a"]);
    });

    test("a whole module moves to a whole module, and a member to a member", async () => {
        await rebind({module: "_tls_wrap"}, {module: "tls"}).rewriteRun(
            //language=javascript
            javascript(`
                const tlsWrap = require('_tls_wrap');

                tlsWrap.connect();`,
                `
                const tlsWrap = require('tls');

                tlsWrap.connect();`));
        await rebind({module: "_tls_wrap", member: "TLSSocket"}, {module: "tls", member: "TLSSocket"}).rewriteRun(
            //language=javascript
            javascript(`
                const {TLSSocket} = require('_tls_wrap');

                new TLSSocket();`,
                `
                const {TLSSocket} = require('tls');

                new TLSSocket();`));
    });

    test("an unaliased member follows the new member's name, while an aliased one keeps its alias", async () => {
        const bound: (string | undefined)[] = [];
        const spec = rebind({module: "lodash", member: "extend"}, {module: "lodash", member: "assign"}, bound);
        await spec.rewriteRun(
            //language=javascript
            javascript(`
                const {extend} = require('lodash');

                extend({}, {});`,
                `
                const {assign} = require('lodash');

                assign({}, {});`),
            //language=javascript
            javascript(`
                const {extend: ext} = require('lodash');

                ext({}, {});`,
                `
                const {assign: ext} = require('lodash');

                ext({}, {});`),
            //language=javascript
            javascript(`
                const {extend, map} = require('lodash');

                extend(map);`,
                `
                const {assign, map} = require('lodash');

                assign(map);`)
        );
        expect(bound).toEqual(["assign", "ext", "assign"]);
    });

    test("a move merges into an existing require of the target", async () => {
        await rebind({module: "_stream_duplex"}, {module: "stream", member: "Duplex"}).rewriteRun(
            //language=javascript
            javascript(`
                const {Readable} = require('stream');
                const Duplex = require('_stream_duplex');

                new Duplex(Readable);`,
                `
                const {Readable, Duplex} = require('stream');

                new Duplex(Readable);`));
        // The target already binds the member under the name the move takes, so only the source goes.
        await rebind({module: "m", member: "a"}, {module: "n", member: "b"}).rewriteRun(
            //language=javascript
            javascript(`
                const {b} = require('n');
                const {a} = require('m');

                b(a);`,
                `
                const {b} = require('n');

                b(b);`));

        await rebind({module: "m", member: "a"}, {module: "n", member: "a"}).rewriteRun(
            //language=javascript
            javascript(`
                const {
                    c,
                } = require('n');
                const {a} = require('m');

                c(a);`,
                `
                const {
                    c,
                    a,
                } = require('n');

                c(a);`));
    });

    test("a moved binding joins no require of the target after the file's leading requires", async () => {
        await rebind({module: "m", member: "a"}, {module: "n", member: "a"}).rewriteRun(
            //language=javascript
            javascript(`
                const {a} = require('m');
                a();
                const {c} = require('n');`,
                `
                const {a} = require('n');
                a();
                const {c} = require('n');`),
            //language=javascript
            javascript(`
                const {a, b} = require('m');
                a(b);
                const {c} = require('n');`,
                `
                const {b} = require('m');
                const {a} = require('n');
                a(b);
                const {c} = require('n');`));
    });

    test("a move keeps its name where the target's binding of it cannot take the move in", async () => {
        const spec = rebind({module: "m", member: "a"}, {module: "n", member: "b"});
        await spec.rewriteRun(
            //language=typescript
            typescript(`
                import {a} from 'm';
                const {b} = require('n');

                b(a);`,
                `
                import {b as a} from 'n';
                const {b} = require('n');

                b(a);`),
            //language=typescript
            typescript(`
                import {b} from 'n';
                const {a} = require('m');

                b(a);`,
                `
                import {b} from 'n';
                const {b: a} = require('n');

                b(a);`),
            //language=javascript
            javascript(`
                const {a} = require('m');
                a();
                const {b} = require('n');
                b();`,
                `
                const {b: a} = require('n');
                a();
                const {b} = require('n');
                b();`),
            //language=javascript
            javascript(`
                const {b} = require('n');
                let {a} = require('m');

                a = b;`,
                `
                const {b} = require('n');
                let {b: a} = require('n');

                a = b;`)
        );
    });

    test("a member leaving a destructuring that binds others gets a require of its own", async () => {
        await rebind({module: "m", member: "b"}, {module: "n", member: "b"}).rewriteRun(
            //language=javascript
            javascript(`
                const {a, b} = require('m');

                a(b);`,
                `
                const {a} = require('m');
                const {b} = require('n');

                a(b);`));
    });

    test("a rebind refuses a nested require, and one it cannot move without changing what the file declares", async () => {
        const bound: (string | undefined)[] = [];
        await rebind({module: "_stream_duplex", member: "Duplex"}, {module: "stream", member: "Duplex"}, bound).rewriteRun(
            //language=javascript
            javascript(`
                function f() {
                    const {Duplex} = require('_stream_duplex');
                    return new Duplex();
                }`),
            //language=javascript
            javascript(`
                const {Duplex, ...rest} = require('_stream_duplex');

                new Duplex(rest);`),
            //language=javascript
            javascript(`
                let {Duplex, x} = require('_stream_duplex');

                Duplex = x;`),
            //language=typescript
            typescript(`
                const {Duplex}: typeof import('_stream_duplex') = require('_stream_duplex');

                new Duplex();`)
        );
        expect(bound).toEqual([undefined, undefined, undefined, undefined]);
    });
});

describe("maybeBind on a CommonJS file", () => {
    const bindWith = async (options: MaybeBindOptions, before: string, after?: string, path?: string) => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind(options, bound));
        await spec.rewriteRun({...javascript(before, after), ...(path ? {path} : {})});
        return bound.name;
    };

    test("a member gets a require of its own after the file's requires", async () => {
        expect(await bindWith({module: "node:fs/promises", member: "rm"},
            //language=javascript
            `
            const { mkdirp } = require('fs-extra');

            target();`,
            //language=javascript
            `
            const { mkdirp } = require('fs-extra');
            const { rm } = require('node:fs/promises');

            rm.target();`
        )).toBe("rm");
    });

    test("a member joins a const destructuring require of its module that has no rest element", async () => {
        expect(await bindWith({module: "fs/promises", member: "rm"},
            //language=javascript
            `
            const {readFile} = require('node:fs/promises');

            target();`,
            //language=javascript
            `
            const {readFile, rm} = require('node:fs/promises');

            rm.target();`
        )).toBe("rm");

        expect(await bindWith({module: "fs/promises", member: "rm"},
            //language=javascript
            `
            const {a, ...rest} = require('fs/promises');
            let {b} = require('fs/promises');

            target();`,
            //language=javascript
            `
            const {a, ...rest} = require('fs/promises');
            let {b} = require('fs/promises');
            const {rm} = require('fs/promises');

            rm.target();`
        )).toBe("rm");
    });

    test("a whole module binds a name, first or after a shebang or 'use strict' in a file without top-level requires", async () => {
        expect(await bindWith({module: "path"},
            //language=javascript
            `
            #!/usr/bin/env node
            module.exports = target();`,
            //language=javascript
            `
            #!/usr/bin/env node
            const path = require('path');

            module.exports = path.target();`
        )).toBe("path");
        expect(await bindWith({module: "path"},
            //language=javascript
            `
            'use strict';

            module.exports = target();`,
            //language=javascript
            `
            'use strict';

            const path = require('path');

            module.exports = path.target();`
        )).toBe("path");
        expect(await bindWith({module: "path", member: "*", preferredName: "p"},
            //language=javascript
            `exports.f = target();`,
            //language=javascript
            `
            const p = require('path');

            exports.f = p.target();`
        )).toBe("p");
    });

    test("a require joins and follows only the file's leading requires, which every top-level use comes after", async () => {
        expect(await bindWith({module: "fs/promises", member: "rm"},
            //language=javascript
            `
            const {x} = require('x');
            target();
            const {readFile} = require('fs/promises');`,
            //language=javascript
            `
            const {x} = require('x');
            const {rm} = require('fs/promises');
            rm.target();
            const {readFile} = require('fs/promises');`
        )).toBe("rm");
    });

    test("a taken name is deconflicted and an alias is bound verbatim", async () => {
        expect(await bindWith({module: "fs/promises", member: "rm"},
            //language=javascript
            `
            const {mkdirp} = require('fs-extra');
            const rm = 1;

            target();`,
            //language=javascript
            `
            const {mkdirp} = require('fs-extra');
            const {rm: rm_1} = require('fs/promises');
            const rm = 1;

            rm_1.target();`
        )).toBe("rm_1");
        expect(await bindWith({module: "fs/promises", member: "rm", alias: "remove"},
            //language=javascript
            `
            const {mkdirp} = require('fs-extra');

            target();`,
            //language=javascript
            `
            const {mkdirp} = require('fs-extra');
            const {rm: remove} = require('fs/promises');

            remove.target();`
        )).toBe("remove");
    });

    test("a type-only request refuses, since a require binds values only, unless it asks for an import", async () => {
        expect(await bindWith({module: "fs/promises", member: "FileHandle", typeOnly: true},
            //language=javascript
            `
            const {mkdirp} = require('fs-extra');

            target();`
        )).toBeUndefined();

        expect(await bindWith({module: "fs/promises", member: "FileHandle", typeOnly: true, style: ImportStyle.ES6Named},
            //language=javascript
            `
            const {mkdirp} = require('fs-extra');

            target();`,
            //language=javascript
            `
            import type {FileHandle} from 'fs/promises';

            const {mkdirp} = require('fs-extra');

            FileHandle.target();`,
            "a.ts"
        )).toBe("FileHandle");
    });
});
