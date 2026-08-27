import {fromVisitor, RecipeSpec} from "../../src/test";
import {
    JavaScriptVisitor, JS, javascript, typescript, moduleBindings, isAmdBlock, ModuleBindings
} from "../../src/javascript";
import {J} from "../../src/java";

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

    test("a file with no bindings reports the lane bindModule would take", async () => {
        const spec = new RecipeSpec();
        const seen: {moduleSystem?: string} = {};
        spec.recipe = fromVisitor(captureBindings(seen));
        await spec.rewriteRun(typescript(`const x = 1;`));
        expect(seen.moduleSystem).toBe("esm");
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
