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
using OpenRewrite.Core.Rpc;

namespace OpenRewrite.Core;

/// <summary>
/// Marker indicating that a member access uses the pointer dereference operator (<c>-&gt;</c>)
/// instead of the dot operator (<c>.</c>).
/// Applied to <c>PointerDereference</c> markers when the expression uses <c>-&gt;</c>.
/// </summary>
public sealed class PointerMemberAccess(Guid id) : Marker, IRpcCodec<PointerMemberAccess>
{
    public Guid Id { get; } = id;

    public PointerMemberAccess WithId(Guid id) =>
        id == Id ? this : new(id);

    public static PointerMemberAccess Instance { get; } = new(Guid.Empty);

    public void RpcSend(PointerMemberAccess after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public PointerMemberAccess RpcReceive(PointerMemberAccess before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));
}
