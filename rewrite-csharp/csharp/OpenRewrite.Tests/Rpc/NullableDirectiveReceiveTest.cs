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

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Regression test for the `#nullable` directive RPC receive bug. The optional
/// <see cref="NullableTarget"/> enum (the target of e.g. <c>#nullable enable annotations</c>)
/// arrives over the wire as a System.Text.Json <see cref="JsonElement"/>. The receiver
/// previously fell through to a hard <c>(NullableTarget)jsonElement</c> cast that threw
/// <see cref="InvalidCastException"/>, aborting receipt of the entire CompilationUnit for
/// any source file containing a targeted <c>#nullable</c> directive (observed building
/// ClosedXML), leaving a partially-built tree that later surfaced as NPEs during recipe runs.
/// </summary>
public class NullableDirectiveReceiveTest
{
    [Theory]
    [InlineData("Annotations", NullableTarget.Annotations)]
    [InlineData("Warnings", NullableTarget.Warnings)]
    public void ReceivesJsonElementEncodedTarget(string wireValue, NullableTarget expected)
    {
        // before: `#nullable enable` (no explicit target)
        var before = new NullableDirective(Guid.NewGuid(), Space.Empty, Markers.Empty,
            NullableSetting.Enable, target: null);

        // Messages consumed in order: ConsumePreVisit reads id/prefix/markers, then
        // VisitNullableDirective reads setting/target/hashSpacing/trailingComment/keywordSpacing.
        // Only Target changes, arriving as a JsonElement — the real System.Text.Json wire shape.
        var noChange = new RpcObjectData { State = RpcObjectData.ObjectState.NO_CHANGE };
        var data = new List<RpcObjectData>
        {
            noChange, // id
            noChange, // prefix
            noChange, // markers
            noChange, // setting
            new()
            {
                State = RpcObjectData.ObjectState.CHANGE,
                Value = JsonSerializer.SerializeToElement(wireValue, RpcJson.Options),
            }, // target
            noChange, // hashSpacing
            noChange, // trailingComment
            noChange, // keywordSpacing
        };

        var queue = new RpcReceiveQueue(data, new Dictionary<int, object>(), sourceFileType: null);

        var result = (NullableDirective)new CSharpReceiver().Visit(before, queue)!;

        Assert.Equal(expected, result.Target);
    }
}
