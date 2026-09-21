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
import {JavaScriptParser, JavaScriptVisitor, JS, sourceFileCache} from "../../src/javascript";
import {J} from "../../src/java";
import {ReferenceMap, RpcReceiveQueue, RpcSendQueue} from "../../src/rpc";
import {TreePrinters} from "../../src/print";
import {create as produce} from "mutative";

const SOURCE = "const o = {a: 1};";

const parser = new JavaScriptParser({sourceFileCache});

class DropModifiers extends JavaScriptVisitor<undefined> {
    protected override async visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, p: undefined): Promise<J | undefined> {
        const visited = await super.visitPropertyAssignment(propertyAssignment, p) as JS.PropertyAssignment;
        return produce(visited, draft => {
            (draft as { modifiers?: J.Modifier[] }).modifiers = undefined;
        });
    }
}

test("a peer holding no modifiers list round-trips to an empty one", async () => {
    const parsed = (await parser.parse({text: SOURCE, sourcePath: "t.ts"}).next()).value as JS.CompilationUnit;
    const stale = await new DropModifiers().visit(parsed, undefined) as JS.CompilationUnit;

    const batch = await new RpcSendQueue(new ReferenceMap(), JS.Kind.CompilationUnit, false).generate(stale, undefined);
    const received = await new RpcReceiveQueue(new Map(), JS.Kind.CompilationUnit, async () => batch, undefined, false)
        .receive<JS.CompilationUnit>(undefined);

    const modifiers: (J.Modifier[] | undefined)[] = [];
    await new class extends JavaScriptVisitor<undefined> {
        protected override async visitPropertyAssignment(pa: JS.PropertyAssignment, p: undefined): Promise<J | undefined> {
            modifiers.push(pa.modifiers);
            return super.visitPropertyAssignment(pa, p);
        }
    }().visit(received, undefined);

    expect(modifiers).toEqual([[]]);
    expect(await TreePrinters.print(received)).toBe(SOURCE);
});
