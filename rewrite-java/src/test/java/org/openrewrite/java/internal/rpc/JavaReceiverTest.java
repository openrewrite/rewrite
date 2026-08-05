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
package org.openrewrite.java.internal.rpc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class JavaReceiverTest {

    private Deque<List<RpcObjectData>> batches;
    private RpcSendQueue sq;
    private RpcReceiveQueue rq;

    @BeforeEach
    void setUp() {
        batches = new ArrayDeque<>();
        String sourceFileType = J.CompilationUnit.class.getName();
        IdentityHashMap<Object, Integer> localRefs = new IdentityHashMap<>();
        sq = new RpcSendQueue(1, e -> batches.addLast(encode(e)), localRefs, sourceFileType, false);
        rq = new RpcReceiveQueue(new HashMap<>(), batches::removeFirst, sourceFileType, null);
    }

    @Test
    void methodInvocationRoundTripPreservesNameType() {
        // given
        JavaType.Method methodType = new JavaType.Method(
                null, 0, null, "foo", null,
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(),
                null, null
        );
        J.Identifier name = new J.Identifier(
                Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                Collections.emptyList(), "foo", methodType, null
        );
        J.MethodInvocation original = new J.MethodInvocation(
                Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                null, null, name,
                JContainer.build(Space.EMPTY, Collections.emptyList(), Markers.EMPTY),
                methodType
        );

        // when: send as ADD (from null) and receive — exercises the actual JavaSender/JavaReceiver
        sq.send(original, null, null);
        sq.flush();
        J.MethodInvocation received = rq.receive(null);

        // then
        assertThat(received.getMethodType()).isNotNull();
        assertThat(received.getName().getType())
                .as("Name identifier type must survive the sender/receiver round trip")
                .isNotNull();
    }

    @Test
    void changingOneSlotOfSharedTypeLeavesOtherSlotsUntouched() {
        // given: two arguments whose type attribution is one and the same JavaType instance
        JavaType.ShallowClass sharedType = JavaType.ShallowClass.build("java.util.List");
        J.Identifier a = new J.Identifier(
                Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                Collections.emptyList(), "a", sharedType, null
        );
        J.Identifier b = new J.Identifier(
                Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                Collections.emptyList(), "b", sharedType, null
        );
        J.MethodInvocation original = new J.MethodInvocation(
                Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                null, null,
                new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                        Collections.emptyList(), "foo", null, null),
                JContainer.build(Space.EMPTY,
                        new ArrayList<>(List.of(JRightPadded.build((Expression) a), JRightPadded.build((Expression) b))),
                        Markers.EMPTY),
                null
        );
        sq.send(original, null, null);
        sq.flush();
        J.MethodInvocation receivedBefore = rq.receive(null);

        // when: a recipe changes only argument `a`'s type to a different instance of the same class
        J.MethodInvocation after = original.withArguments(List.of(
                a.withType(JavaType.ShallowClass.build("java.util.ArrayList")), b));
        sq.send(after, original, null);
        sq.flush();
        J.MethodInvocation received = rq.receive(receivedBefore);

        // then
        assertThat(((JavaType.FullyQualified) received.getArguments().get(0).getType()).getFullyQualifiedName())
                .isEqualTo("java.util.ArrayList");
        assertThat(((JavaType.FullyQualified) received.getArguments().get(1).getType()).getFullyQualifiedName())
                .as("The unchanged slot aliases the receiver's cached instance, which must not be mutated")
                .isEqualTo("java.util.List");
    }

    private List<RpcObjectData> encode(List<RpcObjectData> batch) {
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
