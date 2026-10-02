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
import {emptyMarkers, Markers, MarkersKind} from "../../src/markers";
import {randomId} from "../../src/uuid";

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

function emptyMarkersInstances(tree: any): Set<Markers> {
    const set = new Set<Markers>();
    forEachObject(tree, o => {
        if (o.kind === MarkersKind.Markers && Array.isArray(o.markers) && o.markers.length === 0) {
            set.add(o as Markers);
        }
    });
    return set;
}

test("empty markers received over RPC collapse to the shared emptyMarkers singleton", async () => {
    // given a parsed tree whose every node carries its own distinct empty Markers, the way a peer
    // that did not deduplicate them (e.g. a Java LST with per-node marker ids) sends them across
    const parser = new JavaScriptParser({sourceFileCache});
    const parsed = (await parser.parse({
        text: "const a = 1; const b = 2; function f(x) { return x + 1; }",
        sourcePath: "t.ts"
    }).next()).value as JS.CompilationUnit;
    forEachObject(parsed, o => {
        if (o.markers && o.markers.kind === MarkersKind.Markers && o.markers.markers.length === 0) {
            o.markers = {kind: MarkersKind.Markers, id: randomId(), markers: []};
        }
    });
    expect(emptyMarkersInstances(parsed).size).toBeGreaterThan(1);

    // when the tree round-trips through the RPC send/receive queues
    const batch = await new RpcSendQueue(new ReferenceMap(), JS.Kind.CompilationUnit, false).generate(parsed, undefined);
    const q = new RpcReceiveQueue(new Map(), JS.Kind.CompilationUnit, async () => wireCopy(batch), undefined, false);
    const received = await q.receive<JS.CompilationUnit>(undefined);

    // then every empty Markers in the received tree is the one shared singleton, not a per-node object
    const receivedEmpties = emptyMarkersInstances(received);
    expect(receivedEmpties.size).toBe(1);
    expect(receivedEmpties.has(emptyMarkers)).toBe(true);
});
