import {fromVisitor, RecipeSpec} from "../../src/test";
import {
    JavaScriptVisitor, JS, javascript, typescript, moduleBindings, isAmdBlock, ModuleBindings, maybeBind,
    maybeAddImport, maybeUnbind, maybeRemoveImport, removeNewlyUnusedAmdBindings
} from "../../src/javascript";
import {emptySpace, J, rightPadded} from "../../src/java";
import {emptyMarkers} from "../../src/markers";
import {randomId} from "../../src/uuid";
import {ExecutionContext} from "../../src";

function captureBindings(seen: {moduleSystem?: string, module?: string, binding?: string},
                          localName: string = "Button", moduleName: string = "sap/m/Button") {
    return new class extends JavaScriptVisitor<any> {
        override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: any): Promise<J | undefined> {
            const bindings = moduleBindings(this);
            seen.moduleSystem = bindings.moduleSystem;
            seen.module = bindings.moduleOf(localName);
            seen.binding = bindings.bindingOf(moduleName);
            return super.visitJsCompilationUnit(cu, p);
        }
    };
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

    test("an export alone is enough to read as \"esm\", even with no import", async () => {
        const spec = new RecipeSpec();
        const seen: {moduleSystem?: string} = {};
        spec.recipe = fromVisitor(captureBindings(seen));
        await spec.rewriteRun(typescript(`export const x = 1;`));
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
function rebind(module: string, bound: {name?: string}) {
    return new class extends JavaScriptVisitor<any> {
        override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
            if (m.name.simpleName !== "target") {
                return super.visitMethodInvocation(m, p);
            }
            bound.name = maybeBind(this, {module});
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
