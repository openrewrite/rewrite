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

using System.Text;
using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp.Rpc;

/// <summary>
/// The Java side models a C# XML documentation comment as a structured
/// <c>CsDocComment.DocComment</c> tree and decomposes it over RPC. The C# side has no
/// equivalent structured model — it stores doc comments as raw <see cref="XmlDocComment"/>
/// text — so this receiver consumes the decomposed tree and re-flattens it to the textual
/// form, exactly mirroring the Java <c>CsDocCommentPrinter</c>.
/// </summary>
internal static class CsDocCommentReceiver
{
    /// <summary>
    /// Consume a decomposed <c>CsDocComment.DocComment</c> (id, markers, body, suffix) and
    /// rebuild the raw <see cref="XmlDocComment"/>. The shell ADD message has already been
    /// consumed by the caller, so the queue is positioned at the first field (id).
    /// </summary>
    public static XmlDocComment ReceiveDocComment(RpcReceiveQueue q)
    {
        ConsumeId(q);
        q.Receive<Markers>(Markers.Empty);
        string body = ReceiveNodeList(q);
        string? suffix = q.Receive<string>(null);
        // The printer emits "///" then the body; XmlDocComment.Text is everything after the
        // leading "//", i.e. a single "/" followed by the printed body.
        return new XmlDocComment("/" + body, suffix ?? "", true);
    }

    private static void ConsumeId(RpcReceiveQueue q) => q.Receive<object?>(null);

    /// <summary>Concatenate the printed text of a (non-null) node list field.</summary>
    private static string ReceiveNodeList(RpcReceiveQueue q)
    {
        var nodes = q.ReceiveList<DocNode>(new List<DocNode>(), null);
        if (nodes == null)
        {
            return "";
        }
        var sb = new StringBuilder();
        foreach (var node in nodes)
        {
            sb.Append(node.Text);
        }
        return sb.ToString();
    }

    /// <summary>
    /// Receive a nullable node list field, returning whether it was non-null, its element
    /// count, and its concatenated text. The list is always consumed to keep the queue in sync.
    /// </summary>
    private static (bool present, int count, string text) ReceiveNodeListNullable(RpcReceiveQueue q)
    {
        var nodes = q.ReceiveList<DocNode>(new List<DocNode>(), null);
        if (nodes == null)
        {
            return (false, 0, "");
        }
        var sb = new StringBuilder();
        foreach (var node in nodes)
        {
            sb.Append(node.Text);
        }
        return (true, nodes.Count, sb.ToString());
    }

    /// <summary>Replicates the printer's attribute body: "name[ space[=value]]".</summary>
    private static string ReceiveAttributeBody(RpcReceiveQueue q, string name)
    {
        var spaceBeforeEquals = ReceiveNodeListNullable(q);
        var value = ReceiveNodeListNullable(q);
        if (spaceBeforeEquals.present && spaceBeforeEquals.count > 0)
        {
            string equalsAndValue = value.present ? "=" + value.text : "";
            return name + spaceBeforeEquals.text + equalsAndValue;
        }
        return name;
    }

    // ---- Sentinel node types: not part of the C# LST, used only to drain the decomposed
    //      tree and rebuild text. Each implements IRpcCodec so the receive queue dispatches
    //      to RpcReceive. They are intentionally NOT Tree types. ----

    internal abstract class DocNode : IRpcCodec<DocNode>
    {
        public string Text = "";

        public void RpcSend(DocNode after, RpcSendQueue q) =>
            throw new NotSupportedException("C# never sends structured doc comments");

        public abstract DocNode RpcReceive(DocNode before, RpcReceiveQueue q);
    }

    internal sealed class DocXmlText : DocNode
    {
        public override DocNode RpcReceive(DocNode before, RpcReceiveQueue q)
        {
            ConsumeId(q);
            q.Receive<Markers>(Markers.Empty);
            Text = q.Receive<string>(null) ?? "";
            return this;
        }
    }

    internal sealed class DocLineBreak : DocNode
    {
        public override DocNode RpcReceive(DocNode before, RpcReceiveQueue q)
        {
            // Field order on the Java side is id, margin, markers.
            ConsumeId(q);
            Text = q.Receive<string>(null) ?? "";
            q.Receive<Markers>(Markers.Empty);
            return this;
        }
    }

    internal sealed class DocXmlElement : DocNode
    {
        public override DocNode RpcReceive(DocNode before, RpcReceiveQueue q)
        {
            ConsumeId(q);
            q.Receive<Markers>(Markers.Empty);
            string name = q.Receive<string>(null) ?? "";
            string attributes = ReceiveNodeList(q);
            string spaceBeforeClose = ReceiveNodeList(q);
            string content = ReceiveNodeList(q);
            string closingTagSpaceBeforeClose = ReceiveNodeList(q);
            Text = "<" + name + attributes + spaceBeforeClose + ">" +
                   content +
                   "</" + name + closingTagSpaceBeforeClose + ">";
            return this;
        }
    }

    internal sealed class DocXmlEmptyElement : DocNode
    {
        public override DocNode RpcReceive(DocNode before, RpcReceiveQueue q)
        {
            ConsumeId(q);
            q.Receive<Markers>(Markers.Empty);
            string name = q.Receive<string>(null) ?? "";
            string attributes = ReceiveNodeList(q);
            string spaceBeforeSlashClose = ReceiveNodeList(q);
            Text = "<" + name + attributes + spaceBeforeSlashClose + "/>";
            return this;
        }
    }

    internal sealed class DocXmlAttribute : DocNode
    {
        public override DocNode RpcReceive(DocNode before, RpcReceiveQueue q)
        {
            ConsumeId(q);
            q.Receive<Markers>(Markers.Empty);
            string name = q.Receive<string>(null) ?? "";
            Text = ReceiveAttributeBody(q, name);
            return this;
        }
    }

    internal sealed class DocXmlCrefAttribute : DocNode
    {
        public override DocNode RpcReceive(DocNode before, RpcReceiveQueue q)
        {
            ConsumeId(q);
            q.Receive<Markers>(Markers.Empty);
            Text = ReceiveAttributeBody(q, "cref");
            // Consume the optional type-attributed J reference.
            q.Receive<J?>(null);
            return this;
        }
    }

    internal sealed class DocXmlNameAttribute : DocNode
    {
        public override DocNode RpcReceive(DocNode before, RpcReceiveQueue q)
        {
            ConsumeId(q);
            q.Receive<Markers>(Markers.Empty);
            Text = ReceiveAttributeBody(q, "name");
            // Consume the optional bound parameter/type-parameter reference.
            q.Receive<J?>(null);
            return this;
        }
    }
}

/// <summary>
/// Sentinel <see cref="Comment"/> subtype used purely so the receive queue can instantiate a
/// shell when it encounters a decomposed <c>CsDocComment.DocComment</c>. It never appears in a
/// finished LST — <see cref="CsDocCommentReceiver.ReceiveDocComment"/> replaces it with an
/// <see cref="XmlDocComment"/>.
/// </summary>
internal sealed class StructuredDocComment() : Comment("", "", true);
