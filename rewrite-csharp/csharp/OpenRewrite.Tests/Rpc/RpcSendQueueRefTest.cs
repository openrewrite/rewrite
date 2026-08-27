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
using Rewrite.Core.Rpc;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// A changed ref-deduplicated slot is re-added under a fresh ref, never CHANGEd
/// (see RpcSendQueue.Send for why).
/// </summary>
public class RpcSendQueueRefTest
{
    private sealed class Payload
    {
        public int Value { get; init; }
    }

    [Fact]
    public void ChangedRefSlotIsReAddedInsteadOfChanged()
    {
        var sent = new List<RpcObjectData>();
        var refs = new Dictionary<object, int>(ReferenceEqualityComparer.Instance);
        var q = new RpcSendQueue(100, batch => sent.AddRange(batch), refs, null, false);

        var t1 = new Payload { Value = 1 };
        var t2 = new Payload { Value = 2 };

        q.Send(Reference.AsRef(t1), null, null);
        q.Send(Reference.AsRef(t2), Reference.AsRef(t1), null);
        // A repeat of the same transition dedups against the ref registered by the re-add
        q.Send(Reference.AsRef(t2), Reference.AsRef(t1), null);
        q.Flush();

        Assert.Equal(3, sent.Count);
        Assert.All(sent, d => Assert.Equal(RpcObjectData.ObjectState.ADD, d.State));
        Assert.Equal(1, sent[0].Ref);
        Assert.Equal(2, sent[1].Ref);
        Assert.Equal(2, sent[2].Ref);
        Assert.Null(sent[2].Value);
        Assert.Null(sent[2].ValueType);
    }
}
