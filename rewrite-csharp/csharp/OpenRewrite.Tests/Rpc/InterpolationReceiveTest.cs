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
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Rpc;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// The space before the <c>,</c> and <c>:</c> of an interpolation (<c>{x ,5 :F2}</c>) used to be
/// left out of what is sent and reset on what is received, so it was lost from any tree that had
/// been to the other side and back.
/// </summary>
public class InterpolationReceiveTest
{
    [Fact]
    public void KeepsTheSpaceBeforeAlignmentAndFormat()
    {
        var before = new Interpolation(Guid.NewGuid(), Space.Empty, Markers.Empty,
            Name("x"),
            new JLeftPadded<Expression>(Space.SingleSpace, Name("width")),
            new JLeftPadded<Identifier>(Space.SingleSpace, Name("F2")),
            Space.Empty);

        // id, prefix, markers, expression, alignment's before, alignment, format's before, format
        var noChange = new RpcObjectData { State = RpcObjectData.ObjectState.NO_CHANGE };
        var data = Enumerable.Repeat(noChange, 8).ToList();
        var queue = new RpcReceiveQueue(data, new Dictionary<int, object>(), sourceFileType: null);

        var result = (Interpolation)new CSharpReceiver().Visit(before, queue)!;

        Assert.Equal(" ", result.Alignment!.Before.Whitespace);
        Assert.Equal(" ", result.Format!.Before.Whitespace);
    }

    private static Identifier Name(string name) =>
        new(Guid.NewGuid(), Space.Empty, Markers.Empty, [], name, null, null);
}
