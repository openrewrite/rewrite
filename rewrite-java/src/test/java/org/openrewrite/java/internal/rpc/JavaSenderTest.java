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
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.*;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class JavaSenderTest {
    private final String sourceFileType = J.CompilationUnit.class.getName();
    private final List<RpcObjectData> sent = new ArrayList<>();
    private final Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
    private final RpcSendQueue sq = new RpcSendQueue(1, batch -> {
        sent.addAll(batch);
        // ids arrive as strings, as they do over the wire
        batches.addLast(batch.stream().map(d -> d.getValue() instanceof UUID ?
                new RpcObjectData(d.getState(), d.getValueType(), d.getValue().toString(), d.getRef(), false) : d).toList());
    }, new IdentityHashMap<>(), sourceFileType, false);
    private final RpcReceiveQueue rq = new RpcReceiveQueue(new HashMap<>(), batches::removeFirst, sourceFileType, null);

    @Test
    void changedSpaceInLeftPaddingIsSent() {
        JLeftPadded<Space> before = JLeftPadded.build(Space.EMPTY);
        JLeftPadded<Space> after = before.withElement(Space.SINGLE_SPACE);

        sq.send(after, before, () -> new JavaSender().visitLeftPadded(after, sq));
        sq.flush();
        JLeftPadded<Space> received = rq.receive(before, left -> new JavaReceiver().visitLeftPadded(left, rq));

        assertThat(received.getElement().getWhitespace()).isEqualTo(" ");
    }

    @Test
    void changedSpaceInRightPaddingIsSent() {
        JRightPadded<Space> before = JRightPadded.build(Space.EMPTY);
        JRightPadded<Space> after = before.withElement(Space.SINGLE_SPACE);

        sq.send(after, before, () -> new JavaSender().visitRightPadded(after, sq));
        sq.flush();
        JRightPadded<Space> received = rq.receive(before, right -> new JavaReceiver().visitRightPadded(right, rq));

        assertThat(received.getElement().getWhitespace()).isEqualTo(" ");
    }

    @Test
    void shortCommentFreeSpaceIsSentOnceAsRefAndOthersByValue() {
        J.MethodInvocation invocation = new J.MethodInvocation(randomId(), Space.EMPTY, Markers.EMPTY, null, null,
                identifier(Space.EMPTY, "f"),
                JContainer.build(Space.EMPTY, List.of(
                        JRightPadded.build(identifier(Space.format(" "), "a")),
                        JRightPadded.build(identifier(Space.format(" "), "b")),
                        JRightPadded.build(identifier(Space.format(" /* c */ "), "c")),
                        JRightPadded.build(identifier(Space.format(" ".repeat(51)), "d"))
                ), Markers.EMPTY), null);

        sq.send(invocation, null, null);
        sq.flush();
        J.MethodInvocation received = rq.receive(null);

        assertThat(sent.stream().filter(d -> Space.class.getName().equals(d.getValueType()) && d.getRef() == null))
                .as("the commented and the long Space go by value").hasSize(2);
        assertThat(sent.stream().filter(d -> Space.class.getName().equals(d.getValueType()) && d.getRef() != null))
                .as("each shared Space, EMPTY and \" \", is defined once").hasSize(2);
        List<Expression> args = received.getArguments();
        assertThat(args.get(0).getPrefix()).isSameAs(args.get(1).getPrefix());
        assertThat(args.get(2).getPrefix().getComments()).singleElement()
                .extracting(c -> ((TextComment) c).getText()).isEqualTo(" c ");
    }

    private static J.Identifier identifier(Space prefix, String name) {
        return new J.Identifier(randomId(), prefix, Markers.EMPTY, emptyList(), name, null, null);
    }
}
