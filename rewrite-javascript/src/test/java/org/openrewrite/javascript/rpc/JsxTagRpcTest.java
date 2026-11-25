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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.*;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.javascript.tree.JSX;
import org.openrewrite.marker.Markers;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.io.UncheckedIOException;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class JsxTagRpcTest {

    private static final String SOURCE_FILE_TYPE = JS.CompilationUnit.class.getName();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void methodTypeOfTheRenderedComponentSurvivesARoundTrip() {
        // given
        JavaType.FullyQualified module = JavaType.ShallowClass.build("src/Counter");
        JavaType.Method counter = new JavaType.Method(null, 0L, module, "Counter", JavaType.Unknown.getInstance(),
                singletonList("props"), singletonList(JavaType.Unknown.getInstance()), emptyList(), emptyList(), null, emptyList());
        JSX.Tag tag = tag(counter);

        // when
        JSX.Tag received = receive(send(tag));

        // then
        assertThat(received.getMethodType()).isNotNull();
        assertThat(received.getMethodType().getDeclaringType().getFullyQualifiedName()).isEqualTo("src/Counter");
        assertThat(received.getMethodType().getName()).isEqualTo("Counter");
        assertThat(received.getMethodType().getParameterNames()).containsExactly("props");
        assertThat(received.getOpenName()).isInstanceOf(J.Identifier.class);
        assertThat(received.getSelfClosing()).isEqualTo(Space.SINGLE_SPACE);
    }

    @Test
    void tagWithoutMethodTypeStaysWithout() {
        // given
        JSX.Tag tag = tag(null);

        // when
        JSX.Tag received = receive(send(tag));

        // then
        assertThat(received.getMethodType()).isNull();
        assertThat(received.getSelfClosing()).isEqualTo(Space.SINGLE_SPACE);
    }

    private static JSX.Tag tag(JavaType.@Nullable Method methodType) {
        J.Identifier name = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "Counter",
                methodType, null);
        return new JSX.Tag(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JLeftPadded.build(name), null, Space.EMPTY,
                emptyList(), Space.SINGLE_SPACE, null, null, null, methodType);
    }

    private static List<RpcObjectData> send(JSX.Tag tag) {
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1_000_000, batches::addLast, new IdentityHashMap<>(), SOURCE_FILE_TYPE, false);
        sq.send(tag, null, null);
        sq.flush();
        List<RpcObjectData> all = new ArrayList<>();
        batches.forEach(batch -> batch.forEach(data -> all.add(new RpcObjectData(data.getState(), data.getValueType(),
                overJson(data.getValue()), data.getRef(), false))));
        return all;
    }

    private static @Nullable Object overJson(@Nullable Object value) {
        try {
            return value == null ? null : MAPPER.readValue(MAPPER.writeValueAsString(value), Object.class);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JSX.Tag receive(List<RpcObjectData> wire) {
        Deque<List<RpcObjectData>> drain = new ArrayDeque<>();
        drain.add(wire);
        return new RpcReceiveQueue(new HashMap<>(), drain::removeFirst, SOURCE_FILE_TYPE, null).receive(null);
    }
}
