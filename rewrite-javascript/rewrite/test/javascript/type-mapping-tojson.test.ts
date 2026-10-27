/*
 * Copyright 2025 the original author or authors.
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
import {JavaScriptParser, JS, sourceFileCache} from "../../src/javascript";

const parser = new JavaScriptParser({sourceFileCache});

const SOURCE = `
class Alpha { a: number = 1; }
class Beta { b: string = "x"; }
class Gamma { g: boolean = true; }
const alpha = new Alpha();
const beta = new Beta();
const gamma = new Gamma();
function use(x: Alpha, y: Beta, z: Gamma): number { return x.a; }
`;

function collectToJSON(root: unknown): Set<Function> {
    const seen = new WeakSet<object>();
    const found = new Set<Function>();
    const stack: unknown[] = [root];
    while (stack.length) {
        const node = stack.pop();
        if (node === null || typeof node !== "object") {
            continue;
        }
        if (seen.has(node as object)) {
            continue;
        }
        seen.add(node as object);
        const toJSON = (node as Record<string, unknown>).toJSON;
        if (typeof toJSON === "function" && Object.prototype.hasOwnProperty.call(node, "toJSON")) {
            found.add(toJSON as Function);
        }
        for (const value of Object.values(node as Record<string, unknown>)) {
            stack.push(value);
        }
    }
    return found;
}

test("JavaType instances share a single toJSON function", async () => {
    // given
    const cu = (await parser.parse({text: SOURCE, sourcePath: "t.ts"}).next()).value as JS.CompilationUnit;

    // when
    const distinctToJSON = collectToJSON(cu);

    // then
    expect(distinctToJSON.size).toBeGreaterThan(0);
    expect(distinctToJSON.size).toBe(1);
});
