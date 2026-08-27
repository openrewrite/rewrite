import {JavaScriptParser} from "../../src/javascript";
import {J} from "../../src/java";
import {JS} from "../../src/javascript";
import {amdBlockOf, dependencyNames, parameterNames, withDependency, withoutDependencyAt} from "../../src/javascript/amd";
import {fromVisitor, RecipeSpec} from "../../src/test";
import {javascript, JavaScriptVisitor} from "../../src/javascript";

async function firstCall(source: string): Promise<J.MethodInvocation> {
    const parser = new JavaScriptParser();
    const gen = parser.parse({text: source, sourcePath: "a.js"});
    const parsed = (await gen.next()).value as JS.CompilationUnit;
    const statement = parsed.statements[0].element;
    return statement as J.MethodInvocation;
}

describe("amdBlockOf", () => {
    test("a named block with an export flag still pairs deps with parameters", async () => {
        const call = await firstCall(`sap.ui.define("my/M", ["a/B", "c/D"], function (B, D) {}, true);`);
        const block = amdBlockOf(call)!;
        expect(dependencyNames(block)).toEqual(["a/B", "c/D"]);
        expect(parameterNames(block)).toEqual(["B", "D"]);
    });

    test("a block written without a dependency array has an empty one", async () => {
        const call = await firstCall(`sap.ui.define(function () {});`);
        const block = amdBlockOf(call)!;
        expect(block.dependenciesIndex).toBe(-1);
        expect(dependencyNames(block)).toEqual([]);
    });

    test("an arrow factory is a factory", async () => {
        const call = await firstCall(`define(["a/B"], (B) => {});`);
        expect(parameterNames(amdBlockOf(call)!)).toEqual(["B"]);
    });

    test("a call that is not an AMD block is not one", async () => {
        const call = await firstCall(`foo.bar(["a/B"], function (B) {});`);
        expect(amdBlockOf(call)).toBeUndefined();
    });

    test("a dependency the factory takes no parameter for reads as unbound", async () => {
        const call = await firstCall(`define(["a/B", "c/D"], function (B) {});`);
        expect(parameterNames(amdBlockOf(call)!)).toEqual(["B"]);
    });

    test("a callee written without a dot matches whatever the receiver", async () => {
        const call = await firstCall(`foo.define(["a/B"], function (B) {});`);
        expect(amdBlockOf(call)).toBeDefined();
    });

    test("a dotted callee requires the whole namespaced path", async () => {
        const namespaced = await firstCall(`sap.ui.define(["a/B"], function (B) {});`);
        expect(amdBlockOf(namespaced, ["sap.ui.define"])).toBeDefined();
        expect(amdBlockOf(namespaced, ["sap.ui.other"])).toBeUndefined();

        const bare = await firstCall(`define(["a/B"], function (B) {});`);
        expect(amdBlockOf(bare, ["sap.ui.define"])).toBeUndefined();
    });

    test("an empty dependency array parses as no dependencies, not one J.Empty", async () => {
        const call = await firstCall(`define([], function () {});`);
        expect(dependencyNames(amdBlockOf(call)!)).toEqual([]);
    });
});

function addDependency(module: string, binding: string) {
    return new class extends JavaScriptVisitor<any> {
        override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
            const block = amdBlockOf(m);
            return block === undefined ? super.visitMethodInvocation(m, p) :
                withDependency(m, block, module, binding);
        }
    };
}

describe("withDependency", () => {
    test("a one-per-line block keeps one per line", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(addDependency("c/D", "D"));
        await spec.rewriteRun(javascript(
            `sap.ui.define([\n    "a/B"\n], function (\n    B\n) {});`,
            `sap.ui.define([\n    "a/B",\n    "c/D"\n], function (\n    B,\n    D\n) {});`
        ));
    });

    test("an existing trailing comma moves to the entry that ends up last", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(addDependency("c/D", "D"));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B",], function (B,) {});`,
            `sap.ui.define(["a/B", "c/D",], function (B, D,) {});`
        ));
    });

    test("a block with no dependency array gains one before the factory", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(addDependency("c/D", "D"));
        await spec.rewriteRun(javascript(
            `sap.ui.define(function () {});`,
            `sap.ui.define(["c/D"], function (D) {});`
        ));
    });
});

describe("withoutDependencyAt", () => {
    test("removing the last entry hands the closing bracket's whitespace to its predecessor", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                const block = amdBlockOf(m);
                return block === undefined ? super.visitMethodInvocation(m, p) :
                    withoutDependencyAt(m, block, 1);
            }
        });
        await spec.rewriteRun(javascript(
            `sap.ui.define([\n    "a/B",\n    "c/D"\n], function (\n    B,\n    D\n) {});`,
            `sap.ui.define([\n    "a/B"\n], function (\n    B\n) {});`
        ));
    });

    test("removing the first entry hands its position to the one that takes its place", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                const block = amdBlockOf(m);
                return block === undefined ? super.visitMethodInvocation(m, p) :
                    withoutDependencyAt(m, block, 0);
            }
        });
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B", "c/D"], function (B, D) {});`,
            `sap.ui.define(["c/D"], function (D) {});`
        ));
    });
});
