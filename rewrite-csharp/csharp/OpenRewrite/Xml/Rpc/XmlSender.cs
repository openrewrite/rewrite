/*
 * Copyright 2025 the original author or authors.
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

namespace OpenRewrite.Xml.Rpc;

public class XmlSender : XmlVisitor<RpcSendQueue>
{
    public override Xml? PreVisit(Xml x, RpcSendQueue q)
    {
        q.GetAndSend(x, n => n.Id);
        q.GetAndSend(x, n => n.Prefix);
        q.GetAndSend(x, n => n.Markers);
        return x;
    }

    public override Xml VisitDocument(Document document, RpcSendQueue q)
    {
        q.GetAndSend(document, d => d.SourcePath);
        q.GetAndSend(document, d => d.CharsetName);
        q.GetAndSend(document, d => d.CharsetBomMarked);
        q.GetAndSend(document, d => d.Checksum);
        q.GetAndSend(document, d => d.FileAttributes);
        q.GetAndSend(document, d => d.Prolog, p => Visit(p, q));
        q.GetAndSend(document, d => d.Root, t => Visit(t, q));
        q.GetAndSend(document, d => d.Eof);
        return document;
    }

    public override Xml VisitProlog(Prolog prolog, RpcSendQueue q)
    {
        q.GetAndSend(prolog, p => p.XmlDecl, d => Visit(d, q));
        q.GetAndSendList(prolog, p => p.MiscList, m => m.Id, m => Visit(m, q));
        q.GetAndSendList(prolog, p => p.JspDirectives, j => j.Id, j => Visit(j, q));
        return prolog;
    }

    public override Xml VisitXmlDecl(XmlDecl xmlDecl, RpcSendQueue q)
    {
        q.GetAndSend(xmlDecl, x => x.Name);
        q.GetAndSendList(xmlDecl, x => x.Attributes, a => a.Id, a => Visit(a, q));
        q.GetAndSend(xmlDecl, x => x.BeforeTagDelimiterPrefix);
        return xmlDecl;
    }

    public override Xml VisitProcessingInstruction(ProcessingInstruction pi, RpcSendQueue q)
    {
        q.GetAndSend(pi, p => p.Name);
        q.GetAndSend(pi, p => p.ProcessingInstructions, c => Visit(c, q));
        q.GetAndSend(pi, p => p.BeforeTagDelimiterPrefix);
        return pi;
    }

    public override Xml VisitTag(Tag tag, RpcSendQueue q)
    {
        q.GetAndSend(tag, t => t.Name);
        q.GetAndSendList(tag, t => t.Attributes, a => a.Id, a => Visit(a, q));
        q.GetAndSendList(tag, t => t.ContentList, c => c.Id, c => Visit(c, q));
        q.GetAndSend(tag, t => t.ClosingTag, c => Visit(c, q));
        q.GetAndSend(tag, t => t.BeforeTagDelimiterPrefix);
        return tag;
    }

    public override Xml VisitTagClosing(Tag.Closing closing, RpcSendQueue q)
    {
        q.GetAndSend(closing, c => c.Name);
        q.GetAndSend(closing, c => c.BeforeTagDelimiterPrefix);
        return closing;
    }

    public override Xml VisitAttribute(Attribute attribute, RpcSendQueue q)
    {
        q.GetAndSend(attribute, a => a.Key, k => Visit(k, q));
        q.GetAndSend(attribute, a => a.BeforeEquals);
        q.GetAndSend(attribute, a => a.Val, v => Visit(v, q));
        return attribute;
    }

    public override Xml VisitAttributeValue(Attribute.Value value, RpcSendQueue q)
    {
        q.GetAndSend(value, v => v.QuoteStyle.ToString());
        q.GetAndSend(value, v => v.Val);
        return value;
    }

    public override Xml VisitCharData(CharData charData, RpcSendQueue q)
    {
        q.GetAndSend(charData, c => c.Cdata);
        q.GetAndSend(charData, c => c.Text);
        q.GetAndSend(charData, c => c.AfterText);
        return charData;
    }

    public override Xml VisitComment(Comment comment, RpcSendQueue q)
    {
        q.GetAndSend(comment, c => c.Text);
        return comment;
    }

    public override Xml VisitDocTypeDecl(DocTypeDecl docTypeDecl, RpcSendQueue q)
    {
        q.GetAndSend(docTypeDecl, d => d.Name, n => Visit(n, q));
        q.GetAndSend(docTypeDecl, d => d.DocumentDeclaration);
        q.GetAndSend(docTypeDecl, d => d.ExternalId, e => Visit(e, q));
        q.GetAndSendList(docTypeDecl, d => d.InternalSubset, i => i.Id, i => Visit(i, q));
        q.GetAndSend(docTypeDecl, d => d.ExternalSubsetsNode, e => Visit(e, q));
        q.GetAndSend(docTypeDecl, d => d.BeforeTagDelimiterPrefix);
        return docTypeDecl;
    }

    public override Xml VisitDocTypeDeclExternalSubsets(DocTypeDecl.ExternalSubsets externalSubsets, RpcSendQueue q)
    {
        q.GetAndSendList(externalSubsets, e => e.Elements, el => el.Id, el => Visit(el, q));
        return externalSubsets;
    }

    public override Xml VisitElement(Element element, RpcSendQueue q)
    {
        q.GetAndSendList(element, e => e.Subset, i => i.Id, i => Visit(i, q));
        q.GetAndSend(element, e => e.BeforeTagDelimiterPrefix);
        return element;
    }

    public override Xml VisitIdent(Ident ident, RpcSendQueue q)
    {
        q.GetAndSend(ident, i => i.Name);
        return ident;
    }

    public override Xml VisitJspDirective(JspDirective jspDirective, RpcSendQueue q)
    {
        q.GetAndSend(jspDirective, j => j.BeforeTypePrefix);
        q.GetAndSend(jspDirective, j => j.Type);
        q.GetAndSendList(jspDirective, j => j.Attributes, a => a.Id, a => Visit(a, q));
        q.GetAndSend(jspDirective, j => j.BeforeDirectiveEndPrefix);
        return jspDirective;
    }

    public override Xml VisitJspScriptlet(JspScriptlet jspScriptlet, RpcSendQueue q)
    {
        q.GetAndSend(jspScriptlet, j => j.JspContent);
        return jspScriptlet;
    }

    public override Xml VisitJspExpression(JspExpression jspExpression, RpcSendQueue q)
    {
        q.GetAndSend(jspExpression, j => j.JspContent);
        return jspExpression;
    }

    public override Xml VisitJspDeclaration(JspDeclaration jspDeclaration, RpcSendQueue q)
    {
        q.GetAndSend(jspDeclaration, j => j.JspContent);
        return jspDeclaration;
    }

    public override Xml VisitJspComment(JspComment jspComment, RpcSendQueue q)
    {
        q.GetAndSend(jspComment, j => j.JspContent);
        return jspComment;
    }
}
