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

public class SpaceRefTest
{
    [Fact]
    public void ShortCommentFreeSpaceIsSentOnceAsRefAndOthersByValue()
    {
        Space[] prefixes =
        [
            Space.Format(" "),
            Space.Format(" "),
            new(" ", [new TextComment(" c ", " ", true)]),
            Space.Format(new string(' ', 51))
        ];

        var data = new List<RpcObjectData>();
        var sendQueue = new RpcSendQueue(1024, batch => data.AddRange(batch), new RpcRefs(), null, false);
        foreach (var prefix in prefixes)
        {
            var ident = new Identifier(Guid.NewGuid(), prefix, Markers.Empty, [], "a", null, null);
            sendQueue.Send(ident, null, () => new JavaSender().Visit(ident, sendQueue));
        }
        sendQueue.Flush();

        Assert.Equal(2, data.Count(d => d.ValueType == "org.openrewrite.java.tree.Space" && d.Ref == null));
        Assert.Equal(1, data.Count(d => d.ValueType == "org.openrewrite.java.tree.Space" && d.Ref != null));

        var wireData = JsonSerializer.Deserialize<List<RpcObjectData>>(
            JsonSerializer.Serialize(data, RpcJson.Options), RpcJson.Options)!;
        var receiveQueue = new RpcReceiveQueue(wireData, new Dictionary<int, object>(), null);
        var received = prefixes
            .Select(_ => ((Identifier)receiveQueue.Receive<J>(null, t => new JavaReceiver().Visit(t, receiveQueue)!)!).Prefix)
            .ToList();
        Assert.Same(received[0], received[1]);
        Assert.Equal(" c ", Assert.IsType<TextComment>(Assert.Single(received[2].Comments)).Text);
    }
}
