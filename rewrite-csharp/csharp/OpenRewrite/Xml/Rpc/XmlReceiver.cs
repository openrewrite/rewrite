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

public class XmlReceiver : XmlVisitor<RpcReceiveQueue>
{
    public override Xml? PreVisit(Xml x, RpcReceiveQueue q)
    {
        x = (Xml)x.WithId(q.ReceiveAndGet<Guid, string>(x.Id, Guid.Parse));
        switch (x)
        {
            case Document doc:
                x = doc.WithPrefix(q.Receive(doc.Prefix)!).WithMarkers(q.Receive(doc.Markers) ?? Markers.Empty);
                break;
            case Prolog pro:
                x = pro.WithPrefix(q.Receive(pro.Prefix)!).WithMarkers(q.Receive(pro.Markers) ?? Markers.Empty);
                break;
            case XmlDecl xd:
                x = xd.WithPrefix(q.Receive(xd.Prefix)!).WithMarkers(q.Receive(xd.Markers) ?? Markers.Empty);
                break;
            case ProcessingInstruction pi:
                x = pi.WithPrefix(q.Receive(pi.Prefix)!).WithMarkers(q.Receive(pi.Markers) ?? Markers.Empty);
                break;
            case Tag.Closing closing:
                x = closing.WithPrefix(q.Receive(closing.Prefix)!).WithMarkers(q.Receive(closing.Markers) ?? Markers.Empty);
                break;
            case Tag tag:
                x = tag.WithPrefix(q.Receive(tag.Prefix)!).WithMarkers(q.Receive(tag.Markers) ?? Markers.Empty);
                break;
            case Attribute.Value val:
                x = val.WithPrefix(q.Receive(val.Prefix)!).WithMarkers(q.Receive(val.Markers) ?? Markers.Empty);
                break;
            case Attribute attr:
                x = attr.WithPrefix(q.Receive(attr.Prefix)!).WithMarkers(q.Receive(attr.Markers) ?? Markers.Empty);
                break;
            case CharData cd:
                x = cd.WithPrefix(q.Receive(cd.Prefix)!).WithMarkers(q.Receive(cd.Markers) ?? Markers.Empty);
                break;
            case Comment comment:
                x = comment.WithPrefix(q.Receive(comment.Prefix)!).WithMarkers(q.Receive(comment.Markers) ?? Markers.Empty);
                break;
            case DocTypeDecl.ExternalSubsets es:
                x = es.WithPrefix(q.Receive(es.Prefix)!).WithMarkers(q.Receive(es.Markers) ?? Markers.Empty);
                break;
            case DocTypeDecl dtd:
                x = dtd.WithPrefix(q.Receive(dtd.Prefix)!).WithMarkers(q.Receive(dtd.Markers) ?? Markers.Empty);
                break;
            case Element elem:
                x = elem.WithPrefix(q.Receive(elem.Prefix)!).WithMarkers(q.Receive(elem.Markers) ?? Markers.Empty);
                break;
            case Ident ident:
                x = ident.WithPrefix(q.Receive(ident.Prefix)!).WithMarkers(q.Receive(ident.Markers) ?? Markers.Empty);
                break;
            case JspDirective jd:
                x = jd.WithPrefix(q.Receive(jd.Prefix)!).WithMarkers(q.Receive(jd.Markers) ?? Markers.Empty);
                break;
            case JspScriptlet js:
                x = js.WithPrefix(q.Receive(js.Prefix)!).WithMarkers(q.Receive(js.Markers) ?? Markers.Empty);
                break;
            case JspExpression je:
                x = je.WithPrefix(q.Receive(je.Prefix)!).WithMarkers(q.Receive(je.Markers) ?? Markers.Empty);
                break;
            case JspDeclaration jdec:
                x = jdec.WithPrefix(q.Receive(jdec.Prefix)!).WithMarkers(q.Receive(jdec.Markers) ?? Markers.Empty);
                break;
            case JspComment jc:
                x = jc.WithPrefix(q.Receive(jc.Prefix)!).WithMarkers(q.Receive(jc.Markers) ?? Markers.Empty);
                break;
        }
        return x;
    }

    public override Xml VisitDocument(Document document, RpcReceiveQueue q)
    {
        var d = (Document)document.WithSourcePath(q.ReceiveAndGet<string, string>(document.SourcePath, s => s)!);
        return d
            .WithCharsetName(q.Receive(d.CharsetName))
            .WithCharsetBomMarked(q.Receive(d.CharsetBomMarked))
            .WithChecksum(q.Receive(d.Checksum))
            .WithFileAttributes(q.Receive(d.FileAttributes))
            .WithProlog(q.Receive(d.Prolog, p => (Prolog)VisitNonNull(p, q)))
            .WithRoot(q.Receive(d.Root, t => (Tag)VisitNonNull(t, q))!)
            .WithEof(q.Receive(d.Eof)!);
    }

    public override Xml VisitProlog(Prolog prolog, RpcReceiveQueue q)
    {
        return prolog
            .WithXmlDecl(q.Receive(prolog.XmlDecl, d => (XmlDecl)VisitNonNull(d, q)))
            .WithMiscList(q.ReceiveList(prolog.MiscList, m => (Misc)VisitNonNull(m!, q))!)
            .WithJspDirectives(q.ReceiveList(prolog.JspDirectives, j => (JspDirective)VisitNonNull(j!, q))!);
    }

    public override Xml VisitXmlDecl(XmlDecl xmlDecl, RpcReceiveQueue q)
    {
        return xmlDecl
            .WithName(q.Receive(xmlDecl.Name)!)
            .WithAttributes(q.ReceiveList(xmlDecl.Attributes, a => (Attribute)VisitNonNull(a!, q))!)
            .WithBeforeTagDelimiterPrefix(q.Receive(xmlDecl.BeforeTagDelimiterPrefix)!);
    }

    public override Xml VisitProcessingInstruction(ProcessingInstruction pi, RpcReceiveQueue q)
    {
        return pi
            .WithName(q.Receive(pi.Name)!)
            .WithProcessingInstructions(q.Receive(pi.ProcessingInstructions, c => (CharData)VisitNonNull(c, q))!)
            .WithBeforeTagDelimiterPrefix(q.Receive(pi.BeforeTagDelimiterPrefix)!);
    }

    public override Xml VisitTag(Tag tag, RpcReceiveQueue q)
    {
        return tag
            .WithName(q.Receive(tag.Name)!)
            .WithAttributes(q.ReceiveList(tag.Attributes, a => (Attribute)VisitNonNull(a!, q))!)
            .WithContentList(q.ReceiveList(tag.ContentList, c => (Content)VisitNonNull(c!, q)))
            .WithClosingTag(q.Receive(tag.ClosingTag, c => (Tag.Closing)VisitNonNull(c, q)))
            .WithBeforeTagDelimiterPrefix(q.Receive(tag.BeforeTagDelimiterPrefix)!);
    }

    public override Xml VisitTagClosing(Tag.Closing closing, RpcReceiveQueue q)
    {
        return closing
            .WithName(q.Receive(closing.Name)!)
            .WithBeforeTagDelimiterPrefix(q.Receive(closing.BeforeTagDelimiterPrefix)!);
    }

    public override Xml VisitAttribute(Attribute attribute, RpcReceiveQueue q)
    {
        return attribute
            .WithKey(q.Receive(attribute.Key, k => (Ident)VisitNonNull(k, q))!)
            .WithBeforeEquals(q.Receive(attribute.BeforeEquals)!)
            .WithVal(q.Receive(attribute.Val, v => (Attribute.Value)VisitNonNull(v, q))!);
    }

    public override Xml VisitAttributeValue(Attribute.Value value, RpcReceiveQueue q)
    {
        return value
            .WithQuoteStyle(q.ReceiveAndGet<Attribute.Value.Quote, string>(value.QuoteStyle, s => Enum.Parse<Attribute.Value.Quote>(s)))
            .WithVal(q.Receive(value.Val)!);
    }

    public override Xml VisitCharData(CharData charData, RpcReceiveQueue q)
    {
        return charData
            .WithCdata(q.Receive(charData.Cdata))
            .WithText(q.Receive(charData.Text)!)
            .WithAfterText(q.Receive(charData.AfterText)!);
    }

    public override Xml VisitComment(Comment comment, RpcReceiveQueue q)
    {
        return comment.WithText(q.Receive(comment.Text)!);
    }

    public override Xml VisitDocTypeDecl(DocTypeDecl docTypeDecl, RpcReceiveQueue q)
    {
        return docTypeDecl
            .WithName(q.Receive(docTypeDecl.Name, n => (Ident)VisitNonNull(n, q))!)
            .WithDocumentDeclaration(q.Receive(docTypeDecl.DocumentDeclaration)!)
            .WithExternalId(q.Receive(docTypeDecl.ExternalId, e => (Ident)VisitNonNull(e, q)))
            .WithInternalSubset(q.ReceiveList(docTypeDecl.InternalSubset, i => (Ident)VisitNonNull(i!, q))!)
            .WithExternalSubsetsNode(q.Receive(docTypeDecl.ExternalSubsetsNode, e => (DocTypeDecl.ExternalSubsets)VisitNonNull(e, q)))
            .WithBeforeTagDelimiterPrefix(q.Receive(docTypeDecl.BeforeTagDelimiterPrefix)!);
    }

    public override Xml VisitDocTypeDeclExternalSubsets(DocTypeDecl.ExternalSubsets externalSubsets, RpcReceiveQueue q)
    {
        return externalSubsets
            .WithElements(q.ReceiveList(externalSubsets.Elements, e => (Element)VisitNonNull(e!, q))!);
    }

    public override Xml VisitElement(Element element, RpcReceiveQueue q)
    {
        return element
            .WithSubset(q.ReceiveList(element.Subset, i => (Ident)VisitNonNull(i!, q))!)
            .WithBeforeTagDelimiterPrefix(q.Receive(element.BeforeTagDelimiterPrefix)!);
    }

    public override Xml VisitIdent(Ident ident, RpcReceiveQueue q)
    {
        return ident.WithName(q.Receive(ident.Name)!);
    }

    public override Xml VisitJspDirective(JspDirective jspDirective, RpcReceiveQueue q)
    {
        return jspDirective
            .WithBeforeTypePrefix(q.Receive(jspDirective.BeforeTypePrefix)!)
            .WithType(q.Receive(jspDirective.Type)!)
            .WithAttributes(q.ReceiveList(jspDirective.Attributes, a => (Attribute)VisitNonNull(a!, q))!)
            .WithBeforeDirectiveEndPrefix(q.Receive(jspDirective.BeforeDirectiveEndPrefix)!);
    }

    public override Xml VisitJspScriptlet(JspScriptlet jspScriptlet, RpcReceiveQueue q)
    {
        return jspScriptlet.WithJspContent(q.Receive(jspScriptlet.JspContent)!);
    }

    public override Xml VisitJspExpression(JspExpression jspExpression, RpcReceiveQueue q)
    {
        return jspExpression.WithJspContent(q.Receive(jspExpression.JspContent)!);
    }

    public override Xml VisitJspDeclaration(JspDeclaration jspDeclaration, RpcReceiveQueue q)
    {
        return jspDeclaration.WithJspContent(q.Receive(jspDeclaration.JspContent)!);
    }

    public override Xml VisitJspComment(JspComment jspComment, RpcReceiveQueue q)
    {
        return jspComment.WithJspContent(q.Receive(jspComment.JspContent)!);
    }
}
