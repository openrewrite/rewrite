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

import org.openrewrite.java.tree.JavaType;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

public class JavaTypeRpcCodec extends DynamicDispatchRpcCodec<JavaType> {

    private static final String JAVA_TYPE_PACKAGE = "org.openrewrite.java.tree";

    /**
     * {@code JavaType} is not a source file; it is a cross-language attribution model that
     * appears under every JVM-language source file type. There is therefore no meaningful
     * source-file key for it — it is registered under its own type name and resolved through
     * {@link DynamicDispatchRpcCodec#getCodec} by assignability rather than by key.
     */
    @Override
    public String getSourceFileType() {
        return JavaType.class.getName();
    }

    @Override
    public Class<? extends JavaType> getType() {
        return JavaType.class;
    }

    /**
     * Canonicalizes a (possibly subclassed/proxied) {@code JavaType} runtime type to the wire
     * name the remote can reconstruct. Unlike LST tree nodes — which implement their language
     * interface and sit directly on {@link Object} — {@code JavaType} is a real class hierarchy
     * (e.g. {@code ShallowClass extends Class extends FullyQualified}). So we must NOT use the
     * inherited "walk up while assignable to {@link #getType()}" default, which would collapse
     * {@code JavaType.Class} all the way to {@code FullyQualified}. Instead we strip only the
     * synthetic layers — classes outside the canonical {@code org.openrewrite.java.tree} package,
     * such as a proxy generated for lazy loading — landing on the first real {@code JavaType} class.
     * A value already in the canonical package is returned unchanged.
     */
    @Override
    public String valueType(Object value) {
        Class<?> type = value.getClass();
        while (isSynthetic(type) &&
               type.getSuperclass() != null &&
               JavaType.class.isAssignableFrom(type.getSuperclass())) {
            type = type.getSuperclass();
        }
        return type.getName();
    }

    private static boolean isSynthetic(Class<?> type) {
        Package pkg = type.getPackage();
        return pkg == null || !JAVA_TYPE_PACKAGE.equals(pkg.getName());
    }

    @Override
    public void rpcSend(JavaType after, RpcSendQueue q) {
        new JavaTypeSender().visit(after, q);
    }

    @Override
    public JavaType rpcReceive(JavaType before, RpcReceiveQueue q) {
        return new JavaTypeReceiver().visitNonNull(before, q);
    }
}
