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
import v8 from "node:v8";
import vm from "node:vm";
import {JavaScriptParser, JS, sourceFileCache} from "../../src/javascript";
import {Type} from "../../src/java";

const parser = new JavaScriptParser({sourceFileCache});

const SOURCE = `
class Alpha { a: number = 1; }
class Beta { b: string = "x"; }
class Gamma { g: boolean = true; }
const alpha = new Alpha();
const beta = new Beta();
const gamma = new Gamma();
function use(x: Alpha, y: Beta, z: Gamma): number { return x.a + y.b.length; }
`;

function forceGc(): void {
    if (typeof global.gc === "function") {
        global.gc();
        return;
    }
    v8.setFlagsFromString("--expose_gc");
    try {
        vm.runInNewContext("gc")();
    } finally {
        v8.setFlagsFromString("--no-expose_gc");
    }
}

function collectDistinctToJSON(root: unknown, into: Set<Function>): void {
    const seen = new WeakSet<object>();
    const stack: unknown[] = [root];
    while (stack.length) {
        const node = stack.pop();
        if (node === null || typeof node !== "object" || seen.has(node as object)) {
            continue;
        }
        seen.add(node as object);
        if (Object.prototype.hasOwnProperty.call(node, "toJSON") &&
            typeof (node as Record<string, unknown>).toJSON === "function") {
            into.add((node as Record<string, unknown>).toJSON as Function);
        }
        for (const value of Object.values(node as Record<string, unknown>)) {
            stack.push(value);
        }
    }
}

test("parsing the same file repeatedly does not accumulate distinct toJSON closures", async () => {
    // given
    const parses = Number(process.env.TOJSON_PARSES ?? 300);

    // when
    const held: JS.CompilationUnit[] = [];
    const distinctToJSON = new Set<Function>();
    for (let i = 0; i < parses; i++) {
        const cu = (await parser.parse({text: SOURCE, sourcePath: "t.ts"}).next()).value as JS.CompilationUnit;
        held.push(cu);
        collectDistinctToJSON(cu, distinctToJSON);
    }

    forceGc();
    const retainedMb = process.memoryUsage().heapUsed / (1024 * 1024);
    console.log(`[toJSON] ${parses} parses retained -> distinct toJSON closures: ${distinctToJSON.size}, heapUsed: ${retainedMb.toFixed(1)} MB`);

    // then
    // Every JavaType across every parse references the single module-level `typeSignatureToJSON`.
    // Before the fix each type-attributed node minted its own closure, so this count grew with `parses`.
    expect(held.length).toBe(parses);
    expect(distinctToJSON.size).toBe(1);
}, 300000);

test("per-instance toJSON closures retain heap that a shared function does not", () => {
    // given
    const instances = 10_000;
    const classShape = () => ({
        kind: Type.Kind.Class,
        flags: 0,
        classKind: Type.Class.Kind.Class,
        fullyQualifiedName: "com.example.Sample",
        typeParameters: [],
        annotations: [],
        interfaces: [],
        members: [],
        methods: []
    });

    // when: the pre-fix shape mints a fresh closure per instance
    forceGc();
    const baseline = process.memoryUsage().heapUsed;
    let perInstance: object[] | null = [];
    const perInstanceFns = new Set<Function>();
    for (let i = 0; i < instances; i++) {
        const type = {...classShape(), toJSON: function (this: Type) { return Type.signature(this); }} as Type.Class;
        perInstance.push(type);
        perInstanceFns.add(type.toJSON as Function);
    }
    forceGc();
    const perInstanceBytes = process.memoryUsage().heapUsed - baseline;

    perInstance = null;
    forceGc();

    // and: the post-fix shape references one hoisted function
    const shared = function (this: Type) { return Type.signature(this); };
    const beforeShared = process.memoryUsage().heapUsed;
    let sharedInstances: object[] | null = [];
    const sharedFns = new Set<Function>();
    for (let i = 0; i < instances; i++) {
        const type = {...classShape(), toJSON: shared} as Type.Class;
        sharedInstances.push(type);
        sharedFns.add(type.toJSON as Function);
    }
    forceGc();
    const sharedBytes = process.memoryUsage().heapUsed - beforeShared;

    console.log(`[toJSON] ${instances} instances -> per-instance: ${perInstanceFns.size} closures, ${(perInstanceBytes / (1024 * 1024)).toFixed(2)} MB; shared: ${sharedFns.size} closure, ${(sharedBytes / (1024 * 1024)).toFixed(2)} MB`);
    void sharedInstances;

    // then
    expect(perInstanceFns.size).toBe(instances);
    expect(sharedFns.size).toBe(1);
    expect(perInstanceBytes).toBeGreaterThan(sharedBytes);
}, 120000);
