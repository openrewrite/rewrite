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
using static OpenRewrite.Core.Rpc.RpcObjectData.ObjectState;

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

    /// <summary>
    /// The positions array is what lets a reorder cost one integer per element instead
    /// of re-sending the elements themselves.
    /// </summary>
    [Fact]
    public void ReorderedElementsAreRepositionedNotResent()
    {
        var batch = SendList(["C", "A", "B"], ["A", "B", "C"]);

        Assert.Equal([CHANGE, CHANGE, NO_CHANGE, NO_CHANGE, NO_CHANGE], batch.Select(d => d.State));
        Assert.Equal([2, 0, 1], (IEnumerable<int>)batch[1].Value!);
    }

    [Fact]
    public void EveryElementIsAddedWhenTheBeforeListIsEmpty()
    {
        var batch = SendList(["A", "B"], []);

        Assert.Equal([CHANGE, CHANGE, ADD, ADD], batch.Select(d => d.State));
        Assert.Equal([-1, -1], (IEnumerable<int>)batch[1].Value!);
        Assert.Equal("A", batch[2].Value);
        Assert.Equal("B", batch[3].Value);
    }

    [Fact]
    public void MixedAddsAndRemovals()
    {
        var batch = SendList(["A", "E", "F", "C"], ["A", "B", "C", "D"]);

        Assert.Equal([CHANGE, CHANGE, NO_CHANGE, ADD, ADD, NO_CHANGE], batch.Select(d => d.State));
        Assert.Equal([0, -1, -1, 2], (IEnumerable<int>)batch[1].Value!);
    }

    [Fact]
    public void UnchangedListIsNoChange()
    {
        var before = new List<string> { "A", "B" };

        Assert.Equal([NO_CHANGE], SendList(before, before).Select(d => d.State));
    }

    private static List<RpcObjectData> SendList(IList<string> after, IList<string> before)
    {
        var batch = new List<RpcObjectData>();
        var sq = new RpcSendQueue(1000, b => batch.AddRange(b),
            new Dictionary<object, int>(ReferenceEqualityComparer.Instance), null, false);

        sq.SendList(after, before, x => x, null, false);
        sq.Flush();
        return batch;
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
