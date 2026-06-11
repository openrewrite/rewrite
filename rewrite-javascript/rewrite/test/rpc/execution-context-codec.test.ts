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
import {ExecutionContext, RPC_SHARED_MESSAGE_PREFIX} from "../../src/execution";
import {DATA_TABLE_STORE} from "../../src/data-table";
import {ReferenceMap, RpcObjectState, RpcReceiveQueue, RpcSendQueue} from "../../src/rpc";

describe("ExecutionContext RPC codec", () => {

    async function roundTrip(after: ExecutionContext,
                             senderBefore?: ExecutionContext,
                             remoteBefore?: ExecutionContext): Promise<ExecutionContext> {
        const sq = new RpcSendQueue(new ReferenceMap(), undefined, false);
        const batch = await sq.generate(after, senderBefore);

        const rq = new RpcReceiveQueue(new Map(), undefined, async () => batch, undefined, false);
        const received = await rq.receive<ExecutionContext>(remoteBefore);
        // The codec must consume exactly the messages that were sent.
        expect((await rq.take()).state).toBe(RpcObjectState.END_OF_OBJECT);
        return received;
    }

    test("shared messages transfer, process-local ones do not", async () => {
        const ctx = new ExecutionContext();
        ctx.messages[RPC_SHARED_MESSAGE_PREFIX + "outputDir"] = "/tmp/data-tables";
        ctx.messages[RPC_SHARED_MESSAGE_PREFIX + "columns"] = ["a", "b"];
        ctx.messages["org.openrewrite.processLocal"] = "should-not-transfer";

        const received = await roundTrip(ctx);

        expect(received).not.toBe(ctx);
        expect(received.messages[RPC_SHARED_MESSAGE_PREFIX + "outputDir"]).toBe("/tmp/data-tables");
        expect(received.messages[RPC_SHARED_MESSAGE_PREFIX + "columns"]).toEqual(["a", "b"]);
        expect(received.messages["org.openrewrite.processLocal"]).toBeUndefined();
    });

    test("context without shared messages round-trips", async () => {
        const ctx = new ExecutionContext();
        ctx.messages["org.openrewrite.processLocal"] = "should-not-transfer";

        const received = await roundTrip(ctx);

        expect(Object.keys(received.messages)).toHaveLength(0);
    });

    test("re-receive preserves identity and process-local state", async () => {
        const ctx = new ExecutionContext();
        ctx.messages[RPC_SHARED_MESSAGE_PREFIX + "outputDir"] = "/tmp/v1";

        const received = await roundTrip(ctx);
        // Simulate process-local state installed on the receiving side, e.g. a
        // materialized data table store under a symbol key.
        received.messages[DATA_TABLE_STORE] = {marker: "local-store"};
        received.messages["receiverLocal"] = "kept";

        // A re-fetch sends a diff against a snapshot of what the remote knows.
        const snapshot = new ExecutionContext({...ctx.messages});
        ctx.messages[RPC_SHARED_MESSAGE_PREFIX + "outputDir"] = "/tmp/v2";

        const receivedAgain = await roundTrip(ctx, snapshot, received);

        expect(receivedAgain).toBe(received);
        expect(receivedAgain.messages[RPC_SHARED_MESSAGE_PREFIX + "outputDir"]).toBe("/tmp/v2");
        expect(receivedAgain.messages[DATA_TABLE_STORE]).toEqual({marker: "local-store"});
        expect(receivedAgain.messages["receiverLocal"]).toBe("kept");
    });
});
