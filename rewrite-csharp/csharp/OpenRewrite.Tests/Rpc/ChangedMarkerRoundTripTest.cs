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
using Rewrite.Core.Rpc;

namespace OpenRewrite.Tests.Rpc;

public class ChangedMarkerRoundTripTest
{
    /// <summary>
    /// A codec-less marker (e.g. RecipesThatMadeChanges) that changes but keeps its id
    /// diffs as CHANGE in the markers list rather than ADD.
    /// </summary>
    [Fact]
    public void ChangedMarkerWithSameIdRoundTrips()
    {
        var markerId = Guid.NewGuid();
        var beforeMarker = new RecipesThatMadeChanges { Id = markerId, Recipes = "before" };
        var afterMarker = new RecipesThatMadeChanges { Id = markerId, Recipes = "after" };
        var before = new Markers(Guid.NewGuid(), [beforeMarker]);
        var after = before.WithMarkerList([afterMarker]);

        var data = new List<RpcObjectData>();
        var sendRefs = new Dictionary<object, int>(ReferenceEqualityComparer.Instance);
        var sendQueue = new RpcSendQueue(1024, batch => data.AddRange(batch), sendRefs, null, false);
        sendQueue.Send(after, before, null);
        sendQueue.Flush();

        var receiveQueue = new RpcReceiveQueue(data, new Dictionary<int, object>(), null);
        var received = receiveQueue.Receive(before);

        var receivedMarker = received!.FindFirst<RecipesThatMadeChanges>();
        Assert.NotNull(receivedMarker);
        Assert.Equal("after", receivedMarker!.Recipes);
    }
}
