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
import {ReferenceMap, RpcReceiveQueue, RpcSendQueue, StringInternTable} from "../../src/rpc";
import {JavaScriptParser, JS, sourceFileCache} from "../../src/javascript";

const QUEUES = 40;

// Every getObject over the wire decodes its own page, so the receiver starts from fresh string
// instances each time rather than the identity-shared ones an in-memory batch would hand out.
function wireCopy(batch: any[]): any[] {
    return JSON.parse(JSON.stringify(batch));
}

async function receive(batch: any[], table: StringInternTable): Promise<void> {
    const q = new RpcReceiveQueue(new Map(), JS.Kind.CompilationUnit, async () => wireCopy(batch),
        undefined, false, table);
    await q.receive<JS.CompilationUnit>(undefined);
}

test("one intern table deduplicates strings across many receive queues; per-queue tables do not", async () => {
    // given
    const source = "const r = zzUniqueInternProbe + zzUniqueInternProbe + zzUniqueInternProbe;";
    const parser = new JavaScriptParser({sourceFileCache});
    const parsed = (await parser.parse({text: source, sourcePath: "t.ts"}).next()).value as JS.CompilationUnit;
    const batch = await new RpcSendQueue(new ReferenceMap(), JS.Kind.CompilationUnit, false).generate(parsed, undefined);

    // when: one table receives the same tree over many queues (the fix — one table per connection)
    const shared = new StringInternTable();
    await receive(batch, shared);
    const afterFirst = shared.size;
    for (let i = 1; i < QUEUES; i++) {
        await receive(batch, shared);
    }

    // and: each queue interns into its own table (the pre-fix per-queue scope)
    let perQueueTotal = 0;
    for (let i = 0; i < QUEUES; i++) {
        const table = new StringInternTable();
        await receive(batch, table);
        perQueueTotal += table.size;
    }

    // then: the shared table holds one canonical set no matter how many queues drew from it, while
    // the per-queue tables each retained their own copy, so their entries scale with the queue count.
    expect(afterFirst).toBeGreaterThan(0);
    expect(shared.size).toBe(afterFirst);
    expect(perQueueTotal).toBe(afterFirst * QUEUES);
});
