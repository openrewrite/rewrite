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
package org.openrewrite.python.marker;

import org.junit.jupiter.api.Test;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class PythonResolutionResultRpcTest {

    @Test
    void roundTripCyclicGraph() {
        ResolvedDependency a = new ResolvedDependency("a", "1.0.0", null, new ArrayList<>());
        ResolvedDependency b = new ResolvedDependency("b", "1.0.0", null, new ArrayList<>());
        a.getDependencies().add(b);
        b.getDependencies().add(a);

        PythonResolutionResult marker = new PythonResolutionResult(UUID.randomUUID(), "my-app", "1.0.0",
                null, null, "pyproject.toml", null, null,
                emptyList(),
                singletonList(new PythonResolutionResult.Dependency("a", "==1.0.0", null, null, a)),
                emptyMap(), emptyMap(), emptyList(), emptyList(),
                asList(a, b),
                null, null);

        ResolvedDependency received = sendAndReceive(marker).getDependencies().get(0).getResolved();
        assertThat(received).isNotNull();

        ResolvedDependency receivedB = received.getDependencies().get(0);
        assertThat(receivedB.getName()).isEqualTo("b");

        ResolvedDependency backEdge = receivedB.getDependencies().get(0);
        assertThat(backEdge.getName())
                .as("the dependency closing the cycle must carry its own state, not a blank placeholder")
                .isEqualTo("a");
        assertThat(backEdge)
                .as("the back-reference closing the cycle must resolve to the same instance")
                .isSameAs(received);
    }

    private static PythonResolutionResult sendAndReceive(PythonResolutionResult marker) {
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1_000_000, batches::addLast, new IdentityHashMap<>(), null, false);
        sq.send(marker, null, null);
        sq.flush();

        // The wire carries a UUID as a string, as the transport's JSON encoding would.
        List<RpcObjectData> all = new ArrayList<>();
        while (!batches.isEmpty()) {
            for (RpcObjectData data : batches.removeFirst()) {
                all.add(data.getValue() instanceof UUID ?
                        new RpcObjectData(data.getState(), data.getValueType(), data.getValue().toString(), data.getRef(), false) :
                        data);
            }
        }
        Deque<List<RpcObjectData>> drain = new ArrayDeque<>();
        drain.add(all);

        return new RpcReceiveQueue(new HashMap<>(), drain::removeFirst, null, null).receive(null);
    }
}
