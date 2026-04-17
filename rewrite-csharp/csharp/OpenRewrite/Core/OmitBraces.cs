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
/// Marker on a Block indicating that braces should not be printed.
/// Used for single-statement bodies (lock, using, etc.) where the original
/// source had no braces but the AST wraps the statement in a Block.
/// </summary>
public sealed class OmitBraces(Guid id) : Marker, IRpcCodec<OmitBraces>
{
    public Guid Id { get; } = id;

    public OmitBraces WithId(Guid id) =>
        id == Id ? this : new(id);

    public static OmitBraces Instance { get; } = new(Guid.Empty);

    public void RpcSend(OmitBraces after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public OmitBraces RpcReceive(OmitBraces before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));
}
