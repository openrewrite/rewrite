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
import org.openrewrite.internal.ListUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TreeDataSendQueueTest {

    @Test
    void sendDifference() throws InterruptedException {
        List<String> before = List.of("A", "B", "C", "D");
        List<String> after = List.of("A", "E", "F", "C");
        Map<String, UUID> ids = ListUtils.concatAll(before, after).stream()
          .distinct()
          .collect(Collectors.toMap(s -> s, s -> UUID.randomUUID()));

        CountDownLatch latch = new CountDownLatch(1);
        RpcSendQueue q = new RpcSendQueue(10, t -> {
            assertThat(t.getData()).containsExactly(
              new TreeDatum(TreeDatum.State.CHANGE, null, List.of(0, -1, -1, 2), null),
              new TreeDatum(TreeDatum.State.NO_CHANGE, null, null, null) /* A */,
              new TreeDatum(TreeDatum.State.ADD, "string", ids.get("E"), null),
              new TreeDatum(TreeDatum.State.ADD, "string", ids.get("F"), null),
              new TreeDatum(TreeDatum.State.NO_CHANGE, null, null, null) /* C */
            );
            latch.countDown();
        }, new HashMap<>());

        q.send(after, before, () -> {
        });
        q.flush();

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }
}
