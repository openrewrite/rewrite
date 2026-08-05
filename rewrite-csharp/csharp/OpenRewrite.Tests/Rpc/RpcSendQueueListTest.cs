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

namespace OpenRewrite.Tests.Rpc;

public class RpcSendQueueListTest
{
    [Fact]
    public void ListImplementationClassMayDiffer()
    {
        var before = new List<string> { "A", "B" };
        string[] after = ["A"];

        Assert.Equal(after, RoundTripList(after, before));
    }

    [Fact]
    public void ListImplementationClassMayDifferReverse()
    {
        string[] before = ["A"];
        var after = new List<string> { "A", "B" };

        Assert.Equal(after, RoundTripList(after, before));
    }

    private static IList<string>? RoundTripList(IList<string> after, IList<string> before)
    {
        var batches = new Queue<List<RpcObjectData>>();
        var sq = new RpcSendQueue(1, batches.Enqueue,
            new Dictionary<object, int>(ReferenceEqualityComparer.Instance), null, false);
        var rq = new RpcReceiveQueue(new Dictionary<int, object>(), batches.Dequeue, null);

        sq.SendList(after, before, x => x, null, false);
        sq.Flush();
        return rq.ReceiveList(before, (Func<string, string>?)null);
    }
}
