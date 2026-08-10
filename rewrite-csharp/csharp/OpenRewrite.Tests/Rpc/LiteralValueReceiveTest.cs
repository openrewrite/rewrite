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

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Regression test: <see cref="Literal.Value"/> is declared <c>object</c>, so a received value
/// arrives as a System.Text.Json <see cref="JsonElement"/> unless the receiver materializes the
/// scalar. Before the fix, every recipe pattern of the form <c>Literal {{ Value: string s }}</c>
/// silently stopped matching once the tree crossed the RPC boundary — unit tests (which parse
/// locally) passed while `mod run` found nothing. Observed as WPF0130/0131 finding no
/// <c>GetTemplateChild("PART_...")</c> lookups on a real repo.
/// </summary>
public class LiteralValueReceiveTest
{
    [Fact]
    public void StringLiteralValueMaterializesThroughJsonWire()
    {
        var before = new Literal(Guid.NewGuid(), Space.Empty, Markers.Empty,
            null, "\"before\"", null, JavaType.Primitive.Of(JavaType.PrimitiveKind.String));
        var after = before.WithValue("PART_EditableTextBox").WithValueSource("\"PART_EditableTextBox\"");

        var data = new List<RpcObjectData>();
        var sendQueue = new RpcSendQueue(1024, batch => data.AddRange(batch),
            new Dictionary<object, int>(ReferenceEqualityComparer.Instance), null, false);
        sendQueue.Send(after, before, () => new JavaSender().Visit(after, sendQueue));
        sendQueue.Flush();

        // Through the JSON wire format, so the value arrives the way a remote peer's message
        // actually deserializes — as a JsonElement, not a live CLR string.
        var wireData = JsonSerializer.Deserialize<List<RpcObjectData>>(
            JsonSerializer.Serialize(data, RpcJson.Options), RpcJson.Options)!;
        var receiveQueue = new RpcReceiveQueue(wireData, new Dictionary<int, object>(), null);
        var received = (Literal)receiveQueue.Receive<J>(before,
            t => new JavaReceiver().Visit(t, receiveQueue)!)!;

        var s = Assert.IsType<string>(received.Value);
        Assert.Equal("PART_EditableTextBox", s);
    }
}
