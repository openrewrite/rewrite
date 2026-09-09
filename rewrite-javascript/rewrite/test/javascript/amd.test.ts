import {J} from "../../src/java";
import {Cursor} from "../../src/tree";
import {
    amdBlockOf, bodyOf, deconflict, dependencyNames, derivedBindingName, elementsOf, enclosingAmdBlock,
    identifierOf, javascript, JavaScriptParser, JavaScriptVisitor, JS, lastSegment, namesDeclaredWithin,
    namesUsed, parameterNames, parametersOf, present, references, withDependency, withoutDependencyAt,
    withUnboundDependency
} from "../../src/javascript";
import {fromVisitor, RecipeSpec} from "../../src/test";

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
                withDependency(m, block, module, binding) ?? m;
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

    test("an arrow factory keeps one per line same as a function factory", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(addDependency("c/D", "D"));
        await spec.rewriteRun(javascript(
            `sap.ui.define([\n    "a/B"\n], (\n    B\n) => {});`,
            `sap.ui.define([\n    "a/B",\n    "c/D"\n], (\n    B,\n    D\n) => {});`
        ));
    });

    test("an arrow factory's trailing comma moves to the entry that ends up last", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(addDependency("c/D", "D"));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B",], (B,) => {});`,
            `sap.ui.define(["a/B", "c/D",], (B, D,) => {});`
        ));
    });

    test("an unparenthesized single-parameter arrow gains parens as it grows past one", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(addDependency("c/D", "D"));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B"], B => {});`,
            `sap.ui.define(["a/B", "c/D"], (B, D) => {});`
        ));
    });

    test("a comment before a bare arrow stays with the parameter it followed as parens go on", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(addDependency("c/D", "D"));
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B"], B /*x*/ => {});`,
            `sap.ui.define(["a/B", "c/D"], (B /*x*/ , D) => {});`
        ));
    });

    test("refuses when the factory already binds fewer parameters than there are dependencies", async () => {
        const call = await firstCall(`define(["a/B", "jquery"], function (B) {});`);
        const block = amdBlockOf(call)!;
        expect(withDependency(call, block, "c/D", "D")).toBeUndefined();
    });

    test("refuses when the factory already binds more parameters than there are dependencies", async () => {
        const call = await firstCall(`define(["a/B"], function (B, Extra) {});`);
        const block = amdBlockOf(call)!;
        expect(withDependency(call, block, "c/D", "D")).toBeUndefined();
    });

    test("a trailing comment stays on the entry it followed instead of relabeling the appended one", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(addDependency("c/D", "D"));
        await spec.rewriteRun(javascript(
            `sap.ui.define([\n    "a/B" // note\n], function (\n    B\n) {});`,
            `sap.ui.define([\n    "a/B" // note\n,\n    "c/D"], function (\n    B,\n    D\n) {});`
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

    test("removing the only entry leaves an empty array, not a hole", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                const block = amdBlockOf(m);
                return block === undefined ? super.visitMethodInvocation(m, p) :
                    withoutDependencyAt(m, block, 0);
            }
        });
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B"], function (B) {});`,
            `sap.ui.define([], function () {});`
        ));
    });

    test("an out-of-range index leaves the block unchanged instead of throwing", async () => {
        const call = await firstCall(`define(["a/B", "c/D"], function (B, D) {});`);
        const block = amdBlockOf(call)!;
        expect(dependencyNames(amdBlockOf(withoutDependencyAt(call, block, 2))!)).toEqual(["a/B", "c/D"]);
    });

    function removeFirstDependency() {
        return new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                const block = amdBlockOf(m);
                return block === undefined ? super.visitMethodInvocation(m, p) :
                    withoutDependencyAt(m, block, 0);
            }
        };
    }

    test("removing the only parameter of an unparenthesized arrow keeps the space before the arrow", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(removeFirstDependency());
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B"], B => {});`,
            `sap.ui.define([], () => {});`
        ));
    });

    test("removing the only parameter of a parenthesized arrow leaves its parens and arrow alone", async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(removeFirstDependency());
        await spec.rewriteRun(javascript(
            `sap.ui.define(["a/B"], (B) => {});`,
            `sap.ui.define([], () => {});`
        ));
    });

    test("removing the first entry moves no comment onto the survivor and drops none of its own", async () => {
        const relabeled = new RecipeSpec();
        relabeled.recipe = fromVisitor(removeFirstDependency());
        await relabeled.rewriteRun(javascript(
            `sap.ui.define([/* keep me */ "a/B", "c/D"], function (B, D) {});`,
            `sap.ui.define(["c/D"], function (D) {});`
        ));

        const dropped = new RecipeSpec();
        dropped.recipe = fromVisitor(removeFirstDependency());
        await dropped.rewriteRun(javascript(
            `sap.ui.define(["a/B", /* about D */ "c/D"], function (B, D) {});`,
            `sap.ui.define([/* about D */ "c/D"], function (D) {});`
        ));
    });
});

describe("withUnboundDependency", () => {
    function addUnbound(module: string) {
        return new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(m: J.MethodInvocation, p: any): Promise<J | undefined> {
                const block = amdBlockOf(m);
                return block === undefined ? super.visitMethodInvocation(m, p) :
                    withUnboundDependency(m, block, module) ?? m;
            }
        };
    }

    test("the appended dependency takes no parameter, whether or not the block already leaves one unbound", async () => {
        const bound = new RecipeSpec();
        bound.recipe = fromVisitor(addUnbound("c/D"));
        await bound.rewriteRun(javascript(
            `sap.ui.define(["a/B"], function (B) {});`,
            `sap.ui.define(["a/B", "c/D"], function (B) {});`
        ));

        const alreadyUnbound = new RecipeSpec();
        alreadyUnbound.recipe = fromVisitor(addUnbound("e/F"));
        await alreadyUnbound.rewriteRun(javascript(
            `sap.ui.define(["a/B", "c/D"], function (B) {});`,
            `sap.ui.define(["a/B", "c/D", "e/F"], function (B) {});`
        ));
    });

    test("a surplus parameter refuses, since the appended dependency would bind to it", async () => {
        const call = await firstCall(`sap.ui.define(["a/B"], function (B, D) {});`);
        expect(withUnboundDependency(call, amdBlockOf(call)!, "c/D")).toBeUndefined();
    });
});

describe("the surface a consumer builds on", () => {
    test("a bare cursor finds the block, which then reads entirely through helpers the entry point exports", async () => {
        const call = await firstCall(`sap.ui.define(["a/B", "c/D"], function (B, D) { return B; });`);
        const block = enclosingAmdBlock(new Cursor(call))!.block;
        expect(elementsOf(block)).toHaveLength(2);
        expect(present(block.dependencies.initializer!.elements)).toHaveLength(2);
        expect(parametersOf(block).map(padded => identifierOf(padded.element)?.simpleName)).toEqual(["B", "D"]);
        expect(bodyOf(block)!.kind).toBe(J.Kind.Block);
        expect(await references(block, "B", false)).toBe(true);
        expect((await namesUsed(block, false)).has("D")).toBe(false);

        const declared = namesDeclaredWithin(block.factory);
        expect(lastSegment("a/B")).toBe("B");
        expect(derivedBindingName("a/B")).toBe("B");
        expect(deconflict(derivedBindingName("a/B")!, name => declared.has(name))).toBe("B_1");
    });
});
