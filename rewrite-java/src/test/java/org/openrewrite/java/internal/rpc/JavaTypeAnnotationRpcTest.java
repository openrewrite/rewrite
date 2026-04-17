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
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.rpc.Reference;
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

import static org.assertj.core.api.Assertions.assertThat;

class JavaTypeAnnotationRpcTest {

    @Test
    void roundTripsAnnotationWithSingleConstantValue() {
        JavaType.Method element = methodOn("com.example.Foo", "value", JavaType.Primitive.Int);
        JavaType.Annotation original = annotation("com.example.Foo", List.of(
                new JavaType.Annotation.SingleElementValue(element, 42, null)));

        JavaType.Annotation roundTripped = sendAndReceive(original);

        assertThat(roundTripped.getType().getFullyQualifiedName()).isEqualTo("com.example.Foo");
        assertThat(roundTripped.getValues()).hasSize(1);
        JavaType.Annotation.SingleElementValue sev =
                (JavaType.Annotation.SingleElementValue) roundTripped.getValues().get(0);
        assertThat(sev.getConstantValue()).isEqualTo(42).isInstanceOf(Integer.class);
        assertThat(sev.getReferenceValue()).isNull();
        assertThat(((JavaType.Method) sev.getElement()).getName()).isEqualTo("value");
    }

    @Test
    void roundTripsAnnotationWithReferenceValue() {
        // e.g. @Foo(level = LogLevel.INFO) — referenceValue holds a Variable for the enum constant
        JavaType.Method element = methodOn("com.example.Foo", "level", JavaType.Primitive.Int);
        JavaType.Variable enumConstant = variableOn("INFO", "com.example.LogLevel");
        JavaType.Annotation original = annotation("com.example.Foo", List.of(
                new JavaType.Annotation.SingleElementValue(element, null, enumConstant)));

        JavaType.Annotation roundTripped = sendAndReceive(original);

        JavaType.Annotation.SingleElementValue sev =
                (JavaType.Annotation.SingleElementValue) roundTripped.getValues().get(0);
        assertThat(sev.getConstantValue()).isNull();
        JavaType.Variable receivedRef = (JavaType.Variable) sev.getReferenceValue();
        assertThat(receivedRef.getName()).isEqualTo("INFO");
    }

    @Test
    void roundTripsAnnotationWithArrayConstantValues() {
        // e.g. @SuppressWarnings({"a", "b"}) — array of strings
        JavaType.Method element = methodOn("java.lang.SuppressWarnings", "value", JavaType.Primitive.String);
        Object[] values = {"a", "b"};
        JavaType.Annotation original = annotation("java.lang.SuppressWarnings", List.of(
                new JavaType.Annotation.ArrayElementValue(element, values, null)));

        JavaType.Annotation roundTripped = sendAndReceive(original);

        JavaType.Annotation.ArrayElementValue aev =
                (JavaType.Annotation.ArrayElementValue) roundTripped.getValues().get(0);
        assertThat(aev.getConstantValues()).containsExactly("a", "b");
        assertThat(aev.getReferenceValues()).isNull();
    }

    @Test
    void roundTripsAnnotationWithArrayReferenceValues() {
        // e.g. @Foo(types = {String.class, Integer.class}) — array of class refs
        JavaType.Method element = methodOn("com.example.Foo", "types", JavaType.Primitive.Int);
        JavaType[] refs = {
                JavaType.ShallowClass.build("java.lang.String"),
                JavaType.ShallowClass.build("java.lang.Integer")};
        JavaType.Annotation original = annotation("com.example.Foo", List.of(
                new JavaType.Annotation.ArrayElementValue(element, null, refs)));

        JavaType.Annotation roundTripped = sendAndReceive(original);

        JavaType.Annotation.ArrayElementValue aev =
                (JavaType.Annotation.ArrayElementValue) roundTripped.getValues().get(0);
        assertThat(aev.getConstantValues()).isNull();
        assertThat(aev.getReferenceValues()).hasSize(2);
        assertThat(((JavaType.Class) aev.getReferenceValues()[0]).getFullyQualifiedName())
                .isEqualTo("java.lang.String");
    }

    @Test
    void roundTripsAnnotationWithMixedValues() {
        JavaType.Method intElement = methodOn("com.example.Foo", "intVal", JavaType.Primitive.Int);
        JavaType.Method longElement = methodOn("com.example.Foo", "longVal", JavaType.Primitive.Long);
        JavaType.Method arrayElement = methodOn("com.example.Foo", "arrVal", JavaType.Primitive.String);

        JavaType.Annotation original = annotation("com.example.Foo", List.of(
                new JavaType.Annotation.SingleElementValue(intElement, 42, null),
                new JavaType.Annotation.SingleElementValue(longElement, 42L, null),
                new JavaType.Annotation.ArrayElementValue(arrayElement, new Object[]{"x", "y"}, null)));

        JavaType.Annotation roundTripped = sendAndReceive(original);

        assertThat(roundTripped.getValues()).hasSize(3);
        JavaType.Annotation.SingleElementValue intSev =
                (JavaType.Annotation.SingleElementValue) roundTripped.getValues().get(0);
        JavaType.Annotation.SingleElementValue longSev =
                (JavaType.Annotation.SingleElementValue) roundTripped.getValues().get(1);
        JavaType.Annotation.ArrayElementValue strAev =
                (JavaType.Annotation.ArrayElementValue) roundTripped.getValues().get(2);

        // The whole point: int 42 stays Integer, long 42 stays Long.
        assertThat(intSev.getConstantValue()).isEqualTo(42).isInstanceOf(Integer.class);
        assertThat(longSev.getConstantValue()).isEqualTo(42L).isInstanceOf(Long.class);
        assertThat(strAev.getConstantValues()).containsExactly("x", "y");
    }

    @Test
    void roundTripsAnnotationWithoutValues() {
        // Marker annotation like @Override — values list is empty
        JavaType.Annotation original = annotation("java.lang.Override", Collections.emptyList());

        JavaType.Annotation roundTripped = sendAndReceive(original);

        assertThat(roundTripped.getType().getFullyQualifiedName()).isEqualTo("java.lang.Override");
        assertThat(roundTripped.getValues()).isEmpty();
    }

    private static JavaType.Annotation annotation(String typeFqn,
                                                  List<JavaType.Annotation.ElementValue> values) {
        return new JavaType.Annotation(JavaType.ShallowClass.build(typeFqn), values);
    }

    private static JavaType.Method methodOn(String declaringFqn, String name, JavaType returnType) {
        JavaType.Class declaring = (JavaType.Class) JavaType.ShallowClass.build(declaringFqn);
        List<String> noStrings = Collections.emptyList();
        List<JavaType> noTypes = Collections.emptyList();
        List<JavaType.FullyQualified> noFq = Collections.emptyList();
        return new JavaType.Method(null, 0L, declaring, name, returnType,
                noStrings, noTypes, noTypes, noFq, null, noStrings);
    }

    private static JavaType.Variable variableOn(String name, String ownerFqn) {
        JavaType.Class owner = (JavaType.Class) JavaType.ShallowClass.build(ownerFqn);
        return new JavaType.Variable(null, 0L, name, owner, owner, null);
    }

    /**
     * Run the given annotation through JavaTypeSender → wire batches → JavaTypeReceiver
     * and return the reconstructed value.
     */
    private static JavaType.Annotation sendAndReceive(JavaType.Annotation original) {
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        IdentityHashMap<Object, Integer> sendRefs = new IdentityHashMap<>();
        RpcSendQueue sq = new RpcSendQueue(1_000_000, batches::addLast, sendRefs, null, false);
        sq.send(Reference.asRef(original), null, () -> new JavaTypeSender().visit(original, sq));
        sq.flush();

        // Flatten batches into a single FIFO of messages.
        List<RpcObjectData> all = new ArrayList<>();
        while (!batches.isEmpty()) {
            all.addAll(batches.removeFirst());
        }
        // Wrap in a one-shot supplier returning each batch on demand. The receive queue
        // expects a Supplier<List<RpcObjectData>>; we just hand over the entire thing once.
        Deque<List<RpcObjectData>> drain = new ArrayDeque<>();
        drain.add(all);
        RpcReceiveQueue rq = new RpcReceiveQueue(new HashMap<>(), drain::removeFirst, null, null);

        JavaTypeReceiver receiver = new JavaTypeReceiver();
        return (JavaType.Annotation) rq.receive(
                (JavaType) null,
                jt -> (JavaType) receiver.visit(jt, rq));
    }
}
