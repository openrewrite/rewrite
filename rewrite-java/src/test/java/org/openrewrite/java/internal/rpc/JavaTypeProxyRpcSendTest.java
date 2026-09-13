/*
 * Copyright 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.rpc.Reference;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@link JavaType.Class} that implements {@link RpcCodec} — the signal for a
 * body-less type proxy that resolves from a type table — must reach the wire
 * under its own class name and through its own compact codec. The stub's body
 * getters throw, so a structural send fails loudly.
 */
class JavaTypeProxyRpcSendTest {

    @Test
    void proxyClassSendsCompactAndKeepsDiscriminator() {
        StubProxy proxy = new StubProxy("com.example.Boundary");

        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1_000_000, batches::addLast, new IdentityHashMap<>(), null, false);
        sq.send(Reference.asRef(proxy), null, () -> new JavaTypeSender().visit(proxy, sq));
        sq.flush();

        List<RpcObjectData> all = new ArrayList<>();
        while (!batches.isEmpty()) {
            all.addAll(batches.removeFirst());
        }

        RpcObjectData add = all.get(0);
        assertThat(add.getState()).isEqualTo(RpcObjectData.State.ADD);
        // Discriminator preserved: the proxy's own class, not flattened to the
        // built-in org.openrewrite.java.tree.JavaType$Class the remote can't proxy.
        assertThat(add.getValueType()).isEqualTo(StubProxy.class.getName());
        // Compact wire: the only payload values are the FQN and the kind. If the
        // structural visitClass path had run it would have thrown reading a body
        // getter (and never reached here).
        List<Object> values = new ArrayList<>();
        for (RpcObjectData d : all) {
            if (d.getValue() != null) {
                values.add(d.getValue());
            }
        }
        assertThat(values).containsExactly("com.example.Boundary", JavaType.FullyQualified.Kind.Interface);
    }

    @Test
    void proxyClassRoundTripsPreservingIdentity() {
        StubProxy proxy = new StubProxy("com.example.Boundary");

        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1_000_000, batches::addLast, new IdentityHashMap<>(), null, false);
        sq.send(Reference.asRef(proxy), null, () -> new JavaTypeSender().visit(proxy, sq));
        sq.flush();

        List<RpcObjectData> all = new ArrayList<>();
        while (!batches.isEmpty()) {
            all.addAll(batches.removeFirst());
        }
        Deque<List<RpcObjectData>> drain = new ArrayDeque<>();
        drain.add(all);
        RpcReceiveQueue rq = new RpcReceiveQueue(new HashMap<>(), drain::removeFirst, null, null);
        JavaTypeReceiver receiver = new JavaTypeReceiver();

        JavaType received = rq.receive((JavaType) null, jt -> (JavaType) receiver.visit(jt, rq));

        // The discriminator survived, so the far side rebuilt the proxy type (via
        // its rpcReceive), preserving identity — NOT a desync'd plain Class.
        assertThat(received).isInstanceOf(StubProxy.class);
        JavaType.FullyQualified fq = (JavaType.FullyQualified) received;
        assertThat(fq.getFullyQualifiedName()).isEqualTo("com.example.Boundary");
        assertThat(fq.getKind()).isEqualTo(JavaType.FullyQualified.Kind.Interface);
    }

    /**
     * A minimal stand-in for moderne-cli's chain-bound {@code JavaTypeProxy.Class}:
     * a body-less {@link JavaType.Class} whose body getters throw (they would
     * resolve from a table at recipe time) and that serializes compactly.
     */
    static class StubProxy extends JavaType.Class implements RpcCodec<JavaType.Class> {
        StubProxy(String fqn) {
            super(null, 0L, fqn, Kind.Interface, null, null, null, null, null, null, null);
        }

        @Override
        public List<JavaType> getTypeParameters() {
            throw new AssertionError("body resolved during send (typeParameters)");
        }

        @Override
        public @Nullable FullyQualified getSupertype() {
            throw new AssertionError("body resolved during send (supertype)");
        }

        @Override
        public @Nullable FullyQualified getOwningClass() {
            throw new AssertionError("body resolved during send (owningClass)");
        }

        @Override
        public List<FullyQualified> getAnnotations() {
            throw new AssertionError("body resolved during send (annotations)");
        }

        @Override
        public List<FullyQualified> getInterfaces() {
            throw new AssertionError("body resolved during send (interfaces)");
        }

        @Override
        public List<Variable> getMembers() {
            throw new AssertionError("body resolved during send (members)");
        }

        @Override
        public List<Method> getMethods() {
            throw new AssertionError("body resolved during send (methods)");
        }

        @Override
        public void rpcSend(JavaType.Class after, RpcSendQueue q) {
            // Compact: identity only. Header getters are safe (no resolution).
            q.getAndSend(after, JavaType.Class::getFullyQualifiedName);
            q.getAndSend(after, JavaType.Class::getKind);
        }

        @Override
        public JavaType.Class rpcReceive(JavaType.Class before, RpcReceiveQueue q) {
            // Same compact wire order as rpcSend: [fqn, kind]. before is an
            // objenesis-built shell (ctor bypassed) — populate identity only; the
            // body stays unresolved (a real proxy resolves it from the chain).
            String fqn = q.receive(before.getFullyQualifiedName());
            Kind kind = q.receiveAndGet(before.getKind(), k -> Kind.valueOf(k.toString()));
            return before.unsafeSet(0L, kind, fqn, null, null, null, null, null, null, null);
        }
    }
}
