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
namespace OpenRewrite.Core.Rpc;

public interface IRpcCodec
{
    void RpcSend(object after, RpcSendQueue q);
    object RpcReceive(object before, RpcReceiveQueue q);
}

public interface IRpcCodec<T> : IRpcCodec
{
    void RpcSend(T after, RpcSendQueue q);
    T RpcReceive(T before, RpcReceiveQueue q);

    void IRpcCodec.RpcSend(object after, RpcSendQueue q) => RpcSend((T)after, q);
    object IRpcCodec.RpcReceive(object before, RpcReceiveQueue q) => RpcReceive((T)before, q)!;
}
