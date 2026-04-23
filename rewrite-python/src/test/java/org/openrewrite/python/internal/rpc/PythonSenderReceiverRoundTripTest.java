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
package org.openrewrite.python.internal.rpc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.python.tree.Py;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PythonSenderReceiverRoundTripTest {

    private Deque<List<RpcObjectData>> batches;
    private RpcSendQueue sq;
    private RpcReceiveQueue rq;

    @BeforeEach
    void setUp() {
        batches = new ArrayDeque<>();
        String sourceFileType = Py.CompilationUnit.class.getName();
        IdentityHashMap<Object, Integer> localRefs = new IdentityHashMap<>();
        sq = new RpcSendQueue(1, e -> batches.addLast(encode(e)), localRefs, sourceFileType, false);
        rq = new RpcReceiveQueue(new HashMap<>(), batches::removeFirst, sourceFileType, null);
    }

    @Test
    void expressionStatementWithAwaitChangeRoundTrip() {
        // Reproduces the production failure where the Python receiver hits
        // "Expected positions array but got: RpcObjectData(NO_CHANGE)" inside
        // `_receive_space` → `space.comments` under a nested Py.Await.
        //
        // Root cause: Java's PythonSender.preVisit previously emitted
        // id/prefix/markers for every J node, including Py.ExpressionStatement
        // and Py.StatementExpression, even though their prefix/markers delegate
        // to the wrapped child and the receiver already skips them. When the
        // wrapped expression's prefix was CHANGE (not ADD), the extra Space
        // sub-messages (comments list header + whitespace) leaked into the
        // queue and eventually misaligned a nested receive_list.
        //
        // This test drives a round trip where the await's expression prefix
        // changes between `before` and `after`, which before the fix caused
        // a queue desynchronization visible as a wrong value in a later field.
        Py.ExpressionStatement before = expressionStatementOfAwait("x", Space.EMPTY);
        Py.ExpressionStatement after = expressionStatementOfAwait("x", Space.SINGLE_SPACE);

        // send the diff (CHANGE path)
        sq.send(after, before, null);
        sq.flush();

        Py.ExpressionStatement received = rq.receive(before);

        assertThat(received).isNotNull();
        assertThat(received.getId()).isEqualTo(after.getId());
        assertThat(received.getExpression()).isInstanceOf(Py.Await.class);
        Py.Await receivedAwait = (Py.Await) received.getExpression();
        assertThat(receivedAwait.getPrefix().getWhitespace())
                .as("await prefix whitespace must survive the CHANGE round trip")
                .isEqualTo(" ");
        assertThat(receivedAwait.getPrefix().getComments()).isEmpty();
    }

    @Test
    void expressionStatementWithAwaitAddRoundTrip() {
        // Sanity check: the first-time ADD path also works (it did before the fix
        // because the receiver could recover via the value_type on the header).
        Py.ExpressionStatement after = expressionStatementOfAwait("x", Space.EMPTY);

        sq.send(after, null, null);
        sq.flush();

        Py.ExpressionStatement received = rq.receive(null);

        assertThat(received).isNotNull();
        assertThat(received.getId()).isEqualTo(after.getId());
        assertThat(received.getExpression()).isInstanceOf(Py.Await.class);
    }

    private static Py.ExpressionStatement expressionStatementOfAwait(String name, Space awaitPrefix) {
        UUID esId = Tree.randomId();
        UUID awaitId = Tree.randomId();
        UUID identId = Tree.randomId();
        J.Identifier identifier = new J.Identifier(
                identId, Space.EMPTY, Markers.EMPTY,
                Collections.emptyList(), name, null, null
        );
        Py.Await await = new Py.Await(awaitId, awaitPrefix, Markers.EMPTY, identifier, (JavaType) null);
        return new Py.ExpressionStatement(esId, await);
    }

    private List<RpcObjectData> encode(List<RpcObjectData> batch) {
        List<RpcObjectData> encoded = new ArrayList<>();
        for (RpcObjectData data : batch) {
            if (data.getValue() instanceof UUID || data.getValue() instanceof Path) {
                encoded.add(new RpcObjectData(data.getState(), data.getValueType(),
                        data.getValue().toString(), data.getRef(), false));
            } else {
                encoded.add(data);
            }
        }
        return encoded;
    }
}
