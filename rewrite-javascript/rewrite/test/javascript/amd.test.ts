import {JavaScriptParser} from "../../src/javascript";
import {J} from "../../src/java";
import {JS} from "../../src/javascript";
import {amdBlockOf, dependencyNames, parameterNames} from "../../src/javascript/amd";

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
