import {JavaScriptParser} from "../../src/javascript";
import {J} from "../../src/java";
import {JS} from "../../src/javascript";

async function firstCall(source: string): Promise<J.MethodInvocation> {
    const parser = new JavaScriptParser();
    const gen = parser.parse({text: source, sourcePath: "a.js"});
    const parsed = (await gen.next()).value as JS.CompilationUnit;
    const statement = parsed.statements[0].element;
    return statement as J.MethodInvocation;
}

describe("debug6", () => {
    test("comment after comma on non-last entry", async () => {
        const call = await firstCall(`define([\n    "a/B", // note\n    "c/D"\n], function (B, D) {});`);
        const arr = call.arguments.elements[0].element as J.NewArray;
        arr.initializer!.elements.forEach((e: any, i: number) => {
            console.log(`[${i}] after=`, JSON.stringify(e.after));
        });
    });
    test("comment before comma (attached to entry itself), last entry", async () => {
        const call = await firstCall(`define([\n    "a/B" // note\n], function (B) {});`);
        const arr = call.arguments.elements[0].element as J.NewArray;
        arr.initializer!.elements.forEach((e: any, i: number) => {
            console.log(`[${i}] after=`, JSON.stringify(e.after));
        });
    });
});
