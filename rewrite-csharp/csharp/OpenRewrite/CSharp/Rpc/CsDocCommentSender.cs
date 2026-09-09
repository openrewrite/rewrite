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

namespace OpenRewrite.CSharp.Rpc;

/// <summary>
/// Decomposes a structured <see cref="CsDocComment"/> tree over RPC, mirroring the Java
/// <c>CsDocCommentSender</c>. Field order must match <see cref="CsDocCommentReceiver"/> and the
/// Java sender/receiver exactly so the queue stays in sync.
/// </summary>
internal class CsDocCommentSender : CsDocCommentVisitor<RpcSendQueue>
{
    internal CsDocCommentSender(CSharpVisitor<RpcSendQueue> csharpVisitor) : base(csharpVisitor)
    {
    }

    public override CsDocComment VisitDocComment(CsDocComment.DocComment docComment, RpcSendQueue q)
    {
        q.GetAndSend(docComment, d => d.Id);
        q.GetAndSend(docComment, d => d.Markers);
        q.GetAndSendList(docComment, d => d.Body, b => (object)b.Id, b => Visit(b, q));
        q.GetAndSend(docComment, d => d.Suffix);
        return docComment;
    }

    public override CsDocComment VisitXmlElement(CsDocComment.XmlElement element, RpcSendQueue q)
    {
        q.GetAndSend(element, e => e.Id);
        q.GetAndSend(element, e => e.Markers);
        q.GetAndSend(element, e => e.Name);
        q.GetAndSendList(element, e => e.Attributes, a => (object)a.Id, a => Visit(a, q));
        q.GetAndSendList(element, e => e.SpaceBeforeClose, s => (object)s.Id, s => Visit(s, q));
        q.GetAndSendList(element, e => e.Content, c => (object)c.Id, c => Visit(c, q));
        q.GetAndSendList(element, e => e.ClosingTagSpaceBeforeClose, s => (object)s.Id, s => Visit(s, q));
        return element;
    }

    public override CsDocComment VisitXmlEmptyElement(CsDocComment.XmlEmptyElement element, RpcSendQueue q)
    {
        q.GetAndSend(element, e => e.Id);
        q.GetAndSend(element, e => e.Markers);
        q.GetAndSend(element, e => e.Name);
        q.GetAndSendList(element, e => e.Attributes, a => (object)a.Id, a => Visit(a, q));
        q.GetAndSendList(element, e => e.SpaceBeforeSlashClose, s => (object)s.Id, s => Visit(s, q));
        return element;
    }

    public override CsDocComment VisitXmlText(CsDocComment.XmlText text, RpcSendQueue q)
    {
        q.GetAndSend(text, t => t.Id);
        q.GetAndSend(text, t => t.Markers);
        q.GetAndSend(text, t => t.Text);
        return text;
    }

    public override CsDocComment VisitXmlAttribute(CsDocComment.XmlAttribute attribute, RpcSendQueue q)
    {
        q.GetAndSend(attribute, a => a.Id);
        q.GetAndSend(attribute, a => a.Markers);
        q.GetAndSend(attribute, a => a.Name);
        q.GetAndSendList(attribute, a => a.SpaceBeforeEquals, s => (object)s.Id, s => Visit(s, q));
        q.GetAndSendList(attribute, a => a.Value, v => (object)v.Id, v => Visit(v, q));
        return attribute;
    }

    public override CsDocComment VisitXmlCrefAttribute(CsDocComment.XmlCrefAttribute attribute, RpcSendQueue q)
    {
        q.GetAndSend(attribute, a => a.Id);
        q.GetAndSend(attribute, a => a.Markers);
        q.GetAndSendList(attribute, a => a.SpaceBeforeEquals, s => (object)s.Id, s => Visit(s, q));
        q.GetAndSendList(attribute, a => a.Value, v => (object)v.Id, v => Visit(v, q));
        q.GetAndSend(attribute, a => a.Reference, r => CsharpVisitorVisit(r, q));
        return attribute;
    }

    public override CsDocComment VisitXmlNameAttribute(CsDocComment.XmlNameAttribute attribute, RpcSendQueue q)
    {
        q.GetAndSend(attribute, a => a.Id);
        q.GetAndSend(attribute, a => a.Markers);
        q.GetAndSendList(attribute, a => a.SpaceBeforeEquals, s => (object)s.Id, s => Visit(s, q));
        q.GetAndSendList(attribute, a => a.Value, v => (object)v.Id, v => Visit(v, q));
        q.GetAndSend(attribute, a => a.ParamName, r => CsharpVisitorVisit(r, q));
        return attribute;
    }

    public override CsDocComment VisitLineBreak(CsDocComment.LineBreak lineBreak, RpcSendQueue q)
    {
        q.GetAndSend(lineBreak, l => l.Id);
        q.GetAndSend(lineBreak, l => l.Margin);
        q.GetAndSend(lineBreak, l => l.Markers);
        return lineBreak;
    }
}
