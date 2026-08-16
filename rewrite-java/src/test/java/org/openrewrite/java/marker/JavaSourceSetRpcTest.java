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
package org.openrewrite.java.marker;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.rpc.Reference;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.*;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class JavaSourceSetRpcTest {

    private static final String GAV = "com.example:example:1.0";

    @Test
    void roundTrip() {
        JavaSourceSet before = sourceSet();

        JavaSourceSet received = sendAndReceive(before);

        assertThat(received.getId()).isEqualTo(before.getId());
        assertThat(received.getName()).isEqualTo("main");
        assertThat(received.getClasspath()).extracting(JavaType.FullyQualified::getFullyQualifiedName)
          .containsExactly("java.lang.String", "com.example.A", "com.example.B");
        assertThat(received.getGavToTypes()).containsOnlyKeys(GAV);
        assertThat(received.getGavToTypes().get(GAV)).extracting(JavaType.FullyQualified::getFullyQualifiedName)
          .containsExactly("com.example.A", "com.example.B");
        assertThat(received.getTypeFactory())
          .as("transient and never sent, so the receiver falls back to a fresh factory")
          .isNull();
    }

    @Test
    void gavBucketsAreRefsIntoTheClasspath() {
        JavaSourceSet received = sendAndReceive(sourceSet());

        // The whole point of decomposing gavToTypes rather than sending the map inline: its values
        // are the same instances that are already on the classpath, so the receiver must end up
        // with one type universe rather than two.
        List<JavaType.FullyQualified> bucket = received.getGavToTypes().get(GAV);
        assertThat(bucket.get(0)).isSameAs(received.getClasspath().get(1));
        assertThat(bucket.get(1)).isSameAs(received.getClasspath().get(2));
    }

    @Test
    void aRepeatedSendCollapsesToARefOnlyAdd() {
        JavaSourceSet sourceSet = sourceSet();
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1_000_000, batches::addLast, new IdentityHashMap<>(), null, false);

        sq.send(Reference.asRef(sourceSet), null, null);
        sq.flush();
        assertThat(batches.removeFirst()).hasSizeGreaterThan(1);

        // Markers travel asRef, so once the ref map survives eviction the second source file
        // carrying this same instance pays one message instead of the whole classpath again.
        sq.send(Reference.asRef(sourceSet), null, null);
        sq.flush();
        List<RpcObjectData> second = batches.removeFirst();
        assertThat(second).hasSize(1);
        assertThat(second.get(0).getRef()).isNotNull();
        assertThat(second.get(0).getValueType()).isNull();
        assertThat((Object) second.get(0).getValue()).isNull();
    }

    private static JavaSourceSet sourceSet() {
        JavaType.FullyQualified string = JavaType.ShallowClass.build("java.lang.String");
        JavaType.FullyQualified a = JavaType.ShallowClass.build("com.example.A");
        JavaType.FullyQualified b = JavaType.ShallowClass.build("com.example.B");
        Map<String, List<JavaType.FullyQualified>> gavToTypes = new LinkedHashMap<>();
        gavToTypes.put(GAV, asList(a, b));
        return new JavaSourceSet(UUID.randomUUID(), "main", asList(string, a, b), gavToTypes);
    }

    private static JavaSourceSet sendAndReceive(JavaSourceSet sourceSet) {
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1_000_000, batches::addLast, new IdentityHashMap<>(), null, false);
        sq.send(sourceSet, null, null);
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
