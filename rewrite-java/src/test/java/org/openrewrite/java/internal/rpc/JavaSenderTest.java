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
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class JavaSenderTest {
    private final String sourceFileType = J.CompilationUnit.class.getName();
    private final Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
    private final RpcSendQueue sq = new RpcSendQueue(1, batch -> batches.addLast(new ArrayList<>(batch)),
            new IdentityHashMap<>(), sourceFileType, false);
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
}
