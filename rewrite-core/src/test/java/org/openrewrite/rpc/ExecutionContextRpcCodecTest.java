/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.rpc;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.rpc.RpcObjectData.State.END_OF_OBJECT;

/**
 * Round-trip tests for the {@link ExecutionContext} RPC codec, which transfers
 * only messages under {@link ExecutionContext#RPC_SHARED_MESSAGE_PREFIX} between
 * RPC peers.
 */
class ExecutionContextRpcCodecTest {

    private final Deque<RpcObjectData> wire = new ArrayDeque<>();
    private final RpcSendQueue sendQueue = new RpcSendQueue(10, wire::addAll, new IdentityHashMap<>(), null, false);
    private final RpcReceiveQueue receiveQueue = new RpcReceiveQueue(new HashMap<>(), () -> {
        List<RpcObjectData> batch = new ArrayList<>(wire);
        wire.clear();
        return batch;
    }, null, null);

    /**
     * Mirrors how {@link org.openrewrite.rpc.request.GetObject.Handler} sends and
     * {@link RewriteRpc#getObject} receives a single object, including the
     * END_OF_OBJECT framing that detects send/receive message count mismatches.
     */
    private ExecutionContext roundTrip(ExecutionContext after, @org.jspecify.annotations.Nullable ExecutionContext senderBefore,
                                       @org.jspecify.annotations.Nullable ExecutionContext remoteBefore) {
        sendQueue.send(after, senderBefore, null);
        sendQueue.put(new RpcObjectData(END_OF_OBJECT, null, null, null, false));
        sendQueue.flush();

        ExecutionContext received = receiveQueue.receive(remoteBefore, null);
        assertThat(receiveQueue.take().getState())
          .as("codec must consume exactly the messages that were sent")
          .isEqualTo(END_OF_OBJECT);
        return received;
    }

    @Test
    void sharedMessagesTransfer() {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage(ExecutionContext.RPC_SHARED_MESSAGE_PREFIX + "outputDir", "/tmp/data-tables");
        ctx.putMessage(ExecutionContext.RPC_SHARED_MESSAGE_PREFIX + "columns", List.of("a", "b"));
        ctx.putMessage("org.openrewrite.processLocal", "should-not-transfer");

        ExecutionContext received = roundTrip(ctx, null, null);

        assertThat(received).isNotSameAs(ctx);
        assertThat(received.<String>getMessage(ExecutionContext.RPC_SHARED_MESSAGE_PREFIX + "outputDir"))
          .isEqualTo("/tmp/data-tables");
        assertThat(received.<List<String>>getMessage(ExecutionContext.RPC_SHARED_MESSAGE_PREFIX + "columns"))
          .containsExactly("a", "b");
        assertThat(received.<String>getMessage("org.openrewrite.processLocal")).isNull();
        assertThat(received.<Object>getMessage(ExecutionContext.RUN_TIMEOUT)).isNull();
    }

    @Test
    void noSharedMessages() {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage("org.openrewrite.processLocal", "should-not-transfer");

        ExecutionContext received = roundTrip(ctx, null, null);

        assertThat(received.getMessages()).isEmpty();
    }

    @Test
    void resendUpdatesAndRemovesSharedMessages() {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage(ExecutionContext.RPC_SHARED_MESSAGE_PREFIX + "outputDir", "/tmp/v1");
        ctx.putMessage(ExecutionContext.RPC_SHARED_MESSAGE_PREFIX + "removed", "gone-on-resend");

        ExecutionContext received = roundTrip(ctx, null, null);
        assertThat(received.<String>getMessage(ExecutionContext.RPC_SHARED_MESSAGE_PREFIX + "outputDir"))
          .isEqualTo("/tmp/v1");

        // Snapshot the state the remote knows, then mutate and re-send as a diff.
        InMemoryExecutionContext snapshot = ctx.clone();
        ctx.putMessage(ExecutionContext.RPC_SHARED_MESSAGE_PREFIX + "outputDir", "/tmp/v2");
        ctx.putMessage(ExecutionContext.RPC_SHARED_MESSAGE_PREFIX + "removed", null);

        ExecutionContext receivedAgain = roundTrip(ctx, snapshot, received);
        assertThat(receivedAgain).isSameAs(received);
        assertThat(receivedAgain.<String>getMessage(ExecutionContext.RPC_SHARED_MESSAGE_PREFIX + "outputDir"))
          .isEqualTo("/tmp/v2");
        assertThat(receivedAgain.<String>getMessage(ExecutionContext.RPC_SHARED_MESSAGE_PREFIX + "removed"))
          .isNull();
    }
}
