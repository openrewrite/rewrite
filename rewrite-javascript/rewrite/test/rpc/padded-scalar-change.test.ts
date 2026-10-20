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

function wireCopy(batch: any[]): any[] {
    return JSON.parse(JSON.stringify(batch));
}

function find<T>(o: any, predicate: (o: any) => boolean): T | undefined {
    if (o === null || typeof o !== "object") {
        return undefined;
    }
    if (!Array.isArray(o) && predicate(o)) {
        return o as T;
    }
    for (const v of Array.isArray(o) ? o : Object.values(o)) {
        const found = find<T>(v, predicate);
        if (found) {
            return found;
        }
    }
    return undefined;
}

function replace(o: any, target: any, replacement: any): any {
    if (o === target) {
        return replacement;
    }
    if (o === null || typeof o !== "object") {
        return o;
    }
    if (Array.isArray(o)) {
        const mapped = o.map(e => replace(e, target, replacement));
        return mapped.some((e, i) => e !== o[i]) ? mapped : o;
    }
    let changed: any | undefined;
    for (const [k, v] of Object.entries(o)) {
        const r = replace(v, target, replacement);
        if (r !== v) {
            changed = changed ?? {...o};
            changed[k] = r;
        }
    }
    return changed ?? o;
}

async function roundTrip<T extends JS.CompilationUnit>(
    text: string,
    edit: (cu: JS.CompilationUnit) => JS.CompilationUnit
): Promise<{ before: T, after: T, expected: string }> {
    const parsed = (await new JavaScriptParser({sourceFileCache}).parse({
        text,
        sourcePath: "t.ts"
    }).next()).value as JS.CompilationUnit;
    const localRefs = new ReferenceMap();
    const remoteRefs = new Map<number, any>();

    const initial = await new RpcSendQueue(localRefs, JS.Kind.CompilationUnit, false).generate(parsed, undefined);
    const before = await new RpcReceiveQueue(remoteRefs, JS.Kind.CompilationUnit, async () => wireCopy(initial), undefined, false)
        .receive<T>(undefined);

    const edited = edit(parsed);
    const change = await new RpcSendQueue(localRefs, JS.Kind.CompilationUnit, false).generate(edited, parsed);
    const after = await new RpcReceiveQueue(remoteRefs, JS.Kind.CompilationUnit, async () => wireCopy(change), undefined, false)
        .receive<T>(before);
    return {before, after, expected: await TreePrinters.print(edited)};
}

test("a changed enum operator inside a left padding is applied on receive", async () => {
    // given
    const text = "const c = a == b;";

    // when
    const {before, after} = await roundTrip(text, cu => {
        const binary = find<J.Binary>(cu, o => o.kind === J.Kind.Binary)!;
        return replace(cu, binary.operator, {...binary.operator, element: J.Binary.Type.NotEqual});
    });

    // then
    expect(find<J.Binary>(before, o => o.kind === J.Kind.Binary)!.operator.element).toBe(J.Binary.Type.Equal);
    expect(find<J.Binary>(after, o => o.kind === J.Kind.Binary)!.operator.element).toBe(J.Binary.Type.NotEqual);
    expect(await TreePrinters.print(after)).toBe("const c = a != b;");
});

test("a changed boolean inside a left padding is applied on receive", async () => {
    // given
    const text = "type A = number;\nexport {type A};";

    // when
    const {before, after, expected} = await roundTrip(text, cu => {
        const specifier = find<JS.ExportSpecifier>(cu, o => o.kind === JS.Kind.ExportSpecifier)!;
        return replace(cu, specifier.typeOnly, {...specifier.typeOnly, element: false});
    });

    // then
    expect(find<JS.ExportSpecifier>(before, o => o.kind === JS.Kind.ExportSpecifier)!.typeOnly.element).toBe(true);
    expect(find<JS.ExportSpecifier>(after, o => o.kind === JS.Kind.ExportSpecifier)!.typeOnly.element).toBe(false);
    expect(expected).toBe("type A = number;\nexport { A};");
    expect(await TreePrinters.print(after)).toBe(expected);
});

test("a changed boolean inside a right padding is applied on receive", async () => {
    // given
    const text = "class C {\n    static {\n    }\n}";

    // when
    const {before, after, expected} = await roundTrip(text, cu => {
        const block = find<J.Block>(cu, o => o.kind === J.Kind.Block && o.static.element === true)!;
        return replace(cu, block.static, {...block.static, element: false});
    });

    // then
    expect(find<J.Block>(before, o => o.kind === J.Kind.Block && o.static.element === true)).toBeDefined();
    expect(find<J.Block>(after, o => o.kind === J.Kind.Block && o.static.element === true)).toBeUndefined();
    expect(expected).not.toContain("static");
    expect(await TreePrinters.print(after)).toBe(expected);
});
