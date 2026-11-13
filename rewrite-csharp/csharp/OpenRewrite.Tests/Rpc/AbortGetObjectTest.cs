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
using OpenRewrite.CSharp;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// A transfer that fails on the receiving side used to leave the two sides disagreeing about
/// what had been received: the sender went on handing out the batches of the transfer that
/// failed, and counted the object and the refs it had sent as held by the receiver. The
/// receiver now tells the sender, and both roll the transfer back.
/// </summary>
public class AbortGetObjectTest : RpcRewriteTest
{
    public AbortGetObjectTest(RpcFixture fixture) : base(fixture) { }

    /// <summary>
    /// Long enough that a transfer in either direction takes several batches, so that the sender
    /// is still streaming when the receiver fails on the first.
    /// </summary>
    private static readonly string Source =
        "class C\n{\n" +
        string.Concat(Enumerable.Range(0, 400).Select(i => $"    void M{i}() {{ int x = {i}; }}\n")) +
        "}\n";

    [Fact]
    public void AReceiveThatFailsHereIsRolledBackOnBothSides()
    {
        var cu = new CSharpParser().Parse(Source, sourcePath: "C.cs");

        // Java sends back a tree that changed all over and holds a marker this side cannot decode
        Assert.ThrowsAny<Exception>(() => MarkerRoundTripTest.OnJava(cu, resend: false, poison: true));

        var returned = MarkerRoundTripTest.OnJava(cu, resend: false);
        Assert.Single(returned.Markers.MarkerList.OfType<SearchResult>());
    }

    [Fact]
    public void AReceiveThatFailsOnJavaIsRolledBackOnBothSides()
    {
        var cu = new CSharpParser().Parse(Source, sourcePath: "C.cs");

        // the first thing Java reads of the tree names a class it does not have
        var unreceivable = cu.WithMarkers(cu.Markers.Add(new UnknownMarker(Guid.NewGuid(), "org.openrewrite.NoSuchMarker")));
        Assert.ThrowsAny<Exception>(() => MarkerRoundTripTest.OnJava(unreceivable, resend: false));

        var returned = MarkerRoundTripTest.OnJava(cu, resend: false);
        Assert.Single(returned.Markers.MarkerList.OfType<SearchResult>());
    }
}
