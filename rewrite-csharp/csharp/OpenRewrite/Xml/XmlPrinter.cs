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

namespace OpenRewrite.Xml;

/// <summary>
/// Prints XML LST back to source code.
/// Mirrors Java's org.openrewrite.xml.internal.XmlPrinter.
/// </summary>
public class XmlPrinter<P> : XmlVisitor<PrintOutputCapture<P>>
{
    public override Xml VisitDocument(Document document, PrintOutputCapture<P> p)
    {
        p.Append(document.Prefix);
        if (document.Prolog != null) Visit(document.Prolog, p);
        Visit(document.Root, p);
        p.Append(document.Eof);
        return document;
    }

    public override Xml VisitProlog(Prolog prolog, PrintOutputCapture<P> p)
    {
        p.Append(prolog.Prefix);
        if (prolog.XmlDecl != null) Visit(prolog.XmlDecl, p);
        foreach (var misc in prolog.MiscList) Visit(misc, p);
        foreach (var jsp in prolog.JspDirectives) Visit(jsp, p);
        return prolog;
    }

    public override Xml VisitXmlDecl(XmlDecl xmlDecl, PrintOutputCapture<P> p)
    {
        p.Append(xmlDecl.Prefix);
        p.Append("<?").Append(xmlDecl.Name);
        foreach (var attr in xmlDecl.Attributes) Visit(attr, p);
        p.Append(xmlDecl.BeforeTagDelimiterPrefix).Append("?>");
        return xmlDecl;
    }

    public override Xml VisitTag(Tag tag, PrintOutputCapture<P> p)
    {
        p.Append(tag.Prefix);
        p.Append('<').Append(tag.Name);
        foreach (var attr in tag.Attributes) Visit(attr, p);
        p.Append(tag.BeforeTagDelimiterPrefix);
        if (tag.ClosingTag == null)
        {
            p.Append("/>");
        }
        else
        {
            p.Append('>');
            if (tag.ContentList != null)
            {
                foreach (var content in tag.ContentList) Visit(content, p);
            }
            Visit(tag.ClosingTag, p);
        }
        return tag;
    }

    public override Xml VisitTagClosing(Tag.Closing closing, PrintOutputCapture<P> p)
    {
        p.Append(closing.Prefix);
        p.Append("</").Append(closing.Name).Append(closing.BeforeTagDelimiterPrefix).Append(">");
        return closing;
    }

    public override Xml VisitAttribute(Attribute attribute, PrintOutputCapture<P> p)
    {
        p.Append(attribute.Prefix);
        p.Append(attribute.Key.Prefix).Append(attribute.Key.Name);
        p.Append(attribute.BeforeEquals).Append('=');
        Visit(attribute.Val, p);
        return attribute;
    }

    public override Xml VisitAttributeValue(Attribute.Value value, PrintOutputCapture<P> p)
    {
        p.Append(value.Prefix);
        var delim = value.QuoteStyle == Attribute.Value.Quote.Double ? '"' : '\'';
        p.Append(delim).Append(value.Val).Append(delim);
        return value;
    }

    public override Xml VisitCharData(CharData charData, PrintOutputCapture<P> p)
    {
        p.Append(charData.Prefix);
        if (charData.Cdata)
        {
            p.Append("<![CDATA[").Append(charData.Text).Append("]]>");
        }
        else
        {
            p.Append(charData.Text);
        }
        p.Append(charData.AfterText);
        return charData;
    }

    public override Xml VisitComment(Comment comment, PrintOutputCapture<P> p)
    {
        p.Append(comment.Prefix);
        p.Append("<!--").Append(comment.Text).Append("-->");
        return comment;
    }

    public override Xml VisitProcessingInstruction(ProcessingInstruction pi, PrintOutputCapture<P> p)
    {
        p.Append(pi.Prefix);
        p.Append("<?").Append(pi.Name);
        Visit(pi.ProcessingInstructions, p);
        p.Append(pi.BeforeTagDelimiterPrefix).Append("?>");
        return pi;
    }

    public override Xml VisitDocTypeDecl(DocTypeDecl docTypeDecl, PrintOutputCapture<P> p)
    {
        p.Append(docTypeDecl.Prefix);
        p.Append("<!").Append(docTypeDecl.DocumentDeclaration);
        Visit(docTypeDecl.Name, p);
        if (docTypeDecl.ExternalId != null) Visit(docTypeDecl.ExternalId, p);
        foreach (var ident in docTypeDecl.InternalSubset) Visit(ident, p);
        if (docTypeDecl.ExternalSubsetsNode != null) Visit(docTypeDecl.ExternalSubsetsNode, p);
        p.Append(docTypeDecl.BeforeTagDelimiterPrefix).Append('>');
        return docTypeDecl;
    }

    public override Xml VisitDocTypeDeclExternalSubsets(DocTypeDecl.ExternalSubsets externalSubsets, PrintOutputCapture<P> p)
    {
        p.Append(externalSubsets.Prefix);
        p.Append('[');
        foreach (var elem in externalSubsets.Elements) Visit(elem, p);
        p.Append(']');
        return externalSubsets;
    }

    public override Xml VisitElement(Element element, PrintOutputCapture<P> p)
    {
        p.Append(element.Prefix);
        foreach (var ident in element.Subset) Visit(ident, p);
        p.Append(element.BeforeTagDelimiterPrefix);
        return element;
    }

    public override Xml VisitIdent(Ident ident, PrintOutputCapture<P> p)
    {
        p.Append(ident.Prefix).Append(ident.Name);
        return ident;
    }

    public override Xml VisitJspDirective(JspDirective jspDirective, PrintOutputCapture<P> p)
    {
        p.Append(jspDirective.Prefix);
        p.Append("<%@").Append(jspDirective.BeforeTypePrefix).Append(jspDirective.Type);
        foreach (var attr in jspDirective.Attributes) Visit(attr, p);
        p.Append(jspDirective.BeforeDirectiveEndPrefix).Append("%>");
        return jspDirective;
    }

    public override Xml VisitJspScriptlet(JspScriptlet jspScriptlet, PrintOutputCapture<P> p)
    {
        p.Append(jspScriptlet.Prefix);
        p.Append("<%").Append(jspScriptlet.JspContent).Append("%>");
        return jspScriptlet;
    }

    public override Xml VisitJspExpression(JspExpression jspExpression, PrintOutputCapture<P> p)
    {
        p.Append(jspExpression.Prefix);
        p.Append("<%=").Append(jspExpression.JspContent).Append("%>");
        return jspExpression;
    }

    public override Xml VisitJspDeclaration(JspDeclaration jspDeclaration, PrintOutputCapture<P> p)
    {
        p.Append(jspDeclaration.Prefix);
        p.Append("<%!").Append(jspDeclaration.JspContent).Append("%>");
        return jspDeclaration;
    }

    public override Xml VisitJspComment(JspComment jspComment, PrintOutputCapture<P> p)
    {
        p.Append(jspComment.Prefix);
        p.Append("<%--").Append(jspComment.JspContent).Append("--%>");
        return jspComment;
    }
}
