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

using OpenRewrite.Core.Rpc;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp.Rpc;

/// <summary>
/// Reconstructs a structured <see cref="CsDocComment"/> tree decomposed by
/// <see cref="CsDocCommentSender"/>. Field order must mirror the sender exactly so the queue
/// stays in sync.
/// </summary>
internal class CsDocCommentReceiver : CsDocCommentVisitor<RpcReceiveQueue>
{
    internal CsDocCommentReceiver(CSharpVisitor<RpcReceiveQueue> csharpVisitor) : base(csharpVisitor)
    {
    }

    public override CsDocComment VisitDocComment(CsDocComment.DocComment docComment, RpcReceiveQueue q) =>
        docComment
            .WithId(q.ReceiveAndGet<Guid, string>(docComment.Id, Guid.Parse))
            .WithMarkers(q.Receive(docComment.Markers)!)
            .WithBody(q.ReceiveList(docComment.Body, b => Visit(b, q)!)!)
            .WithSuffix(q.Receive(docComment.Suffix)!);

    public override CsDocComment VisitXmlElement(CsDocComment.XmlElement element, RpcReceiveQueue q) =>
        element
            .WithId(q.ReceiveAndGet<Guid, string>(element.Id, Guid.Parse))
            .WithMarkers(q.Receive(element.Markers)!)
            .WithName(q.Receive(element.Name)!)
            .WithAttributes(q.ReceiveList(element.Attributes, a => Visit(a, q)!)!)
            .WithSpaceBeforeClose(q.ReceiveList(element.SpaceBeforeClose, s => Visit(s, q)!)!)
            .WithContent(q.ReceiveList(element.Content, c => Visit(c, q)!)!)
            .WithClosingTagSpaceBeforeClose(q.ReceiveList(element.ClosingTagSpaceBeforeClose, s => Visit(s, q)!)!);

    public override CsDocComment VisitXmlEmptyElement(CsDocComment.XmlEmptyElement element, RpcReceiveQueue q) =>
        element
            .WithId(q.ReceiveAndGet<Guid, string>(element.Id, Guid.Parse))
            .WithMarkers(q.Receive(element.Markers)!)
            .WithName(q.Receive(element.Name)!)
            .WithAttributes(q.ReceiveList(element.Attributes, a => Visit(a, q)!)!)
            .WithSpaceBeforeSlashClose(q.ReceiveList(element.SpaceBeforeSlashClose, s => Visit(s, q)!)!);

    public override CsDocComment VisitXmlText(CsDocComment.XmlText text, RpcReceiveQueue q) =>
        text
            .WithId(q.ReceiveAndGet<Guid, string>(text.Id, Guid.Parse))
            .WithMarkers(q.Receive(text.Markers)!)
            .WithText(q.Receive(text.Text)!);

    public override CsDocComment VisitXmlAttribute(CsDocComment.XmlAttribute attribute, RpcReceiveQueue q) =>
        attribute
            .WithId(q.ReceiveAndGet<Guid, string>(attribute.Id, Guid.Parse))
            .WithMarkers(q.Receive(attribute.Markers)!)
            .WithName(q.Receive(attribute.Name)!)
            .WithSpaceBeforeEquals(q.ReceiveList(attribute.SpaceBeforeEquals, s => Visit(s, q)!))
            .WithValue(q.ReceiveList(attribute.Value, v => Visit(v, q)!));

    public override CsDocComment VisitXmlCrefAttribute(CsDocComment.XmlCrefAttribute attribute, RpcReceiveQueue q) =>
        attribute
            .WithId(q.ReceiveAndGet<Guid, string>(attribute.Id, Guid.Parse))
            .WithMarkers(q.Receive(attribute.Markers)!)
            .WithSpaceBeforeEquals(q.ReceiveList(attribute.SpaceBeforeEquals, s => Visit(s, q)!))
            .WithValue(q.ReceiveList(attribute.Value, v => Visit(v, q)!))
            .WithReference((J?)q.Receive(attribute.Reference, r => (J)CsharpVisitorVisit(r, q)!));

    public override CsDocComment VisitXmlNameAttribute(CsDocComment.XmlNameAttribute attribute, RpcReceiveQueue q) =>
        attribute
            .WithId(q.ReceiveAndGet<Guid, string>(attribute.Id, Guid.Parse))
            .WithMarkers(q.Receive(attribute.Markers)!)
            .WithSpaceBeforeEquals(q.ReceiveList(attribute.SpaceBeforeEquals, s => Visit(s, q)!))
            .WithValue(q.ReceiveList(attribute.Value, v => Visit(v, q)!))
            .WithParamName((J?)q.Receive(attribute.ParamName, r => (J)CsharpVisitorVisit(r, q)!));

    public override CsDocComment VisitLineBreak(CsDocComment.LineBreak lineBreak, RpcReceiveQueue q) =>
        lineBreak
            .WithId(q.ReceiveAndGet<Guid, string>(lineBreak.Id, Guid.Parse))
            .WithMargin(q.Receive(lineBreak.Margin)!)
            .WithMarkers(q.Receive(lineBreak.Markers)!);
}
