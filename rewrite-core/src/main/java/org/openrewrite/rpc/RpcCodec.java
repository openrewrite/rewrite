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
package org.openrewrite.rpc;

import org.jspecify.annotations.Nullable;

/**
 * A codec decomposes a value into multiple RPC {@link RpcObjectData} events, and
 * on the receiving side reconstitutes the value from those events.
 *
 * @param <T> The type of the value being sent and received.
 */
public interface RpcCodec<T> {

    /**
     * When the value has been determined to have been changed, this method is called
     * to send the values that comprise it.
     *
     * @param after The value that has been either added or changed.
     * @param q     The send queue that is collecting {@link RpcObjectData} to send.
     */
    void rpcSend(T after, RpcSendQueue q);

    /**
     * When the value has been determined to have been changed, this method is called
     * to receive the values that comprise it.
     *
     * @param before The value that has been either added or changed. In the case where it is added,
     *               the before state will be non-null, but will be an initialized object with
     *               all null fields that are expecting to be populated by this method.
     * @param q      The queue that is receiving {@link RpcObjectData} from a remote.
     */
    T rpcReceive(T before, RpcReceiveQueue q);

    static <T> @Nullable RpcCodec<T> forInstance(T t, @Nullable String sourceFileType) {
        if (t instanceof RpcCodec) {
            //noinspection unchecked
            return (RpcCodec<T>) t;
        }
        return DynamicDispatchRpcCodec.getCodec(t, sourceFileType);
    }
}
