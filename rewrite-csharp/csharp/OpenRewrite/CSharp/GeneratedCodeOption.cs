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
using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;

namespace OpenRewrite.CSharp;

/// <summary>
/// The <c>generated_code</c> option that .editorconfig sets for a source file, which overrides what
/// <see cref="GeneratedCode"/> infers from the file's name and header. Absent where no option is set.
/// </summary>
public sealed class GeneratedCodeOption(Guid id, bool generated)
    : Marker, IRpcCodec<GeneratedCodeOption>, IEquatable<GeneratedCodeOption>
{
    public Guid Id { get; } = id;
    public bool Generated { get; } = generated;

    public GeneratedCodeOption WithId(Guid id) => id == Id ? this : new(id, Generated);

    public GeneratedCodeOption WithGenerated(bool generated) => generated == Generated ? this : new(Id, generated);

    public void RpcSend(GeneratedCodeOption after, RpcSendQueue q)
    {
        q.GetAndSend(after, m => m.Id);
        q.GetAndSend(after, m => m.Generated);
    }

    public GeneratedCodeOption RpcReceive(GeneratedCodeOption before, RpcReceiveQueue q) => before
        .WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse))
        .WithGenerated(q.Receive(before.Generated));

    public bool Equals(GeneratedCodeOption? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as GeneratedCodeOption);
    public override int GetHashCode() => Id.GetHashCode();
}
