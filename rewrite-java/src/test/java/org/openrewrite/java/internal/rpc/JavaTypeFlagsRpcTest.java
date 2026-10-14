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

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.rpc.Reference;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class JavaTypeFlagsRpcTest {

    private static final long CLASS_FLAGS = Flag.Public.getBitMask() | Flag.Final.getBitMask();
    // Bit 20 is not a Java flag. C# sets it on extension methods.
    private static final long NON_JAVA_BIT = 1L << 20;
    private static final long METHOD_FLAGS = Flag.Public.getBitMask() | Flag.Default.getBitMask() | NON_JAVA_BIT;
    private static final long VARIABLE_FLAGS = Flag.Private.getBitMask() | Flag.Static.getBitMask() | Flag.Final.getBitMask() | NON_JAVA_BIT;

    @Test
    void flagsOfClassMethodAndVariableRoundTrip() {
        JavaType.Class original = new JavaType.Class(null, CLASS_FLAGS, "com.example.Foo",
                JavaType.FullyQualified.Kind.Class, null, null, null, null, null, null, null);
        JavaType.Method method = new JavaType.Method(null, METHOD_FLAGS, original, "bar", JavaType.Primitive.Void,
                emptyList(), emptyList(), emptyList(), emptyList(), null, emptyList());
        // The constructor masks to Flag.VALID_FLAGS, so the non-Java bit goes in through unsafeSet.
        JavaType.Variable member = new JavaType.Variable(null, 0L, "baz", original, JavaType.Primitive.Int, null)
                .unsafeSet("baz", VARIABLE_FLAGS, original, JavaType.Primitive.Int, null);
        original.unsafeSet(emptyList(), null, null, emptyList(), emptyList(), List.of(member), List.of(method));

        JavaType.Class received = sendAndReceive(original);

        assertThat(received.getFlagsBitMap()).isEqualTo(CLASS_FLAGS);
        assertThat(received.getMethods().get(0).getFlagsBitMap()).isEqualTo(METHOD_FLAGS);
        assertThat(received.getMembers().get(0).getFlagsBitMap()).isEqualTo(VARIABLE_FLAGS);
    }

    private static JavaType.Class sendAndReceive(JavaType.Class original) {
        List<RpcObjectData> messages = new ArrayList<>();
        RpcSendQueue sq = new RpcSendQueue(1_000_000, messages::addAll, new IdentityHashMap<>(), null, false);
        sq.send(Reference.asRef(original), null, () -> new JavaTypeSender().visit(original, sq));
        sq.flush();

        RpcReceiveQueue rq = new RpcReceiveQueue(new HashMap<>(), () -> messages, null, null);
        JavaTypeReceiver receiver = new JavaTypeReceiver();
        return (JavaType.Class) rq.receive((JavaType) null, jt -> (JavaType) receiver.visit(jt, rq));
    }
}
