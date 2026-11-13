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
using System.Text.Json;
using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;
using OpenRewrite.Java;
using OpenRewrite.Java.Rpc;
using Rewrite.Core.Rpc;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Every marker-free node shares <see cref="Markers.Empty"/>, so its Markers travel as a ref:
/// sent in full once per connection and cited by a one-message ADD afterwards.
/// </summary>
public class MarkersRefTest
{
    [Fact]
    public void EmptyMarkersCrossTheWireOnceAndAreSharedOnReceipt()
    {
        var first = new Identifier(Guid.NewGuid(), Space.Empty, Markers.Empty, [], "a", null, null);
        var second = new Identifier(Guid.NewGuid(), Space.Empty, Markers.Empty, [], "b", null, null);

        var data = new List<RpcObjectData>();
        var sendQueue = new RpcSendQueue(1024, batch => data.AddRange(batch), new RpcRefs(), null, false);
        sendQueue.Send(first, null, () => new JavaSender().Visit(first, sendQueue));
        sendQueue.Send(second, null, () => new JavaSender().Visit(second, sendQueue));
        sendQueue.Flush();

        var full = data.Where(d => d.ValueType == "org.openrewrite.marker.Markers").ToList();
        Assert.Single(full);
        var @ref = full[0].Ref;
        Assert.NotNull(@ref);
        Assert.Single(data, d => d.State == RpcObjectData.ObjectState.ADD && d.ValueType == null &&
                                  d.Value == null && d.Ref == @ref);

        var wireData = JsonSerializer.Deserialize<List<RpcObjectData>>(
            JsonSerializer.Serialize(data, RpcJson.Options), RpcJson.Options)!;
        var receiveQueue = new RpcReceiveQueue(wireData, new Dictionary<int, object>(), null);
        var receivedFirst = (Identifier)receiveQueue.Receive<J>(null, t => new JavaReceiver().Visit(t, receiveQueue)!)!;
        var receivedSecond = (Identifier)receiveQueue.Receive<J>(null, t => new JavaReceiver().Visit(t, receiveQueue)!)!;

        Assert.Same(receivedFirst.Markers, receivedSecond.Markers);
        Assert.Equal(Markers.Empty.Id, receivedSecond.Markers.Id);
    }
}
