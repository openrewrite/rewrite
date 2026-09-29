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
import {ReferenceMap, RpcReceiveQueue, RpcSendQueue} from "../../src/rpc";
import {JavaScriptParser, JS, sourceFileCache} from "../../src/javascript";
import {emptySpace, J} from "../../src/java";

// Every getObject over the wire decodes its own page, so the receiver starts from fresh instances.
function wireCopy(batch: any[]): any[] {
    return JSON.parse(JSON.stringify(batch));
}

function forEachObject(o: any, visit: (o: any) => void, seen = new WeakSet<object>()): void {
    if (o === null || typeof o !== "object" || seen.has(o)) {
        return;
    }
    seen.add(o);
    if (Array.isArray(o)) {
        for (const e of o) {
            forEachObject(e, visit, seen);
        }
        return;
    }
    visit(o);
    for (const k of Object.keys(o)) {
        forEachObject(o[k], visit, seen);
    }
}

function emptySpaceInstances(tree: any): Set<J.Space> {
    const set = new Set<J.Space>();
    forEachObject(tree, o => {
        if (o.kind === J.Kind.Space && o.whitespace === "" && Array.isArray(o.comments) && o.comments.length === 0) {
            set.add(o as J.Space);
        }
    });
    return set;
}

test("empty spaces received over RPC collapse to the shared emptySpace singleton", async () => {
    // given a parsed tree whose every empty Space is a distinct object, the way a peer that did not
    // deduplicate them (e.g. a Java LST with per-node Space instances) sends them across
    const parser = new JavaScriptParser({sourceFileCache});
    const parsed = (await parser.parse({
        text: "const a = 1; const b = 2; function f(x) { return x + 1; }",
        sourcePath: "t.ts"
    }).next()).value as JS.CompilationUnit;
    forEachObject(parsed, o => {
        for (const k of Object.keys(o)) {
            const v = o[k];
            if (v && v.kind === J.Kind.Space && v.whitespace === "" && v.comments.length === 0) {
                o[k] = {kind: J.Kind.Space, comments: [], whitespace: ""};
            }
        }
    });
    expect(emptySpaceInstances(parsed).size).toBeGreaterThan(1);

    // when the tree round-trips through the RPC send/receive queues
    const batch = await new RpcSendQueue(new ReferenceMap(), JS.Kind.CompilationUnit, false).generate(parsed, undefined);
    const q = new RpcReceiveQueue(new Map(), JS.Kind.CompilationUnit, async () => wireCopy(batch), undefined, false);
    const received = await q.receive<JS.CompilationUnit>(undefined);

    // then every empty Space in the received tree is the one shared singleton, not a per-node object
    const receivedEmpties = emptySpaceInstances(received);
    expect(receivedEmpties.size).toBe(1);
    expect(receivedEmpties.has(emptySpace)).toBe(true);
});
