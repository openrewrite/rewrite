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
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Rpc;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// An ordering with no direction keyword (<c>orderby x</c>) has a null direction. The receiver
/// used to take it for an ascending one, so the printer wrote an <c>ascending</c> that was never
/// in the source.
/// </summary>
public class OrderingReceiveTest
{
    private static readonly RpcObjectData NoChange = new() { State = RpcObjectData.ObjectState.NO_CHANGE };

    [Fact]
    public void KeepsAnOrderingWithoutDirection()
    {
        Assert.Null(Receive(NoChange).Direction);
    }

    [Theory]
    [InlineData("Ascending", DirectionKind.Ascending)]
    [InlineData("Descending", DirectionKind.Descending)]
    public void ReceivesDirection(string wireValue, DirectionKind expected)
    {
        var direction = new RpcObjectData
        {
            State = RpcObjectData.ObjectState.CHANGE,
            Value = JsonSerializer.SerializeToElement(wireValue, RpcJson.Options),
        };

        Assert.Equal(expected, Receive(direction).Direction);
    }

    private static Ordering Receive(RpcObjectData direction)
    {
        var before = new Ordering(Guid.NewGuid(), Space.Empty, Markers.Empty,
            new JRightPadded<Expression>(new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty), Space.Empty, Markers.Empty),
            direction: null);

        // id, prefix, markers, expression, direction
        var data = new List<RpcObjectData> { NoChange, NoChange, NoChange, NoChange, direction };
        var queue = new RpcReceiveQueue(data, new Dictionary<int, object>(), sourceFileType: null);

        return (Ordering)new CSharpReceiver().Visit(before, queue)!;
    }
}
