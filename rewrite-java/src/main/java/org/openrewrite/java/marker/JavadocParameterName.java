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

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * Records a parameter name that appears after a parameter type in a Javadoc method reference,
 * e.g. the {@code str} in <code>{@literal @}see #bar(String str)</code>. Such names are not part
 * of the Javadoc reference grammar (only parameter types are), so the compiler drops them and they
 * are not modeled as part of the reference's {@link org.openrewrite.java.tree.J.MethodInvocation}
 * arguments. This marker keeps the name out of the surrounding whitespace {@link
 * org.openrewrite.java.tree.Space} (which must be whitespace-only) while still allowing the source
 * to be printed back verbatim. The marker is attached to the parameter type expression and re-emitted
 * by the Javadoc printer immediately after that expression.
 */
@Value
@With
public class JavadocParameterName implements Marker, RpcCodec<JavadocParameterName> {
    UUID id;

    /**
     * The parameter name as it appeared in the source. Whitespace separating the name from the
     * parameter type is retained in the parameter's trailing {@link
     * org.openrewrite.java.tree.Space}, not here.
     */
    String name;

    @Override
    public void rpcSend(JavadocParameterName after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, JavadocParameterName::getName);
    }

    @Override
    public JavadocParameterName rpcReceive(JavadocParameterName before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withName(q.receiveAndGet(before.getName(), (String s) -> s));
    }
}
