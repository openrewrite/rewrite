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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.text.PlainText;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class RpcReceiveQueueTest {

    private Deque<List<RpcObjectData>> batches;
    private RpcSendQueue sq;
    private RpcReceiveQueue rq;

    @BeforeEach
    void setUp() {
        batches = new ArrayDeque<>();
        IdentityHashMap<Object, Integer> localRefs = new IdentityHashMap<>();
        sq = new RpcSendQueue(1, e -> batches.addLast(encode(e)), localRefs, false);
        rq = new RpcReceiveQueue(new HashMap<>(), null, batches::removeFirst, refId ->
            // Find the object with this ref ID in the send queue's local refs
            localRefs.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(refId))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Reference " + refId + " not found in test")));
    }

    @Test
    void add() {
        PlainText before = PlainText.builder()
          .sourcePath(Paths.get("foo.txt"))
          .text("hello")
          .build();

        sq.send(before, null, null);
        assertThat(batches).hasSize(14);
        PlainText after = rq.receive(null);

        assertThat(after).isEqualTo(before);
        assertThat(after.getId()).isEqualTo(before.getId());
        assertThat(after.getSourcePath()).isEqualTo(before.getSourcePath());
    }

    @Test
    @SuppressWarnings("UnnecessaryLocalVariable")
    void noChange() {
        PlainText before = PlainText.builder()
          .sourcePath(Paths.get("foo.txt"))
          .text("hello")
          .build();
        PlainText noChange = before;

        sq.send(noChange, before, null);
        assertThat(batches).hasSize(1);
        PlainText after = rq.receive(noChange);

        assertThat(after).isEqualTo(noChange);
        assertThat(after.getId()).isEqualTo(noChange.getId());
    }

    @Test
    void changeId() {
        PlainText before = PlainText.builder()
          .sourcePath(Paths.get("foo.txt"))
          .text("hello")
          .build();
        PlainText newId = before.withId(Tree.randomId());

        sq.send(newId, before, null);
        assertThat(batches).hasSize(10);
        PlainText after = rq.receive(before);

        assertThat(after).isEqualTo(newId);
        assertThat(after.getId()).isEqualTo(newId.getId());
    }

    private List<RpcObjectData> encode(List<RpcObjectData> batch) {
        List<RpcObjectData> encoded = new ArrayList<>();
        for (RpcObjectData data : batch) {
            if (data.getValue() instanceof UUID || data.getValue() instanceof Path) {
                encoded.add(new RpcObjectData(data.getState(), data.getValueType(), data.getValue().toString(), data.getRef(), data.getTrace()));
            } else {
                encoded.add(data);
            }
        }
        return encoded;
    }
}
