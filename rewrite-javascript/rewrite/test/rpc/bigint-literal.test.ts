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
import {ReferenceMap, RpcReceiveQueue, RpcSendQueue} from "../../src/rpc";
import {JavaScriptParser, JS, sourceFileCache} from "../../src/javascript";
import {J} from "../../src/java";
import {TreePrinters} from "../../src/print";
import {create as produce} from "mutative";

function wireCopy(batch: any[]): any[] {
    return JSON.parse(JSON.stringify(batch));
}

function literals(cu: JS.CompilationUnit): J.Literal[] {
    const decl = cu.statements[0].element as J.VariableDeclarations;
    const array = decl.variables[0].element.initializer!.element as J.NewArray;
    return array.initializer!.elements.map(e => e.element as J.Literal);
}

async function parse(text: string): Promise<JS.CompilationUnit> {
    return (await new JavaScriptParser({sourceFileCache}).parse({text, sourcePath: "t.ts"}).next()).value as JS.CompilationUnit;
}

const text = "const a = [1n, 0x1fn, 123456789012345678901234567890n];";
const large = BigInt("123456789012345678901234567890");
const changed = BigInt("1267650600228229401496703205376");

test("a bigint literal keeps its value and source over the wire", async () => {
    // given
    const parsed = await parse(text);

    // when
    const batch = await new RpcSendQueue(new ReferenceMap(), JS.Kind.CompilationUnit, false).generate(parsed, undefined);
    const received = await new RpcReceiveQueue(new Map(), JS.Kind.CompilationUnit, [wireCopy(batch)], undefined, false)
        .receive<JS.CompilationUnit>(undefined);

    // then
    expect(literals(received).map(l => l.value)).toEqual([BigInt(1), BigInt(31), large]);
    expect(literals(received).map(l => l.valueSource)).toEqual(["1n", "0x1fn", "123456789012345678901234567890n"]);
    expect(await TreePrinters.print(received)).toBe(text);
});

test("a changed bigint value is received as a bigint", async () => {
    // given
    const parsed = await parse(text);
    const localRefs = new ReferenceMap();
    const remoteRefs = new Map<number, any>();
    const initial = await new RpcSendQueue(localRefs, JS.Kind.CompilationUnit, false).generate(parsed, undefined);
    const before = await new RpcReceiveQueue(remoteRefs, JS.Kind.CompilationUnit, [wireCopy(initial)], undefined, false)
        .receive<JS.CompilationUnit>(undefined);
    const edited = produce(parsed, draft => {
        const lit = literals(draft as JS.CompilationUnit)[2] as any;
        lit.value = changed;
        lit.valueSource = `${changed}n`;
    });

    // when
    const change = await new RpcSendQueue(localRefs, JS.Kind.CompilationUnit, false).generate(edited, parsed);
    const after = await new RpcReceiveQueue(remoteRefs, JS.Kind.CompilationUnit, [wireCopy(change)], undefined, false)
        .receive<JS.CompilationUnit>(before);

    // then
    expect(literals(after).map(l => l.value)).toEqual([BigInt(1), BigInt(31), changed]);
    expect(literals(after)[0]).toBe(literals(before)[0]);
    expect(await TreePrinters.print(after)).toBe("const a = [1n, 0x1fn, 1267650600228229401496703205376n];");
});
