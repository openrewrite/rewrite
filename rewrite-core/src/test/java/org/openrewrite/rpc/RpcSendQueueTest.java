/*
 * Copyright 2025 the original author or authors.
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

import java.nio.file.AccessMode;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class RpcSendQueueTest {

    @Test
    void sendList() throws Exception {
        List<String> before = List.of("A", "B", "C", "D");
        List<String> after = List.of("A", "E", "F", "C");

        CountDownLatch latch = new CountDownLatch(1);
        RpcSendQueue q = new RpcSendQueue(10, t -> {
            assertThat(t).containsExactly(
              new RpcObjectData(RpcObjectData.State.CHANGE, null, null, null),
              new RpcObjectData(RpcObjectData.State.CHANGE, null, List.of(0, -1, -1, 2), null),
              new RpcObjectData(RpcObjectData.State.NO_CHANGE, null, null, null) /* A */,
              new RpcObjectData(RpcObjectData.State.ADD, null, "E", null),
              new RpcObjectData(RpcObjectData.State.ADD, null, "F", null),
              new RpcObjectData(RpcObjectData.State.NO_CHANGE, null, null, null) /* C */
            );
            latch.countDown();
        }, new IdentityHashMap<>());

        q.sendList(after, before, Function.identity(), null);
        q.flush();

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void sendEnum() throws Exception {
        List<AccessMode> before = List.of(AccessMode.READ);
        List<AccessMode> after = List.of(AccessMode.READ, AccessMode.WRITE);

        CountDownLatch latch = new CountDownLatch(1);
        RpcSendQueue q = new RpcSendQueue(10, t -> {
            assertThat(t).containsExactly(
              new RpcObjectData(RpcObjectData.State.CHANGE, null, null, null),
              new RpcObjectData(RpcObjectData.State.CHANGE, null, List.of(0, -1), null),
              new RpcObjectData(RpcObjectData.State.NO_CHANGE, null, null, null) /* READ */,
              new RpcObjectData(RpcObjectData.State.ADD, null, AccessMode.WRITE, null)
            );
            latch.countDown();
        }, new IdentityHashMap<>());

        q.sendList(after, before, Function.identity(), null);
        q.flush();

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void emptyList() throws Exception {
        List<String> after = List.of();

        CountDownLatch latch = new CountDownLatch(1);
        RpcSendQueue q = new RpcSendQueue(10, t -> {
            assertThat(t).containsExactly(
              new RpcObjectData(RpcObjectData.State.ADD, null, null, null),
              new RpcObjectData(RpcObjectData.State.CHANGE, null, List.of(), null)
            );
            latch.countDown();
        }, new IdentityHashMap<>());

        q.sendList(after, null, Function.identity(), null);
        q.flush();

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }
}
