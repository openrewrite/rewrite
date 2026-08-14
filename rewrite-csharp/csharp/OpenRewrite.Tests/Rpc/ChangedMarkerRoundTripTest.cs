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
using Rewrite.Core.Rpc;

namespace OpenRewrite.Tests.Rpc;

public class ChangedMarkerRoundTripTest
{
    [Fact]
    public void ChangedMarkerWithSameIdRoundTrips()
    {
        var markerId = Guid.NewGuid();
        var beforeMarker = new RecipesThatMadeChanges(markerId,
            [[new RecipeThatMadeChanges("com.example.Before", "Before", null, null, null)]]);
        var afterMarker = new RecipesThatMadeChanges(markerId,
            [[new RecipeThatMadeChanges("com.example.After", "After", "After `x`",
                new Dictionary<string, object?> { ["opt"] = "x" }, 300000)]]);
        var before = new Markers(Guid.NewGuid(), [beforeMarker]);
        var after = before.WithMarkerList([afterMarker]);

        var data = new List<RpcObjectData>();
        var sendRefs = new Dictionary<object, int>(ReferenceEqualityComparer.Instance);
        var sendQueue = new RpcSendQueue(1024, batch => data.AddRange(batch), sendRefs, null, false);
        sendQueue.Send(after, before, null);
        sendQueue.Flush();

        // Going through JSON is what turns the sender's live values into what the receiver sees;
        // handing the messages over in memory would prove nothing.
        var wire = JsonSerializer.Deserialize<List<RpcObjectData>>(
            JsonSerializer.Serialize(data, RpcJson.Options), RpcJson.Options)!;

        var receiveQueue = new RpcReceiveQueue(wire, new Dictionary<int, object>(), null);
        var received = receiveQueue.Receive(before);

        // Hop 2 exercises this peer's own sender against what its receiver produced. Sending
        // against an empty before keeps it off the NO_CHANGE path.
        var secondData = new List<RpcObjectData>();
        var secondSend = new RpcSendQueue(1024, batch => secondData.AddRange(batch),
            new Dictionary<object, int>(ReferenceEqualityComparer.Instance), null, false);
        secondSend.Send(received, null, null);
        secondSend.Flush();
        Assert.Contains(secondData, d => (d.Value as string) == "com.example.After");

        var secondWire = JsonSerializer.Deserialize<List<RpcObjectData>>(
            JsonSerializer.Serialize(secondData, RpcJson.Options), RpcJson.Options)!;
        received = new RpcReceiveQueue(secondWire, new Dictionary<int, object>(), null).Receive<Markers>(null);

        var receivedMarker = received!.FindFirst<RecipesThatMadeChanges>();
        Assert.NotNull(receivedMarker);
        var identity = Assert.Single(Assert.Single(receivedMarker!.Recipes));
        Assert.Equal("com.example.After", identity.Name);
        Assert.Equal("After", identity.DisplayName);
        Assert.Equal("After `x`", identity.InstanceName);
        Assert.Equal("x", ((JsonElement)identity.Options!).GetProperty("opt").GetString());
        Assert.Equal(300000, identity.EstimatedEffortPerOccurrenceMillis);
    }
}
