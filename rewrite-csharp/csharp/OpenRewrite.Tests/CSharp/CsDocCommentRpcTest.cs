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
using Xunit;

namespace OpenRewrite.Tests.CSharp;

/// <summary>
/// The structured <see cref="CsDocComment"/> tree must survive a full RPC decompose/reconstruct
/// cycle through <see cref="CSharpSender"/> and <see cref="CSharpReceiver"/> — the same path used
/// to ship it to/from the Java side — with byte-identical printed output.
/// </summary>
public class CsDocCommentRpcTest
{
    [Theory]
    [InlineData("/// <summary>doc</summary>")]
    [InlineData("/// <summary>\n/// Adds <paramref name=\"a\"/> to <c>b</c>.\n/// </summary>\n/// <param name=\"a\">first</param>")]
    public void RoundTripsThroughSenderAndReceiver(string fullText)
    {
        var original = CsDocCommentParser.ParseDocComment(fullText, "\n");
        var space = Space.Empty.WithComments(new List<Comment> { original });

        var batches = new Queue<List<RpcObjectData>>();
        var sendQueue = new RpcSendQueue(1, batch => batches.Enqueue(Encode(batch)),
            new Dictionary<object, int>(ReferenceEqualityComparer.Instance), null, false);
        new CSharpSender().VisitSpace(space, sendQueue);
        sendQueue.Flush();

        var receiveQueue = new RpcReceiveQueue(new Dictionary<int, object>(), () => batches.Dequeue(), null);
        var received = new CSharpReceiver().VisitSpace(Space.Empty, receiveQueue);

        var comment = Assert.Single(received.Comments);
        var roundTripped = Assert.IsType<CsDocComment.DocComment>(comment);
        Assert.Equal(original.Id, roundTripped.Id);
        Assert.Equal(Print(original), Print(roundTripped));
    }

    private static string Print(CsDocComment.DocComment doc)
    {
        var capture = new PrintOutputCapture<int>(0);
        new CsDocCommentPrinter<int>().Visit(doc, capture);
        return capture.ToString();
    }

    // Guids travel as strings across the real (JSON) transport; emulate that so the receiver's
    // Guid.Parse mappings see strings, exactly as they would over the wire.
    private static List<RpcObjectData> Encode(List<RpcObjectData> batch)
    {
        var encoded = new List<RpcObjectData>(batch.Count);
        foreach (var d in batch)
        {
            encoded.Add(d.Value is Guid g
                ? new RpcObjectData { State = d.State, ValueType = d.ValueType, Value = g.ToString(), Ref = d.Ref }
                : d);
        }
        return encoded;
    }
}
