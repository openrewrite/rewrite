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
package org.openrewrite.javascript.rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.marker.ResolvedNames;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ResolvedNamesRpcTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final ResolvedNames NAMES = new ResolvedNames(UUID.randomUUID(),
      List.of("lib/view.render", "path.join"),
      Arrays.asList(null, "@types/node"),
      List.of("method", "method-call"),
      new int[]{6, 10, 0, 0, 20, 24, 1, 1});

    @Test
    void everyFieldSurvivesTheTrip() throws Exception {
        assertThat(sendAndReceive(NAMES, null)).isEqualTo(NAMES);
    }

    @Test
    void noNamesAtAllSurviveTheTrip() throws Exception {
        ResolvedNames none = new ResolvedNames(UUID.randomUUID(), List.of(), List.of(), List.of(), new int[0]);

        assertThat(sendAndReceive(none, null)).isEqualTo(none);
    }

    @Test
    void aChangeIsReceivedOverTheMarkerItChanges() throws Exception {
        ResolvedNames changed = NAMES
          .withNames(List.of("lib/view.js"))
          .withPackages(Collections.singletonList(null))
          .withKinds(List.of("import-ref"))
          .withSpans(new int[]{0, 3, 0, 0});

        assertThat(sendAndReceive(changed, NAMES)).isEqualTo(changed);
    }

    private static ResolvedNames sendAndReceive(ResolvedNames after, @Nullable ResolvedNames before) throws Exception {
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1_000_000, batches::addLast, new IdentityHashMap<>(), null, false);
        sq.send(after, before, null);
        sq.flush();

        // The wire carries each value as JSON.
        List<RpcObjectData> all = new ArrayList<>();
        for (List<RpcObjectData> batch : batches) {
            for (RpcObjectData data : batch) {
                Object value = data.getValue();
                all.add(value == null ? data : new RpcObjectData(data.getState(), data.getValueType(),
                  JSON.readValue(JSON.writeValueAsString(value), Object.class), data.getRef(), false));
            }
        }
        Deque<List<RpcObjectData>> drain = new ArrayDeque<>();
        drain.add(all);

        return new RpcReceiveQueue(new HashMap<>(), drain::removeFirst, null, null).receive(before);
    }
}
