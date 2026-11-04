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
package org.openrewrite.java.internal.rpc;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.rpc.RpcObjectData.State.ADD;

class MarkersRefTest {

    @Test
    void emptyMarkersCrossTheWireOnceAndAreSharedOnReceipt() {
        String sourceFileType = J.CompilationUnit.class.getName();
        List<RpcObjectData> sent = new ArrayList<>();
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1, batch -> {
            sent.addAll(batch);
            batches.addLast(encode(batch));
        }, new IdentityHashMap<>(), sourceFileType, false);
        RpcReceiveQueue rq = new RpcReceiveQueue(new HashMap<>(), batches::removeFirst, sourceFileType, null);

        J.Identifier name = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "foo", null, null);
        J.MethodInvocation invocation = new J.MethodInvocation(randomId(), Space.EMPTY, Markers.EMPTY,
                null, null, name, JContainer.build(Space.EMPTY, emptyList(), Markers.EMPTY), null);

        sq.send(invocation, null, null);
        sq.flush();
        J.MethodInvocation received = rq.receive(null);

        List<RpcObjectData> full = sent.stream()
                .filter(d -> Markers.class.getName().equals(d.getValueType()))
                .toList();
        assertThat(full).hasSize(1);
        Integer ref = full.get(0).getRef();
        assertThat(ref).isNotNull();
        // The name and the argument container both cite the one Markers already sent.
        assertThat(sent.stream()
                .filter(d -> d.getState() == ADD && d.getValueType() == null && d.getValue() == null && ref.equals(d.getRef()))
                .count()).isEqualTo(2);

        assertThat(received.getName().getMarkers()).isSameAs(received.getMarkers());
    }

    private static List<RpcObjectData> encode(List<RpcObjectData> batch) {
        List<RpcObjectData> encoded = new ArrayList<>();
        for (RpcObjectData data : batch) {
            if (data.getValue() instanceof UUID || data.getValue() instanceof Path) {
                encoded.add(new RpcObjectData(data.getState(), data.getValueType(), data.getValue().toString(), data.getRef(), false));
            } else {
                encoded.add(data);
            }
        }
        return encoded;
    }
}
