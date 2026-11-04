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
/// Represents a file checksum. Mirrors org.openrewrite.Checksum.
/// </summary>
public sealed class Checksum(string algorithm, object? value) : IRpcCodec<Checksum>
{
    public string Algorithm { get; } = algorithm;
    public object? Value { get; } = value;

    public void RpcSend(Checksum after, RpcSendQueue q)
    {
        q.GetAndSend(after, c => c.Algorithm);
        q.GetAndSend(after, c => c.Value);
    }

    public Checksum RpcReceive(Checksum before, RpcReceiveQueue q)
    {
        var algorithm = q.Receive(before.Algorithm);
        var value = q.Receive<object?>(before.Value);
        return new Checksum(algorithm!, value);
    }
}
