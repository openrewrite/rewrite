import {fromVisitor, RecipeSpec} from "../../src/test";
import {withDir} from "tmp-promise";
import {
    JavaScriptVisitor, JS, javascript, npm, packageJson, tsx, typescript, moduleBindings, isAmdBlock, ModuleBindings, maybeBind,
    maybeAddImport, MaybeBindOptions, maybeUnbind, maybeRebind, maybeRemoveImport, removeNewlyUnusedAmdBindings
} from "../../src/javascript";
import {emptySpace, J, rightPadded, Type} from "../../src/java";
import {emptyMarkers} from "../../src/markers";
import {randomId} from "../../src/uuid";
import {ExecutionContext} from "../../src";

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
        await spec.rewriteRun(typescript(
            `target();`,
            `import Element from 'sap/ui/core/Element';\n\nElement.target();`
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
        await spec.rewriteRun(typescript(
            `target();`,
            `import Element from 'sap/ui/core/Element';\n\nElement.target();`
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

    test("ESM binds a module afresh where the import it would reuse is shadowed", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/m/Button", bound));
        await spec.rewriteRun(typescript(
            `import Button from "sap/m/Button";\n\nfunction inner() { const Button = 1; target(); return Button; }`,
            `import Button from "sap/m/Button";\nimport Button_1 from "sap/m/Button";\n\nfunction inner() { const Button = 1; Button_1.target(); return Button; }`
        ));
        expect(bound.name).toBe("Button_1");

        // A `const` binds its whole block, so reaching the import ahead of one is a TDZ error
        // rather than a reference to the module — the ordering of the two does not change that.
        const declaredLater = new RecipeSpec();
        declaredLater.recipe = fromVisitor(rebind("sap/m/Button", bound));
        await declaredLater.rewriteRun(typescript(
            `import Button from "sap/m/Button";\n\nfunction inner() { target(); const Button = 1; return Button; }`,
            `import Button from "sap/m/Button";\nimport Button_1 from "sap/m/Button";\n\nfunction inner() { Button_1.target(); const Button = 1; return Button; }`
        ));
        expect(bound.name).toBe("Button_1");
    });

    test("ESM binds a member afresh where the specifier it would reuse is shadowed", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind({module: "a/lib", member: "Widget"}, bound));
        await spec.rewriteRun(typescript(
            `import {Widget} from "a/lib";\n\nfunction inner() { const Widget = 1; target(); return Widget; }`,
            `import {Widget, Widget as Widget_1} from "a/lib";\n\nfunction inner() { const Widget = 1; Widget_1.target(); return Widget; }`
        ));
        expect(bound.name).toBe("Widget_1");
    });

    test("ESM reuses a default import bound under another name", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        await spec.rewriteRun(typescript(
            `import Elem from "sap/ui/core/Element";\n\ntarget();`,
            `import Elem from "sap/ui/core/Element";\n\nElem.target();`
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
        await namespaceRequest.rewriteRun(typescript(
            `import * as Button from "sap/m/Button";\n\ntarget();`
        ));

        const bareRequest = new RecipeSpec();
        const bound: {name?: string} = {};
        bareRequest.recipe = fromVisitor(rebind("sap/m/Button", bound));
        await bareRequest.rewriteRun(typescript(
            `import * as Button from "sap/m/Button";\n\ntarget();`,
            `import * as Button from "sap/m/Button";\nimport Button_1 from "sap/m/Button";\n\nButton_1.target();`
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
        await spec.rewriteRun(javascript(
            `const Elem = require("sap/ui/core/Element");\n\ntarget();`
        ));
        expect(bound.name).toBe("Elem");
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
        await namespaceRequest.rewriteRun(typescript(
            `const Button = await import("sap/m/Button");\n\ntarget();`
        ));

        const bareRequest = new RecipeSpec();
        const bound: {name?: string} = {};
        bareRequest.recipe = fromVisitor(rebind("sap/m/Button", bound));
        await bareRequest.rewriteRun(typescript(
            `const Button = await import("sap/m/Button");\n\ntarget();`,
            `import Button_1 from "sap/m/Button";\n\nconst Button = await import("sap/m/Button");\n\nButton_1.target();`
        ));

        expect(namespaceName.value).toBe("Button");
        expect(bound.name).toBe("Button_1");
    });

    test("a destructured await import answers neither request but still occupies its name", async () => {
        const spec = new RecipeSpec();
        const bound: (string | undefined)[] = [];
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.push(maybeBind(this, {module: "sap/m/Button", onlyIfReferenced: false}));
                bound.push(maybeBind(this, {module: "sap/m/Button", member: "*", onlyIfReferenced: false}));
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await spec.rewriteRun(typescript(
            `const {Button} = await import("sap/m/Button");\n\ntarget();`,
            `import Button_1 from "sap/m/Button";\nimport * as Button_2 from "sap/m/Button";\n\nconst {Button} = await import("sap/m/Button");\n\ntarget();`
        ));
        expect(bound).toEqual(["Button_1", "Button_2"]);
    });

    test("a CommonJS file answers with the binding its require already has", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        await spec.rewriteRun(javascript(
            `const Elem = require("sap/ui/core/Element");\n\ntarget();`,
            `const Elem = require("sap/ui/core/Element");\n\nElem.target();`
        ));
        expect(bound.name).toBe("Elem");
    });

    test("a CommonJS file refuses rather than gain an import", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        await spec.rewriteRun(javascript(
            `const other = require("a/Other");\n\ntarget();`
        ));
        expect(bound.name).toBeUndefined();
    });

    test("a member request refuses on a CommonJS file but still creates on an ESM file", async () => {
        const memberBind = (bound: {value?: string}) => new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.value = maybeBind(this, {module: "fs", member: "readFile", onlyIfReferenced: false});
                return super.visitJsCompilationUnit(cu, p);
            }
        };

        const onCommonJs = new RecipeSpec();
        const commonJsName: {value?: string} = {};
        onCommonJs.recipe = fromVisitor(memberBind(commonJsName));
        await onCommonJs.rewriteRun(javascript(
            `const other = require("a/Other");\n\ntarget();`
        ));

        const onEsm = new RecipeSpec();
        const esmName: {value?: string} = {};
        onEsm.recipe = fromVisitor(memberBind(esmName));
        await onEsm.rewriteRun(typescript(
            `target();`,
            `import {readFile} from 'fs';\n\ntarget();`
        ));

        expect(commonJsName.value).toBeUndefined();
        expect(esmName.value).toBe("readFile");
    });

    test("a .mjs file with a require call still creates an import", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("sap/ui/core/Element", bound));
        await spec.rewriteRun({
            ...javascript(
                `const other = require("a/Other");\n\ntarget();`,
                `import Element from "sap/ui/core/Element";\n\nconst other = require("a/Other");\n\nElement.target();`
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
            return spec.rewriteRun(typescript(
                `target();`,
                `import {readFile} from 'fs';\n\ntarget();`
            )).then(() => bound.name);
        };

        const viaAddImport = await bindWith(v => maybeAddImport(v, {module: "fs", member: "readFile", onlyIfReferenced: false}));
        const viaBind = await bindWith(v => maybeBind(v, {module: "fs", member: "readFile", onlyIfReferenced: false}));
        expect(viaAddImport).toBe("readFile");
        expect(viaBind).toBe("readFile");
    });
});

    test("a type-only import does not answer a request for a value", async () => {
        // The name a type-only import binds erases, so reusing it would emit an unbound reference.
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("m", bound));
        await spec.rewriteRun(typescript(
            `import type X from "m";\ntarget();`,
            `import type X from "m";\nimport m from "m";\nm.target();`));
        expect(bound.name).toBe("m");
    });

    test("a file that exports is a module, whatever else it requires", async () => {
        // `export` settles the module system, so a `require` alongside it does not make the file
        // CommonJS and does not block creating an import.
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(rebind("m", bound));
        await spec.rewriteRun(typescript(
            `export function f() {}\nconst p = require("path");\ntarget();`,
            `import m from "m";\n\nexport function f() {}\nconst p = require("path");\nm.target();`));
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
        await spec.rewriteRun(typescript(
            `import Y from "m";\ntarget();`,
            `import Y from "m";\nimport Pinned from "m";\nPinned.target();`));
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

function rebindOldToNew() {
    return new class extends JavaScriptVisitor<any> {
        override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
            maybeRebind(this, {from: {module: "m", member: "Old"}, to: {module: "m2", member: "New"}});
            return super.visitJsCompilationUnit(cu, p);
        }
    };
}

describe("maybeRebind", () => {
    test("an ESM member rename takes the new name, carrying the references that resolve to it", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name = maybeRebind(this, {
                    from: {module: "lodash", member: "extend"},
                    to: {module: "lodash", member: "assign"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await spec.rewriteRun(typescript(
            `import { extend } from "lodash";\n\nextend({}, {});\nobj.extend();\nfunction f() { const extend = 1; return extend; }`,
            `import { assign } from "lodash";\n\nassign({}, {});\nobj.extend();\nfunction f() { const extend = 1; return extend; }`
        ));
        expect(bound.name).toBe("assign");
    });

    test("a pinned alias names the binding, and the references follow it", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name = maybeRebind(this, {
                    from: {module: "lodash", member: "extend"},
                    to: {module: "lodash", member: "assign", alias: "merge"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await spec.rewriteRun(typescript(
            `import { extend } from "lodash";\n\nextend({}, {});`,
            `import { assign as merge } from "lodash";\n\nmerge({}, {});`
        ));
        expect(bound.name).toBe("merge");
    });

    test("an aliased member keeps the alias, which is a name of the file's own choosing", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name = maybeRebind(this, {
                    from: {module: "lodash", member: "extend"},
                    to: {module: "lodash", member: "assign"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await spec.rewriteRun(typescript(
            `import { extend as myExtend } from "lodash";\n\nmyExtend({}, {});`,
            `import { assign as myExtend } from "lodash";\n\nmyExtend({}, {});`
        ));
        expect(bound.name).toBe("myExtend");
    });

    test("an alias the new member name makes redundant is dropped", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name = maybeRebind(this, {
                    from: {module: "lodash", member: "extend"},
                    to: {module: "lodash", member: "assign"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await spec.rewriteRun(typescript(
            `import { extend as assign } from "lodash";\n\nassign({}, {});`,
            `import { assign } from "lodash";\n\nassign({}, {});`
        ));
        expect(bound.name).toBe("assign");
    });

    test("a re-export keeps the name the module publishes, and the binding under it follows", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        await spec.rewriteRun(typescript(
            `import { Old } from "m";\n\nexport { Old };\nconst y = Old;`,
            `import { New } from "m2";\n\nexport { New as Old };\nconst y = New;`
        ));
    });

    test("an aliased re-export renames only the reference beside the name it publishes", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        await spec.rewriteRun(typescript(
            `import { Old } from "m";\n\nconst X = 1;\nexport { Old as Public };\nexport { X as Old };`,
            `import { New } from "m2";\n\nconst X = 1;\nexport { New as Public };\nexport { X as Old };`
        ));
    });

    test("a shorthand property keeps its key and takes the renamed binding as its value", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        await spec.rewriteRun(typescript(
            `import { Old } from "m";\n\nconst p = {Old};\nconst o = {Old: 1};`,
            `import { New } from "m2";\n\nconst p = {Old: New};\nconst o = {Old: 1};`
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
        await spec.rewriteRun(typescript(`import { Old } from "m";\n\nconst taken = 1;\nOld();`));
        expect(bound.name).toBeUndefined();
    });

    test("an AMD alias pin refuses where it renames the parameter, and passes where it agrees", async () => {
        const amdRebind = (alias: string, bound: {name?: string}) => new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (m.name.simpleName !== "target") {
                    return super.visitMethodInvocation(m, p);
                }
                bound.name = maybeRebind(this, {from: {module: "a/Old"}, to: {module: "a/New", alias}});
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
        await spec.rewriteRun(typescript(`import { Old } from "m";\n\nconst y = Old;`));
        expect(bound.name).toBeUndefined();
    });

    test("a name the file only references, never declares, still counts as taken", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                bound.name = maybeRebind(this, {
                    from: {module: "m", member: "Old"},
                    to: {module: "m2", member: "Event"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        // `Event` is an ambient global: the file declares it nowhere, so a check reading only
        // declarations would call the name free and let the import shadow it here.
        await spec.rewriteRun(typescript(
            `import { Old } from "m";\n\nfunction f(e: Event) { return Old(e); }`,
            `import { Event as Old } from "m2";\n\nfunction f(e: Event) { return Old(e); }`
        ));
        expect(bound.name).toBe("Old");
    });

    test("a reference in type-name position renames: a decorator, and a generic's own base", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        await spec.rewriteRun(typescript(
            `import { Old } from "m";\n\n@Old({})\nexport class C {\n    @Old() field: Old<string>;\n}`,
            `import { New } from "m2";\n\n@New({})\nexport class C {\n    @New() field: New<string>;\n}`
        ));
    });

    test("a name a bind queued earlier in the same visit has claimed is taken too", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeBind(this, {module: "other", member: "New", onlyIfReferenced: false});
                bound.name = maybeRebind(this, {
                    from: {module: "m", member: "Old"},
                    to: {module: "m2", member: "New"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await spec.rewriteRun(typescript(
            `import { Old } from "m";\n\nconst y = Old;`,
            `import { New as Old } from "m2";\nimport {New} from "other";\n\nconst y = Old;`
        ));
        expect(bound.name).toBe("Old");
    });

    test("a name in a namespace of its own — a nearer scope, a label — is left alone", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        await spec.rewriteRun(typescript(
            `import { Old } from "m";\n\nconst z = Old;\nOld: for (;;) { break Old; }\nfunction f() { const Old = 1; return {Old}; }`,
            `import { New } from "m2";\n\nconst z = New;\nOld: for (;;) { break Old; }\nfunction f() { const Old = 1; return {Old}; }`
        ));
    });

    test("a re-export from another module names that module's member, not the binding", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        await spec.rewriteRun(typescript(
            `import { Old } from "m";\n\nexport { Old } from "./other";\nconst y = Old;`,
            `import { New } from "m2";\n\nexport { Old } from "./other";\nconst y = New;`
        ));
    });

    test("a second statement binding the same member is left intact, not given the first's name", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(rebindOldToNew());
        // One call moves one binding, so the second statement is a later call's to make; sharing
        // the name read from the first would bind it twice.
        await spec.rewriteRun(typescript(
            `import { Old } from "m";\nimport { Old as Other } from "m";\n\nconst y = Old;\nconst z = Other;`,
            `import { New } from "m2";\nimport { Old as Other } from "m";\n\nconst y = New;\nconst z = Other;`
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
                ...typescript(
                    `import { extend, flatten } from 'lodash';\n\nextend({}, {});\nflatten([[1]]);\n`,
                    `import { flatten } from 'lodash';\nimport { assign } from 'lodash-es';\n\nassign({}, {});\nflatten([[1]]);\n`),
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
                ...typescript(
                    `import { extend, flatten } from 'lodash';\n\nextend({}, {});\nflatten([[1]]);\n`,
                    `import { flatten } from 'lodash';\nimport extend from 'lodash-es';\n\nextend({}, {});\nflatten([[1]]);\n`),
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

    test("a constructed reference's attribution moves with the binding", async () => {
        const spec = new RecipeSpec();
        const declaring: (string | undefined)[] = [];
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {
                    from: {module: "events", member: "EventEmitter"},
                    to: {module: "node:events", member: "EventEmitter"}
                });
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await withDir(async (repo) => {
            await spec.rewriteRun(npm(repo.path, {
                ...typescript(
                    `import { EventEmitter } from 'events';\n\nconst e = new EventEmitter();\n`,
                    `import { EventEmitter } from 'node:events';\n\nconst e = new EventEmitter();\n`),
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
        expect(declaring).toEqual(["node:events"]);
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
                ...typescript(
                    `import { extend } from 'lodash';\n\nextend?.({}, {});\n`,
                    `import { assign } from 'lodash-es';\n\nassign?.({}, {});\n`),
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
                    `import { helper } from './util';\n\nconst x = helper;\n`,
                    `import { helper2 } from './util2';\n\nconst x = helper2;\n`), path: "main.ts"} as any,
                packageJson(`{"name":"t"}`)));
        }, {unsafeCleanup: true});
    }, 30000);

    test("an AMD dependency swap keeps the parameter and its index", async () => {
        const spec = new RecipeSpec();
        const bound: {name?: string} = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (m.name.simpleName !== "target") {
                    return super.visitMethodInvocation(m, p);
                }
                bound.name = maybeRebind(this, {from: {module: "a/Old"}, to: {module: "a/New"}});
                return m;
            }
        });
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B", "a/Old", "a/C"], function (B, Old, C) { target(); });`,
            `sap.ui.define(["a/B", "a/New", "a/C"], function (B, Old, C) { target(); });`
        ));
        expect(bound.name).toBe("Old");
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
        await spec.rewriteRun(typescript(
            `import React, {useState, useMemo} from "react";\n\nReact.foo();\nuseState();\nuseMemo();`,
            `import {useState, useMemo} from "react";\nimport React from "preact";\n\nReact.foo();\nuseState();\nuseMemo();`
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
        await spec.rewriteRun(typescript(
            `import D, * as N from "m";\n\nD();\nN.foo();`,
            `import D from "m";\nimport * as N from "m2";\n\nD();\nN.foo();`
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
        await spec.rewriteRun(typescript(
            `import type { a, b} from "m";\n\nlet x: a;\nlet y: b;`,
            `import type { b} from "m";\nimport type {a} from "m2";\n\nlet x: a;\nlet y: b;`
        ));
    });

    test("a rebind that would change default/named/namespace shape refuses, since the only edit available is in place", async () => {
        const namedToDefault = new RecipeSpec();
        const namedToDefaultBound: {name?: string} = {};
        namedToDefault.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                namedToDefaultBound.name = maybeRebind(this, {from: {module: "old", member: "act"}, to: {module: "new", member: "default"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await namedToDefault.rewriteRun(typescript(`import {act} from "old";\n\nact();`));
        expect(namedToDefaultBound.name).toBeUndefined();

        const defaultToNamed = new RecipeSpec();
        const defaultToNamedBound: {name?: string} = {};
        defaultToNamed.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                defaultToNamedBound.name = maybeRebind(this, {from: {module: "old"}, to: {module: "new", member: "act"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await defaultToNamed.rewriteRun(typescript(`import D from "old";\n\nD();`));
        expect(defaultToNamedBound.name).toBeUndefined();
    });

    test("a moved specifier's own inline type marker survives even where the clause it left is not type-only", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
                maybeRebind(this, {from: {module: "m", member: "a"}, to: {module: "m2", member: "a"}});
                return super.visitJsCompilationUnit(cu, p);
            }
        });
        await spec.rewriteRun(typescript(
            `import {type a, b} from "m";\n\nlet x: a;\nlet y: b;`,
            `import {b} from "m";\nimport type {a} from "m2";\n\nlet x: a;\nlet y: b;`
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
        await spec.rewriteRun(javascript(
            `define(["a/B", "a/C"], function (B, C) { C.f(); });\nmyDefine(["a/B", "a/D"], function (B, D) { D.f(); });`,
            `define(["a/C"], function (C) { C.f(); });\nmyDefine(["a/D"], function (D) { D.f(); });`
        ));
    });
});

describe("maybeUnbind on an ESM file", () => {
    test("removes the import; the AMD lane it also queues finds no block to act on", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(dropModule("sap/m/Button"));
        await spec.rewriteRun(typescript(
            `import Button from "sap/m/Button";\n\nconsole.log(1);`,
            `console.log(1);`
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
        await spec.rewriteRun(typescript(
            `import Button from "sap/m/Button";\n\nconsole.log(1);`,
            `console.log(1);`
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
            return spec.rewriteRun(typescript(
                `import Button from "sap/m/Button";\n\nconsole.log(1);`,
                `console.log(1);`
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
        await spec.rewriteRun(typescript(
            `import D, {a} from "m";\na();`,
            `import D from "m";\nimport {a} from "m2";\na();`));
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
        await spec.rewriteRun(tsx(
            `import { Old } from "m";\n\nconst e = <div Old="x">{Old}</div>;`,
            `import { New } from "m2";\n\nconst e = <div Old="x">{New}</div>;`
        ));
    });
});
