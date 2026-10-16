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
import {isEqual, JavaScriptParser, JS, sourceFileCache} from "../../src/javascript";
import {J} from "../../src/java";
import {foundSearchResult} from "../../src/markers";

const parser = new JavaScriptParser({sourceFileCache});

async function statement(code: string): Promise<J> {
    const cu = (await parser.parse({text: code, sourcePath: "test.ts"}).next()).value as JS.CompilationUnit;
    return cu.statements[0].element;
}

async function equal(a: string, b: string): Promise<boolean> {
    return isEqual(await statement(a), await statement(b));
}

describe("isEqual", () => {
    test("ignores formatting, how a literal is spelled, type attribution and markers a recipe attaches", async () => {
        expect(await equal(`f(a, b);`, `f( a, /* c */ b )`)).toBe(true);

        expect(await equal(`f(a, b,)`, `f(a, b)`)).toBe(true);

        expect(await equal(`x = 'a'`, `x = "a"`)).toBe(true);

        expect(await equal(`x = '\\uD83D\\uDE00'`, `x = "\\uD83D\\uDE00"`)).toBe(true);

        const call = await statement(`Math.max(1, 2)`) as J.MethodInvocation;
        expect(call.methodType).toBeDefined();
        expect(await isEqual(call, {...call, methodType: undefined} as J.MethodInvocation)).toBe(true);

        expect(await isEqual(call, foundSearchResult(call))).toBe(true);
    });

    test("tells apart literal values and literal kinds", async () => {
        expect(await equal(`x = 1`, `x = 2`)).toBe(false);

        expect(await equal(`x = '1'`, `x = 1n`)).toBe(false);

        expect(await equal(`x = /a/g`, `x = '/a/g'`)).toBe(false);

        expect(await equal(`x = '\\uD800'`, `x = '\\uDC00'`)).toBe(false);
    });

    test("tells apart the markers that change what code means", async () => {
        expect(await equal(`a.b`, `a?.b`)).toBe(false);

        expect(await equal(`a.b`, `a!.b`)).toBe(false);

        expect(await equal(`function f() {}`, `function* f() {}`)).toBe(false);

        expect(await equal(`function* f() { yield g() }`, `function* f() { yield* g() }`)).toBe(false);
    });
});
